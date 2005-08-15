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
package com.pb.tlumip.pt;

import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;

import org.apache.log4j.Logger;
import java.util.ResourceBundle;
import java.util.List;

/** 
 * PatternModel builds one nested logit model 
 * to be reused (rather than creating new objects)
 * 
 * @author Steve Hansen
 * @version 1.0 09/02/2003
 *
 */

public class PatternModel{

    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.PatternModel");
    final long debugID=1;
    LogitModel patternChoiceModel;
    PTTimer timer = new PTTimer();
    Pattern homePattern = new Pattern("h");
    PatternChoiceParameters params;
    PersonPatternChoiceAttributes personAttributes = new PersonPatternChoiceAttributes();
    boolean writtenOutTheUtilitiesAlready = false;

     public PatternModel(){
     }
     
     public void buildModel(Patterns patterns, PatternChoiceParameters params){
         patternChoiceModel = new LogitModel("patternChoiceModel", patterns.patterns.length);//.size());
         this.params = params;
         for(int p=0;p<patterns.patterns.length;p++){
             patternChoiceModel.addAlternative(patterns.patterns[p]);//thisPattern);
         }
     }

     public Pattern choosePattern(PTHousehold thisHousehold,
                           PTPerson thisPerson,
                           Patterns patterns,
                           boolean weekday,
                           boolean debug){
          
          //set hh pattern model attributes
          personAttributes.setAttributes(thisHousehold,thisPerson);
          if(debug) personAttributes.print();
          
          //compute utility for alternatives
          for(int p=0;p<patterns.patterns.length;p++){
               Pattern thisPattern = patterns.patterns[p];//(Pattern)patternIterator.next();

               thisPattern.setAvailability(true);

               thisPattern.calcUtility(params,personAttributes,debug);
              
          }


          //choose an alternative
          patternChoiceModel.computeAvailabilities();
          if(weekday)
              thisPerson.weekdayPatternLogsum = patternChoiceModel.getUtility();
          else
              thisPerson.weekendPatternLogsum = patternChoiceModel.getUtility();

         if(!writtenOutTheUtilitiesAlready && logger.isDebugEnabled()) {
             logger.info("Here are the utilities for the alternatives that were passed to the 'buildModel' method");
             List alternatives = patternChoiceModel.getAlternatives();
             for(int i=0;i<alternatives.size();i++){
                logger.info("\tAlternative " + i + ": " + ((Pattern) alternatives.get(i)).getUtility());    
             }
             writtenOutTheUtilitiesAlready = true;
         }
          patternChoiceModel.calculateProbabilities();

          
         //set up the chosenAlternative to be an at-home pattern; this will be replaced when
         // the monte carlo selection of alternatives occurs below
         Pattern chosenAlternative = homePattern;
          
         try{
              chosenAlternative = (Pattern) patternChoiceModel.chooseElementalAlternative();
         }catch(Exception e){
              logger.fatal("Error in choosing pattern alternative");
             //TODO - log this to the node exception log file
              System.exit(1);
         }

         return chosenAlternative;          
          
     }     
     public static void main(String[] args){
         ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         Patterns patterns = new Patterns();
         patterns.readData(rb,"weekdayPatterns.file");
         PatternChoiceParameters wkdayParams = new PatternChoiceParameters();
         wkdayParams.readData(rb,"weekdayParameters.file");
         PatternModel pm = new PatternModel();
         pm.buildModel(patterns,wkdayParams);
         logger.info("test build model complete");
     }

}
