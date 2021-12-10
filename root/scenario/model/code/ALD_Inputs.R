#==============================================================
#Script to Load Inputs for the Aggregate Land Development Model
#==============================================================

#:File Name: ALD_Inputs.R
#:Authors: Brian Gregor
#:Contact: brian.j.gregor@odot.state.or.us
#:Date: 8/11/10
#:Version: 1.30
#:Copyright: 2007, 2008, 2010 Oregon Department of Transportation
#:License: GPL2



#PURPOSE AND DESCRIPTION
#=======================

#This is the program code for the portion of the Aggregate Land Development (ALD) Module of the Oregon2 Transitional Model that loads the input files to run ALD.  Documentation in this script addresses the functions defined within it. For overall documentation of ALD, please refer to the ALD.R script.


#HISTORY
#=======

#10/22/07 PI ExchangeResults.csv and ActivityLocations.csv now includes records for model external zones (import and export zones). The code was changed to filter these out since they are not used by ALD. Lines 261-263 & 309-312.
#4/07/08 Renamed the following input file references:
#"zoning_definitions_v4.csv" renamed to "zoning_definitions.csv"
#"zoning_compatibility_v4a.csv" renamed to "zoning_compatibility.csv"
#"far_v4a.csv" renamed to "far.csv"
#4/29/08 removed section of the script which identifies the input files. The input files are now identified in the global properties files. The main ald script reads these in and parses into the Input_ list.
#6/24/09 changed code to read ED inputs to use header names
#8/11/10 AB - small change for zoning_compatibility:  NP=0, previously NP=1e-50
#5/25/11 CF - added small change to reading in ed/ned data so that column names (just ordering) do not need to be maintained externally
#6/06/11 CF - added AA split activities to excluded list
#6/27/11 KC/AB - changed VL from 0.01 to 0.1, based on the orginal intention from the documentation
#7/26/11 AB - Added a section to read in Avgerage Floorspace price from ExchangeResultsTotals.csv.  The change in average price is used to adjust the size term for the floor space capacity function.
#9/15/11 CF - Divide ExchangeResults floorspace values by 1000000 when using AA
#4/17/14 AB - Update for PECAS code, for PECAS version of SWIM, FloorspaceInventor and Increment inputs and outputs are in SQFT, not MSQFT
#7/28/15 AB - Removed PI functionality
#12/9/21 AB - removed refernce to PUMA 1990

#LOAD THE MODEL INPUT DATA
#=========================

#Input files are read and required data is extracted, operated on, organized and stored so that it is in the proper form to be used by the various ALD models. The data is collected into a list named "Vars_"

#Create lists to store the input data
#------------------------------------

#::

    # Input data are stored in a list named Vars_
    Vars_ <- list()
    

#Define zoning category names, codes and descriptions
#----------------------------------------------------

#::

    # Read the data file
    ZoningDesc.. <- read.csv(Input_$ZoningDefinitions, as.is=TRUE)
    ZoningDesc.. <- ZoningDesc..[,c("code", "name", "description")]
    names(ZoningDesc..) <- c("Code", "Abbr", "Description")
    ZoningDesc.. <- setDataframeClasses(ZoningDesc.., rep("character", ncol(ZoningDesc..)))
    # Define a zoning category naming vector for subsequent use for dimension naming
    Zc <- ZoningDesc..$Abbr
    # Save the results as a list
    Vars_$Zoning_ <- as.list(ZoningDesc..)
    # Name the elements of each vector in the list with Zc
    for(i in 1:length(Vars_$Zoning_)){
        names(Vars_$Zoning_[[i]]) <- Zc
        }
    # Change the name of each component of the list to add a Zc suffix
    names(Vars_$Zoning_) <- paste(names(Vars_$Zoning_), ".Zc", sep="")
    # Clean up workspace
    rm(ZoningDesc..)


#Define floorspace category names and descriptions
#-------------------------------------------------

#::
    # Read the data file
    FloorDefDesc.. <- read.csv(Input_$FloorspaceDefinitions, as.is=TRUE)
    FloorDefDesc.. <- FloorDefDesc..[,c("code", "name", "description", "type")]
    names(FloorDefDesc..) <- c("Code", "Abbr", "Description", "Type")
    FloorDefDesc.. <- setDataframeClasses(FloorDefDesc.., rep("character", ncol(FloorDefDesc..)))
    # Define a floorspace type naming vector for subsequent use for dimension naming
    Ft <- FloorDefDesc..$Abbr
    # Define a naming vector of the different floorspace categories
    Fc <- c("res", "nres", "agfor")
    # Define naming vectors for the different types of floorspace
    Fr <- FloorDefDesc..$Abbr[FloorDefDesc..$Type == "res"]  # Residential
    Fn <- FloorDefDesc..$Abbr[FloorDefDesc..$Type == "nres"]  # Nonresidential
    Fa <- FloorDefDesc..$Abbr[FloorDefDesc..$Type == "agfor"]  # Agriculture and forestry
    Fd <- c(Fr, Fn)
    # Save the results as a list
    Vars_$FloorDef_ <- as.list(FloorDefDesc..)
    # Name the elements of each vector in the list with Ft
    for(i in 1:length(Vars_$FloorDef_)){
        names(Vars_$FloorDef_[[i]]) <- Ft
        }
    # Change the name of each component of the list to add a Ft suffix
    names(Vars_$FloorDef_) <- paste(names(Vars_$FloorDef_), ".Ft", sep="")
    # Clean up workspace
    rm(FloorDefDesc..)


#Define Alpha Zones, Beta Zones, and Regions
#-------------------------------------------

# A region field is added. It is assumed at this time that this field will be in the third column of the table. If this is not the final location, then the following block of code will need to be changed.

#::

    # Read the data file
    AlphaBeta.. <- read.csv(Input_$AlphaBetaCorrespondence, as.is=TRUE)
    AlphaBeta.. <- AlphaBeta..[,c("Azone", "Bzone", "ALDRegion", "State", "COUNTY", "STATEFIPS",
        "COUNTYFIPS", "PUMA2000")]
    names(AlphaBeta..) <- c("AlphaZone", "BetaZone", "Region", "State", "County", "StateFips",
        "CountyFips", "Puma2000")
    AlphaBeta.. <- setDataframeClasses(AlphaBeta.., rep("character", ncol(AlphaBeta..)))
    # Order by alpha zone and select columns to keep
    AlphaBeta.. <- AlphaBeta..[order(as.numeric(AlphaBeta..$AlphaZone)),
        c("AlphaZone", "BetaZone", "Region", "State", "County", "StateFips", "CountyFips", #"Puma1990", 
            "Puma2000")]
    # Make a new unique FIPS code for counties that combines the state and county codes
    AlphaBeta..$Fips <- paste(
        sapply(AlphaBeta..$StateFips, function(x){
            paste(paste(rep("0", 2 - nchar(x)), collapse=""), x, sep="")
            }),
        sapply(AlphaBeta..$CountyFips, function(x){ 
            paste(paste(rep("0", 3 - nchar(x)), collapse=""), x, sep="")
            }),
        sep="")
    # Define an Alpha Zone naming vector to use for subsequent dimension naming
    Az <- AlphaBeta..$AlphaZone
    # Define Beta Zone naming vector
    Bz <- unique(AlphaBeta..$BetaZone)
    Bz <- Bz[order(as.numeric(Bz))]
    # Define Region naming vector
    Rg <- sort(unique(AlphaBeta..$Region))
    # Define State naming vector
    St <- sort(unique(AlphaBeta..$State))
    # Define County naming vector
    Cn <- sort(unique(AlphaBeta..$County))
    # Define Fips (county) naming vector
    Cc <- unique(AlphaBeta..$Fips)
    Cc <- Cc[order(as.numeric(Cc))]
    # Define Puma1990 naming vector
    #Pm1 <- unique(AlphaBeta..$Puma1990)
    #Pm1 <- Pm1[order(as.numeric(Pm1))]
    # Define Puma1990 naming vector
    Pm2 <- unique(AlphaBeta..$Puma2000)
    Pm2 <- Pm2[order(as.numeric(Pm2))]
    # Save the results as a list
    Vars_$AlphaBeta_ <- as.list(AlphaBeta..)
    # Name the elements of each vector in the list with Az
    for(i in 1:length(Vars_$AlphaBeta_)){
        names(Vars_$AlphaBeta_[[i]]) <- Az
        }
    # Change the name of each component of the list to add a Az suffix
    names(Vars_$AlphaBeta_) <- paste(names(Vars_$AlphaBeta_), ".Az", sep="")
    # Clean up the work space
    rm(AlphaBeta..)


#Load statewide residential and nonresidential construction dollar amounts for current year 
#------------------------------------------------------------------------------------------

#::

    # Read in the data
    ConsValData.. <- read.csv(Input_$NivqData, as.is=TRUE)
    # Extract the construction dollar amounts into a vector
    #specify column names for ed/ned robustness - CF 5/26/2011
    names(ConsValData..) <- c("ConstructionType","TotalDollars")
    # Add a 0 value for agriculture and forestry (no construction amounts for these)
    ConsVal.Fc <- c((ConsValData..$TotalDollars / 1000000), 0)
    names(ConsVal.Fc) <- Fc
    # Save the results as a list
    Vars_$ConsVal.Fc <- ConsVal.Fc
    # Clean up the work space
    rm(ConsValData.., ConsVal.Fc)


#Load price and occupancy inputs from PI results for the previous period
#-----------------------------------------------------------------------

#::
 
    # Read the data file
    PiInputs.. <- read.csv(Input_$ExchangeResults, as.is=TRUE)
    # Select only the rows and columns needed
    PiInputs.. <- PiInputs..[PiInputs..$Commodity %in% Ft, c("Commodity", "ZoneNumber", 
        "Imports", "Price")]
    # Convert types appropriately
    PiInputs.. <- setDataframeClasses(PiInputs.., c("character", "character", "numeric", "numeric"))
    # Keep data only for internal zones
    IsInternal. <- as.numeric(PiInputs..$ZoneNumber) <= max(as.numeric(Bz))
    PiInputs.. <- PiInputs..[IsInternal., ] 
    # Put in order, first by floorspace type and then Beta zone
    PiInputs.. <- PiInputs..[order(PiInputs..$Commodity, as.numeric(PiInputs..$ZoneNumber)),]
    # Build matrices of prices and occupied space
    Price.BzFt <- array(0, dim=c(length(Bz), length(Ft)), dimnames=list(Bz, Ft))
    Occupied.BzFt <- array(0, dim=c(length(Bz), length(Ft)), dimnames=list(Bz, Ft))
    for(ft in Ft){
    	 TypeInputs.. <- PiInputs..[PiInputs..$Commodity == ft,]
    	 Price.BzFt[TypeInputs..$ZoneNumber,ft] <- TypeInputs..$Price
    	 # Removed if statement for Using AA, now the following lines assumes AA - AB 7-28-15
       #if (Input_$UsingAA == "true") {
            Occupied.BzFt[TypeInputs..$ZoneNumber,ft] <- TypeInputs..$Imports / 1000000
        # } else {
        #    Occupied.BzFt[TypeInputs..$ZoneNumber,ft] <- TypeInputs..$Imports
        # }
         rm(ft,TypeInputs..)
         }
    # Make a matrix of prices by alpha zone
    # Each alpha zone has the value of the beta zone it is within
    Price.AzFt <- Price.BzFt[Vars_$AlphaBeta_$BetaZone,]
    rownames(Price.AzFt) <- Az
    # Save price and occupancy data in a list
    Vars_$Price.BzFr <- Price.BzFt[,Fr]
    Vars_$Price.BzFn <- Price.BzFt[,Fn]
    Vars_$Price.AzFr <- Price.AzFt[,Fr]
    Vars_$Price.AzFn <- Price.AzFt[,Fn]
    Vars_$Occupied.BzFr <- Occupied.BzFt[,Fr]
    Vars_$Occupied.BzFn <- Occupied.BzFt[,Fn]
    # Clean up the work space
    rm(PiInputs.., Price.BzFt, Occupied.BzFt, Price.AzFt, IsInternal.)


#Load PI quantities and change in quantities
#------------------------------------------

#::

    # Read the data file for the current PI activity (actually activity for previous period)
    PiCurrentActivity.. <- read.csv(Input_$CurrentActivity, as.is=TRUE)
    PiCurrentActivity.. <- PiCurrentActivity..[,c("Activity", "ZoneNumber", "Quantity")]
    PiCurrentActivity.. <- setDataframeClasses(PiCurrentActivity.., 
        c("character", "character", "numeric"))
    # Read the data file for the previous PI activity (actually activity for 2 period before)
    PiPreviousActivity.. <- read.csv(Input_$PreviousActivity, as.is=TRUE)
    PiPreviousActivity.. <- PiPreviousActivity..[,c("Activity", "ZoneNumber", "Quantity")]
    PiPreviousActivity.. <- setDataframeClasses(PiPreviousActivity.., 
        c("character", "character", "numeric"))
    # Keep data only for internal zones
    IsInternal. <- as.numeric(PiCurrentActivity..$ZoneNumber) <= max(as.numeric(Bz))
    PiCurrentActivity.. <- PiCurrentActivity..[IsInternal., ] 
    PiPreviousActivity.. <- PiPreviousActivity..[IsInternal., ]    
    # Define a function to split activity into residential and non-residential and to
    # aggregate by region
    calcRegionActivity <- function(PiActivity..){
        # Identify rows corresponding to residential and non-residential activities
        IsResActivity.X <- rep(FALSE, nrow(PiActivity..))
        IsResActivity.X[grep("HH", PiActivity..$Activity)] <- TRUE
        #edited by crf 6/6/2011 - added AA split activities to excluded list
        ExcludedActivities.Y <- c("AGRICULTURE AND MINING-Agriculture", "CONSTRUCTION", 
            "FORESTRY AND LOGGING", "Capitalists", "GovInstitutions", "HOMEBASED SERVICES",
            "RES_agmin_ag","RES_forst_log","RES_offc_off","CNST_main_xxx","CNST_nres_xxx",
            "CNST_othr_xxx","CNST_res_xxx","CNST_offc_off",
            "GOV_admn_gov","GOV_offc_off","SERV_home_xxx")
        IsExcludedActivity.X <- PiActivity..$Activity %in% ExcludedActivities.Y 
        IsNresActivity.X <- !(IsResActivity.X | IsExcludedActivity.X)
        # Identify the region each zone is located in
        Region.X <- Vars_$AlphaBeta_$Region[match(PiActivity..$ZoneNumber,
            Vars_$AlphaBeta_$BetaZone)]
        # Tabulate residential and non-residential activity by region
        ResActivity.Rg <- tapply(PiActivity..$Quantity[IsResActivity.X], 
            Region.X[IsResActivity.X], sum)
        NresActivity.Rg <- tapply(PiActivity..$Quantity[IsNresActivity.X], 
            Region.X[IsNresActivity.X], sum)
        # Return the results
        cbind(res=ResActivity.Rg, nres=NresActivity.Rg, agfor=rep(0, length(Rg))) 
        }
    # Calculate current activity quantities and activity change by region and floorspace category
    Activity.RgFc <- calcRegionActivity(PiCurrentActivity..)
    PrevActivity.RgFc <- calcRegionActivity(PiPreviousActivity..)
    ActivityChange.RgFc <- Activity.RgFc - calcRegionActivity(PiPreviousActivity..)
    # Save the activity and activity change in a list
    Vars_$Activity.RgFc <- Activity.RgFc
    Vars_$ActivityChange.RgFc <- ActivityChange.RgFc
    # Clean up the work space
    rm(PiCurrentActivity.., PiPreviousActivity.., calcRegionActivity,
        Activity.RgFc, ActivityChange.RgFc, PrevActivity.RgFc, IsInternal.)
    
 
#Load quantities of floorspace by alpha zone and floorspace category
#-------------------------------------------------------------------

#::

    # Read in data
    Quant.AzFt <- as.matrix(read.table(Input_$FloorQuantities, header=TRUE, row.names=1, sep=","))
    dimnames(Quant.AzFt) <- list(Az, Ft)
    # Removed if statement for Using AA, now the following line assumes AA - AB 7-28-15
    Quant.AzFt[,c(Fr,Fn)] <- Quant.AzFt[,c(Fr,Fn)] / 1000000
    # Split into residential, nonresidential and agricultural/forest and add to vars
    Vars_$Quant.AzFr <- Quant.AzFt[,Fr]
    Vars_$Quant.AzFn <- Quant.AzFt[,Fn]
    Vars_$Quant.AzFa <- Quant.AzFt[,Fa]
    # Clean up the work space
    rm(Quant.AzFt)


#Load average construction cost per building square foot by floorspace type
#--------------------------------------------------------------------------

#::

    # Define field names and types
    FieldNames <- c("FloorType", "Cost")
    FieldTypes <- c("character", "numeric")
    # Read the data file for the construction costs
    ConstructCosts.. <- read.table(Input_$ConstructionCosts, header=TRUE, sep=",", col.names=FieldNames,
        row.names=1, colClasses=FieldTypes)
    # Make vectors of residential and non-residential costs
    Costs.Fr <- ConstructCosts..[Fr,"Cost"]
    names(Costs.Fr) <- Fr
    Costs.Fn <- ConstructCosts..[Fn,"Cost"]
    names(Costs.Fn) <- Fn
    # Save the results in a list
    Vars_$Costs.Fr <- Costs.Fr
    Vars_$Costs.Fn <- Costs.Fn
    # Clean up the work space
    rm(FieldNames, FieldTypes, ConstructCosts.., Costs.Fr, Costs.Fn)
    

#Load quantity of land in each zoning category by alpha zone
#-----------------------------------------------------------

#::

    # Read in the data
    ZoningQuant.. <- read.table(Input_$ZoningQuantities, header=TRUE, row.names=1, sep=",")
    # Replace missing values with zeros
    ZoningQuant..[is.na(ZoningQuant..)] <- 0
    # Convert into matrix 
    Zoning.AzZc <- as.matrix(ZoningQuant..)
    # Make sure that matrix is in correct order
    Zoning.AzZc <- Zoning.AzZc[Az,Zc]
    # Convert to millions of square feet
    Zoning.AzZc <- Zoning.AzZc / 1e+06
    # Save as a list
    Vars_$Zoning.AzZc <- Zoning.AzZc
    # Clean up the work space
    rm(ZoningQuant.., Zoning.AzZc)


#Load in data on compatibility of each floorspace type with each zoning category
#-------------------------------------------------------------------------------

#::

    # Define field types
    #FieldTypes <- rep("character",20)
    # Read in data file of compatibility ratings
    ZoneCompIn.FdZc <- as.matrix(read.table(Input_$ZoningCompatibility, header=TRUE, row.names=1, 
        sep=",", colClasses="character"))
    # Make sure is in proper order
    print(Fd)
    print(Zc)
    print(ZoneCompIn.FdZc)
    ZoneCompIn.FdZc <- ZoneCompIn.FdZc[Fd,Zc]
    # Map compatibility categories to numerical values
    CompatibilityKey.X <- c(NP=0, VL=0.1, L=0.2, M=0.5, H=0.8, VH=1) # AB 8-11-10 changed NP from = 1e-50 to 0
                                                                     # KC/AB 6-27-11 changed VL from 0.01 to 0.1
    Vars_$Compat.FdZc <- array(as.vector(CompatibilityKey.X[match(ZoneCompIn.FdZc, 
        names(CompatibilityKey.X))]), dim=dim(ZoneCompIn.FdZc), dimnames=dimnames(ZoneCompIn.FdZc))
    # Clean up work space
    rm(ZoneCompIn.FdZc, CompatibilityKey.X)


#Load in floor to area ratios by zoning type and floorspace type
#---------------------------------------------------------------

#::

    # Load in floor area data file
    Far.FdZc <- as.matrix(read.table(Input_$FloorAreaRatios, header=TRUE, row.names=1, sep=","))
    # Make sure is in proper order
    Far.FdZc <- Far.FdZc[Fd,Zc]
    # Save in list
    Vars_$Far.FdZc <- Far.FdZc
    # Clean up the workspace
    rm(Far.FdZc)
    

#Load in square footage of land area occupied by different floorspace types
#--------------------------------------------------------------------------

#::

    # Read in the occupied land area data table
    LandSqft..Fd <- read.table(Input_$LandSqft, header=TRUE, row.names=1, sep=",")
    # Convert to vector
    LandSqft.Fd <- LandSqft..Fd[,1]
    names(LandSqft.Fd) <- rownames(LandSqft..Fd)
    # Make sure order is correct
    LandSqft.Fd <- LandSqft.Fd[Fd]
    
    # New section of code to weight the Size by the change in price - AB 7/26/11
    #---------------------
    AvgPrice.. <- read.csv(gsub(".csv$", "Totals.csv", Input_$ExchangeResults), as.is=TRUE)
    rownames(AvgPrice..) <- AvgPrice..$Commodity
    CurrAvgPrice.Fd <- AvgPrice..[Fd, "AveragePrice"]
    
    # Bring in the starting (Base) average prices - currently previous activity, unless I can get a base year indicator in the properties file 
    AvgPrice.. <- read.csv(paste(Input_$basePath, "ExchangeResultsTotals.csv", sep=""), as.is=TRUE)
    rownames(AvgPrice..) <- AvgPrice..$Commodity
    BaseAvgPrice.Fd <- AvgPrice..[Fd, "AveragePrice"]
    rm(AvgPrice..)
    
    # determine the percentage change between the base and the current year
    AvgPriceChange.Fd <- CurrAvgPrice.Fd/BaseAvgPrice.Fd
    rm(CurrAvgPrice.Fd, BaseAvgPrice.Fd)
    # adjust so that the overall change is normalized
    AvgPriceChange.Fd <- AvgPriceChange.Fd/mean(AvgPriceChange.Fd)    
    #-------------------------
    
    # Add to list
    Vars_$LandSqft.Fd <- LandSqft.Fd * AvgPriceChange.Fd  # added multiplier for change in Price - AB 7/26/2011
    # Clean up the work space
    rm(LandSqft..Fd, LandSqft.Fd, AvgPriceChange.Fd)



#LOAD THE MODEL COEFFICIENT DATA
#===============================

#Coefficients for the models are contained in text files. The text files containing the coefficient values are read and combined into a list named Cf_. Since residential and nonresidential floor space development markets are treated separately, there are two sets of coefficient values. There are 4 text files specifying the coefficients for each market. One of the text files includes all of the coefficients except for alternative specific constants that are used in the models to allocate construction to regions and to allocate among floor space types. Some of the coefficients in this file vary with floor space type while others do not. For this reason, the coefficients are put into a list where each component is named with the coefficient name. The text files containing alternative specific constants are in tabular form because the values vary by region or by region and floor space type. These are stored as matrices in Cf_. 


#Create list to store the coefficients
#-------------------------------------

#::

    # Coefficients are stored in a list named Cf_
    Cf_ <- list()


#Read the residential and nonresidential coefficients and place into the list
#----------------------------------------------------------------------------

#The model equations for residential and nonresidential floorspace types have the same functional forms. They only differ in the parameter estimates. The residential and nonresidential coefficients are also stored in text files that have the same structure.

#::

    # Define a function to read in a file of coefficients and organize into a list
    loadCoefficients <- function(File){
        # Define field names and types
        FieldNames <- c("Name", "Type", "Coeff")
        FieldTypes <- c("character", "character", "numeric")
        # Read the coefficients file
        Cf.. <- read.table(File, header=TRUE, sep=",", col.names=FieldNames,
            colClasses=FieldTypes)
        # Place coefficients into a list
        Cf_ <- list()
        Cf <- unique(Cf..$Name)
        for(cf in Cf){
            cf. <- Cf..$Coeff[Cf..$Name == cf]
            if(length(cf.) > 1){
                names(cf.) <- Cf..$Type[Cf..$Name == cf]
                }
            Cf_[[cf]] <- cf.
            }
        Cf_
        }
        
    # Read in the residential coefficients
    Cf_$res_$Cf_ <- loadCoefficients(Input_$ResCoefficients)      

    # Read in the nonresidential coefficients
    Cf_$nres_$Cf_ <- loadCoefficients(Input_$NresCoefficients)      


#Read the residential and nonresidential regional alternative specific constants (Asc3.Rg & Asc4.Rg)
#---------------------------------------------------------------------------------------------------

#The alternative specific constants for splitting residential and nonresidential dollar values of floorspace increases and decreases vary by region. Because they don't vary with floor space type, the residential and nonresidential values can be stored in the same tables. There is a table of values for increases by region and a table of values for decreases by region.

#::

    # Define a function to read in a table of values and output the results as vectors
    # One for residential and the other for nonresidential
    loadAsc3Asc4 <- function(File){
        # Define field names and types
        FieldNames <- c("Region", "Res", "Nres")
        FieldTypes <- c("character", "numeric", "numeric")
        # Read the coefficients file
        Cf.. <- read.table(File, header=TRUE, sep=",", col.names=FieldNames,
            colClasses=FieldTypes)
        # Add to Cf_
        ResAsc.Rg <- Cf..$Res
        NresAsc.Rg <- Cf..$Nres
        names(ResAsc.Rg) <- names(NresAsc.Rg) <- Cf..$Region
        list(res=ResAsc.Rg, nres=NresAsc.Rg)
        }
        
    # Read in the residential and nonresidential increase alternative specific constants (Asc3.Rg)
    Asc3_ <- loadAsc3Asc4(Input_$Asc3.RgFc)
    
    # Read in the residential and nonresidential decrease alternative specific constants (Asc4.Rg)
    Asc4_ <- loadAsc3Asc4(Input_$Asc4.RgFc)
    
    # Assign the Asc3 and Asc4 to Cf_
    Cf_$res_$Asc3.Rg <- Asc3_$res
    Cf_$res_$Asc4.Rg <- Asc4_$res
    Cf_$nres_$Asc3.Rg <- Asc3_$nres
    Cf_$nres_$Asc4.Rg <- Asc4_$nres
    rm(Asc3_, Asc4_)
    
#Read the floor space type increase and decrease alternative specific constants
#------------------------------------------------------------------------------

#The alternative specific constants for splitting the residential and nonresidential floor space categories into floor space types vary by region and floor space type. Four files are read in (residential and nonresidential increase and decrease). These files are ALDAsc1.RgFr, ALDAsc2.RgFr, ALDAsc1.RgFn, ALDAsc2.RgFn.

#::

    #Define a function to read in the files 
    loadAsc1Asc2 <- function(File, Fx, Rg){
        Cf.. <- read.table(File, header=TRUE, sep=",", row.names=1, check.names=FALSE)
        Asc.RgFx <- Cf..[,-1]
        if(!all(names(Cf..) %in% Fx) | !all(rownames(Cf..) %in% Rg)){
            stop("Row and/or column names do not match regions or floor space types")
            }
        as.matrix(Cf..)[Rg,Fx]
        }
        
    #Read in the residential Asc1 values
    Cf_$res_$Asc1.RgFr <- loadAsc1Asc2(Input_$Asc1.RgFr, Fr, Rg)
        
    #Read in the residential Asc2 values
    Cf_$res_$Asc2.RgFr <- loadAsc1Asc2(Input_$Asc2.RgFr, Fr, Rg)
        
    #Read in the nonresidential Asc1 values
    Cf_$nres_$Asc1.RgFn <- loadAsc1Asc2(Input_$Asc1.RgFn, Fn, Rg)
        
    #Read in the nonresidential Asc2 values
    Cf_$nres_$Asc2.RgFn <- loadAsc1Asc2(Input_$Asc2.RgFn, Fn, Rg)
        
                    