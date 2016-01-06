# Script to iterate between Household Option Size Calibration and Weighted Floorspace Calibration.

import os
from os import path
import math
import shutil
import householdOptionSizeCalib as hosc
import weightfscalib as wfc

dir = "Iterative"
hosc_base = "HOSC_Baseline"
hosc_final = "HOSC_Final"
hosc_dir = "HOSC_#" # Where to put backed-up HOSC files. The # symbol will be replaced by the current iteration.

hosc_setup_files = [] # HOSC input files that need to be restored before each run; file given by first element of each tuple gets copied to the path given by the second element.

hosc_status = path.join("ResOptSizeResults", "OptSizeCheck.csv") # Where to find the HOSC status file.
hosc_files = [path.join("inputs", "parameters", "TechnologyOptionsI.csv"), path.join("outputs","t19", "aa.properties")] # HOSC files to back up every iteration.
hosc_it_files = [path.join("inputs", "parameters", "TechnologyOptionsI_#_.csv"), path.join("ResOptSizeResults", "OptSizeCheck_#_.csv"), path.join("outputs","t19", "TechnologyChoice_#_.csv"), path.join("outputs","t19", "ActivityLocations_#_.csv"), "event_#_.log"] # HOSC backup files that need to be backed up every iteration. The # symbol will be replaced by every HOSC iteration number.

wfc_base = "WFC_Baseline"
wfc_final = "WFC_Final"
wfc_dir = "WFC_#" # Where to put backed-up WFC files. The # symbol will be replaced by the current iteration.

wfc_setup_files = [] # WFC input files that need to be restored before each run; file given by first element of each tuple gets copied to the path given by the second element.

wfc_status = path.join("FloorSpaceResults", "FloorspaceCalib.csv") # Where to find the WFC status file.
wfc_files = [path.join("outputs","t19", "FloorspaceI.csv"), wfc_status, path.join("outputs","t19", "aa.properties")] # WFC files to back up every iteration.
wfc_it_files = [path.join("outputs","t19", "FloorspaceI_#_.csv"), path.join("outputs","t19", "ExchangeResults_#_.csv"), "event_#_.log"] # WFC backup files that need to be backed up every iteration. The # symbol will be replaced by every WFC iteration number.

base_setup_files = [] # Default input files that need to be restored before the baseline and final confirmation runs; file given by first element of each tuple gets copied to the path given by the second element.

numits = 5

# Values controlling the progressive increase in the number of iterations done by HOSC and WFC. In the it1-th iteration, HOSC will perform it1_hosc_its iterations, and WFC will perform it1_wfc_its iterations. Similarly for it2 and it3. This script will determine all other iteration counts by fitting a rectangular hyperbola (reciprocal function) through the three points given.
it1 = 0
it1_hosc_its = 2
it1_wfc_its = 4
it2 = 2
it2_hosc_its = 3
it2_wfc_its = 5
it3 = 4
it3_hosc_its = 5
it3_wfc_its = 8

# Initial convergence criteria for AA.
hosc_init_spec_clear = 10
hosc_init_total_clear = 10
wfc_init_spec_clear = 10
wfc_init_total_clear = 10

# Final convergence criteria for AA.
hosc_final_spec_clear = 0.03
hosc_final_total_clear = 0.0002
wfc_final_spec_clear = 0.03
wfc_final_total_clear = 0.0002

def make_clear(dir):
    if not path.exists(dir):
        os.mkdir(dir)
    for sub in os.listdir(dir):
        os.remove(path.join(dir, sub))

def make_paths():
    if not path.exists(dir):
        os.mkdir(dir)
    dirbase = path.join(dir, hosc_base)
    make_clear(dirbase)
    dirfinal = path.join(dir, hosc_final)
    make_clear(dirfinal)
    for i in range(numits):
        itd = hosc_dir.replace("#", str(i))
        diritd = path.join(dir, itd)
        make_clear(diritd)
            
    dirbase = path.join(dir, wfc_base)
    make_clear(dirbase)
    dirfinal = path.join(dir, wfc_final)
    make_clear(dirfinal)
    for i in range(numits):
        itd = wfc_dir.replace("#", str(i))
        diritd = path.join(dir, itd)
        make_clear(diritd)

def backup_hosc(dir, its):
    # Backup single files.
    shutil.copy(hosc_status, dir)
    for file in hosc_files:
        shutil.copy(file, dir)
    # Backup iterated files.
    for file in hosc_it_files:
        for i in range(its):
            shutil.copy(file.replace("#", str(i)), dir)
            
def backup_wfc(dir, its):
    # Backup single files.
    shutil.copy(wfc_status, dir)
    for file in wfc_files:
        shutil.copy(file, dir)
    # Backup iterated files.
    for file in wfc_it_files:
        for i in range(its):
            shutil.copy(file.replace("#", str(i)), dir)

def backup_hosc_status(dir):
    shutil.copy(hosc_status, dir)
    for file in hosc_files:
        shutil.copy(file, dir)

def backup_wfc_status(dir):
    shutil.copy(wfc_status, dir)
    for file in wfc_files:
        shutil.copy(file, dir)

def main():
    # Set up directories.
    make_paths()
    
    # Make hyperbolas.
    hosc_hyp = Hyperbola(it1, it1_hosc_its, it2, it2_hosc_its, it3, it3_hosc_its)
    wfc_hyp = Hyperbola(it1, it1_wfc_its, it2, it2_wfc_its, it3, it3_wfc_its)
    hosc_max_its = hosc_hyp(numits - 1)
    wfc_max_its = wfc_hyp(numits - 1)
    
    # Run HOSC and WFC once each with calibration turned off to get baseline values.
    for pair in base_setup_files:
        shutil.copy(*pair)
    hosc.get_status()
    backup_hosc_status(path.join(dir, hosc_base))
    wfc.get_status()
    backup_wfc_status(path.join(dir, wfc_base))
    
    hosc_spec_factor = (hosc_final_spec_clear / hosc_init_spec_clear) ** (1.0 / (hosc_max_its - 1))
    hosc_total_factor = (hosc_final_total_clear / hosc_init_total_clear) ** (1.0 / (hosc_max_its - 1))
    wfc_spec_factor = (wfc_final_spec_clear / wfc_init_spec_clear) ** (1.0 / (wfc_max_its - 1))
    wfc_total_factor = (wfc_final_total_clear / wfc_init_total_clear) ** (1.0 / (wfc_max_its - 1))
    
    for i in range(numits):
        print "Starting HOSC-WFC iteration " + str(i)
        # Set run parameters.
        hosc_its = hosc_hyp(i)
        wfc_its = wfc_hyp(i)
        hosc_spec_clear = hosc_init_spec_clear * (hosc_spec_factor ** (hosc_its - 1))
        hosc_total_clear = hosc_init_total_clear * (hosc_total_factor ** (hosc_its - 1))
        wfc_spec_clear = wfc_init_spec_clear * (wfc_spec_factor ** (wfc_its - 1))
        wfc_total_clear = wfc_init_total_clear * (wfc_total_factor ** (wfc_its - 1))
        
        # Run HOSC.
        for pair in hosc_setup_files:
            shutil.copy(*pair)
        hosc.main(hosc_its, True, hosc_init_spec_clear, hosc_spec_clear, hosc_init_total_clear, hosc_total_clear)
        
        # Back up output files produced by HOSC.
        itd = path.join(dir, hosc_dir.replace("#", str(i)))
        backup_hosc(itd, hosc_its)
        
        # Run WFC.
        for pair in wfc_setup_files:
            shutil.copy(*pair)
        wfc.main(wfc_its, True, wfc_init_spec_clear, wfc_spec_clear, wfc_init_total_clear, wfc_total_clear)
        
        # Back up output files produced by WFC.
        itd = path.join(dir, wfc_dir.replace("#", str(i)))
        backup_wfc(itd, wfc_its)
        print "Finished HOSC-WFC iteration " + str(i)
        
    # Run HOSC and WFC once each with calibration turned off to get final confirmation values.
    for pair in base_setup_files:
        shutil.copy(*pair)
    hosc.get_status()
    backup_hosc_status(path.join(dir, hosc_final))
    wfc.get_status()
    backup_wfc_status(path.join(dir, wfc_final))
    
    print "Done!"

class Hyperbola:
    def __init__(self, x1, y1, x2, y2, x3, y3):
        x1, x2, x3, y1, y2, y3 = float(x1), float(x2), float(x3), float(y1), float(y2), float(y3)
        x21 = x2 - x1
        x32 = x3 - x2
        x13 = x1 - x3
        denom = y1 * x32 + y2 * x13 + y3 * x21
        self.x0 = (x1 * y1 * x32 + x2 * y2 * x13 + x3 * y3 * x21) / denom
        self.y0 = - (y1 * y2 * x21 + y2 * y3 * x32 + y3 * y1 * x13) / denom
        self.k = x21 * x32 * x13 * (y2 - y1) * (y3 - y2) * (y1 - y3) / denom ** 2
    
    def __call__(self, itnum):
        return int(math.floor(self.k / (self.x0 - itnum) + self.y0))
        
if __name__ == "__main__":
    main()