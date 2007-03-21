#!python
"""
ApplicationOrchestratorServer.py

    This class acts as a gateway between a user and a cluster of machines.
    It will handle tasks such as creating a new scenario on the cluster,
    running model components, checking on status of model runs, and
    running sql queries on output data

"""
import sys, os, GetTrueIP, subprocess, csv, glob
from xmlrpclib import ServerProxy as ServerConnection
import time
from RequestServer import RequestServer

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
    process running. (call getProcessList() on each machine) First cut: if ant
    is running, machine is not available

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

  ###################################################

import threading
global modelComponentThread
#determine if this is a windows box or not
windows = False
if 'OS' in os.environ:
  windows = "windows" in os.environ['OS'].lower()

#set working directory
if windows:
  os.chdir('Z:\\')

modelRunnerIpAddress = None
modelRunnerXmlrpcPort = None
modelRunner = None
modelComponentThread = None
modelComponentReturnValue = -1

  def processStatusChecker():
      if not modelComponentThread.isAlive():
         #time.sleep(15)
         #print modelRunner.simulationComplete("Process is stopped")
         stdOutText = file('Stdout.txt')
         antText = stdOutText.read()
         stdOutText.close()
         returnText =  modelRunner.simulationComplete(antText)
      else:
        threading.Timer(2.0, processStatusChecker).start()

    def convertRetValueToString(retVal):
        if(retVal == 0):
            return "SUCCEEDED"
        else:
           return "FAILED"


def runApplication(rootDir, scenarioName, appName, baseYear, t):
  global processID
  global isBusy
  isBusy = True
  try:
    global modelComponentReturnValue, modelRunnerIpAddress
    #targetName = "run" + appName.upper()
    targetName = "run" + appName
    scenArg = "-DscenarioName=" + scenarioName
    baseArg = "-DbaseYear=" + baseYear
    tArg = "-Dt=" + str(t)
    ipArg = "-DremoteServerIp=" + modelRunnerIpAddress
    if windows:
      antCallList = ["ant", "-f", os.path.normpath(os.path.join(runtimeDirectory, "tlumip.xml")),targetName, scenArg, baseArg, tArg, ipArg]
    else:
      antCallList = ["ant", "-f", "--noconfig", os.path.normpath(os.path.join(runtimeDirectory, "tlumip.xml")),targetName, scenArg, baseArg, tArg, ipArg]
    #antCallList = ["ant", "-f", os.path.normpath(os.path.join(runtimeDirectory, "osmp.xml")),'runLogServerTest', scenArg]
    print " ".join(antCallList)
#    modelComponentReturnValue = os.system(" ".join(antCallList))
    stdOutText = file('Stdout.txt','w')
    modelCommand = subprocess.Popen(" ".join(antCallList),shell=True,stdout=stdOutText,stderr=subprocess.STDOUT)
    #modelCommand = subprocess.Popen(" ".join(antCallList),stdout=stdOutText,stderr=subprocess.STDOUT)
    processID = modelCommand.pid
    modelCommand.wait()
    stdOutText.close()
    print "Application Return Value: " + str(modelComponentReturnValue)
  except:
    print 'Exception in runApplication!'
  processID = None
  isBusy = False

  isBusy = False
isBusyDAF = False
processID = None
fileWatcherPID = None
busyStatus = "NOT_BUSY"

  def killApplication(self):
    global processID,windows
    ###windows - uses taskill and kills all child processes spawned by ant
    if windows:
      if processID <> None:
        g = os.system('TASKKILL /F /T /PID ' + str(processID))
        print str(g)
        if g == 0:
          processID = None
          return 0
        else:
          print "Process could not be killed!"
          return 1
      else:
        print str(processID) + "No process running to kill!"
        return -1
      ###
    ##linux - the above method won't work, so it kills all processes with 'java' and the scenario name in their command
    else:
      if self.scenarioNameP <> '':
        tdOutText = file('Pidout.txt','w')
        PIDCheckCommand = subprocess.Popen('ps o pid,command',shell=True,stdout=tdOutText,stderr=subprocess.STDOUT)
        PIDCheckCommand.wait()
        tdOutText.close()
        #PIDText = tdOutText = file('Pidout.txt').read()
        for line in file('Pidout.txt'):
          if (line.find(self.scenarioNameP) <> -1) and (line.find('java') <> -1):
            #print line.split()[0].strip()
            os.kill(int(line.split()[0].strip()),9)
        self.scenarioNameP = ''
        return 0
        ###
      else:
        print str(processID) + "No process running to kill!"
        return -1

  def runSPG1(self, rootDir, scenarioName, baseYear, t ):
    global modelComponentThread
    modelComponentThread = Thread(target = lambda:runApplication(rootDir, scenarioName, 'spg1', baseYear, t))
    modelComponentThread.start()
    processStatusChecker()
    return "SPG1 has started"

  def runNonDafModule(self, module, rootDir, scenarioName, baseYear, t):
    return self.runModule(module, rootDir, scenarioName, baseYear, t, False)

  def runDafModule(self, module, rootDir, scenarioName, baseYear, t):
    return self.runModule(module, rootDir, scenarioName, baseYear, t, True)

  def runAllModules(self, rootDir, scenarioName, baseYear, t):
    return self.runModule('AllApplications', rootDir, scenarioName, baseYear, t, False)

  def runSpatialModules(self, rootDir, scenarioName, baseYear, t):
    return self.runModule('SpatialApplications', rootDir, scenarioName, baseYear, t, False)

  def runTransportModules(self, rootDir, scenarioName, baseYear, t):
    return self.runModule('TransportApplications', rootDir, scenarioName, baseYear, t, False)

  def runModule(self,module,rootDir,scenarioName,baseYear,t,daf):
    global modelComponentThread
    self.scenarioNameP = scenarioName
    if daf:
      modelComponentThread = Thread(target = lambda:runApplication(rootDir, scenarioName, module + 'DAF', baseYear, t))
    else:
      modelComponentThread = Thread(target = lambda:runApplication(rootDir, scenarioName, module, baseYear, t))
    modelComponentThread.start()
    processStatusChecker()
    return module.upper() + " has started..."

  def setModelRunnerIpAndPort(self, ipAddress, xmlrpcPort):
    global modelRunnerIpAddress, modelRunnerXmlrpcPort, modelRunner
    modelRunnerIpAddress = ipAddress
    modelRunnerXmlrpcPort = xmlrpcPort
    modelRunner = ServerConnection("http://" + ipAddress + ":" + str(xmlrpcPort))
    return "Remote IP and Port is set"

  #will eventually turn this into a dynamic daf setup, for the moment, copy from a specific scenario
  def setupDAF(self, guiState):
    os.system('copy z:\\models\\tlumip\\scenario_20070219_Run1Year\\daf\\*.* z:\\models\\tlumip\\scenario_' + guiState['scenarioName'] + '\\daf\\ /Y')
    os.system('copy z:\\models\\tlumip\\scenario_20070219_Run1Year\\code\\*.* z:\\models\\tlumip\\scenario_' + guiState['scenarioName'] + '\\code\\ /Y')
    os.system('copy z:\\models\\tlumip\\scenario_20070219_Run1Year\\t0\\globalTemplate.properties z:\\models\\tlumip\\scenario_' + guiState['scenarioName'] + '\\t0\\ /Y')
    os.system('copy z:\\models\\tlumip\\scenario_20070219_Run1Year\\t0\\pi\\piTemplate.properties z:\\models\\tlumip\\scenario_' + guiState['scenarioName'] + '\\t0\\pi\\ /Y')
    return "DAF Setup finished"

  def startFileWatcher(self,node,scenarioName):
    global isBusyDAF,fileWatcherPID,windows
    if not isBusyDAF:
      isBusyDAF = True
      #run ant command
      if windows:
        antCallList = ["ant", "-f", os.path.normpath(os.path.join(runtimeDirectory, "tlumip.xml")),"startFileMonitor", "-DscenarioName=" + scenarioName, "-Dnode=" + str(node)]
      else:
        antCallList = ["ant", "-f", "--noconfig", os.path.normpath(os.path.join(runtimeDirectory, "tlumip.xml")),"startFileMonitor", "-DscenarioName=" + scenarioName, "-Dnode=" + str(node)]
      fwCommand = subprocess.Popen(" ".join(antCallList),shell=True)
      fileWatcherPID = fwCommand.pid
    return "Started file watcher"

  def stopFileWatcher(self):
    global isBusyDAF
    if isBusyDAF:
      g = os.system('TASKKILL /F /T /PID ' + str(fileWatcherPID))
      if g == 0:
        isBusyDAF = False
        fileWatcherPID = None
    return "Stopped file watcher"

  def setBusy(self,scenario,user,ip):
    global busyStatus
    busyStatus = "scenario:" + scenario + ":" + user + ":" + ip
    return "Busy Set"

  def setNotBusy(self):
    global busyStatus
    busyStatus = "NOT_BUSY"
    return "Not Busy Set"

  def checkStatus(self):
    global isBusy,isBusyDAF,busyStatus
    if busyStatus == "NOT_BUSY":
      if isBusy:
        busyStatus = busyStatus + ":A hanging application running via Application Orchestrator Server!"
      elif isBusyDAF:
        busyStatus = busyStatus + ":A hanging file watcher running via Application Orchestrator Server!"
    return busyStatus

if __name__ == "__main__":
  ipAddress = GetTrueIP.trueIP()
  print "Test is " + str(test)
  if test:
    ipAddress = "localhost"
    print "ApplicationOrchestratorServer in test mode"
    runApplication("/models/osmp", "test", "spg1", "2000", 1)

  ApplicationOrchestratorServer(ipAddress)
