package com.pb.tlumip.grid;

import com.pb.common.grid.GridFile;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.util.OutTextFile;
import com.pb.common.util.Format;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * Contains post-grid synthesis methods that
 * will analyze the output such as NSQFT assigned
 * 
 * @author  Christi Willison
 * @version Jan 2, 2004
 */
public class Analyzer {

    private ArrayList aZones;
    private static Logger log = Logger.getLogger("com.pb.tlumip.grid");
    String[] headingsForTotalCells = {"MH","MF","AT","SFD","RRMH","RRSFD","Accom","Depot","GovSppt","Gschool","HvyInd","Hospital",
                                       "Inst","LtInd","Office","Retail","Whse","Log","Ag","UNDEV","UNDEVFor","UNDEVAg","TP","WATER","NA"};
    String[] headingsForYrBuilt={"1999-2000","1995-98","1990-94","1980-89","1970-79","1960-69","1950-59","1940-49","1900-1939","NoInfo(0)","1859"};






    private void countTotalSQFTByDevType(){
        long[][] totalSQFTByDevType = new long[Ref.NUM_ALPHA_ZONES][Ref.SUMMARYDEVTYPES.length];
        // Prepare to read the grid files with alpha zone numbers and sqft
        GridFile DEVTYPEGrid = null;
        GridFile SQFTGrid = null;
        GridFile AZONEGrid = null;
        try {
           DEVTYPEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/DEVTYPE.grid"));
           SQFTGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/SQFT.grid"));
           AZONEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/AZONE.grid"));
        } catch (FileNotFoundException e) {
              e.printStackTrace();
        }
        int logPos = totalSQFTByDevType[0].length-2;
        try {
            short devTypeCode;

            int[] tempDevTypeRow = new int[DEVTYPEGrid.getParameters().getNumberOfColumns()];
            int[] tempSQFTRow = new int[SQFTGrid.getParameters().getNumberOfColumns()];
            int[] tempAZRow = new int[AZONEGrid.getParameters().getNumberOfColumns()];
            for (int r=1; r<=SQFTGrid.getParameters().getNumberOfRows(); r++) {
                DEVTYPEGrid.getRow(r,tempDevTypeRow);
                SQFTGrid.getRow(r,tempSQFTRow);
                AZONEGrid.getRow(r,tempAZRow);
                for (int c=0; c<tempSQFTRow.length;c++) {
                    devTypeCode=(short)tempDevTypeRow[c];
                    if(devTypeCode == -1 || devTypeCode < 3 || devTypeCode > 40 || devTypeCode==34) continue;
                    else if(devTypeCode==33) totalSQFTByDevType[tempAZRow[c]][logPos]+=tempSQFTRow[c];
                    else if(devTypeCode==32) totalSQFTByDevType[tempAZRow[c]][logPos+1]+=tempSQFTRow[c];
                    else totalSQFTByDevType[tempAZRow[c]][Ref.getPosition(devTypeCode,Ref.DEVTYPECODES)]+=tempSQFTRow[c];
                }
            }
            DEVTYPEGrid.close();
            AZONEGrid.close();
         } catch (IOException e) {
            e.printStackTrace();
         }


        ArrayList headings = new ArrayList();
        headings.add("AZone");
        for(int i=0;i<Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length;i++) headings.add(Ref.DEVTYPES[i]);
        headings.add("LOG");
        headings.add("AG");

        TableDataSet sqftTable = createTable(totalSQFTByDevType,headings);
        printTable(sqftTable,new File("c:/temp/TotalSQFTByDevType.csv"));



    }

    private void countTotalCellsByDevType(){
        int[][] totalCellsByDevType = new int[Ref.NUM_ALPHA_ZONES][headingsForTotalCells.length];
        // Prepare to read the grid files with alpha zone numbers and sqft
        GridFile DEVTYPEGrid = null;
        GridFile AZONEGrid = null;
        try {
            DEVTYPEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/DEVTYPE.grid"));
            AZONEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/AZONE.grid"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int unassignedPos = headingsForTotalCells.length-1;
        int logPos = Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length;
        int agPos=logPos+1;
        int undevelopedPos = logPos+2;
        try {
            short devTypeCode;
            int[] tempDevTypeRow = new int[DEVTYPEGrid.getParameters().getNumberOfColumns()];
            int[] tempAZRow = new int[AZONEGrid.getParameters().getNumberOfColumns()];
            for (int r=1; r<=DEVTYPEGrid.getParameters().getNumberOfRows(); r++) {
                DEVTYPEGrid.getRow(r,tempDevTypeRow);
                AZONEGrid.getRow(r,tempAZRow);
                for (int c=0; c<tempDevTypeRow.length;c++) {
                    devTypeCode=(short)tempDevTypeRow[c];
                    if(devTypeCode==-1 && tempAZRow[c]==-1) continue;
                    if(devTypeCode == -1 ) totalCellsByDevType[tempAZRow[c]][unassignedPos]+=1;
                    else if (devTypeCode < 3 || devTypeCode >40)
                        totalCellsByDevType[tempAZRow[c]][Ref.getPosition(devTypeCode,Ref.UNDEVELOPEDDEVTYPECODES)+undevelopedPos]+=1;
                    else if(devTypeCode==33) totalCellsByDevType[tempAZRow[c]][logPos]+=1;
                    else if(devTypeCode==32) totalCellsByDevType[tempAZRow[c]][agPos]+=1;
                    else totalCellsByDevType[tempAZRow[c]][Ref.getPosition(devTypeCode,Ref.DEVTYPECODES)]+=1;
                }
             }
            //finished counting up cell values so close gridfiles
            DEVTYPEGrid.close();
            AZONEGrid.close();
         } catch (IOException e) {
            e.printStackTrace();
         }

        //get data ready to print
        ArrayList headings = new ArrayList();
        headings.add("AZone");
        for(int i=0;i<headingsForTotalCells.length;i++) headings.add(headingsForTotalCells[i]);
        TableDataSet cellsTable = createTable(totalCellsByDevType, headings);
        printTable(cellsTable,new File("c:/temp/TotalCellsByDevType.csv"));

    }

    private void calculateAvgYrBuilt(){
        //this 2-d array will originally store the sum of the year builts by devType (so it needs to be long)
        //but the entry will be replaced by the average avgYRBuilt[r][c]=avgYRBuiltByDevType[r][c]/totalCellsByDevType[r][c] (short);
        long[][] avgYrBuiltByDevType = new long[Ref.NUM_ALPHA_ZONES][Ref.SUMMARYDEVTYPES.length];
        int[][] totalCellsByDevType = new int[Ref.NUM_ALPHA_ZONES][Ref.SUMMARYDEVTYPES.length];

        GridFile YRBUILTGrid = null;
        GridFile DEVTYPEGrid=null;
        GridFile AZONEGrid = null;
        try {
            YRBUILTGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/YRBUILT.grid"));
            DEVTYPEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/DEVTYPE.grid"));
            AZONEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/AZONE.grid"));
        } catch (FileNotFoundException e) {
              e.printStackTrace();
        }

        int logPos = Ref.getSubscript("LOG",Ref.SUMMARYDEVTYPES);
        try {
            short devTypeCode;
            short yrBuilt;
            int[] tempYrBuiltRow=new int[YRBUILTGrid.getParameters().getNumberOfColumns()];
            int[] tempDevTypeRow = new int[DEVTYPEGrid.getParameters().getNumberOfColumns()];
            int[] tempAZRow = new int[AZONEGrid.getParameters().getNumberOfColumns()];
            for (int r=1; r<=DEVTYPEGrid.getParameters().getNumberOfRows(); r++) {
                YRBUILTGrid.getRow(r,tempYrBuiltRow);
                DEVTYPEGrid.getRow(r,tempDevTypeRow);
                AZONEGrid.getRow(r,tempAZRow);
                for (int c=0; c<tempDevTypeRow.length;c++) {
                    devTypeCode=(short)tempDevTypeRow[c];
                    yrBuilt=(short) tempYrBuiltRow[c];
                    if(devTypeCode==-1 || devTypeCode < 3 || devTypeCode >40) continue;
                    else if(devTypeCode==33) {
                        totalCellsByDevType[tempAZRow[c]][logPos]+=1;
                        avgYrBuiltByDevType[tempAZRow[c]][logPos]+=yrBuilt;
                    }
                    else if(devTypeCode==32) {
                        totalCellsByDevType[tempAZRow[c]][logPos+1]+=1;
                        avgYrBuiltByDevType[tempAZRow[c]][logPos+1]+=yrBuilt;
                    }
                    else {
                        totalCellsByDevType[tempAZRow[c]][Ref.getPosition(devTypeCode,Ref.SUMMARYDEVTYPECODES)]+=1;
                        avgYrBuiltByDevType[tempAZRow[c]][Ref.getPosition(devTypeCode,Ref.SUMMARYDEVTYPECODES)]+=yrBuilt;
                    }
                }
             }
        //finished counting up cell values so close gridfiles
        YRBUILTGrid.close();
        DEVTYPEGrid.close();
        AZONEGrid.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Now we need to find the average.
        for(int r=0;r<avgYrBuiltByDevType.length;r++){
            for(int c=0;c<avgYrBuiltByDevType[0].length;c++){
                if(totalCellsByDevType[r][c]==0) continue;
                else{
                    avgYrBuiltByDevType[r][c]=(long)Math.rint(((double)avgYrBuiltByDevType[r][c])/totalCellsByDevType[r][c]);
                }
            }
        }
        //Now create the TableDataSet
        ArrayList headings = new ArrayList();
        headings.add("AZone");
        for(int i=0;i<Ref.SUMMARYDEVTYPES.length;i++) headings.add(Ref.SUMMARYDEVTYPES[i]);
        TableDataSet avgYrBuiltTable = createTable(avgYrBuiltByDevType,headings);
        printTable(avgYrBuiltTable,new File("c:/temp/AvgYrBuiltByDevType.csv"));


    }

    private void countDistributionOfYearBuiltPerDevType(){
        //9 is the number of intervals that we picked from for yr built + 1 for year 1859 + 1 for 0 - residences outside the halo
        //1999-2000,1995-98,1990-94,1980-89,1970-79,1960-69,1950-59,1940-49,1900-1939,0,1859
        int[][][] distYRBuiltByDevType = new int[Ref.NUM_ALPHA_ZONES][Ref.SUMMARYDEVTYPES.length][11];
        // Prepare to read the grid files with alpha zone numbers, devTypes and yrBuilt
        GridFile YRBUILTGrid = null;
        GridFile DEVTYPEGrid=null;
        GridFile AZONEGrid = null;
        try {
            YRBUILTGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/YRBUILT.grid"));
            DEVTYPEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/DEVTYPE.grid"));
            AZONEGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/AZONE.grid"));
        } catch (FileNotFoundException e) {
              e.printStackTrace();
        }
        int[] tempYrBuiltRow = new int[YRBUILTGrid.getParameters().getNumberOfColumns()];
        int[] tempDevTypeRow = new int[DEVTYPEGrid.getParameters().getNumberOfColumns()];
        int[] tempAZRow = new int[AZONEGrid.getParameters().getNumberOfColumns()];

        try {
            short yrBuilt;
            short devTypeCode;
            int yrBuiltIndex=-1;
            int devTypeIndex=-1;
            for(int r=1;r<=YRBUILTGrid.getParameters().getNumberOfRows();r++){
                YRBUILTGrid.getRow(r,tempYrBuiltRow);
                DEVTYPEGrid.getRow(r,tempDevTypeRow);
                AZONEGrid.getRow(r,tempAZRow);
                for(int c=0;c<tempYrBuiltRow.length;c++){
                    yrBuilt=(short)tempYrBuiltRow[c];
                    devTypeCode=(short)tempDevTypeRow[c];
                    devTypeIndex=Ref.getPosition(devTypeCode,Ref.SUMMARYDEVTYPECODES);
                    if(devTypeIndex==-1 || yrBuilt==-1) continue;
                    else if(yrBuilt>=1999){
                        yrBuiltIndex=0;
                    }else if(yrBuilt>=1995){
                        yrBuiltIndex=1;
                    }else if(yrBuilt>=1990){
                        yrBuiltIndex=2;
                    }else if(yrBuilt>=1980){
                         yrBuiltIndex=3;
                    }else if(yrBuilt>=1970){
                         yrBuiltIndex=4;
                    }else if(yrBuilt>=1960){
                          yrBuiltIndex=5;
                    }else if(yrBuilt>=1950){
                          yrBuiltIndex=6;
                    }else if(yrBuilt>=1940){
                          yrBuiltIndex=7;
                    }else if(yrBuilt>=1900){
                          yrBuiltIndex=8;
                    }else if(yrBuilt==0){
                          yrBuiltIndex=9;
                    }else if(yrBuilt==1859){
                          yrBuiltIndex=10;
                    }
                    distYRBuiltByDevType[tempAZRow[c]][devTypeIndex][yrBuiltIndex]++;
                }//next element in row
            }//next row
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList headings = new ArrayList();
        headings.add("DEVTYPE");
        for(int i=0;i<headingsForYrBuilt.length;i++) headings.add(headingsForYrBuilt[i]);
        readAzones();
        for(int i=0;i<aZones.size();i++){
            TableDataSet yrBuiltDistTable = createTable(distYRBuiltByDevType[Integer.parseInt((String)aZones.get(i))],headings,Ref.SUMMARYDEVTYPES);
            printTable(yrBuiltDistTable,new File("c:/temp/DistributionSummaries/YrBuiltForAZ"+(String)aZones.get(i)+".csv"));
        }


    }

     private void readAzones (){
        aZones = new ArrayList();
        try {
            BufferedReader br = new BufferedReader(new FileReader("c:/Project_Files/tlumip/input_files/AlphaZones.csv"));
            String s;
            br.readLine();    // Skip the header
            while ((s = br.readLine()) != null) {
                aZones.add(s);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TableDataSet createTable(long[][]twoDArray, ArrayList headings){//must create a Table full of Strings
                                                                            //since TableDataSet doesn't handle long values
        TableDataSet tds = new TableDataSet();

        String[] aZoneArray;
        readAzones();
        aZoneArray = new String[aZones.size()];
        for(int i=0;i<aZones.size();i++){
            aZoneArray[i] = (String)aZones.get(i);
        }
        tds.appendColumn(aZoneArray,(String)headings.get(0));



        for(int i=0;i<twoDArray[0].length;i++){
            String[] column = new String[aZoneArray.length];
            for(int j=0;j<aZoneArray.length;j++) {
                column[j]=Long.toString(twoDArray[Integer.parseInt(aZoneArray[j])][i]);
            }
            tds.appendColumn(column,(String)headings.get(i+1));
        }
        return tds;
    }

    private TableDataSet createTable(int[][]twoDArray, ArrayList headings){//must create a Table full of Strings
                                                                            //since TableDataSet doesn't handle double values
        TableDataSet tds = new TableDataSet();
        readAzones();
        String[] aZoneArray = new String[aZones.size()];
        for(int i=0;i<aZones.size();i++){
            aZoneArray[i] = (String)aZones.get(i);
        }
        tds.appendColumn(aZoneArray,(String)headings.get(0));


        for(int i=0;i<twoDArray[0].length;i++){
            String[] column = new String[aZoneArray.length];
            for(int j=0;j<aZoneArray.length;j++) {
                column[j]=Integer.toString(twoDArray[Integer.parseInt(aZoneArray[j])][i]);
            }
            tds.appendColumn(column,(String)headings.get(i+1));
        }
        return tds;
    }

    private TableDataSet createTable(int[][]twoDArray, ArrayList headings, String[] firstColumn){//must create a Table full of Strings
                                                                            //since TableDataSet doesn't handle double values
        TableDataSet tds = new TableDataSet();
        tds.appendColumn(firstColumn,(String)headings.get(0));

        for(int i=0;i<twoDArray[0].length;i++){
            String[] column = new String[firstColumn.length];
            for(int j=0;j<firstColumn.length;j++) {
                column[j]=Integer.toString(twoDArray[j][i]);
            }
            tds.appendColumn(column,(String)headings.get(i+1));
        }
        return tds;
    }

    private void printTable(TableDataSet tds, File file){
        CSVFileWriter fw = new CSVFileWriter();
        try {
            fw.writeFile(tds,file,12,new DecimalFormat("#.#"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    public static void main(String[] args) { //takes about 40 minutes to run both analyses.
        log.info("\tstart time: "+ new Date().toString());
        Analyzer analyze = new Analyzer();
        analyze.countTotalSQFTByDevType();
        analyze.countTotalCellsByDevType();
        analyze.countDistributionOfYearBuiltPerDevType();
        analyze.calculateAvgYrBuilt();
        log.info("\tend time: "+ new Date().toString());
    }
}
