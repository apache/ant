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

import java.io.File;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.tools.ant.util.FileUtils;

/**
 * Ant1Compat version of &lt;ant&gt;, which delegates to the Myrmidon version.
 *
 * @author costin@dnt.ro
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 */
public class Ant
    extends AbstractAnt1AntTask
{

    /** the basedir where is executed the build file */
    private File dir = null;

    /** the build.xml file (can be absolute) in this case dir will be ignored */
    private String antFile = null;

    /** the output */
    private String output = null;

    /** should we inherit references from the parent ? */
    private boolean inheritRefs = false;

    /**
     * If true, inherit all references from parent Project
     * If false, inherit only those defined
     * inside the ant call itself
     */
    public void setInheritRefs( boolean value )
    {
        inheritRefs = value;
    }

    /**
     * ...
     */
    public void setDir( File d )
    {
        this.dir = d;
    }

    /**
     * set the build file, it can be either absolute or relative.
     * If it is absolute, <tt>dir</tt> will be ignored, if it is
     * relative it will be resolved relative to <tt>dir</tt>.
     */
    public void setAntfile( String s )
    {
        // @note: it is a string and not a file to handle relative/absolute
        // otherwise a relative file will be resolved based on the current
        // basedir.
        this.antFile = s;
    }

    public void setOutput( String s )
    {
        this.output = s;
    }

    /** create a property to pass to the new project as a 'user property' */
    public Property createProperty()
    {
        return doCreateProperty();
    }

    /**
     * Construct a TaskModel for the Myrmidon &lt;ant&gt; task, and configure it
     * with sub-class specific values (antfile).
     * @return the TaskModel
     */
    protected DefaultConfiguration buildTaskModel()
    {
        DefaultConfiguration antConfig = new DefaultConfiguration( "ant", "" );

        // Get the "file" value.
        if( antFile == null )
        {
            antFile = "build.xml";
        }

        if( dir == null )
        {
            dir = project.getBaseDir();
        }

        File file = FileUtils.newFileUtils().resolveFile( dir, antFile );
        antFile = file.getAbsolutePath();

        antConfig.setAttribute( "file", antFile );

        return antConfig;
    }
}
