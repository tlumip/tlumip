package com.pb.despair.pt.daf;

import com.pb.common.daf.*;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.BooleanLock;
import com.pb.common.util.ObjectUtil;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.despair.pi.*;
import com.pb.despair.model.OverflowException;
import com.pb.despair.pt.PTDataReader;
import com.pb.despair.pt.PTHousehold;
import com.pb.despair.pt.PTPerson;
import com.pb.despair.pt.OccupationCode;

import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.*;

/**
 * @author Christi Willison
 * @version Mar 18, 2004
 */
public class PTServerTask extends Task{

    Logger logger = Logger.getLogger("com.pb.common.despair.pt.daf");
    protected static BooleanLock signal = new BooleanLock(false);
    boolean CREATE_MODE_CHOICE_LOGSUMS=true;
    ResourceBundle ptRb;
    AlphaToBeta a2b;

    public PTPerson[] persons; // will be initialized in the doWork method
    public PTHousehold[] households;

    public void onStart(){
        logger.info( "***" + getName() + " started");

        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        String scenarioName = null;
        int timeInterval = -1;
        String pathToRb = null;
        try {
            logger.info("Reading RunParams.txt file");
            reader = new BufferedReader(new FileReader(new File( Scenario.runParamsFileName )));
            scenarioName = reader.readLine();
            logger.info("\tScenario Name: " + scenarioName);
            timeInterval = Integer.parseInt(reader.readLine());
            logger.info("\tTime Interval: " + timeInterval);
            pathToRb = reader.readLine();
            logger.info("\tResourceBundle Path: " + pathToRb);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.ptRb = ResourceUtil.getPropertyBundle(new File(pathToRb));

        String file = ResourceUtil.getProperty(ptRb,"alphaToBeta.file");
        this.a2b = new AlphaToBeta(new File(file));


    }

    public void doWork(){
        long startTime = System.currentTimeMillis();
        //Read in from the properties file the number of nodes
        //so that we know how many worker ports we need
        ResourceBundle rb = ResourceUtil.getResourceBundle("ptdaf");
        int nNodes = Integer.parseInt(ResourceUtil.getProperty(rb,"nodes"));
        int nWorkQueues = Integer.parseInt(ResourceUtil.getProperty(rb,"workQueues"));
        int nWorkers = Integer.parseInt(ResourceUtil.getProperty(rb,"workers"));

        PortManager pManager = PortManager.getInstance();
        MessageFactory mFactory = MessageFactory.getInstance();
        Message[] msgs;

        //Get ports to the worker queues to communicate with a queue

        //SignalTask Queue (used to initialize nHHs and nPersons)
        Port SignalQueue = pManager.createPort("SignalQueue");

        Port ModeChoiceServerQueue = pManager.createPort("MCServerQueue");

        //Setup Work Ports
        Port[] SetupWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            SetupWorkPorts[i] = pManager.createPort("SetupWorkQueue_"+ (i+1));
        }

        //Workplace Location Work Ports
        Port[] WLWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            WLWorkPorts[i] = pManager.createPort("WLWorkQueue_"+ (i+1));
        }

        //Update Taz Data Work Ports
        Port[] UTDWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            UTDWorkPorts[i] = pManager.createPort("UTDWorkQueue_"+ (i+1));
        }

        //Aggregate Destination Choice Logsum Work Ports
        Port[] ADCLWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            ADCLWorkPorts[i] = pManager.createPort("ADCLWorkQueue_"+ (i+1));
        }

        //Household Work Ports
        Port[] HHWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            HHWorkPorts[i] = pManager.createPort("HHWorkQueue_"+ (i+1));
        }

        //Send a message to the SignalQueue to initialize the number of worker queues.  It needs
        //to be able to track when all the nodes have initialized.
        Message msg = mFactory.createMessage();
        msg.setId(MessageID.NUM_OF_WORK_QUEUES);
        msg.setValue("nWorkQueues",new Integer(nWorkQueues));
        msg.setValue("nWorkers", new Integer(nWorkers));
        SignalQueue.send(msg);

        //Set up PT on all nodes
        msgs = createSetupMessages(mFactory,nWorkQueues);
        sendWorkToWorkQueues(msgs,SetupWorkPorts,nWorkQueues);

        //Wait while nodes set up.  The nodes will send a message to the SignalTask when they are finished.
        // The SignalTask will call PTServerTask.signalResultsProcessed to flip the signal, triggering control
        // to exit out of the try-catch loop.
        long waitTime = System.currentTimeMillis();
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for " +
                        "nodes to set up");

        //If required, send messages to the workers to calculate the Mode Choice Logsums.
        //Else, send messages to the workers to calculate the Workplace Locations.
        if(CREATE_MODE_CHOICE_LOGSUMS){
            Message startMCLogsumsMsg = mFactory.createMessage();
            startMCLogsumsMsg.setId(MessageID.NUM_OF_WORK_QUEUES);
            startMCLogsumsMsg.setValue("appRb",ptRb);
            startMCLogsumsMsg.setValue("nWorkQueues",new Integer(nWorkQueues));
            startMCLogsumsMsg.setValue("alphaToBetaMap", a2b);
            ModeChoiceServerQueue.send(startMCLogsumsMsg);

            //Set 'signal' to false and when the mode choice logsums have all been
            //calculated the SignalTask will flip the value to true and we will know
            //that all mc logsums have been calculated.  We don't want to wait for
            //that to happen as we can run the AutoOwnership model on the master node
            //in the meantime but we need to be sure that the logsums are done before
            //we go on to the workplace location model.  We will, therefore, check the signal
            //value after the AutoOwnershipModel is run before moving on to the
            //Workplace Location model.
            //are complete and saved in the local node's memory (ie. node 0's memory)
            signal.setValue(false);  //not yet done
        }

        //Read in the households and the persons and run the AutoOwnership.  This needs
        //to happen regardless of the ModeChoiceLogsum calculations and no workers can work
        //until we initialize the households.
        PTDataReader dataReader = new PTDataReader(rb);

        logger.info("Adding synthetic population from database");
        households = dataReader.readHouseholds("households.file");
        //send the number of households to the SignalQueue so that the SignalTask can initialize
        //the number of households. (it will be used to determine when all HHs are processed)
        Message nHHMsg = mFactory.createMessage();
        nHHMsg.setId(MessageID.NUM_OF_HHS);
        nHHMsg.setValue("nHHs", new Integer(households.length));
        SignalQueue.send(nHHMsg);
        logger.fine("Size of households: " + ObjectUtil.sizeOf(households));

        logger.info("Reading the Persons file");
        persons = dataReader.readPersons("persons.file");
        //send the number of persons to the SignalQueue so that the SignalTask can initialize
        //the number of persons. (it will be used to determine when all Persons are processed)
        Message nPersonsMsg = mFactory.createMessage();
        nPersonsMsg.setId(MessageID.NUM_OF_PERSONS);
        nPersonsMsg.setValue("nPersons", new Integer(persons.length));
        SignalQueue.send(nPersonsMsg);

        dataReader.addPersonInfoToHouseholdsAndHouseholdInfoToPersons(households,
                persons);

        logger.info("Starting the AutoOwnershipModel");
        households = dataReader.runAutoOwnershipModel(households);

        //Now we are ready for the WorkplaceLocation calculations
        //so if we did the MC logsums check to see if we are done
        if(CREATE_MODE_CHOICE_LOGSUMS){
            if(signal.isFalse()){ //we must not be done yet so just wait
                waitTime = System.currentTimeMillis();
                try {
                    signal.waitUntilStateChanges(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0
                        + " secs for nodes to set up");
            }
        }

        //So either we are done with creating the MC Logsums or we never needed to create them and we are
        //now ready for the WorkplaceLocation calcs
        msgs = createWorkplaceLocationWorkMessages(mFactory);
        sendWorkToWorkQueues(msgs,WLWorkPorts,nWorkQueues);

        //Wait for a signal from SignalTask that all calcs are complete and that the persons array
        //has been updated with the new persons.  This will be done by the SignalTask
        waitTime = System.currentTimeMillis();
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for " +
                        "CU calculations");

        //Once the WorkplaceLocationTask figures out the workplace location it will send back the
        //person array to the SignalTask which will integrate the persons back into the persons array
        //We need to update the workers TAZDATA with the number of school workers by TAZ and also
        //the number of households by TAZ so send off this info to each node
        msgs = createTazUpdateWorkMessages(mFactory, nWorkQueues);
        sendWorkToWorkQueues(msgs,UTDWorkPorts,nWorkQueues);

        //Wait for a signal from SignalTask that all workers have updated their Taz Data
        waitTime = System.currentTimeMillis();
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for " +
                        "TAZ data updates");

        //Once TAZ data has been updated we are reading to hand out the work for pre-calculating the DC logsums
        msgs = createDCLogsumWorkMessages(mFactory);
        sendWorkToWorkQueues(msgs, ADCLWorkPorts, nWorkQueues);

        //Wait for a signal from SignalTask that all DCLogsums have been calculated and written to file
        //The workers will send the calculated values to PTDafWriter and PTDafWriter will send a message to the
        //SignalTask as if finishes writing each Matrix.  The signal task will wait for all dc logsums to be
        //written and then it will signal that we are ready to go on.
        waitTime = System.currentTimeMillis();
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for " +
                        "DC Logsum calculations");

        //After the DC Logsums have been calculated we are ready to begin processing the households.
        //Due to the relatively long processing time of each household and the household size it is not possible
        //to put all the households into a message array and send them off to the workers.  Instead we will send off
        //20 messages, with 5000 households each, to each workQueue with a 'sendMore' value set to 1 on the 18th message
        //which will be used to noticy the ServerTask to send another set of household messages.
        msgs = createHHWorkMessages(mFactory, nWorkQueues);
        sendWorkToWorkQueues(msgs, HHWorkPorts, nWorkQueues);

        logger.info("Total Time in seconds: "+((System.currentTimeMillis()-startTime)/1000));
        File doneFile = new File(ResourceUtil.getProperty(rb,"done.file"));
        if(!doneFile.exists()){
            try {
                doneFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("Done File has been written");
        return;
    } //end of doWork method.

    /* **********************************************************************************************************
    ******* private methods to create message arrays and send off to workers ***********************************/

    private Message[] createSetupMessages(MessageFactory mFactory, int nWorkQueues){
        Message[] messages =  new Message[nWorkQueues];
        for(int i=0;i<messages.length;i++){
            Message msg = mFactory.createMessage();
            msg.setId("SetupMessage");
            messages[i]=msg;
        }
        return messages;
    }


    

    private Message[] createWorkplaceLocationWorkMessages(MessageFactory mFactory){
        //Sort the person array by segment, occupation code so the first person will
        //have segment code 0, occupation code 0 followed by segment code 0, occupation code 1, etc.
        Arrays.sort(persons); //sorts persons by workSegment (0-8) and then by occupation code (0-8)

        //We want to find all persons that match a particular segment/occupation pair and send those
        //off to a worker to be processed.
        int index = 0; //index will keep track of where we are in the person array
        int nUnemployed = 0;
        int nMsgs = 0;
        ArrayList unemployedPersonList = new ArrayList();
        ArrayList personList = new ArrayList();
        ArrayList msgList = new ArrayList();

        while(index < persons.length){
            int segment = persons[index].householdWorkSegment;
            int occupation = persons[index].occupation;
            int nPersons = 0;  //number of people in subgroup for the seg/occ pair.
            while(persons[index].householdWorkSegment == segment && persons[index].occupation == occupation){
                if(persons[index].occupation == 0){  //person is unemployed, save for later
                    nUnemployed++;
                    unemployedPersonList.add(persons[index]);
                    index++; //go to next person
                }else {
                    nPersons++;
                    personList.add(persons[index]);
                    index++;
                }
            }
            if(nPersons > 0){ //there were persons that matched the seg/occ pair (occ != 0)
                PTPerson[] personsSubset = new PTPerson[nPersons];
                personList.toArray(personsSubset);
                Message msg = mFactory.createMessage();
                msg.setId(MessageID.CALCULATE_WORKPLACE_LOCATIONS);
                msg.setValue("persons",personsSubset);
                msgList.add(msg);
                nMsgs++;

                personList.clear(); //empty the array list so that we can put the
                                    //next group of persons in it.
            }
        }
        //Once the entire list of persons has been processed, we need to put the unemployed
        // persons back into the persons array and convert the
        //message list into an array of messages so that they can be distributed to
        //the workers.
        logger.fine("Total unemployed persons: " + unemployedPersonList.size());
        Iterator iter = unemployedPersonList.iterator();
        while (iter.hasNext()){
            persons[nUnemployed-1] = (PTPerson) iter.next();  //the order doesn't matter so just start at
            nUnemployed--;                                //the array position corresponding to the nUnemployed-1
        }                                                 //and work backward.

        Message[] messages = new Message[nMsgs];
        msgList.toArray(messages);
        return messages;
    }

    private Message[] createTazUpdateWorkMessages(MessageFactory mFactory, int nWorkQueues){
        Message[] messages =  new Message[nWorkQueues];
        int[] householdsByTaz = new int[Ref.MAX_ZONE_NUMBER+1];
        int[] postSecOccup = new int[Ref.MAX_ZONE_NUMBER+1];
        int[] otherSchoolOccup = new int[Ref.MAX_ZONE_NUMBER+1];

        for(int p=0;p<persons.length;p++){
            if(persons[p].occupation==OccupationCode.P0ST_SEC_TEACHERS)
                postSecOccup[persons[p].workTaz]++;
            else if(persons[p].occupation==OccupationCode.OTHER_TEACHERS)
                otherSchoolOccup[persons[p].workTaz]++;
        }
        for(int h=0;h<households.length;h++)
            householdsByTaz[households[h].homeTaz]++;

        for(int i=0;i<messages.length;i++){
            Message msg = mFactory.createMessage();
            msg.setId(MessageID.UPDATE_TAZDATA);
            msg.setValue("householdsByTaz",householdsByTaz);
            msg.setValue("postSecOccup",postSecOccup);
            msg.setValue("otherSchoolOccup",otherSchoolOccup);
            messages[i]=msg;
        }
        return messages;
    }

    private Message[] createDCLogsumWorkMessages(MessageFactory mFactory){
        //The number of messages will be 45 but the total number of logsums written
        //will be 63 because purpose 'c' (school) has 3 subpurposes which will be written.
        Message[] messages =  new Message[(Ref.MC_PURPOSES.length() - 1) *Ref.TOTAL_SEGMENTS];
        //Worker is asked to calculate the Destination Choice logsum for a particular
        //purpose/segment pair.  Total number of messages is (nPurposes-1)*nSegments
        //because we are not doing the work purpose.
        for (int purpose = 1; purpose < Ref.MC_PURPOSES.length(); ++purpose) {
            for (int segment = 0; segment < Ref.TOTAL_SEGMENTS; ++segment) {
                Message dcLogsumMessage = mFactory.createMessage();
                dcLogsumMessage.setId(MessageID.CREATE_DC_LOGSUMS);
                dcLogsumMessage.setValue("purpose", new Character(Ref.MC_PURPOSES.charAt(purpose)));
                dcLogsumMessage.setValue("segment", new Integer(segment));
                messages[((purpose-1)*Ref.TOTAL_SEGMENTS) + segment] = dcLogsumMessage;
            }
        }
        return messages;

    }

    private Message[] createHHWorkMessages(MessageFactory mFactory, int nWorkQueues){
        Arrays.sort(households); //first sort by workLogsumSegment, nonWorkLogsumSegment
        int nMsgsPerQueue = 20;
        int blockSize = Math.min(Ref.MAX_BLOCK_SIZE, households.length); //might have less than 5000 households
        int hhCount = 0;
        Message[] messages = new Message[nMsgsPerQueue * nWorkQueues];
        for(int m=0; m<(nMsgsPerQueue * nWorkQueues); m++){
            for(int i=0; i<blockSize; i++){
        //TODO: think about how you want to set this up without assuming that you have 500,000 households right off the bat.
            }
        }
        return messages;
    }

    private void sendWorkToWorkQueues(Message[] msgs, Port[] WorkPorts, int nWorkQueues){
        long sendTime = System.currentTimeMillis();
        for(int m=0;m<msgs.length;m++){
            //get a message from the array
            Message msg = msgs[m];
            int count = m+1;

            //Send the message
            logger.info( getName() + " sent " + msg.getId() + " to " + WorkPorts[(count%nWorkQueues)].getName());
            WorkPorts[(count%nWorkQueues)].send(msg); //will cycle through the ports
                                                        //till all messages are sent
        }
        logger.info("All messages have been sent.  Time in secs: "+ (System.currentTimeMillis()-sendTime)/1000.0);
    }

    public static void signalResultsProcessed() {
        signal.flipValue();
    }

}
