/*
 * Copyright  2005 PB Consult Inc.
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
package com.pb.tlumip.pc.tasks;

import com.pb.common.daf.*;
import com.pb.tlumip.pc.StatCache;
import com.pb.tlumip.pc.StatKey;
import com.pb.tlumip.pc.beans.StatisticBean;

import org.apache.log4j.Logger;

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
            logger.fatal( "", e);
            return;
        }

        //Add statistic bean to StatCache
        StatCache cache = StatCache.getInstance();
        cache.addValue(applicationName, type, statBean);
    }

}
