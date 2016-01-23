
#Ben Stabler, stabler@pbworld.com, 080509
#Brian Gregor, Brian.J.GREGOR@odot.state.or.us, 052913
#Write updated to use 7zip - requires 7zip installed: http://www.7-zip.org
# Ben Stabler, ben.stabler@rsginc.com, 01/23/15
#Read updated to use gzcon and unz, which is much faster than before
# Ben Stabler, ben.stabler@rsginc.com, 01/22/16
#
#readZipMat("sovDistAm.zmx")
#writeZipMat(Matrix, "sovDistAm.zmx")

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
