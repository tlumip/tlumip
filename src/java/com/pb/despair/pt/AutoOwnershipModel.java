package com.pb.despair.pt;
 
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.CollapsedMatrixCollection;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/** This class applies an auto ownership model
 *  to households in PT
 * @author Joel Freedman
 */
public class AutoOwnershipModel {
     
     public static final boolean debug = false;
     public static final long debugID = 1001;
     protected static Logger logger = Logger.getLogger("com.pb.despair.pt");
     ResourceBundle rb;
     /* Constructor takes an array
     * of households and applies the model
     */
     public AutoOwnershipModel(ResourceBundle rb){
         this.rb = rb;
     }
     
     public PTHousehold[] runAutoOwnershipModel(PTHousehold[] hh, TazData tazdata){
     	  logger.info("running auto ownership model");
          // sum total retail employment within 30 minutes transit service for each taz
          sumRetailWithin30MinutesTransit(tazdata);
          
          AutoOwnershipModelParameters p = new AutoOwnershipModelParameters();
          p.readData(rb,"autoOwnershipParameters.file");
          
          for(int i=0;i<hh.length;++i){
               
     
               if(debug && hh[i].ID==debugID){
                    System.out.println("Household "+hh[i].ID);
                    System.out.println("  Income  = "+hh[i].income);
                    System.out.println("  Size    = "+hh[i].size);
                    System.out.println("  Workers = "+hh[i].workers);
                    System.out.println("  SingleF = "+hh[i].singleFamily);
                    System.out.println("  HomeTaz = "+hh[i].homeTaz);
               }
               
               
               //set income dummy variable
               float income1=0;
               float income2=0;
               float income3=0;
               float income4=0;
               if(hh[i].income<15000)
                    income1=1;
               else if(15000<=hh[i].income&&hh[i].income<30000)
                    income2=1;
               else if(30000<=hh[i].income&&hh[i].income<60000)
                    income3=1;
               else income4=1;
               //set hhSize dummy variable
               float hhsize1=0;
                float hhsize2=0;  
                float hhsize3=0;
                float hhsize4=0;
                switch(hh[i].size){
                    case 1:
                    hhsize1=1;
                    break;
                    case 2:
                    hhsize2=1;
                    break;
                    case 3:
                    hhsize3=1;
                    break;
                    default:
                    hhsize4=1;
                    break;
                }
            //set number of workers dummy variable
            float worker0=0;
             float worker1=0;  
             float worker2=0;
             float worker3=0;
             switch(hh[i].workers){
                 case 0:
                worker0=1;
                 break;
                 case 1:
                worker1=1;
                 break;
                 case 2:
                worker2=1;
                 break;
                 default:
                worker3=1;
                 break;
             }
               //set housing type dummy variable
               int sf=0;
               if(hh[i].singleFamily)
                    sf=1;
                    
               //natural log of retail employment within 30 minutes transit
               double lnTransit=0;
               if(!tazdata.hasTaz(hh[i].homeTaz)){
                    logger.severe("Error: Taz "+hh[i].homeTaz+" not found in taz array");
                    System.exit(1);
               }
               Taz homeTaz = (Taz) tazdata.tazData.get(new Integer(hh[i].homeTaz));
               if(homeTaz.retailEmploymentWithin30MinutesTransit>0)
                    lnTransit = MathUtil.log( new Float(homeTaz.retailEmploymentWithin30MinutesTransit).doubleValue());
               
               //utilities
               double util0=p.auto0Con+ 
                            p.hhsize10*hhsize1 + 
                            p.hhsize20*hhsize2 + 
                            p.worker00*worker0 + 
                            p.worker10*worker1 + 
                            p.income10*income1 + 
                            p.income20*income2 +
                            p.sfdwell0*sf + 
                            p.tot25t0*lnTransit;
                            
               double util1=p.auto1Con + 
                            p.hhsize11*hhsize1 +
                            p.hhsize21*hhsize2 + 
                            p.hhsize31*hhsize3 +
                            p.worker01*worker0 + 
                            p.worker11*worker1 + 
                            p.income11*income1 + 
                            p.income21*income2 +
                            p.income31*income3 +
                            p.sfdwell1*sf + 
                            p.tot25t1*lnTransit;
                            
               double util2=p.auto2Con + 
                            p.hhsize22*hhsize2 + 
                            p.hhsize32*hhsize3 +
                            p.worker12*worker1 + 
                            p.worker22*worker2 + 
                            p.worker32*worker3 + 
                            p.income22*income2 + 
                            p.income32*income3 +
                            p.income42*income4 +
                            p.sfdwell2*sf;
               
               double util3=0;
          
               //exponentiated
               double expUtil0 = MathUtil.exp(util0);
               double expUtil1 = MathUtil.exp(util1);
               double expUtil2 = MathUtil.exp(util2);
               double expUtil3 = 1;
     
               //probabilities
               double sum = expUtil0 + expUtil1 + expUtil2 + expUtil3;          
               double prob0 = expUtil0/sum;
               double prob1 = expUtil1/sum;
               double prob2 = expUtil2/sum;
               double prob3 = 1 - (prob0 + prob1 + prob2);
               
               //choose one
               int autos=0;
               double randomNumber = SeededRandom.getRandom();
               if(randomNumber<prob0)
                    autos=0;
               else if(randomNumber<(prob0+prob1))
                    autos=1;
               else if(randomNumber<(prob0+prob1+prob2))
                    autos=2;
               else 
                    autos=3;               
               
               hh[i].autos=(byte)autos;
          }
//         Arrays.sort(hh); //ask joel if we truly need this line, I don't think we do (Christi: 8/19/04)
         logger.info("Completed auto ownership model.");
         //Test TODO remove when done.
        return hh;
     }


     /** Used to summarize retail employment within 30 minutes
     transit service for every taz
     */
     public void sumRetailWithin30MinutesTransit(TazData tazdata){

          String path = ResourceUtil.getProperty(rb, "skimPath.path");

          //read the skims into memory
          try{
            MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP, new File(path+"pwtivt.zip"));
            Matrix m = mr.readMatrix();
            mr = MatrixReader.createReader(MatrixType.ZIP,new File(path+"pwttwt.zip"));
            Matrix pwttwt = mr.readMatrix();            
            mr = MatrixReader.createReader(MatrixType.ZIP, new File(path+"pwtaux.zip"));
            Matrix pwtaux = mr.readMatrix();
            MatrixCollection pkwlk = new CollapsedMatrixCollection(m, false);
            pkwlk.addMatrix(pwttwt);
            pkwlk.addMatrix(pwtaux);
            
               //productions
               Enumeration pTazEnum=tazdata.tazData.elements();
               while(pTazEnum.hasMoreElements()){
                     Taz productionTaz = (Taz) pTazEnum.nextElement();
                
                     //attractions
                     Enumeration aTazEnum=tazdata.tazData.elements();
                     while(aTazEnum.hasMoreElements()){
                          Taz attractionTaz = (Taz) aTazEnum.nextElement();
                          //total time from production to this attraction
                          float totalTime=pkwlk.getValue(productionTaz.zoneNumber,attractionTaz.zoneNumber,"pwtivt")  
                              + pkwlk.getValue(productionTaz.zoneNumber,attractionTaz.zoneNumber,"pwttwt")
                              + pkwlk.getValue(productionTaz.zoneNumber,attractionTaz.zoneNumber,"pwtaux"); 
                
                          if(totalTime<=30)
                               productionTaz.retailEmploymentWithin30MinutesTransit+=
                                    attractionTaz.retail;
                     }     //end attractions
               } //end productions
          }catch(Exception e){
               logger.severe("Error:Unable to read in transit matrices in Auto Ownership Model Class");
              e.printStackTrace();
                System.exit(1);
          }

     } //end calculating retail employment within 30 minutes transit service
     public static void main(String[] args){
         ResourceBundle rb = ResourceUtil.getResourceBundle("pt");

         AutoOwnershipModel aom = new AutoOwnershipModel(rb);

        PTDataReader dataReader = new PTDataReader(rb);
        logger.info("Adding synthetic population from database"); 
        PTHousehold []households = dataReader.readHouseholds("households.file");
        
        //read the tourDestinationParameters from csv to TableDataSet
        logger.info("Reading tour destination parameters");
        TourDestinationParametersData tdpd = new TourDestinationParametersData();
        tdpd.readData(rb,"tourDestinationParameters.file");
          
        //read the stopDestinationParameters from csv to TableDataSet
        logger.info("Reading stop destination parameters");
        StopDestinationParametersData sdpd = new StopDestinationParametersData();
        sdpd.readData(rb,"stopDestinationParameters.file");
        
        //read the taz data from csv to TableDataSet
        logger.info("Adding TazData");
        TazData tazs = new TazData();
        tazs.readData(rb,"tazData.file");
        tazs.collapseEmployment(tdpd, sdpd);
        
        households = aom.runAutoOwnershipModel(households, tazs);
     }

}
