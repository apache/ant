/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.ejb;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;


/**
 * The deployment tool to add the jonas specific deployment descriptors to 
 * the ejb jar file. JONAS only requires one additional file jonas-ejb-jar.xml.
 *
 * @author <a href="cmorvan@ingenosya.com">Cyrille Morvan</a>,
 *       <a href="http://www.ingenosya.com">Ingenosya France</a>
 * @version 1.0
 * @see EjbJar#createJonas
 */
public class JonasDeploymentTool extends GenericDeploymentTool {

    protected static final String JONAS_DD = "jonas-ejb-jar.xml";

    protected static final String GENIC_CLASS =
               "org.objectweb.jonas_ejb.tools.GenWholeIC";

    protected static final String OLD_GENIC_CLASS =
               "org.objectweb.jonas_ejb.tools.GenIC";

    protected static final String DEFAULT_ORB = "RMI";

    /** Instance variable that stores the suffix for the jonas jarfile. */
    private String jarSuffix = ".jar";

    /**
     * Instance variable that stores the fully qualified classname
     * of the JOnAS GenIC compiler.
     **/
    private String genicClass;

    private String additionalArgs = "";

    /** Instance variable that determines do not delete intermediate generated source files */
    private boolean keepgenerated = false;
    
    /** as websphere and WebLogic taskes */
    private boolean keepGeneric = false;    
    
    /** Instance variable that determines the JOnAS Root directory */
    private File jonasroot;

    /** Instance variable that determines if we could -secpropag */
    private boolean secpropag = false;
    
    /** Instance variable that determines the ouput directory */
    private File ouputdirectory;

    /** Instance variable that determines the path to the compiler to use */
    private String compiler;

    /** Instance variable that determines if GenIC is verbose */
    private boolean verbose;

    /** Instance variable that determines the ORB to use (RMI, JEREMIE, DAVID)*/
    private String orb;

    /** clean the working directory after work **/
    private boolean cleanWorkDir = false;

    private boolean noGENIC = false;

    /**
     * set the name of the GenIC compiler class.
     **/
    public void setGenicClass(final String inGenicClass) {
       genicClass = inGenicClass;
    }


    /**
     * Set the ORB to construct classpath.
     * @param inValue RMI, JEREMIE, DAVID,...
     **/
    public void setOrb(final String inValue) {
       orb = inValue;
    }

    /**
     * The compiler (switch <code>-javac</code>) to use.
     **/
    public void setCompiler(final String inCompiler) {
        compiler = inCompiler;
    }  
    
    /**
     * Setter used to store the value of keepGeneric
     * @param inValue a string, either 'true' or 'false'.
     */
    public void setKeepgeneric(boolean inValue) {
        this.keepGeneric = inValue;
    }
    
    /**
     * GenIC verbose or not
     * @param inValue either 'true' or 'false'
     **/
    public void setVerbose(final boolean inValue) {
        verbose = inValue;
    }

    /**
     * GenIC run or not.
     * @param inValue run or not
     **/
    public void setNoGENIC(final boolean inValue) {
        noGENIC = inValue;
    }

    /**
     * Sets whether -keepgenerated is passed to GenIC (that is,
     * the .java source files are kept).
     * @param inValue either 'true' or 'false'
     **/
    public void setKeepgenerated(final boolean inValue) {
        keepgenerated = inValue;
    }

    /**
     * set the jonas root directory (-Dinstall.root=).
     * 
     * @throws BuildException if the file doesn't exist.
     **/
    public void setJonasroot(final File inValue) {
      jonasroot = inValue;
    }

    /**
     * Modify the RMI Skel. and Stub. to implement
     * the implicit propagation of the transactionnal
     * context and security context.
     * For JOnAS 2.4 and next.
     */
    public void setSecpropag(final boolean inValue) {
      secpropag = inValue;
    }
    
    /**
     * set the output directory (-d ...).
     * <br>
     *  
     * It's the GenIC working directory. It's not the
     * DestDir, which is the 'jar' destination directory. 
     * 
     * @param inValue a file 
     **/
    public void setOuputdir(final File inValue) {
        ouputdirectory = inValue;
    }
    
    
    /**
     * set the output directory (-d ...).
     * Same as setOuputdir().
     * <br>
     * But do not override setDestDir()
     **/
    public void setWorkdir(final File inValue) {
        setOuputdir(inValue);
    }

    /**
     * Clean the specified Work dir after work.
     * @param inValue true : clean ; false : not clean
     **/
    public void setCleanworkdir(final boolean inValue) {
        cleanWorkDir = inValue;
    }
    
    /**
     * Setter used to store the suffix for the generated JOnAS jar file.
     * @param inString the string to use as the suffix.
     **/
    public void setSuffix(String inString) {
        this.jarSuffix = inString;
    }

    /**
     * sets some additional args to send to GenIC.
     **/
    public void setArgs(final String inArgs) {
        additionalArgs = inArgs;
    }

    /**
     * Add any vendor specific files which should be included in the
     * EJB Jar.
     * @param aDdPrefix MyDirectories/MyEjb- or MyDirectories/ 
     **/
    protected void addVendorFiles(final Hashtable someEjbFiles,final String aDdPrefix) {
        // Use Ant Naming convention
        File aJonasDD = new File(getConfig().descriptorDir,aDdPrefix + JONAS_DD);
        if ( aJonasDD.exists() ) {
            someEjbFiles.put(META_DIR + JONAS_DD, aJonasDD);
        } else {
            // try with JOnAS Naming convention
            if( ! addJonasVendorFiles(someEjbFiles,aDdPrefix) ) {            
               log("Unable to locate JOnAS deployment descriptor. It was expected to be in "
                  + aJonasDD.getPath() + ". Or please use JOnAS naming convention.",
                  Project.MSG_WARN);
            }
        }
    }

    /**
     * try to add JOnAS specific file, using JOnAS naming convention.
     * For example : jonas-Account.xml or jonas-ejb-jar.xml
     * @param aDdPrefix MyDirectories/MyEjb- or MyDirectories/ 
     * @return true if Ok
     */
    private boolean addJonasVendorFiles(final Hashtable someEjbFiles,final String aDdPrefix) {
      // replace \ by /, remove the last letter ( a dash - )
      final String aCanonicalDD = aDdPrefix.replace('\\', '/').substring(0,aDdPrefix.length()-1);
      final int index = aCanonicalDD.lastIndexOf('/') + 1;
      String anEjbJarName = aCanonicalDD.substring(index);
      if( "ejb".equals( anEjbJarName ) ) {
         anEjbJarName = "ejb-jar";
      } 
      final String aNewDdPrefix = 
            aDdPrefix.substring(0,index) + "jonas-" + anEjbJarName + ".xml";
      File aConventionNamingJonasDD = new File(getConfig().descriptorDir,aNewDdPrefix);
      
      log("look for jonas specific file using jonas naming convention " + aConventionNamingJonasDD,
                           Project.MSG_VERBOSE);

      if( aConventionNamingJonasDD.exists() ) {
         someEjbFiles.put(META_DIR + JONAS_DD,aConventionNamingJonasDD);
         return true;
      } else {
         return false;
      }
    }
    
    // include javadoc
    // Determine the JAR filename (without filename extension)
    protected String getJarBaseName(String aDescriptorFileName) {
      String aBaseName = null;
      EjbJar.Config aConfig = super.getConfig();
      if (aConfig.namingScheme.getValue().equals(EjbJar.NamingScheme.DESCRIPTOR)) {
         // try to find JOnAS specific convention name
         // ??/MyEJB.xml ( I will find later the ??/jonas-MyEJB.xml file )
         if( aDescriptorFileName.indexOf(aConfig.baseNameTerminator) == -1 ) {
            String aCanonicalDescriptor = aDescriptorFileName.replace('\\','/');
            int lastSeparatorIndex = aCanonicalDescriptor.lastIndexOf('/');
            int endBaseName;
            if (lastSeparatorIndex != -1) {
                endBaseName = aDescriptorFileName.indexOf(".xml", lastSeparatorIndex);
            } else {
                endBaseName = aDescriptorFileName.indexOf(".xml");
            }

            if (endBaseName != -1) {
                aBaseName = aDescriptorFileName.substring(0, endBaseName);
            }
         }
      }

      if( aBaseName == null ) {
         // else get standard BaseName
         aBaseName = super.getJarBaseName(aDescriptorFileName);
      }
      return aBaseName;
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     **/
    protected void writeJar(String baseName, File jarFile, Hashtable files,
                            String publicId) throws BuildException {
        // need to create a generic jar first.
        File genericJarFile = super.getVendorOutputJarFile(baseName);
        super.writeJar(baseName, genericJarFile, files, publicId);

        // todo ? if (alwaysRebuild || isRebuildRequired(genericJarFile, jarFile))
        buildJOnASJar(baseName,genericJarFile, jarFile,files,publicId);
        
        if (!keepGeneric) {
             log("deleting generic jar " + genericJarFile.toString(),
                           Project.MSG_VERBOSE);
             genericJarFile.delete();
        }
    }

    /**
     * Helper method invoked by execute() for each JOnAS jar to be built.
     * Encapsulates the logic of constructing a java task for calling
     * GenIC and executing it.
     * @param inBaseName the base name of the jar
     * @param inSourceJar java.io.File representing the source (EJB1.1) jarfile.
     * @param someFiles list of files in the jar. Add all the new genererated
     *   files.
     * @param inPublicId the link to DTD (to rewrite JAR).
     **/
    private void buildJOnASJar(final String inBaseName,
                               final File inSourceJar,final File inDestJar,
                               final Hashtable someFiles,String inPublicId) {
      org.apache.tools.ant.taskdefs.Java aJavaTask = null;
      String aGenIcClassName = genicClass;
      boolean isOldGenIC = false;
      boolean isTempDirectory = false;
      File anOutputDirectoryFile = null;

      // do not call GenIC
      // only copy file
      if (noGENIC) {
         try {
            FileUtils.newFileUtils().copyFile(inSourceJar, inDestJar);
            return;
         } catch (IOException anIOException) {
            throw new BuildException("Unable to write EJB jar", anIOException);
         }
      }
         
      // call GenIC
      try {
         aJavaTask = (Java) getTask().getProject().createTask("java");
         aJavaTask.setTaskName("genic");
         if(aGenIcClassName == null) {
            aGenIcClassName = GENIC_CLASS;
         } else if( OLD_GENIC_CLASS.equals(aGenIcClassName) ){
            isOldGenIC = true;
         }
         // ClassName
         aJavaTask.setClassname(aGenIcClassName);

         // JVM Args
         if( jonasroot == null ) {
            throw new BuildException("Error : set the jonasroot parameter");
         } else if ( ! jonasroot.isDirectory() ) {
            log("jonasroot attribut '" + jonasroot + "' is not a valid directory",
                     Project.MSG_ERR);
         }
         aJavaTask.createJvmarg().setValue("-Dinstall.root=" + jonasroot);
         File aJavaPolicyFile = new File(jonasroot,"config/java.policy");
         if( aJavaPolicyFile.exists() ) {
            aJavaTask.createJvmarg().setValue("-Djava.security.policy=" 
                                        + aJavaPolicyFile.toString() );
         }

         // Find output directory
         if( ouputdirectory == null ) {
            anOutputDirectoryFile = createTempDir();
            isTempDirectory = true;
            log("Use temporary output directory : " +
                  anOutputDirectoryFile, Project.MSG_VERBOSE);
         } else {
            anOutputDirectoryFile = ouputdirectory;
            log("Use temporary specific output directory : " + 
                  anOutputDirectoryFile, Project.MSG_VERBOSE);
         }
         aJavaTask.createArg().setValue("-d");
         aJavaTask.createArg().setFile(anOutputDirectoryFile);

         // Additionnal args
         aJavaTask.createArg().setLine(additionalArgs);
         // KeepGenerated
         if (keepgenerated) {
             aJavaTask.createArg().setValue("-keepgenerated");
         }

         // Verbose
         if( verbose ) {
            aJavaTask.createArg().setValue("-verbose");
         }
         
         // -secpropag
         if( secpropag ) {
            aJavaTask.createArg().setValue("-secpropag");            
         }
         
         // The compiler
         if (compiler == null) {
             // try to use the compiler specified by build.compiler. Right now we are just going
             // to allow Jikes
             String aBuildCompiler = getTask().getProject().getProperty("build.compiler");
             if ("jikes".equals(aBuildCompiler) ) {
                 aJavaTask.createArg().setValue("-javac");
                 aJavaTask.createArg().setValue("jikes");
             }
         } else {
            if( ! "default".equals( compiler ) ) {
               aJavaTask.createArg().setValue("-javac");
               aJavaTask.createArg().setLine(compiler);
            }
         }

         if( ! isOldGenIC ) {
            // the add in jar features is buggy...
            aJavaTask.createArg().setValue("-noaddinjar");
         }

         aJavaTask.createArg().setValue(inSourceJar.getPath());

         // try to create the classpath for the correct ORB
         Path aClasspath = getCombinedClasspath();
         if( aClasspath == null ) {
            aClasspath = new Path(getTask().getProject());
         }
         if( orb != null ) {
            String aOrbJar = new File(jonasroot,"lib/" + orb + "_jonas.jar").toString();
            String aConfigDir = new File(jonasroot,"config/").toString();
            Path aJOnASOrbPath = new Path(aClasspath.getProject(),
                                       aOrbJar + File.pathSeparator + aConfigDir );
            aClasspath.append( aJOnASOrbPath );
         } else {
            log("No ORB propertie setup (RMI, JEREMIE, DAVID).", Project.MSG_WARN);
         }

         // append the output directory
         aClasspath.append( new Path(aClasspath.getProject(), anOutputDirectoryFile.getPath()));
         aJavaTask.setClasspath(aClasspath);


         aJavaTask.setFork(true);

         log("Calling " + aGenIcClassName + " for " + inSourceJar.toString(),
                       Project.MSG_VERBOSE);

         if (aJavaTask.executeJava() != 0) {
             throw new BuildException("GenIC reported an error");
         }
         // Update the list of files.
         addAllFiles(anOutputDirectoryFile,"",someFiles);

         // rewrite the jar with the new files
         super.writeJar(inBaseName, inDestJar, someFiles, inPublicId);
     } catch(BuildException aBuildException) {
         throw aBuildException;
     } catch (Exception e) {
         // Have to catch this because of the semantics of calling main()
         String msg = "Exception while calling " + aGenIcClassName + ". Details: " + e.toString();
         throw new BuildException(msg, e);
     } finally {
        if( isTempDirectory && anOutputDirectoryFile != null) {
           dellAllFiles(anOutputDirectoryFile);
        } else if( cleanWorkDir && anOutputDirectoryFile != null) {
           dellAllFilesInside(anOutputDirectoryFile);
        }
     }
   }

    /**
     * Get the vendor specific name of the Jar that will be output. The
     * modification date of this jar will be checked against the dependent
     * bean classes.
     **/
    File getVendorOutputJarFile(final String aBaseName) {
        return new File(getDestDir(), aBaseName + jarSuffix);
    }

   /**
    * Create a free tempory directory for GenIC output.
    * @return directory file
    * @throws BuildException if impossible to find a tempory directory
    **/
   private File createTempDir() {
      String theTempDir = System.getProperty("java.io.tmpdir");
      int anIndice = 0;
      File aFile = null;
      // try 50 times to find a free tempory directory
      while( anIndice < 50 && aFile == null) {
         aFile = new File(theTempDir,"GenicTemp" + anIndice);
         if(aFile.exists()) {
            anIndice++;
            aFile = null;
         }
      }

      if( aFile == null ) {
         // problem in temp directory
         throw new BuildException("Impossible to find a free temp directory for output.");
      } else {
         aFile.mkdirs();
         return(aFile);
      }
   }


   /**
    * add all files in anOutputDir + ' / ' + aRootDir to the HashTable someFiles.
    *
    * @param anOutputDir - start directory
    * @param aCurrentDirOrFile - a sub-directory to scan or a file to add.
    * @param someFiles - where to add the files
    **/
   private void addAllFiles(final File anOutputDir, String aCurrentDirOrFile, Hashtable someFiles) {
      File aFile = new File(anOutputDir,aCurrentDirOrFile);
      if( aFile.isDirectory() ) {
         String aCurrentDir = "";
         if( aCurrentDirOrFile.length() > 0 ) {
            aCurrentDir = aCurrentDirOrFile + '/';
         }
         File theFiles[] = aFile.listFiles();
         for(int i=0;i<theFiles.length;i++) {
            addAllFiles(anOutputDir,aCurrentDir + theFiles[i].getName(),someFiles);
         }
      } else {
         // is a file
         someFiles.put(aCurrentDirOrFile,aFile);
      }
   }

   /**
    * Delete all the files in a directory
    * @param aFile file to delete recursivly
    **/
   private void dellAllFiles(File aFile) {
     if(aFile.isDirectory()) {
       File someFiles[] = aFile.listFiles();
       for(int i=0;i<someFiles.length;i++) {
          dellAllFiles(someFiles[i]);
       }
     }
     aFile.delete();
   }
   
   /**
    * Delete all the files in a directory, but don't delete
    * the directory
    * @param aFile file to delete recursivly
    **/   
   private void dellAllFilesInside(File aFile) {
     if(aFile.isDirectory()) {
       File someFiles[] = aFile.listFiles();
       for(int i=0;i<someFiles.length;i++) {
          dellAllFiles(someFiles[i]);
       }
     }      
   }
   
}
// eof.
