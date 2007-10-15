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

import java.util.ArrayList;

/**
 * 
 * 
 * @author  Christi Willison
 * @version Jan 7, 2004
 * Created by IntelliJ IDEA.
 */
public class IntervalUpdate {
    private String interval;
    private ArrayList fileUpdates;
    private ArrayList policyFileUpdates;

    public IntervalUpdate(){
        fileUpdates = new ArrayList();
        policyFileUpdates = new ArrayList();
    }
    public void setInterval(String interval) {
        this.interval = interval;
    }

    public void setFile(String file) {
        this.fileUpdates.add(file);
    }

    public void setPolicyFile(String policyFile) {
        this.policyFileUpdates.add(policyFile);
    }

    public String toString(){
        StringBuffer buf = new StringBuffer("Interval Update: Interval="+interval+"\n");
        for(int i=0;i<fileUpdates.size();i++)
            buf.append("          File "+(i+1)+"='"+fileUpdates.get(i)+"'\n");
        for(int i=0;i<policyFileUpdates.size();i++)
            buf.append("          Policy File "+(i+1)+"='"+policyFileUpdates.get(i)+"'\n");
        return buf.toString();
    }

}
