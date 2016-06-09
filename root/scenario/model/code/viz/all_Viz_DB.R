
#script to create and merge all time step VIZ DBs for a scenario
#requires R in the environment %PATH% variable to run buld_viz_db.R
#R --no-save < z:/viz/all_viz_db.R > log.txt BUILDDBS=F BUILDSCRIPTFILENAME="Z:/viz/build_Viz_DB.R" 
#  ALLZONESFILENAME="Z:/viz/allzones.csv" SPATIALONLY=F
#  INDCODEFILE="z:/models/oregon2tm/parameters/IndustryOccupationSplitIndustryCorrespondence.csv" MICRO=F
#Ben Stabler, stabler@pbworld.com, 040509

library(RSQLite)

#########################################################################
#parameters
#########################################################################

buildDBs = Sys.getenv("BUILDDBS") #T #create tstep databases before merging
buildScriptFileName = Sys.getenv("BUILDSCRIPTFILENAME") #"z:/viz/build_Viz_DB.R"

#passed to build_Viz_DB.R if needed
allZonesFileName = Sys.getenv("ALLZONESFILENAME") #"z:/viz/allzones.csv"
genSpatialOnly = Sys.getenv("SPATIALONLY") #F

#passed to build_MicroViz_DB.R if needed
INDCODEFILE = Sys.getenv("INDCODEFILE") #IndustryOccupationSplitIndustryCorrespondence.csv"

isMicro = Sys.getenv("MICRO") #T or F to run Micro VIZ DB creation; default is F

#########################################################################
#calculated parameters
#########################################################################

wkrDir = getwd()
tstepFolders = dir(wkrDir)[file.info(dir(wkrDir))$isdir]
tstepFolders = tstepFolders[grep("t",tstepFolders)]
tsteps = gsub("t","",tstepFolders)

#########################################################################
#functions
#########################################################################

getFieldsFromString = function(SQLString) {
  startPos = which(strsplit(SQLString,NULL)[[1]] == "(")
  endPos = which(strsplit(SQLString,NULL)[[1]] == ")")
  return(substr(SQLString,startPos+1, endPos-1))
}

#########################################################################
#create database for each tstep 
#########################################################################

if(buildDBs=="T") {
  for(i in 1:length(tsteps)) {
    if(isMicro=="T") {
      cat(paste("SWIM MICRO VIZ DB for", tsteps[i], "at", Sys.time(), "\n"))
    } else {
      cat(paste("SWIM VIZ DB for", tsteps[i], "at", Sys.time(), "\n"))
    }
    setwd(paste("t", tsteps[i], sep=""))
    #requires R in the environment %PATH% variable
    system(paste('R --no-save --no-restore < "', buildScriptFileName, '" > log.txt', 
      ' ALLZONESFILENAME=', allZonesFileName, ' SPATIALONLY=', genSpatialOnly, 
      ' INDCODEFILE=', INDCODEFILE, sep=""), invisible=F)
    setwd("../")
  }
}

#########################################################################
#create database
#########################################################################

m = dbDriver("SQLite")


#create database
sn = basename(dirname(wkrDir))
if(isMicro=="T") {
  databaseFileName = paste(sn, "_micro.db", sep="")
  if(file.exists(databaseFileName)) {
    file.remove(databaseFileName)
  }
  cat(paste("Create SWIM MICRO VIZ DB for", databaseFileName, "at", Sys.time(), "\n"))
  dbFileNames = dir(recursive=T)[grep("_micro.db$",dir(recursive=T))]
} else {
  databaseFileName = paste(sn, ".db", sep="")
  if(file.exists(databaseFileName)) {
    file.remove(databaseFileName)
  }
  cat(paste("Create SWIM VIZ DB for", databaseFileName, "at", Sys.time(), "\n"))
  dbFileNames = dir(recursive=T)[grep(".db$",dir(recursive=T))]
}

#create scenario database file
file.create(databaseFileName)
db = dbConnect(m, dbname = databaseFileName)

#########################################################################
#create TSTEP table
#########################################################################

tsteps = gsub("t","",dirname(dbFileNames))
tstepsAlpha = formatC(as.numeric(tsteps),flag="0",width=2)

#reorder for numeric sort
dbFileNames = dbFileNames[order(tstepsAlpha)]
tsteps = tsteps[order(tstepsAlpha)]
tstepsAlpha = tstepsAlpha[order(tstepsAlpha)]

#write TSTEP table
if(isMicro!="T") {
  dbWriteTable(db, "TSTEP", data.frame(TSTEP=tsteps), row.names=F)
}

#########################################################################
#add databases and copy over tables
#########################################################################

indexes = list() #to keep track of created indexes

for(i in 1:length(tsteps)) {
    cat(paste("Working on file ",dbFileNames[i],"\n"))
  #get tstep database
  tdb = dbConnect(m, dbname = dbFileNames[i])
  dbSchema = dbGetQuery(tdb, "SELECT * FROM SQLITE_MASTER")
  dbSchema = dbSchema[!is.na(dbSchema$sql),] #remove auto index entries

  #loop through all entries
  for(j in 1:nrow(dbSchema)) {
    
    #TABLES
    if(dbSchema[j,"type"] == "table") { 
      tableName = dbSchema[j,"name"]
      query = paste("SELECT * FROM ", tableName, sep="")
      cat(paste(tsteps[i], ": ", query, "\n"))
      tableData = dbGetQuery(tdb, query)
      
      #add TSTEP field
      if(!(tableName=="ALLZONES" | tableName=="BZONE" | tableName=="COUNTLOCATIONS")) { #skip since not by year
        tableData$TSTEP = tsteps[i]
      }
      
      #write table to DB
      if(dbExistsTable(db, tableName)){
        if(!(tableName=="ALLZONES" | tableName=="BZONE" | tableName=="COUNTLOCATIONS")) { #skip since not by year
          
          #add missing fields if needed and reorder columns
          targetTableData = dbGetQuery(db, paste("SELECT * FROM", tableName, "LIMIT 1")) #to get structure
          targetColNames = toupper(colnames(targetTableData))
          colNames = colnames(tableData) = toupper(colnames(tableData))
          
          if(any(!(targetColNames %in% colNames))) { #if new table has less fields than db table
            missingFields = targetColNames[!(targetColNames %in% colNames)]
            for(k in 1:length(missingFields)) {
              tableData[[missingFields[k]]] = 0 #add field
            }
          } 
          
          if(any(!(colNames %in% targetColNames))) { #if db table has less fields than new table
            missingFields = colNames[!(colNames %in% targetColNames)]
            for(k in 1:length(missingFields)) {
              query = paste("ALTER TABLE", tableName, "ADD COLUMN ", missingFields[k], "REAL")
              cat(paste(query, "\n"))
              dbGetQuery(db, query) #add field
            }
          }
          
          #get db table col names again and write data to database
          targetTableData = dbGetQuery(db, paste("SELECT * FROM", tableName, "LIMIT 1")) #to get structure
          targetColNames = toupper(colnames(targetTableData))
          tableData = tableData[,targetColNames] #reorder
          dbWriteTable(db, tableName, tableData, row.names=F, append=T)
        }
      } else {
        dbWriteTable(db, tableName, tableData, row.names=F, append=F)
      }
      
    }
    
    #INDEXES
    if(dbSchema[j,"type"] == "index") { 
      tableNameIndex = dbSchema[j,"tbl_name"]
      fields = getFieldsFromString(dbSchema[j,"sql"])
      indexName = dbSchema[j,"name"]
      #create index with TSTEP field as well
      query = paste("CREATE INDEX ", indexName, " ON ", tableNameIndex, " (TSTEP,", fields, ")", sep="")
      if(!(query %in% indexes)) {
        #check if index already defined
        indexes = c(indexes, query)
        cat(paste(query, "\n"))
        dbGetQuery(db, query)
      }
    }
  }
  
  #close tstep database
  dbDisconnect(tdb)
    cat(paste("Done with file ",dbFileNames[i],"\n"))
}

#########################################################################
#Close
#########################################################################

dbDisconnect(db)
if(isMicro=="T") {
  cat(paste("SWIM MICRO VIZ DB for", databaseFileName, "at", Sys.time(), "Created \n"))
} else {
  cat(paste("SWIM VIZ DB for", databaseFileName, "at", Sys.time(), "Created \n"))
}
quit("no")

