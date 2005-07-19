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
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.tlumip.ed;
import com.pb.common.util.Debug;

import java.util.Vector;

import org.apache.log4j.Logger;

public class SimpleFunctions extends SubModel {
  protected static Logger logger = Logger.getLogger(SimpleFunctions.class);
  Vector equations;

  /**
   * Constructor takes in all basic SubModel data.
   */
  public SimpleFunctions(int o, String n, VariableStore v, Vector e) {
    super(o,n,v);
    equations = e;
  }


  protected void solve() throws Exception {
    Equation e;
    for(int i = 0; i < equations.size(); i++) {
      e = (Equation)equations.get(i);
      if(logger.isDebugEnabled()) logger.debug("  Solving for " + e.getName());
      SimpleSolver.solve(e);
    }
  }

  protected String getType() {
    return "SimpleFunctions";
  }



}
