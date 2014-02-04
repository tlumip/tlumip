#=================================
#Aggregate Land Development Module
#=================================

#:File Name: ALD.R
#:Authors: Brian Gregor
#:Contact: brian.j.gregor@odot.state.or.us
#:Date: 8/09/10
#:Version: 1.30a
#:Copyright: 2007, 2008, 2010, Oregon Department of Transportation
#:License: GPL2



#PURPOSE AND GENERAL DESCRIPTION
#===============================

#This is the main program for the Aggregate Land Development (ALD) Module of the Oregon2 Transitional Model. The ALD module is a component of the Transistional Model that will be replaced by a disaggregate land development module in the final Oregon 2 model. The ALD module computes total floor space additions by Alpha zone and floor space type. The steps in the computation are as follows:

#1. Residential and non-residential construction dollar amounts are input from the ED module and split into floor space removal and floor space construction amounts by region using activity quantities and change in activity quantities from PI. The relevant procedures are defined in the definitions for the "allocateDecreaseToRegions" and "allocateConstToRegions" functions. It should be noted that the decreases do not represent the cost of demolition. They represent the average construction values of the floor spaces removed. After the dollar amounts of decreases and increases are allocated to regions, all subsequent computations, except for the calculation of floor space capacities, are done at the regional level.

#2. Regional residential and non-residential floor space removal and construction dollars are split into the respective floor space types based on occupancy rates and total quantity of floor space of each floor space type. The procedure for allocating decreases is defined in the definition for the "allocateFloorDecrease" function. The procedure for allocating increases is defined in the "allocateFloorProd" function. The dollar values of floor space decreases and increases are converted into square feet using statewide average construction costs by floor space type.

#3. Floor space capacity is calculated for each alpha zone based on the area of land zoned for different types of development, the compatibility of each floor space type with each zoning category, and the allowable floor-area ratio of each zoning category. The procedure for calculating floor space capacity is defined in the calcFloorCapacity function.

#4. Floor space decreases by floor space type are allocated to Alpha zones in each region. These decreases represent the loss of floor space of a type due to demolition or redevelopment. The allocation of floor space decreases by type in each region is a function of the proportion of regional floor space of the type in the Alpha zone and the price of floor space of the type in the Alpha zone. The modeled floor space decreases of any type in any zone are capped so that they may not exceed the total quantity of floor space of that type in that zone. The procedure is defined in the definition for the "calculateFloorDecrease" function.

#5. Floor space increases by floor space type are calculated and allocated to Alpha zones in each region. The increase in floor space of a type is allocated to Alpha zones in a region as a function of the proportion of regional floor space of that type in each zone, the price of floor space of that type in each zone, and the ratio of the amount of floor space of the type in each zone (after decreases have been computed) to the capacity for floor space of the type in the zone. The procedure is defined in the definition for the "allocateIncrease" function.

#After completing the allocations of floorspace increases and new floorspace quantities, several files are written out to be used by other TM modules. These are files of floorspace increases by Alpha zone and floorspace type and floorspace quantities by Alpha zone and floorspace type. The files are written out in both a "matrix" form, where Alpha zones and floorspace types refer to rows and columns of the data, and a "flat" form where all the quantities are contained in one column of the file and other columns provide indexes to the Alpha zones and floorspace types. In addition to these files, the script saves several R objects that may be accessed to diagnose an ALD run.



#SCRIPT ORGANIZATION AND FORMAT
#==============================

#This is the main program for the ALD model. It also calls scripts that loads all of the model inputs (ALD_Inputs.R) and a script that loads all of the functions used by the model (ALD_Functions.R).

#This script and the scripts that it calls uses the object naming system described in "CodingConventions20070105.txt". These conventions specify how object names are used to identify object data structures and dimensionality. A key aspect of object naming is the definition of abbreviations that identify the dimensionality of objects. These abbreviations refer to dimensions in object names. They are also objects that are vectors of names for the categories that the dimension is comprised of. The dimension abbreviations in the ALD scripts are as follows:

#Zc - comprehensive plan zoning categories
#Ft - all floorspace types as used by PI
#Fc - floorspace categories representing groups of types (i.e. residential, nonresidential, agriculture/ forestry
#Fr - residential floorspace types
#Fn - nonresidential floorspace types
#Fa - agriculture and forestry floorspace types
#Fd - developed floorspace types (e.g. residential and nonresidential)
#Az - Alpha zones
#Bz - Beta zones
#Rg - ALD regions
#St - State
#Cn - County names
#Cc - County Census FIPS codes

#This script and the scripts that it calls is formatted using the reStructuredText plaintext markup syntax. HTML formatted documentation can be produced from this file by removing the first column of the text file and then processing the file using Docutils.



#HISTORY
#=======

#This version is the first major revision to ALD. Previous versions of ALD allocated development directly from the model-wide level to the alpha zone level. This version of ALD allocates development in two geographic steps. First, decreases and increases are allocated to 15 regions. Then the regional allocations are suballocated to alpha zones.
#3/19/07 Revised the calls to allocateConstToRegions and allocateDecreaseToRegions to include all the arguments now required by these functions.
#3/19/07 Refomulated allocateConstToRegions and allocateDecreaseToRegions functions again to use original arguments. Make consistent function calls.
#3/07/08 The calculateFloorDecrease() function now keeps track of the decreases that could not be allocated to zones because the decrease amount exceeds the quantity of floor space of the type in the zone. These unallocated decreases are summed up by floor space type for each region. They are then subtracted from the regional increases in the allocateIncrease() function so that the net change at the end of the model is consistent with the net change at the beginning. This change required the addition of data structures to store the unallocated decreases for residential and nonresidential floor space types and an additional argument in the call to the allocateIncrease() function.
#4/07/08 Changes made to be consistent with new model directory structure. The reference directory for ALD is now the same as the ALD base year directory. Also update the input file name references to be consistent with final naming system (in ALD_Inputs.R).
#4/29/08 Now reads properties file to identify locations of input files, code location and output directory
#4/30/08 Corrected errors in how the properties file is parsed to create the Input_ list
#8/09/10 AB - Added a save function for Q_ calculated from the allocateIncrease function
#10/12/10 AB - Added Price.AzFx from PI as an input to the allocateFloorProd function.  Changes were made under ALD_Function.R to use this input. 
#9/02/11 CRF - Fixed properties file gsub regular expression to get literal "." (using [.]) instead of  catchall .
#9/09/11 CRF - Added conditional expression for writing out floorspace data: different formats used for pi and aa

#DEFINE POINTERS TO DIRECTORIES AND FILES TO LOAD
#================================================

#Read in the command line arguments to identify the year index and location of properties file
#---------------------------------------------------------------------------------------------

#The input argument positions depend on whether the operating system is Windows or Linux. The following objects are created containing path and year references:

#::

    # Read the command line arguments to get the current year index
    # and the path to the properties file
    # This is the default code that AO calls to run ALD using Rcmd
    # Comment this out if the ALD code is to be tested in Rgui
    PropertiesFile <- sub("-", "", commandArgs()[length(commandArgs())-1])
    CurrYearIndex <- as.numeric(sub("-", "", commandArgs()[length(commandArgs())]))

    # This is the code to be run in Rgui for testing ALD
    # Comment it out if running ALD using Rcmd
    # PropertiesFile <- "1st_Template_Test/global.properties"
    # DataPath <- ""
    # CurrYearIndex <- 1

#Parse properties file to identify location of input files, output directory, etc.
#---------------------------------------------------------------------------------
    Input_ <- list()

	Properties. <- readLines(PropertiesFile)
	Properties. <- Properties.[grep("ald", Properties.)]
	Properties. <- gsub("ald[.]", "", Properties.)
	sapply(Properties., function(x){
	   NameValue. <- unlist(strsplit(x, "="))
		#Input_[[NameValue.[1]]] <<- NameValue.[2]
		Input_[[sub("^[[:space:]]*(.*?)[[:space:]]*$", "\\1",NameValue.[1], perl=TRUE)]] <<- sub("^[[:space:]]*(.*?)[[:space:]]*$", "\\1",NameValue.[2], perl=TRUE)		
		} )
	CodePath <- Input_$codePath
    ALDCurrDirectory <- Input_$ALDCurrDirectory


#LOAD FUNCTION DEFINITIONS
#=========================

#The bulk of the ALD calculations are carried out by functions defined in the ALD_120_Functions_Load script. The script defines the functions as components of a list named Funs_. After the script is run to define the functions, Funs_ is attached so that the function names are added to the search path and the functions can be called directly.

#::

    source(paste(CodePath,"ALD_Functions.R", sep=""))

    attach(Funs_)
    

#LOAD INPUTS AND COEFFICIENTS NEEDED TO RUN ALD
#==============================================

#Run the ALD_120_Inputs.R script which identifies the input input data files to be read, reads the input data and puts it in a list named Vars_, and reads the calibrated model coefficients and places them in a list called Cf_

#::

    source(paste(CodePath,"ALD_Inputs.R", sep=""),echo=TRUE)
    
    attach(Vars_)

    attach(Cf_)
      

#CALCULATE THE MODEL RESULTS
#===========================

#Split residential and non-residential increases among regions
#-------------------------------------------------------------

#::

    ResNivq.Rg <- allocateConstToRegions(ConsVal.Fc["res"], Activity.RgFc[,"res"],
        ActivityChange.RgFc[,"res"], res_$Cf_[["B1v"]], res_$Cf_[["B2v"]], 
        res_$Cf_[["B3v"]], res_$Cf_[["B4v"]], res_$Asc3.Rg)
    
    NresNivq.Rg <- allocateConstToRegions(ConsVal.Fc["nres"], Activity.RgFc[,"nres"],
        ActivityChange.RgFc[,"nres"], nres_$Cf_[["B1v"]], nres_$Cf_[["B2v"]], 
        nres_$Cf_[["B3v"]], nres_$Cf_[["B4v"]], nres_$Asc3.Rg)


#Split residential and non-residential decreases among regions
#-------------------------------------------------------------

#::

    ResNdvq.Rg <- allocateDecreaseToRegions(ConsVal.Fc["res"], res_$Cf_[["Df"]], 
        Activity.RgFc[,"res"], ActivityChange.RgFc[,"res"], res_$Cf_[["B5v"]], 
        res_$Cf_[["B6v"]], res_$Cf_[["B7v"]], res_$Cf_[["B8v"]], res_$Asc4.Rg)

    NresNdvq.Rg <- allocateDecreaseToRegions(ConsVal.Fc["nres"], nres_$Cf_[["Df"]], 
        Activity.RgFc[,"nres"], ActivityChange.RgFc[,"nres"], nres_$Cf_[["B5v"]], 
        nres_$Cf_[["B6v"]], nres_$Cf_[["B7v"]], nres_$Cf_[["B8v"]], nres_$Asc4.Rg)


#Calculate the residential and nonresidential floor space increases by type for each region
#------------------------------------------------------------------------------------------

#::

    # Calculate the residential increases
    Nivq.RgFr <- array(0, dim=c(length(Rg), length(Fr)), dimnames=list(Rg, Fr))
    for(rg in Rg){
        Nivq.RgFr[rg,] <- allocateFloorProd(ResNivq.Rg, Occupied.BzFr, Quant.AzFr, Costs.Fr, Price.AzFr,
            res_$Cf_[["Lq1"]], res_$Cf_[["Bq1"]], res_$Cf_[["Bq2"]], res_$Asc1.RgFr[rg,], 
            AlphaBeta_, rg)$Nivq.Fx
        }
    Niq.RgFr <- sweep(Nivq.RgFr, 2, Costs.Fr, "/")

    # Calculate the nonresidential increases
    Nivq.RgFn <- array(0, dim=c(length(Rg), length(Fn)), dimnames=list(Rg, Fn))
    for(rg in Rg){
        Nivq.RgFn[rg,] <- allocateFloorProd(NresNivq.Rg, Occupied.BzFn, Quant.AzFn, Costs.Fn, Price.AzFn,
            nres_$Cf_[["Lq1"]], nres_$Cf_[["Bq1"]], nres_$Cf_[["Bq2"]], nres_$Asc1.RgFn[rg,],
            AlphaBeta_, rg)$Nivq.Fx
        }
    Niq.RgFn <- sweep(Nivq.RgFn, 2, Costs.Fn, "/")


#Calculate the residential and nonresidential floor space decreases by type for each region
#------------------------------------------------------------------------------------------

#::

    # Calculate the residential decreases
    Ndvq.RgFr <- array(0, dim=c(length(Rg), length(Fr)), dimnames=list(Rg, Fr))
    for(rg in Rg){
        Ndvq.RgFr[rg,] <- allocateFloorDecrease(ResNdvq.Rg, Occupied.BzFr, Quant.AzFr, 
            Costs.Fr, res_$Cf_$Lq2, res_$Cf_$Bq3, res_$Cf_$Bq4, res_$Asc2.RgFr[rg,], 
            AlphaBeta_, rg)$Ndvq.Fx
        }
    Ndq.RgFr <- sweep(Ndvq.RgFr, 2, Costs.Fr, "/")


    # Calculate the nonresidential decreases
    Ndvq.RgFn <- array(0, dim=c(length(Rg), length(Fn)), dimnames=list(Rg, Fn))
    for(rg in Rg){
        Ndvq.RgFn[rg,] <- allocateFloorDecrease(NresNdvq.Rg, Occupied.BzFn, Quant.AzFn, 
            Costs.Fn, nres_$Cf_$Lq2, nres_$Cf_$Bq3, nres_$Cf_$Bq4, nres_$Asc2.RgFn[rg,], 
            AlphaBeta_, rg)$Ndvq.Fx
        }
    Ndq.RgFn <- sweep(Ndvq.RgFn, 2, Costs.Fn, "/")


#Allocate the residential and nonresidential floor space decreases to Alpha zones
#--------------------------------------------------------------------------------

#::

    # Allocate the residential decreases
    DQ.AzFr <- array(0, dim=c(length(Az), length(Fr)), dimnames=list(Az, Fr))
    UnallocatedDQ.RgFr <- array( 0, dim=c( length(Rg), length(Fr) ), dimnames=list(Rg, Fr) )
    for(rg in Rg){
        DQ_ <- calculateFloorDecrease(Quant.AzFr, Ndq.RgFr[rg,], Price.AzFr, 
            res_$Cf_$B3f, res_$Cf_$B4f, AlphaBeta_, rg)
		DQ.AzFr[rownames(DQ_$DQ.AxFx),] <- DQ_$DQ.AxFx
        UnallocatedDQ.RgFr[rg,] <- DQ_$UnallocatedDQ.Fx
        rm(DQ_)
        }

    # Allocate the nonresidential decreases
    DQ.AzFn <- array(0, dim=c(length(Az), length(Fn)), dimnames=list(Az, Fn))
    UnallocatedDQ.RgFn <- array( 0, dim=c( length(Rg), length(Fn) ), dimnames=list(Rg, Fn) )
    for(rg in Rg){
        DQ_ <- calculateFloorDecrease(Quant.AzFn, Ndq.RgFn[rg,], Price.AzFn, 
            nres_$Cf_$B3f, nres_$Cf_$B4f, AlphaBeta_, rg)
		DQ.AzFn[rownames(DQ_$DQ.AxFx),] <- DQ_$DQ.AxFx
        UnallocatedDQ.RgFn[rg,] <- DQ_$UnallocatedDQ.Fx
        rm(DQ_)
        }

            
#Calculate floor space capacities by alpha zone and floorspace type
#-----------------------------------------------------------------

#Capacity calculations are done for the whole model area, not on a region basis.

#::

    FloorspaceCapacities_ <- calcFloorCapacity(LandSqft.Fd, Compat.FdZc, Zoning.AzZc, Far.FdZc, 
        res_$Cf_$Lp, res_$Cf_$Stc, FloorDef_)
    Cap.AzFr <- FloorspaceCapacities_$ResCap.AzFr
    Cap.AzFn <- FloorspaceCapacities_$NresCap.AzFn


#Allocate the residential and nonresidential floor space increases to Alpha zones
#--------------------------------------------------------------------------------

#::

    # Allocate the residential increases            
    CurrQ.AzFr <- array(0, dim=c(length(Az), length(Fr)), dimnames=list(Az, Fr))
    IQ.AzFr <- array(0, dim=c(length(Az), length(Fr)), dimnames=list(Az, Fr))
    Fr_ <- list()
    for(rg in Rg){
        CurrQ_ <- allocateIncrease(Cap.AzFr, Niq.RgFr[rg,], Quant.AzFr, DQ.AzFr, 
			UnallocatedDQ.RgFr[rg,], Price.AzFr, res_$Cf_$B5f, res_$Cf_$B6f, res_$Cf_$B7f,
			res_$Cf_$B8f, res_$Cf_$B9f, AlphaBeta_, Region=rg)
 		  Fr_[[rg]] <- CurrQ_
     CurrQ.AzFr[rownames(CurrQ_$CurrQ.AxFx),] <- CurrQ_$CurrQ.AxFx
   		IQ.AzFr[rownames(CurrQ_$IQ.AxFx),] <- CurrQ_$IQ.AxFx
        }

    # Allocate the nonresidential increases            
    CurrQ.AzFn <- array(0, dim=c(length(Az), length(Fn)), dimnames=list(Az, Fn))
    IQ.AzFn <- array(0, dim=c(length(Az), length(Fn)), dimnames=list(Az, Fn))
    Fn_ <- list()
    for(rg in Rg){
        CurrQ_ <- allocateIncrease(Cap.AzFn, Niq.RgFn[rg,], Quant.AzFn, DQ.AzFn, 
			UnallocatedDQ.RgFn[rg,], Price.AzFn, nres_$Cf_$B5f, nres_$Cf_$B6f, nres_$Cf_$B7f,
			nres_$Cf_$B8f, nres_$Cf_$B9f, AlphaBeta_, Region=rg)
        Fn_[[rg]] <- CurrQ_
        CurrQ.AzFn[rownames(CurrQ_$CurrQ.AxFx),] <- CurrQ_$CurrQ.AxFx
        IQ.AzFn[rownames(CurrQ_$IQ.AxFx),] <- CurrQ_$IQ.AxFx
        }

        Q_ <- list(Fr=Fr_, Fn=Fn_)

#Write out results files
#=======================

#Combine the residential, nonresidential, agricultural and forest quantities into output tables
#----------------------------------------------------------------------------------------------

#::

    # Combine the floor space totals
    CurrQ.AzFt <- cbind(CurrQ.AzFr, CurrQ.AzFn, Quant.AzFa)
    CurrQ.AzFt <- CurrQ.AzFt[,Ft]
    
    # Combine the floor space increments
    IQ.AzFa <- Quant.AzFa
    IQ.AzFa[,] <- 0
    IQ.AzFt <- cbind(IQ.AzFr, IQ.AzFn, IQ.AzFa)
    IQ.AzFt <- IQ.AzFt[,Ft]
    

#Write out current floorspace inventory in matrix table form
#-----------------------------------------------------------

#::

    writeTmTable(CurrQ.AzFt, ColNm="AZone", 
        FileNm=paste(ALDCurrDirectory, "FloorspaceInventory.csv", sep="/"))


#Write out current floorspace results flat table form
#----------------------------------------------------

#::

    options(scipen=7) # set this option high to avoid scientific notation in the output
    if (Input_$UsingAA == "true") {
        TotQRes.. <- data.frame(list(taz=rep(Az, length(Fr)), 
                        commodity=rep(Fr, each=length(Az)),
                        quantity=zapsmall(as.vector(CurrQ.AzFr*1000000),9)))
        TotQNres.. <- data.frame(list(taz=rep(Az, length(Fn)), 
                        commodity= rep(Fn, each=length(Az)),
                        quantity=zapsmall(as.vector(CurrQ.AzFn*1000000),9)))
        TotQAgFor.. <- data.frame(list(taz=rep(Az, length(Fa)), 
                        commodity= rep(Fa, each=length(Az)),
                        quantity=zapsmall(as.vector(Quant.AzFa),9)))
        TotQOut.. <- rbind(TotQRes.., TotQNres.., TotQAgFor..)
    } else { #PI ways
        TotQRes.. <- data.frame(list(AZone=rep(Az, length(Fr)), 
                        FLRType=rep("Residential", length(Az) * length(Fr)),
                        FLRName= rep(Fr, each=length(Az)),
                        BldgMSQFT=zapsmall(as.vector(CurrQ.AzFr),9)))
        TotQNres.. <- data.frame(list(AZone=rep(Az, length(Fn)), 
                        FLRType=rep("NonResidential", length(Az) * length(Fn)),
                        FLRName= rep(Fn, each=length(Az)),
                        BldgMSQFT=zapsmall(as.vector(CurrQ.AzFn),9)))
        TotQAgFor.. <- data.frame(list(AZone=rep(Az, length(Fa)), 
                        FLRType=rep("AgForest", length(Az) * length(Fa)),
                        FLRName= rep(Fa, each=length(Az)),
                        BldgMSQFT=zapsmall(as.vector(Quant.AzFa),9)))
        TotQOut.. <- rbind(TotQRes.., TotQNres.., TotQAgFor..)
    }
    write.table(TotQOut.., paste(ALDCurrDirectory, "FloorspaceI.csv", sep="/"),
        row.names=FALSE, col.names=TRUE, sep=",", quote=FALSE)
    rm(TotQRes.., TotQNres.., TotQAgFor.., TotQOut..)


#Write out floorspace increments in matrix form
#----------------------------------------------

#::

    writeTmTable(IQ.AzFt, ColNm="AZone", 
        FileNm=paste(ALDCurrDirectory, "Increments_Matrix.csv", sep="/"))
    

#Write out total floorspace increments for PI and other modules that read matrices in flat format
#------------------------------------------------------------------------------------------------

#::

    IQRes.. <- data.frame(list(AZone=rep(Az, length(Fr)), 
                    FLRType=rep("Residential", length(Az) * length(Fr)),
                    FLRName= rep(Fr, each=length(Az)),
                    IncMSQFT=zapsmall(as.vector(IQ.AzFr),9)))
    IQNres.. <- data.frame(list(AZone=rep(Az, length(Fn)), 
                    FLRType=rep("NonResidential", length(Az) * length(Fn)),
                    FLRName= rep(Fn, each=length(Az)),
                    IncMSQFT=zapsmall(as.vector(IQ.AzFn),9)))
    IQOut.. <- rbind(IQRes.., IQNres..)
    write.table(IQOut.., paste(ALDCurrDirectory, "Increments.csv", sep="/"),
        row.names=FALSE, col.names=TRUE, sep=",", quote=FALSE)
    rm(IQRes.., IQNres.., IQOut..)



#Save R objects for diagnostic purposes
#======================================

#::

    # Create list to store region floor space increases
    Nivq_ <- list(ResNivq.Rg=ResNivq.Rg, NresNivq.Rg=NresNivq.Rg, Nivq.RgFr=Nivq.RgFr, 
        Nivq.RgFn=Nivq.RgFn, Niq.RgFr=Niq.RgFr, Niq.RgFn=Niq.RgFn)

    # Create list to store region floor space decreases
    Ndvq_ <- list(ResNdvq.Rg=ResNdvq.Rg, NresNdvq.Rg=NresNdvq.Rg, Ndvq.RgFr=Ndvq.RgFr, 
        Ndvq.RgFn=Ndvq.RgFn, Ndq.RgFr=Ndq.RgFr, Ndq.RgFn=Ndq.RgFn)

    # Create list to store Alpha zone floor space decreases
    DQ_ <- list(DQ.AzFr=DQ.AzFr, DQ.AzFn=DQ.AzFn)

    # Create list to store Alpha zone floor space increase
    CurrQ_ <- list(CurrQ.AzFr=CurrQ.AzFr, IQ.AzFr=IQ.AzFr, CurrQ.AzFn=CurrQ.AzFn, 
        IQ.AzFn=IQ.AzFn)
            
    #Save objects that include diagnostic data
    save(FloorspaceCapacities_, file=paste(ALDCurrDirectory, "FloorspaceCapacities_.RData", sep="/"))
    save(Nivq_, file=paste(ALDCurrDirectory, "Nivq_.RData", sep="/"))
    save(Ndvq_, file=paste(ALDCurrDirectory, "Ndvq_.RData", sep="/"))
    save(DQ_, file=paste(ALDCurrDirectory, "DQ_.RData", sep="/"))
    save(CurrQ_, file=paste(ALDCurrDirectory, "CurrQ_.RData", sep="/"))
    save(Q_, file=paste(ALDCurrDirectory, "Q_.RData", sep="/"))

    #To save the .Rdata file to the ALD directory for the simulation year
    #Comment this out if working in Rgui mode
    setwd(ALDCurrDirectory) 

    #Detach Funs_ , Vars_ and Cf_
    detach(Funs_)
    detach(Vars_)
    detach(Cf_)
    