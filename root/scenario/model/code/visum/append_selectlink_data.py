#append select link data for single link
#Nagendra Dhakar, nagendra.dhakar@rsginc.com, 11/03/16

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
#D:/swim2/model/lib/Python27/python.exe ..\..\model\code\visum\append_selectlink_data.py

import os, sys
import pandas as pd
import numpy as np
import zipfile
import warnings
from Properties import Properties

warnings.filterwarnings("ignore")

#load properties
property_file = 'si.properties'
properties = Properties()
properties.loadPropertyFile(property_file)

out_zip_file = properties['sl.output.bundle.file']
output_folder = os.path.dirname(out_zip_file)

#time of day
am_peak_start = int(properties['am.peak.start'])
am_peak_end = int(properties['am.peak.end'])
md_offpeak_end = int(properties['md.offpeak.end'])
pm_peak_end = int(properties['pm.peak.end'])

select_link_file = properties['sl.output.file.select.link.results'] #only file name - no full file path

household_file = properties['sdt.household.data']
tours_sdt_file = properties['sdt.person.tours']
tours_ldt_file = properties['ldt.tours']
trips_sdt_file = properties['sdt.person.trips']
trips_ldt_file = properties['ldt.person.trips']
trips_ldt_vehicle_file = properties['ldt.vehicle.trips']
trips_ct_file = properties['ct.truck.trips']
trips_et_file = properties['et.truck.trips']
synpop_file = properties['spg2.current.synpop.summary']
employment_file = properties['sdt.current.employment']
alpha2beta_file = os.path.join(output_folder,'alpha2beta.csv')

def read_data(infile, full_file_path = True):
    
    if (full_file_path==False):
        infile = os.path.join(output_folder, infile)
    
    mydata = pd.read_csv(infile)

    return(mydata)

def determine_assignclass(mydata, mode, field_time):
    #debug
    #field_time = 'tripStartTime'

    #create ASSIGNCLASS field
    mydata['ASSIGNCLASS'] = '' #default - none
    mydata.ASSIGNCLASS[mydata[field_time] < am_peak_start] = mode + '_offpeak' #ev off peak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < am_peak_end)] = mode + '_peak' #am peak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < md_offpeak_end)] = mode + '_offpeak' #md offpeak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < pm_peak_end)] = mode + '_peak' #pm peak
    mydata.ASSIGNCLASS[mydata['ASSIGNCLASS'] == ''] = mode + '_offpeak' #ev offpeak

    return(mydata)

def determine_home_zone(mydata):

    #read household file
    households = read_data(household_file)

    #append home zone
    mydata = pd.merge(mydata, households[['HH_ID','TAZ']], left_on = ['hhID'], right_on = ['HH_ID'])
    mydata.rename(columns = {'TAZ':'HOME_ZONE'}, inplace = True)
    mydata = mydata.drop(['HH_ID'], 1)

    return(mydata)

def determine_ldt_trip_type(mydata, mytrips):
    #set trip weights - don't need it
    #mytrips['trip_weight'] = 1.0
    #mytrips.trip_weight[mytrips['tripMode'] == 'DA'] = 1.0
    #mytrips.trip_weight[mytrips['tripMode'] == 'SR2'] = 1.0/2
    #mytrips.trip_weight[mytrips['tripMode'] == 'SR3P'] = 1.0/3.5
    
    # get only single record for each trip - file contains mutiple records depending on occupancy
    mytrips_grouped = mytrips[['hhID','memberID','tourID','origin','destination','tourPurpose','tripPurpose','tripMode','tripStartTime']].groupby(['hhID','memberID','tourID','origin','destination'])
    mytrips_unique = mytrips_grouped['tourPurpose','tripPurpose','tripMode','tripStartTime'].first()
    mytrips_unique = mytrips_unique.reset_index()
    mytrips_unique = mytrips_unique.sort(['hhID','memberID','tourID','tripStartTime'])
    
    # find FROM_TRIP_TYPE
    mytrips_unique = mytrips_unique.groupby(['hhID','memberID','tourID']).apply(last_trip_type)

    #merge with select link data
    mydata_triptype = pd.merge(mydata, mytrips_unique[['hhID','memberID','tourID','origin', 'destination','FROM_TRIP_TYPE']],
                                on = ['hhID','memberID','tourID','origin', 'destination'])
    return(mydata_triptype)

def determine_sdt_trip_type(mydata, mytrips):
       
    #create a dataframe with only unique hhID, memberID, tour# 
    mydata_grouped = mydata[['hhID','memberID','tour#','tourSegment']].groupby(['hhID','memberID','tour#'])
    mydata_unique = mydata_grouped['tourSegment'].sum()
    mydata_unique = mydata_unique.reset_index()
    mydata_unique = mydata_unique.drop(['tourSegment'],1)
    
    #create last_trip_type for unique select link tours/trips
    mytrips_unique = pd.merge(mytrips[['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)','tourPurpose','tripPurpose']], 
                    mydata_unique,
                    on = ['hhID','memberID','tour#'])

    mytrips_unique = mytrips_unique.sort(['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)'])
    mytrips_unique = mytrips_unique.groupby(['hhID','memberID','tour#']).apply(last_trip_type)

    #merge with select link data
    mydata_triptype = pd.merge(mydata, mytrips_unique[['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)','FROM_TRIP_TYPE']],
                                on = ['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)'])
    
    return(mydata_triptype)

def last_trip_type(df):
    #just to be sure
    #df.sort(['tourSegment', 'subTour(yes/no)'])
    last_trip_purpose = ''
    i=0
    for index, row in df.iterrows():
        trip_purpose = row['tripPurpose']
        #if first trip in the tour then set to tour purpose (home or work)
        if (i==0):
            last_trip_purpose = row['tourPurpose']

        df.set_value(index,'FROM_TRIP_TYPE',last_trip_purpose)

        last_trip_purpose = trip_purpose
        i += 1
        
    return(df)

def append_select_link(infile, timefield, mode, selectlink, tourfile = ''):

    #debug
    #infile = trips_ldt_file
    #timefield = 'tripStartTime'
    #mode = 'a'
    #selectlink = select_link_result

    print('reading trip file: ' + infile)
    trips = read_data(infile)

    print('determine assignment class ...')
    trips = determine_assignclass(trips, mode, timefield)
    
    print('append select link results ...')
    trips_select_link = pd.merge(trips, selectlink, left_on = ['ASSIGNCLASS','origin','destination'], right_on = ['ASSIGNCLASS','FROMZONE','TOZONE'])
    trips_select_link.rename(columns = {'FROMZONE':'EXTERNAL_ZONE_ORIGIN', 'TOZONE':'EXTERNAL_ZONE_DESTINATION',
                            'PERCENT':'SELECT_LINK_PERCENT'}, inplace = True)
    
    #assign station number
    trips_select_link.EXTERNAL_ZONE_ORIGIN[trips_select_link['DIRECTION']=='IN'] = '_' + trips_select_link['STATIONNUMBER'].astype(str)
    trips_select_link.EXTERNAL_ZONE_DESTINATION[trips_select_link['DIRECTION']=='OUT'] = '_' + trips_select_link['STATIONNUMBER'].astype(str)

    trips_select_link = trips_select_link.drop(['ASSIGNCLASS','FROMNODETONODE','DIRECTION','STATIONNUMBER'], 1)
    
    #append HOME_ZONE and FROM_TRIP_TYPE fields
    if mode=='a':
        trips_select_link = determine_home_zone(trips_select_link)
        if 'SDT' in infile:
            trips_select_link = determine_sdt_trip_type(trips_select_link, trips)
        elif 'LDT' in infile:
            trips_select_link = determine_ldt_trip_type(trips_select_link, trips)
        else:
            print('Unexpected file: ' + infile)
    else:
        trips_select_link['HOME_ZONE'] = trips_select_link['origin']
        trips_select_link['FROM_TRIP_TYPE'] = ''

    print('writing trip file ...')
    outfile = os.path.splitext(infile)[0] + '_select_link.csv'
    outfile = os.path.join(output_folder, outfile)
    trips_select_link.to_csv(outfile, index = False)
    
    return(outfile)

def zip_output(infile_sdt, infile_ldt, infile_ldt_vehicle, infile_ct, infile_et, synpop_file, employment_file, alpha2beta_file):
    
    #zip all files
    with zipfile.ZipFile(out_zip_file, 'w') as myzip:
        myzip.write(infile_sdt)
        myzip.write(infile_ldt)
        myzip.write(infile_ldt_vehicle)
        myzip.write(infile_ct)
        myzip.write(infile_et)
        myzip.write(synpop_file)
        myzip.write(employment_file)
        myzip.write(alpha2beta_file)
    
    myzip.close()
    
    #delete csv files
    os.remove(infile_sdt)
    os.remove(infile_ldt)
    os.remove(infile_ldt_vehicle)
    os.remove(infile_ct)
    os.remove(infile_et)

def main():
    #read select link data
    select_link_result = read_data(select_link_file, full_file_path = False)
    
    #append select link result to trips
    outfile_sdt = append_select_link(trips_sdt_file, 'tripStartTime', 'a', select_link_result, tourfile = tours_sdt_file)
    outfile_ldt = append_select_link(trips_ldt_file, 'tripStartTime', 'a', select_link_result, tourfile = tours_ldt_file)
    outfile_ldt_vehicle = append_select_link(trips_ldt_vehicle_file, 'tripStartTime', 'a', select_link_result, tourfile = tours_ldt_file)
    outfile_ct = append_select_link(trips_ct_file, 'tripStartTime', 'd', select_link_result)
    outfile_et = append_select_link(trips_et_file, 'tripStartTime', 'd', select_link_result)

    #zip outputs
    zip_output(outfile_sdt, outfile_ldt, outfile_ldt_vehicle, outfile_ct, outfile_et, synpop_file, employment_file, alpha2beta_file)

if __name__ == "__main__":
    main()
