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
package com.pb.tlumip.pt.estimation;
import java.io.PrintWriter;
import java.util.StringTokenizer;


/** 
 * A class that provides methods to construct
 * an estimation file record for home location
 * choice
 * 
 * @author Joel Freedman
 * @version  1.0, 12/1/2003
 * 
 */
public class HomeLocationEstimationFileRecord {


     //household variables
     long sampno;        
     int stratum;        
     int size;           
     int ownHome;        
     int typeHome;       
     int numVehic;       
     int yrsResid;       
     int incLevl1;       
     int income1;        
     int incRef;         
     int assign;         
     int day1;           
     int day2;           
     int fullwrk;        
     int partwrk;        
     int ch0to5;         
     int ch5to10;        
     int ch10to15;       
     int ch15to18;       
     int adults;         
     int homeTaz;        
     int numberWorkers;
                        
     //worker 1 variables    
     int persno1;            
     int relation1;          
     int female1;            
     int age1;
     int license1;           
       int empStat1;           
       int occupat1;           
       int indstry1;           
       int lngAtJb1;           
       int telecom1;           
       int shift1;             
       int stdStat1;           
       int stdLevel1;          
       int edcLevel1;          
       int wrkTaz1; 
       float oHwyTime1; 
       float oHwyDist1; 
       float rHwyTime1; 
       float rHwyDist1; 
       int count1;    
                                           
     //worker 2 variables     
     int persno2;             
     int relation2;           
     int female2;             
     int age2;                
     int license2;
       int empStat2;            
       int occupat2;            
       int indstry2;            
       int lngAtJb2;            
       int telecom2;            
       int shift2;              
       int stdStat2;            
       int stdLevel2;           
       int edcLevel2;           
       int wrkTaz2;             
       float oHwyTime2; 
       float oHwyDist2; 
       float rHwyTime2; 
       float rHwyDist2; 
       int count2;    
                                           
                             
     //worker 3 variables     
     int persno3;             
     int relation3;           
     int female3;             
     int age3;    
     int license3;            
       int empStat3;            
       int occupat3;            
       int indstry3;            
       int lngAtJb3;            
       int telecom3;            
       int shift3;              
       int stdStat3;            
       int stdLevel3;           
       int edcLevel3;           
       int wrkTaz3;             
       float oHwyTime3; 
       float oHwyDist3; 
       float rHwyTime3; 
       float rHwyDist3; 
       int count3;
       
       
     //parse method takes a string and returns a household object
     HomeLocationEstimationFileRecord parse(String inString){

          HomeLocationEstimationFileRecord h = new HomeLocationEstimationFileRecord();
          StringTokenizer inToken = new StringTokenizer(inString,",");

          h.sampno     = new Long(inToken.nextToken()).longValue();        //1 
          h.stratum    = new Integer(inToken.nextToken()).intValue();      //2 
        h.size       = new Integer(inToken.nextToken()).intValue();      //3 
          h.ownHome    = new Integer(inToken.nextToken()).intValue();      //4 
          h.typeHome   = new Integer(inToken.nextToken()).intValue();      //5 
        h.numVehic   = new Integer(inToken.nextToken()).intValue();      //6 
        h.yrsResid   = new Integer(inToken.nextToken()).intValue();      //7 
        h.incLevl1   = new Integer(inToken.nextToken()).intValue();      //8 
        h.income1    = new Integer(inToken.nextToken()).intValue();      //9                     
        h.incRef     = new Integer(inToken.nextToken()).intValue();      //10      
        h.assign     = new Integer(inToken.nextToken()).intValue();      //11      
         h.day1       = new Integer(inToken.nextToken()).intValue();      //12      
        h.day2       = new Integer(inToken.nextToken()).intValue();      //13      
         h.fullwrk    = new Integer(inToken.nextToken()).intValue();      //14      
         h.partwrk    = new Integer(inToken.nextToken()).intValue();      //15      
        h.ch0to5     = new Integer(inToken.nextToken()).intValue();      //16      
        h.ch5to10    = new Integer(inToken.nextToken()).intValue();      //17      
        h.ch10to15   = new Integer(inToken.nextToken()).intValue();      //18      
        h.ch15to18   = new Integer(inToken.nextToken()).intValue();      //19      
        h.adults     = new Integer(inToken.nextToken()).intValue();      //20      
        h.homeTaz    = new Integer(inToken.nextToken()).intValue();      //21       
        h.numberWorkers = new Integer(inToken.nextToken()).intValue();   //22          
                                                                         //
        h.persno1    = new Integer(inToken.nextToken()).intValue();      //23 
        h.relation1  = new Integer(inToken.nextToken()).intValue();      //24 
        h.female1    = new Integer(inToken.nextToken()).intValue();      //25 
        h.age1       = new Integer(inToken.nextToken()).intValue();      //26 
        h.license1   = new Integer(inToken.nextToken()).intValue();      //27 
        h.empStat1   = new Integer(inToken.nextToken()).intValue();      //28 
        h.occupat1   = new Integer(inToken.nextToken()).intValue();      //29 
        h.indstry1   = new Integer(inToken.nextToken()).intValue();      //30 
        h.lngAtJb1   = new Integer(inToken.nextToken()).intValue();      //31 
        h.telecom1   = new Integer(inToken.nextToken()).intValue();      //32 
        h.shift1     = new Integer(inToken.nextToken()).intValue();      //33 
        h.stdStat1   = new Integer(inToken.nextToken()).intValue();      //34 
        h.stdLevel1  = new Integer(inToken.nextToken()).intValue();      //35 
        h.edcLevel1  = new Integer(inToken.nextToken()).intValue();      //36 
        h.wrkTaz1    = new Integer(inToken.nextToken()).intValue();      //37 
        h.oHwyTime1  = new Float(inToken.nextToken()).floatValue();      //38
        h.oHwyDist1  = new Float(inToken.nextToken()).floatValue();      //39
        h.rHwyTime1  = new Float(inToken.nextToken()).floatValue();      //40
        h.rHwyDist1  = new Float(inToken.nextToken()).floatValue();      //41
        h.count1     = new Integer(inToken.nextToken()).intValue();      //42 
                                                                         //
        h.persno2    = new Integer(inToken.nextToken()).intValue();      //43 
        h.relation2  = new Integer(inToken.nextToken()).intValue();      //44 
        h.female2    = new Integer(inToken.nextToken()).intValue();      //45 
        h.age2       = new Integer(inToken.nextToken()).intValue();      //46 
        h.license2   = new Integer(inToken.nextToken()).intValue();      //47 
        h.empStat2   = new Integer(inToken.nextToken()).intValue();      //48 
        h.occupat2   = new Integer(inToken.nextToken()).intValue();      //49 
        h.indstry2   = new Integer(inToken.nextToken()).intValue();      //50 
        h.lngAtJb2   = new Integer(inToken.nextToken()).intValue();      //51 
        h.telecom2   = new Integer(inToken.nextToken()).intValue();      //52 
        h.shift2     = new Integer(inToken.nextToken()).intValue();      //53 
        h.stdStat2   = new Integer(inToken.nextToken()).intValue();      //54 
        h.stdLevel2  = new Integer(inToken.nextToken()).intValue();      //55 
        h.edcLevel2  = new Integer(inToken.nextToken()).intValue();      //56 
        h.wrkTaz2    = new Integer(inToken.nextToken()).intValue();      //57 
        h.oHwyTime2  = new Float(inToken.nextToken()).floatValue();      //58
        h.oHwyDist2  = new Float(inToken.nextToken()).floatValue();      //59
        h.rHwyTime2  = new Float(inToken.nextToken()).floatValue();      //60
        h.rHwyDist2  = new Float(inToken.nextToken()).floatValue();      //61
        h.count2     = new Integer(inToken.nextToken()).intValue();      //62 
                                                                         //
        h.persno3    = new Integer(inToken.nextToken()).intValue();      //63 
        h.relation3  = new Integer(inToken.nextToken()).intValue();      //64 
        h.female3    = new Integer(inToken.nextToken()).intValue();      //65 
        h.age3       = new Integer(inToken.nextToken()).intValue();      //66 
        h.license3   = new Integer(inToken.nextToken()).intValue();      //67 
        h.empStat3   = new Integer(inToken.nextToken()).intValue();      //68 
        h.occupat3   = new Integer(inToken.nextToken()).intValue();      //69 
        h.indstry3   = new Integer(inToken.nextToken()).intValue();      //70 
        h.lngAtJb3   = new Integer(inToken.nextToken()).intValue();      //71 
        h.telecom3   = new Integer(inToken.nextToken()).intValue();      //72 
        h.shift3     = new Integer(inToken.nextToken()).intValue();      //73 
        h.stdStat3   = new Integer(inToken.nextToken()).intValue();      //74 
        h.stdLevel3  = new Integer(inToken.nextToken()).intValue();      //75 
        h.edcLevel3  = new Integer(inToken.nextToken()).intValue();      //76 
        h.wrkTaz3    = new Integer(inToken.nextToken()).intValue();      //77 
        h.oHwyTime3  = new Float(inToken.nextToken()).floatValue();      //78
        h.oHwyDist3  = new Float(inToken.nextToken()).floatValue();      //79
        h.rHwyTime3  = new Float(inToken.nextToken()).floatValue();      //80
        h.rHwyDist3  = new Float(inToken.nextToken()).floatValue();      //81
        h.count3     = new Integer(inToken.nextToken()).intValue();      //82 
                                                                          
           return h;                                                          
    }                                                                      
                                                                           
    public void print(PrintWriter p){                                      
                                                                            
           p.print(                                                           
                 " "+ sampno                                                   
               +" "+ stratum                                                  
               +" "+ size                                                     
               +" "+ ownHome                                                  
               +" "+ typeHome       
               +" "+ numVehic       
               +" "+ yrsResid       
               +" "+ incLevl1       
               +" "+ income1        
               +" "+ incRef         
               +" "+ assign         
               +" "+ day1           
               +" "+ day2           
               +" "+ fullwrk        
               +" "+ partwrk        
               +" "+ ch0to5         
               +" "+ ch5to10        
               +" "+ ch10to15       
               +" "+ ch15to18       
               +" "+ adults         
               +" "+ homeTaz        
               +" "+ numberWorkers
               //worker 1 variables    
               +" "+ persno1            
               +" "+ relation1          
               +" "+ female1            
               +" "+ age1
               +" "+ license1           
                 +" "+ empStat1           
                 +" "+ occupat1           
                 +" "+ indstry1           
                 +" "+ lngAtJb1           
                 +" "+ telecom1           
                 +" "+ shift1             
                 +" "+ stdStat1           
                 +" "+ stdLevel1          
                 +" "+ edcLevel1          
                 +" "+ wrkTaz1 
                 +" "+ oHwyTime1 
                 +" "+ oHwyDist1 
                 +" "+ rHwyTime1 
                 +" "+ rHwyDist1 
                 +" "+ count1    
               //worker 2 variables     
               +" "+ persno2             
               +" "+ relation2           
               +" "+ female2             
               +" "+ age2                
               +" "+ license2
                 +" "+ empStat2            
                 +" "+ occupat2            
                 +" "+ indstry2            
                 +" "+ lngAtJb2            
                 +" "+ telecom2            
                 +" "+ shift2              
                 +" "+ stdStat2            
                 +" "+ stdLevel2           
                 +" "+ edcLevel2           
                 +" "+ wrkTaz2             
                 +" "+ oHwyTime2 
                 +" "+ oHwyDist2 
                 +" "+ rHwyTime2 
                 +" "+ rHwyDist2 
                 +" "+ count2    
               //worker 3 variables     
               +" "+ persno3             
               +" "+ relation3           
               +" "+ female3             
               +" "+ age3    
               +" "+ license3            
                 +" "+ empStat3            
                 +" "+ occupat3            
                 +" "+ indstry3            
                 +" "+ lngAtJb3            
                 +" "+ telecom3            
                 +" "+ shift3              
                 +" "+ stdStat3            
                 +" "+ stdLevel3           
                 +" "+ edcLevel3           
                 +" "+ wrkTaz3             
                 +" "+ oHwyTime3 
                 +" "+ oHwyDist3 
                 +" "+ rHwyTime3 
                 +" "+ rHwyDist3 
                 +" "+ count3     
         );
         
    }
}
