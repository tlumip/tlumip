package com.pb.despair.pt.estimation;
import java.io.PrintWriter;
/**
 * DistrictTaz allows the user to set distance and logsum
 * values for Tazs to be stored in districts, for use
 * in the home location choice estimation file construction
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */     
     
     
     public class DistrictTaz extends TazOld implements Comparable{
     
          float distance;
          //Dc Logsum for each worker
          float workDcLogsum1;
          float workDcLogsum2;
          float workDcLogsum3;
          //Mc Logsum for each worker
          float mcLogsum1;
          float mcLogsum2;
          float mcLogsum3;
          
          //following variables for each zone
          float schoolDcLogsum1;
          float schoolDcLogsum2;
          float schoolDcLogsum3;
          float shopDcLogsum;
          float recreateDcLogsum;
          float otherDcLogsum;
          float workbDcLogsum;
          float probability;
          
          public DistrictTaz(){
             
          }
          /*
          * Implements compareTo, based on distance,
          * for sorting
          *
          */          
          public int compareTo(Object taz){
               int rv=-1;
               float compareDistance = ((DistrictTaz)taz).distance;
               if(compareDistance==distance)     rv=0;
               if(compareDistance>distance) rv=1;
               return rv;
          }
          
          public void print(PrintWriter p){
               
               super.print(p);
     
               p.print(
                    " "+ distance
                    +" "+ workDcLogsum1
                    +" "+ workDcLogsum2
                    +" "+ workDcLogsum3
                    +" "+ mcLogsum1
                    +" "+ mcLogsum2
                    +" "+ mcLogsum3
                    +" "+ schoolDcLogsum1
                    +" "+ schoolDcLogsum2
                    +" "+ schoolDcLogsum3
                    +" "+ shopDcLogsum
                    +" "+ recreateDcLogsum
                    +" "+ otherDcLogsum
                    +" "+ workbDcLogsum
                    +" "+ probability
               );
     
          }     
          public void setVariables(TazOld t){
               zoneNumber                      =t.zoneNumber                    ;
               population                      =t.population                    ;
               households                      =t.households                    ;
                agriculture_Office              =t.agriculture_Office            ;
               agriculture_Agriculture         =t.agriculture_Agriculture       ;
               fFM_Office                      =t.fFM_Office                    ;
               fFM_Forest                      =t.fFM_Forest                    ;
               lightIndustry_Office            =t.lightIndustry_Office          ;
               lightIndustry_LightIndustrial   =t.lightIndustry_LightIndustrial ;
               heavyIndustry_Office            =t.heavyIndustry_Office          ;
               heavyIndustry_HeavyIndustrial   =t.heavyIndustry_HeavyIndustrial ;
               wholesale_Office                =t.wholesale_Office              ;
               wholesale_Warehouse             =t.wholesale_Warehouse           ;
               retail_Office                   =t.retail_Office                 ;
               retail_Retail                   =t.retail_Retail                 ;
               hotel_Hotel                     =t.hotel_Hotel                   ;
               construction_Construction       =t.construction_Construction     ;
               healthCare_Office               =t.healthCare_Office             ;
               healthCare_Hospital             =t.healthCare_Hospital           ;
               healthCare_Institutional        =t.healthCare_Institutional      ;
               transportation_Office           =t.transportation_Office         ;
               transportation_DepotSpace       =t.transportation_DepotSpace     ;
               otherServices_Office            =t.otherServices_Office          ;
               otherServices_LightIndustrial   =t.otherServices_LightIndustrial ;
               otherServices_Retail            =t.otherServices_Retail          ;
               gradeSchool_Office              =t.gradeSchool_Office            ;
               gradeSchool_GradeSchool         =t.gradeSchool_GradeSchool       ;
               postSecondary_Institutional     =t.postSecondary_Institutional   ;
               government_Office               =t.government_Office             ;
               government_GovernmentSupport    =t.government_GovernmentSupport  ;
               government_Institutional        =t.government_Institutional      ;
               workParkingCost                 =t.workParkingCost               ;
               nonWorkParkingCost              =t.nonWorkParkingCost            ;
               acres                           =t.acres                         ;
               pricePerAcre                           =t.pricePerAcre                  ;
               pricePerSqFtSFD                     =t.pricePerSqFtSFD               ;
               singleFamilyHH                          =t.singleFamilyHH                ;
               multiFamilyHH                     =t.multiFamilyHH                 ;

               
          };
               
               
     } 
