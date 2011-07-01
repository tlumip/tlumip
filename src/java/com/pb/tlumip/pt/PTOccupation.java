package com.pb.tlumip.pt;

import com.pb.common.model.ModelException;
import com.pb.models.pt.PTOccupationReferencer;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Dec 12, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public enum PTOccupation implements PTOccupationReferencer {
    NONE, MANAGER, HEALTH, POST_SECONDARY_ED,
    OTHER_ED, PROFESSIONAL, RETAIL, OTHER, NON_OFFICE;

    private static boolean USING_AA = true;

    /**
     * Specifies using AA (which means a different set of PI/PECAS/AA occupations names/codes
     *
     * @param usingAA
     *        {@code true} if using AA, false otherwise (default is {@code false}).
     */
    public static void setUsingAA(boolean usingAA) {
        USING_AA = usingAA;
    }

    /**
     * Convert occupation as a string into an enum.
     *
     * @see 'pt.properties
     *
     * @param occupation
     *            Occupation as a String
     *
     * @return PTOccupation
     */
    public PTOccupation getOccupation(String occupation) {
        return USING_AA ? getOccupationAA(occupation) : getOccupationPI(occupation);
    }

    private PTOccupation getOccupationAA(String occupation) {
        if (occupation.startsWith("No_Occupation"))
            return NONE;
        if (occupation.startsWith("A1-Mgmt Bus"))
            return MANAGER;
        if (occupation.startsWith("B1-Prof Specialty"))
            return PROFESSIONAL;
        if (occupation.startsWith("B2-Education"))
            return OTHER_ED;
        if (occupation.startsWith("B3-Health"))
            return HEALTH;
        if (occupation.startsWith("B4-Technical Unskilled"))
            return PROFESSIONAL;
        if (occupation.startsWith("C1-Sales Clerical Professionals"))
            return PROFESSIONAL;
        if (occupation.startsWith("C2-Sales Service"))
            return RETAIL;
        if (occupation.startsWith("C3-Clerical"))
            return OTHER;
        if (occupation.startsWith("C4-Sales Clerical Unskilled"))
            return OTHER;
        if (occupation.startsWith("D1-Production Specialists"))
            return NON_OFFICE;
        if (occupation.startsWith("D2-MaintConstRepair Specialists"))
            return NON_OFFICE;
        if (occupation.startsWith("D3-ProtectTrans Specialists"))
            return NON_OFFICE;
        if (occupation.startsWith("D4-Blue Collar Unskilled"))
            return NON_OFFICE;
        throw new ModelException("Could not convert the occupation: " + occupation);
    }

    private PTOccupation getOccupationPI(String occupation) {
        if (occupation.startsWith("0_NoOccupation")) {
            return NONE;
        }
        if (occupation.startsWith("1_ManPro")) {
            return MANAGER;
        }
        if (occupation.startsWith("1a_Health")) {
            return HEALTH;
        }
        if (occupation.startsWith("2_PstSec")) {
            return POST_SECONDARY_ED;
        }
        if (occupation.startsWith("3_OthTchr")) {
            return OTHER_ED;
        }
        if (occupation.startsWith("4_OthP&T")) {
            return PROFESSIONAL;
        }
        if (occupation.startsWith("5_RetSls")) {
            return RETAIL;
        }
        if (occupation.startsWith("6_OthR&C")) {
            return OTHER;
        }
        if (occupation.startsWith("7_NonOfc")) {
            return NON_OFFICE;
        }
        throw new ModelException("Could not convert the occupation: "
                + occupation);
    }

    public PTOccupation getOccupation(int index) {
        return USING_AA ? getOccupationAA(index) : getOccupationPI(index);
    }

    private PTOccupation getOccupationAA(int index) {

        switch(index){
            case 0  : return NONE;
            case 1  : return MANAGER;
            case 2  : return PROFESSIONAL;
            case 3  : return OTHER_ED;
            case 4  : return HEALTH;
            case 5  : return PROFESSIONAL;
            case 6  : return PROFESSIONAL;
            case 7  : return RETAIL;
            case 8  : return OTHER;
            case 9  : return OTHER;
            case 10 : return NON_OFFICE;
            case 11 : return NON_OFFICE;
            case 12 : return NON_OFFICE;
            case 13 : return NON_OFFICE;

            default: throw new ModelException("Invalid occupation index code: " + index);
        }
    }

    private PTOccupation getOccupationPI(int index) {

        switch(index){
            case 0: return NONE;
            case 1: return MANAGER;
            case 2: return HEALTH;
            case 3: return POST_SECONDARY_ED;
            case 4: return OTHER_ED;
            case 5: return PROFESSIONAL;
            case 6: return RETAIL;
            case 7: return OTHER;
            case 8: return NON_OFFICE;

            default: throw new ModelException("Invalid occupation index code: "
                    + index);
        }
    }

    public PTOccupation getRetailOccupation(){
        return RETAIL;
    }
    

    public static void main(String[] args){
        PTOccupationReferencer myRef = PTOccupation.NONE;
        Enum occupation = myRef.getOccupation(2);

        System.out.println("Name: " + occupation.name());
        System.out.println("Index: " + occupation.ordinal());
    }
}
