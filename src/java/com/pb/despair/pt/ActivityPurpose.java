package com.pb.despair.pt;

import java.util.logging.Logger;

/**
 * Defines activity purposes.
 *
 * @author    Steve Hansen
 * @version   1.0 12/01/2003
 * 
 */
public final class ActivityPurpose{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    public static final int WORK_SEGMENTS = 4;
    public static final int SCHOOL_SEGMENTS = 3;
    
    

    public static final byte HOME=0;
    public static final byte WORK=1;
    public static final byte WORK_BASED=2;
    public static final byte SCHOOL=3;
    public static final byte SHOP=4;
    public static final byte RECREATE=5;
    public static final byte OTHER=6;
    
    public static final byte TOTAL_ACTIVITIES=7;
    
    /** Array of activity purpose characters */
    public static final char[] ACTIVITY_PURPOSE = {'h','w','b','c','s','r','o'};
    
    /** converts an activity purpose character to a number 
     * 
     * @param activity activity character 
     * @return activity purpose number
     */
    public static final short getActivityPurposeValue(char activity){
        short activityPurposeValue=-1;

        for(short i=0;i<=6;i++)
        if (ACTIVITY_PURPOSE[i]==activity)
            activityPurposeValue=i;           
        return activityPurposeValue;
    }

    public static final char getActivityPurposeChar(short activityValue){
        char activity = '-';

        for(short i=0;i<ACTIVITY_PURPOSE.length;i++){
            if(activityValue == getActivityPurposeValue(ACTIVITY_PURPOSE[i])){
                activity = ACTIVITY_PURPOSE[i];
                break;
            }
        }
        return activity;
    }

    public static int getDCSegments(int purpose){
        int segments = 0;
        switch(purpose){
        case ActivityPurpose.HOME:
            segments = 1;
            break;
        case ActivityPurpose.WORK:
            segments = 4;
            break;
        case ActivityPurpose.WORK_BASED:
            segments = 1;
            break;
        case ActivityPurpose.SCHOOL:
            segments = 3;
            break;
        case ActivityPurpose.SHOP:
            segments = 1;
            break;
        case ActivityPurpose.RECREATE:
            segments = 1;
            break;    
        case ActivityPurpose.OTHER:
            segments = 1;
            break; 
        }
        return segments;
    }
    public static String getMCLogsumMatrixName(int purpose, int segment){
        //char actPurpose = ACTIVITY_PURPOSE[purpose];
        //logger.fine("purpose: "+actPurpose);

        //logger.fine("segment: "+segment);
        if(purpose==WORK||purpose==SCHOOL){
            String name = new String(String.valueOf(ACTIVITY_PURPOSE[purpose])+String.valueOf(segment)+"ls.zip");
            return name;
        }
        else {
            String name = new String(ACTIVITY_PURPOSE[purpose]+"ls.zip");
            return name;
        }
    }
    
    public static void main(String[] args){
        logger.fine("name: "+ActivityPurpose.getMCLogsumMatrixName(1,3));
        
    }
}