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
import com.pb.common.util.ObjectUtil;
import com.pb.despair.pt.PTResults;
import com.pb.despair.pt.PTSummarizer;
import com.pb.despair.pt.PTHousehold;

/**
 * All tasks involving writing files to disk are sent to the file writer
 * @author hansens
 *
 */

public class PTDafWriter extends MessageProcessingTask{

    ResourceBundle rb;
    PTResults results;

    public void onStart() {
        logger.info( "***" + getName() + " started");
        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        String scenarioName = null;
        int timeInterval = -1;
        String pathToRb = null;
        try {
            logger.info("Reading RunParams.txt file");
            reader = new BufferedReader(new FileReader(new File("/models/tlumip/daf/RunParams.txt")));
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
        results = new PTResults(rb);
        logger.info("Creating output files");  //files will be written to after block of households
        results.createFiles();                //is complete.  The files will be closed once all households
    }                                         //have been processed.

    public void onMessage(Message msg) {
        logger.info( getName() + " received messageId=" + msg.getId() + " message from=" + msg.getSender() +
                " @time="+ new Date());
        logger.info("Free memory before writing logsum to disk: "+Runtime.getRuntime().freeMemory());
        if(msg.getId().equals(MessageID.MC_LOGSUMS_CREATED))
            writeMatrix(msg,"modeChoiceLogsumsWrite.path");         //BINARY-ZIP
//            writeBinaryMatrix(msg,"modeChoiceLogsumsWrite.path");

        else if(msg.getId().equals(MessageID.DC_LOGSUMS_CREATED))
            writeMatrix(msg,"dcLogsumWrite.path");                //BINARY-ZIP
//            writeBinaryMatrix(msg,"dcLogsumWrite.path");
        else if(msg.getId().equals(MessageID.DC_EXPUTILITIES_CREATED))
            writeMatrix(msg,"dcExpUtilitesWrite.path");          //BINARY-ZIP
//            writeBinaryMatrix(msg,"dcExpUtilitesWrite.path");

        else if(msg.getId().equals(MessageID.HOUSEHOLDS_PROCESSED)){
            //First create a new message (minus the household block) and
            //forward the message to the
            //Master task so that the master can keep track of the
            //number of households processed and if it needs to send more
            //households to the workers.
            Message masterMsg = createMessage();
            masterMsg.setId(MessageID.HOUSEHOLDS_PROCESSED);
            masterMsg.setValue("nHHs", (Integer)msg.getValue("nHHs"));
            masterMsg.setValue("sendMore",(Integer)msg.getValue("sendMore"));
            masterMsg.setValue("WorkQueue",(String)msg.getValue("WorkQueue"));
            sendTo("TaskMasterQueue",masterMsg);

            //Now send hhs off for processing.
            PTHousehold[] hhs = (PTHousehold[])msg.getValue("households");
            summarizeTours(hhs);
            writeResults(hhs);
        }
        else if(msg.getId().equals(MessageID.ALL_HOUSEHOLDS_PROCESSED)){
            PTSummarizer.writeTourSummaryToFile(ResourceUtil.getProperty(rb,"tourSummary.file"));
            results.close();
            Message filesWritten = createMessage();
            filesWritten.setId(MessageID.ALL_FILES_WRITTEN);
            sendTo("TaskMasterQueue",filesWritten);
        }


        //else if(msg.getId().equals(MessageID.LABOR_FLOWS_CREATED)){
            //writeMatrix(msg, "laborFlows.path");
            //TODO temp change
        //    msg.setValue("matrix",null);
        //    sendTo("TaskMasterQueue",msg);
        //}
    }
    
    private void writeMatrix(Message msg, String pathName){

        Matrix m = (Matrix)(msg.getValue("matrix"));
        if(m != null){
            String path = ResourceUtil.getProperty(rb, pathName);
            long startTime = System.currentTimeMillis();
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(path + m.getName() + ".zip"));  //Open for writing
            mw.writeMatrix(m);

            logger.info("Wrote Matrix "+m.getName()+" to disk in "+(System.currentTimeMillis()-startTime)/1000+" seconds");
            logger.info("Free memory after writing Matrix : "+m.getName()+" memory: "+Runtime.getRuntime().freeMemory());
            msg.setValue("matrix",null);
        }
        sendTo("TaskMasterQueue",msg);
        m=null;
    }

    private void writeBinaryMatrix(Message msg, String pathName){
        Matrix m = (Matrix)(msg.getValue("matrix"));
        if(m!=null){
            String path = ResourceUtil.getProperty(rb,pathName);
            long startTime = System.currentTimeMillis();
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.BINARY,new File(path + m.getName() + ".binary"));
            mw.writeMatrix(m);

            logger.info("Wrote Matrix "+m.getName()+".binary to disk in "+(System.currentTimeMillis()-startTime)/1000+" seconds");
            logger.info("Free memory after writing Matrix : "+m.getName()+" memory: "+Runtime.getRuntime().freeMemory());
            msg.setValue("matrix",null);
        }
        sendTo("TaskMasterQueue",msg);
        m=null;
    }

    /**
     * This method will summarize the Tours as follows:
     *  1.  tours by activity purpose and mode ("w,b,c,o,r,s" : "driver,passenger,walk,bike,wtransit,transitp,ptransti,dtransit")
     *  2.  average tour distance by purpose and mode (purposes are numbered 1-6  : modes are numbered 1-8
     *  3.  work tours by work market segment and mode ("0-8" : see modes above)
     *  4.  non-work tours by purpose, non-work segment and mode ("b,c,o,r,s" : "0-8" : see modes above)
     *  5.  trips on work tours by work market segment and mode ("0-8" : above modes + drive,shared2,shared3+)
     *  6.  trips on non-work tours by purpose, non-work segments and mode ("0-8" : modes are numbered 1-3)
     *  7.  average tour distance for work tours by work market segment and mode
     *  8.  average tour distance for non-work tours by purpose, non-work segment and mode
     * This method will be called after each block of hhs has been operated on.
     */
    private void summarizeTours(PTHousehold[] hhs) {
        PTSummarizer.summarizeTours(hhs);
        hhs = null;
    }//end of Summarize Tours

    /**
     * Get the households from the message and send the
     * households to the writeResults method of the results object.
     *
     */
    private void writeResults(PTHousehold[] hhs) {
        results.writeResults(hhs);
        hhs = null;
    }

}
