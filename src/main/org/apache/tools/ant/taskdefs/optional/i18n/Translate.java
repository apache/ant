/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Locale;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.taskdefs.MatchingTask;

/**
 * Translates text embedded in files using Resource Bundle files.
 *
 * @author Magesh Umasankar
 */
public class Translate extends MatchingTask {

    /**
     * Family name of resource bundle
     */
    private String bundle;
    /**
     * Locale specific language of the resource bundle
     */
    private String bundleLanguage;
    /**
     * Locale specific country of the resource bundle
     */
    private String bundleCountry;
    /**
     * Locale specific variant of the resource bundle
     */
    private String bundleVariant;
    /**
     * Destination directory
     */
    private File toDir;
    /**
     * Source file encoding scheme
     */
    private String srcEncoding;
    /**
     * Destination file encoding scheme
     */
    private String destEncoding;
    /**
     * Resource Bundle file encoding scheme, defaults to srcEncoding
     */
    private String bundleEncoding;
    /**
     * Starting token to identify keys
     */
    private String startToken;
    /**
     * Ending token to identify keys
     */
    private String endToken;
    /**
     * Whether or not to create a new destination file.
     * Defaults to <code>false</code>.
     */
    private boolean forceOverwrite;
    /**
     * Vector to hold source file sets.
     */
    private Vector filesets = new Vector();
    /**
     * Holds key value pairs loaded from resource bundle file
     */
    private Hashtable resourceMap = new Hashtable();
    /**
     * Generated locale based on user attributes
     */
    private Locale locale;
    /**
     * Used to resolve file names.
     */
    private FileUtils fileUtils = FileUtils.newFileUtils();
    /**
     * Last Modified Timestamp of resource bundle file being used.
     */
    private long[] bundleLastModified = new long[7];
    /**
     * Last Modified Timestamp of source file being used.
     */
    private long srcLastModified;
    /**
     * Last Modified Timestamp of destination file being used.
     */
    private long destLastModified;
    /**
     * Has at least one file from the bundle been loaded?
     */
    private boolean loaded = false;

    /**
     * Sets Family name of resource bundle; required.
     */
    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    /**
     * Sets locale specific language of resource bundle; optional.
     */
    public void setBundleLanguage(String bundleLanguage) {
        this.bundleLanguage = bundleLanguage;
    }

    /**
     * Sets locale specific country of resource bundle; optional.
     */
    public void setBundleCountry(String bundleCountry) {
        this.bundleCountry = bundleCountry;
    }

    /**
     * Sets locale specific variant of resource bundle; optional.
     */
    public void setBundleVariant(String bundleVariant) {
        this.bundleVariant = bundleVariant;
    }

    /**
     * Sets Destination directory; required.
     */
    public void setToDir(File toDir) {
        this.toDir = toDir;
    }

    /**
     * Sets starting token to identify keys; required.
     */
    public void setStartToken(String startToken) {
        this.startToken = startToken;
    }

    /**
     * Sets ending token to identify keys; required.
     */
    public void setEndToken(String endToken) {
        this.endToken = endToken;
    }

    /**
     * Sets source file encoding scheme; optional,
     * defaults to encoding of local system.
     */
    public void setSrcEncoding(String srcEncoding) {
        this.srcEncoding = srcEncoding;
    }

    /**
     * Sets destination file encoding scheme; optional.  Defaults to source file
     * encoding
     */
    public void setDestEncoding(String destEncoding) {
        this.destEncoding = destEncoding;
    }

    /**
     * Sets Resource Bundle file encoding scheme; optional.  Defaults to source file
     * encoding
     */
    public void setBundleEncoding(String bundleEncoding) {
        this.bundleEncoding = bundleEncoding;
    }

    /**
     * Whether or not to overwrite existing file irrespective of
     * whether it is newer than the source file as well as the
     * resource bundle file.
     * Defaults to false.
     */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
    }

    /**
     * Adds a set of files to translate as a nested fileset element.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Check attributes values, load resource map and translate
     */
    public void execute() throws BuildException {
        if (bundle == null) {
            throw new BuildException("The bundle attribute must be set.",
                                     location);
        }

        if (startToken == null) {
            throw new BuildException("The starttoken attribute must be set.",
                                     location);
        }

        if (startToken.length() != 1) {
            throw new BuildException(
                "The starttoken attribute must be a single character.",
                                         location);
        }

        if (endToken == null) {
            throw new BuildException("The endtoken attribute must be set.",
                                     location);
        }

        if (endToken.length() != 1) {
            throw new BuildException(
                "The endtoken attribute must be a single character.",
                                         location);
        }

        if (bundleLanguage == null) {
            Locale l = Locale.getDefault();
            bundleLanguage  = l.getLanguage();
        }

        if (bundleCountry == null) {
            bundleCountry = Locale.getDefault().getCountry();
        }

        locale = new Locale(bundleLanguage, bundleCountry);

        if (bundleVariant == null) {
            Locale l = new Locale(bundleLanguage, bundleCountry);
            bundleVariant = l.getVariant();
        }

        if (toDir == null) {
            throw new BuildException("The todir attribute must be set.",
                                     location);
        }

        if (!toDir.exists()) {
            toDir.mkdirs();
        } else {
            if (toDir.isFile()) {
                throw new BuildException(toDir + " is not a directory");
            }
        }

        if (srcEncoding == null) {
            srcEncoding = System.getProperty("file.encoding");
        }

        if (destEncoding == null) {
            destEncoding = srcEncoding;
        }

        if (bundleEncoding == null) {
            bundleEncoding = srcEncoding;
        }

        loadResourceMaps();

        translate();
    }

    /**
     * Load resource maps based on resource bundle encoding scheme.
     * The resource bundle lookup searches for resource files with various
     * suffixes on the basis of (1) the desired locale and (2) the default
     * locale (basebundlename), in the following order from lower-level
     * (more specific) to parent-level (less specific):
     *
     * basebundlename + "_" + language1 + "_" + country1 + "_" + variant1
     * basebundlename + "_" + language1 + "_" + country1
     * basebundlename + "_" + language1
     * basebundlename
     * basebundlename + "_" + language2 + "_" + country2 + "_" + variant2
     * basebundlename + "_" + language2 + "_" + country2
     * basebundlename + "_" + language2
     *
     * To the generated name, a ".properties" string is appeneded and
     * once this file is located, it is treated just like a properties file
     * but with bundle encoding also considered while loading.
     */
    private void loadResourceMaps() throws BuildException {
        Locale locale = new Locale(bundleLanguage,
                                   bundleCountry,
                                   bundleVariant);
        String language = locale.getLanguage().length() > 0 ?
            "_" + locale.getLanguage() :
            "";
        String country = locale.getCountry().length() > 0 ?
            "_" + locale.getCountry() :
            "";
        String variant = locale.getVariant().length() > 0 ?
            "_" + locale.getVariant() :
            "";
        String bundleFile = bundle + language + country + variant;
        processBundle(bundleFile, 0, false);

        bundleFile = bundle + language + country;
        processBundle(bundleFile, 1, false);

        bundleFile = bundle + language;
        processBundle(bundleFile, 2, false);

        bundleFile = bundle;
        processBundle(bundleFile, 3, false);

        //Load default locale bundle files
        //using default file encoding scheme.
        locale = Locale.getDefault();

        language = locale.getLanguage().length() > 0 ?
            "_" + locale.getLanguage() :
            "";
        country = locale.getCountry().length() > 0 ?
            "_" + locale.getCountry() :
            "";
        variant = locale.getVariant().length() > 0 ?
            "_" + locale.getVariant() :
            "";
        bundleEncoding = System.getProperty("file.encoding");

        bundleFile = bundle + language + country + variant;
        processBundle(bundleFile, 4, false);

        bundleFile = bundle + language + country;
        processBundle(bundleFile, 5, false);

        bundleFile = bundle + language;
        processBundle(bundleFile, 6, true);
    }

    /**
     * Process each file that makes up this bundle.
     */
    private void processBundle(final String bundleFile, final int i,
                               final boolean checkLoaded) throws BuildException {
        final File propsFile = new File(bundleFile + ".properties");
        FileInputStream ins = null;
        try {
            ins = new FileInputStream(propsFile);
            loaded = true;
            bundleLastModified[i] = propsFile.lastModified();
            log("Using " + propsFile, Project.MSG_DEBUG);
            loadResourceMap(ins);
        } catch (IOException ioe) {
            log(propsFile + " not found.", Project.MSG_DEBUG);
            //if all resource files associated with this bundle
            //have been scanned for and still not able to
            //find a single resrouce file, throw exception
            if (!loaded && checkLoaded) {
                throw new BuildException(ioe.getMessage(), location);
            }
        }
    }

    /**
     * Load resourceMap with key value pairs.  Values of existing keys
     * are not overwritten.  Bundle's encoding scheme is used.
     */
    private void loadResourceMap(FileInputStream ins) throws BuildException {
        try {
            BufferedReader in = null;
            InputStreamReader isr = new InputStreamReader(ins, bundleEncoding);
            in = new BufferedReader(isr);
            String line = null;
            while ((line = in.readLine()) != null) {
                //So long as the line isn't empty and isn't a comment...
                if (line.trim().length() > 1 && '#' != line.charAt(0) && '!' != line.charAt(0)) {
                    //Legal Key-Value separators are :, = and white space.
                    int sepIndex = line.indexOf('=');
                    if (-1 == sepIndex) {
                        sepIndex = line.indexOf(':');
                    }
                    if (-1 == sepIndex) {
                        for (int k = 0; k < line.length(); k++) {
                            if (Character.isSpaceChar(line.charAt(k))) {
                                sepIndex = k;
                                break;
                            }
                        }
                    }
                    //Only if we do have a key is there going to be a value
                    if (-1 != sepIndex) {
                        String key = line.substring(0, sepIndex).trim();
                        String value = line.substring(sepIndex + 1).trim();
                        //Handle line continuations, if any
                        while (value.endsWith("\\")) {
                            value = value.substring(0, value.length() - 1);
                            if ((line = in.readLine()) != null) {
                                value = value + line.trim();
                            } else {
                                break;
                            }
                        }
                        if (key.length() > 0) {
                            //Has key already been loaded into resourceMap?
                            if (resourceMap.get(key) == null) {
                                resourceMap.put(key, value);
                            }
                        }
                    }
                }
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException ioe) {
            throw new BuildException(ioe.getMessage(), location);
        }
    }

    /**
     * Reads source file line by line using the source encoding and
     * searches for keys that are sandwiched between the startToken
     * and endToken.  The values for these keys are looked up from
     * the hashtable and substituted.  If the hashtable doesn't
     * contain the key, they key itself is used as the value.
     * Detination files and directories are created as needed.
     * The destination file is overwritten only if
     * the forceoverwritten attribute is set to true if
     * the source file or any associated bundle resource file is
     * newer than the destination file.
     */
    private void translate() throws BuildException {
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            String[] srcFiles = ds.getIncludedFiles();
            for (int j = 0; j < srcFiles.length; j++) {
                try {
                    File dest = fileUtils.resolveFile(toDir, srcFiles[j]);
                    //Make sure parent dirs exist, else, create them.
                    try {
                        File destDir = new File(dest.getParent());
                        if (!destDir.exists()) {
                            destDir.mkdirs();
                        }
                    } catch (Exception e) {
                        log("Exception occured while trying to check/create "
                            + " parent directory.  " + e.getMessage(),
                            Project.MSG_DEBUG);
                    }
                    destLastModified = dest.lastModified();
                    File src = fileUtils.resolveFile(ds.getBasedir(), srcFiles[j]);
                    srcLastModified = src.lastModified();
                    //Check to see if dest file has to be recreated
                    if (forceOverwrite
                        || destLastModified < srcLastModified
                        || destLastModified < bundleLastModified[0]
                        || destLastModified < bundleLastModified[1]
                        || destLastModified < bundleLastModified[2]
                        || destLastModified < bundleLastModified[3]
                        || destLastModified < bundleLastModified[4]
                        || destLastModified < bundleLastModified[5]
                        || destLastModified < bundleLastModified[6]) {
                        log("Processing " + srcFiles[j],
                            Project.MSG_DEBUG);
                        FileOutputStream fos = new FileOutputStream(dest);
                        BufferedWriter out
                            = new BufferedWriter(new OutputStreamWriter(fos, destEncoding));
                        FileInputStream fis = new FileInputStream(src);
                        BufferedReader in
                            = new BufferedReader(new InputStreamReader(fis, srcEncoding));
                        String line;
                        while ((line = in.readLine()) != null) {
                            int startIndex = -1;
                            int endIndex = -1;
outer:                      while (true) {
                                startIndex = line.indexOf(startToken, endIndex + 1);
                                if (startIndex < 0 ||
                                    startIndex + 1 >= line.length()) {
                                    break;
                                }
                                endIndex = line.indexOf(endToken, startIndex + 1);
                                if (endIndex < 0) {
                                    break;
                                }
                                String matches = line.substring(startIndex + 1,
                                                                endIndex);
                                    //If there is a white space or = or :, then
                                    //it isn't to be treated as a valid key.
                                for (int k = 0; k < matches.length(); k++) {
                                    char c = matches.charAt(k);
                                    if (c == ':' ||
                                        c == '=' ||
                                        Character.isSpaceChar(c)) {
                                        endIndex = endIndex - 1;
                                        continue outer;
                                    }
                                }
                                String replace = null;
                                replace = (String) resourceMap.get(matches);
                                    //If the key hasn't been loaded into resourceMap,
                                    //use the key itself as the value also.
                                if (replace == null) {
                                    log("Warning: The key: " + matches
                                        + " hasn't been defined.",
                                        Project.MSG_DEBUG);
                                    replace = matches;
                                }
                                line = line.substring(0, startIndex)
                                    + replace
                                    + line.substring(endIndex + 1);
                                endIndex = startIndex + replace.length() + 1;
                                if (endIndex + 1 >= line.length()) {
                                    break;
                                }
                            }
                            out.write(line);
                            out.newLine();
                        }
                        if (in != null) {
                            in.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    } else {
                        log("Skipping " + srcFiles[j] +
                            " as destination file is up to date",
                            Project.MSG_VERBOSE);
                    }
                } catch (IOException ioe) {
                    throw new BuildException(ioe.getMessage(), location);
                }
            }
        }
    }
}
