require 'spec_helper'

describe Bond do
  include_context :bond

  context 'without any agents' do
    it 'should correctly log some normal arguments with a spy point name' do
      bond.spy('my_point_name', spy_key: 1, spy_key_2: 'string value', spy_key_3: 1.4)
      bond.spy('second_point', spy_key: 'string value 2')
    end

    it 'should correctly log some normal arguments without a spy point name' do
      bond.spy(spy_key: 1, spy_key_2: 'string value', spy_key_3: 1.4)
      bond.spy(spy_key: 'string value 2')
    end

    it 'should correctly log nested hashes and arrays with hash sorting' do
      bond.spy(key1: {h1key1: %w(hello world), h1key2: 'hi'},
               key2: [{key2: 'foo', key3: 'bar', key1: 'baz'}, 'array3', 'array2'])
      bond.spy('point_name', key3: {key2: 'foo', key1: 'bar', key3: {hkey: 'hello world'}},
               key1: 'value 1')
    end
  end

  context 'with agents' do
    it 'should work with multiple agents for different spy points' do
      bond.deploy_agent('my_point_1', result: 'mock result')
      bond.deploy_agent('my_point_2', result: 'mock result 2')

      bond.spy('point_1_return', return_value: bond.spy('my_point_1'))
      bond.spy('point_2_return', return_value: bond.spy('my_point_2'))
      bond.spy('point_1_return_2', return_value: bond.spy('my_point_1'))
    end

    context 'with filters' do
      it 'should respect single key/value filters of all types' do
        bond.deploy_agent('my_point', obs_name: 'value', result: 'mocked')
        bond.spy(result: bond.spy('my_point', obs_name: 'value'))
        bond.spy(result: bond.spy('my_point', obs_name: 'not value'))

        bond.deploy_agent('my_point_eq', obs_name__eq: 'value', result: 'mocked')
        bond.spy(result: bond.spy('my_point_eq', obs_name: 'value'))
        bond.spy(result: bond.spy('my_point_eq', obs_name: 'not value'))

        bond.deploy_agent('my_point_exact', obs_name__exact: 'value', result: 'mocked')
        bond.spy(result: bond.spy('my_point_exact', obs_name: 'value'))
        bond.spy(result: bond.spy('my_point_exact', obs_name: 'not value'))

        bond.deploy_agent('my_point_startswith', obs_name__startswith: 'value', result: 'mocked')
        bond.spy(result: bond.spy('my_point_startswith', obs_name: 'value'))
        bond.spy(result: bond.spy('my_point_startswith', obs_name: 'not value'))
        bond.spy(result: bond.spy('my_point_startswith', obs_name: 'value not'))

        bond.deploy_agent('my_point_endswith', obs_name__endswith: 'value', result: 'mocked')
        bond.spy(result: bond.spy('my_point_endswith', obs_name: 'value'))
        bond.spy(result: bond.spy('my_point_endswith', obs_name: 'not value'))
        bond.spy(result: bond.spy('my_point_endswith', obs_name: 'value not'))

        bond.deploy_agent('my_point_contains', obs_name__contains: 'value', result: 'mocked')
        bond.spy(result: bond.spy('my_point_contains', obs_name: 'value'))
        bond.spy(result: bond.spy('my_point_contains', obs_name: 'not value'))
        bond.spy(result: bond.spy('my_point_contains', obs_name: 'value not'))
        bond.spy(result: bond.spy('my_point_contains', obs_name: 'not value not'))
        bond.spy(result: bond.spy('my_point_contains', obs_name: 'foobar'))
      end

      it 'should respect function filters' do
        bond.deploy_agent('my_point', filter: lambda { |obs| obs[:my_key].even? }, result: 'mocked')
        bond.spy(result: bond.spy('my_point', my_key: 5))
        bond.spy(result: bond.spy('my_point', my_key: 10))
      end

      it 'should override old agents with newer agents unless they are filtered out' do
        bond.deploy_agent('my_point', result: 'first mock')
        bond.deploy_agent('my_point', result: 'second mock')
        bond.deploy_agent('my_point', obs_name: 'foobar', result: 'third mock')

        bond.spy(result: bond.spy('my_point'))
        bond.spy(result: bond.spy('my_point', obs_name: 'foobar'))
        bond.spy(result: bond.spy('my_point', obs_name: 'baz'))
      end

      it 'should respect combinations of filters' do
        bond.deploy_agent('my_point', result: 'mocked',
                          filter: lambda { |obs| obs.has_key?(:req_key) },
                          req_key2: 'value', req_key3__contains: 'value')
        bond.spy(result: bond.spy('my_point', req_key: 'foo', req_key2: 'bar', req_key3: 'value'))
        bond.spy(result: bond.spy('my_point', req_key: 'foo', req_key2: 'value', req_key3: 'foovaluebar'))
        bond.spy(result: bond.spy('my_point', req_key: 'foo', req_key2: 'value', req_key3: 'lacking'))
        bond.spy(result: bond.spy('my_point', req_key2: 'value', req_key3: 'value'))
      end
    end

    it 'should call the function passed as result if it is callable' do
      mocked_value = 1
      bond.deploy_agent('my_point', result: lambda { |_| mocked_value += 1 }, obs_name: 'foo')
      bond.spy(result: bond.spy('my_point', obs_name: 'foo'))
      bond.spy(result: bond.spy('my_point', obs_name: 'bar'))
      bond.spy(result: bond.spy('my_point', obs_name: 'foo'))
    end

    it 'should yield values passed to yield' do
      bond.deploy_agent('my_point', yield: 'yielded_value')
      bond.spy('my_point', obs_name: 'foo') { |val| bond.spy('inside block', yielded_val: val) }
    end

    it 'should call the object passed to yield if it is a function' do
      bond.deploy_agent('my_point', yield: lambda { |obs| obs[:obs_name] })
      bond.spy('my_point', obs_name: 'foo') { |val| bond.spy('inside block', yielded_val: val) }
    end

    it 'should throw an exception if specified by agent' do
      bond.deploy_agent('my_point', exception: TypeError.new('TypeError exception!'))
      begin
        bond.spy('my_point')
      rescue TypeError => e
        bond.spy('rescue_point', except: e.to_s)
      end
    end

    it 'should throw the result of the value passed to exception if callable' do
      bond.deploy_agent('my_point', exception: lambda { |obs| ArgumentError.new(obs[:key]) })
      begin
        bond.spy('my_point', key: 'Value passed to exception')
      rescue ArgumentError => e
        bond.spy('rescue_point', except: e.to_s)
      end
    end

    it 'should correctly call a single doer if filter criteria are met' do
      bond.deploy_agent('my_point', my_key__contains: '',
                        do: lambda { |obs| bond.spy('internal', val: obs[:my_key]) })
      bond.spy('my_point', my_key: 'value')
    end

    it 'should correctly call multiple doers' do
      side_effect_value = 0
      doers = [
          lambda { |obs| bond.spy('internal', val: obs[:my_key]) },
          lambda { |obs| side_effect_value += obs[:my_key] }
      ]
      bond.deploy_agent('my_point', do: doers)
      bond.spy('my_point', my_key: 10)
      bond.spy(side_effect_value: side_effect_value)
    end

    it 'should not call doers of overriden agents' do
      bond.deploy_agent('my_point', do: lambda { |_| bond.spy('bad_agent') })
      bond.deploy_agent('my_point', do: lambda { |_| bond.spy('valid_agent') })
      bond.spy('my_point')
    end

    it 'should call doers before returning result' do
      bond.deploy_agent('my_point', do: lambda { |_| bond.spy('internal_doer') },
                        result: lambda { |_| bond.spy('internal_result'); 'mocked' })
      bond.spy(result: bond.spy('my_point'))
    end

    it 'should apply the relevant formatter without modifying original objects' do
      my_dict = { nest1: 'nest_val1', nest2: 'nest_val2' }
      def format_func(obs)
        obs[:key1] = 'mock1'
        obs[:key2][:nest2] = 'nest_mock2'
      end
      bond.deploy_agent('my_point', formatter: method(:format_func))
      bond.spy('my_point', key1: 'value1', key2: my_dict)
      bond.spy('unmodified_object', my_dict: my_dict)
    end

    it 'should skip saving observations when specified' do
      bond.spy_internal('skipped_point', skip_save_observation = true, key: 'value')[:result]

      bond.deploy_agent('skipped_point', result: 'Mock Value')
      ret = bond.spy_internal('skipped_point', skip_save_observation = true, key: 'value')[:result]
      bond.spy('skipped_return_value', val: ret)

      bond.deploy_agent('normal_point', skip_save_observation: false, result: 'Mock Value')
      ret = bond.spy_internal('normal_point', skip_save_observation = true, key: 'value')[:result]
      bond.spy('not_skipped_return_value', val: ret)

      bond.deploy_agent('skipped_point', skip_save_observation: true, result: 'Mock Value')
      ret = bond.spy_internal('skipped_point', key: 'value')[:result]
      bond.spy('skipped_return_value', val: ret)
    end

  end

  # TODO ETK more testing
  # overriding settings using Bond#settings
  # some different start_test parameters

  # finish test: creating output dir,
  # some tests for SpyAgent and SpyAgentFilter - break out into separate files?

end