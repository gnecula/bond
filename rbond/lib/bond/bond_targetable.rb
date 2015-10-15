module BondTargetable

  # Keeps track of the arguments to the last spy_point annotation
  @__last_annotation_args = nil

  protected

  # The annotation which indicates that a method should be spied on. Use like:
  #   +spy_point(options...)+
  #   +def method_being_spied_on()+
  #
  # This will automatically spy every call to the method_being_spied_on. By default,
  # all supplied arguments are logged. For normal arguments and keyword arguments,
  # they will be logged as "argument_name: value". For variable arguments,
  # they are logged are "anonymous_parameter{arg_num}: value". If the call to Bond#spy
  # returns anything other than +:agent_result_none+ or +:agent_result_continue+
  # (i.e. through the use of Bond#deploy_agent), the spied method will never be
  # called and the result will be returned in its place. Else, the method is called
  # as normal.
  #
  # spy_point_name:: If supplied, overrides the default spy point name, which is
  #                  +Class.method_name+ for class methods and +Class#method_name+ for
  #                  other methods.
  # require_agent_result:: If true, you *must* supply a return value via Bond#deploy_agent,
  #                        else an error is thrown.
  # excluded_keys:: An array of key/argument names to exclude from the spy.
  #                 Can be useful if you don't care about the value of some argument.
  #                 Can be symbols or strings.
  # spy_return:: If true, spy on the return value. The spy point name will be
  #              +{spy_point_name}.return+ and the key name will be +return+.
  def spy_point(spy_point_name: nil, require_agent_result: false, excluded_keys: [],
                spy_return: false)
    # TODO eventually want spy_groups here as well
    @__last_annotation_args = {
        spy_point_name: spy_point_name,
        require_agent_result: require_agent_result,
        # Allow for a single key or an array of them, and map to_s in case they were passed as symbols
        excluded_keys: [*excluded_keys].map(&:to_s),
        spy_return: spy_return
    }
  end

  public

  # Hook into method addition, if it was preceeded by a call to #spy_point then spy on it.
  def method_added(name)
    super
    return if @__last_annotation_args.nil?
    annotation_args = @__last_annotation_args
    @__last_annotation_args = nil

    orig_method = instance_method(name)

    if private_method_defined?(name)
      visibility = :private
    elsif protected_method_defined?(name)
      visibility = :protected
    else
      visibility = :public
    end

    point_name = annotation_args.delete(:spy_point_name)
    point_name = point_name_from_method(orig_method) if point_name.nil?

    this = self
    define_method(name) do |*args, **kwargs, &blk|
      this._bond_interceptor(orig_method.bind(self), point_name, annotation_args, *args, **kwargs, &blk)
    end

    case visibility
      when :protected
        protected name
      when :private
        private name
    end
  end

  # Hook into singleton method addition, if it was preceeded by a call to #spy_point then spy on it.
  def singleton_method_added(name)
    super
    return if @__last_annotation_args.nil?
    annotation_args = @__last_annotation_args
    @__last_annotation_args = nil

    orig_method = method(name)

    point_name = annotation_args.delete(:spy_point_name)
    point_name = point_name_from_method(orig_method) if point_name.nil?

    this = self
    this.define_singleton_method(name) do |*args, **kwargs, &blk|
      this._bond_interceptor(orig_method, point_name, annotation_args, *args, **kwargs, &blk)
    end

  end

  # Method that gets wrapped around methods being spied on. Should never be called directly.
  #
  # method:: The method that is being spied on
  # spy_point_name:: The name of the spy point
  # options:: Hash of options, which should be any of the arguments to #spy_point
  #           except for +spy_point_name+
  def _bond_interceptor(method, spy_point_name, options, *args, **kwargs, &blk)
    param_list = method.parameters.select { |type, _| type == :opt || type == :req }.map { |_, name| name }

    observation = {}
    anon_param_cnt = 0
    args.zip(param_list) do |value, name|
      if name.nil?
        observation["anonymous_parameter#{anon_param_cnt}".to_sym] = value
      else
        observation[name] = value unless options[:excluded_keys].include?(name.to_s)
      end
    end
    kwargs.each do |name, value|
      observation[name] = value unless options[:excluded_keys].include?(name.to_s)
    end

    ret = Bond.instance.spy(spy_point_name, observation)
    if options[:require_agent_result] && ret == :agent_result_none
      raise "#{spy_point_name} requires mocking but received :agent_result_none"
    end

    if ret == :agent_result_none || ret == :agent_result_continue
      if kwargs.empty?
        ret = method.call(*args, &blk)
      else
        ret = method.call(*args, **kwargs, &blk)
      end
    end
    Bond.instance.spy("#{spy_point_name}.return", return: ret) if options[:spy_return]
    ret
  end

  private

  # Extract the default point name from the method object: +Class.method_name+
  # for class methods and +Class#method_name+ for other methods.
  def point_name_from_method(method)
    method.inspect.to_s.sub(/#<[^:]+: ([^>]+)>/, '\1')
  end
end