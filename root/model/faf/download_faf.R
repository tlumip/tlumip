# Define FAF file name and links
faf_name="FAF5.5.1"
faf_url= "https://faf.ornl.gov/faf5/data/download_files/xxx.zip"
#faf_url= "https://www.bts.gov/sites/bts.dot.gov/files/legacy/AdditionalAttachmentFiles/xxx.zip"

# Download FAF file
faf_file_url <- gsub("xxx", faf_name, faf_url)
cat(paste0("Downloading \'", faf_name, "\' from ", faf_file_url), "\n")
faf_zip_file <- basename(faf_file_url)
download.file(faf_file_url, faf_zip_file)
sevenzip_command <- paste0("..\\lib\\7za.exe x ",
                           normalizePath(file.path(getwd(),faf_zip_file)),
                           " -o\"", normalizePath(getwd()),"\" -y")
system(sevenzip_command)

# Remove zip files
file.remove(list.files(pattern = ".*zip$", full.names = TRUE))