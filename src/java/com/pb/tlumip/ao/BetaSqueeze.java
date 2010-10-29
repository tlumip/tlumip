package com.pb.tlumip.ao;

import com.pb.common.matrix.*;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.DemandHandler;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 *
 * @author crf
 *         started: Oct 29, 2010
 */
public class BetaSqueeze {
    private static final Logger logger = Logger.getLogger(BetaSqueeze.class);

    public static final String DEMAND_OUTPUT_FILE_PROPERTY = "demand.output.filename";
    //should be beta.compress.file.prefix = beta
    public static final String BETA_SQUEEZE_OUTPUT_ADDENDUM_PROPERTY = "beta.compress.file.prefix";
    public static final String MATRIX_EXTENSION_PROPERTY = "matrix.extension";
    
    //following should be beta.compress.auto.skim.values.mode = a
    public static final String AUTO_SKIM_MODE_TOKENS_PROPERTY = "beta.compress.auto.skim.values.mode";
    //following should be beta.compress.auto.skim.values.period = ampeak,pmpeak
    public static final String AUTO_SKIM_PERIOD_TOKENS_PROPERTY = "beta.compress.auto.skim.values.period";
    public static final String AUTO_SKIM_BASEPATH_PROPERTY = "highwaySkims.directory";
    //should be beta.compress.auto.skim.files = pkautotime,pkautodist,pkautotoll
    public static final String AUTO_SKIM_FILES_PROPERTY = "beta.compress.auto.skim.files";


    //following should be beta.compress.truck.skim.values.mode = d
    public static final String TRUCK_SKIM_MODE_TOKENS_PROPERTY = "beta.compress.truck.skim.values.mode";
    //following should be beta.compress.truck.skim.values.period = ampeak,pmpeak
    public static final String TRUCK_SKIM_PERIOD_TOKENS_PROPERTY = "beta.compress.truck.skim.values.period";
    public static final String TRUCK_SKIM_BASEPATH_PROPERTY = "highwaySkims.directory";
    //should be beta.compress.auto.skim.files = pktrktime,pktrkdist,pktrktoll
    public static final String TRUCK_SKIM_FILES_PROPERTY = "beta.compress.truck.skim.files";
                                                                                                          
    public static final String MCLS_BASEPATH_PROPERTY = "sdt.current.mode.choice.logsums";

    //following should be beta.compress.mcls.skim.values.mode = a,walk_intercity,walk_intracity,walk_hsr,driveLdt_intercity,drive_intracity,driveLdt_hsr,driveLdt_air
    public static final String MCLS_PEAK_MODE_TOKENS_PROPERTY = "beta.compress.mcls.peak.values.mode";
    //following should be beta.compress.mcls.skim.values.period = ampeak,pmpeak
    public static final String MCLS_PEAK_PERIOD_TOKENS_PROPERTY = "beta.compress.mcls.peak.values.period";
    //should be beta.compress.mcls.peak.files = w7mcls,w4mcls,w1mcls,b5mcls,b8mcls,b4mcls
    public static final String MCLS_PEAK_FILES_PROPERTY = "beta.compress.mcls.peak.files";
    
    //following should be beta.compress.mcls.skim.values.mode = a,walk_intercity,walk_intracity,walk_hsr,driveLdt_intercity,drive_intracity,driveLdt_hsr,driveLdt_air
    public static final String MCLS_DAILY_MODE_TOKENS_PROPERTY = "beta.compress.mcls.daily.values.mode";
    //following should be beta.compress.mcls.skim.values.period = ampeak,pmpeak,mdoffpeak,ntoffpeak
    public static final String MCLS_DAILY_PERIOD_TOKENS_PROPERTY = "beta.compress.mcls.daily.values.period";
    //should be beta.compress.auto.skim.files = s4mcls,c4mcls,o4mcls
    public static final String MCLS_DAILY_FILES_PROPERTY = "beta.compress.mcls.daily.files";
        

    private final ResourceBundle rb;
    private final MatrixCompression compressor;
    private float minWeight = 1.0f;
    private Matrix weightMatrix = null;

    public BetaSqueeze(ResourceBundle rb) {
        compressor = new MatrixCompression(new AlphaToBeta(new File( ResourceUtil.getProperty(rb, "alpha2beta.file")),rb.getString("alpha.name"),rb.getString("beta.name")));
        this.rb = rb;
    }

    /**
     * Set the minimum weight for the weight matrix.
     *
     * @param minWeight
     *        The minimum weight matrix.
     */
    public void setMinimumWeight(float minWeight) {
        this.minWeight = minWeight;
    }

    /**
     * Create a weight matrix based on the sum of input matrices.  This will replace current weight matrix.
     *
     * @param matrixFile
     *        The first matrix file for the weights.
     *
     * @param additionalMatrixFiles
     *        Any additional matrix files to weight with.
     *
     * @return {@code true} if the weights were loaded successfully (if {@code false}, then some weight matrix probably missing).
     */
    public boolean loadWeights(String matrixFile, String ... additionalMatrixFiles) {
        File mFile = new File(matrixFile);
        if (!mFile.exists()) {
            logger.warn("Beta squeeze weight matrix not found: " + mFile);
            return false;
        }
        Matrix weightMatrix = new ZipMatrixReader(mFile).readMatrix();
        for (String matFile : additionalMatrixFiles) {
            mFile = new File(matFile);
            if (!mFile.exists()) {
                logger.warn("Beta squeeze weight matrix not found: " + mFile);
                return false;
            }
            weightMatrix.add(new ZipMatrixReader(mFile).readMatrix());
        }
        int[] iExternals = weightMatrix.getExternalRowNumbers();
        int[] jExternals = weightMatrix.getExternalColumnNumbers();
        for (int i = 1; i < iExternals.length; i++)
            for (int j = 1; j < jExternals.length; j++)
                if (weightMatrix.getValueAt(iExternals[i],jExternals[j]) < minWeight)
                    weightMatrix.setValueAt(iExternals[i],jExternals[j],minWeight);
        this.weightMatrix = weightMatrix;
        return true;
    }

    /**
     * Create a weight matrix based on input matrices defined by a tokenized file path. The tokenized matrix file will
     * have its tokens replaced by each set of token values, which will be used as one of the weight matrices.
     *
     * @param tokenizedMatrixFile
     *        The input (tokenized) matrix file.
     *
     * @param tokens
     *        The tokens to be replaced in {@code tokenizedMatrixFile}.
     *
     * @param tokenValues
     *        A series of values to replace the tokens with.  Each sublist should have one value for each token in {@code tokens},
     *        in the same order.  The length of this list will thus be the same as the number of matrices used to create
     *        the weight matrix.
     *
     * @return {@code true} if the weights were loaded successfully (if {@code false}, then some weight matrix probably missing).
     */
    public boolean loadWeights(String tokenizedMatrixFile, List<String> tokens, List<List<String>> tokenValues) {
        String firstWeightMatrix = null;
        String[] weightMatrices = new String[tokenValues.size()-1];
        int counter = 0;
        for (List<String> values : tokenValues) {
            Iterator<String> tokenIterator = tokens.iterator();
            Iterator<String> valueIterator = values.iterator();
            String fileName = tokenizedMatrixFile;
            while (tokenIterator.hasNext())
                fileName = fileName.replace(tokenIterator.next(),valueIterator.next());
            if (firstWeightMatrix == null)
                firstWeightMatrix = fileName;
            else
                weightMatrices[counter++] = fileName;
        }
        return loadWeights(firstWeightMatrix,weightMatrices);
    }

    public Matrix squeezeToBeta(File matrixFile) {
        if (weightMatrix == null)
            throw new IllegalStateException("Weight matrix uninitialized");
        return compressor.getWeightedMeanCompressedMatrix(new ZipMatrixReader(matrixFile).readMatrix(),weightMatrix);
    }
    
    
    public Matrix squeezeToBeta(String matrixFile) {
        return squeezeToBeta(new File(matrixFile));
    }                                              

    public void squeezeToBeta(File matrixFile, File outputPath) {
        if (matrixFile.exists()) {
            logger.info("Squeezing " + matrixFile + " to " + outputPath);
            new ZipMatrixWriter(outputPath).writeMatrix(squeezeToBeta(matrixFile));
        } else {
            logger.info("Cannot find " + matrixFile + ", not squeezing");
        }
    }

    public void squeezeToBeta(String matrixFile, String outputPath) {
        squeezeToBeta(new File(matrixFile),new File(outputPath));
    }

    private boolean loadWeights(String modeTokenProperty, String periodTokenProperty) {
        String demandMatrixTemplate = rb.getString(DEMAND_OUTPUT_FILE_PROPERTY);
        String[] modes = rb.getString(modeTokenProperty).split(",");
        String[] periods = rb.getString(periodTokenProperty).split(",");
        String modeToken = DemandHandler.DEMAND_OUTPUT_MODE_STRING;
        String periodToken = DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING;
        List<String> tokens = Arrays.asList(modeToken,periodToken);
        List<List<String>> values = new LinkedList<List<String>>();
        for (String mode : modes)
            for (String period : periods)
                values.add(Arrays.asList(mode.trim(),period.trim()));
        return loadWeights(demandMatrixTemplate,tokens,values);
    }

    public void squeezeAutoSkims() {
        if (!loadWeights(AUTO_SKIM_MODE_TOKENS_PROPERTY,AUTO_SKIM_PERIOD_TOKENS_PROPERTY)) {
            logger.warn("Missing at least one required demand matrix, updated beta squeeze will not occur");
            return;
        }
        File basePath = new File(rb.getString(AUTO_SKIM_BASEPATH_PROPERTY));
        String matrixExtension = rb.getString(MATRIX_EXTENSION_PROPERTY);
        String filePrefix = rb.getString(BETA_SQUEEZE_OUTPUT_ADDENDUM_PROPERTY);
        String[] files = rb.getString(AUTO_SKIM_FILES_PROPERTY).split(","); 
        for (String file : files) 
            squeezeToBeta(new File(basePath,file + matrixExtension),new File(basePath,filePrefix + file + matrixExtension));
    }                                                                                                                       

    public void squeezeTruckSkims() {
        if (!loadWeights(TRUCK_SKIM_MODE_TOKENS_PROPERTY,TRUCK_SKIM_PERIOD_TOKENS_PROPERTY)) {
            logger.warn("Missing at least one required demand matrix, updated beta squeeze will not occur");
            return;
        }
        File basePath = new File(rb.getString(TRUCK_SKIM_BASEPATH_PROPERTY));
        String matrixExtension = rb.getString(MATRIX_EXTENSION_PROPERTY);
        String filePrefix = rb.getString(BETA_SQUEEZE_OUTPUT_ADDENDUM_PROPERTY);
        String[] files = rb.getString(TRUCK_SKIM_FILES_PROPERTY).split(","); 
        for (String file : files) 
            squeezeToBeta(new File(basePath,file + matrixExtension),new File(basePath,filePrefix + file + matrixExtension));
    }  

    public void squeezePeakMcls() {
        if (!loadWeights(MCLS_PEAK_MODE_TOKENS_PROPERTY,MCLS_PEAK_PERIOD_TOKENS_PROPERTY)) {
            logger.warn("Missing at least one required demand matrix, updated beta squeeze will not occur");
            return;
        }
        File basePath = new File(rb.getString(MCLS_BASEPATH_PROPERTY));
        String matrixExtension = rb.getString(MATRIX_EXTENSION_PROPERTY);
        String fileSuffix = rb.getString(BETA_SQUEEZE_OUTPUT_ADDENDUM_PROPERTY);
        String[] files = rb.getString(MCLS_PEAK_FILES_PROPERTY).split(","); 
        for (String file : files) 
            squeezeToBeta(new File(basePath,file + matrixExtension),new File(basePath,file + "_" + fileSuffix + matrixExtension));
    }                                                         

    public void squeezeDailyMcls() {
        if (!loadWeights(MCLS_DAILY_MODE_TOKENS_PROPERTY,MCLS_DAILY_PERIOD_TOKENS_PROPERTY)) {
            logger.warn("Missing at least one required demand matrix, updated beta squeeze will not occur");
            return;
        }
        File basePath = new File(rb.getString(MCLS_BASEPATH_PROPERTY));
        String matrixExtension = rb.getString(MATRIX_EXTENSION_PROPERTY);
        String fileSuffix = rb.getString(BETA_SQUEEZE_OUTPUT_ADDENDUM_PROPERTY);
        String[] files = rb.getString(MCLS_DAILY_FILES_PROPERTY).split(","); 
        for (String file : files) 
            squeezeToBeta(new File(basePath,file + matrixExtension),new File(basePath,file + "_" + fileSuffix + matrixExtension));
    }

    public void squeezeAll() {
        squeezeAutoSkims();
        squeezeTruckSkims();
        squeezePeakMcls();
        squeezeDailyMcls();
    }


}
