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

import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.ShortestPathTreeH;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.log4j.Logger;



public class TrRoute implements Serializable {

	protected static transient Logger logger = Logger.getLogger(TrRoute.class);

    static int filenum=0;
    static final boolean WRITE_NEW_ROUTE_FILES = false;
    static final char CHANGE_MODE_CHARACTER_FROM = 'b';
    static final char CHANGE_MODE_CHARACTER_TO = 'i';
//    static final char CHANGE_MODE_CHARACTER_FROM = 'z';
//    static final char CHANGE_MODE_CHARACTER_TO = 'z';
    
    String newRouteFileName = "";
    
    
    int maxRoutes=0;
	int lineCount= -1, linkCount=0, totalLinkCount=0;
	int an, bn;
    String[] line;
    String[] description;
    String[] routeType;
	char[] mode;
	int[] vehType;
	double[] headway, speed, ut1, ut2, ut3;
	ArrayList[] transitPath;

	// default segment values
	String[] keyWords = { "dwf", "dwt", "path", "ttfl", "ttft", "ttf", "us1", "us2", "us3", "board", "alight" };
	String[] tkeyWords = {  "lay", "tdwt", "tus1", "tus2", "tus3" };
	ArrayList defaults;
	ArrayList tdefaults;

    


	public TrRoute (int maxRoutes) {
		line = new String[maxRoutes];
        description = new String[maxRoutes];
        routeType = new String[maxRoutes];
		mode = new char[maxRoutes];
		vehType = new int[maxRoutes];
		headway = new double[maxRoutes];
		speed = new double[maxRoutes];
		ut1 = new double[maxRoutes];
		ut2 = new double[maxRoutes];
		ut3 = new double[maxRoutes];
		transitPath = new ArrayList[maxRoutes];
		for (int i=0; i < maxRoutes; i++)
			transitPath[i] = new ArrayList(500);

        this.maxRoutes = maxRoutes;

        defaults = new ArrayList();
        initDefaults();
        
        tdefaults = new ArrayList();
        initTempDefaults();

    }


    void initDefaults () {
        defaults.add(0, Double.valueOf("0.0"));
        defaults.add(1, Double.valueOf("0.01"));
        defaults.add(2, Boolean.valueOf("true"));
        defaults.add(3, Integer.valueOf("0"));
        defaults.add(4, Integer.valueOf("0"));
        defaults.add(5, Integer.valueOf("0"));
        defaults.add(6, Double.valueOf("0.0"));
        defaults.add(7, Double.valueOf("0.0"));
        defaults.add(8, Double.valueOf("0.0"));
        defaults.add(9, Boolean.valueOf("true"));
        defaults.add(10, Boolean.valueOf("true"));
        defaults.add(11, Boolean.valueOf("false"));
        defaults.add(12, Boolean.valueOf("false"));
        defaults.add(13, Boolean.valueOf("false"));
    }

    void resetDefaults () {
        defaults.set(0, Double.valueOf("0.0"));
        defaults.set(1, Double.valueOf("0.01"));
        defaults.set(2, Boolean.valueOf("true"));
        defaults.set(3, Integer.valueOf("0"));
        defaults.set(4, Integer.valueOf("0"));
        defaults.set(5, Integer.valueOf("0"));
        defaults.set(6, Double.valueOf("0.0"));
        defaults.set(7, Double.valueOf("0.0"));
        defaults.set(8, Double.valueOf("0.0"));
        defaults.set(9, Boolean.valueOf("true"));
        defaults.set(10, Boolean.valueOf("true"));
        defaults.set(11, Boolean.valueOf("false"));
        defaults.set(12, Boolean.valueOf("false"));
        defaults.set(13, Boolean.valueOf("false"));
    }

    void initTempDefaults () {
        tdefaults.add(0, Double.valueOf("0.0"));
        tdefaults.add(1, Double.valueOf("0.0"));
        tdefaults.add(2, Double.valueOf("0.0"));
        tdefaults.add(3, Double.valueOf("0.0"));
        tdefaults.add(4, Double.valueOf("0.0"));
    }

    void resetTempDefaults () {
        tdefaults.set(0, Double.valueOf("0.0"));
        tdefaults.set(1, Double.valueOf("0.0"));
        tdefaults.set(2, Double.valueOf("0.0"));
        tdefaults.set(3, Double.valueOf("0.0"));
        tdefaults.set(4, Double.valueOf("0.0"));
    }


    public void readTransitRoutes ( NetworkHandlerIF nh, String fileName ) {
        int recNumber = 0;
        PrintWriter newRoutesWriter = null;
        
        // assume routes are for intracity service if this method is used because of a single route file.
        String routeType = "intracity";
        
        try {
            
            if ( WRITE_NEW_ROUTE_FILES ) {
                newRouteFileName = fileName.substring(0, fileName.indexOf('.')) + "_" + "new" + fileName.substring(fileName.indexOf('.'));
                newRoutesWriter = new PrintWriter ( new BufferedWriter ( new FileWriter (newRouteFileName) ) ) ;
            }
            
            BufferedReader in = new BufferedReader( new FileReader(fileName) );
            String s = new String();
            while ((s = in.readLine()) != null) {
                recNumber++;

                // skip empty records
                if (s.length() > 0) {
                    // skip comment records and the data type record
                    if (s.charAt(0) == 'c' || s.charAt(0) == 't') {
                        if ( WRITE_NEW_ROUTE_FILES )
                            newRoutesWriter.printf("%s\n", s);
                    }
                    // process transit line header records
                    else if (s.charAt(0) == 'a') {
                        if ( WRITE_NEW_ROUTE_FILES )
                            newRoutesWriter.printf("%s\n", s);

                        resetDefaults();
                        resetTempDefaults();

                        lineCount++;
                        totalLinkCount += linkCount;
                        parseHeader(s, routeType);
                        linkCount = 0;
                        an = -1;
                        bn = -1;
                    }
                    // process transit line sequence records
                    else if (s.charAt(0) == ' ') {
                        
                        parseSegments(nh, s, newRoutesWriter);
                        
                        if ( WRITE_NEW_ROUTE_FILES ) {
                            if ( ! (Boolean)defaults.get(2) )
                                newRoutesWriter.printf("%s\n", s);
                        }
                            
                    }
                    //  any other record type is invalid, so exit.
                    else {
                        logger.error ("Transit line file record number " + recNumber + " has an invalid " + s.charAt(0) + " first character.");
                        if (s.charAt(0) == 'd' || s.charAt(0) == 'm')
                            logger.error (s.charAt(0) + " is not a supported update code.");
                        logger.error ("Records may only begin with c, t, a, or blank.");
                        logger.error ("Program exiting while reading transit line file");
                        throw new RuntimeException();
                    }
                }
                else {
                    
                    if ( WRITE_NEW_ROUTE_FILES ) {
                        newRoutesWriter.printf("\n");
                    }

                }
            }
            
            if ( WRITE_NEW_ROUTE_FILES )
                newRoutesWriter.close();
            
        } catch (Exception e) {
            logger.error ("IO Exception caught reading transit route file: " + fileName + ", record number=" + recNumber, e);
        }

        lineCount++;
        logger.info (recNumber + " transit line file records read.");
        logger.info (lineCount + " transit lines found.");
        logger.info (totalLinkCount + " total transit links found in all transit routes.\n");
  
    }


    /**
     * use this method if two or more route files should be combined to build transit network.
     */
    public void readTransitRoutes ( NetworkHandlerIF nh, String[] fileNames, String[] rteTypes ) {
        
        PrintWriter newRoutesWriter = null;

        String s1 = String.format(" %c ", CHANGE_MODE_CHARACTER_FROM); 
        String s2 = String.format(" %c ", CHANGE_MODE_CHARACTER_TO);

        for ( int i=0; i < fileNames.length; i++ ) {
            
            int recNumber = 0;
            int tempLineCount = 0;

            try {
                
                if ( WRITE_NEW_ROUTE_FILES ) {
                    newRouteFileName = fileNames[i].substring(0, fileNames[i].indexOf('.')) + "_" + "new" + fileNames[i].substring(fileNames[i].indexOf('.'));
                    newRoutesWriter = new PrintWriter ( new BufferedWriter ( new FileWriter (newRouteFileName) ) ) ;
                }
                
                BufferedReader in = new BufferedReader( new FileReader(fileNames[i]) );
                String s = new String();
                while ((s = in.readLine()) != null) {
                    recNumber++;

                    // skip empty records
                    if (s.length() > 0) {
                        // skip comment records and the data type record
                        if (s.charAt(0) == 'c' || s.charAt(0) == 't') {
                            if ( WRITE_NEW_ROUTE_FILES )
                                newRoutesWriter.printf("%s\n", s);
                        }
                        // process transit line header records
                        else if (s.charAt(0) == 'a') {
                            if ( WRITE_NEW_ROUTE_FILES ) {
                                if ( s1 != s2 && s.indexOf(s1) >= 0 )
                                    s = s.replaceFirst( s1, s2 ); 
                                newRoutesWriter.printf("%s\n", s);
                            }

                            resetDefaults();
                            resetTempDefaults();

                            lineCount++;
                            tempLineCount++;
                            totalLinkCount += linkCount;
                            parseHeader(s, rteTypes[i]);
                            linkCount = 0;
                            an = -1;
                            bn = -1;
                        }
                        // process transit line sequence records
                        else if (s.charAt(0) == ' ') {
                            
                            parseSegments(nh, s, newRoutesWriter);
                            
                            if ( WRITE_NEW_ROUTE_FILES ) {
                                if ( ! (Boolean)defaults.get(2) )
                                    newRoutesWriter.printf("%s\n", s);
                            }
                                
                        }
                        //  any other record type is invalid, so exit.
                        else {
                            logger.error ("Program exiting while reading transit line file = " + fileNames[i]);
                            logger.error ("Transit line file record number " + recNumber + " has an invalid " + s.charAt(0) + " first character.");
                            if (s.charAt(0) == 'd' || s.charAt(0) == 'm')
                                logger.error (s.charAt(0) + " is not a supported update code.");
                            logger.error ("First character of records may only be c, t, a, or blank.");
                            throw new RuntimeException();
                        }
                    }
                    else {
                        
                        if ( WRITE_NEW_ROUTE_FILES ) {
                            newRoutesWriter.printf("\n");
                        }

                    }
                    
                }

                if ( WRITE_NEW_ROUTE_FILES )
                    newRoutesWriter.close();
                
            } catch (Exception e) {
                logger.error ("IO Exception caught reading transit route file: " + fileNames[i] + ", record number=" + recNumber, e);
            }

            logger.info ( String.format("finished reading %d records from transit line file %s, %d transit lines found.", recNumber, fileNames[i], tempLineCount) );

        }

        lineCount++;

        logger.info (lineCount + " total transit lines found in all files.");
        logger.info (totalLinkCount + " total transit links found in all transit routes.\n");

    }


	// parse the transit line data
	private void parseHeader (String InputString, String rteType) {

		// ignore first character
		String s = InputString.substring(1);
		
		String[] values = getHeaderValues(s);
        
		line[lineCount] = values[0];
		mode[lineCount] = values[1].charAt(0);
		vehType[lineCount] = Integer.parseInt(values[2]);
		headway[lineCount] = Double.parseDouble(values[3]);
		speed[lineCount] = Double.parseDouble(values[4]);
		description[lineCount] = values[5];
		ut1[lineCount] = Double.parseDouble(values[6]);
		ut2[lineCount] = Double.parseDouble(values[7]);
		ut3[lineCount] = Double.parseDouble(values[8]);
        
        routeType[lineCount] = rteType;
		
	}


	// parse the transit line segment data for the transit line itinerary
	private void parseSegments ( NetworkHandlerIF nh, String s, PrintWriter newRoutesWriter ) {
	    
		String keyWord=null, value=null, field=null;
		int stringPointer = 0;

        SegmentChecker segChecker = new SegmentChecker ( nh);
        
        String returnString;
        
		while (stringPointer < s.length()) {
            
            try {
                // get the next field from the input record; advance stringPointer
                field = getSegmentField (s, stringPointer);
                stringPointer += field.length();
                field = field.trim();
                if (field.length() == 0)
                    return;

                // get the keyWord,value pair from field -- (keyWord == null means value is a node)
                if (field.indexOf('=') == -1) {
                    keyWord = null;
                    value = field;
                    if (an == -1) {
                        an = Integer.parseInt(value);
                        if ( WRITE_NEW_ROUTE_FILES && (Boolean)defaults.get(2) )
                            newRoutesWriter.printf("dwt=+0.0 %s ", Integer.toString(an));
                        
                    }
                    else {
                        // if there is already a bn, set it to an and set temp ArrayLists to null.
                        if (bn != -1)
                            an = bn;
                        
                        bn = Integer.parseInt(value);

                    }

                    if (an != -1 && bn != -1) {
                        
                        // use this to handle board/alight flags changing after first an, before bn.
                        TrSegment seg = new TrSegment(lineCount, an, bn, defaults, tdefaults);

                        defaults.set(9,defaults.get(11));
                        defaults.set(10,defaults.get(12));
                        
                        transitPath[lineCount].add(linkCount++, seg);
                        resetTempDefaults();

                        
                        returnString = segChecker.checkSegment(this, seg, linkCount-1);
                        
                        if ( WRITE_NEW_ROUTE_FILES && (Boolean)defaults.get(2) ) {
                            if ( returnString == "" ) {
                                newRoutesWriter.printf("  %d", bn );
                            }
                            else {
                                newRoutesWriter.printf(" %s\n", returnString);
                            }
                        }
                        
                    }
                }
                else {
                    // check for keywords specifying default values in field
                    for (int i=0; i < keyWords.length; i++) {
                        value = keyWordScan (field, keyWords[i]);
                        if (value != null) {
                            keyWord = keyWords[i];
                            switch(i) {
                                case 0:
                                case 1:
                                    if (value.indexOf('<') != -1) {
                                        defaults.set(11, Boolean.valueOf("true"));
                                        defaults.set(12, Boolean.valueOf("false"));
                                        value = value.replace ('<', ' ');
                                    }
                                    else if (value.indexOf('>') != -1) {
                                        defaults.set(11, Boolean.valueOf("false"));
                                        defaults.set(12, Boolean.valueOf("true"));
                                        value = value.replace ('>', ' ');
                                    }
                                    else if (value.indexOf('#') != -1) {
                                        defaults.set(11, Boolean.valueOf("false"));
                                        defaults.set(12, Boolean.valueOf("false"));
                                        value = value.replace ('#', ' ');
                                    }
                                    else if (value.indexOf('+') != -1) {
                                        defaults.set(11, Boolean.valueOf("true"));
                                        defaults.set(12, Boolean.valueOf("true"));
                                        value = value.replace ('+', ' ');
                                    }
                                    else {
                                        defaults.set(11, Boolean.valueOf("true"));
                                        defaults.set(12, Boolean.valueOf("true"));
                                    }
                                    // if * is in the value field, set value to negative and it will get applied as a distance based rate later
                                    if (value.indexOf('*') != -1) {
                                        value = value.replace ('*', '-');
                                    }
                                    defaults.set(i, Double.valueOf(value));
                                    break;
                                case 2:
                                    if (value.equalsIgnoreCase("yes"))
                                        defaults.set(i, Boolean.valueOf("true"));
                                    else
                                        defaults.set(i, Boolean.valueOf("false"));
                                    break;
                                case 3:
                                case 4:
                                case 5:
                                    defaults.set(i, Integer.valueOf(value));
                                    break;
                                case 6:
                                case 7:
                                case 8:
                                    defaults.set(i, Double.valueOf(value));
                                    break;
                            }
                            
                            if ( WRITE_NEW_ROUTE_FILES ) {
                                if ( keyWord.equalsIgnoreCase("path") && value.equalsIgnoreCase("yes") )
                                    value = "no";
                                newRoutesWriter.printf(" %s=%s ", keyWord, value);
                            }

                            break;
                        }
                    }

                    // check for keywords specifying temporary values in field
                    if (keyWord == null) {
                        for (int i=0; i < tkeyWords.length; i++) {
                            value = keyWordScan (field, tkeyWords[i]);
                            if (value != null) {
                                keyWord = tkeyWords[i];
                                if ( i == 0 ) {
                                    TrSegment seg = new TrSegment(lineCount, an, bn, defaults, tdefaults);
                                    seg.layover = true;
                                    seg.lay = Double.parseDouble(value);
                                    transitPath[lineCount].add(linkCount++, seg);
                                    totalLinkCount += linkCount;

                                    returnString = segChecker.checkSegment(this, seg, linkCount-1);
                                    
                                    if ( WRITE_NEW_ROUTE_FILES && (Boolean)defaults.get(2) ) {
                                        if ( returnString != "" ) {
                                            newRoutesWriter.printf(" %s\n", returnString);
                                        }
                                    }
                                    
                                }
                                else if ( i == 1 ) {
                                    if (value.indexOf('<') != -1) {
                                        defaults.set(9, Boolean.valueOf("true"));
                                        defaults.set(10, Boolean.valueOf("false"));
                                        value = value.replace ('<', ' ');
                                    }
                                    else if (value.indexOf('>') != -1) {
                                        defaults.set(9, Boolean.valueOf("false"));
                                        defaults.set(10, Boolean.valueOf("true"));
                                        value = value.replace ('>', ' ');
                                    }
                                    else if (value.indexOf('#') != -1) {
                                        defaults.set(9, Boolean.valueOf("false"));
                                        defaults.set(10, Boolean.valueOf("false"));
                                        value = value.replace ('#', ' ');
                                    }
                                    else if (value.indexOf('+') != -1) {
                                        defaults.set(9, Boolean.valueOf("true"));
                                        defaults.set(10, Boolean.valueOf("true"));
                                        value = value.replace ('+', ' ');
                                    }
                                    // if * is in the value field, set value to negative and it will get applied as a distance based rate later
                                    else if (value.indexOf('*') != -1) {
                                        value = value.replace ('*', '-');
                                    }
                                    tdefaults.set(i, Double.valueOf(value));

                                    if ( WRITE_NEW_ROUTE_FILES )
                                        newRoutesWriter.printf(" %s=%s ", keyWord, value);

                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.fatal("exception thrown parsing:");
                logger.fatal(s);
                logger.fatal("", e);
                throw new RuntimeException();
            }
		}
	}

	
	// parse this one field from the transit line segment data
	private String getSegmentField (String s, int stringPointer) {
	    
	    int start=stringPointer, end=0;

		// fields are separated by spaces
		while (start < s.length() && s.charAt(start) == ' ')
			start++;
		end =  s.indexOf(' ', start);
		if (end == -1)
			end = s.length();
		
		return s.substring (stringPointer, end);
	}


	private String keyWordScan (String field, String keyWord) {
	    int start=0, end=0;

        int equalIndex = field.indexOf("=");
        String keyWordPart = (field.substring(0,equalIndex)).trim();
        
		if (keyWordPart.equalsIgnoreCase(keyWord)) {
			start = equalIndex + 1;
			end =  field.length();
			return field.substring(start,end);
		}
		else {
			return null;
		}
	}

	// return the values using default order of values without keyword names
	String[] getHeaderValues (String s)	{
  	
	    int stringPointer=0, start=0, end=0;
	    String[] tokens = new String[9];

		// first value: line name
		if (s.indexOf("lin=") != -1) {
			start = s.indexOf('\'', s.indexOf("lin="));
			end =  s.indexOf('\'', start+1);
			tokens[0] = s.substring(start+1, end);
			stringPointer = end + 1;
		}
		else if (s.indexOf("line=") != -1) {
			start = s.indexOf('\'', s.indexOf("line="));
			end =  s.indexOf('\'', start+1);
			tokens[0] = s.substring(start+1, end);
			stringPointer = end + 1;
		}
		else {
			start = s.indexOf('\'');
			end =  s.indexOf('\'', start+1);
			tokens[0] = s.substring(start+1, end);
			stringPointer = end + 1;
		}

		// second value: line mode
		if (s.indexOf("mod=") != -1) {
			start = s.indexOf("mod=") + 4;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[1] = s.substring(start, end);
			stringPointer = end;
		}
		else if (s.indexOf("mode=") != -1) {
			start = s.indexOf("mode=") + 5;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[1] = s.substring(start, end);
			stringPointer = end;
		}
		else {
			start = stringPointer;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[1] = s.substring(start, end);
			stringPointer = end;
		}

		// third value: line vehicle type
		if (s.indexOf("veh=") != -1) {
			start = s.indexOf("veh=") + 4;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[2] = s.substring(start, end);
			stringPointer = end;
		}
		else if (s.indexOf("vehicle=") != -1) {
			start = s.indexOf("vehicle=") + 8;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[2] = s.substring(start, end);
			stringPointer = end;
		}
		else {
			start = stringPointer;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[2] = s.substring(start, end);
			stringPointer = end;
		}

		// fourth value: line headway
		if (s.indexOf("headway=") != -1) {
			start = s.indexOf("headway=") + 8;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[3] = s.substring(start, end);
			stringPointer = end;
		}
		else if (s.indexOf("hdwy=") != -1) {
			start = s.indexOf("hdwy=") + 5;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[3] = s.substring(start, end);
			stringPointer = end;
		}
		else if (s.indexOf("hdw=") != -1) {
			start = s.indexOf("hdw=") + 4;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[3] = s.substring(start, end);
			stringPointer = end;
		}
		else {
			start = stringPointer;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[3] = s.substring(start, end);
			stringPointer = end;
		}

		// fifth value: line speed
		if (s.indexOf("speed=") != -1) {
			start = s.indexOf("speed=") + 6;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[4] = s.substring(start, end);
			stringPointer = end;
		}
		else if (s.indexOf("spd=") != -1) {
			start = s.indexOf("spd=") + 4;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[4] = s.substring(start, end);
			stringPointer = end;
		}
		else {
			start = stringPointer;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[4] = s.substring(start, end);
			stringPointer = end;
		}

		// sixth value: line description
		if (s.indexOf("descr=") != -1) {
			start = s.indexOf('\'', s.indexOf("descr="));
			end =  s.indexOf('\'', start+1);
			tokens[5] = s.substring(start+1, end);
			stringPointer = end + 1;
		}
		else {
			start = s.indexOf('\'', stringPointer);
			end =  s.indexOf('\'', start+1);
			tokens[5] = s.substring(start+1, end);
			stringPointer = end + 1;
		}

		// seventh value: line user field 1
		if (s.indexOf("ut1=") != -1) {
			start = s.indexOf("ut1=") + 4;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[6] = s.substring(start, end);
			stringPointer = end;
		}
		else {
			start = stringPointer;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[6] = s.substring(start, end);
			stringPointer = end;
		}

		// eighth value: line user field 2
		if (s.indexOf("ut2=") != -1) {
			start = s.indexOf("ut2=") + 4;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[7] = s.substring(start, end);
			stringPointer = end;
		}
		else {
			start = stringPointer;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[7] = s.substring(start, end);
			stringPointer = end;
		}

		// ninth value: line user field 3
		if (s.indexOf("ut3=") != -1) {
			start = s.indexOf("ut3=") + 4;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			tokens[8] = s.substring(start, end);
			stringPointer = end;
		}
		else {
			start = stringPointer;
			while (s.charAt(start) == ' ')
				start++;
			end =  s.indexOf(' ', start);
			if (end == -1)
				end = s.length();
			tokens[8] = s.substring(start, end);
			stringPointer = end;
		}

		return tokens;
	}


	// print a table of transit route information
	public void printTransitRoutes (String fileName) {
   	
		try {
            
		    PrintWriter out = new PrintWriter (	new BufferedWriter ( new FileWriter (fileName) ) );

		    for (int i=0; i < lineCount; i++) {
				printLineHeader(out, i);
				printLineSegments(out, i);
		    }
			out.close();

		} catch (IOException e) {
			logger.error ("Transit route file could not be opened for writing, or some other IO exception ocurred.", e);
            throw new RuntimeException();
		}
	}


	// print a table of transit line header information
	public void printLineHeader (PrintWriter out, int rte) {
	    
	    String outputRecord = String.format( "%6s%6s%6s%9s%9s%7s%2s%22s%6s%6s%6s", "seq", "line", "mode", "vehicle", "headway", "speed", "  ", "description", "ut1", "ut2", "ut3" ); 

	    String underLine = "-";
		for (int i=0; i < outputRecord.length() - 1; i++)
			underLine += "-";

		out.println (underLine);
		out.println (outputRecord);

		out.printf ( "%6d%6s%6s%9d%9.0f%7.0f%2s%22s%6.2f%6.2f%6.2f", rte+1, line[rte], String.valueOf(mode[rte]), vehType[rte], headway[rte], speed[rte], "  ", description[rte], ut1[rte], ut2[rte], ut3[rte] );
	}


    // print a table of transit line segments information
    public void printLineSegments (PrintWriter out, int rte) {
    

        TrSegment ts;

        String outputRecord = String.format ( "%4s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s%6s", "seq", "link", "an", "bn", "dwf", "dwt", "path", "ttf", "ttf1", "ttft", "us1", "us2", "us3", "brd", "alt", "lay", "tdwt", "tus1", "tus2", "tus3" );

        String underLine = "-";
        for (int i=0; i < outputRecord.length() - 1; i++)
            underLine += "-";

        out.println (underLine);
        out.println (outputRecord);


        for (int i=0; i < transitPath[rte].size(); i++) {
            ts = (TrSegment)transitPath[rte].get(i);
            out.printf( "%4d%6d%6d%6d%6.2f%6.2f%6s%6d%6d%6d%6.2f%6.2f%6.2f%6s%6s%6s%6s%6.2f%6.2f%6.2f%6.2f%6.2f", i+1, ts.link, ts.an, ts.bn, ts.dwf, ts.dwt, String.valueOf(ts.path), ts.ttf, ts.ttf1, ts.ttft, ts.us1, ts.us2, ts.us3, String.valueOf(ts.boardA), String.valueOf(ts.alightA), String.valueOf(ts.boardB), String.valueOf(ts.alightB), ts.lay, ts.tdwt, ts.tus1, ts.tus2, ts.tus3 );
        }

        out.println (underLine);
        out.println ("");
        out.println ("");
    }

    
    // print a data table of transit line segments information
    public void printTransitRouteFile ( String transitRouteDataFilesDirectory ) {

        TrSegment ts;
        PrintWriter out = null;
        
        String fileName = String.format("%s/routes_%d", transitRouteDataFilesDirectory, filenum);
        
        try {
            out = new PrintWriter ( new BufferedWriter ( new FileWriter (fileName) ) ) ;
            filenum++;
        }
        catch (IOException e) {
            logger.fatal ( String.format("Transit route data file %s could not be opened for writing or some other IO exception ocurred", fileName), e);
        }

        out.printf( "%s,%s,%s,%s,%s,%s\n", "rteIndex", "rteName", "segmentIndex", "an", "bn", "linkId" );
        
        for ( int rte=0; rte < transitPath.length; rte++ ) {
            
            for (int i=0; i < transitPath[rte].size(); i++) {
                ts = (TrSegment)transitPath[rte].get(i);
                if ( ! ts.layover )
                    out.printf( "%d,%s,%d,%d,%d,%d\n", rte, line[rte].trim(), i+1, ts.an, ts.bn, ts.link );
            }

        }
        
        out.close();

    }

    
//	public void getLinkIndices (NetworkHandlerIF nh) {
//		int k, ia;
//		boolean linkFound;
//		double dwt, dwf, tdwt;
//		TrSegment ts;
//		TrSegment tsNew;
//
//		
//		int[] nodeIndex = nh.getNodeIndex();
//		int[] indexNode = nh.getIndexNode();
//		int[] sortedLinkIndex = nh.getSortedLinkIndexA();
//		int[] ip = nh.getIpa();
//		int[] ib = nh.getIb();
//        int[] drops = nh.getDrops();
//		double[] dist = nh.getDist();
//
//        
//        ShortestPathTreeH sp = new ShortestPathTreeH(nh);
//
//        // set the highway network attribute on which to skim the network
//        sp.setLinkCost( nh.setLinkGeneralizedCost() );
//        
//        // set the highway network valid links attribute for links which may appear in paths between unconnected highway network nodes in transit routes.
//        sp.setValidLinks( nh.getValidLinksForTransitPaths() );
//
//		for (int rte=0; rte < transitPath.length; rte++) {
//            
//			for (int seg=0; seg < transitPath[rte].size(); seg++) {
//			    
//                try {
//
//                    ts = (TrSegment)transitPath[rte].get(seg);
//
//                    ia = nodeIndex[ts.an];
//                    
//                    // if ia < 0, the node in the route description does not exist in the highway network
//                    if ( ia < 0 ) {
//                        logger.error ( String.format("node %d in route %d: %s does not exist in highway network.", ts.an, rte, line[rte]) );
//                        continue;
//                    }
//                    
//                    linkFound = false;
//                    for (int i=ip[ia]; i < ip[ia+1]; i++) {
//                        k = sortedLinkIndex[i];
//
//                        if (indexNode[ib[k]] == ts.bn) {
//                            ts.link = k;
//                            dwt = ts.getDwt();
//                            dwf = ts.getDwf();
//                            tdwt = ts.getTdwt();
//                            if ( dwt < 0 )
//                                ts.setDwt ( -dwt*dist[k] );
//                            if ( dwf < 0 )
//                                ts.setDwt ( -dwf*dist[k] );
//                            if ( tdwt < 0 )
//                                ts.setTdwt ( -tdwt*dist[k] );
//                            linkFound = true;
//
//                            if ( drops[k] == 1 ) {
//                                logger.error ( String.format("link from node %d to node %d in route %d: %s has a 'dropped from network' value.", ts.an, ts.bn, rte, line[rte]) );
//                            }
//                            
//                            break;
//                        }
//                    }
//
//                    if ( linkFound == false && ts.layover == false ) {
//                            
//                        if ( ts.path ) {
//
//                            if ( ts.an < 0 ) {
//                                logger.error ( "invalid node read from route file " + ts.an + " in route " + rte + ", " + getLine(rte) );
//                                throw new RuntimeException();
//                            }
//                            if ( ts.bn < 0 ) {
//                                logger.error ( "invalid node read from route file " + ts.bn + " in route " + rte + ", " + getLine(rte) );
//                                throw new RuntimeException();
//                            }
//                            if ( nodeIndex[ts.an] < 0 ) {
//                                logger.error ( "node " + ts.an + " in route file in route " + rte + ", " + getLine(rte) + " not found in highway network.");
//                                throw new RuntimeException();
//                            }
//                            if ( nodeIndex[ts.bn] < 0 ) {
//                                logger.error ( "node " + ts.bn + " in route file in route " + rte + ", " + getLine(rte) + " not found in highway network.");
//                                throw new RuntimeException();
//                            }
//                            
//                            sp.buildPath (nodeIndex[ts.an], nodeIndex[ts.bn]);
//                            
//                            int[] nodes = null;
//                            try {
//                                // get the node list (in external node numbers) for the current shortest path in ShortestPathTreeH.
//                                nodes = sp.getNodeList ();
//                            }catch ( Exception e ) {
//                                logger.error ( "path could not be built from " + ts.an + " to " + ts.bn + " in route " + rte + ", " + getLine(rte), e );
//                                throw new RuntimeException();
//                            }
//
//                            transitPath[rte].remove(seg);
//
//                            
//                            // ts.board refers to boarding at anode of segment
//                            // ts.alight refers to alighting at bnode of segment
//                            
//                            // first segment replaces the one just removed and allows boarding only (bnode is an intermediate node in the path and therefore has no alighting)
//                            int an = nodes[0];
//                            int bn = nodes[1];
//                            String newPathString = String.format("%d", an);
//                            
//                            tsNew = ts.segmentCopy(ts, this);
//                            tsNew.an = an;
//                            tsNew.bn = bn;
//                            if ( ! ts.layover )
//                                tsNew.lay = 0.0;
//                            tsNew.board = true;
//                            tsNew.alight = false;
//                            transitPath[rte].add(seg++, tsNew);
//                            
//                            newPathString += String.format("  %d", bn);
//                            
//                            // intermediate segments up to and including the last node allows neither boarding or alighting at anode
//                            for (int j=2; j < nodes.length - 1; j++) {
//                                an = bn;
//                                bn = nodes[j];
//                                newPathString += String.format("  %d", bn);
//
//                                tsNew = ts.segmentCopy(ts, this);
//                                tsNew.an = an;
//                                tsNew.bn = bn;
//                                tsNew.lay = 0.0;
//                                tsNew.board = false;
//                                tsNew.alight = false;
//                                transitPath[rte].add(seg++, tsNew);
//                            }
//
//                            // last segment allows alighting
//                            an = bn;
//                            bn = nodes[nodes.length - 1];
//                            newPathString += String.format("  %d", bn);
//
//                            tsNew = ts.segmentCopy(ts, this);
//                            tsNew.an = an;
//                            tsNew.bn = bn;
//                            if ( ! ts.layover )
//                                tsNew.lay = 0.0;
//                            tsNew.board = false;
//                            tsNew.alight = true;
//                            transitPath[rte].add(seg++, tsNew);
//                            
//                            // reset the seg index number so the next original segment will be processed afetr the new ones are added
//                            seg -= nodes.length;
//                            
//                            if (logger.isDebugEnabled())
//                                logger.info ("building path from " + ts.an + " to " + ts.bn + " for route " + rte + ", " + getLine(rte) + ": " + newPathString);
//
//                        }
//                        else {
//                            
//                            logger.error ( "no highway network link exists from " + ts.an + " to " + ts.bn + " on route " + rte + ", " + getLine(rte) + ", and path=yes was not defined for route." );
//
//                        }
//                        
//                    }
//                    
//                }
//                catch (Exception e) {
//                    logger.error ( "exception caught for rte=" + rte + ", " + getLine(rte) + ", seg=" + seg + ".", e );
//                    throw new RuntimeException();
//                }
//                
//			}
//		}
//	}

    
	public int getTotalLinkCount() {
		return totalLinkCount;
	}

	public int getLineCount() {
		return lineCount;
	}

	public double getHeadway(int rte) {
		return headway[rte];
	}

    public String getRouteType(int rte) {
        return routeType[rte];
    }
    
    public String[] getRouteTypes() {
        return routeType;
    }
    
    public String[] getRouteNames() {
        return line;
    }
    
    public String getLine(int rte) {
        return line[rte];
    }
    
    public int getId(String name) {
        int rte = -1;
        for (int i=0; i < line.length; i++) {
            if ( line[i].equalsIgnoreCase(name) ) {
                rte = i;
                break;
            }
        }
        return rte;
    }

    public char getMode(int rte) {
        return mode[rte];
    }

	public String getDescription(int rte) {
		return description[rte].trim();
	}
    
    public int getMaxRoutes() {
        return this.maxRoutes;
    }





    public class SegmentChecker {

        NetworkHandlerIF nh = null;
        
        TrRoute tr = null;
        TrSegment ts = null;
        int rteSeg = 0;
        
        // get highway network index arrays 
        int[] nodeIndex = null;
        int[] indexNode = null;
        int[] sortedLinkIndex = null;
        int[] ip = null;
        int[] ib = null;
        int[] drops = null;
        double[] dist = null;

        ShortestPathTreeH sp = null;
        
        public SegmentChecker ( NetworkHandlerIF nh ) {
            
            this.nh = nh;
            
            // get highway network index arrays 
            nodeIndex = nh.getNodeIndex();
            indexNode = nh.getIndexNode();
            sortedLinkIndex = nh.getSortedLinkIndexA();
            ip = nh.getIpa();
            ib = nh.getIb();
            drops = nh.getDrops();
            dist = nh.getDist();

            
            // get ShortestPathTreeH object to build path between disconnected highway nodes
            sp = new ShortestPathTreeH(nh);

            // set the highway network attribute on which to skim the network
            sp.setLinkCost( nh.setLinkGeneralizedCost() );
            
            // set the highway network valid links attribute for links which may appear in paths between unconnected highway network nodes in transit routes.
            sp.setValidLinks( nh.getValidLinksForTransitPaths() );

        }
        
        private String checkSegment ( TrRoute tr, TrSegment ts, int rteSeg ) {
            
            int k, ia, rte;
            boolean linkFound;
            double dwt, dwf, tdwt;
            TrSegment tsNew;

            this.ts = ts;
            this.tr = tr;
            this.rteSeg = rteSeg;
            
            rte = ts.rteIndex;

            String newPathString = "";
            
            try {

                ia = nodeIndex[ts.an];
                
                // if ia < 0, the node in the route description does not exist in the highway network
                if ( ia < 0 ) {
                    logger.error ( String.format("node %d in route %d: %s does not exist in highway network.", ts.an, rte, line[rte]) );
                    throw new RuntimeException();
                }
                
                linkFound = false;
                for (int i=ip[ia]; i < ip[ia+1]; i++) {
                    k = sortedLinkIndex[i];

                    if (indexNode[ib[k]] == ts.bn) {
                        ts.link = k;
                        dwt = ts.getDwt();
                        dwf = ts.getDwf();
                        tdwt = ts.getTdwt();
                        if ( dwt < 0 )
                            ts.setDwt ( -dwt*dist[k] );
                        if ( dwf < 0 )
                            ts.setDwt ( -dwf*dist[k] );
                        if ( tdwt < 0 )
                            ts.setTdwt ( -tdwt*dist[k] );
                        linkFound = true;

                        if ( drops[k] == 1 ) {
                            logger.error ( String.format("link from node %d to node %d in route %d: %s has a 'dropped from network' value.", ts.an, ts.bn, rte, line[rte]) );
                        }
                        
                        break;
                    }
                }

                if ( linkFound == false && ts.layover == false ) {
                        
                    if ( ts.path ) {

                        if ( ts.an < 0 ) {
                            logger.error ( "invalid node read from route file " + ts.an + " in route " + rte + ", " + getLine(rte) );
                            throw new RuntimeException();
                        }
                        if ( ts.bn < 0 ) {
                            logger.error ( "invalid node read from route file " + ts.bn + " in route " + rte + ", " + getLine(rte) );
                            throw new RuntimeException();
                        }
                        if ( nodeIndex[ts.an] < 0 ) {
                            logger.error ( "node " + ts.an + " in route file in route " + rte + ", " + getLine(rte) + " not found in highway network.");
                            throw new RuntimeException();
                        }
                        if ( nodeIndex[ts.bn] < 0 ) {
                            logger.error ( "node " + ts.bn + " in route file in route " + rte + ", " + getLine(rte) + " not found in highway network.");
                            throw new RuntimeException();
                        }
                        
                        sp.buildPath (nodeIndex[ts.an], nodeIndex[ts.bn]);
                        
                        int[] nodes = null;
                        try {
                            // get the node list (in external node numbers) for the current shortest path in ShortestPathTreeH.
                            nodes = sp.getNodeList ();
                        }catch ( Exception e ) {
                            logger.error ( "path could not be built from " + ts.an + " to " + ts.bn + " in route " + rte + ", " + getLine(rte), e );
                            throw new RuntimeException();
                        }

                        transitPath[rte].remove(rteSeg);

                        
                        // ts.board refers to boarding at anode of segment
                        // ts.alight refers to alighting at bnode of segment
                        
                        // first segment replaces the one just removed and allows boarding only (bnode is an intermediate node in the path and therefore has no alighting)
                        int an = nodes[0];
                        int bn = nodes[1];
                        
                        tsNew = ts.segmentCopy(ts, tr);
                        tsNew.an = an;
                        tsNew.bn = bn;
                        tsNew.link = nh.getLinkIndex(an, bn); 
                        if ( ! ts.layover )
                            tsNew.lay = 0.0;
                        tsNew.boardA = true;
                        tsNew.alightA = false;
                        tsNew.boardB = false;
                        tsNew.alightB = false;
                        transitPath[rte].add(rteSeg++, tsNew);
                        
                        newPathString += String.format(" dwt=#0.0 %d", bn);
                        
                        // intermediate segments up to and including the last node allows neither boarding or alighting at anode
                        for (int j=2; j < nodes.length - 1; j++) {
                            an = bn;
                            bn = nodes[j];
                            newPathString += String.format(" %d", bn);

                            tsNew = ts.segmentCopy(ts, tr);
                            tsNew.an = an;
                            tsNew.bn = bn;
                            tsNew.link = nh.getLinkIndex(an, bn); 
                            tsNew.lay = 0.0;
                            tsNew.boardA = false;
                            tsNew.alightA = false;
                            tsNew.boardB = false;
                            tsNew.alightB = false;
                            transitPath[rte].add(rteSeg++, tsNew);
                        }

                        // last segment allows alighting
                        an = bn;
                        bn = nodes[nodes.length - 1];
                        newPathString += String.format(" dwt=+0.0 %d", bn);

                        tsNew = ts.segmentCopy(ts, tr);
                        tsNew.an = an;
                        tsNew.bn = bn;
                        tsNew.link = nh.getLinkIndex(an, bn); 
                        if ( ! ts.layover )
                            tsNew.lay = 0.0;
                        tsNew.boardA = false;
                        tsNew.alightA = false;
                        tsNew.boardB = false;
                        tsNew.alightB = true;
                        transitPath[rte].add(rteSeg++, tsNew);
                        
                        // reset the seg index number so the next original segment will be processed afetr the new ones are added
                        rteSeg -= nodes.length;
                        
                        if (logger.isDebugEnabled())
                            logger.info ("building path from " + ts.an + " to " + ts.bn + " for route " + rte + ", " + getLine(rte) + ": " + newPathString);

                    }
                    else {
                        
                        logger.error ( "no highway network link exists from " + ts.an + " to " + ts.bn + " on route " + rte + ", " + getLine(rte) + ", and path=yes was not defined for route." );

                    }
                    
                }
                
                return newPathString;
                
            }
            catch (Exception e) {
                logger.error ( "exception caught for rte=" + rte + ", " + getLine(rte) + ", seg=" + rteSeg + ".", e );
                throw new RuntimeException();
            }
                    
        }

    }

}
