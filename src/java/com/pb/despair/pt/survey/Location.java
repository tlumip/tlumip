//a class for activity locations, currently holds x and y coordinates
//
// In survey package:A class library for travel survey data
// jf 8/00

package com.pb.despair.pt.survey;
import java.io.*;


	//location class
public class Location{
	public long longid;
	public double xCoordinate;
	public double yCoordinate;
	public long taz;
	
	public void setlongid(long id){
		longid=id;
	}
	public long getlongid(){
		return longid;
	}
	public void setXCoordinate(double x){
		xCoordinate=x;
	}
	public double getXCoordinate(){
		return xCoordinate;
	}
	public void setYCoordinate(double y){
		yCoordinate=y;
	}
	public double getYCoordinate(){
		return yCoordinate;
	}        
	
	public void print(PrintWriter pw){
		pw.print(xCoordinate+" "+yCoordinate+" "+
			taz+" ");
	}		
}
 