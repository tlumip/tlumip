package com.pb.despair.pt;
import com.pb.common.util.OutTextFile;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.SkimsInMemory;
import com.pb.despair.model.TravelTimeAndCost;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/** 
 * A class that creates mode choice logsums by
 * 9 market segments for all trip purposes
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class CreateModeChoiceLogsums {
    
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");     
    boolean debug = false;

    //  arrays with segments
    static final int[] auwk0segs={1,0,0,1,0,0,1,0,0};
    static final int[] auwk1segs={0,1,0,0,1,0,0,1,0};
    static final int[] auwk2segs={0,0,1,0,0,1,0,0,1};
    static final int[] aupr0segs={1,0,0,1,0,0,1,0,0};
    static final int[] aupr1segs={0,1,0,0,1,0,0,1,0};
    static final int[] aupr2segs={0,0,1,0,0,1,0,0,1};
    static final int[] inclowsegs={1,1,1,0,0,0,0,0,0};
    static final int[] incmedsegs={0,0,0,1,1,1,0,0,0};
    static final int[] inchisegs={0,0,0,0,0,0,1,1,1};
    
    static final boolean CREATE_BETA_ZONES = false;
    
    ZoneAttributes thisZone = new ZoneAttributes();
    PersonTourModeAttributes ptma = new PersonTourModeAttributes();         
    
    TravelTimeAndCost departCost;
    TravelTimeAndCost returnCost;
    PersonTourModeAttributes thisPerson;
    TableDataSet alphaToBeta;


    public CreateModeChoiceLogsums(){}

    /**
     * setModeChoiceLogsumMatrix
     * Calculates Mode choice logsums and creates a logsum matrix
     * @param taz
     * @param theseParameters
     * @param thisPurpose
     * @param segment
     * @return
     */
    public Matrix setModeChoiceLogsumMatrix(TazData taz, TourModeParameters theseParameters, char thisPurpose,
                                            int segment, SkimsInMemory skims, TourModeChoiceModel tmcm) {

        if(debug) logger.fine("Creating ModeChoiceLogsum Matrix for - Purpose: "+thisPurpose+"  Segment: "+segment);
        String mName = new String(new Character(thisPurpose).toString()
                                + new Integer(segment).toString()
                                + new String("ls"));  //the extension will be added when the file is
                                                      //written out - could be .zip or .binary

        Matrix m = new Matrix(mName, "Created with CreateModeChoiceLogsums", taz.tazData.size(), taz.tazData.size());
        m.setExternalNumbers(taz.getExternalNumberArray());

        Enumeration originEnum=taz.tazData.elements();
        while(originEnum.hasMoreElements()){
            Taz originTaz = (Taz) originEnum.nextElement();
            Enumeration destinationEnum=taz.tazData.elements();
            while(destinationEnum.hasMoreElements()){

                Taz destinationTaz = (Taz) destinationEnum.nextElement();
//                thisPerson = setPersonTourModeAttributes(originTaz, destinationTaz, thisPurpose, segment);
                setPersonTourModeAttributes(originTaz, destinationTaz, thisPurpose, segment);

//                float logsum = getModeChoiceLogsum(skims,theseParameters,thisPerson,thisPurpose,segment,originTaz, destinationTaz, tmcm);
                float logsum = getModeChoiceLogsum(skims,theseParameters,ptma,thisPurpose,segment,originTaz, destinationTaz, tmcm);

                m.setValueAt(originTaz.zoneNumber,destinationTaz.zoneNumber,logsum);

            } //end destination zone loop
        } //end origin zone loop
        return m;
    }

    /**
     * Gets a mode choice logsum
     * @param skims
     * @param theseParameters
     * @param thisPerson
     * @param thisPurpose
     * @param segment
     * @param originTaz
     * @param destinationTaz
     * @return
     */
    public float getModeChoiceLogsum(SkimsInMemory skims,
                                      TourModeParameters theseParameters,
                                      PersonTourModeAttributes thisPerson,
                                      char thisPurpose,
                                      int segment,
                                      Taz originTaz,
                                      Taz destinationTaz,
                                      TourModeChoiceModel tmcm){

        departCost = setDepartCost(thisPurpose, skims, originTaz, destinationTaz);
        //If drivetime > 300, then set logsum to -32
        if(departCost.driveAloneTime > 300)
            return -32;
        else{
            returnCost = setReturnCost(thisPurpose, skims, originTaz, destinationTaz);
            //ZoneAttributes thisZone = new ZoneAttributes();
            thisZone.parkingCost = destinationTaz.getParkingCost(thisPurpose);
            float logsum = calcTourModeChoiceUtility(tmcm, thisZone, theseParameters, thisPerson, thisPurpose);
            return logsum;
        }
    }

    /** calcTourModeChoiceUtility
     *
     * @param tmcm - TourModeChoiceModel
     * @param thisZone - Parking cost at the destination zone
     * @param theseParameters - TourModeParameters
     * @param thisPerson - PersonTourModeAttributes
     * @param thisPurpose - char
     * @return logsum for the origin-destination pair
     */
    public float calcTourModeChoiceUtility(TourModeChoiceModel tmcm,
                                            ZoneAttributes thisZone,
                                            TourModeParameters theseParameters,
                                            PersonTourModeAttributes thisPerson,
                                            char thisPurpose){

        //Set availabilities of all modes to true
        tmcm.thisDriver.setAvailability(true);
        tmcm.thisPassenger.setAvailability(true);
        tmcm.thisWalk.setAvailability(true);
        tmcm.thisBike.setAvailability(true);
        tmcm.thisWalkTransit.setAvailability(true);
        tmcm.thisTransitPassenger.setAvailability(true);
        tmcm.thisDriveTransit.setAvailability(true);

        //calculate utilites of all modes
        tmcm.thisDriver.calcUtility( departCost, returnCost,
                thisZone, theseParameters, thisPerson);

        tmcm.thisPassenger.calcUtility( departCost, returnCost,
                thisZone, theseParameters, thisPerson);

        tmcm.thisWalk.calcUtility( departCost, returnCost,
                thisZone, theseParameters, thisPerson);

        tmcm.thisBike.calcUtility( departCost, returnCost,
                theseParameters, thisPerson);

        tmcm.thisWalkTransit.calcUtility( departCost, returnCost,
                theseParameters, thisPerson);

        tmcm.thisTransitPassenger.calcUtility( departCost, returnCost,
                theseParameters, thisPerson);

        tmcm.thisPassengerTransit.calcUtility( departCost, returnCost,
                thisZone, theseParameters, thisPerson);

        if(thisPurpose=='w')
               tmcm.thisDriveTransit.calcUtility( departCost, returnCost,
                     theseParameters, thisPerson);
        else tmcm.thisDriveTransit.setAvailability(false);

        //set dispersion parameters
        tmcm.autoNest.setDispersionParameter(theseParameters.nestlow/tmcm.root.getDispersionParameter());
        tmcm.nonMotorizedNest.setDispersionParameter(theseParameters.nestlow/tmcm.root.getDispersionParameter());
        tmcm.transitNest.setDispersionParameter(theseParameters.nestlow/tmcm.root.getDispersionParameter());
        tmcm.passengerNest.setDispersionParameter(theseParameters.nestlow/tmcm.root.getDispersionParameter());


        //add alternatives, if they are available, to nesting structure
        tmcm.root.computeAvailabilities();
        float logsum =(float)tmcm.root.getUtility();
        return logsum;
     }

    /**
     * setPersonTourModeAttributes
     * 
     * @param originTaz
     * @param destinationTaz
     * @param thisPurpose
     * @param segment
     * @return
     */ 
    public PersonTourModeAttributes setPersonTourModeAttributes(Taz originTaz,
                                            Taz destinationTaz,
                                            char thisPurpose,
                                            int segment){
        
        //PersonTourModeAttributes ptma = new PersonTourModeAttributes();                                                                                                     
        ptma.originZone=originTaz.zoneNumber;
        ptma.destinationZone=destinationTaz.zoneNumber;
                       
        //set tour purpose in person object
        //ptma.tourPurpose=purposes.charAt(purpose);
        ptma.tourPurpose=thisPurpose;                    
                                        
        //set TravelTimeAndCosts (default=offpeak)
        ptma.primaryDuration=120;
                         
        //set primary duration to 480 for work or school, 120 for other purposes
        if(thisPurpose=='w'||thisPurpose=='c')
            ptma.primaryDuration=480;
        else 
            ptma.primaryDuration=120;

        if(thisPurpose=='w'||thisPurpose=='b'){
            ptma.auwk0=auwk0segs[segment];
            ptma.auwk1=auwk1segs[segment];
            ptma.auwk2=auwk2segs[segment];
            ptma.aupr0=0;
            ptma.aupr1=0;
            ptma.aupr2=0;
        }else{
            ptma.auwk0=0;
            ptma.auwk1=0;
            ptma.auwk2=0;
            ptma.aupr0=aupr0segs[segment];
            ptma.aupr1=aupr1segs[segment];
            ptma.aupr2=aupr2segs[segment];
        }
        if(ptma.auwk0==1||ptma.aupr0==1)
            ptma.autos=0;
        else
            ptma.autos=1;
    
        ptma.inclow=inclowsegs[segment];
        ptma.incmed=incmedsegs[segment];
        ptma.inchi=inchisegs[segment];
        return ptma;
    }// end setPersonTourModeAttributes
    
    /**
     * setDepartCost
     * Calculates the depart traves times and costs for the origin-destination pair
     * @param thisPurpose
     * @param skims
     * @param originTaz
     * @param destinationTaz
     * @return
     */
    public TravelTimeAndCost setDepartCost(char thisPurpose,
                                 SkimsInMemory skims,
                                 Taz originTaz,
                                 Taz destinationTaz){
       
        if(thisPurpose=='w'||thisPurpose=='c')                        
            return skims.setTravelTimeAndCost(originTaz.zoneNumber, 
                                              destinationTaz.zoneNumber, 
                                              800);                         
        else 
            return skims.setTravelTimeAndCost(originTaz.zoneNumber, 
                                              destinationTaz.zoneNumber, 
                                              1200);
     }// end setDepartCost
    
    /** setReturnCost
     * Calculates the return travel times and costs for the origin-destination pair
     * @param thisPurpose
     * @param skims
     * @param originTaz
     * @param destinationTaz
     * @return
     */   
    public TravelTimeAndCost setReturnCost(char thisPurpose,
                                            SkimsInMemory skims,
                                            Taz originTaz,
                                            Taz destinationTaz){
        if(thisPurpose=='w'||thisPurpose=='c') 
            return skims.setTravelTimeAndCost(destinationTaz.zoneNumber, 
                                                                    originTaz.zoneNumber, 
                                                                    1800);
        else
        return skims.setTravelTimeAndCost(destinationTaz.zoneNumber, 
                                                                    originTaz.zoneNumber, 
                                                                    1300);
    }//end setReturnCost 
    

    /**
     * writeModeChoiceLogsumsMatrix
     * This method is called either from writeBetaZoneModeChoiceLogsumsMatrix or writeAlphaZoneModeChoiceLogsumsMatrix
     * @param thisMatrix
     */

    private static void writeModeChoiceLogsumsMatrix(ResourceBundle rb, Matrix thisMatrix){

        logger.info("Writing Matrix "+thisMatrix.getName());
        //get path
        String path = ResourceUtil.getProperty(rb, "modeChoiceLogsums.path");
        String mName = thisMatrix.getName()+".zip";
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(path+mName));  //Open for writing
        mw.writeMatrix(thisMatrix);
    }

    public void compareOldAndNew(Matrix newMatrix, Matrix oldMatrix, TazData taz){
        Enumeration originEnum=taz.tazData.elements();
                //enter loop on origin zones
        //      open file for writing
        OutTextFile outFile = new OutTextFile();
        PrintWriter oFile = outFile.open("d:/temp/oldvsnewMCLogsums.csv");
                while(originEnum.hasMoreElements()){
                    
                    Taz originTaz = (Taz) originEnum.nextElement();
                    Enumeration destinationEnum=taz.tazData.elements();
                    // Enter loop on destination zones
                    while(destinationEnum.hasMoreElements()){
                        Taz destinationTaz = (Taz) originEnum.nextElement();
                        

                        oFile.print(originTaz.zoneNumber+",");
                        oFile.print(destinationTaz.zoneNumber+",");
                        oFile.print(newMatrix.getValueAt(originTaz.zoneNumber, 
                                                         destinationTaz.zoneNumber)+",");
                        oFile.print(newMatrix.getValueAt(originTaz.zoneNumber, 
                                                         destinationTaz.zoneNumber));  
                        oFile.println();                               
                    }
                }
                oFile.close();
    }
    
    public Matrix getBetaMCMatrix(Matrix m, String squeezeType){
        AlphaToBetaData aToB = new AlphaToBetaData();
        Matrix betaZoneMatrix = aToB.getSqueezedMatrix(alphaToBeta,m,squeezeType);
        betaZoneMatrix.setName(m.getName()+"beta");
        return betaZoneMatrix;
    }
          
    public static void main (String[] args){
        logger.info("creating tour mode choice logsums");
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/pt/pt.properties"));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/global.properties"));
        CreateModeChoiceLogsums createMCLogsums = new CreateModeChoiceLogsums();
        //createMCLogsums.buildTourModeChoiceModel();
        //read the skims into memory
        TazData taz = new TazData();
        taz.readData(rb, globalRb, "tazData.file");
        
        TourModeChoiceModel tmcm = new TourModeChoiceModel();
        //read the tourModeParameters from jDataStore; if they don't exist, write them to jDataStore first from csv
        TourModeParametersData tmp = new TourModeParametersData();
        tmp.readData(rb,"tourModeParameters.file");
        SkimsInMemory skims = new SkimsInMemory(globalRb);
        skims.readSkims(rb);
        //enter loop on purposes
        for(int purpose=ActivityPurpose.WORK;purpose<2/*purposes.length()*/;++purpose){
            char thisPurpose=ActivityPurpose.ACTIVITY_PURPOSES[purpose];
            //get tour mode choice parameters based on purpose
            TourModeParameters theseParameters = 
                (TourModeParameters) tmp.getTourModeParameters(purpose);

            //enter loop on segments
            for(int segment=1;segment<6/*TOTALSEGMENTS*/;++segment){
                logger.info("Creating ModeChoiceLogsumMatrix for purpose:"+thisPurpose+" segment: "+segment);
                Matrix m = createMCLogsums.setModeChoiceLogsumMatrix(taz,theseParameters, thisPurpose,segment,skims, tmcm);
                logger.info("Writing Matrix "+m.getName());
                //get path
                String path = ResourceUtil.getProperty(rb, "modeChoiceLogsums.path");
                String mName = m.getName()+".zip";        
                MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File("d:/temp/"+mName));  //Open for writing
                mw.writeMatrix(m);   

                logger.info("Output ModeChoiceLogsumMatrix .zip file for purpose:"+thisPurpose+" segmentL "+segment);

                /*
                 * Quality control check - compared output with current Matrices
                 ResourceBundle rb = ResourceUtil.getResourceBundle( "pt" );
                String path = ResourceUtil.getProperty(rb, "Model.skimPath");
                String name = path+String.valueOf(thisPurpose)+segment+"ls.zip";
                MatrixReader oldReader= MatrixReader.createReader(MatrixType.ZIP,new File(name)); 
                Matrix oldMatrix= oldReader.readMatrix(name);
                logger.info("Compare Matrices");
                createMCLogsums.compareOldAndNew(m,oldMatrix);
                */
                if(CREATE_BETA_ZONES){
                    AlphaToBetaData aToB = new AlphaToBetaData();
                    Matrix betaZoneMatrix = createMCLogsums.getBetaMCMatrix(m,"MEAN");
                    logger.info("Writing Matrix "+betaZoneMatrix.getName());
                    //get path
                    mName = betaZoneMatrix.getName()+".zip";        
                    mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(path+mName));  //Open for writing
                    mw.writeMatrix(betaZoneMatrix);   
                }
                logger.info("Created modeChoiceLogsum for purpose "+thisPurpose+" segment "+segment);
            }
        }
        logger.info("created tour mode choice logsums");
        System.exit(1);
    }//end main

    /**
     * loadTableDataSet
     * @param getProperty
     * @return
     */
    //method is not used 9/10/2004
//    public static TableDataSet loadTableDataSet(ResourceBundle rb, String getProperty) {
//
//        String tazToDistrictFile = "d:/tlumip_data/azonebzone";/*ResourceUtil.getProperty(rb, getProperty);*/
//
//        try {
//            String fileName = tazToDistrictFile + ".csv";
//            CSVFileReader reader = new CSVFileReader();
//            TableDataSet table = reader.readFile(new File(fileName));
//            return table;
//        } catch (IOException e) {
//            logger.severe("Can't find taz to district file");
//            e.printStackTrace();
//        }
//        return null;
//    }

    /**
     * get the necessary parameters
     *
     */
   // public void buildTourModeChoiceModel(){
        //read the skims into memory
        //skims = new SkimsInMemory();
        //skims.readSkims();

        //read the taz data from jDataStore; if it doesn't exist, write it to jDataStore from csv file first
        //taz = new TazData();
        //taz.readData("pt","tazData.file");

        //read the tourModeParameters from jDataStore; if they don't exist, write them to jDataStore first from csv
        //tmp = new TourModeParametersData();
        //tmp.readData("pt","tourDestinationParameters.file");

        //tmcm = new TourModeChoiceModel();
        //tmcm.buildModel();

        //alphaToBeta = loadTableDataSet("pt", "alphatobeta");
   // }

}


