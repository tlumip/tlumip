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
package com.pb.tlumip.pt.survey;

// county8Data.java
//
// This library is used to read the ODOT 8-county
// data
// jef 6/9/00

//package tlumip.java.county8;

import java.io.*;
import java.util.*;

import com.pb.common.util.InTextFile;


public class County8Data{

	public static void main(String[] args)
	throws IOException {

		System.out.println("Executing County8Data");

		County8Households hh = new County8Households("eightcounties_households.csv");

		ArrayList households = hh.getArray();
		
 		County8Persons p = new County8Persons("eightcounties_persons.csv",households);
 		County8Activities a = new County8Activities("eightcounties_activities_latest.csv",households);
		
		//Alocating trips to activities (only necessary for 8-county data, which has trips separate
		new TripAllocator(households);
		//Coding trips for groups of activities at one location
		new TripCoder(households);

		
		for(int i=0;i<hh.getArray().size();++i){
			if(((Household)households.get(i)).sampleNumber==100162){
				((Household)households.get(i)).printAll();
				break;
			}
		}
		
		new TourCoder(households);
		new TourSummaryStatistics(households,"county8.rpt");
		new PersonFile(households,"8ctypersondata.dat");
		new TourFile(households,"8ctytours.dat");
		NestedPatternGenerationFile pg = new NestedPatternGenerationFile(households,"8ctyworkerpatterns.dat",
		 "8ctystudentpatterns.dat","8ctyotherpatterns.dat","8ctywkendpatterns.dat");
//		new PatternEstimation(households,pg.weekdayPatterns,pg.weekendPatterns);
//		new ActivityDurationFile(households);
	}
}
//class County8Households
//This holds data from the County8Household File in an ArrayList
class County8Households{

	ArrayList households = new ArrayList();

	County8Households(String fileName) throws IOException{

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

		Household h = new Household();
		StringTokenizer inToken = new StringTokenizer(inString,",");

		h.sampleNumber = new Long(inToken.nextToken()).longValue();
		h.county =  inToken.nextToken();
        h.householdSize = new Integer(inToken.nextToken()).intValue();
		h.ownHome= new Integer(inToken.nextToken()).intValue();
		h.typeHome= new Integer(inToken.nextToken()).intValue();
        h.numberVehicles= new Integer(inToken.nextToken()).intValue();
        h.yearsResidence= new Integer(inToken.nextToken()).intValue();
        h.incomeLevel= new Integer(inToken.nextToken()).intValue();
        h.income1= new Integer(inToken.nextToken()).intValue();
        h.incomeReference= new Integer(inToken.nextToken()).intValue();
        h.assign= new Integer(inToken.nextToken()).intValue();
        // city,zipcode
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
//class County8Persons
//This class parses a line from the County8Person File into the
//Person class.
class County8Persons{

// Format is pid,id,sampno,county,persno,relate,gender,age,ageno,age999,
//license,employ,occup,indust,length,telecom,shift,jcity,jzip,student,status,
//schname,schcity,level,edu,ethnic,disable,typdisa1,typdisa2,recno,actday1,actday2,
//totact,trpday1,trpday2,tottrip

	County8Persons(String fileName, ArrayList households) throws IOException{

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
				if(((Household)households.get(i)).sampleNumber==p.sampleNumber){
					((Household)households.get(i)).persons.add(p);
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
		Person p = new Person();

		//skip pid,id
		inToken.nextToken();inToken.nextToken();
		p.sampleNumber = new Long(inToken.nextToken()).longValue();
 		//skip county
		inToken.nextToken();
	 	p.personNumber = new Integer(inToken.nextToken()).intValue();
		p.relationship = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==2)
    		p.female=true;
		//skip age,use converted form
		inToken.nextToken();
		p.age = new Float(inToken.nextToken()).intValue();
		//skip age999
		inToken.nextToken();
		p.license = new Integer(inToken.nextToken()).intValue();
		p.employmentStatus = new Integer(inToken.nextToken()).intValue();
		p.occupation = new Integer(inToken.nextToken()).intValue();
		p.industry = new Integer(inToken.nextToken()).intValue();
		p.lengthAtJob = new Integer(inToken.nextToken()).intValue();
		p.telecommute = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
    		p.shift=true;
 		// skip city,zipcode
		inToken.nextToken();inToken.nextToken();
		//skip student
		inToken.nextToken();
		p.studentStatus= new Integer(inToken.nextToken()).intValue();
		//skip schoolname,schoolcity
		inToken.nextToken();inToken.nextToken();
		p.studentLevel = new Integer(inToken.nextToken()).intValue();
		p.educationLevel = new Integer(inToken.nextToken()).intValue();
		p.ethnicity = new Integer(inToken.nextToken()).intValue();
		if( new Integer(inToken.nextToken()).intValue()==1)
			p.disabled  = true;
		p.typeDisability1 = new Integer(inToken.nextToken()).intValue();
		p.typeDisability2 = new Integer(inToken.nextToken()).intValue();
		return p;
	}

}
// County8Activities
//Parses a line from County8ActivityFile to Activity Class
class County8Activities{
	County8Activities(String fileName, ArrayList households) throws IOException{

		//read activity file
		System.out.println("Reading "+fileName);
		InTextFile activityFile = new InTextFile();
		activityFile.open(fileName);
		int read=0,totalAllocated=0;
		String inActivity=new String();
		while((inActivity = activityFile.readLine())!=null){
			Activity a = new Activity();
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
		}
		System.out.println("Activity records read: "+read);
		System.out.println("Activity records allocated: "+totalAllocated);
		activityFile.close();
	}

	Activity parse(String inString){
//1 uid,2 sampno,3 county,4 persno,5 dayno,6 actno,7 activity,8 actloc1,9 actloc2,10 actplace,11 actadd,                 
//12 actxstrt,13 actland,14 actcity,15 actstate,16 actzip,17 starthr,18 startmin,19 startamp,20 endhr,21 endmin,         
//22 endampm,23 duration,24 trdurhr,25 trdurmin,26 trans,27 vehavail,28 paypark,29 partynum,30 vehnum,31 vehicle,        
//32 nonmake,33 nonmodel,34 nonyear,35 driver,36 recno,37 kerchunk,38 model_no,39 pid,40 xcoord,41 ycoord,               

		StringTokenizer inToken = new StringTokenizer(inString,",");
		Activity a = new Activity();
		a.longid = new Long(inToken.nextToken()).longValue();
	 	a.sampleNumber = new Long(inToken.nextToken()).longValue();
	 	//skip county
		inToken.nextToken();
		a.personNumber = new Integer(inToken.nextToken()).intValue();
	 	a.dayNumber = new Integer(inToken.nextToken()).intValue();
	 	a.activityNumber = new Integer(inToken.nextToken()).intValue();
	 	a.activity = new Integer(inToken.nextToken()).intValue();
	 	a.location1= new Integer(inToken.nextToken()).intValue();
	 	a.location2= new Integer(inToken.nextToken()).intValue();
		//skip activity place, actadd, actxstrt,actland,actcity,actstate,actzip
		inToken.nextToken();inToken.nextToken();inToken.nextToken();inToken.nextToken();
		inToken.nextToken();inToken.nextToken();inToken.nextToken();
	 	a.startHour= new Integer(inToken.nextToken()).intValue();
	 	if(a.startHour>12)
	 		a.startHour=a.startHour-12;
	 	a.startMinute= new Integer(inToken.nextToken()).intValue();
	 	a.startAMPM= new Integer(inToken.nextToken()).intValue();
	 	a.endHour= new Integer(inToken.nextToken()).intValue();
	 	if(a.endHour>12)
	 		a.endHour=a.endHour-12;
	 	a.endMinute= new Integer(inToken.nextToken()).intValue();
	 	a.endAMPM= new Integer(inToken.nextToken()).intValue();
	 	a.duration= new Integer(inToken.nextToken()).intValue();
	 	a.activityTrip.tripDurationHour= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.tripDurationMinute= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.mode= new Integer(inToken.nextToken()).intValue();
     	//recode personal vehicle to 7
     	if(a.activityTrip.mode==6)
     		a.activityTrip.mode=7;
     	a.activityTrip.vehicleAvailable= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.payPark= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.partyNumber= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.vehicleNumber= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.autoTrip.partyNumber=a.activityTrip.vehicleNumber;
		//skip vehicle, nonmake, nonmodel,nonyear
		inToken.nextToken();inToken.nextToken();inToken.nextToken();inToken.nextToken();
     	a.activityTrip.drive= new Integer(inToken.nextToken()).intValue();
     	a.activityTrip.autoTrip.drive=a.activityTrip.drive;
		//skip RECNO,KERCHUNK,MODEL_NO,PID
		inToken.nextToken();inToken.nextToken();inToken.nextToken();inToken.nextToken();
		a.location.xCoordinate=  new Double(inToken.nextToken()).doubleValue();
		a.location.yCoordinate= new Double(inToken.nextToken()).doubleValue();
//		if(a.duration==0)
			a.calculateDuration();
		return a;
	}
	//a list of locations read from file
	ArrayList locations = new ArrayList();

	//the following method is used to find the activity location in the locations list
	void findLocation(Activity a){
	
		ListIterator l = locations.listIterator();

		while(l.hasNext()){
			Location thisLocation = (Location)l.next();
	
			if(thisLocation.longid == a.longid){
				a.location.xCoordinate = thisLocation.xCoordinate;
				a.location.yCoordinate = thisLocation.yCoordinate;
				break;
			}
		}
	}

	// Use this method to read activity location file
	// the location file is ascii comma-delimited : long uid, double lat,double long
	void readLocationFile(String locationFile) throws IOException{
			
		InTextFile inFile = new InTextFile();
		inFile.open(locationFile);
		int n=0;
		String inLine= new String();
		System.out.println("Reading "+locationFile);

		while((inLine = inFile.readLine())!=null){
			++n;
//			System.out.println(n);

			if(inLine.length()==0)
				break;

			Location l = new Location();
			StringTokenizer inToken = new StringTokenizer(inLine,",");
			l.longid = new Long(inToken.nextToken()).longValue();
			l.xCoordinate= new Double(inToken.nextToken()).doubleValue();
			l.yCoordinate= new Double(inToken.nextToken()).doubleValue();
			locations.add(l);
		}
	}
}

//following class is used to re-allocate trip records, which are independently
//stored as separate records in the 8-county data, to the following activity
class TripAllocator{
	int intermediateActivitiesAdded=0,homeToHomeTrips=0,homeActivitiesAdded=0;

	//the constructor takes a List of households
	TripAllocator(List households){

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
						newActivity.activityTrip=(Trip)thisActivity.activityTrip.clone();
						a.add(newActivity);
						++intermediateActivitiesAdded;
					}
				}
			}
		}
/*		System.out.println("TripAllocator: Prior to removing home->home trips");
		a=activities.listIterator();
		while(a.hasNext()){
			((Activity)a.next()).print();
		}
*/
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
/*					System.out.println("deleting");
					priorActivity.print();
 					thisActivity.print();					
*/					a.remove();		
					++homeToHomeTrips;													//remove it
				}
			}
		}
/*		System.out.println("TripAllocator: After removing home->home trips");
		a=activities.listIterator();
		while(a.hasNext()){
			((Activity)a.next()).print();
		}
*/		//reset list iterator
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

		//append trip information to the next activity and delete the trip record
		a = activities.listIterator();		
		while(a.hasNext()){
            Activity thisActivity = (Activity)a.next();
			++totalActivities;
			
			if(thisActivity.activity==4){   			//it is a trip 
				Trip trip = (Trip)thisActivity.activityTrip.clone();	//get the trip information
				++totalTrips;
				a.remove();								//remove the current activity

				if(a.hasNext()){
					thisActivity = (Activity)a.next();		//goto the next activity
					thisActivity.activityTrip = (Trip)trip.clone();		//set its trip information to the last trip
					thisActivity.alreadyThere=false;		//not already there
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
					nextActivity.activityTrip = (Trip)trip.clone();					
					a.add(new Activity());    				//add to List of activities		
				} 
			}
		}
//		System.out.println("Total Activities = "+totalActivities);
//		System.out.println("Total Trips      = "+totalTrips);
	}

}

