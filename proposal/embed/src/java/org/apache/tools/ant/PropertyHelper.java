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
 * Deals with properties - substitution, dynamic properties, etc.
 *
 * Eventually the static methods from ProjectHelper should be
 * moved here ( with a wrapper for backward compat ).
 *
 * Also the property store ( Hashtable ) and all property manipulation
 * logic could be moved here.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Costin Manolache
 */
public class PropertyHelper {
    Project project;
    Vector propertyInterceptors=new Vector();
    
    protected PropertyHelper() {
    }

    public void setProject(Project p ) {
        this.project=p;
    }

    /** Factory method to create a property processor.
     *  Right now returns the singleton instance of PropertyHelper,
     *  in future it may use discovery of config to return a
     *  customized version, for integration in other apps.
     */
    public static PropertyHelper getPropertyHelper(Project project) {
        PropertyHelper ph=(PropertyHelper)project.getReference( "ant.PropertyHelper" ); 
        if( ph!=null ) return ph;
        ph=new PropertyHelper();
        ph.setProject( project );

        project.addReference( "ant.PropertyHelper",ph );
        return ph;
    }
    
    public void addPropertyInterceptor( PropertyInterceptor pi ) {
        propertyInterceptors.addElement( pi );
    }

//     public Vector getPropertyInterceptors() {
//         return propertyInterceptors;
//     }
    
    /** Process an value, doing the replacements.
     */
    public String replaceProperties( String value ) {
        if (value == null) {
            return null;
        }

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();

        // XXX Move the static method here - if this is accepted in the main
        // branch.
        ProjectHelper.parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();
        while (i.hasMoreElements()) {
            
            String fragment = (String) i.nextElement();
            if (fragment == null) {
                String propertyName = (String) j.nextElement();

                Object repl=processDynamic( project, propertyName);

                if( repl==null) {
                    // default to the static property.
                    repl=project.getProperty( propertyName );
                }
                
                if (repl==null ) {
                    project.log("Property ${" + propertyName 
                        + "} has not been set", Project.MSG_VERBOSE);
                    fragment="${" + propertyName + "}"; 
                } else {
                    fragment = repl.toString();
                }
            }
            sb.append(fragment);
        }                        
        
        return sb.toString();
    }

    /** Use the reference table to generate values for ${} substitution.
     *  To preserve backward compat ( as much as possible ) we'll only process
     *  ids with a 'namespace-like' syntax.
     *
     *  Currently we support:
     *    dom:idName:/xpath/like/syntax  - the referenced node must be a DOM, we'll use
     *                      XPath to extract a node. ( a simplified syntax is handled
     *                      directly, XXX used for 'real' xpaths ).
     *    toString:idName - we use toString on the referenced object
     *    bean:idName.propertyName - we get the idName and call the getter for the property. 
     */
    Object processDynamic( Project project, String name ) {
        for(int i=0; i<propertyInterceptors.size(); i++ ) {
            PropertyInterceptor pi=(PropertyInterceptor)propertyInterceptors.elementAt( i );
            
            Object o=pi.getProperty( project, null, name );
            if( o!=null )
                return o;
        }

        // experimental - will be removed.
        if( name.startsWith( "toString:" )) {
            name=name.substring( "toString:".length());
            Object v=project.getReference( name );
            if( v==null ) return null;
            return v.toString();
        }

        return null;
    }

    
    /** Configure a component using SAX2 attributes.
     */
    public void configure( Object target, Attributes attrs, Project project )
        throws BuildException
    {
        if (target instanceof TaskAdapter) {
            target = ((TaskAdapter) target).getProxy();
        }
        
        IntrospectionHelper ih = 
            IntrospectionHelper.getHelper(target.getClass());
        
        project.addBuildListener(ih);
        
        for (int i = 0; i < attrs.getLength(); i++) {
            // reflect these into the target
            String value = replaceProperties(attrs.getValue(i));
            
            try {
                ih.setAttribute(project, target, 
                                attrs.getQName(i).toLowerCase(Locale.US), value);
            } catch (BuildException be) {
                // id attribute must be set externally
                if (!attrs.getQName(i).equals("id")) {
                    throw be;
                }
            }
        }
    }

}
