package org.apache.ant;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 *
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
class TaskClassLoader extends ClassLoader {

    // -----------------------------------------------------------------
    // PRIVATE MEMBERS
    // -----------------------------------------------------------------

    /**
     *
     */
    private Hashtable cache = new Hashtable();

    /**
     *
     */
    private ZipFile zf;

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------- 
    
    /**
     * Constructs a classloader that loads classes from the specified
     * zip file.
     */
    TaskClassLoader(ClassLoader parent, ZipFile zf) {
        super(parent);
        this.zf = zf;
    }
     
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // ----------------------------------------------------------------- 
    
    /**
     *
     */
    public Class findClass(String name) 
    throws ClassNotFoundException 
    {
        Class c;
        try {
            return findSystemClass(name);
        } catch (ClassNotFoundException cnfe) {
        }
        try {
            return this.getClass().getClassLoader().loadClass(name);
        } catch (Exception e) {
        }
        Object o = cache.get(name);
        if (o != null) {
            c = (Class)o;
        } else {
            byte[] data = loadClassData(name);
            c = defineClass(data, 0, data.length);
            cache.put(name, c);
        }
        //if (resolve) {
        //    resolveClass(c);
        //}
        return c;
    }
    
    /**
     *
     */
    private byte[] loadClassData(String name) throws ClassNotFoundException {
        String newName = name.replace('.', '/');
        ZipEntry ze = zf.getEntry("/" + newName + ".class");
        //System.out.println("/" + newName + ".class");
        //System.out.println("ZE: " + ze);
        if (ze != null) {
            byte[] buf = new byte[((int)ze.getSize())];
            // System.out.println("ZE SIZE " + ze.getSize());
            try {
                InputStream in = zf.getInputStream(ze);
                int count = 0;
                int thisRead = 0;
                while (count < buf.length && thisRead != -1) {
                    thisRead = in.read(buf, count, buf.length - count);
                    count += thisRead;
                }
                in.close();
            } catch (IOException ioe) {
                throw new ClassNotFoundException("Can't load class: " + name + " " +
                                                 ioe.getMessage());
            }
            return buf;
        } else {
            throw new ClassNotFoundException("Can't find class for: " + name);
        }
    }
}