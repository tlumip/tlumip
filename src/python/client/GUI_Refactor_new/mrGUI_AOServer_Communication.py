#mrGUI_AOServer_Communication.py
import os
from xmlrpclib import ServerProxy as ServerConnection
from threading import Thread
import mrGUI_Globals as Globs
import mrGUI_Status_Logger as Status
from RequestServer import RequestServer

def ensureGlobalMIP(Mip):
  global mip
  mip = Mip

def initAOServerStuff():
  global mip
  mip.connectedToAOS = False
  mip.resultServerStarted = False

def connectToApplicationOrchestratorServer(ipAdd):
  global mip
  appOrchestratorServer = ServerConnection('http://' + ipAdd + ':' + str(Globs.aoXMLRPCSendPort))
  mip.connectedToAOS = True
  return appOrchestratorServer
  
def disconnectFromApplicationOrchestratorServer(aoServer):
  del aoServer
  mip.connectedToAOS = False

class ResultServer(RequestServer):
  def __init__(self, ip, Mip):
    RequestServer.__init__(self, ip, port = Globs.aoXMLRPCResultPort)
    self.mip = Mip
  def status(self, arg):
    #statusWindow.insert(END, str(arg) + "\n")
    return "Model Runner received " + str(arg)
  def simulationComplete(self, arg):
    self.mip.remoteProcessRunning = False
    #statusWindow.insert(END, "Simulation complete: " + str(arg) + "\n")
    antFile = file(Globs.antTextFile, 'w')
    antFile.write(arg)
    antFile.close()
    return "Model Runner received " + str(arg)
  
def startResultServer():
  global mip
  mip.serverThread = Thread(target=lambda:ResultServer(mip.config['ipAddress'],mip))
  mip.serverThread.setDaemon(True)
  mip.serverThread.start()
  mip.resultServerStarted = True


def checkRemoteComputerStatus(all):
  global mip
  computerCheck = {}
  for name in mip.config['computerIPList']:
    if not all:
      if name <> mip.config['baseComputer']:
        if mip.dafBoxEnabled:
          if mip.state['computerChecks'][name] == 0:
            continue
        else:
          continue
    computerCheck[name] = ""
  for name in computerCheck:
    try:
      aoCheckServer = connectToApplicationOrchestratorServer(mip.config['computerIPList'][name])
      computerCheck[name] = aoCheckServer.checkStatus()
    except:
      computerCheck[name] = "REFUSED"
    if mip.connectedToAOS:
      disconnectFromApplicationOrchestratorServer(aoCheckServer)
  return computerCheck

def printRemoteComputerStatus(computerCheck):
  global mip
  Status.write("\n****Remote Computers' Status****\n")
  for name in computerCheck:
    computerCheckResult = computerCheck[name].split(":")
    if computerCheckResult[0] == "NOT_BUSY":
      Status.write(name + ":" + " Not busy.")
      if len(computerCheckResult) > 1:
        Status.write(" (However: " + computerCheckResult[1] + ")")
      Status.write("\n")
    elif computerCheckResult[0] == "scenario":
      Status.write(name + ":" + " In use!\n    Scenario: " + computerCheckResult[1] + "\n    User: " + computerCheckResult[2] + " (" + computerCheckResult[3] + ")\n")
    elif computerCheckResult[0] == "REFUSED":
      #gui.statusWindowWrite(name + ": Connection refused! (ApplicationOrchestratorServer may not be running.)\n")
      Status.write(name + ": Connection refused!\n")
    else:
      Status.write(name + ": Unknown result!\n")
  Status.write("*****************************\n")

def setBusy(computerName,scenarioName):
  global mip
  try:
    aoBusy = connectToApplicationOrchestratorServer(mip.config['computerIPList'][computerName])
    aoBusy.setBusy(scenarioName,os.environ['USERNAME'],mip.config['ipAddress'])
  except:
    print "Connection error in setting computer busy!"
  disconnectFromApplicationOrchestratorServer(aoBusy)

def setNotBusy(computerName):
  global mip
  try:
    aoBusy = connectToApplicationOrchestratorServer(mip.config['computerIPList'][computerName])
    aoBusy.setNotBusy()
  except:
    print "Connection error in setting computer not busy!"
  disconnectFromApplicationOrchestratorServer(aoBusy)

def startFileMonitor(computerName,scenarioName,nodeNumber):
  global mip
  #aoFileWatcher = connectToApplicationOrchestratorServer(mip.config['computerIPList'][computerName])
  #aoFileWatcher.startFileWatcher(nodeNumber,scenarioName)
  #aoFileWatcher.setBusy(scenarioName,os.environ['USERNAME'],mip.config['ipAddress'])
  try:
    aoFileWatcher = connectToApplicationOrchestratorServer(mip.config['computerIPList'][computerName])
    aoFileWatcher.startFileWatcher(nodeNumber,scenarioName)
    aoFileWatcher.setBusy(scenarioName,os.environ['USERNAME'],mip.config['ipAddress'])
  except:
    print "Connection error!"
  disconnectFromApplicationOrchestratorServer(aoFileWatcher)

def stopFileMonitor(computerName):
  global computerNameIP
  try:
    aoFileWatcher = connectToApplicationOrchestratorServer(mip.config['computerIPList'][computerName])
    aoFileWatcher.stopFileWatcher()
    aoFileWatcher.setNotBusy()
  except:
    print "Connection error!"
  disconnectFromApplicationOrchestratorServer(aoFileWatcher)