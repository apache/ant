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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Task to run the WiX utility to create MSI files from an XML description.
 *
 * @see http://sf.net/projects/wix
 */
public class WixTask extends Task {

    /**
     * The vm attribute - if given.
     */
    private String vm;

    /**
     * The source files.
     */
    private ArrayList sources = new ArrayList();

    /**
     * Additional source files (include files in the case of candle,
     * or media/files/whatever in the case of light).
     */
    private ArrayList moreSources = new ArrayList();

    /**
     * A single source file.
     */
    private File source;

    /**
     * The target file.
     */
    private File target;

    /**
     * What to do.
     */
    private Mode mode;

    public WixTask() {
        super();
    }

    /**
     * Set the name of the executable for the virtual machine.
     *
     * @param value the name of the executable for the virtual machine
     */
    public void setVm(String value) {
        this.vm = value;
    }

    /**
     * The main source file.
     *
     * <p><code>candle</code> may include more files than this one,
     * the main source is the one passed on the command line.</p>
     *
     * @param File object of the main source file.
     */
    public void setSource(File f) {
        source = f;
    }

    /**
     * A set of source files.
     */
    public void addSources(FileSet fs) {
        sources.add(fs);
    }

    /**
     * A set of additional source files (include files in the case of
     * candle, or media/files/whatever in the case of light).
     *
     * <p>Unlike the files specified as sources, these will not be
     * passed on the command line, they only help Ant to determine
     * whether the target is out-of-date.</p>
     */
    public void addMoreSources(FileSet fs) {
        moreSources.add(fs);
    }

    public void execute() {
        if (source == null && sources.size() == 0) {
            throw new BuildException("You must specify at least one source"
                                     + " file.");
        }
        String m = Mode.BOTH;
        if (mode != null) {
            m = mode.getValue();
        }

        if (target == null && !m.equals(Mode.CANDLE)) {
            throw new BuildException("You must specify the target if you want"
                                     + " to run light.");
        }

        List lightSources = new ArrayList();
        if (!m.equals(Mode.LIGHT)) {
            doCandle(lightSources);
        } else {
            if (source != null) {
                lightSources.add(source);
            }
            if (sources.size() > 0) {
                lightSources.addAll(grabFiles(sources));
            }
        }
        List moreLightSources = new ArrayList();
        if (moreSources.size() > 0) {
            moreLightSources = grabFiles(moreSources);
        }
        if (!m.equals(Mode.CANDLE)) {
            doLight(lightSources, moreLightSources);
        }
    }

    /**
     * Invoke candle on all sources that are newer than their targets.
     *
     * @param lightSources list that will be filled with File objects
     * pointing to the generated object files.
     */
    private void doCandle(List lightSources) {
        List s = new ArrayList();
        if (source != null) {
            s.add(source);
        }
        if (sources != null) {
            s.addAll(grabFiles(sources));
        }
        List ms = new ArrayList();
        if (moreSources != null) {
            ms.addAll(grabFiles(moreSources));
        }
        Iterator iter = s.iterator();
        List toProcess = new ArrayList();
        while (iter.hasNext()) {
            File thisSource = (File) iter.next();
            File t = target;
            if (t == null) {
                t = getTarget(thisSource);
            }
            if (isOutOfDate(t, thisSource, ms)) {
                toProcess.add(thisSource);
                lightSources.add(t);
            }
        }
        if (toProcess.size() != 0) {
            runCandle(toProcess);
        }
    }

    /**
     * Invoke light on all sources that are newer than their targets.
     */
    private void doLight(List lightSources, List moreLightSources) {
        List tmp = new ArrayList(lightSources);
        tmp.addAll(moreLightSources);
        if (isOutOfDate(target, tmp)) {
            runLight(lightSources);
        }
    }

    /**
     * Run candle passing all files in list on the command line.
     */
    private void runCandle(List s) {
        run("candle.exe", s, null);
    }

    /**
     * Run light passing all files in list on the command line.
     */
    private void runLight(List s) {
        run("light.exe", s, target);
    }

    /**
     * Runs the specified command passing list on the command line an
     * potentially adding an /out parameter.
     */
    private void run(String executable, List s, File target) {
        DotNetExecTask exec = DotNetExecTask.getTask(this, vm, 
                                                     executable, null);
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            File f = (File) iter.next();
            exec.createArg().setValue(f.getAbsolutePath());
        }
        if (target != null) {
            exec.createArg().setValue("/out");
            exec.createArg().setValue(target.getAbsolutePath());
        }
        
        exec.execute();
    }

    /**
     * Is t older than s or any of the files in list?
     */
    private boolean isOutOfDate(File t, File s, List l) {
        return t.lastModified() < s.lastModified() || isOutOfDate(t, l);
    }

    /**
     * Is t older than any of the files in list?
     */
    private boolean isOutOfDate(File t, List l) {
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            File f = (File) iter.next();
            if (t.lastModified() < f.lastModified()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Turn the fileset collection into a list of Files.
     */
    private List grabFiles(List s) {
        List r = new ArrayList();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            FileSet fs = (FileSet) iter.next();
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] f = ds.getIncludedFiles();
            File base = fs.getDir(getProject());
            for (int i = 0; i < f.length; i++) {
                r.add(new File(base, f[i]));
            }
        }
        return r;
    }

    /**
     * Generates the name of a candle target from the source file.
     *
     * <p>Simply chops of the extension and adds .wixobj.</p>
     */
    private File getTarget(File s) {
        String name = s.getAbsolutePath();
        int dot = name.lastIndexOf(".");
        if (dot > -1) {
            return new File(name.substring(0, dot) + ".wixobj");
        } else {
            return new File(name + ".wixobj");
        }
    }

    public static class Mode extends EnumeratedAttribute {
        private final static String CANDLE = "candle";
        private final static String LIGHT = "light";
        private final static String BOTH = "both";

        public Mode() {
            super();
        }

        public String[] getValues() {
            return new String[] {CANDLE, LIGHT, BOTH,};
        }
    }
}