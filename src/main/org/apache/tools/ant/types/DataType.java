/*
 * Copyright  2000-2004 The Apache Software Foundation
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

package org.apache.tools.ant.types;


import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Base class for those classes that can appear inside the build file
 * as stand alone data types.
 *
 * <p>This class handles the common description attribute and provides
 * a default implementation for reference handling and checking for
 * circular references that is appropriate for types that can not be
 * nested inside elements of the same type (i.e. &lt;patternset&gt;
 * but not &lt;path&gt;).</p>
 *
 */
public abstract class DataType extends ProjectComponent {
    /**
     * The description the user has set.
     *
     * @deprecated The user should not be directly referencing
     *   variable. Please use {@link #setDescription} or
     *   {@link #getDescription} instead.
     */
    protected String description;

    /**
     * Value to the refid attribute.
     *
     * @deprecated The user should not be directly referencing
     *   variable. Please use {@link #getRefid} instead.
     */
    protected Reference ref;

    /**
     * Are we sure we don't hold circular references?
     *
     * <p>Subclasses are responsible for setting this value to false
     * if we'd need to investigate this condition (usually because a
     * child element has been added that is a subclass of
     * DataType).</p>
     *
     * @deprecated The user should not be directly referencing
     *   variable. Please use {@link #setChecked} or
     *   {@link #isChecked} instead.
     */
    protected boolean checked = true;

    /**
     * Sets a description of the current data type. It will be useful
     * in commenting what we are doing.
     */
    public void setDescription(final String desc) {
        description = desc;
    }

    /**
     * Return the description for the current data type.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Has the refid attribute of this element been set?
     */
    public boolean isReference() {
        return ref != null;
    }

    /**
     * Set the value of the refid attribute.
     *
     * <p>Subclasses may need to check whether any other attributes
     * have been set as well or child elements have been created and
     * thus override this method. if they do the must call
     * <code>super.setRefid</code>.</p>
     */
    public void setRefid(final Reference ref) {
        this.ref = ref;
        checked = false;
    }

    /**
     * Check to see whether any DataType we hold references to is
     * included in the Stack (which holds all DataType instances that
     * directly or indirectly reference this instance, including this
     * instance itself).
     *
     * <p>If one is included, throw a BuildException created by {@link
     * #circularReference circularReference}.</p>
     *
     * <p>This implementation is appropriate only for a DataType that
     * cannot hold other DataTypes as children.</p>
     *
     * <p>The general contract of this method is that it shouldn't do
     * anything if {@link #checked <code>checked</code>} is true and
     * set it to true on exit.</p>
     */
    protected void dieOnCircularReference(final Stack stack,
                                          final Project project)
        throws BuildException {

        if (checked || !isReference()) {
            return;
        }
        Object o = ref.getReferencedObject(project);

        if (o instanceof DataType) {
            if (stack.contains(o)) {
                throw circularReference();
            } else {
                stack.push(o);
                ((DataType) o).dieOnCircularReference(stack, project);
                stack.pop();
            }
        }
        checked = true;
    }

    /**
     * Performs the check for circular references and returns the
     * referenced object.
     */
    protected Object getCheckedRef(final Class requiredClass,
                                   final String dataTypeName) {
        if (!checked) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, getProject());
        }

        Object o = ref.getReferencedObject(getProject());
        if (!(requiredClass.isAssignableFrom(o.getClass()))) {
            log("Class "+o.getClass()+" is not a subclass of "+requiredClass,
                    Project.MSG_VERBOSE);
            String msg = ref.getRefId() + " doesn\'t denote a " + dataTypeName;
            throw new BuildException(msg);
        } else {
            return o;
        }
    }

    /**
     * Creates an exception that indicates that refid has to be the
     * only attribute if it is set.
     */
    protected BuildException tooManyAttributes() {
        return new BuildException("You must not specify more than one "
            + "attribute when using refid");
    }

    /**
     * Creates an exception that indicates that this XML element must
     * not have child elements if the refid attribute is set.
     */
    protected BuildException noChildrenAllowed() {
        return new BuildException("You must not specify nested elements "
            + "when using refid");
    }

    /**
     * Creates an exception that indicates the user has generated a
     * loop of data types referencing each other.
     */
    protected BuildException circularReference() {
        return new BuildException("This data type contains a circular "
            + "reference.");
    }

    protected boolean isChecked() {
        return checked;
    }

    protected void setChecked(final boolean checked) {
        this.checked = checked;
    }

    /**
     * get the reference set on this object
     * @return the reference or null
     */
    protected Reference getRefid() {
        return ref;
    }

    /**
     * check that it is ok to set attributes, i.e that no reference is defined
     * @since Ant 1.6
     * @throws BuildException if not allowed
     */
    protected void checkAttributesAllowed() {
        if (isReference()) {
            throw tooManyAttributes();
        }
    }

    /**
     * check that it is ok to add children, i.e that no reference is defined
     * @since Ant 1.6
     * @throws BuildException if not allowed
     */
    protected void checkChildrenAllowed() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
    }
}
