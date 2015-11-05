package bond;

import java.util.Map;

public interface Resulter<T> {

  T accept(Map<String, Object> map);

}
