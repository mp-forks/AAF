/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.authz.cm.facade;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.authz.cm.mapper.Mapper;
import com.att.authz.env.AuthzTrans;
import com.att.authz.layer.Result;


/**
 *   
 *
 */
public interface Facade<REQ,CERT,ARTIFACTS,ERROR> {

/////////////////////  STANDARD ELEMENTS //////////////////
	/** 
	 * @param trans
	 * @param response
	 * @param result
	 */
	void error(AuthzTrans trans, HttpServletResponse response, Result<?> result);

	/**
	 * 
	 * @param trans
	 * @param response
	 * @param status
	 */
	void error(AuthzTrans trans, HttpServletResponse response, int status,	String msg, String ... detail);

	/**
	 * Permission checker
	 *
	 * @param trans
	 * @param resp
	 * @param perm
	 * @return
	 * @throws IOException 
	 */
	Result<Void> check(AuthzTrans trans, HttpServletResponse resp, String perm) throws IOException;

	/**
	 * 
	 * @return
	 */
	public Mapper<REQ,CERT,ARTIFACTS,ERROR> mapper();

/////////////////////  STANDARD ELEMENTS //////////////////
	
	/**
	 * 
	 * @param trans
	 * @param resp
	 * @param rservlet
	 * @return
	 */
	public abstract Result<Void> requestCert(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp, boolean withTrust);

	/**
	 * 
	 * @param trans
	 * @param req
	 * @param resp
	 * @return
	 */
	public abstract Result<Void> renewCert(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp, boolean withTrust);

	/**
	 * 
	 * @param trans
	 * @param req
	 * @param resp
	 * @return
	 */
	public abstract Result<Void> dropCert(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp);

	/**
	 * 
	 * @param trans
	 * @param req
	 * @param resp
	 * @return
	 */
	Result<Void> createArtifacts(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp);
	
	/**
	 * 
	 * @param trans
	 * @param req
	 * @param resp
	 * @return
	 */
	Result<Void> readArtifacts(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp);

	/**
	 * 
	 * @param trans
	 * @param resp
	 * @param mechid
	 * @param machine
	 * @return
	 */
	Result<Void> readArtifacts(AuthzTrans trans, HttpServletResponse resp, String mechid, String machine);

	/**
	 * 
	 * @param trans
	 * @param req
	 * @param resp
	 * @return
	 */
	Result<Void> updateArtifacts(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp);
	
	/**
	 * 
	 * @param trans
	 * @param req
	 * @param resp
	 * @return
	 */
	Result<Void> deleteArtifacts(AuthzTrans trans, HttpServletRequest req, HttpServletResponse resp);

	/**
	 * 
	 * @param trans
	 * @param resp
	 * @param mechid
	 * @param machine
	 * @return
	 */
	Result<Void> deleteArtifacts(AuthzTrans trans, HttpServletResponse resp, String mechid, String machine);



}