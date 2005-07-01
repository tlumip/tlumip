/**
 * Factory.java	Java 1.2.2 Mon Jul 31 13:34:34 PDT 2000
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

public class Factory
  {
  public static ISubModelXML newSubModelXML()
    {
    return new com.pb.tlumip.ed.edmodelxml.SubModelXML();
    }

  public static IVariableXML newVariableXML()
    {
    return new com.pb.tlumip.ed.edmodelxml.VariableXML();
    }

  public static IEDModelXML newEDModelXML()
    {
    return new com.pb.tlumip.ed.edmodelxml.EDModelXML();
    }

  public static IEquationXML newEquationXML()
    {
    return new com.pb.tlumip.ed.edmodelxml.EquationXML();
    }

  public static IEquationElementXML newEquationElementXML()
    {
    return new com.pb.tlumip.ed.edmodelxml.EquationElementXML();
    }

  }
