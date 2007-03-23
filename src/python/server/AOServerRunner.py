#!python
"""
AOServerRunner.py
Runs the TLUMIP_ApplicationOrchestratorServer.py and
Keeps the running version up-to-date.
"""
import os, GetTrueIP, subprocess, time, atexit
from xmlrpclib import ServerProxy
from TLUMIP_ApplicationOrchestratorServer import ApplicationOrchestratorServerXMLRPCPort
serverConnection = "http://" + GetTrueIP.trueIP() + ":" + str(ApplicationOrchestratorServerXMLRPCPort)

aos = None

def start():
    print "starting server"
    return {
        'date' : os.path.getmtime("TLUMIP_ApplicationOrchestratorServer.py"),
        'proc' : subprocess.Popen(["python", "TLUMIP_ApplicationOrchestratorServer.py"])
    }

def shutdown():
    global aos
    server = ServerProxy(serverConnection)
    try:
        server.terminate() # method on server calls os.abort()
    except:
        print "server terminating"
    aos = None

atexit.register(shutdown)

while True:
    if not aos or aos['proc'].returncode: # non-None means process has terminated
        aos = start()
    time.sleep(1)
    #server = ServerProxy(serverConnection)
    #print server.checkConnection()
    if os.path.getmtime("TLUMIP_ApplicationOrchestratorServer.py") != aos['date']:
        shutdown()