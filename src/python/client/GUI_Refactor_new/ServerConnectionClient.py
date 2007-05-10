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
        
    #def startModelRun(self, target, scenario, baseScenario, baseYear, interval, machineList):
    #    result = self.server.startModelRun(target, scenario, baseScenario, baseYear, interval, machineList)
    #    return result
        
    def startModelRun(self, target, parameters, machineList):
        result = self.server.startModelRun(target, parameters, machineList)
        return result
        
    def testVersions(self):
        result = self.server.startModelRun("runVersions", {}, ["LX-SALEMMILL-1"])
        print str(result)
    
    def getStartModelRunStdOut(self,machine):
        return self.server.getStdOutText(machine)



###Test Class###
class ServerConnectionTest(ServerConnection):
    def __init__(self, ipAddress, port):
        ServerConnection.__init__(self,ipAddress, port)
    
    def testGetAvailableMachines(self):
        print "Testing get available machines"
        list = self.getAvailableMachines()
        t = ""
        s = ""
        first = True
        for dict in list:
            for key in dict:
                if first:
                    t += key.ljust(15)
                s += dict[key].ljust(15)
            s += "\n"
            first = False
        print t
        print s

    def testGetAvailableAntTargets(self):
        print "Testing get available ant targets"
        list = self.getAvailableRunTargets()
        s = ""
        for item in list:
            first = True
            for i in item:
                if first:
                    s += str(i).replace("\n","\n\t") + "\n"
                    first = False
                    continue
                s += "\t" + str(i).replace("\n","\n\t") + "\n"
            s += "\n"
        print s
    
    def testGetExistingScenarioProperties(self):
        print "Testing get existing scenario properties"
        list = self.getExistingScenarioProperties()     
        s = ""
        for key in list:
            s += key + "\n"
            for key2 in list[key]:
                s += "\t" + key2 + ": " + list[key][key2] + "\n"
            s += "\n"
        print s
    
    def testCreateScenario(self):
        print "Testing create scenario"
        self.createScenario('JimTestScenario','2','1990','Jim','scenario for testing gui, client, and server componenets.')
    
    def testTestVersions(self):
        print "Testing versions"
        self.testVersions()
    
    def testModule(self,target,daf):
        level = "monolithic"
        #machineList = ['Athena']
        machineList = ['LX-SALEMMILL-1']
        if daf:
            level = "DAF"
            machineList.append('Chaos')
            machineList.append('Dione')
        print "Testing " + level + " module: " + target
        parameters = {}
        #parameters['scenarioName'] = 'JimTestScenario'
        parameters['scenarioName'] = 'SalemGUITest1'
        parameters['baseScenario'] = '90_Base'
        parameters['baseYear'] = 1990
        parameters['t'] = 1
        #self.startModelRun(target, scenario, baseScenario, baseYear, interval, machineList)
        self.startModelRun(target, parameters, machineList)
        
    def testEd(self):
        self.testModule('runED',False)
        
    def testAld(self):
        self.testModule('runALD',False)
        
    def testSpg1(self):
        self.testModule('runSPG1',False)
        
    def testPi(self):
        self.testModule('runPIDAF',True)
        
    def testSpg2(self):
        self.testModule('runSPG2',False)
        
    def testPt(self):
        self.testModule('runPTDAF',True)
        
    def testCt(self):
        self.testModule('runCT',False)
        
    def testEt(self):
        self.testModule('runET',False)
        
    def testTs(self):
        self.testModule('runTSDAF',True)
    
    def testGetStartModelRunStdOut(self,machine):
        result = self.getStartModelRunStdOut(machine)
        print result


######

#"""Run test(s)
#test = ServerConnectionTest('192.168.1.221', 8942)
#test = ServerConnectionTest('167.131.72.201', 8942)
#test.testGetAvailableMachines()
#test.testGetAvailableAntTargets()
#test.testGetExistingScenarioProperties()
#test.testCreateScenario()
#test.testTestVersions()
#test.testEd()
#test.testAld()
#test.testSpg1()
#test.testPi()
#test.testSpg2()
#test.testPt()
#test.testCt()
#test.testEt()
#test.testTs()
#test.testGetStartModelRunStdOut('LX-SALEMMILL-1')
#"""