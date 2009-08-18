package com.pb.tlumip.epf.Calibration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;


import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public class EPF_DotMaps {
	protected static Logger logger = Logger.getLogger(EPF_DotMaps.class);
	private final static String FILE_YEAR_TOKEN = "@@epfyear@@";
	private final static String FILE_SUB_DIRECTORY_TOKEN = "@@subDirectory@@";
    
	EPF_Helper helper;
    int startInterval;
    int endInterval; 
	
	TableDataSet actLocReference;
	TableDataSet actLocCalibration;
	TableDataSet industryTable;
	TableDataSet hhTable;	
	
	boolean countedHhName = false;
	List<String> hhNames = new ArrayList<String>();
	
	HashMap<String, HashMap<String, Double>> betaZoneActQty;
	
	public EPF_DotMaps(EPF_Helper helper){
		this.helper = helper;
        startInterval = helper.getStartInterval();
        endInterval = helper.getEndInterval();
    }

    public void produceDotMaps(){
    	
    	//get the beta zone activity quantity hash maps for both HH and Industries.
    	getBetaZoneActQtyHashMap(actLocCalibration, helper.scenarioName);
    	getBetaZoneActQtyHashMap(actLocReference, helper.referenceScenarioName); 
    	
    	//call the R script that produces the 2D and then the 3D arrays that are used to create the 
        //Dot Maps.
    	helper.runRScript("run.array.build.Rscript");
    	System.out.println("The Arrays have been built.");
    	
        
    	//call the R script that uses the previous arrays to create the Dot Maps.
    	//This script is specific to mapping the state of OR and the halo regions.
    	helper.runRScript("run.map.plotter.Rscript");
    	System.out.println("The plots have been built.");
    	
    }   	
	
	private void getBetaZoneActQtyHashMap(TableDataSet nameOfTableDataSet, String subDirectory){
       	
    	//Create a hash map with the keys being the beta zones and the values being a hash map.
    	//This inner hash map has the keys being the activities and the values being the quantities
    	//from the ActivityLocations.csv file.
    	betaZoneActQty = new HashMap<String, HashMap<String, Double>>();
    	
    	for (int i = startInterval; i <= endInterval; i++){
    		nameOfTableDataSet = helper.loadTableDataSet(i, subDirectory);
//    		actLocCalibration = helper.loadTableDataSet(i, false);    	

    			//Create a hash map with the keys as the activities and the values as the quantities
    			//for each betaZone.
    			HashMap<String, Double> actWithQty = new HashMap<String, Double>();
    			
    			//First we will loop through the reference table and get the betaZone. We will then
    			//check to see if the zone exist in the hash map if it does then
    			//we will pull it and check to see if the inner hash map has the associated key,
    			//if it does then we will check the quantity.  If the quantity is null we will add the values 
    			//for the inner hash map. If the zone does not exist we will add it and the inner hash map.
    			System.out.println("The scenario being run: Year" + i);
    	    	for (int row = 1; row <= nameOfTableDataSet.getRowCount(); row++) {
    	    		String zoneNumber = nameOfTableDataSet.getStringValueAt(row, "ZoneNumber");
    	    		if(!betaZoneActQty.keySet().contains(zoneNumber)){
    	    			betaZoneActQty.put(zoneNumber, new HashMap<String, Double>());
    	    		}
    	    		actWithQty = betaZoneActQty.get(zoneNumber);
    	    		double value = nameOfTableDataSet.getValueAt(row, "Quantity");
//    	    		System.out.println("The Activity: " + nameOfTableDataSet.getStringValueAt(row, "Activity"));
//    	    		System.out.println("The Quantity: " + value);
    	    		actWithQty.put(nameOfTableDataSet.getStringValueAt(row, "Activity"), value);
    	    		if (nameOfTableDataSet.getStringValueAt(row, "Activity").startsWith("HH")){
    	    			String hhName = nameOfTableDataSet.getStringValueAt(row, "Activity");
//    	    			System.out.println("The hh name is: " + hhName);
    	    			if (!countedHhName && !hhNames.contains(hhName)){
							hhNames.add(hhName);   	    			
						}
    	    		}

    	    	}
    	    	countedHhName = true;
    	    	
    	    	//get the tableDataSets that will be use to create the output files.
    	    	getFinalTable();
    	    	
    	    	//Write the output table with only the industries in it.
    	    	helper.writeOutputFile(industryTable, "calibration.beta.zones.table.industry", 
    	    			new String[]{FILE_YEAR_TOKEN, FILE_SUB_DIRECTORY_TOKEN}, new String[]{((Integer) i).toString(), subDirectory});
    	    	
    	    	//Write the output table with only the house holds in it.
    	    	helper.writeOutputFile(hhTable, "calibration.beta.zones.table.hh", 
    	    			new String[]{FILE_YEAR_TOKEN, FILE_SUB_DIRECTORY_TOKEN}, new String[]{((Integer) i).toString(), subDirectory});
    	    }
    	}
	
    private void getFinalTable(){

    	//Create array of final column headers.
    	String[] industryColumnHeader = new String [helper.getIndustryNames().size() + 2];
    	String[] hhColumnHeader = new String [hhNames.size() + 2];
    	
//    	for(String hh : hhNames){
//			System.out.println("Column header: " + hh);
//		}
    	
    	//print array size for test.
//    	System.out.println("The size of industry names list: " + industryColumnHeader.length);
//      System.out.println("The size of hh names list: " + hhColumnHeader.length);
    	   	
    	getColumnHeaders(helper.getIndustryNames(), industryColumnHeader);
    	getColumnHeaders(hhNames, hhColumnHeader);
    	
//    	//Print out headers for testing.
//    	for(String header : industryColumnHeader){
//			System.out.println("Column header: " + header);
//		}
//		for(String header : hhColumnHeader){
//			System.out.println("Column header: " + header);
//		}
		
		//Creating a 2D array that contains the data that goes into our TableDataSet.
    	String[][] finalDataIndustry = new String[betaZoneActQty.keySet().size()][industryColumnHeader.length];
    	String[][] finalDataHH = new String[betaZoneActQty.keySet().size()][hhColumnHeader.length];
//    	System.out.println("The number of rows is: " + finalDataIndustry.length);
//		System.out.println("The number of cols is: " + finalDataIndustry[0].length);
//    	System.out.println("The number of rows is: " + finalDataHH.length);
//		System.out.println("The number of cols is: " + finalDataHH[0].length);

		get2DArray(finalDataIndustry, helper.industryNames);
		get2DArray(finalDataHH, hhNames);
		
		//Create table using data and column labels.
    	industryTable = TableDataSet.create(finalDataIndustry, industryColumnHeader);
		hhTable = TableDataSet.create(finalDataHH, hhColumnHeader);		
		
    }
    
    //Create a 2D array where the rows contain the zone number, the activity quantities, and a total.
    private void get2DArray(String[][] nameOfTwoDArray, List<String> listOfActNames){
		int rowCounter = 0;
		int employees = Integer.parseInt( helper.appRb.getString( "calibration.dollar.to.employee.factor") );
		
		for (String betaZone : betaZoneActQty.keySet()){
			double totalEmp = 0;
//			System.out.println("The Beta Zone: " + betaZone);
			HashMap<String, Double> activityQuantities = betaZoneActQty.get(betaZone);
//			System.out.println("Length of Activity Quantities key set: " + activityQuantities.keySet().size());
    		for (String activity : activityQuantities.keySet()){
    			if(listOfActNames.contains(activity)){
//    				System.out.print("The Activity: " + activity + "\n");
    				int listPosition = listOfActNames.indexOf(activity);
    				nameOfTwoDArray[rowCounter][0] = betaZone;
    				if(nameOfTwoDArray.equals(helper.industryNames)){
    					nameOfTwoDArray[rowCounter][listPosition + 1] = Double.toString(activityQuantities.get(activity) / employees);
    				}
    				nameOfTwoDArray[rowCounter][listPosition + 1] = Double.toString(activityQuantities.get(activity));
//    				System.out.println("The quantity: " + activityQuantities.get(activity));
    				totalEmp += activityQuantities.get(activity);
    				nameOfTwoDArray[rowCounter][nameOfTwoDArray[0].length - 1] = Double.toString(totalEmp);
//    				System.out.println("The total for " + betaZone + " is " + totalEmp + "\n");	
    			}
    		}
			rowCounter++;			
		}
    }

	private void getColumnHeaders(List<String> activitiesList, String[] newArrayName){
		//fill the array with the column headers.
		newArrayName[0] = " ";
		newArrayName[newArrayName.length - 1] = "Total";
		for(int i = 0; i < (activitiesList.size()); i++){
			newArrayName[i + 1] = activitiesList.get(i);
		}		
//		//Print out headers for testing.
//		for(String header : newArrayName){
//			System.out.println("Column header: " + header);									
//		}
	}
	
    public static void main(String[] args){
    	File propFile = new File(args[0]);
        ResourceBundle rb = ResourceUtil.getPropertyBundle(propFile);
        int startInterval = Integer.parseInt(args[1]);
        int endInterval = Integer.parseInt(args[2]);
    	EPF_Helper helper = new EPF_Helper(rb, startInterval, endInterval);
    	EPF_DotMaps dotMaps = new EPF_DotMaps(helper);
    	dotMaps.produceDotMaps();
    }  

}
