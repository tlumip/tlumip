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
import java.io.IOException;

public class XMLModelCreator {

  String xmlFileName;
  IXml xmlDocument;
  IEDModelXML currentEDModel;
  ISubModelXML currentSubModel;
  IEquationXML currentEquation;
  IEquationElementXML currentEquationElement;
  IVariableXML currentVariable;

  /**
   * Takes in the file name of the dtd, and the name of the
   * xml file to be written.
   */
  public XMLModelCreator(String dtd, String xmlFile) {
    xmlFileName = xmlFile;
    try {
      File f = new File(dtd);
      //Create the new document with DTD f and ROOT EDModelXML.
      xmlDocument = Xml.newDocument("com.pb.tlumip.ed.edmodelxml",f, "EDModelXML");
      IEDModelXML edmodel = (IEDModelXML) xmlDocument.getRoot();
      currentEDModel = edmodel;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void writeOutXML() {
    try {
      File fout = new File(xmlFileName);
      fout.createNewFile();
      fout.canRead();
      fout.canWrite();
      System.out.println("saving document...");
      xmlDocument.saveDocument(fout);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setEDModelXMLName(String name) {
    currentEDModel.setNameAttribute(name);
  }

  public void addSubModel() {
      currentSubModel = Factory.newSubModelXML();
      currentEDModel.addSubModelXML(currentSubModel);
  }

  public void addEquation() {
      currentEquation = Factory.newEquationXML();
      if(currentSubModel.getTypeAttribute().equals(TextParser.LINEAR)){
        currentEquation.setTypeAttribute(TextParser.LINEAR);
      } else if (currentSubModel.getTypeAttribute().equals(TextParser.SIMPLE)){
        currentEquation.setTypeAttribute(TextParser.SIMPLE);
      }
      currentSubModel.addEquationXML(currentEquation);
  }

  public void addEquationElement() {
    currentEquationElement = Factory.newEquationElementXML();
    currentEquation.addEquationElementXML(currentEquationElement);
  }

  public void addVariable() {
      currentVariable = Factory.newVariableXML();
      currentEquationElement.setVariableXML(currentVariable);
  }

  public void subModelType(String type) {
    currentSubModel.setTypeAttribute(type);
  }

  public void subModelName(String name) {
    currentSubModel.setNameAttribute(name);
  }


  public void equationType(String type) {
    currentEquation.setTypeAttribute(type);
  }

  public void equationName(String name) {
    currentEquation.setNameAttribute(name);
  }

  public void edModelName(String name) {
    currentEDModel.setNameAttribute(name);
  }

  public void variableName(String name) {
    currentVariable.setNameAttribute(name);
  }

  public void variableType(String Type) {
    currentVariable.setTypeAttribute(Type);
  }

  public void addOperator(String data) {
    currentEquationElement.setOperatorXML(data);
  }

  public void addParameter(String data) {
   currentEquationElement.setParameterXML(data);
  }


  public void addLocation(String data) {
    currentVariable.addLocationXML(data);
  }

  public void addLag(String lag) {
    currentVariable.addLagXML(lag);
  }

  public void subModelOrder(String order) {
    currentSubModel.setOrderAttribute(order);
  }

}
