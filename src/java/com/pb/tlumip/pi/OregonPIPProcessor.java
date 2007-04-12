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
package com.pb.tlumip.pi;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.datafile.TableDataSetIndex;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.CSVMatrixWriter;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pecas.Commodity;
import com.pb.models.pecas.LaborProductionAndConsumption;
import com.pb.models.pecas.PIPProcessor;
import com.pb.models.pecas.SomeSkims;
import com.pb.models.pecas.TransportKnowledge;
import com.pb.models.reference.IndustryOccupationSplitIndustryReference;
import com.pb.tlumip.model.IncomeSize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author John Abraham
 *
 */
public class OregonPIPProcessor extends PIPProcessor {

    String year;
    IndustryOccupationSplitIndustryReference indOccRef; //initially null
    String[] occupations; //initially null
    private AlphaToBeta beta2CountyMap = null; /* used for squeezing the commodities from beta flows to County flows */
    MatrixCompression compressor = null;
    
    public OregonPIPProcessor() {
        super();

    }

    /**
     * @param timePeriod
     */
    public OregonPIPProcessor(int timePeriod, ResourceBundle piRb, ResourceBundle globalRb) {
        
        super(timePeriod, piRb, globalRb);
        indOccRef = new IndustryOccupationSplitIndustryReference( ResourceUtil.getProperty(globalRb, "sw_ind_occ_split.correspondence.fileName"));
        this.year = timePeriod < 10 ? "1990" : "2000";
        
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.pi.PIPProcessor#setUpPi()
     */
    public void setUpPi() {

        super.setUpPi();
    }

    public void doProjectSpecificInputProcessing() {
        indOccRef = new IndustryOccupationSplitIndustryReference( ResourceUtil.getProperty(globalRb, "sw_ind_occ_split.correspondence.fileName"));
        String calibrationMetaParameters = ResourceUtil.getProperty(piRb,"pi.readMetaParameters");
        if (calibrationMetaParameters.equalsIgnoreCase("true")) {
            setUpMetaParameters();
        }
        
        boolean doIntegratedModuleRun = false;
        String oregonInputsString = ResourceUtil.getProperty(piRb, "pi.oregonInputs");
        if (oregonInputsString != null ) {
            if (oregonInputsString.equalsIgnoreCase("true")) {
                doIntegratedModuleRun = true;
            }
        }
        if (!doIntegratedModuleRun) return;
        String currPath = ResourceUtil.getProperty(piRb,"pi.current.data");
        File actW = new File(currPath + "ActivitiesW.csv");
        if(actW.exists()){
            actW.delete();
            logger.info("Deleted old ActivitiesW.csv to prepare for new file");
        }

        File flrW = new File(currPath + "FloorspaceW.csv");
        if(flrW.exists()){
            flrW.delete();
            logger.info("Deleted old FloorspaceW.csv to prepare for new file");
        }

        File zoneW = new File(currPath + "ActivitiesZonalValuesW.csv");
        if(zoneW.exists()){
            zoneW.delete();
            logger.info("Deleted old ActivitiesZonalValuesW.csv to prepare for new file");
        }

        File makeUseW = new File(currPath + "MakeUseW.csv");
        if(makeUseW.exists()){
            makeUseW.delete();
            logger.info("Deleted old MakeUseW.csv to prepare for new file");
        }

        createActivitiesWFile();
        createFloorspaceWFile();
        createActivitiesZonalValuesWFile();
        createMakeUseWFile();
    }

    private void createActivitiesWFile(){
        logger.info("Creating new ActivitiesW.csv using current ED and SPG data");
        boolean readSpgHHFile = (ResourceUtil.getProperty(piRb, "pi.readHouseholdsByHHCategory").equalsIgnoreCase("true"));
        boolean readEDDollarFile = (ResourceUtil.getProperty(piRb, "pi.readActivityDollarDataForPI").equalsIgnoreCase("true"));
        if(readSpgHHFile || readEDDollarFile)
        {
            CSVFileReader reader = new CSVFileReader();
            // read the PI intput file into a TableDataSet
            String piInputsPath = ResourceUtil.getProperty(piRb, "pi.base.data");
            TableDataSet actI = null;
            try {
                actI = reader.readFile(new File(piInputsPath + "ActivitiesI.csv"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            int sizeColumnPosition = actI.checkColumnPosition("Size");
            int activityColumnPosition = actI.checkColumnPosition("Activity");

            // the SPG1 File has HHCategory as rows and Size as columns
            if (readSpgHHFile) {
                String hhPath = ResourceUtil.getProperty(piRb, "spg.input.data");
                //read the SPG input file and put the data into an array by hhIndex
                TableDataSet hh = null;
                try {
                    hh = reader.readFile(new File(hhPath + "householdsByHHCategory.csv"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                IncomeSize inc = new IncomeSize();
                int[] householdsByIncomeSize = new int[hh.getRowCount()];
                for (int r = 0; r < hh.getRowCount(); r++) {
                    int incomeSize = inc.getIncomeSizeIndex(hh.getStringValueAt(r + 1, 1)); //HHCategory
                    householdsByIncomeSize[incomeSize] = (int) hh.getValueAt(r + 1, 2); //Size
                }
                // update the values in the actI TableDataSet using the SPG1 results
                for (int r = 0; r < actI.getRowCount(); r++) {

                    int incomeSize = inc.getIncomeSizeIndex(actI.getStringValueAt(r + 1, activityColumnPosition));

                    if (incomeSize >= 0) {
                        actI.setValueAt(r + 1, sizeColumnPosition, householdsByIncomeSize[incomeSize]);
                    }
                }
            }
            //The ED File has 2 columns, Activity and Dollar Amounts
            if(readEDDollarFile){
                String dollarPath = ResourceUtil.getProperty(piRb,"ed.input.data");
                //read the ED input file and put the data into a hashmap by industry
                TableDataSet dollars = null;
                try {
                    dollars = reader.readFile(new File(dollarPath + "ActivityDollarDataForPI.csv"));
                } catch (IOException e) {
                    throw new RuntimeException("Can't find ActivityDollarDataForPI.csv file");
                }

                HashMap<String, Float> dollarsByIndustry = new HashMap<String, Float>(); //header row does not count in row count
                for(int r = 0; r < dollars.getRowCount(); r++){
                    String splitIndustryLabel = dollars.getStringValueAt(r + 1, 1);//Activity Name
                    dollarsByIndustry.put(splitIndustryLabel, dollars.getValueAt(r + 1, 2)); //Total Dollars
                }
                //update the values in the actI TableDataSet using the ED results
                for(int r=0; r< actI.getRowCount(); r++){
                    String key = actI.getStringValueAt(r+1,activityColumnPosition);
                    if (dollarsByIndustry.containsKey(key)){
                        actI.setValueAt(r+1,sizeColumnPosition, dollarsByIndustry.get(key));
                    }
                }
            }
            // write the updated PI input file
            String piOutputsPath = ResourceUtil.getProperty(piRb, "output.data");
            CSVFileWriter writer = new CSVFileWriter();
            try {
                writer.writeFile(actI, new File(piOutputsPath + "ActivitiesW.csv"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }//otherwise just use the ActivitiesI.csv file
    }

    /* This method will read in the FloorspaceI.csv file that was output by ALD
    * and replace the aglog data with the PIAgForestFloorspace.csv base year data.  This will
    * happen every year.
    */

    public void createFloorspaceWFile() {
        logger.info("Creating new FloorspaceW.csv using current ALD PIAgForestFloorspace.csv file");
        //Read in the FloorspaceI.csv file that was produced by ALD
        TableDataSet floorspaceTable = loadTableDataSet("FloorspaceI","ald.input.data");
        if (floorspaceTable == null)
            throw new RuntimeException("floorspaceTable is null.");
        //And the PIAgForestFloorspace.csv file that was created by Tara
        TableDataSet piAgForTable = loadTableDataSet("PIAgForestFloorspace", "pi.base.data");
        if (piAgForTable == null)
            throw new RuntimeException("piAgForTable is null.");
        // Find the maximum alphazone in the file - we haven't yet read in the FloorspaceZones file
        int azoneCol = piAgForTable.checkColumnPosition("AZone");
        int flrTypeCol = piAgForTable.checkColumnPosition("FLRName");
        int mSqftCol = piAgForTable.checkColumnPosition("BldgMSQFT");
        int max = 0;
        for(int row = 1; row < piAgForTable.getRowCount(); row ++){
            if((int)piAgForTable.getValueAt(row,azoneCol) > max)
                max = (int)piAgForTable.getValueAt(row,azoneCol);
        }
        //Take the data out of the PIAgForest file and put it into temporary arrays.
        float[] agSpaceByZone = new float[max + 1];
        float[] forSpaceByZone = new float[max + 1];

        for(int row = 1; row <= piAgForTable.getRowCount(); row ++){
            if(piAgForTable.getStringValueAt(row,flrTypeCol).equalsIgnoreCase("FLR Agriculture")){
                agSpaceByZone[(int)piAgForTable.getValueAt(row,azoneCol)] = piAgForTable.getValueAt(row,mSqftCol);
            }
            else if(piAgForTable.getStringValueAt(row,flrTypeCol).equalsIgnoreCase("FLR Logging")){
                forSpaceByZone[(int)piAgForTable.getValueAt(row,azoneCol)] =  piAgForTable.getValueAt(row,mSqftCol);
            }
            else {
                logger.fatal("Bad floor type name in PIAgForestFloorspace file");
                //TODO - send to node exception log
                System.exit(1);
            }
        }
        //Now go thru the floorspaceTable and replace the data with the data from our arrays
        flrTypeCol = floorspaceTable.checkColumnPosition("FLRName");
        azoneCol = floorspaceTable.checkColumnPosition("AZone");
        mSqftCol = floorspaceTable.checkColumnPosition("BldgMSQFT");
        String flrType = null;
        int azone = 0;
        float mSqft = 0;
        int replaceCount = 0;
        for(int row = 1; row <= floorspaceTable.getRowCount(); row++){
            flrType = floorspaceTable.getStringValueAt(row,flrTypeCol);
            if(!flrType.equalsIgnoreCase("FLR Agriculture") && !flrType.equalsIgnoreCase("FLR Logging"))
                continue;  //this row does not need replacing, go to next row
            else {
                azone = (int)floorspaceTable.getValueAt(row,azoneCol);
                if(flrType.equalsIgnoreCase("FLR Agriculture")){
                    mSqft = agSpaceByZone[azone];
                    floorspaceTable.setValueAt(row,mSqftCol,mSqft);
                    replaceCount++;
                }else if(flrType.equalsIgnoreCase("FLR Logging")){
                    mSqft = forSpaceByZone[azone];
                    floorspaceTable.setValueAt(row,mSqftCol, mSqft);
                    replaceCount++;
                }
            }
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Replaced " + replaceCount + " values in the Floorspace Table");
        }
        //Now write out the FloorspaceW.csv file
        String piOutputsPath = ResourceUtil.getProperty(piRb, "output.data");
        CSVFileWriter writer = new CSVFileWriter();
        try {
            writer.writeFile(floorspaceTable, new File(piOutputsPath + "FloorspaceW.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void readFloorspace() {
        if (maxAlphaZone == 0) readFloorspaceZones();
        TableDataSet floorspaceTable = loadTableDataSet("FloorspaceW","pi.current.data");
        floorspaceInventory = new Hashtable();
        int alphaZoneColumn = floorspaceTable.checkColumnPosition("AZone");
        int quantityColumn = floorspaceTable.checkColumnPosition("BldgMSQFT");
        int floorspaceTypeColumn = floorspaceTable.checkColumnPosition("FLRName");
        for (int row = 1; row <= floorspaceTable.getRowCount(); row++) {
            int alphaZone = (int) floorspaceTable.getValueAt(row,alphaZoneColumn);
            float quantity = floorspaceTable.getValueAt(row,quantityColumn);
            String commodityName = floorspaceTable.getStringValueAt(row,floorspaceTypeColumn);
            Commodity c = Commodity.retrieveCommodity(commodityName);
            if (c==null) throw new Error("Bad commodity name "+commodityName+" in floorspace inventory table");
            ZoneQuantityStorage fi = (ZoneQuantityStorage) floorspaceInventory.get(commodityName);
            if (fi==null) {
                fi = new ZoneQuantityStorage(commodityName);
                floorspaceInventory.put(commodityName,fi);
            }
            fi.increaseQuantityForZone(alphaZone,quantity);
            //fi.inventory[alphaZone]+= quantity;
        }
    }
    /* This method will read in the base year ActivitiesZonalValues file and
    * update the "InitialQuantity" field with the previous year's construction values from ALD's
    * Increment.csv file, and the "Quantity" numberfrom the previous year's ActivityLocations.csv
    * pi file
    */
    private void createActivitiesZonalValuesWFile(){
        logger.info("Creating new ActivitiesZonalValuesW.csv using current PI and ALD data");
        //First read in the t0/pi/ActivitiesZonalValuesI.csv file
        TableDataSet zonalValuesTable = loadTableDataSet("ActivitiesZonalValuesI","pi.base.data");

        //Next read in the alpha2beta.csv file from the reference directory.  This will set
        //up a look-up array so that we know which alphazones are in which beta zones
        //It will also set the max alpha and max beta zone which we can "get" from a2bMap.
        TableDataSet alpha2betaTable = loadTableDataSet("alpha2beta","reference.data");
        AlphaToBeta a2bMap = new AlphaToBeta(alpha2betaTable);

        //Next read in the previous year's pi/ActivityLocations.csv file.
        // ( if this file doesn't exist we will assume that it is year 1 and we do not
        //   need to integrate the 2 files)
        logger.info("\tChecking for previous year's ActivityLocations.csv file");
        TableDataSet actLocationTable = loadTableDataSet("ActivityLocations","pi.previous.data");
        if(actLocationTable != null){  // null implies that the file was not there so can skip this step
            logger.info("\t\tFound it!  Updating Initial Quantities....");
            HashMap activityQuantities = new HashMap();
            for(int r= 1; r<= actLocationTable.getRowCount();r++){
                String activity = actLocationTable.getStringValueAt(r,"Activity"); //get the activityName
                int zone = (int) actLocationTable.getValueAt(r,"ZoneNumber");
                float qty = actLocationTable.getValueAt(r,"Quantity");
                if(!activityQuantities.containsKey(activity)){  //this activity is not yet in the Map
                    float[] qtyByZone = new float[a2bMap.getMaxBetaZone() + 1];
                    qtyByZone[zone] = qty;
                    activityQuantities.put(activity,qtyByZone);
                }else{ //this activity is already in the map, so get the array and put in the value
                    ((float[]) activityQuantities.get(activity))[zone]=qty;
                }
            } //next row of table

            //Now that all the info is in the HashMap, go thru the ZonalValuesTable and update
            //the info
            int initQtyCol = zonalValuesTable.getColumnPosition("InitialQuantity");
            int count = 0;
            for(int r=1; r<= zonalValuesTable.getRowCount(); r++){
                String activity = zonalValuesTable.getStringValueAt(r,"Activity");
                int zone = (int) zonalValuesTable.getValueAt(r,"ZoneNumber");
                float qty = ((float[])activityQuantities.get(activity))[zone];
                zonalValuesTable.setValueAt(r,initQtyCol,qty);
                count++;
            }
            logger.info("\t\t\tWe replaced " + count + " quantities in the ZonalValuesTable");
        }//  We are done integrating the ActivityLocations info into ZonalValuesTable
        else logger.info("\tNo base year ActivityLocations file - Do the ConstructionSizeTerm updates");
        //Now we need to read in the floorspace quantities from the ALD Increments.csv file
        //add up the sqft by betazone and then update the "SizeTerm" in the ZonalValuesTable
        int nFlrNames = 0;                                   //use thess to make sure the increments
        int[] nAddsByZone = new int[a2bMap.getMaxAlphaZone() + 1]; //file only lists each flrType/zone pair once.
        String currentFlrName = "";

        logger.info("\tChecking for current year ALD Increments.csv file");
        TableDataSet incrementsTable = loadTableDataSet("Increments","ald.input.data");
        if(incrementsTable == null) logger.warn("\tALD has not been run or did not output an Increments.csv file." +
                "  The construction size terms will not be updated. ");
        else {
            logger.info("\t\tFound it! We are now updating the construction size terms....");
            float[] mSqftByAZone = new float[a2bMap.getMaxAlphaZone() + 1];
            for(int r=1; r<= incrementsTable.getRowCount(); r++){
                String flrName = incrementsTable.getStringValueAt(r,"FLRName");
                if(!flrName.equals(currentFlrName)){
                    nFlrNames++;
                    currentFlrName = flrName;
                }
                int zone = (int)incrementsTable.getValueAt(r,"AZone");
                float mSqft = incrementsTable.getValueAt(r,"IncMSQFT");
                if(mSqft < 0) mSqft=0;  //don't add up negative numbers
                mSqftByAZone[zone] += mSqft;
                nAddsByZone[zone]++;
            }
            //Each floor name should have had 1 value for each zone so each element in the nAddsByZone array
            //should be equal to the nFlrNames.  If not, we have a problem with the Increments.csv file
            logger.info("\t\t\tEach Zone should have added up " + nFlrNames + " values");
            int[] aZones = a2bMap.getAlphaExternals();
            for(int i=1; i< aZones.length; i++){
                if(nAddsByZone[aZones[i]] != nFlrNames){
                    logger.error("\t\t\t\tZone " + a2bMap.getAlphaExternals()[i] + " added up " + nAddsByZone[a2bMap.getAlphaExternals()[i]]);
                    logger.error("\t\t\t\tCheck the ald/Increments.csv file - there is an error");
                }
            }
            logger.info("\t\t\tIf no warning messages appeared, proceed");

            //Now that we have the total by alpha zone we need to get the total by beta zone
            float[] mSqftByBZone = new float[a2bMap.getMaxBetaZone() + 1];
            for(int i=1; i<aZones.length; i++){
                int betaZone = a2bMap.getBetaZone(aZones[i]);
                if(logger.isDebugEnabled()) {
                    logger.debug("alphaZone " + aZones[i] + " = betaZone " + betaZone);
                }
                mSqftByBZone[betaZone] += mSqftByAZone[aZones[i]];
            }

            //And then update the ZonalValuesTable
            int sizeTermCol = zonalValuesTable.getColumnPosition("SizeTerm");
            for(int r=1; r<=zonalValuesTable.getRowCount();r++){
                String activity = zonalValuesTable.getStringValueAt(r,"Activity");
                if(!activity.equalsIgnoreCase("CONSTRUCTION")) continue;
                else{
                    int zone = (int)zonalValuesTable.getValueAt(r,"ZoneNumber");
                    zonalValuesTable.setValueAt(r,sizeTermCol,mSqftByBZone[zone]);
                }
            }
        }//the SizeTerms have been updated

        //OK, now write out the zonalValuesTable as ActivitiesZonalValuesW.csv into
        //the current pi directory
        logger.info("Writing out the ActivitiesZonalValuesW.csv file to the current pi directory");
        String piOutputsPath = ResourceUtil.getProperty(piRb, "output.data");
        CSVFileWriter writer = new CSVFileWriter();
        try {
            writer.writeFile(zonalValuesTable, new File(piOutputsPath + "ActivitiesZonalValuesW.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createMakeUseWFile(){
        logger.info("Creating new MakeUseW.csv using current ED data");

        //First read in the activity dollar data for pi that ED produced
        //and add up the total dollars of production (don't include capitalists and govt inst.)
        TableDataSet dollars = loadTableDataSet("ActivityDollarDataForPI", "ed.input.data" );
        double totalDollarsProduction = 0.0;
        for(int row=1; row <= dollars.getRowCount(); row++){
            if(!dollars.getStringValueAt(row, 1).equalsIgnoreCase("Capitalists" ) &&
                    !dollars.getStringValueAt(row, 1).equalsIgnoreCase("GovInstitiutions")){
                totalDollarsProduction += dollars.getValueAt(row, 2);
            }
        }
        double totalDollarsProductionInMillions = totalDollarsProduction/1000000.0;
        logger.info("TotalDollarsProductionInMillions: " + totalDollarsProductionInMillions);

        //Next read in the job data for spg1 that ED produced and add up the total
        //number of jobs
        TableDataSet jobs = loadTableDataSet("JobDataForSPG1", "ed.input.data");
        double totalJobs = 0.0;
        for(int row=1; row <= jobs.getRowCount(); row++){
            totalJobs += jobs.getValueAt(row, 2);
        }
        logger.info("Total jobs: " + totalJobs);

        //Calculate Jobs/Dollars ratio
        double dollarsToJobs = totalDollarsProductionInMillions / totalJobs;
        logger.info("DollarsToJobs: " + dollarsToJobs);

        //Read the 98 ratio from the properties file
        double dollarsToJobsTo98 = ResourceUtil.getDoubleProperty(piRb, "pi.98.productivity.rate", 0.094129);
        logger.info("DollarsToJobs-98: " +  dollarsToJobsTo98);

        //Calculate the LaborUseScalingFactor
        double laborUseScalor = dollarsToJobsTo98 / dollarsToJobs;
        logger.info("LaborUseScalingFactor: " + laborUseScalor);

        if(timePeriod == 8 && Math.abs(1-laborUseScalor) > .1){
            logger.warn("WARNING: Expected LaborUseScalingFactor is 1, Actual value is " + laborUseScalor);
        }

        //Now scale minimum and the discretionary amounts of the activities that use labor commodities
        //in the MakeUseI.csv file and write it out as MakeUseW.csv
        TableDataSet makeUseITable = loadTableDataSet("MakeUseI","pi.base.data");
        String activityName;
        String commodityName;
        String mOrU;
        float minimum;
        float discretionary;
        for(int row = 1; row <= makeUseITable.getRowCount(); row++){
            activityName = makeUseITable.getStringValueAt(row,"Activity");
            if(!activityName.startsWith("HH")){
                commodityName = makeUseITable.getStringValueAt(row, "Commodity");
                if(indOccRef.getOccupationLabels().contains(commodityName)){
                    mOrU = makeUseITable.getStringValueAt(row, "MorU");
                    if(mOrU.equalsIgnoreCase("U")){
                        minimum = makeUseITable.getValueAt(row, "Minimum");
                        discretionary = makeUseITable.getValueAt(row, "Discretionary");
                        //mulitiply by scalor
                        minimum *= laborUseScalor;
                        discretionary *= laborUseScalor;
                        makeUseITable.setValueAt(row, "Minimum", minimum);
                        makeUseITable.setValueAt(row, "Discretionary", discretionary);
                    }
                }
            }
        }
        logger.info("Writing out the MakeUseW.csv file to the current pi directory");
        String piOutputsPath = ResourceUtil.getProperty(piRb, "output.data");
        CSVFileWriter writer = new CSVFileWriter();
        try {
            writer.writeFile(makeUseITable, new File(piOutputsPath + "MakeUseW.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Read in some parameter functions, and adjust the parameters as appropriate
     */
    private void setUpMetaParameters() {
        logger.info("Generating parameters from metaparameters");
        CSVFileReader reader = new CSVFileReader();
        CSVFileWriter writer = new CSVFileWriter();
        String piInputsPath = ResourceUtil.getProperty(piRb,"pi.base.data");
        reader.setMyDirectory(new File(piInputsPath));
        writer.setMyDirectory(new File(piInputsPath));
        writer.setMyDecimalFormat(new GeneralDecimalFormat("0.############E0",10000,.001));
        TableDataSetCollection myCollection = new TableDataSetCollection(reader,writer);
        TableDataSetIndex metaParamIndex = new TableDataSetIndex(myCollection,"MetaParameters");
        String[] temp = {"ParameterName"};
        metaParamIndex.setIndexColumns(temp, new String[0]);
        String[] smallHouseholds = {
            "HH0to5k1to2",
            "HH5to10k1to2",
            "HH10to15k1to2",
            "HH15to20k1to2",
            "HH20to30k1to2",
            "HH30to40k1to2",
            "HH40to50k1to2",
            "HH50to70k1to2",
            "HH70kUp1to2"};
        String[] largeHouseholds = {
            "HH0to5k3plus",
            "HH5to10k3plus",
            "HH10to15k3plus",
            "HH15to20k3plus",
            "HH20to30k3plus",
            "HH30to40k3plus",
            "HH40to50k3plus",
            "HH50to70k3plus",
            "HH70kUp3plus"};

        TableDataSetIndex makeUseIndex = new TableDataSetIndex(myCollection,"MakeUseI");
        String[] temp2 = {"Activity","Commodity","MorU"};
        makeUseIndex.setIndexColumns(temp2, new String[0]);

        String[] parameterName = new String[1];
        int[] nothing = new int[0];
        TableDataSet parameters = metaParamIndex.getMyTableDataSet();
        int column = parameters.checkColumnPosition("ParameterValue");

        // get parameters 1 at a time
        parameterName[0] = "MHConstant";
        int[] rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double mhConstant = parameters.getValueAt(rows[0],column);
        parameterName[0] = "MFConstant";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double mfConstant = parameters.getValueAt(rows[0],column);
        parameterName[0] = "ATConstant";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double atConstant = parameters.getValueAt(rows[0],column);
        parameterName[0] = "RuralConstant";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double ruralConstant = parameters.getValueAt(rows[0],column);
        parameterName[0] = "SFDLargeHHConstant";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double sfdLargeHHConstant = parameters.getValueAt(rows[0],column);
        parameterName[0] = "MHIncome";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double mhIncome = parameters.getValueAt(rows[0],column);
        parameterName[0] = "MFIncome";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double mfIncome = parameters.getValueAt(rows[0],column);
        parameterName[0] = "ATIncome";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double atIncome = parameters.getValueAt(rows[0],column);
        parameterName[0] = "RuralIncome";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double ruralIncome = parameters.getValueAt(rows[0],column);
        parameterName[0] = "SFDLargeHHIncome";
        rows = metaParamIndex.getRowNumbers(parameterName,nothing);
        double sfdLargeHHIncome = parameters.getValueAt(rows[0],column);

        String[] makeUseKeys = new String[3];
        makeUseKeys[2] = "U";
        TableDataSet makeUseTable = makeUseIndex.getMyTableDataSet();
        int constantColumn = makeUseTable.checkColumnPosition("UtilityOffset");
        for (int incomeCat = 0;incomeCat<smallHouseholds.length;incomeCat++) {
            // mobile home constants
            makeUseKeys[0] = smallHouseholds[incomeCat];
            makeUseKeys[1] = "FLR MH";
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mhConstant+incomeCat*mhIncome));
            makeUseKeys[0] = largeHouseholds[incomeCat];
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mhConstant+incomeCat*mhIncome));

            // rural mobile home constants
            makeUseKeys[0] = smallHouseholds[incomeCat];
            makeUseKeys[1] = "FLR RRMH";
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mhConstant+ruralConstant+incomeCat*(mhIncome+ruralIncome)));
            makeUseKeys[0] = largeHouseholds[incomeCat];
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mhConstant+ruralConstant+incomeCat*(mhIncome+ruralIncome)));

            // multi family constants
            makeUseKeys[0] = smallHouseholds[incomeCat];
            makeUseKeys[1] = "FLR MF";
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mfConstant+incomeCat*mfIncome));
            makeUseKeys[0] = largeHouseholds[incomeCat];
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mfConstant+incomeCat*mfIncome));

            // attached home constants
            makeUseKeys[0] = smallHouseholds[incomeCat];
            makeUseKeys[1] = "FLR AT";
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (atConstant+incomeCat*atIncome));
            makeUseKeys[0] = largeHouseholds[incomeCat];
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (atConstant+incomeCat*atIncome));

            // rural sfd constants
            makeUseKeys[0] = smallHouseholds[incomeCat];
            makeUseKeys[1] = "FLR RRSFD";
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (ruralConstant+incomeCat*ruralIncome));
            makeUseKeys[0] = largeHouseholds[incomeCat];
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (ruralConstant+sfdLargeHHConstant+incomeCat*(ruralIncome+sfdLargeHHIncome)));

            // sfd constants
            makeUseKeys[0] = largeHouseholds[incomeCat];
            makeUseKeys[1] = "FLR SFD";
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (sfdLargeHHConstant+incomeCat*sfdLargeHHIncome));

           }
        makeUseIndex.dispose();
        metaParamIndex.dispose();
        myCollection.flush();
        myCollection.close();
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.pi.PIPProcessor#setUpTransportConditions(java.lang.String[])
     */
    protected void setUpTransportConditions(String[] skimNames) {
        logger.info("Setting up Transport Conditions");
        String path1 = ResourceUtil.getProperty(piRb, "pt.input.data");
        String path2 = ResourceUtil.getProperty(piRb, "ts.input.data");

        SomeSkims someSkims = new SomeSkims(path1, path2);
        TransportKnowledge.globalTransportKnowledge = someSkims;
        for (int s=0;s<skimNames.length;s++) {
            someSkims.addZipMatrix(skimNames[s]);
        }
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.pi.PIPProcessor#writeOutputs()
     */
    public void writeOutputs() {
        super.writeOutputs();
        writeLaborConsumptionAndProductionFiles(); //writes out laborDollarProduction.csv,laborDollarConsumption.csv
    }

    public void writeLaborConsumptionAndProductionFiles() {
        boolean writeTheseOutputs = false;
        String oregonOutputsString = ResourceUtil.getProperty(piRb, "pi.oregonOutputs");
        if (oregonOutputsString != null) {
            if (oregonOutputsString.equalsIgnoreCase("true")) {
                writeTheseOutputs = true;
            }
        }
        if (!writeTheseOutputs) {
            logger.info("Not writing Oregon-Specific Outputs (labour consumption and production)");
            return;
        }
        //Need several things to pass to the LaborProductionAndConsumption method
        //in order for it to write the output files:
        //1.  a2b table
        //2.  activityLocations2 table
        //3.  String[] occupations
        //4.  String[] hhCategories
        //5.  String[] activities (not including the households)
        TableDataSet alphaToBeta = loadTableDataSet("alpha2beta","reference.data");
        
        TableDataSet householdQuantity = loadTableDataSet("ActivityLocations2","pi.current.data");
        
       
        // an Occupation class must be instantiated in order to get the PUMS/Occupation
        // correspondence file to be read to make the statewide Occupation categories known.
        String[] occupations =  indOccRef.getOccupationLabelsByIndex();
        List<String> tempList = new ArrayList<String>();
        for (String occupation : occupations){
            if(occupation.indexOf("Unemployed")>=0) continue;
            tempList.add(occupation);
        }
        occupations = new String[tempList.size()];
        tempList.toArray(occupations);
        
        
        String[] hhCategories = new IncomeSize().getIncomeSizeLabels();
        
        
        Set activities = indOccRef.getSplitIndustryLabels();
        
        
        LaborProductionAndConsumption labor = new LaborProductionAndConsumption(alphaToBeta,"AZone","BZone",householdQuantity,occupations,hhCategories,activities);
        labor.writeAllFiles(getOutputPath());
    }
    
public void writeFlowZipMatrices(String name, Writer histogramFile, PrintWriter pctFile) {
    
    //first check to see if county outputs are requested of either occupations or SCTGs
    if((ResourceUtil.getProperty(piRb, "pi.writeCountyOccupationFlows") != null 
            && ResourceUtil.getBooleanProperty(piRb, "pi.writeCountyOccupationFlows")) ||
       (ResourceUtil.getProperty(piRb, "pi.writeCountySCTGFlows") != null 
                    && ResourceUtil.getBooleanProperty(piRb, "pi.writeCountySCTGFlows"))){
        
        Commodity com = Commodity.retrieveCommodity(name);
        Matrix s = com.getSellingFlowMatrix();
        Matrix b = com.getBuyingFlowMatrix();
        
        //need to initialize the occupation variable if it hasn't been already.
        if(indOccRef == null){
            indOccRef = new IndustryOccupationSplitIndustryReference( ResourceUtil.getProperty(globalRb, "sw_ind_occ_split.correspondence.fileName"));
        }

        occupations = indOccRef.getOccupationLabelsByIndex();
        Arrays.sort(occupations);

        //Either occupation or SCTG county flows are requested so figure out if the
        //commodity name passed in is an occupation or an SCTG
        if( Arrays.binarySearch(occupations,com.name) >= 0){  //commodity is an occupation
            if(ResourceUtil.getBooleanProperty(piRb, "pi.writeCountyOccupationFlows")){
                //write out the selling county flows for the labor commodities
                if( Arrays.binarySearch(occupations,com.name) >= 0){
                    if(compressor == null){
                        String filePath = ResourceUtil.getProperty(globalRb,"alpha2beta.file");
                        File a2bFile = new File(filePath);
                        beta2CountyMap = new AlphaToBeta(a2bFile,"Bzone","FIPS");
                        compressor = new MatrixCompression(beta2CountyMap);
                    }
                
                    Matrix countySqueeze = compressor.getCompressedMatrix(s,"SUM");
                
                    File output = new File(getOutputPath() + "CountyFlows_Selling_"+ com.name+".csv");
                    MatrixWriter writer = new CSVMatrixWriter(output);
                    writer.writeMatrix(countySqueeze);
                }
            } 
        }else if(name.startsWith("SCTG")){
            if(ResourceUtil.getBooleanProperty(piRb, "pi.writeCountySCTGFlows")){
                if(compressor == null){
                  String filePath = ResourceUtil.getProperty(piRb,"reference.data") + "alpha2beta.csv";
                  File a2bFile = new File(filePath);
                  beta2CountyMap = new AlphaToBeta(a2bFile,"Bzone","FIPS");
                  compressor = new MatrixCompression(beta2CountyMap);
                }
                Matrix countySqueeze = compressor.getCompressedMatrix(b,"SUM");
              
                File output = new File(getOutputPath() + "CountyFlows_Value_"+ com.name+".csv");
                MatrixWriter writer = new CSVMatrixWriter(output);
                writer.writeMatrix(countySqueeze);
            }
        }
        //Write intrazonal numbers to calculate percentages
        writePctIntrazonalFile(pctFile,name,b,s);

        writeFlowHistograms(histogramFile,name,b,s);
        
        
    }else {
        super.writeFlowZipMatrices(name, histogramFile, pctFile);
    }
}
    
    

    /**
     * This method wrote out the county buying flows for the various commodities but as of
     * 9/6/06 we are wanting county selling flows of labor.  I am saving this method in case we 
     * want to go back to SCTG county flows.  We need to organize all of the outputs better.
     */

//    public void writeFlowZipMatrices(String name, Writer histogramFile, PrintWriter pctFile) {
//        Commodity com = Commodity.retrieveCommodity(name);
//        
//        ZipMatrixWriter  zmw = new ZipMatrixWriter(new File(getOutputPath()+"buying_"+com.name+".zipMatrix"));
//        Matrix b = com.getBuyingFlowMatrix();
//        zmw.writeMatrix(b);
//        
//        //write out the county flows for the SCTG commodities
//        if(name.startsWith("SCTG")){
//            if(compressor == null){
//                String filePath = ResourceUtil.getProperty(piRb,"reference.data") + "alpha2beta.csv";
//                File a2bFile = new File(filePath);
//                beta2CountyMap = new AlphaToBeta(a2bFile,"Bzone","FIPS");
//                compressor = new MatrixCompression(beta2CountyMap);
//            }
//            Matrix countySqueeze = compressor.getCompressedMatrix(b,"SUM");
//            
//            File output = new File(getOutputPath() + "CountyFlows_Value_"+ com.name+".csv");
//            MatrixWriter writer = new CSVMatrixWriter(output);
//            writer.writeMatrix(countySqueeze);
//            
//        }
//        
//        zmw = new ZipMatrixWriter(new File(getOutputPath()+"selling_"+com.name+".zipMatrix"));
//        Matrix s = com.getSellingFlowMatrix();
//        zmw.writeMatrix(s);
//        if(logger.isDebugEnabled()) logger.debug("Buying and Selling Commodity Flow Matrices have been written for " + name);
//        
//        //write intrazonal numbers to calculate percentages
//        writePctIntrazonalFile(pctFile,name,b,s);
//        
//        writeFlowHistograms(histogramFile, name,b,s);
//    }
    
    protected void writePctIntrazonalFile(PrintWriter writer,String name,Matrix b, Matrix s){
        boolean closePctFile = false;
        try {
          /* for daf version, we have to write out a file for each commodity so
           * we create a new file each time this routine is called, write to the
           * file and then close it.  In the monolithic version, we just write lines
           * to a single file as we iterate over the commodities and the file
           * will be closed by the calling method.
           */ 
            if (writer == null) { 
                writer = new PrintWriter(new BufferedWriter(new FileWriter(getOutputPath() + "PctIntrazonalxBetazone_"+name+".csv")));
                writer.println("Bzone,Commodity,BuyIntra,BuyFrom,BuyTo,BuyPctIntraFrom,BuyPctIntraTo,SellIntra,SellFrom,SellTo,SellPctIntraFrom,SellPctIntraTo");
                closePctFile = true;
            }   
            
            float buyIntra = 0;
            float buyFrom = 0;
            float buyTo = 0;
            float buyPctFrom = 0;
            float buyPctTo = 0;
            float sellIntra = 0;
            float sellFrom = 0;
            float sellTo = 0;
            float sellPctFrom = 0;
            float sellPctTo = 0;
                
            for(int i=0; i<b.getRowCount(); i++){
                int betaZone = b.getExternalNumber(i);
                buyIntra = b.getValueAt(betaZone,betaZone);
                buyFrom = b.getRowSum(betaZone);
                buyTo = b.getColumnSum(betaZone);
                buyPctFrom = buyIntra/buyFrom;
                buyPctTo = buyIntra/buyTo;
                sellIntra = s.getValueAt(betaZone,betaZone);
                sellFrom = s.getRowSum(betaZone);
                sellTo = s.getColumnSum(betaZone);
                sellPctFrom = sellIntra/sellFrom;
                sellPctTo = sellIntra/sellTo;
                writer.print(betaZone + ",");
                writer.print(name + ",");
                writer.print(buyIntra +","); //buyIntra
                writer.print(buyFrom +","); //buyFrom
                writer.print(buyTo + ","); //buyTo
                writer.print(buyPctFrom + ","); //buyPctFrom
                writer.print(buyPctTo + ","); //buyPctTo
                writer.print(sellIntra +","); //sellIntra
                writer.print(sellFrom +","); //sellFrom
                writer.print(sellTo + ","); //sellTo
                writer.print(sellPctFrom + ","); //sellPctFrom
                writer.println(sellPctTo); //sellPctTo
            }
            
            /* close the file if we are running the DAF version of pi
             * otherwise the file will be closed after we have iterated
             * thru all the commodities.
             */
            if (writer !=null && closePctFile == true) {
                writer.close();
            }
            
        } catch (Exception e) {
            logger.fatal("Error writing to file " + e);
            System.exit(1);
        }    
    }
    
    public static void main(String[] args) {
        ResourceBundle piRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_PleaseWork/t1/pi/pi.properties"));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_PleaseWork/t1/global.properties"));

        OregonPIPProcessor pProcessor =  new OregonPIPProcessor(1,piRb, globalRb);
        pProcessor.createFloorspaceWFile();
    }
    
}
