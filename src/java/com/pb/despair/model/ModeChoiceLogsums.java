package com.pb.despair.model;

import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;
import java.io.Serializable;
import java.io.File;
import java.util.ResourceBundle;

/** A class that holds a Mode Choice logsum skim matrix for a 
    specific market segment in PT
    uses new Matrix package
 * @author Steve Hansen
 */
public class ModeChoiceLogsums implements Serializable{
     public Matrix logsums;
     protected static Logger logger = Logger.getLogger("com.pb.despair.pt");
    ResourceBundle rb;



     public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        ModeChoiceLogsums mcls = new ModeChoiceLogsums(rb);
        mcls.readLogsums('w',1);
     }

     public ModeChoiceLogsums(ResourceBundle rb) {
         this.rb = rb;

     }

     public void readLogsums(char purpose,int market){
          //get path to skims
          String path = ResourceUtil.getProperty(rb, "mcLogsum.path");

          //construct name of file to read
          String name = new String(path+purpose+new Integer(market).toString()+"ls.zip");          

            
          MatrixReader logsumReader= MatrixReader.createReader(MatrixType.ZIP,new File(name)); 
          logsums = logsumReader.readMatrix(name);

     }

    public void readBinaryLogsums(char purpose,int market){
          //get path to skims
          String path = ResourceUtil.getProperty(rb, "mcLogsum.path");

          //construct name of file to read
          String name = new String(path+purpose+new Integer(market).toString()+"ls.binary");


          MatrixReader logsumReader= MatrixReader.createReader(MatrixType.BINARY,new File(name));
          logsums = logsumReader.readMatrix(name);

     }

    public float getLogsum(int originTaz, int destinationTaz){
         float logsum  = logsums.getValueAt(originTaz,destinationTaz);
         return logsum;
    }

    public Matrix getMatrix(){        
         return logsums;
    }


}
