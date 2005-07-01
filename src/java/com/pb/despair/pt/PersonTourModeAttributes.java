package com.pb.tlumip.pt;


/** 
 * Person Attributes for Tour Mode Choice
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class PersonTourModeAttributes {

   public int size;
   public int autos;
   public int fullWorkers;
   public int partWorkers;
   public int workers;
   public int age;
   public int income;
   public int originZone;
   public int destinationZone;

/*
   public int stop1Zone;
   public int stop2Zone;
   public int hasStop1;
   public int hasStop2;
   
   //in military time
   public int timeLeaveOrigin;
   public int timeLeaveDestination;
   public int timeLeaveStop1;
   public int timeLeaveStop2;
*/   
   //in minutes
   public float primaryDuration;

/*
   public float stop1Duration;
   public float stop2Duration;
*/   
   
   //w=work,c=school,s=shop,r=soc\rec,o=other,b=workbased
   public char tourPurpose;

    //dummy variables
   public int totalStops;
   public int stop0;
   public int stop1;
   public int stop2;
   public int size1;
   public int size2;
   public int size3p;
   public int inclow;
   public int incmed;
   public int inchi;
   public int auwk0;
   public int auwk1;
   public int auwk2;
   public int aupr0;
   public int aupr1;
   public int aupr2;

   public int noon;
  
   
   public PersonTourModeAttributes(){                                
                                                                        
                                                                     
        size=2;                                                       
        autos=1;                                     
        fullWorkers=0;                                               
        partWorkers=0;                                               
        workers=0;                                                   
        age=21;                                                      
        income=0;                                                    
        originZone=0;                                                
        destinationZone=0;                                           
                                                                     
     /*                                                              
        stop1Zone=0;                                     
        stop2Zone=0;                                                 
        hasStop1=0;                                                  
        hasStop2=0;                                                  
        timeLeaveOrigin=0;                                           
        timeLeaveDestination=0;                                      
     */                                                              
        primaryDuration=0;                                           
                                                                     
        totalStops=0;                                                
        stop0=0;                                     
        stop1=0;                                                     
        stop2=0;                                                     
        size1=0;                                                     
        size2=0;                                                     
        size3p=0;                                                    
        inclow=0;                                                    
        incmed=0;                                                    
        inchi=0;                                                     
        auwk0=0;                                                     
        auwk1=0;                                                     
        auwk2=0;                                                     
        aupr0=0;                                                     
        aupr1=0;                                                     
        aupr2=0;                                                     
                                                                     
        noon=0;                                                      
        }                                     
   public void setAttributes(PTHousehold thisHousehold, PTPerson thisPerson, Tour thisTour){
                                                                         
       primaryDuration=0;                                           
                                                                     
       totalStops=0;                                                
       stop0=0;                                     
       stop1=0;                                                     
       stop2=0;                                                     
       size1=0;                                                     
       size2=0;                                                     
       size3p=0;                                                    
       inclow=0;                                                    
       incmed=0;                                                    
       inchi=0;                                                     
       auwk0=0;                                                     
       auwk1=0;                                                     
       auwk2=0;                                                     
       aupr0=0;                                                     
       aupr1=0;                                                     
       aupr2=0;                                                     
                                                                     
       noon=0;                                                      
                                                                        
        size=thisHousehold.size;                                      
        autos=thisHousehold.autos;                                    
        workers=thisHousehold.workers;                                
        age=thisPerson.age;                                           
        income=thisHousehold.income;                                  
        originZone=thisTour.begin.location.zoneNumber;                
        destinationZone=thisTour.primaryDestination.location.zoneNumber;
     
        primaryDuration=thisTour.primaryDestination.duration;
        tourPurpose=ActivityPurpose.ACTIVITY_PURPOSES[thisTour.primaryDestination.activityPurpose];
     
          if(thisTour.iStopsCheck(1,thisTour.tourString)==1)
               totalStops=1;
        
          if(thisTour.iStopsCheck(2,thisTour.tourString)==1)
               totalStops=2;
               
          if(totalStops==0)
               stop0=1;
          else if(totalStops==1)
               stop1=1;
          else
               stop2=1;

          if(size==1)
               size1=1;
             else if(size==2)
                  size2=1;
             else
                  size3p=1;
        
        
             if(thisHousehold.income<15000)
                  inclow=1;
             else if(thisHousehold.income>=15000 && thisHousehold.income<30000)
                  incmed=1;
             else
                  inchi=1;
                  
             if(thisHousehold.autos==0)
                  auwk0=1;
             else if(thisHousehold.autos<thisHousehold.workers)
                  auwk1=1;
             else
                  auwk2=1;
                  
             if(thisHousehold.autos==0)
                  aupr0=1;
             else if(thisHousehold.autos<thisHousehold.size)
                  aupr1=1;
             else
                  aupr2=1;

          if(thisTour.begin.endTime>1130 && thisTour.begin.endTime<1300)
               noon=1;
        }


}
     
     
