shared_context :bond do |**settings|

  let(:bond) { Bond.instance }

  before :each do |example|
    bond.start_test(example, **settings)
    true
  end

  after :each do
    if bond._finish_test == :fail
      fail('BOND_FAIL. Pass BOND_RECONCILE=[kdiff3|console|accept] environment variable to reconcile the observations.')
    end
  end
end
