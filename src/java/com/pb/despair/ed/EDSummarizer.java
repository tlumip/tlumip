package com.pb.despair.ed;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.ArrayList;

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
    
    public static void writeActivityCalibrationOutputFile(File prevYearCalibrationFile, File dataFile,
                                                             int year, String outputFilePath){
        createCalibrationOutputTable(prevYearCalibrationFile, dataFile, year, "Activity");
        writeCalibrationOutputFile(outputFilePath, "Activity");
    }

    public static void writeEmploymentCalibrationOutputFile(File prevYearCalibrationFile, File dataFile,
                                                             int year, String outputFilePath){
        createCalibrationOutputTable(prevYearCalibrationFile, dataFile, year, "Employment");
        writeCalibrationOutputFile(outputFilePath, "Employment");
    }

    public static void writeConstructionCalibrationOutputFile(File prevYearCalibrationFile, File dataFile,
                                                             int year, String outputFilePath){
        createCalibrationOutputTable(prevYearCalibrationFile, dataFile, year, "Construction"); 
        writeCalibrationOutputFile(outputFilePath, "Construction");
    }

    public static void writePopulationCalibrationOutputFile(File prevYearCalibrationFile, File dataFile,
                                                             int year, String outputFilePath){
        createCalibrationOutputTable(prevYearCalibrationFile, dataFile, year, "Population");
        writeCalibrationOutputFile(outputFilePath, "Population");
    }



    private static void createCalibrationOutputTable(File prevYearCalibrationOutputFile,
                                                    File dataFile, int year, String type) {

        //Read the model_data.csv file that is ED's output file so that we can copy the data into our new output file
        TableDataSet model_data = readModelDataFile(dataFile);
        String[] model_dataHeaders = model_data.getColumnLabels();
        //find the correct row to extract the data from.
        int rowIndex = findRowInTable(model_data, year);

        float[] currentValues = null;
        if(prevYearCalibrationOutputFile.exists()){  //read in existing calibration output and add current output to it.
            try {
                CSVFileReader reader = new CSVFileReader();
                TableDataSet prevYearOutputDataTable = reader.readFile(prevYearCalibrationOutputFile);
                header = prevYearOutputDataTable.getColumnLabels();
                float[][] prevValues = prevYearOutputDataTable.getValues();

                //Get the current values using the headers from the prev year's output
                currentValues = getCurrentValues( model_data, rowIndex, header );

                //Now create a new set of outputValues which will be turned into a TableDataSet.
                outputValues = new float[prevValues.length+1][header.length]; //1 extra row, same number of columns
                for(int r=0; r< outputValues.length; r++){
                    for(int c=0; c< outputValues[0].length; c++){
                        if(r != outputValues.length-1){   //for all rows but the last row, copy the prevYearData.
                            outputValues[r][c] = prevValues[r][c];
                        }else{                            //on the last row, copy the current data.
                            outputValues[r][c] = currentValues[c];
                        }
                    }
                }

             } catch (IOException e) {
                e.printStackTrace();
            }
        } else { // no previous file so we need to get the headers we want out of the model_data file
            ArrayList file1Header = new ArrayList();
            file1Header.add(0,"year");

            int file1Count = 1;
            for(int i=1; i< model_dataHeaders.length; i++){
                if(type.equals("Activity")){   //the headers will depend on the type of calibration file we are creating.
                    if(model_dataHeaders[i].startsWith("PROD")){       // all headers starting with "PROD" need to be added to
                        file1Header.add(file1Count, model_dataHeaders[i]);    // the production calibration file
                        file1Count++;
                    }
                } else if(type.equals("Employment")){
                    if(model_dataHeaders[i].startsWith("OEMP")){       // all headers starting with "OEMP" need to be added to
                        file1Header.add(file1Count, model_dataHeaders[i]);    // the employment calibration file
                        file1Count++;
                    }
                } else if(type.equals("Construction")){              //all headers ending in "RSV" need to be added
                    if(model_dataHeaders[i].endsWith("RSV")){           //to the construction calibration file
                        file1Header.add(file1Count, model_dataHeaders[i]);
                        file1Count++;
                    }
                } else if(type.equals("Population")){              //one header called OPOP needs to be added
                    if(model_dataHeaders[i].equalsIgnoreCase("OPOP")){     //to the population calibration file
                        file1Header.add(file1Count, model_dataHeaders[i]);
                        file1Count++;
                    }
                }
            }
            //Set header
            header = new String[file1Header.size()];
            for(int i=0; i< file1Header.size(); i++){
                header[i] = (String)file1Header.get(i);
            }
            //Get current values
            currentValues = getCurrentValues(model_data,rowIndex,header);
            //Set outputValues
            outputValues = new float[1][header.length];
            for(int i=0; i< outputValues[0].length; i++){
                outputValues[0][i] = currentValues[i];
            }
            
        }
    }

    private static void writeCalibrationOutputFile(String outputFilePath, String type){
        logger.info("Writing " + outputFilePath);
        TableDataSet outputTable = TableDataSet.create(outputValues, header);
        outputTable.setName(type + " Calibration Output");
        CSVFileWriter writer = new CSVFileWriter();
        try {
            writer.writeFile(outputTable, new File(outputFilePath));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            logger.info("Summarizing data from row " + rowIndex + " corresponding to year " + (int)table.getValueAt(rowIndex,"year"));
        }
        return rowIndex;
    }

    private static float[] getCurrentValues(TableDataSet table, int row, String[] columnNames){
        //get the data corresponding to the correct column (indicated by header index) and
        //put it into a temp. array.  These data will be added to the prevValues and then written
        //out.
        float[] currentValues = new float[header.length];
        for(int i=0; i< currentValues.length; i++){
            currentValues[i] = table.getValueAt(row,header[i]);
        }
        return currentValues;
    }


}
