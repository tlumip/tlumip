package com.pb.despair.model;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;

import java.io.File;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * A class that reads in peak auto skims and facilitates zone pair disutility calculations
 * @author John Abraham, Joel Freedman
 * 
 */
public class SomeSkims extends TransportKnowledge implements TravelAttributesInterface {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pi");

    private ArrayList matrixList = new ArrayList();
    public Matrix[] matrices = new Matrix[0];
    private ArrayList matrixNameList = new ArrayList();

    String my1stPath;
    String my2ndPath;


    public SomeSkims() {
        my1stPath = System.getProperty("user.dir");
    }

    public SomeSkims(String firstPath, String secondPath) {
        my1stPath = firstPath;
        my2ndPath =secondPath;
    };
    
    public Matrix getMatrix(String name) {
        int place = matrixNameList.indexOf(name);
        if (place >=0) return matrices[place];
        return null;
    }

    public void addZipMatrix(String matrixName) {
        if (matrixNameList.contains(matrixName)) {
            logger.warning("SomeSkims already contains matrix named "+matrixName+", not reading it in again");
        } else {
            File skim = new File(my1stPath+matrixName+".zip");
            if(!skim.exists()){
                skim = new File(my2ndPath+matrixName+".zip");
                if(!skim.exists()) logger.severe("Could not find "+ matrixName+".zip in either the pt or the ts directory");
            }
            matrixList.add(new ZipMatrixReader(skim).readMatrix());
            matrixNameList.add(matrixName);
            matrices = (Matrix[]) matrixList.toArray(matrices);
        }
        
        logger.info("finished reading zipmatrix of skims "+matrixName+" into memory");
    }
    
    /** Adds a table data set of skims into the set of skims that are available 
     * 
     * @param s the table dataset of skims.  There must be a column called "origin"
     * and another column called "destination"
     * @param fieldsToAdd the names of the fields from which to create matrices from, all other fields
     * will be ignored.
     */
    public void addTableDataSetSkims(TableDataSet s, String[] fieldsToAdd, int maxZoneNumber) {
        int originField = s.checkColumnPosition("origin");
        int destinationField = s.checkColumnPosition("destination");
        int[] userToSequentialLookup = new int[maxZoneNumber];
        int[] sequentialToUserLookup = new int[maxZoneNumber];
        for (int i =0; i<userToSequentialLookup.length;i++) {
                    userToSequentialLookup[i] = -1;
        }
        int[] origins = s.getColumnAsInt(originField);
        int zonesFound = 0;
        for (int o = 0;o<origins.length;o++) {
            int sequentialOrigin = userToSequentialLookup[origins[o]];
            if (sequentialOrigin == -1) {
                sequentialOrigin = zonesFound;
                zonesFound++;
                userToSequentialLookup[origins[o]]=sequentialOrigin;
                sequentialToUserLookup[sequentialOrigin] = origins[o];
            }
        }
        int[] externalZoneNumbers = new int[zonesFound+1];
        for(int i=1;i<externalZoneNumbers.length;i++){
            externalZoneNumbers[i]=sequentialToUserLookup[i-1];
        }
        //enable garbage collection
        origins = null;
        
        int[] fieldIds = new int[fieldsToAdd.length];
        for (int mNum=0; mNum < fieldsToAdd.length; mNum++) {
            if (matrixNameList.contains(fieldsToAdd[mNum])) {
                fieldIds[mNum] = -1;
                logger.warning("SomeSkims already contains matrix named "+fieldsToAdd[mNum]+", not reading it in again");
            } else {
                fieldIds[mNum] = s.getColumnPosition(fieldsToAdd[mNum]);
                if (fieldIds[mNum]<=0) {
                    logger.severe("No field named "+fieldsToAdd[mNum]+ " in skim TableDataSet "+s);
                }
            }
        }
        float [][][] tempArrays = new float[fieldsToAdd.length][zonesFound][zonesFound];
        for (int row = 1;row <= s.getRowCount();row++) {
            int origin = (int) s.getValueAt(row,originField);
            int destination = (int) s.getValueAt(row,destinationField);
            for (int entry = 0;entry<fieldIds.length;entry++) {
                if (fieldIds[entry]>0) {
                    tempArrays[entry][userToSequentialLookup[origin]][userToSequentialLookup[destination]] = s.getValueAt(row,fieldIds[entry]);
                }
            }
        }
        
        for (int matrixToBeAdded =0; matrixToBeAdded<fieldsToAdd.length; matrixToBeAdded++) {
            if (fieldIds[matrixToBeAdded]>0) {
                matrixNameList.add(fieldsToAdd[matrixToBeAdded]);
                Matrix m = new Matrix(fieldsToAdd[matrixToBeAdded],"",tempArrays[matrixToBeAdded]);
                m.setExternalNumbers(externalZoneNumbers);
                this.matrixList.add(m);
            }
        }
        
        matrices = (Matrix[]) matrixList.toArray(matrices);
        
        logger.info("Finished reading TableDataSet skims "+s+" into memory");
    }




    public double getUtility(int fromZoneUserNumber, int toZoneUserNumber, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return tp.getUtility(fromZoneUserNumber, toZoneUserNumber, this);
    }

    public double[] getUtilityComponents(int fromZoneUserNumber, int toZoneUserNumber, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return tp.getUtilityComponents(fromZoneUserNumber, toZoneUserNumber, this);
    }

    
    /* (non-Javadoc)
     * @see com.pb.despair.model.TransportKnowledge#getUtility(com.pb.despair.model.AbstractTAZ, com.pb.despair.model.AbstractTAZ, com.pb.despair.model.TravelUtilityCalculatorInterface, boolean)
     */
    public double getUtility(AbstractTAZ from, AbstractTAZ to, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return getUtility(from.getZoneUserNumber(), to.getZoneUserNumber(), tp, useRouteChoice);
    }

    /**
     * @param string
     * @return
     */
    public int getMatrixId(String string) {
        
        return matrixNameList.indexOf(string);
    }

    /* (non-Javadoc)
     * @see com.pb.despair.model.TransportKnowledge#getUtilityComponents(com.pb.despair.model.AbstractTAZ, com.pb.despair.model.AbstractTAZ, com.pb.despair.model.TravelUtilityCalculatorInterface, boolean)
     */
    public double[] getUtilityComponents(AbstractTAZ from, AbstractTAZ to, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return getUtilityComponents(from.getZoneUserNumber(), to.getZoneUserNumber(), tp, useRouteChoice);
    }



    /**
     * @param my1stPath The my1stPath to set.
     */
    public void setMy1stPath(String my1stPath) {
        this.my1stPath = my1stPath;
    }

    /**
     * @param my2ndPath The my2ndPath to set.
     */
    public void setMy2ndPath(String my2ndPath) {
        this.my2ndPath = my2ndPath;
    }

};
