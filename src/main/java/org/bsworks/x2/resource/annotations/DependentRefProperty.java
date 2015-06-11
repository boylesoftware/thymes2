package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bsworks.x2.resource.Ref;


/**
 * Dependent persistent resource reference property. The annotation is used to
 * mark a {@link Ref} property of a persistent resource that refers to another
 * persistent resource that contains reference property that points back to
 * this resource. The target resource of the reference marked with this
 * annotation is called a dependent persistent resource and its reference
 * property that point back to this resource is called a reverse reference. What
 * is special about a dependent resource reference property is that its value is
 * not stored with the containing persistent resource. The link is determined by
 * the reverse reference property in the target dependent resource.
 *
 * <p>An update of a dependent resource reference property may lead to removal
 * of the dependent resource record(s) from the persistent storage. This is the
 * only updates that is allowed. For a collection dependent resource property
 * adding elements via update is not allowed. Also, if a dependent resource
 * property has {@link #fetchedByDefault} attribute set to {@code false}, it is
 * completely excluded from the updates.
 *
 * <p>The type of the property marked with this annotation must be {@link Ref}.
 * It can also be a collection, but it cannot be a map. The property must belong
 * to a persistent resource. The reverse reference property in the target
 * dependent resource object must also be a {@link Ref} and it cannot be a
 * collection or a map. Neither reference type parameter can be a wildcard.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependentRefProperty {

	/**
	 * Property access restrictions. Unless explicitly restricted, all types of
	 * access are allowed.
	 *
	 * @return Access restrictions.
	 */
	AccessRestriction[] accessRestrictions() default {};

	/**
	 * If {@code true}, which is the default, the dependent resource references
	 * are fetched by default when the resource is loaded from the persistent
	 * storage. If {@code false}, the dependent resource property is fetched
	 * only if explicitly requested by the properties fetch specification.
	 *
	 * <p>Since the dependent resource record references are not stored together
	 * with the referring resource, fetching them may require additional work in
	 * the persistent storage (for example, a join if the storage is an RDBMS).
	 * Unless the references are needed, to improve performance it often makes
	 * sense to set this attribute to {@code false}.
	 *
	 * @return {@code true} to fetch property value by default, {@code false} to
	 * require explicit fetch request.
	 */
	boolean fetchedByDefault() default true;

	/**
	 * If {@code true}, which is the default, the referred dependent resource
	 * records, if any, are deleted from the persistent storage when an existing
	 * resource is being updated and in the incoming update data the property is
	 * {@code null}. If {@code false} and in the incoming update data the
	 * property is {@code null}, the referred dependent resource records are
	 * left untouched.
	 *
	 * @return {@code true} to set to delete referred records, {@code false} to
	 * leave them untouched.
	 */
	boolean updateIfNull() default true;

	/**
	 * Tells if the value can be {@code null} for a single-valued property or
	 * empty for a collection property. The default is {@code false}. See
	 * {@link Persistence#optional()} for more details.
	 *
	 * @return {@code true} if optional.
	 */
	boolean optional() default false;

	/**
	 * Name of the property in the referred dependent resource that points back
	 * to this resource.
	 *
	 * @return The reverse reference property name.
	 */
	String reverseRefProperty();

	/**
	 * Normally, when the resource record is deleted, all of its dependent
	 * resource records are deleted as well. If this attribute is set to
	 * {@code false}, no attempt to delete the dependent resource records is
	 * made.
	 *
	 * <p>Note, that if the persistent storage enforces referential integrity
	 * (e.g. foreign key constraints in an RDBMS), the whole delete operation
	 * will fail. This attribute, therefore, is used to prevent deletion of the
	 * dependent resource records and the check that such records do not exist
	 * is supposed to be performed separately before making the deletion call.
	 *
	 * @return {@code false} to prevent dependent resource records deletions.
	 * The default is {@code true}.
	 */
	boolean cascadeDelete() default true;
}
