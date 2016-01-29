
package bond.example;

import com.google.common.base.Optional;
import bond.Bond;
import bond.spypoint.SpyPoint;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// A set of functions to find the closest running buses to a given bus stop.
//
// This project uses REST APIs provided by the nextbus.com. For details, see
//   https://www.nextbus.com/xmlFeedDocs/NextBusXMLFeed.pdf
public class Buses {

  // Using the Alameda County Transit authority feed
  private static final String NEXTBUS_AGENCY = "actransit";
  private static final String NEXTBUS_ROUTE = "51B";

  public static void main(String... args) throws Exception {
    List<Bus> buses = getClosestBuses();
    for (Bus b : buses) {
      System.out.println(String.format("Bus %5s is %.2f miles away from your stop", b.id, b.away));
    }
  }

  /**
   * Get the list of running buses for a route, sorted in increasing order of
   * their distance to a given stop
   *
   * @return List of running buses
   */
  public static List<Bus> getClosestBuses() throws Exception {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(getNextBusRequest("command", "routeConfig",
        "a", NEXTBUS_AGENCY, "r", NEXTBUS_ROUTE));

    NodeList stopNodes = doc.getElementsByTagName("stop");
    List<Stop> stops = new ArrayList<Stop>();
    for (int i = 0; i < stopNodes.getLength(); i++) {
      Node stopNode = stopNodes.item(i);
      NamedNodeMap nnm = stopNode.getAttributes();
      Node titleNode = nnm.getNamedItem("title");
      if (titleNode != null) {
        stops.add(new Stop(titleNode.getNodeValue(),
                              Double.parseDouble(nnm.getNamedItem("lat").getNodeValue()),
                              Double.parseDouble(nnm.getNamedItem("lon").getNodeValue())));
      }
    }

    // Ask the user to select the stop
    Stop selectedStop = selectStopInteractive(stops);

    // Now get the running buses on that route
    Document busesDataDOM = dBuilder.parse(getNextBusRequest("command", "vehicleLocations",
        "a", NEXTBUS_AGENCY, "r", NEXTBUS_ROUTE, "t", "0"));

    List<Bus> buses = new ArrayList<Bus>();
    NodeList busNodes = busesDataDOM.getElementsByTagName("vehicle");
    for (int i = 0; i < busNodes.getLength(); i++) {
      NamedNodeMap nnm = busNodes.item(i).getAttributes();
      buses.add(new Bus(nnm.getNamedItem("id").getNodeValue(),
                           Double.parseDouble(nnm.getNamedItem("lat").getNodeValue()),
                           Double.parseDouble(nnm.getNamedItem("lon").getNodeValue())));
    }

    // Now compute how far the buses are from our stop
    for (Bus b : buses) {
      b.setAway(computeLatLonDistance(b, selectedStop));
    }

    Collections.sort(buses);
    return buses;
  }

  /**
   * Ask the user to select among the list of stops.
   * Keep trying to ask the user until they pick a valid stop.
   *
   * @param stops List of Stops to select from
   * @return One of the stops from the list
   */
  public static Stop selectStopInteractive(List<Stop> stops) {
    System.out.println("These are the stops on the route " + NEXTBUS_ROUTE);
    for (int i = 0; i < stops.size(); i++) {
      System.out.println(String.format("%2d - %s", i + 1, stops.get(i).title));
    }

    Stop selected = null;
    while (selected == null) {
      String stopIndexStr = readConsole("Select a stop by its number (1-" + stops.size() + "): ");
      int stopIndex;
      try {
        stopIndex = Integer.parseInt(stopIndexStr);
        if (stopIndex >= 1 && stopIndex < stops.size()) {
          selected = stops.get(stopIndex - 1);
        }
      } catch (NumberFormatException e) {
        continue;
      }
    }

    System.out.println("You selected bus stop " + selected.title);
    return selected;
  }

  /**
   * Read a line from the console
   *
   * @param prompt Prompt to show
   * @return User input
   */
  public static String readConsole(String prompt) {
    System.out.print(prompt);
    try {
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      return input.readLine();
    } catch (IOException e) {
      return "";
    }
  }

  /**
   * Make an HTTP request to the NextBus API and return the result, as an InputStream
   *
   * @param requestArgs Pairs of keyword arguments to be passed to the request URL
   * @return Result of HTTP request
   */
  public static InputStream getNextBusRequest(String... requestArgs) throws Exception {
    String baseNextBusURL = "http://webservices.nextbus.com/service/publicXMLFeed";
    String argString = "";
    for (int i = 0; i < requestArgs.length; i += 2) {
      argString += "&" + URLEncoder.encode(requestArgs[i], "UTF-8") + "=" + URLEncoder.encode(requestArgs[i+1], "UTF-8");
    }
    argString = argString.substring(1);
    URL fullURL = new URL(baseNextBusURL + "?" + argString);
    System.out.println("Request to: " + fullURL);
    URLConnection connection = fullURL.openConnection();
    return connection.getInputStream();
  }

  /**
   * Compute the distance between two points
   * See details and reference implementation at http://andrew.hedges.name/experiments/haversine/
   *
   * @param p1 Point containing latitude and longitude
   * @param p2 Other point containing latitude and longitude
   * @return The distance between the points
   */
  public static double computeLatLonDistance(Point p1, Point p2) {
    double lat1 = Math.toRadians(p1.getLat());
    double lon1 = Math.toRadians(p1.getLon());
    double lat2 = Math.toRadians(p2.getLat());
    double lon2 = Math.toRadians(p2.getLon());
    
    double dlon = lon2 - lon1;
    double dlat = lat2 - lat1;
    double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

    int earthRadius = 3961; // Use 6373 for km
    return earthRadius * c;
  }

  interface Point {
    double getLat();
    double getLon();
  }

  static class Bus implements Point, Comparable<Bus> {

    public String id;
    public double lat;
    public double lon;
    public double away;

    public Bus(String id, double lat, double lon) {
      this.id = id;
      this.lat = lat;
      this.lon = lon;
    }

    public double getLat() {
      return lat;
    }

    public double getLon() {
      return lon;
    }

    public void setAway(double away) {
      this.away = away;
    }

    public int compareTo(Bus other) {
      if (away == other.away) {
        return 0;
      }
      return (away < other.away) ? -1 : 1;
    }
  }

  static class Stop implements Point {

    public String title;
    public double lat;
    public double lon;

    public Stop(String title, double lat, double lon) {
      this.title = title;
      this.lat = lat;
      this.lon = lon;
    }

    public double getLat() {
      return lat;
    }

    public double getLon() {
      return lon;
    }
  }

}
