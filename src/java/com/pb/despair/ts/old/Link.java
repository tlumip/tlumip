/**
 * @(#) Link.java
 */

package com.pb.despair.ts.old;

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
