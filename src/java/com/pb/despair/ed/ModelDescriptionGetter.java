
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.despair.ed;
import com.pb.common.util.Debug;

import java.util.Vector;

public class ModelDescriptionGetter {

  XMLModelSpecification ms;

  /**
   * Takes in xml files value name to read xml file.
   */
  public ModelDescriptionGetter(String filename) {
    Debug.println("Reading in xml file " + filename + "...");
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
    Debug.println("Building model...");
    Vector v = new Vector();
    SubModel s;
    s = this.makeSubModel();
    while(s != null) {
      v.add(s);
      s = this.makeSubModel();
    }
    Debug.println("Completed building model.");
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
