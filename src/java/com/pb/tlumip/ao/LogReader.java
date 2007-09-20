package com.pb.tlumip.ao;

import com.pb.common.datafile.TextFile;

import java.util.*;
import java.io.File;
import java.io.FilenameFilter;

/**
 * <p/>
 *
 * @version 1.0 Sep 20, 2007 - 11:34:38 AM
 *          </p>
 * @author: crf
 */
public class LogReader {
    private static Map<String,String> startEndStringMap = getStartEndStringMap();
    private static Map<String,Integer> stringToIntMonth = getStringToIntMonthMap();
    //This message filter will serve to filter out all unnecessary messages so that full parsing isn't necessary
    // the ending message will use the name of the module as a filter
    private static String messageFilter = "AO will now start ";
    private static String endMessagePart = "is complete";
    private static long millisecondsInASecond = 1000;
    private static long millisecondsInAMinute = 60*millisecondsInASecond;
    private static long millisecondsInAnHour = millisecondsInAMinute *60;
    private static long millisecondsInADay = millisecondsInAnHour *60;

    private TextFile outFile = initializeTextFile();
    private String currentModule = null;
    private Calendar startTime = null;
    private String modelYear = null;

    private TextFile initializeTextFile() {
        TextFile outFile = new TextFile();
        outFile.addLine("Year,ModuleName,StartTime,EndTime,Duration");
        return outFile;
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
    public void readLogsAndReport(String logDirectory, String logBaseName, String outputFileName) {
        readLogs(getFileList(logDirectory, logBaseName));
        outFile.writeTo(outputFileName);
    }

    private File[] getFileList(String logDirectory,final String logBaseName) {
        FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(logBaseName);
            }
        };

        File[] fileList = (new File(logDirectory)).listFiles(ff);

        //sort the files correctly using the convenient fact that they'll be ordered in the opposite of what we want
        TreeSet<String> nameList = new TreeSet<String>();
        for (File f : fileList) {
            nameList.add(f.getName());
        }
        File[] sortedFileList = new File[fileList.length];
        int counter = fileList.length - 1;
        for (String fileName : nameList) {
            for (File f : fileList) {
                if (f.getName().equals(fileName)) {
                    sortedFileList[counter--] = f;
                    break;
                }
            }
        }
        return sortedFileList;
    }

    private void readLogs(File[] fileList) {
        for (File f : fileList) {
            System.out.println("Reading file " + f.getName());
            for (String line : (new TextFile(f.getPath()))) {
                checkLine(line);
            }
        }
        //handle case of module not finished
        if (!(currentModule == null)) {
            recordFinishedModule(null);
        }
    }

    private void checkLine(String line) {
        String[] parsedLine = line.split(",",4);
        if (parsedLine.length < 4)
            return;
        String message = parsedLine[3].trim();
        //filter out line if it is not of interest
        if (message.startsWith(messageFilter))
            moduleStartChecker(message,parsedLine[0].trim());
        if (currentModule != null) {
            if ((message.length() < currentModule.length()) || !message.substring(0,currentModule.length()).equalsIgnoreCase(currentModule))
                return;
            moduleEndChecker(message,parsedLine[0].trim());
        }
    }

    private void moduleStartChecker(String message, String date) {
        for (String s : startEndStringMap.keySet()) {
            if (message.startsWith(s)) {
                if (currentModule != null) {
                    recordFinishedModule(null);
                }
                //module started
                currentModule = startEndStringMap.get(s);
                startTime = parseDateFromString(date);
                modelYear = message.substring(s.length()).trim();
                break;
            }
        }
    }

    private void moduleEndChecker(String message, String date) {
        if (message.substring(currentModule.length()).trim().equals(endMessagePart)) {
            recordFinishedModule(parseDateFromString(date));
        }
    }

    private void recordFinishedModule(Calendar endTime) {
        String outString = modelYear + "," +
                           currentModule.toUpperCase() + "," +
                           startTime.getTime() + ",";
        if (endTime == null) {
            outString += "unfinished,NA";
        } else {
            outString += endTime.getTime() + "," +
                         getTimeDifference(startTime,endTime);
        }
        outFile.addLine(outString);
        currentModule = null;
        modelYear = null;
        startTime = null;
    }

    /**
     * This method gets the time object for a log4j string entry with date formatted as:
     *  dd-MMM-yyyy HH:mm:ss:SSS
     *
     * @param date
     *        The date string from the log4j message.
     *
     * @return the time the log4j line was recorded.
     */
    private Calendar parseDateFromString(String date) {
        //Date format is: dd-MMM-YY HH:mm
        int day = Integer.valueOf(date.substring(0,2));
        int month = stringToIntMonth.get(date.substring(3,6).toLowerCase());
        int year = Integer.valueOf(date.substring(7,9));
        int hour = Integer.valueOf(date.substring(10,12));
        int min = Integer.valueOf(date.substring(13,15));
        return new GregorianCalendar(year,month,day,hour,min);
        //Date format is: dd-MMM-yyyy HH:mm:ss:SSS
//        int day = Integer.valueOf(date.substring(0,2));
//        int month = stringToIntMonth.get(date.substring(3,6).toLowerCase());
//        int year = Integer.valueOf(date.substring(7,11));
//        int hour = Integer.valueOf(date.substring(12,14));
//        int min = Integer.valueOf(date.substring(15,17));
//        int sec = Integer.valueOf(date.substring(18,20));
//        return new GregorianCalendar(year,month,day,hour,min,sec);
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
        long diff = time2.getTime().getTime() - time1.getTime().getTime();
        if (diff < 0)
            throw new IllegalArgumentException("Error: time2 must be greater than time1 when calculating differences!");
        long days = diff / millisecondsInADay;
        long hours = (diff - days* millisecondsInADay) / millisecondsInAnHour;
        long minutes = (diff - days* millisecondsInADay - hours* millisecondsInAnHour) / millisecondsInAMinute;
        long seconds = (diff - days* millisecondsInADay - hours* millisecondsInAnHour - minutes* millisecondsInAMinute) / millisecondsInASecond;
        //ignore days - we shouldn't have anything that runs that long
        return String.format("%02d:%02d:%02d",hours,minutes,seconds);
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

    private static Map<String,Integer> getStringToIntMonthMap() {
        Map<String,Integer> stringToIntMonth = new HashMap<String,Integer>();
        stringToIntMonth.put("jan",0);
        stringToIntMonth.put("feb",1);
        stringToIntMonth.put("mar",2);
        stringToIntMonth.put("apr",3);
        stringToIntMonth.put("may",4);
        stringToIntMonth.put("jun",5);
        stringToIntMonth.put("jul",6);
        stringToIntMonth.put("aug",7);
        stringToIntMonth.put("sep",8);
        stringToIntMonth.put("oct",9);
        stringToIntMonth.put("nov",10);
        stringToIntMonth.put("dec",11);
        return stringToIntMonth;
    }

    private static Map<String,String> getStartEndStringMap() {
        Map<String,String> startEndStringMap = new HashMap<String,String>();
        startEndStringMap.put("AO will now start ED for simulation year","ED");
        startEndStringMap.put("AO will now start ALD for simulation year","ALD");
        startEndStringMap.put("AO will now start SPG1 for simulation year","SPG1");
        startEndStringMap.put("AO will now start PI for simulation year","PI");
        startEndStringMap.put("AO will now start PIDAF for simulation year","PIDAF");
        startEndStringMap.put("AO will now start SPG2 for simulation year","SPG2");
        startEndStringMap.put("AO will now start PTDAF for simulation year","PTDAF");
        startEndStringMap.put("AO will now start CT for simulation year","CT");
        startEndStringMap.put("AO will now start ET for simulation year","ET");
        startEndStringMap.put("AO will now start TS for simulation year","TS");
        startEndStringMap.put("AO will now start TS DAF for simulation year","TSDAF");
        return startEndStringMap;
    }

    public static void main(String[] args) {
       readAndReportLogs("c:\\transfers\\logtest","main_event.log", "c:\\transfers\\logtest\\checklog_TLUMIP.csv");
    }
}
