package com.pb.tlumip.ha;

import com.pb.tlumip.ld.DevelopmentType;
import com.pb.tlumip.ld.DynamicPricesDevelopmentType;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.pecas.DisaggregateActivity;
import com.pb.models.pecas.AbstractTAZ;
import com.pb.models.pecas.DevelopmentTypeInterface;

import java.util.*;

/**
 * A class that represents the full set of households in the model.
 * The households themselves are stored as a Vector in the inherited attribute myEconomicUnits. Responsibilities:
 * A:2 ( A:2.1 and A:2.2 responsibility of each Household) D:  household locational changes (With Household)
 * @see Household
 * @author J. Abraham
 */
public class AllHouseholds extends DisaggregateActivity {
    // Attributes
    final String[] commodityTypes = {"1_ManPro", "1a_Health", "2_PstSec", "3_OthTchr","4_OthP&T","5_RetSls", "6_OthR&C", "7_NonOfc"};
    double[][] commodityPrices;
    double[][] commoditySizes;
    
    final HouseholdLocationLogit tazLocations = new HouseholdLocationLogit();
    final VacationLocationLogit vacationLocations = new VacationLocationLogit();
    //    final HouseholdSpaceChoiceLogit spaceChoiceLogit = new HouseholdSpaceChoiceLogit(this);
    HouseholdSpaceChoiceLogit spaceChoiceLogit;

    void buildSpaceChoiceLogit() {
        spaceChoiceLogit = new HouseholdSpaceChoiceLogit(this);
    }

    private AllHouseholds(AbstractTAZ[] allZones) {
        super("AllHouseholds", allZones);
        tazLocations.addAlternatives(allZones);
        vacationLocations.addAlternatives(allZones);
        commodityPrices = new double[commodityTypes.length][allZones.length];
        commoditySizes = new double[commodityTypes.length][allZones.length];
        for (int s=0;s<allZones.length;s++) {
            for (int c=0; c<commodityTypes.length;c++) {
                //default size is 1.
                commoditySizes[c][s]=1;
            }
        }
    }

    public double getUtility() {
        // what to do!!!
        throw new Error("Utility calculation for allHouseholds not yet implemented");
    }

    static private AllHouseholds allHouseholds = null;
    HouseholdPossibilities joiningUpProbabilities;
    HouseholdPossibilities newHouseholdProbabilities;

    /** AllHouseholds is a singleton class.  This method creates the singleton if it isn't already created, then returns it */
    public static AllHouseholds getAllHouseholds(AbstractTAZ[] allZones) {
        if (allHouseholds == null) {
            allHouseholds = new AllHouseholds(allZones);
        }
        return allHouseholds;
    }

    static double housingMarketSubTimeStep = 2.0/12.0;
    final java.util.Random theRandom = new Random();
    Vector newHouseholdsPool = new Vector();

    /**
     * This is the main workhorse routine that takes the households in the region through their spatial transitions.
     * This routine models a discrete time step.  Generally, the time step
     * will be "1" because the model is initially set up to work in 1 year
     * periods.  But it's important to be able to adjust the time step if
     * necessary.  A longer time step implies larger changes.  There are a
     * lot of binary logit models of whether people do things - whether to
     * die, whether to be born, whether to relocate, etc.  We need to work
     * through the theory of that and make sure that if the timestep is
     * doubled the chance of things happening is also doubled.  This probably
     * is just an extra term (perhaps scaled by the dispersion parameter) in the utility.
     * This routine is directly responsible for: D:2 - matching people to new or existing households (D:2.3, D:2.4
     * can be done by the Household itself D:4 Creating the new households from in-migration (D:4.1 and D:2.4
     * can be done by the household itself) This routine also tells each household to do transitions.  Thus for each
     * household it has to call spatialChanges, demographicChanges.
     * @param timeStep the amount of time that has passed since this function was last called.
     * @param inMigration the amount of activity moving to the region
     * @param outMigration the amount of activity leaving the region.  Net migration is inMigration-outMigration.
     */
    public void migrationAndAllocation(double timeStep, double inMigration, double outMigration) {
        // copy them all into an array, because as we go through the
        // demographic changes households will be added and changed.  If we
        // used an iterator we might get concurrent modification errors.
    
    
        // this section shuffles and divides the households into sections to be
        // processed by subtimestep.  This is where we would divide them into DAF tasks.
        //TODO: Divide into DAF tasks
        System.out.println("getting Vector of households");
        Vector households = new Vector(getEconomicUnits());
        System.out.println("shuffling Vector of households");
        Collections.shuffle(households);
        Household[] allOfThem = new Household[households.size()];
        System.out.println("copying household references into array");
        allOfThem = (Household[]) households.toArray(allOfThem);
        double portionPerSubStep = housingMarketSubTimeStep / timeStep;
        if (portionPerSubStep > 1.0) portionPerSubStep = 1.0;
        double numHouseholdsPerStepDouble = allOfThem.length * portionPerSubStep;
        int numHouseholdsPerStep = ((int)numHouseholdsPerStepDouble) + 1;
        int hhnum = 0;
        int lastMarketClearing = 0;
        System.out.println("...iterating through the households now...");
        // end of dividing households by DAF tasks
        
        while (hhnum < allOfThem.length) {
        	
           // this is the most processor intensive code because it does demographic
            // transitions on every household, including the decision of whether to move.
            // TODO: this block is probably suitable for parallelization using DAF
            if (hhnum < 10 || hhnum % 100 == 0) System.out.println(hhnum + "..");
            Household theOne = allOfThem[hhnum];
    
            if (theOne.demographicChanges(timeStep)) {
                // everyone left home!
                theOne.freeUpFloorspace();
                removeEconomicUnit(theOne);
            } else {
                theOne.decideActionsRegardingLocations(timeStep); // see if they move
           /*     if (!(movingPool.contains(theOne) || secondaryLocationMovingPool.contains(theOne))) {
                    theOne.forgetZUtilities();  // don't need this anymore because households no longer use ZUtilities
                } */
            }
            hhnum++;
    
    
            // This section is probably less suitable for parallelization using DAF, because it only 
            // processes the moving households, and because it interacts with the in-memory residential
            // Grid Cells.
            if (hhnum - lastMarketClearing >= numHouseholdsPerStep || hhnum == allOfThem.length) {
                           /* now enough people have moved to represent a reasonable
                           selection in the housing market and in the market for household
                           partners (spouses, etc.)  Time to clear those markets */
    
                System.out.println("Before joining up new household pool " + reportPools());
                this.joinUpNewHouseholdsPool(housingMarketSubTimeStep);
                System.out.println("Before creating in migration households " + reportPools());
                this.createInMigrationHouseholds(housingMarketSubTimeStep);
                System.out.println("Before moving households out " + reportPools());
                this.moveHouseholdsOut();
                System.out.println("Before finding homes for pools " + reportPools());
                this.findHomesForPools(housingMarketSubTimeStep);
                System.out.println("After " + hhnum + " households " + reportPools());
    
                // now update residential floorspace prices   
    	        Iterator dTypes = allowedIn.iterator();
    	        while (dTypes.hasNext()) {
    	            DevelopmentTypeInterface dt = (DevelopmentTypeInterface)dTypes.next();
    	            if (dt instanceof DynamicPricesDevelopmentType) {
    	                // message #1.8.4 to eachTaz:com.pb.tlumip.pi.TAZ
    	                // eachTaz.updatePricesOfMicrosimDevelopment();
    	                ((DynamicPricesDevelopmentType)dt).updatePrices(housingMarketSubTimeStep);
    	            }
    	        }
                lastMarketClearing = hhnum;
            }
            // TODO: now need to assign new job locations to people who are employed without a location
        }
    }

    /**
     * Households are modelled dynamically, not in equilibrium, so there's no way to reallocate in
     * response to a change in prices
     * @exception CantRedoError not all types of ProductionActivity can redo their allocation.  Obviously, then, they
     * can't be modelled as being in spatial economic equilibrium
     */
    public void reMigrationAndReAllocation() throws CantRedoError {
        throw new CantRedoError("households can't be re-simulated");
    }

    public void setUpLaborPrices(String path) {
//        try {
//            AbstractTAZ[] abstractZones = AbstractTAZ.getAllZones();
//            TAZ[] zones = new TAZ[abstractZones.length];
//            for (int z = 0; z < zones.length; z++) {
//                zones[z] = (TAZ)abstractZones[z];
//            }
//            TableDataSet sietab = HAModel.reloadTableFromScratchFromTextFile(path, "CommodityMarkets");
//            DataRow searchMarkets = new DataRow(sietab,
//                new String[] { "Zone", "Commodity" });
//            for (int l=0; l<commodityTypes.length; l++) {
//                for (int z = 0; z < zones.length; z++) {
//                    searchMarkets.setInt("Zone", zones[z].getZoneUserNumber());
//                    searchMarkets.setString("Commodity", commodityTypes[l]);
//                    boolean found = sietab.locate(searchMarkets, 0x20); // 0x20 is supposed to be
//                    // com.borland.dx.dataset.Locate.FIRST but the Locate class def seems to be missing
//                    if (found) {
//                        commodityPrices[l][z]=sietab.getDouble("Price");
//                        commoditySizes[l][z]=sietab.getDouble("Size");
//                    }
//                }
//            }
//        } catch (DataSetException e) {
//            System.err.println("Error: setUpExchangesAndUtilities()");
//            e.printStackTrace();
//        }
    }

    private double[] vacationHomeSizeTerms = new double[5000];

    public void setUpVacationSizeTerms(String path) {

 /*           AbstractTAZ[] abstractZones = AbstractTAZ.getAllZones();
            TAZ[] zones = new TAZ[abstractZones.length];
            for (int z = 0; z < zones.length; z++) {
                zones[z] = (TAZ)abstractZones[z];
            }*/
            TableDataSet vacationSizeTable = HAModel.reloadTableFromScratchFromTextFile(path, "VacationHomeSizeTerms");
            for(int r=1;r<=vacationSizeTable.getRowCount();r++) {
                int zoneNumber = (int)vacationSizeTable.getValueAt(r,"Zone");
                double size = (double)vacationSizeTable.getValueAt(r,"Size");
                if (zoneNumber >= vacationHomeSizeTerms.length) {
                    double[] old = vacationHomeSizeTerms;
                    vacationHomeSizeTerms = new double[old.length*2];
                    System.arraycopy(old,0,vacationHomeSizeTerms,0,old.length);
                }
                vacationHomeSizeTerms[zoneNumber]=size;
            }
     }


    private void joinUpNewHouseholdsPool(double elapsedTime) {
     final int numberOfCategoriesToTry = 1000;
     int unsuccessfulTries = 0;
     Collections.shuffle(newHouseholdsPool);
     Vector householdLessPeople = new Vector();
     while (newHouseholdsPool.size() > 0 && unsuccessfulTries < numberOfCategoriesToTry) {
         Household h = null;
         HouseholdCategory hc = joiningUpProbabilities.randomCategory();
         h = hc.createANewHousehold(newHouseholdsPool);
         if (h==null) {
             unsuccessfulTries++;
         } else {
            addEconomicUnit(h);
            movingPool.add(h);
            h.setYearsSinceMove((short) 0);
            h.eraseAllButOneJob();
            h.secondSetOfDemographicChanges(elapsedTime);
         }
     }
 }


/* the old way...
 * 
 * private void joinUpNewHouseholdsPool(double elapsedTime) {
        final int numberOfCategoriesToTry = 1000;
        Vector householdLessPeople = new Vector();
        while (newHouseholdsPool.size() > 0) {
            Collections.shuffle(newHouseholdsPool);
            Person[] everyoneSearching = new Person[newHouseholdsPool.size()];
            everyoneSearching = (Person[]) newHouseholdsPool.toArray(everyoneSearching);
            for (int pc = 0; pc < everyoneSearching.length; pc++) {
                Person p = everyoneSearching[pc];
                if (newHouseholdsPool.contains(p)) {
                    //have to be careful, person might have already been removed by someone
                    // else and formed a brand-new household
                    int attempts = 0;
                    Household h = null;
                    Person swappedOut = null;
                    while ((h == null || swappedOut == null) && attempts < numberOfCategoriesToTry) {
                        attempts++;
                        HouseholdCategory hc = newHouseholdProbabilities.randomCategory();
                        h = hc.createANewHousehold(newHouseholdsPool);
                        if (h != null) {
                            swappedOut = hc.swapInPerson(h, p);
                        }
                        if (h != null && swappedOut != null) {
                            if (p != swappedOut) {
                                newHouseholdsPool.remove(p);
                                newHouseholdsPool.add(swappedOut);
                            }
                            addEconomicUnit(h);
                            movingPool.add(h);
                            h.eraseAllButOneJob();
                            h.secondSetOfDemographicChanges(elapsedTime);
                        } else {
                            if (h != null) {
                                h.dissolveInto(newHouseholdsPool);
                            }
                        }
                    }
                    if (h == null || swappedOut == null) {
                        System.out.println("After " + numberOfCategoriesToTry +
                            " tries there still doesn't seem to be suitable new household for " + p);
                        householdLessPeople.add(p);
                        newHouseholdsPool.remove(p);
                    }

                }
            }
        }
        newHouseholdsPool.addAll(householdLessPeople);
    }
*/
    public synchronized void createInMigrationHouseholds(double elapsedTime) {
     //   addEconomicUnit(h);
     //   movingPool.add(h);
    }

    private void moveHouseholdsOut() {
        Collection hh = getEconomicUnits();
        Household[] hhArray = new Household[hh.size()];
        hhArray = (Household[]) hh.toArray(hhArray);
        for (int h = 0; h < hhArray.length; h++) {
            hhArray[h].doAnyFlaggedMoves();
        }
    }

    private void findHomesForPools(double elapsedTime) {
        Household[] movingHouseholds = new Household[movingPool.size()];
        movingHouseholds = (Household[]) movingPool.toArray(movingHouseholds);
        for (int i = 0; i < movingHouseholds.length; i++) {
            // message #1.8.1 to aHousehold:com.pb.tlumip.ha.Household
            // aHousehold.findNewResidences();
            movingHouseholds[i].findNewLocations();
            if (movingHouseholds[i].getPrimaryLocation() == null) {
                //System.out.println("Couldn't find a home for "+movingHouseholds[i]);
                if (!movingPool.contains(movingHouseholds[i])) {
                    throw new Error(movingHouseholds[i] + " doesn't have a home but slipped out of the moving pool");
                }
            }
           // movingHouseholds[i].forgetZUtilities();
            // this will take people out of the SecondaryResidence moving pool too,
            // but only if they were also in the regular moving pool
        }
        movingHouseholds = new Household[secondaryLocationMovingPool.size()];
        movingHouseholds = (Household[]) secondaryLocationMovingPool.toArray(movingHouseholds);
        for (int i = 0; i < movingHouseholds.length; i++) {
            movingHouseholds[i].findNewLocations();
           // movingHouseholds[i].forgetZUtilities();
            // this will take people out of the SecondaryResidence moving pool too,
            // and will also check if they are still in the regular moving pool (they shouldn't be!)
        }
        
    }

    public String toString() { return "AllHouseholds singleton object"; };

    public static void prepareHouseholdVariables() {
        //TODO rewrite this method to use com.pb.common.TableDataSet instead of Borland.TableDataSet
//        if (allHouseholds == null)
//            throw new Error("Forgot to set up AllHouseholds before trying to prepare household variables");
//        TableDataSet householdFile = HAModel.dm.getTableDataSet("SynPopH");
//        TableDataSet personFile = HAModel.dm.getTableDataSet("SynPopP");
//        if (personFile.hasColumn("JOBTAZ")==null) {
//            personFile.addColumn("JOBTAZ",Variant.INT);
//            personFile.restructure();
//        }
//        boolean householdRestructure = false;
//        if (householdFile.hasColumn("VACATIONHOMETAZ")==null) {
//            householdFile.addColumn("VACATIONHOMETAZ",Variant.INT);
//            householdRestructure = true;
//        }
//        if (householdFile.hasColumn("SQFT")==null) {
//            householdFile.addColumn("SQFT",Variant.FLOAT);
//            householdRestructure = true;
//        }
//        if (householdRestructure) householdFile.restructure();
//        // debug june 24
//        String[] columnNames = householdFile.getColumnNames(householdFile.getColumns().length);
//        for (int c = 0; c < columnNames.length; c++) {
//            System.out.println("Column " + c + " (" + columnNames[c] + ") is type " +
//                Variant.typeName(householdFile.getColumn(c).getDataType()));
//        }
//        Household hh = null;
//        int counter = 0;
//
///*        DataRow searchForSerialNo = new DataRow(personFile,
//                new String[] { "SERIALNO"}); */
//
//        Hashtable householdList = new Hashtable(100000, (float)1.0);
//        // read in household file
//        System.out.println("Reading in household data file");
//        do {
//            int hhid = householdFile.getInt("HH_ID");
//            if (++counter < 5 || counter % 10000 == 0) System.out.println("hhnum " + counter + " ID:" + hhid);
//            hh = new Household(hhid);
//            hh.setIncome(householdFile.getInt("RHHINC"));
//            hh.setAutos((short) ( householdFile.getInt("AUTOS") -1));
//            if (hh.getAutos() < 0) hh.setAutos((short) 0);
//            hh.setYearsSinceMove((short) householdFile.getInt("YRMOVED"));
//            Object existingDuplicateHousehold = householdList.get(new Integer(hhid));
//            if (existingDuplicateHousehold!=null) System.out.println("duplicate household ID "+hhid);
//            else householdList.put(new Integer(hhid), hh);
//            if (householdFile.atLast()) break;
//            householdFile.next();
//        } while (true);
//        counter = 0;
//        System.out.println("reading in Person data file");
//        do {
//        	int personId = personFile.getInt("PERS_ID");
//            int hhid = personFile.getInt("HH_ID");
//        	if (personId<100) personId += hhid*100;
//            Person p = new Person(personId);
//            p.age = personFile.getInt("AGE");
//            p.female = personFile.getInt("SEX") == 1 ? true : false;
//            int employStatus = personFile.getInt("RLABOR");
//            if (employStatus == 0 || employStatus == 3 || employStatus == 6) {
//                p.employStatus = 0;
//            } else {
//                if (personFile.getInt("HOURS") >= 35) {
//                    p.employStatus = 2;
//                } else {
//                    p.employStatus = 1;
//                }
//                if (personFile.hasColumn("JOBTAZ")!=null) {
//                    p.myJobTAZ=personFile.getInt("JOBTAZ");
//                }
//            }
//            p.schoolStatus = (short) personFile.getInt("SCHOOL");
//            p.yearsSchool = (short) personFile.getInt("YEARSCH");
//            p.fertil = (short) personFile.getInt("FERTIL");
//            p.occupation = (short) personFile.getInt("OCCUP");
//            if (++counter < 5 || counter % 10000 == 0) System.out.println("personnum " + counter + " ID:" + hhid);
//            hh = (Household)householdList.get(
//                new Integer(hhid));
//            if (hh == null) {
//                System.out.println("Can't find household with ID number " + hhid + " to match person file");
//                throw new Error("Can't find household with ID number " + hhid + " to match person file");
//            }
//            p.addToHousehold(hh);
//            if (personFile.atLast()) break;
//            personFile.next();
//        } while (true);
//        householdFile.first();
//        counter = 0;
//        int errorCounter = 0;
//        System.out.println("Assigning square feet to households");
//        do {
//            int hhid = householdFile.getInt("HH_ID");
//            hh = (Household)householdList.get(
//                new Integer(hhid));
//            hh.sampleIncome();
//            if (++counter < 5 || counter % 1000 == 0) System.out.println("hhnumnum " + counter + " ID:" + hhid);
//            int units1= householdFile.getInt("UNITS1");
//            DevelopmentTypeInterface existingDt = null;
//            if (units1>4) existingDt=getMFDType();
//            if (units1==3 || units1==4) existingDt=getATDType();
//            boolean groupQuarters=false;
//            if (units1==0) {
//            	existingDt=getMFDType();
//            	groupQuarters=true;
//            }
//            if (units1==2 || units1==1) {
//                int oneAcre = householdFile.getInt("ONEACRE");
//                if (oneAcre==2) {
//                	if (units1 == 1) {existingDt = getRRMHDType();}
//                	else {existingDt = getRRSFDDType();}
//                }
//                else {
//                	if (units1==1) {existingDt=getMHDType();}
//                    else existingDt = getSFDDType();
//                }
//            }
//            int rooms = householdFile.getInt("ROOMS");
//            if (groupQuarters) rooms = 1;
//            float floorspace=hh.roomsToSqft((float) rooms,existingDt);
//            //float floorspace=hh.spaceNeeded(existingDt);
//            householdFile.setFloat("SQFT",floorspace);
//            householdFile.setInt("RHHINC",(int) hh.getIncome());
//            if (householdFile.atLast()) break;
//            householdFile.next();
//        } while (true);
    }

    public static void readHouseholdsIntoMemory() {
        //TODO rewrite this method to use com.pb.common.TableDataSet instead of Borland.TableDataSet
//        if (allHouseholds == null)
//            throw new Error("Forgot to set up AllHouseholds before trying to read households from text files");
//        TableDataSet householdFile = HAModel.dm.getTableDataSet("SynPopH");
//        TableDataSet personFile = HAModel.dm.getTableDataSet("SynPopP");
//        // debug june 24
//        String[] columnNames = householdFile.getColumnNames(householdFile.getColumns().length);
//        for (int c = 0; c < columnNames.length; c++) {
//            System.out.println("Column " + c + " (" + columnNames[c] + ") is type " +
//                Variant.typeName(householdFile.getColumn(c).getDataType()));
//        }
//        Household hh = null;
//        int counter = 0;
//
///*        DataRow searchForSerialNo = new DataRow(personFile,
//                new String[] { "SERIALNO"}); */
//
//        Hashtable householdList = new Hashtable(100000, (float)1.0);
//        // read in household file
//        System.out.println("Reading in household data file");
//        do {
//            int hhid = householdFile.getInt("HH_ID");
//            if (++counter < 5 || counter % 10000 == 0) System.out.println("hhnum " + counter + " ID:" + hhid);
//            hh = new Household(hhid);
//            hh.setIncome(householdFile.getInt("RHHINC"));
//            hh.setAutos((short) ( householdFile.getInt("AUTOS") -1));
//            if (hh.getAutos() < 0) hh.setAutos((short) 0);
//            hh.setYearsSinceMove((short) householdFile.getInt("YRMOVED"));
//            Object existingDuplicateHousehold = householdList.get(new Integer(hhid));
//            if (existingDuplicateHousehold!=null) System.out.println("duplicate household ID "+hhid);
//            else householdList.put(new Integer(hhid), hh);
//            if (householdFile.atLast()) break;
//            householdFile.next();
//        } while (true);
//        counter = 0;
//        System.out.println("reading in Person data file");
//        do {
//        	int personId = personFile.getInt("PERS_ID");
//            int hhid = personFile.getInt("HH_ID");
//        	if (personId<100) personId += hhid*100;
//            Person p = new Person(personId);
//            p.age = personFile.getInt("AGE");
//            p.female = personFile.getInt("SEX") == 1 ? true : false;
//            int employStatus = personFile.getInt("RLABOR");
//            if (employStatus == 0 || employStatus == 3 || employStatus == 6) {
//                p.employStatus = 0;
//            } else {
//                if (personFile.getInt("HOURS") >= 35) {
//                    p.employStatus = 2;
//                } else {
//                    p.employStatus = 1;
//                }
//                if (personFile.hasColumn("JOBTAZ")!=null) {
//                    p.myJobTAZ=personFile.getInt("JOBTAZ");
//                }
//            }
//            p.schoolStatus = (short) personFile.getInt("SCHOOL");
//            p.yearsSchool = (short) personFile.getInt("YEARSCH");
//            p.fertil = (short) personFile.getInt("FERTIL");
//            p.occupation = (short) personFile.getInt("OCCUP");
//            if (++counter < 5 || counter % 10000 == 0) System.out.println("personnum " + counter + " ID:" + hhid);
//            hh = (Household)householdList.get(
//                new Integer(hhid));
//            if (hh == null) {
//                System.out.println("Can't find household with ID number " + hhid + " to match person file");
//                throw new Error("Can't find household with ID number " + hhid + " to match person file");
//            }
//            p.addToHousehold(hh);
//            if (personFile.atLast()) break;
//            personFile.next();
//        } while (true);
//        householdFile.first();
//        counter = 0;
//        int errorCounter = 0;
//        System.out.println("Assigning grid cells to households");
//        do {
//            int hhid = householdFile.getInt("HH_ID");
//            AbstractTAZ t = AbstractTAZ.findZoneByUserNumber(householdFile.getInt("TAZ"));
//            hh = (Household)householdList.get(
//                new Integer(hhid));
//            if (++counter < 5 || counter % 1000 == 0) System.out.println("hhnumnum " + counter + " ID:" + hhid);
//            if (t == null) {
//                allHouseholds.addToMovingPool(hh);
//                if (errorCounter < 100) {
//                    System.out.println("can't find TAZ " + householdFile.getInt("TAZ"));
//                    errorCounter++;
//                    if (errorCounter == 100) System.out.println("surpressing further invalid-TAZ errors");
//                }
//            } else {
//	            int units1= householdFile.getInt("UNITS1");
//	            DevelopmentTypeInterface existingDt = null;
//	            if (units1>4) existingDt=getMFDType();
//	            if (units1==3 || units1==4) existingDt=getATDType();
//	            if (units1==0) existingDt=getMFDType();
//	            if (units1==2 || units1==1) {
//		            int oneAcre = householdFile.getInt("ONEACRE");
//	            	if (oneAcre==2) {
//	            		if (units1==1) {existingDt=getRRMHDType();}
//                        else existingDt = getRRSFDDType();
//	            	}
//	            	else {
//	            		if (units1==1) {existingDt=getMHDType();}
//                        else existingDt = getSFDDType();
//                    }
//	            }
//                try {
//                	if (existingDt != null) {
//                		hh.findLocationInZone(t, 0, existingDt);
//                	} else  hh.findLocationInZone(t, 0);
//                } catch (com.pb.tlumip.model.AbstractTAZ.CantFindRoomException e) {
//                    allHouseholds.addToMovingPool(hh);
//                }
//            }
//            allHouseholds.addEconomicUnit(hh);
//            if (householdFile.atLast()) break;
//            householdFile.next();
//        } while (true);
    }


    public static void writeHouseholdsToDatastore() {
//        if (allHouseholds == null)
//            throw new Error("Forgot to set up AllHouseholds before trying to read households from text files");
//        TableDataSet householdFile = HAModel.dm.getTableDataSet("SynPopH");
//        TableDataSet personFile = HAModel.dm.getTableDataSet("SynPopP");
//        personFile.empty();
//        householdFile.empty();
//        if (personFile.hasColumn("JOBTAZ")==null) {
//            personFile.addColumn("JOBTAZ",Variant.INT);
//            personFile.restructure();
//        }
//        if (householdFile.hasColumn("VACATIONHOMETAZ")==null) {
//            householdFile.addColumn("VACATIONHOMETAZ",Variant.INT);
//            householdFile.restructure();
//        }
//        Iterator householdsIterator = allHouseholds.getEconomicUnits().iterator();
//        System.out.println("Writing out household data file");
//        Household hh;
//        while (householdsIterator.hasNext()) {
//            hh = (Household)householdsIterator.next();
//            householdFile.insertRow(false);
//            householdFile.setInt("RHHINC",(int) hh.getIncome());
//            householdFile.setInt("AUTOS", hh.getAutos());
//            householdFile.setInt("YRMOVED", hh.getYearsSinceMove());
//            Iterator peopleIterator = hh.getPeople().iterator();
//            while (peopleIterator.hasNext()) {
//                personFile.insertRow(false);
//                Person p = (Person)peopleIterator.next();
//                personFile.setInt("PERS_ID", p.id);
//                personFile.setInt("AGE", (int) p.age);
//                if (p.female) personFile.setInt("SEX", 1);
//                else
//                    personFile.setInt("SEX", 0);
//                if (p.employStatus == 0) {
//                    personFile.setInt("RLABOR", 6);
//                    personFile.setInt("HOURS", 0);
//                } else {
//                    personFile.setInt("RLABOR", 1);
//                    if (p.employStatus == 2) personFile.setInt("HOURS", 40);
//                    else
//                        personFile.setInt("HOURS", 20);
//                }
//                personFile.setInt("SCHOOL",p.schoolStatus);
//                personFile.setInt("YEARSCH",p.yearsSchool);
//                personFile.setInt("FERTIL",p.fertil);
//                personFile.setInt("OCCUP",p.occupation);
//                personFile.setInt("HH_ID", hh.id);
//                personFile.setInt("JOBTAZ", p.myJobTAZ);
//                personFile.post();
//            }
//            householdFile.setInt("HH_ID", hh.id);
//            DevelopmentTypeInterface existingDt = null;
//   			if (hh.getPrimaryLocation()!=null) {
//   				existingDt = hh.getPrimaryLocation().getIsLocatedWithin().getCurrentDevelopment();
//   				int oneAcre = 0;
//	            if (existingDt==getMFDType()) householdFile.setInt("UNITS1",5);
//	            if (existingDt==getATDType()) householdFile.setInt("UNITS1",3);
//	            if (existingDt==getMHDType()) householdFile.setInt("UNITS1",1);
//	            if (existingDt==getSFDDType()) {
//                    householdFile.setInt("UNITS1",2);
//                    oneAcre = 1;
//                }
//	            if (existingDt==getRRSFDDType()) {
//	            	householdFile.setInt("UNITS1",2);
//	            	oneAcre = 2;
//	            }
//	            if (existingDt==getRRMHDType()) {
//	            	householdFile.setInt("UNITS1",1);
//	            	oneAcre = 2;
//	            }
//	            householdFile.setInt("ONEACRE",oneAcre);
//   			}
//            AbstractTAZ homeZone = hh.getHomeZone();
//            if (homeZone == null) householdFile.setInt("TAZ", 0);
//            else
//                householdFile.setInt("TAZ", homeZone.getZoneUserNumber());
//            AbstractTAZ vacationZone = hh.getSecondaryZone();
//            if (vacationZone == null) householdFile.setInt("VACATIONHOMETAZ", 0);
//            else
//                householdFile.setInt("VACATIONHOMETAZ", vacationZone.getZoneUserNumber());
//            householdFile.post();
//        }
    }
    static DevelopmentTypeInterface mfDType=null;
    static DevelopmentTypeInterface sfdDType=null;
    static DevelopmentTypeInterface mhDType=null;
    static DevelopmentTypeInterface atDType=null;
    static DevelopmentTypeInterface rrsfdDType=null;
    static DevelopmentTypeInterface rrmhDType=null;
    /**
     * Returns the atDType.
     * @return DevelopmentTypeInterface
     */
    public static DevelopmentTypeInterface getATDType() {
    	if (atDType == null) atDType = DevelopmentType.getAlreadyCreatedDevelopmentType("AT");
        return atDType;
    }

    /**
     * Returns the mfDType.
     * @return DevelopmentTypeInterface
     */
    public static DevelopmentTypeInterface getMFDType() {
    	if (mfDType == null) mfDType = DevelopmentType.getAlreadyCreatedDevelopmentType("MF");
        return mfDType;
    }

    /**
     * Returns the mhDType.
     * @return DevelopmentTypeInterface
     */
    public static DevelopmentTypeInterface getMHDType() {
    	if (mhDType == null) mhDType = DevelopmentType.getAlreadyCreatedDevelopmentType("MH");
        return mhDType;
    }

    /**
     * Returns the rrDType.
     * @return DevelopmentTypeInterface
     */
    public static DevelopmentTypeInterface getRRSFDDType() {
    	if (rrsfdDType == null) rrsfdDType = DevelopmentType.getAlreadyCreatedDevelopmentType("RRSFD");
        return rrsfdDType;
    }

    public static DevelopmentTypeInterface getRRMHDType() {
    	if (rrmhDType == null) rrmhDType = DevelopmentType.getAlreadyCreatedDevelopmentType("RRMH");
        return rrmhDType;
    }

    /**
     * Returns the sfdDType.
     * @return DevelopmentTypeInterface
     */
    public static DevelopmentTypeInterface getSFDDType() {
    	if (sfdDType == null) sfdDType = DevelopmentType.getAlreadyCreatedDevelopmentType("SFD");
        return sfdDType;
    }

    /**
     * @param zoneNumber
     * @return
     */
    public double getVacationHomeSizeTerm(int zoneNumber) {
        return vacationHomeSizeTerms[zoneNumber];
    }

}
