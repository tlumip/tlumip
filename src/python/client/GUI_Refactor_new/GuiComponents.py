import wx

LABEL_WIDTH = 120


class NotebookPage(wx.Panel):
    def __init__(self, parent, heading):
        wx.Panel.__init__(self, parent)

        self.SetBackgroundColour('light blue')
        
        pageHeading = wx.StaticText(self, -1, heading)
        pageHeading.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.BOLD))
        self.pageTextArea = wx.TextCtrl(self, -1, '', style=wx.TE_MULTILINE)
        self.pageTextArea.SetBackgroundColour('light blue')
        
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(pageHeading, 0, wx.TOP | wx.BOTTOM, 5)
        sizer.Add(self.pageTextArea, 1, wx.EXPAND)
        self.SetSizer(sizer)
    
    def setPageText(self, fileName):
        f = open(fileName)
        try:
            for line in f:
                self.pageTextArea.WriteText(line)
        finally:
            f.close()


class NotebookPanel(wx.Panel):
    def __init__(self, parent):
        wx.Panel.__init__(self, parent)
    
        # Here we create a panel and a notebook on the panel
        self.nb = wx.Notebook(self)
        self.nb.SetBackgroundColour('light blue')

        # create the page windows as children of the notebook
        #page1 = NotebookPage(self.nb, 'Cluster Machine Consoles')

        # add the pages to the notebook with the label to show on the tab
        #self.nb.AddPage(page1, "")

        # finally, put the notebook in a sizer for the panel to manage the layout
        sizer = wx.BoxSizer()
        sizer.Add(self.nb, 1, wx.EXPAND)
        self.SetSizer(sizer)

    def addPage(self, page, tabString):
        self.nb.AddPage(page, tabString)
        
    def getNbParent(self):
        return self.nb


class TextAreaWithButton(wx.Panel):
    def __init__(self, parent, heading, labels, values, buttonText, buttonCallback, hSpace=10, vSpace=10):
        wx.Panel.__init__(self, parent)
        self.hSpace = hSpace
        self.vSpace = vSpace
        self.textItems = []
        self.textLabels = []
        self.itemIndices = {}
        self.setAreaHeading(heading)
        self.createTextArea(labels, values)
        self.newButton(buttonText, buttonCallback)
        self.layout()
        self.SetBackgroundColour('light blue')

        
    def setAreaHeading(self, heading):
        self.headingItem = wx.StaticText(self, -1, heading)
        self.headingItem.SetFont(wx.Font(11, wx.SWISS, wx.NORMAL, wx.BOLD))

    def addTextItem(self, label, text):
        newItem = wx.StaticText(self, -1, text)
        newItem.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.NORMAL))
        newLabel = wx.StaticText(self, -1, label)
        newLabel.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.NORMAL))
        self.itemIndices[label] = len(self.textItems)
        self.textItems.append(newItem)
        self.textLabels.append(newLabel)
        
    def newButton(self, text, callback):
        id = wx.NewId()
        self.button = wx.Button(self, id, text)
        wx.EVT_BUTTON(self, id, callback)        
        
    def replaceTextItem(self, label, text):
        index = self.itemIndices[label]
        self.textItems[index].SetLabel(text)
        self.layout()

    def createTextArea(self, labels, values):
        for i, l in enumerate(labels):
            self.addTextItem(l, values[i])

    def layout(self):
        sizer = wx.GridBagSizer(self.hSpace, self.vSpace)
        
        # get the width to use for wrapping long text items
        if self.GetSize()[0] > self.headingItem.GetSize()[0]:
            width = self.GetSize()[0] - self.headingItem.GetSize()[0]
        else:
            width = 2*LABEL_WIDTH
        
        sizer.Add(self.headingItem, pos=(0,0), flag=wx.LEFT | wx.TOP | wx.BOTTOM, border=self.vSpace)

        for i, l in enumerate(self.textLabels):
            self.textItems[i].Wrap(width)
            sizer.Add(l, pos=(i+1, 0), flag=wx.ALIGN_LEFT, border=5)
            sizer.Add(self.textItems[i], pos=(i+1, 1), flag=wx.ALIGN_LEFT, border=self.vSpace)
        
        sizer.Add((10,5), pos=(len(self.textLabels)+1, 0), flag=wx.ALL, border=self.vSpace)
        sizer.Add(self.button, pos=(len(self.textLabels)+2, 0), flag=wx.ALIGN_LEFT, border=self.vSpace)
        sizer.Add((10,5), pos=(len(self.textLabels)+3, 0), flag=wx.ALL, border=self.vSpace)
        
        
        sizer.Layout()
        self.SetSizer(sizer)
        
    def infoPrint(self):
        print 'Information for TextArea = %s:' % (self.headingItem.GetLabel())
        print 'Labels = ', [x.GetLabel() for x in self.textLabels]
        print 'Values = ', [x.GetLabel() for x in self.textItems]
        print 'Indices = ', self.itemIndices
        
        
        

class TextArea(wx.Panel):
    def __init__(self, parent, heading, labels, values, hSpace=10, vSpace=10):
        wx.Panel.__init__(self, parent)
        self.hSpace = hSpace
        self.vSpace = vSpace
        self.setAreaHeading(heading)
        self.createTextArea(labels, values)
        self.layout()
        self.SetBackgroundColour('light blue')

        
    def setAreaHeading(self, heading):
        self.headingItem = wx.StaticText(self, -1, heading)
        self.headingItem.SetFont(wx.Font(11, wx.SWISS, wx.NORMAL, wx.BOLD))

    def createTextArea(self, labels, values):
        self.textItems = []
        self.textLabels = []
        self.itemIndices = {}
        for i, l in enumerate(labels):
            self.addTextItem(l, values[i])
        self.layout()

    def addTextItem(self, label, text):
        newItem = wx.StaticText(self, -1, text)
        newItem.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.NORMAL))
        newLabel = wx.StaticText(self, -1, label)
        newLabel.SetFont(wx.Font(10, wx.SWISS, wx.NORMAL, wx.NORMAL))
        self.itemIndices[label] = len(self.textItems)
        self.textItems.append(newItem)
        self.textLabels.append(newLabel)
        
    def replaceTextItem(self, label, text):
        index = self.itemIndices[label]
        self.textItems[index].SetLabel(text)
        self.layout()

    def layout(self):
        sizer = wx.GridBagSizer(self.hSpace, self.vSpace)
        
        # get the width to use for wrapping long text items
        if self.GetSize()[0] > LABEL_WIDTH:
            width = self.GetSize()[0] - LABEL_WIDTH
        else:
            width = 2*LABEL_WIDTH
        
        sizer.Add(self.headingItem, pos=(0,0), flag=wx.LEFT | wx.TOP | wx.BOTTOM, border=self.vSpace)

        for i, l in enumerate(self.textLabels):
            self.textItems[i].Wrap(width)
            sizer.Add(l, pos=(i+1, 0), flag=wx.ALIGN_LEFT | wx.LEFT, border=5)
            sizer.Add(self.textItems[i], pos=(i+1, 1), flag=wx.ALIGN_LEFT | wx.LEFT, border=self.vSpace)
        
        sizer.Layout()
        self.SetSizer(sizer)
        
    def infoPrint(self):
        print 'Information for TextArea = %s:' % (self.headingItem.GetLabel())
        print 'Labels = ', [x.GetLabel() for x in self.textLabels]
        print 'Values = ', [x.GetLabel() for x in self.textItems]
        print 'Indices = ', self.itemIndices
        
        
        

class MainFrame(wx.Frame):
    def __init__(self):
        wx.Frame.__init__(self, None, title="TLUMIP", size=(1200,900))

        # create a menubar
        self.mb = wx.MenuBar()
        self.SetMenuBar(self.mb)
        
        # create a statusbar
        self.sb = wx.StatusBar(self, -1)

        # create a notebook panel
        self.nb = NotebookPanel(self)
        
        
        
    def layoutPanels(self):        
        # text panels
        tSizer = wx.BoxSizer(wx.VERTICAL)
        for item in self.tp:
            tSizer.Add(wx.StaticLine(self), 0, wx.EXPAND | wx.TOP | wx.BOTTOM, 1)
            tSizer.Add(item, 1, wx.EXPAND)
        
        # notbook panel and text panels combined
        pSizer = wx.BoxSizer(wx.HORIZONTAL)
        pSizer.Add(self.nb, 2, wx.EXPAND)
        pSizer.Add(tSizer, 1, wx.EXPAND)

        # add status bar
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(pSizer, 1, wx.EXPAND)
        sizer.Add(self.sb, 0, wx.EXPAND)
        self.SetSizer(sizer)
        
    def getStatusBar(self):
        return self.sb

    def getMenuBar(self):
        return self.mb

    def getNotebook(self):
        return self.nb
        
    def getTextPanels(self):
        return self.tp
        
    def setTextPanels(self, panelList):
        self.tp = panelList
        self.layoutPanels()


if __name__ == "__main__":
    app = wx.App(0) # 0 parameter sends stdout/stderr to console, no parameter sends it to a new window.
    frame = MainFrame()
    frame.Show()
    app.MainLoop()
