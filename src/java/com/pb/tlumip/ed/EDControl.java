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
package com.pb.tlumip.ed;

import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.ModelComponent;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import java.io.*;

public class EDControl extends ModelComponent {

  private static Logger logger = Logger.getLogger("com.pb.tlumip.ed");
  private static int currentYear;
  private static int modelYear;
  private static String defaultDataLocation;
  private static EDDataAccess eda;
  private Model model;
  private Vector splitFiles;

  /**
   * Make settings for database....new version to load from CSV file
   */
  public EDControl(int y, String dl, String xmlf) {
	currentYear = y;
	defaultDataLocation = dl;
	eda = new EDDataAccess();
	ModelDescriptionGetter mdg = new ModelDescriptionGetter(xmlf);
	model = mdg.makeModel();
	splitFiles = new Vector();
  }

    public EDControl(int y, int mYearIndex, String dataFile) {
        this(y, mYearIndex, ResourceUtil.getPropertyBundle(new File(dataFile)));
    }

    //  y = Base year (ex. 1990), mYear = model year index (ex. t=1), ResourceBundle = ed.properties
     public EDControl(int y, int mYear, ResourceBundle rb) {
		String absoluteLocation = new String();
		String marginalLocation = new String();

		splitFiles = new Vector();
        currentYear = y + mYear;
        modelYear = mYear;
        defaultDataLocation = ResourceUtil.getProperty(rb, "defaultDataLocation");
        absoluteLocation = ResourceUtil.getProperty(rb, "absoluteLocation");
        marginalLocation = ResourceUtil.getProperty(rb, "marginalLocation");
        eda = new EDDataAccess(currentYear, defaultDataLocation, absoluteLocation, marginalLocation);
        ModelDescriptionGetter mdg = new ModelDescriptionGetter(ResourceUtil.getProperty(rb,"modelDescriptionFile"));
        model = mdg.makeModel();
        String constructionData = ResourceUtil.getProperty(rb,"constructionData");
        if(logger.isDebugEnabled()) logger.debug("construction data file: "+ constructionData);
        String activityData = ResourceUtil.getProperty(rb, "activityData");
        if(logger.isDebugEnabled()) logger.debug("activity data file: "+ activityData);
        String jobData = ResourceUtil.getProperty(rb, "jobData");
        if(logger.isDebugEnabled()) logger.debug("job data file: "+ jobData);
        splitFiles.add(ResourceUtil.getProperty(rb,"piMini"));
        if(logger.isDebugEnabled()) logger.debug("PI Intermediate File: "+ ResourceUtil.getProperty(rb,"piMini"));
        splitFiles.add(constructionData);
        splitFiles.add(activityData);
        splitFiles.add(jobData);

  }

  /**
   * Returns the EDDataAccess object for accessing the database.
   */
  static EDDataAccess getEDDataAccess() {
    return eda;
  }

  /**
   * Returns the current year this model is projected.
   */
  static int getCurrentYear() {
    return currentYear;
  }

  /**
   * Return the default table of the database.
   */
  static String getDefaultDataLocation() {
    return defaultDataLocation;
  }

  /**
   * Begins running the model and prints the errors and the names of the errors.
   */
  public void startModel() {
      logger.info("Starting ED model in year " + currentYear);
    model.start();
    if(model.hasErrors()) {
      Hashtable e = model.getErrors();
      Enumeration names = e.keys();
      Enumeration errors = e.elements();
      while(names.hasMoreElements()) {
        String s = (String)names.nextElement();
        Exception error = (Exception)errors.nextElement();
        logger.error("Error in SubModel: " + s);
        logger.error(error.getMessage());
      }
    }else if (splitFiles.size() != 0)
    {
        logger.info("Splitting data for application specific outputs");
    	String outFile = new String();
    	String dataFile = new String();
    	int i, lastIndex;
    	
    	for (i=0; i < splitFiles.size(); i++)
    	{
    		dataFile = (String)splitFiles.get(i);
            lastIndex = dataFile.indexOf(".csv") + 4;
    		dataFile = dataFile.substring(0, lastIndex);
    		logger.info("Input File: "+ dataFile);
			
    		outFile = (String)splitFiles.get(i);
            outFile = outFile.substring(lastIndex+1, outFile.indexOf(".csv", lastIndex) + 4);
			outFile = outFile.substring(0, outFile.indexOf("tMY")) + "t" + (modelYear)
						+ outFile.substring(outFile.indexOf("tMY")+3);
			logger.info("Output File: " + outFile);
			try
			{
				CSVSplitter.split(new File(defaultDataLocation), new File(outFile), new File(dataFile), currentYear);
			}catch(Exception e)
			{
				e.printStackTrace();
			}
    	}
    }
  }

    public void startModel(int t) {
        startModel();
    }

    public static void main(String[] args) {
    	final int ITERATIONS = 1;
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/eclipse/workspace/tlumip/config/ed/ed.properties"));
        for (int i=0; i < ITERATIONS; i++)
        {
        	EDControl ed = new EDControl(1990,i,rb);
        	ed.startModel();
        }
    }

}
