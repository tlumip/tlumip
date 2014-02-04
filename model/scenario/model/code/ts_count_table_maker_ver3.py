#ts_count_table_maker.py
#usage python ts_count_table_maker.py base_path(to scenario) outfile
import sys,os

TOTAL_COUNT_KEY = "total_assignment_count"
TRUCK_COUNT_KEY = "truck_assignment_count"
TOTAL_RESULT_KEY = "total_assignment_result"
TRUCK_RESULT_KEY = "truck_assignment_result"


class Network(object):
    def __init__(self,network_file,attribute_file):
        self.links = {}
        self.loadNetwork(network_file,attribute_file)
    
    def loadNetwork(self,network_file,attribute_file):
        mpo_matchup = {}
        
        in_links = False
        for line in open(network_file):
            if line.strip() == "t links init":
                in_links = True
                continue
            if not in_links:
                continue
            link_data = line.strip().split()
            anode = link_data[1]
            bnode = link_data[2]
            fclass = link_data[5]
            mpo_link = link_data[10]
            mpo_anode = None
            mpo_bnode = None
            if mpo_link == "0":
                mpo = -1
            else:
                mpo = mpo_link[0]
            l = Link(anode,bnode)
            l.addAttribute("fclass",fclass)
            l.addAttribute("mpo",mpo)
            l.addAttribute("mpo_anode",mpo_anode)
            l.addAttribute("mpo_bnode",mpo_bnode)
            self.links[anode + " " + bnode] = l
        first = True
        for line in open(attribute_file):
            if first:
                first = False
                continue
            data = line.split(",")
            key = data[0] + " " + data[1]
            if not key in self.links:
                print "Warning, key not found: " + key
            else:
                self.links[key].addAttribute("uniqid",data[2])
                self.links[key].addAttribute("new_taz",data[6])
    
    def loadCountData(self,auto_counts_file,truck_counts_file):
        for line in open(auto_counts_file):
            self.__addCountData__(line,TOTAL_COUNT_KEY)
        for line in open(truck_counts_file):
            self.__addCountData__(line,TRUCK_COUNT_KEY)
    
    def __addCountData__(self,line,assignment_attribute_key):
        data = line.split()
        try:
            l = self.links[data[1] + " " + data[2]]
        except KeyError:
            print "Unknown link: " + data[1] + " " + data[2]
            return
        #if l.getAttribute("uniqid") <> data[3]:
            #print "Warning: Uniqid for link '" + l.anode + " " + l.bnode + "' doesn't match count file: " + l.getAttribute("uniqid") + " vs. " + data[3]
            #return
        if not l.hasAttribute(assignment_attribute_key):
            l.addAttribute(assignment_attribute_key,AssignmentDataCollection())
        l.getAttribute(assignment_attribute_key).addAssignmentData(data[0],data[4],data[5],data[6],data[7])
    
    def loadAssignmentData(self,year,am_peak_results_file=None,midday_results_file=None,pm_peak_results_file=None,ln_results_file=None):
        if am_peak_results_file <> None:
            self.loadPeriodAssignmentData(year,am_peak_results_file,"a")
        if midday_results_file <> None:
            self.loadPeriodAssignmentData(year,midday_results_file,"m")
        if pm_peak_results_file <> None:
            self.loadPeriodAssignmentData(year,pm_peak_results_file,"p")
        if ln_results_file <> None:
            self.loadPeriodAssignmentData(year,ln_results_file,"l")
    
    def loadPeriodAssignmentData(self,year,results_file,period):
        first = True
        for line in open(results_file):
            if first:
                first = False
                continue
            data = line.split(",")
            key = data[1] + " " + data[2]
            l = self.links[key]
            if l.hasAttribute(TOTAL_COUNT_KEY):
                if not l.hasAttribute(TOTAL_RESULT_KEY):
                    l.addAttribute(TOTAL_RESULT_KEY,AssignmentDataCollection())
                if not year in l.getAttribute(TOTAL_RESULT_KEY).assignment_data:
                    l.getAttribute(TOTAL_RESULT_KEY).assignment_data[year] = AssignmentData(-1,-1,-1,-1)
                if data[6] == "":
                	total_count = float(data[5]) + float(data[7]) + float(data[9]) + float(data[11])
                else:
                	total_count = float(data[5]) + float(data[6]) + float(data[7]) + float(data[8])
                if period == "a":
                    l.getAttribute(TOTAL_RESULT_KEY).assignment_data[year].a = total_count
                if period == "m":
                    l.getAttribute(TOTAL_RESULT_KEY).assignment_data[year].m = total_count
                if period == "p":
                    l.getAttribute(TOTAL_RESULT_KEY).assignment_data[year].p = total_count
                if period == "l":
                    l.getAttribute(TOTAL_RESULT_KEY).assignment_data[year].l = total_count
            if l.hasAttribute(TRUCK_COUNT_KEY):
                if not l.hasAttribute(TRUCK_RESULT_KEY):
                    l.addAttribute(TRUCK_RESULT_KEY,AssignmentDataCollection())
                if not year in l.getAttribute(TRUCK_RESULT_KEY).assignment_data:
                    l.getAttribute(TRUCK_RESULT_KEY).assignment_data[year] = AssignmentData(-1,-1,-1,-1)
                if data[6] == "":
                	truck_count = float(data[7]) + float(data[9]) + float(data[11])
                else:
                	truck_count = float(data[6]) + float(data[7]) + float(data[8])
                if period == "a":
                    l.getAttribute(TRUCK_RESULT_KEY).assignment_data[year].a = truck_count
                if period == "m":
                    l.getAttribute(TRUCK_RESULT_KEY).assignment_data[year].m = truck_count
                if period == "p":
                    l.getAttribute(TRUCK_RESULT_KEY).assignment_data[year].p = truck_count
                if period == "l":
                    l.getAttribute(TRUCK_RESULT_KEY).assignment_data[year].l = truck_count
    
    def writeNetworkDataOfInterest(self,outfile):
        f = open(outfile,"w")
        #type is truck or all, period is am, md, etc
        headers = "anode,bnode,fclass,mpo,new_taz,result_year,count_year,period,type,count,result"
        f.write(headers + "\n")
        result_keys = [TOTAL_RESULT_KEY,TRUCK_RESULT_KEY]
        count_keys = [TOTAL_COUNT_KEY,TRUCK_COUNT_KEY]
        types = ["all","truck"]
        for l in self.links:
            link = self.links[l]
            for i in range(len(result_keys)):
                type_name = types[i]
                result_attribute = result_keys[i]
                count_attribute = count_keys[i]
                if link.hasAttribute(result_attribute):
                    result_data_collection = link.getAttribute(result_attribute)
                    for result_year in result_data_collection.assignment_data:
                        base_entry = str(link.anode) + "," + \
                                     str(link.bnode) + "," + \
                                     str(link.getAttribute("fclass")) + "," + \
                                     str(link.getAttribute("mpo")) + "," + \
                                     str(link.getAttribute("new_taz")) + "," + \
                                     str(result_year) + ","
                        if result_data_collection.assignment_data[result_year].a <> -1:
                            count_data_collection = link.getAttribute(count_attribute)
                            for count_year in count_data_collection.assignment_data:
                                if int(count_data_collection.assignment_data[count_year].a) > 0:
                                    f.write(base_entry + \
                                            str(count_year) + "," + \
                                            "am" + "," + \
                                            type_name + "," + \
                                            str(count_data_collection.assignment_data[count_year].a) + "," + \
                                            str(result_data_collection.assignment_data[result_year].a) + "\n")
                        if result_data_collection.assignment_data[result_year].m <> -1:
                            count_data_collection = link.getAttribute(count_attribute)
                            for count_year in count_data_collection.assignment_data:
                                if int(count_data_collection.assignment_data[count_year].m) > 0:
                                    f.write(base_entry + \
                                            str(count_year) + "," + \
                                            "md" + "," + \
                                            type_name + "," + \
                                            str(count_data_collection.assignment_data[count_year].m) + "," + \
                                            str(result_data_collection.assignment_data[result_year].m) + "\n")
                        if result_data_collection.assignment_data[result_year].p <> -1:
                            count_data_collection = link.getAttribute(count_attribute)
                            for count_year in count_data_collection.assignment_data:
                                if int(count_data_collection.assignment_data[count_year].p) > 0:
                                    f.write(base_entry + \
                                            str(count_year) + "," + \
                                            "pm" + "," + \
                                            type_name + "," + \
                                            str(count_data_collection.assignment_data[count_year].p) + "," + \
                                            str(result_data_collection.assignment_data[result_year].p) + "\n")
                        if result_data_collection.assignment_data[result_year].l <> -1:
                            count_data_collection = link.getAttribute(count_attribute)
                            for count_year in count_data_collection.assignment_data:
                                if int(count_data_collection.assignment_data[count_year].l) > 0:
                                    f.write(base_entry + \
                                            str(count_year) + "," + \
                                            "nt" + "," + \
                                            type_name + "," + \
                                            str(count_data_collection.assignment_data[count_year].l) + "," + \
                                            str(result_data_collection.assignment_data[result_year].l) + "\n")
        f.close()
           
            
        
class Link(object):
    def __init__(self,anode,bnode):
        self.anode = anode
        self.bnode = bnode
        self.__attributes__ = {}
    
    def hasAttribute(self,attribute):
        return attribute in self.__attributes__
    
    def addAttribute(self,attribute,value):
        self.__attributes__[attribute] = value
    
    def getAttribute(self,attribute):
        return self.__attributes__[attribute]

class AssignmentDataCollection(object):
    def __init__(self):
        self.assignment_data = {}
    
    def addAssignmentData(self,year,am_peak,midday,pm_peak,late_night):
        self.assignment_data[year] = AssignmentData(am_peak,midday,pm_peak,late_night)

class AssignmentData(object):
    def __init__(self,am_peak,midday,pm_peak,late_night):
        self.a = am_peak
        self.m = midday
        self.p = pm_peak
        self.l = late_night

if __name__ == "__main__":
    #usage = python ts_count_table_maker_ver3.py base_path scenario_name parent_scenario_name
    base_path = sys.argv[1]
    if base_path[-1] != "\\" and base_path[-1] != "/":
        base_path += "/"
    scenario_name = "SCEN_" + sys.argv[2]
    parent_scenario_name = None
    if len(sys.argv) > 3:
        parent_scenario_name = "SCEN_" + sys.argv[3]
    scenario_path = scenario_name + "/"
    if parent_scenario_name <> None:
        scenario_path = parent_scenario_name + "/" + scenario_name + "/"
    output_path = base_path + "analysis/" + scenario_path
    network_path = base_path + "user_inputs/" + scenario_path + "t0/"
    base_path = base_path + scenario_path
    
    network_not_set = True
    if os.path.isdir(network_path):
        if network_not_set:
            if os.path.isfile(network_path + "../t16/export_07_12.d211"):
                network_file = network_path + "../t16/export_07_12.d211"
                attribute_file = network_path + "../t16/export_07_12.csv"
                network = Network(network_file,attribute_file)
                network.loadCountData(network_path + "countsIds.txt", network_path + "summary_for_ts_truck.txt")
                network_not_set = False
            elif os.path.isfile(network_path + "highway_1991_d211.txt"):
                network_file = network_path + "highway_1991_d211.txt"
                attribute_file = network_path + "highway_1991_attribs.csv"
                network = Network(network_file,attribute_file)
                network.loadCountData(network_path + "countsIds.txt", network_path + "summary_for_ts_truck.txt")
                network_not_set = False
            else:
                print "netowrk files not found in network path: " + network_path
    else:
        print "network path not found: " + network_path
        
    #just try all years
    for i in range(17):
        test_dir = base_path + "t" + str(i)
        if os.path.isdir(test_dir):
            print "traversing year t" + str(i)
            if os.path.isdir(test_dir):
                if network_not_set:
                    continue
                if os.path.isfile(test_dir + "/peakAssignmentResults.csv"):
                    print "tabulating assignment results for t" + str(i)
                    network.loadAssignmentData(str(1990 + i),test_dir + "/peakAssignmentResults.csv",test_dir + "/offpeakAssignmentResults.csv",None,None)
                elif os.path.isfile(test_dir + "/pmpeakAssignmentResults.csv"):
                    print "tabulating assignment results for t" + str(i)
                    network.loadAssignmentData(str(1990 + i),test_dir + "/ampeakAssignmentResults.csv",test_dir + "/mdoffpeakAssignmentResults.csv",test_dir + "/pmpeakAssignmentResults.csv",test_dir + "/ntoffpeakAssignmentResults.csv")
                elif os.path.isfile(test_dir + "/ampeakAssignmentResults.csv"):
                    print "tabulating assignment results for t" + str(i)
                    network.loadAssignmentData(str(1990 + i),test_dir + "/ampeakAssignmentResults.csv",test_dir + "/mdoffpeakAssignmentResults.csv")
    network.writeNetworkDataOfInterest(output_path + "ts_result_count_comparison_data_full.csv")
                    
    