/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;

/**
 * Converts path and classpath information to a specific target OS
 * format. The resulting formatted path is placed into the specified property.
 *
 * @since Ant 1.4
 * @ant.task category="utility"
 */
public class PathConvert extends Task {

    /**
     * Set if we're running on windows
     */
    private static boolean onWindows = Os.isFamily("dos");

    // Members
    /**
     * Path to be converted
     */
    private Resources path = null;
    /**
     * Reference to path/fileset to convert
     */
    private Reference refid = null;
    /**
     * The target OS type
     */
    private String targetOS = null;
    /**
     * Set when targetOS is set to windows
     */
    private boolean targetWindows = false;
    /**
     * Set if we should create a new property even if the result is empty
     */
    private boolean setonempty = true;
    /**
     * The property to receive the conversion
     */
    private String property = null;
    /**
     * Path prefix map
     */
    private List<MapEntry> prefixMap = new Vector<>();
    /**
     * User override on path sep char
     */
    private String pathSep = null;
    /**
     * User override on directory sep char
     */
    private String dirSep = null;

    /** Filename mapper */
    private Mapper mapper = null;

    private boolean preserveDuplicates;

    /**
     * Helper class, holds the nested &lt;map&gt; values. Elements will look like
     * this: &lt;map from=&quot;d:&quot; to=&quot;/foo&quot;/&gt;
     *
     * When running on windows, the prefix comparison will be case
     * insensitive.
     */
    public class MapEntry {

        // Members
        private String from = null;
        private String to = null;

        /**
         * Set the &quot;from&quot; attribute of the map entry.
         * @param from the prefix string to search for; required.
         * Note that this value is case-insensitive when the build is
         * running on a Windows platform and case-sensitive when running on
         * a Unix platform.
         */
        public void setFrom(String from) {
            this.from = from;
        }

        /**
         * Set the replacement text to use when from is matched; required.
         * @param to new prefix.
         */
        public void setTo(String to) {
            this.to = to;
        }

        /**
         * Apply this map entry to a given path element.
         *
         * @param elem Path element to process.
         * @return String Updated path element after mapping.
         */
        public String apply(String elem) {
            if (from == null || to == null) {
                throw new BuildException(
                    "Both 'from' and 'to' must be set in a map entry");
            }
            // If we're on windows, then do the comparison ignoring case
            // and treat the two directory characters the same
            String cmpElem =
                onWindows ? elem.toLowerCase().replace('\\', '/') : elem;
            String cmpFrom =
                onWindows ? from.toLowerCase().replace('\\', '/') : from;

            // If the element starts with the configured prefix, then
            // convert the prefix to the configured 'to' value.

            return cmpElem.startsWith(cmpFrom)
                ? to + elem.substring(from.length()) : elem;
        }
    }

    /**
     * An enumeration of supported targets:
     * "windows", "unix", "netware", and "os/2".
     */
    public static class TargetOs extends EnumeratedAttribute {
        /**
         * @return the list of values for this enumerated attribute.
         */
        @Override
        public String[] getValues() {
            return new String[] {"windows", "unix", "netware", "os/2", "tandem"};
        }
    }

    /**
     * Create a nested path element.
     * @return a Path to be used by Ant reflection.
     */
    public Path createPath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        Path result = new Path(getProject());
        add(result);
        return result;
    }

    /**
     * Add an arbitrary ResourceCollection.
     * @param rc the ResourceCollection to add.
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        getPath().add(rc);
    }

    private synchronized Resources getPath() {
        if (path == null) {
            path = new Resources(getProject());
            path.setCache(true);
        }
        return path;
    }

    /**
     * Create a nested MAP element.
     * @return a Map to configure.
     */
    public MapEntry createMap() {
        MapEntry entry = new MapEntry();
        prefixMap.add(entry);
        return entry;
    }

    /**
     * Set targetos to a platform to one of
     * "windows", "unix", "netware", or "os/2";
     * current platform settings are used by default.
     * @param target the target os.
     * @deprecated since 1.5.x.
     *             Use the method taking a TargetOs argument instead.
     * @see #setTargetos(PathConvert.TargetOs)
     */
    @Deprecated
    public void setTargetos(String target) {
        TargetOs to = new TargetOs();
        to.setValue(target);
        setTargetos(to);
    }

    /**
     * Set targetos to a platform to one of
     * "windows", "unix", "netware", or "os/2";
     * current platform settings are used by default.
     * @param target the target os
     *
     * @since Ant 1.5
     */
    public void setTargetos(TargetOs target) {
        targetOS = target.getValue();

        // Currently, we deal with only two path formats: Unix and Windows
        // And Unix is everything that is not Windows

        // for NetWare and OS/2, piggy-back on Windows, since in the
        // validateSetup code, the same assumptions can be made as
        // with windows - that ; is the path separator

        targetWindows = !"unix".equals(targetOS) && !"tandem".equals(targetOS);
    }

    /**
     * Set whether the specified property will be set if the result
     * is the empty string.
     * @param setonempty true or false.
     *
     * @since Ant 1.5
     */
     public void setSetonempty(boolean setonempty) {
         this.setonempty = setonempty;
     }

    /**
     * Set the name of the property into which the converted path will be placed.
     * @param p the property name.
     */
    public void setProperty(String p) {
        property = p;
    }

    /**
     * Add a reference to a Path, FileSet, DirSet, or FileList defined elsewhere.
     * @param r the reference to a path, fileset, dirset or filelist.
     */
    public void setRefid(Reference r) {
        if (path != null) {
            throw noChildrenAllowed();
        }
        refid = r;
    }

    /**
     * Set the default path separator string; defaults to current JVM
     * {@link java.io.File#pathSeparator File.pathSeparator}.
     * @param sep path separator string.
     */
    public void setPathSep(String sep) {
        pathSep = sep;
    }

    /**
     * Set the default directory separator string;
     * defaults to current JVM {@link java.io.File#separator File.separator}.
     * @param sep directory separator string.
     */
    public void setDirSep(String sep) {
        dirSep = sep;
    }

    /**
     * Set the preserveDuplicates.
     * @param preserveDuplicates the boolean to set
     * @since Ant 1.8
     */
    public void setPreserveDuplicates(boolean preserveDuplicates) {
        this.preserveDuplicates = preserveDuplicates;
    }

    /**
     * Get the preserveDuplicates.
     * @return boolean
     * @since Ant 1.8
     */
    public boolean isPreserveDuplicates() {
        return preserveDuplicates;
    }

    /**
     * Learn whether the refid attribute of this element been set.
     * @return true if refid is valid.
     */
    public boolean isReference() {
        return refid != null;
    }

    /**
     * Do the execution.
     * @throws BuildException if something is invalid.
     */
    @Override
    public void execute() throws BuildException {
        Resources savedPath = path;
        String savedPathSep = pathSep; // may be altered in validateSetup
        String savedDirSep = dirSep; // may be altered in validateSetup

        try {
            // If we are a reference, create a Path from the reference
            if (isReference()) {
                Object o = refid.getReferencedObject(getProject());
                if (!(o instanceof ResourceCollection)) {
                    throw new BuildException(
                        "refid '%s' does not refer to a resource collection.",
                        refid.getRefId());
                }
                getPath().add((ResourceCollection) o);
            }
            validateSetup(); // validate our setup

            // Currently, we deal with only two path formats: Unix and Windows
            // And Unix is everything that is not Windows
            // (with the exception for NetWare and OS/2 below)

            // for NetWare and OS/2, piggy-back on Windows, since here and
            // in the apply code, the same assumptions can be made as with
            // windows - that \\ is an OK separator, and do comparisons
            // case-insensitive.
            String fromDirSep = onWindows ? "\\" : "/";

            StringBuilder rslt = new StringBuilder();

            ResourceCollection resources = isPreserveDuplicates() ? path : new Union(path);
            List<String> ret = new ArrayList<>();
            FileNameMapper mapperImpl = mapper == null ? new IdentityMapper() : mapper.getImplementation();
            for (Resource r : resources) {
                String[] mapped = mapperImpl.mapFileName(String.valueOf(r));
                for (int m = 0; mapped != null && m < mapped.length; ++m) {
                    ret.add(mapped[m]);
                }
            }
            boolean first = true;
            for (String string : ret) {
                String elem = mapElement(string); // Apply the path prefix map

                // Now convert the path and file separator characters from the
                // current os to the target os.

                if (!first) {
                    rslt.append(pathSep);
                }
                first = false;

                StringTokenizer stDirectory = new StringTokenizer(elem, fromDirSep, true);

                while (stDirectory.hasMoreTokens()) {
                    String token = stDirectory.nextToken();
                    rslt.append(fromDirSep.equals(token) ? dirSep : token);
                }
            }
            // Place the result into the specified property,
            // unless setonempty == false
            if (setonempty || rslt.length() > 0) {
                String value = rslt.toString();
                if (property == null) {
                    log(value);
                } else {
                    log("Set property " + property + " = " + value, Project.MSG_VERBOSE);
                    getProject().setNewProperty(property, value);
                }
            }
        } finally {
            path = savedPath;
            dirSep = savedDirSep;
            pathSep = savedPathSep;
        }
    }

    /**
     * Apply the configured map to a path element. The map is used to convert
     * between Windows drive letters and Unix paths. If no map is configured,
     * then the input string is returned unchanged.
     *
     * @param elem The path element to apply the map to.
     * @return String Updated element.
     */
    private String mapElement(String elem) {
        // Iterate over the map entries and apply each one.
        // Stop when one of the entries actually changes the element.

        for (MapEntry entry : prefixMap) {
            String newElem = entry.apply(elem);

            // Note I'm using "!=" to see if we got a new object back from
            // the apply method.

            if (newElem != elem) {
                return newElem;
            }
        }
        return elem;
    }

    /**
     * Add a mapper to convert the file names.
     *
     * @param mapper a <code>Mapper</code> value.
     */
    public void addMapper(Mapper mapper) {
        if (this.mapper != null) {
            throw new BuildException(
                "Cannot define more than one mapper");
        }
        this.mapper = mapper;
    }

    /**
     * Add a nested filenamemapper.
     * @param fileNameMapper the mapper to add.
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        Mapper m = new Mapper(getProject());
        m.add(fileNameMapper);
        addMapper(m);
    }

    /**
     * Validate that all our parameters have been properly initialized.
     *
     * @throws BuildException if something is not set up properly.
     */
    private void validateSetup() throws BuildException {

        if (path == null) {
            throw new BuildException("You must specify a path to convert");
        }
        // Determine the separator strings.  The dirsep and pathsep attributes
        // override the targetOS settings.
        String dsep = File.separator;
        String psep = File.pathSeparator;

        if (targetOS != null) {
            psep = targetWindows ? ";" : ":";
            dsep = targetWindows ? "\\" : "/";
        }
        if (pathSep != null) {
            // override with pathsep=
            psep = pathSep;
        }
        if (dirSep != null) {
            // override with dirsep=
            dsep = dirSep;
        }
        pathSep = psep;
        dirSep = dsep;
    }

    /**
     * Creates an exception that indicates that this XML element must not have
     * child elements if the refid attribute is set.
     * @return BuildException.
     */
    private BuildException noChildrenAllowed() {
        return new BuildException(
            "You must not specify nested elements when using the refid attribute.");
    }

}
