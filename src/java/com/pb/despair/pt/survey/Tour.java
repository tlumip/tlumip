// Tour.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.tlumip.pt.survey;
import java.io.*;

public class Tour{

	public int tourStartHour;
	public int tourStartMinute;
	public int tourEndHour;
	public int tourEndMinute;
	public int tourDurationHour;
	public int tourDurationMinute;

	public int tourMode;
	public int[] tripModes;
	public int originalStops;
	
	public Object leaveOrigin;
	public Object intermediateStop1;
	public Object destination;
	public Object intermediateStop2; 
	public Object arriveOrigin;
	
	public int iStop1TravelTime;  //time leaving for first stop
	public int iStop2TravelTime;  //time leaving for second stop
	public int primaryDestinationTravelTime;  //time leaving for primary destination
	public int arriveOriginTravelTime;		//time leaving for tour origin

	public boolean hasIntermediateStop1=false;
	public boolean hasIntermediateStop2=false;
	
	/*type:
	  w=work
	  b=work-based
	  c=school
	  m=major shop
	  s=oth shop
	  r=soc/rec
	  o=other
	*/  
	char type;

	public void setTourStartHour(int h){
		tourStartHour=h;
	}
	public int getTourStartHour(){
		return tourStartHour;
	}
	public void setTourStartMinute(int m){
		tourStartMinute=m;
	}
	public int getTourStartMinute(){
		return tourStartMinute;
	}
	public void setTourEndHour(int h){
		tourEndHour=h;
	}
	public int getTourEndHour(){
		return tourEndHour;
	}
	public void setTourEndMinute(int m){
		tourEndMinute=m;
	}
	public int getTourEndMinute(){
		return tourEndMinute;
	}
	public void setTourDurationHour(int h){
		tourDurationHour=h;
	}
	public int getTourDurationHour(){
		return tourDurationHour;
	}
	public void setTourDurationMinute(int m){
		tourDurationMinute=m;
	}
	public int getTourDurationMinute(){
		return tourDurationMinute;
	}
	public void setTourMode(int m){
		tourMode=m;
	}
	public int getTourMode(){
		return tourMode;
	}
	public void setOriginalStops(int s){
		originalStops=s;
	}
	public int getOriginalStops(){
		return originalStops;
	}
	public void setLeaveOrigin(Object o){
		leaveOrigin=o;
	}
	public Object getLeaveOrigin(){
		return leaveOrigin;
	}
	public void setIntermediateStop1(Object o){
		intermediateStop1=o;
	}
	public Object getIntermediateStop1(){
		return intermediateStop1;
	}
	public void setDestination(Object o){
		destination=o;
	}
	public Object getDestination(){
		return destination;
	}
	public void setIntermediateStop2(Object o){
		intermediateStop2=o;
	}
	public Object getIntermediateStop2(){
		return intermediateStop2;
	}
	public void setArriveOrigin(Object o){
		arriveOrigin=o;
	}
	public Object getArriveOrigin(){
		return arriveOrigin;
	}
	public void setHasIntermediateStop1(boolean is1){
		hasIntermediateStop1=is1;
	}
	public boolean getHasIntermediateStop1(){
		return hasIntermediateStop1;
	}
	public void setHasIntermediateStop2(boolean is2){
		hasIntermediateStop2=is2;
	}
	public boolean getHasIntermediateStop2(){
		return hasIntermediateStop2;
	}
	
	public void calculateTravelTimes(){
		if(hasIntermediateStop1){
			iStop1TravelTime=((Activity)getLeaveOrigin()).militaryEndHour*100+((Activity)getLeaveOrigin()).endMinute;
			primaryDestinationTravelTime=((Activity)getIntermediateStop1()).militaryEndHour*100+
				((Activity)getIntermediateStop1()).endMinute;
		}else{
			primaryDestinationTravelTime=((Activity)getLeaveOrigin()).militaryEndHour*100+
				((Activity)getLeaveOrigin()).endMinute;
		}
		if(hasIntermediateStop2){
			iStop2TravelTime=((Activity)getDestination()).militaryEndHour*100
				+((Activity)getDestination()).endMinute;
			arriveOriginTravelTime=((Activity)getIntermediateStop2()).militaryEndHour*100+
				((Activity)getIntermediateStop2()).endMinute;
		}else{
			arriveOriginTravelTime=((Activity)getDestination()).militaryEndHour*100+
				((Activity)getDestination()).endMinute;
		}
			
	}
	

	//to print to screen
	public void print(){

		((Activity)leaveOrigin).print();
		System.out.println();
		if(hasIntermediateStop1){
			System.out.println("IStop 1");
			((Activity)intermediateStop1).print();
			System.out.println();
		}
		((Activity)destination).print();
		System.out.println();
		if(hasIntermediateStop2){
			System.out.println("IStop 2");			
			((Activity)intermediateStop2).print();
			System.out.println();
		}
		((Activity)arriveOrigin).print();
		System.out.println();		
	}
	/**print to a file*/
	public void print(PrintWriter f){
		f.print(tourStartHour+" "+tourStartMinute+" "+tourEndHour+" "+tourEndMinute+
			" "+tourDurationHour+" "+tourDurationMinute+" "+
				((Activity)destination).getGeneralActivity()+" ");
		if(hasIntermediateStop1)
			f.print("1 ");
		else
			f.print("0 ");
		if(hasIntermediateStop2)
			f.print("1 ");
		else
			f.print("0 ");
	}

	
	
	/** codeActivityLetters: codes letters on activities on tour per following:
	  h =home (all home activities coded h)
	  w =work, not work-based tour
	  b =work, work-based tour
	  c = school
	  s = shop
	  r = soc/rec
	  o = other
	*/
	public static void main(String[] args) {}
}
	
