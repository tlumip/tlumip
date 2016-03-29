from os import path
import subprocess

import retexchange

props_fname = path.join("outputs", "t19", "aa.properties")
year = "t19"

def runAA():
    props = read_props(props_fname)
    
    javacmd = props["aa.command.java"]
    maxheap = props["aa.command.max.heap.size"]
    log4j = props["aa.command.log4j.config.file"]
    classpath = props["aa.command.classpath"]
    classname = props["aa.command.class"]
    
    retexchange.main(year)
    
    # Run AA
    retcode = subprocess.call([javacmd, "-Xmx" + maxheap, "-Dlog4j.configuration=" + log4j,
       "-cp", classpath, "com.hbaspecto.pecas.aa.control.AAControl"])

def read_props(fname):
    props = {}
    with open(fname) as file:
        for line in file:
            if not line.startswith("#") and "=" in line:
                i = line.index("=")
                prop, value = line[:i].strip(), line[i + 1:].strip()
                props[prop] = value
    return props
   
if __name__ == "__main__":
    runAA()