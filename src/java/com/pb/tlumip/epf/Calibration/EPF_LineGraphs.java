	package com.pb.tlumip.epf.Calibration;

	import com.pb.models.reference.ModelComponent;
	import com.pb.common.datafile.CSVFileReader;
	import com.pb.common.datafile.TableDataSetLoader;
	import com.pb.common.datafile.TableDataSet;
	import com.pb.common.datafile.CSVFileWriter;
	import com.pb.common.util.ResourceUtil;

	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.List;
	import java.util.TreeSet;
	import java.util.ResourceBundle;
	import java.util.HashMap;
	import java.io.IOException;
	import java.io.File;

	import org.apache.log4j.Logger;


		/**
		 * @param args
		 */
	public class EPF_LineGraphs {
	    protected static Logger logger = Logger.getLogger(EPF_LineGraphs.class);
	    
		EPF_Helper helper;
	    int startInterval;
	    int endInterval; 
		
		TableDataSet actLocReference;
		TableDataSet actLocCalibration;
		TableDataSet pctChangeTableIndustry;
		TableDataSet pctChangeTableHH;		

    	boolean countedHhs = false;
		int numHhCategories;

    	HashMap<String, TreeSet> betaZonesTreeSet = new HashMap<String, TreeSet>();
    	HashMap<String, HashMap<String, double[]>> geoRegActValues;
    	
		public EPF_LineGraphs(EPF_Helper helper){
			this.helper = helper;
	        startInterval = helper.getStartInterval();
	        endInterval = helper.getEndInterval();
	    }
		
	    public void produceLineGraphs(){
	        
	        //Make a list of the beta zones that are in each geographical region.
	        getBetaZones();  
	        
	        //Print beta zones in TreeSet.
//	        printTreeSet(betaZonesTreeSet.get(EPF_Helper.geoRegionNames[1]), "ID-Boise");
	        
	        //Loop through the data and do the calculations.
	        calculatePercentChange();    
	                
	        //Creates the table data set that is written out.
	        getFinalTable();
	        
	        //Write the output file with only the Industries in it.
	        helper.writeOutputFile(pctChangeTableIndustry, "calibration.percent.change.table.industry");
	        
	        //Write the output file with only the HH activities in it.
	        helper.writeOutputFile(pctChangeTableHH, "calibration.percent.change.table.hh"); 
	        
	    	//call the R scripts that produce the actual graphs and legends.
	    	helper.runRScript("run.line.graphs.Rscript");
	    }
	    
	    //This method creates tree sets of the specific geo regions.
	    private void getBetaZones(){
	    	TreeSet<Integer> haloSet = getGeographicSet("OOO");
	    	betaZonesTreeSet.put(EPF_Helper.geoRegionNames[0], haloSet);
//		    	System.out.println("adding " + geoRegionNames[0]);
	    	
	    	TreeSet<Integer> bendSet = getGeographicSet("Bend");
	    	betaZonesTreeSet.put(EPF_Helper.geoRegionNames[5], bendSet);
	    	
	    	ArrayList<String> boiseCounties = new ArrayList<String> ();
	    	boiseCounties.add("Ada");
	    	boiseCounties.add("Canyon");
	    	TreeSet<Integer> idBoiseSet = getStateCountySet("ID", boiseCounties);
	    	betaZonesTreeSet.put(EPF_Helper.geoRegionNames[1], idBoiseSet);
    	
	    	ArrayList<String> waTriCitiesCounties = new ArrayList<String> ();
	    	waTriCitiesCounties.add("Franklin");
	    	waTriCitiesCounties.add("Benton");
	    	waTriCitiesCounties.add("WallaWalla");
	    	TreeSet<Integer> waTriCitiesSet = getStateCountySet("WA", waTriCitiesCounties);
	    	betaZonesTreeSet.put(EPF_Helper.geoRegionNames[2], waTriCitiesSet);
	    		    	
	    	TreeSet<Integer> orClarkCoSet = getOrClarkSet();
	    	betaZonesTreeSet.put(EPF_Helper.geoRegionNames[3], orClarkCoSet);
	    	
	    	TreeSet<Integer> orWvSet = getWVSet();
	    	betaZonesTreeSet.put(EPF_Helper.geoRegionNames[4], orWvSet);
	    	

    	
	    	
	    }
	    
	    //To get the beta zones for the Halo graphical region
	    //loop through the alpha2beta.csv file column MPOmodeledzones
	    //if the modeled zone is equal to "OOO" get the associated beta zone
	    //and output it as a TreeSet.
	    private TreeSet<Integer> getGeographicSet(String mpoModeledZone){
	    	TreeSet<Integer> geoSet = new TreeSet<Integer>();
	    	String[]  mpoModeledZones = helper.getAlpha2Beta().getColumnAsString("MPOmodeledzones");
	    	int[]  bZones = helper.getAlpha2Beta().getColumnAsInt("Bzone");
	    	
	    	for (int i = 0; i < mpoModeledZones.length; i++){
	    		if (mpoModeledZones[i].equals(mpoModeledZone)){
	    			geoSet.add(bZones[i]); 
	    		}
	    	}
	    	return geoSet;
	    }
	    
	    
	    //To get the beta zones for the ID-Boise graphical region
	    //loop through the alpha2beta.csv file column COUNTY
	    //if the county is equal to "Ada" or "Canyon" get the associated 
	    // beta zone and output it as a TreeSet.
	    private TreeSet<Integer> getStateCountySet(String stateName, ArrayList<String> countyNames){
	    	TreeSet<Integer> stateCountySet = new TreeSet<Integer>();
	    	String[]  state = helper.getAlpha2Beta().getColumnAsString("State");	    
	    	String[]  county = helper.getAlpha2Beta().getColumnAsString("COUNTY");
	    	int[]  bZones = helper.getAlpha2Beta().getColumnAsInt("Bzone");
	    	
	    	for (int i = 0; i < state.length; i++){
	    		if (state[i].equals(stateName)){
	    			if (countyNames.contains(county[i]))
	    				stateCountySet.add(bZones[i]);
	    		}

	    	}
	    	return stateCountySet;
	    }
	    
	    
	    //To get the beta zones for the OR/ClarkCo graphical region
	    //loop through the alpha2beta.csv file column COUNTY
	    //if the county is equal to "Clark"  or if from the State column
	    // the state is equal to "OR" get the associated beta zone 
	    //  and output it as a TreeSet.
	    private TreeSet<Integer> getOrClarkSet(){
	    	TreeSet<Integer> orClarkSet = new TreeSet<Integer>();
	    	String[]  state = helper.getAlpha2Beta().getColumnAsString("State");
	    	String[]  county = helper.getAlpha2Beta().getColumnAsString("COUNTY");
	    	int[]  bZones = helper.getAlpha2Beta().getColumnAsInt("Bzone");
	    	
	    	for (int i = 0; i < state.length; i++){
	    		if (state[i].equals("OR")){
	    			orClarkSet.add(bZones[i]); 
	    		}else if (state[i].equals("WA")){
	    			if (county[i].equals("Clark"))
		    			orClarkSet.add(bZones[i]);
	    		}

	    	}
    		
	    	return orClarkSet;
    	}
	    
	    //To get the beta zones for the OR-WV graphical region
	    //loop through the bzones_OR_clip.csv file column Bzone
	    //and output it as a TreeSet.
	    private TreeSet<Integer> getWVSet(){
	    	TreeSet<Integer> wvSet = new TreeSet<Integer>();
	    	int[]  bZones = helper.getBzonesOrClip().getColumnAsInt("Bzone");
	    	
	    	for (int i = 0; i < bZones.length; i++){
	    		wvSet.add(bZones[i]);     		
	    	}
	    	return wvSet;
	    }
	    
	    private void printTreeSet(TreeSet<Integer> betaZones, String name){
	    	System.out.println("Zones in " + name);
	    	for (Integer zone : betaZones){
	    		System.out.println(zone);
	    	}
	    }
	    private void calculatePercentChange(){
	       	
	    	//Create a hash map with the keys being the geo regions and the values being a hash map.
	    	//This inner hash map has the keys being the activities and the values being the percent difference
	    	//in each year.
	    	geoRegActValues = new HashMap<String, HashMap<String, double[]>>();
	    	
	    	//Tree set used to count hh categories in activity location file.
	    	TreeSet<String> hhCategories = new TreeSet<String>();
	    	
	    	for (int i = startInterval; i <= endInterval; i++){
	    		actLocReference = helper.loadTableDataSet(i, helper.referenceScenarioName);
	    		actLocCalibration = helper.loadTableDataSet(i, helper.scenarioName);    	

	    		for (String geoRegionName : EPF_Helper.geoRegionNames) {
	    			TreeSet<Integer> betaZonesInGeoRegion =  betaZonesTreeSet.get(geoRegionName);
	    	    	
	    			//Create a hash map with the keys as the activities and the values as an array
	    			//with element zero being the sum of the reference values for the beta zones in
	    			//the geo region and element one being the calibration values for the beta zones
	    			//in the geo region.
	    			HashMap<String, double[]> actWithPctDiffs = new HashMap<String, double[]>();
	    	    	
	    			//First we will loop through the reference table and sum up the quantities for each
	    			//beta zone in the geo region for the industries and the household types. 
	    	    	for (int row = 1; row <= actLocReference.getRowCount(); row++) {
						if (helper.getIndustryNames().contains(actLocReference.getStringValueAt(row, "Activity"))) {
							if (betaZonesInGeoRegion.contains(((Float)actLocReference.getValueAt(row, "ZoneNumber")).intValue())) {
								double [] values = actWithPctDiffs.get(actLocReference.getStringValueAt(row, "Activity"));
								if (values == null){
									 values = new double[2];
									 values[0] = actLocReference.getValueAt(row, "Quantity");
									 actWithPctDiffs.put(actLocReference.getStringValueAt(row, "Activity"), values);
								}else{
									values[0] += actLocReference.getValueAt(row, "Quantity");
								}								
							}
						}else if (actLocReference.getStringValueAt(row, "Activity").startsWith("HH")){
							//We need to count how many HH categories are in the file.
							if (!countedHhs){
								hhCategories.add(actLocReference.getStringValueAt(row, "Activity"));
							}
							if (betaZonesInGeoRegion.contains(((Float)actLocReference.getValueAt(row, "ZoneNumber")).intValue())) {
								double [] values = actWithPctDiffs.get(actLocReference.getStringValueAt(row, "Activity"));
								if (values == null){
									 values = new double[2];
									 values[0] = actLocReference.getValueAt(row, "Quantity");
									 actWithPctDiffs.put(actLocReference.getStringValueAt(row, "Activity"), values);
								}else{
									values[0] += actLocReference.getValueAt(row, "Quantity");
								}
							}
						}
					}
	    	    	
	    	    	//Next we will loop through the calibration table and sum up the quantities for each
	    			//beta zone in the geo region for the industries and the household types. 
					for (int row = 1; row <= actLocCalibration.getRowCount(); row++) {
						if (helper.getIndustryNames().contains(actLocCalibration.getStringValueAt(row, "Activity"))) {
							if (betaZonesInGeoRegion.contains(((Float)actLocCalibration.getValueAt(row, "ZoneNumber")).intValue())) {
								double [] values = actWithPctDiffs.get(actLocCalibration.getStringValueAt(row, "Activity"));
								if (values == null){
									 values = new double[2];
									 values[1] = actLocCalibration.getValueAt(row, "Quantity");
									 actWithPctDiffs.put(actLocCalibration.getStringValueAt(row, "Activity"), values);
								}else{
									values[1] += actLocCalibration.getValueAt(row, "Quantity");
								}
							}
						}else if (actLocCalibration.getStringValueAt(row, "Activity").startsWith("HH")){
							if (betaZonesInGeoRegion.contains(((Float)actLocCalibration.getValueAt(row, "ZoneNumber")).intValue())) {
								double [] values = actWithPctDiffs.get(actLocCalibration.getStringValueAt(row, "Activity"));
								if (values == null){
									 values = new double[2];
									 values[1] = actLocCalibration.getValueAt(row, "Quantity");
									 actWithPctDiffs.put(actLocCalibration.getStringValueAt(row, "Activity"), values);
								}else{
									values[1] += actLocCalibration.getValueAt(row, "Quantity");
								}
							}							
						}
					}
					if(!geoRegActValues.containsKey(geoRegionName)){
						HashMap<String, double[]> yearValues = new HashMap<String, double[]>();
						getInnerHashMapValues(actWithPctDiffs, yearValues, i);
						geoRegActValues.put(geoRegionName, yearValues);
					}else{
						HashMap<String, double[]> yearValues = geoRegActValues.get(geoRegionName);
						getInnerHashMapValues(actWithPctDiffs, yearValues, i);
						
					}										
				}
	    		countedHhs = true;
	    		numHhCategories = hhCategories.size();
	    	}	    		
	    }	

	    private void getInnerHashMapValues(HashMap<String, double[]> hashMapToGetSumsFrom, HashMap<String, double[]>  hashMapToPutPctDiffsIn,
	    									int currentYear){
	    	
	    	//Fist we will loop through the hash map that contains the refSums and the calibSums.
	    	//We will calculate the percent differences from the refSums and calibSums and then
	    	//we will fill the new hash map with those percent differences by year.
	    	for(String key : hashMapToGetSumsFrom.keySet()){
				double[] values = hashMapToGetSumsFrom.get(key);
				double pctChange = (values[1] - values[0])/ values[0];		    			    	
		    	if(hashMapToPutPctDiffsIn.containsKey(key)){
		    		hashMapToPutPctDiffsIn.get(key)[currentYear - startInterval] = pctChange;				    
		    	}else{
		    		double[] calculatedValues = new double[endInterval - startInterval + 1];
		    		calculatedValues[currentYear - startInterval] = pctChange;
		    		hashMapToPutPctDiffsIn.put(key, calculatedValues);		    		
		    	}
			}
	    }

	    private void getFinalTable(){

	    	//Create array of final column headers.
	    	String[] finalTableColumnLabels = new String[endInterval - startInterval + 3]; 
	    	finalTableColumnLabels[0] = "GeographicalRegion";
	    	finalTableColumnLabels[1] = "Activity";			
			for(int i = 0; i < (endInterval - startInterval + 1); i ++){
				String years = "Year_" + (startInterval + i);
		    	finalTableColumnLabels[i+2] = years;
			}
			
			//Print out headers for testing.
			for(String header : finalTableColumnLabels){
				System.out.println("Column header: " + header);									

			}
							    		
			//Creating a 2D array that contains the data that goes into our TableDataSet.
	    	String[][] finalDataIndustry = new String[(helper.getIndustryNames().size()) * EPF_Helper.geoRegionNames.length][finalTableColumnLabels.length];
	    	String[][] finalDataHH = new String[(numHhCategories) * EPF_Helper.geoRegionNames.length][finalTableColumnLabels.length];
	    	System.out.println("The number of rows is: " + finalDataIndustry.length);
			System.out.println("The number of cols is: " + finalDataIndustry[0].length);
	    	System.out.println("The number of rows is: " + finalDataHH.length);
			System.out.println("The number of cols is: " + finalDataHH[0].length);

			int rowCounter = 0;
			int colCounter = 0;
			int rowCounterHH = 0;
			int colCounterHH = 0;
			
    		for (String geoRegion : geoRegActValues.keySet()){
    			System.out.println("The GeoRegion: " + geoRegion);
    			HashMap<String, double[]> activityValues = geoRegActValues.get(geoRegion);
    			System.out.println("Length of activityValues key set: " + activityValues.keySet().size());
	    		for (String activity : activityValues.keySet()){
	    			System.out.print("The Activity: " + activity);
	    			if(activity.startsWith("HH")){
	    				finalDataHH[rowCounterHH][colCounterHH++] = geoRegion;
	    				finalDataHH[rowCounterHH][colCounterHH++] = activity;
	    				for (double value : activityValues.get(activity)){				    				
	    					finalDataHH[rowCounterHH][colCounterHH++] = Double.toString(value);
	    					System.out.println("\t Value in yearValues HashMap is: " + value);
		    			}
    				colCounterHH = 0;
    				rowCounterHH++;
	    			}else{
	    				finalDataIndustry[rowCounter][colCounter++] = geoRegion;
	    				finalDataIndustry[rowCounter][colCounter++] = activity;
	    				for (double value : activityValues.get(activity)){				    				
	    					finalDataIndustry[rowCounter][colCounter++] = Double.toString(value);
	    					System.out.println("\t Value in yearValues HashMap is: " + value);
	    				}
    				colCounter = 0;
    				rowCounter++;	
	    			}


		    	}
	    	}
    		
    		//Create table using data and column labels.
    		pctChangeTableIndustry = TableDataSet.create(finalDataIndustry, finalTableColumnLabels);
    		pctChangeTableHH = TableDataSet.create(finalDataHH, finalTableColumnLabels);
	    }
	    
		    public static void main(String[] args){
		    	File propFile = new File(args[0]);
		        ResourceBundle rb = ResourceUtil.getPropertyBundle(propFile);
		        int startInterval = Integer.parseInt(args[1]);
		        int endInterval = Integer.parseInt(args[2]);
		    	EPF_Helper helper = new EPF_Helper(rb, startInterval, endInterval);
		    	EPF_LineGraphs lineGraphs = new EPF_LineGraphs(helper);
		    	lineGraphs.produceLineGraphs();
		    }    	
		


}
