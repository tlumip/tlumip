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
import com.pb.tlumip.ald.ALDModel;
import com.pb.tlumip.ct.CTModel;
import com.pb.tlumip.ed.EDControl;
import com.pb.tlumip.model.ModelComponent;
import com.pb.tlumip.spg.SPGnew;
import com.pb.tlumip.ts.TS;
import com.pb.models.pecas.PIModel;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 
 * @author  Christi Willison
 * @version Jan 6, 2004
 * Created by IntelliJ IDEA.
 */
public class ApplicationOrchestrator {

    private static Logger logger = Logger.getLogger("com.pb.tlumip.ao");
    private String rootDir;
    private String scenarioName;
    private int t;
//    private int baseYear;
    AOProperties aoProps;
    ResourceBundle rb;
    BufferedWriter runLogWriter;
    File runLogPropFile;
    HashMap runLogHashmap;
    
//    CalibrationManager cManager;

    public ApplicationOrchestrator(){
        aoProps=new AOProperties();
    }

    public ApplicationOrchestrator(ResourceBundle rb){
        this.rb = rb;
    }

    public ApplicationOrchestrator(String rootDir, String scenarioName, int timeInterval){
        this.rootDir = rootDir;
        this.scenarioName = scenarioName;
        this.t = timeInterval;

    }

    public void setRb(ResourceBundle rb) {
        this.rb = rb;
    }

    public ResourceBundle getRb() {
        return rb;
    }
    
    private void createBaseYearPropFile(){
        runLogPropFile = new File(rootDir + "/scenario_" + scenarioName + "/t0/runLog.properties");
        if(!runLogPropFile.exists()){
            logger.info("Creating the base year run log in " + rootDir + "/scenario_" + scenarioName + "/t0/");
            try {
                runLogWriter = new BufferedWriter(new FileWriter(runLogPropFile, false));
                runLogWriter.write("# Filter values for properties files");
                runLogWriter.newLine();
                runLogWriter.write("BASEDIR=" + rootDir);
                runLogWriter.newLine();
                runLogWriter.write("SCENARIO_NAME=" + scenarioName);
                runLogWriter.newLine();
                runLogWriter.write("BASE_YEAR=0");
                runLogWriter.newLine();
                runLogWriter.write("CURRENT_YEAR=0");
                runLogWriter.newLine();
//                runLogWriter.write("GLOBAL_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("AO_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("ED_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("ALD_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("SPG_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("PI_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("CT_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("PT_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
//                runLogWriter.write("TS_TEMPLATE_YEAR=0");
//	            runLogWriter.newLine();
                runLogWriter.write("ED_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("ALD_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("SPG1_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("PI_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("SPG2_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("CT_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("PT_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.write("TS_LAST_RUN=0");
                runLogWriter.newLine();
                runLogWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }// else return - base year prop file has already been created.
    }

    private void createRunLogPropFile(){
        runLogPropFile = new File(rootDir + "/scenario_" + scenarioName + "/t" + t + "/runLog.properties");
        logger.info("Looking for the run log in " + rootDir + "/scenario_" + scenarioName + "/t" + t + "/");
        if(!runLogPropFile.exists()){ 
	        try { //create the file and write the current year into it.
	            logger.info("Writing the current year into the run log");
	            runLogWriter = new BufferedWriter(new FileWriter(runLogPropFile, false));
	            runLogWriter.write("# Filter values for properties files");
	            runLogWriter.newLine();
	            runLogWriter.write("BASEDIR=" + rootDir);
	            runLogWriter.newLine();
	            runLogWriter.write("SCENARIO_NAME=" + scenarioName);
	            runLogWriter.newLine();
	            runLogWriter.write("BASE_YEAR=0");
	            runLogWriter.newLine();
	            runLogWriter.write("CURRENT_YEAR=" + t);
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
        String initialRunLogPath = rootDir + "/scenario_" + scenarioName + "/t0/runLog.properties";
        ResourceBundle runLogRb = ResourceUtil.getPropertyBundle(new File(initialRunLogPath));
        runLogHashmap = ResourceUtil.changeResourceBundleIntoHashMap(runLogRb);
        
        //Now update the hashmap with the most current values by looking for runLog.properties
        //files in the t1, t2, up to tn directories and replacing any of the runLog properties
        //with the current values.
        int i = 1;
        File propFile;
        String keyName;
        String value;
        
        while (i<=t){
            String propPath = rootDir + "/scenario_" + scenarioName + "/t" + i + "/runLog.properties";
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
                }else logger.warn("NO RUN LOG EXISTS - PROPERTIES FILES WILL BE INCORRECT");
                i++;
            }
        }
       
    }
    
    private String createAppRb(String appName){
        //First read in the template properties file.  This will have default values and
        //tokens (surrounded by @ symbols).  The tokens will be replace with the values
        //in the runLogHashMap

        //Get Template File
        File appPropertyTemplate = findTemplateFileRecursively(appName);
        String appPropertyTemplateName = appPropertyTemplate.getName();

        //The property file will have the same name as the template file minus the 'Template' part.
        //Keep in mind that the 'appName' passed in will be spg1 or pidaf but the template and properties
        //files are spg.properties or pi.properties.  The 'findTemplateFileRecursively' method will strip off the
        //extra characters (like '1' or 'daf') and so the appPropFileName != appName but the appPropFileName
        //will be the one we want to use to locate and name the properties file.
        String appPropFileName = appPropertyTemplateName.substring(0,appPropertyTemplateName.length()-19);  //subtract off the 'Template.properties'
        String outputPath = rootDir + "/scenario_" + scenarioName + "/t" + t + "/";
        File appPropertyFile = null;

        if (appPropFileName.equalsIgnoreCase("global"))  appPropertyFile = new File(outputPath + "global.properties");
        else appPropertyFile = new File(outputPath + appPropFileName + "/" + appPropFileName + ".properties");

        logger.info("Creating " + appPropertyFile.getAbsolutePath());

        Properties appDefaultProps = new Properties();
        try {
            appDefaultProps.load(new FileInputStream(appPropertyTemplate));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Iterator keys = runLogHashmap.keySet().iterator();
	    while (keys.hasNext()) {
	        String keyName = (String) keys.next();
	        String hashmapValue = (String) runLogHashmap.get(keyName);
	        
	        //Build a pattern and compile it
	        String patternStr = "@" + keyName + "@";
	        Pattern pattern = Pattern.compile(patternStr);
	
	        // Replace all occurrences of pattern in input string
	        Enumeration propNames = appDefaultProps.propertyNames();
	        while(propNames.hasMoreElements()){
	            String propName = (String)propNames.nextElement();
	            String tempStr = new String(appDefaultProps.getProperty(propName));
	            Matcher matcher = pattern.matcher(tempStr);
	            tempStr = matcher.replaceAll(hashmapValue);
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

        return appPropertyFile.getAbsolutePath();
    }



    public File findTemplateFileRecursively(String appName) {
        File templateFile = null;
        String editedName = null;
        int i = t;  // look thru all of the tn directories for a property template file
                    // starting with the most recent t.
        while(i >= 0){
            String templatePath = rootDir + "/scenario_" + scenarioName + "/t" + i + "/";
            //Deal with SPG exception (appName=spg1 or spg2 but template file is spgTemplate.properties for both)
            if(appName.startsWith("spg")){
                editedName = "SPG";
                templateFile = new File(templatePath + "spg/spgTemplate.properties");
            //Deal with the PTDAF and PIDAF exceptions
            }else if (appName.endsWith("daf")) {
                editedName = appName.substring(0,(appName.length()-3)).toUpperCase();
                templateFile = new File(templatePath + appName.substring(0,(appName.length()-3)) +
                        "/" + appName.substring(0,(appName.length()-3)) + "Template.properties");//subtract off the 'daf' part
            }else if (appName.endsWith("constrained")){ //for the piConstrained runs.
                editedName = appName.substring(0,(appName.length()-11)).toUpperCase();
                templateFile = new File(templatePath + appName.substring(0,(appName.length()-11)) +
                        "/" + appName.substring(0,(appName.length()-11)) + "Template.properties");
            //Deal with the global exception
            } else if (appName.equalsIgnoreCase("global")) {
                editedName="GLOBAL";
                templateFile = new File(templatePath + "globalTemplate.properties");
            } else{
                editedName = appName.toUpperCase();
                templateFile = new File(templatePath + appName + "/" + appName + "Template.properties");
            }

            if(templateFile.exists()){
                logger.info("Full Path to Template File: " + templateFile.getAbsolutePath());
                String currentTemplate = (String) runLogHashmap.get(editedName + "_TEMPLATE_YEAR");
                if(currentTemplate == null){ // no previous template was found.
                    runLogHashmap.put(editedName + "_TEMPLATE_YEAR", Integer.toString(i));
                    try {
                        logger.info("Writing the initial template location for this app into the run log");
                        runLogWriter.write(editedName + "_TEMPLATE_YEAR=" + Integer.toString(i));
                        runLogWriter.newLine();
                        runLogWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                 } else if(!currentTemplate.equals(Integer.toString(i))){  //a more recent template file was found
                    try {
                        logger.info("Writing the new template location for this app into the run log");
                        runLogWriter.write(editedName + "_TEMPLATE_YEAR=" + Integer.toString(i));
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
        ResourceBundle rb = null;
        File propFile = new File(pathToRb);
        rb = ResourceUtil.getPropertyBundle(propFile);
        if(rb == null ) logger.fatal("Problem loading resource bundle: " + pathToRb);
        return rb;
    }



    
    public void writeRunParamsToPropertiesFile(int timeInterval, String pathToAppRb, String pathToGlobalRb, String moduleName){
        File runParams = new File(rootDir + "/daf/RunParams.properties");
        logger.info("Writing 'timeInterval' and 'pathToRb' into " + runParams.getAbsolutePath());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(runParams));
            writer.println("scenarioName=" + scenarioName);
            writer.println("timeInterval=" + timeInterval);
            writer.println("pathToAppRb=" + pathToAppRb);
            writer.println("pathToGlobalRb=" + pathToGlobalRb);
            writer.println("pecasName=" + moduleName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.close();
    }
    
    public void writeRunParamsToPropertiesFile(int timeInterval, String pathToAppRb, String pathToGlobalRb){
        File runParams = new File(rootDir + "/daf/RunParams.properties");
        logger.info("Writing 'timeInterval' and 'pathToRb' into " + runParams.getAbsolutePath());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(runParams));
            writer.println("scenarioName=" + scenarioName);
            writer.println("timeInterval=" + timeInterval);
            writer.println("pathToAppRb=" + pathToAppRb);
            writer.println("pathToGlobalRb=" + pathToGlobalRb);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.close();
    }
    
    
    public void runEDModel(int timeInterval, ResourceBundle appRb, int baseYear){

        ModelComponent comp = new EDControl(baseYear,timeInterval,appRb);
        comp.startModel(timeInterval);

    }

    public void runALDModel(int timeInterval, ResourceBundle appRb){
        ModelComponent comp = new ALDModel();
        comp.setApplicationResourceBundle(appRb);
        comp.startModel(timeInterval);
    }

    public void runSPG1Model(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){

        String baseYear = timeInterval < 10 ? "1990" : "2000";
        String currentYear = Integer.toString((Integer.valueOf(baseYear) + Integer.valueOf(timeInterval)));

        SPGnew spg = new SPGnew( appRb, globalRb, baseYear, currentYear );

		spg.getHHAttributesFromPUMS(baseYear);
        spg.spg1(currentYear);
        spg.writeFrequencyTables ();
		TableDataSet table = spg.sumHouseholdsByIncomeSize();
        spg.writePiInputFile ( table );

    }

    public void runPIModel(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){
        PIModel pi = new PIModel();
        pi.setResourceBundles(appRb, globalRb);
        pi.startModel(timeInterval);
    }

    public void runPIDAFModel(int timeInterval, String pathToAppRb, String pathToGlobalRb, String nodeName){
        //Since AO doesn't communicate directly with PI we need to write the absolute
        //path to the resource bundle and the time interval into a file, "RunParams.txt"
        //that will be read by the PIServer Task when the PIDAF application is launched.
        writeRunParamsToPropertiesFile(timeInterval, pathToAppRb, pathToGlobalRb,"pi");
        StartDafApplication appRunner = new StartDafApplication("pidaf", nodeName, timeInterval, rb);
        appRunner.run();
    }
    
    public void runPICONSTRAINEDModel(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){
        PIModel pi = new PIModel();
        pi.setResourceBundles(appRb, globalRb);
        pi.startConstrainedModel(timeInterval);
    }

    public void runSPG2Model(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){

        String baseYear = timeInterval < 10 ? "1990" : "2000";
        String currentYear = Integer.toString((Integer.valueOf(baseYear) + Integer.valueOf(timeInterval)));

        SPGnew spg = new SPGnew( appRb, globalRb, baseYear, currentYear );

        spg.spg2();
        spg.writeHHOutputAttributesFromPUMS(baseYear);

    }

    public void runPTDAFModel(int timeInterval, String pathToAppRb, String pathToGlobalRb,String nodeName){
        writeRunParamsToPropertiesFile(timeInterval, pathToAppRb, pathToGlobalRb);
        StartDafApplication appRunner = new StartDafApplication("ptdaf", nodeName, timeInterval, rb);
        appRunner.run();
    }

    public void runCTModel(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){
        ModelComponent comp = new CTModel(appRb, globalRb);
        comp.startModel(timeInterval);

    }

    public void runTSModel(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){

		TS ts = new TS(appRb, globalRb);
		ts.runHighwayAssignment( "peak" );
		ts.runHighwayAssignment( "offpeak" );

    }
    
    private void logAppRun(String appName){
        if (appName.endsWith("daf")) {
            appName = appName.substring(0,(appName.length()-3));
        }
        try {
            logger.info("Writing the application name and the timeInterval for run into the run log");
            runLogWriter.write(appName.toUpperCase() + "_LAST_RUN=" + t);
            runLogWriter.newLine();
            runLogWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public void addPropertiesObject(AOProperties properties){
        this.aoProps = properties;
    }

    private Digester initDigester(Digester digester){

        //we are setting up rules to parse the AOProperties.xml file
        //addObjectCreate() creates an aoProperty object
        //first param is the tag pattern that we want to identify and set a rule for
        //second param is the default class name
        //third param will override the default class name if it is an attribute in the xml file.
        digester.addObjectCreate("propertiesObject", "com.pb.tlumip.model.ModelProperties", "className");

        digester.addCallMethod("propertiesObject/property","setProperty",2);
        digester.addCallParam("propertiesObject/property/name",0);
        digester.addCallParam("propertiesObject/property/value",1);

        digester.addObjectCreate("propertiesObject/resource","com.pb.tlumip.ao.Resource"); //create a Resource object
        digester.addSetProperties("propertiesObject/resource");  //will set the "name" field in the resource object
        digester.addBeanPropertySetter("propertiesObject/resource/type"); //will set the "type" field in the resource object
        digester.addBeanPropertySetter("propertiesObject/resource/ip");//sets the "ip" field in resource object
        digester.addBeanPropertySetter("propertiesObject/resource/status"); //set the "status" field in resource object
        digester.addSetNext("propertiesObject/resource","addResource"); //adds the resource to the AOProperties object and pops the
                                                                        //resource object off the stack

        digester.addObjectCreate("propertiesObject/scenario","com.pb.tlumip.ao.Scenario"); //create a Scenario object
        digester.addSetProperties("propertiesObject/scenario"); //will set the "name" field in the scenario object
        digester.addBeanPropertySetter("propertiesObject/scenario/start"); //sets the value of "start"
        digester.addBeanPropertySetter("propertiesObject/scenario/end");  //sets the value of "end"
        digester.addSetNext("propertiesObject/scenario","addScenario"); //adds Scenario object to AOProperties object and pops off the stack

        digester.addObjectCreate("propertiesObject/intervalUpdate","com.pb.tlumip.ao.IntervalUpdate");
        digester.addSetProperties("propertiesObject/intervalUpdate"); //sets "year" field in IntervalUpdate object
        digester.addBeanPropertySetter("propertiesObject/intervalUpdate/fileUpdates/file"); //sets an array of file updates
        digester.addBeanPropertySetter("propertiesObject/intervalUpdate/policyFileUpdates/policyFile"); //sets an array of policy file updates
        digester.addSetNext("propertiesObject/intervalUpdate","addIntervalUpdate"); //adds to AOProperties and pops off the stack

        //addSetNext()
        //first param is the tag pattern
        //second param is the method in the ApplicationOrchestrator that will be called
        //to pass back the object that was just created.
        digester.addSetNext("propertiesObject","addPropertiesObject"); //adds the AOProperties object to the ApplicationOrchestrator and pops
                                                                        //off the stack.
        return digester;

    }



    public String toString(){
        String newline = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();

        buf.append("--- properties ---").append(newline);
        buf.append(aoProps.getPropertyMap()).append(newline).append(newline);

        buf.append("---resources---").append(newline);
        for(int i=0;i<aoProps.getResourceList().size();i++)
            buf.append(aoProps.getResourceList().get(i)).append(newline);

        buf.append("--scenario---").append(newline);
        buf.append(aoProps.getScenario()).append(newline);

        buf.append("---interval updates---").append(newline);
        for(int i=0;i<aoProps.getIntervalUpdateList().size();i++)
            buf.append(aoProps.getIntervalUpdateList().get(i)).append(newline);

        return buf.toString();

    }


    public static void main(String[] args) {

        try {
            //Get command line arguments: Scenario Name, Application Name,
            //                            Time Interval and Start Node (for daf applications)
            String rootDir = args[0];
            String scenarioName = args[1];
            String appName = args[2];
            int t = Integer.parseInt(args[3]);

            logger.info("Root Directory: " + rootDir);
            logger.info("Scenario Name: " + scenarioName);
            logger.info("App Name: " + appName);
            logger.info("Time Interval: " + t);
            String nodeName = null; //will only be passed in for daf applications
            if(args.length > 4 ){
                nodeName = args[4];
            	logger.info("Node to Start Cluster: "+ nodeName);
            }

            //Create an ApplicationOrchestrator object that will handle creating the
            //resource bundle for the appropriate app, retrieve that resource bundle and
            //start the application, passing along the property bundle
            ApplicationOrchestrator ao = new ApplicationOrchestrator(rootDir,scenarioName,t);

            //Before starting in year 1, AO needs to create the base year run log
            ao.createBaseYearPropFile();

            //For each year (starting in year 1) AO needs to create the runLogProperty file and write in the current year
            // if the file does not already exist in that year.  This file will then be updated by any application that runs.
            ao.createRunLogPropFile();
            
            //We need to read in the runLog.properties files starting 
            //in year interval=0 and moving up to interval=t.  We will fill 
            //up a hashmap with the appropriate values. 
            ao.updateRunLogPropertiesHashmap(t);
            
            //Create an ao.properties file for the current 't' directory
            // using the most recent aoTemplate.properties file
            String pathToAoRb = ao.createAppRb("ao");

            //Get the ao.properties file that was just created and read in the "base.year"
            //value from the ao.properties so that you can pass it to the application that you are starting.
            ResourceBundle aoRb = ao.findResourceBundle(pathToAoRb);
            ao.setRb(aoRb);
            int baseYear = Integer.parseInt(ResourceUtil.getProperty(ao.getRb(), "base.year"));

            //Create a global.properties file for the current 't' directory
            // using the most recent globalTemplate.properties file
            String pathToGlobalRb = ao.createAppRb("global");

            //Get the global properties file that was just created so that you can pass it to the current application
            ResourceBundle globalRb = null;
            if(!appName.endsWith("daf")){
                globalRb = ao.findResourceBundle(pathToGlobalRb);
            }

            //Read in the appNameTemplate.properties and replace
            //all patterns with values from the RunLogHashMap and write properties file
            //to the appropriate directory.
            String pathToAppRb = ao.createAppRb(appName);


            //Unless AO is being asked to start a daf-application, it should
            //find the actual application resource bundle and pass it to the app
            //on start-up.  Daf applications will be passed a path to a runProperties file
            //instead of the actual file.
            ResourceBundle appRb = null;
            if(! appName.endsWith("daf")){
                appRb = ao.findResourceBundle(pathToAppRb);
            }

            if(appName.equalsIgnoreCase("ED")){
                logger.info("AO will now start ED for simulation year " + (baseYear+t));
                ao.runEDModel(t, appRb, baseYear);
            }else if(appName.equalsIgnoreCase("ALD")){
                logger.info("AO will now start ALD for simulation year " + (baseYear+t));
                ao.runALDModel(t, appRb);
            }else if(appName.equalsIgnoreCase("SPG1")){
                logger.info("AO will now start SPG1 for simulation year " + (baseYear+t));
            	ao.runSPG1Model(t,appRb, globalRb);
            }else if (appName.equalsIgnoreCase("PI")){
                logger.info("AO will now start PI for simulation year " + (baseYear+t));
                ao.runPIModel(t,appRb,globalRb);
            }else if(appName.equalsIgnoreCase("PIDAF")){
                logger.info("AO will now start PIDAF for simulation year " + (baseYear+t));
                ao.runPIDAFModel(t,pathToAppRb,pathToGlobalRb,nodeName);
            }else if(appName.equalsIgnoreCase("PICONSTRAINED")){
                logger.info("AO will now start PICONSTRAINED for simulation year " + (baseYear + t));
                ao.runPICONSTRAINEDModel(t,appRb,globalRb);
            }else if (appName.equalsIgnoreCase("SPG2")){ //not a daf application
                logger.info("AO will now start SPG2 for simulation year " + (baseYear+t));
                ao.runSPG2Model(t,appRb, globalRb);
            }else if(appName.equalsIgnoreCase("PTDAF")){
                logger.info("AO will now start PTDAF for simulation year " + (baseYear+t));
                ao.runPTDAFModel(t, pathToAppRb,pathToGlobalRb,nodeName);

            }else if(appName.equalsIgnoreCase("CT")){
                logger.info("AO will now start CT for simulation year " + (baseYear+t));
                ao.runCTModel(t, appRb, globalRb);
            }else if(appName.equalsIgnoreCase("TS")){
                logger.info("AO will now start TS for simulation year " + (baseYear+t));
                ao.runTSModel(t, appRb, globalRb);
            }else {
                logger.fatal("AppName not recognized");
            }


//        ApplicationOrchestrator ao = new ApplicationOrchestrator();
//        Digester digester = new Digester();
//        digester.push(ao); //is supposed to put the application orchestator on the stack
//        digester = ao.initDigester(digester);
//        try {
//            digester.parse(new File("c:/code/tlumip/config/AOProperties.xml"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (SAXException e) {
//            e.printStackTrace();
//        }
//        System.out.println(ao.toString());

            ao.logAppRun(appName);
            logger.info(appName + " is complete");
            
            
        } catch (Exception e) {
            logger.fatal("An application threw the following error");
            logger.fatal(e);
            e.printStackTrace();
            System.exit(1);
        }
    }




}
