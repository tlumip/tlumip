package com.pb.despair.pt;

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
     public int age18to20;
     public int age21to25;
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
          age18to20=0;
          age21to25=0;
          industryEqualsRetail=0;
          industryEqualsPersonServices=0;
          worksTwoJobs=0;
          age25Plus=0;
          income60Plus=0;
          female=0;
          householdSize3Plus=0;

          if(thisPerson.age>=18 && thisPerson.age<21)
              age18to20=1; 
          if(thisPerson.age>=21 && thisPerson.age<25)
               age21to25=1;          
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
               if(thisHousehold.persons[i].age>19)
                    ++adults;
          }
          if(thisPerson.age>19 && children>0 && adults==1)
               singleAdultWithOnePlusChildren=1;
               

          
     }



}
