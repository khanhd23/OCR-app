# OCR Pro — Android App

Ứng dụng OCR chuyên nghiệp sử dụng ML Kit + Clean Architecture + Jetpack Compose.

---

## Kiến trúc

```
Clean Architecture (3 lớp)
  ├── domain/      → Use cases, models, interfaces (không phụ thuộc framework)
  ├── data/        → Repository impls, Room DB, ML Kit, Retrofit
  └── presentation → Compose UI, ViewModels, Navigation
```

## Luồng hoạt động

```
IntroScreen
  │
  ├─▶ CameraScreen
  │     ├─ Chụp ảnh (CameraX)
  │     ├─ Chọn từ thư viện (Gallery picker)
  │     ├─ Xem preview → OCR
  │     └─ MLKitProcessorImpl → nhận dạng dòng
  │
  ├─▶ ResultScreen
  │     ├─ View: Từng dòng / Toàn văn / Hình ảnh
  │     ├─ Sao chép clipboard
  │     ├─ Xuất .docx (Apache POI)
  │     └─ Chia sẻ qua FileProvider
  │
  └─▶ HistoryScreen
        ├─ Danh sách tài liệu (Room)
        ├─ Tìm kiếm real-time (debounce 300ms)
        └─ Xoá bằng swipe-to-dismiss
```

## Cấu trúc thư mục

```
com.yourapp.ocr/
├── OCRApplication.kt
├── MainActivity.kt
├── core/
│   ├── di/          NetworkModule, DatabaseModule, RepositoryModule, ProcessorModule
│   ├── network/     OCRApi, RetrofitFactory
│   ├── common/      Resource, Constants
│   └── extension/  BitmapExt, FileExt
├── domain/
│   ├── model/       OCRResult, OCRDocument
│   ├── repository/  OCRRepository, ImageProcessor (interfaces)
│   └── usecase/     OCRUseCase, ExportUseCase
├── data/
│   ├── remote/      OCRResponseDto, OCRMapper
│   ├── local/       AppDatabase (Room), MLKitProcessorImpl
│   └── repository/  OCRRepositoryImpl, ImageProcessorImpl
├── presentation/
│   ├── navigation/  Screen, OCRNavGraph
│   ├── theme/       Color, Type, Theme
│   ├── component/   LoadingView, ResultItem, CameraOverlay, Buttons
│   └── feature/
│       ├── intro/   IntroScreen
│       ├── camera/  CameraScreen + ViewModel
│       ├── result/  ResultScreen + ViewModel
│       └── history/ HistoryScreen + ViewModel
└── util/
    └── WordGenerator.kt
```

## Setup

### 1. Đổi package name
Tìm-thay tất cả `com.yourapp.ocr` → package name của bạn trong toàn bộ file.

### 2. Thêm dependencies
Xem file `build_dependencies_reference.kts` để copy dependencies vào `build.gradle.kts`.

### 3. Hilt setup (build.gradle.kts project level)
```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.51" apply false
}
```

### 4. FileProvider authority
Đảm bảo `android:authorities="${applicationId}.provider"` trong Manifest khớp với
`FileProvider.getUriForFile(context, "${context.packageName}.provider", file)` trong ResultScreen.

### 5. Server FastAPI (tuỳ chọn)
Nếu muốn dùng server OCR thay vì ML Kit:
- Chạy FastAPI server tại `http://localhost:8000`
- Endpoint: `POST /api/ocr` nhận `multipart/form-data` với field `file`
- Trả về `OCRResponseDto` format
- Emulator kết nối qua `10.0.2.2:8000`

### 6. ML Kit (mặc định)
App mặc định dùng **ML Kit on-device** — không cần server, hoạt động offline.
`ProcessImageUseCase` → `ImageProcessorImpl` → `MLKitProcessorImpl`

---

## Design System

| Token | Value | Dùng cho |
|---|---|---|
| `Ink900` | `#0A0E1A` | Background chính |
| `Teal400` | `#2DD4C8` | Primary accent |
| `Amber400` | `#FBBF24` | Secondary / Export |
| `SurfaceCard` | `#161E2E` | Card backgrounds |
| `TextPrimary` | `#E2E8F0` | Nội dung chính |

---

## Ghi chú

- Apache POI có thể gây tăng APK size ~8-10MB. Nếu muốn nhẹ hơn, thay bằng
  thư viện `docx4j` cho Android hoặc xuất plain `.txt`.
- `MLKitProcessorImpl.kt` hiện implement `Text` interface với empty fallback —
  cần kiểm tra lại khi update ML Kit version.
- Swipe-to-dismiss trong HistoryScreen dùng `ExperimentalMaterial3Api`.
