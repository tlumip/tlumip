
for region in "pwa" "por" "pnv" "pid" "pca" "hwa" "hor" "hnv" "hid" "hca"
do
  wget -O ss09$region.zip "http://www2.census.gov/programs-surveys/acs/data/pums/2009/5-Year/csv_${region}.zip"
  unzip ss09$region.zip
  rm *.pdf
done

rm *zip
  
