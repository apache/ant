/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Loads property values from a valid XML file, generating the
 * property names from the file's element and attribute names.
 *
 * <p>Example:</p>
 * <pre>
 *   &lt;root-tag myattr="true"&gt;
 *     &lt;inner-tag someattr="val"&gt;Text&lt;/inner-tag&gt;
 *     &lt;a2&gt;&lt;a3&gt;&lt;a4&gt;false&lt;/a4&gt;&lt;/a3&gt;&lt;/a2&gt;
 *     &lt;x&gt;x1&lt;/x&gt;
 *     &lt;x&gt;x2&lt;/x&gt;
 *   &lt;/root-tag&gt;
 *</pre>
 *
 * <p>this generates the following properties:</p>
 *
 * <pre>
 *  root-tag(myattr)=true
 *  root-tag.inner-tag=Text
 *  root-tag.inner-tag(someattr)=val
 *  root-tag.a2.a3.a4=false
 *  root-tag.x=x1,x2
 * </pre>
 *
 * <p>The <i>collapseAttributes</i> property of this task can be set
 * to true (the default is false) which will instead result in the
 * following properties (note the difference in names of properties
 * corresponding to XML attributes):</p>
 *
 * <pre>
 *  root-tag.myattr=true
 *  root-tag.inner-tag=Text
 *  root-tag.inner-tag.someattr=val
 *  root-tag.a2.a3.a4=false
 *  root-tag.x=x1,x2
 * </pre>
 *
 * <p>Optionally, to more closely mirror the abilities of the Property
 * task, a selected set of attributes can be treated specially.  To
 * enable this behavior, the "semanticAttributes" property of this task
 * must be set to true (it defaults to false).  If this attribute is
 * specified, the following attributes take on special meaning
 * (setting this to true implicitly sets collapseAttributes to true as
 * well):</p>
 *
 * <ul>
 *  <li><b>value</b>: Identifies a text value for a property.</li>
 *  <li><b>location</b>: Identifies a file location for a property.</li>
 *  <li><b>id</b>: Sets an id for a property</li>
 *  <li><b>refid</b>: Sets a property to the value of another property
 *       based upon the provided id</li>
 *  <li><b>pathid</b>: Defines a path rather than a property with
 *       the given id.</li>
 * </ul>
 *
 * <p>For example, with keepRoot = false, the following properties file:</p>
 *
 * <pre>
 * &lt;root-tag&gt;
 *   &lt;build&gt;
 *   &lt;build folder="build"&gt;
 *     &lt;classes id="build.classes" location="${build.folder}/classes"/&gt;
 *     &lt;reference refid="build.classes"/&gt;
 *   &lt;/build&gt;
 *   &lt;compile&gt;
 *     &lt;classpath pathid="compile.classpath"&gt;
 *       &lt;pathelement location="${build.classes}"/&gt;
 *     &lt;/classpath&gt;
 *   &lt;/compile&gt;
 *   &lt;run-time&gt;
 *     &lt;jars&gt;*.jar&lt;/jars&gt;
 *     &lt;classpath pathid="run-time.classpath"&gt;
 *       &lt;path refid="compile.classpath"/&gt;
 *       &lt;pathelement path="${run-time.jars}"/&gt;
 *     &lt;/classpath&gt;
 *   &lt;/run-time&gt;
 * &lt;/root-tag&gt;
 * </pre>
 *
 * <p>is equivalent to the following entries in a build file:</p>
 *
 * <pre>
 * &lt;property name="build" location="build"/&gt;
 * &lt;property name="build.classes" location="${build.location}/classes"/&gt;
 * &lt;property name="build.reference" refid="build.classes"/&gt;
 *
 * &lt;property name="run-time.jars" value="*.jar/&gt;
 *
 * &lt;classpath id="compile.classpath"&gt;
 *   &lt;pathelement location="${build.classes}"/&gt;
 * &lt;/classpath&gt;
 *
 * &lt;classpath id="run-time.classpath"&gt;
 *   &lt;path refid="compile.classpath"/&gt;
 *   &lt;pathelement path="${run-time.jars}"/&gt;
 * &lt;/classpath&gt;
 * </pre>
 *
 * <p> This task <i>requires</i> the following attributes:</p>
 *
 * <ul>
 * <li><b>file</b>: The name of the file to load.</li>
 * </ul>
 *
 * <p>This task supports the following attributes:</p>
 *
 * <ul>
 * <li><b>prefix</b>: Optionally specify a prefix applied to
 *     all properties loaded.  Defaults to an empty string.</li>
 * <li><b>keepRoot</b>: Indicate whether the root xml element
 *     is kept as part of property name.  Defaults to true.</li>
 * <li><b>validate</b>: Indicate whether the xml file is validated.
 *     Defaults to false.</li>
 * <li><b>collapseAttributes</b>: Indicate whether attributes are
 *     stored in property names with parens or with period
 *     delimiters.  Defaults to false, meaning properties
 *     are stored with parens (i.e., foo(attr)).</li>
 * <li><b>semanticAttributes</b>: Indicate whether attributes
 *     named "location", "value", "refid" and "path"
 *     are interpreted as ant properties.  Defaults
 *     to false.</li>
 * <li><b>rootDirectory</b>: Indicate the directory to use
 *     as the root directory for resolving location
 *     properties.  Defaults to the directory
 *     of the project using the task.</li>
 * <li><b>includeSemanticAttribute</b>: Indicate whether to include
 *     the semantic attribute ("location" or "value") as
 *     part of the property name.  Defaults to false.</li>
 * </ul>
 *
 * @author <a href="mailto:nicolaken@apache.org">Nicola Ken Barozzi</a>
 * @author Erik Hatcher
 * @author <a href="mailto:paul@priorartisans.com">Paul Christmann</a>
 *
 * @ant.task name="xmlproperty" category="xml"
 */

public class XmlProperty extends org.apache.tools.ant.Task {

    private File src;
    private String prefix = "";
    private boolean keepRoot = true;
    private boolean validate = false;
    private boolean collapseAttributes = false;
    private boolean semanticAttributes = false;
    private boolean includeSemanticAttribute = false;
    private File rootDirectory = null;
    private FileUtils fileUtils = FileUtils.newFileUtils();
    private Hashtable addedAttributes = new Hashtable();

    private static final String ID = "id";
    private static final String REF_ID = "refid";
    private static final String LOCATION = "location";
    private static final String VALUE = "value";
    private static final String PATH = "path";
    private static final String PATHID = "pathid";
    private static final String[] ATTRIBUTES = new String[] {
        ID, REF_ID, LOCATION, VALUE, PATH, PATHID
    };

    /**
     * Constructor.
     */
    public XmlProperty() {
        super();
    }

    /**
     * Initializes the task.
     */

    public void init() {
        super.init();
    }

    /**
     * Run the task.
     * @throws BuildException The exception raised during task execution.
     * @todo validate the source file is valid before opening, print a better error message
     * @todo add a verbose level log message listing the name of the file being loaded
     */
    public void execute()
            throws BuildException {

        if (getFile() == null) {
            String msg = "XmlProperty task requires a file attribute";
            throw new BuildException(msg);
        }

        BufferedInputStream configurationStream = null;

        try {
            log("Loading " + src.getAbsolutePath(), Project.MSG_VERBOSE);

            if (src.exists()) {

              configurationStream =
                      new BufferedInputStream(new FileInputStream(src));

              DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

              factory.setValidating(validate);
              factory.setNamespaceAware(false);

              Element topElement = factory.newDocumentBuilder().parse(configurationStream).getDocumentElement();

              // Keep a hashtable of attributes added by this task.
              // This task is allow to override its own properties
              // but not other properties.  So we need to keep track
              // of which properties we've added.
              addedAttributes = new Hashtable();

              if (keepRoot) {
                  addNodeRecursively(topElement, prefix, null);
              } else {
                  NodeList topChildren = topElement.getChildNodes();
                  int numChildren = topChildren.getLength();
                  for (int i = 0; i < numChildren; i++) {
                    addNodeRecursively(topChildren.item(i), prefix, null);
                  }
              }

            } else {
                log("Unable to find property file: " + src.getAbsolutePath(),
                    Project.MSG_VERBOSE);
            }

        } catch (SAXException sxe) {
            // Error generated during parsing
            Exception x = sxe;
            if (sxe.getException() != null)
                x = sxe.getException();
            throw new BuildException(x);

        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            throw new BuildException(pce);
        } catch (IOException ioe) {
            // I/O error
            throw new BuildException(ioe);
        } finally {
            if (configurationStream != null) {
                try {
                    configurationStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** Iterate through all nodes in the tree. */
    private void addNodeRecursively(Node node, String prefix,
                                    Object container) {

        // Set the prefix for this node to include its tag name.
        String nodePrefix = prefix;
        if (node.getNodeType() != Node.TEXT_NODE) {
            if (prefix.trim().length() > 0) {
                nodePrefix += ".";
            }
            nodePrefix += node.getNodeName();
        }

        // Pass the container to the processing of this node,
        Object nodeObject = processNode(node, nodePrefix, container);

        // now, iterate through children.
        if (node.hasChildNodes()) {

            NodeList nodeChildren = node.getChildNodes();
            int numChildren = nodeChildren.getLength();

            for (int i = 0; i < numChildren; i++) {
                // For each child, pass the object added by
                // processNode to its children -- in other word, each
                // object can pass information along to its children.
                addNodeRecursively(nodeChildren.item(i), nodePrefix,
                                   nodeObject);
            }
        }
    }

    void addNodeRecursively(org.w3c.dom.Node node, String prefix) {
        addNodeRecursively(node, prefix, null);
    }

    /**
     * Process the given node, adding any required attributes from
     * this child node alone -- but <em>not</em> processing any
     * children.
     *
     * @param node the XML Node to parse
     * @param prefix A string to prepend to any properties that get
     * added by this node.
     * @param container Optionally, an object that a parent node
     * generated that this node might belong to.  For example, this
     * node could be within a node that generated a Path.
     * @return the Object created by this node.  Generally, this is
     * either a String if this node resulted in setting an attribute,
     * or a Path.
     */
    public Object processNode (Node node, String prefix, Object container) {

        // Parse the attribute(s) and text of this node, adding
        // properties for each.
        // if the "path" attribute is specified, then return the created path
        // which will be passed to the children of this node.
        Object addedPath = null;

        // The value of an id attribute of this node.
        String id = null;

        if (node.hasAttributes()) {

            NamedNodeMap nodeAttributes = node.getAttributes();

            // Is there an id attribute?
            Node idNode = nodeAttributes.getNamedItem(ID);
            id = (semanticAttributes && idNode != null
                  ? idNode.getNodeValue() : null);

            // Now, iterate through the attributes adding them.
            for (int i = 0; i < nodeAttributes.getLength(); i++) {

                Node attributeNode = nodeAttributes.item(i);

                if (!semanticAttributes) {
                    String attributeName = getAttributeName(attributeNode);
                    String attributeValue = getAttributeValue(attributeNode);
                    addProperty(prefix + attributeName, attributeValue, null);
                } else {

                    String nodeName = attributeNode.getNodeName();
                    String attributeValue = getAttributeValue(attributeNode);

                    Path containingPath =
                        (container != null && container instanceof Path
                         ? (Path) container : null );

                    /*
                     * The main conditional logic -- if the attribute
                     * is somehow "special" (i.e., it has known
                     * semantic meaning) then deal with it
                     * appropriately.
                     */
                    if (nodeName.equals(ID)) {
                        // ID has already been found above.
                        continue;
                    } else if (containingPath != null
                               && nodeName.equals(PATH)) {
                        // A "path" attribute for a node within a Path object.
                        containingPath.setPath(attributeValue);
                    } else if (container instanceof Path
                               && nodeName.equals(REF_ID)) {
                        // A "refid" attribute for a node within a Path object.
                        containingPath.setPath(attributeValue);
                    } else if (container instanceof Path
                               && nodeName.equals(LOCATION)) {
                        // A "location" attribute for a node within a
                        // Path object.
                        containingPath.setLocation(resolveFile(attributeValue));
                    } else if (nodeName.equals(PATHID)) {
                        // A node identifying a new path
                        if (container != null) {
                            throw new BuildException("XmlProperty does not "
                                                     + "support nested paths");
                        }

                        addedPath = new Path(getProject());
                        getProject().addReference(attributeValue, addedPath);
                    } else {
                        // An arbitrary attribute.
                        String attributeName = getAttributeName(attributeNode);
                        addProperty(prefix + attributeName, attributeValue, id);
                    }
                }
            }
        }

        String nodeText = null;
        if (node.getNodeType() == Node.TEXT_NODE) {
            // For the text node, add a property.
            nodeText = getAttributeValue(node);
        } else if ((node.getNodeType() == Node.ELEMENT_NODE)
            && (node.getChildNodes().getLength() == 1)
            && (node.getFirstChild().getNodeType() == Node.CDATA_SECTION_NODE)) {

            nodeText = node.getFirstChild().getNodeValue();
        }

        if (nodeText != null) {
            // If the containing object was a String, then use it as the ID.
            if (semanticAttributes && id == null
                && container instanceof String) {
                id = (String) container;
                System.out.println("Setting id = " + id);
            }

            if (nodeText.trim().length() != 0) {
                addProperty(prefix, nodeText, id);
            }
        }

        // Return the Path we added or the ID of this node for
        // children to reference if needed.  Path objects are
        // definitely used by child path elements, and ID may be used
        // for a child text node.
        return (addedPath != null ? addedPath : id);
    }

    /**
     * Actually add the given property/value to the project
     * after writing a log message.
     */
    private void addProperty (String name, String value, String id) {
        String msg = name + ":" + value;
        if (id != null) {
            msg += ("(id=" + id + ")");
        }
        log(msg, Project.MSG_DEBUG);

        if (addedAttributes.containsKey(name)) {
            // If this attribute was added by this task, then
            // we append this value to the existing value.
            // We use the setProperty method which will
            // forcibly override the property if it already exists.
            // We need to put these properties into the project
            // when we read them, though (instead of keeping them
            // outside of the project and batch adding them at the end)
            // to allow other properties to reference them.
            value = (String)addedAttributes.get(name) + "," + value;
            getProject().setProperty(name, value);
        } else {
            getProject().setNewProperty(name, value);
        }
        addedAttributes.put(name, value);
        if (id != null) {
            getProject().addReference(id, value);
        }
    }

    /**
     * Return a reasonable attribute name for the given node.
     * If we are using semantic attributes or collapsing
     * attributes, the returned name is ".nodename".
     * Otherwise, we return "(nodename)".  This is long-standing
     * (and default) &lt;xmlproperty&gt; behavior.
     */
    private String getAttributeName (Node attributeNode) {
        String attributeName = attributeNode.getNodeName();

        if (semanticAttributes) {
            // Never include the "refid" attribute as part of the
            // attribute name.
            if (attributeName.equals(REF_ID)) {
                return "";
            // Otherwise, return it appended unless property to hide it is set.
            } else if (!isSemanticAttribute(attributeName)
                       || includeSemanticAttribute) {
                return "." + attributeName;
            } else {
                return "";
            }
        } else if (collapseAttributes) {
            return "." + attributeName;
        } else {
            return "(" + attributeName + ")";
        }
    }

    /**
     * Return whether the provided attribute name is recognized or not.
     */
    private static boolean isSemanticAttribute (String attributeName) {
        for (int i=0;i<ATTRIBUTES.length;i++) {
            if (attributeName.equals(ATTRIBUTES[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the value for the given attribute.
     * If we are not using semantic attributes, its just the
     * literal string value of the attribute.
     *
     * <p>If we <em>are</em> using semantic attributes, then first
     * dependent properties are resolved (i.e., ${foo} is resolved
     * based on the foo property value), and then an appropriate data
     * type is used.  In particular, location-based properties are
     * resolved to absolute file names.  Also for refid values, look
     * up the referenced object from the project.</p>
     */
    private String getAttributeValue (Node attributeNode) {
        String nodeValue = attributeNode.getNodeValue().trim();
        if (semanticAttributes) {
            String attributeName = attributeNode.getNodeName();
            nodeValue = getProject().replaceProperties(nodeValue);
            if (attributeName.equals(LOCATION)) {
                File f = resolveFile(nodeValue);
                return f.getPath();
            } else if (attributeName.equals(REF_ID)) {
                Object ref = getProject().getReference(nodeValue);
                if (ref != null) {
                    return ref.toString();
                }
            }
        }
        return nodeValue;
    }

    /**
     * The XML file to parse; required.
     */
    public void setFile(File src) {
        this.src = src;
    }

    /**
     * the prefix to prepend to each property
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix.trim();
    }

    /**
     * flag to include the xml root tag as a
     * first value in the property name; optional,
     * default is true
     */
    public void setKeeproot(boolean keepRoot) {
        this.keepRoot = keepRoot;
    }

    /**
     * flag to validate the XML file; optional, default false
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * flag to treat attributes as nested elements;
     * optional, default false
     */
    public void setCollapseAttributes(boolean collapseAttributes) {
        this.collapseAttributes = collapseAttributes;
    }

    public void setSemanticAttributes (boolean semanticAttributes) {
        this.semanticAttributes = semanticAttributes;
    }

    public void setRootDirectory (File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void setIncludeSemanticAttribute (boolean includeSemanticAttribute) {
        this.includeSemanticAttribute = includeSemanticAttribute;
    }

    /* Expose members for extensibility */

    protected File getFile () {
        return this.src;
    }

    protected String getPrefix () {
        return this.prefix;
    }

    protected boolean getKeeproot () {
        return this.keepRoot;
    }

    protected boolean getValidate () {
        return this.validate;
    }

    protected boolean getCollapseAttributes () {
        return this.collapseAttributes;
    }

    protected boolean getSemanticAttributes () {
        return this.semanticAttributes;
    }

    protected File getRootDirectory () {
        return this.rootDirectory;
    }

    protected boolean getIncludeSementicAttribute () {
        return this.includeSemanticAttribute;
    }

    /**
     * Let project resolve the file - or do it ourselves if
     * rootDirectory has been set.
     */
    private File resolveFile(String fileName) {
        if (rootDirectory == null) {
            return getProject().resolveFile(fileName);
        }
        return fileUtils.resolveFile(rootDirectory, fileName);
    }

}
