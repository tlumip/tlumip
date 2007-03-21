#Reconfigure.py
"""This loads up a wxPython window that will reconfigure the TLUMIP_ModelRunner gui's
   computer settings (which computers and what is the base)
   It allows you to either load a saved configuration settings file, or to create your
   own based on computer selections (assumes base computer files are {name}_CompConfig.txt
   1 argument must be passed in: the PID of the callin TLUMIP_ModelRunner instance"""

import wx, os, sys, time, subprocess
import mrGUI_Globals as Globs

checkSave = False
#####Extend frame class to get shutdown operations
class Framer(wx.Frame):
  def __init__(self, parent, id, title, size):
    wx.Frame.__init__(self, parent, id, title, size=size)
    self.Bind(wx.EVT_CLOSE,self.onWindowClose)
  def onWindowClose(self, event):
    restartApp(False)
#####

#####
class ReconfigureNotebook(wx.Notebook):
  def __init__(self, parent, id):
    wx.Notebook.__init__(self, parent, id)
    
  def configSave(self,event):
    """ Save a file"""
    global checkSave
    dlg = wx.FileDialog(self, wildcard='TLUMIP config files (*.tcn)|*.tcn',style=wx.SAVE|wx.OVERWRITE_PROMPT)
    if dlg.ShowModal() == wx.ID_OK:
      if dlg.GetPath().find(Globs.configFile) <> -1:
        noSaveDialog = wx.MessageDialog(self,'Cannot save over main TLUMIP configuation file!','Error!',wx.OK)
        noSaveDialog.ShowModal()
      else:
        oldFile = file(Globs.configFile,'r')
        newFile = file(dlg.GetPath(),'w')
        newFile.write(oldFile.read())
        oldFile.close()
        newFile.close()
        checkSave = True
    dlg.Destroy()
    
  def closeSelf(self,event):
    restartApp(False)
  
  def checkForSave(self):
    global checkSave
    checkSaveText = 'Current configuration settings may not have been saved.  \n' + \
                    'Setting a new configuration will overwrite current configuration settings!  \n' + \
                    'Click cancel and hit the "Save Current Configuration Settings" button to go back.  \n' + \
                    'Click ok to continue and set the new configuration.'
    if not checkSave:
      askCheckSave = wx.MessageDialog(self,checkSaveText,'Wait!',wx.OK|wx.CANCEL)
      return askCheckSave.ShowModal()
    else:
      return wx.ID_OK
#####

#####
class ReconfigureMainPanel(wx.Panel):
  def __init__(self, parent, id):
    wx.Panel.__init__(self, parent, id)
    mainText = 'Use this tab to load TLUMIP_Model_Runner configuration settings ' + \
               'from a file.  If you would like to construct the settings from a ' + \
               'list of computers, choose the tab with the name of the desired cluster set.'
    self.mainLabel = wx.StaticText(self, -1, mainText,wx.Point(10, 10))
    self.mainLabel.Wrap(320)
    #Load configuration from file button
    self.loadButton = wx.Button(self, 1, " Load Configuration from File ", wx.Point(65, 100), wx.Size(200,25))
    wx.EVT_BUTTON(self, 1, self.configLoad)
    #Save configuration button
    wx.StaticLine(self,-1,wx.Point(15,200),wx.Size(300,2))
    self.saveButton = wx.Button(self, 2, " Save Current Configuration Settings ", wx.Point(15, 215), wx.Size(200,25))
    wx.EVT_BUTTON(self, 2, nb.configSave)
    #Cancel button
    self.saveButton = wx.Button(self, 3, " Cancel ", wx.Point(235, 215), wx.Size(80,25))
    wx.EVT_BUTTON(self, 3, nb.closeSelf)
    
  def configLoad(self,event):
    """ Load a configuation file"""
    if nb.checkForSave() == wx.ID_OK:
      dlg = wx.FileDialog(self, wildcard='TLUMIP config files (*.tcn)|*.tcn',style=wx.OPEN)
      if dlg.ShowModal() == wx.ID_OK:
        if dlg.GetPath().find(Globs.configFile) <> -1:
          noLoadDialog = wx.MessageDialog(self,'Cannot load current configuration file!','Error!',wx.OK)
          noLoadDialog.ShowModal()
        else:
          loadFile = file(dlg.GetPath(),'r')
          loadText = loadFile.read()
          loadFile.close()
          if self.checkConfig(loadText):
            os.remove(Globs.configFile)
            newFile = file(Globs.configFile,'w')
            newFile.write(loadText)
            newFile.close()
            dlg.Destroy()
            restartApp(True)
          else:
            noLoadDialog = wx.MessageDialog(self,'Not a valid configuration file!','Error!',wx.OK)
            noLoadDialog.ShowModal()
      dlg.Destroy()
  
  def checkConfig(self,configText):
    #this only checks that there are enough lines, and that all of the configChecks exists
    configChecks = ['modules','nonDAFModules','dafModules','spatialNonDafModules','spatialDafModules','transportNonDafModules',
                   'transportDafModules','computerNames','computerIPList','baseComputer']
    if configText.count('\n') < 2 * len(configChecks) - 1:
      return False
    for check in configChecks:
      if configText.find(check) == -1:
        return False
    return True
#####

#####
class ReconfigurePanel(wx.Panel):
  def __init__(self, parent, id, compList):
    wx.Panel.__init__(self, parent, id)
    mainText = 'The base computer is the one to which the TLUMIP_Model_Runner program will connect and send its command to.'
    self.mainLabel = wx.StaticText(self, -1, mainText,wx.Point(10, 10))
    self.mainLabel.Wrap(320)
    
    self.baseLabel = wx.StaticText(self, -1, 'Base',wx.Point(10, 40))
    self.compLabel = wx.StaticText(self, -1, 'Computer Name (IP Address)',wx.Point(50, 40))
    self.boldFont = self.compLabel.GetFont()
    #self.basicBoxFont.SetPointSize(11)
    self.boldFont.SetWeight(wx.FONTWEIGHT_BOLD)
    self.baseLabel.SetFont(self.boldFont)
    self.compLabel.SetFont(self.boldFont)
    
    #list computers
    self.compString = ''
    self.compIPString = ''
    self.compCheckBoxList = []
    self.compCheckList = {}
    self.compChecked = ''
    for i in range(len(compList)):
      if self.compString == '':
        self.compString = compList[i][0].strip()
        self.compIPString = compList[i][1].strip()
      else:
        self.compString = self.compString + ' ' + compList[i][0].strip()
        self.compIPString = self.compIPString + ' ' + compList[i][1].strip()
      self.compCheckBoxList.append(wx.CheckBox(self,20 + i,'       ' + compList[i][0].strip() + ' (' + compList[i][1].strip() + ')',wx.Point(13, 55 + 17*i)))
      self.compCheckList[20 + i] = compList[i][0].strip()
      wx.EVT_CHECKBOX(self, 20 + i, self.setBaseComputer)
      
    #Set configuration button
    self.setConfigurationButton = wx.Button(self, 50, "Set Configuration ", wx.Point(215, 145), wx.Size(110,45))
    wx.EVT_BUTTON(self, 50, self.setConfiguration)
    
    #Save configuration button
    wx.StaticLine(self,-1,wx.Point(15,200),wx.Size(300,2))
    self.saveButton = wx.Button(self, 2, " Save Current Configuration Settings ", wx.Point(15, 215), wx.Size(200,25))
    wx.EVT_BUTTON(self, 2, nb.configSave)
    #Cancel button
    self.saveButton = wx.Button(self, 3, " Cancel ", wx.Point(235, 215), wx.Size(80,25))
    wx.EVT_BUTTON(self, 3, nb.closeSelf)
    
  def setBaseComputer(self,event):
    self.clearChecks(event)
    self.compChecked = self.compCheckList[event.GetId()]
    
  def clearChecks(self,event):
    for checkbox in self.compCheckBoxList:
      if checkbox.GetId() <> event.GetId():
        checkbox.SetValue(False)
  
  def setConfiguration(self,event):
    if nb.checkForSave() == wx.ID_OK:
      if self.compChecked == '':
        noBaseDialog = wx.MessageDialog(self,'No base computer selected!','Error!',wx.OK)
        return noBaseDialog.ShowModal()
      else:
        templateTextFile = file(Globs.reconTemplate)
        templateText = templateTextFile.read()
        templateTextFile.close()
        templateText = templateText.replace('@@computer name list@@',self.compString)
        templateText = templateText.replace('@@computer ip list@@',self.compIPString)
        templateText = templateText.replace('@@base computer@@',self.compChecked)
        os.remove(Globs.configFile)
        newFile = file(Globs.configFile,'w')
        newFile.write(templateText)
        newFile.close()
        restartApp(True)
#####
    
def restartApp(yesOrNo):
  global checkFile
  cf = file(checkFile,'w')
  if yesOrNo:
    cf.write('Close')
    cf.close()
    time.sleep(1)
    direct = os.listdir('')
    if 'TLUMIP_Model_Runner.py' in direct:
      subprocess.Popen('python TLUMIP_Model_Runner.py')
    else:
      subprocess.Popen('TLUMIP_Model_Runner.exe')
    os._exit(0)
  else:
    cf.write('Cancel')
    cf.close()
    os._exit(0)


checkFile = ''
if (len(sys.argv) < 2):
  print 'No check file specified!'
else:
  checkFile = sys.argv[1]


reconApp = wx.PySimpleApp()
reconFrame = Framer(None,-1,' Reconfigure TLUMIP Model Runner',size = (350,300))
nb = ReconfigureNotebook(reconFrame,-1)

configDir = Globs.reconFileDirectory
dirList = os.listdir(configDir)
masterList = []
panelList = []
for name in dirList:
  if name.find('CompConfig') > 1:
    masterList.append(name[0:name.find('CompConfig') - 1])
    compList = []
    #get base ip, and add computer name/ip to complist
    for line in file(configDir + '\\' + name):
      if line.strip() <> '':
        compList.append(line.split())
    panelList.append(ReconfigurePanel(nb,-1,compList))

reconMP = ReconfigureMainPanel(nb,-1)
nb.AddPage(reconMP,' Load From File')
for i in range(len(masterList)):
  nb.AddPage(panelList[i], ' ' + masterList[i])
reconFrame.Show(1)
reconApp.MainLoop()
