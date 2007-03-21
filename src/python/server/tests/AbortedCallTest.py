from xmlrpclib import ServerProxy
import sys, time, os, threading
"""
Destroying this program before it completes causes the server to throw an
exception only at the point that it tries to return a value.

The threading approach didn't seem to so work on this test.
"""

serverConnection = "http://192.168.1.221:8942"


server = ServerProxy(serverConnection)
result = server.longTestCall()
"""

class longCall(threading.Thread):
  def run(self):
    server = ServerProxy(serverConnection)
    result = server.longTestCall()

t = longCall()
t.setDaemon(True)
t.start()
"""