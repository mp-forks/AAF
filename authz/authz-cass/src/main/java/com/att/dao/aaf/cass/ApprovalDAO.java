/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.dao.aaf.cass;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.att.authz.env.AuthzTrans;
import com.att.authz.layer.Result;
import com.att.dao.CassDAOImpl;
import com.att.dao.Loader;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;


public class ApprovalDAO extends CassDAOImpl<AuthzTrans,ApprovalDAO.Data> {
	public static final String PENDING = "pending";
	public static final String DENIED = "denied";
	public static final String APPROVED = "approved";
	
	private static final String TABLE = "approval";
	private HistoryDAO historyDAO;
	private PSInfo psByUser, psByApprover, psByTicket, psByStatus;

	
	public ApprovalDAO(AuthzTrans trans, Cluster cluster, String keyspace) {
		super(trans, ApprovalDAO.class.getSimpleName(),cluster,keyspace,Data.class,TABLE, readConsistency(trans,TABLE), writeConsistency(trans,TABLE));
        historyDAO = new HistoryDAO(trans, this);
		init(trans);
	}


	public ApprovalDAO(AuthzTrans trans, HistoryDAO hDAO) {
		super(trans, ApprovalDAO.class.getSimpleName(),hDAO,Data.class,TABLE, readConsistency(trans,TABLE), writeConsistency(trans,TABLE));
		historyDAO=hDAO;
		init(trans);
	}

	private static final int KEYLIMIT = 1;
	public static class Data {
		public UUID   id;
        public UUID   ticket;
		public String user;
		public String approver;
		public String type;
		public String status;
		public String memo;
		public String operation;
		public Date updated;
	}
	
	private static class ApprovalLoader extends Loader<Data> {
		public static final ApprovalLoader deflt = new ApprovalLoader(KEYLIMIT);
		
		public ApprovalLoader(int keylimit) {
			super(keylimit);
		}
		
		@Override
		public Data load(Data data, Row row) {
			data.id = row.getUUID(0);
			data.ticket = row.getUUID(1);
			data.user = row.getString(2);
			data.approver = row.getString(3);
			data.type = row.getString(4);
			data.status = row.getString(5);
			data.memo = row.getString(6);
			data.operation = row.getString(7);
			if(row.getColumnDefinitions().size()>8) {
				// Rows reported in MicroSeconds
				data.updated = new Date(row.getLong(8)/1000);
			}
			return data;
		}

		@Override
		protected void key(Data data, int idx, Object[] obj) {
			obj[idx]=data.id;
		}

		@Override
		protected void body(Data data, int _idx, Object[] obj) {
		    	int idx = _idx;
			obj[idx]=data.ticket;
			obj[++idx]=data.user;
			obj[++idx]=data.approver;
			obj[++idx]=data.type;
			obj[++idx]=data.status;
			obj[++idx]=data.memo;
			obj[++idx]=data.operation;
		}
	}	
	
	private void init(AuthzTrans trans) {
		String[] helpers = setCRUD(trans, TABLE, Data.class, ApprovalLoader.deflt,8);
		// Need a specialty Creator to handle the "now()"
		replace(CRUD.create, new PSInfo(trans, "INSERT INTO " + TABLE + " (" +  helpers[FIELD_COMMAS] +
					") VALUES(now(),?,?,?,?,?,?,?)",new ApprovalLoader(0) {
						@Override
						protected void key(Data data, int idx, Object[] obj) {
							// Overridden because key is the "now()"
						}
					},writeConsistency)
				);

		psByUser = new PSInfo(trans, SELECT_SP + helpers[FIELD_COMMAS] + ", WRITETIME(status) FROM " + TABLE + 
				" WHERE user = ?", new ApprovalLoader(1) {
			@Override
			protected void key(Data data, int idx, Object[] obj) {
				obj[idx]=data.user;
			}
		}, readConsistency);
		
		psByApprover = new PSInfo(trans, SELECT_SP + helpers[FIELD_COMMAS] + ", WRITETIME(status) FROM " + TABLE + 
				" WHERE approver = ?", new ApprovalLoader(1) {
			@Override
			protected void key(Data data, int idx, Object[] obj) {
				obj[idx]=data.approver;
			}
		}, readConsistency);

		psByTicket = new PSInfo(trans, SELECT_SP + helpers[FIELD_COMMAS] + ", WRITETIME(status) FROM " + TABLE + 
				" WHERE ticket = ?", new ApprovalLoader(1) {
			@Override
			protected void key(Data data, int idx, Object[] obj) {
				obj[idx]=data.ticket;
			}
		}, readConsistency);

		psByStatus = new PSInfo(trans, SELECT_SP + helpers[FIELD_COMMAS] + ", WRITETIME(status) FROM " + TABLE + 
				" WHERE status = ?", new ApprovalLoader(1) {
			@Override
			protected void key(Data data, int idx, Object[] obj) {
				obj[idx]=data.status;
			}
		}, readConsistency);


	}
	
	public Result<List<ApprovalDAO.Data>> readByUser(AuthzTrans trans, String user) {
		return psByUser.read(trans, R_TEXT, new Object[]{user});
	}

	public Result<List<ApprovalDAO.Data>> readByApprover(AuthzTrans trans, String approver) {
		return psByApprover.read(trans, R_TEXT, new Object[]{approver});
	}

	public Result<List<ApprovalDAO.Data>> readByTicket(AuthzTrans trans, UUID ticket) {
		return psByTicket.read(trans, R_TEXT, new Object[]{ticket});
	}

	public Result<List<ApprovalDAO.Data>> readByStatus(AuthzTrans trans, String status) {
		return psByStatus.read(trans, R_TEXT, new Object[]{status});
	}	

	/**
     * Log Modification statements to History
     *
     * @param modified        which CRUD action was done
     * @param data            entity data that needs a log entry
     * @param overrideMessage if this is specified, we use it rather than crafting a history message based on data
     */
    @Override
    protected void wasModified(AuthzTrans trans, CRUD modified, Data data, String ... override) {
    	boolean memo = override.length>0 && override[0]!=null;
    	boolean subject = override.length>1 && override[1]!=null;

        HistoryDAO.Data hd = HistoryDAO.newInitedData();
        hd.user = trans.user();
        hd.action = modified.name();
        hd.target = TABLE;
        hd.subject = subject?override[1]:data.user + "|" + data.approver;
        hd.memo = memo
                ? String.format("%s by %s", override[0], hd.user)
                : (modified.name() + "d approval for " + data.user);
        // Detail?
        // Reconstruct?
        if(historyDAO.create(trans, hd).status!=Status.OK) {
        	trans.error().log("Cannot log to History");
        }
    }

}
