package com.pb.tlumip.ts.assign;

import com.pb.common.util.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/** .
 *
 * @author    Jim Hicks
 * @version   1.0, 5/13/2004
 * 
 */
public class TripDataGenerator {

    Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign");

	public static final double GRAVITY_BETA = -0.10;

	
    int nZones;
    double[][] odTable;


    public TripDataGenerator( double[][] hwyDistSkims ) {
		nZones = hwyDistSkims.length;
    	buildODTable ( hwyDistSkims );
    }


    
    public double[][] getOdTable() {
        return odTable;
    }

    
    private void buildODTable ( double[][] hwyDistSkims ) {

        //Apply gravity mode to generate trip table for assignment
        gravityModelTrips ( hwyDistSkims );

    }


    /**
     * Read zonal prods and attrs and use a gravity model to build a trip matrix
     * for assignment.
     */
    int gravityModelTrips ( double[][] hwyDistSkims ) {
        int id, taz, intPart;
        int intras=0, totalIJs=0, totalTrips=0;
        int[] prod = new int[nZones];
        int[] attr = new int[nZones];
        double denominator, odTrips, tripBucket, avgTripLength;
        double beta;
	    Justify myFormat = new Justify();


        // read productions and attractions, total trips read in.
        try {
            BufferedReader in = new BufferedReader(new FileReader(Constants.PROD_ATTR_FILE));
            String s = new String();

            // first record contains column labels
            s = in.readLine();
            
            while ((s = in.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(s);
                if (st.hasMoreTokens()) {
                    id = Integer.parseInt(st.nextToken());
                    taz = Integer.parseInt(st.nextToken());
                    prod[id] = Integer.parseInt(st.nextToken());
                    attr[id] = Integer.parseInt(st.nextToken());
                    totalTrips += prod[id];
                }
            }
            
	        logger.info (totalTrips + " total productions read from file: " + Constants.PROD_ATTR_FILE);
        }
		catch (Exception e) {
            logger.fatal ("IO Exception caught reading trip generation data from : " + Constants.PROD_ATTR_FILE);
            e.printStackTrace();
        }



        odTable = new double[nZones][nZones];

        tripBucket = 0.0;
        totalTrips = 0;
        totalIJs = 0;
        intras = 0;
        avgTripLength = 0.0;

        for (int i=0; i < nZones; i++) {
            
            if ( i <= Constants.MAX_INTERNAL_ZONES )
                beta = GRAVITY_BETA;
            else 
                beta = 10*GRAVITY_BETA;
        	
            denominator = 0.0;
            for (int j=0; j < nZones; j++)
                denominator += attr[j]*Math.exp(beta*hwyDistSkims[i][j]);


            for (int j=0; j < nZones; j++) {

                if (denominator > 0.0)
                    odTrips = 0.1*prod[i]*attr[j]*Math.exp(beta*hwyDistSkims[i][j])/denominator;
                else
                    odTrips = 0.0;


                tripBucket += odTrips;
                if (tripBucket >= 1.0) {
                    if (i == j)
                        intras++;
                    else
                        totalIJs++;

                    intPart = (int)tripBucket;
                    tripBucket -= (int)tripBucket;
                    for (int k=0; k < intPart; k++) {
                        odTable[i][j] += 1.0;
                        totalTrips++;
                        avgTripLength += hwyDistSkims[i][j];
                    }
                }
            }
        }

        if (tripBucket >= 0.5) {
            odTable[nZones-1][nZones-1] += 1.0;
            totalTrips++;
            avgTripLength += hwyDistSkims[nZones-1][nZones-1];
        }


        logger.info (totalTrips + " total trips entered into trip table from " + totalIJs + " od pairs, intras=" + intras + ".");
		logger.info ("regional average trip length= " + myFormat.left(avgTripLength/totalTrips, 14, 4));
		logger.info ("");

        return totalTrips;
    }
}