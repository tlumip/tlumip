package com.pb.despair.pt.estimation;
import com.pb.common.datastore.DataManager;
import com.pb.common.matrix.*;
import com.pb.common.util.InTextFile;
import com.pb.common.util.OutTextFile;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.despair.pt.DestinationChoiceLogsums;
import com.pb.despair.pt.TazData;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.File;
import java.util.*;

/** 
 * A class that creates an estimation
 * file for home location choice
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class CreatePatternEstimationFile {
     
     DataManager dm = new DataManager();  //Create a data manager, connect to default data-store

     static final int TOTALSEGMENTS = 9;
     static final String[] purposes = {"w1","w2","w3","w4","c1","c2","c3","s","r","o","b"};  //work,school,shop,recreate,other,workbased
     static final String path = new String("/temp/matrix/");
     static int SIZE=5000;
     static final boolean debug=true;
     static final int originDebug = 2761;
     static final long sampnoDebug=600287;
     
     
    public static long startTime;
    public static long endTime;
    public static long elapsedTime;

    
     TravelTimeAndCost departCost;
     TravelTimeAndCost returnCost;

    ResourceBundle ptRb;


     public CreatePatternEstimationFile(ResourceBundle appRb, ResourceBundle globalRb) throws FileNotFoundException{
                
          this.ptRb = appRb;
          //read the taz data from jDataStore; if it doesn't exist, write it to jDataStore from csv file first
          TazData taz = new TazData();
//          if(!dm.tableExists("TazData"))
            //   taz.addTazDataToJDataStore(dm);
               
          //might want to send the method the year
          taz.readData(appRb, globalRb, "tazData.file");
          
          //read the time and distance skims into memory
          if(debug) System.out.println("Reading time skims into CollapsedMatrices");
          Matrix m = (ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"pktime.zip"))).readMatrix();
          CollapsedMatrixCollection skims=new CollapsedMatrixCollection(m);
          skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"pkdist.zip")).readMatrix());
          skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"optime.zip")).readMatrix());
          skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"opdist.zip")).readMatrix());
          
          
          //now get work mode choice logsums for all 9 segments, and read them into skims HashMap class as well
          String[] mName = new String[TOTALSEGMENTS];
          
          for(int segment=0;segment<TOTALSEGMENTS;++segment){
               char thisPurpose='w';
               //create matrix for this segment
               mName[segment] = new String(path+ new Character(thisPurpose).toString() + new Integer(segment).toString()
                            + new String("ls.zip"));
                                  
               System.out.println("Reading file "+mName[segment]);
               skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(mName[segment])).readMatrix());
               
          };

          //read dc logsums from jDataStore;store them in a HashMap
          Hashtable dcLogsums = new Hashtable();
          DestinationChoiceLogsums dcl = new DestinationChoiceLogsums();
          for(int purpose=0;purpose<purposes.length;++purpose){
               for(int segment=0;segment<TOTALSEGMENTS;++segment){
               try{
                         dcl = new DestinationChoiceLogsums();
                    }catch(Exception e){
                         e.printStackTrace();  
                         System.exit(1);                                       
                  }                                                                

                    System.out.println("Reading DC LOGSUMS "+purposes[purpose]+" "+segment);
                    dcl.readFromJDataStore("DC LOGSUMS",purposes[purpose],segment);
                    dcLogsums.put((purposes[purpose]+new Integer(segment).toString()),dcl);
               }
          }
          
          //open file for writing
          OutTextFile outFile = new OutTextFile();
          PrintWriter oFile = outFile.open("/temp/patternFile.dat");
          
          //read records from data file into ArrayList - this file includes non-worker hhs, but all attributes are missing except for homeTaz
          System.out.println("Reading household records");
          ArrayList households = new ArrayList();
          InTextFile householdFile = new InTextFile();
          householdFile.open("/tlumip/development/data/household locations.dat");

          String inHousehold=new String();
          
          try{
               while((inHousehold = householdFile.readLine())!=null){
                    if(inHousehold.length()==0)
                         break;
                    HomeLocationEstimationFileRecord thisRecord = new HomeLocationEstimationFileRecord();
                    households.add(thisRecord.parse(inHousehold));
               }
               householdFile.close();
          }catch(Exception e){
               System.out.println("Error parsing household:\n"+inHousehold);
               System.exit(1);
          }
               
          //read records from data file into ArrayList
          System.out.println("Reading day-pattern records");
          InTextFile estimationFile = new InTextFile();
          estimationFile.open("/tlumip/development/data/wkday.dat");

          String inRecord=new String();
          
          try{
               while((inRecord = estimationFile.readLine())!=null){
                    if(inRecord.length()==0)
                         break;
                         
                    StringTokenizer inToken = new StringTokenizer(inRecord," ");
                    long sampno     = new Long(inToken.nextToken()).longValue();       //1 
                    System.out.println("Household "+sampno);

                    int stratum    = new Integer(inToken.nextToken()).intValue();      //2 
                    int size  = new Integer(inToken.nextToken()).intValue();  
                    inToken.nextToken();
                    inToken.nextToken();
                    int autos= new Integer(inToken.nextToken()).intValue();  
                    inToken.nextToken();
                    inToken.nextToken();
                    int income = new Integer(inToken.nextToken()).intValue();
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                    
                    int partTime =  new Integer(inToken.nextToken()).intValue();
                    int fullTime =  new Integer(inToken.nextToken()).intValue();
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                                   
                    int personNo = new Integer(inToken.nextToken()).intValue();          
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                    inToken.nextToken();
                    
                    int occupation = new Integer(inToken.nextToken()).intValue();     
                    int industry = new Integer(inToken.nextToken()).intValue();     
                    
                    if(sampno==sampnoDebug && debug){
                         System.out.print(
                              "sampno "+sampno+"\n"
                              +"stratum "+stratum+"\n"
                              +"size "+size+"\n"
                              +"autos "+autos+"\n"
                              +"income "+income+"\n"
                              +"partTime "+partTime+"\n"
                              +"fullTime "+fullTime+"\n"
                              +"personNo "+personNo+"\n"
                              +"occupation "+occupation+"\n"
                              +"industry "+industry+"\n"
                         );
                    }
                    int homeTaz=0;
                    int workTaz=0;
                    //find this record in household array
                    Iterator hhIterator = households.iterator();
                    HomeLocationEstimationFileRecord thisHousehold = new HomeLocationEstimationFileRecord();
                    boolean foundit=false;
                    while(hhIterator.hasNext()){
                         thisHousehold=(HomeLocationEstimationFileRecord)hhIterator.next();
                         if(thisHousehold.sampno==sampno && thisHousehold.stratum==stratum){
                              foundit=true;
                              break;
                         }
                    }
                    if(foundit==false){
                         System.out.println("Did not find hh "+sampno);
                    }else{
                         //set home Taz
                         homeTaz = thisHousehold.homeTaz;
                    
                         //set work Taz
                         if(personNo==thisHousehold.persno1 && personNo!=0 && personNo!=9999)
                              workTaz=thisHousehold.wrkTaz1;
                         else if(personNo==thisHousehold.persno2 && personNo!=0 && personNo!=9999)
                              workTaz=thisHousehold.wrkTaz2;
                         else if(personNo==thisHousehold.persno3 && personNo!=0 && personNo!=9999)
                              workTaz=thisHousehold.wrkTaz3;
                              
                         if(sampno==sampnoDebug && debug){
                              System.out.print(
                                   "homeTaz "+homeTaz+"\n"
                                   +"workTaz "+workTaz+"\n"
                              );
                         }
                    }
                    //calculate household segments:: work=auto/worker and nonWork =autos/persons  (0->8)
                    int workSegment=calcHouseholdSegment(autos,(partTime+fullTime),
                         income);
                    int nonWorkSegment=calcHouseholdSegment(autos,size,income);     

                    if(sampno==sampnoDebug && debug){
                         System.out.print(
                              "Work segment "+workSegment+"\n"
                              +"NonWork Segment "+nonWorkSegment+"\n"
                         );
                    }

                    //now set DC logsums
                    double schoolDcLogsum1 = 0; 
                    double schoolDcLogsum2 = 0; 
                    double schoolDcLogsum3 = 0; 
                    double shopDcLogsum    = 0; 
                    double recreateDcLogsum= 0; 
                    double otherDcLogsum   = 0; 
                    double workbDcLogsum   = 0;
                    
                    if(homeTaz!=0 && homeTaz!=9999 && taz.hasTaz(homeTaz)){
                         DestinationChoiceLogsums school1DcLogsums = (DestinationChoiceLogsums)dcLogsums.get("c1"
                              +new Integer(nonWorkSegment).toString());
                         DestinationChoiceLogsums school2DcLogsums = (DestinationChoiceLogsums)dcLogsums.get("c2"
                              +new Integer(nonWorkSegment).toString());
                         DestinationChoiceLogsums school3DcLogsums = (DestinationChoiceLogsums)dcLogsums.get("c3"
                              +new Integer(nonWorkSegment).toString());
                         DestinationChoiceLogsums shopDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("s"
                              +new Integer(nonWorkSegment).toString());
                         DestinationChoiceLogsums recreateDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("r"
                              +new Integer(nonWorkSegment).toString());
                         DestinationChoiceLogsums otherDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("o"
                              +new Integer(nonWorkSegment).toString());
                         schoolDcLogsum1 = ((Double)school1DcLogsums.logsums.get(new Integer(homeTaz))).floatValue(); 
                         schoolDcLogsum2 = ((Double)school2DcLogsums.logsums.get(new Integer(homeTaz))).floatValue(); 
                         schoolDcLogsum3 =((Double)school3DcLogsums.logsums.get(new Integer(homeTaz))).floatValue(); 
                         shopDcLogsum = ((Double)shopDcLogsums.logsums.get(new Integer(homeTaz))).floatValue(); 
                         recreateDcLogsum =  ((Double)recreateDcLogsums.logsums.get(new Integer(homeTaz))).floatValue(); 
                         otherDcLogsum = ((Double)otherDcLogsums.logsums.get(new Integer(homeTaz))).floatValue(); 
                    }
                    if(sampno==sampnoDebug && debug){
                         System.out.print(
                              "schoolDcLogsum1 "+schoolDcLogsum1+"\n"
                              +"schoolDcLogsum2 "+schoolDcLogsum2+"\n"
                              +"schoolDcLogsum3 "+schoolDcLogsum3+"\n"
                              +"shopDcLogsum "+shopDcLogsum+"\n"
                              +"recreateDcLogsum "+recreateDcLogsum+"\n"
                              +"otherDcLogsum "+otherDcLogsum+"\n"
                         );
                    }
                    
                    if(workTaz!=0 && workTaz!=9999 && taz.hasTaz(workTaz)){
                         DestinationChoiceLogsums workbDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("b"
                              +new Integer(nonWorkSegment).toString());
                         workbDcLogsum = ((Double)workbDcLogsums.logsums.get(new Integer(workTaz))).floatValue(); 
                         if(sampno==sampnoDebug && debug){
                              System.out.print(
                                   "workbDcLogsum "+workbDcLogsum+"\n"
                              );
                         }
                    }
                    
                    //set work mc logsum
                    float workMCLogsum=0;
                    float workTime=0;
                    float workDist=0;
                    
                    if(workTaz!=0 && workTaz!=9999 && homeTaz!=0 && homeTaz!=9999 && taz.hasTaz(homeTaz) && taz.hasTaz(workTaz)){
                         workMCLogsum = skims.getValue(homeTaz,workTaz,
                              path+"w"+new Integer(workSegment).toString()+"ls.zip");
                         workTime = skims.getValue(homeTaz,workTaz,
                              path+"pktime.zip");          
                         workDist = skims.getValue(homeTaz,workTaz,
                              path+"pkdist.zip");          
                              
                         if(sampno==sampnoDebug && debug){
                              System.out.print(
                                   "workMCLogsum "+workMCLogsum+"\n"
                                   +"workTime "+workTime+"\n"
                                   +"workDist "+workDist+"\n"
                              );
                         }

                    }
                    
                    //write the record
                    oFile.print(
                         inRecord+" "+
                         sampno+" "+
                         stratum+" "+
                         homeTaz+" "+
                         workTaz+" "+
                         workMCLogsum+" "+
                         workTime+" "+
                         workDist+" "+
                         schoolDcLogsum1+" "+
                         schoolDcLogsum2+" "+
                         schoolDcLogsum3+" "+
                         shopDcLogsum+" "+
                         recreateDcLogsum+" "+
                         otherDcLogsum+" "+
                         workbDcLogsum+" "+
                         "\n"
                    );
                    
               } //end while
               estimationFile.close();
          }catch(Exception e){
               System.out.println("Error somewhere");
               System.exit(1);
          }
               

          
     }; //end constructor     
     public static void main (String[] args){
          ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
          System.out.println("creating pattern choice estimation file");
          
          try{
               CreatePatternEstimationFile patternEstimation = new CreatePatternEstimationFile(rb, globalRb);
          }catch(Exception e){
               System.out.println("Error creating estimation file");
               System.exit(1);
          };
          
          System.out.println("created pattern estimation file");
          System.exit(1);
                                            
    
}; //end main


    public static void startTimer() {
        startTime = System.currentTimeMillis();
    }    

    public static void endTimer() {
        endTime = System.currentTimeMillis();
    }    

    public static long elapsedTime() {
        return (endTime-startTime);
    }    

     public int calcWorkerSegment(int occupation, int industry){
          
          int segment=4;
          if(occupation==1||(occupation==2 && industry!=7))           
               segment=1;
          else if(occupation==2 && industry==7)
               segment=2;
          else if(occupation==5||occupation==6)
               segment=3;
          
          return segment;
     }

     public int calcHouseholdSegment(int autos, int size, int income){
          
          int segment=0;
          //hh income - 0-15,15-30,30+
          boolean inclow=false;
          boolean incmed=false;
          boolean inchi=false;
          if(income>=1 && income<=3)
               inclow=true;
          else if(income>=7 && income<=13)
               inchi=true;
          else 
               incmed=true;
          
          if(inclow){
               if(autos==0) segment=0;
               else if(autos<size) segment=1;
               else segment=2;
          }
          if(incmed){
               if(autos==0) segment=3;
               else if(autos<size) segment=4;
               else segment=5;
          }
          if(inchi){
               if(autos==0) segment=6;
               else if(autos<size) segment=7;
               else segment=8;
          }
          

          return segment;
     }


}

