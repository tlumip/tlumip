package com.pb.tlumip.grid;

import com.pb.common.grid.GridParameters;
import com.pb.common.grid.GridFile;
import com.pb.common.util.Format;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * Reads the LU grid file and counts the number of grid cells of each land use.
 * Creates the UneditedTotalCellsByGLC file that will be edited (by hand) and
 * used as an input file into GridSynthesizer.
 * Creates the 3 grid files (DEVTYPE, SQFT, YRBUILT) that we populate in GridSynthesizer.
 *
 * @author  Christi Willison
 * @version Dec 12, 2003
 */
public class Initializer {
    private static Logger logger = Logger.getLogger("com.pb.tlumip.grid");
    private String[] glcForLandUseCode = new String[Ref.NUM_LUCS]; //initialized in the readTotalCellsByGLC method.
    private int[][] totalCellsByGLC = new int[Ref.NUM_ALPHA_ZONES][Ref.GLCS.length];//initialized by readTotalCellsByGLC method.
    private ArrayList aZones = new ArrayList();

    private void readAzones (String filename){
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
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

    private void readGrossLandCategoryDefinitions (String filename){
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringTokenizer st;
            String g, s;
            int lc;
            br.readLine();    // Skip the header, stupid
            while ((s = br.readLine()) != null) {
                st = new StringTokenizer(s, ",");
                g = st.nextToken();   // gross land category
                lc = Integer.parseInt(st.nextToken());
                glcForLandUseCode[lc] = g;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void countTotalCellsByGLC(){

           readGrossLandCategoryDefinitions("c:/Project_Files/tlumip/input_files/S1GLC.csv");
            //Build the totalSupplyByGLC matrix which must be done before any stages can be run.
          // Prepare to read the grid files with alpha zone numbers and land use codes
          GridFile landUseGrid = null;
          GridFile alphaZoneGrid = null;
          try {
             landUseGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/LU9.grid"));
             alphaZoneGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/AZONE.grid"));
          } catch (FileNotFoundException e) {
              e.printStackTrace();
          }

           // I'll assume that the grid files are the same size, an assumption that will probably come back to bite me
           // later on. But right now I'm summing up the number of cells in each alpha zone by gross land categories. We
           // store these in a local matrix for the time being (since we cannot determine the county name (entry into the
           // hash map) from the grid data alone.
           String equivGLC;

           try {
              int[] landUseRow = new int[landUseGrid.getParameters().getNumberOfColumns()];
              int[] azRow = new int[alphaZoneGrid.getParameters().getNumberOfColumns()];
              for (int r=1; r<=landUseGrid.getParameters().getNumberOfRows(); r++) {
                  landUseGrid.getRow(r,landUseRow);
                  alphaZoneGrid.getRow(r,azRow);
                  for (int c=0; c<landUseRow.length; c++) {
//                     if(r%4000==0 && c%8000==0) System.out.println("Processing cell ("+r+","+c+")");
                     if(landUseRow[c] != -1){
//                   System.out.println("Land Use Value in cell ("+r+","+c+") is "+landUseGrid.getValue(r,c));
                         equivGLC = glcForLandUseCode[landUseRow[c]];
                         if(equivGLC != null){
//                          System.out.println("GLC is "+equivGLC);
//                          System.out.println("alpha zone is "+ alphaZoneGrid.getValue(r,c));
                            totalCellsByGLC[azRow[c]][Ref.getSubscript(equivGLC,Ref.GLCS)]++;
                         }
                     }
                  }
               }
           } catch (IOException e) {
              e.printStackTrace();
           }
        try {
            landUseGrid.close();
            alphaZoneGrid.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printTotalCellsByGLC(){
        readAzones("c:/Project_Files/tlumip/input_files/AlphaZones.csv");
        ArrayList colHeadings = new ArrayList();
        colHeadings.add("AZone");
        for(int i=0;i<Ref.GLCS.length;i++)
            colHeadings.add(Ref.GLCS[i]);
        PrintWriter outStream = null;
        try {
            outStream = new PrintWriter (new BufferedWriter( new FileWriter("c:/Project_Files/tlumip/input_files/TotalCellsByGLC.csv") ) );

            //Print titles
            for (int i = 0; i < colHeadings.size(); i++) {
                if (i != 0)
                    outStream.print(",");
                outStream.print( (String)colHeadings.get(i) );
            }
            outStream.println();

            int nRows = aZones.size();
            int nCols = totalCellsByGLC[0].length;

            //Print data
            for (int r=0; r < nRows; r++) {
                outStream.print((String)aZones.get(r));
                for (int c=0; c < nCols; c++) {
                    outStream.print(",");
                    outStream.print( Format.print("%i", totalCellsByGLC[Integer.parseInt((String)aZones.get(r))][c]) );
                }
                outStream.println();
            }
            outStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }





    private void createGridFiles(){ //takes approx. 8 minutes.
        //default parameters are wordSizeInBytes=2, noDataValue=-1 and description=""

        GridParameters shortParameters = new GridParameters(Ref.TLUMIP_GRID_NROWS, Ref.TLUMIP_GRID_NCOLS,
                    Ref.TLUMIP_GRID_XLL, Ref.TLUMIP_GRID_YLL, Ref.TLUMIP_GRID_CELLSIZE, 2, -1, "");
        GridParameters intParameters = new GridParameters(Ref.TLUMIP_GRID_NROWS, Ref.TLUMIP_GRID_NCOLS,
                    Ref.TLUMIP_GRID_XLL, Ref.TLUMIP_GRID_YLL, Ref.TLUMIP_GRID_CELLSIZE, 4, -1, "");

        try {
            GridFile.create(new File("c:/Project_Files/tlumip/grids/DEVTYPE.grid"),shortParameters);
            GridFile.create(new File("c:/Project_Files/tlumip/grids/SQFT.grid"),intParameters);
            GridFile.create(new File("c:/Project_Files/tlumip/grids/YRBUILT.grid"),shortParameters);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



  public static void main(String[] args) {
      logger.info("start time: "+ new Date().toString());
      Initializer init = new Initializer();
      init.createGridFiles();
//      init.countTotalCellsByGLC();
//      init.printTotalCellsByGLC();
      logger.info("end time: "+ new Date().toString());
  }

}
