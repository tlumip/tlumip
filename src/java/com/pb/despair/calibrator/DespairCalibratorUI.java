/*
 * Created on Jun 14, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.awt.FileDialog;
import java.io.File;
import java.text.DecimalFormat;

import javax.swing.JFileChooser;

import com.hbaspecto.calibrator.BatchFileRunner;
import com.hbaspecto.calibrator.BatchSemaphoreRunner;
import com.hbaspecto.calibrator.CalibrationStrategyUI;
import com.hbaspecto.calibrator.LeastSquaresLinearized;
import com.hbaspecto.calibrator.ModelInputsAndOutputs;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.TableDataSetCollection;

/**
 * @author jabraham
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class DespairCalibratorUI extends CalibrationStrategyUI {

    /**
	 *  
	 */
    public DespairCalibratorUI() {
        super();
    }
    
    static String[] myArgs;

    /**
	 * main entrypoint - starts the part when it is run as an application
	 * 
	 * @param args
	 *            java.lang.String[]
	 */
    public static void main(String[] args){
        myArgs= args;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(myArgs);
            }
        });
    }
    
    public static void createAndShowGUI(java.lang.String[] args) {
        try {
            DespairCalibratorUI calibratorUI = new DespairCalibratorUI("Calibrator");
            calibratorUI.addParameterType(TableDataSetParameter.class);
            calibratorUI.addTargetType(TableDataSetTarget.class);
            calibratorUI.addTargetType(TableDataSetSingleKeyFieldRelationship.class);
            calibratorUI.addTargetType(TableDataSetMultipleKeyFieldRelationship.class);
            calibratorUI.setDefaultCloseOperation(EXIT_ON_CLOSE);
            calibratorUI.setVisible(true);
        } catch (Throwable exception) {
            System.err.println("Exception occurred in main() of DespairCalibrator");
            exception.printStackTrace(System.out);
        }
    }

    /**
	 * @param title
	 */
    public DespairCalibratorUI(String title) {
        super(title);
    }

    /**
	 * Selecting the scenario-year for the model run.
	 */
    public void pickModelRunner() {
        JFileChooser fc = new JFileChooser("Select batch file in the directory with the data");
        int returnVal = fc.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // user cancelled
            return;
        }
        File file = fc.getSelectedFile();
        File myDirectory = file.getParentFile();
        LeastSquaresLinearized strat = (LeastSquaresLinearized) getStrategy();
        CSVFileReader r = new CSVFileReader();
        CSVFileWriter w = new CSVFileWriter();
        r.setMyDirectory(myDirectory);
        w.setMyDirectory(myDirectory);
        w.setMyDecimalFormat(new GeneralDecimalFormat("0.############E0",10000,.001));
        
        TableDataSetCollection myData = new TableDataSetCollection(r,w);

        ModelInputsAndOutputs y = new PecasDirectoryInputsAndOutputs(myDirectory.getAbsolutePath(),myData);

        /*
		 * try { y = new ModelInputsAndOutputs( new
		 * java.io.File(fd.getDirectory()));
		 */
        strat.setScenario(y);
        BatchFileRunner runner = new BatchFileRunner(file, y);
        getStrategy().setModelRunner(runner);
        getgetParamsMI().setEnabled(true);
        updateTargetsAndParameters();
    }

}
