package com.pb.tlumip.et.test;
/**
 * ExternalStationParametersTest is a class that tests the ExternalStationParameters class.
 *
 * @author Kimberly Grommes
 * @version 1.0, Oct 26, 2007
 * Created by IntelliJ IDEA.
 */

import com.pb.common.datafile.TableDataSet;
import com.pb.tlumip.et.ExternalStationParameters;
import junit.framework.TestCase;

public class ExternalStationParametersTest extends TestCase {
    ExternalStationParameters externalStationParameters;

    protected void setUp() {
        TableDataSet tblTest = new TableDataSet();

        int[] intsExternalStations = {5001,5002,5003,9999};
        int[] intsYears = {2000,2000,2000,2000};
        float[] fltsOBAuto = {1019, 22420, 9249, 3926};
        float[] fltsOBTruckLight = {162,1367,544,314};

        tblTest.appendColumn(intsExternalStations, "ExSta");
        tblTest.appendColumn(intsYears, "Year");
        tblTest.appendColumn(fltsOBAuto, "OBAuto");
        tblTest.appendColumn(fltsOBTruckLight, "OBTruckLight");

        externalStationParameters = new ExternalStationParameters(tblTest);
    }


    public void testGetValue1() throws Exception {
        float fltValue = externalStationParameters.getValue(5001, "OBAuto");
        assertEquals(1019f, fltValue);
    }

    public void testGetValue2() throws Exception {
        float fltValue = externalStationParameters.getValue(5003, "OBTruckLight");
        assertEquals(544f, fltValue);
    }

}