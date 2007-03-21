from xmlrpclib import ServerProxy
serverConnection = "http://192.168.1.221:8942"
server = ServerProxy(serverConnection)

def testGetAvailableMachines():
    """
    """
    assert server.getAvailableMachines() == "unimplemented"

def testGetAvailableMachines_Failure():
    """
    """
    assert server.getAvailableMachines() == "unimplemented"

def testGetAvailableAntTargets():
    """
    """
    assert server.getAvailableAntTargets() == "unimplemented"

def testGetAvailableAntTargets_Failure():
    """
    """
    assert server.getAvailableAntTargets() == "unimplemented"

def testGetScenarioProperties():
    """
    """
    scenario = "Test"
    assert server.getScenarioProperties(scenario) == "unimplemented"

def testGetScenarioProperties_Failure():
    """
    """
    scenario = "Test"
    assert server.getScenarioProperties(scenario) == "unimplemented"

def testStartModelRun():
    """
    """
    scenario = "None"
    module = "None"
    year = "None"
    baseYear = "None"
    machineList = "None"
    assert server.startModelRun(scenario, module, year, baseYear, machineList) == "unimplemented"

def testStartModelRun_Failure():
    """
    """
    scenario = "None"
    module = "None"
    year = "None"
    baseYear = "None"
    machineList = "None"
    assert server.startModelRun(scenario, module, year, baseYear, machineList) == "unimplemented"

def testVerifyModelIsRunning():
    """
    """
    scenario = "None"
    assert server.verifyModelIsRunning(scenario) == "unimplemented"

def testVerifyModelIsRunning_Failure():
    """
    """
    scenario = "None"
    assert server.verifyModelIsRunning(scenario) == "unimplemented"

def testGetStatus():
    """
    """
    scenario = "None"
    arg2 = "None"
    arg3 = "None"
    assert server.getStatus(scenario, arg2, arg3) == "unimplemented"

def testGetStatus_Failure():
    """
    """
    scenario = "None"
    arg2 = "None"
    arg3 = "None"
    assert server.getStatus(scenario, arg2, arg3) == "unimplemented"

def testCheckForTerminationCondition():
    """
    """
    scenario = "None"
    assert server.checkForTerminationCondition(scenario) == "unimplemented"

def testCheckForTerminationCondition_Failure():
    """
    """
    scenario = "None"
    assert server.checkForTerminationCondition(scenario) == "unimplemented"

def testStoreClientState():
    """
    """
    scenario = "None"
    stateInfo = "None"
    assert server.storeClientState(scenario, stateInfo) == "unimplemented"

def testStoreClientState_Failure():
    """
    """
    scenario = "None"
    stateInfo = "None"
    assert server.storeClientState(scenario, stateInfo) == "unimplemented"

def testShowAvailableScenarios():
    """
    """
    assert server.showAvailableScenarios() == "unimplemented"

def testShowAvailableScenarios_Failure():
    """
    """
    assert server.showAvailableScenarios() == "unimplemented"

def testRetrieveClientState():
    """
    """
    scenario = "None"
    assert server.retrieveClientState(scenario) == "unimplemented"

def testRetrieveClientState_Failure():
    """
    """
    scenario = "None"
    assert server.retrieveClientState(scenario) == "unimplemented"
