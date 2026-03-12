package com.patientagent.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RemoteTableVerifier {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("Usage: <host> <port> <database> <username> <password>");
            System.exit(1);
        }

        String host = args[0];
        String port = args[1];
        String database = args[2];
        String username = args[3];
        String password = args[4];

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name IN "
                + "('patient_user','medical_record','medical_report','chat_session','chat_message') "
                + "ORDER BY table_name";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getString("table_name"));
                }
            }
        }
    }
}
