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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.sitraka;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.DirectoryScanner;
import java.util.Vector;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.Document;

import org.apache.tools.ant.taskdefs.optional.depend.*;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.*;
import java.io.*;

/**
 * Convenient task to run the snapshot merge utility for JProbe Coverage 3.0.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class CovReport extends Task {
    /*
      jpcoverport [options] -output=file -snapshot=snapshot.jpc
      jpcovreport [options] [-paramfile=file] -output=<fileName> -snapshot=<fileName>

      Generate a report based on the indicated snapshot

      -paramfile=file
      A text file containing the report generation options.

      -format=(html|text|xml) defaults to html
      The format of the generated report.

      -type=(executive|summary|detailed|verydetailed) defaults to detailed
      The type of report to be generated. For -format=xml,
      use -type=verydetailed to include source code lines.

      Note: A very detailed report can be VERY large.

      -percent=num            Min 1 Max 101 Default 101
      An integer representing a percentage of coverage.
      Only methods with test case coverage less than the
      percentage are included in reports.

      -filters=string
      A comma-separated list of filters in the form
      <package>.<class>:V, where V can be I for Include or
      E for Exclude. For the default package, omit <package>.

      -filters_method=string
      Optional. A comma-separated list of methods that
      correspond one-to-one with the entries in -filters.

      -output=string  Must be specified
      The absolute path and file name for the generated
      report file.

      -snapshot=string        Must be specified
      The absolute path and file name of the snapshot file.

      -inc_src_text=(on|off)  defaults to on
      Include text of the source code lines.
      Only applies for -format=xml and -type=verydetailed.

      -sourcepath=string      defaults to .
      A semicolon-separated list of source paths.

      /*

      /** coverage home,  mandatory */
    private File home = null;

    /** format of generated report, optional */
    private String format = null;

    /** the name of the output snapshot, mandatory */
    private File tofile = null;

    /** type of report, optional */
    private String type = null;

    /** threshold value for printing methods, optional */
    private Integer percent = null;

    /** comma separated list of filters (???)*/
    private String filters = null;

    /** name of the snapshot file to create report from */
    private File snapshot = null;

    /** sourcepath to use */
    private Path sourcePath = null;

    /** include the text for each line of code (xml report verydetailed)*/
    private boolean includeSource = true;

    private Path coveragePath = null;

    /** */
    private Reference reference = null;


    /**
     * Set the coverage home. it must point to JProbe coverage
     * directories where are stored native libraries and jars.
     */
    public void setHome(File value) {
        this.home = value;
    }

    public static class ReportFormat extends EnumeratedAttribute {
        public String[] getValues(){
            return new String[]{"html", "text", "xml"};
        }
    }
    /** set the format of the report html|text|xml*/
    public void setFormat(ReportFormat value){
        this.format = value.getValue();
    }

    public static class ReportType extends EnumeratedAttribute {
        public String[] getValues(){
            return new String[]{"executive", "summary", "detailed", "verydetailed"};
        }
    }
    /** sets the report type executive|summary|detailed|verydetailed */
    public void setType(ReportType value){
        this.type =  value.getValue();
    }

    /** include source code lines. XML report only */
    public void setIncludesource(boolean value){
        this.includeSource = value;
    }

    /** sets the threshold printing method 0-100*/
    public void setPercent(Integer value){
        this.percent = value;
    }

    /** set the filters */
    public void setFilters(String values){
        this.filters = values;
    }

    public Path createSourcepath(){
        if (sourcePath == null) {
            sourcePath = new Path(project);
        }
        return sourcePath.createPath();
    }

    public void setSnapshot(File value){
        this.snapshot = value;
    }

    /**
     * Set the output snapshot file
     */
    public void setTofile(File value) {
        this.tofile = value;
    }

    //@todo to remove
    public Path createCoveragepath(){
        if (coveragePath == null) {
            coveragePath = new Path(project);
        }
        return coveragePath.createPath();
    }

    public Reference createReference(){
        if (reference == null){
            reference = new Reference();
        }
        return reference;
    }


    public CovReport() {
    }

    /** check for mandatory options */
    protected void checkOptions() throws BuildException {
        if (tofile == null) {
            throw new BuildException("'tofile' attribute must be set.");
        }
        if (snapshot == null) {
            throw new BuildException("'snapshot' attribute must be set.");
        }
        if (home == null) {
            throw new BuildException("'home' attribute must be set to JProbe home directory");
        }
        home = new File(home,"Coverage");
        File jar = new File(home, "coverage.jar");
        if (!jar.exists()) {
            throw new BuildException("Cannot find Coverage directory: " + home);
        }
        if (reference != null && !"xml".equals(format)){
            log("Ignored reference. It cannot be used in non XML report.");
            reference = null; // nullify it so that there is no ambiguity
        }

    }

    public void execute() throws BuildException {
        checkOptions();
        try {
            Commandline cmdl = new Commandline();
            // we need to run Coverage from his directory due to dll/jar issues
            cmdl.setExecutable( new File(home, "jpcovreport").getAbsolutePath() );
            String[] params = getParameters();
            for (int i = 0; i < params.length; i++) {
                cmdl.createArgument().setValue(params[i]);
            }

            // use the custom handler for stdin issues
            LogStreamHandler handler = new LogStreamHandler(this,Project.MSG_INFO,Project.MSG_WARN);
            Execute exec = new Execute( handler );
            log(cmdl.toString(), Project.MSG_VERBOSE);
            exec.setCommandline(cmdl.getCommandline());
            int exitValue = exec.execute();
            if (exitValue != 0) {
                throw new BuildException("JProbe Coverage Report failed (" + exitValue + ")");
            }
            log("coveragePath: " + coveragePath, Project.MSG_VERBOSE);
            log("format: " + format, Project.MSG_VERBOSE);
            if (reference != null && "xml".equals(format)){
                reference.createEnhancedXMLReport();
            }

        } catch (IOException e){
            throw new BuildException("Failed to execute JProbe Coverage Report.", e);
        }
    }


    protected String[] getParameters(){
        Vector v = new Vector();
        if (format != null) {
            v.addElement("-format=" + format);
        }
        if (type != null) {
            v.addElement("-type=" + type);
        }
        if (percent != null) {
            v.addElement("-percent=" + percent);
        }
        if (filters != null) {
            v.addElement("-filters=" + filters);
        }
        v.addElement("-output=" + project.resolveFile(tofile.getPath()));
        v.addElement("-snapshot=" + project.resolveFile(snapshot.getPath()));
        // as a default -sourcepath use . in JProbe, so use project .
        if (sourcePath == null) {
            sourcePath = new Path(project);
            sourcePath.createPath().setLocation(project.resolveFile("."));
        }
        v.addElement("-sourcepath=" + sourcePath);

        if ("verydetailed".equalsIgnoreCase(format) && "xml".equalsIgnoreCase(type)) {
            v.addElement("-inc_src_text=" + (includeSource ? "on" : "off"));
        }

        String[] params = new String[v.size()];
        v.copyInto(params);
        return params;
    }


    public class Reference {
        protected Path classPath;
        protected ReportFilters filters;
        public Path createClasspath(){
            if (classPath == null) {
                classPath = new Path(CovReport.this.project);
            }
            return classPath.createPath();
        }
        public ReportFilters createFilters(){
            if (filters == null){
                filters = new ReportFilters();
            }
            return filters;
        }
        protected void createEnhancedXMLReport() throws BuildException {
            // we need a classpath element
            if (classPath == null){
                throw new BuildException("Need a 'classpath' element.");
            }
            // and a valid one...
            String[] paths = classPath.list();
            if (paths.length == 0){
                throw new BuildException("Coverage path is invalid. It does not contain any existing path.");
            }
            // and we need at least one filter include/exclude.
            if (filters == null || filters.size() == 0){
                createFilters();
                log("Adding default include filter to *.*()", Project.MSG_VERBOSE);
                ReportFilters.Include include = new ReportFilters.Include();
                filters.addInclude( include );
            }
            try {
                log("Creating enhanced XML report", Project.MSG_VERBOSE);
                XMLReport report = new XMLReport(CovReport.this, tofile);
                report.setReportFilters(filters);
                report.setJProbehome( new File(home.getParent()) );
                Document doc = report.createDocument(paths);
                TransformerFactory tfactory = TransformerFactory.newInstance();
                Transformer transformer = tfactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                Source src = new DOMSource(doc);
                Result res = new StreamResult( "file:///" + tofile.toString() );
                transformer.transform(src, res);
            } catch (Exception e){
                throw new BuildException("Error while performing enhanced XML report from file " + tofile, e);
            }
        }
    }
}
