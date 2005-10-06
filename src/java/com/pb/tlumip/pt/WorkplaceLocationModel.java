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

import java.io.File;
import org.apache.log4j.Logger;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ResourceBundle;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom; 
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;


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
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt");

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
                probabilityTotal = probabilityTotal+(laborFlowMatrix.getValueAt(origin,destinationTaz.zoneNumber));
                if (probabilityTotal > selector){
            		destination = destinationTaz.zoneNumber;
            		break;
            	}        
            }

            if(destination==0){
            	logger.error("Error With workplace location model - destination TAZ shouldn't ==0!");
                logger.error("Selector value: " + selector + " Probability total: " + probabilityTotal +
                        " Counter: " + counter);
                logger.error("PersonID "+ thisPerson.ID+" Industry "+thisPerson.industry+
                        " Occupation "+thisPerson.occupation+" WorkSegment "+thisPerson.householdWorkSegment);
                logger.error("The labor flow matrix will be written to the debug directory");
                MatrixWriter mWriter = PTResults.createMatrixWriter(laborFlowMatrix.getName());
                mWriter.writeMatrix(laborFlowMatrix);
                //in the interest of not stopping the model run we will assign the
                // destinationTaz to be the originTaz.  The user will be notified at the
                // end of the run
                destination = (int) thisPerson.homeTaz;
            }
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
                    if(logger.isDebugEnabled()) {
                        logger.debug("labor flow probabilities "+persons[p].occupation+" "+persons[p].householdWorkSegment);
                    }
                    laborFlowProbabiliites = wlm.getLaborFlowProbabilities(rb,persons[p].occupation,persons[p].householdWorkSegment);
                    wlm.lastWorkLogsumSegment = persons[p].occupation*10 + persons[p].householdWorkSegment;
                }
                persons[p].workTaz = wlm.chooseWorkplace(laborFlowProbabiliites,persons[p],tazs);
                if(persons[p].worksTwoJobs)
                    persons[p].workTaz2 = wlm.chooseWorkplace(laborFlowProbabiliites,persons[p],tazs);
            }
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Time to run workplace location model for all households = "+(System.currentTimeMillis()-startTime)/1000);
        }
        households = dataReader.addPersonsToHouseholds(households,persons);
    }
}