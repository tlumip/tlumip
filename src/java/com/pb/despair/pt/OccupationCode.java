package com.pb.tlumip.pt;
/**
 * Creates and defines Occupation Codes from PUMS code in HA file
 * 
 * @author Steve Hansen
 * @version  1.0 12/1/2003
 * @version 2.0 4/1/04 (updated codes)
 * 
 */

public final class OccupationCode{
    public static final byte NOT_EMPLOYED=0;
    public static final byte MANAGERS_PROFESSIONALS=1;
    public static final byte HEALTH=2;
    public static final byte P0ST_SEC_TEACHERS=3;
    public static final byte OTHER_TEACHERS=4;
    public static final byte OTHER_PROF_TECH=5;
    public static final byte RETAIL_SALES=6; 
    public static final byte OTHER_RETAIL_CLERICAL=7; 
    public static final byte ALL_OTHER=8;   

    private String[] occupationString = {"0_NotEmployed",
                                         "1_ManPro",
                                         "1a_Health",
                                         "2_PstSec",
                                         "3_OthTchr",
                                         "4_OthP&T",
                                         "5_RetSls",
                                         "6_OthR&C",
                                         "7_NonOfc"};
    
    
    public String getOccupationString(byte occupationCode){ 
        return occupationString[occupationCode];
    }
    
    //to code occupation from PUMS occupation code, to be compatible with OR HH survey
    public static byte codeOccupationFromPUMS(int pumsCode){
        byte occupation = 0;
        if(pumsCode==0) occupation = NOT_EMPLOYED;                          
        if(pumsCode>=1 && pumsCode<=82) occupation=MANAGERS_PROFESSIONALS;
        if(pumsCode>=83 && pumsCode<=112) occupation=HEALTH;
        if(pumsCode>=113 && pumsCode<=154) occupation=P0ST_SEC_TEACHERS;
        if(pumsCode>=155 && pumsCode<=162) occupation=OTHER_TEACHERS;
        if(pumsCode>=163 && pumsCode<=262) occupation=OTHER_PROF_TECH;
        if(pumsCode>=263 && pumsCode<=282) occupation=RETAIL_SALES;
        if(pumsCode>=283 && pumsCode<=402) occupation=OTHER_RETAIL_CLERICAL;
        if(pumsCode>=403 && pumsCode<=999) occupation=ALL_OTHER;
        return occupation;
    }
    //to code occupation from PUMS occupation code, to be compatible with OR HH survey
    public static byte getOccupationCode(String occupationName){
        byte occupation = -1;
        if(occupationName.equals("1_ManPro")) occupation=MANAGERS_PROFESSIONALS;
        if(occupationName.equals("1a_Health")) occupation=HEALTH;
        if(occupationName.equals("2_PstSec")) occupation=P0ST_SEC_TEACHERS;
        if(occupationName.equals("3_OthTchr")) occupation=OTHER_TEACHERS;
        if(occupationName.equals("4_OthP&T")) occupation=OTHER_PROF_TECH;
        if(occupationName.equals("5_RetSls")) occupation=RETAIL_SALES;
        if(occupationName.equals("6_OthR&C")) occupation=OTHER_RETAIL_CLERICAL;
        if(occupationName.equals("7_NonOfc")) occupation=ALL_OTHER;
        return occupation;
    }                         
}