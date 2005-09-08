/*
 * Created on Dec 10, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.pb.osmp.ld;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.WeakHashMap;

import java.util.Hashtable;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ParcelScorer implements Comparator {


    final int targetZone=0;
    final char coverageType;
    final double geogParam=0;
    // TODO add geographical restrictions/penalty
    final double farOverPenalty=3.0;
    // TODO add mechanism for user specified farOverPenalty (by use type?)
    final double farOverPenaltyKicksInAt=0.7;
    // TODO add mechanism for user specified point at which farOverPenalty starts kicking in
    final double coverageTypeMatches=0.5;
    final double coverageTypeConflicts=-5.0;
    // TODO add mechanism for user specified coverageTypeMatches booster
    ArrayList hintLists = new ArrayList();
    
    public void clearScoreRecord() {
        oldScores.clear();
    }
    
    static class HintList {
        final String fieldName;
        final String[] fieldEntries;
        final double[] matchCoefficients;
        final double[] farCoefficients;  // floor area ratio coefficients 
        
        HintList(String fieldName, int numberOfHints) {
            this.fieldName = fieldName;
            fieldEntries = new String[numberOfHints];
            matchCoefficients = new double[numberOfHints];
            farCoefficients = new double[numberOfHints];
        }
        
        HintList(String fieldName, String[] fieldEntries, double[] matchCoefficients, double[] farCoefficients) {
            this.fieldName = fieldName;
            this.fieldEntries=fieldEntries;
            this.matchCoefficients = matchCoefficients;
            this.farCoefficients = farCoefficients;
        }
        
    }
    
    private WeakHashMap oldScores = new WeakHashMap();
    
    static class ScoreVersion {
    	double score;
    	int revision;
    }
    
    public double score(Parcel c) {
    	ScoreVersion oldScore = (ScoreVersion) oldScores.get(c);
    	if (oldScore != null) {
    		if (c.getRevision() == oldScore.revision) {
    			return oldScore.score;
    		} 
    	}
        double score = 0; 
        double farTarget = 0;
        try {
            if (c.getTaz()==targetZone) score+=geogParam;
            for (int hintListNumber = 0;hintListNumber<hintLists.size();hintListNumber++) {
                HintList hl = (HintList) hintLists.get(hintListNumber);
                String parcelValue = c.getValue(hl.fieldName);
                if (parcelValue == null) {
                    parcelValue = "";
                }
                for (int i=0;i<hl.fieldEntries.length;i++) {
                    if (parcelValue.equals(hl.fieldEntries[i])) {
                        score+=hl.matchCoefficients[i];
                        farTarget +=hl.farCoefficients[i];
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Can't score parcel "+this+" "+e.toString());
        }
        double far = 0;
        try {
            double area = c.getSize();
            if (area==0) return Double.NEGATIVE_INFINITY;
            far = c.getSqFtAssigned()/area;
        } catch (SQLException e1) {
            System.out.println("can't find existing FAR for parcel"+c+", error "+e1);
            e1.printStackTrace();
        }
        if (far>farTarget*farOverPenaltyKicksInAt) {
            score -= (far-farTarget*farOverPenaltyKicksInAt)*farOverPenalty/farTarget;
        }
        try {
            char currentCoverage = c.getCurrentCoverage();
            if (currentCoverage!=' ') {
                if (currentCoverage == coverageType) score+=coverageTypeMatches;
                else score+=coverageTypeConflicts;
                
            }
        } catch (SQLException e2) {
            System.out.println("Can't get current coverage string");
            e2.printStackTrace();
            throw new RuntimeException(e2);
        }
        ScoreVersion scoreRecord = new ScoreVersion();
        scoreRecord.score = score;
        scoreRecord.revision = c.getRevision();
        oldScores.put(c,scoreRecord);
        return score;
    }

    /**
     * 
     */
    public ParcelScorer(char coverage) {
        coverageType= coverage;
    }
    
    void addHint(HintList l) {
        hintLists.add(l);
    }

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2) {
        if (o1 instanceof Parcel && o2 instanceof Parcel) {
            double score1 = score((Parcel) o1);
            double score2 = score((Parcel) o2);
            if (score1<score2) return -1;
            if (score1>score2) return 1;
            if (score2==score1)return 0;
            throw new RuntimeException("Can't compare parcel scores "+score1+" and "+score2);
        }
        throw new ClassCastException("Trying to compare non-parcels with ParcelSorter");
    }

}
