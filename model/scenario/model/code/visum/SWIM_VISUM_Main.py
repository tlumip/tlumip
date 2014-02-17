#SWIM-TS to VISUM main file
#Ben Stabler, stabler@pbworld.com, 03/22/13
#Palvinder Singh, singhp@pbworld.com, 03/22/13
#Yegor Malinovskiy, malinovskiyy@pbworld, 06/03/13
############################################################

#import libraries
import os, shutil, sys, csv, time, struct, zipfile, numpy as np, math
import win32com.client as com, VisumPy.helpers as VisumHelpers
import VisumPy.matrices as VisumMatrices, VisumPy.csvHelpers as VisumCSV
from Properties import Properties

#parameters
############################################################

#available visum years
year_offset = 1990
available_years = [('_98_00',range(1998,2001)),
                   ('_01_06',range(2001,2007)),
                   ('_07_12',range(2007,2013)),
                   ('_13_18',range(2013,2019)),
                   ('_19_24',range(2019,2025)),
                   ('_25_30',range(2025,2100))]

#change file paths in Visum
pathNo = [15,5,8,57,69,1,7,12,62,41,52,25,11,2,37]

#Year = "_07_12"                #user specific (depends on year considered)
vdfField = "VDF"
linkTypeField = "TypeNo"

#vdf look up
vdf0 = [0,1,2,14,22,23,101]        #Link type = 0 for these vdf functions
vdf1 = [3,4,5,6,7,8,9,10,11,12,15,16,17,18,19,20,30,31,32,33,34,35,36,37,
        38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,102]
                                #Link type = 1 for these vdf functions        
vdf2 = [13,21,90,105]           #Link type = 2 for these vdf functions
vdf3 = [103]                    #Link type = 3 for these vdf functions
vdf4 = [104]                    #Link type = 4 for these vdf functions

#Truck passenger car units ;1.7 is the default
truckPCU = 0.1

#zone no and main zone no in Visum
a_zone = 'NO' #azone field in Visum
b_zone = 'MainZoneNo' #bzone field in Visum

#range of beta zones and external stations in aggregated skim matrices
bzoneRange = range(0,518)
externalStation = range(518,530)

#world market list corresponds to external stations 5001, 5002,....,5012
worldMarketList = [6006,6001,6001,6002,6006,6006,6003,6006,6006,6004,6006,6005]
externalStationList = set(range(5001,5013))

#speed in miles per minute to compute skims between world markets and external stations
speed = 50.0/60.0    #Visum outputs skim in minutes and distance in miles

#matrix indexes
#peak and offpeak time periods for which skim is saved
timePeriods = ['pk','op']
hwyDemandMatrices = [1,16] #start and end of matrices
hwySkimMatrices = [17,32] #start and end of matrices
ldtDemandMatrices = [9,12] #start and end of matrices
sdtDemandMatrices = [13,16] #start and end of matrices
transitSkimMatrices = [17,50] #start and end of matrices
transferEqMin = 10
ivtPeakIntraSkimNum = 35
ivtOffpeakIntraSkimNum = 43
ivtPeakInterSkimNum = 21
ivtOffpeakInterSkimNum = 28

#highway skim matrix names
timeMatNames = ['pkautotime', 'opautotime']
distMatNames = ['pkautodist', 'opautodist']

#LTF settings
ivtParameters = [[2.8921, -0.0174, 0.0057],[2.7813, -0.0029, 0.0046]] #peak, offpeak
ovtParameters = [[3.2197,  0.0061,-0.0167],[3.0879,  0.0072,-0.0076]]
ivtFileNames = ['pkwltfivt', 'opwltfivt']
ovtFileNames = ['pkwltfovt', 'opwltfovt']
ivtMatNums = [17,18] #pk,op
ovtMatNums = [19,20] #pk,op
ivt_coeff = 1
ovt_coeff = 2
maxLOS = 300
maxAutoTimeForLTF = 100
NA = 999999

#intercity transit fare function
intercityTransitFareFuncParams = [1.969,-0.4994]

class SwimModel(object):

    def __init__(self,property_file):
        print("start model run - " + time.ctime())
        
        properties = Properties()
        properties.loadPropertyFile(property_file)

        self.visum_version = int(properties['visum.version'])        
        self.path = properties['ta.demand.output.path']
        self.base_version = properties['ta.base.version.file']
        self.version = properties['ta.version.file']
        self.swimModelInputs = properties['ta.zone.field.names']
        self.zmxInputfileNames = properties['ta.zmx.input.files']
        self.assigmentProcedureDirectory = properties['ta.assignment.parameters.directory']
        self.pathAssignmentProcedure = properties['ta.path.assignment.parameters']
        self.LUCEAssignmentProcedure = properties['ta.luce.assignment.parameters']
        self.assignmentType = properties['ta.assignment.type']
        
        self.intracityRailAssignmentProcedure = properties['tr.transit.assignment.intracity.rail.parameters']
        self.intercityRailAssignmentProcedure = properties['tr.transit.assignment.intercity.rail.parameters']
        self.runFinalTransitAssignment = properties['tr.run.final.assignment.with.pt.demand.matrices'] == "true"
        
        self.ACIWorldMarkets = properties['si.activityconstraintsi.worldmarkets']
        self.worldZoneDistances = properties['ta.world.zone.distances']
        self.hwySkimMatrixNames = properties['ta.skim.matrix.names']
        self.transitSkimMatrixNames = properties['tr.skim.matrix.names']
        self.impExpFields = properties['ta.imp.exp.fields']
        self.losInfo = properties['ta.los.info']
        self.synpopFile = properties['spg2.current.synpop.summary']
        self.pythonDependencyDir = properties['ta.dependent.python.directory']
        self.pythonDependencies = eval(properties['ta.dependent.python.files'].strip())
        self.last_transit_path = properties['transit.assign.previous.skim.path']
        self.current_transit_path = properties['transitSkims.directory']
        self.fareZoneFares = properties['fareZoneFares.file']
        self.air_skims_directory = properties['transit.assign.previous.skim.path']
        self.air_skims = properties['pt.air.peak.skims'].strip().split(',') + properties['pt.air.offpeak.skims'].strip().split(',')
        
        y = year_offset + int(properties['t.year'])
        for ay in available_years:
            if y in ay[1]:
                self.year = ay[0]
                break
        
        os.chdir(self.path)

    def copyAirSkims(self):
        for air_skim in self.air_skims:
            air_skim += ".zmx"
            shutil.copy(os.path.join(self.air_skims_directory,air_skim),os.path.join(self.current_transit_path,air_skim))
        
    def copyDependencies(self):
        for python_dependency in self.pythonDependencies:
            shutil.copy(os.path.join(self.pythonDependencyDir,python_dependency),os.path.join(self.path,python_dependency))
        
    def copyVersion(self):
        shutil.copy(self.base_version,self.version)
        
    def startVisum(self):
        print("start VISUM")
        
        self.Visum = VisumHelpers.CreateVisum(self.visum_version)
        
        #sets working directories in VISUM to use relative file names
        for i in range(0,len(pathNo)): 
            self.Visum.SetPath(pathNo[i], self.path)
            
    def loadVersion(self):
        print("load version file: " + self.version)
        
        self.Visum.LoadVersion(self.version)

    def saveVersion(self):
        print("save version file: " + self.version)
        
        self.Visum.SaveVersion(self.version)

    def closeVisum(self):
        print("close visum window")

        self.Visum = 0

    def loadProcedure(self,parFileName):
        print("load procedure file: " + parFileName)

        self.Visum.Procedures.Open(parFileName)

    def executeProcedure(self,parFileName):
        print("execute stored procedure: " + parFileName)

        self.Visum.Procedures.Execute()

    def loadCSV(self,csvFileName):
        print("load csv file: " + csvFileName)

        csvFile = open(csvFileName, "rb")
        reader = csv.reader(csvFile)
        headers = reader.next()
        fieldDictionary = {}

        for h in headers:
            fieldDictionary[h] = []
            
        for row in reader:
            for h, v in zip(headers, row):
                fieldDictionary[h].append(v)

        return (headers, fieldDictionary)
    
    def writeCSV(self, csvFileName, data):
        print("save csv file: " + csvFileName)

        csvOut = open(csvFileName, 'wb')
        wr = csv.writer(csvOut)
        wr.writerows(data)
        csvOut.close()
    
    
    #read zmx file
    def readZMX(self,zmxfileName):
        
        print('......read zmx file: ' + zmxfileName)

        #read header files
        z = zipfile.ZipFile(zmxfileName, "r")
        version = z.read("_version")
        description = z.read("_description")
        name = z.read("_name")
        zoneNames = z.read("_external column numbers")
        rowZoneNames = z.read("_external row numbers")
        columns = z.read("_columns")
        rows = int(z.read("_rows"))

        #read rows, big-endian floats
        mat = []
        for i in range(1, rows+1):
            fileNameRow = "row_" + str(i)
            data = z.read(fileNameRow)
            mat.append(struct.unpack(">" + "f" * rows, data))

        #close connections
        z.close()

        #return matrix data, zone names, matrix name
        return(mat, zoneNames, name)


    #write zmx file
    def writeZMX(self, fileName, Names, mat):
        
        print("......write zmx file: " + fileName)

        #calculate numZones to remove trailing zeros
        numZones = len(Names)

        #write header files
        z = zipfile.ZipFile(fileName, "w")
        z.writestr("_version", str(2))
        z.writestr("_description", fileName)
        z.writestr("_name", fileName)
        z.writestr("_external column numbers", ",".join(Names))
        z.writestr("_external row numbers", ",".join(Names))
        z.writestr("_columns", str(numZones))
        z.writestr("_rows", str(numZones))

        #write rows, trim unused zones, big-endian floats
        for i in range(1, numZones+1):
            fileNameRow = "row_" + str(i)
            data = struct.pack(">" + "f" * numZones, * mat[i-1][0:numZones])
            z.writestr(fileNameRow, data)

        #close connections
        z.close()

    def stringConcatenate(self, data):

        rows  = len(data)
        cols = len(data[0])
        fileArray = list()
        strConc = ''

        for col in range(0,cols):
            strConc = str(data[0][col])
            for row in range(1,rows):
                strConc = strConc + ',' + str(data[row][col])

            fileArray.append(strConc)

        self.fields[0] = 'Azone'
        strConc = self.fields[0]

        for h in range(1,rows):    #header concatenation
            strConc = strConc + ',' + str(self.fields[h])

        fileArray.insert(0,strConc)

        arrayRows = len(fileArray)

        #define file table
        fileTable = [[]]*arrayRows
        for ro in range(arrayRows):
            fileTable[ro] = fileArray[ro].split(',')

        return fileTable

    def cleanZonalDataOfWorldZones(self,file_table):
        cleaned_table = []
        for row in file_table:
            if (len(cleaned_table) == 0) or (not (int(float(row[0])) in externalStationList)):
                cleaned_table.append(row)
        return cleaned_table

    def createModelInput(self):
        print('Create SWIM Model Input files, Remove Non-Year Transit Lines')

        headers, fieldDictionary = self.loadCSV(self.swimModelInputs)
        
        #write SWIM input files
        fileNames = list(set(fieldDictionary[headers[1]]))

        for fileIndex in range(0,len(fileNames)):
            fileName = fileNames[fileIndex]
            print("Write Swim input file:" + fileName)
            
            #get fields for file
            listRecords = [item for item in range(0,len(fieldDictionary[headers[1]]))
                           if fieldDictionary[headers[1]][item] == fileName]
            self.fields = [fieldDictionary[headers[0]][i] for i in listRecords]
            self.fields.insert(0,a_zone) #add azone no. to the field list

            #get required attributes table from Visum
            data = list()
            for fieldCounter in range(0, len(self.fields)):
                self.field = self.fields[fieldCounter]
                data.append(VisumHelpers.GetMulti(self.Visum.Net.Zones, self.field))

            #Make list of lists (rows)
            fileTable = self.cleanZonalDataOfWorldZones(self.stringConcatenate(data))
            
            #Correct FloorspaceInventory field names
            if fileName.lower().find('floorspaceinventory') > -1:
                fileTable[0] = [s.replace("_"," ") for s in fileTable[0]]
            
            #Reformat ActivityConstraintsI table
            if fileName.lower().find('activityconstraintsi') > -1:
                
                #read world markets file
                self.headers, self.columns = self.loadCSV(self.ACIWorldMarkets)
                
                #reformat table by looping across rows
                outTable = []
                for i in range(len(self.columns[self.headers[0]])):
                  row = []
                  for j in range(len(self.headers)):
                    row.append(self.columns[self.headers[j]][i])
                  outTable.append(row)
                  
                #merge regular zone data
                for i in range(len(fileTable)):
                  row = fileTable[i]
                  for j in range(len(row)):
                    azone = row[0]
                    attr = self.fields[j]
                    value = row[j]
                    if j > 0:
                      outTable.append([azone, attr, value])
                  
                #add column headers
                fileTable = outTable
                fileTable.insert(0, self.headers)
        
            #write resulting file
            self.writeCSV(fileName, fileTable)
        
        #set truck PCU factor
        print("Set truck PCU factor: " + str(truckPCU))
        self.Visum.Net.TSystems.ItemByKey("d").SetAttValue("PCU", truckPCU)
        
        #remove transit lines not in year
        print("Remove transit lines not in year: " + str(self.year))
        lineID = VisumHelpers.GetMulti(self.Visum.Net.LineRoutes, "ID")
        inYear = VisumHelpers.GetMulti(self.Visum.Net.LineRoutes, "NET" + self.year)
        for i in range(len(inYear)):
          if inYear[i] == 0:
            self.Visum.Net.RemoveLineRoute(self.Visum.Net.LineRoutes.ItemByID(lineID[i]))
        
    #read matrices in Python and then insert into Visum
    def insertMatrixInVisum(self, ODmode, start, end, fixedDemand=0):
        
        print("insert demand matrices in Visum: " + ODmode)
        headers, fieldDictionary = self.loadCSV(self.zmxInputfileNames)
        fileNames = list(fieldDictionary[headers[0]])

        for i in range(start-1,end):
            
            #read zmx file from emx folder and post to bank
            fileName = fileNames[i] + '.zmx'
            mat, zoneNames, name = self.readZMX(fileName)
            print("matrix %i sum %i" % (i+1, sum(map(sum, mat))))
            
            #matrix set to fixed demand amount
            if fixedDemand>0:
              for j in range(len(mat)):
                mat[j] = [fixedDemand] * len(mat)
            
            VisumHelpers.SetODMatrix(self.Visum, i+1, mat)
            
    #delete skim matrices with indicated start and end no.s of matrices in Visum 
    def deleteSkimMatrices(self, start, end):
           
        print("delete skim matrices in Visum")
        for matInd in range(start,end+1):
            #remove skim matrix
            matRefObject = self.Visum.Net.Matrices.ItemByKey(matInd)
            self.Visum.Net.RemoveMatrix(matRefObject)

    def importExportFields(self, impFieldName, expFieldName):
        print("......copy data from " + impFieldName + self.year + " to " + expFieldName)

        impField = VisumHelpers.GetMulti(self.Visum.Net.Links, impFieldName + self.year)
        expField = VisumHelpers.SetMulti(self.Visum.Net.Links, expFieldName, impField)

    def runChangeFields(self):
        print("run change year specific fields to general fields")
        fieldheaders, fieldNames = self.loadCSV(self.impExpFields) 

        for i in range(0,len(fieldNames[fieldheaders[0]])):
            self.importExportFields(fieldNames[fieldheaders[0]][i], fieldNames[fieldheaders[1]][i])

    def vdfLookup(self):
        vdfLookup = VisumHelpers.GetMulti(self.Visum.Net.Links, vdfField + self.year)
        typeNo = list()

        for i in range(0,len(vdfLookup)):
            if vdfLookup[i] in vdf0:
                typeNo.append(0)
            elif vdfLookup[i] in vdf1:
                typeNo.append(1)
            elif vdfLookup[i] in vdf2:
                typeNo.append(2)
            elif vdfLookup[i] in vdf3:
                typeNo.append(3)
            elif vdfLookup[i] in vdf4:
                typeNo.append(4)        
        VisumHelpers.SetMulti(self.Visum.Net.Links, linkTypeField, typeNo)

    def aggregateMatSortAverage(self, mat, mainZones):

        #this function is valid for sum,len, min and max functions and default function is sum
        #Get unique district codes and create outMat
        mainZones = np.array(mainZones)
        umainZones = VisumMatrices.unique(mainZones)
        umainZones.sort()

        #initialize arrays
        outMat = np.zeros([len(umainZones),len(umainZones)], dtype=np.float64)

        #build locator array
        locators = []
        for i in range(0, len(umainZones)):
            locators.append(np.where(mainZones == umainZones[i]))

        for i in range(len(locators)):
            origin_locator = locators[i]
            if len(origin_locator) == 0:
                continue
            for j in range(len(locators)):
                dest_locator = locators[j]
                if len(dest_locator) == 0:
                    continue
                outMat[i,j] = np.average(mat[origin_locator][:,dest_locator])

        return outMat

    def aggregateMatSort(self, mat, mainZones, function=sum):

        #this function is valid for sum,len, min and max functions and default function is sum
        #Get unique district codes and create outMat
        mainZones = np.array(mainZones)
        umainZones = VisumMatrices.unique(mainZones)
        umainZones.sort()

        #initialize arrays
        outMat = np.zeros([len(umainZones),len(umainZones)], dtype=np.float64)
        rowMat = np.zeros([len(mainZones),len(umainZones)], dtype=np.float64)

        #Collapse columns
        for i in range(0,len(mat[0])):
            for j in range(0, len(umainZones)):
                locate = np.where(mainZones == umainZones[j])
                rowMat[i,j] = function(mat[i][list(locate[0])])

        #makes method more generic
        if function == len:
            function = sum

        #Collapse rows
        for i in range(0, len(umainZones)):
            for j in range(0, len(umainZones)):
                locate = np.where(mainZones == umainZones[j])
                outMat[j,i] = function(rowMat[:,i][list(locate[0])])

        return outMat

    def zonefieldVariables(self):

        self.zoneNo = VisumHelpers.GetMulti(self.Visum.Net.Zones, a_zone)
        self.zoneNo = map(int, self.zoneNo)
        self.zoneNames = map(str, self.zoneNo)

        self.mainZoneNo = VisumHelpers.GetMulti(self.Visum.Net.Zones, b_zone)
        self.mainZoneNo = map(int, self.mainZoneNo)
        self.uniqueMainZoneNo = VisumMatrices.unique(self.mainZoneNo)
        self.uniqueMainZoneNo.sort()
        self.uniqueMainZoneNo = list(self.uniqueMainZoneNo)

        #delete external station no.s from uniqueMainZoneNo
        self.uniqueMainZoneNo = [i for j, i in enumerate(self.uniqueMainZoneNo) if j not in externalStation]
        
        self.worldMainZoneNo = self.uniqueMainZoneNo + worldMarketList

        self.uniqueMainZoneNo = self.uniqueMainZoneNo + list(set(worldMarketList))
        self.mainZoneNames = map(str, self.uniqueMainZoneNo)

    def matrixCorrection(self, matCount, mat, headers, fieldDictionary):

        for bz in bzoneRange:
            k = 0
            for est in externalStation:
                if matCount % 4 in (0,1):
                    mat[bz][est] += int(fieldDictionary[headers[3]][k])/speed
                    mat[est][bz] += int(fieldDictionary[headers[2]][k])/speed
                elif matCount % 4 == 2:
                    mat[bz][est] += int(fieldDictionary[headers[3]][k])
                    mat[est][bz] += int(fieldDictionary[headers[2]][k])
                k += 1

        for ro_est in range(0,len(externalStation)):
            for col_est in range(0,len(externalStation)):
                if matCount % 4 in (0,1):
                    mat[externalStation[ro_est]][externalStation[col_est]] += int(fieldDictionary[headers[2]][ro_est])/speed + int(fieldDictionary[headers[3]][col_est])/speed
                elif matCount % 4 == 2:
                    mat[externalStation[ro_est]][externalStation[col_est]] += int(fieldDictionary[headers[2]][ro_est]) + int(fieldDictionary[headers[3]][col_est])
                    
        return mat
    
    
    #write highway skim matrices 
    def writeHighwaySkimZMX(self, start, writeBetaMatrices=True):
         
        print("saving highway skim matrices into zmx files")
        
        #load worldZoneDistances.csv for distance between external stations and world markets
        worldheaders, worldfieldDictionary = self.loadCSV(self.worldZoneDistances)

        matheaders, matName = self.loadCSV(self.hwySkimMatrixNames)

        for timeIndex in range(0,len(timePeriods)):
            for matCount in range(0,len(matName[matheaders[0]])):
                
                #get matrix indices of Visum              
                matIndex = matCount + (timeIndex * len(matName[matheaders[0]]))
                VisumMatIndex = start + matIndex              

                azonemat = VisumHelpers.GetSkimMatrix(self.Visum, VisumMatIndex)
                fileName = timePeriods[timeIndex] + matName[matheaders[0]][matCount] + '.zmx'

                #write azone skim matrices to zmx format
                self.writeZMX(fileName, self.zoneNames, azonemat)

                #generate beta zone level skim matrix
                if writeBetaMatrices:
                    betaaggregateMat = self.aggregateMatSortAverage(azonemat,self.mainZoneNo)
                    betaaggregateMat = self.matrixCorrection(matCount, betaaggregateMat, worldheaders, worldfieldDictionary)
                    aggregateMat = self.aggregateMatSortAverage(betaaggregateMat,self.worldMainZoneNo)
                    #write bzone skim matrices to zmx format
                    fileName = 'beta' + fileName
                    self.writeZMX(fileName,self.mainZoneNames,aggregateMat)
                    del(betaaggregateMat,aggregateMat)

    #write transit skim matrices    
    def writeTransitSkimZMX(self, start):
        
        print("saving transit skim matrices into zmx files")
        
        matheaders, matName = self.loadCSV(self.transitSkimMatrixNames)

        for matCount in range(len(matName[matheaders[0]])):

            VisumMatIndex = start + matCount
            azonemat = VisumHelpers.GetSkimMatrix(self.Visum, VisumMatIndex)
            fileName = matName[matheaders[0]][matCount] + '.zmx'
  
            #number of boardings = number of transfers + 1
            if matName[matheaders[0]][matCount]== 'pkwtbrd' or matName[matheaders[0]][matCount]== 'opwtbrd':
              azonemat[azonemat != NA] = azonemat[azonemat != NA] + 1
            
            #set NA to 0 for reading into SWIM
            azonemat[azonemat == NA] = 0
  
            #write azone skim matrices to zmx format
            print 'writing skim matrix ' + fileName + ': ' + str(VisumMatIndex)
            self.writeZMX(fileName, self.zoneNames, azonemat)

    #compute level of service for all the service areas
    def losCompute(self):
        
        print('calculate level of service for station areas')
        
        #input level of service for different stations
        headers, data = self.loadCSV(self.losInfo)
        
        population = map(int, data[headers[5]])
        serviceMiles = map(int, data[headers[6]])

        headers.append('los')
        data[headers[7]] = [min(maxLOS, pop/serv) for pop,serv in zip(population, serviceMiles)]

        #return a lookup of service areas and los        
        return (headers, data)
    

    #create a dictionary of service areas and los (in order of alpha zones in Visum)
    def zoneServiceLookup(self):
        
        print('assign transit level of service to each alpha zone based on service area')

        #get service area from Visum
        self.service_data = dict()
        self.service_data["SERVICEAREA"] = VisumHelpers.GetMulti(self.Visum.Net.Zones, "SERVICEAREA")

        #get level of service data
        losheaders, los_data = self.losCompute()

        losList = []
        for ser in range(len(self.service_data["SERVICEAREA"])):
            for lo in range(len(los_data[losheaders[0]])):
                if int(self.service_data["SERVICEAREA"][ser]) == int(los_data[losheaders[0]][lo]):
                    losList.append(los_data[losheaders[7]][lo])

        self.service_data["LOS"] = losList

    #calculate service area data for LTF
    def calcServiceAreaData(self):
        
        #get zone area
        areas = VisumHelpers.GetMulti(self.Visum.Net.Zones, "AREA")
        areas = [item/(5280**2) for item in areas] #from sq ft to miles
        self.service_data["AREA"] = areas
        
        #read synpop summary table
        self.headers, self.fields = self.loadCSV(self.synpopFile)
        self.service_data["POP"] = self.fields["TotalPersons"]
        self.service_data["EMP"] = self.fields["TotalWorkers"]
        
        #define dict of accumulated measures by service area
        serviceareas = self.service_data["SERVICEAREA"]
        serviceAreaSums = dict()
        for i in range(len(self.service_data["POP"])): #only internal zones
          
          if serviceareas[i] not in serviceAreaSums:
            serviceAreaSums[serviceareas[i]] = [0,0,0,0] #area,pop,emp,p2eDen
            
          sums = serviceAreaSums[serviceareas[i]]
          sums[0] = sums[0] + self.service_data["AREA"][i]
          sums[1] = sums[1] + int(self.service_data["POP"][i])
          sums[2] = sums[2] + int(self.service_data["EMP"][i])
          serviceAreaSums[serviceareas[i]] = sums
        
        #create P2E density field for service area
        for sArea in serviceAreaSums:
          sums = serviceAreaSums[sArea]
          if sums[0] != 0:
              sums[3] = (sums[1] + 2*sums[2])/sums[0]
          else:
              sums[3] = 0
          serviceAreaSums[serviceareas[i]] = sums
        
        #code each zone with its P2E
        self.service_data["P2EDEN"] = [0]*len(self.service_data["AREA"])
        for i in range(len(self.service_data["POP"])):
          sums = serviceAreaSums[serviceareas[i]]
          self.service_data["P2EDEN"][i] = sums[3]

    #inner production of two vectors
    def inner_prod(self, v1, v2):
        v = np.zeros(shape=(len(v1)))
        for i in xrange(len(v1)):
            v[i] = v1[i] * v2[i]
        return v

    #find square root of items of list
    def squareRoot(self, list):
        outList = [math.sqrt(i) for i in list]
        return outList  

    #create local bus ivt and ovt functions
    def createLocalBusSkimMatrix(self, timemat, distmat, isPeak, NAMatrix):
                
        losList = self.service_data["LOS"]
        P2EList = self.service_data["P2EDEN"]
        
        #initialize arrays
        ivtmat = np.zeros([len(timemat),len(timemat)], dtype=np.float64)
        ovtmat = np.zeros([len(distmat),len(distmat)], dtype=np.float64)
        
        #parameters
        if isPeak:
          ivtParams = ivtParameters[0]
          ovtParams = ovtParameters[0]
        else:
          ivtParams = ivtParameters[1]
          ovtParams = ovtParameters[1]
        
        #trace calculations
        o = 1380
        d = 2327
        o_index = VisumHelpers.GetMulti(self.Visum.Net.Zones, "NO").index(o)
        d_index = VisumHelpers.GetMulti(self.Visum.Net.Zones, "NO").index(d)
        
        #calculate matrices
        for ro in range(len(timemat)):
            P2EOrigDest = [(P2EList[ro]**0.5) + item for item in self.squareRoot(P2EList)]
            losListOrigDest = [(losList[ro]*0.5) + item*0.5 for item in losList]
            ivtmat[ro] = ivtParams[0] * timemat[ro] + ivtParams[1] * timemat[ro]**2 + ivtParams[2] * self.inner_prod(losListOrigDest, timemat[ro])
            ovtmat[ro] = [ovtParams[0] * i for i in self.squareRoot(losListOrigDest)] + ovtParams[1] * self.inner_prod(losListOrigDest, distmat[ro]) + [ovtParams[2] * i for i in P2EOrigDest]
            
            #set ivt and ovt to NA if needed
            ivtmat[ro][NAMatrix[ro]] = NA
            ovtmat[ro][NAMatrix[ro]] = NA
            
            #trace calculations
            if ro == o_index:
              print('LTF Trace Zones')
              print('  o: ' + str(o))
              print('  d: ' + str(d))
              print('LTF IVT')
              print('  timemat[o][d]: ' + str(timemat[ro][d_index]))
              print('  losListOrigDest[d]: ' + str(losListOrigDest[d_index]))
              print('  ivtmat[o][d]: ' + str(ivtmat[ro][d_index]))
              print('LTF OVT')
              print('  losListOrigDest[d]: ' + str(losListOrigDest[d_index]))
              print('  distmat[o][d]: ' + str(distmat[ro][d_index]))
              print('  P2EOrigDest[d]: ' + str(P2EList[d_index]))
              print('  ovtmat[o][d]: ' + str(ovtmat[ro][d_index]))
        
        #return ivt and ovt matrices
        return (ivtmat, ovtmat)

    #calculate intracity and intercity transit fare matrices
    def calcFareMatrices(self):
      
      print("calculate transit fare matrices")
      
      #intracity based on district to district lookup table
      
      #get zone fare district, intracity district to district fare file
      zoneFare = VisumHelpers.GetMulti(self.Visum.Net.Zones, "FARE", False)
      colNames, fieldDict = self.loadCSV(self.fareZoneFares)
      odKey = map(" ".join, zip(fieldDict['OFareDistrict'], fieldDict['DFareDistrict']))
      
      #set fare by zone pair
      peak_far = VisumHelpers.GetMatrix(self.Visum, ivtPeakIntraSkimNum + 7)
      offpeak_far = VisumHelpers.GetMatrix(self.Visum, ivtOffpeakIntraSkimNum + 7)
      for i in range(len(zoneFare)):
        for j in range(len(zoneFare)):
          key = zoneFare[i] + " " + zoneFare[j]
          if peak_far[i,j]==0: #0=avail;999999=NA
            if key in odKey:
              peak_far[i,j] = fieldDict['Fare_2007$'][odKey.index(key)]
              offpeak_far[i,j] = fieldDict['Fare_2007$'][odKey.index(key)]
          
          if offpeak_far[i,j]==0: #0=avail;999999=NA
            if key in odKey:
              peak_far[i,j] = fieldDict['Fare_2007$'][odKey.index(key)]
              offpeak_far[i,j] = fieldDict['Fare_2007$'][odKey.index(key)]
      
      #put matrices back in
      VisumHelpers.SetSkimMatrix(self.Visum, ivtPeakIntraSkimNum + 7, peak_far)
      VisumHelpers.SetSkimMatrix(self.Visum, ivtOffpeakIntraSkimNum + 7, offpeak_far)
      
      #intercity based on trip distance function
      
      #get auto distance matrix and distance function parameters
      distmat, distNames, distmatname = self.readZMX(distMatNames[1]  + '.zmx') #offpeak distance
      distmat = np.array(distmat)
      alpha = intercityTransitFareFuncParams[0]
      beta = intercityTransitFareFuncParams[1]
      
      #set fare by zone pair
      peak_far = VisumHelpers.GetMatrix(self.Visum, ivtPeakInterSkimNum + 6)
      offpeak_far = VisumHelpers.GetMatrix(self.Visum, ivtOffpeakInterSkimNum + 6)
      for i in range(len(zoneFare)):
        for j in range(len(zoneFare)):
          fare_ij = distmat[i,j] * (alpha * distmat[i,j]**beta)
          if peak_far[i,j]==0: #0=avail;999999=NA
            peak_far[i,j] = fare_ij
          if offpeak_far[i,j]==0:
            offpeak_far[i,j] = fare_ij

      #put matrices back in
      VisumHelpers.SetSkimMatrix(self.Visum, ivtPeakInterSkimNum + 6, peak_far)
      VisumHelpers.SetSkimMatrix(self.Visum, ivtOffpeakInterSkimNum + 6, offpeak_far)
      
    #read highway skims from *.zmx files and create local bus ivt and ovt skims
    def createLocalBusSkims(self):
        
        print('write ivt and ovt LTF matrices in zmx files')
        
        #create valid OD pairs to calculate LTF values based on offpeak auto time max
        timemat, timeNames, timematname = self.readZMX(timeMatNames[1]  + '.zmx')
        timemat = np.array(timemat)
        NAMatrix = timemat > maxAutoTimeForLTF
        NAMatrix = NAMatrix + NAMatrix.T #make symmetrical
        
        for index in range(len(timeMatNames)):
            timematname = timeMatNames[index]  + '.zmx'
            distmatname = distMatNames[index]  + '.zmx'

            #import highway matrices
            timemat, timeNames, timematname = self.readZMX(timematname)
            distmat, distNames, distmatname = self.readZMX(distmatname)

            #convert matrices into arrays
            timemat = np.array(timemat)
            distmat = np.array(distmat)

            timeNames = map(str, timeNames.split(','))
            distNames = map(str, distNames.split(','))

            #create local bus skims
            if index==0:
              isPeak = True
            else:
              isPeak = False
            ivtskim, ovtskim = self.createLocalBusSkimMatrix(timemat, distmat, isPeak, NAMatrix)

            #post matrices to bank
            self.Visum.Net.AddSkimMatrix (ivtMatNums[index])
            self.Visum.Net.Matrices.ItemByKey(ivtMatNums[index]).SetAttValue("Name", ivtFileNames[index])
            VisumHelpers.SetSkimMatrix(self.Visum, ivtMatNums[index], ivtskim)
            self.Visum.Net.AddSkimMatrix (ovtMatNums[index])
            self.Visum.Net.Matrices.ItemByKey(ovtMatNums[index]).SetAttValue("Name", ovtFileNames[index])
            VisumHelpers.SetSkimMatrix(self.Visum, ovtMatNums[index], ovtskim)

    #adjust skimmed IVT and LTF IVT and OVT skims
    def adjustSkimsDueToLTF(self, isPeak):
        print("adjust skimmed IVT and LTF IVT and OVT skims so only best is available")
        
        if(isPeak):
            skimMatrixStartNum = ivtPeakIntraSkimNum
            ltfIvtMatNum = ivtMatNums[0]
            ltfOvtMatNum = ovtMatNums[0]
        else:
            skimMatrixStartNum = ivtOffpeakIntraSkimNum
            ltfIvtMatNum = ivtMatNums[1]
            ltfOvtMatNum = ovtMatNums[1]
          
        #get intracity skimmed transit skims
        ivt = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum)
        owt = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 1)
        twt = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 2)
        wkt = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 3)
        act = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 4)
        egt = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 5)
        ntr = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 6)
        skm_util = (ivt_coeff * ivt + ovt_coeff * (owt + twt + wkt + act + egt + (ntr * transferEqMin)))
        skm_util[ivt==NA] = NA
        del(ivt, owt, twt, wkt, act, egt, ntr)
        
        #get local transit function skim matrices
        ltf_ivt = VisumHelpers.GetMatrix(self.Visum, ltfIvtMatNum)
        ltf_ovt = VisumHelpers.GetMatrix(self.Visum, ltfOvtMatNum)
        ltf_util = (ivt_coeff * ltf_ivt + ovt_coeff * ltf_ovt)
        ltf_util[ltf_ivt==NA] = NA
        del(ltf_ivt, ltf_ovt)
        
        #Determine best option
        ltfBetter = ltf_util < skm_util
        
        #get intracity skimmed transit skims
        ivt = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum)
        
        #Turn off the worst option by OD pair
        ivt[np.logical_and(ltf_util != NA, ltfBetter)] = NA
        skimOffsets = [1,2,3,4,5,6,7] #other skims
        for i in skimOffsets:
          mat = VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + i)
          mat[np.logical_and(ltf_util != NA, ltfBetter)] = NA
          VisumHelpers.SetSkimMatrix(self.Visum, skimMatrixStartNum + i, mat)
          del(mat)
        
        #get local transit function skim matrices
        ltf_ivt = VisumHelpers.GetMatrix(self.Visum, ltfIvtMatNum)
        ltf_ovt = VisumHelpers.GetMatrix(self.Visum, ltfOvtMatNum)
        
        #Turn off the worst option by OD pair
        ltf_ivt[np.logical_and(skm_util != NA, ltfBetter==False)] = NA
        ltf_ovt[np.logical_and(skm_util != NA, ltfBetter==False)] = NA
  
        #copy over IVT from LTF IVT to skimmed IVT
        ivt[np.logical_and(ltf_util != NA, ltfBetter)] = ltf_ivt[np.logical_and(ltf_util != NA, ltfBetter)]

        #set revised skims
        VisumHelpers.SetSkimMatrix(self.Visum, skimMatrixStartNum, ivt)
        VisumHelpers.SetSkimMatrix(self.Visum, ltfIvtMatNum, ltf_ivt)
        VisumHelpers.SetSkimMatrix(self.Visum, ltfOvtMatNum, ltf_ovt)
        
    #distance function
    def calcDist(self, x1,x2,y1,y2):  
        return(((x1-x2)**2 + (y1-y2)**2)**0.5)

    #Build Transit Connectors
    def buildTransitConnectors(self, ivt_matrix_num, ovt_matrix_num, ivt_coeff, ovt_coeff):
  
        #get stop attributes
        stopNo       =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "No", False)
        stopX        =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "XCoord", False)
        stopY        =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "YCoord", False)
        stopNodeNo   =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "NodeNo", False)
        stopZone     =  [0] * len(stopNo) #will be calculated below

        #get zone attributes
        zoneNo       =  VisumHelpers.GetMulti(self.Visum.Net.Zones, "No", False)
        zoneX        =  VisumHelpers.GetMulti(self.Visum.Net.Zones, "XCoord", False)
        zoneY        =  VisumHelpers.GetMulti(self.Visum.Net.Zones, "YCoord", False)
        zoneStop     =  [0] * len(zoneNo) #will be calculated below
        
        #determine nearest stop for each zone
        for i in range(len(zoneNo)):
            max_dist = 99999999
            for j in range(len(stopNo)):
                dist = self.calcDist(zoneX[i], stopX[j], zoneY[i], stopY[j])
                if dist <= max_dist:
                    zoneStop[i] = stopNo[j]
                    max_dist = dist
        
        #determine each stop's zone
        for i in range(len(stopNo)):
            max_dist = 99999999
            for j in range(len(zoneNo)):
                dist = self.calcDist(stopX[i], zoneX[j], stopY[i], zoneY[j])
                if dist <= max_dist:
                    stopZone[i] = zoneNo[j]
                    max_dist = dist
        
        #get local transit function skim matrices
        ivt = VisumHelpers.GetMatrix(self.Visum, ivt_matrix_num) #ltf ivt
        ovt = VisumHelpers.GetMatrix(self.Visum, ovt_matrix_num) #ltf ovt
        
        #create connectors
        for i in range(len(zoneNo)):
            
            #create connector if needed
            zone = self.Visum.Net.Zones.ItemByKey(zoneNo[i])
            node = self.Visum.Net.Nodes.ItemByKey(stopNodeNo[stopNo.index(zoneStop[i])])
            
            if self.Visum.Net.Connectors.ExistsByKey(node, zone):
                sCon = self.Visum.Net.Connectors.SourceItemByKey(zone, node)
                dCon = self.Visum.Net.Connectors.DestItemByKey(node, zone)
            else:
                self.Visum.Net.AddConnector(zone, node)
                sCon = self.Visum.Net.Connectors.SourceItemByKey(zone, node)
                dCon = self.Visum.Net.Connectors.DestItemByKey(node, zone)
            
            #get IVT and OVT for OD pair
            fz_index = zoneNo.index(zoneNo[i])
            tz_index = zoneNo.index(stopZone[stopNo.index(zoneStop[i])])
            ivt_od = ivt[fz_index, tz_index]
            ovt_od = ovt[fz_index, tz_index]
            
            #set connector cost
            if ivt_od != NA:
              cost = (ivt_coeff * ivt_od + ovt_coeff * ovt_od) * 60 #from min to sec
            else: 
              cost = NA
            sCon.SetAttValue("T0_TSYS(w)", max(cost,0))
            tsys = sCon.AttValue("TSYSSET")
            if 'w' not in tsys:
              tsys = tsys + ",w"
              sCon.SetAttValue("TSYSSET", tsys)
            
            dCon.SetAttValue("T0_TSYS(w)", max(cost,0))
            tsys = dCon.AttValue("TSYSSET")
            if 'w' not in tsys:
              tsys = tsys + ",w"
              dCon.SetAttValue("TSYSSET", tsys)

############################################################
#Main entry point

if __name__== "__main__":
    
    #run modes can be:
    # inputs - create swim inputs
    # highway - run highway assignment
    # transit - run transit assignment

    property_file = sys.argv[1]
    mode = sys.argv[2].lower()
    s = SwimModel(property_file)
    
    #create SWIM model inputs
    ######################################################
    if mode == 'inputs':
        s.copyVersion()
        s.copyDependencies()
        s.startVisum()
        s.loadVersion()
        s.createModelInput()
        s.saveVersion()
        s.closeVisum()
        
    #path based highway assignment
    ######################################################
    if mode == 'highway':
        
        #multiclass private transport assignment    
        s.startVisum()
        s.loadVersion()
        s.insertMatrixInVisum('auto and truck', start=hwyDemandMatrices[0], end=hwyDemandMatrices[1])
        s.runChangeFields()
        s.vdfLookup()
        s.saveVersion()
        s.closeVisum()
        
        #execute procedure file
        pdir = s.assigmentProcedureDirectory
        if s.assignmentType == "PATHBASED":
            procedure = s.pathAssignmentProcedure
        elif s.assignmentType == "LUCE":
            procedure = s.LUCEAssignmentProcedure
        s.startVisum()
        s.loadVersion()
        s.loadProcedure(os.path.join(pdir,procedure))
        s.executeProcedure(os.path.join(pdir,procedure))
        s.saveVersion()
        s.closeVisum()
        
        #write skim matrices in *.zmx files
        s.startVisum()
        s.loadVersion()
        s.zonefieldVariables()
        s.writeHighwaySkimZMX(start=hwySkimMatrices[0], writeBetaMatrices=True)
        s.deleteSkimMatrices(start=hwySkimMatrices[0], end=hwySkimMatrices[1])
        s.saveVersion()
        s.closeVisum()
        
    #transit assignment
    ######################################################
    if mode == 'transit':
        #local bus ivt and ovt functions and premium connector build
        s.startVisum()
        s.loadVersion()
        s.zoneServiceLookup()
        s.calcServiceAreaData()
        s.createLocalBusSkims()
        s.buildTransitConnectors(ivtMatNums[0], ovtMatNums[0], ivt_coeff, ovt_coeff)
        s.saveVersion()
        s.closeVisum()
        
        #intercity transit assignment
        s.startVisum()
        s.loadVersion()
        if s.runFinalTransitAssignment:
          s.insertMatrixInVisum('intercity transit', start=ldtDemandMatrices[0], end=ldtDemandMatrices[1])
        else: 
          s.insertMatrixInVisum('intercity transit', start=ldtDemandMatrices[0], end=ldtDemandMatrices[1], fixedDemand=1)
        s.saveVersion()
        s.closeVisum()
        
        #load and execute procedure file 
        pdir = s.assigmentProcedureDirectory
        procedure = s.intercityRailAssignmentProcedure
        s.startVisum()
        s.loadVersion()
        s.loadProcedure(os.path.join(pdir,procedure))
        s.executeProcedure(os.path.join(pdir,procedure))
        s.saveVersion()
        s.closeVisum()
        
        #intracity transit assignment
        s.startVisum()
        s.loadVersion()
        if s.runFinalTransitAssignment:
          s.insertMatrixInVisum('intracity transit', start=sdtDemandMatrices[0], end=sdtDemandMatrices[1])
        else: 
          s.insertMatrixInVisum('intracity transit', start=sdtDemandMatrices[0], end=sdtDemandMatrices[1], fixedDemand=1)
        s.saveVersion()
        s.closeVisum()
        
        #load and execute procedure file    
        pdir = s.assigmentProcedureDirectory
        procedure = s.intracityRailAssignmentProcedure
        s.startVisum()
        s.loadVersion()
        s.loadProcedure(os.path.join(pdir,procedure))
        s.executeProcedure(os.path.join(pdir,procedure))
        s.saveVersion()
        s.closeVisum()
       
        #Adjust skimmed SDT IVT and LTF IVT and OVT skims
        if not s.runFinalTransitAssignment:
          s.startVisum()        
          s.loadVersion()
          s.calcFareMatrices()
          s.adjustSkimsDueToLTF(isPeak=True)
          s.adjustSkimsDueToLTF(isPeak=False)
          s.saveVersion()
          s.closeVisum() 
        
          #write skim matrices in *.zmx files
          s.startVisum()
          s.loadVersion()
          s.zonefieldVariables()
          s.writeTransitSkimZMX(start=transitSkimMatrices[0])
          s.saveVersion()
          s.closeVisum()
        
        #copy air skims, because it needs to look like they've been run
        s.copyAirSkims()
        
    print("end model run - " + time.ctime())
  