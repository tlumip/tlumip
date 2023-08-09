import os
import subprocess
import sys
from os.path import join
import shutil
import csv

import retexchange
import techscaling


def main(year):
    retexchange.main("t{}".format(year))
    props = techscaling.read_props(year)
    #Copy floor space inventory file only for 2016
    if year == 26:
        copy_floorspace_files(props)
    #Copy activitytotal file and technology options file for year 2016 and beyond
    if year >= 26:
        copy_act_and_techopt_files(props)
    run_preprocessor(props)
    if props["aa.technologyScaling"] and props["aa.updateImportsAndExports"]:
        move_preprocessor_files(props)
        techscaling.run_techscaling(year)


def run_preprocessor(props):
    javacmd = props["aa.command.java"]
    maxheap = props["aa.command.max.heap.size"]
    log4j = props["aa.command.log4j.config.file"]
    classpath = props["aa.command.classpath"]

    subprocess.call(
        [javacmd, "-Xmx" + maxheap, "-Dlog4j.configuration=" + log4j,
         "-cp", classpath, "com.hbaspecto.pecas.aa.control.AAPProcessor"]
    )


def move_preprocessor_files(props):
    cur_dir = props["aa.current.data"]
    try:
        os.remove(join(cur_dir, "ActivityTotalsI.csv"))
    except OSError:
        pass
    os.rename(join(cur_dir, "ActivityTotalsW.csv"), join(cur_dir, "ActivityTotalsI.csv"))

def copy_act_and_techopt_files(props):
    cur_dir = props["aa.current.data"]
    act_techopt_dir = props.get('aa.activitytotalsi.technologyoptionsi.dir', props['aa.base.data'])
    try:
        os.remove(join(cur_dir, "ActivityTotalsI.csv"))
        os.remove(join(cur_dir, "TechnologyOptionsI.csv"))
    except OSError:
        pass
    shutil.copy(join(act_techopt_dir, 'ActivityTotalsI.csv'), join(cur_dir, 'ActivityTotalsI.csv'))
    shutil.copy(join(act_techopt_dir, 'ActivityTotalsI.csv'), join(cur_dir, 'ActivityTotalsW.csv'))
    shutil.copy(join(act_techopt_dir, 'TechnologyOptionsI.csv'), join(cur_dir, 'TechnologyOptionsI.csv'))

def copy_floorspace_files(props):
    cur_dir = props["aa.current.data"]
    act_techopt_dir = props.get('aa.activitytotalsi.technologyoptionsi.dir', props['aa.base.data'])
    try:
        os.remove(join(cur_dir, "FloorspaceI.csv"))
    except OSError:
        pass
    shutil.copy(join(act_techopt_dir, 'FloorspaceI.csv'), join(cur_dir, 'FloorspaceI.csv'))
    # Reformat FloorspaceI to FloorSpaceInventory file
    flspace_inventory_fname = join(cur_dir, "FloorspaceInventory.csv")
    with open(flspace_inventory_fname, "rU") as file:
        reader = csv.reader(file)
        flspace_headers = reader.next()
    
    flspace = retexchange.read_flspace(join(cur_dir, 'FloorspaceI.csv'))
    flspace_tazs = set(key[0] for key in flspace.keys())
    flspace_type = set(key[1] for key in flspace.keys())
    flspace_type = [fltype.replace('.', ' ') for fltype in flspace_headers if fltype.replace('.', ' ') in flspace_type]
    flspace_headers = ['AZone']+[fltype.replace(' ','.') for fltype in flspace_type]
    flspace_data = [[str(int(taz))]+[str(flspace.get((taz,fltype), 0)) for fltype in flspace_type] for taz in flspace_tazs]
    with open(flspace_inventory_fname, "w") as file:
        writer = csv.writer(file, retexchange.excelOne)
        writer.writerow(flspace_headers)
        writer.writerows(flspace_data)


if __name__ == "__main__":
    main(int(sys.argv[1]))
