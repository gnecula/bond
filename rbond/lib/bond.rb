require 'singleton'
require 'json'
require 'fileutils'
require 'shellwords'

# Singleton class providing the core functionality of Bond. You will generally
# access this through the {BondTargetable#bond} method exported to you when you
# `extend` BondTargetable, but it can also be accessed (e.g. for functions not
# contained in a class/module) via `Bond.instance`.
class Bond
  include Singleton
  # TODO ETK make this able to use other test frameworks as well

  # Maximum number of characters to allow in any test file name
  # File names which would be longer than this will be truncated
  # to 10 fewer characters than the max, and a hash of the full
  # name will be appended to uniquely identify the file.
  MAX_FILE_NAME_LENGTH = 100

  # Returns true if Bond is currently active (in testing mode), else false.
  # If this returns false, you can safely assume that calls to {#spy} will
  # have no effect.
  def active?
    !@current_test.nil?
  end

  # Change the settings for Bond, overriding anything which was previously set by
  # {#start_test}. Accepts any of the keyword arguments that {#start_test} does
  # except for `test_name`, which cannot be changed.
  # @param (see #start_test)
  def settings(spy_groups: nil, observation_directory: nil, reconcile: nil)
    raise 'not yet implemented' unless spy_groups.nil? # TODO spy_groups
    @observation_directory = observation_directory unless observation_directory.nil?
    @reconcile = reconcile unless @reconcile.nil?
  end

  # Enable testing for Bond. When using RSpec, this will be called automatically
  # for you when you `include_context :bond`, to which you can pass all of the
  # same keyword arguments as `start_test`.
  # @param rspec_test The current test that is being run through RSpec.
  # @param test_name [String] The name of the current test. If not provided,
  #     the default is the test file concatenated with the full description
  #     string of the RSpec test, e.g. for a test within my_class_spec.rb that
  #     appeared as:
  #
  #       ```
  #       describe MyClass do
  #         include_context :bond
  #         context 'when nothing is wrong' do
  #           it 'should work!' do
  #             ... test code ...
  #           end
  #         end
  #       end
  #       ```
  #
  #     `test_name` would be 'my_class_spec.MyClass_when_nothing_is_wrong_should_work_'
  #     (note that all non-alphanumeric characters except periods are replaced with
  #     an underscore).
  # @param spy_groups NOT YET IMPLEMENTED.
  # @param observation_directory [String] Path to the directory where
  #     the observations for this test should be stored. The default is
  #     a 'test_observations' directory located in the same directory
  #     as the file containing the current test. Test observations will
  #     be stored within this directory. Any hierarchy (as specified by .)
  #     in the test name becomes a directory hierarchy, e.g. a test name of
  #     'bond.my_tests.test_name' would be stored at
  #     '`{observation_directory}`/bond/my_tests/test_name.json'
  # @param reconcile The action to take when there are differences found between
  #     the reference versions of test output and the current test output.
  #     Should be one of:
  #
  #     - **`:console`** - interactive console prompts to decide what to merge (default)
  #     - `:abort` - don't accept any new changes
  #     - `:accept` - accept all new changes
  #     - `:kdiff3` - use the kdiff3 graphical merge tool to reconcile differences
  #
  def start_test(rspec_test, test_name: nil, spy_groups: nil,
                 observation_directory: nil, reconcile: nil)

    @observations = []
    @spy_agents = Hash.new { |hash, key|
      hash[key] = []
    }
    @observation_directory = nil
    @current_test = rspec_test
    @reconcile = reconcile

    if test_name.nil?
      test_file = @current_test.metadata[:file_path]
      # TODO ETK allow other characters besides alphanumeric?
      @test_name = File.basename(test_file, File.extname(test_file)) + '.' +
          @current_test.metadata[:full_description].gsub(/[^A-z0-9.]/, '_')
    else
      @test_name = test_name
    end

    settings(spy_groups: spy_groups, observation_directory: observation_directory, reconcile: reconcile)
  end

  # The main entrypoint, used to observe some program state. If Bond is not active,
  # does nothing. Observes all keyword arguments within `observation`, recording them
  # to be written out to a file at the end of the current test. A deep copy
  # of the arguments is made, all hashes (at any level of nesting) are sorted, and
  # any object that is not an Array or Hash will have its `Object#clone` method called.
  # If it is not cloneable (`clone` throws an error), the original object will be used.
  # The arguments are then JSON-serialized. For any object which has a `to_json` method,
  # this will be called to serialize it. Note that `to_json` takes one argument which
  # is a JSON::Ext::Generator::State object; you should pass this object into any `to_json`
  # calls you make within your `to_json` function.
  # @param spy_point_name [#to_s] The name of this spy point. Will be used to subsequently
  #     refer to this point for, e.g., {#deploy_agent}. This name also gets printed as
  #     part of the observation with the key `__spy_point__`.
  # @param observation Keyword arguments which should be observed.
  # @return if an agent has been set for this spy point, this will return whatever value
  #     is specified by that agent. Otherwise, returns `:agent_result_none`.
  def spy(spy_point_name=nil, **observation)
    return :agent_result_none unless active? # If we're not testing, don't do anything

    spy_point_name = spy_point_name.nil? ? nil : spy_point_name.to_s

    observation[:__spy_point__] = spy_point_name unless spy_point_name.nil?
    observation = deep_clone_sort_hashes(observation)
    active_agent = spy_point_name.nil? ? nil : @spy_agents[spy_point_name].find { |agent| agent.process?(observation) }

    res = :agent_result_none
    begin
      unless active_agent.nil?
        active_agent.do(observation)
        res = active_agent.result(observation)
      end
    ensure
      formatted = format_observation(observation, active_agent)
      @observations <<= formatted
      #TODO ETK printing
      puts "Observing: #{formatted} #{", returning <#{res.to_s}>" if res != :agent_result_none}"
    end

    res
  end

  # Deploy an agent to watch a specific spy point. The most recently deployed agent
  # will always take precedence over any previous agents for a given spy point.
  # However, if the most recent agent does not apply to the current observation
  # (because its filters do not match), the next most recent agent will be used,
  # and so on. At most one agent will be applied.
  # @param spy_point_name [#to_s] The name of the spy point for which to deploy this agent
  # @param (see SpyAgent#initialize)
  def deploy_agent(spy_point_name, **opts)
    raise 'You must enable testing before using deploy_agent' unless active?
    spy_point_name = spy_point_name.to_s
    @spy_agents[spy_point_name] = @spy_agents[spy_point_name].unshift(SpyAgent.new(**opts))
  end

  private

  # Clean up from the current test. Marks Bond as no longer active, and saves
  # all of the current observations to a file specified by {#observation_file_name}.
  # Creates whatever directory structure necessary for this, and adds a .gitignore
  # file to ignore Bond's temporary output (if one does not already exist). Then begins
  # the reconciliation process. If the new observations are different
  # from the reference files, reconciles them using whatever means specified
  # by the `reconcile` setting.
  # @return If the test failed, returns `:test_fail`. If reconciliation fails
  #     (new changes are not accepted), returns `:bond_fail`. Else returns `:pass`
  def finish_test
    fname = observation_file_name
    fdir = File.dirname(fname)
    unless File.directory?(fdir)
      FileUtils.mkdir_p(fdir)
      top_git_ignore = File.join(observation_directory, '.gitignore')
      puts top_git_ignore
      unless File.file?(top_git_ignore)
        # TODO ETK make this configurable in case you don't use git?
        File.open(top_git_ignore, 'w') do |outfile|
          outfile.print("*_now.json\n*.diff\n")
        end
      end
    end

    test_fail = !@current_test.exception.nil?

    ref_file = fname + '.json'
    cur_file = fname + '_now.json'
    File.delete(cur_file) if File.exists?(cur_file)
    save_observations(cur_file)

    reconcile_result = reconcile_observations(ref_file, cur_file,
                                              test_fail ? 'Test had failure(s)!' : nil)
    return :test_fail if test_fail
    return reconcile_result
  ensure
    @current_test = nil
  end

  # Reconcile observations, for now using an external Python script.
  # Depending on the `reconcile` setting, will take action to reconcile the differences.
  # @param ref_file [String] Path to the accepted/reference test output.
  #     If this does not exist, it will be treated as an empty file.
  # @param cur_file [String] Path to the current test output.
  # @param no_save [nil, String] If not `nil`, `ref_file` will *not* be overwritten
  #     and the string will be displayed as the reason why saving is not allowed.
  # @return `:pass` if the reconciliation succeeds, else `:bond_fail`
  def reconcile_observations(ref_file, cur_file, no_save=nil)
    bond_reconcile_script = File.absolute_path(File.join(File.dirname(File.dirname(__FILE__)), 'bin', 'bond_reconcile.py'))
    unless File.exists?(bond_reconcile_script)
      raise "Cannot find the bond_reconcile script: #{bond_reconcile_script}"
    end

    cmd = "#{bond_reconcile_script} --reference #{ref_file} --current #{cur_file} --test #{@test_name} " +
        (@reconcile.nil? ? '' : "--reconcile #{@reconcile.to_s}") +
        (no_save.nil? ? '' : "--no-save #{Shellwords.shellescape(no_save.to_s)}")
    puts "Running: #{cmd}"
    code = system(cmd)
    code ? :pass : :bond_fail
  end

  # Save all current observations to a file. Assumes that `@observations`
  # has already been JSON-serialized and outputs them all as a JSON array.
  # @param fname [String] Path where the file should be saved.
  def save_observations(fname)
    File.open(fname, 'w') do |f|
      f.print("[\n#{@observations.join(",\n")}\n]\n")
    end
  end

  # Return the file name where observations for the current test should be
  # stored. Any hierarchy (as specified by .) in the test name becomes
  # a directory hierarchy, e.g. a test name of 'bond.my_tests.test_name'
  # would be stored at '`{base_directory}`/bond/my_tests/test_name.json'
  # If any portion of the file name (i.e. a directory or file name) is
  # longer than {#MAX_FILE_NAME_LENGTH} - 5 (to account for a possible
  # `.json` extension), reduce the length to 10 characters less than
  # this and fill the remaining 10 characters with a hash of the full name.
  def observation_file_name
    name_array = @test_name.split('.').map do |name|
      if name.length <= MAX_FILE_NAME_LENGTH - 5
        name
      else
        # Using djb2 hash algorithm translated from http://www.cse.yorku.ca/~oz/hash.html
        name_hash = name.chars.inject(5381) { |sum, c| ((sum << 5) + sum) + c.to_i }
        # Take start of name, up to first 10 chars of the hash as base 36 (alphanumerics)
        name[0, MAX_FILE_NAME_LENGTH - 15] + name_hash.to_s(36)[0, 10]
      end
    end
    File.join(observation_directory, name_array)
  end

  # Return the directory where observations should be stored
  # This can be specified with the `observation_directory` setting.
  # If not set, it will be a 'test_observations' directory located
  # in the same directory as the file containing the current test. 
  def observation_directory
    return @observation_directory unless @observation_directory.nil?
    test_file = @current_test.metadata[:file_path]
    File.join(File.dirname(File.absolute_path(test_file)), 'test_observations')
  end

  # Formats the observation hash. Currently, this just JSON-serializes
  # the hash. If any objects encountered have a `to_json` method, it will
  # be called to serialize the object. Note that `to_json` takes one argument
  # which is a JSON::Ext::Generator::State object; you should pass this object
  # into any `to_json` calls you make within your `to_json` function.
  # @param observation [Hash] Observations to be formatted.
  # @return The formatted hash.
  def format_observation(observation, agent = nil)
    # TODO ETK actually have formatters
    JSON.pretty_generate(observation, indent: ' '*4)
  end

  # Deep-clones an object while sorting any Hashes at any depth:
  #
  # - Hash: Creates a new hash containing all of the old key-value
  #         pairs sorted by key
  # - Array: Creates a new array with the old contents *not* sorted
  # - Other: Attempts to call Object#clone. If this fails (results in #TypeError)
  #          then the object is returned as-is (assumes that non-cloneable objects
  #          are immutable and thus don't need cloning)
  #
  # @param obj The object to be cloned.
  # @return The deep-clone with hashes sorted.
  def deep_clone_sort_hashes(obj)
    if obj.is_a?(Hash)
      {}.tap do |new|
        obj.sort.each do |k, v|
          new[k] = deep_clone_sort_hashes(v)
        end
      end
    elsif obj.is_a?(Array) # Don't sort arrays, just clone
      obj.map { |x| deep_clone_sort_hashes(x) }
    else
      begin
        obj.clone
      rescue TypeError # Some types, e.g. Fixnum and Symbol, can't be cloned
        obj
      end
    end
  end

end

# Represents an agent deployed by {Bond#deploy_agent}. Takes
# action on spy points depending on the options specified upon
# initialization.
# @api private
class SpyAgent

  # Initialize, setting the options for this SpyAgent.
  # @param opts Key-value pairs that control whether the agent is
  #     active and what it does. The following keys are recognized:
  #
  #     - Keys that restrict for which invocations of bond.spy this
  #       agent is active. All of these conditions must be true for
  #       the agent to be the active one:
  #
  #         - `key: val` - only when the observation dictionary contains
  #           the `key` with the given value
  #         - `key__contains: substr` - only when the observation dictionary
  #           contains the `key` with a string value that contains the given substr.
  #         - `key__startswith: substr` - only when the observation dictionary
  #           contains the `key` with a string value that starts with the given substr.
  #         - `key__endswith: substr` - only when the observation dictionary contains
  #           the `key` with a string value that ends with the given substr.
  #         - `filter: func` - only when the given func returns true when passed
  #           observation dictionary. The function should not make changes to
  #           the observation dictionary. Uses the observation before formatting.
  #
  #     - Keys that control what the observer does when processed:
  #
  #         - `do: func` - executes the given function with the observation dictionary.
  #           func can also be a list of functions, executed in order.
  #           The function should not make changes to the observation dictionary.
  #           Uses the observation before formatting.
  #
  #     - Keys that control what the corresponding spy returns (by default `:agent_result_none`):
  #
  #         - `exception: x` - the call to bond.spy throws the given exception. If `x`
  #           is a function it is invoked on the observation dictionary to compute
  #           the exception to throw. The function should not make changes to the
  #           observation dictionary. Uses the observation before formatting.
  #         - `result: x` - the call to bond.spy returns the given value. If `x` is a
  #           function it is invoked on the observe argument dictionary to compute
  #           the value to return. If the function throws an exception then the
  #           spied function throws an exception. The function should not make
  #           changes to the observation dictionary. Uses the observation before
  #           formatting.
  #
  #     - Keys that control how the observation is saved. This is processed after all
  #       the above functions. **NOT YET AVAILABLE**
  #
  #         - `formatter: func` - If specified, a function that is given the observation and
  #           can update it in place. The formatted observation is what gets serialized and saved.
  #
  def initialize(**opts)
    # TODO ETK needs formatters
    @result_spec = :agent_result_none
    @exception_spec = nil
    @doers = []
    @filters = []

    opts.each do |k, v|
      case k.to_s # Convert to string in case it was passed as a symbol
        when 'result'
          @result_spec = v
        when 'exception'
          @exception_spec = v
        when 'do'
          @doers = [*v]
        else # Must be a filter
          @filters <<= SpyAgentFilter.new(k.to_s, v)
      end
    end
  end

  # Checks if this agent should process this observation
  # @return true iff this agent should process this observation
  def process?(observation)
    @filters.empty? || @filters.all? do |filter|
      filter.accept?(observation)
    end
  end

  # Carries out all of the actions specified by the `do` option
  def do(observation)
    @doers.each { |doer| doer.call(observation) }
  end

  # Gets the result that this agent should return, dependent on the
  # `result` and `exception` options. If neither is present,
  # returns `:agent_result_none`.
  def result(observation)
    unless @exception_spec.nil?
      raise @exception_spec.respond_to?(:call) ? @exception_spec.call(observation) : @exception_spec
    end

    if !@result_spec.nil? && @result_spec.respond_to?(:call)
      @result_spec.call(observation)
    else
      @result_spec
    end
  end
end

# Filters used to determine whether or not a {SpyAgent} should
# be applied to a given observation.
# @api private
class SpyAgentFilter

  # Initialize this filter.
  # @see Bond#deploy_agent
  # @param filter_key [#to_s] The key for the filter.
  # @param filter_value The value for the filter.
  def initialize(filter_key, filter_value)
    @field_name = nil
    @filter_func = nil

    filter_key = filter_key.to_s
    if filter_key == 'filter'
      raise 'When using filter, passed value must be callable' unless filter_value.respond_to?(:call)
      @filter_func = filter_value
      return
    end

    key_parts = filter_key.split('__')
    if key_parts.length == 1
      @field_name = key_parts[0]
      @filter_func = lambda { |val| val == filter_value }
    elsif key_parts.length == 2
      @field_name = key_parts[0]
      case key_parts[1]
        when 'exact','eq'
          @filter_func = lambda { |val| val == filter_value }
        when 'startswith'
          @filter_func = lambda { |val| val.start_with?(filter_value) }
        when 'endswith'
          @filter_func = lambda { |val| val.end_with?(filter_value) }
        when 'contains'
          @filter_func = lambda { |val| val.include?(filter_value) }
        else
          raise "Unknown operator: #{key_parts[1]}"
      end
    else
      raise "Invalid key passed in: #{filter_key}"
    end
  end

  # Return true iff the provided observation meets this filter.
  def accept?(observation)
    if @field_name.nil?
      @filter_func.call(observation)
    elsif observation.has_key?(@field_name)
        @filter_func.call(observation[@field_name])
    elsif observation.has_key?(@field_name.to_sym)
      @filter_func.call(observation[@field_name.to_sym])
    else
      false
    end
  end

end

require_relative 'bond/bond_targetable'