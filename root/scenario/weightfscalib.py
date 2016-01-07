import csv
import os
from os import path
import time
import math
import shutil

import csvutil as cu
import scriptutil as su
from floorspace import Floorspace
from calib import Calibrator

###################################################################
#                        GLOBAL CONSTANTS                         #
###################################################################

# define the files to access; these should be standard.
year = "t19"
# name of floorspace file
spaceFileName = path.join("outputs", year, "FloorspaceI.csv")
# file containing floorspace quantity targets
spaceTargetFileName = path.join("outputs", year, "FloorspaceTargets.csv")
# file containing space price targets
priceTargetFileName = path.join("outputs", year, "ExchangeResultsTargets.csv")
# has most recent exchange results
resultsFileName = path.join("outputs", year, "ExchangeResults.csv")
xresi_fname = path.join("outputs", year, "ExchangeResultsI.csv")
# TAZ->LUZ correspondence
tazFileName = path.join("outputs", year, "alpha2beta.csv")
# name of floorspace overrides file (specify None if not used)
overrideFileName = None
logFileName = "event.log"

# The floorspace file often needs to be copied somewhere else after calibration
# (e.g. FloorspaceCalc). Specify the target path here, or None if not needed.
finalSpaceFileName = "CalibratedFloorspaceForVisum.csv"

checkFileName = "FloorspaceCalib.csv"  # for tracking calibration progress
calibLogFileName = "calib.log" # logfile for calibration output
modelCommand = "python run_aa.py"  #the file that runs AA
modelProps = path.join("outputs", year, "aa.properties") # the AA properties file
runModel = True

# Column names
# In the TAZ->LUZ correspondence
TAZLUZ_TAZ = "Azone"
TAZLUZ_LUZ = "Bzone"
# In the price targets file
PTARG_COMMOD = "Commodity"
PTARG_ZONE = "ZoneNumber"
PTARG_TARG = "TargetPrice"
PTARG_STDEV = "Tolerance"
# In the space targets file
STARG_COMMOD = "Commodity"
STARG_ZONE = "ZoneNumber"
STARG_TARG = "TargetFloorspace"
STARG_STDEV = "Tolerance"
# In floor space file
SPACE_ZONE = "taz"
SPACE_COMMOD = "commodity"
SPACE_AMT = "quantity"
# In exchange results (current prices) file
XRES_COMMOD = "Commodity"
XRES_ZONE = "ZoneNumber"
XRES_SUPPLY = "Supply"
XRES_PRICE = "Price"
XRES_DERIV = "Derivative"


taz_fspace = True # set to False if FloorspaceI is LUZ-based rather than TAZ-based.
maxIts = 5     # maximum number of iterations to run
initStep = 0.35    # initial step size
minStep = 0.02
maxStep = 1
stepInc = 1.25   # factor to increase the step size by if the error improved
stepDec = 0.8   # factor to decrease the step size by if the error did not improve
minRatio = 0.5  # minimum allowed ratio between new and old floorspace amounts
maxRatio = 2. # maximum allowed ratio between new and old floorspace amounts
minSpace = 1000   # minimum allowed floorspace

rampup = False # Whether to tighten the AA convergence criteria every iteration. If set to false, the following four parameters will be ignored and the convergence criteria from the properties file will be used.
initSpecClear = 10 # First-iteration value for maximum specific clearance in AA
finalSpecClear = 0.1 # Last-iteration value for maximum specific clearance in AA
initTotalClear = 1 # First-iteration value for maximum total clearance in AA
finalTotalClear = 0.01 # Last-iteration value for maximum total clearance in AA

# Whether to feed previous iteration's ExchangeResults back into AA.
aa_cascading = True

def main(props):
    calib = WeightFSCalib(props)
    calib.calibrate()

def get_status(
        edit_props=False, specClear=finalSpecClear, totalClear=finalTotalClear):
    """
    Runs AA and produces normal progress-checking output,
    but does no calibration.
    """
    
    class Properties(object):
        pass
    
    props = Properties()
    props.maxIts = 0
    props.rampup = edit_props
    props.initSpecClear = specClear
    props.finalSpecClear = specClear
    props.initTotalClear = totalClear
    props.finalTotalClear = totalClear
    props.backup = False
    
    main(props)

class WeightFSCalib(Calibrator):
    def __init__(self, props):
        super(WeightFSCalib, self).__init__()
        self.__props = props
        self.maxits = props.maxIts
        self.countsubits = True # Probably remove this later.
        self.initstep = initStep
        self.minstep = minStep
        self.maxstep = maxStep
        self.stepinc = stepInc
        self.stepdec = stepDec
        self.logfile = calibLogFileName
    
    @property
    def parameters(self):
        return [spaceFileName]
    
    @property
    def outputs(self):
        return [resultsFileName, logFileName]
    
    @property
    def checks(self):
        return [checkFileName]
    
    def set_up(self):
        super(WeightFSCalib, self).set_up()
        if taz_fspace:
            self.log("Reading in TAZ->LUZ correspondence...")
            self.__zone_dict = readZoneData()
            self.log("Done reading in TAZ->LUZ correspondence")
        else:
            self.__zone_dict = su.IdDict()
        
        self.__flspace = Floorspace(self.__zone_dict)
        if minSpace is not None:
            self.__add_min_space()
        
        self.log("Reading in price targets file...")
        self.__price_targs = readPriceTargets()
        self.log("Done reading in price targets file")
        
        self.log("Reading in floor space targets file...")
        self.__space_targs = readSpaceTargets()
        self.log("Done reading in floor space targets file")
        
        if aa_cascading:
            su.backup(xresi_fname, "Backup")
        
        self.__spec_clear = None
        self.__total_clear = None
        
        self.__prev_exp_price = None
        self.__prev_error = None
        self.__prev_exp_error = None
    
    def __add_min_space(self):
        su.backup(spaceFileName, "orig")
        flspace = self.__flspace
        flspace.load_floorspace(spaceFileName,
                                SPACE_ZONE, SPACE_COMMOD, SPACE_AMT)
        basespace = flspace.basespace
        self.__originally_zero = set()
        for zone in basespace:
            for sptype in basespace[zone]:
                if basespace[zone][sptype] == 0:
                    self.__originally_zero.add((sptype, zone))
                if basespace[zone][sptype] < minSpace:
                    basespace[zone][sptype] = minSpace
        flspace.save_floorspace()
    
    def tear_down(self):
        try:
            flspace = self.__flspace
        except AttributeError:
            self.log("Floorspace not yet initialized")
        else:
            # Get rid of any artifacts of the minimum floor space cutoff.
            try:
                if minSpace is not None:
                    flspace.load_floorspace(su.backup_name(spaceFileName, "final"),
                                        SPACE_ZONE, SPACE_COMMOD, SPACE_AMT)
                    basespace = flspace.basespace
                    for sptype, zone in self.__originally_zero:
                        if basespace[zone][sptype] <= minSpace:
                            basespace[zone][sptype] = 0
                    flspace.save_floorspace()
            except IOError:
                pass
        
        # Copy floor space to its final destination.
        if finalSpaceFileName is not None:
            try:
                shutil.copy(
                        su.backup_name(spaceFileName, "final"),
                        finalSpaceFileName)
            except IOError:
                self.log("Did not copy the best floor space; "
                         "no iterations completed.")
        super(WeightFSCalib, self).tear_down()
    
    def read_parameters(self):
        self.log("Reading current space quantities...")
        self.__flspace.load_floorspace(spaceFileName,
                                       SPACE_ZONE, SPACE_COMMOD, SPACE_AMT)
        space = flatten(self.__flspace.space)
        self.log("Done reading current space quantities")
        return space
    
    def write_parameters(self, param_obj):
        self.log("Writing out new floor space file")
        self.__flspace.space = unflatten(param_obj)
        self.__flspace.save_floorspace()
        self.log("Done writing out new floor space file")
    
    def read_outputs(self):
        self.log("Reading in results file...")
        prices = readCurrentPrices(self.__flspace.available_types)
        self.log("Done reading in results file")
        return prices
    
    def write_checks(self, oldparams, newparams, oldoutputs, newoutputs):
        with open(checkFileName, "w") as cfile:
            writer = csv.writer(cfile, cu.ExcelOne)
            header = ["Commodity", "Zone", "Current Floorspace",
                      "Adjusted Floorspace", "Target Floorspace",
                      "Floorspace Tolerance", "Expected Price", "Current Price",
                      "Expected New Price", "Target Price", "Price Tolerance",
                      "Previous Error", "Expected Error", "Current Error",
                      "Expected New Error"]
            writer.writerow(header)
            all_prx_p = self.__prev_exp_price
            all_prev_e = self.__prev_error
            all_prx_e = self.__prev_exp_error
            
            all_errors = {}
            all_exp_errors = {}
            
            for czpair in sorted(oldparams, key=lambda (c, z): (c, int(z))):
                commodity, zone = czpair
                v = self.__unpack_values(czpair, oldparams, oldoutputs)
                if v is not None:
                    cur_e = (((v.cur_q - v.targ_q) / v.stdev_q) ** 2 +
                             ((v.cur_p - v.targ_p) / v.stdev_p) ** 2)
                    all_errors[czpair] = cur_e
                    
                    # Only if there was a previous iteration:
                    if all_prx_p is not None:
                        prx_p = all_prx_p[czpair][1]
                        prev_e = all_prev_e[czpair]
                        prx_e = all_prx_e[czpair]
                    else:
                        prx_p, prev_e, prx_e = ("N/A",) * 3
                    
                    # Only if there will be a next iteration:
                    if newparams is not None:
                        adj_q = newparams[czpair]
                        exp_p = newoutputs[czpair][1]
                        exp_e = (((adj_q - v.targ_q) / v.stdev_q) ** 2 +
                                 ((exp_p - v.targ_p) / v.stdev_p) ** 2)
                        all_exp_errors[czpair] = exp_e
                    else:
                        adj_q, exp_p, exp_e = ("N/A",) * 3
                    
                    row = [commodity, zone, v.cur_q,
                           adj_q, v.targ_q,
                           v.stdev_q, prx_p, v.cur_p,
                           exp_p, v.targ_p, v.stdev_p,
                           prev_e, prx_e, cur_e, exp_e]
                    writer.writerow(row)
            
            self.__prev_exp_price = newoutputs
            self.__prev_error = all_errors
            if newparams is not None:
                self.__prev_exp_error = all_exp_errors
    
    def log_iteration(
            self, it, old_step, new_step, cur_error, prev_error, exp_error):
        super(WeightFSCalib, self).log_iteration(
                it, old_step, new_step, cur_error, prev_error, exp_error)
        if aa_cascading:
            shutil.copy(resultsFileName, xresi_fname)
    
    def run_model(self, it):
        if self.__props.rampup:
            self.__prepare_rampup(it)
        
        if runModel:
            self.log("Running AA... ")
            os.system(modelCommand)
    
    def __prepare_rampup(self, it):
        if it == 0:
            init_spec = self.__props.initSpecClear
            final_spec = self.__props.finalSpecClear
            init_total = self.__props.initTotalClear
            final_total = self.__props.finalTotalClear
            self.__spec_clear = init_spec
            self.__total_clear = init_total
            
            # By how much do we reduce the convergence criteria each iteration?
            self.__spec_factor = ((final_spec / init_spec) **
                                  (1.0 / (self.maxits - 1)))
            self.__total_factor = ((final_total / init_total) **
                                   (1.0 / (self.maxits - 1)))
        else:
            self.__spec_clear *= self.__spec_factor
            self.__total_clear *= self.__total_factor
        
        set_aa_params(self.__spec_clear, self.__total_clear)
    
    def update_parameters(self, params, outputs, step):
        self.log("Calculating new floor space amounts...")
        new_params = {}
        exp_outputs = {}
        
        for czpair in params.keys():
            new_params[czpair] = params[czpair]
            exp_outputs[czpair] = outputs[czpair]
            v = self.__unpack_values(czpair, params, outputs)
            if v is not None:
                ds_dq = 1
                stdev_ratio = v.stdev_p ** 2 / v.stdev_q ** 2
                dp_dq = -ds_dq / v.aaderiv
                
                # Find the optimal change in the quantity
                numerator = (stdev_ratio * (v.targ_q - v.cur_q) +
                             dp_dq * (v.targ_p - v.cur_p))
                denominator = stdev_ratio + dp_dq ** 2
                delta_q = numerator / denominator
                
                # Step size control
                ratio = 1 + delta_q / (v.cur_q + v.targ_q) # Replace with the following after testing!
                # ratio = 1 + delta_q / v.cur_q
                if ratio < 0:
                    ratio = 0.05
                ratio = ratio ** step
                if ratio < minRatio:
                    ratio = minRatio
                if ratio > maxRatio:
                    ratio = maxRatio
                new_q = v.cur_q * ratio
                new_params[czpair] = new_q
                # We have no idea what the supply and derivative will be, and
                # we actually don't care. Use 0 as a placeholder.
                exp_outputs[czpair] = (
                        0, v.cur_p + dp_dq * (new_q - v.cur_q), 0)
        
        self.log("Done calculating new floor space amounts")
        return (new_params, exp_outputs)
    
    def error(self, params, outputs):
        total_error = 0
        for czpair in params.keys():
            v = self.__unpack_values(czpair, params, outputs)
            if v is not None:
                total_error += (((v.cur_q - v.targ_q) / v.stdev_q) ** 2 +
                                ((v.cur_p - v.targ_p) / v.stdev_p) ** 2)
        return total_error
    
    def error_comps(self, params, outputs):
        quantity_error = 0
        price_error = 0
        for czpair in params.keys():
            v = self.__unpack_values(czpair, params, outputs)
            if v is not None:
                quantity_error += ((v.cur_q - v.targ_q) / v.stdev_q) ** 2
                price_error += ((v.cur_p - v.targ_p) / v.stdev_p) ** 2
        return {"Quantity Error": quantity_error, "Price Error": price_error}
    
    def __unpack_values(self, czpair, params, outputs):
        if (czpair in self.__price_targs and
                czpair in self.__space_targs and
                params[czpair] > 0):
            v = Values()
            v.targ_p = self.__price_targs[czpair][0]
            v.cur_p = outputs[czpair][1]
            v.stdev_p = self.__price_targs[czpair][1]
            v.targ_q = self.__space_targs[czpair][0]
            v.cur_q = params[czpair]
            v.stdev_q = self.__space_targs[czpair][1]
            v.aaderiv = outputs[czpair][2]
            return v
        else:
            return None

class Values():
    pass

def set_aa_params(specClear, totalClear):
    #Import is here so no dependency on prop_edit unless this function is called.
    #pylint:disable=import-error
    import prop_edit
    prop_edit.load_props(modelProps)
    prop_edit.set_prop("aa.maxSpecificClearance", specClear)
    prop_edit.set_prop("aa.maxTotalClearance", totalClear)
    prop_edit.save_props(modelProps)

def readZoneData():
    zoneDict = {}
    with open(tazFileName, "rU") as tazFile:
        tazCsvReader = csv.reader(tazFile)
        tazHeader = tazCsvReader.next()
        for row in tazCsvReader:
            taz = row[tazHeader.index(TAZLUZ_TAZ)]
            luz = row[tazHeader.index(TAZLUZ_LUZ)]
            zoneDict[taz]=luz
    return zoneDict

def readPriceTargets():
    priceTargDict = {}
    with open(priceTargetFileName, "rU") as tfile:
        targFile = csv.reader(tfile)
        targetHeader = targFile.next()
        for row in targFile:
            commod = row[targetHeader.index(PTARG_COMMOD)]
            zone = row[targetHeader.index(PTARG_ZONE)]
            target = row[targetHeader.index(PTARG_TARG)]
            stdev = row[targetHeader.index(PTARG_STDEV)]
            czpair = (commod, zone)
            priceTargDict[czpair] = (float(target), float(stdev))
    return priceTargDict

def readSpaceTargets():
    spaceTargDict = {}
    with open(spaceTargetFileName, "rU") as tfile:
        targFile = csv.reader(tfile)
        targetHeader = targFile.next()
        for row in targFile:
            commod = row[targetHeader.index(STARG_COMMOD)]
            zone = row[targetHeader.index(STARG_ZONE)]
            target = row[targetHeader.index(STARG_TARG)]
            stdev = row[targetHeader.index(STARG_STDEV)]
            czpair = (commod, zone)
            spaceTargDict[czpair] = (float(target), float(stdev))
        tfile.close()
    return spaceTargDict

def readCurrentPrices(space_types):
    resultsDict = {}
    with open(resultsFileName, "rU") as rfile:
        resultsFile = csv.reader(rfile)
        resultsHeader = resultsFile.next()
        for row in resultsFile:
            commod = row[resultsHeader.index(XRES_COMMOD)]
            
            # We're only interested in floorspace commodities
            if commod in space_types:
                zone = row[resultsHeader.index(XRES_ZONE)]
                supply = float(row[resultsHeader.index(XRES_SUPPLY)])
                price = float(row[resultsHeader.index(XRES_PRICE)])
                deriv = float(row[resultsHeader.index(XRES_DERIV)])
                resultsDict[commod, zone] = (supply, price, deriv)
    return resultsDict

def flatten(nested_space):
    flat_space = {}
    for zone, by_zone in nested_space.iteritems():
        for sptype, amt in by_zone.iteritems():
            flat_space[sptype, zone] = amt
    return flat_space

def unflatten(flat_space):
    nested_space = {}
    for (sptype, zone), amt in flat_space.iteritems():
        by_zone = nested_space.setdefault(zone, {})
        by_zone[sptype] = amt
    return nested_space
    
if __name__ == "__main__":
    class Properties(object):
        pass
    
    props = Properties()
    props.maxIts = maxIts
    props.rampup = rampup
    props.initSpecClear = initSpecClear
    props.finalSpecClear = finalSpecClear
    props.initTotalClear = initTotalClear
    props.finalTotalClear = finalTotalClear
    props.backup = True
    
    main(props)
