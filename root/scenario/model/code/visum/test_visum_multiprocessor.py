import os, shutil, sys, csv, time, numpy as np, math
import pandas as pd, subprocess
from SWIM_VISUM_Main import *

if __name__== "__main__":    
    property_file = sys.argv[1]
    min_processors = int(sys.argv[2])
    max_processors = int(sys.argv[3])
    increment_value = int(sys.argv[4])
    runtime_df = pd.DataFrame.from_dict({'NumCores':[i+1 for i in range(min_processors-1, max_processors, increment_value)]})
    runtime_df['Highway (minutes)'] = -1.0
    runtime_df['Transit (minutes)'] = -1.0
    scenario_dir = os.path.abspath(os.path.join(property_file, '../../..'))
    code_dir = os.path.abspath(os.path.join(scenario_dir, 'model', 'code', 'visum'))
    runtime_df = runtime_df.set_index('NumCores')
    s = SwimModel(property_file)    
    script_path = os.path.abspath(os.path.join(s.scenario_root, 'model', 'code', 'visum', 'SWIM_VISUM_Main.py'))
    output_dir = os.path.abspath(s.path)
    test_file = open(os.path.join(s.scenario_root, 'multiprocessor_test_log.txt'), "w")
    for num_processor in range(min_processors-1, max_processors, increment_value):
        print("Running highway assignment with {} processors".format(num_processor+1))
        # Highway run
        start_time = time.time()
        swim_process = subprocess.Popen(['python', script_path, property_file, 'highway', str(num_processor+1)], cwd=code_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
        while True:
            swim_output = swim_process.stdout.readline()
            if swim_output == '' and swim_process.poll() is not None:
                break
            if swim_output:
                # print(swim_output.strip())
                test_file.write(swim_output)
                test_file.flush()
        return_code = swim_process.wait()
        runtime = (time.time()-start_time)/60
        if return_code == 0:
            runtime_df.loc[(num_processor+1),'Highway (minutes)'] = runtime
        # Transit run
        print("Running transit assignment with {} processors".format(num_processor+1))
        start_time = time.time()
        swim_process = subprocess.Popen(['python', script_path, property_file, 'transit', str(num_processor+1)], cwd=code_dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
        while True:
            swim_output = swim_process.stdout.readline()
            if swim_output == '' and swim_process.poll() is not None:
                break
            if swim_output:
                # print(swim_output.strip())
                test_file.write(swim_output)
                test_file.flush()
        return_code = swim_process.wait()
        runtime = (time.time()-start_time)/60
        if return_code == 0:
            runtime_df.loc[(num_processor+1),'Transit (minutes)'] = runtime

    test_file.close()
    runtime_df.to_csv(os.path.join(output_dir, 'multiprocessor_runtimes.csv'))

