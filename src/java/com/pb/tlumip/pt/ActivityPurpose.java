/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.pt;

import org.apache.log4j.Logger;

/**
 * Defines activity purposes.
 *
 * @author    Steve Hansen
 * @version   1.0 12/01/2003
 * 
 */
public final class ActivityPurpose{
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
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
    public static final char[] ACTIVITY_PURPOSES = {'h','w','b','c','s','r','o'};

    public static final String[] DC_LOGSUM_PURPOSES = {"c1","c2","c3","s","r","o","b"};  //school,shop,recreate,other,workbased

    /** converts an activity purpose character to a number
     * 
     * @param activity activity character 
     * @return activity purpose number
     */
    public static final short getActivityPurposeValue(char activity){
        short activityPurposeValue=-1;

        for(short i=0;i<=6;i++)
        if (ACTIVITY_PURPOSES[i]==activity)
            activityPurposeValue=i;           
        return activityPurposeValue;
    }

    public static final char getActivityPurposeChar(short activityValue){
        char activity = '-';

        for(short i=0;i<ACTIVITY_PURPOSES.length;i++){
            if(activityValue == getActivityPurposeValue(ACTIVITY_PURPOSES[i])){
                activity = ACTIVITY_PURPOSES[i];
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

}