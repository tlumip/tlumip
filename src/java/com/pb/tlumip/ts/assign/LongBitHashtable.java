/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.ts.assign;

import java.util.Arrays;

/**
 * A special hashtable which stores only 'bit' values using 'int' keys.
 *
 */

public class LongBitHashtable {

    public static int EMPTY_KEY = -1;
    public static long EMPTY_VALUE = -1;

    /**
     * The hash table keys.
     */
    private int[] keyTable;

    /**
     * The hash table data.
     */
    private long[] valueTable;

    /**
     * The total number of entries in the hash table.
     */
    private int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     */
    private int threshold;

    /**
     * The load factor for the hashtable.
     */
    private float loadFactor;

    boolean debug;

    public void debugOn () {
        debug = true;
    }

    public void debugOff () {
        debug = false;
    }


    /**
    * Constructs a new, empty hashtable with the specified initial
    * capacity and the specified load factor.
    */
    public LongBitHashtable(int initialCapacity, float loadFactor) {

        if (initialCapacity <= 0) {
            initialCapacity = 1;
        }

        if (loadFactor <= 0) {
            loadFactor = 0.75f;
        }

        this.loadFactor = loadFactor;
        keyTable = new int[initialCapacity];
        valueTable = new long[initialCapacity];
        threshold = (int)(initialCapacity * loadFactor);

        Arrays.fill(keyTable, EMPTY_KEY);
    }


    /**
    * Constructs a new, empty hashtable with the specified initial capacity
    * and default load factor, which is <tt>0.75</tt>.
    */
    public LongBitHashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }


    /**
    * Constructs a new, empty hashtable with a default capacity and load
    * factor.
    */
    public LongBitHashtable() {
        this(40, 0.75f);
    }


    /**
     * Returns the number of keys in this hashtable.
     */
    public int size() {
        return count;
    }


    /**
     * Returns the capacity of this hashtable.
     */
    public int capacity() {
        return keyTable.length;
    }


    /**
     * Tests if this hashtable is empty.
     */
    public boolean isEmpty() {
        return count == 0;
    }


    /**
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable.
     *
     * The value can be retrieved by calling the <code>get</code>
     * method with a key that is equal to the original key.
     */
    public long put(int key, long value) {

        //Rehash the table if the threshold is exceeded
        if (count >= threshold) {
            rehash();
        }


        //Makes sure the key is not already in the hashtable
        int index = computeHashValue(key);

        if (debug) {
            System.out.println ("key=" + key + ", index=" + index + ", keyTable[index]=" + keyTable[index]);
        }

        while (keyTable[index] != EMPTY_KEY) {

            if (key == keyTable[index]) {
                long oldValue = valueTable[index];
                valueTable[index] = value;

                if (debug) {
                    System.out.println ("returning after finding index=" + index);
                }

                return oldValue;
            }
            index++;
            if (index == keyTable.length)
                index = 0;
        }

        if (debug) {
            System.out.println ("empty index found=" + index + ", count=" + count + ", threshold=" + threshold);
        }


        //Store value
        keyTable[index] = key;
        valueTable[index] = value;
        count++;

        if (debug) {
            System.out.println ("returning");
        }

        return EMPTY_VALUE;
    }


    /**
     * Returns the value to which the specified key is mapped in this hashtable.
     */
    public long get(int key) {
        int index = computeHashValue(key);

        while (keyTable[index] != EMPTY_KEY) {
            if (key == keyTable[index]) {
                return valueTable[index];
            }
            index++;
            if (index == keyTable.length)
                index = 0;
        }

        return EMPTY_VALUE;
    }


    /**
     * Returns true if the specified key is mapped in this hashtable, false otherwise.
     */
    public boolean containsKey(int key) {
        int index = computeHashValue(key);

        try {
            while (keyTable[index] != EMPTY_KEY) {
                if (key == keyTable[index]) {
                    return true;
                }
                index++;
                if (index == keyTable.length)
                    index = 0;
            }
            return false;
        } catch ( Exception x ) {
            System.out.println ("key="+key + "  index="+index);
            System.out.flush ();
        }
        return false;
    }


    /**
     * Removes the key (and its corresponding value) from this
     * hashtable. This method does nothing if the key is not in
     * the hashtable.
     */
    public long remove(int key) {

        int index = computeHashValue(key);

        while (keyTable[index] != EMPTY_KEY) {
            if (key == keyTable[index]) {
                long oldValue = valueTable[index];
                valueTable[index] = EMPTY_VALUE;
                count--;
                return oldValue;
            }
            index++;
            if (index == keyTable.length)
                index = 0;
        }
        return EMPTY_VALUE;
    }


    /**
    * Compute a hash code/index for int key values.
    */
    private int computeHashValue(int key) {

        //TODO compute a better index
        int index = (key & 0x7FFFFFFF) % keyTable.length;
        return index;
    }


    /**
     * Increases the capacity of and internally reorganizes this
     * hashtable, in order to accommodate and access its entries more
     * efficiently.  This method is called automatically when the
     * number of keys in the hashtable exceeds this hashtable's capacity
     * and load factor.
     */
    protected void rehash() {

        int oldCapacity = valueTable.length;
        int[] oldKeys = keyTable;
        long[] oldValues = valueTable;

        int newCapacity = oldCapacity*2 + 1;
        threshold = (int) (newCapacity * loadFactor);

        keyTable = new int[newCapacity];
        valueTable = new long[newCapacity];
        Arrays.fill(keyTable, EMPTY_KEY);
        Arrays.fill(valueTable, EMPTY_VALUE);

        if (debug) {
            System.out.println ("rehash(), oldCapacity=" + oldCapacity + ", newCapacity=" + newCapacity + ", threshold=" + threshold + ", keyTable.length=" + keyTable.length);
        }

        int newIndex;
        for (int i=0; i < oldCapacity; i++) {
            newIndex = computeHashValue(oldKeys[i]);
            keyTable[newIndex] = oldKeys[i];
            valueTable[newIndex] = oldValues[i];
        }
    }


    /**
     * Reduces the capacity of this hashtable to accommodate the actual
     * final number of elements.  Should be called after filling the
     * hash table for each o/d.
     */
    public void trimHash() {

        int[] oldKeys = keyTable;
        long[] oldValues = valueTable;

        keyTable = new int[size()];
        valueTable = new long[size()];
        Arrays.fill(keyTable, EMPTY_KEY);
        Arrays.fill(valueTable, EMPTY_VALUE);

        if (debug) {
            System.out.println ("trimHash(), size()=" + size() + ", oldKeys.length=" + oldKeys.length + ", keyTable.length=" + keyTable.length);
        }

        int newIndex;
        for (int i=0; i < size(); i++) {
            newIndex = computeHashValue(oldKeys[i]);
            keyTable[newIndex] = oldKeys[i];
            valueTable[newIndex] = oldValues[i];
        }
    }


    /**
    * Return a copy of the key table.
    */
    public int[] getKeys() {
        return keyTable;
    }


    /**
    * Return a copy of the value table.
    */
    public long[] getValues() {
        return valueTable;
    }


    /**
     * Clears this hashtable so that it contains no keys.
     */
    public void clear() {
        Arrays.fill(keyTable, EMPTY_KEY);
    }


    /**
     * Returns a string representation of this hasg table object.
     */
    public String toString() {
        String returnString;

        returnString = "key[0]=" + keyTable[0] + ", valueTable[0]=" + valueTable[0];
        for (int i=1; i < size(); i++)
            returnString += ", key[" + i + "]=" + keyTable[i] + ", valueTable[" + i + "]=" + valueTable[i];

        return returnString;
    }


    /**
     * prints a listing of the hash table object to stdout.
     */
    public void print() {

        System.out.println ("");
        System.out.println ("");
        for (int i=0; i < keyTable.length; i++)
            System.out.println (i + "  " + keyTable[i] + "   " + valueTable[i] + "   " + computeHashValue(keyTable[i]));
        System.out.println ("");
        System.out.println ("");

    }





    public static void main(String[] args) {

        int nBits = 135;
        int someKey = 1001;
        int[] someArray = new int[20];
        long[] aSet = new long[20];

        LongBitHashtable table = new LongBitHashtable(20);

        //Create a bit set and set one bit to true
        for (int i=0; i < 20; i++)
            aSet[i] = 0;

/*
        aSet[0].set(10);
        table.put(someKey, aSet[0]);
        System.out.println( "key=" + someKey + "  value=" + table.get(someKey) );

        //Update the bitset in the table
        aSet[1] = table.get(someKey);
        aSet[1].set(20);

        table.put(someKey, aSet[1]);
        System.out.println( "key=" + someKey + "  value=" + table.get(someKey) );
*/

        table.debugOn();

        //Let the hash table be resized
        for (int i=0; i < 20; i++) {
            someArray[i] = (int)(Math.random()*100000);
            aSet[i] = (long)(Math.random()*1000);
            table.put(someArray[i], aSet[i]);
            table.print();
        }
    }

}

