package com.pb.despair.ao;

/**
 *  Contains the description for the TLUMIP
 * scenario specified in AOProperties.xml
 * 
 * @author  Christi Willison
 * @version Jan 7, 2004
 * Created by IntelliJ IDEA.
 */
public class Scenario {
    private String name;
    private String start;
    private String end;


//------- Setter methods ------
    public void setName(String name) {
        this.name = name;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }



//------- Getter methods -------
    public String getName() {
        return name;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public String toString(){
            StringBuffer buf = new StringBuffer("Scenario: Name='"+name+"'\n");
            buf.append("          Start="+start+"\n");
            buf.append("          End="+end+"\n");
            return buf.toString();
        }

}
