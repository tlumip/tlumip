package com.pb.despair.pt.daf;

import java.util.logging.Logger;
import java.util.Date;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.MatrixCompression;
import com.pb.despair.pt.ActivityPurpose;
import com.pb.despair.pt.CreateModeChoiceLogsums;
import com.pb.despair.pt.LogsumManager;
import com.pb.despair.pt.PTModelInputs;
import com.pb.despair.pt.TourModeChoiceModel;
import com.pb.despair.pt.TourModeParameters;

/**
 * AggregateModeChoiceLogsumsTask
 *
 * This class builds aggregate mode choice logsums for subsequent steps of the pt model and other
 * TLUMIP model components.
 * 
 * @author Freedman
 * @version Aug 10, 2004
 * 
 */
public class AggregateModeChoiceLogsumsTask  extends MessageProcessingTask {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.daf");

    CreateModeChoiceLogsums mcLogsumCalculator = new CreateModeChoiceLogsums();
    TourModeChoiceModel tmcm = new TourModeChoiceModel();
    LogsumManager logsumManager;
    String matrixWriterQueue = "MatrixWriterQueue";

    /**
     * OnStart method sets up model
     */
    public void onStart() {
        logger.info( "***" + getName() + " started");

    }
    /**
     * A worker bee that will process a block of households.
     *
     */
    public void onMessage(Message msg) {
        logger.info("********" + getName() + " received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " at " + new Date());

        if (msg.getId().equals(MessageID.CREATE_MC_LOGSUMS))
            createMCLogsums(msg);
    }      
    
    /**
     * The work happens here: create mode choice logsums for the market segment and 
     * purpose in the message.
     * 
     * @param msg
     */
    public void createMCLogsums(Message msg) {
        logger.fine("Free memory before creating MC logsum: " +
            Runtime.getRuntime().freeMemory());

        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        boolean collapse = ((Boolean) msg.getValue("collapse")).booleanValue();
        AlphaToBeta a2b = (AlphaToBeta) msg.getValue("alphaBetaMap");

        //Creating the ModeChoiceLogsum Matrix
        logger.info("Creating ModeChoiceLogsumMatrix for purpose: " + purpose +
            " segment: " + segment);

        TourModeParameters theseParameters = (TourModeParameters) PTModelInputs.tmpd.getTourModeParameters(ActivityPurpose.getActivityPurposeValue(
                    purpose.charAt(0)));
        long startTime = System.currentTimeMillis();
        Matrix m = mcLogsumCalculator.setModeChoiceLogsumMatrix(PTModelInputs.tazs,
                theseParameters, purpose.charAt(0), segment.intValue(),
                PTModelInputs.getSkims(), new TourModeChoiceModel());
        logger.fine("Created ModeChoiceLogsumMatrix in " +
            ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");

        //Collapse the required matrices

        if (collapse) {
            logger.info("Collapsing ModeChoiceLogsumMatrix for purpose: " + purpose +
                " segment: " + segment);
            collapseMCLogsums(m,a2b);
        }

        //Sending message to TaskMasterQueue
        msg.setId(MessageID.MC_LOGSUMS_CREATED);
        msg.setValue("matrix", m);
        sendTo(matrixWriterQueue, msg);
    }

    /**
     * Collapse the logsums in the alpha zone matrix to beta zones.  The
     * collapsed matrix will be send to the fileWriterQueue.
     *
     * @param m  Logsum matrix
     * @param a2b AlphaToBeta mapping
     */
    public void collapseMCLogsums(Matrix m, AlphaToBeta a2b){
            MatrixCompression mc = new MatrixCompression(a2b);

            Matrix compressedMatrix = mc.getCompressedMatrix(m,"MEAN");

        //Need to do a little work to get only the purpose/segment part out of the name
            String newName = m.getName();
            newName = newName.replaceAll("ls","betals");
        logger.info("Old name: " + m.getName() + " New name: " + newName);
            compressedMatrix.setName(newName);

            //Sending message to TaskMasterQueue
            Message msg = createMessage();
            msg.setId(MessageID.MC_LOGSUMS_COLLAPSED);
            msg.setValue("matrix", compressedMatrix);
            sendTo(matrixWriterQueue, msg);

    }


}
