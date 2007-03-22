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
import GetTrueIP_VPN
import wx,os,time,threading


#sys.stderr = file(Globs.stdErrFile,'w')
#sys.stdout = file(Globs.stdOutFile,'w')

#Load configuration, and last state
#guiConfig = mrGUI_Configuration.loadGUIConfig()
#guiConfig['ipAddress'] = GetTrueIP_VPN.trueIP(False)
#guiState = mrGUI_Configuration.loadGUIState(Globs.stateBackup,guiConfig)

remoteProcessRunning = False

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
  def __init__(self, parent, id, title, size, state):
    wx.Frame.__init__(self, parent, id, title, size=size)
    self.Bind(wx.EVT_CLOSE,self.onWindowClose)
    
    self.state = state
    
    menuBar = wx.MenuBar()
    scenarioMenu = wx.Menu()
    
    #a list of scenarios retrieved from AOServer
    scenarioList = ['pleaseWork', 'bingBangBoom', 'oh $#%@$?!']
    self.openSubmenu = wx.Menu()
    for scenario in scenarioList:
      item = self.openSubmenu.Append(-1, scenario)
      self.Bind(wx.EVT_MENU, self.onOpenSubmenuItem, item)
    
    new = scenarioMenu.Append(-1, "&New")
    self.Bind(wx.EVT_MENU, self.onNew, new)
    scenarioMenu.AppendMenu(-1, "&Open...", self.openSubmenu)
    
    menuBar.Append(scenarioMenu,"&Scenario")
    self.SetMenuBar(menuBar)
    
  def onNew(self, event):
    print "selected 'new' from Scenario menu"
    frame = NewScenarioFrame(self, self.state)
    frame.Show()
    self.disableFrame()
  
  def onOpenSubmenuItem(self, event):
    item = self.openSubmenu.FindItemById(event.GetId())
    itemText = item.GetText()
    print "selected scenario '%s' for opening from Scenario menu" % itemText
    
  def enableFrame(self):
    self.Enable(True)
    print self.state.getState()
  
  def disableFrame(self):
    self.Enable(False)
    
  def onWindowClose(self, event):
    closeWindowActions()
    os._exit(0)



def closeWindowActions():
  #for name in mip.logPIDs:
  #  WinLog.killExternalLoggerWindow(name)
  #mrGUI_Configuration.backupGUIState(Globs.stateBackup,mip.state)
  None

################State information classs######################
class ScenarioState(object):
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
    self.sc.SetRange(1,30)
    self.sc.SetValue(self.scenarioYears)
    wx.EVT_SPINCTRL(panel,12,self.setScenarioYears)
    wx.EVT_TEXT(self, 12, self.setScenarioYears)
    
    #Scenario description text
    wx.StaticText(panel, -1, 'Scenario Description', wx.Point(12,155))
    self.scenarioDescription = ''
    self.scenarioDescriptionTextBox = wx.TextCtrl(panel, 17, self.scenarioDescription, wx.Point(12,170), wx.Size(335,60), style = wx.TE_MULTILINE)
    wx.EVT_TEXT(panel, 17, self.setScenarioDescription)
    
    
    #Create scenario button
    self.createScenarioButton = wx.Button(panel, 20, " CreateScenario ", wx.Point(195, 240), wx.Size(150,25))
    wx.EVT_BUTTON(self, 20, self.onCreate)
    
    #Create scenario button
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
    self.callingFrame.enableFrame()
    self.Destroy()
  
  def onCancel(self,event):
    self.callingFrame.enableFrame()
    self.Destroy()
      

###############################################################



  
app = wx.PySimpleApp()
state = ScenarioState()
frame = Framer(None,-1," TLUMIP Model Runner",size = (870,485), state = state)
#sw = wx.SplitterWindow(frame,-1)
#mip = mainPanel(sw,-1)
#mlp = WinLog.mainLoggerPanel(sw,-1,mip)
#sw.SplitHorizontally(mip,mlp,460)
#sw.Unsplit()
frame.Show(1)
app.MainLoop()