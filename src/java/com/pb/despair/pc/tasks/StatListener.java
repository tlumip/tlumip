package com.pb.despair.pc.tasks;

import com.pb.common.daf.*;
import com.pb.despair.pc.StatCache;
import com.pb.despair.pc.StatKey;
import com.pb.despair.pc.beans.StatisticBean;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Processes application status messages.
 *
 * @author    Tim Heier
 * @version   1.0, 12/27/2002
 */
public class StatListener extends Task {

    public Logger logger = Logger.getLogger("com.pb.common.pc");

    public static final String TASK_NAME = "StatListener";


    public StatListener() {
        //super( TASK_NAME );
    }


    public void doWork() {

        PortManager pManager = PortManager.getInstance();

        //Listen on the STATUS topic
        //Port port = pManager.getPort(getName(), Topic.STATUS);

        logger.info(getName()+", waiting to receive message...");

        while (true) {
            //Message msg = port.receive();
            //handleMessage(msg);
        }
    }

    /** Retrieve values from message and add statistic bean to StatCache
     */
    private void handleMessage(Message msg) {

        String applicationName = null;
        String type = null;
        StatisticBean statBean = null;

        try {
            applicationName = msg.getStringValue( StatKey.APPLICATION_NAME );
            type = msg.getStringValue( StatKey.STATISTIC_TYPE );
            statBean = (StatisticBean) msg.getValue( StatKey.STATISTIC_BEAN );
        }
        catch (RuntimeException e) {
            logger.log(Level.SEVERE, "", e);
            return;
        }

        //Add statistic bean to StatCache
        StatCache cache = StatCache.getInstance();
        cache.addValue(applicationName, type, statBean);
    }

}
