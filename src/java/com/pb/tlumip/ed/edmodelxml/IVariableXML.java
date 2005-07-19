/**
 * IVariableXML.java	Java 1.4.2_07 Tue Jul 19 13:08:03 MDT 2005
 *
 * Copyright 1999 by ObjectSpace, Inc.,
 * 14850 Quorum Dr., Dallas, TX, 75240 U.S.A.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of ObjectSpace, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with ObjectSpace.
 */

package com.pb.tlumip.ed.edmodelxml;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

public interface IVariableXML extends com.objectspace.xml.IDXMLInterface ,com.objectspace.xml.IAttributeContainer
  {

  // element Attributes
  public String getNameAttribute();
  public void setNameAttribute( String value );
  public String removeNameAttribute();
  public String getTypeAttribute();
  public void setTypeAttribute( String value );
  public String removeTypeAttribute();

  // element LocationXML
  public void addLocationXML( String arg0  );
  public int getLocationXMLCount();
  public void setLocationXMLs( Vector arg0 );
  public String[] getLocationXMLs();
  public void setLocationXMLs( String[] arg0 );
  public Enumeration getLocationXMLElements();
  public String getLocationXMLAt( int arg0 );
  public void insertLocationXMLAt( String arg0, int arg1 );
  public void setLocationXMLAt( String arg0, int arg1 );
  public boolean removeLocationXML( String arg0 );
  public void removeLocationXMLAt( int arg0 );
  public void removeAllLocationXMLs();

  // element LagXML
  public void addLagXML( String arg0  );
  public int getLagXMLCount();
  public void setLagXMLs( Vector arg0 );
  public String[] getLagXMLs();
  public void setLagXMLs( String[] arg0 );
  public Enumeration getLagXMLElements();
  public String getLagXMLAt( int arg0 );
  public void insertLagXMLAt( String arg0, int arg1 );
  public void setLagXMLAt( String arg0, int arg1 );
  public boolean removeLagXML( String arg0 );
  public void removeLagXMLAt( int arg0 );
  public void removeAllLagXMLs();
  }