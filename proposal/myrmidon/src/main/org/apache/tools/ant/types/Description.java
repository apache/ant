/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;

/**
 * Description is used to provide a project-wide description element (that is, a
 * description that applies to a buildfile as a whole). If present, the
 * &lt;description&gt; element is printed out before the target descriptions.
 * Description has no attributes, only text. There can only be one project
 * description per project. A second description element will overwrite the
 * first.
 *
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Revision$ $Date$
 */
public class Description extends DataType
{

    /**
     * Adds descriptive text to the project.
     *
     * @param text The feature to be added to the Text attribute
     */
    public void addText( String text )
    {
        String currentDescription = project.getDescription();
        if( currentDescription == null )
        {
            project.setDescription( text );
        }
        else
        {
            project.setDescription( currentDescription + text );
        }
    }
}
