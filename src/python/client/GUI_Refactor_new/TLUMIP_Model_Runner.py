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
import wx,os


#sys.stderr = file(Globs.stdErrFile,'w')
#sys.stdout = file(Globs.stdOutFile,'w')

#Load configuration, and last state
guiConfig = mrGUI_Configuration.loadGUIConfig()
guiConfig['ipAddress'] = GetTrueIP_VPN.trueIP(False)
guiState = mrGUI_Configuration.loadGUIState(Globs.stateBackup,guiConfig)

remoteProcessRunning = False

class mainPanel(wx.Panel):
  def __init__(self, parent, id):
    global guiConfig, guiState
    wx.Panel.__init__(self, parent, id)
    self.state = guiState
    self.config = guiConfig
    self.initer = True
    
    #load up gui elements
    WinBI.basicInputsPanel(self)
    WinMod.modulesPanel(self)
    WinDAF.dafPanel(self)
    WinUts.utilitiesPanel(self)
    Status.statusLogger(self)
    WinControl.modelControl(self)
    WinLog.loggingControls(self)
    AOS.ensureGlobalMIP(self)
    AOS.initAOServerStuff()
    
    self.initer = False
    WinUts.okToRun()
    #get ip address info
    Status.write('ipAddress = ' + self.config['ipAddress'] + '\n')
    self.vpnChecker()
    Status.write('Base computer = ' + self.config['baseComputer'] + ' (' + self.config['computerIPList'][self.config['baseComputer']] + ')\n')
    #load up initial information
    WinUts.loadStatus()
    self.initer=False
    WinMod.checkForDaf()

  def vpnChecker(self):
    ipInfo = os.popen('ipconfig').read()
    ip = ipInfo.find('SonicWALL Virtual Adapter') 
    if ip <> -1:
      vpnDialog = wx.MessageDialog(self,'Are you connecting to base computer via VPN?','VPN',wx.YES_NO|wx.YES_DEFAULT)
      vpnAnswer = vpnDialog.ShowModal()
      if vpnAnswer == wx.ID_YES:
        ipAddress = GetTrueIP_VPN.trueIP(True)
        self.statusWindow.Clear()
        Status.write('ipAddress = ' + self.config['ipAddress'] + '\n')
        
#Extend wx.Frame class so that correct actions are performed on close
class Framer(wx.Frame):
  def __init__(self, parent, id, title, size):
    wx.Frame.__init__(self, parent, id, title, size=size)
    self.Bind(wx.EVT_CLOSE,self.onWindowClose)
    
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
    frame = NewScenarioFrame()
    frame.Show()
  
  def onOpenSubmenuItem(self, event):
    item = self.openSubmenu.FindItemById(event.GetId())
    itemText = item.GetText()
    print "selected scenario '%s' for opening from Scenario menu" % itemText
    
  def onWindowClose(self, event):
    closeWindowActions()
    os._exit(0)



def closeWindowActions():
  for name in mip.logPIDs:
    WinLog.killExternalLoggerWindow(name)
  mrGUI_Configuration.backupGUIState(Globs.stateBackup,mip.state)

################State information classs######################
class scenarioState(object):
  def __init__(self):
    self.state = {}
  
  def setBaseYear(value):
    self.state['baseYear']
  
  def setScenarioYears(value):
    self.state['scenarioYears']
  
  def setScenarioName(value):
    self.state['scenarioName'] = value
    
  def setUserName(value):
    self.state['userName'] = value
  
  def setScenarioCreationTime(value):
    self.state['scenarioCreationTime'] = value
    
  def setScenarioDescription(value):
    self.state['scenarioDescription'] = value
  
  def getState():
    return self.state


#############################################################


#############Create new scenario popup box####################
class NewScenarioFrame(wx.Frame):
  def __init__(self):  #, size = (40,70)
    wx.Frame.__init__(self, None, -1, "Create New Scenario")
    
    panel = wx.Panel(self, -1)

    #Scenario name text box    
    wx.StaticText(panel, -1, 'Scenario Name', wx.Point(12,43))
    self.scenarioName = wx.TextCtrl(panel, 10, '', wx.Point(88, 40), wx.Size(225,-1))

    #Base year radio button
    self.rb = wx.RadioBox(panel, -1, "Base Year", wx.Point(10,101), wx.DefaultSize, ['1990','2000'], 2, wx.RA_SPECIFY_COLS)
    
    #Scenario years text
    wx.StaticText(panel, -1, 'Scenario Years', wx.Point(200,101))
    self.sc = wx.SpinCtrl(panel, -1, '', wx.Point(200,120), wx.Size(50,-1))
    self.sc.SetRange(1,30)
    self.sc.SetValue(1)


###############################################################



  
app = wx.PySimpleApp()
frame = Framer(None,-1," TLUMIP Model Runner",size = (870,485))
sw = wx.SplitterWindow(frame,-1)
mip = mainPanel(sw,-1)
mlp = WinLog.mainLoggerPanel(sw,-1,mip)
sw.SplitHorizontally(mip,mlp,460)
sw.Unsplit()
frame.Show(1)
app.MainLoop()