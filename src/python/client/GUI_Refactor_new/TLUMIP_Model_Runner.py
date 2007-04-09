#TLUMIP_Model_Runner.py

#importations
import mrGUI_Globals as Globs
import mrGUI_Configuration
import mrGUI_Basic_Inputs as WinBI
import mrGUI_Modules as WinMod
import mrGUI_DAF as WinDAF
import mrGUI_Status_Logger as Status
import mrGUI_Utilities as WinUts
import mrGUI_Model_Control as WinControl
import mrGUI_Logging as WinLog
import mrGUI_AOServer_Communication as AOS
import ServerConnectionClient as Server
import GetTrueIP_VPN
import wx,os,time,threading


#sys.stderr = file(Globs.stdErrFile,'w')
#sys.stdout = file(Globs.stdOutFile,'w')

#Load configuration, and last state
#guiConfig = mrGUI_Configuration.loadGUIConfig()
#guiConfig['ipAddress'] = GetTrueIP_VPN.trueIP(False)
#guiState = mrGUI_Configuration.loadGUIState(Globs.stateBackup,guiConfig)

remoteProcessRunning = False

#Initial GUI Dimensions
FRAME_WIDTH = 870
FRAME_HEIGHT = 485
PANEL_WIDTH = 870 - 8
PANEL_HEIGHT = 485 - 75
INITIAL_SASH_X = int(0.70*FRAME_WIDTH)

LEFT_PANEL_COLOR = "pale green"
RIGHT_PANEL_COLOR = "khaki"

STATE_INFO_X = 12
STATE_INFO_OFFSET_X = 70
STATE_INFO_OFFSET_Y = 16
SCENARIO_STATE_TEXT_Y = 25
ANT_STATE_TEXT_Y = STATE_INFO_OFFSET_X + 100
CLUSTER_STATE_TEXT_Y = STATE_INFO_OFFSET_X + 220



class mainPanel(wx.Panel):
  def __init__(self, parent, id):
    #global guiConfig, guiState
    wx.Panel.__init__(self, parent, id)
    #self.state = guiState
    #self.config = guiConfig
    #self.initer = True
    #
    ##load up gui elements
    #WinBI.basicInputsPanel(self)
    #WinMod.modulesPanel(self)
    #WinDAF.dafPanel(self)
    #WinUts.utilitiesPanel(self)
    #Status.statusLogger(self)
    #WinControl.modelControl(self)
    #WinLog.loggingControls(self)
    #AOS.ensureGlobalMIP(self)
    #AOS.initAOServerStuff()
    #
    #self.initer = False
    #WinUts.okToRun()
    ##get ip address info
    #Status.write('ipAddress = ' + self.config['ipAddress'] + '\n')
    #self.vpnChecker()
    #Status.write('Base computer = ' + self.config['baseComputer'] + ' (' + self.config['computerIPList'][self.config['baseComputer']] + ')\n')
    ##load up initial information
    #WinUts.loadStatus()
    #self.initer=False
    #WinMod.checkForDaf()

  #def vpnChecker(self):
  #  ipInfo = os.popen('ipconfig').read()
  #  ip = ipInfo.find('SonicWALL Virtual Adapter') 
  #  if ip <> -1:
  #    vpnDialog = wx.MessageDialog(self,'Are you connecting to base computer via VPN?','VPN',wx.YES_NO|wx.YES_DEFAULT)
  #    vpnAnswer = vpnDialog.ShowModal()
  #    if vpnAnswer == wx.ID_YES:
  #      ipAddress = GetTrueIP_VPN.trueIP(True)
  #      self.statusWindow.Clear()
  #      Status.write('ipAddress = ' + self.config['ipAddress'] + '\n')
        
#Extend wx.Frame class so that correct actions are performed on close
class Framer(wx.Frame):
  def __init__(self, parent, id, title, size, state, serverConnection):
    wx.Frame.__init__(self, parent, id, title, size=size)
    self.Bind(wx.EVT_CLOSE,self.onWindowClose)
    
    self.serverConnection = serverConnection
    self.existingScenarioProperties = serverConnection.getExistingScenarioProperties()
    self.existingScenarioNames = self.existingScenarioProperties.keys()
    self.antTargetsTable = serverConnection.getAvailableAntTargets()
    self.antTargetNames = self.getAntTargetNames(self.antTargetsTable)
    self.clusterDicts = serverConnection.getAvailableMachines()
    self.clusterNames = self.getClusterNames(self.clusterDicts,'NAME')
    self.state = state

    self.createMenuBar()
    self.createStatusBar()

    self.spw = wx.SplitterWindow(self, size=(PANEL_WIDTH,PANEL_HEIGHT))
    self.panelLeft = wx.Panel(self.spw, size=(PANEL_WIDTH,PANEL_HEIGHT))
    self.panelLeft.SetBackgroundColour(LEFT_PANEL_COLOR)
    self.panelRight = wx.Panel(self.spw, size=(PANEL_WIDTH,PANEL_HEIGHT))
    self.panelRight.SetBackgroundColour(RIGHT_PANEL_COLOR)
    self.spw.SplitVertically(self.panelLeft, self.panelRight, sashPosition=INITIAL_SASH_X)
    self.spw.SetMinimumPaneSize(10)

    self.createScenarioStateTextItems()
    self.antStateHeadingTextItems = []
    self.antStateLabelTextItems = []
    self.clusterStateHeadingTextItems = []
    self.clusterStateLabelTextItems = []
    self.antStateHeadings = ['Target:', "Req'd Args:", 'Description:']
    self.createStateTextItems('Selected Ant Run Target:', self.antStateHeadings, ANT_STATE_TEXT_Y, self.antStateHeadingTextItems, self.antStateLabelTextItems)
    self.clusterStateHeadings = ['Cluster Machines:']
    self.createStateTextItems('Selected Cluster:', self.clusterStateHeadings, CLUSTER_STATE_TEXT_Y, self.clusterStateHeadingTextItems, self.clusterStateLabelTextItems)
    
  def getAntTargetNames(self, table):
    names = []
    for item in table:
        names.append(item[0])
    return names
       
  def getClusterNames(self, clusterDicts, nameKey):
    names = []
    for item in clusterDicts:
        names.append(item[nameKey])
    return names
       
  def createStatusBar(self):
    self.statusBar = wx.StatusBar(self,-1)
    
  def setBaseStatusBarText(self,labels):
    self.statusBar.SetFieldsCount(len(labels))
    self.statusBarBaseText = {}
    for item in labels:
        if item == '':
            self.statusBarBaseText[item] = item
        else:
            self.statusBarBaseText[item] = '%s = ' % item
    
  def clearStatusBar(self, labels):
    self.setBaseStatusBarText(labels)
    for i, item in enumerate(labels):
        self.statusBar.SetStatusText(self.statusBarBaseText[item],i)
  
  def populateStatusBar(self, labels, values, widths = []):
    self.setBaseStatusBarText(labels)
    for i, item in enumerate(labels):
        self.statusBar.SetStatusText(self.statusBarBaseText[item]+values[i],i)
        if len(widths) > 0:
            self.statusBar.SetStatusWidths(widths)
  
  
  def createMenuBar(self):
    self.menuBar = wx.MenuBar()
    self.scenarioMenu = wx.Menu()
    
    new = self.scenarioMenu.Append(-1, "&New...")
    self.Bind(wx.EVT_MENU, self.onNew, new)
    
    #a list of scenarios is retrieved from AOServer
    self.createOpenSubmenu()
    #a list of ant targets is retrieved from AOServer
    self.createRunMenu()
    #a list of cluster machines is retrieved from AOServer
    self.createClusterMenu()
    
    self.menuBar.Append(self.scenarioMenu,"&Scenario")
    self.menuBar.Append(self.runMenu,"&Run")
    self.menuBar.Append(self.clusterMenu,"&Cluster")
    self.SetMenuBar(self.menuBar)

    self.menuBar.Bind(wx.EVT_MENU_CLOSE, self.onMenuClose)

  def createOpenSubmenu(self):
    self.openSubmenu = wx.Menu()
    self.existingScenarioNames.sort()
    for scenario in self.existingScenarioNames:
      item = self.openSubmenu.Append(-1, scenario)
      self.Bind(wx.EVT_MENU, self.onOpenSubmenuItem, item)
      self.Bind(wx.EVT_MENU_HIGHLIGHT, self.onOpenSubmenuMouseOver, item)
    self.Bind(wx.EVT_MENU_CLOSE, self.onMenuClose)
      
    self.scenarioMenu.AppendMenu(201, "&Open", self.openSubmenu)

  def createRunMenu(self):
    self.runMenu = wx.Menu()
    for name in self.antTargetNames:
      item = self.runMenu.Append(-1, name)
      self.Bind(wx.EVT_MENU, self.onRunMenuItem, item)
      self.Bind(wx.EVT_MENU_HIGHLIGHT, self.onRunMenuMouseOver, item)

  def createClusterMenu(self):
    self.clusterMenu = wx.Menu()
    item = self.clusterMenu.Append(-1, "&New...")
    self.Bind(wx.EVT_MENU, self.onClusterMenuItem, item)
    
    """
    self.clusterMenu = wx.Menu()
    for name in self.clusterNames:
      item = self.clusterMenu.Append(-1, name)
      self.Bind(wx.EVT_MENU, self.onClusterMenuItem, item)
      self.Bind(wx.EVT_MENU_HIGHLIGHT, self.onClusterMenuMouseOver, item)
    """

  def destroyOpenSubmenu(self):
    self.scenarioMenu.Remove(201)
          
  def onNew(self, event):
    frame = NewScenarioFrame(self, self.state)
    frame.Show()
    self.disableFrame()
  
  def onOpenSubmenuItem(self, event):
    item = self.openSubmenu.FindItemById(event.GetId())
    itemText = item.GetText()
    self.setCurrentScenario(itemText)
    
  def onRunMenuItem(self,event):
    item = self.runMenu.FindItemById(event.GetId())
    itemText = item.GetText()
    self.setCurrentAntTarget(itemText)
            
  def onClusterMenuItem(self,event):
    self.clusterFrame = DefineClusterFrame(self)
    self.clusterFrame.Show()
    self.disableFrame()
    
  def onOpenSubmenuMouseOver(self,event):
    item = self.openSubmenu.FindItemById(event.GetId())
    itemText = item.GetText()
    self.setStatusBarScenarioInfo(itemText)
  
  def onRunMenuMouseOver(self,event):
    item = self.runMenu.FindItemById(event.GetId())
    itemText = item.GetText()
    self.setStatusBarAntInfo(itemText)
  
  def onClusterMenuMouseOver(self,event):
    item = self.clusterMenu.FindItemById(event.GetId())
    itemText = item.GetText()
    self.setStatusBarClusterInfo(itemText)
      
  def onMenuClose(self,event):
    self.clearStatusBar([''])
  
  def createNewScenario(self):
    result = self.serverConnection.tempCreateScenario(self.state.getScenarioName(),self.state.getScenarioYears(),self.state.getBaseYear(),self.state.getUserName(),self.state.getScenarioDescription())
    print result
    waitThread = threading.Thread(target=lambda:self.waitForScenarioReady())
    waitThread.start()
    
  def setStatusBarScenarioInfo(self,scenarioName):
    scenarioProperties = self.existingScenarioProperties[scenarioName]
    scenarioProperties['scenarioName'] = scenarioName
    labels = self.setStatusBarScenarioItems()
    values = []
    for item in labels:
        values.append(scenarioProperties[item])
    self.populateStatusBar(labels, values, [-1,100,110,130,270])
    
  def setStatusBarAntInfo(self,antName):
    for item in self.antTargetsTable:
        if antName == item[0]:
            antDescription = item[1]
            break
    labels = self.setStatusBarAntItems()
    values = []
    values.append(antName)
    values.append(antDescription)
    self.populateStatusBar(labels, values, [200,-1])
    
  def setStatusBarClusterInfo(self,machineName):
    for item in self.clusterDicts:
        if item['NAME'] == machineName:
            machineIp = item['IP']
            machineCpus = item['PROCESSORS']
            machineRam = item['RAM']
            machineOS = item['OS']
            machineDescription = item['DESCRIPTION']
            machineStatus = item['STATUS']
            break
    labels = self.setStatusBarClusterItems()
    values = []
    values.append(machineName)
    values.append(machineIp)
    values.append(machineStatus)
    values.append(machineCpus)
    values.append(machineRam + 'GB')
    values.append(machineOS)
    values.append(machineDescription)
    self.populateStatusBar(labels, values, [105,150,115,55,65,85,-1])
    
  def setCurrentScenario(self,scenarioName):
    scenarioProperties = self.existingScenarioProperties[scenarioName]
    self.state.setScenarioName(scenarioName)
    self.state.setBaseYear(scenarioProperties['baseYear'])
    self.state.setScenarioYears(scenarioProperties['scenarioYears'])
    self.state.setUserName(scenarioProperties['userName'])
    self.state.setScenarioCreationTime(scenarioProperties['scenarioCreationTime'])
    self.state.setScenarioDescription(scenarioProperties['scenarioDescription'])
    self.showSenarioState()
    
  def setCurrentAntTarget(self,antName):
    for item in self.antTargetsTable:
        if antName == item[0]:
            self.state.setAntTarget(item[0])
            self.state.setAntDescription(item[1])
            self.state.setAntReqdArgs(item[2])
            break
    print self.state.getState()
    self.showAntState()

  def setCurrentCluster(self):
    machines = self.clusterFrame.getSelectedMachines()
    self.state.setMachinesUsed(machines)

  def setStatusBarScenarioItems(self):    
    labels = []
    labels.append('scenarioName')
    labels.append('baseYear')
    labels.append('scenarioYears')
    labels.append('userName')
    labels.append('scenarioCreationTime')
    return labels
  
  def setStatusBarAntItems(self):    
    labels = []
    labels.append('antTarget')
    labels.append('antDescription')
    return labels
  
  def setStatusBarClusterItems(self):
    labels = []
    labels.append('Machine')
    labels.append('Ip address')
    labels.append('Status')
    labels.append('CPUs')
    labels.append('RAM')
    labels.append('OS')
    labels.append('Description')
    return labels

  def enableFrame(self):
    self.Enable(True)
    print self.state.getState()
  
  def disableFrame(self):
    self.Enable(False)
    
  def waitForScenarioReady(self):
    while self.serverConnection.isScenarioReady(self.state.getScenarioName()) == "Scenario Not Ready":
        time.sleep(1)
    self.existingScenarioProperties = serverConnection.getExistingScenarioProperties()
    self.existingScenarioNames = self.existingScenarioProperties.keys()
    self.destroyOpenSubmenu()
    self.createOpenSubmenu()
    self.setCurrentScenario(self.state.getScenarioName())

  def createScenarioStateTextItems(self):
    self.scenarioStateHeadingItems = []
    self.scenarioStateLabelItems = []
    sectionHeading = wx.StaticText(self.panelRight, -1, 'Selected Scenario:', (STATE_INFO_X, SCENARIO_STATE_TEXT_Y - 1.2*STATE_INFO_OFFSET_Y))
    sectionHeadingFont = wx.Font(10, wx.SWISS, wx.NORMAL, wx.BOLD)
    sectionHeading.SetFont(sectionHeadingFont)
    headings = ['Name:', 'Created By:', 'Created At:', 'Base Year:', 'Description:']
    for i, h in enumerate(headings):
        self.scenarioStateHeadingItems.append(wx.StaticText(self.panelRight, -1, h, (STATE_INFO_X, SCENARIO_STATE_TEXT_Y + i*STATE_INFO_OFFSET_Y)))
        self.scenarioStateLabelItems.append(wx.StaticText(self.panelRight, -1, '', (STATE_INFO_X + STATE_INFO_OFFSET_X, SCENARIO_STATE_TEXT_Y + i*STATE_INFO_OFFSET_Y)))
    
  def createStateTextItems(self, title, headings, yOffset, headingTextItems, labelTextItems):
    sectionHeading = wx.StaticText(self.panelRight, -1, title, (STATE_INFO_X, yOffset - 1.2*STATE_INFO_OFFSET_Y))
    sectionHeading.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.BOLD))
    for i, h in enumerate(headings):
        headingTextItems.append(wx.StaticText(self.panelRight, -1, h, (STATE_INFO_X, yOffset + i*STATE_INFO_OFFSET_Y)))
        labelTextItems.append(wx.StaticText(self.panelRight, -1, '', (STATE_INFO_X + STATE_INFO_OFFSET_X, yOffset + i*STATE_INFO_OFFSET_Y)))
    
  def showSenarioState(self):
    values = [self.state.getScenarioName(), self.state.getUserName(), self.state.getScenarioCreationTime(), self.state.getBaseYear(), self.state.getScenarioDescription()]
    for i, l in enumerate(self.scenarioStateLabelItems):
        l.SetLabel(values[i])
    descriptionWidth = self.panelRight.GetSizeTuple()[0] - (STATE_INFO_X + STATE_INFO_OFFSET_X)
    self.scenarioStateLabelItems[-1].Wrap(descriptionWidth)
      
  def showAntState(self):
    labelWidth = self.panelRight.GetSizeTuple()[0] - (STATE_INFO_X + STATE_INFO_OFFSET_X)

    headings = []
    for h in self.antStateHeadings:
        headings.append(h)

    argsList = self.state.getAntReqdArgs()
    argsString = argsList[0]
    for x in argsList[1:]:
        argsString += ', %s' % x
    values = [self.state.getAntTarget(), argsString, self.state.getAntDescription()]

    newValues = []
    n = 1
    for i, v in enumerate(values):
        l = wx.StaticText(self.panelRight, -1, v, (-1000,-1000))
        l.Wrap(labelWidth)
        lines = l.GetLabel().splitlines()
        for k, line in enumerate(lines):
            newValues.append(line)
            if k > 0:
                headings.insert(n, '')
                n += 1
        n += 1

    for i, h in enumerate(self.antStateHeadingTextItems):
        h.SetLabel('')
        self.antStateLabelTextItems[i].SetLabel('')

    self.antStateHeadingTextItems = []        
    self.antStateLabelTextItems = []        
    for i, h in enumerate(headings):
        self.antStateHeadingTextItems.append(wx.StaticText(self.panelRight, -1, h, (STATE_INFO_X, ANT_STATE_TEXT_Y + i*STATE_INFO_OFFSET_Y)))
        self.antStateLabelTextItems.append(wx.StaticText(self.panelRight, -1, newValues[i], (STATE_INFO_X + STATE_INFO_OFFSET_X, ANT_STATE_TEXT_Y + i*STATE_INFO_OFFSET_Y)))

  def showClusterState(self):
    self.clusterStateHeadingTextItems = []
    self.clusterStateLabelTextItems = []
    values = self.state.getMachinesUsed()
    for i, v in enumerate(values):
        self.clusterStateHeadingTextItems.append(wx.StaticText(self.panelRight, -1, '', (STATE_INFO_X, ANT_STATE_TEXT_Y + i*STATE_INFO_OFFSET_Y)))
        self.clusterStateLabelTextItems.append(wx.StaticText(self.panelRight, -1, v, (STATE_INFO_X + STATE_INFO_OFFSET_X, CLUSTER_STATE_TEXT_Y + i*STATE_INFO_OFFSET_Y)))
  
  def onWindowClose(self, event):
    closeWindowActions()
    os._exit(0)



def closeWindowActions():
  #for name in mip.logPIDs:
  #  WinLog.killExternalLoggerWindow(name)
  #mrGUI_Configuration.backupGUIState(Globs.stateBackup,mip.state)
  None

################State information classs######################
class ModelRunState(object):
  def __init__(self):
    self.state = {}
  
  def setBaseYear(self,value):
    self.state['baseYear'] = value
  
  def setScenarioYears(self,value):
    self.state['scenarioYears'] = value
  
  def setScenarioName(self,value):
    self.state['scenarioName'] = value
    
  def setUserName(self,value):
    self.state['userName'] = value
  
  def setScenarioCreationTime(self,value):
    self.state['scenarioCreationTime'] = value
    
  def setScenarioDescription(self,value):
    self.state['scenarioDescription'] = value
  
  def setAntTarget(self,value):
    self.state['antTarget'] = value
  
  def setAntDescription(self,value):
    self.state['antDescription'] = value
  
  def setAntReqdArgs(self,value):
    self.state['antReqdArgs'] = value
    
  def setMachinesUsed(self,value):
    self.state['ClusterMachinesUsed'] = value
  
  def getBaseYear(self):
    return self.state['baseYear']
  
  def getScenarioYears(self):
    return self.state['scenarioYears']
  
  def getScenarioName(self):
    return self.state['scenarioName']
    
  def getUserName(self):
    return self.state['userName']
  
  def getScenarioCreationTime(self):
    return self.state['scenarioCreationTime']
    
  def getScenarioDescription(self):
    return self.state['scenarioDescription']
  
  def getAntTarget(self):
    return self.state['antTarget']
  
  def getAntReqdArgs(self):
    return self.state['antReqdArgs']
  
  def getAntDescription(self):
    return self.state['antDescription']
  
  def getMachinesUsed(self):
    return self.state['ClusterMachinesUsed']
  
  def getState(self):
    return self.state


#############################################################


#############Create new scenario popup box####################
class NewScenarioFrame(wx.Frame):
  def __init__(self,callingFrame, state):  #, size = (40,70)
    wx.Frame.__init__(self, None, -1, "Create New Scenario",size = (370,305))
    
    self.callingFrame = callingFrame
    
    panel = wx.Panel(self, -1)

    #Scenario name text box  
    self.scenarioName = ''  
    wx.StaticText(panel, -1, 'Scenario Name', wx.Point(12,23))
    self.scenarioNameTextBox = wx.TextCtrl(panel, 10, self.scenarioName, wx.Point(88, 20), wx.Size(225,-1))
    wx.EVT_TEXT(panel, 10, self.setScenarioName)

    #User name text box  
    self.userName = os.environ['USERNAME']  
    wx.StaticText(panel, -1, 'User Name', wx.Point(12,63))
    self.scenarioNameTextBox = wx.TextCtrl(panel, 15, self.userName, wx.Point(88, 60), wx.Size(85,-1))
    wx.EVT_TEXT(panel, 15, self.setUserName)

    #Base year radio button
    self.radioBoxItems = ['1990','2000']
    self.baseYear = self.radioBoxItems[0]
    self.rb = wx.RadioBox(panel, 11, "Base Year", wx.Point(10,95), wx.DefaultSize, self.radioBoxItems, 2, wx.RA_SPECIFY_COLS)
    wx.EVT_RADIOBOX(panel, 11, self.setBaseYear)
    
    #Scenario years text
    self.scenarioYears = 1
    wx.StaticText(panel, -1, 'Scenario Years', wx.Point(200,95))
    self.sc = wx.SpinCtrl(panel, 12, '', wx.Point(200,114), wx.Size(50,-1), )
    self.sc.SetRange(1,31)
    self.sc.SetValue(self.scenarioYears)
    wx.EVT_SPINCTRL(panel,12,self.setScenarioYears)
    wx.EVT_TEXT(self, 12, self.setScenarioYears)
    
    #Scenario description text
    wx.StaticText(panel, -1, 'Scenario Description', wx.Point(12,155))
    self.scenarioDescription = ''
    self.scenarioDescriptionTextBox = wx.TextCtrl(panel, 17, self.scenarioDescription, wx.Point(12,170), wx.Size(335,60), style = wx.TE_MULTILINE)
    wx.EVT_TEXT(panel, 17, self.setScenarioDescription)
    
    
    #Create scenario button
    self.createScenarioButton = wx.Button(panel, 20, " Create Scenario ", wx.Point(195, 240), wx.Size(150,25))
    wx.EVT_BUTTON(self, 20, self.onCreate)
    
    #Cancel button
    self.createScenarioButton = wx.Button(panel, 22, " Cancel ", wx.Point(10, 240), wx.Size(150,25))
    wx.EVT_BUTTON(self, 22, self.onCancel)
    
  def setScenarioName(self,event):
    self.scenarioName = event.GetString()
  
  def setUserName(self,event):
    self.userName = event.GetString()
    
  def setBaseYear(self,event):
    self.baseYear = self.radioBoxItems[event.GetInt()]
    
  def setScenarioYears(self,event):
    self.scenarioYears = self.sc.GetValue()
    self.sc.SetValue(self.scenarioYears)
  
  def setScenarioDescription(self,event):
    self.scenarioDescription = event.GetString()
    
  def onCreate(self,event):
    #print [self.scenarioName,self.baseYear,self.scenarioYears]
    state.setScenarioName(self.scenarioName)
    state.setUserName(self.userName)
    state.setBaseYear(self.baseYear)
    state.setScenarioYears(self.scenarioYears)
    state.setScenarioDescription(self.scenarioDescription)
    
    self.callingFrame.createNewScenario()
    self.callingFrame.enableFrame()
    self.Destroy()
  
  def onCancel(self,event):
    self.callingFrame.enableFrame()
    self.Destroy()
      


#############Create cluster definition popup box####################
class DefineClusterFrame(wx.Frame):
  def __init__(self, callingFrame):  #, size = (40,70)
    wx.Frame.__init__(self, callingFrame, -1, "Select Machines for Cluster", size=(300,400))
    
    self.callingFrame = callingFrame
    
    panel = wx.Panel(self, -1)

    #Machine list check boxes  
    xOffset = 30
    yOffset = 30
    
    self.panelItems = []
    wx.StaticText(panel, -1, 'Check available machines to include in cluster', wx.Point(xOffset,yOffset))
    for name in callingFrame.clusterNames:
        for item in callingFrame.clusterDicts:
            if item['NAME'] == name:
                avail = False
                if item['STATUS'] == "Available":
                    avail = True
                break
                
        yOffset = yOffset + 20
        p = wx.CheckBox(panel, -1, name, wx.Point(xOffset,yOffset), wx.Size(75,-1))
        p.Enable(avail) 
        self.panelItems.append(p)
      
    #Create scenario button
    self.createScenarioButton = wx.Button(panel, 20, " Select Cluster ", wx.Point(150, 300), wx.Size(100,25))
    wx.EVT_BUTTON(self, 20, self.onCreate)
    
    #Cancel button
    self.createScenarioButton = wx.Button(panel, 22, " Cancel ", wx.Point(35, 300), wx.Size(80,25))
    wx.EVT_BUTTON(self, 22, self.onCancel)


  def onCreate(self, event):
    self.callingFrame.setCurrentCluster()
    self.callingFrame.enableFrame()
    self.Destroy()
    
  def onCancel(self, event):
    self.callingFrame.enableFrame()
    self.Destroy()
    
  def getSelectedMachines(self):
      checked = False
      if self.panelItems == None or len(self.panelItems) == 0:
          print 'no machines in list'
          return []
      else:
          result = []
          for i, p in enumerate(self.panelItems):
            if p.IsChecked():
              checked = True
              result.append(self.callingFrame.clusterNames[i])
            if checked:
                qualifier = ''
            else:
                qualifier = 'not'
            print 'machine %s is %s checked' % (self.callingFrame.clusterNames[i], qualifier)
    
      return result


###############################################################



  
app = wx.PySimpleApp()
state = ModelRunState()
serverConnection = Server.ServerConnection("192.168.1.141", 8942)
frame = Framer(None,-1," TLUMIP Model Runner", (FRAME_WIDTH,FRAME_HEIGHT), state, serverConnection)
#sw = wx.SplitterWindow(frame,-1)
#mip = mainPanel(sw,-1)
#mlp = WinLog.mainLoggerPanel(sw,-1,mip)
#sw.SplitHorizontally(mip,mlp,460)
#sw.Unsplit()
frame.Show(1)
app.MainLoop()