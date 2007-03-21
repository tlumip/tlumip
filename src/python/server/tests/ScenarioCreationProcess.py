from xmlrpclib import ServerProxy
import sys, time, os, threading

serverConnection = "http://192.168.1.221:8942"

server = ServerProxy(serverConnection)

testDirName2 = sys.argv[1]

file("processStarted.txt", 'w').write("started")
result = server.createScenario(testDirName2, "1", "1990",
    os.environ.get("username"), "Separate Process")
file("processfinished.txt", 'w').write(str(result))