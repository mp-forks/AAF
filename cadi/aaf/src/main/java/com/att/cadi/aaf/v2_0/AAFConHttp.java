/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.cadi.aaf.v2_0;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.Principal;

import com.att.cadi.CadiException;
import com.att.cadi.Locator;
import com.att.cadi.Locator.Item;
import com.att.cadi.LocatorException;
import com.att.cadi.PropAccess;
import com.att.cadi.SecuritySetter;
import com.att.cadi.client.AbsTransferSS;
import com.att.cadi.client.Rcli;
import com.att.cadi.client.Retryable;
import com.att.cadi.config.Config;
import com.att.cadi.config.SecurityInfoC;
import com.att.cadi.http.HBasicAuthSS;
import com.att.cadi.http.HMangr;
import com.att.cadi.http.HRcli;
import com.att.cadi.http.HTransferSS;
import com.att.cadi.http.HX509SS;
import com.att.cadi.principal.BasicPrincipal;
import com.att.inno.env.APIException;

public class AAFConHttp extends AAFCon<HttpURLConnection> {
	private final HMangr hman;

	public AAFConHttp(PropAccess access) throws CadiException, GeneralSecurityException, IOException {
		super(access,Config.AAF_URL,new SecurityInfoC<HttpURLConnection>(access));
		hman = new HMangr(access,Config.loadLocator(access, access.getProperty(Config.AAF_URL,null)));
	}

	public AAFConHttp(PropAccess access, String tag) throws CadiException, GeneralSecurityException, IOException {
		super(access,tag,new SecurityInfoC<HttpURLConnection>(access));
		hman = new HMangr(access,Config.loadLocator(access, access.getProperty(tag,null)));
	}

	public AAFConHttp(PropAccess access, String urlTag, SecurityInfoC<HttpURLConnection> si) throws CadiException {
		super(access,urlTag,si);
		hman = new HMangr(access,Config.loadLocator(access, access.getProperty(urlTag,null)));
	}

	public AAFConHttp(PropAccess access, Locator<URI> locator) throws CadiException, GeneralSecurityException, IOException {
		super(access,Config.AAF_URL,new SecurityInfoC<HttpURLConnection>(access));
		hman = new HMangr(access,locator);
	}

	public AAFConHttp(PropAccess access, Locator<URI> locator, SecurityInfoC<HttpURLConnection> si) throws CadiException {
		super(access,Config.AAF_URL,si);
		hman = new HMangr(access,locator);
	}

	public AAFConHttp(PropAccess access, Locator<URI> locator, SecurityInfoC<HttpURLConnection> si, String tag) throws CadiException {
		super(access,tag,si);
		hman = new HMangr(access, locator);
	}
	
	private AAFConHttp(AAFCon<HttpURLConnection> aafcon, String url) {
		super(aafcon);
		hman = new HMangr(aafcon.access,Config.loadLocator(access, url));
	}

	@Override
	public AAFCon<HttpURLConnection> clone(String url) {
		return new AAFConHttp(this,url);
	}

	/* (non-Javadoc)
	 * @see com.att.cadi.aaf.v2_0.AAFCon#basicAuth(java.lang.String, java.lang.String)
	 */
	@Override
	public SecuritySetter<HttpURLConnection> basicAuth(String user, String password) throws CadiException {
		if(password.startsWith("enc:???")) {
			try {
				password = access.decrypt(password, true);
			} catch (IOException e) {
				throw new CadiException("Error decrypting password",e);
			}
		}
		try {
			return new HBasicAuthSS(user,password,si);
		} catch (IOException e) {
			throw new CadiException("Error creating HBasicAuthSS",e);
		}
	}

	public SecuritySetter<HttpURLConnection> x509Alias(String alias) throws APIException, CadiException {
		try {
			return set(new HX509SS(alias,si));
		} catch (Exception e) {
			throw new CadiException("Error creating X509SS",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.att.cadi.aaf.v2_0.AAFCon#rclient(java.net.URI, com.att.cadi.SecuritySetter)
	 */
	@Override
	protected Rcli<HttpURLConnection> rclient(URI ignoredURI, SecuritySetter<HttpURLConnection> ss) throws CadiException {
		if(hman.loc==null) {
			throw new CadiException("No Locator set in AAFConHttp"); 
		}
		try {
			return new HRcli(hman, hman.loc.best() ,ss);
		} catch (Exception e) {
			throw new CadiException(e);
		}
	}

	@Override
	public AbsTransferSS<HttpURLConnection> transferSS(Principal principal) throws CadiException {
		return new HTransferSS(principal, app,si);
	}
	
	/* (non-Javadoc)
	 * @see com.att.cadi.aaf.v2_0.AAFCon#basicAuthSS(java.security.Principal)
	 */
	@Override
	public SecuritySetter<HttpURLConnection> basicAuthSS(BasicPrincipal principal) throws CadiException {
		try {
			return new HBasicAuthSS(principal,si);
		} catch (IOException e) {
			throw new CadiException("Error creating HBasicAuthSS",e);
		}
	}

	public HMangr hman() {
		return hman;
	}

	@Override
	public <RET> RET best(Retryable<RET> retryable) throws LocatorException, CadiException, APIException {
		return hman.best(ss, (Retryable<RET>)retryable);
	}

	/* (non-Javadoc)
	 * @see com.att.cadi.aaf.v2_0.AAFCon#initURI()
	 */
	@Override
	protected URI initURI() {
		try {
			Item item = hman.loc.best();
			if(item!=null) {
				return hman.loc.get(item);
			}
		} catch (LocatorException e) {
			access.log(e, "Error in AAFConHttp obtaining initial URI");
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.att.cadi.aaf.v2_0.AAFCon#setInitURI(java.lang.String)
	 */
	@Override
	protected void setInitURI(String uriString) throws CadiException {
		// TODO Auto-generated method stub
		
	}
	
}
