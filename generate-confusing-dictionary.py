#!/usr/bin/env python3
"""
Script để tạo obfuscation dictionary với các ký tự dễ nhầm lẫn (I, l, 1)
Tạo ra các tên như: IllllIlllIIllIl, IIIIllIIIIIlIl, v.v.
"""

import random
import itertools

def generate_confusing_name(min_length=8, max_length=20):
    """
    Tạo một tên ngẫu nhiên chỉ sử dụng I, l, và 1
    """
    length = random.randint(min_length, max_length)
    chars = ['I', 'l', '1']
    return ''.join(random.choices(chars, k=length))

def generate_all_combinations(max_length=15):
    """
    Tạo tất cả các combination có thể với I, l, 1
    (chỉ cho length nhỏ để tránh quá nhiều)
    """
    chars = ['I', 'l', '1']
    combinations = set()
    
    # Generate cho length từ 3 đến max_length
    for length in range(3, max_length + 1):
        # Sử dụng itertools để tạo combinations
        # Nhưng chỉ lấy một số lượng giới hạn để tránh quá nhiều
        for _ in range(1000):  # Giới hạn 1000 cho mỗi length
            combo = ''.join(random.choices(chars, k=length))
            combinations.add(combo)
    
    return sorted(combinations)

def generate_smart_combinations(count=2000):
    """
    Tạo các combination thông minh hơn:
    - Đảm bảo có sự đa dạng
    - Tránh patterns quá đơn giản (như IIIIIII)
    - Tạo các pattern phức tạp hơn
    """
    combinations = set()
    chars = ['I', 'l', '1', 'O', '0']
    
    # Pattern 1: Alternating (I l I l ...)
    for length in range(8, 20):
        pattern1 = ''.join(['I' if i % 2 == 0 else 'l' for i in range(length)])
        pattern2 = ''.join(['l' if i % 2 == 0 else 'I' for i in range(length)])
        combinations.add(pattern1)
        combinations.add(pattern2)
    
    # Pattern 2: Random với đảm bảo đa dạng
    while len(combinations) < count:
        length = random.randint(8, 25)
        name = ''
        
        # Đảm bảo có ít nhất 2 loại ký tự khác nhau
        name += random.choice(chars)
        for _ in range(length - 1):
            # 70% giữ nguyên, 30% đổi
            if random.random() < 0.3:
                name += random.choice(chars)
            else:
                name += name[-1] if random.random() < 0.5 else random.choice(chars)
        
        # Đảm bảo có ít nhất 2 loại ký tự
        if len(set(name)) >= 2:
            combinations.add(name)
    
    return sorted(combinations)

def main():
    print("Generating confusing obfuscation dictionary...")
    print("   (Using I, l, 1, O, and 0 characters)")
    
    # Tạo dictionary
    words = generate_smart_combinations(count=3000)
    
    # Thêm một số pattern đặc biệt
    special_patterns = [
        'IIIIllIIIIIlIl',
        'IllllIlllIIllIl',
        'IIIllIIIlIIIIIl',
        'IIllIIIlIlIIlII',
        'IlII111',
        'IIllllllIIIllllll',
        'IlIIIIIIIIIIIIIl',
        'IIllIlllIIIlllllIl',
        'IlIIIIIIIIIIIIIIl',
    ]
    
    for pattern in special_patterns:
        words.append(pattern)
    
    # Loại bỏ duplicates và sort
    words = sorted(set(words))
    
    # Ghi vào file
    with open('obfuscation-dictionary.txt', 'w', encoding='utf-8') as f:
        for word in words:
            f.write(f"{word}\n")
    
    print(f"Generated {len(words)} confusing names")
    print(f"Saved to: obfuscation-dictionary.txt")
    print(f"\nSample names:")
    for i, word in enumerate(words[:10]):
        print(f"   {i+1}. {word}")
    print(f"   ... and {len(words) - 10} more")

if __name__ == '__main__':
    main()

