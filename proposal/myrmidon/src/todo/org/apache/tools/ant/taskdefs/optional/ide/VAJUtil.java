/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;
import java.io.File;
import java.util.Vector;

/**
 * Helper interface for VAJ tasks. Encapsulates the interface to the VAJ tool
 * API.
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */
interface VAJUtil
{
    // log levels
    public final static int MSG_DEBUG = 4;
    public final static int MSG_ERR = 0;
    public final static int MSG_INFO = 2;
    public final static int MSG_VERBOSE = 3;
    public final static int MSG_WARN = 1;

    /**
     * export the array of Packages
     *
     * @param dest Description of Parameter
     * @param includePatterns Description of Parameter
     * @param excludePatterns Description of Parameter
     * @param exportClasses Description of Parameter
     * @param exportDebugInfo Description of Parameter
     * @param exportResources Description of Parameter
     * @param exportSources Description of Parameter
     * @param useDefaultExcludes Description of Parameter
     * @param overwrite Description of Parameter
     */
    void exportPackages(
                         File dest,
                         String[] includePatterns, String[] excludePatterns,
                         boolean exportClasses, boolean exportDebugInfo,
                         boolean exportResources, boolean exportSources,
                         boolean useDefaultExcludes, boolean overwrite );

    /**
     * Do the import.
     *
     * @param importProject Description of Parameter
     * @param srcDir Description of Parameter
     * @param includePatterns Description of Parameter
     * @param excludePatterns Description of Parameter
     * @param importClasses Description of Parameter
     * @param importResources Description of Parameter
     * @param importSources Description of Parameter
     * @param useDefaultExcludes Description of Parameter
     */
    void importFiles(
                      String importProject, File srcDir,
                      String[] includePatterns, String[] excludePatterns,
                      boolean importClasses, boolean importResources,
                      boolean importSources, boolean useDefaultExcludes );

    /**
     * Load specified projects.
     *
     * @param projectDescriptions Description of Parameter
     */
    void loadProjects( Vector projectDescriptions );

    /**
     * Logs a message with the specified log level.
     *
     * @param msg Description of Parameter
     * @param level Description of Parameter
     */
    void log( String msg, int level );
}
