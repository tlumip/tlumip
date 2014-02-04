"""
    Build the DAF configuration files for a model run.
    
    crf March 18, 2013
"""

import socket
import os
import time
import multiprocessing

LOCALHOST_IPADDRESS = "127.0.0.1"
CPU_COUNT = multiprocessing.cpu_count()

"""
Keys used for file map returned by buildAll function.
"""
DAF_FILE_KEY = 'daf'
PT_DAF_FILE_KEY = 'pt_daf'
#TS_DAF_FILE_KEY = 'ts_daf'
DAF_STARTNODE_0_FILE_KEY = 'startnode_0'
#TS_DAF_STARTNODE_0_FILE_KEY = 'ts_startnode_0'

def getIpaddress():
    """
    Get the ipaddress for the computer running this script.
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(('8.8.8.8', 80))
    ipaddress = s.getsockname()[0]
    s.close()
    return ipaddress

def getComputerName():
    """
    Get the name of the computer running this script.
    """
    return socket.gethostname()
    
def writeLine(f,line):
    """
    Convenience method to write a line to a file (including a line separator).
    """
    f.write(line + os.linesep)

def generateDafProperties(directory,daf_admin_server_port,daf_message_port,daf_admin_port,use_localhost=True):
    """
    Create a general daf properties file. Returns the final output filepath.
    """
    ipaddress = LOCALHOST_IPADDRESS
    if not use_localhost:
        ipaddress = getIpaddress()
    file = os.path.join(directory,"daf.properties")
    f = open(file,"wb")
    writeLine(f,"# DAF Properties, auto generated on " + time.asctime())
    writeLine(f,"")
    writeLine(f,"# admin settings")
    writeLine(f,"adminServerPort = " + daf_admin_server_port)
    writeLine(f,"adminServerContentDir = /common-daf-v2-tests/src/html")
    writeLine(f,"connectonRetryTime = 1000")
    writeLine(f,"receiveWaitTime = 5000")
    writeLine(f,"defaultQueueSize = 1000")
    writeLine(f,"timeToLive = 150000001")
    writeLine(f,"")
    writeLine(f,"# node definitions")
    writeLine(f,"nodeList = node0")
    writeLine(f,"")
    writeLine(f,"# " + getComputerName())
    writeLine(f,"node0.address = " + ipaddress)
    writeLine(f,"node0.messagePort = " + daf_message_port)
    writeLine(f,"node0.adminPort = " + daf_admin_port)
    writeLine(f,"")
    f.close()
    return file

def formTaskName(base,id):
    """
    Form a task name by combining the base name and a number identifier.
    """
    ids = str(id)
    #if id < 10:
    ids = "0" + ids #this number is used to indicate node number, so must always be zero
    return base + ids

def generatePtDafProperties(directory,classpath,cpu_factor=1):
    """
    Create a PT DAF properties file. This will automatically scale up the daf tasks to (try to) use all
    of the processors available in the system. If cpu_factor != 1, then the number of (effective) processors
    that the tasks will be matched to is actual_processor_count*cpu_factor.
    Returns the final output filepath.
    """
    cpus = max(1,int(round(CPU_COUNT*cpu_factor)))
    file = os.path.join(directory,"ptdaf.properties")
    f = open(file,"wb")
    writeLine(f,"# PT DAF Properties, auto generated on " + time.asctime())
    writeLine(f,"")
    writeLine(f,"classpath = " + classpath)
    writeLine(f,"")
    writeLine(f,"# Task Definitions")
    writeLine(f,"")
    #build a series of tasklists; these orderings and formattings are essentially legacies of traditional
    # formatting; may seem strange but when output is fairly logical
    tasklist1 = "taskList = MasterTask, ModeChoiceWriterTask1, DestinationChoiceWriterTask, ResultsWriterTask,LongDistanceWorkerTask,"
    tasklist2 = "           SReaderTask0,"
    tasklist3 = "          "
    tasklist4 = "           MSServerTask0,"
    tasklist5 = "          "
    tasklist6 = "          "
    #one of each of these tasks for the "factored cpus"
    for i in range(1,1+cpus):
        tasklist2 += formTaskName("MCWorkerTask",i) + ","
        tasklist3 += formTaskName("DCWorkerTask",i) + ","
        tasklist5 += formTaskName("PNWorkerTask",i) + ","
        tasklist6 += formTaskName("HHWorkerTask",i) + ","
    writeLine(f,tasklist1 + "\\")
    writeLine(f,tasklist2 + "\\")
    writeLine(f,tasklist3 + "\\")
    writeLine(f,tasklist4 + "\\")
    writeLine(f,tasklist5 + "\\")
    writeLine(f,tasklist6[:-1]) #remove final comma
    writeLine(f,"")
    
    writeLine(f,"# Node 0 Tasks")
    writeLine(f,"MasterTask.className = com.pb.models.pt.daf.PTMasterTask")
    writeLine(f,"MasterTask.queueName = TaskMasterQueue")
    writeLine(f,"MasterTask.nodeName = node0")
    writeLine(f,"")
    writeLine(f,"ModeChoiceWriterTask1.className = com.pb.models.pt.daf.MCWriterTask")
    writeLine(f,"ModeChoiceWriterTask1.queueName = ModeChoiceWriterQueue")
    writeLine(f,"ModeChoiceWriterTask1.nodeName = node0")
    writeLine(f,"")
    writeLine(f,"DestinationChoiceWriterTask.className = com.pb.models.pt.daf.DCWriterTask")
    writeLine(f,"DestinationChoiceWriterTask.queueName = DestinationChoiceWriterQueue")
    writeLine(f,"DestinationChoiceWriterTask.nodeName = node0")
    writeLine(f,"")
    writeLine(f,"ResultsWriterTask.className = com.pb.models.pt.daf.PTResultsWriterTask")
    writeLine(f,"ResultsWriterTask.queueName = ResultsWriterQueue")
    writeLine(f,"ResultsWriterTask.nodeName = node0")
    writeLine(f,"")
    writeLine(f,"LongDistanceWorkerTask.className = com.pb.models.pt.daf.LongDistanceWorker")
    writeLine(f,"LongDistanceWorkerTask.queueName = LongDistanceWorkQueue")
    writeLine(f,"LongDistanceWorkerTask.nodeName = node0")
    writeLine(f,"")
    
    writeLine(f,"SReaderTask0.className = com.pb.models.pt.daf.SkimsReaderTask")
    writeLine(f,"SReaderTask0.queueName = SR_node0Queue")
    writeLine(f,"SReaderTask0.nodeName = node0")
    writeLine(f,"")
    for i in range(1,1+cpus):
        writeLine(f,formTaskName("MCWorkerTask",i) + ".className = com.pb.tlumip.pt.daf.TLUMIPMCLogsumCalculatorTask")
        writeLine(f,formTaskName("MCWorkerTask",i) + ".queueName = MC_node0WorkQueue")
        writeLine(f,formTaskName("MCWorkerTask",i) + ".nodeName = node0")
        writeLine(f,"")
        writeLine(f,formTaskName("DCWorkerTask",i) + ".className = com.pb.models.pt.daf.DCLogsumCalculatorTask")
        writeLine(f,formTaskName("DCWorkerTask",i) + ".queueName = DC_node0WorkQueue")
        writeLine(f,formTaskName("DCWorkerTask",i) + ".nodeName = node0")
        writeLine(f,"")
    writeLine(f,"MSServerTask0.className = com.pb.tlumip.pt.daf.TLUMIPMicroSimulationServer")
    writeLine(f,"MSServerTask0.queueName = MS_node0WorkQueue")
    writeLine(f,"MSServerTask0.nodeName = node0")
    writeLine(f,"")
    for i in range(1,1+cpus):
        writeLine(f,formTaskName("PNWorkerTask",i) + ".className = com.pb.tlumip.pt.daf.TLUMIPWorkplaceLocationTask")
        writeLine(f,formTaskName("PNWorkerTask",i) + ".queueName = " + formTaskName("PN_",i) + "WorkQueue")
        writeLine(f,formTaskName("PNWorkerTask",i) + ".nodeName = node0")
        writeLine(f,"")
        writeLine(f,formTaskName("HHWorkerTask",i) + ".className = com.pb.tlumip.pt.daf.TLUMIPMicroSimulationWorkerTask")
        writeLine(f,formTaskName("HHWorkerTask",i) + ".queueName = " + formTaskName("HH_",i) + "WorkQueue")
        writeLine(f,formTaskName("HHWorkerTask",i) + ".nodeName = node0")
        writeLine(f,"")
    
    writeLine(f,"# Queue Definitions")
    queuelist1 = "queueList = TaskMasterQueue,ModeChoiceWriterQueue,DestinationChoiceWriterQueue,ResultsWriterQueue,LongDistanceWorkQueue,"
    queuelist2 = "            SR_node0Queue,MC_node0WorkQueue,DC_node0WorkQueue,MS_node0WorkQueue,"
    queuelist3 = ""
    queuelist4 = ""
    for i in range(1,1+cpus):
        queuelist3 += formTaskName("PN_",i) + "WorkQueue,"
        queuelist4 += formTaskName("HH_",i) + "WorkQueue,"
    writeLine(f,queuelist1 + "\\")
    writeLine(f,queuelist2 + "\\")
    writeLine(f,queuelist3 + "\\")
    writeLine(f,queuelist4[:-1]) #remove final comma
    writeLine(f,"")
    
    writeLine(f,"# Node 0 Queues")
    writeLine(f,"")
    writeLine(f,"TaskMasterQueue.nodeName = node0")
    writeLine(f,"TaskMasterQueue.size = 1000")
    writeLine(f,"")
    writeLine(f,"ModeChoiceWriterQueue.nodeName = node0")
    writeLine(f,"ModeChoiceWriterQueue.size = 1500")
    writeLine(f,"")
    writeLine(f,"DestinationChoiceWriterQueue.nodeName = node0")
    writeLine(f,"DestinationChoiceWriterQueue.size = 1500")
    writeLine(f,"")
    writeLine(f,"ResultsWriterQueue.nodeName = node0")
    writeLine(f,"ResultsWriterQueue.size = 1500")
    writeLine(f,"")
    writeLine(f,"LongDistanceWorkQueue.nodeName = node0")
    writeLine(f,"LongDistanceWorkQueue.size = 1500")
    writeLine(f,"")
    writeLine(f,"SR_node0Queue.nodeName = node0")
    writeLine(f,"SR_node0Queue.size = 1000")
    writeLine(f,"")
    writeLine(f,"MC_node0WorkQueue.nodeName = node0")
    writeLine(f,"MC_node0WorkQueue.size = 1000")
    writeLine(f,"")
    writeLine(f,"DC_node0WorkQueue.nodeName = node0")
    writeLine(f,"DC_node0WorkQueue.size = 1000")
    writeLine(f,"")
    writeLine(f,"MS_node0WorkQueue.nodeName = node0")
    writeLine(f,"MS_node0WorkQueue.size = 1000")
    writeLine(f,"")
    for i in range(1,1+cpus):
        writeLine(f,formTaskName("PN_",i) + "WorkQueue.nodeName = node0")
        writeLine(f,formTaskName("PN_",i) + "WorkQueue.size = 1000")
        writeLine(f,"")
        writeLine(f,formTaskName("HH_",i) + "WorkQueue.nodeName = node0")
        writeLine(f,formTaskName("HH_",i) + "WorkQueue.size = 1000")
        writeLine(f,"")
    f.close()
    return file

def generateTsDafProperties(directory,tsdaf_message_port,use_localhost=True):
    """
    Create a main DAF (v3) property file for the TS module. Returns the final output filepath.
    """
    ipaddress = LOCALHOST_IPADDRESS
    if not use_localhost:
        ipaddress = getIpaddress()
    file = os.path.join(directory,"tsdaf.groovy")
    f = open(file,"wb")
    writeLine(f,"// TS DAF Properties, auto generated on " + time.asctime())
    writeLine(f,"")
    writeLine(f,'//Define node URLs')
    writeLine(f,'nodes[0].name = "node0"')
    writeLine(f,'nodes[0].url = "tcp://' + ipaddress + ':' + tsdaf_message_port + '"')
    writeLine(f,'')
    writeLine(f,'//Define handler classes that are rpc-end points; These handlers will be started prior to running the application')
    writeLine(f,'handlers[0].name = "networkHandler"')
    writeLine(f,'handlers[0].className = "com.pb.tlumip.ts.NetworkHandler"')
    writeLine(f,'handlers[0].node = "node0"')
    writeLine(f,'')
    writeLine(f,'handlers[1].name = "demandHandler"')
    writeLine(f,'handlers[1].className = "com.pb.tlumip.ts.DemandHandler"')
    writeLine(f,'handlers[1].node = "node0"')
    writeLine(f,'')
    writeLine(f,'handlers[2].name = "spBuildLoadHandler_node1"')
    writeLine(f,'handlers[2].className = "com.pb.tlumip.ts.SpBuildLoadHandler"')
    writeLine(f,'handlers[2].node = "node0"')
    writeLine(f,"")
    f.close()
    return file

def generateDafStartnodeProperties(directory,classpath,java_executable="java",process_memory="10000m"):
    """
    Create a DAF startnode file. Returns the final output filepath.
    """
    file = os.path.join(directory,"startnode0.properties")
    f = open(file,"wb")
    writeLine(f,java_executable)
    writeLine(f,"-classpath")
    writeLine(f,classpath)
    writeLine(f,"-Xms" + process_memory)
    writeLine(f,"-Xmx" + process_memory)
    writeLine(f,"-DnodeName=node0")
    writeLine(f,"-Dlog4j.configuration=info_log4j_node0.xml")
    writeLine(f,"com.pb.common.daf.admin.StartNode")
    f.close()
    return file

def generateTsDafStartnodeProperties(directory,classpath,java_executable="java",process_memory="18000m"):
    """
    Create a DAF (v3) startnode file for the TS module. Returns the final output filepath.
    """
    file = os.path.join(directory,"startnode0.daf3")
    f = open(file,"wb")
    writeLine(f,java_executable)
    writeLine(f,"-classpath")
    writeLine(f,classpath)
    writeLine(f,"-Xms" + process_memory)
    writeLine(f,"-Xmx" + process_memory)
    writeLine(f,"-DnodeName=node0")
    writeLine(f,"-Dlog4j.configuration=info_log4j_node0.xml")
    writeLine(f,"com.pb.common.rpc.DafNode")
    writeLine(f,"-node")
    writeLine(f,"node0")
    writeLine(f,"-config")
    writeLine(f,os.path.join(directory,"tsdaf.groovy").replace("\\","/"))
    writeLine(f,"")
    f.close()
    return file

def buildAll(directory,cpu_factor,use_localhost,classpath,java_executable,daf_memory,tsdaf_memory,daf_admin_server_port,daf_message_port,daf_admin_port,tsdaf_message_port):
    """
    Build all of the required DAF configuration files. These are (informally):
        
        daf.properties
        ptdaf.properties
        tsdaf.groovy
        startnode0.properties
        startnode0.daf3
    
    This method returns a map {daf_file_key:file}, where daf_file_key values are defined at the top of this file.
    """
    file_map = {}
    file_map[DAF_FILE_KEY] = generateDafProperties(directory,daf_admin_server_port,daf_message_port,daf_admin_port,use_localhost)
    file_map[PT_DAF_FILE_KEY] = generatePtDafProperties(directory,classpath,cpu_factor)
    #file_map[TS_DAF_FILE_KEY] = generateTsDafProperties(directory,tsdaf_message_port,use_localhost)
    file_map[DAF_STARTNODE_0_FILE_KEY] = generateDafStartnodeProperties(directory,classpath,java_executable,daf_memory)
    #file_map[TS_DAF_STARTNODE_0_FILE_KEY] = generateTsDafStartnodeProperties(directory,classpath,java_executable,tsdaf_memory)
    return file_map
