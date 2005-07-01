/**
 * SubModelXML.java	Java 1.2.2 Mon Jul 31 13:34:35 PDT 2000
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
import com.objectspace.xml.xgen.ClassDecl;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class SubModelXML implements ISubModelXML
  {
  public Hashtable _Attributes = new Hashtable();
  public Vector _EquationXML = new Vector();
  
  public static IClassDeclaration getStaticDXMLInfo()
    {
    return ClassDecl.find( "com.pb.tlumip.ed.edmodelxml.SubModelXML" );
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
  
  public String getOrderAttribute()
    {
    return getAttribute( "order" );
    }
  
  public void setOrderAttribute( String value )
    {
    setAttribute( "order", value );
    }
  
  public String removeOrderAttribute()
    {
    return removeAttribute( "order" );
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

  // element EquationXML
  
  public void addEquationXML( IEquationXML arg0  )
    {
    if( _EquationXML != null )
      _EquationXML.addElement( arg0 );
    }
  
  public int getEquationXMLCount()
    {
    return _EquationXML == null ? 0 : _EquationXML.size();
    }
  
  public void setEquationXMLs( Vector arg0 )
    {
    if( arg0 == null )
      {
      _EquationXML = null;
      return;
      }

    _EquationXML = new Vector();

    for( Enumeration e = arg0.elements(); e.hasMoreElements(); )
      {
      String string = (String) e.nextElement();
      _EquationXML.addElement( string );
      }
    }
  
  public IEquationXML[] getEquationXMLs()
    {
    if( _EquationXML == null )
      return null;

    IEquationXML[] array = new IEquationXML[ _EquationXML.size() ];
    _EquationXML.copyInto( array );

    return array;
    }
  
  public void setEquationXMLs( IEquationXML[] arg0 )
    {
    Vector v = arg0 == null ? null : new Vector();

    if( arg0 != null )
      {
      for( int i = 0; i < arg0.length; i++ )
        v.addElement( arg0[ i ] );
      }

    _EquationXML = v ;
    }
  
  public Enumeration getEquationXMLElements()
    {
    return _EquationXML == null ? null : _EquationXML.elements();
    }
  
  public IEquationXML getEquationXMLAt( int arg0 )
    {
    return _EquationXML == null ? null :  (IEquationXML) _EquationXML.elementAt( arg0 );
    }
  
  public void insertEquationXMLAt( IEquationXML arg0, int arg1 )
    {
    if( _EquationXML != null )
      _EquationXML.insertElementAt( arg0, arg1 );
    }
  
  public void setEquationXMLAt( IEquationXML arg0, int arg1 )
    {
    if( _EquationXML != null )
      _EquationXML.setElementAt( arg0, arg1 );
    }
  
  public boolean removeEquationXML( IEquationXML arg0 )
    {
    return _EquationXML == null ? false : _EquationXML.removeElement( arg0 );
    }
  
  public void removeEquationXMLAt( int arg0 )
    {
    if( _EquationXML == null )
      return;

    _EquationXML.removeElementAt( arg0 );
    }
  
  public void removeAllEquationXMLs()
    {
    if( _EquationXML == null )
      return;

    _EquationXML.removeAllElements();
    }
  }
