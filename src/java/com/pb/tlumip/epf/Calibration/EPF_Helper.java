package com.pb.tlumip.epf.Calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.util.ResourceUtil;


public class EPF_Helper {
	protected static Logger logger = Logger.getLogger(EPF_Helper.class);
    int startInterval;
    int endInterval;
    
    ResourceBundle appRb;
    
	TableDataSet alpha2Beta;
	TableDataSet bzonesOrClip;
	
	TableDataSet actLocReference;
	TableDataSet actLocCalibration;
	
	List<String> industryNames ;
	
	public String scenarioName;
	public String referenceScenarioName;
	
	static String[] geoRegionNames = new String[]{"Halo Total", "IDBoise", "WA-Tri-Cities", "OR-CarkCo", "OR-WV", "OR-Bend"};
	
	public EPF_Helper(ResourceBundle appRb, int start, int end){
        this.appRb = appRb;
        startInterval = start;
        endInterval = end;
        scenarioName = appRb.getString("scenario.name");
        referenceScenarioName = appRb.getString("reference.scenario.name");
        
    }
	
	private void readAlpha2BetaFile(){
		//Read in the alpha2beta.csv
	    logger.info("Reading in the alpha2beta.csv");
	    alpha2Beta = TableDataSetLoader.loadTableDataSet(appRb, "calibration.alpha2Beta");
	}

	private void readBZonesClipFile(){
	    //Read in the bzones_OR_clip.csv
	    logger.info("Reading in the bzones_OR_clip.csv");
	    bzonesOrClip = TableDataSetLoader.loadTableDataSet(appRb, "calibration.bzonesOrClip");
	}

    private void readIndustryTypeFile(){
	    //Read in the IndustryType.csv to get the list of industry names.
	    logger.info("Reading in the IndustryType.csv");
	    TableDataSet industryType = TableDataSetLoader.loadTableDataSet(appRb, "calibration.industryType");
	    industryNames =  Arrays.asList(industryType.getColumnAsString("Industry"));
    }

	public TableDataSet getAlpha2Beta() {
		if (alpha2Beta == null){
			readAlpha2BetaFile();
		}
		return alpha2Beta;
	}

	public TableDataSet getBzonesOrClip() {
		if (bzonesOrClip == null){
			readBZonesClipFile();
		}
		return bzonesOrClip;
	}

	public TableDataSet getActLocReference() {
		return actLocReference;
	}

	public TableDataSet getActLocCalibration() {
		return actLocCalibration;
	}

	public List<String> getIndustryNames() {
		if (industryNames == null){
			readIndustryTypeFile();
		}
		return industryNames;
	}
	
	public int getStartInterval() {
		return startInterval;
	}
	
	public int getEndInterval() {
		return endInterval;
	}
	
	public TableDataSet loadTableDataSet(int currentYear, String subDirectory){
		String rootDirectory = ResourceUtil.getProperty(appRb, "root.dir");
		String fileName = ResourceUtil.getProperty(appRb, "activity.locations.file.name");
		
		String path = rootDirectory + subDirectory + "/t" + currentYear + "/" + fileName;
         TableDataSet table =null;
         try {
             CSVFileReader reader = new CSVFileReader();
             table = reader.readFile(new File(path));

         } catch (IOException e) {
            throw new RuntimeException("Can't find input table "+path, e);
         }
         return table;
    }
	
	public void writeOutputFile(TableDataSet tableToBeOutput, String outputPathName){
		String outputPath = appRb.getString(outputPathName);
		CSVFileWriter fileWriter = new CSVFileWriter();
		try {
			fileWriter.writeFile(tableToBeOutput, new File(outputPath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}

	public void writeOutputFile(TableDataSet tableToBeOutput, String outputPathName, String[] tokens, String[] values){
		if (tokens.length != values.length)
			throw new IllegalArgumentException("Tokens and values must be of the same length.");
		String outputPath = appRb.getString(outputPathName);
		for(int i = 0; i < tokens.length; i++)
			outputPath = outputPath.replace(tokens[i], values[i]);
		CSVFileWriter fileWriter = new CSVFileWriter();
		try {
			fileWriter.writeFile(tableToBeOutput, new File(outputPath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}
	
	public void runRScript(String rScriptToBeRun){
		
		String command = appRb.getString(rScriptToBeRun);
		Runtime rt = Runtime.getRuntime();
		try {
		    Process process = rt.exec(command);
		    try {
		    	String a;
		    	BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()),5000); 
		    	while ( (a = in.readLine()) != null)  
		    		System.out.println(a);
		    	in = new BufferedReader(new InputStreamReader(process.getErrorStream()),5000); 
		    	while ( (a = in.readLine()) != null)  
		    		System.out.println(a);
		        process.waitFor();
		    } catch (InterruptedException e) {
		        logger.fatal(e);
		        throw new RuntimeException(e);
		    }
		} catch (IOException e) {
		    logger.fatal(e);
		    throw new RuntimeException(e);
		}
	}

}
