package com.pb.despair.pt;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import java.io.File;

import java.util.logging.Logger;
import java.io.Serializable;
import java.util.ResourceBundle;
import com.pb.common.util.ResourceUtil;

/** A class that holds expUtilities skim matrices for 
    specific market segments in PT
    uses new Matrix package
 * @author Joel Freedman
 */
public class DCExpUtilitiesManager implements Serializable{

     protected static Logger logger = Logger.getLogger("com.pb.despair.pt"); 
     boolean debug = false;
     Matrix[] expUtilities; 
     ResourceBundle rb;
     int currentWorkSegment=-1;
     int currentNonWorkSegment=-1;
     
     public static final byte WORK_BASED=0;
     public static final byte SCHOOL1=1;
  //   public static final byte SCHOOL2=0;
     public static final byte SCHOOL3=2;
     public static final byte SHOP=3;
     public static final byte RECREATE=4;
     public static final byte OTHER=5;
  
    public DCExpUtilitiesManager(ResourceBundle rb){
        this.rb = rb;
        
        int arrayLength = ActivityPurpose.WORK_SEGMENTS + ActivityPurpose.SCHOOL_SEGMENTS+
            ActivityPurpose.TOTAL_ACTIVITIES-2;
        //set the array to the total number of out-home activities
        expUtilities = new Matrix[arrayLength];
        
    }
     /**
      * Use this method to update expUtilities; only will update if they are different from the
      * segments passed into the model.
      * Note: Does not store work expUtilities at this time.
      * @param workSegment
      * @param nonWorkSegment
      */
     public int[] updateExpUtilities(int workSegment, int nonWorkSegment){
         int[] readNew = new int[2];
         if(debug) logger.fine("Free memory before updating expUtilities: "+Runtime.getRuntime().freeMemory());
         
         //if the workSegment is different from the current worksegment, update work expUtilities
         if(workSegment!=currentWorkSegment){
             readNew[0]=1;
            logger.info("Updating work-based expUtilities: segment "+workSegment);

            expUtilities[WORK_BASED]=readMatrix(new String("b"),workSegment); //BINARY-ZIP
//             expUtilities[WORK_BASED]=readBinaryMatrix(new String("b"),workSegment);
            currentWorkSegment=workSegment;
         }
     
         //if the nonWorkSegment is different from the current worksegment, update non-work expUtilities
         if(nonWorkSegment!=currentNonWorkSegment){
             readNew[1] = 1;
             logger.info("Updating non-work expUtilities: segment "+nonWorkSegment);
            
             expUtilities[SCHOOL1]=readMatrix(new String("c1"),nonWorkSegment);  //BINARY-ZIP
             expUtilities[SCHOOL3]=readMatrix(new String("c3"),nonWorkSegment);  //BINARY-ZIP
             expUtilities[SHOP]=readMatrix(new String("s"),nonWorkSegment);      //BINARY-ZIP
             expUtilities[RECREATE]=readMatrix(new String("r"),nonWorkSegment);  //BINARY-ZIP
             expUtilities[OTHER]=readMatrix(new String("o"),nonWorkSegment);    //BINARY-ZIP

//             expUtilities[SCHOOL1]=readBinaryMatrix(new String("c1"),nonWorkSegment);
//             expUtilities[SCHOOL3]=readBinaryMatrix(new String("c3"),nonWorkSegment);
//             expUtilities[SHOP]=readBinaryMatrix(new String("s"),nonWorkSegment);
//             expUtilities[RECREATE]=readBinaryMatrix(new String("r"),nonWorkSegment);
//             expUtilities[OTHER]=readBinaryMatrix(new String("o"),nonWorkSegment);

             currentNonWorkSegment=nonWorkSegment;
         }
         if(debug) logger.fine("Free memory after updating expUtilities: "+Runtime.getRuntime().freeMemory());
         return readNew;

     }
    /**
     * Read and return the exponentiated utilities for the given
     * purpose and market segment
     * @param purpose
     * @param market
     * @return the exponentiated utility Matrix for the purpose and
     * household market segment
     */
    protected Matrix readMatrix(String purpose, int market){
   
        //get path to skims
        String path = ResourceUtil.getProperty(rb, "dcExpUtilitesWrite.path");

        //construct name of file to read
        String name = new String(path+purpose+new Integer(market).toString()+"dceu.zip");
        logger.info("\t\t\tReading expUtilities Matrix "+name);
            
        MatrixReader utilityMatrixReader= MatrixReader.createReader(MatrixType.ZIP,new File(name)); 
        return utilityMatrixReader.readMatrix(name);
        
    }

    /**
     * Read and return the exponentiated utilities for the given
     * purpose and market segment
     * @param purpose
     * @param market
     * @return the exponentiated utility Matrix for the purpose and
     * household market segment
     */
    protected Matrix readBinaryMatrix(String purpose, int market){

        //get path to skims
        String path = ResourceUtil.getProperty(rb, "dcExpUtilitesWrite.path");

        //construct name of file to read
        String name = new String(path+purpose+new Integer(market).toString()+"dceu.binary");
        logger.info("\t\t\tReading expUtilities Matrix "+name);

        MatrixReader utilityMatrixReader= MatrixReader.createReader(MatrixType.BINARY,new File(name));
        return utilityMatrixReader.readMatrix(name);

    }

    /**
     * Get the matrix for the activity purpose and the activity purpose segment if purpose
     * is work or school (otherwise pass in 0).
     * @param activityPurpose
     * @param purposeSegment
     * @return the expUtilities matrix for the appropriate purpose.
     */
    public Matrix getMatrix(int activityPurpose, int purposeSegment){
        
        Matrix expMatrix;
        
        if(activityPurpose==ActivityPurpose.WORK){
            logger.severe("Error: attempting to access work dc utilities, not supported");
            throw new RuntimeException();
        }else if(activityPurpose==ActivityPurpose.WORK_BASED){
            expMatrix=expUtilities[WORK_BASED];
        }else if(activityPurpose==ActivityPurpose.SCHOOL){
            if(purposeSegment==1)
                expMatrix=expUtilities[SCHOOL1];
            else if(purposeSegment==3)
                expMatrix=expUtilities[SCHOOL3];
            else{
                logger.severe("Error: attempting to access school  dc utilities 2, not supported");
                throw new RuntimeException();
            }
        }else if(activityPurpose==ActivityPurpose.SHOP){
            expMatrix=expUtilities[SHOP];
        }else if(activityPurpose==ActivityPurpose.RECREATE){
            expMatrix=expUtilities[RECREATE];
        }else if(activityPurpose==ActivityPurpose.OTHER){
            expMatrix=expUtilities[OTHER];
        }else{
            logger.severe("Error: attempting to access purpose "+activityPurpose+", not supported");
            throw new RuntimeException();
        }
        return expMatrix;
        
    }

        public static void main(String[] args) {}   
}