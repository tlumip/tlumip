
/**
 * Company:      ECONorthwest<p>
 * DataHound is used to interact with EDDataAccess to get and set data.
 */
package com.pb.despair.ed;

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
