/**
 * VariableXML.java	Java 1.4.2_07 Tue Jul 19 13:08:04 MDT 2005
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
import com.objectspace.xml.core.StringWrapper;
import com.objectspace.xml.IClassDeclaration;

public class VariableXML implements IVariableXML
  {
  public Hashtable _Attributes = new Hashtable();
  public Vector _LocationXML = new Vector();
  public Vector _LagXML = new Vector();
  
  public static IClassDeclaration getStaticDXMLInfo()
    {
    return ClassDecl.find( "com.pb.tlumip.ed.edmodelxml.VariableXML" );
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

  // element LocationXML
  
  public void addLocationXML( String arg0  )
    {
    if( _LocationXML != null )
      _LocationXML.addElement( arg0 == null ? null : new StringWrapper( arg0 ) );
    }
  
  public int getLocationXMLCount()
    {
    return _LocationXML == null ? 0 : _LocationXML.size();
    }
  
  public void setLocationXMLs( Vector arg0 )
    {
    if( arg0 == null )
      {
      _LocationXML = null;
      return;
      }

    _LocationXML = new Vector();

    for( Enumeration e = arg0.elements(); e.hasMoreElements(); )
      {
      String string = (String) e.nextElement();
      _LocationXML.addElement( string == null ? null : new StringWrapper( string ) );
      }
    }
  
  public String[] getLocationXMLs()
    {
    if( _LocationXML == null )
      return null;

    String[] array = new String[ _LocationXML.size() ];
    int i = 0;

    for( Enumeration e = _LocationXML.elements(); e.hasMoreElements(); i++ )
      array[ i ] = ((StringWrapper) e.nextElement()).getRecursiveValue();

    return array;
    }
  
  public void setLocationXMLs( String[] arg0 )
    {
    Vector v = arg0 == null ? null : new Vector();

    if( arg0 != null )
      {
      for( int i = 0; i < arg0.length; i++ )
        v.addElement( arg0[ i ] == null ? null : new StringWrapper( arg0[ i ] ) );
      }

    _LocationXML = v ;
    }
  
  public Enumeration getLocationXMLElements()
    {
    if( _LocationXML == null )
      return null;

    Vector v = new Vector();

    for( Enumeration e = _LocationXML.elements(); e.hasMoreElements(); )
      v.addElement( ((StringWrapper) e.nextElement()).getRecursiveValue() );

    return v.elements();
    }
  
  public String getLocationXMLAt( int arg0 )
    {
    return _LocationXML == null ? null :  ((StringWrapper) _LocationXML.elementAt( arg0 )).getRecursiveValue();
    }
  
  public void insertLocationXMLAt( String arg0, int arg1 )
    {
    if( _LocationXML != null )
      _LocationXML.insertElementAt( arg0 == null ? null : new StringWrapper( arg0 ), arg1 );
    }
  
  public void setLocationXMLAt( String arg0, int arg1 )
    {
    if( _LocationXML != null )
      _LocationXML.setElementAt( arg0 == null ? null : new StringWrapper( arg0 ), arg1 );
    }
  
  public boolean removeLocationXML( String arg0 )
    {
    if( _LocationXML == null )
      return false;

    int i = 0;

    for( Enumeration e = _LocationXML.elements(); e.hasMoreElements(); i++ )
      if( ((StringWrapper) e.nextElement()).getRecursiveValue().equals( arg0 ) )
        {
        _LocationXML.removeElementAt( i );
        return true;
        }

    return false;
    }
  
  public void removeLocationXMLAt( int arg0 )
    {
    if( _LocationXML == null )
      return;

    _LocationXML.removeElementAt( arg0 );
    }
  
  public void removeAllLocationXMLs()
    {
    if( _LocationXML == null )
      return;

    _LocationXML.removeAllElements();
    }

  // element LagXML
  
  public void addLagXML( String arg0  )
    {
    if( _LagXML != null )
      _LagXML.addElement( arg0 == null ? null : new StringWrapper( arg0 ) );
    }
  
  public int getLagXMLCount()
    {
    return _LagXML == null ? 0 : _LagXML.size();
    }
  
  public void setLagXMLs( Vector arg0 )
    {
    if( arg0 == null )
      {
      _LagXML = null;
      return;
      }

    _LagXML = new Vector();

    for( Enumeration e = arg0.elements(); e.hasMoreElements(); )
      {
      String string = (String) e.nextElement();
      _LagXML.addElement( string == null ? null : new StringWrapper( string ) );
      }
    }
  
  public String[] getLagXMLs()
    {
    if( _LagXML == null )
      return null;

    String[] array = new String[ _LagXML.size() ];
    int i = 0;

    for( Enumeration e = _LagXML.elements(); e.hasMoreElements(); i++ )
      array[ i ] = ((StringWrapper) e.nextElement()).getRecursiveValue();

    return array;
    }
  
  public void setLagXMLs( String[] arg0 )
    {
    Vector v = arg0 == null ? null : new Vector();

    if( arg0 != null )
      {
      for( int i = 0; i < arg0.length; i++ )
        v.addElement( arg0[ i ] == null ? null : new StringWrapper( arg0[ i ] ) );
      }

    _LagXML = v ;
    }
  
  public Enumeration getLagXMLElements()
    {
    if( _LagXML == null )
      return null;

    Vector v = new Vector();

    for( Enumeration e = _LagXML.elements(); e.hasMoreElements(); )
      v.addElement( ((StringWrapper) e.nextElement()).getRecursiveValue() );

    return v.elements();
    }
  
  public String getLagXMLAt( int arg0 )
    {
    return _LagXML == null ? null :  ((StringWrapper) _LagXML.elementAt( arg0 )).getRecursiveValue();
    }
  
  public void insertLagXMLAt( String arg0, int arg1 )
    {
    if( _LagXML != null )
      _LagXML.insertElementAt( arg0 == null ? null : new StringWrapper( arg0 ), arg1 );
    }
  
  public void setLagXMLAt( String arg0, int arg1 )
    {
    if( _LagXML != null )
      _LagXML.setElementAt( arg0 == null ? null : new StringWrapper( arg0 ), arg1 );
    }
  
  public boolean removeLagXML( String arg0 )
    {
    if( _LagXML == null )
      return false;

    int i = 0;

    for( Enumeration e = _LagXML.elements(); e.hasMoreElements(); i++ )
      if( ((StringWrapper) e.nextElement()).getRecursiveValue().equals( arg0 ) )
        {
        _LagXML.removeElementAt( i );
        return true;
        }

    return false;
    }
  
  public void removeLagXMLAt( int arg0 )
    {
    if( _LagXML == null )
      return;

    _LagXML.removeElementAt( arg0 );
    }
  
  public void removeAllLagXMLs()
    {
    if( _LagXML == null )
      return;

    _LagXML.removeAllElements();
    }
  }