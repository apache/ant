/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.jlink;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

/**
 * This class defines objects that can link together various jar and zip files.
 * <p>
 *
 * It is basically a wrapper for the jlink code written originally by <a
 * href="mailto:beard@netscape.com">Patrick Beard</a> . The classes
 * org.apache.tools.ant.taskdefs.optional.jlink.Jlink and
 * org.apache.tools.ant.taskdefs.optional.jlink.ClassNameReader support this
 * class.</p> <p>
 *
 * For example: <code>
 * <pre>
 * &lt;jlink compress=&quot;false&quot; outfile=&quot;out.jar&quot;/&gt;
 *   &lt;mergefiles&gt;
 *     &lt;pathelement path=&quot;${build.dir}/mergefoo.jar&quot;/&gt;
 *     &lt;pathelement path=&quot;${build.dir}/mergebar.jar&quot;/&gt;
 *   &lt;/mergefiles&gt;
 *   &lt;addfiles&gt;
 *     &lt;pathelement path=&quot;${build.dir}/mac.jar&quot;/&gt;
 *     &lt;pathelement path=&quot;${build.dir}/pc.zip&quot;/&gt;
 *   &lt;/addfiles&gt;
 * &lt;/jlink&gt;
 * </pre> </code>
 *
 * @author <a href="mailto:matthew.k.heun@gaerospace.com">Matthew Kuperus Heun
 *      </a>
 */
public class JlinkTask extends MatchingTask
{

    private File outfile = null;

    private Path mergefiles = null;

    private Path addfiles = null;

    private boolean compress = false;

    private String ps = System.getProperty( "path.separator" );

    /**
     * Sets the files to be added into the output.
     *
     * @param addfiles The new Addfiles value
     */
    public void setAddfiles( Path addfiles )
        throws TaskException
    {
        if( this.addfiles == null )
        {
            this.addfiles = addfiles;
        }
        else
        {
            this.addfiles.append( addfiles );
        }
    }

    /**
     * Defines whether or not the output should be compacted.
     *
     * @param compress The new Compress value
     */
    public void setCompress( boolean compress )
    {
        this.compress = compress;
    }

    /**
     * Sets the files to be merged into the output.
     *
     * @param mergefiles The new Mergefiles value
     */
    public void setMergefiles( Path mergefiles )
        throws TaskException
    {
        if( this.mergefiles == null )
        {
            this.mergefiles = mergefiles;
        }
        else
        {
            this.mergefiles.append( mergefiles );
        }
    }

    /**
     * The output file for this run of jlink. Usually a jar or zip file.
     *
     * @param outfile The new Outfile value
     */
    public void setOutfile( File outfile )
    {
        this.outfile = outfile;
    }

    /**
     * Establishes the object that contains the files to be added to the output.
     *
     * @return Description of the Returned Value
     */
    public Path createAddfiles()
        throws TaskException
    {
        if( this.addfiles == null )
        {
            this.addfiles = new Path( getProject() );
        }
        return this.addfiles.createPath();
    }

    /**
     * Establishes the object that contains the files to be merged into the
     * output.
     *
     * @return Description of the Returned Value
     */
    public Path createMergefiles()
        throws TaskException
    {
        if( this.mergefiles == null )
        {
            this.mergefiles = new Path( getProject() );
        }
        return this.mergefiles.createPath();
    }

    /**
     * Does the adding and merging.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        //Be sure everything has been set.
        if( outfile == null )
        {
            throw new TaskException( "outfile attribute is required! Please set." );
        }
        if( !haveAddFiles() && !haveMergeFiles() )
        {
            throw new TaskException( "addfiles or mergefiles required! Please set." );
        }
        getLogger().info( "linking:     " + outfile.getPath() );
        getLogger().debug( "compression: " + compress );
        jlink linker = new jlink();
        linker.setOutfile( outfile.getPath() );
        linker.setCompression( compress );
        if( haveMergeFiles() )
        {
            getLogger().debug( "merge files: " + mergefiles.toString() );
            linker.addMergeFiles( mergefiles.list() );
        }
        if( haveAddFiles() )
        {
            getLogger().debug( "add files: " + addfiles.toString() );
            linker.addAddFiles( addfiles.list() );
        }
        try
        {
            linker.link();
        }
        catch( Exception ex )
        {
            throw new TaskException( "Error", ex );
        }
    }

    private boolean haveAddFiles()
        throws TaskException
    {
        return haveEntries( addfiles );
    }

    private boolean haveEntries( Path p )
        throws TaskException
    {
        if( p == null )
        {
            return false;
        }
        if( p.size() > 0 )
        {
            return true;
        }
        return false;
    }

    private boolean haveMergeFiles()
        throws TaskException
    {
        return haveEntries( mergefiles );
    }
}

