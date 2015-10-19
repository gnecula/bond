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
      # test
      #      all of the different filter types
      #      results
      #      exceptions
      #      overriding previous agents
    end

    # overriding settings using Bond#settings
    # some different start_test parameters

    # finish test: creating output dir,
    # some tests for SpyAgent and SpyAgentFilter - break out into separate files?

end