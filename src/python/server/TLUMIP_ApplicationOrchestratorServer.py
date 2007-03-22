#!python
"""
ApplicationOrchestratorServer.py

        This class acts as a gateway between a user and a cluster of machines.
        It will handle tasks such as creating a new scenario on the cluster,
        running model components, checking on status of model runs, and
        running sql queries on output data

"""
import sys, os, GetTrueIP, subprocess, csv, glob, string, re
from StringIO import StringIO
from xmlrpclib import ServerProxy as ServerConnection
import time
from RequestServer import RequestServer
from CommandExecutionDaemon import CommandExecutionDaemonServerXMLRPCPort

""" Global Variables """
ApplicationOrchestratorServerXMLRPCPort = 8942
pythonScriptDirectory = "/models"
scenarioDirectory = "/models/tlumip/scenario_"
createdScenariosFile = "CreatedScenarios.csv"  ####### Create full path for this
runtimeDirectory = "/models/tlumip/runtime"


class ApplicationOrchestratorServer(RequestServer):
    """
    Handle xml-rpc requests
    """
    def __init__(self, ip):
        RequestServer.__init__(self, ip, port = ApplicationOrchestratorServerXMLRPCPort)
        self.scenarioNameP = ''

    def checkConnection(self):
        """
        For sanity checking
        """
        return "Connection OK"

    def longTestCall(self):
        """
        Testing colliding calls
        """
        print "Starting long test call"
        time.sleep(7)
        print "Completed long test call"
        return "longTestCall completed"

    def createScenario(self, scenarioName, numYears, baseYear, userName, description):
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

    def getAvailableMachines(self):
        """
        Create a list/table of what processes a client started
        Create a file that shows activity history

        Go to each machine in the cluster and find which machine has no "essential"
        process running. (call getProcessList() on each machine) First cut: if java
        is running, machine is not available
        """
        availableMachines = []
        for machine in file("ClusterMachines.txt"):
            name, ip = machine.split()
            if name not in ["Athena", "Chaos"] : continue
            processList = str(ServerConnection("http://" + ip + ":" + str(CommandExecutionDaemonServerXMLRPCPort)).getProcessList())
            if "python" not in processList:
                availableMachines.append(name)
        return availableMachines

    def getAvailableAntTargets(self):
        """
        Return as list: name, description, list of arguments.
        Description contains embedded newlines for formatting.
        """
        data = file(r"c:\zShare\models\tlumip\runtime\tlumip_dev.xml").read()
        echo = data.split('<target name="echo">')[1].split('</target>')[0]
        message = re.compile('<echo message="(.*?)"/>', re.DOTALL)
        messages = [m for m in map(string.strip, message.findall(echo)) if m.startswith("run")]
        result = []
        for entry in messages:
            name, description, arguments = entry.split("'")
            name = name.split(',')[0].strip()
            description = (" ".join(description.replace("\n", "##n").split())).replace("##n", "\n")
            description = description.replace("\n ", "\n").strip()
            arguments = [t for t in map(string.strip, arguments.split(',')) if t]
            result.append([name, description, arguments])
        return result

    def getScenarioProperties(self, scenario):
        """
        Return as dict
        """
        return "unimplemented"

    def startModelRun(self, scenario, module, year, baseYear, machineList):
        """
        """
        return "unimplemented"

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

    def showAvailableScenarios(self):
        """
        Discover the scenario state information that is stored on the server.
        """
        return "unimplemented"

    def retrieveClientState(self, scenario):
        """
        Client uses this to store any state information so that when the client
        goes away and comes back, can restore itself exactly as it was before.
        """
        return "unimplemented"

############# Private functions, not part of the server class #####################

def createDAFPropertiesFile(scenario, machineNames):
    """
    This is a 'private' function that will be called prior to a daf run
    that will create the daf.properties file.
    """
    assert machineNames[0] == "Athena"
    filePath = scenarioDirectory + scenario + os.sep + "daf" + os.sep
    newDaf = file(filePath + "daf_TEMPLATE.properties").read().replace("@NODE_LIST@",
         ",".join(["node%d" % i for i in range(len(machineNames))]))
    machines = {}
    for m in map(string.split, file("ClusterMachines.txt").readlines()):
        machines[m[0]] = m[1]
    for i, name in enumerate(machineNames[1:]):
        tag = "@NODE_%d_ADDRESS@" % (i + 1)
        newDaf = newDaf.replace(tag, machines[name])
    file(filePath + "daf.properties", 'w').write(newDaf)


###################################################

if __name__ == "__main__":
    ipAddress = GetTrueIP.trueIP()
    print "ApplicationOrchestratorServer running"
    ApplicationOrchestratorServer(ipAddress).createDAFPropertiesFile(self, "A", ["athena", "chaos"])

