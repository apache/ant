/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.cvslib;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.TimeZone;

/**
 * Class used to generate an XML changelog.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class ChangeLogWriter {
    /** output format for dates writtn to xml file */
    private static final SimpleDateFormat c_outputDate 
        = new SimpleDateFormat("yyyy-MM-dd");
    /** output format for times writtn to xml file */
    private static final SimpleDateFormat c_outputTime 
        = new SimpleDateFormat("HH:mm");

    static {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        c_outputDate.setTimeZone(utc);
        c_outputTime.setTimeZone(utc);
    }

    /**
     * Print out the specifed entrys.
     *
     * @param output writer to which to send output.
     * @param entries the entries to be written.
     */
    public void printChangeLog(final PrintWriter output,
                               final CVSEntry[] entries) {
        output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        output.println("<changelog>");
        for (int i = 0; i < entries.length; i++) {
            final CVSEntry entry = entries[i];

            printEntry(output, entry);
        }
        output.println("</changelog>");
        output.flush();
        output.close();
    }


    /**
     * Print out an individual entry in changelog.
     *
     * @param entry the entry to print
     * @param output writer to which to send output.
     */
    private void printEntry(final PrintWriter output, final CVSEntry entry) {
        output.println("\t<entry>");
        output.println("\t\t<date>" + c_outputDate.format(entry.getDate()) 
            + "</date>");
        output.println("\t\t<time>" + c_outputTime.format(entry.getDate()) 
            + "</time>");
        output.println("\t\t<author><![CDATA[" + entry.getAuthor() 
            + "]]></author>");

        final Enumeration enumeration = entry.getFiles().elements();

        while (enumeration.hasMoreElements()) {
            final RCSFile file = (RCSFile) enumeration.nextElement();

            output.println("\t\t<file>");
            output.println("\t\t\t<name>" + file.getName() + "</name>");
            output.println("\t\t\t<revision>" + file.getRevision() 
                + "</revision>");

            final String previousRevision = file.getPreviousRevision();

            if (previousRevision != null) {
                output.println("\t\t\t<prevrevision>" + previousRevision 
                    + "</prevrevision>");
            }

            output.println("\t\t</file>");
        }
        output.println("\t\t<msg><![CDATA[" + entry.getComment() + "]]></msg>");
        output.println("\t</entry>");
    }
}

