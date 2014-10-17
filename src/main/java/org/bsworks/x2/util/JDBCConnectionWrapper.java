package org.bsworks.x2.util;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;


/**
 * JDBC connection wrapper.
 *
 * @author Lev Himmelfarb
 */
public class JDBCConnectionWrapper
	implements Connection {

	/**
	 * The wrapped connection.
	 */
	protected final Connection con;


	/**
	 * Wrap a connection.
	 *
	 * @param con The connection to wrap.
	 */
	public JDBCConnectionWrapper(final Connection con) {

		this.con = con;
	}


	/* (non-Javadoc)
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Override
	public <T> T unwrap(final Class<T> iface)
		throws SQLException {

		return this.con.unwrap(iface);
	}

	/* (non-Javadoc)
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Override
	public boolean isWrapperFor(final Class<?> iface)
		throws SQLException {

		return this.con.isWrapperFor(iface);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStatement()
	 */
	@Override
	public Statement createStatement()
		throws SQLException {

		return this.con.createStatement();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String)
	 */
	@Override
	public PreparedStatement prepareStatement(final String sql)
		throws SQLException {

		return this.con.prepareStatement(sql);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareCall(java.lang.String)
	 */
	@Override
	public CallableStatement prepareCall(final String sql)
		throws SQLException {

		return this.con.prepareCall(sql);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#nativeSQL(java.lang.String)
	 */
	@Override
	public String nativeSQL(final String sql)
		throws SQLException {

		return this.con.nativeSQL(sql);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setAutoCommit(boolean)
	 */
	@Override
	public void setAutoCommit(final boolean autoCommit)
		throws SQLException {

		this.con.setAutoCommit(autoCommit);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getAutoCommit()
	 */
	@Override
	public boolean getAutoCommit()
		throws SQLException {

		return this.con.getAutoCommit();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#commit()
	 */
	@Override
	public void commit()
		throws SQLException {

		this.con.commit();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#rollback()
	 */
	@Override
	public void rollback()
		throws SQLException {

		this.con.rollback();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#close()
	 */
	@Override
	public void close()
		throws SQLException {

		this.con.close();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#isClosed()
	 */
	@Override
	public boolean isClosed()
		throws SQLException {

		return this.con.isClosed();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getMetaData()
	 */
	@Override
	public DatabaseMetaData getMetaData()
		throws SQLException {

		return this.con.getMetaData();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(final boolean readOnly)
		throws SQLException {

		this.con.setReadOnly(readOnly);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#isReadOnly()
	 */
	@Override
	public boolean isReadOnly()
		throws SQLException {

		return this.con.isReadOnly();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setCatalog(java.lang.String)
	 */
	@Override
	public void setCatalog(final String catalog)
		throws SQLException {

		this.con.setCatalog(catalog);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getCatalog()
	 */
	@Override
	public String getCatalog()
		throws SQLException {

		return this.con.getCatalog();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setTransactionIsolation(int)
	 */
	@Override
	public void setTransactionIsolation(final int level)
		throws SQLException {

		this.con.setTransactionIsolation(level);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getTransactionIsolation()
	 */
	@Override
	public int getTransactionIsolation()
		throws SQLException {

		return this.con.getTransactionIsolation();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getWarnings()
	 */
	@Override
	public SQLWarning getWarnings()
		throws SQLException {

		return this.con.getWarnings();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#clearWarnings()
	 */
	@Override
	public void clearWarnings()
		throws SQLException {

		this.con.clearWarnings();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStatement(int, int)
	 */
	@Override
	public Statement createStatement(final int resultSetType,
			final int resultSetConcurrency)
		throws SQLException {

		return this.con.createStatement(resultSetType, resultSetConcurrency);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(final String sql,
			final int resultSetType, final int resultSetConcurrency)
		throws SQLException {

		return this.con.prepareStatement(sql, resultSetType,
				resultSetConcurrency);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
	 */
	@Override
	public CallableStatement prepareCall(final String sql,
			final int resultSetType, final int resultSetConcurrency)
		throws SQLException {

		return this.con.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getTypeMap()
	 */
	@Override
	public Map<String, Class<?>> getTypeMap()
		throws SQLException {

		return this.con.getTypeMap();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setTypeMap(java.util.Map)
	 */
	@Override
	public void setTypeMap(final Map<String, Class<?>> map)
		throws SQLException {

		this.con.setTypeMap(map);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setHoldability(int)
	 */
	@Override
	public void setHoldability(final int holdability)
		throws SQLException {

		this.con.setHoldability(holdability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getHoldability()
	 */
	@Override
	public int getHoldability()
		throws SQLException {

		return this.con.getHoldability();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setSavepoint()
	 */
	@Override
	public Savepoint setSavepoint()
		throws SQLException {

		return this.con.setSavepoint();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setSavepoint(java.lang.String)
	 */
	@Override
	public Savepoint setSavepoint(final String name)
		throws SQLException {

		return this.con.setSavepoint(name);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	@Override
	public void rollback(final Savepoint savepoint)
		throws SQLException {

		this.con.rollback(savepoint);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
	 */
	@Override
	public void releaseSavepoint(final Savepoint savepoint)
		throws SQLException {

		this.con.releaseSavepoint(savepoint);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStatement(int, int, int)
	 */
	@Override
	public Statement createStatement(final int resultSetType,
			final int resultSetConcurrency, final int resultSetHoldability)
		throws SQLException {

		return this.con.createStatement(resultSetType, resultSetConcurrency,
				resultSetHoldability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(final String sql,
			final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability)
		throws SQLException {

		return this.con.prepareStatement(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
	 */
	@Override
	public CallableStatement prepareCall(final String sql,
			final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability)
		throws SQLException {

		return this.con.prepareCall(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int)
	 */
	@Override
	public PreparedStatement prepareStatement(final String sql,
			final int autoGeneratedKeys)
		throws SQLException {

		return this.con.prepareStatement(sql, autoGeneratedKeys);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
	 */
	@Override
	public PreparedStatement prepareStatement(final String sql,
			final int[] columnIndexes)
		throws SQLException {

		return this.con.prepareStatement(sql, columnIndexes);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
	 */
	@Override
	public PreparedStatement prepareStatement(final String sql,
			final String[] columnNames)
		throws SQLException {

		return this.con.prepareStatement(sql, columnNames);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createClob()
	 */
	@Override
	public Clob createClob()
		throws SQLException {

		return this.con.createClob();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createBlob()
	 */
	@Override
	public Blob createBlob()
		throws SQLException {

		return this.con.createBlob();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createNClob()
	 */
	@Override
	public NClob createNClob()
		throws SQLException {

		return this.con.createNClob();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createSQLXML()
	 */
	@Override
	public SQLXML createSQLXML()
		throws SQLException {

		return this.con.createSQLXML();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#isValid(int)
	 */
	@Override
	public boolean isValid(final int timeout)
		throws SQLException {

		return this.con.isValid(timeout);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
	 */
	@Override
	public void setClientInfo(final String name, final String value)
		throws SQLClientInfoException {

		this.con.setClientInfo(name, value);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setClientInfo(java.util.Properties)
	 */
	@Override
	public void setClientInfo(final Properties properties)
		throws SQLClientInfoException {

		this.con.setClientInfo(properties);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getClientInfo(java.lang.String)
	 */
	@Override
	public String getClientInfo(final String name)
		throws SQLException {

		return this.con.getClientInfo(name);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getClientInfo()
	 */
	@Override
	public Properties getClientInfo()
		throws SQLException {

		return this.con.getClientInfo();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createArrayOf(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Array createArrayOf(final String typeName, final Object[] elements)
		throws SQLException {

		return this.con.createArrayOf(typeName, elements);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Struct createStruct(final String typeName, final Object[] attributes)
		throws SQLException {

		return this.con.createStruct(typeName, attributes);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setSchema(java.lang.String)
	 */
	@Override
	public void setSchema(final String schema)
		throws SQLException {

		this.con.setSchema(schema);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getSchema()
	 */
	@Override
	public String getSchema()
		throws SQLException {

		return this.con.getSchema();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#abort(java.util.concurrent.Executor)
	 */
	@Override
	public void abort(final Executor executor)
		throws SQLException {

		this.con.abort(executor);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setNetworkTimeout(java.util.concurrent.Executor, int)
	 */
	@Override
	public void setNetworkTimeout(final Executor executor,
			final int milliseconds)
		throws SQLException {

		this.con.setNetworkTimeout(executor, milliseconds);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getNetworkTimeout()
	 */
	@Override
	public int getNetworkTimeout()
		throws SQLException {

		return this.con.getNetworkTimeout();
	}
}
