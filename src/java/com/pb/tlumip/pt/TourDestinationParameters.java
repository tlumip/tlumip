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


/** 
 * A class that contains Tour Mode Parameters
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TourDestinationParameters {


     public String purpose = new String();  //w=work, c=school,s=shop,r=recreate,o=other,b=workbased
     public float logsum;                  //logsum
     public float distance;                 //distance
     public float distancePower;            //distance squared parameter
     public float intraZonal;              //intrazonal - ln(acres)
     public float retail;            //retail employment-retail land-use
     public float nonRetail;               //Total non- retail land-use
     public float gradeSchool;            //grade School employment
     public float secondarySchool;      //secondary school employment
     public float postSecondarySchool;  //post-secondary school employment
     public float households;              //households
     public float office;                    //office emp.
     public float nonOffice;               //non office
     public float industrial;               //industrial
     public float nonIndustrial;          //non-industrial
     public float otherWork;               //non-office,retail, or industrial

     

     public TourDestinationParameters(){
        
             logsum=0;
         distance=0;
         distancePower=0;
          intraZonal=0;
          retail=0;     
          nonRetail=0;
          gradeSchool=0;      
          secondarySchool=0;     
          postSecondarySchool=0; 
          households=0;
          office=0;
          nonOffice=0;
          industrial=0;
          nonIndustrial=0; 
          otherWork=0;      
        };
   
        public void print(){

            System.out.print(
                  "\n"+ logsum
                  +"\n"+ distance
                  +"\n"+ distancePower
                 +"\n"+ intraZonal
                  +"\n"+ retail
                 +"\n"+ nonRetail
                 +"\n"+ gradeSchool
                 +"\n"+ secondarySchool
                 +"\n"+ postSecondarySchool
                 +"\n"+ households
                 +"\n"+ office
                 +"\n"+ nonOffice
                 +"\n"+ industrial
                 +"\n"+ nonIndustrial
                 +"\n"+ otherWork
           );
        }

}                                                                  
                                                                   
                                                                   
                                                                   
