# Knit the reference scenario
# Usage: Rscript swimr_render_rmd.R <template> <current database path> <reference database path> <third database path>
# 19 Mar 2019  matt.landis@rsginc.com

# <template> argument can be 'reference' or 'compare'

# Load libraries --------------------------------------------------------------
library('rmarkdown')

# Define functions ------------------------------------------------------------

# # read.properties function is modified from the properties package
# read_properties <- function (file, fields = NULL, encoding = "UTF-8"){
#     if (is.character(file)) {
#         file <- file(file, "r", encoding = encoding)
#         on.exit(close(file))
#     }
#     if (!inherits(file, "connection"))
#         stop("'file' must be a character string or connection")
#     lines <- readLines(file)
#     commentedLines <- grepl("^#.*$", lines)
#     lines <- lines[!commentedLines]
#     line_is_not_empty <- !grepl("^[[:space:]]*$", lines)
#     lines <- lines[line_is_not_empty]
#     line_has_tag <- grepl("^[^[:blank:]][^=:]*=", lines)
#     ind <- which(!line_has_tag)
#     if (length(ind)) {
#         lines <- strtrim(lines[ind], 0.7 * getOption("width"))
#         stop("Invalid Java properties file format.\nContinuation lines must not start a record.\nOffending lines start with:\n%s",
#             paste("  ", lines, sep = "", collapse = "\n"))
#     }
#     keys <- gsub("^([^= ]+) *= *[^ ].*$", "\\1", lines)
#     values <- gsub("^[^= ]+ *= *([^ ].*)$", "\\1", lines)
#     names(values) <- keys
#     out <- as.list(values)
#     out <- if (!is.null(fields))
#         out[fields]
#     else out
#     return(out)
# }


# Main -----------------------------------------------------------------------

args <- commandArgs(trailingOnly = TRUE)  # Use argparse package for more flexibility
template <- args[1]

if ( template == 'Reference' ){

  ref_db = args[2]

  template_folder <- 'single_scenario'

  output_file <- file.path(dirname(ref_db), 'swimr_reference_scenario.html')
  param_list <- list(ref_db = ref_db)

} else if ( template == 'Compare' ){

  current_db = args[2]
  ref_db = args[3]

  template_folder <- 'compare_scenario'

  output_file <- file.path(dirname(current_db), 'swimr_compare_scenarios.html')
  param_list <- list(current_db =current_db, ref_db = ref_db)

} else if ( template == 'Population' ){

  current_db = args[2]
  ref_db = args[3]
  compare_db = args[4]

  template_folder <- 'population'

  output_file <- file.path(dirname(ref_db), 'swimr_population.html')
  param_list <- list(ref_db = ref_db,
                 current_db = current_db,
                 compare_db = compare_db)

} else {
  stop("Template '", template, "' is unknown.  Use 'Reference' or 'Compare'")
}

rmd_file <- system.file("rmarkdown", "templates", template_folder,
                        "skeleton", "skeleton.Rmd", package="swimr")

rmarkdown::render(input = rmd_file, output_file = output_file,
                  params = param_list, envir=new.env())
