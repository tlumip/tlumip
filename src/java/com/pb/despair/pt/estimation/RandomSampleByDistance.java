package com.pb.despair.pt.estimation;
import com.pb.common.matrix.CollapsedMatrixCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

/** 
 * A class for a random sample by distance
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class RandomSampleByDistance {

    //each district can contain a number of tazs from 1 to n where n is numberDistricts
    int numberDistricts = 1;
    ArrayList districts;
    static final boolean debug = false;
    static Logger logger = Logger.getLogger("com.pb.despair.pt.RandomSampleByDistance");


	/**
	 * default constructor
	 */
	public RandomSampleByDistance(){
		
	}

   /**
    *randomSampleByDistrict(): creates bins or districts
    * 
    *@param nd number of districts to create
    *
    **/

    public RandomSampleByDistance(int nd) {

        districts = new ArrayList(numberDistricts);
        numberDistricts = nd;
        for (int i = 0; i < numberDistricts; ++i) {
            ArrayList tazs = new ArrayList();
            districts.add(tazs);
        }
    }

    /**
    *addTazsToDistricts(): adds the tazs to districts
    * 
    *@param tazs: an array of tazs, sorted from closest to furthest
    * from origin zone
    *
    **/

    public void addTazsToDistricts(DistrictTaz[] tazs) {

        int numberInEachDistrict = tazs.length / numberDistricts;
        logger.finer("There will be " + numberInEachDistrict + " tazs in each district");
        int numberAllocated = 0;
        Iterator districtIterator = districts.iterator();
        ArrayList thisDistrict = new ArrayList();
        while (districtIterator.hasNext()) {

            thisDistrict = (ArrayList) districtIterator.next();
            logger.finer("adding tazs to district");
            for (int j = 0; j < numberInEachDistrict; ++j) {
                if (numberAllocated < tazs.length) {
                    DistrictTaz thisTaz = (DistrictTaz) tazs[numberAllocated];
                    logger.finer("   taz " + thisTaz.zoneNumber);
                    thisDistrict.add(thisTaz);
                    ++numberAllocated;
                }
            }
        }
        //add any remaining unallocated tazs to last district
        while (numberAllocated < tazs.length) {
            if (debug)
                logger.finer("adding unallocated tazs to last district\n");
            DistrictTaz thisTaz = (DistrictTaz) tazs[numberAllocated];
            thisDistrict.add(thisTaz);
            ++numberAllocated;
        }

    }

    /**
    *randomlySelectFromDistricts(): randomly selects one taz from each district
    * 
    *@return DistrictTaz[]: an array of tazs, sorted from closest to furthest
    * from origin zone
    *
    *@param chosenZoneNumber: the number of the chosen Taz
    */

    public DistrictTaz[] randomlySelectFromDistricts(int chosenZoneNumber) {

        Iterator districtIterator = districts.iterator();

        if (debug)
            logger.finer("tazArray size = " + districts.size());

        DistrictTaz[] tazArray = new DistrictTaz[districts.size()];
        int i = 0;
        boolean foundTaz = false;

        Random r = new Random();
        while (districtIterator.hasNext()) {

            ArrayList thisDistrict = (ArrayList) districtIterator.next();

            //search for chosen taz in this district; if found, set the 
            //taz = chosen taz and continue
            boolean foundTazInThisDistrict = false;
            Iterator tazIterator = thisDistrict.iterator();
            if (debug)
                logger.finer("Searching districtArray " + (i + 1));
            while (tazIterator.hasNext() && foundTaz == false) {

                DistrictTaz thisTaz = (DistrictTaz) tazIterator.next();
                if (thisTaz.zoneNumber == chosenZoneNumber) {
                    if (debug)
                        logger.finer("Found taz " + thisTaz.zoneNumber);
                    foundTaz = true;
                    foundTazInThisDistrict = true;
                    tazArray[i] = thisTaz;
                    ++i;
                }
            }

            if (foundTazInThisDistrict)
                continue;
            if (debug)
                logger.finer("Drawing random taz in districtArray " + (i + 1) + " size " + thisDistrict.size());

            //else draw a random number, select a taz based on the number, and continue
            int randomNumber = r.nextInt(thisDistrict.size());
            if (debug)
                logger.finer("Drawing taz " + randomNumber);
            tazArray[i] = (DistrictTaz) thisDistrict.get(randomNumber);
            ++i;
        }

        return tazArray;
    }
    /**
    * ChooseTazsAccordingToReferenceTAZ
    * 
    *@param chosenZoneNumber: the number of the chosen Taz
    *@param referenceZoneNumber: the number of the reference TAZ
    *@param skims  Matrix collection of skims
    *@param skimName The name of the skim to use for distance-based sampling
    * 
    *  element   description
    * -------   ------------
    * 0         chosen zone
    * 1         randomly chosen zone closer to reference zone than chosen zone
    * 2-4       3 next closest zones
    * 5-11      7 randomly selected zones within 10 miles of chosen
    * 12-20     8 randomly selected zones longer than 10 miles from chosen
    * 
    **/

    public ArrayList chooseTAZsAccordingToReferenceTAZ(
        DistrictTaz[] tazs,
        int chosenZoneNumber,
        int referenceZoneNumber,
        CollapsedMatrixCollection skims,
        String skimName,
        boolean worker) {

        float distanceFromReferenceToChosen = 0;
        ArrayList sample = new ArrayList();
        ArrayList tazArrayList = new ArrayList(Arrays.asList(tazs));

        //district 0 has chosen zone
        int numberAllocated = 0;
        ArrayList district0 = new ArrayList();
        for (int i = 0; i < tazs.length; ++i) {
            if (tazs[i].zoneNumber == chosenZoneNumber) {
                distanceFromReferenceToChosen = tazs[i].distance;
                tazs[i].probability = 1;
                district0.add(tazs[i]);
                if (debug)
                    logger.info("Distance from reference to chosen = " + distanceFromReferenceToChosen);
            }
        }
        //check to make sure chosen zone was found                    
        if (district0.size() == 0) {
            logger.severe("Cannot find chosen taz in tazs array");
            System.exit(1);
        }

        //add the chosen zone to the sample
        sample.add(district0.get(0));

        //district 1 has 1 zone as close or closer to chosen than reference zone
        ArrayList district1 = new ArrayList();

        if (worker) {
            float minDistance = 1000;
            int closestTaz = 0;
            for (int i = 0; i < tazs.length; ++i) {
                if (tazs[i].zoneNumber == chosenZoneNumber) //skip chosen
                    continue;

                if (tazs[i].distance < minDistance) {
                    minDistance = tazs[i].distance;
                    closestTaz = i;
                }
                if (tazs[i].distance <= distanceFromReferenceToChosen)
                    district1.add(tazs[i]);
            }
            if (debug)
                logger.info(
                    "There are " + district1.size() + " zones as close or closer to reference zone than chosen zone");

            //if no zone closer to chosen than reference, 
            //pick next closest zone to reference
            if (district1.size() == 0) {
                district1.add(tazs[closestTaz]);

            }
        }else{
            //if non-worker, recode all the distances based
            //on distance from chosen zone
            for (int i = 0; i < tazs.length; ++i) {
                tazs[i].distance = skims.getValue(chosenZoneNumber, tazs[i].zoneNumber, skimName);
            }
            //sort the tazs based on distance
            Arrays.sort(tazs);
            
            //choose the closest taz to the chosen zone
            float shortestDistance=1000;
            int shortestTaz=0;
            for(int i=0;i<tazs.length;++i){
                if(tazs[i].zoneNumber==chosenZoneNumber)
                    continue;
                if(tazs[i].distance<shortestDistance){
                    shortestDistance=tazs[i].distance;
                    shortestTaz=i;
                }
            }
            district1.add(tazs[shortestTaz]);
                   
        }
        //sample 1 TAZ from district 1
        ArrayList district1Chosen = this.randomlySelectFromDistrict(district1, 1);
        //set the choice probability
        DistrictTaz d1 = (DistrictTaz) district1Chosen.get(0);
        d1.probability = (float) 1.0 / (float) district1.size();
        //add it to sample
        sample.add(d1);
        //get the unchosen TAZs from all tazs
        ArrayList unchosen = this.getUnchosenTAZs(district1Chosen, tazArrayList);

        //remove the chosen zone from the list of unchosen TAZs
        for(int i=0;i<unchosen.size();++i){
            DistrictTaz thisTaz = (DistrictTaz) unchosen.get(i);
            if(thisTaz.zoneNumber==chosenZoneNumber)
                unchosen.remove(i);
        }

        //district 2 has 3 zones closest to chosen TAZ; need to reset distances
        ArrayList district2 = new ArrayList();
        for (int i = 0; i < unchosen.size(); ++i) {
            DistrictTaz taz = (DistrictTaz) unchosen.get(i);
            taz.distance = skims.getValue(chosenZoneNumber, taz.zoneNumber, skimName);
        }

        //sort the taz array based on newly calculated distance
        DistrictTaz[] newTazs = (DistrictTaz[]) unchosen.toArray();
        Arrays.sort(newTazs);

        //add the first 3 zones to district 2 and to sample
        for (int i = 0; i < 3; ++i) {
            newTazs[i].probability = 1;
            district2.add(newTazs[i]);
            sample.add(newTazs[i]);
        }

        //now get the unchosen zones again
        unchosen = this.getUnchosenTAZs(district2, unchosen);

        //district 3 has 7 randomly selected zones within 10 miles of chosen
        //district 4 has 8 randomly selected zones >=10 miles of chosen
        ArrayList district3 = new ArrayList();
        ArrayList district4 = new ArrayList();
        for (int i = 0; i < unchosen.size(); ++i) {
            DistrictTaz thisTaz = (DistrictTaz) unchosen.get(i);
            if (thisTaz.distance < 10.0) {
                district3.add(thisTaz);
            } else {
                district4.add(thisTaz);
            }
        }

        //if less than 7 tazs within 10 miles of chosen taz, exit
        if (district3.size() < 7) {
            logger.severe("Error: Less than 7 TAZs within 10 miles of chosen");
            System.exit(1);
        }

        ArrayList district3Chosen = this.randomlySelectFromDistrict(district3, 7);
        ArrayList district4Chosen = this.randomlySelectFromDistrict(district3, 8);

        for (int i = 0; i < district3Chosen.size(); ++i) {
            DistrictTaz thisTaz = (DistrictTaz) district3Chosen.get(i);
            thisTaz.probability = (float) district3Chosen.size() / (float) district3.size();
            sample.add(thisTaz);
        }

        for (int i = 0; i < district4Chosen.size(); ++i) {
            DistrictTaz thisTaz = (DistrictTaz) district4Chosen.get(i);
            thisTaz.probability = (float) district4Chosen.size() / (float) district4.size();
            sample.add(thisTaz);
        }
        if (debug)
            logger.info("There are " + sample.size() + " elements in sample");
        return sample;
    }

    /**
    *randomlySelectFromDistrict(): randomly selects a set of tazs from a district
    * 
    *@param district An arraylist of tazs to choose from
    *@param numberOfTAZs the number of tazs to choose
    *@return an array of tazs randomly selected from the district
    */

    public ArrayList randomlySelectFromDistrict(ArrayList district, int numberOfTAZs) {

        Iterator districtIterator = district.iterator();

        if (debug) {
            logger.info(
                "randomly selecting "
                    + numberOfTAZs
                    + " from a district of tazs "
                    + district.size()
                    + " elements long");
        }

        ArrayList chosenArray = new ArrayList();

        //an array of bools that tracks whether the taz has been chosen or not
        boolean[] chosen = new boolean[district.size()];

        //the total choices to make for this district
        int totalToChoose = Math.min(district.size(), numberOfTAZs);

        Random r = new Random();

        for (int i = 0; i < totalToChoose; ++i) {
            while (true) {
                int randomNumber = r.nextInt(district.size());

                if (chosen[randomNumber] == true)
                    continue;
                else {
                    if (debug)
                        logger.finer("Drawing taz " + randomNumber);
                    chosenArray.add(district.get(randomNumber));
                    break;
                }
            }
        }
        //now fill up rest of district with empty TAZs
        for (int i = 0; i < (numberOfTAZs - totalToChoose); ++i)
            chosenArray.add(new DistrictTaz());
        return chosenArray;

    }

    /**
     * Return an ArrayList of unchosen TAZs
     * 
     * @param chosenTAZs An arraylist of chosen tazs
     * @param totalTAZs  An arraylist of all tazs
     * @return ArrayList An arraylist of the totalTAZs minus chosenTAZs
     */
    public ArrayList getUnchosenTAZs(ArrayList chosenTAZs, ArrayList totalTAZs) {
        ArrayList unChosenTAZs = new ArrayList();
        //set an array of booleans to true for chosen tazs
        boolean[] chosen = new boolean[totalTAZs.size()];
        for (int i = 0; i < chosenTAZs.size(); ++i) {
            DistrictTaz chosenTAZ = (DistrictTaz) chosenTAZs.get(i);
            for (int j = 0; j < totalTAZs.size(); ++j)
                if (chosenTAZ.zoneNumber == ((DistrictTaz) totalTAZs.get(j)).zoneNumber) {
                    chosen[j] = true;
                    break;
                }
        }

        for (int i = 0; i < totalTAZs.size(); ++i)
            if (chosen[i] == false)
                unChosenTAZs.add(totalTAZs.get(i));

        return unChosenTAZs;

    }

    /**
     * Add unchosen tazs to an existing arraylist
     * 
     * @param chosenTAZs  The chosen TAZs
     * @param totalTAZs   Total TAZs
     * @param allUnchosenTAZs an Arraylist of allUnChosenTAZs to add 
     *  totalTAZs - chosenTAZs to.
     */
    public void addUnchosenTAZs(ArrayList chosenTAZs, ArrayList totalTAZs, ArrayList allUnchosenTAZs) {

        if (allUnchosenTAZs == null)
            allUnchosenTAZs = new ArrayList();

        ArrayList unchosenTAZs = getUnchosenTAZs(chosenTAZs, totalTAZs);

        for (int i = 0; i < unchosenTAZs.size(); ++i)
            allUnchosenTAZs.add(unchosenTAZs.get(i));
    }

}