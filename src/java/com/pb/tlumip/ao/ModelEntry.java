package com.pb.tlumip.ao;

import com.pb.common.util.ResourceUtil;
import com.pb.models.reference.ModelComponent;
import com.pb.tlumip.aa.AAModel;
import com.pb.tlumip.ald.ALDModel;
import com.pb.tlumip.ct.CTModel;
import com.pb.tlumip.ed.NEDModel;
import com.pb.tlumip.et.ETPythonModel;
import com.pb.tlumip.sl.SelectLink;
import com.pb.tlumip.spg.SPGnew;
import com.pb.tlumip.ts.TSModelComponent;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * The {@code ModelEntry} ...
 *
 * @author crf
 *         Started 2/4/13 3:42 PM
 */
public enum ModelEntry {
    NED(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    ALD(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    SPG1(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    AA(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    SPG2(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    PT(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    CT(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    ET(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    TS(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    SL(ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    VIZ(Arrays.asList(ModelEntryParameterKeys.VIZ_YEARS),
        ModelEntryParameterKeys.PROPERTY_FILE_PATH),
    MICROVIZ(Arrays.asList(ModelEntryParameterKeys.VIZ_YEARS),
             ModelEntryParameterKeys.PROPERTY_FILE_PATH);

    private final Set<String> requiredParameterKeys;
    private final Set<String> optionalParameterKeys;

    private ModelEntry(List<String> optionalParameters, String ... requiredParameterKeys) {
        this.requiredParameterKeys = new LinkedHashSet<String>(Arrays.asList(requiredParameterKeys));
        this.optionalParameterKeys = new LinkedHashSet<String>(optionalParameters);
    }

    private ModelEntry(String ... requiredParameterKeys) {
        this(new LinkedList<String>(),requiredParameterKeys);
    }

    public static class ModelEntryParameterKeys {
        public static final String PROPERTY_FILE_PATH = "property_file";
        public static final String VIZ_YEARS = "viz_years";
    }


    private void checkKeys(Map<String,String> parameters) {
        Set<String> missingKeys = new HashSet<String>();
        for (String requiredKey : requiredParameterKeys)
            if (!parameters.containsKey(requiredKey))
                missingKeys.add(requiredKey);
        if (missingKeys.size() > 0)
            throw new IllegalArgumentException("Missing required parameters for " + this + ": " + missingKeys.toString());
    }

    private ResourceBundle getResourceBundle(Map<String,String> parameters) {
        return ResourceUtil.getPropertyBundle(new File(parameters.get(ModelEntryParameterKeys.PROPERTY_FILE_PATH)));
    }

    private List<String> getVizYears(Map<String,String> parameters) {
        return (parameters.containsKey(ModelEntryParameterKeys.VIZ_YEARS)) ?
                   Arrays.asList(parameters.get(ModelEntryParameterKeys.VIZ_YEARS).split(",")) : null;
    }

    private int getBaseYear(Map<String,String> parameters) {
        return ResourceUtil.getIntegerProperty(getResourceBundle(parameters),"base.year");
    }

    private String getRootDir(Map<String,String> parameters) {
        return ResourceUtil.getProperty(getResourceBundle(parameters),"root.dir");
    }

    private int getTYear(Map<String,String> parameters) {
        return ResourceUtil.getIntegerProperty(getResourceBundle(parameters),"t.year");
    }

    private boolean getTsDaily(Map<String,String> parameters) {
        return ResourceUtil.getBooleanProperty(getResourceBundle(parameters),"ts.daily.assignment");
    }

    private String getTsDafConfigFile(Map<String,String> parameters) {
        return getResourceBundle(parameters).getString("ts.daf.config.file");
    }

    private String getSlMode(Map<String,String> parameters) {
        return getResourceBundle(parameters).getString("sl.mode");
    }

    private void runNed(Map<String,String> parameters) {
        NED.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        new NEDModel(resourceBundle,resourceBundle).startModel(getBaseYear(parameters),getTYear(parameters));
    }

    private void runAld(Map<String,String> parameters) {
        ALD.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        ModelComponent comp = new ALDModel();
        comp.setApplicationResourceBundle(resourceBundle);
        comp.startModel(getBaseYear(parameters),getTYear(parameters));
    }

    private void runSpg1(Map<String,String> parameters) {
        SPG1.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        int baseYearNumber = getBaseYear(parameters);
        String baseYear = Integer.toString(baseYearNumber);
        String currentYear = Integer.toString(baseYearNumber+getTYear(parameters));

        SPGnew spg = new SPGnew(resourceBundle,resourceBundle,baseYear,currentYear);

        if (spg.isPersonAgeConstraintEnabled()) {
            spg.getHHAttributeData(baseYear);
            //run once with age constraint off to get population value
            spg.disablePersonAgeConstraint();
            spg.spg1(currentYear);
            spg.setPopulationTotal(spg.getPopulationTotal());
            //enable age constraint, for final run
            spg.enablePersonAgeConstraint();
            spg.resetSPG1BalancingCount();
        }
        spg.getHHAttributeData(baseYear); //reset for second spg run
        spg.spg1(currentYear);
        spg.writePiInputFile(spg.sumHouseholdsByIncomeSize());
    }

    private void runAa(Map<String,String> parameters) {
        AA.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        //NOTE: AA assumes aa.properties in classpath - so the folder this file is in needs to be in the classpath
        File aaPropertyFile = new File(resourceBundle.getString("aa.property.file"));
        File propertyFile = new File(parameters.get(ModelEntryParameterKeys.PROPERTY_FILE_PATH));
        if (!propertyFile.equals(aaPropertyFile)) //property file is not the same as the one aa needs, so copy it
            copyFile(propertyFile,aaPropertyFile);

        new AAModel(resourceBundle,resourceBundle).startModel(getBaseYear(parameters),getTYear(parameters));
    }

    private void copyFile(File sourceFile, File destFile) {
        FileInputStream fIn = null;
        FileOutputStream fOut = null;
        FileChannel source = null;
        FileChannel destination = null;
        try {
            fIn = new FileInputStream(sourceFile);
            source = fIn.getChannel();
            fOut = new FileOutputStream(destFile);
            destination = fOut.getChannel();
            long transfered = 0;
            long bytes = source.size();
            while (transfered < bytes)
                destination.position(transfered += destination.transferFrom(source,0,source.size()));
        }  catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            simpleClose(source);
            simpleClose(fIn);
            simpleClose(destination);
            simpleClose(fOut);
        }
    }

    private void simpleClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }

    private void runSpg2(Map<String,String> parameters) {
        SPG2.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        int baseYearNumber = getBaseYear(parameters);
        String baseYear = Integer.toString(baseYearNumber);
        String currentYear = Integer.toString(baseYearNumber+getTYear(parameters));

        SPGnew spg = new SPGnew(resourceBundle,resourceBundle,baseYear,currentYear );
        spg.spg2();
        spg.writeZonalSummaryToCsvFile();
        spg.writeHHOutputAttributes(baseYear);
    }

    public void writeRunParamsToPropertiesFile(Map<String,String> parameters){
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        int baseYear = getBaseYear(parameters);
        int tYear = getTYear(parameters);
        String propertyFile = parameters.get(ModelEntryParameterKeys.PROPERTY_FILE_PATH);
        String scenarioName = resourceBundle.getString("scenario.name");

        File runParams = new File(resourceBundle.getString("pt.daf.run.params.file"));
        PrintWriter writer;
        try {
            writer = new PrintWriter(new FileWriter(runParams));
            writer.println("scenarioName=" + scenarioName);
            writer.println("baseYear=" + baseYear);
            writer.println("timeInterval=" + tYear);
            writer.println("pathToAppRb=" + propertyFile);
            writer.println("pathToGlobalRb=" + propertyFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open the RunParams file", e);
        }
        writer.close();
    }

    private void runPt(Map<String,String> parameters) {
        PT.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        writeRunParamsToPropertiesFile(parameters);
        new StartDafApplication("ptdaf",resourceBundle,getTYear(parameters)).run();
    }

    private void runCt(Map<String,String> parameters) {
        CT.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        new CTModel(resourceBundle,resourceBundle).startModel(getBaseYear(parameters),getTYear(parameters));
    }

    private void runEt(Map<String,String> parameters) {
        ET.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        new ETPythonModel(resourceBundle,resourceBundle).startModel(getBaseYear(parameters),getTYear(parameters));
    }

    private void runTs(Map<String,String> parameters) {
        TS.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        new TSModelComponent(resourceBundle,resourceBundle,getTsDafConfigFile(parameters),getTsDaily(parameters))
                .startModel(getBaseYear(parameters),getTYear(parameters));
    }

    private void runSl(Map<String,String> parameters) {
        SL.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        new SelectLink(resourceBundle,getTYear(parameters)).runStages(getSlMode(parameters));
    }

    private void runViz(Map<String,String> parameters) {
        VIZ.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        List<String> vizYears = getVizYears(parameters);
        new VizDbBuilder(resourceBundle,vizYears).buildVizDb();
    }

    private void runMicroViz(Map<String,String> parameters) {
        MICROVIZ.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        List<String> vizYears = getVizYears(parameters);
        new VizDbBuilder(resourceBundle,vizYears).buildMicroVizDb();
    }

    private static String usage() {
        StringBuilder builder = new StringBuilder("ModelEntry usage:\n");
        builder.append("java ... com.pb.tlumip.ao.ModelEntry model key1=parameter1 ...\n");
        builder.append("  where models and keys (r = required) are:\n");
        builder.append("  Model        Parameters \n");
        builder.append("  -----        -----------\n");
        for (ModelEntry entry : ModelEntry.values()) {
            String entryName = entry.name();
            int gap = 13 - entryName.length();
            builder.append("  ").append(entryName);
            for (int i = 0; i < gap; i++)
                builder.append(" ");
            boolean first = true;
            for (String param : entry.requiredParameterKeys) {
                if (first)
                    first = false;
                else
                    builder.append("               ");
                builder.append(param).append(" (r)\n");
            }
            for (String param : entry.optionalParameterKeys) {
                if (first)
                    first = false;
                else
                    builder.append("               ");
                builder.append(param).append("\n");
            }
        }
        return builder.toString();
    }

    private static Map<String,String> parseParameters(String ... args) {
        Map<String,String> parameters = new HashMap<String,String>();
        //skip first one
        for (int i = 1; i < args.length; i++) {
            String[] split = args[i].split("=",2);
            parameters.put(split[0].trim(),split.length > 1 ? split[1].trim() : "");
        }
        return parameters;
    }

    public static void main(String ... args) {
        if (args.length < 1)
            throw new IllegalArgumentException(usage());
        ModelEntry model;
        try {
            model = ModelEntry.valueOf(args[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown model type: " + args[0] + "\n" + usage());
        }
        Map<String,String> parameters = parseParameters(args);
        switch (model) {
            case NED      :  NED.runNed(parameters); break;
            case ALD      :  ALD.runAld(parameters); break;
            case SPG1     :  SPG1.runSpg1(parameters); break;
            case AA       :  AA.runAa(parameters); break;
            case SPG2     :  SPG2.runSpg2(parameters); break;
            case PT       :  PT.runPt(parameters); break;
            case CT       :  CT.runCt(parameters); break;
            case ET       :  ET.runEt(parameters); break;
            case TS       :  TS.runTs(parameters); break;
            case SL       :  SL.runSl(parameters); break;
            case VIZ      :  VIZ.runViz(parameters); break;
            case MICROVIZ :  MICROVIZ.runMicroViz(parameters); break;
        }
    }
}
