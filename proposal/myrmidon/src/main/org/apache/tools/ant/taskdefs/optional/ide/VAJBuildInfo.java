/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

/**
 * This class wraps the Ant project information needed to start Ant from Visual
 * Age. It serves the following purposes: - acts as model for AntMakeFrame -
 * converts itself to/from String (to store the information as ToolData in the
 * VA repository) - wraps Project functions for the GUI (get target list,
 * execute target) - manages a seperate thread for Ant project execution this
 * allows interrupting a running build from a GUI
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */

class VAJBuildInfo implements Runnable
{

    // name of the VA project this BuildInfo belongs to
    private String vajProjectName = "";

    // name of the Ant build file
    private String buildFileName = "";

    // main targets found in the build file
    private ArrayList projectTargets = new ArrayList();

    // target selected for execution
    private java.lang.String target = "";

    // log level
    private int outputMessageLevel = Project.MSG_INFO;

    // is true if Project initialization was successful
    private transient boolean projectInitialized = false;

    // Support for bound properties
    protected transient PropertyChangeSupport propertyChange;

    // thread for Ant build execution
    private Thread buildThread;

    // Ant Project created from build file
    private transient Project project;

    // the listener used to log output.
    private BuildListener projectLogger;

    /**
     * Creates a BuildInfo object from a String The String must be in the format
     * outputMessageLevel'|'buildFileName'|'defaultTarget'|'(project target'|')*
     *
     * @param data java.lang.String
     * @return org.apache.tools.ant.taskdefs.optional.vaj.BuildInfo
     */
    public static VAJBuildInfo parse( String data )
    {
        VAJBuildInfo result = new VAJBuildInfo();

        try
        {
            StringTokenizer tok = new StringTokenizer( data, "|" );
            result.setOutputMessageLevel( tok.nextToken() );
            result.setBuildFileName( tok.nextToken() );
            result.setTarget( tok.nextToken() );
            while( tok.hasMoreTokens() )
            {
                result.projectTargets.add( tok.nextToken() );
            }
        }
        catch( Throwable t )
        {
            // if parsing the info fails, just return
            // an empty VAJBuildInfo
        }
        return result;
    }

    /**
     * Search for the insert position to keep names a sorted list of Strings
     * This method has been copied from org.apache.tools.ant.Main
     *
     * @param names Description of Parameter
     * @param name Description of Parameter
     * @return Description of the Returned Value
     */
    private static int findTargetPosition( ArrayList names, String name )
    {
        int res = names.size();
        for( int i = 0; i < names.size() && res == names.size(); i++ )
        {
            if( name.compareTo( (String)names.get( i ) ) < 0 )
            {
                res = i;
            }
        }
        return res;
    }

    /**
     * Sets the build file name
     *
     * @param newBuildFileName The new BuildFileName value
     */
    public void setBuildFileName( String newBuildFileName )
    {
        String oldValue = buildFileName;
        buildFileName = newBuildFileName;
        setProjectInitialized( false );
        firePropertyChange( "buildFileName", oldValue, buildFileName );
    }

    /**
     * Sets the log level (value must be one of the constants in Project)
     *
     * @param newOutputMessageLevel The new OutputMessageLevel value
     */
    public void setOutputMessageLevel( int newOutputMessageLevel )
    {
        int oldValue = outputMessageLevel;
        outputMessageLevel = newOutputMessageLevel;
        firePropertyChange( "outputMessageLevel",
                            new Integer( oldValue ), new Integer( outputMessageLevel ) );
    }

    /**
     * Sets the target to execute when executeBuild is called
     *
     * @param newTarget build target
     */
    public void setTarget( String newTarget )
    {
        String oldValue = target;
        target = newTarget;
        firePropertyChange( "target", oldValue, target );
    }

    /**
     * Sets the name of the Visual Age for Java project where this BuildInfo
     * belongs to
     *
     * @param newVAJProjectName The new VAJProjectName value
     */
    public void setVAJProjectName( String newVAJProjectName )
    {
        String oldValue = vajProjectName;
        vajProjectName = newVAJProjectName;
        firePropertyChange( "VAJProjectName", oldValue, vajProjectName );
    }

    /**
     * Returns the build file name.
     *
     * @return build file name.
     */
    public String getBuildFileName()
    {
        return buildFileName;
    }

    /**
     * Returns the log level
     *
     * @return log level.
     */
    public int getOutputMessageLevel()
    {
        return outputMessageLevel;
    }

    /**
     * return a list of all targets in the current buildfile
     *
     * @return The ProjectTargets value
     */
    public ArrayList getProjectTargets()
    {
        return projectTargets;
    }

    /**
     * returns the selected target.
     *
     * @return The Target value
     */
    public java.lang.String getTarget()
    {
        return target;
    }

    /**
     * returns the VA project name
     *
     * @return The VAJProjectName value
     */
    public String getVAJProjectName()
    {
        return vajProjectName;
    }

    /**
     * Returns true, if the Ant project is initialized (i.e. buildfile loaded)
     *
     * @return The ProjectInitialized value
     */
    public boolean isProjectInitialized()
    {
        return projectInitialized;
    }

    /**
     * The addPropertyChangeListener method was generated to support the
     * propertyChange field.
     *
     * @param listener The feature to be added to the PropertyChangeListener
     *      attribute
     */
    public synchronized void addPropertyChangeListener( PropertyChangeListener listener )
    {
        getPropertyChange().addPropertyChangeListener( listener );
    }

    /**
     * Returns the BuildInfo information as String. The BuildInfo can be rebuilt
     * from that String by calling parse().
     *
     * @return java.lang.String
     */
    public String asDataString()
    {
        String result = getOutputMessageLevel() + "|" + getBuildFileName()
            + "|" + getTarget();
        for( Iterator e = getProjectTargets().iterator();
             e.hasNext(); )
        {
            result = result + "|" + e.next();
        }

        return result;
    }

    /**
     * cancels a build.
     */
    public void cancelBuild()
    {
        buildThread.interrupt();
    }

    /**
     * Executes the target set by setTarget().
     *
     * @param logger Description of Parameter
     */
    public void executeProject( BuildListener logger )
    {
        Throwable error;
        projectLogger = logger;
        try
        {
            buildThread = new Thread( this );
            buildThread.setPriority( Thread.MIN_PRIORITY );
            buildThread.start();
        }
        catch( RuntimeException exc )
        {
            error = exc;
            throw exc;
        }
        catch( Error err )
        {
            error = err;
            throw err;
        }
    }

    /**
     * The firePropertyChange method was generated to support the propertyChange
     * field.
     *
     * @param propertyName Description of Parameter
     * @param oldValue Description of Parameter
     * @param newValue Description of Parameter
     */
    public void firePropertyChange( java.lang.String propertyName, java.lang.Object oldValue, java.lang.Object newValue )
    {
        getPropertyChange().firePropertyChange( propertyName, oldValue, newValue );
    }

    /**
     * The removePropertyChangeListener method was generated to support the
     * propertyChange field.
     *
     * @param listener Description of Parameter
     */
    public synchronized void removePropertyChangeListener( PropertyChangeListener listener )
    {
        getPropertyChange().removePropertyChangeListener( listener );
    }

    /**
     * Executes a build. This method is executed by the Ant execution thread
     */
    public void run()
    {
        try
        {
            InterruptedChecker ic = new InterruptedChecker( projectLogger );
            BuildEvent e = new BuildEvent( getProject() );
            try
            {
                ic.buildStarted( e );

                if( !isProjectInitialized() )
                {
                    initProject();
                }

                project.addBuildListener( ic );
                project.executeTarget( target );

                ic.buildFinished( e );
            }
            catch( Throwable t )
            {
                e.setException( t );
                ic.buildFinished( e );
            }
            finally
            {
                project.removeBuildListener( ic );
            }
        }
        catch( Throwable t2 )
        {
            System.out.println( "unexpected exception!" );
            t2.printStackTrace();
        }
    }

    /**
     * reloads the build file and updates the target list
     */
    public void updateTargetList()
    {
        project = new Project();
        initProject();
        projectTargets.clear();
        Iterator ptargets = project.getTargets().iterator();
        while( ptargets.hasNext() )
        {
            Target currentTarget = (Target)ptargets.next();
            if( currentTarget.getDescription() != null )
            {
                String targetName = currentTarget.getName();
                int pos = findTargetPosition( projectTargets, targetName );
                projectTargets.insertElementAt( targetName, pos );
            }
        }
    }

    /**
     * Accessor for the propertyChange field.
     *
     * @return The PropertyChange value
     */
    protected PropertyChangeSupport getPropertyChange()
    {
        if( propertyChange == null )
        {
            propertyChange = new PropertyChangeSupport( this );
        }
        return propertyChange;
    }

    /**
     * Sets the log level (value must be one of the constants in Project)
     *
     * @param outputMessageLevel log level as String.
     */
    private void setOutputMessageLevel( String outputMessageLevel )
    {
        int level = Integer.parseInt( outputMessageLevel );
        setOutputMessageLevel( level );
    }

    /**
     * sets the initialized flag
     *
     * @param initialized The new ProjectInitialized value
     */
    private void setProjectInitialized( boolean initialized )
    {
        Boolean oldValue = new Boolean( projectInitialized );
        projectInitialized = initialized;
        firePropertyChange( "projectInitialized", oldValue, new Boolean( projectInitialized ) );
    }

    /**
     * Returns the Ant project
     *
     * @return org.apache.tools.ant.Project
     */
    private Project getProject()
    {
        if( project == null )
        {
            project = new Project();
        }
        return project;
    }

    /**
     * Initializes the Ant project. Assumes that the project attribute is
     * already set.
     */
    private void initProject()
    {
        try
        {
            project.init();
            File buildFile = new File( getBuildFileName() );
            project.setUserProperty( "ant.file", buildFile.getAbsolutePath() );

            //FIXME: Need to convert to Myrmidon style
            //ProjectHelper.configureProject( project, buildFile );
            setProjectInitialized( true );
        }
        catch( RuntimeException exc )
        {
            setProjectInitialized( false );
            throw exc;
        }
        catch( Error err )
        {
            setProjectInitialized( false );
            throw err;
        }
    }

    /**
     * This exception is thrown when a build is interrupted
     *
     * @author RT
     */
    public static class BuildInterruptedException extends TaskException
    {
        public String toString()
        {
            return "BUILD INTERRUPTED";
        }
    }

    /**
     * BuildListener which checks for interruption and throws Exception if build
     * process is interrupted. This class is a wrapper around a 'real' listener.
     *
     * @author RT
     */
    private class InterruptedChecker implements BuildListener
    {
        // the real listener
        BuildListener wrappedListener;

        /**
         * Can only be constructed as wrapper around a real listener
         *
         * @param listener the real listener
         */
        public InterruptedChecker( BuildListener listener )
        {
            super();
            wrappedListener = listener;
        }

        /**
         * Fired after the last target has finished. This event will still be
         * thrown if an error occured during the build.
         *
         * @param event Description of Parameter
         */
        public void buildFinished( BuildEvent event )
        {
            wrappedListener.buildFinished( event );
            checkInterrupted();
        }

        /**
         * Fired before any targets are started.
         *
         * @param event Description of Parameter
         */
        public void buildStarted( BuildEvent event )
        {
            wrappedListener.buildStarted( event );
            checkInterrupted();
        }

        /**
         * Fired whenever a message is logged.
         *
         * @param event Description of Parameter
         */
        public void messageLogged( BuildEvent event )
        {
            wrappedListener.messageLogged( event );
            checkInterrupted();
        }

        /**
         * Fired when a target has finished. This event will still be thrown if
         * an error occured during the build.
         *
         * @param event Description of Parameter
         */
        public void targetFinished( BuildEvent event )
        {
            wrappedListener.targetFinished( event );
            checkInterrupted();
        }

        /**
         * Fired when a target is started.
         *
         * @param event Description of Parameter
         */
        public void targetStarted( BuildEvent event )
        {
            wrappedListener.targetStarted( event );
            checkInterrupted();
        }

        /**
         * Fired when a task has finished. This event will still be throw if an
         * error occured during the build.
         *
         * @param event Description of Parameter
         */
        public void taskFinished( BuildEvent event )
        {
            wrappedListener.taskFinished( event );
            checkInterrupted();
        }

        /**
         * Fired when a task is started.
         *
         * @param event Description of Parameter
         */
        public void taskStarted( BuildEvent event )
        {
            wrappedListener.taskStarted( event );
            checkInterrupted();
        }

        /**
         * checks if the thread was interrupted. When an interrupt occured,
         * throw an Exception to stop the execution.
         */
        protected void checkInterrupted()
        {
            if( buildThread.isInterrupted() )
            {
                throw new BuildInterruptedException();
            }
        }
    }
}
