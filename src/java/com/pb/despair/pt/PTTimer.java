package com.pb.despair.pt;

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
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    
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