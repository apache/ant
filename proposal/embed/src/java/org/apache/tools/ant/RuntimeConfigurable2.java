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

package org.apache.tools.ant;

import org.apache.tools.ant.helper.*;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;
import java.util.Hashtable;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributeListImpl;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Wrapper class that holds the attributes of an element, its children, and 
 * any text within it. It then takes care of configuring that element at 
 * runtime.
 *
 * This uses SAX2 and a more flexible substitution mechansim, based on
 * o.a.tomcat.util.IntrospectionUtil.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Costin Manolache
 */
public class RuntimeConfigurable2 extends RuntimeConfigurable {

    /** Name of the element to configure. elementName in UE */
    private String elementTag = null;  
    /** List of child element wrappers.  */
    private Vector children = new Vector();
    /** The element to configure. realThing in UE */
    private Object wrappedObject = null;
    /** XML attributes for the element. */
    private Attributes attributes;
    /** Text appearing within the element. */
    private StringBuffer characters = new StringBuffer();

    Project project;
    protected Location location = Location.UNKNOWN_LOCATION;
    
    /**
     * Sole constructor creating a wrapper for the specified object.
     * 
     * @param proxy The element to configure. Must not be <code>null</code>.
     * @param elementTag The tag name generating this element.
     *                   Should not be <code>null</code>.
     */
    public RuntimeConfigurable2(Project project, Location location, Object proxy, String elementTag) {
        super( proxy, elementTag );
        wrappedObject = proxy;
        this.location=location;
        this.project=project;
        this.elementTag = elementTag;
        // This should never happen - all objects are lazy
        if( proxy instanceof Task )
            ((Task)proxy).setRuntimeConfigurableWrapper( this );
    }

    Project getProject() {
        return project;
    }

    Location getLocation() {
        return location;
    }    
    /**
     * Sets the element to configure. This is used when the real type of 
     * an element isn't known at the time of wrapper creation.
     * 
     * @param proxy The element to configure. Must not be <code>null</code>.
     */
    public void setProxy(Object proxy) {
        wrappedObject = proxy;
    }

    public Object getProxy() {
        return wrappedObject;
    }

    /**
     * Sets the attributes for the wrapped element.
     * 
     * @param attributes List of attributes defined in the XML for this
     *                   element. May be <code>null</code>.
     * @deprecated It shouldn't be called by anyone except ProjectHelper
     */
    public void setAttributes(AttributeList attributes) {
        //    this.attributes = new AttributeListImpl(attributes);
    }

    public void setAttributes2(Attributes attributes) {
        this.attributes=new AttributesImpl( attributes );
    }
    
    /**
     * Returns the list of attributes for the wrapped element.
     * 
     * @return An AttributeList representing the attributes defined in the
     *         XML for this element. May be <code>null</code>.
     * @deprecated only for bkwd compatibility
     */
    public AttributeList getAttributes() {
        return sax1Attributes( attributes );
    }

    public Attributes getAttributes2() {
        return attributes;
    }

    public static AttributeList sax1Attributes( Attributes sax2Att ) {
        AttributeListImpl sax1Att=new AttributeListImpl();
        int length = sax2Att.getLength();
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                // System.out.println("Attributes: " + sax2Att.getQName(i) + " " +
                //                    sax2Att.getValue(i));
                sax1Att.addAttribute( sax2Att.getQName(i), 
                                      sax2Att.getType(i),
                                      sax2Att.getValue(i));
            }
	}
        return sax1Att;
    }

    /**
     * Adds a child element to the wrapped element.
     * 
     * @param child The child element wrapper to add to this one.
     *              Must not be <code>null</code>.
     */
    public void addChild(RuntimeConfigurable child) {
        // addChild( UnknownElement ) in UE
        children.addElement(child);
    }

    /**
     * Returns the child wrapper at the specified position within the list.
     * 
     * @param index The index of the child to return.
     * 
     * @return The child wrapper at position <code>index</code> within the
     *         list.
     */
    public RuntimeConfigurable getChild(int index) {
        return (RuntimeConfigurable) children.elementAt(index);
    }

    /**
     * Adds characters from #PCDATA areas to the wrapped element.
     * 
     * @param data Text to add to the wrapped element. 
     *        Should not be <code>null</code>.
     */
    public void addText(String data) {
        characters.append(data);
    }

    /**
     * Adds characters from #PCDATA areas to the wrapped element.
     * 
     * @param buf A character array of the text within the element.
     *            Must not be <code>null</code>.
     * @param start The start element in the array.
     * @param count The number of characters to read from the array.
     * 
     */
    public void addText(char[] buf, int start, int count) {
        addText(new String(buf, start, count));
    }

    /**
     * Returns the tag name of the wrapped element.
     * 
     * @return The tag name of the wrapped element. This is unlikely
     *         to be <code>null</code>, but may be.
     */
    public String getElementTag() {
        // getTag in UE
        return elementTag;
    }

    /**
     * Configures the wrapped element and all its children.
     * The attributes and text for the wrapped element are configured,
     * and then each child is configured and added. Each time the
     * wrapper is configured, the attributes and text for it are
     * reset.
     * 
     * If the element has an <code>id</code> attribute, a reference
     * is added to the project as well.
     * 
     * @param p The project containing the wrapped element. 
     *          Must not be <code>null</code>.
     * 
     * @exception BuildException if the configuration fails, for instance due
     *            to invalid attributes or children, or text being added to
     *            an element which doesn't accept it.
     */
    public void maybeConfigure(Project p) throws BuildException {
        String id = null;

        PropertyHelper ph=PropertyHelper.getPropertyHelper(p);

        if (attributes != null) {
            ph.configure(wrappedObject, attributes, p);
            id = attributes.getValue("id");
            attributes = null;
        }
        
        if (characters.length() != 0) {
//             // First do substitution. This allows <echo>Message</echo> to work
//             // like <echo message="..." />. And it's more fun.
            String txt=characters.toString();
//             txt=ph.replaceProperties( txt );
            ProjectHelper.addText(p, wrappedObject, txt);
            characters.setLength(0);
        }
        Enumeration enum = children.elements();
        while (enum.hasMoreElements()) {
            RuntimeConfigurable2 child 
                = (RuntimeConfigurable2) enum.nextElement();
            if (child.wrappedObject instanceof Task) {
                Task childTask = (Task) child.wrappedObject;
                childTask.setRuntimeConfigurableWrapper(child);
                childTask.maybeConfigure();
            } else {
                child.maybeConfigure(p);
            }
            ProjectHelper.storeChild(p, wrappedObject, child.wrappedObject, 
                                     child.getElementTag().toLowerCase(Locale.US));
        }

        if (id != null) {
            // p.addReference(id, wrappedObject);
            p.getReferences().put( id, wrappedObject );
            //System.out.println("XXX updating reference " + this + " " + id + " " + wrappedObject );
        }
    }


    // -------------------- Merged from UE
    RuntimeConfigurable getWrapper() {
        return this;
    }
      
    
    /**
     * Creates the real object instance and child elements, then configures
     * the attributes and text of the real object. This unknown element
     * is then replaced with the real object in the containing target's list
     * of children.
     *
     * @exception BuildException if the configuration fails
     */
    protected void maybeConfigureTask() throws BuildException {
//         wrappedObject = makeObject(this, getWrapper());

//         getWrapper().setProxy(wrappedObject);
//         if (wrappedObject instanceof Task) {
//             ((Task) wrappedObject).setRuntimeConfigurableWrapper(getWrapper());
//         }

//         handleChildren(wrappedObject, getWrapper());

//         getWrapper().maybeConfigure(getProject());
    }

    /**
     * Creates child elements, creates children of the children
     * (recursively), and sets attributes of the child elements.
     *
     * @param parent The configured object for the parent.
     *               Must not be <code>null</code>.
     *
     * @param parentWrapper The wrapper containing child wrappers
     *                      to be configured. Must not be <code>null</code>
     *                      if there are any children.
     *
     * @exception BuildException if the children cannot be configured.
     */
    protected void handleChildren(Object parent,
                                  RuntimeConfigurable parentWrapper)
        throws BuildException {

        if (parent instanceof TaskAdapter) {
            parent = ((TaskAdapter) parent).getProxy();
        }

        Class parentClass = parent.getClass();
        IntrospectionHelper ih = IntrospectionHelper.getHelper(parentClass);

        for (int i = 0;  i < children.size(); i++) {
            RuntimeConfigurable childWrapper = parentWrapper.getChild(i);
            UnknownElement child = (UnknownElement) children.elementAt(i);
            Object realChild = null;

            if (parent instanceof TaskContainer) {
                realChild = makeTask(child, childWrapper, false);
                ((TaskContainer) parent).addTask((Task) realChild);
            } else {
                realChild = ih.createElement(getProject(), parent, child.getTag());
            }

            childWrapper.setProxy(realChild);
            if (parent instanceof TaskContainer) {
                ((Task) realChild).setRuntimeConfigurableWrapper(childWrapper);
            }

            child.handleChildren(realChild, childWrapper);

            if (parent instanceof TaskContainer) {
                ((Task) realChild).maybeConfigure();
            }
        }
    }

    /**
     * Creates a named task or data type. If the real object is a task,
     * it is configured up to the init() stage.
     *
     * @param ue The unknown element to create the real object for.
     *           Must not be <code>null</code>.
     * @param w  Ignored in this implementation.
     *
     * @return the task or data type represented by the given unknown element.
     */
//     protected Object makeObject(UnknownElement ue, RuntimeConfigurable w) {
//         Object o = makeTask(ue, w, true);
//         if (o == null) {
//             o = getProject().createDataType(ue.getTag());
//         }
//         if (o == null) {
//             throw getNotFoundException("task or type", ue.getTag());
//         }
//         return o;
//     }

    /**
     * Creates a named task and configures it up to the init() stage.
     *
     * @param ue The UnknownElement to create the real task for.
     *           Must not be <code>null</code>.
     * @param w  Ignored.
     * @param onTopLevel Whether or not this is definitely trying to create
     *                   a task. If this is <code>true</code> and the
     *                   task name is not recognised, a BuildException
     *                   is thrown.
     *
     * @return the task specified by the given unknown element, or
     *         <code>null</code> if the task name is not recognised and
     *         onTopLevel is <code>false</code>.
     */
    protected Task makeTask(UnknownElement ue, RuntimeConfigurable w,
                            boolean onTopLevel) {
        Task task = getProject().createTask(ue.getTag());
        if (task == null && !onTopLevel) {
            throw getNotFoundException("task", ue.getTag());
        }

        if (task != null) {
            task.setLocation(getLocation());
            // UnknownElement always has an associated target
//            task.setOwningTarget(getOwningTarget());
            task.init();
        }
        return task;
    }
    /**
     * Returns a very verbose exception for when a task/data type cannot
     * be found.
     *
     * @param what The kind of thing being created. For example, when
     *             a task name could not be found, this would be
     *             <code>"task"</code>. Should not be <code>null</code>.
     * @param elementName The name of the element which could not be found.
     *                    Should not be <code>null</code>.
     *
     * @return a detailed description of what might have caused the problem.
     */
    protected BuildException getNotFoundException(String what,
                                                  String elementName) {
        String lSep = System.getProperty("line.separator");
        String msg = "Could not create " + what + " of type: " + elementName
            + "." + lSep + lSep
            + "Ant could not find the task or a class this "
            + "task relies upon." + lSep + lSep
            + "This is common and has a number of causes; the usual " + lSep
            + "solutions are to read the manual pages then download and" + lSep
            + "install needed JAR files, or fix the build file: " + lSep
            + " - You have misspelt '" + elementName + "'." + lSep
            + "   Fix: check your spelling." + lSep
            + " - The task needs an external JAR file to execute" + lSep
            + "   and this is not found at the right place in the classpath." + lSep
            + "   Fix: check the documentation for dependencies." + lSep
            + "   Fix: declare the task." + lSep
            + " - The task is an Ant optional task and optional.jar is absent" + lSep
            + "   Fix: look for optional.jar in ANT_HOME/lib, download if needed" + lSep
            + " - The task was not built into optional.jar as dependent"  + lSep
            + "   libraries were not found at build time." + lSep
            + "   Fix: look in the JAR to verify, then rebuild with the needed" + lSep
            + "   libraries, or download a release version from apache.org" + lSep
            + " - The build file was written for a later version of Ant" + lSep
            + "   Fix: upgrade to at least the latest release version of Ant" + lSep
            + " - The task is not an Ant core or optional task " + lSep
            + "   and needs to be declared using <taskdef>." + lSep
            + lSep
            + "Remember that for JAR files to be visible to Ant tasks implemented" + lSep
            + "in ANT_HOME/lib, the files must be in the same directory or on the" + lSep
            + "classpath" + lSep
            + lSep
            + "Please neither file bug reports on this problem, nor email the" + lSep
            + "Ant mailing lists, until all of these causes have been explored," + lSep
            + "as this is not an Ant bug.";


        return new BuildException(msg, getLocation());
    }

    /**
     * Returns the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName() {
//         return wrappedObject == null || !(wrappedObject instanceof Task) ?
//             super.getTaskName() : ((Task) wrappedObject).getTaskName();
        if( wrappedObject!=null && (wrappedObject instanceof Task))
            return ((Task) wrappedObject).getTaskName();
        else
            return elementTag;
    }

    /**
     * Returns the task instance after it has been created and if it is a task.
     *
     * @return a task instance or <code>null</code> if the real object is not
     *         a task.
     */
//     public Task getTask() {
//         if (wrappedObject instanceof Task) {
//             return (Task) wrappedObject;
//         }
//         return null;
//     }

}
