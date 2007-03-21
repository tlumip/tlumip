#mrGUI_Model_Control
import wx,os,subprocess,time
from threading import Thread
import mrGUI_Status_Logger as Status
import mrGUI_Modules as WinMod
import mrGUI_Logging as Logging
import mrGUI_AOServer_Communication as AOS
import mrGUI_Globals as Globs

def modelControl(Mip):
  global mip
  mip = Mip
  #Start button and green "light"
  mip.greenLightWindow = wx.Window(mip, -1, wx.Point(675,367), wx.Size(25,25), style=wx.SUNKEN_BORDER)
  mip.defaultGrey = mip.greenLightWindow.GetBackgroundColour()
  mip.startButton = wx.Button(mip, 500, "Start", wx.Point(713, 360), wx.Size(140,40))
  mip.startButton.SetFont(mip.basicBoxFont)
  wx.EVT_BUTTON(mip, 500, startRun)
  
  #Stop button and red "light"
  mip.redLightWindow = wx.Window(mip, -1, wx.Point(675,417), wx.Size(25,25), style=wx.SUNKEN_BORDER)
  mip.redLightWindow.SetBackgroundColour(wx.RED)
  mip.stopButton = wx.Button(mip, 510, "Stop", wx.Point(713, 410), wx.Size(140,40))
  mip.stopButton.SetFont(mip.basicBoxFont)
  mip.stopButton.Enable(False)
  wx.EVT_BUTTON(mip, 510, stopRun)
  
  AOS.ensureGlobalMIP(mip)
  
  
def startRun(event):
  global mip
  mip.mlp.showSelf(event)
  mip.mlp.saveButton.Enable(False)
  mip.mlp.clearButton.Enable(False)
  Logging.killExternalLoggerWindow('main')
  if os.path.isfile(Globs.mainLogFile.replace('@SCENARIO@',mip.state['scenarioName'])):
    #This loop is because java sometimes hasn't released control of the log file when python tries to delete it
    loopCheck = True
    while loopCheck:
      try:
        os.remove(Globs.mainLogFile.replace('@SCENARIO@',mip.state['scenarioName']))
        loopCheck = False
      except:
        continue
  Logging.startExternalLoggerWindow('main','7001',Globs.mainLogFile.replace('@SCENARIO_NAME@',mip.state['scenarioName']),'8001')
  mip.greenLightWindow.SetBackgroundColour(wx.GREEN)
  mip.redLightWindow.SetBackgroundColour(mip.defaultGrey)
  mip.Refresh()
  startSimulation()
  mip.startButton.Enable(False)
  mip.stopButton.Enable(True)

def startSimulation():
  global mip
  mip.mainRunThread = Thread(target=lambda:startSimulationThread())
  mip.mainRunThread.start()
  checkerThread = Thread(target=lambda:mainThreadChecker())
  checkerThread.start()

def mainThreadChecker():
  global mip
  while mip.mainRunThread.isAlive():
    time.sleep(4)
  mip.greenLightWindow.SetBackgroundColour(mip.defaultGrey)
  mip.redLightWindow.SetBackgroundColour(wx.RED)
  mip.stopButton.Enable(False)
  mip.startButton.Enable(True)
  mip.mlp.clearButton.Enable(True)
  mip.mlp.saveButton.Enable(True)
  mip.Refresh()

def stopRun(event):
  global mip
  if mip.remoteProcessRunning:
    killReturn = mip.appOrchestratorServer.killApplication()
    if killReturn == 0:
      mip.stopRunning = True
      mip.stopButton.Enable(False)
    else:
      Status.write('No process to stop!\n')
  else:
    Status.write('No process to stop!\n')

def startSimulationThread():
  global mip,createScenarioReturnValue
  mip.remoteProcessRunning = False
  ##Checks for midway finished version of mip
  #notAllowedList = ['PT','CT','TS']
  #if mip.dafBoxEnabled:
  #  Status.write("DAF runs not allowed at the moment.\n")
  #  return
  if int(mip.state['simulationYears']) > 1:
    Status.write("Only one simulation year allowed at the moment.\n")
    return
  #for name in notAllowedList:
  #  if mip.state['nonDAFChecks'].has_key(name):
  #    if mip.state['nonDAFChecks'][name] == 1:
  #      Status.write(name + " runs not allowed at the moment.\n")
  #      return
  #  if mip.state['dafChecks'].has_key(name):
  #    if mip.state['dafChecks'][name] == 1:
  #      Status.write(name + " runs not allowed at the moment.\n")
  #      return
  #Check computer(s) status
  computerCheck = AOS.checkRemoteComputerStatus(False)
  for name in computerCheck:
    computerStatus = computerCheck[name].split(":")
    if not ((computerStatus[0] == "NOT_BUSY") and (len(computerStatus) == 1)):
      Status.write("One or more computers busy! Here is the computer(s) status:\n")
      AOS.printRemoteComputerStatus(computerCheck)
      return
  mip.stopRunning = False
  mip.keepLoggerAlive = True
  logThread = Thread(target=lambda:Logging.startPySocketServer(8001,'main'))
  logThread.start()
  mip.antOutText = ''
  Logging.checkAntLog()
  if not mip.connectedToAOS:
    mip.appOrchestratorServer = AOS.connectToApplicationOrchestratorServer(mip.config['computerIPList'][mip.config['baseComputer']])
  if not mip.resultServerStarted:
    AOS.startResultServer()
  retval = mip.appOrchestratorServer.setModelRunnerIpAndPort(mip.config['ipAddress'], Globs.aoXMLRPCResultPort)
  if mip.state['createScenario'] == 1:
    mip.stopButton.Enable(False)
    createScenarioThread = Thread(target=lambda:generateScenario())
    createScenarioThread.start()
    Status.write("Starting scenario creation...")
    createScenarioReturnValue = -99
    while createScenarioThread.isAlive():
      time.sleep(2)
    if createScenarioReturnValue == -99:
      Status.write('\nUnknown create scenario error!  Please try again.')
      return
    if createScenarioReturnValue == 0:
      Status.write("finished.\n")
    else:
      Status.write("failed!\n")
    mip.stopButton.Enable(True)
  #if in daf mode, start all file monitors and prepare daf
  WinMod.checkForDaf()
  if mip.dafBoxEnabled:
    nodeLevel = 0
    for name in mip.config['computerNamesOrd']:
      if mip.state['computerChecks'][name] == 1:
        AOS.startFileMonitor(name,mip.state['scenarioName'],nodeLevel)
        print name
        nodeLevel = nodeLevel + 1
    print "TODO: setup all the daf files and such"
    Status.write(mip.appOrchestratorServer.setupDAF(mip.state))
  else:
    AOS.setBusy(mip.config['baseComputer'],mip.state['scenarioName'])
  #try running the "canned" multi module runs if checked
  if mip.state['spatialCheck'] == 1:
    Status.write('Starting spatial run...\n')
    retval = mip.appOrchestratorServer.runSpatialModules('rootDir', mip.state['scenarioName'], mip.state['baseYear'], mip.state['simulationYears'])
    mip.remoteProcessRunning = True
    while mip.remoteProcessRunning:
      time.sleep(2)
    if mip.stopRunning:
      Status.write('Spatial run stopped by user.\n')
    else:
      Status.write('Spatial run finished.\n')
    mip.antOutText = mip.antOutText + '\n\n' + Logging.cleanAntFile(Globs.antTextFile)
  elif mip.state['transportCheck'] == 1:
    Status.write('Starting transport run...\n')
    retval = mip.appOrchestratorServer.runTransportModules('rootDir', mip.state['scenarioName'], mip.state['baseYear'], mip.state['simulationYears'])
    mip.remoteProcessRunning = True
    while mip.remoteProcessRunning:
      time.sleep(2)
    if mip.stopRunning:
      Status.write('Transport run stopped by user.\n')
    else:
      Status.write('Transport run finished.\n')
    mip.antOutText = mip.antOutText + '\n\n' + Logging.cleanAntFile(Globs.antTextFile)
  elif mip.state['allCheck'] == 1:
    Status.write('Starting all modules run...\n')
    retval = mip.appOrchestratorServer.runAllModules('rootDir', mip.state['scenarioName'], mip.state['baseYear'], mip.state['simulationYears'])
    mip.remoteProcessRunning = True
    while mip.remoteProcessRunning:
      time.sleep(2)
    if mip.stopRunning:
      Status.write('All modules run stopped by user.\n')
    else:
      Status.write('All modules run finished.\n')
    mip.antOutText = mip.antOutText + '\n\n' + Logging.cleanAntFile(Globs.antTextFile)
  else:
    #if not a "canned" multi-module run, then run each module individually
    for name in mip.config['modulesOrd']:
     if mip.stopRunning:
       mip.keepLoggerAlive = False
       break
     #non-daf/daf run checked for this module
     nonDafCheck = (mip.state['nonDAFChecks'].has_key(name)) and (mip.state['nonDAFChecks'][name] == 1)
     dafCheck = (mip.state['dafChecks'].has_key(name)) and (mip.state['dafChecks'][name] == 1)
     if nonDafCheck | dafCheck:
       printName = name
       if nonDafCheck:
         Status.write('Starting ' + printName + ' run...\n')
         retval = mip.appOrchestratorServer.runNonDafModule(name.lower(), 'rootDir', mip.state['scenarioName'], mip.state['baseYear'], mip.state['simulationYears'])
       else:
         printName = name + ' (DAF)'
         Status.write('Starting ' + name + ' run...\n')
         retval = mip.appOrchestratorServer.runDafModule(name.lower(), 'rootDir', mip.state['scenarioName'], mip.state['baseYear'], mip.state['simulationYears'])
       mip.remoteProcessRunning = True
       while mip.remoteProcessRunning:
         time.sleep(2)
       if mip.stopRunning:
         Status.write(printName + ' run stopped by user.\n')
       else:
         Status.write(printName + ' run finished.\n')
       mip.antOutText = mip.antOutText + '\n\n' + Logging.cleanAntFile(Globs.antTextFile)
   #older version of the daf code
   #if mip.state['nonDAFChecks'].has_key(name):
   #  if mip.state['nonDAFChecks'][name] == 1:
   #    Status.write('Starting ' + name + ' run...\n')
   #    retval = mip.appOrchestratorServer.runNonDafModule(name.lower(), 'rootDir', mip.state['scenarioName'], mip.state['baseYear'], mip.state['simulationYears'])
   #    mip.remoteProcessRunning = True
   #    while mip.remoteProcessRunning:
   #      time.sleep(2)
   #    if mip.stopRunning:
   #      Status.write(name + ' run stopped by user.\n')
   #    else:
   #      Status.write(name + ' run finished.\n')
   #    mip.antOutText = mip.antOutText + '\n\n' + Logging.cleanAntFile(Globs.antTextFile)
   #if mip.state['dafChecks'].has_key(name):
   #  if mip.state['dafChecks'][name] == 1:
   #    Status.write('Starting ' + name + ' (DAF) run...\n')
   #    retval = mip.appOrchestratorServer.runDafModule(name.lower(), 'rootDir', mip.state['scenarioName'], mip.state['baseYear'], mip.state['simulationYears'])
   #    mip.remoteProcessRunning = True
   #    while mip.remoteProcessRunning:
   #      time.sleep(2)
   #    if mip.stopRunning:
   #      Status.write(name + ' (DAF) run stopped by user.\n')
   #    else:
   #      Status.write(name + ' (DAF) run finished.\n')
   #    mip.antOutText = mip.antOutText + '\n\n' + Logging.cleanAntFile(Globs.antTextFile)
  mip.appOrchestratorServer.setNotBusy()
  AOS.disconnectFromApplicationOrchestratorServer(mip.appOrchestratorServer)
  ##kill all file monitors
  for name in mip.config['computerNamesOrd']:
    if mip.state['computerChecks'][name] == 1:
      AOS.stopFileMonitor(name)
  ##wait for logs to come over the network
  mip.stopButton.Enable(False)
  mip.redLightWindow.SetBackgroundColour(wx.BLUE)
  mip.greenLightWindow.SetBackgroundColour(wx.BLUE)
  mip.Refresh()
  Status.write('Gathering remaining logs.')
  for i in range(10):
    time.sleep(1)
    Status.write('.')
  else:
    Status.write('\n')
  mip.keepLoggerAlive = False
  Logging.checkAntLog()
  if mip.antOutText <> '':
    mip.antOutText = '[Note: Java INFO logs and extra lines have been removed.]\n' + mip.antOutText
    #Logging.checkAntLog()
    if mip.antOutText.find('BUILD FAILED') <> -1:
      Status.write('Model run failed! Check ANT and java logs for details.\n')
      
def generateScenario():
  global mip,createScenarioReturnValue
  createScenarioReturnValue = mip.appOrchestratorServer.createScenario(mip.state['scenarioName'], mip.state['scenarioYears'], mip.state['baseYear'])
