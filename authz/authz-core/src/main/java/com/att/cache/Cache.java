/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.att.inno.env.Env;
import com.att.inno.env.Trans;

/**
 * Create and maintain a Map of Maps used for Caching
 * 
 *
 * @param <TRANS>
 * @param <DATA>
 */
public class Cache<TRANS extends Trans, DATA> {
	private static Clean clean;
	private static Timer cleanseTimer;

	public static final String CACHE_HIGH_COUNT = "CACHE_HIGH_COUNT";
	public static final String CACHE_CLEAN_INTERVAL = "CACHE_CLEAN_INTERVAL";
//	public static final String CACHE_MIN_REFRESH_INTERVAL = "CACHE_MIN_REFRESH_INTERVAL";

	private static final Map<String,Map<String,Dated>> cacheMap;

	static {
		cacheMap = new HashMap<String,Map<String,Dated>>();
	}

	/**
	 * Dated Class - store any Data with timestamp
	 * 
	 *
	 */
	public final static class Dated { 
		public Date timestamp;
		public List<?> data;
		
		public Dated(List<?> data) {
			timestamp = new Date();
			this.data = data;
		}

		public <T> Dated(T t) {
			timestamp = new Date();
			ArrayList<T> al = new ArrayList<T>(1);
			al.add(t);
			data = al;
		}

		public void touch() {
			timestamp = new Date();
		}
	}
	
	public static Map<String,Dated> obtain(String key) {
		Map<String, Dated> m = cacheMap.get(key);
		if(m==null) {
			m = new ConcurrentHashMap<String, Dated>();
			synchronized(cacheMap) {
				cacheMap.put(key, m);
			}
		}
		return m;
	}

	/**
	 * Clean will examine resources, and remove those that have expired.
	 * 
	 * If "highs" have been exceeded, then we'll expire 10% more the next time.  This will adjust after each run
	 * without checking contents more than once, making a good average "high" in the minimum speed.
	 * 
	 *
	 */
	private final static class Clean extends TimerTask {
		private final Env env;
		private Set<String> set;
		
		// The idea here is to not be too restrictive on a high, but to Expire more items by 
		// shortening the time to expire.  This is done by judiciously incrementing "advance"
		// when the "highs" are exceeded.  This effectively reduces numbers of cached items quickly.
		private final int high;
		private long advance;
		private final long timeInterval;
		
		public Clean(Env env, long cleanInterval, int highCount) {
			this.env = env;
			high = highCount;
			timeInterval = cleanInterval;
			advance = 0;
			set = new HashSet<String>();
		}
		
		public synchronized void add(String key) {
			set.add(key);
		}

		public void run() {
			int count = 0;
			int total = 0;
			// look at now.  If we need to expire more by increasing "now" by "advance"
			Date now = new Date(System.currentTimeMillis() + advance);
			
			
			for(String name : set) {
				Map<String,Dated> map = cacheMap.get(name);
				if(map!=null) for(Map.Entry<String,Dated> me : map.entrySet()) {
					++total;
					if(me.getValue().timestamp.before(now)) {
						map.remove(me.getKey());
						++count;
					}
				}
//				if(count>0) {
//					env.info().log(Level.INFO, "Cache removed",count,"expired",name,"Elements");
//				}
			}
			
			if(count>0) {
				env.info().log(Level.INFO, "Cache removed",count,"expired Cached Elements out of", total);
			}

			// If High (total) is reached during this period, increase the number of expired services removed for next time.
			// There's no point doing it again here, as there should have been cleaned items.
			if(total>high) {
				// advance cleanup by 10%, without getting greater than timeInterval.
				advance = Math.min(timeInterval, advance+(timeInterval/10));
			} else {
				// reduce advance by 10%, without getting lower than 0.
				advance = Math.max(0, advance-(timeInterval/10));
			}
		}
	}

	public static synchronized void startCleansing(Env env, String ... keys) {
		if(cleanseTimer==null) {
			cleanseTimer = new Timer("Cache Cleanup Timer");
			int cleanInterval = Integer.parseInt(env.getProperty(CACHE_CLEAN_INTERVAL,"60000")); // 1 minute clean cycles 
			int highCount = Integer.parseInt(env.getProperty(CACHE_HIGH_COUNT,"5000"));
			cleanseTimer.schedule(clean = new Clean(env, cleanInterval, highCount), cleanInterval, cleanInterval);
		}
		
		for(String key : keys) {
			clean.add(key);
		}
	}

	public static void stopTimer() {
		if(cleanseTimer!=null) {
			cleanseTimer.cancel();
			cleanseTimer = null;
		}
	}

	public static void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Cache.stopTimer();
			}
		}); 
	}

}
