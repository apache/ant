/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.ejb;


import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

import java.io.File;

/**
 * Starts a WebLogic server.
 * A number of parameters are used to control the operation of the weblogic instance. Note that the task,
 * and hence ant, will not complete until the weblogic instance is stopped.</p>
 *
 * @author Conor MacNeill, Cortex ebusiness Pty Limited
 */
public class WLRun extends Task {
    protected static final String DEFAULT_WL51_POLICY_FILE = "weblogic.policy";
    protected static final String DEFAULT_WL60_POLICY_FILE = "lib/weblogic.policy";
    protected static final String DEFAULT_PROPERTIES_FILE = "weblogic.properties";

    /**
     * The classpath to be used when running the Java VM. It must contain the weblogic
     * classes <b>and</b> the implementation classes of the home and remote interfaces.
     */
    private Path classpath;

    /**
     * The weblogic classpath to the be used when running weblogic.
     */
    private Path weblogicClasspath;

    private String weblogicMainClass = "weblogic.Server";

    /**
     * Addional arguments to pass to the JVM used to run weblogic
     */
    private String additionalArgs = "";

    /**
     * The security policy to use when running the weblogic server
     */
    private String securityPolicy;

    /**
     * The weblogic system home directory
     */
    private File weblogicSystemHome;

    /**
     * The weblogic domain
     */
    private String weblogicDomainName;

    /**
     * The name of the weblogic server - used to select the server's directory in the
     * weblogic home directory.
     */
    private String weblogicSystemName = "myserver";

    /**
     * The file containing the weblogic properties for this server.
     */
    private String weblogicPropertiesFile = null;

    /**
     * additional args to pass to the spawned jvm
     */
    private String additionalJvmArgs = "";

    /**
     * The location of the BEA Home under which this server is run.
     * WL6 only
     */
    private File beaHome = null;

    /**
     * The management username
     */
    private String managementUsername = "system";

    /**
     * The management password
     */
    private String managementPassword = null;

    /**
     * The provate key password - used for SSL
     */
    private String pkPassword = null;

    /**
     * Add the classpath for the user classes
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(project);
        }
        return classpath.createPath();
    }

    /**
     * Get the classpath to the weblogic classpaths
     */
    public Path createWLClasspath() {
        if (weblogicClasspath == null) {
            weblogicClasspath = new Path(project);
        }
        return weblogicClasspath.createPath();
    }

    /**
     * Do the work.
     *
     * The work is actually done by creating a separate JVM to run a helper task.
     * This approach allows the classpath of the helper task to be set. Since the
     * weblogic tools require the class files of the project's home and remote
     * interfaces to be available in the classpath, this also avoids having to
     * start ant with the class path of the project it is building.
     *
     * @exception BuildException if someting goes wrong with the build
     */
    public void execute() throws BuildException {
        if (weblogicSystemHome == null) {
            throw new BuildException("weblogic home must be set");
        }
        if (!weblogicSystemHome.isDirectory()) {
            throw new BuildException("weblogic home directory " + weblogicSystemHome.getPath() +
                                     " is not valid");
        }

        if (beaHome != null) {
            executeWLS6();
        } else {
            executeWLS();
        }
    }

    private File findSecurityPolicyFile(String defaultSecurityPolicy) {
        String securityPolicy = this.securityPolicy;
        if (securityPolicy == null) {
            securityPolicy = defaultSecurityPolicy;
        }
        File securityPolicyFile = new File(weblogicSystemHome, securityPolicy);
        // If an explicit securityPolicy file was specified, it maybe an
        // absolute path.  Use the project to resolve it.
        if (this.securityPolicy != null && !securityPolicyFile.exists()) {
            securityPolicyFile = project.resolveFile(securityPolicy);
        }
        // If we still can't find it, complain
        if (!securityPolicyFile.exists()) {
            throw new BuildException("Security policy " + securityPolicy +
                                     " was not found.");
        }
        return securityPolicyFile;
    }

    private void executeWLS6() {
        File securityPolicyFile
            = findSecurityPolicyFile(DEFAULT_WL60_POLICY_FILE);
        if (!beaHome.isDirectory()) {
            throw new BuildException("BEA home " + beaHome.getPath() +
                                     " is not valid");
        }

        File configFile = new File(weblogicSystemHome, "config/"
            + weblogicDomainName + "/config.xml");
        if (!configFile.exists()) {
            throw new BuildException("Server config file " + configFile
                + " not found.");
        }

        if (managementPassword == null) {
            throw new BuildException("You must supply a management password to start the server");
        }

        Java weblogicServer = (Java) project.createTask("java");
        weblogicServer.setTaskName(getTaskName());
        weblogicServer.setFork(true);
        weblogicServer.setDir(weblogicSystemHome);
        weblogicServer.setClassname(weblogicMainClass);

        String jvmArgs = additionalJvmArgs;

        jvmArgs += " -Dweblogic.Domain=" + weblogicDomainName;
        jvmArgs += " -Dweblogic.Name=" + weblogicSystemName;
        jvmArgs += " -Dweblogic.system.home=" + weblogicSystemHome;

        jvmArgs += " -Dbea.home=" + beaHome;
        jvmArgs += " -Djava.security.policy==" + securityPolicyFile;

        jvmArgs += " -Dweblogic.management.username=" + managementUsername;
        jvmArgs += " -Dweblogic.management.password=" + managementPassword;
        if (pkPassword != null) {
            jvmArgs += " -Dweblogic.pkpassword=" + pkPassword;
        }


        weblogicServer.createJvmarg().setLine(jvmArgs);
        weblogicServer.createArg().setLine(additionalArgs);

        if (classpath != null) {
            weblogicServer.setClasspath(classpath);
        }

        if (weblogicServer.executeJava() != 0) {
            throw new BuildException("Execution of weblogic server failed");
        }
     }

    private void executeWLS() {
        File securityPolicyFile
            = findSecurityPolicyFile(DEFAULT_WL51_POLICY_FILE);
        File propertiesFile = null;


        if (weblogicPropertiesFile == null) {
            weblogicPropertiesFile = DEFAULT_PROPERTIES_FILE;
        }
        propertiesFile = new File(weblogicSystemHome, weblogicPropertiesFile);
        if (!propertiesFile.exists()) {
            // OK, properties file may be absolute
            propertiesFile = project.resolveFile(weblogicPropertiesFile);
            if (!propertiesFile.exists()) {
                throw new BuildException("Properties file " + weblogicPropertiesFile +
                                         " not found in weblogic home " + weblogicSystemHome +
                                         " or as absolute file");
            }
        }

        Java weblogicServer = (Java) project.createTask("java");
        weblogicServer.setTaskName(getTaskName());
        weblogicServer.setFork(true);
        weblogicServer.setClassname(weblogicMainClass);

        String jvmArgs = additionalJvmArgs;

        if (weblogicClasspath != null) {
            jvmArgs += " -Dweblogic.class.path=" + weblogicClasspath;
        }

        jvmArgs += " -Djava.security.manager -Djava.security.policy==" + securityPolicyFile;
        jvmArgs += " -Dweblogic.system.home=" + weblogicSystemHome;
        jvmArgs += " -Dweblogic.system.name=" + weblogicSystemName;
        jvmArgs += " -Dweblogic.system.propertiesFile=" + weblogicPropertiesFile;

        weblogicServer.createJvmarg().setLine(jvmArgs);
        weblogicServer.createArg().setLine(additionalArgs);

        if (classpath != null) {
            weblogicServer.setClasspath(classpath);
        }
        if (weblogicServer.executeJava() != 0) {
            throw new BuildException("Execution of weblogic server failed");
        }
    }


    /**
     * The classpath to be used with the Java Virtual Machine that runs the Weblogic
     * Server; required. Prior to Weblogic 6.0, this is typically set to the Weblogic
     * boot classpath. Under Weblogic 6.0 this should include all the
     * weblogic jars
     *
     * @param classpath the classpath to use when executing the weblogic server.
     */
    public void setClasspath(Path classpath) {
        this.classpath = classpath;
    }

    /**
     * Set the weblogic classpath used by the Weblogic Server;
     * optional, and only applicable to WL4.5.1
     *
     * The weblogic classpath is used by weblogic to support dynamic class loading.
     *
     * @param weblogicClasspath the weblogic classpath
     */
    public void setWlclasspath(Path weblogicClasspath) {
        this.weblogicClasspath = weblogicClasspath;
    }

    /**
     * The name of the security policy file within the weblogic home directory that
     * is to be used. If not specified, the default policy file <code>weblogic.policy</code>
     * is used.
     *
     * @param securityPolicy the security policy to use.
     */
    public void setPolicy(String securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    /**
     * The location where weblogic lives.
     * Required. This is the absolute location, not relative to
     *  BEA home.
     * @param weblogicHome the home directory of weblogic.
     *
     */
    public void setHome(File weblogicHome) {
        weblogicSystemHome = weblogicHome;
    }

    /**
     * The location of the BEA Home; implicitly
     * selects Weblogic 6.0; optional.
     *
     * @param beaHome the BEA Home directory.
     *
     */
    public void setBEAHome(File beaHome) {
        this.beaHome = beaHome;
    }

    /**
     * The name of the weblogic server within the weblogic home which is to be run.
     * Optiona, defaults to &quot;myserver&quot;
     *
     * @param serverName the name of the server.
     */
    public void setName(String serverName) {
        this.weblogicSystemName = serverName;
    }

    /**
     * Set the Domain to run in; required for WL6.0
     *
     * @param domain the domain
     */
    public void setDomain(String domain) {
        this.weblogicDomainName = domain;
    }

    /**
     * The name of the server's properties file within the weblogic home directory
     * used to control the weblogic instance;
     * required for WL4.5.1
     *
     *
     * @param propertiesFilename the properties file name
     */
    public void setProperties(String propertiesFilename) {
        this.weblogicPropertiesFile = propertiesFilename;
    }

    /**
     * Set the additional arguments to pass to the weblogic JVM
     * @param args the arguments to be passed to the JVM
     */
    public void setJvmargs(String args) {
        this.additionalJvmArgs = args;
    }

    /**
     * Set the management username to run the server;
     * optional and only applicable to WL6.0.
     *
     * @param username the management username of the server.
     */
    public void setUsername(String username) {
        this.managementUsername = username;
    }


    /**
     * Set the management password of the server;
     * optional and only applicable to WL6.0.
     * @param password the management pasword of the server.
     */
    public void setPassword(String password) {
        this.managementPassword = password;
    }

    /**
     * Set the private key password so the server can decrypt the SSL private key file;
     * optional and only applicable to WL6.0.
     * @param pkpassword the private key password,
     */
    public void setPKPassword(String pkpassword) {
        this.pkPassword = pkpassword;
    }

    /**
     * Additional argument string passed to the Weblogic instance;
     * optional.
     */
    public void setArgs(String args) {
        additionalArgs = args;
    }

    /**
     * name of the main class for weblogic; optional.
     */
    public void setWeblogicMainClass(String c) {
        weblogicMainClass = c;
    }
}
