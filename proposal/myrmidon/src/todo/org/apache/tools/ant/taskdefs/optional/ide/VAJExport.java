/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Pattern;
import org.apache.myrmidon.framework.PatternSet;

/**
 * Export packages from the Visual Age for Java workspace. The packages are
 * specified similar to all other MatchingTasks. Since the VA Workspace is not
 * file based, this task is simulating a directory hierarchy for the workspace:
 * The 'root' contains all project 'dir's, and the projects contain their
 * respective package 'dir's. Example: <blockquote> &lt;vajexport
 * destdir="C:/builddir/source"> &nbsp;&lt;include
 * name="/MyVAProject/org/foo/subsystem1/**" /> &nbsp;&lt;exclude
 * name="/MyVAProject/org/foo/subsystem1/test/**"/> &lt;/vajexport>
 * </blockquote> exports all packages in the project MyVAProject which start
 * with 'org.foo.subsystem1' except of these starting with
 * 'org.foo.subsystem1.test'. There are flags to choose which items to export:
 * exportSources: export Java sources exportResources: export project resources
 * exportClasses: export class files exportDebugInfo: export class files with
 * debug info (use with exportClasses) default is exporting Java files and
 * resources.
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */

public class VAJExport extends VAJTask
{
    protected boolean exportSources = true;
    protected boolean exportResources = true;
    protected boolean exportClasses = false;
    protected boolean exportDebugInfo = false;
    protected boolean useDefaultExcludes = true;
    protected boolean overwrite = true;

    protected PatternSet patternSet = new PatternSet();
    //set set... method comments for description
    protected File destDir;

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *      should be used, "false"|"off"|"no" when they shouldn't be used.
     */
    public void setDefaultexcludes( boolean useDefaultExcludes )
    {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Set the destination directory into which the selected items should be
     * exported
     *
     * @param destDir The new Destdir value
     */
    public void setDestdir( File destDir )
    {
        this.destDir = destDir;
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space. Currently only patterns denoting packages are supported
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( String excludes )
    {
        patternSet.setExcludes( excludes );
    }

    /**
     * if exportClasses is set, class files are exported
     *
     * @param doExport The new ExportClasses value
     */
    public void setExportClasses( boolean doExport )
    {
        exportClasses = doExport;
    }

    /**
     * if exportDebugInfo is set, the exported class files contain debug info
     *
     * @param doExport The new ExportDebugInfo value
     */
    public void setExportDebugInfo( boolean doExport )
    {
        exportDebugInfo = doExport;
    }

    /**
     * if exportResources is set, resource file will be exported
     *
     * @param doExport The new ExportResources value
     */
    public void setExportResources( boolean doExport )
    {
        exportResources = doExport;
    }

    /**
     * if exportSources is set, java files will be exported
     *
     * @param doExport The new ExportSources value
     */
    public void setExportSources( boolean doExport )
    {
        exportSources = doExport;
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.Currently only patterns denoting packages are supported
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( String includes )
    {
        patternSet.setIncludes( includes );
    }

    /**
     * if Overwrite is set, files will be overwritten during export
     *
     * @param doOverwrite The new Overwrite value
     */
    public void setOverwrite( boolean doOverwrite )
    {
        overwrite = doOverwrite;
    }

    /**
     * add a name entry on the exclude list
     *
     * @return Description of the Returned Value
     */
    public void addExclude( final Pattern pattern )
    {
        patternSet.addExclude( pattern );
    }

    /**
     * add a name entry on the include list
     */
    public void addInclude( final Pattern pattern )
    {
        patternSet.addInclude( pattern );
    }

    /**
     * do the export
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        // first off, make sure that we've got a destdir
        if( destDir == null )
        {
            throw new TaskException( "destdir attribute must be set!" );
        }

        // delegate the export to the VAJUtil object.
        getUtil().exportPackages( destDir,
                                  patternSet.getIncludePatterns( getContext() ),
                                  patternSet.getExcludePatterns( getContext() ),
                                  exportClasses, exportDebugInfo,
                                  exportResources, exportSources,
                                  useDefaultExcludes, overwrite );
    }

}
