package com.pb.tlumip.ts.old;

import java.util.Iterator;
import java.util.Vector;
/**
 * A class that represents a node in a network
 * @author John Abraham
 */
public class Node implements NodeInterface
{

    private static final Vector allNodes = new Vector();
    public final int nodeNumber;

    private Node(int nodeNumber) {
       this.nodeNumber = nodeNumber;
       allNodes.add(this);
    }

    public static Node retrieveOrCreateNode(int nodeNumber) {
      Iterator it = allNodes.iterator();
      while (it.hasNext()) {
        Node other = (Node) it.next();
        if (other.nodeNumber == nodeNumber) {
          return other;
        }
      }
      return new Node(nodeNumber);
   }

    /**
     * @associates <{Link}>
     * @supplierCardinality 0..*
     */
    protected Vector arriving=new Vector();

    /**
     * @associates <{Link}>
     * @supplierCardinality 0..*
     */
    protected Vector departing = new Vector();

    public String toString() {return "node "+nodeNumber;};
    public int hashCode() {return nodeNumber;};

    public boolean equals(Object o) {
      Node other = (Node) o;
      if (other!=null) {
        if (other.nodeNumber==nodeNumber) return true;
      }
      return false;
    }
}
