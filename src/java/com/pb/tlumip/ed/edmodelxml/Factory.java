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
