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
package com.pb.tlumip.pt;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Random;
import org.apache.log4j.Logger;
/** 
 * A class containing all information about a person 
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class PTPerson implements Serializable, Comparable{
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
     public boolean employed; //will be true if 'RLABOR' code = 1,2,4 or 5.
     public boolean student;
     public boolean worksTwoJobs;
     public byte age;
     public boolean female;
     public byte occupation; //you can have a person that is unemployed with an occupation
                            //code of something other than 0 (ie. an unemployed retail worker)
     public byte industry;  //these are less general than occupation and correspond to the Ed Industry categories
     public int ID;
     public int hhID;     

     //double workDCLogsum;
     double schoolDCLogsum;
     double shopDCLogsum;
     double recreateDCLogsum;
     double otherDCLogsum;
     double workBasedDCLogsum;
     double weekdayPatternLogsum;
     double weekendPatternLogsum;
     

     
     public short workTaz;
     public short workTaz2; //for persons who work multiple jobs
     short schoolTaz;

     //not sure if these are available in application
     //int worksFullTime;
     //int worksPartTime;
     
     //int studentFullTime;
     //int studentPartTime;
             
     Pattern weekdayPattern;
     Pattern weekendPattern;

     public Tour[] weekdayTours;
     Tour[] weekendTours;

     Tour[] weekdayWorkBasedTours;
     Tour[] weekendWorkBasedTours;
     
     //These need to be added 
     public short homeTaz;
     public byte householdWorkSegment;
     //constructor
     public PTPerson(){}


     // generates random data for person 
     public void createRandomPerson(TazData tazdata){
     
          Random r = new Random();
          
          employed = r.nextBoolean();
          student = r.nextBoolean();
          female = r.nextBoolean();
          
          if(employed)
               worksTwoJobs = r.nextBoolean();
          else
               worksTwoJobs=false;
               
          if(employed){
               occupation=(byte)(r.nextInt(14)+1);
          }
        
          //age = 1->80
          age=(byte)(r.nextInt(80)+1);
          
          //work tazs     
          if(employed){
               boolean foundTaz=false;
               while(!foundTaz){
                    short randomTaz = (short)(r.nextInt(4200)+1);
                    if(tazdata.hasTaz(randomTaz)){
                         workTaz=randomTaz;
                         foundTaz=true;
                    }
               }
          }          
          if(worksTwoJobs){
               boolean foundTaz=false;
               while(!foundTaz){
                    short randomTaz = (short)(r.nextInt(4200)+1);
                    if(tazdata.hasTaz(randomTaz)){
                         workTaz2=randomTaz;
                         foundTaz=true;
                    }
               }
          }          
          if(student){
               boolean foundTaz=false;
               while(!foundTaz){
                    short randomTaz = (short)(r.nextInt(4200)+1);
                    if(tazdata.hasTaz(randomTaz)){
                         schoolTaz=randomTaz;
                         foundTaz=true;
                    }
               }
          }          

          
     }
     
     /*
     * this is the old logsum segment, when we still had industry
     */
     //public int calcWorkerLogsumSegment(){
          
     //     int segment=4;
     //     if(occupation==OccupationCode.MANAGEMENT||(occupation==OccupationCode.TECHNICAL && industry!=IndustryCode.RETAIL))           
     //          segment=1;
     //     else if(occupation==OccupationCode.TECHNICAL && industry==IndustryCode.RETAIL)
     //          segment=2;
     //     else if(occupation==OccupationCode.PRODUCTION||occupation==OccupationCode.FABRICATION)
     //          segment=3;
          
     //     return segment;
     //}
     
     
    /*
     * for the Mode/DC Logsums market segment
     * @deprecated
     * This method isn't used and it shouldn't be
     * because the occupation is saved as a number
     * between 0-8 (see PTDataReader.readPersons) and
     * therefore this will not calculate an accurate segment
     */
//     public int calcWorkerLogsumSegment(){
//          
//          int segment=4;
//          if(occupation<=262)           // Manager/Professional
//               segment=1;
//          else if(occupation>=263 && occupation <= 282) // retail
//               segment=2;
//          else if(occupation>402) // Production/Fabrication
//               segment=3;
//          
//          return segment;
//     }
     
          
     /*
     * for the Mode/DC Logsums market segment
     */
     public int calcStudentLogsumSegment(){
          
          int segment=1;
          
          if(age > 18)          //College +   (<=18 implies K-12 segment)
               segment=3;
               
          return segment;
     }
     
//     This method was luckily not used - in the tlumip
     //project - the segmentation is done in the household
     //object instead.
//     public int getDCSegment(int purpose){
//         if(purpose==ActivityPurpose.WORK)
//            return calcWorkerLogsumSegment();
//         else if(purpose==ActivityPurpose.SCHOOL)
//            return calcStudentLogsumSegment();
//         else return 1;
//     }
     
     public void print(){
         logger.info("");
          logger.info("PERSON INFO: ");
          logger.info("employed=                  "+employed);               
          logger.info("student=                   "+student);                
          logger.info("worksTwoJobs=              "+worksTwoJobs);           
          logger.info("age=                       "+age);                    
          logger.info("female=                    "+female);                              
          logger.info("occupation=                "+occupation);             
          //logger.info("workDCLogsum=              "+workDCLogsum);           
          logger.info("schoolDCLogsum=            "+schoolDCLogsum);         
          logger.info("shopDCLogsum=              "+shopDCLogsum);           
          logger.info("recreateDCLogsum=          "+recreateDCLogsum);       
          logger.info("otherDCLogsum=             "+otherDCLogsum);          
          logger.info("workBasedDCLogsum=         "+workBasedDCLogsum);      
          logger.info("workTaz=                   "+workTaz);                
          logger.info("workTaz2=                  "+workTaz2);                
          logger.info("schoolTaz=                 "+schoolTaz);         
     }     
     
     //to write to a text file, csv format
     public void printCSV(PrintWriter file){

          file.println(
               hhID + "," 
               +ID+","
               +booleanToInt(employed)+","
               +booleanToInt(student)+","
               +booleanToInt(worksTwoJobs)+","
               +age+","
               +booleanToInt(female)+","
               +occupation+","
               //+workDCLogsum+","
               +schoolDCLogsum+","
               +shopDCLogsum+","
               +recreateDCLogsum+","
               +otherDCLogsum+","
               +workBasedDCLogsum+","
               +workTaz+","
               +workTaz2+","
               +schoolTaz+","
               +weekdayPattern+","
//               +weekendPattern+","
               +weekdayTours.length+","
//               +weekendTours.length+","
               +weekdayWorkBasedTours.length
//               +weekendWorkBasedTours.length+","
          );
     }
     
    public int booleanToInt(boolean boo){
        return (boo==false) ? 0 : 1;
    }
        
                        
    /* sorts persons by work segment, then by occupation
    */
    public int compareTo(Object person){
        PTPerson p = (PTPerson)person;
        int cs = p.occupation+(p.householdWorkSegment*10);
        int compositeSegment = this.occupation+(this.householdWorkSegment*10);
  
        if(compositeSegment < cs) return -1;
        else if(compositeSegment > cs) return 1;
        else return 0;
        
    }

}

