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
package com.pb.tlumip.epf;

import com.borland.dx.dataset.Column;
import com.pb.models.reference.ModelComponent;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.datafile.TableDataSet;

import java.util.ResourceBundle;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * EdPiFeedback is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  May 27, 2009
 */
public class EdPiFeedback extends ModelComponent {
    protected static Logger logger = Logger.getLogger(EdPiFeedback.class);
    int timeInterval;

    TableDataSet actSumRefTable;
    TableDataSet edPiFeedParams;
    TableDataSet edPiInflFactors;
    TableDataSet piTMinus1ActSummary;
    TableDataSet piTMinus2ActSummary;

    HashMap<String, Double> lCalculationsMap = new HashMap <String, Double>();


    public EdPiFeedback(ResourceBundle appRb){
        super();
        setApplicationResourceBundle(appRb);
    }

    public void startModel(int baseYear, int timeInterval){
        this.timeInterval = timeInterval;

        logger.info("Reading in the ActivitySummaryReferenceFile");
        actSumRefTable = TableDataSetLoader.loadTableDataSet(appRb, "epf.activity.summary.reference");

        //Read in the ED-PI Feedback input files -
        //ED_PIFeedbackParametersI.csv
        //and ED_PIInfluenceFactors.csv
        logger.info("Reading in the ED_PIFeedbackParametersI.csv");
        edPiFeedParams = TableDataSetLoader.loadTableDataSet(appRb, "epf.feedback.parameters");

        logger.info("Reading in the ED_PIInfluenceFactors.csv");
        edPiInflFactors = TableDataSetLoader.loadTableDataSet(appRb, "epf.influence.factors");

        //This method will involve creating some mappings
        //between the PI industries and the SPG and ALD industries

        //Read in the ActivitySummary file for timeInterval-1
        //and timeInterval-2 and store both the size terms and
        //the composite utilities in a HashMap.
        logger.info("Reading in the ActivitySummary file for timeInterval-1");
        piTMinus1ActSummary = TableDataSetLoader.loadTableDataSet(appRb, "epf.ActivitySummary");

        logger.info("Reading in the ActivitySummary file for timeInterval-2");
        piTMinus2ActSummary = TableDataSetLoader.loadTableDataSet(appRb, "epf.PreviousActivitySummary");

        //For each PI Industry in the ED_PIFeedbackParametersI file,
        //calculate L and store in HashMap.
        calculateL();

        //We need two calculatePIF methods.  One that takes just a
        //*_industry (pi or spg) and the other that takes an array
        //of *_industries (spg and ald).  Save PIF values in
        //the ED_PIInfluenceFactors.csv file

        //Read in the 3 ED output files - ActivityDollarDataForPi
        //JobDataForSPG1 and ConstructionDollarDataForALD for the
        //current time interval

        //For each of the output files, loop thru the industries
        //and calculate the new values.  Store in TableDataSet and
        //write to CSV at the end.

    }

    //For each PI Industry in the ED_PIFeedbackParametersI file,
    //calculate L and store in HashMap.
    private void calculateL(){
        double eta;         //Exp Term Coeff
        double previousC;      //utility of timeInteval - 1 for pi Activity from ActivitySummary
        double previousPreviousC;   //utility of timeInteval - 2 for pi Activity from PreviousActivitySummary
        double previousRefC;        //utility of timeInteval - 1 for pi Activity from Reference Scenario
        double previousPreviousRefC;      //utility of timeInteval - 2 for pi Activity from Reference Scenario

        for(String piActivity: edPiFeedParams.getColumnAsString("PI_Activity")){
            eta = getStringIndexedValue(piActivity, "PI_Activity", edPiFeedParams, "Eta Utility Scaling");
            previousC = getStringIndexedValue(piActivity, "Activity", piTMinus1ActSummary, "CompositeUtility");
            previousPreviousC = getStringIndexedValue(piActivity, "Activity", piTMinus2ActSummary, "CompositeUtility");
            previousRefC = getStringIndexedValue(piActivity, "Activity", actSumRefTable, ""+ (timeInterval-1)+"_CompositeUtility");
            previousPreviousRefC = getStringIndexedValue(piActivity, "Activity", actSumRefTable, ""+ (timeInterval-2)+"_CompositeUtility");

        }
    }

    private double getStringIndexedValue(String actName, String colToFindActNameIn, 
    									TableDataSet table, String colToGetDataFrom){
        //To get the value in the colToGetDataFrom, first loop through the elements 
    	//in the colToFindActNameIn column in search of the specific actName,

    	String[] activityNames = table.getColumnAsString(colToFindActNameIn);
    	for(int i = 0; i < activityNames.length; i++){
    		if( activityNames[i] == actName){
    			int rowNumber=i;
    			break;
    		}
    	}
    		
    	//get the column called colToGetDataFrom from the table
    	//from the array get element[rowNumber]
    	double[] activityValue = table.getColumnAsDoubleFromDouble(colToGetDataFrom);
        return activityValue[rowNumber];    
    }

    private HashMap getSpgMapping(){
    	return null;
    }
    

}
