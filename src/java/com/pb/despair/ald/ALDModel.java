package com.pb.despair.ald;

import com.pb.despair.model.ModelComponent;
import com.pb.common.util.ResourceUtil;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The ALD model component is actually a R program
 * and will be run in R
 * This class will use the Java Runtime class to
 * call the R program.
 * 
 * @author Christi Willison
 * @version Jun 2, 2004
 */
public class ALDModel extends ModelComponent {
    Logger logger = Logger.getLogger("pb.com.despair.ald");
    Process process;

    public void startModel(int t){
        /*  first we need to create the Strings that ALD uses as runtime arguments
            R expects 3 arguments:
                1. the path to the ald_inputs_XXX.R file which is located
                    in the same directory as the ald_XXX.R source code
                2.  the path to the input files (up to the scenario_Name directory)
                3.  the time interval (t) of the current simulation year
            Then we need the full path to the R code itself
        */
        String pathToRCode = ResourceUtil.getProperty(appRb, "codePath");
        String pathToRCodeArg = "-" + pathToRCode;

        String pathToIOFiles = ResourceUtil.getProperty(appRb, "filePath");
        String pathToIOFilesArg = "-" + pathToIOFiles;

        String yearArg = "-" + t;

        String rFileName = ResourceUtil.getProperty(appRb, "nameOfRCode");
        String rCode = pathToRCode + rFileName;

        String rOut = pathToIOFiles + "t" + t +"/ald/ald.Rout";

        String execCommand = "R CMD BATCH "+ pathToRCodeArg + " "
                                + pathToIOFilesArg + " " + yearArg + " " + rCode + " " + rOut;
        logger.info("Executing "+execCommand);
        Runtime rt = Runtime.getRuntime();
        try {
//            process = rt.exec("cmd.exe /c " + execCommand);
            process = rt.exec(execCommand);
            logger.info("ALD is done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ALDModel ald = new ALDModel();
        ald.setApplicationResourceBundle(ResourceUtil.getResourceBundle("ald"));
        ald.startModel(1);
    }

}
