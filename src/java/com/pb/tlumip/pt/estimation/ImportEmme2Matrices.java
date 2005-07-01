package com.pb.tlumip.pt.estimation;

/** 
 * Imports emme2 matrices, Stores them in compressed matrix format.  
 *
 * @author    Joel Freedman
 * @version   1.0, 8/2001
 *
 */

import com.pb.common.util.InTextFile;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;

import org.apache.log4j.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.StringTokenizer;

public class ImportEmme2Matrices {
    
     static Logger logger = Logger.getLogger("com.pb.tlumip.pt.ImportEmme2Matrices");
     static int TOTALMATRICES=30;
     static int SIZE=5000;
     static final int MAXTAZ=4141;
     static final int MAX_SEQUENTIAL_TAZ=2983;
     static int MATRIXSIZE =0;

     //emme2 matrix names
    public static String[] eName = {"H:/gen2/SKIMS/dc skims/pktime.out",
                                            "H:/gen2/SKIMS/dc skims/pkdist.out",
                                            "H:/gen2/SKIMS/dc skims/optime.out",
                                            "H:/gen2/SKIMS/dc skims/opdist.out",
                                            "H:/gen2/SKIMS/dc skims/pwtivt.out",
                                            "H:/gen2/SKIMS/dc skims/pwtfwt.out",
                                            "H:/gen2/SKIMS/dc skims/pwttwt.out",
                                            "H:/gen2/SKIMS/dc skims/pwtaux.out",
                                            "H:/gen2/SKIMS/dc skims/pwtbrd.out",
                                            "H:/gen2/SKIMS/dc skims/pwtfar.out",
                                            "H:/gen2/SKIMS/dc skims/owtivt.out",
                                            "H:/gen2/SKIMS/dc skims/owtfwt.out",
                                            "H:/gen2/SKIMS/dc skims/owttwt.out",
                                            "H:/gen2/SKIMS/dc skims/owtaux.out",
                                            "H:/gen2/SKIMS/dc skims/owtbrd.out",
                                            "H:/gen2/SKIMS/dc skims/owtfar.out",
                                            "H:/gen2/SKIMS/dc skims/pdtivt.out",
                                            "H:/gen2/SKIMS/dc skims/pdtfwt.out",
                                            "H:/gen2/SKIMS/dc skims/pdttwt.out",
                                            "H:/gen2/SKIMS/dc skims/pdtwlk.out",
                                            "H:/gen2/SKIMS/dc skims/pdtbrd.out",
                                            "H:/gen2/SKIMS/dc skims/pdtdrv.out",
                                            "H:/gen2/SKIMS/dc skims/pdtfar.out",
                                            "H:/gen2/SKIMS/dc skims/odtivt.out",
                                            "H:/gen2/SKIMS/dc skims/odtfwt.out",
                                            "H:/gen2/SKIMS/dc skims/odttwt.out",
                                            "H:/gen2/SKIMS/dc skims/odtwlk.out",
                                            "H:/gen2/SKIMS/dc skims/odtbrd.out",
                                            "H:/gen2/SKIMS/dc skims/odtdrv.out",
                                            "H:/gen2/SKIMS/dc skims/odtfar.out"};

     //output compressed skims
    public static String[] mName = {"pktime",
                                            "pkdist",
                                            "optime",
                                            "opdist",
                                            "pwtivt",
                                            "pwtfwt",
                                            "pwttwt",
                                            "pwtaux",
                                            "pwtbrd",
                                            "pwtfar",
                                            "owtivt",
                                            "owtfwt",
                                            "owttwt",
                                            "owtaux",
                                            "owtbrd",
                                            "owtfar",
                                            "pdtivt",
                                            "pdtfwt",
                                            "pdttwt",
                                            "pdtwlk",
                                            "pdtbrd",
                                            "pdtdrv",
                                            "pdtfar",
                                            "odtivt",
                                            "odtfwt",
                                            "odttwt",
                                            "odtwlk",
                                            "odtbrd",
                                            "odtdrv",
                                            "odtfar"};
     

    public static void main (String[] args) throws FileNotFoundException, IOException {
        

        /*------------------  Read data and write compressed file matrix --------------------
        *
        *  Note: uses InTextFile class
        *
        */
        
        if(logger.isDebugEnabled()) {
            logger.debug("Getting TAZ count from "+eName[0]);
        }
        InTextFile tazCountText = new InTextFile();    
        tazCountText.open(eName[0]);
       
         /* BOOKKEEPING */
          String inRow=new String();
          int lastRowNumber=1;
          
          //for storing values
          int[] lookup   = new int[MAX_SEQUENTIAL_TAZ];
          //initialize row1
          for(int i=0;i<MAX_SEQUENTIAL_TAZ;++i) lookup[i]=0;
          int rowNumber=0;
          int sequentialTaz=0;
          int totalRows=0;
          //read only the first rowNumber
          while((inRow = tazCountText.readLine())!=null && rowNumber<=1){
               
               if(inRow.startsWith("c")||inRow.startsWith("t")||inRow.startsWith("a")) continue;
                              
               StringTokenizer inToken = new StringTokenizer(inRow," :");
               
               if(inToken.hasMoreTokens()==false) break;
               
               rowNumber = new Integer(inToken.nextToken()).intValue();
               
               if(rowNumber!=lastRowNumber){
                   totalRows=sequentialTaz;
                    break;
               }
               
               while(inToken.hasMoreTokens()){
                    ++sequentialTaz;
                    int colNumber = new Integer(inToken.nextToken()).intValue();
                    lookup[sequentialTaz]=colNumber;
                    logger.info("setting row1["+sequentialTaz+"]="+colNumber);
               }
                
        }                               
        for(int matrix=0;matrix<TOTALMATRICES;++matrix){

              /* INPUT FILE */
              logger.info("Reading "+eName[matrix]);
              InTextFile emme2File = new InTextFile();
              emme2File.open(eName[matrix]);

            String path = "D:/tlumip_data/matrix/";
                String fullPath = path+mName[matrix]+".zip";
             /* OUTPUT FILE */
              MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(fullPath));  //Open for writing
              Matrix m1 = new Matrix(mName[matrix], 
                                     "Created with ImportEmme2Matrices",
                                     totalRows,
                                     totalRows
                                     );
                                     
              m1.setExternalNumbers(lookup);
       
                //read emme2 punch and store compressed
               while((inRow = emme2File.readLine())!=null){
               
                    if(inRow.startsWith("c")||inRow.startsWith("t")||inRow.startsWith("a")) continue;
               
               
                    StringTokenizer inToken = new StringTokenizer(inRow," :");
               
                    if(inToken.hasMoreTokens()==false) break;
               
                    rowNumber = new Integer(inToken.nextToken()).intValue();
               
                    if(rowNumber <=5 || rowNumber%100==0)
                        logger.info("Row "+rowNumber);
                            
               
                    while(inToken.hasMoreTokens()){
     
                         int colNumber = new Integer(inToken.nextToken()).intValue();
                         float value = new Float(inToken.nextToken()).floatValue();
                         m1.setValueAt(rowNumber, colNumber, value);
                    }
             }

                logger.info("Writing "+eName[matrix]);
                mw.writeMatrix(m1);
         } //end loop on matrices
     }  //endmain
}     //end class
