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
package com.pb.tlumip.ts.transit;

import java.io.Serializable;
import java.util.ArrayList;



public class TrSegment implements Serializable {
	int rteIndex, link, an, bn;
	int ttf, ttf1, ttft;
	double dwf, dwt, us1, us2, us3;
	double lay, tdwt, tus1, tus2, tus3;
	boolean path, boardA, alightA, boardB, alightB, layover;

	TrSegment (int rteIndex, int an, int bn, ArrayList defaults, ArrayList tdefaults) {
		dwf    =  ((Double)defaults.get(0)).doubleValue();
		dwt    =  ((Double)defaults.get(1)).doubleValue();
		path   =  ((Boolean)defaults.get(2)).booleanValue();
		ttf    =  ((Integer)defaults.get(3)).intValue();
		ttf1   =  ((Integer)defaults.get(4)).intValue();
		ttft   =  ((Integer)defaults.get(5)).intValue();
		us1    =  ((Double)defaults.get(6)).doubleValue();
		us2    =  ((Double)defaults.get(7)).doubleValue();
		us3    =  ((Double)defaults.get(8)).doubleValue();
        boardA  =  ((Boolean)defaults.get(9)).booleanValue();
        alightA =  ((Boolean)defaults.get(10)).booleanValue();
        boardB  =  ((Boolean)defaults.get(11)).booleanValue();
        alightB =  ((Boolean)defaults.get(12)).booleanValue();
		layover = ((Boolean)defaults.get(13)).booleanValue();
		lay    =  ((Double)tdefaults.get(0)).doubleValue();
		tdwt   =  ((Double)tdefaults.get(1)).doubleValue();
		tus1   =  ((Double)tdefaults.get(2)).doubleValue();
		tus2   =  ((Double)tdefaults.get(3)).doubleValue();
		tus3   =  ((Double)tdefaults.get(4)).doubleValue();

		link = -1;

		this.an = an;
		this.bn = bn;
        this.rteIndex = rteIndex;
	}


	public TrSegment segmentCopy (TrSegment ts, TrRoute tr) {

		TrSegment tsNew = new TrSegment (ts.rteIndex, ts.an, ts.bn, tr.defaults, tr.tdefaults);

		tsNew.dwf     = ts.dwf;
		tsNew.dwt     = ts.dwt;
		tsNew.path    = ts.path;
		tsNew.ttf     = ts.ttf;
		tsNew.ttf1    = ts.ttf1;
		tsNew.ttft    = ts.ttft;
		tsNew.us1     = ts.us1;
		tsNew.us2     = ts.us2;
		tsNew.us3     = ts.us3;
        tsNew.boardA   = ts.boardA;
        tsNew.alightA  = ts.alightA;
        tsNew.boardB   = ts.boardB;
        tsNew.alightB  = ts.alightB;
		tsNew.layover = ts.layover;
		tsNew.lay     = ts.lay;
		tsNew.tdwt    = ts.tdwt;
		tsNew.tus1    = ts.tus1;
		tsNew.tus2    = ts.tus2;
		tsNew.tus3    = ts.tus3;

		tsNew.link = -1;

		return tsNew;
	}
	
	
	public void setDwf( double dwf ) {
		this.dwf = dwf; 
	}
	
	public void setDwt( double dwt ) {
		this.dwt = dwt; 
	}
	
	public void setTdwt( double tdwt ) {
		this.tdwt = tdwt; 
	}
	
	public int getTtf() {
		return this.ttf; 
	}
	
	public double getDwf() {
		return this.dwf; 
	}
	
	public double getDwt() {
		return this.dwt; 
	}
	
	public double getTdwt() {
		return this.tdwt; 
	}
	
}
