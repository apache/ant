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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.api.TaskException;
import java.io.File;
import java.util.Vector;
import java.util.Iterator;

/**
 * Call Ant in a sub-project.
 *
 *  <pre>
 *  &lt;target name=&quot;foo&quot; depends=&quot;init&quot;&gt;
 *    &lt;ant antfile=&quot;build.xml&quot; target=&quot;bar&quot; &gt;
 *      &lt;property name=&quot;property1&quot; value=&quot;aaaaa&quot; /&gt;
 *      &lt;property name=&quot;foo&quot; value=&quot;baz&quot; /&gt;
 *    &lt;/ant&gt;</SPAN>
 *  &lt;/target&gt;</SPAN>
 *
 *  &lt;target name=&quot;bar&quot; depends=&quot;init&quot;&gt;
 *    &lt;echo message=&quot;prop is ${property1} ${foo}&quot; /&gt;
 *  &lt;/target&gt;
 * </pre>
 *
 *
 * @author costin@dnt.ro
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 */
public class Ant extends Task {

    /** the basedir where is executed the build file */
    private File dir = null;
    
    /** the build.xml file (can be absolute) in this case dir will be ignored */
    private String antFile = null;
    
    /** the target to call if any */
    private String target = null;
    
    /** the output */
    private String output = null;
    
    /** should we inherit properties from the parent ? */
    private boolean inheritAll = true;
    
    /** should we inherit references from the parent ? */
    private boolean inheritRefs = false;
    
    /** the properties to pass to the new project */
    private Vector properties = new Vector();
    
    /** the references to pass to the new project */
    private Vector references = new Vector();

    /**
     * If true, inherit all properties from parent Project
     * If false, inherit only userProperties and those defined
     * inside the ant call itself
     */
    public void setInheritAll(boolean value) {
       inheritAll = value;
    }

    /**
     * If true, inherit all references from parent Project
     * If false, inherit only those defined
     * inside the ant call itself
     */
    public void setInheritRefs(boolean value) {
        inheritRefs = value;
    }

    /**
     * ...
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * set the build file, it can be either absolute or relative.
     * If it is absolute, <tt>dir</tt> will be ignored, if it is
     * relative it will be resolved relative to <tt>dir</tt>.
     */
    public void setAntfile(String s) {
        // @note: it is a string and not a file to handle relative/absolute
        // otherwise a relative file will be resolved based on the current
        // basedir.
        this.antFile = s;
    }

    /**
     * set the target to execute. If none is defined it will
     * execute the default target of the build file
     */
    public void setTarget(String s) {
        this.target = s;
    }

    public void setOutput(String s) {
        this.output = s;
    }

    /** create a property to pass to the new project as a 'user property' */
    public Property createProperty() {
        Property p = new Property(true);
        properties.addElement( p );
        return p;
    }

    /** 
     * create a reference element that identifies a data type that
     * should be carried over to the new project.
     */
    public void addReference(Reference r) {
        references.addElement(r);
    }

    /**
     * Helper class that implements the nested &lt;reference&gt;
     * element of &lt;ant&gt; and &lt;antcall&gt;.
     */
    public static class Reference 
        extends org.apache.tools.ant.types.Reference {

        public Reference() {super();}
        
        private String targetid=null;
        public void setToRefid(String targetid) { this.targetid=targetid; }
        public String getToRefid() { return targetid; }
    }

    /**
     * Called by the project to let the task do its work. This method may be
     * called more than once, if the task is invoked more than once.
     * For example,
     * if target1 and target2 both depend on target3, then running
     * "ant target1 target2" will run all tasks in target3 twice.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException
    {
        Object ant1project = unsetProject();

        try
        {
            Configuration antConfig = buildAntTaskConfiguration();

            executeTask( antConfig );
        }
        finally
        {
            resetProject( ant1project );
        }
    }

    private void executeTask( Configuration antConfig )
    {
        try
        {
            Executor executor = (Executor) m_context.getService( Executor.class );
            ExecutionFrame frame =
                (ExecutionFrame) m_context.getService( ExecutionFrame.class );
            executor.execute( antConfig, frame );
        }
        catch( TaskException e )
        {
            throw new BuildException( e );
        }
    }

    private Configuration buildAntTaskConfiguration()
    {
        DefaultConfiguration antConfig = new DefaultConfiguration( "ant", "" );

        antConfig.setAttribute( "inherit-all", String.valueOf( inheritAll ) );

        // Ignore inheritRefs for now ( inheritAll == inheritRefs )

        if ( target != null )
        {
            antConfig.setAttribute( "target", target );
        }

        // Get the "file" value.
        if (antFile == null) {
            antFile = "build.xml";
        }

        if ( dir == null )
        {
            dir = project.getBaseDir();
        }

        File file = FileUtils.newFileUtils().resolveFile(dir, antFile);
        antFile = file.getAbsolutePath();

        antConfig.setAttribute( "file", antFile );

        // Add all of the properties.
        Iterator iter = properties.iterator();
        while( iter.hasNext() )
        {
            DefaultConfiguration param = new DefaultConfiguration( "param", "" );
            Property property = (Property)iter.next();
            param.setAttribute( "name", property.getName() );
            param.setAttribute( "value", property.getValue() );
            antConfig.addChild( param );
        }
        return antConfig;
    }

    private void resetProject( Object ant1project ) throws BuildException
    {
        try
        {
            m_context.setProperty( "ant1.project", ant1project );
        }
        catch( TaskException e )
        {
            throw new BuildException( e );
        }
    }

    private Object unsetProject() throws BuildException
    {
        Object ant1project = null;
        try
        {
            ant1project = m_context.getProperty( "ant1.project" );
            m_context.setProperty( "ant1.project", null );
        }
        catch( TaskException e )
        {
            throw new BuildException( e );
        }
        return ant1project;
    }
}
