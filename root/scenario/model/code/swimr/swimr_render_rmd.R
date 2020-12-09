# Knit the reference scenario
# Usage: Rscript swimr_render_rmd.R <template> <pandoc location> <current database path> <reference database path> <third database path>
# 19 Mar 2019  matt.landis@rsginc.com

# <template> argument can be 'reference' or 'compare'

args = commandArgs()  # Use argparse package for more flexibility

# # For testing
# # Reference
# args = c(
#   'root/model/lib/R-3.3.2/bin/x64/Rterm.exe',
#   '--no-restore',
#   '--no-save',
#   '--file=root/scenario/model/code/swimr/swimr_render_rmd.R',
#   'Reference',
#   'root/model/lib/pandoc/pandoc.exe',
#   'D:/Projects/Clients/OR_DOT/SWIM_WOC7/Data/FromClient/Database/Ref26_NoFB.db'
# )

# Compare
args = c(
  'root/model/lib/R-3.3.2/bin/x64/Rterm.exe',
  '--no-restore',
  '--no-save',
  '--file=root/scenario/model/code/swimr/swimr_render_rmd.R',
  'Compare',
  'root/model/lib/pandoc/pandoc.exe',
  'D:/Projects/Clients/OR_DOT/SWIM_WOC7/Data/FromClient/Database/Ref26_NoFB.db',
  'D:/Projects/Clients/OR_DOT/SWIM_WOC7/Data/FromClient/Database/Ref26_RRA2_FB_half.db'
)

# # Population
# args = c(
#   'root/model/lib/R-3.3.2/bin/x64/Rterm.exe',
#   '--no-restore',
#   '--no-save',
#   '--file=root/scenario/model/code/swimr/swimr_render_rmd.R',
#   'Population',
#   'root/model/lib/pandoc/pandoc.exe',
#   'D:/Projects/Clients/OR_DOT/SWIM_WOC7/Data/FromClient/Database/Ref26_NoFB.db',
#   'D:/Projects/Clients/OR_DOT/SWIM_WOC7/Data/FromClient/Database/Ref26_RRA2_FB_half.db',
#   'D:/Projects/Clients/OR_DOT/SWIM_WOC7/Data/FromClient/Database/Ref26_RRA2_FB_half.db'
# )

# Get location of executable and use only the library in that installation
exe <- args[1]
libPath <- gsub('bin[\\|/]x64[\\|/]R(term|script)?.exe', 'library', exe)
.libPaths(libPath)

file_arg = args[grepl(pattern='--file', x=args)]
file_path = gsub('--file=', '', file_arg)
this_dir = dirname(file_path)

# For testing
# cat("\n-----Args------\n")
# cat(paste(args, collapse="\n"))
# cat("\n\nReference arg:", which(args == "Reference"), "\n")
# cat("\nlibPaths:", .libPaths(), '\n')

# Load libraries --------------------------------------------------------------

pandoc_exe = args[grepl(pattern='pandoc[.]exe', args)]
cat("pandoc_exe:", pandoc_exe, "\n")

Sys.setenv(RSTUDIO_PANDOC = dirname(pandoc_exe))
#Sys.getenv("RSTUDIO_PANDOC")

library('rmarkdown')
# rmarkdown:::find_pandoc()
# cat('rmarkdown thinks pandoc is here:', rmarkdown:::.pandoc$dir, "\n")

library('RSQLite')

# Define functions ------------------------------------------------------------


# Main -----------------------------------------------------------------------

template <- args[grepl(pattern='(Reference|Compare|Population)', args)]

db_args = args[grepl(pattern='[.]db$', args)]

if ( template == 'Reference' ){

  ref_db = db_args[1]
  
  rmd_file = file.path(this_dir, 'single_scenario.Rmd')
  output_file <- file.path(dirname(ref_db), 'swimr_reference_scenario.html')
  param_list <- list(ref_db = ref_db)

} else if ( template == 'Compare' ){

  rmd_file = file.path(this_dir, 'compare_scenario.Rmd')
  current_db = db_args[1]
  ref_db = db_args[2]

  output_file <- file.path(dirname(current_db), 'swimr_compare_scenarios.html')
  param_list <- list(current_db =current_db, ref_db = ref_db)

} else if ( template == 'Population' ){

  rmd_file = file.path(this_dir, 'population.Rmd')
  current_db = db_args[1]
  ref_db = db_args[2]
  compare_db = db_args[3]

  output_file <- file.path(dirname(current_db), 'swimr_population.html')
  param_list <- list(ref_db = ref_db,
                     current_db = current_db,
                     compare_db = compare_db)

} else {
  stop("Template '", template, "' is unknown.  Use 'Reference', 'Compare', or 'Population'")
}

# Location of Rmd templates in SWIMR package (not used)
# rmd_file <- system.file("rmarkdown", "templates", template_folder,
#                         "skeleton", "skeleton.Rmd", package="swimr")

rmarkdown::render(input = rmd_file, output_file = output_file,
                  params = param_list, envir=new.env())
