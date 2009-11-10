package com.pb.tlumip.ao;

import com.pb.common.datafile.TextFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FilenameFilter;

/**
 * <p/>
 *
 * @version 1.0 Dec 12, 2007 - 11:20:46 AM
 *          </p>
 * @author crf
 */
public class LogReader {
    private static final long MILLISECONDS_IN_A_SECOND = 1000;
    private static final long MILLIESECONDS_IN_A_MINUTE = 60* MILLISECONDS_IN_A_SECOND;
    private static final long MILLISECONDS_IN_AN_HOUR = MILLIESECONDS_IN_A_MINUTE *60;
    private static final long MILLISECONDS_IN_A_DAY = MILLISECONDS_IN_AN_HOUR *60;

    private static final String MODULE_SUMMARY_FILE_NAME = "ModuleSummary.csv";
    private static final String PI_ITERATION_SUMMARY_FILE_NAME = "PiIterationSummary.txt";
    private static final String PI_CONVERGENCE_SUMMARY_FILE_NAME = "PiConvergenceSummary.txt";
    private static final String PT_SUMMARY_FILE_NAME = "PtIterationSummary.txt";
    private static final String TS_SUMMARY_FILE_NAME = "TsSummary.txt";

    private SortedSet<SegmentInformation> segments = new TreeSet<SegmentInformation>();
    private File outputsDirectory;
    

   /**
     * Read logs from a log directory and report a summary of times to run each module.
     *
     * @param logDirectory
     *        The directory in which the logs will be found.
     *
     * @param logBaseName
     *        The base name of the logs, including the extension (e.g. the base name of "event.log" and "event.log.1"
     *        is "event.log".
     *
     * @param outputsDirectory
     *        The directory where the output files are to be put.
    *
     * @param full
     *        {@code true} if full report is desired, {@code false} for a basic report.
     */
    public void readLogsAndReport(String logDirectory, String logBaseName, String outputsDirectory, boolean full) {
        readLogs(getFileList(logDirectory, logBaseName));
        this.outputsDirectory = new File(outputsDirectory);
        if (!this.outputsDirectory.exists())
            throw new IllegalArgumentException("Outputs directory must exists: " + outputsDirectory);
        if (!this.outputsDirectory.isDirectory())
            throw new IllegalArgumentException("Outputs directory must be a directory: " + outputsDirectory);
        if (full)
            writeSummaries();
        else
            writeModuleSummary();
    }

    private File[] getFileList(String logDirectory,String logBaseName) {
        String[] baseNames = logBaseName.split(",");
        List<File> fileList = new LinkedList<File>();

        int insertPoint = 0;
        for (final String baseName : baseNames) {
            FilenameFilter ff = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(baseName);
                }
            };


            SortedMap<String,File> nameList = new TreeMap<String,File>(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    String[] subo1 = o1.split("[.]");
                    String[] subo2 = o2.split("[.]");
                    int c = subo1[0].compareTo(subo2[0]);
                    if (subo1.length != subo2.length)
                        c = o1.length() < o2.length() ? -1 : 1;
                    else if (c == 0)
                        try {
                            c = new Integer(subo1[subo1.length-1]).compareTo(new Integer(subo2[subo2.length-1]));
                        } catch (Exception e) {
                            //swallow
                        }
                    return c;
                }
            });
            for (File f : (new File(logDirectory)).listFiles(ff)) {
                nameList.put(f.getName(),f);
            }
            for (String fileName : nameList.keySet()) {
                fileList.add(insertPoint,nameList.get(fileName));
            }
            insertPoint = fileList.size();
        }

        return fileList.toArray(new File[fileList.size()]);
//        File[] test = new File[1];
//        test[0] = fileList.get(0);
//        test[0] = fileList.get(fileList.size() - 1);
//        return test;

    }

    private void readLogs(File[] fileList) {
        for (File f : fileList) {
            System.out.println("Reading file " + f.getName());
            for (String line : (new TextFile(f.getPath()))) {
                checkLine(line);
            }
        }
    }

    private void checkLine(String line) {
        String[] parsedLine = line.split(",",4);
        if (parsedLine.length < 4)
            return;
        String message = parsedLine[3].trim();
        Matcher m = null;
        SegmentInformation si = null;
        for (SegmentInformation tempSi : segments) {
            if (!tempSi.segmentFinished()) {
                Matcher mm = tempSi.segment.messageEndRegexp.matcher(message);
                if (mm.matches()) {
                    si = tempSi;
                    m = mm;
                }
            }
        }
        if (si != null)
            si.segmentEnded(parsedLine[0].trim(),m);
        //now check if something started
        for (Segment s : Segment.values()) {
            m = s.messageStartRegexp.matcher(message);
            if (m.matches()) {
                segments.add(new SegmentInformation(s,parsedLine[0].trim(),m));
            }
        }
    }

    private EnumSet<Segment> getModuleSegments() {
        return EnumSet.of(Segment.MODULE,Segment.PI);
    }

    private void writeModuleSummary() {
        TextFile moduleSummary = new TextFile();
        moduleSummary.addLine("Year,ModuleName,StartTime,EndTime,Duration");
        String currentYear = "";
        Calendar currentYearStart = null;
        Calendar currentYearEnd = null;
        long currentYearTime = 0;
        String firstYear = "";
        Calendar fullRunStart = null;
        //long fullRunTime = 0;

        EnumSet<Segment> moduleSegments = getModuleSegments();
        for (SegmentInformation si : segments) {
            String year;
            if (moduleSegments.contains(si.segment)) {
                if (si.segment == Segment.MODULE) 
                    year = si.startCapturedGroups[2];
                else
                    year = si.startCapturedGroups[1];
                if (!currentYear.equals(year)) {
                    if (!currentYear.equals(""))
                        moduleSummary.addLine(getCsvLine(currentYear,"Full Year Run",getStringTime(currentYearStart),getStringTime(currentYearEnd),getStringDuration(currentYearTime)));
                    else {
                        firstYear = year;
                        fullRunStart = si.start;
                    }
                    //fullRunTime += currentYearTime;
                    currentYearStart = si.start;
                    currentYear = year;
                    currentYearTime = 0;
                }

                //Do this after year check to get full year summary order correct
                if (si.segment == Segment.MODULE)
                    moduleSummary.addLine(getCsvLine(year,si.startCapturedGroups[1],getStringTime(si.start),getStringTime(si.end),getTimeDifference(si.start, si.end)));
                else
                    moduleSummary.addLine(getCsvLine(year,si.segment.toString(),getStringTime(si.start),getStringTime(si.end),getTimeDifference(si.start, si.end)));

                if (si.end != null)
                    currentYearTime += si.end.getTime().getTime() - si.start.getTime().getTime();
                currentYearEnd = si.end;
            }
        }
        //moduleSummary.addLine(getCsvLine(currentYear,"Full Year Run",getStringTime(currentYearStart),getStringTime(currentYearEnd),getStringDuration(currentYearTime)));
        //moduleSummary.addLine(getCsvLine(firstYear + "-" + currentYear,"Full Run",getStringTime(fullRunStart),getStringTime(currentYearEnd),getStringDuration(fullRunTime)));
        moduleSummary.addLine(getCsvLine(currentYear,"Full Year Run",getStringTime(currentYearStart),getStringTime(currentYearEnd),getTimeDifference(currentYearStart,currentYearEnd)));
        moduleSummary.addLine(getCsvLine(firstYear + "-" + currentYear,"Full Run",getStringTime(fullRunStart),getStringTime(currentYearEnd),getTimeDifference(fullRunStart,currentYearEnd)));

        moduleSummary.writeTo(outputsDirectory.toString() + File.separator + MODULE_SUMMARY_FILE_NAME);
    }

    private void writePiIterationSummary() {
        int currentIteration = 9999;
        int currentCalibrationIteration = 9999;
        Map<SegmentInformation,List<List<Long>>> iterationTimes = new LinkedHashMap<SegmentInformation,List<List<Long>>>();
        Map<SegmentInformation,List<List<Long>>> globalSearchTimes = new LinkedHashMap<SegmentInformation,List<List<Long>>>();
        Map<SegmentInformation,Calendar[]> startEndMap = new HashMap<SegmentInformation,Calendar[]>();
        Calendar previousEnd = null;

        SegmentInformation currentPiSegment = null;

//        for (SegmentInformation si : segments) {
//            System.out.println(si.segment + ":" + si.start.getTime());
//        }

        for (SegmentInformation si : segments) {
            if (si.segment == Segment.PI)  {
                currentPiSegment = si;
                iterationTimes.put(currentPiSegment,new LinkedList<List<Long>>());
                iterationTimes.get(currentPiSegment).add(new LinkedList<Long>());
                globalSearchTimes.put(currentPiSegment,new LinkedList<List<Long>>());
                globalSearchTimes.get(currentPiSegment).add(new LinkedList<Long>());
                startEndMap.put(currentPiSegment,new Calendar[2]);
                startEndMap.get(currentPiSegment)[0] = si.start;
            } else if (si.segment == Segment.PI_ITERATION) {
               if (currentPiSegment == null)
                   continue;
               int iteration = Integer.valueOf(si.startCapturedGroups[2]);
                //if new iterations, then restart
                if (currentIteration > iteration) {
                    int calibrationIteration = Integer.valueOf(si.startCapturedGroups[1]);
                    if (currentCalibrationIteration < calibrationIteration) {
                        iterationTimes.get(currentPiSegment).add(new LinkedList<Long>());
                        globalSearchTimes.get(currentPiSegment).add(new LinkedList<Long>());
                    }
                    currentCalibrationIteration = calibrationIteration;
                }
                currentIteration = iteration;
                if (si.endCapturedGroups != null)
                    iterationTimes.get(currentPiSegment).get(currentCalibrationIteration).add((long) (Float.valueOf(si.endCapturedGroups[2])*1000.0f));
                previousEnd = si.end;
                if (si.end != null) {
                    startEndMap.get(currentPiSegment)[1] = si.end;
                }
            } else if (si.segment == Segment.PI_GLOBAL_SEARCH) {
               if (currentPiSegment == null)
                   continue;
                if (si.end != null) {
                    //peculiarities of logging may allow reverse times
                    long start = si.start.getTime().getTime();
                    long end = si.end.getTime().getTime();
                    if (end > start)
                        globalSearchTimes.get(currentPiSegment).get(currentCalibrationIteration).add(si.end.getTime().getTime() - si.start.getTime().getTime());
//                    else
//                        System.out.println("Iteration #" + currentIteration + "; start time: " + start + "; end time: " + end);
                }
            }
        }
        if (currentPiSegment != null)
            startEndMap.get(currentPiSegment)[1] = previousEnd;

        TextFile piIterationSummary = new TextFile();
        piIterationSummary.addLine("**PI Iteration Summary**\n");
        for (SegmentInformation piSegment : iterationTimes.keySet()) {
            piIterationSummary.addLine(getPiIterationSummary(iterationTimes.get(piSegment),globalSearchTimes.get(piSegment),startEndMap.get(piSegment)[0],startEndMap.get(piSegment)[1],piSegment));
        }
        piIterationSummary.writeTo(outputsDirectory.toString() + File.separator + PI_ITERATION_SUMMARY_FILE_NAME);

    }

    private String getPiIterationSummary(List<List<Long>> iterationTimesArray, List<List<Long>> globalSearchTimesArray, Calendar start, Calendar end, SegmentInformation piSegment) {
        StringBuilder sb = new StringBuilder();
        sb.append("").append(piSegment.startCapturedGroups[1]).append(" PI run started at: ").append(getStringTime(piSegment.start)).append("\n");
        sb.append("\tStartup time: ").append(getTimeDifference(piSegment.start, start)).append("\n");
        long iterationTime = 0;
        long globalSearchIterationTime = 0;

        int calibrationIteration = 0;
        for (int i = 0; i < iterationTimesArray.size(); i++) {
            List<Long> times = iterationTimesArray.get(i);
            long maxTime = -1;
            long minTime = Long.MAX_VALUE;

            long sum = 0;
            long squareSum = 0;
            //sd = ave(x^2) - ave(x)^2

            for (Long duration : times) {
//                System.out.println("" + newTime + ":" + lastTime + ":" + delta);
                if (duration > maxTime)
                    maxTime = duration;
                if (duration < minTime)
                    minTime = duration;
                sum += duration;
                squareSum += duration*duration;
            }

            iterationTime += sum;
            float averageTime = (float) sum / (float) times.size();
            double sdTime = Math.sqrt((float) squareSum / (float) times.size() - averageTime);



            String indent;
            if (iterationTimesArray.size() > 1) {
                sb.append("\tCalibration iteration: ").append(calibrationIteration++).append("\n");
                indent = "\t\t";
            } else {
                indent = "\t";
            }

            sb.append(indent).append("Number of iterations: ").append(times.size()).append("\n");
            if (iterationTimesArray.size() > 1)
                sb.append(indent).append("Total iteration time: ").append(getStringDuration(sum)).append("\n");
            if (times.size() > 1) {
                sb.append(indent).append("Average iteration duration: ").append(getStringDuration((long) averageTime)).append("\n");
                sb.append(indent).append("Iteration duration standard error: ").append(getStringDuration((long) sdTime)).append("\n");
                sb.append(indent).append("Minimum iteration duration: ").append(getStringDuration(minTime)).append("\n");
                sb.append(indent).append("Maximum iteration duration: ").append(getStringDuration(maxTime)).append("\n");
            }

            times = globalSearchTimesArray.get(i);
            maxTime = -1;
            minTime = Long.MAX_VALUE;

            sum = 0;
            squareSum = 0;
            //sd = ave(x^2) - ave(x)^2

            for (Long duration : times) {
                if (duration > maxTime)
                    maxTime = duration;
                if (duration < minTime)
                    minTime = duration;
                sum += duration;
                squareSum += duration*duration;
            }

            globalSearchIterationTime += sum;
            averageTime = (float) sum / (float) times.size();
            sdTime = Math.sqrt((float) squareSum / (float) times.size() - averageTime);

            if (globalSearchTimesArray.size() > 1) {
                sb.append(indent).append("Total global search iteration time: ").append(getStringDuration(sum)).append("\n");
            }
            if (times.size() > 1) {
                sb.append(indent).append("Average global search iteration duration: ").append(getStringDuration((long) averageTime)).append("\n");
                sb.append(indent).append("Global search iteration duration standard error: ").append(getStringDuration((long) sdTime)).append("\n");
                sb.append(indent).append("Minimum global search iteration duration: ").append(getStringDuration(minTime)).append("\n");
                sb.append(indent).append("Maximum global search iteration duration: ").append(getStringDuration(maxTime)).append("\n");
            }
        }


        sb.append("\tTotal iteration time: ").append(getStringDuration(iterationTime)).append("\n");
        sb.append("\tTotal global search iteration time: ").append(getStringDuration(globalSearchIterationTime)).append("\n");
        if (piSegment.end != null && end != null)
            sb.append("\tFinish up time: ").append(getTimeDifference(end,piSegment.end)).append("\n");

        return sb.toString();
    }

    private void writePtSummary() {
        EnumSet<Segment> ptSegments = EnumSet.of(Segment.PT_MC_LOGSUM,
                                                 Segment.PT_AUTO_OWNERSHIP,
                                                 Segment.PT_WORKPLACE_LOCATION,
                                                 Segment.PT_DC_LOGSUM,
                                                 Segment.PT_HH_PROCESSING);
        TextFile ptSummary = new TextFile();
        ptSummary.addLine("**PT Summary**");
        Calendar start = null;
        Calendar end = null;
        Calendar lastEnd = null;

        for (SegmentInformation si : segments) {
            if (si.segment == Segment.MODULE && (si.startCapturedGroups[1].equalsIgnoreCase("PT") || si.startCapturedGroups[1].equalsIgnoreCase("PTDAF"))) {
                if (end != null && lastEnd != null) {
                    if (lastEnd.compareTo(end) < 0)
                        ptSummary.addLine("\tFinish up time: " + getTimeDifference(lastEnd,end));
                    else {
                        System.out.println("end time mismatch for pt: " + lastEnd.getTime() + " - " + end.getTime());
                        ptSummary.addLine("\tFinish up time: <00:01:00");
                    }
                }
                start = si.start;
                end = si.end;
                ptSummary.addLine("\n" + si.startCapturedGroups[2] + " PT run started at: " + getStringTime(start));
            } else if (ptSegments.contains(si.segment)) {
                lastEnd = si.end;
                if (start != null) {
                    ptSummary.addLine("\tStart up time: " + getTimeDifference(start,si.start));
                    start = null;
                }
                switch (si.segment) {
                    case PT_MC_LOGSUM : {
                        ptSummary.addLine("\tMC logsum time: " + getTimeDifference(si.start,si.end));
                        break;
                    }
                    case PT_AUTO_OWNERSHIP : {
                        ptSummary.addLine("\tAuto-ownership time: " + getTimeDifference(si.start,si.end));
                        break;
                    }
                    case PT_WORKPLACE_LOCATION : {
                        ptSummary.addLine("\tWorkplace location time: " + getTimeDifference(si.start,si.end));
                        break;
                    }
                    case PT_DC_LOGSUM : {
                        ptSummary.addLine("\tDC logsum time: " + getTimeDifference(si.start,si.end));
                        break;
                    }
                    case PT_HH_PROCESSING : {
                        ptSummary.addLine("\tHousehold processing time: " + getTimeDifference(si.start,si.end));
                        break;
                    }
                }
            }
        }

        if (end != null && lastEnd != null) {
            if (lastEnd.compareTo(end) < 0)
                ptSummary.addLine("\tFinish up time: " + getTimeDifference(lastEnd,end));
            else {
                System.out.println("end time mismatch for pt: " + lastEnd.getTime() + " - " + end.getTime());
                ptSummary.addLine("\tFinish up time: <00:01:00");
            }
        }


        ptSummary.writeTo(outputsDirectory.toString() + File.separator + PT_SUMMARY_FILE_NAME);
    }

    private void writeTsSummary() {
        TextFile tsSummary = new TextFile();
        tsSummary.addLine("**TS Summary**");
        long totalHighway = 0;
        long totalTransit = 0;
        String subTransitType = "";
        long subTransit = 0;

        long subSubTransit = 0;

        for (SegmentInformation si : segments) {
            Segment segment = si.segment;
            if (segment != Segment.TSDAF_HWY && segment != Segment.TS_TRANSIT && segment != Segment.TS_SUB_TRANSIT)
                continue;
            if (segment == Segment.TSDAF_HWY) {
                if (subTransit > 0)
                    tsSummary.addLine("\ttotal " + subTransitType + " transit assignment time: " + getStringDuration(subTransit));
                subTransit = 0;
                if (si.startCapturedGroups[1] != null) {
                    //start new ts
                    //first write out last results
                    if (totalHighway > 0)
                        tsSummary.addLine("\ttotal highway assignment time: " + getStringDuration(totalHighway));
                    if (totalTransit > 0)
                        tsSummary.addLine("\ttotal transit assignment time: " + getStringDuration(totalTransit));
                    totalHighway = 0;
                    totalTransit = 0;
                    tsSummary.addLine("\n" + si.startCapturedGroups[1] + " TS Run started at " + getStringTime(si.start));
                } else {
                    tsSummary.addLine("");
                }
                if (si.endCapturedGroups != null) {
                    tsSummary.addLine("\t" + si.endCapturedGroups[1] + " highway assignment time: " + getTimeDifference(si.start,si.end));
                    totalHighway += getLongTimeDifference(si.start,si.end);
                } else {
                    if (!getTimeDifference(si.start, si.end).equals("NA"))
                        tsSummary.addLine("\t? highway assignment time: " + getTimeDifference(si.start,si.end));
                }
            }
            else if (segment == Segment.TS_TRANSIT) {
                if (si.endCapturedGroups != null) {
                    tsSummary.addLine("\t" + si.endCapturedGroups[1] + " " + si.endCapturedGroups[2] + " loading and skimming time: " + getTimeDifference(si.start,si.end));
                    totalTransit += getLongTimeDifference(si.start,si.end);
                    subTransit += getLongTimeDifference(si.start,si.end);
                    subTransitType = si.endCapturedGroups[1];

                }
            } else {
                if (si.endCapturedGroups != null) {
                    if (si.startCapturedGroups[0].equals(si.endCapturedGroups[0])) {
                        subSubTransit += getLongTimeDifference(si.start,si.end);
                    } else {
                        subSubTransit += getLongTimeDifference(si.start,si.end);
                        String transitName = "";
                        if (si.startCapturedGroups[0].equals("AIR")) {
                            transitName = "Air";
                        } else if (si.startCapturedGroups[0].equals("HSR_DRIVE")) {
                            transitName = "High Speed Rail Drive";
                        } else if (si.startCapturedGroups[0].equals("TRANSIT_DRIVE")) {
                            transitName = "Intercity Transit Drive";
                        } else if (si.startCapturedGroups[0].equals("DR_TRAN")) {
                            transitName = "Intracity Transit Drive";
                        } else if (si.startCapturedGroups[0].equals("HSR_WALK")) {
                            transitName = "High Speed Rail Walk";
                        } else if (si.startCapturedGroups[0].equals("TRANSIT_WALK")) {
                            transitName = "Intercity Transit Walk";
                        } else if (si.startCapturedGroups[0].equals("WK_TRAN")) {
                            transitName = "Intracity Transit Walk";
                        }
                        tsSummary.addLine("\t" + subTransitType + " " + transitName + " assignment time: " + getStringDuration(subSubTransit));
                        subSubTransit = 0;
                    }
                }
            }
        }

        //write out last one
        if (subTransit > 0)
                tsSummary.addLine("\ttotal " + subTransitType + " transit assignment time: " + getStringDuration(subTransit));
        if (totalHighway > 0)
            tsSummary.addLine("\ttotal highway assignment time: " + getStringDuration(totalHighway));
        if (totalTransit > 0)
            tsSummary.addLine("\ttotal transit assignment time: " + getStringDuration(totalTransit));

        tsSummary.writeTo(outputsDirectory.toString() + File.separator + TS_SUMMARY_FILE_NAME);
    }

    private void writePiConvergenceSumary() {
        Map<Integer,String> results = new TreeMap<Integer,String>();
        for (SegmentInformation si : segments) {
            Segment s = si.segment;
            if (s == Segment.PI_CONVERGENCE) {
                int year = Integer.parseInt(si.startCapturedGroups[1]);
                if (si.segmentFinished()) {
                    if (si.endCapturedGroups[1] != null)
                        results.put(year,"PI has reached " + si.endCapturedGroups[1] + " in " + si.endCapturedGroups[2] + " iterations and " + si.endCapturedGroups[3] + " seconds");
                    else
                        results.put(year,"PI has reached " + si.endCapturedGroups[4] + " in " + si.endCapturedGroups[5] + " seconds");
                } else {
                    results.put(year,null);
                }
            } else if (s == Segment.PI_MERIT_MEASURE) {
                int year = Integer.parseInt(si.startCapturedGroups[1]);
                if (results.get(year) == null)
                    results.put(year,"unfinished");
                else
                    results.put(year,results.get(year) + " (final merit measure: " + si.endCapturedGroups[1] + ")");
            }
        }
        TextFile tf = new TextFile();
        tf.addLine("year: report");
        for (int year : results.keySet())
            tf.addLine("" + year + ": " + results.get(year));
        tf.writeTo(outputsDirectory.toString() + File.separator + PI_CONVERGENCE_SUMMARY_FILE_NAME);
    }

    private void writeSummaries() {
        writeModuleSummary();
        writePiIterationSummary();
        writePiConvergenceSumary();
        writePtSummary();
        writeTsSummary();
    }

    private String getCsvLine(String ... elements) {
        StringBuilder s = new StringBuilder();
        s.append(elements[0]);
        for (int i = 1; i < elements.length; i++)
            s.append(",").append(elements[i]);
        return s.toString();
    }

    /**
     * This method gets the time difference between two Calendar objects.  Note that this only gets the time down the second
     * and doesn't round the seconds to the nearest millisecond.  Also, this won't take daylight savings time into account
     * correctly, so beware when using this around "spring forward" and "fall back" times.
     *
     * @param time1
     *        The (temporally) first time.
     *
     * @param time2
     *        The second (later) time.
     *
     * @return a "nicely" formatted time difference between the times.
     *
     * @throws IllegalArgumentException if time2 is before time1.
     */
    private String getTimeDifference(Calendar time1, Calendar time2) {
        if (time2 == null)
            return "NA";
        long duration = getLongTimeDifference(time1,time2);
        return getStringDuration(duration);
    }

    private long getLongTimeDifference(Calendar time1, Calendar time2) {
        return time2.getTime().getTime() - time1.getTime().getTime();
    }

    private String getStringDuration(long duration) {
        if (duration < 0)
            throw new IllegalArgumentException("Error: duration must be positive when calculating differences!");
        long days = duration / MILLISECONDS_IN_A_DAY;
        long hours = (duration - days* MILLISECONDS_IN_A_DAY) / MILLISECONDS_IN_AN_HOUR;
        long minutes = (duration - days* MILLISECONDS_IN_A_DAY - hours* MILLISECONDS_IN_AN_HOUR) / MILLIESECONDS_IN_A_MINUTE;
        long seconds = (duration - days* MILLISECONDS_IN_A_DAY - hours* MILLISECONDS_IN_AN_HOUR - minutes* MILLIESECONDS_IN_A_MINUTE) / MILLISECONDS_IN_A_SECOND;
        //ignore days - we shouldn't have anything that runs that long
        if (days == 0 && minutes == 0 && seconds == 0)
            return "<00:01:00";
        else
            return String.format("%02d:%02d:%02d",hours,minutes,seconds);
    }

    private String getStringTime(Calendar time) {
        if (time == null)
            return "Unfinished";
        return time.getTime().toString();
    }

    private enum Segment {
        PI("AO will now start PI.*? for simulation year (\\d\\d\\d\\d)","pi.*? is complete"),
        MODULE("AO will now start (.[^iI].*) for simulation year (\\d\\d\\d\\d)","(.+) is complete"),
        PI_ITERATION(".*Starting iteration (\\d+)-(\\d+).*",".*End of iteration (\\d+).  Time in seconds: (\\d+\\.\\d+).*"),
        PI_GLOBAL_SEARCH("Calculating average commodity price change","Finished calculating average commodity price change"),
//        PI_CONVERGENCE("com.pb.models.pt.daf.PTMasterTask,               Time Interval: (\\d+)","PI has reached (equilibrium) in (\\d+). Time in seconds: (\\d+)|PI has reached (maxIterations). Time in seconds: (\\d+)"),
//        PI_CONVERGENCE("Time Interval: (\\d+)","PI has reached (equilibrium) in (\\d+). Time in seconds: (\\d+)|PI has reached (maxIterations). Time in seconds: (\\d+)"),
        PI_CONVERGENCE("Writing ActivitiesW file: .*t(\\d+)/ActivitiesW.csv","\\*   PI has reached (equilibrium) in (\\d+). Time in seconds: (\\d+)|\\*   PI has reached (maxIterations). Time in seconds: (\\d+)"),
        PI_MERIT_MEASURE("Writing ActivitiesW file: .*t(\\d+)/ActivitiesW.csv","\\*   Final merit measure is (\\d+\\.\\d+E\\d+)"),
        PT_MC_LOGSUM("MasterTask, Sending out mode choice logsum calculation work","MasterTask, Signaling that the ModeChoice Logsums are finished."),
        PT_AUTO_OWNERSHIP("MasterTask, Sending out auto-ownership work","MasterTask, Signaling that the AutoOwnership is finished."),
        PT_WORKPLACE_LOCATION("MasterTask, Sending calculate workplace location work","MasterTask, Signaling that the Workplace Location is finished."),
        PT_DC_LOGSUM("MasterTask, Sending destination choice logsums work","MasterTask, Signaling that the Destination Choice Logsums are finished."),
        PT_HH_PROCESSING("MasterTask, Starting ldt/sdt household processing work","MasterTask, Signaling that the all Hhs have been processed."),
        //TSDAF_HWY("AO will now start TS DAF for simulation year (\\d\\d\\d\\d)|AO will now start TS DAF peak & offPeak periods models for simulation year (\\d\\d\\d\\d)|done with (.*) period transit loading and skimming\\.","done with (.*peak) highway assignment\\."),
        TSDAF_HWY("AO will now start TS DAF peak & offPeak periods models for simulation year (\\d\\d\\d\\d)|done with (.*) period transit loading and skimming\\.","done with (.*peak) highway assignment\\."),
        TS_TRANSIT("done with (.*peak) highway assignment\\.|done with (.*peak) period transit loading and skimming\\.","done with (.*peak) period (transit) loading and skimming\\."),
        TS_SUB_TRANSIT("AIR|HSR_DRIVE|TRANSIT_DRIVE|DR_TRAN|HSR_WALK|TRANSIT_WALK|WK_TRAN","AIR|HSR_DRIVE|TRANSIT_DRIVE|DR_TRAN|HSR_WALK|TRANSIT_WALK|WK_TRAN|(.*)peak period walk access intracity task finished\\.")
        ;


        private Pattern messageStartRegexp;
        private Pattern messageEndRegexp;

        private Segment(String messageStartRegexp, String messageEndRegexp) {
            this.messageStartRegexp = Pattern.compile(messageStartRegexp);
            this.messageEndRegexp = Pattern.compile(messageEndRegexp);
        }

    }

    private class SegmentInformation implements Comparable<SegmentInformation> {
        private Segment segment;
        private Calendar start;
        private Calendar end = null;
        private String[] startCapturedGroups;
        private String[] endCapturedGroups;

        SegmentInformation(Segment segment,String startTime, Matcher startMatcher) {
            this.segment = segment;
            start = parseDateFromString(startTime);
            startCapturedGroups = new String[startMatcher.groupCount()+1];
            for (int i = 0; i < startCapturedGroups.length; i++) {
                startCapturedGroups[i] = startMatcher.group(i);
            }
        }

        void segmentEnded(String endTime, Matcher endMatcher) {
            end = parseDateFromString(endTime);
            endCapturedGroups = new String[endMatcher.groupCount()+1];
            for (int i = 0; i < endCapturedGroups.length; i++) {
                endCapturedGroups[i] = endMatcher.group(i);
            }
        }

        boolean segmentFinished() {
            return end != null;
        }

        private Calendar parseDateFromString(String date) {
            //Date format is: dd-MMM-YY HH:mm:[ss]
            int day = Integer.valueOf(date.substring(0,2));
            int month = getMonthInt(date.substring(3,6).toLowerCase());
            int year = 2000+Integer.valueOf(date.substring(7,9));
            int hour = Integer.valueOf(date.substring(10,12));
            int min = Integer.valueOf(date.substring(13,15));
            if (date.length() > 15) {
                int sec = Integer.valueOf(date.substring(16,18));
                return new GregorianCalendar(year,month,day,hour,min,sec);
            }
            return new GregorianCalendar(year,month,day,hour,min);
        }

        private int getMonthInt(String month) {
            if (month.equals("jan")) {
                return 0;
            } else if (month.equals("feb")) {
                return 1;
            } else if (month.equals("mar")) {
                return 2;
            } else if (month.equals("apr")) {
                return 3;
            } else if (month.equals("may")) {
                return 4;
            } else if (month.equals("jun")) {
                return 5;
            } else if (month.equals("jul")) {
                return 6;
            } else if (month.equals("aug")) {
                return 7;
            } else if (month.equals("sep")) {
                return 8;
            } else if (month.equals("oct")) {
                return 9;
            } else if (month.equals("nov")) {
                return 10;
            } else if (month.equals("dec")) {
                return 11;
            } else {
                throw new IllegalArgumentException("Month not recognized: " + month);
            }
        }

        public int compareTo(SegmentInformation si) {
            int timeDiff = (new Long(start.getTime().getTime())).compareTo(si.start.getTime().getTime());
            if (timeDiff != 0)
                return timeDiff;
            else {
//                System.out.println(this.segment + ":" + si.segment);
//                System.out.println(this.equals(si));
                //if it isn't equal and has the same time, then creation/insert preference is give
                return (this.equals(si)) ? 0 : 1;
            }
        }
    }

    /**
     * Read logs from a log directory and report a summary of times to run each module.
     *
     * @param logDirectory
     *        The directory in which the logs will be found.
     *
     * @param logBaseName
     *        The base name of the logs, including the extension (e.g. the base name of "event.log" and "event.log.1"
     *        is "event.log".
     *
     * @param outputFileName
     *        The full path and filename of the output file to put the summary csv file.
     *
     * @param full
     *        {@code true} if full report is desired, {@code false} for a basic report.
     */
    public static void readAndReportLogs(String logDirectory, String logBaseName, String outputFileName,boolean full) {
        LogReader lr = new LogReader();
        lr.readLogsAndReport(logDirectory,logBaseName,outputFileName,full);
    }

    public static void readAndReportLogs(String logDirectory, String logBaseName, String outputFileName) {
        readAndReportLogs(logDirectory,logBaseName,outputFileName,true);
    }

    public static String usage() {
        StringBuilder sb = new StringBuilder();
        sb.append("LogReader: Reads TLUMIP log files and creates runtime reports (still beta).\n");
        sb.append("  usage: java -classpath [...] com.pb.tlumip.ao.LogReader [log directory] [output directory] (-f)\n");
        sb.append("    where:\n");
        sb.append("    [log directory] = the directory where the log files are located\n");
        sb.append("    [output directory] = the directory where the output files are to be placed\n");
        sb.append("    -f = optional parameter which forces the reader to do a full summary (less reliable)");
        return sb.toString();
    }

    public static void main(String ... args) {
//        System.out.println(new Date());
        //readAndReportLogs("c:\\transfers\\logtest","main_event.log,node0_event.log", "c:\\transfers\\logtest");
        //readAndReportLogs("c:\\transfers\\logtesthhhertergfe","main_event_4period.log,node0_event_4period.log", "c:\\transfers\\logtest");
        //readAndReportLogs("C:\\Models\\TLUMIP\\final_deliverable_logs\\fd_runtime","main_event.log,node0_event.log", "C:\\Models\\TLUMIP\\final_deliverable_logs\\fd_runtime");
//        String base = "C:\\chris\\projects\\tlumip\\gui\\FinalDistributions\\Gui\\temp";
//        String[] base_folders = {"base","conf1","conf2"};
//        for (String folder : base_folders)
//            readAndReportLogs(base + folder,"main_event.log,node0_event.log", base + folder);
//        readAndReportLogs(base,"main_event.log,node0_event.log", base);
//        System.out.println(new Date());

        String logDirectory;
        String outputDirectory;
        boolean full = false;
        switch (args.length) {
            case 3 :
                if (args[2].equals("-f"))
                    full = true;
                else
                    throw new IllegalArgumentException("Unknown argument: " + args[2]);
            case 2 :
                logDirectory = args[0];
                outputDirectory = args[1];
                break;
            default:
                System.out.println("Invalid argument count!");
                System.out.println(usage());
                return;
        }
        String logBase = "main_event.log";
        if (full)
            logBase += ",node0_event.log";
        readAndReportLogs(logDirectory,logBase,outputDirectory,full);
    }


}
