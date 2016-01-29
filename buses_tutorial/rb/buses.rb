#
# A set of functions to find the closest running buses to a given bus stop.
#
# This project uses REST APIs provided by the nextbus.com. For details, see
#     https://www.nextbus.com/xmlFeedDocs/NextBusXMLFeed.pdf

require 'rexml/document'
require 'rexml/xpath'
require 'uri'
require 'net/http'

require 'bond'

module Buses
  extend BondTargetable

  # We will be using Alameda County Transit authority feed
  NEXTBUS_AGENCY = 'actransit'
  NEXTBUS_ROUTE = '51B'

  # Get the list of running buses for a route, sorted in increasing order of their
  # distance to a given stop.
  def self.get_closest_buses
    stops_xml = Buses.get_nextbus_request(command: 'routeConfig', a: NEXTBUS_AGENCY, r: NEXTBUS_ROUTE)
    stops_dom = REXML::Document.new(stops_xml)

    # List of stops, each with { :lat, :lon, :title }
    stops = REXML::XPath.each(stops_dom, '//stop').select { |dom| !dom.attributes['title'].nil? }.map do |dom|
      {
          title: dom.attributes['title'],
          lat: dom.attributes['lat'].to_f,
          lon: dom.attributes['lon'].to_f,
      }
    end

    # Ask the user to select the stop
    our_stop = select_stop_interactive(stops)

    # Now get the running buses on that route
    buses_data_xml = get_nextbus_request(command: 'vehicleLocations', a: NEXTBUS_AGENCY, r: NEXTBUS_ROUTE, t: 0)
    buses_data_dom = REXML::Document.new(buses_data_xml)

    buses = REXML::XPath.each(buses_data_dom, '//vehicle').map do |v|
      {
          id: v.attributes['id'],
          lat: v.attributes['lat'].to_f,
          lon: v.attributes['lon'].to_f
      }
    end

    # Now compute how far the buses are from our stop
    buses.each { |bus| bus[:away] = compute_lat_long_distance(bus, our_stop) }

    buses.sort_by { |bus| bus[:away] }
  end

  # Ask the user to select among the list of stops.
  # Keep trying to ask the user until they pick a valid stop.
  # stops: list of stops with { :lat, :lon, :title }
  def self.select_stop_interactive(stops)
    puts "These are the stops on route #{NEXTBUS_ROUTE}"
    stops.each_with_index do |stop, idx|
      puts "#{(idx+1).to_s.rjust(2, '0')} - #{stop[:title]}"
    end

    our_stop = nil
    while our_stop.nil? do
      stop_idx_str = read_console("Select a stop by its number (1-#{stops.length}): ")
      begin
        # Convert to an integer
        stop_idx = Integer(stop_idx_str.strip)
        our_stop = stops[stop_idx - 1] if stop_idx >= 1 && stop_idx < stops.length
      rescue ArgumentError
        # Not a number. Keep trying
      end
    end

    puts "You selected bus stop #{our_stop[:title]}"
    our_stop
  end

  # Read a line from the console.
  # prompt: The prompt to show
  def self.read_console(prompt)
    puts prompt
    gets.chomp
  end

  # Make a HTTP request to the NextBus API and return the result, as a string
  # kwargs: the query parameters to pass to the request
  def self.get_nextbus_request(**kwargs)
    base_nextbus_url = 'http://webservices.nextbus.com/service/publicXMLFeed'
    full_url = "#{base_nextbus_url}?#{URI.encode_www_form(kwargs)}"
    puts "GET request to #{full_url}"
    Net::HTTP.get_response(URI(full_url)).body
  end

  # Compute the distance between two records that have fields :lat and :lon
  # See details and reference implementation at http://andrew.hedges.name/experiments/haversine/
  def self.compute_lat_long_distance(point1, point2)
    lat1 = degree_to_rad(point1[:lat])
    lon1 = degree_to_rad(point1[:lon])
    lat2 = degree_to_rad(point2[:lat])
    lon2 = degree_to_rad(point2[:lon])

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = Math::sin(dlat / 2) * Math::sin(dlat / 2) +
        Math::cos(lat1) * Math::cos(lat2) * Math::sin(dlon / 2) * Math::sin(dlon / 2)
    c = 2 * Math::atan2(Math::sqrt(a), Math::sqrt(1 - a))

    earth_radius = 3961 # Use 6373 for km

    d = earth_radius * c
    d.round(3)
  end

  # Convert degree to radians
  def self.degree_to_rad(deg)
    deg * Math::PI / 180.0
  end

end

if __FILE__ == $0
  Buses.get_closest_buses.each do |bus|
    puts "Bus #{bus[:id]} is #{bus[:away].round(2)} miles away from your stop."
  end
end
