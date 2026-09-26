# Hướng Dẫn Fix Lỗi OkHttp Trong Quá Trình Obfuscation với ProGuard

## 📋 Tổng Quan

Tài liệu này mô tả cách fix lỗi `NoClassDefFoundError: okhttp3.OkHttpClient$Builder` khi sử dụng ProGuard để obfuscate JAR file có chứa dependencies như OkHttp, Gson, v.v.

## 🔴 Vấn Đề Gặp Phải

### Lỗi Runtime
```
java.lang.NoClassDefFoundError: okhttp3/OkHttpClient$Builder
    at com.example.pvpscheduler.license.LicenseClient.<init>(LicenseClient.java:66)
```

### Nguyên Nhân
1. **ProGuard đang obfuscate JAR không có dependencies**: ProGuard được cấu hình để obfuscate file JAR gốc (không có dependencies) thay vì file shaded JAR (đã bao gồm tất cả dependencies).
2. **Dependencies không được include**: OkHttp, Gson và các dependencies khác không được đóng gói vào JAR cuối cùng.
3. **Thiếu keep rules**: ProGuard có thể xóa hoặc obfuscate các class cần thiết của dependencies.

## ✅ Giải Pháp

### Bước 1: Kiểm Tra Cấu Hình Maven Shade Plugin

Đảm bảo `maven-shade-plugin` được cấu hình đúng để tạo shaded JAR với tất cả dependencies:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <executions>
        <execution>
            <id>shade-jar</id>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <!-- QUAN TRỌNG: Tạo file JAR riêng với classifier "shaded" -->
                <shadedArtifactAttached>true</shadedArtifactAttached>
                <shadedClassifierName>shaded</shadedClassifierName>
                <createDependencyReducedPom>false</createDependencyReducedPom>
                <artifactSet>
                    <excludes>
                        <exclude>org.spigotmc:spigot-api</exclude>
                        <!-- Exclude các dependencies provided khác nếu cần -->
                    </excludes>
                </artifactSet>
                <filters>
                    <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>META-INF/*.SF</exclude>
                            <exclude>META-INF/*.DSA</exclude>
                            <exclude>META-INF/*.RSA</exclude>
                        </excludes>
                    </filter>
                </filters>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.example.pvpscheduler.PvPScheduler</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Lưu ý quan trọng:**
- `shadedArtifactAttached=true`: Tạo file JAR riêng với classifier
- `shadedClassifierName=shaded`: Tên classifier sẽ là "shaded"
- File output sẽ là: `${project.build.finalName}-shaded.jar`

### Bước 2: Cấu Hình ProGuard Sử Dụng Shaded JAR

**QUAN TRỌNG**: ProGuard phải obfuscate file **shaded JAR**, không phải file JAR gốc!

```xml
<plugin>
    <groupId>com.github.wvengen</groupId>
    <artifactId>proguard-maven-plugin</artifactId>
    <version>2.6.0</version>
    <dependencies>
        <dependency>
            <groupId>com.guardsquare</groupId>
            <artifactId>proguard-base</artifactId>
            <version>7.2.2</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>proguard</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <proguardVersion>7.2.2</proguardVersion>
        <obfuscate>true</obfuscate>
        <!-- SỬA: Sử dụng file shaded JAR làm input -->
        <injar>${project.build.finalName}-shaded.jar</injar>
        <outjar>${project.build.finalName}-obfuscated.jar</outjar>
        <outputDirectory>${project.build.directory}</outputDirectory>
        <attach>true</attach>
        <attachArtifactClassifier>obfuscated</attachArtifactClassifier>
        <proguardInclude>proguard.conf</proguardInclude>
    </configuration>
</plugin>
```

**Lưu ý:**
- `injar`: Phải là `${project.build.finalName}-shaded.jar` (file đã có dependencies)
- `outjar`: Tên file output sau khi obfuscate
- ProGuard sẽ tự động append tên input vào output, nên file cuối cùng sẽ là: `${project.build.finalName}-shaded-obfuscated.jar`

### Bước 3: Cấu Hình ProGuard Keep Rules

Thêm các keep rules vào file `proguard.conf` để giữ lại các class cần thiết:

#### 3.1. Keep Rules cho OkHttp

```proguard
# Keep OkHttp và các dependencies của nó (đã được shade vào JAR)
# QUAN TRỌNG: Phải keep cả tên class và members để tránh ClassNotFoundException
-keep class okhttp3.** {
    *;
}
-keepnames class okhttp3.** {
    *;
}
# Keep cụ thể inner classes của OkHttpClient (bao gồm Builder)
-keep class okhttp3.OkHttpClient$* {
    *;
}
-keepnames class okhttp3.OkHttpClient$* {
    *;
}
# Keep cụ thể OkHttpClient$Builder
-keep class okhttp3.OkHttpClient$Builder {
    *;
}
-keepnames class okhttp3.OkHttpClient$Builder {
    *;
}
# Keep OkHttp dependencies
-keep class okio.** {
    *;
}
-keepnames class okio.** {
    *;
}
-keepclassmembers class okhttp3.** {
    *;
}
-keepclassmembers class okio.** {
    *;
}
```

#### 3.2. Keep Rules cho Gson

```proguard
# Keep Gson và các dependencies của nó (đã được shade vào JAR)
-keep class com.google.gson.** {
    *;
}
-keepnames class com.google.gson.** {
    *;
}
-keepclassmembers class com.google.gson.** {
    *;
}
```

#### 3.3. Keep Rules cho ASM (nếu sử dụng)

```proguard
# Keep ASM classes (được sử dụng bởi StackMapFrameGenerator)
-keep class org.objectweb.asm.** {
    *;
}
-keepnames class org.objectweb.asm.** {
    *;
}
-keepclassmembers class org.objectweb.asm.** {
    *;
}
```

#### 3.4. Dontwarn Rules

Thêm các dontwarn rules để bỏ qua warnings không cần thiết:

```proguard
# Bỏ qua warning về Java standard library
-dontwarn java.**
-dontwarn javax.**

# Bỏ qua warning về Android và các platform khác (OkHttp có thể reference nhưng không cần)
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn dalvik.**
-dontwarn android.**
-dontwarn sun.misc.Unsafe
-dontwarn sun.security.ssl.**
-dontwarn java.nio.file.Files
-dontwarn java.sql.Date
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.openjsse.**
```

#### 3.5. Các Rules Quan Trọng Khác

```proguard
# QUAN TRỌNG: Không xóa các class không được sử dụng trực tiếp
-dontshrink

# Giữ lại các annotation và metadata cần thiết
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
-keepattributes StackMapTable

# Tắt optimization và preverification để tránh lỗi
-dontoptimize
-dontpreverify

# Bỏ qua warnings
-ignorewarnings
```

### Bước 4: Cập Nhật StackMapFrameGenerator (nếu có)

Nếu bạn sử dụng tool để generate stackmap frames sau obfuscation, cần cập nhật path:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>generate-stackmap-frames</id>
            <phase>package</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>com.example.pvpscheduler.tools.StackMapFrameGenerator</mainClass>
                <classpathScope>compile</classpathScope>
                <arguments>
                    <!-- SỬA: Sử dụng đúng tên file mà ProGuard tạo -->
                    <argument>${project.build.directory}/${project.build.finalName}-shaded-obfuscated.jar</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Lưu ý:** ProGuard sẽ tự động append tên input JAR vào output, nên nếu input là `-shaded.jar` và output là `-obfuscated.jar`, file cuối cùng sẽ là `-shaded-obfuscated.jar`.

## 🔍 Kiểm Tra Build

### Build Command
```bash
mvn clean package
```

### Files Được Tạo
Sau khi build thành công, bạn sẽ có các files sau trong thư mục `target/`:

1. `${project.build.finalName}.jar` - JAR gốc (không có dependencies)
2. `${project.build.finalName}-shaded.jar` - JAR đã shade (có đầy đủ dependencies)
3. `${project.build.finalName}-shaded-obfuscated.jar` - JAR đã obfuscate (file cuối cùng để deploy)

### File Sử Dụng
**Sử dụng file `${project.build.finalName}-shaded-obfuscated.jar` để deploy lên server.**

## 📝 Checklist

Khi áp dụng cho dự án khác, đảm bảo:

- [ ] Maven Shade Plugin được cấu hình với `shadedArtifactAttached=true`
- [ ] ProGuard `injar` sử dụng `${project.build.finalName}-shaded.jar`
- [ ] ProGuard có keep rules cho tất cả dependencies cần thiết (OkHttp, Gson, v.v.)
- [ ] ProGuard có dontwarn rules cho các platform classes không cần thiết
- [ ] StackMapFrameGenerator (nếu có) sử dụng đúng tên file output
- [ ] Build thành công và file obfuscated JAR được tạo
- [ ] Test runtime để đảm bảo không còn `NoClassDefFoundError`

## 🎯 Tóm Tắt Thay Đổi Chính

1. **pom.xml - ProGuard Plugin:**
   ```xml
   <!-- TRƯỚC (SAI) -->
   <injar>${project.build.finalName}.jar</injar>
   
   <!-- SAU (ĐÚNG) -->
   <injar>${project.build.finalName}-shaded.jar</injar>
   ```

2. **proguard.conf - Thêm Keep Rules:**
   - Keep rules cho OkHttp và okio
   - Keep rules cho Gson
   - Keep rules cho ASM (nếu cần)
   - Dontwarn rules cho platform classes

3. **pom.xml - StackMapFrameGenerator (nếu có):**
   ```xml
   <!-- Sử dụng đúng tên file output -->
   <argument>${project.build.directory}/${project.build.finalName}-shaded-obfuscated.jar</argument>
   ```

## ⚠️ Lưu Ý Quan Trọng

1. **Thứ tự build:**
   - Maven Shade Plugin chạy trước (tạo `-shaded.jar`)
   - ProGuard chạy sau (obfuscate `-shaded.jar` → `-shaded-obfuscated.jar`)
   - StackMapFrameGenerator chạy cuối (xử lý `-shaded-obfuscated.jar`)

2. **Tên file output:**
   - ProGuard sẽ tự động append tên input vào output
   - Input: `-shaded.jar` + Output: `-obfuscated.jar` = `-shaded-obfuscated.jar`

3. **Dependencies scope:**
   - Chỉ shade các dependencies có scope `compile` hoặc `runtime`
   - Không shade các dependencies có scope `provided` (như Spigot API)

4. **Keep rules:**
   - Luôn keep cả class name và members cho dependencies quan trọng
   - Sử dụng `-keepnames` để giữ nguyên tên class (tránh obfuscate)

## 🐛 Troubleshooting

### Vẫn gặp lỗi NoClassDefFoundError

1. Kiểm tra file JAR có chứa class không:
   ```bash
   jar tf target/your-project-shaded-obfuscated.jar | grep okhttp3
   ```

2. Kiểm tra keep rules có đúng không:
   - Đảm bảo có `-keep class okhttp3.** { *; }`
   - Đảm bảo có `-keepnames class okhttp3.** { *; }`

3. Kiểm tra ProGuard có sử dụng đúng input file không:
   - Xem log build để confirm ProGuard đọc file `-shaded.jar`

### Build fail với warnings

- Thêm `-ignorewarnings` vào proguard.conf nếu warnings không ảnh hưởng
- Thêm `-dontwarn` cho các class không cần thiết

### File output không đúng tên

- Kiểm tra cấu hình `injar` và `outjar` trong ProGuard plugin
- ProGuard sẽ tự động append tên input, nên cần tính toán đúng

## 🔐 Hướng Dẫn Làm Rối Mã (Obfuscation) Nâng Cao

### Tổng Quan Obfuscation

Obfuscation là quá trình làm rối mã nguồn để bảo vệ code khỏi reverse engineering. ProGuard cung cấp nhiều tùy chọn để obfuscate code một cách hiệu quả.

### Các Kỹ Thuật Obfuscation

#### 1. Dictionary-Based Obfuscation (Làm Rối Bằng Từ Điển)

Thay vì sử dụng tên ngắn như `a`, `b`, `c`, bạn có thể sử dụng dictionary với các từ ngẫu nhiên để làm rối mã khó đọc hơn.

**Tạo file dictionary:**

Tạo file `obfuscation-dictionary.txt` trong thư mục gốc của project:

```
# obfuscation-dictionary.txt
# Mỗi dòng là một từ sẽ được dùng để đặt tên class/method/field
# Càng nhiều từ, obfuscation càng mạnh

alpha
beta
gamma
delta
epsilon
zeta
eta
theta
iota
kappa
lambda
mu
nu
xi
omicron
pi
rho
sigma
tau
upsilon
phi
chi
psi
omega
# ... thêm nhiều từ khác
```

**Cấu hình ProGuard:**

```proguard
# Sử dụng dictionary cho obfuscation
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt
```

**Lưu ý:**
- `-obfuscationdictionary`: Dictionary cho tất cả identifiers
- `-classobfuscationdictionary`: Dictionary riêng cho class names
- `-packageobfuscationdictionary`: Dictionary riêng cho package names

#### 2. Aggressive Obfuscation (Làm Rối Mạnh)

```proguard
# Cho phép thay đổi access modifier để obfuscate tốt hơn
-allowaccessmodification

# Repackage tất cả classes vào root package (xóa package structure)
-repackageclasses ''

# Overload methods aggressively (tạo nhiều methods cùng tên với signatures khác nhau)
-overloadaggressively

# Sử dụng tên unique cho mỗi class member
-useuniqueclassmembernames

# Không bỏ qua non-public library classes
-dontskipnonpubliclibraryclasses

# Không bỏ qua non-public library class members
-dontskipnonpubliclibraryclassmembers
```

#### 3. String Encryption (Mã Hóa String)

ProGuard không hỗ trợ string encryption trực tiếp, nhưng bạn có thể sử dụng các plugin khác như:
- **Stringer Java Obfuscator**
- **Allatori Java Obfuscator**
- **Zelix KlassMaster**

Hoặc tự implement string encryption trong code trước khi obfuscate.

#### 4. Control Flow Obfuscation

ProGuard không hỗ trợ control flow obfuscation. Nếu cần, sử dụng các tool chuyên dụng khác.

### Cấu Hình Obfuscation Hoàn Chỉnh

File `proguard.conf` mẫu với obfuscation mạnh:

```proguard
# ============================================
# PHẦN 1: BASIC CONFIGURATION
# ============================================

# Không shrink (giữ lại tất cả classes)
-dontshrink

# Không optimize (tránh lỗi với Bukkit API)
-dontoptimize

# Không preverify (sẽ generate stackmap frames sau)
-dontpreverify

# Bỏ qua warnings
-ignorewarnings

# ============================================
# PHẦN 2: DICTIONARY-BASED OBFUSCATION
# ============================================

# Sử dụng dictionary cho obfuscation
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# ============================================
# PHẦN 3: AGGRESSIVE OBFUSCATION
# ============================================

# Cho phép thay đổi access modifier
-allowaccessmodification

# Repackage tất cả vào root package
-repackageclasses ''

# Overload methods aggressively
-overloadaggressively

# Sử dụng tên unique cho members
-useuniqueclassmembernames

# Không bỏ qua non-public classes/members
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

# ============================================
# PHẦN 4: KEEP RULES (Giữ lại những gì cần thiết)
# ============================================

# Keep main class
-keep public class com.example.pvpscheduler.PvPScheduler { *; }

# Keep entry points (commands, listeners, etc.)
-keep public class com.example.pvpscheduler.command.** { *; }
-keep public class com.example.pvpscheduler.listener.** { *; }

# Keep dependencies (OkHttp, Gson, etc.)
-keep class okhttp3.** { *; }
-keepnames class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keepnames class com.google.gson.** { *; }

# Keep enum classes (không obfuscate enum)
-keep enum * {
    *;
}
-keepnames enum * {
    *;
}

# Keep attributes cần thiết
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
-keepattributes StackMapTable

# ============================================
# PHẦN 5: DONTWARN RULES
# ============================================

-dontwarn java.**
-dontwarn javax.**
-dontwarn org.bukkit.**
-dontwarn org.bouncycastle.**
-dontwarn android.**
-dontwarn dalvik.**
```

### Tạo Dictionary File Tự Động

Script Python để tạo dictionary file với nhiều từ ngẫu nhiên:

```python
# generate-dictionary.py
import random
import string

# Danh sách từ gốc
base_words = [
    'alpha', 'beta', 'gamma', 'delta', 'epsilon', 'zeta', 'eta', 'theta',
    'iota', 'kappa', 'lambda', 'mu', 'nu', 'xi', 'omicron', 'pi', 'rho',
    'sigma', 'tau', 'upsilon', 'phi', 'chi', 'psi', 'omega',
    'nova', 'quasar', 'pulsar', 'nebula', 'galaxy', 'star', 'planet',
    'comet', 'asteroid', 'meteor', 'cosmos', 'universe', 'void', 'space'
]

# Tạo các biến thể
words = set(base_words)

# Thêm số vào cuối
for word in base_words:
    for i in range(10):
        words.add(f"{word}{i}")
        words.add(f"{word}_{i}")

# Thêm ký tự ngẫu nhiên
for _ in range(1000):
    length = random.randint(4, 10)
    word = ''.join(random.choices(string.ascii_lowercase, k=length))
    words.add(word)

# Ghi vào file
with open('obfuscation-dictionary.txt', 'w') as f:
    for word in sorted(words):
        f.write(f"{word}\n")

print(f"Generated {len(words)} words in obfuscation-dictionary.txt")
```

Chạy script:
```bash
python generate-dictionary.py
```

### Mức Độ Obfuscation

#### Mức 1: Cơ Bản (Basic)
```proguard
# Chỉ obfuscate tên class/method cơ bản
-repackageclasses ''
```

#### Mức 2: Trung Bình (Medium)
```proguard
-repackageclasses ''
-overloadaggressively
-useuniqueclassmembernames
```

#### Mức 3: Mạnh (Strong) - Khuyến Nghị
```proguard
-obfuscationdictionary obfuscation-dictionary.txt
-repackageclasses ''
-overloadaggressively
-useuniqueclassmembernames
-allowaccessmodification
```

#### Mức 4: Cực Mạnh (Very Strong)
```proguard
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt
-repackageclasses ''
-overloadaggressively
-useuniqueclassmembernames
-allowaccessmodification
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
```

### Best Practices

1. **Luôn test sau khi obfuscate:**
   - Obfuscation có thể gây lỗi runtime
   - Test đầy đủ các chức năng trước khi release

2. **Giữ lại mapping file:**
   - ProGuard tạo file `proguard_map.txt` chứa mapping giữa tên gốc và tên obfuscated
   - Lưu file này để debug nếu cần

3. **Không obfuscate dependencies:**
   - Chỉ obfuscate code của bạn
   - Keep tất cả dependencies (OkHttp, Gson, v.v.)

4. **Giữ lại entry points:**
   - Main class, commands, listeners phải được keep
   - Nếu không, plugin sẽ không hoạt động

5. **Enum classes:**
   - Không nên obfuscate enum vì có thể gây lỗi
   - Luôn keep enum classes

### Kiểm Tra Kết Quả Obfuscation

#### 1. Kiểm tra file JAR
```bash
# Xem các class trong JAR
jar tf target/your-project-shaded-obfuscated.jar | head -20

# Tìm class đã obfuscate
jar tf target/your-project-shaded-obfuscated.jar | grep -E "^[a-z]+\.class$"
```

#### 2. Decompile để kiểm tra
Sử dụng tools như:
- **JD-GUI**: Decompile JAR để xem code đã obfuscate
- **Fernflower**: Command-line decompiler
- **CFR**: Another decompiler

```bash
# Sử dụng JD-GUI (GUI tool)
# Hoặc sử dụng CFR
java -jar cfr.jar your-project-shaded-obfuscated.jar --outputdir decompiled/
```

#### 3. Kiểm tra mapping file
```bash
# Xem mapping giữa tên gốc và tên obfuscated
cat target/proguard_map.txt | head -50
```

### Troubleshooting Obfuscation

#### Lỗi: ClassNotFoundException sau obfuscation

**Nguyên nhân:** Class bị obfuscate nhưng được reference bằng tên gốc.

**Giải pháp:**
```proguard
# Keep class đó
-keep class com.example.pvpscheduler.YourClass { *; }
```

#### Lỗi: Method không tìm thấy

**Nguyên nhân:** Method bị obfuscate nhưng được gọi bằng reflection.

**Giải pháp:**
```proguard
# Keep method đó
-keepclassmembers class com.example.pvpscheduler.YourClass {
    public void yourMethod();
}
```

#### Lỗi: Enum không hoạt động

**Nguyên nhân:** Enum bị obfuscate.

**Giải pháp:**
```proguard
# Luôn keep enum
-keep enum * {
    *;
}
-keepnames enum * {
    *;
}
```

### So Sánh Trước/Sau Obfuscation

#### Trước Obfuscation:
```java
package com.example.pvpscheduler.manager;

public class PvPManager {
    private StatsManager statsManager;
    
    public void startScheduler() {
        // code
    }
}
```

#### Sau Obfuscation (với dictionary):
```java
// Package đã bị xóa (repackageclasses '')
public class alpha {
    private beta gamma;
    
    public void delta() {
        // code
    }
}
```

### Tài Nguyên Bổ Sung

- **Tạo dictionary tự động:** Sử dụng script Python ở trên
- **Dictionary mẫu:** Có thể tìm các dictionary file mẫu trên GitHub
- **Obfuscation tools khác:** Allatori, Zelix KlassMaster, Stringer

## 📚 Tài Liệu Tham Khảo

- [ProGuard Manual](https://www.guardsquare.com/manual/configuration/usage)
- [ProGuard Obfuscation Options](https://www.guardsquare.com/manual/configuration/usage#obfuscationoptions)
- [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/)
- [ProGuard Maven Plugin](https://github.com/wvengen/proguard-maven-plugin)
- [Dictionary-Based Obfuscation](https://www.guardsquare.com/manual/configuration/usage#obfuscationdictionary)

---

**Tạo bởi:** AI Assistant  
**Ngày:** 2025-11-30  
**Phiên bản:** 2.0 (Added Obfuscation Guide)

