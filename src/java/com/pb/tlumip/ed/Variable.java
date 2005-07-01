
/**
 * Company:      ECONorthwest<p>
 *
 */

package com.pb.tlumip.ed;


public class Variable implements  DoubleInterface  {

  String name;
  int year;
  double value;
  boolean dependant;
  boolean hasData;
  String location;

  /**
 * Constructor.
 */
  public Variable(String n, String l, int y){
    name = n;
    location = l;
    year = y;
    dependant = false;
  }

  String getIdentifier() {
    return (String) (name + year);
  }

  /**
   * Returns the name of the variable.
   */
  String getName() {
    return name;
  }

  /**
   * Returns the year of the variable.
   */
  int getYear() {
    return year;
  }

  /**
   * Returns true if the variables year and name are equal.
   */
  boolean equals(Variable v) {
    if((this.getName().equals(v.getName())) && (this.getYear()== v.getYear())) {
      return true;
    }
    return false;
  }


 /**
   * Returns true if the variable has a value.
   */
  public boolean hasData() {
    return hasData;
  }

  /**
   * Set the double value of the value object.
   */
  public void setValue(double d) {
    hasData = true;
    value = d;
  }

  /**
   * Return the double value of the value object.
   */
  public double getValue() throws UnknownValueException {
    if (hasData){
      return value;
    } else {
      throw new UnknownValueException("Variable: " + this.getName() + " has no value.");
    }
  }


    /**
   * Set the location value of this variable.  (Out location if dependant, and
   * in location if independant.)
   */
  protected void setLocation(String l) {
    location = l;
  }

  /**
   * Return the location of the variable object
   */
  protected String getLocation() {
    return location;
  }

    /**
   * Returns true if the variable is dependant.  This is not accurate until
   * this value is set by a SubModel.
   */
  protected boolean isDependant() {
    return dependant;
  }

    /**
   * Sets this variables dependant flag.
   */
  protected void setDependant(boolean d) {
    dependant = d;
  }

}
