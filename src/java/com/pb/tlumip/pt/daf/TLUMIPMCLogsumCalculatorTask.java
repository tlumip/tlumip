/*
 * Copyright 2006 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.pt.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageFactory;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.daf.MCLogsumCalculatorTask;
import com.pb.models.pt.daf.MessageID;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Sep 26, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TLUMIPMCLogsumCalculatorTask extends MCLogsumCalculatorTask {

    protected static final Object localLock = new Object();
    protected static boolean localInitialized = false;
    protected static boolean runningSEAM;
    protected static ArrayList matricesToCollapse;
    protected static AlphaToBeta a2bCorrespondence;

    protected WorldZoneExternalZoneUtil wzEzUtil;
    protected MatrixCompression matrixCompression;

    public void onStart(){
        super.onStart();
        synchronized (localLock) {
            if (!localInitialized) {
                mcLogger.info(getName() + ", Initializing TLUMIP version of object");
                matricesToCollapse = ResourceUtil.getList(ptRb,
                        "sdt.matrices.for.pecas");

                //Create an alphaToBeta object to do the alpha to beta squeeze
                String fileName = ResourceUtil.getProperty(globalRb, "alpha2beta.file");
                String alphaName = globalRb.getString("alpha.name");
                String betaName = globalRb.getString("beta.name");
                logger.info("Reading " + fileName);
                a2bCorrespondence = new AlphaToBeta(new File(fileName), alphaName, betaName);

                localInitialized = true;
                mcLogger.info(getName() + ", Finished initializing tlumip child object");
            }
            wzEzUtil = new WorldZoneExternalZoneUtil(globalRb);
            matrixCompression = new MatrixCompression(a2bCorrespondence);
       }
    }

    public void onMessage(Message msg) {
        mcLogger.info(getName() + ", Received messageId=" + msg.getId()
                + " message from=" + msg.getSender() + ". MsgNum: " + msg.getIntValue("msgNum"));
        ActivityPurpose purpose = (ActivityPurpose)(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");
        String purSeg = ActivityPurpose.getActivityString(purpose)
                + segment.toString();
        Matrix logsum = createMCLogsums(purpose, segment);
        sendMCLogsumToWriter(msg, logsum);

        // Collapse the required matrices
        if (matricesToCollapse.contains(purSeg)) {
            mcLogger.info(getName()
                + ", Collapsing ModeChoiceLogsumMatrix for purpose: "
                + purpose + " segment: " + segment);
            Matrix squeezed = collapseMCLogsums(logsum, matrixWriterQueue);

            // Sending message to TaskMasterQueue
            Message collapsedMessage = createMessage();
            collapsedMessage.setId(MessageID.MC_LOGSUMS_COLLAPSED);
            collapsedMessage.setValue("matrix", squeezed);
            mcLogger.info(getName() + ", Sending " + squeezed.getName() + " to " +  matrixWriterQueue);
            sendTo(matrixWriterQueue, collapsedMessage);
        }


    }

    /**
     * Collapse the logsums in the alpha zone matrix to beta zones. The
     * collapsed matrix will be send to the matrixWriterQueue.
     *
     * @param m
     *            Logsum matrix
     * @param queueName queue name
     * @return beta6000s - a matrix that has both beta zones and world zones.
     */
    public Matrix collapseMCLogsums(Matrix m, String queueName) {
        // Need to do a little work to get only the purpose/segment part out of
        // the name
        String newName = m.getName();
        newName = newName.replaceAll("ls", "ls_beta");

        Matrix betaMatrix = matrixCompression.getCompressedMatrix(m, "MEAN");
        int[] betaExternals = betaMatrix.getExternalNumbers();

        int[] newExternalNumbers = new int[betaExternals.length + wzEzUtil.getNumberOfWorldZones()];
        System.arraycopy(betaExternals, 1, newExternalNumbers, 1, betaExternals.length-1);
        int index = 0;
        for(int zone : wzEzUtil.getWorldZones()){
            newExternalNumbers[betaExternals.length + index] = zone;
            index++;
        }


        Matrix beta6000s = new Matrix(newName, "beta mclogsums", newExternalNumbers.length-1, newExternalNumbers.length-1);
        beta6000s.setExternalNumbers(newExternalNumbers);
        beta6000s.setName(newName);

        //first copy over the internal values
        int originZone;
        int destinationZone;
        for(int zRowIndex = 1; zRowIndex < betaExternals.length; zRowIndex++){
            for(int zColIndex = 1; zColIndex < betaExternals.length; zColIndex++){
                originZone = betaExternals[zRowIndex];
                destinationZone = betaExternals[zColIndex];
                beta6000s.setValueAt(originZone, destinationZone, betaMatrix.getValueAt(originZone, destinationZone));
            }
        }

        //next fill the worldzone rows and columns with -100
        for(int oZone : betaExternals){
            for(int dZone : betaExternals){
                if(wzEzUtil.isWorldZone(oZone) || wzEzUtil.isWorldZone(dZone)){
                    beta6000s.setValueAt(oZone, dZone, -100);
                }
            }
        }

        return beta6000s;
     }

    public static void main(String[] args) {
        MessageFactory mFactory = MessageFactory.getInstance();
        MCLogsumCalculatorTask mcCalc = new TLUMIPMCLogsumCalculatorTask();
        mcCalc.onStart();

        Message mcLogsumMessage = mFactory.createMessage();
        mcLogsumMessage.setId(MessageID.CREATE_MC_LOGSUMS);
        mcLogsumMessage.setValue("purpose", ActivityPurpose.WORK);
        mcLogsumMessage.setValue("segment", 0);
        mcLogsumMessage.setValue("id", 1);
        mcLogsumMessage.setValue("msgNum", 1);

        mcCalc.onMessage(mcLogsumMessage);
        System.out.println("Segment 1, Purpose work  sent to MCLogsum Calculator Task.");
    }
}
