
#Create output average VIZ DB based on a series of queries
#Ben Stabler, stabler@pbworld.com, 081810

#Input parameters: 
#1) VIZ DBs to average
#2) Output SQLite database filename
#3) A list of average field lists, that contains:
	#a) tableName - the name of the table to query
	#b) keys - a string of comma separated table primary keys to ensure unique records are returned
	#c) fields - a string of comma separated table fields to average (via simple average)
	
#########################################################################
#parameters
#########################################################################

dbFileNames = c("E:/swimviz/SCEN_2006to2036_FrtPlanRef_NoTransit.db",
								"E:/swimviz/SCEN_2006to2036FrtPlanHiCost.db")

outputDbFileName = "E:/swimviz/average_test.db"

averageFields = list()
averageFields[[1]] = c(tableName="LINK_DATA",keys="TSTEP,ANODE,BNODE,ASSIGNCLASS",fields="TIME_AM,VOL_AM,TIME_MD,VOL_MD,TIME_PM,VOL_PM,TIME_NT,VOL_NT")
averageFields[[2]] = c(tableName="TRIPMATRIX",keys="TSTEP,FROMBZONE,TOBZONE",fields="AM_DA")

#########################################################################
#functions
#########################################################################

buildQuery = function(averageFieldList) {

	#get info
	tableName = averageFieldList["tableName"]
	keys = averageFieldList["keys"]
	fields = averageFieldList["fields"]
	
	query = paste("SELECT", keys, ",", fields, "FROM", tableName)
	return(query)
}

#########################################################################
#create output database
#########################################################################

library(RSQLite)
m = dbDriver("SQLite")

#create database
cat(paste("Create Average SWIM VIZ DB", outputDbFileName, "at", Sys.time(), "\n"))
if(file.exists(outputDbFileName)) {
  file.remove(outputDbFileName)
}
file.create(outputDbFileName)
db = dbConnect(m, dbname = outputDbFileName)

#########################################################################
#loop through DBs and query and average 
#########################################################################

#loop through queries
for(i in 1:length(averageFields)) {

	#loop through DBs
	for(j in 1:length(dbFileNames)) {
	
		#connect to input db and query
		cat(paste("Connect to", dbFileNames[j], "\n"))
		inDB = dbConnect(m, dbname = dbFileNames[j])
		query = buildQuery(averageFields[[i]])
		cat(paste(query, "\n"))
		tableData = dbGetQuery(inDB, query)
		colnames(tableData) = toupper(colnames(tableData))
		
		#get fields to average as a vector
		fieldsAsVector = strsplit(averageFields[[i]]["fields"],",")[[1]]
		
		#add together
		if(j==1) {
			sumData = tableData
		} else {
		
			if(nrow(sumData) == nrow(tableData)) {
				#loop through fields
				for(f in fieldsAsVector) {
				  cat(paste("Add", f, "\n"))
					sumData[,f] = sumData[,f] + tableData[,f] #assumes row order is the same across DBs
				}
			} else {
					stop("Different number of resulting records across databases for the query")
			}
			
		}
	}
	
	#simple average of results
	for(f in fieldsAsVector) {
	  cat(paste("Average", f, "\n"))
		sumData[,f] = sumData[,f] / length(dbFileNames)
	}
	
	#write result to output db
	dbWriteTable(db, averageFields[[i]]["tableName"], sumData, row.names=F, append=F)

}

#########################################################################
#close and complete
#########################################################################

#close database
sqliteCloseConnection(db)
sqliteCloseConnection(inDB)

cat(paste("SWIM Average VIZ DB Complete at", Sys.time(), "\n"))
