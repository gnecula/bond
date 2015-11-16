package io.github.necula01.bond.reconcile;

/**
 * All of the possible types of reconciliation.
 */
public enum ReconcileType {

  /**
   * Console-type reconciliation. The user will be asked to provide input
   * on what actions to take via the console, or, if none is present, through
   * a modal dialog box.
   */
  CONSOLE ("console"),
  /**
   * Do not accept any new changes (fail-fast; this is the default).
   */
  ABORT ("abort"),
  /**
   * Accept all new changes - this should be used with caution as no real
   * testing assertions are being made.
   */
  ACCEPT ("accept"),
  /**
   * Run the graphical kdiff3 tool to reconcile differences. Must have
   * kdiff3 installed and available in your path.
   */
  KDIFF3 ("kdiff3");

  private final String _name;

  ReconcileType(String name) {
    _name = name;
  }

  /**
   * Essentially a case-insensitive version of {@link #valueOf}; gets the
   * appropriate {@code ReconcileType} for the given name.
   *
   * @param name The name of the {@code ReconcileType} to retrieve
   * @throws IllegalArgumentException If {@code name} is not a valid name for
   *         a {@code ReconcileType}
   * @return The matching {@code ReconcileType}
   */
  public static ReconcileType getFromName(String name) {
    for (ReconcileType type : ReconcileType.values()) {
      if (type.getName().equalsIgnoreCase(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("No ReconcileType exists with name: " + name);
  }

  /**
   * Return the name of this constant.
   *
   * @return Name
   */
  public String getName() {
    return _name;
  }

}
