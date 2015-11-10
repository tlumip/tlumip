
#Write ZMX file
#Ben Stabler, stabler@pbworld.com, 080509
#writeZipMat(Matrix, "sovDistAm.zmx")
#Updated to use 7zip, ben.stabler@rsginc.com, 01/23/15
#Requires 7zip installed: http://www.7-zip.org

writeZipMat = function(Matrix, FileName, SevenZipExe="C:/Program Files/7-Zip/7z.exe") {
  
  #Make a temporary directory to put unzipped files into
  tempDir = tempdir()
  print(tempDir)
  oldDir = getwd()
  setwd(tempDir)

  #Write matrix attributes
  cat(2, file="_version")
  cat(FileName, file="_name")
  cat(FileName, file="_description")
  
  cat(nrow(Matrix), file="_rows")
  cat(ncol(Matrix), file="_columns")
  cat(paste(rownames(Matrix),collapse=","), file="_external row numbers")
  cat(paste(colnames(Matrix),collapse=","), file="_external column numbers")
  
  #Write rows
  for(i in 1:nrow(Matrix)) {
    writeBin(Matrix[i,], paste("row_", i, sep=""), size=4, endian="big")
  }
  
  #Create file
  filesToInclude = normalizePath(dir(tempDir, full.names=T))
  filesToInclude = paste(paste('"', filesToInclude, '"\n', sep=""), collapse=" ")
  listFileName = paste(tempDir, "\\listfile.txt", sep="")
  write(filesToInclude, listFileName)
  setwd(oldDir)
  command = paste(paste('"', SevenZipExe, '"', sep=""), " a -tzip ", FileName, " @", listFileName, sep="")
  system(command)
}
