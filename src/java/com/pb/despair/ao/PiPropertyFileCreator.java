package com.pb.despair.ao;

import java.io.File;
import java.io.IOException;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * 
 *
 *Written on March 3, 2005
 *@author Christi Willison
 */
public class PiPropertyFileCreator {
	
    String scenarioName;   //name of the current scenario
    String application;    //name of the application you are creating prop file for (i.e. "pi")
	int modulus;		   //interval in which the scenario repeats itself
						   //currently the code ASSUMES that it will repeat at least
						   //every 5 year.
	
	int nYears; 		   //number of years that the model is being run altogether
	
	TableDataSet props;	   //the csv file that contains the skeleton properties
    
    
	public void readFileTemplate (File propTemplate){
	    
	    try {
			props = (CSVFileReader.createReader(propTemplate)).readFile(propTemplate);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(int r=1; r<= props.getRowCount();r++){
		    
		}
	}
	
	public void writePropertyFile(int year){
	    
	    int selector = year % modulus;  //this will produce an integer
	    								//and will be used to determine the case
	    
	    switch(selector){
	    	
	    }
	    
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
	}
}
