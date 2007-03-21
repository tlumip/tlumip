#mrGUI_Modules.py
import wx

def modulesPanel(Mip):
  global mip
  mip = Mip
  mip.moduleBox = wx.Window(mip, -1, wx.Point(470,10), wx.Size(191,440), style=wx.RAISED_BORDER)
  mip.moduleBoxName = wx.StaticText(mip.moduleBox, -1, 'What to Run', wx.Point(47,5))
  mip.moduleBoxName.SetFont(mip.basicBoxFont)
  mip.dafText = wx.StaticText(mip.moduleBox, -1, 'DAF', wx.Point(78,41))
  mip.nonDAFText = wx.StaticText(mip.moduleBox, -1, 'Non-DAF', wx.Point(20,41))
  mip.moduleText = wx.StaticText(mip.moduleBox, -1, 'Module', wx.Point(118,41))
  mip.dafTextFont = mip.dafText.GetFont()
  mip.dafTextFont.SetWeight(wx.FONTWEIGHT_BOLD)
  mip.dafText.SetFont(mip.dafTextFont)
  mip.nonDAFText.SetFont(mip.dafTextFont)
  mip.moduleText.SetFont(mip.dafTextFont)
  wx.StaticLine(mip.moduleBox, -1, wx.Point(103,39), wx.Size(-1,222),style=wx.LI_VERTICAL)
  wx.StaticLine(mip.moduleBox, -1, wx.Point(70,39), wx.Size(-1,222),style=wx.LI_VERTICAL)
  wx.StaticBox(mip.moduleBox, -1, '', wx.Point(14,29), wx.Size(161,232))
  counter = 1
  mip.dafCheckboxList = {}
  mip.nonDAFCheckboxList = {}
  for name in mip.config['modulesOrd']:
    wx.StaticText(mip.moduleBox, -1, name, wx.Point(111,40 + counter*25))
    mip.myGroup = wx.StaticLine(mip.moduleBox, -1, wx.Point(19,34 + counter*25), wx.Size(152,-1),style=wx.LI_HORIZONTAL)
    if mip.config['nonDAFModules'].has_key(name):
      mip.nonDAFCheckboxList[name] = wx.CheckBox(mip.moduleBox, 100 + counter, '',wx.Point(38,41 + counter*25))
      mip.config['nonDAFModules'][name] = 100 + counter
      wx.EVT_CHECKBOX(mip.moduleBox, 100 + counter, nonDafChecker)
    if mip.config['dafModules'].has_key(name):
      mip.dafCheckboxList[name] = wx.CheckBox(mip.moduleBox, 150 + counter, '',wx.Point(81,41 + counter*25))
      mip.config['dafModules'][name] = 150 + counter
      wx.EVT_CHECKBOX(mip.moduleBox, 150 + counter, dafChecker)
    counter = counter + 1
  ##Here is how to enable/disable/check boxes and the like
  #mip.nonDAFCheckboxList['ED'].Enable(False)
  #mip.nonDAFCheckboxList['ED'].SetValue(True)
  ##
  #Spatial button
  mip.spatialButton = wx.Button(mip.moduleBox, 210, " Spatial ", wx.Point(50, 275), wx.Size(120,30))
  mip.spatialCheckBox = wx.CheckBox(mip.moduleBox, 210, '', wx.Point(20, 283))
  wx.EVT_BUTTON(mip, 210, spatialModule)
  wx.EVT_CHECKBOX(mip, 210, spatialModule)
  #Transport button
  mip.transportButton = wx.Button(mip.moduleBox, 220, " Transport ", wx.Point(50, 317), wx.Size(120,30))
  mip.transportCheckBox = wx.CheckBox(mip.moduleBox, 220, '', wx.Point(20, 325))
  wx.EVT_BUTTON(mip, 220, transportModule)
  wx.EVT_CHECKBOX(mip, 220, transportModule)
  #All button
  mip.allButton = wx.Button(mip.moduleBox, 230, " All ", wx.Point(50, 359), wx.Size(120,30))
  mip.allCheckBox = wx.CheckBox(mip.moduleBox, 230, '', wx.Point(20, 367))
  wx.EVT_BUTTON(mip, 230, allModule)
  wx.EVT_CHECKBOX(mip, 230, allModule)
  #Clear button
  mip.clearButton = wx.Button(mip.moduleBox, 240, " Clear ", wx.Point(10, 400), wx.Size(160,30))
  wx.EVT_BUTTON(mip, 240, clearModule)
  #this is used to keep from having recursive function calls
  mip.flipper = True
  
  
def checkNonDAFDefs():
  global mip
  for name in mip.state['nonDAFChecks']:
    if mip.state['nonDAFChecks'][name] == 1:
      mip.nonDAFCheckboxList[name].SetValue(True)
    else:
      mip.nonDAFCheckboxList[name].SetValue(False)
      
def checkDAFDefs():
  global mip
  for name in mip.state['dafChecks']:
    if mip.state['dafChecks'][name] == 1:
      mip.dafCheckboxList[name].SetValue(True)
    else:
      mip.dafCheckboxList[name].SetValue(False)
      
def checkSpatialDefs():
  global mip
  if mip.state['spatialCheck'] == 1:
    mip.state['spatialCheck'] = 0
    spatialModule(wx.CommandEvent(0,0))
  else:
    mip.spatialCheckBox.SetValue(False)
    
def checkTransportDefs():
  global mip
  if mip.state['transportCheck'] == 1:
    mip.state['transportCheck'] = 0
    transportModule(wx.CommandEvent(0,0))
  else:
    mip.transportCheckBox.SetValue(False)
    
def checkAllDefs():
  global mip
  if mip.state['allCheck'] == 1:
    mip.state['allCheck'] = 0
    allModule(wx.CommandEvent(0,0))
  else:
    mip.allCheckBox.SetValue(False)
    
def nonDafChecker(event):
  global mip
  for name in mip.config['nonDAFModules']:
    if mip.config['nonDAFModules'][name] == event.GetId():
      if event.IsChecked():
        mip.state['nonDAFChecks'][name] = 1
        if name in mip.state['dafChecks']:
          mip.state['dafChecks'][name] = 0
          mip.dafCheckboxList[name].SetValue(False)
      else:
        mip.state['nonDAFChecks'][name] = 0
  checkForDaf()
  
def dafChecker(event):
  global mip
  for name in mip.config['dafModules']:
    if mip.config['dafModules'][name] == event.GetId():
      if event.IsChecked():
        mip.state['dafChecks'][name] = 1
        if name in mip.state['nonDAFChecks']:
          mip.state['nonDAFChecks'][name] = 0
          mip.nonDAFCheckboxList[name].SetValue(False)
      else:
        mip.state['dafChecks'][name] = 0
  checkForDaf()
  
def spatialModule(event):
  global mip
  if mip.transportCheckBox.GetValue() and mip.flipper:
    mip.flipper = False
    transportModule(event)
    mip.flipper = True
  if mip.allCheckBox.GetValue() and mip.flipper:
    mip.flipper = False
    allModule(event)
    mip.flipper = True
  mip.flipper = False
  clearModule(event)
  mip.flipper = True
  if mip.state['spatialCheck'] == 1:
    mip.state['spatialCheck'] = 0
    mip.spatialCheckBox.SetValue(False)
    unGreyOutModules()
  else:
    for name in mip.config['spatialNonDafModules']:
      mip.nonDAFCheckboxList[name].SetValue(True)
    for name in mip.config['spatialDafModules']:
      mip.dafCheckboxList[name].SetValue(True)
    mip.state['spatialCheck'] = 1
    mip.spatialCheckBox.SetValue(True)
    greyOutModules()
  checkForDaf()
  
def transportModule(event):
  global mip
  if mip.spatialCheckBox.GetValue() and mip.flipper:
    mip.flipper = False
    spatialModule(event)
    mip.flipper = True
  if mip.allCheckBox.GetValue() and mip.flipper:
    mip.flipper = False
    mip.allModule(event)
    mip.flipper = True
  mip.flipper = False
  clearModule(event)
  mip.flipper = True
  if mip.state['transportCheck'] == 1:
    mip.state['transportCheck'] = 0
    mip.transportCheckBox.SetValue(False)
    unGreyOutModules()
  else:
    for name in mip.config['transportNonDafModules']:
      mip.nonDAFCheckboxList[name].SetValue(True)
    for name in mip.config['transportDafModules']:
      mip.dafCheckboxList[name].SetValue(True)
    mip.state['transportCheck'] = 1
    mip.transportCheckBox.SetValue(True)
    greyOutModules()
  checkForDaf()
  
def allModule(event):
  #assumes that daf will be run, if it can
  global mip
  if mip.spatialCheckBox.GetValue() and mip.flipper:
    mip.flipper = False
    spatialModule(event)
    mip.flipper = True
  if mip.transportCheckBox.GetValue() and mip.flipper:
    mip.flipper = False
    transportModule(event)
    mip.flipper = True
  mip.flipper = False
  clearModule(event)
  mip.flipper = True
  if mip.state['allCheck'] == 1:
    mip.state['allCheck'] = 0
    mip.allCheckBox.SetValue(False)
    unGreyOutModules()
  else:
    mip.state['allCheck'] = 1
    mip.allCheckBox.SetValue(True)
    for name in mip.state['dafChecks']:
      mip.dafCheckboxList[name].SetValue(True)
      mip.state['dafChecks'][name] = 0
    for name in mip.state['nonDAFChecks']:
      if name in mip.state['dafChecks']:
        mip.nonDAFCheckboxList[name].SetValue(False)
      else:
        mip.nonDAFCheckboxList[name].SetValue(True)
      mip.state['nonDAFChecks'][name] = 0
    greyOutModules()  
  checkForDaf()  
  
def clearModule(event):
  global mip
  for name in mip.state['dafChecks']:
    mip.dafCheckboxList[name].SetValue(False)
    mip.state['dafChecks'][name] = 0
  for name in mip.state['nonDAFChecks']:
    mip.nonDAFCheckboxList[name].SetValue(False)
    mip.state['nonDAFChecks'][name] = 0
  if mip.flipper:
    mip.flipper = False
    if mip.state['spatialCheck'] == 1:
      spatialModule(event)
    if mip.state['transportCheck'] == 1:
      transportModule(event)
    if mip.state['allCheck'] == 1:
      allModule(event)
    mip.flipper = True
  checkForDaf()
  
def greyOutModules():
  global mip
  for name in mip.state['nonDAFChecks']:
    mip.nonDAFCheckboxList[name].Enable(False)
  for name in mip.state['dafChecks']:
    mip.dafCheckboxList[name].Enable(False)
    
def unGreyOutModules():
  global mip
  for name in mip.state['nonDAFChecks']:
    mip.nonDAFCheckboxList[name].Enable(True)
  for name in mip.state['dafChecks']:
    mip.dafCheckboxList[name].Enable(True)
    
def checkForDaf():
  global mip
  dafChecked = False
  for name in mip.state['dafChecks']:
    if mip.state['dafChecks'][name] == 1:
      dafChecked = True
      break
  if not mip.initer:
    if (mip.state['spatialCheck'] == 1) or (mip.state['transportCheck'] == 1) or (mip.state['allCheck'] == 1) or dafChecked or ((mip.state['simulationYears'] <> '') and (int(mip.state['simulationYears']) > 1)):
      mip.dafSettingsBox.Enable(True)
      mip.dafBoxEnabled = True
    else:
      mip.dafSettingsBox.Enable(False)
      mip.dafBoxEnabled = False

def loadModStatus():
  checkNonDAFDefs()
  checkDAFDefs()
  unGreyOutModules()
  checkSpatialDefs()
  checkTransportDefs()
  checkAllDefs()