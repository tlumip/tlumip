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
FRAME_WIDTH = 950
FRAME_HEIGHT = 600

LEFT_PANEL_COLOR = "light blue"
RIGHT_PANEL_COLOR = "khaki"

"""
STATE_INFO_X = 12
STATE_INFO_OFFSET_X = 70
STATE_INFO_OFFSET_Y = 16
SCENARIO_STATE_TEXT_Y = 25
ANT_STATE_TEXT_Y = STATE_INFO_OFFSET_X + 100
CLUSTER_STATE_TEXT_Y = STATE_INFO_OFFSET_X + 330
"""
TEXT_WIDTH = 150


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

    self.createMainPanel()


    
  def createMainPanel(self):

    self.panel = wx.Panel(self)
    
    #make run target button
    runTargetButton = wx.Button(self.panel, 20, " Run Ant Target ")
    wx.EVT_BUTTON(self, 20, self.onRunAntTarget)        

    rightSizer = wx.BoxSizer(wx.VERTICAL)
    """
    sizers = self.createRightPanel()
    for s in sizers:
        rightSizer.Add(s, 1, wx.EXPAND)
    """
    rightSizer.Add((20,20))
    rightSizer.Add(runTargetButton, 1, wx.EXPAND)

    #panelSizer.Add(self.nb, 0, wx.EXPAND)
    #panelSizer.Add(rightSizer, 0, wx.EXPAND)
    
    #mainSizer.Add(panelSizer, 1, wx.EXPAND)
    #mainSizer.Add(self.statusBar, 0, wx.EXPAND)
    
    self.panel.SetSizer(rightSizer)
    rightSizer.Fit(self)
    rightSizer.SetSizeHints(self)
    #mainSizer.Fit(self)
    #mainSizer.SetSizeHints(self)


  def createRightPanel(self):
    sizers = []
    self.scenarioStateLabelItems = []
    sizers.append(self.createStateTextLayout('Selected Scenario:', ['Name:', 'Created By:', 'Created At:', 'Base Year:', 'Description:'], self.scenarioStateLabelItems))
    self.antStateLabelTextItems = []
    sizers.append(self.createStateTextLayout('Selected Ant Run Target:', ['Target:', "Req'd Args:", 'Description:'], self.antStateLabelTextItems))
    self.clusterStateLabelTextItems = []
    sizers.append(self.createStateTextLayout('Selected Cluster:', ['Cluster Machines:'], self.clusterStateLabelTextItems))
    return sizers
    
  def createLeftPanel(self, list):
    self.nb = wx.Notebook(self.panel)
    if len(list) == 0:
        return
    for item in list:
      p = TextPanel(self.nb, 'stdout for pid=%d on %s' % (item[1], item[0]))
      self.nb.AddPage(p, '%s, pid=%d' % (item[0], item[1]))
      p.setPageText('z:/models/pythonSrc/tmp/%s/stdout.txt' % item[0])
          
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
  
  def onRunAntTarget(self,event):
    machines = self.state.getMachinesUsed()
    print machines
    if len(machines) == 0:
        wx.MessageBox ("Please define a cluster with at least one machine.", "No Cluster Defined", wx.OK)
        return
        
    # result is a list of tuples: (str machineName, int processId)
    result = self.serverConnection.startModelRun("runVersions", "", "", 0, 0, machines)
    print 'result of onRunAntTarget:', result
    
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
    self.showScenarioState()
    
  def setCurrentAntTarget(self,antName):
    for item in self.antTargetsTable:
        if antName == item[0]:
            self.state.setAntTarget(item[0])
            self.state.setAntDescription(item[1])
            self.state.setAntReqdArgs(item[2])
            break
    self.showAntState()

  def setCurrentCluster(self,machines):
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

  def createStateTextLayout(self, title, headings, labelTextItems):
    sizer = wx.FlexGridSizer(cols=2, hgap=5, vgap=5)
    sectionHeading = wx.StaticText(self.panel, -1, title)
    sectionHeading.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.BOLD))
    sizer.Add(sectionHeading, 0, wx.ALIGN_RIGHT)
    sizer.Add((10,10))
    for i, h in enumerate(headings):
        headingItem = wx.StaticText(self.panel, -1, h)
        labelItem = wx.StaticText(self.panel, -1, '', size=(TEXT_WIDTH,-1), style=wx.ST_NO_AUTORESIZE)
        labelTextItems.append(labelItem)
        sizer.Add(headingItem, 0, wx.ALIGN_RIGHT)
        sizer.Add(labelItem, 0, wx.ALIGN_LEFT)
    return sizer
    
  def showScenarioState(self):
    values = [self.state.getScenarioName(), self.state.getUserName(), self.state.getScenarioCreationTime(), self.state.getBaseYear(), self.state.getScenarioDescription()]
    for i, l in enumerate(self.scenarioStateLabelItems):
        l.SetLabel(values[i])
        l.Wrap(TEXT_WIDTH)
      
  def showAntState(self):
    argsList = self.state.getAntReqdArgs()
    argsString = argsList[0]
    for x in argsList[1:]:
        argsString += ', %s' % x
    values = [self.state.getAntTarget(), argsString, self.state.getAntDescription()]

    for i, l in enumerate(self.antStateLabelTextItems):
        l.SetLabel(values[i])
        l.Wrap(TEXT_WIDTH)

  def showClusterState(self):
    values = self.state.getMachinesUsed()
    for i, l in enumerate(self.clusterStateLabelTextItems):
        l.SetLabel(values[i])
        l.Wrap(TEXT_WIDTH)
  
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
    result = []
    if self.state.has_key('ClusterMachinesUsed'):
        result = self.state['ClusterMachinesUsed']
    return result
    
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
    self.callingFrame.setCurrentCluster([])
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
    names = self.getSelectedMachines()
    self.callingFrame.setCurrentCluster(names)
    self.callingFrame.showClusterState()
    self.callingFrame.enableFrame()
    self.Destroy()
    
  def onCancel(self, event):
    self.callingFrame.enableFrame()
    self.Destroy()
    
  def getSelectedMachines(self):
    checked = False
    enabled = False
    checkedQualifier = 'not'
    enabledQualifier = 'not'
    if self.panelItems == None or len(self.panelItems) == 0:
      print 'no machines in list'
      return []
    else:
      result = []
      for i, p in enumerate(self.panelItems):
        if p.IsEnabled():
          enabled = True
          enabledQualifier = '\b'
          if p.IsChecked():
            checked = True
            checkedQualifier = '\b'
            result.append(self.callingFrame.clusterNames[i])
          else:
            checkedQualifier = 'not'
        else:
          enabledQualifier = 'not'
        print '%s is %s enabled and is %s checked' % (self.callingFrame.clusterNames[i], enabledQualifier, checkedQualifier)
          
    
    return result


###############################################################

class TextPanel(wx.Panel):
  def __init__(self, parent, heading):
    wx.Panel.__init__(self, parent)
    
    pageHeading = wx.StaticText(self, -1, heading)
    pageHeading.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.BOLD))
    #self.SetBackgroundColour(LEFT_PANEL_COLOR)
    self.pageTextArea = wx.TextCtrl(self, -1, '', style=wx.TE_MULTILINE)
    #self.pageTextArea.SetBackgroundColour(LEFT_PANEL_COLOR)
    self.pageTextArea.SetFont(wx.Font(10, wx.MODERN, wx.NORMAL, wx.NORMAL))
    
    sizer = wx.BoxSizer(wx.VERTICAL)
    sizer.Add(pageHeading, 0, wx.EXPAND)
    sizer.Add(self.pageTextArea, 1, wx.EXPAND)
    self.SetSizer(sizer)
    
    
  def setPageText(self, file):
    f = open(file)
    try:
      for line in f:
        self.pageTextArea.WriteText(line)
    finally:
      f.close()



  
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