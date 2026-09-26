# Hướng Dẫn Tạo Obfuscation Với Ký Tự Dễ Nhầm Lẫn (I, l, 1)

## 🎯 Mục Tiêu

Tạo obfuscation giống như trong ảnh - sử dụng các ký tự dễ nhầm lẫn như `I` (chữ i hoa), `l` (chữ L thường), và `1` (số một) để tạo ra các tên như:
- `IllllIlllIIllIl`
- `IIIIllIIIIIlIl`
- `IIIllIIIlIIIIIl`
- `IIllIIIlIlIIlII`

## ⚠️ Lưu Ý Quan Trọng

**Các ký tự này rất khó phân biệt khi đọc code:**
- `I` (chữ i hoa) vs `l` (chữ L thường) vs `1` (số một)
- Trong nhiều font, chúng trông gần như giống nhau
- Điều này làm cho code rất khó đọc và reverse engineer

## 🚀 Cách Thực Hiện

### Bước 1: Tạo Dictionary File

Sử dụng script Python để tạo dictionary:

```bash
python generate-confusing-dictionary.py
```

Script này sẽ tạo file `obfuscation-dictionary.txt` với hàng nghìn tên chỉ sử dụng `I`, `l`, và `1`.

### Bước 2: Cấu Hình ProGuard

Đảm bảo file `proguard.conf` có các dòng sau:

```proguard
# Sử dụng dictionary cho obfuscation
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# Obfuscate mạnh
-repackageclasses ''
-overloadaggressively
-useuniqueclassmembernames
-allowaccessmodification
```

### Bước 3: Build

```bash
mvn clean package
```

## 📝 Dictionary Mẫu

File `obfuscation-dictionary.txt` sẽ chứa các tên như:

```
IIIIllIIIIIlIl
IllllIlllIIllIl
IIIllIIIlIIIIIl
IIllIIIlIlIIlII
IlII111
IIllllllIIIllllll
IlIIIIIIIIIIIIIl
IIllIlllIIIlllllIl
IlIIIIIIIIIIIIIIl
IIllIIllIIllIIll
lIIlIIlIIlIIlIIl
1II1ll1II1ll1II1
...
```

## 🔧 Tùy Chỉnh Script

Bạn có thể chỉnh sửa file `generate-confusing-dictionary.py` để:

### Thay đổi độ dài tên:
```python
# Trong hàm generate_smart_combinations
length = random.randint(8, 25)  # Thay đổi 8-25 thành giá trị bạn muốn
```

### Thay đổi số lượng tên:
```python
# Trong hàm main()
words = generate_smart_combinations(count=3000)  # Thay đổi 3000 thành số bạn muốn
```

### Thêm ký tự khác:
```python
# Trong hàm generate_smart_combinations
chars = ['I', 'l', '1', 'O', '0']  # Thêm O và 0 (cũng dễ nhầm)
```

## 🎨 Các Pattern Khác

### Pattern 1: Chỉ I và l
```python
chars = ['I', 'l']
```

### Pattern 2: I, l, 1, O, 0
```python
chars = ['I', 'l', '1', 'O', '0']
```

### Pattern 3: Tất cả ký tự dễ nhầm
```python
chars = ['I', 'l', '1', 'O', '0', 'S', 's', '5']
```

## ⚡ Quick Start

1. **Chạy script:**
   ```bash
   python generate-confusing-dictionary.py
   ```

2. **Kiểm tra file đã tạo:**
   ```bash
   head -20 obfuscation-dictionary.txt
   ```

3. **Build project:**
   ```bash
   mvn clean package
   ```

4. **Kiểm tra kết quả:**
   ```bash
   # Decompile JAR để xem code đã obfuscate
   jar tf target/*-shaded-obfuscated.jar | head -20
   ```

## 🔍 Kiểm Tra Kết Quả

Sau khi build, decompile JAR để xem code:

```bash
# Sử dụng JD-GUI hoặc CFR
java -jar cfr.jar your-project-shaded-obfuscated.jar --outputdir decompiled/
```

Bạn sẽ thấy code như:
```java
public class IllllIlllIIllIl {
    private IIIIllIIIIIlIl field;
    
    public void IIIllIIIlIIIIIl() {
        // code
    }
}
```

## ⚠️ Cảnh Báo

1. **Khó debug:** Code obfuscated với các ký tự này rất khó đọc và debug
2. **Mapping file:** Luôn lưu file `proguard_map.txt` để có thể map lại tên gốc
3. **Test kỹ:** Test đầy đủ sau khi obfuscate vì rất khó debug lỗi
4. **Backup:** Backup code gốc và mapping file

## 📚 Tài Liệu Liên Quan

- Xem `FIX_OKHTTP_OBFUSCATION_GUIDE.md` để biết cách setup obfuscation cơ bản
- Xem `OBFUSCATION_DICTIONARY_GUIDE.md` để biết cách tạo dictionary tùy chỉnh

---

**Tạo bởi:** AI Assistant  
**Ngày:** 2025-11-30  
**Phiên bản:** 1.0

