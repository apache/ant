/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional.junit;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 *
 * @author Thomas Haas
 */
public class JUnitTest {
    private boolean systemExit = false;
    private boolean haltOnError = false;
    private boolean haltOnFail = false;
    private boolean printSummary = true;
    private boolean printXml = true;
    private String name = null;
    private String outfile = null;
    private boolean fork = false;

    private long runs, failures, errors;
    private long runTime;

    public JUnitTest() {
    }

    public JUnitTest(boolean fork, boolean haltOnError, boolean haltOnFail, 
                     boolean printSummary, boolean printXml, String name, 
                     String outfile) {
        this.fork = fork;
        this.haltOnError = haltOnError;
        this.haltOnFail = haltOnFail;
        this.printSummary = printSummary;
        this.printXml = printXml;
        this.name  = name;
        this.outfile = outfile;
    }

    public void setFork(boolean value) {
        fork = value;
    }

    public boolean getFork() {
        return fork;
    }

    public void setHaltonerror(boolean value) {
        haltOnError = value;
    }

    public void setHaltonfailure(boolean value) {
        haltOnFail = value;
    }

    public void setPrintsummary(boolean value) {
        printSummary = value;
    }

    public void setPrintxml(boolean value) {
        printXml = value;
    }

    public void setName(String value) {
        name = value;
    }

    public void setOutfile(String value) {
        outfile = value;
    }


    public boolean getHaltonerror() {
        return haltOnError;
    }

    public boolean getHaltonfailure() {
        return haltOnFail;
    }

    public boolean getPrintsummary() {
        return printSummary;
    }

    public boolean getPrintxml() {
        return printXml;
    }

    public String getName() {
        return name;
    }

    public String getOutfile() {
        return outfile;
    }

    public void setCommandline(String [] args) {
        for (int i=0; i<args.length; i++) {
            if (args[i] == null) continue;
            if (args[i].startsWith("haltOnError=")) {
                haltOnError = Project.toBoolean(args[i].substring(12));
            } else if (args[i].startsWith("haltOnFailure=")) {
                haltOnFail = Project.toBoolean(args[i].substring(14));
            } else if (args[i].startsWith("printSummary=")) {
                printSummary = Project.toBoolean(args[i].substring(13));
            } else if (args[i].startsWith("printXML=")) {
                printXml = Project.toBoolean(args[i].substring(9));
            } else if (args[i].startsWith("outfile=")) {
                outfile = args[i].substring(8);
            }
        }
    }

    public String[] getCommandline() {
        final Commandline result = new Commandline();
        if (name != null && name.length() > 0) {
            result.setExecutable(name);
        }
        result.createArgument().setValue("exit=" + systemExit);
        result.createArgument().setValue("haltOnError=" + haltOnError);
        result.createArgument().setValue("haltOnFailure=" + haltOnFail);
        result.createArgument().setValue("printSummary=" + printSummary);
        result.createArgument().setValue("printXML=" + printXml);
        if (outfile != null && outfile.length() > 0) {
            result.createArgument().setValue("outfile=" + outfile);
        }
        return result.getCommandline();
    }

    public void setCounts(long runs, long failures, long errors) {
        this.runs = runs;
        this.failures = failures;
        this.errors = errors;
    }

    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    public long runCount() {return runs;}
    public long failureCount() {return failures;}
    public long errorCount() {return errors;}
    public long getRunTime() {return runTime;}


    public String toString() {
        return Commandline.toString(getCommandline());
    }

}
