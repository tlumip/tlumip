package com.pb.despair.pt.old;
//import com.borland.dx.dataset.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
//import com.pb.common.datastore.DataManager;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pt.StopDestinationParametersData;
import com.pb.despair.pt.TazData;
import com.pb.despair.pt.TourDestinationParameters;
import com.pb.despair.pt.TourDestinationParametersData;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import java.util.Enumeration;

/** 
 * Class to access TAZ information from CSV File 
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TazDataOld {

    String tazDataTableName="TazData";
     
    //a hashtable of taz objects
    public Hashtable tazData = new Hashtable();
    
    protected LogitModel tourDestinationChoiceModel;
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     
     public boolean hasTaz(int zoneNumber){
          
          return tazData.containsKey(new Integer(zoneNumber));
     }
     
    public static TableDataSet loadTableDataSet(String resourceBundle, String fileName) {
        
        try {
            ResourceBundle rb = ResourceUtil.getResourceBundle(resourceBundle);
            String tazFile = ResourceUtil.getProperty(rb, fileName);
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(tazFile));
            return table;
        } catch (IOException e) {
            logger.fatal("Can't find TazData input table " + fileName);
            e.printStackTrace();
        }
        return null;
    }
    
     public void readData(String resourceBundle, String fileName){
         logger.info("Getting table: "+tazDataTableName);
         TableDataSet table = loadTableDataSet(resourceBundle, fileName);

             for(int rowNumber=1;rowNumber<=table.getRowCount();rowNumber++) {
                 TazOld thisTaz = new TazOld();
                 thisTaz.zoneNumber                  =(int) table.getValueAt(rowNumber,table.getColumnPosition("ZoneNumber"));
                 thisTaz.population                     =table.getValueAt(rowNumber,table.getColumnPosition("Population"));                                        
                 thisTaz.households                     =table.getValueAt(rowNumber,table.getColumnPosition("Households"));                       
                 thisTaz.agriculture_Office             =table.getValueAt(rowNumber,table.getColumnPosition("Agriculture_Office"));               
                 thisTaz.agriculture_Agriculture        =table.getValueAt(rowNumber,table.getColumnPosition("agriculture_Agriculture"));                      
                 thisTaz.fFM_Office                     =table.getValueAt(rowNumber,table.getColumnPosition("FFM_Office"));                                                
                 thisTaz.fFM_Forest                     =table.getValueAt(rowNumber,table.getColumnPosition("FFM_Forest"));                                                
                 thisTaz.lightIndustry_Office           =table.getValueAt(rowNumber,table.getColumnPosition("LightIndustry_Office"));                            
                 thisTaz.lightIndustry_LightIndustrial  =table.getValueAt(rowNumber,table.getColumnPosition("LightIndustry_LightIndustrial"));          
                 thisTaz.heavyIndustry_Office           =table.getValueAt(rowNumber,table.getColumnPosition("HeavyIndustry_Office"));                            
                 thisTaz.heavyIndustry_HeavyIndustrial  =table.getValueAt(rowNumber,table.getColumnPosition("HeavyIndustry_HeavyIndustrial"));          
                 thisTaz.wholesale_Office               =table.getValueAt(rowNumber,table.getColumnPosition("Wholesale_Office"));                                    
                 thisTaz.wholesale_Warehouse            =table.getValueAt(rowNumber,table.getColumnPosition("Wholesale_Warehouse"));                              
                 thisTaz.retail_Office                  =table.getValueAt(rowNumber,table.getColumnPosition("Retail_Office"));                                          
                 thisTaz.retail_Retail                  =table.getValueAt(rowNumber,table.getColumnPosition("Retail_Retail"));                                          
                 thisTaz.hotel_Hotel                    =table.getValueAt(rowNumber,table.getColumnPosition("Hotel_Hotel"));                                              
                 thisTaz.construction_Construction      =table.getValueAt(rowNumber,table.getColumnPosition("Construction_Construction"));                  
                 thisTaz.healthCare_Office              =table.getValueAt(rowNumber,table.getColumnPosition("HealthCare_Office"));                                  
                 thisTaz.healthCare_Hospital            =table.getValueAt(rowNumber,table.getColumnPosition("HealthCare_Hospital"));                              
                 thisTaz.healthCare_Institutional       =table.getValueAt(rowNumber,table.getColumnPosition("HealthCare_Institutional"));                    
                 thisTaz.transportation_Office          =table.getValueAt(rowNumber,table.getColumnPosition("Transportation_Office"));                          
                 thisTaz.transportation_DepotSpace      =table.getValueAt(rowNumber,table.getColumnPosition("Transportation_DepotSpace"));                  
                 thisTaz.otherServices_Office           =table.getValueAt(rowNumber,table.getColumnPosition("OtherServices_Office"));                            
                 thisTaz.otherServices_LightIndustrial  =table.getValueAt(rowNumber,table.getColumnPosition("OtherServices_LightIndustrial"));          
                 thisTaz.otherServices_Retail           =table.getValueAt(rowNumber,table.getColumnPosition("OtherServices_Retail"));                            
                 thisTaz.gradeSchool_Office             =table.getValueAt(rowNumber,table.getColumnPosition("GradeSchool_Office"));                                
                 thisTaz.gradeSchool_GradeSchool        =table.getValueAt(rowNumber,table.getColumnPosition("GradeSchool_GradeSchool"));                      
                 thisTaz.postSecondary_Institutional    =table.getValueAt(rowNumber,table.getColumnPosition("PostSecondary_Institutional"));              
                 thisTaz.government_Office              =table.getValueAt(rowNumber,table.getColumnPosition("Government_Office"));                                  
                 thisTaz.government_GovernmentSupport   =table.getValueAt(rowNumber,table.getColumnPosition("Government_GovernmentSupport"));            
                 thisTaz.government_Institutional       =table.getValueAt(rowNumber,table.getColumnPosition("Government_Institutional"));                    
                 thisTaz.workParkingCost                =table.getValueAt(rowNumber,table.getColumnPosition("WorkParkingCost"));                                      
                 thisTaz.nonWorkParkingCost             =table.getValueAt(rowNumber,table.getColumnPosition("NonWorkParkingCost"));                                
                 thisTaz.acres                          =table.getValueAt(rowNumber,table.getColumnPosition("Acres"));                        
                 thisTaz.pricePerAcre                   =table.getValueAt(rowNumber,table.getColumnPosition("PricePerAcre"));
                 thisTaz.pricePerSqFtSFD                =table.getValueAt(rowNumber,table.getColumnPosition("PricePerSqFtSFD"));
                 thisTaz.singleFamilyHH                 =table.getValueAt(rowNumber,table.getColumnPosition("SingleFamilyHH"));
                 thisTaz.multiFamilyHH                  =table.getValueAt(rowNumber,table.getColumnPosition("MultiFamilyHH"));
                    
                 tazData.put(new Integer(thisTaz.zoneNumber),thisTaz);                                                     
                    
             }
         
     }
    public void collapseEmployment(TourDestinationParametersData tdpd,
                                   StopDestinationParametersData sdpd){
                
        //collapse employment categories for tazs
        logger.info("Collapsing employment");
        Enumeration tazEnum=tazData.elements();
        while(tazEnum.hasMoreElements()){
            TazOld thisTaz = (TazOld) tazEnum.nextElement();
            thisTaz.collapseEmployment();
            thisTaz.setLnAcres();
        }
        //TODO separate this
        setSizeTerms(tdpd,sdpd);
    }
    
    public void setSizeTerms(TourDestinationParametersData tdpd,
                             StopDestinationParametersData sdpd){
        logger.info("Setting Size Terms");
        Enumeration tazEnum=tazData.elements();
        while(tazEnum.hasMoreElements()){
            TazOld thisTaz = (TazOld) tazEnum.nextElement();
            thisTaz.setTourSizeTerms(tdpd);
            thisTaz.setStopSizeTerms(sdpd);
        }
    }
    public int[] getExternalNumberArray(){
        //Set up external numbers         
        int tazCounter=1;
        //create an array lookup to store all tazs
        int[] lookup = new int[tazData.size()+1];
        
        //enumerate through all tazs and sort in lookup array 
        Enumeration tazEnum=tazData.elements();        
        while(tazEnum.hasMoreElements()){
            TazOld thisTaz = (TazOld) tazEnum.nextElement();
            lookup[tazCounter]=thisTaz.zoneNumber;
            tazCounter++;
        }
        //sort array        
        Arrays.sort(lookup);
        return lookup;
    }
    
    public void setLogitModel(){
        tourDestinationChoiceModel = new LogitModel("destinationModel",tazData.size());
        Enumeration destinationEnum=tazData.elements();
        while(destinationEnum.hasMoreElements()){
            TazOld destinationTaz = (TazOld) destinationEnum.nextElement();
            tourDestinationChoiceModel.addAlternative(destinationTaz);
        }     
    }
    
    public LogitModel getLogitModel(){
        return tourDestinationChoiceModel;   
    }
     
    public static void main (String[] args) throws Exception {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
         TazData taz = new TazData();
         taz.readData(rb,globalRb,"tazData.file");
         logger.info("Finished reading TazData Table");
         //read the tourDestinationParameters from csv to TableDataSet
         logger.info("Reading tour destination parameters");
         TourDestinationParametersData tdpd = new TourDestinationParametersData();
         tdpd.readData(rb,"tourDestinationParameters.file");
         //read the stopDestinationParameters from csv to TableDataSet
         logger.info("Reading stop destination parameters");
         StopDestinationParametersData sdpd = new StopDestinationParametersData();
         sdpd.readData(rb,"stopDestinationParameters.file");
         taz.collapseEmployment(tdpd,sdpd); 
         TourDestinationParameters tdp = tdpd.getParameters(1,3);
         Enumeration tazEnum=taz.tazData.elements();
         TazOld testTaz = (TazOld)tazEnum.nextElement();
         
         
         //Note - this takes 37 seconds with old utility calcs and 4 seconds with new utility calcs
         long startTime = System.currentTimeMillis();
         for(int i=0;i<100000000;i++){
            testTaz.calcTourDestinationUtility(1,3,tdp,0.1);
         }
         logger.info("Total time = "+(System.currentTimeMillis()-startTime)/1000+" seconds.");
         System.exit(1);
    };

}
