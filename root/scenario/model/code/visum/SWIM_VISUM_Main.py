
#import libraries
import os, shutil, sys, csv, time, struct, zipfile, numpy as np, math
import win32com.client as com, VisumPy.helpers as VisumHelpers
import VisumPy.matrices as VisumMatrices, VisumPy.csvHelpers as VisumCSV
from Properties import Properties
import heapq
import operator

#parameters
############################################################

#available visum years
year_offset = 1990

#change file paths in Visum
pathNo = [15,5,8,57,69,1,7,12,62,41,52,25,11,2,37]

#Truck passenger car units ;1.7 is the default
#Was 0.1, changed after Rick's new CT (11/24/2014) YMM
truckPCU = 1.7
truckDemandClass = "d"

#zone no and main zone no in Visum
a_zone = 'NO' #azone field in Visum
b_zone = 'MainZoneNo' #bzone field in Visum
zoneAlias = {'AZONE': a_zone, 'BZONE': b_zone}

#state and county fips codes
state_fips = {'CA': 6, 'OR': 41, 'WA': 53}
county_fips = {'Baker': '001',
               'Benton': '003',
               'Benton_WA': '005',
               'Clackamas': '005',
               'Clark_WA': '011',
               'Clatsop': '007',
               'Columbia': '009',
               'Coos': '011',
               'Cowlitz_WA': '015',
               'Crook': '013',
               'Curry': '015',
               'DelNorte_CA': '015',
               'Deschutes': '017',
               'Douglas': '019',
               'Franklin_WA': '021',
               'Gilliam': '021',
               'Grant': '023',
               'Harney': '025',
               'Hood River': '027',
               'Jackson': '029',
               'Jefferson': '031',
               'Josephine': '033',
               'Klamath': '035',
               'Klickitat_WA': '039',
               'Lake': '037',
               'Lane': '039',
               'Lincoln': '041',
               'Linn': '043',
               'Malheur': '045',
               'Marion': '047',
               'Morrow': '049',
               'Multnomah': '051',
               'Pacific_WA': '049',
               'Polk': '053',
               'Sherman': '055',
               'Siskiyou_CA': '093',
               'Skamania_WA': '059',
               'Tillamook': '057',
               'Umatilla': '059',
               'Union': '061',
               'Wahkiakum_WA': '069',
               'WallaWalla_WA': '071',
               'Wallowa': '063',
               'Wasco': '065',
               'Washington': '067',
               'Wheeler': '069',
               'Yamhill': '071',
               'EXTSTA': '0'}

# Conversion factor for mi^2 to acres
Mi2Acres = 640

#range of beta zones and external stations in aggregated skim matrices
bzoneRange = range(0,457)
externalStation = range(457,469)
bzoneRangeMiniModel = range(0,2)
externalStationMiniModel = range(2,14)
maxMiniModelZones = 100 #mini model can have up to this number of zones

#world market list corresponds to external stations 5001, 5002,....,5012
worldMarketList = [6006,6001,6001,6002,6006,6006,6003,6006,6006,6004,6006,6005]
worldMarketZones = ['6001', '6002', '6003', '6004', '6005', '6006']

#speed in miles per minute to compute skims between world markets and external stations
speed = 50.0/60.0    #Visum outputs skim in minutes and distance in miles

#matrix indexes
#peak and offpeak time periods for which skim is saved
timePeriods = ['pk','op','pm','nt']
hwyDemandMatrices = [1,16] #start and end of matrices
hwySkimMatrices = [21,52] #start and end of matrices
ldtDemandMatrices = [9,12] #start and end of matrices
sdtDemandMatrices = [13,16] #start and end of matrices
transitSkimMatrices = [21,50] #start and end of matrices
transferEqMin = 10
ivtPeakIntraSkimNum = 39
ivtOffpeakIntraSkimNum = 47
ivtPeakInterSkimNum = 25
ivtOffpeakInterSkimNum = 32

#highway skim matrix names
timeMatNames = ['pkautotime', 'opautotime']#'pmautotime','ntautotime']
distMatNames = ['pkautodist', 'opautodist']#,'pmautodist','ntautodist']

#LTF settings
ivtParameters = [[2.8921, -0.0174, 0.0057],[2.7813, -0.0029, 0.0046]] #peak, offpeak
ovtParameters = [[3.2197,  0.0061,-0.0167],[3.0879,  0.0072,-0.0076]]
ivtFileNames = ['pkwltfivt', 'opwltfivt']
ovtFileNames = ['pkwltfovt', 'opwltfovt']
ivtMatNums = [21,22] #pk,op
ovtMatNums = [23,24] #pk,op
ivt_coeff = 1
ovt_coeff = 2
maxLOS = 300
maxAutoTimeForLTF = 100
NA = 999999
numTransitConnectors = 2
walkSpeed = 3

#intercity transit fare function
intercityTransitFareFuncParams = [1.969,-0.4994]

#countlocations file name
countLocationsFileName = "countlocations.csv"

class SwimModel(object):

    def __init__(self,property_file):
        print("start model run - " + time.ctime())

        properties = Properties()
        properties.loadPropertyFile(property_file)

        self.current_year = properties['t.year']
        self.prefix = properties['t.year.prefix']

        self.visum_version = int(properties['visum.version'])
        self.path = properties['ta.demand.output.path']
        self.base_version = properties['ta.base.version.file']
        self.version = properties['ta.version.file']
        self.swimModelInputs = properties['ta.zone.field.names']
        self.swimVizOutputs = properties['ta.link.field.names']
        self.zmxInputfileNames = properties['ta.zmx.input.files']
        self.assigmentProcedureDirectory = properties['ta.assignment.parameters.directory']
        self.initialAutoAssignmentProcedure = properties['ta.initial.auto.assignment.parameters']
        self.initialTransitAssignmentProcedure = properties['ta.initial.transit.assignment.parameters']
        self.pathAllPeriodAssignmentProcedure = properties['ta.path.allperiod.assignment.parameters']

        self.externalStationList = map(int, properties['external.stations'].strip().split(' '))

        self.intracityRailAssignmentProcedure = properties['tr.transit.assignment.intracity.rail.parameters']
        self.intercityRailAssignmentProcedure = properties['tr.transit.assignment.intercity.rail.parameters']

        self.auto_op_cost = properties['auto.operating.cost']
        self.agForestFloorspace = properties['agforest.floorspace.file']
        self.activityTotals = properties['aa.activity.totals']
        self.activityTotalsOther = properties['aa.activity.totals.other']
        self.copyToPreviousYear = properties['ald.copyToPreviousYear']
        self.currentActivity = properties['ald.CurrentActivity']
        self.previousActivity = properties['ald.PreviousActivity']
        self.worldZoneDistances = properties['world.to.external.distances']
        self.mclsmatrixdir = properties['skim.data']
        self.tourmodechoiceparams = properties['sdt.tour.mode.parameters']
        self.hwySkimMatrixNames = properties['ta.skim.matrix.names']
        self.transitSkimMatrixNames = properties['tr.skim.matrix.names']
        self.losInfo = properties['ta.los.info']
        self.synpopFile = properties['spg2.current.synpop.summary']
        self.lastTRansit_path = properties['transit.assign.previous.skim.path']
        self.currentTRansit_path = properties['transitSkims.directory']
        self.fareZoneFares = properties['fareZoneFares.file']
        self.air_skims_directory = properties['transit.assign.previous.skim.path']
        self.air_skims = properties['pt.air.peak.skims'].strip().split(',') + properties['pt.air.offpeak.skims'].strip().split(',')

        self.ampeakstart = int(properties['am.peak.start'])
        self.ampeakend = int(properties['am.peak.end'])
        self.pmpeakstart = int(properties['pm.peak.start'])
        self.pmpeakend = int(properties['pm.peak.end'])
        self.mdoffpeakstart = int(properties['md.offpeak.start'])
        self.mdoffpeakend = int(properties['md.offpeak.end'])
        self.ntoffpeakstart = int(properties['nt.offpeak.start'])
        self.ntoffpeakend = int(properties['nt.offpeak.end'])

        self.ampeakcapadj = float(properties['am.peak.capacity.adjustment'])
        self.pmpeakcapadj = float(properties['pm.peak.capacity.adjustment'])
        self.mdoffpeakcapadj = float(properties['md.offpeak.capacity.adjustment'])
        self.ntoffpeakcapadj = float(properties['nt.offpeak.capacity.adjustment'])

        self.imp_penalty_interstate = float(properties['imp.penalty.interstate'])
        self.imp_penalty_principal_arterial = float(properties['imp.penalty.principal.arterial'])
        self.imp_penalty_minor_arterial = float(properties['imp.penalty.minor.arterial'])
        self.imp_penalty_other_roads = float(properties['imp.penalty.other.roads'])

        self.volumeFactorPercentile = float(properties['ta.volume.factor.percentile'])
        self.worldMarketFieldnames = properties['world.market.fieldnames.file']
        self.sdtTODTripsFile = properties['sdt.tod.trips.file']
        self.airInputsFile = properties['air.inputs']

        self.reSeedMatrices = properties['new.zone.system']

        self.ta_num_processors = properties['ta.num.processors']
        self.tr_num_processors = properties['tr.num.processors']
        self.userPreferenceFile = os.path.join(self.path, properties['user.preference.file'])
        self.scenario_root = properties['scenario.root']


        os.chdir(self.path)

    def copyAirSkims(self):
        for air_skim in self.air_skims:
            air_skim += ".zmx"
            shutil.copy(os.path.join(self.air_skims_directory,air_skim),os.path.join(self.currentTRansit_path,air_skim))

    def copyVersion(self):

        currentToken = str(self.prefix) + str(self.current_year)

        if os.path.isfile(self.base_version):
            shutil.copy(self.base_version,self.version)
        else:
            for i in range(1, int(self.current_year)+1):
                newToken = str(self.prefix) + str(int(self.current_year) - i)
                newPath = str(self.base_version).replace(currentToken, newToken)
                if os.path.isfile(newPath):
                    print("Copying version file from " + newToken + " to " + currentToken)
                    shutil.copy(newPath,self.version)
                    break


    def startVisum(self):
        print("start VISUM " + str(self.visum_version))

        numtry = 20
        pauseSec = 30
        for i in range(numtry):
          try:
            self.Visum = VisumHelpers.CreateVisum(self.visum_version)
            print("Sucessfully started VISUM COM server")
            break
          except:
            print("Failed starting VISUM COM server")
            time.sleep(pauseSec)
        
        if (i+1)==numtry:
          print("Failed to start VISUM COM server")

        #sets working directories in VISUM to use relative file names
        for i in range(0,len(pathNo)):
            self.Visum.SetPath(pathNo[i], self.path)

        self.Visum.UserPreferences.EnvironmentUserPreferences.SetAttValue("NUMCPUCORESUSED", num_processors)


    def loadVersion(self, suffix=""):
        print("load version file: " + str(self.version)[:-4] + suffix + ".ver")
        if suffix == "":
            self.Visum.LoadVersion(self.version)
        else:
            self.Visum.LoadVersion(str(self.version)[:-4] + suffix + ".ver")

    def saveVersion(self, suffix=""):
        print("save version file: " + str(self.version)[:-4] + suffix + ".ver")
        if suffix == "":
            self.Visum.SaveVersion(self.version)
        else:
            self.Visum.SaveVersion(str(self.version)[:-4] + suffix + ".ver")

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

    def readZMX2array(self, zmxfileName):
        """
        Read a ZMX file to a mutable numpy array.

        The readZMX function reads to immutable tuple, which obviously doesn't
        work if future transformations are needed.
        :param zmxfileName: The path to the zmx file
        :return: A list containing [0] a np.array, [1] a list of zone names, and
        [2] the name of the matrix.
        """

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
        for i in range(1, rows+1):
            fileNameRow = "row_" + str(i)
            data = z.read(fileNameRow)
            if i == 1:  # create the array
                mat = np.array([struct.unpack(">" + "f" * rows, data)])
            else:  # add a row to the array
                mat = np.vstack([mat, struct.unpack(">" + "f" * rows, data)])

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

    def stringConcatenate(self, data, zones=True):

        rows  = len(data)
        cols = len(data[0])
        fileArray = list()
        strConc = ''

        for col in range(0,cols):
            strConc = str(data[0][col])
            for row in range(1,rows):
                strConc = strConc + ',' + str(data[row][col])

            fileArray.append(strConc)

        if(zones):
            self.fields[0] = 'Azone'
        else:
            self.fields[0] = 'NO'

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

    def push_csv_py27(self,csvFileName, attnames, data): # for python 2.7
        print("save csv file: " + csvFileName)
        with open(csvFileName, 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter =',')
            writer.writerow(attnames)
            writer.writerows(data)

    def cleanZonalDataOfWorldZones(self,file_table):
        cleaned_table = []
        for row in file_table:
            if (len(cleaned_table) == 0) or (not (int(float(row[0])) in self.externalStationList)):
                cleaned_table.append(row)
        return cleaned_table

    def cleanModelInputs(self):
        print('Remove Non-Year Inputs')

        lineID = VisumHelpers.GetMulti(self.Visum.Net.LineRoutes, "ID")
        inYear = VisumHelpers.GetMulti(self.Visum.Net.LineRoutes, "NET")
        for i in range(len(inYear)):
          if inYear[i] == 0:
            self.Visum.Net.RemoveLineRoute(self.Visum.Net.LineRoutes.ItemByID(lineID[i]))

    def copyActivityLocations(self):
        if(self.copyToPreviousYear == "true"):
            shutil.copy(self.currentActivity, self.previousActivity)

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
            state_label = [field for field in self.fields if field.upper() == 'STATE']
            county_label = [field for field in self.fields if field.upper() == 'COUNTY']
            for fieldCounter in range(0, len(self.fields)):
                self.field = self.fields[fieldCounter]
                # if BZONE then get MainZoneNo
                if self.field.upper() == 'BZONE':
                    self.field = zoneAlias.get(self.field.upper())
                if self.field.upper() == 'STATEFIPS':
                    data.append([state_fips.get(state_name) for state_name in
                                 VisumHelpers.GetMulti(self.Visum.Net.Zones, state_label[0])])
                elif self.field.upper() == 'COUNTYFIPS':
                    data.append([county_fips.get(county_name) for county_name in
                                 VisumHelpers.GetMulti(self.Visum.Net.Zones, county_label[0])])
                elif self.field.upper() == 'GRIDACRES':
                    data.append([AreaMi2 * Mi2Acres for AreaMi2 in VisumHelpers.GetMulti(self.Visum.Net.Zones, 'AREAMI2')])
                elif self.field.upper() == 'AREASQFT':
                    data.append([AreaMi2 * 52800 * 5280 for AreaMi2 in VisumHelpers.GetMulti(self.Visum.Net.Zones, 'AREAMI2')])
                else:
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Zones, self.field))

            #Make list of lists (rows)
            if not fileName.lower().find('allzones') > -1:
                fileTable = self.cleanZonalDataOfWorldZones(self.stringConcatenate(data))
            elif fileName.lower().find('allzones') > -1:
                # Replace 0s in Bzone with external stations
                data[1] = [bzone if bzone > 0 else data[0][index] for index, bzone in enumerate(data[1])]
                data[0].extend(["6001.0","6002.0","6003.0","6004.0","6005.0","6006.0"])
                data[1].extend(["6001.0","6002.0","6003.0","6004.0","6005.0","6006.0"])
                data[2].extend(["WM","WM","WM","WM","WM","WM"])
                data[3].extend(["WM","WM","WM","WM","WM","WM"])
                data[4].extend([0,0,0,0,0,0])
                data[5].extend(["WM","WM","WM","WM","WM","WM"])
                data[6].extend([0,0,0,0,0,0])
                data[7].extend(["WM","WM","WM","WM","WM","WM"])
                fileTable = self.stringConcatenate(data)

            #Correct FloorspaceInventory field names
            if fileName.lower().find('floorspaceinventory') > -1:
                fileTable[0] = [s.replace("_"," ") for s in fileTable[0]]
            #Reformat FloorspaceI table
            elif fileName.lower().find('floorspacei') > -1:
                self.headers = ['taz', 'commodity', 'quantity']
                outTable = []
                for i in range(1,len(fileTable)):
                  row = fileTable[i]
                  for j in range(len(row)):
                    azone = row[0]
                    sptype = self.fields[j].replace("_"," ")
                    value = row[j]
                    if j > 0:
                      outTable.append([str(int(float(azone))), sptype, value])
                fileTable = outTable
                fileTable.insert(0, self.headers)


            if fileName.lower().find('activityconstraintsi') > -1:

                #read world markets file (fix)
                #self.headers = ["sctg01_fkp_lvsk_expt","sctg01_fkp_lvsk_impt","sctg02_fkp_agri_cereal_expt","sctg02_fkp_agri_cereal_impt","sctg03_fkp_agri_other_expt","sctg03_fkp_agri_other_impt","sctg04_fkp_feed_expt","sctg04_fkp_feed_impt","sctg05_fkp_food_meat_expt","sctg05_fkp_food_meat_impt","sctg06_fkp_agri_grain_expt","sctg06_fkp_agri_grain_impt","sctg07_fkp_food_prep_expt", "sctg07_fkp_food_prep_impt","sctg08_fkp_food_alc_expt","sctg08_fkp_food_alc_impt","sctg10_cms_clay_expt","sctg10_cms_clay_impt","sctg11_cms_sand_expt","sctg11_cms_sand_impt", "sctg13_cms_mine_nonmet_expt","sctg13_cms_mine_nonmet_impt","sctg14_cms_mine_met_expt","sctg14_cms_mine_met_impt","sctg15_pcc_coal_expt","sctg15_pcc_coal_impt","sctg16_pcc_petr_crude_expt","sctg16_pcc_petr_crude_impt","sctg17_pcc_fuel_expt","sctg17_pcc_fuel_impt","sctg18_pcc_petr_oil_expt","sctg18_pcc_petr_oil_impt","sctg19_pcc_coal_prod_expt","sctg19_pcc_coal_prod_impt","sctg20_pcc_chem_basic_expt","sctg20_pcc_chem_basic_impt","sctg21_pcc_chem_pharma_expt","sctg21_pcc_chem_pharma_impt","sctg22_pcc_chem_fert_expt","sctg22_pcc_chem_fert_impt","sctg23_pcc_chem_prod_expt","sctg23_pcc_chem_prod_impt","sctg24_pcc_petr_plast_expt","sctg24_pcc_petr_plast_impt","sctg25_fwp_logs_expt","sctg25_fwp_logs_impt","sctg26_fwp_wood_expt","sctg26_fwp_wood_impt","sctg27_ppp_papr_puplp_expt","sctg27_ppp_papr_puplp_impt","sctg28_ppp_papr_paper_expt","sctg28_ppp_papr_paper_impt","sctg29_ppp_papr_print_expt","sctg29_ppp_papr_print_impt","sctg30_oth_clth_expt","sctg30_oth_clth_impt","sctg31_cms_min_expt","sctg31_cms_min_impt","sctg32_mit_metl_base_expt","sctg32_mit_metl_base_impt","sctg33_mit_metl_prod_expt","sctg33_mit_metl_prod_impt","sctg34_mit_mach_expt","sctg34_mit_mach_impt","sctg35_mit_elct_expt","sctg35_mit_elct_impt","sctg36_mitTRan_expt","sctg36_mitTRan_impt","sctg37_mit_instTRansp_expt","sctg37_mit_instTRansp_impt","sctg38_mit_inst_prec_expt","sctg38_mit_inst_prec_impt","sctg39_oth_furn_expt","sctg39_oth_furn_impt","sctg40_oth_misc_expt","sctg40_oth_misc_impt","sctg41_waste_scrap_expt","sctg41_waste_scrap_impt"]
                self.headers = self.loadCSV(self.worldMarketFieldnames)[1]["FIELDNAME"]
                self.poiColumns = []

                for id in self.headers:
                    self.poiColumns.append(VisumHelpers.GetMulti(self.Visum.Net.POICategories.ItemByKey(7).POIs, id))

                wmrkts = VisumHelpers.GetMulti(self.Visum.Net.POICategories.ItemByKey(7).POIs, "Code");

                outTable = []
                for z in range(len(wmrkts)):
                    for i in range(len(self.headers)):
                        row = [wmrkts[z], self.headers[i], self.poiColumns[i][z]]
                        outTable.append(row)

                for i in range(1,len(fileTable)):
                  row = fileTable[i]
                  for j in range(len(row)):
                    azone = row[0]
                    attr = self.fields[j]

                    value = row[j]
                    if j > 0:
                      outTable.append([azone, attr, value])
                #add column headers
                fileTable = outTable
                fileTable.insert(0, ["Azone", "activity", "quantity"])



            if fileName.lower().find('activitytotalsi') > -1:
                outTable = []

                # self.actTotals = self.loadCSV(self.activityTotals)[1]
                self.actTotalsOther = self.loadCSV(self.activityTotalsOther)[1]

                # WORLD IMPORTS/EXPORTS TOTALS
                self.headers = self.loadCSV(self.worldMarketFieldnames)[1]["FIELDNAME"]
                self.poiColumns = []
                for id in self.headers:
                    self.poiColumns.append(VisumHelpers.GetMulti(self.Visum.Net.POICategories.ItemByKey(7).POIs, id))
                wmrkts = VisumHelpers.GetMulti(self.Visum.Net.POICategories.ItemByKey(7).POIs, "Code");
                for i in range(len(self.headers)):
                    outTable.append([self.headers[i], sum(self.poiColumns[i])])

                # LOCAL ACTIVITY TOTALS
                # AA requires a very particular capitalization, so this is a hack for that :/
                # This means that a seed ActivityTotalsI.csv file is required
                for i in self.fields:
                    if i.lower().find('zone') < 0:
                        activity = VisumHelpers.GetMulti(self.Visum.Net.Zones, i)
                        value = sum(activity)
                        # Lowercase words after the first underscore
                        if i.find('_') > 0:
                            l = i.split('_')
                            l[1] = '_'.join(l[1:])
                            l = l[:2]
                            name = '_'.join([item.lower() if l.index(item) > 0 else item for item in l])
                        elif i.startswith('HH'):
                            name = i
                        if not any(e[0] == name for e in outTable):
                            outTable.append([name, value])

                tempAttr = []
                tempAttr.extend(self.actTotalsOther['Activity'])

                # IGNORED ACTIVITIES (JUST USE EXISTING DATA) DONE LAST TO COMPLEMENT
                for i in tempAttr:
                    totalsInd = self.actTotalsOther['Activity'].index(i)
                    if not any(e[0] == i for e in outTable):
                        outTable.append([i, self.actTotalsOther["TotalAmount"][totalsInd]])

                # add column headers
                fileTable = outTable
                fileTable.insert(0, ["Activity", "TotalAmount"])
                self.writeCSV(self.activityTotals, fileTable)

            #write resulting file
            elif fileName.lower().find('agforestfloorspace') > -1:
                newFileTable = []
                counter = 0
                for r in fileTable:
                    if counter > 0 and float(r[1]) > 0.0:
                        line = []
                        line.extend([r[0], "AgForest", "FLR Logging", r[1]])
                        newFileTable.append(line)
                    if counter > 0 and float(r[2]) > 0.0:
                        line = []
                        line.extend([r[0], "AgForest", "FLR Agriculture", r[2]])
                        newFileTable.append(line)
                    counter +=1
                newFileTable.insert(0, ["Azone","FLRType","FLRName","BldgMSQFT"])



                self.writeCSV(self.agForestFloorspace, newFileTable)
            else:
                self.writeCSV(fileName, fileTable)

        #set truck PCU factor
        print("Set truck PCU factor for class" + truckDemandClass + " : " + str(truckPCU))
        self.Visum.Net.TSystems.ItemByKey(truckDemandClass).SetAttValue("PCU", truckPCU)


    def assignLinkAzone(self):
      
      print('Populate Link AZONE field with nearest ZONE')
      
      #get link UDAs
      udaNames = []
      for i in self.Visum.Net.Links.Attributes.GetAll:
        if i.category=="User-defined attributes":
          udaNames.append(i.Name)
      if "AZONE" not in udaNames:
        self.Visum.Net.Links.AddUserDefinedAttribute("AZONE","AZONE","AZONE",2,3) #1=int, 2=float, 5=text
    
      #assign each node to a zone and then select zone for closest ANODE or BNODE of link
      zone_no     = VisumHelpers.GetMulti(self.Visum.Net.Zones, "NO")
      zone_xcoord = VisumHelpers.GetMulti(self.Visum.Net.Zones, "XCOORD")
      zone_ycoord = VisumHelpers.GetMulti(self.Visum.Net.Zones, "YCOORD")
      
      link_fnodex = VisumHelpers.GetMulti(self.Visum.Net.Links, "FROMNODE\XCOORD")
      link_fnodey = VisumHelpers.GetMulti(self.Visum.Net.Links, "FROMNODE\YCOORD")
      link_tnodex = VisumHelpers.GetMulti(self.Visum.Net.Links, "TONODE\XCOORD")
      link_tnodey = VisumHelpers.GetMulti(self.Visum.Net.Links, "TONODE\YCOORD")
      azones      = VisumHelpers.GetMulti(self.Visum.Net.Links, "AZONE")
      
      for i in range(len(link_fnodex)):
        fn_x = np.repeat(link_fnodex[i], len(zone_no))
        fn_y = np.repeat(link_fnodey[i], len(zone_no))
        tn_x = np.repeat(link_tnodex[i], len(zone_no))
        tn_y = np.repeat(link_tnodey[i], len(zone_no))
        fn_dist = ((fn_x-zone_xcoord)**2 + (fn_y-zone_ycoord)**2)**0.5
        tn_dist = ((tn_x-zone_xcoord)**2 + (tn_y-zone_ycoord)**2)**0.5
        azones[i] = int(zone_no[np.argmin(fn_dist)] if (np.min(fn_dist) < np.min(tn_dist)) else zone_no[np.argmin(tn_dist)])

      VisumHelpers.SetMulti(self.Visum.Net.Links, "AZONE", azones)


    def assignLinkImpPenalty(self):
      
      print('Populate Link IMP_Penalty field with nearest penalties defined in global properties')
      
      #get link UDAs
      udaNames = []
      for i in self.Visum.Net.Links.Attributes.GetAll:
        if i.category=="User-defined attributes":
          udaNames.append(i.Name)
      if "IMP_Penalty" not in udaNames:
        self.Visum.Net.Links.AddUserDefinedAttribute("IMP_Penalty","IMP_Penalty","IMP_Penalty",2,3, DefVal=self.imp_penalty_other_roads) #1=int, 2=float, 5=text
    
      #assign each node to a zone and then select zone for closest ANODE or BNODE of link
      link_fn_class     = VisumHelpers.GetMulti(self.Visum.Net.Links, "PLANNO")
      imp_penalty = VisumHelpers.GetMulti(self.Visum.Net.Links, "IMP_Penalty")
      
      for i in range(len(link_fn_class)):
        if link_fn_class[i] in [1,11]:
            imp_penalty[i] = self.imp_penalty_interstate
        elif link_fn_class[i] in [2,12]:
            imp_penalty[i] = self.imp_penalty_principal_arterial
        elif link_fn_class[i] in [4,14]:
            imp_penalty[i] = self.imp_penalty_minor_arterial
        else:
            imp_penalty[i] = self.imp_penalty_other_roads

      VisumHelpers.SetMulti(self.Visum.Net.Links, "IMP_Penalty", imp_penalty)

    def createVizOutput(self):
        
        #write SWIM input files
        print('Create SWIM Link outputs for Viz')
        headers, fieldDictionary = self.loadCSV(self.swimVizOutputs)
        fileNames = list(set(fieldDictionary[headers[1]]))

        for fileIndex in range(0,len(fileNames)):
            fileName = fileNames[fileIndex]
            print("Create Viz output file:" + fileName)

            #get fields for file
            listRecords = [item for item in range(0,len(fieldDictionary[headers[1]]))
                           if fieldDictionary[headers[1]][item] == fileName]
            self.fields = [fieldDictionary[headers[0]][i] for i in listRecords]

            #get required attributes table from Visum
            data = list()
            for fieldCounter in range(0, len(self.fields)):
                self.field = self.fields[fieldCounter]
                #some fields with commas need to be refactored
                if self.field.lower().find('oldmodes') > -1 or self.field.lower().find('tsysset') > -1:
                    newTsysset = []
                    tsysset = VisumHelpers.GetMulti(self.Visum.Net.Links, self.field)
                    for t in tsysset:
                        newTsysset.append(str(t).replace(',',';'))
                    data.append(newTsysset);

                elif self.field.lower().find('anode') > -1 and not (self.field.lower().find('x') > -1 or self.field.lower().find('y') > -1):
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Links, 'FromNodeNo'))
                elif self.field.lower().find('bnode') > -1 and not (self.field.lower().find('x') > -1 or self.field.lower().find('y') > -1):
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Links, 'ToNodeNo'))

                elif self.field.lower().find('anodex') > -1:
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Links, 'FromNode\XCoord'))
                elif self.field.lower().find('anodey') > -1:
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Links, 'FromNode\YCoord'))
                elif self.field.lower().find('bnodex') > -1:
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Links, 'ToNode\XCoord'))
                elif self.field.lower().find('bnodey') > -1:
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Links, 'ToNode\YCoord'))

                else:
                    data.append(VisumHelpers.GetMulti(self.Visum.Net.Links, self.field))

            self.writeCSV(fileName, self.stringConcatenate(data, False))
        
        #write SWIM VIZDB count locations
        print('Create SWIM count locations for Viz')
        #data = list()
        #self.fields = map(lambda x: x.ID, self.Visum.Net.CountLocations.Attributes.GetAll)
        #for aField in self.fields: # self.fields required by stringConcatenate 
        #    data.append(VisumHelpers.GetMulti(self.Visum.Net.CountLocations, aField)) # VisumHelpers no longer worked with count locations for Visum 2020
        #self.writeCSV(countLocationsFileName, self.stringConcatenate(data, False))

        atts = self.Visum.Net.CountLocations.Attributes.GetAll
        attnames = []
        
        for att in atts:
            #attnames.append(att.ID)
            try:
                self.Visum.Net.CountLocations.GetMultiAttValues(att.ID)
                print ("success in getting attribute: {}".format(att.ID))
                attnames.append(att.ID)
            except:
                print ("failed to get attribute: {}".format(att.ID))

        data = map(list, self.Visum.Net.CountLocations.GetMultipleAttributes(attnames))
        #data = [ *map(list,self.Visum.Net.CountLocations.GetMultipleAttributes(attnames)) ] 

        self.push_csv_py27(countLocationsFileName, attnames, data)

    #read matrices in Python and then insert into Visum
    def insertMatrixInVisum(self, ODmode, start, end):

        print("insert demand matrices in Visum: " + ODmode)
        headers, fieldDictionary = self.loadCSV(self.zmxInputfileNames)
        fileNames = list(fieldDictionary[headers[0]])

        for i in range(start-1,end):

            #read zmx file
            fileName = fileNames[i] + '.zmx'
            mat, zoneNames, name = self.readZMX(fileName)
            print("matrix %i sum %i" % (i+1, sum(map(sum, mat))))

            #add a fraction of a trip to OD 2->3 to ensure skimming and assignment run
            mat = np.array(mat)
            mat[1,2] = mat[1,2] + 0.001

            #post to bank
            VisumHelpers.SetODMatrix(self.Visum, i+1, mat)

    #create seed demand for seed skim creation
    def insertSeedMatricesInVisum(self):
        for i in range(hwyDemandMatrices[0], hwyDemandMatrices[1]):
            mat = np.zeros([len(self.zoneNames),len(self.zoneNames)], dtype=np.float64)
            for j in range(len(mat)):
                mat[j] = [0.0001] * len(mat)
            print("matrix %i sum %i" % (i, sum(map(sum, mat))))
            VisumHelpers.SetODMatrix(self.Visum, i, mat)

    #delete skim matrices with indicated start and end no.s of matrices in Visum
    def deleteSkimMatrices(self, start, end):

        print("delete skim matrices in Visum")
        for matInd in range(start,end+1):
            #remove skim matrix
            matRefObject = self.Visum.Net.Matrices.ItemByKey(matInd)
            self.Visum.Net.RemoveMatrix(matRefObject)

    def importExportFields(self, impFieldName, expFieldName):
        print("......copy data from " + impFieldName + " to " + expFieldName)

        impField = VisumHelpers.GetMulti(self.Visum.Net.Links, impFieldName)
        expField = VisumHelpers.SetMulti(self.Visum.Net.Links, expFieldName, impField)

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

        #mini model run if less than max mini size
        global externalStation
        global bzoneRange
        if len(self.zoneNo) < maxMiniModelZones:
          bzoneRange = bzoneRangeMiniModel
          externalStation = externalStationMiniModel

        self.mainZoneNo = VisumHelpers.GetMulti(self.Visum.Net.Zones, b_zone)
        self.mainZoneNo = map(int, self.mainZoneNo)
        self.mainZoneNo = [bzone if bzone > 0 else self.zoneNo[index] for index, bzone in enumerate(self.mainZoneNo)]
        self.uniqueMainZoneNo = VisumMatrices.unique(self.mainZoneNo)
        self.uniqueMainZoneNo.sort()
        self.uniqueMainZoneNo = list(self.uniqueMainZoneNo)

        #delete external station no.s from uniqueMainZoneNo
        self.uniqueMainZoneNo = [i for j, i in enumerate(self.uniqueMainZoneNo) if j not in externalStation]

        self.worldMainZoneNo = self.uniqueMainZoneNo + worldMarketList

        self.uniqueMainZoneNo = self.uniqueMainZoneNo + list(set(worldMarketList))
        self.mainZoneNames = map(str, self.uniqueMainZoneNo)


    def matrixCorrection(self, matCount, mat, headers, fieldDictionary):

        #mini model run if less than max mini size
        global externalStation
        global bzoneRange
        if len(self.zoneNo) < maxMiniModelZones:
          bzoneRange = bzoneRangeMiniModel
          externalStation = externalStationMiniModel

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

        #sort the worldfieldDictionary by ExternalStation and WorldMarket
        [worldfieldDictionary[worldheaders[0]], worldfieldDictionary[worldheaders[1]], worldfieldDictionary[worldheaders[2]],
         worldfieldDictionary[worldheaders[3]]] = [list(x) for x in zip(*sorted(zip(worldfieldDictionary[worldheaders[0]],
                                                                                    worldfieldDictionary[worldheaders[1]],
                                                                                    worldfieldDictionary[worldheaders[2]],
                                                                                    worldfieldDictionary[worldheaders[3]]),
                                                                                key=lambda pair: (pair[1], pair[0])))]

        #find indices of duplicate ExtStation
        duplicate_indices = [idx for idx, item in enumerate(worldfieldDictionary[worldheaders[1]]) if
                             item in worldfieldDictionary[worldheaders[1]][:idx]]

        #remove duplicate ExtStation based on their indices
        for item in range(0, len(worldheaders)):
            worldfieldDictionary[worldheaders[item]] = [i for j, i in
                                                            enumerate(worldfieldDictionary[worldheaders[item]]) if
                                                            j not in duplicate_indices]

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
            print('writing skim matrix ' + fileName + ': ' + str(VisumMatIndex))
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
        
        #add AREA as well
        areas = VisumHelpers.GetMulti(self.Visum.Net.Zones, "AREAMI2")
        # Changing pointer to Visums AREAMI2 from User attribute AREASQMI, so this line is no longer needed
        #areas = [item/(5280**2) for item in areas] #from sq ft to miles
        self.service_data["AREA"] = areas
        self.service_data["P2EDEN"] = [0]*len(self.service_data["AREA"])

    #calculate service area data for LTF
    def calcServiceAreaData(self):

        #get zone area
        areas = VisumHelpers.GetMulti(self.Visum.Net.Zones, "AREAMI2")
        # Changing pointer to Visums AREAMI2 from User attribute AREASQMI, so this line is no longer needed
        #areas = [item/(5280**2) for item in areas] #from sq ft to miles
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
        o = 1
        d = 17
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
      peak_far = np.array(VisumHelpers.GetMatrix(self.Visum, ivtPeakIntraSkimNum + 7))
      offpeak_far = np.array(VisumHelpers.GetMatrix(self.Visum, ivtOffpeakIntraSkimNum + 7))
      for i in range(len(zoneFare)):
        for j in range(len(zoneFare)):
          key = zoneFare[i] + " " + zoneFare[j]
          if peak_far[i][j]==0: #0=avail;999999=NA
            if key in odKey:
              peak_far[i][j] = fieldDict['Fare'][odKey.index(key)]
              offpeak_far[i][j] = fieldDict['Fare'][odKey.index(key)]

          if offpeak_far[i][j]==0: #0=avail;999999=NA
            if key in odKey:
              peak_far[i][j] = fieldDict['Fare'][odKey.index(key)]
              offpeak_far[i][j] = fieldDict['Fare'][odKey.index(key)]

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
      peak_far = np.array(VisumHelpers.GetMatrix(self.Visum, ivtPeakInterSkimNum + 6))
      offpeak_far = np.array(VisumHelpers.GetMatrix(self.Visum, ivtOffpeakInterSkimNum + 6))
      for i in range(len(zoneFare)):
        for j in range(len(zoneFare)):
          fare_ij = distmat[i][j] * (alpha * distmat[i][j]**beta)
          if peak_far[i][j]==0: #0=avail;999999=NA
            peak_far[i][j] = fare_ij
          if offpeak_far[i][j]==0:
            offpeak_far[i][j] = fare_ij

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
            self.Visum.Net.AddMatrix (ivtMatNums[index])
            self.Visum.Net.Matrices.ItemByKey(ivtMatNums[index]).SetAttValue("Name", ivtFileNames[index])
            VisumHelpers.SetSkimMatrix(self.Visum, ivtMatNums[index], ivtskim)
            self.Visum.Net.AddMatrix (ovtMatNums[index])
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
        ivt = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum))
        owt = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 1))
        twt = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 2))
        wkt = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 3))
        act = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 4))
        egt = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 5))
        ntr = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + 6))
        skm_util = (ivt_coeff * ivt + ovt_coeff * (owt + twt + wkt + act + egt + (ntr * transferEqMin)))
        skm_util[ivt==NA] = NA
        del(ivt, owt, twt, wkt, act, egt, ntr)

        #get local transit function skim matrices
        ltf_ivt = np.array(VisumHelpers.GetMatrix(self.Visum, ltfIvtMatNum))
        ltf_ovt = np.array(VisumHelpers.GetMatrix(self.Visum, ltfOvtMatNum))
        ltf_util = (ivt_coeff * ltf_ivt + ovt_coeff * ltf_ovt)
        ltf_util[ltf_ivt==NA] = NA
        del(ltf_ivt, ltf_ovt)

        #Determine best option
        ltfBetter = ltf_util < skm_util

        #get intracity skimmed transit skims
        ivt = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum))

        #Turn off the worst option by OD pair
        ivt[np.logical_and(ltf_util != NA, ltfBetter)] = NA
        skimOffsets = [1,2,3,4,5,6,7] #other skims
        for i in skimOffsets:
          mat = np.array(VisumHelpers.GetMatrix(self.Visum, skimMatrixStartNum + i))
          mat[np.logical_and(ltf_util != NA, ltfBetter)] = NA
          VisumHelpers.SetSkimMatrix(self.Visum, skimMatrixStartNum + i, mat)
          del(mat)

        #get local transit function skim matrices
        ltf_ivt = np.array(VisumHelpers.GetMatrix(self.Visum, ltfIvtMatNum))
        ltf_ovt = np.array(VisumHelpers.GetMatrix(self.Visum, ltfOvtMatNum))

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

        print('build transit connectors using LTF skims')

        #get stop attributes
        stopNo       =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "No", False)
        stopX        =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "XCoord", False)
        stopY        =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "YCoord", False)
        stopNodeNo   =  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "NodeNo", False)
        stopLineModes=  VisumHelpers.GetMulti(self.Visum.Net.StopPoints, "CONCATENATE:LINEROUTES\TSYSCODE", False)
        stopZone     =  [0] * len(stopNo) #will be calculated below

        #get zone attributes
        zoneNo       =  VisumHelpers.GetMulti(self.Visum.Net.Zones, "No", False)
        zoneX        =  VisumHelpers.GetMulti(self.Visum.Net.Zones, "XCoord", False)
        zoneY        =  VisumHelpers.GetMulti(self.Visum.Net.Zones, "YCoord", False)
        zoneStops    =  [[]] * len(zoneNo) #will be calculated below

        #determine each stop's zone
        for i in range(len(stopNo)):
            max_dist = 99999999
            for j in range(len(zoneNo)):
                dist = self.calcDist(stopX[i], zoneX[j], stopY[i], zoneY[j])
                if dist <= max_dist:
                    stopZone[i] = zoneNo[j]
                    max_dist = dist

        #determine nearest stops for each zone
        for i in range(len(zoneNo)):

            #store distances
            dists = []
            for k in range(numTransitConnectors):
              heapq.heappush(dists, (-99999999, 0)) #negative distance, stopNo

            for j in range(len(stopNo)):

                #skip if air stop
                if 'n' not in stopLineModes[j]:

                  dist = self.calcDist(zoneX[i], stopX[j], zoneY[i], stopY[j])
                  for entry in dists:
                    if (-1 * dist) > entry[0]:
                      heapq.heappushpop(dists, (-dist, stopNo[j]))
                      break

            #store stopNos by zone
            zoneStops[i] = []
            for entry in dists:
              zoneStops[i].append(entry[1])

        #get local transit function skim matrices
        ivt = VisumHelpers.GetMatrix(self.Visum, ivt_matrix_num) #ltf ivt
        ovt = VisumHelpers.GetMatrix(self.Visum, ovt_matrix_num) #ltf ovt

        #create connectors
        self.Visum.Graphic.ShowMinimized()
        for i in range(len(zoneNo)):
          for j in range(len(zoneStops[0])):

            #create connector if needed
            zone = self.Visum.Net.Zones.ItemByKey(zoneNo[i])
            node = self.Visum.Net.Nodes.ItemByKey(stopNodeNo[stopNo.index(zoneStops[i][j])])

            if self.Visum.Net.Connectors.ExistsByKey(node, zone):
                sCon = self.Visum.Net.Connectors.SourceItemByKey(zone, node)
                dCon = self.Visum.Net.Connectors.DestItemByKey(node, zone)
            else:
                self.Visum.Net.AddConnector(zone, node)
                sCon = self.Visum.Net.Connectors.SourceItemByKey(zone, node)
                dCon = self.Visum.Net.Connectors.DestItemByKey(node, zone)

            #get IVT and OVT for OD pair
            fz_index = zoneNo.index(zoneNo[i])
            tz_index = zoneNo.index(stopZone[stopNo.index(zoneStops[i][j])])
            ivt_od = ivt[fz_index][tz_index]
            ovt_od = ovt[fz_index][tz_index]

            #set connector cost - take min of walk time or LTF time

            #walk time
            nodeX = stopX[stopNo.index(zoneStops[i][j])]
            nodeY = stopY[stopNo.index(zoneStops[i][j])]
            dist = self.calcDist(zoneX[i], nodeX, zoneY[i], nodeY)
            walk_cost = (dist / 5280) * (60 / walkSpeed) * 60 #from min to sec

            #LTF time
            if ivt_od != NA:
              ltf_cost = (ivt_coeff * ivt_od + ovt_coeff * ovt_od) * 60 #from min to sec
            else:
              ltf_cost = NA

            cost = min(walk_cost, ltf_cost)

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


    def createVolumeFactor(self):
        #get empty list
        volumeFactors = VisumHelpers.GetMulti(self.Visum.Net.Links, "PK_VOL_FACTOR")

        factor = self.calcVolumeFactor("ampeak")
        for i in range(len(volumeFactors)):
            volumeFactors[i] = factor
        VisumHelpers.SetMulti(self.Visum.Net.Links, "PK_VOL_FACTOR", volumeFactors)

        factor = self.calcVolumeFactor("mdoffpeak")
        for i in range(len(volumeFactors)):
            volumeFactors[i] = factor
        VisumHelpers.SetMulti(self.Visum.Net.Links, "OP_VOL_FACTOR", volumeFactors)

        factor = self.calcVolumeFactor("pmpeak")
        for i in range(len(volumeFactors)):
            volumeFactors[i] = factor
        VisumHelpers.SetMulti(self.Visum.Net.Links, "PM_VOL_FACTOR", volumeFactors)

        factor = self.calcVolumeFactor("ntoffpeak")
        for i in range(len(volumeFactors)):
            volumeFactors[i] = factor
        VisumHelpers.SetMulti(self.Visum.Net.Links, "NT_VOL_FACTOR", volumeFactors)

    def recomputeWMLS(self, mcls, tt, di, purpose, auto_suff, income):
        """
        Recompute World Market Log Sums

        The function calculates logsums for beta zone-world market
        pairs based on highway travel time and distance, in addition to
        tour mode choice parameters based on an income level and
        trip purpose. The function overwrites the input logsum matrix, adjusting
        only the values to world market zones.

        :param mcls: mode choice logsum matrix to be modified
        :param tt: auto travel time skim used for external market
        :param di: auto distance skim used for external market
        :param purpose: purpose from the mode choice model, 0:7 (w = 1, etc)
        :param auto_suff: one of '0', 'I', 'S'
        :param income: one of 'Hi', 'Med', 'Low'
        :return: NULL, writes a modified version of `mcls`
        """
        
        # read utility coefficients
        tourmodechoiceparams = self.loadCSV(self.tourmodechoiceparams)
        bcost = float(tourmodechoiceparams[1]["cost" + income][purpose]) * float(self.auto_op_cost)
        apass = float(tourmodechoiceparams[1]["passAuto" + auto_suff][purpose])
        btime = float(tourmodechoiceparams[1]["ivt"][purpose])
        mnest = float(tourmodechoiceparams[1]["nest"][purpose])

        # load mcls matrix that needs to be recalculated
        mcls_zmx = self.readZMX2array(mcls)
        mcls_mat = mcls_zmx[0]
        mcls_zones = mcls_zmx[1].split(",")

        # load time and distance matrices to external zones
        tt_zmx = self.readZMX(tt)
        tt_mat = tt_zmx[0]
        tt_zones = tt_zmx[1].split(",")

        di_zmx = self.readZMX(di)
        di_mat = di_zmx[0]
        di_zones = di_zmx[1].split(",")

        for zi in mcls_zones:
            for zj in mcls_zones:
                # if neither i nor j is world market, just keep it as it is.
                if zi in worldMarketZones or zj in worldMarketZones:
                    time = tt_mat[tt_zones.index(zi)][tt_zones.index(zj)]
                    dist = di_mat[di_zones.index(zi)][di_zones.index(zj)]
                    #simplified utility equations
                    udrive = math.exp(2 * (time * btime + dist * bcost) / mnest)
                    upass  = math.exp(2 * (time * btime + dist * bcost + apass) / mnest)
                    if udrive == 0.0:
                        udrive = sys.float_info.min
                    if upass == 0.0:
                        upass = sys.float_info.min
                    new = mnest * math.log(udrive + upass)
                    mcls_mat[mcls_zones.index(zi)][mcls_zones.index(zj)] = new

        # write modified matrix in the place
        self.writeZMX(mcls, mcls_zones, mcls_mat)

    def editskims(self):
        """
        Edit the moodechoice logsum skims
        PT squeezes beta zone mcls skims and puts an illogical value of 100. This
        corrects that illogical value.
        """

        skim_dir = self.mclsmatrixdir
        tt = skim_dir + "betapkautofftime.zmx"
        di = skim_dir + "betapkautodist.zmx"

        # only recompute mode choice logsums for the main model; skim in small
        if len(self.zoneNo) > maxMiniModelZones:
            self.recomputeWMLS(skim_dir + 'b4mcls_beta.zmx', tt, di, 1, "I", "Med")
            self.recomputeWMLS(skim_dir + 'b5mcls_beta.zmx', tt, di, 1, "S", "Med")
            self.recomputeWMLS(skim_dir + 'b8mcls_beta.zmx', tt, di, 1, "S", "Hi")
            self.recomputeWMLS(skim_dir + 'c4mcls_beta.zmx', tt, di, 2, "I", "Med")
            self.recomputeWMLS(skim_dir + 'o4mcls_beta.zmx', tt, di, 5, "I", "Med")
            self.recomputeWMLS(skim_dir + 's4mcls_beta.zmx', tt, di, 3, "I", "Med")
            self.recomputeWMLS(skim_dir + 'w1mcls_beta.zmx', tt, di, 1, "0", "Low")
            self.recomputeWMLS(skim_dir + 'w4mcls_beta.zmx', tt, di, 1, "I", "Med")
            self.recomputeWMLS(skim_dir + 'w7mcls_beta.zmx', tt, di, 1, "S", "Hi")
        
    def createAirSkims(self):
        for timeperiod in timeMatNames:
            headers, airInputs = self.loadCSV(self.airInputsFile)
            timemat, timeNames, timematname = self.readZMX(timeperiod  + '.zmx')
            timemat = np.array(timemat)


            airdrv = np.zeros([len(timemat),len(timemat)], dtype=np.float64)
            airivt = np.zeros([len(timemat),len(timemat)], dtype=np.float64)
            airfar = np.zeros([len(timemat),len(timemat)], dtype=np.float64)
            airfwt = np.zeros([len(timemat),len(timemat)], dtype=np.float64)

            for i in range(len(timemat)):

                closestFromAirport = 0
                accessTime = 0
                minTime = 99999999.9
                for k in range(len(airInputs["From"])):
                    airportZoneFrom = airInputs["From"][k]
                    if int(airportZoneFrom) < len(timemat):
                        travelTime = float(timemat[i][int(airportZoneFrom)])
                        if(minTime > travelTime):
                            closestFromAirport = airportZoneFrom
                            accessTime = travelTime
                            minTime = travelTime

                for j in range(len(timemat)):

                    closestToAirport = 0
                    egressTime = 0
                    minTime = 99999999.9
                    for k in range(len(airInputs["To"])):
                        airportZoneTo = airInputs["To"][k]
                        if int(airportZoneTo) < len(timemat):
                            travelTime = float(timemat[j][int(airportZoneTo)])
                            if(minTime > travelTime):
                                closestToAirport = airportZoneTo
                                egressTime = travelTime
                                minTime = travelTime

                    index = 0

                    for k in range(len(airInputs["To"])):
                        if(airInputs["To"][k] == closestToAirport and airInputs["From"][k] == closestFromAirport):
                            index = k

                    #Index will be zero in cases where the closes 'from' and 'to' airpots are the same. In this case, air mode makes no sense and skims will be 999999
                    if(index == 0):
                        airdrv[i][j] = 99999999.9
                        airfar[i][j] = 99999999.9
                        airivt[i][j] = 99999999.9
                        airfwt[i][j] = 99999999.9
                    else:
                        airdrv[i][j] = accessTime + egressTime
                        airfar[i][j] = airInputs["FAR"][index]
                        airivt[i][j] = airInputs["IVT"][index]
                        airfwt[i][j] = airInputs["FWT"][index] + airInputs["AWT"][index]

            for air_skim in self.air_skims:
               air_skim += ".zmx"
               period_token = air_skim[:2]
               if air_skim.lower().find(period_token) > -1 and timeperiod.lower().find(period_token) > -1:
                   if air_skim.lower().find('dairdrv') > -1:
                       print('writing airdrv skim matrix ' + air_skim)
                       self.writeZMX(air_skim, self.zoneNames, airdrv)
                   elif air_skim.lower().find('dairfar') > -1:
                       print('writing airfar skim matrix ' + air_skim)
                       self.writeZMX(air_skim, self.zoneNames, airfar)
                   elif air_skim.lower().find('dairivt') > -1:
                       print('writing airivt skim matrix ' + air_skim)
                       self.writeZMX(air_skim, self.zoneNames, airivt)
                   elif air_skim.lower().find('dairfwt') > -1:
                       print('writing airfwt skim matrix ' + air_skim)
                       self.writeZMX(air_skim, self.zoneNames, airfwt)

    def calcVolumeFactor(self, timePeriod):

        print('calculate volume factor')

        volumeFactor = 0.0
        startHour = 0
        endHour = 0
        hours = 0

        #get time period definitions from property files
        if timePeriod.lower().find('ampeak') > -1:
            # startHour = self.ampeakstart
            # endHour = self.ampeakend
            # hours = (endHour + 41 - startHour) / 100.0 #convert 59th min to 100th min
            hours = self.ampeakcapadj

        elif timePeriod.lower().find('pmpeak') > -1:
            # startHour = self.pmpeakstart
            # endHour = self.pmpeakend
            # hours = (endHour + 41 - startHour) / 100.0
            hours = self.pmpeakcapadj

        elif timePeriod.lower().find('mdoffpeak') > -1:
            # startHour = self.mdoffpeakstart
            # endHour = self.mdoffpeakend
            # hours = (endHour + 41 - startHour) / 100.0
            hours = self.mdoffpeakcapadj

        elif timePeriod.lower().find('ntoffpeak') > -1:
            # startHour = self.ntoffpeakstart
            # endHour = self.ntoffpeakend
            # hours = (endHour + 41 + (2400 - startHour)) / 100.0
            hours = self.ntoffpeakcapadj

        else:
            print ( "time period specifed as: " + timePeriod + ", but must be either 'ampeak', 'mdoffpeak', 'pmpeak', or 'ntoffpeak'." )
            return -1

        volumeFactor = 0
        if(hours > 0):
            volumeFactor = hours

        #log results
        print( "calculated volume factor: " + str(volumeFactor))
        print( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

        return (volumeFactor)

    def writeZoneShapes(self):
        print('write zones shapes in VISUM order to WKT format')
        zoneNO = VisumHelpers.GetMulti(self.Visum.Net.Zones, "NO")
        zoneWKTSURFACE = VisumHelpers.GetMulti(self.Visum.Net.Zones, "WKTSURFACE")
        f = open("zone_shapes.wkt","w")
        f.write("NO;WKTSURFACE\n")
        for i in range(len(zoneNO)):
          f.write(str(zoneNO[i]) + ";" + zoneWKTSURFACE[i] + "\n")
        f.close()
 
############################################################
#Main entry point

if __name__== "__main__":

    #run modes can be:
    # inputs - create swim inputs
    # highway - run highway assignmentl
    # transit - run transit assignment
    property_file = sys.argv[1]
    mode = sys.argv[2].lower()
    if(len(sys.argv)>2):
        num_cores = int(sys.argv[3])
    else:
        num_cores = -1
    s = SwimModel(property_file)
    pdir = s.assigmentProcedureDirectory

    #create SWIM model inputs
    ######################################################
    if mode == 'inputs':
        s.copyVersion()
        if s.reSeedMatrices == 'True':
            #Auto seed skims
            s.startVisum()
            s.loadVersion()
            s.zonefieldVariables()
            s.insertSeedMatricesInVisum()
            procedure = s.initialAutoAssignmentProcedure
            s.loadProcedure(os.path.join(pdir,procedure))
            s.executeProcedure(os.path.join(pdir,procedure))
            s.writeHighwaySkimZMX(start=hwySkimMatrices[0], writeBetaMatrices=True)
            #s.saveVersion("_INIT_AUTO")
            s.closeVisum()

            #Transit seed skims
            s.startVisum()
            s.loadVersion()
            s.zoneServiceLookup()
            s.insertSeedMatricesInVisum()
            s.createLocalBusSkims()
            procedure = s.initialTransitAssignmentProcedure
            s.loadProcedure(os.path.join(pdir,procedure))
            s.executeProcedure(os.path.join(pdir,procedure))
            s.writeTransitSkimZMX(start=transitSkimMatrices[0])
            #s.saveVersion("_INIT_TRANSIT")
            s.closeVisum()

            #Air skims
            s.createAirSkims()
            
        s.startVisum()
        s.loadVersion()
        s.zonefieldVariables()
        s.assignLinkAzone()
        s.createModelInput()
        s.assignLinkImpPenalty()
        s.writeZoneShapes()
        s.saveVersion()
        s.closeVisum()
        s.editskims()  # recompute mode choice logsums to external beta zones
        s.copyActivityLocations()

    #path based highway assignment
    ######################################################
    if mode == 'highway':

        #multiclass private transport assignment
        num_cores = s.ta_num_processors if num_cores < 0 else num_cores
        s.startVisum(num_cores)
        s.loadVersion()
        s.insertMatrixInVisum('auto and truck', start=hwyDemandMatrices[0], end=hwyDemandMatrices[1])
        s.createVolumeFactor()
        s.saveVersion()
        s.closeVisum()

        #execute procedure file - four assigments - one for each time period
        #note NT output version file also contains link attributes with the total daily volumes
        s.startVisum(num_cores)
        s.loadVersion()
        s.loadProcedure(os.path.join(pdir,s.pathAllPeriodAssignmentProcedure))
        s.executeProcedure(os.path.join(pdir,s.pathAllPeriodAssignmentProcedure))
        s.saveVersion()
        s.closeVisum()

        #write PK and OP skim matrices
        s.startVisum(num_cores)
        s.loadVersion()
        s.zonefieldVariables()
        s.writeHighwaySkimZMX(start=hwySkimMatrices[0], writeBetaMatrices=True)
        s.deleteSkimMatrices(start=hwySkimMatrices[0], end=hwySkimMatrices[1])
        s.saveVersion()
        s.closeVisum()

        #delete other period skims in the output version files since the procedure accumulates
        #the skims from each time period (i.e. the fourth assignment has four sets of skims)
        savedVersionFileNames = ["OP_PATHS.ver","PM_PATHS.ver","NT_PATHS.ver"] #in par file
        for i in range(len(savedVersionFileNames)):
          s.startVisum()
          s.Visum.LoadVersion(savedVersionFileNames[i])
          s.deleteSkimMatrices(start=hwySkimMatrices[0], end=hwySkimMatrices[0]+(8*(i+1)))
          s.Visum.SaveVersion(savedVersionFileNames[i])
          s.closeVisum()
        
    #transit assignment
    ######################################################
    if mode == 'transit':
        
        #local bus ivt and ovt functions and premium connector build
        num_cores = s.tr_num_processors if num_cores < 0 else num_cores
        s.startVisum(num_cores)
        s.loadVersion()
        s.zoneServiceLookup()
        s.calcServiceAreaData()
        s.createLocalBusSkims()
        s.buildTransitConnectors(ivtMatNums[0], ovtMatNums[0], ivt_coeff, ovt_coeff)
        s.saveVersion("_TR")
        s.closeVisum()

        #intercity transit assignment
        s.startVisum(num_cores)
        s.loadVersion("_TR")
        s.insertMatrixInVisum('intercity transit', start=ldtDemandMatrices[0], end=ldtDemandMatrices[1])
        s.saveVersion("_TR")
        s.closeVisum()

        #load and execute procedure file
        procedure = s.intercityRailAssignmentProcedure
        s.startVisum(num_cores)
        s.loadVersion("_TR")
        s.loadProcedure(os.path.join(pdir,procedure))
        s.executeProcedure(os.path.join(pdir,procedure))
        s.saveVersion("_TR")
        s.closeVisum()

        #intracity transit assignment
        s.startVisum(num_cores)
        s.loadVersion("_TR")
        s.insertMatrixInVisum('intracity transit', start=sdtDemandMatrices[0], end=sdtDemandMatrices[1])
        s.saveVersion("_TR")
        s.closeVisum()

        #load and execute procedure file
        procedure = s.intracityRailAssignmentProcedure
        s.startVisum(num_cores)
        s.loadVersion("_TR")
        s.loadProcedure(os.path.join(pdir,procedure))
        s.executeProcedure(os.path.join(pdir,procedure))
        s.saveVersion("_TR")
        s.closeVisum()

        #Adjust skimmed SDT IVT and LTF IVT and OVT skims
        s.startVisum(num_cores)
        s.loadVersion("_TR")
        s.calcFareMatrices()
        s.adjustSkimsDueToLTF(isPeak=True)
        s.adjustSkimsDueToLTF(isPeak=False)
        s.saveVersion("_TR")
        s.closeVisum()

        #write skim matrices
        s.startVisum(num_cores)
        s.loadVersion("_TR")
        s.zonefieldVariables()
        s.writeTransitSkimZMX(start=transitSkimMatrices[0])
        s.createVizOutput()
        s.saveVersion("_TR")
        s.closeVisum()

        #copy air skims, because it needs to look like they've been run
        s.copyAirSkims()

    print("end model run - " + time.ctime())
