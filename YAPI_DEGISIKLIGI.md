# 🔄 Oyun Yapısı Değişikliği

## 📊 Yeni Hiyerarşi

### ÖNCE (Yanlış)
```
Ana Sayfa
├─ Kart 1: Saat Okuma (Level 1 - Tam Saatler)
├─ Kart 2: Yarım Saatler
└─ Kart 3: Dakika Ustası
```

### SONRA (Doğru) ✅
```
Ana Sayfa (Oyunlar)
├─ 🎮 Saat Okuma (yüklü)
│  ├─ Level 1: Tam Saatler ✅ (Açık)
│  ├─ Level 2: Yarım Saatler 🔒 (70 puan gerekli)
│  └─ Level 3: Çeyrek Saatler 🔒 (140 puan gerekli)
│
├─ ➕ Toplama/Çıkarma (indirilecek)
│  ├─ Level 1: Toplama
│  └─ Level 2: Çıkarma
│
└─ 🔤 Harf Öğrenme (indirilecek)
   ├─ Level 1: Sesli Harfler
   └─ Level 2: Sessiz Harfler
```

## 🎯 Kullanıcı Akışı

### Adım 1: Ana Sayfa
Kullanıcı 3 oyun kartı görür:
- ⏰ **Saat Okuma** (Yüklü - tıklanabilir)
- ➕ **Toplama/Çıkarma** (İndirilecek - tıklayınca indirir)
- 🔤 **Harf Öğrenme** (İndirilecek - tıklayınca indirir)

### Adım 2: Oyun Kartına Tıklama
Kullanıcı "Saat Okuma" kartına tıkladığında:
→ **Level Seçim Ekranı** açılır

### Adım 3: Level Seçim Ekranı
```
⏰ Saat Okuma

Seviyeler:

┌─────────────────────────────────┐
│ 🕐  Level 1                     │
│     Tam Saatler                 │
│     1:00, 2:00, 3:00...         │
└─────────────────────────────────┘
         ↓ (tıklanabilir)

┌─────────────────────────────────┐
│ 🕜  Level 2  🔒                 │
│     Yarım Saatler               │
│     1:30, 2:30, 3:30...         │
│     🏆 70 puan gerekiyor        │
└─────────────────────────────────┘
         ↓ (kilitli)

┌─────────────────────────────────┐
│ 🕒  Level 3  🔒                 │
│     Çeyrek Saatler              │
│     1:15, 1:45...               │
│     🏆 140 puan gerekiyor       │
└─────────────────────────────────┘
         ↓ (kilitli)
```

### Adım 4: Level'e Tıklama
Kullanıcı "Level 1: Tam Saatler"e tıkladığında:
→ **Oyun Ekranı** açılır (mevcut saat oyunu)

## 📁 Değiştirilen Dosyalar

### 1. ✅ `data/GameModule.kt`
**Değişiklik**: `GameLevel` data class'ı eklendi
```kotlin
data class GameModule(
    // ... mevcut alanlar
    val levels: List<GameLevel> = emptyList() // YENİ
)

data class GameLevel(  // YENİ CLASS
    val id: String,
    val levelNumber: Int,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val requiredScore: Int = 0
)
```

### 2. ✅ `data/GameRepository.kt`
**Değişiklik**: Her oyunun level'ları tanımlandı
```kotlin
GameModule(
    id = "clock_reading",
    title = "Saat Okuma",
    levels = listOf(
        GameLevel(
            levelNumber = 1,
            title = "Tam Saatler",
            isUnlocked = true  // İlk level her zaman açık
        ),
        GameLevel(
            levelNumber = 2,
            title = "Yarım Saatler",
            isUnlocked = false,
            requiredScore = 70  // Level 1'den 70 puan gerekli
        ),
        // ...
    )
)
```

### 3. ✅ `ui/screens/LevelSelectionScreen.kt` (YENİ)
**Amaç**: Level seçim ekranını gösterir
```kotlin
@Composable
fun LevelSelectionScreen(
    gameModule: GameModule,
    onBackClick: () -> Unit,
    onLevelClick: (GameLevel) -> Unit
)
```

**Özellikler**:
- Oyun başlığı ve ikonu gösterir
- Tüm level'ları listeler
- Kilitli level'ları gösterir (🔒 ikonu)
- Gerekli puanı gösterir
- Sadece açık level'lara tıklanabilir

### 4. ✅ `viewmodel/MainViewModel.kt`
**Değişiklik**: Navigasyon yapısı güncellendi
```kotlin
sealed class NavigationDestination {
    object MainMenu
    data class LevelSelection(val module: GameModule)  // YENİ
    data class Game(val moduleId: String, val levelId: String)  // levelId eklendi
}

fun navigateToModule(moduleId: String)  // Yeni
fun navigateToLevel(moduleId: String, levelId: String)  // Yeni
fun navigateBack()  // Yeni
```

### 5. ✅ `MainActivity.kt`
**Değişiklik**: Level seçim ekranı rotası eklendi
```kotlin
when (destination) {
    MainMenu -> MainMenuScreen(...)
    LevelSelection -> LevelSelectionScreen(...)  // YENİ
    Game -> ClockGameScreen(...)
}
```

## 🎨 UI Değişiklikleri

### Ana Sayfa (MainMenuScreen)
**Değişiklik yok** - Aynı kalıyor, sadece tıklama davranışı değişti

### Level Seçim (YENİ Ekran)
**Tasarım**:
- Pembe-kırmızı gradyan arka plan (#F093FB → #F5576C)
- Geri butonu (sol üst)
- Oyun başlığı ve ikonu
- "Seviyeler" başlığı (bounce animasyonu)
- Level kartları (LazyColumn)

**Level Kartı**:
- Beyaz kart (açıksa), gri kart (kilitliyse)
- Sol: Level ikonu (daire içinde)
- Orta: Level bilgileri
  - "Level X" etiketi
  - Başlık
  - Açıklama
  - Gerekli puan (kilitliyse)
- Sağ: Kilit ikonu (kilitliyse)

### Oyun Ekranı (ClockGameScreen)
**Değişiklik yok** - Aynı kalıyor

## 🔄 Navigasyon Akışı

```
[Ana Sayfa]
     ↓ (Oyun kartına tıkla)
[Level Seçim]
     ↓ (Level'e tıkla)
[Oyun Ekranı]
     ↓ (Geri butonu)
[Level Seçim]
     ↓ (Geri butonu)
[Ana Sayfa]
```

## 🎮 Örnek Senaryolar

### Senaryo 1: İlk Kullanım
1. Kullanıcı uygulamayı açar → Ana sayfa
2. "Saat Okuma" kartına tıklar → Level seçim
3. "Level 1: Tam Saatler" açık ve tıklanabilir
4. "Level 2" ve "Level 3" kilitli
5. Level 1'e tıklar → Oyun başlar

### Senaryo 2: 70 Puan Kazandıktan Sonra
1. Kullanıcı Level 1'de 70 puan kazandı
2. Level seçim ekranına döner
3. "Level 2: Yarım Saatler" artık açık ✅
4. Level 2'ye tıklayabilir

### Senaryo 3: Yeni Oyun İndirme
1. Kullanıcı "Toplama/Çıkarma" kartına tıklar
2. İndirme başlar (progress bar)
3. İndirme tamamlanır
4. Level seçim ekranı açılır
5. "Level 1: Toplama" açık

## 📊 Veri Yapısı

```kotlin
GameModule {
    id = "clock_reading"
    title = "Saat Okuma"
    icon = "⏰"
    isInstalled = true
    levels = [
        GameLevel {
            id = "clock_full_hours"
            levelNumber = 1
            title = "Tam Saatler"
            isUnlocked = true
            requiredScore = 0
        },
        GameLevel {
            id = "clock_half_hours"
            levelNumber = 2
            title = "Yarım Saatler"
            isUnlocked = false
            requiredScore = 70
        },
        GameLevel {
            id = "clock_quarter_hours"
            levelNumber = 3
            title = "Çeyrek Saatler"
            isUnlocked = false
            requiredScore = 140
        }
    ]
}
```

## ✅ Test Senaryoları

### Test 1: Ana Sayfa
- [ ] 3 oyun kartı görünüyor
- [ ] "Saat Okuma" yüklü olarak işaretli
- [ ] Diğer oyunlar "indirilecek" olarak işaretli

### Test 2: Level Seçim
- [ ] "Saat Okuma" kartına tıklayınca level seçim açılıyor
- [ ] 3 level görünüyor
- [ ] Level 1 açık, Level 2-3 kilitli
- [ ] Kilitli level'larda "🏆 X puan gerekiyor" yazıyor
- [ ] Geri butonu ana sayfaya dönüyor

### Test 3: Oyun Başlatma
- [ ] Level 1'e tıklayınca oyun başlıyor
- [ ] Geri butonu level seçime dönüyor
- [ ] Oyun ekranı önceki gibi çalışıyor

## 🚀 Sonraki Adımlar

1. ✅ Yapı değişikliği tamamlandı
2. ⏳ Build ve test
3. ⏳ Level kilitleme mantığı (puan kazanınca açma)
4. ⏳ Level 2 ve 3 oyun mantığı (yarım saatler, çeyrek saatler)
5. ⏳ Diğer oyunlar (Toplama/Çıkarma, Harf Öğrenme)

---

**Artık doğru yapıda! Ana sayfa → Level seçim → Oyun** 🎉
