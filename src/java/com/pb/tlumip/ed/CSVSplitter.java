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

/**
 * Company:      ECONorthwest<p>
 * CSVSplitter is used to read in CSV files and split them into multiple output
 * files.
 */

package com.pb.tlumip.ed;
//import com.pb.common.util.Debug;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

public class CSVSplitter  {
	private static Logger logger = Logger.getLogger("com.pb.tlumip.ed");

  public CSVSplitter() {}

  //  infile = where the data is coming from, outfile is where the data is going to
  //  datafile = the file that tells how to split the data, year = current year
  public static void split(File infile, File outfile, File datafile, int year) throws Exception {
	int i,nyear;
	String lastSector;
	float tmpVal;
	TableDataSet dataSet, inSet, outSet;
	float collection[];
	EDControl ed;
	EDDataAccess eda;

	nyear=year;
  	
	// Initialize everything...
	try
	{
		dataSet = new CSVFileReader().readFile(datafile);
		inSet = new CSVFileReader().readFile(infile);
		outSet = new TableDataSet();
		collection = new float[dataSet.getRowCount()];
	}catch (IOException e)
	{
		logger.fatal("Could not read data file!");
		e.printStackTrace();
		return;
	}

	// Figure out which is the correct row to be reading from (we need to make sure we're getting
	// the data for the correct year
	for (i=1; i<inSet.getRowCount()+1; i++)
	{
		if ((int)inSet.getValueAt(i, 1) == year)
		{
			nyear = i;
			break;
		}
	}
	
	if (nyear != i)
		throw new Exception("Could not find correct year in file...");

	// Transfer over the data to a new data set...now with real anti-duplicate action!
	Vector v = new Vector();
	String[] namedata = dataSet.getColumnAsString(2);
	int offset = 0;

	lastSector = null;
	for (i=1; i < dataSet.getRowCount()+1; i++)
	{
		if (lastSector != dataSet.getStringValueAt(i,1))
			lastSector = dataSet.getStringValueAt(i, 1);

		if (dataSet.getColumnType()[dataSet.getColumnCount()-1] == com.pb.common.datafile.DataTypes.NUMBER)
		{
			tmpVal = dataSet.getValueAt(i, dataSet.getColumnCount());
		}else
		{
			ed = new EDControl(year, infile.getAbsolutePath(), 
								dataSet.getStringValueAt(i, dataSet.getColumnCount()));
			ed.startModel();
			eda = EDControl.getEDDataAccess();
			tmpVal = (float)eda.getValue(dataSet.getStringValueAt(i,2));
		}

		if (!v.contains(namedata[i-1]))
		{
			v.add(namedata[i-1]);
			if (dataSet.getStringValueAt(i, 1).equals("one"))
				collection[i-1-offset] = tmpVal;
			else
				collection[i-1-offset] = tmpVal * inSet.getValueAt(nyear, inSet.getColumnPosition(dataSet.getStringValueAt(i, 1)));
		}
		else
		{
			offset++;
			if (dataSet.getStringValueAt(i, 1).equals("one"))
				collection[i-1-offset] = tmpVal;
			else
				collection[v.indexOf(namedata[i-1])] += tmpVal * inSet.getValueAt(nyear, inSet.getColumnPosition(dataSet.getStringValueAt(i, 1)));
		}
	}
	
	float[] resizedCollection = new float[v.size()];
	for (i=0; i<v.size(); i++)
		resizedCollection[i] = collection[i];

	outSet.appendColumn(v.toArray(new String[1]), dataSet.getColumnLabel(2));
	outSet.appendColumn(resizedCollection, dataSet.getColumnLabel(dataSet.getColumnCount()));
	
	// Now write out that data!
	try
	{
        (new CSVFileWriter()).writeFile(outSet, outfile, new DecimalFormat("#.##########"));
	}catch (IOException e)
	{
			logger.fatal("Problem working with the outfile");
			e.printStackTrace();
			return;
	}
  }
}