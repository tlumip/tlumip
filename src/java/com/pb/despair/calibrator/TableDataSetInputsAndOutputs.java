/*
 * Created on Jun 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.io.IOException;

import com.hbaspecto.calibrator.DirectoryModelInputsAndOutputs;
import com.hbaspecto.calibrator.ModelInputsAndOutputs;
import com.pb.common.datafile.*;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TableDataSetInputsAndOutputs implements DirectoryModelInputsAndOutputs {

    public final TableDataSetCollection myTableDataSetCollection;
    private String myDirectory;
    
    /**
     * 
     */
    public TableDataSetInputsAndOutputs(String directoryForBinaryFiles, TableDataSetCollection collection) {
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
    }

    /* (non-Javadoc)
     * @see com.hbaspecto.calibrator.DirectoryModelInputsAndOutputs#getDirectory()
     */
    public String getDirectory() {
        return myDirectory;
    }

}
