/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.jdbc.impl.actions;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.TaskQueue;
import io.vertx.core.json.JsonArray;

import java.sql.*;
import java.sql.ResultSet;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 */
public class JDBCQuery extends AbstractJDBCAction<io.vertx.ext.sql.ResultSet> {

  private final String sql;
  private final JsonArray in;
  private final int timeout;

  public JDBCQuery(Vertx vertx, JDBCStatementHelper helper, Connection connection, ContextInternal ctx, TaskQueue statementsQueue, int timeout, String sql, JsonArray in) {
    super(vertx, helper, connection, ctx, statementsQueue);
    this.sql = sql;
    this.in = in;
    this.timeout = timeout;
  }

  @Override
  protected io.vertx.ext.sql.ResultSet execute() throws SQLException {
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      if (timeout >= 0) {
        statement.setQueryTimeout(timeout);
      }

      helper.fillStatement(statement, in);
      boolean retResult = statement.execute();

      io.vertx.ext.sql.ResultSet resultSet = null;

      if (retResult) {
        io.vertx.ext.sql.ResultSet ref = null;
        // normal return only
        while (retResult) {
          try (ResultSet rs = statement.getResultSet()) {
            // 1st rs
            if (ref == null) {
              resultSet = helper.asList(rs);
              ref = resultSet;
            } else {
              ref.setNext(helper.asList(rs));
              ref = ref.getNext();
            }
          }
          try {
            retResult = statement.getMoreResults();
          } catch (SQLFeatureNotSupportedException e) {
            retResult = false;
          }
        }
      }

      return resultSet;
    }
  }

  @Override
  protected String name() {
    return "query";
  }
}
