/*
 * Created on May 20, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.pt.daf;

import java.util.Date;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.SkimsInMemory;

/**
 * @author hansens
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PTDafReader extends MessageProcessingTask{
    SkimsInMemory skims = new SkimsInMemory();
    public void onStart() {
        logger.info( "***" + getName() + " started");

        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        int timeInterval = -1;
        String pathToRb = null;
        try {
            logger.info("Reading RunParams.txt file");
            reader = new BufferedReader(new FileReader(new File( Scenario.runParamsFileName )));
            timeInterval = Integer.parseInt(reader.readLine());
            logger.info("\tTime Interval: " + timeInterval);
            pathToRb = reader.readLine();
            logger.info("\tResourceBundle Path: " + pathToRb);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ResourceBundle PTrb = ResourceUtil.getPropertyBundle(new File(pathToRb));

        ResourceBundle rb = ResourceUtil.getResourceBundle("ptdaf");
        final int NUMBER_OF_WORK_QUEUES = Integer.parseInt(ResourceUtil.getProperty(rb,"workQueues"));
        skims.readSkims(PTrb);
        for(int q=1;q<=NUMBER_OF_WORK_QUEUES;q++){
            Message skimsMessage = createMessage();
            skimsMessage.setId(MessageID.SKIMS);
            skimsMessage.setValue("skims",skims);
            String queueName = new String("WorkQueue"+q);
            logger.fine("Skims sent to "+queueName+" at "+ new Date());
            sendTo(queueName,skimsMessage);
            logger.fine("Free memory after creating MC logsum: "+Runtime.getRuntime().freeMemory());   
        }
        skims=null;
    }

    public void onMessage(Message msg) {
        logger.info( getName() + " received messageId=" + msg.getId() + " message from=" + msg.getSender() );
        sendTo(String.valueOf(msg.getValue("queue")),msg);        
    }
}
