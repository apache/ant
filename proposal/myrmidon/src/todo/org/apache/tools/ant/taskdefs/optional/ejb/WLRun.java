/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 * Execute a Weblogic server.
 *
 * @author <a href="mailto:conor@cortexebusiness.com.au">Conor MacNeill</a> ,
 *      Cortex ebusiness Pty Limited
 */
public class WLRun extends Task
{
    protected final static String DEFAULT_WL51_POLICY_FILE = "weblogic.policy";
    protected final static String DEFAULT_WL60_POLICY_FILE = "lib/weblogic.policy";
    protected final static String DEFAULT_PROPERTIES_FILE = "weblogic.properties";

    private String weblogicMainClass = "weblogic.Server";

    /**
     * Addional arguments to pass to the JVM used to run weblogic
     */
    private String additionalArgs = "";

    /**
     * The name of the weblogic server - used to select the server's directory
     * in the weblogic home directory.
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
     * The location of the BEA Home under which this server is run. WL6 only
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
     * The classpath to be used when running the Java VM. It must contain the
     * weblogic classes <b>and</b> the implementation classes of the home and
     * remote interfaces.
     */
    private Path classpath;

    /**
     * The security policy to use when running the weblogic server
     */
    private String securityPolicy;

    /**
     * The weblogic classpath to the be used when running weblogic.
     */
    private Path weblogicClasspath;

    /**
     * The weblogic domain
     */
    private String weblogicDomainName;

    /**
     * The weblogic system home directory
     */
    private File weblogicSystemHome;

    public void setArgs( String args )
    {
        additionalArgs = args;
    }

    /**
     * The location of the BEA Home.
     *
     * @param beaHome the BEA Home directory.
     */
    public void setBEAHome( File beaHome )
    {
        this.beaHome = beaHome;
    }

    /**
     * Set the classpath to be used for this execution.
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
    {
        this.classpath = classpath;
    }

    /**
     * Set the Domain to run in
     *
     * @param domain the domain
     */
    public void setDomain( String domain )
    {
        this.weblogicDomainName = domain;
    }

    /**
     * The location where weblogic lives.
     *
     * @param weblogicHome the home directory of weblogic.
     */
    public void setHome( File weblogicHome )
    {
        weblogicSystemHome = weblogicHome;
    }

    /**
     * Set the additional arguments to pass to the weblogic JVM
     *
     * @param args the arguments to be passed to the JVM
     */
    public void setJvmargs( String args )
    {
        this.additionalJvmArgs = args;
    }

    /**
     * Set the name of the server to run
     *
     * @param serverName The new Name value
     */
    public void setName( String serverName )
    {
        this.weblogicSystemName = serverName;
    }

    /**
     * Set the private key password so the server can decrypt the SSL private
     * key file.
     *
     * @param pkpassword the private key password,
     */
    public void setPKPassword( String pkpassword )
    {
        this.pkPassword = pkpassword;
    }

    /**
     * Set the management password of the server
     *
     * @param password the management pasword of the server.
     */
    public void setPassword( String password )
    {
        this.managementPassword = password;
    }

    /**
     * Set the security policy for this invocation of weblogic.
     *
     * @param securityPolicy the security policy to use.
     */
    public void setPolicy( String securityPolicy )
    {
        this.securityPolicy = securityPolicy;
    }

    /**
     * Set the properties file to use. The location of the properties file is
     * relative to the weblogi system home
     *
     * @param propertiesFilename the properties file name
     */
    public void setProperties( String propertiesFilename )
    {
        this.weblogicPropertiesFile = propertiesFilename;
    }

    /**
     * Set the management username to run the server
     *
     * @param username the management username of the server.
     */
    public void setUsername( String username )
    {
        this.managementUsername = username;
    }

    public void setWeblogicMainClass( String c )
    {
        weblogicMainClass = c;
    }

    /**
     * Set the weblogic classpath. The weblogic classpath is used by weblogic to
     * support dynamic class loading.
     *
     * @param weblogicClasspath the weblogic classpath
     */
    public void setWlclasspath( Path weblogicClasspath )
    {
        this.weblogicClasspath = weblogicClasspath;
    }

    /**
     * Add the classpath for the user classes
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( classpath == null )
        {
            classpath = new Path( getProject() );
        }
        return classpath.createPath();
    }

    /**
     * Get the classpath to the weblogic classpaths
     *
     * @return Description of the Returned Value
     */
    public Path createWLClasspath()
    {
        if( weblogicClasspath == null )
        {
            weblogicClasspath = new Path( getProject() );
        }
        return weblogicClasspath.createPath();
    }

    /**
     * Do the work. The work is actually done by creating a separate JVM to run
     * a helper task. This approach allows the classpath of the helper task to
     * be set. Since the weblogic tools require the class files of the project's
     * home and remote interfaces to be available in the classpath, this also
     * avoids having to start ant with the class path of the project it is
     * building.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
        if( weblogicSystemHome == null )
        {
            throw new TaskException( "weblogic home must be set" );
        }
        if( !weblogicSystemHome.isDirectory() )
        {
            throw new TaskException( "weblogic home directory " + weblogicSystemHome.getPath() +
                                     " is not valid" );
        }

        if( beaHome != null )
        {
            executeWLS6();
        }
        else
        {
            executeWLS();
        }
    }

    private void executeWLS()
        throws TaskException
    {
        File securityPolicyFile = findSecurityPolicyFile( DEFAULT_WL51_POLICY_FILE );
        File propertiesFile = null;

        if( weblogicPropertiesFile == null )
        {
            weblogicPropertiesFile = DEFAULT_PROPERTIES_FILE;
        }
        propertiesFile = new File( weblogicSystemHome, weblogicPropertiesFile );
        if( !propertiesFile.exists() )
        {
            // OK, properties file may be absolute
            propertiesFile = resolveFile( weblogicPropertiesFile );
            if( !propertiesFile.exists() )
            {
                throw new TaskException( "Properties file " + weblogicPropertiesFile +
                                         " not found in weblogic home " + weblogicSystemHome +
                                         " or as absolute file" );
            }
        }

        Java weblogicServer = (Java)getProject().createTask( "java" );
        weblogicServer.setFork( true );
        weblogicServer.setClassname( weblogicMainClass );

        String jvmArgs = additionalJvmArgs;

        if( weblogicClasspath != null )
        {
            jvmArgs += " -Dweblogic.class.path=" + weblogicClasspath;
        }

        jvmArgs += " -Djava.security.manager -Djava.security.policy==" + securityPolicyFile;
        jvmArgs += " -Dweblogic.system.home=" + weblogicSystemHome;
        jvmArgs += " -Dweblogic.system.name=" + weblogicSystemName;
        jvmArgs += " -Dweblogic.system.propertiesFile=" + weblogicPropertiesFile;

        weblogicServer.createJvmarg().setLine( jvmArgs );
        weblogicServer.createArg().setLine( additionalArgs );

        if( classpath != null )
        {
            weblogicServer.setClasspath( classpath );
        }
        if( weblogicServer.executeJava() != 0 )
        {
            throw new TaskException( "Execution of weblogic server failed" );
        }
    }

    private void executeWLS6()
        throws TaskException
    {
        File securityPolicyFile = findSecurityPolicyFile( DEFAULT_WL60_POLICY_FILE );
        if( !beaHome.isDirectory() )
        {
            throw new TaskException( "BEA home " + beaHome.getPath() +
                                     " is not valid" );
        }

        File configFile = new File( weblogicSystemHome, "config/" + weblogicDomainName + "/config.xml" );
        if( !configFile.exists() )
        {
            throw new TaskException( "Server config file " + configFile + " not found." );
        }

        if( managementPassword == null )
        {
            throw new TaskException( "You must supply a management password to start the server" );
        }

        Java weblogicServer = (Java)getProject().createTask( "java" );
        weblogicServer.setFork( true );
        weblogicServer.setDir( weblogicSystemHome );
        weblogicServer.setClassname( weblogicMainClass );

        String jvmArgs = additionalJvmArgs;

        jvmArgs += " -Dweblogic.Domain=" + weblogicDomainName;
        jvmArgs += " -Dweblogic.Name=" + weblogicSystemName;
        jvmArgs += " -Dweblogic.system.home=" + weblogicSystemHome;

        jvmArgs += " -Dbea.home=" + beaHome;
        jvmArgs += " -Djava.security.policy==" + securityPolicyFile;

        jvmArgs += " -Dweblogic.management.username=" + managementUsername;
        jvmArgs += " -Dweblogic.management.password=" + managementPassword;
        if( pkPassword != null )
        {
            jvmArgs += " -Dweblogic.pkpassword=" + pkPassword;
        }

        weblogicServer.createJvmarg().setLine( jvmArgs );
        weblogicServer.createArg().setLine( additionalArgs );

        if( classpath != null )
        {
            weblogicServer.setClasspath( classpath );
        }

        if( weblogicServer.executeJava() != 0 )
        {
            throw new TaskException( "Execution of weblogic server failed" );
        }
    }

    private File findSecurityPolicyFile( String defaultSecurityPolicy )
    {
        String securityPolicy = this.securityPolicy;
        if( securityPolicy == null )
        {
            securityPolicy = defaultSecurityPolicy;
        }
        File securityPolicyFile = new File( weblogicSystemHome, securityPolicy );
        // If an explicit securityPolicy file was specified, it maybe an
        // absolute path.  Use the project to resolve it.
        if( this.securityPolicy != null && !securityPolicyFile.exists() )
        {
            securityPolicyFile = resolveFile( securityPolicy );
        }
        // If we still can't find it, complain
        if( !securityPolicyFile.exists() )
        {
            throw new TaskException( "Security policy " + securityPolicy +
                                     " was not found." );
        }
        return securityPolicyFile;
    }
}
