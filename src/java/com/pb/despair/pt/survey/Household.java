// survey.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.despair.pt.survey;
import java.util.*;
import java.io.*;

import com.pb.common.util.OutTextFile;

public class Household implements Comparable{

	public Household(){}

	public long sampleNumber;
	public String county;
	public int householdSize;
	public int ownHome;
	public int typeHome;
	public int numberVehicles;
	public int yearsResidence;
	public int incomeLevel;
	public int income1;
	public int incomeReference;
	public int assign;
	public int day1;
	public int day2;
	public int stratum;
	public int fullWorkers;
	public int partWorkers;
	public int child0to5;
	public int child5to10;
	public int child10to15;
	public int child15to18;
	public int adults;

	public ArrayList persons = new ArrayList();

	public void calculateStructure(){
		ListIterator p = persons.listIterator();
		while(p.hasNext()){
			Person thisPerson = (Person)p.next();
			if(thisPerson.employmentStatus==1||thisPerson.employmentStatus==3)
				++fullWorkers;
			if(thisPerson.employmentStatus==2||thisPerson.employmentStatus==4)
				++partWorkers;
			
			if(thisPerson.age<=4.999)
				++child0to5;
			else if(thisPerson.age<=9.999)
				++child5to10;
			else if(thisPerson.age<=14.999)
				++child10to15;
			else if(thisPerson.age<=17.999)
				++child15to18;
			else
				++adults;
		}
	}			

	public long getSampleNumber(){
		return sampleNumber;
	}
	public ArrayList getPersons(){
		return persons;
	}

	public int getDay1(){
		return day1;
	}

	public int getDay2(){
		return day2;
	}

	//to print to screen
	public void print(){
		System.out.println(sampleNumber+","+county+","+householdSize+","+
			ownHome+","+typeHome+","+numberVehicles+","+yearsResidence+","+
			incomeLevel+","+income1+","+incomeReference+","+assign+","+
			day1+","+day2);
	}
	/**to print to file, takes a PrintWriter object, space-delimited, no line feed-
	* county not printed
	* samplenumber
	* stratum
	* household size
	* ownHome
	* typeHome
	* numberVehicles
	* yearsResidence
	* incomeLevel1
	* income1
	* incomeReference
	* assign
	* day1
	* day2
	*/
	public void print(PrintWriter f){
		f.print(
 			sampleNumber+" "+
			stratum+" "+
			householdSize+" "+
			ownHome+" "+
			typeHome+" "+
			numberVehicles+" "+
			yearsResidence+" "+
			incomeLevel+" "+
			income1+" "+
			incomeReference+" "+
			assign+" "+
			day1+" "+
			day2+" "+
			fullWorkers+" "+
			partWorkers+" "+
			child0to5+" "+
			child5to10+" "+
			child10to15+" "+
			child15to18+" "+
			adults+" "
		);
	}
	//to print to file, takes a OutTextFile object, space-delimited, no line feed-
	//county not printed
	public void print(OutTextFile f) throws IOException {
		f.print(sampleNumber+" "+stratum+" "+householdSize+" "+
			ownHome+" "+typeHome+" "+numberVehicles+" "+yearsResidence+" "+
			incomeLevel+" "+income1+" "+incomeReference+" "+assign+" "+
			day1+" "+day2+" ");
	}


	//to print household, person, and trip information to screen
	public void printAll(){
		//household information
		System.out.println("Household: "+sampleNumber+","+county+","+householdSize+","+
			ownHome+","+typeHome+","+numberVehicles+","+yearsResidence+","+
			incomeLevel+","+income1+","+incomeReference+","+assign+","+
			day1+","+day2);

		for(int i=0;i<persons.size();++i)
			((Person)persons.get(i)).printAll();

	
	}
		
	


	//for sorting
	public int compareTo(Object h){
		int i=0;
		if(sampleNumber <((Household)h).sampleNumber)
			i=-1;
		else if(sampleNumber > ((Household)h).sampleNumber)
			i=1;
		return i;
	}	

	//for equals when passed a person object
	public boolean equals(Object h){
		System.out.println("HERE\n");
		if(sampleNumber==((Person)h).sampleNumber)
			return true;
		else
			return false;
		
	}

	public static void main(String[] args) {

		System.out.println("Household");
	}
}
