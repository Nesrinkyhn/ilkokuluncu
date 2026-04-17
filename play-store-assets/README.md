# 🎨 İlkokuluncu - Icon ve Grafikler

## 📱 Uygulama İkonu

### Adaptive Icon (Android 8.0+)
✅ **Oluşturuldu ve projeye eklendi**

**Dosyalar:**
- `app/src/main/res/drawable/ic_launcher_background.xml` - Arka plan (gradyan)
- `app/src/main/res/drawable/ic_launcher_foreground.xml` - Ön plan (saat + kalem)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` - Adaptive icon tanımı
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` - Yuvarlak adaptive icon

**Tasarım Özellikleri:**
- 🎨 **Arka Plan**: Mor-mavi gradyan (#667eea → #764ba2)
- ⏰ **Ana Öğe**: Sarı analog saat (3:00'ı gösteriyor)
- ✏️ **İkincil Öğe**: Kırmızı kalem (eğitim vurgusu)
- ⭐ **Dekorasyon**: Altın sarısı yıldızlar
- 🎯 **Mesaj**: Eğlenceli + Eğitici

### Icon Boyutları (Otomatik Üretilir)
Android Studio ile otomatik oluşturulabilir:
```
Tools → Image Asset → Icon Type: Launcher Icons (Adaptive and Legacy)
```

**Veya manuel:**
- mdpi: 48x48 px
- hdpi: 72x72 px
- xhdpi: 96x96 px
- xxhdpi: 144x144 px
- xxxhdpi: 192x192 px

## 🏪 Play Store Grafikleri

### 1. Play Store Icon (512x512)
📁 **Dosya**: `play-store-assets/ic_launcher_512.svg`

**Özellikler:**
- Boyut: 512x512 px
- Format: SVG (PNG'ye dönüştürülmeli)
- Tasarım: Adaptive icon ile aynı ama daha detaylı
- Ekstra: "İlkokuluncu" yazısı altta

**PNG'ye dönüştürme:**
```bash
# İnternet üzerinden: https://cloudconvert.com/svg-to-png
# Veya Inkscape ile:
inkscape ic_launcher_512.svg --export-png=ic_launcher_512.png --export-width=512
```

### 2. Feature Graphic (1024x500)
📁 **Dosya**: `play-store-assets/feature_graphic_1024x500.svg`

**Özellikler:**
- Boyut: 1024x500 px (Play Store banner)
- Format: SVG (PNG/JPG'ye dönüştürülmeli)
- Tasarım:
  - Sol: Büyük analog saat
  - Orta: "İlkokuluncu" başlık
  - Alt başlık: "Eğlenceli Saat Öğrenme Oyunu"
  - Özellikler: ⭐ Renkli Saatler | ❤️ Sevimli Hayvanlar | 🏆 Level Sistemi
  - Sağ: 4 hayvan karakteri (🐱🐶🐰🐻)
  - Alt: 5-8 YAŞ | EĞİTİM | ÜCRETSİZ

**JPG'ye dönüştürme:**
```bash
# Kalite 100 ile:
inkscape feature_graphic_1024x500.svg --export-png=feature_graphic.png --export-width=1024
# PNG'den JPG'ye:
convert feature_graphic.png -quality 100 feature_graphic.jpg
```

## 📸 Ekran Görüntüleri (Gerekli)

### Telefon Ekran Görüntüleri
**Gerekli:** En az 2 adet
**Önerilen:** 4-8 adet
**Boyut:** 1080x1920 px (veya daha yüksek)
**Format:** PNG veya JPG

**Önerilen Ekranlar:**
1. **Ana Menü** - 3 oyun kartı göster
2. **Saat Oyunu** - Hayvan karakteri + saat + seçenekler
3. **Doğru Cevap** - Konfeti animasyonu
4. **Test Ekranı** - "Level 2'ye Geçiş Testi"
5. **Sonuç Ekranı** - "HARIKA! TEST GEÇTİN!"

### Tablet Ekran Görüntüleri (Opsiyonel)
**Boyut:** 1920x1080 px veya 2048x1536 px
**Format:** PNG veya JPG

## 🎬 Tanıtım Videosu (Opsiyonel ama Önerilir)

**Süre:** 30 saniye - 2 dakika
**Format:** MP4, MOV, veya AVI
**Maksimum Boyut:** 100 MB
**Önerilen İçerik:**
1. Uygulama açılışı (0-3 sn)
2. Ana menü gösterimi (3-8 sn)
3. Saat okuma oyunu (8-20 sn)
4. Farklı hayvan karakterleri (20-25 sn)
5. Test ve başarı (25-30 sn)

## 🛠️ Icon Üretme Rehberi

### Android Studio ile (Önerilen)
1. **Right-click** `app/res` klasörüne
2. **New → Image Asset**
3. **Icon Type:** Launcher Icons (Adaptive and Legacy)
4. **Foreground Layer:** `drawable/ic_launcher_foreground.xml` seç
5. **Background Layer:** `drawable/ic_launcher_background.xml` seç
6. **Next → Finish**

### Online Araçlar
- **App Icon Generator**: https://appicon.co/
- **Android Asset Studio**: https://romannurik.github.io/AndroidAssetStudio/
- **Figma/Canva**: Manuel tasarım

## 📋 Play Store Yükleme Checklist

### Gerekli Grafikler
- [ ] Uygulama ikonu (512x512 PNG)
- [ ] Feature graphic (1024x500 PNG/JPG)
- [ ] En az 2 telefon ekran görüntüsü (1080x1920)
- [ ] (Opsiyonel) Tablet ekran görüntüleri
- [ ] (Opsiyonel) Tanıtım videosu

### Dosya Formatları
- **Icon:** PNG, 32-bit, alpha channel
- **Feature Graphic:** JPG veya PNG
- **Ekran Görüntüleri:** PNG veya JPG (tercihen PNG)

### Kalite Kontrol
- [ ] İkon net ve okunabilir mi?
- [ ] Feature graphic'te yazılar okunaklı mı?
- [ ] Ekran görüntüleri güncel mi?
- [ ] Renkler canlı ve çekici mi?
- [ ] Çocuklara uygun mu?

## 🎨 Renk Paleti (Referans)

```
Mor-Mavi Gradyan:
#667eea → #764ba2

Pembe Gradyan:
#F093FB → #F5576C

Sarı (Saat):
#FFF8DC → #FFE082

Buton Renkleri:
#FF6B6B (Kırmızı)
#4ECDC4 (Turkuaz)
#FFA07A (Turuncu)

Aksan Renkleri:
#FFD93D (Altın Sarısı - Yıldızlar)
#E74C3C (Kırmızı - Akrep)
#3498DB (Mavi - Yelkovan)
#2ECC71 (Yeşil - Başarı)
```

## 📞 Yardım

### SVG'yi PNG'ye Çevirme
**Online:**
- https://cloudconvert.com/svg-to-png
- https://svgtopng.com/

**Yazılım:**
- Inkscape (Ücretsiz)
- Adobe Illustrator
- Figma (Online)

### Ekran Görüntüsü Alma
**Android Studio Emülatör:**
1. Emülatörü çalıştır
2. Uygulama açık iken: Camera ikonu (sağ panelde)
3. Görüntü otomatik kaydedilir

**Fiziksel Cihaz:**
1. USB Debugging aktif
2. `adb shell screencap -p /sdcard/screenshot.png`
3. `adb pull /sdcard/screenshot.png`

---

**Not:** Tüm grafikler SVG formatında oluşturuldu. PNG/JPG'ye dönüştürmek için yukarıdaki araçları kullanın.

**Tasarım Prensibi:** Renkli, eğlenceli, çocuk dostu, profesyonel görünüm!
