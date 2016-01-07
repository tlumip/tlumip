import csv
import math
import os
import shutil
import time

###################################################################
#                        GLOBAL CONSTANTS                         #
###################################################################

# define the files to access; these are/will be standard.
actnumFile = "outputs/t19/ActivityNumbers.csv" # activity numbers, for calculating technology choice totals
actlocFile = "outputs/t19/ActivityLocations.csv" # activity locations, for calculating technology choice totals
choiceFile = "outputs/t19/TechnologyChoice.csv" # technology choice, for caclulating technology choice totals

targetFile = "outputs/t19/householdClusterChoiceTargets.csv"     # name of targets file
techFile = "inputs/parameters/TechnologyOptionsI.csv"     # name of Technology Options file
logFile = "event.log" # name of log file, which we want to back up each year

outFile = "ResOptSizeResults/OptSizeCheck.csv"    # output file name
modelCommand = "python run_aa.py"     # the file that runs AA
modelProps = "outputs/t19/aa.properties" # the AA properties file
runModel = True

# Script parameters; these effect how it runs
maxUp = 2.0  # Ceiling for scaling up option sizes
maxDown = 0.15 # Floor for scaling down option sizes
numIts = 1 # Number of iterations to run
keepAllFiles = True # Whether to maintain copies of TechnologyOptions
                    #   and OptSize from each iteration
rampup = False # Whether to tighten the AA convergence criteria every iteration. If set to false, the following four parameters will be ignored and the convergence criteria from the properties file will be used.
initSpecClear = 10 # First-iteration value for maximum specific clearance in AA
finalSpecClear = 0.03 # Last-iteration value for maximum specific clearance in AA
initTotalClear = 10 # First-iteration value for maximum total clearance in AA
finalTotalClear = 0.0002 # Last-iteration value for maximum total clearance in AA



class excelOne(csv.excel):
    # define CSV dialect for Excel to avoid blank lines from default \r\n
    lineterminator = "\n"


        


def buildTargetDictionary():
    ###################### This section builds a target dictionary
    fin = open(targetFile, "rU")
    targs = csv.reader(fin)
    targHeader = targs.next()

    targDict = {}
    for row in targs:
        # Build a set of keys in the form [Activity, Commodity, MorU]
        key1 = row[targHeader.index("Activity")]
        key2 = row[targHeader.index("option")]
        #key3 = row[targHeader.index("MorU")]
        tgt = float(row[targHeader.index("Target")])
        key = (key1, key2) #(key1, key2, key3)
        targDict[key] = [tgt, 0] # each entry has target, amount
    fin.close()
    return targDict
    
# Reads ActivityNumbers, ActivityLocations, TechnologyChoice to get technology choice totals
def readTechOptSum(targDict):
    startTime = time.clock()
    
    numDict = {} # activity number -> activity
    print "Reading in", actnumFile
    with open(actnumFile, "rU") as fin:
        source = csv.reader(fin)
        header = source.next()
        numcol = header.index("ActivityNumber")
        actcol = header.index("Activity")
        for row in source:
            numDict[row[numcol]] = row[actcol]
    
    actDict = {}
    print "Reading in", actlocFile
    with open(actlocFile, "rU") as fin:
        source = csv.reader(fin)
        header = source.next()
        actcol = header.index("Activity")
        zonecol = header.index("ZoneNumber")
        qcol = header.index("Quantity")
        for row in source:
            key = (row[actcol], row[zonecol])
            actDict[key] = float(row[qcol])
    
    print "Reading in ", choiceFile
    with open(choiceFile, "rU") as fin:
        source = csv.reader(fin)

        header = source.next()
        actcol = header.index("Activity")
        zonecol = header.index("Zone")
        optcol = header.index("Option")
        probcol = header.index("Probability")
        i = 0

        for row in source:
            act = numDict[row[actcol]]
            opt = row[optcol]
            zone = row[zonecol]
            amount = actDict[act, zone] * float(row[probcol])
            key = (act, opt)
            if key in targDict:
                targDict[key][1] = targDict[key][1] + amount
            
            i = i + 1
            if i % 100000 == 0:
                print "Processed", i, "lines:", time.clock() - startTime
    
    return targDict

def writeStatus(targDict):
    ######################### This writes out the status (tgt vs model)
    print "Writing results."

    fout = open(outFile, "w")
    outWriter = csv.writer(fout, excelOne)

    outWriter.writerow(["Activity", "Option", "Target", "Amount", "Error"])

    keyList = targDict.keys()
    totalError = 0
    for k in keyList:
        row = [k[0], k[1], targDict[k][0], targDict[k][1]]
        error = (targDict[k][0] - targDict[k][1]) * (targDict[k][0] - targDict[k][1]) #^2
        row.append(error)
        outWriter.writerow(row)
        totalError = totalError + error
    fout.close()

    print "Total error at this point:", totalError
    
    return totalError

    ######################## This reads in the technology options file
def readWriteTechOptions(targDict):
    # Things we assume about the technology options file:
    # option name consists of the commodities affected by that row
    # option name has "|" breaking each commodity
    # option name has "cluster" in it (otherwise, not adjusted)

    fin = open(techFile, "rU")
    techReader = csv.reader(fin)

    techHeader = techReader.next()

    techList = []
    for row in techReader:
        optAct = row[techHeader.index("Activity")]
        optName = row[techHeader.index("OptionName")]
        optSize = float(row[techHeader.index("OptionWeight")])

        key = (optAct, optName)
        #numadj = 0.0
        adj = 1.0
        
        if targDict.has_key(key):
            targ, mod = targDict[key]
            if mod == 0:
                adj = adj * maxDown
            else:
                trialAdj = targ / mod
                if maxDown > trialAdj:
                    adj  = adj * maxDown
                elif maxUp < trialAdj:
                    adj = adj * maxUp
                else:
                    adj = adj * trialAdj
            #print "Adjusted", key2, "Adj =", adj, "Targ =", targ, "Mod =", mod
        else:
            pass
            #print "WARNING: Couldn't find key:", key, "for activity", optAct, "in tech option", optName
        row[techHeader.index("OptionWeight")]  = optSize * adj
        techList.append(row)
        
    fin.close()

    ################# Write Tech Options file
    fout = open(techFile, "w")
    techOutWriter = csv.writer(fout, excelOne)

    techOutWriter.writerow(techHeader)
    for row in techList:
        techOutWriter.writerow(row)

    print "Wrote", len(techList), "rows out."
    fout.close()
    return
    
def set_aa_params(specClear, totalClear):
    #Import is here so no dependency on prop_edit unless this function is called.
    import prop_edit
    prop_edit.load_props(modelProps)
    origSpecClear = prop_edit.set_prop("aa.maxSpecificClearance", specClear)
    origTotalClear = prop_edit.set_prop("aa.maxTotalClearance", totalClear)
    prop_edit.save_props(modelProps)
    return origSpecClear, origTotalClear

def main(numIts = numIts, rampup = rampup, initSpecClear = initSpecClear, finalSpecClear = finalSpecClear, initTotalClear = initTotalClear, finalTotalClear = finalTotalClear):
    
    if rampup:
        specClear = initSpecClear
        totalClear = initTotalClear
        
        origSpecClear, origTotalClear = set_aa_params(specClear, totalClear)
        
        # Figure out by how much we reduce the convergence criteria each iteration.
        specFactor = (finalSpecClear / initSpecClear) ** (1.0 / (numIts - 1))
        totalFactor = (finalTotalClear / initTotalClear) ** (1.0 / (numIts - 1))
    
    for x in range(numIts):
        ts = time.clock()
        
        if keepAllFiles:
            # Backup technology options file
            copyName = ".".join(techFile.split(".")[0:-1]) + "_" + str(x) + "_.csv"
            shutil.copy(techFile, copyName)
        
        if rampup and x > 0:
            specClear = specClear * specFactor
            totalClear = totalClear * totalFactor
            
            set_aa_params(specClear, totalClear)
        
        # Run AA
        if runModel:
            os.system(modelCommand)        

        targetDictionary = buildTargetDictionary()

        targetDictionary = readTechOptSum(targetDictionary)

        currentError = writeStatus(targetDictionary)
        
        readWriteTechOptions(targetDictionary)

        if keepAllFiles:            
            # Back up output file
            copyName = ".".join(outFile.split(".")[0:-1]) + "_" + str(x) + "_.csv"
            shutil.copy(outFile, copyName)
            
            # Back up choice file
            copyName = ".".join(choiceFile.split(".")[0:-1]) + "_" + str(x) + "_.csv"
            shutil.copy(choiceFile, copyName)
            
            # Back up activity locations file
            copyName = ".".join(actlocFile.split(".")[0:-1]) + "_" + str(x) + "_.csv"
            shutil.copy(actlocFile, copyName)
            
            # Back up log file
            copyName = ".".join(logFile.split(".")[0:-1]) + "_" + str(x) + "_.log"
            shutil.copy(logFile, copyName)
        
        print "********************************************************"
        print "Generation", x, " -- Error this run:", currentError
        if rampup:
            print "AA convergence criteria: specific = " + str(specClear) + ", total = " + str(totalClear)
        print "Elapsed time:", round(time.clock()-ts, ), "seconds."
        print "********************************************************"
        print

def get_status(specClear = finalSpecClear, totalClear = finalTotalClear):
    """Runs AA and produces normal progress-checking output, but does no calibration.
    """
    
    set_aa_params(specClear, totalClear)
    
    ts = time.clock()
    
    if runModel:
        os.system(modelCommand)
    targetDictionary = buildTargetDictionary()
    targetDictionary = readTechOptSum(targetDictionary)
    currentError = writeStatus(targetDictionary)
    
    print "********************************************************"
    print "Non-calibrating progress check -- Error:", currentError
    print "AA convergence criteria: specific = " + str(specClear) + ", total = " + str(totalClear)
    print "Elapsed time:", round(time.clock()-ts, ), "seconds."
    print "********************************************************"
    print

if __name__ == "__main__":
    main()
    #os.system("sudo shutdown now -h")
