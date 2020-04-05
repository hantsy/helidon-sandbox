package demo;

import java.sql.*;

public class JdbcMetadataSample {
    private final String url = "jdbc:postgresql://localhost/test";
    private final String user = "user";
    private final String password = "password";

    public void printPersonTableMetadata() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");

            var statement = conn.prepareStatement("select FullName, EMAIL, createdAt from PERSON");
            ResultSet rs = statement.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String label = metaData.getColumnLabel(i);
                String name = metaData.getColumnName(i);
                System.out.println("label:" + label);
                System.out.println("name:" + name);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        JdbcMetadataSample app = new JdbcMetadataSample();
        app.printPersonTableMetadata();
    }
}
