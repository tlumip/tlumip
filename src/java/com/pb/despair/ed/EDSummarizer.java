package com.pb.despair.ed;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Iterator;

/**
 * Author: willison
 * Date: Nov 22, 2004
 * <p/>
 * Created by IntelliJ IDEA.
 */
public class EDSummarizer {
    static Logger logger = Logger.getLogger("com.pb.despair.ed");
    static String[] header = null;
    static float[][] outputValues = null;


    public static TableDataSet summarize(String output, int timeInterval, int baseYear, ResourceBundle appRb, ArrayList colHeaders){

        //Read the model_data.csv file that is ED's output file so that we can process the data into a calibration file
        File resultsFile = new File(ResourceUtil.getProperty(appRb, "defaultDataLocation"));
        TableDataSet model_data = readModelDataFile(resultsFile);


        TableDataSet dataSet = null;

        if(output.equalsIgnoreCase("oregon.activity")){
            dataSet = summarizeOregonActivity(timeInterval, baseYear, model_data, colHeaders);
        }else if(output.equalsIgnoreCase("oregon.employment"))
            dataSet = summarizeOregonEmployment(timeInterval, baseYear, model_data);
        else if(output.equalsIgnoreCase("odot.employment"))
            dataSet = summarizeODOTEmployment(timeInterval, baseYear, model_data, colHeaders);
        else
            logger.warning("No method has been developed to summarize " + output + ".  ED can" +
                    "currently produce the oregon activity, oregon employment and odot employement summaries -" +
                    "check calibration.properties");
        return dataSet;
    }

    private static TableDataSet summarizeOregonActivity(int timeInterval, int baseYear, TableDataSet model_data, ArrayList actHeaders){

        actHeaders.add(0,"year");
        float[][] values = new float[timeInterval][actHeaders.size()];

        int startIndex = findRowInTable(model_data, baseYear+1);

        for(int r=0; r< values.length; r++){
            for(int c=0; c< values[0].length; c++){
                values[r][c] =  model_data.getValueAt(startIndex+r, (String)actHeaders.get(c));
            }
        }

        TableDataSet data = TableDataSet.create(values,actHeaders);
        return data;
    }

    private static TableDataSet summarizeOregonEmployment(int timeInterval, int baseYear, TableDataSet model_data){

        //First get the headers that you need out of the model_data file.  The
        //headers are unique in that they all start with OEMP so there is no need to list
        //them in the calibration.properties file the way we do with the activity headers.
        String[] model_dataHeaders = model_data.getColumnLabels();
        ArrayList empHeader = new ArrayList();
        empHeader.add(0,"year");
        int count = 1;
        for(int i=1; i< model_dataHeaders.length; i++){
            if(model_dataHeaders[i].startsWith("OEMP")){       // all headers starting with "OEMP" need to be added to
                empHeader.add(count, model_dataHeaders[i]);    // the employment calibration file
                count++;
            }
        }

        //figure out which row we need to extract data from
        int startIndex = findRowInTable(model_data, baseYear+1);

        //create an array of values
        float[][] values = new float[timeInterval][empHeader.size()];

        //fill up the values array
        for(int r=0; r< values.length; r++){
            for(int c=0; c< values[0].length; c++){
                values[r][c] =  model_data.getValueAt(startIndex+r, (String)empHeader.get(c));
            }
        }

        //pass back the values.
        TableDataSet data = TableDataSet.create(values,empHeader);
        return data;

    }

    private static TableDataSet summarizeODOTEmployment(int timeInterval, int baseYear, TableDataSet model_data, ArrayList odotHeader){
        //This is similar to the oregon employment except that certain columns will be summed together
        //for example ACCOMMODATION + PERSONAL SERVICES + HOMEBASED SERVICES will be one category called
        //OEMPACCPERSHOME instead of the three separate categories.
        //We start out by adding year to our list of column headers
        odotHeader.add(0,"year");

        //define our array of values
        float[][] values = new float[timeInterval][odotHeader.size()];

        //figure out where to start
        int startIndex = findRowInTable(model_data, baseYear+1);

        //fill up the values array
        for(int r=0; r< values.length; r++){
            for(int c=0; c< values[0].length; c++){
                if(c==1) values[r][c] = model_data.getValueAt(startIndex+r, "OEMPACC") +
                        model_data.getValueAt(startIndex+r, "OEMPPERS") + model_data.getValueAt(startIndex+r, "OEMPHOME");
                else if (c==10) values[r][c] = model_data.getValueAt(startIndex+r, "OEMPHIED") +
                        model_data.getValueAt(startIndex+r, "OEMPLOED");
                else if (c==12) values[r][c] = model_data.getValueAt(startIndex+r, "OEMPPAP") +
                        model_data.getValueAt(startIndex+r, "OEMPOND");
                else if(c==14) values[r][c] = model_data.getValueAt(startIndex+r, "OEMPTRAN") +
                        model_data.getValueAt(startIndex+r, "OEMPCOM");
                else values[r][c] =  model_data.getValueAt(startIndex+r, (String)odotHeader.get(c));
            }
        }

        //pass back the values.
        TableDataSet data = TableDataSet.create(values,odotHeader);
        return data;


    }



    private static TableDataSet readModelDataFile (File dataFile){
        CSVFileReader reader = new CSVFileReader();
        TableDataSet model_data = null;
        try {
                model_data = reader.readFile(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model_data;
    }

    private static int findRowInTable (TableDataSet table, int rowYear){
        //find the correct row to extract the data from.
        int rowIndex = -1;
        for(int r=1; r<= table.getRowCount(); r++){
            if( (int) table.getValueAt(r,"year") == rowYear)
                rowIndex = r;
        }
        if(rowIndex == -1) {
            logger.severe("The table did not have any entries for year " + rowYear);
            logger.severe("No calibration output file can be created - system will exit");
            System.exit(10);
        } else {
            logger.fine("Summarizing data from row " + rowIndex + " corresponding to year " + (int)table.getValueAt(rowIndex,"year"));
        }
        return rowIndex;
    }


}
