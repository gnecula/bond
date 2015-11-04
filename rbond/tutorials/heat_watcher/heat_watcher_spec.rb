#
# Tests using Bond for the heat_watcher.py app
#
# See a full explanation of this example at
# http://necula01.github.io/bond/example_heat.html
#

require 'bond/spec_helper'
require_relative 'heat_watcher'

describe HeatWatcher do
  # Sets up the testing environment to use Bond. Exports bond as a variable
  # to use as e.g. bond.deploy_agent.
  include_context :bond

  # Helper function to setup the time mocks and agents
  def deploy_time_mock
    @time_mocker = TimeMocker.new

    bond.deploy_agent('HeatWatcher#get_current_time',
                      result: lambda { |_| @time_mocker.time })
    bond.deploy_agent('HeatWatcher#sleep',
                      result: lambda { |obs| @time_mocker.sleep(obs[:seconds]) })
  end

  it 'should properly report warnings and switch back to OK status' do
    # A test where the higher-level functions get_temperature and send_alert
    # are mocked out to return specified temperatures and do nothing, respectively.
    deploy_time_mock
    @temp_mocker = TemperatureMocker.new(time_mocker: @time_mocker,
                                         temp_start: 70,
                                         temp_rates: [[0, 0.5], [60, 1.3], [110, 0.1]])

    bond.deploy_agent('HeatWatcher#get_temperature',
                      result: lambda { |_| @temp_mocker.temperature })
    bond.deploy_agent('HeatWatcher#send_alert',
                      result: nil)

    HeatWatcher.new.monitor_loop(@time_mocker.time + 400)
  end

  it 'should properly report critical errors' do
    # A test where make_request is mocked out, and returns different responses
    # depending on the URL that is passed in. This can allow you to test that the
    # parsing logic in get_temperature is working correctly.
    deploy_time_mock
    @temp_mocker = TemperatureMocker.new(time_mocker: @time_mocker,
                                         temp_start: 70,
                                         temp_rates: [[0, 0.5], [60, 2.5], [120, 3], [140, 0.5]])

    bond.deploy_agent('HeatWatcher#make_request',
                      url__contains: 'messages',
                      result: nil)
    bond.deploy_agent('HeatWatcher#make_request',
                      url__contains: 'temperature',
                      result: lambda do |_|
                        [200, "<temperature>#{@temp_mocker.temperature}</temperature>"]
                      end)

    HeatWatcher.new.monitor_loop(@time_mocker.time + 210)
  end
end

# rst_TimeMocker
# A class to mock time
class TimeMocker
  def initialize(current_time = 1445567700)
    # Default 10/23/2015 @ 2:35am (UTC)
    @current_time = current_time
  end

  def time
    @current_time
  end

  def sleep(seconds)
    @current_time += seconds
  end
end

# rst_TemperatureMocker
# A class to mock temperature
class TemperatureMocker

  # @param time_mocker a reference to the current time mocker
  # @param temp_start the starting temperature
  # @param temp_rates a list of pairs (time_since_start,
  #     temperature_increase_rate_per_min) ordered by time_since_start
  def initialize(time_mocker:, temp_start:, temp_rates: [])
    @time_mocker = time_mocker
    @start_time = time_mocker.time
    @last_temp = temp_start       # last temp read
    @last_temp_time = @start_time  # last temp read time
    @temp_rates = temp_rates
  end

  def temperature
    now = @time_mocker.time
    time_since_start = now - @start_time
    # See if we need to advance to the next temperature rate
    if @temp_rates.length > 1 && time_since_start >= @temp_rates[1][0]

      old_rate = @temp_rates.shift[1]

      old_rate_time = @temp_rates[0][0]
      @last_temp += (old_rate_time + @start_time - @last_temp_time) / 60.0 * old_rate
      @last_temp_time = old_rate_time + @start_time
    end

    # The first pair is the one we use to get the rate
    rate = @temp_rates.length > 0 ? @temp_rates[0][1] : 0
    @last_temp += (now - @last_temp_time) / 60.0 * rate
    @last_temp_time = now
    @last_temp
  end
end
