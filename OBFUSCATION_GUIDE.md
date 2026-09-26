# Hướng Dẫn Obfuscate Code

## 📦 Cài Đặt

ProGuard Maven Plugin đã được thêm vào `pom.xml`. Khi build, code sẽ tự động được obfuscate.

## 🚀 Cách Sử Dụng

### Build với Obfuscation

```bash
mvn clean package
```

Sau khi build xong, bạn sẽ có 2 file:
- `target/PvPScheduler-Kayji-1.0-SNAPSHOT-shaded.jar` - File bình thường
- `target/PvPScheduler-Kayji-1.0-SNAPSHOT-obfuscated.jar` - File đã obfuscate

### Sử Dụng File Obfuscated

Copy file `*-obfuscated.jar` vào thư mục `plugins` của server.

## ⚙️ Cấu Hình

### Giữ Lại Class Cần Thiết

Các class sau được giữ nguyên để tương thích với Bukkit:
- `com.example.pvpscheduler.PvPScheduler` (main class)
- `com.example.pvpscheduler.command.**` (commands)
- `com.example.pvpscheduler.listener.**` (listeners)
- `com.example.pvpscheduler.manager.**` (managers)
- `com.example.pvpscheduler.model.**` (models)
- `com.example.pvpscheduler.util.**` (utilities)
- `com.example.pvpscheduler.license.**` (license)

### Obfuscation Dictionary

File `obfuscation-dictionary.txt` chứa các tên ngắn để thay thế tên class/method:
- `a`, `b`, `c`, ... `z`
- `aa`, `ab`, `ac`, ... `zz`

Có thể tùy chỉnh file này để thay đổi cách obfuscate.

## 🔧 Tùy Chỉnh

### Thêm Class Cần Giữ Lại

Nếu có class nào cần giữ nguyên tên, thêm vào `pom.xml`:

```xml
<option>-keep class com.example.pvpscheduler.yourpackage.** { *; }</option>
```

### Thay Đổi Mức Độ Obfuscation

Trong `pom.xml`, có thể điều chỉnh:
- `-optimizationpasses 5` - Số lần tối ưu (tăng để obfuscate mạnh hơn)
- `-overloadaggressively` - Overload method để khó đọc hơn

### Tắt Obfuscation

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

## ⚠️ Lưu Ý

1. **Test kỹ sau khi obfuscate**: Code obfuscated có thể gây lỗi nếu không cấu hình đúng
2. **Giữ lại source code**: Obfuscation làm code khó đọc, cần source code để debug
3. **Không phải bảo mật hoàn hảo**: Obfuscation chỉ làm khó đọc, không phải không thể reverse
4. **Tương thích Bukkit**: Cần giữ lại các class/method mà Bukkit cần

## 🐛 Troubleshooting

### Plugin không chạy sau khi obfuscate

- Kiểm tra xem có class nào bị obfuscate nhầm không
- Thêm class đó vào `-keep` option

### Lỗi ClassNotFoundException

- Thêm class bị thiếu vào `-keep` option
- Kiểm tra package name có đúng không

### File quá lớn

- Giảm `-optimizationpasses`
- Loại bỏ các option không cần thiết

