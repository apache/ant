/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.repository;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This task will retrieve one or more libraries from a repository. <ol>
 * <li>Users must declare a repository, either inline or by reference</li>
 * <li>Dependency checking is used (timestamps) unless forceDownload=true</li>
 * <li>It is an error if, at the end of the task, a library is missing.
 *
 * @ant.task
 * @since Ant 1.7
 */
public class GetLibraries extends Task {

    /**
     * destination
     */
    private File destDir;

    /**
     * flag to force a download
     */
    private boolean forceDownload = false;

    /**
     * flag to force offline
     */
    private boolean offline = false;

    /**
     * list of libraries
     */
    private List libraries = new LinkedList();

    /**
     * repository for retrieval
     */

    private Repository repository;

    /**
     * Optional. A name for a path to define from the dependencies specified.
     */
    private String pathid;



    public static final String ERROR_ONE_REPOSITORY_ONLY = "Only one repository is allowed";
    public static final String ERROR_NO_DEST_DIR = "No destination directory";
    public static final String ERROR_NO_REPOSITORY = "No repository defined";
    public static final String ERROR_NO_LIBRARIES = "No libraries to load";
    public static final String ERROR_REPO_PROBE_FAILED = "repository probe failed with ";
    public static final String ERROR_LIBRARY_FETCH_FAILED = "failed to retrieve ";
    public static final String ERROR_FORCED_DOWNLOAD_FAILED = "Failed to download every file on a forced download";
    public static final String ERROR_INCOMPLETE_RETRIEVAL = "Not all libraries could be retrieved";

    /**
     * add a repository. Only one is (currently) supported
     *
     * @param repo
     */
    public void add(Repository repo) {
        if (repository != null) {
            throw new BuildException(ERROR_ONE_REPOSITORY_ONLY);
        }
        repository = repo;
    }


    /**
     * add a repository. Unless there is explicit support for a subclass
     *
     * @param repo
     */
    public void addRepository(RepositoryRef repo) {
        add(repo);
    }


    /**
     * add a maven repository.
     */
/*
    public void addMavenRepository(MavenRepository repo) {
        add(repo);
    }
*/

    /**
     * bind to a repository.
     */
    public void setRepositoryRef(final Reference ref) {
        //create a special repository that can only
        //resolve references.
        Repository r = new RepositoryRef(getProject(), ref);
        add(r);
    }


    /**
     * add a library for retrieval
     *
     * @param lib
     */
    public void addLibrary(Library lib) {
        libraries.add(lib);
    }

    /**
     * destination directory for all library files
     *
     * @param destDir
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * flag to force a download even if the clock indicates it aint needed.
     *
     * @param forceDownload
     */
    public void setForceDownload(boolean forceDownload) {
        this.forceDownload = forceDownload;
    }

    /**
     * test for being offline
     *
     * @return true if the offline flag is set
     */
    public boolean isOffline() {
        return offline;
    }

    /**
     * declare the system offline. This disables any attempt to retrieve files.
     * In this mode, only the presence of files is verified. If forceDownload is
     * true, or there is a missing library, then an error will be raised.
     *
     * @param offline
     */
    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public File getDestDir() {
        return destDir;
    }

    /**
     * get fore download flag
     * @return
     */
    public boolean isForceDownload() {
        return forceDownload;
    }

    /**
     * get the list of libraries
     * @return
     */
    public List getLibraries() {
        return libraries;
    }

    /**
     * get our repository
     * @return
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * get the pathID if defined
     * @return
     */
    public String getPathid() {
        return pathid;
    }

    /**
     * the name of a path reference to be created referring
     * to the libraries.
     * @param pathid
     */
    public void setPathid(String pathid) {
        this.pathid = pathid;
    }

    /**
     * validate ourselves
     *
     * @throws BuildException
     */
    public void validate() {
        if (destDir == null || !destDir.exists() || !destDir.isDirectory()) {
            throw new BuildException(ERROR_NO_DEST_DIR);
        }
        if (repository == null) {
            throw new BuildException(ERROR_NO_REPOSITORY);
        }
        Iterator it = libraries.iterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            library.validate();
        }
    }

    /**
     * Called by the project to let the task do its work.
     *
     * @throws org.apache.tools.ant.BuildException
     *          if something goes wrong with the build
     */
    public void execute() throws BuildException {
        validate();
        Repository repo = repository.resolve();
        repo.validate();
        if (libraries.size() == 0) {
            throw new BuildException(ERROR_NO_LIBRARIES);
        }
        int failures = 0;
        log("Getting libraries from " + repo.toString(), Project.MSG_VERBOSE);
        log("Saving libraries to " + destDir.toString(), Project.MSG_VERBOSE);

        bindAllLibraries();
        if (isOffline()) {
            log("No retrieval, task is \"offline\"");
            //when offline, we just make sure everything is in place
            verifyAllLibrariesPresent();
            return;
        }


        //connect the repository
        repo.connect(this);
        try {

            //check for reachability.
            //it is up to each repository to decide that.
            boolean reachable;
            try {
                log("Checking repository for reachability", Project.MSG_DEBUG);
                reachable = repo.checkRepositoryReachable();
            } catch (IOException e) {

                log(ERROR_REPO_PROBE_FAILED + e.getMessage(),
                        Project.MSG_VERBOSE);
                reachable = false;
            }
            if (!reachable) {
                if (forceDownload) {
                    throw new BuildException(repo.toString()
                            + " is unreachable and forceDownload is set");
                }
            } else {
                log("Repository is live", Project.MSG_DEBUG);
            }

            //iterate through the libs we have
            Iterator it = filteredIterator();
            while (it.hasNext()) {
                Library library = (Library) it.next();
                try {
                    //fetch it
                    if (repo.fetch(library)) {
                    }
                } catch (IOException e) {
                    //failures to fetch are logged at verbose level
                    log(ERROR_LIBRARY_FETCH_FAILED + library);
                    log(e.getMessage(), Project.MSG_VERBOSE);
                    //add failures
                    failures++;
                }
            }
        } finally {
            repo.disconnect();
        }

        //at this point downloads have finished.
        //we do still need to verify that everything worked.
        if ((failures>0 && forceDownload)) {
            throw new BuildException(ERROR_FORCED_DOWNLOAD_FAILED);
        }

        //validate the download
        verifyAllLibrariesPresent();

        //create the path
        if(pathid!=null) {
            createPath();
        }

    }

    /**
     * bind all libraries to our destination
     */
    protected void bindAllLibraries() {
        Iterator it = libraries.iterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            library.bind(destDir);
        }
    }

    /**
     * verify that all libraries are present
     */
    protected void verifyAllLibrariesPresent() {
        //iterate through the libs we have
        boolean missing = false;

        Iterator it = filteredIterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            //check for the library existing
            if (!library.exists()) {
                //and log if one is missing
                log("Missing: " + library.toString(),
                        Project.MSG_ERR);
                missing = true;
            }
        }
        if (missing) {
            throw new BuildException(ERROR_INCOMPLETE_RETRIEVAL);
        }
    }

    /**
     * create a path; requires pathID!=null
     */
    private void createPath() {
        Path path = new Path(getProject());
        for (Iterator iterator = filteredIterator();
             iterator.hasNext();) {
            ((Library) iterator.next()).appendToPath(path);
        }
        getProject().addReference(pathid, path);
    }

    /**
     * get a filtered iterator of the dependencies
     * @return a new iterator that ignores disabled libraries
     */
    protected Iterator filteredIterator() {
        return new LibraryIterator(libraries,getProject());
    }

    /**
     * iterator through a list that skips everything that
     * is not enabled
     */
    private static class LibraryIterator implements Iterator {
        private Iterator _underlyingIterator;
        private Library _next;
        private Project _project;


        /**
         * constructor
         * @param collection
         * @param project
         */
        LibraryIterator(Collection collection, Project project) {
            _project = project;
            _underlyingIterator = collection.iterator();
        }


        /**
         * test for having another enabled component
         * @return
         */
        public boolean hasNext() {
            while (_next == null && _underlyingIterator.hasNext()) {
                Library candidate = (Library) _underlyingIterator.next();
                if (candidate.isEnabled(_project)) {
                    _next = candidate;
                }
            }
            return (_next != null);
        }


        /**
         * get the next element
         * @return
         */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Library result = _next;
            _next = null;
            return result;
        }


        /**
         * removal is not supported
         * @throws UnsupportedOperationException always
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


}
