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
package org.apache.tools.ant.taskdefs;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.*;

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
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class Javadoc extends Task {

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
                path = new Path(getProject());
            }
            return path.createPath();
        }

        /**
         * Adds a reference to a CLASSPATH defined elsewhere.
         */
        public void setPathRef(Reference r) {
            createPath().setRefid(r);
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

    public static class PackageName {
        private String name;
        public void setName(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public String toString() {
            return getName();
        }
    }

    public static class SourceFile {
        private File file;
        public void setFile(File file) {
            this.file = file;
        }
        public File getFile() {
            return file;
        }
    }

    public static class Html {
        private StringBuffer text = new StringBuffer();
        public void addText(String t) {
            text.append(t);
        }
        public String getText() {
            return text.toString();
        }
    }

    public static class AccessType extends EnumeratedAttribute {
        public String[] getValues() {
            // Protected first so if any GUI tool offers a default
            // based on enum #0, it will be right.
            return new String[] {"protected", "public", "package", "private"};
        }
    }

    private Commandline cmd = new Commandline();
    private static boolean javadoc1 = 
        (Project.getJavaVersion() == Project.JAVA_1_1);


    private void addArgIf(boolean b, String arg) {
        if (b) {
            cmd.createArgument().setValue(arg);
        }
    }

    private void add12ArgIfNotEmpty(String key, String value) {
        if (!javadoc1) {
            if (value != null && value.length() != 0) {
                cmd.createArgument().setValue(key);
                cmd.createArgument().setValue(value);
            } else {
                project.log(this, 
                            "Warning: Leaving out empty argument '" + key + "'", 
                            Project.MSG_WARN);
            }
        } 
    }

    private void add11ArgIf(boolean b, String arg) {
        if (javadoc1 && b) {
            cmd.createArgument().setValue(arg);
        }
    }

    private void add12ArgIf(boolean b, String arg) {
        if (!javadoc1 && b) {
            cmd.createArgument().setValue(arg);
        }
    }

    private boolean foundJavaFile = false;
    private boolean failOnError = false;
    private Path sourcePath = null;
    private File destDir = null;
    private Vector sourceFiles = new Vector();
    private Vector packageNames = new Vector(5);
    private Vector excludePackageNames = new Vector(1);
    private boolean author = true;
    private boolean version = true;
    private DocletInfo doclet = null;
    private Path classpath = null;
    private Path bootclasspath = null;
    private String group = null;
    private Vector compileList = new Vector(10);
    private String packageList = null;
    private Vector links = new Vector(2);
    private Vector groups = new Vector(2);
    private boolean useDefaultExcludes = true;
    private Html doctitle = null;
    private Html header = null;
    private Html footer = null;
    private Html bottom = null;
    private boolean useExternalFile = false;
    private File tmpList = null;

    /**
     * Work around command line length limit by using an external file
     * for the sourcefiles.
     */
    public void setUseExternalFile(boolean b) {
        if (!javadoc1) {
            useExternalFile = b;
        }
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions 
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
       this.useDefaultExcludes = useDefaultExcludes;
    }

    public void setMaxmemory(String max){
        if(javadoc1){
            cmd.createArgument().setValue("-J-mx" + max);
        } else{
            cmd.createArgument().setValue("-J-Xmx" + max);
        }
    }

    public void setAdditionalparam(String add){
        cmd.createArgument().setLine(add);
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
            sourcePath = new Path(project);
        }
        return sourcePath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     */
    public void setSourcepathRef(Reference r) {
        createSourcepath().setRefid(r);
    }

    public void setDestdir(File dir) {
        destDir = dir;
        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(destDir);
    }
    public void setSourcefiles(String src) {
        StringTokenizer tok = new StringTokenizer(src, ",");
        while (tok.hasMoreTokens()) {
            String f = tok.nextToken();
            SourceFile sf = new SourceFile();
            sf.setFile(project.resolveFile(f));
            addSource(sf);
        }
    }
    public void addSource(SourceFile sf) {
        sourceFiles.addElement(sf);
    }
    public void setPackagenames(String src) {
        StringTokenizer tok = new StringTokenizer(src, ",");
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName(p);
            addPackage(pn);
        }
    }
    public void addPackage(PackageName pn) {
        packageNames.addElement(pn);
    }

    public void setExcludePackageNames(String src) {
        StringTokenizer tok = new StringTokenizer(src, ",");
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName(p);
            addExcludePackage(pn);
        }
    }
    public void addExcludePackage(PackageName pn) {
        excludePackageNames.addElement(pn);
    }

    public void setOverview(File f) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-overview");
            cmd.createArgument().setFile(f);
        }
    }
    public void setPublic(boolean b) {
        addArgIf(b, "-public");
    }
    public void setProtected(boolean b) {
        addArgIf(b, "-protected");
    }
    public void setPackage(boolean b) {
        addArgIf(b, "-package");
    }
    public void setPrivate(boolean b) {
        addArgIf(b, "-private");
    }
    public void setAccess(AccessType at) {
        cmd.createArgument().setValue("-" + at.getValue());
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

    public void setDocletPathRef(Reference r) {
        if (doclet == null) {
            doclet = new DocletInfo();
        }
        doclet.createPath().setRefid(r);
    }

    public DocletInfo createDoclet() {
        doclet = new DocletInfo();
        return doclet;
    }

    public void setOld(boolean b) {
        add12ArgIf(b, "-1.1");
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
            classpath = new Path(project);
        }
        return classpath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
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
            bootclasspath = new Path(project);
        }
        return bootclasspath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     */
    public void setBootClasspathRef(Reference r) {
        createBootclasspath().setRefid(r);
    }

    public void setExtdirs(String src) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-extdirs");
            cmd.createArgument().setValue(src);
        }
    }
    public void setVerbose(boolean b) {
        add12ArgIf(b, "-verbose");
    }
    public void setLocale(String src) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-locale");
            cmd.createArgument().setValue(src);
        }
    }
    public void setEncoding(String enc) {
        cmd.createArgument().setValue("-encoding");
        cmd.createArgument().setValue(enc);
    }
    public void setVersion(boolean src) {
        version = src;
    }
    public void setUse(boolean b) {
        add12ArgIf(b, "-use");
    }
    public void setAuthor(boolean src) {
        author = src;
    }
    public void setSplitindex(boolean b) {
        add12ArgIf(b, "-splitindex");
    }
    public void setWindowtitle(String src) {
        add12ArgIfNotEmpty("-windowtitle", src);
    }
    public void setDoctitle(String src) {
        Html h = new Html();
        h.addText(src);
        addDoctitle(h);
    }
    public void addDoctitle(Html text) {
        if (!javadoc1) {
            doctitle = text;
        }
    }
    public void setHeader(String src) {
        Html h = new Html();
        h.addText(src);
        addHeader(h);
    }
    public void addHeader(Html text) {
        if (!javadoc1) {
            header = text;
        }
    }

    public void setFooter(String src) {
        Html h = new Html();
        h.addText(src);
        addFooter(h);
    }
    public void addFooter(Html text) {
        if (!javadoc1) {
            footer = text;
        }
    }

    public void setBottom(String src) {
        Html h = new Html();
        h.addText(src);
        addBottom(h);
    }
    public void addBottom(Html text) {
        if (!javadoc1) {
            bottom = text;
        }
    }

    public void setLinkoffline(String src) {
        if (!javadoc1) {
            LinkArgument le = createLink();
            le.setOffline(true);
            String linkOfflineError = "The linkoffline attribute must include a URL and " + 
                "a package-list file location separated by a space";
            if (src.trim().length() == 0) {
                throw new BuildException(linkOfflineError);
            }                
            StringTokenizer tok = new StringTokenizer(src, " ", false);
            le.setHref(tok.nextToken());

            if (!tok.hasMoreTokens()) {
                throw new BuildException(linkOfflineError);
            }                                        
            le.setPackagelistLoc(project.resolveFile(tok.nextToken()));
        }
    }
    public void setGroup(String src) {
        group = src;
    }
    public void setLink(String src) {
        if (!javadoc1) {
            createLink().setHref(src);
        }
    }
    public void setNodeprecated(boolean b) {
        addArgIf(b, "-nodeprecated");
    }
    public void setNodeprecatedlist(boolean b) {
        add12ArgIf(b, "-nodeprecatedlist");
    }
    public void setNotree(boolean b) {
        addArgIf(b, "-notree");
    }
    public void setNoindex(boolean b) {
        addArgIf(b, "-noindex");
    }
    public void setNohelp(boolean b) {
        add12ArgIf(b, "-nohelp");
    }
    public void setNonavbar(boolean b) {
        add12ArgIf(b, "-nonavbar");
    }
    public void setSerialwarn(boolean b) {
        add12ArgIf(b, "-serialwarn");
    }
    public void setStylesheetfile(File f) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-stylesheetfile");
            cmd.createArgument().setFile(f);
        }
    }
    public void setHelpfile(File f) {
        if (!javadoc1) {
            cmd.createArgument().setValue("-helpfile");
            cmd.createArgument().setFile(f);
        }
    }
    public void setDocencoding(String enc) {
        cmd.createArgument().setValue("-docencoding");
        cmd.createArgument().setValue(enc);
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
        private File packagelistLoc;
        
        public LinkArgument() {
        }

        public void setHref(String hr) {
            href = hr;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setPackagelistLoc(File src) {
            packagelistLoc = src;
        }
        
        public File getPackagelistLoc() {
            return packagelistLoc;
        }
        
        public void setOffline(boolean offline) {
            this.offline = offline;
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
        private Html title;
        private Vector packages = new Vector(3);

        public GroupArgument() {
        }

        public void setTitle(String src) {
            Html h = new Html();
            h.addText(src);
            addTitle(h);
        }
        public void addTitle(Html text) {
            title = text;
        }

        public String getTitle() {
            return title != null ? title.getText() : null;
        }

        public void setPackages(String src) {
            StringTokenizer tok = new StringTokenizer(src, ",");
            while (tok.hasMoreTokens()) {
                String p = tok.nextToken();
                PackageName pn = new PackageName();
                pn.setName(p);
                addPackage(pn);
            }
        }
        public void addPackage(PackageName pn) {
            packages.addElement(pn);
        }

        public String getPackages() {
            StringBuffer p = new StringBuffer();
            for (int i = 0; i < packages.size(); i++) {
                if ( i > 0 ) {
                    p.append( ":" );
                }
                p.append( packages.elementAt(i).toString() );
            }
            return p.toString();
        }
    }
    
    public void setCharset(String src) {
        this.add12ArgIfNotEmpty("-charset", src);
    }

    /**
     * Should the build process fail if javadoc fails (as indicated by
     * a non zero return code)?
     *
     * <p>Default is false.</p>
     */
    public void setFailonerror(boolean b) {
        failOnError = b;
    }

    public void execute() throws BuildException {
        if ("javadoc2".equals(taskType)) {
            log("!! javadoc2 is deprecated. Use javadoc instead. !!");
        }

        if (sourcePath == null) {
            String msg = "sourcePath attribute must be set!";
            throw new BuildException(msg);
        }

        log("Generating Javadoc", Project.MSG_INFO);

        if (doctitle != null) {
            cmd.createArgument().setValue("-doctitle");
            cmd.createArgument().setValue(expand(doctitle.getText()));
        }
        if (header != null) {
            cmd.createArgument().setValue("-header");
            cmd.createArgument().setValue(expand(header.getText()));
        }
        if (footer != null) {
            cmd.createArgument().setValue("-footer");
            cmd.createArgument().setValue(expand(footer.getText()));
        }
        if (bottom != null) {
            cmd.createArgument().setValue("-bottom");
            cmd.createArgument().setValue(expand(bottom.getText()));
        }

        Commandline toExecute = (Commandline)cmd.clone();
	toExecute.setExecutable( getJavadocExecutableName() );

// ------------------------------------------------ general javadoc arguments
        if (classpath == null)
            classpath = Path.systemClasspath;
        else
            classpath = classpath.concatSystemClasspath("ignore");

        if (!javadoc1) {
            toExecute.createArgument().setValue("-classpath");
            toExecute.createArgument().setPath(classpath);
            toExecute.createArgument().setValue("-sourcepath");
            toExecute.createArgument().setPath(sourcePath);
        } else {
            toExecute.createArgument().setValue("-classpath");
            toExecute.createArgument().setValue(sourcePath.toString() +
                                                System.getProperty("path.separator") + classpath.toString());
        }

        if (version && doclet == null)
            toExecute.createArgument().setValue("-version");
        if (author && doclet == null)
            toExecute.createArgument().setValue("-author");

        if (javadoc1 || doclet == null) {
            if (destDir == null) {
                String msg = "destDir attribute must be set!";
                throw new BuildException(msg);
            }
        }
        

// --------------------------------- javadoc2 arguments for default doclet

// XXX: how do we handle a custom doclet?

        if (!javadoc1) {
            if (doclet != null) {
                if (doclet.getName() == null) {
                    throw new BuildException("The doclet name must be specified.", location);
                }
                else {                
                    toExecute.createArgument().setValue("-doclet");
                    toExecute.createArgument().setValue(doclet.getName());
                    if (doclet.getPath() != null) {
                        toExecute.createArgument().setValue("-docletpath");
                        toExecute.createArgument().setPath(doclet.getPath());
                    }
                    for (Enumeration e = doclet.getParams(); e.hasMoreElements();) {
                        DocletParam param = (DocletParam)e.nextElement();
                        if (param.getName() == null) {
                            throw new BuildException("Doclet parameters must have a name");
                        }
                        
                        toExecute.createArgument().setValue(param.getName());
                        if (param.getValue() != null) {
                            toExecute.createArgument().setValue(param.getValue());
                        }
                    }                        
                }
            } 
            if (bootclasspath != null) {
                toExecute.createArgument().setValue("-bootclasspath");
                toExecute.createArgument().setPath(bootclasspath);
            }
            
            // add the links arguments
            if (links.size() != 0) {
                for (Enumeration e = links.elements(); e.hasMoreElements(); ) {
                    LinkArgument la = (LinkArgument)e.nextElement();
                
                    if (la.getHref() == null) {
                        throw new BuildException("Links must provide the URL to the external class documentation.");
                    }
                
                    if (la.isLinkOffline()) {
                        File packageListLocation = la.getPackagelistLoc();
                        if (packageListLocation == null) {
                            throw new BuildException("The package list location for link " + la.getHref() +
                                                     " must be provided because the link is offline");
                        }
                        File packageList = new File(packageListLocation, "package-list");
                        if (packageList.exists()) {
                            toExecute.createArgument().setValue("-linkoffline");
                            toExecute.createArgument().setValue(la.getHref());
                            toExecute.createArgument().setValue(packageListLocation.getAbsolutePath());
                        }
                        else {
                            log("Warning: No package list was found at " + packageListLocation, 
                                Project.MSG_VERBOSE);
                        }
                    }
                    else {
                        toExecute.createArgument().setValue("-link");
                        toExecute.createArgument().setValue(la.getHref());
                    }
                }
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
            //   E.g., group="XSLT_Packages org.apache.xalan.xslt*,XPath_Packages org.apache.xalan.xpath*"
            if (group != null) {
                StringTokenizer tok = new StringTokenizer(group, ",", false);
                while (tok.hasMoreTokens()) {
                    String grp = tok.nextToken().trim();
                    int space = grp.indexOf(" ");
                    if (space > 0){
                        String name = grp.substring(0, space);
                        String pkgList = grp.substring(space + 1);
                        toExecute.createArgument().setValue("-group");
                        toExecute.createArgument().setValue(name);
                        toExecute.createArgument().setValue(pkgList);
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
                    toExecute.createArgument().setValue("-group");
                    toExecute.createArgument().setValue(expand(title));
                    toExecute.createArgument().setValue(packages);
                }
            }

        }

        tmpList = null;
        if (packageNames.size() > 0) {
            Vector packages = new Vector();
            Enumeration enum = packageNames.elements();
            while (enum.hasMoreElements()) {
                PackageName pn = (PackageName) enum.nextElement();
                String name = pn.getName().trim();
                if (name.endsWith(".*")) {
                    packages.addElement(name);
                } else {
                    toExecute.createArgument().setValue(name);
                }
            }

            Vector excludePackages = new Vector();
            if (excludePackageNames.size() > 0) {
                enum = excludePackageNames.elements();
                while (enum.hasMoreElements()) {
                    PackageName pn = (PackageName) enum.nextElement();
                    excludePackages.addElement(pn.getName().trim());
                }
            }
            if (packages.size() > 0) {
                evaluatePackages(toExecute, sourcePath, packages, excludePackages);
            }
        }

        if (sourceFiles.size() > 0) {
            PrintWriter srcListWriter = null;
            try {

                /**
                 * Write sourcefiles to a temporary file if requested.
                 */
                if (useExternalFile) {
                    if (tmpList == null) {
                        tmpList = createTempFile();
                        toExecute.createArgument().setValue("@" + tmpList.getAbsolutePath());
                    }
                    srcListWriter = new PrintWriter(new FileWriter(tmpList.getAbsolutePath(), 
                                                                   true));
                }
            
                Enumeration enum = sourceFiles.elements();
                while (enum.hasMoreElements()) {
                    SourceFile sf = (SourceFile) enum.nextElement();
                    String sourceFileName = sf.getFile().getAbsolutePath();
                    if (useExternalFile) {
                        srcListWriter.println(sourceFileName);
                    } else {
                        toExecute.createArgument().setValue(sourceFileName);
                    }
                }

            } catch (IOException e) {
                throw new BuildException("Error creating temporary file", 
                                         e, location);
            } finally {
                if (srcListWriter != null) {
                    srcListWriter.close();
                }
            }
        }

        if (packageList != null) {
            toExecute.createArgument().setValue("@" + packageList);
        }
        log("Javadoc args: " + toExecute, Project.MSG_VERBOSE);

        log("Javadoc execution", Project.MSG_INFO);

        JavadocOutputStream out = new JavadocOutputStream(Project.MSG_INFO);
        JavadocOutputStream err = new JavadocOutputStream(Project.MSG_WARN);
        Execute exe = new Execute(new PumpStreamHandler(out, err));
        exe.setAntRun(project);
        
        /*
         * No reason to change the working directory as all filenames and
         * path components have been resolved already.
         *
         * Avoid problems with command line length in some environments.
         */
        exe.setWorkingDirectory(null);
        try {
            exe.setCommandline(toExecute.getCommandline());
            int ret = exe.execute();
            if (ret != 0 && failOnError) {
                throw new BuildException("Javadoc returned "+ret, location);
            }
        } catch (IOException e) {
            throw new BuildException("Javadoc failed: " + e, e, location);
        } finally {

            if (tmpList != null) {
                tmpList.delete();
                tmpList = null;
            }
            
            out.logFlush();
            err.logFlush();
            try {
                out.close();
                err.close();
            } catch (IOException e) {}
        }
    }

    /**
     * Given a source path, a list of package patterns, fill the given list
     * with the packages found in that path subdirs matching one of the given
     * patterns.
     */
    private void evaluatePackages(Commandline toExecute, Path sourcePath, 
                                  Vector packages, Vector excludePackages) {
        log("Source path = " + sourcePath.toString(), Project.MSG_VERBOSE);
        StringBuffer msg = new StringBuffer("Packages = ");
        for (int i=0; i<packages.size(); i++) {
            if (i > 0) {
                msg.append(",");
            }
            msg.append(packages.elementAt(i));
        }
        log(msg.toString(), Project.MSG_VERBOSE);

        msg.setLength(0);
        msg.append("Exclude Packages = ");
        for (int i=0; i<excludePackages.size(); i++) {
            if (i > 0) {
                msg.append(",");
            }
            msg.append(excludePackages.elementAt(i));
        }
        log(msg.toString(), Project.MSG_VERBOSE);

        Vector addedPackages = new Vector();

        String[] list = sourcePath.list();
        if (list == null) list = new String[0];

        FileSet fs = new FileSet();
        fs.setDefaultexcludes(useDefaultExcludes);

        Enumeration e = packages.elements();
        while (e.hasMoreElements()) {
            String pkg = (String)e.nextElement();
            pkg = pkg.replace('.','/');
            if (pkg.endsWith("*")) {
                pkg += "*";
            }

            fs.createInclude().setName(pkg);
        } // while

        e = excludePackages.elements();
        while (e.hasMoreElements()) {
            String pkg = (String)e.nextElement();
            pkg = pkg.replace('.','/');
            if (pkg.endsWith("*")) {
                pkg += "*";
            }
            
            fs.createExclude().setName(pkg);
        }
        
        PrintWriter packageListWriter = null;
        try {
            if (useExternalFile) {
                tmpList = createTempFile();
                toExecute.createArgument().setValue("@" + tmpList.getAbsolutePath());
                packageListWriter = new PrintWriter(new FileWriter(tmpList));
            }


            for (int j=0; j<list.length; j++) {
                File source = project.resolveFile(list[j]);
                fs.setDir(source);
                
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                String[] packageDirs = ds.getIncludedDirectories();
                
                for (int i=0; i<packageDirs.length; i++) {
                    File pd = new File(source, packageDirs[i]);
                    String[] files = pd.list(new FilenameFilter () {
                            public boolean accept(File dir1, String name) {
                                if (name.endsWith(".java")) {
                                    return true;
                                }
                                return false;	// ignore dirs
                            }
                        });
                    
                    if (files.length > 0) {
                        String pkgDir = packageDirs[i].replace('/','.').replace('\\','.');
                        if (!addedPackages.contains(pkgDir)) {
                            if (useExternalFile) {
                                packageListWriter.println(pkgDir);
                            } else {
                                toExecute.createArgument().setValue(pkgDir);
                            }
                            addedPackages.addElement(pkgDir);
                        }
                    }
                }
            }
        } catch (IOException ioex) {
            throw new BuildException("Error creating temporary file", 
                                     ioex, location);
        } finally {
            if (packageListWriter != null) {
                packageListWriter.close();
            }
        }
    }

    private class JavadocOutputStream extends LogOutputStream {
        JavadocOutputStream(int level) {
            super(Javadoc.this, level);
        }

        //
        // Override the logging of output in order to filter out Generating
        // messages.  Generating messages are set to a priority of VERBOSE
        // unless they appear after what could be an informational message.
        //
        private String queuedLine = null;
        protected void processLine(String line, int messageLevel) {
            if (messageLevel == Project.MSG_INFO && line.startsWith("Generating ")) {
                if (queuedLine != null) {
                    super.processLine(queuedLine, Project.MSG_VERBOSE);
                }
                queuedLine = line;
            } else {
                if (queuedLine != null) {
                    if (line.startsWith("Building "))
                        super.processLine(queuedLine, Project.MSG_VERBOSE);
                    else
                        super.processLine(queuedLine, Project.MSG_INFO);
                    queuedLine = null;
                }
                super.processLine(line, messageLevel);
            }
        }

        
        protected void logFlush() {
            if (queuedLine != null) {
                super.processLine(queuedLine, Project.MSG_VERBOSE);
                queuedLine = null;
            }
        }
    }

    /**
     * Convenience method to expand properties.
     */
    protected String expand(String content) {
        return ProjectHelper.replaceProperties(project, content, 
                                               project.getProperties());
    }

    /**
     * Creates a temporary file.
     */
    private File createTempFile() {
        return new File("javadoc" + (new Random(System.currentTimeMillis())).nextLong());
    }

    private String getJavadocExecutableName()
    {
	// This is the most common extension case - exe for windows and OS/2, 
        // nothing for *nix.
        String os = System.getProperty("os.name").toLowerCase();
        boolean dosBased = 
            os.indexOf("windows") >= 0 || os.indexOf("os/2") >= 0;
	String extension =  dosBased? ".exe" : "";

	// Look for javadoc in the java.home/../bin directory.  Unfortunately
	// on Windows java.home doesn't always refer to the correct location, 
	// so we need to fall back to assuming javadoc is somewhere on the
	// PATH.
	File jdocExecutable = new File( System.getProperty("java.home") +
		"/../bin/javadoc" + extension );

	if (jdocExecutable.exists())
	{
	    return jdocExecutable.getAbsolutePath();
	}
	else
	{
	    log( "Unable to locate " + jdocExecutable.getAbsolutePath() +
		    ". Using \"javadoc\" instead.", Project.MSG_VERBOSE );
	    return "javadoc";
	}
    }
    
}
