package io.github.necula01.bond;

/**
 * An exception thrown whenever an agent result or exception is requested
 * (for example, through {@link Bond#spy(String, Class)}) and the agent that
 * is found does not match the correct type.
 */
public class IllegalSpyAgentException extends RuntimeException {

  public IllegalSpyAgentException(String message) {
    super(message);
  }

}
