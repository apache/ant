/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.file.Path;
import org.apache.myrmidon.framework.java.ExecuteJava;
import org.apache.tools.todo.types.Argument;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.EnumeratedAttribute;
import org.apache.tools.todo.types.EnvironmentData;
import org.apache.tools.todo.types.EnvironmentVariable;
import org.apache.tools.todo.types.PathUtil;
import org.apache.tools.todo.types.SysProperties;

/**
 * Ant task to run JUnit tests. <p>
 *
 * JUnit is a framework to create unit test. It has been initially created by
 * Erich Gamma and Kent Beck. JUnit can be found at <a
 * href="http://www.junit.org">http://www.junit.org</a> . <p>
 *
 * <code>JUnitTask</code> can run a single specific <code>JUnitTest</code> using
 * the <code>test</code> element. For example, the following target <code><pre>
 *   &lt;target name="test-int-chars" depends="jar-test"&gt;
 *       &lt;echo message="testing international characters"/&gt;
 *       &lt;junit printsummary="no" haltonfailure="yes" fork="false"&gt;
 *           &lt;classpath refid="classpath"/&gt;
 *           &lt;formatter type="plain" usefile="false" /&gt;
 *           &lt;test name="org.apache.ecs.InternationalCharTest" /&gt;
 *       &lt;/junit&gt;
 *   &lt;/target&gt;
 * </pre></code> runs a single junit test (<code>org.apache.ecs.InternationalCharTest</code>
 * ) in the current VM using the path with id <code>classpath</code> as
 * classpath and presents the results formatted using the standard <code>plain</code>
 * formatter on the command line. <p>
 *
 * This task can also run batches of tests. The <code>batchtest</code> element
 * creates a <code>BatchTest</code> based on a fileset. This allows, for
 * example, all classes found in directory to be run as testcases. For example,
 * <code><pre>
 * &lt;target name="run-tests" depends="dump-info,compile-tests" if="junit.present"&gt;
 *   &lt;junit printsummary="no" haltonfailure="yes" fork="${junit.fork}"&gt;
 *     &lt;jvmarg value="-classic"/&gt;
 *     &lt;classpath refid="tests-classpath"/&gt;
 *     &lt;sysproperty key="build.tests" value="${build.tests}"/&gt;
 *     &lt;formatter type="brief" usefile="false" /&gt;
 *     &lt;batchtest&gt;
 *       &lt;fileset dir="${tests.dir}"&gt;
 *         &lt;include name="**&#047;*Test*" /&gt;
 *       &lt;/fileset&gt;
 *     &lt;/batchtest&gt;
 *   &lt;/junit&gt;
 * &lt;/target&gt;
 * </pre></code> this target finds any classes with a <code>test</code>
 * directory anywhere in their path (under the top <code>${tests.dir}</code>, of
 * course) and creates <code>JUnitTest</code>'s for each one. <p>
 *
 * Of course, <code>&lt;junit&gt;</code> and <code>&lt;batch&gt;</code> elements
 * can be combined for more complex tests. For an example, see the ant <code>build.xml</code>
 * target <code>run-tests</code> (the second example is an edited version). <p>
 *
 * To spawn a new Java VM to prevent interferences between different testcases,
 * you need to enable <code>fork</code>. A number of attributes and elements
 * allow you to set up how this JVM runs.
 * <ul>
 *   <li> {@link #setTimeout} property sets the maximum time allowed before a
 *   test is 'timed out'
 *   <li> {@link #setMaxmemory} property sets memory assignment for the forked
 *   jvm
 *   <li> {@link #setJvm} property allows the jvm to be specified
 *   <li> The <code>&lt;jvmarg&gt;</code> element sets arguements to be passed
 *   to the forked jvm
 * </ul>
 *
 *
 * @author Thomas Haas
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @author <a href="mailto:Gerrit.Riessen@web.de">Gerrit Riessen</a>
 * @author <a href="mailto:ehatcher@apache.org">Erik Hatcher</a>
 * @see JUnitTest
 * @see BatchTest
 */
public class JUnitTask extends AbstractTask
{
    private ArrayList tests = new ArrayList();
    private ArrayList batchTests = new ArrayList();
    private ArrayList formatters = new ArrayList();

    private Integer timeout = null;
    private boolean summary = false;
    private String summaryValue = "";
    private JUnitTestRunner runner = null;
    private File dir;
    private String jvm;
    private String maxMem;
    private EnvironmentData sysProperties = new EnvironmentData();
    private Path classPath = new Path();
    private Commandline vmArgs = new Commandline();

    /**
     * The directory to invoke the VM in. Ignored if no JVM is forked.
     *
     * @param dir the directory to invoke the JVM from.
     * @see #setFork(boolean)
     */
    public void setDir( final File dir )
    {
        this.dir = dir;
    }

    /**
     * Tells this task to set the named property to "true" when there is a error
     * in a test. This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test), however, it can possibly be overriden by their own
     * properties.
     *
     * @param propertyName The new ErrorProperty value
     */
    public void setErrorProperty( String propertyName )
    {
        Iterator enum = allTests();
        while( enum.hasNext() )
        {
            BaseTest test = (BaseTest)enum.next();
            test.setErrorProperty( propertyName );
        }
    }

    /**
     * Tells this task to set the named property to "true" when there is a
     * failure in a test. This property is applied on all BatchTest (batchtest)
     * and JUnitTest (test), however, it can possibly be overriden by their own
     * properties.
     *
     * @param propertyName The new FailureProperty value
     */
    public void setFailureProperty( String propertyName )
    {
        Iterator enum = allTests();
        while( enum.hasNext() )
        {
            BaseTest test = (BaseTest)enum.next();
            test.setFailureProperty( propertyName );
        }
    }

    /**
     * Tells this task whether to smartly filter the stack frames of JUnit
     * testcase errors and failures before reporting them. This property is
     * applied on all BatchTest (batchtest) and JUnitTest (test) however it can
     * possibly be overridden by their own properties.
     *
     * @param value <tt>false</tt> if it should not filter, otherwise <tt>true
     *      <tt>
     */
    public void setFiltertrace( boolean value )
    {
        Iterator enum = allTests();
        while( enum.hasNext() )
        {
            BaseTest test = (BaseTest)enum.next();
            test.setFiltertrace( value );
        }
    }

    /**
     * Tells whether a JVM should be forked for each testcase. It avoids
     * interference between testcases and possibly avoids hanging the build.
     * this property is applied on all BatchTest (batchtest) and JUnitTest
     * (test) however it can possibly be overridden by their own properties.
     *
     * @param value <tt>true</tt> if a JVM should be forked, otherwise <tt>false
     *      </tt>
     * @see #setTimeout
     */
    public void setFork( boolean value )
    {
        Iterator enum = allTests();
        while( enum.hasNext() )
        {
            BaseTest test = (BaseTest)enum.next();
            test.setFork( value );
        }
    }

    /**
     * Tells this task to halt when there is an error in a test. this property
     * is applied on all BatchTest (batchtest) and JUnitTest (test) however it
     * can possibly be overridden by their own properties.
     *
     * @param value <tt>true</tt> if it should halt, otherwise <tt>false</tt>
     */
    public void setHaltonerror( boolean value )
    {
        Iterator enum = allTests();
        while( enum.hasNext() )
        {
            BaseTest test = (BaseTest)enum.next();
            test.setHaltonerror( value );
        }
    }

    /**
     * Tells this task to halt when there is a failure in a test. this property
     * is applied on all BatchTest (batchtest) and JUnitTest (test) however it
     * can possibly be overridden by their own properties.
     *
     * @param value <tt>true</tt> if it should halt, otherwise <tt>false</tt>
     */
    public void setHaltonfailure( boolean value )
    {
        Iterator enum = allTests();
        while( enum.hasNext() )
        {
            BaseTest test = (BaseTest)enum.next();
            test.setHaltonfailure( value );
        }
    }

    /**
     * Set a new VM to execute the testcase. Default is <tt>java</tt> . Ignored
     * if no JVM is forked.
     *
     * @param value the new VM to use instead of <tt>java</tt>
     * @see #setFork(boolean)
     */
    public void setJvm( final String value )
    {
        jvm = value;
    }

    /**
     * Set the maximum memory to be used by all forked JVMs.
     *
     * @param max the value as defined by <tt>-mx</tt> or <tt>-Xmx</tt> in the
     *      java command line options.
     */
    public void setMaxmemory( final String max )
    {
        maxMem = max;
    }

    /**
     * Tells whether the task should print a short summary of the task.
     *
     * @param value <tt>true</tt> to print a summary, <tt>withOutAndErr</tt> to
     *      include the test&apos;s output as well, <tt>false</tt> otherwise.
     * @see SummaryJUnitResultFormatter
     */
    public void setPrintsummary( SummaryAttribute value )
    {
        summaryValue = value.getValue();
        summary = value.asBoolean();
    }

    /**
     * Set the timeout value (in milliseconds). If the test is running for more
     * than this value, the test will be canceled. (works only when in 'fork'
     * mode).
     *
     * @param value the maximum time (in milliseconds) allowed before declaring
     *      the test as 'timed-out'
     * @see #setFork(boolean)
     */
    public void setTimeout( Integer value )
    {
        timeout = value;
    }

    /**
     * Add a new formatter to all tests of this task.
     *
     * @param fe The feature to be added to the Formatter attribute
     */
    public void addFormatter( FormatterElement fe )
    {
        formatters.add( fe );
    }

    /**
     * Add a nested sysproperty element. This might be useful to tranfer Ant
     * properties to the testcases when JVM forking is not enabled.
     *
     * @param sysp The feature to be added to the Sysproperty attribute
     */
    public void addSysproperty( EnvironmentVariable sysp )
    {
        sysProperties.addVariable( sysp );
    }

    /**
     * Add a new single testcase.
     *
     * @param test a new single testcase
     * @see JUnitTest
     */
    public void addTest( JUnitTest test )
    {
        tests.add( test );
    }

    /**
     * Create a new set of testcases (also called ..batchtest) and add it to the
     * list.
     */
    public void addBatchTest( final BatchTest test )
    {
        batchTests.add( test );
    }

    /**
     * <code>&lt;classpath&gt;</code> allows classpath to be set for tests.
     */
    public void addClasspath( final Path path )
    {
        classPath.add( path );
    }

    /**
     * Create a new JVM argument. Ignored if no JVM is forked.
     *
     * @see #setFork(boolean)
     */
    public void addJvmarg( final Argument argument )
    {
        vmArgs.addArgument( argument );
    }

    /**
     * Runs the testcase.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {

        /*
         * Adds the jars or directories containing Ant, this task and JUnit to the
         * classpath - this should make the forked JVM work without having to
         * specify them directly.
         */
        addClasspathEntry( "/junit/framework/TestCase.class" );
        addClasspathEntry( "/org/apache/tools/ant/Task.class" );
        addClasspathEntry( "/org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner.class" );

        Iterator list = getIndividualTests();
        while( list.hasNext() )
        {
            JUnitTest test = (JUnitTest)list.next();
            final TaskContext context = getContext();
            if( test.shouldRun( context ) )
            {
                execute( test );
            }
        }
    }

    /**
     * Merge all individual tests from the batchtest with all individual tests
     * and return an enumeration over all <tt>JUnitTest</tt> .
     *
     * @return The IndividualTests value
     */
    protected Iterator getIndividualTests()
        throws TaskException
    {
        Iterator[] enums = new Iterator[ batchTests.size() + 1 ];
        for( int i = 0; i < batchTests.size(); i++ )
        {
            BatchTest batchtest = (BatchTest)batchTests.get( i );
            enums[ i ] = batchtest.iterator();
        }
        enums[ enums.length - 1 ] = tests.iterator();
        return new CompoundIterator( enums );
    }

    /**
     * return the file or null if does not use a file
     *
     * @param fe Description of Parameter
     * @param test Description of Parameter
     * @return The Output value
     */
    protected File getOutput( FormatterElement fe, JUnitTest test )
        throws TaskException
    {
        if( fe.getUseFile() )
        {
            String filename = test.getOutfile() + fe.getExtension();
            File destFile = new File( test.getTodir(), filename );
            final String absFilename = destFile.getAbsolutePath();
            return getContext().resolveFile( absFilename );
        }
        return null;
    }

    /**
     * Search for the given resource and add the directory or archive that
     * contains it to the classpath. <p>
     *
     * Doesn't work for archives in JDK 1.1 as the URL returned by getResource
     * doesn't contain the name of the archive.</p>
     *
     * @param resource The feature to be added to the ClasspathEntry attribute
     */
    protected void addClasspathEntry( String resource )
    {
        URL url = getClass().getResource( resource );
        if( url != null )
        {
            String u = url.toString();
            if( u.startsWith( "jar:file:" ) )
            {
                int pling = u.indexOf( "!" );
                String jarName = u.substring( 9, pling );
                getContext().debug( "Implicitly adding " + jarName + " to classpath" );
                classPath.addLocation( new File( jarName ) );
            }
            else if( u.startsWith( "file:" ) )
            {
                int tail = u.indexOf( resource );
                String dirName = u.substring( 5, tail );
                getContext().debug( "Implicitly adding " + dirName + " to classpath" );
                classPath.addLocation( new File( dirName ) );
            }
            else
            {
                getContext().debug( "Don\'t know how to handle resource URL " + u );
            }
        }
        else
        {
            getContext().debug( "Couldn\'t find " + resource );
        }
    }

    protected Iterator allTests()
    {
        Iterator[] enums = {tests.iterator(), batchTests.iterator()};
        return new CompoundIterator( enums );
    }

    /**
     * Run the tests.
     *
     * @param test Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void execute( final JUnitTest test )
        throws TaskException
    {
        // set the default values if not specified
        //@todo should be moved to the test class instead.
        if( test.getTodir() == null )
        {
            test.setTodir( getBaseDirectory() );
        }

        if( test.getOutfile() == null )
        {
            test.setOutfile( "TEST-" + test.getName() );
        }

        // execute the test and get the return code
        int exitValue = JUnitTestRunner.ERRORS;
        boolean wasKilled = false;
        if( !test.getFork() )
        {
            exitValue = executeInVM( test );
        }
        else
        {
            exitValue = executeAsForked( test );
        }

        // if there is an error/failure and that it should halt, stop everything otherwise
        // just log a statement
        boolean errorOccurredHere = exitValue == JUnitTestRunner.ERRORS;
        boolean failureOccurredHere = exitValue != JUnitTestRunner.SUCCESS;
        if( errorOccurredHere || failureOccurredHere )
        {
            if( errorOccurredHere && test.getHaltonerror()
                || failureOccurredHere && test.getHaltonfailure() )
            {
                throw new TaskException( "Test " + test.getName() + " failed"
                                         + ( wasKilled ? " (timeout)" : "" ) );
            }
            else
            {
                final String message = "TEST " + test.getName() + " FAILED" +
                    ( wasKilled ? " (timeout)" : "" );
                getContext().error( message );
                if( errorOccurredHere && test.getErrorProperty() != null )
                {
                    final String name = test.getErrorProperty();
                    getContext().setProperty( name, "true" );
                }
                if( failureOccurredHere && test.getFailureProperty() != null )
                {
                    final String name = test.getFailureProperty();
                    getContext().setProperty( name, "true" );
                }
            }
        }
    }

    protected void handleErrorOutput( String line )
    {
        if( runner != null )
        {
            runner.handleErrorOutput( line );
        }
        else
        {
            //super.handleErrorOutput( line );
        }
    }

    // in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)


    protected void handleOutput( String line )
    {
        if( runner != null )
        {
            runner.handleOutput( line );
        }
        else
        {
            //super.handleOutput( line );
        }
    }

    /**
     * Execute a testcase by forking a new JVM. The command will block until it
     * finishes. To know if the process was destroyed or not, use the <tt>
     * killedProcess()</tt> method of the watchdog class.
     *
     * @param test the testcase to execute.
     */
    private int executeAsForked( JUnitTest test )
        throws TaskException
    {
        ExecuteJava cmd = new ExecuteJava();
        cmd.setJvm( jvm );
        cmd.setIgnoreReturnCode( true );
        cmd.setWorkingDirectory( dir );
        cmd.setMaxMemory( maxMem );
        cmd.getClassPath().add( classPath );
        cmd.getVmArguments().addArguments( vmArgs );
        cmd.getSysProperties().addVariables( sysProperties );

        cmd.setClassName( "org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner" );
        cmd.getArguments().addArgument( test.getName() );
        cmd.getArguments().addArgument( "filtertrace=" + test.getFiltertrace() );
        cmd.getArguments().addArgument( "haltOnError=" + test.getHaltonerror() );
        cmd.getArguments().addArgument( "haltOnFailure=" + test.getHaltonfailure() );
        if( summary )
        {
            getContext().info( "Running " + test.getName() );
            cmd.getArguments().addArgument( "formatter=org.apache.tools.ant.taskdefs.optional.junit.SummaryJUnitResultFormatter" );
        }

        StringBuffer formatterArg = new StringBuffer( 128 );
        final FormatterElement[] feArray = mergeFormatters( test );
        for( int i = 0; i < feArray.length; i++ )
        {
            FormatterElement fe = feArray[ i ];
            formatterArg.append( "formatter=" );
            formatterArg.append( fe.getClassname() );
            File outFile = getOutput( fe, test );
            if( outFile != null )
            {
                formatterArg.append( "," );
                formatterArg.append( outFile );
            }
            cmd.getArguments().addArgument( formatterArg.toString() );
            formatterArg.setLength( 0 );
        }

        // Create a temporary file to pass the Ant properties to the forked test
        File propsFile = new File( "junit" + ( new Random( System.currentTimeMillis() ) ).nextLong() + ".properties" );
        cmd.getArguments().addArgument( "propsfile=" + propsFile.getAbsolutePath() );
        Properties props = new Properties();
        props.putAll( getContext().getProperties() );
        try
        {
            final FileOutputStream outstream = new FileOutputStream( propsFile );
            props.save( outstream, "Ant JUnitTask generated properties file" );
            outstream.close();
        }
        catch( IOException ioe )
        {
            throw new TaskException( "Error creating temporary properties file.", ioe );
        }

        try
        {
            return cmd.executeForked( getContext() );
        }
        finally
        {
            if( !propsFile.delete() )
            {
                throw new TaskException( "Could not delete temporary properties file." );
            }
        }
    }

    /**
     * Execute inside VM.
     */
    private int executeInVM( JUnitTest test )
        throws TaskException
    {
        test.setProperties( getContext().getProperties() );

        SysProperties.setSystem( sysProperties );

        try
        {
            getContext().debug( "Using System properties " + System.getProperties() );
            final ClassLoader classLoader = PathUtil.createClassLoader( classPath, getContext() );

            runner = new JUnitTestRunner( test,
                                          test.getHaltonerror(),
                                          test.getFiltertrace(),
                                          test.getHaltonfailure(),
                                          classLoader );
            if( summary )
            {
                getContext().info( "Running " + test.getName() );

                SummaryJUnitResultFormatter f =
                    new SummaryJUnitResultFormatter();
                f.setWithOutAndErr( "withoutanderr".equalsIgnoreCase( summaryValue ) );
                f.setOutput( System.out );
                runner.addFormatter( f );
            }

            final FormatterElement[] feArray = mergeFormatters( test );
            for( int i = 0; i < feArray.length; i++ )
            {
                FormatterElement fe = feArray[ i ];
                File outFile = getOutput( fe, test );
                if( outFile != null )
                {
                    fe.setOutfile( outFile );
                }
                else
                {
                    fe.setOutput( System.out );
                }
                runner.addFormatter( fe.createFormatter() );
            }

            runner.run();
            return runner.getRetCode();
        }
        finally
        {
            SysProperties.restoreSystem();
        }
    }

    private FormatterElement[] mergeFormatters( JUnitTest test )
    {
        final ArrayList feArrayList = (ArrayList)formatters.clone();
        test.addFormattersTo( feArrayList );
        return (FormatterElement[])feArrayList.toArray( new FormatterElement[ feArrayList.size() ] );
    }

    /**
     * Print summary enumeration values.
     *
     * @author RT
     */
    public static class SummaryAttribute extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"true", "yes", "false", "no",
                                "on", "off", "withOutAndErr"};
        }

        public boolean asBoolean()
        {
            final String value = getValue();
            return "true".equals( value ) ||
                "on".equals( value ) ||
                "yes".equals( value ) ||
                "withOutAndErr".equals( value );
        }
    }

}
