/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

import java.util.*;

/**
 *  Used by a workspace to read project files.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public interface Importer {
    public void importProject(Project project) throws BuildException;
}