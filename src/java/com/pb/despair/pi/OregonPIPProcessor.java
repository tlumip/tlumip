package com.pb.despair.pi;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.ResourceBundle;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
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

    public OregonPIPProcessor() {
        super();
    }

    /**
     * @param timePeriod
     */
    public OregonPIPProcessor(int timePeriod, ResourceBundle rb) {
        super(timePeriod, rb);
    }

    public void readFloorspace() {
        if (maxAlphaZone == 0) readFloorspaceZones();
        TableDataSet floorspaceTable = loadTableDataSet("FloorspaceI","ald.input.data");
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

    /* (non-Javadoc)
     * @see com.pb.despair.pi.PIPProcessor#setUpPi()
     */
    public void setUpPi() {
        String actWPath = ResourceUtil.getProperty(rb,"pi.current.data");
        File actW = new File(actWPath + "ActivitiesW.csv");
        if(actW.exists()){
            actW.delete();
            logger.info("Deleted old ActivitiesW.csv to prepare for new file");
        }
        String oregonInputsString = ResourceUtil.getProperty(rb, "pi.oregonInputs");
        if (oregonInputsString != null ) {
            if (oregonInputsString.equalsIgnoreCase("true")) {
                if(loadTableDataSet("ActivitesW","pi.current.data") == null){
                    doOregonSpecificInputProcessing();
                }

            }
        }
        super.setUpPi();
    }

    private void doOregonSpecificInputProcessing() {
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

}
