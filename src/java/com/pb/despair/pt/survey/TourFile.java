// PersonFile.java
//
//
// This class writes a file with tour records 
// 
package com.pb.tlumip.pt.survey;
import java.util.*;
import java.io.*;

import com.pb.common.util.OutTextFile;

public class TourFile{

	/** constructor takes a list of households and a filename to write to
	* tours should be coded on household list.
	* format of output file includes:
	* Household Data
	* Person Data
	* Person Day
	* Tour Number
    * Tour Start Hour
	* Tour Start Minute
	* Tour End Hour
	* Tour End Minute
	* Tour Duration Hours
	* Tour Duration Minutes
	* Tour Primary Activity 
	* Has Intermediate Stop 1 (0,1)
	* Has Intermediate Stop 2 (0,1)
	* IStop 1 Mode (0-8,99)
	* IStop 2 Mode (0-8,99)
	* Primary Destination Mode (0-8,99)
	* Return Origin Mode (0-8,99)
	* IStop 1 Detailed Mode (0-8,99)
	* IStop 2 Detailed Mode (0-8,99)
	* Primary Destination Detailed Mode (0-8,99)
	* Return Origin Detailed Mode (0-8,99)
	* Tour Origin X
	* Tour Origin Y
	* Tour Origin TAZ
	* Tour Destination X
	* Tour Destination Y
	* Tour Destination TAZ
	* Tour IStop1 X
	* Tour IStop1 Y
	* Tour IStop1 TAZ
	* Tour IStop2 X
	* Tour IStop2 Y
	* Tour IStop2 TAZ
	* IStop 1 Travel Time (0-2799)
	* IStop 2 Travel Time (0-2799)
	* Primary Destination Travel Time (0-2799)
	* Primary Origin Travel Time (0-2799) 
	* Tour IStop1 Duration
	* Tour IStop2 Duration
	* Tour Destination Duration

*/

	public TourFile(List households, String tourFileName) throws IOException{
		//open report file
		OutTextFile tourFile = new OutTextFile();
		PrintWriter tFile = tourFile.open(tourFileName);

		ListIterator h = households.listIterator();
		while(h.hasNext()){
			Household thisHousehold = (Household)h.next();
			ListIterator p = thisHousehold.persons.listIterator();
			while(p.hasNext()){
				Person thisPerson = (Person)p.next();
				//write tours 
				ListIterator t = thisPerson.day1Tours.listIterator();
				int n=0;
				while(t.hasNext()){
					Tour thisTour = (Tour)t.next();
					thisTour.calculateTravelTimes();
					++n;
					thisHousehold.print(tFile);
					thisPerson.print(tFile);
					tFile.print(1+" ");
					tFile.print(n+" ");
					thisTour.print(tFile);
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop1()).activityTrip.mode+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop2()).activityTrip.mode+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+((Activity)thisTour.getDestination()).activityTrip.mode+" ");
					tFile.print(" "+((Activity)thisTour.getArriveOrigin()).activityTrip.mode+" ");
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop1()).activityTrip.getDetailedMode()+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop2()).activityTrip.getDetailedMode()+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+((Activity)thisTour.getDestination()).activityTrip.getDetailedMode()+" ");
					tFile.print(" "+((Activity)thisTour.getArriveOrigin()).activityTrip.getDetailedMode()+" ");
					((Activity)thisTour.getArriveOrigin()).getLocation().print(tFile);
					((Activity)thisTour.getDestination()).getLocation().print(tFile);
					//print locations
					if(thisTour.getHasIntermediateStop1())
						((Activity)thisTour.getIntermediateStop1()).getLocation().print(tFile);
					else
						tFile.print(" "+0+" "+0+" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						((Activity)thisTour.getIntermediateStop2()).getLocation().print(tFile);
					else
						tFile.print(" "+0+" "+0+" "+0+" ");
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+thisTour.iStop1TravelTime+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+thisTour.iStop2TravelTime+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+thisTour.primaryDestinationTravelTime+" ");
					tFile.print(" "+thisTour.arriveOriginTravelTime+" ");
					//print durations
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop1()).getDuration()+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop2()).getDuration()+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+((Activity)thisTour.getDestination()).getDuration()+" ");


					tFile.println();
				}

				//day2stats
				t = thisPerson.day2Tours.listIterator();
				n=0;
				while(t.hasNext()){
					Tour thisTour = (Tour)t.next();
					thisTour.calculateTravelTimes();
					++n;
					thisHousehold.print(tFile);
					thisPerson.print(tFile);
					tFile.print(2+" ");
					tFile.print(n+" ");
					thisTour.print(tFile);
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop1()).activityTrip.mode+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop2()).activityTrip.mode+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+((Activity)thisTour.getDestination()).activityTrip.mode+" ");
					tFile.print(" "+((Activity)thisTour.getArriveOrigin()).activityTrip.mode+" ");
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop1()).activityTrip.getDetailedMode()+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop2()).activityTrip.getDetailedMode()+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+((Activity)thisTour.getDestination()).activityTrip.getDetailedMode()+" ");
					tFile.print(" "+((Activity)thisTour.getArriveOrigin()).activityTrip.getDetailedMode()+" ");
					((Activity)thisTour.getArriveOrigin()).getLocation().print(tFile);
					((Activity)thisTour.getDestination()).getLocation().print(tFile);
					if(thisTour.getHasIntermediateStop1())
						((Activity)thisTour.getIntermediateStop1()).getLocation().print(tFile);
					else
						tFile.print(" "+0+" "+0+" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						((Activity)thisTour.getIntermediateStop2()).getLocation().print(tFile);
					else
						tFile.print(" "+0+" "+0+" "+0+" ");
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+thisTour.iStop1TravelTime+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+thisTour.iStop2TravelTime+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+thisTour.primaryDestinationTravelTime+" ");
					tFile.print(" "+thisTour.arriveOriginTravelTime+" ");
					//print durations
					if(thisTour.getHasIntermediateStop1())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop1()).getDuration()+" ");
					else
						tFile.print(" "+0+" ");
					if(thisTour.getHasIntermediateStop2())
						tFile.print(" "+((Activity)thisTour.getIntermediateStop2()).getDuration()+" ");
					else
						tFile.print(" "+0+" ");
					tFile.print(" "+((Activity)thisTour.getDestination()).getDuration()+" ");
					tFile.println();
				}
			}
		}
	
		tFile.close();
	}
}
