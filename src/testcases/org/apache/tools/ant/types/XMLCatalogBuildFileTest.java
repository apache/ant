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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * BuildFileTest testcases for org.apache.tools.ant.types.XMLCatalog
 * 
 * @see org.apache.tools.ant.types.XMLCatalogTest
 *
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a> 
 * @version $Id$
 */
public class XMLCatalogBuildFileTest extends BuildFileTest {

    public XMLCatalogBuildFileTest(String name) {
        super(name);
    }

    public void setUp() { 
    }

    public void tearDown() {
    }

    //
    // Ensure that an external entity resolves as expected with NO
    // XMLCatalog involvement:
    //
    // Transform an XML file that refers to the entity into a text
    // file, stuff result into property: val1
    //
    public void testEntityNoCatalog() { 
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testentitynocatalog", "val1", 
                          "A stitch in time saves nine");
    }

    //
    // Ensure that an external entity resolves as expected Using an
    // XMLCatalog:
    //
    // Transform an XML file that refers to the entity into a text
    // file, entity is listed in the XMLCatalog pointing to a
    // different file.  Stuff result into property: val2
    //
    /*
    public void testEntityWithCatalog() { 
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testentitywithcatalog", "val2", 
                          "No news is good news");
    }
    */

    //
    // Ensure that an external entity resolves as expected with NO
    // XMLCatalog involvement:
    //
    // Transform an XML file that contains a reference to a _second_ XML file
    // via the document() function.  The _second_ XML file refers to an entity.
    // Stuff result into the property: val3
    // 
    public void testDocumentNoCatalog() { 
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testdocumentnocatalog", "val3", 
                          "A stitch in time saves nine");
    }

    //
    // Ensure that an external entity resolves as expected Using an
    // XMLCatalog:
    //
    // Transform an XML file that contains a reference to a _second_ XML file
    // via the document() function.  The _second_ XML file refers to an entity.
    // The entity is listed in the XMLCatalog pointing to a different file.
    // Stuff result into the property: val4
    // 
    /*
    public void testDocumentWithCatalog() { 
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testdocumentwithcatalog", "val4", 
                          "No news is good news");
    }
    */
}
