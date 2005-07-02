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
 * Company:      ECONorthwest<p>
 * This is an abstract class for all SubModels.
 */
package com.pb.tlumip.ed;

abstract class SubModel {

  private int order;
  private String name;
  private VariableStore vs;

  /**
   * Constructor takes in all basic SubModel data.
   */
  public SubModel(int o, String n, VariableStore v) {
    order = o;
    name = n;
    vs = v;
  }

  /**
   * Returns the name of the submodel.
   */
  String getName() {
    return name;
  }

  abstract void solve() throws Exception;

  abstract String getType();

  /**
   * Calls the variable store to retrieve data.  (Retrieve data from the
   * database).
   */
  void getData() {
    vs.getData();
  }

  /**
   * Calls the variable store to set the data. (Write out to the database).
   */
  void setData() {
    vs.setData();
  }

  /**
   * Return the order the database will run in.
   */
  int getOrder() {
    return order;
  }

  /**
   * Returns the variable store.
   */
  VariableStore getVariableStore() {
    return vs;
  }

}
