package com.pb.despair.spg;

import java.io.File;
import java.io.IOException;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;


public class EdIndustry {

    int[] pumsToEd = new int[1000];
    
    String[] edIndustryLabels = {
		"ACCOMMODATIONS",
		"AGRICULTURE AND MINING",
		"COMMUNICATIONS AND UTILITIES",
		"CONSTRUCTION",
		"ELECTRONICS AND INSTRUMENTS-Light Industry",
		"FIRE BUSINESS AND PROFESSIONAL SERVICES",
		"FOOD PRODUCTS-Heavy Industry",
		"FOOD PRODUCTS-Light Industry",
		"FORESTRY AND LOGGING",
		"GOVERNMENT ADMINISTRATION",
		"HEALTH SERVICES-Hospital",
		"HEALTH SERVICES-Institutional",
		"HEALTH SERVICES-Office",
		"HIGHER EDUCATION",
		"HOMEBASED SERVICES",
		"LOWER EDUCATION",
		"LUMBER AND WOOD PRODUCTS-Heavy Industry",
		"OTHER DURABLES-Heavy Industry",
		"OTHER DURABLES-Light Industry",
		"OTHER NON-DURABLES-Heavy Industry",
		"OTHER NON-DURABLES-Light Industry",
		"PERSONAL AND OTHER SERVICES AND AMUSEMENTS",
		"PULP AND PAPER-Heavy Industry",
		"RETAIL TRADE",
		"TRANSPORT",
		"WHOLESALE TRADE",
		"UNEMPLOYED"
    };
    
/*
	int[] edIndustryEmployment = {
		64,
		188, 
		45,
		239,
		71,
		546,
		30,
		15,
		17, 
		248,
		83,
		34,
		109,
		96,
		21,
		149,
		70,
		82,
		42,
		19,
		33,
		326,
		19,
		574,
		105,
		161
	};
*/
    
	float[] edIndustryEmployment = {
		64997,
		188146, 
		45304,
		239022,
		71484,
		546557,
		30776,
		15412,
		17838, 
		248056,
		83617,
		34723,
		109699,
		96966,
		21337,
		149835,
		70874,
		82635,
		42884,
		19220,
		33964,
		326965,
		19968,
		574600,
		105767,
		161896
	};
    
    
    public EdIndustry () {

		// establish the correspondence table between pums codes and ED industry categories
		pumsIndustryToEdIndustry();

    }

    

	// return the number of ED industry categories.    
	public int getNumberEdIndustries() {
		return edIndustryLabels.length;
	}



	// return the ED industry category index given the pums industry code
	// from the pums person record INDUSTRY field.    
	public int getEdIndustry(int pumsIndustry) {
		return pumsToEd[pumsIndustry];
	}



	// return the ED industry category label given the index.    
	public String getEdIndustryLabel(int edIndustry) {
		return edIndustryLabels[edIndustry];
	}



	// return all the ED industry category labels.    
	public String[] getEdIndustryLabels() {
		return edIndustryLabels;
	}


	// return the default array of ed industry employment
	public float[] getEdIndustryEmployment() {
		return edIndustryEmployment;
	}
	
	
	// return the array of ed industry employment from the named file
	public float[] getEdIndustryEmployment( String fileName ) {
	 
		// read the ED regional employment by industry output file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		// this table has one row of employment totals for each industry
		String[] tempIndustryLabels = table.getColumnAsString(1);
		float[] edIndustryEmployment = table.getColumnAsFloat(2);
		
		edIndustryLabels = new String[tempIndustryLabels.length+1];
		for (int i=0; i < tempIndustryLabels.length; i++)
		    edIndustryLabels[i] = tempIndustryLabels[i];
		edIndustryLabels[edIndustryLabels.length-1] = "UNEMPLOYED";
	    
		return edIndustryEmployment;
	}
	
	
	// define the pums/ed industry index correspondence table
    private void pumsIndustryToEdIndustry () {
        
        //ACCOMMODATIONS
		for (int i=762; i <= 770; i++)
			pumsToEd[i] = 0;
        
        //AGRICULTURE AND MINING
		for (int i=1; i <= 30; i++)
			pumsToEd[i] = 1;
		for (int i=32; i <= 59; i++)
			pumsToEd[i] = 1;
		
		//COMMUNICATIONS AND UTILITIES
		for (int i=440; i <= 499; i++)
			pumsToEd[i] = 2;

		//CONSTRUCTION
		for (int i=60; i <= 99; i++)
			pumsToEd[i] = 3;

		//ELECTRONICS AND INSTRUMENTS-Light Industry
		for (int i=322; i <= 330; i++)
			pumsToEd[i] = 4;
		for (int i=371; i <= 380; i++)
			pumsToEd[i] = 4;

		//FIRE BUSINESS AND PROFESSIONAL SERVICES
		for (int i=700; i <= 741; i++)
		    pumsToEd[i] = 5;
		for (int i=882; i <= 899; i++)
			pumsToEd[i] = 5;
		pumsToEd[841] = 5;

		//FOOD PRODUCTS-Heavy Industry
		for (int i=100; i <= 110; i++)
			pumsToEd[i] = 6;

		//FOOD PRODUCTS-Light Industry
		for (int i=111; i <= 129; i++)
			pumsToEd[i] = 7;
		
		//FORESTRY AND LOGGING
		pumsToEd[31] = 8;
		pumsToEd[230] = 8;

		//GOVERNMENT ADMINISTRATION
		for (int i=900; i <= 999; i++)
			pumsToEd[i] = 9;
		
		//HEALTH SERVICES-Hospital
		for (int i=833; i <= 839; i++)
			pumsToEd[i] = 10;
		pumsToEd[831] = 10;
		
		//HEALTH SERVICES-Institutional
		pumsToEd[832] = 11;
		
		//HEALTH SERVICES-Office
		for (int i=812; i <= 830; i++)
			pumsToEd[i] = 12;
		pumsToEd[840] = 12;
		
		//HIGHER EDUCATION
		pumsToEd[850] = 13;
		pumsToEd[851] = 13;
		
		//HOMEBASED SERVICES
		pumsToEd[761] = 14;
		
		//LOWER EDUCATION
		for (int i=842; i <= 849; i++)
			pumsToEd[i] = 15;
		
		//LUMBER AND WOOD PRODUCTS-Heavy Industry
		for (int i=231; i <= 241; i++)
			pumsToEd[i] = 16;
		
		//OTHER DURABLES-Heavy Industry
		for (int i=251; i <= 260; i++)
			pumsToEd[i] = 17;
		for (int i=262; i <= 280; i++)
			pumsToEd[i] = 17;
		for (int i=282; i <= 300; i++)
			pumsToEd[i] = 17;
		for (int i=302; i <= 320; i++)
			pumsToEd[i] = 17;
		for (int i=331; i <= 340; i++)
			pumsToEd[i] = 17;
		for (int i=342; i <= 349; i++)
			pumsToEd[i] = 17;
		for (int i=351; i <= 369; i++)
			pumsToEd[i] = 17;
		
		//OTHER DURABLES-Light Industry
		for (int i=242; i <= 250; i++)
			pumsToEd[i] = 18;
		for (int i=381; i <= 399; i++)
			pumsToEd[i] = 18;
		pumsToEd[261] = 18;
		pumsToEd[281] = 18;
		pumsToEd[301] = 18;
		pumsToEd[321] = 18;
		pumsToEd[341] = 18;
		pumsToEd[350] = 18;
		pumsToEd[370] = 18;
		
		//OTHER NON-DURABLES-Heavy Industry
		for (int i=141; i <= 150; i++)
			pumsToEd[i] = 19;
		for (int i=180; i <= 220; i++)
			pumsToEd[i] = 19;
		pumsToEd[132] = 19;
		
		//OTHER NON-DURABLES-Light Industry
		for (int i=133; i <= 140; i++)
			pumsToEd[i] = 20;
		for (int i=151; i <= 159; i++)
			pumsToEd[i] = 20;
		for (int i=171; i <= 179; i++)
			pumsToEd[i] = 20;
		for (int i=221; i <= 229; i++)
			pumsToEd[i] = 20;
		pumsToEd[130] = 20;
		pumsToEd[131] = 20;
		
		//PERSONAL AND OTHER SERVICES AND AMUSEMENTS
		for (int i=742; i <= 760; i++)
			pumsToEd[i] = 21;
		for (int i=771; i <= 811; i++)
			pumsToEd[i] = 21;
		for (int i=852; i <= 881; i++)
			pumsToEd[i] = 21;

		//PULP AND PAPER-Heavy Industry
		for (int i=160; i <= 170; i++)
			pumsToEd[i] = 22;

		//RETAIL TRADE
		for (int i=580; i <= 699; i++)
			pumsToEd[i] = 23;

		//TRANSPORT
		for (int i=400; i <= 439; i++)
			pumsToEd[i] = 24;

		//WHOLESALE TRADE
		for (int i=500; i <= 579; i++)
			pumsToEd[i] = 25;

		//UNEMPLOYED
		pumsToEd[0] = 26;
    }

    // return the IncomeSize category index given the label.
	public int getIndustryIndex(String industryLabel) {

		int returnValue = -1;

		for (int i=0; i < edIndustryLabels.length; i++) {
			if ( industryLabel.equalsIgnoreCase( edIndustryLabels[i] ) ) {
				returnValue = i;
				break;
			}
		}

		return returnValue;
	}
}

