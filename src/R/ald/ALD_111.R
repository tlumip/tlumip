#/
#Aggregate Land Development Module
#@filename ALD_111.R
#@authors Brian Gregor, Tara Weidner
#@version 1.11
#@date 12/10/04
#/


#/
# Define pointers to code and data directories
#/

##/
 # Read in the command line arguments to identify the code path, data path and year index
 # The input argument positions depend on whether the operating system is Windows or Linux
 # @return codePath - the path to the ALD code directory
 # @return dataPath - the path to the model scenario data
 # @return baseYearIndex - the index corresponding to the base year of the scenario
 # @return currYearIndex - the index corresponding to the current simulation year
 # @return prevYearIndex - the index corresponding to the previous simulation year
##/
codeVersion <- "111"
baseYearIndex <- 0

codePath <- sub("-", "", commandArgs()[length(commandArgs())-2])
dataPath <- sub("-", "", commandArgs()[length(commandArgs())-1])
currYearIndex <- as.numeric(sub("-", "", commandArgs()[length(commandArgs())]))

if(currYearIndex == baseYearIndex){
    prevYearIndex <- baseYearIndex
} else {
    prevYearIndex <- currYearIndex - 1
}


##/
 # Identify the locations/names where the input files reside and where the outputs will be written
 # @param dataPath - see ALD.R
 # @param baseYearIndex - see ALD.R
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


#/
# Read in data and define variables
#/

##/
 # Source in an R file that identifies locations of data files and declares the model parameters
 # @params ald_inputs.R
 # @params codeVersion
 # @params codePath
 # @return input - a list of data locations for inputs referred to in the script
 # @return cf - a list containing all of the model coefficients
##/
# build an input source file name based on the code version
inputsSourceFile <- paste(codePath, "ald_inputs_", codeVersion, ".R", sep="")
source(inputsSourceFile)

##/
 # Create a list to store the variables
 # @return vars - a list of variables and their attributes
##/
vars <- list()

##/
 # Define zoning category names, codes and descriptions
 # @param input$zoningDefinitions
 # @return vars$zoning - a list with elements $code (zoning codes), $abbr (zoning abbreviations), $description (zoning descriptions)
##/
fieldNames <- c("code", "abbr", "description")
fieldTypes <- rep("character",3)
zoningDesc <- read.table(input$zoningDefinitions, header=T, sep=",", col.names=fieldNames, colClasses=fieldTypes)
vars$zoning <- as.list(zoningDesc)
rm(fieldNames, fieldTypes, zoningDesc)

##/
 # Define floorspace category names and descriptions
 # @param input$floorspaceDefinitions
 # @return vars$floorDef - a list with elements $code (floorspace codes), $abbr (floorspace abbreviations), $description (floorspace descriptions)
##/
fieldNames <- c("code", "abbr", "description", "type")
fieldTypes <- rep("character",4)
floorDefDesc <- read.table(input$floorspaceDefinitions, header=T, sep=",", col.names=fieldNames, colClasses=fieldTypes)
vars$floorDef <- as.list(floorDefDesc)
rm(fieldNames, fieldTypes, floorDefDesc)

##/
 # Alpha and Beta Zone correspondence table
 # @param input$alphaBetaCorrespondence
 # @return vars$alphaBeta - a list with elements $alphaZone, $betaZone
##/
fieldNames <- c("alphaZone", "betaZone", "state", "county", "fips", "puma1pct", "puma5pct", "gridAcres", "luIntensity","MPOcalibrationzones", "MPOmodeledzones")
fieldTypes <- c("character", "character", "character", "character", "character", "character", "character", "numeric", "character", "character", "character")
alphaBeta <- read.table(input$alphaBetaCorrespondence, header=T, sep=",", col.names=fieldNames, colClasses=fieldTypes)
alphaBeta <- alphaBeta[order(as.numeric(alphaBeta$alphaZone)),1:2]
vars$alphaBeta <- as.list(alphaBeta)
rm(fieldNames, fieldTypes, alphaBeta)

##/
 # Total value of residential and non-residential construction for current year
 # @param input$nivqData
 # @return vars$nivq - a list with elements $res (residential construction $), $nres (nonresidential construction $)
##/
fieldNames <- c("ConstructionType", "TotalDollars")
nivqData <- read.table(input$nivqData, header=T, sep=",")
nivq <- (nivqData[,2] / 1000000)
names(nivq) <- c("res", "nres")
vars$nivq <- as.list(nivq)
rm(fieldNames,nivqData, nivq)

##/
 # Price and occupancy inputs from PI
 # @param input$exchangeResults
 # @return vars$price.bf - a list with elements $res and $nres where each is a matrix with floorspace prices by beta zone (row) and floorspace type (col)
 # @return vars$price.af - a list as above but by alpha zone
 # @return vars$occupied.bf - a list with elements $res and $nres where each is a matrix of millions of square feet of occupied space by beta zone (row) and floorspace type (col)
##/
# Read in data, correct spelling and put in proper order
fieldNames <- c("floorType", "betaZone", "boughtFrom", "soldTo", "surplus", "imports", "exports",
                "price")
fieldTypes <- c("character", "character", "numeric", "numeric", "numeric", "numeric", "numeric",
                "numeric")
piInputs <- read.table(input$exchangeResults, header=T, sep=",", col.names=fieldNames, colClasses=fieldTypes)
piInputs$floorType[piInputs$floorType == "FLR Accomodation"] <- "FLR Accommodation"
piInputs <- piInputs[piInputs$floorType %in% vars$floorDef$abbr, c("floorType", "betaZone", "imports", "price")]
piInputs <- piInputs[order(piInputs$floorType, as.numeric(piInputs$betaZone)),]
# Define variables to help in building matrices to store imports (occupied space) and prices
zoneNames <- unique(vars$alphaBeta$betaZone)
zoneNames <- zoneNames[order(as.numeric(zoneNames))]
numZones <- length(zoneNames)
typeNames <- vars$floorDef$abbr
numTypes <- length(typeNames)
# Build matrices of prices and occupied space
## Create empty price matrix
price.bf <- matrix(0, nrow=numZones, ncol=numTypes)
rownames(price.bf) <- zoneNames
colnames(price.bf) <- typeNames
## Create empty occupied space matrix
occupied.bf <- matrix(0, nrow=numZones, ncol=numTypes) # rows are beta zones, columns are floorspace types
rownames(occupied.bf) <- zoneNames
colnames(occupied.bf) <- typeNames
for(type in typeNames){
    typeInputs <- piInputs[piInputs$floorType==type,]
    price.bf[typeInputs$betaZone,type] <- typeInputs$price
    occupied.bf[typeInputs$betaZone,type] <- typeInputs$imports
    rm(type,typeInputs)
    }
# Add price and occupancy data to vars
## Create separate matrices for residential and nonresidential prices
vars$price.bf$res <- price.bf[,colnames(price.bf) %in% vars$floorDef$abbr[vars$floorDef$type=="res"]]
vars$price.bf$nres <- price.bf[,colnames(price.bf) %in% vars$floorDef$abbr[vars$floorDef$type=="nres"]]
## Disaggregate prices to alpha zone level
vars$price.af$res <- vars$price.bf$res[vars$alphaBeta$betaZone,]
rownames(vars$price.af$res) <- vars$alphaBeta$alphaZone
vars$price.af$nres <- vars$price.bf$nres[vars$alphaBeta$betaZone,]
rownames(vars$price.af$nres) <- vars$alphaBeta$alphaZone
## Create separate matrices for residential and nonresidential floorspace occupancy
vars$occupied.bf$res <- occupied.bf[,colnames(occupied.bf) %in% vars$floorDef$abbr[vars$floorDef$type=="res"]]
vars$occupied.bf$nres <- occupied.bf[,colnames(occupied.bf) %in% vars$floorDef$abbr[vars$floorDef$type=="nres"]]
# clean up
rm(fieldNames, fieldTypes, piInputs, zoneNames, numZones, typeNames, numTypes, price.bf, occupied.bf)

##/
 # Quantities of floorspace by alpha zone and floorspace category
 # @param input$floorQuantities - base year building square footage (thousands) by alpha zone (row) and floorspace type (col)
 # @return vars$quant.af$res - a matrix with building square feet by alpha zone (row) and floorspace type (col) for residential floorspace
 # @return vars$quant.af$nres - a data frame with building square feet by alpha zone (row) and floorspace type (col) for nonresidential floorspace
 # @return vars$quant.af$agfor - a data frame with land square feet by alpha zone (row) and space type (col) for agricultural and forest lands
##/
# Read in data, correct spelling and put in proper order
quant.af <- as.matrix(read.table(input$floorQuantities, header=TRUE, row.names=1, sep=","))
colnames(quant.af) <- vars$floorDef$abbr
# Keep only the rows that are part of the defined zones.
quant.af <- quant.af[rownames(quant.af) %in% vars$alphaBeta$alphaZone,]
## Split into residential, nonresidential and agricultural/forest and add to vars
vars$quant.af$res <- quant.af[,colnames(quant.af) %in% vars$floorDef$abbr[vars$floorDef$type=="res"]]
vars$quant.af$nres <- quant.af[,colnames(quant.af) %in% vars$floorDef$abbr[vars$floorDef$type=="nres"]]
vars$quant.af$agfor <- quant.af[,colnames(quant.af) %in% vars$floorDef$abbr[vars$floorDef$type=="agfor"]]
# clean up
rm(quant.af)

##/
 # Average construction cost per building square foot by floorspace type
 # @param input$constructionCosts
 # @return constructCosts - a data frame of average consruction costs ($/sq.ft.) by floorspace type
##/
fieldNames <- c("floorType", "cost")
fieldTypes <- c("character", "numeric")
constructCosts <- read.table(input$constructionCosts, header=T, sep=",", col.names=fieldNames, colClasses=fieldTypes)
vars$costs$res <- constructCosts[constructCosts$floorType %in% vars$floorDef$abbr[vars$floorDef$type=="res"],]
vars$costs$nres <- constructCosts[constructCosts$floorType %in% vars$floorDef$abbr[vars$floorDef$type=="nres"],]
rm(fieldNames, fieldTypes, constructCosts)

##/
 # Quantity of land in each zoning category by alpha zone
 # @param input$zoningQuantities
 # @return vars$zoning.az - a matrix of land square feet (in millions) by alpha zone and zoning category
##/
zoningQuant <- read.table(input$zoningQuantities, header=T, row.names=1, sep=",")
zoningQuant[is.na(zoningQuant)] <- 0 # replace missing values with zeros
zoningQuant <- zoningQuant[,vars$zoning$abbr] # just keep those that have valid names
zoningQuant <- as.matrix(zoningQuant)
numZones <- length(vars$alphaBeta$alphaZone)
numCategories <- length(vars$zoning$abbr)
vars$zoning.az <- matrix(0, nrow=numZones, ncol=numCategories)
rownames(vars$zoning.az) <- vars$alphaBeta$alphaZone
colnames(vars$zoning.az) <- vars$zoning$abbr
vars$zoning.az[rownames(zoningQuant),] <- zoningQuant
vars$zoning.az <- vars$zoning.az / 1000000 # convert to millions of square feet
rm(zoningQuant, numZones, numCategories)

##/
 # Degree of compatibility of each floorspace type with each zoning category
 # @params input$zoningCompatibility
 # @return vars$compat.fz - a matrix of compatibility values by floorspace type (row) and zoning category (col)
##/
fieldTypes <- rep("character",34)
zoneCompIn <- as.matrix(read.table(input$zoningCompatibility, header=T, row.names=1, sep=",", colClasses=fieldTypes))
compatibilityKey <- c(NP=1e-50, VL=0.01, L=0.2, M=0.5, H=0.8, VH=1)
vars$compat.fz <- matrix(as.vector(compatibilityKey[match(zoneCompIn, names(compatibilityKey))]), nrow=dim(zoneCompIn)[1])
rownames(vars$compat.fz) <- rownames(zoneCompIn)
colnames(vars$compat.fz) <- colnames(zoneCompIn)
rm(fieldTypes, zoneCompIn, compatibilityKey)

##/
 # Floor to area ratios by zoning type and floorspace type
 # @params input$floorAreaRatios
 # @return vars$far.fz - a matrix of floor to area ratios by floorspace type (rows) and zoning category (cols)
##/
vars$far.fz <- as.matrix(read.table(input$floorAreaRatios, header=T, row.names=1, sep=","))

##/
 # Square footage of land area occupied by different floorspace types
 # @params input$landSqft
 # @return vars$landSqft.f - a vector of square footage of land area occupied by floorspace type
##/
landSqft.f <- read.table(input$landSqft, header=T, row.names=1, sep=",")
vars$landSqft.f <- unlist(landSqft.f) # convert to vector and assign to vars
names(vars$landSqft.f) <- rownames(landSqft.f) # name the vector elements
vars$landSqft.f <- vars$landSqft.f[vars$floorDef$abbr[1:17]] # put them in the proper order
rm(landSqft.f)

##/
 # aglog PI factors by floorspace type (ag/log only) by bzone
 # This section is currently commented out. It may be used in the future if ALD is revised to keep track of the conversion of agricultural and forest lands to development
 # @param input$AgLogPIFactors
 # @return vars$AgLogPIfactors.bf - a matrix of factors by bzone (rows) and floorspace type (cols)
 # or - a data frame of factors by bzone (rows) and floorspace type (cols)
##/
# fieldNames <- c("betaZone", "floorType", "Factor")
# fieldTypes <- c("character", "character","numeric")
# PIFactorsIN <- read.table(input$AgLogPIFactors, header=T, sep=",", col.names=fieldNames, colClasses=fieldTypes)
# PIFactorsIN <- PIFactorsIN[order(PIFactorsIN$floorType, as.numeric(PIFactorsIN$betaZone)),] # put in proper order by floor type and beta zone to put in matrix in column major order
# numZones <- length(unique(vars$alphaBeta$betaZone))
# typeNames <- c("FLR Agriculture","FLR Logging")
# numTypes <- length(typeNames)
# # Note: input data is only for zones having ag or log values but output matrix needs to have values for all betaZones
# PIFactors.bf <- matrix(0, nrow=numZones, ncol=numTypes)
# rownames(PIFactors.bf) <- unique(vars$alphaBeta$betaZone)
# colnames(PIFactors.bf) <- typeNames
# for(type in typeNames){
#     typePIFactors <- PIFactorsIN[PIFactorsIN$floorType==type,]
#     PIFactors.bf[typePIFactors$betaZone,type] <- typePIFactors$Factor
#     rm(type, typePIFactors)
#     }
# vars$PIFactors.bf <- PIFactors.bf[,colnames(PIFactors.bf) %in% vars$floorDef$abbr[vars$floorDef$type=="agfor"]]
# rm(fieldNames, fieldTypes, PIFactors, numZones, numTypes, typeNames)


#/
# Define global functions to be used in the script
#/

##/
 # Create a list to store the variables
 # @return vars - a list of variables and their attributes
##/
funs <- list()

##/
 # %row*%
 # This function sweeps a matrix with a vector, multiplying each row of the matrix with the vector elementwise
 # @param x - a numeric matrix
 # @param y - a numeric vector
 # @return a numeric matrix
##/
funs[["%row*%"]] <- function(x, y){
    if(!is.matrix(x)) stop("First operand must be matrix")
    if(!is.vector(y)) stop("Second operand must be vector")
    if(length(y) != dim(x)[2]) stop("Length of vector != number of columns")
    if((mode(x) != "numeric") | (mode(y) != "numeric")) stop("Operands must be numeric")
    sweep(x, 2, y, "*")
    }

##/
 # %col+%
 # This function sweeps a matrix with a vector, adding each column of the matrix with the vector elementwise
 # @param x - a numeric matrix
 # @param y - a numeric vector
 # @return a numeric matrix
##/
funs[["%col+%"]] <- function(x, y){
    if(!is.matrix(x)) stop("First operand must be matrix")
    if(!is.vector(y)) stop("Second operand must be vector")
    if(length(y) != dim(x)[1]) stop("Length of vector != number of rows")
    if((mode(x) != "numeric") | (mode(y) != "numeric")) stop("Operands must be numeric")
    sweep(x, 1, y, "+")
    }

##/
 # %row/%
 # This function sweeps a matrix with a vector, dividing each row of the matrix with the vector elementwise
 # @param x - a numeric matrix
 # @param y - a numeric vector
 # @return a numeric matrix
##/
funs[["%row/%"]] <- function(x, y){
    if(!is.matrix(x)) stop("First operand must be matrix")
    if(!is.vector(y)) stop("Second operand must be vector")
    if(length(y) != dim(x)[2]) stop("Length of vector != number of columns")
    if((mode(x) != "numeric") | (mode(y) != "numeric")) stop("Operands must be numeric")
    sweep(x, 2, y, "/")
    }

##/
 # aggregateAlpha
 # This function sums the alpha quantities by beta zone
 # @param mat.ax - a matrix where the rows are alpha zones and the columns may represent any numerical quantity
 # @param vars$alphaBeta - a list of alpha zones and corresponding beta zones
 # @return mat.bx - a generic name for a matrix where the rows are beta zones and the columns represent any numerical quantity
##/
funs$aggregateAlpha <- function(mat.ax, alphaBeta=vars$alphaBeta){
    df.bx <- aggregate(mat.ax, list(alphaBeta$betaZone), sum)
    mat.bx <- as.matrix(df.bx[,-1])
    rownames(mat.bx) <- as.character(df.bx[,1])
    mat.bx[order(as.numeric(rownames(mat.bx))),]
    }

##/
 # allocateFloorProd
 # This function allocates modelwide floorspace production in dollars to floorspace types
 # @param nivq - modelwide floorspace production
 # @param occupied.bf - floorspace price by beta zone (row) and floorspace type (column)
 # @param prevq.af - floorspace quantity by alpha zone (row) and floorspace type (column)
 # @param Lq - Lamda coefficient for logit function
 # @param Bq1 - Coefficient for utility function
 # @param Bq2 - Coefficient for utility function
 # @param asc.f - A vector of alternative specific constants for utility function by floorspace type
 # @return nivq.f - A vector of floorspace production in dollars to floorspace types
##/
funs$allocateFloorProd <- function(nivq, occupied.bf, prevq.af, cost.f, Lq, Bq1.f, Bq2, asc.f){
    # Make the scalar Bq2 coefficient into a vector the length of the number of floorspace types
    Bq2.f <- rep(Bq2, length(asc.f))
    # Calculate average vacancy rates by floorspace type
    totalSpace.f <- colSums(prevq.af)
    occupied.f <- colSums(occupied.bf)
    vacancy.f <- (totalSpace.f - occupied.f) / totalSpace.f
    # Calculate the value of the total floorspace
    tvq.f <- totalSpace.f * cost.f
	# Calculate the utility functions for floorspace categories
	util.f <- Bq1.f * vacancy.f + Bq2.f * log(tvq.f/sum(tvq.f)) + asc.f
    # Create a list to hold the results
    NIVQ <- list()
	# Allocate floorspace $ quantities among floorspace types
	NIVQ$nivq.f <- nivq * (exp(Lq * util.f)/sum(exp(Lq * util.f)))
    # Add the vacancy calcualtion for diagnostics
    NIVQ$vacancy.f <- vacancy.f
    # Add the utility calculation for diagnostics
    NIVQ$util.f <- util.f
	# The result if a vector of floorspace quantity values by floorspace type
	NIVQ
}

##/
 # calcFloorCapacity
 # This function calculates the capacity for floorspace of each type by alpha zone
 # @params compat.fz (an element of vars)
 # @params zoning.az (an element of vars)
 # @params far.fz (an element of vars)
##/
funs$calcFloorCapacity <- function(Lp, Stc, landSqft.f, compat.fz, zoning.az, far.fz, floorDef){
    # Calculate the proportion of the land area of each zoning category allocated to each floorspace type
    size.f <- log(landSqft.f) / Lp
    compatUtil <- log(compat.fz) %col+% (Stc * size.f)
    expCompatUtil <- exp(compatUtil)
    landProp.fz <- expCompatUtil %row/% colSums(expCompatUtil)
    # Make sure that were a zone allows no floorspace, that none will be allocated
    noneAllowed <- colSums(compat.fz) < 1e-48  # Find out zone types where compatibility is NP for all floorspace types
    landProp.fz[,noneAllowed] <- 0
    # Define a function for allocating land area to floorspace types for an individual zone
    allocateLandArea <- function(zoneArea.z, landProp.fz){
        rowSums(landProp.fz %row*% zoneArea.z)
        }
    # Apply the function to calculate land area by floorspace type and alpha zone
    # This is done for diagnostic purposes only. The results are not used in further calculations
    landArea.af <- t(apply(zoning.az, 1, allocateLandArea, landProp.fz))
    # Define a function for allocating floorspace capacity for an individual zone
    allocateCapacity <- function(zoneArea.z, landProp.fz, far.fz){
        landCap.fz <- landProp.fz %row*% zoneArea.z
        floorCap.f <- rowSums(landCap.fz * far.fz)
        floorCap.f
        }
    # Apply the function to zoning.az
    cap.af <- t(apply(zoning.az, 1, allocateCapacity, landProp.fz, far.fz))
    floorCap <- list()
    floorCap$res <- cap.af[,floorDef$abbr[floorDef$type=="res"]]
    floorCap$nres <- cap.af[,floorDef$abbr[floorDef$type=="nres"]]
    # Add the diagnostics
    floorCap$floorspaceProp <- landProp.fz
    floorCap$landAllocations <- landArea.af
    # Return the results
    floorCap
    }

##/
 # calculateFloorDecrease
 # This function calculates a decrease in floorspace quantities by floorspact type an alpha zone
 # @param quant.af - an element of vars that contains the quantity of floorspace by type and alpha zone
 # @param Df - a coefficient which specifies the proportion of floorspace that is eliminated
 # @param price.af - matrix of floorspace prices by alpha zone and floorspace type
 # @param B3f - coefficient in utility
 # @param B4f - coefficient in utility
 # @return DQ.af - list that includes matrix of decrease in floorspace by alpha zone and floorspace type and diagnostic information
##/
funs$calculateFloorDecrease <- function(quant.af, Df, price.af, B3f, B4f){
    prevProp.af <- quant.af %row/% colSums(quant.af) # calculate the zonal proportions of floorspace
    DQ.f <- colSums(quant.af) * Df # calculate decrease in floorspace by floorspace type
    A.af <- prevProp.af^B3f * price.af^B4f # calculate the "utilities" for decreasing floorspace by zone and floorspace type
    DQ.af <- (A.af %row/% colSums(A.af)) %row*% DQ.f # allocate floorspace decrease by alpha zone
    DQ.af[DQ.af > quant.af] <- quant.af[DQ.af > quant.af] # the decrease cannot be any greater than the quantity in the zone
    # Make list to hold the results
    DQ <- list()
    DQ$A.af <- A.af
    DQ$DQ.af <- DQ.af
    DQ
    }
    
##/
 # allocateFloorIncrease
 # This function calculates the increase in floorspace quantities by floorspace type and alpha zone
 # @param cap.af - matrix of floorspace capacities by alpha zone and floorspace type
 # @param niq.f - vector of floorspace increases (in square feet) by floorspace type
 # @param quant.af - matrix of floorspace quantities by floorspace type and alpha zone
 # @param dq.af - a matrix of floorspace decreases by floorspace type and alpha zone
 # @param price.af - a matrix of floorspace prices by floorspace type and alpha zone
 # @param B5f - coefficient in utility
 # @param B6f - coefficient in utility
 # @param B7f - coefficient in utility
 # @param B8f - coefficient in utility
 # @param B9f - coefficient in utility
 # @return IQ.af - list that includes matrix of increase in floorspace by alpha zone and floorspace type and diagnostic information
##/
funs$allocateIncrease <- function(cap.af, niq.f, quant.af, dq.af, price.af, B5f, B6f, B7f, B8f, B9f){
    prevProp.af <- quant.af %row/% colSums(quant.af) # calculate the zonal proportions of floorspace
    prevDQ.af <- quant.af - dq.af
    # Check if the capacity is less than the quantity and set to the quantity is so
    isLowCapacity <- quant.af > cap.af
    cap.af[isLowCapacity] <- quant.af[isLowCapacity]
	propCap.af <- prevDQ.af / cap.af # ratio of floorspace to zonal capacity of floorspace
	propCap.af[is.nan(propCap.af) | is.infinite(propCap.af)] <- 0
   	B.af <- (prevProp.af + B9f)^B5f * price.af^B6f * (1 - B7f * propCap.af^B8f)
    B.af[B.af < 0] <- 0  # make sure that B.af is not negative
    IQ.af <- (B.af %row/% colSums(B.af)) %row*% (niq.f + colSums(dq.af))
    currQ.af <- prevDQ.af + IQ.af
    currQ.af[currQ.af < 0] <- 0
    # Make a list to hold results to be returned from function
    Q <- list()
    Q$prevDQ.af <- prevDQ.af
    Q$lowCap.af <- isLowCapacity
    Q$propCap.af <- propCap.af
    Q$B.af <- B.af
    Q$IQ.af <- IQ.af
	Q$currQ.af <- currQ.af
	Q
	}


#/
# Calculate the model results
#/

##/
 # Attach lists of variables (vars), coefficients (cf) and functions (funs) to the workspace to simplify referencing of variables contained in that list
##/
attach(vars)
attach(cf)
attach(funs)

##/
 # Make some lists to store results of computations.
 # @params none
 # @result nivq.f - an empty list. This will have two elements, $res and $nres. Each will be a vector of the value of new construction by floorspace type.
 # @result niq.f - an empty list. This will have two elements, $res and $nres. Each will be a vector of the square footage of new construction by floorspace type.
 # @result cap.af - an empty list. This will have two elements, $res and $nres. Each will be a matrix of the capacity, in square feet, of each alpha zone for each floorspace type.
 # @result dq.af - an empty list. This will have two elements, $res and $nres. Each will be a matrix of the floorspace decreases by floorspace typs and alpha zone
 # @result currQ.af - an empty list. This will have three element, $res, $nres, $agfor. Each will be a matrix of the total floorspace by alpha zone and floorspace type. In the case of $agfor it is the land area.
##/
nivq.f <- list()
niq.f <- list()
cap.af <- list()
dq.af <- list()
currQ.af <- list()

##/
 # Allocate residential floorspace dollar value and square footage among floorspace categories
 # @param  nivq$res (an element of vars)
 # @param  occupied.bf$res (an element of vars)
 # @param  quant.af$res (an element of vars)
 # @param  rLq (an element of cf)
 # @param  rBq1 (an element of cf)
 # @param  rBq2 (an element of cf)
 # @param  asc$res (an element of cf)
 # @param  costs$res$cost (an element of vars)
 # @return residentialFloorspace - a list containing the floorspace computations and other diagnostic outputs
 # @return nivq.f$res - a vector of dollar values of new floorspace by floorspace type for the model area
 # @return niq.f$res - a vector of millions of square feet of new floorspace by floorspace type for the model area
 ##/
residentialFloorspace <- allocateFloorProd(nivq$res, occupied.bf$res, quant.af$res, costs$res$cost, rLq, rBq1, rBq2, asc$res)
nivq.f$res <- residentialFloorspace$nivq.f
niq.f$res <- nivq.f$res / costs$res$cost

##/
 # Allocate nonresidential floorspace dollar value and square footage among floorspace categories
 # @param  nivq$nres (an element of vars)
 # @param  occupied.bf$nres (an element of vars)
 # @param  quant.af$nres (an element of vars)
 # @param  nLq (an element of cf)
 # @param  nBq1 (an element of cf)
 # @param  nBq2 (an element of cf)
 # @param  asc$nres (an element of cf)
 # @param  costs$nres$cost (an element of vars)
 # @param  nonresidentialFloorspace - a list containing the floorspace computations and other diagnostic outputs
 # @return nivq.f$nres - a vector of dollar values of new floorspace by floorspace type for the model area
 # @return niq.f$nres - a vector of millions of square feet of new floorspace by floorspace type for the model area
 ##/
nonresidentialFloorspace <- allocateFloorProd(nivq$nres, occupied.bf$nres, quant.af$nres, costs$nres$cost, nLq, nBq1, nBq2, asc$nres)
nivq.f$nres <- nonresidentialFloorspace$nivq.f
niq.f$nres <- nivq.f$nres / costs$nres$cost

##/
 # Calculate floorspace capacities by alpha zone and floorspace type
 # @param  zoning.az (an element of vars)
 # @param  compat.fz (an element of vars)
 # @param  far.fz (an element of vars)
 # @return floorspaceCapacities - a list containing floorspace capacities and other diagnostic outputs
 # @return cap.af$res - a matrix of residential floorspace capacities by floorspace type and alpha zone
 # @return cap.af$nres - a matrix of nonresidential floorspace capacities by floorspace type and alpha zone
##/
floorspaceCapacities <- calcFloorCapacity(Lp, Stc, landSqft.f, compat.fz, zoning.az, far.fz, floorDef)
cap.af$res <- floorspaceCapacities$res
cap.af$nres <- floorspaceCapacities$nres

##/
 # Calculate residential floorspace decreases
 # This function calculates a decrease in floorspace quantities by floorspace type and alpha zone
 # @param quant.af$res (an element of vars)
 # @param rDf - (an element of cf)
 # @param price.af$res (an element of vars)
 # @param rB3f (an element of cf)
 # @param rB4f (an element of cf)
 # @return dq.af$res - a matrix of residential floorspace decrements by floorspace type and alpha zone
##/
resDQ <- calculateFloorDecrease(quant.af$res, rDf, price.af$res, rB3f, rB4f)
dq.af$res <- resDQ$DQ.af

##/
 # Calculate nonresidential floorspace decreases
 # This function calculates a decrease in floorspace quantities by floorspace type and alpha zone
 # @param quant.af$nres (an element of vars)
 # @param nDf - (an element of cf)
 # @param price.af$nres (an element of vars)
 # @param nB3f (an element of cf)
 # @param nB4f (an element of cf)
 # @return dq.af$nres - a matrix of nonresidential floorspace decrements by floorspace type and alpha zone
##/
nresDQ <- calculateFloorDecrease(quant.af$nres, nDf, price.af$nres, nB3f, nB4f)
dq.af$nres <- nresDQ$DQ.af

##/
 # Calculate current year residential floorspace quantities
 # @param  cap.af$res - see above
 # @param  niq.f$res - see above
 # @param  quant.af$res (an element of vars)
 # @param  dq.af$res - see above
 # @param  price.af$res (an element of vars)
 # @param  rB5f  (an element of cf)
 # @param  rB6f (an element of cf)
 # @param  rB7f (an element of cf)
 # @param  rB8f (an element of cf)
 # @param  rB9f (an element of cf)
 # @return resQNew - a list containing the new floorspace quantities, the increments and the ratio of quantity to capacity
 # @return currq.af$res - a matrix of the current quantity in residential floorspace (square feet) by alpha zone and floorspace type
 # @return currq.bf$res - a matrix of the current quantity in residential floorspace (square feet) by beta zone and floorspace type
 ##/
resQNew <- allocateIncrease(cap.af$res, niq.f$res, quant.af$res, dq.af$res, price.af$res, rB5f, rB6f, rB7f, rB8f, rB9f)
currQ.af$res <- resQNew$currQ.af

##/ 
 # Calculate current year nonresidential floorspace quantities
 # @param  cap.af$nres - see above
 # @param  niq.f$nres - see above
 # @param  quant.af$nres (an element of vars)
 # @param  dq.af$nres (an element of vars)
 # @param  price.af$nres (an element of vars)
 # @param  nB5f  (an element of cf)
 # @param  nB6f (an element of cf)
 # @param  nB7f (an element of cf)
 # @param  nB8f (an element of cf)
 # @param  nB9f (an element of cf)
 # @return nresQNew - a list containing the new floorspace quantities, the increments and the ratio of quantity to capacity
 # @return currq.af$nres - a matrix of the current quantity in floorspace (square feet) by alpha zone and floorspace type
 # @return currq.bf$nres - a matrix of the current quantity in floorspace (square feet) by alpha zone and floorspace type
 ##/
nresQNew <- allocateIncrease(cap.af$nres, niq.f$nres, quant.af$nres, dq.af$nres, price.af$nres, nB5f, nB6f, nB7f, nB8f, nB9f)
currQ.af$nres <- nresQNew$currQ.af

##/
 # Combine the residential, nonresidential, agricultural and forest quantities into one table for output that ALD will use
 # @params currQ.af$res - see above
 # @params currQ.af$nres - see above
 # @params quant.af$agfor (an element of vars)
 # @return totq.af - a list of three elements: $res, $nres, $agfor. Each is a matrix of the total quantity of floorspace by alpha zone and floorspace type.
##/
totQ.af <- cbind(currQ.af$res, currQ.af$nres, quant.af$agfor)


#/
# Write out the results for ALD and diagnostics
#/

##/
 # Write out current floorspace inventory in form the ALD uses
 # @param totQ.af
 # @param ALDCurrDirectory
 # @return "FloorspaceInventory.csv" - matrix results in the ALDCurrDirectory
##/
# Write in ALD matrix format that ALD and PT can read
totQ.af <- cbind(AZone=as.numeric(rownames(totQ.af)), totQ.af)
write.table(totQ.af, paste(ALDCurrDirectory, "FloorspaceInventory.csv", sep="/"), row.names=FALSE, col.names=TRUE, sep=",")

##/
 # Write out residential and nonresidential floorspace allocation calculations for diagnostic purposes
 # @param residentialFloorspace - residential floorspace value calculations
 # @param nonresidentialFloorspace - nonresidential floorspace value calculations
 # @param niq - residential and nonresidential floorspace results
 # @param "FloorspaceDiagnostics.csv"
##/
floorOut <- list()
floorOut$vacancy <- c(residentialFloorspace$vacancy.f, nonresidentialFloorspace$vacancy.f)
floorOut$utility <- c(residentialFloorspace$util.f, nonresidentialFloorspace$util.f)
floorOut$MDollars <- c(residentialFloorspace$nivq.f, nonresidentialFloorspace$nivq.f)
floorOut$MSqFt <- c(niq.f$res, niq.f$nres)
floorOut <- data.frame(floorOut)
write.table(floorOut, paste(ALDCurrDirectory, "FloorspaceDiagnostics.csv", sep="/"), row.names=TRUE, col.names=TRUE, sep=",")
rm(floorOut)

##/
 # Write out residential and nonresidential floorspace capacity calculations for diagnostic purposes
 # @param floorspaceCapacities
 # @result "ResidentialCapacity.csv"
 # @result "NonresidentialCapacity.csv"
 # @result "FloorspaceProportionsByZoning.csv"
 # @result "LandAllocations.csv"
##/
write.table(floorspaceCapacities$res, paste(ALDCurrDirectory, "ResidentialCapacity.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(floorspaceCapacities$nres, paste(ALDCurrDirectory, "NonresidentialCapacity.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(floorspaceCapacities$floorspaceProp, paste(ALDCurrDirectory, "FloorspaceProportionsByZoning.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(floorspaceCapacities$landAllocations, paste(ALDCurrDirectory, "LandAllocations.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")

##/
 # Write out residential and nonresidential floorspace decreases for diagnostic purposes
 # @param resDQ
 # @param nresDQ
 # @result "ResidentialDecreaseUtilities.csv"
 # @result "ResidentialDecrease.csv"
 # @result "NonresidentialDecreaseUtilities.csv"
 # @result "NonresidentialDecrease.csv"
##/
write.table(resDQ$A.af, paste(ALDCurrDirectory, "ResidentialDecreaseUtilities.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(resDQ$DQ.af, paste(ALDCurrDirectory, "ResidentialDecrease.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(nresDQ$A.af, paste(ALDCurrDirectory, "NonresidentialDecreaseUtilities.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(nresDQ$DQ.af, paste(ALDCurrDirectory, "NonresidentialDecrease.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")

##/
 # Write out the residential and nonresidential floorspace increase diagnostics
 # @param resQNew
 # @param nresQNew
 # @return "ResQuantitiesAfterDecrease.csv"
 # @return "NonresQuantitiesAfterDecrease.csv"
 # @return "ResLowCapacity.csv"
 # @return "NonresLowCapacity.csv"
 # @return "ResCapacityUtilization.csv"
 # @return "NonresCapacityUtilization.csv"
 # @return "ResIncreaseUtilities.csv"
 # @return "NonresCapacityUtilities.csv"
 # @return "ResidentialIncreases.csv"
 # @return "NonresidentialIncreases.csv"
##/
write.table(resQNew$prevDQ.af, paste(ALDCurrDirectory, "ResQuantitiesAfterDecrease.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(nresQNew$prevDQ.af, paste(ALDCurrDirectory, "NonresQuantitiesAfterDecrease.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(resQNew$lowCap.af, paste(ALDCurrDirectory, "ResLowCapacity.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(nresQNew$lowCap.af, paste(ALDCurrDirectory, "NonresLowCapacity.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(resQNew$propCap.af, paste(ALDCurrDirectory, "ResCapacityUtilization.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(nresQNew$propCap.af, paste(ALDCurrDirectory, "NonresCapacityUtilization.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(resQNew$B.af, paste(ALDCurrDirectory, "ResIncreaseUtilities.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(nresQNew$B.af, paste(ALDCurrDirectory, "NonresIncreaseUtilities.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(resQNew$IQ.af, paste(ALDCurrDirectory, "ResidentialIncreases.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")
write.table(nresQNew$IQ.af, paste(ALDCurrDirectory, "NonresidentialIncreases.csv", sep="/"),
            row.names=TRUE, col.names=TRUE, sep=",")


#/
# Write out results for PI and other modules that read matrices in flat format
#/

##/
 # Write out current floorspace inventory in form that PI uses
 # @param currQ.af
 # @quant.af$agfor
 # @param ALDCurrDirectory
 # @return "FloorspaceI.csv" - flat form results in the ALDCurrDirectory
##/

options(scipen=7) # set this option high to avoid scientific notation in the output
totQRes <- cbind(AZone=as.vector(rownames(currQ.af$res)), 
                FLRType=rep("Residential",ncol(currQ.af$res)*nrow(currQ.af$res)), 
                FLRName=rep(colnames(currQ.af$res),each=nrow(currQ.af$res)),
                BldgMSQFT=zapsmall(as.vector(currQ.af$res),9))
totQNRes <- cbind(AZone=as.vector(rownames(currQ.af$nres)), 
                FLRType=rep("NonResidential",ncol(currQ.af$nres)*nrow(currQ.af$nres)), 
                FLRName=rep(colnames(currQ.af$nres),each=nrow(currQ.af$nres)),
                BldgMSQFT=zapsmall(as.vector(currQ.af$nres),9))
totQAgFor <- cbind(AZone=as.vector(rownames(quant.af$agfor)), 
                FLRType=rep("AgForest",ncol(quant.af$agfor)*nrow(quant.af$agfor)), 
                FLRName=rep(colnames(quant.af$agfor),each=nrow(quant.af$agfor)),
                BldgMSQFT=zapsmall(as.vector(quant.af$agfor),9))
totQOut <- rbind(totQRes, totQNRes, totQAgFor)
totQOut <- totQOut[totQOut[,"BldgMSQFT"]!="0",]
write.table(totQOut, paste(ALDCurrDirectory, "FloorspaceI.csv", sep="/"), row.names=FALSE, col.names=TRUE, sep=",")
rm(totQRes, totQNRes, totQAgFor, totQOut)

##/
 # Write out floorspace increments in flat form
 # @param resQNew
 # @param nresQNew
 # @return "Increments.csv"
##/
IQRes <- cbind(AZone=as.vector(rownames(resQNew$IQ.af)), 
               FLRType=rep("Residential",ncol(resQNew$IQ.af)*nrow(resQNew$IQ.af)), 
               FLRName=rep(colnames(resQNew$IQ.af),each=nrow(resQNew$IQ.af)),
               IncMSQFT=zapsmall(as.vector(resQNew$IQ.af),9))
IQNres <- cbind(AZone=as.vector(rownames(nresQNew$IQ.af)), 
               FLRType=rep("NonResidential",ncol(nresQNew$IQ.af)*nrow(nresQNew$IQ.af)), 
               FLRName=rep(colnames(nresQNew$IQ.af),each=nrow(nresQNew$IQ.af)),
               IncMSQFT=zapsmall(as.vector(nresQNew$IQ.af),9))
IQOut <- rbind(IQRes, IQNres)
IQOut <- IQOut[IQOut[,"IncMSQFT"]!="0",]
write.table(IQOut, paste(ALDCurrDirectory, "Increments.csv", sep="/"), row.names=FALSE, col.names=TRUE, sep=",")
rm(IQRes, IQNres, IQOut)

##/
 # Write out floorspace increments in matrix form
 # @param resQNew
 # @param nresQNew
 # @return "Increments_Matrix.csv"
##/
IQOut.af <- cbind(AZone=as.numeric(rownames(totQ.af)), resQNew$IQ.af, nresQNew$IQ.af)
write.table(IQOut.af, paste(ALDCurrDirectory, "Increments_Matrix.csv", sep="/"), row.names=FALSE, col.names=TRUE, sep=",")
rm(IQOut.af)

##/
 # Write out the residential and non-residential capacity utilization in matrix form
 #@param resQNew$propCap.af
 #@param nresQNew$propCap.af
 #@return "ResNonResCapacityUtilization.output"
##RCUOut.af <- cbind(cbind(AZone=as.numeric(rownames(totQ.af)), resQNew$IQ.af, nresQNew$IQ.af)

#/
# Detach cf, funs and vars
#/
detach(cf)
detach(funs)
detach(vars)

setwd(ALDCurrDirectory) # To save the .Rdata file to the ALD directory for the simulation year

