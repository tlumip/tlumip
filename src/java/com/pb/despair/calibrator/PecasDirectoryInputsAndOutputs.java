/*
 * Created on Jun 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.io.IOException;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import com.hbaspecto.calibrator.DirectoryModelInputsAndOutputs;
import com.hbaspecto.calibrator.ModelInputsAndOutputs;
import com.pb.common.datafile.*;
import com.pb.common.matrix.StringIndexedNDimensionalMatrix;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PecasDirectoryInputsAndOutputs implements DirectoryModelInputsAndOutputs {

    private static Logger logger = Logger.getLogger("com.hbaspecto.calibrator");
    public final TableDataSetCollection myTableDataSetCollection;
    private String myDirectory;
    HashMap loadedFiles = new HashMap();
    
    /**
     * 
     */
    public PecasDirectoryInputsAndOutputs(String directoryForBinaryFiles, TableDataSetCollection collection) {
        myTableDataSetCollection = collection;
        myDirectory = directoryForBinaryFiles;
    }

    /* (non-Javadoc)
     * @see com.hbaspecto.calibrator.ModelInputsAndOutputs#flush()
     */
    public void flush() {
        myTableDataSetCollection.flush();
    }

    /* (non-Javadoc)
     * @see com.hbaspecto.calibrator.ModelInputsAndOutputs#invalidate()
     */
    public void invalidate() throws IOException {
        myTableDataSetCollection.invalidate();
        loadedFiles.clear();
    }

    /* (non-Javadoc)
     * @see com.hbaspecto.calibrator.DirectoryModelInputsAndOutputs#getDirectory()
     */
    public String getDirectory() {
        return myDirectory;
    }

    StringIndexedNDimensionalMatrix getMatrixByName(String fileName, String tableName) {
        StringIndexedNDimensionalMatrix theOneWereLookingFor = null;
        WeakHashMap loadedTables = (WeakHashMap) loadedFiles.get(fileName);
        if (loadedTables!=null) {
            theOneWereLookingFor = (StringIndexedNDimensionalMatrix) loadedTables.get(tableName);
        }
        if (theOneWereLookingFor!=null) return theOneWereLookingFor;
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(fileName);
            java.io.ObjectInputStream in = new java.io.ObjectInputStream(fis);
            StringIndexedNDimensionalMatrix temp = null;
            loadedTables = new WeakHashMap();
            loadedFiles.put(fileName,loadedTables);
            while (fis.available()>0) { 
                Object another = in.readObject();
                if (another instanceof StringIndexedNDimensionalMatrix) {
                    temp = (StringIndexedNDimensionalMatrix) another;
                    if (temp.matrixName.equals(tableName)) {
                        theOneWereLookingFor = temp;
                    }
                    loadedTables.put(temp.matrixName,temp);
                }
            }
            in.close();
            fis.close();
        } catch (Exception e) {
            logger.severe("Cant read in stringIndexedTable "+fileName+" : "+tableName);
            throw new RuntimeException("Cant read in stringIndexedTable "+fileName+" : "+tableName);
           }
        return theOneWereLookingFor;
    }

}
