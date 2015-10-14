require 'spec_helper'

shared_context :bond do |**settings|

  let(:bond) { Bond.instance }

  before :each do |example|
    bond.start_test(example, **settings)
    true
  end

  after :each do
    bond.finish_test
  end
end

describe Bond do
  include_context :bond,
                  observation_directory: File.join(File.dirname(__FILE__), 'test_observations')

  it 'does something useful' do
    bond.spy('mypoint', my_key: 'some')
    bond.spy('other_stuff', other_key: 'blah')
  end
end
