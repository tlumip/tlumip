# Utility routines for model calibration/utility scripts

import shutil
from os import path
import errno

# Determines the file name that would be used as the backup by the backup()
# function, without actually backing up the file.
def backup_name(fname, itnum):
    return suffix_name(fname, "_" + str(itnum) + "_")

# Creates an iteration-tagged backup of the specified file.
# The backup has the iteration number, bracketed by underscores,
# between the base file name and the extension.
def backup(fname, itnum):
    shutil.copy(fname, backup_name(fname, itnum))

# Inserts the specified suffix into the given filename immediately before
# the dot and extension. For example, suffix_name("foo.txt", "bar") returns
# "foobar.txt".
def suffix_name(fname, suffix):
    bits = fname.split(".")
    if len(bits) == 1:
        return fname + suffix
    else:
        return ".".join(bits[0:-1]) + suffix + "." + bits[-1]

# "Inclusive range" - both start and stop are inclusive, which is more intuitive
# for some applications.
def irange(start, stop):
    return range(start, stop + 1)

# Returns the indices of the specified values in the specified list.
# Useful for finding column positions in a CSV file with headers.
# Not optimized in any way, so only good for small lists.
def indices(alist, *values):
    return [alist.index(value) for value in values]

# Converts a string to a bool the way you'd expect, rather than the stupid
# way that the built-in bool() function does it.
def tobool(string):
    lower = string.lower()
    if lower == "true":
        return True
    elif lower == "false":
        return False
    else:
        raise ValueError(string)

# Takes a nested dictionary and flips the order of the outermost two nests.
# For example, if ndict["foo"]["bar"] == "spam" in the original dictionary,
# then flipkeys(ndict)["bar"]["foo"] == "spam". The result is always a plain
# Python dictionary, regardless of the class of the argument.
def flipkeys(ndict):
    result = {}
    for key1, indict in ndict.iteritems():
        for key2, value in indict.iteritems():
            inresult = result.setdefault(key2, {})
            inresult[key1] = value
    return result

# Opens a file for writing, as if by open(fname, "w"). If the file cannot be
# opened (because it is locked or it is a directory, for example), then this
# function will try appending " (1)" to the filename. If that also fails, then
# it will try appending " (2)", and so on. Eventually, this function will
# return a valid file object. This function will immediately raise an IOError
# if the directory being written to does not exist.
# Set binary=True to use binary mode ("wb") instead of text mode.
def smart_open(fname, binary=False):
    mode = "wb" if binary else "w"
    try:
        return open(fname, mode)
    except IOError as e:
        if e.errno != errno.EACCES:
            raise e
        i = 1
        while True:
            bracname = suffix_name(fname, " (" + str(i) + ")")
            try:
                return open(bracname, mode)
            except IOError:
                if e.errno != errno.EACCES:
                    raise e
                i += 1

# An unmodifiable dictionary-like object that maps everything to itself.
# It "contains" all possible elements.
# Retrieving or iterate over the keys, values, or items is undefined,
# since an IdDict "contains" infinitely many elements in an unspecified order.
class IdDict(object):
    def __getitem__(self, key):
        return key
    
    def __contains__(self, key):
        return True