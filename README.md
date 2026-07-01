# 📱 OCR Pro - Smart Text Recognition System

**OCR Pro** là một ứng dụng Android chuyên nghiệp, hiệu suất cao được thiết kế để trích xuất văn bản từ hình ảnh và tài liệu một cách chính xác nhất. Dự án được xây dựng với mục tiêu thực hành các tiêu chuẩn phát triển Android hiện đại nhất, sẵn sàng cho môi trường doanh nghiệp.

---

## 🌟 Tính năng nổi bật

- **⚡ OCR Thời gian thực:** Sử dụng CameraX và Google ML Kit để nhận diện văn bản tức thì với tốc độ khung hình cao.
- **🖼️ Xử lý ảnh từ thư viện:** Hỗ trợ nhập ảnh từ Gallery, xử lý và trích xuất nội dung văn bản.
- **📝 Xuất tài liệu chuyên nghiệp:** Tích hợp API Server để chuyển đổi văn bản đã quét thành định dạng **Microsoft Word (.docx)**.
- **📚 Quản lý lịch sử thông minh:** Lưu trữ và tìm kiếm các bản quét cũ cục bộ bằng Room Database.
- **🌍 Đa ngôn ngữ (Localization):** Hỗ trợ đầy đủ các ngôn ngữ: **Tiếng Việt, Tiếng Anh, Tiếng Trung**.
- **🎨 Giao diện linh hoạt:** 
  - Chế độ **Dark Mode** và **Light Mode** tùy chỉnh hoặc theo hệ thống.
  - Thiết kế theo ngôn ngữ Material 3 hiện đại.
- **⚙️ Cấu hình nâng cao:** 
  - Lựa chọn mô hình OCR (Google ML Kit vs TrOCR).
  - Tùy chọn bật/tắt tự động lưu lịch sử.
  - Quản lý bộ nhớ (Xóa toàn bộ lịch sử).

---

## 🛠 Tech Stack & Kiến trúc

Dự án áp dụng mô hình **Clean Architecture** phân lớp nghiêm ngặt kết hợp với **MVVM**, đảm bảo tính **Testability** và **Scalability**.

### Công nghệ lõi:
- **Ngôn ngữ:** Kotlin 100% (Coroutines & Flow cho lập trình bất đồng bộ).
- **Giao diện:** Jetpack Compose (Declarative UI).
- **Dependency Injection:** Hilt (Dagger) - Quản lý vòng đời object chuyên nghiệp.
- **Lưu trữ dữ liệu:**
  - **Room DB:** Lưu trữ dữ liệu cấu trúc (Lịch sử OCR).
  - **DataStore Preferences:** Lưu trữ cấu hình người dùng (Theme, Language, Settings).
- **Xử lý ảnh & AI:**
  - **CameraX:** Chụp ảnh và phân tích luồng video.
  - **ML Kit:** Engine nhận dạng văn bản offline nhanh chóng.
- **Networking:** Retrofit 2 & OkHttp 4 (Xử lý API upload & download file).
- **Khác:** Coil (Load ảnh), Navigation Compose, Splash Screen API, Accompanist.

---

## 🏗 Cấu trúc dự án (Project Structure)

```text
com.example.ocr/
├── core/               # Common utilities, DI Modules, Constants, Extensions
├── data/               # Data Layer: Repositories Impl, Local DB (Room), DataStore, Remote API
├── domain/             # Domain Layer: Business Models, Repository Interfaces, UseCases
├── presentation/       # UI Layer:
│   ├── feature/        # Feature-based screens (Splash, Intro, Camera, History, Result, Settings)
│   ├── component/      # Reusable UI components
│   ├── navigation/     # NavHost & Screen definitions
│   └── theme/          # Material 3 Design System (Color, Type, Shape, Theme)
└── OCRApplication.kt   # Hilt Application class
```

---

## 🚀 Hướng dẫn cài đặt

1. **Yêu cầu:** Android Studio Ladybug (2024.2.1) trở lên.
2. **Clone dự án:**
   ```bash
   git clone https://github.com/yourusername/ocr-pro.git
   ```
3. **Mở dự án:** Chọn thư mục `Fe Mobile OCR` trong Android Studio.
4. **Cấu hình API (Tùy chọn):** 
   Cập nhật `BASE_URL` trong `Constants.kt` nếu bạn muốn sử dụng tính năng Xuất Word với Server riêng.
5. **Build & Run:** Sử dụng thiết bị Android thực (API 24+) để có trải nghiệm Camera tốt nhất.

---

## 📸 Ảnh chụp màn hình

| 🚀 Splash & Intro | 📸 Camera Scan | 📄 OCR Result | ⚙️ Settings |
| :---: | :---: | :---: | :---: |
| ![Splash](https://via.placeholder.com/150x300) | ![Camera](https://via.placeholder.com/150x300) | ![Result](https://via.placeholder.com/150x300) | ![Settings](https://via.placeholder.com/150x300) |

---

## 💡 Tư duy phát triển (Key Takeaways)
- Áp dụng **SOLID principles** vào việc thiết kế Repository và UseCase.
- Xử lý **Memory Management** khi làm việc với Bitmap (Recycle đúng lúc để tránh OOM).
- Tối ưu **UI UX** với các hiệu ứng AnimatedVisibility và Shimmer Loading.
- Quản lý **State** chặt chẽ bằng `StateFlow` và `collectAsStateWithLifecycle`.

---

**Phát triển bởi:** [Tên của bạn]
**Vị trí:** Android Intern Candidate
**Email:** [Email của bạn]
**LinkedIn:** [Link của bạn]
