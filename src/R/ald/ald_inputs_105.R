#/
# Define locations of inputs for ALD
# @filename ald_inputs_105.R
# @author Brian Gregor
# @version 1.5
# @date 6/16/04
#/


##/
 # Identify the locations/names where the input files reside and where the outputs will be written
 # @param dataPath - see ALD.R
 # @param currYearIndex - see ALD.R
 # @param prevYearIndex - see ALD.R
 # @return EDDirectory
 # @return PIDirectory
 # @return ALDCurrDirectory
 # @return ALDPrevDirectory
##/
ReferenceDirectory <- paste(dataPath, "reference", sep="")
EDDirectory <- paste(dataPath, "t", currYearIndex, "/ed", sep="")
PIDirectory <- paste(dataPath, "t", prevYearIndex, "/pi", sep="")
ALDBaseDirectory <- paste(dataPath, "t", baseYearIndex, "/ald", sep="")
ALDCurrDirectory <- paste(dataPath, "t", currYearIndex, "/ald", sep="")
ALDPrevDirectory <- paste(dataPath, "t", prevYearIndex, "/ald", sep="")

##/
 # Identify the input file path names
 # @return input - a list of the locations/names of the input files
##/
input <- list()
input$zoningDefinitions         <-      paste(ALDBaseDirectory, "zoning_definitions.csv", sep="/")
input$floorspaceDefinitions     <-      paste(ALDBaseDirectory, "floorspace_definitions.csv", sep="/")
input$alphaBetaCorrespondence   <-      paste(ReferenceDirectory, "alpha2beta.csv", sep="/")
input$nivqData                  <-      paste(EDDirectory, "ConstructionDollarDataForALD.csv", sep="/")
input$exchangeResults           <-      paste(PIDirectory, "ExchangeResults.csv", sep="/")
input$floorQuantities           <-      paste(ALDPrevDirectory, "FloorspaceInventory.csv", sep="/")
input$constructionCosts         <-      paste(ALDBaseDirectory, "ConstructionCosts.csv", sep="/")
input$zoningQuantities          <-      paste(ALDBaseDirectory, "LandSQFTxZoning.csv", sep="/")
input$zoningCompatibility       <-      paste(ALDBaseDirectory, "zoning_compatibility_v2.csv", sep="/")
input$floorAreaRatios           <-      paste(ALDBaseDirectory, "far_v2.csv", sep="/")

##/
 # Define the coefficients for the model
 # @return cf - a list of the model coefficients
##/
cf <- list()
cf$rLq                                  <- 0.1      # equation 01, Lambda for residential space
cf$nLq                                  <- 0.1      # equation 01, Lambda for nonresidential space
cf$rBq1                                 <- 0.0001   # equation 02, Bq1 for residential space
cf$nBq1                                 <- 0.0001   # equation 02, Bq1 for nonresidential space
cf$rBq2                                 <- 1000     # equation 02, Bq2 for residential space
cf$nBq2                                 <- 1000     # equation 02, Bq2 for nonresidential space
cf$asc$res["FLR MH"]                    <- 2290     # equation 02, ASC for FLR MH
cf$asc$res["FLR MF"]                    <- 1573     # equation 02, ASC for FLR MF
cf$asc$res["FLR AT"]                    <- 2574     # equation 02, ASC for FLR AT
cf$asc$res["FLR SFD"]                   <- 0        # equation 02, ASC for FLR SFD
cf$asc$res["FLR RRMH"]                  <- 2742     # equation 02, ASC for FLR RRMH
cf$asc$res["FLR RRSFD"]                 <- 1022     # equation 02, ASC for FLR RRSFD
cf$asc$nres["FLR Accommodation"]        <- 2018     # equation 02, ASC for FLR Accommodation
cf$asc$nres["FLR Depot"]                <- 582      # equation 02, ASC for FLR Depot
cf$asc$nres["FLR Government Support"]   <- 2042     # equation 02, ASC for FLR Government Support
cf$asc$nres["FLR Grade-school"]         <- 1111     # equation 02, ASC for FLR Grade-school
cf$asc$nres["FLR Heavy Industry"]       <- 992      # equation 02, ASC for FLR Heavy Industry
cf$asc$nres["FLR Hospital"]             <- 2737     # equation 02, ASC for FLR Hospital
cf$asc$nres["FLR Institutional"]        <- 2337     # equation 02, ASC for FLR Institutional
cf$asc$nres["FLR Light Industry"]       <- 1196     # equation 02, ASC for FLR Light Industry
cf$asc$nres["FLR Office"]               <- 547      # equation 02, ASC for FLR Office
cf$asc$nres["FLR Retail"]               <- 0        # equation 02, ASC for FLR Retail
cf$asc$nres["FLR Warehouse"]            <- 2180     # equation 02, ASC for FLR Warehouse
cf$Lp					                <- 0.1      # Lambda for zoning compatibility utility
cf$rB3f                                 <- 0.549    # equation 07, B3f for residential space
cf$nB3f                                 <- 0.966    # equation 07, B3f for nonresidential space
cf$rB4f                                 <- 1.997    # equation 07, B4f for residential space
cf$nB4f                                 <- 0.815    # equation 07, B4f for nonresidential space
cf$rB5f                                 <- 0.896    # equation 10, B5f for residential space
cf$nB5f                                 <- 1.014    # equation 10, B5f for nonresidential space
cf$rB6f                                 <- 0.5      # equation 10, B6f for residential space
cf$nB6f                                 <- 0.25     # equation 10, B6f for nonresidential space
cf$rB7f                                 <- 0.894    # equation 10, B7f for residential space
cf$nB7f                                 <- 1.22     # equation 10, B7f for nonresidential space
cf$rB8f                                 <- 2.737    # equation 10, B8f for residential space
cf$nB8f                                 <- 4        # equation 10, B8f for nonresidential space
cf$rB9f                                 <- 0.001    # equation 10, B9f for residential space
cf$nB9f                                 <- 0.001    # equation 10, B9f for nonresidential space
cf$rDf                                  <- 0.050    # equation 06, Df for residential space
cf$nDf                                  <- 0.1      # equation 07, Df for nonresidential space

