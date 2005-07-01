package com.pb.tlumip.model;


public class IncomeSizeOld {

    String[] incomeSizeLabels = {
		"HH0to5k0to2",
		"HH0to5k2plus",
		"HH5to10k0to2",
		"HH5to10k2plus",
		"HH10to15k0to2",
		"HH10to15k2plus",
		"HH15to20k0to2",
		"HH15to20k2plus",
		"HH20to30k0to2",
		"HH20to30k2plus",
		"HH30to40k0to2",
		"HH30to40k2plus",
		"HH40to50k0to2",
		"HH40to50k2plus",
		"HH50to70k0to2",
		"HH50to70k2plus",
		"HH70kUp0to2",
		"HH70kUp2plus"
    };
    
    
    
    public IncomeSizeOld () {
    }

    

	// return the number of HH income/HH size categories.    
	public int getNumberIncomeSizes() {
		return incomeSizeLabels.length;
	}



	// return the IncomeSize category index given the label.    
	public int getIncomeSizeIndex(String incomeSizeLabel) {
	    
		int returnValue = -1;
	    
		for (int i=0; i < incomeSizeLabels.length; i++) {
			if ( incomeSizeLabel.equalsIgnoreCase( incomeSizeLabels[i] ) ) {
				returnValue = i;
				break;
			}
		}
		
		return returnValue;
	}



	// return the IncomeSize category index given the pums IncomeSize code
	// from the pums person record OCCUP field.    
	public int getIncomeSize(int income, int hhSize) {

	    int returnValue=0;

		
	    // define incomeSize indices for income and hh size ranges.
		if ( income < 5000 ) {
		    if ( hhSize < 2 )
		        returnValue = 0;
		    else
		        returnValue = 1;
		}
		else if ( income < 10000 ) {
			if ( hhSize < 2 )
				returnValue = 2;
			else
				returnValue = 3;
		}
		else if ( income < 15000 ) {
			if ( hhSize < 2 )
				returnValue = 4;
			else
				returnValue = 5;
		}
		else if ( income < 20000 ) {
			if ( hhSize < 2 )
				returnValue = 6;
			else
				returnValue = 7;
		}
		else if ( income < 30000 ) {
			if ( hhSize < 2 )
				returnValue = 8;
			else
				returnValue = 9;
		}
		else if ( income < 40000 ) {
			if ( hhSize < 2 )
				returnValue = 10;
			else
				returnValue = 11;
		}
		else if ( income < 50000 ) {
			if ( hhSize < 2 )
				returnValue = 12;
			else
				returnValue = 13;
		}
		else if ( income < 70000 ) {
			if ( hhSize < 2 )
				returnValue = 14;
			else
				returnValue = 15;
		}
		else {
			if ( hhSize < 2 )
				returnValue = 16;
			else
				returnValue = 17;
		}

		return returnValue;
	}



	// return the IncomeSize category label given the index.    
	public String getIncomeSizeLabel(int incomeSizeIndex) {
		return incomeSizeLabels[incomeSizeIndex];
	}



	// return all the IncomeSize category labels.    
	public String[] getIncomeSizeLabels() {
		return incomeSizeLabels;
	}

}

