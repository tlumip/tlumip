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
/**
 * @(#) Link.java
 */

package com.pb.tlumip.ts.old;

/**
 * A class that represents ...
 * @see OtherClasses
 * @author your_name_here
 */
public class Link  implements LinkInterface
{
    /**
     * @supplierCardinality 1
     * @supplierRole from
     */
    private Node from;

    /**
     * @supplierCardinality 1
     * @supplierRole to
     */
    private Node to;
    public String toString() {return "link from "+from+" to "+to;};
}
