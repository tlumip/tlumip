
#Convert ZMX to OMX
#Ben Stabler, ben.stabler@rsginc.com, 113015
#convertZMXtoOMX("pmautodist.zmx") #outputs pmautodist.omx

source("readZMXFile.R")
source("omx.r") #Requires rhdf5 package from bioconductor

convertZMXtoOMX = function(fName) {
  x = readZipMat(fName)
  outFName = gsub(".zmx",".omx",fName)
  matName = gsub(".zmx","",fName)
  createFileOMX(outFName, nrow(x), ncol(x), 7)
  writeMatrixOMX(outFName,x, matName)
  writeLookupOMX(outFName, rownames(x), "NO")
}

