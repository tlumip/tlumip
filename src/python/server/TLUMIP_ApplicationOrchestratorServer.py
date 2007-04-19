#!python
"""
ApplicationOrchestratorServer.py

        This class acts as a gateway between a user and a cluster of machines.
        It will handle tasks such as creating a new scenario on the cluster,
        running model components, checking on status of model runs, and
        running sql queries on output data

"""
import sys, os, GetTrueIP, subprocess, csv, glob, string, re, time, pprint
from StringIO import StringIO
from xmlrpclib import ServerProxy as ServerConnection
from RequestServer import RequestServer
from CommandExecutionDaemon import CommandExecutionDaemonServerXMLRPCPort
#import TargetRules, types

""" Global Variables """
ApplicationOrchestratorServerXMLRPCPort = 8942
pythonScriptDirectory = r"Z:\models"
scenarioDirectory = r"Z:\models\tlumip\scenario_"
createdScenariosFile = "CreatedScenarios.csv"  ####### Create full path for this
runtimeDirectory = r"Z:\models\tlumip\runtime" + '\\'

def sendRemoteCommand(machine, command):
    remoteDaemon = ServerConnection("http://" + machineIP[machine] + ":" + str(CommandExecutionDaemonServerXMLRPCPort))
    result = remoteDaemon.checkConnection()
    print "Checking connection to CommandExecutionDaemonServer:", result

    print "sendRemoteCommand: %s, %s" % (machine, command)
    result = remoteDaemon.runRemoteCommand(command)
    print "result: %s" % str(result)
    return result

class ApplicationOrchestratorServer(RequestServer):
    """
    Handle xml-rpc requests
    """
    def __init__(self, ip):
        RequestServer.__init__(self, ip, port = ApplicationOrchestratorServerXMLRPCPort)
        self.scenarioNameP = ''
        self.readClusterMachinesFile()
        
    def checkConnection(self):
        """
        For sanity checking
        """
        return "Connection to ApplicationOrchestratorServer OK"

    def terminate(self):
        """
        For restarting by AOServerRunner.py
        A hard stop (might be a better way to do this)
        """
        os.abort()

    def longTestCall(self):
        """
        Testing colliding calls
        """
        print "Starting long test call"
        time.sleep(7)
        print "Completed long test call"
        return "longTestCall completed"

    def tempCreateScenario(self, scenarioName, numYears, baseYear, userName, description):
        try:
            writer = csv.writer(file(createdScenariosFile, "ab+"))
            writer.writerow([scenarioName, userName, time.asctime(), numYears, baseYear, description])
        except Exception, val:
            return "SERVER ERROR: Temp Scenario Creator threw exception " + str(val)

        return "scenario %s created." % scenarioName

    def createScenario(self, scenarioName, numYears, baseYear, userName, description):
        if not os.path.exists(createdScenariosFile):
            writer = csv.writer(file(createdScenariosFile, "wb"))
            writer.writerow("scenarioName userName scenarioCreationTime scenarioYears baseYear scenarioDescription".split())
            writer = None
        try:
            dirCreatorRetval = subprocess.call(["python", os.path.normpath(os.path.join(pythonScriptDirectory, "TLUMIP_ScenarioDirectoryCreator.py")), scenarioName, numYears, baseYear])
        except Exception, val:
            return "ERROR: Directory creator threw exception " + str(val)
        if dirCreatorRetval == 0:
            try:
                fileCopierRetval = subprocess.call(["python", os.path.normpath(os.path.join(pythonScriptDirectory, "TLUMIP_ScenarioFileCopier.py")), scenarioName, baseYear])
                if fileCopierRetval == 0:
                    writer = csv.writer(file(createdScenariosFile, "ab+"))
                    writer.writerow([scenarioName, userName, time.asctime(), numYears, baseYear, description])
                    return 0
                else:
                    return "ERROR: File Copier returned %d" % fileCopierRetval
            except Exception, val:
                return "ERROR: File Copier threw exception " + str(val)
        else:
            return "ERROR: Directory Creator returned %d" % dirCreatorRetval

    def isScenarioReady(self, scenarioName):
        try:
            reader = csv.reader(open(createdScenariosFile, "rb"))
            for row in reader:
                if row[0] == scenarioName:
                    return "Scenario Ready"
        except Exception, val:
            return ("ERROR: exception thrown when reading %s " % createdScenariosFile) + str(val)
        return "Scenario Not Ready"

    def readClusterMachinesFile(self):
        # Map of machine names to IP addresses:
        global machineIP
        machineIP = {}
        machineProperties = []
        serverMachine = None
        clusterMachines = file("ClusterMachines.txt")
        header = clusterMachines.next().split()
        
        for machine in clusterMachines:
            vars, DESCRIPTION, trash = machine.split('"')
            NAME, IP, PROCESSORS, RAM, OS = vars.split()
            if not serverMachine: # First name in ClusterMachines.txt is defined as server
                serverMachine = NAME
            machineIP[NAME] = IP
            machineProperties.append( {
                "NAME" : NAME,
                "IP" : IP,
                "PROCESSORS" : PROCESSORS,
                "RAM" : RAM,
                "OS" : OS,
                "DESCRIPTION" : DESCRIPTION,
                "STATUS" : "Unreachable"
            })
        #pprint.pprint(machineProperties)
        return machineProperties

    def getAvailableMachines(self):
        """
        TODO:
        Return a list of all the machines:
        machineName, status
        status code:
            Unreachable
            Busy
            Available

        Create a list/table of what processes a client started
        Create a file that shows activity history

        Go to each machine in the cluster and find which machine has no "essential"
        process running. (call getProcessList() on each machine) First cut: if java
        is running, machine is not available
        """
        machineProperties = self.readClusterMachinesFile()
        for machine in machineProperties:
            #if machine not in ["Athena", "Chaos"] : continue  ############## TEST TEST TEST
            cmdDaemon = "http://" + machine['IP'] + ":" + str(CommandExecutionDaemonServerXMLRPCPort)
            try:
                cmdDaemon = ServerConnection(cmdDaemon)
                processList = cmdDaemon.getProcessList()
                if "java" not in processList:
                    machine['STATUS'] = "Available"
                else:
                    machine['STATUS'] = "Busy"
            except Exception, e:
                machine['STATUS'] = "Unreachable"
        return machineProperties

    def getAvailableAntTargets(self):
        """
        Return as list: name, description, list of arguments.
        Description contains embedded newlines for formatting.
        """
        data = file(os.path.join(runtimeDirectory, "tlumip.xml")).read()
        echo = data.split('<target name="echo">')[1].split('</target>')[0]
        message = re.compile('<echo message="(.*?)"/>', re.DOTALL)
        messages = [m for m in map(string.strip, message.findall(echo)) if m.startswith("run")]
        result = []
        for entry in messages:
            print entry
            name, description, arguments = entry.split("'")
            name = name.split(',')[0].strip()
            description = (" ".join(description.replace("\n", "##n").split())).replace("##n", "\n")
            description = description.replace("\n ", "\n").strip()
            arguments = [t for t in map(string.strip, arguments.split(',')) if t]
            result.append([name, description, arguments])
        return result

    def startModelRun(self, target, scenario, baseScenario, baseYear, interval, machineList):
        """
        Pass empty strings for non-used extra arguments
        test:
          run ed
          run pydaf
          "ant -f targetname"
        """
        #print "args", target, scenario, baseScenario, baseYear, interval, machineList
        if target == "runVersions":
            resultList = []
            for m in machineList:
                result = sendRemoteCommand(m, ["ant", "-f", r"%stlumip.xml" % runtimeDirectory, "runVersions"])
                
                # a valid result is an int pid, anything else is an exception message, so return it
                try:
                    int(result)
                except ValueError:
                    return '%s EXCEPTION: %s' (m, result)
                    
                resultList.append((m, result))
            return resultList
        if target == "specialCommand":
            sendRemoteCommand("Athena", ["ping", "www.google.com"])
            return "Special Command Sent"
        if target == "testFileCommand":
            path = "%stlumip.xml" % (runtimeDirectory)
            sendRemoteCommand("Athena", ["type", path])
            return "testFileCommand Sent"
        print "=*=" * 80
        print "len(machineList):", len(machineList)
        if len(machineList) > 1:
            """
            There's an ant target called startfilemonitor on each
            machine in the machine list
            -- Also startbootstrapserver
            -- Create daf property file
            """
            createDAFPropertiesFile(scenario, machineList)
            # Send commands to every machine in the list:
            for i, machine in enumerate(machineList):
                command1 = (r"ant -f %stlumip.xml startFileMonitor -DscenarioName=%s -Dnode=%d" %
                           (runtimeDirectory, scenario, i)).split()
                sendRemoteCommand(machine, command1)
                command2 = (r"ant -f %stlumip.xml startBootstrapServer -DscenarioName=%s -DmachineName=%s" %
                           (runtimeDirectory, scenario, machine)).split()
                sendRemoteCommand(machine, command2)

        # Call ant target, or special target if it exists
        # executeRule(target, scenario, baseScenario, baseYear, interval)
        dlist = []
        if scenario:
            dlist = [ "-DscenarioName=%s" % scenario ]
        if baseScenario:
            dlist.append("-DbaseScenario=%s" % baseScenario)
        if baseYear:
            dlist.append("-DbaseYear=%s" % baseYear)
        if interval:
            dlist.append("-Dt=%s" % interval)
        command3 = (r"ant -f %stlumip.xml %s" % (runtimeDirectory, target)).split()
        command3 = command3 + dlist
        print "command3:", command3
        # If serverMachine is in the list, send to serverMachine, otherwise send to first machine in list
        if serverMachine in machineList:
            sendRemoteCommand(serverMachine, command3)
        else:
            sendRemoteCommand(machineList[0], command3)
        print "Started: " + " ".join(command3)
        return "Started: " + " ".join(command3)

    def verifyModelIsRunning(self, scenario):
        """
        Did the thing get going?
        How do we know?
        """
        return "unimplemented"

    def getStatus(self, scenario, arg2, arg3):
        """
        Still not entirely sure what this means.
        """
        return "unimplemented"

    def checkForTerminationCondition(self, scenario):
        """
        Still not entirely sure what this means.
        """
        return "unimplemented"

    def getLogFileNames(self, scenarioName):
        log_dir = scenarioDirectory + scenarioName + os.sep + "logs"
        return map(os.path.basename, glob.glob(log_dir + os.sep + "*.log"))

    def getLogData(self, scenarioName, logFileName):
        log_dir = scenarioDirectory + scenarioName + os.sep + "logs"
        return file(log_dir + os.sep + logFileName).read()

    def storeClientState(self, scenario, stateInfo):
        """
        Client uses this to store any state information so that when the client
        goes away and comes back, can restore itself exactly as it was before.
        """
        return "unimplemented"

    def retrieveClientState(self, scenario):
        """
        Client uses this to store any state information so that when the client
        goes away and comes back, can restore itself exactly as it was before.
        """
        return "unimplemented"

    def showAvailableScenarios(self):
        """
        Discover the scenario state information that is stored on the server.
        """
        return "unimplemented"

    def getExistingScenarioProperties(self):
        """
        Get the properties for scenarios that already exist on the server.
        """

        allProps = {}
        keyFieldName = 'scenarioName'

        try:
            reader = csv.reader(open(createdScenariosFile, "rb"))
            for i, row in enumerate(reader):
                if i == 0:
                    headerNames = row

                    # get the field index for keyFieldName
                    for k, name in enumerate(headerNames):
                        if name.lower() == keyFieldName.lower():
                            keyIndex = k
                            break
                else:
                    scenProps = {}
                    key = row[keyIndex]

                    for j, value in enumerate(row):
                        if j != keyIndex:
                            name = headerNames[j]
                            scenProps[name] = value

                    allProps[key] = scenProps

        except Exception, val:
            return ("SERVER ERROR: exception thrown when reading %s " % createdScenariosFile) + str(val)

        return allProps

    def getScenarioProperties_Bruce(self, scenario):
        """
        Return as list
        """
        try:
            reader = csv.reader(open(createdScenariosFile, "rb"))
            for row in reader:
                if row[0] == scenario:
                    return row
        except Exception, val:
            return ("ERROR: exception thrown when reading %s " % createdScenariosFile) + str(val)
        return "ERROR: Scenario not created"


############# Private functions, not part of the server class #####################

def createDAFPropertiesFile(scenario, machineNames):
    """
    This is a 'private' function that will be called prior to a daf run
    that will create the daf.properties file.
    """
    print "scenario", scenario
    print "machineNames", machineNames
    filePath = os.path.normpath(scenarioDirectory + scenario)
    filePath = os.path.join(filePath, "daf")
    templateFilePath = os.path.join(filePath, "daf_TEMPLATE.properties")
    print "templateFilePath", templateFilePath
    newDaf = file(templateFilePath).read().replace("@NODE_LIST@",
         ",".join(["node%d" % i for i in range(len(machineNames))]))
    if serverMachine in machineNames:
        # Force it to be the first name in the list:
        machineNames.remove(serverMachine)
        machineNames.insert(0, serverMachine)

    print "machineNames:", machineNames
    for i, name in enumerate(machineNames):
        tag1 = "@NODE_%d_ADDRESS@" % (i)
        tag2 = "@NODE_%d_NAME@" % (i)
        newDaf = newDaf.replace(tag1, machineIP[name])
        newDaf = newDaf.replace(tag2, name)
    print "newDaf:", newDaf

    propertyFilePath = os.path.join(filePath, "daf.properties")
    print "propertyFilePath:", propertyFilePath
    file(propertyFilePath, 'w').write(newDaf)


###################################################

if __name__ == "__main__":
    ipAddress = GetTrueIP.trueIP()
    print "ApplicationOrchestratorServer running"
    ApplicationOrchestratorServer(ipAddress)

