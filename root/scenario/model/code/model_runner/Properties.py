"""
    Properties.py
    Property file class.  Can load multiple files and save changed values (if saving allowed).
    Properties can also be loaded in from dicts.  Each property set gets a unique name (property
    file names serve as their set names) and these property groups can be accessed as needed.
    
    crf March 18, 2013
"""
import os

class Properties(dict):
    """
    Extension of a dict which is a representation of a set of properties.
    """
    def __init__(self):
        dict.__init__(self)
        self.set_to_property_map = {}
        self.savable_file_list = []
        self.verbose = False
    
    #doesn't allow spaces in key/value beginnings/ends
    #if property repeats and in saveable file, only last occurance will get written out
    #specifically, the last occurance of a given property is what is used
    #this property allows dynamic reloading of properties
    #if a property is repeated across files/dicts and is written out, the original occurances will 
    #  not be preserved - only the last occurance
    def loadPropertyFile(self,file,saveable=False):
        if not os.path.isfile(file):
            print "Property file not found: " + file
            return
        properties = {}
        for line in open(file):
            line = line.strip()
            if len(line) == 0 or line[0] == "#" or line[0:2] == "//":
                continue
            key_value = line.split("=",1)
            if len(key_value) <> 2:
                print "Property line not understood (" + file + "): \n\t" + line
                continue
            properties[key_value[0].strip()] = key_value[1].strip()
        self.loadProperties(properties,file)
        if saveable:
            self.savable_file_list.append(file)
    
    def loadProperties(self,properties,set_name):
        if set_name in self.set_to_property_map:
            if self.verbose:
                print "Note: Property set already exists, properties will be appended/replaced (" + set_name + ")"
        else:
            self.set_to_property_map[set_name] = []
        for key in properties:
            if self.verbose and key in self:
                print "Note: Property key already defined: " + key
            self[key] = properties[key]
            if not key in self.set_to_property_map[set_name]:
                self.set_to_property_map[set_name].append(key)

    def getSetNames(self):
        return self.set_to_property_map.keys()
    
    def getPropertySet(self,set_name):
        if not set_name in self.set_to_property_map:
            print "Property set not found: " + set_name
            return {}
        set_properties = {}
        for key in self.set_to_property_map[set_name]:
            set_properties[key] = self[key]
        return set_properties
    
    def savePropertyFile(self,file,outfile=None):
        if not file in self.set_to_property_map:
            print "File not loaded property file: " + file
            return
        if outfile == None and not file in self.savable_file_list:
            print "Property file not saveable: " + file
            return
        if outfile == None:
            outfile = file
        f = open(outfile,"w")
        for key in self.getPropertySet(file):
            f.write(key + "=" + self[key] + "\n")
        f.close()
    
    def setVerbose(self,verbose=True):
        self.verbose = verbose
        
    #seems dangerous
    #def saveAllPropertyFiles(self):
        #for file in self.savable_file_list:
            #self.savePropertyFile(file)
    
    def formPath(self,*path_properties):
        #paths = []
        #for prop in path_properties:
            #path = self[prop]
            #if path[-1] == "\\" or path[-1] == "/":
                #path = path[:-1]
            #paths.append(path)
        #return os.sep.join(paths)
        #return "/".join(paths)
        ##will append property as a directory element if not found in properties
        paths = []
        for prop in path_properties:
            if prop in self:
                paths.append(self[prop])
            else:
                paths.append(prop)
        return os.path.join(*paths)
    
    #--parsing methods below--#
    def parseBoolean(self,key):
        value = self[key].lower()
        if value == "true" or value == "t" or value == "1":
            return True
        elif value == "false" or value == "f" or value == "0":
            return False
        else:
            raise ValueError("Cannot parse boolean for key '" + key + "': " + self[key])
    
    def parseInt(self,key):
        try:
            return int(self[key])
        except ValueError:
            raise ValueError("Cannot parse int for key '" + key + "': " + self[key])
    
    def parseFloat(self,key):
        try:
            return float(self[key])
        except ValueError:
            raise ValueError("Cannot parse float for key '" + key + "': " + self[key])
        
            