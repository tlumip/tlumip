#import libraries
import os, shutil, sys, csv, time, struct, zipfile, numpy as np, math
import win32com.client as com, VisumPy.helpers as VisumHelpers
import VisumPy.matrices as VisumMatrices, VisumPy.csvHelpers as VisumCSV
sys.path.append(os.path.join(os.getcwd(),'model','code'))
sys.path.append(os.path.join(os.getcwd(),'model','code', 'visum'))
from Properties import Properties
import heapq
import operator
import pandas as pd
# from SWIM_VISUM_Main import SwimModel

#parameters
############################################################

#load properties
if len(sys.argv) < 2:
    print(len(sys.argv))
    print(sys.argv)
    print("missing arguments!")
    sys.exit(1)

property_file = sys.argv[1]
properties = Properties()
properties.loadPropertyFile(property_file)

pathNo = [15,5,8,57,69,1,7,12,62,41,52,25,11,2,37]
prefix = properties['t.year.prefix']
visum_version = int(properties['visum.version'])
path = properties['ta.demand.output.path']
version = properties['ta.version.file']
outputDir = os.path.join(properties['scenario.outputs'], properties['t.year.prefix']+properties['t.year'])
inputDir = os.path.join(properties['scenario.inputs'], properties['t.year.prefix']+'0')
out_zip_file = properties['sl.output.bundle.file']
output_folder = os.path.dirname(out_zip_file)
continuoustt = int(properties['sl.continuous.travel.time']) #mins
breaktime = int(properties['sl.break.time']) #mins
useSkims = properties['sl.use.skims']
mclsmatrixdir = properties['skim.data']


def unzip_output():
    basename = os.path.basename(out_zip_file)
    out_directory = basename.split('.')[0]
    extracted_directory = os.path.join(output_folder, out_directory)

    #unzip files
    zip_ref = zipfile.ZipFile(out_zip_file,'r')
    zip_ref.extractall(extracted_directory)
    zip_ref.close()

    return(extracted_directory)


numtry = 20
pauseSec = 30
Visum = VisumHelpers.CreateVisum(visum_version)
for i in range(0,len(pathNo)):
	Visum.SetPath(pathNo[i], path)

suffix="_SL"
print("load version file: " + str(version)[:-4] + suffix + ".ver")
Visum.LoadVersion(version)


zone_no     = VisumHelpers.GetMulti(Visum.Net.Zones, "NO")
zone_xcoord = VisumHelpers.GetMulti(Visum.Net.Zones, "XCOORD")
zone_ycoord = VisumHelpers.GetMulti(Visum.Net.Zones, "YCOORD")

link_fnodeno = VisumHelpers.GetMulti(Visum.Net.Links, "FROMNODE\NO")
link_tnodeno = VisumHelpers.GetMulti(Visum.Net.Links, "TONODE\NO")
link_fnodex = VisumHelpers.GetMulti(Visum.Net.Links, "FROMNODE\XCOORD")
link_fnodey = VisumHelpers.GetMulti(Visum.Net.Links, "FROMNODE\YCOORD")
link_tnodex = VisumHelpers.GetMulti(Visum.Net.Links, "TONODE\XCOORD")
link_tnodey = VisumHelpers.GetMulti(Visum.Net.Links, "TONODE\YCOORD")
link_fnodeno = VisumHelpers.GetMulti(Visum.Net.Links, "FROMNODE\NO")
link_tnodeno = VisumHelpers.GetMulti(Visum.Net.Links, "TONODE\NO")
link_azone = VisumHelpers.GetMulti(Visum.Net.Links, "AZONE")
select_links = pd.read_csv(os.path.join(inputDir, 'selectLinks.csv'))

link_df = pd.DataFrame({'FROMNODE':link_fnodeno,
'TONODE': link_tnodeno,
'ZoneNo': link_azone})

StationNo = select_links.merge(link_df,how='left',on=['FROMNODE','TONODE'])[['STATIONNUMBER', 'ZoneNo']].drop_duplicates().sort_values('STATIONNUMBER').reset_index(drop=True)
StationNo['ZoneNo'] = StationNo['ZoneNo'].astype(int)
Visum = 0

def readZMX2array(zmxfileName):
    """
    Read a ZMX file to a mutable numpy array.

    The readZMX function reads to immutable tuple, which obviously doesn't
    work if future transformations are needed.
    :param zmxfileName: The path to the zmx file
    :return: A list containing [0] a np.array, [1] a list of zone names, and
    [2] the name of the matrix.
    """

    print('......read zmx file: ' + zmxfileName)

    #read header files
    z = zipfile.ZipFile(zmxfileName, "r")
    version = z.read("_version")
    description = z.read("_description")
    name = z.read("_name")
    zoneNames = z.read("_external column numbers")
    rowZoneNames = z.read("_external row numbers")
    columns = z.read("_columns")
    rows = int(z.read("_rows"))

    #read rows, big-endian floats
    for i in range(1, rows+1):
        fileNameRow = "row_" + str(i)
        data = z.read(fileNameRow)
        if i == 1:  # create the array
            mat = np.array([struct.unpack(">" + "f" * rows, data)])
        else:  # add a row to the array
            mat = np.vstack([mat, struct.unpack(">" + "f" * rows, data)])

    #close connections
    z.close()

    #return matrix data, zone names, matrix name
    return(mat, zoneNames, name)

skim_dir = mclsmatrixdir
tt = skim_dir + useSkims + "autotime.zmx"
auto_skims_df = readZMX2array(tt)

tt = skim_dir + useSkims + "trk1time.zmx"
truck_skims_df = readZMX2array(tt)

def get_tt(row, rowname='FROMNODE', colname='TONODE', skims_df=[]):
    zone_no = skims_df[1].split(",")
    if not row[[rowname,colname]].isnull().any():
        return skims_df[0][zone_no.index(str(int(row[rowname]))), zone_no.index(str(int(row[colname])))]
    else:
        return np.nan


def calcMins(starttime):
    hour = starttime / 100 % 100
    mins = starttime % 100
    return sum(i*j for i, j in zip((hour, mins), (60,1)))

def calcTime(starttimemins):
    hour = starttimemins // 60
    mins = starttimemins % 60
    return (hour * 100) + mins


def update_start_time(trips_df, StationNo, skims_df):
    original_columns = trips_df.columns
    trips_df['origTripStartTime'] = trips_df['tripStartTime']
    trips_df['totaltt'] = trips_df.apply(get_tt, rowname='origin', colname='destination', skims_df=skims_df, axis=1)
    trips_df['EXTERNALSTATIONORIGIN'] = trips_df['EXTERNAL_ZONE_ORIGIN'].where(trips_df['EXTERNAL_ZONE_ORIGIN'].str.contains('_'),'0').str.replace('_','').astype(int)
    trips_df['EXTERNALSTATIONDESTINATION'] = trips_df['EXTERNAL_ZONE_DESTINATION'].where(trips_df['EXTERNAL_ZONE_DESTINATION'].str.contains('_'),'0').str.replace('_','').astype(int)
    trips_df = trips_df.merge(StationNo, how='left', left_on='EXTERNALSTATIONORIGIN', right_on='STATIONNUMBER').rename(columns={'ZoneNo':'ClosestOriginZone'}).drop('STATIONNUMBER', axis=1)
    trips_df = trips_df.merge(StationNo, how='left', left_on='EXTERNALSTATIONDESTINATION', right_on='STATIONNUMBER').rename(columns={'ZoneNo':'ClosestDestinationZone'}).drop('STATIONNUMBER', axis=1)
    trips_df['toexttt'] = np.nan
    trips_df.loc[trips_df.EXTERNALSTATIONORIGIN>0,'toexttt'] = trips_df.loc[trips_df.EXTERNALSTATIONORIGIN>0].apply(get_tt, rowname='origin', colname='ClosestOriginZone', skims_df=skims_df, axis=1)
    trips_df['fromexttt'] = np.nan
    trips_df.loc[trips_df.EXTERNALSTATIONDESTINATION>0,'fromexttt'] = trips_df.loc[trips_df.EXTERNALSTATIONDESTINATION>0].apply(get_tt, rowname='ClosestDestinationZone', colname='destination', skims_df=skims_df, axis=1)
    trips_df['thruexttt'] = np.nan
    trips_df.loc[(trips_df.EXTERNALSTATIONORIGIN>0) & (trips_df.EXTERNALSTATIONDESTINATION>0),'thruexttt'] = trips_df.loc[(trips_df.EXTERNALSTATIONORIGIN>0) & (trips_df.EXTERNALSTATIONDESTINATION>0)].apply(get_tt, rowname='ClosestOriginZone', colname='ClosestDestinationZone', skims_df=skims_df, axis=1)
    trips_df['tripStartTimeMins'] = calcMins(trips_df.tripStartTime.values)

    trips_df['addbreaks'] = (trips_df.toexttt // continuoustt) * breaktime

    trips_df['adjustst'] = trips_df['toexttt'] + trips_df['addbreaks']
    trips_df['adjustedTripStartTimeMins'] = trips_df['tripStartTimeMins'] + trips_df['adjustst']

    trips_df[(trips_df.adjustedTripStartTimeMins!=trips_df.tripStartTimeMins) & ~(trips_df['toexttt'].isnull())]
    trips_df['adjustedTripStartTimeMins'] = trips_df['tripStartTimeMins'].where(trips_df['adjustedTripStartTimeMins'].isnull(),trips_df['adjustedTripStartTimeMins']).astype(int)
    trips_df['tripStartTime'] = calcTime(trips_df['adjustedTripStartTimeMins'])
    keep_columns = list(original_columns) + ['origTripStartTime']
    return trips_df[keep_columns]

def main():
    #zip outputs
    print('Unzip select link outputs')
    data_dir = unzip_output()
    # output_dir = os.path.join(outputDir, 'select_link_updated_outputs')
    # if not os.path.exists(output_dir):
    #     os.mkdir(output_dir)

    print("Updating start time")
    et_trips = pd.read_csv(os.path.join(data_dir, 'Trips_ETTruck_select_link.csv'))
    et_trips_updated = update_start_time(et_trips, StationNo, truck_skims_df)
    et_trips_updated.to_csv(os.path.join(data_dir, 'Trips_ETTruck_select_link.csv'), index=False)

    ct_trips = pd.read_csv(os.path.join(data_dir, 'Trips_CTTruck_select_link.csv'))
    ct_trips_updated = update_start_time(ct_trips, StationNo, truck_skims_df)
    ct_trips_updated.to_csv(os.path.join(data_dir, 'Trips_CTTruck_select_link.csv'), index=False)


    ldpt_trips = pd.read_csv(os.path.join(data_dir, 'Trips_LDTPerson_select_link.csv'))
    ldpt_trips_updated = update_start_time(ldpt_trips, StationNo, auto_skims_df)
    ldpt_trips_updated.to_csv(os.path.join(data_dir, 'Trips_LDTPerson_select_link.csv'), index=False)

    ldvt_trips = pd.read_csv(os.path.join(data_dir, 'Trips_LDTVehicle_select_link.csv'))
    ldvt_trips_updated = update_start_time(ldvt_trips, StationNo, auto_skims_df)
    ldvt_trips_updated.to_csv(os.path.join(data_dir, 'Trips_LDTVehicle_select_link.csv'), index=False)

    sdpt_trips = pd.read_csv(os.path.join(data_dir, 'Trips_SDTPerson_select_link.csv'))
    sdpt_trips_updated = update_start_time(sdpt_trips, StationNo, auto_skims_df)
    sdpt_trips_updated.to_csv(os.path.join(data_dir, 'Trips_SDTPerson_select_link.csv'), index=False)

    print("Cleaning and zipping up output")
    zip_files = [zip_file for zip_file in os.walk(data_dir)][0]
    zip_files = [os.path.join(data_dir, zip_file) for zip_file in zip_files[2]]
    with zipfile.ZipFile(out_zip_file, 'w', zipfile.ZIP_DEFLATED) as myzip:
        for out_file in zip_files:
            myzip.write(out_file,os.path.basename(out_file))

    myzip.close()
    #remove the un-zipped folder
    print('Delete unzipped outputs')
    if os.path.isdir(data_dir):
        shutil.rmtree(data_dir)

if __name__ == "__main__":
    main()