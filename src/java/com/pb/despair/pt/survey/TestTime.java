package com.pb.despair.pt.survey;

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
