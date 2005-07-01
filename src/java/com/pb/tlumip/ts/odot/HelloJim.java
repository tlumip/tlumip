/*
 * Created on Sep 15, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.pb.tlumip.ts.odot;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author hicksji
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class HelloJim {

    public HelloJim () {
	}


	public static void main ( String args ) {
	
	    writeArgs(args);
	    
	}
	
	
	public static String writeArgs (String args) {
	 
	    
	    PrintWriter pw=null;
        try {
            pw = new PrintWriter( new FileWriter("/tmp/R.out"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String returnValue = "java writing argunent passed in: " + args;
        
	    pw.println( returnValue);
	    pw.close();
	    
	    return returnValue;
	    
	}
}
