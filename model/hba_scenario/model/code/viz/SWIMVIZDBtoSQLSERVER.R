
#########################################################################
#parameters
#########################################################################

sqliteDBFileName = "C:/projects/odot/tlumip/viz/SCEN_2006to2036_FrtPlanRef_NoTransit.db"

outputFolder = "C:/projects/odot/tlumip/viz/sqloutput"
outputSQLFileName = "BulkInsertScenario.sql"
sqlServerDBName = "SWIM"

writeCSVs = TRUE

#########################################################################
#functions
#########################################################################

getFieldsFromString = function(SQLString) {
  startPos = which(strsplit(SQLString,NULL)[[1]] == "(")
  endPos = which(strsplit(SQLString,NULL)[[1]] == ")")
  return(substr(SQLString,startPos+1, endPos-1))
}

writeTableCreate = function(tableName, SQLiteSQL, indexFields, schemaName, outFileCon) {

  SQLiteSQL = gsub("CREATE TABLE +", paste("CREATE TABLE ", sn, colapse=".", sep=""), SQLiteSQL)
  SQLiteSQL = gsub(" TEXT", " VARCHAR(100)", SQLiteSQL)
  if(indexFields != "") {
    keyString = paste(",CONSTRAINT", paste(tableName, "_pkey", sep=""), "PRIMARY KEY (", indexFields, "))")
    SQLiteSQL = gsub(")$", keyString, SQLiteSQL)
  }
  cat(SQLiteSQL, ";\n", file=outFileCon)
  
  bulkInsertStr = paste("BULK INSERT", paste(schemaName, tableName, sep="."), "FROM", paste('"', tableName, ".csv", '"', sep=""),
   "WITH ( FIELDTERMINATOR=',', ROWTERMINATOR='\\n', FIRSTROW=2, MAXERRORS=0, TABLOCK);\n")
  cat(bulkInsertStr, file=outFileCon)
  
}

#########################################################################
#create database connection and output
#########################################################################

library(RSQLite)

cat(paste("SWIM VIZ DB to SQL SERVER Started at", Sys.time(), "\n"))

m = dbDriver("SQLite")
db = dbConnect(m, dbname = sqliteDBFileName)

#get scenario name
sn = gsub(".db","",basename(sqliteDBFileName))

#create output folder
if(!file.exists(outputFolder)) {
  dir.create(outputFolder)
}
setwd(outputFolder)

#create output sql file
outFile = file(outputSQLFileName, "wt")

#########################################################################
#write start sql script - requires scenario not already in the DB
#########################################################################

cat(paste("USE",  sqlServerDBName, ";\n"), file=outFile)
cat(paste("ALTER DATABASE",  sqlServerDBName, "SET RECOVERY BULK_LOGGED;\n"), file=outFile)
cat(paste("GO\nCREATE SCHEMA",  sn, ";\nGO\n"), file=outFile)

#########################################################################
#loop through the tables and indexes and write CSVs + BULK LOAD SQL
#########################################################################

dbSchema = dbGetQuery(db, "SELECT * FROM SQLITE_MASTER")
dbSchema$index = ""

#loop through all entries and get indexes for later
#assumes only 0 or 1 indexes per table 
for(j in 1:nrow(dbSchema)) {
  
  #INDEXES
  if(dbSchema[j,"type"] == "index") { 
    tableNameIndex = dbSchema[j,"tbl_name"]
    fields = getFieldsFromString(dbSchema[j,"sql"])
    dbSchema$index[match(tableNameIndex, dbSchema$name)] = fields
  }
}

#write table to CSV and SQL for bulk loading
for(j in 1:nrow(dbSchema)) {
  
  #TABLES
  if(dbSchema[j,"type"] == "table") { 
    tableName = dbSchema[j,"name"]
    if(writeCSVs) {
      tableData = dbGetQuery(db, paste("SELECT * FROM ", tableName))
      write.csv(tableData, paste(tableName, ".csv", sep=""), row.names=F, na="", quote=F)
      rm(tableData)
    }
    writeTableCreate(dbSchema[j,"name"], dbSchema[j,"sql"], dbSchema[j,"index"], sn, outFile)
    cat(paste(tableName, "Written \n"))
  }
}

#close database
sqliteCloseConnection(db)

#########################################################################
#write end of sql script
#########################################################################

cat(paste("ALTER DATABASE",  sqlServerDBName, "SET RECOVERY FULL;\n"), file=outFile)
close(outFile)
cat(paste("SWIM VIZ DB Written Out for Bulk Loading into SQL SERVER at", Sys.time(), "\n"))
