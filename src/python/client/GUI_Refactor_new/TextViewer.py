#TextViewer.py
#requires a text file as an input, whose format is such that the first line is the title for the
#  window, and the rest is the text to display

import wx, os, sys

#####Gui class for viewing text files
class externalTextPanel(wx.Panel):
  def __init__(self, parent, id):
    global title,sizer
    wx.Panel.__init__(self, parent, id)
    self.mainLabel = wx.StaticText(self, -1, title,wx.Point(20, 3))
    self.mainLabelFont = self.mainLabel.GetFont()
    self.mainLabelFont.SetPointSize(11)
    self.mainLabelFont.SetWeight(wx.FONTWEIGHT_BOLD)
    self.mainLabel.SetFont(self.mainLabelFont)
    self.logTextCtrl = wx.TextCtrl(self, 1, '', wx.Point(1,25), wx.Size(600,400),style=wx.TE_MULTILINE|wx.TE_READONLY|wx.HSCROLL|wx.VSCROLL|wx.TE_RICH2) 
    sizer = wx.BoxSizer(wx.VERTICAL)
    #vertical should have a 28 px border on the bottom
    sizer.Add(self.logTextCtrl,-1,flag=wx.EXPAND|wx.TOP,border=25)
    #spacer window
    self.buttonWindow = wx.Window(self)
    a = sizer.Add(self.buttonWindow,0)
    a.SetInitSize(600,3)
    
    #save output button
    self.saveButton = wx.Button(self, 2, " Save ", wx.Point(140, 3), wx.Size(60,25))
    wx.EVT_BUTTON(self, 2, self.outputSave)
    #hide window button
    self.closeButton = wx.Button(self, 3, " Close ", wx.Point(420, 3), wx.Size(60,25))
    wx.EVT_BUTTON(self, 3, self.closeSelf)
    self.Bind(wx.EVT_CLOSE,self.closeSelf)
    
    
    sizerh = wx.BoxSizer(wx.HORIZONTAL)
    sizerh.Add(wx.Size(60,-1),0,flag=wx.ALIGN_LEFT)
    sizerh.Add(self.saveButton,flag=wx.ALIGN_LEFT)
    sizerh.Add(wx.Size(220,0),1)
    sizerh.Add(self.closeButton,flag=wx.ALIGN_RIGHT)
    sizerh.Add(wx.Size(60,-1),0,flag=wx.ALIGN_RIGHT)
    sizer.Add(sizerh,flag=wx.ALIGN_BOTTOM|wx.EXPAND)
    
    
  def closeSelf(self, event):
    os._exit(0)
  def outputSave(self,event):
    """ Save a file"""
    dlg = wx.FileDialog(self, style=23)
    if dlg.ShowModal() == wx.ID_OK:
      self.logTextCtrl.SaveFile(dlg.GetPath())
    dlg.Destroy()
  def loader(self,filename):
    self.logTextCtrl.LoadFile(filename)
  def remover(self,length):
    self.logTextCtrl.Remove(0,length)

title = ''
inFile = sys.argv[1]

if os.path.exists(inFile):
  counter = 0
  for line in file(inFile):
    if counter == 0:
      title = line.strip()
      firstLine = line
      counter = 1
      break
else:
  print 'file not found!'


extApp = wx.PySimpleApp()
extFrame = wx.Frame(None,-1," " + title,size = (610,480))
extPanel = externalTextPanel(extFrame,-1)
extPanel.loader(inFile)
extPanel.remover(len(firstLine)+1)
#extPanel.loadText(text[0:29614])
extPanel.SetSizerAndFit(sizer)
extPanel.Layout()
extFrame.Show(1)
extApp.MainLoop()
