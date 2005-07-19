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
package com.pb.tlumip.ed;

import com.pb.tlumip.model.ModelComponent;
import com.pb.common.util.ResourceUtil;

import java.util.Vector;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * EDModel implements...
 * The model class contains and starts all the submodels.
 *
 * @author    Carl Batten
 * @version   1.0, 3/31/2000
 *
 */

public class EDModel extends ModelComponent {
    protected static Logger logger = Logger.getLogger(EDModel.class);
    private Vector submodels;
      private Hashtable errors;
      private boolean hasErrors;

      /**
       * This constructor takes a vector of SubModels as input, and arranges the
       * order they are run based on the SubModel order attribute.
       */
      public EDModel(Vector sms) {
        submodels = new Vector();
        SubModel tempSM;
        for(int i=0; i<sms.size() ; i++) {
          tempSM = (SubModel) sms.get(i);
          addSubModel(tempSM);
        }
      }

      /**
       * Adds a SubModel to the vector of SubModels in the order specified by
       * the SubModels order attribute.
       */
      public void addSubModel(SubModel sm) {
        int beginSize = submodels.size();
        boolean addedSubModel=false;
        for(int i=0; i<submodels.size(); i++) {
            if(sm.getOrder() < ((SubModel)submodels.get(i)).getOrder()) {
              submodels.add(i,sm);
              addedSubModel = true;
            }
        }
        if (!addedSubModel) {
          submodels.add(sm);
        }
      }




      /**
       *Retrieves data for each submodel, solves the submodel, and writes output
       * to the database.  Errors are placed in a Hashtable and may be looked at
       * when the model finishes running.
       */
      protected void start() {
        SubModel s;
        hasErrors = false;
        errors = new Hashtable();
        int max = submodels.size();
        for(int i=0 ; i<max; i++) {
          try {
            s =(SubModel)submodels.get(i);
            if(logger.isDebugEnabled()) logger.debug("Model: Reading in data for " + s.getType() + " submodel: " + s.getName());
            s.getData();
            if(logger.isDebugEnabled()) logger.debug("Model: Solving " + s.getType() + " submodel: " + s.getName());
            s.solve();
            if(logger.isDebugEnabled()) logger.debug("Model: Setting data for " + s.getType() + " submodel: " + s.getName());
            s.setData();
          } catch(Exception e) {
            hasErrors = true;
            logger.error(e.toString());
            SubModel errorSubModel = (SubModel) submodels.get(i);
            if(errorSubModel.getName() ==null){
              errors.put(String.valueOf(errorSubModel.getOrder()), e);
            }else{
              errors.put(errorSubModel.getName(), e);
            }
          }
        }
      }

      /**
       * This method returns a hashtable of errors for the last running of this
       * model.
       */
      protected Hashtable getErrors() {
        return errors;
      }

      /**
       * Returns true if there was an error in the last run of the model.
       */
      protected boolean hasErrors() {
        return hasErrors;
      }

      /**
       * This method returns the number of submodels in this model.
       */
      protected int numberSubModels() {
        return submodels.size();
      }


        public void startModel(int timeInterval){
            try {
                int currentYear = (Integer.parseInt(ResourceUtil.getProperty(appRb,"baseYear"))
                        + timeInterval);
                String dataLocation = ResourceUtil.getProperty(appRb,"dataLocation");
                String xmlLocation = ResourceUtil.getProperty(appRb,"home")+
                        ResourceUtil.getProperty(appRb,"xmlLocation");

                EDControl e = new EDControl(currentYear,dataLocation,xmlLocation);
                e.startModel();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        };
}
