shared_context :bond do |**settings|

  let(:bond) { Bond.instance }

  before :each do |example|
    bond.start_test(example, **settings)
    true
  end

  after :each do
    if bond.finish_test == :fail
      fail('BOND_FAIL. Pass BOND_MERGE=[kdiff3|console|accept] environment variable to merge the observations.')
    end
  end
end
