from xmlrpclib import ServerProxy
import sys, time, os, threading, subprocess


class ServerConnection(object):
    def __init__(self, ipAddress, port):
        try:
            serverConnection = "http://" + ipAddress + ":" + str(port)
            print "connecting to", serverConnection
            self.server = ServerProxy(serverConnection)
            result = self.server.checkConnection()
            print "server", result
            
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
        # get list of [name, descr, reqd args[]] for ant targets
        result = self.server.getAvailableMachines()
        return result
        


        
        
c = ServerConnection('192.168.1.221', 8942)
list = c.getAvailableMachines()
for i, item in enumerate(list):
    print "%d: %s" % (i, item)
    
#list = c.getAvailableAntTargets()
#for i, item in enumerate(list):
#    print "%d: %s" % (i, item)
#    
#print c.getExistingScenarioProperties()
#print c.tempCreateScenario('Jim Scenario','30','1990','Jim','my test scenario')

