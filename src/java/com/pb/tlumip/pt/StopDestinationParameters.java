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
package com.pb.tlumip.pt;


/** 
 * A class that contains stop Destination Parameters 
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class StopDestinationParameters {


     public String purpose = new String();       //w=work, c=school,s=shop,r=recreate,o=other,b=workbased
     public float distanceAuto;
     public float distanceWalk;
     public float distanceBike;
     public float distanceTransit;
     public float distancePowerAuto;
     public float distancePowerWalk;
     public float distancePowerBike;
     public float distancePowerTransit;
     public float timeAuto;                  //add'l time if mode = auto driver, passenger
     public float timeWalk;                   //add'l time if mode = walk
     public float timeBike;                       //add'l time if mode = bike
     public float timeTransit;                 //add'l time if mode = WalkTransit,TransitPassenger,PassengerTransit,DriveTransit
     public float intraAuto;                     //*ln(acres) if mode = autoDriver,autoPassenger
     public float intraNonMotor;               //*ln(acres) if mode = walk,bike
     public float intraTransit;                 //*ln(acres) if mode = WalkTransit,TransitPassenger,PassengerTransit,DriveTransit
     public float retail;                   //total retail employment
     public float nonRetail;                    //nonRetail employment
     public float gradeSchool;                    //gradeSchool employment
     public float hhs;                              //households

     public StopDestinationParameters(){
        
             timeAuto=0;
         distanceAuto=0;
         distanceWalk=0;
         distanceBike=0;
         distanceTransit=0;
         distancePowerAuto=0;
          distancePowerWalk=0;
         distancePowerBike=0;
          distancePowerTransit=0;
          timeWalk=0;
          timeBike=0;             
          timeTransit=0;        
          intraNonMotor=0;      
          intraAuto=0;           
          intraTransit=0;           
          retail=0;          
          nonRetail=0;              
          gradeSchool=0;          
          hhs=0;                    
        };
   

}                                                                  
                                                                   
                                                                   
                                                                   