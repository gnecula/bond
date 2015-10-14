require 'singleton'
require 'json'
require 'fileutils'

class Bond
  include Singleton

  DEFAULT_OBSERVATION_DIRECTORY = '/tmp/bond_observations'
  @testing = false

  def initialize
    @spy_agents = Hash.new { |hash, key|
      hash[key] = []
    }
    @observations = []
    @current_test = nil
  end

  def update_settings(**opt)
    opt.each_pair do |k, v|
      case k
        when :spy_groups
          # TODO
        when :observation_directory
          @observation_directory = v
        when :merge
          @merge_type = v
        else
          raise ArgumentError, "Reached an unrecognized setting: #{k} = #{v}"
      end
    end
  end

  # TODO make this able to use other test frameworks as well
  def start_test(rspec_test, **settings)
    @testing = true
    @observations = []
    @spy_agents = Hash.new { |hash, key|
      hash[key] = []
    }

    @current_test = rspec_test
    if settings.include?(:test_name)
      @test_name = settings[:test_name]
      settings.delete(:test_name)
    else
      test_file = @current_test.metadata[:file_path]
      # TODO decide exactly what characters to allow?
      @test_name = File.basename(test_file, File.extname(test_file)) + '.' +
          @current_test.metadata[:full_description].gsub(/[^A-z0-9.()]/, '_')
    end

    @observation_directory = nil
    update_settings(**settings)

    if @observation_directory.nil?
      @observation_directory = DEFAULT_OBSERVATION_DIRECTORY
      puts 'WARNING: You should set the settings(observation_directory).' +
               "Observations saved to #{DEFAULT_OBSERVATION_DIRECTORY}"
    end

    # TODO get the test information and stuff
    # spy groups
  end

  def spy(spy_point_name, **observation)
    raise 'You must enable testing before using spy' unless @testing
    spy_point_name = spy_point_name.to_s

    applicable_agents = @spy_agents[spy_point_name].select { |agent| agent.accept?(observation) }

    # TODO checking for 'ignore' and such

    formatted = format_observation(deep_clone(observation), applicable_agents)
    @observations <<= formatted

    # TODO
    puts "Observing: #{formatted}"

    applicable_agents.each do |agent|
      agent.do(observation)
      res = agent.result(observation)
      return res unless res == :agent_result_none
    end

    :agent_result_none
  end

  def deploy_agent(spy_point_name, **opts)
    raise 'You must enable testing before using deploy_agent' unless @testing
    spy_point_name = spy_point_name.to_s
    @spy_agents[spy_point_name] = @spy_agents[spy_point_name].unshift(SpyAgent.new(**opts))
  end

  def finish_test
    # TODO Collect errors, deal with failures

    fname = observation_file_name
    fdir = File.dirname(fname)
    unless File.directory?(fdir)
      FileUtils.mkdir_p(fdir)
      top_git_ignore = File.join(@observation_directory, '.gitignore')
      puts top_git_ignore
      unless File.file?(top_git_ignore)
        # TODO make this configurable in case you don't use git?
        File.open(top_git_ignore, 'w') do |outfile|
          outfile.print("*_now.json\n*.diff\n")
        end
      end
    end

    ref_file = fname + '.json'
    cur_file = fname + '_now.json'
    if File.exists?(ref_file)
      if File.exists?(cur_file)
        File.delete(cur_file)
      end

      save_observations(cur_file)
      # TRANSITION CODE: Run the external command to reconcile
      # TODO right now using bond_reconcile from the python module, but really there should be
      #      a local copy (in rbond/bin). Also less frailness in terms of finding the script -
      #      it probably shouldn't be based on the location of the @observation_directory
      bond_reconcile_script = File.absolute_path(@observation_directory + '/../../../pybond/bond/bond_reconcile.py')
      unless File.exists?(bond_reconcile_script)
        raise "Cannot find the bond_reconcile script: #{bond_reconcile_script}"
      end

      unless @merge_type.nil?
        ENV['BOND_MERGE'] = @merge_type
      end

      cmd = "#{bond_reconcile_script} --reference #{ref_file} --current #{cur_file} --test #{@test_name}"
      puts "Running: #{cmd}"
      code = system(cmd)
      return code ? :pass : :fail
    else
      # TODO printing
      puts "Saved observations in file #{ref_file}"
      save_observations(cur_file)
    end
  ensure
    @current_test = false
    @testing = false
  end

  private

  def save_observations(fname)
    File.open(fname, 'w') do |f|
      f.print("[\n")
      @observations.each_with_index do |obs, idx|
        f.print(obs)
        f.print(idx == @observations.length-1 ? "\n" : ",\n")
      end
      f.print("]\n")
    end
  end

  def observation_file_name
    File.join(@observation_directory, @test_name.split('.'))
  end

  def format_observation(observation, agents = [])
    # TODO actually have formatters
    # custom serialization options for the json serializer...?
    # way to sort the keys...?
    JSON.pretty_generate(observation, ident: ' '*4)
  end

  def deep_clone(obj)
    if obj.is_a?(Array)
      obj.map { |item| deep_clone(item) }
    elsif obj.is_a?(Hash)
      Hash[obj.map { |k,v| [deep_clone(k), deep_clone(v)] }]
    else
      begin
        obj.clone
      rescue TypeError # Some types, e.g. Fixnum and Symbol, can't be cloned
        obj
      end
    end
  end

end

class SpyAgent
# TODO needs formatters
  def initialize(**opts)
    @result_spec = nil
    @exception_spec = nil
    @doers = []
    @filters = []

    opts.each_pair do |k,v|
      case k
        when 'result'
          @result_spec = v
        when 'exception'
          @exception_spec = v
        when 'do'
          @doers += [*v]
        else # Must be a filter
          @filters <<= SpyAgentFilter.new(k, v)
      end
    end
  end

  def process?(observation)
    @filters.any? do |filter|
      !filter.accept?(observation)
    end
  end

  def do(observation)
    @doers.each { |doer| doer.call(observation) }
  end

  def result(observation)
    unless @exception_spec.nil?
      if @exception_spec.respond_to?(:call)
        raise @exception_spec.call
      else
        raise @exception_spec
      end
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

    if filter_key == 'filter'
      # TODO is this the correct check?
      unless filter_value.respond_to?(:call)
        raise 'When using filter, passed value must be callable'
      end
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

module BondTargetable

  @__last_annotation__ = nil

  def method_added(name)
    super
    return if @__last_annotation__.nil?

    orig_method = instance_method(name)

    if private_method_defined?(name)
      visibility = :private
    elsif protected_method_defined?(name)
      visibility = :protected
    else
      visibility = :public
    end

    this = self
    define_method(name) do |*args, &blk|
      this._bond_interceptor(orig_method.bind(self), *args, &blk)
    end

    case visibility
      when :protected
        protected name
      when :private
        private name
    end

    @__last_annotation__ = nil
  end

  def singleton_method_added(name)
    super
    return if @__last_annotation__.nil?

    orig_method = method(name)

    this = self
    this.define_singleton_method(name) do |*args, &blk|
      this._bond_interceptor(orig_method, *args, &blk)
    end

    @__last_annotation__ = nil
  end

  def _bond_interceptor(method, *args, &blk)
    Bond.instance.spy()
    method.call(*args, &blk)
    # TODO
  end

  def spy_point(spy_point_name = nil, require_agent_result = false, excluded_keys = [])
    @__last_annotation__ = {
        :spy_point_name => spy_point_name,
        :require_agent_result => require_agent_result,
        :excluded_keys => excluded_keys
    }
  end
end


# class Test
#   @inst = 4
#
#   def to_json(arg)
#     # puts arg
#     {inst: "4"}.to_json
#   end
# end
#
# bond = Bond.instance
# bond.start_test(nil)
# bond.spy('obsPoint1', my_test: Test.new, my_value: 'blah', my_array: ['blah', 'blah2'], my_hash: {blah_key: 'blah_value', 4 => 'my4'})

