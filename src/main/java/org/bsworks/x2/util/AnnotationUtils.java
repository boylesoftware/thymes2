package org.bsworks.x2.util;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * Collection of utility methods for working with annotations.
 *
 * @author Lev Himmelfarb
 */
public final class AnnotationUtils {

	/**
	 * All methods are static.
	 */
	private AnnotationUtils() {}


	/**
	 * Get bean property annotation. The annotation is first searched on the
	 * property getter method, then on the setter, and then on the member
	 * variable with the property name in the bean class and in the classes up
	 * the inheritance chain.
	 *
	 * @param <T> The annotation type.
	 * @param annotationClass The annotation class.
	 * @param pd The bean property descriptor.
	 * @param beanClass The bean class.
	 *
	 * @return The annotation, or {@code null} if none.
	 */
	public static <T extends Annotation> T getPropertyAnnotation(
			final Class<T> annotationClass, final PropertyDescriptor pd,
			final Class<?> beanClass) {

		Method m = pd.getReadMethod();
		if (m != null) {
			final T res = m.getAnnotation(annotationClass);
			if (res != null)
				return res;
		}

		m = pd.getWriteMethod();
		if (m != null) {
			final T res = m.getAnnotation(annotationClass);
			if (res != null)
				return res;
		}

		for (Class<?> cls = beanClass; cls != Object.class;
				cls = cls.getSuperclass()) {
			try {
				return cls.getDeclaredField(pd.getName()).getAnnotation(
						annotationClass);
			} catch (final NoSuchFieldException e) {
				// nothing, try superclass
			}
		}

		return null;
	}
}
