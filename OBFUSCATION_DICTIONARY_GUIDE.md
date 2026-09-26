# Hướng Dẫn Sử Dụng Obfuscation Dictionary

## 📖 Tổng Quan

File `obfuscation-dictionary.txt` chứa danh sách các từ sẽ được ProGuard sử dụng để đặt tên cho các class, method, và field sau khi obfuscate. Thay vì sử dụng tên ngắn như `a`, `b`, `c`, dictionary cho phép sử dụng các từ dài hơn và khó đoán hơn.

## 🎯 Mục Đích

1. **Làm rối mã tốt hơn**: Tên class/method dài hơn khó reverse engineer hơn
2. **Tránh conflict**: Với nhiều từ trong dictionary, ít khả năng trùng tên
3. **Tùy chỉnh**: Bạn có thể tạo dictionary phù hợp với nhu cầu

## 📝 Cấu Trúc File

Mỗi dòng trong file là một từ sẽ được sử dụng:

```
alpha
beta
gamma
delta
...
```

## ⚙️ Cấu Hình ProGuard

Thêm vào file `proguard.conf`:

```proguard
# Sử dụng dictionary cho tất cả identifiers
-obfuscationdictionary obfuscation-dictionary.txt

# Hoặc sử dụng dictionary riêng cho từng loại
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt
```

## 🔧 Tạo Dictionary Tùy Chỉnh

### Phương Pháp 1: Tạo Thủ Công

Mở file `obfuscation-dictionary.txt` và thêm các từ bạn muốn:

```
# Từ tiếng Hy Lạp
alpha
beta
gamma
delta
epsilon

# Từ thiên văn
nova
quasar
pulsar
nebula
galaxy

# Từ ngẫu nhiên
xylophone
quintessential
paradox
zenith
vortex
```

### Phương Pháp 2: Sử Dụng Script Python

Tạo file `generate-dictionary.py`:

```python
import random
import string

# Danh sách từ gốc
base_words = [
    # Tiếng Hy Lạp
    'alpha', 'beta', 'gamma', 'delta', 'epsilon', 'zeta', 'eta', 'theta',
    'iota', 'kappa', 'lambda', 'mu', 'nu', 'xi', 'omicron', 'pi', 'rho',
    'sigma', 'tau', 'upsilon', 'phi', 'chi', 'psi', 'omega',
    
    # Thiên văn
    'nova', 'quasar', 'pulsar', 'nebula', 'galaxy', 'star', 'planet',
    'comet', 'asteroid', 'meteor', 'cosmos', 'universe', 'void', 'space',
    
    # Từ ngẫu nhiên
    'xylophone', 'quintessential', 'paradox', 'zenith', 'vortex',
    'cryptic', 'enigma', 'mystery', 'cipher', 'code'
]

# Tạo set để tránh trùng
words = set(base_words)

# Thêm số vào cuối
for word in base_words:
    for i in range(10):
        words.add(f"{word}{i}")
        words.add(f"{word}_{i}")

# Thêm ký tự ngẫu nhiên (4-10 ký tự)
for _ in range(2000):
    length = random.randint(4, 10)
    word = ''.join(random.choices(string.ascii_lowercase, k=length))
    words.add(word)

# Ghi vào file
with open('obfuscation-dictionary.txt', 'w') as f:
    for word in sorted(words):
        f.write(f"{word}\n")

print(f"✅ Generated {len(words)} words in obfuscation-dictionary.txt")
```

Chạy script:
```bash
python generate-dictionary.py
```

### Phương Pháp 3: Sử Dụng Online Generator

Tìm các tool online để generate dictionary tự động.

## 📊 Kích Thước Dictionary

- **Nhỏ (< 100 từ)**: Obfuscation yếu, dễ bị trùng tên
- **Trung bình (100-500 từ)**: Obfuscation tốt, phù hợp hầu hết trường hợp
- **Lớn (500-2000 từ)**: Obfuscation mạnh, ít trùng tên
- **Rất lớn (> 2000 từ)**: Obfuscation rất mạnh, nhưng có thể làm chậm build

**Khuyến nghị**: 500-1000 từ là lý tưởng.

## ✅ Best Practices

1. **Không sử dụng từ có nghĩa rõ ràng:**
   ```
   ❌ BAD: manager, handler, processor
   ✅ GOOD: alpha, beta, gamma, nova
   ```

2. **Tránh từ quá ngắn:**
   ```
   ❌ BAD: a, b, c, d
   ✅ GOOD: alpha, beta, gamma (ít nhất 4-5 ký tự)
   ```

3. **Sử dụng ký tự hợp lệ:**
   - Chỉ sử dụng chữ cái (a-z, A-Z), số (0-9), và dấu gạch dưới (_)
   - Không sử dụng ký tự đặc biệt hoặc khoảng trắng

4. **Tránh từ Java keywords:**
   ```
   ❌ BAD: class, public, private, static
   ✅ GOOD: alpha, beta, gamma
   ```

5. **Tạo dictionary unique cho mỗi project:**
   - Không share dictionary giữa các project
   - Mỗi project nên có dictionary riêng

## 🔍 Kiểm Tra Dictionary

### Kiểm tra số lượng từ:
```bash
wc -l obfuscation-dictionary.txt
```

### Kiểm tra từ trùng:
```bash
sort obfuscation-dictionary.txt | uniq -d
```

### Kiểm tra từ không hợp lệ:
```bash
# Tìm các từ có ký tự đặc biệt
grep -E '[^a-zA-Z0-9_]' obfuscation-dictionary.txt
```

## 🎨 Dictionary Mẫu

### Dictionary Đơn Giản (100 từ)
```
alpha
beta
gamma
delta
epsilon
...
```

### Dictionary Trung Bình (500 từ)
Bao gồm:
- Từ tiếng Hy Lạp
- Từ thiên văn
- Từ ngẫu nhiên
- Từ với số

### Dictionary Mạnh (2000+ từ)
Bao gồm tất cả trên cộng với:
- Nhiều từ ngẫu nhiên được generate
- Nhiều biến thể (với số, với underscore)
- Từ dài hơn (6-10 ký tự)

## ⚠️ Lưu Ý

1. **File phải ở thư mục gốc của project** (cùng cấp với `pom.xml`)

2. **Encoding phải là UTF-8** hoặc ASCII

3. **Mỗi dòng một từ**, không có khoảng trắng thừa

4. **Không có dòng trống** ở cuối file (có thể gây lỗi)

5. **Backup dictionary** nếu bạn muốn reproduce build sau này

## 🐛 Troubleshooting

### Lỗi: Dictionary file not found
```
Error: Can't read [obfuscation-dictionary.txt] (No such file or directory)
```

**Giải pháp:**
- Đảm bảo file ở thư mục gốc của project
- Kiểm tra tên file chính xác (case-sensitive)

### Lỗi: Invalid dictionary entry
```
Warning: Invalid dictionary entry: 'class'
```

**Giải pháp:**
- Xóa các từ là Java keywords
- Xóa các từ có ký tự đặc biệt

### Obfuscation không hoạt động
**Nguyên nhân:** Dictionary quá nhỏ hoặc có vấn đề

**Giải pháp:**
- Tăng số lượng từ trong dictionary
- Kiểm tra cấu hình ProGuard có đúng không

## 📚 Tài Liệu Tham Khảo

- [ProGuard Dictionary Options](https://www.guardsquare.com/manual/configuration/usage#obfuscationdictionary)
- [Best Practices for Obfuscation](https://www.guardsquare.com/manual/configuration/usage#obfuscationoptions)

---

**Tạo bởi:** AI Assistant  
**Ngày:** 2025-11-30  
**Phiên bản:** 1.0



