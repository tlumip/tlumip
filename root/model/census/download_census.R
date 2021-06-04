# Define census regions and links
census_regions <- c("pwa", "por", "pnv", "pid", "pca", "hwa", "hor", "hnv", "hid", "hca")
census_url <- "http://www2.census.gov/programs-surveys/acs/data/pums/2009/5-Year/csv_xxx.zip"

# Download files for 2009
for(region in census_regions){
  region_url <- gsub("xxx", region, census_url)
  cat(paste0("Downloading \'", region, "\' from ", region_url), "\n")
  region_zip_file <- basename(region_url)
  download.file(region_url, region_zip_file)
sevenzip_command <- paste0("..\\lib\\7za.exe x ", 
                           normalizePath(file.path(getwd(),region_zip_file)),
                           " -o\"", normalizePath(getwd()),"\" -y")
  system(sevenzip_command)
}

# Download files for 2017
census_url <- "http://www2.census.gov/programs-surveys/acs/data/pums/2017/5-Year/csv_xxx.zip"

for(region in census_regions){
  region_url <- gsub("xxx", region, census_url)
  cat(paste0("Downloading \'", region, "\' from ", region_url), "\n")
  region_zip_file <- basename(region_url)
  download.file(region_url, region_zip_file)
sevenzip_command <- paste0("..\\lib\\7za.exe x ", 
                           normalizePath(file.path(getwd(),region_zip_file)),
                           " -o\"", normalizePath(getwd()),"\" -y")
  system(sevenzip_command)
}

# Remove pdf files
file.remove(list.files(pattern = ".*pdf$", full.names = TRUE))
# Remove zip files
file.remove(list.files(pattern = ".*zip$", full.names = TRUE))