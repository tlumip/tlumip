package com.pb.tlumip.model;

import com.pb.common.util.ResourceUtil;
import com.pb.models.censusdata.SwIncomeSize;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The {@code IncomeSize2} ...
 *
 * @author crf <br/>
 *         Started May 26, 2011 9:24:50 AM
 */
public class IncomeSize2 extends SwIncomeSize {
    public static final String INCOME_SIZE_CONVERSION_PROPERTY = "spg.income.size.conversion.factor";
    public static final String INCOME_CATEGORY_UPPER_BOUNDS_PROPERTY = "spg.income.size.income.upper.bounds";
    public static final String HHSIZEE_CATEGORY_UPPER_BOUNDS_PROPERTY = "spg.income.size.hh.size.upper.bounds";

    private final double incomeSizeConversion;
    private final boolean conversionOn;
    private final String[] incomeSizeLabels;
    private final int[] incomeEdges;
    private final int[] hhSizeEdges;




    public IncomeSize2 (Map<?,?> rbMap) {
        super();

        incomeSizeConversion = Double.parseDouble((String)rbMap.get(INCOME_SIZE_CONVERSION_PROPERTY));
        conversionOn = incomeSizeConversion != 1.0;
        incomeEdges = parseList((String) rbMap.get(INCOME_CATEGORY_UPPER_BOUNDS_PROPERTY));
        hhSizeEdges = parseList((String) rbMap.get(HHSIZEE_CATEGORY_UPPER_BOUNDS_PROPERTY));
        incomeSizeLabels = formLabels();
        if (conversionOn) {
            for (int i = 0; i < incomeEdges.length; i++)
                incomeEdges[i] = (int) Math.round(incomeEdges[i]*incomeSizeConversion);
        }
    }

    private int[] parseList(String intList) {
        String[] sints = intList.split(",");
        int[] ints = new int[sints.length];
        int counter = 0;
        for (String sint : sints)
            ints[counter++] = Integer.parseInt(sint.trim());
        return ints;
    }

    private String[] formLabels() {
        //HHXXtoYYk3plus
        List<String> labels = new LinkedList<String>();
        int bottomIncome = 0;
        for (int i : incomeEdges) {
            int labelIncome = i / 1000;
            String incPart = "HH" + bottomIncome + "to" + labelIncome + "k";
            int bottomHHSize = 1;
            for (int h : hhSizeEdges) {
                labels.add(incPart + bottomHHSize + "to" + h);
                bottomHHSize = h+1;
            }
            labels.add(incPart + bottomHHSize + "plus");
            bottomIncome = labelIncome;
        }
        String incPart = "HH" + bottomIncome + "kUp";
        int bottomHHSize = 1;
        for (int h : hhSizeEdges) {
            labels.add(incPart + bottomHHSize + "to" + h);
            bottomHHSize = h+1;
        }
        labels.add(incPart + bottomHHSize + "plus");
        return labels.toArray(new String[labels.size()]);
    }



    // return all the IncomeSize category labels.
    public String[] getIncomeSizeLabels() {
        return incomeSizeLabels;
    }


    // return the number of HH income/HH size categories.
    public int getNumberIncomeSizes() {
        return incomeSizeLabels.length;
    }


    // return the IncomeSize category index given the label.
    public int getIncomeSizeIndex(String incomeSizeLabel) {

        int returnValue = -1;

        for (int i=0; i < incomeSizeLabels.length; i++) {
            if ( incomeSizeLabel.equalsIgnoreCase( incomeSizeLabels[i] ) ) {
                returnValue = i;
                break;
            }
        }

        return returnValue;
    }


    // return the IncomeSize category label given the index.
    public String getIncomeSizeLabel(int incomeSizeIndex) {
        return incomeSizeLabels[incomeSizeIndex];
    }


	// return the IncomeSize category index given the pums IncomeSize code
	// from the pums person record OCCUP field.
	public int getIncomeSize(int income, int hhSize) {

        int incIndex = 0;
        int sizeIndex = 0;
        for (; incIndex < incomeEdges.length; incIndex++)
            if (income < incomeEdges[incIndex])
                break;
        for (; sizeIndex < hhSizeEdges.length; sizeIndex++)
            if (hhSize <= hhSizeEdges[sizeIndex])
                break;

	    return incIndex*(hhSizeEdges.length+1)+sizeIndex;
	}

}

