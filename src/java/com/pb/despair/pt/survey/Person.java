// survey.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.tlumip.pt.survey;
import java.util.*;
import java.io.*;

import com.pb.common.util.OutTextFile;

public class Person{

	public long sampleNumber;

	public int personNumber;
	public int relationship;
	public boolean female;
	public float age;
	public int license;
	public int employmentStatus;
	public int occupation;
	public int industry;
	public int lengthAtJob;
	public int telecommute;
	public boolean shift;
	public int studentStatus;
	public int studentLevel;
	public int educationLevel;
	public int ethnicity;
	public boolean disabled;
	public int typeDisability1;
	public int typeDisability2;

	public ArrayList day1Activities = new ArrayList();
	public ArrayList day2Activities = new ArrayList();
	public ArrayList day1Trips = new ArrayList();
	public ArrayList day2Trips = new ArrayList();
	public ArrayList day1Tours = new ArrayList();
	public ArrayList day2Tours = new ArrayList();
	

    /** getWordWithStops returns a word describing the activities
    on Home-Based tours in person-day 1. Two at-home 
    activities in  a row are reported as one letter 'h'.
    The codes are h=home,w=work-no workbased,b=work-workbased, c=school,s=shop,
    r=soc/rec,o=other.  Each intermediate stop activity is 
    enumerated, with the stop purpose. */
    public String getWordWithStops(int dayno){
        char[] activityLetters = new char[] { 'b',
            'w','c','s','s','r','o','o','o','w'};
        StringBuffer dayLetters = new StringBuffer();
        ListIterator t=day1Tours.listIterator();
        if(dayno==2)
        	t=day2Tours.listIterator();
		boolean workBasedTour=false;
        while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity thisActivity=(Activity)thisTour.getLeaveOrigin();
			
			//if tour is work-based, continue
			if(thisTour.type=='b'){
				workBasedTour=true;
				thisActivity.letter='b';
				thisActivity=(Activity)thisTour.getArriveOrigin();
				thisActivity.letter='b';
				thisActivity=(Activity)thisTour.getDestination();
				thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				if(thisTour.hasIntermediateStop1){
					thisActivity=(Activity)thisTour.getIntermediateStop1();
					thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				}
				if(thisTour.hasIntermediateStop2){
					thisActivity=(Activity)thisTour.getIntermediateStop2();
					thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				}
				continue;
			}
			
			thisActivity.letter='h';
			//append h to dayLetters
            dayLetters.append("h");
			//Intermediate Stop 1
            if(thisTour.hasIntermediateStop1){
				thisActivity = (Activity)thisTour.getIntermediateStop1();
				thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
			}
			//primary destination
			thisActivity = (Activity)thisTour.getDestination();

			if(activityLetters[thisActivity.getGeneralActivity()]=='w'){
				//it is a work tour, check to see if last tour was a work-based
				if(workBasedTour){
					dayLetters.append('b');
					workBasedTour=false;
					thisActivity.letter='b';
				}else{
					dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
           			thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
           		}
			}else{
				dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
	            thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
            }
			//intermediate stop 2
			if(thisTour.hasIntermediateStop2){
				thisActivity = (Activity)thisTour.getIntermediateStop2();
				thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
			}
			thisActivity = (Activity)thisTour.getArriveOrigin();
			if(thisActivity.getGeneralActivity()==1||thisActivity.getGeneralActivity()==9){
				if(thisActivity.getGeneralActivity()==1)
					thisActivity.letter='w';
				else
					thisActivity.letter='b';
			}else{
				thisActivity.letter='h';
			}	
		} //next tour
        dayLetters.append("h");
      	return dayLetters.toString();
	}
			
          
	/** numberOfDay1HomeBasedTourIStops searches the number of the tour
   passed as an argument and returns the number of stops on the tour if 
   it is a home-based tour. 
	@param numberOfTour.
    @return number of intermediate stops on the tour, 0 if no stops or no tour, -1 if
    not a home-based tour */
 	public int numberOfDay1HomeBasedTourIStops(int numberOfTour){

		int numberOfIStops=0;

		if(numberOfTour<=day1Tours.size()){
			Tour thisTour = (Tour)day1Tours.get(numberOfTour-1);
			Activity originActivity = (Activity)thisTour.getLeaveOrigin();
			if(originActivity.getGeneralActivity()==1||originActivity.getGeneralActivity()==9){
				numberOfIStops=-1;
			}else{
				if(thisTour.hasIntermediateStop1)
					++numberOfIStops;
				if(thisTour.hasIntermediateStop2)
					++numberOfIStops;
			}
		}
		return numberOfIStops;
	}

	/** numberOfDay2HomeBasedTourIStops searches the number of the tour
   passed as an argument and returns the number of stops on the tour if 
   it is a home-based tour. 
	@param numberOfTour.
    @return number of intermediate stops on the tour, 0 if no stops or no tour, -1 if
    not a home-based tour */
 	public int numberOfDay2HomeBasedTourIStops(int numberOfTour){

		int numberOfIStops=0;

		if(numberOfTour<=day2Tours.size()){
			Tour thisTour = (Tour)day2Tours.get(numberOfTour-1);
			Activity originActivity = (Activity)thisTour.getLeaveOrigin();
			if(originActivity.getGeneralActivity()==1||originActivity.getGeneralActivity()==9){
				numberOfIStops=-1;
			}else{
				if(thisTour.hasIntermediateStop1)
					++numberOfIStops;
				if(thisTour.hasIntermediateStop2)
					++numberOfIStops;
			}
		}
		return numberOfIStops;
	}
	/** numberOfDay1Tours searches the generalActivity number
	of each tour primary destination and returns the number
	of tours where the primary destination general Activity equals
	the searchActivity	*/
	public int numberOfDay1ActivityTours(int searchActivity){
		int  numberTours=0;
		ListIterator t = day1Tours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity destination = (Activity)thisTour.destination;
			if(destination.generalActivity==searchActivity)
				++numberTours;
		}
		return numberTours;
	}

	/** numberOfDay2Tours searches the generalActivity number
	of each tour primary destination and returns the number
	of tours where the primary destination general Activity equals
	the searchActivity	*/
	public int numberOfDay2ActivityTours(int searchActivity){
		int  numberTours=0;
		ListIterator t = day2Tours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity destination = (Activity)thisTour.destination;
			if(destination.generalActivity==searchActivity)
				++numberTours;
		}
		return numberTours;
	}
	/** This method returns the total number of intermediate stops
	on all tours on day1 */
	public int numberOfDay1Stops(){
		int numberStops=0;
		ListIterator t = day1Tours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			if(thisTour.hasIntermediateStop1)
				++numberStops;
			if(thisTour.hasIntermediateStop2)
				++numberStops;
		}
		return numberStops;
	}
	/** This method returns the total number of intermediate stops
	on all tours on day2 */
	public int numberOfDay2Stops(){
		int numberStops=0;
		ListIterator t = day2Tours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			if(thisTour.hasIntermediateStop1)
				++numberStops;
			if(thisTour.hasIntermediateStop2)
				++numberStops;
		}
		return numberStops;
	}
	
	/** This method returns the number of day1tours that begin between the beginning
	and ending times passed as arguments.  The beginning time is the time leaving
	home for Home-Based Tours and the time leaving work for Work-Based Tours */
	public int numberOfDay1ToursBeginning(int fromTime, int toTime){
			
		int numberTours=0;
		ListIterator t= day1Tours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity originActivity	= (Activity)thisTour.leaveOrigin;
			//convert origin activity ending time to military
			int leaveTime = originActivity.endHour*100 + originActivity.endMinute;
			if(originActivity.endAMPM==2 && originActivity.endHour!=12)
				leaveTime=leaveTime+1200;

			if(leaveTime>=fromTime && leaveTime<toTime)
				++numberTours;
		}
		return numberTours;
	}	
	
	/** This method returns the number of day2tours that begin between the beginning
	and ending times passed as arguments.  The beginning time is the time leaving
	home for Home-Based Tours and the time leaving work for Work-Based Tours */
	public int numberOfDay2ToursBeginning(int fromTime, int toTime){
			
		int numberTours=0;
		ListIterator t= day2Tours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity originActivity	= (Activity)thisTour.leaveOrigin;
			//convert origin activity ending time to military
			int leaveTime = originActivity.endHour*100 + originActivity.endMinute;
			if(originActivity.endAMPM==2 && originActivity.endHour!=12)
				leaveTime=leaveTime+1200;

			if(leaveTime>=fromTime && leaveTime<toTime)
				++numberTours;
		}
		return numberTours;
	}	
    public boolean isWorker(){
        if(employmentStatus>=1 && employmentStatus<=4)
            return true;
        else 
            return false;
    }
    public boolean isStudent(){
        if(studentStatus==1||studentStatus==2)
            return true;
        else
            return false;
    }		
	public long getSampleNumber(){
		return sampleNumber;
	}
	public int getPersonNumber(){
		return personNumber;
	}
	public int getStudentStatus(){
		return studentStatus;
	}
	public int getEmploymentStatus(){
		return employmentStatus;
	}
	public void setDay1Activities(ArrayList a){
		day1Activities=a;
	}
	public ArrayList getDay1Activities(){
		return day1Activities;
	}
	public void setDay2Activities(ArrayList a){
		day2Activities=a;
	}
	public ArrayList getDay2Activities(){
		return day2Activities;
	}
	public void setDay1Trips(ArrayList trips){
		day1Trips=trips;
	}
	public ArrayList getDay1Trips(){
		return day1Trips;
	}
	public void setDay2Trips(ArrayList trips){
		day2Trips=trips;
	}
	public ArrayList getDay2Trips(){
		return day2Trips;
	}
	public void setDay1Tours(ArrayList tours){
		day1Tours=tours;
	}
	public ArrayList getDay1Tours(){
		return day1Tours;
	}
	public void setDay2Tours(ArrayList tours){
		day2Tours=tours;
	}
	public ArrayList getDay2Tours(){
		return day2Tours;
	}
	
	//to print to screen
	public void print(){
		System.out.println(personNumber+","+relationship+","+female+","+
			age+","+license+","+employmentStatus+","+occupation+","+
			industry+","+lengthAtJob+","+telecommute+","+shift+","
			+studentStatus+","+studentLevel+","+educationLevel+","+
			ethnicity+","+disabled+","+typeDisability1+","+typeDisability2);
	}
	/* to print to file, takes a PrintWriter Object - no new line,space-delimited
	* personNumber
	* relationship
	* female
	* age
	* license
	* employmentStatus
	* occupation
	* industry
	* lengthAtJob
	* telecommute
	* shift
	* studentStatus
	* studentLevel
	* educationLevel
	* ethnicity
	* disabled
	* typeDisability1
	* typeDisability2
	*/
	public void print(PrintWriter f){
		f.print(
			personNumber+" "+
			relationship+" ");
		if(female)
			f.print("1 ");
		else
			f.print("0 ");
		f.print(
			age+" "+
			license+" "+
			employmentStatus+" "+
			occupation+" "+
			industry+" "+
			lengthAtJob+" "+
			telecommute+" ");
		if(shift)
			f.print("1 ");
		else
			f.print("0 ");
		f.print(
			studentStatus+" "+
			studentLevel+" "+
			educationLevel+" "+
			ethnicity+" ");
		if(disabled)
			f.print("1 ");
		else
			f.print("0 ");
		f.print(
			typeDisability1+" "+
			typeDisability2+" ");
	}
	//to print to file, takes a OutTextFile object - no new line,space-delimited
	public void print(OutTextFile f) throws IOException{
		f.print(personNumber+" "+relationship+" "+female+" "+
			age+" "+license+" "+employmentStatus+" "+occupation+" "+
			industry+" "+lengthAtJob+" "+telecommute+" "+shift+" "
			+studentStatus+" "+studentLevel+" "+educationLevel+" "+
			ethnicity+" "+disabled+" "+typeDisability1+" "+typeDisability2+" ");
	}
	//print all to screen
	public void printAll(){
		//print the person data
		System.out.println(personNumber+","+relationship+","+female+","+
			age+","+license+","+employmentStatus+","+occupation+","+
			industry+","+lengthAtJob+","+telecommute+","+shift+","+
			studentStatus+","+studentLevel+","+educationLevel+","+
			ethnicity+","+disabled+","+typeDisability1+","+typeDisability2);
		//print the day1Activities data
		for(int i=0;i<day1Activities.size();++i)
			((Activity)day1Activities.get(i)).print();
		//print the day2Activities data
		for(int i=0;i<day2Activities.size();++i)
			((Activity)day2Activities.get(i)).print();
		//print the day1Trips data
		for(int i=0;i<day1Trips.size();++i)
			((Activity)day1Trips.get(i)).print();
		//print the day2Trips data
		for(int i=0;i<day2Trips.size();++i)
			((Activity)day2Trips.get(i)).print();

}


	
	public static void main(String[] args) {}
}