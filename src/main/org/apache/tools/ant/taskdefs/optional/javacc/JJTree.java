/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.javacc;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Runs the JJTree compiler compiler.
 *
 * @author thomas.haas@softwired-inc.com
 * @author Michael Saunders <a href="mailto:michael@amtec.com">michael@amtec.com</a>
 */
public class JJTree extends Task {

    // keys to optional attributes
    private static final String BUILD_NODE_FILES  = "BUILD_NODE_FILES";
    private static final String MULTI             = "MULTI";
    private static final String NODE_DEFAULT_VOID = "NODE_DEFAULT_VOID";
    private static final String NODE_FACTORY      = "NODE_FACTORY";
    private static final String NODE_SCOPE_HOOK   = "NODE_SCOPE_HOOK";
    private static final String NODE_USES_PARSER  = "NODE_USES_PARSER";
    private static final String STATIC            = "STATIC";
    private static final String VISITOR           = "VISITOR";

    private static final String NODE_PACKAGE      = "NODE_PACKAGE";
    private static final String VISITOR_EXCEPTION = "VISITOR_EXCEPTION";
    private static final String NODE_PREFIX       = "NODE_PREFIX";

    private final Hashtable optionalAttrs = new Hashtable();

    // required attributes
    private File outputDirectory = null;
    private File target          = null;
    private File javaccHome      = null;

    private CommandlineJava cmdl = new CommandlineJava();


    /**
     * Sets the BUILD_NODE_FILES grammar option.
     */
    public void setBuildnodefiles(boolean buildNodeFiles) {
        optionalAttrs.put(BUILD_NODE_FILES, new Boolean(buildNodeFiles));
    }

    /**
     * Sets the MULTI grammar option.
     */
    public void setMulti(boolean multi) {
        optionalAttrs.put(MULTI, new Boolean(multi));
    }

    /**
     * Sets the NODE_DEFAULT_VOID grammar option.
     */
    public void setNodedefaultvoid(boolean nodeDefaultVoid) {
        optionalAttrs.put(NODE_DEFAULT_VOID, new Boolean(nodeDefaultVoid));
    }

    /**
     * Sets the NODE_FACTORY grammar option.
     */
    public void setNodefactory(boolean nodeFactory) {
        optionalAttrs.put(NODE_FACTORY, new Boolean(nodeFactory));
    }

    /**
     * Sets the NODE_SCOPE_HOOK grammar option.
     */
    public void setNodescopehook(boolean nodeScopeHook) {
        optionalAttrs.put(NODE_SCOPE_HOOK, new Boolean(nodeScopeHook));
    }

    /**
     * Sets the NODE_USES_PARSER grammar option.
     */
    public void setNodeusesparser(boolean nodeUsesParser) {
        optionalAttrs.put(NODE_USES_PARSER, new Boolean(nodeUsesParser));
    }

    /**
     * Sets the STATIC grammar option.
     */
    public void setStatic(boolean staticParser) {
        optionalAttrs.put(STATIC, new Boolean(staticParser));
    }

    /**
     * Sets the VISITOR grammar option.
     */
    public void setVisitor(boolean visitor) {
        optionalAttrs.put(VISITOR, new Boolean(visitor));
    }

    /**
     * Sets the NODE_PACKAGE grammar option.
     */
    public void setNodepackage(String nodePackage) {
        optionalAttrs.put(NODE_PACKAGE, new String(nodePackage));
    }

    /**
     * Sets the VISITOR_EXCEPTION grammar option.
     */
    public void setVisitorException(String visitorException) {
        optionalAttrs.put(VISITOR_EXCEPTION, new String(visitorException));
    }

    /**
     * Sets the NODE_PREFIX grammar option.
     */
    public void setNodeprefix(String nodePrefix) {
        optionalAttrs.put(NODE_PREFIX, new String(nodePrefix));
    }

    /**
     * The directory to write the generated file to.
     * If not set, the files are written to the directory
     * containing the grammar file.
     */
    public void setOutputdirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * The jjtree grammar file to process.
     */
    public void setTarget(File target) {
        this.target = target;
    }

    /**
     * The directory containing the JavaCC distribution.
     */
    public void setJavacchome(File javaccHome) {
        this.javaccHome = javaccHome;
    }

    public JJTree() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
        cmdl.setClassname("COM.sun.labs.jjtree.Main");
    }

    public void execute() throws BuildException {

        // load command line with optional attributes
        Enumeration iter = optionalAttrs.keys();
        while (iter.hasMoreElements()) {
            String name  = (String) iter.nextElement();
            Object value = optionalAttrs.get(name);
            cmdl.createArgument().setValue("-" + name + ":" + value.toString());
        }

        if (target == null || !target.isFile()) {
            throw new BuildException("Invalid target: " + target);
        }
        
        // use the directory containing the target as the output directory
        if (outputDirectory == null) {
            outputDirectory = new File(target.getParent());
        }        
        if (!outputDirectory.isDirectory()) {
            throw new BuildException("'outputdirectory' " + outputDirectory 
                + " is not a directory.");
        }
        // convert backslashes to slashes, otherwise jjtree will put this as
        // comments and this seems to confuse javacc
        cmdl.createArgument().setValue("-OUTPUT_DIRECTORY:" 
            + outputDirectory.getAbsolutePath().replace('\\', '/'));
        
        String targetName = target.getName();
        final File javaFile = new File(outputDirectory,
            targetName.substring(0, targetName.indexOf(".jjt")) + ".jj");
        if (javaFile.exists() 
             && target.lastModified() < javaFile.lastModified()) {
            log("Target is already built - skipping (" + target + ")",
                Project.MSG_VERBOSE);
            return;
        }
        cmdl.createArgument().setValue(target.getAbsolutePath());

        final Path classpath = cmdl.createClasspath(getProject());
        final File javaccJar = JavaCC.getArchiveFile(javaccHome);
        classpath.createPathElement().setPath(javaccJar.getAbsolutePath());
        classpath.addJavaRuntime();

        final Commandline.Argument arg = cmdl.createVmArgument();
        arg.setValue("-mx140M");
        arg.setValue("-Dinstall.root=" + javaccHome.getAbsolutePath());

        final Execute process =
            new Execute(new LogStreamHandler(this,
                                             Project.MSG_INFO,
                                             Project.MSG_INFO),
                        null);
        log(cmdl.describeCommand(), Project.MSG_VERBOSE);
        process.setCommandline(cmdl.getCommandline());

        try {
            if (process.execute() != 0) {
                throw new BuildException("JJTree failed.");
            }
        } catch (IOException e) {
            throw new BuildException("Failed to launch JJTree", e);
        }
    }
}
