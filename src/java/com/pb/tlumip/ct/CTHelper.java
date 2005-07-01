package com.pb.tlumip.ct;

import java.util.Date;

public class CTHelper {
  static Date start;

  // Return a string that reports the amount of time that has elapsed between
  // two times
  public static String elapsedTime(Date start, Date stop) {
    long hours = 0, minutes = 0, seconds = 0, elapsedSeconds, totalSeconds;
    elapsedSeconds = totalSeconds =
      (long)(stop.getTime()-start.getTime())/1000;
    if (elapsedSeconds>=3600) {
      hours = elapsedSeconds/3600;
      elapsedSeconds -= (hours*3600);
    }
    if (elapsedSeconds>=60) {
      minutes = elapsedSeconds/60;
      elapsedSeconds -= (minutes*60);
    }
    return hours+"h "+minutes+"m "+elapsedSeconds+"s ("+totalSeconds+"s)";
  }

  // Return a right-justified fixed-length string for formatted printing
  public static String rightAlign (String s, int len) {
    if (s.length()<len)
      for (int i=s.length(); i<len; i++)
        s = " "+s;
    return s;
  }
  public static String rightAlign (double d, int len) {
    String s = ""+d;
    if (s.length()<len)
      for (int i=s.length(); i<len; i++)
        s = " "+s;
    return s;
  }
  public static String rightAlign (int d, int len) {
    String s = ""+d;
    if (s.length()<len)
      for (int i=s.length(); i<len; i++)
        s = " "+s;
    return s;
  }
    //will be replace by Math.tanh in Java 5.0 but will use these for 1.4.2
    public static double tanh (double x){
        return sinh(x)/cosh(x);
    }

    private static double cosh (double y) {
        return 0.5*(Math.exp(y)+Math.exp(-y));
    }
    private static double sinh (double z) {
        return 0.5*(Math.exp(z)-Math.exp(-z));
    }


  public static void main (String[] args) {
    start = new Date();

    // Throw in a timing loop
    long r;
    for (long i=0; i<487654321; i++)
      r = (long)Math.random()+i;

    System.out.println(elapsedTime(start, new Date()));
  }
}
