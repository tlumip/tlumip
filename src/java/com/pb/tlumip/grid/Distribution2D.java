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
package com.pb.tlumip.grid;
// Distribution2D is a small class that reads a histogram (statistical distri-
// bution) defined in comma-separated value file into a cumulative distribution.
// This distribution can be repeatedly sampled with externally supplied random
// numbers. The input file requires two or more records on which are defined the
// lower bounds, the upper bounds, and the probability associated with that
// interval. If the sum of the probabilities exceeds unity the distribution is
// normalized. The distribution is set up in the constructor, and only one
// method (drawSample) is provided.
// @author "Rick Donnelly <rdonnelly@pbtfsc.com>"
// @version "0.9, 11/02/01"
// #---project "TLUMIP2 CT component"

import java.util.StringTokenizer;
import java.io.*;

public class Distribution2D {
   String label, source;
   double[] lowerBound, upperBound, prob, cumulativeProb;
   int intervals;


   Distribution2D (String title, String filename) {
      // Set properties
      label = title;
      source = filename;
      intervals = 0;

      // Decode the input data file
      try {
         BufferedReader br = new BufferedReader(new FileReader(filename));
         StringTokenizer st;
         String s, cs = "";
         double totalProb = 0.0;

         // Since we don't know a priori how many intervals are in the data file we
         // will simply build up a string with the data we find on each record,
         // and populate the appropriate data structures in the next step.
         double l, u, p;
         while ((s = br.readLine()) != null) {
            if (s.startsWith("#")) continue;    // comment or header record
            st = new StringTokenizer(s,", ");
            intervals++;
            l = Double.parseDouble(st.nextToken());
            u = Double.parseDouble(st.nextToken());
            p = Double.parseDouble(st.nextToken());
            totalProb += p;
            cs += l+","+u+","+p+",";    // Append triplet to cumulative string
         }
         br.close();

         // Now size the internal data structures accordingly, but not worrying
         // about initialization since that is handled for all members next
         lowerBound = new double[intervals];
         upperBound = new double[intervals];
         prob = new double[intervals];
         cumulativeProb = new double[intervals];

         // Normalize the percentages in each interval, and place the data in
         // appropriate data structures
         double scalingFactor = 1.0/totalProb;
         st = new StringTokenizer(cs,",");
         for (int n=0; n<intervals; n++) {
            lowerBound[n] = Double.parseDouble(st.nextToken());
            upperBound[n] = Double.parseDouble(st.nextToken());
            prob[n] = Double.parseDouble(st.nextToken());
            cumulativeProb[n] = prob[n]*scalingFactor;
            if (n>0) cumulativeProb[n] += cumulativeProb[n-1];
         }

      } catch (IOException e) { e.printStackTrace(); }
   }  // end of Distribution2D()

   public String toString () {
      String s = label+" ("+source+"):\n";
      for (int n=0; n<intervals; n++)
         s += "  ["+n+"] l="+lowerBound[n]+" u="+upperBound[n]+" p="+
            prob[n]+" cp="+cumulativeProb[n]+"\n";
      return s;
   }  // end of toString()


   // drawSample is where all of the work is done in this class. It is designed
   // so that large numbers of samples can be drawn from the distribution. The
   // caller will provide the random number(s) used to guide the draw. The first
   // random number (required) specifies the interval in the cumulative distri-
   // bution that the value will be drawn from. The second (optional) parameter
   // determines where on the range between lower and upper bounds of the
   // selected interval the return value will be interpolated at. If one value
   // is passed to the method it will be used for both purposes.
   public double drawSample (double r) {
      return drawSample(r, r);
   }
   public double drawSample (double r1, double r2) {
      // Determine the target interval we're going to sample from
      int targetInterval = 0;
      for (int n=1; n<intervals; n++) {
         if ((r1>=cumulativeProb[n-1]) & (r1<=cumulativeProb[n])) {
            targetInterval = n;
            break;
         }
      }

      // Return an interpolated value from the target interval
      return lowerBound[targetInterval]+
         ((upperBound[targetInterval]-lowerBound[targetInterval])*r2);
   }


   public static void main (String[] args) {
      double r1, r2;

      Distribution2D d1 = new Distribution2D("first test", "test1.txt");
      System.out.println(d1);
      for (int i=0; i<25; i++) {
         r1 = Math.random();
         System.out.println("  r1="+r1+" ds="+d1.drawSample(r1));
      }

      Distribution2D d2 = new Distribution2D("second test", "test2.txt");
      System.out.println(d2);
      for (int i=0; i<25; i++) {
         r1 = Math.random();
         r2 = Math.random();
         System.out.println("  r1="+r1+" r2="+r2+" ds="+d2.drawSample(r1, r2));
      }
   }

   // test1.txt
   // # lower, upper, value
   //       5,    15,   0.3
   //      16,    20,   0.4
   //      21,    35,   0.3
   // test2.txt
   // # lower, upper, value
   //       0,     2,  14.5
   //       2,     4,  17.8
   //       4,     6,  28.5
   //       6,     7,  59.7
   //       8,     9,  30.0
}
