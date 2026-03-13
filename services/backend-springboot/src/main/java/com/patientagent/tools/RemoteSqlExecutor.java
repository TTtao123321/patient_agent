package com.patientagent.tools;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 远程 SQL 脚本执行工具，通过命令行参数连接到指定 MySQL 实例并执行建表 SQL 脚本。
 * <p>
 * 使用场景：圖远程环境（如测试服务器、生产环境）初始化建表结构。
 * </p>
 * <p>用法：{@code java RemoteSqlExecutor <host> <port> <database> <username> <password> <sqlFilePath>}</p>
 */
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

        // 先以 admin 身份连接，确保目标数据库已存在。
        String adminUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        try (Connection adminConn = DriverManager.getConnection(adminUrl, username, password);
             Statement stmt = adminConn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + database
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }

        // 切换到目标库并执行 SQL 脚本。
        try (Connection dbConn = DriverManager.getConnection(dbUrl, username, password)) {
            EncodedResource resource = new EncodedResource(new FileSystemResource(sqlFilePath), StandardCharsets.UTF_8);
            ScriptUtils.executeSqlScript(dbConn, resource);
        }

        System.out.println("SQL script executed successfully.");
    }
}
