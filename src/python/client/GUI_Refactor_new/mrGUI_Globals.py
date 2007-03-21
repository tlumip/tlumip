#mrGUI_Globals.py

#Ports
global XMLRPCResultPort
aoXMLRPCSendPort = 8942
aoXMLRPCResultPort = 8943


#Standard out and standard error file locations
global stdOutFile,stdErrFile
stdOutFile = '.\\Text\\stdoutMain.txt'
stdErrFile = '.\\Text\\stderrMain.txt'

#Config file location and name:
global configFile,stateBackup
configFile = '.\\Config\\TLUMIP_GUI_Config.tcn'
stateBackup = '.\\Text\\TLUMIPModelRunnerBackup.txt'
reconFileDirectory = '.\\Config'
reconTemplate = reconFileDirectory + '\\TLUMIP_GUI_Config_Template.txt'

#ANT text file location - used for reading ANT logs
global antTextFile,tempAntFile
antTextFile = '.\\Text\\antText.txt'
tempAntFile = '.\\Text\\tempAntText.txt'

#Log file locations
global mainLog
mainLogFile = '.\\Text\\@SCENARIO_NAME@_MainLog.log'

#Reconfigure text check file - used to see what happened with the reconfigure window
global reconfigureCheckFile
reconfigureCheckFile = 'reconfigure_check.txt'

#code paths
global commonBaseClasspath,log4jClasspath
commonBaseClasspath = '.\\Java\\common-base.jar'
log4jClasspath = '.\\Java\\log4j-1.2.9.jar'

#The configuration "checks," or the items that the configuration file will define
# A dictionary is used as such: 
#  0:[the line splitter to create a dictionary list] 
#  1:[the dictionary which the split list will map from]
global configChecks
configChecks = {'modules':{0:' '},
                'nonDAFModules':{0:' '},
                'dafModules':{0:' '},
                'spatialNonDafModules':{0:' '},
                'spatialDafModules':{0:' '},
                'transportNonDafModules':{0:' '},
                'transportDafModules':{0:' '},
                'computerNames':{0:' '},
                'computerIPList':{0:' ',1:'computerNames'},
                'baseComputer':'',
                'commonBaseClasspath':'',
                'log4jClasspath':''
                }

#The state items of the GUI in a dictionary form: the mapped value of each key is their default/"clean state" value; if
# the item is a dictionary, then it is initialized with string keys from the corresponding dictionary in the configChecks
# dictionary above
global guiStateItems
guiStateItems = {'rootDir':'',
                 'scenarioName':'',
                 'createScenario':0,
                 'baseYear':'',
                 'startYear':'',
                 'simulationYears':'',
                 'scenarioYears':'',
                 'nonDAFChecks':{0:'nonDAFModules'},
                 'dafChecks':{0:'dafModules'},
                 'spatialCheck':0,
                 'transportCheck':0,
                 'allCheck':0,
                 'computerChecks':{0:'computerNames'}
                 }              


