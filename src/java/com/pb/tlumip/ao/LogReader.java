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
    private static final String PT_SUMMARY_FILE_NAME = "PtIterationSummary.txt";
    private static final String TS_SUMMARY_FILE_NAME = "TsSummary.txt";

    private List<SegmentInformation> segments = new ArrayList<SegmentInformation>();
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
     */
    public void readLogsAndReport(String logDirectory, String logBaseName, String outputsDirectory) {
        readLogs(getFileList(logDirectory, logBaseName));
        this.outputsDirectory = new File(outputsDirectory);
        if (!this.outputsDirectory.exists())
            throw new IllegalArgumentException("Outputs directory must exists: " + outputsDirectory);
        if (!this.outputsDirectory.isDirectory())
            throw new IllegalArgumentException("Outputs directory must be a directory: " + outputsDirectory);
        writeSummaries();
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


            SortedMap<String,File> nameList = new TreeMap<String,File>();
            for (File f : (new File(logDirectory)).listFiles(ff)) {
                nameList.put(f.getName(),f);
            }
            for (String fileName : nameList.keySet()) {
                fileList.add(insertPoint,nameList.get(fileName));
            }
            insertPoint = fileList.size();
        }

        return fileList.toArray(new File[fileList.size()]);
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
        Matcher m;
        //first check if something ended
        for (int i = segments.size() - 1; i > -1; i--) {
            SegmentInformation si = segments.get(i);
            if (si.segmentFinished())
                continue;
            m = si.segment.messageEndRegexp.matcher(message);
            if (m.matches()) {
                //System.out.println(m.group());
                si.segmentEnded(parsedLine[0].trim(),m);
                break;
            }
        }
        //now check if something started
        for (Segment s : Segment.values()) {
            m = s.messageStartRegexp.matcher(message);
            if (m.matches()) {
                //System.out.println(m.group());
                segments.add(new SegmentInformation(s,parsedLine[0].trim(),m));
            }
        }
    }

    private EnumSet<Segment> getModuleSegments() {
//        return EnumSet.of(Segment.ED,Segment.SPG1,Segment.PI,Segment.PIDAF,Segment.SPG2,Segment.CT,Segment.ET,Segment.TS,Segment.TSDAF);
        return EnumSet.of(Segment.MODULE,Segment.PI,Segment.PIDAF);
    }

    private void writeModuleSummary() {
        TextFile moduleSummary = new TextFile();
        moduleSummary.addLine("Year,ModuleName,StartTime,EndTime,Duration");

        EnumSet<Segment> moduleSegments = getModuleSegments();
        for (SegmentInformation si : segments) {
            if (moduleSegments.contains(si.segment)) {
                if (si.segment == Segment.MODULE)
                    moduleSummary.addLine(getCsvLine(si.startCapturedGroups[2],si.startCapturedGroups[1],getStringTime(si.start),getStringTime(si.end),getTimeDifference(si.start, si.end)));
                else
                    moduleSummary.addLine(getCsvLine(si.startCapturedGroups[1],si.segment.toString(),getStringTime(si.start),getStringTime(si.end),getTimeDifference(si.start, si.end)));
            }
        }

        moduleSummary.writeTo(outputsDirectory.toString() + File.separator + MODULE_SUMMARY_FILE_NAME);
    }

    private void writePiIterationSummary() {
        int currentIteration = 9999;
        int currentCalibrationIteration = 9999;
        Map<SegmentInformation,List<List<Long>>> iterationClump = new LinkedHashMap<SegmentInformation,List<List<Long>>>();
        Map<SegmentInformation,Calendar[]> startEndMap = new HashMap<SegmentInformation,Calendar[]>();
        Calendar previousEnd = null;

        SegmentInformation currentPiSegment = null;

        for (SegmentInformation si : segments) {
            if (si.segment == Segment.PI_ITERATION) {
                int iteration = Integer.valueOf(si.startCapturedGroups[2]);
                //if new iterations, then restart
                if (currentIteration > iteration) {
                    int calibrationIteration = Integer.valueOf(si.startCapturedGroups[1]);
                    if (currentCalibrationIteration < calibrationIteration) {
                        iterationClump.get(currentPiSegment).add(new LinkedList<Long>());
                    } else {
                        if (previousEnd != null) {
                            startEndMap.get(currentPiSegment)[1] = previousEnd;
                        }
                        //this is a new pi run
                        //have to find the pi run this goes with
                        for (int i = segments.size() - 1; i > -1; i--) {
                            if (segments.get(i).segment != Segment.PI && segments.get(i).segment != Segment.PIDAF)
                                continue;
                            if (si.start.getTime().getTime() - segments.get(i).start.getTime().getTime() > 0) {
                                if (segments.get(i).end != null && si.start.getTime().getTime() - segments.get(i).end.getTime().getTime() > 0)
                                    continue;
                                currentPiSegment = segments.get(i);
                                break;
                            }
                        }
                        if (currentPiSegment == null) {
                            System.out.println("PI Iteration Problem: " + si.start.getTime() + ":" + si.startCapturedGroups[0]);
                            continue;
                        }

                        iterationClump.put(currentPiSegment,new LinkedList<List<Long>>());
                        iterationClump.get(currentPiSegment).add(new LinkedList<Long>());
                        startEndMap.put(currentPiSegment,new Calendar[2]);
                        startEndMap.get(currentPiSegment)[0] = si.start;
                    }
                    currentCalibrationIteration = calibrationIteration;
                }
                currentIteration = iteration;
                if (si.endCapturedGroups != null)
                    iterationClump.get(currentPiSegment).get(currentCalibrationIteration).add((long) (Float.valueOf(si.endCapturedGroups[2])*1000.0f));
                previousEnd = si.end;
            }
        }
        startEndMap.get(currentPiSegment)[1] = previousEnd;

        TextFile piIterationSummary = new TextFile();
        piIterationSummary.addLine("**PI Iteration Summary**\n");
        for (SegmentInformation piSegment : iterationClump.keySet()) {
            piIterationSummary.addLine(getPiIterationSummary(iterationClump.get(piSegment),startEndMap.get(piSegment)[0],startEndMap.get(piSegment)[1],piSegment));
        }
        piIterationSummary.writeTo(outputsDirectory.toString() + File.separator + PI_ITERATION_SUMMARY_FILE_NAME);

    }

    private String getPiIterationSummary(List<List<Long>> timesArray, Calendar start, Calendar end, SegmentInformation piSegment) {
        StringBuilder sb = new StringBuilder();
        sb.append("").append(piSegment.startCapturedGroups[1]).append(" PI run started at: ").append(getStringTime(piSegment.start)).append("\n");
        sb.append("\tStartup time: ").append(getTimeDifference(piSegment.start, start)).append("\n");
        long iterationTime = 0;

        int calibrationIteration = 0;
        for (List<Long> times : timesArray) {
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
            if (timesArray.size() > 1) {
                sb.append("\tCalibration iteration: ").append(calibrationIteration++).append("\n");
                indent = "\t\t";
            } else {
                indent = "\t";
            }

            sb.append(indent).append("Number of iterations: ").append(times.size()).append("\n");
            if (timesArray.size() > 1)
                sb.append(indent).append("Total iteration time: ").append(getStringDuration(sum)).append("\n");
            if (times.size() > 1) {
                sb.append(indent).append("Average iteration duration: ").append(getStringDuration((long) averageTime)).append("\n");
                sb.append(indent).append("Iteration duration standard error: ").append(getStringDuration((long) sdTime)).append("\n");
                sb.append(indent).append("Minimum iteration duration: ").append(getStringDuration(minTime)).append("\n");
                sb.append(indent).append("Maximum iteration duration: ").append(getStringDuration(maxTime)).append("\n");
            }
        }


        sb.append("\tTotal iteration time: ").append(getStringDuration(iterationTime)).append("\n");
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

        for (SegmentInformation si : segments) {
            if (si.segment == Segment.MODULE && (si.startCapturedGroups[1].equalsIgnoreCase("PT") || si.startCapturedGroups[1].equalsIgnoreCase("PTDAF"))) {
                if (end != null) {
                    ptSummary.addLine("\tFinish up time: " + getTimeDifference(si.end,end));
                }
                start = si.start;
                end = si.end;
                ptSummary.addLine("PT run started at: " + getStringTime(start));
            } else if (ptSegments.contains(si.segment)) {
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


        ptSummary.writeTo(outputsDirectory.toString() + File.separator + PT_SUMMARY_FILE_NAME);
    }

    private void writeTsSummary() {
        TextFile tsSummary = new TextFile();
        tsSummary.addLine("**TS Summary**");
        long totalHighway = 0;
        long totalTransit = 0;
        String subTransitType = "";
        long subTransit = 0;

        for (SegmentInformation si : segments) {
            Segment segment = si.segment;
            if (segment != Segment.TSDAF_HWY && segment != Segment.TS_TRANSIT)
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
                }
                if (si.endCapturedGroups != null) {
                    tsSummary.addLine("\t" + si.endCapturedGroups[1] + " highway assignment time: " + getTimeDifference(si.start,si.end));
                    totalHighway += getLongTimeDifference(si.start,si.end);
                } else {
                    tsSummary.addLine("\t? highway assignment time: " + getTimeDifference(si.start,si.end));
                }
            }
            else {
                if (si.endCapturedGroups != null) {
                    tsSummary.addLine("\t" + si.endCapturedGroups[1] + " " + si.endCapturedGroups[2] + " loading and skimming time: " + getTimeDifference(si.start,si.end));
                    totalTransit += getLongTimeDifference(si.start,si.end);
                    subTransit += getLongTimeDifference(si.start,si.end);
                    subTransitType = si.endCapturedGroups[1];
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

    private void writeSummaries() {
        writeModuleSummary();
        writePiIterationSummary();
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
            return "<00:00:01";
        else
            return String.format("%02d:%02d:%02d",hours,minutes,seconds);
    }

    private String getStringTime(Calendar time) {
        if (time == null)
            return "Unfinished";
        return time.getTime().toString();
    }

    private enum Segment {
        PI("AO will now start PI for simulation year (\\d\\d\\d\\d)","pi is complete"),
        PIDAF("AO will now start PIDAF for simulation year (\\d\\d\\d\\d)","pidaf is complete"),
        MODULE("AO will now start (.[^iI].*) for simulation year (\\d\\d\\d\\d)","(.+) is complete"),
        PI_ITERATION(".*Starting iteration (\\d+)-(\\d+).*",".*End of iteration (\\d+).  Time in seconds: (\\d+\\.\\d+).*"),
        PT_MC_LOGSUM("MasterTask, Sending out mode choice logsum calculation work"," Signaling that the ModeChoice Logsums are finished."),
        PT_AUTO_OWNERSHIP("MasterTask, Sending out auto-ownership work","MasterTask, Signaling that the AutoOwnership is finished."),
        PT_WORKPLACE_LOCATION("MasterTask, Sending calculate workplace location work","MasterTask, Signaling that the Workplace Location is finished."),
        PT_DC_LOGSUM("MasterTask, Sending destination choice logsums work","MasterTask, Signaling that the Destination Choice Logsums are finished."),
        PT_HH_PROCESSING("MasterTask, Starting ldt/sdt household processing work","MasterTask, Signaling that the all Hhs have been processed."),
        TSDAF_HWY("AO will now start TS DAF for simulation year (\\d\\d\\d\\d)|done with peak period transit loading and skimming\\.","done with (.*peak) highway assignment\\."),
        TS_TRANSIT("done with (.*)peak highway assignment\\.|done writing (.*) skims files\\.","done writing (.*peak) (.*) skims files\\.")
        ;

        private Pattern messageStartRegexp;
        private Pattern messageEndRegexp;

        private Segment(String messageStartRegexp, String messageEndRegexp) {
            this.messageStartRegexp = Pattern.compile(messageStartRegexp);
            this.messageEndRegexp = Pattern.compile(messageEndRegexp);
        }

    }

    private class SegmentInformation {
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
            //Date format is: dd-MMM-YY HH:mm
            int day = Integer.valueOf(date.substring(0,2));
            int month = getMonthInt(date.substring(3,6).toLowerCase());
            int year = Integer.valueOf(date.substring(7,9));
            int hour = Integer.valueOf(date.substring(10,12));
            int min = Integer.valueOf(date.substring(13,15));
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
     */
    public static void readAndReportLogs(String logDirectory, String logBaseName, String outputFileName) {
        LogReader lr = new LogReader();
        lr.readLogsAndReport(logDirectory,logBaseName,outputFileName);
    }

    public static void main(String ... args) {
       //readAndReportLogs("c:\\transfers\\logtest","mainevent.log,node0_event.log", "c:\\transfers\\logtest");
        readAndReportLogs("c:\\transfers\\logtest","main_event_nn.log,node0_event_nn.log", "c:\\transfers\\logtest");
        
    }


}
