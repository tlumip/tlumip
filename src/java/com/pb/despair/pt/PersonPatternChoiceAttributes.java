package com.pb.despair.pt;
import java.util.logging.Logger;

/**  
 * Attributes of a person that are inputs to the pattern choice model
 * 
 * @author Freedman
 * @version 1.0 12/1/2003
 *
 */

public class PersonPatternChoiceAttributes{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     //following are boolean (0,1) integers
     
     //for weekday model
     int worker; 
     int student;
     int unemployed;
     int age00To05;
     int age05To15;
     int age00To21;
     int age21To25;
     int age15To25;
     int age25To50;
     int age25To60;
     int age50To70;
     int age70To80;
     int age80Plus;
     int childlt15;
     int householdIncome00To15k;
     int householdIncome50kPlus;
     int autos0;
     int householdSize1;
     int householdSize2;
     int householdSize3Plus;
     double shopDCLogsum;
     double otherDCLogsum;
     double workBasedDCLogsum;
     int singleWithChild0_5;
     int worksTwoJobs;


     //int for weekend model (but not already included above)
     int age00To15;
     int age15To21;
     int age21To50;
     int child00To05;
     int child05To10;
     int child10To15;
     int noNonWorkingAdults;
     int oneNonWorkingAdult;
     int householdIncome15To25k;
     int householdIncome25To55k;
     int householdIncome55kPlus;
     int autosLessThanAdults;
     int female;
     int age05To10;
     int age10To20;
     int age20To30;
     int age30To50;
     int industryEqualsRetail;
     int industryEqualsPersonalServices;

     /** constructor takes a person object and populates the attributes
     relevant to the Pattern Choice Model 
     */
     public PersonPatternChoiceAttributes(){
     }
     
     public void setAttributes(PTHousehold thisHousehold, PTPerson thisPerson){
        
        worker = 0; 
        student = 0;
        unemployed = 0;
        age00To05 = 0;
        age05To15 = 0;
        age00To21 = 0;
        age21To25 = 0;
        age15To25 = 0;
        age25To50 = 0;
        age25To60 = 0;
        age50To70 = 0;
        age70To80 = 0;
        age80Plus = 0;
        childlt15 = 0;
        householdIncome00To15k = 0;
        householdIncome50kPlus = 0;
        autos0 = 0;
        householdSize1 = 0;
        householdSize2 = 0;
        householdSize3Plus = 0;
        shopDCLogsum = 0;
        otherDCLogsum = 0;
        workBasedDCLogsum = 0;
        singleWithChild0_5 = 0;
        worksTwoJobs = 0;


        //for weekend model (but not already included above)
        age00To15 = 0;
        age15To21 = 0;
        age21To50 = 0;
        child00To05 = 0;
        child05To10 = 0;
        child10To15 = 0;
        noNonWorkingAdults = 0;
        oneNonWorkingAdult = 0;
        householdIncome15To25k = 0;
        householdIncome25To55k = 0;
        householdIncome55kPlus = 0;
        autosLessThanAdults = 0;
        female = 0;
        age05To10 = 0;
        age10To20 = 0;
        age20To30 = 0;
        age30To50 = 0;
        industryEqualsRetail = 0;
        industryEqualsPersonalServices = 0;
          //first set person terms
          if(thisPerson.employed)
               worker=1;
          else
               unemployed=1;
               
          if(thisPerson.worksTwoJobs)
               worksTwoJobs=1;
               
          if(thisPerson.student)
               student=1;

          if(thisPerson.age<5)
               age00To05=1;
          if(thisPerson.age>=5 && thisPerson.age<15)
               age05To15=1;
          if(thisPerson.age>=5 && thisPerson.age<10)
               age05To10=1;
          if(thisPerson.age<21)
               age00To21=1;
          if(thisPerson.age>=21 && thisPerson.age<25)
               age21To25=1;
          if(thisPerson.age>=15 && thisPerson.age<25)
               age15To25=1;
          if(thisPerson.age>=25 && thisPerson.age<50)
               age25To50=1;
          if(thisPerson.age>=25 && thisPerson.age<60)
               age25To60=1;
          if(thisPerson.age>=50 && thisPerson.age<70)
               age50To70=1;
          if(thisPerson.age>=70 && thisPerson.age<80)
               age70To80=1;
          if(thisPerson.age>=80)
               age80Plus=1;

          if(thisPerson.age<15)
               age00To15=1;
          if(thisPerson.age>=15 && thisPerson.age<21)
               age15To21=1;
          if(thisPerson.age>=21 && thisPerson.age<50)
               age21To50=1;

          if(thisPerson.age>=5 && thisPerson.age<10)
               age05To10=1;
          if(thisPerson.age>=10 && thisPerson.age<20)
               age10To20=1;
          if(thisPerson.age>=20 && thisPerson.age<30)
               age20To30=1;
          if(thisPerson.age>=30 && thisPerson.age<50)
               age30To50=1;
               
          if(thisPerson.female)
               female=1;
               
          if(thisPerson.occupation==OccupationCode.RETAIL_SALES)
               industryEqualsRetail=1;
               
          //if(thisPerson.industry==IndustryCode.PERSONAL_SERVICE)
          //     industryEqualsPersonalServices=1;      
               
          shopDCLogsum           =thisPerson.shopDCLogsum;     
          otherDCLogsum          =thisPerson.otherDCLogsum;    
          workBasedDCLogsum      =thisPerson.workBasedDCLogsum;


          //Household terms
          if(thisHousehold.autos==0)
               autos0=1;
               
          if(thisHousehold.size==1)
               householdSize1=1;
          else if(thisHousehold.size==2)
               householdSize2=1;
          else if(thisHousehold.size>=3)
               householdSize3Plus=1;
               
          if(thisHousehold.income<15000)
               householdIncome00To15k=1;
               
          if(thisHousehold.income>=50000)
               householdIncome50kPlus=1;

          if(thisHousehold.income>=15000 && thisHousehold.income<25000)
               householdIncome15To25k=1;
          
          if(thisHousehold.income>=25000 && thisHousehold.income<55000)
               householdIncome25To55k=1;

          if(thisHousehold.income>=55000)
               householdIncome55kPlus=1;
          
          //search through persons in household for rest of variables
          int adults=0;
          for(int i=0;i<thisHousehold.persons.length;++i){
               if(thisHousehold.persons[i].age<15)
                    childlt15=1;
               if(thisHousehold.persons[i].age<5)
                    child00To05=1;
               if(thisHousehold.persons[i].age>=5 && thisHousehold.persons[i].age<10)
                     child05To10=1;
               if(thisHousehold.persons[i].age>=10 && thisHousehold.persons[i].age<15)
                     child10To15=1;
               if(thisHousehold.persons[i].age>19){
                    ++adults;
                    if(!thisHousehold.persons[i].employed)
                         oneNonWorkingAdult=1;
               }
          }
          if(thisHousehold.workers>=adults)
               noNonWorkingAdults=1;
               
          if(thisHousehold.autos<adults)
               autosLessThanAdults=1;
               
          if(adults==1 && child00To05==1)
               singleWithChild0_5=1;
          
     }
     
     void print(){
          //for weekday model
          logger.info("worker=                           "+worker);                   
          logger.info("student=                          "+student);                 
          logger.info("unemployed=                       "+unemployed);              
          logger.info("age00To05=                        "+age00To05);               
          logger.info("age05To15=                        "+age05To15);               
          logger.info("age00To21=                        "+age00To21);               
          logger.info("age21To25=                        "+age21To25);               
          logger.info("age15To25=                        "+age15To25);               
          logger.info("age25To50=                        "+age25To50);               
          logger.info("age25To60=                        "+age25To60);               
          logger.info("age50To70=                        "+age50To70);               
          logger.info("age70To80=                        "+age70To80);               
          logger.info("age80Plus=                        "+age80Plus);               
          logger.info("childlt15=                        "+childlt15);               
          logger.info("householdIncome00To15k=           "+householdIncome00To15k);  
          logger.info("householdIncome50kPlus=           "+householdIncome50kPlus);  
          logger.info("autos0=                           "+autos0);                  
          logger.info("householdSize1=                   "+householdSize1);          
          logger.info("householdSize2=                   "+householdSize2);          
          logger.info("householdSize3Plus=               "+householdSize3Plus);      
          logger.info("double shopDCLogsum=              "+shopDCLogsum);         
          logger.info("double otherDCLogsum=             "+otherDCLogsum);        
          logger.info("double workBasedDCLogsum=         "+workBasedDCLogsum);    
          logger.info("int singleWithChild0_5=           "+singleWithChild0_5);      
          logger.info("int worksTwoJobs=                 "+worksTwoJobs);            
         
         
          //int for weekend model (but not already included above)
          logger.info("age00To15=                        "+age00To15);                      
          logger.info("age15To21=                        "+age15To21);                      
          logger.info("age21To50=                        "+age21To50);                      
          logger.info("child00To05=                      "+child00To05);                    
          logger.info("child05To10=                      "+child05To10);                    
          logger.info("child10To15=                      "+child10To15);                    
          logger.info("noNonWorkingAdults=               "+noNonWorkingAdults);             
          logger.info("oneNonWorkingAdult=               "+oneNonWorkingAdult);             
          logger.info("householdIncome15To25k=           "+householdIncome15To25k);         
          logger.info("householdIncome25To55k=           "+householdIncome25To55k);         
          logger.info("householdIncome55kPlus=           "+householdIncome55kPlus);         
          logger.info("autosLessThanAdults=              "+autosLessThanAdults);            
          logger.info("female=                           "+female);                         
          logger.info("age05To10=                        "+age05To10);                      
          logger.info("age10To20=                        "+age10To20);                      
          logger.info("age20To30=                        "+age20To30);                      
          logger.info("age30To50=                        "+age30To50);                      
          logger.info("industryEqualsRetail=             "+industryEqualsRetail);           
          logger.info("industryEqualsPersonalServices=   "+industryEqualsPersonalServices); 
         
     }


}


