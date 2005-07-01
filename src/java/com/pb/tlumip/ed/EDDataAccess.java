
/**
 * Company:      ECONorthwest<p>
 * Class to interface the database.  Will probably use Tim's code for this in
 * the future.
 */
package com.pb.tlumip.ed;

//import com.pb.common.dataBeans;
//import com.pb.common.util.Debug;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import java.io.File;
import java.util.Hashtable;
import org.apache.log4j.Logger;

public class EDDataAccess {

String defaultLocation;
int defaultYear;
Hashtable dataSet;
Hashtable absoluteOverride;
Hashtable marginalOverride;
TableDataSet csvSet;
private static Logger logger = Logger.getLogger("com.pb.tlumip.ed");

EDDataAccess(int defaultYear, String defaultLocation, String absoluteLocation, String marginalLocation) {
	int i, j;
	
  try {
	absoluteOverride = new Hashtable();
	marginalOverride = new Hashtable();
	dataSet = new Hashtable();
	Hashtable tmpSet;

	//  Plus we need to do the same thing for an absolute override and a marginal override
	//  for the data...
	try
	{
	csvSet = new CSVFileReader().readFile(new File(marginalLocation));
	for (i=2; i<csvSet.getColumnCount()+1; i++)
	{
		marginalOverride.put(csvSet.getColumnLabel(i), new Hashtable());
		tmpSet = (Hashtable)marginalOverride.get(csvSet.getColumnLabel(i));
		for (j=1; j<csvSet.getRowCount()+1; j++)
		{
			tmpSet.put(Integer.toString((int)csvSet.getValueAt(j, 1)),
						Float.toString(csvSet.getValueAt(j, i)));
		}
	}
	}catch(Exception e) {}

	try
	{
	csvSet = new CSVFileReader().readFile(new File(absoluteLocation));
	for (i=2; i<csvSet.getColumnCount()+1; i++)
	{
		absoluteOverride.put(csvSet.getColumnLabel(i), new Hashtable());
		tmpSet = (Hashtable)absoluteOverride.get(csvSet.getColumnLabel(i));
		for (j=1; j<csvSet.getRowCount()+1; j++)
		{
			tmpSet.put(Integer.toString((int)csvSet.getValueAt(j, 1)),
						Float.toString(csvSet.getValueAt(j, i)));
		}
	}
	}catch(Exception e) {}
	
	csvSet = new CSVFileReader().readFile(new File(defaultLocation));
	//  Skip the year...
	for (i=2; i<csvSet.getColumnCount()+1; i++)
	{
		
		// Here's how the hashtable works:  Each variable (GDP, inflation, etc.)
		// is the key to another hashtable which has keys based on the year that give
		// access to the data...note that we assume that the first column is the year...
		dataSet.put(csvSet.getColumnLabel(i), new Hashtable());
		tmpSet = (Hashtable)dataSet.get(csvSet.getColumnLabel(i));
		for (j=1; j<csvSet.getRowCount()+1; j++)
		{
			tmpSet.put(Integer.toString((int)csvSet.getValueAt(j, 1)),
						Float.toString(csvSet.getValueAt(j, i)));
		}
	}

    this.defaultLocation = defaultLocation;
    this.defaultYear = defaultYear;
    logger.info("Default location:  " + defaultLocation + "  default year:  " + defaultYear);
    //Debug.println("Default location:  " + defaultLocation + "    default year:   " + defaultYear);
  } catch(Exception e) {
    e.printStackTrace();
  }
}

EDDataAccess() {
  this(EDControl.getCurrentYear(), EDControl.getDefaultDataLocation(), null, null);
}

double getValue(String name, String location, int year) throws Exception {
	String r;
	//logger.info("EDDataAccess:  getting data");
	//Debug.println("EDDataAccess:  getting data");
	
	if (name.equals("one"))
		return 1.;
	
	if (location == defaultLocation)
	{
		try
		{
			r = (String) ((Hashtable)dataSet.get(name)).get(Integer.toString(year));
			if (r != null)	{
				return new Double(r).doubleValue();
			}else {
				logger.warn("Data does not exist:  Looking for " + name + " in " + year);
				//Debug.println("Data does not exist:  Looking for " + name + " in " + year);
				throw new Exception("Data does not exist:  Looking for " + name + " in " + year);
			}
		}catch (Exception e)
		{
			logger.fatal("Big problem accessing hash table.  " + name);
			e.printStackTrace();
			//Debug.println("Danger Will Robinson, Danger!  " + name);
		}
	}else
	{
		try
		{
			TableDataSet smallSet = new CSVFileReader().readFile(new File(location));
			if (smallSet.getColumnPosition(name) == -1)
				throw new Exception("Could not find column " + name + " in file " + location);
			
			for (int i=2; i < smallSet.getRowCount()+1; i++)
			{
				if ((int)smallSet.getValueAt(i, 1) == year)
				{
					if ((Hashtable)dataSet.get(name) == null)
						dataSet.put(name, new Hashtable());
						
					((Hashtable)dataSet.get(name)).put(Integer.toString(year), Float.toString(smallSet.getValueAt(i, name)));
					return (double)smallSet.getValueAt(i, name);
				}
			}
			
			throw new Exception("Could not find data for " + name + " in file " + location);
		}catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	return 0;
}

double getValue(String name) throws Exception {
  return getValue(name, defaultLocation,defaultYear);
}


void insertValue(String name, double d, int year, String location) {
  float[] newColumn = new float[csvSet.getRowCount()];
  int i;
  
  //logger.info("EDDataAccess: setting data");
  //Debug.println("EDDataAccess: setting data");

  if (marginalOverride.get(name) != null && ((Hashtable)marginalOverride.get(name)).get(Integer.toString(year)) != null)
	d += new Double((String)((Hashtable)marginalOverride.get(name)).get(Integer.toString(year))).doubleValue();
  
  if (absoluteOverride.get(name) != null &&( (Hashtable)absoluteOverride.get(name)).get(Integer.toString(year)) != null)
  {
	double d1;
	d1 = new Double((String)((Hashtable)absoluteOverride.get(name)).get(Integer.toString(year))).doubleValue();
	if (d1 != -1)
		d = d1;
  }

  if (dataSet.get(name) == null)
  {
  	dataSet.put(name, new Hashtable());
  	((Hashtable)dataSet.get(name)).put(Integer.toString(year), Double.toString(d));
  	for (i=0; i < csvSet.getRowCount(); i++)
  	{
  		//  Gotta convert to a 1-based table
  		if ((int)csvSet.getValueAt(i+1, 1) != year)
  			newColumn[i] = 0;
  		else
  			newColumn[i] = (float)d;
  	}
  	
	csvSet.appendColumn(newColumn, name);
  }else
  {
	((Hashtable)dataSet.get(name)).put(Integer.toString(year), Double.toString(d));
	
	//  This table is 1-based...
	for (i=1; i < csvSet.getRowCount()+1; i++)
	{
		if ((int)csvSet.getValueAt(i, 1) == year)
		{
			csvSet.setValueAt(i, csvSet.getColumnPosition(name), (float) d);
		}
	}
  }
  
  try {
	CSVFileWriter csvw = new CSVFileWriter();
	csvw.writeFile(csvSet, new File(defaultLocation), new java.text.DecimalFormat("0.#########"));
	logger.info("Writing final CSV file...");
	//Debug.println("Writing final CSV file...");
  }catch (Exception e) {
  	e.printStackTrace();
  }
}

void insertValue(String name, double d, int year) {
  insertValue(name,d,year, defaultLocation);
}

void insertValue(String name, double d) {
  insertValue(name,d,defaultYear,defaultLocation);
}


}
