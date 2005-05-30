package com.pb.despair.ha;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.despair.ld.DevelopmentType;
import com.pb.despair.ld.DynamicPricesDevelopmentType;
import com.pb.despair.ld.LDModel;
import com.pb.despair.ld.ZoningScheme;
import com.pb.models.pecas.AbstractTAZ;
import com.pb.models.pecas.DevelopmentTypeInterface;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ResourceBundle;


public class TestAllHouseholds {
//    static double totalVacancy = 0;
//    static double oldTotalVacancy = 0;
    static int oldNumberOfHouseholds = 0;
    static int newNumberOfHouseholds = 0;
    static AllHouseholds ahh;
    static AbstractTAZ[] zones;
    static private String populationPath;
    static private String spaceTypePath;
    static private String householdCategoriesPath;
    static private Hashtable householdCategories = new Hashtable();

    public static void main(String[] args) {
        //final int numHouseholdsForTest = 1400;
        ResourceBundle rb = ResourceUtil.getResourceBundle( "ha" );
        populationPath = ResourceUtil.getProperty(rb, "population.path");
        spaceTypePath = ResourceUtil.getProperty(rb, "spaceType.path");
        householdCategoriesPath = ResourceUtil.getProperty(rb, "householdCategories.path");
  //      Household.useGridCells = (ResourceUtil.getProperty(rb, "useGridCells").equals("true"));
//        AppProperties appProps = PropertiesManager.getAppProperties("despair.properties");
        //int numTestZones = Integer.valueOf(appProps.getProperty("Model.numZones")).intValue();
        DevelopmentTypeInterface dtypes[] = setUpDevelopmentTypes();
        //TAZ.createTazArray(numTestZones);
        //for (int z = 0; z < numTestZones; z++) {
        //    TAZ.createTaz(z); // automatically puts it into the array based on the zone number z
        //}


        TableDataSet ztab = HAModel.reloadTableFromScratchFromTextFile(spaceTypePath,"Zones");
        TAZ.setUpZones(ztab);

        zones = TAZ.getAllZones();
        //Commodity.setUpCommodities(path);
        //Commodity.setUpExchangesAndZUtilities(path);
        
        ahh = AllHouseholds.getAllHouseholds(zones);
        Household.setAllHouseholds(ahh);
        Person.setAllHouseholds(ahh);
        // TODO use TableDataSetCollection to read in DevelopmentTypeUsage for HA 
        //DevelopmentType.setUpDevelopmentTypeUsage(HAModel.dm);
        ahh.buildSpaceChoiceLogit();
        ahh.setUpVacationSizeTerms(spaceTypePath);
        ZoningScheme.setUpZoningSchemes(HAModel.reloadTableFromScratchFromTextFile(spaceTypePath,"ZoningSchemes"));

        TableDataSet gridCellTable = HAModel.reloadTableFromScratchFromTextFile(spaceTypePath,"GridCells");
        LDModel.setUpGridCells(gridCellTable);
        
        readInSpacePrices();
        ahh.setUpLaborPrices(spaceTypePath);

        setHouseholdCategories(ahh);
        reportStatus(-2);

        AllHouseholds.readHouseholdsIntoMemory();
        reportStatus(-1);

/*        for (int h = 0; h < numHouseholdsForTest; h++) {
            Household testHH = new Household();
            int numberOfPeople = ahh.theRandom.nextInt(5) + 1;
            for (int p = 0; p < numberOfPeople; p++) {
                Person someone = new Person();
                someone.age = ahh.theRandom.nextInt(90) + 5; // no babies initially, 'cause they can't travel
                someone.myTravelPreferences = new com.pb.despair.pt.TimeAndDistanceTravelUtilityCalculator(ahh.theRandom.nextDouble() * 20,0.06);
                // 0 to 20 dollars/hr value of time
                someone.addToHousehold(testHH);
            }
            testHH.sampleIncome();
            testHH.samplePreferences();
            ahh.addEconomicUnit(testHH);
            ahh.addToMovingPool(testHH);

            testHH.findNewLocations();
            testHH.forgetZUtilities();
        } */
        // ok, we're all set up to test things out.
        ahh.migrationAndAllocation(1.0, 0, 0); // regular function for HA.
        reportStatus(0);
        for (int z = 0; z < zones.length; z++) {
           zones[z].gridCellDevelopmentUpdate(1.0);
        }

// only run one year for symposium
  //      ahh.migrationAndAllocation(1.0, 0, 0); // regular function for HA.
  //      reportStatus(1);

/*        for (int year = 2; year < 10; year++) {
            ahh.migrationAndAllocation(1.0, 0, 0); // regular function for HA.
            reportStatus((float) year);
            for (int z = 0; z < zones.length; z++) {
                zones[z].gridCellDevelopmentUpdate(1.0);
            }
        }
        */
        HAModel.dm.exportTable("SpacePricesAndUse", spaceTypePath+"SpacePricesAndUse");        
        AllHouseholds.writeHouseholdsToDatastore();
        LDModel.writeUpdatedGridCells(gridCellTable);
        HAModel.dm.closeStore();

        System.exit(0);
    }

    public static DevelopmentType[] setUpDevelopmentTypes() {
        TableDataSet ctab = HAModel.reloadTableFromScratchFromTextFile(spaceTypePath,"DevelopmentTypes");
        ArrayList dtypes = new ArrayList();
        for(int r=1;r<=ctab.getRowCount();r++) {
            String typeName = ctab.getStringValueAt(r,"DevelopmentTypeName");
            int gridCode = ctab.getStringValueAt(r,"GridCode").charAt(0);
            boolean dynamic = Boolean.valueOf(ctab.getStringValueAt(r,"DynamicPricesDevelopmentType")).booleanValue();
            if (dynamic) {
                dtypes.add(new DynamicPricesDevelopmentType(typeName,
                    (double)ctab.getValueAt(r,"PortionVacantMultiplier"),
                    (double)ctab.getValueAt(r,"EquilibriumVacancyRate"),
                    (double)ctab.getValueAt(r,"MinimumBasePrice"),gridCode));
            } else {
                dtypes.add(new DevelopmentType(typeName,gridCode));
            }
        }
       DevelopmentType[] d = new DevelopmentType[dtypes.size()];
        return (DevelopmentType[]) dtypes.toArray(d);
    }

    static void reportStatus(float time) {
        System.out.println(ahh.reportPools());
        //  System.out.println("Size of new households pool =" + ahh.newHouseholdsPool.size());
        newNumberOfHouseholds = ahh.getEconomicUnits().size();
        //  Systemh.out.println("Number of households =" + newNumberOfHouseholds);
        int population = 0;
        Household[] meu = new Household[newNumberOfHouseholds];
        meu = (Household[]) ahh.getEconomicUnits().toArray(meu);
        for (int hhnum = 0; hhnum < meu.length; hhnum++) {
            Household hh = meu[hhnum];
            population += hh.getPeople().size();
/*            if (hh.getPrimaryLocation() == null) { } //System.out.println(hh+" doesn't have a home");
            else
                floorspaceUsed[hh.getHomeZone().getZoneIndex()] += hh.getPrimaryLocation().getSize();
            if (hh.getSecondaryLocation() != null)
                floorspaceUsed[hh.getSecondaryZone().getZoneIndex()] += hh.getSecondaryLocation().getSize();
                */
        }
        System.out.println("Population = " + population);
/*        for (int z = 0; z < floorspaceUsed.length; z++) {
            //      System.out.println("Zone "+z+" floorspace used "+floorspaceUsed[z]);
        } */
        // TODO: Have to write this to use our TableDataSets instead of Borland's JDatastore TableDataSets
        //DevelopmentType.writeStatusForAll(HAModel.dm, time);
        oldNumberOfHouseholds = newNumberOfHouseholds;
    }


    public static void readInHouseholdCategories() {
        TableDataSet tab = HAModel.reloadTableFromScratchFromTextFile(spaceTypePath,"HouseholdCategories");
        for(int r=1;r<=tab.getRowCount();r++) {
            String typeName = tab.getStringValueAt(r,"CategoryName");
            FixedSizeHouseholdCategory category = (FixedSizeHouseholdCategory) householdCategories.get(typeName);
            if (category == null) {
            	category = new FixedSizeHouseholdCategory();
            	householdCategories.put(typeName,category);
            }
            FixedSizeHouseholdCategory.PersonDescriptor pd = new FixedSizeHouseholdCategory.PersonDescriptor();
            pd.minAge = (int)tab.getValueAt(r,"MinAge");
            pd.maxAge = (int)tab.getValueAt(r,"MaxAge");
            pd.gender = tab.getStringValueAt(r,"Gender").charAt(0);
            pd.fullTimeEmployedProbability = (double)tab.getValueAt(r,"FullTimeEmployedProbability");
            pd.partTimeEmployedProbability = (double)tab.getValueAt(r,"PartTimeEmployedProbability");
            pd.minEducation = (int)tab.getValueAt(r,"MinEducation");
            pd.maxEducation = (int)tab.getValueAt(r,"MaxEducation");
            category.addPerson(pd);
        }
    }
    	
    public static void setHouseholdCategories(final AllHouseholds ahh) {
    	readInHouseholdCategories();
        ahh.newHouseholdProbabilities = new HouseholdPossibilities();
        ahh.joiningUpProbabilities = new HouseholdPossibilities();
        TableDataSet tab = HAModel.reloadTableFromScratchFromTextFile(spaceTypePath,"HouseholdPossibilities");
        for(int r=1;r<=tab.getRowCount();r++) {
            String typeName = tab.getStringValueAt(r,"JorN");
            	String categoryName = tab.getStringValueAt(r,"CategoryName");
            	double weight = (double)tab.getValueAt(r,"Weight");
            	HouseholdCategory category = (HouseholdCategory) householdCategories.get(categoryName);
            	if (category==null) throw new RuntimeException("Invalid household category name in householdpossibilities.csv: "+categoryName);
            if (typeName.equals("J")) {
            	ahh.joiningUpProbabilities.add(category,weight);
            }
            if (typeName.equals("N")) {
            	ahh.newHouseholdProbabilities.add(category,weight);
            }
        }
    }
    

    public static void readInSpacePrices() {
        TableDataSet tab = HAModel.reloadTableFromScratchFromTextFile(spaceTypePath,"FloorspaceRents");
        for(int r=1;r<=tab.getRowCount();r++) {
            String typeName = tab.getStringValueAt(r,"DevelopmentType");
            DevelopmentTypeInterface dt = DevelopmentType.getAlreadyCreatedDevelopmentType(typeName);
            int zone = (int)tab.getValueAt(r,"Zone");
            double price = (double)tab.getValueAt(r,"Price");
            AbstractTAZ taz = AbstractTAZ.findZoneByUserNumber(zone);
            if (taz == null) {
            	System.out.println("ERROR: unknown zone number in FloorspaceRents.csv "+zone);
        	}  else taz.updatePrice(dt,price);
        }
    }
    
}
