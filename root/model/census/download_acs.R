setwd("c:/projects")

regions = c("pwa","por","pnv","pid","pca","hwa","hor","hnv","hid","hca")

for(region in regions) {
    print(region)
    
    #2009 5 year ACS PUMS
    url = paste0("http://www2.census.gov/programs-surveys/acs/data/pums/2009/5-Year/csv_", region, ".zip")
    outfile = paste0("ss09",region,".zip")
    download.file(url, outfile)
    unzip(outfile)
    file.remove("ACS2005-2009_PUMS_README.pdf")
    file.remove(outfile)
    
    #2017 5 year ACS PUMS
    url = paste0("http://www2.census.gov/programs-surveys/acs/data/pums/2017/5-Year/csv_", region, ".zip")
    outfile = paste0("ss17",region,".zip")
    download.file(url, outfile)
    unzip(outfile)
    file.remove("ACS2013_2017_PUMS_README.pdf")
    file.remove(outfile)
}