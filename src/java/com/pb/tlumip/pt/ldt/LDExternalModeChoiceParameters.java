/*
 * Copyright  2006 PB Consult Inc.
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
package com.pb.tlumip.pt.ldt;

/**
 * Defines column IDs for mode shares for long-distance 
 * tour external mode choice models.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 23, 2006
 *
 */
public class LDExternalModeChoiceParameters {

    public static final int AUTOSHARE         = 0; 
    public static final int AIRSHARE          = 1; 
    public static final int TRANSITDRIVESHARE = 2; 
    public static final int TRANSITWALKSHARE  = 3; 
    public static final int HSRDRIVESHARE     = 4; 
    public static final int HSRWALKSHARE      = 5; 
}
