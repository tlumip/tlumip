package com.pb.tlumip.ts;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Utility class for TS. Includes:
 *      methods to convert primitive typed arrays to ArrayLists.
 *      methods to convert ArrayLists to primitive typed arrays.
 *      
 * @author JHicks
 *
 */
public class TsUtil {

    /**
     * return an int[] of the values in the list.
     * @param list
     * @return int[]
     */
    public static int[] intArray ( ArrayList list ) {
        
        int[] array = new int[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Integer)list.get(i);
        }
        return array;

    }
    
    /**
     * return an int[][] of the values in the list of lists.
     * @param list
     * @return int[][]
     */
    public static int[][] int2Array ( ArrayList list ) {
        
        int[][] array = new int[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = (ArrayList)list.get(i);
            array[i] = new int[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Integer)tempList.get(j);
        }
        return array;

    }
    
    /**
     * return a double[] of the values in the list.
     * @param list
     * @return double[]
     */
    public static double[] doubleArray ( ArrayList list ) {
        
        double[] array = new double[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Double)list.get(i);
        }
        return array;

    }
    
    /**
     * return a double[][] of the values in the list of lists.
     * @param list
     * @return double[][]
     */
    public static double[][] double2Array ( ArrayList list ) {
        
        double[][] array = new double[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = (ArrayList)list.get(i);
            array[i] = new double[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Double)tempList.get(j);
        }
        return array;

    }
    
    /**
     * return a double[][][] of the values in the list of lists of lists.
     * @param list
     * @return double[][][]
     */
    public static double[][][] double3Array ( ArrayList list ) {
        
        double[][][] array = new double[list.size()][][];
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList1 = (ArrayList)list.get(i);
            array[i] = new double[tempList1.size()][];
            for ( int j=0; j < array[i].length; j++ ) {
                ArrayList tempList2 = (ArrayList)tempList1.get(j);
                array[i][j] = new double[tempList2.size()];
                for ( int k=0; k < array[i][j].length; k++ )
                    array[i][j][k] = (Double)tempList2.get(k);
            }
        }
        return array;

    }
    
    /**
     * return a char[] of the values in the list.
     * @param list
     * @return char[]
     */
    public static char[] charArray ( ArrayList list ) {
        
        char[] array = new char[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Character)list.get(i);
        }
        return array;

    }
    
    /**
     * return a char[][] of the values in the list of lists.
     * @param list
     * @return char[][]
     */
    public static char[][] char2Array ( ArrayList list ) {
        
        char[][] array = new char[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = (ArrayList)list.get(i);
            array[i] = new char[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Character)tempList.get(j);
        }
        return array;

    }
    
    /**
     * return a boolean[] of the values in the list.
     * @param list
     * @return boolean[]
     */
    public static boolean[] booleanArray ( ArrayList list ) {
        
        boolean[] array = new boolean[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Boolean)list.get(i);
        }
        return array;

    }
    
    /**
     * return a boolean[][] of the values in the list of lists.
     * @param list
     * @return boolean[][]
     */
    public static boolean[][] boolean2Array ( ArrayList list ) {
        
        boolean[][] array = new boolean[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = (ArrayList)list.get(i);
            array[i] = new boolean[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Boolean)tempList.get(j);
        }
        return array;

    }
    
    /**
     * return a String[] of the values in the list.
     * @param list
     * @return String[]
     */
    public static String[] stringArray ( ArrayList list ) {
        
        String[] array = new String[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (String)list.get(i);
        }
        return array;

    }
    
    /**
     * return a list of the values in the int[].
     * @param array
     * @return list
     */
    public static ArrayList intList ( int[] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the int[][].
     * @param array
     * @return list
     */
    public static ArrayList int2List ( int[][] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = new ArrayList(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the double[].
     * @param array
     * @return list
     */
    public static ArrayList doubleList ( double[] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the double[][].
     * @param array
     * @return list
     */
    public static ArrayList double2List ( double[][] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = new ArrayList(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the double[][][].
     * @param array
     * @return list
     */
    public static ArrayList double3List ( double[][][] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList1 = new ArrayList(array[i].length);
            for ( int j=0; j < array[i].length; j++ ) {
                ArrayList tempList2 = new ArrayList(array[i][j].length);
                for ( int k=0; k < array[i][j].length; k++ )
                    tempList2.add(k,array[i][j][k]);
                tempList1.add(j,tempList2);
            }
            list.add(i,tempList1);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the char[].
     * @param array
     * @return list
     */
    public static ArrayList charList ( char[] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the char[][].
     * @param array
     * @return list
     */
    public static ArrayList char2List ( char[][] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = new ArrayList(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the boolean[].
     * @param array
     * @return list
     */
    public static ArrayList booleanList ( boolean[] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a list of the values in the boolean[][].
     * @param array
     * @return list
     */
    public static ArrayList boolean2List ( boolean[][] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            ArrayList tempList = new ArrayList(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }

    /**
     * return a list of the values in the String[].
     * @param array
     * @return list
     */
    public static ArrayList stringList ( String[] array ) {
        
        ArrayList list = new ArrayList(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a Vector of the values in the int[].
     * @param array
     * @return Vector
     */
    public static Vector intVector ( int[] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a Vector of Vectors of the values in the int[][].
     * @param array
     * @return Vector
     */
    public static Vector int2Vector ( int[][] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = new Vector(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }
   
    /**
     * return a Vector of the values in the double[].
     * @param array
     * @return Vector
     */
    public static Vector doubleVector ( double[] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a Vector of Vectors of the values in the double[][].
     * @param array
     * @return Vector
     */
    public static Vector double2Vector ( double[][] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = new Vector(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }
   
    /**
     * return a Vector of Vectors of Vectors of the values in the double[][][].
     * @param array
     * @return Vector
     */
    public static Vector double3Vector ( double[][][] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList1 = new Vector(array[i].length);
            for ( int j=0; j < array[i].length; j++ ) {
                Vector tempList2 = new Vector(array[i][j].length);
                for ( int k=0; k < array[i][j].length; k++ )
                    tempList2.add(k,array[i][j][k]);
                tempList1.add(j,tempList2);
            }
            list.add(i,tempList1);
        }
        return list;
        
    }

    /**
     * return a Vector of the values in the char[].
     * @param array
     * @return Vector
     */
    public static Vector charVector ( char[] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a Vector of Vectors of the values in the char[][].
     * @param array
     * @return Vector
     */
    public static Vector char2Vector ( char[][] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = new Vector(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }
    
    /**
     * return a Vector of the values in the boolean[].
     * @param array
     * @return Vector
     */
    public static Vector booleanVector ( boolean[] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return a Vector of Vectors of the values in the boolean[][].
     * @param array
     * @return Vector
     */
    public static Vector boolean2Vector ( boolean[][] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = new Vector(array[i].length);
            for (int j=0; j < array[i].length; j++)
                tempList.add(j,array[i][j]);
            list.add(i,tempList);
        }
        return list;
        
    }

    /**
     * return a Vector of the values in the String[].
     * @param array
     * @return Vector
     */
    public static Vector stringVector ( String[] array ) {
        
        Vector list = new Vector(array.length);
        for ( int i=0; i < array.length; i++ ) {
            list.add(i,array[i]);
        }
        return list;
        
    }
    
    /**
     * return an int[] of the values in the Vector.
     * @param Vector
     * @return int[]
     */
    public static int[] vectorInt ( Vector list ) {
        
        int[] array = new int[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Integer)list.get(i);
        }
        return array;

    }
    
    /**
     * return an int[][] of the values in the Vector of Vectors.
     * @param Vector
     * @return int[][]
     */
    public static int[][] vectorInt2 ( Vector list ) {
        
        int[][] array = new int[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = (Vector)list.get(i);
            array[i] = new int[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Integer)tempList.get(j);
        }
        return array;

    }
    
    /**
     * return a double[] of the values in the Vector.
     * @param Vector
     * @return double[]
     */
    public static double[] vectorDouble ( Vector list ) {
        
        double[] array = new double[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Double)list.get(i);
        }
        return array;

    }
    
    /**
     * return a double[][] of the values in the Vector of Vectors.
     * @param Vector
     * @return double[][]
     */
    public static double[][] vectorDouble2 ( Vector list ) {
        
        double[][] array = new double[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = (Vector)list.get(i);
            array[i] = new double[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Double)tempList.get(j);
        }
        return array;
        
    }
    
    /**
     * return a double[][][] of the values in the Vector of Vectors of Vectors.
     * @param Vector
     * @return double[][][]
     */
    public static double[][][] vectorDouble3 ( Vector list ) {
        
        double[][][] array = new double[list.size()][][];
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList1 = (Vector)list.get(i);
            array[i] = new double[tempList1.size()][];
            for ( int j=0; j < array[i].length; j++ ) {
                Vector tempList2 = (Vector)tempList1.get(j);
                array[i][j] = new double[tempList2.size()];
                for ( int k=0; k < array[i][j].length; k++ )
                    array[i][j][k] = (Double)tempList2.get(k);
            }
        }
        return array;

    }
    
    /**
     * return a char[] of the values in the Vector.
     * @param Vector
     * @return char[]
     */
    public static char[] vectorChar ( Vector list ) {
        
        char[] array = new char[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Character)list.get(i);
        }
        return array;

    }
    
    /**
     * return a char[][] of the values in the Vector of Vectors.
     * @param Vector
     * @return char[][]
     */
    public static char[][] vectorChar2 ( Vector list ) {
        
        char[][] array = new char[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = (Vector)list.get(i);
            array[i] = new char[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Character)tempList.get(j);
        }
        return array;

    }
    
    /**
     * return a boolean[] of the values in the Vector.
     * @param Vector
     * @return boolean[]
     */
    public static boolean[] vectorBoolean ( Vector list ) {
        
        boolean[] array = new boolean[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (Boolean)list.get(i);
        }
        return array;

    }
    
    /**
     * return a boolean[][] of the values in the Vector of Vectors.
     * @param Vector
     * @return boolean[][]
     */
    public static boolean[][] vectorBoolean2 ( Vector list ) {
        
        boolean[][] array = new boolean[list.size()][];
        for ( int i=0; i < array.length; i++ ) {
            Vector tempList = (Vector)list.get(i);
            array[i] = new boolean[tempList.size()];
            for ( int j=0; j < array[i].length; j++ )
                array[i][j] = (Boolean)tempList.get(j);
        }
        return array;

    }
    
    /**
     * return a String[] of the values in the Vector.
     * @param Vector
     * @return String[]
     */
    public static String[] vectorString ( Vector list ) {
        
        String[] array = new String[list.size()];
        for ( int i=0; i < array.length; i++ ) {
            array[i] = (String)list.get(i);
        }
        return array;

    }
    
}