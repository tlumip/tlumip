package com.pb.despair.pt;

import com.pb.common.util.ObjectUtil;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.spg.EdIndustry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import java.util.*;

/** 
 * PTDataReader reads Household and Person data 
 * Adds household information to person array that are necessary for workplace location model
 * adds persons to household array (done after workplace location model has run)
 * 
 * @author Steve Hansen
 * @version 2.0 May 27, 2004
 * 
 */

public class PTDataReader{

    ResourceBundle ptRb;
    ResourceBundle globalRb;

    public PTDataReader(ResourceBundle appRb, ResourceBundle globalRb){
        this.ptRb = appRb;
        this.globalRb = globalRb;
    }

    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");

    public BufferedReader openFile(String name){
        try {
        	if(logger.isDebugEnabled()) {
                logger.debug("Adding table "+name);
            }

        	String pathName = ResourceUtil.getProperty(ptRb, name);
        	if(logger.isDebugEnabled()) {
                logger.debug("pathName: "+pathName);
            }
        	BufferedReader inStream = null;
            inStream = new BufferedReader( new FileReader(pathName) );
            return inStream;
            
        } catch (IOException e) {
            logger.fatal("Can't find input table " + name);
            e.printStackTrace();
        }
        return null;
    };

    /**
     * Helper method to find the number of lines in a text file.
     * 
     * @return total number of data records in file
     * 
     */
    private int findNumberOfRecordsInFile(String name){
        int numberOfRows = 0;
        
        try {
            String pathName = ResourceUtil.getProperty(ptRb, name);
            BufferedReader stream = new BufferedReader( new FileReader(pathName) );
            while (stream.readLine() != null) {
                numberOfRows++;
            }
            stream.close();
        }
        catch (IOException e) {
             e.printStackTrace();
        }
        //Don't count header row
        return numberOfRows-1;
    }
    

    public PTHousehold[] readHouseholds(String file){
        
        //Load data to TableDataSet
        logger.info("Opening household file");
        BufferedReader hhReader = openFile(file);
        int numberOfHouseholds = findNumberOfRecordsInFile(file);
        if(logger.isDebugEnabled()) {
            logger.debug("Number of households = "+numberOfHouseholds);
        }
        PTHousehold[] householdArray = new PTHousehold[numberOfHouseholds];
        PTHousehold thisHousehold = null;                                   
        
        try{        
            
            //read header lines
            String line = hhReader.readLine();         
            //read first data line            
            line = hhReader.readLine();
            
            String[] fields;
            int householdCounter = 0;
            while(line!=null){
                fields = line.split(",");
                thisHousehold = new PTHousehold();
                thisHousehold.ID = (int)Integer.parseInt(fields[0]);  //HH_ID
                thisHousehold.size = (byte)Integer.parseInt(fields[1]);  //PERSONS
                //get units and set vars
                int units = (int)Integer.parseInt(fields[2]); //UNITS1
                if(units==1||units==2||units==3)     //mobile home, one-family detached, one-family attached = single-family
                    thisHousehold.singleFamily=true;
                else
                    thisHousehold.multiFamily=true;
                thisHousehold.autos = (byte)Integer.parseInt(fields[3]); //AUTOS
                thisHousehold.income = (int)Integer.parseInt(fields[4]);  //RHHINC
                thisHousehold.homeTaz = (short)Integer.parseInt(fields[5]);  //ALPHAZONE

                if(thisHousehold.homeTaz>4105&&thisHousehold.homeTaz<4135)
                    logger.info("taz: "+thisHousehold.homeTaz);

                householdArray[householdCounter] = thisHousehold;
                line = hhReader.readLine();
                householdCounter++;
                thisHousehold=null;
            } 
        } catch (IOException e) {
            logger.fatal("Error reading households file");
            e.printStackTrace();
          }

        return householdArray;   
    }//end read households 
    

    public PTPerson[] readPersons(String name){

        //Load data to TableDataSet
        logger.info("Opening person file");
        BufferedReader personReader = openFile(name);
        int numberOfPersons = findNumberOfRecordsInFile(name);
        
        PTPerson[] personArray = new PTPerson[numberOfPersons];
        PTPerson thisPerson = new PTPerson();
                
        try{
            //read header lines
            String personLine = personReader.readLine();
            //read first data line            
            personLine = personReader.readLine();
            
            int personCounter=0;
            String[] fields;
            while(personLine!=null){
            	fields = personLine.split(",");
                thisPerson = new PTPerson();
                thisPerson.hhID = (int)Integer.parseInt(fields[0]); //HH_ID
                thisPerson.ID = Integer.parseInt(fields[1]);      //PERS_ID
                thisPerson.female = (int)Integer.parseInt(fields[2])==1 ? true : false; //SEX
                thisPerson.age =(byte)Integer.parseInt(fields[3]); //AGE
                int school = (int)Integer.parseInt(fields[4]);  //SCHOOL
                if(school==0||school==1)
                    thisPerson.student=false;
                else
                    thisPerson.student=true;
                int employStatus = (int)Integer.parseInt(fields[5]);   //RLABOR
                if (employStatus == 0 || employStatus == 3 || employStatus == 6)
                	thisPerson.employed = false;
                else 
                    thisPerson.employed = true;
                thisPerson.industry = (byte)(new EdIndustry()).getEdIndustry((int)Integer.parseInt(fields[6])); //INDUSTRY
                thisPerson.occupation = (byte)OccupationCode.codeOccupationFromPUMS((int)Integer.parseInt(fields[7]));  //OCCUP

                personArray[personCounter]=thisPerson;
                personCounter++;
                personLine = personReader.readLine();
            }
        }catch (Exception e) {            
            System.err.println("Error reading person file");                   
            e.printStackTrace();                                         
        }
        logger.info("Persons loaded into memory.");
        return personArray;
    }
    
    /**
     * Run the auto ownership model for the household block.
     * @param households
     * @return
     */
    public PTHousehold[] runAutoOwnershipModel(PTHousehold[] households){
        //read the tourDestinationParameters from csv to TableDataSet
        logger.info("Reading tour destination parameters");
        TourDestinationParametersData tdpd = new TourDestinationParametersData();
        tdpd.readData(ptRb,"tourDestinationParameters.file");
          
        //read the stopDestinationParameters from csv to TableDataSet
        logger.info("Reading stop destination parameters");
        StopDestinationParametersData sdpd = new StopDestinationParametersData();
        sdpd.readData(ptRb,"stopDestinationParameters.file");
        
        //read the taz data from csv to TableDataSet
        logger.info("Adding TazData");
        TazData tazs = new TazData();
        tazs.readData(ptRb, globalRb,"tazData.file");
        tazs.collapseEmployment(tdpd, sdpd);
        
        AutoOwnershipModel aom = new AutoOwnershipModel(ptRb);
        return aom.runAutoOwnershipModel(households, tazs);
           
    }
    /** This method sorts the Household array by HHID and the Persons array by PersonID and then adds worker
     * information to the households by calling the 'addWorkerInformationToHouseholds' method and
     * then adds homeTAZ and hhWorkSegment to the persons array by calling 'addHomeTazAndHHWorkSegmentToPersons'
     * method.  This method must be called from PTDafMaster before the AutoOwnership model is run.
     */
    public void addPersonInfoToHouseholdsAndHouseholdInfoToPersons(PTHousehold[] households, PTPerson[] persons){

        //first sort by hhID and personID
        sortPersonsAndHouseholds(households,persons);

        //next add Worker info to Households
        addWorkerInformationToHouseholds(households,persons);

        //next add Household info to Workers
        addHomeTazAndHHWorkSegmentToPersons(households,persons);
    }

    /**
     * This method makes sure that the persons are in their proper household and sets the households's workers
     * attribute to the number of workers depending on the employment status of the persons in the person
     * array.  It returns the array of households.
     *
     * @param households
     * @param persons
     * @return the household array (not really necessary)
     */
    private PTHousehold[] addWorkerInformationToHouseholds(PTHousehold[] households, PTPerson[] persons){

        try{
            //persons counter
            int p=0;
            //loop through all households
            for(int h=0;h<households.length;h++){
                //loop through the persons in this household
                for(int i=0;i<households[h].size;i++){
                    if(persons[p].hhID!=households[h].ID){
                        Exception e = new Exception();
                        throw e;
                    }
                    if(persons[p].employed) households[h].workers++;
                    p++;
                    if(p==persons.length) break;
                }
            }

        }catch(Exception e){
             logger.fatal("Household file and person file don't match up.  Unable to add worker data to households.");
             e.printStackTrace();
             System.exit(1);
        }
        return households;
    }


    /**
     * This method assumes sets the person's home
     * TAZ to the household TAZ and the person's householdWorkSegment to the household work 
     * logsum segment. It returns the array of persons.
     * 
     * @param households
     * @param persons
     * @return the person array (not really necessary)
     */
    private PTPerson[] addHomeTazAndHHWorkSegmentToPersons(PTHousehold[] households, PTPerson[] persons){

       //persons counter
        int p=0;
        //loop through all households
        for(int h=0;h<households.length;h++){
            //loop through the persons in this household
            for(int i=0;i<households[h].size;i++){
                persons[p].homeTaz = households[h].homeTaz;
                persons[p].householdWorkSegment = (byte)households[h].calcWorkLogsumSegment();
                p++;
                if(p==persons.length) break;
            }
        }
        return persons;
    }
    
    /**
     * Sort the household and person arrays by ID. Generate a PTPerson array for each household.
     * Fill it with persons from the person array that have the same ID as the household.  Set the
     * person array for the household to the created array.
     * @param households
     * @param persons
     * @return An array of households with persons
     */
    public PTHousehold[] addPersonsToHouseholds(PTHousehold[] households, PTPerson[] persons){
        
        //Sort households by household ID and persons by person ID
        sortPersonsAndHouseholds(households, persons);

        try{
        	//persons counter
        	int p=0;
        	//loop through all households
        	for(int h=0;h<households.length;h++){
        		households[h].persons = new PTPerson[households[h].size];
        		for(int personNumber=0;personNumber<households[h].persons.length;personNumber++){
        			if(persons[p].hhID!=households[h].ID){
        				Exception e = new Exception();
        				throw e;
        			}
        			households[h].persons[personNumber]= persons[p];
        			p++;
        			if(p==persons.length) break;
        		}
        	}
            
        }catch(Exception e){
             logger.fatal("Household file and person file don't match up.  Unable to add persons to households.");
             e.printStackTrace();
            //TODO - log this exception to the node exception logger
             System.exit(1);
        }
        return households;
    }

    private void sortPersonsAndHouseholds(PTHousehold[] households, PTPerson[] persons){
        //Sort households by household ID
        Arrays.sort(households, new Comparator() {
                                                  public int compare(Object a, Object b) {
                                                      PTHousehold hha = (PTHousehold)a;
                                                  	  PTHousehold hhb = (PTHousehold)b;
                                                  	  if(hha.ID<hhb.ID) return -1;
                                                  	  else if(hha.ID>hhb.ID) return 1;
                                                  	  else return 0;
                                                  }
        });

        //Sort persons by household ID
        Arrays.sort(persons, new Comparator() {
                                               public int compare(Object a, Object b) {
                                               	   PTPerson pa = (PTPerson)a;
                                               	   PTPerson pb = (PTPerson)b;
                                               	   if(pa.hhID<pb.hhID) return -1;
                                               	   else if(pa.hhID>pb.hhID) return 1;
                                               	   else return 0;
                                               }
        });


    }

    public static void main(String[] args){
        long startTime = System.currentTimeMillis();
        ResourceBundle ptRb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        PTDataReader dataReader = new PTDataReader(ptRb, globalRb);
        
        logger.info("Starting dataReader");
        
        PTHousehold[] pth = dataReader.readHouseholds("households.file");
        PTPerson[] ptp = dataReader.readPersons("persons.file");
        ptp = dataReader.addHomeTazAndHHWorkSegmentToPersons(pth,ptp);
        pth = dataReader.addPersonsToHouseholds(pth,ptp);
        pth = dataReader.runAutoOwnershipModel(pth);
        logger.info("Finished dataReader");
        logger.info("Total time: "+((System.currentTimeMillis()-startTime)/1000)+" seconds.");
        logger.info("Size of households with persons = "+ObjectUtil.sizeOf(pth));        
    }
}