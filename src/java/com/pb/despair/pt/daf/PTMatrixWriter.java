package com.pb.despair.pt.daf;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.ResourceBundle;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;

/**
 * All tasks involving writing files to disk are sent to the file writer
 * @author hansens
 *
 */

public class PTMatrixWriter extends MessageProcessingTask{

    protected static Object lock = new Object();
    protected static boolean initialized = false;

    static ResourceBundle rb;

    String modeChoiceLogsumsWritePath = null;
    String dcLogsumsWritePath = null;
    String dcExpUtilitesWritePath = null;
    
    
    public void onStart() {
    	
        synchronized (lock) {
            logger.info( "***" + getName() + " started");

            if (!initialized) {
                //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
                //that was written by the Application Orchestrator
                
            	BufferedReader reader = null;
                String scenarioName = null;
                int timeInterval = -1;
                String pathToRb = null;
                try {
                    logger.info("Reading RunParams.txt file");
                    reader = new BufferedReader(new FileReader(new File( Scenario.runParamsFileName )));
                    scenarioName = reader.readLine();
                    logger.info("\tScenario Name: " + scenarioName);
                    timeInterval = Integer.parseInt(reader.readLine());
                    logger.info("\tTime Interval: " + timeInterval);
                    pathToRb = reader.readLine();
                    logger.info("\tResourceBundle Path: " + pathToRb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                rb = ResourceUtil.getPropertyBundle(new File(pathToRb));

                initialized = true;
            }
            
            modeChoiceLogsumsWritePath = ResourceUtil.getProperty(rb, "modeChoiceLogsumsWrite.path");
            dcLogsumsWritePath = ResourceUtil.getProperty(rb, "dcLogsumsWrite.path");
            dcExpUtilitesWritePath = ResourceUtil.getProperty(rb, "dcExpUtilitesWrite.path");

        }
        
    }

    public void onMessage(Message msg) {
        logger.info( getName() + " received messageId=" + msg.getId() + " message from=" + msg.getSender() +
                " @time="+ new Date());
        logger.info("Free memory before writing logsum to disk: "+Runtime.getRuntime().freeMemory());
        if(msg.getId().equals(MessageID.MC_LOGSUMS_CREATED))
//            writeMatrix(msg, modeChoiceLogsumsWritePath);         //BINARY-ZIP
            writeBinaryMatrix(msg,modeChoiceLogsumsWritePath);

        else if(msg.getId().equals(MessageID.MC_LOGSUMS_COLLAPSED))
            writeMatrix(msg, modeChoiceLogsumsWritePath);         //BINARY-ZIP
//            writeBinaryMatrix(msg,modeChoiceLogsumsWritePath);

        else if(msg.getId().equals(MessageID.DC_LOGSUMS_CREATED)){
//            writeMatrix(msg, dcLogsumsWritePath);                //BINARY-ZIP
            writeBinaryMatrix(msg,dcLogsumsWritePath);
        }

        else if(msg.getId().equals(MessageID.DC_EXPUTILITIES_CREATED)){
//            writeMatrix(msg, dcExpUtilitesWritePath);          //BINARY-ZIP
            writeBinaryMatrix(msg,dcExpUtilitesWritePath);
        }

    }
    
    private void writeMatrix(Message msg, String path) {

        Matrix m = (Matrix)(msg.getValue("matrix"));
        if(m != null){
            long startTime = System.currentTimeMillis();
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(path + m.getName() + ".zip"));  //Open for writing
            mw.writeMatrix(m);

            logger.info("Wrote Matrix "+m.getName()+".zip to "+ path + " in "+(System.currentTimeMillis()-startTime)/1000.0 +" seconds");
            logger.info("Free memory after writing Matrix : "+m.getName()+" memory: "+Runtime.getRuntime().freeMemory());
            msg.setValue("matrix",null);
        }
        sendTo("TaskMasterQueue",msg);
        m=null;
    }

    private void writeBinaryMatrix(Message msg, String path) {
    	
        Matrix m = (Matrix)(msg.getValue("matrix"));
        if(m!=null){
            long startTime = System.currentTimeMillis();
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.BINARY,new File(path + m.getName() + ".binary"));
            mw.writeMatrix(m);

            logger.info("Wrote Matrix "+m.getName()+".binary to "+ path + " in "+(System.currentTimeMillis()-startTime)/1000.0 +" seconds");
            logger.info("Free memory after writing Matrix : "+m.getName()+" memory: "+Runtime.getRuntime().freeMemory());
            msg.setValue("matrix",null);
        }
        sendTo("TaskMasterQueue",msg);
        m=null;
    }

    

}
