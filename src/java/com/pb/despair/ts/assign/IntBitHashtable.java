package com.pb.despair.ts.assign;

import com.pb.common.util.ObjectUtil;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A special hashtable which stores  'bit patterns' in 'int' values using
 * 'int' keys.
 *
 */

public class IntBitHashtable implements Serializable {

    public static int EMPTY_KEY = -1;
    public static int EMPTY_VALUE = -1;

    /**
     * The hash table keys.
     */
    private int[] keyTable;

    /**
     * The hash table data.
     */
    private int[] valueTable;

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
    public IntBitHashtable(int initialCapacity, float loadFactor) {

        if (initialCapacity <= 0) {
            initialCapacity = 1;
        }

        if (loadFactor <= 0) {
            loadFactor = 0.75f;
        }

        this.loadFactor = loadFactor;
        keyTable = new int[initialCapacity];
        valueTable = new int[initialCapacity];
        threshold = (int)(initialCapacity * loadFactor);

        Arrays.fill(keyTable, EMPTY_KEY);
    }


    /**
    * Constructs a new, empty hashtable with the specified initial capacity
    * and default load factor, which is <tt>0.75</tt>.
    */
    public IntBitHashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }


    /**
    * Constructs a new, empty hashtable with a default capacity and load
    * factor.
    */
    public IntBitHashtable() {
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
    public int put(int key, int value) {

        int hashCount;
        int index;

        //Rehash the table if the threshold is exceeded
        if (count >= threshold) {
            rehash();
        }


        //Makes sure the key is not already in the hashtable
        index = computeHashValue(key);

//        if (debug) {
//          System.out.println ("key=" + key + ", index=" + index + ", keyTable[index]=" + keyTable[index]);
//        }

        hashCount = 0;
        while (keyTable[index] != EMPTY_KEY) {

            if (key == keyTable[index]) {
                int oldValue = valueTable[index];
                valueTable[index] = value;
                return oldValue;
            }

            hashCount++;
            if (hashCount == keyTable.length) {
                rehash();
                index = computeHashValue(key);
            } else {
                index++;
                if (index == keyTable.length)
                    index = 0;
            }
        }

        //Store value
        keyTable[index] = key;
        valueTable[index] = value;
        count++;

        return EMPTY_VALUE;
    }


    /**
     * Returns the value to which the specified key is mapped in this hashtable.
     */
    public int get(int key) {
        int hashCount;
        int index = computeHashValue(key);

        hashCount = 0;
        while (keyTable[index] != EMPTY_KEY) {
            if (key == keyTable[index]) {
                return valueTable[index];
            }
            hashCount++;
            index++;

            if (hashCount == keyTable.length)
                break;

            if (index == keyTable.length)
                index = 0;
        }

        return EMPTY_VALUE;
    }


    /**
     * Returns true if the specified key is mapped in this hashtable, false otherwise.
     */
    public boolean containsKey(int key) {
        int hashCount;
        int index = computeHashValue(key);

        try {
            hashCount = 0;
            while (keyTable[index] != EMPTY_KEY) {
                if (key == keyTable[index]) {
                    return true;
                }
                index++;
                hashCount++;

                if (hashCount == keyTable.length)
                    break;

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
     */    public int remove(int key) {

        int index = computeHashValue(key);

        while (keyTable[index] != EMPTY_KEY) {
            if (key == keyTable[index]) {
                int oldValue = valueTable[index];
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
        int[] oldValues = valueTable;

        int newCapacity = (int)(oldCapacity + oldCapacity*0.75 + 1);
        threshold = (int) (newCapacity * loadFactor);

        keyTable = new int[newCapacity];
        valueTable = new int[newCapacity];
        Arrays.fill(keyTable, EMPTY_KEY);
        Arrays.fill(valueTable, EMPTY_VALUE);

//      if (debug) {
//        System.out.println ("rehash(), oldCapacity=" + oldCapacity + ", newCapacity=" + newCapacity + ", threshold=" + threshold + ", keyTable.length=" + keyTable.length);
//      }

        int newIndex;
        for (int i=0; i < oldCapacity; i++) {
            newIndex = computeHashValue(oldKeys[i]);
            while (keyTable[newIndex] != EMPTY_KEY) {
                newIndex++;
                if (newIndex == keyTable.length)
                    newIndex = 0;
            }
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
        int[] oldValues = valueTable;

        int newSize = (int)(size() + 0.20*size());

        keyTable = new int[newSize];
        valueTable = new int[newSize];
        Arrays.fill(keyTable, EMPTY_KEY);
        Arrays.fill(valueTable, EMPTY_VALUE);

//        if (debug) {
//          System.out.println ("trimHash(), size()=" + size() + ", newSize()=" + newSize + ", oldKeys.length=" + oldKeys.length + ", keyTable.length=" + keyTable.length);
//        }

        int newIndex;
        for (int i=0; i < oldKeys.length; i++) {
            if (oldKeys[i] != EMPTY_KEY) {
                newIndex = computeHashValue(oldKeys[i]);
                while (keyTable[newIndex] != EMPTY_KEY) {
                    newIndex++;
                    if (newIndex == keyTable.length)
                        newIndex = 0;
                }
                keyTable[newIndex] = oldKeys[i];
                valueTable[newIndex] = oldValues[i];
            }
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
    public int[] getValues() {
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

/*
        int nBits = 135;
        int someKey = 1001;
        int[] someArray = new int[20];
        int[] aSet = new int[20];


        IntBitHashtable table = new IntBitHashtable(20);


        //Create a bit set and set one bit to true
        for (int i=0; i < 20; i++)
          aSet[i] = 0;

        aSet[0].set(10);
        table.put(someKey, aSet[0]);
        System.out.println( "key=" + someKey + "  value=" + table.get(someKey) );

        //Update the bitset in the table
        aSet[1] = table.get(someKey);
        aSet[1].set(20);

        table.put(someKey, aSet[1]);
        System.out.println( "key=" + someKey + "  value=" + table.get(someKey) );

        table.debugOn();

        //Let the hash table be resized
        for (int i=0; i < 20; i++) {
          someArray[i] = (int)(Math.random()*100000);
          aSet[i] = (int)(Math.random()*1000);
          table.put(someArray[i], aSet[i]);
          table.print();
        }
*/

        IntBitHashtable b;

        b = new IntBitHashtable(0);
        System.out.println ("0 element IntBitHashtable size= " + ObjectUtil.sizeOf( b ));

        b = new IntBitHashtable(1);
        System.out.println ("1 element IntBitHashtable size= " + ObjectUtil.sizeOf( b ));

        b = new IntBitHashtable(2);
        System.out.println ("2 element IntBitHashtable size= " + ObjectUtil.sizeOf( b ));

        b = new IntBitHashtable(3);
        System.out.println ("3 element IntBitHashtable size= " + ObjectUtil.sizeOf( b ));
    }

}
