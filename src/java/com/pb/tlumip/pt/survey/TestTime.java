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

public class TestTime{

	public static void main(String[] args){
		System.out.println("Executing TestTime");
		new Test();
	}
}	
	

	class Test{
	
		Test(){
			System.out.println("HERE");
			String startTime = "104";
			int startAMPM= 1;
			int startHour=0;
			int startMinute=0;

			//calculate startHour, startMinute
			if(startTime.length()==4){
				startHour= new Integer(startTime.substring(0,2)).intValue(); 
				startMinute = new Integer(startTime.substring(2,4)).intValue();
			}else if(startTime.length()==3){
				startHour= new Integer(startTime.substring(0,1)).intValue(); 
				startMinute = new Integer(startTime.substring(1,3)).intValue();
			}else if(startTime.length()==2){
				startHour=0;
				startMinute=new Integer(startTime).intValue();
			}
			System.out.println("Start Hour "+startHour+" Start Minute "+startMinute);

		}
	}
