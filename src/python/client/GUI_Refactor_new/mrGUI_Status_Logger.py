#mrGUI_Status_Logger.py
import wx

def statusLogger(Mip):
  global mip
  mip = Mip
  mip.statusWindow = wx.TextCtrl(mip, 600, '', wx.Point(10,195), wx.Size(230,255),style=wx.TE_MULTILINE|wx.TE_READONLY|wx.VSCROLL|wx.TE_BESTWRAP) 
  mip.statusWindowName = wx.StaticText(mip, -1, 'Status Window', wx.Point(45,175))
  mip.statusWindowName.SetFont(mip.basicBoxFont)
  mip.statusWindowClearButton = wx.Button(mip, 499, "Clear", wx.Point(175, 175), wx.Size(60,18))
  wx.EVT_BUTTON(mip, 499, clearStatusWindow)

def write(newText):
  global mip
  mip.statusWindow.AppendText(newText)

def clearStatusWindow(event):
  global mip
  mip.statusWindow.Clear()