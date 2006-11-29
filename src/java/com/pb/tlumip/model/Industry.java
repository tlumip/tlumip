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

import com.pb.common.datafile.TableDataSet;
import com.pb.models.censusdata.SwIndustry;
import com.pb.models.reference.IndustryOccupationSplitIndustryReference;


public class Industry extends SwIndustry {

    public Industry (String corespondenceFile, String year, IndustryOccupationSplitIndustryReference ref) {
        super(corespondenceFile, year, ref);
    }

    

    // the Oregon model uses an industry employment data file with one column of
    // employment dollars for an implied year.  There is a separate file for every
    // simulation year.  Therefore the year passed in to this method is meaningless
    // and the column index should be set to 2 for the TableDataSet that will be created.
    public float[] getIndustryEmployment( String fileName, String columnIndicator) {

        TableDataSet table = getIndustryEmploymentTableData( fileName );
        return getIndustryEmploymentForColumn( table, 2 );
        
    }

}

