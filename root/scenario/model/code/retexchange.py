"""Script to caculate size terms for retail commodities based on floorspace.
"""

import sys
import os
from os.path import join
import csv

# File names.

# Name of file with TAZ->LUZ correspondence.
zone_fname = join("outputs", "%year%", "alpha2beta.csv")
# Name of floorspace file.
flspace_fname = join("outputs", "%year%", "FloorspaceI.csv")
# Name of file that gives retail floorspace types for each retail commodity.
retail_types_fname = join("inputs", "parameters", "ExchangeSizeTermTypes.csv")
# Name of the base size term file.
exchange_base_fname = join("inputs", "parameters", "ExchangeImportExportI.csv")
# Name of file where size terms should be stored.
exchange_fname = join("outputs", "%year%", "ExchangeImportExportI.csv")

# Column name constants.
zone_taz_col = "Azone"
zone_luz_col = "Bzone"
flspace_taz_col = "taz"
flspace_type_col = "commodity"
flspace_amount_col = "quantity"
retail_commod_col = "Commodity"
retail_space_col = "Floorspace"
exchange_luz_col = "ZoneNumber"
exchange_commod_col = "Commodity"
exchange_size_cols = ["BuyingSize", "SellingSize"]

class excelOne(csv.excel):
    # define CSV dialect for Excel to avoid blank lines from default \r\n
    lineterminator = "\n"

def read_zones(fname):
    """Reads the TAZ-->LUZ correspondence file.
    
    Returns a dictionary from each TAZ number to the corresponding LUZ number.
    """
    zones = {}
    with open(fname, "rb") as file:
        reader = csv.reader(file)
        header = reader.next()
        for row in reader:
            taz = int(float(row[header.index(zone_taz_col)]))
            luz = int(float(row[header.index(zone_luz_col)]))
            zones[taz]=luz
    return zones

def read_retail_types(fname):
    """Reads the retail commodities and their corresponding retail floorspace types.
    
    Returns a dictionary from each retail commodity to the list of floorspace types that are
    considered suitable space for retail of that commodity.
    """
    types = {}
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        header = reader.next()
        for row in reader:
            commod = row[header.index(retail_commod_col)]
            space = row[header.index(retail_space_col)]
            if commod in types:
                types[commod].append(space)
            else:
                types[commod] = [space]
    return types

def read_flspace(fname):
    """Reads the current floorspace quantities.
    
    Returns a dictionary from each TAZ-space type pair to the amount of floorspace of that type in that TAZ.
    """
    space = {}
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        header = reader.next()
        for row in reader:
            taz = int(float(row[header.index(flspace_taz_col)]))
            type = row[header.index(flspace_type_col)]
            amount = float(row[header.index(flspace_amount_col)])
            key = (taz, type)
            if key not in space:
                space[key] = amount
    return space

def read_exchange(fname):
    """Reads the current exchange/import/export file.
    
    Returns the file header as a list of strings, and a dictionary from each LUZ-commodity pair
    (including the dummy LUZ -1) to the row specified for that pair. Each dictionary value is the entire row
    as a list of strings.
    """
    exchange = {}
    with open(fname, "rU") as file:
        reader = csv.reader(file)
        header = reader.next()
        for row in reader:
            luz = int(row[header.index(exchange_luz_col)])
            commod = row[header.index(exchange_commod_col)]
            key = (luz, commod)
            if key not in exchange:
                exchange[key] = row
    return header, exchange

def write_exchange(fname, header, exchange):
    """Writes out the modified exchange/import/export file.
    
    The header and exchange arguments should be in the same format as returned by read_exchange(). Specifically:
    fname -- the name of the output file
    header -- the file header as a list of strings
    exchange -- a dictionary from each LUZ-commodity pair (including the dummy LUZ -1) to the row
        specified for that pair. Each dictionary value is the entire row as a list of strings.
    
    The rows are emitted in sorted order: first, all dummy rows in alphabetical order by commodity name, then
    all regular rows in alphabetical order by commodity name and then numerical order by LUZ number.
    """
    
    with open(fname, "w") as file:
        writer = csv.writer(file, excelOne)
        writer.writerow(header)
        
        def get_key(key):
            luz, commod = key
            if luz < 0:
                return luz, commod.lower()
            else:
                return 0, commod.lower(), luz
        
        keys = sorted(exchange, key=get_key)
        for key in keys:
            writer.writerow(exchange[key])

###################################################################
#                              MAIN                               #
###################################################################
def main(year):
    # Read in TAZ->LUZ correspondences.
    zones = read_zones(zone_fname.replace("%year%",str(year)))
    
    # Read in retail commodities and floorspace types.
    retail_types = read_retail_types(retail_types_fname)
    
    # Read in floorspace amounts.
    flspace = read_flspace(flspace_fname.replace("%year%", str(year)))
    
    # Read in templates from ExchangeImportExportI file.
    header, exchange = read_exchange(exchange_base_fname)
    
    # Add size terms in ExchangeImportExportI.
    for commod, fltypes in retail_types.items():
        # Zero out size terms for existing rows.
        for luz in set(zones.values()):
            key = (luz, commod)
            if key in exchange:
                row = exchange[key]
                for col in exchange_size_cols:
                    row[header.index(col)] = 0
        # Go through floorspace file and add appropriate amounts.
        for fltype in fltypes:
            for taz in zones:
                key = (taz, fltype)
                if key in flspace:
                    amount = flspace[key]
                    luz = zones[taz]
                    key = (luz, commod)
                    if key in exchange:
                        # Row already exists: update exchange size terms.
                        row = exchange[key]
                        for col in exchange_size_cols:
                            row[header.index(col)] += amount
                    else:
                        # Row does not exist: copy the appropriate template.
                        row = list(exchange[-1, commod])
                        row[header.index(exchange_luz_col)] = luz
                        for col in exchange_size_cols:
                            row[header.index(col)] = amount
                        exchange[key] = row
    
    # Write out modified ExchangeImportExportI file.
    write_exchange(exchange_fname.replace("%year%", str(year)), header, exchange)

if __name__ == "__main__":
    main(sys.argv[1])
