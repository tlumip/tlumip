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
// survey.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.tlumip.pt.survey;
import java.io.*;

public class Trip implements Cloneable{

	public int tripDurationHour;
	public int tripDurationMinute;
	public int mode;
	public int vehicleAvailable;
	public int payPark;
	public int partyNumber;
	public int vehicleNumber;
	public int drive;
	public int startTime;
	public int startAMPM;
	public int endTime;
	public int endAMPM;
	public boolean changeModes;
	public int modeChanged;

	public AutoTripData autoTrip = new AutoTripData();
	public TransitTripData transitTrip = new TransitTripData();
	public OtherTripData otherTrip = new OtherTripData();

	
	public class AutoTripData{
		public int vehicleNumber;
		public int drive;
		public int partyNumber;
		public int parkLocation;
		public boolean payPark;
		public float parkingCost;
		public int parkingTime;
		public int subsidizedPark;
		public float fullParkingCost;
		public int fullParkingTime;
	}	

	public class TransitTripData{
		public boolean vehicleAvailable;
		public boolean payPark;
		public float parkingCost;
		public int parkingTime;
//		public String firstRoute;
//		public String boardingLocation;
		public int accessMode;
		public int egressMode;
		public int fareType;
		public int subsidizedFare;
		public boolean transfer;
//		public String transferRoute;
		public boolean transferAgain;
		public int partyNumber;
	}

	public class OtherTripData{
		public boolean vehicleAvailable;
		public boolean payPark;
		public float parkingCost;
		public int parkingTime;
		public int partyNumber;
	 }
	//to print to screen
	public void print(){
		System.out.print(tripDurationHour+","+tripDurationMinute+","+mode+","+
			vehicleAvailable+","+payPark+","+partyNumber+","+vehicleNumber+","+
			drive);
	}
	public void print(PrintWriter pw){
		pw.print(tripDurationHour+" "+tripDurationMinute+" "+mode+" "
			+vehicleAvailable+" "+payPark+" "+partyNumber+" "
			+vehicleNumber+" "+drive);
	}

	/*following method returns mode code as follows:
	  0 = Unknown
	  1 = Drive-Alone
	  2 = Driver - 2 Person
	  3 = Driver - 3+Person
	  4 = Passgr - 2 Person
	  5 = Passgr - 3+Person
	  6 = Walk
	  7 = Bike
	  8 = School Bus
	  9 = Walk - Bus - Walk
	  10= Walk - Bus - PNR
	  11= Walk - Bus - KNR
	  12= PNR  - Bus - Walk
	  13= KNR  - Bus - Walk
	  14= Walk - MAX - Walk
	  15= Walk- MAX - PNR
	  16= Walk- MAX - KNR
	  17= PNR - MAX - Walk
	  18= KNR - MAX - Walk
	*/
	public int getDetailedMode(){
		int dmode=0;
		
		if(mode==2){			//walk
			dmode=6;
		}else if(mode==3){	//bike
			dmode=7;
		}else if(mode==4){	//school bus
		 	dmode=8;
		}else if(mode==5){	//public bus
			if(transitTrip.accessMode==0){
				dmode=9;
			}else if(transitTrip.accessMode==3 && transitTrip.egressMode==3){
				dmode=9;
			}else if(transitTrip.accessMode==3 && (transitTrip.egressMode==1||transitTrip.egressMode==4)){
				dmode=10;
			}else if(transitTrip.accessMode==3 && transitTrip.egressMode==2){
				dmode=11;
			}else if((transitTrip.accessMode==1||transitTrip.accessMode==4) && transitTrip.egressMode==3){
				dmode=12;
			}else if(transitTrip.accessMode==2 && transitTrip.egressMode==3){
				dmode=13;
			}else{
				dmode=9;}
		}else if(mode==6){	//MAX
			if(transitTrip.accessMode==0){
				dmode=14;
			}else if(transitTrip.accessMode==3 && transitTrip.egressMode==3){
				dmode=14;
			}else if(transitTrip.accessMode==3 && (transitTrip.egressMode==1||transitTrip.egressMode==4)){
				dmode=15;
			}else if(transitTrip.accessMode==3 && transitTrip.egressMode==2){
				dmode=16;
			}else if((transitTrip.accessMode==1||transitTrip.accessMode==4) && transitTrip.egressMode==3){
				dmode=17;
			}else if(transitTrip.accessMode==2 && transitTrip.egressMode==3){
				dmode=18;
			}else{
				dmode=14;}
		}
		else if(mode==7||mode==8){ //Personal or other vehicle
			if(drive==1){
				if(vehicleNumber==1){
				 	dmode=1;
				 }else if(vehicleNumber==2){
				 	dmode=2;
				 }else if(vehicleNumber>=3){
				 	dmode=3;}
			}else if(drive==2){
				if(vehicleNumber==2){
					dmode=4;
				}else if(vehicleNumber>=3){
					dmode=5;
				}else{
					dmode=1;}
			}
		}else
			dmode=1;
		return dmode;
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
	
