package com.pb.despair.ao;

/**
 * 
 * 
 * @author  Christi Willison
 * @version Jan 7, 2004
 * Created by IntelliJ IDEA.
 */
public class Resource {
    private String type;
    private String name;
    private String ip;
    private String status;

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public String getStatus() {
        return status;
    }

    public String toString(){
        StringBuffer buf = new StringBuffer("Resource: Name='"+name+"'\n");
        buf.append("          Type="+type+"\n");
        buf.append("          IP="+ip+"\n");
        buf.append("          Status="+status+"\n");
        return buf.toString();
    }

}
