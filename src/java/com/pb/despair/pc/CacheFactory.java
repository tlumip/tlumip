package com.pb.despair.pc;




/** A factory method implementation to return the appropriate DataCache to a DataListener.
 *
 * @author    Christi Williosn
 * @version   1.0, 10/9/2003
 */
public class CacheFactory {



    private static CacheFactory instance = new CacheFactory();

    /** Keep this class from being created with "new".
     *
     */
    private CacheFactory() {
    }

    /** Return instances of this class.
     *
     */
    public static CacheFactory getInstance() {
        return instance;
    }



}