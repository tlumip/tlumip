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
package com.pb.tlumip.pc;




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