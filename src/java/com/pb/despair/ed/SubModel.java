
/**
 * Company:      ECONorthwest<p>
 * This is an abstract class for all SubModels.
 */
package com.pb.despair.ed;

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
