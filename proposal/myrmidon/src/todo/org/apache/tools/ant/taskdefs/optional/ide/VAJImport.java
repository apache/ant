/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;

/**
 * Import source, class files, and resources to the Visual Age for Java
 * workspace using FileSets. <p>
 *
 * Example: <pre>
 * &lt;vajimport project="MyVAProject"&gt;
 *   &lt;fileset dir="src"&gt;
 *     &lt;include name="org/foo/subsystem1/**" /&gt;
 *     &lt;exclude name="/org/foo/subsystem1/test/**" /&gt;
 *  &lt;/fileset&gt;
 * &lt;/vajexport&gt;
 * </pre> import all source and resource files from the "src" directory which
 * start with 'org.foo.subsystem1', except of these starting with
 * 'org.foo.subsystem1.test' into the project MyVAProject. </p> <p>
 *
 * If MyVAProject isn't loaded into the Workspace, a new edition is created in
 * the repository and automatically loaded into the Workspace. There has to be
 * at least one nested FileSet element. </p> <p>
 *
 * There are attributes to choose which items to export:
 * <tableborder="1" cellpadding="2" cellspacing="0">
 *
 *   <tr>
 *
 *     <tdvalign="top">
 *       <b>Attribute</b>
 *     </td>
 *
 *     <tdvalign="top">
 *       <b>Description</b>
 *     </td>
 *
 *     <tdalign="center" valign="top">
 *       <b>Required</b>
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <tdvalign="top">
 *       project
 *     </td>
 *
 *     <tdvalign="top">
 *       the name of the Project to import to
 *     </td>
 *
 *     <tdalign="center" valign="top">
 *       Yes
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <tdvalign="top">
 *       importSources
 *     </td>
 *
 *     <tdvalign="top">
 *       import Java sources, defaults to "yes"
 *     </td>
 *
 *     <tdalign="center" valign="top">
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <tdvalign="top">
 *       importResources
 *     </td>
 *
 *     <tdvalign="top">
 *       import resource files (anything that doesn't end with .java or .class),
 *       defaults to "yes"
 *     </td>
 *
 *     <tdalign="center" valign="top">
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <tdvalign="top">
 *       importClasses
 *     </td>
 *
 *     <tdvalign="top">
 *       import class files, defaults to "no"
 *     </td>
 *
 *     <tdalign="center" valign="top">
 *       No
 *     </td>
 *
 *   </tr>
 *
 * </table>
 *
 *
 * @author RT
 * @author: Glenn McAllister, inspired by a similar task written by Peter Kelley
 */
public class VAJImport extends VAJTask
{
    protected ArrayList filesets = new ArrayList();
    protected boolean importSources = true;
    protected boolean importResources = true;
    protected boolean importClasses = false;
    protected String importProject = null;
    protected boolean useDefaultExcludes = true;

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
     * Import .class files.
     *
     * @param importClasses The new ImportClasses value
     */
    public void setImportClasses( boolean importClasses )
    {
        this.importClasses = importClasses;
    }

    /**
     * Import resource files (anything that doesn't end in .class or .java)
     *
     * @param importResources The new ImportResources value
     */
    public void setImportResources( boolean importResources )
    {
        this.importResources = importResources;
    }

    /**
     * Import .java files
     *
     * @param importSources The new ImportSources value
     */
    public void setImportSources( boolean importSources )
    {
        this.importSources = importSources;
    }

    /**
     * The VisualAge for Java Project name to import into.
     *
     * @param projectName The new Project value
     */
    public void setProject( String projectName )
    {
        this.importProject = projectName;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.add( set );
    }

    /**
     * Do the import.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( filesets.size() == 0 )
        {
            throw new TaskException( "At least one fileset is required!" );
        }

        if( importProject == null || "".equals( importProject ) )
        {
            throw new TaskException( "The VisualAge for Java Project name is required!" );
        }

        for( Iterator e = filesets.iterator(); e.hasNext(); )
        {
            importFileset( (FileSet)e.next() );
        }
    }

    /**
     * Import all files from the fileset into the Project in the Workspace.
     *
     * @param fileset Description of Parameter
     */
    protected void importFileset( FileSet fileset )
    {
        DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fileset );
        if( ds.getIncludedFiles().length == 0 )
        {
            return;
        }

        String[] includes = null;
        String[] excludes = null;

        // Hack to get includes and excludes. We could also use getIncludedFiles,
        // but that would result in very long HTTP-requests.
        // Therefore we want to send the patterns only to the remote tool server
        // and let him figure out the files.
        try
        {
            Class directoryScanner = ds.getClass();

            Field includesField = directoryScanner.getDeclaredField( "includes" );
            includesField.setAccessible( true );
            includes = (String[])includesField.get( ds );

            Field excludesField = directoryScanner.getDeclaredField( "excludes" );
            excludesField.setAccessible( true );
            excludes = (String[])excludesField.get( ds );
        }
        catch( NoSuchFieldException nsfe )
        {
            throw new TaskException(
                "DirectoryScanner.includes or .excludes missing" + nsfe.getMessage() );
        }
        catch( IllegalAccessException iae )
        {
            throw new TaskException(
                "Access to DirectoryScanner.includes or .excludes not allowed" );
        }

        getUtil().importFiles( importProject, ds.getBasedir(),
                               includes, excludes,
                               importClasses, importResources, importSources,
                               useDefaultExcludes );
    }
}
