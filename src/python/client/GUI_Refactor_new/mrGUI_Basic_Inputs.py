#mrGUI_Basic_Inputs.py
import wx,time
import mrGUI_Modules as WinMod

def basicInputsPanel(Mip):
  global mip
  mip = Mip
  mip.basicBox = wx.Window(mip, -1, wx.Point(10,10), wx.Size(450,155), style=wx.RAISED_BORDER)
  mip.basicBoxName = wx.StaticText(mip.basicBox, -1, 'Basic Inputs', wx.Point(180,5))
  mip.basicBoxFont = mip.basicBoxName.GetFont()
  mip.basicBoxFont.SetPointSize(11)
  mip.basicBoxFont.SetWeight(wx.FONTWEIGHT_BOLD)
  mip.basicBoxName.SetFont(mip.basicBoxFont)
  #Scenario Name
  mip.lblnameSN = wx.StaticText(mip.basicBox, -1, 'Scenario Name',wx.Point(10,43))
  mip.editnameSN = wx.TextCtrl(mip.basicBox, 10, mip.state['scenarioName'], wx.Point(88, 40), wx.Size(225,-1))  
  wx.EVT_TEXT(mip.basicBox, 10, setScenarioName)
  #Create scenario first checkbox
  mip.createScenarioBox = wx.CheckBox(mip.basicBox, 11, "Create Scenario First",wx.Point(50,75))
  wx.EVT_CHECKBOX(mip.basicBox, 11, setCreateScenario)
  #Scenario years text
  mip.scenarioYearsText = wx.StaticText(mip.basicBox, -1, 'Scenario Years',wx.Point(200,75),style=wx.ALIGN_CENTER)
  wx.EVT_TEXT(mip.basicBox, 12, setScenarioYears)   
  #Base year radio button
  mip.baseYearRB = wx.RadioBox(mip.basicBox, 13, "Base Year", wx.Point(10,101), wx.DefaultSize,['1990','2000'], 2, wx.RA_SPECIFY_COLS)
  wx.EVT_RADIOBOX(mip.basicBox, 13, setBaseYear)
  if mip.state['baseYear'] == '':
    mip.state['baseYear'] = '1990'
  #Start year
  mip.lblnameSY = wx.StaticText(mip.basicBox, -1, 'Start Year',wx.Point(135,118))
  setStartYearList()
  wx.EVT_TEXT(mip.basicBox, 14, setStartYear)
  #Number of simulation years
  mip.lblnameSMY = wx.StaticText(mip.basicBox, -1, 'Number of Simulation Years',wx.Point(278,113),style=wx.ALIGN_CENTER)
  mip.lblnameSMY.Wrap(100)
  wx.EVT_TEXT(mip.basicBox, 15, setSimulationYears)
  loadBIStatus()
  
def checkTextValues():
  global mip
  mip.editnameSN.SetValue(mip.state['scenarioName'])
  mip.scenarioYearsSelectBox.SetValue(mip.state['scenarioYears'])
  mip.startYearSelectBox.SetValue(mip.state['startYear'])
  mip.simulationYearSelectBox.SetValue(mip.state['simulationYears'])

def setScenarioName(event):
  global mip
  mip.state['scenarioName'] = event.GetString()
  
def checkCreateScenario():
  global mip
  if mip.state['createScenario'] == 1:
    mip.createScenarioBox.SetValue(True)
  else:
    mip.createScenarioBox.SetValue(False)
    
def setCreateScenario(event):
  global mip
  if event.IsChecked():
    mip.state['createScenario'] = 1
  else:
    mip.state['createScenario'] = 0
  activateScenarioYears()
  setStartYearList()

def activateScenarioYears():
  global mip
  if mip.state['createScenario'] == 1:
    mip.scenarioYearsText.Enable(True)
    mip.scenarioYearsSelectBox.Enable(True)
  else:
    mip.scenarioYearsText.Enable(False)
    mip.scenarioYearsSelectBox.Enable(False)

def setScenarioYearsList():
  global mip
  length = 40
  if mip.state['baseYear'] == '1990':
    length = 50
  else:
    if (mip.state['scenarioYears'] <> '') and (int(mip.state['scenarioYears']) > 40):
      mip.state['scenarioYears'] = ''
  scenarioYearList = []
  for i in range(1,length+1):
    scenarioYearList.append(str(i))
  if not mip.initer:
    #self.tempBox=wx.ComboBox(self.basicBox, 17, startYear, wx.Point(397, 83), wx.Size(43, -1),[], wx.CB_READONLY)
    mip.tempBox=wx.ComboBox(mip.basicBox, 14, mip.state['startYear'], wx.Point(277, 73), wx.Size(43, -1),[], wx.CB_READONLY)
    mip.scenarioYearsSelectBox.Destroy()
    #self.scenarioYearsSelectBox=wx.ComboBox(self.basicBox, 17, scenarioYears, wx.Point(397, 83), wx.Size(43, -1),scenarioYearList, wx.CB_READONLY)
    mip.scenarioYearsSelectBox=wx.ComboBox(mip.basicBox, 12, mip.state['scenarioYears'], wx.Point(277, 73), wx.Size(43, -1),scenarioYearList, wx.CB_READONLY)
    mip.tempBox.Destroy()
  else:
    #self.scenarioYearsSelectBox=wx.ComboBox(self.basicBox, 17, scenarioYears, wx.Point(397, 83), wx.Size(43, -1),scenarioYearList, wx.CB_READONLY)
    mip.scenarioYearsSelectBox=wx.ComboBox(mip.basicBox, 12, mip.state['scenarioYears'], wx.Point(277, 73), wx.Size(43, -1),scenarioYearList, wx.CB_READONLY)
  activateScenarioYears()
 
def setScenarioYears(event):
  global mip
  mip.state['scenarioYears'] = event.GetString()

def getBaseYear():
  global mip
  if mip.state['baseYear'] == '1990':
    mip.baseYearRB.SetSelection(0)
  else:
    mip.baseYearRB.SetSelection(1)
  setScenarioYearsList()
    
def setBaseYear(event):
  global mip
  if event.GetInt() == 0:
    mip.state['baseYear'] = '1990'
  else:
    mip.state['baseYear'] = '2000'
    if (mip.state['startYear'] <> '') and (int(mip.state['startYear']) < 2000):
      startYear = ''
  #this tempBox keeps the box from flashing on the redraw
  mip.tempBox=wx.ComboBox(mip.basicBox, 14, mip.state['startYear'], wx.Point(189, 115), wx.Size(60, -1),[], wx.CB_READONLY)
  mip.startYearSelectBox.Destroy()  
  setStartYearList()
  mip.tempBox.Destroy()
  if not mip.initer:
    WinMod.checkForDaf()
  setScenarioYearsList()
  
def setStartYearList():
  global mip
  mip.startYearList = []
  if not mip.initer:
    tempBox=wx.ComboBox(mip.basicBox, 14, mip.state['startYear'], wx.Point(189, 115), wx.Size(60, -1),[], wx.CB_READONLY)  
    try:
      mip.startYearSelectBox.Destroy()
    except:
      time.sleep(0) #do nothing
  if mip.state['createScenario'] == 1:
    mip.state['startYear'] = str(int(mip.state['baseYear']) + 1)
    mip.startYearSelectBox=wx.ComboBox(mip.basicBox, 14, mip.state['startYear'], wx.Point(189, 115), wx.Size(60, -1),[str(int(mip.state['baseYear']) + 1)], wx.CB_READONLY)  
  else:
    timeSpan = 2040 - int(mip.state['baseYear'])
    for i in range(timeSpan):
      mip.startYearList.append(str(int(mip.state['baseYear']) + i + 1))
    mip.startYearSelectBox=wx.ComboBox(mip.basicBox, 14, mip.state['startYear'], wx.Point(189, 115), wx.Size(60, -1),mip.startYearList, wx.CB_READONLY)  
  if not mip.initer:
    tempBox.Destroy()
  try:
    mip.tempBox2=wx.ComboBox(mip.basicBox, 15, mip.state['simulationYears'], wx.Point(365, 116), wx.Size(43, -1),[], wx.CB_READONLY)   
    mip.simulationYearSelectBox.Destroy()
    setSimulationYearList()
    mip.tempBox2.Destroy()
  except:
    mip.tempBox2.Destroy()
    setSimulationYearList() 
  if not mip.initer:
    WinMod.checkForDaf()
     
def setStartYear(event):
  global mip
  mip.state['startYear'] = event.GetString()
  mip.tempBox2=wx.ComboBox(mip.basicBox, 15, mip.state['simulationYears'], wx.Point(365, 116), wx.Size(43, -1),[], wx.CB_READONLY)  
  mip.simulationYearSelectBox.Destroy()
  setSimulationYearList()
  mip.tempBox2.Destroy()
  if not mip.initer:
    WinMod.checkForDaf()
  
def setSimulationYearList():
  global mip
  simulationYearList = []
  if mip.state['startYear'] == '':
    timeSpan = 2040 - int(mip.state['baseYear']) + 1
  else:
    timeSpan = 2040 - int(mip.state['startYear']) + 1
  if (mip.state['simulationYears'] <> '') and (mip.state['startYear'] <> '') and (int(mip.state['startYear']) + int(mip.state['simulationYears']) - 1 > 2040):
    mip.state['simulationYears'] = ''
  for i in range(timeSpan):
    simulationYearList.append(str(i + 1))
  mip.simulationYearSelectBox=wx.ComboBox(mip.basicBox, 15, mip.state['simulationYears'], wx.Point(365, 116), wx.Size(43, -1),simulationYearList, wx.CB_READONLY)      
  if not mip.initer:
    WinMod.checkForDaf()
  
def setSimulationYears(event):
  global mip
  dafNotOpen = True
  if (mip.state['simulationYears'] <> '') and (int(mip.state['simulationYears']) > 1):
    dafNotOpen = False
  mip.state['simulationYears'] = event.GetString()
  if (int(mip.state['simulationYears']) > 1) and dafNotOpen:
    alert = wx.MessageDialog(mip, 'Choosing to run the model for more than one year will cause\n the full model to be run for all years except the final one.\nThus, DAF must be configured.','Note',style=wx.OK)
    response = alert.ShowModal()
    alert.Destroy()
  if not mip.initer:
    WinMod.checkForDaf()

def loadBIStatus():
  global mip
  checkCreateScenario()
  getBaseYear()
  activateScenarioYears()
  checkTextValues()