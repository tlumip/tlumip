from xmlrpclib import ServerProxy
import sys, time, os, threading, subprocess, webbrowser

serverConnection = "http://192.168.1.221:8947"
commandDaemon = ServerProxy(serverConnection)

def testConnection():
    assert commandDaemon.checkConnection() == "Connection OK"

pid = None

def testRemoteCommand():
    global pid
    result = commandDaemon.runRemoteCommand(["notepad"])
    pid = result.split()[-1]
    print "Got result pid = [%s]" % pid

def testRemoteKill():
    print commandDaemon.killRemoteCommand(str(pid))

testRemoteCommand()
testRemoteKill()