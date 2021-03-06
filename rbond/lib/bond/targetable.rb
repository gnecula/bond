# Module which should be `extend`ed into any class or module which you would like
# to spy on, either via {Bond#spy} or via a {#spy_point}. Within that class/module,
# the method {#bond} is available to access Bond, i.e. `bond.spy` and `bond.spy_point`.
#
# For example, if you would like to spy on MyClass, you would do this:
#
#     class MyClass
#       extend BondTargetable
#
#       bond.spy_point(spy_point_name: 'my_name')
#       def method_to_spy_on
#         ...
#         bond.spy(intermediate_state: state_variable)
#         ...
#       end
#     end
#
module BondTargetable

  protected

  # Keeps track of the arguments to the last spy_point annotation
  @__last_annotation_args = nil
  # Keeps track of the spy points defined by spy_point_on
  @__annotation_args = {}

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
  # they are logged as `anonymous_parameter{arg_num}: value` (e.g. `anonymous_parameter0: 'foo'`).
  # If the call to {Bond#spy} returns anything other than `:agent_result_none` or
  # `:agent_result_continue` (i.e. through the use of {Bond#deploy_agent}), the spied
  # method will never be called and the result will be returned in its place. Else, the
  # method is called as normal.
  #
  # @param spy_point_name [String] If supplied, overrides the default spy point name, which is
  #     `Class.method_name` for class methods and `Class#method_name` for other methods.
  # @param require_agent_result [Boolean] If true, you *must* supply a return value via
  #     {Bond#deploy_agent} during testing, else an error is thrown.
  # @param excluded_keys [String, Symbol, Array<String, Symbol>] An array of key/argument
  #     names to exclude from the spy. Can be useful if you don't care about the value of some argument.
  # @param spy_result [Boolean] If true, spy on the return value. The spy point name will be
  #     `{spy_point_name}.result` and the key name will be `result`.
  # @param mock_only [Boolean] If true, don't record calls to this method as observations.
  #     This allows for the ability to mock a method without observing all of its calls, useful
  #     if mocking e.g. a small utility method whose call order is unimportant. This can be
  #     overridden by using the `skip_save_observation` parameter when deploying an agent
  #     ({Bond#deploy_agent}).
  # @api public
  def spy_point(spy_point_name: nil, require_agent_result: false, excluded_keys: [],
                spy_result: false, mock_only: false)
    @__last_annotation_args = {
        spy_point_name: spy_point_name,
        require_agent_result: require_agent_result,
        # Allow for a single key or an array of them, and map to_s in case they were passed as symbols
        excluded_keys: [*excluded_keys].map(&:to_s),
        spy_result: spy_result,
        mock_only: mock_only
    }
  end

  # Acts identically to {#spy_point}, except that you specify which method to place a spy
  # point on using the `method_name` parameter (i.e., the position of the call is not relevant).
  # This is especially useful for spying on members of a base/mixed-in class/module, since it may be
  # difficult to place annotations on those methods (especially if you are inheriting from a library).
  #
  # @param method_name [Symbol] Name of the method on which to place a spy point.
  # @param (see BondTargetable#spy_point)
  # @api public
  def spy_point_on(method_name, spy_point_name: nil, require_agent_result: false, excluded_keys: [],
                   spy_result: false, mock_only: false)
    annotation_args = {
        spy_point_name: spy_point_name,
        require_agent_result: require_agent_result,
        # Allow for a single key or an array of them, and map to_s in case they were passed as symbols
        excluded_keys: [*excluded_keys].map(&:to_s),
        spy_result: spy_result,
        mock_only: mock_only
    }
    if self.method_defined?(method_name) || self.respond_to?(method_name)
      wrap_instance_method(method_name, annotation_args) if self.method_defined?(method_name)
      wrap_singleton_method(method_name, annotation_args) if self.respond_to?(method_name)
    else
      @__annotation_args = {} if @__annotation_args.nil?
      @__annotation_args[method_name.to_sym] = annotation_args
    end
  end

  # Simple method to access Bond from within a BondTargetable class/module.
  # @api public
  def bond
    PassthroughClass.new(self)
  end

  public

  # Hook into method addition, if it was preceded by a call to #spy_point then spy on it.
  # @param name Name of the method being added
  # @private
  def method_added(name)
    super
    return if @__last_annotation_args.nil? && (@__annotation_args.nil? || !@__annotation_args.has_key?(name))
    annotation_args = @__last_annotation_args || @__annotation_args.delete(name)
    @__last_annotation_args = nil

    wrap_instance_method(name, annotation_args)
  end

  # Hook into singleton method addition, if it was preceded by a call to #spy_point then spy on it.
  # @param name Name of the method being added
  # @private
  def singleton_method_added(name)
    super
    return if @__last_annotation_args.nil?
    annotation_args = @__last_annotation_args
    @__last_annotation_args = nil

    wrap_singleton_method(name, annotation_args)
  end


  private

  # Wrap the instance method with name `name` in the `bond_interceptor` method,
  # enabling it to be spied on.
  # @param name [Symbol] The name of the method to wrap.
  # @param annotation_args [Hash] Hash of the arguments provided to {#spy_point}
  #     or {#spy_point_on} which instructed this method to be wrapped.
  def wrap_instance_method(name, annotation_args)
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

  # Wrap the singleton method with name `name` in the `bond_interceptor` method,
  # enabling it to be spied on.
  # @param (see BondTargetable#wrap_instance_method)
  def wrap_singleton_method(name, annotation_args)
    orig_method = method(name)

    point_name = annotation_args.delete(:spy_point_name)
    point_name = point_name_from_method(orig_method) if point_name.nil?

    this = self
    this.define_singleton_method(name) do |*args, **kwargs, &blk|
      this.send(:bond_interceptor, orig_method, point_name, annotation_args, *args, **kwargs, &blk)
    end
  end

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
        anon_param_cnt += 1
      else
        observation[name] = value unless options[:excluded_keys].include?(name.to_s)
      end
    end
    kwargs.each do |name, value|
      observation[name] = value unless options[:excluded_keys].include?(name.to_s)
    end

    instance_name = Bond.instance.instance_name(method.receiver)
    unless instance_name.nil?
      observation[:__instance_name__] = instance_name
    end

    spy_ret = Bond.instance.spy_internal(spy_point_name, options[:mock_only], **observation)
    ret = spy_ret[:result]

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
    if options[:spy_result] or spy_ret[:record_replay]
      ret2 = Bond.instance.spy_internal("#{spy_point_name}.result", result: ret)[:result]
      if spy_ret[:record_replay] and ret2 != :agent_result_none
        return ret2 # Support for changing the returned value during recording
      end
    end

    yield ret if spy_ret[:should_yield]
    ret
  end

  # Extract the default point name from the method object, which is `Class.method_name`
  # for class methods and `Class#method_name` for other methods (and analogously for modules).
  # If a method was inherited or mixed-in from another class/module, the originally defining
  # entity is the one whose name is used.
  def point_name_from_method(method)
    match = /#<[^:]+: ([^.#(]+)(?:\(([^)]+)\))?(\.|#)([^>]+)>/.match(method.inspect.to_s)
    class_name = match[2].nil? ? match[1] : match[2]
    "#{class_name}#{match[3]}#{match[4]}"
  end

  # A class that acts as if it was Bond by passing through all method calls
  # *except* for `spy_point`, which it sends back to whatever object was passed
  # in upon initialization (which should be something that `extend`s BondTargetable).
  # This is used since calls to `spy_point` should be directed to BondTargetable
  # and other calls should be directed to Bond, but we want this to happen
  # transparently to the end-user.
  # @private
  class PassthroughClass
    def initialize(parent)
      @parent = parent
    end

    def spy_point(**kwargs)
      @parent.send(:spy_point, **kwargs)
    end

    def spy_point_on(method_name, **kwargs)
      @parent.send(:spy_point_on, method_name, **kwargs)
    end

    def method_missing(meth, *args)
      Bond.instance.send(meth, *args)
    end
  end

  # A module to export the `bond` method as an instance method in addition to
  # a class method (which it will already appear as due to the `extend` statement)
  # @private
  module BondTargetableInstanceMethods
    protected
    def bond
      PassthroughClass.new(self)
    end
  end

  public
  # Used to mix in BondTargetableInstanceMethods, allowing for `bond` to appear as both
  # an instance method and a class method.
  # @private
  def self.extended(base)
    base.include(BondTargetableInstanceMethods)
  end
end