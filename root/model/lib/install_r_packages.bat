set packages=iterators_1.0.7,foreach_1.4.2,doParallel_1.0.8,RSQLite_1.0.0

for %%x in (%packages%) do (
  R-3.1.2\bin\x64\R.exe CMD INSTALL %%x.zip
)
