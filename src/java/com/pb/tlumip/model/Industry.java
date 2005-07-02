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


public class Industry {

    String[] industryLabels = {
		"ACCOMMODATIONS",
		"AGRICULTURE AND MINING-Agriculture",
        "AGRICULTURE AND MINING-Office",
		"COMMUNICATIONS AND UTILITIES-Light Industry",
        "COMMUNICATIONS AND UTILITIES-Office",
		"CONSTRUCTION",
        "ELECTRONICS AND INSTRUMENTS-Light Industry",
        "ELECTRONICS AND INSTRUMENTS-Office",
		"FIRE BUSINESS AND PROFESSIONAL SERVICES",
		"FOOD PRODUCTS-Heavy Industry",
		"FOOD PRODUCTS-Light Industry",
        "FOOD PRODUCTS-Office",
		"FORESTRY AND LOGGING",
		"GOVERNMENT ADMINISTRATION-Government Support",
        "GOVERNMENT ADMINISTRATION-Office",
		"HEALTH SERVICES-Hospital",
		"HEALTH SERVICES-Institutional",
		"HEALTH SERVICES-Office",
		"HIGHER EDUCATION",
		"HOMEBASED SERVICES",
		"LOWER EDUCATION-Grade School",
        "LOWER EDUCATION-Office",
		"LUMBER AND WOOD PRODUCTS-Heavy Industry",
        "LUMBER AND WOOD PRODUCTS-Office",
		"OTHER DURABLES-Heavy Industry",
		"OTHER DURABLES-Light Industry",
        "OTHER DURABLES-Office",
		"OTHER NON-DURABLES-Heavy Industry",
		"OTHER NON-DURABLES-Light Industry",
        "OTHER NON-DURABLES-Office",
		"PERSONAL AND OTHER SERVICES AND AMUSEMENTS",
		"PULP AND PAPER-Heavy Industry",
        "PULP AND PAPER-Office",
		"RETAIL TRADE-Office",
        "RETAIL TRADE-Retail",
		"TRANSPORT-Depot",
        "TRANSPORT-Office",
		"WHOLESALE TRADE-Office",
        "WHOLESALE TRADE-Warehouse",
		"Capitalists",
        "GovInstitutions"
    };
    
    

    
    
    
    public Industry () {
    }

    

	// return the number of industry categories.
	public int getNumberIndustries() {
		return industryLabels.length;
	}



	// return the industry category label given the index.
	public String getIndustryLabel(int industryIndex) {
		return industryLabels[industryIndex];
	}



	// return all the industry category labels.
	public String[] getIndustryLabels() {
		return industryLabels;
	}




    // return the industy index given the label.
	public int getIndustryIndex(String industryLabel) {

		int returnValue = -1;

		for (int i=0; i < industryLabels.length; i++) {
			if ( industryLabel.equalsIgnoreCase( industryLabels[i] ) ) {
				returnValue = i;
				break;
			}
		}

		return returnValue;
	}
}

