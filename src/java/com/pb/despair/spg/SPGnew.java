package com.pb.despair.spg;

import com.pb.common.util.IndexSort;
import com.pb.common.util.Format;
import com.pb.common.util.SeededRandom;
import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.TableDataSet;
import com.pb.despair.model.Halo;
import com.pb.despair.model.IncomeSize;
import com.pb.despair.model.Occupation;
import com.pb.despair.spg.EdIndustry;

import java.util.*;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;


import java.util.logging.Logger;

/**
 * The SPG class is used to produce a set of synthetic households
 * from PUMS data consistent with employment forecasted by ED and
 * assigned to alpha zones.
 * 
 * The procedure is implemented in 2 parts:
 * SPG1 - extract a set of PUMS households consistent with ED employment forecasts
 * SPG2 - using output from PI, allocate households to alpha zones 
 *
 */

public class SPGnew {

    protected static Logger logger = Logger.getLogger("com.pb.despair.spg");

	static final int HH_SELECTED_INDEX = 0;
	static final int HH_OVERFLOW_INDEX = 1;
	static final int HH_UNEMPLOYED_INDEX = 2;
	static final int HH_ALLOCATED_INDEX = 3;
	static final int HHID_INDEX = PUMSData.HHID_INDEX;
	static final int STATE_ATTRIB_INDEX = PUMSData.STATE_INDEX;
	static final int PUMA_ATTRIB_INDEX = PUMSData.PUMA_INDEX;
	static final int NUM_PERSONS_ATTRIB_INDEX = PUMSData.HHSIZE_INDEX;
	static final int HH_INCOME_ATTRIB_INDEX = PUMSData.HHINC_INDEX;
	static final int HH_WEIGHT_ATTRIB_INDEX = PUMSData.HHWT_INDEX;
	static final int NUM_WORKERS_ATTRIB_INDEX = PUMSData.HHWRKRS_INDEX;
	static final int PERSON_ARRAY_ATTRIB_INDEX = PUMSData.PERSON_ARRAY_INDEX;

	static final double MAXIMUM_ALLOWED_CONTROL_DIFFERENCE = 1.0;
	
	// person attributes for person j:
	// industry: PERSON_ARRAY_ATTRIB_INDEX + j*3 + 0
	// occup:    PERSON_ARRAY_ATTRIB_INDEX + j*3 + 1
	// employed: PERSON_ARRAY_ATTRIB_INDEX + j*3 + 2

	
	
	HashMap propertyMap;
	
	Halo halo = null;
	EdIndustry edInd = null;
	Workers workers = null;
	Occupation occ = null;	
	IncomeSize incSize = null;
	
	
	int[][][] hhArray = null;
	int[][] hhSelectedIndexArray = null;
	int[][] hhUnemployedIndexArray = null;
	
	int[] householdsByIncomeSize = null;
	double[][] regionLaborDollars = null;	

	
	float[] tempEmploymentTargets = null;
	int[] employmentTargets = null;
	int[] hhWorkerTargets = null;
	
	double[][] observedEmployment = null;
	double[][] observedHhWorkers = null;
	double[][] employmentControls = null;
	double[][] hhWorkerControls = null;
	double[] employmentControlTotals = null;
	double[] hhWorkerControlTotals = null;
	
	double[] hhWeights = null;

	int balancingCount = 0;
	
	
    public SPGnew () {
		propertyMap = ResourceUtil.getResourceBundleAsHashMap("spg");
        spgInit ();
    }

    public SPGnew ( String propertyFileName ) {
		propertyMap = ResourceUtil.getResourceBundleAsHashMap( propertyFileName );
        spgInit ();
    }

    public SPGnew (ResourceBundle rb) {
        propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        spgInit ();
    }

    private void spgInit () {
    	
		SeededRandom.setSeed( 0 );

		halo = new Halo( (String)propertyMap.get("zoneIndex.fileName") );
		
		edInd = new EdIndustry();
		workers = new Workers();
		occ = new Occupation();
		incSize = new IncomeSize();


    }


    // SPG1 - Select households randomly from Pums records in halo.  Keep track of which
    // industry category persons in household belong to.  When enough households have been
    // selected such that the frequency of selected person industry codes matches the
    // employment totals provided by ED, we're done.
	public void spg1 () {
        
		logger.info ("Start of SPG1\n");
        
		int employedPersonsInHH = 0;
		int employedPersonsInEmployedHhZeroIndustry = 0;
		int employedPersonsInEmployedHhNonZeroIndustry = 0;
		int employedPersonsInEmployedHh = 0;
		int unemployedPersonsInEmployedHh = 0;
		int employed = 0;
	    int numSelectedHHs = 0;
	    int numOverflowHHs = 0;
	    int randomDrawCount = 0;
		int unemployedHHs = 0;
		int finalUnemployedHHs = 0;
		int finalPersonsInUnemployedHHs = 0;
		int personsInEmployedHHs = 0;
		int employedHHs = 0;
	    int edCategoriesNotFilled = 0;
	    int totalEmployedHouseholds;
		int pumsIndustryCode;
		int pumsOccupationCode;
		int edIndustryCode;
		int pumsIncomeCode;
        int incomeSizeCode;
        int workersCode;
		int[] hhAttribs = null;

		double totalWorkers = 0.0;
		double totalEmployees = 0.0;
		
		

		
		// get the array of regional employment targets which must be matched by workers in 
		// selected households.
		tempEmploymentTargets = edInd.getRegionalIndustryEmployment( (String)propertyMap.get("ed.employment.fileName") );
		employmentTargets = new int[tempEmploymentTargets.length];
		double remainder = 0.0;
		for (int i=0; i < tempEmploymentTargets.length; i++) {
			remainder += ( tempEmploymentTargets[i] - (int)tempEmploymentTargets[i] );
			employmentTargets[i] = (int)tempEmploymentTargets[i];
			if ( remainder >= 1.0 ) {
				employmentTargets[i] += 1;
				remainder -= 1.0;
			}
			totalEmployees += employmentTargets[i];
		}
		if (remainder >= 0.5) {
			employmentTargets[tempEmploymentTargets.length-2] ++; // if remainder > 0.5, add extra employee to last actual employment category (length-2).
			totalEmployees ++;
		}

		
		
		// read the number of households in each workers per household category.
        logger.info("Worker Marginal File Path" + (String)propertyMap.get("workers.marginal.fileName"));
		hhWorkerTargets = workers.getWorkersPerHousehold( (String)propertyMap.get("workers.marginal.fileName") );
		
		// adjust the hh worker targets to be consistent with total employment
		// calculate the proportions of workers in each hh worker category and the factor to determine number of households
		for (int i=0; i < hhWorkerTargets.length; i++)
			totalWorkers += hhWorkerTargets[i];

		double[] workerProportions = new double[hhWorkerTargets.length];
		double hhFactor = 0.0f;
		for (int i=0; i < hhWorkerTargets.length; i++) {
			workerProportions[i] = hhWorkerTargets[i]/totalWorkers;
			hhFactor += i*workerProportions[i];
		}
		
		// divide total employment by the hhFactor to get number of households consistent with specified employment and hh worker distribution
		double totalHouseholds = (int)(totalEmployees/hhFactor + 0.5);
		
		// calculate the new set of hh worker targets from these proportions and number of households
		totalWorkers = 0.0;
		remainder = 0.0;
		for (int i=0; i < hhWorkerTargets.length; i++) {
			remainder += ( workerProportions[i]*totalHouseholds - ((int)(workerProportions[i]*totalHouseholds)) );
			hhWorkerTargets[i] = ((int)(workerProportions[i] * totalHouseholds));
			if ( remainder >= 1.0 ) {
				hhWorkerTargets[i] += 1;
				remainder -= 1.0;
			}
			totalWorkers += i*hhWorkerTargets[i];
		}
		if (remainder >= 0.5) {
			hhWorkerTargets[hhWorkerTargets.length-1] += 1;
			totalWorkers += (hhWorkerTargets.length-1);
		}
		
		// adjust targets so that number of workers in employment categories match number of workers in workers per household categories
		int diff = ((int)(totalEmployees - totalWorkers));
		if ( diff > 0 )
			hhWorkerTargets[diff]++;
		else if ( diff < 0 )
			hhWorkerTargets[-diff]--;

		// check adjustment
		totalWorkers = 0.0;
		for (int i=0; i < hhWorkerTargets.length; i++)
			totalWorkers += i*hhWorkerTargets[i];
		
		
		
		logger.info ( "total employment from ED = " + Format.print(" %-10.0f", totalEmployees ) );
		logger.info ( "total workers in final hh workers targets = " + Format.print(" %-10.0f", totalWorkers ) );
		
		// count the total number of unique PUMS household records
		int numHouseholds = getTotalPumsHouseholds();
		int numWorkers = getTotalWorkers();

		// set the dimensions for the control arrays, then initialize their
		initializeTableBalancingArrays( numHouseholds, numWorkers, employmentTargets );
		

		
		
		// iterate over updating balancing factors, and updating balancing arrays
		// when the convergence criteria is met, updateTableBalancingArrays() will return false 
		while ( updateTableBalancingArrays() ) {
		}
		
		
		
		// write comparison of final employment control totals vs control targets to logger
		float targetsTotal = 0.0f;
		float modelTotal = 0.0f;
		String[] categoryLabels = edInd.getEdIndustryLabels();
		logger.info(Format.print("%-45s", "Employment Categories") + "  " +	Format.print("%12s", "Index") + "  " + Format.print("%18s", "Targets") + "  " + Format.print("%18s", "Workers" ) );
		for (int i=0; i < employmentTargets.length; i++) {
			String label = categoryLabels[i];
			logger.info( Format.print("%-45s", label) + "  " + Format.print("%12d", i) + "  " + Format.print("%18d", employmentTargets[i] ) + "  " + Format.print("%18.1f", employmentControlTotals[i] ) );
			targetsTotal += employmentTargets[i];
			modelTotal += employmentControlTotals[i];
		}
		logger.info(Format.print("%-45s", "Total") + "  " +	Format.print("%12s", " ") + "  " + Format.print("%18.0f", targetsTotal ) + "  " + Format.print("%18.1f\n\n\n", modelTotal ) );

		
		// write comparison of final hh worker control totals vs control targets to logger
		targetsTotal = 0.0f;
		modelTotal = 0.0f;
		categoryLabels = workers.getWorkersLabels();
		logger.info(Format.print("%-45s", "HH Worker Categories") + "  " +	Format.print("%12s", "Index") + "  " + Format.print("%18s", "Targets") + "  " + Format.print("%18s", "Households" ) );
		for (int i=0; i < hhWorkerTargets.length; i++) {
			String label = categoryLabels[i];
			logger.info( Format.print("%-45s", label) + "  " + Format.print("%12d", i) + "  " + Format.print("%18d", hhWorkerTargets[i] ) + "  " + Format.print("%18.1f", hhWorkerControlTotals[i] ) );
			targetsTotal += hhWorkerTargets[i];
			modelTotal += hhWorkerControlTotals[i];
		}
		logger.info(Format.print("%-45s", "Total") + "  " +	Format.print("%12s", " ") + "  " + Format.print("%18.0f", targetsTotal ) + "  " + Format.print("%18.1f\n\n\n", modelTotal ) );

		
		
		
		// update hhArray data structure with the final numbers of selected employed and unemployed hhs
		int hhid = 0;
		double[] buckets = new double[observedHhWorkers[0].length];
		for (int i=0; i < hhArray.length; i++) {
		    
			for (int k=0; k < hhArray[i].length; k++) {
		
				numWorkers = hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				workersCode = workers.getWorkers(numWorkers); 

				// check the observed zero workers per household category to see if the PUMS household is unemployed or not
				// if so, the integerized final hhWeight is the number of times this unemployed hh is selected.
				if ( workersCode == 0 ) {
					hhArray[i][k][HH_SELECTED_INDEX] = 0;
					buckets[0] += hhWeights[hhid] - (int)hhWeights[hhid];
					hhArray[i][k][HH_UNEMPLOYED_INDEX] = (int)hhWeights[hhid];
					if ( buckets[0] >= 1.0 ) {
						hhArray[i][k][HH_UNEMPLOYED_INDEX] += 1;
						buckets[0] -= 1.0;
					}
				}
				// else, the integerized final hhWeight is the number of times this employed hh is selected.
				else {
					hhArray[i][k][HH_UNEMPLOYED_INDEX] = 0;
					buckets[workersCode] += hhWeights[hhid] - (int)hhWeights[hhid];
					hhArray[i][k][HH_SELECTED_INDEX] = (int)hhWeights[hhid];
					if ( buckets[workersCode] >= 1.0 ) {
						hhArray[i][k][HH_SELECTED_INDEX] += 1;
						buckets[workersCode] -= 1.0;
					}
				}
				
				hhid++;
			        
			}

		}

		
		
		
		
		// count up the final numbers of selected employed and unemployed hhs and persons
		logger.info ("summarizing households and persons from synthetic population.");
		for (int i=0; i < hhArray.length; i++) {
		    
			for (int k=0; k < hhArray[i].length; k++) {
		
				if ( hhArray[i][k][HH_UNEMPLOYED_INDEX] > 0 ) {
					finalUnemployedHHs += hhArray[i][k][HH_UNEMPLOYED_INDEX];
					finalPersonsInUnemployedHHs += hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX]*hhArray[i][k][HH_UNEMPLOYED_INDEX];
				}
				else {
					employedHHs += hhArray[i][k][HH_SELECTED_INDEX];
					personsInEmployedHHs += hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX]*hhArray[i][k][HH_SELECTED_INDEX];
				}
			        
			}
		    
		}

		
		
		logger.info ("writing hhArray");
		writeHhArray ();



		
		// get the regional number of employed persons by industry and occupation in hhArray from SPG1
		int[][] indJobs = new int[4][edInd.getNumberEdIndustries()];
		int[][] occJobs = new int[4][occ.getNumberOccupations()];
		int[][] hhIncSizes = new int[2][incSize.getNumberIncomeSizes()];
		int[][] hhWorkers = new int[2][workers.getNumberWorkerCategories()];
		for (int i=0; i < hhArray.length; i++) {
			for (int k=0; k < hhArray[i].length; k++) {

			    int hhSize = hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX];
				pumsIncomeCode = hhArray[i][k][HH_INCOME_ATTRIB_INDEX];

				incomeSizeCode = incSize.getIncomeSize(pumsIncomeCode, hhSize); 
				hhIncSizes[1][incomeSizeCode] += hhArray[i][k][HH_SELECTED_INDEX];
				hhIncSizes[0][incomeSizeCode] += hhArray[i][k][HH_UNEMPLOYED_INDEX];

				workersCode = hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				hhWorkers[1][workersCode] += hhArray[i][k][HH_SELECTED_INDEX];
				hhWorkers[0][workersCode] += hhArray[i][k][HH_UNEMPLOYED_INDEX];
				
				for (int j=0; j < hhSize; j++) {

				    pumsIndustryCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 0];
					pumsOccupationCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 1];
			        employed = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 2];

			        int personIndustry = edInd.getEdIndustry(pumsIndustryCode); 
					int personOccupation = occ.getOccupation(pumsOccupationCode); 
					indJobs[employed][personIndustry] += hhArray[i][k][HH_SELECTED_INDEX];
					occJobs[employed][personOccupation] += hhArray[i][k][HH_SELECTED_INDEX];
					indJobs[employed+2][personIndustry] += hhArray[i][k][HH_UNEMPLOYED_INDEX];
					occJobs[employed+2][personOccupation] += hhArray[i][k][HH_UNEMPLOYED_INDEX];

				}
			}
		}
		

		
		// write frequency tables of unemployed and employed persons in employed households by industry and occup.
		logger.info ("");
		writeFreqSummaryToLogger ( "employed persons in employed households by industry category", "INDUSTRY", edInd.getEdIndustryLabels(), indJobs[1] );		
		writeFreqSummaryToLogger ( "unemployed persons in employed households by industry category", "INDUSTRY", edInd.getEdIndustryLabels(), indJobs[0] );		

		logger.info ("");
		writeFreqSummaryToLogger ( "employed persons in employed households by occupation category", "OCCUPATION", occ.getOccupationLabels(), occJobs[1] );		
		writeFreqSummaryToLogger ( "unemployed persons in employed households by occupation category", "OCCUPATION", occ.getOccupationLabels(), occJobs[0] );		
		

		
		// write frequency tables of unemployed and employed persons in unemployed households by industry and occup.
		logger.info ("");
		writeFreqSummaryToLogger ( "employed persons in unemployed households by industry category", "INDUSTRY", edInd.getEdIndustryLabels(), indJobs[3] );		
		writeFreqSummaryToLogger ( "unemployed persons in unemployed households by industry category", "INDUSTRY", edInd.getEdIndustryLabels(), indJobs[2] );		

		logger.info ("");
		writeFreqSummaryToLogger ( "employed persons in unemployed households by occupation category", "OCCUPATION", occ.getOccupationLabels(), occJobs[3] );		
		writeFreqSummaryToLogger ( "unemployed persons in unemployed households by occupation category", "OCCUPATION", occ.getOccupationLabels(), occJobs[2] );		
		

		
		// write frequency tables of employed and unemployed households by hh category.
		logger.info ("");
		writeFreqSummaryToLogger ( "employed households by household category", "HH_INCOME_SIZE", incSize.getIncomeSizeLabels(), hhIncSizes[1] );		
		writeFreqSummaryToLogger ( "unemployed households by household category", "HH_INCOME_SIZE", incSize.getIncomeSizeLabels(), hhIncSizes[0] );		

		logger.info ("");
		writeFreqSummaryToLogger ( "employed households by Number of Workers in Household", "HH_WORKERS", workers.getWorkersLabels(), hhWorkers[1] );		
		writeFreqSummaryToLogger ( "unemployed households by Number of Workers in Household", "HH_WORKERS", workers.getWorkersLabels(), hhWorkers[0] );		
		

		
	
		
		logger.info ("summarizing and writing frequency tables by state");
		writeFrequencyTables();
		
		
		
		logger.info ( "" );
		logger.info ("end of spg1");
	}
    

	

	public void spg2 () {
	    
		logger.info ("Start of SPG2\n");
        long time = System.currentTimeMillis();
		PrintWriter hhOutStream = null;
		PrintWriter personOutStream = null;

		int[] indexZone = halo.getIndexZone();
		int[] zoneIndex = halo.getZoneIndex();


		int numOccupations = occ.getNumberOccupations();
		int numIncomeSizes = incSize.getNumberIncomeSizes();

		int[] hhsByZoneIndex = new int[indexZone.length+1];

		double[][] regionJobs = new double[numOccupations][numIncomeSizes];
		int[][] regionJobsAllocated = new int[numOccupations][numIncomeSizes];
		regionLaborDollars = new double[numOccupations][numIncomeSizes];
		double[][] regionLaborDollarsPerJob = new double[numOccupations][numIncomeSizes];
		int[] hhArrayHhsIncomeSize = new int[numIncomeSizes];
		int[] regionalHhsIncomeSize = new int[numIncomeSizes];
		int[] regionalHhsIncomeSizeAllocated = new int[numIncomeSizes];
		int[][] hhsIncomeSizeAllocated = new int[indexZone.length][numIncomeSizes];
		int[][] hhsIncomeSizePI = null;
		double[][][] laborDollarsPI = null;

		double[] laborDollarDensity = new double[indexZone.length];
		double[] probabilities = new double[indexZone.length];
		
		int[] hhAttribs = null;
		int occup = 0;
		int hhIndex;
		int hhSize = 0;
		int hhIncome = 0;
		int incomeSize = 0;
		int pumsOccupationCode = 0;
		int personOccupation = 0;
		int selectedIndex = 0;
		int selectedZone = 0;
		int count;
		
		int unallocatedHHs = 0; 
		int unallocatedRegionalHHs = 0; 
		double reduction = 0.0;
		
		String[] values = null;

		int employedCount = 0;
		int unemployedCount = 0;
		int employedCount0 = 0;
		int unemployedCount0 = 0;
		int employedZone0 = 0;		
		int unemployedZone0 = 0;		

		
		
		readHhArray();

		
		// count the total number of household records
		int householdsAllocated = 0;
		int numHouseholds = getSelectedHouseholdsIndexArray();



		// create an array of hh indices (0,...,number of regional households - 1)
		// sorted randomly to control the order in which households are allocated zones
		int[] tempData = new int[numHouseholds];
		for (int i=0; i < numHouseholds; i++)
			tempData[i] = (int)(SeededRandom.getRandom()*100000000);

		int[] hhOrder = IndexSort.indexSort(tempData);
		tempData = null;



		// read the input files produced by PI which runs after SPG1
		hhsIncomeSizePI = readPiIncomeSizeHHs ( (String)propertyMap.get("pi.hhCategory.fileName") );		
		laborDollarsPI = readPiLaborDollars ( (String)propertyMap.get("pi.laborDollarProduction.fileName") );
		
		

		// get the zonal array of hhs from PI - for checking purposes
		// and for allocating zones when SPG2 densities are all zero.
		for (int i=0; i < hhsIncomeSizePI.length; i++) {
			for (int j=0; j < hhsIncomeSizePI[i].length; j++) {
				hhsByZoneIndex[i] += hhsIncomeSizePI[i][j];
				regionalHhsIncomeSize[j] += hhsIncomeSizePI[i][j]; 
			}
		}
		
		
		// get the regional number of employed persons by occupation from SPG1
		int cumHHs = 0;
		count = 1;
		for (int i=0; i < hhArray.length; i++) {
			for (int k=0; k < hhArray[i].length; k++) {
				hhIndex = hhArray[i][k][HHID_INDEX];
				hhSize = hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX];
				hhIncome = hhArray[i][k][HH_INCOME_ATTRIB_INDEX];
				incomeSize = incSize.getIncomeSize(hhIncome, hhSize);
				hhArrayHhsIncomeSize[incomeSize] += hhArray[i][k][HH_SELECTED_INDEX]; 
				for (int j=0; j < hhSize; j++) {
					pumsOccupationCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 1];
					personOccupation = occ.getOccupation(pumsOccupationCode); 
					regionJobs[personOccupation][incomeSize] += hhArray[i][k][HH_SELECTED_INDEX];
				}
			}
		}
		

		// get the regional mean labor$ per job to use in decrementing labor$ as households are allocated to zones.
		logger.info ("");
		for (int i=0; i < regionLaborDollars.length; i++) {
			for (int j=0; j < regionLaborDollars[i].length; j++) {
			    regionLaborDollarsPerJob[i][j] = regionLaborDollars[i][j]/regionJobs[i][j];
			    logger.info ( "occup=" + occ.getOccupationLabel(i) + "   incSize=" + incSize.getIncomeSizeLabel(j) + "   laborDollars=" + regionLaborDollars[i][j]+ "   jobs=" + regionJobs[i][j] + "   dollarsPerJob=" +  regionLaborDollarsPerJob[i][j] );
			}
		}
		logger.info ("");



		



		String fileName = (String)propertyMap.get("spg.hhRecordList.fileName");
		try {
			hhOutStream = new PrintWriter(new BufferedWriter(
				new FileWriter( fileName )));

			//write household file header record
			String[] labels = { "hhid", "state", "zone" };
			values = new String[labels.length];
			writePumsData(hhOutStream, labels);

		}
		catch (IOException e) {
			logger.severe("IO Exception when opening synthetic household file: " + fileName );
		}


		
		// go through the list of households randomly and allocate them to zones.
		for (int h=0; h < hhOrder.length; h++) {

		
		    if (h % 100000 == 0)
		        logger.info ("household " + h);
		    
		    
			// get the indices into the hhArray for a household drawn at random
			int[] stateAndHouseholdIndices = getHouseholdIndices ( hhOrder[h] );

			// get the hh attributes
			hhAttribs = hhArray[stateAndHouseholdIndices[0]][stateAndHouseholdIndices[1]];
			hhIndex = hhAttribs[HHID_INDEX];
			hhSize = hhAttribs[NUM_PERSONS_ATTRIB_INDEX];
			hhIncome = hhAttribs[HH_INCOME_ATTRIB_INDEX];
				
			incomeSize = incSize.getIncomeSize(hhIncome, hhSize);
			

		    // if household has any employeed persons, use labor$ for those occupation categories to allocate hh to alpha zone
			if ( hhAttribs[NUM_WORKERS_ATTRIB_INDEX] > 0 ) {
		        
				// get the occupations of each person in this household
				int[] personOccupations = new int[hhSize];
				for (int i=0; i < hhSize; i++) {
				    
					pumsOccupationCode = hhAttribs[PERSON_ARRAY_ATTRIB_INDEX + i*3 + 1];
					personOccupations[i] = occ.getOccupation(pumsOccupationCode); 

				}


				
				// loop over alpha zones and compute total regional unallocated households
				unallocatedRegionalHHs = 0;
				for (int i=0; i < indexZone.length; i++) {
					laborDollarDensity[i] = 0.0;
					unallocatedRegionalHHs += (hhsIncomeSizePI[i][incomeSize] - hhsIncomeSizeAllocated[i][incomeSize]);
				}

				
				if ( unallocatedRegionalHHs > 0 ) {
				    
					// loop over alpha zones and compute zone selection densities from
					// total unallocated households in the zones within the household category
					for (int i=0; i < indexZone.length; i++) {
						// determine zonal density and total density
						unallocatedHHs = (hhsIncomeSizePI[i][incomeSize] - hhsIncomeSizeAllocated[i][incomeSize]);
						laborDollarDensity[i] = (double)unallocatedHHs/unallocatedRegionalHHs;
					}
					
				}
				
				
				// loop over alpha zones and compute zone selection densities from labor$ by person occupations in the zones
				for (int i=0; i < indexZone.length; i++) {

				    // determine zonal density over employed persons in hh.
					for (int j=0; j < hhSize; j++) {
						if (personOccupations[j] > 0) {
						    if ( laborDollarsPI[i][personOccupations[j]][incomeSize] > 0.0 ) {
						        laborDollarDensity[i] *= ( laborDollarsPI[i][personOccupations[j]][incomeSize]/regionLaborDollars[personOccupations[j]][incomeSize] );
						    }
						    else {
						        laborDollarDensity[i] = 0.0;
						        break;
						    }
						}
					}

				}

				
				// convert zonal densities into zone selection probabilities
				double totalDensity = 0.0;
				for (int i=0; i < indexZone.length; i++) {

					// if all hhs for a income/size category in a zone are allocated,
				    // or the labor$ for the occupations of any of the workers < 0,
				    // make the selection probabilities 0.
					if ( hhsIncomeSizeAllocated[i][incomeSize] < hhsIncomeSizePI[i][incomeSize] &&
					    laborDollarDensity[i] > 0.0 ) {
							probabilities[i] = laborDollarDensity[i];
							totalDensity += laborDollarDensity[i]; 
					}
					else {
						probabilities[i] = 0.0;
					}
 
				} 

				
				// if the sum over all probabilities is > 0, select a zone
				if ( totalDensity > 0.0 ) {

				    for (int i=0; i < indexZone.length; i++)
				        probabilities[i] /= totalDensity;
				    
					// get the selected zone from the probabilities
					selectedIndex = getMonteCarloSelection(probabilities);
					selectedZone = indexZone[selectedIndex];

					
					// reduce labor$ in selected zone in each person's occupation category by mean labor$/job.
					for (int i=0; i < hhSize; i++) {
						occup = personOccupations[i];
						if (occup > 0) {
						    if ( laborDollarsPI[selectedIndex][occup][incomeSize] < regionLaborDollarsPerJob[occup][incomeSize] )
						    	reduction = laborDollarsPI[selectedIndex][occup][incomeSize];
						    else
						        reduction = regionLaborDollarsPerJob[occup][incomeSize];
							laborDollarsPI[selectedIndex][occup][incomeSize] -= reduction;
							
							regionLaborDollars[occup][incomeSize] -= reduction;
							regionJobsAllocated[occup][incomeSize] ++;
						}
					}
				
					// increase number of hhs allocated in zone and income/size category
					hhsIncomeSizeAllocated[selectedIndex][incomeSize] ++;
					regionalHhsIncomeSizeAllocated[incomeSize] ++;
				
					employedCount++;
					
				}
				else {

					// if totalDensity is zero, meaning all zones with remaining $Labor
				    // have already been fully allocated employed households,
					// allocate this household to a zone based on distribution
					// of total number of households per zone
					totalDensity = 0.0;
					for (int i=0; i < indexZone.length; i++) {
						probabilities[i] = hhsByZoneIndex[i];
						totalDensity += probabilities[i];
					} 
				    
					employedCount0++;

					
					// if the sum over all probabilities is > 0, select a zone
					if ( totalDensity > 0.0 ) {

						for (int i=0; i < indexZone.length; i++)
							probabilities[i] /= totalDensity;
				    
						// get the selected zone from the probabilities
						selectedIndex = getMonteCarloSelection(probabilities);
						selectedZone = indexZone[selectedIndex];

					
						// reduce labor$ in selected zone in each person's occupation category by mean labor$/job.
						for (int i=0; i < hhSize; i++) {
							occup = personOccupations[i];
							if (occup > 0) {
								if ( laborDollarsPI[selectedIndex][occup][incomeSize] < regionLaborDollarsPerJob[occup][incomeSize] )
									reduction = laborDollarsPI[selectedIndex][occup][incomeSize];
								else
									reduction = regionLaborDollarsPerJob[occup][incomeSize];
								laborDollarsPI[selectedIndex][occup][incomeSize] -= reduction;
							
								regionLaborDollars[occup][incomeSize] -= reduction;
								regionJobsAllocated[occup][incomeSize] ++;
							}
						}
				
						// increase number of hhs allocated in zone and income/size category
						hhsIncomeSizeAllocated[selectedIndex][incomeSize] ++;
						regionalHhsIncomeSizeAllocated[incomeSize] ++;
				
					}
					// otherwise, set the zone to 0 so we can determine how often this happens
					else {
					
					    selectedZone = 0;
					    employedZone0 ++;
					
						/*
						logger.info ("employed hh=" + h + ", incomeSize=" + incomeSize + ", regionalHhsIncomeSize[incomeSize]=" + regionalHhsIncomeSize[incomeSize] + ", regionalHhsIncomeSizeAllocated[incomeSize]=" + regionalHhsIncomeSizeAllocated[incomeSize] );
						for (int i=0; i < hhSize; i++) {
						    occup = personOccupations[i];
						    logger.info ("occup=" + occup + ", regionLaborDollars[occup][incomeSize]=" + regionLaborDollars[occup][incomeSize] + ", regionJobsAllocated[occup][incomeSize]=" + regionJobsAllocated[occup][incomeSize] );
						}
						
						for (int i=0; i < indexZone.length; i++)
						    if ( hhsIncomeSizePI[i][incomeSize] > hhsIncomeSizeAllocated[i][incomeSize] )
						        logger.info ( "h=" + h + ", i=" + i + ", zone=" + indexZone[i] + ", hhsIncomeSizeAllocated[i][incomeSize]=" + hhsIncomeSizeAllocated[i][incomeSize] + ", hhsIncomeSizePI[i][incomeSize]=" + hhsIncomeSizePI[i][incomeSize] + ", laborDollarDensity[i]=" + laborDollarDensity[i] );
								
						System.exit(1);
						*/

					}
					
				}

				

				
				// save the selected zone with the pums state and record number to a file.
				values[0] = Integer.toString( hhAttribs[HHID_INDEX] );
				values[1] = Integer.toString( stateAndHouseholdIndices[0] );
				values[2] = Integer.toString( selectedZone );
				writePumsData(hhOutStream, values);

				
			}
			else {

				double totalDensity;
				
				// this was an unemployed hh

				// loop over alpha zones and compute total regional unallocated households
				unallocatedRegionalHHs = 0;
				for (int i=0; i < indexZone.length; i++) {
				    unallocatedRegionalHHs += (hhsIncomeSizePI[i][incomeSize] - hhsIncomeSizeAllocated[i][incomeSize]);
				}

				
				if ( unallocatedRegionalHHs > 0 ) {
				    
					// loop over alpha zones and compute zone selection densities from
					// total unallocated households in the zones within the household category
					for (int i=0; i < indexZone.length; i++) {
						// determine zonal density and total density
						unallocatedHHs = (hhsIncomeSizePI[i][incomeSize] - hhsIncomeSizeAllocated[i][incomeSize]);
						laborDollarDensity[i] = (double)unallocatedHHs/unallocatedRegionalHHs;
					}

				
					// convert zonal densities into zone selection probabilities
					totalDensity = 0.0;
					for (int i=0; i < indexZone.length; i++) {

						// if all hhs for a income/size category in a zone are allocated,
						// or the labor$ for the occupations of any of the workers <= 0,
						// make the selection probabilities 0.
						if ( hhsIncomeSizeAllocated[i][incomeSize] < hhsIncomeSizePI[i][incomeSize] &&
							laborDollarDensity[i] > 0.0 ) {
							probabilities[i] = laborDollarDensity[i];
							totalDensity += laborDollarDensity[i];
						}
						else {
							probabilities[i] = 0.0;
						}
 
					} 
				    
					unemployedCount++;

				}
				else {
				    
					// if all zones have been fully allocated employed households,
				    // allocate this unemployed household to a zone based on distribution
				    // of total number of households per zone
					totalDensity = 0.0;
					for (int i=0; i < indexZone.length; i++) {
						probabilities[i] = hhsByZoneIndex[i];
						totalDensity += probabilities[i];
					} 
				    
					unemployedCount0++;
				}
				

				// if the sum over all probabilities is > 0, select a zone
				if ( totalDensity > 0.0 ) {

					for (int i=0; i < indexZone.length; i++)
						probabilities[i] /= totalDensity;
				    
					// get the selected zone from the probabilities
					selectedIndex = getMonteCarloSelection(probabilities);
					selectedZone = indexZone[selectedIndex];

					
					// increase number of hhs allocated in zone and income/size category
					//hhsIncomeSizeAllocated[selectedIndex][incomeSize]++;
				
				}
				// otherwise, set the zone to 0 so we can determine how often this happens
				else {

					selectedZone = 0;
					unemployedZone0 ++;
					
					/*
					logger.info ("non-employed hh=" + h + ", incomeSize=" + incomeSize + ", regionalHhsIncomeSize[incomeSize]=" + regionalHhsIncomeSize[incomeSize] + ", regionalHhsIncomeSizeAllocated[incomeSize]=" + regionalHhsIncomeSizeAllocated[incomeSize]);
					
					for (int i=0; i < indexZone.length; i++)
						if ( hhsIncomeSizePI[i][incomeSize] > hhsIncomeSizeAllocated[i][incomeSize] )
							logger.info ( "h=" + h + ", i=" + i + ", zone=" + indexZone[i] + ", hhsIncomeSizeAllocated[i][incomeSize]=" + hhsIncomeSizeAllocated[i][incomeSize] + ", hhsIncomeSizePI[i][incomeSize]=" + hhsIncomeSizePI[i][incomeSize] + ", laborDollarDensity[i]=" + laborDollarDensity[i] );
					*/		
					
				}

				
				// save the selected zone with the pums state and record number to a file.
				values[0] = Integer.toString( hhAttribs[HHID_INDEX] );
				values[1] = Integer.toString( stateAndHouseholdIndices[0] );
				values[2] = Integer.toString( selectedZone );
				writePumsData(hhOutStream, values);

				
			}

		}
		
		

        try {

			hhOutStream.close();

        }
		catch (Exception e) {
            logger.severe("Exception when closing synthetic population files.");
        }


		logger.info ( employedCount + " households with at least one employee assigned a zone based on zonal distribution of $Labor density.");
		logger.info ( unemployedCount + " households with no employees assigned a zone based on zonal distribution of $Labor density.");
		
		logger.info ( employedCount0 + " households with at least one employee assigned a zone based on zonal distribution of total households.");
		logger.info ( unemployedCount0 + " households with no employees assigned a zone based on zonal distribution of total households.");
		
		logger.info ( employedZone0 + " households with at least one employee assigned zone 0.");
		logger.info ( unemployedZone0 + " households with no employees assigned zone 0.");

        logger.info("SPG2 runtime: " + (System.currentTimeMillis()-time)/1000.0);
	}
	

	
	// write the contents of the hhArray array computed in SPG1 to disk so it can read back in
	// and used in SPG2.
	private void writeHhArray () {
	    
		try {
			FileOutputStream out = new FileOutputStream( (String)propertyMap.get("spg.hhDiskObject.fileName") );
			ObjectOutputStream s = new ObjectOutputStream(out);
			s.writeObject(hhArray);
			s.flush();
		}
		catch (IOException e) {
			logger.severe("IO Exception when writing hhArray file: " + (String)propertyMap.get("spg.hhDiskObject.fileName") );
		}

	}
	
	

	// read the hhArray array computed in SPG1 so it can be used in SPG2.
	private void readHhArray () {

		try{
			FileInputStream in = new FileInputStream( (String)propertyMap.get("spg.hhDiskObject.fileName") );
			ObjectInputStream s = new ObjectInputStream(in);
			hhArray = (int[][][])s.readObject();
		}catch(IOException e){
			logger.severe("IO Exception when writing hhArray file: " + (String)propertyMap.get("spg.hhDiskObject.fileName") );
		}catch(ClassNotFoundException e){
			logger.severe("Class Not Found Exception when writing hhArray file: " + (String)propertyMap.get("spg.hhDiskObject.fileName") );
		}

	}
	
	

	// draw a household at random from the total number of hhs.  
	// for this random hh index, determine which state and hh index
	// within states to which it refers and return those two values.
	private int[] getRandomHouseholdIndices ( int numHouseholds ) {
	
		int[] returnValues = new int[2];

	    int randomIndex = (int)(SeededRandom.getRandom()*numHouseholds);
	    
	    int cumulativeTotalHHs = 0;
	    for (int i=0; i < hhArray.length; i++) {
	        if (randomIndex < cumulativeTotalHHs + hhArray[i].length) {
				returnValues[0] = i;
				returnValues[1] = randomIndex - cumulativeTotalHHs;
	            break;
	        }
			cumulativeTotalHHs += hhArray[i].length;
	    }

	    return returnValues;
	}
	

	
	// for a household index selected at random from the total number of hhs,  
	// determine the state and hh index within states to which it refers and
	// return those two values which serve as indices into hhArray[][][].
	private int[] getHouseholdIndices ( int hhIndex ) {
	
		int[] returnValues = new int[2];

	    for (int i=0; i < hhArray.length; i++) {

			if ( hhIndex < hhSelectedIndexArray[i+1][0]) {

			    for (int j=0; j < hhSelectedIndexArray[i].length; j++) {
			        if (hhIndex < hhSelectedIndexArray[i][j]) {
						returnValues[0] = i;
						returnValues[1] = j-1;
					    return returnValues;
			        }
				}

			}

	    }

	    return returnValues;
	}
	

	
	// initialize the sample observed employment values, employment controls, 
	// household controls, and hhWeights.
	private void initializeTableBalancingArrays ( int numPUMSHouseholds, int numPUMSWorkers, int[] employmentTargets ) {

		int hhid = 0;
		int numPersons;
		int numWorkers;
		int workersCode;
		int pumsIndustryCode = 0;
		int edIndustryCode = 0;
		int employmentStatusCode = 0;
		
		int numWorkerCategories = workers.getNumberWorkerCategories();
		int numEmploymentCategories = edInd.getNumberEdIndustries();

		hhWeights = new double[numPUMSHouseholds];

		employmentControlTotals = new double[numEmploymentCategories];
		hhWorkerControlTotals = new double[numWorkerCategories];
		observedHhWorkers = new double[numPUMSHouseholds][numWorkerCategories];
		hhWorkerControls = new double[numPUMSHouseholds][numWorkerCategories];
		observedEmployment = new double[numPUMSHouseholds][numEmploymentCategories];
		employmentControls = new double[numPUMSHouseholds][numEmploymentCategories];

		
		
		// calculate initial hhWeight
		float totalRegionalJobs = 0;
		for (int i=0; i < employmentTargets.length; i++)
			totalRegionalJobs += employmentTargets[i];
		
		float hhWeight = totalRegionalJobs/numPUMSWorkers;
		
		
		
		// loop over state PUMS
		for (int i=0; i < hhArray.length; i++) {

			// loop over household records for this state
			for (int k=0; k < hhArray[i].length; k++) {
			
				numPersons = hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX];
				numWorkers = hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				workersCode = workers.getWorkers(numWorkers); 
		
				// set the initial hhWeights for each hh
				hhWeights[hhid] = hhWeight; 

				// set the fixed observed number of workers in this hh
				observedHhWorkers[hhid][workersCode] = 1;
				hhWorkerControls[hhid][workersCode] = hhWeight;
				hhWorkerControlTotals[workersCode] += hhWeight;

				// loop through person attributes for this household 
				for (int j=0; j < numPersons; j++) {

					// check employment status of person
					employmentStatusCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 2];
					if ( employmentStatusCode > 0 ) {
						
						// set the fixed observed employment value for employment category of this worker
						pumsIndustryCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 0];
						edIndustryCode = edInd.getEdIndustry(pumsIndustryCode);
						observedEmployment[hhid][edIndustryCode] += 1;
						employmentControls[hhid][edIndustryCode] += hhWeight;
						employmentControlTotals[edIndustryCode] += hhWeight;

					}
					
				}
				
				hhid++;

			}
		}
	
	}
	

	
	
	// initialize the sample observed employment values, employment controls, 
	// household controls, and hhWeights.
	private boolean updateTableBalancingArrays ( ) {

		int hhid = 0;
		double balancingFactor = 0.0;
		double oldBalancingFactor = 1000000.0;
		
		
		//loop over each employment control
		for (int i=0; i < employmentControlTotals.length; i++) {
			
			// if marginal total for this control is not positive, skip update
			if ( employmentTargets[i] <= 0.0 || employmentControlTotals[i] <= 0.0 )
				continue;
			
			// calculate new hhWeight for this control
			balancingFactor = employmentTargets[i]/employmentControlTotals[i];
			
			
			// update the hhWeights for any hh with a worker in the current employment category
			for (hhid=0; hhid < observedEmployment.length; hhid++) {
				
				if ( observedEmployment[hhid][i] > 0 )
					hhWeights[hhid] *= balancingFactor;
				
			}

			
			// update all controls based on this new balancing factor
			updateControls();
			
		}
		

	
	
		//loop over each hh worker control
		for (int i=0; i < hhWorkerControlTotals.length; i++) {
			
			// if marginal total for this control is not positive, skip update
			if ( hhWorkerTargets[i] <= 0.0 || hhWorkerControlTotals[i] <= 0.0 )
				continue;
			
			// calculate new hhWeight for this control
			balancingFactor = hhWorkerTargets[i]/hhWorkerControlTotals[i];
			
			
			// update the hhWeights for any hh with a worker in the current employment category
			for (hhid=0; hhid < observedHhWorkers.length; hhid++) {
				
				if ( observedHhWorkers[hhid][i] > 0 )
					hhWeights[hhid] *= balancingFactor;
				
			}

			
			// update all controls based on this new balancing factor
			updateControls();
			
		}
		
		
		
		logger.info( "Balancing Factor Difference for iteration " + balancingCount + " =  " + Format.print("%18.8f", Math.abs( 1.0 - balancingFactor ) ) );
		
		
		
		balancingCount++;
		
		// check for convergence and return
		double maxDiff = 0.0;
		for (int i=0; i < employmentTargets.length; i++) {
			if ( Math.abs(employmentControlTotals[i] - employmentTargets[i]) > maxDiff )
				maxDiff = Math.abs(employmentControlTotals[i] - employmentTargets[i]);
		}
		for (int i=0; i < hhWorkerTargets.length; i++) {
			if ( Math.abs(hhWorkerControlTotals[i] - hhWorkerTargets[i]) > maxDiff )
				maxDiff = Math.abs(hhWorkerControlTotals[i] - hhWorkerTargets[i]);
		}

		int dummy = 0;
		if ( balancingCount > 10 && maxDiff > MAXIMUM_ALLOWED_CONTROL_DIFFERENCE ) {
			dummy = 1;
		}
		
		
		return ( maxDiff > MAXIMUM_ALLOWED_CONTROL_DIFFERENCE );
		
	}
	

	
	
	private void updateControls () {

		// initialize control total arrays
		for (int i=0; i < employmentControlTotals.length; i++)
			employmentControlTotals[i] = 0.0; 

		for (int i=0; i < hhWorkerControlTotals.length; i++)
			hhWorkerControlTotals[i] = 0.0; 

		
		
		// loop over all households
		for (int hhid=0; hhid < observedEmployment.length; hhid++) {

			// update all employment controls for this household
			for (int j=0; j < observedEmployment[hhid].length; j++) {
				employmentControls[hhid][j] = observedEmployment[hhid][j] * hhWeights[hhid];
				employmentControlTotals[j] += employmentControls[hhid][j]; 
			}

			
			// update all hh worker controls for this household
			for (int j=0; j < observedHhWorkers[hhid].length; j++) {
				hhWorkerControls[hhid][j] = observedHhWorkers[hhid][j] * hhWeights[hhid];				
				hhWorkerControlTotals[j] += hhWorkerControls[hhid][j]; 
			}
		
		}

		
	}
			
				
			
    // count the total number of workers in the PUMS sample 
	public int getTotalWorkers () {

		int w;
		int[] workerBins = new int[workers.getNumberWorkerCategories()];
		
		int numWorkers = 0;
		
		for (int i=0; i < hhArray.length; i++) {
			for (int k=0; k < hhArray[i].length; k++) {
				numWorkers += hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				w = hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				if ( w > workers.getNumberWorkerCategories()-1 )
					w = workers.getNumberWorkerCategories()-1;
				workerBins[w]++;
			}
		}
		
		return numWorkers;
	}
	
	
	
	
	// determine whether any ED industry categories have remaining employment
	private boolean employmentRemaining ( float[] edEmployment ) {
	
	    for (int i=0; i < edEmployment.length; i++) {
	        if (edEmployment[i] >= 1)
	            return true;
	    }
	    
	    return false;
	    
	}

	
	
	// determine whether any ED industry categories have been filled
	private boolean employmentFilled ( float[] edEmployment ) {
	
	    for (int i=0; i < edEmployment.length; i++) {
	        if (edEmployment[i] < 1)
	            return true;
	    }
	    
	    return false;
	    
	}

	
	
	// count the number of unique PUMS household records in the hhArray over all states
	private int getTotalPumsHouseholds () {
        
		int numHouseholds = 0;
		for (int i=0; i < hhArray.length; i++)
			numHouseholds += hhArray[i].length;
        
        return numHouseholds;
        
	}
    
    

	// add up the total weights of unique PUMS household records in the hhArray over all states
	private int getTotalWeightedPumsHouseholds () {
        
		int numWiegthedHouseholds = 0;
		for (int i=0; i < hhArray.length; i++) {
			for (int j=0; j < hhArray[i].length; j++) {
			    numWiegthedHouseholds += hhArray[i][j][HH_WEIGHT_ATTRIB_INDEX];
			}
		}
        
        return numWiegthedHouseholds;
        
	}
    
    

	// count the number of selected households in the hhArray
	private int getSelectedHouseholdsIndexArray () {
        
		hhSelectedIndexArray = new int[hhArray.length+1][];
	    
		hhSelectedIndexArray[0] = new int[hhArray[0].length+1];
		hhSelectedIndexArray[0][0] = 0;

		int numHouseholds = 0;
		int totalHouseholds = 0;
		for (int i=0; i < hhArray.length; i++) {
		    
			if ( i > 0 ) {
				hhSelectedIndexArray[i] = new int[hhArray[i].length+1];
				hhSelectedIndexArray[i][0] = hhSelectedIndexArray[i-1][hhArray[i-1].length];
			}

			for (int j=0; j < hhArray[i].length; j++) { 
				numHouseholds = ( hhArray[i][j][HH_SELECTED_INDEX] + hhArray[i][j][HH_UNEMPLOYED_INDEX] );
				hhSelectedIndexArray[i][j+1] = hhSelectedIndexArray[i][j] + numHouseholds;
			    totalHouseholds += numHouseholds;
			}
			
		}

		hhSelectedIndexArray[hhArray.length] = new int[1];
		hhSelectedIndexArray[hhArray.length][0] = hhSelectedIndexArray[hhArray.length-1][hhArray[hhArray.length-1].length];

        
		return totalHouseholds;
        
	}
    
    

	// count the number of unemployed households in the hhArray
	private int getUnemployedHouseholdsIndexArray () {
        
		hhUnemployedIndexArray = new int[hhArray.length+1][];
	    
		hhUnemployedIndexArray[0] = new int[hhArray[0].length+1];
		hhUnemployedIndexArray[0][0] = 0;

		int numHouseholds = 0;
		int totalHouseholds = 0;
		for (int i=0; i < hhArray.length; i++) {
		    
			if ( i > 0 ) {
			    hhUnemployedIndexArray[i] = new int[hhArray[i].length+1];
			    hhUnemployedIndexArray[i][0] = hhUnemployedIndexArray[i-1][hhArray[i-1].length];
			}

			for (int j=0; j < hhArray[i].length; j++) { 
				numHouseholds = ( hhArray[i][j][HH_UNEMPLOYED_INDEX] );
				hhUnemployedIndexArray[i][j+1] = hhUnemployedIndexArray[i][j] + numHouseholds;
			    totalHouseholds += numHouseholds;
			}
			
		}

		hhUnemployedIndexArray[hhArray.length] = new int[1];
		hhUnemployedIndexArray[hhArray.length][0] = hhUnemployedIndexArray[hhArray.length-1][hhArray[hhArray.length-1].length];

        
		return totalHouseholds;
        
	}
    
    

	// get the set of hh attributes need for SPG1 from PUMS files
	public void getHHAttributesFromPUMS () {
        
		ArrayList hhList = new ArrayList();

		hhArray = new int[halo.getNumberOfStates()][][];

		PUMSData pums = new PUMSData ( (String)propertyMap.get("pumsDictionary.fileName") );
  		
		String propertyName;
		String[] PUMSFILE = new String[halo.getNumberOfStates()];
		for (int i=0; i < PUMSFILE.length; i++) {
			propertyName = "pums" + halo.getStateLabel(i) + ".fileName";
			PUMSFILE[i] = (String)propertyMap.get( propertyName );
		}

		
		for (int i=0; i < halo.getNumberOfStates(); i++) {
            
			hhList = pums.readSpg1Attributes ( PUMSFILE[i], (String)propertyMap.get("zoneIndex.fileName") );

			logger.info ( hhList.size() + " household records found in " + halo.getStateLabel(i) + " PUMS data file." ); 

			hhArray[i] = new int[hhList.size()][];

			for (int k=0; k < hhList.size(); k++) {
				hhArray[i][k] = (int[])hhList.get(k);
			}

		}

	}



    
	// get the set of PUMS attributes that SPG2 will write to household output file.
	public void writeHHOutputAttributesFromPUMS () {
        
		int hhid;
		int state;
		int zone;
		
		logger.info ("");
		logger.info ("");
		logger.info ("writing SynPopH and SynPopP files.");
		logger.info ("");

		
		//Parse list of hh variables from properties file.
		String variableString = (String)propertyMap.get("pumsHH.variables");
		ArrayList variableList = new ArrayList();
		StringTokenizer st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		String[] hhVariables = new String[variableList.size()];
		for (int i=0; i < hhVariables.length; i++)
			hhVariables[i] = (String)variableList.get(i);

		//Parse list of person variables from properties file.
		variableString = (String)propertyMap.get("pumsPerson.variables");
		variableList.clear();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		String[] personVariables = new String[variableList.size()];
		for (int i=0; i < personVariables.length; i++)
			personVariables[i] = (String)variableList.get(i);

		
		String[] hhFieldValues = new String[hhVariables.length + 2];
		String[][] personFieldValues = null;
		String[][] pumsAttributeValues = null;

		PrintWriter hhOutStream = null;
		PrintWriter personOutStream = null;
		
		String propertyName;
		String[] PUMSFILE = new String[halo.getNumberOfStates()];
		for (int i=0; i < PUMSFILE.length; i++) {
			propertyName = "pums" + halo.getStateLabel(i) + ".fileName";
			PUMSFILE[i] = (String)propertyMap.get( propertyName );
		}
		
		PUMSData pums = new PUMSData ( (String)propertyMap.get("pumsDictionary.fileName") );


		
	    
		// read the SPG2 output file of synthetic household records into a TableDataSet
		logger.info ("reading the SPG2 output file of household index and state index into a TableDataSet.");
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( (String)propertyMap.get("spg.hhRecordList.fileName") ));
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
		// write the synthetic household attributes file.
		
		try {
			hhOutStream = new PrintWriter(new BufferedWriter(
				new FileWriter( (String)propertyMap.get("spg.synpopH.fileName") )));

			//write household attributes file header record
			String[] fieldNames = new String[hhVariables.length + 2];
			fieldNames[0] = "HH_ID";
			for (int i=0; i < hhVariables.length; i++)
				fieldNames[i+1] = hhVariables[i];
			fieldNames[fieldNames.length-1] = "ALPHAZONE";
			writePumsData( hhOutStream, fieldNames);
		}
		catch (Exception e) {
			logger.severe("Exception when opening synthetic household attributes file.");
		}

		try {
			personOutStream = new PrintWriter(new BufferedWriter(
				new FileWriter( (String)propertyMap.get("spg.synpopP.fileName") )));

			//write household attributes file header record
			String[] fieldNames = new String[personVariables.length + 2];
			fieldNames[0] = "HH_ID";
			fieldNames[1] = "PERS_ID";
			for (int i=0; i < personVariables.length; i++)
				fieldNames[i+2] = personVariables[i];
			writePumsData( personOutStream, fieldNames);
		}
		catch (Exception e) {
			logger.severe("Exception when opening synthetic person attributes file.");
		}


		
		int hhCount = 1;
		for (int i=0; i < halo.getNumberOfStates(); i++) {
		    
		    logger.info ("reading PUMS data file for " + halo.getStateLabel(i));
			ArrayList pumsList = pums.readSpg2OutputAttributes ( PUMSFILE[i], hhVariables, personVariables, (String)propertyMap.get("zoneIndex.fileName") );
			
		    logger.info ("looking up PUMS records corresponding to household/state indices in TableDataSet.");
			for (int k=0; k < table.getRowCount(); k++) {
			    
			    state = (int)table.getValueAt( k+1, 2 );
			    if (state == i) {

					hhid = (int)table.getValueAt( k+1, 1 );
					zone = (int)table.getValueAt( k+1, 3 );
					
					pumsAttributeValues = (String[][])pumsList.get( hhid );

					
					hhFieldValues[0] = Integer.toString( hhCount );
					for (int j=0; j < pumsAttributeValues[0].length; j++)
						hhFieldValues[j+1] = pumsAttributeValues[0][j];
					hhFieldValues[hhFieldValues.length-1] = Integer.toString( zone );

					
					personFieldValues = new String[pumsAttributeValues.length][personVariables.length + 2];
					for (int p=1; p < pumsAttributeValues.length; p++) {
						personFieldValues[p][0] = Integer.toString( hhCount );
						personFieldValues[p][1] = Integer.toString( p );
						for (int j=0; j < pumsAttributeValues[p].length; j++)
							personFieldValues[p][j+2] = pumsAttributeValues[p][j];
					}
					
					hhCount++;
					
					
					try {
						writePumsData( hhOutStream, hhFieldValues );
					}
					catch (Exception e) {
						logger.severe("Exception when writing synthetic household attributes file.");
					}
					
					try {
						for (int p=1; p < pumsAttributeValues.length; p++)
							writePumsData( personOutStream, personFieldValues[p] );
					}
					catch (Exception e) {
						logger.severe("Exception when writing synthetic person attributes file.");
					}
					
			    }
			
			}

		}

		
		try {
			hhOutStream.close();
		}
		catch (Exception e) {
			logger.severe("Exception when closing synthetic household attributes file.");
		}
		
		try {
			personOutStream.close();
		}
		catch (Exception e) {
			logger.severe("Exception when closing synthetic person attributes file.");
		}
		
	}



    
    // write category frequency tables by state to logger
	public void writeFrequencyTables () {

		int numPersons;
		int numWorkers;
		int pumsIndustryCode;
		int pumsOccupationCode;
		int pumsIncomeCode;
		int edIndustryCode;
		int occupationCode;
		int incomeSizeCode;
		int workersCode;
		int pumsHHWeight;
		int employmentStatusCode;
		

		int[] edIndFreq = new int[edInd.getNumberEdIndustries()];
		int[] pumsIndFreq = new int[edInd.getNumberEdIndustries()];
		int[] pumsWtIndFreq = new int[edInd.getNumberEdIndustries()];

		int[] occFreq = new int[occ.getNumberOccupations()];
		int[] pumsOccFreq = new int[occ.getNumberOccupations()];
		int[] pumsWtOccFreq = new int[occ.getNumberOccupations()];

		int[] incSizeFreq = new int[incSize.getNumberIncomeSizes()];
		int[] pumsIncSizeFreq = new int[incSize.getNumberIncomeSizes()];
		int[] pumsWtIncSizeFreq = new int[incSize.getNumberIncomeSizes()];

		int[] workersFreq = new int[workers.getNumberWorkerCategories()];
		int[] pumsWorkersFreq = new int[workers.getNumberWorkerCategories()];
		int[] pumsWtWorkersFreq = new int[workers.getNumberWorkerCategories()];


		int numOccupations = occ.getNumberOccupations();
		int numIncomeSizes = incSize.getNumberIncomeSizes();
		

		// Write frequency tables of total workers by industry and occupation codes
		// and total households by household category and number of workers in all final synthetic households,
		// PUMS records, and weighted PUMS records for each state in the halo area
		for (int i=0; i < hhArray.length; i++) {

			Arrays.fill (edIndFreq, 0);
			Arrays.fill (occFreq, 0);
			Arrays.fill (incSizeFreq, 0);
			Arrays.fill (workersFreq, 0);
			Arrays.fill (pumsIndFreq, 0);
			Arrays.fill (pumsOccFreq, 0);
			Arrays.fill (pumsIncSizeFreq, 0);
			Arrays.fill (pumsWorkersFreq, 0);
			Arrays.fill (pumsWtIndFreq, 0);
			Arrays.fill (pumsWtOccFreq, 0);
			Arrays.fill (pumsWtIncSizeFreq, 0);
			Arrays.fill (pumsWtWorkersFreq, 0);
		    
			for (int k=0; k < hhArray[i].length; k++) {
		
				numPersons = hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX];
				numWorkers = hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				pumsIncomeCode = hhArray[i][k][HH_INCOME_ATTRIB_INDEX];
				pumsHHWeight = hhArray[i][k][HH_WEIGHT_ATTRIB_INDEX];

				incomeSizeCode = incSize.getIncomeSize(pumsIncomeCode, numPersons); 
				incSizeFreq[incomeSizeCode] += ( hhArray[i][k][HH_SELECTED_INDEX] + hhArray[i][k][HH_UNEMPLOYED_INDEX] );
				pumsIncSizeFreq[incomeSizeCode]++;
				pumsWtIncSizeFreq[incomeSizeCode] += pumsHHWeight;

				workersCode = workers.getWorkers(numWorkers); 
				workersFreq[numWorkers] += ( hhArray[i][k][HH_SELECTED_INDEX] + hhArray[i][k][HH_UNEMPLOYED_INDEX] );
				pumsWorkersFreq[numWorkers]++;
				pumsWtWorkersFreq[numWorkers] += pumsHHWeight;

				for (int j=0; j < numPersons; j++) {

					employmentStatusCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 2];
					if ( employmentStatusCode > 0 ) {
						
						pumsIndustryCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 0];
						pumsOccupationCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 1];
						edIndustryCode = edInd.getEdIndustry(pumsIndustryCode);
						occupationCode = occ.getOccupation(pumsOccupationCode); 
						
						edIndFreq[edIndustryCode] += ( hhArray[i][k][HH_SELECTED_INDEX] );
						pumsIndFreq[edIndustryCode]++;
						pumsWtIndFreq[edIndustryCode] += pumsHHWeight;
	
						occFreq[occupationCode] += ( hhArray[i][k][HH_SELECTED_INDEX] );
						pumsOccFreq[occupationCode]++;
						pumsWtOccFreq[occupationCode] += pumsHHWeight;
					
					}
					
				}
		        
			}
		    
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Employed Persons by ED Industry categories for all households in final sample", "Industry", edInd.getEdIndustryLabels(), edIndFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Employed Persons by Occupation categories for all households in final sample", "Occupation", occ.getOccupationLabels(), occFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Households by Income/Household Size categories for all households in final sample", "IncomeSize", incSize.getIncomeSizeLabels(), incSizeFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Households by Workers categories for all households in final sample", "Workers", workers.getWorkersLabels(), workersFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Employed Persons by ED Industry categories for all PUMS households", "Industry", edInd.getEdIndustryLabels(), pumsIndFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Employed Persons by Occupation categories for all PUMS households", "Occupation", occ.getOccupationLabels(), pumsOccFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Households by Income/Household Size categories for all PUMS households", "IncomeSize", incSize.getIncomeSizeLabels(), pumsIncSizeFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Households by Workers categories for all PUMS households", "Workers", workers.getWorkersLabels(), pumsWorkersFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Employed Persons by ED Industry categories for all Weighted PUMS households", "Industry", edInd.getEdIndustryLabels(), pumsWtIndFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Employed Persons by Occupation categories for all Weighted PUMS households", "Occupation", occ.getOccupationLabels(), pumsWtOccFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Households by Income/Household Size categories for all Weighted PUMS households", "IncomeSize", incSize.getIncomeSizeLabels(), pumsWtIncSizeFreq );
			writeFreqSummaryToLogger( halo.getStateLabel(i) + " Households by Workers categories for all Weighted PUMS households", "Workers", workers.getWorkersLabels(), pumsWtWorkersFreq );
		
		}

		
		// Now repeat, but for all states combined:
		// Write frequency tables of total person industry and occupation codes
		// and household category and number of workers in all final synthetic households,
		// PUMS records, and weighted PUMS records over all states combined in the halo area.
		Arrays.fill (edIndFreq, 0);
		Arrays.fill (occFreq, 0);
		Arrays.fill (incSizeFreq, 0);
		Arrays.fill (workersFreq, 0);
		Arrays.fill (pumsIndFreq, 0);
		Arrays.fill (pumsOccFreq, 0);
		Arrays.fill (pumsIncSizeFreq, 0);
		Arrays.fill (pumsWorkersFreq, 0);
		Arrays.fill (pumsWtIndFreq, 0);
		Arrays.fill (pumsWtOccFreq, 0);
		Arrays.fill (pumsWtIncSizeFreq, 0);
		Arrays.fill (pumsWtWorkersFreq, 0);
	    
		for (int i=0; i < hhArray.length; i++) {

			for (int k=0; k < hhArray[i].length; k++) {
		
				numPersons = hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX];
				numWorkers = hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				pumsIncomeCode = hhArray[i][k][HH_INCOME_ATTRIB_INDEX];
				pumsHHWeight = hhArray[i][k][HH_WEIGHT_ATTRIB_INDEX];

				incomeSizeCode = incSize.getIncomeSize(pumsIncomeCode, numPersons); 
				incSizeFreq[incomeSizeCode] += ( hhArray[i][k][HH_SELECTED_INDEX] + hhArray[i][k][HH_UNEMPLOYED_INDEX] );
				pumsIncSizeFreq[incomeSizeCode]++;
				pumsWtIncSizeFreq[incomeSizeCode] += pumsHHWeight;

				workersCode = workers.getWorkers(numWorkers); 
				workersFreq[numWorkers] += ( hhArray[i][k][HH_SELECTED_INDEX] + hhArray[i][k][HH_UNEMPLOYED_INDEX] );
				pumsWorkersFreq[numWorkers]++;
				pumsWtWorkersFreq[numWorkers] += pumsHHWeight;

				for (int j=0; j < numPersons; j++) {

					employmentStatusCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 2];
					if ( employmentStatusCode > 0 ) {
						
					    pumsIndustryCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 0];
						pumsOccupationCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 1];
						edIndustryCode = edInd.getEdIndustry(pumsIndustryCode);
						occupationCode = occ.getOccupation(pumsOccupationCode); 
						
						edIndFreq[edIndustryCode] += ( hhArray[i][k][HH_SELECTED_INDEX] );
						pumsIndFreq[edIndustryCode]++;
						pumsWtIndFreq[edIndustryCode] += pumsHHWeight;
	
						occFreq[occupationCode] += ( hhArray[i][k][HH_SELECTED_INDEX] );
						pumsOccFreq[occupationCode]++;
						pumsWtOccFreq[occupationCode] += pumsHHWeight;

					}
					
				}
		        
			}
		    
		}

		writeFreqSummaryToLogger( " Regional Employed Persons by ED Industry categories for all households in final sample", "Industry", edInd.getEdIndustryLabels(), edIndFreq );
		writeFreqSummaryToLogger( " Regional Employed Persons by Occupation categories for all households in final sample", "Occupation", occ.getOccupationLabels(), occFreq );
		writeFreqSummaryToLogger( " Regional Households by Income/Household Size categories for all households in final sample", "IncomeSize", incSize.getIncomeSizeLabels(), incSizeFreq );
		writeFreqSummaryToLogger( " Regional Households by Workers categories for all households in final sample", "Workers", workers.getWorkersLabels(), workersFreq );
		writeFreqSummaryToLogger( " Regional Employed Persons by ED Industry categories for all PUMS households", "Industry", edInd.getEdIndustryLabels(), pumsIndFreq );
		writeFreqSummaryToLogger( " Regional Employed Persons by Occupation categories for all PUMS households", "Occupation", occ.getOccupationLabels(), pumsOccFreq );
		writeFreqSummaryToLogger( " Regional Households by Income/Household Size categories for all PUMS households", "IncomeSize", incSize.getIncomeSizeLabels(), pumsIncSizeFreq );
		writeFreqSummaryToLogger( " Regional Households by Workers categories for all PUMS households", "Workers", workers.getWorkersLabels(), pumsWorkersFreq );
		writeFreqSummaryToLogger( " Regional Employed Persons by ED Industry categories for all Weighted PUMS households", "Industry", edInd.getEdIndustryLabels(), pumsWtIndFreq );
		writeFreqSummaryToLogger( " Regional Employed Persons by Occupation categories for all Weighted PUMS households", "Occupation", occ.getOccupationLabels(), pumsWtOccFreq );
		writeFreqSummaryToLogger( " Regional Households by Income/Household Size categories for all Weighted PUMS households", "IncomeSize", incSize.getIncomeSizeLabels(), pumsWtIncSizeFreq );
		writeFreqSummaryToLogger( " Regional Households by Workers categories for all Weighted PUMS households", "Workers", workers.getWorkersLabels(), pumsWtWorkersFreq );
	
	}
		
	

	// summarize households by incomeSize category for updating PI input file.
	public TableDataSet sumHouseholdsByIncomeSize () {

		int numPersons;
		int pumsIncomeCode;
		int incomeSizeCode;
		


		int numIncomeSizes = incSize.getNumberIncomeSizes();
		householdsByIncomeSize = new int[numIncomeSizes];
		

		// get frequency table of total households by household category codes in all final synthetic households
		Arrays.fill (householdsByIncomeSize, 0);
		for (int i=0; i < hhArray.length; i++) {

			for (int k=0; k < hhArray[i].length; k++) {
		
				numPersons = hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX];
				pumsIncomeCode = hhArray[i][k][HH_INCOME_ATTRIB_INDEX];

				incomeSizeCode = incSize.getIncomeSize(pumsIncomeCode, numPersons); 
				householdsByIncomeSize[incomeSizeCode] += ( hhArray[i][k][HH_SELECTED_INDEX] + hhArray[i][k][HH_UNEMPLOYED_INDEX] );

			}
		    
		}


		// save the total households in TableDataSet for writing later
		float[] tableData = new float[numIncomeSizes];
		String[][] labels = new String[numIncomeSizes][1];
		for (int i=0; i < householdsByIncomeSize.length; i++) {
			labels[i][0] = incSize.getIncomeSizeLabel(i);
			tableData[i] = householdsByIncomeSize[i];
		}
		ArrayList headings = new ArrayList();
		headings.add("hhCategory");
		
		TableDataSet table = TableDataSet.create ( labels, headings );
		table.appendColumn ( tableData, "spg1Households");

		
		
		
		return table;
		
	}
		
	

    private void writeFreqSummaryToLogger ( String tableTitle, String fieldName, String[] freqDescriptions, int[] freqs ) {
        
    		// print a simple summary table
    		logger.info( "Frequency Report table: " + tableTitle );
    		logger.info( "Frequency for field " + fieldName );
    		logger.info(Format.print("%8s", "Value") + Format.print("%13s", "Description") + Format.print("%45s", "Frequency"));
    		
    		int total = 0;
    		for (int i = 0; i < freqs.length; i++) {
    			if (freqs[i] > 0) {
    				String description = freqDescriptions[i];
    				logger.info( Format.print("%8d", i) + "  " + Format.print("%-45s", description) + Format.print("%11d", freqs[i] ) );
    				total += freqs[i];
    			}
    		}
    		
    		logger.info(Format.print("%15s", "Total") +	Format.print("%51d\n\n\n", total));
    	}
        
        

    // write out the summary statistics for the final synthetic population and the corresponding weighted PUMS summaries
    // statistics are summarized for various categories by PUMA
    private void writeFreqSummaryToCsvFile () {

    	String state;
    	int puma;
    	String incomeSizeLabel;
    	String industryLabel;
    	String occupationLabel;
    	int value;
    	
    	int pumaIndex;
		int numPersons;
		int numWorkers;
		int pumsIncomeCode;
		int pumsIndustryCode;
		int pumsOccupationCode;
		int incomeSizeCode;
		int edIndustryCode;
		int occupationCode;
		int employmentStatusCode;
		

		
		int[][] pumas = halo.getPumas();
		int numIncomeSizes = incSize.getNumberIncomeSizes();
		int numOccupations = occ.getNumberOccupations();
		int numIndustries = edInd.getNumberEdIndustries();
		
		// define array of states by pumas for summarizing data
		int[][][][] hhsByStatePumaCategory = new int[pumas.length][][][];
		int[][][][] personsByStatePumaIndOcc = new int[pumas.length][][][];
		for (int i=0; i < pumas.length; i++) {
			hhsByStatePumaCategory[i] = new int[pumas[i].length][][];
			personsByStatePumaIndOcc[i] = new int[pumas[i].length][][];
		}
		
		
		

		// summarize households by household size/income categories over all final synthetic households
		// and summarize persons by industryt by occupation;
		// i index loops over states, j index loops over pumas in states
		for (int i=0; i < pumas.length; i++) {
			for (int j=0; j < pumas[i].length; j++) {
				hhsByStatePumaCategory[i][j] = new int[numIncomeSizes][workers.getNumberWorkerCategories()];
				personsByStatePumaIndOcc[i][j] = new int[numIndustries][numOccupations];
			}
		}
		
		

		// loop over all PUMS records used in making synthetic population and summarize
		// by incomeSize and industry/occupation categories by state and puma
		for (int i=0; i < hhArray.length; i++) {

			for (int k=0; k < hhArray[i].length; k++) {
		
				puma = hhArray[i][k][PUMA_ATTRIB_INDEX];
				
				pumaIndex = halo.getPumaIndex(i,puma);
				
				numPersons = hhArray[i][k][NUM_PERSONS_ATTRIB_INDEX];
				pumsIncomeCode = hhArray[i][k][HH_INCOME_ATTRIB_INDEX];
				numWorkers = hhArray[i][k][NUM_WORKERS_ATTRIB_INDEX];
				if (numWorkers > workers.getNumberWorkerCategories()-1)
					numWorkers = workers.getNumberWorkerCategories()-1;

				incomeSizeCode = incSize.getIncomeSize(pumsIncomeCode, numPersons);

				for (int j=0; j < numPersons; j++) {

					employmentStatusCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 2];
					if ( employmentStatusCode > 0 ) {
						
						pumsIndustryCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 0];
						pumsOccupationCode = hhArray[i][k][PERSON_ARRAY_ATTRIB_INDEX + j*3 + 1];
						edIndustryCode = edInd.getEdIndustry(pumsIndustryCode);
						occupationCode = occ.getOccupation(pumsOccupationCode);
	
						personsByStatePumaIndOcc[i][pumaIndex][edIndustryCode][occupationCode] += ( hhArray[i][k][HH_SELECTED_INDEX] );
					
					}
				}
				
				hhsByStatePumaCategory[i][pumaIndex][incomeSizeCode][numWorkers] += ( hhArray[i][k][HH_SELECTED_INDEX] + hhArray[i][k][HH_UNEMPLOYED_INDEX] );

			}
		    
		}


    	
		String incomeSizeFileName = (String)propertyMap.get("incomeSizeCalibration.fileName");    	
		String industryOccupationFileName = (String)propertyMap.get("industryOccupationCalibration.fileName");    	
    	
    	
		PrintWriter outStream = null;

        // open output stream for writing SPG1 calibration results file
		try {
			
			// write csv file header record
			outStream = new PrintWriter (new BufferedWriter( new FileWriter(incomeSizeFileName) ) );
			outStream.println ( "State,PUMA,IncomeSize,Workers,HH_Frequency");

			// write hh size/income category descriptions and frequencies by state and puma
			for (int i=0; i < hhsByStatePumaCategory.length; i++) {
				for (int j=0; j < hhsByStatePumaCategory[i].length; j++) {
					for (int k=0; k < hhsByStatePumaCategory[i][j].length; k++) {
						for (int m=0; m < hhsByStatePumaCategory[i][j][k].length; m++) {
						
							state = halo.getStateLabel(i);
							puma = halo.getPumaLabel(i, j);
							incomeSizeLabel = incSize.getIncomeSizeLabel(k);
							value = hhsByStatePumaCategory[i][j][k][m];
						
							if (value > 0)
								outStream.println( state + "," + puma + "," + incomeSizeLabel + "," + (m < workers.getNumberWorkerCategories()-1 ? Integer.toString(m) : (workers.getNumberWorkerCategories()-1 + "+")) + "," + value );
						}
					}
				}
			}
			outStream.close();


		
		
			// write csv file header record
			outStream = new PrintWriter (new BufferedWriter( new FileWriter(industryOccupationFileName) ) );
			outStream.println ( "State,PUMA,Industry,Occupation,Worker_Frequency");

			// write hh size/income category descriptions and frequencies by state and puma
			for (int i=0; i < personsByStatePumaIndOcc.length; i++) {
				for (int j=0; j < personsByStatePumaIndOcc[i].length; j++) {
					for (int k=0; k < personsByStatePumaIndOcc[i][j].length; k++) {
						for (int m=0; m < personsByStatePumaIndOcc[i][j][k].length; m++) {
							state = halo.getStateLabel(i);
							puma = halo.getPumaLabel(i, j);
							industryLabel = edInd.getEdIndustryLabel(k);
							occupationLabel = occ.getOccupationLabel(m);
							value = personsByStatePumaIndOcc[i][j][k][m];
							
							if (value > 0)
								outStream.println( state + "," + puma + "," + industryLabel + "," + occupationLabel + "," + value );
						}
					}
				}
			}
			outStream.close();

		}
		catch (IOException e) {
			logger.severe ("I/O exception writing SPG calibration results file.");
			e.printStackTrace();
		}


	}
			
        
        
        
    // return a monte carlo selection from the range [0,...,probabilities.length-1]
	// a return value of -1 indicates no selection alternatives had a positive probability.
	public int getMonteCarloSelection (double[] probabilities) {
	    
		double randomNumber = SeededRandom.getRandom();
		int returnValue = -1;

		double sum = probabilities[0];
		for (int i=1; i < probabilities.length; i++) {
			if (randomNumber <= sum) {
				returnValue = i-1;
				break;
			}
			else {
				sum += probabilities[i];
			}
		}

		if (returnValue < 0) {
		    if ( randomNumber <= 1.0 ) {
		        returnValue = probabilities.length - 1;
		    }
		    else {
		        logger.info ( "getMonteCarloSelection has randomNumber=" + randomNumber +", sum=" + sum);
		    }
		}

		return returnValue;
		
	}
	
	
	
	private void writePumsData(PrintWriter outStream, String[] record) {
	    
		outStream.print(record[0]);

		for (int i = 1; i < record.length; i++)
			outStream.print("," + record[i]);

		outStream.println();
		
	}


	
	private void writePumsData(PrintWriter outStream, double[] record) {
	    
		outStream.print(record[0]);

		for (int i = 1; i < record.length; i++)
			outStream.print("," + record[i]);

		outStream.println();
		
	}


	
	private double[][][] readPiLaborDollars ( String fileName ) {
	    
	    int index;
		int zone;
		int occup;
		double dollars;
	    
	    

	    
		// read the PI output file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		
		// get valuies from TableDataSet into array to return
		double[][][] laborDollars = new double[halo.getNumberOfZones()][occ.getNumberOccupations()][incSize.getNumberIncomeSizes()];

		
		
		for (int r=0; r < table.getRowCount(); r++) {
		    
			occup = occ.getOccupationIndex( table.getStringValueAt(r+1, 2) );
			zone = (int)table.getValueAt(r+1, 1);

			index = halo.getZoneIndex(zone);

			if (index < 0)
				continue;

			dollars = 0.0;
			for (int c=0; c < incSize.getNumberIncomeSizes(); c++) {
				dollars = table.getValueAt(r+1, c+3);
				laborDollars[index][occup][c] = dollars;
				regionLaborDollars[occup][c] += dollars;
			}
			
		}
		
		return laborDollars;
		
	}
	
	

	private int[][] readPiIncomeSizeHHs ( String fileName ) {
	    
		int index;
		int zone;
		int incomeSize;
		int hhs;
		int oldHhs = 0;
		float piHhs = 0.0f;
		float oldPiHhs = 0.0f;
		float remainder = 0.0f;
		float oldRemainder = 0.5f;
	    

	    
		// read the PI output file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		
		// get values from TableDataSet into array to return
		int[][] dataTable = new int[halo.getNumberOfZones()][incSize.getNumberIncomeSizes()];

		int totalHhs = 0;
		for (int r=0; r < table.getRowCount(); r++) {
			incomeSize = incSize.getIncomeSizeIndex( table.getStringValueAt(r+1, 1) );
			
			if (incomeSize >= 0) {
			    
				zone = (int)table.getValueAt(r+1, 2);
				piHhs = table.getValueAt(r+1, 3);
				
				remainder = oldRemainder - oldHhs + (int)oldPiHhs + (piHhs - (int)piHhs);
				hhs = (int)piHhs + (int)remainder;
			
				index = halo.getZoneIndex(zone);

                if (index <= 0 || index >= halo.getNumberOfZones())
                    logger.info ("r="+r + ", index="+index + ", zone="+zone);

				dataTable[index][incomeSize] = hhs;
				
				oldHhs = hhs;
				oldPiHhs = piHhs;
				oldRemainder = remainder;
				
				totalHhs += hhs;
			}
		}
		
		return dataTable;
		
	}
	
	

	public void writePiInputFile ( TableDataSet table ) {
	    
	    
		String fileName = (String)propertyMap.get("spg.hhsByHhCategory.fileName");
		
		// write the PI input file from a TableDataSet
		CSVFileWriter writer = new CSVFileWriter();
        
		try {
			writer.writeFile( table, new File( fileName ), new GeneralDecimalFormat("0.#####E0",10000000,.01 ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	

	// the following main() is used to test the methods implemented in this object.
    public static void main (String[] args) {
        
		long startTime = System.currentTimeMillis();
		String which = args[0];
        SPGnew testSPG = new SPGnew( "spg_full" );

        if(which.equals("spg1"))
		{
            testSPG.getHHAttributesFromPUMS();
            testSPG.spg1();
            TableDataSet table = testSPG.sumHouseholdsByIncomeSize();
            testSPG.writePiInputFile(table);
            testSPG.writeFreqSummaryToCsvFile();
            logger.info("SPG1 finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
            startTime = System.currentTimeMillis();
        }
        else {
            testSPG.spg2();
            logger.info("SPG2 finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
            startTime = System.currentTimeMillis();



            testSPG.writeHHOutputAttributesFromPUMS();
            logger.info("writing SynPop files finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

        }
    }

}

