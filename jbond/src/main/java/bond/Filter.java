package bond;

import java.util.Map;

public interface Filter {

  boolean accept(Map<String, Object> map);

}
