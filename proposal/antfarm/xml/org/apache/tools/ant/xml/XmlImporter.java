/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.xml;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.tools.ant.*;
import org.xml.sax.*;

/**
 *  This class knows how to locate xml project files
 *  and import them into the workspace.
 */
public class XmlImporter implements Importer {
    private URL[] path;

    /**
     *  Constructs an importer for a workspace.
     */
    public XmlImporter() {
        this.path = getProjectPath();
    }

    /**
     *  Imports the project with the specified name.
     */
    public void importProject(Project project) throws BuildException {
        // Locate the project file
        URLConnection conn = findProjectFile(project);

        // Parse the xml
        parseProjectFile(project, conn);
    }

    /**
     *  Find the .ant file for this project. Searches each directory and
     *  jar in the project path.
     */
    private URLConnection findProjectFile(Project project) throws BuildException {
        String fileName = project.getName() + ".ant";
        for (int i = 0; i < path.length; i++) {
            try {
                URL url = new URL(path[i], fileName);
                URLConnection conn = url.openConnection();
                conn.connect();
                project.setBase(path[i]);
                project.setLocation(url.toString());
                return conn;
            }
            catch(FileNotFoundException exc) {
                // The file ins't in this directory/jar, keep looking
            }
            catch(IOException exc) {
                // Not sure what to do here...
                exc.printStackTrace();
            }
        }

        throw new BuildException("Project \"" + project.getName() + "\" not found");
    }

    /**
     *  Parse the xml file.
     */
    private void parseProjectFile(Project project, URLConnection conn) throws BuildException {
        ProjectHandler handler = new ProjectHandler(project);

        try {
            InputSource source = new InputSource(conn.getInputStream());
            source.setPublicId(conn.getURL().toString());
            SAXParser parser = parserFactory.newSAXParser();
            /* parser.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", handler);  */
            parser.parse(source, handler);
        }
        catch(SAXParseException exc) {
            if (exc.getException() instanceof BuildException) {
                throw (BuildException) exc.getException();
            }

            throw new BuildException(exc.getMessage(), exc.getPublicId() + ":" + exc.getLineNumber());
        }
        catch(SAXException exc) {
            if (exc.getException() instanceof BuildException) {
                throw (BuildException) exc.getException();
            }
            else {
                throw new AntException("Parse error", exc);
            }
        }
        catch(ParserConfigurationException exc) {
            throw new AntException("Parser configuration error", exc);
        }
        catch(FileNotFoundException exc) {
            // This should never happen, since conn.connect()
            // has already been called successfully
            throw new AntException("Project file not found", exc);
        }
        catch(IOException exc) {
            throw new AntException("Error reading project file", exc);
        }

        return;
    }

    /**
     *  Parses the project path (specified using the "ant.project.path"
     *  system propertyinto URL objects.
     */
    private static URL[] getProjectPath() {
        String s = System.getProperty("ant.project.path", ".");

        StringTokenizer tokens = new StringTokenizer(s, System.getProperty("path.separator"));
        int i = 0;
        URL[] path = new URL[tokens.countTokens()];
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();

            try {
                if (token.endsWith(".jar")) {
                    path[i] = new URL("jar:file:" + token + "!/");
                }
                else if (token.endsWith("/")) {
                    path[i] = new URL("file:" + token);
                }
                else {
                    path[i] = new URL("file:" + token + "/");
                }
            }
            catch(MalformedURLException exc) {
                exc.printStackTrace();
            }

            i++;
        }

        return path;
    }


    /**
     * JAXP stuff.
     */
    private static SAXParserFactory parserFactory;

    static {
        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(false);
        parserFactory.setNamespaceAware(true);
    }
}
