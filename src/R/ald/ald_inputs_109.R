#/
# Define locations of inputs for ALD
# @filename ald_inputs_109.R
# @author Brian Gregor, Tara Weidner
# @version 1.9
# @date 9/30/04
#/


##/
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
input$landSqft                  <-      paste(ALDBaseDirectory, "LandSQFTxFLR.csv", sep="/")

##/
 # Define the coefficients for the model
 # @return cf - a list of the model coefficients
##/
cf <- list()
cf$rLq                                  <- 0.1      	# equation 01, Lambda for residential space
cf$nLq                                  <- 0.1      	# equation 01, Lambda for nonresidential space
cf$rBq1["FLR MH"]                       <- -4.4220613   # equation 02, Bq1 for FLR MH
cf$rBq1["FLR MF"]                       <- -23.9145878  # equation 02, Bq1 for FLR MF
cf$rBq1["FLR AT"]                       <- -11.1609317  # equation 02, Bq1 for FLR AT
cf$rBq1["FLR SFD"]                      <- -16.3775386  # equation 02, Bq1 for FLR SFD
cf$rBq1["FLR RRMH"]                     <- -4.4220613   # equation 02, Bq1 for FLR RRMH
cf$rBq1["FLR RRSFD"]                    <- -16.3775386  # equation 02, Bq1 for FLR RRSFD
cf$nBq1["FLR Accommodation"]            <- -6.1740102  	# equation 02, Bq1 for FLR Accommodation
cf$nBq1["FLR Depot"]                    <- -10.8359135  # equation 02, Bq1 for FLR Depot
cf$nBq1["FLR Government Support"]       <- -3.7960069   # equation 02, Bq1 for FLR Government Support
cf$nBq1["FLR Grade-school"]             <- -6.1740102   # equation 02, Bq1 for FLR Grade-school
cf$nBq1["FLR Heavy Industry"]           <- -10.8359135  # equation 02, Bq1 for FLR Heavy Industry
cf$nBq1["FLR Hospital"]                 <- -6.1740102   # equation 02, Bq1 for FLR Hospital
cf$nBq1["FLR Institutional"]            <- -6.1740102   # equation 02, Bq1 for FLR Institutional
cf$nBq1["FLR Light Industry"]           <- -10.8359135  # equation 02, Bq1 for FLR Light Industry
cf$nBq1["FLR Office"]                   <- -6.1740102   # equation 02, Bq1 for FLR Office
cf$nBq1["FLR Retail"]                   <- -3.7960069  	# equation 02, Bq1 for FLR Retail
cf$nBq1["FLR Warehouse"]                <- -10.8359135  # equation 02, Bq1 for FLR Warehouse
cf$rBq2                                 <- 1     	# equation 02, Bq2 for residential space
cf$nBq2                                 <- 1     	# equation 02, Bq2 for nonresidential space
cf$asc$res["FLR MH"]                    <- 0.0687179    # equation 02, ASC for FLR MH
cf$asc$res["FLR MF"]                    <- 0.0898928    # equation 02, ASC for FLR MF
cf$asc$res["FLR AT"]                    <- -0.7650127   # equation 02, ASC for FLR AT
cf$asc$res["FLR SFD"]                   <- 0.25025      # equation 02, ASC for FLR SFD
cf$asc$res["FLR RRMH"]                  <- 0.0687179    # equation 02, ASC for FLR RRMH
cf$asc$res["FLR RRSFD"]                 <- 0.25025     	# equation 02, ASC for FLR RRSFD
cf$asc$nres["FLR Accommodation"]        <- 1.4879747    # equation 02, ASC for FLR Accommodation
cf$asc$nres["FLR Depot"]                <- 1.284698     # equation 02, ASC for FLR Depot
cf$asc$nres["FLR Government Support"]   <- 1.055263     # equation 02, ASC for FLR Government Support
cf$asc$nres["FLR Grade-school"]         <- 1.4879747    # equation 02, ASC for FLR Grade-school
cf$asc$nres["FLR Heavy Industry"]       <- 1.284698     # equation 02, ASC for FLR Heavy Industry
cf$asc$nres["FLR Hospital"]             <- 1.4879747    # equation 02, ASC for FLR Hospital
cf$asc$nres["FLR Institutional"]        <- 1.4879747    # equation 02, ASC for FLR Institutional
cf$asc$nres["FLR Light Industry"]       <- 1.284698     # equation 02, ASC for FLR Light Industry
cf$asc$nres["FLR Office"]               <- 1.4879747    # equation 02, ASC for FLR Office
cf$asc$nres["FLR Retail"]               <- 1.055263     # equation 02, ASC for FLR Retail
cf$asc$nres["FLR Warehouse"]            <- 1.284698     # equation 02, ASC for FLR Warehouse
cf$Lp					                <- 2.0      # equation 05, Lambda for zoning compatibility utility
cf$Stc                                  <- 1.0      # equation 05, Size coefficient for zoning compatibility utility
cf$rB3f                                 <- 0.549    # equation 09, B3f for residential space
cf$nB3f                                 <- 0.966    # equation 09, B3f for nonresidential space
cf$rB4f                                 <- 1.997    # equation 09, B4f for residential space
cf$nB4f                                 <- 0.815    # equation 09, B4f for nonresidential space
cf$rB5f                                 <- 0.896    # equation 12, B5f for residential space
cf$nB5f                                 <- 1.014    # equation 12, B5f for nonresidential space
cf$rB6f                                 <- 0.5      # equation 12, B6f for residential space
cf$nB6f                                 <- 0.25     # equation 12, B6f for nonresidential space
cf$rB7f                                 <- 0.894    # equation 12, B7f for residential space
cf$nB7f                                 <- 1.22     # equation 12, B7f for nonresidential space
cf$rB8f                                 <- 2.737    # equation 12, B8f for residential space
cf$nB8f                                 <- 4        # equation 12, B8f for nonresidential space
cf$rB9f                                 <- 0.001    # equation 12, B9f for residential space
cf$nB9f                                 <- 0.001    # equation 12, B9f for nonresidential space
cf$rDf                                  <- 0.050    # equation 08, Df for residential space
cf$nDf                                  <- 0.1      # equation 08, Df for nonresidential space

