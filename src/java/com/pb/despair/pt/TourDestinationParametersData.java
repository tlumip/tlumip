package com.pb.despair.pt;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**  
 * A class to access Tour Destionation Parameters from TableDataSet
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TourDestinationParametersData {
     protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     String tourDestinationParametersTableName="TourDestinationParameters";

     private TourDestinationParameters tourDestinationParameters[][] = new TourDestinationParameters[ActivityPurpose.ACTIVITY_PURPOSES.length][];
     public TourDestinationParameters getParameters(int activityPurpose, int segment){
        //work segments 1,2,3, and 4 are stored in array positions 0,1,2,3 respectively
        //Same for schools segments.  that is why [segment-1]
         return tourDestinationParameters[activityPurpose][segment-1];
     }

     public static TableDataSet loadTableDataSet(ResourceBundle rb, String fileName) {
        
        try {
            String tazFile = ResourceUtil.getProperty(rb, fileName);
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(tazFile));
            return table;
        } catch (IOException e) {
            logger.fatal("Can't find TourDestinationParameters file " + fileName);
            //TODO - log this exception to the node exception log
            e.printStackTrace();
        } 
        return null;
    }
    
     public void readData(ResourceBundle rb, String fileName){
        
        tourDestinationParameters[ActivityPurpose.WORK] = new TourDestinationParameters[ActivityPurpose.WORK_SEGMENTS];
        tourDestinationParameters[ActivityPurpose.SCHOOL] = new TourDestinationParameters[ActivityPurpose.SCHOOL_SEGMENTS];
        tourDestinationParameters[ActivityPurpose.SHOP] = new TourDestinationParameters[1];
        tourDestinationParameters[ActivityPurpose.RECREATE] = new TourDestinationParameters[1];
        tourDestinationParameters[ActivityPurpose.OTHER] = new TourDestinationParameters[1];
        tourDestinationParameters[ActivityPurpose.WORK_BASED] = new TourDestinationParameters[1];
        
         if(logger.isDebugEnabled()) {
             logger.debug("Getting table: "+fileName);
         }
         TableDataSet table = loadTableDataSet(rb, fileName);
        try {
            for(int rowNumber = 1; rowNumber<=table.getRowCount(); rowNumber++) {

                 
                TourDestinationParameters thisPurpose = new TourDestinationParameters();
                thisPurpose.purpose             = table.getStringValueAt(rowNumber,table.getColumnPosition("Purpose")); 
                thisPurpose.logsum              = table.getValueAt(rowNumber,table.getColumnPosition("logsum"));
                thisPurpose.distance              = table.getValueAt(rowNumber,table.getColumnPosition("distance"));
                thisPurpose.distancePower              = table.getValueAt(rowNumber,table.getColumnPosition("distancePower"));
                thisPurpose.intraZonal          = table.getValueAt(rowNumber,table.getColumnPosition("intraZonal"));
                thisPurpose.retail              = table.getValueAt(rowNumber,table.getColumnPosition("retail"));                
                thisPurpose.nonRetail           = table.getValueAt(rowNumber,table.getColumnPosition("nonRetail"));                   
                thisPurpose.gradeSchool         = table.getValueAt(rowNumber,table.getColumnPosition("gradeSchool"));                 
                thisPurpose.secondarySchool     = table.getValueAt(rowNumber,table.getColumnPosition("secondarySchool"));             
                thisPurpose.postSecondarySchool = table.getValueAt(rowNumber,table.getColumnPosition("postSecondarySchool"));         
                thisPurpose.households          = table.getValueAt(rowNumber,table.getColumnPosition("households"));                  
                thisPurpose.office              = table.getValueAt(rowNumber,table.getColumnPosition("office"));                      
                thisPurpose.nonOffice           = table.getValueAt(rowNumber,table.getColumnPosition("nonOffice"));                   
                thisPurpose.industrial          = table.getValueAt(rowNumber,table.getColumnPosition("industrial"));                  
                thisPurpose.nonIndustrial       = table.getValueAt(rowNumber,table.getColumnPosition("nonIndustrial"));               
                thisPurpose.otherWork           = table.getValueAt(rowNumber,table.getColumnPosition("otherWork"));                                   
                
                int purpose = ActivityPurpose.getActivityPurposeValue(thisPurpose.purpose.charAt(0));
                if(thisPurpose.purpose.length()==2){
                    if(logger.isDebugEnabled()) {
                        logger.debug("first char "+thisPurpose.purpose.substring(0,1));
                        logger.debug("second char "+thisPurpose.purpose.substring(1,2));
                    }
                    String second = new String(thisPurpose.purpose.substring(1,2));
                    int segment = (Integer.valueOf(second)).intValue();
                    tourDestinationParameters[purpose][segment-1] = thisPurpose;
                }
                else tourDestinationParameters[purpose][0] = thisPurpose;
            }                                                            
        } catch (Exception e) {            
            logger.fatal("Error reading TourDestinationParameters");
            //TODO - log this exception to the node exception file
            e.printStackTrace();
        }                                                                

     };                                                                   
                                                                          

    public static void main (String[] args) throws Exception {
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/pt/pt.properties"));
//        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         TourDestinationParametersData tdpd = new TourDestinationParametersData();
         tdpd.readData(rb, "tourDestinationParameters.file");
         for(int i=1;i<ActivityPurpose.ACTIVITY_PURPOSES.length;i++){
            int length = tdpd.tourDestinationParameters[i].length;
            System.out.println("purpose " + ActivityPurpose.getActivityPurposeChar((short)i));
            for(int j=1;j<=length;j++){
                System.out.println("\tsegment " + j);
                System.out.println("\t\tlogsum  = "+tdpd.getParameters(i,j).logsum);
                System.out.println("\t\tdistance  = "+tdpd.getParameters(i,j).distance);
                System.out.println("\t\tdistancePower  = "+tdpd.getParameters(i,j).distancePower);
            }
         }                                                                          
         System.exit(1);                                                  
    }                                       
}                                                                        

