# Knit the reference scenario
# Usage: Rscript swimr_render_rmd.R <template> <current database path> <reference database path> <third database path>
# 19 Mar 2019  matt.landis@rsginc.com

# <template> argument can be 'reference' or 'compare'

args <- commandArgs()  # Use argparse package for more flexibility

# Get location of executable and use only the library in that installation
exe <- args[1]
libPath <- gsub("bin\\\\x64\\\\Rterm.exe", "library", exe)
.libPaths(libPath)

# For testing
# cat("\n-----Args------\n")
# cat(paste(args, collapse="\n"))
# cat("\n\nReference arg:", which(args == "Reference"), "\n")
# cat("\nlibPaths:", .libPaths(), '\n')


# Load libraries --------------------------------------------------------------

pandoc_exe <- args[7]
cat("pandoc_exe:", pandoc_exe, "\n")

Sys.setenv(RSTUDIO_PANDOC = dirname(pandoc_exe))
#Sys.getenv("RSTUDIO_PANDOC")

library('rmarkdown')
rmarkdown:::find_pandoc()

library('RSQLite')
# cat('rmarkdown thinks pandoc is here:', rmarkdown:::.pandoc$dir, "\n")

# Define functions ------------------------------------------------------------


# Main -----------------------------------------------------------------------

template <- args[6]

if ( template == 'Reference' ){

  ref_db = args[8]

  template_folder <- 'single_scenario'

  output_file <- file.path(dirname(ref_db), 'swimr_reference_scenario.html')
  param_list <- list(ref_db = ref_db)

} else if ( template == 'Compare' ){

  current_db = args[8]
  ref_db = args[9]

  template_folder <- 'compare_scenario'

  output_file <- file.path(dirname(current_db), 'swimr_compare_scenarios.html')
  param_list <- list(current_db =current_db, ref_db = ref_db)

} else if ( template == 'Population' ){

  current_db = args[8]
  ref_db = args[9]
  compare_db = args[10]

  template_folder <- 'population'

  output_file <- file.path(dirname(current_db), 'swimr_population.html')
  param_list <- list(ref_db = ref_db,
                 current_db = current_db,
                 compare_db = compare_db)

} else {
  stop("Template '", template, "' is unknown.  Use 'Reference', 'Compare', or 'Population'")
}

rmd_file <- system.file("rmarkdown", "templates", template_folder,
                        "skeleton", "skeleton.Rmd", package="swimr")

rmarkdown::render(input = rmd_file, output_file = output_file,
                  params = param_list, envir=new.env())
