package com.pb.despair.pt;
 
import java.io.File;
import java.io.Serializable;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.matrix.MatrixExpansion;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.TableDataSetLoader;


/**
 * Class to store Labor Flow $ by occupation
 * Reads in .csv file output from PI and creates three matrix collections:
 * 1) Labor $ flows for each origin - destination
 * 2) Column vectors of total flows from an origin
 * 3) Probability Matrix = ($Flows [origin to destination]) / (sum($Flows [origin to all destinations])
 * 
 * @author Steve Hansen
 * @version 1.0
 * April 2, 2004
 * 
 */

public class LaborFlows implements Serializable{
    
    public static AlphaToBeta a2b;
    
    public static MatrixCollection betaLaborFlows = new MatrixCollection();
    public static MatrixCollection alphaProduction;
    public static MatrixCollection alphaConsumption;
    
    static double dispersionParameter = 0.54; //this is the default value but it is also set in the properties file
    
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");
    static boolean debug = false;
    ResourceBundle rb;

    public LaborFlows(ResourceBundle rb){
        this.rb = rb;
        dispersionParameter = Double.parseDouble(ResourceUtil.getProperty(rb,"labor.flow.dispersion.parameter"));
    }

    public void setZoneMap(TableDataSet table){
        int zoneCount=0;
        for(int i=1;i<=table.getRowCount();i++){
           if(!table.getStringValueAt(i,"County").equals("outsideHalo")){
               zoneCount++;
           }
        }
        int[] aZone = new int[zoneCount];
        int[] bZone = new int[zoneCount];
        int counter = 0;
        for(int i=1;i<=table.getRowCount();i++){
            if(!table.getStringValueAt(i,"County").equals("outsideHalo")){
                aZone[counter] = (int)table.getValueAt(i,"Azone");
                bZone[counter] = (int)table.getValueAt(i,"Bzone");
                if(debug) logger.fine("alpha zone: "+aZone[counter]);
                counter++;
                
            }           
         }
        if(debug) logger.fine("counter"+counter);
        
    	a2b = new AlphaToBeta(table.getColumnAsInt(table.getColumnPosition("AZone")),
				   			  table.getColumnAsInt(table.getColumnPosition("BZone")));
    	//a2b = new AlphaToBeta(table);
    	//me = new MatrixExpansion(a2b);

    }
    
      
    public static Matrix calculatePropensityMatrix(Matrix m){

        logger.fine("Set travel propensity Matrix.");
        Matrix alphaPropensityMatrix = new Matrix(m.getRowCount(),m.getColumnCount());
        String name = m.getName();
        int lsIndex = name.indexOf("ls");
        name = name.substring(0,lsIndex) + "propensity";
        alphaPropensityMatrix.setName(name);
        alphaPropensityMatrix.setExternalNumbers(m.getExternalNumbers());
    	int origin;
    	int destination;
    	float propensity;
    	for(int i=0;i<m.getRowCount();i++){
    		for(int j=0;j<m.getColumnCount();j++){
    			origin = m.getExternalNumber(i);
    			destination = m.getExternalNumber(j);
    			propensity = (float)MathUtil.exp(dispersionParameter*m.getValueAt(origin,destination));              
    			alphaPropensityMatrix.setValueAt(origin,destination,propensity);
    		}
    	}
        return alphaPropensityMatrix;
    }
    
    /*public void readBetaLaborFlows(TableDataSet table){
    	int origin;
    	int destination;
    	float flows;
    	
        logger.info("Getting labor flows table");
        
        betaLaborFlows = setMatrixCollection(a2b.betaSize(),a2b.betaSize(),a2b.getBetaExternals());
        
        int occupationColumn = table.getColumnPosition("Commodity");
        int originColumn = table.getColumnPosition("Origin");
        int destinationColumn = table.getColumnPosition("Destination");
        int flowsColumn = table.getColumnPosition("Flow");
        
        
        for(int rowNumber=1;rowNumber<=table.getRowCount();rowNumber++) {
            
            byte occupation = OccupationCode.getOccupationCode(
                                  table.getStringValueAt(rowNumber,occupationColumn));
            origin = (int)table.getValueAt(rowNumber, originColumn);
            destination = (int)table.getValueAt(rowNumber, destinationColumn);
            flows = table.getValueAt(rowNumber, flowsColumn);
            
            switch (occupation){
                case OccupationCode.MANAGERS_PROFESSIONALS:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
                case OccupationCode.HEALTH:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
                case OccupationCode.P0ST_SEC_TEACHERS:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
                case OccupationCode.OTHER_TEACHERS:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
                case OccupationCode.OTHER_PROF_TECH:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
                case OccupationCode.RETAIL_SALES:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
                case OccupationCode.OTHER_RETAIL_CLERICAL:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
                case OccupationCode.ALL_OTHER:
                betaLaborFlows.setValue(origin, destination, (new Byte(occupation)).toString(), flows);
                break;
            }
        }
    }*/

    public void readBetaLaborFlows(){
    	String[] fileNames = {"not employed",
                              "selling_1_ManPro.zipMatrix",
                              "selling_1a_Health.zipMatrix",
                              "selling_2_PstSec.zipMatrix",
                              "selling_3_OthTchr.zipMatrix",
                              "selling_4_OthP&T.zipMatrix",
                              "selling_5_RetSls.zipMatrix",
                              "selling_6_OthR&C.zipMatrix",
                              "selling_7_NonOfc.zipMatrix"
                              };
        
        String path = ResourceUtil.getProperty(rb, "betaFlows.path");
    	
        for(int occupation=1;occupation<fileNames.length;occupation++){
            MatrixReader mr= MatrixReader.createReader(MatrixType.ZIP,new File(path+fileNames[occupation]));                   
            Matrix m = mr.readMatrix(path+fileNames[occupation]);
            Matrix newMatrix = new Matrix(a2b.getBetaExternals().length-1,a2b.getBetaExternals().length-1);
            newMatrix.setExternalNumbers(a2b.getBetaExternals());
            for(int origin=0;origin<m.getColumnCount();origin++){
                for(int destination=0;destination<m.getColumnCount();destination++){
                	newMatrix.setValueAt(m.getExternalNumber(origin),
                                         m.getExternalNumber(destination), 
                                         m.getValueAt(m.getExternalNumber(origin),m.getExternalNumber(destination)));
                }
            }
            newMatrix.setName(String.valueOf(occupation));
            betaLaborFlows.addMatrix(newMatrix);
        }
    }
    
    public void readAlphaValues(TableDataSet production, TableDataSet consumption){
    	int taz;
    	float value;
    	
        String[] occupationString = {"1_ManPro",
                                     "1a_Health",
									 "2_PstSec",
									 "3_OthTchr",
									 "4_OthP&T",
									 "5_RetSls",
									 "6_OthR&C",
									 "7_NonOfc",};
        

        
        alphaProduction = setMatrixCollection(a2b.alphaSize(),1,a2b.getAlphaExternals());
        
        int tazColumn = production.getColumnPosition("zoneNumber");
        
        for(int rowNumber=1;rowNumber<=production.getRowCount();rowNumber++) {
            taz = (int)production.getValueAt(rowNumber, tazColumn);
            for(int o=0;o<occupationString.length;o++){
                byte occupationCode = OccupationCode.getOccupationCode(occupationString[o]);
                alphaProduction.setValue(taz,0,(new Byte(occupationCode)).toString(),production.getValueAt(rowNumber,occupationString[o]));   
            }
        }
        
        alphaConsumption = setMatrixCollection(a2b.alphaSize(),1,a2b.getAlphaExternals());
        
        tazColumn = production.getColumnPosition("zoneNumber");
        
        for(int rowNumber=1;rowNumber<=consumption.getRowCount();rowNumber++) {
            taz = (int)consumption.getValueAt(rowNumber, tazColumn);
            for(int o=0;o<occupationString.length;o++){
                byte occupationCode = OccupationCode.getOccupationCode(occupationString[o]);
                alphaConsumption.setValue(taz,0,(new Byte(occupationCode)).toString(),consumption.getValueAt(rowNumber,occupationString[o]));   
            }
        }
        
    }
    
    private static MatrixCollection setMatrixCollection(int rows, int columns, int[] externalNumbers){
        MatrixCollection mc = new MatrixCollection();
        
        for(int i=1;i<=8;i++){
            String occupationCode = Integer.toString(i);                        
            mc.addMatrix(occupationCode,rows,columns);
            mc.getMatrix(occupationCode).setExternalNumbers(externalNumbers);      
        }
        return mc;
    }
    
    public static Matrix calculateAlphaLaborFlowsMatrix(Matrix mcLogsum, int segment, int occupation){
    	MatrixExpansion me = new MatrixExpansion(a2b);
    	me.setPropensityMatrix(calculatePropensityMatrix(mcLogsum));

        String occupationCode = Integer.toString(occupation);
        if (debug) {
            logger.fine("occupation "+occupation);
            logger.fine("segment "+segment);
            logger.fine("betaLaborFlows: "+betaLaborFlows.getHashMap());
        }
        Matrix blf = betaLaborFlows.getMatrix(occupationCode);
        me.setBetaFlowMatrix(blf);
        me.setConsumptionMatrix(alphaConsumption.getMatrix(occupationCode));
        me.setProductionMatrix(alphaProduction.getMatrix(occupationCode));
        if(debug) logger.info("Calculating alphaZone flows for occupation "+occupationCode);
        me.setAlphaFlowMatrix(a2b);
        if(debug) logger.info("Calculating alphaZone flow probabilities for occupation "+occupationCode);
        me.setProbabilityMatrix(me.getAlphaFlowMatrix());
        Matrix m = me.getAlphaFlowProbabilityMatrix();
        m.setName("lf"+occupation+segment+".zip");
        me=null;        
        return m;
    }
    
    public void debugMatrix(Matrix m){
    	//for(int i=1;i<=8;i++){            

            for(int r=0;r<m.getRowCount();r++){
            	float rowSum = m.getRowSum(m.getExternalNumber(r));
            	if(rowSum<1)
            		logger.info("rowsum for occupation "+m.getName()+" taz "+m.getExternalNumber(r)+" = "+rowSum);
            }
    	//}
    	
    }
    
    public void writeLaborFlowProbabilities(Matrix lfProbability){       
        logger.info("Calculate labor flow probabilities.");
        long startTime = System.currentTimeMillis();
        if(debug) logger.fine("Time to calculate labor flow matrix: "+(System.currentTimeMillis()-startTime)/1000+" seconds.");
        String mName = lfProbability.getName();
		if(debug) logger.info("Writing probability Matrix : "+mName);
		//get path
		String outputPath = ResourceUtil.getProperty(rb, "betaFlows.path");
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(outputPath+mName+".zip"));  //Open for writing
        startTime = System.currentTimeMillis();
        mw.writeMatrix(lfProbability);
        if(debug) logger.fine("Time to write labor flow matrix: "+(System.currentTimeMillis()-startTime)/1000+" seconds.");
    }
    
    public static void main(String args[]){
        ResourceBundle rb =ResourceUtil.getResourceBundle("pt");
        MatrixCollection laborFlowProbabilities;

        LaborFlows lf = new LaborFlows(rb);
        logger.info("Reading alphaZone to betaZone mapping.");
        TableDataSet alphaToBeta = TableDataSetLoader.loadTableDataSet(rb,"alphatobeta.file");
        logger.info("Creating alphaZone to betaZone mapping based on the TableDataSet alphaToBeta");
        lf.setZoneMap(alphaToBeta);
        
        logger.info("Reading alpha production and consumption numbers.");
        TableDataSet productionValues = TableDataSetLoader.loadTableDataSet(rb,"productionValues.file");
        TableDataSet consumptionValues = TableDataSetLoader.loadTableDataSet(rb,"consumptionValues.file");
        //logger.info("Reading betaZone labor flows");
        //TableDataSet betaLaborFlowsTable = loadTableDataSet("pt","betaFlows.file");

        logger.info("Put production and consumption numbers by alphaZone into MatrixCollection alphaProductionAndConsumption.");
        lf.readAlphaValues(productionValues, consumptionValues);
        logger.info("Put betaZone labor flows into MatrixCollection betaLaborFlows.");
        lf.readBetaLaborFlows();

        String path = ResourceUtil.getProperty(rb, "mcLogsum.path");
        
        //Loop through each work market segment
        for(int segment=1;segment<2;segment++){

            logger.info("Reading work MC logsums for segment "+segment);
            MatrixReader mr= MatrixReader.createReader(MatrixType.ZIP,new File(path+"w"+segment+"ls.zip"));                   
            Matrix m = mr.readMatrix(path+"w"+segment+"ls.zip");
            LaborFlows.calculatePropensityMatrix(m);
        	//Loop through each occupation type
        	for(int occupation=1;occupation<=5;occupation++){
        		Matrix lfMatrix = LaborFlows.calculateAlphaLaborFlowsMatrix(m, segment, occupation);
        		//lf.writeLaborFlowProbabilities(lfMatrix);
        	}
        }       
        //logger.info("SizeOf(laborFlowProbabilities) = " + ObjectUtil.sizeOf( laborFlowProbabilities ) + " bytes"); 
    }
}
