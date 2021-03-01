export faf_name="FAF4.5.1_csv"
echo "Downloading ${faf_name}.zip file"
wget -O $faf_name.zip "https://www.bts.gov/sites/bts.dot.gov/files/legacy/AdditionalAttachmentFiles/${faf_name}.zip"
if [ $? -ne 0]
	then 
		echo "${faf_name}.zip not found. Please visit https://www.bts.gov/faf/ to download the latest FAF file and update the globalTemplate.properties accordingly."
	else 
		unzip $faf_name.zip
		rm *zip
fi