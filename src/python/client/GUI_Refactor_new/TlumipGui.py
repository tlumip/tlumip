#TlumipGui.py

import GuiComponents as Gui
import ServerConnectionClient as Server
import wx, os, time, threading
import wx.html

#Initial GUI Dimensions
FRAME_WIDTH = 1100
FRAME_HEIGHT = 800

LEFT_PANEL_COLOR = "light blue"
RIGHT_PANEL_COLOR = "khaki"

APP_SERVER_IP_ADDRESS = '167.131.72.201'
#APP_SERVER_IP_ADDRESS = '192.168.1.213'
#APP_SERVER_IP_ADDRESS = '192.168.1.221'
APP_SERVER_PORT = 8942

# this is our set of test clusters
clusters = { '1 node - Zufa' : 0, '1 node - Athena' : 1, '2 node - Athena,Zufa' : 2, '3 node - Athena,Enyo,Isis' : 3 }
CLUSTER_MENU_ITEMS = clusters.keys()
CLUSTER_MENU_ITEMS.sort()
CLUSTER_NAMES = [ [ 'Zufa' ], [ 'Athena' ], [ 'Athena', 'Zufa' ], [ 'Athena', 'Enyo', 'Isis' ] ]

clusters = { '1 node - Salem Mill' : 0, '8 - Salem Mill 8 computers': 1}
CLUSTER_MENU_ITEMS = clusters.keys()
CLUSTER_MENU_ITEMS.sort()
CLUSTER_NAMES = [ [ 'LX-SALEMMILL-1' ], [ 'LX-SALEMMILL-1', 'LX-SALEMMILL-2','LX-SALEMMILL-3','LX-SALEMMILL-4','LX-SALEMMILL-5','LX-SALEMMILL-6','LX-SALEMMILL-7','LX-SALEMMILL-8' ] ]






#determine if this is a windows box or not
windows = False
if 'OS' in os.environ:
    windows = "windows" in os.environ['OS'].lower()

#if windows:
#    SHARED = '//athena/zshare'
#else:
#    SHARED = '/zshare'


    #Extend wx.Frame class so that correct actions are performed on close
class MainFrame(Gui.MainFrame):
    def __init__(self, parent, id, title, size):
        Gui.MainFrame.__init__(self)
        self.Bind(wx.EVT_CLOSE,self.onWindowClose)

        self.serverConnection = Server.ServerConnection(APP_SERVER_IP_ADDRESS, APP_SERVER_PORT)
        self.scenarioData = SelectScenarioData(self.serverConnection)
        self.clusterData = SelectClusterData(self.serverConnection)
        self.runTargetData = SelectRunTargetData(self.serverConnection, self.scenarioData, self.clusterData)

        self.createMenuBar()
        self.createStatusBar()
        self.createLeftPanel()
        self.SetRightPanelItems()


    def createStatusBar(self):
        self.statusBar = self.getStatusBar()

    def SetRightPanelItems(self):
        list = []
        list.append(Gui.TextAreaWithButton(self, 'Selected Scenario:', self.scenarioData.getTextPanelScenarioLabels(), self.scenarioData.getTextPanelScenarioValues(), 'Create Scenario', self.onCreateNewScenario, hSpace=5, vSpace=5))
        list.append(Gui.TextArea(self, 'Selected Cluster:', self.clusterData.getTextPanelClusterLabels(), self.clusterData.getTextPanelClusterValues(), hSpace=5, vSpace=5))
        list.append(Gui.TextAreaWithButton(self, 'Selected Run Target:', self.runTargetData.getTextPanelRunTargetLabels(), self.runTargetData.getTextPanelRunTargetValues(), 'Run Target', self.onRunTarget, hSpace=5, vSpace=5))
        list[0].disableButton()
        self.setTextPanels(list)

    def createLeftPanel(self):
        self.nb = self.getNotebook()

    def updateLeftPanel(self, list):
        if len(list) == 0:
            return
        for i in range(self.nb.getNbParent().GetPageCount()):
            self.nb.getNbParent().DeletePage(i)
        for item in list:
            print item
            p = Gui.NotebookPage(self.nb.getNbParent(), 'stdout for pid=%s on %s' % (item[1], item[0]))
            self.nb.addPage(p, '%s, pid=%s' % (item[0], item[1]))
            #p.setPageTextFromFile(SHARED + r"/models/pythonSrc/tmp/%s/stdout.txt" % item[0])
            p.setPageText(self.serverConnection.getStartModelRunStdOut(item[0]))

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
        menuBar = self.getMenuBar()

        self.scenarioMenu = wx.Menu()
        self.runMenu = wx.Menu()
        self.clusterMenu = wx.Menu()

        newScenario = self.scenarioMenu.Append(-1, "&New...")
        self.Bind(wx.EVT_MENU, self.onScenarioNew, newScenario)

        #a list of scenarios is retrieved from AOServer
        self.createOpenSubmenu()

        self.createClusterMenu()
        
        #a list of ant targets is retrieved from AOServer
        self.createRunMenu()

        menuBar.Append(self.scenarioMenu,"&Scenario")
        menuBar.Append(self.clusterMenu,"&Cluster")
        menuBar.Append(self.runMenu,"&Run")
        self.SetMenuBar(menuBar)

        menuBar.Bind(wx.EVT_MENU_CLOSE, self.onMenuClose)

    def createOpenSubmenu(self):
        self.openSubmenu = wx.Menu()
        scenarioNames = self.scenarioData.getExistingScenarioNames()
        scenarioNames.sort()
        for scenario in scenarioNames:
            item = self.openSubmenu.Append(-1, scenario)
            self.Bind(wx.EVT_MENU, self.onOpenSubmenuItem, item)
            self.Bind(wx.EVT_MENU_HIGHLIGHT, self.onOpenSubmenuMouseOver, item)
        self.Bind(wx.EVT_MENU_CLOSE, self.onMenuClose)
        self.scenarioMenu.AppendMenu(201, "&Open", self.openSubmenu)

    def destroyOpenSubmenu(self):
        self.scenarioMenu.Remove(201)

    def createRunMenu(self):
        for target in self.runTargetData.getRunTargetNames():
            item = self.runMenu.Append(-1, target)
            self.Bind(wx.EVT_MENU, self.onRunMenuItem, item)
            self.Bind(wx.EVT_MENU_HIGHLIGHT, self.onRunMenuMouseOver, item)

    def createClusterMenu(self):
        for name in self.clusterData.getClusterMenuItems():
            item = self.clusterMenu.Append(-1, name)
            self.Bind(wx.EVT_MENU, self.onClusterMenuItem, item)
            self.Bind(wx.EVT_MENU_HIGHLIGHT, self.onClusterMenuMouseOver, item)

    def onMenuClose(self,event):
        self.clearStatusBar([''])



    def onScenarioNew(self, event):
        self.scenarioNewFrame = SelectScenarioFrame(self, self.scenarioData)
        self.scenarioNewFrame.Show()
        self.disableFrame()

    def onOpenSubmenuItem(self, event):
        item = self.openSubmenu.FindItemById(event.GetId())
        itemText = item.GetText()
        panels = self.getTextPanels()
        panels[0].disableButton()
        self.scenarioData.setSelectedScenario(itemText)
        self.setScenarioPanelText()

    def onRunMenuItem(self,event):
        item = self.runMenu.FindItemById(event.GetId())
        itemText = item.GetText()
        self.runTargetData.setSelectedRunTarget(itemText)
        self.runTargetData.setTargetYear(self.runTargetData.getDefaultTargetYear())
        if self.runTargetData.getTRequiredValue():
            self.disableFrame()
            yearFrame = TargetYearPopup(self, self.runTargetData)
            yearFrame.Show()
        self.setRunTargetPanelText()


    def onClusterMenuItem(self,event):
        item = self.clusterMenu.FindItemById(event.GetId())
        itemText = item.GetText()
        print itemText
        self.clusterData.setSelectedClusterItem(itemText)
        self.setClusterPanelText()

    def onClusterNew(self, event):
        self.clusterNewFrame = SelectClusterFrame(self, self.clusterData)
        self.clusterNewFrame.Show()
        self.disableFrame()



    def onOpenSubmenuMouseOver(self,event):
        item = self.openSubmenu.FindItemById(event.GetId())
        itemText = item.GetText()
        self.setStatusBarScenarioValues(itemText)

    def onRunMenuMouseOver(self,event):
        item = self.runMenu.FindItemById(event.GetId())
        itemText = item.GetText()
        self.setStatusBarAntInfo(itemText)

    def onClusterMenuMouseOver(self,event):
        item = self.clusterMenu.FindItemById(event.GetId())
        itemText = item.GetText()
        self.setStatusBarClusterValues(itemText)






    def setStatusBarScenarioValues(self,scenarioName):
        labels = self.scenarioData.getStatusBarScenarioLabels()
        values = self.scenarioData.getStatusBarScenarioValues(scenarioName)
        self.populateStatusBar(labels, values, [-1,-1,-1,-1,-2,-3])

    def setStatusBarAntInfo(self,targetName):
        labels = self.runTargetData.getStatusBarRunTargetLabels()
        values = self.runTargetData.getStatusBarRunTargetValues(targetName)
        self.populateStatusBar(labels, values, [-1,-2,-3])

    def setStatusBarClusterValues(self,itemText):
        labels = self.clusterData.getStatusBarClusterLabels()
        values = self.clusterData.getStatusBarClusterValues(itemText)
        self.populateStatusBar(labels, values, [-1,-4])

    def setScenarioPanelText(self):
        panels = self.getTextPanels()
        labels = self.scenarioData.getTextPanelScenarioLabels()
        values = self.scenarioData.getTextPanelScenarioValues()
        for i, label in enumerate(labels):
            panels[0].replaceTextItem(label, values[i])

    def setClusterPanelText(self):
        panels = self.getTextPanels()
        labels = self.clusterData.getTextPanelClusterLabels()
        values = self.clusterData.getTextPanelClusterValues()
        panels[1].createTextArea(labels, values)
        self.setTextPanels(panels)

    def setRunTargetPanelText(self):
        panels = self.getTextPanels()
        labels = self.runTargetData.getTextPanelRunTargetLabels()
        values = self.runTargetData.getTextPanelRunTargetValues()
        for i, label in enumerate(labels):
            panels[2].replaceTextItem(label, values[i])



    def onRunTarget(self,event):
        machines = self.clusterData.getSelectedMachines()
        print machines
        if len(machines) == 0:
            wx.MessageBox ("Please define a cluster with at least one machine.", "No Cluster Defined", wx.OK)
            return

        targetName = self.runTargetData.getSelectedRunTargetName()
        # scenario, baseScenario, baseYear, interval
        parameters = self.runTargetData.getSelectedRunTargetParameters()

        result = self.serverConnection.startModelRun(targetName, parameters, machines)
        print 'result of onRunMenuItem() for %s: ' % targetName, result
        self.updateLeftPanel(result)


    def onCreateNewScenario(self,event):
        #get list of scenario values: [scenarioName, baseYear, scenarioYears, userName, scenarioCreationTime, description]
        values = self.scenarioData.getSelectedScenarioValues()
        panels = self.getTextPanels()
        panels[0].disableButton()
        #result = self.serverConnection.tempCreateScenario(values[0],values[2],values[1],values[3],values[5])
        result = self.serverConnection.createScenario(values[0],values[2],values[1],values[3],values[5])
        print result
        waitThread = threading.Thread(target=lambda:self.waitForScenarioReady())
        waitThread.start()

    def waitForScenarioReady(self):
        while self.serverConnection.isScenarioReady(self.scenarioData.getSelectedScenarioName()) == "Scenario Not Ready":
            time.sleep(1)
        self.scenarioData.updateExistingScenarioProperties()
        self.destroyOpenSubmenu()
        self.createOpenSubmenu()

    def enableFrame(self):
        self.Enable(True)

    def disableFrame(self):
        self.Enable(False)

    def onWindowClose(self, event):
        os._exit(0)





        #Data object to hold data related to ant run target selection
class SelectRunTargetData(object):
    def __init__(self, serverConnection, scenarioData, clusterData):
        self.selectedRunTarget = None
        self.defaultTargetYear = 1
        self.targetYear = self.defaultTargetYear
        self.scenarioData = scenarioData
        self.clusterData = clusterData
        # get list of [name, descr, reqd args[]] for runTarget
        self.runTargetsTable = serverConnection.getAvailableRunTargets()
        self.setStatusBarRunTargetLabels()
        self.setTextPanelRunTargetLabels()
        

    def setTargetYear(self, targetYear):
        self.targetYear = targetYear

    def getTargetYear(self):
        return self.targetYear

    def getDefaultTargetYear(self):
        return self.defaultTargetYear

    def setStatusBarRunTargetLabels(self):
        self.statusBarLabels = []
        self.statusBarLabels.append('antTarget')
        self.statusBarLabels.append('antReqdArgs')
        self.statusBarLabels.append('antDescription')

    def getStatusBarRunTargetLabels(self):
        return self.statusBarLabels

    def getStatusBarRunTargetValues(self, runTarget):
        values = []
        index = self.getRunTargetsTableIndex(runTarget)
        target = self.runTargetsTable[index]
        values.append(target[0])
        argsString = 'None'
        if len(target[2]) > 0:
            argsString = target[2][0]
            for x in target[2][1:]:
                argsString += ', %s' % x
        values.append(argsString)
        values.append(target[1])
        return values

    def setTextPanelRunTargetLabels(self):
        self.textPanelLabels = []
        self.textPanelLabels.append('antTarget')
        self.textPanelLabels.append('antReqdArgs')
        self.textPanelLabels.append('antDescription')

    def getTextPanelRunTargetLabels(self):
        return self.textPanelLabels

    def getTRequiredValue(self):
        tRequired = False
        index = self.getRunTargetsTableIndex(self.selectedRunTarget)
        target = self.runTargetsTable[index]
        print target
        if target[2].count('t') > 0:
            tRequired = True
        return tRequired

    def getTextPanelRunTargetValues(self):
        values = []
        if self.selectedRunTarget == None:
            for i in range(len(self.textPanelLabels)):
                values.append('')
        else:
            index = self.getRunTargetsTableIndex(self.selectedRunTarget)
            target = self.runTargetsTable[index]
            values.append(target[0])
            
            argsString = 'None'
            if len(target[2]) > 0:
                dataItem = target[2][0]
                dataValue = self.scenarioData.getSelectedScenarioData(dataItem)
                argsItem = '%s=%s' % (dataItem, dataValue)
                argsString = argsItem
                for x in target[2][1:-1]:
                    dataItem = x
                    dataValue = self.scenarioData.getSelectedScenarioData(dataItem)
                    argsItem = '%s=%s' % (dataItem, dataValue)
                    argsString += ', %s' % argsItem
                dataItem = 't'
                dataValue = self.targetYear
                argsItem = '%s=%s' % (dataItem, dataValue)
                argsString += ', %s' % argsItem
                dataItem = 'machines'
                dataValue = self.clusterData.getSelectedMachines()
                argsItem = '%s=%s' % (dataItem, dataValue)
                argsString += ', %s' % argsItem
                dataItem = 'machines'
            values.append(argsString)
            values.append(target[1])
        return values

    def getRunTargetNames(self):
        targetNames = []
        for target in self.runTargetsTable:
            targetNames.append(target[0])
        return targetNames

    def setSelectedRunTarget(self, runTarget):
        self.selectedRunTarget = runTarget

    def getRunTargetsTableIndex(self, runTarget):
        index = -1
        for i, target in enumerate(self.runTargetsTable):
            if target[0] == runTarget:
                index = i
                break
        return i

    def getSelectedRunTargetName(self):
        return self.selectedRunTarget

    def getSelectedRunTargetArgs(self):
        index = self.getRunTargetsTableIndex(self.selectedRunTarget)
        target = self.runTargetsTable[index]
        return target[2]

    def getSelectedRunTargetParameters(self):
        params = {}
        parameterNames = self.getSelectedRunTargetArgs()
        for name in parameterNames:
            if name == 't':
                if self.getTRequiredValue():
                    params['t'] = self.targetYear
            else:
                params[name] = self.scenarioData.getSelectedScenarioData(name)
        return params
        

#Data object to hold data related to scenario definition/selection
class SelectScenarioData(object):
    def __init__(self, serverConnection):
        self.selectedScenario = None
        self.baseScenarioName = '90_Base'
        self.serverConnection = serverConnection
        self.setStatusBarScenarioLabels()
        self.setTextPanelScenarioLabels()
        self.updateExistingScenarioProperties()


    def updateExistingScenarioProperties(self):
        self.existingScenarioProperties = self.serverConnection.getExistingScenarioProperties()
        self.existingScenarioNames = self.existingScenarioProperties.keys()

    def getExistingScenarioNames(self):
        return self.existingScenarioNames

    def setStatusBarScenarioLabels(self):
        self.statusBarLabels = []
        self.statusBarLabels.append('Name')
        self.statusBarLabels.append('baseYear')
        self.statusBarLabels.append('scenarioYears')
        self.statusBarLabels.append('User')
        self.statusBarLabels.append('Created')
        self.statusBarLabels.append('Description')

    def getStatusBarScenarioLabels(self):
        return self.statusBarLabels

    def getStatusBarScenarioValues(self, scenarioName):
        scenarioProperties = self.existingScenarioProperties[scenarioName]
        values = []
        values.append(scenarioName)
        values.append(scenarioProperties['baseYear'])
        values.append(scenarioProperties['scenarioYears'])
        values.append(scenarioProperties['userName'])
        values.append(scenarioProperties['scenarioCreationTime'])
        values.append(scenarioProperties['scenarioDescription'])
        return values

    def setTextPanelScenarioLabels(self):
        self.textPanelLabels = []
        self.textPanelLabels.append('Scenario Name:')
        self.textPanelLabels.append('Base Scenario Name:')
        self.textPanelLabels.append('Base Year:')
        self.textPanelLabels.append('Scenario Years:')
        self.textPanelLabels.append('User Name:')
        self.textPanelLabels.append('Created at:')
        self.textPanelLabels.append('Description:')

    def getTextPanelScenarioLabels(self):
        return self.textPanelLabels

    def getTextPanelScenarioValues(self):
        values = []
        if self.selectedScenario == None:
            for i in range(len(self.textPanelLabels)):
                values.append('')
        else:
            scenarioProperties = self.existingScenarioProperties[self.selectedScenario]
            values.append(self.selectedScenario)
            values.append(self.baseScenarioName)
            values.append(scenarioProperties['baseYear'])
            values.append(scenarioProperties['scenarioYears'])
            values.append(scenarioProperties['userName'])
            values.append(scenarioProperties['scenarioCreationTime'])
            values.append(scenarioProperties['scenarioDescription'])
        return values

    def setSelectedScenario(self, openedScenarioName):
        self.selectedScenario = openedScenarioName

    def setNewScenarioValues(self, newValues):
        self.selectedScenario = newValues[0]
        scenarioProperties = {}
        scenarioProperties['baseYear'] = newValues[1]
        scenarioProperties['scenarioYears'] = newValues[2]
        scenarioProperties['userName'] = newValues[3]
        scenarioProperties['scenarioCreationTime'] = newValues[4]
        scenarioProperties['scenarioDescription'] = newValues[5]
        self.existingScenarioProperties[self.selectedScenario] = scenarioProperties

    def getSelectedScenarioValues(self):
        scenarioProperties = self.existingScenarioProperties[self.selectedScenario]
        values = []
        values.append(self.selectedScenario)
        values.append(scenarioProperties['baseYear'])
        values.append(scenarioProperties['scenarioYears'])
        values.append(scenarioProperties['userName'])
        values.append(scenarioProperties['scenarioCreationTime'])
        values.append(scenarioProperties['scenarioDescription'])
        return values

    def getSelectedScenarioName(self):
        return self.selectedScenario

    def getSelectedScenarioData(self, dataItem):
        if dataItem == 'scenarioName':
            return self.selectedScenario
        elif dataItem == 'baseScenarioName':
            return self.baseScenarioName
        else:
            scenarioProperties = self.existingScenarioProperties[self.selectedScenario]
            return scenarioProperties[dataItem]




# popup box to get information about new scenario to be defined, and allow that new scenario to be the selected one.
class SelectScenarioFrame(wx.Frame):
    def __init__(self, callingFrame, scenarioData):
        wx.Frame.__init__(self, None, -1, "Create New Scenario",size = (370,305))

        self.callingFrame = callingFrame
        self.scenarioData = scenarioData

        panel = wx.Panel(self, -1)

        #Scenario name text box
        self.scenarioName = ''
        wx.StaticText(panel, -1, 'Scenario Name', wx.Point(12,23))
        id = wx.NewId()
        self.scenarioNameTextBox = wx.TextCtrl(panel, id, self.scenarioName, wx.Point(88, 20), wx.Size(225,-1))
        wx.EVT_TEXT(panel, id, self.onScenarioName)

        #User name text box
        self.userName = os.environ['USERNAME']
        wx.StaticText(panel, -1, 'User Name', wx.Point(12,63))
        id = wx.NewId()
        self.scenarioNameTextBox = wx.TextCtrl(panel, id, self.userName, wx.Point(88, 60), wx.Size(85,-1))
        wx.EVT_TEXT(panel, id, self.onUserName)

        #Base year radio button
        self.radioBoxItems = ['1990','2000']
        self.baseYear = self.radioBoxItems[0]
        id = wx.NewId()
        self.rb = wx.RadioBox(panel, id, "Base Year", wx.Point(10,95), wx.DefaultSize, self.radioBoxItems, 2, wx.RA_SPECIFY_COLS)
        wx.EVT_RADIOBOX(panel, id, self.onBaseYear)

        #Scenario years text
        initialValue = 1
        self.scenarioYears = str(initialValue)
        wx.StaticText(panel, -1, 'Scenario Years', wx.Point(200,95))
        self.sc = wx.SpinCtrl(panel, id, '', wx.Point(200,114), wx.Size(50,-1), )
        self.sc.SetRange(1,31)
        self.sc.SetValue(initialValue)
        id = wx.NewId()
        wx.EVT_SPINCTRL(panel,id,self.onScenarioYears)
        wx.EVT_TEXT(self, id, self.onScenarioYears)

        #Scenario description text
        wx.StaticText(panel, -1, 'Scenario Description', wx.Point(12,155))
        self.scenarioDescription = ''
        id = wx.NewId()
        self.scenarioDescriptionTextBox = wx.TextCtrl(panel, id, self.scenarioDescription, wx.Point(12,170), wx.Size(335,60), style = wx.TE_MULTILINE)
        wx.EVT_TEXT(panel, id, self.onScenarioDescription)


        #Select scenario button
        id = wx.NewId()
        self.selectScenarioButton = wx.Button(panel, id, "Select Scenario", wx.Point(195, 240), wx.Size(150,25))
        wx.EVT_BUTTON(self, id, self.onSelectScenario)

        #Cancel button
        id = wx.NewId()
        self.cancelButton = wx.Button(panel, id, " Cancel ", wx.Point(10, 240), wx.Size(150,25))
        wx.EVT_BUTTON(self, id, self.onCancel)

    def onScenarioName(self,event):
        self.scenarioName = event.GetString()

    def onUserName(self,event):
        self.userName = event.GetString()

    def onBaseYear(self,event):
        self.baseYear = self.radioBoxItems[event.GetInt()]

    def onScenarioYears(self,event):
        value = self.sc.GetValue()
        self.scenarioYears = str(value)
        self.sc.SetValue(value)

    def onScenarioDescription(self,event):
        self.scenarioDescription = event.GetString()

    def onSelectScenario(self,event):
        newValues = []
        newValues.append(self.scenarioName)
        newValues.append(self.baseYear)
        newValues.append(self.scenarioYears)
        newValues.append(self.userName)
        scenarioCreationTime = ''
        newValues.append(scenarioCreationTime)
        newValues.append(self.scenarioDescription)

        self.scenarioData.setNewScenarioValues(newValues)
        self.callingFrame.setScenarioPanelText()
        self.callingFrame.destroyOpenSubmenu()
        self.callingFrame.createOpenSubmenu()
        panels = self.callingFrame.getTextPanels()
        panels[0].enableButton()
        self.callingFrame.enableFrame()
        self.Destroy()

    def onCancel(self,event):
        self.callingFrame.enableFrame()
        self.Destroy()

# popup box to get information about target to be run.
class TargetYearPopup(wx.Frame):
    def __init__(self, parent, targetData):
        wx.Frame.__init__(self, parent, -1, "Set Target Year", style=wx.DEFAULT_FRAME_STYLE | wx.FRAME_TOOL_WINDOW | wx.FRAME_FLOAT_ON_PARENT, pos=(parent.GetPositionTuple()[0]+300,parent.GetPositionTuple()[1]+100))
        self.Bind(wx.EVT_CLOSE, self.onClose)

        self.parent = parent 
        self.targetData = targetData

        self.defaultValue = self.targetData.getDefaultTargetYear()
        self.targetYear = self.defaultValue
        self.spinValue = self.defaultValue
         
        panel = wx.Panel(self, -1)
        panel.SetBackgroundColour(LEFT_PANEL_COLOR)

        # text label 
        label = wx.StaticText(panel, -1, 'Target Year:')
 
        #spin control
        id = wx.NewId()
        self.sc = wx.SpinCtrl(panel, id, '', size=(50,20))
        self.sc.SetRange(1,30)
        self.sc.SetValue(self.defaultValue)
        wx.EVT_SPINCTRL(panel, id, self.onSetValue)
        wx.EVT_TEXT(panel, id, self.onSetValue)

        #OK button
        id = wx.NewId()
        self.okButton = wx.Button(panel, id, "Ok", size=(45,20))
        wx.EVT_BUTTON(self, id, self.onOk)
 
        #Cancel button
        id = wx.NewId()
        self.cancelButton = wx.Button(panel, id, "Cancel", size=(55,20))
        wx.EVT_BUTTON(self, id, self.onCancel)
 
        #Sizer
        sizer = wx.GridSizer(rows=2, cols=2)
        sizer.Add(label, 0, wx.ALL, 10)
        sizer.Add(self.sc, 0, wx.ALL, 8)
        sizer.Add(self.okButton, 0, wx.ALL, 4)
        sizer.Add(self.cancelButton, 0, wx.ALL, 4)
        panel.SetSizer(sizer)
        sizer.Fit(self)
 
    def onSetValue(self,event):
        value = self.sc.GetValue()
        self.spinValue = value
        self.sc.SetValue(value)

    def onOk(self,event):
        self.targetData.setTargetYear(str(self.spinValue))
        self.onClose(event)

    def onCancel(self,event):
        self.targetData.setTargetYear(str(self.defaultValue))
        self.onClose(event)

    def onClose(self,event):
        self.parent.enableFrame()
        self.parent.setRunTargetPanelText()
        self.Destroy()


#Data object to hold data related to cluster selection
class SelectClusterData(object):
    def __init__(self, serverConnection):
        self.serverConnection = serverConnection
        self.selectedMachineList = []
        self.setStatusBarClusterLabels()
        self.setTextPanelClusterLabels()
        #self.updateAvavailableMachines()


    def getClusterMenuItems(self):
        return CLUSTER_MENU_ITEMS
    
    def setSelectedClusterItem(self, itemText):
        menuIndex = self.getClusterMenuItemIndex(itemText)
        clusterIndex = clusters[itemText]
        self.selectedMachineList = CLUSTER_NAMES[clusterIndex]
        self.setTextPanelClusterLabels()
    
    
    def updateAvavailableMachines(self):
        self.clusterDicts = self.serverConnection.getAvailableMachines()

    def setStatusBarClusterLabels(self):
        self.statusBarLabels = []
        self.statusBarLabels.append('Cluster')
        self.statusBarLabels.append('Description')

    def getStatusBarClusterLabels(self):
        return self.statusBarLabels

    def getStatusBarClusterValues(self, itemText):
        values = []
        index = self.getClusterMenuItemIndex(itemText)
        names = CLUSTER_NAMES[index]
        values.append(itemText)
        namesString = names[0]
        for x in names[1:]:
            namesString += ', %s' % x
        values.append(namesString)
        return values

    def getClusterMenuItemIndex(self, itemText):
        index = None
        for i, item in enumerate(CLUSTER_MENU_ITEMS):
            if item == itemText:
                index = i
                break
        if index is None:
            print 'error getting cluster menu item index for creating status bar values'
        else:
            return index        

    def setTextPanelClusterLabels(self):
        self.textPanelLabels = []
        self.textPanelValues = []
        for i, m in enumerate(self.selectedMachineList):
            self.textPanelLabels.append('%d:' % (i))
            self.textPanelValues.append(m)

    def getTextPanelClusterLabels(self):
        return self.textPanelLabels

    def getTextPanelClusterValues(self):
        return self.textPanelValues

    def getAllMachineNames(self):
        self.updateAvavailableMachines()
        values = []
        for item in self.clusterDicts:
            values.append(item['NAME'])
        return values

    def getMachineDetails(self, name):
        values = []
        for item in self.clusterDicts:
            if name == item['NAME']:
                values.append(name)
                values.append(item['IP'])
                values.append(item['STATUS'])
                values.append(item['PROCESSORS'])
                values.append(item['RAM'] + 'GB')
                values.append(item['OS'])
                values.append(item['DESCRIPTION'])
                break
        return values

    def getMachineStatus(self, name):
        status = None
        for item in self.clusterDicts:
            if name == item['NAME']:
                status = item['STATUS']
                break
        return status

    def setSelectedMachines(self, machines):
        self.selectedMachineList = machines
        self.setTextPanelClusterLabels()

    def getSelectedMachines(self):
        return self.selectedMachineList




#Create cluster definition popup box
class SelectClusterFrame(wx.Frame):
    def __init__(self, callingFrame, clusterData):
        wx.Frame.__init__(self, callingFrame, -1, "Select Machines for Cluster", size=(300,400))

        self.callingFrame = callingFrame
        self.clusterData = clusterData

        panel = wx.Panel(self, -1)

        #Machine list check boxes
        xOffset = 30
        yOffset = 30

        self.panelItems = []
        wx.StaticText(panel, -1, 'Check available machines to include in cluster', wx.Point(xOffset,yOffset))

        #Create checkboxes for machines and set their availability to be checked
        for name in self.clusterData.getAllMachineNames():
            avail = False
            if self.clusterData.getMachineStatus(name) == "Available":
                avail = True

            yOffset = yOffset + 20
            p = wx.CheckBox(panel, -1, name, wx.Point(xOffset,yOffset), wx.Size(75,-1))
            p.Enable(avail)
            self.panelItems.append((name,p))

            #Create scenario button
        id = wx.NewId()
        self.selectClusterButton = wx.Button(panel, id, "Select Cluster", wx.Point(150, 300), wx.Size(100,25))
        wx.EVT_BUTTON(self, id, self.onSelect)

        #Cancel button
        id = wx.NewId()
        self.cancelButton = wx.Button(panel, id, "Cancel", wx.Point(35, 300), wx.Size(80,25))
        wx.EVT_BUTTON(self, id, self.onCancel)

    def onSelect(self, event):
        checked = False
        enabled = False
        checkedQualifier = 'not'
        enabledQualifier = 'not'
        result = []
        if self.panelItems == None or len(self.panelItems) == 0:
            print 'no machines in list'
        else:
            for i, tuple in enumerate(self.panelItems):
                selectedMachine = tuple[0]
                p = tuple[1]
                if p.IsEnabled():
                    enabled = True
                    enabledQualifier = '\b'
                    if p.IsChecked():
                        checked = True
                        checkedQualifier = '\b'
                        result.append(selectedMachine)
                    else:
                        checkedQualifier = 'not'
                else:
                    enabledQualifier = 'not'
                print '%s is %s enabled and is %s checked' % (selectedMachine, enabledQualifier, checkedQualifier)
        self.clusterData.setSelectedMachines(result)
        self.clusterData.setTextPanelClusterLabels()

        initialMachineList = []
        for m in self.clusterData.getSelectedMachines():
            initialMachineList.append((m, 'no processes'))
        self.callingFrame.setClusterPanelText()
        self.callingFrame.enableFrame()
        self.Destroy()

    def onCancel(self, event):
        self.callingFrame.enableFrame()
        self.Destroy()



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

    def setPageText(self,text):
        self.pageTextArea.WriteText(text)

    def setPageTextFromFile(self, file):
        f = open(file)
        try:
            for line in f:
                self.pageTextArea.WriteText(line)
        finally:
            f.close()




app = wx.PySimpleApp()
frame = MainFrame(None,-1," TLUMIP Model Runner", (FRAME_WIDTH,FRAME_HEIGHT))
frame.Show(1)
app.MainLoop()