/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.dao.aaf.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.junit.Test;

import com.att.authz.layer.Result;
import com.att.dao.aaf.cass.CertDAO;
import com.att.dao.aaf.cass.CertDAO.Data;
import com.att.inno.env.APIException;

/**
 * UserDAO unit test.
 * User: tp007s
 * Date: 7/19/13
 */
public class JU_CertDAO  extends AbsJUCass {
	@Test
	public void test() throws IOException, NoSuchAlgorithmException, APIException {
		CertDAO cdao = new CertDAO(trans,cluster,"authz");
		try {
			// Create
	        CertDAO.Data data = new CertDAO.Data();
	        data.serial=new BigInteger("11839383");
	        data.id = "m55555@tguard.att.com";
	        data.x500="CN=ju_cert.dao.att.com, OU=AAF, O=\"ATT Services, Inc.\", L=Southfield, ST=Michigan, C=US";
	        data.x509="I'm a cert";
	        data.ca = "aaf";
			cdao.create(trans,data);

//	        Bytification
	        ByteBuffer bb = data.bytify();
	        Data bdata = new CertDAO.Data();
	        bdata.reconstitute(bb);
	        checkData1(data, bdata);

			// Validate Read with key fields in Data
			Result<List<CertDAO.Data>> rlcd = cdao.read(trans,data);
			assertTrue(rlcd.isOKhasData());
			for(CertDAO.Data d : rlcd.value) {
				checkData1(data,d);
			}

			// Validate Read with key fields in Data
			rlcd = cdao.read(trans,data.ca,data.serial);
			assertTrue(rlcd.isOKhasData());
			for(CertDAO.Data d : rlcd.value) {
				checkData1(data,d);
			}

			// Update
			data.id = "m66666.tguard.att.com";
			cdao.update(trans,data);
			rlcd = cdao.read(trans,data);
			assertTrue(rlcd.isOKhasData());
			for(CertDAO.Data d : rlcd.value) {
				checkData1(data,d);
			}			
			
			cdao.delete(trans,data, true);
		} finally {
			cdao.close(trans);
		}

		
	}

	private void checkData1(Data data, Data d) {
		assertEquals(data.ca,d.ca);
		assertEquals(data.serial,d.serial);
		assertEquals(data.id,d.id);
		assertEquals(data.x500,d.x500);
		assertEquals(data.x509,d.x509);
	}

}
