# Quick Fix Checklist - OkHttp Obfuscation Issue

## ⚡ Quick Reference

### Vấn Đề
```
NoClassDefFoundError: okhttp3/OkHttpClient$Builder
```

### Nguyên Nhân
ProGuard đang obfuscate JAR không có dependencies thay vì shaded JAR.

---

## ✅ 3 Bước Fix Nhanh

### 1. Sửa ProGuard Input (pom.xml)
```xml
<!-- TRƯỚC -->
<injar>${project.build.finalName}.jar</injar>

<!-- SAU -->
<injar>${project.build.finalName}-shaded.jar</injar>
```

### 2. Thêm Keep Rules (proguard.conf)
```proguard
# OkHttp
-keep class okhttp3.** { *; }
-keepnames class okhttp3.** { *; }
-keep class okhttp3.OkHttpClient$* { *; }
-keep class okio.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepnames class com.google.gson.** { *; }

# Dontwarn
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn android.**
-dontwarn dalvik.**
```

### 3. Sửa StackMapFrameGenerator Path (nếu có)
```xml
<argument>${project.build.directory}/${project.build.finalName}-shaded-obfuscated.jar</argument>
```

---

## 📦 Files Output

- `project-shaded.jar` → Input cho ProGuard
- `project-shaded-obfuscated.jar` → **File cuối cùng để deploy**

---

## 🔍 Verify

```bash
# Build
mvn clean package

# Check file exists
ls target/*-shaded-obfuscated.jar

# Check contains OkHttp
jar tf target/*-shaded-obfuscated.jar | grep okhttp3
```

---

---

## 🔐 Obfuscation Quick Setup

### 1. Tạo Dictionary File
```bash
# Tạo file obfuscation-dictionary.txt với các từ ngẫu nhiên
# Hoặc sử dụng script Python (xem guide đầy đủ)
```

### 2. Thêm Obfuscation Rules (proguard.conf)
```proguard
# Dictionary-based obfuscation
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# Aggressive obfuscation
-repackageclasses ''
-overloadaggressively
-useuniqueclassmembernames
-allowaccessmodification
```

### 3. Verify Obfuscation
```bash
# Check obfuscated classes
jar tf target/*-shaded-obfuscated.jar | grep -E "^[a-z]+\.class$"

# Check mapping file
cat target/proguard_map.txt | head -20
```

---

**Xem file `FIX_OKHTTP_OBFUSCATION_GUIDE.md` để biết chi tiết đầy đủ về cả fix lỗi và obfuscation.**

