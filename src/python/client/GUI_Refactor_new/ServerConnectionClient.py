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
        
    def isScenarioReady(self, scenarioName):
        result = self.server.isScenarioReady(scenarioName)
        return result
        
    def getAvailableAntTargets(self):
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
        
    def testVersions(self):
        result = self.server.startModelRun("runVersions", "", "", 0, 0, ["Zufa"])
        print str(result)

        
"""        
c = ServerConnection('192.168.1.141', 8942)
list = c.getAvailableMachines()
for i, item in enumerate(list):
    print "%d: %s" % (i, item)
"""
   
    
#list = c.getAvailableAntTargets()
#for i, item in enumerate(list):
#    print "%d: %s" % (i, item)
#    
#print c.getExistingScenarioProperties()
#print c.tempCreateScenario('Jim Scenario','30','1990','Jim','my test scenario')


c = ServerConnection('192.168.1.141', 8942)
c.testVersions()

