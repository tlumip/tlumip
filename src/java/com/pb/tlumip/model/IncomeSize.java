package com.pb.tlumip.model;

import java.io.File;
import java.io.IOException;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;


public class IncomeSize {

    String[] incomeSizeLabels = {
		"HH0to5k1to2",
		"HH0to5k3plus",
		"HH5to10k1to2",
		"HH5to10k3plus",
		"HH10to15k1to2",
		"HH10to15k3plus",
		"HH15to20k1to2",
		"HH15to20k3plus",
		"HH20to30k1to2",
		"HH20to30k3plus",
		"HH30to40k1to2",
		"HH30to40k3plus",
		"HH40to50k1to2",
		"HH40to50k3plus",
		"HH50to70k1to2",
		"HH50to70k3plus",
		"HH70kUp1to2",
		"HH70kUp3plus"
    };
    
    
    
    public IncomeSize () {
    }

    

	// return the number of HH income/HH size categories.    
	public int getNumberIncomeSizes() {
		return incomeSizeLabels.length;
	}



	// return the IncomeSize category index given the label.    
	public int getIncomeSizeIndex(String incomeSizeLabel) {
	    
		int returnValue = -1;
	    
		for (int i=0; i < incomeSizeLabels.length; i++) {
			if ( incomeSizeLabel.equalsIgnoreCase( incomeSizeLabels[i] ) ) {
				returnValue = i;
				break;
			}
		}
		
		return returnValue;
	}



	// return the IncomeSize category index given the pums IncomeSize code
	// from the pums person record OCCUP field.    
	public int getIncomeSize(int income, int hhSize) {

	    int returnValue=0;

		
	    // define incomeSize indices for income and hh size ranges.
		if ( income < 5000 ) {
		    if ( hhSize < 3 )
		        returnValue = 0;
		    else
		        returnValue = 1;
		}
		else if ( income < 10000 ) {
			if ( hhSize < 3 )
				returnValue = 2;
			else
				returnValue = 3;
		}
		else if ( income < 15000 ) {
			if ( hhSize < 3 )
				returnValue = 4;
			else
				returnValue = 5;
		}
		else if ( income < 20000 ) {
			if ( hhSize < 3 )
				returnValue = 6;
			else
				returnValue = 7;
		}
		else if ( income < 30000 ) {
			if ( hhSize < 3 )
				returnValue = 8;
			else
				returnValue = 9;
		}
		else if ( income < 40000 ) {
			if ( hhSize < 3 )
				returnValue = 10;
			else
				returnValue = 11;
		}
		else if ( income < 50000 ) {
			if ( hhSize < 3 )
				returnValue = 12;
			else
				returnValue = 13;
		}
		else if ( income < 70000 ) {
			if ( hhSize < 3 )
				returnValue = 14;
			else
				returnValue = 15;
		}
		else {
			if ( hhSize < 3 )
				returnValue = 16;
			else
				returnValue = 17;
		}

		return returnValue;
	}



	// return the IncomeSize category label given the index.    
	public String getIncomeSizeLabel(int incomeSizeIndex) {
		return incomeSizeLabels[incomeSizeIndex];
	}



	// return all the IncomeSize category labels.    
	public String[] getIncomeSizeLabels() {
		return incomeSizeLabels;
	}

	
	
		// return the array of households by Income/HHSize HH Category from the named file
	public int[] getIncSizeHouseholds( String fileName ) {
	 
		String[] formats = { "STRING", "NUMBER" };
		
		// read the base households by incSize file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFileWithFormats( new File( fileName ), formats );
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		// this table has one row of number of households for each incSize category
		String[] tempIncSizeLabels = table.getColumnAsString(1);
		int[] incSize = table.getColumnAsInt(2);
		
		incomeSizeLabels = new String[tempIncSizeLabels.length];

		for (int i=0; i < tempIncSizeLabels.length; i++)
			incomeSizeLabels[i] = tempIncSizeLabels[i];
	    
		return incSize;
	}
	
	
	
}

