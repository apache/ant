/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;

/**
 * Class used to generate an XML changelog.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class ChangeLogWriter
{
    /** output format for dates writtn to xml file */
    private static final SimpleDateFormat c_outputDate = new SimpleDateFormat( "yyyy-MM-dd" );
    /** output format for times writtn to xml file */
    private static final SimpleDateFormat c_outputTime = new SimpleDateFormat( "hh:mm" );

    /**
     * Print out the specifed entrys.
     */
    public void printChangeLog( final PrintWriter output,
                                final CVSEntry[] entries )
    {
        output.println( "<changelog>" );
        for( int i = 0; i < entries.length; i++ )
        {
            final CVSEntry entry = entries[ i ];
            printEntry( output, entry );
        }
        output.println( "</changelog>" );
        output.flush();
        output.close();
    }

    /**
     * Print out an individual entry in changelog.
     *
     * @param entry the entry to print
     */
    private void printEntry( final PrintWriter output, final CVSEntry entry )
    {
        output.println( "\t<entry>" );
        output.println( "\t\t<date>" + c_outputDate.format( entry.getDate() ) + "</date>" );
        output.println( "\t\t<time>" + c_outputTime.format( entry.getDate() ) + "</time>" );
        output.println( "\t\t<author><![CDATA[" + entry.getAuthor() + "]]></author>" );

        final Iterator iterator = entry.getFiles().iterator();
        while( iterator.hasNext() )
        {
            final RCSFile file = (RCSFile)iterator.next();
            output.println( "\t\t<file>" );
            output.println( "\t\t\t<name>" + file.getName() + "</name>" );
            output.println( "\t\t\t<revision>" + file.getRevision() + "</revision>" );

            final String previousRevision = file.getPreviousRevision();
            if( previousRevision != null )
            {
                output.println( "\t\t\t<prevrevision>" + previousRevision + "</prevrevision>" );
            }

            output.println( "\t\t</file>" );
        }
        output.println( "\t\t<msg><![CDATA[" + entry.getComment() + "]]></msg>" );
        output.println( "\t</entry>" );
    }
}
