/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.antlib.xml;

import java.io.File;

/**
 * Proxy interface for XSLT processors.
 *
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 * @see XSLTProcess
 */
public interface XSLTLiaison
{
    /**
     * the file protocol prefix for systemid. This file protocol must be
     * appended to an absolute path. Typically: <tt>FILE_PROTOCOL_PREFIX +
     * file.getAbsolutePath()</tt> This is not correct in specification terms
     * since an absolute url in Unix is file:// + file.getAbsolutePath() while
     * it is file:/// + file.getAbsolutePath() under Windows. Whatever, it
     * should not be a problem to put file:/// in every case since most parsers
     * for now incorrectly makes no difference between it.. and users also have
     * problem with that :)
     */
    String FILE_PROTOCOL_PREFIX = "file:///";

    /**
     * set the stylesheet to use for the transformation.
     *
     * @param stylesheet the stylesheet to be used for transformation.
     * @exception Exception Description of Exception
     */
    void setStylesheet( File stylesheet )
        throws Exception;

    /**
     * Add a parameter to be set during the XSL transformation.
     *
     * @param name the parameter name.
     * @param expression the parameter value as an expression string.
     * @throws Exception thrown if any problems happens.
     */
    void addParam( String name, String expression )
        throws Exception;

    /**
     * set the output type to use for the transformation. Only "xml" (the
     * default) is guaranteed to work for all parsers. Xalan2 also supports
     * "html" and "text".
     *
     * @param type the output method to use
     * @exception Exception Description of Exception
     */
    void setOutputtype( String type )
        throws Exception;

    /**
     * Perform the transformation of a file into another.
     *
     * @param infile the input file, probably an XML one. :-)
     * @param outfile the output file resulting from the transformation
     * @see #setStylesheet(File)
     * @throws Exception thrown if any problems happens.
     */
    void transform( File infile, File outfile )
        throws Exception;

}//-- XSLTLiaison
