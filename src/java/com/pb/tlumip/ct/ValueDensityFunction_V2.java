package com.pb.tlumip.ct;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author crf <br/>
 *         Started: Aug 9, 2010 10:07:37 PM
 */
public class ValueDensityFunction_V2 {
    private final Map<String,ValueDensityFunctionParameters> paramMap;

    public ValueDensityFunction_V2(File f) {
        paramMap = new HashMap<String,ValueDensityFunctionParameters>();
        //value density parameters file is now a csv
        //columns are: commodity,mode,slope,vaf,ie_d2t,ei_d2t,ii_d2t,min_shipment_size  (value added factor, dollar 2 tons)
        TableDataSet vdp;
        try {
//            vdp = CSVFileReader.createReader(f).readTable("value_density_parameters");
            vdp = new CSVFileReader().readFile(f);
        } catch (IOException e) {
            throw new RuntimeException(e); //rethrow, of course
        }

        for (int i = 1; i <= vdp.getRowCount(); i++) {
            String key = vdp.getStringValueAt(i,"commodity")+vdp.getStringValueAt(i,"mode");
            double vaf = vdp.getValueAt(i,"vaf");
            double ie_d2t = vdp.getValueAt(i,"ie_d2t");
            double ei_d2t = vdp.getValueAt(i,"ei_d2t");
            double ii_d2t = vdp.getValueAt(i,"ii_d2t");
            double smallestAllowableTonnage = vdp.getValueAt(i,"min_shipment_size");
            paramMap.put(key,new ValueDensityFunctionParameters(vaf,ie_d2t,ei_d2t,ii_d2t,smallestAllowableTonnage));
        }

    }

    private class ValueDensityFunctionParameters {
        private final double vaf;
        private final double ie_d2t;
        private final double ei_d2t;
        private final double ii_d2t;
        private final double smallestAllowableTonnage;

        private ValueDensityFunctionParameters(double vaf, double ie_d2t, double ei_d2t, double ii_d2t, double smallestAllowableTonnage) {
            this.vaf = vaf;
            this.ie_d2t = ie_d2t;
            this.ei_d2t = ei_d2t;
            this.ii_d2t = ii_d2t;
            this.smallestAllowableTonnage = smallestAllowableTonnage;
        }
    }

    public double getVAF(String key) {
        ValueDensityFunctionParameters params = paramMap.get(key);
        return params.vaf;
    }

    public double getAnnualIETonsFactor (String key) {
        ValueDensityFunctionParameters params = paramMap.get(key);
        return params.vaf/params.ie_d2t;
    }

    public double getAnnualEITonsFactor (String key) {
        ValueDensityFunctionParameters params = paramMap.get(key);
        return params.vaf/params.ei_d2t;
    }

    public double getAnnualIITonsFactor (String key) {
        ValueDensityFunctionParameters params = paramMap.get(key);
         return params.vaf/params.ii_d2t;
    }

    public double getSmallestAllowableTonnage(String key) {
        return paramMap.get(key).smallestAllowableTonnage;
    }
}
