package com.pb.despair.pt;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**  
 * Class to access stop destination parameters from TableDataSet
 * 
 * @author Joel Freedman
 * @version 1.0 12/1/2003
 * 
 */
public class StopDestinationParametersData {
    
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     String stopDestinationParametersTableName="StopDestinationParameters";
     
     //a hashtable of taz objects
     public StopDestinationParameters[] stopDestinationParameters = new StopDestinationParameters[ActivityPurpose.ACTIVITY_PURPOSES.length];


     public static TableDataSet loadTableDataSet(ResourceBundle rb, String fileName) {
        
        try {
            String tazFile = ResourceUtil.getProperty(rb, fileName);
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(tazFile));
            return table;
        } catch (IOException e) {
            logger.fatal("Can't find TourDestinationParameters file " + fileName);
            //TODO - log exception to the node exception log file
            e.printStackTrace();
        } 
        return null;
    }
    
     public void readData(ResourceBundle rb, String fileName){
         if(logger.isDebugEnabled()) {
             logger.debug("Getting table: "+fileName);
         }
         TableDataSet table = loadTableDataSet(rb, fileName);

        for(int rowNumber = 1; rowNumber<=table.getRowCount(); rowNumber++) {
                 
            StopDestinationParameters thisPurpose = new StopDestinationParameters();
            thisPurpose.purpose       =  table.getStringValueAt(rowNumber,table.getColumnPosition("purpose"));
            thisPurpose.distanceAuto  =  table.getValueAt(rowNumber,table.getColumnPosition("distanceAuto"));
            thisPurpose.distanceWalk  =  table.getValueAt(rowNumber,table.getColumnPosition("distanceWalk"));
            thisPurpose.distanceBike  =  table.getValueAt(rowNumber,table.getColumnPosition("distanceBike"));
            thisPurpose.distanceTransit  =  table.getValueAt(rowNumber,table.getColumnPosition("distanceTransit"));
            thisPurpose.distancePowerAuto  =  table.getValueAt(rowNumber,table.getColumnPosition("distancePowerAuto"));
            thisPurpose.distancePowerWalk  =  table.getValueAt(rowNumber,table.getColumnPosition("distancePowerWalk"));
            thisPurpose.distancePowerBike  =  table.getValueAt(rowNumber,table.getColumnPosition("distancePowerBike"));
            thisPurpose.distancePowerTransit  =  table.getValueAt(rowNumber,table.getColumnPosition("distancePowerTransit"));
            thisPurpose.timeAuto      =  table.getValueAt(rowNumber,table.getColumnPosition("timeAuto"));
            thisPurpose.timeWalk      =  table.getValueAt(rowNumber,table.getColumnPosition("timeWalk"));
            thisPurpose.timeBike      =  table.getValueAt(rowNumber,table.getColumnPosition("timeBike"));
            thisPurpose.timeTransit   =  table.getValueAt(rowNumber,table.getColumnPosition("timeTransit"));
            thisPurpose.intraAuto     =  table.getValueAt(rowNumber,table.getColumnPosition("intraAuto"));
            thisPurpose.intraNonMotor =  table.getValueAt(rowNumber,table.getColumnPosition("intraNonMotor"));
            thisPurpose.intraTransit  =  table.getValueAt(rowNumber,table.getColumnPosition("intraTransit"));
            thisPurpose.retail        =  table.getValueAt(rowNumber,table.getColumnPosition("retail"));
            thisPurpose.nonRetail     =  table.getValueAt(rowNumber,table.getColumnPosition("nonRetail"));
            thisPurpose.gradeSchool   =  table.getValueAt(rowNumber,table.getColumnPosition("GradeSchool"));
            thisPurpose.hhs           =  table.getValueAt(rowNumber,table.getColumnPosition("hhs"));

            int activityValue = ActivityPurpose.getActivityPurposeValue(thisPurpose.purpose.charAt(0));
            stopDestinationParameters[activityValue] = thisPurpose;
        }
     };
                                                                          
    public static void main (String[] args) throws Exception {
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/pt/pt.properties"));
         StopDestinationParametersData tmp = new StopDestinationParametersData();
         tmp.readData(rb, "stopDestinationParameters.file");
    }
                                                                         
}                                                                        
