package com.pb.despair.pt.daf;

import java.util.Date;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pt.PTResults;
import com.pb.despair.pt.PTSummarizer;
import com.pb.despair.pt.PTHousehold;

/**
 * All tasks involving writing files to disk are sent to the file writer
 * @author hansens
 *
 */

public class PTResultsWriter extends MessageProcessingTask{

	boolean firstMessage = true;
    PTResults results;

    public void onStart() {
        logger.info( "***" + getName() + " started");
    }

    
    public void onMessage(Message msg) {
        logger.info( getName() + " received messageId=" + msg.getId() + " message from=" + msg.getSender() +
                " @time="+ new Date());
        
        
        if ( firstMessage ) {

            results = new PTResults(PTMatrixWriter.rb);
            logger.info("Creating output files");  // files will be written to after block of households
            results.createFiles();                 // is complete.  The files will be closed once all households
            									   // have been processed.
            firstMessage = false;
        }
     
        
        if(msg.getId().equals(MessageID.HOUSEHOLDS_PROCESSED)){
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
            
            hhs = null;
        }
        else if(msg.getId().equals(MessageID.ALL_HOUSEHOLDS_PROCESSED)){
            PTSummarizer.writeTourSummaryToFile(ResourceUtil.getProperty(PTMatrixWriter.rb,"tourSummary.file"));
            results.close();
            Message filesWritten = createMessage();
            filesWritten.setId(MessageID.ALL_FILES_WRITTEN);
            sendTo("TaskMasterQueue",filesWritten);
        }

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
    }//end of Summarize Tours

    /**
     * Get the households from the message and send the
     * households to the writeResults method of the results object.
     *
     */
    private void writeResults(PTHousehold[] hhs) {
        results.writeResults(hhs);
    }

}
