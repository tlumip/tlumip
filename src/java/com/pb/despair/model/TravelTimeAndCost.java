
package com.pb.despair.model;


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

 }
