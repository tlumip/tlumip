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
 * PTTimer outputs elapsed time information to the log file
 * The methods startTimer(), endTimer(), and elapsedTimer() used to be directly in PTModel
 * 
 * @author Steve Hansen
 * @version 1.0 08/2003
 * 
 */
public class PTTimer{
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
    
    long startTime;
    long endTime;
    long elapsedTime;
    
    public PTTimer(){}
    
    
 /*   public void showMemory() {

        runningTime = System.currentTimeMillis() - markTime;
        if (logger.isLoggable(Level.FINE)) {
            logger.info("total running minutes = " + (float) ((runningTime / 1000.0) / 60.0));
            logger.info("totalMemory()=" + Runtime.getRuntime().totalMemory() + " mb.");
            logger.info("freeMemory()=" + Runtime.getRuntime().freeMemory() + " mb.");
        }

    }*/
    
    
    public void startTimer() {
        startTime = System.currentTimeMillis();
    }

    public void endTimer() {
        endTime = System.currentTimeMillis();
    }

    public long elapsedTime() {
        elapsedTime = endTime-startTime;
        return elapsedTime;
    }
        
    public void getElapsedTime() {
        elapsedTime = endTime-startTime;
        logger.info("startTime = "+startTime);
        logger.info("endTime = "+endTime);
        logger.info("elapsedTime = "+elapsedTime);
    }
    //Gets the elapsed time and starts a new timer.   
    public void getElapsedTimeFor(String getElapsedTimeString){    
        endTimer();        
        logger.info("Elapsed Time for "+getElapsedTimeString+": "+elapsedTime);        
        startTimer();
    }    
    
}