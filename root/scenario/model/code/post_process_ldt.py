import os
import subprocess
import sys
from os.path import join
import pandas as pd

def main(ldtp_file, ldtv_file):
    ldtp = pd.read_csv(ldtp_file)
    ldtv = pd.read_csv(ldtv_file)
    ldtv = pd.concat([ldtv[ldtv.tripMode!='TRANSIT_WALK'],ldtp[ldtp.tripMode=='TRANSIT_WALK']], axis=0)
    ldtv.to_csv(ldtv_file, index=False)

if __name__ == "__main__":
    # First argument is LDT person trips, and last LDT vehicle trips
    main(sys.argv[1], sys.argv[2])
