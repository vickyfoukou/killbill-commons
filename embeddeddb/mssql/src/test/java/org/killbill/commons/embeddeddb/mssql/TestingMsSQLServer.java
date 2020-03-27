/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.commons.embeddeddb.mssql;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.testng.Assert.assertTrue;

public class TestingMsSQLServer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(TestingMsSQLServer.class);

    private final String user;
    private final String password;
    private final String database;
    private final int port;
    private final EmbeddedMsSQL server;

    public TestingMsSQLServer(final String user, final String database) throws Exception {
        this(user, null, null, database);
    }

    public TestingMsSQLServer(final String user, final String password, @Nullable final Integer portOrNull, final String database) throws Exception {
        // Make sure the driver is registered
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        this.user = checkNotNull(user, "user is null");
        this.password = checkNotNull(password, "password is null");
        this.database = checkNotNull(database, "database is null");

        if (portOrNull == null) {
            server = new EmbeddedMsSQL();
        } else {
            server = new EmbeddedMsSQL(portOrNull);
        }
        port = server.getPort();

        Connection connection = null;
        try {
            connection = server.getMssqlDatabase();
            Statement statement = null;
            try {
                statement = connection.createStatement();
                //execute(statement, format("DROP SERVER ROLE test_role"));
                execute(statement, format("IF NOT EXISTS (SELECT name from sys.server_principals where name='test_role') CREATE SERVER ROLE test_role AUTHORIZATION %s", user));
                execute(statement, format("IF NOT EXISTS (SELECT * FROM sys.databases where name ='%s_Test' ) CREATE DATABASE %s_Test", database, database));
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        } catch (final Exception e) {
            if (connection != null) {
                connection.close();
            }
            server.close();
            throw e;
        }

        log.info("SQL_SERVER server ready: {}", getJdbcUrl());
    }

    private static void execute(final Statement statement, final String sql) throws SQLException {
        log.debug("Executing: {}", sql);
        statement.execute(sql);
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    public String getUser() {
        return user;
    }

    public String getDatabase() {
        return database;
    }

    public int getPort() {
        return port;
    }

    public String getJdbcUrl() {
        return server.getJdbcUrl(user, password, database);
    }

    public static String toString(final InputStream inputStream) throws IOException {
        try {
            return new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    public EmbeddedMsSQL getServer() {
        return server;
    }

    @Test(groups = "fast")
    public void testEmbeddeddb(){
        assertTrue(server != null);
    }
}
