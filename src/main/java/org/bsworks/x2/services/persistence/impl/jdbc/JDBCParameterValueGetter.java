package org.bsworks.x2.services.persistence.impl.jdbc;

import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.Resources;


/**
 * Universal interface for parameter value handler getter.
 *
 * @author Lev Himmelfarb
 */
interface JDBCParameterValueGetter {

	/**
	 * Simple value getter.
	 */
	static final JDBCParameterValueGetter VAL = new JDBCParameterValueGetter() {
		@Override
		public JDBCParameterValue get(final Resources resources,
				final ParameterValuesFactoryImpl paramsFactory,
				final ResourcePropertyHandler propHandler,
				final Object propVal) {

			return paramsFactory.getParameterValue(
					propHandler.getValueHandler().getPersistentValueType(),
					propVal);
		}
	};

	/**
	 * Reference value getter (gets the target record id).
	 */
	static final JDBCParameterValueGetter REF = new JDBCParameterValueGetter() {
		@Override
		public JDBCParameterValue get(final Resources resources,
				final ParameterValuesFactoryImpl paramsFactory,
				final ResourcePropertyHandler propHandler,
				final Object propVal) {

			final PersistentResourceHandler<?> th =
				resources.getPersistentResourceHandler(
						((RefPropertyHandler) propHandler)
							.getReferredResourceClass());

			return paramsFactory.getParameterValue(
					th.getIdProperty().getValueHandler()
						.getPersistentValueType(),
					(propVal == null ? null : ((Ref<?>) propVal).getId()));
		}
	};


	/**
	 * Get parameter value handler.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param propHandler Property handler.
	 * @param propVal Property value. May be {@code null}.
	 *
	 * @return Parameter value handler.
	 */
	JDBCParameterValue get(Resources resources,
			ParameterValuesFactoryImpl paramsFactory,
			ResourcePropertyHandler propHandler, Object propVal);
}
