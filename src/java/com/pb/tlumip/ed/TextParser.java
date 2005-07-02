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
 * Description:  TextParser is an independant utility that is run seperate from
 * the ED model, and reads a text file and produces an xml file.
 * Company:      ECONorthwest<p>
 */
package com.pb.tlumip.ed;

import java.io.*;
import java.util.StringTokenizer;



public class TextParser {

  public static final String VARIABLE = "v";
  public static final String VARIABLETYPE = "vt";
  public static final String OPERATOR = "o";
  public static final String PARAMETER = "p";
  public static final String LOCATION = "l";
  public static final String EQUATION = "eq";
  public static final String SUBMODEL = "s";
  public static final String VARIABLELAG = "vl";
  public static final String EDMODELNAME = "emn";
  public static final String SUBMODELNAME = "smn";
  public static final String SUBMODELORDER = "smo";

  public static final String PLUS = Operator.PLUS;
  public static final String MINUS = Operator.MINUS;
  public static final String MULTIPLY = Operator.MULTIPLY;
  public static final String DIVIDE = Operator.DIVIDE;
  public static final String POWER = Operator.POWER;
  public static final String NATURALLOG = Operator.NATURALLOG;

  public static final String TEMPORARY = "t";

  public static final String LINEAR = "linear";
  public static final String LOGIT = "logit";
  public static final String SIMPLE = "simple";

  StringTokenizer st;
  XMLModelCreator xmlc;
  String s;
  int count;

  public static void main(String args[]) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String infile, outfile;
      System.out.println("Enter the name of the file to be read: ");
      infile = br.readLine();
      System.out.println("Enter the name of the file to be written: ");
      outfile = br.readLine();
      XMLModelCreator x = new XMLModelCreator(Test.home + "EDModelXML.dtd", Test.home + outfile);
      TextParser tp = new TextParser(Test.home + infile,x);
      tp.startParsing();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Takes the filename to be read, and an XMLModelCreator to produce XML files.
   */
  TextParser (String filename, XMLModelCreator x) throws IOException, FileNotFoundException{
    BufferedReader in = new BufferedReader(new FileReader(filename));

    String temp = in.readLine();
    String stemp = "";
    while(temp != null) {
      stemp = stemp+ " " + temp;
      temp = in.readLine();
    }
    st = new StringTokenizer(stemp,"\b\t\n\r ");
    xmlc = x;
  }

  /**
   * Reads the file and produces xml code.
   */
  void startParsing() throws TextReadingException {
    count = 1;
    while(st.hasMoreTokens()) {
      s = st.nextToken();
      if(s.equals(null)) {
      } else if(s.equals(VARIABLE)) {
        readVariable();
      } else if(s.equals(VARIABLETYPE)) {
        readVariableType();
      } else if(s.equals(PARAMETER)) {
        this.readParameter();
      } else if(s.equals(OPERATOR)) {
        this.readOperator();
      } else if (s.equals(LOCATION)) {
        readLocation();
      } else if (s.equals(EQUATION)) {
        readEquation();
      } else if (s.equals(SUBMODEL)) {
        readSubModel();
      } else if (s.equals(EDMODELNAME)) {
        readEDModelName();
      } else if (s.equals(SUBMODELNAME)) {
        readSubModelName();
      } else if (s.equals(SUBMODELORDER)) {
        readSubModelOrder();
      } else if (s.equals(VARIABLELAG)) {
        readLag();
      }
      else {
        throw new TextReadingException("Error on statement: " + count+ ".  Cant read token: " + s);
      }
      count++;

    }//while
    System.out.println("writing output...");
    xmlc.writeOutXML();
  }

  void readVariable(){
    xmlc.addEquationElement();
    xmlc.addVariable();
    s = st.nextToken();
    xmlc.variableName(s);
  }

  void readVariableType() throws TextReadingException {
    s = st.nextToken();
    if(TEMPORARY.equals(s)) {
      xmlc.variableType(TEMPORARY);
    } else {
      throw new TextReadingException("Error on statement: " + count+ ".  Cant read token: " + s);
    }
  }

  void readParameter() {
    xmlc.addEquationElement();
    s = st.nextToken();
    double temp = Double.parseDouble(s);
    xmlc.addParameter(Double.toString(temp));
  }

  void readOperator() throws TextReadingException {
    xmlc.addEquationElement();
    s = st.nextToken();
    if(PLUS.equals(s)){
      xmlc.addOperator(PLUS);
    }else if(MINUS.equals(s)) {
      xmlc.addOperator(MINUS);
    }else if(MULTIPLY.equals(s)) {
      xmlc.addOperator(MULTIPLY);
    }else if(DIVIDE.equals(s)) {
      xmlc.addOperator(DIVIDE);
    }else if(NATURALLOG.equals(s)) {
      xmlc.addOperator(NATURALLOG);
    }else if(POWER.equals(s)) {
      xmlc.addOperator(POWER);
    }else {
       throw new TextReadingException("Error on statement: " + count+ ".  Cant read token: " + s);
    }
  }

  void readLag() throws TextReadingException {
    s = st.nextToken();
    try {
      //Make sure the value is actually an integer.
      int temp = Integer.parseInt(s);
      xmlc.addLag(Integer.toString(temp));
    }catch (Exception e) {
      throw new TextReadingException("Error on statement: " + count+ ".  Cant read token: " + s);
    }
  }

  void readLocation() {
    s = st.nextToken();
    xmlc.addLocation(s);
  }

  void readEquation() throws TextReadingException {
    s = st.nextToken();
    xmlc.addEquation();
    xmlc.equationName(s);
  }

  void readSubModel() throws TextReadingException {
    s = st.nextToken();
    xmlc.addSubModel();
    if(LINEAR.equals(s)) {
      xmlc.subModelType(LINEAR);
    } else if(LOGIT.equals(s)) {
      xmlc.subModelType(LOGIT);
    } else if(SIMPLE.equals(s)) {
      xmlc.subModelType(s);
    } else {
        throw new TextReadingException("Error on statement: " + count+ ".  Cant read token: " + s);
    }
  }

  void readEDModelName() {
    s = st.nextToken();
    xmlc.edModelName(s);
  }

  void readSubModelName() {
    s = st.nextToken();
    xmlc.subModelName(s);
  }

  void readSubModelOrder() throws TextReadingException {
    s = st.nextToken();
    try {
      int temp = Integer.parseInt(s);
      xmlc.subModelOrder(Integer.toString(temp));
    }catch (Exception e) {
      throw new TextReadingException("Error on statement: " + count+ ".  Cant read token: " + s);
    }
  }

}
