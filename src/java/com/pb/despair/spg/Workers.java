package com.pb.despair.spg;

import java.io.File;
import java.io.IOException;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;


public class Workers {

    String[] workersLabels = {
    		"0",
    		"1",
    		"2",
    		"3",
    		"4",
    		"5",
    		"6",
    		"7",
    		"8",
    		"9",
    		"10+"
        };
        
        
        
    String[] fixedCategories = {
    		"0",
    		"1",
    		"2",
    		"3",
    		"4",
    		"5+"
        };
        
        
        
    public Workers () {
    }

    

	// return the number of HH workers categories.    
	public int getNumberWorkers() {
		return workersLabels.length;
	}



	// return the workers category index given the pums number of workers code
	// from the pums person record RLABOR field.    
	public int getWorkers(int workers) {

		int returnValue=0;

		if ( workers < 10 )
			returnValue = workers;
		else
		    returnValue = 10;

		
		return returnValue;
	}



	// return the workers category label given the index.    
	public String getWorkersLabel(int workersIndex) {
		return workersLabels[workersIndex];
	}



	// return all the workers category labels.    
	public String[] getWorkersLabels() {
		return workersLabels;
	}



	// return the array of households by number of workers from the named file
	public int[] getWorkersPerHousehold( String fileName ) {
	 
		String[] formats = { "STRING", "NUMBER" };
		
		// read the base households by number of workers file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFileWithFormats( new File( fileName ), formats );
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		// this table has one row of number of households for each workers per household category
		String[] tempWorkersLabels = table.getColumnAsString(1);
		int[] workers = table.getColumnAsInt(2);
		
		workersLabels = new String[tempWorkersLabels.length];

		for (int i=0; i < tempWorkersLabels.length; i++)
		    workersLabels[i] = tempWorkersLabels[i];
	    
		return workers;
	}
	
	
	// return the proportions of worker categories relative to total employed households
	public float[] getWorkersPerHouseholdProportions( int[] workersPerHousehold ) {
		
		float[] proportions = new float[fixedCategories.length];
		float[] tempWorkers = new float[fixedCategories.length];
		float totalEmployedHouseholds = 0;
		
		// workers in employed households start at workers category 1
		for (int i=1; i < workersPerHousehold.length; i++) {
			totalEmployedHouseholds += workersPerHousehold[i];
			if (i < fixedCategories.length - 1)
				tempWorkers[i] = workersPerHousehold[i];
			else
				tempWorkers[fixedCategories.length - 1] += workersPerHousehold[i];
		}
		
		
		// calculate proportions of workers in employed households
		for (int i=1; i < fixedCategories.length; i++)
			proportions[i] = tempWorkers[i]/totalEmployedHouseholds;
		
		return proportions;
	}
	
	
	
}

