package bond.reconcile;

public enum ReconcileType {

  CONSOLE ("console"),
  ABORT ("abort"),
  ACCEPT ("accept"),
  KDIFF3 ("kdiff3");

  private final String _name;

  ReconcileType(String name) {
    _name = name;
  }

  public static ReconcileType getFromName(String name) {
    for (ReconcileType type : ReconcileType.values()) {
      if (type.getName().equals(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("No ReconcileType exists with name: " + name);
  }

  public String getName() {
    return _name;
  }

}
