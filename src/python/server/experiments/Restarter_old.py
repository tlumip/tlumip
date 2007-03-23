#!python
"""
Restarter.py

Demonstration of an XMLRPC server that can restart itself.

"""
import sys, os, GetTrueIP, subprocess, time
from RequestServer import RequestServer

RestarterPort = 8949

#determine if this is a windows box or not
windows = False
if 'OS' in os.environ:
  windows = "windows" in os.environ['OS'].lower()

class Restarter(RequestServer):
  """
  Handle xml-rpc requests
  """
  def __init__(self, ip):
    RequestServer.__init__(self, ip, port = RestarterPort)

  def checkConnection(self):
    """
    For sanity checking
    """
    return "Connection OK"

  def restart(self):
    subprocess.Popen(["python", "Restarter.py"])
    os.abort()

if __name__ == "__main__":
  time.sleep(3)
  print "Running Restarter"
  Restarter("127.0.0.1")
