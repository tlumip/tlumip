package com.pb.despair.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;


/** 
 * A class that contains pattern choice parameters
 * 
 * @author Joel Freedman
 * @version 1.0 12/1/2003
 * 
 */
public class PatternChoiceParameters {
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     String patternChoiceParametersFileName;
     String patternChoiceParametersTableName;


     String version = new String();
     float workWorker;
     float schoolStudent;
     float shopActivityDummyFemale;
     float shopActivityDummyUnemployed;
     float shopActivityDummySize1;
     float shopActivityDummySize2;
     float shopActivityDummySize3p;
     float shopActivitiesSingleParentWithChild0_5;
     float recreateActivityDummyAge0_21;
     float recreateActivityDummyAge21_25;
     float recreateActivityDummyAge25_60;
     float recreateActivityDummyWorker;
     float recreateActivityDummyUnemployed;
     float numberToursAge0_5;
     float numberToursAge5_15;
     float numberToursAge15_25;
     float numberToursAge25_50;
     float numberToursAge50_70;
     float numberToursAge70_80;
     float numberToursAge80p;
     float workStopsChildrenAge0_15;
     float workStopsIncome0_15k;
     float workStopsIncome50kp;
     float workStopsAutos0;
     float workStops_by_NWKTours;
     float workTourNotFirst;
     float schoolToursNotFirst;
     float workStops;
     float schoolStops;
     float shopStops;
     float nonWorkStopsChildrenAge0_15;
     float nonWorkStopsIncome50kp;
     float nonWorkStopsAutos0;
     float shopActivities;
     float recreateActivities;
     float otherActivities;
     float workBasedWorker;
     float shopTours;
     float otherTours;
     float workBasedTours;
     float shopToursByShopDCLogsum;
     float otherToursByOtherDCLogsum;
     float workBasedToursByWorkBasedDCLogsum;
     float homeBasedToursGT2ByWorkBasedTours;
     float shopToursAutos0;
     float otherToursAutos0;
     float workBasedToursAutos0;   
     float numberToursIfChildren0_5;
     float numberToursIfChildren5_15;
     float numberToursIncome0_15k;
     float numberToursIncome15_25k;
     float numberToursIncome25_55k;
     float numberToursIncome55kp;
     float numberToursAutos0;
     float numberToursAutosLtAdults;
     float otherActivityDummy;
     float workBasedDummy;
     float recreateDummyAge5_10;
     float shopActivitiesIncome0_15k;
     float shopActivitiesIncome15_25k;
     float shopActivitiesIncome25_55k;
     float shopActivitiesIncome55kp;
     float recreateActivitiesAge10_20;
     float recreateActivitiesAge20_30;
     float recreateActivitiesAge30_50;
     float recreateStops;
     float otherStops;
     float workDummyIndustryRetail;       
     float workDummyIndustryPersonalServices;
    float oneTour;
    float twoTours;
    float threeTours;
    float fourPlusTours;


    //constructor
    public PatternChoiceParameters(){
    }
    

    public void readData(ResourceBundle rb, String fileName){
        CSVFileReader reader = new CSVFileReader();
        try{
            if(logger.isDebugEnabled()) {
                logger.debug("Getting table: "+fileName);
            }
            String file = ResourceUtil.getProperty(rb, fileName);           
            TableDataSet table = reader.readFile(new File(file));

        
            int rowNumber = 1;
            if(logger.isDebugEnabled()) {
                logger.debug("Getting table: "+patternChoiceParametersTableName);
            }
         
            version=                               table.getStringValueAt(rowNumber, table.getColumnPosition("version")); 
            workWorker=                            table.getValueAt(rowNumber, table.getColumnPosition("workWorker"));                                       
            schoolStudent=                                      table.getValueAt(rowNumber, table.getColumnPosition("schoolStudent"));                                    
            shopActivityDummyFemale=                            table.getValueAt(rowNumber, table.getColumnPosition("shopActivityDummyFemale"));                          
            shopActivityDummyUnemployed=                        table.getValueAt(rowNumber, table.getColumnPosition("shopActivityDummyUnemployed"));                      
                    shopActivityDummySize1=                             table.getValueAt(rowNumber, table.getColumnPosition("shopActivityDummySize1"));                           
                    shopActivityDummySize2=                             table.getValueAt(rowNumber, table.getColumnPosition("shopActivityDummySize2"));                           
                    shopActivityDummySize3p=                            table.getValueAt(rowNumber, table.getColumnPosition("shopActivityDummySize3p"));                          
                    shopActivitiesSingleParentWithChild0_5=             table.getValueAt(rowNumber, table.getColumnPosition("shopActivitiesSingleParentWithChild0_5"));           
                   recreateActivityDummyAge0_21=                       table.getValueAt(rowNumber, table.getColumnPosition("recreateActivityDummyAge0_21"));                     
                   recreateActivityDummyAge21_25=                      table.getValueAt(rowNumber, table.getColumnPosition("recreateActivityDummyAge21_25"));                    
                   recreateActivityDummyAge25_60=                      table.getValueAt(rowNumber, table.getColumnPosition("recreateActivityDummyAge25_60"));                    
                   recreateActivityDummyWorker=                        table.getValueAt(rowNumber, table.getColumnPosition("recreateActivityDummyWorker"));                      
                   recreateActivityDummyUnemployed=                    table.getValueAt(rowNumber, table.getColumnPosition("recreateActivityDummyUnemployed"));                  
                   numberToursAge0_5=                                  table.getValueAt(rowNumber, table.getColumnPosition("numberToursAge0_5"));                                
                   numberToursAge5_15=                                 table.getValueAt(rowNumber, table.getColumnPosition("numberToursAge5_15"));                               
                   numberToursAge15_25=                                table.getValueAt(rowNumber, table.getColumnPosition("numberToursAge15_25"));                              
                   numberToursAge25_50=                                table.getValueAt(rowNumber, table.getColumnPosition("numberToursAge25_50"));                              
                   numberToursAge50_70=                                table.getValueAt(rowNumber, table.getColumnPosition("numberToursAge50_70"));                              
                   numberToursAge70_80=                                table.getValueAt(rowNumber, table.getColumnPosition("numberToursAge70_80"));                              
           numberToursAge80p=                                  table.getValueAt(rowNumber, table.getColumnPosition("numberToursAge80p"));                                
           workStopsChildrenAge0_15=                           table.getValueAt(rowNumber, table.getColumnPosition("workStopsChildrenAge0_15"));                         
           workStopsIncome0_15k=                               table.getValueAt(rowNumber, table.getColumnPosition("workStopsIncome0_15k"));                             
           workStopsIncome50kp=                                table.getValueAt(rowNumber, table.getColumnPosition("workStopsIncome50kp"));                              
           workStopsAutos0=                                    table.getValueAt(rowNumber, table.getColumnPosition("workStopsAutos0"));                                  
           workStops_by_NWKTours=                              table.getValueAt(rowNumber, table.getColumnPosition("workStops_by_NWKTours"));                            
                workTourNotFirst=                                   table.getValueAt(rowNumber, table.getColumnPosition("workTourNotFirst"));                                 
                schoolToursNotFirst=                                table.getValueAt(rowNumber, table.getColumnPosition("schoolToursNotFirst"));                              
                workStops=                                          table.getValueAt(rowNumber, table.getColumnPosition("workStops"));                                        
                schoolStops=                                        table.getValueAt(rowNumber, table.getColumnPosition("schoolStops"));                                      
                shopStops=                                          table.getValueAt(rowNumber, table.getColumnPosition("shopStops"));                                        
                nonWorkStopsChildrenAge0_15=                        table.getValueAt(rowNumber, table.getColumnPosition("nonWorkStopsChildrenAge0_15"));                      
                nonWorkStopsIncome50kp=                             table.getValueAt(rowNumber, table.getColumnPosition("nonWorkStopsIncome50kp"));                           
                nonWorkStopsAutos0=                                 table.getValueAt(rowNumber, table.getColumnPosition("nonWorkStopsAutos0"));                               
                shopActivities=                                     table.getValueAt(rowNumber, table.getColumnPosition("shopActivities"));                                   
                recreateActivities=                                 table.getValueAt(rowNumber, table.getColumnPosition("recreateActivities"));                               
                otherActivities=                                    table.getValueAt(rowNumber, table.getColumnPosition("otherActivities"));                                  
                workBasedWorker=                                    table.getValueAt(rowNumber, table.getColumnPosition("workBasedWorker"));                                  
                shopTours=                                          table.getValueAt(rowNumber, table.getColumnPosition("shopTours"));                                        
                otherTours=                                         table.getValueAt(rowNumber, table.getColumnPosition("otherTours"));                                       
                workBasedTours=                                     table.getValueAt(rowNumber, table.getColumnPosition("workBasedTours"));                                   
                shopToursByShopDCLogsum=                            table.getValueAt(rowNumber, table.getColumnPosition("shopToursByShopDCLogsum"));                          
                otherToursByOtherDCLogsum=                          table.getValueAt(rowNumber, table.getColumnPosition("otherToursByOtherDCLogsum"));                        
                workBasedToursByWorkBasedDCLogsum=                  table.getValueAt(rowNumber, table.getColumnPosition("workBasedToursByWorkBasedDCLogsum"));                
                homeBasedToursGT2ByWorkBasedTours=                  table.getValueAt(rowNumber, table.getColumnPosition("homeBasedToursGT2ByWorkBasedTours"));                
                shopToursAutos0=                                    table.getValueAt(rowNumber, table.getColumnPosition("shopToursAutos0"));                                  
                otherToursAutos0=                                   table.getValueAt(rowNumber, table.getColumnPosition("otherToursAutos0"));                                 
                workBasedToursAutos0=                               table.getValueAt(rowNumber, table.getColumnPosition("workBasedToursAutos0"));  
                numberToursIfChildren0_5=                              table.getValueAt(rowNumber, table.getColumnPosition("numberToursIfChildren0_5"));
                numberToursIfChildren5_15=                        table.getValueAt(rowNumber, table.getColumnPosition("numberToursIfChildren5_15"));
              numberToursIncome0_15k=                                 table.getValueAt(rowNumber, table.getColumnPosition("numberToursIncome0_15k"  ));
              numberToursIncome15_25k=                                    table.getValueAt(rowNumber, table.getColumnPosition("numberToursIncome15_25k"     ));
              numberToursIncome25_55k=                                    table.getValueAt(rowNumber, table.getColumnPosition("numberToursIncome25_55k"     ));
              numberToursIncome55kp=                                    table.getValueAt(rowNumber, table.getColumnPosition("numberToursIncome55kp"     ));
              numberToursAutos0=                                         table.getValueAt(rowNumber, table.getColumnPosition("numberToursAutos0"          ));
              numberToursAutosLtAdults=                               table.getValueAt(rowNumber, table.getColumnPosition("numberToursAutosLtAdults"));
              otherActivityDummy=                                   table.getValueAt(rowNumber, table.getColumnPosition("otherActivityDummy"));
              workBasedDummy=                                          table.getValueAt(rowNumber, table.getColumnPosition("workBasedDummy"));     
              recreateDummyAge5_10=                                   table.getValueAt(rowNumber, table.getColumnPosition("recreateDummyAge5_10")); 
              shopActivitiesIncome0_15k=                              table.getValueAt(rowNumber, table.getColumnPosition("shopActivitiesIncome0_15k")); 
              shopActivitiesIncome15_25k=                              table.getValueAt(rowNumber, table.getColumnPosition("shopActivitiesIncome15_25k")); 
              shopActivitiesIncome25_55k=                              table.getValueAt(rowNumber, table.getColumnPosition("shopActivitiesIncome25_55k")); 
              shopActivitiesIncome55kp=                               table.getValueAt(rowNumber, table.getColumnPosition("shopActivitiesIncome55kp")); 
              recreateActivitiesAge10_20=                              table.getValueAt(rowNumber, table.getColumnPosition("recreateActivitiesAge10_20")); 
              recreateActivitiesAge20_30=                              table.getValueAt(rowNumber, table.getColumnPosition("recreateActivitiesAge20_30")); 
              recreateActivitiesAge30_50=                              table.getValueAt(rowNumber, table.getColumnPosition("recreateActivitiesAge30_50"));
              recreateStops=                                          table.getValueAt(rowNumber, table.getColumnPosition("recreateStops"));
              otherStops=                                             table.getValueAt(rowNumber, table.getColumnPosition("otherStops"));
              workDummyIndustryRetail                                 = table.getValueAt(rowNumber, table.getColumnPosition("workDummyIndustryRetail"));
              workDummyIndustryPersonalServices                         = table.getValueAt(rowNumber, table.getColumnPosition("workDummyIndustryPersonalServices"));
              oneTour=                                                         table.getValueAt(rowNumber,table.getColumnPosition("oneTour"));
              twoTours=                                                         table.getValueAt(rowNumber,table.getColumnPosition("twoTours"));
              threeTours=                                                         table.getValueAt(rowNumber,table.getColumnPosition("threeTours"));
              fourPlusTours=                                                         table.getValueAt(rowNumber,table.getColumnPosition("fourPlusTours"));

        }catch(IOException e) {
            logger.fatal("Error reading PatternChoiceParameters file.");
            //TODO - log exception to the node exception file
            e.printStackTrace();
     }

      }
}  

