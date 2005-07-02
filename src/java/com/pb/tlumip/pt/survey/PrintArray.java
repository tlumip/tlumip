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
// a class to print an array to an open OutTextFile
//
//
package com.pb.tlumip.pt.survey;
import java.io.*;

		
public class PrintArray{

	//constructor 
	public PrintArray(String[] labels, int[] values, int minimumValue, PrintWriter file) throws
		IOException{
	
		if(labels.length != values.length){
			System.out.println("PrintArray error: label array size not equal to values array size");
			System.exit(1);
		}

		for(int i=0;i<values.length;++i)
			if(values[i]>=minimumValue)
				file.println(i + ":"+labels[i] + "    " +values[i]);
	}
}