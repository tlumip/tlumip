#MRGUI_Configuration.py
import mrGUI_Globals as Globs
import os,string,copy


"""
This method loads the GUI configuration (which is what computer names, modules, etc. the GUI will display; this does not
include information about what items have been checked or whatnot.  See the loadState method for that. This returns a 
dictionary containing the configuration of the GUI.
"""
def loadGUIConfig():
  ######Load configuration info
  configFile = Globs.configFile
  configChecks = Globs.configChecks
  guiConfig = {}
  configKey = ''
  if os.path.exists(configFile):
    checker = False
    for line in file(configFile):
      if checker:
        if type(configChecks[configKey]) == type({}):
          if configChecks[configKey].has_key(0):
            if configChecks[configKey].has_key(1):
              if guiConfig.has_key(configChecks[configKey][1]):
                if type(configChecks[configChecks[configKey][1]]) == type({}):
                  lineArray = line.strip().split(configChecks[configKey][0])
                  if len(lineArray) == len(guiConfig[configChecks[configKey][1]]):
                    guiConfig[configKey] = {}
                    counter = 0
                    for name in guiConfig[configChecks[configKey][1] + 'Ord']:
                      guiConfig[configKey][name] = lineArray[counter]
                      counter = counter + 1
                  else:
                    print 'Line corresponding to ' + configKey + ' does not have an equal number of elements to ' + configChecks[configKey][1] + '!'
                else:
                  print 'Key ' + configChecks[configKey][1] + ' should map to a dictionary!'
              else:
                print 'Key ' + configChecks[configKey][1] + ' for ' + configKey + ' not found!'
            else:
              guiConfig[configKey] = {}
              guiConfig[configKey + 'Ord'] = []
              for name in line.strip().split(configChecks[configKey][0]):
                guiConfig[configKey][name] = None
                guiConfig[configKey + 'Ord'].append(name)
          else:
            print 'Missing splitter string for ' + configKey + '!'
        else:
          guiConfig[configKey] = line.strip()
        checker = False
      else:
        firstWord = line.split()[0]
        if configChecks.has_key(firstWord):
          configKey = firstWord
          checker = True
  return guiConfig


"""
This method generates the blank dictionary which will be used to store the GUI's state
"""
def generateBlankState(guiConfig):
  guiState = copy.deepcopy(Globs.guiStateItems)
  for name in guiState:
    if type(guiState[name]) == type({}):
      keyName = guiState[name].pop(0)
      if guiConfig.has_key(keyName):
        if type(guiConfig[keyName]) == type({}):
          for key in guiConfig[keyName]:
            guiState[name][key] = 0
        else:
          print 'Key ' + guiState[name][0] + ' should point to a dictionary!'
      else:
        print 'Key ' + keyName + ' not found in GUI configuration settings!'
  guiState['blank'] = True
  return guiState


"""
This method loads the state of the GUI - which means what modules have been selected, what computers have been picked (if in 
DAF mode) etc - from a file.  It returns a dictionary containing the state of the GUI.
"""
def loadGUIState(savedFile,guiConfig):
  guiState = generateBlankState(guiConfig)
  if os.path.exists(savedFile):
    for line in file(savedFile):
      key, val = line.split(':')
      val = val.strip()
      if guiState.has_key(key):
        if type(guiState[key]) == type(0):
          guiState[key] = int(val)
        else:
          guiState[key] = val
      else:
        for k in guiState:
          if type(guiState[k]) == type({}):
            if key.find(k) > -1:
              if guiState[k].has_key(key.replace(k,'')):
                #try to cast to an int
                try:
                  guiState[k][key.replace(k,'')] = int(val)
                except:
                  guiState[k][key.replace(k,'')] = val
              else:
                #something not matching previous saved state: blank it all
                return generateBlankState(guiConfig)
  guiState['blank'] = False
  return guiState
    
"""
Thes method saves the state of the GUI in a text file that can be read back and loaded using the loadState method
"""
def backupGUIState(saveFile,guiState):
  backupText = ''
  for name in guiState:
    if type(guiState[name]) == type({}):
      for key in guiState[name]:
        backupText = backupText + name + key + ':' + str(guiState[name][key]) + '\n'
    else:
      backupText = backupText + name + ':' + str(guiState[name]) + '\n'
  backup = file(saveFile, 'w')
  backup.write(backupText)
  backup.close()

