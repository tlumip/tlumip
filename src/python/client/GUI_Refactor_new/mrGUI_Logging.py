#mrGUI_Logging.py
import wx,os,socket,subprocess
from threading import Thread
import mrGUI_Globals as Globs

class mainLoggerPanel(wx.Panel):
  def __init__(self, parent, id, Mip):
    wx.Panel.__init__(self, parent, id)
    self.mip = Mip
    mip.mlp = self
    self.lblnameSTD = wx.StaticText(self, -1, "Logger Window",wx.Point(20, 3))
    self.basicBoxFont = self.lblnameSTD.GetFont()
    self.basicBoxFont.SetPointSize(11)
    self.basicBoxFont.SetWeight(wx.FONTWEIGHT_BOLD)
    self.lblnameSTD.SetFont(self.basicBoxFont)
    #self.stdoutName = wx.TextCtrl(self, 500, '', wx.Point(1,25), wx.Size(859,200),style=wx.TE_MULTILINE|wx.TE_READONLY|wx.HSCROLL|wx.VSCROLL) 
    self.mip.logText['main'] =  wx.TextCtrl(self, 500, '', wx.Point(1,25), wx.Size(859,200),style=wx.TE_MULTILINE|wx.TE_READONLY|wx.HSCROLL|wx.VSCROLL|wx.TE_RICH2) 
    #clear output button
    self.clearButton =wx.Button(self, 510, " Clear ", wx.Point(665, 5), wx.Size(60,18))
    wx.EVT_BUTTON(self, 510, self.clearOutputText)
    #save output button
    self.saveButton =wx.Button(self, 520, " Save ", wx.Point(730, 5), wx.Size(60,18))
    wx.EVT_BUTTON(self, 520, self.outputSave)
    #hide window button
    self.hideButton =wx.Button(self, 530, " Hide ", wx.Point(795, 5), wx.Size(60,18))
    wx.EVT_BUTTON(self, 530, self.hideSelf)
  
  def clearOutputText(self, event):
    self.mip.logText['main'].Clear()
    
  def outputSave(self,event):
    """ Save a file"""
    dlg = wx.FileDialog(self, style=23)
    if dlg.ShowModal() == wx.ID_OK:
      self.mip.logText['main'].SaveFile(dlg.GetPath())
    dlg.Destroy()
    
  def hideSelf(self,event):
    self.GetParent().Unsplit()
    self.GetGrandParent().SetSize((870,485))
    self.mip.openLoggerButton.Show()
    self.mip.hideLoggerButton.Hide()
    
  def showSelf(self,event):
    self.GetGrandParent().SetSize((870,715))
    self.GetParent().SplitHorizontally(self.mip,self,456)
    self.mip.hideLoggerButton.Show()
    self.mip.openLoggerButton.Hide()



def loggingControls(Mip):
  global mip
  mip = Mip
  #ant log button
  mip.antLogButton = wx.Button(mip, 505, 'View Ant Log', wx.Point(733, 330), wx.Size(100,20))
  mip.antLogButton.Enable(False)
  wx.EVT_BUTTON(mip, 505, spawnExternalAntViewer)
  
  #initialize and fill logging log constructs
  mip.logText = {'main':-1}
  mip.logPIDs = {'main':-1}
  for name in mip.config['computerNames']:
    mip.logText[name] = -1
    mip.logPIDs[name] = -1
  
  
##ANT File Viewer Definitions##
def checkAntLog():
  global mip
  if mip.antOutText <> '':
    mip.antLogButton.Enable(True)
  else:
    mip.antLogButton.Enable(False)

def spawnExternalAntViewer(event):
  global mip
  at = file(Globs.tempAntFile,'w')
  at.write('ANT Log\n' + mip.antOutText)
  at.close()
  #self.antViewer = subprocess.Popen('python TextViewer.py ' + Globs.tempAntFile)
  mip.antViewer = subprocess.Popen('TextViewer.exe ' + Globs.tempAntFile)
  mip.antLogButton.Enable(False)
  watcherThread = Thread(target=lambda:watchAntViewerThread())
  watcherThread.start()

def watchAntViewerThread():
  global mip
  mip.antViewer.wait()
  #os.remove(Globs.tempAntFile)
  checkAntLog()

def cleanAntFile(antFile):
  antText = ''
  if os.path.isfile(antFile):
    for line in file(antFile):
      testSplit = line.split()
      if (line.strip()).split() <> []:
        if not ((testSplit[0] == '[java]') and (line.find('INFO') <> -1)):
          antText = antText + line
  return antText
####
   
def startPySocketServer(port,name):
  global mip
  if not mip.logText.has_key(name):
    print 'No logger with key name: ' + name
    return
  pySS = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
  pySS.bind(('localhost',port))
  pySS.listen(5)
  #start up server
  while mip.keepLoggerAlive:
    pySS.settimeout(5.0)
    try:
      connection, address = pySS.accept()
      connection.settimeout(None)
      print 'Server connected by ', address
      while 1:
        data = connection.recv(1024)
        if not data: 
          break
        #print data to designated logger
        mip.logText[name].AppendText(data)
        mip.logText[name].ShowPosition(mip.logText[name].GetLastPosition() - 1000)
      connection.close()
    except:
      print 'Socket timed out'

def startExternalLoggerWindow(name,port='7001',filename='.\\Text\\Client.log',intPort='8001'):
  """Start up java log4j clientlogger to get log files"""
  global mip
  if mip.logPIDs[name] == -1:
    #logPIDs[name] = subprocess.Popen('javaw -classpath ' + commonBaseClasspath + ';' + log4jClasspath + ' com.pb.common.logging.LogServer -port ' + port + ' -file ' + filename,shell=False).pid
    mip.logPIDs[name] = subprocess.Popen('javaw -classpath .\\Java;' + Globs.commonBaseClasspath + ';' + Globs.log4jClasspath + ' com.pb.common.logging.LogServer -port ' + port + ' -fileName ' + filename + ' -localPort ' + intPort,shell=False).pid
    
def killExternalLoggerWindow(name):
  """Close a(n) (external) logger (window)"""
  global mip
  if name in mip.logPIDs:
    if mip.logPIDs[name] <> -1:
      #os.system('TASKKILL /F /T /PID ' + str(logPIDs[name]))
      #stdoutTxt = file('.\\Text\\stdout.txt','w')
      #killer = subprocess.Popen('TASKKILL /F /T /PID ' + str(logPIDs[name]),shell=False,stdout=stdoutTxt)
      killer = subprocess.Popen('TASKKILL /F /T /PID ' + str(mip.logPIDs[name]),shell=True)
      killer.wait()
      #stdoutTxt.close()
      mip.logPIDs[name] = -1