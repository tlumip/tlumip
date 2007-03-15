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
package com.pb.tlumip.ts;

import com.pb.common.rpc.DafNode;
import com.pb.common.util.ResourceUtil;
import com.pb.models.reference.ModelComponent;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.util.ResourceBundle;


/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Mar 15, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TSModelComponent extends ModelComponent {

    Logger logger = Logger.getLogger(TSModelComponent.class);
    private String configFileName;

    TS ts;

    /**
     * If running in DAF mode, the configFileName will be something other than "null".
     * If the configFileName is null, then TS will run monolithically.
     * @param appRb
     * @param globalRb
     * @param configFileName
     */
    public TSModelComponent(ResourceBundle appRb, ResourceBundle globalRb, String configFileName){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.
        this.configFileName = configFileName;
    }
    public void startModel(int baseYear, int timeInterval){
        logger.info("Config file name: " + configFileName);
        if ( configFileName != null ) {
            try {
                DafNode.getInstance().initClient(configFileName);
            }
            catch (MalformedURLException e) {
                logger.error( "MalformedURLException caught initializing a DafNode.", e);
            }
            catch (Exception e) {
                logger.error( "Exception caught initializing a DafNode.", e);
            }

        }
        ts = new TS(appRb, globalRb);

        assignAndSkimHwyAndTransit("peak");
        assignAndSkimHwyAndTransit("offpeak");


    }

    private void assignAndSkimHwyAndTransit(String period){
        NetworkHandlerIF nh = NetworkHandler.getInstance(configFileName);

        if ( nh.getStatus() ) {
            logger.info ( nh.getClass().getCanonicalName() + " instance created, and handler is active." );
        }

        try {
            nh.setRpcConfigFileName( configFileName );
            if ( ts.setupNetwork( nh, ResourceUtil.changeResourceBundleIntoHashMap(appRb), ResourceUtil.changeResourceBundleIntoHashMap(globalRb), period ) < 0 )
                throw new Exception();
            logger.info ("created " + period + " Highway NetworkHandler object: " + nh.getNodeCount() + " highway nodes, " + nh.getLinkCount() + " highway links." );
        }
        catch (Exception e) {
            logger.error ( "Exception caught setting up network in " + nh.getClass().getCanonicalName(), e );
            System.exit(-1);
        }


        ts.runHighwayAssignment( nh );
        ts.writeHighwaySkimMatrices ( nh, 'a' );
        ts.assignAndSkimTransit ( nh,  appRb, globalRb );
    }

    
}
