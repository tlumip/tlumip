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

import com.pb.common.datafile.*;
import com.pb.common.matrix.*;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pecas.*;
import com.pb.models.reference.IndustryOccupationSplitIndustryReference;
import com.pb.tlumip.model.IncomeSize;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;

import java.io.*;
import java.util.*;

/**
 * @author John Abraham
 *
 */
public class OregonPIPProcessor extends PIPProcessor {

    String year;
    IndustryOccupationSplitIndustryReference indOccRef; //initially null
    String[] occupations; //initially null
    MatrixCompression compressor = null;
    ArrayList<String> activities;
    String[] activitiesColumns;
    HashMap<String, Double> activitiesI;

    public OregonPIPProcessor() {
        super();

    }

    /**
     * @param timePeriod time perios
     * @param piRb pi resource bundle
     * @param globalRb global resource bundle
     */
    public OregonPIPProcessor(int timePeriod, ResourceBundle piRb, ResourceBundle globalRb) {

        super(timePeriod, piRb, globalRb);
//        indOccRef = new IndustryOccupationSplitIndustryReference( ResourceUtil.getProperty(globalRb, "industry.occupation.to.split.industry.correspondence"));
        indOccRef = new IndustryOccupationSplitIndustryReference(IndustryOccupationSplitIndustryReference.getSplitCorrespondenceFilepath(globalRb));
        this.year = timePeriod < 10 ? "1990" : "2000";

    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.pi.PIPProcessor#setUpPi()
     */
    public void setUpPi() {

        super.setUpPi();
    }

    public void doProjectSpecificInputProcessing() {
        //indOccRef = new IndustryOccupationSplitIndustryReference( ResourceUtil.getProperty(globalRb, "industry.occupation.to.split.industry.correspondence"));
        indOccRef = new IndustryOccupationSplitIndustryReference(IndustryOccupationSplitIndustryReference.getSplitCorrespondenceFilepath(globalRb));



        //create PECASZonesI and FloorspaceZonesI.csv file for PI to use
        createZoneFiles();

        //set up calibration params.
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

        if (doIntegratedModuleRun) {

            String currPath = ResourceUtil.getProperty(piRb,"pi.current.data");

            deleteOldFile(currPath, "ActivitiesW.csv");
            deleteOldFile(currPath, "FloorspaceW.csv");
            deleteOldFile(currPath, "ActivitiesZonalValuesW.csv");
            deleteOldFile(currPath, "MakeUseW.csv");

            createActivitiesWFile();
            createFloorspaceWFile();
            createActivitiesZonalValuesWFile();
            createMakeUseWFile();
        }
    }

    private void deleteOldFile(String currPath, String fileName) {
        File file = new File(currPath + fileName);
        if(file.exists()){
            file.delete();
            logger.info("Deleted old " + fileName + " to prepare for new file");
        }
    }

    private void createZoneFiles(){
        logger.info("Creating PECASZonesI and FloorspaceZonesI");
        TableDataSet a2b = loadTableDataSet("alpha2beta","reference.data");
        WorldZoneExternalZoneUtil wZEZUtil = new WorldZoneExternalZoneUtil(globalRb);
        int[] worldZones = wZEZUtil.getWorldZones();

        //First create FloorspaceZone file
        //get array of alpha internals and add the world zones to it.
        int[] internals = a2b.getColumnAsInt("Azone");
        int[] alphas = new int[internals.length + worldZones.length];
        System.arraycopy(internals,0,alphas,0,internals.length);
        System.arraycopy(worldZones,0, alphas,internals.length,worldZones.length);
        //now get the array of beta internals and add the world zones to it.
        int[] internalBetas = a2b.getColumnAsInt("Bzone");
        int[] betas = new int[internalBetas.length + worldZones.length];
        System.arraycopy(internalBetas,0,betas,0,internalBetas.length);
        System.arraycopy(worldZones,0, betas, internalBetas.length,worldZones.length);
        //finally get the FIPS code and add the world zones to the bottom of that
        //The FIPS is a concatenation of 2 columns from a2b.
        int[] stateFIPS = a2b.getColumnAsInt("STATEFIPS");
        int[] countyFIPS = a2b.getColumnAsInt("COUNTYFIPS");
        int[] fips = new int[stateFIPS.length + worldZones.length];
        for(int i=0; i<stateFIPS.length; i++){
            fips[i] = stateFIPS[i]*1000 + countyFIPS[i];
        }
        System.arraycopy(worldZones,0,fips,stateFIPS.length,worldZones.length);

        TableDataSet floorspaceZonesI = new TableDataSet();
        floorspaceZonesI.appendColumn(alphas, "AlphaZone");
        floorspaceZonesI.appendColumn(betas, "PecasZone");
        floorspaceZonesI.appendColumn(fips, "FIPS");

        //Now do the PECASZones file
        //The beta zone column is already done from above
        //but we also need the ZoneName column
        String[] internalNames = a2b.getColumnAsString("PECASName");
        String[] names = new String[internalNames.length + worldZones.length];
        System.arraycopy(internalNames, 0, names, 0, internalNames.length);
        for(int i=0; i<worldZones.length; i++){
            names[i+internalNames.length] = "Z" + worldZones[i] + "ImportExport";
        }

        //these arrays have repeats so we have to eliminate them
        //do that with a TreeMap which will sort and only store the
        //first of each pair
        TreeMap<Integer, String> map = new TreeMap<Integer,String>();
        for(int i=0; i< betas.length; i++){
            map.put(betas[i],names[i]);
        }
        //Now put the values back into an array
        betas = new int[map.size()];
        names = new String[map.size()];
        int index = 0;
        for(Integer zone : map.keySet()){
            betas[index] = zone;
            names[index] = map.get(zone);
            index++;
        }

        TableDataSet pecasZonesI = new TableDataSet();
        pecasZonesI.appendColumn(betas, "ZoneNumber");
        pecasZonesI.appendColumn(names, "ZoneName");

        // write the updated PI input file
        String referencePath = ResourceUtil.getProperty(piRb, "reference.data");
        CSVFileWriter writer = new CSVFileWriter();
        try {
            writer.writeFile(floorspaceZonesI, new File(referencePath + "FloorspaceZonesI.csv"),0,new GeneralDecimalFormat("0.#####E0",100000,.01));
            writer.writeFile(pecasZonesI, new File(referencePath + "PECASZonesI.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createActivitiesWFileOLD(){
        logger.info("Creating new ActivitiesW.csv using current ED and SPG data");
        boolean readSpgHHFile = (ResourceUtil.getProperty(piRb, "pi.readHouseholdsByHHCategory").equalsIgnoreCase("true"));
        boolean readEDDollarFile = (ResourceUtil.getProperty(piRb, "pi.readActivityDollarDataForPI").equalsIgnoreCase("true"));
        boolean updateImportsAndExports = ResourceUtil.getBooleanProperty(piRb, "pi.updateImportsAndExports", true);
        if(readSpgHHFile || readEDDollarFile || updateImportsAndExports) {

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
            int[] householdsByIncomeSize = null;                         //needed to update imports and exports.
            if (readSpgHHFile || updateImportsAndExports) {
                String hhPath = ResourceUtil.getProperty(piRb, "spg.input.data");
                //read the SPG input file and put the data into an array by hhIndex
                TableDataSet hh = null;
                try {
                    hh = reader.readFile(new File(hhPath + "householdsByHHCategory.csv"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                IncomeSize inc = new IncomeSize(piRb);
                householdsByIncomeSize = new int[hh.getRowCount()];
                for (int r = 0; r < hh.getRowCount(); r++) {
                    int incomeSize = inc.getIncomeSizeIndex(hh.getStringValueAt(r + 1, 1)); //HHCategory
                    householdsByIncomeSize[incomeSize] = (int) hh.getValueAt(r + 1, 2); //Size
                }

                if (readSpgHHFile) {
                    // update the values in the actI TableDataSet using the SPG1 results
                    for (int r = 0; r < actI.getRowCount(); r++) {
                        int incomeSize = inc.getIncomeSizeIndex(actI.getStringValueAt(r + 1, activityColumnPosition));
                        if (incomeSize >= 0) {
                            actI.setValueAt(r + 1, sizeColumnPosition, householdsByIncomeSize[incomeSize]);
                        }
                    }
                }
            }

            //The ED File has 2 columns, Activity and Dollar Amounts
            HashMap<String, Float> dollarsByIndustry = new HashMap<String, Float>(); //needed to updateImportsAndExports

            if(readEDDollarFile || updateImportsAndExports){
                String dollarPath = ResourceUtil.getProperty(piRb,"ed.input.data");
                String filePath = dollarPath + "ActivityDollarDataForPI.csv";
                //read the ED input file and put the data into a hashmap by industry
                TableDataSet dollars = null;
                try {
                    dollars = reader.readFile(new File(filePath));

                } catch (IOException e) {
                    throw new RuntimeException("Can't find ActivityDollarDataForPI.csv file at " + filePath);
                }

                //header row does not count in row count
                for(int r = 0; r < dollars.getRowCount(); r++){
                    String splitIndustryLabel = dollars.getStringValueAt(r + 1, 1);//Activity Name
                    dollarsByIndustry.put(splitIndustryLabel, dollars.getValueAt(r + 1, "Factor")); //Total Dollars
                }

                if (readEDDollarFile) {
                    //update the values in the actI TableDataSet using the ED results
                    for(int r=0; r< actI.getRowCount(); r++){
                        String key = actI.getStringValueAt(r+1,activityColumnPosition);
                        if (dollarsByIndustry.containsKey(key)){
                            actI.setValueAt(r+1,sizeColumnPosition, dollarsByIndustry.get(key));
                        }
                    }
                }
            }

            if(updateImportsAndExports){
                TableDataSet importShareByComm = loadTableDataSet("ImportShareByCommodity", "pi.base.data");
                TableDataSet makeUse = loadTableDataSet("MakeUseI","pi.base.data");
                HashMap<String, Float> importExportSize = new HashMap<String, Float>();
                IncomeSize inc = new IncomeSize(piRb);

                //Get the list of commodities that will be processed.
                String commodity;
                float importShare;
                float imports;
                float exports;
                //for each commodity
                for(int row = 1; row <= importShareByComm.getRowCount(); row++){
                    importShare = importShareByComm.getValueAt(row, "98ImportShareOfModelwideUse");
                    commodity = importShareByComm.getStringValueAt(row, "SCTG");

                    //go thru the MakeUseI file and figure out the total amount
                    //of the commodity that is made and used.
                    String makeUseCommodity;
                    String industry;
                    float[] makeUseAmts = new float[2]; //0-make, 1=use
                    for(int rowInMUFile = 1; rowInMUFile <= makeUse.getRowCount(); rowInMUFile++){
                        makeUseCommodity = makeUse.getStringValueAt(rowInMUFile, "Commodity");
                        if(makeUseCommodity.equals(commodity)){
                            //figure out if it is make or use and what industry is involved.
                            int index = makeUse.getStringValueAt(rowInMUFile, "MorU").equals("U")?1:0;
                            industry = makeUse.getStringValueAt(rowInMUFile, "Activity");
                            if(indOccRef.isSplitIndustryLabelValid(industry) || industry.equals("Capitalists") || industry.equals("GovInstitutions")){
                                makeUseAmts[index] += dollarsByIndustry.get(industry) * makeUse.getValueAt(rowInMUFile, "Minimum");
                            }else if(industry.contains("HH")){
                                makeUseAmts[index] += householdsByIncomeSize[inc.getIncomeSizeIndex(industry)]*makeUse.getValueAt(rowInMUFile, "Minimum");
                            }
                        } //else go to the next line in the MakeUseI file.
                    } //next row in MakeUseI
                    imports = makeUseAmts[1] * importShare;
                    exports = makeUseAmts[0] - makeUseAmts[1] + imports;
                    System.out.println("Commodity: " + commodity + " Import Share: " + importShare + " Use: " + makeUseAmts[1] + " Make: " + makeUseAmts[0]);
                    importExportSize.put((commodity + " Importers"), imports);
                    importExportSize.put((commodity + " Exporters"), exports);
                } //next commodity

                for(int r=0; r< actI.getRowCount(); r++){
                    String key = actI.getStringValueAt(r+1,activityColumnPosition);
                    if (importExportSize.containsKey(key)){
                        actI.setValueAt(r+1,sizeColumnPosition, importExportSize.get(key));
                    }
                }
            }  //done updating the imports and exports


            // write the updated PI input file
            String piOutputsPath = ResourceUtil.getProperty(piRb, "output.data");
            CSVFileWriter writer = new CSVFileWriter();
            writer.setMyDecimalFormat(new GeneralDecimalFormat("0.#########E0",10000000,.001));

            try {
                writer.writeFile(actI, new File(piOutputsPath + "ActivitiesWOld.csv"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }//otherwise just use the ActivitiesI.csv file
    }

    private void createActivitiesWFile(){
        logger.info("Creating new ActivitiesW.csv using current ED and SPG data");
        boolean readSpgHHFile = (ResourceUtil.getProperty(piRb, "pi.readHouseholdsByHHCategory").equalsIgnoreCase("true"));
        boolean readEDDollarFile = (ResourceUtil.getProperty(piRb, "pi.readActivityDollarDataForPI").equalsIgnoreCase("true"));
        boolean updateImportsAndExports = ResourceUtil.getBooleanProperty(piRb, "pi.updateImportsAndExports", true);
        if(readSpgHHFile || readEDDollarFile || updateImportsAndExports) {

            CSVFileReader reader = new CSVFileReader();
            // read the PI intput file into a TableDataSet
            String piInputsPath = ResourceUtil.getProperty(piRb, "pi.base.data");

            activitiesI = new HashMap<String, Double>();

            try {
                activitiesI = readActivitiesIFile(piInputsPath + "ActivitiesI.csv");

            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            // the SPG1 File has HHCategory as rows and Size as columns
            int[] householdsByIncomeSize = null;                         //needed to update imports and exports.
            if (readSpgHHFile || updateImportsAndExports) {
                String hhPath = ResourceUtil.getProperty(piRb, "spg.input.data");
                //read the SPG input file and put the data into an array by hhIndex
                TableDataSet hh = null;
                try {
                    hh = reader.readFile(new File(hhPath + "householdsByHHCategory.csv"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                IncomeSize inc = new IncomeSize(piRb);
                householdsByIncomeSize = new int[hh.getRowCount()];
                for (int r = 0; r < hh.getRowCount(); r++) {
                    int incomeSize = inc.getIncomeSizeIndex(hh.getStringValueAt(r + 1, 1)); //HHCategory
                    householdsByIncomeSize[incomeSize] = (int) hh.getValueAt(r + 1, 2); //Size
                }

                if (readSpgHHFile) {
                    // update the values in the actI TableDataSet using the SPG1 results
                    for (String activity: activities) {
                        int incomeSize = inc.getIncomeSizeIndex(activity);
                        if (incomeSize >= 0) {
                            activitiesI.put(activity + "_" + "Size", Double.parseDouble(Integer.toString(householdsByIncomeSize[incomeSize])));
                        }
                    }
                }
            }

            //The ED File has 2 columns, Activity and Dollar Amounts
            HashMap<String, Double> dollarsByIndustry = new HashMap<String, Double>(); //needed to updateImportsAndExports
            if(readEDDollarFile || updateImportsAndExports){
                String dollarPath = ResourceUtil.getProperty(piRb,"ed.input.data");
                //read the ED input file and put the data into a hashmap by industry

                try {
                    dollarsByIndustry = readActivityDollarDataFile(dollarPath + "ActivityDollarDataForPI.csv");
                } catch (IOException e) {
                    throw new RuntimeException("Can't find ActivityDollarDataForPI.csv file");
                }


                if (readEDDollarFile) {
                    //update the values in the actI TableDataSet using the ED results
                    for (String activity: activities) {
                        if (dollarsByIndustry.containsKey(activity)){
                            activitiesI.put(activity + "_" + "Size", dollarsByIndustry.get(activity));
                        }
                    }
                }
            }

            if(updateImportsAndExports){
                TableDataSet importShareByComm = loadTableDataSet("ImportShareByCommodity", "pi.base.data");
                TableDataSet makeUse = loadTableDataSet("MakeUseI","pi.base.data");
                HashMap<String, Float> importExportSize = new HashMap<String, Float>();
                IncomeSize inc = new IncomeSize(piRb);

                //Get the list of commodities that will be processed.
                String commodity;
                float importShare;
                float imports;
                float exports;
                //for each commodity
                for(int row = 1; row <= importShareByComm.getRowCount(); row++){
                    importShare = importShareByComm.getValueAt(row, "98ImportShareOfModelwideUse");
                    commodity = importShareByComm.getStringValueAt(row, "SCTG");

                    //go thru the MakeUseI file and figure out the total amount
                    //of the commodity that is made and used.
                    String makeUseCommodity;
                    String industry;
                    float[] makeUseAmts = new float[2]; //0-make, 1=use
                    for(int rowInMUFile = 1; rowInMUFile <= makeUse.getRowCount(); rowInMUFile++){
                        makeUseCommodity = makeUse.getStringValueAt(rowInMUFile, "Commodity");
                        if(makeUseCommodity.equals(commodity)){
                            //figure out if it is make or use and what industry is involved.
                            int index = makeUse.getStringValueAt(rowInMUFile, "MorU").equals("U")?1:0;
                            industry = makeUse.getStringValueAt(rowInMUFile, "Activity");
                            if(indOccRef.isSplitIndustryLabelValid(industry) || industry.equals("Capitalists") || industry.equals("GovInstitutions")){
                                makeUseAmts[index] += dollarsByIndustry.get(industry) * makeUse.getValueAt(rowInMUFile, "Minimum");
                            }else if(industry.contains("HH")){
                                makeUseAmts[index] += householdsByIncomeSize[inc.getIncomeSizeIndex(industry)]*makeUse.getValueAt(rowInMUFile, "Minimum");
                            }
                        } //else go to the next line in the MakeUseI file.
                    } //next row in MakeUseI
                    imports = makeUseAmts[1] * importShare;
                    exports = makeUseAmts[0] - makeUseAmts[1] + imports;
                    System.out.println("Commodity: " + commodity + " Import Share: " + importShare + " Use: " + makeUseAmts[1] + " Make: " + makeUseAmts[0]);
                    importExportSize.put((commodity + " Importers"), imports);
                    importExportSize.put((commodity + " Exporters"), exports);
                } //next commodity

                for (String activity: activities) {
                    if (importExportSize.containsKey(activity)){
                        activitiesI.put(activity + "_" + "Size", Double.parseDouble(Float.toString(importExportSize.get(activity))));

                    }
                }
            }  //done updating the imports and exports
            writeActivitiesW();


        }//otherwise just use the ActivitiesI.csv file
    }

    private void writeActivitiesW() {
        // write the updated PI input file
        String piOutputsPath = ResourceUtil.getProperty(piRb, "output.data");
        String filePath = piOutputsPath + "ActivitiesW.csv";

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filePath).getAbsolutePath()));

            logger.info("Writing ActivitiesW file: " + filePath);
            String headerLine = "";
            for (String header: activitiesColumns) {
                headerLine += header + ",";
            }
            //remove final comma
            headerLine = headerLine.substring(0, headerLine.length()-1);

            bw.write(headerLine);
            bw.newLine();
            for (String activity: activities) {
                String currentLine = activity + ",";
                for (String header: activitiesColumns) {
                    if (header.equals("Activity")) {
                        continue;
                    }
                    if (header.equals("NonModelledProduction") || (header.equals("NonModelledConsumption"))) {
                        double value = activitiesI.get(activity + "_" + header);
                        if (value == 1.0) {
                            currentLine += "TRUE,";
                        } else {
                            currentLine += "FALSE,";
                        }
                    } else {
                        currentLine += activitiesI.get(activity + "_" + header) + ",";
                    }
                }
                //remove final comma
                currentLine = currentLine.substring(0, currentLine.length()-1);
                bw.write(currentLine);
                bw.newLine();
            }

            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private HashMap<String, Double> readActivitiesIFile(String filePath) throws IOException {
        HashMap<String, Double> activitiesI = new HashMap<String, Double>();
        activities = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
            String s;
            StringTokenizer st;
            s = br.readLine();
            //st = new StringTokenizer(s, ", ");
            //activitiesColumns = new ArrayList<String>();
            activitiesColumns = s.split(",");

            /*String[] headers = new String[14];
            for (int i = 0; i < headers.length; i++) {
                headers[i] = st.nextToken();
            }*/
            while ((s = br.readLine()) != null) {
                st = new StringTokenizer(s, ",");
                String activity = st.nextToken();
                if (activity.startsWith("\""))
                    activity = activity.substring(1, activity.length()-1);
                activities.add(activity);
                activitiesI.put(activity + "_" + activitiesColumns[1], Double.parseDouble(st.nextToken())); //locationDispersionParameter
                activitiesI.put(activity + "_" + activitiesColumns[2], Double.parseDouble(st.nextToken()));  //SizeTermCoefficient
                activitiesI.put(activity + "_" + activitiesColumns[3], Double.parseDouble(st.nextToken()));  //productionUtilityScaling
                activitiesI.put(activity + "_" + activitiesColumns[4], Double.parseDouble(st.nextToken()));   //productionSubstitutionNesting
                activitiesI.put(activity + "_" + activitiesColumns[5], Double.parseDouble(st.nextToken()));//consumptionUtilityScaling
                activitiesI.put(activity + "_" + activitiesColumns[6], Double.parseDouble(st.nextToken())); //consumptionSubstitutionNesting
                activitiesI.put(activity + "_" + activitiesColumns[7], Double.parseDouble(st.nextToken()));//size
                activitiesI.put(activity + "_" + activitiesColumns[8], Double.parseDouble(st.nextToken())); //inertiaTermCoefficient
                activitiesI.put(activity + "_" + activitiesColumns[9], Double.parseDouble(st.nextToken()));//inertiaTermConstant
                Boolean nonModelledConsumption = Boolean.parseBoolean(st.nextToken());  //nonModelledConsumption
                if (nonModelledConsumption) {
                    activitiesI.put(activity + "_" + activitiesColumns[10], 1d);
                } else {
                    activitiesI.put(activity + "_" + activitiesColumns[10], 0d);
                }
                activitiesI.put(activity + "_" + activitiesColumns[11], Double.parseDouble(st.nextToken()));//utilityOfNonModelledConsumption
                Boolean nonModelledProduction = Boolean.parseBoolean(st.nextToken());//nonModelledProduction
                if (nonModelledProduction) {
                    activitiesI.put(activity + "_" + activitiesColumns[12], 1d);
                } else {
                    activitiesI.put(activity + "_" + activitiesColumns[12], 0d);
                }
                activitiesI.put(activity + "_" + activitiesColumns[13], Double.parseDouble(st.nextToken()));//utilityOfNonModelledProduction

            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return activitiesI;
    }

    private HashMap<String, Double> readActivityDollarDataFile(String filePath) throws IOException {
        HashMap<String, Double> activityDollarData = new HashMap<String, Double>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
            String s;
            StringTokenizer st;
            //This is the header line
            br.readLine();
            while ((s = br.readLine()) != null) {
                if (s.startsWith("#")) continue;    // skip comment records
                st = new StringTokenizer(s, ",");
                String activity = st.nextToken();
                if (activity.startsWith("\""))
                    activity = activity.substring(1, activity.length()-1);
                activityDollarData.put(activity,   // activity
                        Double.parseDouble(st.nextToken()) );  // factor
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return activityDollarData;
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
        floorspaceInventory = new Hashtable<String, ZoneQuantityStorage>();
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
        TableDataSet alpha2betaTable = loadTableDataSet("FloorspaceZonesI","reference.data");
        AlphaToBeta a2bMap = new AlphaToBeta(alpha2betaTable, "AlphaZone", "PecasZone");

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
            int[] aZones = a2bMap.getAlphaExternals1Based();
            for(int i=1; i< aZones.length; i++){
                if(nAddsByZone[aZones[i]] != nFlrNames){
                    logger.error("\t\t\t\tZone " + a2bMap.getAlphaExternals1Based()[i] + " added up " + nAddsByZone[a2bMap.getAlphaExternals1Based()[i]]);
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
                    !dollars.getStringValueAt(row, 1).equalsIgnoreCase("GovInstitutions")){
                totalDollarsProduction += dollars.getValueAt(row, 2);
            }
        }
        double totalDollarsProductionInMillions = totalDollarsProduction/1000000.0;
        logger.debug("TotalDollarsProductionInMillions: " + totalDollarsProductionInMillions);

        //Next read in the job data for spg1 that ED produced and add up the total
        //number of jobs
        TableDataSet jobs = loadTableDataSet("JobDataForSPG1", "ed.input.data");
        double totalJobs = 0.0;
        for(int row=1; row <= jobs.getRowCount(); row++){
            totalJobs += jobs.getValueAt(row, 2);
        }
        logger.debug("Total jobs: " + totalJobs);

        //Calculate Jobs/Dollars ratio
        double dollarsToJobs = totalDollarsProductionInMillions / totalJobs;
        logger.debug("DollarsToJobs: " + dollarsToJobs);

        //Read the 98 ratio from the properties file
        double dollarsToJobsTo98 = ResourceUtil.getDoubleProperty(piRb, "pi.98.productivity.rate", 0.094129);
        logger.debug("DollarsToJobs-98: " +  dollarsToJobsTo98);

        //Calculate the LaborUseScalingFactor
        double laborUseScalor = dollarsToJobsTo98 / dollarsToJobs;
        logger.debug("LaborUseScalingFactor: " + laborUseScalor);

//        if(timePeriod == 8 && Math.abs(1-laborUseScalor) > .1){
//            logger.warn("WARNING: Expected LaborUseScalingFactor is 1, Actual value is " + laborUseScalor);
//        }


        if(timePeriod == 8 && laborUseScalor != 1.0d){
            logger.warn("WARNING: Expected LaborUseScalingFactor is 1, Actual value is " + laborUseScalor);
        } else {
            logger.info("LaborUseScalingFactor is: " + laborUseScalor);
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
        writer.setMyDecimalFormat(new GeneralDecimalFormat("0.#########E0",10000000,.001));
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
        boolean writeOregonOutputs = ResourceUtil.getBooleanProperty(piRb, "pi.oregonOutputs", false);

        if (!writeOregonOutputs) {
            logger.info("Not writing Oregon-Specific Outputs (labour consumption and production)");
            return;
        }
        logger.info("Writing LaborDollarProduction and Consumption file");
        //Need several things to pass to the LaborProductionAndConsumption method
        //in order for it to write the output files:
        //1.  a2b table
        //2.  activityLocations2 table
        //3.  String[] occupations
        //4.  String[] hhCategories
        //5.  String[] activities (not including the households)


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


        String[] hhCategories = new IncomeSize(piRb).getIncomeSizeLabels();


        Set activities = indOccRef.getSplitIndustryLabels();

//        TableDataSet alphaToBeta = loadTableDataSet("alpha2beta","reference.data");
//        LaborProductionAndConsumption labor = new LaborProductionAndConsumption(alphaToBeta,"AZone","BZone",householdQuantity,occupations,hhCategories,activities);

        TableDataSet alphaToBeta = loadTableDataSet("FloorspaceZonesI","reference.data");
        LaborProductionAndConsumption labor = new LaborProductionAndConsumption(alphaToBeta,"AlphaZone","PecasZone",householdQuantity,occupations,hhCategories,activities);
        labor.writeAllFiles(getOutputPath());
    }

    public void writeAllFlowRelatedFiles() {
        //Set up the Histogram Specifications.
        readInHistogramSpecifications();

        //Next get some properties from the properties file that will determine which flow matrices
        //to write out.  FLR commodities will never be written out.  If flows are written out, then
        //the PctIntrazonal.csv file will also be written to for that commodity.
        boolean writeBuyingGoodFlows = ResourceUtil.getBooleanProperty(piRb, "pi.write.buying.good.flows", true);
        boolean writeSellingGoodFlows = ResourceUtil.getBooleanProperty(piRb, "pi.write.selling.good.flows", false);

        boolean writeBuyingServiceFlows = ResourceUtil.getBooleanProperty(piRb, "pi.write.buying.service.flows", false);
        boolean writeSellingServiceFlows = ResourceUtil.getBooleanProperty(piRb, "pi.write.selling.service.flows", false);

        boolean writeBuyingLaborFlows = ResourceUtil.getBooleanProperty(piRb, "pi.write.buying.labor.flows", false);
        boolean writeSellingLaborFlows = ResourceUtil.getBooleanProperty(piRb, "pi.write.selling.labor.flows", true);

        //Regardless of whether we write out the flows, we will always write out a histogram file that looks
        //only at internal zones for all commodities except FLR and a histogram file that looks at all zones
        //for the goods commodities.
        //Set up the files
        try {
            logger.info("Creating Histograms_InternalZones.csv and Histograms_AllZones.csv");
            BufferedWriter internalHistogramFile = new BufferedWriter(new FileWriter(getOutputPath() + "Histograms_InternalZones.csv"));
            internalHistogramFile.write("Commodity,BuyingSelling,BandNumber,LowerBound,Quantity,AverageLength\n");

            BufferedWriter allHistogramFile = new BufferedWriter(new FileWriter(getOutputPath() + "Histograms_AllZones.csv"));
            allHistogramFile.write("Commodity,BuyingSelling,BandNumber,LowerBound,Quantity,AverageLength\n");

            //if any of the flows are written out then we will write out the PctIntrazonal file for those flows
            //as well.
            PrintWriter pctFile = null;
            if ((writeBuyingGoodFlows && writeSellingGoodFlows) || (writeBuyingServiceFlows && writeSellingServiceFlows)
                    || (writeBuyingLaborFlows && writeSellingLaborFlows)) {
                pctFile = new PrintWriter(new BufferedWriter(new FileWriter(getOutputPath() + "PctIntrazonalxCommodityxBzone.csv")));
                pctFile.println("Bzone,Commodity,BuyIntra,BuyFrom,BuyTo,BuyPctIntraFrom,BuyPctIntraTo,SellIntra,SellFrom,SellTo,SellPctIntraFrom,SellPctIntraTo");
            }

            Commodity commodity;
            Matrix buyAllZns = null;
            Matrix sellAllZns = null;
            Matrix buyInternalZns;
            Matrix sellInternalZns;
            for (AbstractCommodity abstComm : Commodity.getAllCommodities()) {
                commodity = (Commodity) abstComm;
                if(commodity.name.contains("FLR")) continue;       //skip over the floorspace commodities
                if(commodity.name.equalsIgnoreCase("Import")) continue;
                if(commodity.name.equalsIgnoreCase("Export")) continue;

                //We want internal and all zone histograms and only all zone flows (if requested)
                if(commodity.name.contains("SCTG") || commodity.name.contains("GOODS")){
                    buyAllZns = commodity.getBuyingFlowMatrix();
                    sellAllZns = commodity.getSellingFlowMatrix();
                    writeFlowHistograms(allHistogramFile, commodity.name, buyAllZns, sellAllZns);

                    if(writeBuyingGoodFlows) writeFlowZipMatrix(commodity.name, buyAllZns, "buying");
                    if(writeSellingGoodFlows) writeFlowZipMatrix(commodity.name, sellAllZns, "selling");
                    if(writeBuyingGoodFlows && writeSellingGoodFlows){
                        writePctIntrazonalFile(pctFile, commodity.name, buyAllZns, sellAllZns);
                    }

                    //Now do the internal stuff
                    buyInternalZns = buyAllZns.getSubMatrix(internalBetaZones);
                    sellInternalZns = sellAllZns.getSubMatrix(internalBetaZones);
                    writeFlowHistograms(internalHistogramFile, commodity.name, buyInternalZns, sellInternalZns);

                    boolean writeCountyLabor = ResourceUtil.getBooleanProperty(piRb, "pi.writeCountyOccupationFlows", false);
                    if(writeCountyLabor) writeCountyOutputs(commodity.name, buyInternalZns, null, "goods");

                } else {
                    try {
                        //For labor commodities we want internal histograms only and internal flows
                        //only (if requested)
                        Integer.parseInt(commodity.name.substring(0,1));
                        //if the above line executed correctly than the commodity is a TLUMIP labor
                        //category, if not it will be caught in the exception and treated as a service
                        buyInternalZns = commodity.getBuyingFlowMatrix().getSubMatrix(internalBetaZones);
                        sellInternalZns = commodity.getSellingFlowMatrix().getSubMatrix(internalBetaZones);
                        writeFlowHistograms(internalHistogramFile, commodity.name, buyInternalZns, sellInternalZns);

                        if(writeBuyingLaborFlows) writeFlowZipMatrix(commodity.name, buyInternalZns, "buying");
                        if(writeSellingLaborFlows) writeFlowZipMatrix(commodity.name, sellInternalZns, "selling");
                        if(writeBuyingLaborFlows && writeSellingLaborFlows)
                            writePctIntrazonalFile(pctFile, commodity.name, buyInternalZns, sellInternalZns);

                        boolean writeCountyLabor = ResourceUtil.getBooleanProperty(piRb, "pi.writeCountyOccupationFlows", false);
                        if(writeCountyLabor) writeCountyOutputs(commodity.name, null, sellInternalZns, "labor");


                    } catch (NumberFormatException e) {  //implies that the commodity is a service
                        //In the case of service commodities, we want internal histograms only but all zone
                        //flows (if requested)
                        if(writeBuyingServiceFlows) {
                            buyAllZns = commodity.getBuyingFlowMatrix();
                            writeFlowZipMatrix(commodity.name, buyAllZns, "buying");
                        }
                        if(writeSellingServiceFlows) {
                            sellAllZns = commodity.getSellingFlowMatrix();
                            writeFlowZipMatrix(commodity.name, sellAllZns, "selling");
                        }
                        if(writeBuyingServiceFlows && writeSellingServiceFlows)
                            writePctIntrazonalFile(pctFile, commodity.name, buyAllZns, sellAllZns);

                        buyInternalZns = commodity.getBuyingFlowMatrix().getSubMatrix(internalBetaZones);
                        sellInternalZns = commodity.getSellingFlowMatrix().getSubMatrix(internalBetaZones);
                        writeFlowHistograms(internalHistogramFile, commodity.name, buyInternalZns, sellInternalZns);

                    }
                }

            }
            internalHistogramFile.close();
            allHistogramFile.close();
            if(pctFile != null) pctFile.close();
            logger.info("\tHistograms_InternalZones.csv and Histograms_AllZones.csv have been written");
            logger.info("\tFlow matrices have been written by request - see pi.properties file");
        } catch (IOException e) {
            logger.fatal("Problems writing flow related files (histograms, flow matrices and pctIntrazonal) "+e);
            e.printStackTrace();
        }
    }

    public void writeCountyOutputs(String name, Matrix buy, Matrix sell, String goodsOrLabor) {

        if(compressor == null){
            String a2bFilePath = ResourceUtil.getProperty(globalRb,"alpha2beta.file");
            AlphaToBeta beta2CountyMap = new AlphaToBeta(new File(a2bFilePath), "Bzone", "FIPS");
            compressor = new MatrixCompression(beta2CountyMap);
        }

        if(goodsOrLabor.equals("labor")){
            Matrix countySqueeze = compressor.getCompressedMatrix(sell,"SUM");
            File output = new File(getOutputPath() + "CountyFlows_Selling_"+ name+".csv");
            MatrixWriter writer = new CSVMatrixWriter(output);
            writer.writeMatrix(countySqueeze);

        } else {
            Matrix countySqueeze = compressor.getCompressedMatrix(buy,"SUM");
            File output = new File(getOutputPath() + "CountyFlows_Value_"+ name+".csv");
            MatrixWriter writer = new CSVMatrixWriter(output);
            writer.writeMatrix(countySqueeze);
        }
    }

    /**
     * This method wrote out the county buying flows for the various commodities but as of
     * 9/6/06 we are wanting county selling flows of labor.  I am saving this method in case we 
     * want to go back to SCTG county flows.  We need to organize all of the outputs better.
     */

//    public void writeFlowsPercentIntrazonalsAndHistogramFiles(String name, Writer histogramFile, PrintWriter pctFile) {
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


    public static void main(String[] args) {
        ResourceBundle piRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_seam/t0/pi/pi.properties"));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_seam/t0/global.properties"));

        OregonPIPProcessor pProcessor =  new OregonPIPProcessor(1,piRb, globalRb);
        pProcessor.createActivitiesWFile();
        pProcessor.createActivitiesWFileOLD();
        //pProcessor.createZoneFiles();
    }

}
