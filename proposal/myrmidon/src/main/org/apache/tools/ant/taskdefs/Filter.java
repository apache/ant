/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * This task sets a token filter that is used by the file copy methods of the
 * project to do token substitution, or sets mutiple tokens by reading these
 * from a file.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author Gero Vermaas <a href="mailto:gero@xs4all.nl">gero@xs4all.nl</a>
 * @author <A href="gholam@xtra.co.nz">Michael McCallum</A>
 */
public class Filter extends Task
{
    private File filtersFile;

    private String token;
    private String value;

    public void setFiltersfile( File filtersFile )
    {
        this.filtersFile = filtersFile;
    }

    public void setToken( String token )
    {
        this.token = token;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public void execute()
        throws TaskException
    {
        boolean isFiltersFromFile = filtersFile != null && token == null && value == null;
        boolean isSingleFilter = filtersFile == null && token != null && value != null;

        if( !isFiltersFromFile && !isSingleFilter )
        {
            throw new TaskException( "both token and value parameters, or only a filtersFile parameter is required" );
        }

        if( isSingleFilter )
        {
            getProject().getGlobalFilterSet().addFilter( token, value );
        }

        if( isFiltersFromFile )
        {
            readFilters();
        }
    }

    protected void readFilters()
        throws TaskException
    {
        log( "Reading filters from " + filtersFile, Project.MSG_VERBOSE );
        getProject().getGlobalFilterSet().readFiltersFromFile( filtersFile );
    }
}
