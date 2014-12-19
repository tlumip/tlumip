
#Check SWIM skims
#Ben Stabler, stabler@pbworld.com, 01/14/14

################################################################################

skimDir = "D:/swim2/base_scenario/outputs/t19"
os = c(296,1,137,801,13) #row indexes, not zone numbers
ds = c(296,1,137,801,13) #column indexes, not zone numbers
outFileName = "skimCheck.csv"
skims = c('pkwltfivt',
          'opwltfivt',
          'pkwltfovt',
          'opwltfovt',
          'pkwicrivt',
          'pkwicrfwt',
          'pkwicrtwt',
          'pkwicrxwk',
          'pkwicrawk',
          'pkwicrewk',
          'pkwicrfar',
          'opwicrivt',
          'opwicrfwt',
          'opwicrtwt',
          'opwicrxwk',
          'opwicrawk',
          'opwicrewk',
          'opwicrfar',
          'pkwtivt',
          'pkwtfwt',
          'pkwttwt',
          'pkwtxwk',
          'pkwtawk',
          'pkwtewk',
          'pkwtbrd',
          'pkwtfar',
          'opwtivt',
          'opwtfwt',
          'opwttwt',
          'opwtxwk',
          'opwtawk',
          'opwtewk',
          'opwtbrd',
          'opwtfar',
          'pkautofftime',
          'pkautotime',
          'pkautodist',
          'pkautotoll',
          'pktrk1fftime',
          'pktrk1time',
          'pktrk1dist',
          'pktrk1toll',
          'opautofftime',
          'opautotime',
          'opautodist',
          'opautotoll',
          'optrk1fftime',
          'optrk1time',
          'optrk1dist',
          'optrk1toll')
          
################################################################################

#Read ZMX File with unz file connection
readZipMat = function(FileName) {
  
  #Make a temporary directory to put unzipped files into
  tDir = tempdir()
  dir.create(tDir,showWarnings=F)
  
  #Read matrix attributes
  NumRows = as.integer( scan( unzip( FileName, "_rows", exdir=tDir), what="", quiet=T ) )
  NumCols = as.integer( scan( unzip( FileName, "_columns", exdir=tDir), what="", quiet=T ) )
  RowNames = strsplit( scan( unzip( FileName, "_external row numbers", exdir=tDir), what="", quiet=T ),"," )[[1]]
  ColNames = strsplit( scan( unzip( FileName, "_external column numbers", exdir=tDir), what="", quiet=T ),"," )[[1]]
  
  #Initialize matrix to hold values
  Result.ZnZn = matrix( 0, NumRows, NumCols )
  rownames( Result.ZnZn ) = RowNames
  colnames( Result.ZnZn ) = ColNames
  
  #Read matrix data by row and place in initialized matrix
  RowDataEntries. = paste("row_", 1:NumRows, sep="")
  for(i in 1:NumRows) {
    Result.ZnZn[ i, ] = readBin( unzip( FileName, RowDataEntries.[i], exdir=tDir),
                                 what=double(), n=NumCols, size=4, endian="big" )
  }
  
  #Remove the temporary directory
  unlink(tDir, recursive=TRUE)
  
  #Return the matrix
  Result.ZnZn
}

################################################################################

#Read files and output table of skim values
setwd(skimDir)
output = data.frame()
for(i in 1:length(skims)) {
  skim = skims[i]
  print(paste("read", skim))
  x = readZipMat(paste(skim, ".zmx", sep=""))
  for(o in os) {
    for(d in ds) {
      value = x[o,d]
      row = data.frame(skim, o, d, value)
      output = rbind(output, row)
    }
  }
}
write.csv(output, outFileName, row.names=F,quote=F)
