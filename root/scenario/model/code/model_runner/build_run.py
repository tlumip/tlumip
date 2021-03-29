#build_run.py
"""
    usage: python build_run.py model_install_directory scenario_name path_to_model_definition

    Build the configurations and programs for a model run. The model run
    is defined in an external (tsteps.csv) file, and creates log4j and DAF
    configuration files, creates necessary output directories, builds property 
    files defining the model state at each module execution point,  creates 
    a batch file (for running in batch mode) and a command file (for running
    in Python mode), and creates shortcuts to run the model. Also, it logs
    its configuration state in the scenario history file.
    
    crf March 18, 2013
"""

import os,sys,subprocess,time,shutil
from Properties import Properties
import build_daf_setup
import build_log4j_config
import properties_file_creator
from properties_file_creator import PropertyTokens
import module_commands

DEFAULT_EXTRA_ARG_KEY = '**default**'
EXTRA_ARG_LENGTH_KEY = '**length**'

root_dir = sys.argv[1].replace('\\','/')
scenario_name = sys.argv[2]
definition_file = sys.argv[3].replace('\\','/')

years = [] #ordered tyears
module_map = [] #same order as years -> list of modules (not a dict because year may be repeated
module_init_map = [] #same as module map, but used for initialization overriding

#start off by reading in the definition file to form model run structure
header = None
# Initiate tracking of viz final year
viz_final_year = 0
for line in open(definition_file):
    line = line.strip()
    if len(line) == 0:
        continue
    line = map(str.strip,line.split(','))
    if header is None:
        header = line #these are the modules
    else:
        #first column is year
        year = line[0]
        years.append(year)
        m = []
        m0 = []
        for i in range(1,len(line)):
            if line[i] == '1': #1 means run module
                m.append(header[i])
            elif line[i] == 'i': #i means pretend we ran it, but don't run it
                m0.append(header[i])
            if 'VIZ' == header[i]:
                viz_final_year = year
        module_map.append(m)
        module_init_map.append(m0)

#these are the available (base) modules
modules = ['SI','NED','ALD','SPG1','POPSIMSPG1','AA','PT','SPG2','POPSIMSPG2','CT','ET','TA','TR','SL']
daf_modules = ['PT']
t_map = {}
#figure out which modules have "_LAST_RUN_YEAR" tokens in the property file
# that need to be maintained
for module in modules:
    try:
        t_map[module] = getattr(PropertyTokens,module + '_LAST_RUN_YEAR')
    except:
        pass #ignore, because no token for this module

t_previous_map = {'AA' : PropertyTokens.AA_PRIOR_RUN_YEAR} #AA has special second-to-last-year parameter

# # Add the viz final year run property token
# t_map.update({'VIZ': PropertyTokens.VIZ_FINAL_YEAR})

#build extra arguments needed for PT and SL
extra_args_map = {}

pt_map = {}
pt_map[EXTRA_ARG_LENGTH_KEY] = 1
pt_map[1] = [PropertyTokens.PT_LOGSUMS,PropertyTokens.PT_LDT,PropertyTokens.PT_SDT]
pt_map[PropertyTokens.PT_LOGSUMS] = {'LOGSUMS_ONLY':'true' ,'LDT_ONLY':'false',DEFAULT_EXTRA_ARG_KEY:'true'}
pt_map[PropertyTokens.PT_LDT] =     {'LOGSUMS_ONLY':'false','LDT_ONLY':'true' ,DEFAULT_EXTRA_ARG_KEY:'true'}
pt_map[PropertyTokens.PT_SDT] =     {'LOGSUMS_ONLY':'false','LDT_ONLY':'false',DEFAULT_EXTRA_ARG_KEY:'true'}
extra_args_map['PT'] = pt_map

sl_map = {}
sl_map[EXTRA_ARG_LENGTH_KEY] = 1
sl_map[1] = [PropertyTokens.SL_MODE]
sl_mode_map = {}
sl_mode_map['GENERATE_PATHS'] = 'g'
sl_mode_map['GENERATE_SELECT_LINK_DATA'] = 'd'
sl_mode_map['CREATE_SUBAREA_MATRIX'] = 's'
sl_mode_map['APPEND_SELECT_LINK_TO_TRIPS'] = 'a'
sl_mode_map[DEFAULT_EXTRA_ARG_KEY] = 'none' #to throw an error
sl_map[PropertyTokens.SL_MODE] = sl_mode_map
extra_args_map['SL'] = sl_map

def updateTokenMap(module_set,year,token_map):
    """
    Update the current token map with a module that has run in a given year.
    """
    module = module_set[0]
    if module in t_previous_map:
        if t_map[module] in token_map: #if not, then (should be) the first cycle, so no previous available
            token_map[t_previous_map[module]] = token_map[t_map[module]]
    if module in t_map:
        token_map[t_map[module]] = year

def parseModuleName(module):
    """
    Build a "safe" version of the module name, splitting out parameterized levels. Basically splits the name
    by "-" characters, and then replaces all spaces with underscores.
    """
    module_set = map(str.upper,map(str.strip,module.split('-')))
    for i in range(len(module_set)):
        module_set[i] = module_set[i].replace(' ','_')
    return module_set

scenario_dir = os.path.join(root_dir,scenario_name).replace('\\','/')
scenario_inputs = os.path.join(scenario_dir,'inputs').replace('\\','/')
scenario_outputs = os.path.join(scenario_dir,'outputs').replace('\\','/')
config_path = os.path.join(scenario_dir,'model','config').replace('\\','/')

#in some cases, an initial property file definition can be set
initial_token_map_file = os.path.join(config_path,'model_run_initialization.properties')
token_map = Properties()
if os.path.exists(initial_token_map_file):
    token_map.loadPropertyFile(initial_token_map_file)
#update token mapping with overridden initialization
for y in range(len(years)):
    for module_name in module_init_map[y]:
        updateTokenMap(parseModuleName(module_name),years[y],token_map)
        
#add in fixed or initialized tokens
token_map[PropertyTokens.SL_MODE] = 'none'
token_map[PropertyTokens.SCENARIO_NAME] = scenario_name
token_map[PropertyTokens.ROOT_DIR] = root_dir
token_map[PropertyTokens.SCENARIO_INPUTS_DIR] = 'inputs'
token_map[PropertyTokens.SCENARIO_OUTPUTS_DIR] = 'outputs'

# add viz final year token
token_fixed_map = {}
token_fixed_map[PropertyTokens.VIZ_FINAL_YEAR] = viz_final_year
    
#make output dirs if they don't exist
for year in years:
    year_dir = os.path.join(scenario_outputs,'t' + year).replace('\\','/')
    if not os.path.exists(year_dir):
        os.mkdir(year_dir)

#detemplify property file for each module
#save the first one so we can get classpath for other setups
prop_file = None
template_properties = os.path.join(scenario_inputs,'globalTemplate.properties')

#build a list of AA constrained years - kind of hacky to put it here, but we don't want to create a separate module
temp_props = Properties()
temp_props.loadPropertyFile(template_properties)
aa_constrained_years = temp_props['constrained.years'].strip().split(',')

prop_files = [] #we need to save these so that we can avoid naming conflicts
used_prop_files = {}
for y in range(len(years)): #for each year entry (in order) defined in tsteps file
    year = years[y]
    if not year in used_prop_files:
        used_prop_files[year] = []
    property_files = {}
    prop_files.append(property_files)
    #load up all previous years updates
    # redo this every time in case years are run backwards as well
    # don't know why anyone would do this, but crazier stuff has happened
    # this doesn't change functionality, just keeps things consistent
    update_template_properties = Properties()
    for update_year in range(int(year)+1):
        update_template_properties_file = os.path.join(scenario_inputs,'t' + str(update_year),'globalTemplateUpdate.properties')
        if os.path.exists(update_template_properties_file):
            update_template_properties.loadPropertyFile(update_template_properties_file)
    
    token_map[PropertyTokens.CURRENT_T_YEAR] = year

    #set constrained property
    if year in aa_constrained_years:
        token_map[PropertyTokens.AA_CONSTRAINED] = 'true'
    else:
        token_map[PropertyTokens.AA_CONSTRAINED] = 'false'

    for module_name in module_map[y]: #for each (ordered) module in a given year
        module_set = parseModuleName(module_name)
        module = module_set[0]
        if module in extra_args_map: #build extra arguments, if necessary
            for i in range(1,extra_args_map[module][EXTRA_ARG_LENGTH_KEY]+1):
                tokens = extra_args_map[module][i]
                for token in tokens:
                    arg_map = extra_args_map[module][token]
                    if len(module_set) > i and module_set[i] in arg_map:
                        arg = module_set[i]
                    else:
                        arg = DEFAULT_EXTRA_ARG_KEY
                    token_map[token] = arg_map[arg]
        
        #build property file for this module step, and then update token map as if this module had been run
        base_output_properties_file = os.path.join(scenario_outputs,'t' + year,module_commands.formPropertiesFileName(module_set))
        output_properties_file = base_output_properties_file
        cycle = 1
        while output_properties_file in used_prop_files[year]:
            output_properties_file = base_output_properties_file.replace('.properties',str(cycle) + '.properties')
            cycle += 1
        used_prop_files[year].append(output_properties_file)
        property_files[module_name] = output_properties_file
        if prop_file is None:
            prop_file = output_properties_file
        token_fixed_map.update(token_map)
        properties_file_creator.detemplifyFile(template_properties,output_properties_file,token_fixed_map,update_template_properties)
        updateTokenMap(module_set,year,token_map)

#load up a representative property file so we can use the generic (global) properties
properties = Properties()
properties.loadPropertyFile(prop_file)

#collect information from property file, and build config files
cpu_factor = float(properties['cpu.factor'])
use_localhost = properties['use.localhost'].upper() == 'TRUE'
classpath = properties['model.classpath']
java_executable = properties['java.executable']
daf_memory = properties['daf.memory']
daf_admin_port = properties['daf.admin.port']
daf_admin_server_port = properties['daf.admin.server.port']
daf_message_port = properties['daf.message.port']

runscript_file = properties['model.run.bat.file']
command_list_file = properties['model.run.command.file']
logger_program = properties['report.log.program']
report_log_file = properties['report.log.file']
run_out_file = properties['model.run.out.file']
setup_file = properties['model.build.program']
stop_java_program = properties['stop.java.program']
stop_run_program = properties['stop.run.program']
save_history_program = properties['model.save.run.history.program']
run_history_file = properties['model.run.history.file']
settings_base_folder = properties.formPath('scenario.root','_settings')

#build daf and log4j configuration files
daf_file_map = build_daf_setup.buildAll(config_path,cpu_factor,use_localhost,classpath,java_executable,daf_memory,daf_admin_server_port,daf_message_port,daf_admin_port)
log4j_file_map = build_log4j_config.buildAll(scenario_dir)

#build run script
#first, check if daf is needed
file_monitor = False
for y in range(len(years)):
    for module_name in module_map[y]:
        module = parseModuleName(module_name)[0]
        if module in daf_modules:
            file_monitor = True

#rsf is batch file to run model, clf is command file used to run model in Python mode
rsf = open(runscript_file,'wb')
clf = open(command_list_file,'wb')

build_daf_setup.DAF_FILE_KEY
build_daf_setup.PT_DAF_FILE_KEY

commands = module_commands.ModuleCommands(properties,java_executable,classpath,daf_file_map,log4j_file_map)

def dosSafeFile(f):
    """
    Get a filepath safe for use in a dos-prompt style command
    """
    return '"' + f.replace('/','\\') + '"'
    
logger_program = dosSafeFile(logger_program)
report_log = dosSafeFile(report_log_file)
run_out = dosSafeFile(run_out_file)
stop_java_program = dosSafeFile(stop_java_program)
stop_run_program = dosSafeFile(stop_run_program)
save_history_program = dosSafeFile(save_history_program)

def dosCleanCommand(command):
    """
    "Clean" a command set up for batch/dos execution.
    Essentially escapes characters which are reserverd for another use.
    """
    escape_chars = ['^','&','<','>','|']
    for escape_char in escape_chars:
        command = command.replace(escape_char,'^' + escape_char)
    return command

def splitCommand(command):
    """
    
    """
    com = []
    start = 0
    in_quote = False
    for i in range(len(command)):
        if command[i] == ' ':
            if in_quote: #part of argument
                continue
            if start != i: #end of collection
                com.append(command[start:i])
            start = i+1 #start at the next spot
        elif command[i] == '"' and ((not in_quote) or (i == (len(command)-1)) or (command[i+1] == ' ')): #end of quote if end of command or space after quote, otherwise, part of argument
            if in_quote: #end of argument
                com.append(command[start:i])
                in_quote = False
                start = i+1
            else:
                in_quote = True
                start = i+1
    else:
        if start < len(command):
            com.append(command[start:])
    return com

def write_bat_line(line):
    """
    Write a line to the batch command file.
    """
    rsf.write(line + os.linesep)
def write_command_line(type,command,error):
    """
    Write a line to the command file. The command type, and if it runs in error/non-error mode.
    """
    if error is None: #do both
        write_command_line(type,command,True)
        write_command_line(type,command,False)
    else:
        if error:
            type = 'error ' + type
        clf.write(type + ' ' + str(splitCommand(command)) + os.linesep)

def log_bat(message):
    """
    Write a report log call to only the batch command file.
    """
    write_bat_line('cmd /C "' + logger_program + ' ' + message + '>>' + report_log + ' 2>&1"')

def log(message,error=False):
    """
    Write a report log call to both the batch and Python command files.
    """
    log_bat(message)
    write_command_line('log','"' + message + '"',error)

def write_command(command,call=False,error=False):
    """
    Write a command call to both the batch and Python command files.
    If call, then the command will not wait for a finish before executing the
    next command. If error, then the command will only be written if the model
    run is in error mode.
    """
    if call:
        write_bat_line('start "" ' + command)
        write_command_line('start',command,error)
    else:
        write_bat_line(command)
        if (not error is None) and (not error):
            write_bat_line('IF %ERRORLEVEL% NEQ 0 GOTO MODEL_ERROR')
        write_command_line('call',command,error)
        
#Write some overhead for the batch file, then log a model run start
write_bat_line('@ECHO OFF')
write_bat_line('echo. 2>' + report_log)
log('*****Model run started*****')

if file_monitor:
    log('Starting file monitor')
    for command in commands.runFileMonitor():
        write_command(command,True)
if file_monitor:
   for command in commands.runPause(30):
        write_command(command)

write_bat_line('IF EXIST "viz_failed.txt" del -f viz_failed.txt')

#loop over all of the years and modules and write out module commands
for y in range(len(years)):
    year = years[y]
    property_files = prop_files[y]
    for module_name in module_map[y]:
        module_set = parseModuleName(module_name)
        module = module_set[0].upper()
        if ('VIZ' in module):
            command = 'IF EXIST "viz_failed.txt" set ERRORLEVEL=<viz_failed.txt'
            write_command(command)
        log('Starting ' + module + ' in year ' + str(year)) #log module start
        for command in getattr(commands,'run' + module)(module_set,scenario_outputs,property_files[module_name],year,properties):
            if ('VIZ' in module) & (year != viz_final_year):
                write_command(command, call=True, error=True)
            else:
                write_command(command)
        log('Finished ' + module) #log module end
if file_monitor:
    log('Stopping file monitor')
    for command in commands.runStopFileMonitor():
        write_command(command)
    for command in commands.runPause(20):
        write_command(command)
log('*****Model run finished*****')
write_bat_line('GOTO END')
write_bat_line(os.linesep)
write_bat_line(':MODEL_ERROR')
log('*****Model run error (finished abnormally)*****',True)
write_bat_line(os.linesep)
write_bat_line(':END')
write_command('CALL ' + stop_run_program,error=None)

rsf.close()
clf.close()

#save settings and zip up
command = '"' + properties['seven.zip.executable'].replace('/','\\') + '" @type@ -tzip "' + run_history_file.replace('/','\\') + '"'
#check if run_history.zip file exists
if not os.path.exists(run_history_file):
    c = command.replace('@type@','a') + ' "' + properties.formPath('scenario.root','*.notexist').replace('/','\\') + '"'
    subprocess.Popen(c,stdout=subprocess.PIPE,shell=True).wait()
#create temporary settings folder and copy files
settings_folder = os.path.join(settings_base_folder,time.strftime('%d_%m_%Y__%I_%M_%S%p')).replace('/','\\')
os.makedirs(settings_folder)
for f in [runscript_file,command_list_file,template_properties,definition_file]:
    shutil.copyfile(f,os.path.join(settings_folder,os.path.basename(f)))
#zip it and then delete folder
c = command.replace('@type@','u') + ' "_settings"'
c = command.replace('@type@','u') + ' "' + settings_base_folder.replace('/','\\') + '*"'
subprocess.Popen(c,stdout=subprocess.PIPE,shell=True).wait()
shutil.rmtree(settings_base_folder)

#finally, build shortcuts
model_run_batch = []
model_run_batch.append('@ECHO OFF')
model_run_batch.append('pushd "%~dp0"')
#ymm - add the build step to the run file, in case user forgets to run build after changes
model_run_batch.append('cmd /c ""' + setup_file.replace('/','\\') + '" > "' + run_out_file.replace('/','\\') + '" 2>&1"')
model_run_batch.append('cmd /c ""' + runscript_file.replace('/','\\') + '" > "' + run_out_file.replace('/','\\') + '" 2>&1"')

model_run_batch.append('CALL ' + save_history_program)
model_run_batch.append('popd')
model_run_program = open(properties['model.run.program'],'wb')
model_run_program.write(os.linesep.join(model_run_batch) + os.linesep)
model_run_program.close()



model_run_py_program = properties['model.run.python.program'].replace('/','\\')

model_run_py = []
model_run_py.append('@ECHO OFF')
model_run_py.append('pushd "' + os.path.dirname(properties['model.run.python.file']).replace('/','\\') + '"')
model_run_py.append('cmd /c ""' + properties['python.executable'].replace('/','\\') + '" "' + properties['model.run.python.file'].replace('/','\\') + '" "' + command_list_file.replace('/','\\')  +  '" "' + report_log_file.replace('/','\\') + '" > "' + run_out_file.replace('/','\\') + '" 2>&1"')
model_run_py.append('CALL ' + save_history_program)
model_run_py.append('popd')
model_run_py_program = open(properties['model.run.python.program'],'wb')
model_run_py_program.write(os.linesep.join(model_run_py) + os.linesep)
model_run_py_program.close()
