package com.pb.despair.ao;

import org.apache.commons.digester.Digester;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.pb.despair.model.ModelComponent;
import com.pb.despair.ed.EDControl;
import com.pb.despair.pi.PIModel;
import com.pb.despair.ald.ALDModel;
import com.pb.despair.spg.SPGnew;
import com.pb.despair.ct.CTModel;
import com.pb.despair.ts.TS;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

/**
 * 
 * 
 * @author  Christi Willison
 * @version Jan 6, 2004
 * Created by IntelliJ IDEA.
 */
public class ApplicationOrchestrator {

    private static Logger logger = Logger.getLogger("com.pb.despair.ao");
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
    
    private void createRunLogPropFile(){
        runLogPropFile = new File(rootDir + "/scenario_" + scenarioName + "/t" + t + "/runLog.properties");
        logger.info("Looking for the run log in " + rootDir + "/scenario_" + scenarioName + "/t" + t + "/");
        if(!runLogPropFile.exists()){ 
	        try { //create the file and write the current year into it.
	            logger.info("Writing the current year into the run log");
	            runLogWriter = new BufferedWriter(new FileWriter(runLogPropFile, true));
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
        File propFile = null;
        String keyName = null;
        String value = null;
        
        while (i<=t){
            String propPath = rootDir + "/scenario_" + scenarioName + "/t" + i + "/runLog.properties";
            propFile = new File(propPath);
            if(propFile.exists()){
                logger.info(" Reading in Run Log from year " + i + " and updating the RunLog HashMap: ");
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
    
    private void createAppRb(String appName){
        //First read in the template properties file.  This will have default values and
        //tokens (surrounded by @ symbols).  The tokens will be replace with the values
        //in the runLogHashMap
        File appPropertyTemplate = null;
        File appPropertyFile = null;
        String templatePath = rootDir + "/scenario_" + scenarioName + "/t0/";
        String outputPath = rootDir + "/scenario_" + scenarioName + "/t" + t + "/";
        //Deal with SPG exception (appName=spg1 or spg2 but properties file is spg.properties for both)
        if(appName.startsWith("spg")){
            appPropertyTemplate = new File(templatePath + "spg/spgTemplate.properties");
            appPropertyFile =  new File(outputPath + "spg/spg.properties");
        //Deal with the PTDAF and PIDAF exceptions
        }else if (appName.endsWith("daf")) {
            appName = appName.substring(0,(appName.length()-3));
            appPropertyTemplate = new File(templatePath + appName +
                    "/" + appName + "Template.properties"); //subtract off the 'daf' part
            appPropertyFile = new File(outputPath + appName +
                    "/" + appName + ".properties");
        } else if (appName.equalsIgnoreCase("global")) {
            appPropertyTemplate = new File(templatePath + "globalTemplate.properties");
            appPropertyFile = new File(outputPath + "global.properties");
        } else{
            appPropertyTemplate = new File(templatePath + appName + "/" + appName + "Template.properties");
            appPropertyFile = new File(outputPath + appName + "/" + appName + ".properties");
        }
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
	    
	    try {
            appDefaultProps.store(new FileOutputStream(appPropertyFile), appName.toUpperCase() + " Properties File for Interval " + t);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /* PTDAF and PIDAF just need the path to the resource bundle
    * while all other modules need the actual bundle
    */
    public String findPathToResourceBundle(String appName) {
        String pathToRb = null;

        int i = t;  // look thru all of the tn directories for a properties file
                    // starting with the most recent t.
        File propFile = null;
        while(i >= 0){
            String propPath = rootDir + "/scenario_" + scenarioName + "/t" + i + "/";
            //Deal with SPG exception (appName=spg1 or spg2 but properties file is spg.properties for both)
            if(appName.startsWith("spg")){
                propFile = new File(propPath + "spg/spg.properties");
            //Deal with the PTDAF and PIDAF exceptions
            }else if (appName.endsWith("daf")) {
                propFile = new File(propPath + appName.substring(0,(appName.length()-3)) +
                        "/" + appName.substring(0,(appName.length()-3)) + ".properties"); //subtract off the 'daf' part
            //Deal with the global exception
            } else if (appName.equalsIgnoreCase("global")) {
                propFile = new File(propPath + "global.properties");
            } else{
                propFile = new File(propPath + appName + "/" + appName + ".properties");
            }

            if(propFile.exists()){
                pathToRb = propFile.getAbsolutePath();
                logger.info(" Full Path to Properties File: " + pathToRb);
                return pathToRb;
            }else {
                i--;
            }
        }
        // if you get to here it means a properties file couldn't be found in any of the tn directories
        // so pathToRb = null
        logger.fatal("Couldn't find Resource Bundle Path for " + appName + ".  Returning null");
        return pathToRb;
    }

    public ResourceBundle findResourceBundle(String pathToRb) {
        ResourceBundle rb = null;
        File propFile = new File(pathToRb);
        rb = ResourceUtil.getPropertyBundle(propFile);
        if(rb == null ) logger.fatal("Problem loading resource bundle: " + pathToRb);
        return rb;
    }


    public void writeRunParamsToFile(int timeInterval, String pathToAppRb, String pathToGlobalRb){
        File runParams = new File(rootDir + "/daf/RunParams.txt");
        logger.info("Writing 'timeInterval' and 'pathToRb' into " + runParams.getAbsolutePath());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(runParams));
            writer.println(scenarioName);
            writer.println(timeInterval);
            writer.println(pathToAppRb);
            writer.println(pathToGlobalRb);
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

		SPGnew testSPG = new SPGnew(appRb, globalRb);

		testSPG.getHHAttributesFromPUMS();
		testSPG.spg1();
		testSPG.writeFrequencyTables ();
		TableDataSet table = testSPG.sumHouseholdsByIncomeSize();
		testSPG.writePiInputFile ( table );

    }

    public void runPIModel(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){
        ModelComponent comp = new PIModel();
        comp.setResourceBundles(appRb, globalRb);
        comp.startModel(timeInterval);
    }

    public void runPIDAFModel(int timeInterval, String pathToAppRb, String pathToGlobalRb, String nodeName){
        //Since AO doesn't communicate directly with PI we need to write the absolute
        //path to the resource bundle and the time interval into a file, "RunParams.txt"
        //that will be read by the PIServer Task when the PIDAF application is launched.
        writeRunParamsToPropertiesFile(timeInterval, pathToAppRb, pathToGlobalRb);
        StartDafApplication appRunner = new StartDafApplication("pidaf", nodeName, timeInterval, rb);
        appRunner.run();
    }

    public void runSPG2Model(int timeInterval, ResourceBundle appRb, ResourceBundle globalRb){

		SPGnew testSPG = new SPGnew(appRb, globalRb);
        testSPG.spg2();
        testSPG.writeHHOutputAttributesFromPUMS();

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
        ts.assignPeakAuto();
        ts.assignOffPeakAuto();

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
        digester.addObjectCreate("propertiesObject", "com.pb.despair.model.ModelProperties", "className");

        digester.addCallMethod("propertiesObject/property","setProperty",2);
        digester.addCallParam("propertiesObject/property/name",0);
        digester.addCallParam("propertiesObject/property/value",1);

        digester.addObjectCreate("propertiesObject/resource","com.pb.despair.ao.Resource"); //create a Resource object
        digester.addSetProperties("propertiesObject/resource");  //will set the "name" field in the resource object
        digester.addBeanPropertySetter("propertiesObject/resource/type"); //will set the "type" field in the resource object
        digester.addBeanPropertySetter("propertiesObject/resource/ip");//sets the "ip" field in resource object
        digester.addBeanPropertySetter("propertiesObject/resource/status"); //set the "status" field in resource object
        digester.addSetNext("propertiesObject/resource","addResource"); //adds the resource to the AOProperties object and pops the
                                                                        //resource object off the stack

        digester.addObjectCreate("propertiesObject/scenario","com.pb.despair.ao.Scenario"); //create a Scenario object
        digester.addSetProperties("propertiesObject/scenario"); //will set the "name" field in the scenario object
        digester.addBeanPropertySetter("propertiesObject/scenario/start"); //sets the value of "start"
        digester.addBeanPropertySetter("propertiesObject/scenario/end");  //sets the value of "end"
        digester.addSetNext("propertiesObject/scenario","addScenario"); //adds Scenario object to AOProperties object and pops off the stack

        digester.addObjectCreate("propertiesObject/intervalUpdate","com.pb.despair.ao.IntervalUpdate");
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
        
        //AO needs to create the runLogProperty file and write in the current year.  This file will 
        //then be updated by any application that runs.
        ao.createRunLogPropFile();
        
        //We need to read in the runLog.properties files starting 
        //in year interval=0 and moving up to interval=t.  We will fill 
        //up a hashmap with the appropriate values. 
        ao.updateRunLogPropertiesHashmap(t);
        
        //Get the ao.properties file updated with values
        ao.createAppRb("ao");
        ao.createAppRb("global");
        
        //Read in the appNameTemplate.properties from the t0 directory and replace
        //all patterns with values from the RunLogHashMap and write properties file
        //to the appropriate directory.
        ao.createAppRb(appName);
        ResourceBundle aoRb = ao.findResourceBundle(ao.findPathToResourceBundle("ao"));
        ao.setRb(aoRb);
        int baseYear = Integer.parseInt(ResourceUtil.getProperty(ao.getRb(), "base.year"));

        String pathToAppRb = ao.findPathToResourceBundle(appName);
        ResourceBundle appRb = null;
        if(! appName.endsWith("daf")){
            appRb = ao.findResourceBundle(pathToAppRb);
        }

        String pathToGlobalRb = ao.findPathToResourceBundle("global");
        ResourceBundle globalRb = null;
        if(!appName.endsWith("daf")){
            globalRb = ao.findResourceBundle(pathToGlobalRb);
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
    }




}
