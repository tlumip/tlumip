'''
targetRules = {}

for r in dir(TargetRules):
    obj = eval("TargetRules." + r)
    if type(obj) == types.FunctionType:
        targetRules[r] = obj

def executeRule(target, scenario, baseScenario, baseYear, interval):
    if target in targetRules:
        targetRules[target](scenario, baseScenario, baseYear, interval)
    else:
        targetRules['default'](scenario, baseScenario, baseYear, interval)
'''