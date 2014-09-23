import os,subprocess,sys,time
"""
    Runs the model using a command list file, which essentially lists
    the sequential command line calls needed to run a model. All commands
    in the file are of the form 
    
        [error] type command_list
    
    where all the commands prefixed by "error" are skipped until (if) any
    of the calls returns abnormally/in error, in which case *only* those
    commands are executed. Type can be "call", "start", or "log". "call"
    waits for the process to finish, and the "start" just starts it and
    moves on to the next command. (A "start" command cannot put this
    script into an "error" mode.) The command list is a string that is
    a list of the program and program arguments that would be correctly
    parsed to a list by Python. Here is an example line:
    
        call ['c:\\some_program.exe','arg1','arg2']
    
    The "log" type logs a message to the report file, and its command list
    only contains one entry (the message).
    
    crf March 18, 2013
"""

def call_program(command_list,wait=False):
    """
    Run the program listed in command_list. If wait, then
    wait for the command to finish and return its return code,
    otherwise return -1.
    """
    for i in range(len(command_list)):
        if command_list[i].find(' ') > -1:
            command_list[i] = '"' + command_list[i] + '"'
    #shell must be true, otherwise shell commands won't run and other expected functionality fails
    p = subprocess.Popen(' '.join(command_list),shell=True)
    result = -1
    if wait:
        p.wait()
        result = p.returncode
    return result

def logit(file,message):
    """
    Log a message to a file (append if it already exists), prefixing 
    the message with a  date/timestamp.
    """
    file.write(time.asctime() + ' - ' + message + os.linesep)

def run_model(command_file,log_file):
    """
    Run the commands in the command_file per the definition set out
    at the beginning of this file. Logging will go to log_file.
    """
    #open log file in preparation of writing...
    log = open(log_file,'wb')
    error = False #in non-error mode to start (we hope!)
    #loop through commands
    for line in open(command_file):
        line = line.strip()
        if len(line) == 0:
            continue
        line = line.split(' ',1)
        type = line[0].lower()
        #run error code if we are in error
        if error:
            if type == 'error':
                line = line[1].split(' ',1)
                type = line[0].lower()
            else:
                continue
        #if in error, then all other commands are skipped
        if type == 'error':
            continue
        command_list = eval(line[1])
        if type == 'start': #start a progam, but  don't wait for a finish
            call_program(command_list)
        elif type == 'call': #start and wait for a finish
            result = call_program(command_list,True)
            if result != 0: #assumes return code is 0 if ok
                error = True
        elif type == 'log': #write message to log file
            logit(log,command_list[0])
            log.flush()
        else:
            print 'unknown command type: ' + ' '.join(line)
    log.close()
    
if __name__ == '__main__':
    command_file = sys.argv[1]
    log_file = sys.argv[2]
    run_model(command_file,log_file)
