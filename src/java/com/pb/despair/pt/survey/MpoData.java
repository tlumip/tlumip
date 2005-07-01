package com.pb.tlumip.pt.survey;

// MpoData.java
//
// This library is used to read and code MPO and 3-county data
// jef 8/00


import java.io.*;
import java.util.*;

import com.pb.common.util.InTextFile;

public class MpoData{

	public static void main(String[] args)
	throws IOException {


		System.out.println("Executing MpoData");

		MpoHouseholds hh = new MpoHouseholds("allhousehold.csv");

		ArrayList households = hh.getArray();
		
 		MpoPersons p = new MpoPersons("allperson.csv",households);
 		MpoActivities a = new MpoActivities("all_acts2.csv",households);
		
		//Alocating trips to activities (this class was changed to only add at-home for first activity)
		new TripAllocator(households);
		//Coding trips for groups of activities at one location
		new TripCoder(households);

		
		for(int i=0;i<hh.getArray().size();++i){
			if(((Household)households.get(i)).sampleNumber==220615){
				((Household)households.get(i)).printAll();
				break;
			}
		}
		
		new TourCoder(households);
		new TourSummaryStatistics(households,"mpo.rpt");
		new PersonFile(households,"personTours.dat");
		new TourFile(households,"tours.dat");
		NestedPatternGenerationFile pg =	new NestedPatternGenerationFile(households,"mpoworkerpatterns.dat",
		    "mpostudentpatterns.dat","mpootherpatterns.dat","mpowkendpatterns.dat");
//		new PatternEstimation(households,pg.weekdayPatterns,pg.weekendPatterns);
//		new ActivityDurationFile(households);
	}

}
//class MpoHouseholds
//This holds data from the Mpo File in an ArrayList
class MpoHouseholds{

	protected ArrayList households = new ArrayList();

	MpoHouseholds(String fileName) throws IOException{

		//read household file
		System.out.println("Reading "+fileName);
		InTextFile householdFile = new InTextFile();
		householdFile.open(fileName);

		String inHousehold=new String();

		while((inHousehold = householdFile.readLine())!=null){
			if(inHousehold.length()==0)
				break;
			try{
				households.add(parse(inHousehold));
			}catch(Exception e){
				System.out.println("Error parsing household:\n"+inHousehold);
				System.exit(1);
			}
		}
		householdFile.close();
	}

	//parse method takes a string and returns a household object
	Household parse(String inString){

		MpoHousehold h = new MpoHousehold();
		StringTokenizer inToken = new StringTokenizer(inString,",");

		h.phase = new Integer(inToken.nextToken()).intValue();
		h.interviewNumber = new Integer(inToken.nextToken()).intValue();
		h.sampleNumber = new Long(inToken.nextToken()).longValue();
		h.city   =  inToken.nextToken();
		h.state  =  inToken.nextToken();
		h.zip    =  inToken.nextToken();
		h.sampleType = new Integer(inToken.nextToken()).intValue();
		h.stratum = new Integer(inToken.nextToken()).intValue();
        h.householdSize = new Integer(inToken.nextToken()).intValue();
		h.phones = new Integer(inToken.nextToken()).intValue();
		h.partyLine  = new Integer(inToken.nextToken()).intValue();
		h.carPhone = new Integer(inToken.nextToken()).intValue();
        h.numberVehicles= new Integer(inToken.nextToken()).intValue();
		h.ownHome= new Integer(inToken.nextToken()).intValue();
        h.yearsResidence= new Integer(inToken.nextToken()).intValue();
		h.typeHome= new Integer(inToken.nextToken()).intValue();
		h.oldArea = new Integer(inToken.nextToken()).intValue();
        h.income1= new Integer(inToken.nextToken()).intValue();
        // travel date1,date2
		inToken.nextToken();inToken.nextToken();
		h.day1= new Integer(inToken.nextToken()).intValue();
        h.day2= new Integer(inToken.nextToken()).intValue();
		return h;
	}
	//to get the arraylist
	ArrayList getArray(){
		return households;
	}
}

//class MPO household 
class MpoHousehold extends Household{

	int phase;
	int interviewNumber;
	String city;
	String state;
	String zip;
	int sampleType;
	int phones;
	int partyLine;
	int carPhone;
	int oldArea;
}



//class MpoPersons
//This class parses a line from the MPO and 3-county person File into the
//Person class.
class MpoPersons{

// Format is pid,id,sampno,county,persno,relate,gender,age,ageno,age999,
//license,employ,occup,indust,length,telecom,shift,jcity,jzip,student,status,
//schname,schcity,level,edu,ethnic,disable,typdisa1,typdisa2,recno,actday1,actday2,
//totact,trpday1,trpday2,tottrip

	MpoPersons(String fileName, ArrayList households) throws IOException{

		//read person file
		System.out.println("Reading "+fileName);
		InTextFile personFile = new InTextFile();
		personFile.open(fileName);

		String inPerson=new String();
		int read=0,allocated=0;

		while((inPerson = personFile.readLine())!=null){
			Person p = new Person();
			if(inPerson.length()==0)
				break;
			try{
				p = parse(inPerson);
			}catch(Exception e){
				System.out.println("Error parsing person:\n" + inPerson);
				System.exit(1);
			}
			++read;
			//get the index of the household for p, add the person to it;
			for(int i=0;i<households.size();++i){
			    Household h = (Household) households.get(i);
				if(h.sampleNumber==p.sampleNumber){
					(h.persons).add(p);
					++allocated;
					break;
				}
			}
		}
		System.out.println("Person records read: "+read);
		System.out.println("Person records allocated: "+allocated);
		personFile.close();
	}

	//parse method takes a string and returns a person object
	Person parse(String inString){

		StringTokenizer inToken = new StringTokenizer(inString,",");
		MpoPerson p = new MpoPerson();

		//skip pid
		inToken.nextToken();
		p.phase = new Integer(inToken.nextToken()).intValue();
		p.sampleNumber = new Long(inToken.nextToken()).longValue();
	 	p.personNumber = new Integer(inToken.nextToken()).intValue();
		p.relationship = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==2)
    		p.female=true;
		p.age = new Float(inToken.nextToken()).intValue();
		p.ethnicity = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
    		p.notEnglish=true;
		p.otherLanguage = new Integer(inToken.nextToken()).intValue();
		p.speakEnglish = new Integer(inToken.nextToken()).intValue();
		p.license = new Integer(inToken.nextToken()).intValue();
		p.employmentStatus = new Integer(inToken.nextToken()).intValue();
		p.hoursWorked = new Integer(inToken.nextToken()).intValue();
		p.occupation = new Integer(inToken.nextToken()).intValue();
		p.industry = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
    		p.workAtHome=true;
		p.hoursWorkedAtHome = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
    		p.subsidizedParking=true;
		if( new Integer(inToken.nextToken()).intValue()==1)
			p.shift = true;
		if( new Integer(inToken.nextToken()).intValue()==1)
			p.paysToPark = true;
		p.parkingCost = new Float(inToken.nextToken()).floatValue();
 		// skip drive,carpool,transit,other,nowork
		inToken.nextToken();inToken.nextToken();inToken.nextToken();inToken.nextToken();inToken.nextToken();
		p.lengthAtJob = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
			p.twoJobs= true;
		p.lastJob = new Integer(inToken.nextToken()).intValue();
		p.studentStatus = new Integer(inToken.nextToken()).intValue();
		p.headHouseholdStudentLevel = new Integer(inToken.nextToken()).intValue();
		p.studentLevel= new Integer(inToken.nextToken()).intValue();
		//skip schoolname,schoolcity
		inToken.nextToken();inToken.nextToken();
		//skip schldrive,schpool,schbus,schother,nosch
		inToken.nextToken();inToken.nextToken();inToken.nextToken();inToken.nextToken();inToken.nextToken();
		if( new Integer(inToken.nextToken()).intValue()==1)
			p.disabled = true;
		p.typeDisability1 = new Integer(inToken.nextToken()).intValue();
        if(p.relationship==9)
			p.studentLevel=p.headHouseholdStudentLevel;
		return p;
	}

}

class MpoPerson extends Person{

	public int phase;	
	public boolean notEnglish;
	public int otherLanguage;
	public int speakEnglish;
	public int hoursWorked;
	public boolean workAtHome;
	public int hoursWorkedAtHome;
	public boolean subsidizedParking;
	public boolean paysToPark;
	public float parkingCost;
	public boolean twoJobs;
	public int lastJob;
	public int headHouseholdStudentLevel;
}
// MpoActivities
//Parses a line from MpoActivityFile to Activity Class
class MpoActivities{
	MpoActivities(String fileName, ArrayList households) throws IOException{

		//read activity file
		System.out.println("Reading "+fileName);
		InTextFile activityFile = new InTextFile();
		activityFile.open(fileName);
		long read=0,totalAllocated=0;
		String inActivity=new String();
		while((inActivity = activityFile.readLine())!=null){
			MpoActivity a = new MpoActivity();
			if(inActivity.length()==0)
				break;
			try{
				a = parse(inActivity);
			}catch(Exception e){
				System.out.println("Error parsing activity:\n" + inActivity);
				System.exit(1);
			}
			++read;
			boolean allocated=false;
			//find the activity in the household\person vector
			for(int i=0;i<households.size();++i){
				if(((Household)households.get(i)).sampleNumber==a.sampleNumber){ 	//found household
					Household h = (Household) households.get(i);
					for(int j=0;j<h.persons.size();++j){							// now find person
						long persno = ((Person)h.persons.get(j)).personNumber;
						if(persno==a.personNumber){									// found person, now allocate
							if(a.dayNumber==1)
								((Person)h.persons.get(j)).day1Activities.add(a);
							else
								((Person)h.persons.get(j)).day2Activities.add(a);
							++totalAllocated;
							allocated=true;
							break;
						}
					}
				}
				if(allocated)
					break;
			}
			if(read==1 || (read % 10000) ==0)
				System.out.println("Observation number "+read);
		}
		System.out.println("Activity records read: "+read);
		System.out.println("Activity records allocated: "+totalAllocated);
		activityFile.close();
	}
// uid phase stratum sampno persno dayno actno q1 q3 actloc q4 q4ampm q5
//   q5ampm q7 q8 q8a q8b q8btime q9 q10 q11 q11a q11atime q12 q13name q14 q14a 
// q15 q16 q17 q18 q19 q19a q20 q21 q22 q23 q24 q25 q25time q26 q27 q27time q28 
// q28ampm q29 q29ampm q29ahour q29amin q30 q31name q32 x_coord y_coord

	MpoActivity parse(String inString){
		StringTokenizer inToken = new StringTokenizer(inString,",");
		MpoActivity a = new MpoActivity();
		//skip longid
//		a.longid = new Long(inToken.nextToken()).longValue();
		inToken.nextToken();
		a.phase = new Integer(inToken.nextToken()).intValue();
		a.stratum = new Integer(inToken.nextToken()).intValue();
	 	a.sampleNumber = new Long(inToken.nextToken()).longValue();
		a.personNumber = new Integer(inToken.nextToken()).intValue();
	 	a.dayNumber = new Integer(inToken.nextToken()).intValue();
	 	a.activityNumber = new Integer(inToken.nextToken()).intValue();
	 	a.activity = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==2){
//			System.out.println("Not already There");
    		a.alreadyThere=false;
		}
	 	a.location1= new Integer(inToken.nextToken()).intValue();
		//start time		
		String startTime = inToken.nextToken();
	 	a.startAMPM= new Integer(inToken.nextToken()).intValue();

			//calculate startHour, startMinute
			if(startTime.length()==4){
				a.startHour= new Integer(startTime.substring(0,2)).intValue(); 
				a.startMinute = new Integer(startTime.substring(2,4)).intValue();
			}else if(startTime.length()==3){
				a.startHour= new Integer(startTime.substring(0,1)).intValue(); 
				a.startMinute = new Integer(startTime.substring(1,3)).intValue();
			}else if(startTime.length()==2){
				a.startHour=0;
				a.startMinute=new Integer(startTime).intValue();
			}
		String endTime = inToken.nextToken();
	 	a.endAMPM= new Integer(inToken.nextToken()).intValue();

		//calculate endHour, endMinute
		if(endTime.length()==4){
			a.endHour= new Integer(endTime.substring(0,2)).intValue(); 
			a.endMinute = new Integer(endTime.substring(2,4)).intValue();
		}else if(endTime.length()==4||endTime.length()==3){
			a.endHour= new Integer(endTime.substring(0,1)).intValue(); 
			a.endMinute = new Integer(endTime.substring(1,3)).intValue();
		}else if(endTime.length()==2){
			a.endHour=0;
			a.endMinute=new Integer(endTime).intValue();
		}

     	a.activityTrip.mode= new Integer(inToken.nextToken()).intValue();
     	
/*     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
     	inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
*/
		//Next few Questions for non-motorized trips
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.otherTrip.vehicleAvailable=true;
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.otherTrip.payPark=true;
		a.activityTrip.otherTrip.parkingCost= new Float(inToken.nextToken()).floatValue();
	 	a.activityTrip.otherTrip.parkingTime= new Integer(inToken.nextToken()).intValue();		
	 	a.activityTrip.otherTrip.partyNumber= new Integer(inToken.nextToken()).intValue();		

		//Next few Questions for transit (bus & max) trips
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.transitTrip.vehicleAvailable=true;
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.transitTrip.payPark=true;
		a.activityTrip.transitTrip.parkingCost= new Float(inToken.nextToken()).floatValue();
	 	a.activityTrip.transitTrip.parkingTime= new Integer(inToken.nextToken()).intValue();		
		//skip first route
		inToken.nextToken();
		//skip boarding location
		inToken.nextToken();
		a.activityTrip.transitTrip.accessMode =new Integer(inToken.nextToken()).intValue();	
		a.activityTrip.transitTrip.egressMode =new Integer(inToken.nextToken()).intValue();	
		a.activityTrip.transitTrip.fareType=new Integer(inToken.nextToken()).intValue();
		a.activityTrip.transitTrip.subsidizedFare=new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.transitTrip.transfer=true;
		//skip transfer route
		inToken.nextToken();
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.transitTrip.transferAgain=true;
	 	a.activityTrip.transitTrip.partyNumber= new Integer(inToken.nextToken()).intValue();		

		//Finally, a set of questions if mode is auto vehicle
		a.activityTrip.autoTrip.vehicleNumber= new Integer(inToken.nextToken()).intValue();	
		a.activityTrip.autoTrip.drive=new Integer(inToken.nextToken()).intValue();
		a.activityTrip.drive=a.activityTrip.autoTrip.drive;
		a.activityTrip.autoTrip.partyNumber= new Integer(inToken.nextToken()).intValue();
		a.activityTrip.vehicleNumber=a.activityTrip.autoTrip.partyNumber;	
		a.activityTrip.autoTrip.parkLocation= new Integer(inToken.nextToken()).intValue();		
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.autoTrip.payPark=true;
		a.activityTrip.autoTrip.parkingCost= new Float(inToken.nextToken()).floatValue();
		a.activityTrip.autoTrip.parkingTime= new Integer(inToken.nextToken()).intValue();	
		a.activityTrip.autoTrip.subsidizedPark= new Integer(inToken.nextToken()).intValue();	
		a.activityTrip.autoTrip.fullParkingCost= new Float(inToken.nextToken()).floatValue();
		a.activityTrip.autoTrip.fullParkingTime= new Integer(inToken.nextToken()).intValue();	

		a.activityTrip.startTime= new Integer(inToken.nextToken()).intValue();
		a.activityTrip.startAMPM= new Integer(inToken.nextToken()).intValue();
		a.activityTrip.endTime = new Integer(inToken.nextToken()).intValue();
		a.activityTrip.endAMPM = new Integer(inToken.nextToken()).intValue();
	 	a.activityTrip.tripDurationHour= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.tripDurationMinute= new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
			a.activityTrip.changeModes=true;

		//skip where changed from
		inToken.nextToken();

     	a.activityTrip.modeChanged= new Integer(inToken.nextToken()).intValue();

/*     	a.activityTrip.vehicleAvailable= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.payPark= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.partyNumber= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.vehicleNumber= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.drive= new Integer(inToken.nextToken()).intValue();
*/
		a.location.xCoordinate=  new Double(inToken.nextToken()).doubleValue();
		a.location.yCoordinate= new Double(inToken.nextToken()).doubleValue();
		a.location.taz = new Long(inToken.nextToken()).longValue();
//		if(a.duration==0)
			a.calculateDuration();
		return a;

	}
}
class MpoActivity extends Activity{

	public int phase;
	public int stratum;
}



//following class is used to re-allocate trip records, which are independently
//stored as separate records in the 8-county data, to the following activity
class MpoTripAllocator{
	int intermediateActivitiesAdded=0,homeToHomeTrips=0,homeActivitiesAdded=0;

	//the constructor takes a List of households
	MpoTripAllocator(List households){

		System.out.println("Allocating trips to activities");
		for(int i=0;i<households.size();++i){
			Household h = (Household) households.get(i);
			for(int j=0;j<h.persons.size();++j){							// get person
				Person p = (Person)h.persons.get(j);
			
//				System.out.println("Household "+h.sampleNumber+" Person "+p.personNumber);
				
				if(p.day1Activities.size()>0)
					allocateTrips(p.day1Activities);
				if(p.day2Activities.size()>0)
					allocateTrips(p.day2Activities);
			}
		}
		System.out.println("Added "+intermediateActivitiesAdded+" intermediate activities");
		System.out.println("Removed "+homeToHomeTrips+" home to home trips");
		System.out.println("Added "+homeActivitiesAdded+" home activities at start of day");
	}

	//here is the method called by the constructor; pass a List of activities
	void allocateTrips(List activities){
		
		ListIterator a = activities.listIterator();
		int totalTrips=0;
		int totalActivities=0;

/*		
		//first sort through the activities, see if there are two trips in a row, which is an error,
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.activity==4){							//found a trip
				if(a.hasNext()){	
					Activity nextActivity = (Activity)a.next();		//goto next activity
					if(nextActivity.activity==4){				//found a trip on next activity too!
						a.previous();							//go back one
						Activity newActivity = new Activity();	//create a new activity to add
						newActivity.sampleNumber=thisActivity.sampleNumber;
						newActivity.personNumber=thisActivity.personNumber;
						newActivity.dayNumber=thisActivity.dayNumber;
						newActivity.activity=99;			//unknown activity
						newActivity.location1=4;	//unknown location1
						newActivity.location2=4;	//unknown location2
						newActivity.startHour=thisActivity.endHour;
						newActivity.startMinute=thisActivity.endMinute;
						newActivity.startAMPM=thisActivity.endAMPM;
						newActivity.endHour=nextActivity.startHour;
						newActivity.endMinute=nextActivity.startMinute;
						newActivity.endAMPM=nextActivity.startAMPM;
						newActivity.calculateDuration();
						newActivity.location=thisActivity.location;
						a.add(newActivity);
						++intermediateActivitiesAdded;
					}
				}
			}
		}
		//now sort through the activities, find if there is a trip from home to home, which
		//is an error.  If so, delete the trip.
		a=activities.listIterator();
		int n=-1;
		while(a.hasNext()){
			++n;
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.activity==4 && thisActivity.location1==1 && n>0){//it is a trip to home
				a.previous();
				Activity priorActivity=(Activity)a.previous();							//previous activity
				a.next();
				thisActivity=(Activity)a.next();
				if(priorActivity.location1==1){									//it is a trip from home
					a.remove();		
					++homeToHomeTrips;													//remove it
				}
			}
		}
*/
		//reset list iterator
		a = activities.listIterator();
		Location homeLocation = new Location();
		//look at the first activity of the day.  If it is not at home,see if there is another
		//at-home activity.  If there is, add an at-home activity at the first activity of the day
		if(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.location1!=1){
				a=activities.listIterator();
				boolean home=false;
				while(a.hasNext()){
					Activity searchActivity=(Activity) a.next();
					if(searchActivity.location1==1){			//found a home activity
						home=true;
						homeLocation = searchActivity.location;
						break;
					}
				}
				if(home){	
					a=activities.listIterator();
					Activity homeActivity = new Activity();
					homeActivity.startHour=3;
					homeActivity.startAMPM=1;
					homeActivity.endHour=thisActivity.startHour;
					homeActivity.endMinute=thisActivity.startMinute;
					homeActivity.endAMPM=thisActivity.startAMPM;
					homeActivity.calculateDuration();
					homeActivity.location1=1;
					homeActivity.location2=1;
					homeActivity.sampleNumber=thisActivity.sampleNumber;
					homeActivity.personNumber=thisActivity.personNumber;
					homeActivity.dayNumber=thisActivity.dayNumber;
					homeActivity.activity=99;
					homeActivity.location=homeLocation;
					a.add(homeActivity);
					++homeActivitiesAdded;
					//and set the alreadyThere variable for the next trip
					Activity nextActivity= (Activity)a.next();
					nextActivity.setAlreadyThere(false);
				}
			}
		}
/*
		//append trip information to the next activity and delete the trip record
		a = activities.listIterator();		
		while(a.hasNext()){
            Activity thisActivity = (Activity)a.next();
			++totalActivities;
			
			if(thisActivity.activity==4){   			//it is a trip 
				Trip trip = thisActivity.activityTrip;	//get the trip information
				++totalTrips;
				a.remove();								//remove the current activity

				if(a.hasNext()){
					thisActivity = (Activity)a.next();		//goto the next activity
					thisActivity.activityTrip = trip;		//set its trip information to the last trip
				}else{										//there is no activity after this trip for this personday
					Activity nextActivity = new Activity();	//create an activity
					nextActivity.sampleNumber=thisActivity.sampleNumber;
					nextActivity.personNumber=thisActivity.personNumber;
					nextActivity.dayNumber=thisActivity.dayNumber;
					nextActivity.activityNumber=thisActivity.activityNumber+1;
					nextActivity.activity=99;
					nextActivity.location1=1;
					nextActivity.location2=1;
					nextActivity.startHour=thisActivity.endHour;
					nextActivity.startMinute=thisActivity.endMinute;
					nextActivity.startAMPM=thisActivity.endAMPM;
					nextActivity.calculateDuration();
					nextActivity.activityTrip = trip;					
					a.add(new Activity());    				//add to List of activities		
				} 
			}
		}
*/
//		System.out.println("Total Activities = "+totalActivities);
//		System.out.println("Total Trips      = "+totalTrips);
	}
}

