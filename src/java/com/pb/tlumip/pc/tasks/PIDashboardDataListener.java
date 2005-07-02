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


import com.pb.tlumip.pc.beans.StatisticBean;
import com.pb.tlumip.pc.beans.DashboardDataBean;
import com.pb.tlumip.pc.*;
import com.pb.common.daf.Task;

import org.apache.log4j.Logger;
import java.util.logging.Level;

/**
 * Listener task for PI dashboard
 *
 * @author Christi Willison
 * @version Oct 29, 2003
 * Created by IntelliJ IDEA.
 */
public class PIDashboardDataListener extends Task {

    public Logger logger = Logger.getLogger("com.pb.common.pc");

        public static final String TASK_NAME = "PIDBListener";


        public PIDashboardDataListener() {
           // super( TASK_NAME );
        }


        public void doWork() {

            //PortManager pManager = PortManager.getInstance();

            //Listen on the STATUS topic
            //Port port = pManager.getPort(getName(), Topic.STATUS);

            logger.info(getName()+", waiting to receive message...");

            while (true) {
                //Message msg = port.receive();
                //handleMessage(msg);
            }
        }

        /** Put messages into the appropriate PI*DataCache using the messageID.
         */
//        //private void handleMessage(Message msg) {
//
//            String cacheName = null;
//            DashboardDataBean dbDataBean = null;
//
//            //try {
//                cacheName = msg.getStringValue( DashboardDataKey.MSG_ID);
//                dbDataBean = (DashboardDataBean) msg.getValue( DashboardDataKey.DASHBOARDDATA_BEAN);
//            }
//            catch (RuntimeException e) {
//                logger.log(Level.SEVERE, "", e);
//                return;
//            }
//
//            /*Add bean to the appropriate PI DataCache
//            *   PICommoditySurplusDataCache
//            *   PIMeritMeasureDataCache
//            *   PICommodityPriceDataCache
//            *   PIStepScaleDataCache
//            *   PIIBSEGoodsDataCache
//            *   PIIBSEServicesDataCache
//            *   PIIBSELaborDataCache
//            *   PIIBSEFloorspaceDataCache
//            */
//            if(cacheName.equals("PISurplus")){
//                PICommoditySurplusDataCache cache = PICommoditySurplusDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else if(cacheName.equals("PIMeritMeasure")){
//                PIMeritMeasureDataCache cache = PIMeritMeasureDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else if(cacheName.equals("PICommodityPrice")){
//                PICommodityPriceDataCache cache = PICommodityPriceDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else if(cacheName.equals("PIStepScale")){
//                PIStepScaleDataCache cache = PIStepScaleDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else if(cacheName.equals("PIIBSEGoods")){
//                PIIBSEGoodsDataCache cache = PIIBSEGoodsDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else if(cacheName.equals("PIIBSEServices")){
//                PIIBSEServicesDataCache cache = PIIBSEServicesDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else if(cacheName.equals("PIIBSELabor")){
//                PIIBSELaborDataCache cache = PIIBSELaborDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else if(cacheName.equals("PIIBSEFloorspace")){
//                PIIBSEFloorspaceDataCache cache = PIIBSEFloorspaceDataCache.getInstance();
//                cache.addValue(dbDataBean);
//            }else logger.info("message ID: "+ cacheName +" is invalid.  Message could not be cached");
//
//
//
//        }
//
}
