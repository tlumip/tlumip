package com.pb.despair.pt;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;

import java.io.File;
import java.io.IOException;

import java.util.ResourceBundle;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import java.util.Iterator;

/** 
 * Class to squeeze taz matrix to district matrix 
 * 
 * @author Steve Hansen
 * @version 1.0 02/09/2004
 * 
 */
public class AlphaToBetaData {
     
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    protected int numberOfDistricts;
    int tazI;
    int tazJ;
    int districtI;
    int districtJ;
    
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
     

    /**
     * Generates and returns a district matrix.  The size of the matrix is based on the number of unique
     * values in the column vector.
     * 
     * @param Column Vector v - uses this vector to determine matrix size.  Size is based on the number of unique values in the vector.
     * @param name - Name of new Matrix
     * @param description - description of new matrix
     * 
     */

    
    public int[] getUniqueZoneList(ColumnVector v)   {
    
        TreeSet list = new TreeSet();
        for(int i=0;i<v.size();i++){
            int externalNumber = v.getExternalNumber(i);
            list.add(new Integer((int)v.getValueAt(externalNumber)));
        }
        Iterator listIterator=list.iterator();
        
        int[] uniqueZones = new int[list.size()+1];
        int count = 1;
        while(listIterator.hasNext()){
            Integer value = (Integer) listIterator.next();
            uniqueZones[count] = value.intValue(); 
            count++;  
        }
    return uniqueZones;      
    }
        

    private Matrix squeezeSum(Matrix tazMatrix, Matrix dMatrix, ColumnVector district){
        float sum = 0;

        for(int i=0;i<district.getRowCount();i++){
            for(int j=0;j<district.getRowCount();j++){
                tazI = (int)tazMatrix.getExternalNumber(i);
                tazJ = (int)tazMatrix.getExternalNumber(j);
                districtI = (int)district.getValueAt(tazI);
                districtJ = (int)district.getValueAt(tazJ);
                
                sum = dMatrix.getValueAt(districtI,districtJ);
                float tm =  tazMatrix.getValueAt(tazI,tazJ);
                sum = sum + tazMatrix.getValueAt(tazI,tazJ);
                dMatrix.setValueAt(districtI,districtJ,sum);
            }
        }
    return dMatrix;
    }

    private Matrix squeezeMean(Matrix tazMatrix, Matrix dMatrix, Matrix countMatrix, ColumnVector district){
        float mean = 0;
        int count = 0;
               
        for(int i=0;i<district.getRowCount();i++){
            for(int j=0;j<district.getRowCount();j++){
                tazI = (int)tazMatrix.getExternalNumber(i);
                tazJ = (int)tazMatrix.getExternalNumber(j);
                districtI = (int)district.getValueAt(tazI);
                districtJ = (int)district.getValueAt(tazJ);
                
                mean = dMatrix.getValueAt(districtI,districtJ);
                count = (int)countMatrix.getValueAt(districtI,districtJ);
                float tempSum = mean * count;
                mean = (tempSum + tazMatrix.getValueAt(tazI,tazJ))/(count +1);
                dMatrix.setValueAt(districtI, districtJ, mean);
                countMatrix.setValueAt(districtI,districtJ,count + 1);
            }
        }
        
        return dMatrix;
    }
    
    private Matrix squeezeMin(Matrix tazMatrix, Matrix dMatrix,  Matrix countMatrix, ColumnVector district){
        float currentMin = 0;
        float nextValue = 0;
        int count = 0;
        
        for(int i=0;i<district.getRowCount();i++){
            for(int j=0;j<district.getRowCount();j++){
                tazI = (int)tazMatrix.getExternalNumber(i);
                tazJ = (int)tazMatrix.getExternalNumber(j);
                districtI = (int)district.getValueAt(tazI);
                districtJ = (int)district.getValueAt(tazJ);
                
                count = (int)countMatrix.getValueAt(districtI,districtJ);
                currentMin = dMatrix.getValueAt(districtI,districtJ);
                nextValue = tazMatrix.getValueAt(tazI,tazJ);
                if(count==0 || nextValue<currentMin)
                    currentMin=nextValue;
                dMatrix.setValueAt(districtI, districtJ, currentMin);
                countMatrix.setValueAt(districtI,districtJ,count + 1);
            }
        }
        return dMatrix;
    }
    
    private Matrix squeezeMax(Matrix tazMatrix, Matrix dMatrix, Matrix countMatrix, ColumnVector district){
        float currentMax = 0;
        float nextValue = 0;
        int count = 0;
                
        for(int i=0;i<district.getRowCount();i++){
            for(int j=0;j<district.getRowCount();j++){
                tazI = (int)tazMatrix.getExternalNumber(i);
                tazJ = (int)tazMatrix.getExternalNumber(j);
                districtI = (int)district.getValueAt(tazI);
                districtJ = (int)district.getValueAt(tazJ);
                
                count = (int)countMatrix.getValueAt(districtI,districtJ);
                currentMax = dMatrix.getValueAt(districtI,districtJ);
                nextValue = tazMatrix.getValueAt(tazI,tazJ);
                if(count==0 || nextValue>currentMax)
                    currentMax=nextValue;
                dMatrix.setValueAt(districtI, districtJ, currentMax);
                countMatrix.setValueAt(districtI,districtJ,count + 1);
            }
        }
        return dMatrix;
    }
     
    /**
     * Squeezes taz matrix to district level
     * @param m the taz matrix
     * @param district - a ColumnVector of districts
     * @param matrixReturnType - The type of district matrix to return ("SUM" "MEAN" "MIN" or "MAX"
     * @return dmatrix - the district matrix
     **/
    public Matrix squeeze(Matrix m,ColumnVector district, String matrixReturnType){        
        
        int[] uniqueBetaZoneList =getUniqueZoneList(district);
        Matrix dMatrix = new Matrix("district matrix", "district matrix", uniqueBetaZoneList.length-1, uniqueBetaZoneList.length-1);
        dMatrix.setExternalNumbers(uniqueBetaZoneList);
        

        if(matrixReturnType == "SUM")
            dMatrix = squeezeSum(m, dMatrix, district);
        else if(matrixReturnType == "MEAN"){       
            Matrix countMatrix = new Matrix("count matrix", "count matrix", uniqueBetaZoneList.length-1, uniqueBetaZoneList.length-1);
                    countMatrix.setExternalNumbers(uniqueBetaZoneList);
            squeezeMean(m, dMatrix, countMatrix, district);
        }
        else if(matrixReturnType == "MIN"){
            Matrix countMatrix = new Matrix("count matrix", "count matrix", uniqueBetaZoneList.length-1, uniqueBetaZoneList.length-1);
                    countMatrix.setExternalNumbers(uniqueBetaZoneList);
            squeezeMin(m, dMatrix, countMatrix, district);
        }
        else if(matrixReturnType == "MAX"){
            Matrix countMatrix = new Matrix("count matrix", "count matrix", uniqueBetaZoneList.length-1, uniqueBetaZoneList.length-1);
                    countMatrix.setExternalNumbers(uniqueBetaZoneList);
            squeezeMax(m, dMatrix, countMatrix, district);
        }
        
        return dMatrix;
    }
    
    public Matrix expand(Matrix districtMatrix, ColumnVector taz, ColumnVector district, int[] uniqueBetaZoneList){
        
        int[] externalBZoneNumbers = new int[district.size()+1];
        for (int i=1; i <= district.size(); i++) {
            externalBZoneNumbers[i] = i;
        }
        district.setExternalNumbers(externalBZoneNumbers);
        
        int[] externalAZoneNumbers = new int[taz.size()+1];
        for (int i=1; i <= taz.size(); i++) {
            externalAZoneNumbers[i] = i;
        }
        taz.setExternalNumbers(externalAZoneNumbers);
        
        Matrix expandedMatrix = new Matrix("expanded matrix", "expanded matrix", uniqueBetaZoneList.length-1, uniqueBetaZoneList.length-1);
                expandedMatrix.setExternalNumbers(uniqueBetaZoneList);
        
        float value = 0;
        for(int i=1;i<=district.size();i++){
            for(int j=1;j<=district.size();j++){
                value = districtMatrix.getValueAt((int) district.getValueAt(i),(int) district.getValueAt(j));
                expandedMatrix.setValueAt((int) taz.getValueAt(i),(int) taz.getValueAt(j),value);
            }
        }
        
        return expandedMatrix;
    }

     
    public static TableDataSet loadTableDataSet(ResourceBundle rb, String getProperty) {
        
        String tazToDistrictFile = "d:/tlumip_data/azonebzone";/*ResourceUtil.getProperty(rb, getProperty);*/
        
        try {
            String fileName = tazToDistrictFile + ".csv";
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fileName));
            return table;
        } catch (IOException e) {
            logger.fatal("Can't find taz to district file");
            e.printStackTrace();
        }
        return null;
    }
    
    /** 
     * getExternalNumberArray takes a column of a TableDataSet and puts them into an array for
     * purposes of setting external numbers.  The column should be the alpha zones.  These will
     * be the external numbers for the betazone ColumnVector 
     * @param alphaToBeta - The TableDataSet of containing the alpha zone to beta zone conversion
     * @param columnName - should be "AZone" if using the method to get external numbers for the 
     * betazone ColumnVector
     * @return 
     */
    public static int[] getExternalNumberArray(TableDataSet alphaToBeta, String columnName){
        int[] columnArray = alphaToBeta.getColumnAsInt(alphaToBeta.getColumnPosition(columnName));
        int[] externalNumberArray = new int[columnArray.length+1];
        for(int i=1;i<externalNumberArray.length;i++)
        externalNumberArray[i] = columnArray[i-1];
        return externalNumberArray;
    }
    
    /**
     * getSqueezedMatrix takes an alphazone matrix and ouputs a squeezed matrix based on 
     * the alphaToBeta TableDataSet
     * 
     * @param alphaToBeta - TableDataSet of alphazone to betazone mapping
     * @param alphaMatrix -  matrix to be squeezed
     * @param squeezeType - Type of squeeze to perform (SUM, MEAN, MIN, or MAX)
     * @return
     */
    public Matrix getSqueezedMatrix(TableDataSet alphaToBeta, Matrix alphaMatrix, String squeezeType){
        ColumnVector bZone = new ColumnVector(alphaToBeta.getColumnAsFloat(alphaToBeta.getColumnPosition("BZone")));
        int[] bZoneExternals = getExternalNumberArray(alphaToBeta,"AZone");
        bZone.setExternalNumbers(bZoneExternals);
        Matrix m = squeeze(alphaMatrix, bZone,squeezeType);
        return m;
    }

    public static void main (String[] args) throws Exception {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        AlphaToBetaData aToB = new AlphaToBetaData();
        TableDataSet table = loadTableDataSet(rb, "alphatobeta");
                
        String path = ResourceUtil.getProperty(rb, "transitSkims.path");
        String writePathName = ResourceUtil.getProperty(rb, "modeChoiceLogsumsWrite.path");
        
        for(int i=5;i<mName.length;i++){
            logger.info("Reading skims "+mName[i]);
            MatrixReader opDistReader= MatrixReader.createReader(MatrixType.ZIP,new File(path+mName[i]+".zip"));                   
            Matrix m = opDistReader.readMatrix(path+mName[i]+".zip");
            
            String fullPath = writePathName+"beta"+mName[i]+".zip";
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(fullPath));
            mw.writeMatrix(aToB.getSqueezedMatrix(table,m,"MEAN"));            
        }
    }
         
}
