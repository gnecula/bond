require 'singleton'
require 'json'
require 'fileutils'

# TODO still needs lots more documentation here...
class Bond
  include Singleton

  DEFAULT_OBSERVATION_DIRECTORY = '/tmp/bond_observations'
  @testing = false

  def settings(spy_groups: nil, observation_directory: nil, merge_type: nil)
    raise 'not yet implemented' unless spy_groups.nil? # TODO spy_groups
    @observation_directory = observation_directory unless observation_directory.nil?
    @merge_type = merge_type unless @merge_type.nil?
  end

  # TODO ETK make this able to use other test frameworks as well
  def start_test(rspec_test, test_name: nil, spy_groups: nil,
                 observation_directory: nil, merge_type: nil)
    @testing = true
    @observations = []
    @spy_agents = Hash.new { |hash, key|
      hash[key] = []
    }
    @observation_directory = nil
    @current_test = rspec_test
    @merge_type = merge_type

    if test_name.nil?
      test_file = @current_test.metadata[:file_path]
      # TODO ETK decide exactly what characters to allow?
      @test_name = File.basename(test_file, File.extname(test_file)) + '.' +
          @current_test.metadata[:full_description].gsub(/[^A-z0-9.()]/, '_')
    else
      @test_name = test_name
    end

    settings(spy_groups: spy_groups, observation_directory: observation_directory, merge_type: merge_type)

    # TODO ETK get the test information and stuff
    # spy groups
  end

  def spy(spy_point_name, **observation)
    raise 'You must enable testing before using spy' unless @testing
    spy_point_name = spy_point_name.to_s

    observation[:__spy_point__] = spy_point_name
    observation = deep_clone_sort_hashes(observation)
    active_agent = @spy_agents[spy_point_name].find { |agent| agent.process?(observation) }

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

  # Deploy an agent to watch a specific spy point (corresponding to +spy_point_name+).
  # Any keyword arguments specified as +opts+ will be passed to SpyAgent#new
  # The most recently deployed agent will always take precedence over any previous
  # agents for a given spy point.
  def deploy_agent(spy_point_name, **opts)
    raise 'You must enable testing before using deploy_agent' unless @testing
    spy_point_name = spy_point_name.to_s
    @spy_agents[spy_point_name] = @spy_agents[spy_point_name].unshift(SpyAgent.new(**opts))
  end

  def _finish_test
    # TODO ETK Collect errors, deal with failures

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

    ref_file = fname + '.json'
    cur_file = fname + '_now.json'
    File.delete(cur_file) if File.exists?(cur_file)
    save_observations(cur_file)

    # TRANSITION CODE: Run the external command to reconcile
    # TODO ETK right now using bond_reconcile from the python module, but really there should be
    #      a local copy (in rbond/bin?). Also less frailness in terms of finding the script -
    #      it probably shouldn't be based on the location of the @observation_directory
    return reconcile_observations(ref_file, cur_file)
  ensure
    @current_test = false
    @testing = false
  end

  private

  # Reconcile observations, for now using an external python script.
  # Takes ref_file to be the correct test output, and compares cur_file against it.
  # If ref_file does not exist, it will be treated as an empty file.
  # Returns +:pass+ if the reconciliation succeeds, else +:fail+
  def reconcile_observations(ref_file, cur_file)
    bond_reconcile_script = File.absolute_path(observation_directory + '/../../../pybond/bond/bond_reconcile.py')
    unless File.exists?(bond_reconcile_script)
      raise "Cannot find the bond_reconcile script: #{bond_reconcile_script}"
    end

    ENV['BOND_MERGE'] = @merge_type unless @merge_type.nil?

    cmd = "#{bond_reconcile_script} --reference #{ref_file} --current #{cur_file} --test #{@test_name}"
    puts "Running: #{cmd}"
    code = system(cmd)
    code ? :pass : :fail
  end

  # Save all current observations to a file located at fname. Assumes that
  # +@observations+ has already been JSON-serialized and outputs them all
  # as a JSON array.
  def save_observations(fname)
    File.open(fname, 'w') do |f|
      f.print("[\n#{@observations.join(",\n")}\n]\n")
    end
  end

  # Return the file name where observations for the current test should be
  # stored. Any hierarchy (as specified by .) in the test name becomes
  # a directory hierarchy, e.g. a test name of 'bond.my_tests.test_name'
  # would be stored at '{+base_directory+}/bond/my_tests/test_name.json'
  def observation_file_name
    File.join(observation_directory, @test_name.split('.'))
  end

  # Return the directory where observations should be stored
  # This can be specified with the :observation_directory setting
  # If not set, it will be a 'test_observations' directory located
  # in the same directory as the file containing the current test. 
  def observation_directory
    return @observation_directory unless @observation_directory.nil?
    test_file = @current_test.metadata[:file_path]
    File.join(File.dirname(File.basename(test_file, File.extname(test_file))),
        'test_observations')
  end

  def format_observation(observation, agent = nil)
    # TODO ETK actually have formatters
    # custom serialization options for the json serializer...?
    # way to sort the keys...?
    JSON.pretty_generate(observation, ident: ' '*4)
  end

  # Deep-clones an object while sorting any Hashes at any depth:
  #  #Hash::  Creates a new hash containing all of the old key-value
  #           pairs sorted by key
  #  #Array:: Creates a new array with the old contents *not* sorted
  #  Other::  Attempts to call Object#clone. If this fails (results in #TypeError)
  #           then the object is returned as-is (assumes that non-cloneable objects
  #           are immutable and thus don't need cloning)
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

# TODO needs more documentation
class SpyAgent
# TODO ETK needs formatters
  def initialize(**opts)
    @result_spec = nil
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

  def process?(observation)
    @filters.empty? || @filters.any? do |filter|
      !filter.accept?(observation)
    end
  end

  def do(observation)
    @doers.each { |doer| doer.call(observation) }
  end

  def result(observation)
    unless @exception_spec.nil?
      raise @exception_spec.respond_to?(:call) ? @exception_spec.call(observation) : @exception_spec
    end

    if @result_spec.nil?
      :agent_result_none
    elsif @result_spec.respond_to?(:call)
      @result_spec.call(observation)
    else
      @result_spec
    end
  end
end

class SpyAgentFilter

  def initialize(filter_key, filter_value)
    @field_name = nil
    @filter_func = nil

    filter_key = filter_key.to_s
    if filter_key == 'filter'
      # TODO ETK is this the correct check?
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

  def accept?(observation)
    if @field_name.nil?
      @filter_func.call(observation)
    else
      observation.has_key?(@field_name) && @filter_func.call(observation[@field_name])
    end
  end
end
