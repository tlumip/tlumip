package com.pb.despair.model;

import com.pb.common.util.ResourceUtil;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.matrix.CollapsedMatrixCollection;

import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.io.Serializable;
 


/** A class that creates mode choice logsums by
 *  9 market segments for all trip purposes
 * @author Joel Freedman
 */
public class SkimsInMemory implements Serializable {
	
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");	
    public MatrixCollection pkwlk,pkdrv,opwlk,opdrv;
    public Matrix pkTime,pkDist,opTime,opDist;
	public static int AOC = 12;
	public static int WALKMPH=3;
	public static int BIKEMPH=12;
	public static int FIRSTWAITSEGMENT=10;
	public static int DRIVETRANSITMPH=25;
    static final int MAX_SEQUENTIAL_TAZ=3000;     
    static final int MAXZONENUMBER=5000;     
    public static String[] mNameGlobal;
    TravelTimeAndCost tc = new TravelTimeAndCost();
    
     public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        SkimsInMemory skims= new SkimsInMemory();
        skims.readSkims(rb);
        skims.checkSkims(skims);
     }
    public void checkSkims(SkimsInMemory skims) { 
    
        int ptaz=1;
        int ataz=1;
        
        //check 1    
        logger.info("PTAZ "+ptaz+ " ATAZ "+ataz);
        float pkwtivt = skims.pkwlk.getValue(ptaz,ataz,"pwtivt");
        float pkdtfwt = skims.pkdrv.getValue(ptaz,ataz,"pdtfwt");
        float pktimet = skims.pkTime.getValueAt(ptaz,ataz);       
        logger.info("pk walk time "+pkwtivt);
        logger.info("pk drive fwait "+pkdtfwt);       
        logger.info("pkTime "+pktimet);
        ptaz=1;
        ataz=2;
        
        //check 2    
        logger.info("PTAZ "+ptaz+ " ATAZ "+ataz);
        pkwtivt = skims.pkwlk.getValue(ptaz,ataz,"pwtivt");
        pkdtfwt = skims.pkdrv.getValue(ptaz,ataz,"pdtfwt");       
        pktimet = skims.pkTime.getValueAt(ptaz,ataz);  
        logger.info("pk walk time "+pkwtivt);
        logger.info("pk drive fwait "+pkdtfwt);       
        logger.info("pkTime "+pktimet);

        //check 3
        ptaz=1;
        ataz=1240;
        logger.info("PTAZ "+ptaz+ " ATAZ "+ataz);
        pkwtivt = skims.pkwlk.getValue(ptaz,ataz,"pwtivt");
        pkdtfwt = skims.pkdrv.getValue(ptaz,ataz,"pdtfwt");     
        pktimet = skims.pkTime.getValueAt(ptaz,ataz);  
        logger.info("pk walk time "+pkwtivt);
        logger.info("pk drive fwait "+pkdtfwt);
        logger.info("pkTime "+pktimet);
        
        //check 4
        ptaz=2;
        ataz=3;
        logger.info("PTAZ "+ptaz+ " ATAZ "+ataz);
        pkwtivt = skims.pkwlk.getValue(ptaz,ataz,"pwtivt");
        pkdtfwt = skims.pkdrv.getValue(ptaz,ataz,"pdtfwt");  
        pktimet = skims.pkTime.getValueAt(ptaz,ataz);     
        logger.info("pk walk time "+pkwtivt);
        logger.info("pk drive fwait "+pkdtfwt);
        logger.info("pkTime "+pktimet);
        logger.info(" ");
     }
     
	public SkimsInMemory() {
    }
    //createSkimsMatrices (6 file inputs)
    public MatrixCollection createSkimsMatrices(File ivt, File fwt, File twt, File aux, File brd, File far){
        
        MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP, ivt);
        Matrix m = mr.readMatrix();     
        MatrixReader fwtmr = MatrixReader.createReader(MatrixType.ZIP, fwt);
        Matrix fwtm = fwtmr.readMatrix();   
        MatrixReader twtmr = MatrixReader.createReader(MatrixType.ZIP, twt);
        Matrix twtm = twtmr.readMatrix();     
        MatrixReader auxmr = MatrixReader.createReader(MatrixType.ZIP, aux);
        Matrix auxm = auxmr.readMatrix();      
        MatrixReader brdmr = MatrixReader.createReader(MatrixType.ZIP,brd);
        Matrix brdm = brdmr.readMatrix();    
        MatrixReader farmr = MatrixReader.createReader(MatrixType.ZIP, far);
        Matrix farm = farmr.readMatrix();                 
        MatrixCollection mc = new CollapsedMatrixCollection(m, false);
        mc.addMatrix(fwtm);  
        mc.addMatrix(twtm);         
        mc.addMatrix(auxm);      
        mc.addMatrix(brdm);      
        mc.addMatrix(farm);    
        return mc;
    }
    //createSkimsMatrices (7 file inputs)
    public MatrixCollection createSkimsMatrices(File ivt, File fwt, File twt, File aux, File brd, File drv, File far){
        
        MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP, ivt);
        Matrix m = mr.readMatrix();  
        MatrixReader fwtmr = MatrixReader.createReader(MatrixType.ZIP, fwt);
        Matrix fwtm = fwtmr.readMatrix();     
        MatrixReader twtmr = MatrixReader.createReader(MatrixType.ZIP, twt);
        Matrix twtm = twtmr.readMatrix();       
        MatrixReader auxmr = MatrixReader.createReader(MatrixType.ZIP, aux);
        Matrix auxm = auxmr.readMatrix();   
        MatrixReader brdmr = MatrixReader.createReader(MatrixType.ZIP,brd);
        Matrix brdm = brdmr.readMatrix();  
        MatrixReader drvmr = MatrixReader.createReader(MatrixType.ZIP,drv);
        Matrix drvm = drvmr.readMatrix();              
        MatrixReader farmr = MatrixReader.createReader(MatrixType.ZIP, far);
        Matrix farm = farmr.readMatrix();                 
        MatrixCollection mc = new CollapsedMatrixCollection(m, false);
        mc.addMatrix(fwtm);  
        mc.addMatrix(twtm);         
        mc.addMatrix(auxm);     
        mc.addMatrix(brdm);  
        mc.addMatrix(drvm);       
        mc.addMatrix(farm);   
        return mc;
    }
    public void readSkims(ResourceBundle rb){
		
          String hwyPath = ResourceUtil.getProperty(rb, "hwySkims.path");
          String transitPath = ResourceUtil.getProperty(rb, "transitSkims.path");
          logger.info("Hwy Skim Path = "+hwyPath);
          logger.info("Transit Skim Path = " + transitPath);
          
		//read skims (this part should eventually be changed to read the data from jds)
        String[] mName = {hwyPath+"pktime.zip", //0
           hwyPath+ "pkdist.zip",     //1
           hwyPath+ "optime.zip",     //2
           hwyPath+ "opdist.zip",     //3
           transitPath+ "pwtivt.zip",     //4
           transitPath+ "pwtfwt.zip",     //5
           transitPath+ "pwttwt.zip",     //6
           transitPath+ "pwtaux.zip",     //7
           transitPath+ "pwtbrd.zip",     //8
           transitPath+ "pwtfar.zip",     //9
           transitPath+ "owtivt.zip",     //10
           transitPath+ "owtfwt.zip",     //11
           transitPath+ "owttwt.zip",     //12
           transitPath+ "owtaux.zip",     //13
           transitPath+ "owtbrd.zip",     //14
           transitPath+ "owtfar.zip",     //15
           transitPath+ "pdtivt.zip",     //16
           transitPath+ "pdtfwt.zip",     //17
           transitPath+ "pdttwt.zip",     //18
           transitPath+ "pdtwlk.zip",     //19
           transitPath+ "pdtbrd.zip",     //20
           transitPath+ "pdtdrv.zip",     //21
           transitPath+ "pdtfar.zip",     //22
           transitPath+ "odtivt.zip",     //23
           transitPath+ "odtfwt.zip",     //24
           transitPath+ "odttwt.zip",     //25
           transitPath+ "odtwlk.zip",     //26
           transitPath+ "odtbrd.zip",     //27
           transitPath+ "odtdrv.zip",     //28
           transitPath+ "odtfar.zip"};    //29
           
           mNameGlobal=mName;
                                           

//Steve Test here 
//Highway skims to array here 
  try{
      //highway matrices stored as Matrix
            
    
      logger.fine("Reading Highway Matrices into memory");

      long startTime = System.currentTimeMillis();
      MatrixReader pkTimeReader= MatrixReader.createReader(MatrixType.ZIP,new File(mName[0])); 
      pkTime= pkTimeReader.readMatrix(mName[0]);
      logger.fine("\tTime to read pkTime : "+(System.currentTimeMillis()-startTime)/1000+" seconds");
  
      startTime = System.currentTimeMillis();
      MatrixReader pkDistReader= MatrixReader.createReader(MatrixType.ZIP,new File(mName[1]));    
      pkDist= pkDistReader.readMatrix(mName[1]);
      logger.fine("\tTime to read pkDist : "+(System.currentTimeMillis()-startTime)/1000+" seconds");

              
      MatrixReader opTimeReader= MatrixReader.createReader(MatrixType.ZIP,new File(mName[2])); 
      startTime = System.currentTimeMillis();
      opTime= opTimeReader.readMatrix(mName[2]);
      logger.fine("\tTime to read opTime : "+(System.currentTimeMillis()-startTime)/1000+" seconds");

              
      MatrixReader opDistReader= MatrixReader.createReader(MatrixType.ZIP,new File(mName[3]));     
      startTime = System.currentTimeMillis();
      opDist= opDistReader.readMatrix(mName[3]);
      logger.fine("\tTime to read opDist : "+(System.currentTimeMillis()-startTime)/1000+" seconds");

      
        logger.fine("Reading skims into memory");
        startTime = System.currentTimeMillis();
        logger.fine("\tReading Peak Walk-Transit into memory");
        pkwlk = createSkimsMatrices(new File(mName[4]),
                                                       new File(mName[5]),
                                                       new File(mName[6]),
                                                       new File(mName[7]),
                                                       new File(mName[8]),
                                                       new File(mName[9]));

        logger.fine("\t\tTime to read pkwlk : "+(System.currentTimeMillis()-startTime)/1000+" seconds");

        startTime = System.currentTimeMillis();
        logger.fine("\tReading off Peak Walk-Transit into memory");
        opwlk = createSkimsMatrices(new File(mName[10]),
                                                       new File(mName[11]),
                                                       new File(mName[12]),
                                                       new File(mName[13]),
                                                       new File(mName[14]),
                                                       new File(mName[15]));
        logger.fine("\t\tTime to read opwlk : "+(System.currentTimeMillis()-startTime)/1000+" seconds");

        startTime = System.currentTimeMillis();
        logger.fine("\tReading Peak Drive-Transit into memory");
        pkdrv = createSkimsMatrices(new File(mName[16]),
                                                       new File(mName[17]),
                                                       new File(mName[18]),
                                                       new File(mName[19]),
                                                       new File(mName[20]),
                                                       new File(mName[21]),
                                                       new File(mName[22]));
        logger.fine("\t\tTime to read pkdrv : "+(System.currentTimeMillis()-startTime)/1000+" seconds");

        startTime = System.currentTimeMillis();
        logger.fine("\tReading off Peak Drive-Transit into memory");
        opdrv = createSkimsMatrices(new File(mName[23]),
                                                       new File(mName[24]),
                                                       new File(mName[25]),
                                                       new File(mName[26]),
                                                       new File(mName[27]),
                                                       new File(mName[28]),
                                                       new File(mName[29]));
        logger.fine("\t\tTime to read opdrv : "+(System.currentTimeMillis()-startTime)/1000+" seconds");

       }catch(Exception ioerr){
	    	logger.info("Error:SkimsInMemory class:IO Exception reading matrices");   
	    	ioerr.printStackTrace();
	   };
    	
    	logger.fine("Finished reading skims into memory");
    } //end constructor
	
    
    public float getDistance(int endTime, int originTaz, int destinationTaz){
        if((endTime>=700 && endTime<=830)||(endTime>=1600 && endTime<=1800)){  //peak
            //if PM Peak, then reverse origin and destination to get peak skims 
            if (endTime>=1600 && endTime<=1800)
                return pkDist.getValueAt(destinationTaz,originTaz); 
            else
                return pkDist.getValueAt(originTaz,destinationTaz);
        }
        else return opDist.getValueAt(originTaz,destinationTaz);
    }
    
	//to set the travel time and cost, based on the origin taz, the destination taz, and the time of day.
	//time of day is in military time from 0 -> 2359
	public TravelTimeAndCost setTravelTimeAndCost(int originTaz, int destinationTaz, int time){
		
		//TravelTimeAndCost tc = new TravelTimeAndCost();
	
   		if((time>=700 && time<=830)||(time>=1600 && time<=1800)){  //peak
            //if PM Peak, then reverse origin and destination to get peak skims 
			if (time>=1600 && time<=1800){
			
                tc.driveAloneTime  = pkTime.getValueAt(destinationTaz, originTaz);
                tc.sharedRide2Time = pkTime.getValueAt(destinationTaz, originTaz);
                tc.sharedRide3Time = pkTime.getValueAt(destinationTaz, originTaz);
                tc.walkTime        = ((pkTime.getValueAt(destinationTaz, originTaz)/WALKMPH)*60);
                tc.bikeTime        = ((pkTime.getValueAt(destinationTaz, originTaz)/BIKEMPH)*60);
            //else it is AM peak
            }else {
            
                tc.driveAloneTime  = pkTime.getValueAt(originTaz,destinationTaz);
                tc.sharedRide2Time = pkTime.getValueAt(originTaz,destinationTaz);
                tc.sharedRide3Time = pkTime.getValueAt(originTaz,destinationTaz);
                tc.walkTime        = ((pkTime.getValueAt(originTaz,destinationTaz)/WALKMPH)*60);
                tc.bikeTime        = ((pkTime.getValueAt(originTaz,destinationTaz)/BIKEMPH)*60);
            }                      
			tc.driveAloneDistance= pkDist.getValueAt(originTaz,destinationTaz);                  
			tc.driveAloneCost    = pkDist.getValueAt(originTaz,destinationTaz)*AOC;                      
                                                                                    
			tc.sharedRide2Distance = pkDist.getValueAt(originTaz,destinationTaz);                      
			tc.sharedRide2Cost     = pkDist.getValueAt(originTaz,destinationTaz)*AOC;                  
                                             
			tc.sharedRide3Distance = pkDist.getValueAt(originTaz,destinationTaz);                      
			tc.sharedRide3Cost     = pkDist.getValueAt(originTaz,destinationTaz)*AOC;                  
                                 
			tc.walkDistance        = pkDist.getValueAt(originTaz,destinationTaz);                                                                                                          
			tc.bikeDistance        = pkDist.getValueAt(originTaz,destinationTaz);               
			
            // if PM Peak, then reverse origin and destination to get peak skims
            if (time>=1600 && time<=1800){
			    tc.walkTransitInVehicleTime      = pkwlk.getValue(destinationTaz,originTaz,"pwtivt");  
			    if(tc.walkTransitInVehicleTime>0){          
				    tc.walkTransitFirstWaitTime      = pkwlk.getValue(destinationTaz,originTaz,"pwtfwt");             
				    tc.walkTransitShortFirstWaitTime = Math.min(tc.walkTransitFirstWaitTime,FIRSTWAITSEGMENT);       
				    tc.walkTransitLongFirstWaitTime  = Math.max((tc.walkTransitFirstWaitTime-FIRSTWAITSEGMENT),0);       
				    tc.walkTransitTotalWaitTime      = pkwlk.getValue(destinationTaz,originTaz,"pwttwt");   
				    tc.walkTransitTransferWaitTime   = Math.max((tc.walkTransitTotalWaitTime-tc.walkTransitFirstWaitTime),0);      
				    tc.walkTransitNumberBoardings    = pkwlk.getValue(destinationTaz,originTaz,"pwtbrd");      
				    tc.walkTransitWalkTime           = pkwlk.getValue(destinationTaz,originTaz,"pwtaux");      
				    tc.walkTransitFare               = pkwlk.getValue(destinationTaz,originTaz,"pwtfar");       
			    };
			
			    tc.driveTransitInVehicleTime       = pkdrv.getValue(destinationTaz,originTaz,"pdtivt");  
			    if(tc.driveTransitInVehicleTime>0){
				    tc.driveTransitFirstWaitTime       = pkdrv.getValue(destinationTaz,originTaz,"pdtfwt");     
				    tc.driveTransitShortFirstWaitTime  = Math.min(tc.driveTransitFirstWaitTime,FIRSTWAITSEGMENT);                     
				    tc.driveTransitLongFirstWaitTime   = Math.max((tc.driveTransitFirstWaitTime-FIRSTWAITSEGMENT),0);                 
				    tc.driveTransitTotalWaitTime       = pkdrv.getValue(destinationTaz,originTaz,"pdttwt");                          
				    tc.driveTransitTransferWaitTime    = Math.max((tc.driveTransitTotalWaitTime-tc.driveTransitFirstWaitTime),0);      
				    tc.driveTransitNumberBoardings     = pkdrv.getValue(destinationTaz,originTaz,"pdtbrd");                          
				    tc.driveTransitWalkTime            = pkdrv.getValue(destinationTaz,originTaz,"pdtwlk");                          
				    tc.driveTransitDriveTime           = pkdrv.getValue(destinationTaz,originTaz,"pdtdrv");                          
				    tc.driveTransitDriveCost           = (tc.driveTransitDriveTime /60) * DRIVETRANSITMPH;                                                                           
				    tc.driveTransitFare                = pkdrv.getValue(destinationTaz,originTaz,"pdtfar");
			    };
            // else it is AM peak
            } else{
                tc.walkTransitInVehicleTime      = pkwlk.getValue(originTaz,destinationTaz,"pwtivt");  
                if(tc.walkTransitInVehicleTime>0){
         
                    tc.walkTransitFirstWaitTime      = pkwlk.getValue(originTaz,destinationTaz,"pwtfwt");             
                    tc.walkTransitShortFirstWaitTime = Math.min(tc.walkTransitFirstWaitTime,FIRSTWAITSEGMENT);       
                    tc.walkTransitLongFirstWaitTime  = Math.max((tc.walkTransitFirstWaitTime-FIRSTWAITSEGMENT),0);       
                    tc.walkTransitTotalWaitTime      = pkwlk.getValue(originTaz,destinationTaz,"pwttwt");   
                    tc.walkTransitTransferWaitTime   = Math.max((tc.walkTransitTotalWaitTime-tc.walkTransitFirstWaitTime),0);      
                    tc.walkTransitNumberBoardings    = pkwlk.getValue(originTaz,destinationTaz,"pwtbrd");      
                    tc.walkTransitWalkTime           = pkwlk.getValue(originTaz,destinationTaz,"pwtaux");      
                    tc.walkTransitFare               = pkwlk.getValue(originTaz,destinationTaz,"pwtfar");       
                };
            
                tc.driveTransitInVehicleTime       = pkdrv.getValue(originTaz,destinationTaz,"pdtivt");  
                if(tc.driveTransitInVehicleTime>0){
                    tc.driveTransitFirstWaitTime       = pkdrv.getValue(originTaz,destinationTaz,"pdtfwt");     
                    tc.driveTransitShortFirstWaitTime  = Math.min(tc.driveTransitFirstWaitTime,FIRSTWAITSEGMENT);                     
                    tc.driveTransitLongFirstWaitTime   = Math.max((tc.driveTransitFirstWaitTime-FIRSTWAITSEGMENT),0);                 
                    tc.driveTransitTotalWaitTime       = pkdrv.getValue(originTaz,destinationTaz,"pdttwt");                          
                    tc.driveTransitTransferWaitTime    = Math.max((tc.driveTransitTotalWaitTime-tc.driveTransitFirstWaitTime),0);      
                    tc.driveTransitNumberBoardings     = pkdrv.getValue(originTaz,destinationTaz,"pdtbrd");                          
                    tc.driveTransitWalkTime            = pkdrv.getValue(originTaz,destinationTaz,"pdtwlk");                          
                    tc.driveTransitDriveTime           = pkdrv.getValue(originTaz,destinationTaz,"pdtdrv");                          
                    tc.driveTransitDriveCost           = (tc.driveTransitDriveTime /60) * DRIVETRANSITMPH;                                                                           
                    tc.driveTransitFare                = pkdrv.getValue(originTaz,destinationTaz,"pdtfar");
                };
            }
        //else it is offpeak            
		}else{
			tc.driveAloneTime    = opTime.getValueAt(originTaz,destinationTaz);                      
			tc.driveAloneDistance= opDist.getValueAt(originTaz,destinationTaz);                  
			tc.driveAloneCost    = opDist.getValueAt(originTaz,destinationTaz)*AOC;                      
                                   
			tc.sharedRide2Time     = opTime.getValueAt(originTaz,destinationTaz);                      
			tc.sharedRide2Distance = opDist.getValueAt(originTaz,destinationTaz);                      
			tc.sharedRide2Cost     = opDist.getValueAt(originTaz,destinationTaz)*AOC;                  
                                     
			tc.sharedRide3Time     = opTime.getValueAt(originTaz,destinationTaz);                      
			tc.sharedRide3Distance = opDist.getValueAt(originTaz,destinationTaz);                      
			tc.sharedRide3Cost     = opDist.getValueAt(originTaz,destinationTaz)*AOC;                  
                                     
			tc.walkTime            = ((opTime.getValueAt(originTaz,destinationTaz)/WALKMPH)*60);
			tc.walkDistance        = opDist.getValueAt(originTaz,destinationTaz);                                                            
                                                                                
			tc.bikeTime            = ((opTime.getValueAt(originTaz,destinationTaz)/BIKEMPH)*60);
			tc.bikeDistance        = opDist.getValueAt(originTaz,destinationTaz);                                   
                                                                                                     
			tc.walkTransitInVehicleTime      = opwlk.getValue(originTaz,destinationTaz,"owtivt");            
			
			if(tc.walkTransitInVehicleTime>0){
				tc.walkTransitFirstWaitTime      = opwlk.getValue(originTaz,destinationTaz,"owtfwt");             
				tc.walkTransitShortFirstWaitTime = Math.min(tc.walkTransitFirstWaitTime,FIRSTWAITSEGMENT);       
				tc.walkTransitLongFirstWaitTime  = Math.max((tc.walkTransitFirstWaitTime-FIRSTWAITSEGMENT),0);       
				tc.walkTransitTotalWaitTime      = opwlk.getValue(originTaz,destinationTaz,"owttwt");   
				tc.walkTransitTransferWaitTime   = Math.max((tc.walkTransitTotalWaitTime-tc.walkTransitFirstWaitTime),0);      
				tc.walkTransitNumberBoardings    = opwlk.getValue(originTaz,destinationTaz,"owtbrd");      
				tc.walkTransitWalkTime           = opwlk.getValue(originTaz,destinationTaz,"owtaux");      
				tc.walkTransitFare               = opwlk.getValue(originTaz,destinationTaz,"owtfar");       
        	};
                
              
			tc.driveTransitInVehicleTime       = opdrv.getValue(originTaz,destinationTaz,"odtivt");                          
            if(tc.driveTransitInVehicleTime>0){   			
	            tc.driveTransitFirstWaitTime       = opdrv.getValue(originTaz,destinationTaz,"odtfwt");                          
				tc.driveTransitShortFirstWaitTime  = Math.min(tc.driveTransitFirstWaitTime,FIRSTWAITSEGMENT);                     
				tc.driveTransitLongFirstWaitTime   = Math.max((tc.driveTransitFirstWaitTime-FIRSTWAITSEGMENT),0);                 
				tc.driveTransitTotalWaitTime       = opdrv.getValue(originTaz,destinationTaz,"odttwt");                          
				tc.driveTransitTransferWaitTime    = Math.max((tc.driveTransitTotalWaitTime-tc.driveTransitFirstWaitTime),0);      
				tc.driveTransitNumberBoardings     = opdrv.getValue(originTaz,destinationTaz,"odtbrd");                          
				tc.driveTransitWalkTime            = opdrv.getValue(originTaz,destinationTaz,"odtwlk");                          
				tc.driveTransitDriveTime           = opdrv.getValue(originTaz,destinationTaz,"odtdrv");                          
				tc.driveTransitDriveCost           = (tc.driveTransitDriveTime /60) * DRIVETRANSITMPH;                                                                           
				tc.driveTransitFare                = opdrv.getValue(originTaz,destinationTaz,"odtfar"); 
			};
		};                   
		
		return tc;
				
		
	};  
	   
	/*
	* for intermediate stop destination choice
	*
	*/
    
    
    
	public float getAdditionalAutoTime(int fromTaz, int toTaz, int stopTaz, int time){
				
		float directTime=0;
		float totalTime=0;
		
		if((time>=700 && time<=830)||(time>=1600 && time<=1800)){  //peak
			directTime = pkTime.getValueAt(fromTaz,toTaz);
			totalTime = pkTime.getValueAt(fromTaz,stopTaz)
				+ 	pkTime.getValueAt(stopTaz,toTaz);
		}else{
			directTime = opTime.getValueAt(fromTaz,toTaz); 
			totalTime = opTime.getValueAt(fromTaz,stopTaz)
				+ 	opTime.getValueAt(stopTaz,toTaz);
		}
		
		return Math.max((totalTime-directTime),0);
		
	}
    
	/*
	* for intermediate stop destination choice
	*
	*/
    
    
	public float getAdditionalWalkTime(int fromTaz, int toTaz, int stopTaz,  int time){
		
		float directTime=0;
		float totalTime=0;
		
		if((time>=700 && time<=830)||(time>=1600 && time<=1800)){  //peak
			directTime = (pkTime.getValueAt(fromTaz,toTaz)/WALKMPH)*60;
			totalTime = (pkTime.getValueAt(fromTaz,stopTaz)/WALKMPH)*60
				+ (pkTime.getValueAt(stopTaz,toTaz)/WALKMPH)*60;
		}else{
			directTime = (opTime.getValueAt(fromTaz,toTaz)/WALKMPH)*60; 
			totalTime = (opTime.getValueAt(fromTaz,stopTaz)/WALKMPH)*60
				+ (opTime.getValueAt(stopTaz,toTaz)/WALKMPH)*60;
		}
		
		return Math.max((totalTime-directTime),0);
		
		
		
	}
	/*
	* for intermediate stop destination choice
	*
	*/
    
    
	public float getAdditionalBikeTime(int fromTaz, int toTaz,  int stopTaz, int time){
		
		float directTime=0;
		float totalTime=0;
		
		if((time>=700 && time<=830)||(time>=1600 && time<=1800)){  //peak
			directTime = (pkTime.getValueAt(fromTaz,toTaz)/BIKEMPH)*60;
			totalTime = (pkTime.getValueAt(fromTaz,stopTaz)/BIKEMPH)*60
				+ (pkTime.getValueAt(stopTaz,toTaz)/BIKEMPH)*60;
		}else{
			directTime = (opTime.getValueAt(fromTaz,toTaz)/BIKEMPH)*60; 
			totalTime = (opTime.getValueAt(fromTaz,stopTaz)/BIKEMPH)*60
				+ (opTime.getValueAt(stopTaz,toTaz)/BIKEMPH)*60;
		}
		
		return Math.max((totalTime-directTime),0);
			
	}
    
	/*
	* for intermediate stop destination choice
	*
	*/
    
	public float getAdditionalGeneralizedTransitCost(int fromTaz, int toTaz,  int stopTaz, int time){
		
		float directCost=0;
		float totalCost=0;
		float inVehicleTime=0;
		float firstWaitTime=0;
		float totalWaitTime=0;
		float transferWaitTime=0;
		float walkTime=0;
        float firstWaitFactor=(float)1.5;
        float transferWaitFactor=(float)2.5;
        float walkFactor=(float)3.0;

		//  the following formula was used to compute generalized cost for model estimation:
		//  	costToStop= ivtToStop + 1.5*fwtToStop + 2.5*(transfer wait) + 3.0*auxToStop
		
		if((time>=700 && time<=830)||(time>=1600 && time<=1800)){  //peak

			//fromTaz->toTaz
			inVehicleTime      = pkwlk.getValue(fromTaz,toTaz,"pwtivt");            
			if(inVehicleTime<=0)
				return 0;
			
			firstWaitTime      = pkwlk.getValue(fromTaz,toTaz,"pwtfwt");             
			totalWaitTime      = pkwlk.getValue(fromTaz,toTaz,"pwttwt");   
			transferWaitTime   = Math.max((totalWaitTime-firstWaitTime),0);      
			walkTime           = pkwlk.getValue(fromTaz,toTaz,"pwtaux");      
			
			directCost = inVehicleTime + firstWaitFactor*firstWaitTime + transferWaitFactor*transferWaitTime + walkFactor*walkTime;
			
			//fromTaz->stopTaz
			inVehicleTime      = pkwlk.getValue(fromTaz,stopTaz,"pwtivt");            
			if(inVehicleTime<=0)
				return 0;
			
			firstWaitTime      = pkwlk.getValue(fromTaz,stopTaz,"pwtfwt");             
			totalWaitTime      = pkwlk.getValue(fromTaz,stopTaz,"pwttwt");   
			transferWaitTime   = Math.max((totalWaitTime-firstWaitTime),0);      
			walkTime           = pkwlk.getValue(fromTaz,stopTaz,"pwtaux");      
						
			totalCost =  inVehicleTime + firstWaitFactor*firstWaitTime + transferWaitFactor*transferWaitTime + walkFactor*walkTime;
			
			//stopTaz->toTaz
			inVehicleTime      = pkwlk.getValue(stopTaz,toTaz,"pwtivt");            
			if(inVehicleTime<=0)
				return 0;
			
			firstWaitTime      = pkwlk.getValue(stopTaz,toTaz,"pwtfwt");             
			totalWaitTime      = pkwlk.getValue(stopTaz,toTaz,"pwttwt");   
			transferWaitTime   = Math.max((totalWaitTime-firstWaitTime),0);      
			walkTime           = pkwlk.getValue(stopTaz,toTaz,"pwtaux");      
			
			totalCost += inVehicleTime + firstWaitFactor*firstWaitTime + transferWaitFactor*transferWaitTime + walkFactor*walkTime;

		}else{
			//fromTaz->toTaz
			inVehicleTime      = opwlk.getValue(fromTaz,toTaz,"owtivt");            
			if(inVehicleTime<=0)
				return 0;
			
			firstWaitTime      = opwlk.getValue(fromTaz,toTaz,"owtfwt");             
			totalWaitTime      = opwlk.getValue(fromTaz,toTaz,"owttwt");   
			transferWaitTime   = Math.max((totalWaitTime-firstWaitTime),0);      
			walkTime           = opwlk.getValue(fromTaz,toTaz,"owtaux");      
			
			directCost = inVehicleTime + firstWaitFactor*firstWaitTime + transferWaitFactor*transferWaitTime + walkFactor*walkTime;

			//fromTaz->stopTaz
			inVehicleTime      = opwlk.getValue(fromTaz,stopTaz,"owtivt");            
			if(inVehicleTime<=0)
				return 0;
			
			firstWaitTime      = opwlk.getValue(fromTaz,stopTaz,"owtfwt");             
			totalWaitTime      = opwlk.getValue(fromTaz,stopTaz,"owttwt");   
			transferWaitTime   = Math.max((totalWaitTime-firstWaitTime),0);      
			walkTime           = opwlk.getValue(fromTaz,stopTaz,"owtaux");      
						
			totalCost =  inVehicleTime + firstWaitFactor*firstWaitTime + transferWaitFactor*transferWaitTime + walkFactor*walkTime;

			//stopTaz->toTaz
			inVehicleTime      = opwlk.getValue(stopTaz,toTaz,"owtivt");            
			if(inVehicleTime<=0)
				return 0;
			
			firstWaitTime      = opwlk.getValue(stopTaz,toTaz,"owtfwt");             
			totalWaitTime      = opwlk.getValue(stopTaz,toTaz,"owttwt");   
			transferWaitTime   = Math.max((totalWaitTime-firstWaitTime),0);      
			walkTime           = opwlk.getValue(stopTaz,toTaz,"owtaux");      
			
			totalCost += inVehicleTime + firstWaitFactor*firstWaitTime + transferWaitFactor*transferWaitTime + walkFactor*walkTime;

		}
		
		return Math.max((totalCost-directCost),0);
			
	}

    public float[] getAdditionalAutoDistance(int fromTaz, int toTaz,  int stopTaz, int time){

		float[] autoDists = new float[2];

		if((time>=700 && time<=830)||(time>=1600 && time<=1800)){  //peak
			autoDists[0] = pkDist.getValueAt(fromTaz,toTaz);
			autoDists[1] = pkDist.getValueAt(fromTaz,stopTaz)+ pkDist.getValueAt(stopTaz,toTaz);
		}else{
			autoDists[0] = opDist.getValueAt(fromTaz,toTaz);
			autoDists[1] = opDist.getValueAt(fromTaz,stopTaz) + opDist.getValueAt(stopTaz,toTaz);
		}

		return autoDists;

	}



};
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
