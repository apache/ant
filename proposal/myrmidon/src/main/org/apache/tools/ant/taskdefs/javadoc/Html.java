/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.javadoc;

public class Html
{
    private StringBuffer m_text = new StringBuffer();

    public String getText()
    {
        return m_text.toString();
    }

    public void addContent( final String text )
    {
        m_text.append( text );
    }
}
