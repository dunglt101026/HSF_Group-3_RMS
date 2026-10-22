package com.group3.rms.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class DemoService {

    @Autowired(required = false)
    private DataSource dataSource;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.datasource.url:Chưa cấu hình}")
    private String datasourceUrl;

    /**
     * Lấy thông tin chào mừng người dùng và thông số hệ thống
     */
    public Map<String, Object> getGreetingData(String name) {
        Map<String, Object> data = new HashMap<>();
        String safeName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Bạn";

        data.put("name", safeName);
        data.put("message", "Chào mừng " + safeName + " đến với hệ thống RMS (Group 3)!");
        data.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy")));
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("osName", System.getProperty("os.name"));
        data.put("activeProfile", activeProfile);

        return data;
    }

    /**
     * Kiểm tra trạng thái kết nối Database (MS SQL Server) an toàn
     * Không làm sập ứng dụng nếu chưa có bảng hoặc chưa bật SQL Server.
     */
    public Map<String, Object> checkDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("checkedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy")));
        result.put("datasourceUrl", maskSensitiveUrl(datasourceUrl));

        if (dataSource == null) {
            result.put("connected", false);
            result.put("message", "DataSource chưa được khởi tạo. Hãy kiểm tra cấu hình application.properties");
            return result;
        }

        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProductName = metaData.getDatabaseProductName();
            String dbProductVersion = metaData.getDatabaseProductVersion();
            String catalog = connection.getCatalog();

            // Thực thi câu lệnh test cơ bản
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    result.put("connected", true);
                    result.put("message", "Kết nối Database thành công!");
                    result.put("databaseProduct", dbProductName);
                    result.put("databaseVersion", dbProductVersion);
                    result.put("databaseName", (catalog != null ? catalog : "Mặc định"));
                    result.put("latencyMs", latencyMs + " ms");
                    result.put("driverName", metaData.getDriverName());
                }
            }
        } catch (Exception ex) {
            result.put("connected", false);
            result.put("message", "Không thể kết nối đến Database: " + ex.getMessage());
            result.put("errorClass", ex.getClass().getSimpleName());
            result.put("troubleshootTip", "Hãy đảm bảo SQL Server đang chạy và mật khẩu trong 'application-local.properties' chính xác.");
        }

        return result;
    }

    private String maskSensitiveUrl(String url) {
        if (url == null) return "N/A";
        // Ẩn mật khẩu nếu url có chứa password
        return url.replaceAll("(?i)password=[^;]+", "password=******");
    }
}
