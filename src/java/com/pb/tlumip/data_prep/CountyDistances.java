/*
 * Copyright 2007 PB Americas
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Created on Mar 1, 2007 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.tlumip.data_prep;

import java.io.File;

import org.apache.log4j.Logger;

import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;

/**
 * @author Andrew Stryker
 * @version 0.1
 */
public class CountyDistances {
    private static Logger logger = Logger.getLogger(CountyDistances.class);

    /**
     * Squeeze an alpha distance matrix into county-to-county distances.
     * 
     * Usage: java com.pb.tlumip.data_prep.CountyDistances <correspondence file>
     * <alpha distance matrix> <output county distance matrix>
     * 
     * Where the correspondence file contains the fields "aZone" and
     * "countyNumber".
     * 
     * @param args
     * 
     */
    public static void main(String[] args) {
        AlphaToBeta alphaToBeta;
        MatrixCompression squeezer;
        MatrixWriter writer;
        Matrix alphaDistances;
        Matrix countyDistances;
        
        logger.info("Squeezing alpha distances distances to county-to-county.");
        
        logger.info("Reading the correspondence file in " + args[0]);
        alphaToBeta = new AlphaToBeta(new File(args[0]), "Azone", "countyNumber");
        
        logger.info("Reading the alpha distance matrix in " + args[1]);
        alphaDistances = MatrixReader.readMatrix(new File(args[1]), "opDist");
        
        logger.info("Squeezing the distance matrix.");
        squeezer = new MatrixCompression(alphaToBeta);
        countyDistances = squeezer.getCompressedMatrix(alphaDistances, "MEAN");
        
        logger.info("Writing county-to-county distance matrix to " + args[2]);
        writer = MatrixWriter.createWriter(args[2]);
        writer.writeMatrix(countyDistances);
        
        logger.info("All done.");
    }
}
