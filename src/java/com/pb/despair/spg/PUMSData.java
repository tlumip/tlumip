package com.pb.despair.spg;

import com.pb.despair.model.Halo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The PUMSData class is used to manage input of PUMS data.
 *
 */

public class PUMSData {

    protected static Logger logger = Logger.getLogger("com.pb.despair.spg");

	public static final int HHID_INDEX = 4;
	public static final int STATE_INDEX = 5;
	public static final int PUMA_INDEX = 6;
	public static final int HHSIZE_INDEX = 7;
	public static final int HHINC_INDEX = 8;
	public static final int HHWT_INDEX = 9;
	public static final int HHWRKRS_INDEX = 10;
	public static final int PERSON_ARRAY_INDEX = HHWRKRS_INDEX + 1;
	
    DataDictionary dd;

    
    
    public PUMSData (String PUMSDataDictionary) {
        this.dd = new DataDictionary(PUMSDataDictionary);
    }


    
	public ArrayList readSpg1Attributes (String fileName, String zoneIndexFile ) {

		int hhCount=0;
		int recCount=0;
		
		int hhid = 0;
		int numPersons = 0;
		int industry = 0;
		int occup = 0;
		int rlabor;
		int workers;
		int employed = 0;

		ArrayList hhList = new ArrayList();
		int[] hhAttribs = null;
		
		Halo halo = new Halo( zoneIndexFile );
		Workers hhWorkers = new Workers();
		
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String s = new String();
            
			while ((s = in.readLine()) != null) {
				recCount++;
        
				numPersons = getPUMSHHDataValue (s, "PERSONS");
				
				
				// skip HH records where persons field is zero
				if ( numPersons > 0 ) {

					// read the household attributes from the household data record
					if (getPUMSRecType(s).equals("H")) {

						hhAttribs = new int[PERSON_ARRAY_INDEX + 3*numPersons];
						hhAttribs[HHID_INDEX] = hhid;
						hhAttribs[STATE_INDEX] = getPUMSHHDataValue (s, "STATE");
						hhAttribs[PUMA_INDEX] = getPUMSHHDataValue (s, "PUMA");
						hhAttribs[HHSIZE_INDEX] = numPersons;
						hhAttribs[HHINC_INDEX] = getPUMSHHDataValue (s, "RHHINC");
						hhAttribs[HHWT_INDEX] = getPUMSHHDataValue (s, "HOUSWGT");


						// don't save info if hh is not in halo.  read person records then skip to nex hh record.
						if ( !halo.isFipsPumaInHalo (hhAttribs[STATE_INDEX], hhAttribs[PUMA_INDEX]) ) {

							for (int i=0; i < numPersons; i++)
								s = in.readLine();

							continue;
						}
            
						// read the person records for the number of persons in the household.
						workers = 0;
						for (int i=0; i < numPersons; i++) {
							s = in.readLine();
					
							if (! getPUMSRecType(s).equals("P")) {
								logger.severe("Expected P record type on record: " + recCount + " but got: " + getPUMSRecType(s) + ".");
								logger.severe("exiting readData(" + fileName + ") in PUMSData.");
								logger.severe("exit (21)");
								System.exit (21);
							}
							
							industry = getPUMSPersDataValue (s, "INDUSTRY");
							occup = getPUMSPersDataValue (s, "OCCUP");
							rlabor = getPUMSPersDataValue (s, "RLABOR");

							switch (rlabor) {
								case 0:
								case 3:
								case 6:
								    employed = 0;
									break;
								case 1:
								case 2:
								case 4:
								case 5:
								    employed = 1;
									workers++;
									break;
							}

							// save industry for each person followed by occup for each person in hhAttrib array.
							hhAttribs[PERSON_ARRAY_INDEX + i*3 + 0] = industry;
							hhAttribs[PERSON_ARRAY_INDEX + i*3 + 1] = occup;
							hhAttribs[PERSON_ARRAY_INDEX + i*3 + 2] = employed;
						}
						
						if ( workers > hhWorkers.getNumberWorkerCategories()-1 )
							hhAttribs[HHWRKRS_INDEX] = hhWorkers.getNumberWorkerCategories()-1;
						else
							hhAttribs[HHWRKRS_INDEX] = workers;
						
					}
					else {
						logger.severe("Expected H record type on record: " + recCount + " but got: " + getPUMSRecType(s) + ".");
						logger.severe("exiting readData(" + fileName + ") in PUMSData.");
						logger.severe("exit (20)");
						System.exit (20);
					}
				
	
					hhList.add (hhid, hhAttribs);
					hhid++;

				}
				
			}

		} catch (Exception e) {

			logger.severe ("IO Exception caught reading pums data file: " + fileName);
			e.printStackTrace();
			
		}

		
		return (hhList);
	}
    
    
	public ArrayList readSpg2OutputAttributes (  String fileName, String[] hhFieldNames, String[] personFieldNames, String zoneIndexFile ) {

		int recCount=0;
		
		int hhid = 0;
		int numPersons = 0;
		int puma = 0;
		int state = 0;

		ArrayList hhList = new ArrayList();
		
		Halo halo = new Halo( zoneIndexFile );
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String s = new String();
            
			while ((s = in.readLine()) != null) {
			    
				recCount++;
				numPersons = getPUMSHHDataValue (s, "PERSONS");

				// skip HH records where persons field is zero
				if ( numPersons > 0 ) {

					String[][] outputFieldValues = new String[numPersons+1][];
					outputFieldValues[0] = new String[hhFieldNames.length];
					for (int i=0; i < numPersons; i++)
						outputFieldValues[i+1] = new String[personFieldNames.length];
				
				
					// read the household attributes from the household data record
					if (getPUMSRecType(s).equals("H")) {

						state = getPUMSHHDataValue (s, "STATE");
						puma = getPUMSHHDataValue (s, "PUMA");

						// don't save info if hh is not in halo.  read person records then skip to next hh record.
						if ( !halo.isFipsPumaInHalo ( state, puma ) ) {

							for (int i=0; i < numPersons; i++)
								s = in.readLine();

							continue;
							
						}
            

						for (int j=0; j < hhFieldNames.length; j++)
							outputFieldValues[0][j] = Integer.toString( getPUMSHHDataValue ( s, hhFieldNames[j] ) );
					    

						// read the person records for the number of persons in the household.
						for (int i=0; i < numPersons; i++) {
							s = in.readLine();

							if (! getPUMSRecType(s).equals("P")) {
								logger.severe("Expected P record type on record: " + recCount + " but got: " + getPUMSRecType(s) + ".");
								logger.severe("exiting readData(" + fileName + ") in PUMSData.");
								logger.severe("exit (21)");
								System.exit (21);
							}

							for (int j=0; j < personFieldNames.length; j++)
								outputFieldValues[i+1][j] = Integer.toString( getPUMSPersDataValue ( s, personFieldNames[j] ) );
						}
						
					}
					else {
						logger.severe("Expected H record type on record: " + recCount + " but got: " + getPUMSRecType(s) + ".");
						logger.severe("exiting readData(" + fileName + ") in PUMSData.");
						logger.severe("exit (20)");
						System.exit (20);
					}
				
	
					hhList.add (hhid, outputFieldValues);
					hhid++;

				}
				
			}

		} catch (Exception e) {

			logger.severe ("IO Exception caught reading pums data file: " + fileName);
			e.printStackTrace();
			
		}

		
		return (hhList);
	}
    
    
    private String getPUMSRecType (String s) {
        return s.substring(dd.getStartCol(dd.HHAttribs, "RECTYPE"), dd.getLastCol(dd.HHAttribs, "RECTYPE"));        
    }


    private int getPUMSHHDataValue (String s, String PUMSVariable) {
        return Integer.parseInt ( s.substring(dd.getStartCol(dd.HHAttribs, PUMSVariable), dd.getLastCol(dd.HHAttribs, PUMSVariable)) );        
    }


    private int getPUMSPersDataValue (String s, String PUMSVariable) {
        return Integer.parseInt ( s.substring(dd.getStartCol(dd.PersAttribs, PUMSVariable), dd.getLastCol(dd.PersAttribs, PUMSVariable)) );        
    }


    public void printPUMSDictionary () {
     
        logger.info ("PUMS Houshold Attributes");
		logger.info ("------------------------");
        dd.printDictionary(dd.HHAttribs);

		logger.info (" ");
		logger.info (" ");
           
		logger.info ("PUMS Person Attributes");
		logger.info ("----------------------");
        dd.printDictionary(dd.PersAttribs);
    }

}

