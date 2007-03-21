from xmlrpclib import ServerProxy
import sys, time, os, threading, subprocess, webbrowser

serverConnection = "http://192.168.1.221:8942"
server = ServerProxy(serverConnection)
scenarioBase = "90_Base"

def testGetLogFileNames():
    names = server.getLogFileNames(scenarioBase)
    assert names == ['bootstrap_client.log', 'bootstrap_server_athena.log',
        'Dummy.log',
        'fileMonitor_event.log', 'main_event.log',
        'node0_event.log', 'node1_event.log', 'node2_event.log']

def testGetLogData():
    logInfo = server.getLogData(scenarioBase, 'bootstrap_client.log')
    assert logInfo.startswith("14-Mar-2007 19:45:33:711, INFO, reading config file: /models/tlumip/scenario_20070308_EverythingNew/")

def testLargeLogFileRetrieval():
    logInfo = server.getLogData(scenarioBase, 'Dummy.log')
    assert logInfo.startswith("15-Mar-07 14:07, INFO, com.pb.tlumip.ao.ApplicationOrchestrator, Root Directory: /models/tlumip")
    file("LargeLogfile.txt", 'w').write(logInfo)


