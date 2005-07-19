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
 * Company:      ECONorthwest<p>
 * DataHound is used to interact with EDDataAccess to get and set data.
 */
package com.pb.tlumip.ed;

import org.apache.log4j.Logger;



public class DataHound  {
  protected static Logger logger = Logger.getLogger(DataHound.class);
  EDDataAccess eda;

  public DataHound(EDDataAccess e) {
    eda = e;
  }


    /**
     * Gets data from the database and writes it to the variable value.
     */
  public void getData(Variable v) {
    if(!v.isDependant()) {
    	try
    	{
        	v.setValue(eda.getValue(v.getName(), v.getLocation(), v.getYear()));
    	}catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }
  }

    /**
   * Double variable value is output to the database by exponentiating the
   * variables value.
   */
  public void setData(Variable v) {
    try {
    if(v.isDependant()) {
    	//Need to actually set data at some point... 
        eda.insertValue(v.getName(),v.getValue(),v.getYear());
        logger.info("\t\tvariable: " + v.getName() + " value: "+ v.getValue());
    }
    } catch (UnknownValueException e) {
      e.printStackTrace();
    }
  }
  
  public void writeData() {
      eda.writeData();
  }
}
