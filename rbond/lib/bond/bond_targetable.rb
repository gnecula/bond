module BondTargetable
  def self.included(base)
    base.extend(BondTargetableClassMethods)
  end

  protected

  def bond
    PassthroughClass.new(self)
  end

  private

  class PassthroughClass
    def initialize(parent)
      @parent = parent
    end

    def spy_point(**kwargs)
      @parent.send(:spy_point, **kwargs)
    end

    def method_missing(meth, *args)
      Bond.instance.send(meth, *args)
    end
  end

  module BondTargetableClassMethods

    protected

    def bond
      PassthroughClass.new(self)
    end

    # Keeps track of the arguments to the last spy_point annotation
    @__last_annotation_args = nil

    # The annotation which indicates that a method should be spied on. Use like:
    #
    #     bond.spy_point(options...)
    #     def method_being_spied_on()
    #
    # This is safe to use in production code; it will have effects only if the function
    # {Bond#start_test} has been called to initialize Bond.
    # This will automatically spy every call to the `method_being_spied_on`. By default,
    # all supplied arguments are logged. For normal arguments and keyword arguments,
    # they will be logged as `argument_name: value`. For variable arguments,
    # they are logged are `anonymous_parameter{arg_num}: value`. If the call to {Bond#spy}
    # returns anything other than `:agent_result_none` or `:agent_result_continue`
    # (i.e. through the use of {Bond#deploy_agent}), the spied method will never be
    # called and the result will be returned in its place. Else, the method is called
    # as normal.
    #
    # @param spy_point_name [String] If supplied, overrides the default spy point name, which is
    #     `Class.method_name` for class methods and `Class#method_name` for other methods.
    # @param require_agent_result [Boolean] If true, you *must* supply a return value via
    #     {Bond#deploy_agent}, else an error is thrown.
    # @param excluded_keys [String, Symbol, Array<String, Symbol>] An array of key/argument
    #     names to exclude from the spy. Can be useful if you don't care about the value of some argument.
    # @param spy_result [Boolean] If true, spy on the return value. The spy point name will be
    #     `{spy_point_name}.result` and the key name will be `result`.
    # @api public
    def spy_point(spy_point_name: nil, require_agent_result: false, excluded_keys: [],
                  spy_result: false)
      @__last_annotation_args = {
          spy_point_name: spy_point_name,
          require_agent_result: require_agent_result,
          # Allow for a single key or an array of them, and map to_s in case they were passed as symbols
          excluded_keys: [*excluded_keys].map(&:to_s),
          spy_result: spy_result
      }
    end

    public

    # Hook into method addition, if it was preceded by a call to #spy_point then spy on it.
    # @param name Name of the method being added
    # @private
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
        this.send(:bond_interceptor, orig_method.bind(self), point_name, annotation_args, *args, **kwargs, &blk)
      end

      case visibility
        when :protected
          protected name
        when :private
          private name
      end
    end

    # Hook into singleton method addition, if it was preceeded by a call to #spy_point then spy on it.
    # @param name Name of the method being added
    # @private
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
        this.send(:bond_interceptor, orig_method, point_name, annotation_args, *args, **kwargs, &blk)
      end

    end

    private

    # Method that gets wrapped around methods being spied on. Should never be called directly.
    #
    # @param method [Method] The method that is being spied on
    # @param spy_point_name [String] The name of the spy point
    # @param options [Hash] Hash of options, which should be any of the arguments to {#spy_point}
    #                except for `spy_point_name`
    def bond_interceptor(method, spy_point_name, options, *args, **kwargs, &blk)
      return kwargs.empty? ? method.call(*args, &blk) : method.call(*args, **kwargs, &blk) unless Bond.instance.active?
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
      Bond.instance.spy("#{spy_point_name}.result", result: ret) if options[:spy_result]
      ret
    end

    # Extract the default point name from the method object, which is `Class.method_name`
    # for class methods and `Class#method_name` for other methods.
    def point_name_from_method(method)
      method.inspect.to_s.sub(/#<[^:]+: ([^>]+)>/, '\1')
    end
  end
end