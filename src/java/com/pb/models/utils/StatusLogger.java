package com.pb.models.utils;

import org.apache.log4j.Logger;

/**
 * <p/>
 *
 * @version 1.0 Nov 16, 2007 - 9:34:32 AM
 * @author: crf
 */
public class StatusLogger {
    private static Logger statusLogger = Logger.getLogger(("status"));

    /**
     * Log a text status message. This consists of a text message, and a title for the status entry. As the logger
     * semicolon delimits its message (for whatever processor may use it), semicolons should not be used in any of the
     * arguments in the message.
     *
     * @param module
     *        The module logging this message.  Should be in the form <code>module.submodule.subsubmodule</code>(etc.).
     *        An example would be <code>pt.ld</code>
     *
     * @param title
     *        The title to use for this status entry.
     *
     * @param text
     *        The status message to log.
     */
    public static void logText(String module, String title,String text) {
        statusLogger.info(module + ".status.text;" + title + ";" + text);
    }

    /**
     * Log a histogram status message.  A histogram message can be looked at as an "amount complete" gauge; it essentially
     * reports how much has been completed, and how much will have been completed when the model/module is finished.  As
     * the logger semicolon delimits its message (for whatever processor may use it), semicolons should not be used in
     * any of the arguments in the message.
     *
     * @param module
     *        The module logging this message.  Should be in the form <code>module.submodule.subsubmodule</code>(etc.).
     *        An example would be <code>pt.ld</code>
     *
     * @param title
     *        The title to use for this status entry.
     *
     * @param goal
     *        The point at which the model/module will be finished.
     *
     * @param currentPoint
     *        The current point.
     *
     * @param xAxisLabel
     *        The label to use for the histogram's x-axis.
     *
     * @param yAxisLabel
     *        The label to use for the histogram's y-axis.
     */
    public static void logHistogram(String module, String title, float goal, float currentPoint, String xAxisLabel, String yAxisLabel) {
        statusLogger.info(module + ".status.histo;" + title + ";" + goal + ";" + currentPoint + ";" + xAxisLabel + ";" + yAxisLabel);
    }

    /**
     * Log a graph status message. This consists primarily of a pair of points (x,y) representing the current status of
     * the model/module.  A collection of these points could be read by the processor and graphed to show both the
     * current status as well as its progression. As the logger semicolon delimits its message (for whatever processor
     * may use it), semicolons should not be used in any of the arguments in the message.
     *
     * @param module
     *        The module logging this message.  Should be in the form <code>module.submodule.subsubmodule</code>(etc.).
     *        An example would be <code>pt.ld</code>
     *
     * @param title
     *        The title to use for this status entry.
     *
     * @param xPoint
     *        The x coordinate of the point to be graphed.
     *
     * @param yPoint
     *        The y coordinate of the point to be graphed.
     *
     * @param xAxisLabel
     *        The label to use for the histogram's x-axis.
     *
     * @param yAxisLabel
     *        The label to use for the histogram's y-axis.
     */
    public static void logGraph(String module, String title, float xPoint, float yPoint, String xAxisLabel, String yAxisLabel) {
        statusLogger.info(module + ".status.graph;" + title + ";" + xPoint + ";" + yPoint + ";" + xAxisLabel + ";" + yAxisLabel);
    }
}
