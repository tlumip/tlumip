package com.pb.despair.ct;

import java.util.*;
import java.io.*;

class ConvertWeeklyDemand {

   public static void main (String[] args) {
     try {
       String f = "/temp/FD/t9WeeklyDemand.binary";
       DataInputStream di = new DataInputStream(new FileInputStream(f));
       int commodity, origin, destination, intWeeklyTons;
       double weeklyTons;
       boolean eof = false;
       try {
         while (!eof) {
           commodity = di.readInt();
           origin = di.readInt();
           destination = di.readInt();
           weeklyTons = di.readDouble();
           System.out.println(commodity+","+origin+","+destination+","+weeklyTons);
         }
       } catch (EOFException e) { eof = true; }

       di.close();
     } catch (IOException e) { e.printStackTrace(); }

   }

}
