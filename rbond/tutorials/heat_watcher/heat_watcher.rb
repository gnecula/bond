#
# A simple demonstration of using Bond for spying and mocking
# an application for monitoring temperature and sending alerts
#
# See a full explanation of this example at
# http://necula01.github.io/bond/example_heat.html
#
# rst_Start

require 'bond'

# Monitor temperature rise over time.
# See description in the Bond documentation.
class HeatWatcher
  # Marks this class as being able to be spied on. Exports `bond` method
  # to use as e.g. bond.spy and bond.spy_point.
  extend BondTargetable

  def initialize
    @last_temp = nil # The last temp measurement
    @last_time = nil  # The time when we took the last measurement
    @last_alert_state = 'Ok'   # Ok, Warning, Critical
    @last_alert_time = -Float::INFINITY # The time when we sent the last alert
  end

  # Monitor the temperature and send alerts
  # @param exit_time the time when to exit the monitor loop.
  def monitor_loop(exit_time = nil)
    loop do
      temp = get_temperature
      now = get_current_time
      return if !exit_time.nil? && now >= exit_time

      if @last_temp.nil?
        # First reading
        @last_temp = temp
        @last_time = now
        interval = 60
      else
        change_rate = (temp - @last_temp) / (now - @last_time) * 60
        if change_rate < 1
            interval = 60
            alert_state = 'Ok'
        elsif change_rate < 2
            interval = 10
            alert_state = 'Warning'
        else
            interval = 10
            alert_state = 'Critical'
        end

        @last_temp = temp
        @last_time = now

        if alert_state != @last_alert_state ||
                        (alert_state != 'Ok' && now >= 600 + @last_alert_time)
          # Send an alert
          send_alert("#{alert_state}: Temperature is rising at #{change_rate.round(1)} deg/min")
          @last_alert_time = now
        end

        @last_alert_state = alert_state
      end

      sleep(interval)
    end
  end

  # Spy this function, want to spy the result
  bond.spy_point(spy_result: true)
  # Read the temperature from a sensor
  def get_temperature
    resp_code, temp_data =
        make_request('http://system.server.com/temperature')
    raise 'Error while retrieving temperature!' unless resp_code == 200
    match = /<temperature>([0-9.]+)<\/temperature>/.match(temp_data)
    raise "Error while parsing temperature from: #{temp_data}" if match.nil?
    match[1].to_f
  end

  bond.spy_point
  # Read the current time
  def get_current_time
      Time.now.to_i
  end


  bond.spy_point
  # Sleep a few seconds
  def sleep(seconds)
    sleep(seconds)
  end

  bond.spy_point
  # Send an alert
  def send_alert(message)
    make_request('http://backend.server.com/messages', {message: message})
  end

  bond.spy_point(require_agent_result: true)
  # HTTP request (GET, or POST if the data is provided)
  def make_request(url, data = nil)
    full_url = "#{url}?#{URI.encode_www_form(data)}"
    resp = Net::HTTP.get_response(URI(full_url))
    [resp.code.to_i, resp.body]
  end
end
