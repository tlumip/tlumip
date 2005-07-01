package com.pb.tlumip.model;


import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Author: willison
 * Date: Nov 23, 2004
 * <p/> This class will map the alphazones and beta zones
 * to their respective MPO's for data summary purposes.  The
 * correspondence has been defined in the alpha2beta.csv file
 * which will need to have been read in by the calling class as a TableDataSet.
 * The TableDataSet will be able to give you the arrays necessary for the class
 * to be constructed.  This class is similar to the AlphaToBeta class that resides
 * in the com.pb.common.matrix package.
 *
 * Created by IntelliJ IDEA.
 */
public class MPOMapping {
    static Logger logger = Logger.getLogger("com.pb.tlumip.model");

    HashMap azonesByMPO;
    HashMap bzonesByMPO;

    String[] azoneToMPOLookUp;
    String[] bzoneToMPOLookUp;

    private MPOMapping(){}

    public MPOMapping(int[] azones, int[] bzones, String[] mpos){
        //first check to make sure that your arrays are all the same size.  If they aren't we have to
        //exit and figure out why.
        if(azones.length != bzones.length && azones.length != mpos.length){
            logger.fatal("azone, bzone and mpo arrays must be the same length - mapping can not be defined");
            System.exit(10);
        }
        azonesByMPO = new HashMap();
        bzonesByMPO = new HashMap();

        //create mapping
        for(int i=0; i< mpos.length; i++){

            String mpo = mpos[i];

            if(!azonesByMPO.containsKey(mpo)){ //create an entry in both hashmaps
                TreeSet azs = new TreeSet();   //will not repeat objects and keeps them in order.
                TreeSet bzs = new TreeSet();

                azs.add(new Integer(azones[i]));
                bzs.add(new Integer(bzones[i]));

                azonesByMPO.put(mpo, azs);
                bzonesByMPO.put(mpo, bzs);

            } else { // entry already exists, get the array lists and add the current azone and bzone to them.

                TreeSet azs = (TreeSet) azonesByMPO.get(mpo);
                TreeSet bzs = (TreeSet) bzonesByMPO.get(mpo);

                azs.add(new Integer(azones[i]));
                bzs.add(new Integer(bzones[i]));

            }
        }

        //create look-ups
        int maxAZone = getMaxZoneNumber(azones);
        int maxBZone = getMaxZoneNumber(bzones);
        azoneToMPOLookUp = new String[maxAZone + 1];
        bzoneToMPOLookUp = new String[maxBZone + 1];
        for(int i=0; i< azones.length; i++){
            azoneToMPOLookUp[azones[i]] = mpos[i];
            bzoneToMPOLookUp[bzones[i]] = mpos[i];
        }



    }

    public int[] getAllAlphaZonesInMPO( String mpo){
        int[] azones = getZonesInMPO("alpha",mpo);
        return azones;
    }

    public int[] getAllBetaZonesInMPO( String mpo){
        int[] bzones = getZonesInMPO("beta", mpo );
        return bzones;
    }

    private int getMaxZoneNumber(int[] array){
        int largest = array[0];
        for(int i=1;i<array.length;i++){
            if(array[i]>largest)
                largest = array[i];
        }
    	return largest;
    }

    private int[] getZonesInMPO (String alphaOrBeta, String mpo){
        TreeSet zones;
        int[] zoneArray;
        if(alphaOrBeta.equalsIgnoreCase("alpha")){
            zones = (TreeSet) azonesByMPO.get(mpo);
        }else {
            zones = (TreeSet) bzonesByMPO.get(mpo);
        }
        zoneArray = new int[zones.size()];
        int count = 0;
        Iterator iterator = zones.iterator();
        while(iterator.hasNext()){
            zoneArray[count] = ((Integer)iterator.next()).intValue();
            count++;
        }
        return zoneArray;
    }

    public static void main(String[] args){
        int[] azones = {1,2,3,4,5,6,7,8,9,13};
        int[] bzones = {1,1,5,5,6,6,7,8,15,1000};
        String[] mpos = {"mpo1", "mpo2", "mpo3", "mpo4", "mpo4",
                         "mpo4", "mpo4", "mpo5", "mpo6", "mpo6"};

        MPOMapping map = new MPOMapping(azones, bzones,mpos);

        for (int nMPOs = 0; nMPOs < mpos.length; nMPOs++) {
            logger.info("Here is a list of all alphaZones in " +  mpos[nMPOs] );
            int[] azonesInMPO = map.getAllAlphaZonesInMPO(mpos[nMPOs]);
            for(int i=0; i< azonesInMPO.length; i++){
                logger.info("\t" + azonesInMPO[i]);
            }
        }

        for (int nMPOs = 0; nMPOs < mpos.length; nMPOs++) {
            logger.info("Here is a list of all betaZones in " +  mpos[nMPOs] );
            int[] bzonesInMPO = map.getAllBetaZonesInMPO(mpos[nMPOs]);
            for(int i=0; i< bzonesInMPO.length; i++){
                logger.info("\t" + bzonesInMPO[i]);
            }
        }

        logger.info("Alpha Zone 13 is in Zone " + map.azoneToMPOLookUp[13]);
        if(map.azoneToMPOLookUp[13] == "mpo6") logger.info("Which is correct");
        else logger.info("But it should be in mpo6");
    }

}
