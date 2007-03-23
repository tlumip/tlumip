from xmlrpclib import ServerProxy
serverConnection = "http://192.168.1.221:8942"
server = ServerProxy(serverConnection)

result = server.getAvailableMachines()
print "list information", result, type(result)
assert result == []