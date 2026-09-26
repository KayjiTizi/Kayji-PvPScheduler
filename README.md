# Kayji-PvPScheduler

Plugin Minecraft (Spigot) **lịch PvP theo khung giờ**: tự bật/tắt PvP theo giờ trong tuần, cộng thêm hệ thống **nhiệm vụ (quest)**, **thống kê**, **bảng xếp hạng** và **menu admin**.

> **Tác giả:** Kayji · **Phiên bản:** 2.0 · **API:** 1.16+ · **Softdepend:** GriefPrevention

## Tính năng

- **Khung giờ PvP**: `start-time` / `end-time` (định dạng `HH:mm`), **ngày trong tuần** bật PvP (`enabled-days`), thông báo trước `notify-before` giây.
- **Bật/tắt thủ công** bất cứ lúc nào (`/pvp enable`, `/pvp disable`), trạng thái được lưu.
- **Thống kê người chơi**: kill, kill/death (KDR), chuỗi thắng (streak) — lưu file, tự lưu mỗi `auto-save-interval` giây.
- **Bảng xếp hạng** (`/pvp top`).
- **Nhiệm vụ PvP**: tự gán nhiệm vụ khi tham gia (`auto-assign`), giới hạn `max-quests-per-player`, cộng thưởng qua `RewardManager`.
- **Menu admin** (`/pvp menu`) + tab-complete.
- **Giám sát & bảo mật**: chống spam (`RateLimiter`), log hoạt động (`ActivityLogger`), audit log, kiểm tra toàn vẹn file/class, chống chỉnh sửa code (`CodeIntegrityChecker`), backup (`BackupManager`), monitor hiệu năng (`PerformanceMonitor`).
- **Mã hóa/obfuscate** bằng ProGuard trong build (xem các file `OBFUSCATION_*.md`).

## Bảng lệnh

Gõ trong game với dấu `/` (alias: `/pvpscheduler`, `/pvps`):

| Lệnh | Quyền | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `/pvp status` | `pvpscheduler.status` | **mọi người** | Xem trạng thái PvP hiện tại |
| `/pvp top` | `pvpscheduler.top` | **mọi người** | Bảng xếp hạng |
| `/pvp stats [player]` | `pvpscheduler.stats.view` | **mọi người** | Xem thống kê (của mình hoặc người khác) |
| `/pvp kills` / `/pvp kill` | `pvpscheduler.stats.view` | **mọi người** | Số kill |
| `/pvp kdr` / `/pvp kd` | `pvpscheduler.stats.view` | **mọi người** | Tỉ lệ KDR |
| `/pvp streak` / `/pvp streaks` | `pvpscheduler.stats.view` | **mọi người** | Chuỗi thắng |
| `/pvp quest` | `pvpscheduler.quest.view` | **mọi người** | Xem nhiệm vụ |
| `/pvp quest assign` | `pvpscheduler.quest.assign` | **mọi người** | Nhận nhiệm vụ |
| `/pvp help` | — | mọi người | Hướng dẫn |
| `/pvp setstart <HH:mm>` | `pvpscheduler.setstart` | op | Đặt giờ bắt đầu PvP |
| `/pvp setend <HH:mm>` | `pvpscheduler.setend` | op | Đặt giờ kết thúc PvP |
| `/pvp enable` | `pvpscheduler.enable` | op | Bật PvP ngay |
| `/pvp disable` | `pvpscheduler.disable` | op | Tắt PvP ngay |
| `/pvp reload` | `pvpscheduler.reload` | op | Tải lại `config.yml` |
| `/pvp menu` | `pvpscheduler.admin` | op | Mở menu quản trị |
| `/pvp admin` | `pvpscheduler.admin` | op | Công cụ quản trị |

> Quyền cha `pvpscheduler.*` (op) gộp tất cả; nhóm `pvpscheduler.use` (mọi người) gồm status/top/stats/quest.

## Cấu hình

```yaml
start-time: "18:00"        # giờ bắt đầu PvP (HH:mm, 24h)
end-time: "06:00"          # giờ kết thúc
notify-before: 30          # thông báo trước 30 giây

enabled-days:              # để trống = mọi ngày
  - MONDAY
  - TUESDAY
  # ... đến SUNDAY

stats:
  save-stats: true
  auto-save-interval: 300  # tự lưu mỗi 300 giây

quests:
  enabled: true
  auto-assign: true
  max-quests-per-player: 3
```

## Cài đặt

```bash
mvn clean package
```

Có 3 bản jar trong `target/`:

| File | Ghi chú |
| --- | --- |
| `PvPScheduler-Kayji-1.0-SNAPSHOT.jar` | Bản thường |
| `PvPScheduler-Kayji-1.0-SNAPSHOT-shaded.jar` | Đóng gói dependency (okhttp, gson, asm) |
| `PvPScheduler-Kayji-1.0-SNAPSHOT-shaded-obfuscated.jar` | **Bản obfuscate bằng ProGuard** (khuyến nghị phân phối) |

Copy 1 file vào thư mục `plugins/` rồi restart.

## Cấu trúc dự án

```
├── pom.xml + proguard.conf                 Build + obfuscation
├── OBFUSCATION_*.md / SECURITY.md          Tài liệu build/bảo mật
└── src/main
    ├── java/com/example/pvpscheduler
    │   ├── PvPScheduler.java               Lớp chính
    │   ├── command/                        /pvp + tab-complete
    │   ├── listener/                       Sự kiện PvP + bảo mật
    │   ├── manager/                        PvP, Quest, Stats, Leaderboard,
    │   │                                   AntiCheat, Reward, Backup, Audit...
    │   ├── security/                       Kiểm tra toàn vẹn mã nguồn
    │   └── model/ + util/ + tools/
    └── resources
        ├── plugin.yml                      16 subcommand + cây quyền
        └── config.yml                      Khung giờ, ngày, stats, quest
```

> Thư mục `testplugins/` (server Leaf để test, ~84 MB), log build và log công cụ **không được commit** — xem `.gitignore`.

## Giấy phép

[GNU General Public License v3.0](LICENSE)
