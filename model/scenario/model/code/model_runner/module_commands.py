#ModuleCommands.py
"""
    Class which defines how the actual model modules are run. That is, it creates a list
    of (command line) calls that, when executed in order, will completely run an entire
    module. The methods all return a list of valid command line calls, so composition
    may be achieved by using the '+' operator on the definitions.
    
    crf March 18, 2013
"""

import os
import build_daf_setup
import build_log4j_config
        
def quote(string):
    """
    Quote a string.
    """
    return '"' + string + '"'
    
def normalizeBackslash(string):
    """
    Normalize a (presumed path) to use backslashes for a directory separator.
    """
    return string.replace('/','\\')
    
def normalizeSlash(string):
    """
    Normalize a (presumed path) to use slashes for a directory separator.
    """
    return string.replace('\\','/')
    
def formPropertiesFileName(module_set):
    """
        Form a properties file name from an input name. The file is created by
        replaceing all spaces in module_set with underscores, and appending
        ".properties".
    """
    return ('_'.join(module_set).lower() + '.properties').replace(' ','_')


class ModuleCommands(object):
    """
    This class defines the various command sequences needed to run a module.
    """
    def __init__(self,properties,java_executable,classpath,daf_file_map,log4j_file_map):
        """
        The daf_file_map and log4j_file_map are dicts whose values are files and keys are
        defined by the build_daf_setup.py and build_log4j_config.py scripts.
        """
        self.properties = properties
        self.java_executable = normalizeBackslash(java_executable)
        #need to remove slashes from end of directories because they muck with command line sometimes
        self.classpath = normalizeBackslash(classpath).replace('\\;',';')
        if self.classpath[-1] == '\\':
            self.classpath = self.classpath[:-1]
        self.daf_file_map = daf_file_map
        self.log4j_file_map = log4j_file_map
        
    def conditional(self,predicate,commands):
        """
        Execute commands only if predicate is true.
        """
        if type(commands) == str:
            commands = [commands]
        if_statement = 'IF ' + predicate + ' '
        new_commands = []
        for command in commands:
            new_commands.append(if_statement + command)
        return new_commands
        
    def conditionalIfExists(self,fileOrFolder,commands):
        """
        Execute commands only if fileOrFolder exists.
        """
        return self.conditional('EXIST ' + quote(normalizeBackslash(fileOrFolder)),commands)
        
    def conditionalIfNotExists(self,fileOrFolder,commands):
        """
        Execute commands only if fileOrFolder does not exist.
        """
        return self.conditional('NOT EXIST ' + quote(normalizeBackslash(fileOrFolder)),commands)
        
    def deleteDirectory(self,directory):
        """
        Delete a directory.
        """
        d = quote(normalizeBackslash(directory))
        return self.conditionalIfExists(directory,'RMDIR /S /Q ' + d)
        
    def deleteFile(self,file):
        """
        Delete a File.
        """
        f = quote(normalizeBackslash(file))
        return self.conditionalIfExists(file,'DEL /Q ' + f)
        
    def makeDirectory(self,directory):
        """
        Create a directory.
        """
        d = quote(normalizeBackslash(directory))
        return self.conditionalIfNotExists(directory,'MKDIR ' + d)
        
    def copy(self,directoryOrFile,destination):
        """
        Copy directoryOrFile to destination.
        """
        #for now only does it if destination doesn't exist - delete it first if it does
        o = quote(normalizeBackslash(directoryOrFile))
        d = quote(normalizeBackslash(destination))
        return self.conditionalIfNotExists(destination,'XCOPY ' + o + ' /E /K ' + d)
        
    def zip(self,inputFilesOrFolders,outputFile,update=False):
        """
        Zip inputFilesOrFolders to outputFile. If update, then add to the zip file,
        otherwise overwrite.
        """
        #if just one file/folder listed then put it in a list
        createOrUpdate = 'a'
        if update:
            createOrUpdate = 'u'
        if type(inputFilesOrFolders) == str:
            inputFilesOrFolders = [inputFilesOrFolders]
        command = [quote(normalizeBackslash(self.properties['seven.zip.executable'])),
                   createOrUpdate,
                   '-tzip',
                   quote(normalizeBackslash(outputFile))]
        for f in inputFilesOrFolders:
            command.append(quote(normalizeBackslash(f)))
        return [' '.join(command)]
        
    def getDafCommandFile(self):
        """
        Get the daf command filepath. (Not a command definition.)
        """
        return normalizeSlash(os.path.join(self.properties['daf.command.file.dir'],self.properties['daf.command.file']))
        
    def runPause(self,seconds):
        """
        Pause for seconds.
        """
        return ['cmd /C "ping 127.0.0.1 -n ' + str(seconds+1) + ' > NUL"']
        #return ['ping 127.0.0.1 -n ' + str(seconds+1) + ' > NUL']

    def runFileMonitor(self):
        """
        Run the DAF file monitor.
        """
        startnode_file = normalizeSlash(self.daf_file_map[build_daf_setup.DAF_STARTNODE_0_FILE_KEY])
        command = [quote(self.java_executable),
                   '-cp',
                   quote(self.classpath),
                   quote('-Dlog4j.configuration=' + normalizeSlash(self.log4j_file_map[build_log4j_config.FILE_MONITORY_FILE_KEY])),
                   '-server',
                   'com.pb.common.daf.admin.FileMonitor',
                   quote(self.getDafCommandFile()),
                   quote(startnode_file)]
        return [' '.join(command)]
        
    def runStopFileMonitor(self):
        """
        Stop the DAF file monitor.
        """
        return ['cmd /C "ECHO StopNode>' + quote(self.getDafCommandFile()) + '"',
                self.runPause(10)[0],
                'cmd /C "ECHO StopMonitor>' + quote(self.getDafCommandFile()) + '"']

    def runBootstrapServer(self,properties):
        """
        Run the DAF bootstrap server.
        """
        admin_port = properties['daf.admin.server.port']
        startnode_file = normalizeSlash(self.daf_file_map[build_daf_setup.TS_DAF_STARTNODE_0_FILE_KEY])
        command = [quote(self.java_executable),
                   '-cp',
                   quote(self.classpath),
                   quote('-Dlog4j.configuration=' + normalizeSlash(self.log4j_file_map[build_log4j_config.BOOTSTRAP_SERVER_FILE_KEY])),
                   '-server',
                   'com.pb.common.rpc.DafNode',
                   '-bootstrap',
                   '-port',
                   admin_port,
                   '-command',
                   quote(startnode_file)]
        return [' '.join(command)]

    def runStartBootstrapClient(self,properties):
        """
        Run the DAF bootstrap client.
        """
        admin_port = properties['daf.admin.server.port']
        tsdaf_file = normalizeSlash(self.daf_file_map[build_daf_setup.TS_DAF_FILE_KEY])
        command = [quote(self.java_executable),
                   '-cp',
                   quote(self.classpath),
                   quote('-Dlog4j.configuration=' + normalizeSlash(self.log4j_file_map[build_log4j_config.BOOTSTRAP_CLIENT_FILE_KEY])),
                   '-server',
                   'com.pb.common.rpc.BootstrapClient',
                   '-startAll',
                   '-port',
                   admin_port,
                   '-config',
                   quote(tsdaf_file)]
        return [' '.join(command)]

    def runStopBootstrapClient(self,properties):
        """
        Stop the DAF bootstrap client.
        """
        admin_port = properties['daf.admin.server.port']
        tsdaf_file = normalizeSlash(self.daf_file_map[build_daf_setup.TS_DAF_FILE_KEY])
        command = [quote(self.java_executable),
                   '-cp',
                   quote(self.classpath),
                   quote('-Dlog4j.configuration=' + normalizeSlash(self.log4j_file_map[build_log4j_config.BOOTSTRAP_CLIENT_FILE_KEY])),
                   '-server',
                   'com.pb.common.rpc.BootstrapClient',
                   '-stopAll',
                   '-port',
                   admin_port,
                   '-config',
                   quote(tsdaf_file)]
        return [' '.join(command)]
                              

    def runModule(self,module_set,scenario_outputs,property_file,year,vm_size=None,extra_args={}):
        """
        Run a model module via the ModelEntry Java class. If vm_size is omitted, then 3 gigs will be 
        used. Extra args are the extra arguments passed to the java program in the "K=V" format.
        """
        if vm_size is None: #default is 3 gigs
            vm_size = '3000'
        else:
            vm_size = str(vm_size)
        #property_file = normalizeSlash(os.path.join(scenario_outputs,'t' + year,formPropertiesFileName(module_set)))
        property_file = normalizeSlash(property_file)
        command = [quote(self.java_executable),
                   '-cp',
                   quote(self.classpath),
                   '-Xmx' + vm_size + 'm', #tokenize this
                   quote('-Dlog4j.configuration=' + normalizeSlash(self.log4j_file_map[build_log4j_config.MAIN_EVENT_FILE_KEY])),
                   '-server',
                   'com.pb.tlumip.ao.ModelEntry',
                   module_set[0].upper(),
                   quote('property_file=' + property_file)]
        for key in extra_args:
            command += [quote(key + '=' + extra_args[key])]
        return [' '.join(command)]
        
        
    def runSI(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the SI module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
        
    def runNED(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the NED module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runALD(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the ALD module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runSPG1(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the SPG1 module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runAA(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the AA module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year,250)
        
    def runSPG2(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the SPG2 module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runPT(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the PT module.
        """
        command =  self.runModule(module_set,scenario_outputs,property_file,year,250)
        input_folder = os.path.join(scenario_outputs,'t' + year,'debug')
        output_file = os.path.join(scenario_outputs,'t' + year,'debug.zip')
        command += self.conditionalIfExists(input_folder,self.zip(input_folder,output_file))
        command += self.deleteDirectory(input_folder)
        return command
        
    def runCT(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the CT module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runET(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the ET module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runTA(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the TA module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runTS(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the TS module.
        """
        return self.runStartBootstrapClient(properties) + \
               self.runPause(30) + \
               self.runModule(module_set,scenario_outputs,property_file,year,5000) + \
               self.runStopBootstrapClient(properties)
               
    def runTR(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the TR module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)
        
    def runSL(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the SL module.
        """
        return self.runModule(module_set,scenario_outputs,property_file,year)

    def runVIZ(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the VIZ database creation.
        """
        commands = self.runModule(module_set,scenario_outputs,property_file,year,250)
        if properties['viz.delete.subyear.dbs'].lower().strip() == 'true':
            commands += ['FOR /F "usebackq" %d IN (`dir "' + properties['viz.subyear.dbs.wildcard'].replace('/','\\') + '" /b /s`) DO IF EXIST "%%d" DEL /Q "%%d"']
        if properties['viz.zip.final.db'].lower().strip() == 'true':
            commands += self.zip(properties['viz.final.db'],properties['viz.zip.file'])
        return commands
        
    def runMICROVIZ(self,module_set,scenario_outputs,property_file,year,properties):
        """
        Run the MICROVIZ database creation.
        """
        commands = self.runModule(module_set,scenario_outputs,property_file,year,250)
        if properties['viz.micro.delete.subyear.dbs'].lower().strip() == 'true':
            commands += ['FOR /F "usebackq" %d IN (`dir "' + properties['viz.micro.subyear.dbs.wildcard'].replace('/','\\') + '" /b /s`) DO IF EXIST "%%d" DEL /Q "%%d"']
        if properties['viz.micro.zip.final.db'].lower().strip() == 'true':
            commands += self.zip(properties['viz.micro.final.db'],properties['viz.micro.zip.file'])
        return commands
        
