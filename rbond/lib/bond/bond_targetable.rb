module BondTargetable

  @__last_annotation_args = nil

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
    if point_name.nil?
      point_name = point_name_from_method(orig_method)
    end

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

  protected

  def spy_point(spy_point_name: nil, require_agent_result: false, excluded_keys: [],
                spy_return: false)
    @__last_annotation_args = {
        spy_point_name: spy_point_name,
        require_agent_result: require_agent_result,
        # Allow for a single key or an array of them, and map to_s in case they were passed as symbols
        excluded_keys: [*excluded_keys].map { |key| key.to_s },
        spy_return: spy_return
    }
  end

  private

  def point_name_from_method(method)
    method.inspect.to_s.sub(/#<[^:]+: ([^>]+)>/, '\1')
  end
end