import csv
import copy

import csvutil as cu
import scriptutil as su

class Floorspace(object):
    def __init__(self, zmap=su.IdDict()):
        self.zmap = zmap
    
    def load_floorspace(self, fname, zone_cname, type_cname, amt_cname):
        """
        Loads the space quantities into memory.
        
        The quantities are stored in the space attribute, a nested dictionary
        from zones to space types to quantities.
        If a zone map is in place, then the quantities by the zone system used
        in the floor space file is stored in the basespace attribute, while
        space holds the mapped quantities.
        The set of all space types encountered is stored in the available_types
        attribute.
        """
        self.fname = fname
        with open(fname, "rU") as file:
            reader = csv.reader(file)
            self.raw_space = list(reader)
        self.basespace = cu._dict()
        self.space = cu._dict()
        self.available_types = set()
        header = self.raw_space[0]
        self._zone_col = header.index(zone_cname)
        self._type_col = header.index(type_cname)
        self._amt_col = header.index(amt_cname)
        for line in self.raw_space[1:]:
            sptype = line[self._type_col]
            taz = line[self._zone_col]
            amt = float(line[self._amt_col])
            by_taz = self.basespace.setdefault(taz, cu._dict())
            by_luz = self.space.setdefault(self.zmap[taz], cu._dict())
            by_taz[sptype] = amt
            by_luz[sptype] = by_luz.get(sptype, 0) + amt
            self.available_types.add(sptype)
        self._oldspace = copy.deepcopy(self.space)
    
    def save_floorspace(self):
        """
        Writes the floor space quantities back to the file,
        including any changes to the space or basespace attribute.
        Changes to basespace are written directly to the file, while changes
        to space are allocated to the basespace zones in proportion to the
        original quantities.
        If there are changes to both basespace and space, then the changes
        to basespace are applied first, after which the changes to space
        are allocated in proportion to the new basespace quantities.
        """
        with open(self.fname, "w") as file:
            writer = csv.writer(file, cu.ExcelOne)
            writer.writerow(self.raw_space[0])
            for line in self.raw_space[1:]:
                taz = line[self._zone_col]
                luz = self.zmap[taz]
                sptype = line[self._type_col]
                oldspace = self._oldspace.get(luz, {}).get(sptype, 0)
                basespace = self.basespace.get(taz, {}).get(sptype, 0)
                if oldspace == 0:
                    new_basespace = basespace
                else:
                    adj = self.space[luz][sptype] / oldspace
                    new_basespace = basespace * adj
                line[self._amt_col] = new_basespace
                by_taz = self.basespace.setdefault(taz, {})
                by_taz[sptype] = new_basespace
                writer.writerow(line)
        self._oldspace = copy.deepcopy(self.space)