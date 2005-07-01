
/**
 * Title:        EDModel<p>
 * Description:  <p>
 * Copyright:    Copyright (c) <p>
 * Company:      ECONorthwest<p>
 * @author
 * @version 1.0
 */

package com.pb.tlumip.ed;
import com.pb.common.util.Debug;

public class IOObject {

  static void PrintSingleArray(double[] x) {
    for(int i = 0; i < x.length; i++) {
      Debug.println(String.valueOf(x[i]));
    }
    Debug.println("");
  }

  static void PrintDoubleArray(double[][] y) {
  String s= "";
    for(int i=0; i<y.length; i++) {
      for(int j=0; j<y.length; j++) {
        s = s + ""+ y[i][j] + "  ";
      }
      Debug.println(s);
      s =  "";
    }
    Debug.println("");
  }

}
