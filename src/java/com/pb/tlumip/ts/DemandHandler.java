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
package com.pb.tlumip.ts;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 6/30/2004
 */


import com.pb.models.pt.TripModeType;


import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.rpc.DafNode;
import com.pb.common.matrix.ZipMatrixWriter;
import com.pb.common.matrix.Matrix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import org.apache.log4j.Logger;



public class DemandHandler implements DemandHandlerIF, Serializable {

	protected static transient Logger logger = Logger.getLogger(DemandHandler.class);

    public static final String DEMAND_OUTPUT_MODE_STRING = "{MODE}";
    public static final String DEMAND_OUTPUT_TIME_PERIOD_STRING = "{PERIOD}";

    static final double AVERAGE_SR3P_AUTO_OCCUPANCY = 3.33;
    static final int MAX_TRUCK_CLASSES = 5;
    
    
    int maxDistrict;
    String[] districtNames;
    int[] alphaDistrictIndex;
    int networkNumCentroids;
    int networkNumUserClasses;
    int[] networkNodeIndexArray;
    int[] networkIndexNodeArray;
    boolean networkUserClassesIncludeTruck;
    char[] highwayModeCharacters;
    char[][] networkAssignmentGroupChars;
    
    double ptSampleRate;
    
    String demandOutputFileName;
    String sdtFileName;
    String ldtFileName;
    String ctFileName;
    String etFileName;
    String timePeriod;
    int startHour;
    int endHour;

    float[] userClassPces;

    double[][][] multiclassVehicleTripTable = null;
    
    double[][] multiclassVehicleTripTableRowSums = null;

	double[][][] multiclassVehicleDistrictTable = null;
    


    public DemandHandler() {
    }

    
    // Factory Method to return either local or remote instance
    public static DemandHandlerIF getInstance( String rpcConfigFile ) {
    
        if ( rpcConfigFile == null ) {

            // if rpc config file is null, then all handlers are local, so return local instance
            return new DemandHandler();

        }
        else {
            
            // return either a local instance or an rpc instance depending on how the handler was defined.
            Boolean isLocal = DafNode.getInstance().isHandlerLocal( HANDLER_NAME );

            if ( isLocal == null )
                // handler name not found in config file, so create a local instance.
                return new DemandHandler();
            else 
                // handler name found in config file but is not local, so create an rpc instance.
                return new DemandHandlerRpc( rpcConfigFile );

        }
        
    }
    

    // Factory Method to return local instance only
    public static DemandHandlerIF getInstance() {
        return new DemandHandler();
    }
    

   
    public boolean setup( float[] userClassPces, String demandOutputFileName, String sdtFileName, String ldtFileName, double ptSampleRate, String ctFileName, String etFileName, int startHour, int endHour, String timePeriod, int numCentroids, int numUserClasses, int[] indexNodeArray, int[] nodeIndexArray, int[] alphaDistrictArray, String[] alphaDistrictNames, char[][] assignmentGroupChars, char[] highwayModeCharacters, boolean userClassesIncludeTruck ) {
        
        this.userClassPces =  userClassPces; 
        networkNumCentroids = numCentroids;
        networkNumUserClasses = numUserClasses;
        networkAssignmentGroupChars = assignmentGroupChars;
        networkNodeIndexArray = nodeIndexArray;
        networkIndexNodeArray = indexNodeArray;
        networkUserClassesIncludeTruck = userClassesIncludeTruck;
        this.highwayModeCharacters = highwayModeCharacters;
        this.timePeriod = timePeriod;
        this.demandOutputFileName = demandOutputFileName;
        this.sdtFileName = sdtFileName;
        this.ldtFileName = ldtFileName;
        this.ctFileName = ctFileName;
        this.etFileName = etFileName;
        this.startHour = startHour;
        this.endHour = endHour;
        this.ptSampleRate = ptSampleRate;

        alphaDistrictIndex = alphaDistrictArray;
        districtNames = alphaDistrictNames;

        maxDistrict = 0;
        for (int i=0; i < alphaDistrictArray.length; i++)
            if ( alphaDistrictArray[i] > maxDistrict )
                maxDistrict = alphaDistrictArray[i];
        
        return true;
        
    }

    
    
    // this method called by methods running in a different VM and thus making a remote method call to setup this object
    public boolean setupRpc( float[] userClassPces, String demandOutputFileName, String sdtFileName, String ldtFileName, double ptSampleRate, String ctFileName, String etFileName, int startHour, int endHour, String timePeriod, int numCentroids, int numUserClasses, int[] indexNodeArray, int[] nodeIndexArray, int[] alphaDistrictArray, String[] alphaDistrictNames, char[][] assignmentGroupChars, char[] highwayModeCharacters, boolean userClassesIncludeTruck ) {
        
        return setup( userClassPces, demandOutputFileName, sdtFileName, ldtFileName, ptSampleRate, ctFileName, etFileName, startHour, endHour, timePeriod, numCentroids, numUserClasses, indexNodeArray, nodeIndexArray, alphaDistrictArray, alphaDistrictNames, assignmentGroupChars, highwayModeCharacters, userClassesIncludeTruck );
    
    }
    
    
    
    public boolean buildHighwayDemandObject() {
        
        int i=0;
        int j=0;
        int k=0;
        
        // load the trips from PT and CT trip lists into multiclass o/d demand matrices for assignment
        try {

            logger.info ( "creating demand trip tables from " + sdtFileName + ", " + ldtFileName + ", and " + ctFileName + " for the " + timePeriod + " period." );
            
            
            multiclassVehicleDistrictTable = new double[networkNumUserClasses][maxDistrict+1][maxDistrict+1];
            
            // read in the trip lists
            multiclassVehicleTripTable = createMulticlassDemandMatrices ();
            
            
            multiclassVehicleTripTableRowSums = new double[multiclassVehicleTripTable.length][multiclassVehicleTripTable[0].length];
            
            // summarize the trip table rows for each user class
            for (i=0; i < multiclassVehicleTripTable.length; i++)
                for (j=0; j < multiclassVehicleTripTable[i].length; j++)
                    for (k=0; k < multiclassVehicleTripTable[i][j].length; k++)
                        multiclassVehicleTripTableRowSums[i][j] += multiclassVehicleTripTable[i][j][k];
            
            
            return true;
            
        }
        catch (Exception e) {
            
            logger.error ("num zones = " + networkNumCentroids);
            logger.error ("error building multiclass od demand matrices for " + timePeriod + " period.");
            logger.error ("multiclassTripTable.length=" + multiclassVehicleTripTable.length);
            logger.error ("multiclassTripTable[0].length=" + multiclassVehicleTripTable[0].length);
            logger.error ("multiclassTripTable[0][0].length=" + multiclassVehicleTripTable[0][0].length);
            logger.error ("multiclassTripTableRowSums.length=" + multiclassVehicleTripTableRowSums.length);
            logger.error ("multiclassTripTableRowSums[0].length=" + multiclassVehicleTripTableRowSums[0].length);
            logger.error ("i=" + i + ", j=" + j + ", k=" + k, e);
            throw new RuntimeException();
            
        }
        
    }
    
    
    public double[] getTripTableRow ( int userClass, int row ) {
        return multiclassVehicleTripTable[userClass][row];
    }
    
    
    public double[] getTripTableRowRpc ( int userClass, int row ) {
        double[] tripRow = getTripTableRow ( userClass, row );
        return tripRow;
    }
    
    
    public double[][][] getMulticlassTripTables () {
        return multiclassVehicleTripTable;
    }
    
    
    public double[][][] getMulticlassTripTablesRpc () {
        double[][][] tripTables = getMulticlassTripTables();
        return tripTables;
    }
    
    
    public double[][] getTripTableRowSums () {
        return multiclassVehicleTripTableRowSums;
    }
    
    
    public double[][] getTripTableRowSumsRpc () {
        double[][] rowSums = getTripTableRowSums();
        return rowSums;
    }
    
    private double[][][] createMulticlassDemandMatrices () {
        
		String myDateString;
		
    	double[][][] multiclassTripTable = new double[networkNumUserClasses][][];
		

		// check that at least one valid user class has been defined
		if ( networkNumUserClasses == 0 ) {
			logger.error ( "No valid user classes defined in the application properties file.", new RuntimeException() );
		}
		
		
        
		// read PT trip list into o/d trip matrix if auto user class was defined
        boolean assignmentGroupContainsAuto = false;
        for (int i=0; i < networkAssignmentGroupChars[0].length; i++) {
            if ( networkAssignmentGroupChars[0][i] == 'a' ) {
                assignmentGroupContainsAuto = true;
                break;
            }
        }
                
		if ( assignmentGroupContainsAuto ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ( String.format("reading %s PT trip list at: %s, %d zones in trip tables.", timePeriod, myDateString, networkNumCentroids ) );
            ArrayList<String> tripModeList = new ArrayList<String>();
            tripModeList.add( String.valueOf(TripModeType.DA) );
            tripModeList.add( String.valueOf(TripModeType.SR2) );
            tripModeList.add( String.valueOf(TripModeType.SR3P) );
            
			multiclassTripTable[0] = getTripTablesForModes ( tripModeList );
		}
		else {
			logger.info ("no auto class defined, so " + timePeriod + " PT trip list was not read." );
		}

        
        // read CT and ET trip list into o/d trip matrix if at least one truck class was defined
        // make sure the trips in ET file have a truck class defined
        if ( networkUserClassesIncludeTruck ) {

            myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("reading " + timePeriod + " CT trip list at: " + myDateString);
            double[][][] truckTripTables = getTruckAssignmentGroupTripTableFromCTList ();

            // read ET trip list and accumulate into o/d truck trip matrices.
            myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("reading " + timePeriod + " ET trip list at: " + myDateString);
            truckTripTables = getExternalAssignmentGroupTripTableFromETList ( truckTripTables );

            
            for(int i=0; i < truckTripTables.length - 1; i++)
                multiclassTripTable[i+1] = truckTripTables[i];
            
        }



        // if a file name has been specified for the output demand file, write the file.
        if ( demandOutputFileName != null ){
            for( int m=0; m < multiclassTripTable.length; m++ ) {
                File outputFile = new File(demandOutputFileName.replace(DEMAND_OUTPUT_MODE_STRING,"" + highwayModeCharacters[m])
                                                               .replace(DEMAND_OUTPUT_TIME_PERIOD_STRING,timePeriod));
                logger.info("Writing demand matrix: " + outputFile);
                float[][] demandMatrix = new float[multiclassTripTable[m].length][multiclassTripTable[m][0].length];
                for (int i = 0; i < demandMatrix.length; i++)
                    for (int j = 0; j < demandMatrix[0].length; j++)
                        demandMatrix[i][j] = (float) multiclassTripTable[m][i][j];
                ZipMatrixWriter zmw = new ZipMatrixWriter(outputFile);
                String mName = outputFile.getName();
                mName = mName.substring(0,mName.indexOf("."));
                zmw.writeMatrix(new Matrix(mName,mName,demandMatrix));
            }
        }
        

        return multiclassTripTable;
		
    }
	
	

    
    public double[][] getTripTableForMode ( String tripModeString ) {

        // this method gets called for each modal transit assignment - it does not get called when reading highway demand
        multiclassVehicleDistrictTable = new double[1][maxDistrict+1][maxDistrict+1];
        
        ArrayList<String> tripModes = new ArrayList<String>();
        tripModes.add(tripModeString);
        
        return getTripTablesForModes ( tripModes );
        
    }

    
    public double[][] getTripTablesForModes ( ArrayList<String> tripModes ) {
        
        double[][] sdtTripTable = getTripTableFromSdtLdtListsForModes ( tripModes, sdtFileName );
        double[][] ldtTripTable = getTripTableFromSdtLdtListsForModes ( tripModes, ldtFileName );

        // add ldt trips to sdt trip table and return that combined table
        for (int o=0; o < sdtTripTable.length; o++)
            for (int d=0; d < sdtTripTable[o].length; d++)
                sdtTripTable[o][d] += ldtTripTable[o][d];
        
        return sdtTripTable;
            
    }
    

    private double[][] getTripTableFromSdtLdtListsForModes ( ArrayList<String> tripModes, String fileName ) {
        
        int orig;
        int dest;
        int startTime;
        int o;
        int d;
        int totalValid = 0;
        int totalPeriod = 0;
        int total = 0;
        String mode;
        double totalVehicle = 0;

        BufferedReader in = null;
        String fileHeader = null;
        String s;
        
        double[][] tripTable = null;
        try {
            tripTable = new double[networkNumCentroids+1][networkNumCentroids+1];
        }
        catch ( Exception e) {
            logger.error("could not allocate trip table array for " + networkNumCentroids + " zones.", e);
            throw new RuntimeException();
        }

        TreeMap<String, Integer> totalModeFreqMap = new TreeMap<String, Integer>();
        TreeMap<String, Integer> periodModeFreqMap = new TreeMap<String, Integer>();

        

        // open the SDT or LDT trip list file for reading
        try {
            if ( fileName != null && ! fileName.equals("") ) {
                in = new BufferedReader(new FileReader(fileName));
                fileHeader = in.readLine();
            }
            else {
                new IOException("null input file name.");
            }
        }
        catch (IOException e) {
            logger.error ( "error opening person trip list file: " + fileName, e );
        }
        
        

        // get the field indices for the fields we want to read
        StringTokenizer st = new StringTokenizer(fileHeader, ",\n\r");
        int[] intFieldFlags = new int[st.countTokens()];
        int[] stringFieldFlags = new int[st.countTokens()];

        int i =0;
        while ( st.hasMoreTokens() ) {
            s = st.nextToken();
            if ( s.equals("origin") )
                intFieldFlags[i] = 1;
            else if ( s.equals("destination") )
                intFieldFlags[i] = 2;
            else if ( s.equals("tripStartTime") )
                intFieldFlags[i] = 3;
            else if ( s.equals("tripMode") )
                stringFieldFlags[i] = 1;
            i++;
        }
        
        // define values arrays for 3 ints and 1 String
        int[] intValues = new int[3+1];
        String[] stringValues = new String[1+1];
                

        
        // parse fields for the values we were interested in.
        int lineCount =0;
        try {
            
            while ( (s = in.readLine()) != null) {
                lineCount++;
                
                // get the int values
                i = 0;
                st = new StringTokenizer(s, ",\n\r");
                while ( st.hasMoreTokens() ) {
                    String stringValue = st.nextToken();
                    if ( intFieldFlags[i] > 0 )
                        intValues[intFieldFlags[i]] = (int)Float.parseFloat(stringValue);
                    i++;
                }
                
                // get the String values
                i = 0;
                st = new StringTokenizer(s, ",\n\r");
                while ( st.hasMoreTokens() ) {
                    String stringValue = st.nextToken();
                    if ( stringFieldFlags[i] > 0 )
                        stringValues[stringFieldFlags[i]] = stringValue;
                    i++;
                }
                
                orig = intValues[1];
                dest = intValues[2];
                startTime = intValues[3];
                mode = stringValues[1];
                total++;

                int value = 0;
                if ( totalModeFreqMap.containsKey(mode) )
                    value = (Integer)totalModeFreqMap.get(mode);
                totalModeFreqMap.put ( mode, (value+1) );


                // SDT and LDT use vehicle class 0 (a).
                double tripFactor = userClassPces[0];



                o = networkNodeIndexArray[orig];
                d = networkNodeIndexArray[dest];
                
                if ( tripStartsInCurrentPeriod ( startTime ) ) {
                        
                    // accumulate a frequency table of all trips within period by mode
                    totalPeriod++;
                    value = 0;
                    if ( periodModeFreqMap.containsKey(mode) )
                        value = (Integer)periodModeFreqMap.get(mode);
                    periodModeFreqMap.put ( mode, (value+1) );

                
                    // accumulate all specified period and mode person trips.
                    // highway trips are accumulated as vehicle trips
                    for (i=0; i < tripModes.size(); i++) {
                        if ( ((String)tripModes.get(i)).equalsIgnoreCase( mode ) ) {
                            
                            double trips = 0.0;
                            if ( mode.equalsIgnoreCase(TripModeType.SR2.name()) )
                                trips = tripFactor/2.0;
                            else if ( mode.equalsIgnoreCase(TripModeType.SR3P.name()) )
                                trips = tripFactor/AVERAGE_SR3P_AUTO_OCCUPANCY;
                            else
                                trips = tripFactor;
                            
                            tripTable[o][d] += trips*ptSampleRate;
                            totalValid++;

                            // accumulate district/district trip summaries by user class
                            multiclassVehicleDistrictTable[0][alphaDistrictIndex[orig]][alphaDistrictIndex[dest]] += trips*ptSampleRate;
                            
                            break;
                        }
                        
                    }

                }
                        
                
            }
        }
        catch (NumberFormatException e) {
            logger.error ( String.format("reading trip list file = %s.", fileName), e);
            throw new RuntimeException();
        }
        catch (IOException e) {
            logger.error ( String.format("reading trip list file = %s.", fileName), e);
            throw new RuntimeException();
        }
        catch (Exception e) {
            logger.error ( String.format("reading line %d of trip list file = %s.", lineCount, fileName), e);
            throw new RuntimeException();
        }

        logger.info("read " + lineCount + " lines from " + fileName + "." );


        
        Set<String> keys = periodModeFreqMap.keySet();
        Iterator<String> it = keys.iterator();
        logger.info ( "");
        logger.info ( String.format ("Mode Frequency Table of %s Trip File for %d to %d Period Trips:", fileName, startHour, endHour) );
        logger.info ( String.format ( "%-12s %8s %16s %16s", "mode", "freq", "pct", "cumPct" ) );
        logger.info ( "-----------------------------------------------------" );
        double cumPct = 0.0;
        while ( it.hasNext() ) {
            mode = (String)it.next();
            int value = (Integer)periodModeFreqMap.get(mode);
            double pct = value*100.0/totalPeriod;
            cumPct += pct;
            logger.info ( String.format ( "%-12s %8d %16.2f %16.2f", mode, value, pct, cumPct ) );
        }
        logger.info ( "-----------------------------------------------------" );
        logger.info ( String.format ( "%-12s %8d %16.2f %16.2f", "Total", totalPeriod, 100.0, cumPct ) );
        logger.info ( "");
        
        
        
        keys = totalModeFreqMap.keySet();
        it = keys.iterator();
        logger.info ( "");
        logger.info ( String.format ("Mode Frequency Table of %s Trip File for All Trips:", fileName) );
        logger.info ( String.format ( "%-12s %8s %16s %16s", "mode", "freq", "pct", "cumPct" ) );
        logger.info ( "-----------------------------------------------------" );
        cumPct = 0.0;
        while ( it.hasNext() ) {
            mode = (String)it.next();
            int value = (Integer)totalModeFreqMap.get(mode);
            double pct = value*100.0/total;
            cumPct += pct;
            logger.info ( String.format ( "%-12s %8d %16.2f %16.2f", mode, value, pct, cumPct ) );
        }
        logger.info ( "-----------------------------------------------------" );
        logger.info ( String.format ( "%-12s %8d %16.2f %16.2f", "Total", total, 100.0, cumPct ) );
        logger.info ( "");
        
        logger.info ( "");
        logger.info ( String.format( "%d person trips read from %s for %d to %d period triptable, %d of which were for specified modes:", totalPeriod, fileName, startHour, endHour, totalValid) );
        for (i=0; i < tripModes.size(); i++)
            logger.info( (String)tripModes.get(i) );
        

        totalVehicle = 0.0;
        for(i=0; i < tripTable.length; i++)
            for(int j=0; j < tripTable[i].length; j++)
                totalVehicle += tripTable[i][j];
        
        logger.info ( "");
        logger.info ( String.format( "%.0f total vehicle trips read from %s for %d to %d period triptable:", totalVehicle, fileName, startHour, endHour) );
        
        return tripTable;
            
    }
    


    private boolean tripStartsInCurrentPeriod ( int tripStartTime ) {
        
        boolean tripStartsInCurrentPeriod = false;
        
        // if startHour > endHour, the period wraps midnight, so two checks are required
        if ( startHour > endHour) {
            if ( ( tripStartTime >= startHour && tripStartTime <= 2400 ) || ( tripStartTime >= 0 && tripStartTime <= endHour ) )
                tripStartsInCurrentPeriod = true;
        }
        else {
            if ( tripStartTime >= startHour && tripStartTime <= endHour )
                tripStartsInCurrentPeriod = true;
        }
        
        return tripStartsInCurrentPeriod;
        
    }

    
    
    
    
    private double[][][] getTruckAssignmentGroupTripTableFromCTList () {

        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int group;
        char modeChar;
        String truckType;
        double tripFactor = 1.0;
        int allTruckTripCount=0;
        

        double[] tripsByUserClass = new double[highwayModeCharacters.length];
        double[] tripsByAssignmentGroup = new double[networkNumUserClasses];
        double[][][] tripTable = new double[networkNumUserClasses][networkNumCentroids+1][networkNumCentroids+1];



        // read the CT output file into a TableDataSet
        OLD_CSVFileReader reader = new OLD_CSVFileReader();

        TableDataSet table = null;
        int tripRecord = 0;
        try {
            if ( ctFileName != null && ! ctFileName.equals("") ) {

                table = reader.readFile(new File( ctFileName ));

                // traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
                for (int i=0; i < table.getRowCount(); i++) {
                    tripRecord = i+1;
                    
                    orig = (int)table.getValueAt( i+1, "origin" );
                    dest = (int)table.getValueAt( i+1, "destination" );
                    startTime = (int)table.getValueAt( i+1, "tripStartTime" );
                    truckType = (String)table.getStringValueAt( i+1, "truckType" );
                    //tripFactor = (int)table.getValueAt( i+1, "tripFactor" );  // replace tripFactor from CT and ET with PCE based on truck type.

                    mode = Integer.parseInt( truckType.substring(3) );
                    modeChar = highwayModeCharacters[mode];
                    group = -1;
                    for (int j=1; j < networkAssignmentGroupChars.length; j++) {
                        for (int k=0; k < networkAssignmentGroupChars[j].length; k++) {
                            if ( networkAssignmentGroupChars[j][k] == modeChar ) {
                                group = j;
                                break;
                            }
                        }
                    }
                    if ( group < 0 ) {
                        logger.error ( "modeChar = " + modeChar + " associated with CT integer mode = " + mode + " not found in any assignment group." );
                        System.exit(-1);
                    }


                    tripFactor = userClassPces[group];



                    o = networkNodeIndexArray[orig];
                    d = networkNodeIndexArray[dest];

                    
                    // accumulate all peak period highway mode trips
                    if ( tripStartsInCurrentPeriod ( startTime ) ) {
    
                        tripTable[group-1][o][d] += tripFactor;
                        tripsByUserClass[mode-1] += tripFactor;
                        tripsByAssignmentGroup[group-1] += tripFactor;

                        // accumulate district/district trip summaries by user class
                        multiclassVehicleDistrictTable[group][alphaDistrictIndex[orig]][alphaDistrictIndex[dest]] += tripFactor;
                        
                    }
                    
                    allTruckTripCount += tripFactor;
    
                }
    
                // done with trip list TabelDataSet
                table = null;

            }
            
        } catch (Exception e) {
            logger.error ("exception caught reading CT truck trip record " + tripRecord, e);
            System.exit(-1);
        }

        
        logger.info (allTruckTripCount + " total trips by all truck user classes read from CT file.");
        logger.info ("trips by truck user class read from CT file from " + startHour + " to " + endHour + ":");
        for (int i=0; i < tripsByUserClass.length; i++)
            if (tripsByUserClass[i] > 0)
                logger.info ( tripsByUserClass[i] + " truck trips with user class " + highwayModeCharacters[i+1] );

        logger.info ("trips by truck assignment groups read from CT file from " + startHour + " to " + endHour + ":");
        for (int i=0; i < tripsByAssignmentGroup.length; i++)
            if (tripsByAssignmentGroup[i] > 0)
                logger.info ( tripsByAssignmentGroup[i] + " truck trips in assignment group " + (i+1) );

        
        double totalVehicle = 0.0;
        for(int i=0; i < tripTable.length; i++)
            for(int j=0; j < tripTable[i].length; j++)
                for(int k=0; k < tripTable[i][j].length; k++)
                    totalVehicle += tripTable[i][j][k];
        
        logger.info ( "");
        logger.info ( String.format( "%.0f total vehicle trips read from %s for %d to %d period triptable:", totalVehicle, ctFileName, startHour, endHour) );
        
        return tripTable;

    }
    
    
    
    
    private double[][][] getExternalAssignmentGroupTripTableFromETList ( double[][][] tripTable ) {

        int orig;
        int dest;
        int startTime;
        int o;
        int d;
        int group, mode;
        char modeChar;
        String truckType;
        double truckVolume = 1.0;
        double tripFactor = 1.0;
        int allTruckTripCount=0;

        

        double[] tripsByUserClass = new double[highwayModeCharacters.length];
        double[] tripsByAssignmentGroup = new double[networkNumUserClasses];



        // read the ET output file into a TableDataSet
        OLD_CSVFileReader reader = new OLD_CSVFileReader();

        TableDataSet table = null;
        int tripRecord = 0;
        try {
            if ( etFileName != null && ! etFileName.equals("") ) {

                table = reader.readFile(new File( etFileName ));

                // traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
                for (int i=0; i < table.getRowCount(); i++) {
                    tripRecord = i+1;
                    
                    orig = (int)table.getValueAt( i+1, "origin" );
                    dest = (int)table.getValueAt( i+1, "destination" );
                    startTime = (int)table.getValueAt( i+1, "tripStartTime" );
                    truckType = (String)table.getStringValueAt( i+1, "truckClass" );
                    
                    // truck volume is 1 by default, 1 trip per record.
                    if ( table.containsColumn( "truckVolume" ) )
                        truckVolume = (double)table.getValueAt( i+1, "truckVolume" );
                    //tripFactor = ET_DEFAULT_TRUCK_FACTOR;  // replace tripFactor from CT and ET with PCE based on truck type.
    
                    mode = Integer.parseInt( truckType.substring(3) );
                    modeChar = highwayModeCharacters[mode];
                    group = -1;
                    for (int j=1; j < networkAssignmentGroupChars.length; j++) {
                        for (int k=0; k < networkAssignmentGroupChars[j].length; k++) {
                            if ( networkAssignmentGroupChars[j][k] == modeChar ) {
                                group = j;
                                break;
                            }
                        }
                    }
                    if ( group < 0 ) {
                        logger.error ( "modeChar = " + modeChar + " associated with ET integer mode = " + truckType + " not found in any assignment group." );
                        System.exit(-1);
                    }
                    

                    tripFactor = userClassPces[group];



                    o = networkNodeIndexArray[orig];
                    d = networkNodeIndexArray[dest];

                    
                    // accumulate all peak period highway mode trips
                    if ( tripStartsInCurrentPeriod ( startTime ) ) {
    
                        tripTable[group-1][o][d] += truckVolume*tripFactor;
                        tripsByUserClass[mode-1] += truckVolume*tripFactor;
                        tripsByAssignmentGroup[group-1] += truckVolume*tripFactor;

                        // accumulate district/district trip summaries by user class
                        multiclassVehicleDistrictTable[group][alphaDistrictIndex[orig]][alphaDistrictIndex[dest]] += truckVolume*tripFactor;
                        
                    }
                    
                    allTruckTripCount += truckVolume*tripFactor;
    
                }
    
                // done with trip list TabelDataSet
                table = null;

            }
            
        } catch (Exception e) {
            logger.error ("exception caught reading ET external trip record " + tripRecord, e);
            System.exit(-1);
        }

        
        logger.info (allTruckTripCount + " total external trips by all user classes read from ET file.");
        logger.info ("external trips by user class read from ET file from " + startHour + " to " + endHour + ":");
        for (int i=0; i < tripsByUserClass.length; i++)
            if (tripsByUserClass[i] > 0)
                logger.info ( tripsByUserClass[i] + " external trips with user class " + highwayModeCharacters[i+1] );

        logger.info ("external trips by truck assignment groups read from ET file from " + startHour + " to " + endHour + ":");
        for (int i=0; i < tripsByAssignmentGroup.length; i++)
            if (tripsByAssignmentGroup[i] > 0)
                logger.info ( tripsByAssignmentGroup[i] + " external trips in assignment group " + (i+1) );

        
        double totalVehicle = 0.0;
        for(int i=0; i < tripTable.length; i++)
            for(int j=0; j < tripTable[i].length; j++)
                for(int k=0; k < tripTable[i][j].length; k++)
                    totalVehicle += tripTable[i][j][k];
        
        logger.info ( "");
        logger.info ( String.format( "%.0f total external vehicle trips read from %s for %d to %d period triptable:", totalVehicle, etFileName, startHour, endHour) );
        
        return tripTable;

    }
    
    
    
    
    public int logDistrictReport () {
        
        double[] rowTotals = new double[districtNames.length];
        double[] colTotals = new double[districtNames.length];
        double total = 0.0;
        
        String record = null;
        
        for (int m=0; m < networkNumUserClasses; m++) {

            logger.info ( String.format("District - District Summary of vehicle trips for class %c:", highwayModeCharacters[m]) );
            logger.info ( "" );
            
            record = String.format("%-18s", "District");
            for (int i=0; i < districtNames.length; i++)
                record += String.format("%18s", districtNames[i]);
            record += String.format("%18s", "Total");

            logger.info ( record );

            total = 0.0;
            for (int i=0; i < districtNames.length; i++) {
                rowTotals[i] =  0.0;
                colTotals[i] =  0.0;
            }
            
            for (int i=0; i < districtNames.length; i++) {

                record = String.format("%-18s", districtNames[i]);
                for (int j=0; j < districtNames.length; j++) {
                    record += String.format("%18.1f", multiclassVehicleDistrictTable[m][i][j]);
                    rowTotals[i] +=  multiclassVehicleDistrictTable[m][i][j];
                    colTotals[j] +=  multiclassVehicleDistrictTable[m][i][j];
                    total +=  multiclassVehicleDistrictTable[m][i][j];
                }
                record += String.format("%18.1f", rowTotals[i]);
                
                logger.info ( record );

            }
            
            record = String.format("%-18s", "Total");
            for (int i=0; i < districtNames.length; i++)
                record += String.format("%18.1f", colTotals[i]);
            record += String.format("%18.1f", total);

            logger.info ( record );
            logger.info ( "" );
            logger.info ( "" );
            logger.info ( "" );

        }
        
        return 1;
        
    }



    public int writeDistrictReport ( String fileName ) {
        
        try {
            
            String record = null;

            double[] rowTotals = new double[districtNames.length];
            double[] colTotals = new double[districtNames.length];
            double total = 0.0;
            

            
            PrintWriter outStream =  new PrintWriter(new BufferedWriter( new FileWriter( fileName ) ) );

            for (int m=0; m < networkNumUserClasses; m++) {

                outStream.println( String.format("District - District Summary of vehicle trips for class %c:", highwayModeCharacters[m]) );
                outStream.println ( "" );
                
                record = String.format("%-18s", "District");
                for (int i=0; i < districtNames.length; i++)
                    record += String.format("%18s", districtNames[i]);
                record += String.format("%18s", "Total");

                outStream.println ( record );

                total = 0.0;
                for (int i=0; i < districtNames.length; i++) {
                    rowTotals[i] =  0.0;
                    colTotals[i] =  0.0;
                }
                
                for (int i=0; i < districtNames.length; i++) {

                    record = String.format("%-18s", districtNames[i]);
                    for (int j=0; j < districtNames.length; j++) {
                        record += String.format("%18.1f", multiclassVehicleDistrictTable[m][i][j]);
                        rowTotals[i] +=  multiclassVehicleDistrictTable[m][i][j];
                        colTotals[j] +=  multiclassVehicleDistrictTable[m][i][j];
                        total +=  multiclassVehicleDistrictTable[m][i][j];
                    }
                    record += String.format("%18.1f", rowTotals[i]);
                    
                    outStream.println ( record );

                }
                
                record = String.format("%-18s", "Total");
                for (int i=0; i < districtNames.length; i++)
                    record += String.format("%18.1f", colTotals[i]);
                record += String.format("%18.1f", total);

                outStream.println ( record );
                outStream.println ( "" );
                outStream.println ( "" );
                outStream.println ( "" );

            }
                
            outStream.close();

        }
        catch (IOException e) {
            logger.fatal("IO Exception writing district to district trip summary report to file: " + fileName, e );
        }
        
        return 1;
        
    }

    
}
