/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;

import java.io.*;
import java.util.*;

/**
 * This task makes it easy to generate Javadoc documentation for a collection
 * of source code.
 *
 * <P>Current known limitations are:
 *
 * <P><UL>
 *    <LI>patterns must be of the form "xxx.*", every other pattern doesn't
 *        work.
 *    <LI>the java comment-stripper reader is horribly slow
 *    <LI>there is no control on arguments sanity since they are left
 *        to the javadoc implementation.
 *    <LI>argument J in javadoc1 is not supported (what is that for anyway?)
 * </UL>
 *
 * <P>If no <CODE>doclet</CODE> is set, then the <CODE>version</CODE> and
 * <CODE>author</CODE> are by default <CODE>"yes"</CODE>.
 *
 * <P>Note: This task is run on another VM because the Javadoc code calls
 * <CODE>System.exit()</CODE> which would break Ant functionality.
 *
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Patrick Chanezon <a href="mailto:chanezon@netscape.com">chanezon@netscape.com</a>
 * @author Ernst de Haan <a href="mailto:ernst@jollem.com">ernst@jollem.com</a>
 */

public class Javadoc extends Exec {

    public class DocletParam {
        private String name;
        private String value;
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public class DocletInfo {
        private String name;
        private Path path;
        
        private Vector params = new Vector();
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public void setPath(Path path) {
            if (this.path == null) {
                this.path = path;
            } else {
                this.path.append(path);
            }
        }

        public Path getPath() {
            return path;
        }
        
        public Path createPath() {
            if (path == null) {
                path = new Path();
            }
            return path;
        }

        public DocletParam createParam() {
            DocletParam param = new DocletParam();
            params.addElement(param);
            
            return param;
        }
        
        public Enumeration getParams() {
            return params.elements();
        }
    }

    private String maxmemory = null;
    private Path sourcePath = null;
    private String additionalParam = null;
    private File destDir = null;
    private File overviewFile = null;
    private String sourceFiles = null;
    private String packageNames = null;
    private boolean pub = false;
    private boolean prot = false;
    private boolean pack = false;
    private boolean priv = false;
    private boolean author = true;
    private boolean version = true;
    private DocletInfo doclet = null;
    private boolean old = false;
    private Path classpath = null;
    private Path bootclasspath = null;
    private String extdirs = null;
    private boolean verbose = false;
    private String locale = null;
    private String encoding = null;
    private boolean use = false;
    private boolean splitindex = false;
    private String windowtitle = null;
    private String doctitle = null;
    private String header = null;
    private String footer = null;
    private String bottom = null;
    private String linkoffline = null;
    private String link = null;
    private String group = null;
    private boolean nodeprecated = false;
    private boolean nodeprecatedlist = false;
    private boolean notree = false;
    private boolean noindex = false;
    private boolean nohelp = false;
    private boolean nonavbar = false;
    private boolean serialwarn = false;
    private File stylesheetfile = null;
    private File helpfile = null;
    private String docencoding = null;
    private Vector compileList = new Vector(10);
    private String packageList = null;
    private Vector links = new Vector(2);
    private Vector groups = new Vector(2);
    private String charset = null;


    public void setMaxmemory(String src){
        maxmemory = src;
    }

    public void setadditionalParam(String src){
        additionalParam = src;
    }
    
    public void setSourcepath(Path src) {
        if (sourcePath == null) {
            sourcePath = src;
        } else {
            sourcePath.append(src);
        }
    }
    public Path createSourcepath() {
        if (sourcePath == null) {
            sourcePath = new Path();
        }
        return sourcePath;
    }
    public void setDestdir(String src) {
        destDir = project.resolveFile(src);
    }
    public void setSourcefiles(String src) {
        sourceFiles = src;
    }
    public void setPackagenames(String src) {
        packageNames = src;
    }
    public void setOverview(String src) {
        overviewFile = project.resolveFile(src);
    }
    public void setPublic(String src) {
        pub = Project.toBoolean(src);
    }
    public void setProtected(String src) {
        prot = Project.toBoolean(src);
    }
    public void setPackage(String src) {
        pack = Project.toBoolean(src);
    }
    public void setPrivate(String src) {
        priv = Project.toBoolean(src);
    }
    public void setDoclet(String src) {
        if (doclet == null) {
            doclet = new DocletInfo();
        }
        doclet.setName(src);
    }
    
    public void setDocletPath(Path src) {
        if (doclet == null) {
            doclet = new DocletInfo();
        }
        doclet.setPath(src);
    }

    public DocletInfo createDoclet() {
        doclet = new DocletInfo();
        return doclet;
    }

    public void setOld(String src) {
        old = Project.toBoolean(src);
    }
    public void setClasspath(Path src) {
        if (classpath == null) {
            classpath = src;
        } else {
            classpath.append(src);
        }
    }
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path();
        }
        return classpath;
    }
    public void setBootclasspath(Path src) {
        if (bootclasspath == null) {
            bootclasspath = src;
        } else {
            bootclasspath.append(src);
        }
    }
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path();
        }
        return bootclasspath;
    }
    public void setExtdirs(String src) {
        extdirs = src;
    }
    public void setVerbose(String src) {
        verbose = Project.toBoolean(src);
    }
    public void setLocale(String src) {
        locale = src;
    }
    public void setEncoding(String src) {
        encoding = src;
    }
    public void setVersion(String src) {
        version = Project.toBoolean(src);
    }
    public void setUse(String src) {
        use = Project.toBoolean(src);
    }
    public void setAuthor(String src) {
        author = Project.toBoolean(src);
    }
    public void setSplitindex(String src) {
        splitindex = Project.toBoolean(src);
    }
    public void setWindowtitle(String src) {
        windowtitle = src;
    }
    public void setDoctitle(String src) {
        doctitle = src;
    }
    public void setHeader(String src) {
        header = src;
    }
    public void setFooter(String src) {
        footer = src;
    }
    public void setBottom(String src) {
        bottom = src;
    }
    public void setLinkoffline(String src) {
        linkoffline = src;
    }
    public void setGroup(String src) {
        group = src;
    }
    public void setLink(String src) {
        link = src;
    }
    public void setNodeprecated(String src) {
        nodeprecated = Project.toBoolean(src);
    }
    public void setNodeprecatedlist(String src) {
        nodeprecatedlist = Project.toBoolean(src);
    }
    public void setNotree(String src) {
        notree = Project.toBoolean(src);
    }
    public void setNoindex(String src) {
        noindex = Project.toBoolean(src);
    }
    public void setNohelp(String src) {
        nohelp = Project.toBoolean(src);
    }
    public void setNonavbar(String src) {
        nonavbar = Project.toBoolean(src);
    }
    public void setSerialwarn(String src) {
        serialwarn = Project.toBoolean(src);
    }
    public void setStylesheetfile(String src) {
        stylesheetfile = project.resolveFile(src);
    }
    public void setDocencoding(String src) {
        docencoding = src;
    }
    public void setPackageList(String src) {
        packageList = src;
    }
    
    public LinkArgument createLink() {
        LinkArgument la = new LinkArgument();
        links.addElement(la);
        return la;
    }
    
    public class LinkArgument {
        private String href;
        private boolean offline = false;
        private String packagelistLoc;
        
        public LinkArgument() {
        }

        public void setHref(String hr) {
            href = hr;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setPackagelistLoc(String src) {
            packagelistLoc = src;
        }
        
        public String getPackagelistLoc() {
            return packagelistLoc;
        }
        
        public void setOffline(String offline) {
            this.offline = Project.toBoolean(offline);
        }
        
        public boolean isLinkOffline() {
            return offline;
        }
    }
    
    public GroupArgument createGroup() {
        GroupArgument ga = new GroupArgument();
        groups.addElement(ga);
        return ga;
    }

    public class GroupArgument {
        private String title;
        private String packages;

        public GroupArgument() {
        }

        public void setTitle(String src) {
            title = src;
        }

        public String getTitle() {
            return title;
        }

        public void setPackages(String src) {
            packages = src;
        }

        public String getPackages() {
            return packages;
        }
    }
    
    public void setCharset(String src) {
        charset = src;
    }

    public void execute() throws BuildException {
        if (sourcePath == null || destDir == null ) {
            String msg = "sourcePath and destDir attributes must be set!";
            throw new BuildException(msg);
        }

        boolean javadoc1 = (Project.getJavaVersion() == Project.JAVA_1_1);

        log("Generating Javadoc", Project.MSG_INFO);

        Vector argList = new Vector();

// ------------------------------------------------ general javadoc arguments
        if (classpath == null)
            classpath = Path.systemClasspath;


        if(maxmemory != null){
            if(javadoc1){
                argList.addElement("-J-mx" + maxmemory);
            }
            else{
                argList.addElement("-J-Xmx" + maxmemory);
            }
        }

        if ( (!javadoc1) || (sourcePath == null) ) {
            argList.addElement("-classpath");
            argList.addElement(classpath.toString());
            if (sourcePath != null) {
                argList.addElement("-sourcepath");
                argList.addElement(sourcePath.toString());
            }
        } else {
            argList.addElement("-classpath");
            argList.addElement(sourcePath.toString() +
                System.getProperty("path.separator") + classpath.toString());
        }

        if (destDir != null) {
            argList.addElement("-d");
            argList.addElement(destDir.getAbsolutePath());
        }
        if (version && doclet == null)
            argList.addElement ("-version");
        if (nodeprecated)
            argList.addElement ("-nodeprecated");
        if (author && doclet == null)
            argList.addElement ("-author");
        if (noindex)
            argList.addElement ("-noindex");
        if (notree)
            argList.addElement ("-notree");
        if (pub)
            argList.addElement ("-public");
        if (prot)
            argList.addElement ("-protected");
        if (pack)
            argList.addElement ("-package");
        if (priv)
            argList.addElement ("-private");
        if (encoding != null) {
            argList.addElement("-encoding");
            argList.addElement(encoding);
        }
        if (docencoding != null) {
            argList.addElement("-docencoding");
            argList.addElement(docencoding);
        }

// --------------------------------- javadoc2 arguments for default doclet

// XXX: how do we handle a custom doclet?

        if (!javadoc1) {
            if (overviewFile != null) {
                argList.addElement("-overview");
                argList.addElement(overviewFile.getAbsolutePath());
            }
            if (old)
                argList.addElement("-1.1");
            if (verbose)
                argList.addElement("-verbose");
            if (use)
                argList.addElement("-use");
            if (splitindex)
                argList.addElement("-splitindex");
            if (nodeprecatedlist)
                argList.addElement("-nodeprecatedlist");
            if (nohelp)
                argList.addElement("-nohelp");
            if (nonavbar)
                argList.addElement("-nonavbar");
            if (serialwarn)                     
                argList.addElement("-serialwarn");
            if (doclet != null) {
                if (doclet.getName() == null) {
                    throw new BuildException("The doclet name must be specified.");
                }
                else {                
                    argList.addElement("-doclet");
                    argList.addElement(doclet.getName());
                    if (doclet.getPath() != null) {
                        argList.addElement("-docletpath");
                        argList.addElement(doclet.getPath().toString());
                    }
                    for (Enumeration e = doclet.getParams(); e.hasMoreElements();) {
                        DocletParam param = (DocletParam)e.nextElement();
                        if (param.getName() == null) {
                            throw new BuildException("Doclet parameters must have a name");
                        }
                        
                        argList.addElement(param.getName());
                        if (param.getValue() != null) {
                            argList.addElement(param.getValue());
                        }
                    }                        
                }
            } 
            if (bootclasspath != null) {
                argList.addElement("-bootclasspath");
                argList.addElement(bootclasspath.toString());
            }
            if (extdirs != null) {
                argList.addElement("-extdirs");
                argList.addElement(extdirs);
            }
            if (locale != null) {
                argList.addElement("-locale");
                argList.addElement(locale);
            }
            if (encoding != null) {
                argList.addElement("-encoding");
                argList.addElement(encoding);
            }
            if (windowtitle != null) {
                argList.addElement("-windowtitle");
                argList.addElement(windowtitle);
            }
            if (doctitle != null) {
                argList.addElement("-doctitle");
                argList.addElement(doctitle);
            }
            if (header != null) {
                argList.addElement("-header");
                argList.addElement(header);
            }
            if (footer != null) {
                argList.addElement("-footer");
                argList.addElement(footer);
            }
            if (bottom != null) {
                argList.addElement("-bottom");
                argList.addElement(bottom);
            }
            
            // add the single link arguments
            if (link != null) {
                argList.addElement("-link");
                argList.addElement(link);
            }
            
            // add the links arguments
            if (links.size() != 0) {
                for (Enumeration e = links.elements(); e.hasMoreElements(); ) {
                    LinkArgument la = (LinkArgument)e.nextElement();
                
                    if (la.getHref() == null) {
                        throw new BuildException("Links must provide the RUL to the external class documentation.");
                    }
                
                    if (la.isLinkOffline()) {
                        String packageListLocation = la.getPackagelistLoc();
                        if (packageListLocation == null) {
                            throw new BuildException("The package list location for link " + la.getHref() +
                                                     " must be provided because the link is offline");
                        }
                        argList.addElement("-linkoffline");
                        argList.addElement(la.getHref());
                        argList.addElement(packageListLocation);
                    }
                    else {
                        argList.addElement("-link");
                        argList.addElement(la.getHref());
                    }
                }
            }                                   
                                                
            // add the single linkoffline arguments
            if (linkoffline != null) {
                argList.addElement("-linkoffline");
                argList.addElement(linkoffline);
            }
            
            // add the single group arguments
            // Javadoc 1.2 rules:
            //   Multiple -group args allowed.
            //   Each arg includes 3 strings: -group [name] [packagelist].
            //   Elements in [packagelist] are colon-delimited.
            //   An element in [packagelist] may end with the * wildcard.

            // Ant javadoc task rules for group attribute:
            //   Args are comma-delimited.
            //   Each arg is 2 space-delimited strings.
            //   E.g., group="XSLT_Packages org.apache.xalan.xslt*,XPath_Packages orgapache.xalan.xpath*"
            if (group != null) {
                StringTokenizer tok = new StringTokenizer(group, ",", false);
                while (tok.hasMoreTokens()) {
                  String grp = tok.nextToken().trim();
                  int space = grp.indexOf(" ");
                  if (space > 0){
                    String name = grp.substring(0, space);
                    String pkgList = grp.substring(space + 1);
                    argList.addElement("-group");
                    argList.addElement(name);
                    argList.addElement(pkgList);
                  }
                }
            }
            
            // add the group arguments
            if (groups.size() != 0) {
                for (Enumeration e = groups.elements(); e.hasMoreElements(); ) {
                    GroupArgument ga = (GroupArgument)e.nextElement();
                    String title = ga.getTitle();
                    String packages = ga.getPackages();
                    if (title == null || packages == null) {
                        throw new BuildException("The title and packages must be specified for group elements.");
                    }
                    argList.addElement("-group");
                    argList.addElement(title);
                    argList.addElement(packages);
                }
            }

            if (stylesheetfile != null) {
                argList.addElement("-stylesheetfile");
                argList.addElement(stylesheetfile.getAbsolutePath());
            }
            if (helpfile != null) {
                argList.addElement("-helpfile");
                argList.addElement(helpfile.getAbsolutePath());
            }
            if (charset != null) {
                argList.addElement("-charset");
                argList.addElement(charset);
            }
            if (additionalParam != null) {
                argList.addElement(additionalParam);
            }
        }

        if ((packageNames != null) && (packageNames.length() > 0)) {
            Vector packages = new Vector();
            StringTokenizer tok = new StringTokenizer(packageNames, ",", false);
            while (tok.hasMoreTokens()) {
                String name = tok.nextToken().trim();
                if (name.endsWith(".*")) {
                    packages.addElement(name);
                } else {
                    argList.addElement(name);
                }
            }
            if (packages.size() > 0) {
                evaluatePackages(sourcePath, packages, argList);
            }
        }

        if ((sourceFiles != null) && (sourceFiles.length() > 0)) {
            StringTokenizer tok = new StringTokenizer(sourceFiles, ",", false);
            while (tok.hasMoreTokens()) {
                argList.addElement(tok.nextToken().trim());
            }
        }

         if (packageList != null) {
            argList.addElement("@" + packageList);
        }
        log("Javadoc args: " + argList.toString(), Project.MSG_VERBOSE);

        log("Javadoc execution", Project.MSG_INFO);

        StringBuffer b = new StringBuffer();
        b.append("javadoc ");

        Enumeration e = argList.elements();
        while (e.hasMoreElements()) {
            String arg = (String) e.nextElement();
            if (!arg.startsWith("-")) {
                b.append("\"");
                b.append(arg);
                b.append("\"");
            } else {
                b.append(arg);
            }
            if (e.hasMoreElements()) b.append(" ");
        }

        run(b.toString());
    }

    /**
     * Given a source path, a list of package patterns, fill the given list
     * with the packages found in that path subdirs matching one of the given
     * patterns.
     */
    private void evaluatePackages(Path sourcePath, Vector packages, Vector argList) {
        log("Parsing source files for packages", Project.MSG_INFO);
        log("Source path = " + sourcePath.toString(), Project.MSG_VERBOSE);
        log("Packages = " + packages, Project.MSG_VERBOSE);

        Vector addedPackages = new Vector();
        String[] list = sourcePath.list();
        for (int j=0; j<list.length; j++) {
            File source = project.resolveFile(list[j]);
            
            Hashtable map = mapClasses(source);

            Enumeration e = map.keys();
            while (e.hasMoreElements()) {
                String pack = (String) e.nextElement();
                for (int i = 0; i < packages.size(); i++) {
                    if (matches(pack, (String) packages.elementAt(i))) {
                        if (!addedPackages.contains(pack)) {
                            argList.addElement(pack);
                            addedPackages.addElement(pack);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Implements the pattern matching. For now it's only able to
     * guarantee that "aaa.bbb.ccc" matches "aaa.*" and "aaa.bbb.*"
     * FIXME: this code needs much improvement.
     */
    private boolean matches(String string, String pattern) {
        return string.startsWith(pattern.substring(0, pattern.length() - 2));
    }

    /**
     * Returns an hashtable of packages linked to the last parsed
     * file in that package. This map is use to return a list of unique
     * packages as map keys.
     */
    private Hashtable mapClasses(File path) {
        Hashtable map = new Hashtable();

        Vector files = new Vector();
        getFiles(path, files);

        Enumeration e = files.elements();
        while (e.hasMoreElements()) {
            File file = (File) e.nextElement();
            String packageName = getPackageName(file);
            if (packageName != null) map.put(packageName, file);
        }

        return map;
    }

    /**
     * Fills the given vector with files under the given path filtered
     * by the given file filter.
     */
    private void getFiles(File path, Vector list) {
        if (!path.exists()) {
            throw new BuildException("Path " + path + " does not exist.");
        }

        String[] files = path.list();
        String cwd = path.getPath() + System.getProperty("file.separator");

        if (files != null) {
            int count = 0;
            for (int i = 0; i < files.length; i++) {
                File file = new File(cwd + files[i]);
                if (file.isDirectory()) {
                    getFiles(file, list);
                } else if (files[i].endsWith(".java")) {
                    count++;
                    list.addElement(file);
                }
            }
            if (count > 0) {
                log("found " + count + " source files in " + path, Project.MSG_VERBOSE);
            }
        } else {
            throw new BuildException("Error occurred during " + path + " evaluation.");
        }
    }

    /**
     * Return the package name of the given java source file.
     * This method performs valid java parsing to figure out the package.
     */
    private String getPackageName(File file) {
        String name = null;

        try {
            // do not remove the double buffered reader, this is a _major_ speed up in this special case!
            BufferedReader reader = new BufferedReader(new JavaReader(new BufferedReader(new FileReader(file))));
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    log("Could not evaluate package for " + file, Project.MSG_WARN);
                    return null;
                }
                if (line.trim().startsWith("package ") ||
                    line.trim().startsWith("package\t")) {
                    name = line.substring(8, line.indexOf(";")).trim();
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            log("Exception " + e + " parsing " + file, Project.MSG_WARN);
            return null;
        }

        log(file + " --> " + name, Project.MSG_VERBOSE);

        return name;
    }

    //
    // Override the logging of output in order to filter out Generating
    // messages.  Generating messages are set to a priority of VERBOSE
    // unless they appear after what could be an informational message.
    //
    private String queuedLine = null;
    protected void outputLog(String line, int messageLevel) {
        if (messageLevel==project.MSG_INFO && line.startsWith("Generating ")) {
            if (queuedLine != null) {
                super.outputLog(queuedLine, Project.MSG_VERBOSE);
            }
            queuedLine = line;
        } else {
            if (queuedLine != null) {
                if (line.startsWith("Building "))
                    super.outputLog(queuedLine, Project.MSG_VERBOSE);
                else
                    super.outputLog(queuedLine, Project.MSG_INFO);
                queuedLine = null;
            }
            super.outputLog(line, messageLevel);
        }
    }

    protected void logFlush() {
        if (queuedLine != null) {
            super.outputLog(queuedLine, Project.MSG_VERBOSE);
            queuedLine = null;
        }
        super.logFlush();
    }

    /**
     * This is a java comment and string stripper reader that filters
     * these lexical tokens out for purposes of simple Java parsing.
     * (if you have more complex Java parsing needs, use a real lexer).
     * Since this class heavily relies on the single char read function,
     * you are reccomended to make it work on top of a buffered reader.
     */
    class JavaReader extends FilterReader {

        public JavaReader(Reader in) {
            super(in);
        }

        public int read() throws IOException {
            int c = in.read();
            if (c == '/') {
                c = in.read();
                if (c == '/') {
                    while (c != '\n' && c != -1) c = in.read();
                } else if (c == '*') {
                    while (c != -1) {
                        c = in.read();
                        if (c == '*') {
                            c = in.read();
                            while (c == '*' && c != -1) {
                                c = in.read();
                            }
                            
                            if (c == '/') {
                                c = read();
                                break;
                            }
                        }
                    }
                }
            }
            if (c == '"') {
                while (c != -1) {
                    c = in.read();
                    if (c == '\\') {
                        c = in.read();
                    } else if (c == '"') {
                        c = read();
                        break;
                    }
                }
            }
            if (c == '\'') {
                c = in.read();
                if (c == '\\') c = in.read();
                c = in.read();
                c = read();
            }
            return c;
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                int c = read();
                if (c == -1) {
                    if (i == 0) {
                        return -1;
                    } else {
                        return i;
                    }
                }
                cbuf[off + i] = (char) c;
            }
            return len;
        }

        public long skip(long n) throws IOException {
            for (long i = 0; i < n; i++) {
                if (in.read() == -1) return i;
            }
            return n;
        }
    }
}
