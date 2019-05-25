/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.LineContainsRegExp;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.RedirectorElement;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * This is factored out from {@link SignJar}; a base class that can be used
 * for both signing and verifying JAR files using jarsigner
 */

public abstract class AbstractJarSignerTask extends Task {
    /**
     * error string for unit test verification: {@value}
     */
    public static final String ERROR_NO_SOURCE =
        "jar must be set through jar attribute or nested filesets";

    /**
     * name of JDK program we are looking for
     */
    protected static final String JARSIGNER_COMMAND = "jarsigner";

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * The name of the jar file.
     */
    protected File jar;
    /**
     * The alias of signer.
     */
    protected String alias;
    /**
     * The url or path of keystore file.
     */
    protected String keystore;
    /**
     * password for the store
     */
    protected String storepass;
    /**
     * type of store,-storetype param
     */
    protected String storetype;
    /**
     * password for the key in the store
     */
    protected String keypass;
    /**
     * verbose output
     */
    protected boolean verbose;
    /**
     * strict checking
     * @since Ant 1.9.1
     */
    protected boolean strict = false;
    /**
     * The maximum amount of memory to use for Jar signer
     */
    protected String maxMemory;
    /**
     * the filesets of the jars to sign
     */
    protected Vector<FileSet> filesets = new Vector<>();
    // CheckStyle:VisibilityModifier ON

    /**
     * redirector used to talk to the jarsigner program
     */
    private RedirectorElement redirector;

    /**
     * Java declarations -J-Dname=value
     */
    private Environment sysProperties = new Environment();

    /**
     * Path holding all non-filesets of filesystem resources we want to sign.
     *
     * @since Ant 1.7
     */
    private Path path = null;

    /**
     * The executable to use instead of jarsigner.
     *
     * @since Ant 1.8.0
     */
    private String executable;

    /**
     * Values for the providerName, providerClass, and providerArg options.
     *
     * @since Ant 1.10.6
     */
    private String providerName, providerClass, providerArg;

    private List<Commandline.Argument> additionalArgs = new ArrayList<>();

    /**
     * Set the maximum memory to be used by the jarsigner process
     *
     * @param max a string indicating the maximum memory according to the JVM
     *            conventions (e.g. 128m is 128 Megabytes)
     */
    public void setMaxmemory(String max) {
        maxMemory = max;
    }

    /**
     * the jar file to sign; required
     *
     * @param jar the jar file to sign
     */
    public void setJar(final File jar) {
        this.jar = jar;
    }

    /**
     * the alias to sign under; required
     *
     * @param alias the alias to sign under
     */
    public void setAlias(final String alias) {
        this.alias = alias;
    }

    /**
     * keystore location; required
     *
     * @param keystore the keystore location
     */
    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    /**
     * password for keystore integrity; required
     *
     * @param storepass the password for the keystore
     */
    public void setStorepass(final String storepass) {
        this.storepass = storepass;
    }

    /**
     * keystore type; optional
     *
     * @param storetype the keystore type
     */
    public void setStoretype(final String storetype) {
        this.storetype = storetype;
    }

    /**
     * password for private key (if different); optional
     *
     * @param keypass the password for the key (if different)
     */
    public void setKeypass(final String keypass) {
        this.keypass = keypass;
    }

    /**
     * Enable verbose output when signing; optional: default false
     *
     * @param verbose if true enable verbose output
     */
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * do strict checking
     * @since Ant 1.9.1
     * @param strict boolean
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * Adds a set of files to sign
     *
     * @param set a set of files to sign
     * @since Ant 1.4
     */
    public void addFileset(final FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Add a system property.
     *
     * @param sysp system property.
     */
    public void addSysproperty(Environment.Variable sysp) {
        sysProperties.addVariable(sysp);
    }

    /**
     * Adds a path of files to sign.
     *
     * @return a path of files to sign.
     * @since Ant 1.7
     */
    public Path createPath() {
        if (path == null) {
            path = new Path(getProject());
        }
        return path.createPath();
    }

    /**
     * Sets the value for the -providerName command line argument.
     *
     * @param providerName the value for the -providerName command line argument
     *
     * @since Ant 1.10.6
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * Sets the value for the -providerClass command line argument.
     *
     * @param providerClass the value for the -providerClass command line argument
     *
     * @since Ant 1.10.6
     */
    public void setProviderClass(String providerClass) {
        this.providerClass = providerClass;
    }

    /**
     * Sets the value for the -providerArg command line argument.
     *
     * @param providerArg the value for the -providerArg command line argument
     *
     * @since Ant 1.10.6
     */
    public void setProviderArg(String providerArg) {
        this.providerArg = providerArg;
    }

    /**
     * Adds a nested &lt;arg&gt; element that can be used to specify
     * command line arguments not supported via specific attributes.
     *
     * @param arg the argument to add
     *
     * @since Ant 1.10.6
     */
    public void addArg(Commandline.Argument arg) {
        additionalArgs.add(arg);
    }

    /**
     * init processing logic; this is retained through our execution(s)
     */
    protected void beginExecution() {

        redirector = createRedirector();
    }

    /**
     * any cleanup logic
     */
    protected void endExecution() {
        redirector = null;
    }

    /**
     * Create the redirector to use, if any.
     *
     * @return a configured RedirectorElement.
     */
    private RedirectorElement createRedirector() {
        RedirectorElement result = new RedirectorElement();
        if (storepass != null) {
            StringBuilder input = new StringBuilder(storepass).append('\n');
            if (keypass != null) {
                input.append(keypass).append('\n');
            }
            result.setInputString(input.toString());
            result.setLogInputString(false);
            // Try to avoid showing password prompts on log output, as they would be confusing.
            LineContainsRegExp filter = new LineContainsRegExp();
            RegularExpression rx = new RegularExpression();
            // TODO only handles English locale, not ja or zh_CN
            rx.setPattern("^(Enter Passphrase for keystore: |Enter key password for .+: )$");
            filter.addConfiguredRegexp(rx);
            filter.setNegate(true);
            result.createErrorFilterChain().addLineContainsRegExp(filter);
        }
        return result;
    }

    /**
     * get the redirector. Non-null between invocations of
     * {@link #beginExecution()} and {@link #endExecution()}
     * @return a redirector or null
     */
    public RedirectorElement getRedirector() {
        return redirector;
    }

    /**
     * Sets the actual executable command to invoke, instead of the binary
     * <code>jarsigner</code> found in Ant's JDK.
     * @param executable the command to invoke.
     * @since Ant 1.8.0
     */
    public void setExecutable(String executable) {
        this.executable = executable;
    }

    /**
     * these are options common to signing and verifying
     * @param cmd  command to configure
     */
    protected void setCommonOptions(final ExecTask cmd) {
        if (maxMemory != null) {
            addValue(cmd, "-J-Xmx" + maxMemory);
        }

        if (verbose) {
            addValue(cmd, "-verbose");
        }

        if (strict) {
            addValue(cmd, "-strict");
        }

        //now patch in all system properties
        for (Environment.Variable variable : sysProperties.getVariablesVector()) {
            declareSysProperty(cmd, variable);
        }

        for (Commandline.Argument arg : additionalArgs) {
            addArgument(cmd, arg);
        }
    }

    /**
     *
     * @param cmd command to configure
     * @param property property to set
     * @throws BuildException if the property is not correctly defined.
     */
    protected void declareSysProperty(
        ExecTask cmd, Environment.Variable property) throws BuildException {
        addValue(cmd, "-J-D" + property.getContent());
    }

    /**
     * bind to a keystore if the attributes are there
     * @param cmd command to configure
     */
    protected void bindToKeystore(final ExecTask cmd) {
        if (null != keystore) {
            // is the keystore a file
            addValue(cmd, "-keystore");
            String loc;
            File keystoreFile = getProject().resolveFile(keystore);
            if (keystoreFile.exists()) {
                loc = keystoreFile.getPath();
            } else {
                // must be a URL - just pass as is
                loc = keystore;
            }
            addValue(cmd, loc);
        }
        if (null != storetype) {
            addValue(cmd, "-storetype");
            addValue(cmd, storetype);
        }
        if (null != providerName) {
            addValue(cmd, "-providerName");
            addValue(cmd, providerName);
        }
        if (null != providerClass) {
            addValue(cmd, "-providerClass");
            addValue(cmd, providerClass);
            if (null != providerArg) {
                addValue(cmd, "-providerArg");
                addValue(cmd, providerArg);
            }
        } else if (null != providerArg) {
            log("Ignoring providerArg as providerClass has not been set");
        }
    }

    /**
     * create the jarsigner executable task
     * @return a task set up with the executable of jarsigner, failonerror=true
     * and bound to our redirector
     */
    protected ExecTask createJarSigner() {
        final ExecTask cmd = new ExecTask(this);
        if (executable == null) {
            cmd.setExecutable(JavaEnvUtils.getJdkExecutable(JARSIGNER_COMMAND));
        } else {
            cmd.setExecutable(executable);
        }
        cmd.setTaskType(JARSIGNER_COMMAND);
        cmd.setFailonerror(true);
        cmd.addConfiguredRedirector(redirector);
        return cmd;
    }

    /**
     * clone our filesets vector, and patch in the jar attribute as a new
     * fileset, if is defined
     * @return a vector of FileSet instances
     */
    protected Vector<FileSet> createUnifiedSources() {
        Vector<FileSet> sources = new Vector<>(filesets);
        if (jar != null) {
            //we create a fileset with the source file.
            //this lets us combine our logic for handling output directories,
            //mapping etc.
            FileSet sourceJar = new FileSet();
            sourceJar.setProject(getProject());
            sourceJar.setFile(jar);
            sources.add(sourceJar);
        }
        return sources;
    }

    /**
     * clone our path and add all explicitly specified FileSets as
     * well, patch in the jar attribute as a new fileset if it is
     * defined.
     * @return a path that contains all files to sign
     * @since Ant 1.7
     */
    protected Path createUnifiedSourcePath() {
        Path p = path == null ? new Path(getProject()) : (Path) path.clone();
        for (FileSet fileSet : createUnifiedSources()) {
            p.add(fileSet);
        }
        return p;
    }

    /**
     * Has either a path or a fileset been specified?
     * @return true if a path or fileset has been specified.
     * @since Ant 1.7
     */
    protected boolean hasResources() {
        return !(path == null && filesets.isEmpty());
    }

    /**
     * add a value argument to a command
     * @param cmd command to manipulate
     * @param value value to add
     */
    protected void addValue(final ExecTask cmd, String value) {
        cmd.createArg().setValue(value);
    }

    /**
     * add an argument to a command
     * @param cmd command to manipulate
     * @param arg argument to add
     */
    protected void addArgument(final ExecTask cmd, Commandline.Argument arg) {
        cmd.createArg().copyFrom(arg);
    }
}
