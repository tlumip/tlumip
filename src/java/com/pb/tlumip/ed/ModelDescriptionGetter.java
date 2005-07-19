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

import java.util.Vector;

import org.apache.log4j.Logger;

public class ModelDescriptionGetter {

  XMLModelSpecification ms;
  protected static Logger logger = Logger.getLogger(ModelDescriptionGetter.class);

  /**
   * Takes in xml files value name to read xml file.
   */
  public ModelDescriptionGetter(String filename) {
      if(logger.isDebugEnabled()) logger.debug("Reading in xml file " + filename + "...");
    try {
      ms = new XMLModelSpecification(filename);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Reads and builds the model based on the xml read in this program.
   */
  Model makeModel() {
    if(logger.isDebugEnabled()) logger.debug("Building model...");
    Vector v = new Vector();
    SubModel s;
    s = this.makeSubModel();
    while(s != null) {
      v.add(s);
      s = this.makeSubModel();
    }
    if(logger.isDebugEnabled()) logger.debug("Completed building model.");
    return new Model(v);
  }

  /**
   * Makes a SubModel based on the xml file being read.  Returns the SubModel
   * to the makeModel method.
   */
  SubModel makeSubModel() {
    try {
    if(ms.nextSubModelXML()) {
      VariableStore vs = new VariableStore();
      SubModel sm = SubModelFactory.build(ms, vs);
      return sm;
    }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }


}
