package org.apache.tools.ant.taskdefs.optional;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

/**
 * Task for creating a manifest file for a jar archiv.
 * use:
 * <pre>
 *   <taskdef name="manifest" classname="ManifestFile"/>
 *   <target name="jar_manifest">
 *     <manifest file="manifest.mf" method="replaceAll">
 *         <entry value="Manifest-Version: 1_0"/>
 *     </manifest>
 *   </target>
 * </pre>
 *
 *@version 1.0 2001-10-11
 *@author Thomas Kerle
 */
public class ManifestFile extends Task {

    private static final String newLine = System.getProperty("line.separator");
    private static final String keyValueSeparator = ":";
    private static final String UPDATE_ = "update";
    private static final String REPLACEALL_ = "replaceAll";

    private File manifestFile;
    private Vector entries;
    private EntryContainer container;
    private String currentMethod;

    public ManifestFile() {
        entries = new Vector();
        container = new EntryContainer();
    }

    /**
     * execute task
     * @exception BuildException : Failure in building
     */
    public void execute() throws BuildException {
        checkParameters();
        if (isUpdate(currentMethod))
            readFile();

        executeOperation();
        writeFile();
    }

    /**
     * adding entries to a container
     * @exception BuildException
     */
    private void executeOperation() throws BuildException {
        Enumeration enum = entries.elements();

        while (enum.hasMoreElements()) {
            Entry entry = (Entry) enum.nextElement();
            entry.addTo (container);
        }
    }

    /**
     * creating entries by Ant
     *
     *
     */
    public Entry createEntry() {
        Entry entry = new Entry();
        entries.addElement(entry);
        return entry;
    }


    private boolean isUpdate (String method) {
        return method.equals(UPDATE_.toUpperCase());
    }

    private boolean isReplaceAll (String method) {
        return method.equals(REPLACEALL_.toUpperCase());
    }

    /**
     * Setter for the method attribute (update/replaceAll)
     * @param method Method to set task
     */
    public void setMethod (String method) {
        currentMethod = method.toUpperCase();
    }

    /**
     * Setter for the file attribute
     * @param filename for the manifest
     */
    public void setFile(File f) {
        manifestFile = f;
    }


    private StringBuffer buildBuffer () {
        StringBuffer buffer = new StringBuffer ();

        ListIterator iterator = container.elements();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();

            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String entry_string = key + keyValueSeparator + value;

            buffer.append (entry_string + this.newLine);
        }

        return buffer;
    }


    private void writeFile() throws BuildException {
        try {
            manifestFile.delete();
            log ("Replacing or creating new manifest file " + manifestFile.getAbsolutePath());
            if (manifestFile.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(manifestFile);

                StringBuffer buffer = buildBuffer();

                int size = buffer.length();

                for (int i=0; i<size; i++) {
                    fos.write( (char) buffer.charAt(i));
                }

                fos.flush();
                fos.close();
            } else {
                throw new BuildException ("Can't create manifest file");
            }

        } catch (IOException ioe) {
            throw new BuildException ("An input/ouput error occured" + ioe.toString());
        }
    }

    private StringTokenizer getLineTokens (StringBuffer buffer) {
        String manifests = buffer.toString();
        StringTokenizer strTokens = new StringTokenizer(manifests, newLine);
        return strTokens;
    }

    private void addLine (String line) {
        Entry entry = new Entry();

        entry.setValue (line);
        entry.addTo(container);
    }

    private void readFile() throws BuildException {

        if (manifestFile.exists()) {
            this.log("update existing manifest file " + manifestFile.getAbsolutePath());

            if (container != null) {
                try {
                    FileInputStream fis = new FileInputStream(manifestFile);

                    int c;
                    StringBuffer buffer = new StringBuffer("");
                    boolean stop = false;
                    while (!stop) {
                        c = fis.read();
                        if (c == -1){
                            stop =true;
                        } else
                            buffer.append( (char) c);
                    }
                    fis.close();
                    StringTokenizer lineTokens = getLineTokens (buffer);
                    while (lineTokens.hasMoreElements()) {
                        String currentLine = (String) lineTokens.nextElement();
                        addLine (currentLine);
                    }
                }
                catch (FileNotFoundException fnfe) {
                    throw new BuildException ("File not found exception " + fnfe.toString());
                }
                catch (IOException ioe) {
                    throw new BuildException ("Unknown input/output exception " + ioe.toString());
                }
            }
        }

    }

    private void checkParameters() throws BuildException {
        if (!checkParam(manifestFile)) {
            throw new BuildException ("file token must not be null.", location);
        }
    }

    private boolean checkParam (String param) {
        return !((param==null) || (param.equals("null")));
    }

    private boolean checkParam (File param) {
        return !(param == null);
    }

    public class EntryContainer {

        private ArrayList list = null;

        public EntryContainer () {
            list = new ArrayList();
        }

        public void set (Entry entry) {

            if (list.contains(entry)) {
                int index = list.indexOf(entry);

                list.remove(index);
                list.add(index, entry);
            } else {
                list.add(entry);
            }
        }

        public ListIterator elements() {
            ListIterator iterator = list.listIterator();
            return iterator;
        }
    }

    public class Entry implements Comparator {
        //extern format
        private String value = null;

        //intern representation
        private String val = null;
        private String key = null;

        public Entry () {

        }

        public void setValue (String value) {
            this.value = new String(value);
        }

        public String getKey () {
            return key;
        }

        public String getValue() {
            return val;
        }

        private void checkFormat () throws BuildException {

            if (value==null) {
                throw new BuildException ("no argument for value");
            }

            StringTokenizer st = new StringTokenizer(value, ManifestFile.keyValueSeparator);
            int size = st.countTokens();

            if (size < 2 ) {
                throw new BuildException ("value has not the format of a manifest entry");
            }
        }

        private void split () {
            StringTokenizer st = new StringTokenizer(value, ManifestFile.keyValueSeparator);
            key = (String) st.nextElement();
            val = (String) st.nextElement();
        }

        public int compare (Object o1, Object o2) {
            int result = -1;

            try {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;

                String key_1 = e1.getKey();
                String key_2 = e2.getKey();


                result = key_1.compareTo(key_2);
            } catch (Exception e) {

            }
            return result;
        }


        public boolean equals (Object obj) {
            Entry ent = new Entry();
            boolean result = false;
            int res = ent.compare (this,(Entry) obj );
            if (res==0)
                result =true;

            return result;
        }


        protected void addTo (EntryContainer container) throws BuildException {
            checkFormat();
            split();
            container.set(this);
        }

    }
}
