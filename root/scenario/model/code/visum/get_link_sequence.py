from __future__ import with_statement

##################################################
#get_link_sequence.py
# Gets the correct sequence of links for a given path (in a specific
#   dseg) between o-d pairs. The od and link information is specified
#   in an input file with the columns:
#
#      od,links,percentage
#
#   where od is in the format "origin destination", links is a semicolon-
#   delimited list of link ids, and percentage is the percentage of flow
#   between the od pair that uses that link (this value is just passed
#   through by this script).
#
# This script writes over that input file with the same input data,
#   appending an "ordering" column wich is a semicolon-delimited list of
#   1-based indices for the ordering of the links in the actual path.
#
# This script is called as:
#
#      python get_link_sequence.py version_file weaving_file dseg
#
#   where the files must be the full (absolute) paths, and dseg is the
#   demand segment.
#
# crf - 4/2013
##################################################


###################################################
##Parameters
#path = 'D:/projects/tlumip/vissum/'
#version_file = path + 'SWIMNetwork_v3_ABZ_mod032713.ver'
#weaving_file = path + 'weaving_data.csv'
#dseg = 'a'
###################################################

#VISUM program version
programVersion = '14'

import os,sys,csv
import win32com.client as com
import VisumPy.helpers

if len(sys.argv) < 4:
    print "Missing arguments! Usage:\n\t" + "python get_link_sequence.py version_file weaving_file dseg"

version_file = sys.argv[1]
weaving_file = os.path.join(os.path.dirname(version_file),sys.argv[2])
dseg = sys.argv[3]

#Start Visum and load file
Visum = com.Dispatch('visum.visum.'+programVersion)
Visum.LoadVersion(version_file)

output_rows = []
paths = {} #so we can save across queries
with open(weaving_file,'rb') as f:
    reader = csv.DictReader(f)
    for row in reader:
        new_row = {}
        for column in row:
            new_row[column] = row[column]
        od_pair = tuple(row['od'].strip().split())
        (origin,destination) = map(int,od_pair)
        #links = map(int,row['links'].strip().split(';'))
        links = [(int(x[0]),int(x[1])) for x in map(str.split,row['links'].strip().split(';'))]
        
        if not od_pair in paths:
            paths[od_pair] = {}

            #build equivalent path links in paths
            segment = Visum.Net.DemandSegments.ItemByKey(dseg)
            num_paths = segment.GetNumPaths(origin,destination)
            for path_id in range(1,num_paths+1):
                paths[od_pair][path_id] = []
                path_nodes = segment.GetPathNodes(origin,destination,path_id).GetAll
                bnode =  int(path_nodes[0].AttValue('No'))
                for n in range(1,len(path_nodes)):
                    anode = bnode
                    bnode = int(path_nodes[n].AttValue('No'))
                    if not (anode,bnode) in links:
                        continue
                    linkno = Visum.Net.Links.ItemByKey(anode,bnode).AttValue('No')
                    paths[od_pair][path_id].append(linkno)
                    anode = bnode

        for path_id in paths[od_pair]:
            link_path = paths[od_pair][path_id]
            if len(link_path) == len(links):
                #found it, hopefully
                order = []
                success = True
                for link in links:
                    if not link in link_path:
                        success = False
                        break
                    order.append(str(link_path.index(link)+1))
                if not success:
                    continue
                new_row['ordering'] = ";".join(order)
                output_rows.append(new_row)
                break
        else:
            #didn't find it
            print "ERROR: could not find weaving path for od pair " + str(od_pair) + ", links " + str(links)

with open(weaving_file,'wb') as f:
    writer = csv.DictWriter(f,['od','links','percentage','ordering'])
    #writer.writeHeader()
    writer.writer.writerow(writer.fieldnames)
    for row in output_rows:
        writer.writerow(row)


