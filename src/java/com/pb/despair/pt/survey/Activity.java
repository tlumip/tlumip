// survey.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.despair.pt.survey;

import java.io.*;

public class Activity implements Cloneable{
	public long longid;
	public long sampleNumber;
	public int personNumber;
	public int dayNumber;
	public int activityNumber;
	public int activity;
	public int location1;
	public int location2;
	public int militaryStartHour;
	public int militaryEndHour;
	public int startHour;
	public int startMinute;
	public int startAMPM;
	public int endHour;
	public int endMinute;
	public int endAMPM;
	public int duration;
	public int generalActivity;
	public int parentTourNumber;  		//which tour does activity belong to?
	public int parentTourPrimary;		//is it the primary activity of the tour (0,1)?
	public int parentTourIStop1;		//is it intermediate stop 1 (0,1)?
	public int parentTourIStop2;		//is it intermediate stop 2 (0,1)?
	public int parentTourOrigin;		//is it the home/work origin (0,1)?
	public int parentTourDestination;	//is it the home/work destination (0,1)?
	public char letter;					//identifies the activity pattern letter (w=wrk no wrk-based, b=wrk wrk-based)
	public boolean alreadyThere=true;
	public Location location = new Location();
	public Trip activityTrip = new Trip();

	public void print(){
		System.out.print(dayNumber+","+activityNumber+","+
			activity+","+location1+","+location2+","+startHour+","+
			startMinute+","+startAMPM+","+endHour+","+endMinute+","
			+endAMPM+","+duration+","+location.xCoordinate+","+location.yCoordinate+","+generalActivity);
	};
	
	public void print(PrintWriter pw){
		pw.print(dayNumber+" "+activityNumber+" "+
			activity+" "+location1+" "+location2+" "+startHour+" "+
			startMinute+" "+startAMPM+" "+endHour+" "+endMinute+" "
			+endAMPM+" "+duration+" "+location.xCoordinate+" "+location.yCoordinate+" "+generalActivity);
	}	
	public void setSampleNumber(long s){
		sampleNumber=s;
	}
	public long getSampleNumber(){
		return sampleNumber;
	}
	public void setPersonNumber(int p){
		personNumber=p;
	}
	public int getPersonNumber(){
		return personNumber;
	}
	public void setDayNumber(int d){
		dayNumber=d;
	}
	public int getDayNumber(){
		return dayNumber;
	}
	public void setActivityNumber(int an){
		activityNumber=an;
	}
	public int getActivityNumber(){
		return activityNumber;
	}
	public void setActivity(int a){
		activity=a;
	}
	public int getActivity(){
		return activity;
	}
	public void setLocation1(int l1){
		location1=l1;
	}
	public int getLocation1(){
		return location1;
	}
	public void setLocation2(int l2){
		location2=l2;
	}
	public int getLocation2(){
		return location2;
	}
	public void setDuration(int d){
		duration=d;
	}
	public int getDuration(){
		return duration;
	}
	public void setGeneralActivity(int ga){
		generalActivity=ga;
	}
	public int getGeneralActivity(){
		return generalActivity;
	}
	public void setAlreadyThere(boolean at){
		alreadyThere=at;
	}
	public boolean getAlreadyThere(){
		return alreadyThere;
	}
	public void setLocation(Location l){
		location = l;
	}
	public Location getLocation(){
		return location;
	}
	public void setActivityTrip(Trip t){
		activityTrip=(Trip)t.clone();
	}
	public Trip getActivityTrip(){
		return (Trip)activityTrip.clone();
	}
	public void setStartHour(int h){
		startHour=h;
	}
	public int getStartHour(){
		return startHour;
	}
	public void setStartMinute(int m){
		startMinute=m;
	}
	public int getStartMinute(){
		return startMinute;
	}
	public void setStartAMPM(int a){
		startAMPM=a;
	}
	public int getStartAMPM(){
		return startAMPM;
	}
	public void setEndHour(int h){
		endHour=h;
	}
	public int getEndHour(){
		return endHour;
	}
	public void setEndMinute(int m){
		endMinute=m;
	}
	public int getEndMinute(){
		return endMinute;
	}
	public void setEndAMPM(int a){
		endAMPM=a;
	}
	public int getEndAMPM(){
		return endAMPM;
	}
	//calculate duration: the pm time is in military already
	public void calculateDuration(){
					
		int minutes=(60-startMinute) + endMinute;

		militaryStartHour=startHour;
		militaryEndHour  =endHour;

		//Use if not in military time
		if(startAMPM==2 && startHour!=12)
			militaryStartHour +=12;
		if(endAMPM==2 && endHour !=12)
			militaryEndHour +=12;

		//PM->AM
		if(startAMPM==2 && endAMPM==1 && endHour!=12)
			militaryEndHour +=24;

		//AM->AM where end hour is less than begin hour
		if(startAMPM==1 && endAMPM==1 &&(startHour>endHour))
			militaryEndHour +=24;

		//finally, automatically recode 2:59 am
		if(endAMPM==1 && startHour==2 && startMinute==59)
			militaryEndHour +=24;
			
		int hours = (militaryEndHour-militaryStartHour);
		duration=((hours-1)*60) + minutes;

		if(duration<0)
			duration=0;
	}

	public Object clone() {
		Object o = null;
		try {
			o= super.clone();
		} catch(CloneNotSupportedException e) {
			System.err.println("Activity object cannot clone");
		}
		return o;
	}	


	public static void main(String[] args) {}
}