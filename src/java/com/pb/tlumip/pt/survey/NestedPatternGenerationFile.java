// PatternGenerationFile.java
//      jf 10/9/2000


package com.pb.tlumip.pt.survey;
import java.util.* ;
import java.io.* ;
import java.lang.Math ;

import com.pb.common.util.OutTextFile;

/**  This class creates an estimation
     file for the generation of day-patterns.
*/
public class NestedPatternGenerationFile{

	//number of alternatives
	int numberAlternatives=40 ; //1st=chosen, 2nd = home

	public ArrayList workerWeekdayPatternsWork1Tour = new ArrayList() ;  //6 alts
	public ArrayList workerWeekdayPatternsWork2Tour = new ArrayList() ;  //6 alts
	public ArrayList workerWeekdayPatternsWork3Tour = new ArrayList() ;  //6 alts
	public ArrayList workerWeekdayPatternsNoWork1Tour = new ArrayList();    //6 alts
	public ArrayList workerWeekdayPatternsNoWork2Tour = new ArrayList() ;    //6 alts
	public ArrayList workerWeekdayPatternsNoWork3Tour = new ArrayList() ;    //6 alts
	public ArrayList studentWeekdayPatternsSchool1Tour = new ArrayList() ; //6 alts
	public ArrayList studentWeekdayPatternsSchool2Tour = new ArrayList() ; //6 alts
	public ArrayList studentWeekdayPatternsSchool3Tour = new ArrayList() ; //6 alts
	public ArrayList studentWeekdayPatternsNoSchool1Tour = new ArrayList() ;   //6 alts
	public ArrayList studentWeekdayPatternsNoSchool2Tour = new ArrayList() ;   //6 alts
	public ArrayList studentWeekdayPatternsNoSchool3Tour = new ArrayList() ;   //6 alts
	public ArrayList otherWeekdayPatterns1Tour = new ArrayList(); //12 alts
	public ArrayList otherWeekdayPatterns2Tour = new ArrayList() ; //12 alts
	public ArrayList otherWeekdayPatterns3Tour = new ArrayList() ; //12 alts

	public ArrayList weekendPatterns=new ArrayList() ;

	public NestedPatternGenerationFile(List households, String workerPatternFileName, String studentPatternFileName,
	    String otherPatternFileName, String weekendPatternFileName) 
		throws IOException{

		//open output estimation files
		OutTextFile workerPatternFile = new OutTextFile() ;
		PrintWriter workerFile = workerPatternFile.open(workerPatternFileName) ;
		OutTextFile studentPatternFile = new OutTextFile() ;
		PrintWriter studentFile = studentPatternFile.open(studentPatternFileName) ;
		OutTextFile otherPatternFile = new OutTextFile() ;
		PrintWriter otherFile = otherPatternFile.open(otherPatternFileName) ;

		OutTextFile weekendPatternFile = new OutTextFile() ;
		PrintWriter wkendFile = weekendPatternFile.open(weekendPatternFileName) ;
		addWords(households) ;
		choosePattern(households,workerFile,studentFile,otherFile,wkendFile) ;
		
		workerFile.close() ;
		studentFile.close() ;
		otherFile.close() ;
		wkendFile.close() ;

		


	}
	public void addWords(List households){

        int workerHomeAllDay= 0;
        int workerWork1Tour = 0;
        int workerWork2Tour = 0;
        int workerWork3Tour = 0;
        int workerOther1Tour= 0;
        int workerOther2Tour= 0;
        int workerOther3Tour= 0;
      
        int studentHomeAllDay = 0;
        int studentSchool1Tour= 0;
        int studentSchool2Tour= 0;
        int studentSchool3Tour= 0;
        int studentOther1Tour = 0;
        int studentOther2Tour = 0;
        int studentOther3Tour = 0;
        
        int otherHomeAllDay = 0;
        int other1Tour = 0;
        int other2Tour = 0;
        int other3Tour = 0;
  
	    int totalWeekendPatterns =0;    

		// iterate through household vector, add patterns to pattern vectors
		System.out.println("Adding words to arraylists\n") ;
		ListIterator h = households.listIterator() ;
		while(h.hasNext()){
			Household thisHousehold = (Household)h.next() ;
			ListIterator p = thisHousehold.persons.listIterator() ;
			while(p.hasNext()){
				
				Person thisPerson = (Person)p.next() ;
		
				Pattern dayPattern = new Pattern(thisPerson.getWordWithStops(1)) ;
				//add the day1 pattern to the correct ArrayList
				
				for(int i=0 ;i<2 ;++i){
				
				    int day=0;
				    if(i==0){ //day1
				        day=thisHousehold.getDay1() ;
				        dayPattern = new Pattern(thisPerson.getWordWithStops(1)) ;
				    }else{ //day2
				        day=thisHousehold.getDay2() ;
				        dayPattern = new Pattern(thisPerson.getWordWithStops(2)) ;
				    }
				    if(day<=5){ //weekday
		            
		                if(thisPerson.isWorker()){
		                    if(dayPattern.workActivities>=1){
		                        if(dayPattern.homeActivities==1){
		                            ++workerHomeAllDay ;   //no tours
		                        }else if(dayPattern.homeActivities==2){
		                            addToList(dayPattern,workerWeekdayPatternsWork1Tour) ;
		                            ++workerWork1Tour;
		                        }else if(dayPattern.homeActivities==3){
		                            addToList(dayPattern,workerWeekdayPatternsWork2Tour) ;
		                            ++workerWork2Tour;
                                }else{
                                    addToList(dayPattern,workerWeekdayPatternsWork3Tour) ;
                                    ++workerWork3Tour;
                                }
                             }else{
		                        if(dayPattern.homeActivities==1){
		                            ++workerHomeAllDay ;     //no tours
		                        }else if(dayPattern.homeActivities==2){
		                            addToList(dayPattern,workerWeekdayPatternsNoWork1Tour) ;
		                            ++workerOther1Tour;
		                        }else if(dayPattern.homeActivities==3){
		                            addToList(dayPattern,workerWeekdayPatternsNoWork2Tour) ;
		                            ++workerOther2Tour;
                                }else{
                                    addToList(dayPattern,workerWeekdayPatternsNoWork3Tour) ;
                                    ++workerOther3Tour;
                                }
                             }
                       }else if(thisPerson.isStudent()){
		                    if(dayPattern.schoolActivities>=1){
		                        if(dayPattern.homeActivities==1){
		                            ++studentHomeAllDay ;    //no tours
		                        }else if(dayPattern.homeActivities==2){
		                            addToList(dayPattern,studentWeekdayPatternsSchool1Tour) ;    
		                            ++studentSchool1Tour;
		                        }else if(dayPattern.homeActivities==3){
		                            addToList(dayPattern,studentWeekdayPatternsSchool2Tour) ;
                                    ++studentSchool2Tour;
                                }else{
                                    addToList(dayPattern,studentWeekdayPatternsSchool3Tour) ;
                                    ++studentSchool3Tour;
                                }
                             }else{
		                        if(dayPattern.homeActivities==1){
		                            ++studentHomeAllDay;     //no tours
		                        }else if(dayPattern.homeActivities==2){
		                            addToList(dayPattern,studentWeekdayPatternsNoSchool1Tour) ;
		                            ++studentOther1Tour;
		                        }else if(dayPattern.homeActivities==3){
		                            addToList(dayPattern,studentWeekdayPatternsNoSchool2Tour) ;
		                            ++studentOther2Tour;
                                }else{
                                    addToList(dayPattern,studentWeekdayPatternsNoSchool3Tour) ;
                                    ++studentOther3Tour;
                                }
                             }
                       }else{
		                        if(dayPattern.homeActivities==1){
		                            ++otherHomeAllDay;   //no tours
		                        }else if(dayPattern.homeActivities==2){
		                            addToList(dayPattern,otherWeekdayPatterns1Tour) ;
		                            ++other1Tour;
		                        }else if(dayPattern.homeActivities==3){
		                            addToList(dayPattern,otherWeekdayPatterns2Tour) ;
                                    ++other2Tour;
                                }else{
                                    addToList(dayPattern,otherWeekdayPatterns3Tour) ;   
                                    ++other3Tour;
                                }
                       }         
				    }else{ //weekend
				        addToList(dayPattern,weekendPatterns) ;
				        ++totalWeekendPatterns;
                    }    
                }
            }
		}

        System.out.println("Total workerHomeAllDay = "   + workerHomeAllDay   ); 
        System.out.println("Total workerWork1Tour = "    + workerWork1Tour    );
        System.out.println("Total workerWork2Tour = "    + workerWork2Tour    );
        System.out.println("Total workerWork3Tour = "    + workerWork3Tour    );
        System.out.println("Total workerOther1Tour = "   + workerOther1Tour   );
        System.out.println("Total workerOther2Tour = "   + workerOther2Tour   );
        System.out.println("Total workerOther3Tour = "   + workerOther3Tour   );
        System.out.println("Total studentHomeAllDay = "  + studentHomeAllDay  );
        System.out.println("Total studentSchool1Tour = " + studentSchool1Tour );
        System.out.println("Total studentSchool2Tour = " + studentSchool2Tour );
        System.out.println("Total studentSchool3Tour = " + studentSchool3Tour );
        System.out.println("Total studentOther1Tour = "  + studentOther1Tour  );
        System.out.println("Total studentOther2Tour = "  + studentOther2Tour  );
        System.out.println("Total studentOther3Tour = "  + studentOther3Tour  );
        System.out.println("Total otherHomeAllDay = "    + otherHomeAllDay    );
        System.out.println("Total other1Tour = "         + other1Tour         );
        System.out.println("Total other2Tour = "         + other2Tour         );
        System.out.println("Total other3Tour = "         + other3Tour         );

	    System.out.println("Unique workerWeekdayPatternsWork1Tour        "+ workerWeekdayPatternsWork1Tour.size());
	    System.out.println("Unique workerWeekdayPatternsWork2Tour        "+ workerWeekdayPatternsWork2Tour.size());
	    System.out.println("Unique workerWeekdayPatternsWork3Tour        "+ workerWeekdayPatternsWork3Tour.size());
	    System.out.println("Unique workerWeekdayPatternsNoWork1Tour      "+ workerWeekdayPatternsNoWork1Tour.size());
	    System.out.println("Unique workerWeekdayPatternsNoWork2Tour      "+ workerWeekdayPatternsNoWork2Tour.size());
	    System.out.println("Unique workerWeekdayPatternsNoWork3Tour      "+ workerWeekdayPatternsNoWork3Tour.size());
	    System.out.println("Unique studentWeekdayPatternsSchool1Tour     "+ studentWeekdayPatternsSchool1Tour.size());
	    System.out.println("Unique studentWeekdayPatternsSchool2Tour     "+ studentWeekdayPatternsSchool2Tour.size());
	    System.out.println("Unique studentWeekdayPatternsSchool3Tour     "+ studentWeekdayPatternsSchool3Tour.size());
	    System.out.println("Unique studentWeekdayPatternsNoSchool1Tour   "+ studentWeekdayPatternsNoSchool1Tour.size());
	    System.out.println("Unique studentWeekdayPatternsNoSchool2Tour   "+ studentWeekdayPatternsNoSchool2Tour.size());
	    System.out.println("Unique studentWeekdayPatternsNoSchool3Tour   "+ studentWeekdayPatternsNoSchool3Tour.size());
	    System.out.println("Unique otherWeekdayPatterns1Tour             "+ otherWeekdayPatterns1Tour.size());
	    System.out.println("Unique otherWeekdayPatterns2Tour             "+ otherWeekdayPatterns2Tour.size());
	    System.out.println("Unique otherWeekdayPatterns3Tour             "+ otherWeekdayPatterns3Tour.size());

		System.out.println("Total weekend patterns  = "+totalWeekendPatterns) ;
		System.out.println("Unique weekend patterns = "+weekendPatterns.size()) ;
	}
	public void choosePattern(List households,PrintWriter workerFile, PrintWriter studentFile, PrintWriter otherFile, PrintWriter wkendFile){
		//now iterate through household vector, choose 39 patterns at random
		//for a total of 40 alternatives
		System.out.println("Constructing day-pattern estimation files\n") ;
		
		//create a day-pattern with only home activity for comparison
		Pattern homeAllDay=new Pattern("h") ;
		
		ListIterator h = households.listIterator() ;
		while(h.hasNext()){
			Household thisHousehold = (Household)h.next() ;
			ListIterator p = thisHousehold.persons.listIterator() ;
			while(p.hasNext()){
				
				Person thisPerson = (Person)p.next() ;
				Pattern dayPattern = new Pattern();
				ArrayList selectedPatterns = new ArrayList(numberAlternatives) ;
				
				int day=0;
				for(int d=0;d<2;++d){
				    if(d==0){ //day1
				        day=thisHousehold.getDay1();
				        dayPattern = new Pattern(thisPerson.getWordWithStops(1)) ;
				    }else{
				        day=thisHousehold.getDay2();
				        dayPattern = new Pattern(thisPerson.getWordWithStops(2)) ;
				    }
				    
				    selectedPatterns.add(dayPattern);   //first pattern is chosen   
				    selectedPatterns.add(homeAllDay);   //second pattern is home-all-day
				    
				    if(day<=5){ //weekday    	
	                
	                    if(thisPerson.isWorker()){
	                        thisHousehold.print(workerFile) ;
					        thisPerson.print(workerFile) ;
					        workerFile.print((d+1)+" ") ;			//dayno
 					        workerFile.print((1)+" ") ;			//chosen alternative #
                            for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,workerWeekdayPatternsWork1Tour,dayPattern,workerFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,workerWeekdayPatternsWork2Tour,dayPattern,workerFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,workerWeekdayPatternsWork3Tour,dayPattern,workerFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,workerWeekdayPatternsNoWork1Tour,dayPattern,workerFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,workerWeekdayPatternsNoWork2Tour,dayPattern,workerFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,workerWeekdayPatternsNoWork3Tour,dayPattern,workerFile);
					        workerFile.print("\n") ;
	                    }else if(thisPerson.isStudent()){
	                        thisHousehold.print(studentFile) ;
					        thisPerson.print(studentFile) ;
					        studentFile.print((d+1)+" ") ;			//dayno
 					        studentFile.print((1)+" ") ;			//chosen alternative #
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,studentWeekdayPatternsSchool1Tour,dayPattern,studentFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,studentWeekdayPatternsSchool2Tour,dayPattern,studentFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,studentWeekdayPatternsSchool3Tour,dayPattern,studentFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,studentWeekdayPatternsNoSchool1Tour,dayPattern,studentFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,studentWeekdayPatternsNoSchool2Tour,dayPattern,studentFile);
	                        for(int i=0;i<6;++i)
	                            selectRandomPattern(selectedPatterns,studentWeekdayPatternsNoSchool3Tour,dayPattern,studentFile);
	                        studentFile.print("\n");	                    
	                    }else{
	                        thisHousehold.print(otherFile) ;
					        thisPerson.print(otherFile) ;
					        otherFile.print((d+1)+" ") ;			//dayno
 					        otherFile.print((1)+" ") ;			//chosen alternative #
	                        for(int i=0;i<12;++i)
	                            selectRandomPattern(selectedPatterns,otherWeekdayPatterns1Tour,dayPattern,otherFile);
	                        for(int i=0;i<12;++i)
	                            selectRandomPattern(selectedPatterns,otherWeekdayPatterns2Tour,dayPattern,otherFile);
	                        for(int i=0;i<12;++i)
	                            selectRandomPattern(selectedPatterns,otherWeekdayPatterns3Tour,dayPattern,otherFile);
	                        otherFile.print("\n");
	                    }
	                
	                }else{ //weekend, do same way as before, except pattern 1 is chosen, pattern 2 is home, 3-40 is random from weekend list
	                    thisHousehold.print(wkendFile) ;
					    thisPerson.print(wkendFile) ;
					    wkendFile.print((d+1)+" ") ;			//dayno
 					    wkendFile.print((1)+" ") ;			//chosen alternative #
                        for(int i=0;i<38;++i)
	                        selectRandomPattern(selectedPatterns,weekendPatterns,dayPattern,wkendFile);
                        wkendFile.print("\n");
                    }
                }
            }
		}
	}
	public void addToList(Pattern dayPattern, ArrayList al){					
	                  	
	    //if this pattern already exists in arraylist, increment up observed by 1,else add ;
		boolean foundit=false ;
		int index=0 ;
		for(index=0 ;index<al.size() ;++index){
			foundit=(dayPattern.equals(al.get(index))) ;
			if(foundit)
				break ;
		}
		if(foundit==false){
			al.add(dayPattern) ;
		}else{
			System.out.println("found pattern\n") ;
			Pattern thisPattern = (Pattern) al.get(index) ;
			++thisPattern.observed ;
			al.set(index,thisPattern) ;
		}
    }

    public void selectRandomPattern(ArrayList selectedPatterns, ArrayList samplePatterns, Pattern chosenPattern, PrintWriter p){
	    
		//create a day-pattern with only home activity for comparison
		Pattern homeAllDay=new Pattern("h") ;
	    Pattern randomPattern = new Pattern("h");
	    while(true){
			//random #s ranges 0 thru number of patterns
			int s = new Double(Math.floor(Math.random() * samplePatterns.size())).intValue() ;
			randomPattern = (Pattern) samplePatterns.get(s) ;
			
			//don't use home pattern as alternative
			if(randomPattern.equals(homeAllDay)|| randomPattern.equals(chosenPattern)){
				System.out.println("Skipped at-home pattern|chosenPattern") ;
				continue ;
			}
			if(selectedPatterns.contains(randomPattern)==false){
				selectedPatterns.add(randomPattern) ;
				break ;
			}
		}
		//print the pattern and selection probability to estimation file
		randomPattern.print(p);
		double probability = ((double)randomPattern.observed)/((double)samplePatterns.size());
		p.print((1/probability)+" ") ;
					      
    }			
}