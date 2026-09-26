# 📘 Hướng Dẫn Tích Hợp License System vào Plugin Java

Hướng dẫn chi tiết để tích hợp hệ thống license management vào plugin Java của bạn.

---

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Cấu Trúc API](#cấu-trúc-api)
3. [Cài Đặt Dependencies](#cài-đặt-dependencies)
4. [Code LicenseClient.java](#code-licenseclientjava)
5. [Cách Sử Dụng trong Plugin](#cách-sử-dụng-trong-plugin)
6. [Cấu Hình](#cấu-hình)
7. [Ví Dụ Hoàn Chỉnh](#ví-dụ-hoàn-chỉnh)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 Tổng Quan

Hệ thống license management hoạt động theo quy trình:

1. **Validate**: Plugin gửi license key để kiểm tra tính hợp lệ
2. **Register**: Nếu hợp lệ, đăng ký server activation
3. **Heartbeat**: Gửi heartbeat mỗi 60 giây để giữ license active

**Lưu ý quan trọng:**
- ✅ **API URL và Plugin ID phải hardcode trong code** (không cho user thay đổi)
- ✅ **License key lấy từ config file** (user có thể thay đổi)
- ✅ **Server IP, Port, Location tự động detect** (có thể override)

---

## 🔌 Cấu Trúc API

### Base URL
```
https://your-domain.com/api/index.php?path=plugin/{endpoint}
```

### Endpoints

#### 1. Validate License
```
POST /api/index.php?path=plugin/validate
```

**Request Body:**
```json
{
  "licenseKey": "LIC-ABC123DEF456",
  "pluginId": "your-plugin-id",
  "serverIp": "192.168.1.100"
}
```

**Response (200 OK - Valid):**
```json
{
  "valid": true,
  "success": true,
  "message": "License is valid"
}
```

**Response (403 Forbidden - Invalid):**
```json
{
  "valid": false,
  "success": false,
  "message": "License is invalid or expired"
}
```

#### 2. Register Activation
```
POST /api/index.php?path=plugin/register
```

**Request Body:**
```json
{
  "licenseKey": "LIC-ABC123DEF456",
  "pluginId": "your-plugin-id",
  "serverIp": "192.168.1.100",
  "serverName": "My Server",
  "pluginVersion": "1.0.0",
  "serverInfo": {
    "port": 25565,
    "location": "Hanoi, Vietnam",
    "serverName": "My Server",
    "serverIp": "192.168.1.100",
    "pluginVersion": "1.0.0",
    "javaVersion": "17.0.1",
    "osName": "Linux",
    "osVersion": "5.4.0",
    "osArch": "amd64"
  }
}
```

**Response (200 OK):**
```json
{
  "success": true
}
```

**Response (400/403 - Failed):**
```json
{
  "success": false,
  "error": "Invalid license or license limit reached"
}
```

#### 3. Heartbeat
```
POST /api/index.php?path=plugin/heartbeat
```

**Request Body:**
```json
{
  "licenseKey": "LIC-ABC123DEF456",
  "serverIp": "192.168.1.100",
  "serverInfo": {
    "port": 25565,
    "location": "Hanoi, Vietnam",
    "serverName": "My Server",
    "pluginVersion": "1.0.0"
  }
}
```

**Response (200 OK):**
```json
{
  "success": true
}
```

**Response (404 - Not Found):**
```json
{
  "success": false
}
```

**Response (403 - Blocked):**
```json
{
  "success": false,
  "error": "IP or IP+Port is blocked"
}
```

---

## 📦 Cài Đặt Dependencies

### Maven (`pom.xml`)

```xml
<dependencies>
    <!-- OkHttp for HTTP requests -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>5.0.0-alpha.12</version>
    </dependency>
    
    <!-- Gson for JSON parsing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.11.0</version>
    </dependency>
</dependencies>
```

### Gradle (`build.gradle`)

```gradle
dependencies {
    implementation "com.squareup.okhttp3:okhttp:5.0.0-alpha.12"
    implementation "com.google.code.gson:gson:2.11.0"
}
```

---

## 💻 Code LicenseClient.java

Copy file `LicenseClient.java` vào project của bạn và đổi package name.

**File đã có sẵn trong project root:** `LicenseClient.java`

**Các tính năng:**
- ✅ Validate license với server
- ✅ Register server activation
- ✅ Heartbeat tự động mỗi 60 giây
- ✅ SSL/TLS support
- ✅ User-Agent header
- ✅ Error handling đầy đủ
- ✅ Support serverPort và serverLocation (optional)

---

## 🚀 Cách Sử Dụng trong Plugin

### Bước 1: Hardcode API URL và Plugin ID

Trong main plugin class (ví dụ: `YourPlugin.java`):

```java
public final class YourPlugin extends JavaPlugin {
    
    // ========== LICENSE MANAGEMENT ==========
    // Hardcoded configuration - không thể thay đổi bởi user
    private static final String LICENSE_API_URL = "https://your-domain.com";
    private static final String PLUGIN_ID = "your-plugin-id";
    private LicenseClient licenseClient;
    // ========== END LICENSE MANAGEMENT ==========
    
    @Override
    public void onEnable() {
        // ... other code ...
        
        // ========== LICENSE VALIDATION ==========
        String licenseKey = getConfig().getString("license.key");
        
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            getLogger().severe("==========================================");
            getLogger().severe("License key is not configured!");
            getLogger().severe("Please add your license key to config.yml");
            getLogger().severe("==========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get server information
        String serverIp = getServerIp(); // Implement this method
        String serverName = getServer().getServerName();
        String pluginVersion = getDescription().getVersion();
        int serverPort = getServer().getPort(); // If available
        String serverLocation = "Unknown"; // Optional: detect from IP
        
        // Create license client
        licenseClient = new LicenseClient(
            LICENSE_API_URL,
            licenseKey,
            PLUGIN_ID,
            serverIp,
            serverName,
            pluginVersion,
            serverPort,
            serverLocation
        );
        
        // Initialize and validate
        if (!licenseClient.initialize()) {
            getLogger().severe("==========================================");
            getLogger().severe("License validation failed!");
            getLogger().severe("Plugin will not start.");
            getLogger().severe("==========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getLogger().info("==========================================");
        getLogger().info("License validated successfully!");
        getLogger().info("Plugin ID: " + PLUGIN_ID);
        getLogger().info("Plugin Version: " + pluginVersion);
        getLogger().info("==========================================");
        // ========== END LICENSE VALIDATION ==========
    }
    
    @Override
    public void onDisable() {
        // Stop heartbeat when plugin disables
        if (licenseClient != null) {
            licenseClient.stopHeartbeat();
        }
    }
    
    /**
     * Get server IP address
     * Try multiple IP detection services for reliability
     */
    private String getServerIp() {
        String[] ipServices = {
            "https://api.ipify.org",
            "https://icanhazip.com",
            "https://ifconfig.me/ip"
        };
        
        for (String serviceUrl : ipServices) {
            try {
                java.net.URI uri = new java.net.URI(serviceUrl);
                java.net.URL url = uri.toURL();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                
                try (java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()))) {
                    String ip = in.readLine();
                    if (ip != null && !ip.trim().isEmpty() && !ip.equals("unknown")) {
                        return ip.trim();
                    }
                }
            } catch (Exception e) {
                // Try next service
                continue;
            }
        }
        
        // Fallback: try to get local IP
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
```

### Bước 2: Thêm License Key vào Config

Trong `config.yml`:

```yaml
license:
  key: "LIC-ABC123DEF456"  # License key từ admin panel
```

---

## ⚙️ Cấu Hình

### 1. API URL (Hardcode trong code)

```java
private static final String LICENSE_API_URL = "https://your-domain.com";
```

**Lưu ý:** 
- Không có `/license-management-web` ở cuối
- Chỉ cần domain: `https://your-domain.com`
- API sẽ tự động thêm `/api/index.php?path=plugin/...`

### 2. Plugin ID (Hardcode trong code)

```java
private static final String PLUGIN_ID = "your-plugin-id";
```

**Lưu ý:**
- Plugin ID phải khớp chính xác với Plugin ID trên license management web admin
- Phải tạo Plugin trên web admin trước khi tạo license key

### 3. License Key (Config file)

User sẽ thêm license key vào config file sau khi nhận từ admin.

---

## 📝 Ví Dụ Hoàn Chỉnh

### File: `YourPlugin.java`

```java
package com.yourcompany.yourplugin;

import org.bukkit.plugin.java.JavaPlugin;
import your.package.license.LicenseClient;

public final class YourPlugin extends JavaPlugin {
    
    private static final String LICENSE_API_URL = "https://your-domain.com";
    private static final String PLUGIN_ID = "your-plugin-id";
    private LicenseClient licenseClient;
    
    @Override
    public void onEnable() {
        // Load config
        saveDefaultConfig();
        reloadConfig();
        
        // License validation
        String licenseKey = getConfig().getString("license.key");
        
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            getLogger().severe("License key is not configured!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        String serverIp = getServerIp();
        String serverName = getServer().getServerName();
        String pluginVersion = getDescription().getVersion();
        
        licenseClient = new LicenseClient(
            LICENSE_API_URL,
            licenseKey,
            PLUGIN_ID,
            serverIp,
            serverName,
            pluginVersion
        );
        
        if (!licenseClient.initialize()) {
            getLogger().severe("License validation failed!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getLogger().info("License validated successfully!");
    }
    
    @Override
    public void onDisable() {
        if (licenseClient != null) {
            licenseClient.stopHeartbeat();
        }
    }
    
    private String getServerIp() {
        // Implementation from above
        return "unknown";
    }
}
```

### File: `config.yml`

```yaml
license:
  key: ""
```

---

## 🔧 Troubleshooting

### License validation failed

**Nguyên nhân có thể:**
1. License key không đúng
2. Plugin ID không khớp
3. License bị khóa trên web admin
4. License đã hết hạn
5. Max IPs đã đạt giới hạn
6. Server IP bị block
7. Không thể kết nối đến license server

**Giải pháp:**
- Kiểm tra license key trong config
- Kiểm tra Plugin ID có khớp không
- Kiểm tra license status trên web admin
- Kiểm tra API URL có đúng không
- Kiểm tra kết nối mạng

### Không thể kết nối đến license server

**Nguyên nhân:**
- API URL sai
- Firewall chặn
- License server không chạy
- SSL certificate issue

**Giải pháp:**
- Kiểm tra API URL
- Kiểm tra firewall
- Kiểm tra license server logs
- Thử truy cập API URL từ browser

### Heartbeat failed

**Nguyên nhân:**
- Activation không tồn tại
- IP bị block
- License bị khóa

**Giải pháp:**
- Kiểm tra activation trên web admin
- Kiểm tra blocked IPs
- Kiểm tra license status

---

## 📌 Lưu Ý Quan Trọng

1. **API URL và Plugin ID phải hardcode** - User không thể thay đổi
2. **License key lấy từ config** - User có thể thay đổi
3. **Server IP tự động detect** - Có thể override nếu cần
4. **Heartbeat tự động** - Gửi mỗi 60 giây
5. **Plugin sẽ disable nếu license invalid** - Bảo vệ plugin

---

## 📞 Support

Nếu gặp vấn đề:
1. Kiểm tra console logs
2. Kiểm tra license server logs
3. Kiểm tra config file
4. Liên hệ support

---

**Chúc bạn tích hợp thành công! 🎉**

