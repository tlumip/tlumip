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
 * EquationElementXML.java	Java 1.2.2 Mon Jul 31 13:34:35 PDT 2000
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

import com.objectspace.xml.IClassDeclaration;
import com.objectspace.xml.core.StringWrapper;
import com.objectspace.xml.xgen.ClassDecl;

import java.util.Hashtable;

public class EquationElementXML implements IEquationElementXML
  {
  public Hashtable _Attributes = new Hashtable();
  public IVariableXML _VariableXML = null;
  public StringWrapper _ParameterXML = null;
  public StringWrapper _OperatorXML = null;
  
  public static IClassDeclaration getStaticDXMLInfo()
    {
    return ClassDecl.find( "com.pb.tlumip.ed.edmodelxml.EquationElementXML" );
    }
  
  public IClassDeclaration getDXMLInfo()
    {
    return getStaticDXMLInfo();
    }

  // element Attributes
  
  public String getAttribute( String name )
    {
    String value = (String) _Attributes.get( name );

    if( value != null ) 
      return value;

    return null;
    }
  
  public Hashtable getAttributes()
    {
    Hashtable clone = (Hashtable) _Attributes.clone();

    return clone;
    }
  
  public void setAttribute( String name, String value )
    {
    _Attributes.put( name, value );
    }
  
  public String removeAttribute( String name )
    {
    return (String) _Attributes.remove( name );
    }
  
  public String getTypeAttribute()
    {
    return getAttribute( "type" );
    }
  
  public void setTypeAttribute( String value )
    {
    setAttribute( "type", value );
    }
  
  public String removeTypeAttribute()
    {
    return removeAttribute( "type" );
    }

  // element VariableXML
  
  public IVariableXML getVariableXML()
    {
    return _VariableXML;
    }
  
  public void setVariableXML( IVariableXML arg0 )
    {
    _ParameterXML = null;
    _OperatorXML = null;

    _VariableXML = arg0;
    }

  // element ParameterXML
  
  public String getParameterXML()
    {
    return _ParameterXML == null ? null : _ParameterXML.getRecursiveValue();
    }
  
  public void setParameterXML( String arg0 )
    {
    _VariableXML = null;
    _OperatorXML = null;

    _ParameterXML = arg0 == null ? null : new StringWrapper( arg0 );
    }

  // element OperatorXML
  
  public String getOperatorXML()
    {
    return _OperatorXML == null ? null : _OperatorXML.getRecursiveValue();
    }
  
  public void setOperatorXML( String arg0 )
    {
    _VariableXML = null;
    _ParameterXML = null;

    _OperatorXML = arg0 == null ? null : new StringWrapper( arg0 );
    }
  }
