/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Core of the bytecode analyzer. It loads classes from a given classpath.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class ClassPathLoader {
	
	public final static FileLoader NULL_LOADER = new NullLoader();
	
	/** the list of files to look for */
	protected File[] files;
	
	/**
	 * create a new instance with a given classpath. It must be urls
	 * separated by the platform specific path separator.
	 * @param classPath the classpath to load all the classes from.
	 */
	public ClassPathLoader(String classPath){
		StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
		Vector entries = new Vector();
		while (st.hasMoreTokens()){
			File file = new File(st.nextToken());
			entries.addElement(file);
		}
		files = new File[entries.size()];
		entries.copyInto(files);
	}
	
	/**
	 * create a new instance with a given set of urls.
	 * @param entries valid file urls (either .jar, .zip or directory)
	 */
	public ClassPathLoader(String[] entries){
		files = new File[entries.length];
		for (int i = 0; i < entries.length; i++){
			files[i] = new File(entries[i]);
		}
	}
	
	/**
	 * create a new instance with a given set of urls
	 * @param entries file urls to look for classes (.jar, .zip or directory)
	 */
	public ClassPathLoader(File[] entries){
		files = entries;
	}
	
	/** the interface to implement to look up for specific resources */
	public interface FileLoader {
		/** the file url that is looked for .class files */
		public File getFile();
		/** return the set of classes found in the file */
		public ClassFile[] getClasses() throws IOException;
	}
	
	/**
	 * @return the set of <tt>FileLoader</tt> loaders matching the given classpath.
	 */
	public Enumeration loaders(){
		return new LoaderEnumeration();
	}
	
	/**
	 * return the whole set of classes in the classpath. Note that this method
	 * can be very resource demanding since it must load all bytecode from
	 * all classes in all resources in the classpath at a time.
	 * To process it in a less resource demanding way, it is maybe better to
	 * use the <tt>loaders()</tt> that will return loader one by one.
	 *
	 * @return the hashtable containing ALL classes that are found in the given
	 * classpath. Note that the first entry of a given classname will shadow
	 * classes with the same name (as a classloader does)
	 */
	public Hashtable getClasses() throws IOException {
		Hashtable map = new Hashtable();
		Enumeration enum = loaders();
		while ( enum.hasMoreElements() ){
			FileLoader loader = (FileLoader)enum.nextElement();
			System.out.println("Processing " + loader.getFile());
			long t0 = System.currentTimeMillis();
			ClassFile[] classes = loader.getClasses();
			long dt = System.currentTimeMillis() - t0;
			System.out.println("" + classes.length + " loaded in " + dt + "ms");
			for (int j = 0; j < classes.length; j++){
				String name = classes[j].getName();
				// do not allow duplicates entries to preserve 'classpath' behavior
				// first class in wins
				if ( !map.contains(name) ){
					map.put(name, classes[j]);
				}
			}
		}
		return map;
	}
	
	/** dirty little test, should be moved to a testcase */
	public static void main(String[] args) throws Exception {
		ClassPathLoader cl = new ClassPathLoader("e:/jdk/jdk1.3.1/lib/tools.jar;e:/jdk/jdk1.3.1/jre/lib/rt.jar");
		Hashtable map = cl.getClasses();
		System.out.println("Loaded classes: " + map.size());
	}
	
	/** the loader enumeration that will return loaders */
	protected class LoaderEnumeration implements Enumeration {
		protected int index = 0;
		public boolean hasMoreElements(){
			return index < files.length;
		}
		public Object nextElement(){
			if (index >= files.length){
				throw new NoSuchElementException();
			}
			File file = files[index++];
			if ( !file.exists() ){
				return new NullLoader(file);
			}
			if ( file.isDirectory() ){
				// it's a directory
				return new DirectoryLoader(file);
			} else if ( file.getName().endsWith(".zip") || file.getName().endsWith(".jar") ){
				// it's a jar/zip file
				return new JarLoader(file);
			}
			return new NullLoader(file);
			
		}
	}
	
	/**
	 * useful methods to read the whole input stream in memory so that
	 * it can be accessed faster. Processing rt.jar and tools.jar from JDK 1.3.1
	 * brings time from 50s to 7s.
	 */
    public static InputStream getCachedStream(InputStream is) throws IOException {
		is = new BufferedInputStream(is);
		byte[] buffer = new byte[8192];
		ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		int n;
		baos.reset();
		while ((n = is.read(buffer, 0, buffer.length)) != -1) {
			baos.write(buffer, 0, n);
		}
		is.close();
		return new ByteArrayInputStream(baos.toByteArray());
    }
}

/** a null loader to return when the file is not valid */
class NullLoader implements ClassPathLoader.FileLoader {
	private File file;
	NullLoader(){
		this(null);
	}
	NullLoader(File file){
		this.file = file;
	}
	public File getFile(){
		return file;
	}
	public ClassFile[] getClasses() throws IOException {
		return new ClassFile[0];
	}
}

/**
 * jar loader specified in looking for classes in jar and zip
 * @todo read the jar manifest in case there is a Class-Path
 * entry.
 */
class JarLoader implements ClassPathLoader.FileLoader {
	private File file;
	JarLoader(File file){
		this.file = file;
	}
	public File getFile(){
		return file;
	}
	public ClassFile[] getClasses() throws IOException {
		ZipFile zipFile = new ZipFile(file);
		Vector v = new Vector();
		Enumeration entries = zipFile.entries();
		while (entries.hasMoreElements()){
			ZipEntry entry = (ZipEntry)entries.nextElement();
			if (entry.getName().endsWith(".class")){
				InputStream is = ClassPathLoader.getCachedStream(zipFile.getInputStream(entry));
				ClassFile classFile = new ClassFile(is);
				is.close();
				v.addElement(classFile);
			}
		}
		ClassFile[] classes = new ClassFile[v.size()];
		v.copyInto(classes);
		return classes;
	}
}

/**
 * directory loader that will look all classes recursively
 * @todo should discard classes which package name does not
 * match the directory ?
 */
class DirectoryLoader implements ClassPathLoader.FileLoader {
	private File directory;
	
	DirectoryLoader(File dir){
		directory = dir;
	}
	public File getFile(){
		return directory;
	}
	public ClassFile[] getClasses() throws IOException {
		Vector v = new Vector();
		Vector files = listFiles( directory, new ClassFilter(), true);
		for (int i = 0; i < files.size(); i++){
			File file = (File)files.elementAt(i);
			InputStream is = null;
			try {
				is = ClassPathLoader.getCachedStream(new FileInputStream(file));
				ClassFile classFile = new ClassFile(is);
				is.close();
				is = null;
				v.addElement(classFile);
			} finally {
				if (is != null){
					try {
						is.close();
					} catch (IOException ignored){}
				}
			}
		}
		ClassFile[] classes = new ClassFile[v.size()];
		v.copyInto(classes);
		return classes;
	}

	/**
	 * List files that obeys to a specific filter recursively from a given base
	 * directory.
	 * @param   directory   the directory where to list the files from.
	 * @param   filter      the file filter to apply
	 * @param   recurse     tells whether or not the listing is recursive.
	 * @return  the list of <tt>File</tt> objects that applies to the given
	 *          filter.
	 */
	public static Vector listFiles(File directory, FilenameFilter filter, boolean recurse){
		if (!directory.isDirectory()){
			 throw new IllegalArgumentException(directory + " is not a directory");
		}
		Vector list = new Vector();
		listFilesTo(list, directory, filter, recurse);
		return list;
	}

	/**
	 * List and add files to a given list. As a convenience it sends back the
	 * instance of the list given as a parameter.
	 * @param   list    the list of files where the filtered files should be added
	 * @param   directory   the directory where to list the files from.
	 * @param   filter      the file filter to apply
	 * @param   recurse     tells whether or not the listing is recursive.
	 * @return  the list instance that was passed as the <tt>list</tt> argument.
	 */
	private static Vector listFilesTo(Vector list, File directory, FilenameFilter filter, boolean recurse){
		String[] files = directory.list(filter);
		for (int i = 0; i < files.length; i++){
			list.addElement( new File(directory, files[i]) );
		}
		files = null;   // we don't need it anymore
		if (recurse){
			String[] subdirs = directory.list( new DirectoryFilter() );
			for (int i = 0; i < subdirs.length; i++){
				listFilesTo(list, new File(directory, subdirs[i]), filter, recurse);
			}
		}
		return list;
	}

}

/** Convenient filter that accepts only directory <tt>File</tt> */
class DirectoryFilter implements FilenameFilter {
	public boolean accept(File directory, String name){
		File pathname = new File(directory, name);
		return pathname.isDirectory();
	}
}
/** convenient filter to accept only .class files */
class ClassFilter implements FilenameFilter {
	public boolean accept(File dir, String name){
		return name.endsWith(".class");
	}
}
