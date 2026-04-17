# 🚀 İlkokuluncu - Kurulum Kılavuzu

## 📋 Gereksinimler

### Sistem Gereksinimleri
- **İşletim Sistemi**: Windows 10/11, macOS 10.14+, veya Linux
- **RAM**: En az 8 GB (16 GB önerilir)
- **Disk Alanı**: En az 10 GB boş alan
- **İnternet**: İlk kurulum için gerekli

### Yazılım Gereksinimleri
- **Android Studio**: Arctic Fox (2020.3.1) veya daha yeni
- **JDK**: 8, 11 veya 17 (Android Studio ile birlikte gelir)
- **Android SDK**: API 28-34 arası

## 📥 1. Android Studio Kurulumu

### Windows
1. https://developer.android.com/studio adresine gidin
2. "Download Android Studio" butonuna tıklayın
3. İndirilen `.exe` dosyasını çalıştırın
4. Kurulum sihirbazını takip edin
5. "Standard" kurulum tipini seçin

### macOS
1. https://developer.android.com/studio adresine gidin
2. "Download Android Studio" butonuna tıklayın
3. İndirilen `.dmg` dosyasını açın
4. Android Studio'yu Applications klasörüne sürükleyin
5. Uygulamayı açın ve kurulum sihirbazını tamamlayın

### Linux (Ubuntu/Debian)
```bash
sudo snap install android-studio --classic
# veya
sudo apt install android-studio
```

## 📂 2. Projeyi Açma

### Adım 1: Projeyi İndirin
```bash
# ZIP dosyası indirdiyseniz, çıkartın
unzip Ilkokuluncu.zip
cd Ilkokuluncu

# veya Git kullanarak
git clone [repository-url]
cd Ilkokuluncu
```

### Adım 2: Android Studio'da Açın
1. Android Studio'yu başlatın
2. "Open" veya "Open an Existing Project" seçin
3. `Ilkokuluncu` klasörünü seçin ve "OK" yapın
4. Gradle sync işlemini bekleyin (5-10 dakika sürebilir)

## ⚙️ 3. SDK Kurulumu

### SDK Manager'ı Açma
1. Android Studio menüsünden: **Tools → SDK Manager**
2. Veya: Üst araç çubuğunda SDK Manager ikonuna tıklayın

### Gerekli SDK'ları Yükleyin
**SDK Platforms** sekmesinde:
- ✅ Android 14.0 (API 34) - **Önerilir**
- ✅ Android 9.0 (API 28) - **Minimum**

**SDK Tools** sekmesinde:
- ✅ Android SDK Build-Tools 34.0.0
- ✅ Android Emulator
- ✅ Android SDK Platform-Tools
- ✅ Google Play services

"Apply" butonuna tıklayın ve kurulumu bekleyin.

## 📱 4. Emülatör Kurulumu (Test için)

### Yeni Sanal Cihaz Oluşturma
1. **Tools → Device Manager**
2. "Create Device" butonuna tıklayın
3. Bir cihaz seçin (örn: Pixel 5)
4. Sistem imajı seçin:
   - **Önerilen**: API 34 (Android 14.0)
   - **Minimum**: API 28 (Android 9.0)
5. "Download" yapın (ilk seferde gerekli)
6. "Next" ve "Finish"

### Emülatörü Başlatma
1. Device Manager'da cihazın yanındaki ▶️ butonuna tıklayın
2. Emülatörün açılmasını bekleyin (ilk açılış 2-3 dakika sürebilir)

## 🔧 5. Projeyi Çalıştırma

### Gradle Sync (İlk Kez)
1. Proje açıldığında otomatik olarak başlar
2. Veya manuel: **File → Sync Project with Gradle Files**
3. Hataları düzeltin (genellikle SDK yolu ile ilgili)

### Uygulamayı Çalıştırma

#### Emülatörde
1. Emülatörü başlatın
2. Üst araç çubuğunda hedef cihazı seçin
3. **Run** (▶️) butonuna tıklayın veya `Shift + F10`
4. Uygulamanın açılmasını bekleyin

#### Fiziksel Cihazda
1. **Telefonunuzda USB Debugging açın**:
   - Ayarlar → Telefon Hakkında → Yapı Numarasına 7 kez tıklayın
   - Ayarlar → Geliştirici Seçenekleri → USB Hata Ayıklama ✅
   
2. USB ile bilgisayara bağlayın
3. "USB debugging" iznini verin
4. Android Studio'da cihazınız görünecek
5. **Run** butonuna tıklayın

## 🏗️ 6. APK Oluşturma

### Debug APK (Test için)
```bash
# Terminal veya CMD'de
cd Ilkokuluncu
./gradlew assembleDebug

# Windows'ta
gradlew.bat assembleDebug
```

APK yolu: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (Yayın için)

#### 1. Keystore Oluşturma
```bash
keytool -genkey -v -keystore ilkokuluncu.keystore \
  -alias ilkokuluncu-key \
  -keyalg RSA -keysize 2048 -validity 10000
```

Sorulara cevap verin:
- Şifre: Güçlü bir şifre girin (unutmayın!)
- Ad, organizasyon, şehir, ülke bilgilerini doldurun

#### 2. Signing Config
`app/build.gradle.kts` dosyasını açın ve ekleyin:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../ilkokuluncu.keystore")
            storePassword = "SİFRENİZ"
            keyAlias = "ilkokuluncu-key"
            keyPassword = "SİFRENİZ"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... mevcut ayarlar
        }
    }
}
```

#### 3. Release APK Oluştur
```bash
./gradlew assembleRelease
```

APK yolu: `app/build/outputs/apk/release/app-release.apk`

## 🐛 Yaygın Sorunlar ve Çözümler

### Gradle Sync Hatası
```
Could not find com.android.tools.build:gradle:8.2.0
```
**Çözüm**: İnternet bağlantınızı kontrol edin ve tekrar sync yapın.

### SDK Bulunamadı
```
SDK location not found
```
**Çözüm**: 
1. `local.properties` dosyası oluşturun
2. İçine ekleyin:
```properties
sdk.dir=/Users/KULLANICI_ADINIZ/Library/Android/sdk
# Windows: C\:\\Users\\KULLANICI_ADINIZ\\AppData\\Local\\Android\\Sdk
# Linux: /home/KULLANICI_ADINIZ/Android/Sdk
```

### Emülatör Açılmıyor
**Çözüm**:
1. BIOS'ta virtualization (VT-x/AMD-V) açık olmalı
2. Hyper-V kapalı olmalı (Windows)
3. Device Manager'dan farklı bir sistem imajı deneyin

### Build Hatası
```
Unsupported Java version
```
**Çözüm**: JDK 17 kullanın
```bash
# Kontrol et
java -version

# Android Studio'da ayarla
File → Settings → Build, Execution, Deployment → Build Tools → Gradle
→ Gradle JDK: Use JDK 17
```

## 📝 Notlar

### Proje Yapısı
```
Ilkokuluncu/
├── app/                    # Ana uygulama modülü
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Kotlin kaynak kodları
│   │       ├── res/        # Kaynaklar (XML, drawable)
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts    # App seviye build config
├── gradle/                 # Gradle wrapper
├── build.gradle.kts        # Proje seviye build config
├── settings.gradle.kts     # Proje ayarları
└── README.md
```

### Performans İpuçları
- **Gradle Daemon**: Otomatik çalışır, build süresini kısaltır
- **Parallel Build**: `gradle.properties` dosyasında aktif
- **Build Cache**: İlk build 5-10 dk, sonrakiler 30-60 sn

### İlk Build
İlk build uzun sürebilir çünkü:
- Gradle dependencies indirilir (~500 MB)
- Android SDK componentleri indirilir
- Indexing yapılır

## 🆘 Yardım

### Dokümantasyon
- Android Geliştirici: https://developer.android.com
- Kotlin: https://kotlinlang.org/docs
- Jetpack Compose: https://developer.android.com/jetpack/compose

### Topluluk
- Stack Overflow: Tag `android` `kotlin` `jetpack-compose`
- Reddit: r/androiddev
- Discord: Android Dev Turkey

## ✅ Başarılı Kurulum Kontrolü

Kurulum başarılıysa:
1. ✅ Proje hatasız açıldı
2. ✅ Gradle sync tamamlandı
3. ✅ Emülatör veya cihazda çalıştı
4. ✅ Ana menü ekranı göründü
5. ✅ Saat okuma oyunu açıldı

**Tebrikler! 🎉 Artık geliştirmeye başlayabilirsiniz!**

---

**Son Güncelleme**: 2024
