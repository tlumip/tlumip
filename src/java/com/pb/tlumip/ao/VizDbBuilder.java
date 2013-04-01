package com.pb.tlumip.ao;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code VizDbBuilder} ...
 *
 * @author crf
 *         Started 2/12/13 7:11 AM
 */
public class VizDbBuilder {
    private static final Logger logger = Logger.getLogger(VizDbBuilder.class);
    private final ResourceBundle properties;
    private final AtomicBoolean errorIndicator = new AtomicBoolean(false);
    private final AtomicInteger processCount = new AtomicInteger(0); //use this to delocalize so that we can be assured to observe error condition
    private final List<Process> processList = Collections.synchronizedList(new LinkedList<Process>());
    private final Map<Boolean,List<File>> outputDirMap;

    public VizDbBuilder(ResourceBundle properties){
        this(properties,null);
    }

    public VizDbBuilder(ResourceBundle properties, List<String> outputYears){
		this.properties = properties;
        //if dir list is empty, then just combine, if null, then auto find all the dirs
        outputDirMap = (outputYears == null) ? getOutputDirMap() : getOutputDirMap(getOutputDirList(outputYears));
    }

    private List<File> getOutputDirList(List<String> outputYears) {
        String prefix = getOutputDirPrefix();
        File outputDir = getOutputDir();
        List<File> outputDirList = new LinkedList<File>();
        for (String f : outputYears)
            outputDirList.add(new File(outputDir,prefix + f));
        return outputDirList;
    }

    private String getOutputDirPrefix() {
        return properties.getString("t.year.prefix");
    }

    private File getOutputDir() {
        return new File(properties.getString("scenario.outputs"));
    }

    private Map<Boolean,List<File>> getOutputDirMap(List<File> outputDirList) {
        Set<File> tsYears = new LinkedHashSet<File>();
        Set<File> notTsYears = new LinkedHashSet<File>();
        String indicator = properties.getString("viz.is.ts.year.indicator.file");
        for (File f : outputDirList) {
            if (new File(f,indicator).exists())
                tsYears.add(f);
            else
                notTsYears.add(f);
        }
        Map<Boolean,List<File>> outputs = new HashMap<Boolean,List<File>>();
        outputs.put(true,new LinkedList<File>(tsYears));
        outputs.put(false,new LinkedList<File>(notTsYears));
        return outputs;
    }

    private Map<Boolean,List<File>> getOutputDirMap() { //true: is ts year, false: not ts year
        File outputDirBase = getOutputDir();
        final String outputDirPrefix = getOutputDirPrefix().toLowerCase();
        File[] outputDirs = outputDirBase.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isFile() && pathname.getName().toLowerCase().startsWith(outputDirPrefix);
            }
        });
        return getOutputDirMap(Arrays.asList(outputDirs));
    }

    private String[] buildCommand(String property) {
        String commandProperty = properties.getString(property).trim();
        commandProperty = commandProperty.substring(1,commandProperty.length()-1); //strip off [ and ] from value
        String[] commandPropertySplit = commandProperty.split("\\|");
        for (int i = 0; i < commandPropertySplit.length; i++)
            if (properties.containsKey(commandPropertySplit[i]))
                commandPropertySplit[i] = properties.getString(commandPropertySplit[i]);
        StringBuilder rebuildCommand = new StringBuilder();
        for (String s : commandPropertySplit)
            rebuildCommand.append(s);
        return rebuildCommand.toString().split(",");
    }

    public void buildVizDb() {
        buildVizDb("viz.build.command.list","viz.combine.command.list","viz db");
    }

    public void buildMicroVizDb() {
        buildVizDb("microviz.build.command.list","microviz.combine.command.list","micro viz db");
    }

    private void buildVizDb(String buildCommandProperty, String combineCommandProperty, String name) {
        logger.info(name + " builder started");
        buildSingleYearVizDb(buildCommandProperty,name);
        if (!errorIndicator.get()) {  //shouldn't get here if this is true, but check anyway
            logger.info("Finished building individual (" + name + ") dbs, beginning combination");
            String[] command = buildCommand(combineCommandProperty);
            runProgram(command,getOutputDir());
            logger.info("Combination complete, commencing cleanup");

            logger.info(name + " builder complete");
        }
        if (errorIndicator.get()) { //this covers both above processes
            logger.error(name + " builder had errors");
            throw new IllegalStateException(name + " builder had errors");
        }
    }

    private void buildSingleYearVizDb(String buildCommandProperty, String name) {
        String[] command = buildCommand(buildCommandProperty);
        Iterator<File> nonTsIterator = outputDirMap.get(false).iterator();
        Iterator<File> tsIterator = outputDirMap.get(true).iterator();
        Semaphore tsSemaphore = new Semaphore(ResourceUtil.getIntegerProperty(properties,"viz.max.concurrent.ts"));
        Semaphore totalSemaphore = new Semaphore(ResourceUtil.getIntegerProperty(properties,"viz.max.concurrent.total"));
        name = "individual year " + name;
        boolean tsAvailable;
        boolean nonTsAvailable;
        while ((nonTsAvailable = nonTsIterator.hasNext()) | (tsAvailable = tsIterator.hasNext())) {
            try {
                totalSemaphore.acquire();
                if (tsAvailable) {
                    if (tsSemaphore.tryAcquire()) {
                        startProgram(name,command,tsIterator.next(),tsSemaphore,totalSemaphore);
                    } else if (nonTsAvailable) {
                        startProgram(name,command,nonTsIterator.next(),totalSemaphore);
                    } else {
                        tsSemaphore.acquire();
                        startProgram(name,command,tsIterator.next(),tsSemaphore,totalSemaphore);
                    }
                } else {
                    startProgram(name,command,nonTsIterator.next(),totalSemaphore);
                }
                processCount.incrementAndGet();
            } catch (InterruptedException e) {
                //ignore, we'll deal with this below
                errorIndicator.set(true);
                break;
            }
        }

        //wait and cleanup, if necessary
        while (processCount.get() != 0) {
            //copy to avoid concurrent modification problems
            List<Process> processListCopy = new LinkedList<Process>();
            synchronized (processList) {
                processListCopy.addAll(processList);
            }
            for (Process p : processListCopy) {
                try {
                    if (errorIndicator.get()) {
                        p.exitValue();
                    } else {
                        if (p.waitFor() != 0)
                            errorIndicator.set(true);
                    }
                } catch (IllegalThreadStateException e) { //happens on exit value when the process isn't finished - only happens when already in error so kill it
                    p.destroy();
                } catch (InterruptedException e) { //process not done but another had a problem (presumably)
                    p.destroy();
                    errorIndicator.set(true); //just in case
                }
            }
        }
    }

    private void startProgram(final String name, final String[] command, final File workingDir, final Semaphore ... blocks) {
        final Thread thisThread = Thread.currentThread();
        new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("Building " + name + " for " + workingDir);
                runProgram(command,workingDir,thisThread,blocks);
                logger.info("Finished building " + name + " for " + workingDir);
            }
        }).start();
    }

    private void runProgram(String[] command, File workingDir) {
        runProgram(command,workingDir,null);
    }

    private void runProgram(String[] command, File workingDir, Thread callingThread, Semaphore ... blocks) {
        logger.info("Running command (" + workingDir + "): " + Arrays.toString(command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        final Process p;
        try {
//            Thread.sleep(20000); //chill out for a few seconds
            p = pb.start();
            processList.add(p);
            //log error stream
            new Thread(new Runnable() {
                public void run() {
                    logInputStream(p.getErrorStream(),true);
                }
            }).start();
            logInputStream(p.getInputStream(),false);
            if (p.waitFor() != 0)
                indicateErrors();
        } catch (Exception e) {
            logger.error("An exception occurred while trying to run viz db builder",e);
            indicateErrors(e);
        } finally {
            if (callingThread != null) {
                for (Semaphore block : blocks)
                    block.release();
                processCount.decrementAndGet();
                if (errorIndicator.get())
                    callingThread.interrupt();  //wait until last step to interrupt to be sure to clear everything out
            }
        }
    }

    private void indicateErrors() {
        indicateErrors(null);
    }

    private void indicateErrors(Exception e) {
        if (e == null)
            logger.error("Errors occurred building viz db.");
        else
            logger.error("Errors occurred building viz db.",e);
        errorIndicator.set(true);
    }

    private void logInputStream(InputStream stream, boolean error) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
            String line;
            while ((line = reader.readLine()) != null)
                if (error)
                    logger.error(line);
                else
                    logger.info(line);
        } catch (IOException e) {
            logger.error("An IO exception occurred while logging viz db builder output",e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }
}
