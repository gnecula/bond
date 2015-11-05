package bond;

import java.util.Map;

public interface Excepter {

  <T extends RuntimeException> T accept(Map<String, Object> map);

}
