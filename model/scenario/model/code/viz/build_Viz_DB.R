
#script to build the swim vizualization db
#R --no-save < ../../build_viz_db.R > log.txt ALLZONESFILENAME="Z:/viz/allzones.csv" 
#  D211FILE="z:/network.d211" EXATTFILE="z:/extraAttribs.csv" SPATIALONLY=F
#Ben Stabler, stabler@pbworld.com, 070110

# If this script is run manually the use the following parameters
# allZonesFileName = "allzones.csv"
# genSpatialOnly = F
# extraAttribsFileName = "export_01_06.csv"
# networkFileName ="network.d211" 

#updated 03/05/14 bts - allows for no ALD outputs and no longer reads TS outputs

#######################################################################

library(RSQLite)

#########################################################################
#parameters
#########################################################################

#create new database file (deletes existing as well)
createNewDBFile = T

#income bins
incomeBins = c(-100000,29999,69999,1000000)

#helper tables
allZonesFileName = Sys.getenv("ALLZONESFILENAME") #"Z:/viz/allzones.csv"

#generate only spatial tables
genSpatialOnly = Sys.getenv("SPATIALONLY") #F

#network and extra attributes file
extraAttribsFileName = Sys.getenv("EXATTFILE") #"z:/extraAttribs.csv"
networkFileName = Sys.getenv("D211FILE")#"z:/network.d211" 

#########################################################################
#calculated parameters
#########################################################################

#scenario name and time step
wkrDir = getwd()
sn = basename(dirname(dirname(wkrDir)))
tstep = gsub("t","",basename(wkrDir))

#is this a PI year
isPIYear = file.exists("CommodityZutilities.csv")

#is this a transport model year
isTransportYear = file.exists("SWIM_PeakAssignmentPaths.ver")

#is this a ALD year
isALDYear = file.exists(".RData")

#########################################################################
#functions
#########################################################################

#Read ZMX File
readZipMat = function(fileName) {
  
  #define matrix
  rowCon = unz(fileName,"_rows")
  colCon = unz(fileName,"_columns")
  xRowNumCon = unz(fileName,"_external row numbers")
  xColNumCon = unz(fileName,"_external column numbers")
  nrows = as.integer(scan(rowCon, what="", quiet=T))
  ncols = as.integer(scan(colCon, what="", quiet=T))
  rowNames = strsplit(scan(xRowNumCon, what="", quiet=T),",")[[1]]
  colNames = strsplit(scan(xColNumCon, what="", quiet=T),",")[[1]]
  close(rowCon)
  close(colCon)
  close(xRowNumCon)
  close(xColNumCon)
  
  #create matrix
  outMat = matrix(0, nrows, ncols)
  rownames(outMat) = rowNames
  colnames(outMat) = colNames
  
  #read records
  zipEntries = paste("row_", 1:nrows, sep="")
  for(i in 1:nrows) {
    con = unz(fileName,zipEntries[i],"rb")
    outMat[i,] = readBin(con,what=double(),n=ncols, size=4, endian="big")
    close(con)
  }
  #return matrix
  return(outMat)
}

#convert R matrix with names to a table of FROMZONE, TOZONE, MATRIX value
matAsTable = function(mat) {
  df = data.frame(expand.grid(FROMZONE=as.integer(rownames(mat)),TOZONE=as.integer(colnames(mat))), MATRIX=as.vector(mat))
  df = df[order(df$FROMZONE,df$TOZONE),]
  df
}

#function to convert AZONE to BZONE
azone2bzone= function(azones, allAZONES, allBZONES) {
  return(allBZONES[match(azones, allAZONES)])
}

#create vectorized indexes - watch out for duplicates on the left side of assignment
vectorIndex = function(fromIndex, toIndex, nrow) {
  return((toIndex-1)*nrow+fromIndex)
}

#time period functions
isAM = function(x) { x > 699 & x < 900 }
isMD = function(x) { x > 899 & x < 1600 }
isPM = function(x) { x > 1599 & x < 1800 }
isNT = function(x) { x > 1799 | x < 700 }

#fix activity names in employment.csv file
fixActivityNames = function(empTable) {

  replaceString = function(x, charIndex, newString) {
    if(length(charIndex)>0) {
      if(!is.na(charIndex) & charIndex > 0) {
        return(paste(substr(x,1,charIndex-1), newString, substr(x,charIndex+1,nchar(x)), sep=""))
      } else {
        return(x)  
      }
    } else {
      return(x)  
    }
  }
  
  cNames = colnames(empTable)
  cNames = strsplit(cNames,"[.]")
  cNamesIsUpper = lapply(cNames, function(x) x==toupper(x))
  cNamesOut = lapply(cNames, function(x) paste(x, collapse="_"))
  whichToReplace = lapply(cNamesIsUpper, function(x) which(x==F)[1])
  charToReplace = lapply(cNamesOut, function(x) gregexpr("_",x)[[1]])
  indexes = mapply(function(x,y) {x[y-1]}, charToReplace,whichToReplace)
  cNamesOut = mapply(replaceString, cNamesOut, indexes, "-")
  cNamesOut = gsub("NON_DURABLES", "NON-DURABLES", cNamesOut)

  colnames(empTable) = cNamesOut
  return(empTable)
}


#########################################################################
  #Exit if NOT a PI Year
#########################################################################
  
#exit if NOT PI year
if(!isPIYear) {
  cat(paste("No database created since not a PI year ", Sys.time(), "\n"))
  quit("no")
}

#########################################################################
#create database
#########################################################################

#create scenario database
m = dbDriver("SQLite")
databaseFileName = paste(sn, "_t", tstep, ".db", sep="")
cat(paste("Create SWIM VIZ DB for", databaseFileName, "at", Sys.time(), "\n"))
if(createNewDBFile) {
  if(file.exists(databaseFileName)) { 
    file.remove(databaseFileName) 
  }
  file.create(databaseFileName)
}
db = dbConnect(m, dbname = databaseFileName)

#########################################################################
#read a the helper table and create BZONE table
#########################################################################

#read helper tables
allZones = read.csv(allZonesFileName)

#create allzones tables
dbWriteTable(db,"ALLZONES",allZones, row.names=F)
azones = dbGetQuery(db,"SELECT AZONE FROM ALLZONES WHERE TYPE != 'WM'")[[1]] #no world markets

#create BZONE table
dbGetQuery(db,"CREATE TABLE BZONE AS SELECT DISTINCT BZONE, TYPE, STATE, COUNTY, SUM(ACRES) AS ACRES, 
  MPO, ACT, DESCRIPTION FROM ALLZONES GROUP BY BZONE")
dbGetQuery(db,"UPDATE BZONE SET DESCRIPTION = 'BZONE ' || BZONE WHERE DESCRIPTION LIKE '%AZONE%'")
bzones = dbGetQuery(db,"SELECT BZONE FROM BZONE")[[1]]

#########################################################################
#Read ActivityLocations
#########################################################################

#Read in ActivityLocations.csv - note this table contains 6000 numbered external ED zones 
ActivityLocations = read.csv("ActivityLocations.csv")
ActivityLocations$Activity = gsub(" |&","_",as.character(ActivityLocations$Activity)) #replace " " & "&"

# Remove import (_impt), export (_expt) and govt (_acct_) activity ?? 
# ActivityLocations <- ActivityLocations[grepl("_expt", as.character(ActivityLocations$Activity))== FALSE,]
# ActivityLocations <- ActivityLocations[grepl("_impt", as.character(ActivityLocations$Activity))== FALSE,]
# ActivityLocations <- ActivityLocations[grepl("_acct_",as.character(ActivityLocations$Activity))== FALSE,]

key = paste(ActivityLocations$Activity, ActivityLocations$ZoneNumber)

#Add ZoneMakeUse dat
zMakeUse = read.csv("ZonalMakeUse.csv")
zMakeUse$Activity = gsub(" |&","_",as.character(zMakeUse$Activity)) #replace " " & "&"
zMakeUse$Commodity = gsub(" |&","_",as.character(zMakeUse$Commodity)) #replace " " & "&"

zMakeUseLabor = zMakeUse[zMakeUse$MorU=="U",]
zMakeUseLabor = zMakeUseLabor[grep("[A-Z][0-9]-",zMakeUse$Commodity),]
zMakeUseLabor = tapply(zMakeUseLabor$Amount, list(zMakeUseLabor$ZoneNumber, zMakeUseLabor$Activity), sum)
zMakeUseLaborTable = data.frame(Zone=rownames(zMakeUseLabor), Activity=rep(colnames(zMakeUseLabor), each=length(rownames(zMakeUseLabor))), Value=as.vector(zMakeUseLabor))

keyZ = paste(zMakeUseLaborTable$Activity, zMakeUseLaborTable$Zone)
ActivityLocations$LaborUseQty = zMakeUseLaborTable$Value[match(key,keyZ)]

#add employment field 
ActivityLocations$Employment = 0

#Add employment data if transport year
if(isTransportYear) {

  emp = read.csv("Employment.csv")
  emp = fixActivityNames(emp)
  
  empOut = data.frame()
  for(i in 2:ncol(emp)) {
  
    act = colnames(emp)[i] 
    x = data.frame(emp$Azone, act, emp[,i])
    colnames(x) = c("AZONE","ACTIVITY","EMP")
    empOut = rbind(empOut,x) #transpose data
  }
  
  empOut$BZONE = azone2bzone(empOut$AZONE,allZones$AZONE,allZones$BZONE)
  keyEmp = paste(empOut$ACTIVITY, empOut$BZONE)
  
  #aggregate azone to bzone
  empOut = aggregate(empOut$EMP, list(empOut$ACTIVITY, empOut$BZONE), sum)
  colnames(empOut) = c("ACTIVITY","BZONE","EMP")
  keyEmp = paste(empOut$ACTIVITY, empOut$BZONE)
  
  #add Employment field to ActivityLocations
  ActivityLocations$Employment = empOut$EMP[match(key,keyEmp)]
}

#Activity quantities in 2009$ except HHs which are HHs
dbGetQuery(db,"CREATE TABLE ActivityLocations (
  BZONE INT, 
  ACTIVITY TEXT, 
  QUANTITY REAL,
  LaborUseQty REAL,
  Employment REAL
  )")
dbWriteTable(db, "ActivityLocations", ActivityLocations[,c("ZoneNumber","Activity","Quantity","LaborUseQty","Employment")], append=T, row.names=F)
dbGetQuery(db, "CREATE INDEX ActivityLocationsIndex ON ActivityLocations (BZONE,ACTIVITY)")

#########################################################################
# Add Activity Constraints Index Table
#########################################################################

if(file.exists("ActivityConstraintsI.csv")) {
    actCon = read.csv("ActivityConstraintsI.csv")
    
    # Add BZONE and create data frame by BZONE
     actCon$BZONE = azone2bzone(actCon$taz,allZones$AZONE,allZones$BZONE)
     actCon$BZACT = paste(actCon$BZONE,actCon$Activity,sep="-")
     actConBzQnt  = tapply(actCon$Quantity, actCon$BZACT, sum) 
     actConBzQnt = as.data.frame(actConBzQnt)
     actConBzQnt$ACTIVITY = actCon$Activity[match(rownames(actConBzQnt),actCon$BZACT)]
     colnames(actConBzQnt) <- c("QUANTITY_CON", "ACTIVITY_CON") 
    
     temp_act <- dbGetQuery(db, "select * from ActivityLocations") 
     KEY       <- paste(temp_act$BZONE,temp_act$ACTIVITY,sep="-")
     temp_act$QUANTITY_CON <-  actConBzQnt$QUANTITY_CON[match(KEY,rownames(actConBzQnt))] 
     temp_act$QUANTITY_CON[is.na(temp_act$QUANTITY_CON)] <- 0
    
     dbGetQuery(db, "DROP TABLE ActivityLocations") 
     dbWriteTable(db, "ActivityLocations", temp_act, append=T, row.names=F)  
}
 
#########################################################################
#Read ExchangeResults
#########################################################################

#Read in ExchangeResults.csv
ExchangeResults = read.csv("ExchangeResults.csv")
ExchangeResults = ExchangeResults[grepl("Receipts",ExchangeResults$Commodity)==FALSE,] #Remove all "Receipts"
ExchangeResults$Commodity = gsub(" |&","_",as.character(ExchangeResults$Commodity))    #replace " " & "&"

key = paste(ExchangeResults$Commodity, ExchangeResults$ZoneNumber)
ExchangeResults$TransportComponent1 = 0
ExchangeResults$TransportComponent2 = 0
ExchangeResults$TransportComponent3 = 0
ExchangeResults$TransportComponent4 = 0
  
#Read in ZUtilities as well
zUtil = read.csv("CommodityZutilities.csv")
zUtil$Commodity = gsub(" |&","_",as.character(zUtil$Commodity)) #replace " " & "&"

#buying
zUtilBuy = zUtil[zUtil$BuyingOrSelling=="B",]
keyZ = paste(zUtilBuy$Commodity, zUtilBuy$Zone)
ExchangeResults$BoughtPriceComponent = zUtilBuy$PriceComponent[match(key,keyZ)]
ExchangeResults$BoughtQuantity = zUtilBuy$Quantity[match(key,keyZ)]

#selling
zUtilSell = zUtil[zUtil$BuyingOrSelling=="S",]
keyZ = paste(zUtilSell$Commodity, zUtilSell$Zone)
ExchangeResults$SoldPriceComponent = zUtilSell$PriceComponent[match(key,keyZ)]
ExchangeResults$SoldQuantity = zUtilSell$Quantity[match(key,keyZ)]

if("TransportComponent1" %in% colnames(zUtil)) {

  #determine if B or S value should be used
  zUtil$BorS = "B"
  zUtil$BorS[grep("^FLR",zUtil$Commodity)] = "S"
  zUtil$BorS[grep("[A-Z][0-9]-",zUtil$Commodity)] = "S"
  zUtil = zUtil[zUtil$BuyingOrSelling==zUtil$BorS,]

  #match up data
  keyZ = paste(zUtil$Commodity, zUtil$Zone)
  ExchangeResults$TransportComponent1 = zUtil$TransportComponent1[match(key,keyZ)]
  ExchangeResults$TransportComponent2 = zUtil$TransportComponent2[match(key,keyZ)]
  ExchangeResults$TransportComponent3 = zUtil$TransportComponent3[match(key,keyZ)]
  ExchangeResults$TransportComponent4 = zUtil$TransportComponent4[match(key,keyZ)]
}

#make use data
zMakeUseUse = zMakeUse[zMakeUse$MorU=="U",]
zMakeUseUse = tapply(zMakeUseUse$Amount, list(zMakeUseUse$ZoneNumber, zMakeUseUse$Commodity), sum)
zMakeUseUseTable = data.frame(Zone=rownames(zMakeUseUse), Commodity=rep(colnames(zMakeUseUse), each=length(rownames(zMakeUseUse))), Value=as.vector(zMakeUseUse))
keyZ = paste(zMakeUseUseTable$Commodity, zMakeUseUseTable$Zone)
ExchangeResults$UseQty = zMakeUseUseTable$Value[match(key,keyZ)]

zMakeUseMake = zMakeUse[zMakeUse$MorU=="M",]
zMakeUseMake = tapply(zMakeUseMake$Amount, list(zMakeUseMake$ZoneNumber, zMakeUseMake$Commodity), sum)
zMakeUseMakeTable = data.frame(Zone=rownames(zMakeUseMake), Commodity=rep(colnames(zMakeUseMake), each=length(rownames(zMakeUseMake))), Value=as.vector(zMakeUseMake))
keyZ = paste(zMakeUseMakeTable$Commodity, zMakeUseMakeTable$Zone)
ExchangeResults$MakeQty = zMakeUseMakeTable$Value[match(key,keyZ)]

dbGetQuery(db,"CREATE TABLE ExchangeResults (
  BZONE INTEGER,
  Commodity TEXT,
  Demand REAL,
  InternalBought REAL,
  Exports REAL,
  Supply REAL,
  InternalSold REAL,
  Imports REAL,
  Surplus REAL,
  Price REAL,
  TransportComponent1 REAL,
  TransportComponent2 REAL,
  TransportComponent3 REAL,
  TransportComponent4 REAL,
  BoughtPriceComponent REAL,
  BoughtQuantity REAL,
  SoldPriceComponent REAL,
  SoldQuantity REAL,
  UseQty REAL,
  MakeQty REAL
  )")
colOrder = c("ZoneNumber","Commodity","Demand","InternalBought",
  "Exports","Supply","InternalSold","Imports","Surplus","Price",
  "TransportComponent1","TransportComponent2","TransportComponent3", "TransportComponent4",
  "BoughtPriceComponent", "BoughtQuantity", "SoldPriceComponent", "SoldQuantity", "UseQty", "MakeQty"
  )
dbWriteTable(db, "ExchangeResults", ExchangeResults[,colOrder], append=T, row.names=F)
dbGetQuery(db, "CREATE INDEX ExchangeResultsIndex ON ExchangeResults (BZONE,Commodity)")

#########################################################################
# Add Exchange Results Targets Index  Table
#########################################################################

if(file.exists("ExchangeResultsTargetsI.csv")) {
     exRes = read.csv("ExchangeResultsTargetsI.csv")
     exRes$Commodity <- gsub(" |&","_",as.character(exRes$Commodity))
     key2 <- paste(exRes$ZoneNumber,exRes$Commodity , sep="-")
     
     exTable <- dbGetQuery(db, "select * from ExchangeResults") 
     key1 <- paste(exTable$BZONE,exTable$Commodity , sep="-")
     exTable$Price_I <- exRes$Price[match(key1,key2)]
     exTable$Region <- exRes$region[match(key1,key2)]
     exTable$Type <- exRes$type[match(key1,key2)]
     
     dbGetQuery(db, "DROP TABLE ExchangeResults") 
     dbWriteTable(db, "ExchangeResults", exTable, append=T, row.names=F)  
}

#########################################################################
#Read FloorspaceInventory + Floorspace Capacity
#########################################################################

if(isALDYear) {

  #read FloorspaceInventory.csv
  FloorspaceInventory = read.csv("FloorspaceInventory.csv")
  commodityNames = gsub("[.]"," ",colnames(FloorspaceInventory)[2:ncol(FloorspaceInventory)])
  commodityNames = gsub(" |&","_",commodityNames) #replace " " & "&"
  
  FSI = data.frame(FloorspaceInventory$AZone,unlist(FloorspaceInventory[2:ncol(FloorspaceInventory)]))
  FSI$COMMODITY = rep(commodityNames,each=nrow(FloorspaceInventory))
  colnames(FSI) =c("AZONE","FLR","COMMODITY")
  
  #add Floorspace Capacity as well
  load(".RData") #ALD output
  FSC = data.frame()
  for(i in 1:ncol(FloorspaceCapacities_$ResCap.AzFr)) {
    com = colnames(FloorspaceCapacities_$ResCap.AzFr)[i]
    com = gsub(" |-","_", com)
    x = data.frame(rownames(FloorspaceCapacities_$ResCap.AzFr), com, FloorspaceCapacities_$ResCap.AzFr[,i])
    colnames(x) = c("AZONE","COMMODITY","CAP")
    FSC = rbind(FSC,x)
  }
  
  for(i in 1:ncol(FloorspaceCapacities_$NresCap.AzFn)) {
    com = colnames(FloorspaceCapacities_$NresCap.AzFn)[i]
    com = gsub(" |-","_", com)
    x = data.frame(rownames(FloorspaceCapacities_$NresCap.AzFn), com, FloorspaceCapacities_$NresCap.AzFn[,i])
    colnames(x) = c("AZONE","COMMODITY","CAP")
    FSC = rbind(FSC,x)
  }
  FSI = merge(FSI, FSC)

  #increments matrix
  increments = read.csv("Increments_Matrix.csv")
  colnames(increments) = gsub("[.]","_",colnames(increments)) #replace "." with "_"
  increments2 = data.frame(AZONE=increments$AZone, COMMODITY=rep(colnames(increments)[-1], each=nrow(increments)), 
    INCREMENT=as.vector(as.matrix(increments[,2:ncol(increments)])))
  increments2$COMMODITY = as.character(increments2$COMMODITY)
  increments2 = increments2[!(increments2$COMMODITY %in% c("FLR_Agriculture", "FLR_Logging")),]
  FSI$INCREMENT = 0
  FSI$INCREMENT = increments2$INCREMENT[match(paste(FSI$AZONE, FSI$COMMODITY), paste(increments2$AZONE, increments2$COMMODITY))]
  
  FSI$BZONE = azone2bzone(FSI$AZONE,allZones$AZONE,allZones$BZONE)
  dbWriteTable(db, "FloorspaceInventory", FSI, row.names=F)
  
  dbGetQuery(db,"CREATE TABLE FLR_INVENTORY AS SELECT BZONE, COMMODITY, SUM(FLR) AS FLR, 
    SUM(Cap) AS CAP, SUM(INCREMENT) AS INCREMENT FROM FloorspaceInventory GROUP BY BZONE,COMMODITY")
  dbGetQuery(db,"DROP TABLE FloorspaceInventory")
  dbGetQuery(db, "CREATE INDEX FLR_INVENTORYIndex ON FLR_INVENTORY (BZONE,COMMODITY)")
  
  #########################################################################
  # Add Floorspace Index Table
  #########################################################################
  flrSpace = read.csv("FloorspaceI.csv")
  flrSpace$commodity <- gsub(" |&","_",as.character(flrSpace$commodity))
  
  # Add BZONE and create data frame by BZONE
  flrSpace$BZONE = azone2bzone(flrSpace$taz,allZones$AZONE,allZones$BZONE)
  flrSpace$BZCOM = paste(flrSpace$BZONE,flrSpace$commodity,sep="-")
  flrComBzQnt    = tapply(flrSpace$quantity, flrSpace$BZCOM, sum) 
  flrComBzQnt    = as.data.frame(flrComBzQnt)
  colnames(flrComBzQnt) <- c("QUANTITY_CON")
  
  flrTable <- dbGetQuery(db, "select * from FLR_INVENTORY")
  key1 <- paste(flrTable$BZONE,flrTable$COMMODITY, sep="-")
  flrTable$QUANTITY_CON <-  flrComBzQnt$QUANTITY_CON[match(key1,rownames(flrComBzQnt))]
  
  dbGetQuery(db, "DROP TABLE FLR_INVENTORY") 
  dbWriteTable(db, "FLR_INVENTORY", flrTable, append=T, row.names=F) 
}

#########################################################################
#create BuySell matrices (buying + selling matrices)
#########################################################################

#read matrix data (each selling matrix and a buying matrix if it exists)
sellingMats = dir()[grep("selling_",dir())]

if(length(sellingMats > 0)) {
  
  commodityNames = gsub("selling_|[.]zmx","",basename(sellingMats))
  commodityNames = gsub(" |&","_",commodityNames) #replace " " & "&"
  commodityNames = gsub("-","_",commodityNames) #replace "-" 
    
  dbGetQuery(db,"CREATE TABLE BuySellMatrix (
    FROMBZONE INTEGER,
    TOBZONE INTEGER,
    PRIMARY KEY(FROMBZONE,TOBZONE)
    )")
  dbGetQuery(db,"INSERT INTO BuySellMatrix SELECT BZ1.BZONE, BZ2.BZONE FROM BZONE AS BZ1, BZONE AS BZ2")
  
  for (i in 1:length(sellingMats)) {
    
    #read selling matrix first and then if there is a buying matrix read and use it instead
    cat(paste("reading: ", sellingMats[i], "\n"))
    mat = matAsTable(readZipMat(sellingMats[i]))
    possibleBuyMatName = gsub("selling","buying",sellingMats[i])
    if(file.exists(possibleBuyMatName)) {
      cat(paste("reading: ", possibleBuyMatName, "\n"))
      buy_mat = matAsTable(readZipMat(possibleBuyMatName))
      mat[,3] = buy_mat[,3]
    }
    colName = paste("BuySell_", commodityNames[i], sep="")
    colnames(mat)[3] = colName
  
    #write temp table and join to BuySellMatrices
    dbWriteTable(db, "Temp", mat, append=T, row.names=F)
    dbGetQuery(db, "CREATE INDEX TempIndex ON Temp (FROMZONE, TOZONE)")
    dbGetQuery(db, paste("CREATE TABLE Temp2 AS SELECT bsm.*, ", colName,
      " FROM BuySellMatrix AS bsm LEFT OUTER JOIN Temp ON bsm.FromBZone=Temp.FromZone 
      AND bsm.ToBZone=Temp.ToZone", sep=""))
    dbGetQuery(db, "DROP TABLE Temp")
    dbGetQuery(db, "DROP TABLE BuySellMatrix")
    dbGetQuery(db, "ALTER TABLE Temp2 RENAME TO BuySellMatrix")
    dbGetQuery(db, "CREATE INDEX BuySellMatrixIndex ON BuySellMatrix (FromBZone, ToBZone)")
  }
}

#########################################################################
#Exit if NOT a transport year
#########################################################################

if((!isTransportYear) | as.logical(genSpatialOnly)) {
  #Close and compact database
  dbGetQuery(db, "VACUUM")
  sqliteCloseConnection(db)
  cat(paste("SWIM VIZ DB for", databaseFileName, "at", Sys.time(), "Created \n"))
  quit("no")
} 

#########################################################################
#Get HH home taz for later
#########################################################################
hhData = read.csv("HouseholdData.csv")

#########################################################################
#Process Trips_SDT
#########################################################################

#read and process trip file
trips = read.csv("Trips_SDTPerson.csv")

#remove extra fields
trips = trips[,c("hhID","origin","destination","tripStartTime","tripPurpose","tripMode","income","distance")]

#bin income
trips$hhInc = as.integer(cut(trips$income, incomeBins))

#calc tod
trips$tod[isAM(trips$tripStartTime)] = "AM"
trips$tod[isMD(trips$tripStartTime)] = "MD"
trips$tod[isPM(trips$tripStartTime)] = "PM"
trips$tod[isNT(trips$tripStartTime)] = "NT"

#add quantity field by tod
trips$amtrips = 0 
trips$mdtrips = 0 
trips$pmtrips = 0 
trips$nttrips = 0 
trips$amtrips[trips$tod == "AM"] = 1 
trips$mdtrips[trips$tod == "MD"] = 1 
trips$pmtrips[trips$tod == "PM"] = 1 
trips$nttrips[trips$tod == "NT"] = 1

#add distance
trips$amDistance = 0 
trips$mdDistance = 0 
trips$pmDistance = 0 
trips$ntDistance = 0
trips$amDistance[trips$tod == "AM"] = trips$distance[trips$tod == "AM"]
trips$mdDistance[trips$tod == "MD"] = trips$distance[trips$tod == "MD"]
trips$pmDistance[trips$tod == "PM"] = trips$distance[trips$tod == "PM"]
trips$ntDistance[trips$tod == "NT"] = trips$distance[trips$tod == "NT"]

#add bzone
trips$Bzone = azone2bzone(trips$origin,allZones$AZONE,allZones$BZONE)

#add home zone
trips$HomeZone = hhData$TAZ[match(trips$hhID, hhData$HH_ID)]
trips$HomeBzone = azone2bzone(trips$HomeZone,allZones$AZONE,allZones$BZONE)
        
#write origin table to database
dbWriteTable(db,"Trips_SDT_Temp",trips[,c("Bzone","hhInc","tripPurpose","tripMode",
  "amtrips","mdtrips","pmtrips","nttrips","amDistance","mdDistance","pmDistance","ntDistance")], append=T, row.names=F)

#write home zone table to database
dbWriteTable(db,"Trips_SDT_Home_Temp",trips[,c("HomeBzone","hhInc","tripPurpose","tripMode",
  "amtrips","mdtrips","pmtrips","nttrips","amDistance","mdDistance","pmDistance","ntDistance")], append=T, row.names=F)
        
#create indexes
from_SDT_Index = match(trips$origin,azones)
to_SDT_Index = match(trips$destination,azones)
sdtIndex = vectorIndex(from_SDT_Index, to_SDT_Index, length(azones))  
        
SDTModes = c("BIKE","DA","DR_TRAN","SCHOOL_BUS","SR2","SR3P","WALK","WK_TRAN")
for(aMode in SDTModes) {
  
  #create OD matrices
  for(tod in c("am","md","pm","nt")) {
  
    #create empty matrix
    mat = matrix(0, length(azones), length(azones)) 
    
    #calculate matrix volumes
    sdtVolumes = trips[,paste(tod, "trips", sep="")][trips$tripMode == aMode]
    sdtIndexTemp = sdtIndex[trips$tripMode == aMode]
    sdtVolumes = tapply(sdtVolumes, sdtIndexTemp, sum) #sum by duplicate index 
    
    #convert to vehicle trips for shared ride modes
    if(aMode == "SR2") {
      sdtVolumes = sdtVolumes / 2
    } else if (aMode == "SR3P") {
      sdtVolumes = sdtVolumes / 3.33
    } 
    
    #add to matrix
    if(length(sdtVolumes) > 0) {
      mat[as.integer(names(sdtVolumes))] = mat[as.integer(names(sdtVolumes))] + sdtVolumes
    }
    
    #save to RData file as object called "mat"
    save(mat, file=paste(tod, "_", aMode, "_MAT.RData", sep=""))
  }
}
rm(trips)

#aggregate results
dbGetQuery(db,"CREATE INDEX Trips_SDT_TempIndex ON Trips_SDT_Temp (BZONE)")
dbGetQuery(db,"CREATE INDEX Trips_SDT_Home_TempIndex ON Trips_SDT_Home_Temp (HomeBZONE)")

#origin table
dbGetQuery(db,paste("CREATE TABLE Trips_SDT AS SELECT BZONE, hhInc, tripPurpose, tripMode, 
  SUM(amtrips) AS amtrips, SUM(mdtrips) AS mdtrips, SUM(pmtrips) AS pmtrips, SUM(nttrips) AS nttrips,
  SUM(amDistance) AS amDistance, SUM(mdDistance) AS mdDistance, SUM(pmDistance) AS pmDistance, SUM(ntDistance) AS ntDistance  
  FROM Trips_SDT_Temp GROUP BY bzone, hhInc, tripPurpose, tripMode", sep=""))

#home table
dbGetQuery(db,paste("CREATE TABLE Trips_SDT_Home AS SELECT HomeBZONE AS BZONE, hhInc, tripPurpose, tripMode, 
  SUM(amtrips) AS amtrips, SUM(mdtrips) AS mdtrips, SUM(pmtrips) AS pmtrips, SUM(nttrips) AS nttrips,
  SUM(amDistance) AS amDistance, SUM(mdDistance) AS mdDistance, SUM(pmDistance) AS pmDistance, SUM(ntDistance) AS ntDistance  
  FROM Trips_SDT_Home_Temp GROUP BY HomeBZone, hhInc, tripPurpose, tripMode", sep=""))

dbGetQuery(db, "CREATE INDEX Trips_SDTIndex ON Trips_SDT (BZONE,HHINC,TRIPPURPOSE,TRIPMODE)")
dbGetQuery(db, "CREATE INDEX Trips_SDT_HomeIndex ON Trips_SDT_Home (BZONE,HHINC,TRIPPURPOSE,TRIPMODE)")
dbGetQuery(db, "DROP TABLE Trips_SDT_Temp")
dbGetQuery(db, "DROP TABLE Trips_SDT_Home_Temp")

#########################################################################
#Process Trips_ET
#########################################################################

TripsET = read.csv("Trips_ETTruck.csv")

#add peak and offPeak distance to table
opTruckDist = matAsTable(readZipMat("optrk1dist.zmx"))
pkTruckDist = matAsTable(readZipMat("pktrk1dist.zmx"))
TripsET$distance_op = opTruckDist$MATRIX[match(paste(TripsET$origin,TripsET$destination),paste(opTruckDist$FROMZONE,opTruckDist$TOZONE))]
TripsET$distance_pk = pkTruckDist$MATRIX[match(paste(TripsET$origin,TripsET$destination),paste(pkTruckDist$FROMZONE,pkTruckDist$TOZONE))]
TripsET$distance[isAM(TripsET$tripStartTime) | isPM(TripsET$tripStartTime)] = TripsET$distance_pk[isAM(TripsET$tripStartTime) | isPM(TripsET$tripStartTime)]
TripsET$distance[isMD(TripsET$tripStartTime) | isNT(TripsET$tripStartTime)] = TripsET$distance_op[isMD(TripsET$tripStartTime) | isNT(TripsET$tripStartTime)]
rm(opTruckDist, pkTruckDist)

TripsET$amVol = 0
TripsET$mdVol = 0
TripsET$pmVol = 0
TripsET$ntVol = 0

TripsET$amVol[isAM(TripsET$tripStartTime)] = TripsET$truckVolume[isAM(TripsET$tripStartTime)]
TripsET$mdVol[isMD(TripsET$tripStartTime)] = TripsET$truckVolume[isMD(TripsET$tripStartTime)]
TripsET$pmVol[isPM(TripsET$tripStartTime)] = TripsET$truckVolume[isPM(TripsET$tripStartTime)]
TripsET$ntVol[isNT(TripsET$tripStartTime)] = TripsET$truckVolume[isNT(TripsET$tripStartTime)]

TripsET$amDistance = 0
TripsET$mdDistance = 0
TripsET$pmDistance = 0
TripsET$ntDistance = 0

TripsET$amDistance[isAM(TripsET$tripStartTime)] = TripsET$distance[isAM(TripsET$tripStartTime)]
TripsET$mdDistance[isMD(TripsET$tripStartTime)] = TripsET$distance[isMD(TripsET$tripStartTime)]
TripsET$pmDistance[isPM(TripsET$tripStartTime)] = TripsET$distance[isPM(TripsET$tripStartTime)]
TripsET$ntDistance[isNT(TripsET$tripStartTime)] = TripsET$distance[isNT(TripsET$tripStartTime)]

TripsET$Bzone = azone2bzone(TripsET$origin,allZones$AZONE,allZones$BZONE)

dbWriteTable(db,"TripsET",TripsET,row.names=F)
dbGetQuery(db,"CREATE TABLE Trips_ET AS SELECT BZONE,truckClass,
  SUM(amVol) AS amtrips, SUM(mdVol) AS mdtrips, SUM(pmVol) AS pmtrips, SUM(ntVol) AS nttrips, 
  SUM(amDistance) AS amDistance, SUM(mdDistance) AS mdDistance, SUM(pmDistance) AS pmDistance, SUM(ntDistance) AS ntDistance 
  FROM TripsET GROUP BY BZONE, TruckClass")
dbGetQuery(db,"DROP TABLE TripsET")
dbGetQuery(db, "CREATE INDEX Trips_ETIndex ON Trips_ET (BZONE,TRUCKCLASS)")

#########################################################################
#Process Trips_CT
#########################################################################

TripsCT = read.csv("Trips_CTTruck.csv")
TripsCT$commodity = sprintf("SCTG%02i",TripsCT$commodity) 

#add production zone
homeZone = tapply(TripsCT$origin,TripsCT$truckID,function(x)x[1]) #assumes trips written in order
TripsCT$HomeZone = homeZone[match(TripsCT$truckID,names(homeZone))]
TripsCT$HomeBzone = azone2bzone(TripsCT$HomeZone,allZones$AZONE,allZones$BZONE)
        
TripsCT$amVol = 0
TripsCT$mdVol = 0
TripsCT$pmVol = 0
TripsCT$ntVol = 0

TripsCT$amVol[isAM(TripsCT$tripStartTime)] = 1
TripsCT$mdVol[isMD(TripsCT$tripStartTime)] = 1
TripsCT$pmVol[isPM(TripsCT$tripStartTime)] = 1
TripsCT$ntVol[isNT(TripsCT$tripStartTime)] = 1

TripsCT$amWeight = 0
TripsCT$mdWeight = 0
TripsCT$pmWeight = 0
TripsCT$ntWeight = 0

TripsCT$amWeight[isAM(TripsCT$tripStartTime)] = TripsCT$weight[isAM(TripsCT$tripStartTime)]
TripsCT$mdWeight[isMD(TripsCT$tripStartTime)] = TripsCT$weight[isMD(TripsCT$tripStartTime)]
TripsCT$pmWeight[isPM(TripsCT$tripStartTime)] = TripsCT$weight[isPM(TripsCT$tripStartTime)]
TripsCT$ntWeight[isNT(TripsCT$tripStartTime)] = TripsCT$weight[isNT(TripsCT$tripStartTime)]

TripsCT$amDistance = 0
TripsCT$mdDistance = 0
TripsCT$pmDistance = 0
TripsCT$ntDistance = 0

TripsCT$amDistance[isAM(TripsCT$tripStartTime)] = TripsCT$distance[isAM(TripsCT$tripStartTime)]
TripsCT$mdDistance[isMD(TripsCT$tripStartTime)] = TripsCT$distance[isMD(TripsCT$tripStartTime)]
TripsCT$pmDistance[isPM(TripsCT$tripStartTime)] = TripsCT$distance[isPM(TripsCT$tripStartTime)]
TripsCT$ntDistance[isNT(TripsCT$tripStartTime)] = TripsCT$distance[isNT(TripsCT$tripStartTime)]

TripsCT$Bzone = azone2bzone(TripsCT$origin,allZones$AZONE,allZones$BZONE)      

#trip origin
dbWriteTable(db,"TripsCT",TripsCT[,c("Bzone","truckType","commodity","amVol","mdVol","pmVol","ntVol",
  "amWeight","mdWeight","pmWeight","ntWeight",
  "amDistance","mdDistance","pmDistance","ntDistance")],row.names=F)
  
dbGetQuery(db,"CREATE TABLE Trips_CT AS SELECT BZONE,truckType AS TruckClass,Commodity, 
  SUM(amVol) AS amtrips, SUM(mdVol) AS mdtrips, SUM(pmVol) AS pmtrips, SUM(ntVol) AS nttrips, 
  SUM(amWeight) AS amWeight, SUM(mdWeight) AS mdWeight, SUM(pmWeight) AS pmWeight, SUM(ntWeight) AS ntWeight, 
  SUM(amDistance) AS amDistance, SUM(mdDistance) AS mdDistance, SUM(pmDistance) AS pmDistance, SUM(ntDistance) AS ntDistance 
  FROM TripsCT GROUP BY BZONE, TruckType, Commodity")
dbGetQuery(db,"DROP TABLE TripsCT")
dbGetQuery(db, "CREATE INDEX Trips_CTIndex ON Trips_CT (BZONE,TRUCKCLASS,COMMODITY)")

#trip production (home zone)
dbWriteTable(db,"TripsCT_Home",TripsCT[,c("HomeBzone","truckType","commodity","amVol","mdVol","pmVol","ntVol",
  "amWeight","mdWeight","pmWeight","ntWeight",
  "amDistance","mdDistance","pmDistance","ntDistance")],row.names=F)
  
dbGetQuery(db,"CREATE TABLE Trips_CT_Home AS SELECT HomeBZONE AS BZONE,truckType AS TruckClass,Commodity, 
  SUM(amVol) AS amtrips, SUM(mdVol) AS mdtrips, SUM(pmVol) AS pmtrips, SUM(ntVol) AS nttrips, 
  SUM(amWeight) AS amWeight, SUM(mdWeight) AS mdWeight, SUM(pmWeight) AS pmWeight, SUM(ntWeight) AS ntWeight, 
  SUM(amDistance) AS amDistance, SUM(mdDistance) AS mdDistance, SUM(pmDistance) AS pmDistance, SUM(ntDistance) AS ntDistance 
  FROM TripsCT_Home GROUP BY BZONE, TruckType, Commodity")
dbGetQuery(db,"DROP TABLE TripsCT_Home")
dbGetQuery(db, "CREATE INDEX Trips_CT_HomeIndex ON Trips_CT_Home (BZONE,TRUCKCLASS,COMMODITY)")

#########################################################################
#Process Trips_LDT
#########################################################################

#Trips_LDTVehicle contains all the trips (Trips_LDTPerson does not)
TripsLDT = read.csv("Trips_LDTVehicle.csv")

TripsLDT$amVol = 0
TripsLDT$mdVol = 0
TripsLDT$pmVol = 0
TripsLDT$ntVol = 0

TripsLDT$amVol[isAM(TripsLDT$tripStartTime)] = 1
TripsLDT$mdVol[isMD(TripsLDT$tripStartTime)] = 1
TripsLDT$pmVol[isPM(TripsLDT$tripStartTime)] = 1
TripsLDT$ntVol[isNT(TripsLDT$tripStartTime)] = 1

TripsLDT$amDistance = 0
TripsLDT$mdDistance = 0
TripsLDT$pmDistance = 0
TripsLDT$ntDistance = 0

TripsLDT$amDistance[isAM(TripsLDT$tripStartTime)] = TripsLDT$distance[isAM(TripsLDT$tripStartTime)]
TripsLDT$mdDistance[isMD(TripsLDT$tripStartTime)] = TripsLDT$distance[isMD(TripsLDT$tripStartTime)]
TripsLDT$pmDistance[isPM(TripsLDT$tripStartTime)] = TripsLDT$distance[isPM(TripsLDT$tripStartTime)]
TripsLDT$ntDistance[isNT(TripsLDT$tripStartTime)] = TripsLDT$distance[isNT(TripsLDT$tripStartTime)]

TripsLDT$Bzone = azone2bzone(TripsLDT$origin,allZones$AZONE,allZones$BZONE)
TripsLDT$hhInc = as.integer(cut(TripsLDT$income, incomeBins))

TripsLDT$HomeZone = hhData$TAZ[match(TripsLDT$hhID, hhData$HH_ID)]
TripsLDT$HomeBzone = azone2bzone(TripsLDT$HomeZone,allZones$AZONE,allZones$BZONE)

#origin table
dbWriteTable(db,"TripsLDT",TripsLDT[,c("Bzone","hhInc","tripPurpose","tripMode",
  "amVol","mdVol","pmVol","ntVol",
  "amDistance","mdDistance","pmDistance","ntDistance")],row.names=F)
dbGetQuery(db,"CREATE TABLE Trips_LDT AS SELECT BZONE,hhInc,tripPurpose,tripMode, 
  SUM(amVol) AS amtrips, SUM(mdVol) AS mdtrips, SUM(pmVol) AS pmtrips, SUM(ntVol) AS nttrips,
  SUM(amDistance) AS amDistance, SUM(mdDistance) AS mdDistance, SUM(pmDistance) AS pmDistance, SUM(ntDistance) AS ntDistance 
  FROM TripsLDT GROUP BY BZONE, hhInc, tripPurpose, tripMode")
dbGetQuery(db,"DROP TABLE TripsLDT")
dbGetQuery(db, "CREATE INDEX Trips_LDTIndex ON Trips_LDT (BZONE,HHINC,TRIPPURPOSE,TRIPMODE)")

#home zone table
dbWriteTable(db,"TripsLDT_Home",TripsLDT[,c("HomeBzone","hhInc","tripPurpose","tripMode",
  "amVol","mdVol","pmVol","ntVol",
  "amDistance","mdDistance","pmDistance","ntDistance")],row.names=F)
dbGetQuery(db,"CREATE TABLE Trips_LDT_Home AS SELECT HomeBZONE AS BZONE,hhInc,tripPurpose,tripMode, 
  SUM(amVol) AS amtrips, SUM(mdVol) AS mdtrips, SUM(pmVol) AS pmtrips, SUM(ntVol) AS nttrips,
  SUM(amDistance) AS amDistance, SUM(mdDistance) AS mdDistance, SUM(pmDistance) AS pmDistance, SUM(ntDistance) AS ntDistance 
  FROM TripsLDT_Home GROUP BY HomeBZONE, hhInc, tripPurpose, tripMode")
dbGetQuery(db,"DROP TABLE TripsLDT_Home")
dbGetQuery(db, "CREATE INDEX Trips_LDT_HomeIndex ON Trips_LDT_Home (BZONE,HHINC,TRIPPURPOSE,TRIPMODE)")

#########################################################################
#Create AZONE OD matrices for OD flow analysis - will be deleted later
#########################################################################

ETTruckClasses = dbGetQuery(db,"SELECT DISTINCT TRUCKCLASS FROM Trips_ET")[[1]]
CTTruckClasses = dbGetQuery(db,"SELECT DISTINCT TRUCKCLASS FROM Trips_CT")[[1]]
TruckClasses = sort(unique(c(ETTruckClasses, CTTruckClasses)))

LDTModes = dbGetQuery(db,"SELECT DISTINCT TRIPMODE FROM Trips_LDT")[[1]]

#create vectorized indexes - watch out for duplicates on the left side of assignment
from_ET_Index = match(TripsET$origin,azones)
to_ET_Index = match(TripsET$destination,azones)
etIndex = vectorIndex(from_ET_Index, to_ET_Index, length(azones))

from_CT_Index = match(TripsCT$origin,azones)
to_CT_Index = match(TripsCT$destination,azones)
ctIndex = vectorIndex(from_CT_Index, to_CT_Index, length(azones))

from_LDT_Index = match(TripsLDT$origin,azones)
to_LDT_Index = match(TripsLDT$destination,azones)
ldtIndex = vectorIndex(from_LDT_Index, to_LDT_Index, length(azones))

#create ET and CT matrices
for(truckClass in TruckClasses) {
  
  #create empty matrix
  mat = matrix(0, length(azones), length(azones))

  for(tod in c("am","md","pm","nt")) {
    #ET Volumes
    etVolumes = TripsET[,paste(tod, "Vol", sep="")][TripsET$truckClass == truckClass]
    etIndexTemp = etIndex[TripsET$truckClass == truckClass]
    etVolumes = tapply(etVolumes, etIndexTemp, sum) #sum by duplicate index 
    if(length(etVolumes) > 0) {
      mat[as.integer(names(etVolumes))] = etVolumes
    }
    
    #CT Volumes
    ctVolumes = TripsCT[,paste(tod, "Vol", sep="")][TripsCT$truckType == truckClass]
    ctIndexTemp = ctIndex[TripsCT$truckType == truckClass]
    ctVolumes = tapply(ctVolumes, ctIndexTemp, sum) #sum by duplicate index 
    if(length(ctVolumes) > 0) {
      mat[as.integer(names(ctVolumes))] = mat[as.integer(names(ctVolumes))] + ctVolumes
    }
    
    #save to RData file as object called "mat"
    save(mat, file=paste(tod, "_", truckClass, "_MAT.RData", sep=""))
    mat[] = 0 #reset matrix
  }
}

#create LDT and add to SDT matrices
for(aMode in LDTModes) {
  
  for(tod in c("am","md","pm","nt")) {
  
    #get SDT matrix (object called mat) from earlier
    if(file.exists(paste(tod, "_", aMode, "_MAT.RData", sep=""))) {
      load(paste(tod, "_", aMode, "_MAT.RData", sep=""))
    } else {
      mat = matrix(0, length(azones), length(azones)) #create empty matrix
    }
  
    #LDT Volumes
    ldtVolumes = TripsLDT[,paste(tod, "Vol", sep="")][TripsLDT$tripMode == aMode]
    ldtIndexTemp = ldtIndex[TripsLDT$tripMode == aMode]
    ldtVolumes = tapply(ldtVolumes, ldtIndexTemp, sum) #sum by duplicate index 
    
    #convert to vehicle trips for shared ride modes
    if(aMode == "SR2") {
      ldtVolumes = ldtVolumes / 2
    } else if (aMode == "SR3P") {
      ldtVolumes = ldtVolumes / 3.33
    } 
    
    #Add to matrix
    if(length(ldtVolumes) > 0) {
      mat[as.integer(names(ldtVolumes))] = mat[as.integer(names(ldtVolumes))] + ldtVolumes
    }

    #save to RData file as object called "mat"
    save(mat, file=paste(tod, "_", aMode, "_MAT.RData", sep=""))
  }
}

#########################################################################
#Destination Choice logsums
#########################################################################

dcLogsums = read.csv("dcLogsums.csv")

values = unlist(dcLogsums[,!(colnames(dcLogsums) %in% ("Azone"))])
logsums = data.frame(AZONE=dcLogsums[,"Azone"], 
  PURPOSESEGMENT=rep(colnames(dcLogsums)[-1], each=nrow(dcLogsums)), LOGSUM=values)

logsums$PURPOSESEGMENT = as.character(logsums$PURPOSESEGMENT)
logsums$SEGMENT = substr(logsums$PURPOSESEGMENT,nchar(logsums$PURPOSESEGMENT),nchar(logsums$PURPOSESEGMENT))
logsums$PURPOSE = substr(logsums$PURPOSESEGMENT,1,nchar(logsums$PURPOSESEGMENT)-1)

logsums$BZONE = azone2bzone(logsums$AZONE,allZones$AZONE,allZones$BZONE)

dbWriteTable(db, "DCLOGSUMS", logsums[,c("BZONE","PURPOSE","SEGMENT","LOGSUM")], row.names=F)

dbGetQuery(db, "CREATE TABLE DC_LOGSUM AS SELECT BZONE,PURPOSE, SEGMENT,AVG(LOGSUM) AS AVGLOGSUM FROM DCLOGSUMS 
  GROUP BY BZONE, PURPOSE, SEGMENT")
dbGetQuery(db, "CREATE INDEX DCLogsumIndex ON DC_LOGSUM (BZONE,PURPOSE,SEGMENT)")
dbGetQuery(db, "DROP TABLE DCLOGSUMS")

#########################################################################
#Read skim matrices
#########################################################################

skimsToProcess = c("opautodist.zmx","opautotime.zmx","opautotoll.zmx","pkautodist.zmx","pkautotime.zmx","pkautotoll.zmx",
  "optrk1dist.zmx","optrk1time.zmx","optrk1toll.zmx","pktrk1dist.zmx","pktrk1time.zmx","pktrk1toll.zmx")
tripWeightMatrix = c("md_DA_MAT.RData","md_DA_MAT.RData","md_DA_MAT.RData","am_DA_MAT.RData","am_DA_MAT.RData","am_DA_MAT.RData",
  "md_TRK1_MAT.RData","md_TRK1_MAT.RData","md_TRK1_MAT.RData","am_TRK1_MAT.RData","am_TRK1_MAT.RData","am_TRK1_MAT.RData") #same order as skimsToProcess

for(i in 1:length(skimsToProcess)) {

  #read trip matrix for weighting as object named mat
  load(tripWeightMatrix[i])
  weightMatrix = mat
  
  #read skim and add FROMBZONE and TOBZONE fields
  cat(paste("reading: ", skimsToProcess[i], "\n"))
  mat = matAsTable(readZipMat(skimsToProcess[i]))
  mat$FROMBZONE = azone2bzone(mat$FROMZONE,allZones$AZONE,allZones$BZONE)
  mat$TOBZONE = azone2bzone(mat$TOZONE,allZones$AZONE,allZones$BZONE)
  mat$weight = as.vector(t(weightMatrix)) #add weight
  
  #collapse by weighted mean
  top = tapply(mat[,3]*mat$weight, list(mat$FROMBZONE,mat$TOBZONE), sum) #weighted = sum(skim * trips)
  bottom = tapply(mat$weight, list(mat$FROMBZONE,mat$TOBZONE), sum) #total trips
  matMean = tapply(mat[,3], list(mat$FROMBZONE,mat$TOBZONE), mean) #unweighted for when no trips
  mat = matrix(0, nrow(top), ncol(top))
  mat[bottom>0] = top[bottom>0] / bottom[bottom>0] #weighted / total trips
  mat[bottom<=0] = matMean[bottom<=0] #weighted / total trips
  mat = data.frame(FROMBZONE=rep(rownames(top),each=length(rownames(top))), 
    TOBZONE=rep(rownames(top), length(rownames(top))), as.vector(t(mat)))
  colName = gsub(".zmx","",skimsToProcess[i])
  colnames(mat)[3] = colName
  
  #write temp table and join to SKIM
  if(i==1) {
    dbWriteTable(db, "SKIM", mat, row.names=F)
  } else {
    dbWriteTable(db, "TEMP", mat, append=T, row.names=F)
    dbGetQuery(db, "CREATE INDEX TempIndex ON Temp (FROMBZONE, TOBZONE)")
    dbGetQuery(db, paste("CREATE TABLE Temp2 AS SELECT SKIM.*, ", colName,
      " FROM SKIM LEFT OUTER JOIN Temp ON SKIM.FromBZone=Temp.FromBZone AND SKIM.ToBZone=Temp.ToBZone", sep=""))
    dbGetQuery(db, "DROP TABLE Temp")
    dbGetQuery(db, "DROP TABLE SKIM")
    dbGetQuery(db, "ALTER TABLE Temp2 RENAME TO SKIM")
    dbGetQuery(db, "CREATE INDEX SKIMIndex ON SKIM (FromBZone, ToBZone)")
  }
}

#########################################################################
#Read OD matrices
#########################################################################

matsToProcess = dir()[grep("_MAT.RData",dir())]

for(i in 1:length(matsToProcess)) {

  #read matrix and add FROMBZONE and TOBZONE fields
  cat(paste("reading: ", matsToProcess[i], "\n"))
  load(matsToProcess[i])
  mat = data.frame(FROMZONE=rep(azones,each=length(azones)), TOZONE=rep(azones, length(azones)), as.vector(t(mat)))
  mat$FROMBZONE = azone2bzone(mat$FROMZONE,allZones$AZONE,allZones$BZONE)
  mat$TOBZONE = azone2bzone(mat$TOZONE,allZones$AZONE,allZones$BZONE)

  #collapse by sum
  mat = tapply(mat[,3], list(mat$FROMBZONE,mat$TOBZONE), sum)
  mat = data.frame(FROMBZONE=rep(rownames(mat),each=length(rownames(mat))), 
    TOBZONE=rep(rownames(mat), length(rownames(mat))), as.vector(t(mat)))
  colName = gsub("_MAT.RData","",matsToProcess[i])
  colnames(mat)[3] = colName
    
  #write temp table and join to TRIPMATRIX
  if(i==1) {
    dbWriteTable(db, "TRIPMATRIX", mat, append=T, row.names=F)
  } else {
    dbWriteTable(db, "Temp", mat, append=T, row.names=F)
    dbGetQuery(db, "CREATE INDEX TempIndex ON Temp (FROMBZONE, TOBZONE)")
    dbGetQuery(db, paste("CREATE TABLE Temp2 AS SELECT TRIPMATRIX.*, ", colName,
      " FROM TRIPMATRIX LEFT OUTER JOIN Temp ON TRIPMATRIX.FromBZone=Temp.FromBZone AND TRIPMATRIX.ToBZone=Temp.ToBZone", sep=""))
    dbGetQuery(db, "DROP TABLE Temp")
    dbGetQuery(db, "DROP TABLE TRIPMATRIX")
    dbGetQuery(db, "ALTER TABLE Temp2 RENAME TO TRIPMATRIX")
    dbGetQuery(db, "CREATE INDEX TRIPMATRIXIndex ON TRIPMATRIX (FromBZone, ToBZone)")
  }
  
  #delete file
  file.remove(matsToProcess[i])
}

#########################################################################
#Create population and employment at the alpha zone level
#########################################################################

emp = read.csv("Employment.csv")
synpop_h = read.csv("SynPopH.csv")
synpop_tazsummary = read.csv("SynPop_Taz_Summary.csv")

#population
aZonePop = tapply(synpop_h$PERSONS, synpop_h$Azone, sum)

#employment
popandemp = data.frame(AZONE=sort(emp$Azone),POPULATION=0,EMPLOYMENT=0)
popandemp$POPULATION = aZonePop[match(popandemp$AZONE, names(aZonePop))]
popandemp$EMPLOYMENT = emp$Total[match(popandemp$AZONE, emp$Azone)]

#synpop_taz fields
colnames(synpop_tazsummary) = toupper(colnames(synpop_tazsummary))
popandemp = merge(popandemp, synpop_tazsummary, by.x="AZONE", by.y="TAZ")

dbWriteTable(db, "AZONE", popandemp, row.names=F)

#########################################################################
#Create MODELWIDE table
#########################################################################

#create table to store data in
modelwide = data.frame(DATA=c(),VALUE=c())

#Activity Summary data
actsum = read.csv("ActivitySummary.csv")
actsum$Activity = gsub(" |&","_",as.character(actsum$Activity)) #replace " " & "&"

modelwide = rbind(modelwide, 
  data.frame(DATA=paste(actsum$Activity, "CompositeUtility", sep="_"), VALUE=actsum$CompositeUtility))

modelwide = rbind(modelwide, 
  data.frame(DATA=paste(actsum$Activity, "Size", sep="_"), VALUE=actsum$Size))

#construction dollars
constDollars = read.csv("construction_forecast.csv")
constDollars$activity = gsub(" |&","_",as.character(constDollars$activity)) #replace " " & "&"

modelwide = rbind(modelwide, 
  data.frame(DATA=paste(constDollars$activity, "TotalDollars", sep="_"), VALUE=constDollars$dollars))

dbWriteTable(db, "MODELWIDE", modelwide, row.names=F)

#########################################################################
#Close and compact database
#########################################################################

#Close and compact database
dbGetQuery(db, "VACUUM")
sqliteCloseConnection(db)
cat(paste("SWIM VIZ DB for", databaseFileName, "at", Sys.time(), "Created \n"))
quit("no")


