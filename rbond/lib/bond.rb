require 'singleton'
require 'neatjson'
require 'fileutils'
require 'shellwords'
require_relative 'bond/targetable'

# Singleton class providing the core functionality of Bond. You will generally
# access this through the {BondTargetable#bond} method exported to you when you
# `extend` BondTargetable, but it can also be accessed (e.g. for functions not
# contained in a class/module) via `Bond.instance`.
class Bond
  extend BondTargetable
  include Singleton
  # TODO ETK make this able to use other test frameworks as well

  # Maximum number of characters to allow in any test file name.
  # File names which would be longer than this will be truncated
  # to 10 fewer characters than the max, and a hash of the full
  # name will be appended to uniquely identify the file.
  MAX_FILE_NAME_LENGTH = 100

  attr_reader :test_name # The name of the test currently being evaluated

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
  def settings(spy_groups: nil, observation_directory: nil, reconcile: nil,
               record_mode: nil, decimal_precision: nil)
    raise 'not yet implemented' unless spy_groups.nil? # TODO spy_groups
    unless observation_directory.nil?
      @observation_directory = observation_directory
      load_replay_values
    end
    @reconcile = reconcile unless reconcile.nil?
    @record_mode = record_mode unless record_mode.nil?
    @decimal_precision = decimal_precision unless decimal_precision.nil?
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
  #         include_context :bond, observation_directory: 'spec/observations'
  #
  #         context 'when nothing is wrong' do
  #           it 'should work!' do
  #             ... test code ...
  #             # `include_context :bond` automatically exports a `bond` variable
  #             # for you to access for e.g. `spy` and `deploy_agent`
  #             bond.spy('spy_point', key: value, ...)
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
  # @param decimal_precision The precision to use when serializing Float and Double values.
  #     If not specified, defaults to 4 decimal places.
  # @param record_mode If true, set all record-replay style agents (see {Bond#deploy_record_replay_agent})
  #     into record mode for the duration of this test. This is overriden if
  #     `record_mode: false` is explicitly set on a given agent.
  #
  def start_test(rspec_test, test_name: nil, spy_groups: nil,
                 observation_directory: nil, reconcile: nil,
                 decimal_precision: nil, record_mode: false)

    @observations = []
    @spy_agents = Hash.new { |hash, key|
      hash[key] = []
    }
    @replay_values = Hash.new(:agent_result_none)
    @instance_names = Hash.new
    @observation_directory = nil
    @current_test = rspec_test

    if test_name.nil?
      test_file = @current_test.metadata[:file_path]
      # TODO ETK allow other characters besides alphanumeric?
      @test_name = File.basename(test_file, File.extname(test_file)) + '.' +
          @current_test.metadata[:full_description].gsub(/[^A-z0-9.]/, '_')
    else
      @test_name = test_name
    end

    settings(spy_groups: spy_groups, observation_directory: observation_directory, record_mode: record_mode,
             reconcile: reconcile, decimal_precision: decimal_precision.nil? ? 4 : decimal_precision)
    load_replay_values
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
  # @return if an agent has been set for this spy point, this will return (or, if specified,
  #     yield) whatever value is specified by that agent. Otherwise, returns `:agent_result_none`.
  def spy(spy_point_name=nil, **observation, &blk)
    spy_res = spy_internal(spy_point_name, false, **observation, &blk)
    result = spy_res[:result]
    yield result if spy_res[:should_yield]
    result
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

  # Deploy a record-replay style agent to watch a specific spy point. The most
  # recently deployed agent will always take precedence over any previous agents
  # for a given spy point. However, if the most recent agent does not apply to the
  # current observation (because its filters do not match), the next most recent agent
  # will be used, and so on. At most one agent will be applied.
  # Note that this agent type is *only* valid for method spy points, i.e. those specified
  # by {BondTargetable#spy_point} or {BondTargetable#spy_point_on}.
  # This agent is special in that it implements record-replay. If `record_mode` is `true`,
  # any time during testing that this agent is activated, it will call the underlying method,
  # record the output of the method, and display it to the user. The user then has the option
  # of accepting the output of the method, or first editing that output. The (optionally
  # edited) output will be saved for future use when in replay mode. If `record_mode` is `false`
  # (the default), the record-replay agent will search for a saved value to use (based on the
  # arguments to the method being spied on) and return that value. This is useful for e.g.
  # mocking out complex HTTP interactions; simply play it once in record mode, see that
  # the external system responded as expected, and save for future use.
  # Only invocations whose parameters match the recorded invocation exactly will be
  # replayed; a single spy point can store multiple replay values with different
  # call arguments simultaneously.
  # When in replay mode, if a stored value cannot be found, the behavior depends on
  # the current reconcile mode set for bond:
  #   - accept: It is treated as if the spy point was in record mode.
  #   - abort: An error is thrown
  #   - dialog/console: The user is asked whether or not the spy point should
  #                     proceed as if in record mode or abort and throw an error.
  # @param spy_point_name [#to_s] The name of the spy point for which to deploy this agent
  # @param (see RecordReplaySpyAgent#initialize)
  def deploy_record_replay_agent(spy_point_name, **opts)
    raise 'You must enable testing before using deploy_record_replay_agent' unless active?
    spy_point_name = spy_point_name.to_s
    unless opts.has_key?(:record_mode) # User-specified overrides test settings
      opts[:record_mode] = @record_mode
    end
    agent = RecordReplaySpyAgent.new(**opts)
    @spy_agents[spy_point_name] = @spy_agents[spy_point_name].unshift(agent)
    @spy_agents[spy_point_name + '.result'] = @spy_agents[spy_point_name + '.result'].unshift(agent)
  end

  # Register a specific object instance, giving it a name. Subsequent to this, whenever
  # a call to a method being spied on (via {BondTargetable#spy_point}) is made on the
  # given instance, the observation will include "`__instance_name__: name`".
  # This can be useful to differentiate calls made to different objects of the
  # same type.
  # @param object_instance [Object] The object instance to register a name to.
  # @param name [#to_s] The name to give the object instance.
  def register_instance(object_instance, name)
    raise 'You must enable testing before using register_instance' unless active?
    @instance_names[object_instance.object_id] = name.to_s
  end

  # An internal-use only method for passing some extra parameters to and getting extra
  # returns from spy; used to facilitate spy_points.
  # @param skip_save_observation [Boolean] Whether or not to skip recording the observation
  #     created during this call to spy; other agent actions are enabled regardless. This is
  #     used by {BondTargetable#spy_point} to enable `mock_only` spy points.
  # @param (see #spy)
  # @private
  def spy_internal(spy_point_name=nil, skip_save_observation=false, **observation, &blk)
    return :agent_result_none unless active? # If we're not testing, don't do anything

    spy_point_name = spy_point_name.nil? ? nil : spy_point_name.to_s

    observation[:__spy_point__] = spy_point_name unless spy_point_name.nil?
    observation = deep_clone(observation)
    active_agent = spy_point_name.nil? ? nil : @spy_agents[spy_point_name].find { |agent| agent.process?(observation) }

    do_save_observation = !skip_save_observation
    unless active_agent.nil? or active_agent.skip_save_observation.nil?
      do_save_observation = !active_agent.skip_save_observation
    end

    spy_ret = { result: :agent_result_none, should_yield: false, record_replay: false }
    begin
      unless active_agent.nil?
        active_agent.do(observation)
        spy_ret = active_agent.result(observation)
      end
    ensure
      if do_save_observation
        formatted = format_observation(observation, active_agent)
        @observations <<= formatted
        #TODO ETK printing
        puts "Observing: #{formatted} #{", returning <#{spy_ret[:result].to_s}>" if spy_ret[:result] != :agent_result_none}"
      end
    end

    spy_ret
  end

  # Look for a saved replay value matching the given observation
  # @private
  def get_replay_value(observation)
    @replay_values[Utils.observation_json_serde(observation)]
  end

  # Add in a new replay value to the mapping; this should be done whenever
  # a value is recorded so that it can be reused later.
  # @private
  def add_replay_value(observation, value)
    cleaned_obs = Utils.observation_json_serde(observation)
    cleaned_obs[:__replay_index__] = 0
    while @replay_values.has_key?(cleaned_obs)
      cleaned_obs[:__replay_index__] = cleaned_obs[:__replay_index__] + 1
    end
    @replay_values[cleaned_obs] = value
  end

  # Clear all replay values; used for testing purposes only
  # @private
  def clear_replay_values
    @replay_values.clear
  end

  # Internal method; used to retrieve the name associated with a given object instance.
  # @param object_instance [Object] The object instance for which to look up a name
  # @return `nil` if no name is associated with `object_instance`, else the associated name
  #     (from a previous call to `register_instance`).
  # @private
  def instance_name(object_instance)
    @instance_names[object_instance.object_id]
  end

  bond.spy_point(mock_only: true)
  # Return the current reconciliation mode
  # @private
  def reconcile_mode
    @reconcile
  end

  private

  # Directory containing the Python scripts Ruby-Bond relies on.
  BOND_PYTHON_DIR = File.join(File.dirname(File.dirname(__FILE__)), 'bin')
  # Script for reconciliation
  BOND_RECONCILE_SCRIPT = File.absolute_path(File.join(BOND_PYTHON_DIR, 'bond_reconcile.py'))
  # Script for obtaining user input
  BOND_DIALOG_SCRIPT = File.absolute_path(File.join(BOND_PYTHON_DIR, 'bond_dialog.py'))

  # Return the file name where observations for the current test should be
  # stored. Any hierarchy (as specified by .) in the test name becomes
  # a directory hierarchy, e.g. a test name of 'bond.my_tests.test_name'
  # would be stored at '`{base_directory}`/bond/my_tests/test_name.json'
  # If any portion of the file name (i.e. a directory or file name) is
  # longer than {#MAX_FILE_NAME_LENGTH} - 5 (to account for a possible
  # `.json` extension), reduce the length to 10 characters less than
  # this and fill the remaining 10 characters with a hash of the full name.
  #
  # An old version of this would return a very conflict-heavy hash which was
  # dependent only on the length of the file name; for backwards compatibility,
  # if a file cannot be found with the normal hash, this checks for one with
  # the older style of hash as well, and if found, returns that file name.
  def observation_file_name
    fname = hashed_file_name
    if not File.exist?(fname + '.json') and File.exist?(hashed_file_name(true) + '.json')
      hashed_file_name(true)
    else
      fname
    end
  end

  # Load all of the replay values in the current observation file into the
  # replay_values map.
  def load_replay_values
    fname = observation_file_name + '.json'
    return unless File.exist?(fname) # Can't load if we don't have an existing obs file
    File.open(fname, 'r') do |f|
      observations = Utils.observation_from_json(f.read)
      last_args = nil
      observations.each do |obs|
        if obs.has_key?(:__record_args__)
          last_args = obs
        elsif obs.has_key?(:__replay_result__)
          add_replay_value(last_args, obs[:result])
        end
      end
    end
  end

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
    
    if @current_test.exception.nil?
      test_fail = nil
    else
      test_fail = "Test had failure(s): #{@current_test.exception}\n#{@current_test.exception.backtrace.join("\n")}"
    end

    ref_file = fname + '.json'
    cur_file = fname + '_now.json'
    File.delete(cur_file) if File.exists?(cur_file)
    save_observations(cur_file)

    reconcile_result = reconcile_observations(ref_file, cur_file, test_fail)
    return :test_fail unless test_fail.nil?
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
    unless File.exists?(BOND_RECONCILE_SCRIPT)
      raise "Cannot find the bond_reconcile script: #{BOND_RECONCILE_SCRIPT}"
    end

    cmd = "#{Shellwords.shellescape(BOND_RECONCILE_SCRIPT)} " +
        "--reference #{Shellwords.shellescape(ref_file)} " +
        "--current #{Shellwords.shellescape(cur_file)} " +
        "--test #{Shellwords.shellescape(@test_name)} " +
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
    FileUtils.mkdir_p(File.dirname(fname))
    File.open(fname, 'w') do |f|
      f.print("[\n#{@observations.join(",\n")}\n]\n")
    end
  end

  # Return a hashed file name as specified by {#observation_file_name}; if
  # use_old_hash is true, it uses the older, more conflict-heavy version of
  # the hash function.
  def hashed_file_name(use_old_hash = false)
    name_array = @test_name.split('.').map do |name|
      if name.length <= MAX_FILE_NAME_LENGTH - 5
        name
      else
        # Using djb2 hash algorithm translated from http://www.cse.yorku.ca/~oz/hash.html
        if use_old_hash
          name_hash = name.chars.inject(5381) { |sum, c| ((sum << 5) + sum) + c.to_i }
        else
          name_hash = name.chars.inject(5381) { |sum, c| (((sum << 5) + sum) + c.ord) % 2**64 }
        end
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

  # Formats the observation hash. If any objects encountered have a `to_json` method,
  # it will be called to serialize the object. Note that `to_json` takes one argument
  # which is a JSON::Ext::Generator::State object; you should pass this object
  # into any `to_json` calls you make within your `to_json` function.
  # @param observation [Hash] Observations to be formatted.
  # @return The formatted hash.
  def format_observation(observation, agent = nil)
    agent.format(observation) unless agent.nil?
    Utils.observation_to_json(observation, @decimal_precision)
  end

  # Deep-clones an object
  #
  # - Hash: Creates a new hash containing all of the old key-value pairs
  # - Array: Creates a new array with the old contents
  # - Other: Attempts to call Object#clone. If this fails (results in #TypeError)
  #          then the object is returned as-is (assumes that non-cloneable objects
  #          are immutable and thus don't need cloning)
  #
  # @param obj The object to be cloned.
  # @return The deep-clone.
  def deep_clone(obj)
    if obj.is_a?(Hash)
      {}.tap do |new|
        obj.each do |k, v|
          new[k] = deep_clone(v)
        end
      end
    elsif obj.is_a?(Array)
      obj.map { |x| deep_clone(x) }
    else
      begin
        obj.clone
      rescue TypeError # Some types, e.g. Fixnum and Symbol, can't be cloned
        obj
      end
    end
  end

end


require_relative 'bond/spy_agent'
require_relative 'bond/utils'