package bond;

public enum Reconcile {

  CONSOLE ("console"),
  ABORT ("abort"),
  ACCEPT ("accept"),
  KDIFF3 ("kdiff3");

  private final String _name;

  Reconcile(String name) {
    _name = name;
  }

  public String getName() {
    return _name;
  }

}
