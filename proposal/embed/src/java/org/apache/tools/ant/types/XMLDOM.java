/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import java.io.*;
import java.util.*;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Loads an xml file as DOM in a DataType
 *
 * @author Nicola Ken Barozzi nicolaken@apache.org
 */
public class XMLDOM extends DataType {
    /** The name of this data type */
    public static final String DATA_TYPE_NAME = "xmldom";

    private File xmlfile = null;
    private Document docRoot = null;

    public XMLDOM() {
    }

    public void setFile(File xmlfile) {
        this.xmlfile = xmlfile;
    }

    /***
     * Gets the Document root of the DOM 
     * given project.
     */
    public Document getRoot() {
      if(docRoot==null){
        try{
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          //FIXME eventually set DocumentBuilderFactory properties in the future 
          DocumentBuilder builder = factory.newDocumentBuilder();
          //FIXME eventually set DocumentBuilder properties in the future 
          this.docRoot = builder.parse(xmlfile);
        }
        catch(ParserConfigurationException pce){
          throw new BuildException("Error in the configuration of the parser", pce);
        }
        catch(SAXException se){
          throw new BuildException("Error during parsing", se);
        }
        catch(IOException ioe){
          throw new BuildException("Can't load the specified file", ioe);
        }
      }      
      
      return docRoot;
    }


    /***
     * Get the RegularExpression this reference refers to in
     * the given project.  Check for circular references too
     */
/*
    public Substitution getRef(Project p) {
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }

        
        Object o = getRefid().getReferencedObject(p);
        if (!(o instanceof Substitution)) {
            String msg = getRefid().getRefId() + " doesn\'t denote a substitution";
            throw new BuildException(msg);
        } else {
            return (Substitution) o;
        }
    }
*/
} //-- XMLDOM.java
