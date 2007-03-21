#mrGUI_DAF.py
import wx

def dafPanel(Mip):
  global mip
  mip = Mip
  mip.dafSettingsBox = wx.Window(mip, -1, wx.Point(671,10), wx.Size(180,300), style=wx.RAISED_BORDER)
  mip.dafSettingsBoxName = wx.StaticText(mip.dafSettingsBox, -1, 'DAF Settings', wx.Point(43,5))
  mip.dafSettingsBoxName.SetFont(mip.basicBoxFont)
  counter = 1
  mip.computerCheckboxList = {}
  mip.loggerButtonList = {}
  mip.computerText1 = wx.StaticText(mip.dafSettingsBox, -1, 'Include', wx.Point(15,27))
  mip.computerText2 = wx.StaticText(mip.dafSettingsBox, -1, 'Computer', wx.Point(11,38))
  mip.computerText1.SetFont(mip.dafTextFont)
  mip.computerText2.SetFont(mip.dafTextFont)
  for name in mip.config['computerNamesOrd']:
    wx.StaticText(mip.dafSettingsBox, -1, name, wx.Point(35,30 + counter*25))
    mip.computerCheckboxList[name] = wx.CheckBox(mip.dafSettingsBox, 300 + counter, '',wx.Point(16,31 + counter*25))
    mip.config['computerNames'][name] = 300 + counter
    wx.EVT_CHECKBOX(mip.dafSettingsBox, 300 + counter, computerChecker)
    mip.loggerButtonList[name] = wx.Button(mip.dafSettingsBox, 320 + counter, "Open Logger", wx.Point(82,29 + counter*25), wx.Size(80,18))
    #loggerName[name] = 320 + counter
    mip.loggerButtonList[name].Enable(False)
    counter = counter + 1
  #Clear button
  mip.clearButton = wx.Button(mip.dafSettingsBox, 340, "Clear", wx.Point(7, 272), wx.Size(69,20))
  wx.EVT_BUTTON(mip, 340, clearDafSettings)
  #Default button
  mip.defaultButton = wx.Button(mip.dafSettingsBox, 350, "Default Settings", wx.Point(80, 272), wx.Size(85,20))
  wx.EVT_BUTTON(mip, 350, defaultDafSettings)
  for name in mip.state['computerChecks']:
    if mip.state['computerChecks'][name] == 1:
      break
  else:
    setDefaultDafSettings()
  mip.dafSettingsBox.Enable(False)
  mip.dafBoxEnabled = False

def checkComputers():
  global mip
  for name in mip.state['computerChecks']:
    if mip.state['computerChecks'][name] == 1:
      mip.computerCheckboxList[name].SetValue(True)
    else:
      mip.computerCheckboxList[name].SetValue(False)
      
def computerChecker(event):
  global mip
  for name in mip.state['computerChecks']:
    if mip.config['computerNames'][name] == event.GetId():
      if event.IsChecked():
        mip.state['computerChecks'][name] = 1
      else:
        mip.state['computerChecks'][name] = 0
        
def clearDafSettings(event):
  global mip
  for name in mip.state['computerChecks']:
    mip.state['computerChecks'][name] = 0
    mip.computerCheckboxList[name].SetValue(False)
    
def defaultDafSettings(event):
  setDefaultDafSettings()
  
def setDefaultDafSettings():
  global mip
  for name in mip.state['computerChecks']:
    mip.state['computerChecks'][name] = 1
    mip.computerCheckboxList[name].SetValue(True)