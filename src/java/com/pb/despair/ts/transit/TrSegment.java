package com.pb.despair.ts.transit;

import java.io.Serializable;
import java.util.ArrayList;



public class TrSegment implements Serializable {
	int link, an, bn;
	int ttf, ttf1, ttft;
	double dwf, dwt, us1, us2, us3;
	double lay, tdwt, tus1, tus2, tus3;
	boolean path, board, alight, layover;

	TrSegment (int an, int bn, ArrayList defaults, ArrayList tdefaults) {
		dwf    =  ((Double)defaults.get(0)).doubleValue();
		dwt    =  ((Double)defaults.get(1)).doubleValue();
		path   =  ((Boolean)defaults.get(2)).booleanValue();
		ttf    =  ((Integer)defaults.get(3)).intValue();
		ttf1   =  ((Integer)defaults.get(4)).intValue();
		ttft   =  ((Integer)defaults.get(5)).intValue();
		us1    =  ((Double)defaults.get(6)).doubleValue();
		us2    =  ((Double)defaults.get(7)).doubleValue();
		us3    =  ((Double)defaults.get(8)).doubleValue();
		board  =  ((Boolean)defaults.get(9)).booleanValue();
		alight =  ((Boolean)defaults.get(10)).booleanValue();
		layover = ((Boolean)defaults.get(11)).booleanValue();
		lay    =  ((Double)tdefaults.get(0)).doubleValue();
		tdwt   =  ((Double)tdefaults.get(1)).doubleValue();
		tus1   =  ((Double)tdefaults.get(2)).doubleValue();
		tus2   =  ((Double)tdefaults.get(3)).doubleValue();
		tus3   =  ((Double)tdefaults.get(4)).doubleValue();

		link = 0;

		this.an = an;
		this.bn = bn;
	}


	public TrSegment segmentCopy (TrSegment ts, TrRoute tr) {

		TrSegment tsNew = new TrSegment (ts.an, ts.bn, tr.defaults, tr.tdefaults);

		tsNew.dwf     = ts.dwf;
		tsNew.dwt     = ts.dwt;
		tsNew.path    = ts.path;
		tsNew.ttf     = ts.ttf;
		tsNew.ttf1    = ts.ttf1;
		tsNew.ttft    = ts.ttft;
		tsNew.us1     = ts.us1;
		tsNew.us2     = ts.us2;
		tsNew.us3     = ts.us3;
		tsNew.board   = ts.board;
		tsNew.alight  = ts.alight;
		tsNew.layover = ts.layover;
		tsNew.lay     = ts.lay;
		tsNew.tdwt    = ts.tdwt;
		tsNew.tus1    = ts.tus1;
		tsNew.tus2    = ts.tus2;
		tsNew.tus3    = ts.tus3;

		tsNew.link = 0;

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
