package qbyp.serial.read.util;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

public class ConnectionPool4MySql {

	private static Logger msgLogInfo = Logger.getLogger("msgInfo");
	private static ComboPooledDataSource ds;
	private static ConnectionPool4MySql pool;

	/**
	 * Ctor.
	 */
	private ConnectionPool4MySql() {
	}

	/**
	 * 初始化连接池
	 */
	static {
		ds = new ComboPooledDataSource();

		try {
			ds.setDriverClass(PropertiesReader4MySql.getProperty("DriverClass"));
		} catch (PropertyVetoException e) {
			msgLogInfo.error("加载mysql数据库驱动时出现异常!e:" + e);
			e.printStackTrace();
		}

		ds.setJdbcUrl(PropertiesReader4MySql.getProperty("Url"));
		ds.setUser(PropertiesReader4MySql.getProperty("User"));
		ds.setPassword(PropertiesReader4MySql.getProperty("Password"));
		// 初始化时获取100条连接
		ds.setInitialPoolSize(PropertiesReader4MySql.getIntProperty("InitialPoolSize", 30));
		// 连接池中保留的最大连接数
		ds.setMaxPoolSize(PropertiesReader4MySql.getIntProperty("MaxPoolSize", 200));
		// 连接池中保留的最小连接数。
		ds.setMinPoolSize(PropertiesReader4MySql.getIntProperty("MinPoolSize", 200));
		// 当连接池中的连接耗尽的时候c3p0一次同时获取的连接数
		ds.setAcquireIncrement(PropertiesReader4MySql.getIntProperty("AcquireIncrement", 10));
		// 每60秒检查所有连接池中的空闲连接
		ds.setIdleConnectionTestPeriod(PropertiesReader4MySql.getIntProperty("IdleConnectionTestPeriod", 60));
		// 最大空闲时间,3600秒内未使用则连接被丢弃。若为0则永不丢弃
		ds.setMaxIdleTime(PropertiesReader4MySql.getIntProperty("MaxIdleTime", 3600));
		// 连接关闭时默认将所有未提交的操作回滚。Default: false autoCommitOnClose
		ds.setAutoCommitOnClose(PropertiesReader4MySql.getBooleanProperty("AutoCommitOnClose", true));
		// 定义在从数据库获取新连接失败后重复尝试的次数
		ds.setAcquireRetryAttempts(PropertiesReader4MySql.getIntProperty("AcquireRetryAttempts", 30));
		// 两次连接中间隔时间，单位毫秒
		ds.setAcquireRetryDelay(PropertiesReader4MySql.getIntProperty("AcquireRetryDelay", 1000));
		// 获取连接失败将会引起所有等待连接池来获取连接的线程抛出异常。
		// 但是数据源仍有效保留，并在下次调用getConnection()的时候继续尝试获取连接。
		// 如果设为true，那么在尝试获取连接失败后该数据源将申明已断开并永久关闭
		ds.setBreakAfterAcquireFailure(PropertiesReader4MySql.getBooleanProperty("BreakAfterAcquireFailure", false));// true
	}

	/**
	 * 获取连接池实例
	 * 
	 * @return
	 */
	public static synchronized final ConnectionPool4MySql getInstance() {
		if (pool == null) {
			try {
				pool = new ConnectionPool4MySql();
			} catch (Exception e) {
				msgLogInfo.error(e.getMessage());
				e.printStackTrace();
			}
		}
		return pool;
	}

	/**
	 * 重载finalize
	 */
	protected void finalize() throws Throwable {
		DataSources.destroy(ds);
		super.finalize();
	}

	/**
	 * 释放资源
	 * 
	 * @param rs
	 * @param stmt
	 * @param con
	 */
	public void free(ResultSet rs, PreparedStatement stmt, Connection con) {
		try {
			if (rs != null)
				rs.close();
			if (stmt != null)
				stmt.close();
			if (con != null)
				con.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 获取数据库连接
	 * 
	 * @return
	 */
	public synchronized final Connection getConnection() {
		try {
			return ds.getConnection();
		} catch (SQLException e) {
			msgLogInfo.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 执行语句
	 * 
	 * @Title: execBatch
	 * @Description: 执行sql PreparedStatement
	 * @author wuyy
	 * @date 2016年5月20日 上午10:25:59
	 *
	 * @param sql
	 * @param arrObj
	 */
	public boolean execBatch(String sql, List<Object[]> arrObj) {
		Connection conn = null;
		PreparedStatement pstm = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);// 1,首先把Auto commit设置为false,不让它自动提交
			pstm = conn.prepareStatement(sql);
			final int batchSize = 1000;
			int count = 0;
			for (Object[] item : arrObj) {
				for (int i = 0; i < item.length; i++) {
					pstm.setObject(i + 1, item[i]);
				}
				pstm.addBatch();
				if (++count % batchSize == 0) {
					pstm.executeBatch(); // 提交一部分;
				}
			}
			pstm.executeBatch(); // 提交剩下的;
			conn.commit();// 2,进行手动提交（commit）
			conn.setAutoCommit(true);// 3,提交完成后回复现场将Auto commit,还原为true,
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				// 若出现异常，对数据库中所有已完成的操作全部撤销，则回滚到事务开始状态
				if (!conn.isClosed()) {
					conn.rollback();// 4,当异常发生执行catch中SQLException时，记得要rollback(回滚)；
					conn.setAutoCommit(true);
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		} finally {
			free(null, pstm, conn);
		}
	}
	
	/**
	 * 执行sql
	 * 
	 * @param sql
	 * @return
	 */
	public boolean update(String sql) {
		boolean result = false;
		Connection conn = null;
		try {
			conn = ConnectionPool4MySql.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.execute(sql);
			result = true;
		} catch (Exception e) {
			msgLogInfo.error(e.getMessage(), e);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					msgLogInfo.error(e.getMessage(), e);
				}
			}
		}
		return result;
	}
}
