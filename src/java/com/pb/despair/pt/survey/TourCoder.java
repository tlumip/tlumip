//
// TourCoder.java
// 
// part of the survey package: used to code tours
//
// jef 8/00
package com.pb.despair.pt.survey;

import java.util.*;

public class TourCoder{

	int homeActivitiesRemoved=0,daysNotAtHome=0,workRelatedActivities=0,lessThan2HomeActivities=0;
	int recodedWorkRelated=0,recodedSecondJob=0,workActivitiesRemoved=0;
	//constructor takes hh vector
	public TourCoder(List households){

		System.out.println("Coding Tours for groups of trips");
		for(int i=0;i<households.size();++i){
			Household h = (Household) households.get(i);
			for(int j=0;j<h.getPersons().size();++j){							// get person
				Person p = (Person)h.persons.get(j);
			
				System.out.println("Household "+h.getSampleNumber()+" Person "+p.getPersonNumber());
				
				if(p.getDay1Trips().size()>0){
					System.out.println("Coding "+p.getDay1Trips().size()+" trips");
					p.setDay1Tours((ArrayList)findTour(p.getDay1Trips()));
				}
				if(p.getDay2Trips().size()>0){
					System.out.println("Coding "+p.getDay2Trips().size()+" trips");
					p.setDay2Tours((ArrayList)findTour(p.getDay2Trips()));
				}
			}
		}
		System.out.println("Removed "+homeActivitiesRemoved+" consecutive at-home activities");	
		System.out.println("Removed "+workActivitiesRemoved+" consecutive at-work activities");		
		System.out.println(daysNotAtHome+" person-days did not start at home");
		System.out.println(recodedWorkRelated+" activities were recoded from work to work-related");
		System.out.println(recodedSecondJob+" activities were recoded from work to second job");
		System.out.println(workRelatedActivities+" activities were recoded from work to work-related");
		System.out.println("There were "+lessThan2HomeActivities+" days with less than 2 home activities");
	}

	//method for finding tours in the list of activities, pass a list of activities
	//Note: the activity list should consist of activities with one activity per
	//location; each activity in the list should have a trip associated with it,
	//except for the first activity, which should occur at home
	public List findTour(List activities){
		
		ListIterator a = activities.listIterator();
		ArrayList codedTours = new ArrayList();

		int n=0;
		int workLocation=0;

		System.out.println("Prior to cleanup");
		while(a.hasNext()){
			Activity thisActivity=(Activity)a.next();
			thisActivity.print();
			System.out.print(",");
			((Trip)thisActivity.activityTrip).print();
			System.out.println(","+thisActivity.getAlreadyThere());
		}

		a = activities.listIterator();
		int homeActivity=0;
		//make sure there are at least two at-home activities; if not, return empty tour container
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.getLocation1()==1){		//at home activity
				++homeActivity;
			}
		}
		if(homeActivity<2){
			++lessThan2HomeActivities;
			return codedTours;
		}
		a = activities.listIterator();

		//make sure there aren't any cases where there are two at-home activities in a row
		//if so, delete second activity, reset end time of first activity to end of deleted activity
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.getLocation1()==1){		//at home activity
				if(a.hasNext()){
					Activity nextActivity=(Activity)a.next();
					if(nextActivity.getLocation1()==1){	//another at-home activity!
						thisActivity.setEndHour(nextActivity.getEndHour());
						thisActivity.setEndMinute(nextActivity.getEndMinute());					
						thisActivity.setEndAMPM(nextActivity.getEndAMPM());
						thisActivity.calculateDuration();	
						a.remove();	
						++homeActivitiesRemoved;
						a.previous();
					}
				}
			}
		}	


		//code the work activities correctly
		codeWork(activities);
	
		//make sure there aren't any cases where there are two at-work activities in a row
		//if so, delete second activity, reset end time of first activity to end of deleted activity
		a = activities.listIterator();
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.getGeneralActivity()==1 && thisActivity.getLocation1()!=1){		//at work activity
				if(a.hasNext()){
					Activity nextActivity=(Activity)a.next();
					if(nextActivity.getGeneralActivity()==1 && nextActivity.getLocation1()!=1){	//another at-work activity!
						thisActivity.setEndHour(nextActivity.getEndHour());
						thisActivity.setEndMinute(nextActivity.getEndMinute());					
						thisActivity.setEndAMPM(nextActivity.getEndAMPM());
						thisActivity.calculateDuration();	
						a.remove();	
						++workActivitiesRemoved;
						a.previous();
					}
				}
			}
		}	
	
		//reset list iterator
		a = activities.listIterator();
		//make sure there aren't any cases where the first activity of the day is out-of-home;
		//if so, delete activities until home activity found.  If home is the last activity, 
		//of the day, delete the home activity and return
		if(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.getLocation1()!=1){			//not at home
				++daysNotAtHome;
				a.remove();									//remove it
				while(a.hasNext()){							//begin cycling
					thisActivity = (Activity)a.next();
					if(thisActivity.getLocation1()==1)	//at-home found, so break
						break;
					else									//not at-home, so remove
						a.remove();
				}
				if(!a.hasNext()){		//no more activities
					a.remove();			//remove the single at-home activity
					return codedTours;	//return
				}
			}
		}
		//code the first activity of the day with the correct time if it is screwed up
		a = activities.listIterator();
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			if(thisActivity.getStartHour()==99 || thisActivity.getStartMinute()==99){		//first activity of the day
				thisActivity.setStartHour(3);
				thisActivity.setStartMinute(1);
				thisActivity.setStartAMPM(1);
				if(a.hasNext()){
					Activity nextActivity=(Activity)a.next();
					thisActivity.setEndHour(nextActivity.getStartHour());
					thisActivity.setEndMinute(nextActivity.getStartMinute()-1);
					thisActivity.setEndAMPM(nextActivity.getStartAMPM());
					thisActivity.calculateDuration();	
				}
			}
			break;
		}

		System.out.println("After cleanup");
		a = activities.listIterator();
		while(a.hasNext()){
			Activity thisActivity=(Activity)a.next();
			thisActivity.print();
			System.out.print(",");
			((Trip)thisActivity.activityTrip).print();
			System.out.println(","+thisActivity.getAlreadyThere());
		}

		//first find work-based tours for the first job
		//cycle through the activities for this day,
		ArrayList recodedActivities=new ArrayList();
		a=activities.listIterator();

		while(a.hasNext())
			recodedActivities.add(a.next());
		a = recodedActivities.listIterator();
		int workActivityNumber=0,firstWorkBasedActivity=0,lastWorkBasedActivity=0,localWorkBasedTour=0;
		workLocation=0;
		n=0;
		boolean removeActivities=false;
		while(a.hasNext()){

			Activity thisActivity = (Activity)a.next();
			++n;
			
			//it is a work activity
			if(thisActivity.getGeneralActivity()==1 && thisActivity.getLocation1()!=1){
				++workLocation;
				if(workLocation==1) 
					workActivityNumber=n;
			}
			
			//if back to work, there is a work-based tour
			if(workLocation>1 && thisActivity.getGeneralActivity()==1 && thisActivity.getLocation1()!=1){
				//cycle through activities from the work activity to the next work activity
				//adding the activities to a list of workActivities, and deleting them from
				//the list of homeTourActivities
				ArrayList workTourActivities=new ArrayList();
				System.out.println("Adding work-based activities");
				for(int i=(workActivityNumber-1);i<n;++i){
					Activity workTourActivity = (Activity) recodedActivities.get(i);
					workTourActivity.print();
					System.out.print(",");
					((Trip)workTourActivity.activityTrip).print();
					System.out.println(","+workTourActivity.getAlreadyThere());
					workTourActivities.add(workTourActivity);
					++localWorkBasedTour;
					if(localWorkBasedTour==1)
						firstWorkBasedActivity=workActivityNumber;
					lastWorkBasedActivity=n;
				}
				removeActivities=true;
				codedTours.add(codeWorkBasedTour(workTourActivities));
				workTourActivities.clear();
				workLocation=0;
				workActivityNumber=0;
			}
		}					
		//we only want one work activity for the home tour;remove all activities except for
		//first activity, which is at work
		ListIterator r = activities.listIterator();
		int rCount=0;
		Activity removeActivity=new Activity();
		while(r.hasNext() && removeActivities){
			removeActivity = (Activity)r.next();
			++rCount;
			if(rCount>firstWorkBasedActivity && rCount<=lastWorkBasedActivity){
				System.out.println("removing "+rCount);
				removeActivity.print();
				System.out.print(",");
				((Trip)removeActivity.activityTrip).print();
				System.out.println(","+removeActivity.getAlreadyThere());
				r.remove();
				if(rCount==lastWorkBasedActivity)
					break;
			}
		}
 		//now that work tour activities were removed, set the first work
		//activity end time to the last work activity end time
		if(removeActivities){
			Activity firstWorkActivity = (Activity)activities.remove(firstWorkBasedActivity-1);
			firstWorkActivity.setEndHour(removeActivity.getEndHour());
			firstWorkActivity.setEndMinute(removeActivity.getEndMinute());
			firstWorkActivity.setEndAMPM(removeActivity.getEndAMPM());
			activities.add((firstWorkBasedActivity-1),firstWorkActivity);
		}

		//next find work-based tours for the second job
		//cycle through the activities for this day,
		recodedActivities.clear();
		a=activities.listIterator();
		while(a.hasNext())
			recodedActivities.add(a.next());
		a = recodedActivities.listIterator();
		workActivityNumber=0;firstWorkBasedActivity=0;lastWorkBasedActivity=0;localWorkBasedTour=0;
		workLocation=0;
		n=0;
		removeActivities=false;
		while(a.hasNext()){

			Activity thisActivity = (Activity)a.next();
			++n;
			
			//it is a work activity
			if(thisActivity.getGeneralActivity()==9){
				++workLocation;
				if(workLocation==1)
					workActivityNumber=n;
			}
			
			//if back to work, there is a work-based tour
			if(workLocation>1 && thisActivity.getGeneralActivity()==9){
				//cycle through activities from the work activity to the next work activity
				//adding the activities to a list of workActivities, and deleting them from
				//the list of homeTourActivities
				ArrayList workTourActivities=new ArrayList();
				System.out.println("Adding work-based activities");
				for(int i=(workActivityNumber-1);i<n;++i){
					Activity workTourActivity = (Activity) recodedActivities.get(i);
					workTourActivity.print();
					System.out.print(",");
					((Trip)workTourActivity.activityTrip).print();
					System.out.println(","+workTourActivity.getAlreadyThere());
					workTourActivities.add(workTourActivity);
					++localWorkBasedTour;
					if(localWorkBasedTour==1)
						firstWorkBasedActivity=workActivityNumber;
					lastWorkBasedActivity=n;
					workLocation=0;
					workActivityNumber=0;
				}
				removeActivities=true;
				codedTours.add(codeWorkBasedTour(workTourActivities));
				workTourActivities.clear();
				workLocation=0;
				workActivityNumber=0;
			}

		}					
		//we only want one work activity for the home tour;remove all activities except for
		//first activity, which is at work
		r = activities.listIterator();
		rCount=0;
		while(r.hasNext() && removeActivities){
			++rCount;
			removeActivity = (Activity)r.next();
			if(rCount>firstWorkBasedActivity && rCount<=lastWorkBasedActivity){
				System.out.println("removing "+rCount);
				removeActivity.print();
				System.out.print(",");
				((Trip)removeActivity.activityTrip).print();
				System.out.println(","+removeActivity.getAlreadyThere());
				r.remove();
				if(rCount==lastWorkBasedActivity)
					break;
			}
		}
 		//now that work tour activities were removed, set the first work
		//activity end time to the last work activity end time
		if(removeActivities){
			Activity firstWorkActivity = (Activity)activities.remove(firstWorkBasedActivity-1);
			firstWorkActivity.setEndHour(removeActivity.getEndHour());
			firstWorkActivity.setEndMinute(removeActivity.getEndMinute());
			firstWorkActivity.setEndAMPM(removeActivity.getEndAMPM());
			activities.add((firstWorkBasedActivity-1),firstWorkActivity);
		}
		//next find home-based tours
		//cycle through the activities for this day,
		recodedActivities=(ArrayList)activities;
		a = recodedActivities.listIterator();
		ArrayList homeTourActivities = new ArrayList();
		int homeLocation=0;
		while(a.hasNext()){

			Activity thisActivity = (Activity)a.next();
			++n;
			
			//add the activities to the homeTourActivities list
			homeTourActivities.add(thisActivity);
			//at home
			if(thisActivity.getLocation1()==1)
				++homeLocation;

			//if back to home, there is a home-based tour
			if(homeLocation==2 && thisActivity.getLocation1()==1){
				codedTours.add(codeHomeBasedTour(homeTourActivities));
				homeTourActivities.clear();
				homeLocation=0;
				a.previous();
			}
		}					

		return codedTours;
	}

	//use this method to code Home-Based tours.  The list of activities passed should start and end with a home
	//activity.  There should not be work-based tours in this list.
	public Tour codeHomeBasedTour(List activities){
		System.out.println("Coding Home-based Tour");

		//if less than three activities, its not a tour!
		if(activities.size()<3){
			System.out.println("Error: Less than 3 activities on tour!");
			System.exit(1);
		}

		//if first activity not at home, its not a home-based tour!
		if(((Activity)activities.get(0)).getLocation1()!=1){
			System.out.println("Error: Activity 1 not at home!");
			System.exit(1);
		}

		//if last activity not at home, its not a home-based tour!
		if(((Activity)activities.get(activities.size()-1)).getLocation1()!=1){
			System.out.println("Error: Last activity not at home!");
			System.exit(1);
		}

		boolean workTour=false;
		boolean schoolTour=false;
		boolean otherTour=false;
		boolean majorShopTour=false;
		boolean otherShopTour=false;
		boolean recreationTour=false;
		boolean pickupDropoffTour=false;
		boolean workBasedTour=false;

		Tour thisTour=new Tour();
		//tour leave origin activity is first activity, arrive origin is last activity
		thisTour.setLeaveOrigin(activities.get(0));
		thisTour.setArriveOrigin(activities.get(activities.size()-1));
		//tour number of stops is number of activities - leaveOrigin - destination - arriveOrigin
		thisTour.setOriginalStops(activities.size()-3); 
	
		ListIterator a = activities.listIterator();
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			thisActivity.print();
			System.out.print(",");
			((Trip)thisActivity.activityTrip).print();
			System.out.println(","+thisActivity.getAlreadyThere());
		}
		a = activities.listIterator();
		int n=0;
		//what kind of tour is it?
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			++n;	

			//skip at-home activities
			if(thisActivity.getLocation1()==1)
				continue;
			if(thisActivity.getGeneralActivity()==1||thisActivity.getGeneralActivity()==9)	//worktour
				workTour=true;
			
			if(thisActivity.getGeneralActivity()==2)
				schoolTour=true;				//schooltour
			
			if(thisActivity.getGeneralActivity()==3)
				majorShopTour=true;				//major shop tour

			if(thisActivity.getGeneralActivity()==4)	
				otherShopTour=true;				//other shop tour

			if(thisActivity.getGeneralActivity()==5)
				recreationTour=true;			//recreation tour

			if(thisActivity.getGeneralActivity()==6)
				otherTour=true;					//other tour

			if(thisActivity.getGeneralActivity()==7)
				pickupDropoffTour=true;			//pickup/dropoff
		}		
		//if no tour found, exit with error message
		if(workTour!=true && schoolTour!=true && majorShopTour!=true && otherTour!=true
				&& pickupDropoffTour !=true && otherShopTour != true && recreationTour!=true){
			System.out.println("Error: no tour purpose found!");
			System.exit(1);
		}
		//tour heirarchy
		if(workTour){
			System.out.println("Work Tour");
			schoolTour=false;	
			majorShopTour=false;
			otherShopTour=false;
			recreationTour=false;
			otherTour=false;
			pickupDropoffTour=false;
			thisTour.type='w';
		}
		if(schoolTour){
			System.out.println("School Tour");
			workTour=false;	
			majorShopTour=false;
			otherShopTour=false;
			recreationTour=false;
			otherTour=false;
			pickupDropoffTour=false;
			thisTour.type='c';
		}
		if(majorShopTour){
			System.out.println("MajorShop Tour");
			schoolTour=false;	
			workTour=false;
			otherShopTour=false;
			recreationTour=false;
			otherTour=false;
			pickupDropoffTour=false;
			thisTour.type='m';
		}
		if(otherShopTour){
			System.out.println("OtherShop Tour");
			schoolTour=false;	
			workTour=false;
			majorShopTour=false;
			recreationTour=false;
			otherTour=false;
			pickupDropoffTour=false;
			thisTour.type='s';
		}
		if(recreationTour){
			System.out.println("Recreation Tour");
			schoolTour=false;	
			workTour=false;
			otherShopTour=false;
			majorShopTour=false;
			otherTour=false;
			pickupDropoffTour=false;
			thisTour.type='r';
		}
		if(otherTour){
			System.out.println("Other Tour");
			schoolTour=false;	
			majorShopTour=false;
			otherShopTour=false;
			recreationTour=false;
			workTour=false;
			pickupDropoffTour=false;
			thisTour.type='o';
		}
		if(pickupDropoffTour){
			System.out.println("Pickup-Dropoff Tour");
			schoolTour=false;	
			majorShopTour=false;
			otherShopTour=false;
			recreationTour=false;
			otherTour=false;
			workTour=false;
			thisTour.type='o';
		}

		//find primary destination
		int primaryDestinationNumber=0;	//track where is the primary destination in the activity list 
		a=activities.listIterator();	//reset the list iterator back to the first activity

		//if it is a work tour, it is the work location
		if(workTour){
			
			while(a.hasNext()){
				Activity thisActivity = (Activity)a.next();
				++primaryDestinationNumber;
				
				//skip at home activities
				if(thisActivity.getLocation1()==1)
					continue;
				//check if primary destination
				if(thisActivity.getGeneralActivity()==1||thisActivity.getGeneralActivity()==9){
					thisTour.setDestination(thisActivity);
					break;
				}
			}
		}
		
		//if it is a school tour, it is the school location
		if(schoolTour){
			while(a.hasNext()){
				Activity thisActivity = (Activity)a.next();
				++primaryDestinationNumber;
				//skip at home activities
				if(thisActivity.getLocation1()==1)
					continue;
				//check if primary destination
				if(thisActivity.getGeneralActivity()==2){
					thisTour.setDestination(thisActivity);
					break;
				}
			}
		}
		//if it is a pickupDropoff tour, it is the pickup\dropoff location
		if(pickupDropoffTour){
			while(a.hasNext()){
				Activity thisActivity = (Activity)a.next();
				++primaryDestinationNumber;
				//skip at home activities
				if(thisActivity.getLocation1()==1)
					continue;
				//check if primary destination
				if(thisActivity.getGeneralActivity()==7){
					thisTour.setDestination(thisActivity);
					break;
				}
			}
		}

		//if it is a majorShop tour, it is the majorShop location with the longest duration
		if(majorShopTour){
			int maxDuration=-1,currentActivityNumber=0;
			while(a.hasNext()){
				Activity thisActivity = (Activity)a.next();
				++currentActivityNumber;
				//skip at home activities
				if(thisActivity.getLocation1()==1)
					continue;
				//check if primary destination
				if(thisActivity.getGeneralActivity()==3 && thisActivity.getDuration()>maxDuration){
					thisTour.setDestination(thisActivity);
					maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
					primaryDestinationNumber=currentActivityNumber;
				}
			}
		}
		//if it is a otherShop tour, it is the otherShop location with the longest duration
		if(otherShopTour){
			int maxDuration=-1,currentActivityNumber=0;
			while(a.hasNext()){
				Activity thisActivity = (Activity)a.next();
				++currentActivityNumber;
				//skip at home activities
				if(thisActivity.getLocation1()==1)
					continue;
				//check if primary destination
				if(thisActivity.getGeneralActivity()==4 && thisActivity.getDuration()>maxDuration){
					thisTour.setDestination(thisActivity);
					maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
					primaryDestinationNumber=currentActivityNumber;
				}
			}
		}
		//if it is a recreation tour, it is the recreation location with the longest duration
		if(recreationTour){
			int maxDuration=-1,currentActivityNumber=0;
			while(a.hasNext()){
				Activity thisActivity = (Activity)a.next();
				++currentActivityNumber;
				//skip at home activities
				if(thisActivity.getLocation1()==1)
					continue;
				//check if primary destination
				if(thisActivity.getGeneralActivity()==5 && thisActivity.getDuration()>maxDuration){
					thisTour.setDestination(thisActivity);
					maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
					primaryDestinationNumber=currentActivityNumber;
				}
			}
		}
		//if it is an other tour, it is the location with the longest duration
		//if the activities do not have durations, then it is the last activity before home
		if(otherTour){
			int maxDuration=-1;
			int currentActivityNumber=0;
			while(a.hasNext()){
				Activity thisActivity = (Activity)a.next();
				++currentActivityNumber;
				//skip at home activities
				if(thisActivity.getLocation1()==1)
					continue;
				//check if primary destination
				if(thisActivity.getLocation1()!=1){
					if(thisActivity.getDuration()>maxDuration){
						thisTour.setDestination(thisActivity);
						maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
						primaryDestinationNumber=currentActivityNumber;
					}
				}
			}
		}
		//find intermediate stop on journey to primary destination
		int maxDuration=-1;
		a=activities.listIterator();	//reset the list iterator back to first activity
		a.next();						//goto second activity (past the home)
		//start with second activity, cycle up to the number of the primary destination,
		//set the intermediateStop1 to the activity with the longest duration
		for(int i=1;i<(primaryDestinationNumber-1);++i){
			thisTour.setHasIntermediateStop1(true);
			Activity thisActivity = (Activity)a.next();
			
			if(thisActivity.getDuration()>maxDuration)
				thisTour.setIntermediateStop1(thisActivity);

			maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
		}	

		//find intermediate stop on journey back to origin
		maxDuration=-1;
        a.next();						//skip primary destination
		//start with first activity after the destination, cycle up to the activity before the home activity,
		//set the intermediateStop2 to the activity with the longest duration
		for(int i=primaryDestinationNumber;i<(activities.size()-1);++i){
			Activity thisActivity = (Activity)a.next();
			thisTour.setHasIntermediateStop2(true);	
			if(thisActivity.getDuration()>maxDuration)
				thisTour.setIntermediateStop2(thisActivity);

			maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
		}
		System.out.println("Coded Home-Based Tour");
		thisTour.print();		
		return thisTour;
	}

	//use for coding work-based tour
	public Tour codeWorkBasedTour(List activities){
		Tour thisTour = new Tour();
		ListIterator a = activities.listIterator();
		System.out.println("Coding Work-based tour");
		
		thisTour.type='b';
		
		//if less than three activities, its not a tour!
		if(activities.size()<3){
			System.out.println("Error: Less than 3 activities on tour!");
			System.exit(1);
		}

		//if first activity not at work, its not a work-based tour!
		if(((Activity)activities.get(0)).getGeneralActivity()!=1 && 
			((Activity)activities.get(0)).getGeneralActivity()!=9 ){
			System.out.println("Error: Activity 1 not at work!");
			System.exit(1);
		}

		//if last activity not at work, its not a work-based tour!
		if(((Activity)activities.get(activities.size()-1)).getGeneralActivity()!=1 && 
			((Activity)activities.get(activities.size()-1)).getGeneralActivity()!=9){
			System.out.println("Error: Last activity not at work!");
			System.exit(1);
		}

		//tour leave origin activity is first activity, arrive origin is last activity
		thisTour.setLeaveOrigin(activities.get(0));
		thisTour.setArriveOrigin(activities.get(activities.size()-1));
		//tour number of stops is number of activities - leaveOrigin - destination - arriveOrigin
		thisTour.setOriginalStops(activities.size()-3); 

		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			thisActivity.print();
			System.out.print(",");
			((Trip)thisActivity.activityTrip).print();
			System.out.println(","+thisActivity.getAlreadyThere());
		}
		//Work-based tours are alot like other tours; the primary destination is the location with the longest duration
		//if the activities do not have durations, then it is the last activity before work
		a=activities.listIterator();
		int maxDuration=-1;
		int currentActivityNumber=0,primaryDestinationNumber=0;
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			++currentActivityNumber;
			if(currentActivityNumber==1 || currentActivityNumber==activities.size())
				continue;
			if(thisActivity.getDuration()>maxDuration){
				thisActivity.setGeneralActivity(8);
				thisTour.setDestination(thisActivity);
				maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
				primaryDestinationNumber=currentActivityNumber;
			}
		}

		//find intermediate stop on journey to primary destination
		maxDuration=-1;
		a=activities.listIterator();	//reset the list iterator back to first activity
		a.next();						//goto second activity (past the home)
		//start with second activity, cycle up to the number of the primary destination,
		//set the intermediateStop1 to the activity with the longest duration
		for(int i=1;i<(primaryDestinationNumber-1);++i){
			Activity thisActivity = (Activity)a.next();
			thisTour.setHasIntermediateStop1(true);			
			if(thisActivity.getDuration()>maxDuration)
				thisTour.setIntermediateStop1(thisActivity);

			maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
		}	

		//find intermediate stop on journey back to origin
		maxDuration=-1;
        a.next();						//skip primary destination
		//start with first activity after the destination, cycle up to the activity before the home activity,
		//set the intermediateStop2 to the activity with the longest duration
		for(int i=primaryDestinationNumber;i<(activities.size()-1);++i){
			Activity thisActivity = (Activity)a.next();
			thisTour.setHasIntermediateStop2(true);
			if(thisActivity.getDuration()>maxDuration)
				thisTour.setIntermediateStop2(thisActivity);

			maxDuration=Math.max(thisActivity.getDuration(),maxDuration);
		}
		System.out.println("Coded Work-Based Tour");
		thisTour.print();		
		return thisTour;

	}
	
	//codeWork(): Use this method to code correct workplace information
	//This will correct problems with tours with more than one workplace
	public void codeWork(List activities){

		int n=0;
		double workX=0,workY=0;
		ListIterator a = activities.listIterator();
		boolean workLocationFound=false;


		//home->worklocation1->worklocation2->home : the first
		//work location will be coded as work (location1==2, activity==12), the others will be coded as 
		//work-related (location1==4, generalActivity==4).  
		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
			++n;
		
			//first activity at work
			if(workLocationFound==false && thisActivity.getGeneralActivity()==1 && thisActivity.getLocation1()!=1){	
				workLocationFound=true;
				workX=thisActivity.getLocation().getXCoordinate();
				workY=thisActivity.getLocation().getYCoordinate();
			}	
			//If they return home, don't search anymore
			if(workLocationFound && thisActivity.getLocation1()==1)
				workLocationFound=false;
		
			//not first activity at work
			if(workLocationFound && thisActivity.getGeneralActivity()==1 && thisActivity.getLocation1()!=1){
				//same location as first activity?
				if(thisActivity.getLocation().getXCoordinate()!=workX || 
					thisActivity.getLocation().getYCoordinate()!=workY ||
					thisActivity.getLocation().getXCoordinate()==99.0||
					thisActivity.getLocation().getYCoordinate()==99.0){
					//not same location, so recode generalActivity as other
					thisActivity.setLocation1(4);
					thisActivity.setLocation2(4);
					thisActivity.setGeneralActivity(6);
					++recodedWorkRelated;
				}
			}
		}

		//the second condition to check for is cases where someone works two jobs; in this case,
		//we want to distinguish between persons who go home between two different jobs and those
		//who go home between one job and the next.  We need to create a new code for the second
		//job; generalActivity=9, location1=9 (second job)
		a = activities.listIterator();
		workLocationFound=false;
		boolean startSearching=false;

		while(a.hasNext()){
			Activity thisActivity = (Activity)a.next();
		
			//first activity at work
			if(workLocationFound==false && thisActivity.getGeneralActivity()==1 
				&& thisActivity.getLocation1()!=1){
				workLocationFound=true;
				workX=thisActivity.getLocation().getXCoordinate();
				workY=thisActivity.getLocation().getYCoordinate();
			}	

			//if they return home, start searching for next work activity
			if(workLocationFound && thisActivity.getLocation1()==1)
				startSearching=true;

			//they went to work, got home, and went to work again
			if(startSearching && thisActivity.getGeneralActivity()==1 && thisActivity.getLocation1()!=1){
				//but not at the first work location
				if(thisActivity.getLocation().getXCoordinate()!=workX || 
					thisActivity.getLocation().getYCoordinate()!=workY){
					thisActivity.setLocation1(9);
					thisActivity.setGeneralActivity(9);
					++recodedSecondJob;
				}
			}
		}
	}
	public static void main(String[] args) {}

}