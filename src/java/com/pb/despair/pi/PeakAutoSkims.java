package com.pb.despair.pi;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pi.TransportKnowledge;
import com.pb.despair.model.TravelUtilityCalculatorInterface;
import com.pb.despair.model.DistanceAndTime;
import com.pb.despair.model.AbstractTAZ;

/**
 * A class that reads in peak auto skims and facilitates zone pair disutility calculations
 * @deprecated  This class is deprecated. Use SimeSkims and LinearFunctionOfSomeSkims
 * 
 *
 * @author John Abraham & J. Freedman
 */
public class PeakAutoSkims extends TransportKnowledge {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pi");
    static final int MAXBETAZONENUMBER=4141;
    static final int MAXZONENUMBER=5000;
    private Matrix pkTime,pkDist;

    /*
    *This constructor is called when the Skims are in ZIP format
    */
    public PeakAutoSkims() {
        logger.info("Reading peak auto skims into memory");
        ResourceBundle rb = ResourceUtil.getResourceBundle( "despair" );
        String path = ResourceUtil.getProperty(rb, "Model.skimPath");
        String[] mName = {path+"betapkdist.zip",
                  path+"betapktime.zip"};
        //When reading zip files, the reader reads in an "_external numbers" entry
        //and passes those along to the Matrix class when it reads the Matrix.
        //So in Matrix there is an internalNumber array (length = max eternal number + 1) and an
        //externalNumber array.  To know if a row existed in the original zip file, you can
        //call matrix.getInternalNumber(int external number) and if the external number
        //corresponded to a row number in the zip file, the internal row number of the
        //matrix will be returned.  Otherwise -1 will be returned.
        pkDist=new ZipMatrixReader(new File(mName[0])).readMatrix();
        pkTime=new ZipMatrixReader(new File(mName[1])).readMatrix();

        logger.info("Finished reading skims into memory");
    }; //end constructor


    /* This constructor is called whent the skims are in a CSV format
    */
    public PeakAutoSkims(TableDataSet s, int originField, int destinationField, int distanceField, int timeField) {
        int[] userToSequentialLookup = new int[MAXZONENUMBER+1];
        int[] sequentialToUserLookup = new int[MAXZONENUMBER+1];
        for (int i =0; i<userToSequentialLookup.length;i++) {
                    userToSequentialLookup[i] = -1;
        }
        int[] origins = s.getColumnAsInt(originField);
        int zonesFound = 0;
        for (int o = 0;o<origins.length;o++) {
            int sequentialOrigin = userToSequentialLookup[origins[o]];
            if (sequentialOrigin == -1) {
                sequentialOrigin = zonesFound;
                zonesFound++;
                userToSequentialLookup[origins[o]]=sequentialOrigin;
                sequentialToUserLookup[sequentialOrigin] = origins[o];
            }
        }
        //In the Matrix class there is an internalNumber array (length = max eternal number + 1) and an
        //externalNumber array.  You can use these arrays to keep track of skipped numbers
        // in the skim files.  You can set the external numbers array in your matrix (start at 1, sort from low to high)
        //To know if a row existed in the original file, you can
        //call matrix.getInternalNumber(int external number) and if the external number
        //corresponded to a row number in the table, the internal row number of the
        //matrix will be returned.  Otherwise -1 will be returned.
        int[] externalZoneNumbers = new int[zonesFound+1];
        for(int i=1;i<externalZoneNumbers.length;i++){
            externalZoneNumbers[i]=sequentialToUserLookup[i-1];
        }
        //Arrays.sort(externalZoneNumbers);
        //enable garbage collection
        origins = null;
        //userToSequentialLookup = null;
        //sequentialToUserLookup = null;

        //now copy info into temp value arrays
        float[][] tempPkTime=new float[zonesFound][zonesFound];
        float[][] tempPkDist=new float[zonesFound][zonesFound];
        for (int row = 1;row <= s.getRowCount();row++) {
            int origin = (int) s.getValueAt(row,originField);
            int destination = (int) s.getValueAt(row,destinationField);
            tempPkTime[userToSequentialLookup[origin]][userToSequentialLookup[destination]] = s.getValueAt(row,timeField);
            tempPkDist[userToSequentialLookup[origin]][userToSequentialLookup[destination]] = s.getValueAt(row,distanceField);
        }


        pkTime=new Matrix("pkTime","beta-zone peak travel times",tempPkTime);
        pkTime.setExternalNumbers(externalZoneNumbers);
        pkDist=new Matrix("pkDist","beta-zone peak travel distances",tempPkDist);
        pkDist.setExternalNumbers(externalZoneNumbers);

        logger.info("Finished reading skims into memory");
    }


    public double[] getUtilityComponents(int fromZoneUserNumber, int toZoneUserNumber, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        
        DistanceAndTime dt = new DistanceAndTime();
        dt.time = pkTime.getValueAt(fromZoneUserNumber,toZoneUserNumber);
        dt.distance = pkDist.getValueAt(fromZoneUserNumber,toZoneUserNumber);
        return tp.getUtilityComponents(fromZoneUserNumber, toZoneUserNumber, dt);
    }

    

    public double getUtility(int fromZoneUserNumber, int toZoneUserNumber, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        int printSkims = 0;

        DistanceAndTime dt = new DistanceAndTime();
        dt.time = pkTime.getValueAt(fromZoneUserNumber,toZoneUserNumber);
        dt.distance = pkDist.getValueAt(fromZoneUserNumber,toZoneUserNumber);
        if (dt.distance < 0 || dt.time < 0 || Double.isNaN(dt.distance) || Double.isNaN(dt.time)) {
            throw new Error("Bad distance "+dt.distance+" or bad time "+dt.time+" in skim from "+fromZoneUserNumber+" to "+toZoneUserNumber);
        }
        double rutility = tp.getUtility(fromZoneUserNumber, toZoneUserNumber, dt);
        // debug Feb 26 2003
        if (printSkims > 0 || Double.isNaN(rutility)) {
        	logger.info("skim from "+fromZoneUserNumber+" to "+toZoneUserNumber+" is "+rutility+" for "+tp);
        	logger.info("time = "+dt.time+", distance = "+dt.distance);
        	printSkims--;
        }
        return rutility;
    }

    public double getUtility(int fromZoneUserNumber, int toZoneUserNumber, double valueOfTime, double costOfDistance) {
        float distance = pkDist.getValueAt(fromZoneUserNumber,toZoneUserNumber);
        float time = pkTime.getValueAt(fromZoneUserNumber,toZoneUserNumber);
        // debug March 9 2004
        if (distance < 0 || time < 0 || Double.isNaN(distance) || Double.isNaN(time)) {
            throw new Error("Bad distance "+distance+" or bad time "+time+" in skim from "+fromZoneUserNumber+" to "+toZoneUserNumber);
        }

    	double compositeUtility;
    	if (costOfDistance == 0) compositeUtility = +valueOfTime*time;
    	else compositeUtility = (distance*costOfDistance)+(valueOfTime*time);
        return compositeUtility;
    }

    public double getTime(int fromZoneUserNumber, int toZoneUserNumber) {
        return pkTime.getValueAt(fromZoneUserNumber,toZoneUserNumber);
    }


    public double getDistance(int fromZoneUserNumber, int toZoneUserNumber) {
        Hashtable badSkims = new Hashtable();
        double distance = pkDist.getValueAt(fromZoneUserNumber,toZoneUserNumber);
        if (Double.isNaN(distance) || distance < 0) {
        	Integer badOrigin = new Integer(fromZoneUserNumber);
      		Integer badDestination = new Integer(toZoneUserNumber);
        	ArrayList originArray = (ArrayList) badSkims.get(badOrigin);
        	if (originArray !=null) {
        		if (originArray.contains(badDestination)) {
        			return 1000;
        		}
        		originArray.add(badDestination);
        	} else {
	        	originArray = new ArrayList();
	        	originArray.add(badDestination);
	        	badSkims.put(badOrigin,originArray);
        	}
        	logger.warn(distance +" distance in skims, from zone "+fromZoneUserNumber+" to "+toZoneUserNumber);
        	logger.warn("using 1000 instead");
        	return 1000;
        }
        return distance;
    }

    /* (non-Javadoc)
     * @see com.pb.despair.pi.TransportKnowledge#getUtility(com.pb.despair.model.AbstractTAZ, com.pb.despair.model.AbstractTAZ, com.pb.despair.model.TravelUtilityCalculatorInterface, boolean)
     */
    public double getUtility(AbstractTAZ from, AbstractTAZ to, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return getUtility(from.getZoneUserNumber(), to.getZoneUserNumber(), tp, useRouteChoice);
    }


    /* (non-Javadoc)
     * @see com.pb.despair.pi.TransportKnowledge#getUtilityComponents(com.pb.despair.model.AbstractTAZ, com.pb.despair.model.AbstractTAZ, com.pb.despair.model.TravelUtilityCalculatorInterface, boolean)
     */
    public double[] getUtilityComponents(AbstractTAZ from, AbstractTAZ to, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return getUtilityComponents(from.getZoneUserNumber(), to.getZoneUserNumber(), tp, useRouteChoice);
    }



};
