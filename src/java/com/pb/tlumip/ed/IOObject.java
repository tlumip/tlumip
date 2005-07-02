/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

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
