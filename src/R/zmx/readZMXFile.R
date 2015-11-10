
#Read a PB ZMX file
#Ben Stabler, stabler@pbworld.com, 080509
#Brian Gregor, Brian.J.GREGOR@odot.state.or.us, 052913
#readZipMat("sovDistAm.zmx")

#Read ZMX File with unz file connection
readZipMat = function(FileName) {
  
  #Make a temporary directory to put unzipped files into
  dir.create( "TempZip" )
  
  #Read matrix attributes
  NumRows = as.integer( scan( unzip( FileName, "_rows", exdir="TempZip" ), what="", quiet=T ) )
  NumCols = as.integer( scan( unzip( FileName, "_columns", exdir="TempZip" ), what="", quiet=T ) )
  RowNames = strsplit( scan( unzip( FileName, "_external row numbers", exdir="TempZip" ), what="", quiet=T ),"," )[[1]]
  ColNames = strsplit( scan( unzip( FileName, "_external column numbers", exdir="TempZip" ), what="", quiet=T ),"," )[[1]]
  
  #Initialize matrix to hold values
  Result.ZnZn = matrix( 0, NumRows, NumCols )
  rownames( Result.ZnZn ) = RowNames
  colnames( Result.ZnZn ) = ColNames
  
  #Read matrix data by row and place in initialized matrix
  RowDataEntries. = paste("row_", 1:NumRows, sep="")
  for(i in 1:NumRows) {
    Result.ZnZn[ i, ] = readBin( unzip( FileName, RowDataEntries.[i], exdir="TempZip" ),
                                 what=double(), n=NumCols, size=4, endian="big" )
  }
  
  #Remove the temporary directory
  unlink( "TempZip", recursive=TRUE )
  
  #Return the matrix
  Result.ZnZn
}
