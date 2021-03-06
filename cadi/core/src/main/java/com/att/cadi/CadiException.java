/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.cadi;

/**
 * CADI Specific Exception
 */
public class CadiException extends Exception {
	/**
	 *  Generated ID 
	 */
	private static final long serialVersionUID = -4180145363107742619L;

	public CadiException() {
		super();
	}

	public CadiException(String message) {
		super(message);
	}

	public CadiException(Throwable cause) {
		super(cause);
	}

	public CadiException(String message, Throwable cause) {
		super(message, cause);
	}

}
