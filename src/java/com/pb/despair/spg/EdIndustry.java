package com.pb.despair.spg;

import java.io.File;
import java.io.IOException;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;


public class EdIndustry {

    int[] pumsToEd = new int[1000];
    
    float[] edIndustryEmployment;
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
    
    
    public EdIndustry () {

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
	
	
	// return the array of regional industry employment from the named file
	public float[] getRegionalIndustryEmployment( String fileName ) {
	 
		// read the ED regional employment by industry output file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		// this table has one row of employment totals for each industry
		String[] tempEdIndustryLabels = table.getColumnAsString(1);
		float[] tempEdIndustryEmployment = table.getColumnAsFloat(2);
		edIndustryEmployment = new float[tempEdIndustryLabels.length + 1];
		
		for (int i=0; i < tempEdIndustryLabels.length; i++) {
	    	int industryIndex = getIndustryIndex( tempEdIndustryLabels[i] );
			edIndustryEmployment[industryIndex] = tempEdIndustryEmployment[i];
		}
		edIndustryEmployment[tempEdIndustryLabels.length] = 0.0f;
		
		// establish the correspondence table between pums codes and ED industry categories
		pumsIndustryToEdIndustry();

		return edIndustryEmployment;
		
	}
	
	
	// define the pums/ed industry index correspondence table
    private void pumsIndustryToEdIndustry () {
    	
    	int industryIndex = 0;
    	
        
        //ACCOMMODATIONS
    	industryIndex = getIndustryIndex("ACCOMMODATIONS");
		for (int i=762; i <= 770; i++)
			pumsToEd[i] = industryIndex;
        
        //AGRICULTURE AND MINING
    	industryIndex = getIndustryIndex("AGRICULTURE AND MINING");
		for (int i=1; i <= 30; i++)
			pumsToEd[i] = industryIndex;
		for (int i=32; i <= 59; i++)
			pumsToEd[i] = industryIndex;
		
		//COMMUNICATIONS AND UTILITIES
    	industryIndex = getIndustryIndex("COMMUNICATIONS AND UTILITIES");
		for (int i=440; i <= 499; i++)
			pumsToEd[i] = industryIndex;

		//CONSTRUCTION
    	industryIndex = getIndustryIndex("CONSTRUCTION");
		for (int i=60; i <= 99; i++)
			pumsToEd[i] = industryIndex;

		//ELECTRONICS AND INSTRUMENTS-Light Industry
    	industryIndex = getIndustryIndex("ELECTRONICS AND INSTRUMENTS-Light Industry");
		for (int i=322; i <= 330; i++)
			pumsToEd[i] = industryIndex;
		for (int i=371; i <= 380; i++)
			pumsToEd[i] = industryIndex;

		//FIRE BUSINESS AND PROFESSIONAL SERVICES
    	industryIndex = getIndustryIndex("FIRE BUSINESS AND PROFESSIONAL SERVICES");
		for (int i=700; i <= 741; i++)
		    pumsToEd[i] = industryIndex;
		for (int i=882; i <= 899; i++)
			pumsToEd[i] = industryIndex;
		pumsToEd[841] = industryIndex;

		//FOOD PRODUCTS-Heavy Industry
    	industryIndex = getIndustryIndex("FOOD PRODUCTS-Heavy Industry");
		for (int i=100; i <= 110; i++)
			pumsToEd[i] = industryIndex;

		//FOOD PRODUCTS-Light Industry
    	industryIndex = getIndustryIndex("FOOD PRODUCTS-Light Industry");
		for (int i=111; i <= 129; i++)
			pumsToEd[i] = industryIndex;
		
		//FORESTRY AND LOGGING
    	industryIndex = getIndustryIndex("FORESTRY AND LOGGING");
		pumsToEd[31] = industryIndex;
		pumsToEd[230] = industryIndex;

		//GOVERNMENT ADMINISTRATION
    	industryIndex = getIndustryIndex("GOVERNMENT ADMINISTRATION");
		for (int i=900; i <= 999; i++)
			pumsToEd[i] = industryIndex;
		
		//HEALTH SERVICES-Hospital
    	industryIndex = getIndustryIndex("HEALTH SERVICES-Hospital");
		for (int i=833; i <= 839; i++)
			pumsToEd[i] = industryIndex;
		pumsToEd[831] = industryIndex;
		
		//HEALTH SERVICES-Institutional
    	industryIndex = getIndustryIndex("HEALTH SERVICES-Institutional");
		pumsToEd[832] = industryIndex;
		
		//HEALTH SERVICES-Office
    	industryIndex = getIndustryIndex("HEALTH SERVICES-Office");
		for (int i=812; i <= 830; i++)
			pumsToEd[i] = industryIndex;
		pumsToEd[840] = industryIndex;
		
		//HIGHER EDUCATION
    	industryIndex = getIndustryIndex("HIGHER EDUCATION");
		pumsToEd[850] = industryIndex;
		pumsToEd[851] = industryIndex;
		
		//HOMEBASED SERVICES
    	industryIndex = getIndustryIndex("HOMEBASED SERVICES");
		pumsToEd[761] = industryIndex;
		
		//LOWER EDUCATION
    	industryIndex = getIndustryIndex("LOWER EDUCATION");
		for (int i=842; i <= 849; i++)
			pumsToEd[i] = industryIndex;
		
		//LUMBER AND WOOD PRODUCTS-Heavy Industry
    	industryIndex = getIndustryIndex("LUMBER AND WOOD PRODUCTS-Heavy Industry");
		for (int i=231; i <= 241; i++)
			pumsToEd[i] = industryIndex;
		
		//OTHER DURABLES-Heavy Industry
    	industryIndex = getIndustryIndex("OTHER DURABLES-Heavy Industry");
		for (int i=251; i <= 260; i++)
			pumsToEd[i] = industryIndex;
		for (int i=262; i <= 280; i++)
			pumsToEd[i] = industryIndex;
		for (int i=282; i <= 300; i++)
			pumsToEd[i] = industryIndex;
		for (int i=302; i <= 320; i++)
			pumsToEd[i] = industryIndex;
		for (int i=331; i <= 340; i++)
			pumsToEd[i] = industryIndex;
		for (int i=342; i <= 349; i++)
			pumsToEd[i] = industryIndex;
		for (int i=351; i <= 369; i++)
			pumsToEd[i] = industryIndex;
		
		//OTHER DURABLES-Light Industry
    	industryIndex = getIndustryIndex("OTHER DURABLES-Light Industry");
		for (int i=242; i <= 250; i++)
			pumsToEd[i] = industryIndex;
		for (int i=381; i <= 399; i++)
			pumsToEd[i] = industryIndex;
		pumsToEd[261] = industryIndex;
		pumsToEd[281] = industryIndex;
		pumsToEd[301] = industryIndex;
		pumsToEd[321] = industryIndex;
		pumsToEd[341] = industryIndex;
		pumsToEd[350] = industryIndex;
		pumsToEd[370] = industryIndex;
		
		//OTHER NON-DURABLES-Heavy Industry
    	industryIndex = getIndustryIndex("OTHER NON-DURABLES-Heavy Industry");
		for (int i=141; i <= 150; i++)
			pumsToEd[i] = industryIndex;
		for (int i=180; i <= 220; i++)
			pumsToEd[i] = industryIndex;
		pumsToEd[132] = industryIndex;
		
		//OTHER NON-DURABLES-Light Industry
    	industryIndex = getIndustryIndex("OTHER NON-DURABLES-Light Industry");
		for (int i=133; i <= 140; i++)
			pumsToEd[i] = industryIndex;
		for (int i=151; i <= 159; i++)
			pumsToEd[i] = industryIndex;
		for (int i=171; i <= 179; i++)
			pumsToEd[i] = industryIndex;
		for (int i=221; i <= 229; i++)
			pumsToEd[i] = industryIndex;
		pumsToEd[130] = industryIndex;
		pumsToEd[131] = industryIndex;
		
		//PERSONAL AND OTHER SERVICES AND AMUSEMENTS
    	industryIndex = getIndustryIndex("PERSONAL AND OTHER SERVICES AND AMUSEMENTS");
		for (int i=742; i <= 760; i++)
			pumsToEd[i] = industryIndex;
		for (int i=771; i <= 811; i++)
			pumsToEd[i] = industryIndex;
		for (int i=852; i <= 881; i++)
			pumsToEd[i] = industryIndex;

		//PULP AND PAPER-Heavy Industry
    	industryIndex = getIndustryIndex("PULP AND PAPER-Heavy Industry");
		for (int i=160; i <= 170; i++)
			pumsToEd[i] = industryIndex;

		//RETAIL TRADE
    	industryIndex = getIndustryIndex("RETAIL TRADE");
		for (int i=580; i <= 699; i++)
			pumsToEd[i] = industryIndex;

		//TRANSPORT
    	industryIndex = getIndustryIndex("TRANSPORT");
		for (int i=400; i <= 439; i++)
			pumsToEd[i] = industryIndex;

		//WHOLESALE TRADE
    	industryIndex = getIndustryIndex("WHOLESALE TRADE");
		for (int i=500; i <= 579; i++)
			pumsToEd[i] = industryIndex;

		//UNEMPLOYED
    	industryIndex = getIndustryIndex("UNEMPLOYED");
		pumsToEd[0] = industryIndex;
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

