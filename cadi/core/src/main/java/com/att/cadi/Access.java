/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.cadi;

import java.io.IOException;
import java.io.InputStream;

/**
 * Various Environments require different logging mechanisms, or at least allow
 * for different ones. We need the Framework to be able to hook into any particular instance of logging
 * mechanism, whether it be a Logging Object within a Servlet Context, or a direct library like log4j.
 * This interface, therefore, allows maximum pluggability in a variety of different app styles.  
 *  
 *
 */
public interface Access {
	// levels to use
	public enum Level {
		DEBUG(0x1), INFO(0x10), AUDIT(0x100), WARN(0x2000), ERROR(0x4000), INIT(0x8000),NONE(0XFFFF);
		private final int bit;
		
		Level(int ord) {
			bit = ord;
		}
		
		public boolean inMask(int mask) {
			return (mask & bit) == bit;
		}
		
		public int addToMask(int mask) {
			return mask | bit;
		}

		public int delFromMask(int mask) {
			return mask & ~bit;
		}

		public int toggle(int mask) {
			if(inMask(mask)) {
				return delFromMask(mask);
			} else {
				return addToMask(mask);
			}
		}


		public int maskOf() {
			int mask=0;
			for(Level l : values()) {
				if(ordinal()<l.ordinal()) {
					mask|=l.bit;
				}
			}
			return mask;
		}
	}

	/**
	 * Write a variable list of Object's text via the toString() method with appropriate space, etc.
	 * @param elements
	 */
	public void log(Level level, Object ... elements);

	/**
	 * Printf mechanism for Access
	 * @param level
	 * @param fmt
	 * @param elements
	 */
	public void printf(Level level, String fmt, Object ... elements);
	
	/** 
	 * Check if message will log before constructing
	 * @param level
	 * @return
	 */
	public boolean willLog(Level level);

	/**
	 * Write the contents of an exception, followed by a variable list of Object's text via the 
	 * toString() method with appropriate space, etc.
	 * 
	 * The Loglevel is always "ERROR"
	 * 
	 * @param elements
	 */
	public void log(Exception e, Object ... elements);
	
	/**
	 * Set the Level to compare logging too
	 */
	public void setLogLevel(Level level);
		
	/**
	 * It is important in some cases to create a class from within the same Classloader that created
	 * Security Objects.  Specifically, it's pretty typical for Web Containers to separate classloaders
	 * so as to allow Apps with different dependencies. 
	 * @return
	 */
	public ClassLoader classLoader();

	public String getProperty(String string, String def);
	
	public void load(InputStream is) throws IOException;

	/**
	 * if "anytext" is true, then decryption will always be attempted.  Otherwise, only if starts with 
	 * Symm.ENC
	 * @param encrypted
	 * @param anytext
	 * @return
	 * @throws IOException
	 */
	public String decrypt(String encrypted, boolean anytext) throws IOException;

	public static final Access NULL = new Access() {
		public void log(Level level, Object... elements) {
		}

		@Override
		public void printf(Level level, String fmt, Object... elements) {
		}

		public void log(Exception e, Object... elements) {
		}

		public ClassLoader classLoader() {
			return this.classLoader();
		}

		public String getProperty(String string, String def) {
			return null;
		}

		public void load(InputStream is) throws IOException {
		}

		public void setLogLevel(Level level) {
		}

		public String decrypt(String encrypted, boolean anytext) throws IOException {
			return encrypted;
		}

		@Override
		public boolean willLog(Level level) {
			return false;
		}
	};


}
