require_relative 'utils'

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
  #         - `yield: x` - the call to bond.spy yields the given value. If `x` is a function
  #            it is invokved on the observe argument dictionary to compute the value to yield.
  #            Uses the observation before formatting.
  #
  #     - Keys that control how the observation is saved. This is processed after all
  #       the above functions.
  #
  #         - `formatter: func` - If specified, a function that is given the observation and
  #           can update it in place. The formatted observation is what gets serialized and saved.
  #         - `skip_save_observation: Boolean` - If specified, determines whether or not the
  #           observation will be saved after all of the agent's other actions have been processed.
  #           Useful for hiding observations of a spy point that e.g. is sometimes useful but in some
  #           tests is irrelevant and clutters up the observations. This value, if present, will override
  #           the `skip_save_observation` parameter of {Bond#spy} and the `mock_only` parameter of
  #           {BondTargetable#spy_point}.
  #
  def initialize(**opts)
    @result_spec = :agent_result_none
    @yield_spec = nil
    @exception_spec = nil
    @formatter_spec = nil
    @doers = []
    @filters = []
    @skip_save_observation = nil

    opts.each do |k, v|
      case k.to_s # Convert to string in case it was passed as a symbol
        when 'result'
          @result_spec = v
        when 'yield'
          @yield_spec = v
        when 'exception'
          @exception_spec = v
        when 'formatter'
          @formatter_spec = v
        when 'do'
          @doers = [*v]
        when 'skip_save_observation'
          @skip_save_observation = v
        else # Must be a filter
          @filters <<= SpyAgentFilter.new(k.to_s, v)
      end
    end
  end

  attr_reader :skip_save_observation

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
    unless @yield_spec.nil?
      return { result: @yield_spec.respond_to?(:call) ? @yield_spec.call(observation) : @yield_spec,
               should_yield: true, record_replay: false }
    end

    if !@result_spec.nil? && @result_spec.respond_to?(:call)
      ret = @result_spec.call(observation)
    else
      ret =  @result_spec
    end
    { result: ret, should_yield: false, record_replay: false }
  end

  # Formats the observation according to the `formatter` option;
  # if none is specified, do nothing. Modifies the observation
  # in-place.
  def format(observation)
    unless @formatter_spec.nil?
      @formatter_spec.call(observation)
    end
  end
end

# Represents an agent deployed by {Bond#deploy_record_replay_agent}.
# Facilitates the use of a record-replay paradigm for recording the previous
# return value of a spy point ("record") and returning that same value in
# subsequent executions ("replay"). If the agent is deployed in replay mode
# and no saved value is available, an error will be thrown. Should only be
# deployed on `spy_point` annotations, not on an explicit call to {Bond#spy}.
# See {Bond#deploy_record_replay_agent} for more information.
# @api private
class RecordReplaySpyAgent < SpyAgent
  # Initialize, setting the options for this RecordReplaySpyAgent.
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
  #         - `record_mode: Boolean` - If true, this record-replay point will be in
  #           record mode. Upon calling the spy point, the value will be saved for use
  #           in future executions. Otherwise, this point will be in replay mode.
  #           This can also be specified for the entire test (e.g., to easily switch all
  #           RecordReplaySpyAgents simultaneously) using `bond.settings(record_mode: true)`.
  #           A `record_mode` parameter on an individual agent will override the test-wide setting.
  #         - `order_dependent: Boolean` - If true, this record-replay point will differentiate
  #           between multiple calls to the same method with the same arguments, returning
  #           different order-dependent replay values. Otherwise, once a replay value
  #           is recorded for a set of arguments, that same value is used every time that
  #           set of arguments is seen.
  #
  def initialize(**opts)
    @current_call_args = nil
    @record_mode = false
    @record_mode_temp = false # If true, @record_mode is only true for the duration of this
                              # call to spy point
    @order_dependent = false
    @args_seen = Hash.new(0) # Map of argument observation -> number of times seen
    @doers = []
    @filters = []

    opts.each do |k, v|
      case k.to_s # Convert to string in case it was passed as a symbol
        when 'record_mode'
          @record_mode = v
        when 'order_dependent'
          @order_dependent = v
        when 'do'
          @doers = [*v]
        else # Must be a filter
          @filters <<= SpyAgentFilter.new(k.to_s, v)
      end
    end
  end

  # Do not allowing skipping saving observation in record-replay mode since that is
  # part of how the replay value gets saved.
  def skip_save_observation
    false
  end

  # Retrieve the result for this agent; when in replay mode, looks for the correct
  # value and returns that, throwing an error if none is found.
  # In record mode, this is called *both* before and after the real method is
  # called; once before to record the call arguments, and once after to record
  # the result. The second time, the result is displayed to the user, who can
  # accept, edit, or reject it before saving for future replay.
  def result(observation)
    if @current_call_args.nil?
      @current_call_args = observation
      before_result(observation)
    else
      ret = after_result(observation, @current_call_args)
      @current_call_args = nil
      ret
    end
  end

  # The portion of the `result` which is performed *after* the real method is
  # called.
  def after_result(observation, inital_call_arg_obs)
    observation[:__replay_result__] = ''
    ret = if @record_mode and (@order_dependent or not @args_seen.has_key?(inital_call_arg_obs))
      if @record_mode_temp # Clear @record_mode if it was only set temporarily
        @record_mode_temp = false
        @record_mode = false
      end
      current_result = observation[:result]
      observation[:result] = edit_confirm_result(inital_call_arg_obs, current_result)
      Bond.instance.add_replay_value(inital_call_arg_obs, observation[:result])
      { result: observation[:result], should_yield: false, record_replay: true }
    else
      { result: :agent_result_none, should_yield: false, record_replay: true }
    end
    @args_seen[inital_call_arg_obs] = @args_seen[inital_call_arg_obs] + 1
    ret
  end

  def get_index(observation)
    return 0 unless @order_dependent
    @args_seen[observation]
  end

  # The portion of the `result` which is performed *before* the real method is
  # called.
  def before_result(observation)
    observation[:__record_args__] = ''
    observation[:__replay_index__] = get_index(observation)
    val = Bond.instance.get_replay_value(observation)
    observation.delete(:__replay_index__)
    if val == :agent_result_none and !@record_mode
      case Bond.instance.reconcile_mode
        when 'abort'
          raise RuntimeError, "RecordReplaySpyAgent could not find a replay value to return for: #{observation}"
        when 'accept'
          @record_mode = true
          @record_mode_temp = true
        when 'console', 'dialog'
          args = observation.clone
          args.delete(:__spy_point__)
          args.delete(:__record_args__)
          before_prompt = "For test #{Bond.instance.test_name}:\n" +
              "Attempting to make a request through #{observation[:__spy_point__]} for which " +
              "no replay value is currently available. These are the arguments it was called with:"
          after_prompt = 'Do you wish to allow this request to proceed?'
          response = Utils.get_user_input(before_prompt, after_prompt, args, %w'accept deny', %w'a d')
          if response == 'accept'
            @record_mode = true
            @record_mode_temp = true
          else
            raise RuntimeError, "RecordReplaySpyAgent could not find a replay value to return for: #{observation}"
          end
      end
    end
    if @record_mode and (@order_dependent or not @args_seen.has_key?(observation))
      { result: :agent_result_continue, should_yield: false, record_replay: true }
    else
      { result: val, should_yield: false, record_replay: true }
    end
  end

  # Given the observation dictionary from the initial call to the method
  # (initial_call_arg_obs) and the result returned from the method (current_result),
  # show this information to the user and ask them to accept or reject the
  # result. They are also given the option of editing the result before accepting.
  def edit_confirm_result(initial_call_arg_obs, current_result)
    if current_result.is_a?(String)
      content = current_result
      use_json = false
    else
      content = Utils.observation_to_json(current_result)
      use_json = true
    end
    args = initial_call_arg_obs.clone
    args.delete(:__spy_point__)
    args.delete(:__record_args__)
    args = Utils.observation_to_json(args)
    before_prompt = "For test #{Bond.instance.test_name}:\n" +
        "Below is the current result#{use_json ? ' (JSON-serialized)' : ''} returned by " +
        "#{initial_call_arg_obs[:__spy_point__]} when called with arguments as follows:\n\n#{args}\n"
    after_prompt = 'Do you wish to save this result for future replay? You can also edit it before accepting.'
    opt, new_content = Utils.get_user_input_with_edits(before_prompt, after_prompt, content, %w'Accept Reject')
    if opt == 'Accept'
      if new_content == content
        current_result
      elsif use_json
        Utils.observation_from_json(new_content)
      else
        new_content
      end
    else
      raise RuntimeError,
            "Unable to continue because the recorded value was not accepted for #{initial_call_arg_obs[:__spy_point__]}"
    end
  end

  # Checks if this agent should process this observation
  # @return true iff this agent should process this observation
  def process?(observation)
    super(@current_call_args.nil? ? observation : @current_call_args.merge(observation))
  end

  # Override; do nothing.
  def format(observation) end

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
          @filter_func = lambda { |val| val.to_s.start_with?(filter_value) }
        when 'endswith'
          @filter_func = lambda { |val| val.to_s.end_with?(filter_value) }
        when 'contains'
          @filter_func = lambda { |val| val.to_s.include?(filter_value) }
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
