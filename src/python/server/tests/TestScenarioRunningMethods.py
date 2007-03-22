from xmlrpclib import ServerProxy
serverConnection = "http://192.168.1.221:8942"
server = ServerProxy(serverConnection)

def testGetAvailableMachines():
    """
    """
    result = server.getAvailableMachines()
    assert type(result) == type([])

def testGetAvailableAntTargets():
    """
    """
    targetResult = [
    ['runED', 'Runs the Economic Development Model for a single year.\nNo dependencies.', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runEDCalib', 'Runs the Economic Development Model in t=0 for calibration purposes.\nNo dependencies.', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ['runALD', 'Runs the Land Development Model for a single year.\nDepends on ED.', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runSPG1', 'Runs Synthetic Population Generation- part 1 for a single year.\nDepends on ED.', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runPI', 'Runs the Production Allocation Model in monolithic mode for a single year.\nDepends on ED, ALD and SPG1.', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runPIDAF', 'Runs the Production Allocation Model in distributed mode for a single year.\nDepends on ED, ALD and SPG1.', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runSPG2', 'Runs the Synthetic Population Generation - part 2 for a single year.\nDepends on SPG1 and PI', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runPTDAF', 'Runs the Person Transport Model for a single year.\nDepends on PI and SPG2.', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runCT', 'Runs the Commodity Transport Model for a single year.\nDepends on PI.', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runET', 'Runs the External Transport Model for a single year.\nDepends on PI', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runTS', 'Runs the Transport Supply Model for a single year in monolithic mode.\nDepends on PT, ET and CT', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runTSDAF', 'Runs the Transport Supply Model for a single year in distributed mode\nDepends on PT, ET and CT', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['runED20Years', 'Runs the Economic Development model for 20 years, starting in t=1', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ['run1YearSpatial', 'Runs ED, ALD and PIDAF for a single year', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['run1YearSpatialCalibration', 'Runs ED, ALD and PIDAF for t=0', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ['run10YearSpatial', 'Runs ED, ALD and PIDAF for 10 years starting in t=1', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ['run1YearTransport', 'Runs SPG2, PTDAF, ET, CT, and TSDAF for a single year\nDepends on ED, ALD and PI', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['run1YearCalibration', 'Runs ED, ALD, SPG1, PIDAF, SPG2, PTDAF, ET, CT, and TSDAF\nin t=0.', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ['run1Year', 'Runs ED, ALD, SPG1, PIDAF, SPG2, PTDAF, ET, CT, and TSDAF for a single year', ['scenarioName', 'baseScenarioName', 'baseYear', 't']],
    ['run4Year', 'Runs ED, ALD, SPG1, PIDAF, SPG2, PTDAF, ET, CT, and TSDAF for 4 years, starting in t=1', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ['run16Year', 'Runs ED, ALD, SPG1, PIDAF, SPG2, PTDAF, ET, CT, and TSDAF for 16 years, starting in t=1', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ['run31Year', 'Runs ED, ALD, SPG1, PIDAF, SPG2, PTDAF, ET, CT, and TSDAF for 31 years, starting in t=1', ['scenarioName', 'baseScenarioName', 'baseYear']],
    ]
    result = server.getAvailableAntTargets()
    assert result == targetResult

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
