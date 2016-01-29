#
# A set of functions to find the closest running buses to a given bus stop.
#
# This project uses REST APIs provided by the nextbus.com. For details, see
#     https://www.nextbus.com/xmlFeedDocs/NextBusXMLFeed.pdf

import urllib2
import urllib
import time
import math
from xml.dom.minidom import parseString

from bond import bond

# We will be using Alameda County Transit authority feed
NEXTBUS_AGENCY = 'actransit'
NEXTBUS_ROUTE = '51B'

def get_closest_buses():
    """
    Get the list of running buses for a route, sorted in increasing order
    of their distance to a given stop.
    :return:
    """

    stops_xml = get_nextbus_request(command='routeConfig', a=NEXTBUS_AGENCY, r=NEXTBUS_ROUTE)
    stops_dom = parseString(stops_xml)

    stops = [] # List of stops, with { 'lat', 'lon', 'title'  }
    for s in stops_dom.getElementsByTagName('stop'):
        if s.attributes.get('title'):
            stops.append({
                    'title': s.attributes['title'].value,
                    'lat': float(s.attributes['lat'].value),
                    'lon': float(s.attributes['lon'].value)
            })

    # Ask the user to select the stop
    our_stop = select_stop_interactive(stops)

    # Now get the running buses on that route

    buses_data_xml = get_nextbus_request(command='vehicleLocations', a=NEXTBUS_AGENCY, r=NEXTBUS_ROUTE, t=0)
    buses_data_dom = parseString(buses_data_xml)

    # Construct the list of buses with { 'id', 'lat', 'lon' }
    buses = []
    for v in buses_data_dom.getElementsByTagName('vehicle'):
        buses.append({
            'id': v.attributes['id'].value,
            'lat': float(v.attributes['lat'].value),
            'lon': float(v.attributes['lon'].value)
        })

    # Now compute how far the buses are from our stop
    for bus in buses:
        bus['away'] = compute_lat_long_distance(bus, our_stop)

    buses = sorted(buses, key=lambda b: b['away'])
    return buses


def select_stop_interactive(stops):
    """
    Ask the user to select among the list of stops.
    Keep trying to ask the user until they pick a valid stop.

    :param stops: list of stops with { 'lat', 'lon', 'title' }
    :return: one of the stops from the list
    """
    # Now get the user to choose the stop
    print("These are the stops on route {}".format(NEXTBUS_ROUTE))
    for sidx in range(len(stops)):
        print("{:2d} - {}".format(sidx + 1, stops[sidx]['title']))

    our_stop = None
    while True:
        stop_idx_str = read_console('Select a stop by its number (1-{}): '.format(len(stops)))
        try:
            # Convert to an integer
            stop_idx = int(stop_idx_str.strip())
            if stop_idx >= 1 and stop_idx < len(stops):
                our_stop = stops[stop_idx - 1]
                break
            else:
                continue

        except ValueError:
            # Not a number. Keep trying
            continue

    print("You selected bus stop {}".format(our_stop['title']))
    return our_stop

def get_routes():
    """
    Get a list of routes for this agency.
    NOT USED. RESERVE.
    :return:
    """
    # First get the list of routes
    route_list_xml = get_nextbus_request(command='routeList', a=NEXTBUS_AGENCY)
    route_list_dom = parseString(route_list_xml)

    routes = [] # Collect here the routes
    for route_dom in route_list_dom.getElementsByTagName('route'):
        routes.append(route_dom.attributes['tag'].value)
    return routes

def read_console(prompt):
    """
    Read a line from the console
    :param prompt: the prompt to show
    :return:
    """
    return raw_input(prompt)

def get_nextbus_request(**kwargs):
    """
    Make a HTTP request to the NextBus API and return the result, as a string
    :param kwargs: the query parameters to pass to the request
    :return:
    """
    base_nextbus_url = 'http://webservices.nextbus.com/service/publicXMLFeed'
    full_url = base_nextbus_url + '?' + urllib.urlencode(kwargs)
    print("GET request to {}".format(full_url))
    return urllib2.urlopen(full_url).read()

def compute_lat_long_distance(point1, point2):
    """
    Compute the distance between two records that have fields 'lat' and 'lon'.
    See details and reference implementation at http://andrew.hedges.name/experiments/haversine/

    :param point1: a record with { 'lat', 'lon' }
    :param point2: a record with { 'lat', 'lon' }
    :return:
    """
    lat1 = degree_to_rad(point1['lat'])
    lat2 = degree_to_rad(point2['lat'])
    lon1 = degree_to_rad(point1['lon'])
    lon2 = degree_to_rad(point2['lon'])

    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = math.sin(dlat / 2) * math.sin(dlat / 2) + \
        math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) * math.sin(dlon / 2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    earth_radius = 3961  # Use 6373 for km

    d = earth_radius * c  # In miles
    return round(d, 3)


def degree_to_rad(deg):
    """
    Convert degree to radians
    :param deg:
    :return:
    """
    return deg * math.pi / 180.0


if __name__ == '__main__':
    buses = get_closest_buses()
    for b in buses:
        print('Bus {:5s} is {:.2f} miles away from your stop'.format(b['id'], b['away']))
