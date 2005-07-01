package com.pb.tlumip.pt;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Author: willison
 * Date: Sep 16, 2004
 * <p/>
 * Created by IntelliJ IDEA.
 */
public class PTSummarizer {

    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.PTSummarizer");

    static int[][] purposeMode = new int[6][8]; //table of freq. of tours by purpose/mode
    static int purposeSum = 0;
    static float[][] purposeModeDist = new float[6][8]; //table of dist of tours by purpose/mode

    static int[][] wSegMode = new int[9][8];
    static int workSum = 0;
    static float[][] wSegModeDist = new float[9][8]; //will be used to store total dist and then to hold average dist

    static int[][][] nwSegMode = new int[5][9][8];
    static int[] nwSum = new int[5];
    static float[][][] nwSegModeDist = new float[5][9][8];  //will be used to store total dist and then to hold average dist

    static int[][] wTrips = new int[9][11];
    static int wTripSum = 0;
    static int[][][] nwTrips = new int[5][9][11];
    static int[] nwTripSum = new int[5];

    static int totalTours=0;

    public PTSummarizer(){}

    /* This method will summarize the Households by
    *  1.  income range (0-15,15-30,30+)
    *  2.  household size (1,2,3,4,5+)
    *  3.  workers per household (0,1,2,3,4+)
    *  4.  auto ownership (0,1,2,3+)
    *  5.  work market segment (0-8)
    *  6.  non-work market segment (0-8)
    *  This method should be called after the Households
    *  are read in from the SynPopH file (done in PTDafMaster)
    */
    public static void summarizeHouseholds(PTHousehold[] hhs, String outputFile){

        int[] income = new int[3];
        int incomeSum = 0;
        int[] size = new int[5];
        int sizeSum = 0;
        int[] workers = new int[5];
        int workersSum = 0;
        int[] autos = new int[4];
        int autosSum = 0;
        int[] workSegment = new int[9];
        int workSegmentSum = 0;
        int[] nonWorkSegment = new int[9];
        int nonWorkSegmentSum = 0;        //All 'sums' should add up to the total number of households

        for(int h=0; h< hhs.length; h++){

            //First look at hh.income
            if(hhs[h].income < 15000)   {
                income[0]++;
                incomeSum++;
            }
            else if(hhs[h].income < 30000) {
                income[1]++;
                incomeSum++;
            }
            else {
                income[2]++;
                incomeSum++;
            }

            //Next look at size
            if(hhs[h].size == 1) {
                size[0]++;
                sizeSum++;
            }
            else if(hhs[h].size == 2) {
                size[1]++;
                sizeSum++;
            }
            else if(hhs[h].size == 3) {
                size[2]++;
                sizeSum++;
            }
            else if(hhs[h].size == 4) {
                size[3]++;
                sizeSum++;
            }
            else {
                size[4]++;
                sizeSum++;
            }

            //Next look at workers
            if(hhs[h].workers == 0) {
                workers[0]++;
                workersSum++;
            }
            else if(hhs[h].workers == 1) {
                workers[1]++;
                workersSum++;
            }
            else if(hhs[h].workers == 2) {
                workers[2]++;
                workersSum++;
            }
            else if(hhs[h].workers == 3) {
                workers[3]++;
                workersSum++;
            }
            else {
                workers[4]++;
                workersSum++;
            }

            //Next look at autos
            if(hhs[h].autos == 0) {
                autos[0]++;
                autosSum++;
            }
            else if(hhs[h].autos == 1) {
                autos[1]++;
                autosSum++;
            }
            else if(hhs[h].autos == 2) {
                autos[2]++;
                autosSum++;
            }
            else {
                autos[3]++;
                autosSum++;
            }

            //Next look at work segment
            int workLogsumSegment = hhs[h].calcWorkLogsumSegment();
            workSegment[workLogsumSegment]++;
            workSegmentSum++;

            //Finally look at non-work segment
            int nonWorkLogsumSegment = hhs[h].calcNonWorkLogsumSegment();
            nonWorkSegment[nonWorkLogsumSegment]++;
            nonWorkSegmentSum++;

        } //end of for loop through households.

        //Add up the numbers in the arrays to be sure that they all total the number of households
        //and form the output strings.
        String[] incomeString = {"0 - 15","15 - 30","30+"};
        String[] autoString = {"0","1","2","3+"};
        String[] workersString = {"0","1","2","3","4+"};
        String[] sizeString = {"1","2","3","4","5+"};
        String[] workSegmentString = {"0","1","2","3","4","5","6","7","8"};
        String[] nonWorkSegmentString = {"0","1","2","3","4","5","6","7","8"};

        for(int i=0;i<income.length;i++){
            incomeString[i]+=",";
            incomeString[i]+=income[i];
            incomeString[i]+=",";
            incomeString[i]+=((float)income[i]/incomeSum)*100;
        }
        for(int i=0;i<autos.length;i++){
            autoString[i]+=",";
            autoString[i]+=autos[i];
            autoString[i]+=",";
            autoString[i]+=((float)autos[i]/autosSum)*100;
        }
        for(int i=0;i<size.length;i++){
            workersString[i]+=",";
            workersString[i]+=workers[i];
            workersString[i]+=",";
            workersString[i]+=((float)workers[i]/workersSum)*100;

            sizeString[i]+=",";
            sizeString[i]+=size[i];
            sizeString[i]+=",";
            sizeString[i]+=((float)size[i]/sizeSum)*100;
        }
        for(int i=0;i<workSegment.length;i++){
            workSegmentString[i]+=",";
            workSegmentString[i]+=workSegment[i];
            workSegmentString[i]+=",";
            workSegmentString[i]+=((float)workSegment[i]/workSegmentSum)*100;

            nonWorkSegmentString[i]+=",";
            nonWorkSegmentString[i]+=nonWorkSegment[i];
            nonWorkSegmentString[i]+=",";
            nonWorkSegmentString[i]+=((float)nonWorkSegment[i]/nonWorkSegmentSum)*100;
        }

        //Now we want to write out these results to a CSV file
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(outputFile));

            pw.println("Summary of Household Information - Total Households in SynPopH: " + hhs.length);

            pw.println();
            pw.println();

            //First write out HHs by Income Range table
            pw.println("Households by Income Range - Total Households in table: " + incomeSum);
            pw.println("range,nHHs,percent");
            for (int i=0;i<incomeString.length;i++) {
                pw.println(incomeString[i]);
            }

            pw.println();
            pw.println();

            //Now write out HHs by Size
            pw.println("Households by Size - Total Households in table: " + sizeSum);
            pw.println("size,nHHs,percent");
            for (int i=0;i<sizeString.length;i++) {
                pw.println(sizeString[i]);
            }

            pw.println();
            pw.println();

            //Now write out HHs by Workers
            pw.println("Households by Workers - Total Households in table: " + workersSum);
            pw.println("workers,nHHs,percent");
            for (int i=0;i<workersString.length;i++) {
                pw.println(workersString[i]);
            }

            pw.println();
            pw.println();

            //Now write out HHs by Auto Ownership
            pw.println("Households by Auto Ownership - Total Households in table: "+ autosSum);
            pw.println("autos,nHHs,percent");
            for (int i=0;i<autoString.length;i++) {
                pw.println(autoString[i]);
            }

            pw.println();
            pw.println();

            //Now write out HHs by Work Market Segment
            pw.println("Households by Work Market Segment - Total Households in table: " + workSegmentSum);
            pw.println("work segment,nHHs,percent");
            for (int i=0;i<workSegmentString.length;i++) {
                pw.println(workSegmentString[i]);
            }

            pw.println();
            pw.println();

            //Now write out HHs by Non-Work Market Segment
            pw.println("Households by Non-Work Market Segment - Total Households in table: " + nonWorkSegmentSum);
            pw.println("non-work segment,nHHs,percent");
            for (int i=0;i<nonWorkSegmentString.length;i++) {
                pw.println(nonWorkSegmentString[i]);
            }

            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    } //end of summarizeHouseholds

    /* This method will count the number of hhs in each work/non-work segment group.
    *  The values will be used to determine the number of blocks per worker when
    *  it comes time to send the hhs off to the workers.
    */
    public static int[][] summarizeHHsByWorkAndNonWorkSegments(PTHousehold[] hhs){
        int[][] workByNonWork = new int[9][9]; //nWorkSegs by nNonWorkSegs
        for(int h=0; h< hhs.length; h++){
            workByNonWork[hhs[h].calcWorkLogsumSegment()][hhs[h].calcNonWorkLogsumSegment()]++;
        }
        return workByNonWork;
    }

    /* This method will summarize the Persons by
    *  1.  age range (0-4,5-18,19-39,40-65,66-80,80+)
    *  2.  gender  (m,f)
    *  3.  workers by age and gender (see categories above)
    *  4.  non-workers by age and gender (see categories above)
    *  5.  persons by occupation (0,1,2,3,4,5,6,7,8)
    *  6.  workers by occupation (0,1,2,3,4,5,6,7,8)  //first element of array should be 0
    *  7.  persons by industry (0-26) //industry is more specific than occupation, see com.pb.tlumip.spg.EdIndustry.java
    *  8.  workers by industry (0-26) //for list of industries
    *  9.  students by age (<=18, 19+)
    * This method will be called after the SynPopP file
    * has been read in (by PTDafMaster)
    */
    public static void summarizePersons(PTPerson[] ps, String outputFile){
        int[][] ageGender = new int[2][6]; // ageGender[0][] = females, ageGender[1] = males
        int[] agesSum = new int[6];       //total persons per age category
        int[] genderSum = new int[2];
        int[][] wAgeGender = new int[2][6];
        int[] wAgeSum = new int[6];  //gives total number of employed persons by age category
        int[] wGenderSum = new int[2]; //give total number of workers by gender
        int workerSum = 0; //gives total workers
        int[][] nwAgeGender = new int[2][6];
        int[] nwAgeSum = new int[6];   //gives total number of unemployed/non-working persons  by age category
        int[] nwGenderSum = new int[2]; //gives total number of unemployed persons by gender
        int nonWorkerSum = 0;       //gives total non-workers (workerSum + nonWorkerSum = ps.length)
        int[] pOccupation = new int[9];  // total population's occupation code (unemployed persons sometimes have
                                        //an occupation code that is not 0 (unemployed). codes are 0-8
        int pOccupationSum = 0;    //should add up to total persons
        int[] wOccupation = new int[9]; //occupation of the employed persons (PTPerson.employed == true)
        int wOccupationSum = 0;   //should add up to number of employed persons.
        int[] pIndustry = new int[27];
        int pIndustrySum = 0; //should add up to total persons.
        int[] wIndustry = new int[27];
        int wIndustrySum = 0; //should add up to number of employed persons
        int[] studentAge = new int[2];
        int[] student = new int[2];
        int studentAgeSum = 0;
        int studentSum = 0;

        //First look at ages
        for(int p=0; p< ps.length; p++){
            //First look at age and gender of entire population, and by employment status
            if (ps[p].female) {
                genderSum[0]++;
                if(ps[p].age < 5)  {
                    ageGender[0][0]++;
                    agesSum[0]++;
                    if(ps[p].employed) {
                        wAgeGender[0][0]++;
                        wAgeSum[0]++;
                    }
                    else {
                        nwAgeGender[0][0]++;
                        nwAgeSum[0]++;
                    }

                }
                else if(ps[p].age < 19) {
                    ageGender[0][1]++;
                    agesSum[1]++;
                    if(ps[p].employed) {
                        wAgeGender[0][1]++;
                        wAgeSum[1]++;
                    }
                    else {
                        nwAgeGender[0][1]++;
                        nwAgeSum[1]++;
                    }
                }
                else if(ps[p].age < 40) {
                    ageGender[0][2]++;
                    agesSum[2]++;
                    if(ps[p].employed) {
                        wAgeGender[0][2]++;
                        wAgeSum[2]++;
                    }
                    else {
                        nwAgeGender[0][2]++;
                        nwAgeSum[2]++;
                    }
                }
                else if(ps[p].age < 66) {
                    ageGender[0][3]++;
                    agesSum[3]++;
                    if(ps[p].employed) {
                        wAgeGender[0][3]++;
                        wAgeSum[3]++;
                    }
                    else {
                        nwAgeGender[0][3]++;
                        nwAgeSum[3]++;
                    }
                }
                else if(ps[p].age < 80) {
                    ageGender[0][4]++;
                    agesSum[4]++;
                    if(ps[p].employed) {
                        wAgeGender[0][4]++;
                        wAgeSum[4]++;
                    }
                    else {
                        nwAgeGender[0][4]++;
                        nwAgeSum[4]++;
                    }
                }
                else {
                    ageGender[0][5]++;
                    agesSum[5]++;
                    if(ps[p].employed) {
                        wAgeGender[0][5]++;
                        wAgeSum[5]++;
                    }
                    else {
                        nwAgeGender[0][5]++;
                        nwAgeSum[5]++;
                    }
                }
            }else {
                genderSum[1]++;
                if(ps[p].age < 5) {
                    ageGender[1][0]++;
                    agesSum[0]++;
                    if(ps[p].employed) {
                        wAgeGender[1][0]++;
                        wAgeSum[0]++;
                    }
                    else {
                        nwAgeGender[1][0]++;
                        nwAgeSum[0]++;
                    }
                }
                else if(ps[p].age < 19) {
                    ageGender[1][1]++;
                    agesSum[1]++;
                    if(ps[p].employed) {
                        wAgeGender[1][1]++;
                        wAgeSum[1]++;
                    }
                    else {
                        nwAgeGender[1][1]++;
                        nwAgeSum[1]++;
                    }
                }
                else if(ps[p].age < 40) {
                    ageGender[1][2]++;
                    agesSum[2]++;
                    if(ps[p].employed) {
                        wAgeGender[1][2]++;
                        wAgeSum[2]++;
                    }
                    else {
                        nwAgeGender[1][2]++;
                        nwAgeSum[2]++;
                    }
                }
                else if(ps[p].age < 66) {
                    ageGender[1][3]++;
                    agesSum[3]++;
                    if(ps[p].employed) {
                        wAgeGender[1][3]++;
                        wAgeSum[3]++;
                    }
                    else {
                        nwAgeGender[1][3]++;
                        nwAgeSum[3]++;
                    }
                }
                else if(ps[p].age < 80) {
                    ageGender[1][4]++;
                    agesSum[4]++;
                    if(ps[p].employed) {
                        wAgeGender[1][4]++;
                        wAgeSum[4]++;
                    }
                    else {
                        nwAgeGender[1][4]++;
                        nwAgeSum[4]++;
                    }
                }
                else {
                    ageGender[1][5]++;
                    agesSum[5]++;
                    if(ps[p].employed) {
                        wAgeGender[1][5]++;
                        wAgeSum[5]++;
                    }
                    else {
                        nwAgeGender[1][5]++;
                        nwAgeSum[5]++;
                    }
                }
            }


            //Next look at Persons by Occupation
            pOccupation[ps[p].occupation]++;    //codes are 0-8
            pOccupationSum++;

            //Next look at Employed Persons by Occupation
            if(ps[p].employed) {
                workerSum++;
                wOccupation[ps[p].occupation]++;
                wOccupationSum++;
                if(ps[p].female)wGenderSum[0]++;
                else wGenderSum[1]++;
            }else {
                nonWorkerSum++;
                if(ps[p].female) nwGenderSum[0]++;
                else nwGenderSum[1]++;
            }

            //Next look at Persons by Industry
            pIndustry[ps[p].industry]++;     //codes are 0-26 (26 is unemployed)
            pIndustrySum++;

            //Next look at Employed Persons by industry
            if(ps[p].employed) {
                wIndustry[ps[p].industry]++;
                wIndustrySum++;
            }

            //next look at age of students
            if(ps[p].student){
                student[0]++; //yes, I'm a student
                studentSum++;
                if(ps[p].age <= 18) studentAge[0]++;
                else studentAge[1]++;
            }else student[1]++; //no, not a student
        } //end of for loop

        //Now sum up arrays and form print strings
        String[] ageString = {"0 to 4","5 to 18","19 to 39","40 to 65","66 to 80","80+"};
        String[] wAgeString = {"0 to 4","5 to 18","19 to 39","40 to 65","66 to 80","80+"};
        String[] nwAgeString = {"0 to 4","5 to 18","19 to 39","40 to 65","66 to 80","80+"};
        String[] pOccupationString = {"Unemployed","1_ManPro","1a_Health","2_PstSec"
                                      ,"3_OthTchr","4_OthP&T","5_RetSls","6_OthR&C","7_NonOfc"};
        String[] wOccupationString = {"Unemployed","1_ManPro","1a_Health","2_PstSec"
                                      ,"3_OthTchr","4_OthP&T","5_RetSls","6_OthR&C","7_NonOfc"};
        String[] pIndustryString = {"ACCOMMODATIONS","AGRICULTURE AND MINING","COMMUNICATIONS AND UTILITIES","CONSTRUCTION",
                    "ELECTRONICS AND INSTRUMENTS-Light Industry","FIRE BUSINESS AND PROFESSIONAL SERVICES",
                    "FOOD PRODUCTS-Heavy Industry","FOOD PRODUCTS-Light Industry","FORESTRY AND LOGGING",
                    "GOVERNMENT ADMINISTRATION","HEALTH SERVICES-Hospital","HEALTH SERVICES-Institutional",
                    "HEALTH SERVICES-Office","HIGHER EDUCATION","HOMEBASED SERVICES","LOWER EDUCATION",
                    "LUMBER AND WOOD PRODUCTS-Heavy Industry","OTHER DURABLES-Heavy Industry",
                    "OTHER DURABLES-Light Industry","OTHER NON-DURABLES-Heavy Industry","OTHER NON-DURABLES-Light Industry",
                    "PERSONAL AND OTHER SERVICES AND AMUSEMENTS","PULP AND PAPER-Heavy Industry","RETAIL TRADE","TRANSPORT",
                    "WHOLESALE TRADE","UNEMPLOYED"};

        String[] wIndustryString = {"ACCOMMODATIONS","AGRICULTURE AND MINING","COMMUNICATIONS AND UTILITIES","CONSTRUCTION",
                    "ELECTRONICS AND INSTRUMENTS-Light Industry","FIRE BUSINESS AND PROFESSIONAL SERVICES",
                    "FOOD PRODUCTS-Heavy Industry","FOOD PRODUCTS-Light Industry","FORESTRY AND LOGGING",
                    "GOVERNMENT ADMINISTRATION","HEALTH SERVICES-Hospital","HEALTH SERVICES-Institutional",
                    "HEALTH SERVICES-Office","HIGHER EDUCATION","HOMEBASED SERVICES","LOWER EDUCATION",
                    "LUMBER AND WOOD PRODUCTS-Heavy Industry","OTHER DURABLES-Heavy Industry",
                    "OTHER DURABLES-Light Industry","OTHER NON-DURABLES-Heavy Industry","OTHER NON-DURABLES-Light Industry",
                    "PERSONAL AND OTHER SERVICES AND AMUSEMENTS","PULP AND PAPER-Heavy Industry","RETAIL TRADE","TRANSPORT",
                    "WHOLESALE TRADE","UNEMPLOYED"};

        for(int i=0; i< ageGender[0].length; i++){
            for(int j=0; j<ageGender.length; j++){
                ageString[i]+=",";
                wAgeString[i]+=",";
                nwAgeString[i]+=",";
                ageString[i]+=ageGender[j][i];
                wAgeString[i]+=wAgeGender[j][i];
                nwAgeString[i]+=nwAgeGender[j][i];
            }
            ageString[i]+=",";
            wAgeString[i]+=",";
            nwAgeString[i]+=",";
            ageString[i]+=((float)ageGender[1][i]/agesSum[i])*100; // just show percent male.
            wAgeString[i]+=((float)wAgeGender[1][i]/wAgeSum[i])*100;
            nwAgeString[i]+=((float)nwAgeGender[1][i]/nwAgeSum[i])*100;
        }
        for(int i=0; i< pOccupation.length; i++){
            pOccupationString[i]+=",";
            pOccupationString[i]+=pOccupation[i];
            pOccupationString[i]+=",";
            pOccupationString[i]+=((float)pOccupation[i]/pOccupationSum)*100;
        }
        for(int i=0; i< wOccupation.length; i++){
            wOccupationString[i]+=",";
            wOccupationString[i]+=wOccupation[i];
            wOccupationString[i]+=",";
            wOccupationString[i]+=((float)wOccupation[i]/wOccupationSum)*100;
        }
        for(int i=0; i< pIndustry.length; i++){
            pIndustryString[i]+=",";
            pIndustryString[i]+=pIndustry[i];
            pIndustryString[i]+=",";
            pIndustryString[i]+=((float)pIndustry[i]/pIndustrySum)*100;
        }
        for(int i=0; i< wIndustry.length; i++){
            wIndustryString[i]+=",";
            wIndustryString[i]+=wIndustry[i];
            wIndustryString[i]+=",";
            wIndustryString[i]+=((float)wIndustry[i]/wIndustrySum)*100;
        }

        studentAgeSum = studentAge[0] + studentAge[1];
        studentSum = student[0]+student[1];

        String[] studentAgeString ={"18 years or less," + studentAge[0] + "," + ((float)studentAge[0]/studentAgeSum)*100,
                                    "19 plus,"+ studentAge[1]+","+((float)studentAge[1]/studentAgeSum)*100};
        String[] studentString = {"student,"+student[0]+","+ ((float)student[0]/studentSum)*100,
                                  "non-student,"+student[1]+","+((float)student[1]/studentSum)*100};


        //Now write out the results to a file
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(outputFile));
            pw.println("Summary of Person Information - Total Persons in SynPopP: " + ps.length);

            pw.println();
            pw.println();

            //First write out total persons by age and gender
            pw.println("Persons by age and gender");
            pw.println(",gender");
            pw.println("age,female,male,percent male");
            for (int i=0;i<ageString.length;i++) {
                pw.println(ageString[i]);
            }
            pw.println("totals,"+genderSum[0]+","+genderSum[1]+","+(((float)genderSum[1])/ps.length)*100);


            pw.println();
            pw.println();

            //Next write out total employed persons by age and gender
            pw.println("Workers by age and gender - Total number of workers in table: " + workerSum);
            pw.println(",gender");
            pw.println("age,female,male,percent male");
            for (int i=0;i<wAgeString.length;i++) {
                pw.println(wAgeString[i]);
            }
            pw.println("totals,"+wGenderSum[0]+","+wGenderSum[1]+","+(((float)wGenderSum[1])/workerSum)*100);

            pw.println();
            pw.println();

            //Next write out non-working persons by age and gender
            pw.println("Non-workers by age and gender - Total number of non-workers in table: " + nonWorkerSum);
            pw.println(",gender");
            pw.println("age,female,male,percent male");
            for (int i=0;i<nwAgeString.length;i++) {
                pw.println(nwAgeString[i]);
            }
            pw.println("totals,"+nwGenderSum[0]+","+nwGenderSum[1]+","+(((float)nwGenderSum[1])/nonWorkerSum)*100);

            pw.println();
            pw.println();

            //Next write out the persons by Occupation
            pw.println("Persons by Occupation - Total number of persons in table: " + pOccupationSum );
            pw.println("occupation,nPersons,percent");
            for (int i=0;i<pOccupationString.length;i++) {
                pw.println(pOccupationString[i]);
            }

            pw.println();
            pw.println();

            //Next write out the persons by Occupation
            pw.println("Workers by Occupation - Total number of workers in table: " + wOccupationSum );
            pw.println("occupation,nWorkers,percent");
            for (int i=0;i<wOccupationString.length;i++) {
                pw.println(wOccupationString[i]);
            }

            pw.println();
            pw.println();

            //Next write out the persons by Industry
            pw.println("Persons by Industry - Total number of persons in table: " + pIndustrySum );
            pw.println("industry,nPersons,percent");
            for (int i=0;i<pIndustryString.length;i++) {
                pw.println(pIndustryString[i]);
            }

            pw.println();
            pw.println();

            //Next write out the persons by Occupation
            pw.println("Workers by Industry - Total number of workers in table: " + wIndustrySum );
            pw.println("industry,nWorkers,percent");
            for (int i=0;i<wIndustryString.length;i++) {
                pw.println(wIndustryString[i]);
            }

            pw.println();
            pw.println();

            //Finally write out students by age
            pw.println("Students by age - Total number of students: " + studentAgeSum);
            pw.println("age,nStudents,percent");
            for (int i=0;i<studentAgeString.length;i++) {
                pw.println(studentAgeString[i]);
            }

            pw.println();
            pw.println();

            pw.println("Persons by student category - Total persons in table: " + studentSum);
            pw.println("student,non-student,percent");
            for (int i=0;i<studentString.length;i++) {
                pw.println(studentString[i]);
            }

            pw.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    } //end of summarizePersons

    /**
     * This method will summarize the Tours as follows:
     *  1.  tours by activity purpose and mode ("w,b,c,o,r,s" : "driver,passenger,walk,bike,wtransit,transitp,ptransti,dtransit")
     *  2.  average tour distance by purpose and mode (purposes are numbered 1-6  : modes are numbered 1-8
     *  3.  work tours by work market segment and mode ("0-8" : see modes above)
     *  4.  non-work tours by purpose, non-work segment and mode ("b,c,o,r,s" : "0-8" : see modes above)
     *  5.  trips on work tours by work market segment and mode ("0-8" : above modes + drive,shared2,shared3+)
     *  6.  trips on non-work tours by purpose, non-work segments and mode ("0-8" : modes are numbered 1-3)
     *  7.  average tour distance for work tours by work market segment and mode
     *  8.  average tour distance for non-work tours by purpose, non-work segment and mode
     * This method will be called throughout the processing of households as workers
     * complete the PTModels on a household block. PTModel.summarizeTours method will call.
     */
    public static void summarizeTours(PTHousehold[] hhs) { //subset of all the households

        //Go thru each household, then each person in the household, then each tour
        //in the household.  Get the hh's work and non-work segment, the tour's primary destination activityPurpose,
        //primaryMode.type, and departDist.  If the tour has intermediateStop1 or intermediateStop2 get the
        //stop's tripMode and distanceToActivity.
        int wSegment,
            nwSegment,
            purpose,
            primaryMode,
            trip1Mode=-99,
            trip2Mode=-99;
        float departDist;


        for (int h=0;h< hhs.length;h++){
            wSegment = hhs[h].calcWorkLogsumSegment();
            nwSegment = hhs[h].calcNonWorkLogsumSegment();
            for(int p=0;p<hhs[h].persons.length;p++){
                for(int t=0;t<hhs[h].persons[p].weekdayTours.length;t++){
                    totalTours++;
                    purpose = hhs[h].persons[p].weekdayTours[t].primaryDestination.activityPurpose;
                    primaryMode =  (int) hhs[h].persons[p].weekdayTours[t].primaryMode.type;
                    departDist = hhs[h].persons[p].weekdayTours[t].departDist;
                    if(hhs[h].persons[p].weekdayTours[t].intermediateStop1 != null){
                        if(primaryMode == 1 || primaryMode == 2){
                            trip1Mode = hhs[h].persons[p].weekdayTours[t].intermediateStop1.tripMode.type;
                        }else trip1Mode = primaryMode;
                    }
                    if(hhs[h].persons[p].weekdayTours[t].intermediateStop2 != null){
                        if(primaryMode == 1 || primaryMode == 2){
                            trip2Mode = hhs[h].persons[p].weekdayTours[t].intermediateStop2.tripMode.type;
                        }else trip2Mode = primaryMode;
                    }

                    purposeMode[purpose-1][primaryMode-1]++;
                    purposeModeDist[purpose-1][primaryMode-1]+=departDist;

                    //use segment, purpose, primaryMode and departDist to increment all arrays
                    if(purpose == 1) { //work tour
                        wSegMode[wSegment][primaryMode-1]++;
                        wSegModeDist[wSegment][primaryMode-1]+=departDist;
                        wTrips[wSegment][primaryMode-1]+=2;
                        if(hhs[h].persons[p].weekdayTours[t].intermediateStop1 != null){
                              wTrips[wSegment][primaryMode-1]+=1;
//                            if(primaryMode == 1 || primaryMode == 2){
//                                 wTrips[wSegment][trip1Mode+7]++; //tour modes are in positions 0-7, so trip modes are 8,9,10
//                            }else wTrips[wSegment][trip1Mode-1]++;
                        }
                        if(hhs[h].persons[p].weekdayTours[t].intermediateStop2 != null){
                            wTrips[wSegment][primaryMode-1]+=1;
//                            if(primaryMode == 1 || primaryMode == 2){
//                                 wTrips[wSegment][trip2Mode+7]++; //tour modes are in positions 0-7, so trip modes are 8,9,10
//                            }else wTrips[wSegment][trip2Mode-1]++;
                        }
                    }else{ //non-work tour
                        nwSegMode[purpose-2][nwSegment][primaryMode-1]++;
                        nwSegModeDist[purpose-2][nwSegment][primaryMode-1]+=departDist;
                        nwTrips[purpose-2][nwSegment][primaryMode-1]+=2;
                        if(hhs[h].persons[p].weekdayTours[t].intermediateStop1 != null){
                              nwTrips[purpose-2][nwSegment][primaryMode-1]+=1;
//                            if(primaryMode == 1 || primaryMode == 2){
//                                 nwTrips[purpose-2][nwSegment][trip1Mode+7]++; //tour modes are in positions 0-7, so auto trip modes are 8,9,10
//                            }else nwTrips[purpose-2][nwSegment][trip1Mode-1]++;  //if not an auto trip than use 0-8 positioning
                        }
                        if(hhs[h].persons[p].weekdayTours[t].intermediateStop2 != null){
                              nwTrips[purpose-2][nwSegment][primaryMode-1]+=1;
//                            if(primaryMode == 1 || primaryMode == 2){
//                                 nwTrips[purpose-2][nwSegment][trip2Mode+7]++; //tour modes are in positions 0-7, so trip modes are 8,9,10
//                            }else nwTrips[purpose-2][nwSegment][trip2Mode-1]++; //if not an auto trip than use 0-8 positioning
                        }
                    } //end of counting work, non-work tour info
                }// next tour
            } //next person
        } //next household
    }

    public static void writeTourSummaryToFile(String outputFile){

        //All tours and trips have been counted.  We now need to go through the distance arrays and calc.
        //the average distances.   avg = total dist / # of tours
        //This is for the Tours and Tour Distance by Purpose and Mode
        int[] purposeModeRowSum = new int[purposeMode.length];  //total tours by purpose
        int[] purposeModeColSum = new int[purposeMode[0].length]; //total tours by mode
        int totalTours = 0;      //total tours across all purposes and modes
        float[] purposeModeDistRowSum = new float[purposeModeDist.length];  //total distance by purpose
        float[] purposeModeDistColSum = new float[purposeModeDist[0].length];    //total distance by mode
        float totalDistance=0.0f; //total distance across all purposes and modes
        float[] totalRowAvgs = new float[purposeMode.length];  //total distance by purpose / total tours by purpose
        float[] totalColAvgs = new float[purposeMode[0].length]; //total distance by mode / total tours by mode
        float avgDist = 0.0f; //totalDistance / total Tours
        float[][] purposeModeAvgDist = new float[purposeMode.length][purposeMode[0].length];
        for(int r=0; r< purposeMode.length; r++){
            for(int c=0; c< purposeMode[0].length; c++){
                purposeModeRowSum[r]+=purposeMode[r][c];
                purposeModeColSum[c]+=purposeMode[r][c];
                purposeModeDistRowSum[r]+=purposeModeDist[r][c];
                purposeModeDistColSum[c]+=purposeModeDist[r][c];
                purposeModeAvgDist[r][c] = purposeModeDist[r][c]/purposeMode[r][c];
            }
            totalTours+=purposeModeRowSum[r];
            totalDistance+=purposeModeDistRowSum[r];
        }
        for(int r=0;r<purposeMode.length;r++){
            totalRowAvgs[r]=purposeModeDistRowSum[r] / purposeModeRowSum[r];
        }
        for(int c=0;c<purposeMode[0].length;c++){
            totalColAvgs[c]= purposeModeDistColSum[c] / purposeModeColSum[c];
        }
        avgDist = totalDistance / totalTours;


        //Next deal with the Work Tours and Work Tour Distance by Work Segment and Mode
        float[][] wSegModeAvgDist = new float[wSegMode.length][wSegMode[0].length]; //stores avg dist. by seg - mode
        float[] wSegModeDistRowSums = new float[wSegModeDist.length]; //total work tour distance by segment
        float[] wSegModeDistColSums = new float[wSegModeDist[0].length]; //total work tour distance by mode
        int[] wSegModeRowSums = new int[wSegMode.length];  //total work tours by segment
        int[] wSegModeColSums = new int[wSegMode[0].length];   //total work tours by mode
        float[] wSegModeSegAvgs = new float[wSegMode.length];   //total tour distance / total tours by segment
        float[] wSegModeModeAvgs = new float[wSegMode[0].length]; //total tour distance / total tours by mode
        float totalWDist = 0.0f;  // sum of all work tour distances (will be a sum of the row sums)
        int totalWTours = 0;  //sum of all work tours (will be a sum of the row sums)
        float wAvgDist = 0.0f;  //sum of all avg. work tour distances (totalWDist / totalWTours)
        for(int r=0; r< wSegMode.length ; r++){
            for(int c=0; c< wSegMode[0].length; c++){
                wSegModeAvgDist[r][c] = wSegModeDist[r][c] / wSegMode[r][c];
                wSegModeDistRowSums[r] += wSegModeDist[r][c];
                wSegModeDistColSums[c] += wSegModeDist[r][c];
                wSegModeRowSums[r] += wSegMode[r][c];
                wSegModeColSums[c] += wSegMode[r][c];
            }
            totalWDist+=wSegModeDistRowSums[r];
            totalWTours+=wSegModeRowSums[r];
        }
        for(int r=0;r<wSegMode.length;r++){
            wSegModeSegAvgs[r]=wSegModeDistRowSums[r] / wSegModeRowSums[r];
        }
        for(int c=0;c<wSegMode[0].length;c++){
            wSegModeModeAvgs[c]= wSegModeDistColSums[c] / wSegModeColSums[c];
        }
        wAvgDist = totalWDist / totalWTours;

        //Lastly deal with the Non-work Tours
        float[][][] nwSegModeAvgDist = new float[nwSegMode.length][nwSegMode[0].length][nwSegMode[0][0].length];
        float[][] nwSegModeDistRowSums = new float[nwSegMode.length][nwSegMode[0].length];
        float[][] nwSegModeDistColSums = new float[nwSegMode.length][nwSegMode[0][0].length];
        int[][] nwSegModeRowSums = new int[nwSegMode.length][nwSegMode[0].length];
        int[][] nwSegModeColSums = new int[nwSegMode.length][nwSegMode[0][0].length];
        float[][] nwSegModeSegAvgs = new float[nwSegMode.length][nwSegMode[0].length];
        float[][] nwSegModeModeAvgs = new float[nwSegMode.length][nwSegMode[0][0].length];
        float[] totalNWDist = new float[nwSegMode.length];   //total distance for each type of non-work tour.
        int[] totalNWTours = new int[nwSegMode.length];   //total tours for each type of non-work tour.
        float[] nwAvgDist = new float[nwSegMode.length];
        for(int p=0; p<nwSegMode.length; p++){
            for(int r=0; r<nwSegMode[0].length; r++){
                for(int c=0; c<nwSegMode[0][0].length; c++){
                    nwSegModeAvgDist[p][r][c] = nwSegModeDist[p][r][c] / nwSegMode[p][r][c];
                    nwSegModeDistRowSums[p][r] += nwSegModeDist[p][r][c];
                    nwSegModeDistColSums[p][c] += nwSegModeDist[p][r][c];
                    nwSegModeRowSums[p][r] += nwSegMode[p][r][c];
                    nwSegModeColSums[p][c] += nwSegMode[p][r][c];
                }
                totalNWDist[p] += nwSegModeDistRowSums[p][r];
                totalNWTours[p] += nwSegModeRowSums[p][r];
            }
        }
        for(int p=0; p<nwSegMode.length; p++){
            for(int r=0; r<nwSegMode[0].length; r++){
                nwSegModeSegAvgs[p][r] = nwSegModeDistRowSums[p][r] / nwSegModeRowSums[p][r];
            }
        }
        for(int p=0; p<nwSegMode.length; p++){
            for(int c=0; c<nwSegMode[0][0].length; c++){
                nwSegModeModeAvgs[p][c] = nwSegModeDistColSums[p][c] / nwSegModeColSums[p][c];
            }
        }
        for(int p=0; p<nwSegMode.length; p++){
            nwAvgDist[p] = totalNWDist[p] / totalNWTours[p];
        }

        //Now create output strings

        //First do purposeMode
        String[] purposeDistString = {"","","","","","",""}; //length = # of purposes plus 1
        String[] purposeString = {"","","","","","",""};  //add extra row for column totals
        String[] purposeModeAvgString={"","","","","","",""};
        for(int r=0; r<purposeMode.length; r++){  //purpose (6)
            for(int c=0; c< purposeMode[0].length; c++){  //modes (8)
                purposeDistString[r]+=(purposeModeDist[r][c]+",");    //total dist
                purposeString[r]+=(purposeMode[r][c]+",");           //total tours
                purposeModeAvgString[r]+=(purposeModeAvgDist[r][c]+","); //avg dist
            }
            purposeDistString[r]+=purposeModeDistRowSum[r]; //  add on the row total
            purposeString[r]+=purposeModeRowSum[r];
            purposeModeAvgString[r]+=totalRowAvgs[r];
        }
        for(int c=0;c<purposeMode[0].length;c++){
            purposeDistString[purposeDistString.length-1]+=(purposeModeDistColSum[c]+",");  //create the row of column totals
            purposeString[purposeString.length-1]+=(purposeModeColSum[c]+",");
            purposeModeAvgString[purposeModeAvgString.length-1]+=(totalColAvgs[c]+",");
        }
        purposeDistString[purposeDistString.length-1]+=totalDistance;   //add on the row total
        purposeString[purposeString.length-1]+=totalTours;
        purposeModeAvgString[purposeModeAvgString.length-1]+=avgDist;

        //Next do the wSegMode
        String[] wDistString = {"","","","","","","","","",""};
        String[] wString = {"","","","","","","","","",""};
        String[] wAvgString = {"","","","","","","","","",""};
        for(int r=0; r< wSegMode.length; r++){   // work segments(9)    //these 2 for loops create the rows
            for(int c=0; c< wSegMode[0].length; c++){  //modes(8)       //for each segment
                wDistString[r] +=(wSegModeDist[r][c] + ",");
                wString[r] += (wSegMode[r][c] + ",");
                wAvgString[r] += (wSegModeAvgDist[r][c] + ",");
            }
            wDistString[r] += wSegModeDistRowSums[r];         //and append the row totals on the end
            wString[r] += wSegModeRowSums[r];
            wAvgString[r] += wSegModeSegAvgs[r];
        }
        for(int c=0; c< wSegMode[0].length;c++){               //this loop creates the row of column totals
            wDistString[wDistString.length-1] += (wSegModeDistColSums[c] + ",");
            wString[wString.length-1] += (wSegModeColSums[c] + ",");
            wAvgString[wAvgString.length-1] += (wSegModeModeAvgs[c] + ",");
        }
        wDistString[wDistString.length-1] += totalWDist;      //and appends the table total on the end
        wString[wString.length-1] += totalWTours;
        wAvgString[wAvgString.length-1] += wAvgDist;


        //Next do the nwSegMode
        String[][] nwDistString = new String[nwSegMode.length][nwSegMode[0].length + 1];
        String[][] nwString = new String[nwSegMode.length][nwSegMode[0].length + 1];
        String[][] nwAvgString = new String[nwSegMode.length][nwSegMode[0].length + 1];
        for(int i=0; i< nwString.length; i++){    //initialize the String arrays, 1 for each non-work purpose
            for(int j=0; j<nwString[0].length; j++){
                nwString[i][j]="";
                nwDistString[i][j]="";
                nwAvgString[i][j]="";
            }
        }
        for(int p=0; p< nwSegMode.length; p++){  //non-work purposes (5) form the table body
            for(int r=0; r<nwSegMode[0].length; r++){  //non-work segments (9)
                for(int c=0; c<nwSegMode[0][0].length; c++){ //modes (8)
                    nwDistString[p][r] += (nwSegModeDist[p][r][c] + ",");
                    nwString[p][r] += (nwSegMode[p][r][c] +",");
                    nwAvgString[p][r] += (nwSegModeAvgDist[p][r][c] + ",");
                }
                nwDistString[p][r] += nwSegModeDistRowSums[p][r];   //add the row totals to the ends
                nwString[p][r] += nwSegModeRowSums[p][r];
                nwAvgString[p][r] += nwSegModeSegAvgs[p][r];
            }
        }
        for(int p=0; p< nwSegMode.length; p++){      //create the row of column totals
            for(int c=0; c<nwSegMode[0][0].length; c++){
                nwDistString[p][nwDistString[0].length-1] += (nwSegModeDistColSums[p][c] + ",");
                nwString[p][nwString[0].length-1] += (nwSegModeColSums[p][c] + ",");
                nwAvgString[p][nwAvgString[0].length-1] += (nwSegModeModeAvgs[p][c] + ",");
            }
        }
        for(int p=0; p<nwSegMode.length;p++){       //add on the row totals
            nwDistString[p][nwDistString[0].length-1] += totalNWDist[p];
            nwString[p][nwString[0].length-1] += totalNWTours[p];
            nwAvgString[p][nwAvgString[0].length-1] += nwAvgDist[p];
        }

        //Finally do the work trips
        String[] wTripString = {"","","","","","","","",""};
        for(int r=0; r< wTrips.length; r++){ //segments (9)
            for(int c=0; c< wTrips[0].length; c++){  //modes (8 + 3)
                wTripString[r]+=wTrips[r][c];
                wTripString[r]+=",";
                wTripSum+=wTrips[r][c];
            }
        }
        //And the non-work trips
        String[][] nwTripString = new String[5][9];
        for(int i=0; i<nwTripString.length;i++){
            for(int j=0; j<nwTripString[0].length;j++){ //initialize String array with blanks
                nwTripString[i][j]="";
            }
        }
        for(int p=0; p< nwTrips.length; p++){  //non-work purposes (5)
            for(int r=0; r<nwTrips[0].length; r++){  //non-work segments (9)
                for(int c=0; c<nwTrips[0][0].length; c++){ //modes (8 + 3)
                    nwTripString[p][r]+=nwTrips[p][r][c];
                    nwTripString[p][r]+=",";
                    nwTripSum[p]+=nwTrips[p][r][c];
                }
            }
        }

        //Now we need to write out the summary to a file
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(outputFile));
            pw.println("Summary of Tour Information - Total tours: "+ totalTours);

            pw.println();
            pw.println();

            //First write out total tour Distance by purpose and mode
            pw.println("Total Tour Distance by purpose and mode");
            pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
            for(int s=0;s<purposeModeDist.length;s++){
                pw.println(ActivityPurpose.ACTIVITY_PURPOSES[s+1]+","+purposeDistString[s]);
            }
            pw.println("totals,"+purposeDistString[purposeDistString.length-1]);

            pw.println();
            pw.println();

            //Then write out number of Tours by purpose and mode
            pw.println("Tour Frequency by purpose and mode");
            pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
            for(int s=0;s<purposeMode.length;s++){
                pw.println(ActivityPurpose.ACTIVITY_PURPOSES[s+1]+","+purposeString[s]);
            }
            pw.println("totals,"+purposeString[purposeString.length-1]);

            pw.println();
            pw.println();

            //Next write out average dist of tours by purpose and mode
            pw.println("Average Tour Distance by purpose and mode");
            pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,averages");
            for(int s=0;s<purposeModeAvgDist.length;s++){
                pw.println(ActivityPurpose.ACTIVITY_PURPOSES[s+1]+","+purposeModeAvgString[s]);
            }
            pw.println("averages,"+purposeModeAvgString[purposeModeAvgString.length-1]);

            pw.println();
            pw.println();

            //Now write out the Distance of Work Tours by work segment and mode
            pw.println("Total Work Tour Distance by work segment and mode");
            pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
            for(int s=0; s<wSegMode.length; s++){
                pw.println(s+"," + wDistString[s]);
            }
            pw.println("totals,"+ wDistString[wDistString.length - 1]);

            pw.println();
            pw.println();

            //Next write out Work Tours by work segment and mode
            pw.println("Work Tour Frequency by work segment and mode");
            pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
            for(int s=0;s<wSegMode.length;s++){
                pw.println(s+","+wString[s]);
            }
            pw.println("totals,"+wString[wString.length-1]);

            pw.println();
            pw.println();

            //Next write out average work tour distance by work segment and mode
            pw.println("Average Distance of Work Tours by work segment and mode");
            pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
            for(int s=0;s<wSegMode.length;s++){
                pw.println(s+","+wAvgString[s]);
            }
            pw.println("averages," + wAvgString[wAvgString.length-1]);

            //Next write out Non-work tours by non-work segment and mode

            for(int p=0; p<nwSegMode.length;p++){  //for each non-work purpose
                pw.println();
                pw.println();

                pw.println("Total Tour Distance by non-work segment and mode - Purpose " + ActivityPurpose.ACTIVITY_PURPOSES[p+2]);
                pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
                for(int s=0; s<nwSegMode[0].length; s++){
                    pw.println(s+"," + nwDistString[p][s]);
                }
                pw.println("totals,"+ nwDistString[p][nwDistString[0].length - 1]);

                pw.println();
                pw.println();

                pw.println("Tour Frequency by non-work segment and mode - Purpose " + ActivityPurpose.ACTIVITY_PURPOSES[p+2] );
                pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
                for(int s=0; s<nwSegMode[0].length;s++){
                    pw.println(s+","+nwString[p][s]);
                }
                pw.println("totals,"+nwString[p][nwString[0].length - 1]);

                pw.println();
                pw.println();

                pw.println("Average Tour Distance by non-work segment and mode - Purpose " + ActivityPurpose.ACTIVITY_PURPOSES[p+2]);
                pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,totals");
                for(int s=0; s<nwSegMode[0].length;s++){
                    pw.println(s+","+nwAvgString[p][s]);
                }
                pw.println("averages,"+ nwAvgString[p][nwAvgString[0].length -1]);
            }

            pw.println();
            pw.println();

            //Next write out Work Trips by work segment and mode
            pw.println("Work Trips by work segment and mode - Total trips in table: " + wTripSum);
            pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,driveAlone,shared2,shared3+");
            for(int s=0; s<wTripString.length; s++){
                pw.println(s+","+wTripString[s]);
            }

            pw.println();
            pw.println();

            //Finally write out Non-work Trips by non-work segment and mode
            pw.println("Non-work Trips by non-work segment and mode");
            for(int p=0; p<nwTripString.length; p++){
                pw.println();
                pw.println("Purpose " + ActivityPurpose.ACTIVITY_PURPOSES[p+2]+" - Total trips in table: " + nwTripSum[p]);
                pw.println(",driver,passenger,walk,bike,wtransit,transitp,ptransit,dtransit,driveAlone,shared2,shared3+");
                for(int s=0;s<nwTripString[0].length; s++){
                    pw.println(s+","+nwTripString[p][s]);
                }
            }

            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }//end of Summarize Tours


}
