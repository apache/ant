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

package org.apache.tools.ant.types;

import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.util.IdentityStack;

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
public abstract class DataType extends ProjectComponent implements Cloneable {
    // CheckStyle:VisibilityModifier OFF

    /**
     * Value to the refid attribute.
     *
     * @deprecated since 1.7.
     *             The user should not be directly referencing
     *             variable. Please use {@link #getRefid} instead.
     */
    @Deprecated
    protected Reference ref;

    /**
     * Are we sure we don't hold circular references?
     *
     * <p>Subclasses are responsible for setting this value to false
     * if we'd need to investigate this condition (usually because a
     * child element has been added that is a subclass of
     * DataType).</p>
     *
     * @deprecated since 1.7.
     *             The user should not be directly referencing
     *             variable. Please use {@link #setChecked} or
     *             {@link #isChecked} instead.
     */
    @Deprecated
    protected boolean checked = true;
    // CheckStyle:VisibilityModifier ON

    /**
     * Has the refid attribute of this element been set?
     * @return true if the refid attribute has been set
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
     * @param ref the reference to use
     */
    public void setRefid(final Reference ref) {
        this.ref = ref;
        checked = false;
    }

    /**
     * Gets as descriptive as possible a name used for this datatype instance.
     * @return <code>String</code> name.
     */
    protected String getDataTypeName() {
        return ComponentHelper.getElementName(getProject(), this, true);
    }

    /**
     * Convenience method.
     * @since Ant 1.7
     */
    protected void dieOnCircularReference() {
        dieOnCircularReference(getProject());
    }

    /**
     * Convenience method.
     * @param p the Ant Project instance against which to resolve references.
     * @since Ant 1.7
     */
    protected void dieOnCircularReference(Project p) {
        if (checked || !isReference()) {
            return;
        }
        dieOnCircularReference(new IdentityStack<>(this), p);
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
     * anything if {@link #checked} is true and
     * set it to true on exit.</p>
     * @param stack the stack of references to check.
     * @param project the project to use to dereference the references.
     * @throws BuildException on error.
     */
    protected void dieOnCircularReference(final Stack<Object> stack,
                                          final Project project)
        throws BuildException {

        if (checked || !isReference()) {
            return;
        }
        Object o = ref.getReferencedObject(project);

        if (o instanceof DataType) {
            IdentityStack<Object> id = IdentityStack.getInstance(stack);

            if (id.contains(o)) {
                throw circularReference();
            } else {
                id.push(o);
                ((DataType) o).dieOnCircularReference(id, project);
                id.pop();
            }
        }
        checked = true;
    }

    /**
     * Allow DataTypes outside org.apache.tools.ant.types to indirectly call
     * dieOnCircularReference on nested DataTypes.
     * @param dt the DataType to check.
     * @param stk the stack of references to check.
     * @param p the project to use to dereference the references.
     * @throws BuildException on error.
     * @since Ant 1.7
     */
    public static void invokeCircularReferenceCheck(DataType dt, Stack<Object> stk,
                                                    Project p) {
        dt.dieOnCircularReference(stk, p);
    }

    /**
     * Allow DataTypes outside org.apache.tools.ant.types to indirectly call
     * dieOnCircularReference on nested DataTypes.
     *
     * <p>Pushes dt on the stack, runs dieOnCircularReference and pops
     * it again.</p>
     * @param dt the DataType to check.
     * @param stk the stack of references to check.
     * @param p the project to use to dereference the references.
     * @throws BuildException on error.
     * @since Ant 1.8.0
     */
    public static void pushAndInvokeCircularReferenceCheck(DataType dt,
                                                           Stack<Object> stk,
                                                           Project p) {
        stk.push(dt);
        dt.dieOnCircularReference(stk, p);
        stk.pop();
    }

    /**
     * Performs the check for circular references and returns the
     * referenced object.
     * @param <T> required reference type
     * @return the dereferenced object.
     * @throws BuildException if the reference is invalid (circular ref, wrong class, etc).
     * @since Ant 1.7
     * @deprecated use getCheckedRef(Class)
     */
    @Deprecated
    protected <T> T getCheckedRef() {
        return getCheckedRef(getProject());
    }

    /**
     * Performs the check for circular references and returns the
     * referenced object.
     * @param <T> required reference type
     * @param requiredClass the class that this reference should be a subclass of.
     * @return the dereferenced object.
     * @throws BuildException if the reference is invalid (circular ref, wrong class, etc).
     * @since Ant 1.10.6
     */
    protected <T> T getCheckedRef(final Class<T> requiredClass) {
        return getCheckedRef(requiredClass, getDataTypeName(), getProject());
    }

    /**
     * Performs the check for circular references and returns the
     * referenced object.
     * @param <T> required reference type
     * @param p the Ant Project instance against which to resolve references.
     * @return the dereferenced object.
     * @throws BuildException if the reference is invalid (circular ref, wrong class, etc).
     * @since Ant 1.7
     * @deprecated use getCheckedRef(Class)
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    protected <T> T getCheckedRef(Project p) {
        return getCheckedRef((Class<T>) getClass(), getDataTypeName(), p);
    }

    /**
     * Performs the check for circular references and returns the
     * referenced object.
     * @param <T> required reference type
     * @param requiredClass the class that this reference should be a subclass of.
     * @param dataTypeName  the name of the datatype that the reference should be
     *                      (error message use only).
     * @return the dereferenced object.
     * @throws BuildException if the reference is invalid (circular ref, wrong class, etc).
     */
    protected <T> T getCheckedRef(final Class<T> requiredClass,
                                   final String dataTypeName) {
        return getCheckedRef(requiredClass, dataTypeName, getProject());
    }

    /**
     * Performs the check for circular references and returns the
     * referenced object.  This version allows the fallback Project instance to be specified.
     * @param <T> required reference type
     * @param requiredClass the class that this reference should be a subclass of.
     * @param dataTypeName  the name of the datatype that the reference should be
     *                      (error message use only).
     * @param project       the fallback Project instance for dereferencing.
     * @return the dereferenced object.
     * @throws BuildException if the reference is invalid (circular ref, wrong class, etc),
     *                        or if <code>project</code> is <code>null</code>.
     * @since Ant 1.7
     */
    protected <T> T getCheckedRef(final Class<T> requiredClass,
                                  final String dataTypeName, final Project project) {
        if (project == null) {
            throw new BuildException("No Project specified");
        }
        dieOnCircularReference(project);
        T o = ref.getReferencedObject(project);
        if (requiredClass.isAssignableFrom(o.getClass())) {
            return o;
        }
        log("Class " + displayName(o.getClass())
                        + " is not a subclass of "
                        + displayName(requiredClass),
                Project.MSG_VERBOSE);
        throw new BuildException(ref.getRefId() + " doesn't denote a " + dataTypeName);
    }

    /**
     * Creates an exception that indicates that refid has to be the
     * only attribute if it is set.
     * @return the exception to throw
     */
    protected BuildException tooManyAttributes() {
        return new BuildException("You must not specify more than one "
            + "attribute when using refid");
    }

    /**
     * Creates an exception that indicates that this XML element must
     * not have child elements if the refid attribute is set.
     * @return the exception to throw
     */
    protected BuildException noChildrenAllowed() {
        return new BuildException("You must not specify nested elements "
            + "when using refid");
    }

    /**
     * Creates an exception that indicates the user has generated a
     * loop of data types referencing each other.
     * @return the exception to throw
     */
    protected BuildException circularReference() {
        return new BuildException("This data type contains a circular "
            + "reference.");
    }

    /**
     * The flag that is used to indicate that circular references have been checked.
     * @return true if circular references have been checked
     */
    protected boolean isChecked() {
        return checked;
    }

    /**
     * Set the flag that is used to indicate that circular references have been checked.
     * @param checked if true, if circular references have been checked
     */
    protected void setChecked(final boolean checked) {
        this.checked = checked;
    }

    /**
     * get the reference set on this object
     * @return the reference or null
     */
    public Reference getRefid() {
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

    /**
     * Basic DataType toString().
     * @return this DataType formatted as a String.
     */
    @Override
    public String toString() {
        String d = getDescription();
        return d == null ? getDataTypeName() : getDataTypeName() + " " + d;
    }

    /**
     * @since Ant 1.7
     * @return a shallow copy of this DataType.
     * @throws CloneNotSupportedException if there is a problem.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        DataType dt = (DataType) super.clone();
        dt.setDescription(getDescription());
        if (getRefid() != null) {
            dt.setRefid(getRefid());
        }
        dt.setChecked(isChecked());
        return dt;
    }

    private String displayName(Class<?> clazz) {
        return clazz.getName() + " (loaded via " + clazz.getClassLoader() + ")";
    }
}

