package org.bsworks.x2.toolbox.handlers;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.responses.OKResponse;
import org.bsworks.x2.services.auth.impl.PasswordActorAuthenticationService;


/**
 * Password-based actor login endpoint handler. The handler takes three request
 * parameters&mdash;{@value #LOGIN_NAME_PARAM}, {@value #PASSWORD_PARAM} and
 * optional {@value #OPAQUE_PARAM}&mdash;and attempts to authenticate the actor
 * assuming that the application uses a
 * {@link PasswordActorAuthenticationService} authentication service
 * implementation.
 *
 * <p>If authentication is successful, the endpoint returns an HTTP 200 (OK)
 * response with the authenticated actor in the response body and the
 * appropriate authentication headers. If the authentication failed, an HTTP 400
 * (Bad Request) response is returned.
 *
 * @author Lev Himmelfarb
 */
public class PasswordLoginEndpointCallHandler
	extends ReadOnlyEndpointCallHandler {

	/**
	 * Name of the login name request parameter.
	 */
	public static final String LOGIN_NAME_PARAM = "user";

	/**
	 * Name of the password request parameter.
	 */
	public static final String PASSWORD_PARAM = "password";

	/**
	 * Name of the opaque request parameter.
	 */
	public static final String OPAQUE_PARAM = "opaque";


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isAllowed(final HttpMethod requestMethod,
			final String requestURI, final List<String> uriParams,
			final Actor actor) {

		return true;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public EndpointCallResponse handleCall(final EndpointCallContext ctx,
			final Void requestEntity)
		throws EndpointCallErrorException {

		final PasswordActorAuthenticationService authService =
			(PasswordActorAuthenticationService) ctx.getRuntimeContext()
			.getActorAuthenticationService();

		final Actor actor = authService.authenticate(
				ctx.getRequestParam(LOGIN_NAME_PARAM),
				ctx.getRequestParam(PASSWORD_PARAM),
				ctx.getRequestParam(OPAQUE_PARAM));
		if (actor == null)
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, "Invalid login.");

		ctx.assumeActor(actor);

		return new OKResponse(actor, null, null);
	}
}
