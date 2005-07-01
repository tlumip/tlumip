package com.pb.tlumip.pt;

import java.io.PrintWriter;


/** 
 * A class that contains Trip Mode Parameters
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TripModeParameters {


     public String purpose = new String();  //w=work, c=school,s=shop,r=recreate,o=other,b=workbased

        public float ivt;
        public float opclow;
        public float opcmed;
        public float opchi;
        public float pkglow;
        public float pkgmed;
        public float pkghi;
        public float opcpas;
        public float sr2hh2;
        public float sr2hh3p;
        public float sr3hh3p;
        public float driverSr2;
        public float driverSr3p;
        public float passSr3p;



     public TripModeParameters(){
        
          ivt=0;            
          opclow=0;         
          opcmed=0;         
          opchi=0;          
          pkglow=0;         
          pkgmed=0;         
          pkghi=0;          
          opcpas=0;         
          sr2hh2=0;         
          sr2hh3p=0;        
          sr3hh3p=0;        
          driverSr2=0;      
          driverSr3p=0;     
          passSr3p=0;       
        };

    public void print(PrintWriter file){
        file.println("Trip Mode Parameters: " );
        file.println("\tivt = " + ivt);
        file.println("\topclow = " + opclow);
        file.println("\topcmed = " + opcmed);
        file.println("\topchi = " + opchi);
        file.println("\tpkglow = " + pkglow);
        file.println("\tpkgmed = " + pkgmed);
        file.println("\tpkghi = " + pkghi);
        file.println("\topcpas = " + opcpas);
        file.println("\tsr2hh2 = " + sr2hh2);
        file.println("\tsr2hh3p = " + sr2hh3p);
        file.println("\tsr3hh3p = " + sr3hh3p);
        file.println("\tdriverSr2 = " + driverSr2);
        file.println("\tdriverSr3p = " + driverSr3p);
        file.println("\tpassSr3p = " + passSr3p);
        file.println();
        file.println();

        file.flush();

    }


}
                                                                   
                                                                   
                                                                   
