/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface is used to access the data content of a file.
 *
 * <p>To read from a file, use the {@link #getInputStream} method.
 *
 * <p>To write to a file, use the {@link #getOutputStream} method.  This
 * method will create the file and the parent folder, if necessary.
 *
 * <p>To prevent concurrency problems, only a single <code>InputStream</code>,
 * or <code>OutputStream</code> may be open at any time, for each file.
 *
 * <p>TODO - allow multiple input streams?
 *
 * @see FileObject#getContent
 *
 * @author Adam Murdoch
 */
public interface FileContent
{
    /**
     * Returns the file which this is the content of.
     */
    FileObject getFile();

    /**
     * Determines the size of the file, in bytes.
     *
     * @return
     *      The size of the file, in bytes.
     *
     * @throws FileSystemException
     *      If the file does not exist, or is being written to, or on error
     *      determining the size.
     */
    long getSize() throws FileSystemException;

    /**
     * Determines the last-modified timestamp of the file.
     *
     * @return
     *      The last-modified timestamp.
     *
     * @throws FileSystemException
     *      If the file does not exist, or is being written to, or on error
     *      determining the last-modified timestamp.
     */
    long getLastModifiedTime() throws FileSystemException;

    /**
     * Sets the last-modified timestamp of the file.  Creates the file if
     * it does not exist.
     *
     * @param modTime
     *      The time to set the last-modified timestamp to.
     *
     * @throws FileSystemException
     *      If the file is read-only, or is being read, or on error setting
     *      the last-modified timestamp.
     */
    void setLastModifiedTime( long modTime ) throws FileSystemException;

    /**
     * Gets the value of an attribute of the file's content.
     *
     * <p>TODO - change to <code>Map getAttributes()</code> instead?
     *
     * <p>TODO - define the standard attribute names, and define which attrs
     * are guaranteed to be present.
     *
     * @param attrName
     *      The name of the attribute.
     *
     * @return
     *      The value of the attribute.
     *
     * @throws FileSystemException
     *      If the file does not exist, or is being written, or if the
     *      attribute is unknown.
     */
    Object getAttribute( String attrName ) throws FileSystemException;

    /**
     * Sets the value of an attribute of the file's content.  Creates the
     * file if it does not exist.
     *
     * @param attrName
     *      The name of the attribute.
     *
     * @param value
     *      The value of the attribute.
     *
     * @throws FileSystemException
     *      If the file is read-only, or is being read, or if the attribute
     *      is not supported, or on error setting the attribute.
     */
    void setAttribute( String attrName, Object value ) throws FileSystemException;

    /**
     * Returns an input stream for reading the file's content.
     *
     * <p>There may only be a single input or output stream open for the
     * file at any time.
     *
     * @return
     *      An input stream to read the file's content from.  The input
     *      stream is buffered, so there is no need to wrap it in a
     *      <code>BufferedInputStream</code>.
     *
     * @throws FileSystemException
     *      If the file does not exist, or is being read, or is being written,
     *      or on error opening the stream.
     */
    InputStream getInputStream() throws FileSystemException;

    /**
     * Returns an output stream for writing the file's content.
     *
     * If the file does not exist, this method creates it, and the parent
     * folder, if necessary.  If the file does exist, it is replaced with
     * whatever is written to the output stream.
     *
     * <p>There may only be a single input or output stream open for the
     * file at any time.
     *
     * @return
     *      An output stream to write the file's content to.  The stream is
     *      buffered, so there is no need to wrap it in a
     *      <code>BufferedOutputStream</code>.
     *
     * @throws FileSystemException
     *      If the file is read-only, or is being read, or is being written,
     *      or on error opening the stream.
     */
    OutputStream getOutputStream() throws FileSystemException;

    /**
     * Closes all resources used by the content, including any open stream.
     * Commits pending changes to the file.
     *
     * <p>This method is a hint to the implementation that it can release
     * resources.  This object can continue to be used after calling this
     * method.
     */
    void close() throws FileSystemException;
}
