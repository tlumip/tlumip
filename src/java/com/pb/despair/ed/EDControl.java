
/**
* EDControl makes calls to build the model and then run the model.
 */
package com.pb.despair.ed;

import com.pb.common.util.Debug;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.ModelComponent;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import java.io.*;

public class EDControl extends ModelComponent {

  private static Logger logger = Logger.getLogger("com.pb.despair.ed");
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

    //  y = Base year (ex. 1990), modelYearIndex = model year index (ex. t=1), datafile = ed.properties
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
        logger.info("construction data file: "+ constructionData);
        String activityData = ResourceUtil.getProperty(rb, "activityData");
        logger.info("activity data file: "+ activityData);
        String jobData = ResourceUtil.getProperty(rb, "jobData");
        logger.info("job data file: "+ jobData);
        splitFiles.add(ResourceUtil.getProperty(rb,"piMini"));
        logger.info("PI Intermediate File: "+ ResourceUtil.getProperty(rb,"piMini"));
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
    model.start();
    if(model.hasErrors()) {
      Hashtable e = model.getErrors();
      Enumeration names = e.keys();
      Enumeration errors = e.elements();
      while(names.hasMoreElements()) {
        String s = (String)names.nextElement();
        Exception error = (Exception)errors.nextElement();
        Debug.println("Error in SubModel: " + s);
        Debug.println(error.getMessage());
      }
    }else if (splitFiles.size() != 0)
    {
    	String outFile = new String();
    	String dataFile = new String();
    	int i, lastIndex;
    	
    	for (i=0; i < splitFiles.size(); i++)
    	{
    		dataFile = (String)splitFiles.get(i);
            logger.info("datafile: "+ dataFile);
			lastIndex = dataFile.indexOf(".csv") + 4;
    		dataFile = dataFile.substring(0, lastIndex);
  
			outFile = (String)splitFiles.get(i);
            logger.info("outfile: " + outFile);
			outFile = outFile.substring(lastIndex+1, outFile.indexOf(".csv", lastIndex) + 4);
			outFile = outFile.substring(0, outFile.indexOf("tMY")) + "t" + (modelYear)
						+ outFile.substring(outFile.indexOf("tMY")+3);
			logger.info("outfile: " + outFile);
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
