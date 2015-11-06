package bond;

import java.util.Map;

public interface CheckedExcepter<E extends Exception> {

  <T extends E> T accept(Map<String, Object> map);

}
