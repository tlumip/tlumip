// a class to print an array to an open OutTextFile
//
//
package com.pb.despair.pt.survey;
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