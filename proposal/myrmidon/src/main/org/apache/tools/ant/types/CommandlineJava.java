/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
import org.apache.tools.ant.Project;

/**
 * A representation of a Java command line that is nothing more than a composite
 * of 2 <tt>Commandline</tt> . 1 for the vm/options and 1 for the
 * classname/arguments. It provides specific methods for a java command line.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="sbailliez@apache.org">Stephane Bailliez</a>
 */
public class CommandlineJava implements Cloneable
{
    private Commandline vmCommand = new Commandline();
    private Commandline javaCommand = new Commandline();
    private SysProperties sysProperties = new SysProperties();
    private Path classpath;
    private String maxMemory;

    /**
     * Indicate whether it will execute a jar file or not, in this case the
     * first vm option must be a -jar and the 'executable' is a jar file.
     */
    private boolean executeJar;

    public CommandlineJava()
    {
        setVm( getJavaExecutableName() );
    }

    /**
     * set the classname to execute
     *
     * @param classname the fully qualified classname.
     */
    public void setClassname( String classname )
    {
        javaCommand.setExecutable( classname );
        executeJar = false;
    }

    /**
     * set a jar file to execute via the -jar option.
     *
     * @param jarpathname The new Jar value
     */
    public void setJar( String jarpathname )
    {
        javaCommand.setExecutable( jarpathname );
        executeJar = true;
    }

    /**
     * -mx or -Xmx depending on VM version
     *
     * @param max The new Maxmemory value
     */
    public void setMaxmemory( String max )
    {
        this.maxMemory = max;
    }

    public void setSystemProperties()
        throws TaskException
    {
        sysProperties.setSystem();
    }

    public void setVm( String vm )
    {
        vmCommand.setExecutable( vm );
    }

    /**
     * @return the name of the class to run or <tt>null</tt> if there is no
     *      class.
     * @see #getJar()
     */
    public String getClassname()
    {
        if( !executeJar )
        {
            return javaCommand.getExecutable();
        }
        return null;
    }

    public Path getClasspath()
    {
        return classpath;
    }

    /**
     * get the command line to run a java vm.
     *
     * @return the list of all arguments necessary to run the vm.
     */
    public String[] getCommandline()
        throws TaskException
    {
        String[] result = new String[ size() ];
        int pos = 0;
        String[] vmArgs = getActualVMCommand().getCommandline();
        // first argument is the java.exe path...
        result[ pos++ ] = vmArgs[ 0 ];

        // -jar must be the first option in the command line.
        if( executeJar )
        {
            result[ pos++ ] = "-jar";
        }
        // next follows the vm options
        System.arraycopy( vmArgs, 1, result, pos, vmArgs.length - 1 );
        pos += vmArgs.length - 1;
        // properties are part of the vm options...
        if( sysProperties.size() > 0 )
        {
            System.arraycopy( sysProperties.getJavaVariables(), 0,
                              result, pos, sysProperties.size() );
            pos += sysProperties.size();
        }
        // classpath is a vm option too..
        Path fullClasspath = classpath != null ? classpath.concatSystemClasspath( "ignore" ) : null;
        if( fullClasspath != null && fullClasspath.toString().trim().length() > 0 )
        {
            result[ pos++ ] = "-classpath";
            result[ pos++ ] = fullClasspath.toString();
        }
        // this is the classname to run as well as its arguments.
        // in case of 'executeJar', the executable is a jar file.
        System.arraycopy( javaCommand.getCommandline(), 0,
                          result, pos, javaCommand.size() );
        return result;
    }

    /**
     * @return the pathname of the jar file to run via -jar option or <tt>null
     *      </tt> if there is no jar to run.
     * @see #getClassname()
     */
    public String getJar()
    {
        if( executeJar )
        {
            return javaCommand.getExecutable();
        }
        return null;
    }

    public Commandline getJavaCommand()
    {
        return javaCommand;
    }

    public SysProperties getSystemProperties()
    {
        return sysProperties;
    }

    public Commandline getVmCommand()
    {
        return getActualVMCommand();
    }

    public void addSysproperty( EnvironmentVariable sysp )
    {
        sysProperties.addVariable( sysp );
    }

    /**
     * Clear out the java arguments.
     */
    public void clearJavaArgs()
    {
        javaCommand.clearArgs();
    }

    public Object clone()
    {
        CommandlineJava c = new CommandlineJava();
        c.vmCommand = (Commandline)vmCommand.clone();
        c.javaCommand = (Commandline)javaCommand.clone();
        c.sysProperties = (SysProperties)sysProperties.clone();
        c.maxMemory = maxMemory;
        if( classpath != null )
        {
            c.classpath = (Path)classpath.clone();
        }
        c.executeJar = executeJar;
        return c;
    }

    public Argument createArgument()
    {
        return javaCommand.createArgument();
    }

    public Path createClasspath( Project p )
    {
        if( classpath == null )
        {
            classpath = new Path();
        }
        return classpath;
    }

    public Argument createVmArgument()
    {
        return vmCommand.createArgument();
    }

    public void restoreSystemProperties()
        throws TaskException
    {
        sysProperties.restoreSystem();
    }

    /**
     * The size of the java command line.
     *
     * @return the total number of arguments in the java command line.
     * @see #getCommandline()
     */
    public int size()
        throws TaskException
    {
        int size = getActualVMCommand().size() + javaCommand.size() + sysProperties.size();
        // classpath is "-classpath <classpath>" -> 2 args
        Path fullClasspath = classpath != null ? classpath.concatSystemClasspath( "ignore" ) : null;
        if( fullClasspath != null && fullClasspath.toString().trim().length() > 0 )
        {
            size += 2;
        }
        // jar execution requires an additional -jar option
        if( executeJar )
        {
            size++;
        }
        return size;
    }

    public String toString()
    {
        try
        {
            return Commandline.toString( getCommandline() );
        }
        catch( TaskException e )
        {
            return e.toString();
        }
    }

    private Commandline getActualVMCommand()
    {
        Commandline actualVMCommand = (Commandline)vmCommand.clone();
        if( maxMemory != null )
        {
            actualVMCommand.createArgument().setValue( "-Xmx" + maxMemory );
        }
        return actualVMCommand;
    }

    private String getJavaExecutableName()
    {
        // This is the most common extension case - exe for windows and OS/2,
        // nothing for *nix.
        String extension = Os.isFamily( "dos" ) ? ".exe" : "";

        // Look for java in the java.home/../bin directory.  Unfortunately
        // on Windows java.home doesn't always refer to the correct location,
        // so we need to fall back to assuming java is somewhere on the
        // PATH.
        java.io.File jExecutable =
            new java.io.File( System.getProperty( "java.home" ) +
                              "/../bin/java" + extension );

        if( jExecutable.exists() && !Os.isFamily( "netware" ) )
        {
            // NetWare may have a "java" in that directory, but 99% of
            // the time, you don't want to execute it -- Jeff Tulley
            // <JTULLEY@novell.com>
            return jExecutable.getAbsolutePath();
        }
        else
        {
            return "java";
        }
    }

}
