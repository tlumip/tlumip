package com.pb.despair.model;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import java.util.Arrays;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;



public class Halo {
    private static Logger logger = Logger.getLogger("com.pb.despair.model.Halo");
	String[] stateLabels = { "California", "Idaho", "Nevada", "Oregon", "Washington" };

	int maxAlphaZone = 0;
	int numAlphaZones = 0;

	int[] indexZone = null;
	int[] zoneIndex = null;
		
	final int MAX_STATE_FIPS = 200;
	final int NUM_STATES = stateLabels.length;

	int[][] pumas = {
		{ 100, 200, 300, 500 },
		{ 100, 200, 301, 302 },
		{ 100, 300, 400 },
		{ 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500 },
		{ 700, 800, 900, 1100, 1400, 1901, 1902 }
	};
	
	static final int CA = 0;
	static final int ID = 1;
	static final int NV = 2;
	static final int OR = 3;
	static final int WA = 4;
	static final int CAfips = 6;
	static final int IDfips = 16;
	static final int NVfips = 32;
	static final int ORfips = 41;
	static final int WAfips = 53;

	int[] stateIndex = null;
	int[] stateFips = null;

    
    public Halo ( String zoneIndexFile ) {

        stateFips = new int[NUM_STATES];
		stateFips[CA] = CAfips;
		stateFips[ID] = IDfips;
		stateFips[NV] = NVfips;
		stateFips[OR] = ORfips;
		stateFips[WA] = WAfips;

		stateIndex = new int[MAX_STATE_FIPS];
		stateIndex[CAfips] = CA;
		stateIndex[IDfips] = ID;
		stateIndex[NVfips] = NV;
		stateIndex[ORfips] = OR;
		stateIndex[WAfips] = WA;

		readZoneIndices ( zoneIndexFile );		
    }

    
	public int getNumberOfStates() {
		return NUM_STATES;
	}
    
    
	public int getMaxZoneNumber() {
		return maxAlphaZone;
	}
    
    
	public int getNumberOfZones() {
		return numAlphaZones;
	}
    
    
	public String getStateLabel( int i ) {
		return stateLabels[i];
	}
    
    
    
    public boolean isPumaInHalo (int stFips, int puma) {
        
        int stIndex = stateIndex[stFips];
        
        for (int i=0; i < pumas[stIndex].length; i++) {
            
            if (puma == pumas[stIndex][i])
                return true;
            
        }
        
        return false;
            
    }


	public int[] getZoneIndex () {
		return zoneIndex;
	}
    

	public int getZoneIndex (int zone) {
		return zoneIndex[zone];
	}
    

	public int[] getIndexZone () {
		return indexZone;
	}
    

	public int getIndexZone (int index) {
		return indexZone[index];
	}
    

	private void readZoneIndices ( String fileName ) {
	    
		int zone;
		int index;
	    String county;
        String state;


		// read the PI output file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}

		numAlphaZones = table.getRowCount();
		indexZone = new int[numAlphaZones];
		for (int i=0; i < numAlphaZones; i++)
			if ( (int)table.getValueAt(i+1, "Azone") > maxAlphaZone ) 
				maxAlphaZone = (int)table.getValueAt(i+1, "Azone");

        logger.info("Num alpha zones: " +  numAlphaZones);
        logger.info("Max alpha zone: " + maxAlphaZone);
		zoneIndex = new int[maxAlphaZone+1];
		
		Arrays.fill (indexZone, -1);
		Arrays.fill (zoneIndex, -1);

		index = 0;
		for (int r=0; r < numAlphaZones; r++) {
		    
			zone = (int)table.getValueAt(r+1, "Azone");
            county = (String)table.getStringValueAt(r+1, "County");
            state = (String)table.getStringValueAt(r+1, "State");

			indexZone[index] = zone;
            zoneIndex[zone] = index;
            index++;
		}
		
	}
	
	
}

