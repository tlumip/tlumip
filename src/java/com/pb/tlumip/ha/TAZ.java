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
package com.pb.tlumip.ha;

import com.pb.common.datafile.TableDataSet;
import com.pb.models.pecas.AbstractTAZ;
import com.pb.models.pecas.UnitOfLand;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * A class that represents a transport analysis zone -  a higher level amount of land.
 *
 * @author J. Abraham
 */
public class TAZ extends AbstractTAZ implements UnitOfLand {

    int zoneUserNumber;
    String zoneName;

    /**
     * private, to ensure that only one zone of each zone number is created
     */
    private TAZ(int index, int zUserNumber, String zname) {
        super(index, zUserNumber);
        //buyingCommodityZUtilities = new Hashtable();
        //sellingCommodityZUtilities = new Hashtable();
        zoneUserNumber = zUserNumber;
        zoneName = zname;
    }

    /**
     * Creates a TAZ and puts it in the global TAZ array
     */
    public static TAZ createTaz(int zoneIndex) {
        AbstractTAZ zones[] = getAllZones();
        if (zoneIndex >= zones.length || zoneIndex < 0)
            throw new Error("Need to index zones consecutively within the allocated array size");
        if (zones[zoneIndex] != null) throw new Error("Attempt to create zone with index" + zoneIndex + " more than once");
        return new TAZ(zoneIndex, zoneIndex, "");
    }

    public static TAZ createTaz(int zoneIndex, int zoneUserNumber, String zoneName) {
        AbstractTAZ zones[] = getAllZones();
        if (zoneIndex >= zones.length || zoneIndex < 0)
            throw new Error("Need to index zones consecutively within the allocated array size");
        if (zones[zoneIndex] != null) throw new Error("Attempt to create zone with index" + zoneIndex + " more than once");
        return new TAZ(zoneIndex, zoneUserNumber, zoneName);
    }

    public static void setUpZones(TableDataSet ztab) {
        class NumberName {
            String zoneName;
            int zoneNumber;

            public NumberName(int znum, String zname) {
                zoneName = zname;
                zoneNumber = znum;
            }
        }
        ;
        ArrayList tempZoneStorage = new ArrayList();
        for (int row = 1; row <= ztab.getRowCount(); row++) {
            String zoneName = ztab.getStringValueAt(row, "ZoneName");
            int zoneNumber = (int) ztab.getValueAt(row, "ZoneNumber");
            NumberName nn = new NumberName(zoneNumber, zoneName);
            tempZoneStorage.add(nn);
        }
        TAZ.createTazArray(tempZoneStorage.size());
        Iterator it = tempZoneStorage.iterator();
        int z = 0;
        while (it.hasNext()) {
            NumberName nn = (NumberName) it.next();
            TAZ.createTaz(z, nn.zoneNumber, nn.zoneName); // automatically puts it into the array based on the zone number z
            z++;
        }
    }


    public boolean equals(Object o) {
        if (!(o instanceof TAZ)) {
            return false;
        }
        TAZ other = (TAZ) o;
        if (other.zoneIndex == this.zoneIndex) {
            return true;
        }
        return false;
    }


    public int getZoneUserNumber() {
        return zoneUserNumber;
    }





/*    public static void setUpExchangesAndZUtilities() {
/*    try {  /
        Iterator comit = Commodity.getAllCommodities().iterator();
        AbstractTAZ[] abstractZones = AbstractTAZ.getAllZones();
        TAZ[] zones = new TAZ[abstractZones.length];
        for (int z=0;z<zones.length;z++) {
            zones[z] = (TAZ) abstractZones[z];
        }
/*        TableDataSet sizeTermsData = reloadTableFromScratchFromTextFile("SizeTerms");
        DataRow searchSizeTerms = new DataRow(sizeTermsData,
            new String[] { "Zone", "Commodity" }); /
        while (comit.hasNext()) {
            Commodity c = (Commodity)comit.next();
            for (int z = 0; z < zones.length; z++) {
                SellingZUtility szu = new SellingZUtility(c, zones[z]);
                BuyingZUtility bzu = new BuyingZUtility(c, zones[z]);
                szu.setDispersionParameter(c.getDefaultSellingDispersionParameter().getValue());
                bzu.setDispersionParameter(c.getDefaultBuyingDispersionParameter().getValue());
                if (c.createExchangeInProductionZone || c.createExchangeInConsumptionZone) {
                    Exchange xc = new Exchange(c, zones[z]);
/*                    searchSizeTerms.setInt("Zone", z);
                    searchSizeTerms.setString("Commodity", c.name);
                    if (!sizeTermsData.locate(searchSizeTerms, 0x20)) { // 0x20 is supposed to be
                        // com.borland.dx.dataset.Locate.FIRST but the Locate class def seems to be missing
                        throw new Error("Can't locate size term for Commodity " + c + " zone " + z);
                    }
                    xc.setBuyingSizeTerm(sizeTermsData.getDouble("BuyingST")); // read from table
                    xc.setSellingSizeTerm(sizeTermsData.getDouble("SellingST")); /
                    xc.setBuyingSizeTerm(1.0);
                    xc.setSellingSizeTerm(1.0);
                    if (c.createExchangeInProductionZone) szu.addExchange(xc);
                    if (c.createExchangeInConsumptionZone) bzu.addExchange(xc);
                }
            }
            for (int z = 0; z < zones.length; z++) {
                if (c.producerShipsToAllExchanges) {
                    CommodityZUtility czu = (CommodityZUtility)zones[z].getSellingCommodityZUtilities().get(c); // get the selling zutility again
                    czu.addAllExchanges(); // add in all the other exchanges for that commodity
                }
                if (c.consumerShipsFromAllExchanges) {
                    CommodityZUtility czu = (CommodityZUtility)zones[z].getBuyingCommodityZUtilities().get(c); // get the buying zutility again
                    czu.addAllExchanges(); // add in all the other exchanges for that commodity
                }
            }
        }
/*      } catch (DataSetException e) {
            System.err.println("Error: setUpExchangesAndUtilities()");
            e.printStackTrace();
        } /

    }     */

    //private Hashtable sellingCommodityZUtilities;

    /**
     * The buyingCommodityZUtilities is a table of the buying ZUtilities
     * for this zone.  The items are stored in a hastable, so they can be looked up by the associated commodity
     */
    //private Hashtable buyingCommodityZUtilities;

    /**
     * This gets the ZUtility for a commodity in a zone, either the selling ZUtility or the buying ZUtility.
     *
     * @param c       the commodity to get the buying or selling utility of
     * @param selling if true, get the selling utility.  Otherwise get the buying utility
     */
  /*  public double calcZUtility(Commodity c, boolean selling) throws ChoiceModelOverflowException {
        // message #1.2.3.1.3.1 to theZutilityOfACommodityInAZone:com.pb.tlumip.pi.CommodityZutility
        // double unnamed = theZutilityOfACommodityInAZone.getUtility();
        if (selling) {
            return ((Alternative) sellingCommodityZUtilities.get(c)).getUtility(1);
        } else {
            return ((Alternative) buyingCommodityZUtilities.get(c)).getUtility(1);
        }
    } */

    /**
     * This gets the ZUtility for a commodity in a zone, either the selling ZUtility or the buying ZUtility.
     * @param c the commodity to get the buying or selling utility of
     * @param selling if true, get the selling utility.  Otherwise get the buying utility
     */
/*    public double calcZUtilityForPreferences(Commodity c, boolean selling, TravelUtilityCalculatorInterface tp,
        boolean withRouteChoice) {
            // message #2.5.1 to zUtilityOfLabourOrConsumption:com.pb.tlumip.pi.CommodityZutility
            // double unnamed = zUtilityOfLabourOrConsumption.calcUtilityForPreferences(TravelPreferences);
            if (selling) {
                return ((CommodityZUtility)sellingCommodityZUtilities.get(c)).calcUtilityForPreferences(tp, withRouteChoice);
            } else {
                return ((CommodityZUtility)buyingCommodityZUtilities.get(c)).calcUtilityForPreferences(tp, withRouteChoice);
            }
    } */

    /*    public static void createTestTazAndExchange(ZoningSchemeInterface zs) {
        final int numTestZones = 10;
        final int numTestComs = 4;
        final int numGridCellsPerTypePerZone = 24;
        // set up zones
        AbstractTAZ.createTazArray(numTestZones);
        for (int i = 0; i < numTestZones; i++) {
            TAZ t = new TAZ(i,i,"");
        }
        // set up commodities
        Commodity[] coms = new Commodity[numTestComs];
        for (int i = 0; i < numTestComs; i++) {
            coms[i] = Commodity.createOrRetrieveCommodity(String.valueOf(i),'a');
            coms[i].setSellingUtilityCoefficients(1.0, 1.0, 1.0);
            coms[i].setBuyingUtilityCoefficients(1.0,1.0,1.0);
            coms[i].setDefaultBuyingDispersionParameter(new Parameter(1.0));
            coms[i].setDefaultSellingDispersionParameter(new Parameter(1.0));
        }
        // set up BuyingZUtilities, SellingZUtilities and Exchanges

        Commodity.setUpExchangesAndZUtilities("");
        AbstractTAZ[] allZonesCopy = AbstractTAZ.getAllZones();
        /* change this section of code to match RunPA
        for (int c = 0; c < numTestComs; c++) {
            for (int z = 0; z < numTestZones; z++) {
                SellingZUtility szu = new SellingZUtility(coms[c], (TAZ)allZonesCopy[z]); // constructor has to set up links in both directions
                BuyingZUtility bzu = new BuyingZUtility(coms[c], (TAZ)allZonesCopy[z]);
                szu.setDispersionParameter(0.2);
                bzu.setDispersionParameter(0.2);
                Exchange xc = new Exchange(coms[c], (TAZ)allZonesCopy[z]);
                if (c < numTestComs / 2) // half of commodities exchanged in consumption zone
                {
                    bzu.addExchange(xc);
                } else {
                    szu.addExchange(xc);
                }
            }
            for (int z = 0; z < numTestZones; z++) {
                TAZ t = (TAZ)allZonesCopy[z];
                if (c < numTestComs / 2) {
                    CommodityZUtility czu = (CommodityZUtility)t.sellingCommodityZUtilities.get(coms[c]);
                    czu.addAllExchanges();
                } else {
                    CommodityZUtility czu = (CommodityZUtility)t.buyingCommodityZUtilities.get(coms[c]);
                    czu.addAllExchanges();
                }
            }
        }
        /* end of code section to change to match RunPA



        //set up grid cells;
        Iterator it = zs.allowedDevelopmentTypes();
        DevelopmentTypeInterface dt1 = (DevelopmentTypeInterface) it.next();
        DevelopmentTypeInterface dt2 = (DevelopmentTypeInterface) it.next();
        for (int z = 0; z < numTestZones; z++) {
            for (int g = 0; g < numGridCellsPerTypePerZone; g++) {
                GridCell gc = new GridCell(allZonesCopy[z], (float)10.0, dt1, (float) 10000.0, Math.random() * 50, zs); // 10 acres, 10000
                // sq ft of space, up to 50 years old
                gc = new GridCell(allZonesCopy[z], (float) 10.0, dt2, (float) 10000.0, Math.random() * 50, zs); // 10 acres, 10000 sq ft of space, up to 50 years old
            }
        }
    } */

    /*  public void addSellingZUtility(SellingZUtility aSellingCommodityZUtility, Commodity com) {
        sellingCommodityZUtilities.put(com, aSellingCommodityZUtility);
    } */

/*    public void addBuyingZUtility(BuyingZUtility aBuyingCommodityZUtility, Commodity com) {
        buyingCommodityZUtilities.put(com, aBuyingCommodityZUtility);
    } */

/*    public static void main(String[] args) {
        createTestTazAndExchange();
    } */

    /**
     * Should rely return an immutable hashtable
     */
  /*  public Hashtable getSellingCommodityZUtilities() {
        return sellingCommodityZUtilities;
    } */

    /**
     * Should rely return an immutable hashtable
     */
 /*   public Hashtable getBuyingCommodityZUtilities() {
        return buyingCommodityZUtilities;
    } */
}

