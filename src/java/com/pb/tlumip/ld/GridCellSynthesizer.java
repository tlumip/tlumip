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

package com.pb.osmp.ld;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.datafile.JDBCTableReader;
import com.pb.common.datafile.JDBCTableWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.datafile.TableDataSetIndexedValue;
import com.pb.common.sql.JDBCConnection;
import com.pb.common.util.ResourceUtil;

/**
 * This class creates grid cells in a zone based on the aggregate characteristics of the built form in the zone
 * @author John Abraham 
 */
public class GridCellSynthesizer {

    private static Logger logger = Logger.getLogger("com.pb.osmp.ld");
    
     public static void main(String[] args) {
         GridCellSynthesizer me = new GridCellSynthesizer();
         me.initialize();
         me.synthesizeGridCells();
     }

    private TableDataSet gridCellTable;
    private TableDataSetCollection landData;
    private TableDataSet employmentTable;
    ResourceBundle ldRb;
    
    java.util.Random myRandom;
    private TableDataSet landQuantityTable;
    private TableDataSet zoningSchemesTab;
    private String employmentTableName;
    private String residentialTableName;
    private double gridCellSize;
    private int zoneRow;
    private int taz;
     
     public void initialize() {
          
        myRandom = new java.util.Random();
        ldRb = ResourceUtil.getResourceBundle("ld");
        
        JDBCConnection myConnection = new JDBCConnection(ldRb.getString("land.database"), ldRb.getString("land.jdbcDriver"), "", "");
        
        JDBCTableReader myReader = new JDBCTableReader(myConnection);
        JDBCTableWriter myWriter = new JDBCTableWriter(myConnection);
        
        landData = new TableDataSetCollection(myReader,myWriter);
        landQuantityTable = landData.getTableDataSet(ldRb.getString("land.quantities.table"));
        employmentTableName = ldRb.getString("employment.table");
        employmentTable = landData.getTableDataSet(employmentTableName);
        residentialTableName = ldRb.getString("residential.table");
        //TODO read in zoning schemes
        //zoningSchemesTab = landData.getTableDataSet("ZoningSchemes");
        float landQuantity= landQuantityTable.getColumnTotal(landQuantityTable.checkColumnPosition(ldRb.getString("land.quantity.field")));
        gridCellSize = Integer.valueOf(ldRb.getString("land.gridSize")).intValue();
        long cellQuantity = Math.round(landQuantity/gridCellSize);
        //TODO check for cell quantities that are too large for an int.
        gridCellTable = setUpGridCellTable((int) cellQuantity);
     }


     public TableDataSet setUpGridCellTable(int numberOfCells) {
         
         gridCellTable = new TableDataSet();

        gridCellTable.appendColumn(new int[numberOfCells],"TAZ");
        gridCellTable.appendColumn(new int[numberOfCells],"ID");
        gridCellTable.appendColumn(new float[numberOfCells],"AmountOfLand");
        gridCellTable.appendColumn(new String[numberOfCells],"DevelopmentType");
        gridCellTable.appendColumn(new float[numberOfCells],"AmountOfDevelopment");
        gridCellTable.appendColumn(new int[numberOfCells],"YearBuilt");
        gridCellTable.appendColumn(new String[numberOfCells],"ZoningScheme");
        gridCellTable.appendColumn(new int[numberOfCells],"NodeID");
        return gridCellTable;
     }

     class ResidentialLand {
         float calculated;
         float observed;
         String title;
         double farAdjustment=0;
         double acres;
         int cells;
         
         ResidentialLand(TableDataSetIndexedValue myTds, String myTitle) {
             title = myTitle;
             myTds.setMyFieldName(myTitle);
             calculated = myTds.retrieveValue(landData);
             myTds.setMyFieldName(myTitle +" Observed");
             observed = myTds.retrieveValue(landData);
             if (observed ==0) {
                 farAdjustment = 5;
             } else {
                 farAdjustment = calculated/observed;
             }
             if (farAdjustment >5) farAdjustment = 5;
             if (farAdjustment <0.2) farAdjustment = 0.2;
             // TODO put maximum and minimum far adjustment into properties file.
             // TODO figure out how to make more vacantacres or take away from forest or something if the adjustment is >max or< min
             acres = calculated/farAdjustment;
             cells = (int) Math.round(acres/gridCellSize);
         }
     }
     
     class NonResidentialLand {
         String[] landNames;
         double employees;
         double land;
         double employeesPerAcre; // Floor Area Ratio
         double maxEmployeesPerAcre=100;
         double minEmployeesPerAcre=10;
         int cells;

         NonResidentialLand(String[] landNames, TableDataSetIndexedValue employment, String[] employeeNames) {
             double[] employeePortions = new double[employeeNames.length];
             for (int i=0;i<employeePortions.length;i++) {
                 employeePortions[i]=1.0;
             }
             initialize(landNames,employment,employeeNames,employeePortions);
         }

        /**
         * @param landNames
         * @param employment
         * @param employeeNames
         * @param employeePortions
         */
        private void initialize(String[] landNames, TableDataSetIndexedValue employment, String[] employeeNames, double[] employeePortions) {
            // first add up all the land;
            this.landNames = landNames;
            land = 0;
            for (int l=0;l<landNames.length;l++) {
                land+= landQuantityTable.getValueAt(zoneRow,landQuantityTable.checkColumnPosition(landNames[l]));
            }
            if (land <0) land =0;
            employees = 0;
            for (int e=0;e<employeeNames.length;e++) {
                employment.setMyFieldName(employeeNames[e]);
                double theseEmployees = employment.retrieveValue(landData);
                employees += theseEmployees * employeePortions[e];
            }
            cells = (int) Math.round(land/gridCellSize);
            if (cells ==0 && employees>0) {
                cells = 1;
            }
            employeesPerAcre = employees/cells/gridCellSize;
            if (employees==0) employeesPerAcre = 0;
            if (employeesPerAcre < minEmployeesPerAcre) logger.warn("taz "+taz+" for "+landNames[0]+" employees per acre too low at "+employeesPerAcre);
            if (employeesPerAcre > maxEmployeesPerAcre) logger.warn("taz "+taz+" for "+landNames[0]+" employees per acres too high at "+employeesPerAcre);
            
        }
        
        void setMinAndMaxIntensity(double min, double max) {
            minEmployeesPerAcre= min;
            maxEmployeesPerAcre= max;
            if (employeesPerAcre < minEmployeesPerAcre) {
                logger.warn("taz "+taz+" for "+landNames[0]+" employees per acre too low at "+employeesPerAcre+", decreasing land requirements");
                employeesPerAcre = minEmployeesPerAcre;
                land = employeesPerAcre/employees;
            }
            if (employeesPerAcre > maxEmployeesPerAcre) {
                logger.warn("taz "+taz+" for "+landNames[0]+" employees per acres too high at "+employeesPerAcre+", increasing land requirements");
                employeesPerAcre = maxEmployeesPerAcre;
                land = employeesPerAcre/employees;
            }
            cells = (int) Math.round(land/gridCellSize);
            if (cells ==0 && employees > 0) {
                cells = 1;
            }
            // correct for rounding
            employeesPerAcre= employees/cells/gridCellSize;
        }

        /**
         * @param landNames
         * @param employment
         * @param employeeNames
         * @param employeePortions
         */
        NonResidentialLand(String[] landNames, TableDataSetIndexedValue employment, String[] employeeNames, double[] employeePortions) {
            initialize(landNames,employment,employeeNames,employeePortions);
            
        }

     }
     
     
     public void synthesizeGridCells() {
         // do this row by row in the land table, each row is a TAZ
         // can't use the indexing function built into TableDataSet because the TAZ numbers are way too huge, would eat up to much memory
         // instead use TableDataSetIndexedValue
         int[][] keyValues = new int[1][1];
         String[][] stringKeyValues = new String[1][0];
         String[] stringKeyColumns = new String[0];
         String[] intKeyColumns = new String[] {"TAZ"};
         String[] resultColumnNames = new String[] {"taz","heavy","light","k12","office","commRetail","higherEd","health","govt","ag","military","mf","sfd","rs","ra"};
         TableDataSet gridCounts = TableDataSet.create(new float[landQuantityTable.getRowCount()][resultColumnNames.length],resultColumnNames);
         gridCounts.setName("gridcounts");
         TableDataSet intensities = TableDataSet.create(new float[landQuantityTable.getRowCount()][resultColumnNames.length],resultColumnNames);
         intensities.setName("intensities");
         
         for (zoneRow =1; zoneRow <= landQuantityTable.getRowCount();zoneRow++) {
             taz = (int) landQuantityTable.getValueAt(zoneRow,landQuantityTable.checkColumnPosition("TAZ"));
             keyValues[0][0] = taz;
             gridCounts.setValueAt(zoneRow,1,taz);
             intensities.setValueAt(zoneRow,1,taz);
             
             //first do heavy industry land
             TableDataSetIndexedValue tds = new TableDataSetIndexedValue(employmentTableName,stringKeyColumns,intKeyColumns,stringKeyValues,keyValues,"04_HvyInd production");
             NonResidentialLand heavyIndustry = new NonResidentialLand(
                     new String[] {"Heavy industrial Land"},
                     tds ,
                     new String[] {"04_HvyInd production","02_Metal production","05_Transp production"});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("heavy"),heavyIndustry.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("heavy"),(float) heavyIndustry.employeesPerAcre);

             
             // light industry land
             NonResidentialLand lightIndustry = new NonResidentialLand(
                     new String[] {"Light industrial Land"},
                     tds, 
                     new String[] {"03_LightInd production","06_Whlsl production","11_TrHndl","12_OthSvc"}, new double[] {1.0,1.0,1.0,0.61843});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("light"),lightIndustry.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("light"),(float) lightIndustry.employeesPerAcre);
             
             // K12 land
             NonResidentialLand k12 = new NonResidentialLand(new String[] {"K-12 school land"},tds, new String[] {"13_K-12"}, new double[] {.78024578});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("k12"),k12.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("k12"),(float) k12.employeesPerAcre);
             
             NonResidentialLand office = new NonResidentialLand(
                     new String[] {"Office Land"},
                     tds,
                     new String[] {"01_AgForFish office support",
                             "02_Metal office",
                             "03_LightInd office",
                             "04_HvyInd office",
                             "05_Transp office",
                             "06_Whlsl office",
                             "07_Retail office",
                             "10_Health",
                             "11_TrHndl",
                             "13_K-12",
                             "15_Govt"},
                             //TODO make splitting health jobs into office and other automatic
                             //TODO make splitting transportation handling jobs into office and other automatic
                             //TODO make splitting k12 jobs into office and other automatic
                             //TODO make splitting govt jobs into office and other automatic
                             new double[] {1,1,1,1,1,1,1,100/250,.381567,.219754217,0.611988064});
             NonResidentialLand commercialRetail = new NonResidentialLand(
                     new String[] {"Commercial Land","Retail Land"},
                     tds,
                     new String[] {"07_Retail production",
                             "08_Hotel"});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("office"),office.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("office"),(float) office.employeesPerAcre);
             
             NonResidentialLand higherEducation = new NonResidentialLand(new String[] {"Higher Educ Land"},tds,new String[] {"14_HigherEd"});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("higherEd"),higherEducation.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("higherEd"),(float) higherEducation.employeesPerAcre);
             // TODO health split between office and institutional needs to be automatic
             NonResidentialLand health = new NonResidentialLand(new String[] {"Healthcare Institution Land"},tds,new String[] {"10_Health"}, new double[]{150/250});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("health"),health.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("health"),(float) health.employeesPerAcre);
             // TODO automate govt jobssplit into govt space and office space
             NonResidentialLand govt = new NonResidentialLand(new String[] {"Government Land"},tds,new String[] {"15_Govt"}, new double[]{0.38801193});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("govt"),govt.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("govt"),(float) govt.employeesPerAcre);
             NonResidentialLand ag = new NonResidentialLand(new String[] {"Agricultural Land"},tds,new String[] {"01_AgForFish production"});
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("ag"),ag.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("ag"),(float) ag.employeesPerAcre);
             NonResidentialLand military = new NonResidentialLand(new String[] {"Military base land"},tds,new String[0]);
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("military"),military.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("military"),(float) military.employeesPerAcre);
             
             // Now figure out land used by residences.
             tds = new TableDataSetIndexedValue(residentialTableName,stringKeyColumns,intKeyColumns,stringKeyValues,keyValues,"MF");
             
             ResidentialLand mfLand = new ResidentialLand(tds,"MF");
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("mf"),mfLand.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("mf"),(float) mfLand.farAdjustment);
             ResidentialLand sfdLand = new ResidentialLand(tds,"SFD");
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("sfd"),sfdLand.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("sfd"),(float) sfdLand.farAdjustment);
             ResidentialLand rsLand = new ResidentialLand(tds,"Rural Subdivision");
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("rs"),rsLand.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("rs"),(float) rsLand.farAdjustment);
             ResidentialLand raLand = new ResidentialLand(tds,"Rural Acreage");
             gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition("ra"),raLand.cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition("ra"),(float) raLand.farAdjustment);
             
             double landQuantity= landQuantityTable.getValueAt(zoneRow,landQuantityTable.checkColumnPosition(ldRb.getString("land.quantity.field")));
             
         }
         landData.addTableDataSet(gridCounts);
         landData.addTableDataSet(intensities);
         landData.flush();
    }
}