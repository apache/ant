/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.util;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Offers some helper methods on the Path structure in ant.
 *
 * <p>Basic idea behind this utility class is to use it from inside the 
 * different ant objects (and user defined objects) that need dclassLoading 
 * for their operation.
 * Normally those would have a setClasspathRef() {for the @@classpathref} 
 * and/or a createClasspath() {for the nested &lt;classpath&gt;}
 * Typically one would have</p> 
 *
 * <pre><code>
 * public void setClasspathRef(Reference r) {
 *     this.classpathId = r.getRefId();
 *     createClasspath().setRefid(r);
 * }
 * 
 * public Path createClasspath() {
 *     if (this.classpath == null) {
 *         this.classpath = new Path(getProject());
 *     }
 *     return this.classpath.createPath();
 * }
 * 
 * public void setClassname(String fqcn) {
 *     this.classname = fqcn;
 * }
 * </code></pre>
 * 
 * <p>when you actually need the classloading you can just:</p>
 * 
 * <pre><code>
 *     ClassLoader cl = ClasspathUtils.getClassLoaderForPath(this.classpath, this.classpathId);
 *     Object o = ClasspathUtils.newInstance(this.classname, cl);
 * </code></pre>
 *
 * @since Ant 1.6
 */
public class ClasspathUtils {
    private static final String LOADER_ID_PREFIX = "ant.loader.";
    public static final String REUSE_LOADER_REF = "ant.reuse.loader";

    /** 
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Project, Reference, boolean)}.
     *
     * <p>Assumes the logical 'false' for the reverseLoader.</p>
     *  
     * @param path 
     * @param pathId
     * @return
     */
    public static ClassLoader getClassLoaderForPath(Project p, Reference ref) {
        return getClassLoaderForPath(p, ref, false);
    }
    
    /** 
     * Convenience overloaded version of {@link #geClassLoader(Path,
     * String, boolean)}.
     *
     * <p>Delegates to the other one after extracting the referenced
     * Path from the Project This checks also that the passed
     * Reference is pointing to a Path all right.</p>
     * @param p current ant project
     * @param ref Reference to Path structure
     * @param reverseLoader if set to true this new loader will take
     * precedence over it's parent (which is contra the regular
     * classloader behaviour)
     * @return
     */
    public static ClassLoader getClassLoaderForPath(Project p, Reference ref, 
                                                    boolean reverseLoader) {
        String pathId = ref.getRefId();
        Object path = p.getReference(pathId);
        if (!(path instanceof Path)){
            throw new BuildException ("The specified classpathref " + pathId + 
                " does not reference a Path.");
        }        
        return getClassLoaderForPath((Path)path, pathId, reverseLoader);
    }

    /** 
     * Convenience overloaded version of {@link
     * #getClassLoaderForPath(Path, String, boolean)}.
     *
     * <p>Assumes the logical 'false' for the reverseLoader.</p>
     *  
     * @param path 
     * @param pathId
     * @return
     */
    public static ClassLoader getClassLoaderForPath(Path path, String pathId){
        return getClassLoaderForPath(path, pathId, false);    
    }
    
    /**
     * Gets a classloader that loads classes from the classpath
     * defined in the path argument.
     *
     * <p>Based on the setting of the magic property
     * 'ant.reuse.loader' this will try to reuse the perviously
     * created loader with that id, and of course store it there upon
     * creation.</p>
     * @param path Path object to be used as classpath for this classloader
     * @param pathId identification for this Path, will be used to
     * identify the classLoader as well.
     * @param reverseLoader if set to true this new loader will take
     * precedence over it's parent (which is contra the regular
     * classloader behaviour)
     * @return ClassLoader that uses the Path as its classpath. 
     */
    public static ClassLoader getClassLoaderForPath(Path path, String pathId, 
                                                    boolean reverseLoader) {
        ClassLoader cl = null;
        Project p = path.getProject();
        String loaderId = LOADER_ID_PREFIX + pathId;
        // code stolen from o.a.t.a.taskdefs.Definer, might be a todo
        // to remove it there didn't look at the reverse loader stuff
        // however (todo that first)

        // magic property
        if (p.getProperty(REUSE_LOADER_REF) != null) {
            //chose not to do the extra instanceof checking here, consider it 
            // a programming error and not a config error if this fails
            // so I assume the RuntimeException is OK 
            cl = (ClassLoader)p.getReference(loaderId);
        }
        if (cl == null){
            cl = getUniqueClassLoaderForPath(path, reverseLoader);
            p.addReference(loaderId, cl);            
        } 
        
        return cl;        
    }

    /**
     * Gets a fresh, different, not used before classloader that uses the
     * passed path as it's classpath.
     *
     * <p>This method completely ignores the ant.reuse.loader magic
     * property and should be used with caution.</p>
     * @param path the classpath for this loader
     * @param reverseLoader
     * @return
     */
    public static ClassLoader getUniqueClassLoaderForPath(Path path,
                                                          boolean reverseLoader) {
        ClassLoader cl = null;
        Project p = path.getProject();

        AntClassLoader acl = p.createClassLoader(Path.systemClasspath);
        if (reverseLoader) {
            acl.setParentFirst(false);
            acl.addJavaLibraries();
        }

        return cl;        
    }
    
    public static Object newInstance(String className,
                                     ClassLoader userDefinedLoader){
        try {
            Class clazz = userDefinedLoader.loadClass(className);
            Object o = clazz.newInstance();        
            return o; 
        } catch (ClassNotFoundException e) {
            throw new BuildException("Class " + className + 
                                     " not found by the specific classLoader.",
                                     e);
        } catch (InstantiationException e) {
            throw new BuildException("Could not instantiate " + className 
                                     + ". Specified class should have a no "
                                     + "argument constructor.", e);
        } catch (IllegalAccessException e) {
            throw new BuildException("Could not instantiate " + className 
                                     + ". Specified class should have a "
                                     + "public constructor.", e);
        }
    }

}
