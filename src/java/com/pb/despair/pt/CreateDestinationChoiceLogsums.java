package com.pb.despair.pt;

import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import java.util.logging.Logger;
import java.util.ArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.ResourceBundle;


/** 
 * A class that creates destination choice logsums by
 * 9 market segments for all trip purposes
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class CreateDestinationChoiceLogsums {

    static final int TOTALSEGMENTS = 9;
    static final int TOTALPURPOSES = 7;
    static final String[] purposes = {"c1","c2","c3","s","r","o","b"};  //work,school,shop,recreate,other,workbased

    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");
    boolean debug = false;
    
    LogitModel tourDCModel;
    
    protected Matrix expUtilities;

    /**
     * Default constructor
     *
     */
    public CreateDestinationChoiceLogsums(){

    }
    /** 
     * Build a logit destination choice model: note that the tazs will be cloned for inclusion in the model
     * @param tazs
     */
    public void buildModel(TazData tazs){
        
        tourDCModel = new LogitModel("tourDCModel",tazs.tazData.size());
        TazData modelTazs = (TazData) tazs.clone();
        Enumeration tourDestinationEnum=modelTazs.tazData.elements();
         while(tourDestinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) tourDestinationEnum.nextElement();
             tourDCModel.addAlternative(destinationTaz);
         }
        //create a Matrix to store exponentiated utilities from all origins to all destinations
        expUtilities = new Matrix(tazs.tazData.size(),tazs.tazData.size());
        int[] tazNumbers = tazs.getExternalNumberArray();
        expUtilities.setExternalNumbers(tazNumbers);        

    }
    
    /*
     * Get the exponentiated utilities matrix for this purpose.
     */
    public Matrix getExpUtilities(){
        return expUtilities;
    }

    /**
     * Calculates the logsum for an origin Taz Number.  Stores exponentiated utilities in the 
     * Matrix expUtilities.
     * @param purpose
     * @param segment
     * @param logsumMatrix
     * @param originTazNumber
     * @param theseParameters
     * @return
     * @throws FileNotFoundException
     */ 
    public float getLogsum(int purpose,
                           int segment, 
                           Matrix logsumMatrix,
                           int originTazNumber,
                           TourDestinationParameters theseParameters) throws FileNotFoundException{
        
        
        //Loop through all destinations
        ArrayList tazs = tourDCModel.getAlternatives();
        for(int i=0;i<tazs.size();++i){
            Taz destinationTaz = (Taz) tazs.get(i);
            
            //calculate the utility
            destinationTaz.calcTourDestinationUtility(purpose, segment, theseParameters,
                                                      logsumMatrix.getValueAt(originTazNumber, destinationTaz.zoneNumber));
            
                              
        } //end destination loop
        tourDCModel.computeAvailabilities();
        
        float logsum =(float)tourDCModel.getUtility();
        
        //Loop through all destinations
        tazs = tourDCModel.getAlternatives();
        
        double[] eu = tourDCModel.getExponentiatedUtilities();

        for(int i=0;i<tazs.size();++i){
            Taz destinationTaz = (Taz) tazs.get(i);
            
            if(destinationTaz.isAvailable())
                expUtilities.setValueAt(originTazNumber,destinationTaz.zoneNumber,(float)eu[i]);              
        } //end destination loop
        
        return  logsum;
    }
    
    /**
     * Writes the logsum matrix to a zip file
     * @param thisMatrix
     */
    public void writeDestinationChoiceLogsumsMatrix(ResourceBundle rb, Matrix thisMatrix){

        logger.info("Writing DC Logsum Matrix; "+thisMatrix.getName()+".zip");
        //Get path and name
        String path = ResourceUtil.getProperty(rb, "dcLogsumWrite.path");
        String mName = thisMatrix.getName()+".zip";
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(path+mName));  //Open for writing
        mw.writeMatrix(thisMatrix);           
    }

    /**
     * Writes the logsum matrix to a binary file
     * @param thisMatrix
     */
    public void writeDestinationChoiceLogsumsMatrixAsBinary(ResourceBundle rb, Matrix thisMatrix){

        logger.info("Writing DC Logsum Matrix; "+thisMatrix.getName()+".binary");
        //Get path and name
        String path = ResourceUtil.getProperty(rb, "dcLogsumWrite.path");
        String mName = thisMatrix.getName()+".binary";
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.BINARY,new File(path+mName));  //Open for writing
        mw.writeMatrix(thisMatrix);
    }

    /**
     * Writes the exponentiated utilities matrix to a zip file
     */
    public void writeDestinationChoiceExpUtilitiesMatrix(ResourceBundle rb){

        logger.info("Writing DC Exponentiated Utilities Matrix; "+expUtilities.getName()+".zip");
        //Get path and name
        String path = ResourceUtil.getProperty(rb, "dcExpUtilitesWrite.path");
        String mName = expUtilities.getName() + ".zip";
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(path+mName));  //Open for writing
        mw.writeMatrix(expUtilities);           
    }

    /**
     * Writes the exponentiated utilities matrix to a zip file
     */
    public void writeDestinationChoiceExpUtilitiesBinaryMatrix(ResourceBundle rb){

        logger.info("Writing DC Exponentiated Utilities Matrix; "+expUtilities.getName()+".binary");
        //Get path and name
        String path = ResourceUtil.getProperty(rb, "dcExpUtilitesWrite.path");
        String mName = expUtilities.getName() + ".binary";
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.BINARY,new File(path+mName));  //Open for writing
        mw.writeMatrix(expUtilities);
    }

    /**
     * Calculates the DC logsum for the purpose and segment.  Also stores exponentiated
     * utilities in the Matrix expUtilities.  Unavailable destinations will be set to 0.
     * @param thisPurpose
     * @param logsumMatrix
     * @return
     */
    public ColumnVector getDCLogsumVector(TazData tazs, TourDestinationParametersData tdp, String thisPurpose, int marketSegment, Matrix logsumMatrix){
        
        int segment;
        if(thisPurpose.length()==2){
            String second = new String(thisPurpose.substring(1,2));
            segment = (Integer.valueOf(second)).intValue();
        }
        else segment = 1;
        
        int purpose = ActivityPurpose.getActivityPurposeValue(thisPurpose.charAt(0));
        
        //create a ColumnVector to store destination choice logsums (one logsum for every origin taz)
        ColumnVector vector = new ColumnVector(tazs.tazData.size());
        int[] tazNumbers = tazs.getExternalNumberArray();
        vector.setName(thisPurpose+marketSegment+"dcls");  //the extension will be added when the matrix is written out.
      	vector.setExternalNumbers(tazNumbers);
        
        //set the name of the exponentiated utilities matrix
        expUtilities.setName(thisPurpose+marketSegment+"dceu"); //extension will be added when the matrix is written out.

        TourDestinationParameters theseParameters = 
            (TourDestinationParameters) tdp.getParameters(purpose,segment);
    	//Loop on origin zones
    	for(int zone=1; zone<tazNumbers.length;zone++){
    		int originTaz=tazNumbers[zone];
            	try{ 
                    float logsum = getLogsum(purpose, segment, logsumMatrix,originTaz,theseParameters);
            		vector.setValueAt(tazNumbers[zone],logsum ); 
            	}catch(Exception e){
            		logger.info("Error creating logsums");
            		e.printStackTrace();
            	}      
    	}
      	return vector;
    }
    
    public static void main (String[] args){
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        CreateDestinationChoiceLogsums createDCLogsums = new CreateDestinationChoiceLogsums();
//      Read in TazData
        TazData tazs = new TazData();
        tazs.readData(rb,"tazData.file");
        //collapse employment categories for tazs
        logger.info("Collapsing employment");
        
        Enumeration tazEnum=tazs.tazData.elements();
        while(tazEnum.hasMoreElements()){
              ((Taz) tazEnum.nextElement()).collapseEmployment();
        }
        createDCLogsums.buildModel(tazs);
        
        //read the tourDestinationParameters from csv to a TableDataSet
        logger.info("Reading tour destination parameters");
        TourDestinationParametersData tdp = new TourDestinationParametersData();
        tdp.readData(rb,"tourDestinationParameters.file");
        StopDestinationParametersData sdp = new StopDestinationParametersData();
        sdp.readData(rb,"stopDestinationParameters.file");
        tazs.collapseEmployment(tdp,sdp);
        
        //try{
          String columnName;
          logger.info("creating tour destination choice logsums");
         
          for(int purpose=1;purpose<2/*purposes.length*/;++purpose){ 
              String thisPurpose=purposes[purpose];
                
              for(int segment=0;segment<5/*TOTALSEGMENTS*/;segment++){
                  ResourceBundle despairRb = ResourceUtil.getResourceBundle( "despair" );
                  String path = ResourceUtil.getProperty(despairRb, "Model.skimPath");
                  String mName= new String(path+ thisPurpose.substring(0,1) + new Integer(segment).toString()
                           + new String("ls.zip"));
                                  
                  logger.info("Opening and reading mc logsum file "+mName);
                  MatrixReader lsReader= MatrixReader.createReader(MatrixType.ZIP,new File(mName));                   
                  Matrix logsumMatrix = lsReader.readMatrix(mName);
                  
                  ColumnVector dcLogsumVector = createDCLogsums.getDCLogsumVector(tazs, tdp, thisPurpose,segment,logsumMatrix);
                  createDCLogsums.writeDestinationChoiceLogsumsMatrix(rb, dcLogsumVector);
              }
          }   
    } //end main   
}
