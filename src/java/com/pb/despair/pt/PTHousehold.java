package com.pb.despair.pt;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Random;
import org.apache.log4j.Logger;

/** 
 * A class for a Household in PT
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class PTHousehold implements Comparable, Serializable{

//     public static int numberOfCurrentHousehold;
     protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");

    public static final int NUM_WORK_SEGMENTS = 9;
    public static final int NUM_NONWORK_SEGMENTS = 9;

     public int ID;
     public byte size;
     public byte autos;
     public byte workers;
     public int income;
     public boolean singleFamily;
     public boolean multiFamily;
     public short homeTaz;
     
     
     //the auto/income segment
     public byte compositeSegment;
     
     public PTPerson[] persons;
     
     //not sure if we'll have the following
//     public int fullWorkers;
//     public int partWorkers;

     //for sorting
     public boolean idSort = false;
     public boolean segmentSort = true;
     
     //constructor
     public PTHousehold(){

     }


     /* A class to create a household
     and generate random data
     @TazData: takes
     a TazData class so that it can select
     a user-defined taz number
     */
     public void createRandomHousehold(TazData tazdata){
     
          Random r = new Random(System.currentTimeMillis() + ID);
          //size = 1->4
          size = (byte)(r.nextInt(4)+1);
          //income = 1 -> 100000
          income = (byte)(r.nextInt(100000)+1);
          
          singleFamily = r.nextBoolean();
          if(!singleFamily)
               multiFamily = true;
          
          boolean foundTaz=false;
          while(!foundTaz){
               short randomTaz = (short)(r.nextInt(4200)+1);
               if(tazdata.hasTaz(randomTaz)){
                    homeTaz=randomTaz;
                    foundTaz=true;
               }
          }          
          
          //generate persons
          persons = new PTPerson[size];
          
          for(int i=0;i<size;++i){
               persons[i] = new PTPerson();
               persons[i].ID=i+1;
               persons[i].createRandomPerson(tazdata);
               if(persons[i].employed==true)
                    ++workers;
          }     
     
     }
     
     /*
     * for the Mode/DC Logsums market segment
     *
     */
     public int calcWorkLogsumSegment(){
          
          int segment=0;
          //hh income - 0-15,15-30,30+
          boolean inclow=false;
          boolean incmed=false;
          boolean inchi=false;
          if(income<15000)
               inclow=true;
          else if(income>=15000 && income<30000)
               incmed=true;
          else 
               inchi=true;
          
          if(inclow){
               if(autos==0) segment=0;
               else if(autos<workers) segment=1;
               else segment=2;
          }
          if(incmed){
               if(autos==0) segment=3;
               else if(autos<workers) segment=4;
               else segment=5;
          }
          if(inchi){
               if(autos==0) segment=6;
               else if(autos<workers) segment=7;
               else segment=8;
          }
          

          return segment;
     }

     public int calcNonWorkLogsumSegment(){
          
          int segment=0;
          //hh income - 0-15,15-30,30+
          boolean inclow=false;
          boolean incmed=false;
          boolean inchi=false;
          if(income<15000)
               inclow=true;
          else if(income>=15000 && income<30000)
               incmed=true;
          else 
               inchi=true;
          
          if(inclow){
               if(autos==0) segment=0;
               else if(autos<size) segment=1;
               else segment=2;
          }
          if(incmed){
               if(autos==0) segment=3;
               else if(autos<size) segment=4;
               else segment=5;
          }
          if(inchi){
               if(autos==0) segment=6;
               else if(autos<size) segment=7;
               else segment=8;
          }
          

          return segment;
     }
     
     public int calcCompositeSegment(){
         return   (calcNonWorkLogsumSegment()*10)+calcWorkLogsumSegment();
     }

    /* to sort households, use a composite of worker segment
        and nonworker segment xx = 10*nonworksegment+worksegment
     */
     public int compareTo(Object household){
     	  
          PTHousehold h = (PTHousehold)household;

              int cs = (h.calcNonWorkLogsumSegment()*10)+h.calcWorkLogsumSegment();
              compositeSegment = (byte)((this.calcNonWorkLogsumSegment()*10)+this.calcWorkLogsumSegment());
    
              if(compositeSegment<cs) return -1;
              else if(compositeSegment>cs) return 1;
              else return 0;
    }
        
     
     
     public void print(){
          logger.info("");
          logger.info("HOUSEHOLD INFO: ");
          logger.info("ID=               "+     ID);
          logger.info("size=             "+        size);            
          logger.info("autos=            "+        autos);           
          logger.info("workers=          "+     workers);         
          logger.info("income=           "+     income);          
          logger.info("singleFamily=     "+     singleFamily);
          logger.info("multiFamily=      "+     multiFamily); 
          logger.info("homeTaz=          "+     homeTaz);         
     }

     //to write to a text file, csv format
     public void printCSV(PrintWriter file){

          file.println(
               ID+","+
               +size+","
               +autos+","
               +workers+","
               +income+","
               +booleanToInt(singleFamily)+","
               +booleanToInt(multiFamily)+","
               +homeTaz
          );
     }
     
     public int booleanToInt(boolean boo){
         return (boo==false) ? 0 : 1;
     }
     
     
     public static void main(String[] args){          
         
     }

}



