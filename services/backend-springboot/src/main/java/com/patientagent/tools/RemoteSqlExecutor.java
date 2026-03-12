package com.patientagent.tools;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class RemoteSqlExecutor {

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println("Usage: <host> <port> <database> <username> <password> <sqlFilePath>");
            System.exit(1);
        }

        String host = args[0];
        String port = args[1];
        String database = args[2];
        String username = args[3];
        String password = args[4];
        String sqlFilePath = args[5];

        String adminUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        try (Connection adminConn = DriverManager.getConnection(adminUrl, username, password);
             Statement stmt = adminConn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + database
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }

        try (Connection dbConn = DriverManager.getConnection(dbUrl, username, password)) {
            EncodedResource resource = new EncodedResource(new FileSystemResource(sqlFilePath), StandardCharsets.UTF_8);
            ScriptUtils.executeSqlScript(dbConn, resource);
        }

        System.out.println("SQL script executed successfully.");
    }
}
