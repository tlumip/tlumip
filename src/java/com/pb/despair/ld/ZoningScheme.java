package com.pb.despair.ld;

import com.pb.common.datafile.TableDataSet;
import com.pb.models.pecas.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;


/**
 * A class that represents a the allowed zoning in a Grid Cell
 * @author John Abraham
 */
public class ZoningScheme implements ZoningSchemeInterface {

    //TODO: read in current year-1990
    public static int currentYear = 100;
    public static final int maxZoningSchemeIndex = 32767;
    
    

    
    static final double interestRate = 0.0722;
    static final double compounded = Math.pow(1+interestRate,30);
    static final double amortizationFactor = interestRate *compounded/(compounded -1);
    static final double rentCoefficient = 1.0;
    static final double profitCoefficient = 1.0;
    static final double vacancyCoefficient = 1.0 * 9691;
    public static double developmentDispersionParameter;
    public static  double developmentAlternativesDispersionParameter;
    static LandInventory land = null;
    
    // these five variables are used in the nested classes in the logit model, 
    // so they need to be set before the logitmodel is used.
    DevelopmentType existingDT;
    long row;
    long col;
    double gridFee;


    private class DevelopmentAlternative implements Alternative {
        DevelopmentType dt;
        final ZoningRegulation zoningReg;
        double sizeTerm; // size term for change alternatives.

        DevelopmentAlternative(DevelopmentType dt) {
            this.dt = dt;
            zoningReg = (ZoningRegulation) zoning.get(dt);
        }
        
        public double getUtility(double higherLevelDispersionParameter) {
           
            // build up utility from parts.
            
            // first part is revenue
            double price = land.getPrice(row,col,dt.getGridCode());
            double profit = (price-dt.getMaintenanceCost())*getAllowedFAR(dt)*land.getSize(row,col);
            
            // TODO: remove this debug println
            if (profit>0 && numProfitPrints >0) {
                numProfitPrints --;
                System.out.println("Profit:"+profit+" price:"+price+" for DT "+dt+" at FAR "+getAllowedFAR(dt));
            }
            
            
            // next is construction cost
            // TODO: add in service attribute
            double serviceCost = 0.0; // should use service grid attribute but don't have that yet
            // TODO: add in gridfee and make sure we only *get* it once from the grid file
            double siteCost = 0.0; // supposed to use slope, bedrock, watertable formula
            
            double buildingCost = dt.getConstructionCost() * zoningReg.maxFAR * land.getSize(row,col);
            
            double amortizedConstructionCost = amortizationFactor*(serviceCost + gridFee + zoningReg.fee + siteCost +  buildingCost);
            
            double vacancy = land.getLocalVacancyRate(row,col,dt.getGridCode(),1.0);
            
            double overallCostBenefit = profitCoefficient*(profit - amortizedConstructionCost) +
                    vacancyCoefficient * vacancy ;
            return overallCostBenefit/land.getSize(row,col)+ dt.getNewASC() + existingDT.getTransitionCoefficient(dt);
            
            
        }
        
        private void doDevelopment() {
            float newSquareFeet = (float) (zoningReg.maxFAR * land.getSize(row,col) * Math.random());
            float oldSquareFeet = land.getQuantity(row,col);
            int oldDT = land.getCoverage(row,col);
            land.putCoverage(row,col,dt.getGridCode());
            land.putQuantity(row,col,newSquareFeet);
            int oldYear = land.getYearBuilt(row,col);
            land.putYearBuilt(row,col,currentYear);
            try {
                developmentLog.write("C,"+land.elementToString(row,col)+","+name+","+((char) oldDT)+","+dt.getGridCode()+","+oldSquareFeet+","+newSquareFeet+","+oldYear+","+currentYear+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private class DemolishAlternative implements Alternative {
        DevelopmentType dt;
        double sizeTerm; // size term for change alternatives.

        DemolishAlternative(DevelopmentType vacantType) {
            this.dt = vacantType;
        }
        
        public double getUtility(double higherLevelDispersionParameter) {
            if (existingDT == dt) return Double.NEGATIVE_INFINITY;

            return existingDT.getAddASC() + existingDT.getTransitionCoefficient(dt);
            
            
        }
        
        private void doDevelopment() {
            float newSquareFeet = 0;
            float oldSquareFeet = land.getQuantity(row,col);
            int oldDT = land.getCoverage(row,col);
            land.putCoverage(row,col,dt.getGridCode());
            land.putQuantity(row,col,newSquareFeet);
            int oldYear = land.getYearBuilt(row,col);
            land.putYearBuilt(row,col,currentYear);
            try {
                developmentLog.write("D,"+land.elementToString(row,col)+","+name+","+((char) oldDT)+","+dt.getGridCode()+","+oldSquareFeet+","+newSquareFeet+","+oldYear+","+currentYear+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    // TODO: remove this debug print counter
    static int numProfitPrints = 1000;

    private class DevelopMoreAlternative implements Alternative {
        DevelopmentType dt;
        ZoningRegulation zoningReg;

        DevelopMoreAlternative() {
        }
        
        
        public double getUtility(double higherLevelDispersionParameter) {
            dt = existingDT;
            if (existingDT.isVacant()) return Double.NEGATIVE_INFINITY;
            zoningReg = (ZoningRegulation) zoning.get(dt);
            if (zoningReg == null) return Double.NEGATIVE_INFINITY;
           
            float existingSpace = land.getQuantity(row,col);
            float moreSpace = (float) (getAllowedFAR(dt)*land.getSize(row,col) - existingSpace);
            if (moreSpace <=0) return Double.NEGATIVE_INFINITY;
            
            // build up utility from parts.
            
            // first part is revenue
            double price = land.getPrice(row,col,dt.getGridCode());
            double profit = (price-dt.getMaintenanceCost())*moreSpace;
            if (profit>0 && numProfitPrints >0) {
                numProfitPrints --;
                System.out.println("Profit:"+profit+" price:"+price+" for DT "+dt+" at FAR "+getAllowedFAR(dt));
            }
            
            int age = currentYear - land.getYearBuilt(row,col);
            
            double rent = (price*dt.getRentDiscountFactor(age)-dt.getMaintenanceCost())*existingSpace;
            if (rent >0 && numProfitPrints >0) {
                System.out.println("Rent:"+rent+" for existing "+existingSpace+" sqft");
            }
            
            // next is construction cost
            // TODO: add in service attribute
            double serviceCost = 0.0; // should use service grid attribute but don't have that yet
            // TODO: add in gridfee and make sure we only *get* it once from the grid file
            double siteCost = 0.0; // supposed to use slope, bedrock, watertable formula
            
            double buildingCost = dt.getConstructionCost() * zoningReg.maxFAR * land.getSize(row,col);;
            
            double amortizedConstructionCost = amortizationFactor*(serviceCost + gridFee + zoningReg.fee + siteCost +  buildingCost);
            
            double proRatedACC = amortizedConstructionCost *moreSpace/getAllowedFAR(dt)/land.getSize(row,col);
            
            double vacancy = land.getLocalVacancyRate(row,col,dt.getGridCode(),1.0);
            
            double overallCostBenefit = profitCoefficient*(profit - proRatedACC) + rentCoefficient*rent +
                    vacancyCoefficient * vacancy ;
            return overallCostBenefit/land.getSize(row,col)+ dt.getAddASC();
        }

        private void doDevelopment() {
            float oldSquareFeet = land.getQuantity(row,col);
            float newSquareFeet = oldSquareFeet;
            if (zoningReg.maxFAR * land.getSize(row,col) > oldSquareFeet) {
                float landSize = land.getSize(row,col);
                newSquareFeet = (float) ((zoningReg.maxFAR * landSize  - oldSquareFeet)* Math.random() + oldSquareFeet);
            }
            land.putQuantity(row,col,newSquareFeet);
            int oldYear = land.getYearBuilt(row,col);
            int newYear = (int) ((oldYear * oldSquareFeet + currentYear*(newSquareFeet - oldSquareFeet))/newSquareFeet);
            land.putYearBuilt(row,col,newYear);
            try {
                developmentLog.write("A,"+land.elementToString(row,col)+","+name+","+existingDT.getGridCode()+","+existingDT.getGridCode()+","+oldSquareFeet+","+newSquareFeet+","+oldYear+","+newYear+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };



    private class NoChangeAlternative implements Alternative {
        public double getUtility(double higherLevelDispersionParameter) {
            if (existingDT.isVacant()) return 0;
            float existingSpace = land.getQuantity(row,col);

            double price = land.getPrice(row,col,existingDT.getGridCode());
            int age = currentYear - land.getYearBuilt(row,col);
            
            float rent = (float) (price*existingDT.getRentDiscountFactor(age)-existingDT.getMaintenanceCost())*existingSpace;

            float vacancy = (float) (land.getLocalVacancyRate(row,col,existingDT.getGridCode(),1.0));
            
 
            double overallCostBenefit = rentCoefficient*rent +
                    vacancyCoefficient * vacancy;
            return overallCostBenefit/land.getSize(row,col);
        }
 
    };


    
    public void doDevelopment(LandInventory l, long id1, long id2) {
        
        if (zoning.size() > 0) // if we're not zoned for anything then can't do anything at all.
        {
            synchronized(this) {
                land = l;
                this.row =id1;
                this.col = id2;
                existingDT = DevelopmentType.getAlreadyCreatedDevelopmentByCode(land.getCoverage(row,col));
                boolean doIt = land.isDevelopable(row,col);
                if (!doIt) return; // don't do development if it's impossible to develop!
                // TODO: add in grid fee grid attribute
                gridFee = 0.0;
                LogitModel developChoice = getMyLogitModel();
                Alternative a;
                try {
                    a = developChoice.monteCarloElementalChoice();
                } catch (NoAlternativeAvailable e) {
                    throw new Error("no reasonable development choices available for " + this);
                } catch (ChoiceModelOverflowException e) {
                    e.printStackTrace();
                    throw new Error("overflow trying to find reasonable development choices available for " + this);
                }
                
                if (a instanceof DevelopmentAlternative) {
                    ((DevelopmentAlternative) a).doDevelopment();
                }
                if (a instanceof DevelopMoreAlternative) {
                    ((DevelopMoreAlternative) a).doDevelopment();
                }
                if (a instanceof DemolishAlternative) {
                    ((DemolishAlternative) a).doDevelopment();
                }
            }
        }
    }


    LogitModel myLogitModel = null;
    
    private LogitModel getMyLogitModel() {
        if (myLogitModel != null) return myLogitModel;
        myLogitModel = new LogitModel();
        myLogitModel.setDispersionParameter(developmentDispersionParameter);
        Alternative noChange = new NoChangeAlternative();
        myLogitModel.addAlternative(noChange);
        LogitModel changeOptions = new LogitModel();
        Iterator it = allowedDevelopmentTypes();
        while (it.hasNext()) {
            DevelopmentType whatWeCouldBuild = (DevelopmentType)it.next();
            Alternative aDevelopmentType;
            if (whatWeCouldBuild.isVacant()) aDevelopmentType = new DemolishAlternative(whatWeCouldBuild);
            else aDevelopmentType = new DevelopmentAlternative(whatWeCouldBuild); // rebuilding entirely
            changeOptions.addAlternative(aDevelopmentType);
        }
        changeOptions.addAlternative(new DevelopMoreAlternative());
        changeOptions.setDispersionParameter(developmentAlternativesDispersionParameter);
        myLogitModel.addAlternative(changeOptions);
        return myLogitModel;
    }
        

    public String getName() {return name;}
    
    final short gridCode;

    public void allowDevelopmentType(DevelopmentTypeInterface dt, double maxFloorAreaRatio, float fee) {
        if (!zoning.contains(dt)) {
            zoning.put(dt,
                new ZoningRegulation(maxFloorAreaRatio,fee));
        }
        myLogitModel = null;
    }

//    public void allowDevelopmentType(DevelopmentTypeInterface dt, double maxFloorAreaRatio) {
//        allowDevelopmentType(dt,maxFloorAreaRatio,0);
//    }

    public void noLongerAllowDevelopmentType(DevelopmentTypeInterface dt) {
        if (zoning != null) {
            zoning.remove(dt);
        }
        myLogitModel = null;
    }

    /**
     * The list of DevelopmentTypes that are allowed in the cell
     * associates <{DevelopmentType}>
     *
     * supplierCardinality *
     */
    protected final Hashtable zoning;
    
    private static class ZoningRegulation {
        double maxFAR;
        double fee;
        
        ZoningRegulation(double maxFAR, double fee) {
            this.maxFAR = maxFAR;
            this.fee = fee;
        }
    }    

    private static Hashtable allZoningSchemes = new Hashtable();
    private static ZoningScheme[] allZoningSchemesIndexArray = new ZoningScheme[maxZoningSchemeIndex];
    
    public static ZoningScheme getAlreadyCreatedZoningScheme(String name) {
       ZoningScheme zs = (ZoningScheme) allZoningSchemes.get(name);
       return zs;
    }
    
    

    public final String name;

    private ZoningScheme(String zoningSchemeName, short gridCodeValue) {
       gridCode = gridCodeValue;
       if (allZoningSchemes.get(zoningSchemeName)!=null)  throw new Error("Error: tried to create zoning scheme twice "+zoningSchemeName+" !");
       if (allZoningSchemesIndexArray[gridCodeValue] !=null) throw new Error("Error: tried to create zoning scheme twice - CODE:"+gridCodeValue);
       zoning = new Hashtable();
       name = zoningSchemeName;
       allZoningSchemes.put(zoningSchemeName,this);
       allZoningSchemesIndexArray[gridCodeValue] = this;
    }

    public static void setUpZoningSchemes(TableDataSet ztab) {
        for(int r=1;r<ztab.getRowCount();r++) {
            String zoningSchemeName = ztab.getStringValueAt(r,"ZoningScheme");
            short gridCodeValue = (short)ztab.getValueAt(r,"GridCode");
            ZoningScheme zs = getAlreadyCreatedZoningScheme(zoningSchemeName);
            if (zs == null) {
                zs = new ZoningScheme(zoningSchemeName,gridCodeValue);
            }
            DevelopmentTypeInterface dt = DevelopmentType.getAlreadyCreatedDevelopmentType(ztab.getStringValueAt(r,"AllowedDevelopmentType"));
            if (dt == null) throw new Error("Error: undefined development type " + ztab.getStringValueAt(r,"AllowedDevelopmentType") + " in ZoningSchemes.csv");
            float maxFar = ztab.getValueAt(r,"MaximumFAR");
            float fee = ztab.getValueAt(r,"Fee");
            zs.allowDevelopmentType(dt,maxFar,fee);
            }
     }

    public double getAllowedFAR(DevelopmentTypeInterface dt) {
        ZoningRegulation reg = (ZoningRegulation) zoning.get(dt);
        if (reg == null) return 0;
        return (float) reg.maxFAR;
    }

    public Iterator allowedDevelopmentTypes() {
        return zoning.keySet().iterator();
    }
    
    public int size() {return zoning.size();}

    /**
     * Method openGridFiles.
     */
    public static void openLogFile(String gridPath) {
        try {
            developmentLog = new BufferedWriter(new FileWriter(gridPath+"developmentEvents.csv"));
            developmentLog.write("EventType,Id1,Id2,Zoning,OldType,NewType,OldSqft,NewSqft,OldYrBuilt,NewYrBuilt\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * Method openGridFiles.
     */
    public static void closeLogFile() {
        try {
            developmentLog.close();
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }

    private static BufferedWriter developmentLog;


    /**
     * Method getZoningSchemeByIndex.
     * @param i
     * @return ZoningScheme
     */
    public static ZoningScheme getZoningSchemeByIndex(int i) {
        return allZoningSchemesIndexArray[i];
    }


    /**
     * Method flushDevelopmentLog.
     */
    public static void flushDevelopmentLog() {
        try {
            developmentLog.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param zoneNumber
     * @param parcel
     */
    public static void logBadZoning(long zoneNumber, long parcel) {
        char coverage = land.getCoverage(zoneNumber,parcel);
        float quantity = land.getQuantity(zoneNumber,parcel);
        int yearBuilt = land.getYearBuilt(zoneNumber,parcel);
        try {
            developmentLog.write("X,"+land.elementToString(zoneNumber,parcel)+","+land.getZoning(zoneNumber,parcel)+","+coverage+","+coverage+","+quantity+","+quantity+","+yearBuilt+","+yearBuilt+"\n");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
