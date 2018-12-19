#error check select link output
#Nagendra Dhakar, nagendra.dhakar@rsginc.com, 10/18/18

#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at

#    http://www.apache.org/licenses/LICENSE-2.0

#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.

#Run from the tstep year outputs folder such as D:\swim2\scenario_16\outputs\t23
#D:/swim2/model/lib/Python27/python.exe ..\..\model\code\visum\check_select_link_output.py

import os, sys, shutil
import pandas as pd
import numpy as np
import zipfile
import warnings
from Properties import Properties
from pandas.api.types import is_string_dtype

#do not print warnings
warnings.filterwarnings("ignore")

#load properties
if len(sys.argv) < 2:
    print(len(sys.argv))
    print(sys.argv)
    print("missing arguments!")
    sys.exit(1)

property_file = sys.argv[1]
properties = Properties()
properties.loadPropertyFile(property_file)

out_zip_file = properties['sl.output.bundle.file']
output_folder = os.path.dirname(out_zip_file)

#trip files
trips_sdt_file = properties['sdt.person.trips']
trips_ldt_file = properties['ldt.person.trips']
trips_ct_file = properties['ct.truck.trips']
trips_et_file = properties['et.truck.trips']

#time of day
am_peak_start = int(properties['am.peak.start'])
am_peak_end = int(properties['am.peak.end'])
md_offpeak_end = int(properties['md.offpeak.end'])
pm_peak_end = int(properties['pm.peak.end'])

auto_classes = properties['sl.auto.classes'].split(",") #am, md, pm, ni
truck_classes = properties['sl.truck.classes'].split(",") #am, md, pm, ni

select_link_file = properties['sl.output.file.select.link.results'] #only file name - no full file path
select_link_review_file = properties['sl.output.file.select.link.review'] #only file name - no full file path

def unzip_output():
    basename = os.path.basename(out_zip_file)
    out_directory = basename.split('.')[0]
    extracted_directory = os.path.join(output_folder, out_directory)

    #unzip files
    zip_ref = zipfile.ZipFile(out_zip_file,'r')
    zip_ref.extractall(extracted_directory)
    zip_ref.close()

    return(extracted_directory)

def read_file(infile,data_folder):
    select_link_file = os.path.basename(infile).split('.')[0] + '_select_link.csv'
    select_link_file_path = os.path.join(data_folder, select_link_file)

    mydata = pd.read_csv(select_link_file_path)

    return(mydata)

def determine_assign_class(mydata, field_time, mode_classes):
    #create ASSIGNCLASS field
    print('determine assignment class')
    mydata['ASSIGNCLASS'] = '' #default - none
    mydata.ASSIGNCLASS[mydata[field_time] < am_peak_start] = mode_classes[3] #ni off peak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < am_peak_end)] = mode_classes[0]  #am peak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < md_offpeak_end)] = mode_classes[1]  #md offpeak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < pm_peak_end)] = mode_classes[2]  #pm peak
    mydata.ASSIGNCLASS[mydata['ASSIGNCLASS'] == ''] = mode_classes[3] #ni offpeak

    #time period
    mydata['ASSIGNCLASS_NEW'] = ''
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_peak')] = 'peak'
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_offpeak')] = 'offpeak'
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_ni')] = 'ni'
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_pm')] = 'pm'

    return(mydata)

def determine_direction(mydata):
    print('determine direction')
    mydata['DIRECTION'] = ''
    
    if is_string_dtype(mydata['EXTERNAL_ZONE_ORIGIN']):
        mydata.DIRECTION[mydata['EXTERNAL_ZONE_ORIGIN'].str.contains('_')] ='IN'

    if is_string_dtype(mydata['EXTERNAL_ZONE_DESTINATION']):    
        mydata.DIRECTION[mydata['EXTERNAL_ZONE_DESTINATION'].str.contains('_')] = 'OUT'
        
    if (is_string_dtype(mydata['EXTERNAL_ZONE_ORIGIN'])) & (is_string_dtype(mydata['EXTERNAL_ZONE_DESTINATION'])):    
        mydata.DIRECTION[(mydata['EXTERNAL_ZONE_ORIGIN'].str.contains('_')) & (mydata['EXTERNAL_ZONE_DESTINATION'].str.contains('_'))] = 'EXT'


    mydata['STATIONNUMBER'] = ''
    mydata.STATIONNUMBER[mydata['DIRECTION']=='IN'] =  mydata.EXTERNAL_ZONE_ORIGIN[mydata['DIRECTION']=='IN']
    mydata.STATIONNUMBER[mydata['DIRECTION']=='OUT'] = mydata.EXTERNAL_ZONE_DESTINATION[mydata['DIRECTION']=='OUT']
    mydata.STATIONNUMBER[mydata['DIRECTION']=='EXT'] = mydata.EXTERNAL_ZONE_DESTINATION[mydata['DIRECTION']=='EXT']
    
    return(mydata)

def review_data(data_dir):
    #read SL outputs
    sdt = read_file(trips_sdt_file,data_dir)
    ldt = read_file(trips_ldt_file,data_dir)
    ct = read_file(trips_ct_file,data_dir)
    et = read_file(trips_et_file,data_dir)

    sdt = determine_assign_class(sdt,"tripStartTime",auto_classes)
    ldt = determine_assign_class(ldt,"tripStartTime",auto_classes)
    ct = determine_assign_class(ct,"tripStartTime",truck_classes)
    et = determine_assign_class(et,"tripStartTime",truck_classes)

    sdt = determine_direction(sdt)
    ldt = determine_direction(ldt)
    ct = determine_direction(ct)
    et = determine_direction(et)

    sdt = sdt[["origin","destination","DIRECTION","ASSIGNCLASS","SELECT_LINK_PERCENT","STATIONNUMBER"]]
    ldt = ldt[["origin","destination","DIRECTION","ASSIGNCLASS","SELECT_LINK_PERCENT","STATIONNUMBER"]]
    ct = ct[["origin","destination","DIRECTION","ASSIGNCLASS","SELECT_LINK_PERCENT","STATIONNUMBER"]]
    et = et[["origin","destination","DIRECTION","ASSIGNCLASS","SELECT_LINK_PERCENT","STATIONNUMBER"]]

    auto = sdt.append(ldt, ignore_index=True)
    truck = ct.append(et, ignore_index=True)

    mydata = auto[truck.columns].append(truck, ignore_index=True)
        
    summary_temp = mydata.groupby(["ASSIGNCLASS","DIRECTION","origin","destination","STATIONNUMBER"]).mean()
    summary_temp = summary_temp.reset_index()

    summary = summary_temp.groupby(["ASSIGNCLASS","DIRECTION","origin","destination"]).sum()
    summary = summary.reset_index()

    #od_review = summary_temp[summary_temp['SELECT_LINK_PERCENT']<0.99]
    od_review = summary[summary['SELECT_LINK_PERCENT']<0.99]
    
    #return(summary)
    return(od_review)
    

'''
main function that appends select link data to trip files
'''
def main():

    #zip outputs
    print('Unzip select link outputs')
    data_dir = unzip_output()

    print('Review select link outputs')
    sl_review = review_data(data_dir)

    #write summary
    print('Write select link summary')
    sl_review.to_csv(os.path.join(output_folder, select_link_review_file), header=True, index=False)
    
    #remove the un-zipped folder
    print('Delete unzipped outputs')
    if os.path.isdir(data_dir):
        shutil.rmtree(data_dir)
    
if __name__ == "__main__":
    main()
