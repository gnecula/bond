package bond;

import java.util.Map;

public interface CheckedExcepter {

  Exception accept(Map<String, Object> map);

}
