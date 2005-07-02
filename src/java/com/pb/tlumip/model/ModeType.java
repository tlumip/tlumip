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
package com.pb.tlumip.model;

public final class ModeType {

     public final static byte AUTODRIVER            = 1;
     public final static byte AUTOPASSENGER         = 2;
     public final static byte WALK                  = 3;
     public final static byte BIKE                  = 4;
     public final static byte WALKTRANSIT           = 5;
     public final static byte TRANSITPASSENGER      = 6;     
     public final static byte PASSENGERTRANSIT      = 7;
     public final static byte DRIVETRANSIT          = 8;
     
    public final static int DRIVEALONE              = 1;
    public final static int SHAREDRIDE2             = 2;
    public final static int SHAREDRIDE3PLUS         = 3;
    public final static int WALKTRIP                = 4;
    public final static int BIKETRIP                = 5;
    public static final int WALKTRANSITTRIP         = 6;
    public static final int DRIVETRANSITTRIP        = 7;

    
}