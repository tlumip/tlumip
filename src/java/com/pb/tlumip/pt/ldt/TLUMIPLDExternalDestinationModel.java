/*
 * Copyright  2006 PB Consult Inc.
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
package com.pb.tlumip.pt.ldt;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ModelException;
import com.pb.models.pt.ldt.LDExternalDestinationModel;
import com.pb.models.pt.ldt.LDSkimsInMemory;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.LDTourModeType;
import com.pb.models.pt.ldt.ParameterReader;
import static com.pb.tlumip.pt.ldt.LDExternalDestinationChoiceParameters.SIZECOUNT;
import static com.pb.tlumip.pt.ldt.LDExternalDestinationChoiceParameters.TIME;

import java.util.ResourceBundle;

/**
 * 
 * @author Erhardt
 * @version 1.0 Mar 8, 2007
 *
 */
public class TLUMIPLDExternalDestinationModel  extends LDExternalDestinationModel {
 
    private Matrix time;
    private float[][] parameters;

    private ConcreteAlternative[] alts;
    private float volume[]; 
    private LogitModel model;
    
    /**
     * Default Constructor    
     */
    public TLUMIPLDExternalDestinationModel() {
        super();
    }
    
    /**
     * Initializes the class.  
     * 
     * @param globalRb
     * @param ptRb
     */
    public void initialize(ResourceBundle globalRb, ResourceBundle ptRb) {

        super.initialize(globalRb, ptRb);        
        
        // get the time matrix
        time = LDSkimsInMemory.getOffPeakMatrix(LDTourModeType.AUTO, "Time");
        
        readParameters(); 
        buildModel(); 
    }
    
    
    /**
     * Read parameters from file specified in properties.
     * 
     */
    private void readParameters() {
        
        logger.info("Reading LDInternalDestinationChoiceParameters");
        parameters = ParameterReader.readParameters(ptRb,
                "LDExternalDestinationChoiceParameters.file");
        
    }
    
    /**
     * Computes size terms in each external station. 
     * 
     */
    private void buildModel() {

        logger.info("Building External Destination Choice Model...");

        time = LDSkimsInMemory.getOffPeakMatrix(LDTourModeType.AUTO, "Time");

        TableDataSet externalStationData = ParameterReader.readParametersAsTable(ptRb,
                "ExternalStationVolumes.file");
        alts = new ConcreteAlternative[externalStationData.getRowCount()];
        volume = new float[externalStationData.getRowCount()]; 
        model = new LogitModel("LD External Destination Choice");

        for (int i = 0; i < externalStationData.getRowCount(); i++) {
            Integer taz = new Integer((int) externalStationData.getValueAt(i + 1, "ExSta"));
            volume[i] = externalStationData.getValueAt(i + 1, "Vehicles");
            alts[i] = new ConcreteAlternative(taz.toString(), taz);
            model.addAlternative(alts[i]);
        }
    }
    
    
    
    /**
     * Calculate utilites for all possible destinations
     * and return logsum for origin TAZ.
     * 
     * @param tour Decision-makers long-distance tour.
     * 
     * @return The destination choice logsum from tour origin TAZ to all TAZs. 
     */
    private double calculateUtility(LDTour tour) {
                
        // for each alternative
        for (int i=0; i<alts.length; i++) {
            
            Integer taz = (Integer) alts[i].getAlternative(); 
            float travelTime = time.getValueAt(tour.homeTAZ, taz);
          
            double size = parameters[tour.purpose.ordinal()][SIZECOUNT] * volume[i];
            
            if (size > 0) {
                double utility = parameters[tour.purpose.ordinal()][TIME] * travelTime
                               + Math.log(size); 
                alts[i].setUtility(utility);  
                alts[i].setAvailability(true); 
            }
            else {
                alts[i].setAvailability(false);
            }
        }
        
        return model.getUtility();
    }
    
    
    
    /**
     * 
     * @return  The ID of the chosen TAZ.  
     */
    public int chooseTaz(LDTour tour) {
                                
        // choose the taz      
        Integer chosenTaz; 
        try {
            calculateUtility(tour);
            model.calculateProbabilities();
            ConcreteAlternative chosen = (ConcreteAlternative) model.chooseElementalAlternative();
            chosenTaz = (Integer) chosen.getAlternative(); 
        } catch (Exception e) {
            String msg = "Error in mode choice: no modes available ";
            logger.fatal(msg);
            throw new ModelException(msg);
        }
        
        if (trace) {
            logger.info("    The External Destination Choice for HH + " + tour.hh.ID
                    + " person " + tour.person.memberID + " tour " + tour.ID
                    + " is : " + chosenTaz);
        }
        
        return chosenTaz;
    }
}
