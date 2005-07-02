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
 * Attributes of a person that are inputs to the duration model
 * 
 * @author Freedman
 * @version 1.0 12/1/2003
 *
 */

public class PersonDurationAttributes{


     public int singleAdultWithOnePlusChildren;
     public int autos0;
     public int age19to21;
     public int age22to24;
     public int industryEqualsRetail;
     public int industryEqualsPersonServices;
     public int worksTwoJobs;
     public int age25Plus;
     public int income60Plus;
     public int female;
     public int householdSize3Plus;
     
     /**
      * Default constructor
      *
      */
     public PersonDurationAttributes(){
         
     }
     
     public void setAttributes(PTHousehold thisHousehold, PTPerson thisPerson){
 
          singleAdultWithOnePlusChildren=0;
          autos0=0;
          age19to21=0;
          age22to24=0;
          industryEqualsRetail=0;
          industryEqualsPersonServices=0;
          worksTwoJobs=0;
          age25Plus=0;
          income60Plus=0;
          female=0;
          householdSize3Plus=0;

          if(thisPerson.age>=19 && thisPerson.age<22)
              age19to21=1;
          if(thisPerson.age>=22 && thisPerson.age<25)
               age22to24=1;
          if(thisPerson.age>=25)
               age25Plus=1;
          
          if(thisPerson.worksTwoJobs)
               worksTwoJobs=1;
               
          if(thisPerson.occupation==OccupationCode.RETAIL_SALES)
               industryEqualsRetail=1;
               
          //if(thisPerson.occupation==OccupationCode.PERSONAL_SERVICE)
          //     industryEqualsPersonServices=1;
               
          if(thisPerson.female)
               female=1;
               
          if(thisHousehold.autos==0)
               autos0=1;
          
          if(thisHousehold.income>=60000)
               income60Plus=1;
          
          if(thisHousehold.size>=3)
               householdSize3Plus=1;
               
          int children=0;
          int adults=0;
          for(int i=0;i<thisHousehold.persons.length;++i){
               if(thisHousehold.persons[i].age<5)
                    ++children;
               if(thisHousehold.persons[i].age>=19)
                    ++adults;
          }
          if(thisPerson.age>19 && children>0 && adults==1)
               singleAdultWithOnePlusChildren=1;
               

          
     }



}
