/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.authz.gw.api;

import static com.att.authz.layer.Result.OK;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.internal.jetty.http.HttpStatus;
import com.att.authz.env.AuthzTrans;
import com.att.authz.gw.GwAPI;
import com.att.authz.gw.GwCode;
import com.att.authz.gw.facade.GwFacade;
import com.att.authz.gw.mapper.Mapper.API;
import com.att.authz.layer.Result;
import com.att.cadi.Symm;
import com.att.cssa.rserv.HttpMethods;

/**
 * API Apis
 *
 */
public class API_Api {
	/**
	 * Normal Init level APIs
	 * 
	 * @param gwAPI
	 * @param facade
	 * @throws Exception
	 */
	public static void init(final GwAPI gwAPI, GwFacade facade) throws Exception {
		////////
		// Overall APIs
		///////
		gwAPI.route(HttpMethods.GET,"/api",API.VOID,new GwCode(facade,"Document API", true) {
			@Override
			public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
				Result<Void> r = context.getAPI(trans,resp,gwAPI);
				switch(r.status) {
				case OK:
					resp.setStatus(HttpStatus.OK_200);
					break;
				default:
					context.error(trans,resp,r);
			}

			}
		});

		////////
		// Overall Examples
		///////
		gwAPI.route(HttpMethods.GET,"/api/example/*",API.VOID,new GwCode(facade,"Document API", true) {
			@Override
			public void handle(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp) throws Exception {
				String pathInfo = req.getPathInfo();
				int question = pathInfo.lastIndexOf('?');
				
				pathInfo = pathInfo.substring(13, question<0?pathInfo.length():question);// IMPORTANT, this is size of "/api/example/"
				String nameOrContextType=Symm.base64noSplit.decode(pathInfo);
//				String param = req.getParameter("optional");
				Result<Void> r = context.getAPIExample(trans,resp,nameOrContextType,
						question>=0 && "optional=true".equalsIgnoreCase(req.getPathInfo().substring(question+1))
						);
				switch(r.status) {
				case OK:
					resp.setStatus(HttpStatus.OK_200);
					break;
				default:
					context.error(trans,resp,r);
			}

			}
		});

	}
}
