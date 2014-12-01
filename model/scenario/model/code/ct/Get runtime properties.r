# @tag{Setup CT's runtime parameters by parsing SWIM properties file}
# @author{Rick Donnelly} @date{18-Nov-2014} @version{0.8}
# We will need to start CT by reading the SWIM2 runtime properties file, which
# should be passed as a command line parameter when starting CT. This will give
# us the paths and program parameters we need to operate properly.

# It's important to note that all of the parameters are strings, and need to be
# converted to numeric or integer format, as appropriate, where they are used in
# the code. It would be nice to add logic for doing so here, but will leave that
# for another time.

set_CT_runtime_parameters <- function(swim2_properties_filename) {
    require(stringr)
    
    # We only want to keep a few of the AO properties and all CT parameters. The
    # SWIM2 runtime parameters file is not hierarchical, so we will have to 
    # search for what we want by prefix name.
    SP1 <- scan(file=swim2_properties_filename, what="character", sep='\n',
        comment.char='#', quote=NULL)
    SP2 <- str_replace(noquote(SP1), '\t', "")   # Remove embedded tabs
    
    # Split each record into token and value by using the equals sign to split
    # between them. Remember that str_locate returns two values: the first and
    # last occurrences of the character in the string. We only need the first
    # number in this case, which is stored in the first column.
    parameters <- data.frame(token = str_trim(str_sub(SP2, 1, str_locate(SP2, "=")[,1]-1)))
    parameters$value <- str_trim(str_sub(SP2, str_locate(SP2, "=")[,1]+1, str_length(SP2)))
    
    # Retain several important Application Orchestrator (AO) properties
    keep <- c("root.dir", "scenario.name", "base.year", "t.year", "t.year.prefix",
        "scenario.outputs", "alpha2beta.file.base")
    firstPass <- parameters[parameters$token %in% keep,]
    
    # Add in all fields with the "ct." prefix
    hasCtPrefix <- str_sub(parameters$token, 1, 3)=="ct."
    hasCtPrefix[is.na(hasCtPrefix)] <- FALSE
    secondPass <- parameters[hasCtPrefix==TRUE,]
    
    # Combine the global and CT-specific parameters into a single data frame and
    # then populate the runtime parameters with it
    combined <- rbind(firstPass, secondPass)
    RTP <- new.env()
    for (i in 1:nrow(combined)) {
        RTP[[as.character(combined$token[i])]] <- combined$value[i]
    }
    
    # We will save the RTP environment so that each successive module has
    # access to this information. This will require building the output path and
    # filename to store the parameters in. We don't want to have to recreate the
    # working folder pathname every time we use it, so store that shortcut.
    RTP[["Working_Folder"]] <- str_c(
        combined$value[combined$token=="scenario.outputs"], "/",
        combined$value[combined$token=="t.year.prefix"],
        combined$value[combined$token=="t.year"], "/"
    )
    properties_FN <- str_c(RTP[["Working_Folder"]], "ct-runtime-parameters.RData")
    print(str_c("Saving CT runtime properties to ", properties_FN), quote=FALSE)
    save(RTP, file=properties_FN)
    
    # Return the properties as well so that they are saved in the current run
    RTP
}
