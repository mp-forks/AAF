/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.authz.local;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.att.authz.local.DataFile.Token;
import com.att.authz.local.DataFile.Token.Field;
import com.att.inno.env.Env;
import com.att.inno.env.TimeTaken;
import com.att.inno.env.Trans;

public class TextIndex {
	private static final int REC_SIZE=8;
	
	private File file;
	private DataFile dataFile=null;
	
	public TextIndex(File theFile) {
		file = theFile;
	}
	
	public void open() throws IOException {
		dataFile = new DataFile(file,"r");
		dataFile.open();
	}
	
	public void close() throws IOException {
		if(dataFile!=null) {dataFile.close();}
	}

	public int find(Object key, AbsData.Reuse reuse, int offset) throws IOException {
		return find(key,reuse.getTokenData(),reuse.getFieldData(),offset);
	}
	
	public int find(Object key, DataFile.Token dtok, Field df, int offset) throws IOException {
		if(dataFile==null) {throw new IOException("File not opened");}
		long hash = hashToLong(key.hashCode());
		int min=0, max = (int)(dataFile.size()/REC_SIZE);
		Token ttok = dataFile.new Token(REC_SIZE);
		IntBuffer tib = ttok.getIntBuffer();
		long lhash;
		int curr;
		while((max-min)>100) {
			ttok.pos((curr=(min+(max-min)/2))*REC_SIZE);
			tib.rewind();
			lhash = hashToLong(tib.get());
			if(lhash<hash) {
				min=curr+1;
			} else if(lhash>hash) {
				max=curr-1;
			} else {
				min=curr-40;
				max=curr+40;
				break;
			}
		}
		
		List<Integer> entries = new ArrayList<Integer>();
		for(int i=min;i<=max;++i) {
			ttok.pos(i*REC_SIZE);
			tib.rewind();
			lhash = hashToLong(tib.get());
			if(lhash==hash) {
				entries.add(tib.get());
			} else if(lhash>hash) {
				break;
			}
		}
		
		for(Integer i : entries) {
			dtok.pos(i);
			if(df.at(offset).equals(key)) {
				return i;
			}
		}
		return -1;
	}
	

	/*
	 * Have to change Bytes into a Long, to avoid the inevitable signs in the Hash
	 */
	private static long hashToLong(int hash) {
		long rv;
		if(hash<0) {
			rv = 0xFFFFFFFFL & hash;
		} else {
			rv = hash;
		}
		return rv;
	}
	
	public void create(final Trans trans,final DataFile data, int maxLine, char delim, int fieldOffset, int skipLines) throws IOException {
		RandomAccessFile raf;
		FileChannel fos;
		
		List<Idx> list = new LinkedList<Idx>(); // Some hashcodes will double... DO NOT make a set
		TimeTaken tt2 = trans.start("Open Files", Env.SUB);
		try {
			raf = new RandomAccessFile(file,"rw");
			raf.setLength(0L);
			fos = raf.getChannel();
		} finally {
			tt2.done();
		}
		
		try {
			
			Token t = data.new Token(maxLine);  
			Field f = t.new Field(delim);
			
			int count = 0;
			if(skipLines>0) {
				trans.info().log("Skipping",skipLines,"line"+(skipLines==1?" in":"s in"),data.file().getName());
			}
			for(int i=0;i<skipLines;++i) {
				t.nextLine();
			}
			tt2 = trans.start("Read", Env.SUB);
			try {
				while(t.nextLine()) {
					list.add(new Idx(f.at(fieldOffset),t.pos()));
					++count;
				}
			} finally {
				tt2.done();
			}
			trans.checkpoint("    Read " + count + " records");
			tt2 = trans.start("Sort List", Env.SUB);
			Collections.sort(list);
			tt2.done();
			tt2 = trans.start("Write Idx", Env.SUB);
			try {
				ByteBuffer bb = ByteBuffer.allocate(8*1024);
				IntBuffer ib = bb.asIntBuffer();
				for(Idx idx : list) {
					if(!ib.hasRemaining()) {
						fos.write(bb);
						ib.clear();
						bb.rewind();
					}
					ib.put(idx.hash);
					ib.put(idx.pos);
				}
				bb.limit(4*ib.position());
				fos.write(bb);
			} finally {
				tt2.done();
			}
		} finally {
			fos.close();
			raf.close();
		}
	}
	
	public class Iter {
		private int idx;
		private Token t;
		private long end;
		private IntBuffer ib;


		public Iter() {
			try {
				idx = 0;
				end = dataFile.size();
				t  = dataFile.new Token(REC_SIZE);
				ib = t.getIntBuffer();

			} catch (IOException e) {
				end = -1L;
			}
		}
		
		public int next() {
			t.pos(idx);
			ib.clear();
			ib.get();
			int rec = ib.get();
			idx += REC_SIZE;
			return rec;
		}

		public boolean hasNext() {
			return idx<end;
		}
	}
	
	private static class Idx implements Comparable<Idx> {
		public int hash, pos;
		public Idx(Object obj, int pos) {
			hash = obj.hashCode();
			this.pos = pos;
		}
		
		@Override
		public int compareTo(Idx ib) {
			long a = hashToLong(hash);
			long b = hashToLong(ib.hash);
			return a>b?1:a<b?-1:0;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			if(o!=null && o instanceof Idx) {
				return hash == ((Idx)o).hash;
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return hash;
		}
	}
}
