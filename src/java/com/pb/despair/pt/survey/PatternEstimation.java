// PatternEstimation.java
//      jf 10/9/2000


package com.pb.despair.pt.survey;
import java.util.*;
import java.io.*;
import java.lang.Math;

import com.pb.common.util.OutTextFile;

/**  This class estimates the generation of day-patterns.
*/
public class PatternEstimation{

	//following class variables are common to the household or person
	float d1;  
	float d2; 
	float d3;  
	float d4;  
	float d5;  
	float d6;  
	float d7;  
	float d8;  
	float d9;  
	float d10; 
	float d11; 
	float d12; 
	float d13; 
	float d14; 
	float d15; 
	float d16; 
	float d17; 
	float d18; 
	float d19; 
	float d20; 
	float d21; 
	float d22; 
	float d23; 
	float d24; 
	float d25; 
	float d26;
	float d27; 
	float d28; 
	float d29; 
	float d30; 
	float d31; 
	float d32; 
	float d33; 
	float d34; 
	float d35; 
	float d36; 
	float d37; 
	float wrkfull;
	float wrkpart;
	float unempd;
	float stdfull;
	float stdpart;
	float female;
	float hsize1;
	float hsize2;
	float hsize3p;
	float ageu21;
	float ageu25;
	float ageu60;
	float age00_15;
	float age15_21;
	float age21_50;
	float age50_70;
	float age70_80;
	float age80p;
	float age00_05;
	float age15_18;
	float age18_25;
	float age25_50;
	float age05_10;
	float age05_15;
	float age15_25;
	float age10_20;
	float age20_30;
	float age30_50;
	float age50_60;
	float age60_70;
	float chl0_5;
	float chl5_10;
	float chl0_15;
	float nwrkadlt;
	float nwrkadlt0;
	float nwrkadlt1;
	float inc0_15;
	float inclt15;
	float inc15_25;
	float inc25_55;
	float inc55p;
	float inc50p;
	float auto0;
	float auto1;
	float auto2p;
	float retail;
	float pserv;




	public PatternEstimation(List households, List weekdayPatterns, List weekendPatterns){
		//open output files
		OutTextFile OFile = new OutTextFile();
		PrintWriter estFile = OFile.open("EstPatterns.dat");
	
		ListIterator h = households.listIterator();
		while(h.hasNext()){
			Household thisHousehold = (Household)h.next();
			ListIterator p = thisHousehold.persons.listIterator();
			while(p.hasNext()){
				Person thisPerson = (Person)p.next(); 
				// set up data items
				//Following variables for each household
   				d1  = thisHousehold.sampleNumber;
   				d2  = thisHousehold.stratum;
   				d3  = thisHousehold.householdSize;
   				d4  = thisHousehold.ownHome;
   				d5  = thisHousehold.typeHome;
   				d6  = thisHousehold.numberVehicles;
   				d7  = thisHousehold.yearsResidence;
   				d8  = thisHousehold.incomeLevel;
   				d9  = thisHousehold.income1;
   				d10 = thisHousehold.incomeReference;
   				d11 = thisHousehold.assign;
   				d12 = thisHousehold.day1;
   				d13 = thisHousehold.day2;
   				d14 = thisHousehold.fullWorkers;
   				d15 = thisHousehold.partWorkers;
   				d16 = thisHousehold.child0to5;
   				d17 = thisHousehold.child5to10;
   				d18 = thisHousehold.child10to15;
   				d19 = thisHousehold.child15to18;
   				d20 = thisHousehold.adults;
				//Following variables for each person
   				d21 = thisPerson.personNumber;
   				d22 = thisPerson.relationship;
   				d23 = ifeq(thisPerson.female,true);
   				d24 = thisPerson.age;
   				d25 = thisPerson.license;
   				d26 = thisPerson.employmentStatus;
   				d27 = thisPerson.occupation;
   				d28 = thisPerson.industry;
   				d29 = thisPerson.lengthAtJob;
   				d30 = thisPerson.telecommute;
   				d31 = ifeq(thisPerson.shift,true);
   				d32 = thisPerson.studentStatus;
   				d33 = thisPerson.studentLevel;
   				d34 = thisPerson.educationLevel;
   				d35 = thisPerson.ethnicity;
   				d36 = ifeq(thisPerson.disabled,true);
   				d37 = thisPerson.typeDisability1;
  				//data transformations
  				wrkfull=ifeq(d26,1) | ifeq(d26,3);
				wrkpart=ifeq(d26,2) | ifeq(d26,4);
				unempd=ifgt(d26,4);
				stdfull=ifeq(d32,1);
				stdpart=ifeq(d32,2);
				female=ifeq(d23,1);
				hsize1=ifeq(d3,1);
				hsize2=ifeq(d3,2);
				hsize3p=ifge(d3,3);
				ageu21=iflt(d24,21);
				ageu25=ifge(d24,21) * iflt(d24,25);
				ageu60=ifge(d24,25) * iflt(d24,60);
				age00_15=iflt(d24,15);
				age05_15=ifge(d24,5) * iflt(d24,15);
				age15_21=ifge(d24,15) * iflt(d24,21);
				age21_50=ifge(d24,21) * iflt(d24,50);
				age50_70=ifge(d24,50) * iflt(d24,70);
				age80p=ifge(d24,80) * iflt(d24,99);            
				age05_10=ifge(d24,5) * iflt(d24,10);
				age10_20=ifge(d24,10) * iflt(d24,20);
				age15_25=ifge(d24,15) * iflt(d24,25);
				age25_50=ifge(d24,25) * iflt(d24,50);
				age20_30=ifge(d24,20) * iflt(d24,30);
				age30_50=ifge(d24,30) * iflt(d24,50);
				age50_60=ifge(d24,50) * iflt(d24,60);
				age60_70=ifge(d24,60) * iflt(d24,70);
				age70_80=ifge(d24,70) * iflt(d24,80);
				age00_05=iflt(d24,5);
				age15_18=ifge(d24,15) * iflt(d24,18);
				age18_25=ifge(d24,18) * iflt(d24,25);
				chl0_5=ifgt(d16,0);
				chl5_10=ifgt(d17,0);
				chl0_15=ifgt(d16,0) | ifgt(d17,0) | ifgt(d18,0);
				nwrkadlt=d20-d14;
				nwrkadlt=nwrkadlt*ifge(nwrkadlt,0); 
				nwrkadlt0=ifeq(nwrkadlt,0);
				nwrkadlt1=ifeq(nwrkadlt,1);
				inc0_15=ifle(d9,3);
				inclt15=ifle(d9,3);
				inc15_25=ifgt(d9,3) * ifle(d9,5);
				inc25_55=ifgt(d9,5) * ifle(d9,11);
				inc55p=ifgt(d9,11) * ifle(d9,13); 
				inc50p=ifge(d9,11);
				auto0=ifeq(d6,0);
				auto1=iflt(d6,20) * ifeq(auto0,0);
				auto2p=ifge(d6,20) * ifeq(auto0,0) * ifeq(auto1,0);
				retail=ifeq(d28,7);
				pserv=ifeq(d28,10);
				if(d1==100002 && d21 == 1)
					System.out.println( 
						 "d1 "+       d1       
						+"d2 "+       d2       
						+"d3 "+       d3       
						+"d4 "+       d4       
						+"d5 "+       d5       
						+"d6 "+       d6       
						+"d7 "+       d7       
						+"d8 "+       d8       
						+"d9 "+       d9       
						+"d10 "+      d10      
						+"d11 "+      d11      
						+"d12 "+      d12      
						+"d13 "+      d13      
						+"d14 "+      d14      
						+"d15 "+      d15      
						+"d16 "+      d16      
						+"d17 "+      d17      
						+"d18 "+      d18      
						+"d19 "+      d19      
						+"d20 "+      d20      
						+"d21 "+      d21      
						+"d22 "+      d22      
						+"d23 "+      d23      
						+"d24 "+      d24      
						+"d25 "+      d25      
						+"d26 "+      d26      
						+"d27 "+      d27      
						+"d28 "+      d28      
						+"d29 "+      d29      
						+"d30 "+      d30      
						+"d31 "+      d31      
						+"d32 "+      d32      
						+"d33 "+      d33      
						+"d34 "+      d34      
						+"d35 "+      d35      
						+"d36 "+      d36      
						+"d37 "+      d37      
						+"wrkfull "+  wrkfull  
						+"wrkpart "+  wrkpart  
						+"unempd "+   unempd   
						+"stdfull "+  stdfull  
						+"stdpart "+  stdpart  
						+"female "+   female   
						+"hsize1 "+   hsize1   
						+"hsize2 "+   hsize2   
						+"hsize3p "+  hsize3p  
						+"ageu21 "+   ageu21   
						+"ageu25 "+   ageu25   
						+"ageu60 "+   ageu60   
						+"age00_15 "+ age00_15 
						+"age15_21 "+ age15_21 
						+"age21_50 "+ age21_50 
						+"age50_70 "+ age50_70 
						+"age70_80 "+ age70_80 
						+"age80p "+   age80p   
						+"age00_05 "+ age00_05 
						+"age15_18 "+ age15_18 
						+"age18_25 "+ age18_25 
						+"age25_50 "+ age25_50 
						+"age05_10 "+ age05_10 
						+"age05_15 "+ age05_15 
						+"age15_25 "+ age15_25 
						+"age10_20 "+ age10_20 
						+"age20_30 "+ age20_30 
						+"age30_50 "+ age30_50 
						+"age50_60 "+ age50_60 
						+"age60_70 "+ age60_70 
						+"chl0_5 "+   chl0_5   
						+"chl5_10 "+  chl5_10  
						+"chl0_15 "+  chl0_15  
						+"nwrkadlt "+ nwrkadlt 
						+"nwrkadlt0 "+nwrkadlt0
						+"nwrkadlt1 "+nwrkadlt1
						+"inc0_15 "+  inc0_15  
						+"inclt15 "+  inclt15  
						+"inc15_25 "+ inc15_25 
						+"inc25_55 "+ inc25_55 
						+"inc55p "+   inc55p   
						+"inc50p "+   inc50p   
						+"auto0 "+    auto0    
						+"auto1 "+    auto1    
						+"auto2p "+   auto2p   
						+"retail "+   retail   
						+"pserv "+    pserv );
				Pattern day1Pattern,day2Pattern;
				//day 1 logit model
				if(thisHousehold.day1<=5){  //weekday
					day1Pattern = chooseWeekday(weekdayPatterns);
				}else{                      //weekend
					day1Pattern = chooseWeekend(weekendPatterns);
				
				}
				//print day1
				thisHousehold.print(estFile);
				thisPerson.print(estFile);
				estFile.print(1+" ");
				day1Pattern.print(estFile);
				estFile.println();

		
				//day 2 logit model
				if(thisHousehold.day1<=5){	//weekday
					day2Pattern = chooseWeekday(weekdayPatterns);
				}else{						//weekend
					day2Pattern = chooseWeekend(weekendPatterns);
				}
				//print day2
				thisHousehold.print(estFile);
				thisPerson.print(estFile);
				estFile.print(2+" ");
				day2Pattern.print(estFile);
				estFile.println();
			
			} //end persons
		} //end households
		estFile.close();
	} //end PatternEstimation
	
	Pattern chooseWeekday(List weekdayPatterns){
		
		ArrayList alternatives = new ArrayList();
		Pattern chosenPattern = new Pattern("");		
		double totalExponents=0;
		ListIterator wkdayIterator = weekdayPatterns.listIterator();
		while(wkdayIterator.hasNext()){

			Pattern pattern = (Pattern) wkdayIterator.next();
			Alternative alt = new Alternative();
			
			//skip if pattern contains work and not worker
			if(pattern.wrkDummy==1 && (wrkfull+wrkpart)==0)
				continue;
			//skip if pattern contains school and not student
			if(pattern.schDummy==1 && (stdfull+stdpart)==0)
				continue;

			float dd1 	= pattern.homeActivities;     
			float dd2	= pattern.workActivities;	   
			float dd3	= pattern.schoolActivities;	  
			float dd4	= pattern.shopActivities;	   
			float dd5	= pattern.recreateActivities;	  
			float dd6	= pattern.otherActivities;	   
			float dd7	= pattern.workBasedTours;	   
			float dd8	= pattern.tour1IStops;	    
			float dd9	= pattern.tour2IStops;	    
			float dd10	= pattern.tour3IStops;	    
			float dd11	= pattern.tour4PIStops;	   
			float dd12	= pattern.workTourIStops;	   
			float dd13	= pattern.nonWorkTourIStops;	  
			float dd14	= pattern.workPSchool;	    
			float dd15	= pattern.workPShop;	    
			float dd16	= pattern.workPRecreate;	   
			float dd17	= pattern.workPOther;	    
			float dd18	= pattern.schoolPShop;	    
			float dd19	= pattern.schoolPRecreate;	   
			float dd20	= pattern.schoolPOther;	   
			float dd21	= pattern.shopPRecreate;	   
			float dd22	= pattern.shopPOther;	    
			float dd23	= pattern.recreatePOther;	   
			float dd24	= pattern.stopsOnWorkTours;	  
			float dd25	= pattern.stopsOnSchoolTours;	  
			float dd26	= pattern.stopsOnShopTours;	  
			float dd27	= pattern.stopsOnRecreateTours;	 
			float dd28	= pattern.stopsOnOtherTours;	  
			float dd29	= pattern.tour1Purpose;	   
			float dd30	= pattern.tour2Purpose;	   
			float dd31	= pattern.tour3Purpose;	   
			float dd32	= pattern.tour4Purpose;	   
			float dd33	= pattern.t1Dummy;                  
			float dd34	= pattern.t2Dummy;                  
			float dd35	= pattern.t3Dummy;                  
			float dd36	= pattern.t4Dummy;                  
			float dd37	= pattern.t5pDummy;                 
			float dd38  = pattern.wrkDummy;                 
			float dd39  = pattern.schDummy;                 
			float dd40  = pattern.shpDummy;                 
			float dd41  = pattern.recDummy;                 
			float dd42  = pattern.othDummy;                 
			float dd43  = pattern.wkbDummy;                 
			float n5000 =(wrkfull*dd38 );                          
			float n5001 =(wrkpart*dd38 );                          
			float n5002 =(stdfull*dd39 );                          
			float n5003 =(stdpart*dd39 );                          
			float n5004 =(female*dd40 ) ;                          
			float n5005 =(unempd*dd40 )  ;                         
			float n5006 =(hsize1*dd40 ) ;                          
			float n5007 =(hsize2*dd40 ) ;                          
			float n5008 =(hsize3p*dd40 ) ;                         
			float n5009 =(ageu21*dd41 ) ;                          
			float n5010 =(ageu25*dd41 ) ;                          
			float n5011 =(ageu60*dd41 ) ;                          
			float n5012 =(wrkfull*dd41 );                          
			float n5013 =(wrkpart*dd41 );                          
			float n5014 =(unempd*dd41 ) ;                          
			float n5015 =(age00_05*(dd1 -1));                      
			float n5016 =(age05_15*(dd1 -1));                     
			float n5017 =(age15_18*(dd1 -1));                     
			float n5018 =(age18_25*(dd1 -1));                     
			float n5019 =(age25_50*(dd1 -1));                      
			float n5020 =(age50_60*(dd1 -1));                      
			float n5021 =(age60_70*(dd1 -1));                      
			float n5022 =(age70_80*(dd1 -1));                      
			float n5023 =(age80p*(dd1 -1));                        
			float n5024 =(chl0_15*dd24 *ifeq(dd38 ,1));            
			float n5025 =(inc0_15*dd24 *ifeq(dd38 ,1));            
			float n5026 =(inc50p*dd24 *ifeq(dd38 ,1));             
			float n5027 =(auto0*dd24 *ifeq(dd38 ,1));              
			float n5028 =(dd24 *(dd1 -2));                         
			float n5034 =(age15_25*(dd1 -1));                      
			float n5035 =(age50_70*(dd1 -1));                      
			float n5029 =(ifgt(dd2 ,0)*ifne(dd29 ,1));             
			float n5030 =(ifgt(dd3 ,0)*ifne(dd29 ,2));             
			float n5031 =dd24;                                     
			float n5032 =dd25;                                     
			float n5033 =dd26;                                     
			float n5036 =chl0_15*((dd25 )+(dd26 )+(dd27 )+(dd28 ));
			float n5037 =inc0_15*((dd25 )+(dd26 )+(dd27 )+(dd28 ));
			float n5038 =inc50p*((dd25 )+(dd26 )+(dd27 )+(dd28 )); 
            float n5039 =auto0*((dd25 )+(dd26 )+(dd27 )+(dd28 ));  
            float n5040 =dd4;                                      
            float n5041 =dd5;                                      
		    float n5042 =dd6;                                      
		    float n5043 =(wrkfull*dd43 );                          
			float n5044 =(wrkpart*dd43 ); 
			alt.utility= (  1.78531786813     *n5000)  
						+(  1.55373224779     *n5001)  
						+(  3.05825932958     *n5002) 
						+(  1.36136599016     *n5003) 
						+(  .364256181405     *n5004) 
						+(  .547345701521     *n5005) 
						+( -.480664296428     *n5006) 
						+( -.440659923444     *n5007) 
						+( -.692676914465     *n5008) 
						+(  .663342938958     *n5009) 
						+(  .607446295017     *n5010) 
						+(  .204328107819     *n5011) 
						+( -.419702425052     *n5012) 
						+( -.176053715063     *n5013) 
						+(  .254042556524     *n5014) 
						+( -2.40106637703     *n5015)
						+( -2.52922763949     *n5016)
						+( -2.29786066364     *n5034)
						+( -2.06175778338     *n5019)
						+( -2.23128787209     *n5035)
						+( -2.24671073626     *n5022)
						+( -2.52793869695     *n5023)
						+(  .109001318266     *n5024)
						+( -.222915547413     *n5025)
						+( -.113116905124     *n5026)
						+( -.367064800472     *n5027)
						+(  .062406762333     *n5028)
						+( -1.73616536401     *n5029)
						+( -2.69475910880     *n5030)
						+( -1.94431081184     *n5031)
						+( -1.89526001838     *n5032)
						+( -.491900204056     *n5033)
						+( -.443514206685     *n5036)
						+( -.570193898424     *n5038)
						+( -.985861777303     *n5039)
						+( -.179274893697     *n5040)
						+( -.529394713412     *n5041)
						+( -.049637382232     *n5042)
						+( -.119682369559     *n5043)
						+( -1.16427008830     *n5044);
				alt.exponentiatedUtility=Math.exp(alt.utility);
				alternatives.add(alt);
				totalExponents += alt.exponentiatedUtility;
				if(d1==100002 && d21 == 1)
					System.out.println("utility "+alt.utility+" expUtility "+alt.exponentiatedUtility
						+" totalExp "+totalExponents);
			}		
			ListIterator altIterator=alternatives.listIterator();
			double cumulativeProbability=0;
			int n=0;
			//random #randomDraw ranges 0 0.999
			double randomDraw = Math.random();
			if(d1==100002 && d21 == 1)
				System.out.println("randomDraw "+ randomDraw+" alternative size "+alternatives.size());
			while(altIterator.hasNext()){
				Pattern thisPattern = (Pattern)weekdayPatterns.get(n);
				++n;				

				//skip if pattern contains work and not worker
				if(thisPattern.wrkDummy==1 && (wrkfull+wrkpart)==0)
					continue;
				//skip if pattern contains school and not student
				if(thisPattern.schDummy==1 && (stdfull+stdpart)==0)
					continue;
				
				Alternative thisAlternative = (Alternative)altIterator.next();
					
				cumulativeProbability += ( thisAlternative.exponentiatedUtility/totalExponents);
				
				if(d1==100002 && d21 == 1)
					System.out.println("n "+n+" probability "+ ( thisAlternative.exponentiatedUtility/totalExponents)
						+" cumulativeProb "+cumulativeProbability);
						
				if(cumulativeProbability >= randomDraw ){
					chosenPattern= thisPattern;			
					if(d1==100002 && d21 == 1)
						System.out.println("chosen pattern "+chosenPattern.dayPattern);
					break;
				}		
				
			}
			return chosenPattern;
		}
	Pattern chooseWeekend(List weekendPatterns){
		
		ArrayList alternatives = new ArrayList(); 
		double totalExponents=0;
		ListIterator wkendIterator =  weekendPatterns.listIterator();	
		Pattern chosenPattern = new Pattern("");
		while(wkendIterator.hasNext()){
			Alternative alt = new Alternative();
			Pattern pattern = (Pattern) wkendIterator.next();

			//skip if pattern contains work and not worker
			if(pattern.wrkDummy==1 && (wrkfull+wrkpart)==0)
				continue;
			//skip if pattern contains school and not student
			if(pattern.schDummy==1 && (stdfull+stdpart)==0)
				continue;

			float dd1 	= pattern.homeActivities;     
			float dd2	= pattern.workActivities;	   
			float dd3	= pattern.schoolActivities;	  
			float dd4	= pattern.shopActivities;	   
			float dd5	= pattern.recreateActivities;	  
			float dd6	= pattern.otherActivities;	   
			float dd7	= pattern.workBasedTours;	   
			float dd8	= pattern.tour1IStops;	    
			float dd9	= pattern.tour2IStops;	    
			float dd10	= pattern.tour3IStops;	    
			float dd11	= pattern.tour4PIStops;	   
			float dd12	= pattern.workTourIStops;	   
			float dd13	= pattern.nonWorkTourIStops;	  
			float dd14	= pattern.workPSchool;	    
			float dd15	= pattern.workPShop;	    
			float dd16	= pattern.workPRecreate;	   
			float dd17	= pattern.workPOther;	    
			float dd18	= pattern.schoolPShop;	    
			float dd19	= pattern.schoolPRecreate;	   
			float dd20	= pattern.schoolPOther;	   
			float dd21	= pattern.shopPRecreate;	   
			float dd22	= pattern.shopPOther;	    
			float dd23	= pattern.recreatePOther;	   
			float dd24	= pattern.stopsOnWorkTours;	  
			float dd25	= pattern.stopsOnSchoolTours;	  
			float dd26	= pattern.stopsOnShopTours;	  
			float dd27	= pattern.stopsOnRecreateTours;	 
			float dd28	= pattern.stopsOnOtherTours;	  
			float dd29	= pattern.tour1Purpose;	   
			float dd30	= pattern.tour2Purpose;	   
			float dd31	= pattern.tour3Purpose;	   
			float dd32	= pattern.tour4Purpose;	   
			float dd33	= pattern.t1Dummy;                  
			float dd34	= pattern.t2Dummy;                  
			float dd35	= pattern.t3Dummy;                  
			float dd36	= pattern.t4Dummy;                  
			float dd37	= pattern.t5pDummy;                 
			float dd38  = pattern.wrkDummy;                 
			float dd39  = pattern.schDummy;                 
			float dd40  = pattern.shpDummy;                 
			float dd41  = pattern.recDummy;                 
			float dd42  = pattern.othDummy;                 
			float dd43  = pattern.wkbDummy;                 
			float n5000 =(this.age00_15*(dd1 -1));
			float n5001 =(age15_21*(dd1 -1));
			float n5002 =(age21_50*(dd1 -1));
			float n5003 =(age50_70*(dd1 -1));
			float n5004 =(age70_80*(dd1 -1));
			float n5005 =(age80p*(dd1 -1));
			float n5006 =(chl0_5*(dd1 -1));
			float n5007 =(chl5_10*(dd1 -1));
			float n5008 =(wrkfull*(dd1 -1));
			float n5009 =(nwrkadlt0*(dd1 -1));
			float n5010 =(nwrkadlt1*(dd1 -1));
			float n5011 =(inclt15*(dd1 -1));
			float n5012 =(inc15_25*(dd1 -1));
			float n5013 =(inc25_55*(dd1 -1));
			float n5014 =(inc55p*(dd1 -1));
			float n5015 =(auto0*(dd1 -1));
			float n5016 =(auto1*(dd1 -1));
			float n5017 =(auto2p*(dd1 -1));
			float n5018 =(ifeq(d23,1)*dd40);
			float n5019 =(age05_10*dd41 );
			float n5020 =(age10_20*dd41 );
			float n5021 =(age20_30*dd41 );
			float n5022 =(age30_50*dd41 );
			float n5023 =inclt15*dd4 ;
			float n5024 =inc15_25*dd4 ;
			float n5025 =inc25_55*dd4 ;
			float n5026 =inc55p*dd4 ;
			float n5027 =(age05_10*dd5 );
			float n5028 =(age10_20*dd5 );
			float n5029 =(age20_30*dd5 );
			float n5030 =(age30_50*dd5 );
			float n5031 =(dd26 *(dd40 ));
			float n5032 =(dd27 *(dd41 ));
			float n5033 =(dd28 *(dd42 ));
			float n5034 =(ifeq(d3,1)*dd26 );
			float n5035 =(ifeq(d6,0)*dd26 );
			float n5036 =(ifeq(d6,0)*dd27 );
			float n5037 =inclt15*dd27; 
			float n5038 =(ifeq(d6,0)*dd28 );
			float n5039 =(retail*dd38 );
			float n5040 =(pserv*dd38 );
			alt.utility= ( -.240903548354    *n5000)
					+(  .040649211873    *n5001)
					+(  .240451678146    *n5002)
					+(  .062300512548    *n5003)
					+( -.042098088939    *n5004)
					+( -.178930364287    *n5005)
					+( -.173690056602    *n5006)
					+(  .134811333209    *n5007)
					+( -.012518754687    *n5008)
					+(  .052210646659    *n5009)
					+(  .128343422834    *n5010)
					+( -.016190038537    *n5011)
					+(  .142461523076    *n5012)
					+(  .189543299550    *n5013)
					+(  .259985533146    *n5014)
					+( -2.31426089549    *n5015)
					+( -1.81391357894    *n5016)
					+( -2.78571635086    *dd38 )
					+( -3.72833504329    *dd39 )
					+( -.601309628366    *dd40 )
					+( -.301749617950    *dd41 )
					+( -.862613960212    *dd42 )
					+( -4.50536892120    *dd43 )
					+(  .307984994387    *n5018)
					+(  .972139676550    *n5019)
					+(  .444374973732    *n5020)
					+(  .475772170099    *n5021)
					+(  .378483383573    *n5022)
					+( -.323520122389    *n5023)
					+( -.216040334892    *n5024)
					+( -.273575682921    *n5025)
					+( -.234417822996    *n5026)
					+( -.031280922868    *n5027)
					+(  .056890577097    *n5028)
					+( -.367216075262    *n5029)
					+( -.377956871558    *n5030)
					+( -1.04055665745    *n5031)
					+( -1.23617871900    *n5032)
					+( -1.62687282022    *n5033)
					+(  .146863786989    *n5034)
					+( -.262834013304    *n5035)
					+( -.398248472497    *n5036)
					+( -.292818223703    *n5037)
					+( -.500798555289    *n5038)
					+(  1.93329780671    *n5039)
					+(  1.19703741694    *n5040);
					if(d1==100002 && d21 == 1)
						System.out.println("utility "+alt.utility+" expUtility "+alt.exponentiatedUtility
							+" totalExp "+totalExponents);
					alt.exponentiatedUtility=Math.exp(alt.utility);
					alternatives.add(alt);
					totalExponents += alt.exponentiatedUtility;
				}		
			ListIterator altIterator=alternatives.listIterator();
			double cumulativeProbability=0;
			int n=0;
			//random #randomDraw ranges 0 0.999
			double randomDraw = Math.random();
			if(d1==100002 && d21 == 1)
				System.out.println("randomDraw "+ randomDraw+" alternative size "+alternatives.size());
			while(altIterator.hasNext()){
				Pattern thisPattern = (Pattern)weekendPatterns.get(n);
				++n;
				//skip if pattern contains work and not worker
				if(thisPattern.wrkDummy==1 && (wrkfull+wrkpart)==0)
					continue;
				//skip if pattern contains school and not student
				if(thisPattern.schDummy==1 && (stdfull+stdpart)==0)
					continue;
				Alternative thisAlternative = (Alternative)altIterator.next();
				
				cumulativeProbability += ( thisAlternative.exponentiatedUtility/totalExponents);
				
				if(d1==100002 && d21 == 1)
					System.out.println("probability "+ ( thisAlternative.exponentiatedUtility/totalExponents)
						+" cumulativeProb "+cumulativeProbability);

				if(cumulativeProbability >= randomDraw ){
					chosenPattern= thisPattern;			
					if(d1==100002 && d21 == 1)
						System.out.println("chosen pattern "+chosenPattern.dayPattern);
					break;
				}		
				
			}
			return chosenPattern;
		}
	
	int ifeq(float d1, float d2){
		if(d1==d2)
			return 1;
		else 
			return 0;
	}
	int ifeq(boolean d1, boolean d2){
		if(d1==d2)
			return 1;
		else 
			return 0;
	}
	int ifeq(float d1, int d2){
		if(d1==d2)
			return 1;
		else 
			return 0;
	}
	int ifge(float d1, float d2){
		if(d1>=d2)
			return 1;
		else
			return 0;
	}
	int ifge(float d1, int d2){
		if(d1>=d2)
			return 1;
		else
			return 0;
	}
	int ifgt(float d1, float d2){
		if(d1>=d2)
			return 1;
		else
			return 0;
	}
	int ifgt(float d1, int d2){
		if(d1>=d2)
			return 1;
		else
			return 0;
	}
	int ifle(float d1, float d2){
		if(d1<=d2)
			return 1;
		else
			return 0;
	}
	int ifle(float d1, int d2){
		if(d1<=d2)
			return 1;
		else
			return 0;
	}
	int iflt(float d1, float d2){
		if(d1<d2)
			return 1;
		else
			return 0;
	}
	int iflt(float d1, int d2){
		if(d1<d2)
			return 1;
		else
			return 0;
	}
	int ifne(float d1, float d2){
		if(d2!=d1)
			return 1;
		else
			return 0;
	}
	int ifne(float d1, int d2){
		if(d2!=d1)
			return 1;
		else
			return 0;
	}
	class Alternative{
		public double utility;
		public double exponentiatedUtility;
		public double probability;
	}
		
}