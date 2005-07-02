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
 * IEDModelXML.java	Java 1.2.2 Mon Jul 31 13:34:35 PDT 2000
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

public interface IEDModelXML extends com.objectspace.xml.IDXMLInterface ,com.objectspace.xml.IAttributeContainer
  {

  // element Attributes
  public String getNameAttribute();
  public void setNameAttribute( String value );
  public String removeNameAttribute();

  // element SubModelXML
  public void addSubModelXML( ISubModelXML arg0  );
  public int getSubModelXMLCount();
  public void setSubModelXMLs( Vector arg0 );
  public ISubModelXML[] getSubModelXMLs();
  public void setSubModelXMLs( ISubModelXML[] arg0 );
  public Enumeration getSubModelXMLElements();
  public ISubModelXML getSubModelXMLAt( int arg0 );
  public void insertSubModelXMLAt( ISubModelXML arg0, int arg1 );
  public void setSubModelXMLAt( ISubModelXML arg0, int arg1 );
  public boolean removeSubModelXML( ISubModelXML arg0 );
  public void removeSubModelXMLAt( int arg0 );
  public void removeAllSubModelXMLs();
  }
