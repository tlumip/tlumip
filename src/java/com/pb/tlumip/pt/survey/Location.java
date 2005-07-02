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
//a class for activity locations, currently holds x and y coordinates
//
// In survey package:A class library for travel survey data
// jf 8/00

package com.pb.tlumip.pt.survey;
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
 