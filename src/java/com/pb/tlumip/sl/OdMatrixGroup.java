package com.pb.tlumip.sl;

import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.RowVector;

import java.util.HashMap;
import java.util.Map;

class OdMatrixGroup {
    private final Matrix[] matrices;
    private Map<String,Integer> zoneMatrixMap;

    OdMatrixGroup() {
        matrices = new Matrix[4];
    }

    public int getClassCount() {
        return matrices.length;
    }

    public Matrix getMatrix(int period) {
        return matrices[period];
    }

    public void setZoneMatrixMap(Map<String,Integer> zoneMatrixMap) {
        this.zoneMatrixMap = zoneMatrixMap;
    }

    public Map<String,Integer> getZoneMatrixMap() {
        return zoneMatrixMap;
    }

    //for debugging
    public String getValueFromZone(int zone) {
        for (String key : zoneMatrixMap.keySet())
            if (zoneMatrixMap.get(key) == zone)
                return key;
        return null;
    }

    public void initMatrix(Matrix baseMatrix, int period, boolean copy) {
        Matrix m = new Matrix(baseMatrix.getRowCount(), baseMatrix.getColumnCount());
        float[][] values = m.getValues();
        float[][] baseValues = baseMatrix.getValues();

        if (copy)
            for (int j = 0; j < baseMatrix.getRowCount(); j++)
                System.arraycopy(baseValues[j], 0, values[j], 0, baseValues[j].length);
        matrices[period] = m;
    }

    public void initMatrix(Matrix baseMatrix, int period) {
        initMatrix(baseMatrix,period,true);
    }

    static class OdMatrixGroupCollection extends HashMap<String,OdMatrixGroup> {
        private final OdMatrixGroup template;

        public OdMatrixGroupCollection(OdMatrixGroup template) {
            super();
            this.template = template;
        }

        OdMatrixGroup getTemplate() {
            return template;
        }

        public OdMatrixGroup get(Object key) {
            if (!containsKey(key)) {
                OdMatrixGroup g = template instanceof OdMarginalMatrixGroup ? new OdMarginalMatrixGroup() : new OdMatrixGroup();
                for (int i = 0; i < 4; i++)
                    g.initMatrix(template.getMatrix(i),i,false);
                g.setZoneMatrixMap(template.getZoneMatrixMap());
                put((String) key,g);
            }
            return super.get(key);
        }
    }
    
    static class OdMarginalMatrixGroup extends OdMatrixGroup {
        private final ColumnVector[] originMarginals;
        private final RowVector[] destinationMarginals;

        OdMarginalMatrixGroup() {
            originMarginals = new ColumnVector[4];
            destinationMarginals = new RowVector[4];
        }
        
        public void initMatrix(Matrix baseMatrix, int period, boolean copy) {
            super.initMatrix(baseMatrix,period,copy);
            originMarginals[period] = new ColumnVector(baseMatrix.getRowCount());
            destinationMarginals[period] = new RowVector(baseMatrix.getColumnCount());
        }

        public void initMatrix(Matrix baseMatrix, int period) {
            initMatrix(baseMatrix,period,true);
        }
        
        public ColumnVector getOriginMarginals(int period) {
            return originMarginals[period];
        }                                  
        
        public RowVector getDestinationMarginals(int period) {
            return destinationMarginals[period];
        }
    }
}