//Pattern.java
//
package com.pb.despair.pt.survey;
import java.io.*;

/** 
This class stores the out-of-home activity pattern for each person-day,
as well as summary information about the pattern that can
be used in model estimation.
*/
public class Pattern{

    public Pattern(){};
	//To hold the pattern
	public StringBuffer dayPattern = new StringBuffer();

	//Simple number of activities by type in pattern variables
	public int homeActivities;
    public int workActivities;
	public int schoolActivities;
	public int shopActivities;
	public int recreateActivities;
	public int otherActivities;
	public int workBasedTours;
	public int t1Dummy;
	public int t2Dummy;
	public int t3Dummy;
	public int t4Dummy;
	public int t5pDummy;  
	public int wrkDummy;     
	public int schDummy;    
	public int shpDummy;     
	public int recDummy;     
	public int othDummy;     
	public int wkbDummy;
	
	//Number of intermediate stop variables	
	public int tour1IStops;
	public int tour2IStops;
	public int tour3IStops;
	public int tour4PIStops;
 	public int workTourIStops;
	public int nonWorkTourIStops;
	
	//Combination of activities on tour variables
	public int workPSchool;
	public int workPShop;
 	public int workPRecreate;
	public int workPOther;
	public int schoolPShop;
	public int schoolPRecreate;
	public int schoolPOther;
	public int shopPRecreate;
	public int shopPOther;
	public int recreatePOther;

	//stops variables
	public int stopsOnWorkTours;
	public int stopsOnSchoolTours;
	public int stopsOnShopTours;
	public int stopsOnRecreateTours;
	public int stopsOnOtherTours;

	//Sequence variables
	public int tour1Purpose;
	public int tour2Purpose;
	public int tour3Purpose;
	public int tour4Purpose;

	//pattern file variables
	public int observed=1;

	
/** Pattern class constructor 
@param word A day-pattern encoded as a String of any length 
with the following character values:  h=home,w=work(no work-based tour),
b=work(work-based tour),c=school,s=shop,r=social/recreation,o=other.
*/
	public Pattern(String word){

		dayPattern.append(word);

		countActivitiesbyPurpose();

		countIntermediateStops();

		countActivityCombinations();
		
		
	}

	/** counts the number of activities by purpose, stores results in class variables. */
	void countActivitiesbyPurpose(){
		//search through word, count number of activities by purpose
		for(int i=0;i<dayPattern.length();++i){

			char thisChar=dayPattern.charAt(i);

			if(thisChar=='h')
				++homeActivities;
			if(thisChar=='w'){
				++workActivities;
				wrkDummy=1;
			}
			if(thisChar=='b'){
				++workActivities;
			}
			if(thisChar=='c'){
				++schoolActivities;
				schDummy=1;
			}
			if(thisChar=='s'){
				++shopActivities;
				shpDummy=1;
			}
			if(thisChar=='r'){
				++recreateActivities;
				recDummy=1;
			}
			if(thisChar=='o'){
				++otherActivities;
				othDummy=1;
			}
			if(thisChar=='b'){
				++workBasedTours;
				wkbDummy=1;
			}
		}

		if(homeActivities>=2)
			t1Dummy=1;
		if(homeActivities>=3)
			t2Dummy=1;
		if(homeActivities>=4)
			t3Dummy=1;
		if(homeActivities>=5)
			t4Dummy=1;
		if(homeActivities>=6)
			t5pDummy=1;
				
	} //end countActivitiesbyPurpose()

	public String getTourString(int tourNumber){
		String dayString = new String(dayPattern.toString());		
		StringBuffer tourString = new StringBuffer();
		//the following indices are used to locate the at-home activities on either end of a tour
		int lastHomeActivityIndex=0;
		int firstHomeActivityIndex;
		int n=0;
		//get desired tour
		while(dayString.length()>lastHomeActivityIndex+1){
			++n;
			firstHomeActivityIndex=lastHomeActivityIndex;
			lastHomeActivityIndex=dayString.indexOf("h",firstHomeActivityIndex+1);
			if(n==tourNumber){
				tourString.append(dayString.substring(firstHomeActivityIndex,lastHomeActivityIndex+1));
				break;		
			}
		}
		return tourString.toString();
	}


	/** counts the number of intermediate stops in this word, stores results in class variables. */
	void countIntermediateStops(){
		if(homeActivities>=2)
			tour1IStops=getTourString(1).length()-3;
		if(homeActivities>=3)
			tour2IStops=getTourString(2).length()-3;
		if(homeActivities>=4)
			tour3IStops=getTourString(2).length()-3;
		for(int i=5;i<=homeActivities;++i)
			tour4PIStops += getTourString(i-1).length()-3;
	}

	/** counts the number of activity combinations in this word, stores results in class variables */
	void countActivityCombinations(){ 
		int tourNumber=1;
		while(homeActivities>=(tourNumber+1)){
			String thisTour=getTourString(tourNumber);
			boolean workActivity=false;
			boolean schoolActivity=false;
			boolean shopActivity=false;
			boolean recreateActivity=false;
			boolean otherActivity=false;
			//cycle through letters on this tour between two home locations
			for(int i=1;i<(thisTour.length()-1);++i){
				if(thisTour.charAt(i)=='w'||thisTour.charAt(i)=='b')
					workActivity=true;
				if(thisTour.charAt(i)=='c')
					schoolActivity=true;
				if(thisTour.charAt(i)=='s')
					shopActivity=true;
				if(thisTour.charAt(i)=='r')
					recreateActivity=true;
				if(thisTour.charAt(i)=='o')
					otherActivity=true;
			} //end cycling through letters of this tour
			//number of stops
			if(workActivity && thisTour.length()>3)
				stopsOnWorkTours += thisTour.length()-3;
			else if(schoolActivity && thisTour.length()>3)
				stopsOnSchoolTours += thisTour.length()-3;
			else if(shopActivity && thisTour.length()>3)
				stopsOnShopTours += thisTour.length()-3;
			else if(recreateActivity && thisTour.length()>3)
				stopsOnRecreateTours += thisTour.length()-3;
			else if(otherActivity && thisTour.length()>3)
				stopsOnOtherTours += thisTour.length()-3;

			//combinations
			if(workActivity && schoolActivity)
				++workPSchool;
			if(workActivity && shopActivity)
				++workPShop;
			if(workActivity && recreateActivity)
				++workPRecreate;
			if(workActivity && otherActivity)
				++workPOther;
			if(schoolActivity && shopActivity)
				++schoolPShop;
			if(schoolActivity && recreateActivity)
				++schoolPRecreate;
			if(schoolActivity && otherActivity)
				++schoolPOther;
			if(shopActivity && recreateActivity)
				++shopPRecreate;
			if(shopActivity && otherActivity)		
				++shopPOther;
			if(recreateActivity && otherActivity)
				++recreatePOther;

			//sequence
			if(tourNumber==1){
				if(workActivity)
					tour1Purpose=1;
				else if(schoolActivity)
					tour1Purpose=2;
				else if(shopActivity)
					tour1Purpose=3;
				else if(recreateActivity)
					tour1Purpose=4;
				else if(otherActivity)
					tour1Purpose=5;
			}else if(tourNumber==2){
				if(workActivity)
					tour2Purpose=1;
				else if(schoolActivity)
					tour2Purpose=2;
				else if(shopActivity)
					tour2Purpose=3;
				else if(recreateActivity)
					tour2Purpose=4;
				else if(otherActivity)
					tour2Purpose=5;
			}else if(tourNumber==3){
				if(workActivity)
					tour1Purpose=1;
				else if(schoolActivity)
					tour3Purpose=2;
				else if(shopActivity)
					tour3Purpose=3;
				else if(recreateActivity)
					tour3Purpose=4;
				else if(otherActivity)
					tour3Purpose=5;
			}else if(tourNumber==4){
				if(workActivity)
					tour4Purpose=1;
				else if(schoolActivity)
					tour4Purpose=2;
				else if(shopActivity)
					tour4Purpose=3;
				else if(recreateActivity)
					tour4Purpose=4;
				else if(otherActivity)
					tour4Purpose=5;
			}
			++tourNumber;
		} //end this tour	
	}
	public void print(PrintWriter f){
		f.print(	
			homeActivities+" "+
    		workActivities+" "+
			schoolActivities+" "+
			shopActivities+" "+
			recreateActivities+" "+
			otherActivities+" "+
			workBasedTours+" "+
			tour1IStops+" "+
			tour2IStops+" "+
			tour3IStops+" "+
			tour4PIStops+" "+
		 	workTourIStops+" "+
			nonWorkTourIStops+" "+
			workPSchool+" "+
			workPShop+" "+
		 	workPRecreate+" "+
			workPOther+" "+
			schoolPShop+" "+
			schoolPRecreate+" "+
			schoolPOther+" "+
			shopPRecreate+" "+
			shopPOther+" "+
			recreatePOther+" "+
			stopsOnWorkTours+" "+
			stopsOnSchoolTours+" "+
			stopsOnShopTours+" "+
			stopsOnRecreateTours+" "+
			stopsOnOtherTours+" "+
			tour1Purpose+" "+
			tour2Purpose+" "+
			tour3Purpose+" "+
			tour4Purpose+" "+
			t1Dummy+" "+
			t2Dummy+" "+
			t3Dummy+" "+
			t4Dummy+" "+
			t5pDummy+" "+
			wrkDummy+" "+     
			schDummy+" "+     
			shpDummy+" "+     
			recDummy+" "+     
			othDummy+" "+     
			wkbDummy+" "     
		);
	}
	

	
	public boolean equals(Object obj){
		
		Pattern comparePattern = (Pattern)obj;
		boolean tf=false;
        String compareString=comparePattern.dayPattern.toString();
		String thisString=this.dayPattern.toString();
		if(compareString.compareTo(thisString)==0)
			tf=true;
		return tf;
	}

    
}   