#!python
"""
CommandExecutionDaemon.py

Runs a limited set of commands on this machine, called remotely through XMLRPC.

"""

legalCommands = """
  dir
  cmd
  start
  python
  ping
  notepad
  ant
  type
""".split()

currentlyRunningCommands = {}

import sys, os, GetTrueIP, subprocess
from RequestServer import RequestServer
CommandExecutionDaemonServerXMLRPCPort = 8947

#determine if this is a windows box or not
windows = False
if 'OS' in os.environ:
  windows = "windows" in os.environ['OS'].lower()

class CommandExecutionDaemonServer(RequestServer):
  """
  Handle xml-rpc requests
  """
  def __init__(self, ip):
    RequestServer.__init__(self, ip, port = CommandExecutionDaemonServerXMLRPCPort)

  def checkConnection(self):
    """
    For sanity checking
    """
    return "Connection OK"

  def runRemoteCommand(self, cmdlist):
    if cmdlist[0] not in legalCommands: return "ERROR: Illegal command " + cmdlist[0]
    #print "Executing", " ".join(cmdlist)
    try:
        pid = subprocess.Popen(cmdlist, shell=True).pid
    except Exception, e:
        s = "Subprocess Popen failed " + str(e)
        print s
        return s
    #print "pid:", pid
    currentlyRunningCommands[str(pid)] = cmdlist
    return "Remote Command Started with pid: " + str(pid)

  def killRemoteCommand(self, pid):
    if not str(pid) in currentlyRunningCommands:
        return "ERROR: pid [%s] not in running list %s" % (pid, str(currentlyRunningCommands))
    if windows:
        result = os.system('TASKKILL /F /T /PID ' + str(pid))
    else:
        result = os.kill(pid) #### TODO: This may not kill all the child processes
    del currentlyRunningCommands[pid]
    return result

  def getProcessList(self):
    if windows:
    	proc = ["tasklist"]
    else:
    	proc = ["ps"]
    return subprocess.Popen(proc, stdout=subprocess.PIPE).communicate()[0]

if __name__ == "__main__":
  ipAddress = GetTrueIP.trueIP()
  print "CommandExecutionDaemonServer running"
  CommandExecutionDaemonServer(ipAddress)
