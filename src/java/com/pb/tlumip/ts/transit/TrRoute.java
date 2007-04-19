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
import com.pb.tlumip.ts.assign.ShortestPath;


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

	protected static transient Logger logger = Logger.getLogger("com.pb.tlumip.ts.transit");

    int maxRoutes=0;
	int lineCount= -1, linkCount=0, totalLinkCount=0;
	int an, bn;
	String[] line, description;
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
		defaults = new ArrayList();
		tdefaults = new ArrayList();

        this.maxRoutes = maxRoutes;
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
	}


	void initTempDefaults () {
		tdefaults.add(0, Double.valueOf("0.0"));
		tdefaults.add(1, Double.valueOf("0.0"));
		tdefaults.add(2, Double.valueOf("0.0"));
		tdefaults.add(3, Double.valueOf("0.0"));
		tdefaults.add(4, Double.valueOf("0.0"));
	}


	public void readTransitRoutes (String fileName) {
		int recNumber = 0;

		try {
		    BufferedReader in = new BufferedReader( new FileReader(fileName) );
		    String s = new String();
		    while ((s = in.readLine()) != null) {
		        recNumber++;

		        // skip empty records
		        if (s.length() > 0) {
		            // skip comment records and the data type record
		            if (s.charAt(0) == 'c' || s.charAt(0) == 't') {
		            }
		            // process transit line header records
		            else if (s.charAt(0) == 'a') {
//						if (linkCount > 0 && ((Double)tdefaults.get(0)).doubleValue() >= 0.0) {  // lay > 0 at end of route description
//							ts = (TrSegment)transitPath[lineCount].get(0);
//							if (ts.an == bn) {
//								TrSegment seg = new TrSegment(ts.an, ts.bn, defaults, tdefaults);
//								seg.layover = true;
//								transitPath[lineCount].add(linkCount++, seg);
//							}
//						}

		                initDefaults();
		                initTempDefaults();

		                lineCount++;
		                totalLinkCount += linkCount;
		                parseHeader(s);
		                linkCount = 0;
		                an = -1;
		                bn = -1;
		            }
		            // process transit line sequence records
		            else if (s.charAt(0) == ' ') {
                        
		                parseSegments(s);
		            }
		            //  any other record type is invalid, so exit.
		            else {
		                System.out.println ("Transit line file record number " + recNumber + " has an invalid " + s.charAt(0) + " first character.");
		                if (s.charAt(0) == 'd' || s.charAt(0) == 'm')
		                    System.out.println (s.charAt(0) + " is not a supported update code.");
		                System.out.println ("Records may only begin with c, t, a, or blank.");
		                System.out.println ("Program exiting while reading transit line file");
		                System.exit(-1);
		            }
		        }
		    }
      // done reading route records.  Add layover link at end of last route.
//			if (linkCount > 0 && ((Double)tdefaults.get(0)).doubleValue() >= 0.0) {  // lay > 0 at end of route description
//				ts = (TrSegment)transitPath[lineCount].get(0);
//				if (ts.an == bn) {
//					TrSegment seg = new TrSegment(ts.an, ts.bn, defaults, tdefaults);
//					seg.layover = true;
//					transitPath[lineCount].add(linkCount++, seg);
//					totalLinkCount += linkCount;
//				}
//			}
		} catch (Exception e) {
		    logger.error ("IO Exception caught reading transit route file: " + fileName + ", record number=" + recNumber, e);
		}

		lineCount++;
 		logger.info (recNumber + " transit line file records read.");
        logger.info (lineCount + " transit lines found.");
        logger.info (totalLinkCount + " total transit links found in all transit routes.\n");
  
    }


	// parse the transit line data
	private void parseHeader (String InputString) {

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
		
	}


	// parse the transit line segment data for the transit line itinerary
	private void parseSegments (String s) {
	    
		String keyWord=null, value=null, field=null;
		int stringPointer = 0;

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
                    }
                    else {
                        if (bn != -1)
                            an = bn;
                        bn = Integer.parseInt(value);
                    }

                    if (an != -1 && bn != -1) {
                        TrSegment seg = new TrSegment(an, bn, defaults, tdefaults);
                        transitPath[lineCount].add(linkCount++, seg);
                        initTempDefaults();
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
                                    else {
                                        defaults.set(9, Boolean.valueOf("true"));
                                        defaults.set(10, Boolean.valueOf("true"));
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
                                    TrSegment seg = new TrSegment(an, bn, defaults, tdefaults);
                                    seg.layover = true;
                                    seg.lay = Double.parseDouble(value);
                                    transitPath[lineCount].add(linkCount++, seg);
                                    totalLinkCount += linkCount;
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
			System.out.println ("Transit route file could not be opened for writing,");
			System.out.println ("or some other IO exception ocurred");
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

		out.printf ( "%6d%6d%6s%9d%9d%7d%2s%22s%6.2f%6.2f%6.2f", rte+1, line[rte], String.valueOf(mode[rte]), vehType[rte], headway[rte], speed[rte], "  ", description[rte], ut1[rte], ut2[rte], ut3[rte] );
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
			out.printf( "%4d%6d%6d%6d%6.2f%6.2f%6s%6d%6d%6d%6.2f%6.2f%6.2f%6s%6s%6.2f%6.2f%6.2f%6.2f%6.2f", i+1, ts.link, ts.an, ts.bn, ts.dwf, ts.dwt, String.valueOf(ts.path), ts.ttf, ts.ttf1, ts.ttft, ts.us1, ts.us2, ts.us3, String.valueOf(ts.board), String.valueOf(ts.alight), ts.lay, ts.tdwt, ts.tus1, ts.tus2, ts.tus3 );
	  }

		out.println (underLine);
		out.println ("");
		out.println ("");
	}

	
	public void getLinkIndices (NetworkHandlerIF nh) {
		int k, ia;
		boolean linkFound;
		double dwt, dwf, tdwt;
		TrSegment ts;
		TrSegment tsNew;

		ShortestPath sp = new ShortestPath(nh);
		
		int[] nodeIndex = nh.getNodeIndex();
		int[] indexNode = nh.getIndexNode();
		int[] sortedLinkIndex = nh.getSortedLinkIndexA();
		int[] ip = nh.getIpa();
		int[] ib = nh.getIb();
		double[] dist = nh.getDist();
		
		for (int rte=0; rte < transitPath.length; rte++) {
            
            int dummy = 0;
            if ( rte == 260 ) {
                dummy = 1;
            }
            
            
			for (int seg=0; seg < transitPath[rte].size(); seg++) {
			    
                try {

                    ts = (TrSegment)transitPath[rte].get(seg);

                    ia = nodeIndex[ts.an];
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
                            break;
                        }
                    }

                    if (! linkFound) {
//                      logger.info ("building path from " + ts.an + " to " + ts.bn);
                        sp.buildPath (nodeIndex[ts.an], nodeIndex[ts.bn]);
                        
                        int[] nodes = null;
                        try {
                            nodes = sp.getNodeList (nodeIndex[ts.an], nodeIndex[ts.bn]);
                        }catch ( Exception e ) {
                            logger.error ( "path could not be built from " + ts.an + " to " + ts.bn, e );
                            System.exit(-1);
                        }

                        transitPath[rte].remove(seg);

                        int a = nodes[0];
                        for (int j=1; j < nodes.length; j++) {
                            int b = nodes[j];
                            tsNew = ts.segmentCopy(ts, this);
                            tsNew.an = indexNode[a];
                            tsNew.bn = indexNode[b];
                            if (j > 1 && tsNew.lay >= 0.0)
                                tsNew.lay = 0.0;
                            transitPath[rte].add(seg++, tsNew);
                            a = b;
                        }
                        seg -= (nodes.length + 1);
                    }
                    
                }
                catch (Exception e) {
                    logger.error ( "exception caught for rte=" + rte + ", " + getLine(rte) + ", seg=" + seg + ".", e );
                    System.exit(-1);
                }
                
			}
		}
	}

    
	public int getTotalLinkCount() {
		return totalLinkCount;
	}

	public int getLineCount() {
		return lineCount;
	}

	public double getHeadway(int rte) {
		return headway[rte];
	}

    public String getLine(int rte) {
        return line[rte];
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

}
