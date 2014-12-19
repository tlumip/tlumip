#usage: GrowET.py properties_file year
##usage: GrowET.py output_file base_file base_year year
# years are the tX years
import sys,os,math
from Properties import Properties

#outfile = sys.argv[1]
#base_file = sys.argv[2]
#base_year = int(sys.argv[3])
#year = int(sys.argv[4])

properties = Properties()
properties.loadPropertyFile(sys.argv[1])

outfile = properties["et.truck.trips"]
base_file = properties["et.basis.matrix"]
base_year = int(properties["et.basis.year"])
year = int(sys.argv[2])

large_growth = float(properties["large.road.growth.rate"])/100.0
small_growth = float(properties["small.road.growth.rate"])/100.0
large_roads = map(str.strip,properties["large.roads"].split(","))
small_roads = map(str.strip,properties["small.roads"].split(","))


#large_growth = 0.024
#small_growth = 0.01
#large_roads = ["5002","5003","5004","5007","5010"]
#small_roads = ["5001","5005","5006","5008","5009","5011"]
f = open(outfile,"wb")
first = True
for line in open(base_file):
    if first:
        f.write(line.strip() + os.linesep)
        first = False
        continue
    data = line.strip().split(",")
    if data[0] in large_roads:
        factor = large_growth
    else:
        factor = small_growth
    if data[1] in large_roads:
        factor += large_growth
    else:
        factor += small_growth
    data[4] = str(float(data[4])/math.pow(1.0 + factor/2.0,base_year-year))
    f.write(",".join(data) + os.linesep)
f.close()
