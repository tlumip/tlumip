package com.pb.despair.pt;

import java.io.PrintWriter;

/**  
 * Attributes of a TAZ - only parking cost right now
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class ZoneAttributes{

     public double parkingCost;

    public void print(PrintWriter file){
        file.println("Zone Attributes:" );
        file.println("\tparkingCost = " + parkingCost);
        file.println();
        file.println();
        
        file.flush();
    }

}
