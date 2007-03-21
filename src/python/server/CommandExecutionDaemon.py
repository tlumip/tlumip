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
""".split()

currentlyRunningCommands = {}

import sys, os, GetTrueIP, subprocess
from RequestServer import RequestServer

# Test flag determined by existence of "test" file in local directory:
test = os.path.exists("test")
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
    pid = subprocess.Popen(cmdlist).pid
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

  def getPIDList(self):
    return "unimplimented"

if __name__ == "__main__":
  ipAddress = GetTrueIP.trueIP()
  print "Test is " + str(test)
  CommandExecutionDaemonServer(ipAddress)
