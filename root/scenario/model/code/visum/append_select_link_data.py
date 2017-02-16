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

#time of day
am_peak_start = int(properties['am.peak.start'])
am_peak_end = int(properties['am.peak.end'])
md_offpeak_end = int(properties['md.offpeak.end'])
pm_peak_end = int(properties['pm.peak.end'])

auto_classes = properties['sl.auto.classes'].split(",") #am, md, pm, ni
truck_classes = properties['sl.truck.classes'].split(",") #am, md, pm, ni

select_link_file = properties['sl.output.file.select.link.results'] #only file name - no full file path

#tour and trip files
household_file = properties['sdt.household.data']
tours_sdt_file = properties['sdt.person.tours']
tours_ldt_file = properties['ldt.tours']
trips_sdt_file = properties['sdt.person.trips']
trips_ldt_file = properties['ldt.person.trips']
trips_ldt_vehicle_file = properties['ldt.vehicle.trips']
trips_ct_file = properties['ct.truck.trips']
trips_et_file = properties['et.truck.trips']

#other files
synpop_file = properties['spg2.current.synpop.summary']
employment_file = properties['sdt.current.employment']
alpha2beta_file = os.path.join(output_folder,'alpha2beta.csv')

#output summary file
out_summary_file = properties['sl.output.file.select.link.summary']

'''
reads a csv file
'''
def read_data(infile, full_file_path = True):
    
    if (full_file_path==False):
        infile = os.path.join(output_folder, infile)
    
    mydata = pd.read_csv(infile)

    return(mydata)

'''
assigns assignment class ([mode]_pea, [mode]_offpeak, [mode]_pm, [mode]_ni - where mode is 'a' or 'd') to auto/truck trips
'''
def determine_assignclass(mydata, mode, field_time):

    #periods of OD pairs to select from the select link file
    #for example: if MD period has 'a_peak' then OD pairs in 'a_peak' of the select link file fill be selected from the MD pperiod in the auto trip files.
    #this gives more control to user on what od pairs to assign to different time periods
    if (mode == 'CT_TRIP')|(mode == 'ET_TRIP'):
        mode_classes = truck_classes
    else:
        mode_classes = auto_classes

    #create ASSIGNCLASS field
    mydata['ASSIGNCLASS'] = '' #default - none
    mydata.ASSIGNCLASS[mydata[field_time] < am_peak_start] = mode_classes[3] #ni off peak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < am_peak_end)] = mode_classes[0]  #am peak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < md_offpeak_end)] = mode_classes[1]  #md offpeak
    mydata.ASSIGNCLASS[(mydata['ASSIGNCLASS'] == '') & (mydata[field_time] < pm_peak_end)] = mode_classes[2]  #pm peak
    mydata.ASSIGNCLASS[mydata['ASSIGNCLASS'] == ''] = mode_classes[3] #ni offpeak

    return(mydata)

'''
assigns home zone id to trips
'''
def determine_home_zone(mydata):

    #read household file
    households = read_data(household_file)

    #append home zone
    mydata = pd.merge(mydata, households[['HH_ID','TAZ']], left_on = ['hhID'], right_on = ['HH_ID'])
    mydata.rename(columns = {'TAZ':'HOME_ZONE'}, inplace = True)
    mydata = mydata.drop(['HH_ID'], 1)

    return(mydata)


'''
determines from trip type (trip purpose) for long distance travel (LDT) trips
'''
def determine_ldt_trip_type(mydata, mytrips):
    
    # get only single record for each trip - file contains mutiple records depending on occupancy
    mytrips_grouped = mytrips[['hhID','memberID','tourID','origin','destination','tourPurpose','tripPurpose','tripMode','tripStartTime']].groupby(['hhID','memberID','tourID','origin','destination'])
    mytrips_unique = mytrips_grouped['tourPurpose','tripPurpose','tripMode','tripStartTime'].first()
    mytrips_unique = mytrips_unique.reset_index()
    mytrips_unique = mytrips_unique.sort(['hhID','memberID','tourID','tripStartTime'])

    #select link trips
    mydata_grouped = mydata[['hhID','memberID','tourID','tripMode']].groupby(['hhID','memberID','tourID'])
    mydata_unique = mydata_grouped['tripMode'].first()
    mydata_unique = mydata_unique.reset_index()
    
    #select only trips of tours that are in the select link results - to save time
    mytrips_selectlink = pd.merge(mytrips_unique, 
                    mydata_unique[['hhID','memberID','tourID']],
                    on = ['hhID','memberID','tourID']) 

    # find FROM_TRIP_TYPE
    mytrips_triptype = mytrips_selectlink.groupby(['hhID','memberID','tourID']).apply(last_trip_type)

    #merge with select link data
    mydata_triptype = pd.merge(mydata, mytrips_triptype[['hhID','memberID','tourID','origin', 'destination','FROM_TRIP_TYPE']],
                                on = ['hhID','memberID','tourID','origin', 'destination'])
    return(mydata_triptype)

'''
determines from trip type (trip purpose) for short distance travel (SDT) trips
'''
def determine_sdt_trip_type(mydata, mytrips):
       
    #create a dataframe with only unique hhID, memberID, tour# 
    mydata_grouped = mydata[['hhID','memberID','tour#','tourSegment']].groupby(['hhID','memberID','tour#'])
    mydata_unique = mydata_grouped['tourSegment'].sum()
    mydata_unique = mydata_unique.reset_index()
    mydata_unique = mydata_unique.drop(['tourSegment'],1)

    #select only trips of tours that are in the select link results - to save time
    mytrips_selectlink = pd.merge(mytrips[['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)','tourPurpose','tripPurpose']], 
                    mydata_unique,
                    on = ['hhID','memberID','tour#'])

    #create last_trip_type for unique select link tours/trips
    mytrips_selectlink = mytrips_selectlink.sort(['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)'])
    mytrips_triptype = mytrips_selectlink.groupby(['hhID','memberID','tour#']).apply(last_trip_type)

    #merge with select link data
    mydata_triptype = pd.merge(mydata, mytrips_triptype[['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)','FROM_TRIP_TYPE']],
                                on = ['hhID','memberID','tour#','tourSegment', 'subTour(yes/no)'])
    
    return(mydata_triptype)

'''
determines trip type (trip purpose) of the previous trip
'''
def last_trip_type(df):
    #just to be sure
    #df.sort(['tourSegment', 'subTour(yes/no)'])
    last_trip_purpose = ''
    i=0
    for index, row in df.iterrows():
        trip_purpose = row['tripPurpose']
        #if first trip in the tour then set to tour purpose (home or work)
        if (i==0):
            if trip_purpose == 'HOME':
                last_trip_purpose = 'WORK'
            elif trip_purpose == 'WORK':
                last_trip_purpose = 'HOME'

        df.set_value(index,'FROM_TRIP_TYPE',last_trip_purpose)

        last_trip_purpose = trip_purpose
        i += 1
        
    return(df)

'''
selects select link OD pairs in trip file and attach select link information
'''
def append_select_link(infile, timefield, selectlink, tourfile, colname, summary_df):

    print('reading trip file: ' + infile)
    trips = read_data(infile)

    print('determine assignment class ...')
    trips = determine_assignclass(trips, colname, timefield)
    
    print('append select link results ...')
    trips_select_link = pd.merge(trips, selectlink, left_on = ['ASSIGNCLASS','origin','destination'], right_on = ['ASSIGNCLASS','FROMZONE','TOZONE'])
    trips_select_link.rename(columns = {'FROMZONE':'EXTERNAL_ZONE_ORIGIN', 'TOZONE':'EXTERNAL_ZONE_DESTINATION',
                            'PERCENT':'SELECT_LINK_PERCENT'}, inplace = True)

    print('total select link trips: ' + str(len(trips_select_link)))
    
    #assign station number
    trips_select_link.EXTERNAL_ZONE_ORIGIN[trips_select_link['DIRECTION']=='IN'] = '_' + trips_select_link['STATIONNUMBER'].astype(str)
    trips_select_link.EXTERNAL_ZONE_DESTINATION[trips_select_link['DIRECTION']=='OUT'] = '_' + trips_select_link['STATIONNUMBER'].astype(str)

    #summary of trips by time period, stations, and direction
    if len(trips_select_link) > 0:
        #update assignclass definitions - time periods
        trips_select_link.ASSIGNCLASS[trips_select_link['ASSIGNCLASS'].str.contains('_peak')] = 'peak'
        trips_select_link.ASSIGNCLASS[trips_select_link['ASSIGNCLASS'].str.contains('_offpeak')] = 'offpeak'
        trips_select_link.ASSIGNCLASS[trips_select_link['ASSIGNCLASS'].str.contains('_ni')] = 'ni'
        trips_select_link.ASSIGNCLASS[trips_select_link['ASSIGNCLASS'].str.contains('_pm')] = 'pm'
    
        #summary of trips by time period, station number and direction
        summary = trips_select_link.groupby(['ASSIGNCLASS', 'STATIONNUMBER','DIRECTION']).count()['SELECT_LINK_PERCENT'].reset_index()
        summary = summary.rename(columns={'SELECT_LINK_PERCENT':colname})
        summary_df = pd.merge(summary_df, summary, on = ['ASSIGNCLASS', 'STATIONNUMBER','DIRECTION'], how = 'left')
        summary_df = summary_df.fillna(0)
    else:
        summary_df[colname] = 0
    
    #drop unnecessary fields
    trips_select_link = trips_select_link.drop(['ASSIGNCLASS','ASSIGNCLASS_NEW','FROMNODETONODE','DIRECTION','STATIONNUMBER', 'AUTO_SL_OD', 'TRUCK_SL_OD'], 1)
    
    #append HOME_ZONE and FROM_TRIP_TYPE fields
    #trucks
    if (colname == 'CT_TRIP') | (colname == 'ET_TRIP'):
        #set HOME_ZONE as trip origin and FROM_TRIP_TYPE to empty
        trips_select_link['HOME_ZONE'] = trips_select_link['origin']
        trips_select_link['FROM_TRIP_TYPE'] = ''

    #autos
    else:
        trips_select_link = determine_home_zone(trips_select_link)
        if 'SDT' in infile:
            trips_select_link = determine_sdt_trip_type(trips_select_link, trips)
        elif 'LDT' in infile:
            trips_select_link = determine_ldt_trip_type(trips_select_link, trips)
        else:
            print('Unexpected file: ' + infile)
            
    print('writing trip file ...')
    outfile = os.path.splitext(infile)[0] + '_select_link.csv'
    outfile = os.path.join(output_folder, outfile)
    trips_select_link.to_csv(outfile, index = False)
    
    return(outfile, summary_df)

'''
creates a zip file of the outputs and delete csv files
'''
def zip_output(infile_sdt, infile_ldt, infile_ldt_vehicle, infile_ct, infile_et):
    
    #zip all files
    with zipfile.ZipFile(out_zip_file, 'w') as myzip:
        myzip.write(infile_sdt,os.path.basename(infile_sdt))
        myzip.write(infile_ldt,os.path.basename(infile_ldt))
        myzip.write(infile_ldt_vehicle,os.path.basename(infile_ldt_vehicle))
        myzip.write(infile_ct,os.path.basename(infile_ct))
        myzip.write(infile_et,os.path.basename(infile_et))
        myzip.write(synpop_file,os.path.basename(synpop_file))
        myzip.write(employment_file,os.path.basename(employment_file))
        myzip.write(alpha2beta_file,os.path.basename(alpha2beta_file))
    
    myzip.close()
    
    #delete csv files
    os.remove(infile_sdt)
    os.remove(infile_ldt)
    os.remove(infile_ldt_vehicle)
    os.remove(infile_ct)
    os.remove(infile_et)

'''
generate summary of OD pairs in selectliLinkResults.csv by time period and mode
'''
def generate_select_link_summary(mydata):
    mydata['AUTO_SL_OD'] = 0
    mydata['TRUCK_SL_OD'] = 0
    mydata['ASSIGNCLASS_NEW'] = 0

    #mode
    mydata.AUTO_SL_OD[mydata['ASSIGNCLASS'].str.contains('a_')] = 1
    mydata.TRUCK_SL_OD[mydata['ASSIGNCLASS'].str.contains('d_')] = 1

    #time period
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_peak')] = 'peak'
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_offpeak')] = 'offpeak'
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_ni')] = 'ni'
    mydata.ASSIGNCLASS_NEW[mydata['ASSIGNCLASS'].str.contains('_pm')] = 'pm'
    
    summary_df = mydata.groupby(['ASSIGNCLASS_NEW', 'STATIONNUMBER','DIRECTION']).sum()[['AUTO_SL_OD', 'TRUCK_SL_OD']].reset_index()
    summary_df = summary_df.rename(columns = {'ASSIGNCLASS_NEW': 'ASSIGNCLASS'})

    return(summary_df)

'''
main function that appends select link data to trip files
'''
def main():
    global select_link_summary
    #read select link data
    select_link_result = read_data(select_link_file, full_file_path = False)
    select_link_summary = generate_select_link_summary(select_link_result)
    
    #append select link result to trips
    outfile_sdt, select_link_summary = append_select_link(trips_sdt_file, 'tripStartTime', select_link_result, tourfile=tours_sdt_file, colname='SDT_PERSON_TRIP', summary_df=select_link_summary)
    outfile_ldt, select_link_summary = append_select_link(trips_ldt_file, 'tripStartTime', select_link_result, tourfile=tours_ldt_file, colname='LDT_PERSON_TRIP', summary_df=select_link_summary)
    outfile_ldt_vehicle, select_link_summary = append_select_link(trips_ldt_vehicle_file, 'tripStartTime', select_link_result, tourfile=tours_ldt_file, colname='LDT_VEHICLE_TRIP', summary_df=select_link_summary)
    outfile_ct, select_link_summary = append_select_link(trips_ct_file, 'tripStartTime', select_link_result, tourfile=None, colname='CT_TRIP', summary_df=select_link_summary)
    outfile_et, select_link_summary = append_select_link(trips_et_file, 'tripStartTime', select_link_result, tourfile=None, colname='ET_TRIP', summary_df=select_link_summary)

    #zip outputs
    zip_output(outfile_sdt, outfile_ldt, outfile_ldt_vehicle, outfile_ct, outfile_et)

    #write summary
    select_link_summary.to_csv(os.path.join(output_folder, out_summary_file), header=True, index=False) 
    
if __name__ == "__main__":
    main()
