#!python
"""
Restarter.py

Keeps a server running.

Also keeps it up-to-date with a master copy.

"""
import sys, os, GetTrueIP, subprocess, time
from RequestServer import RequestServer

#determine if this is a windows box or not
windows = False
if 'OS' in os.environ:
    windows = "windows" in os.environ['OS'].lower()
print "windows =", windows
if windows:
  	proc = ["tasklist"]
else:
	  proc = ["ps"]
print "proc =", proc
while True:
    time.sleep(1)
    tasks = subprocess.Popen(proc, stdout=subprocess.PIPE).communicate()[0]
    for t in tasks.split("\n"):
        if "python" in t or "Restarter" in t:
            print t