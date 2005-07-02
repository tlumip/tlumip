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

public class DataHound  {

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
    	//Need to actually set data at some point... Oh, and get rid of the println (= debug)
        eda.insertValue(v.getName(),v.getValue(),v.getYear());
        System.out.println("name:  " + v.getName() + "\nvalue:  "+ v.getValue() + "\nyear:  " + v.getYear());
    }
 /*   } catch (SQLException e) {
      e.printStackTrace();*/
    } catch (UnknownValueException e) {
      e.printStackTrace();
    }
  }
}
