package com.pb.despair.model;


public class Occupation {

    int[] pumsToOcc = new int[1000];
    
    String[] occupationLabels = {
		"0_Unemployed",
		"1_ManPro",
		"1A_Health",
		"2_PstSec",
		"3_OthTchr",
		"4_OthP&T",
		"5_RetSls",
		"6_OthR&C",
		"7_NonOfc"
    };
    
    
    
    public Occupation () {

		// establish the correspondence table between pums codes and occupation categories
		pumsToOccupation();

    }

    

	// return the number of occupation categories.    
	public int getNumberOccupations() {
		return occupationLabels.length;
	}



	// return the occupation category index given the pums occupation code
	// from the pums person record OCCUP field.    
	public int getOccupation(int pumsOccupation) {
		return pumsToOcc[pumsOccupation];
	}



	// return the occupation category index given the label.    
	public int getOccupationIndex(String occupationLabel) {
	    int returnValue=0;
	    
		for (int i=0; i < occupationLabels.length; i++) {
		    if ( occupationLabel.equalsIgnoreCase( occupationLabels[i] ) ) {
		        returnValue = i;
		        break;
		    }
		}
		
		return returnValue;
	}



	// return the occupation category label given the index.    
	public String getOccupationLabel(int pumsOccupation) {
		return occupationLabels[pumsOccupation];
	}



	// return all the occupation category labels.    
	public String[] getOccupationLabels() {
		return occupationLabels;
	}


	// define the pums/occupation index correspondence table
    private void pumsToOccupation () {
        
		//0_Unemployed
		pumsToOcc[0] = 0;
		
		//7_NonOfc
		for (int i=1; i < 1000; i++)
			pumsToOcc[i] = 8;
		
        //1_ManPro
		for (int i=1; i <= 82; i++)
			pumsToOcc[i] = 1;
        
        //1A_Health
		for (int i=83; i <= 112; i++)
			pumsToOcc[i] = 2;
		
		//2_PstSec
		for (int i=113; i <= 154; i++)
			pumsToOcc[i] = 3;

		//3_OthTchr
		for (int i=155; i <= 162; i++)
			pumsToOcc[i] = 4;

		//4_OthP&T
		for (int i=163; i <= 262; i++)
			pumsToOcc[i] = 5;

		//5_RetSls
		for (int i=263; i <= 282; i++)
		    pumsToOcc[i] = 6;

		//6_OthR&C
		for (int i=283; i <= 402; i++)
			pumsToOcc[i] = 7;
    }
    
}

