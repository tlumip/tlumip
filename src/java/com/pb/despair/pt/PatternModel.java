package com.pb.despair.pt;

import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;

import java.util.logging.Logger;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Iterator;

/** 
 * PatternModel builds one nested logit model 
 * to be reused (rather than creating new objects)
 * 
 * @author Steve Hansen
 * @version 1.0 09/02/2003
 *
 */

public class PatternModel{

    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.PatternModel");
    final boolean debug=false;
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
                           boolean weekday){
          
          //set hh pattern model attributes
          personAttributes.setAttributes(thisHousehold,thisPerson);
                                   
          //compute utility for alternatives
          if(!writtenOutTheUtilitiesAlready && debug){
                  logger.info("Here are the utilities for the patterns passed into the 'choosePattern' method:");
          }
          for(int p=0;p<patterns.patterns.length;p++){
               Pattern thisPattern = patterns.patterns[p];//(Pattern)patternIterator.next();

               thisPattern.setAvailability(true);

               thisPattern.calcUtility(params,personAttributes);
              if(!writtenOutTheUtilitiesAlready && debug){
                  logger.info("\tPattern " + p + ": " + thisPattern.getUtility());

              }
          }


          //choose an alternative
          patternChoiceModel.computeAvailabilities();
          if(weekday)
              thisPerson.weekdayPatternLogsum = patternChoiceModel.getUtility();
          else
              thisPerson.weekendPatternLogsum = patternChoiceModel.getUtility();

         if(!writtenOutTheUtilitiesAlready && debug){
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
              logger.severe("Error in choosing pattern alternative");
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
         logger.fine("test build model complete");
     }

}
