data = """runED20Years, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runPI , 'Use this target to run the Producation Allocation Model in monolithic mode for a single year - input files from ED' , scenarioName, baseScenarioName, baseYear, t
runEDCalib, 'Use this target to run the Economic Development Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runED, 'Use this target to run the Economic Development Model for a single year', scenarioName, baseScenarioName, baseYear, t
runET, 'Use this target to run the External Transport Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runPTDAF, 'Use this target to run the Economic Development Model', scenarioName, baseScenarioName, baseYear, t
runCT, 'Use this target to run the CT Model', scenarioName, baseScenarioName, baseYear, t
run1YearSpatialCalibration, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runSPG2, 'Use this target to run the Economic Development Model', scenarioName, baseScenarioName, baseYear, t
run15Year, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runPIDAF, 'Use this target to run the Economic Development Model', scenarioName, baseScenarioName, baseYear, t
runSPG1, 'Use this target to run the first step in the Synthetic Population Generation for a single year', scenarioName, baseScenarioName, baseYear, t
run4Year, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
run10YearSpatial, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runTSDAF, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
run1YearSpatial, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
run1Year, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runTS, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
run1YearCalibration, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
run30Year, 'Use this target to run the Transport Supply Model in the base year for calibration purposes', scenarioName, baseScenarioName, baseYear, t
runALD, 'Use this target to run the Land Development Model for a single year', scenarioName, baseScenarioName, baseYear, t
"""

import csv, string
from StringIO import StringIO

reader = csv.reader(StringIO(data))
for r in reader:
    row = map(string.strip, r)
    row[1] = row[1].strip("'")
    print row