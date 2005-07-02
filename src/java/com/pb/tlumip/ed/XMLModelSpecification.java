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
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.tlumip.ed;

import com.objectspace.xml.IXml;
import com.objectspace.xml.Xml;
import com.pb.tlumip.ed.edmodelxml.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
//implements ModelSpecificationInterface
public class XMLModelSpecification {

  private IEDModelXML currentModel;
  private ISubModelXML currentSubModelXML;
  private IEquationXML currentEquationXML;
  private IVariableXML currentVariableXML;
  private String currentLocationXML;
  private String currentLagXML;
  private IEquationElementXML currentEquationElementXML;
  private String fileName;
  private IXml xmlDocument;

  private int countSubModelXML;
  private int countEquationXML;
  private int countEquationElementXML;

  private int numSubModelXMLs;
  private int numEquationXMLs;
  private int numEquationElementXMLs;

  public XMLModelSpecification(String fn) throws FileNotFoundException, IOException  {
    fileName=fn;
    xmlDocument = Xml.openDocument("com.pb.tlumip.ed.edmodelxml", new File(fileName));
    currentModel = (IEDModelXML) xmlDocument.getRoot();
    numSubModelXMLs = currentModel.getSubModelXMLCount();
    System.out.println("Number of submodels = " + numSubModelXMLs);
    countSubModelXML = 0;
    countEquationXML = 0;
    countEquationElementXML = 0;
  }

  public String getFileName() {
    return fileName;
  }

  public boolean nextSubModelXML() {
    if(numSubModelXMLs> countSubModelXML){
      currentSubModelXML = currentModel.getSubModelXMLAt(countSubModelXML);
      numEquationXMLs = currentSubModelXML.getEquationXMLCount();
      countEquationXML = 0;
      countSubModelXML++;
      return true;
    }
    currentSubModelXML=null;
    return false;

  }

  public boolean nextEquationXML() {
    if(numEquationXMLs > countEquationXML){
      currentEquationXML = currentSubModelXML.getEquationXMLAt(countEquationXML);
      numEquationElementXMLs = currentEquationXML.getEquationElementXMLCount();
      countEquationElementXML = 0;
      countEquationXML++;
      return true;
    }
    currentEquationXML=null;
    return false;
  }

  public boolean nextEquationElementXML() {
    if(numEquationElementXMLs > countEquationElementXML){
      currentEquationElementXML = currentEquationXML.getEquationElementXMLAt(countEquationElementXML);
      countEquationElementXML++;
      return true;
    }
    currentEquationElementXML = null;
    return false;
  }

  public boolean isVariableXML() {
    currentVariableXML = currentEquationElementXML.getVariableXML();
    if(currentVariableXML == null) {
      return false;
    }
    return true;
  }

  public boolean isOperatorXML() {
      if (currentEquationElementXML.getOperatorXML() == null){
        return false;
      }
      return true;
  }

  public boolean isParameterXML() {
      if (currentEquationElementXML.getParameterXML() == null){
        return false;
      }
      return true;
  }

  public boolean nextLocationXML() {

      if (currentVariableXML.getLocationXMLCount() >0) {
        currentLocationXML = currentVariableXML.getLocationXMLAt(0);
        return false;
      }
      return true;
  }

  public boolean nextLagXML() {
      if (currentVariableXML.getLagXMLCount() >0) {
        currentLagXML = currentVariableXML.getLagXMLAt(0);
        return true;
      }
      return false;
  }

  public String getSubModelOrder() {
    return currentSubModelXML.getOrderAttribute();
  }

  public String getSubModelName() {
    return currentSubModelXML.getNameAttribute();
  }

  public String getSubModelType() {
    return currentSubModelXML.getTypeAttribute();
  }

  public String getEquationName() {
    return currentEquationXML.getNameAttribute();
  }

  public String getEquationType() {
    return currentEquationXML.getTypeAttribute();
  }

  public String getVariableName() {
    return currentVariableXML.getNameAttribute();
  }

  public String getVariableType() {
    return currentVariableXML.getTypeAttribute();
  }

  public String getParameterXML() {
    return currentEquationElementXML.getParameterXML();
  }

  public String getOperatorXML() {
    return currentEquationElementXML.getOperatorXML();
  }

  public String getLocationXML() {
    return currentLocationXML;
  }

  public String getLagXML() {
    return currentLagXML;
  }

  public void goToFirstEquation() {
    countEquationXML = 0;
  }

  public void goToFirstEquationElement(){
    countEquationElementXML = 0;
  }



}
