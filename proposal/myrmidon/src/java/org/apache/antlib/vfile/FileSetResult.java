/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.vfs.FileObject;

/**
 * The contents of a {@link FileSet}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public interface FileSetResult
{
    /**
     * Returns the files in the result.
     */
    FileObject[] getFiles();

    /**
     * Returns the virtual paths of the files.
     */
    String[] getPaths();
}
