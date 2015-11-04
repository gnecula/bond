require_relative '../bond'

# This file defines a shared context which should be included
# into any RSpec test using Bond via:
#
#     include_context :bond
#
# within your `describe` statement. It makes the `bond` variable
# available for you to use (for e.g. `bond.spy` and `bond.deploy_agent`)
# and automatically initializes Bond to be used in your tests.
#
# You may pass all of the same arguments to the `include_context` statement
# that you can to {Bond#start_test}. For example, to set the test name:
#
#     include_context :bond, test_name: 'my_test_name'
#

shared_context :bond do |**settings|

  let(:bond) { Bond.instance }

  before :each do |example|
    bond.start_test(example, **settings)
    true
  end

  after :each do
    if bond.send(:finish_test) == :bond_fail
      fail('BOND_FAIL. Pass BOND_RECONCILE=[kdiff3|console|accept] environment variable to reconcile the observations.')
    end
  end
end
