/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Creates the necessary compiler adapter, given basic criteria.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public class CompilerAdapterFactory
{

    /**
     * This is a singlton -- can't create instances!!
     */
    private CompilerAdapterFactory()
    {
    }

    /**
     * Based on the parameter passed in, this method creates the necessary
     * factory desired. The current mapping for compiler names are as follows:
     *
     * <ul>
     *   <li> jikes = jikes compiler
     *   <li> classic, javac1.1, javac1.2 = the standard compiler from JDK
     *   1.1/1.2
     *   <li> modern, javac1.3 = the new compiler of JDK 1.3
     *   <li> jvc, microsoft = the command line compiler from Microsoft's SDK
     *   for Java / Visual J++
     *   <li> kjc = the kopi compiler</li>
     *   <li> gcj = the gcj compiler from gcc</li>
     *   <li> <i>a fully quallified classname</i> = the name of a compiler
     *   adapter
     * </ul>
     *
     *
     * @param compilerType either the name of the desired compiler, or the full
     *      classname of the compiler's adapter.
     * @param task a task to log through.
     * @return The Compiler value
     * @throws TaskException if the compiler type could not be resolved into a
     *      compiler adapter.
     */
    public static CompilerAdapter getCompiler( String compilerType, Task task )
        throws TaskException
    {
        /*
         * If I've done things right, this should be the extent of the
         * conditional statements required.
         */
        if( compilerType.equalsIgnoreCase( "jikes" ) )
        {
            return new Jikes();
        }
        if( compilerType.equalsIgnoreCase( "extJavac" ) )
        {
            return new JavacExternal();
        }
        if( compilerType.equalsIgnoreCase( "classic" ) ||
            compilerType.equalsIgnoreCase( "javac1.1" ) ||
            compilerType.equalsIgnoreCase( "javac1.2" ) )
        {
            return new Javac12();
        }
        if( compilerType.equalsIgnoreCase( "modern" ) ||
            compilerType.equalsIgnoreCase( "javac1.3" ) ||
            compilerType.equalsIgnoreCase( "javac1.4" ) )
        {
            // does the modern compiler exist?
            try
            {
                Class.forName( "com.sun.tools.javac.Main" );
            }
            catch( ClassNotFoundException cnfe )
            {
                task.log( "Modern compiler is not available - using "
                          + "classic compiler", Project.MSG_WARN );
                return new Javac12();
            }
            return new Javac13();
        }
        if( compilerType.equalsIgnoreCase( "jvc" ) ||
            compilerType.equalsIgnoreCase( "microsoft" ) )
        {
            return new Jvc();
        }
        if( compilerType.equalsIgnoreCase( "kjc" ) )
        {
            return new Kjc();
        }
        if( compilerType.equalsIgnoreCase( "gcj" ) )
        {
            return new Gcj();
        }
        if( compilerType.equalsIgnoreCase( "sj" ) ||
            compilerType.equalsIgnoreCase( "symantec" ) )
        {
            return new Sj();
        }
        return resolveClassName( compilerType );
    }

    /**
     * Tries to resolve the given classname into a compiler adapter. Throws a
     * fit if it can't.
     *
     * @param className The fully qualified classname to be created.
     * @return Description of the Returned Value
     * @throws TaskException This is the fit that is thrown if className isn't
     *      an instance of CompilerAdapter.
     */
    private static CompilerAdapter resolveClassName( String className )
        throws TaskException
    {
        try
        {
            Class c = Class.forName( className );
            Object o = c.newInstance();
            return (CompilerAdapter)o;
        }
        catch( ClassNotFoundException cnfe )
        {
            throw new TaskException( className + " can\'t be found.", cnfe );
        }
        catch( ClassCastException cce )
        {
            throw new TaskException( className + " isn\'t the classname of "
                                     + "a compiler adapter.", cce );
        }
        catch( Throwable t )
        {
            // for all other possibilities
            throw new TaskException( className + " caused an interesting "
                                     + "exception.", t );
        }
    }

}
