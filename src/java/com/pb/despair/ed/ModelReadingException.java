
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.despair.ed;

public class ModelReadingException extends Exception {

  private String unknownString;

  public ModelReadingException(String s, String u) {
    super(s);
    unknownString = u;
  }

  public String getMessage() {
    return "Unknown " + super.getMessage() + ": " + unknownString;
  }

}
