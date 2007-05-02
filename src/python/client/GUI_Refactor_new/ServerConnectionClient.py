from xmlrpclib import ServerProxy
import sys, time, os, threading, subprocess


class ServerConnection(object):
    def __init__(self, ipAddress, port):
        try:
            serverConnection = "http://" + ipAddress + ":" + str(port)
            print "connecting to", serverConnection
            self.server = ServerProxy(serverConnection)
            result = self.server.checkConnection()
            print "Checking connection to ApplicationOrchestratorServer:", result
            
        except Exception, v:
            print "ERROR", v
        
    def getExistingScenarioProperties(self):
        result = self.server.getExistingScenarioProperties()
        return result
        
    def tempCreateScenario(self, scenarioName, numYears, baseYear, userName, description):
        result = self.server.tempCreateScenario(scenarioName, numYears, baseYear, userName, description)
        return result

    def createScenario(self, scenarioName, numYears, baseYear, userName, description):
        result = self.server.createScenario(scenarioName, numYears, baseYear, userName, description)
        return result
        
    def isScenarioReady(self, scenarioName):
        result = self.server.isScenarioReady(scenarioName)
        return result
        
    def getAvailableRunTargets(self):
        # get list of [name, descr, reqd args[]] for ant targets
        result = self.server.getAvailableAntTargets()
        return result
        
    def getAvailableMachines(self):
        # get a list of dicts with { NAME, IP, PROCESSORS, RAM, OS, DESCRIPTION, STATUS } properties for each machine
        # named in ClusterMachines.txt on server 
        result = self.server.getAvailableMachines()
        return result
        
    def startModelRun(self, target, scenario, baseScenario, baseYear, interval, machineList):
        result = self.server.startModelRun(target, scenario, baseScenario, baseYear, interval, machineList)
        return result
        
    def startModelRun(self, target, parameters, machineList):
        result = self.server.startModelRun(target, parameters, machineList)
        return result
        
    def testVersions(self):
        result = self.server.startModelRun("runVersions", {}, ["Zufa"])
        print str(result)

        
"""        
c = ServerConnection('192.168.1.141', 8942)
list = c.getAvailableMachines()
for i, item in enumerate(list):
    print "%d: %s" % (i, item)
"""
   
    
"""
list = c.getAvailableAntTargets()
for i, item in enumerate(list):
    print "%d: %s" % (i, item)
    
print c.getExistingScenarioProperties()
print c.tempCreateScenario('Jim Scenario','30','1990','Jim','my test scenario')
"""


"""
c = ServerConnection('192.168.1.141', 8942)
c.testVersions()
"""




"""
# first create a new scenario for testing
c = ServerConnection('192.168.1.141', 8942)
c.createScenario('JimTestScenario','2','1990','Jim','scenario for testing gui, client, and server componenets.')
"""

"""
c = ServerConnection('192.168.1.141', 8942)
target = 'runED'
scenario = 'JimTestScenario'
baseScenario = '90_Base'
baseYear = 1990
interval = 1
machineList = ['Athena']
c.startModelRun(target, scenario, baseScenario, baseYear, interval, machineList)
"""

"""
c = ServerConnection('192.168.1.141', 8942)
target = 'runWebServer'
parameterList = ['JimTestScenario']
machineList = ['Athena']
result = c.startModelRun(target, parameterList, machineList)
print 'result =', result
"""
