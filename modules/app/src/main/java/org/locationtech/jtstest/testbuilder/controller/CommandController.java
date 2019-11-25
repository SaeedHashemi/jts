/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jtstest.testbuilder.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jtstest.geomfunction.GeometryFunctionInvocation;
import org.locationtech.jtstest.testbuilder.JTSTestBuilder;
import org.locationtech.jtstest.util.CommandRunner;
import org.locationtech.jtstest.util.io.MultiFormatReader;

public class CommandController {

  public static void execCommand(String cmdIn) {
    String cmd = expandCommand(cmdIn);
    //System.out.println(cmd);
    int returnCode = -1;
    String errMsg = "";
    Geometry result = null;
    CommandRunner runner = new CommandRunner();
    try {
       returnCode = runner.exec(cmd);
       errMsg = runner.getStderr();
    } catch (Exception e) {
      errMsg = e.getClass().getName() + " : " + e.getMessage();
      //showError(e);
    }
    boolean isSuccess = returnCode == 0 && errMsg.length() == 0;
    
    if (isSuccess) {
      result = loadResult( runner.getStdout() );
    }
    else {
      if (errMsg.length() == 0)
        errMsg = "Return code = " + returnCode;
      //JTSTestBuilder.controller().clearResult();
      JTSTestBuilder.frame().getCommandPanel().setError(errMsg);
    }
    logCommand(cmdIn, result, errMsg);

  }
  private static void logCommand(String cmd, Geometry geom, String errMsg) {
    String cmdLog = "Command: " + limitLength( cmd, 200);
    if (geom != null) {
      String geomLog = GeometryFunctionInvocation.toString(geom);
      cmdLog += "\n ==> " + geomLog;
    }
    if (errMsg.length() > 0) {
      String errLog = limitLength( errMsg, 200);
      cmdLog += "\n ERROR: " + errLog;
    }
    
    JTSTestBuilder.controller().displayInfo(cmdLog, false);
  }
  public static final String VAR_A = "$A";
  public static final String VAR_A_WKT = "$A.wkt";
  
  private static String expandCommand(String cmdSrc) {
    String cmdLine = removeNewline(cmdSrc);
    
    String cmd = cmdLine;
    
    if (cmdLine.contains(VAR_A)) {
      cmd = cmd.replace(VAR_A, valueWKT(0));
    }
    if (cmdLine.contains(VAR_A_WKT)) {
      cmd = cmd.replace(VAR_A_WKT, valueWKT(0));
    }
    return cmd;
  }

  private static String valueWKT(int i) {
    Geometry geom = JTSTestBuilderController.model().getCurrentCase().getGeometry(i);
    if (geom == null) return "";
    return geom.toString();
  }

  private static String removeNewline(String s) {
    return s.replace('\n', ' ');
  }
  
  private static String limitLength(String s, int n) {
    if (s.length() <= n) return s;
    return s.substring(0, n) + "...";
  }
  
  private static Geometry loadResult(String output) {
    JTSTestBuilder.frame().showResultWKTTab();
    MultiFormatReader reader = new MultiFormatReader(new GeometryFactory());
    reader.setStrict(false);
    Geometry result = null;
    try {
      result = reader.read(output);
      JTSTestBuilder.controller().setResult(result);
    } catch (ParseException | IOException e) {
      showError(e);
    }
    return result;
  }
  
  private static void showError(Exception e) {
    //String msg = e.getClass().getName() + " : " + e.getMessage();
    JTSTestBuilder.controller().setResult(e);
  }
  
  // NOT USED
  
  /**
   * Executes a command and returns the contents of stdout as a string.
   * The command should be a single line, otherwise things seem to hang.
   * 
   * @param cmd command to execute (should be a single line)
   * @return text of stdout
   * @throws IOException
   * @throws InterruptedException
   */
  private static String exec(String cmd) throws IOException, InterruptedException {
    // ensure cmd is single line (seems to hang otherwise
    
    boolean isWindows = System.getProperty("os.name")
        .toLowerCase().startsWith("windows");
    // -- Linux --
    // Run a shell command
    // Process process = Runtime.getRuntime().exec("ls /home/foo/");
    // Run a shell script
    // Process process = Runtime.getRuntime().exec("path/to/hello.sh");

    // -- Windows --
    // Run a command
    //Process process = Runtime.getRuntime().exec("cmd /c dir C:\\Users\\foo");
    
    /**
     * Use array form of exec args, because that doesn't do weird things with quotes
     */
    String[] osCmd = new String[3];
    if (isWindows) {
      osCmd[0] = "cmd";
      osCmd[1] = "/c";     
    }
    else {  // assume *nix
      osCmd[0] = "sh";
      osCmd[1] = "-c";
    }
    osCmd[2] = cmd;
    
    Process process = Runtime.getRuntime().exec( osCmd );

    StringBuilder output = new StringBuilder();

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()));

    String line;
    while ((line = reader.readLine()) != null) {
      output.append(line + "\n");
    }

    int exitVal = process.waitFor();
    if (exitVal != 0) {
      // TODO: handle error
      return null;
    }
    return output.toString();
  }
}