package com.pararede.alfresco.containersecurity;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationServiceImpl;
import org.alfresco.repo.security.authentication.SimpleAcceptOrRejectAllAuthenticationComponentImpl;
import org.alfresco.repo.security.authentication.TicketComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.app.servlet.AuthenticationStatus;
import org.alfresco.web.bean.LoginBean;
import org.alfresco.web.bean.repository.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class AlfrescoContainerSecurityFilter implements Filter {

    private static final Log logger = LogFactory.getLog(AlfrescoContainerSecurityFilter.class);

    private WebApplicationContext context;
    private ServiceRegistry registry;

    public void init(FilterConfig config) throws ServletException {
	this.context = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	this.registry = (ServiceRegistry) this.context.getBean("ServiceRegistry");
    }

    public void destroy() {
	this.registry = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
	HttpServletRequest httpRequest = (HttpServletRequest) request;
	HttpServletResponse httpResponse = (HttpServletResponse) response;
	HttpSession httpSession = httpRequest.getSession();

	String userName = httpRequest.getUserPrincipal().getName();
	User userAuth = AuthenticationHelper.getUser(httpRequest, httpResponse);
	if ((userAuth == null) || !userName.equals(userAuth.getUserName())) {
	    try {
		TransactionService transactionService = this.registry.getTransactionService();
		UserTransaction tx = transactionService.getUserTransaction();
		try {
		    tx.begin();

		    // remove the session invalidated flag (used to remove last username cookie by
		    // AuthenticationFilter)
		    httpSession.removeAttribute(AuthenticationHelper.SESSION_INVALIDATED);

		    if (logger.isDebugEnabled()) {
			logger.debug("Authenticating user " + userName);
		    }
		    AuthenticationService authenticationService = getAuthenticationService();
		    authenticationService.authenticate(userName, null);

		    PersonService personService = this.registry.getPersonService();
		    userAuth = new User(userName, authenticationService.getCurrentTicket(), personService.getPerson(userName));

		    NodeService nodeService = this.registry.getNodeService();
		    NodeRef homeSpaceRef = (NodeRef) nodeService.getProperty(personService.getPerson(userName), ContentModel.PROP_HOMEFOLDER);
		    if (!nodeService.exists(homeSpaceRef)) {
			throw new InvalidNodeRefException(homeSpaceRef);
		    }
		    userAuth.setHomeSpaceId(homeSpaceRef.getId());

		    httpSession.setAttribute(AuthenticationHelper.AUTHENTICATION_USER, userAuth);
		    httpSession.setAttribute(LoginBean.LOGIN_EXTERNAL_AUTH, true);

		    tx.commit();
		} catch (Throwable e) {
		    tx.rollback();
		    throw new ServletException(e);
		}
	    } catch (SystemException e) {
		throw new ServletException(e);
	    }
	} else {
	    if (logger.isDebugEnabled()) {
		logger.debug("User " + userName + " already authenticated");
	    }

	    AuthenticationStatus status = AuthenticationHelper.authenticate(httpSession.getServletContext(), httpRequest, httpResponse, false);
	    if (status != AuthenticationStatus.Success) {
		throw new ServletException("User not correctly autheticated");
	    }
	}

	chain.doFilter(request, response);
    }

    private AuthenticationService getAuthenticationService() {
	SimpleAcceptOrRejectAllAuthenticationComponentImpl authenticationComponent = new SimpleAcceptOrRejectAllAuthenticationComponentImpl();
	authenticationComponent.setAccept(true);

	AuthenticationServiceImpl authenticationService = new AuthenticationServiceImpl();
	authenticationService.setAuthenticationComponent(authenticationComponent);
	authenticationService.setTicketComponent((TicketComponent) this.context.getBean("ticketComponent"));

	return authenticationService;
    }
}
