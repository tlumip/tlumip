import csv
import os
import shutil
import time

###################################################################
#                        GLOBAL CONSTANTS                         #
###################################################################

# define the files to access; these are/will be standard.
targetFileName = "TLCTargetsI.csv"   # name of target file
groupFileName = "TLCGroupsI.csv"     # name of group definition file
histoFileName = "outputs/t19/Histograms.csv"     # histogram file created by AA
commodFileName = "inputs/parameters/CommoditiesI.csv"  # commoditiesI file from AA

outFileName = "TLCCalib.csv"  # output file name
modelCommand = "run_model.bat"  #the file that runs AA

filesToVersion = ["outputs/t19/Histograms.csv"] # copy all files in this list after each run

upperClip = 1.5 # the maximum increase in param. value between iterations default 2
lowerClip = 0.75  # the minimum increase in param. value between iterations default 0.5
maxIts = 10     # maximum number of iterations to run
gapRange = 0.05  # stop calibration when all errors are +/- this value
initScale = 1.2  # initial scale factor; adjust parameters by this for second run
parmup = 100 # Upper limit for dispersion parameter
parmlow = 5 # lower limit for dispersion parameter

###################################################################

# This program works on a paramList, which is a list of groups, structured
# as follows:
# ["name", target, curr param, curr result, last param, last result]
# where:
#    0: "name" is the group name (e.g. heavy commercial vehicles)
#    1: target is the trip length target, taken from whatever skim is used in HistogramsI
#    2: curr param is the parameter for the current / most recent model run
#    3: curr result is the average trip length from the most recent model run
#    4: last param is the parameter from the previous run
#    5: last result is the average trip length from the previous run


class excelOne(csv.excel):
    # define CSV dialect for Excel to avoid blank lines from default \r\n
    lineterminator = "\n"
     
    
def readTargets(targetFileName):
    # This function returns an initial paramList
    # as defined at the top; it reads in the name of the file containing the group names,
    # target values and the parameter values used for the initial run
    # of AA. This file is called TLCTargetsI.csv

        # open target file and read header
    tFile = open(targetFileName, "r")
    targetFile = csv.reader(tFile)
    
    targHeader  = targetFile.next()
    print "Reading targets file, contains:", targHeader
    groupI = targHeader.index("Group")
    targetI = targHeader.index("Target")
    paramI = targHeader.index("Param")

        # Populate the group list
    paramList = []
    for targ in targetFile:
        row = [targ[groupI], float(targ[targetI]), float(targ[paramI]), 0, 0, 0]
        paramList.append(row)

    tFile.close()
    return paramList


def readGroups(groupFileName):
    # This function returns a groupDict file, which is
    # a concordance between the commodity and buying/selling
    # status and the underlying group. For instance, we might
    # want to do average trip lengths for home to work by
    # including all labour flows in one group.
    # This dictionary has that. Input is the name of the file which specifies this concordance


    gFile = open(groupFileName, "r")
    groupFile = csv.reader(gFile)
        # Parse the header
    groupHeader = groupFile.next()
    print "Reading group file, contains:", groupHeader
    groupI = groupHeader.index("Group")
    commodI = groupHeader.index("Commodity")
    buysellI = groupHeader.index("BuyingSelling")

    groupDict = {}
    for commod in groupFile:
        cname = (commod[commodI], commod[buysellI])
        groupDict[cname] = commod[groupI]

    gFile.close()
    return groupDict



def getTripLengths(paramList, groupDict, hFileName):
    # This function returns an updated paramList, with the current trip length
    # updated to the average of all the groups. Input is the paramList,
    # the groupDict (which says which commodity type is with which group
    # and the name of the histogram file.

    # Open the histogram file and read headers
    hFile = open(hFileName, "r")
    histoFile = csv.reader(hFile)

    histoHeader = histoFile.next()
    commodI = histoHeader.index("Commodity")
    buysellI = histoHeader.index("BuyingSelling")
    volI = histoHeader.index("Quantity")
    lenI = histoHeader.index("AverageLength")

    # define length dictionary, each entry's key is a group name and
    # the value is a list containing [trips, VMT] that is
    # updated through the commodity file work
    lengthDict = {}
    for p in paramList:
        lengthDict[p[0]] = [0, 0]

    # go through histogram file and add the values to the appropriate group
    
    for row in histoFile:
        cname = (row[commodI], row[buysellI])

        if groupDict.has_key(cname):
            vol = float(row[volI])
            vmt = vol * float(row[lenI])
            gname = groupDict[cname]
            lengthDict[gname][0] = lengthDict[gname][0] + vol
            lengthDict[gname][1] = lengthDict[gname][1] + vmt

    # calculate the average trip length and update the paramList file
    for p in paramList:
    
        gname = p[0]
        #print gname
        #print lengthDict[gname][1]
        #print lengthDict[gname][0]
        triplen = lengthDict[gname][1] / lengthDict[gname][0]
        print "Group:", gname, " Average length:", round(triplen,2), " Target:", round(p[1],2), " Error:", round((triplen/p[1]-1)*100,1), "%"
        p[3] = triplen

    hFile.close()
    return paramList


def adjustParams(paramList, init = "no"):
    # Takes in a paramList file, adjusts the parameters and returns it.
    # If the value if init is "init", it scales the current parameters
    # by a fixed proportion (specified at the top), but normally
    # it uses the Secant method and the two previous parameter values
    # to calculate a new one. It also moves the "current" values to
    # "previous" to prepare for the next run.

        # first run, scale parameters up or down
    if init == "init":
        for p in paramList:
            p[4] = p[2] # copy current to previous
            p[5] = p[3]
            if p[3] > p[1]: # model > target
                p[2] = p[2] * initScale
            else:
                p[2] = p[2] / initScale
            if p[2] > parmup: # the dispersion param can't be greater than 100
                p[2] = parmup
            elif p[2] < parmlow and p[2] > 0: # the dispersion param can't be less than 1
                p[2] = parmlow 
    else:
        sameLens = 0
        for p in paramList:
                # rename params for secant method
            x0 = p[4]
            x1 = p[2]
            y0 = p[5] / p[1] - 1
            y1 = p[3] / p[1] - 1

                # secant method
            if y1 == y0: # possible error, or model at edge of limits
                sameLens = sameLens + 1
                if p[3] > p[1]: # model > target
                    x2 = p[2] * initScale
                else:
                    x2 = p[2] / initScale                
            else: #this should run normally
                x2 = x1 - ( (x1 - x0) / (y1 - y0)) * y1

            if x2 > parmup: # the dispersion param can't be greater than 100
                x2 = parmup
            elif x2 < parmlow and x2 > 0: # the dispersion param can't be less than 1
                x2 = parmlow 
                        
            if x2 > x1 * upperClip: # clip parameter (restrict it to prevent from being too big or small)
                    x2 = x1 * upperClip
            if x2 < x1 * lowerClip:
                    x2 = x1 * lowerClip			
            #
                
            
            p[4] = p[2] # copy current to previous

            p[2] = x2  

            p[5] = p[3]
                
                
            
        if sameLens == len(paramList): # if all trip lengths are the same from one run to the next
            #fout.close()
            raise StandardError, "Model trip lengths didn't change -- probably AA crash"
            
    # TODO perhaps switch to the false position method from secant method

    return paramList

def writeParams(paramList, groupDict, commodFileName):
# read the existing commodity file into memory
    fin = open(commodFileName, "r")
    inFile = csv.reader(fin)

    commHeader = inFile.next()
    commList = []
    for row in inFile:
        commList.append(row)
    fin.close()

    # parse headers
    commodI = commHeader.index("Commodity")
    buyI = commHeader.index("BuyingDispersionParameter")
    sellI = commHeader.index("SellingDispersionParameter")

    # build a dictionary of parameters by group
    paramDict = {}
    for row in paramList:
        paramDict[row[0]] = row[2]

    # now write a new one
    fout = open(commodFileName, "w")
    commOut = csv.writer(fout, excelOne)
    commOut.writerow(commHeader)

    # write the parameters to buying and selling
    for row in commList:
        commod = row[commodI]
        cBuy = (commod,"buying")
        cSell = (commod,"selling")

        if (groupDict.has_key(cBuy) and groupDict.has_key(cSell)):
            row[buyI] = paramDict[groupDict[cBuy]]
            row[sellI] = paramDict[groupDict[cSell]]
        # if specified for only one of buy or sell, write same param to both
        elif groupDict.has_key(cBuy):
            row[buyI] = paramDict[groupDict[cBuy]]
            row[sellI] = paramDict[groupDict[cBuy]]
        elif groupDict.has_key(cSell):
            row[buyI] = paramDict[groupDict[cSell]]
            row[sellI] = paramDict[groupDict[cSell]]

        commOut.writerow(row)

    fout.close()
    return

def versionFiles(filesToVersion, i):
    # Copies selected files to make a backup with the iteration ID appended

    
    for fname in filesToVersion:
        copyName = fname + ".bak_" + str(i)
        shutil.copy(fname, copyName)
        

  
###################################################################
#                              MAIN                               #
###################################################################
def main():
    ts = time.clock()

    # open an output file to write progress to
    fout = open(outFileName, "w")
    outputFile = csv.writer(fout, excelOne)
    header = ["Iteration", "Group", "Parameter", "Target", "Model", "Error"]
    outputFile.writerow(header)

    # read in the targets file and the group definitions
    paramList = readTargets(targetFileName)
    groupDict = readGroups(groupFileName)
    
    # Run AA once to initialize
        # write the parameters, run the model
    writeParams(paramList, groupDict, commodFileName)
    os.system(modelCommand)
    versionFiles(filesToVersion, -2)
        
        # update the paramList with new trip lengths
    paramList = getTripLengths(paramList, groupDict, histoFileName)

        # output results before changing parameters
    for p in paramList:
        row = [-1, p[0], p[2], p[1], p[3], (p[3] / p[1] - 1)]
        outputFile.writerow(row)
    fout.flush()

        #update the paramList to calculate initial adjusted parameters
    paramList = adjustParams(paramList, "init")

        # write to commodities file
    writeParams(paramList, groupDict, commodFileName)
    
        # run the model
    os.system(modelCommand)
    versionFiles(filesToVersion, -1)


    # -------------------
    # At this point, the model has been run once to initialize and
    # once with program adjusted parameters.
    # We can now start running it iteratively, using the secant method

    # iterate
    for i in range(maxIts):

        print "----------------------------------------------------"
        print "Completed model run:", i
        print "Time spent so far:", round(time.clock() - ts, 1), "seconds, or", round((time.clock()-ts) / 60, 1), "minutes." 
        
        # update the paramList with new trip lengths
        paramList = getTripLengths(paramList, groupDict, histoFileName)

        # output results before changing parameters
        for p in paramList:
            row = [i, p[0], p[2], p[1], p[3], (p[3] / p[1] - 1)]
            outputFile.writerow(row)
        fout.flush()
         
        # check to see if all model results are close to target
        paramInRange = 0
        totalError = 0
        for p in paramList:
            totalError = totalError + abs(p[3] / p[1] - 1)
            if abs(p[3] / p[1] - 1) < gapRange:
                paramInRange = paramInRange + 1
        print paramInRange, "of", len(paramList), "model values within", gapRange, "of target"
        print round(totalError / len(paramList) * 100, 2), "percent average error overall."
        if paramInRange == len(paramList):
            print
            print "Run ended due to trip length convergence; completed", i, "iterations."
                               
        #update the paramList and calculate new parameters
        paramList = adjustParams(paramList)

        # write to commodities file
        writeParams(paramList, groupDict, commodFileName)
        
        # run the model
        os.system(modelCommand)
        versionFiles(filesToVersion, i)

        
    if i == maxIts:
        print
        print "Run ended due to maxiumum iterations; completed", i, "iterations."
        
    print "Main ran successfully. Good on you."
    fout.close()
    print "TLC.py ran for", time.clock()-ts, "seconds, or", round((time.clock()-ts) / 60, 1), "minutes."



main()
