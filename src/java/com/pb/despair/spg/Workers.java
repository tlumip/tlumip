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
	 
		// read the base households by number of workers file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		// this table has one row of number of households for each workers per household category
		String[] tempWorkersLabels = table.getColumnAsString(1);
		int[] workers = table.getColumnAsInt(2);
		
		tempWorkersLabels = new String[tempWorkersLabels.length+1];
		for (int i=0; i < tempWorkersLabels.length; i++)
		    workersLabels[i] = tempWorkersLabels[i];
	    
		return workers;
	}
	
	
}

