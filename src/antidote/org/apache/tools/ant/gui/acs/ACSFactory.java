/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui.acs;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;
import org.w3c.dom.*;
import com.sun.xml.parser.Parser;
import com.sun.xml.tree.SimpleElementFactory;
import com.sun.xml.tree.XmlDocument;
import com.sun.xml.tree.XmlDocumentBuilder;
import java.util.Properties;
import com.sun.xml.parser.Resolver;

/**
 * Factory for loading Ant Construction set elements.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ACSFactory {
    /** Singleton instance of the factory. */
    private static ACSFactory _instance = null;

    /** Element maping. */
    private static final Properties _elementMap = new Properties();

    static {
        try {
            _elementMap.load(ACSFactory.class.
                             getResourceAsStream("acs-element.properties"));
        }
        catch(Throwable ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

	/** 
	 * Default ctor.
	 * 
	 */
    private ACSFactory() {

    }

	/** 
	 * Load a project from the given XML file.
     * XXX fix me.
	 * 
	 * @param f File to load.
	 * @return 
	 */
    public ACSProjectElement load(File f) throws IOException {
        XmlDocument doc = null;

        try {
            SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
            Parser parser = (Parser) sax.getParser();
            XmlDocumentBuilder builder = new XmlDocumentBuilder();
            builder.setIgnoringLexicalInfo(false);
            SimpleElementFactory fact = new SimpleElementFactory();
            fact.addMapping(_elementMap, ACSFactory.class.getClassLoader());

            builder.setElementFactory(fact);
            
            parser.setDocumentHandler(builder);
            parser.setEntityResolver(new Resolver());
            //parser.setErrorHandler();

            sax.parse(f, null);

            doc = builder.getDocument();
        }
        catch(Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }

        return (ACSProjectElement) doc.getDocumentElement();
    }

	/** 
	 * Get an instance of the factory.
	 * 
	 * @return Factory instance.
	 */
    public static ACSFactory getInstance() {
        if(_instance == null) {
            _instance = new ACSFactory();
        }
        return _instance;
    }


	/** 
	 * Test code
	 * 
	 * @param args  XML file to parse.
	 */
    public static void main(String[] args) {
        try {
            ACSFactory f = ACSFactory.getInstance();

            System.out.println(f.load(new File(args[0])));
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}
