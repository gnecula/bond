
require 'bond/spec_helper'
require_relative 'buses'
require 'rspec'

describe Buses do
#  include_context :bond
#  Uncomment the above line to enable using Bond on these tests

  # The data was verified manually w.r.t. http://andrew.hedges.name/experiments/haversine/
  it 'should properly compute distance between points' do
    d1 = Buses.compute_lat_long_distance(
        { lat: 38.898, lon: -77.037 },
        { lat: 38.897, lon: -77.043 }
    )
    expect(d1).to eq(0.330) # 0.531 km

    d2 = Buses.compute_lat_long_distance(
        { lat: 38.898, lon: -97.030 },
        { lat: 38.890, lon: -97.044}
    )
    expect(d2).to eq(0.935) # 1.504 km

    d3 = Buses.compute_lat_long_distance(
        { lat: 38.958, lon: -97.038 },
        { lat: 38.890, lon: -97.044 }
    )
    expect(d3).to eq(4.712) # 7.581 km
  end

end
