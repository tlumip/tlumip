package com.pb.despair.pt.daf;

import java.util.logging.Logger;
import java.util.Date;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.MatrixCompression;
import com.pb.despair.pt.*;

/**
 * CalculateModeChoiceLogsumTask
 *
 * This class builds aggregate mode choice logsums for subsequent steps of the pt model and other
 * TLUMIP model components.
 * 
 * @author Freedman
 * @version Aug 10, 2004
 * 
 */
public class CalculateModeChoiceLogsumTask  extends MessageProcessingTask {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.daf");
    boolean debug = false;

    CreateModeChoiceLogsums mcLogsumCalculator = new CreateModeChoiceLogsums();
    String matrixWriterQueue = "MatrixWriterQueue";

    /**
     * OnStart method sets up model
     */
    public void onStart() {
        logger.info( "***" + getName() + " started");

    }

    public void onMessage(Message msg) {
        logger.info("********" + getName() + " received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " at " + new Date());

        char purpose = ((Character) msg.getValue("purpose")).charValue();
        int segment = ((Integer) msg.getValue("segment")).intValue();
        TourModeParameters modeParameters = (TourModeParameters) msg.getValue("tourModeParameters");
        boolean collapse = ((Boolean) msg.getValue("collapse")).booleanValue();

        //Creating the ModeChoiceLogsum Matrix
        logger.info("Creating ModeChoiceLogsumMatrix for purpose: " + purpose +
            " segment: " + segment);

        long startTime = System.currentTimeMillis();
        Matrix m = mcLogsumCalculator.setModeChoiceLogsumMatrix(PTModelInputsNew.TAZS,
                modeParameters, purpose, segment, PTModelInputsNew.SKIMS, new TourModeChoiceModel());
        logger.fine("Created ModeChoiceLogsumMatrix in " +
            ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");

        //Collapse the required matrices

        if (collapse) {
            logger.info("Collapsing ModeChoiceLogsumMatrix for purpose: " + purpose +
                " segment: " + segment);
            collapseMCLogsums(m,PTModelInputsNew.A2B_MAP);
        }

        //Sending message to MatrixWriterQueue so that the matrix can be written to disk
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
    private void collapseMCLogsums(Matrix m, AlphaToBeta a2b){
            MatrixCompression mc = new MatrixCompression(a2b);

            Matrix compressedMatrix = mc.getCompressedMatrix(m,"MEAN");

            //Need to do a little work to get only the purpose/segment part out of the name
            String newName = m.getName().replaceAll("ls","betals");
            if(debug) logger.info("Old name: " + m.getName() + " New name: " + newName);
            compressedMatrix.setName(newName);

            //Sending message to MatrixWriterQueue so that the compressed matrix can be written to disk
            Message msg = createMessage();
            msg.setId(MessageID.MC_LOGSUMS_COLLAPSED);
            msg.setValue("matrix", compressedMatrix);
            sendTo(matrixWriterQueue, msg);

    }


}
