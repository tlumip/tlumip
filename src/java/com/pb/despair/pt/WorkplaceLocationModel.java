package com.pb.despair.pt;

import java.io.File;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ResourceBundle;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom; 
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;


/** 
 * WorkplaceLocationModel determines the workplace location of a person 
 * using labor flows between zones by occupation.  The model uses a 
 * Monte Carlo Selection.
 * 
 * @author Steve Hansen
 * @version 1.0 03/28/2004
 * 
 */

public class WorkplaceLocationModel{

    private int lastWorkLogsumSegment = -1;    
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");

    public WorkplaceLocationModel(){}

    private Matrix getLaborFlowProbabilities(ResourceBundle rb, int occupation, int segment){
        String path = ResourceUtil.getProperty(rb, "laborFlows.path");
        String mName = new String(path+ "lf"+occupation+segment+new String(".zip"));
        MatrixReader lsReader= MatrixReader.createReader(MatrixType.ZIP,new File(mName));
        lastWorkLogsumSegment = occupation*10 + segment;
        return lsReader.readMatrix(mName); 
        
    }
    
    public short chooseWorkplace(Matrix laborFlowMatrix, PTPerson thisPerson,
                               TazData tazs){
        int destination = 0;
        if(thisPerson.employed){

            double selector = SeededRandom.getRandom();
            //sum is a running calculation of the total proportion of labor flows 
            double probabilityTotal = 0;
            int counter = 0;
            //origin taz number
            int origin = (int)thisPerson.homeTaz;
            //destination taz number to be calculated.
      
            Enumeration destinationEnum=tazs.tazData.elements();
            while(destinationEnum.hasMoreElements()){
                counter++;
            	Taz destinationTaz = (Taz) destinationEnum.nextElement();
                float flow = laborFlowMatrix.getValueAt(origin,destinationTaz.zoneNumber);
                if(Float.isNaN(flow)) {
                    logger.severe("FLOW IS NaN. matrix is " + laborFlowMatrix.getName() + " origin: " + origin
                                    + " destination " + destinationTaz.zoneNumber);
                    System.exit(10);
                }
            	probabilityTotal = probabilityTotal+(laborFlowMatrix.getValueAt(origin,destinationTaz.zoneNumber));
                if(Double.isNaN(probabilityTotal)) {
                    logger.severe("Probability IS NaN. matrix is " + laborFlowMatrix.getName() + " origin: " + origin
                                    + " destination " + destinationTaz.zoneNumber +
                                    "laborFlowValue is: " + laborFlowMatrix.getValueAt(origin,destinationTaz.zoneNumber));
                    System.exit(10);
                }
            	if (probabilityTotal > selector){
            		destination = destinationTaz.zoneNumber;
            		//logger.finer("Workplace location: "+destination);
            		break;
            	}        
            }

            if(destination==0){
            	logger.severe("Error With workplace location model - destination TAZ shouldn't ==0!");
                logger.severe("Selector value: " + selector + " Probability total: " + probabilityTotal +
                        " Counter: " + counter);
                System.exit(1);
            }
            //lastWorkLogsumSegment = thisPerson.occupation*10 + thisPerson.workSegment;
        }
        return (short)destination;
    }
    
    public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        WorkplaceLocationModel wlm = new WorkplaceLocationModel();
        PTDataReader dataReader = new PTDataReader(rb, globalRb);
        logger.info("Adding synthetic population from database"); 
        PTHousehold[] households = dataReader.readHouseholds("households.file");
        PTPerson[] persons = dataReader.readPersons("persons.file");
        dataReader.addPersonInfoToHouseholdsAndHouseholdInfoToPersons(households,persons);

        households = dataReader.runAutoOwnershipModel(households);
      
        Arrays.sort(persons);

        logger.info("Creating labor flow probabilities.");

        //read the tourDestinationParameters from csv to TableDataSet
        logger.info("Reading tour destination parameters");
        TourDestinationParametersData tdpd = new TourDestinationParametersData();
        tdpd.readData(rb,"tourDestinationParameters.file");
          
        //read the stopDestinationParameters from csv to TableDataSet
        logger.info("Reading stop destination parameters");
        StopDestinationParametersData sdpd = new StopDestinationParametersData();
        sdpd.readData(rb,"stopDestinationParameters.file");
        
        //read the taz data from csv to TableDataSet
        logger.info("Adding TazData");
        TazData tazs = new TazData();
        tazs.readData(rb, globalRb, "tazData.file");
        tazs.collapseEmployment(tdpd, sdpd);
        long startTime = System.currentTimeMillis();
        int initp = 0;
        while(persons[initp].employed==false) initp++;
        
        Matrix laborFlowProbabiliites = wlm.getLaborFlowProbabilities(rb,persons[initp].occupation,persons[initp].householdWorkSegment);
        wlm.lastWorkLogsumSegment = persons[initp].occupation*10 + persons[initp].householdWorkSegment;
        for(int p=0;p<persons.length;p++){
            if(persons[p].employed){
                if(persons[p].occupation*10 + persons[p].householdWorkSegment!=wlm.lastWorkLogsumSegment){
                    logger.fine("labor flow probabilities "+persons[p].occupation+" "+persons[p].householdWorkSegment);
                    laborFlowProbabiliites = wlm.getLaborFlowProbabilities(rb,persons[p].occupation,persons[p].householdWorkSegment);
                    wlm.lastWorkLogsumSegment = persons[p].occupation*10 + persons[p].householdWorkSegment;
                }
                //logger.fine("worktaz "+persons[p].workTaz);
                persons[p].workTaz = wlm.chooseWorkplace(laborFlowProbabiliites,persons[p],tazs);
                if(persons[p].worksTwoJobs)
                    persons[p].workTaz2 = wlm.chooseWorkplace(laborFlowProbabiliites,persons[p],tazs);
                //logger.fine("worktaz after"+persons[p].workTaz);
            }
        }
        logger.fine("Time to run workplace location model for all households = "+(System.currentTimeMillis()-startTime)/1000);
        households = dataReader.addPersonsToHouseholds(households,persons);
    }
}
