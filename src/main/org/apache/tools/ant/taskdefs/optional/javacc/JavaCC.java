/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.javacc;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipFile;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * JavaCC compiler compiler task.
 *
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
    private static final String KEEP_LINE_COLUMN       = "KEEP_LINE_COLUMN";

    private final Hashtable optionalAttrs = new Hashtable();

    // required attributes
    private File outputDirectory = null;
    private File targetFile      = null;
    private File javaccHome      = null;

    private CommandlineJava cmdl = new CommandlineJava();

    protected static final int TASKDEF_TYPE_JAVACC = 1;
    protected static final int TASKDEF_TYPE_JJTREE = 2;
    protected static final int TASKDEF_TYPE_JJDOC = 3;

    protected static final String[] ARCHIVE_LOCATIONS =
        new String[] {
            "JavaCC.zip",
            "bin/lib/JavaCC.zip",
            "bin/lib/javacc.jar",
            "javacc.jar", // used by jpackage for JavaCC 3.x
        };

    protected static final int[] ARCHIVE_LOCATIONS_VS_MAJOR_VERSION =
        new int[] {
            1,
            2,
            3,
            3,
        };

    protected static final String COM_PACKAGE = "COM.sun.labs.";
    protected static final String COM_JAVACC_CLASS = "javacc.Main";
    protected static final String COM_JJTREE_CLASS = "jjtree.Main";
    protected static final String COM_JJDOC_CLASS = "jjdoc.JJDocMain";

    protected static final String ORG_PACKAGE_3_0 = "org.netbeans.javacc.";
    protected static final String ORG_PACKAGE_3_1 = "org.javacc.";
    protected static final String ORG_JAVACC_CLASS = "parser.Main";
    protected static final String ORG_JJTREE_CLASS = COM_JJTREE_CLASS;
    protected static final String ORG_JJDOC_CLASS = COM_JJDOC_CLASS;

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
     * Sets the KEEP_LINE_COLUMN grammar option.
     */
    public void setKeeplinecolumn(boolean keepLineColumn) {
        optionalAttrs.put(KEEP_LINE_COLUMN, new Boolean(keepLineColumn));
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
    public void setTarget(File targetFile) {
        this.targetFile = targetFile;
    }

    /**
     * The directory containing the JavaCC distribution.
     */
    public void setJavacchome(File javaccHome) {
        this.javaccHome = javaccHome;
    }

    public JavaCC() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
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
        if (targetFile == null || !targetFile.isFile()) {
            throw new BuildException("Invalid target: " + targetFile);
        }

        // use the directory containing the target as the output directory
        if (outputDirectory == null) {
            outputDirectory = new File(targetFile.getParent());
        } else if (!outputDirectory.isDirectory()) {
            throw new BuildException("Outputdir not a directory.");
        }
        cmdl.createArgument().setValue("-OUTPUT_DIRECTORY:"
                                       + outputDirectory.getAbsolutePath());

        // determine if the generated java file is up-to-date
        final File javaFile = getOutputJavaFile(outputDirectory, targetFile);
        if (javaFile.exists() && targetFile.lastModified() < javaFile.lastModified()) {
            log("Target is already built - skipping (" + targetFile + ")", Project.MSG_VERBOSE);
            return;
        }
        cmdl.createArgument().setValue(targetFile.getAbsolutePath());

        final Path classpath = cmdl.createClasspath(getProject());
        final File javaccJar = JavaCC.getArchiveFile(javaccHome);
        classpath.createPathElement().setPath(javaccJar.getAbsolutePath());
        classpath.addJavaRuntime();

        cmdl.setClassname(JavaCC.getMainClass(classpath,
                                              JavaCC.TASKDEF_TYPE_JAVACC));

        final Commandline.Argument arg = cmdl.createVmArgument();
        arg.setValue("-mx140M");
        arg.setValue("-Dinstall.root=" + javaccHome.getAbsolutePath());

        Execute.runCommand(this, cmdl.getCommandline());
    }

    /**
     * Helper method to retrieve the path used to store the JavaCC.zip
     * or javacc.jar which is different from versions.
     *
     * @param home the javacc home path directory.
     * @throws BuildException thrown if the home directory is invalid
     * or if the archive could not be found despite attempts to do so.
     * @return the file object pointing to the JavaCC archive.
     */
    protected static File getArchiveFile(File home) throws BuildException {
        return new File(home,
                        ARCHIVE_LOCATIONS[getArchiveLocationIndex(home)]);
    }

    /**
     * Helper method to retrieve main class which is different from versions.
     * @param home the javacc home path directory.
     * @param type the taskdef.
     * @throws BuildException thrown if the home directory is invalid
     * or if the archive could not be found despite attempts to do so.
     * @return the main class for the taskdef.
     */
    protected static String getMainClass(File home, int type)
        throws BuildException {

        Path p = new Path(null);
        p.createPathElement().setLocation(getArchiveFile(home));
        p.addJavaRuntime();
        return getMainClass(p, type);
    }

    /**
     * Helper method to retrieve main class which is different from versions.
     * @param path classpath to search in.
     * @param type the taskdef.
     * @throws BuildException thrown if the home directory is invalid
     * or if the archive could not be found despite attempts to do so.
     * @return the main class for the taskdef.
     * @since Ant 1.7
     */
    protected static String getMainClass(Path path, int type)
        throws BuildException {
        String packagePrefix = null;
        String mainClass = null;

        AntClassLoader l = new AntClassLoader();
        l.setClassPath(path.concatSystemClasspath("ignore"));
        String javaccClass = COM_PACKAGE + COM_JAVACC_CLASS;
        InputStream is = l.getResourceAsStream(javaccClass.replace('.', '/')
                                               + ".class");
        if (is != null) {
            packagePrefix = COM_PACKAGE;
            switch (type) {
            case TASKDEF_TYPE_JAVACC:
                mainClass = COM_JAVACC_CLASS;

                break;

            case TASKDEF_TYPE_JJTREE:
                mainClass = COM_JJTREE_CLASS;

                break;

            case TASKDEF_TYPE_JJDOC:
                mainClass = COM_JJDOC_CLASS;

                break;
            }
        } else {
            javaccClass = ORG_PACKAGE_3_1 + ORG_JAVACC_CLASS;
            is = l.getResourceAsStream(javaccClass.replace('.', '/')
                                       + ".class");
            if (is != null) {
                packagePrefix = ORG_PACKAGE_3_1;
            } else {
                javaccClass = ORG_PACKAGE_3_0 + ORG_JAVACC_CLASS;
                is = l.getResourceAsStream(javaccClass.replace('.', '/')
                                           + ".class");
                if (is != null) {
                    packagePrefix = ORG_PACKAGE_3_0;
                }
            }

            if (is != null) {
                switch (type) {
                case TASKDEF_TYPE_JAVACC:
                    mainClass = ORG_JAVACC_CLASS;
                    
                break;
                
                case TASKDEF_TYPE_JJTREE:
                    mainClass = ORG_JJTREE_CLASS;
                    
                    break;

                case TASKDEF_TYPE_JJDOC:
                    mainClass = ORG_JJDOC_CLASS;
                    
                    break;
                }
            }
        }

        if (packagePrefix == null) {
            throw new BuildException("failed to load JavaCC");
        }
        if (mainClass == null) {
            throw new BuildException("unknown task type " + type);
        }
        return packagePrefix + mainClass;
    }

    /**
     * Helper method to determine the archive location index.
     *
     * @param home the javacc home path directory.
     * @throws BuildException thrown if the home directory is invalid
     * or if the archive could not be found despite attempts to do so.
     * @return the archive location index
     */
    private static int getArchiveLocationIndex(File home)
        throws BuildException {

        if (home == null || !home.isDirectory()) {
            throw new BuildException("JavaCC home must be a valid directory.");
        }

        for (int i = 0; i < ARCHIVE_LOCATIONS.length; i++) {
            File f = new File(home, ARCHIVE_LOCATIONS[i]);

            if (f.exists()) {
                return i;
            }
        }

        throw new BuildException("Could not find a path to JavaCC.zip "
                                 + "or javacc.jar from '" + home + "'.");
    }

    /**
     * Helper method to determine the major version number of JavaCC.
     *
     * @param home the javacc home path directory.
     * @throws BuildException thrown if the home directory is invalid
     * or if the archive could not be found despite attempts to do so.
     * @return a the major version number
     */
    protected static int getMajorVersionNumber(File home)
        throws BuildException {

        return
            ARCHIVE_LOCATIONS_VS_MAJOR_VERSION[getArchiveLocationIndex(home)];
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
