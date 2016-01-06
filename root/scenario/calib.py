# Universal calibration script.
# To do a calibration process, make a subclass of Calibrator with the desired
# attributes, then call its calibrate() method.
from abc import ABCMeta, abstractmethod, abstractproperty
import itertools
import csv
import shutil
import sys

import scriptutil as su
import csvutil as cu

class Calibrator(object):
    __metaclass__ = ABCMeta
    """
    Base class for calibrators.
    
    Aside from its methods, this class defines several attributes that control
    the calibration. They can be assigned a value other than the default after
    the call to the superclass constructor. The attributes are as follows, with
    the default values listed in parentheses after the attribute names.
    
    maxits(=50) - Maximum number of iterations that can be done
    countsubits(=False) - Whether to count sub-iterations (i.e. iterations
            that are dead ends and trigger a backtrack) towards the iteration
            count. If False, the model will run at most (maxits + 1) times.
            If True, calibration will continue until maxits successful steps
            have been made.
    initstep(=0.2) - Initial step size (exactly what is meant by "step size"
            is up to the implementation
    minstep(=0.02) - Minimum allowed step size; calibration terminates if no
            progress is made at or below this step size
    maxstep(=1.0) - Maximum allowed step size
    stepinc(=1.25) - Factor to increase step size by if error improved
    stepdec(=0.8) - Factor to decrease step size by if error did not improve
    summary(="Summary.csv") - summary file where the error trend is recorded
    allchecks(="AllChecks.csv") - file where all of the check files are compiled
    logfile(=None) - Name of the file where logging messages should be sent in
            addition to the console
    backup(=True) - Whether to back up parameters/outputs/calibration files
            every iteration
    """
    
    def __init__(self):
        self.maxits = 50
        self.countsubits = False
        self.initstep = 0.2
        self.minstep = 0.02
        self.maxstep = 1.0
        self.stepinc = 1.25
        self.stepdec = 0.8
        self.summary = "Summary.csv"
        self.allchecks = "AllChecks.csv"
        self.logfile = None
        self.backup = True
        self.__finish_text = {
                "converged": "The calibration converged!",
                "maxits": "The calibration stopped at the maximum iterations.",
                "stalled": "No further progress could be made.",
                "program": "The calibration encountered a Python error; "
                           "check the correctness of the subclass.",
                "external": "The model run encountered an error; check the "
                            "model configuration."}
    
    @abstractproperty
    def parameters(self):
        """
        List of filenames of model parameter files to back up before each
        iteration
        """
        pass
    
    @abstractproperty
    def outputs(self):
        """
        List of filenames of model output files to back up after each iteration
        """
        pass
    
    @abstractproperty
    def checks(self):
        """
        List of calibration check files to back up after each iteration and
        combine into a single file at the end (for analysis by e.g. Excel Pivot
        Tables).
        """
        pass
    
    def calibrate(self):
        """
        Performs the full calibration run, including setup and tear-down.
        
        Should not normally be overridden.
        """
        try:
            self.set_up()
            self.iterate()
        finally:
            self.tear_down()
    
    def set_up(self):
        """
        Performs any setup that has to be done before the first iteration.
        
        By default, reinitialized state variables needed for proper calibration.
        """
        self.__bestit = None
        self.__checks = []
        
        if self.logfile is not None:
            self.__logfile = open(self.logfile, "w")
    
    def tear_down(self):
        """
        Performs any cleanup that has to be done after the last iteration.
        
        This is called even if the calibration ends prematurely due to an
        error. Therefore, it should be written defensively to do all the
        cleanup that it can, even in the case of failure. For example, if
        set_up() binds a name, then tear_down() should surround any references
        to that name with a try/except on NameError in case the failure occurred
        before the name was bound.
        
        By default, this ensures that the parameter files from the best known
        iteration are copied as "FileName_final_.ext", and that the Summary
        and AllCheck files are written.
        """
        if self.__bestit is not None:
            for fname in self.parameters:
                shutil.copy(su.backup_name(fname, self.__bestit),
                            su.backup_name(fname, "final"))
            with open(self.summary, "w") as sfile:
                writer = csv.writer(sfile, cu.ExcelOne)
                compheader = self.__progress[0][3].keys()
                writer.writerow(["Iteration", "Sub-Iteration", "Step Size"] +
                                compheader +
                                ["Total Error"])
                for it, subit, step, comps, curerr in self.__progress:
                    writer.writerow([it, subit, step] +
                                    [comps[header] for header in compheader] +
                                    [curerr])
            # Compile the AllChecks files.
            for check in self.checks:
                with open("All" + check, "w") as allf:
                    writer = csv.writer(allf, cu.ExcelOne)
                    first = True
                    for checkit in self.__checks:
                        with open(su.backup_name(check, checkit), "rU") as itf:
                            reader = csv.reader(itf)
                            header = reader.next()
                            if first:
                                writer.writerow(["Iteration"] + header)
                                first = False
                            for row in reader:
                                writer.writerow([checkit] + row)
        
        if self.logfile is not None:
            try:
                self.__logfile.close()
            except (AttributeError, IOError):
                pass
    
    @abstractmethod
    def read_parameters(self):
        """
        Reads the parameter files into a Python object and returns it.
        
        The return value can be of any type. The only restriction is that it
        must not be mutated once it is returned.
        """
        pass
    
    @abstractmethod
    def write_parameters(self, param_obj):
        """
        Writes a parameter object back out to the parameter files.
        """
        pass
    
    @abstractmethod
    def read_outputs(self):
        """
        Reads the model output files into a Python object and returns it.
        
        The return value can be of any type. The only restriction is that it
        must not be mutated once it is returned.
        """
        pass
    
    @abstractmethod
    def write_checks(self, oldparams, newparams, oldoutputs, newoutputs):
        """
        Writes the check files that track calibration progress.
        
        Arguments:
        oldparams: the parameter values at the start of the iteration
        newparams: the current adjusted parameter values
        oldoutputs: the output values generated by this iteration's model run
        newoutputs: the predicted output values that the model will generate
                using newparams as its parameter values
        
        Pass None for newparams and newoutputs if there is no next iteration.
        """
        pass
    
    # Variation of write_checks for when this iteration is a dead end - because
    # either it's the last iteration or the calibrator needs to backtrack.
    def __dead_write_checks(self, params, outputs):
        self.write_checks(params, None, outputs, None)
    
    def log_finish(self, it, step, error, reason):
        """
        Log the end of the calibration.
        
        This is called (in addition to a final call to write_checks) when the
        calibration stops for any reason. Arguments:
        it: the iteration that was in progress when the calibration stopped
        step: the step size at the time the calibration stopped
        error: the best total error reached
        reason: the reason for the stop. This can be:
            "converged" - calibration converged
            "maxits" - calibration reached maximum iterations
            "stalled" - calibration was unable to make further progress
            "program" - programming error in the subclass
            "external" - external error occurred - e.g. a file read-write error
                    or keyboard interrupt
        """
        self.log(self.__finish_text[reason])
        self.log("The calibration stopped at iteration {}".format(it))
        self.log("The best known total error was {:.3G}".format(error))
    
    def log_iteration(
            self, it, old_step, new_step, cur_error, prev_error, exp_error):
        """
        Log the end of a successful iteration.
        
        This is called at the end of each successful iteration. Arguments:
        it: the iteration that just completed
        old_step: the step size used for the most recent iteration
        new_step: the step size that will be used for the next iteration
        cur_error: the most recent total error (according to the error() method)
        prev_error: the total error from the previous iteration (or None if this
                is the first iteration)
        exp_error: the total error predicted by the update_parameter() method
        
        By default, writes a simple log message summarizing the arguments.
        """
        self.log("Iteration {} finished!".format(it))
        if prev_error is None:
            self.log("The total error is {:.3G}".format(cur_error))
        else:
            self.log("The total error has improved from {:.3G} to {:.3G}"
                     .format(prev_error, cur_error))
        if new_step > old_step:
            self.log("Step size increased from {:.3G} to {:.3G}".format(
                    old_step, new_step))
        else:
            self.log("Current step size is {:.3G}".format(new_step))
        self.log("The predicted error for the next iteration is {:.3G}".format(
                exp_error))
    
    def log_bad_iteration(
            self, it, attempt, old_step, new_step, cur_error, best_error,
            exp_error):
        """
        Log the end of an unsuccessful iteration (one where the errer went up).
        
        This is called at the end of each unsuccessful iteration. Arguments:
        it: the iteration that was attempted
        attempt: the number of this attempt at the current iteration;
                starts at 0 and increments until a successful iteration occurs
        old_step: the step size used for the most recent iteration
        new_step: the step size that will be used for the next iteration
        cur_error: the most recent total error
        best_error: the best known total error
        exp_error: the total error predicted for the next attempt
        
        By default, writes a simple log message summarizing the arguments.
        """
        self.log("Iteration {} attempt {} failed to improve the error.".format(
                it, attempt))
        self.log("It produced an error of {:.3G} "
                "when the best known error is {:.3G}".format(
                        cur_error, best_error))
        if new_step < old_step:
            self.log("Step size decreased from {:.3G} to {:.3G}".format(
                    old_step, new_step))
        else:
            self.log("Current step size is {:.3G}".format(new_step))
        self.log("The predicted error for the next iteration is {:.3G}".format(
                exp_error))
    
    def log(self, object):
        """
        Logs a line of text.
        
        This is called whenever the calibration process needs to write logging
        information. Subclasses should use it instead of print. By default, logs
        to the console (using print). Override to redirect logging elsewhere.
        """
        print object
        if self.logfile is not None:
            self.__logfile.write(str(object) + "\n")
            self.__logfile.flush()
    
    @abstractmethod
    def run_model(self, it):
        """
        Runs the model once.
        
        This method can also include other steps that have to be taken before
        and after each model run.
        """
        pass
    
    @abstractmethod
    def update_parameters(self, params, outputs, step):
        """
        Updates the parameter values based on the model outputs.
        
        This method calculates new parameter values based on the current
        parameter values (the param argument) and the resulting model outputs
        (the outputs argument). It also tries to predict what the output values
        will be when running the model with the new parameters. The new
        parameters and predicted outputs are returned as the two elements in a
        tuple.
        
        The current calibration step size is passed as the step argument. The
        implementation decides what step size means, but generally a step size
        of 1 means the implementation goes all the way to what it believes are
        the optimal parameters, while step sizes less than one are more cautious
        steps towards the optimal parameters.
        """
        pass
    
    def iterate(self):
        """
        Iterates the model to convergence or up to the maximum iterations.
        
        This implementation includes "backtracking" with dynamic step size
        management to prevent the calibration from getting stuck.
        It should not normally be overridden.
        """
        try:
            step = self.initstep
            params = self.read_parameters()
            self.__progress = []
            
            itcount = 0
            for it in itertools.count():
                found_good = False
                for subit in itertools.count():
                    self.run_model(it)
                    outputs = self.read_outputs()
                    
                    curerr = self.error(params, outputs)
                    comps = self.error_comps(params, outputs)
                    self.__progress.append((it, subit, step, comps, curerr))
                    
                    #pylint:disable=used-before-assignment
                    if it > 0 and curerr >= besterr:
                        # The new iteration is a step backward. Revert to last
                        # good iteration and try again with a smaller step size.
                        fullit = str(it) + "(" + str(subit) + ")"
                        self.__backup(fullit)
                        oldstep = step
                        step *= self.stepdec
                        if step < self.minstep:
                            self.__dead_write_checks(params, outputs)
                            self.__backup_checks(fullit)
                            raise CalibrationStalled
                        if self.countsubits:
                            itcount += 1
                            if itcount >= self.maxits:
                                self.__dead_write_checks(params, outputs)
                                self.__backup_checks(fullit)
                                break
                        newparams, predoutputs = self.update_parameters(
                                bestparams, bestoutputs, step)
                        self.write_checks(
                                params, newparams, outputs, predoutputs)
                        self.__backup_checks(fullit)
                        experr = self.error(newparams, predoutputs)
                        self.log_bad_iteration(
                                it, subit, oldstep, step, curerr, besterr,
                                experr)
                        self.write_parameters(newparams)
                    else:
                        found_good = True
                        break
                
                oldstep = step
                if found_good:
                    # Now we have a good iteration.
                    if it > 0:
                        preverr = besterr
                        # pylint:disable=undefined-loop-variable
                        if subit == 0:
                            # If the iteration succeeded on the first try,
                            # encourage more aggresive steps.
                            step *= self.stepinc
                            if step > self.maxstep:
                                step = self.maxstep
                    besterr = curerr
                    bestparams = params
                    bestoutputs = outputs
                    self.__bestit = it
                    self.__backup(it)
                
                # Check for convergence or max iterations.
                stop = False
                if it > 0 and self.is_converged(bestparams, params):
                    stop = True
                    converged = True
                elif itcount >= self.maxits:
                    stop = True
                    converged = False
                if stop:
                    self.__dead_write_checks(params, outputs)
                    self.__backup_checks(it)
                    self.log_finish(it, step, besterr,
                                    "converged" if converged else "maxits")
                    break
                
                # All branches that don't set found_good should exit in the
                # block above.
                assert found_good
                
                # Still calibrating.
                # Calculate new parameters and prepare for next iteration.
                newparams, predoutputs = self.update_parameters(
                        params, outputs, step)
                
                self.write_checks(params, newparams, outputs, predoutputs)
                self.__backup_checks(it)
                experr = self.error(newparams, predoutputs)
                self.log_iteration(
                        it, oldstep, step,
                        besterr, preverr if it > 0 else None, experr)
                self.write_parameters(newparams)
                
                params = newparams
                itcount += 1
                
        except CalibrationStalled:
            self.log_finish(it, step, besterr, "stalled")
        except (ArithmeticError, AssertionError, AttributeError, ImportError,
                LookupError, NameError, NotImplementedError, SyntaxError,
                TypeError, ValueError) as e:
            self.__reraise(
                    lambda: self.log_finish(it, step, besterr, "program"))
        except Exception as e:
            self.__reraise(
                    lambda: self.log_finish(it, step, besterr, "external"))
    
    def __reraise(self, fun):
        # Preserve stack trace, even if there were errors in fun.
        exc_info = sys.exc_info()
        try:
            fun()
        except NameError:
            pass
        raise exc_info[0], exc_info[1], exc_info[2]
    
    def __backup(self, it):
        if self.backup:
            for paramfile in self.parameters:
                su.backup(paramfile, it)
            
            for outfile in self.outputs:
                su.backup(outfile, it)
    
    def __backup_checks(self, it):
        if self.backup:
            for checkfile in self.checks:
                su.backup(checkfile, it)
                self.__checks.append(it)
                
    @abstractmethod
    def error(self, params, outputs):
        """
        Returns the total error for the specified parameters and outputs.
        
        The total error is a single value such that smaller values mean better
        calibration.
        """
        pass
    
    def error_comps(self, params, outputs):
        """
        Returns a dictionary-like object containing the error components.
        
        Each key is the name of an error component, with the corresponding
        value being the contribution from that component. If the dictionary
        is non-empty, the total of the contributions from all components should
        equal the current total error; i.e. sum(x.error_comps().values()) should
        be equal within roundoff error to x.error().
        
        Returns an empty dictionary by default.
        """
        return {}
    
    def is_converged(self, oldparams, newparams):
        """
        Checks whether the calibration has converged.
        
        Always returns false by default - calibration will always proceed to
        maximum iterations. This can be overridden to allow calibration to stop
        before the maximum iterations if it is deemed to have converged.
        """
        return False

class CalibrationStalled(Exception):
    pass