package com.pb.despair.pi;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.ResourceBundle;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.datafile.TableDataSetIndex;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.IncomeSize;
import com.pb.despair.model.Industry;
import com.pb.despair.model.LaborProductionAndConsumption;
import com.pb.despair.model.SomeSkims;
import com.pb.despair.model.TransportKnowledge;

/**
 * @author John Abraham
 *
 */
public class OregonPIPProcessor extends PIPProcessor {

    public OregonPIPProcessor() {
        super();
    }

    /**
     * @param timePeriod
     */
    public OregonPIPProcessor(int timePeriod, ResourceBundle rb) {
        super(timePeriod, rb);
    }

    /* (non-Javadoc)
     * @see com.pb.despair.pi.PIPProcessor#setUpPi()
     */
    public void setUpPi() {
        String currPath = ResourceUtil.getProperty(rb,"pi.current.data");
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

        String oregonInputsString = ResourceUtil.getProperty(rb, "pi.oregonInputs");
        if (oregonInputsString != null ) {
            if (oregonInputsString.equalsIgnoreCase("true")) {
                doOregonSpecificInputProcessing();
             }
        }
        String calibrationMetaParameters = ResourceUtil.getProperty(rb,"pi.readMetaParameters");
        if (calibrationMetaParameters.equalsIgnoreCase("true")) {
            setUpMetaParameters();
        }
        super.setUpPi();
    }

    private void doOregonSpecificInputProcessing() {
        if(loadTableDataSet("ActivitesW","pi.current.data") == null){
            createActivitiesWFile();
        }
        if(loadTableDataSet("FloorspaceW","pi.current.data") == null){
            createFloorspaceWFile();
        }
    }

    private void createActivitiesWFile(){
        logger.info("Creating new ActivitiesW.csv using current ED and ALD data");
        boolean readSpgHHFile = (ResourceUtil.getProperty(rb, "pi.readHouseholdsByHHCategory").equalsIgnoreCase("true"));
        boolean readEDDollarFile = (ResourceUtil.getProperty(rb, "pi.readActivityDollarDataForPI").equalsIgnoreCase("true"));
        if(readSpgHHFile || readEDDollarFile)
        {
            CSVFileReader reader = new CSVFileReader();
            // read the PI intput file into a TableDataSet
            String piInputsPath = ResourceUtil.getProperty(rb, "pi.base.data");
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
                String hhPath = ResourceUtil.getProperty(rb, "spg.input.data");
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
                String dollarPath = ResourceUtil.getProperty(rb,"ed.input.data");
                //read the ED input file and put the data into an array by Industry Index
                TableDataSet dollars = null;
                try {
                    dollars = reader.readFile(new File(dollarPath + "ActivityDollarDataForPI.csv"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Industry industry = new Industry();
                float[] dollarsByIndustry = new float[dollars.getRowCount()]; //header row does not count in row count
                logger.fine("column count: "+ dollars.getColumnCount());
                for(int r = 0; r < dollars.getRowCount(); r++){
                    int industryIndex = industry.getIndustryIndex(dollars.getStringValueAt(r + 1, 1));//Activity Name
                    dollarsByIndustry[industryIndex] = dollars.getValueAt(r + 1, 2); //Total Dollars
                }
                //update the values in the actI TableDataSet using the ED results
                for(int r=0; r< actI.getRowCount(); r++){
                    int industryIndex = industry.getIndustryIndex(actI.getStringValueAt(r+1,activityColumnPosition));
                    if (industryIndex >= 0){
                        actI.setValueAt(r+1,sizeColumnPosition, dollarsByIndustry[industryIndex]);
                    }
                }
            }
            // write the updated PI input file
            String piOutputsPath = ResourceUtil.getProperty(rb, "output.data");
            CSVFileWriter writer = new CSVFileWriter();
            try {
                writer.writeFile(actI, new File(piOutputsPath + "ActivitiesW.csv"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }//otherwise just use the ActivitiesI.csv file
        return;
    }

    /* This method will read in the FloorspaceI.csv file that was output by ALD
    * and replace the aglog data with the PIAgForestFloorspace.csv base year data.  This will
    * happen every year.
    */

    public void createFloorspaceWFile() {
        //Read in the FloorspaceI.csv file that was produced by ALD
        TableDataSet floorspaceTable = loadTableDataSet("FloorspaceI","ald.input.data");
        //And the PIAgForestFloorspace.csv file that was created by Tara
        TableDataSet piAgForTable = loadTableDataSet("PIAgForestFloorspace", "pi.base.data");
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
                logger.severe("Bad floor type name in PIAgForestFloorspace file");
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
        logger.info("Replaced " + replaceCount + " values in the Floorspace Table");
        //Now write out the FloorspaceW.csv file
        String piOutputsPath = ResourceUtil.getProperty(rb, "output.data");
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
            FloorspaceQuantityStorage fi = (FloorspaceQuantityStorage) floorspaceInventory.get(commodityName);
            if (fi==null) {
                fi = new FloorspaceQuantityStorage(commodityName,maxAlphaZone+1);
                floorspaceInventory.put(commodityName,fi);
            }
            fi.inventory[alphaZone]+= quantity;
        }
    }

    /**
     * Read in some parameter functions, and adjust the parameters as appropriate
     */
    private void setUpMetaParameters() {
        CSVFileReader reader = new CSVFileReader();
        CSVFileWriter writer = new CSVFileWriter();
        String piInputsPath = ResourceUtil.getProperty(rb,"pi.base.data");
        reader.setMyDirectory(new File(piInputsPath));
        writer.setMyDirectory(new File(piInputsPath));
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
            "HH50to70k1to2",
            "HH70kUp1to2"};
        String[] largeHouseholds = {
            "HH0to5k3plus",
            "HH5to10k3plus",
            "HH10to15k3plus",
            "HH15to20k3plus",
            "HH20to30k3plus",
            "HH30to40k3plus",
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
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mhConstant+incomeCat*(mhIncome+ruralIncome)+ruralConstant));
            makeUseKeys[0] = largeHouseholds[incomeCat];
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (mhConstant+incomeCat*(mhIncome+ruralIncome)+ruralConstant));

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
            makeUseKeys[1] = "FLR SFD";
            makeUseKeys[0] = largeHouseholds[incomeCat];
            rows = makeUseIndex.getRowNumbers(makeUseKeys,nothing);
            makeUseTable.setValueAt(rows[0],constantColumn,(float) (sfdLargeHHConstant+incomeCat*sfdLargeHHIncome));

           }
        makeUseIndex.dispose();
        metaParamIndex.dispose();
        myCollection.flush();
        myCollection.close();
    }

    /* (non-Javadoc)
     * @see com.pb.despair.pi.PIPProcessor#setUpTransportConditions(java.lang.String[])
     */
    protected void setUpTransportConditions(String[] skimNames) {
        logger.info("Setting up Transport Conditions");
        String path1 = ResourceUtil.getProperty(rb, "pt.input.data");
        String path2 = ResourceUtil.getProperty(rb, "ts.input.data");

        SomeSkims someSkims = new SomeSkims(path1, path2);
        TransportKnowledge.globalTransportKnowledge = someSkims;
        for (int s=0;s<skimNames.length;s++) {
            someSkims.addZipMatrix(skimNames[s]);
        }
    }

    /* (non-Javadoc)
     * @see com.pb.despair.pi.PIPProcessor#writeOutputs()
     */
    public void writeOutputs() {
        super.writeOutputs();
        writeLaborConsumptionAndProductionFiles(); //writes out laborDollarProduction.csv,laborDollarConsumption.csv
    }

    public void writeLaborConsumptionAndProductionFiles() {
        boolean writeTheseOutputs = false;
        String oregonOutputsString = ResourceUtil.getProperty(rb, "pi.oregonOutputs");
        if (oregonOutputsString != null) {
            if (oregonOutputsString.equalsIgnoreCase("true")) {
                writeTheseOutputs = true;
            }
        }
        if (!writeTheseOutputs) {
            logger.warning("Not writing Oregon-Specific Outputs (labour consumption and production)");
            return;
        }
        LaborProductionAndConsumption labor = new LaborProductionAndConsumption(rb);
        TableDataSet householdQuantity = labor.loadTableDataSet("ActivityLocations2.csv","output.data");
        logger.fine("loaded ActivityLocations2.csv");
        TableDataSet alphaToBeta = labor.loadTableDataSet("alpha2beta.csv","reference.data");
        logger.fine("loaded alpha2beta.csv");
        labor.setZoneMap(alphaToBeta);
        logger.fine("created zone map");

        labor.createOccupationSet();
        logger.fine("created occupation set");
        labor.createActivitySet();
        logger.fine("created activity set");
        labor.createHouseholdSegmentSet();
        logger.fine("created household segment set");

        labor.setPopulation(householdQuantity);
        logger.fine("set population");

        labor.setProductionAndConsumption();
        logger.fine("set production and consumption");
        labor.writeToCSV();
    
        labor.sumProductionActivities();
        labor.sumConsumptionActivities();
        logger.fine("Finished setProductionAndConsumption()");
    }

    public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_PleaseWork/t1/pi/pi.properties"));
        OregonPIPProcessor pProcessor =  new OregonPIPProcessor(1,rb);
        pProcessor.createFloorspaceWFile();
    }

}
