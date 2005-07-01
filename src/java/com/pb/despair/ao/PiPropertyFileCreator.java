package com.pb.tlumip.ao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
	
	
	TableDataSet props;	   //the csv file that contains the skeleton properties
    
    
	public void readFileTemplate (File propTemplate){
	    
	    try {
			props = (CSVFileReader.createReader(propTemplate)).readFile(propTemplate);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void writePropertyFile(){
	    
	    try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("/models/tlumip/scenario_boomBaby1/t1/pi"));
        } catch (IOException e) {
            e.printStackTrace();
        }
	    
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
	}
}
