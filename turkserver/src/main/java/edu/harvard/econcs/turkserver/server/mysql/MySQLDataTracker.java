package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.schema.*;
import edu.harvard.econcs.turkserver.server.TSConfig;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mysema.query.QueryFlag.Position;
import com.mysema.query.sql.MySQLTemplates;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLQueryImpl;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.types.TemplateExpressionImpl;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

/**
 * Connects to a mysql database for persistent users across sessions
 * 
 * schema for turk experiments
 * 
 * HIT ID 30 characters
 * Assignment ID 30 characters
 * Worker ID 14 characters
 * 
 * username 40 characters
 * 
 * @author mao
 *
 */
@Singleton
public class MySQLDataTracker extends ExperimentDataTracker {	
	
	QSets _sets = QSets.sets;
	QExperiment _experiment = QExperiment.experiment;
	QSession _session = QSession.session;	
	QQuiz _quiz = QQuiz.quiz;
	QWorker _worker = QWorker.worker;
	
	private final String setID;		
	
	private final PoolBackedDataSource pbds;	
	private final SQLTemplates dialect;	

	@Inject
	public MySQLDataTracker(			
			MysqlConnectionPoolDataSource ds, 
			@Named(TSConfig.EXP_SETID) String setID) throws PropertyVetoException {
		super();
		
		this.setID = setID;
		
		pbds = new PoolBackedDataSource();
		pbds.setConnectionPoolDataSource(ds);
		 								
		dialect = new MySQLTemplates();					
		
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			// ensure this setId exists
			new SQLInsertClause(conn, dialect, _sets)
			.columns(_sets.name)
			.values(setID)
			.addFlag(Position.START_OVERRIDE, "INSERT IGNORE INTO ")
			.execute();
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
	}
	
	@Override
	public Collection<Experiment> getSetExperiments() {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			return new SQLQueryImpl(conn, dialect)
			.from(_experiment)
			.where(_experiment.setId.eq(setID))
			.list(_experiment);
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
		return null;
	}

	@Override
	public boolean hitExistsInDB(String hitId) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
						
			long count = new SQLQueryImpl(conn, dialect)
			.from(_session)
			.where(_session.hitId.eq(hitId))
			.count();		
			
			return count > 0;
			
			// TODO replicate expired experiment logic elsewhere			
//			if( results != null && results.size() > 0) {
//				Object experimentId = results.get(0)[0];			
//				if( experimentId != null && "EXPIRED".equals(experimentId.toString()) ) 
//					throw new SessionExpiredException();
//				return true;
//			}
//			else return false;
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
		
		return false;		
	}

	@Override
	public Collection<Quiz> getSetQuizRecords(String workerId) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			SQLQuery query = new SQLQueryImpl(conn, dialect);
			
			return query.from(_quiz)
					.where(_quiz.setId.eq(setID), 
							_quiz.workerId.eq(workerId))
					.list(_quiz);
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
		return null;		
	}

	@Override
	public Collection<Session> getSetSessionInfoForWorker(String workerId) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			SQLQuery query = new SQLQueryImpl(conn, dialect);
			
			return query.from(_session)
					.where(_session.workerId.eq(workerId),
							_session.setId.eq(setID))
					.list(_session);
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
		return null;
	}

	@Override
	public Session getStoredSessionInfo(String hitId) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			List<Session> result = new SQLQueryImpl(conn, dialect)
			.from(_session)
			.where(_session.hitId.eq(hitId))
			.list(_session);
				
			// Return the first element if one exists
			return (result == null || result.size() == 0 ? null : result.get(0));
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
		return null;
	}

	@Override
	protected void saveSession(Session record) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			new SQLInsertClause(conn, dialect, _session)
			.populate(record)
			.addFlag(Position.START_OVERRIDE, "REPLACE ")
			.execute();
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}		
	}

	@Override
	public void saveHITId(String hitId) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			/*
			 * INSERT INTO session (hitId, setId) VALUES (?, ?)
			 * ON DUPLICATE KEY UPDATE setId = ?
			 */
			
			new SQLInsertClause(conn, dialect, _session)
			.columns(_session.hitId, _session.setId)
			.values(hitId, setID)
			.addFlag(Position.END, TemplateExpressionImpl.create(				
					String.class, " ON DUPLICATE KEY UPDATE {0}", _session.setId.eq(setID) ))
			.execute();
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}	
	}

	@Override
	public void saveAssignmentForSession(String hitId,
			String assignmentId, String workerId) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			// Make sure the worker table contains this workerId first, but ignore if already exists
			/*
			 * INSERT IGNORE INTO worker(id) VALUES (?)
			 */
			new SQLInsertClause(conn, dialect, _worker)
			.columns(_worker.id)
			.values(workerId)
			.addFlag(Position.START_OVERRIDE, "INSERT IGNORE INTO ")
			.execute();
			
			/*
			 * UPDATE session SET assignmentId=?, workerId=? WHERE hitId=?
			 */
			new SQLUpdateClause(conn, dialect, _session)
			.set(_session.assignmentId, assignmentId)
			.set(_session.workerId, workerId)
			.where(_session.hitId.eq(hitId))
			.execute();
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}				
	}

	@Override
	public void saveQuizResults(String hitId, String workerId, QuizResults results) {					
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			// Make sure the worker table contains this workerId first, but ignore if already exists
			new SQLInsertClause(conn, dialect, _worker)
			.columns(_worker.id)
			.values(workerId)
			.addFlag(Position.START_OVERRIDE, "INSERT IGNORE INTO ")
			.execute();
			
			/* creates
			 * INSERT IGNORE INTO worker(id) VALUES ("workerId");
			 */
			
			double score = 1.0*results.correct/results.total;
			
			new SQLInsertClause(conn, dialect, _quiz)
			.columns(_quiz.sessionId, _quiz.workerId, _quiz.setId, _quiz.numCorrect, _quiz.numTotal, _quiz.score)
			.values(hitId, workerId, setID, results.correct, results.total, score )
			.execute();		
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}					
	}

	@Override
	protected void saveExpStartTime(String expId, int size, String inputdata, long startTime) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			new SQLInsertClause(conn, dialect, _experiment)
			.columns(_experiment.id, _experiment.setId, _experiment.participants, _experiment.inputdata, _experiment.startTime)
			.values(expId, setID, size, inputdata, new Timestamp(startTime))
			.execute();
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}				
	}

	@Override
	protected void saveExpEndTime(String expId, long endTime) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			new SQLUpdateClause(conn, dialect, _experiment)
			.where(_experiment.id.eq(expId))
			.set(_experiment.endTime, new Timestamp(endTime))
			.execute();	
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
	}

	@Override
	public void clearWorkerForSession(String hitId) {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			/*
			 * TODO this used to be set to default but not sure how to do with QueryDSL
			 * UPDATE session SET workerId=DEFAULT, username=DEFAULT WHERE hitId=?
			 */
			new SQLUpdateClause(conn, dialect, _session)
			.setNull(_session.workerId)
			.setNull(_session.username)
			.where(_session.hitId.eq(hitId))
			.execute();
			
			logger.info(String.format("HIT %s has workerId cleared", hitId));
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}		
	}
	
	@Override
	public List<Session> expireUnusedSessions() {
		Connection conn = null;		
		try {
			conn = pbds.getConnection();
			
			/*
			 * SELECT * FROM session WHERE setId=? AND experimentId IS NULL
			 */
			List<Session> expired = new SQLQueryImpl(conn, dialect)
			.from(_session)
			.where(_session.setId.eq(setID), _session.experimentId.isNull())
			.list(_session);
				
			logger.info("Found " + expired.size() + " unused sessions");
			
			/* 
			 * TODO this used to set to EXPIRED, but now we reuse,
			 * so we can just delete them. Verify that this is okay.  
			 * 
			 * UPDATE session SET experimentId='EXPIRED' WHERE setId=? AND experimentId IS NULL
			 */		
			new SQLDeleteClause(conn, dialect, _session)
			.where(_session.setId.eq(setID), _session.experimentId.isNull())
			.execute();
			
			return expired;
			
		} catch (SQLException e) {			
			e.printStackTrace();
		} finally {
			if( conn != null ) {
				try { conn.close(); }
				catch (SQLException e) { e.printStackTrace(); }
			}
		}
		return null;
	}
	
	/**
	 * Create the TurkServer schema from a configuration.
	 * TODO only works on linux-based machines with mysql client installed.
	 * 
	 * @param conf
	 * @throws Exception
	 */
	public static int createSchema(Configuration conf) throws Exception {
		URL url = MySQLDataTracker.class.getClassLoader().getResource("schema.sql");
		final File f = new File(url.getFile());
		
		if( !f.exists() ) throw new FileNotFoundException("schema.sql was not found");
		
		String host = conf.getString(TSConfig.MYSQL_HOST, null);
		String db = conf.getString(TSConfig.MYSQL_DATABASE, null);
		String user = conf.getString(TSConfig.MYSQL_USER, null);
		String pw = conf.getString(TSConfig.MYSQL_PASSWORD, null);
		
		if( db == null ) throw new Exception ("Need database name!");
		String userStr = user == null ? "" : String.format("-u %s ", user);
		String hostStr = host == null ? "" : String.format("-h %s ", host);
		String pwStr = pw == null ? "" : String.format("-p%s ", pw);
		
		String cmd = String.format("mysql %s %s %s %s", hostStr, userStr, pwStr, db);
		
		System.out.println(cmd + " < schema.sql");		
		
		final Process pr = Runtime.getRuntime().exec(cmd);

		new Thread() {
			public void run() {
				OutputStream stdin = null;
				try {
					Files.copy(f, stdin = pr.getOutputStream());
				} 
				catch (IOException e) { e.printStackTrace(); }
				finally {
					if( stdin != null ) {
						try { stdin.close(); } 
						catch (IOException e) { e.printStackTrace(); }
					}
				}				
			}
		}.start();
		
		new Thread() {
			public void run() {
				InputStream stdout = null;
				try {
					ByteStreams.copy(stdout = pr.getInputStream(), System.out);
				} 
				catch (IOException e) { e.printStackTrace(); }
				finally {
					if( stdout != null ) {
						try { stdout.close(); } 
						catch (IOException e) { e.printStackTrace(); }
					}
				}				
			}
		}.start();				

		int exitVal = pr.waitFor();
		if( exitVal == 0 )
			System.out.println("Create db succeeded!");
		else	
			System.out.println("Exited with error code " + exitVal);
		return exitVal;
	}
	
}
