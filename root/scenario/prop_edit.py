# Utility module for editing a properties file on the fly.

props = {}

def load_props(file_name):
    """Loads the specified properties file into memory.
    
    Ignores all lines that start with a hash symbol or do not contain an equal sign. Raises an IOError if there is a problem reading the file.
    """
    global props
    global contents
    props = {}
    contents = []
    i = 0
    with open(file_name, "r") as file:
        for line in file:
            contents.append(line)
            if not line.startswith("#") and "=" in line:
                parts = line.split("=")
                if len(parts) == 2:
                    props[parts[0].strip()] = i
            i = i + 1

def set_prop(prop_name, new_value):
    """Sets the named property to the specified value.
    
    The old value is returned as a string. The new_value parameter is converted to a string using the normal string conversion rules. Raises KeyError if the property doesn't exist or if the file hasn't been loaded yet.
    """
    global contents
    index = props[prop_name]
    line = contents[index]
    parts = line.split("=")
    name = parts[0].strip()
    old_value = parts[1].strip()
    contents[index] = name + "=" + str(new_value) + "\n"
    return old_value

def save_props(file_name):
    """Saves the modified properties to the specified file.
    
    All comments and other ignored lines will be written back to the file in their original order. Raises an IOError if there is a problem writing the file.
    """
    with open(file_name, "w") as file:
        file.writelines(contents)