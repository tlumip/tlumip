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
/* Generated by Together */

package com.pb.tlumip.ha;

import java.util.Hashtable;
import java.util.Iterator;

/** <p> This class is a list of possible household types. */
public class HouseholdPossibilities {
    public double sumWeights(Household h) {
        double sum = 0;
        Iterator it = individualPossibilities.keySet().iterator();
        while (it.hasNext()) {
            HouseholdCategory hc = (HouseholdCategory)it.next();
            if (hc.householdFits(h)) {
                sum += ((Double)individualPossibilities.get(hc)).doubleValue();
            }
        }
        return sum;
    }

    public double sumWeights(Household h, Person p) {
        double sum = 0;
        Iterator it = individualPossibilities.keySet().iterator();
        while (it.hasNext()) {
            HouseholdCategory hc = (HouseholdCategory)it.next();
            if (hc.householdFits(h, p)) {
                sum += ((Double)individualPossibilities.get(hc)).doubleValue();
            }
        }
        return sum;
    }

    public void add(HouseholdCategory householdCategory, double weight) {
        individualPossibilities.put(householdCategory, new Double(weight));
    }

    public HouseholdCategory randomCategory() {
        Iterator it = individualPossibilities.keySet().iterator();
        double sum = 0;
        HouseholdCategory hc=null;
        while (it.hasNext()) {
            hc = (HouseholdCategory)it.next();
            sum += ((Double)individualPossibilities.get(hc)).doubleValue();
        }
        double selector = Math.random() * sum;
        sum = 0;
        it = individualPossibilities.keySet().iterator();
        while (sum <= selector) {
            hc = (HouseholdCategory)it.next();
            sum+= ((Double)individualPossibilities.get(hc)).doubleValue();
        }
        return hc;
    }

    /**
     * 
     */
    private Hashtable individualPossibilities = new Hashtable(10);
}
