#build_log4j_config.py
"""
    Build the log4j configuration files used in a model run.
    
    crf March 18, 2013
"""


import os

"""
List of logger keys used in configuration file map returned by buildAll function.
"""
ROOT_LOGGER_NAME = 'root'
MAIN_EVENT_FILE_KEY = 'main'
AA_MAIN_EVENT_FILE_KEY = 'aa_main'
FILE_MONITORY_FILE_KEY = 'file_monitor'
NODE_0_FILE_KEY = 'node0'
MAX_FILE_SIZE = '500MB'

class AppenderType:
    """
    The appender type identifier.
    """
    CONSOLE      = 1
    FILE         = 2
    ROLLING_FILE = 3

class LoggerLevel:
    """
    The logging level identifier.
    """
    DEBUG = 1
    INFO  = 2
    WARN  = 3
    ERROR = 4
    FATAL = 5

class AppenderSpec:
    """
    Specification for an appender. If a file type, then a file must be defined, and if
    rolling, then max_file_size defines its rolling cutoff. conversion_pattern is the
    string used to construct a log message (if omitted then a default will be used).
    """
    def __init__(self,type,conversion_pattern=None,file=None,append=True,max_file_size=MAX_FILE_SIZE):
        self.type = type
        self.conversion_pattern = conversion_pattern
        self.file = file
        self.append = append
        self.max_file_size = max_file_size

class LoggerSpec:
    """
    Specification for a logger. A logger has a logging level attached to it and
    a set of appenders it will write to.
    """
    def __init__(self,level,appenders): #appenders is a list of names
        self.level = level
        self.appenders = appenders


def writeLine(f,line):
    """
    Convenience function to write a line to a file (with a line separator).
    """
    f.write(line + os.linesep)

def createLog4jConfig(directory,filename,appender_map,logger_map): 
    """
    Create a log4j configuration file. The filename should just be a name with no extension.
    appender_map maps {appender_name:appender_spec} and logger_map maps {logger_name:logger_spec}.
    If an appender is listed but is not found in the logger_specs, it will not be used.
    Returns the full filepath of the resultant configuration file.
    """
    appenders = {}
    for logger_name in logger_map:
        logger_spec = logger_map[logger_name]
        for appender in logger_spec.appenders:
            appenders[appender] = appender_map[appender]
    
    file = os.path.join(directory,'model','config',filename)
    f = open(file,"wb")
    
    writeLine(f,'<?xml version="1.0"?>')
    writeLine(f,'<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">')
    writeLine(f,'<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">')
    writeLine(f,'')
    
    for appender_name in appenders:
        appender_spec = appenders[appender_name]
        if appender_spec.type == AppenderType.CONSOLE:
            buildConsoleAppender(f,appender_name,appender_spec.conversion_pattern)
        elif appender_spec.type == AppenderType.FILE:
            buildFileAppender(f,appender_name,appender_spec.file,appender_spec.append,appender_spec.conversion_pattern)
        elif appender_spec.type == AppenderType.ROLLING_FILE:
            buildRollingFileAppender(f,appender_name,appender_spec.file,appender_spec.append,appender_spec.conversion_pattern,appender_spec.max_file_size)
    
    for logger_name in logger_map:
        logger_spec = logger_map[logger_name]
        buildLogger(f,logger_name,logger_spec.level,logger_spec.appenders)
        
    writeLine(f,'</log4j:configuration>')
    f.close()
    return os.path.basename(file)

def getDefaultConversionPattern():
    """
    Get the default conversion pattern.
    """
    return '%d{dd-MMM-yyyy HH:mm}, %p, %m%n'

def buildConsoleAppender(f,name,conversion_pattern=None):
    """
    Build a console appender xml definition and add it to the configuration file.
    """
    if conversion_pattern is None:
        conversion_pattern = getDefaultConversionPattern()
    writeLine(f,'    <appender name="' + name + '" class="org.apache.log4j.ConsoleAppender">')
    writeLine(f,'        <layout class="org.apache.log4j.PatternLayout">')
    writeLine(f,'            <param name="ConversionPattern" value="' + conversion_pattern + '"/>')
    writeLine(f,'        </layout>')
    writeLine(f,'    </appender>')

def buildFileAppender(f,name,file,append=True,conversion_pattern=None):
    """
    Build a file appender xml definition and add it to the configuration file.
    """
    if conversion_pattern is None:
        conversion_pattern = getDefaultConversionPattern()
    if append:
        append = 'true'
    else:
        append = 'false'
    writeLine(f,'    <appender name="' + name + '" class="org.apache.log4j.FileAppender">')
    writeLine(f,'        <param name="File" value="' + file.replace('\\','/') + '"/>')
    writeLine(f,'        <param name="Append" value="' + append + '"/>')
    writeLine(f,'        <layout class="org.apache.log4j.PatternLayout">')
    writeLine(f,'            <param name="ConversionPattern" value="' + conversion_pattern + '"/>')
    writeLine(f,'        </layout>')
    writeLine(f,'    </appender>')

def buildRollingFileAppender(f,name,file,append=True,conversion_pattern=None,max_file_size='10MB'):
    """
    Build a rolling file appender xml definition and add it to the configuration file.
    """
    if conversion_pattern is None:
        conversion_pattern = getDefaultConversionPattern()
    if append:
        append = 'true'
    else:
        append = 'false'
    writeLine(f,'    <appender name="' + name + '" class="org.apache.log4j.RollingFileAppender">')
    writeLine(f,'        <param name="File" value="' + file.replace('\\','/') + '"/>')
    writeLine(f,'        <param name="Append" value="' + append + '"/>')
    writeLine(f,'        <param name="MaxFileSize" value="' + max_file_size + '"/>')
    writeLine(f,'        <param name="MaxBackupIndex" value="300"/>') # just set at a big number
    writeLine(f,'        <layout class="org.apache.log4j.PatternLayout">')
    writeLine(f,'            <param name="ConversionPattern" value="' + conversion_pattern + '"/>')
    writeLine(f,'        </layout>')
    writeLine(f,'    </appender>')

def buildLogger(f,name,level,appenders):
    """
    Build logger xml definition and add it to the configuration file.
    """
    if level == LoggerLevel.DEBUG:
        level = 'DEBUG'
    if level == LoggerLevel.INFO:
        level = 'INFO'
    if level == LoggerLevel.WARN:
        level = 'WARN'
    if level == LoggerLevel.ERROR:
        level = 'ERROR'
    if level == LoggerLevel.FATAL:
        level = 'FATAL'
    
    if name == ROOT_LOGGER_NAME:
        writeLine(f,'    <root>')
    else:
        writeLine(f,'    <logger name="' + name + '" additivity="false">')
    
    writeLine(f,'        <level value="' + level + '"/>')
    for appender in appenders:
        writeLine(f,'        <appender-ref ref="' + appender + '"/>')
    
    if name == ROOT_LOGGER_NAME:
        writeLine(f,'    </root>')
    else:
        writeLine(f,'    </logger>')

def buildAll(scenario_directory):
    """
    Build all of the required log4j configuration files. These are (informally):
        
        main event       - main logger
        main event_ aa   - logger used for AA run
        file monitor     - DAF loggers
        node0            -  logger for DAF node 0 processes
    
    This method returns a map {logger_key:logger_configuration_file}, where logger_key
    values are defined at the top of this file.
    """
    config_directory = os.path.join(scenario_directory,"model","config")
    
    #setup the appenders
    console_appender = AppenderSpec(AppenderType.CONSOLE)
    main_event_appender = AppenderSpec(AppenderType.ROLLING_FILE,
                                       file=os.path.join(scenario_directory,'main_event.log'))
    status_appender = AppenderSpec(AppenderType.FILE,
                                   file=os.path.join(scenario_directory,'status.log'),
                                   append=False,
                                   conversion_pattern='%m%n')
    file_monitor_file_appender = AppenderSpec(AppenderType.FILE,
                                              file=os.path.join(scenario_directory,'fileMonitor_event.log'),
                                              append=False)
    node_event_appender = AppenderSpec(AppenderType.ROLLING_FILE,
                                       file=os.path.join(scenario_directory,'node0_event.log'))
    
    #setup the loggers
    console_only_logger = LoggerSpec(LoggerLevel.INFO,('CONSOLE',))
    file_only_logger = LoggerSpec(LoggerLevel.INFO,('FILE',))
    console_file_logger = LoggerSpec(LoggerLevel.INFO,('CONSOLE','FILE'))
    status_logger = LoggerSpec(LoggerLevel.INFO,('STATUS',))

    #build configuration files, and add them to filemap
    file_map = {}
    file_map[MAIN_EVENT_FILE_KEY] = createLog4jConfig(scenario_directory,'info_log4j.xml',
                         {'STATUS'         : status_appender,
                          'FILE'           : main_event_appender,
                          'CONSOLE'        : console_appender},
                         {'status'         : status_logger,
                          ROOT_LOGGER_NAME : console_file_logger})
    file_map[AA_MAIN_EVENT_FILE_KEY] = createLog4jConfig(scenario_directory,'info_log4j_aa.xml',
                         {'STATUS'         : status_appender,
                          'CONSOLE'        : console_appender},
                         {'status'         : status_logger,
                          ROOT_LOGGER_NAME : console_only_logger})
    file_map[FILE_MONITORY_FILE_KEY] = createLog4jConfig(scenario_directory,'info_log4j_fileMonitor.xml',
                         {'FILE'           : file_monitor_file_appender,
                          'CONSOLE'        : console_appender},
                         {ROOT_LOGGER_NAME : file_only_logger})
    file_map[NODE_0_FILE_KEY] = createLog4jConfig(scenario_directory,'info_log4j_node0.xml',
                         {'STATUS'         : status_appender,
                          'FILE'           : node_event_appender,
                          'CONSOLE'        : console_appender},
                         {'status'         : status_logger,
                          ROOT_LOGGER_NAME : console_file_logger})
    return file_map
