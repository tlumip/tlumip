package com.pb.despair.pt;

import com.pb.common.matrix.Matrix;
import com.pb.despair.model.ModeChoiceLogsums;

import java.util.logging.Logger;
import java.io.Serializable;
import java.util.ResourceBundle;

/** A class that holds a Mode Choice logsum skim matrix for 
    specific market segments in PT
    uses new Matrix package
 * @author Steve Hansen
 */
public class LogsumManager implements Serializable{

     protected static Logger logger = Logger.getLogger("com.pb.despair.pt"); 
     boolean debug = false;
     ModeChoiceLogsums[] logsums; 
     ResourceBundle rb;
     int currentWorkSegment=-1;
     int currentNonWorkSegment=-1;
    
    public LogsumManager(ResourceBundle rb){
        this.rb = rb;
        //set the array to the total number of out-home activities
        logsums = new ModeChoiceLogsums[ActivityPurpose.TOTAL_ACTIVITIES];
        
        for(int i=0;i<logsums.length;++i)
            logsums[i]= new ModeChoiceLogsums(rb);
    }
     /**
      * Use this method to update logsums; only will update logsums if they are different from the
      * segments passed into the model.
      * @param workSegment
      * @param nonWorkSegment
      */
     public void updateLogsums(int workSegment, int nonWorkSegment){
         
         if(debug) logger.fine("Free memory before updating logsums: "+Runtime.getRuntime().freeMemory());
         
         //if the workSegment is different from the current worksegment, update work Logsums
         if(workSegment!=currentWorkSegment){
            logger.info("Updating work and work-based logsums: segment "+workSegment);
            //loop through activity purposes, updating work activity logsums
            for(int i=0;i<ActivityPurpose.TOTAL_ACTIVITIES;++i){
                if(i==ActivityPurpose.WORK||i==ActivityPurpose.WORK_BASED)
                    logsums[i].readLogsums(ActivityPurpose.ACTIVITY_PURPOSE[i],workSegment);
                currentWorkSegment=workSegment;
            }
         }
     
         //if the nonWorkSegment is different from the current worksegment, update non-work Logsums
         if(nonWorkSegment!=currentNonWorkSegment){
             logger.info("Updating non-work logsums: segment "+nonWorkSegment);
            //loop through activity purposes, updating non-work activity logsums
            for(int i=0;i<ActivityPurpose.TOTAL_ACTIVITIES;++i){
                if(i!=ActivityPurpose.WORK && i!=ActivityPurpose.WORK_BASED && i!=ActivityPurpose.HOME)
                    logsums[i].readLogsums(ActivityPurpose.ACTIVITY_PURPOSE[i],nonWorkSegment);
                    currentNonWorkSegment=nonWorkSegment;
                }         
            }
         if(debug) logger.fine("Free memory after updating logsums: "+Runtime.getRuntime().freeMemory());
     
     }

    public void updateNonWorkLogsums(int nonWorkSegment){

        if(debug) logger.fine("Free memory before updating non-work logsums: "+Runtime.getRuntime().freeMemory());

        logger.info("Updating non-work logsums: segment "+nonWorkSegment);
        //loop through activity purposes, updating non-work activity logsums
        for(int i=0;i<ActivityPurpose.TOTAL_ACTIVITIES;++i){
            if(i!=ActivityPurpose.WORK && i!=ActivityPurpose.WORK_BASED && i!=ActivityPurpose.HOME)
                logsums[i].readLogsums(ActivityPurpose.ACTIVITY_PURPOSE[i],nonWorkSegment);
        }
         if(debug) logger.fine("Free memory after updating logsums: "+Runtime.getRuntime().freeMemory());

     }

    public void updateWorkLogsums(int workSegment){

        if(debug) logger.fine("Free memory before updating work and work-based logsums: "+Runtime.getRuntime().freeMemory());

        logger.info("Updating work and work-based logsums: segment "+workSegment);
        //loop through activity purposes, updating work activity logsums
        for(int i=0;i<ActivityPurpose.TOTAL_ACTIVITIES;++i){
            if(i==ActivityPurpose.WORK||i==ActivityPurpose.WORK_BASED)
                logsums[i].readLogsums(ActivityPurpose.ACTIVITY_PURPOSE[i],workSegment);
        }
     }



    public Matrix getMatrix(int ActivityPurpose){
        
        ModeChoiceLogsums mcl = logsums[ActivityPurpose];
        Matrix logsumMatrix = mcl.getMatrix();
        
        return logsumMatrix;
    }
        public static void main(String[] args) {}   
}
