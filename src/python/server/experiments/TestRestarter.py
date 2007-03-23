import sys, os, GetTrueIP, subprocess
#from RequestServer import RequestServer
from xmlrpclib import ServerProxy

server = ServerProxy("http://127.0.0.1:8949")
print "proxy", server
print server.checkConnection()
server.restart()
#print server.checkConnection()
