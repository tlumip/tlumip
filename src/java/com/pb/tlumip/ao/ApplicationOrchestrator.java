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
package com.pb.tlumip.ao;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pecas.PIModel;
import com.pb.models.reference.ModelComponent;
import com.pb.models.utils.StatusLogger;
import com.pb.tlumip.aa.AAModel;
import com.pb.tlumip.ald.ALDModel;
import com.pb.tlumip.ct.CTModel;
import com.pb.tlumip.ed.EDControl;
import com.pb.tlumip.ed.NEDModel;
import com.pb.tlumip.et.ETModel;
import com.pb.tlumip.et.ETPythonModel;
import com.pb.tlumip.spg.SPGnew;
import com.pb.tlumip.ts.TSModelComponent;
import com.pb.tlumip.epf.EdPiFeedback;
import com.pb.tlumip.sl.SelectLink;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;


/**
 *
 *
 * @author  Christi Willison, Kimberly Grommes
 * @version May 27, 2008
 * Created by IntelliJ IDEA.
 */
public class ApplicationOrchestrator {

    private static Logger logger = Logger.getLogger(ApplicationOrchestrator.class);
    private String rootDir;
    private String baseScenarioName;
    private String scenarioName;
    private int t;
    private int baseYear;
    ResourceBundle rb;
    BufferedWriter runLogWriter;
    File runLogPropFile;
    HashMap<String,String> runLogHashmap;
    String scenarioOutputs;
    String scenarioInputs;

    public ApplicationOrchestrator(ResourceBundle rb){
        this.rb = rb;
    }

    public ApplicationOrchestrator(String rootDir, String baseScenarioName, String scenarioName, int timeInterval, int baseYear){
        this.rootDir = rootDir;
        this.baseScenarioName = baseScenarioName;
        this.scenarioName = scenarioName;
        this.t = timeInterval;
        this.baseYear = baseYear;
    }

    public void setRb(ResourceBundle rb) {
        this.rb = rb;
    }

    public ResourceBundle getRb() {
        return rb;
    }

    private void createBaseYearPropFile(){
        runLogPropFile = new File(rootDir + "/" + baseScenarioName + "/t0/zzInitialRunLog.properties");

        if(!runLogPropFile.exists()){
            logger.info("Creating the base year run log in " + rootDir + "/" + baseScenarioName + "/t0/");
            
            try {
                runLogWriter = new BufferedWriter(new FileWriter(runLogPropFile, false));
                runLogWriter.write("# Filter values for properties files");
                runLogWriter.newLine();
                runLogWriter.write("ROOT.DIR=" + rootDir);
                runLogWriter.newLine();
                runLogWriter.write("BASE.SCENARIO.NAME=" + baseScenarioName);
                runLogWriter.newLine();
                runLogWriter.write("BASE.YEAR=" + baseYear);
                runLogWriter.newLine();
                runLogWriter.write("GLOBAL.TEMPLATE.INTERVAL=0");
	            runLogWriter.newLine();
                runLogWriter.write("ED.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("EPF.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("ALD.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("SPG1.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("PI.PRIOR.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("PI.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("SPG2.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("CT.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("ET.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("PT.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("TS.LAST.RUN=0");
                runLogWriter.newLine();
                runLogWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }// else return - base year prop file has already been created.
    }

    private String getUserInputsDirectoryPart() {
        String inputsDir = scenarioInputs + "/user_inputs";
        if (!new File(rootDir + "/" + inputsDir).exists())
            inputsDir = scenarioInputs + "/user_inputs";
        return inputsDir;
    }

    private void createRunLogPropFile(){

        if(baseScenarioName.equals(scenarioName)){
            scenarioInputs = baseScenarioName;
            scenarioOutputs = baseScenarioName;
            //}else if (t==1){ //It was decided to initially make a complete copy of the parent scenario for the child scenario.
            //    scenarioInputs = baseScenarioName;
            //    scenarioOutputs = baseScenarioName + "/" + scenarioName;
        }else{
            scenarioInputs = baseScenarioName + "/" + scenarioName;
            scenarioOutputs = baseScenarioName + "/" + scenarioName;
        }
        runLogPropFile = new File(rootDir + "/" + scenarioOutputs + "/t" + t + "/zzRunLog.properties");

        logger.info("Looking for the run log in " + rootDir + "/" + scenarioOutputs + "/t" + t + "/");

        if(!runLogPropFile.exists()){
            try { //create the file and write the current year into it.
                logger.info("Writing the current scenario information into the run log");
                runLogWriter = new BufferedWriter(new FileWriter(runLogPropFile, false));
                runLogWriter.write("# Filter values for properties files");
                runLogWriter.newLine();
                runLogWriter.write("SCENARIO.NAME=" + scenarioName);
                runLogWriter.newLine();
                runLogWriter.write("CURRENT.INTERVAL=" + t);
                runLogWriter.newLine();
                runLogWriter.write("SCENARIO.INPUTS=" + scenarioInputs);
                runLogWriter.newLine();
                runLogWriter.write("SCENARIO.OUTPUTS=" + scenarioOutputs);
                runLogWriter.newLine();
                runLogWriter.write("USER.INPUTS=" + getUserInputsDirectoryPart());
                runLogWriter.newLine();
                runLogWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else { // the file may have already been created by a call from an earlier app.
            // so just attach a writer to it that can append the appLog info.
            try {
                runLogWriter = new BufferedWriter(new FileWriter(runLogPropFile, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void updateRunLogPropertiesHashmap (int t){
        //Get the base year runLog and read in all of the properties into a hashmap.
        String initialRunLogPath = rootDir + "/" + baseScenarioName + "/t0/zzInitialRunLog.properties";
        logger.info("Reading initial Run log file: " + initialRunLogPath);

        ResourceBundle runLogRb = ResourceUtil.getPropertyBundle(new File(initialRunLogPath));
        runLogHashmap = ResourceUtil.changeResourceBundleIntoHashMap(runLogRb);

        //Now update the hashmap with the most current values by looking for runLog.properties
        //files in the t1, t2, up to tn directories and replacing any of the runLog properties
        //with the current values.
        int i = 0;
        File propFile;
        String keyName;
        String value;

        while (i<=t){
            String propPath = rootDir + "/" + scenarioOutputs + "/t" + i + "/zzRunLog.properties";

            propFile = new File(propPath);
            if(propFile.exists()){
                logger.info("Reading in Run Log from year " + i + " and updating the RunLog HashMap: ");
                runLogRb = ResourceUtil.getPropertyBundle(new File(propPath));
                Enumeration rbEnum = runLogRb.getKeys();
                while (rbEnum.hasMoreElements()) {
                    //get the name and value pair from the current run log and
                    keyName = (String) rbEnum.nextElement();
                    value = runLogRb.getString(keyName);

                    //if the key already exists in the hashmap (which it should) replace
                    //the matching value with the runLog value.
                    if(runLogHashmap.containsKey(keyName)){
                        runLogHashmap.put(keyName,value);
                    }else {
                        logger.info("The RunLog HashMap doesn't contain the key " + keyName);
                        logger.info("Adding key to HashMap");
                        runLogHashmap.put(keyName,value);
                    }
                }
                i++;
            }else {
                if(t==1){
                    logger.info("No applications have been run yet so no run log exists");
                }else logger.debug("NO RUN LOG EXISTS IN t" + i + " - PROPERTY FILES COULD BE AFFECTED" );
                i++;
            }
        }

    }

    private String createAppRb(String appName){
        //First read in the template properties file.  This will have default values and
        //tokens (surrounded by @ symbols).  The tokens will be replaced with the values
        //in the runLogHashMap

        //Get Template File
        File appPropertyTemplate = findTemplateFileRecursively(appName);

        //A series of update files may be used, the more recent ones are loaded second, so their values supercede older ones
        List<File> appPropertyUpdates = findTemplatUpdateFiles(appName);

        String appPropertyTemplateName = appPropertyTemplate.getName();
        //The property file will have the same name as the template file minus the 'Template' part.

        String appPropFileName = appPropertyTemplateName.substring(0,appPropertyTemplateName.indexOf("Temp"));  //subtract off the 'Template.properties'
        String outputPath = rootDir + "/" + scenarioOutputs + "/t" + t + "/";
        String appPropertyFilePath = outputPath + appPropFileName + ".properties";
        File appPropertyFile;

        appPropertyFile = new File(appPropertyFilePath);

        logger.info("Creating " + appPropertyFile.getAbsolutePath());

        Properties appDefaultProps = new Properties();
        try {
            appDefaultProps.load(new FileInputStream(appPropertyTemplate));
            //load updates second, to override current values
            for (File f : appPropertyUpdates)
                appDefaultProps.load(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String keyName : runLogHashmap.keySet()) {

            String hashmapValue = runLogHashmap.get(keyName);
            hashmapValue = hashmapValue.replace(":", ":/"); //the separator gets lost when read in from prop file

            String patternStr = "@" + keyName + "@";

            // Replace all occurrences of pattern in input string
            Enumeration propNames = appDefaultProps.propertyNames();
            while (propNames.hasMoreElements()) {
                String propName = (String) propNames.nextElement();
                String tempStr = appDefaultProps.getProperty(propName);
                tempStr = tempStr.replaceAll(patternStr, hashmapValue);
                appDefaultProps.setProperty(propName, tempStr);
            }
        }
        //write the properties file to the output stream in a format suitable for loading into a
        //properties table using the "load" method.
        try {
            appDefaultProps.store(new FileOutputStream(appPropertyFile), appPropFileName.toUpperCase() + " Properties File for Interval " + t);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return (appPropertyFilePath);
    }

    private List<File> findTemplatUpdateFiles(String appName) {
        List<File> templateUpdateFiles = new LinkedList<File>();
        String templateUpdatePathPrefix = rootDir + "/" + getUserInputsDirectoryPart() + "/t";
        String templateUpdatePathSuffix = "/" + appName + "TemplateUpdate.properties";
        for (int i = 0; i <= t; i++) {
            File templateUpdateFile = new File(templateUpdatePathPrefix + i + templateUpdatePathSuffix);
            if (templateUpdateFile.exists())
                templateUpdateFiles.add(templateUpdateFile); 
        }
        return templateUpdateFiles;
    }

    public File findTemplateFileRecursively(String appName) {
        String templateSuffix = "Template.properties";
        String editedName = appName.toUpperCase();
        File templateFile = null;
        String templateEntry = editedName + ".TEMPLATE.INTERVAL";

        int i = t;  // look thru all of the tn directories for a property template file
        // starting with the most recent t.
        while(i >= 0){
            String templatePath = rootDir + "/" + getUserInputsDirectoryPart() + "/t" + i + "/";
            templateFile = new File(templatePath + appName + templateSuffix);
            if(templateFile.exists()){
                logger.info("Full Path to Template File: " + templateFile.getAbsolutePath());
                String currentTemplate = runLogHashmap.get(templateEntry);
                if(currentTemplate == null){ // no previous template was found.
                    runLogHashmap.put(templateEntry, Integer.toString(i));
                    try {
                        logger.info("Writing the initial template location for this app into the run log");
                        runLogWriter.write(templateEntry + "=" + Integer.toString(i));
                        runLogWriter.newLine();
                        runLogWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if(!currentTemplate.equals(Integer.toString(i))){  //a more recent template file was found
                    try {
                        logger.info("Writing the new template location for this app into the run log");
                        runLogWriter.write(templateEntry + "=" + Integer.toString(i));
                        runLogWriter.newLine();
                        runLogWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } //if template file is the same as the last one found don't write it into the log.
                return templateFile;
            }else {
                i--;
            }
        }
        // if you get to here it means a template file couldn't be found in any of the tn directories
        // so templateFile = null
        logger.fatal("Couldn't find Template File for " + appName + ".  Returning a file that does not exist");
        return templateFile;
    }

    public ResourceBundle findResourceBundle(String pathToRb) {
        File propFile = new File(pathToRb);
        ResourceBundle rb = ResourceUtil.getPropertyBundle(propFile);
        if(rb == null ) logger.fatal("Problem loading resource bundle: " + pathToRb);
        return rb;
    }

    public void writeRunParamsToPropertiesFile(String pathToAppRb, String pathToGlobalRb, String moduleName){
        File runParams = new File(rootDir + "/" + scenarioInputs + "/ops/RunParams.properties");
        logger.info("Writing 'scenarioName', 'baseYear, 'timeInterval', 'pathToRb' and 'pathToGlobalRb' into " + runParams.getAbsolutePath());
        PrintWriter writer;
        try {
            writer = new PrintWriter(new FileWriter(runParams));
            writer.println("scenarioName=" + scenarioName);
            writer.println("baseYear=" + baseYear);
            writer.println("timeInterval=" + t);
            writer.println("pathToAppRb=" + pathToAppRb);
            writer.println("pathToGlobalRb=" + pathToGlobalRb);
            writer.println("pecasName=" + moduleName);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open the RunParams.txt file", e);
        }
        writer.close();
    }

    public void writeRunParamsToPropertiesFile(String pathToAppRb, String pathToGlobalRb){
        File runParams = new File(rootDir + "/" + scenarioInputs + "/ops/RunParams.properties");
        logger.info("Writing 'scenarioName', 'baseYear, 'timeInterval', 'pathToRb' and 'pathToGlobalRb' into " + runParams.getAbsolutePath());
        PrintWriter writer;
        try {
            writer = new PrintWriter(new FileWriter(runParams));
            writer.println("scenarioName=" + scenarioName);
            writer.println("baseYear=" + baseYear);
            writer.println("timeInterval=" + t);
            writer.println("pathToAppRb=" + pathToAppRb);
            writer.println("pathToGlobalRb=" + pathToGlobalRb);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open the RunParams.txt file", e);
        }
        writer.close();
    }


    public void runEDModel(int baseYear, int timeInterval, ResourceBundle appRb){

        ModelComponent comp = new EDControl(baseYear,timeInterval,appRb);
        comp.startModel(baseYear, timeInterval);

    }


    public void runNEDModel(int baseYear, int timeInterval, ResourceBundle appRb){
        ModelComponent comp = new NEDModel(appRb,appRb);
        comp.startModel(baseYear, timeInterval);
    }

    public void runEPFModel(int baseYear, int timeInterval, ResourceBundle appRb){

        ModelComponent comp = new EdPiFeedback(appRb);
        comp.startModel(baseYear, timeInterval);

    }

    public void runALDModel(int baseYear, int timeInterval, ResourceBundle appRb){
        ModelComponent comp = new ALDModel();
        comp.setApplicationResourceBundle(appRb);
        comp.startModel(baseYear, timeInterval);
    }

    public void runSPG1Model(int baseYear, int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){

        String baseYearS = Integer.toString(baseYear);
        String currentYear = Integer.toString(baseYear + timeInterval);

        SPGnew spg = new SPGnew( appRb, globalRb, baseYearS, currentYear );


        if (spg.isPersonAgeConstraintEnabled()) {
            spg.getHHAttributeData(baseYearS);
            //run once with age constraint off to get population value
            spg.disablePersonAgeConstraint();
            spg.spg1(currentYear);
            spg.setPopulationTotal(spg.getPopulationTotal());
            //enable age constraint, for final run
            spg.enablePersonAgeConstraint();
            spg.resetSPG1BalancingCount();
        }
        spg.getHHAttributeData(baseYearS); //reset for second spg run
        spg.spg1(currentYear);
        TableDataSet table = spg.sumHouseholdsByIncomeSize();
        spg.writePiInputFile(table);

    }

    public void runPIModel(int baseYear, int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){
        PIModel pi = new PIModel();
        pi.setResourceBundles(appRb, globalRb);
        pi.startModel(baseYear, timeInterval);
    }

    public void runPIDAFModel(int timeInterval, String pathToAppRb, String pathToGlobalRb, String nodeName){
        //Since AO doesn't communicate directly with PI we need to write the absolute
        //path to the resource bundle and the time interval into a file, "RunParams.txt"
        //that will be read by the PIServer Task when the PIDAF application is launched.
        writeRunParamsToPropertiesFile(pathToAppRb, pathToGlobalRb,"pi");
        StartDafApplication appRunner = new StartDafApplication("pidaf", rootDir, scenarioOutputs, timeInterval);

        appRunner.run();
    }

    public void runAAModel(int baseYear, int timeInterval, ResourceBundle appRb, ResourceBundle globalRb) throws IOException {
        AAModel aa = new AAModel(appRb,globalRb);
        //need to make a copy of global.properties to aa.properties
        String outputPath = rootDir + "/" + scenarioOutputs + "/t" + t + "/";
        copyFile(new File(outputPath + "global.properties"),new File(outputPath + "aa.properties"));
        aa.startModel(baseYear, timeInterval);
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists())
            destFile.createNewFile();
        FileInputStream fIn = null;
        FileOutputStream fOut = null;
        FileChannel source = null;
        FileChannel destination = null;
        try {
            fIn = new FileInputStream(sourceFile);
            source = fIn.getChannel();
            fOut = new FileOutputStream(destFile);
            destination = fOut.getChannel();
            long transfered = 0;
            long bytes = source.size();
            while (transfered < bytes)
                destination.position(transfered += destination.transferFrom(source,0,source.size()));
        } finally {
            simpleClose(source);
            simpleClose(fIn);
            simpleClose(destination);
            simpleClose(fOut);
        }
    }

    private void simpleClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }

    public void runSPG2Model(int baseYr, int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){

        String baseYear = Integer.toString(baseYr);
        String currentYear = Integer.toString(baseYr + timeInterval);

        SPGnew spg = new SPGnew( appRb, globalRb, baseYear, currentYear );

        spg.spg2();
        spg.writeZonalSummaryToCsvFile();
        spg.writeHHOutputAttributes(baseYear);

    }

    public void runPTDAFModel(int timeInterval, String pathToAppRb, String pathToGlobalRb,String nodeName){
        writeRunParamsToPropertiesFile(pathToAppRb, pathToGlobalRb);
        StartDafApplication appRunner = new StartDafApplication("ptdaf", rootDir, scenarioOutputs, timeInterval);

        appRunner.run();
    }

    public void runCTModel(int baseYear, int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){
        ModelComponent comp = new CTModel(appRb, globalRb);
        comp.startModel(baseYear, timeInterval);

    }

    public void runETModel(int baseYear, int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){
//        ETModel et = new ETModel(appRb, globalRb);
        ETPythonModel et = new ETPythonModel(appRb,globalRb);

        et.startModel(baseYear, timeInterval);
    }

    public void runTSModel(int baseYear, int timeInterval, ResourceBundle appRb, ResourceBundle globalRb, Boolean daily){
        ModelComponent tsModel = new TSModelComponent(appRb, globalRb, null, daily);
        tsModel.startModel(baseYear, timeInterval);
    }

    public void runTSDAFModel(int baseYear, int timeInterval, String pathToAppRb, String pathToGlobalRb, String configFileName, Boolean daily){
        ResourceBundle appRb = findResourceBundle(pathToAppRb);
        ResourceBundle globalRb = findResourceBundle(pathToGlobalRb);
        ModelComponent tsModel = new TSModelComponent(appRb, globalRb, configFileName, daily);
        tsModel.startModel(baseYear, timeInterval);
    }

    public void runSLModel(int timeInterval, ResourceBundle rb, String slMode) {
        SelectLink sl = new SelectLink(rb,timeInterval);
        sl.runStages(slMode);
    }

    private void logAppRun(String appName){
        if (appName.endsWith("daf")) {
            appName = appName.substring(0,(appName.length()-3));
        }
        try {
            logger.info("Writing the application name and the timeInterval for run into the run log");
            String upperAppName = appName.toUpperCase();
            if (upperAppName.equals("PI") || upperAppName.equals("AA")) {
                int piPriorRun = Integer.parseInt(runLogHashmap.get(upperAppName + ".LAST.RUN"));
                runLogWriter.write(upperAppName + ".PRIOR.RUN=" + piPriorRun);
                runLogWriter.newLine();
            }
            runLogWriter.write(upperAppName + ".LAST.RUN=" + t);
            runLogWriter.newLine();
            runLogWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {
            //Get command line arguments: Root Directory, Scenario Name, Application Name,
            //                            Time Interval and Start Node (for daf applications)
            String rootDir = args[0];
            String baseScenarioName = args[1];
            String scenarioName = args[2];
            String appName = args[3];
            int baseYear = Integer.parseInt(args[4]);
            int t = Integer.parseInt(args[5]);

            StatusLogger.logText(appName.toLowerCase(),appName + " has started for t" + t + ".");

            logger.info("Root Directory: " + rootDir);
            logger.info("Base Scenario Name: " + baseScenarioName);
            logger.info("Scenario Name: " + scenarioName);
            logger.info("App Name: " + appName);
            logger.info("Base Year: " + baseYear);
            logger.info("Time Interval: " + t);
            String configFileOrNodeName = null; //nodeName is for daf2, configFile is for daf3
            String slMode = null; //mode for sl
            if(args.length >= 7 ){
                if (appName.equalsIgnoreCase("SL")) {
                    slMode = args[6];
                    logger.info("Select Link mode: " + slMode);
                } else {
                    configFileOrNodeName = args[6];
                    logger.info("Daf Property: "+ configFileOrNodeName);
                }
            }
            Boolean tsDaily = null;
            if (args.length == 8) {
                //Note: "true", ignoring case, will yield a true boolean
                //  everything else will be false; so effectively there is
                //  a default of false which will pass through, even if
                // argument is junk
                tsDaily = Boolean.parseBoolean(args[7]);
                logger.info("Run TS daily: " + tsDaily);
            }

            //Create an ApplicationOrchestrator object that will handle creating the
            //resource bundle for the appropriate app, retrieve that resource bundle and
            //start the application, passing along the property bundle
            ApplicationOrchestrator ao = new ApplicationOrchestrator(rootDir,baseScenarioName, scenarioName,t,baseYear);

            //Before starting, AO needs to create the base year run log
            ao.createBaseYearPropFile();

            //For each t interval that the model runs in,  AO needs to create the runLogProperty file and write in the current year
            // if the file does not already exist in that year.  This file will then be updated by any application that runs.
            ao.createRunLogPropFile();

            //We need to read in the runLog.properties files starting
            //in year interval=0 and moving up to interval=t.  We will fill
            //up a hashmap with the appropriate values.
            ao.updateRunLogPropertiesHashmap(t);

            //Create a global.properties file for the current 't' directory
            // using the most recent globalTemplate.properties file
            String pathToAppRb = ao.createAppRb("global");

            ResourceBundle appRb = ao.findResourceBundle(pathToAppRb);
            ao.setRb(appRb);

            if(appName.equalsIgnoreCase("ED")){
                logger.info("AO will now start ED for simulation year " + (baseYear+t));
                ao.runEDModel(baseYear, t, appRb);
            }else if(appName.equalsIgnoreCase("NED")){
                logger.info("AO will now start NED for simulation year " + (baseYear+t));
                ao.runNEDModel(baseYear, t, appRb);
            }else if(appName.equalsIgnoreCase("EPF")){
                logger.info("AO will now start EPF for simulation year " + (baseYear+t));
                ao.runEPFModel(baseYear, t, appRb);
            }else if(appName.equalsIgnoreCase("ALD")){
                logger.info("AO will now start ALD for simulation year " + (baseYear+t));
                ao.runALDModel(baseYear, t, appRb);
            }else if(appName.equalsIgnoreCase("SPG1")){
                logger.info("AO will now start SPG1 for simulation year " + (baseYear+t));
                ao.runSPG1Model(baseYear,t,appRb, appRb);
            }else if (appName.equalsIgnoreCase("PI")){
                logger.info("AO will now start PI for simulation year " + (baseYear+t));
                ao.runPIModel(baseYear,t,appRb,appRb);
            } else if (appName.equalsIgnoreCase("AA")) {
                logger.info("AO will now start AA for simulation year " + (baseYear+t));
                ao.runAAModel(baseYear, t, appRb, appRb);
            }else if(appName.equalsIgnoreCase("PIDAF")){
                logger.info("AO will now start PIDAF for simulation year " + (baseYear+t));
                ao.runPIDAFModel(t,pathToAppRb,pathToAppRb,configFileOrNodeName);
            }else if (appName.equalsIgnoreCase("SPG2")){ //not a daf application
                logger.info("AO will now start SPG2 for simulation year " + (baseYear+t));
                ao.runSPG2Model(baseYear,t,appRb, appRb);
            }else if(appName.equalsIgnoreCase("PTDAF")){
                logger.info("AO will now start PTDAF for simulation year " + (baseYear+t));
                ao.runPTDAFModel(t, pathToAppRb,pathToAppRb,configFileOrNodeName);
            }else if(appName.equalsIgnoreCase("CT")){
                logger.info("AO will now start CT for simulation year " + (baseYear+t));
                ao.runCTModel(baseYear, t, appRb, appRb);
            }else if(appName.equalsIgnoreCase("ET")){
                logger.info("AO will now start ET for simulation year " + (baseYear+t));
                ao.runETModel(baseYear, t, appRb, appRb);
            }else if(appName.equalsIgnoreCase("TS")){
                logger.info("AO will now start TS for simulation year " + (baseYear+t));
                if (tsDaily == null) {
                    throw new RuntimeException("'daily' jvm argument required for TS not present, application will exit.");
                }
                ao.runTSModel(baseYear, t, appRb, appRb,tsDaily);
            }else if(appName.equalsIgnoreCase("TSDAF")){
                logger.info("AO will now start TS DAF peak & offPeak periods models for simulation year " + (baseYear+t));
                if (tsDaily == null) {
                    throw new RuntimeException("'daily' jvm argument required for TS not present, application will exit.");
                }
                ao.runTSDAFModel(baseYear, t, pathToAppRb, pathToAppRb, configFileOrNodeName,tsDaily);
            }else if(appName.equalsIgnoreCase("SL")){
                logger.info("AO will now start SL for simulation year " + (baseYear+t));
                ao.runSLModel(t,appRb,slMode);
            }else {
                logger.fatal("AppName not recognized");
            }

            ao.logAppRun(appName);
            logger.info(appName + " is complete");
            StatusLogger.logText(appName.toLowerCase(),appName + " has finished for t" + t + "."); 

        } catch (Exception e) {
            logger.fatal("An application threw the following error",e);
            StatusLogger.logText("unknown","Application did not finish due to an exception (check log files).");
            throw new RuntimeException("An application threw the following error", e);
        }
    }


}
