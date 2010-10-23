/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.model;

import com.pb.common.util.ResourceUtil;
import com.pb.models.censusdata.SwIncomeSize;

import java.util.ResourceBundle;


public class IncomeSize extends SwIncomeSize {
    public static final String INCOME_SIZE_CONVERSION_PROPERTY = "spg.income.size.conversion.factor";

    private final double incomeSizeConversion;
    private final boolean conversionOn;

    String[] incomeSizeLabels = {
		"HH0to5k1to2",
		"HH0to5k3plus",
		"HH5to10k1to2",
		"HH5to10k3plus",
		"HH10to15k1to2",
		"HH10to15k3plus",
		"HH15to20k1to2",
		"HH15to20k3plus",
		"HH20to30k1to2",
		"HH20to30k3plus",
		"HH30to40k1to2",
		"HH30to40k3plus",
		"HH40to50k1to2",
		"HH40to50k3plus",
		"HH50to70k1to2",
		"HH50to70k3plus",
		"HH70kUp1to2",
		"HH70kUp3plus"
    };
    
    
    
    public IncomeSize (double incomeSizeConversion) {
        super();
        this.incomeSizeConversion = incomeSizeConversion;
        conversionOn = incomeSizeConversion != 1.0;
    }

    public IncomeSize (ResourceBundle rb) {
        this(ResourceUtil.getDoubleProperty(rb,INCOME_SIZE_CONVERSION_PROPERTY));
    }

    

    // return all the IncomeSize category labels.    
    public String[] getIncomeSizeLabels() {
        return incomeSizeLabels;
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

    
    // return the IncomeSize category label given the index.    
    public String getIncomeSizeLabel(int incomeSizeIndex) {
        return incomeSizeLabels[incomeSizeIndex];
    }


    public int getIncomeSize(int income, int hhSize) {
        return conversionOn ? getIncomeSizeConverted(income,hhSize) : getIncomeSizeStraight(income,hhSize);
    }
    
    
	// return the IncomeSize category index given the pums IncomeSize code
	// from the pums person record OCCUP field.    
	private int getIncomeSizeStraight(int income, int hhSize) {

	    int returnValue=0;

		
	    // define incomeSize indices for income and hh size ranges.
		if ( income < 5000 ) {
		    if ( hhSize < 3 )
		        returnValue = 0;
		    else
		        returnValue = 1;
		}
		else if ( income < 10000 ) {
			if ( hhSize < 3 )
				returnValue = 2;
			else
				returnValue = 3;
		}
		else if ( income < 15000 ) {
			if ( hhSize < 3 )
				returnValue = 4;
			else
				returnValue = 5;
		}
		else if ( income < 20000 ) {
			if ( hhSize < 3 )
				returnValue = 6;
			else
				returnValue = 7;
		}
		else if ( income < 30000 ) {
			if ( hhSize < 3 )
				returnValue = 8;
			else
				returnValue = 9;
		}
		else if ( income < 40000 ) {
			if ( hhSize < 3 )
				returnValue = 10;
			else
				returnValue = 11;
		}
		else if ( income < 50000 ) {
			if ( hhSize < 3 )
				returnValue = 12;
			else
				returnValue = 13;
		}
		else if ( income < 70000 ) {
			if ( hhSize < 3 )
				returnValue = 14;
			else
				returnValue = 15;
		}
		else {
			if ( hhSize < 3 )
				returnValue = 16;
			else
				returnValue = 17;
		}

		return returnValue;
	}

    private int getIncomeSizeConverted(int income, int hhSize) {
	    int returnValue=0;

	    // define incomeSize indices for income and hh size ranges.
		if ( income < Math.round(5000*incomeSizeConversion)) {
		    if ( hhSize < 3 )
		        returnValue = 0;
		    else
		        returnValue = 1;
		}
		else if ( income < Math.round(10000*incomeSizeConversion)) {
			if ( hhSize < 3 )
				returnValue = 2;
			else
				returnValue = 3;
		}
		else if ( income < Math.round(15000*incomeSizeConversion)) {
			if ( hhSize < 3 )
				returnValue = 4;
			else
				returnValue = 5;
		}
		else if ( income < Math.round(20000*incomeSizeConversion)) {
			if ( hhSize < 3 )
				returnValue = 6;
			else
				returnValue = 7;
		}
		else if ( income < Math.round(30000*incomeSizeConversion)) {
			if ( hhSize < 3 )
				returnValue = 8;
			else
				returnValue = 9;
		}
		else if ( income < Math.round(40000*incomeSizeConversion)) {
			if ( hhSize < 3 )
				returnValue = 10;
			else
				returnValue = 11;
		}
		else if ( income < Math.round(50000*incomeSizeConversion)) {
			if ( hhSize < 3 )
				returnValue = 12;
			else
				returnValue = 13;
		}
		else if ( income < Math.round(70000*incomeSizeConversion)) {
			if ( hhSize < 3 )
				returnValue = 14;
			else
				returnValue = 15;
		}
		else {
			if ( hhSize < 3 )
				returnValue = 16;
			else
				returnValue = 17;
		}

		return returnValue;
	}

}

