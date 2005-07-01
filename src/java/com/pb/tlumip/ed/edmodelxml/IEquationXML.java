/**
 * IEquationXML.java	Java 1.2.2 Mon Jul 31 13:34:35 PDT 2000
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

import java.util.Enumeration;
import java.util.Vector;

public interface IEquationXML extends com.objectspace.xml.IDXMLInterface ,com.objectspace.xml.IAttributeContainer
  {

  // element Attributes
  public String getTypeAttribute();
  public void setTypeAttribute( String value );
  public String removeTypeAttribute();
  public String getNameAttribute();
  public void setNameAttribute( String value );
  public String removeNameAttribute();

  // element EquationElementXML
  public void addEquationElementXML( IEquationElementXML arg0  );
  public int getEquationElementXMLCount();
  public void setEquationElementXMLs( Vector arg0 );
  public IEquationElementXML[] getEquationElementXMLs();
  public void setEquationElementXMLs( IEquationElementXML[] arg0 );
  public Enumeration getEquationElementXMLElements();
  public IEquationElementXML getEquationElementXMLAt( int arg0 );
  public void insertEquationElementXMLAt( IEquationElementXML arg0, int arg1 );
  public void setEquationElementXMLAt( IEquationElementXML arg0, int arg1 );
  public boolean removeEquationElementXML( IEquationElementXML arg0 );
  public void removeEquationElementXMLAt( int arg0 );
  public void removeAllEquationElementXMLs();
  }
