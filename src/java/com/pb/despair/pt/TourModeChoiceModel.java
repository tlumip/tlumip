package com.pb.despair.pt;

import org.apache.log4j.Logger;

import com.pb.despair.model.Mode;
import com.pb.despair.model.SkimsInMemory;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.common.model.LogitModel;
import com.pb.despair.pt.tourmodes.*;

/** 
 * This model implements a logit model to choose a tour mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
 
public class TourModeChoiceModel{
    
     protected static Logger logger = Logger.getLogger("com.pb.despair.pt.TourModeChoiceModel");
    
     Mode chosenMode;
     double logsum;
     PersonTourModeAttributes personAttributes = new PersonTourModeAttributes();
    //Root LogitModel
    public LogitModel root = new LogitModel("root");
    
    //Mode Nests
    public LogitModel autoNest = new LogitModel("autoNest");
    public LogitModel nonMotorizedNest = new LogitModel("nonMotorizedNest");
    public LogitModel transitNest = new LogitModel("transitNest");
    public LogitModel passengerNest = new LogitModel("passengerNest");
    
    //Elemental Alternatives                
    public AutoDriver thisDriver = new AutoDriver();
    public AutoPassenger thisPassenger = new AutoPassenger();
    public Walk thisWalk = new Walk();
    public Bike thisBike = new Bike();
    public WalkTransit thisWalkTransit = new WalkTransit();
    public TransitPassenger thisTransitPassenger = new TransitPassenger();
    public PassengerTransit thisPassengerTransit = new PassengerTransit();
    public DriveTransit thisDriveTransit = new DriveTransit(); 

     
     
     final static int debugID = 1;
     
     public TourModeChoiceModel(){
        
        autoNest.addAlternative(thisDriver);
        
        nonMotorizedNest.addAlternative(thisWalk);
        nonMotorizedNest.addAlternative(thisBike);
        
        transitNest.addAlternative(thisWalkTransit);
        transitNest.addAlternative(thisDriveTransit);
        
        passengerNest.addAlternative(thisPassenger);
        passengerNest.addAlternative(thisPassengerTransit);
        passengerNest.addAlternative(thisTransitPassenger);
        
        root.addAlternative(autoNest);
        root.addAlternative(nonMotorizedNest);
        root.addAlternative(transitNest);
        root.addAlternative(passengerNest);
        
    }
     
     public void calculateTourMode(PTHousehold thisHousehold, PTPerson thisPerson, int tourNumber, SkimsInMemory skims, TourModeParametersData allParams,
          Taz destinationTaz){
          
          Tour thisTour = thisPerson.weekdayTours[tourNumber];
          
          //set travel time and cost
          TravelTimeAndCost departCost = skims.setTravelTimeAndCost(thisTour.begin.location.zoneNumber, 
               thisTour.primaryDestination.location.zoneNumber, thisTour.begin.endTime);
                         
          TravelTimeAndCost returnCost = skims.setTravelTimeAndCost(thisTour.primaryDestination.location.zoneNumber, 
               thisTour.end.location.zoneNumber, thisTour.primaryDestination.endTime);

          //set tour mode choice attributes
          TourModeParameters params = new TourModeParameters();
          //char actPurpose = ActivityPurpose.ACTIVITY_PURPOSES[thisTour.primaryDestination.activityPurpose];
          params = (TourModeParameters) allParams.getTourModeParameters(thisTour.primaryDestination.activityPurpose);
               
          //set taz attributes (only parking cost at this point)
          ZoneAttributes zone = new ZoneAttributes();
          if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK||
          thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)
               zone.parkingCost=(destinationTaz.workParkingCost/60 * thisTour.primaryDestination.duration);
          else
               zone.parkingCost=(destinationTaz.nonWorkParkingCost/60 * thisTour.primaryDestination.duration);
          
          //set person tour mode attributes
          personAttributes.setAttributes(thisHousehold,thisPerson,thisTour);
          
          calculateUtility(params,departCost,returnCost,personAttributes,thisTour,zone);
     }
     
    public void calculateUtility(TourModeParameters theseParameters, TravelTimeAndCost departCost, TravelTimeAndCost returnCost,
             PersonTourModeAttributes thisPerson, Tour thisTour, ZoneAttributes thisZone){
          
             this.thisDriver.setAvailability(true);
             this.thisPassenger.setAvailability(true);
             this.thisWalk.setAvailability(true);
             this.thisBike.setAvailability(true);
             this.thisWalkTransit.setAvailability(true);
             this.thisTransitPassenger.setAvailability(true);
             this.thisPassengerTransit.setAvailability(true);
             this.thisDriveTransit.setAvailability(true); 
          
          
               //calculate utilities
             this.thisDriver.calcUtility( departCost, returnCost,
                  thisZone, theseParameters, thisPerson);
                         
             this.thisPassenger.calcUtility( departCost, returnCost,
                  thisZone, theseParameters, thisPerson);
             
             this.thisWalk.calcUtility( departCost, returnCost,
                  thisZone, theseParameters, thisPerson);
             
             this.thisBike.calcUtility( departCost, returnCost,
                  theseParameters, thisPerson);
             
             this.thisWalkTransit.calcUtility( departCost, returnCost,
                  theseParameters, thisPerson);
             
             this.thisTransitPassenger.calcUtility( departCost, returnCost,
                  theseParameters, thisPerson);
             
             this.thisPassengerTransit.calcUtility( departCost, returnCost,
                  thisZone, theseParameters, thisPerson);
                      
             this.thisDriveTransit.calcUtility( departCost, returnCost,
                  theseParameters, thisPerson);
             
             this.autoNest.setDispersionParameter(theseParameters.nestlow/this.root.getDispersionParameter());
             this.nonMotorizedNest.setDispersionParameter(theseParameters.nestlow/this.root.getDispersionParameter());
             this.transitNest.setDispersionParameter(theseParameters.nestlow/this.root.getDispersionParameter());
             this.passengerNest.setDispersionParameter(theseParameters.nestlow/this.root.getDispersionParameter());
                                              
             this.root.computeAvailabilities();
             logsum = this.root.getUtility();     
             this.root.calculateProbabilities();
    }

     
     public void chooseMode(){
          try{ 
               chosenMode =  (Mode) this.root.chooseElementalAlternative();
          }catch(Exception e){
               System.out.println(e);
               logger.error("Error in mode choice: no modes available ");
               System.exit(1);
          }
     }


}
