package com.pb.despair.pt;


/**  
 * A class that contains tour Mode Parameters
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TourModeParameters {


     public String purpose = new String();  //w=work, c=school,s=shop,r=recreate,o=other,b=workbased
     public float ivt;     //InVehicleTime
     public float wlk;       //Transit walk time
     public float shfwt;   //ShortFirstWaitTime
     public float lgfwt;   //LongFirstWaitTime
     public float xwt;     //SecondWaitTime
     public float dvt;     //DriveTime
     public float shwlk;   //ShortWalkTime
     public float lgwlk;   //LongWalkTime
     public float bmt;     //BikeModeTime
     public float pkglow;  //PkgIncome0-30
     public float pkgmed;  //PkgIncome30-60
     public float pkghi;   //PkgIncome60k
     public float opclow;  //OPCIncome0-30
     public float opcmed;  //OPCIncome30-60
     public float opchi;   //OPCIncome60k
     public float opcpas;  //OPCPassenger
     public float passtp;  //PassengerStops
     public float wlkstp;  //WalkStops
     public float bikstp;  //BikeStops
     public float wktstp;  //WlkTrnStops
     public float trpstp;   //TransitPassengerStops
     public float ptrstp;    //PassengerTransitStops
     public float drtstp;  //DrvTrnStops
     public float pasgt16; //Passenger if Age>16
     public float pashh1;    //Passenger if hhsize=1
     public float pashh2;  //AutoPassHH2+
     public float pashh3p;
     public float trphh1;  //TransitPassengerHH1
     public float trphh2;  //TransitPassengerHH2+
     public float trphh3p;
     public float ptrhh1;    //PassengerTransitHH
     public float ptrhh2;    //PassengerTransitHH2+
     public float ptrhh3p;
     public float dra1619;  //Driver age 16-19
     public float tra10;   //Transit age <=10
     public float noon;   //indicator for noon trip (work-based tours)
     public float drvaw0;   //Driver - Autos\Workers 0
     public float drvaw1;   //Driver - Autos\Workers 1
     public float drvaw2;   //Driver - Autos\Workers 2
     public float pasaw0;   //Pass - Autos\Workers 0
     public float pasaw1;   //Pass - Autos\Workers 1
     public float pasaw2;   //Pass - Autos\Workers 2
     public float wlkaw0;   //Walk - Autos\Workers 0
     public float wlkaw1;   //Walk - Autos\Workers 1
     public float wlkaw2;   //Walk - Autos\Workers 2
     public float bikaw0;   //Bike - Autos\Workers 0
     public float bikaw1;   //Bike - Autos\Workers 1
     public float bikaw2;   //Bike - Autos\Workers 2
     public float wktaw0;   //WlkTran - Autos\Workers 0
     public float wktaw1;   //WlkTran - Autos\Workers 1
     public float wktaw2;   //WlkTran - Autos\Workers 2
     public float trpaw0;   //TranPas - Autos\Workers 0
     public float trpaw1;   //TranPas - Autos\Workers 1
     public float trpaw2;   //TranPas - Autos\Workers 2
     public float ptraw0;   //PasTran - Autos\Workers 0
     public float ptraw1;   //PasTran - Autos\Workers 1
     public float ptraw2;   //PasTran - Autos\Workers 2
     public float drtaw0;   //DrvTran - Autos\Workers 0
     public float drtaw1;   //DrvTran - Autos\Workers 1
     public float drtaw2;   //DrvTran - Autos\Workers 2
     public float drvap0;   //Driver - Autos\Persons 0
     public float drvap1;   //Driver - Autos\Persons 1
     public float drvap2;   //Driver - Autos\Persons 2
     public float pasap0;   //Pass - Autos\Persons 0
     public float pasap1;   //Pass - Autos\Persons 1
     public float pasap2;   //Pass - Autos\Persons 2
     public float wlkap0;   //Walk - Autos\Persons 0
     public float wlkap1;   //Walk - Autos\Persons 1
     public float wlkap2;   //Walk - Autos\Persons 2
     public float bikap0;   //Bike - Autos\Persons 0
     public float bikap1;   //Bike - Autos\Persons 1
     public float bikap2;   //Bike - Autos\Persons 2
     public float wktap0;   //WlkTran - Autos\Persons 0
     public float wktap1;   //WlkTran - Autos\Persons 1
     public float wktap2;   //WlkTran - Autos\Persons 2
     public float trpap0;   //TranPas - Autos\Persons 0
     public float trpap1;   //TranPas - Autos\Persons 1
     public float trpap2;   //TranPas - Autos\Persons 2
     public float ptrap0;   //PasTran - Autos\Persons 0
     public float ptrap1;   //PasTran - Autos\Persons 1
     public float ptrap2;   //PasTran - Autos\Persons 2
     public float drtap0;   //DrvTran - Autos\Persons 0
     public float drtap1;   //DrvTran - Autos\Persons 1
     public float drtap2;   //DrvTran - Autos\Persons 2
     public float nestlow;   //NestLow
     

     public TourModeParameters(){
        
             ivt=0;    
          wlk=0;      
          shfwt=0;  
          lgfwt=0;  
          xwt=0;    
          dvt=0;    
          shwlk=0;  
          lgwlk=0;  
          bmt=0;    
          pkglow=0; 
          pkgmed=0; 
          pkghi=0;  
          opclow=0; 
          opcmed=0; 
          opchi=0;  
          opcpas=0; 
          passtp=0; 
          wlkstp=0; 
          bikstp=0; 
          wktstp=0; 
          trpstp=0; 
          ptrstp=0; 
          drtstp=0; 
          pasgt16=0;
          pashh1=0; 
          pashh2=0; 
          pashh3p=0;
          trphh1=0; 
          trphh2=0; 
          trphh3p=0;
          ptrhh1=0; 
          ptrhh2=0; 
          ptrhh3p=0;
          dra1619=0;
          tra10=0;  
          noon=0;   
          drvaw0=0; 
          drvaw1=0; 
          drvaw2=0; 
          pasaw0=0; 
          pasaw1=0; 
          pasaw2=0; 
          wlkaw0=0; 
          wlkaw1=0; 
          wlkaw2=0; 
          bikaw0=0; 
          bikaw1=0; 
          bikaw2=0; 
          wktaw0=0; 
          wktaw1=0; 
          wktaw2=0; 
          trpaw0=0; 
          trpaw1=0; 
          trpaw2=0; 
          ptraw0=0; 
          ptraw1=0; 
          ptraw2=0; 
          drtaw0=0; 
          drtaw1=0; 
          drtaw2=0; 
          drvap0=0; 
          drvap1=0; 
          drvap2=0; 
          pasap0=0; 
          pasap1=0; 
          pasap2=0; 
          wlkap0=0; 
          wlkap1=0; 
          wlkap2=0; 
          bikap0=0; 
          bikap1=0; 
          bikap2=0; 
          wktap0=0; 
          wktap1=0; 
          wktap2=0; 
          trpap0=0; 
          trpap1=0; 
          trpap2=0; 
          ptrap0=0; 
          ptrap1=0; 
          ptrap2=0; 
          drtap0=0; 
          drtap1=0; 
          drtap2=0; 
          nestlow=0;
     
        
        };
   

}                                                                  
                                                                   
                                                                   
                                                                   
