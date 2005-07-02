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
package com.pb.tlumip.ct;

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
