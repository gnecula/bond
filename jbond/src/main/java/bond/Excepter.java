package bond;

import java.util.Map;

public interface Excepter extends CheckedExcepter {

  RuntimeException accept(Map<String, Object> map);

}
