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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;

import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * JavaCC compiler compiler task.
 *
 * @author thomas.haas@softwired-inc.com
 * @author Michael Saunders <a href="mailto:michael@amtec.com">michael@amtec.com</a>
 */
public class JavaCC extends Task {

    // keys to optional attributes
    private static final String LOOKAHEAD              = "LOOKAHEAD";
    private static final String CHOICE_AMBIGUITY_CHECK = "CHOICE_AMBIGUITY_CHECK";
    private static final String OTHER_AMBIGUITY_CHECK  = "OTHER_AMBIGUITY_CHECK";

    private static final String STATIC                 = "STATIC";
    private static final String DEBUG_PARSER           = "DEBUG_PARSER";
    private static final String DEBUG_LOOKAHEAD        = "DEBUG_LOOKAHEAD";
    private static final String DEBUG_TOKEN_MANAGER    = "DEBUG_TOKEN_MANAGER";
    private static final String OPTIMIZE_TOKEN_MANAGER = "OPTIMIZE_TOKEN_MANAGER";
    private static final String ERROR_REPORTING        = "ERROR_REPORTING";
    private static final String JAVA_UNICODE_ESCAPE    = "JAVA_UNICODE_ESCAPE";
    private static final String UNICODE_INPUT          = "UNICODE_INPUT";
    private static final String IGNORE_CASE            = "IGNORE_CASE";
    private static final String COMMON_TOKEN_ACTION    = "COMMON_TOKEN_ACTION";
    private static final String USER_TOKEN_MANAGER     = "USER_TOKEN_MANAGER";
    private static final String USER_CHAR_STREAM       = "USER_CHAR_STREAM";
    private static final String BUILD_PARSER           = "BUILD_PARSER";
    private static final String BUILD_TOKEN_MANAGER    = "BUILD_TOKEN_MANAGER";
    private static final String SANITY_CHECK           = "SANITY_CHECK";
    private static final String FORCE_LA_CHECK         = "FORCE_LA_CHECK";
    private static final String CACHE_TOKENS           = "CACHE_TOKENS";

    private final Hashtable optionalAttrs = new Hashtable();

    // required attributes
    private File outputDirectory = null;
    private File target          = null;
    private File javaccHome      = null;

    private CommandlineJava cmdl = new CommandlineJava();


    /**
     * Sets the LOOKAHEAD grammar option.
     */
    public void setLookahead(int lookahead) {
        optionalAttrs.put(LOOKAHEAD, new Integer(lookahead));
    }

    /**
     * Sets the CHOICE_AMBIGUITY_CHECK grammar option.
     */
    public void setChoiceambiguitycheck(int choiceAmbiguityCheck) {
        optionalAttrs.put(CHOICE_AMBIGUITY_CHECK, new Integer(choiceAmbiguityCheck));
    }

    /**
     * Sets the OTHER_AMBIGUITY_CHECK grammar option.
     */
    public void setOtherambiguityCheck(int otherAmbiguityCheck) {
        optionalAttrs.put(OTHER_AMBIGUITY_CHECK, new Integer(otherAmbiguityCheck));
    }

    /**
     * Sets the STATIC grammar option.
     */
    public void setStatic(boolean staticParser) {
        optionalAttrs.put(STATIC, new Boolean(staticParser));
    }

    /**
     * Sets the DEBUG_PARSER grammar option.
     */
    public void setDebugparser(boolean debugParser) {
        optionalAttrs.put(DEBUG_PARSER, new Boolean(debugParser));
    }

    /**
     * Sets the DEBUG_LOOKAHEAD grammar option.
     */
    public void setDebuglookahead(boolean debugLookahead) {
        optionalAttrs.put(DEBUG_LOOKAHEAD, new Boolean(debugLookahead));
    }

    /**
     * Sets the DEBUG_TOKEN_MANAGER grammar option.
     */
    public void setDebugtokenmanager(boolean debugTokenManager) {
        optionalAttrs.put(DEBUG_TOKEN_MANAGER, new Boolean(debugTokenManager));
    }

    /**
     * Sets the OPTIMIZE_TOKEN_MANAGER grammar option.
     */
    public void setOptimizetokenmanager(boolean optimizeTokenManager) {
        optionalAttrs.put(OPTIMIZE_TOKEN_MANAGER, new Boolean(optimizeTokenManager));
    }

    /**
     * Sets the ERROR_REPORTING grammar option.
     */
    public void setErrorreporting(boolean errorReporting) {
        optionalAttrs.put(ERROR_REPORTING, new Boolean(errorReporting));
    }

    /**
     * Sets the JAVA_UNICODE_ESCAPE grammar option.
     */
    public void setJavaunicodeescape(boolean javaUnicodeEscape) {
        optionalAttrs.put(JAVA_UNICODE_ESCAPE, new Boolean(javaUnicodeEscape));
    }

    /**
     * Sets the UNICODE_INPUT grammar option.
     */
    public void setUnicodeinput(boolean unicodeInput) {
        optionalAttrs.put(UNICODE_INPUT, new Boolean(unicodeInput));
    }

    /**
     * Sets the IGNORE_CASE grammar option.
     */
    public void setIgnorecase(boolean ignoreCase) {
        optionalAttrs.put(IGNORE_CASE, new Boolean(ignoreCase));
    }

    /**
     * Sets the COMMON_TOKEN_ACTION grammar option.
     */
    public void setCommontokenaction(boolean commonTokenAction) {
        optionalAttrs.put(COMMON_TOKEN_ACTION, new Boolean(commonTokenAction));
    }

    /**
     * Sets the USER_TOKEN_MANAGER grammar option.
     */
    public void setUsertokenmanager(boolean userTokenManager) {
        optionalAttrs.put(USER_TOKEN_MANAGER, new Boolean(userTokenManager));
    }

    /**
     * Sets the USER_CHAR_STREAM grammar option.
     */
    public void setUsercharstream(boolean userCharStream) {
        optionalAttrs.put(USER_CHAR_STREAM, new Boolean(userCharStream));
    }

    /**
     * Sets the BUILD_PARSER grammar option.
     */
    public void setBuildparser(boolean buildParser) {
        optionalAttrs.put(BUILD_PARSER, new Boolean(buildParser));
    }

    /**
     * Sets the BUILD_TOKEN_MANAGER grammar option.
     */
    public void setBuildtokenmanager(boolean buildTokenManager) {
        optionalAttrs.put(BUILD_TOKEN_MANAGER, new Boolean(buildTokenManager));
    }

    /**
     * Sets the SANITY_CHECK grammar option.
     */
    public void setSanitycheck(boolean sanityCheck) {
        optionalAttrs.put(SANITY_CHECK, new Boolean(sanityCheck));
    }

    /**
     * Sets the FORCE_LA_CHECK grammar option.
     */
    public void setForcelacheck(boolean forceLACheck) {
        optionalAttrs.put(FORCE_LA_CHECK, new Boolean(forceLACheck));
    }

    /**
     * Sets the CACHE_TOKENS grammar option.
     */
    public void setCachetokens(boolean cacheTokens) {
        optionalAttrs.put(CACHE_TOKENS, new Boolean(cacheTokens));
    }

    /**
     * The directory to write the generated files to.
     * If not set, the files are written to the directory
     * containing the grammar file.
     */
    public void setOutputdirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * The grammar file to process.
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

    public JavaCC() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
        cmdl.setClassname("COM.sun.labs.javacc.Main");
    }

    public void execute() throws BuildException {

        // load command line with optional attributes
        Enumeration iter = optionalAttrs.keys();
        while (iter.hasMoreElements()) {
            String name  = (String) iter.nextElement();
            Object value = optionalAttrs.get(name);
            cmdl.createArgument().setValue("-" + name + ":" + value.toString());
        }

        // check the target is a file
        if (target == null || !target.isFile()) {
            throw new BuildException("Invalid target: " + target);
        }

        // use the directory containing the target as the output directory
        if (outputDirectory == null) {
            outputDirectory = new File(target.getParent());
        } else if (!outputDirectory.isDirectory()) {
            throw new BuildException("Outputdir not a directory.");
        }
        cmdl.createArgument().setValue("-OUTPUT_DIRECTORY:"
            + outputDirectory.getAbsolutePath());

        // determine if the generated java file is up-to-date
        final File javaFile = getOutputJavaFile(outputDirectory, target);
        if (javaFile.exists() && target.lastModified() < javaFile.lastModified()) {
            log("Target is already built - skipping (" + target + ")", Project.MSG_VERBOSE);
            return;
        }
        cmdl.createArgument().setValue(target.getAbsolutePath());

        final Path classpath = cmdl.createClasspath(project);
        final File javaccJar = JavaCC.getArchiveFile(javaccHome);
        classpath.createPathElement().setPath(javaccJar.getAbsolutePath());
        classpath.addJavaRuntime();

        final Commandline.Argument arg = cmdl.createVmArgument();
        arg.setValue("-mx140M");
        arg.setValue("-Dinstall.root=" + javaccHome.getAbsolutePath());

        Execute.runCommand(this, cmdl.getCommandline());
    }

    /**
     * Helper class to retrieve the path used to store the JavaCC.zip which is
     * different from versions.
     * @param home the javacc home path directory.
     * @throws BuildException thrown if the home directory is invalid or if the archive
     * could not be found despite attemps to do so.
     * @return the file object pointing to the JavaCC archive.
     */
    protected static File getArchiveFile(File home) throws BuildException {
        if (home == null || !home.isDirectory()) {
            throw new BuildException("JavaCC home must be a valid directory.");
        }
        // javacc prior to 2.0
        File f = new File(home, "JavaCC.zip");
        if (f.exists()){
          return f;
        }
        // javacc install 2.0+
        f = new File(home, "bin/lib/JavaCC.zip");
        if (f.exists()){
          return f;
        }
        throw new BuildException("Could not find a path to JavaCC.zip from '" + home + "'.");
    }

    /**
     * Determines the output Java file to be generated by the given grammar
     * file.
     * 
     */
    private File getOutputJavaFile(File outputdir, File srcfile) {
        String path = srcfile.getPath();

        // Extract file's base-name
        int startBasename = path.lastIndexOf(File.separator);
        if (startBasename != -1) {
            path = path.substring(startBasename + 1);
        }

        // Replace the file's extension with '.java'
        int startExtn = path.lastIndexOf('.');
        if (startExtn != -1) {
            path = path.substring(0, startExtn) + ".java";
        } else {
            path += ".java";
        }

        // Change the directory
        if (outputdir != null) {
            path = outputdir + File.separator + path;
        }

        return new File(path);
    }
}
