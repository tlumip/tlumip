from time import time
from wxPython.wx  import *
from pyPgSQL import PgSQL
from xmlrpclib import ServerProxy

import Numeric
import FloatCanvas
import sys


ID_ABOUT_MENU = wxNewId()
ID_EXIT_MENU  = wxNewId()
ID_ZOOM_TO_FIT_MENU = wxNewId()
ID_DRAWTEST_MENU = wxNewId()
ID_LINETEST_MENU = wxNewId()
ID_DRAWMAP_MENU = wxNewId()
ID_CLEAR_MENU = wxNewId()

NODE_CIRCLE_MENUITEM = wxNewId()
NODE_LABEL_MENUITEM = wxNewId()
LINK_LABEL_MENUITEM = wxNewId()

ATTRIB_LENGTH_MENUITEM = wxNewId()
ATTRIB_SPEED_MENUITEM = wxNewId()
ATTRIB_CAPACITY_MENUITEM = wxNewId()
ATTRIB_LANES_MENUITEM = wxNewId()
ATTRIB_TIMAU_MENUITEM = wxNewId()
ATTRIB_VC_MENUITEM = wxNewId()
ATTRIB_PATH1_MENUITEM = wxNewId()

PATH_ORIG = 1739
PATH_DEST = 1714
PATH_TYPE = 3

#object types specific to application
ID_TYPE_CENTROID_NODE = wxNewId()
ID_TYPE_NODE = wxNewId()
ID_TYPE_CENTROID_LINK = wxNewId()
ID_TYPE_LINK = wxNewId()
ID_TYPE_NODE_LABEL = wxNewId()
ID_TYPE_LINK_LABEL = wxNewId()

# latest tlumip network
NODE_DIAMETER = .05

# older tlumip network
NODE_DIAMETER = .001



BACKGROUND_COLOR = "light gray"
NODE_COLOR = "slate blue"
CENTROID_COLOR = "khaki"
#CENTROID_COLOR = BACKGROUND_COLOR
NODE_LABEL_COLOR = "Blue"
LINK_COLOR = "slate blue"

#define fixed lists of 13 items for labels, colors, linewidths.
#attribute index will be 0-12, 0 for centroid connector links
colors = ["light gray","gray","pale green","medium goldenrod","sky blue","maroon","coral","salmon","purple","red","orange","blue","cyan"]
#colors = ["light gray","medium violet red","pale green","medium goldenrod","sky blue","blue violet","coral","salmon","purple","red","orange","maroon","cyan"]
attribs = {}

LineStyles = FloatCanvas.draw_object.LineStyleList.keys()


NODE_TABLE = "nodes"
LINK_TABLE = "links"
cx = PgSQL.connect(user="jhicks", password="cats1761", host="192.168.1.212", database="tlumip")
cu = cx.cursor()


netServer = ServerProxy("http://192.168.1.141:6003")



class DrawFrame(wxFrame):

    """

    A frame used for the FloatCanvas Demo
    
    """
    
    def __init__(self,parent, id,title,position,size):
        wxFrame.__init__(self,parent, id,title,position, size)
        
        ## Set up the MenuBar
        
        MenuBar = wxMenuBar()
        
        file_menu = wxMenu()
        file_menu.Append(ID_EXIT_MENU, "&Close","Close this frame")
        EVT_MENU(self, ID_EXIT_MENU,       self.OnCloseWindow)
        MenuBar.Append(file_menu, "&File")
        
        draw_menu = wxMenu()
        draw_menu.Append(ID_CLEAR_MENU, "&Clear","Clear the Canvas")
        EVT_MENU(self, ID_CLEAR_MENU,self.Clear)
        MenuBar.Append(draw_menu, "&Draw")
        
        view_menu = wxMenu()
        view_menu.Append(ID_ZOOM_TO_FIT_MENU, "Zoom to &Fit","Zoom to fit the window")
        EVT_MENU(self, ID_ZOOM_TO_FIT_MENU,self.ZoomToFit)
        MenuBar.Append(view_menu, "&View")
      
        label_menu = wxMenu()
        self.node_label_menuitem = label_menu.AppendCheckItem(NODE_LABEL_MENUITEM, "&Node Labels", "Label nodes in window")
        EVT_MENU(self, NODE_LABEL_MENUITEM, self.toggleNodeLabelsEnabled)
        self.node_circle_menuitem = label_menu.AppendCheckItem(NODE_CIRCLE_MENUITEM, "&Node Circles", "Show nodes in window")
        EVT_MENU(self, NODE_CIRCLE_MENUITEM, self.toggleNodeCirclesEnabled)
        self.link_label_menuitem = label_menu.AppendCheckItem(LINK_LABEL_MENUITEM, "&Link Labels", "Label links in window")
        EVT_MENU(self, LINK_LABEL_MENUITEM, self.toggleLinkLabelsEnabled)
        MenuBar.Append(label_menu, "&Label")
        
        attributes_menu = wxMenu()
        self.attrib_length_menuitem = attributes_menu.AppendRadioItem(ATTRIB_LENGTH_MENUITEM, "&Length", "Color links by length value")
        EVT_MENU(self, ATTRIB_LENGTH_MENUITEM, self.colorByLength)
        self.attrib_speed_menuitem = attributes_menu.AppendRadioItem(ATTRIB_SPEED_MENUITEM, "&Speed", "Color links by speed value")
        EVT_MENU(self, ATTRIB_SPEED_MENUITEM, self.colorBySpeed)
        self.attrib_capacity_menuitem = attributes_menu.AppendRadioItem(ATTRIB_CAPACITY_MENUITEM, "&Capacity", "Color links by capacity value")
        EVT_MENU(self, ATTRIB_CAPACITY_MENUITEM, self.colorByCapacity)
        self.attrib_lanes_menuitem = attributes_menu.AppendRadioItem(ATTRIB_LANES_MENUITEM, "&Lanes", "Color links by lanes value")
        EVT_MENU(self, ATTRIB_LANES_MENUITEM, self.colorByLanes)
        self.attrib_timau_menuitem = attributes_menu.AppendRadioItem(ATTRIB_TIMAU_MENUITEM, "&Timau", "Color links by timau value")
        EVT_MENU(self, ATTRIB_TIMAU_MENUITEM, self.colorByTimau)
        self.attrib_vc_menuitem = attributes_menu.AppendRadioItem(ATTRIB_VC_MENUITEM, "&Vol/Cap", "Color links by vol/cap value")
        EVT_MENU(self, ATTRIB_VC_MENUITEM, self.colorByVc)
        self.attrib_path1_menuitem = attributes_menu.AppendRadioItem(ATTRIB_PATH1_MENUITEM, "5-6 Paths %d-%d" % (PATH_ORIG, PATH_DEST), "Links in 5-6 paths from %d-%d" % (PATH_ORIG, PATH_DEST))
        EVT_MENU(self, ATTRIB_PATH1_MENUITEM, self.colorPath1)
        MenuBar.Append(attributes_menu, "&Attributes")
        
        help_menu = wxMenu()
        help_menu.Append(ID_ABOUT_MENU, "&About",
                                "More information About this program")
        EVT_MENU(self, ID_ABOUT_MENU,      self.OnAbout)
        MenuBar.Append(help_menu, "&Help")
        
        self.SetMenuBar(MenuBar)
        
        # Other event handlers:
        EVT_CLOSE(self, self.OnCloseWindow)
        
        # Add the Canvas
        self.Canvas = FloatCanvas.FloatCanvas(self,-1,(500,500),
                                  ProjectionFun = None,
                                  Debug = 0,
                                  EnclosingFrame = self,
                                  BackgroundColor = BACKGROUND_COLOR,
                                  UseBackground = 0,
                                  UseToolbar = 1)

        EVT_RIGHT_DOWN(self.Canvas.DrawPanel, self.RightButtonPressedEvent)
        EVT_LEFT_DOWN(self.Canvas.DrawPanel, self.LeftButtonPressedEvent)

        self.CreateStatusBar()
        self.SetStatusText("")
        

        self.Show(True)
        
        return None

        
    def RightButtonPressedEvent(self,event):
        # do something here as needed (mouse over?)
        event.Skip()
        
    def LeftButtonPressedEvent(self,event):
        # do something here as needed (mouse over?)
        event.Skip()
        
    def OnAbout(self, event):
        dlg = wxMessageDialog(self, "This is a small program to demonstrate\n"
                                                  "the use of the FloatCanvas\n",
                                                  "About Me", wxOK | wxICON_INFORMATION)
        dlg.ShowModal()
        dlg.Destroy()
        
    def ZoomToFit(self,event):
        self.Canvas.ZoomToBB()
        
    def Clear(self,event = None):
        self.Canvas.Clear()

        
    def OnCloseWindow(self, event):
        print "NetworkViewer is finished"
        self.Canvas.info.Close()
        self.Canvas.Close()
        self.Destroy()

        
    def DrawTrafficMap(self,event = None):
    
        wxGetApp().Yield()
        self.Clear()
        
        print "reading nodes ..."
        self.nodes = Nodes()
        print "read", len(self.nodes), "nodes.\n"

        print "reading links ..."
        self.links = Links(self.nodes)
        print "read", len(self.links), "links.\n"


        # add node circle and label objects to draw list
        for node in self.nodes.floatCanvasNodes():
            if node[1] == '100':
                color = CENTROID_COLOR
            else:
                color = NODE_COLOR
            self.Canvas.AddCircle(node[0],node[2],node[3],NODE_DIAMETER,Type=ID_TYPE_NODE,LineWidth=1,LineColor=color,FillColor=BACKGROUND_COLOR)
            self.Canvas.AddText(node[0],str(node[0]),node[2],node[3],Type=ID_TYPE_NODE_LABEL,ForeGround=color)

        # add link line objects to draw list
        for segment in self.links.floatCanvasLinks():
            if segment[1] == 1:
                type = ID_TYPE_LINK
                color = LINK_COLOR
            else:
                type = ID_TYPE_CENTROID_LINK
                color = CENTROID_COLOR
            self.Canvas.AddOffsetLine(segment[0], segment[2], NODE_DIAMETER, Type=type, LineColor=color)
            self.Canvas.AddOffsetText(segment[0], str(segment[0]), segment[2], NODE_DIAMETER, Type=ID_TYPE_LINK_LABEL, ForeGround=color)


        print "read static link attributes"
        self.linkTable = self.links.linkTable()
        

        print "updating database ..."

        query = 'DROP TABLE links'
        cu.execute(query)
        
        query = 'CREATE TABLE links ( id INT, type INT, an INT, bn INT, length FLOAT4, speed FLOAT4, capacity FLOAT4, mode VARCHAR(16), vdf INT, lanes FLOAT4, linktype INT, count INT, timau FLOAT4, volau FLOAT4)'
        cu.execute(query)
        
        fileObj = open('/temp/tempfile','w')
        for i in range(0,len(self.linkTable)):
            fileObj.write( "%d,%d,%d,%d,%.3f,%.3f,%.3f,'%s',%d,%.3f,%d,%d,%.3f,%.3f\n" % self.linkTable[i][0] )
        fileObj.close()

        query = "COPY links ( id, type, an, bn, length, speed, capacity, mode, vdf, lanes, linktype, count, timau, volau) FROM '/mnt/zufa/temp/tempfile' WITH CSV"
        cu.execute(query)
        
        
        query = 'COMMIT'
        cu.execute(query)

        
        self.Canvas.SetLinkTable(self.linkTable)

        print "drawing ..."

        self.Canvas.SetDefaultNodeType(ID_TYPE_NODE)
        self.Canvas.SetDefaultLineType(ID_TYPE_LINK)
        self.Canvas.SetDefaultTextType(ID_TYPE_LINK_LABEL)
        self.Canvas.SetDefaultLinkColor(Color=LINK_COLOR)
        self.Canvas.SetVisible(ID_TYPE_NODE_LABEL, False)
        self.Canvas.SetVisible(ID_TYPE_LINK_LABEL, False)

       
        self.Canvas.ZoomToBB()


    def toggleNodeCirclesEnabled(self, event):
        if event.IsChecked():
            self.menuToggler(ID_TYPE_NODE, True)
        else:
            self.menuToggler(ID_TYPE_NODE, False)

    def toggleNodeLabelsEnabled(self, event):
        if event.IsChecked():
            self.menuToggler(ID_TYPE_NODE_LABEL, True)
        else:
            self.menuToggler(ID_TYPE_NODE_LABEL, False)

    def toggleLinkLabelsEnabled(self, event):
        if event.IsChecked():
            self.menuToggler(ID_TYPE_LINK_LABEL, True)
        else:
            self.menuToggler(ID_TYPE_LINK_LABEL, False)

    def menuToggler(self, type, toggleValue):
        self.Canvas.SetVisible(type, toggleValue)
        self.Canvas.Draw()



    def colorByLength(self, event):

        labels = ["","","","","","","","","","","","",""]
        widths = [1,1,1,1,1,1,1,1,1,1,1,1,1]


        freqs = {}        
        for link in self.linkTable:

            id = link[0][0]
            type = link[0][1]
            length = link[0][4]


            if type == 100:
                index = 0
                label = "Centroid"
            elif length > 0 and length < 0.1:
                index = 1
                label = "0 < length < 0.10"
            elif length < 0.50:
                index = 2
                label = "0.10 <= length < 0.50"
            elif length < 1.00:
                index = 3
                label = "0.50 <= length < 1.00"
            elif length < 5.0:
                index = 4
                label = "1.0 <= length < 5.0"
            elif length < 999999999:
                index = 5
                label = "length = 5.0+"
            else:
                index = 6
                label = "length = other"

            attribs[id] = index
            labels[index] = label
                
            value = freqs.get(index)
            if value is None:
                freqs[index] = 1
            else:
                value = freqs.get(index) + 1
                freqs[index] = value

        values = freqs.values()

        self.Canvas.SetAttributes(ID_TYPE_LINK, attribs, labels, colors, widths, values)
        self.Canvas.Draw()
            

    def colorByTimau(self, event):

        labels = ["","","","","","","","","","","","",""]
        widths = [1,1,1,1,1,1,1,1,1,1,1,1,1]


        freqs = {}        
        for link in self.linkTable:

            id = link[0][0]
            type = link[0][1]
            timau = link[0][12]


            if type == 100:
                index = 0
                label = "Centroid"
            elif timau > 0 and timau < 1:
                index = 1
                label = "0 < timau < 1.0"
            elif timau < 5.0:
                index = 2
                label = "1.0 <= timau < 5.0"
            elif timau < 10.00:
                index = 3
                label = "5.0 <= timau < 10.0"
            elif timau < 25.0:
                index = 4
                label = "10.0 <= timau < 25.0"
            elif timau < 999999999:
                index = 5
                label = "timau = 25.0+"
            else:
                index = 6
                label = "timau = other"

            attribs[id] = index
            labels[index] = label
                
            value = freqs.get(index)
            if value is None:
                freqs[index] = 1
            else:
                value = freqs.get(index) + 1
                freqs[index] = value

        values = freqs.values()

        self.Canvas.SetAttributes(ID_TYPE_LINK, attribs, labels, colors, widths, values)
        self.Canvas.Draw()
            

    def colorByVc(self, event):

        labels = ["","","","","","","","","","","","",""]
        widths = [1,1,1,1,1,1,1,1,1,1,1,1,1]


        freqs = {}        
        for link in self.linkTable:

            id = link[0][0]
            type = link[0][1]
            volau = link[0][12]
            lanes = link[0][9]
            capacity = link[0][6]
            
            if lanes > 0 and capacity > 0:
                vc = volau/(lanes*capacity)
            else:
                vc = -1


            if type == 100:
                index = 0
                label = "Centroid"
            elif vc > 0.001 and vc < 0.8:
                index = 1
                label = "0 < Vol/Cap < 0.8"
            elif vc < 1.2:
                index = 2
                label = "0.8 <= Vol/Cap < 1.2"
            elif vc < 2.00:
                index = 3
                label = "1.2 <= Vol/Cap < 2.0"
            elif vc < 999999999:
                index = 4
                label = "Vol/Cap = 2.0+"
            else:
                index = 5
                label = "Vol/Cap = other"

            attribs[id] = index
            labels[index] = label
                
            value = freqs.get(index)
            if value is None:
                freqs[index] = 1
            else:
                value = freqs.get(index) + 1
                freqs[index] = value

        values = freqs.values()

        self.Canvas.SetAttributes(ID_TYPE_LINK, attribs, labels, colors, widths, values)
        self.Canvas.Draw()
            

    def colorBySpeed(self, event):

        colors = ["light gray","pale green","medium goldenrod","sky blue","blue violet","coral","salmon","purple","red","orange","maroon","cyan","medium violet red"]
        labels = ["Centroid",
                    "0 < speed < 10",
                    "10 <= speed < 20",
                    "20 <= speed < 30",
                    "30 <= speed < 40",
                    "40 <= speed < 50",
                    "50 <= speed < 60",
                    "60 <= speed < 70",
                    "speed = 70+",
                    "speed = other",
                    "",
                    "",
                    ""]
        widths = [1,1,1,1,1,1,1,1,2,1,1,1,1]

        
        freqs = {}
        for link in self.linkTable:

            id = link[0][0]
            type = link[0][1]
            speed = link[0][5]

            index = -1
            if type == 100:
                index = 0
            elif speed > 0 and speed < 10:
                index = 1
            elif speed < 20:
                index = 2
            elif speed < 30:
                index = 3
            elif speed < 40:
                index = 4
            elif speed < 50:
                index = 5
            elif speed < 60:
                index = 6
            elif speed < 70:
                index = 7
            elif speed < 999999999:
                index = 8
            else:
                index = 9

            attribs[id] = index

            value = freqs.get(index)
            if value is None:
                freqs[index] = 1
            else:
                value = freqs.get(index) + 1
                freqs[index] = value

        values = freqs.values()
                
        self.Canvas.SetAttributes(ID_TYPE_LINK, attribs, labels, colors, widths, values)
        self.Canvas.Draw()
            

    def colorByLanes(self, event):

        labels = ["","","","","","","","","","","","",""]
        widths = [1,1,1,1,1,1,1,1,1,1,1,1,1]

        
        freqs = {}
        for link in self.linkTable:

            id = link[0][0]
            type = link[0][1]
            lanes = int(link[0][9])


            if type == 100:
                index = 0
                label = "Centroid"
            else:
                index = lanes + 1
                label = str(lanes + 1) + "lanes"

            attribs[id] = index
            labels[index] = label
                
            value = freqs.get(index)
            if value is None:
                freqs[index] = 1
            else:
                value = freqs.get(index) + 1
                freqs[index] = value

        values = freqs.values()

        self.Canvas.SetAttributes(ID_TYPE_LINK, attribs, labels, colors, widths, values)
        self.Canvas.Draw()
            

    def colorByCapacity(self, event):

        labels = ["","","","","","","","","","","","",""]
        widths = [1,1,1,1,1,1,1,1,1,1,1,1,1]

        
        freqs = {}
        for link in self.linkTable:
            capacity = link[0][6]
            value = freqs.get(capacity)
            if value is None:
                freqs[capacity] = 1
            else:
                value = freqs.get(capacity) + 1
                freqs[capacity] = value

        keys = freqs.keys()
        keys.sort()
        
                        
        freqs = {}
        for link in self.linkTable:

            id = link[0][0]
            type = link[0][1]
            capacity = link[0][6]

            if type == 100:
                index = 0
                label = "Centroid"
            else:
                index = keys.index(capacity)
                index = index+1
                label = str(capacity)

            attribs[id] = index
            labels[index] = label

            value = freqs.get(index)
            if value is None:
                freqs[index] = 1
            else:
                value = freqs.get(index) + 1
                freqs[index] = value

        values = freqs.values()
                
        self.Canvas.SetAttributes(ID_TYPE_LINK, attribs, labels, colors, widths, values)
        self.Canvas.Draw()


    def colorPath1(self, event):

        labels = ["","","","","","","","","","","","",""]
        widths = [1,1,1,1,1,3,1,1,1,3,1,1,1]

        
        freqs = {}
        for link in self.linkTable:

            id = link[0][0]
            type = link[0][1]

            if type == 0:
                index = 0
                label = "Centroid"
            else:
                index = 1
                label = "regular link"

            attribs[id] = index
            labels[index] = label
                
            value = freqs.get(index)
            if value is None:
                freqs[index] = 1
            else:
                value = freqs.get(index) + 1
                freqs[index] = value

                
        query = 'select links from paths_%d_%d where vehid=%d and period<4' % (PATH_ORIG, PATH_DEST, PATH_TYPE)
        cu.execute(query)
        resultSet = cu.fetchall()
        
        drawSet = {}
        for result in resultSet:
            for path in result:
                try:
                    for link in path:
                        if drawSet.get(link) is None:
                            drawSet[link] = link
                            
                            index = 5
                            attribs[link] = index
                            labels[index] = "autos 5-6am"

                            value = freqs.get(index)
                            if value is None:
                                freqs[index] = 1
                            else:
                                value = freqs.get(index) + 1
                                freqs[index] = value

                except TypeError:
                    link = path
                    if drawSet.get(link) is None:
                        drawSet[link] = link

        values = freqs.values()

        self.Canvas.SetAttributes(ID_TYPE_LINK, attribs, labels, colors, widths, values)
        self.Canvas.Draw()
                

            


class Node(object):
    def __init__(self, nodeId, xCoord, yCoord):
        self.nodeId = nodeId
        if self.nodeId > 9999: self.type = 1
        else: self.type = 100
        self.xCoord = xCoord
        self.yCoord = yCoord

class Nodes(dict):
    """Reads the nodes from tlumip.ts.NetworkHandler java object"""
    def __init__(self):
        nodeList = netServer.networkDataServer.getNodes()
        nodeXList = netServer.networkDataServer.getNodeX()
        nodeYList = netServer.networkDataServer.getNodeY()
        
        try:
            for k, node in enumerate(nodeList):
                if node > 0:
                    nodeId = node
                    xCoord = nodeXList[k]
                    yCoord = nodeYList[k]
                    self[nodeId] = Node(nodeId, xCoord, yCoord)
        except IndexError, v:
            print "ERROR", v
            print "k=", k, ", id=", id
            sys.exit()

    def floatCanvasNodes(self):
        return [(self[k].nodeId,self[k].type,self[k].xCoord,self[k].yCoord) for k in self]

""" Reads the nodes database table
class Nodes(dict):
    def __init__(self):
        cu.execute( 'SELECT id, xcoord, ycoord FROM ' + NODE_TABLE )
        resultSet = cu.fetchall()
        for tup in resultSet:
            nodeId = tup[0]
            xCoord = tup[2]
            yCoord = tup[3]
            self[nodeId] = Node(nodeId, xCoord, yCoord)

    def floatCanvasNodes(self):
        return [(self[k].nodeId,self[k].type,self[k].xCoord,self[k].yCoord) for k in self]
"""

class Link(object):
    def __init__(self, nodes, id, an, bn, length, mode, vdf, lanes, linktype, speed, capacity, congestedTime, volau):
        self.nodes = nodes
        self.linkID = id
        if an < 10000 or bn < 10000: self.type = 100
        else: self.type = 1
        self.startNode = an
        self.endNode = bn
        self.length = length
        self.speed = speed
        self.capacity = capacity
        self.mode = mode
        self.vdf = vdf
        self.lanes = lanes
        self.linktype = linktype
        self.count = 0
        self.congestedTime = congestedTime
        self.volau = volau
        
    def segment(self):
        start = self.nodes[self.startNode]
        end = self.nodes[self.endNode]
        return [(start.xCoord, start.yCoord), (end.xCoord, end.yCoord)]
        
    def attributes(self):
        return [(self.linkID,self.type,self.startNode,self.endNode,self.length,self.speed,self.capacity,self.mode,self.vdf,self.lanes,self.linktype,self.count,self.congestedTime,self.volau)]

""" Reads the links database table
class Links(dict):
    def __init__(self, nodes):
        cu.execute( 'SELECT id, type, an, bn, length, mode, vdf, lanes, linktype FROM ' + LINK_TABLE )
        resultSet = cu.fetchall()
        for tup in resultSet:
            linkID = tup[0]
            type = tup[1]
            an = tup[2]
            bn = tup[3]
            length = tup[4]
            mode = tup[5]
            vdf = tup[6]
            lanes = tup[7]
            linktype = tup[8]
            self[linkID] = Link(nodes, linkID, type, an, bn, length, mode, vdf, lanes, linktype)

    def linkTable(self):
        return [self[k].attributes() for k in self]

    def floatCanvasLinks(self):
        return [(self[k].linkID, self[k].type, Numeric.array(self[k].segment())) for k in self]
"""

class Links(dict):
    """Reads the links from tlumip.ts.NetworkHandler java object"""
    def __init__(self, nodes):
        indexList = netServer.networkDataServer.getSortedLinkIndexA()
        indexNodeList = netServer.networkDataServer.getIndexNode()
        iaList = netServer.networkDataServer.getIa()
        ibList = netServer.networkDataServer.getIb()
        lengthList = netServer.networkDataServer.getDist()
        modeList = netServer.networkDataServer.getMode()
        vdfList = netServer.networkDataServer.getVdfIndex()
        lanesList = netServer.networkDataServer.getLanes()
        linkTypeList = netServer.networkDataServer.getLinkType()
        speedList = netServer.networkDataServer.getFreeFlowSpeed()
        capacityList = netServer.networkDataServer.getCapacity()
        congestedTimeList = netServer.networkDataServer.getCongestedTime()
        volauList = netServer.networkDataServer.getVolau()
        
        for k, link in enumerate(indexList):
            linkID = indexList[k] + 1
            an = indexNodeList[iaList[linkID-1]]
            bn = indexNodeList[ibList[linkID-1]]
            length = lengthList[k]
            mode = modeList[k]
            vdf = vdfList[k]
            lanes = lanesList[k]
            linktype = linkTypeList[k]
            speed = speedList[k]
            capacity = capacityList[k]
            congestedTime = congestedTimeList[k]
            volau = volauList[k]
            self[linkID] = Link(nodes, linkID, an, bn, length, mode, vdf, lanes, linktype, speed, capacity, congestedTime, volau)

        # get the link counts from a database table
        query = 'select an, bn, count as count96 from atr_counts where year = 1996'
        cu.execute(query)
        resultSet = cu.fetchall()
        
        for result in resultSet:
            id = netServer.networkDataServer.getLinkId(result[0],result[1])
            if id > 0:
                self[id].count = result[2]
                if result[0] == 10540 and result[1] == 10555:
                    print 'an=', result[0], ', bn=', result[1], ', id=', id
            else:
                print 'count record with an=', result[0], ', bn=', result[1], 'does not have matching link in Network.'

    def linkTable(self):
        return [self[k].attributes() for k in self]

    def floatCanvasLinks(self):
        return [(self[k].linkID, self[k].type, Numeric.array(self[k].segment())) for k in self]
        

class MainApp(wxApp):
    
    def OnInit(self):

                
        frame = DrawFrame(None, -1, "Network Viewer",wxDefaultPosition,wxSize(1000,800))
        frame.DrawTrafficMap()

        self.SetTopWindow(frame)

        return True
            

      
if __name__ == "__main__":
    app = MainApp(0)
    app.MainLoop()
