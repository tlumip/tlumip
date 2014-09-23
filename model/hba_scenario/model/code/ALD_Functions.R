#===========================================
#Aggregate Land Development Module Functions
#===========================================

#:File Name: ALD_Functions.R
#:Authors: Brian Gregor
#:Contact: brian.j.gregor@odot.state.or.us
#:Date: 8/11/10
#:Version: 1.30
#:Copyright: 2007, 2008, 2010, Oregon Department of Transportation
#:License: GPL2


#HISTORY
#=======

#1/06/07 - Revised the allocateIncrease function
#3/07/08 The calculateFloorDecrease() function now keeps track of the decreases that could not be allocated to zones because the decrease amount exceeds the quantity of floor space of the type in the zone. These unallocated decreases are summed up by floor space type for each region. They are then subtracted from the regional increases in the allocateIncrease() function so that the net change at the end of the model is consistent with the net change at the beginning. Also corrected an error in the allocateIncrease() functions that was causing the sum of the net changes in floor space by floor space type to exceed the sums calculated earlier in the model. This was due to the decrease being added to the increase before allocating among zones. The decrease should not have been added to the increase.
#This version is the first major revision to ALD. Allocations are done by regions, rather than statewide. A new set of calculations are added to divide modelwide construction dollars into regional shares. The rest of the calculations are carried out as before, but on a regional basis.
#3/19/07 Changed the allocateConstToRegions and allocateDecreaseToRegions functions to normalize the activity change data so that the minimum value is zero, before applying proportion.
#3/24/07 Corrected an error in allocateConstToRegions and allocateDecreaseToRegions functions. These functions were substituting ones for zeros in ActivityChange.Rg if all values were zero (no activity change in the first two iterations). The normalization,then in this case by subtracting the minimum, recreated a vector of zeros. This was fixed by testing for all zeros after normalization.
#8/11/10 AB - Updating the allocateIncrease and calcFloorCapacity function to add hard limits on capacity and allowed quantity constraints
#9/24/10 AB - Changed the capacity limitation to be based on Currently Quantity not Quant - Decrease.
#10/12/10 AB - Updated the allocateFloorProd function to be based on the Value of a sqft from PI instead of assumed 1990 construction cost.  This allows the dollars given to each floorspace type to react to changing demand.  Previously the unreactive size term was dominating the utility
#7/22/11 AB - Corrected the compatibility equations in the calcFloorCapacity function

#PURPOSE AND DESCRIPTION
#=======================

#This is the portion of the ALD script that defines model functions. Documentation in this script addresses the functions defined within it. For overall documentation of ALD, please refer to the ALD.R script.




#CREATE A LIST FOR HOLDING THE FUNCTIONS
#=======================================

#::

    Funs_ <- list()



#DEFINE UTILITY FUNCTIONS USED IN THE SCRIPT
#===========================================

#Function to simplify multiplying matrix with row vector
#-------------------------------------------------------

#This function sweeps a matrix with a vector, multiplying each row of the matrix with the vector elementwise. The function is defined so that it may be used as an operator.

#:Name: %row*%
#:Argument: x - a numeric matrix
#:Argument: y - a numeric vector
#:Result: a numeric matrix

#::

    Funs_[["%row*%"]] <- function(x, y){
        if(!is.matrix(x)) stop("First operand must be matrix")
        if(!is.vector(y)) stop("Second operand must be vector")
        if(length(y) != dim(x)[2]) stop("Length of vector != number of columns")
        if((mode(x) != "numeric") | (mode(y) != "numeric")) stop("Operands must be numeric")
        sweep(x, 2, y, "*")
        }


#Function to simplify adding a column vector to a matrix
#-------------------------------------------------------

#This function sweeps a matrix with a vector, adding each column of the matrix with the vector elementwise. The function is defined so that it may be used as an operator.

#:Name: %col+%
#:Argument: x - a numeric matrix
#:Argument: y - a numeric vector
#:Result: a numeric matrix

#::

    Funs_[["%col+%"]] <- function(x, y){
        if(!is.matrix(x)) stop("First operand must be matrix")
        if(!is.vector(y)) stop("Second operand must be vector")
        if(length(y) != dim(x)[1]) stop("Length of vector != number of rows")
        if((mode(x) != "numeric") | (mode(y) != "numeric")) stop("Operands must be numeric")
        sweep(x, 1, y, "+")
        }


#Function to simplify dividing a matrix with a row vector
#--------------------------------------------------------

#This function sweeps a matrix with a vector, dividing each row of the matrix with the vector elementwise. The function is defined so that it may be used as an operator.

#:Name: %row/%
#:Argument: x - a numeric matrix
#:Argument: y - a numeric vector
#:Result: a numeric matrix

#::

    Funs_[["%row/%"]] <- function(x, y){
        if(!is.matrix(x)) stop("First operand must be matrix")
        if(!is.vector(y)) stop("Second operand must be vector")
        if(length(y) != dim(x)[2]) stop("Length of vector != number of columns")
        if((mode(x) != "numeric") | (mode(y) != "numeric")) stop("Operands must be numeric")
        sweep(x, 2, y, "/")
        }


#Function to sum Alpha zone values by Beta zone
#----------------------------------------------

#This function aggregates Alpha zone values to the Beta zone level by summing

#:Name: aggregateAlpha
#:Argument: Values.AzX - a matrix where the rows are Alpha zones and the columns may represent any numerical value. The rows of the matrix must be properly named using the Alpha zone names.
#:Argument: BetaZone.Az - a index vector of Beta zones corresponding to Alpha zones. The values of the index vector are Beta zones. The names of the index vector elements are Alpha zones.
#:Argument: Bz - A naming vector of Beta zones.
#:Result: Values.BzX - a matrix where the rows are Beta zones and the columns represent any numerical value. The rows of the matrix are ordered according to Bz.

#::

    Funs_$aggregateAlpha <- function(Values.AzX, BetaZone.Az, Az, Bz){
        # Check that all Beta zones are accounted for the the BetaZone.Az index vector
        if(!(setequal(BetaZone.Az, Bz))){
            stop("BetaZone.Az does not properly account for all Beta zones")
            }
        if(!(setequal(names(BetaZone.Az), Az))){
            stop("BetaZone.Az does not properly account for all Alpha zones")
            }
        if(!(setequal(rownames(Values.AzX), Az))){
            stop("The row names for Values.AzX do not correspond to Az")
            }
        apply(Values.AzX, 2, function(x) tapply(x, BetaZone.Az, sum))[Bz,]
        }


#Function to calculate proportions
#---------------------------------

#This function calculates proportions that each element of a vector or matrix represents of the whole, or also in the case of a matrix, the row sum or the column sum.

#:Name: proportion
#:Argument: Values. - a vector or matrix of values
#:Argument: Type - the type of proportioning process. The function currently recognizes two types: "ordinary" is simply each value divided by the sum of all values, "logit" is the exponential of each value divided by the sum of the exponentials of each value. The default is "ordinary".
#:Argument: Dim - the dimension over which proportioning occurs. The default is "all" in which case the result is the proportion of the whole vector or matrix. For matrices, the other options are "row" or "col" which calculate the proportion of row and column totals respectively.
#:Result: An object of the same dimensionality as the input object that contains the specified proportions.

#::

    Funs_$proportion <- function(Values., Type="ordinary", Dim="all"){
        # Perform input checks
        if(!(is.vector(Values.) | is.matrix(Values.))){
            stop("Error! This function only operates on vectors or matrices.")
            }
        if(!(Type %in% c("ordinary", "logit"))){
            stop("Error! Inappropriate Type argument. Use only ordinary or logit.")
            }
        if(!(Dim %in% c("all", "row", "col"))){
            stop("Error! Inappropriate Dim argument. Use only all, row, or col.")
            }
        if((is.vector(Values.)) & (Dim != "all")){
            Dim="all"
            warning("Attempt to use an inappropriate Dim argument to a vector")
            }
        # Make a dimension number, DimNum, corresponding to the Dim argument
        DimNum <- 0
        if(Dim == "row") DimNum <- 1
        if(Dim == "col") DimNum <- 2
        # If the Type is logit, change the Values. by exponentiating
        if(Type == "logit") Values. <- exp(Values.)
        # Do the proportioning based on the DimNum
        if(DimNum == 0){
            Props. <- Values. / sum(Values.)
            }
        if(DimNum != 0){
            Totals. <- apply(Values., DimNum, sum)
            Props. <- sweep(Values., DimNum, Totals., "/")
            }
        # Return the result
        Props.
        }


#Function to write out a table in the form expected by other TM modules
#----------------------------------------------------------------------

#This function writes out a table in the form expected by other TM modules. The row names of the table are placed in the first column of the table and every column in the table has a header.

#:Name: writeTmTable
#:Argument: X - a matrix or data frame
#:Argument: ColNm - the name for the first column which will contain the row names
#:Argument: FileNm - the file path name
#:Result: No value is return. File of name FileNm is written to disk

#::

    Funs_$writeTmTable <- function(X, ColNm, FileNm){
        if(is.matrix(X)) X <- data.frame(X)
        X <- cbind(rownames(X), X)
        colnames(X)[1] <- ColNm
        write.table(X, FileNm, row.names=FALSE, col.names=TRUE, sep=",", quote=FALSE)
        }


#Function to simplify the process for setting the classes of all data in a dataframe
#-----------------------------------------------------------------------------------

#This function sets all the columns of data in a dataframe to a vector of classes

#:Name: setDataframeClasses
#:Argument: Dataframe.. - a dataframe
#:Argument: Classes. - a vector of classes with the same length as the number of columns of the dataframe
#:Result: A dataframe that is the same as Dataframe.. but with the classes changed

    setDataframeClasses <- function(Dataframe.., Classes.){
        # Check that the length of Classes. corresponds to Dataframe..
        if(length(Classes.) != ncol(Dataframe..)){
            stop("Length of Classes. not equal to number of columns in the Dataframe.. argument")
            }
        for(i in 1:length(Classes.)){
            class(Dataframe..[[i]]) <- Classes.[i]
            }
        Dataframe..
        }
        
#DEFINE FUNCTIONS TO CARRY OUT ALD ALLOCATION PROCEDURES
#=======================================================

#Function to allocate residential and nonresidential construction dollars by region
#----------------------------------------------------------------------------------

#This function allocates residential and nonresidential construction dollars among regions proportional to the proportions of activity and the change in activity in each region. A Cobb-Douglas formulation is used. Before proportioning the change in activity, it is transformed to start at a zero scale.

#:Name: allocateConstToRegions
#:Argument: ConsVal - modelwide floor space production in dollars
#:Argument: Activity.Rg - amount of activity by region
#:Argument: ActivityChange.Rg - change in activity by region
#:Argument: B1v - inertia term for activity proportions in Cobb-Douglas equation
#:Argument: B2v - exponent of the activity proportions in Cobb-Douglas equation
#:Argument: B3v - inertia term for the activity change proportions in Cobb-Douglas equation
#:Argument: B4v - exponent of the activity change proportions in Cobb-Douglas equation
#:Argument: Asc3.Rg - vector of alternative specific constants for the regions
#:Result: Nivq.Rg - the value of floor space production by region

#::

    Funs_$allocateConstToRegions <- function(ConsVal, Activity.Rg, ActivityChange.Rg, 
        B1v, B2v, B3v, B4v, Asc3.Rg){
        # Compute the proportions of Activity in each region
        ActProp.Rg <- proportion(Activity.Rg)
        # Transform ActivityChange.Rg so that the minimum value is zero
        ActivityChange.Rg <- ActivityChange.Rg - min(ActivityChange.Rg)
        # Before two model periods have occurred, there is no change in activity.
        # To avoid NaN error assign a value of one. Gives equal proportions with respect to this value
        if(all(ActivityChange.Rg == 0)) ActivityChange.Rg[] <- 1
        # Compute the proportions of the normalized activity change
        ActChangeProp.Rg <- proportion(ActivityChange.Rg)
        # Compute a Cobb-Douglas function to use for proportioning the increase among regions
        CobbDouglas.Rg <- (ActProp.Rg + B1v)^B2v * (ActChangeProp.Rg + B3v)^B4v * Asc3.Rg
        NivqProp.Rg <- proportion(CobbDouglas.Rg)
        # Apportion production among regions
        Nivq.Rg <- ConsVal * NivqProp.Rg
        Nivq.Rg
        }


#Function to allocate the value of residential and nonresidential space decreases by region
#------------------------------------------------------------------------------------------

#This function allocates the value of residential and nonresidential floor space decreases among regions proportional to the proportions of activity and the change in activity in each region. A Cobb-Douglas formulation is used.

#:Name: allocateDecreaseToRegions
#:Argument: ConsVal - modelwide floor space production in dollars
#:Argument: Df - modelwide proportion of floor space value that is removed
#:Argument: Activity.Rg - amount of activity by region
#:Argument: ActivityChange.Rg - change in activity by region
#:Argument: B5v - inertia term for activity proportions in Cobb-Douglas equation
#:Argument: B6v - exponent of the activity proportions in Cobb-Douglas equation
#:Argument: B7v - inertia term for the activity change proportions in Cobb-Douglas equation
#:Argument: B8v - exponent of the activity change proportions in Cobb-Douglas equation
#:Argument: Asc4.Rg - vector of alternative specific constants for the regions
#:Result: Ndvq.Rg - value of the floorspace that is removed

#::

    Funs_$allocateDecreaseToRegions <- function(ConsVal, Df, Activity.Rg, ActivityChange.Rg, 
        B5v, B6v, B7v, B8v, Asc4.Rg){
        # Calculate the total decrease. It is a proportion of the construction increase.
        Ndvq <- ConsVal * Df
        # Compute the proportions of Activity in each region
        ActProp.Rg <- proportion(Activity.Rg)
        # Transform ActivityChange.Rg so that the minimum value is zero
        ActivityChange.Rg <- ActivityChange.Rg - min(ActivityChange.Rg)
        # Before two model periods have occurred, there is no change in activity.
        # To avoid NaN error assign a value of one. Gives equal proportions with respect to this value
        if(all(ActivityChange.Rg == 0)) ActivityChange.Rg[] <- 1
        # Compute the proportions of the normalized activity change
        ActChangeProp.Rg <- proportion(ActivityChange.Rg)
        # Compute a utility function to use for proportioning the decrease among regions
        CobbDouglas.Rg <- (ActProp.Rg + B5v)^B6v * (ActChangeProp.Rg + B7v)^B8v * Asc4.Rg
        NdvqProp.Rg <- proportion(CobbDouglas.Rg)
        # Apportion decrease among regions
        Ndvq.Rg <- Ndvq * NdvqProp.Rg
        Ndvq.Rg
        }


#Function to allocate floor space production in dollars into floorspace types
#----------------------------------------------------------------------------

#This function allocates regional floor space production in dollars for each region into floor space production value by floorspace type for each region.

#:Name: allocateFloorProd
#:Argument: Nivq - regionwide floor space production
#:Argument: Occupied.BzFx - quantity of occupied floor space by beta zone and floor space type
#:Argument: Prevq.AzFx - floor space quantity for the previous year by alpha zone and floor space type
#:Argument: Cost.Fx - construction cost by floor space type
#:Argument: Lq1 - Lamda coefficient for logit function
#:Argument: Bq1.Fx - Coefficients for utility function differentiated by floor space type
#:Argument: Bq2 - Coefficient for utility function
#:Argument: Asc.Fx - A vector of alternative specific constants for utility function by floor space type
#:Argument: AlphaBeta_ - a list of index vectors between Alpha zones, Beta zones, and regions
#:Argument: Region - name of a region
#:Result: Nivq_ - A list whose primary component (Nivq.Fx) is vector of floor space production in dollars to floor space types. The list also includes other information for diagnostic purposes

#::

    Funs_$allocateFloorProd <- function(Nivq.Rg, Occupied.BzFx, Prevq.AzFx, Cost.Fx, Price.AzFx,
        Lq1, Bq1.Fx, Bq2, Asc1.Fx, AlphaBeta_, Region){
        # Extract the portions of Nivq.Rg, Occupied.BzFx and Prevq.AzFx for the region
        Nivq <- Nivq.Rg[Region]
        Occupied.BxFx <- Occupied.BzFx[unique(AlphaBeta_$BetaZone.Az[AlphaBeta_$Region.Az == Region]),]
        Prevq.AxFx <- Prevq.AzFx[AlphaBeta_$Region.Az == Region,]
        Price.AxFx <- Price.AzFx[AlphaBeta_$Region.Az == Region,]
        # Make the scalar Bq2 coefficient into a vector the length of the number of floorspace types
        Bq2.Fx <- rep(Bq2, length(Asc1.Fx))
        # Calculate average vacancy rates by floorspace type
        TotalSpace.Fx <- colSums(Prevq.AxFx)
        TotalSpace.Fx[TotalSpace.Fx == 0] <- 0.001
        Occupied.Fx <- colSums(Occupied.BxFx)
        Vacancy.Fx <- (TotalSpace.Fx - Occupied.Fx) / TotalSpace.Fx
        # Calculate the value of the total floorspace
        Tvq.Fx <- TotalSpace.Fx * Cost.Fx
        # Calculate the value of the total Floorspace based on PI's assessed Value, not the cost to construct (AB - 10-12-10)  
        Tvq.Fx <- TotalSpace.Fx * colSums(Prevq.AxFx*Price.AxFx)/colSums(Prevq.AxFx)/1000000
        # Calculate the utility functions for floorspace categories
        Util.Fx <- Bq1.Fx * Vacancy.Fx + Bq2.Fx * log(Tvq.Fx/sum(Tvq.Fx)) + Asc1.Fx
        # Create a list to hold the results
        Nivq_ <- list()
        # Allocate floorspace $ quantities among floorspace types
        Nivq_$Nivq.Fx <- Nivq * (exp(Lq1 * Util.Fx)/sum(exp(Lq1 * Util.Fx)))
        # Add the vacancy calculation for diagnostics
        Nivq_$Vacancy.Fx <- Vacancy.Fx
        # Add the utility calculation for diagnostics
        Nivq_$Util.Fx <- Util.Fx
        # The result if a vector of floorspace quantity values by floorspace type
        Nivq_
        }


#Function to allocate value of regional floor space decreases in dollars into floor space types
#----------------------------------------------------------------------------------------------

#This function allocates regional floor space decrease in dollars into decrease by floor space types.

#:Name: allocateFloorDecrease
#:Argument: Ndvq - regionwide floor space decrease
#:Argument: Occupied.BzFx - floor space occupancy by beta zone and floor space type
#:Argument: Prevq.AzFx - floor space quantity by alpha zone and floor space type
#:Argument: Cost.Fx - construction cost by floor space type
#:Argument: Lq2 - Lamda coefficient for logit function
#:Argument: Bq3.Fx - Coefficients for utility function differentiated by floor space type
#:Argument: Bq4 - Coefficient for utility function
#:Argument: Asc2.Fx - A vector of alternative specific constants for utility function by floor space type
#:Argument: AlphaBeta_ - a list of index vectors between Alpha zones, Beta zones, and regions
#:Argument: Region - name of a region
#:Result: Ndvq_ - A list whose primary component (Ndvq.Fx) is vector of floor space decrease in dollars to floor space types. The list also includes other information for diagnostic purposes

#::

    Funs_$allocateFloorDecrease <- function(Ndvq.Rg, Occupied.BzFx, Prevq.AzFx, Cost.Fx, Lq2, Bq3.Fx,
        Bq4, Asc2.Fx, AlphaBeta_, Region){
        # Extract the portions of Ndvq.Rg, Occupied.BzFx and Prevq.AzFx for the region
        Ndvq <- Ndvq.Rg[Region]
        Occupied.BxFx <- Occupied.BzFx[unique(AlphaBeta_$BetaZone.Az[AlphaBeta_$Region.Az == Region]),]
        Prevq.AxFx <- Prevq.AzFx[AlphaBeta_$Region.Az == Region,]
        # Make the scalar Bq2 coefficient into a vector the length of the number of floorspace types
        Bq4.Fx <- rep(Bq4, length(Asc2.Fx))
        # Calculate average vacancy rates by floorspace type
        TotalSpace.Fx <- colSums(Prevq.AxFx)
        TotalSpace.Fx[ TotalSpace.Fx == 0 ] <- 0.001
        Occupied.Fx <- colSums(Occupied.BxFx)
        Vacancy.Fx <- (TotalSpace.Fx - Occupied.Fx) / TotalSpace.Fx
        # Calculate the value of the total floorspace
        Tvq.Fx <- TotalSpace.Fx * Cost.Fx
        # Calculate the utility functions for floorspace categories
        Util.Fx <- Bq3.Fx * Vacancy.Fx + Bq4.Fx * log(Tvq.Fx/sum(Tvq.Fx)) + Asc2.Fx
        # Create a list to hold the results
        Ndvq_ <- list()
        # Allocate floorspace $ quantities among floorspace types
        Ndvq_$Ndvq.Fx <- Ndvq * (exp(Lq2 * Util.Fx)/sum(exp(Lq2 * Util.Fx)))
        # Add the vacancy calcualtion for diagnostics
        Ndvq_$Vacancy.Fx <- Vacancy.Fx
        # Add the utility calculation for diagnostics
        Ndvq_$Util.Fx <- Util.Fx
        # The result if a vector of floorspace quantity values by floorspace type
        Ndvq_
        }


#Function to calculate floor space capacity of each type by alpha zone
#---------------------------------------------------------------------

#This function calculates floor space capacity of each type of floor space for each alpha zone. The results are used in the allocation of floor space increments to alpha zones in each region.

#:Name: calcFloorCapacity
#:Argument: LandSqft.Fd - modelwide land area devoted to each developed floor space type (fixed)
#:Argument: Compat.FdZc - ratings of compatibility of developed floor space types (excludes agriculture and forestry) with zoning categories
#:Argument: Zoning.AzZc - amount of land in millions of square feet zoned in each zoning category in each Alpha zone
#:Argument: Far.FdZc - allowable floor area ratios for developed floor space types for each zoning category
#:Argument: Lp - dispersion parameter of the land proportion utility
#:Argument: Stc - parameter for the sensitivity to the size term in the land proportion utility
#:Argument: FloorDef_ - a list of floor space type definitions
#:Result: FloorCap_ - a list whose primary components are ResCap.AzFr and NresCap.AzFn. These matrices contain the floor space capacities by Alpha zone for residential and nonresidential floor space types respectively.  The list also includes other information for diagnostic purposes

#::
                     
    Funs_$calcFloorCapacity <- function(LandSqft.Fd, Compat.FdZc, Zoning.AzZc, Far.FdZc, Lp, Stc,
        FloorDef_){
        # Calculate the proportion of the land area of each zoning category
        # allocated to each floorspace type
        #Size.Fd <- log(LandSqft.Fd) / Lp   # AB 7-22-11
        CompatUtil.FdZc <- Lp * (log(Compat.FdZc) %col+% (Stc * log(LandSqft.Fd))) # AB 7-22-11
        ExpCompatUtil.FdZc <- exp(CompatUtil.FdZc)
        LandProp.FdZc <- ExpCompatUtil.FdZc %row/% colSums(ExpCompatUtil.FdZc)
        # Make sure that where a zone allows no floorspace, that none will be allocated
        # Find out zone types where compatibility is NP for all floorspace types
          #NoneAllowed.Zc <- colSums(CompatUtil.FdZc) < 1e-48  # AB 8-11-10
          #LandProp.FdZc[,NoneAllowed.Zc] <- 0 # AB 8-11-10
        LandProp.FdZc[is.nan(LandProp.FdZc)] <- 0 # AB 8-11-10 
        # Define a function for allocating land area to floorspace types for an individual zone
        allocateLandArea <- function(ZoneArea.Zc, LandProp.FdZc){
            rowSums(LandProp.FdZc %row*% ZoneArea.Zc)
            }
        # Apply the function to calculate land area by floorspace type and alpha zone
        # This is done for diagnostic purposes only as results are not used in further calculations
        LandArea.AzFd <- t(apply(Zoning.AzZc, 1, allocateLandArea, LandProp.FdZc))
        # Define a function for allocating floorspace capacity for an individual zone
        allocateCapacity <- function(ZoneArea.Zc, LandProp.FdZc, Far.FdZc){
            LandCap.FdZc <- LandProp.FdZc %row*% ZoneArea.Zc
            FloorCap.Fd <- rowSums(LandCap.FdZc * Far.FdZc)
            FloorCap.Fd
            }
        # Apply the function to zoning.az
        Cap.AzFd <- t(apply(Zoning.AzZc, 1, allocateCapacity, LandProp.FdZc, Far.FdZc))
        FloorCap_ <- list()
        FloorCap_$ResCap.AzFr <- Cap.AzFd[,Fr]
        FloorCap_$NresCap.AzFn <- Cap.AzFd[,Fn]
        # Add the diagnostics
        FloorCap_$LandProp.FdZc <- LandProp.FdZc
        FloorCap_$LandArea.AzFd <- LandArea.AzFd
        # Return the results
        FloorCap_
        }


#Function to allocate regional floor space decreases to Alpha zones
#------------------------------------------------------------------

#This function allocates regional decreases in floor space by type to Alpha zones. The function operates on either residential or nonresidential floor space types.

#:Name: calculateFloorDecrease
#:Argument: Quant.AzFx - matrix containing the existing quantity of floor space by type and Alpha zone
#:Argument: Ndq.Fx - the decrease in floor space by floor space type for the region
#:Argument: Price.AzFx - matrix of floor space prices by Alpha zone and floor space type
#:Argument: B3f - coefficient in Cobb-Douglas equation
#:Argument: B4f - coefficient in Cobb-Douglas equation
#:Argument: AlphaBeta_ - a list of index vectors between Alpha zones, Beta zones, and regions
#:Argument: Region - name of a region
#:Result: DQ_ - a list whose primary component is a matrix of decreases in floor space by Alpha zone and floor space type. The list also includes the Cobb-Douglas values used to calculate proportions

#::

    Funs_$calculateFloorDecrease  <- function(Quant.AzFx, Ndq.Fx, Price.AzFx, 
        B3f, B4f, AlphaBeta_, Region){
        # Extract the portions of Quant.AzFx, Price.AzFx for the region
        Quant.AxFx <- Quant.AzFx[AlphaBeta_$Region.Az == Region,]
        # Check if quantity in all zones for a type is zero
        # If so, put in a small amount
        Quant.Fx <- colSums(Quant.AxFx)
        if(any(Quant.Fx == 0)){
          SmallAdd.Fx <- numeric(ncol(Quant.AxFx))
          SmallAdd.Fx[Quant.Fx == 0] <- 0.001 / nrow(Quant.AxFx)
          Quant.AxFx <- sweep(Quant.AxFx, 2, SmallAdd.Fx, "+")
        }
        Price.AxFx <- Price.AzFx[AlphaBeta_$Region.Az == Region,]
        # Calculate the zonal proportions of floorspace
        PrevProp.AxFx <- Quant.AxFx %row/% colSums(Quant.AxFx)
        # Calculate the Cobb-Douglas function for decreasing floorspace by zone and floorspace type
        A.AxFx <- PrevProp.AxFx^B3f * Price.AxFx^B4f
        # Calculate the decrease proportions
        DProp.AxFx <- A.AxFx %row/% colSums(A.AxFx)
        # Allocate floorspace decrease by Alpha zone
        DQ.AxFx <- DProp.AxFx %row*% Ndq.Fx
        # The decrease cannot be any greater than the quantity in the zone
        QuantMinusDQ.AxFx <- Quant.AxFx - DQ.AxFx
        IsTooMuchDQ.AxFx <- QuantMinusDQ.AxFx < 0
        DQ.AxFx[IsTooMuchDQ.AxFx] <- Quant.AxFx[IsTooMuchDQ.AxFx]
        # Save the unallocated decreases to subtract from the regional increases later
        # so that increase and decreases total up properly
        UnallocatedDQ.AxFx <- QuantMinusDQ.AxFx
        UnallocatedDQ.AxFx[!IsTooMuchDQ.AxFx] <- 0
        UnallocatedDQ.Fx <- colSums(UnallocatedDQ.AxFx)
        # Make list to hold the results
        DQ_ <- list()
        DQ_$A.AxFx <- A.AxFx
        DQ_$DQ.AxFx <- DQ.AxFx
        DQ_$QuantMinusDQ.AxFx <- QuantMinusDQ.AxFx
        DQ_$IsTooMuchDQ.AxFx <- IsTooMuchDQ.AxFx
        DQ_$UnallocatedDQ.Fx <- UnallocatedDQ.Fx
        DQ_
        }
    

#Function to calculate floors pace increases
#-------------------------------------------

#This function allocates regional increases in floor space by type to Alpha zones. The function operates on either residential or nonresidential floor space types.

#:Name: allocateFloorIncrease
#:Argument: Cap.AzFx - matrix of floor space capacities by alpha zone and floor space type
#:Argument: Niq.Fx - vector of floor space increases (in square feet) by floor space type for a region
#:Argument: Quant.AzFx - matrix of floor space quantities by floor space type and alpha zone
#:Argument: DQ.AxFx - a matrix of floor space decreases by floor space type and alpha zone for a region
#:Argument: Price.AzFx - a matrix of floor space prices by floor space type and alpha zone
#:Argument: B5f - coefficient in the Cobb-Douglas equation
#:Argument: B6f - coefficient in the Cobb-Douglas equation
#:Argument: B7f - coefficient in the Cobb-Douglas equation
#:Argument: B8f - coefficient in the Cobb-Douglas equation
#:Argument: B9f - coefficient in the Cobb-Douglas equation
#:Argument: AlphaBeta_ - zone crosswalk list
#:Argument: Region - name of a region
#:Result: IQ.af - list that includes matrix of increase in floor space by alpha zone and floor space type and diagnostic information

#::

    Funs_$allocateIncrease <- function(Cap.AzFx, Niq.Fx, Quant.AzFx, DQ.AzFx, UnallocatedDQ.Fx,
		Price.AzFx, B5f, B6f, B7f, B8f, B9f, AlphaBeta_, Region){
        # Extract the portions of Quant.AzFx, DQ.AzFx, UnallocatedDQ.AxFx, Price.AzFx for the region
        Quant.AxFx <- Quant.AzFx[AlphaBeta_$Region.Az == Region,]
        # Check if quantity in all zones for a type is zero
        # If so, put in a small amount
        Quant.Fx <- colSums(Quant.AxFx)
        if(any(Quant.Fx == 0)){
          SmallAdd.Fx <- numeric(ncol(Quant.AxFx))
          SmallAdd.Fx[Quant.Fx == 0] <- 0.001 / nrow(Quant.AxFx)
          Quant.AxFx <- sweep(Quant.AxFx, 2, SmallAdd.Fx, "+")
        }
        DQ.AxFx <- DQ.AzFx[AlphaBeta_$Region.Az == Region,]
        Price.AxFx <- Price.AzFx[AlphaBeta_$Region.Az == Region,]
        Cap.AxFx <- Cap.AzFx[AlphaBeta_$Region.Az == Region,]
        # Calculate the zonal proportions of floorspace
        PrevProp.AxFx <- Quant.AxFx %row/% colSums(Quant.AxFx)
        # Subtract out the decrease
        PrevDQ.AxFx <- Quant.AxFx - DQ.AxFx
        # Check if the capacity is less than the quantity and set to the quantity is so
        # AB - Commented out 8-11-10, because it is believed that these lines are allowing quantitiy to creep up over time.
         #IsLowCapacity.AxFx <- Quant.AxFx > Cap.AxFx
         #Cap.AxFx[IsLowCapacity.AxFx] <- Quant.AxFx[IsLowCapacity.AxFx]
        # Ratio of floorspace to zonal capacity of floorspace
        #PropCap.AxFx <- PrevDQ.AxFx / Cap.AxFx # ratio of floorspace to zonal capacity of floorspace 
        PropCap.AxFx <- Quant.AxFx / Cap.AxFx # Calculate capacity based on Last Years Quantitiy AB - 9-24-10      
        # AB - Adding hard capacity limits
        IsLowCapacity.AxFx <- is.nan(PropCap.AxFx) | is.infinite(PropCap.AxFx) | PropCap.AxFx >= 1 # AB- 8-11-10
        PropCap.AxFx[IsLowCapacity.AxFx] = 1  # AB- 8-11-10
        # PropCap.AxFx[is.nan(PropCap.AxFx) | is.infinite(PropCap.AxFx)] <- 0 # AB - old line prior to 8-11-10
   	    B.AxFx <- (PrevProp.AxFx + B9f)^B5f * Price.AxFx^B6f * (1 - B7f * PropCap.AxFx^B8f)
        B.AxFx[IsLowCapacity.AxFx] = 0  # AB- 8-11-10
        # Make sure that B.AxFx is not negative
        B.AxFx[B.AxFx < 0] <- 0
        # Calculate the proportion of the increase going to each zone for each floorspace type
        IQProp.AxFx <- B.AxFx %row/% colSums(B.AxFx)
         # Calculate the increase in each zone and floorspace type
         # add the unallocated decreases (they are negative numbers)
        IQ.AxFx <- IQProp.AxFx %row*% (Niq.Fx + UnallocatedDQ.Fx)
        # Add the increases to the previous quantities (inc. decreases) to get new quantities
        CurrQ.AxFx <- PrevDQ.AxFx + IQ.AxFx
        CurrQ.AxFx[CurrQ.AxFx < 0] <- 0
        # Make a list to hold results to be returned from function
        Q_ <- list()
        Q_$PrevDQ.AxFx <- PrevDQ.AxFx
        Q_$IsLowCapacity.AxFx <- IsLowCapacity.AxFx
        Q_$PropCap.AxFx <- PropCap.AxFx
        Q_$B.AxFx <- B.AxFx
        Q_$IQ.AxFx <- IQ.AxFx
        Q_$CurrQ.AxFx <- CurrQ.AxFx
        Q_
        }
