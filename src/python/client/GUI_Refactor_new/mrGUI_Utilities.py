#mrGUI_Utilities.py
import wx,os,subprocess,time
from threading import Thread
import mrGUI_Basic_Inputs as WinBI
import mrGUI_Modules as WinMod
import mrGUI_DAF as WinDaf
import mrGUI_Status_Logger as Status
import mrGUI_Configuration as Config
import mrGUI_AOServer_Communication as AOS
import mrGUI_Globals as Globs

def utilitiesPanel(Mip):
  global mip,matixPID
  mip = Mip
  mip.utilitiesBox = wx.Window(mip, -1, wx.Point(250,175), wx.Size(210,275), style=wx.RAISED_BORDER)
  mip.utilitiesName = wx.StaticText(mip.utilitiesBox, -1, 'Utilities', wx.Point(72,5))
  mip.utilitiesName.SetFont(mip.basicBoxFont)
  mip.saveSettingsButton = halfButton(1,1,'Save Settings',400)
  mip.loadSettingsButton = halfButton(1,2,'Load Settings',401)
  mip.checkStatusButton = fullButton(2,'Check Status',402)
  mip.checkRemoteButton = fullButton(3,'Check Remote Computers',403)
  mip.openLoggerButton = fullButton(4,'Open Logger',404)
  mip.hideLoggerButton = fullButton(4,'Hide Logger',405)
  mip.hideLoggerButton.Hide()
  mip.openMatrixViewerButton = fullButton(5,'Open Matrix Viewer',406)
  mip.closeMatrixViewerButton = fullButton(5,'Close Matrix Viewer',407)
  mip.closeMatrixViewerButton.Hide()
  mip.reconfigureButton = fullButton(6,'Reconfigure GUI',408)
  wx.EVT_BUTTON(mip.utilitiesBox, 400, saveSettings)
  wx.EVT_BUTTON(mip.utilitiesBox, 401, loadSettings)
  wx.EVT_BUTTON(mip.utilitiesBox, 402, checkStatus)
  wx.EVT_BUTTON(mip.utilitiesBox, 403, checkRemoteStatus)
  wx.EVT_BUTTON(mip.utilitiesBox, 404, openMainLoggerWindow)
  wx.EVT_BUTTON(mip.utilitiesBox, 405, hideMainLoggerWindow)
  wx.EVT_BUTTON(mip.utilitiesBox, 406, openMatrixViewer)
  wx.EVT_BUTTON(mip.utilitiesBox, 407, closeMatrixViewer)
  wx.EVT_BUTTON(mip.utilitiesBox, 408, reconfigureGUI)
  matrixPID = -1
  AOS.ensureGlobalMIP(mip)


def fullButton(row,name,eventNumber):
  return wx.Button(mip.utilitiesBox, eventNumber, name, wx.Point(10, 30 + 40 * (row - 1)), wx.Size(180,30))

def halfButton(row,col,name,eventNumber):
  return wx.Button(mip.utilitiesBox, eventNumber, name, wx.Point(10 + 95 * (col - 1), 30 + 40 * (row - 1)), wx.Size(85,30))

def loadStatus():
  WinBI.loadBIStatus()
  WinMod.loadModStatus()
  WinDaf.checkComputers()
  WinMod.checkForDaf()
  
def loadSettings(event):
  global mip
  filename = ''
  dlg = wx.FileDialog(mip, "Choose a File",style=wx.OPEN)
  if dlg.ShowModal() == wx.ID_OK:
    filename=dlg.GetPath()
  dlg.Destroy()
  if filename <> '':
    try:
      mip.state = Config.loadGUIState(filename,mip.config)
      loadStatus()
      if mip.state['blank']:
        Status.write("Not a valid state file!" + "\n\n")
      else:
        Status.write("State loaded from " + filename + "\n\n")
    except:
      Status.write("Not a valid state file!" + "\n\n")
      
def saveSettings(event):
  global mip
  filename = ''
  dlg = wx.FileDialog(mip, "Select File",style=wx.SAVE|wx.OVERWRITE_PROMPT)
  if dlg.ShowModal() == wx.ID_OK:
    filename=dlg.GetPath()
  dlg.Destroy()
  if filename <> '':
    try:
      Config.backupGUIState(filename,mip.state)
      Status.write("Current state saved in " + filename + "\n\n")
    except:
      Status.write("Save file failed!" + "\n\n")
      
def checkStatus(event):
  status = statusText()
  errors = okToRun()
  Status.write(status + errors)
  
def statusText():
  global mip
  status = '****Basic Data****\n'
  #status = status + 'Root Directory: ' + rootDir + '\n'
  status = status + 'Scenario Name: ' + mip.state['scenarioName'] + '\n'
  if mip.state['createScenario'] == 0:
    status = status + 'Create Scenario: No\n'
  else:
    status = status + 'Create Scenario: Yes'
    if mip.state['scenarioYears'] == '':
      status = status + ' (no years selected)\n'
    else:
      status = status + ' (' + mip.state['scenarioYears'] + ' years)\n'
  status = status + 'Base Year: ' + mip.state['baseYear'] + '\n'
  status = status + 'Start Year: ' + mip.state['startYear'] + '\n'
  status = status + 'Simulation Years: ' + mip.state['simulationYears'] + '\n'
  status = status + '*******Run*******\n'
  runChecker = False
  for name in mip.config['modulesOrd']:
    if name in mip.state['nonDAFChecks']:
      if mip.state['nonDAFChecks'][name] == 1:
        runChecker = True
        status = status + '    ' + name + '\n'
    if name in mip.state['dafChecks']:
      if mip.state['dafChecks'][name] == 1:
        runChecker = True
        status = status + '    ' + name + '-DAF\n'
  if mip.state['spatialCheck'] == 1:
    status = status + '    {Spatial Modules}\n'
    for name in mip.config['modulesOrd']:
      if name in mip.config['spatialNonDafModulesOrd']:
        runChecker = True
        status = status + '        ' + name + '\n'
      if name in mip.config['spatialDafModulesOrd']:
        runChecker = True
        status = status + '        ' + name + '-DAF\n'
  if mip.state['transportCheck'] == 1:
    status = status + '    {Transport Modules}\n'
    for name in mip.config['modulesOrd']:
      if name in mip.config['transportNonDafModulesOrd']:
        runChecker = True
        status = status + '        ' + name + '\n'
      if name in mip.config['transportDafModulesOrd']:
        runChecker = True
        status = status + '        ' + name + '-DAF\n'
  if mip.state['allCheck'] == 1:
    status = status + '    {All Modules}\n'
    for name in mip.config['modulesOrd']:
      if name in mip.config['dafModulesOrd']:
        runChecker = True
        status = status + '        ' + name + '-DAF\n'
      else:
        runChecker = True
        status = status + '        ' + name + '\n'
  if not runChecker:
    status = status + '(none)\n'
  elif mip.state['simulationYears'] <> '' and int(mip.state['simulationYears']) > 1:
    status = status + '\n(All modules run for\n simulation years 1 thru ' + str(int(mip.state['simulationYears']) - 1) + ')\n'
  if mip.dafBoxEnabled:
    status = status + '*******DAF*******\nComputers selected\n'
    for name in mip.config['computerNamesOrd']:
      if mip.state['computerChecks'][name] == 1:
        status = status + '    ' + name + '\n'
  #status = status + '*****************\n'
  return status
  
def okToRun():
  global mip
  errorChecker = False
  errors = '******Errors******\n'
  #if rootDir == '':
  #  errors = errors + 'Root directory not specified.\n'
  #  errorChecker = True
  if mip.state['scenarioName'] == '':
    errors = errors + 'Scenario name not specified.\n'
    errorChecker = True
  if mip.state['baseYear'] == '':
    errors = errors + 'Base year not specified.\n'
    errorChecker = True
  if mip.state['startYear'] == '':
    errors = errors + 'Start year not specified.\n'
    errorChecker = True
  if mip.state['simulationYears'] == '':
    errors = errors + 'Simulation years not specified.\n'
    errorChecker = True
  if (mip.state['createScenario'] == 1) and (mip.state['scenarioYears'] == ''):
    errors = errors + 'Scenario years not specified.\n'
    errorChecker = True
  status = statusText()
  if status.find('(none)') <> -1:
    errors = errors + 'No modules selected to run.\n'
    errorChecker = True
  if mip.dafBoxEnabled:
    for name in mip.state['computerChecks']:
      if mip.state['computerChecks'][name] == 1:
        break
    else:
      errors = errors + 'No computers selected for DAF run.\n'
      errorChecker = True
  if not errorChecker:
    errors = errors + 'None - ready for run.\n'
    mip.readyToRun = True
    mip.startButton.Enable(True)
  else:
    mip.startButton.Enable(False)
  errors = errors + '*****************\n'
  return errors

def checkRemoteStatus(event):
  global mip
  AOS.printRemoteComputerStatus(AOS.checkRemoteComputerStatus(True))
    
def openMatrixViewer(event):
  global mip,matrixPID
  mip.matrixViewer = subprocess.Popen('javaw -classpath ' + Globs.commonBaseClasspath + ';' + Globs.log4jClasspath + ' com.pb.common.matrix.ui.MatrixViewer')
  mip.closeMatrixViewerButton.Show(True)
  mip.openMatrixViewerButton.Hide()
  matrixPID = mip.matrixViewer.pid
  Status.write("The matrix viewer is running\n")
  matrixThread = Thread(target=lambda:isMatrixViewerOpen())
  matrixThread.start()
  
def closeMatrixViewer(event):
  global matrixPID
  if matrixPID <> -1:
    killer = subprocess.Popen('TASKKILL /F /T /PID ' + str(matrixPID),shell=True)
    killer.wait()
    
def isMatrixViewerOpen():
  global matrixPID
  if matrixPID <> -1:
    mip.matrixViewer.wait()
    Status.write("The matrix viewer has closed.\n")
    matrixPID = -1
    mip.openMatrixViewerButton.Show(True)
    mip.closeMatrixViewerButton.Hide()
    
def reconfigureGUI(event):
  #this opens the reconfiguration window and, if reconfiguration is selected, will close this mip
  checkFile = Globs.reconfigureCheckFile
  if os.path.exists(checkFile):
    os.remove(checkFile)
  startFile = file(checkFile,'w')
  startFile.close()
  direct = os.listdir('')
  subprocess.Popen('Reconfigure.exe ' + checkFile)
  #subprocess.Popen('python Reconfigure.py ' + checkFile)
  reconWaitThread = Thread(target=lambda:reconfigureWait(checkFile))
  reconWaitThread.start()
  mip.Enable(False)
  
  
def reconfigureWait(checkFile):
  global mip
  reconDone = False
  while not reconDone:
    time.sleep(1)
    if os.path.exists(checkFile):
      startFile = file(checkFile,'r')
      tesxt = startFile.read()
      startFile.close()
      if tesxt.find('Cancel') <> -1:
        reconDone = True
        os.remove(checkFile)
      elif tesxt.find('Close') <> -1:
        os.remove(checkFile)
        mip.GetGrandParent().onWindowClose(wx.CommandEvent(0,0))
  mip.Enable(True)
  
def openMainLoggerWindow(event):
  global mip
  mip.mlp.showSelf(event)
  
def hideMainLoggerWindow(event):
  global mip
  mip.mlp.hideSelf(event)