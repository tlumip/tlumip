import csv
import collections

def ident(arg):
    """
    Identity function that simply returns its argument unchanged.
    
    Used as a default value for the transformation arguments to the read functions.
    """
    return arg

def isiter(item):
    return (isinstance(item, collections.Iterable)
            and not isinstance(item, basestring))

def read_list(fname, item_f=ident):
    """
    Reads a csv file as a list.
    
    Regardless of the form of the csv file, every entry in the file is simply assembled in order (row-by-row) into a single list and returned.
    
    This function accepts an optional parameter, which must be callable. It will be applied to each entry in the file before it is stored in the list. This is useful if, for example, the entries represent numbers and should be converted to int.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        result = reduce(lambda ls1, ls2: ls1 + ls2, reader)
        result = [item_f(entry) for entry in result]
    
    return result
    
def read_table(fname, row_f=ident, col_f=ident, value_f=ident, rowh_size=1, colh_size=1):
    """
    Reads a csv table.
    
    The file referred to by fname must be in table format, with column headers in the first row and row headers in the first column. The value in the first row and first column is ignored. This function returns a dictionary from row names to other dictionaries, each from column names to table entries.
    
    If rowh_size is specified, it indicates how many of the first columns should be incorporated into the row header. If greater than 1, the row headers will be tuples containing the values from those columns.
    
    If colh_size is specified, it indicates how many of the first rows should be incorporated into the column header. If greater than 1, the column headers will be tuples containing the values from these rows.
    
    This function accepts three optional callable parameters. The arguments row_f, col_f, and value_f will be applied to each row name, column name, and value, respectively, before it is stored in the dictionary. This is useful if, for example, the values represent numbers and should be converted to int. The row_f function is applied to the entire row header tuple if rowh_size is greater than 1. Similarly, the col_f function is applied to the entire column header tuple if colh_size is greater than 1.
    
    On Python 2.7 and later, the returned dictionaries are all OrderedDicts.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        headers = []
        for i in range(colh_size):
            headers.append(reader.next()[rowh_size:])
        headers = zip(*headers)
        if colh_size == 1:
            headers = [t[0] for t in headers] # Extract from the tuple.
        headers = map(col_f, headers)
        result = _dict()
        for line in reader:
            if rowh_size > 1:
                row_name = row_f(tuple(line[:rowh_size]))
            else:
                row_name = row_f(line[0])
            record = map(value_f, line[rowh_size:])
            result[row_name] = _dict(zip(headers, record))
    
    return result

def read_lintable(fname, row_name, col_name, value_name, row_f=ident, col_f=ident, value_f=ident):
    """
    Reads a table stored as a list in a csv file. (e.g. IJV format)
    
    The file referred to by fname must have at least three columns, one with each of row_name, col_name, and value_name as its header. The file will be read as a list of (row, column, value) triplets. The row and column values of a triplet must serve as a unique identifier for that triplet; if more than one triplet has the same row and column values, the last to appear in the file will overwrite all previous ones. This function returns a dictionary from row names to other dictionaries, each from column names to table entries.
    
    This function accepts three optional parameters, which must be callable. The arguments row_f, col_f, and value_f will be applied to each row name, column name, and value, respectively, before it is stored in the dictionary. This is useful if, for example, the values represent numbers and should be converted to int.
    
    Optionally, value_name can be a list of column names rather than a single name. If so, each value in the dictionary will be a list containing the values in those columns. If value_f is a single function, it is applied to every value; otherwise, it must be a list of functions whose length is the number of value columns, with each function applied to the corresponding column.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        header = reader.next()
        row_col = header.index(row_name)
        col_col = header.index(col_name)
        if isiter(value_name):
            value_col = [header.index(name) for name in value_name]
        else:
            value_col = header.index(value_name)
        result = _read_lintable(reader, row_col, col_col, value_col, row_f, col_f, value_f)
    
    return result

def nh_read_lintable(fname, row_col=0, col_col=1, value_col=2, row_f=ident, col_f=ident, value_f=ident, skip_header=False):
    """
    Reads a table stored as a list in a csv file without headers.
    
    The file referred to by fname must have at least three columns. The file will be read as a list of (row, column, value) triplets; by default, the first column is taken as containing the row names, the second column as the column names, and the third column as the values, but this can be changed by specifying (zero-based) column positions using the arguments row_col, col_col, and value_col. The row and column values of a triplet must serve as a unique identifier for that triplet; if more than one triplet has the same row and column values, the last to appear in the file will overwrite all previous ones. This function returns a dictionary from row names to other dictionaries, each from column names to table entries.
    
    This function accepts three optional callable parameters. The arguments row_f, col_f, and value_f will be applied to each row name, column name, and value, respectively, before it is stored in the dictionary. This is useful if, for example, the values represent numbers and should be converted to int.
    
    Optionally, value_col can be a list of column numbers rather than a single number. If so, each value in the dictionary will be a list containing the values in those columns. If value_f is a single function, it is applied to every value; otherwise, it must be a list of functions whose length is the number of value columns, with each function applied to the corresponding column.
    
    If skip_header is set to True, then the first row will be treated as a header and ignored.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        if skip_header:
            reader.next()
        result = _read_lintable(reader, row_col, col_col, value_col, row_f, col_f, value_f)
    
    return result

def _read_lintable(reader, row_col, col_col, value_col, row_f, col_f, value_f):
    result = _dict()
    for line in reader:
        row = row_f(line[row_col])
        col = col_f(line[col_col])
        if isiter(value_f):
            value = [f(line[x]) for f, x in zip(value_f, value_col)]
        elif isiter(value_col):
            value = [value_f(line[x]) for x in value_col]
        else:
            value = value_f(line[value_col])
        item = result.setdefault(row, _dict())
        item[col] = value
    return result

def read_dict(fname, key_name, value_name, key_f=ident, value_f=ident):
    """
    Reads a dictionary from a csv file.
    
    The file referred to by fname must have at least two columns, one with key_name as its header and the other with value_name as its header. The file will be read as a list of key-value pairs. Each key must appear only once in the file (though values may be repeated); if a duplicate key is present, the last copy to appear in the file will overwrite any previous ones. This function returns a dictionary from keys to values.
    
    The key_name parameter may be an iterable of column names, in which case the values in those columns will be wrapped into tuples to serve as the dictionary keys. The key_f function is applied to these tuples to transform them before inserting them into the dictionary. Similarly, the value_name parameter may be an iterable of column names, whose values are wrapped into tuples and transformed with the value_f function to make the dictionary values.
    
    This function accepts two optional parameters, which must be callable. The arguments key_f and value_f will be applied to each key and value, respectively, before it is stored in the dictionary. This is useful if, for example, the values represent numbers and should be converted to int.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        header = reader.next()
        try:
            key_col = header.index(key_name)
        except ValueError:
            key_col = [header.index(name) for name in key_name]
        try:
            value_col = header.index(value_name)
        except ValueError:
            value_col = [header.index(name) for name in value_name]
        result = _read_dict(reader, key_col, value_col, key_f, value_f)
    
    return result

def nh_read_dict(fname, key_col=0, value_col=1, key_f=ident, value_f=ident, skip_header=False):
    """
    Reads a dictionary from a csv file without headers.
    
    The file referred to by fname must have at least two columns. The file will be read as a list of key-value pairs; by default, the first column is taken as containing the keys and the second as the values, but this can be changed by specifying (zero-based) column positions using the arguments key_col and value_col. Each key must appear only once in the file (though values may be repeated); if a duplicate key is present, the last copy to appear in the file will overwrite any previous ones. This function returns a dictionary from keys to values.
    
    The key_col parameter may be an iterable of column numbers, in which case the values in those columns will be wrapped into tuples to serve as the dictionary keys. The key_f function is applied to these tuples to transform them before inserting them into the dictionary. Similarly, the value_col parameter may be an iterable of column numbers, whose values are wrapped into tuples and transformed with the value_f function.
    
    This function accepts two optional callable parameters. The arguments key_f and value_f will be applied to each key and value, respectively, before it is stored in the dictionary. This is useful if, for example, the values represent numbers and should be converted to int.
    
    If skip_header is set to True, then the first row will be treated as a header and ignored.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        if skip_header:
            reader.next()
        result = _read_dict(reader, key_col, value_col, key_f, value_f)
    
    return result

def _read_dict(reader, key_col, value_col, key_f, value_f):
    result = _dict()
    try:
        iter(key_col) # Throws TypeError if key_col is just a number.
        def create_key(line):
            return key_f(tuple(line[col] for col in key_col))
    except TypeError:
        def create_key(line):
            return key_f(line[key_col])
        
    for line in reader:
        result[create_key(line)] = value_f(line[value_col])
    return result

def read_relation(fname, key_name, value_name, key_f=ident, value_f=ident):
    """
    Reads a relation between two sets from a csv file.
    
    The file referred to by fname must have at least two columns, one with key_name as its header and the other with value_name as its header. The file will be read as a list of key-value pairs. Both keys and values may appear more than once in the file. This function returns a dictionary mapping each key in the file to the set of values it is paired with.
    
    This function accepts two optional parameters, which must be callable. The arguments key_f and value_f will be applied to each key and value, respectively, before it is stored in the dictionary. This is useful if, for example, the values represent numbers and should be converted to int.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        header = reader.next()
        key_col = header.index(key_name)
        value_col = header.index(value_name)
        result = _read_relation(reader, key_col, value_col, key_f, value_f)
    
    return result

def nh_read_relation(fname, key_col=0, value_col=1, key_f=ident, value_f=ident, skip_header=False):
    """
    Reads a relation between two sets from a csv file.
    
    The file referred to by fname must have at least two columns. The file will be read as a list of key-value pairs; by default, the first column is taken as containing the keys and the second as the values, but this can be changed by specifying (zero-based) column positions using the arguments key_col and value_col. Both keys and values may appear more than once in the file. This function returns a dictionary mapping each key in the file to the set of values it is paired with.
    
    This function accepts two optional callable parameters. The arguments key_f and value_f will be applied to each key and value, respectively, before it is stored in the dictionary. This is useful if, for example, the values represent numbers and should be converted to int.
    
    If skip_header is set to True, then the first row will be treated as a header and ignored.
    """
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        if skip_header:
            reader.next()
        result = _read_relation(reader, key_col, value_col, key_f, value_f)
    
    return result
    
def _read_relation(reader, key_col, value_col, key_f, value_f):
    result = _dict()
    for line in reader:
        key = key_f(line[key_col])
        value = value_f(line[value_col])
        if key in result:
            result[key].add(value)
        else:
            result[key] = set([value])
    return result

def write_table(fname, table, corner_label="", rows_sorted=True, cols_sorted=True):
    """
    Writes a csv table.
    
    The fname argument is the filename to write to. The table must be a mapping object from row names to other mapping objects, each from column names to table entries. If the row names are iterables with n elements, then their elements fill the first n columns of the resulting file; if they are single values, then those values fill the first column of the resulting file. Similarly, the column names fill the first rows of the file, and can be iterables or single values.
    
    By default, the row names and column names are in their natural sorted order; if rows_sorted is set to false, the row names are in iteration order instead, and if cols_sorted is set to false, the column names are in the iteration order of the first row. The entry in the first row and first column is corner_label if supplied (otherwise it is blank). It is allowed for corner_label to be an iterable, in which case its elements will attempt to fill any leftover space in the upper-left corner of the table.
    
    All items are converted to strings before being written to the file.
    """
    with open(fname, "w") as file:
        writer = csv.writer(file, ExcelOne)
        if cols_sorted:
            header = sorted(table.itervalues().next())
        else:
            header = table.itervalues().next().keys()
        rowh_size = _max_len(table)
        colh_size = _max_len(header)
        headers = map(list, zip(*map(_pad(colh_size, ""), header)))
        corner_list = list(corner_label)
        for row in headers:
            writer.writerow(map(str, _grab(rowh_size, corner_list, "") + row))
        if rows_sorted:
            rowitems = sorted(table.iteritems(), key=lambda x: x[0])
        else:
            rowitems = table.iteritems()
        for rowlabel, trow in rowitems:
            row = map(str, _pad(rowh_size, "")(rowlabel)
                    + [trow[collabel] for collabel in header])
            writer.writerow(row)

# Finds the longest element of the iterable.
# Elements that are not iterable are considered to have length 1.
# Strings are not considered to be iterable, so they also have length 1.
def _max_len(iterable):
    def screwy_len(item):
        if isiter(item):
            return len(item)
        else:
            return 1
    
    return max(map(screwy_len, iterable))

# Returns a padding function that forces its argument to have the given size.
# If the argument is shorter than size, it is padded with padvalues.
# If the argument is longer than size, the later elements are cut off.
# If the argument is not an iterable, it is padded as if it had size 1.
# Strings are treated as single items rather than iterables.
# The padding function always returns a list.
def _pad(size, padvalue):
    def pad(item):
        if (isinstance(item, collections.Iterable)
                and not isinstance(item, basestring)):
            return list(item)[:size] + [padvalue] * (size - len(item))
        else:
            return [item][:size] + [padvalue] * (size - 1)
    
    return pad

# Pulls num elements off the beginning of the sequence and returns them.
# If there aren't enough elements, the result is padded with padvalues.
def _grab(num, seq, padvalue):
    result = _pad(num, padvalue)(seq[:num])
    del seq[:num]
    return result

def write_lintable(fname, table, row_name, col_name, value_name, rows_sorted=True, cols_sorted=True):
    """
    Writes a table as a list to a csv file.
    
    The resulting file has three columns, with row names in the first column, column names in the second column, and values in the third. The arguments row_name, col_name, and value_name give the headers for these three columns. By default, the rows of the file are in sorted order, first by row name, then by column name. If rows_sorted is set to false, the row names are in iteration order instead, and similarly for cols_sorted.
    """
    with open(fname, "w") as file:
        writer = csv.writer(file, ExcelOne)
        writer.writerow([row_name, col_name, value_name])
        if rows_sorted:
            rowitems = sorted(table.iteritems(), key=lambda x: x[0])
        else:
            rowitems = table.iteritems()
        for rowlabel, trow in rowitems:
            if cols_sorted:
                colitems = sorted(trow.iteritems(), key=lambda x: x[0])
            else:
                colitems = trow.iteritems()
            for collabel, value in colitems:
                writer.writerow([rowlabel, collabel, value])

def _dict(*args, **kwargs):
    try:
        return collections.OrderedDict(*args, **kwargs)
    except AttributeError:
        return dict(*args, **kwargs)

class ExcelOne(csv.excel):
    """
    Define CSV dialect for Excel to avoid blank lines from default \r\n
    """
    lineterminator = "\n"