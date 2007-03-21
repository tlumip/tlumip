from xmlrpclib import ServerProxy
import sys, time, os, threading, subprocess

serverConnection = "http://192.168.1.221:8942"
badServerConnection = "http://192.168.1.221:8943"

def testConnection():
    try:
        server = ServerProxy(serverConnection)
        assert server.checkConnection() == "Connection OK"
    except Exception, v:
        print "ERROR", v
        assert False

def testConnection_Failure():
    try:
        server = ServerProxy(badServerConnection)
        assert server.checkConnection() == "Connection OK"
    except Exception, v:
        print "ERROR", v
        return
    assert False

testDirName = "ZZZ%d" % time.time()
server = ServerProxy(serverConnection)

def testScenarioCreation():
    result = server.createScenario(testDirName, "1", "1990", os.environ.get("username"), "Nose test results")
    assert result == 0, result

def testDuplicateScenarioNameCreation_Failure():
    result = server.createScenario(testDirName, "1", "1990", os.environ.get("username"), "Nose test results")
    assert result.startswith("ERROR:"), result

def testScenarioCreationComplete():
    result = server.isScenarioReady(testDirName)
    assert result == "Scenario Ready", result

testDirName2 = "ZZZ_%d" % time.time()

def testSimultaneousScenarioCreation_Failure():
    subprocess.Popen(["python", "ScenarioCreationProcess.py", testDirName2])
    assert server.isScenarioReady(testDirName2) == "Scenario Not Ready"
    time.sleep(1)
    result = server.createScenario(testDirName2, "1", "1990",
        os.environ.get("username"), "Nose test results")
    assert result == "ERROR: Directory Creator returned 1"
    assert server.isScenarioReady(testDirName2) == "Scenario Ready"

def testSimultaneousScenarioCreation():
    testDirName3 = "ZZZ__%d" % time.time()
    subprocess.Popen(["python", "ScenarioCreationProcess.py", testDirName3])
    assert server.isScenarioReady(testDirName3) == "Scenario Not Ready"
    testDirName4 = "ZZZ__%d" % time.time()
    subprocess.Popen(["python", "ScenarioCreationProcess.py", testDirName4])
    assert server.isScenarioReady(testDirName4) == "Scenario Not Ready"
    time.sleep(2)
    assert server.isScenarioReady(testDirName3) == "Scenario Ready"
    assert server.isScenarioReady(testDirName4) == "Scenario Ready"


# Test the csv file?
# Retrieve all information in a csv row?
# Do we need to do an os "directory lock" on the csv file?

# Try to run scenario before creation is complete


