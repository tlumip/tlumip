// survey.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.despair.pt.survey;
import java.util.*;

//This class codes trips on activity survey data
public class TripCoder{

	//constructor takes a list of households, iterates through, calls method for Lists of activities
	public TripCoder(List households){

		System.out.println("Coding Trips for groups of activities");

		for(int i=0;i<households.size();++i){
			Household h = (Household) households.get(i);
			for(int j=0;j<h.persons.size();++j){							// get person
				Person p = (Person)h.getPersons().get(j);
			
//				System.out.println("Household "+h.sampleNumber+" Person "+p.personNumber);
				
				if(p.getDay1Activities().size()>0){
//					System.out.println("Day1Activities>0");
					p.setDay1Trips((ArrayList)codeTrips(p.getDay1Activities()));
				}
				if(p.getDay2Activities().size()>0){
//					System.out.println("Day2Activities>0");
					p.setDay2Trips((ArrayList)codeTrips(p.getDay2Activities()));
				}
			}
		}
	}

	//method to code a days worth of activities into trips, returns a List of trips
	//the method goes through the list of activities, adding each activity
	//to a vector of activities.  If the activity is a trip, it sends the vector of 
	//activities to the prioritizeActivities() method, which returns a single activity,
	//with trip information.  This is added to a vector of trips, which is then returned
	//after all activities in the vector have been prioritized.
	public List codeTrips(List activities){

		ListIterator a = activities.listIterator();
		Activity newActivity = new Activity();		//a new activity
		ArrayList trips = new ArrayList();			//empty arrayList for list of trips

		//Need an ArrayList of the activities leading up to the next trip
		ArrayList tripActivities = new ArrayList();

		// search through these activities, generate subset of activities up to the next trip
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();

			//if thisActivity required a trip, pass the current ArrayList to the prioritize method
			if(thisActivity.getAlreadyThere()==false && tripActivities.size()>0){
				newActivity = prioritizeActivities(tripActivities);	//prioritize the current set of activities
				trips.add(newActivity);
				tripActivities.clear();					//clear the subset of activities
				tripActivities.add(thisActivity);		//add the current activity to the list
			}else{
				tripActivities.add(thisActivity);		//if not a trip, just add the activity to the list
			}
		}
		//now prioritize the last set of activities
		if(tripActivities.size()>0){
			newActivity = prioritizeActivities(tripActivities);
			trips.add(newActivity);
		}
		if(trips.size()<2)		//1 or less trips = no tour
			trips.clear();
		return trips;
	}

	//method to prioritize a set of activities, accumulating activity times,
	//and summarizing to one activity type: use for a set of activities at 
	//one location
	Activity prioritizeActivities(List activities){

		ListIterator a = activities.listIterator();
		int n=0;	//a counter
		int startHour=0,startMinute=0,startAMPM=0;
		Activity thisActivity = new Activity();
		Activity newActivity = new Activity();
		int NUMBER_OF_CRITERIA=7;
		boolean[] tests = new boolean[NUMBER_OF_CRITERIA];

		while(a.hasNext()){
			
			++n;
			
 			thisActivity = (Activity)a.next();
			
			if(thisActivity.getActivity()==12)		//1=work
				tests[0]=true;
			else if(thisActivity.getActivity()==41)	//2=school 
				tests[1]=true;
			else if(thisActivity.getActivity()==15)	//3=major shop
				tests[2]=true;
			else if(thisActivity.getActivity()==14) 	//4=other shop
				tests[3]=true;
			else if(thisActivity.getActivity()>=31 && thisActivity.getActivity()<=56 && thisActivity.getActivity()!=55
				& thisActivity.getActivity()!=41) 
				tests[4]=true;					//5=social-recreational
			else if(thisActivity.getActivity()==22)	//7=pickup/drop-off
				tests[6]=true;
			else								//6=other
				tests[5]=true;					

			//get the first activity's beginning time, and trip information
			if(n==1){
				newActivity.setSampleNumber(thisActivity.getSampleNumber());
				newActivity.setPersonNumber(thisActivity.getPersonNumber());
				newActivity.setDayNumber(thisActivity.getDayNumber());
				newActivity.setStartHour(thisActivity.getStartHour());
				newActivity.setStartMinute(thisActivity.getStartMinute());
				newActivity.setStartAMPM(thisActivity.getStartAMPM());
				newActivity.setActivityTrip(thisActivity.getActivityTrip());
				newActivity.setLocation(thisActivity.getLocation());
				
			}
		}

		//here we are at the last activity
		newActivity.setEndHour(thisActivity.getEndHour());
		newActivity.setEndMinute(thisActivity.getEndMinute());
		newActivity.setEndAMPM(thisActivity.getEndAMPM());
		newActivity.setLocation1(thisActivity.getLocation1());
		newActivity.setLocation2(thisActivity.getLocation2());
		newActivity.calculateDuration();
	
		//iterate through array of tests, set the value of activity to 1 + the test index
		for(int i=0;i<tests.length;++i){
			if(tests[i]==true){
				newActivity.setGeneralActivity((i+1));
				break;
			}
		}

		//set generalActivity to 6 for home activities
/*		if(newActivity.location1==1)
			newActivity.generalActivity=6;
*/
		return newActivity;
	}
}
	
 
