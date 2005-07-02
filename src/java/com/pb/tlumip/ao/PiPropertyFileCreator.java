/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
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
