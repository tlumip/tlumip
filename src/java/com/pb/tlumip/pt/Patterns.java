package com.pb.tlumip.pt;
//import com.borland.dx.dataset.TableDataSet;
//import com.pb.common.datastore.DataManager;
import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import java.io.File;
import java.io.IOException;

import java.util.*;
import org.apache.log4j.Logger;

/**  
 * Class to access patterns from TableDataSet
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class Patterns implements Cloneable{

     String patternsFileName;
     String patternsTableName;
     final static Logger logger = Logger.getLogger("com.pb.tlumip.pt");

     //an ArrayList of weekdayPatterns
     //public ArrayList patterns = new ArrayList();
     public Pattern[] patterns;
    
     public static TableDataSet loadTableDataSet(ResourceBundle rb, String fileName){
        if(logger.isDebugEnabled()) {
            logger.debug("loadTableSet is getting the following property from the rb " + fileName);
        }
        try {
           CSVFileReader reader = new CSVFileReader();
           String file = ResourceUtil.getProperty(rb, fileName);
           if(logger.isDebugEnabled()) {
               logger.debug("loadTableDataSet is trying to read file: "+ file);
           }
           TableDataSet table = reader.readFile(new File(file));
           return table;
        } catch (IOException e) {
           logger.fatal("Can't find Patterns input table " + fileName);
            //TODO - log exception to the node exception log file
           e.printStackTrace();
          }
        return null;
    }
     
    public void readData(ResourceBundle rb, String fileName){
       ArrayList patternList = new ArrayList();
       if(logger.isDebugEnabled()) {
           logger.debug("Getting table: "+fileName);
       }
       TableDataSet table = loadTableDataSet(rb, fileName);
       for (int rowNumber=1; rowNumber<=table.getRowCount(); rowNumber++) {
       	    Pattern thisPattern = new Pattern(table.getStringValueAt(rowNumber, table.getColumnPosition("patternString")));
         patternList.add(thisPattern);    
       }
       patterns = new Pattern[patternList.size()];
       patternList.toArray(patterns);
       if(logger.isDebugEnabled()) {
           logger.debug("Success reading table: "+patternsTableName);
       }

 /*    public Patterns(String fileName,String tableName, DataManager dm){

          patternsFileName=fileName;
          patternsTableName=tableName;
          
          //read patterns to JdataStore if not there yet
         //DataManager dm = new DataManager();  //Create a data manager, connect to default data-store
          if(!dm.tableExists(patternsTableName))
              dm.loadTable(patternsTableName,patternsFileName);
         //dm.closeStore();
 */    }

    public Object clone(){
        Patterns newPatterns;
        try {
            newPatterns = (Patterns) super.clone();
            newPatterns.patternsFileName = this.patternsFileName;
            newPatterns.patternsTableName = this.patternsTableName;
            newPatterns.patterns = new Pattern[this.patterns.length];
            for(int i=0;i<patterns.length;i++){
                newPatterns.patterns[i] = (Pattern) patterns[i].clone();
            }
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.toString());
        }
       return newPatterns;
    }


                                                                          
    public static void main (String[] args) throws Exception {
         ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         Patterns patt = new Patterns();
         patt.readData(rb,"weekdayPatterns.file");
         System.exit(1);                                                  
    };                                                                   
                                                                         
}                                                                        
