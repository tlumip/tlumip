
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
