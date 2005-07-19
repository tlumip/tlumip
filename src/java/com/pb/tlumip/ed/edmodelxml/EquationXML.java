/**
 * EquationXML.java	Java 1.4.2_07 Tue Jul 19 13:08:04 MDT 2005
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
import com.objectspace.xml.xgen.ClassDecl;
import com.objectspace.xml.IClassDeclaration;

public class EquationXML implements IEquationXML
  {
  public Hashtable _Attributes = new Hashtable();
  public Vector _EquationElementXML = new Vector();
  
  public static IClassDeclaration getStaticDXMLInfo()
    {
    return ClassDecl.find( "com.pb.tlumip.ed.edmodelxml.EquationXML" );
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
  
  public String getNameAttribute()
    {
    return getAttribute( "name" );
    }
  
  public void setNameAttribute( String value )
    {
    setAttribute( "name", value );
    }
  
  public String removeNameAttribute()
    {
    return removeAttribute( "name" );
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

  // element EquationElementXML
  
  public void addEquationElementXML( IEquationElementXML arg0  )
    {
    if( _EquationElementXML != null )
      _EquationElementXML.addElement( arg0 );
    }
  
  public int getEquationElementXMLCount()
    {
    return _EquationElementXML == null ? 0 : _EquationElementXML.size();
    }
  
  public void setEquationElementXMLs( Vector arg0 )
    {
    if( arg0 == null )
      {
      _EquationElementXML = null;
      return;
      }

    _EquationElementXML = new Vector();

    for( Enumeration e = arg0.elements(); e.hasMoreElements(); )
      {
      String string = (String) e.nextElement();
      _EquationElementXML.addElement( string );
      }
    }
  
  public IEquationElementXML[] getEquationElementXMLs()
    {
    if( _EquationElementXML == null )
      return null;

    IEquationElementXML[] array = new IEquationElementXML[ _EquationElementXML.size() ];
    _EquationElementXML.copyInto( array );

    return array;
    }
  
  public void setEquationElementXMLs( IEquationElementXML[] arg0 )
    {
    Vector v = arg0 == null ? null : new Vector();

    if( arg0 != null )
      {
      for( int i = 0; i < arg0.length; i++ )
        v.addElement( arg0[ i ] );
      }

    _EquationElementXML = v ;
    }
  
  public Enumeration getEquationElementXMLElements()
    {
    return _EquationElementXML == null ? null : _EquationElementXML.elements();
    }
  
  public IEquationElementXML getEquationElementXMLAt( int arg0 )
    {
    return _EquationElementXML == null ? null :  (IEquationElementXML) _EquationElementXML.elementAt( arg0 );
    }
  
  public void insertEquationElementXMLAt( IEquationElementXML arg0, int arg1 )
    {
    if( _EquationElementXML != null )
      _EquationElementXML.insertElementAt( arg0, arg1 );
    }
  
  public void setEquationElementXMLAt( IEquationElementXML arg0, int arg1 )
    {
    if( _EquationElementXML != null )
      _EquationElementXML.setElementAt( arg0, arg1 );
    }
  
  public boolean removeEquationElementXML( IEquationElementXML arg0 )
    {
    return _EquationElementXML == null ? false : _EquationElementXML.removeElement( arg0 );
    }
  
  public void removeEquationElementXMLAt( int arg0 )
    {
    if( _EquationElementXML == null )
      return;

    _EquationElementXML.removeElementAt( arg0 );
    }
  
  public void removeAllEquationElementXMLs()
    {
    if( _EquationElementXML == null )
      return;

    _EquationElementXML.removeAllElements();
    }
  }