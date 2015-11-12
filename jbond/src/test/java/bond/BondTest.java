package bond;

import org.junit.Rule;
import org.junit.Test;

/*
TESTS TO WRITE:

- spy agents
  - filters of all types
  - returns
  - exceptions (checked + unchecked)
  - doers
- spying directly on Bond instead of on an Observation

- test the reconciler
  - different types (accept, abort)
  - loading the correct reconciler type (i.e. from environment variable if not set)
  - switching from console to kdiff3
  - not reconciling if a noSaveMessage is set
 */
public class BondTest {

  @Rule
  public BondTestRule btr = new BondTestRule();

}
