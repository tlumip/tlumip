// PatternGenerationFile.java
//      jf 10/9/2000


package com.pb.tlumip.pt.survey;
import java.util.*;
import java.io.*;
import java.lang.Math;

import com.pb.common.util.OutTextFile;

/**  This class creates an estimation
     file for the generation of day-patterns.
*/
public class PatternGenerationFile{

	//number of alternatives
	int numberAlternatives=40;
	int totalWeekdayPatterns=0;
	int totalWeekendPatterns=0;
	public ArrayList weekdayPatterns=new ArrayList();
	public ArrayList weekendPatterns=new ArrayList();

	public PatternGenerationFile(List households, String weekdayPatternFileName, String weekendPatternFileName) 
		throws IOException{

		//open output estimation files
		OutTextFile weekdayPatternFile = new OutTextFile();
		PrintWriter wkdayFile = weekdayPatternFile.open(weekdayPatternFileName);
		OutTextFile weekendPatternFile = new OutTextFile();
		PrintWriter wkendFile = weekendPatternFile.open(weekendPatternFileName);
		addWords(households);
		choosePattern(households,wkdayFile,wkendFile);
		
		wkdayFile.close();
		wkendFile.close();

		


	}
	public void addWords(List households){
		// iterate through household vector, add patterns to pattern vectors
		System.out.println("Adding words to arraylists\n");
		ListIterator h = households.listIterator();
		while(h.hasNext()){
			Household thisHousehold = (Household)h.next();
			ListIterator p = thisHousehold.persons.listIterator();
			while(p.hasNext()){
				
				Person thisPerson = (Person)p.next();
		
				Pattern dayPattern = new Pattern(thisPerson.getWordWithStops(1));
				//add the day1 pattern to the correct ArrayList
				if(thisHousehold.getDay1()<=5){
		
					//if this pattern already exists in arraylist, increment up observed by 1,else add;
					boolean foundit=false;
					int index=0;
					for(index=0;index<weekdayPatterns.size();++index){
						foundit=(dayPattern.equals(weekdayPatterns.get(index)));
						if(foundit)
							break;
					}
					if(foundit==false){
						weekdayPatterns.add(dayPattern);
					}else{
						System.out.println("found pattern\n");
						Pattern thisPattern = (Pattern) weekdayPatterns.get(index);
						++thisPattern.observed;
						weekdayPatterns.set(index,thisPattern);
					}
					++totalWeekdayPatterns;
				}else{
					//if this pattern already exists in arraylist, increment up observed by 1,else add;
					boolean foundit=false;
					int index=0;
					for(index=0;index<weekendPatterns.size();++index){
						foundit=(dayPattern.equals(weekendPatterns.get(index)));
						if(foundit)
							break;
					}
					if(foundit==false){
						weekendPatterns.add(dayPattern);
					}else{
						System.out.println("found pattern\n");
						Pattern thisPattern = (Pattern) weekendPatterns.get(index);
						++thisPattern.observed;
						weekendPatterns.set(index,thisPattern);
					}
					++totalWeekendPatterns;
				}

				dayPattern = new Pattern(thisPerson.getWordWithStops(2));
				//add the day2 pattern to the correct ArrayList
				if(thisHousehold.getDay2()<=5){
				
					//if this pattern already exists in arraylist, increment up observed by 1,else add;
					boolean foundit=false;
					int index=0;
					for(index=0;index<weekdayPatterns.size();++index){
						foundit=(dayPattern.equals(weekdayPatterns.get(index)));
						if(foundit)
							break;
					}
					if(foundit==false){
						weekdayPatterns.add(dayPattern);
					}else{
						System.out.println("found pattern\n");
						Pattern thisPattern = (Pattern) weekdayPatterns.get(index);
						++thisPattern.observed;
						weekdayPatterns.set(index,thisPattern);
					}
					++totalWeekdayPatterns;
				}else{
		
					//if this pattern already exists in arraylist, increment up observed by 1,else add;
					boolean foundit=false;
					int index=0;
					for(index=0;index<weekendPatterns.size();++index){
						foundit=(dayPattern.equals(weekendPatterns.get(index)));
						if(foundit)
							break;
					}
					if(foundit==false){
						weekendPatterns.add(dayPattern);
					}else{
						System.out.println("found pattern\n");
						Pattern thisPattern = (Pattern) weekendPatterns.get(index);
						++thisPattern.observed;
						weekendPatterns.set(index,thisPattern);
					}
					++totalWeekendPatterns;
				}
			}
		}
	
		System.out.println("Total weekday patterns  = "+totalWeekdayPatterns);
		System.out.println("Unique weekday patterns = "+weekdayPatterns.size());
		System.out.println("Total weekend patterns  = "+totalWeekendPatterns);
		System.out.println("Unique weekend patterns = "+weekendPatterns.size());
	}
	public void choosePattern(List households,PrintWriter wkdayFile, PrintWriter wkendFile){
		//now iterate through household vector, choose 39 patterns at random
		//for a total of 40 alternatives
		System.out.println("Constructing day-pattern estimation files\n");
		
		//create a day-pattern with only home activity for comparison
		Pattern homeAllDay=new Pattern("h");
		
		ListIterator h = households.listIterator();
		while(h.hasNext()){
			Household thisHousehold = (Household)h.next();
			ListIterator p = thisHousehold.persons.listIterator();
			while(p.hasNext()){
				
				Person thisPerson = (Person)p.next();
				Pattern dayPattern = new Pattern(thisPerson.getWordWithStops(1));
				ArrayList samplePatterns = new ArrayList(numberAlternatives);
				
				int n=0;
				//if the dayPattern is home (base pattern choice), set the choice to 40
				//else
				//generate the random number n used for this persons chosen pattern;
				//will range from 0 to (numberAlternatives-1)
				if(dayPattern.equals(homeAllDay))
					n=39;
				else
					n= new Double(Math.floor(Math.random() * numberAlternatives)).intValue();

				//enter loop on total alternatives
				for(int i=0;i<numberAlternatives;++i){
	
					//if i==the random number n, the pattern is the chosen pattern
					if(i==n){
						samplePatterns.add(dayPattern);
					}else if(thisHousehold.getDay1()<=5){
						while(true){
							//random #s ranges 0 thru number of weekday patterns
							int s = new Double(Math.floor(Math.random() * weekdayPatterns.size())).intValue();
							Pattern randomPattern = (Pattern) weekdayPatterns.get(s);
							
							//don't use home pattern as alternative
							if(randomPattern.equals(homeAllDay)){
								System.out.println("Skipped at-home pattern");
								continue;
							}
							if(samplePatterns.contains(randomPattern)==false){
								samplePatterns.add(randomPattern);
								break;
							}
						}
					}else{
						while(true){
							//random #s ranges 0 thru number of weekday patterns
							int s = new Double(Math.floor(Math.random() * weekendPatterns.size())).intValue();
							Pattern randomPattern = (Pattern) weekendPatterns.get(s);
							
							//don't use home pattern as alternative
							if(randomPattern.equals(homeAllDay)){
								System.out.println("Skipped at-home pattern");
								continue;
							}

							if(samplePatterns.contains(randomPattern)==false){
								samplePatterns.add(randomPattern);
								break;
							}
						}
					} //end if
				}  //end loop on number of alternatives

				//print day1 household data,person data, alternatives
				if(thisHousehold.getDay1()<=5){
					thisHousehold.print(wkdayFile);
					thisPerson.print(wkdayFile);
					wkdayFile.print(1+" ");			//dayno
 					wkdayFile.print((n+1)+" ");			//chosen alternative #
					for(int i=0;i<numberAlternatives;++i){
						Pattern thisPattern=(Pattern) samplePatterns.get(i);
						thisPattern.print(wkdayFile);
						double probability = ((double)thisPattern.observed)/((double)weekdayPatterns.size());
						wkdayFile.print((1/probability)+" ");
					}
					wkdayFile.print("\n");
				}else{
					thisHousehold.print(wkendFile);
					thisPerson.print(wkendFile);
					wkendFile.print(1+" ");			//dayno
 					wkendFile.print((n+1)+" ");			//chosen alternative #
					for(int i=0;i<numberAlternatives;++i){
						Pattern thisPattern=(Pattern) samplePatterns.get(i);
						thisPattern.print(wkendFile);
						double probability = ((double)thisPattern.observed)/((double)weekendPatterns.size());
						wkendFile.print((1/probability)+" ");
					}
					wkendFile.print("\n");
				}

				dayPattern = new Pattern(thisPerson.getWordWithStops(2));
				samplePatterns = new ArrayList(numberAlternatives);

				//if the dayPattern is home (base pattern choice), set the choice to 40
				//else
				//generate the random number n used for this persons chosen pattern;
				//will range from 0 to (numberAlternatives-1)
				if(dayPattern.equals(homeAllDay))
					n=39;
				else
					n= new Double(Math.floor(Math.random() * numberAlternatives)).intValue();

				//enter loop on total alternatives
				for(int i=0;i<numberAlternatives;++i){
	
					//if i==the random number n, the pattern is the chosen pattern
					if(i==n){
						samplePatterns.add(dayPattern);
					}else if(thisHousehold.getDay2()<=5){
						while(true){
							//random #s ranges 0 thru number of weekday patterns
							int s = new Double(Math.floor(Math.random() * weekdayPatterns.size())).intValue();
							Pattern randomPattern = (Pattern) weekdayPatterns.get(s);

							//don't use home pattern as alternative
							if(randomPattern.equals(homeAllDay)){
								System.out.println("Skipped at-home pattern");
								continue;
							}
							if(samplePatterns.contains(randomPattern)==false){
								samplePatterns.add(randomPattern);
								break;
							}
						}
					}else{
						while(true){
							//random #s ranges 0 thru number of weekday patterns
							int s = new Double(Math.floor(Math.random() * weekendPatterns.size())).intValue();
							Pattern randomPattern = (Pattern) weekendPatterns.get(s);

							//don't use home pattern as alternative
							if(randomPattern.equals(homeAllDay)){
								System.out.println("Skipped at-home pattern");
								continue;
							}
							if(samplePatterns.contains(randomPattern)==false){
								samplePatterns.add(randomPattern);
								break;
							}
						}
					} //end if
				}  //end loop on number of alternatives

				//print day2 household data,person data, alternatives
				if(thisHousehold.getDay2()<=5){
					thisHousehold.print(wkdayFile);
					thisPerson.print(wkdayFile);
					wkdayFile.print(2+" ");			//dayno
 					wkdayFile.print((n+1)+" ");			//chosen alternative #
					for(int i=0;i<numberAlternatives;++i){
						Pattern thisPattern=(Pattern) samplePatterns.get(i);
						thisPattern.print(wkdayFile);
						double probability = ((double)thisPattern.observed)/((double)weekdayPatterns.size());
						wkdayFile.print((1/probability)+" ");
					}
					wkdayFile.print("\n");
				}else{
					thisHousehold.print(wkendFile);
					thisPerson.print(wkendFile);
					wkendFile.print(2+" ");			//dayno
 					wkendFile.print((n+1)+" ");			//chosen alternative #
					for(int i=0;i<numberAlternatives;++i){
						Pattern thisPattern=(Pattern) samplePatterns.get(i);
						thisPattern.print(wkendFile);
						double probability = ((double)thisPattern.observed)/((double)weekendPatterns.size());
						wkendFile.print((1/probability)+" ");
					}
					wkendFile.print("\n");
				}
			
			}
		}
	}

}