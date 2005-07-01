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
