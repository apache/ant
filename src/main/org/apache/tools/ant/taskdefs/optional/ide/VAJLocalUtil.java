/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.ide;

import java.util.Vector;
import java.util.Enumeration;
import java.io.File;

import com.ibm.ivj.util.base.Package;
import com.ibm.ivj.util.base.IvjException;
import com.ibm.ivj.util.base.Workspace;
import com.ibm.ivj.util.base.ToolEnv;
import com.ibm.ivj.util.base.ExportCodeSpec;
import com.ibm.ivj.util.base.ProjectEdition;
import com.ibm.ivj.util.base.ImportCodeSpec;
import com.ibm.ivj.util.base.Type;
import com.ibm.ivj.util.base.Project;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;


/**
 * Helper class for VAJ tasks. Holds Workspace singleton and
 * wraps IvjExceptions into BuildExceptions
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */
abstract class VAJLocalUtil implements VAJUtil{
    // singleton containing the VAJ workspace
    private static Workspace workspace;

    /**
     * Wraps IvjException into a BuildException
     *
     * @return org.apache.tools.ant.BuildException
     * @param errMsg Additional error message
     * @param e IvjException which is wrapped
     */
    static BuildException createBuildException(
                                               String errMsg, IvjException e) {
        errMsg = errMsg + "\n" + e.getMessage();
        String[] errors = e.getErrors();
        if (errors != null) {
            for (int i = 0; i < errors.length; i++) {
                errMsg = errMsg + "\n" + errors[i];
            }
        }
        return new BuildException(errMsg, e);
    }

    /**
     * returns the current VAJ workspace.
     * @return com.ibm.ivj.util.base.Workspace
     */
    static Workspace getWorkspace() {
        if (workspace == null) {
            workspace = ToolEnv.connectToWorkspace();
            if (workspace == null) {
                throw new BuildException(
                                         "Unable to connect to Workspace! "
                                         + "Make sure you are running in VisualAge for Java.");
            }
        }

        return workspace;
    }


    //-----------------------------------------------------------
    // export
    //-----------------------------------------------------------

    /**
     * export packages
     */
    public void exportPackages(
                               File dest,
                               String[] includePatterns, String[] excludePatterns,
                               boolean exportClasses, boolean exportDebugInfo,
                               boolean exportResources, boolean exportSources,
                               boolean useDefaultExcludes,    boolean overwrite) {
        if (includePatterns == null || includePatterns.length == 0) {
            log("You must specify at least one include attribute. "
                + "Not exporting", MSG_ERR);
        } else {
            try {
                VAJWorkspaceScanner scanner = new VAJWorkspaceScanner();
                scanner.setIncludes(includePatterns);
                scanner.setExcludes(excludePatterns);
                if (useDefaultExcludes) {
                    scanner.addDefaultExcludes();
                }
                scanner.scan();

                Package[] packages = scanner.getIncludedPackages();

                log("Exporting " + packages.length + " package(s) to "
                    + dest, MSG_INFO);
                for (int i = 0; i < packages.length; i++) {
                    log("    " + packages[i].getName(), MSG_VERBOSE);
                }

                ExportCodeSpec exportSpec = new ExportCodeSpec();
                exportSpec.setPackages(packages);
                exportSpec.includeJava(exportSources);
                exportSpec.includeClass(exportClasses);
                exportSpec.includeResources(exportResources);
                exportSpec.includeClassDebugInfo(exportDebugInfo);
                exportSpec.useSubdirectories(true);
                exportSpec.overwriteFiles(overwrite);
                exportSpec.setExportDirectory(dest.getAbsolutePath());

                getWorkspace().exportData(exportSpec);
            } catch (IvjException ex) {
                throw createBuildException("Exporting failed!", ex);
            }
        }
    }


    //-----------------------------------------------------------
    // load
    //-----------------------------------------------------------

    /**
     * Load specified projects.
     */
    public void loadProjects(Vector projectDescriptions) {
        Vector expandedDescs = getExpandedDescriptions(projectDescriptions);

        // output warnings for projects not found
        for (Enumeration e = projectDescriptions.elements(); e.hasMoreElements();) {
            VAJProjectDescription d = (VAJProjectDescription) e.nextElement();
            if (!d.projectFound()) {
                log("No Projects match the name " + d.getName(), MSG_WARN);
            }
        }

        log("Loading " + expandedDescs.size()
            + " project(s) into workspace", MSG_INFO);

        for (Enumeration e = expandedDescs.elements();
             e.hasMoreElements();) {
            VAJProjectDescription d = (VAJProjectDescription) e.nextElement();

            ProjectEdition pe = findProjectEdition(d.getName(), d.getVersion());
            try {
                log("Loading '" + d.getName() + "', Version '" + d.getVersion()
                    + "', into Workspace", MSG_VERBOSE);
                pe.loadIntoWorkspace();
            } catch (IvjException ex) {
                throw createBuildException("Project '" + d.getName()
                                            + "' could not be loaded.", ex);
            }
        }
    }

    /**
     * returns a list of project names matching the given pattern
     */
    private Vector findMatchingProjects(String pattern) {
        String[] projectNames;
        try {
            projectNames = getWorkspace().getRepository().getProjectNames();
        } catch (IvjException e) {
            throw createBuildException("VA Exception occured: ", e);
        }

        Vector matchingProjects = new Vector();
        for (int i = 0; i < projectNames.length; i++) {
            if (VAJWorkspaceScanner.match(pattern, projectNames[i])) {
                matchingProjects.addElement(projectNames[i]);
            }
        }

        return matchingProjects;
    }


    /**
     * return project descriptions containing full project names instead
     * of patterns with wildcards.
     */
    private Vector getExpandedDescriptions(Vector projectDescs) {
        Vector expandedDescs = new Vector(projectDescs.size());
        try {
            String[] projectNames =
                getWorkspace().getRepository().getProjectNames();
            for (int i = 0; i < projectNames.length; i++) {
                for (Enumeration e = projectDescs.elements();
                     e.hasMoreElements();) {
                    VAJProjectDescription d = (VAJProjectDescription) e.nextElement();
                    String pattern = d.getName();
                    if (VAJWorkspaceScanner.match(pattern, projectNames[i])) {
                        d.setProjectFound();
                        expandedDescs.addElement(new VAJProjectDescription(
                                                                           projectNames[i], d.getVersion()));
                        break;
                    }
                }
            }
        } catch (IvjException e) {
            throw createBuildException("VA Exception occured: ", e);
        }

        return expandedDescs;
    }

    /**
     * Finds a specific project edition in the repository.
     *
     * @param name project name
     * @param versionName project version name
     * @return com.ibm.ivj.util.base.ProjectEdition the specified edition
     */
    private ProjectEdition findProjectEdition(
                                              String name, String versionName) {
        try {
            ProjectEdition[] editions = null;
            editions = getWorkspace().getRepository().getProjectEditions(name);

            if (editions == null) {
                throw new BuildException("Project " + name + " doesn't exist");
            }

            ProjectEdition pe = null;
            for (int i = 0; i < editions.length && pe == null; i++) {
                if (versionName.equals(editions[i].getVersionName())) {
                    pe = editions[i];
                }
            }
            if (pe == null) {
                throw new BuildException("Version " + versionName
                    + " of Project " + name + " doesn't exist");
            }
            return pe;

        } catch (IvjException e) {
            throw createBuildException("VA Exception occured: ", e);
        }

    }



    //-----------------------------------------------------------
    // import
    //-----------------------------------------------------------


    /**
     * Do the import.
     */
    public void importFiles(
                            String importProject, File srcDir,
                            String[] includePatterns, String[] excludePatterns,
                            boolean importClasses, boolean importResources,
                            boolean importSources, boolean useDefaultExcludes)
        throws BuildException {

        if (importProject == null || "".equals(importProject)) {
            throw new BuildException("The VisualAge for Java project "
                                     + "name is required!");
        }

        ImportCodeSpec importSpec = new ImportCodeSpec();
        importSpec.setDefaultProject(getVAJProject(importProject));

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(srcDir);
        ds.setIncludes(includePatterns);
        ds.setExcludes(excludePatterns);
        if (useDefaultExcludes) {
            ds.addDefaultExcludes();
        }
        ds.scan();

        Vector classes = new Vector();
        Vector sources = new Vector();
        Vector resources = new Vector();

        scanForImport(srcDir, ds.getIncludedFiles(), classes, sources, resources);

        StringBuffer summaryLog = new StringBuffer("Importing ");
        addFilesToImport(importSpec, importClasses, classes, "Class", summaryLog);
        addFilesToImport(importSpec, importSources, sources, "Java", summaryLog);
        addFilesToImport(importSpec, importResources, resources, "Resource", summaryLog);
        importSpec.setResourcePath(srcDir.getAbsolutePath());

        summaryLog.append(" into the project '");
        summaryLog.append(importProject);
        summaryLog.append("'.");

        log(summaryLog.toString(), MSG_INFO);

        try {
            Type[] importedTypes = getWorkspace().importData(importSpec);
            if (importedTypes == null) {
                throw new BuildException("Unable to import into Workspace!");
            } else {
                log(importedTypes.length + " types imported", MSG_DEBUG);
                for (int i = 0; i < importedTypes.length; i++) {
                    log(importedTypes[i].getPackage().getName()
                        + "." + importedTypes[i].getName()
                        + " into " + importedTypes[i].getProject().getName(),
                        MSG_DEBUG);
                }
            }
        } catch (IvjException ivje) {
            throw createBuildException("Error while importing into workspace: ",
                                       ivje);
        }
    }

    /**
     * get a project from the Workspace.
     */
    static Project getVAJProject(String importProject) {
        Project found = null;
        Project[] currentProjects = getWorkspace().getProjects();

        for (int i = 0; i < currentProjects.length; i++) {
            Project p = currentProjects[i];
            if (p.getName().equals(importProject)) {
                found = p;
                break;
            }
        }


        if (found == null) {
            try {
                found = getWorkspace().createProject(importProject, true);
            } catch (IvjException e) {
                throw createBuildException("Error while creating Project "
                                           + importProject + ": ", e);
            }
        }

        return found;
    }


    /**
     * Sort the files into classes, sources, and resources.
     */
    private void scanForImport(
                               File dir,
                               String[] files,
                               Vector classes,
                               Vector sources,
                               Vector resources) {
        for (int i = 0; i < files.length; i++) {
            String file = (new File(dir, files[i])).getAbsolutePath();
            if (file.endsWith(".java") || file.endsWith(".JAVA")) {
                sources.addElement(file);
            } else
                if (file.endsWith(".class") || file.endsWith(".CLASS")) {
                    classes.addElement(file);
                } else {
                    // for resources VA expects the path relative to the resource path
                    resources.addElement(files[i]);
                }
        }
    }

    /**
     * Adds files to an import specification. Helper method
     * for importFiles()
     *
     * @param spec       import specification
     * @param doImport   only add files if doImport is true
     * @param files      the files to add
     * @param fileType   type of files (Source/Class/Resource)
     * @param summaryLog buffer for logging
     */
    private void addFilesToImport(ImportCodeSpec spec, boolean doImport,
                                  Vector files, String fileType,
                                  StringBuffer summaryLog) {

        if (doImport) {
            String[] fileArr = new String[files.size()];
            files.copyInto(fileArr);
            try {
                // here it is assumed that fileType is one of the
                // following strings: // "Java", "Class", "Resource"
                String methodName = "set" + fileType + "Files";
                Class[] methodParams = new Class[]{fileArr.getClass()};
                java.lang.reflect.Method method =
                    spec.getClass().getDeclaredMethod(methodName, methodParams);
                method.invoke(spec, new Object[]{fileArr});
            } catch (Exception e) {
                throw new BuildException(e);
            }
            if (files.size() > 0) {
                logFiles(files, fileType);
                summaryLog.append(files.size());
                summaryLog.append(" " + fileType.toLowerCase() + " file");
                summaryLog.append(files.size() > 1 ? "s, " : ", ");
            }
        }
    }

    /**
     * Logs a list of file names to the message log
     * @param fileNames java.util.Vector file names to be logged
     * @param type java.lang.String file type
     */
    private void logFiles(Vector fileNames, String fileType) {
        log(fileType + " files found for import:", MSG_VERBOSE);
        for (Enumeration e = fileNames.elements(); e.hasMoreElements();) {
            log("    " + e.nextElement(), MSG_VERBOSE);
        }
    }
}
