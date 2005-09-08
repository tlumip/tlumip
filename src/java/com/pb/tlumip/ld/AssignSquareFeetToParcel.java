/*
 * Created on Dec 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.pb.osmp.ld;
import java.util.*;
import java.io.*;
import java.sql.*;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AssignSquareFeetToParcel {
    // parameters
    
    // initialization
    static Properties props = new Properties();
    static Connection conn;
    static String database;

    String zoneColumnName = null;
    String parcelIdField = null;
    PrintWriter errorLog = null;
    ResultSet sqFeetInventory;
    Hashtable sqFtTypes = new Hashtable();
    Hashtable zoningTypes = new Hashtable();
    int typeCount = 0;
    ArrayList zoneNumbers = new ArrayList();
    String sqftInventoryTableName = null;
    String matchCoeffTableName = null;
    String parcelTableName = null;
    TreeMap inventoryMap = new TreeMap();
    Hashtable parcelSorters = new Hashtable();
    String[] typeNames = null;
    Hashtable sortedParcelLists;
    
    public static void main(String[] args) {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			System.out.println("error: be sure to put the location of the properties file on the command line");
			e.printStackTrace();
			System.exit(-1);
		}
		try {
			props.load(fin);
		} catch (IOException e) {
			System.out.println("Error reading properties file "+args[0]);
			System.exit(-1);
		}
		try {
			String databaseDriver = props.getProperty("JDBCDriver");
			Class.forName(databaseDriver).newInstance();
            database = props.getProperty("Database");
            conn = DriverManager.getConnection(props.getProperty("Database"));
        } catch (Exception e) {
            System.out.println("error opening JDBC connection to database");
            System.out.println(e.toString());
            e.printStackTrace();
            System.exit(-1);
        }
        
        AssignSquareFeetToParcel mySquareFeetToParcel = new AssignSquareFeetToParcel();
        mySquareFeetToParcel.initialize();
        mySquareFeetToParcel.setUpInventory();
        mySquareFeetToParcel.setUpSorters();
        mySquareFeetToParcel.assignSquareFeet();
    }
    
    
        
   public void initialize() {     
        try {
            errorLog = new PrintWriter(new FileOutputStream(new File(props.getProperty("OutputFile"))));
        } catch (Exception e) {
            System.out.println("error opening error log file "+props.getProperty("OutputFile"));
            System.out.println("be sure to set OutputFile property in properties file");
            System.exit(-1);
        }
        
        zoneColumnName = props.getProperty("ZoneField");
        parcelIdField = props.getProperty("ParcelIdField");
        

        // some more initializers        

        sqftInventoryTableName = props.getProperty("SqftInventoryTable");
        matchCoeffTableName = props.getProperty("MatchCoeffTableName");
        parcelTableName = props.getProperty("ParcelTableName");
   }
        

   static class SizeAndChunk {
       double size;
       double chunk;
   }
   
   
   public void setUpInventory() {
            
        try {
            Statement getSqFtStatement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
            
            // get inventory of square feet to be assigned and a list of the types
            sqFeetInventory = getSqFtStatement.executeQuery("SELECT * FROM "+sqftInventoryTableName);
            while (sqFeetInventory.next()) {
                String type = sqFeetInventory.getString("PECASTYPE");
                if (!sqFtTypes.containsKey(type)) {
                    sqFtTypes.put(type,new Integer(typeCount++));
                }
                Integer zoneNumber = new Integer(sqFeetInventory.getInt(zoneColumnName));
                if (!zoneNumbers.contains(zoneNumber)) zoneNumbers.add(zoneNumber);
            }
            
            // create array of inventory of square feet in a map of double arrays mapped by zone number
            
            sqFeetInventory.first();
            double totalInventory = 0;
            typeNames = new String[typeCount];
            do  {
                String type = sqFeetInventory.getString("PECASTYPE");
                int typeIndex = ((Integer)sqFtTypes.get(type)).intValue();
                typeNames[typeIndex]=type;
                double amount = sqFeetInventory.getDouble("QUANTITY");
                double chunkSizeFromTable = sqFeetInventory.getDouble("chunksize");
                Integer zoneNum = new Integer(sqFeetInventory.getInt(zoneColumnName));
                SizeAndChunk[] inventoryForZone = (SizeAndChunk[]) inventoryMap.get(zoneNum);
                if (inventoryForZone == null) {
                    inventoryForZone = new SizeAndChunk[typeCount];
                    for (int i=0;i<typeCount;i++) {
                        inventoryForZone[i] = new SizeAndChunk();
                    }
                    inventoryMap.put(zoneNum,inventoryForZone);
                }
                inventoryForZone[typeIndex].size += amount;
                inventoryForZone[typeIndex].chunk = chunkSizeFromTable;
                
            } while (sqFeetInventory.next());
            
        } catch (Exception e) {
            System.out.println("Error in setting up square feet inventory");
            System.out.println(e);
            e.printStackTrace();
        }
   }
        
      public void setUpSorters() {
         try {
             // create parcel sorters
            Statement getMatchStatement = conn.createStatement();
            ResultSet coverageTypes = getMatchStatement.executeQuery("SELECT `PECASTYPE` FROM "+matchCoeffTableName+" GROUP BY `PECASTYPE`");
            while (coverageTypes.next()) {
                // one sorter for each coverage type
                String coverageType = coverageTypes.getString("PECASTYPE");
                ParcelScorer ps = new ParcelScorer(coverageType.charAt(0));
                Statement statement1 = conn.createStatement();
                ResultSet fieldNames = statement1.executeQuery("SELECT FIELDNAME FROM "+matchCoeffTableName+" WHERE `PECASTYPE`='"+coverageType+"' GROUP BY FIELDNAME");
                while (fieldNames.next()) {
                    // one hintlist for each field in the parcel database that we are using for a hint
                    String field = fieldNames.getString("FIELDNAME");
                    Statement statement2 = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
                    ResultSet coeffs = statement2.executeQuery("SELECT * FROM "+matchCoeffTableName+" WHERE `FIELDNAME`='"+field+"' AND `PECASTYPE`='"+coverageType+"'");
                    coeffs.last();
                    int number = coeffs.getRow();
                    coeffs.first();
                    String[] fields = new String[number];
                    double[] coeffValues = new double[number];
                    double[] farCoeffs = new double[number];
                    int fieldNum = 0;
                    do {
                        fields[fieldNum] = coeffs.getString("FIELDVALUE");
                        coeffValues[fieldNum] = coeffs.getDouble("MATCH");
                        farCoeffs[fieldNum] = coeffs.getDouble("FARTARGET");
                        fieldNum++;
                    } while(coeffs.next());
                    ps.addHint(new ParcelScorer.HintList(field,fields,coeffValues,farCoeffs));
                    
                }
                parcelSorters.put(coverageType,ps);
                
            }
         } catch (Exception e) {
             System.out.println("Error in sq feet to grid");
             System.out.println(e);
             e.printStackTrace();
         }
      }
            
     public void assignSquareFeet() {
         try {
            // now go through zones in a random order
            Collections.shuffle(zoneNumbers);
            Iterator zoneNumberIterator = zoneNumbers.iterator();
            int zoneNumber = 0;
//            while (zoneNumberIterator.hasNext()) {          
            conn = DriverManager.getConnection(database);
            Statement stmtUpdateable = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                
            if (stmtUpdateable.getMaxRows()!=0) {
                System.out.println("Max rows is set by default in statement .. attempting to remove maxrows limitation");
                stmtUpdateable.setMaxRows(0);
            }
            while (zoneNumberIterator.hasNext()) {
               zoneNumber++;
               
                // make our lists of sorted parcels
                sortedParcelLists = new Hashtable();
                Enumeration coverageTypeEnumeration = parcelSorters.keys();
                while (coverageTypeEnumeration.hasMoreElements()) {
                    String coverageType = (String) coverageTypeEnumeration.nextElement();
                    sortedParcelLists.put(coverageType,new Vector());
                }
                
                Integer taz = (Integer) zoneNumberIterator.next();
                System.out.println("Progress: now starting zone "+zoneNumber+" of "+zoneNumbers.size());
                SizeAndChunk[] inventoryForZone = (SizeAndChunk[]) inventoryMap.get(taz);
                if (inventoryForZone != null ) {
                    
                    ParcelResultSet someParcelsSQL = new ParcelResultSet(stmtUpdateable.executeQuery("SELECT *,`"+parcelIdField+"` FROM "+parcelTableName+" where `"+zoneColumnName+"`="+taz),zoneColumnName);
                    while (someParcelsSQL.getSet().next()) {
                        // get one parcel
                        Parcel p = new Parcel(someParcelsSQL);
                        Iterator parcelListsIterator = sortedParcelLists.values().iterator();
                        while (parcelListsIterator.hasNext()) {
                            // add it to each list
                            List aParcelArray = (List) parcelListsIterator.next();
                            aParcelArray.add(p);
                        }
                    }
                    // now sort each of the lists;
                    Enumeration coverageTypeIterator = sortedParcelLists.keys();
                    while (coverageTypeIterator.hasMoreElements()) {
                        Object coverageType = coverageTypeIterator.nextElement();
                        System.out.println("sorting parcels for usage "+coverageType+" in "+zoneColumnName+" "+taz);
                        List parcelList = (List) sortedParcelLists.get(coverageType);
                        ParcelScorer parcelScorer = (ParcelScorer) parcelSorters.get(coverageType);
                        Collections.sort(parcelList,parcelScorer);
                    }
                    
                    // set up the ratios so that we can go through the types maintaining the same ratios,
                    // so that a use with only a small amount of square feet doesn't get first dibs on 
                    // the best parcels in the zone
                    double[] ratiosForZone = new double[typeCount];
                    double totalInventoryForZone = 0;
                    for (int type=0;type<typeCount;type++) {
                        totalInventoryForZone += inventoryForZone[type].size; 
                    }
                    for (int type=0;type<typeCount;type++) {
                        ratiosForZone[type] = inventoryForZone[type].size/totalInventoryForZone;
                    }
                    
                    // now go through the types assigning chunks
                    
                    // TODO only assign x% the first round; also allow spillover into other TAZ's
                    double lastSquareFeetReported = -1;
                    boolean done =true;
                    do { // while !done
                        double remainingSquareFeet = 0;
                        for (int type =0;type<typeCount;type++) {
                            remainingSquareFeet += inventoryForZone[type].size;
                        }
                        done = true;
                        for (int type=0;type<typeCount;type++) {
                            while ((inventoryForZone[type].size >0) && (inventoryForZone[type].size/remainingSquareFeet >= ratiosForZone[type]-.00001)) {
                                
                                done = false; // keep going until we've assigned everything
                                String typeName = typeNames[type];
                                float amount = (float) inventoryForZone[type].chunk;
                                if (inventoryForZone[type].size<amount) {
                                    amount = (float) inventoryForZone[type].size;
                                    inventoryForZone[type].size= 0;
                                    remainingSquareFeet -= amount;
                                } else {
                                    inventoryForZone[type].size-=amount;
                                    remainingSquareFeet -= amount;
                                }
                                
                                // find the right sorted list
                                List parcelList= (List) sortedParcelLists.get(typeName);
                                if (parcelList.size()==0 ) {
                                    errorLog.println("NoSuitableParcel,"+typeName+","+taz+","+amount);
                                } else {
                                    Parcel theParcel = (Parcel) parcelList.get(parcelList.size()-1);
    
                                    // some debugging information, SACRAMENTO COUNTY SPECIFIC
                                    //Parcel firstParcel =(Parcel) parcelList.get(0);
                                    //ParcelScorer ps = (ParcelScorer) parcelSorters.get(typeName);
                                    //System.out.println("In taz "+taz+" for type "+typeNames[type]);
                                    //System.out.println("first score is "+ps.score(firstParcel)+" for parcel of type "+firstParcel.getValue("Vacancy"));
                                    //System.out.println("last score is "+ps.score(theParcel)+" for parcel of type "+theParcel.getValue("Vacancy"));
                                    char currentCoverage = theParcel.getCurrentCoverage();
                                    if (currentCoverage==' ') {
                                        theParcel.setCurrentCoverage(typeNames[type].charAt(0));
                                        currentCoverage = typeNames[type].charAt(0);
                                    } 
                                    if (currentCoverage ==  typeNames[type].charAt(0)) {
                                        theParcel.addSqFtAssigned(amount);
                                    } else {
                                        // now we have to deal with the fact that the best scored parcel
                                        // already has another coverage assigned.  The strategy is to
                                        //find another parcel with blank or current coverage and see if it's better
                                        // to put the new chunk of floorspace onto the other parcel,
                                        // or whether to put the new chunk of floorspace onto the original
                                        // best scored parcel, and swap the existing assigned floorspace
                                        // so that each parcel still only has one type of floorspace
                                        Parcel anotherParcel = null;
                                        for (int i=parcelList.size()-2;i>=0;i--) {
                                            anotherParcel = (Parcel) parcelList.get(i);
                                            char anotherCoverage = anotherParcel.getCurrentCoverage();
                                            if (anotherCoverage == ' ' || anotherCoverage == typeNames[type].charAt(0)) {
                                                break;
                                            }
                                            anotherParcel = null;
                                        }
                                        if (anotherParcel == null) {
                                            errorLog.println("NotEnoughParcelsForCoverageTypesInTaz,"+typeName+","+taz+","+amount);
                                        } else {
                                            // first try adding space to parcel without swapping coverage
                                            if (anotherParcel.getCurrentCoverage() == ' ') {
                                                anotherParcel.setCurrentCoverage(typeNames[type].charAt(0));
                                            }
                                            anotherParcel.addSqFtAssigned(amount);
                                            ParcelScorer addingTypeScorer = getScorer(typeNames[type]);
                                            ParcelScorer bumpingTypeScorer = getScorer(currentCoverage);
                                            double score1 = addingTypeScorer.score(anotherParcel) + bumpingTypeScorer.score(theParcel);
                                            
                                            // now try swapping them
                                            float inventorySwap = anotherParcel.getSqFtAssigned();
                                            anotherParcel.setCurrentCoverage(currentCoverage);
                                            anotherParcel.setSqFtAssigned(theParcel.getSqFtAssigned());
                                            theParcel.setCurrentCoverage(typeNames[type].charAt(0));
                                            theParcel.setSqFtAssigned(inventorySwap);
                                            
                                            double score2 = addingTypeScorer.score(theParcel) + bumpingTypeScorer.score(anotherParcel);
                                            if (score1 > score2) {
                                                // ok, the first option is better, put it back
                                                theParcel.setSqFtAssigned(anotherParcel.getSqFtAssigned());
                                                theParcel.setCurrentCoverage(currentCoverage);
                                                anotherParcel.setSqFtAssigned(inventorySwap);
                                                anotherParcel.setCurrentCoverage(typeNames[type].charAt(0));
                                                
                                            }
                                            
                                            rescoreParcel(anotherParcel);
                                            
                                        }
                                    }
                                    
                                    // now the parcel has changed, it needs to be reinserted into each of the sorted lists
                                    rescoreParcel(theParcel);
                                    
                                }
                            }
                        }
                        if (lastSquareFeetReported == -1) {
                            lastSquareFeetReported = remainingSquareFeet;
                            System.out.println("******************************************");
                            System.out.println("Assigning "+remainingSquareFeet+" in "+zoneColumnName+" "+taz);
                        } else if (lastSquareFeetReported-remainingSquareFeet>10000) {
                            System.out.println(remainingSquareFeet+" sqft of buildings still to be assigned");
                            lastSquareFeetReported = remainingSquareFeet;
                        }
                    } while (!done);
                    someParcelsSQL.close();
                }
                
                // delete list of parcels to bypass JDBC memory leak -- hope this helps!
                //Iterator myIterator = sortedParcelLists.values().iterator();
                //while (myIterator.hasNext()) {
                //    List parcelList = (List) myIterator.next();
                //    Iterator myParcelIterator= parcelList.iterator();
                //    while(myParcelIterator.hasNext()) {
                //        Parcel aParcel = (Parcel) myParcelIterator.next();
                //        aParcel.set=null;
                //    }
                //    parcelList.clear();
               // }
               // sortedParcelLists.clear();
                
                // this is necessary to delete cache of scores
                Iterator myIterator = parcelSorters.values().iterator();
                while(myIterator.hasNext()) {
                    ParcelScorer ps = (ParcelScorer) myIterator.next();
                    ps.clearScoreRecord();
                }
                
                
             //used to remake these for each zone   
             //   stmtUpdateable.close();
             //   conn.close();
           }
           errorLog.flush();
           errorLog.close();
        } catch (Exception e) {
            System.out.println("Error in sq feet to grid");
            System.out.println(e);
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            System.out.println("Error in sq feet to grid");
            System.out.println(e);
            e.printStackTrace();
        }
	}
     
    /**
     * @param currentCoverage
     * @return
     */
    private ParcelScorer getScorer(char coverageCode) {
        String coverageName = null;
        for (int i=0;i<typeNames.length;i++) {
            if (typeNames[i].charAt(0) == coverageCode) {
                // just a check for duplicates
                if (coverageName !=null) {
                    throw new RuntimeException("Coverage "+coverageName+" and "+typeNames[i]+" both start with the same character...error");
                }
                coverageName = typeNames[i];
            }
        }
        return getScorer(coverageName);
    }



    /**
     * @param string
     * @return
     */
    private ParcelScorer getScorer(String string) {
        return (ParcelScorer) parcelSorters.get(string);
    }



    public void rescoreParcel(Parcel theParcel) {
     Enumeration coverageTypeIterator = sortedParcelLists.keys();
     while (coverageTypeIterator.hasMoreElements()) {
         Object coverageType = coverageTypeIterator.nextElement();
         List parcelList = (List) sortedParcelLists.get(coverageType);
         ParcelScorer parcelScorer = (ParcelScorer) parcelSorters.get(coverageType);
         int size = parcelList.size();
         if (!parcelList.remove(theParcel)){
             throw new Error("Can't remove "+theParcel+" from list "+parcelList);
         }
         if (parcelList.remove(theParcel)) {
             throw new Error("Parcel "+theParcel+" was in list "+parcelList+" more than once!");
         }
         int location = Collections.binarySearch(parcelList,theParcel,parcelScorer);
         if (location>=0) {
             parcelList.add(location,theParcel);
         } else {
             parcelList.add(-(location+1),theParcel);
         }
         if (parcelList.size()!=size) {
             throw new Error(parcelList+" is changing size from "+size+" to "+parcelList.size());
         }
     }
    }
}
