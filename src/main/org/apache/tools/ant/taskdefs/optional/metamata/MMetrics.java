/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;

/**
 * Computes the metrics of a set of Java files and write the results to an XML
 * file. As a convenience, a stylesheet is given in <tt>etc</tt> directory,
 * so that an HTML report can be generated from the XML file.
 * <p>
 * You will not be able to use this task with the evaluation version since
 * as of Metamata 2.0, Metrics does not support command line :-(
 *
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class MMetrics extends AbstractMetamataTask {
/*
    The command line options as of Metamata 2.0 are as follows:

Usage
    mmetrics <option>... <path>...

Parameters
    path              File or directory to measure.

Options
    -arguments   -A   <file>      Includes command line arguments from file.
    -classpath   -cp  <path>      Sets class path (also source path unless one
                                  explicitly set). Overrides METAPATH/CLASSPATH.
    -compilation-units            Measure compilation units.
    -files                        Measure compilation units.
    -format      -f   <format>    Sets output format, default output file type.
    -help        -h               Prints help and exits.
    -indent      -i   <string>    Sets string used to indent labels one level.
    -methods                      Measure methods, types, and compilation units.
    -output      -o   <file>      Sets output file name.
    -quiet       -q               Suppresses copyright message.
    -sourcepath       <path>      Sets source path. Overrides SOURCEPATH.
    -types                        Measure types and compilation units.
    -verbose     -v               Prints all messages.
    -version     -V               Prints version and exits.

Format Options
    comma csv                     Format output as comma-separated text.
    html htm                      Format output as an HTML table.
    tab tab-separated tsv         Format output as tab-separated text.
    text txt                      Format output as space-aligned text.
*/

    /** the granularity mode. Should be one of 'files', 'methods' and 'types'. */
    private String granularity = null;

    /** the XML output file */
    private File outFile = null;

    /** the location of the temporary txt report */
    private File tmpFile;

    private Path path = null;

    //--------------------------- PUBLIC METHODS -------------------------------

    /** default constructor */
    public MMetrics() {
        super("com.metamata.sc.MMetrics");
    }

    /**
     * Attributes for granularity.
     */
    public static class GranularityAttribute extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{"compilation-units", "files", "methods", "types", "packages"};
        }
    }

    /**
     * set the granularity of the audit. Should be one of 'files', 'methods'
     * or 'types'.
     * @param granularity   the audit reporting mode.
     */
    public void setGranularity(GranularityAttribute granularity) {
        this.granularity = granularity.getValue();
    }

    /**
     * Set the output XML file
     * @param file the xml file to write the XML report to.
     */
    public void setTofile(File file) {
        this.outFile = file;
    }

    /**
     * Set a new path (directory) to measure metrics from.
     * @return the path instance to use.
     */
    public Path createPath() {
        if (path == null) {
            path = new Path(project);
        }
        return path;

    }

    //------------------- PROTECTED / PRIVATE METHODS --------------------------


    // check for existing options and outfile, all other are optional
    protected void checkOptions() throws BuildException {
        super.checkOptions();

        if (outFile == null) {
            throw new BuildException("Output XML file must be set via 'tofile' attribute.");
        }
        if (path == null && fileSets.size() == 0) {
            throw new BuildException("Must set either paths (path element) or files (fileset element)");
        }
        // I don't accept dirs and files at the same time, I cannot recognize the semantic in the result
        if (path != null && fileSets.size() > 0) {
            throw new BuildException("Cannot set paths (path element) and files (fileset element) at the same time");
        }
        tmpFile = createTmpFile();
    }

    protected void execute0(ExecuteStreamHandler handler) throws BuildException {
        super.execute0(handler);
        transformFile();
    }

    /**
     * transform the generated file via the handler
     * This function can either be called if the result is written to the output
     * file via -output or we could use the handler directly on stdout if not.
     * @see #createStreamHandler()
     */
    protected void transformFile() throws BuildException {
        FileInputStream tmpStream = null;
        try {
            tmpStream = new FileInputStream(tmpFile);
        } catch (IOException e) {
            throw new BuildException("Error reading temporary file: " + tmpFile, e);
        }
        FileOutputStream xmlStream = null;
        try {
            xmlStream = new FileOutputStream(outFile);
            ExecuteStreamHandler xmlHandler = new MMetricsStreamHandler(this, xmlStream);
            xmlHandler.setProcessOutputStream(tmpStream);
            xmlHandler.start();
            xmlHandler.stop();
        } catch (IOException e) {
            throw new BuildException("Error creating output file: " + outFile, e);
        } finally {
            if (xmlStream != null) {
                try {
                    xmlStream.close();
                } catch (IOException ignored) {
                }
            }
            if (tmpStream != null) {
                try {
                    tmpStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }


    /** cleanup the temporary txt report */
    protected void cleanUp() throws BuildException {
        try {
            super.cleanUp();
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
                tmpFile = null;
            }
        }
    }

    /**
     * if the report is transform via a temporary txt file we should use a
     * a normal logger here, otherwise we could use the metrics handler
     * directly to capture and transform the output on stdout to XML.
     */
    protected ExecuteStreamHandler createStreamHandler() {
        // write the report directtly to an XML stream
        // return new MMetricsStreamHandler(this, xmlStream);
        return new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_INFO);
    }


    protected Vector getOptions() {
        Vector options = new Vector(512);
        // there is a bug in Metamata 2.0 build 37. The sourcepath argument does
        // not work. So we will use the sourcepath prepended to classpath. (order
        // is important since Metamata looks at .class and .java)
        if (sourcePath != null) {
            sourcePath.append(classPath); // srcpath is prepended
            classPath = sourcePath;
            sourcePath = null; // prevent from using -sourcepath
        }

        // don't forget to modify the pattern if you change the options reporting
        if (classPath != null) {
            options.addElement("-classpath");
            options.addElement(classPath.toString());
        }
        options.addElement("-output");
        options.addElement(tmpFile.toString());

        options.addElement("-" + granularity);

        // display the metamata copyright
        // options.addElement( "-quiet");
        options.addElement("-format");

        // need this because that's what the handler is using, it's
        // way easier to process than any other separator
        options.addElement("tab");

        // specify a / as the indent character, used by the handler.
        options.addElement("-i");
        options.addElement("/");

        // directories
        String[] dirs = path.list();
        for (int i = 0; i < dirs.length; i++) {
            options.addElement(dirs[i]);
        }
        // files next.
        addAllVector(options, includedFiles.keys());
        return options;
    }

}
