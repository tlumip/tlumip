package com.pb.despair.pt.old;

import com.pb.common.model.LogitModel;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.despair.pt.ActivityPurpose;
import com.pb.despair.pt.PersonTourModeAttributes;
import com.pb.despair.pt.Tour;
import com.pb.despair.pt.TourModeParameters;
import com.pb.despair.pt.ZoneAttributes;
import com.pb.despair.pt.tourmodes.*;

/** 
 * ModeChoiceModel is used to build one nested mode choice logit model 
 * to be reused (rather than creating new objects).
 * 
 * @author Steve Hansen
 * @version 1.0 09/02/2003
 *
 */

public class ModeChoiceModel {
    
    //Root LogitModel
    public LogitModel tourModeChoiceModel = new LogitModel("tourModeChoiceModel");
    
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
     
    //Constructor   
    public ModeChoiceModel(){
        buildModel();
    };

    public void buildModel(){
        
        autoNest.addAlternative(thisDriver);
        
        nonMotorizedNest.addAlternative(thisWalk);
        nonMotorizedNest.addAlternative(thisBike);
        
        transitNest.addAlternative(thisWalkTransit);
        transitNest.addAlternative(thisDriveTransit);
        transitNest.addAlternative(thisPassengerTransit);
        
        passengerNest.addAlternative(thisPassenger);
        passengerNest.addAlternative(thisPassengerTransit);
        
        tourModeChoiceModel.addAlternative(autoNest);
        tourModeChoiceModel.addAlternative(nonMotorizedNest);
        tourModeChoiceModel.addAlternative(transitNest);
        tourModeChoiceModel.addAlternative(passengerNest);
        
    }
    
    public void calcUtilities(TourModeParameters theseParameters, TravelTimeAndCost departCost, TravelTimeAndCost returnCost,
             PersonTourModeAttributes thisPerson, Tour thisTour, ZoneAttributes thisZone){
          
             this.thisDriver.setAvailability(true);
             this.thisPassenger.setAvailability(true);
             this.thisWalk.setAvailability(true);
             this.thisBike.setAvailability(true);
             this.thisWalkTransit.setAvailability(true);
             this.thisTransitPassenger.setAvailability(true);
             this.thisDriveTransit.setAvailability(true); 
          
          
             if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED && 
                thisTour.driveToWork==false)
                this.thisDriver.isAvailable=false; 
                     
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
                      
             //if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK||
             //thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)
             this.thisDriveTransit.calcUtility( departCost, returnCost,
                  theseParameters, thisPerson);
             //else mcModel.thisDriveTransit.setAvailability(false);     
    }

}
