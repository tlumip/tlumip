/**
 * ISubModelXML.java	Java 1.4.2_07 Tue Jul 19 13:08:04 MDT 2005
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

public interface ISubModelXML extends com.objectspace.xml.IDXMLInterface ,com.objectspace.xml.IAttributeContainer
  {

  // element Attributes
  public String getOrderAttribute();
  public void setOrderAttribute( String value );
  public String removeOrderAttribute();
  public String getNameAttribute();
  public void setNameAttribute( String value );
  public String removeNameAttribute();
  public String getTypeAttribute();
  public void setTypeAttribute( String value );
  public String removeTypeAttribute();

  // element EquationXML
  public void addEquationXML( IEquationXML arg0  );
  public int getEquationXMLCount();
  public void setEquationXMLs( Vector arg0 );
  public IEquationXML[] getEquationXMLs();
  public void setEquationXMLs( IEquationXML[] arg0 );
  public Enumeration getEquationXMLElements();
  public IEquationXML getEquationXMLAt( int arg0 );
  public void insertEquationXMLAt( IEquationXML arg0, int arg1 );
  public void setEquationXMLAt( IEquationXML arg0, int arg1 );
  public boolean removeEquationXML( IEquationXML arg0 );
  public void removeEquationXMLAt( int arg0 );
  public void removeAllEquationXMLs();
  }