require 'spec_helper'

describe BondTargetable do
  include_context :bond

  module IncludedModule
    extend BondTargetable

    bond.spy_point
    def annotated_included_method(arg1) end

    def unannotated_included_method(arg1) end
  end

  module ExtendedModule
    extend BondTargetable

    bond.spy_point
    def annotated_extended_method(arg1) end

    def unannotated_extended_method(arg1) end
  end
  
  class BaseClass
    extend BondTargetable
    
    bond.spy_point
    def annotated_base_method(arg1) end

    def unannotated_base_method(arg1) end
  end

  class TestClass < BaseClass
    extend BondTargetable
    include IncludedModule
    extend ExtendedModule

    bond.spy_point_on(:unannotated_base_method)
    bond.spy_point_on(:unannotated_included_method)
    bond.spy_point_on(:unannotated_extended_method)

    bond.spy_point_on(:unannotated_standard_method)

    bond.spy_point
    def annotated_standard_method(arg1, arg2) end

    bond.spy_point
    def self.annotated_class_method(arg1, arg2) end

    bond.spy_point
    def annotated_method_var_args(arg1, arg2, *args) end

    bond.spy_point
    def annotated_method_kw_params_mixed(arg1:, arg2: 'default', arg3: 'default') end

    bond.spy_point
    def annotated_method_kw_params_optional(arg1: 'default', arg2: 'default') end

    bond.spy_point
    def annotated_method_variable_kw_args(arg1:, arg2: 'default', **kwargs) end

    bond.spy_point
    def annotated_method_mixed_params(arg1, arg2, arg3: 'default', arg4: 'foobar') end

    bond.spy_point
    def annotated_method_mixed_variable_params(arg1, *args, arg_n1:, arg_n2: 'default', **kwargs) end

    bond.spy_point(spy_point_name: 'my_name')
    def annotated_method_with_name; end

    bond.spy_point(excluded_keys: :arg1)
    def annotated_method_single_exclude(arg1, arg2) end

    bond.spy_point(excluded_keys: ['arg1', 'arg3'])
    def annotated_method_multiple_exclude(arg1, arg2:, arg3: 'default') end

    bond.spy_point(spy_point_name: 'mock_required', require_agent_result: true)
    def annotated_method_mocking_required(arg1) 'return' end

    bond.spy_point(spy_point_name: 'spy_return', spy_result: true)
    def annotated_method_spy_return(arg1) 'return' end

    bond.spy_point(spy_point_name: 'mock_only', mock_only: true)
    def annotated_method_mock_only; 'return' end

    bond.spy_point
    def annotated_method_with_block(arg1, &blk) yield; end

    def unannotated_standard_method(arg1) end

    def method_calling_protected; annotated_protected_method('value') end

    def method_calling_private; annotated_private_method('value') end

    protected
    spy_point
    def annotated_protected_method(arg1) end

    private
    spy_point
    def annotated_private_method(arg1) end
  end

  let (:tc) { TestClass.new }

  context 'with different argument types' do
    it 'correctly spies on a normal method' do
      tc.annotated_standard_method(1, 2)
    end

    it 'correctly spies on a class method' do
      TestClass.annotated_class_method(1, 2)
    end

    it 'correctly spies on a method with variable arguments' do
      tc.annotated_method_var_args('value1', 'value2')
      tc.annotated_method_var_args('value1', 'value2', 'value3', 'value4')
    end

    it 'correctly spies on a mix of required and optional keyword arguments' do
      tc.annotated_method_kw_params_mixed(arg1: 'value')
      tc.annotated_method_kw_params_mixed(arg1: 'value', arg3: 'new value')
      tc.annotated_method_kw_params_mixed(arg1: 'value', arg3: 'new value3', arg2: 'new value2')
    end

    it 'correctly spies on all optional keyword arguments' do
      tc.annotated_method_kw_params_optional
      tc.annotated_method_kw_params_optional(arg1: 'value1', arg2: 'value2')
    end

    it 'correctly spies on variable keyword arguments' do
      tc.annotated_method_variable_kw_args(arg1: 'value1')
      tc.annotated_method_variable_kw_args(arg1: 'value1', arg2: 'value2')
      tc.annotated_method_variable_kw_args(arg1: 'value1', arg3: 'value3')
      tc.annotated_method_variable_kw_args(arg1: 'value1', arg4: 'value4', arg3: 'value3')
    end

    it 'correctly spies on a mix of positional and named arguments' do
      tc.annotated_method_mixed_params('foo', 'bar')
      tc.annotated_method_mixed_params('foo', 'bar', arg4: 'new value4')
      tc.annotated_method_mixed_params('foo', 'bar', arg3: 'new value3', arg4: 'new value4')
    end

    it 'correctly spies on a mix of positional and named arguments, both variable' do
      tc.annotated_method_mixed_variable_params('value1', arg_n1: 'value_n1')
      tc.annotated_method_mixed_variable_params('value1', arg_n1: 'value_n1', arg_n2: 'value_n2')
      tc.annotated_method_mixed_variable_params('value1', 'value2', 'value3', arg_n1: 'value_n1', arg_n2: 'value_n2')
      tc.annotated_method_mixed_variable_params('value1', 'value2', 'value3', arg_n1: 'value_n1')
      tc.annotated_method_mixed_variable_params('value1', 'value2', 'value3', arg_n1: 'value_n1', arg_n3: 'value_n3')
    end
  end

  context 'with different spy_point parameters' do
    it 'correctly changes the spy point name if it is specified' do
      tc.annotated_method_with_name
    end

    it 'correctly ignores excluded keys' do
      tc.annotated_method_single_exclude('value1', 'value2')
      tc.annotated_method_multiple_exclude('value1', arg2: 'value2', arg3: 'value3')
    end

    it 'correctly errors when mocking is required and none is specified' do
      begin
        tc.annotated_method_mocking_required(2)
        fail
      rescue RuntimeError
      end
    end

    it 'correctly mocks when one is specified' do
      bond.deploy_agent('mock_required', result: 'new return')
      ret = tc.annotated_method_mocking_required(5)
      bond.spy('return_value', return: ret)
    end

    it 'correctly spies the return value when not mocking' do
      tc.annotated_method_spy_return('arg_value')
    end

    it 'correctly spies the return value when mocking' do
      bond.deploy_agent('spy_return', result: 'new return')
      tc.annotated_method_spy_return('arg_value')
    end

    it 'correctly respects mock_only' do
      bond.spy('unmocked_return', val: tc.annotated_method_mock_only)

      bond.deploy_agent('mock_only', result: 'mocked return')
      bond.spy('mocked_return', val: tc.annotated_method_mock_only)

      bond.deploy_agent('mock_only', skip_save_observation: false, result: 'mocked return')
      bond.spy('mocked_return', val: tc.annotated_method_mock_only)
    end
  end

  context 'with inheritance and mixins' do
    it 'correctly applies spy_point_on to methods in base classes' do
      tc.unannotated_base_method('foo')
    end

    it 'correctly spies on annotated methods in base classes' do
      tc.annotated_base_method('foo')
    end

    it 'correctly applies spy_point_on to methods in included modules' do
      tc.unannotated_included_method('foo')
    end

    it 'correctly spies on annotated methods in included modules' do
      tc.annotated_included_method('foo')
    end

    it 'correctly applies spy_point_on to methods in extended modules' do
      TestClass.unannotated_extended_method('foo')
    end

    it 'correctly spies on annotated methods in extended modules' do
      TestClass.annotated_extended_method('foo')
    end
  end
  
  it 'correctly spies protected methods' do
    tc.method_calling_protected
  end

  it 'correctly spies private methods' do
    tc.method_calling_private
  end

  it 'correctly differentiates object instances if register_instance has been called' do
    tc1 = TestClass.new
    tc2 = TestClass.new

    bond.register_instance(tc1, 'TestClass1')
    bond.register_instance(tc2, 'TestClass2')

    tc1.annotated_standard_method('foo', '1')
    tc2.annotated_standard_method('foo', '2')
    tc.annotated_standard_method('foo', 'bar')
  end

  it 'respects spy_point_on' do
    tc.unannotated_standard_method('foo')
  end
  
  it 'correctly continues to the method when agent_result_continue is returned' do
    bond.deploy_agent('mock_required', result: :agent_result_continue)
    ret = tc.annotated_method_mocking_required('value1')
    bond.spy('return value', ret: ret)
  end

  it 'correctly continues to the method when agent_result_none is returned' do
    bond.deploy_agent('spy_return', result: :agent_result_none)
    ret = tc.annotated_method_spy_return('value')
    bond.spy('return value', ret: ret)
  end

  it 'correctly passes through blocks' do
    ret = tc.annotated_method_with_block('foo') { 'value' }
    bond.spy('return value', ret: ret)
  end

  it 'correctly yields' do
    bond.deploy_agent('TestClass#annotated_method_with_block', yield: 'yielded_val')
    tc.annotated_method_with_block('foo') { |val| bond.spy('inside block', yielded_val: val) }
  end

  it 'correctly returns nil (and mocks) when an agent returns nil' do
    arr = [0]
    bond.deploy_agent('TestClass#annotated_method_with_block', result: nil)
    ret = tc.annotated_method_with_block('foo') { arr[0] = 1 }
    bond.spy('return value', ret: ret, arr_val: arr[0])
  end

  context 'with modules' do
    module TestModule
      extend BondTargetable

      bond.spy_point
      def self.annotated_class_method(arg1, arg2) end

      bond.spy_point
      def annotated_standard_method(arg1) end

    end

    class TestClassWithMixin; include TestModule; end

    it 'correctly spies on module methods' do
      TestModule.annotated_class_method('value1', 'value2')
    end

    it 'correctly spies on included module methods' do
      TestClassWithMixin.new.annotated_standard_method('value')
    end

  end
end
