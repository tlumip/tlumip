package com.pb.despair.ct;


// A class to define two-dimensional matrices and common spatial interaction
// functions. The data are read and written to text files.
// @author Rick Donnelly <rdonnelly@pbtfsc.com>
// @version "0.92,3/12/02"

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Locale;
import java.io.*;
import java.util.StringTokenizer;


public class Matrix2d {

  private String label;
  private int rows, columns;
  private int maxIterations = 100;
  private double maxRelativeError = 0.0001;
  double[][] cell;
  boolean trace = false;

  Matrix2d (int i, int j, String title) {
    rows = i;
    columns = j;
    label = new String(title);
    cell = new double[rows][columns];
    for (int n=0; n<rows; n++)
      for (int p=0; p<columns; p++)
        cell[n][p] = 0.0;
  }

  public void apply (String functionName, Matrix2d cost, double a) {
    apply(functionName, cost, a, Double.NaN, Double.NaN);
  }
  public void apply (String functionName, Matrix2d cost, double a, double b) {
    apply(functionName, cost, a, b, Double.NaN);
  }
  public void apply (String functionName, Matrix2d cost, double a, double b,
      double c) {
    if (trace) System.out.println("Entering apply(): functionName="+
      functionName+" a="+a+" b="+b);
    for (int i=0; i<rows; i++)
      for (int j=0; j<columns; j++) {
        if (functionName.equals("gamma"))
          cell[i][j] = a*cost.cell[i][j]*Math.exp(b*cost.cell[i][j]);
        else if (functionName.equals("exponential"))
          cell[i][j] = a*Math.exp(b*cost.cell[i][j]);
        else if (functionName.equals("exp+power"))
          cell[i][j] = a*Math.pow(cost.cell[i][j],2)*
            Math.exp(b*cost.cell[i][j]);
        else if (functionName.equals("crc3.6.2"))
          cell[i][j] = 1/(a+b*Math.exp(c*cost.cell[i][j]));
        else {
          System.err.println("Error: Undefined function "+functionName+
            " passed to apply()");
          System.exit(-2);
        }
        if (trace) System.out.println("i="+i+" j="+j+" cost[i][j]="+
          cost.cell[i][j]+" cell[i][j]="+cell[i][j]);
      }
  }

  // An implementation of two-dimensional matrix balancing as proposed by
  // Susan Evans
  public String balance2d (Matrix2d f, double[] origins,
      double[] destinations) {
    if (trace) System.out.println("Entering balance2d()...");

    // (1) Initialisation step: Set balancing factors to 1.0 if there are
    // corresponding origins or destinations (as appropriate) defined.
    double[] alpha = new double[rows];
    for (int p=0; p<rows; p++)
      if (origins[p]>0.0) alpha[p] = 1.0;
    double[] beta = new double[columns];
    for (int q=0; q<columns; q++)
      if (destinations[q]>0.0) beta[q] = 1.0;

    double a, maxAlphaError, b, maxBetaError;
    double previousError = 0.0, x = 0.0;
    int iterations = 1;     // how many iterations have we done?
    int p, q;   // row and column loop increments
    for (int i=1; i<maxIterations; i++, iterations++) {
      if (trace) System.out.println("--------------- iteration="+i+
        "---------------");

      // (2) Calculate new value for the row balancing factor (alpha)
      maxAlphaError = 0.0;
      for (p=0; p<rows; p++) {
        if (origins[p]>0.0) {
          a = alpha[p];
          x = 0.0;
          for (q=0; q<columns; q++)
            x += f.cell[p][q]*beta[q];
          alpha[p] = origins[p]/x;
          maxAlphaError = Math.max(maxAlphaError, ((alpha[p]-a)/alpha[p]));
          if (trace) System.out.println("i="+i+
            " p="+p+
            " origins="+origins[p]+
            " a="+(float)a+
            " alpha="+(float)alpha[p]+
            " maxAlphaError="+(float)maxAlphaError);
        }
      }

      // (3) Calculate new values for column balancing factors (beta)
      maxBetaError = 0.0;
      for (q=0; q<columns; q++) {
        if (destinations[q]>0.0) {
          b = beta[q];
          x = 0.0;
          for (p=0; p<rows; p++)
            x += f.cell[p][q]*alpha[p];
          beta[q] = destinations[q]/x;
          maxBetaError = Math.max(maxBetaError, ((beta[q]-b)/beta[q]));
          if (trace) System.out.println("i="+i+
            " q="+q+
            " destinations="+destinations[q]+
            " b="+(float)b+
            " beta="+(float)beta[q]+
            " maxBetaError="+(float)maxBetaError);
        }
      }

      // (4) Evaluate stopping criteria
      x = Math.max(maxAlphaError, maxBetaError);
      if (trace) System.out.println("iteration="+i+" error="+x);
      if ((x <= maxRelativeError) || (x == previousError) ||
        (Double.isNaN(x))) break;
      previousError = x;
    }

    // Once steps 2-4 above have iterated to closure or reached the maximum
    // number of iterations, calculate the cell values
    double finalTotal = 0.0;
    for (p=0; p<rows; p++)
      for (q=0; q<columns; q++)
        finalTotal += cell[p][q] = alpha[p]*beta[q]*f.cell[p][q];

    // Set up formatting and prepare the results string, which will contain
    // the total number of iterations, total trips, and the error term
    DecimalFormat df =
      (DecimalFormat)NumberFormat.getNumberInstance(Locale.US);
    df.setGroupingSize(0);
    df.setMaximumFractionDigits(0);
    String s = iterations+",";
    s += df.format(finalTotal)+",";
    df.setMaximumFractionDigits(4);
    s += df.format(x);
    return s;
  }

  // A variant on the matrix balancing method would return a double value
  // that represents the average trip length; this would be useful for
  // searching for parameter values during model calibration...
  public String balance2d (Matrix2d f, Matrix2d u, double[] origins,
      double[] destinations) {
    String s = balance2d(f, origins, destinations);
    return s+", mean trip length="+weightedAverage(u);
  }

  // A method to aggregate the matrix by zeroing out entries below a user-
  // specified threshold, and scaling the remainder to sum to the original
  // total.
  public void coalesce (double lowerThreshold) {
    // Zero out the cells that fall below the user-specified threshold
    double initialTotal = 0.0, residual = 0.0;
    int p, q;
    for (p=0; p<rows; p++)
      for (q=0; q<columns; q++) {
        initialTotal += cell[p][q];
        if (cell[p][q]<lowerThreshold) {
          residual += cell[p][q];
          cell[p][q] = 0.0;
        }
      }

    // Scale the remainding cells up to the original values
    double expansionFactor = initialTotal/(initialTotal-residual);
    double revisedTotal = 0.0;
    for (p=0; p<rows; p++)
      for (q=0; q<columns; q++) {
        if (cell[p][q]>0.0) cell[p][q] *= expansionFactor;
        revisedTotal += cell[p][q];
      }

    System.out.println("Initial matrix total="+initialTotal+
      ", revised total="+revisedTotal);
  }

  public double bucketRound (int places, boolean skipZero) {
    double z = Math.pow(10,places), r = 0.0, x;
    long i;
    for (int n=0; n<rows; n++)
      for (int p=0; p<columns; p++) {
        if (skipZero && cell[n][p] == 0.0) continue;
        x = (cell[n][p]+r)*z;
        i = Math.round(x);
        r = (x-i)/z;
        cell[n][p] = (double)i/z;
      }
    return r;    // return the residual
  }

  public void fill () { fill(0.0); }
  public void fill (double initValue) {
    for (int n=0; n<rows; n++)
      for (int p=0; p<columns; p++)
        cell[n][p] = initValue;
  }

  public String getLabel() { return label; }

  public int getRowSize() { return rows; }

  public int getColumnSize() { return columns; }

  public double readTextMatrixFile (String filename) {
    double d = readTextMatrixFile(filename, false);
    return d;
  }
  public double readTextMatrixFile (String filename, boolean trace) {
    String s;
    int i,j;
    int index = 0;
    double dval, total = 0.0;

    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      System.out.print("Reading text matrix file "+filename);
      while ((s = br.readLine()) != null) {
        ++index;
        if (s.startsWith(";")) continue;      // a comment card
        StringTokenizer st = new StringTokenizer(s,", ");
        i = Integer.parseInt(st.nextToken());
        j = Integer.parseInt(st.nextToken());
        dval = Double.parseDouble(st.nextToken());
        if (trace) System.out.println("i="+i+" j="+j+" value="+dval);
        cell[i][j] += dval;
        total += dval;
      }
      System.out.println(": "+index+" records read, total="+total);
      br.close();
    } catch (IOException e) { e.printStackTrace(); }
    return total;
  }

  public void scale (double scaleValue) {
    int ix = getRowSize(), jx = getColumnSize();
    for (int i=0; i<ix; i++)
      for (int j=0; j<jx; j++)
        cell[i][j] *= scaleValue;
  }

  public void setLabel (String s) { label = s; }

  public void setMaxIterations (int i) { maxIterations = i; }

  public void setMaxRelativeError (double d) { maxRelativeError = d; }

  public double total () {
    double d = 0.0;
    for (int n=0; n<rows; n++)
      for (int p=0; p<columns; p++)
        d += cell[n][p];
    return d;
  }

  public double weightedAverage (Matrix2d w) {
    if (trace) System.out.println("Entering weightedAverage()...");
    double total = 0.0, weightedTotal = 0.0, result = 0.0;
    for (int i=0; i<rows; i++)
      for (int j=0; j<columns; j++) {
        weightedTotal += this.cell[i][j]*w.cell[i][j];
        total += this.cell[i][j];
      }
    if (total>0.0) result = weightedTotal/total;
    return result;
  }

  public double weightedSum (Matrix2d w) {
    if (trace) System.out.println("Entering weightedSum()...");
    double sum = 0.0;
    for (int i=0; i<rows; i++)
      for (int j=0; j<columns; j++)
        sum += this.cell[i][j]*w.cell[i][j];
    return sum;
  }

  public void writeInroMatrixFile (String filename, int id, String name,
      double defaultValue, String title) {
    writeInroMatrixFile (filename, id, name, defaultValue, title, false);
  }
  public void writeInroMatrixFile (String filename, int id, String name,
      double defaultValue, String title, boolean append) {
    int MAX_COLUMN_WIDTH = 80;   // maintain 80 column text format
    String prefix, suffix;
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(filename,
        append));
      if (!append) {
        bw.write("c "+filename+" written by Matrix2d on "+new java.util.Date());
        bw.newLine();
        bw.write("t matrices");
        bw.newLine();
      }
      bw.write("a matrix=mf"+id+" "+name+" "+defaultValue+" "+title);
      bw.newLine();
      for (int i=0; i<rows; i++) {
        prefix = " "+i;
        for (int j=0; j<columns; j++)
          if (cell[i][j] > 0.0) {
            suffix = " "+j+":"+cell[i][j];
            if (suffix.endsWith(".0")) {
              int p = suffix.length()-2;
              suffix = suffix.substring(0,p);
            }
            if ( (prefix.length()+suffix.length()) < MAX_COLUMN_WIDTH)
              prefix += suffix;
            else {
              bw.write(prefix);
              bw.newLine();
              prefix = " "+i+suffix;
            }
          }

          // Write out prefix if it has trips remaining in it
          if (prefix.indexOf(':') > 0) {
            bw.write(prefix);
            bw.newLine();
          }
      }
      bw.flush();
      bw.close();
    } catch (IOException e) { e.printStackTrace(); }
  }

  // The method will write a matrix in i,j,value format with an optional file
  // header. Note that the number of places past the decimal is not explicitly
  // tied to the precision of other methods in this class, such as bucket-
  // Round(). The user should be consistent (e.g., pass the same variable for
  // number of places to both methods) but that is not enforced. Caveat emptor.
  public void writeTextMatrixFile (String filename, int places) {
    writeTextMatrixFile (filename, places, "null");
  }
  public void writeTextMatrixFile (String filename, int places,
      String header) {
    if (trace) System.out.println("Entering writeTextMatrixFile(): "+
      "filename="+filename);

    // Set up decimal formatting
    DecimalFormat df =
      (DecimalFormat)NumberFormat.getNumberInstance(Locale.US);
    df.setGroupingSize(0);
    df.setMaximumFractionDigits(places);

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
      if (!header.equals("null")) {
        bw.write(header);
        bw.newLine();
      }
      for (int i=0; i<rows; i++)
        for (int j=0; j<columns; j++) {
          if (trace) System.out.println("i="+i+" j="+j+" value="+
            cell[i][j]);
          if (cell[i][j]!=0.0) {
            bw.write(i+","+j+","+df.format(cell[i][j]));
            bw.newLine();
          }
        }
      bw.flush();
      bw.close();
    } catch (IOException e) { e.printStackTrace(); }
  }

  public static void main (String[] args) {
    Matrix2d m = new Matrix2d(6, 6, "no name");
    m.readTextMatrixFile("test.dat");
    m.coalesce(1.5);
    m.writeTextMatrixFile("test.out", 3);
  }

}
