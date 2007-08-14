/*
 * Copyright  2006 PB Consult Inc.
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
package com.pb.tlumip.pt.ldt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.NDimensionalMatrix;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.ldt.LDTripModeType;
import com.pb.models.reference.ModelComponent;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.TreeSet;

/**
 * After LDT is run, this class is called to aggregate the trip list,
 *  aggregate them to I-J pairs, and write them to a file of the format:
 * 
 * i, j, mode, period, trips
 *  
 * 
 * @author Erhardt
 * @version 1.0 Apr 27, 2007
 *
 */
public class LDTAssignmentPreparation extends ModelComponent {
    private static Logger logger = Logger.getLogger(LDTAssignmentPreparation.class);

    private static final String ITAZ = "origin";
    private static final String JTAZ = "destination";
    private static final String MODE = "tripMode";    
    private static final String TIME = "tripStartTime";
    private static final String TRIP_TABLE_PROPERTIES_KEY="ldt.vehicle.trips";    
    private static final String OUTPUT_TABLE_PROPERTIES_KEY="ldt.vehicle.trip.matrix";    
    private static final String SAMPLE_RATE_KEY="pt.sample.rate";

    private static int NUM_PERIODS = 4; 
    private static int startAmPeak;
    private static int startMid;
    private static int startPmPeak;
    private static int startNight;
    
    private Hashtable<Integer, Integer> tazToIndex; 
    private Hashtable<Integer, Integer> indexToTaz; 
    
    private float tripWeight; 
    
    public LDTAssignmentPreparation(String appRbString) {
        appRb= ResourceBundle.getBundle(appRbString);
        globalRb= ResourceBundle.getBundle("global");      
    }
    
    /**
     * Start aggregating long-distance trips into i-j pairs for assignment.
     * 
     * This module is not distributed.
     * 
     * @param baseYear
     * @param timeInterval
     * @see com.pb.models.reference.ModelComponent#startModel(int, int)
     */
    @Override
    public void startModel(int baseYear, int timeInterval) {
        
        logger.info("Start aggregating LDT trips ");
        initPeriods();        
        initTripWeight(); 
        TableDataSet tripList = readTrips(); 
        createTazEquivalency(tripList); 
        
        processTrips(tripList); 
        logger.info("Finished aggregating LDT trips");
    }
    
    private void processTrips(TableDataSet tripList) {
                
        NDimensionalMatrix trips = createTripMatrix(); 
        
        logger.info("Tabulating " + tripList.getRowCount() + " long distance trips." );
        for (int row=1; row<=tripList.getRowCount(); row++) {
            int itaz = getOriginIndex(tripList.getValueAt(row, ITAZ)); 
            int jtaz = getDestinationIndex(tripList.getValueAt(row, JTAZ)); 
            int mode = getModeIndex(tripList.getStringValueAt(row, MODE)); 
            int period = getPeriodIndex(tripList.getValueAt(row, TIME));
            int[] location = {itaz, jtaz, mode, period}; 
            trips.incrementValue(tripWeight, location); 
        }
        
        String fileName = ResourceUtil.getProperty(globalRb, OUTPUT_TABLE_PROPERTIES_KEY);
        trips.printMatrixDelimited(",", fileName);
    }
    
    /** 
     * Reads the vehicle trip list.  
     * 
     * @return a TableDataSet with the list of trips. 
     */
    private TableDataSet readTrips() {
        String fileName = ResourceUtil.getProperty(globalRb, TRIP_TABLE_PROPERTIES_KEY);
        logger.info("Reading LDT trip list in file " + fileName); 
       
        TableDataSet tripList;
        try {
            CSVFileReader reader = new CSVFileReader(); 
            tripList = reader.readFile(new File(fileName));
            reader.close(); 
            return tripList; 
        } catch (Exception e) {
            logger.error("Error reading file " + fileName);
            e.printStackTrace(); 
        }
        
        throw new RuntimeException("Error reading file " + fileName); 
    }
    

    /** 
     * Initializes the matrix used to store the trips.  
     * 
     * @return an NDimensionalMatrix to store the trips. 
     */
    private NDimensionalMatrix createTripMatrix() {
        int numOrig = tazToIndex.size(); 
        int numDest = tazToIndex.size(); 
        int numModes = LDTripModeType.values().length;  
        int numPeriods = NUM_PERIODS;         
        int[] shape = {numOrig, numDest, numModes, numPeriods}; 
        
        NDimensionalMatrix trips = new NDimensionalMatrix("LDTrips", shape.length, shape); 
        
        // label everything
        String[] dimensionNames = {"i", "j", "mode", "period"}; 
        trips.setDimensionNames(dimensionNames);
        
        String[] tazLabels = new String[indexToTaz.size()]; 
        for (int i=0; i<tazLabels.length; i++) {
            tazLabels[i] = indexToTaz.get(i).toString();
        }
        trips.setLabels(0, tazLabels);
        trips.setLabels(1, tazLabels);
        
        return trips; 
    }
    
    private int getOriginIndex(float origin) {        
        Integer o = new Integer((int) origin);
        return tazToIndex.get(o); 
    }
    
    private int getDestinationIndex(float destination) {  
        Integer d = new Integer((int) destination);
        return tazToIndex.get(d); 
    }
           
    private int getModeIndex(String tripMode) {
        return LDTripModeType.valueOf(tripMode).ordinal(); 
    }
        
    private int getPeriodIndex(float time) {
        
        if (time < startAmPeak) {
            return 3;             // "EV"
        }
        if (time < startMid) {
            return 0;             // "AM"
        }
        if (time < startPmPeak) {
            return 1;             // "MD"
        }
        if (time < startNight) {
            return 2;             // "PM"
        }
        return 3;                 // "EV"
    }
        
    /**
     * Initialize time periods based on values defined in global.properties
     * 
     */
    private void initPeriods() {
        logger.info("Initializing time period definitions");
        
        startAmPeak = ResourceUtil.getIntegerProperty(globalRb, "AM_PEAK_START");
        startMid    = ResourceUtil.getIntegerProperty(globalRb, "OFF_PEAK_START");
        startPmPeak = ResourceUtil.getIntegerProperty(globalRb, "PM_PEAK_START");
        startNight  = ResourceUtil.getIntegerProperty(globalRb, "PM_PEAK_END") + 1;
    }

    /**
     * Reads the sample rate for weighting matrices.  
     * 
     */
    private void initTripWeight() {
        tripWeight = (float) ResourceUtil.getDoubleProperty(globalRb, SAMPLE_RATE_KEY);
    }
    
//    /** 
//     * Reads the TAZ data.  
//     *
//     */
//    private TazManager readTazData() {
//        logger.info("Reading TAZ data");
//
//        String tazManagerClass = ResourceUtil.getProperty(appRb,"taz.manager.class");
//        Class tazClass = null;
//        TazManager tazManager = null;
//        try {
//            tazClass = Class.forName(tazManagerClass);
//            tazManager = (TazManager) tazClass.newInstance();
//        } catch (Exception e){
//            throw new RuntimeException(e);
//        }
//
//        String tazClassName = appRb.getString("taz.class");
//        tazManager.setTazClassName(tazClassName);
//        tazManager.readData(globalRb, appRb);
//        
//        return tazManager; 
//    }
    
    /**
     * Creates an equivalency between the tazID, and the index
     * used in an array. 
     *
     */
    private void createTazEquivalency(TableDataSet tripList) {
        
        TreeSet<Integer> tazIDset = new TreeSet<Integer>(); 
        for (int row=1; row<=tripList.getRowCount(); row++) {
            Integer orig = (int) tripList.getValueAt(row, ITAZ);
            Integer dest = (int) tripList.getValueAt(row, JTAZ);
            tazIDset.add(orig);
            tazIDset.add(dest);
        }
           
        tazToIndex = new Hashtable<Integer, Integer>(); 
        indexToTaz = new Hashtable<Integer, Integer>(); 
        
        int index = 0; 
        for (Integer tazID : tazIDset) {
            tazToIndex.put(tazID, index); 
            indexToTaz.put(index, tazID); 
            index++; 
        }        
    }
    /**
    for testing
    **/

    public static void main(String[] args){
        LDTAssignmentPreparation ldtTrips = new LDTAssignmentPreparation("pt");
        ldtTrips.startModel(1990, 0);
    }
}
