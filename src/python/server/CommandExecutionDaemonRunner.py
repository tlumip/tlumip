#!python
"""
CommandExecutionDaemonRunner.py
Runs the CommandExecutionDaemon.py and
Keeps the running version up-to-date.
"""
import os, GetTrueIP, subprocess, time, atexit
from xmlrpclib import ServerProxy
from CommandExecutionDaemon import CommandExecutionDaemonServerXMLRPCPort
serverConnection = "http://" + GetTrueIP.trueIP() + ":" + str(CommandExecutionDaemonServerXMLRPCPort)
commandExecutionDaemonSourcePath = 'Z:/models/tlumip/runtime/server/'

ced = None

def start():
    print "starting daemon"
    try:
        commandExecutionDaemonStartCommand = commandExecutionDaemonSourcePath + "CommandExecutionDaemon.py"
        result = {
            'date' : os.path.getmtime(commandExecutionDaemonStartCommand),
            'proc' : subprocess.Popen(["python", commandExecutionDaemonStartCommand])
        }
    except:
        print "start failed"
        return None
    return result

def shutdown():
    global ced
    daemon = ServerProxy(serverConnection)
    try:
        daemon.terminate() # method on server calls os.abort()
    except:
        print "daemon terminating"
    ced = None

atexit.register(shutdown)

counter = 0

while True:
    if not ced or ced['proc'].returncode: # non-None means process has terminated
        ced = start()
    time.sleep(2)
    counter += 1
    daemon = ServerProxy(serverConnection)
    if counter % 15 == 0:
        print daemon.checkConnection()
    if os.path.getmtime(commandExecutionDaemonStartCommand) != ced['date']:
        shutdown()