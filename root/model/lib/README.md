# Model software
Different pieces of the model use R, Python, and Java. We bundle the appropriate
version of each piece of software with the model in this folder.

Upon installation, extract the files in `dependencies64.zip`. This should leave
you with the following directories:

  - `java7`
  - `Python27`
  - `R-3.1.2`

`7za.exe` is included directly in the repository and does not need to be
unzipped.

## R packages
Four R packages are included with the model; these should be installed from the
zipped source files and not from CRAN for reproducibility.

  1. Start RGui in `.\R-3.1.2\bin\x64\RGui.exe`
  2. Choose "Packages -> Install package(s) from local zip files".
  3. Choose the following packages from `.\`:
    - iterators_1.0.7
    - foreach_1.4.2
    - doParallel_1.0.8
    - RSQLite_1.0.0
