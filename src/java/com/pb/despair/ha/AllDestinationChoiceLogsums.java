package com.pb.despair.ha;
import org.apache.log4j.Logger;

import com.borland.dx.dataset.TableDataSet;
import com.pb.common.datastore.DataManager;


/** A class that contains all destination choice logsums
 * for all purposes and market segments; 
 * readFromJDataStore puts them in Hashtable
 * @author Joel Freedman
 */
public class AllDestinationChoiceLogsums {

    protected static transient Logger logger =
        Logger.getLogger("com.pb.despair.ha");
    
    public final int maxZoneID=6000;
    static final int MAXPURPOSES1 = 9;
    static final int MAXPURPOSES2= 8;
    static final int MAXSEGMENTS = 12;
    static final int MAXZONES = 5000;
    int nextPurpose1 = 1;
    int nextPurpose2 = 1;
    int nextSegment = 1;
    int[] purposeMap1 = new int[32767]; // map char purposes to matrix
    int[] purposeMap2 = new int[32767]; // map char purposes to matrix
    int[] segmentMap = new int[32767]; // mpa char segments to matrix
    double logsums[][][][] = new double[MAXPURPOSES1][][][];
    static final String dcTableName="DC LOGSUMS";

    public AllDestinationChoiceLogsums(){
    };

    public void readFromJDataStore(){
         logger.info("\nGetting table: "+dcTableName);
         
        DataManager dm = new DataManager();  //Create a data manager, connect to default data-store
        TableDataSet table = dm.getTableDataSet(dcTableName);
 
       try {
            while (table.inBounds()) {

                 String purpose = table.getString("Purpose");
                 int segment = table.getInt("Segment");
                 int zone = table.getInt("ZoneNumber");
                 double logsum = table.getDouble("Logsum");
                 int purposeInt1;
                 int purposeInt2;
                 if (purpose.length()==1) {
                 	purposeInt1 = Math.abs((int) purpose.charAt(0));
                 	purposeInt2 = Math.abs((int) ' ');
                 } else if (purpose.length()==2) {
                 	purposeInt1 = Math.abs((int) purpose.charAt(0));
                 	purposeInt2 = Math.abs((int) purpose.charAt(1));
                 } else {
                 	logger.fatal("problem -- DC logsum \"purpose\" is not 1 or 2 char long: "+purpose);
                 	throw new RuntimeException("problem -- DC logsum \"purpose\" is not 1 or 2 char long: "+purpose);
                 }
                 if (purposeMap1[purposeInt1]==0) {
                 	logsums[nextPurpose1] = new double[MAXPURPOSES2][][];
                 	purposeMap1[purposeInt1] = nextPurpose1++;
                 }
                 if (purposeMap2[purposeInt2]==0) {
                 	purposeMap2[purposeInt2] = nextPurpose2++;
                 }
                 if (segmentMap[segment]==0) {
                 	segmentMap[segment] = nextSegment++;
                 }
                 int purposeIndex1 = purposeMap1[purposeInt1];
                 int purposeIndex2 = purposeMap2[purposeInt2];
                 int segmentIndex = segmentMap[segment];
                 if (logsums[purposeIndex1][purposeIndex2] == null) {
                 	logsums[purposeIndex1][purposeIndex2] = new double[MAXSEGMENTS][];
                 }
                 if (logsums[purposeIndex1][purposeIndex2][segmentIndex] == null) {
                 	logsums[purposeIndex1][purposeIndex2][segmentIndex] = new double[MAXZONES];
                 }
                 logsums[purposeIndex1][purposeIndex2][segmentIndex][zone] = logsum;
                 table.next();
                 
                         
            }           
            DataManager.closeTable (table);                            //Close table     
               dm.closeStore();                                                               
//        } catch (com.borland.dx.dataset.DataSetException e) {            
        } catch (Exception e) {            
            e.printStackTrace();                                         
        } 
     };  
     
     
     public double getDCLogsum(char purpose, char personSegment, int householdSegment, int zone) {
        int purposeInt1 = Math.abs((int) purpose);
        int purposeIndex1 = purposeMap1[purposeInt1];
        int purposeInt2 = Math.abs((int) personSegment);
        int purposeIndex2 = purposeMap2[purposeInt2];
        int segmentIndex= segmentMap[householdSegment];
        double logsum;
        try {
        	logsum = logsums[purposeIndex1][purposeIndex2][segmentIndex][zone];
        }
        catch (NullPointerException e) {
        	throw new RuntimeException("No DC logsum for segment "+purpose+personSegment+householdSegment+zone);
        }
        catch (ArrayIndexOutOfBoundsException e) {
        	throw new RuntimeException("HA AllDestinationChoiceLogsums needs bigger arrays "+purposeIndex1+","+purposeIndex2+","+segmentIndex+","+zone);
        }
        return logsum;
     }

        
         

        
         
}
