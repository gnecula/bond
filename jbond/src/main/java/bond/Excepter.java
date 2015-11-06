package bond;

import java.util.Map;

public interface Excepter extends CheckedExcepter<RuntimeException> {

  <T extends RuntimeException> T accept(Map<String, Object> map);

}
