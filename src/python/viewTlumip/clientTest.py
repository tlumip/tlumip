from xmlrpclib import ServerProxy
import sys

serverConnection = "http://192.168.1.141:6003"

try:
    server = ServerProxy(serverConnection)
    print server
    
    print "linkCount = ", server.networkDataServer.getLinkCount()
    print "zoneCount = ", server.networkDataServer.getNumCentroids()
    print "userClasses = ", server.networkDataServer.getUserClassStrings()
    
    indexNode = server.networkDataServer.getIndexNode()
    print "indexNode[0:10] = ", indexNode[0:10]
    
#    nodeIndex = server.networkDataServer.getNodeIndex()
#    print "nodeIndex[0:10] = ", nodeIndex[0:10]
    
except Exception, v:
    print "ERROR", v
    sys.exit()
