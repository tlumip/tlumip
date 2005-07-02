/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.ao;

/**
 * This class defines valid entries for the command file
 * which is used to start and stop daf applications
 *
 * @author Christi Willison
 * @version Jun 7, 2004
 */
public class Entry {

    public static String START_NODE = "StartNode";
    public static String START_CLUSTER = "StartCluster";
    public static String START_APPLICATION = "StartApplication";
    public static String STOP_NODE = "StopNode";

    public static String NODE0 = "node0";
    public static String NODE1 = "node1";
    public static String NODE2 = "node2";
    public static String NODE3 = "node3";
    public static String NODE4 = "node4";
    public static String NODE5 = "node5";
    public static String NODE6 = "node6";
    public static String NODE7 = "node7";

    public static String PIDAF = "pidaf";
    public static String PTDAF = "ptdaf";
}
