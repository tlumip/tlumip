package com.pb.tlumip.ts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.xmlrpc.WebServer;

import org.apache.log4j.Logger;




/**
 * This data server exposes methods can be called by remote xml-rpc clients written in any language.
 * The methods accept xml-rpc compliant arguments and return xml-rpc compliant data types.
 * 
 * @author JHicks
 *
 */
public class SimVatDataServer {

    protected static Logger logger = Logger.getLogger(SimVatDataServer.class);

    public static final int MAX_LINK_ID = 43000; 
    public static final int MAX_TIME_ID = 600; 
    public static final int TIME_STEP = 6; 
    
    public static final String HANDLER_NAME = "simVatDataServer";
    public static final int HANDLER_PORT = 6010;

    WebServer server = null;
    
    int[][][][] linkMvts;
    int[][] linkTimeStepOccups = null;
    
    String fileName;
    int recNum;
    
    
    private SimVatDataServer () {
    }
    
    
    // Factory Method to return instance only
    public static SimVatDataServer getInstance( int port, String handlerName ) {
        
        SimVatDataServer ns =  new SimVatDataServer();
        
        ns.server = new WebServer(port);
        ns.server.addHandler( handlerName, ns );

        return ns;
        
    }
    
    public void startServer() {
        server.start();
    }
    
    public void stopServer() {
        server.shutdown();
    }
    
    
    public Vector getLinkMinuteVehMvts ( int link, int minute ) {

        Vector mvts;
        
        if ( linkMvts[link][minute] != null ) {

            int[][] vehMvts = linkMvts[link][minute];

            mvts = new Vector(vehMvts.length);
            for (int i=0; i < vehMvts.length; i++) {
                Vector values = new Vector(vehMvts[i].length);
                for (int j=0; j < vehMvts[i].length; j++)
                    values.add(vehMvts[i][j]);
                mvts.add(values);
            }
                
        }
        else {
            
            mvts = new Vector(0);
            
        }

        return mvts;
    }
    
    
    
    public Vector getLinkVehMvtsInRange ( int link, int startMinute, int endMinute ) {

        Vector mvts = new Vector();
        
        for ( int minute=startMinute; minute <= endMinute; minute++ ) {

            if ( linkMvts[link][minute] != null ) {

                for (int i=0; i < linkMvts[link][minute].length; i++) {
                    Vector values = new Vector(linkMvts[link][minute][i].length);
                    for (int j=0; j < linkMvts[link][minute][i].length; j++)
                        values.add(linkMvts[link][minute][i][j]);
                    mvts.add(values);
                }
                    
            }
            
        }

        return mvts;
    }
    
    
    
    public Vector getTimeStepOccupanciesForLinks ( Vector links, int timeStepId ) {

        Iterator it = links.iterator();
        Vector occups = new Vector(links.size());
        
        while ( it.hasNext() ) {
            int linkId = (Integer)it.next();
            int occ = linkTimeStepOccups[linkId][timeStepId];
            occups.add(occ);
        }
        
        return occups;
    }
    
    
    
    private void reReadSimVatFile( String fileName ) {

        int i=0;
        
        int[][] lastMvtIndices = new int[MAX_LINK_ID][MAX_TIME_ID];
        
        try {

            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String s = new String();

            logger.info("opened " + fileName + " for re-reading.");
            
            // the order of the fields is assumed known and fixed as coded here.
            recNum = 0;
            while ((s = in.readLine()) != null) {
                recNum++;
            
                if ( i % 500000 == 0 )
                    logger.info("re-read trajectory for vehicle record " + i + ".");
                
                Trajectory t = parseVehicleRecord( s );
                parseTrajectory( t, in.readLine() );
                recNum++;
                
                // if number of links in trajectory is 1, skip processing this trajectory
                if ( t.arrivals.length > 1 )
                    processTrajectory(t, lastMvtIndices);

                t.arrivals = null;
                t = null;
                
                    
                i++;
                
            }

        } catch (Exception e) {

            logger.fatal ("Exception caught reading record " + recNum + " in file: " + fileName, e);
            System.exit(1);
            
        }

        logger.info("stored " + i + " vehicle trajectories in linkMvts array.");

    }
    
    
    private int[][] countSimVatLinkMvts( String fileName ) {

        int totalMvts = 0;
        int[][] linkMvtCounts = new int[MAX_LINK_ID][MAX_TIME_ID];
        
        int i=0;
        
        try {

            BufferedReader in = new BufferedReader(new FileReader(fileName));
            
            logger.info("");
            logger.info("opened " + fileName + " for reading.");
            
            // the order of the fields is assumed known and fixed as coded here.
            recNum = 0;
            while ( (in.readLine()) != null ) {
                recNum++;

                if ( i % 500000 == 0 )
                    logger.info("read trajectory for vehicle record " + i + ".");
                
                totalMvts += countMvts( linkMvtCounts, in.readLine() );
                recNum++;
                
                i++;
                
            }
            
            in.close();

        } catch (Exception e) {

            logger.fatal ("Exception caught reading record " + recNum + " while counting movements in file: " + fileName, e);
            System.exit(1);
            
        }

        // count the number of linkId,timeId pairs for which vehicle movements will be recorded.
        int pairsFound = 0;
        int maxCount = 0;
        int maxLink = 0;
        int maxTime = 0;
        for (int j=0; j < linkMvtCounts.length; j++)
            for (int k=0; k < linkMvtCounts[j].length; k++)
                if ( linkMvtCounts[j][k] > 0 ) {
                    pairsFound++;
                    if ( linkMvtCounts[j][k] > maxCount ) {
                        maxCount = linkMvtCounts[j][k];
                        maxLink = j;
                        maxTime = k;
                    }
                }
                
        logger.info("read " + i + " vehicle trajectories from " + recNum + " total sim.vat records.");
        logger.info( pairsFound + " linkId,timeId pairs have " + totalMvts + " total vehicle movements.");
        
        logger.info ( "max number of vehicle movements counted = " + maxCount + " at linkId = " + maxLink + ", timeId = " + maxTime + "." );
        
        
        return linkMvtCounts;
    }
    
    
    private Trajectory parseVehicleRecord( String s ) {
        
        // sample data records
        //1 1668379 285.000 504.000
        //7 38468 285.00 30550 288.00 30552 348.00 18884 390.00 18898 444.00 18905 456.00 29351 504.00

        
        //Test for an empty file
        if ( s == null ) {
            logger.error ("empty string found in record while reading sim.vat vehicle record.  Last file record number successfully read = " + recNum+1, new IOException() );
        }

        //Tokenize the data record.  We know all values are numeric and separated by spaces
        StringTokenizer st = new StringTokenizer( s , " \n\r");

        //Read vehicle record values
        Trajectory t = new Trajectory();
        t.vehType = Integer.parseInt( st.nextToken() );
        t.vehId = Integer.parseInt( st.nextToken() );
        t.startTime = Float.parseFloat( st.nextToken() );
        t.endTime = Float.parseFloat( st.nextToken() );
        
        return t;

    }
    
    
    
    private Trajectory parseTrajectory( Trajectory t, String s ) {
        
        // sample data records
        //1 1668379 285.000 504.000
        //7 38468 285.00 30550 288.00 30552 348.00 18884 390.00 18898 444.00 18905 456.00 29351 504.00

        
        //Test for an empty file
        if ( s == null ) {
            logger.error ("empty string found in record while reading sim.vat trajectory record.  Last file record number successfully read = " + recNum+1, new IOException() );
        }

        //Tokenize the data record.  We know all values are numeric and separated by spaces
        StringTokenizer st = new StringTokenizer( s , " \n\r");

        // get number of link,arrival time pairs in the trajectory
        int numArrivals = Integer.parseInt( st.nextToken() );
        
        // read the link,arrival time pairs
        t.arrivals = new Arrival[numArrivals];
        for (int i=0; i < numArrivals; i++) {
            t.arrivals[i] = new Arrival();
            t.arrivals[i].link = Integer.parseInt( st.nextToken() );
            t.arrivals[i].time = Float.parseFloat( st.nextToken() );
        }
        
        return t;

    }

    
    private int countMvts( int[][] mvtCounts, String s ) {
        
        // sample data records
        //1 1668379 285.000 504.000
        //7 38468 285.00 30550 288.00 30552 348.00 18884 390.00 18898 444.00 18905 456.00 29351 504.00

        //Tokenize the data record.  We know all values are numeric and separated by commas
        StringTokenizer st = new StringTokenizer( s , " \n\r");

        // get number of link,arrival time pairs in the trajectory
        int numArrivals = Integer.parseInt( st.nextToken() );
        
        // read the link,arrival time pairs
        int totalMvts = 0;
        for (int i=0; i < numArrivals-1; i++) {
            
            int linkId = Integer.parseInt( st.nextToken() );
            int timeId = (int)( Float.parseFloat(st.nextToken()) / 60.0 );

            if ( linkId >= MAX_LINK_ID ) {
                logger.error( "" );
                logger.error( "linkId = " + linkId + " in vehicle trajectory number " + i + " on record number " + recNum );
                logger.error( "in " + fileName + " exceeds the maximum number defined for this program = " + MAX_LINK_ID + "." );
                logger.error( "Increase the value of MAX_LINK_ID and recompile.\n" );
                System.exit(-1);
            }
            
            if ( timeId >= MAX_TIME_ID ) {
                logger.error( "" );
                logger.error( "timeId = " + timeId + " in vehicle trajectory number " + i + " on record number " + recNum );
                logger.error( "in " + fileName + " exceeds the maximum number defined for this program = " + MAX_TIME_ID + "." );
                logger.error( "Increase the value of MAX_TIME_ID and recompile.\n" );
                System.exit(-1);
            }
            
            mvtCounts[linkId][timeId]++;            
            totalMvts++;
            
        }

        return totalMvts;

    }

    
    private void processTrajectory(Trajectory t, int[][] lastMvtIndices) {
        
        int link1 = t.arrivals[0].link;
        float time1 = t.arrivals[0].time;
        
        int link2 = t.arrivals[1].link;
        float time2 = t.arrivals[1].time;
        
        
        for (int i=2; i < t.arrivals.length; i++) {
            
            int link3 = t.arrivals[i].link;
            float time3 = t.arrivals[i].time;

            int timeId = (int)(time1/60.0);
            int lastIndex = lastMvtIndices[link1][timeId];
            
            linkMvts[link1][timeId][lastIndex][0] = t.vehId;
            linkMvts[link1][timeId][lastIndex][1] = (int)time1;
            linkMvts[link1][timeId][lastIndex][2] = (int)( time2 - time1 );
            linkMvts[link1][timeId][lastIndex][3] = link2;
            linkMvts[link1][timeId][lastIndex][4] = (int)( time3 - time2 );
            
            lastMvtIndices[link1][timeId]++;
                           
            link1 = link2;
            time1 = time2;
            
            link2 = link3;
            time2 = time3;
            
        }

        float time3 = t.endTime;

        int timeId = (int)(time1/60.0);
        int lastIndex = lastMvtIndices[link1][timeId];

        linkMvts[link1][timeId][lastIndex][0] = t.vehId;
        linkMvts[link1][timeId][lastIndex][1] = (int)time1;
        linkMvts[link1][timeId][lastIndex][2] = (int)( time2 - time1 );
        linkMvts[link1][timeId][lastIndex][3] = link2;
        linkMvts[link1][timeId][lastIndex][4] = (int)( time3 - time2 );

        lastMvtIndices[link1][timeId]++;

    }
    
    
    private int[][][][] allocateLinkMvtArrays( int[][] mvtCounts ) {
        
        int[][][][] linkMvts = new int[MAX_LINK_ID][MAX_TIME_ID][][];
        
        // allocate an array for the number of movements counted
        int k = 0;
        int total = 0;
        for ( int i=0; i < mvtCounts.length; i++ ) {
            for ( int j=0; j < mvtCounts[i].length; j++ ) {
                if ( mvtCounts[i][j] > 0 ) {
                    linkMvts[i][j] = new int[mvtCounts[i][j]][5];
                    total += mvtCounts[i][j];

                    if ( k % 1000000 == 0 ) {
                        logger.info ( "allocated " + (total*20/(1024*1024)) + " MB for " + total + " movements for " + k + " linkId,timeId pairs." );
                    }
                    
                    k++;
                }
            }
            
        }
        logger.info ( "allocated " + (total*20/(1024*1024)) + " MB for " + total + " movements for " + k + " linkId,timeId pairs." );
        

        int count = 0;
        for ( int i=0; i < linkMvts.length; i++ ) {
            for ( int j=0; j < linkMvts[i].length; j++ ) {
                if ( linkMvts[i][j] != null ) {
                    for ( k=0; k < linkMvts[i][j].length; k++ ) {
                        for ( int l=0; l < linkMvts[i][j][k].length; l++ ) {
                            count++;
                        }
                    }
                }
            }
        }

        
        return linkMvts;
        
    }
    
    
    public static void main ( String[] args ) {

        logger.info( "start of main()." );

        SimVatDataServer s = SimVatDataServer.getInstance( HANDLER_PORT, HANDLER_NAME );

        //String fileName = "192.168.1.213:/var/lib/vista/vista/atlanta_2030/sim.vat";
        //String fileName = "//192.168.1.213/root/var/lib/vista/vista/atlanta_2030/sim.vat";
        //String fileName = "/jim/projects/source/Eckel_aug2006/smallSim.vat";
        //String fileName = "/jim/projects/atlanta/I285/data2030/sim7578.vat";
        //String fileName = "/jim/projects/source/viewTlumip/sim.vat";
        
        //String fileName = "/mnt/zufa/jim/projects/source/viewTlumip/sim.vat";
        String fileName = "/home/jhicks/sim.vat";
        
        s.fileName = fileName;

        
        
        // first store the count of movements for each approach link/arrival minute combination
        // read the sim.vat file, counting the number of movements that will need information recorded.
        logger.info( "before countSimVatLinkMvts() in main()." );
        int[][] linkMvtCounts = s.countSimVatLinkMvts( fileName );
        // should require approx 100MB for linkMvtCounts array
        
        
        // allocate memory for the int[][][][] linkMvts array to hold all the required information.
        logger.info( "before allocateLinkMvtArrays() in main()." );
        s.linkMvts = s.allocateLinkMvtArrays( linkMvtCounts );
        linkMvtCounts = null;
        // should require approx 2GB for linkMvts array


        
        // re-read the sim.vat file, storing the movement information in the linkMvts array that was previously allocated.
        logger.info( "before reReadSimVatFile() in main()." );
        s.reReadSimVatFile( fileName );
        // should add approx 100MB for local array in this method, which would then be eligible for garbage collection
        

        
        logger.info( "before filling linkTimeStepOccups array in main()." );
        s.linkTimeStepOccups = new int[MAX_LINK_ID][60*MAX_TIME_ID/TIME_STEP];

        for (int i=0; i < s.linkMvts.length; i++) {
            for (int j=0; j < s.linkMvts[i].length; j++) {
                if (s.linkMvts[i][j] != null) {
                    for (int k=0; k < s.linkMvts[i][j].length; k++) {
                        int id1 = (int)(s.linkMvts[i][j][k][1]/TIME_STEP);
                        int id2 = (int)((s.linkMvts[i][j][k][1]+s.linkMvts[i][j][k][2])/TIME_STEP);
                        // incement the occupancy count for this vehicle on link1 for each time step occupied 
                        for (int m=id1; m < id2; m++)
                            s.linkTimeStepOccups[i][m]++;
                    }
                }
            }
        }
        
        
        
        // start the server so that clients cn request data stored in linkMvts array,
        logger.info( "linkMvts array complete, starting server ..." );
        s.startServer();
        // total memory used at this point should be approx 2GB.
        
        
        while (true)
            ;
        
    }
    
    
    
    // Inner classes
    
    class Trajectory {
        int vehId;
        int vehType;
        float startTime;
        float endTime;
        Arrival[] arrivals;
    }
    
    class Arrival {
        int link;
        float time;
    }
    
}
