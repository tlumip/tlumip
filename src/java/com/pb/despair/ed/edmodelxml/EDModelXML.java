/**
 * EDModelXML.java	Java 1.2.2 Mon Jul 31 13:34:35 PDT 2000
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

package com.pb.despair.ed.edmodelxml;

import com.objectspace.xml.IClassDeclaration;
import com.objectspace.xml.xgen.ClassDecl;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class EDModelXML implements IEDModelXML
  {
  public Hashtable _Attributes = new Hashtable();
  public Vector _SubModelXML = new Vector();
  
  public static IClassDeclaration getStaticDXMLInfo()
    {
    return ClassDecl.find( "com.pb.despair.ed.edmodelxml.EDModelXML" );
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

  // element SubModelXML
  
  public void addSubModelXML( ISubModelXML arg0  )
    {
    if( _SubModelXML != null )
      _SubModelXML.addElement( arg0 );
    }
  
  public int getSubModelXMLCount()
    {
    return _SubModelXML == null ? 0 : _SubModelXML.size();
    }
  
  public void setSubModelXMLs( Vector arg0 )
    {
    if( arg0 == null )
      {
      _SubModelXML = null;
      return;
      }

    _SubModelXML = new Vector();

    for( Enumeration e = arg0.elements(); e.hasMoreElements(); )
      {
      String string = (String) e.nextElement();
      _SubModelXML.addElement( string );
      }
    }
  
  public ISubModelXML[] getSubModelXMLs()
    {
    if( _SubModelXML == null )
      return null;

    ISubModelXML[] array = new ISubModelXML[ _SubModelXML.size() ];
    _SubModelXML.copyInto( array );

    return array;
    }
  
  public void setSubModelXMLs( ISubModelXML[] arg0 )
    {
    Vector v = arg0 == null ? null : new Vector();

    if( arg0 != null )
      {
      for( int i = 0; i < arg0.length; i++ )
        v.addElement( arg0[ i ] );
      }

    _SubModelXML = v ;
    }
  
  public Enumeration getSubModelXMLElements()
    {
    return _SubModelXML == null ? null : _SubModelXML.elements();
    }
  
  public ISubModelXML getSubModelXMLAt( int arg0 )
    {
    return _SubModelXML == null ? null :  (ISubModelXML) _SubModelXML.elementAt( arg0 );
    }
  
  public void insertSubModelXMLAt( ISubModelXML arg0, int arg1 )
    {
    if( _SubModelXML != null )
      _SubModelXML.insertElementAt( arg0, arg1 );
    }
  
  public void setSubModelXMLAt( ISubModelXML arg0, int arg1 )
    {
    if( _SubModelXML != null )
      _SubModelXML.setElementAt( arg0, arg1 );
    }
  
  public boolean removeSubModelXML( ISubModelXML arg0 )
    {
    return _SubModelXML == null ? false : _SubModelXML.removeElement( arg0 );
    }
  
  public void removeSubModelXMLAt( int arg0 )
    {
    if( _SubModelXML == null )
      return;

    _SubModelXML.removeElementAt( arg0 );
    }
  
  public void removeAllSubModelXMLs()
    {
    if( _SubModelXML == null )
      return;

    _SubModelXML.removeAllElements();
    }
  }
