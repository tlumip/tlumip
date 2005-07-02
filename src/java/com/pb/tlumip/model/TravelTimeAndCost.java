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

package com.pb.tlumip.model;

import java.io.PrintWriter;


/** A class that represents LOS for a zone pair
 * 
 * @author Joel Freedman
 */
public class TravelTimeAndCost {

	public float driveAloneTime;
	public float driveAloneDistance;
	public float driveAloneCost;
	
	public float sharedRide2Time;
	public float sharedRide2Distance;
	public float sharedRide2Cost;
	
	public float sharedRide3Time;
	public float sharedRide3Distance;
	public float sharedRide3Cost;
	
	public float walkTime;
	public float walkDistance;

	public float bikeTime;
	public float bikeDistance;
		
	public float walkTransitInVehicleTime;
	public float walkTransitFirstWaitTime;
	public float walkTransitShortFirstWaitTime;
	public float walkTransitLongFirstWaitTime;
	public float walkTransitTransferWaitTime;
	public float walkTransitTotalWaitTime;
	public float walkTransitNumberBoardings;
	public float walkTransitWalkTime;
	public float walkTransitFare;
	
	public float driveTransitInVehicleTime;
	public float driveTransitFirstWaitTime;
	public float driveTransitShortFirstWaitTime;
	public float driveTransitLongFirstWaitTime;
	public float driveTransitTotalWaitTime;
	public float driveTransitTransferWaitTime;
	public float driveTransitNumberBoardings;
	public float driveTransitWalkTime;
	public float driveTransitDriveTime;
	public float driveTransitDriveCost;
	public float driveTransitFare;
	
	public TravelTimeAndCost(){


	};
	
	
     public void printToScreen(){
    
    	System.out.println("driveAloneTime                  = "+driveAloneTime);                
		System.out.println("driveAloneDistance              = "+driveAloneDistance);            
		System.out.println("driveAloneCost                  = "+driveAloneCost);                
		System.out.println("sharedRide2Time                 = "+sharedRide2Time);               
		System.out.println("sharedRide2Distance             = "+sharedRide2Distance);           
		System.out.println("sharedRide2Cost                 = "+sharedRide2Cost);               
		System.out.println("sharedRide3Time                 = "+sharedRide3Time);               
		System.out.println("sharedRide3Distance             = "+sharedRide3Distance);           
		System.out.println("sharedRide3Cost                 = "+sharedRide3Cost);               
		System.out.println("walkTime                        = "+walkTime);                      
		System.out.println("walkDistance                    = "+walkDistance);                  
		System.out.println("bikeTime                        = "+bikeTime);                      
		System.out.println("bikeDistance                    = "+bikeDistance);                  
		System.out.println("walkTransitInVehicleTime        = "+walkTransitInVehicleTime);      
		System.out.println("walkTransitFirstWaitTime        = "+walkTransitFirstWaitTime);      
		System.out.println("walkTransitShortFirstWaitTime   = "+walkTransitShortFirstWaitTime); 
		System.out.println("walkTransitLongFirstWaitTime    = "+walkTransitLongFirstWaitTime);  
		System.out.println("walkTransitTransferWaitTime     = "+walkTransitTransferWaitTime);   
		System.out.println("walkTransitTotalWaitTime        = "+walkTransitTotalWaitTime);      
		System.out.println("walkTransitNumberBoardings      = "+walkTransitNumberBoardings);    
		System.out.println("walkTransitWalkTime             = "+walkTransitWalkTime);           
		System.out.println("walkTransitFare                 = "+walkTransitFare);               
		System.out.println("driveTransitInVehicleTime       = "+driveTransitInVehicleTime);     
		System.out.println("driveTransitFirstWaitTime       = "+driveTransitFirstWaitTime);     
		System.out.println("driveTransitShortFirstWaitTime  = "+driveTransitShortFirstWaitTime);
		System.out.println("driveTransitLongFirstWaitTime   = "+driveTransitLongFirstWaitTime); 
		System.out.println("driveTransitTotalWaitTime       = "+driveTransitTotalWaitTime);     
		System.out.println("driveTransitTransferWaitTime    = "+driveTransitTransferWaitTime);  
		System.out.println("driveTransitNumberBoardings     = "+driveTransitNumberBoardings);   
		System.out.println("driveTransitWalkTime            = "+driveTransitWalkTime);          
		System.out.println("driveTransitDriveTime           = "+driveTransitDriveTime);         
		System.out.println("driveTransitDriveCost           = "+driveTransitDriveCost);         
		System.out.println("driveTransitFare                = "+driveTransitFare);                
  	};

    public void print(PrintWriter file){
        file.println("\tTravel Time and Cost Values: ");
    	file.println("\tdriveAloneTime = "+driveAloneTime);
		file.println("\tdriveAloneDistance = "+driveAloneDistance);
		file.println("\tdriveAloneCost = "+driveAloneCost);
		file.println("\tsharedRide2Time = "+sharedRide2Time);
		file.println("\tsharedRide2Distance = "+sharedRide2Distance);
		file.println("\tsharedRide2Cost = "+sharedRide2Cost);
		file.println("\tsharedRide3Time = "+sharedRide3Time);
		file.println("\tsharedRide3Distance = "+sharedRide3Distance);
		file.println("\tsharedRide3Cost = "+sharedRide3Cost);
		file.println("\twalkTime = "+walkTime);
		file.println("\twalkDistance = "+walkDistance);
		file.println("\tbikeTime = "+bikeTime);
		file.println("\tbikeDistance = "+bikeDistance);
		file.println("\twalkTransitInVehicleTime = "+walkTransitInVehicleTime);
		file.println("\twalkTransitFirstWaitTime = "+walkTransitFirstWaitTime);
		file.println("\talkTransitShortFirstWaitTime = "+walkTransitShortFirstWaitTime);
		file.println("\twalkTransitLongFirstWaitTime = "+walkTransitLongFirstWaitTime);
		file.println("\twalkTransitTransferWaitTime = "+walkTransitTransferWaitTime);
		file.println("\twalkTransitTotalWaitTime = "+walkTransitTotalWaitTime);
		file.println("\twalkTransitNumberBoardings = "+walkTransitNumberBoardings);
		file.println("\twalkTransitWalkTime = "+walkTransitWalkTime);
		file.println("\twalkTransitFare = "+walkTransitFare);
		file.println("\tdriveTransitInVehicleTime = "+driveTransitInVehicleTime);
		file.println("\tdriveTransitFirstWaitTime = "+driveTransitFirstWaitTime);
		file.println("\tdriveTransitShortFirstWaitTime = "+driveTransitShortFirstWaitTime);
		file.println("\tdriveTransitLongFirstWaitTime = "+driveTransitLongFirstWaitTime);
		file.println("\tdriveTransitTotalWaitTime = "+driveTransitTotalWaitTime);
		file.println("\tdriveTransitTransferWaitTime = "+driveTransitTransferWaitTime);
		file.println("\tdriveTransitNumberBoardings = "+driveTransitNumberBoardings);
		file.println("\tdriveTransitWalkTime = "+driveTransitWalkTime);
		file.println("\tdriveTransitDriveTime = "+driveTransitDriveTime);
		file.println("\tdriveTransitDriveCost = "+driveTransitDriveCost);
		file.println("\tdriveTransitFare= "+driveTransitFare);
        file.println();
        file.println();

        file.flush();
  	}

 }
