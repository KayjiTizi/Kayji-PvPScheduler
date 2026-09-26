# Hệ Thống Bảo Mật PvPScheduler

## Tổng Quan

PvPScheduler được tích hợp với hệ thống bảo mật đa lớp để bảo vệ chống lại:
- Code injection (tiêm mã độc)
- File injection (tiêm file lạ)
- Class loading động từ nguồn không đáng tin
- Reflection calls nguy hiểm
- Thay đổi file trái phép

## Các Thành Phần Bảo Mật

### 1. CodeIntegrityChecker
**Chức năng:**
- Quét và tính hash SHA-256 cho tất cả class trong plugin JAR
- Kiểm tra tính toàn vẹn của class khi load
- Phát hiện class có tên đáng ngờ hoặc pattern nguy hiểm
- Chặn các package và class nguy hiểm

**Bảo vệ chống:**
- Class injection
- Code tampering
- Malicious class loading

### 2. ClassLoaderProtection
**Chức năng:**
- Kiểm tra class loader có hợp lệ không
- Chặn load class từ nguồn không đáng tin
- Giới hạn số lần cố gắng load class
- Kiểm tra file có được phép load như class không

**Bảo vệ chống:**
- Dynamic class loading
- URLClassLoader abuse
- External JAR injection

### 3. FileIntegrityManager
**Chức năng:**
- Monitor file trong data folder
- Tính và so sánh hash của file
- Phát hiện file mới được tạo, sửa đổi, hoặc xóa
- Chặn các file extension nguy hiểm (.exe, .bat, .dll, etc.)
- Cảnh báo khi phát hiện file Java/Class lạ

**Bảo vệ chống:**
- File injection
- Configuration tampering
- Malicious file upload

### 4. SecurityPolicy
**Chức năng:**
- Tổng hợp tất cả các lớp bảo mật
- Quản lý whitelist/blacklist
- Ghi log tất cả vi phạm bảo mật
- Cung cấp API để kiểm tra class/file có được phép không

## Cách Sử Dụng

### Kiểm Tra Class Có Được Phép Load
```java
SecurityManager securityManager = plugin.getSecurityManager();
if (securityManager.isClassAllowed("com.example.SuspiciousClass")) {
    // Load class
} else {
    // Class bị chặn
}
```

### Kiểm Tra File Có Được Phép Load
```java
File file = new File(plugin.getDataFolder(), "config.yml");
if (securityManager.isFileAllowed(file)) {
    // Load file
} else {
    // File bị chặn hoặc đã bị thay đổi
}
```

### Thêm Class Vào Whitelist/Blacklist
```java
SecurityPolicy policy = securityManager.getSecurityPolicy();
policy.addWhitelistedClass("com.example.AllowedClass");
policy.addBlacklistedClass("com.example.BlockedClass");
```

## Mã Hóa Code (Obfuscation)

### Sử Dụng ProGuard

Plugin đã được cấu hình với ProGuard Maven Plugin để mã hóa code khi build.

**Build với obfuscation:**
```bash
mvn clean package
```

File obfuscated sẽ được tạo tại: `target/PvPScheduler-Kayji-1.0-SNAPSHOT-obfuscated.jar`

**Lưu ý:**
- Obfuscation chỉ áp dụng cho code, không ảnh hưởng đến Bukkit API
- Tất cả class trong package `com.example.pvpscheduler` sẽ được giữ nguyên tên để tương thích với Bukkit
- Các class nội bộ sẽ bị obfuscate để bảo vệ logic

### Tắt Obfuscation (Nếu Cần)

Nếu muốn tắt obfuscation, comment plugin trong `pom.xml`:
```xml
<!--
<plugin>
    <groupId>com.github.wvengen</groupId>
    <artifactId>proguard-maven-plugin</artifactId>
    ...
</plugin>
-->
```

## Cấu Hình Bảo Mật

### Strict Mode
Bật/tắt strict mode trong SecurityPolicy:
```java
SecurityPolicy policy = securityManager.getSecurityPolicy();
policy.setStrictMode(true); // Bật chế độ nghiêm ngặt
```

### Xem Log Bảo Mật
```java
SecurityPolicy policy = securityManager.getSecurityPolicy();
List<String> logs = policy.getSecurityLog();
for (String log : logs) {
    System.out.println(log);
}
```

## Các Pattern Bị Chặn

### Class Names
- Chứa từ khóa: Exploit, Hack, Backdoor, Malware, Virus, Trojan, Inject, Bypass
- Chứa pattern: Runtime, ProcessBuilder, Process, ClassLoader, ScriptEngine

### Packages
- `java.lang.reflect.*` (một số method)
- `javax.script.*`
- `groovy.*`
- `org.codehaus.groovy.*`
- `org.mozilla.javascript.*`

### File Extensions
- `.exe`, `.bat`, `.sh`, `.dll`, `.so`, `.dylib`

## Xử Lý Vi Phạm

Khi phát hiện vi phạm bảo mật:
1. Ghi log vào console và file log
2. Chặn hành động nguy hiểm
3. Ghi vào security log
4. Cảnh báo admin (nếu có)

## Best Practices

1. **Luôn sử dụng file obfuscated trong production**
2. **Kiểm tra file integrity định kỳ**
3. **Xem log bảo mật thường xuyên**
4. **Không tắt SecurityPolicy trừ khi cần thiết**
5. **Cập nhật whitelist/blacklist khi cần**

## Troubleshooting

### Plugin không load được sau khi obfuscate
- Kiểm tra xem có class nào bị obfuscate nhầm không
- Thêm class vào keep rules trong ProGuard config

### False positive (báo nhầm)
- Thêm class/file vào whitelist
- Điều chỉnh strict mode

### Performance issues
- File monitoring có thể ảnh hưởng performance nhẹ
- Có thể tắt file monitoring nếu không cần thiết

## Liên Hệ

Nếu phát hiện lỗ hổng bảo mật, vui lòng báo cáo ngay lập tức.

