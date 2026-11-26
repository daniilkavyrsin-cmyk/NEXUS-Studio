package com.nexusstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.content.Intent
import android.provider.OpenableColumns
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexusStudio() }
    }
}

private data class Sample(val glyph: String, val name: String)

@Composable
fun NexusStudio() {
    val palettes = listOf(
        "Лаванда" to Color(0xFF76539B), "Лёд" to Color(0xFF438CCB),
        "Мята" to Color(0xFF368C79), "Золото" to Color(0xFFB77927),
        "Роза" to Color(0xFFB65D79), "Графит" to Color(0xFF50515B)
    )
    var accent by remember { mutableStateOf(palettes[0].second) }
    var style by remember { mutableStateOf("Material You") }
    var shape by remember { mutableStateOf("Скруглённый") }
    var scale by remember { mutableFloatStateOf(1f) }
    var tab by remember { mutableIntStateOf(0) }
    var hex by remember { mutableStateOf("#76539B") }
    var saved by remember { mutableStateOf(false) }
    var tileHex by remember { mutableStateOf("#FFF6E8") }
    var glassOpacity by remember { mutableFloatStateOf(0.40f) }
    val customIcons = remember { mutableStateMapOf<String, String>() }
    var iconTarget by remember { mutableStateOf("Почта") }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nexus_prefs", android.content.Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        prefs.getString("style", null)?.let { style = it }
        prefs.getString("shape", null)?.let { shape = it }
        prefs.getString("accent", null)?.let { runCatching { accent = Color(android.graphics.Color.parseColor(it)) } }
        prefs.getString("tile", null)?.let { tileHex = it }
        prefs.getFloat("scale", 1f).let { scale = it }
        prefs.getFloat("glass", 0.40f).let { glassOpacity = it }
        prefs.all.filterKeys { it.startsWith("icon_") }.forEach { (key, value) ->
            if (value is String) customIcons[key.removePrefix("icon_")] = value
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            customIcons[iconTarget] = uri.toString()
            prefs.edit().putString("icon_$iconTarget", uri.toString()).apply()
        }
    }
    fun persistSettings() {
        prefs.edit()
            .putString("style", style)
            .putString("shape", shape)
            .putString("accent", String.format("#%08X", accent.value.toLong()))
            .putString("tile", tileHex)
            .putFloat("scale", scale)
            .putFloat("glass", glassOpacity)
            .apply()
    }

    val dark = style == "AMOLED"
    val bg = if (dark) Color(0xFF111116) else Color(0xFFF4F0F8)
    val card = if (dark) Color(0xFF202027) else Color.White
    val ink = if (dark) Color(0xFFF4F1F8) else Color(0xFF28242E)
    val muted = if (dark) Color(0xFFB9B3C2) else Color(0xFF77717E)

    MaterialTheme(colorScheme = lightColorScheme(primary = accent, background = bg, surface = card)) {
        Scaffold(containerColor = bg) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("NEXUS", color = ink, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("STUDIO · МАСТЕРСКАЯ КАСТОМИЗАЦИИ", color = muted, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                    IconButton(onClick = { saved = true }) { Icon(Icons.Default.BookmarkBorder, null, tint = ink) }
                    IconButton(onClick = { style = "Material You"; accent = palettes[0].second; shape = "Скруглённый"; scale = 1f; tileHex = "#FFF6E8"; glassOpacity = .40f; persistSettings() }) {
                        Icon(Icons.Default.Refresh, null, tint = ink)
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Студия", "Каталог", "Приложения", "Система").forEachIndexed { i, label ->
                        FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
                    }
                }
                when (tab) {
                    0 -> StudioEditor(accent, { accent = it }, palettes, style, { style = it }, shape, { shape = it }, scale, { scale = it }, hex, { hex = it }, tileHex, { tileHex = it }, glassOpacity, { glassOpacity = it }, customIcons, { name -> iconTarget = name; imagePicker.launch(arrayOf("image/*")) }, bg, card, ink, muted, { persistSettings(); saved = true })
                    1 -> Catalog(accent, card, ink, muted)
                    2 -> InstalledApps(accent, card, ink, muted, customIcons, { name -> iconTarget = name; imagePicker.launch(arrayOf("image/*")) }, { persistSettings(); saved = true })
                    else -> SystemInfo(card, ink, muted)
                }
            }
        }
        if (saved) AlertDialog(
            onDismissRequest = { saved = false },
            title = { Text("Настройки сохранены") },
            text = { Text("Параметры оформления и выбранные изображения сохранены на устройстве.") },
            confirmButton = { TextButton(onClick = { saved = false }) { Text("Понятно") } }
        )
    }
}

@Composable
private fun StudioEditor(
    accent: Color, setAccent: (Color) -> Unit, palettes: List<Pair<String, Color>>,
    style: String, setStyle: (String) -> Unit, shape: String, setShape: (String) -> Unit,
    scale: Float, setScale: (Float) -> Unit, hex: String, setHex: (String) -> Unit,
    tileHex: String, setTileHex: (String) -> Unit, glassOpacity: Float, setGlassOpacity: (Float) -> Unit,
    customIcons: Map<String, String>, importIcon: (String) -> Unit,
    bg: Color, card: Color, ink: Color, muted: Color, save: () -> Unit
) {
    val samples = listOf(
        Sample("✉", "Почта"), Sample("☎", "Телефон"), Sample("♫", "Музыка"), Sample("➤", "Telegram"),
        Sample("◎", "Камера"), Sample("◉", "Браузер"), Sample("▣", "Файлы"), Sample("☼", "Погода"),
        Sample("⚙", "Настройки"), Sample("▤", "Календарь"), Sample("♥", "Здоровье"), Sample("▶", "Видео")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Предпросмотр рабочего стола", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(accent, accent.copy(.55f), Color(0xFF252238)))).padding(18.dp)) {
            Column {
                Text("14:23", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
                Text("Среда, 26 ноября", color = Color.White.copy(.8f), fontSize = 13.sp)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) { items(8) { IconSample(samples[it], accent, style, shape, scale, tileHex, glassOpacity, true, customIcons[samples[it].name]) } }
        }
        Spacer(Modifier.height(20.dp))
        Heading("Стиль иконок", ink)
        listOf("Material You", "Liquid Glass", "AMOLED", "Монохром", "Оригинал").forEach { item ->
            Row(Modifier.fillMaxWidth().padding(bottom = 7.dp).clip(RoundedCornerShape(16.dp))
                .background(if (style == item) accent.copy(.15f) else card)
                .clickable { setStyle(item) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(.2f)), contentAlignment = Alignment.Center) {
                    Text(item.take(1), color = accent, fontWeight = FontWeight.Bold)
                }
                Text(item, Modifier.weight(1f).padding(start = 12.dp), color = ink)
                if (style == item) Icon(Icons.Default.CheckCircle, null, tint = accent)
            }
        }
        Spacer(Modifier.height(14.dp))
        Heading("Цвет иконок", ink)
        Text("Выбери палитру или введи свой HEX-код", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            palettes.forEach { (name, color) ->
                Column(Modifier.weight(1f).clickable { setAccent(color) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                        if (accent == color) Icon(Icons.Default.Check, null, tint = Color.White)
                    }
                    Text(name, color = muted, fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = hex, onValueChange = setHex, label = { Text("Цвет символа · HEX") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { TextButton(onClick = { runCatching { setAccent(Color(android.graphics.Color.parseColor(hex))) } }) { Text("Применить") } })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = tileHex, onValueChange = setTileHex, label = { Text("Цвет фона иконки · HEX") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Для обычных стилей фон применится к плитке; для Liquid Glass задаёт оттенок стекла.", color = muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Text("Прозрачность Liquid Glass: ${(glassOpacity * 100).roundToInt()}%", color = ink)
        Slider(value = glassOpacity, onValueChange = setGlassOpacity, valueRange = .08f..0.85f)
        Spacer(Modifier.height(16.dp))
        Heading("Форма иконок", ink)
        val shapes = listOf("Круг", "Скруглённый", "Квадрат", "Капля", "Шестиугольник")
        shapes.chunked(2).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                line.forEach { s ->
                    FilterChip(selected = shape == s, onClick = { setShape(s) }, label = { Text(s) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Размер: ${(scale * 100).roundToInt()}%", color = ink)
        Slider(value = scale, onValueChange = setScale, valueRange = .8f..1.35f)
        Spacer(Modifier.height(14.dp))
        Heading("Редактор отдельных иконок", ink)
        Text("Нажми на иконку, чтобы выбрать собственное изображение из галереи. Изменения сохраняются на устройстве.", color = muted, fontSize = 12.sp)
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(290.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp), userScrollEnabled = false) {
            items(samples.size) { index ->
                Column(Modifier.clickable { importIcon(samples[index].name) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    IconSample(samples[index], accent, style, shape, scale, tileHex, glassOpacity, false, customIcons[samples[index].name])
                    Text(if (customIcons.containsKey(samples[index].name)) "Изменить" else "Загрузить", color = accent, fontSize = 9.sp)
                }
            }
        }
        Button(onClick = save, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Сохранить стиль")
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun IconSample(sample: Sample, accent: Color, style: String, shape: String, scale: Float, tileHex: String, glassOpacity: Float, preview: Boolean, customUri: String? = null) {
    val isGlass = style == "Liquid Glass"
    val customTile = runCatching { Color(android.graphics.Color.parseColor(tileHex)) }.getOrDefault(Color(0xFFFFF6E8))
    val base = when (style) {
        "AMOLED" -> Color(0xFF08080C)
        "Монохром" -> Color(0xFF37343D)
        else -> customTile
    }
    val fg = when (style) {
        "AMOLED" -> accent
        "Монохром" -> Color.White
        "Liquid Glass" -> Color.White
        else -> accent
    }
    val corner = when (shape) { "Круг" -> 100; "Квадрат" -> 8; "Капля" -> 35; "Шестиугольник" -> 22; else -> 28 }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size((48 * scale).dp).clip(RoundedCornerShape(percent = corner))
            .background(if (isGlass) Brush.linearGradient(listOf(Color.White.copy(glassOpacity), accent.copy(glassOpacity * .55f), base.copy(.55f))) else Brush.linearGradient(listOf(base, base.copy(.9f))))
            .then(if (isGlass) Modifier.border(1.dp, Color.White.copy((glassOpacity + .2f).coerceAtMost(.95f)), RoundedCornerShape(percent = corner)) else Modifier),
            contentAlignment = Alignment.Center) {
            val context = LocalContext.current
            val bitmap = remember(customUri) {
                customUri?.let { uri -> runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull() }
            }
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = sample.name, modifier = Modifier.fillMaxSize().padding(5.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                Text(sample.glyph, color = fg, fontSize = (24 * scale).sp, fontWeight = FontWeight.Bold)
            }
        }
        if (!preview) Text(sample.name, color = Color(0xFF827B89), fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun Catalog(accent: Color, card: Color, ink: Color, muted: Color) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Heading("Каталог оформления", ink)
        Text("Демонстрационные стили для дальнейшего редактирования", color = muted)
        Spacer(Modifier.height(12.dp))
        listOf("Material You", "Liquid Glass", "AMOLED Gold", "Pastel", "Monochrome", "Neon").forEachIndexed { i, name ->
            Row(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(18.dp)).background(card).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(listOf(accent, Color(0xFF9BC8E8), Color(0xFFB18A4B), Color(0xFFE7C9EE), Color.DarkGray, Color(0xFF3BC6B0))[i]), contentAlignment = Alignment.Center) {
                    Text("✦", color = Color.White, fontSize = 23.sp)
                }
                Text(name, Modifier.weight(1f).padding(start = 12.dp), color = ink, fontWeight = FontWeight.SemiBold)
                Text("Открыть", color = accent, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SystemInfo(card: Color, ink: Color, muted: Color) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Heading("Система и экспорт", ink)
        listOf(
            "Устройство" to "Redmi Note 14S · HyperOS 3",
            "Версия приложения" to "NEXUS Studio 0.6.0",
            "Импорт своих иконок" to "Доступен для демонстрационного набора",
            "Каталог приложений" to "Добавлено обнаружение приложений с ярлыком запуска",
            "Экспорт набора иконок" to "ZIP с PNG 512×512 и manifest.json",
            "Экспорт темы MTZ" to "Пока не реализован: требуется адаптация под формат тем MIUI/HyperOS",
            "Панель управления" to "Нужен отдельный системный модуль"
        ).forEach { (title, detail) ->
            Column(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(18.dp)).background(card).padding(15.dp)) {
                Text(title, color = ink, fontWeight = FontWeight.SemiBold)
                Text(detail, color = muted, fontSize = 12.sp)
            }
        }
        Text("Прямое применение защищённых системных компонентов HyperOS без root не гарантируется. Этот прототип пока редактирует и показывает параметры оформления внутри приложения.", color = muted, fontSize = 12.sp)
    }
}


@Composable
private fun InstalledApps(accent: Color, card: Color, ink: Color, muted: Color, customIcons: Map<String, String>, chooseIcon: (String) -> Unit, save: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf(false) }
    val apps = remember {
        runCatching {
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
                .map { it.loadLabel(context.packageManager).toString() to it.activityInfo.packageName }
                .distinctBy { it.second }
                .sortedBy { it.first.lowercase() }
        }.getOrDefault(emptyList())
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    ZipOutputStream(output).use { zip ->
                        val records = customIcons.entries.mapNotNull { (packageName, imageUri) ->
                            val bitmap = runCatching {
                                context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { BitmapFactory.decodeStream(it) }
                            }.getOrNull()
                            if (bitmap != null) {
                                val edge = minOf(bitmap.width, bitmap.height)
                                val left = (bitmap.width - edge) / 2
                                val top = (bitmap.height - edge) / 2
                                val square = Bitmap.createBitmap(bitmap, left, top, edge, edge)
                                val normalized = Bitmap.createScaledBitmap(square, 512, 512, true)
                                val bytes = java.io.ByteArrayOutputStream().use { stream ->
                                    normalized.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    stream.toByteArray()
                                }
                                if (square !== bitmap) square.recycle()
                                if (normalized !== square) normalized.recycle()
                                if (bitmap.isRecycled.not()) bitmap.recycle()
                                val safe = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                                zip.putNextEntry(ZipEntry("icons/$safe.png")); zip.write(bytes); zip.closeEntry()
                                "\"$packageName\": \"icons/$safe.png\""
                            } else null
                        }
                        zip.putNextEntry(ZipEntry("manifest.json"))
                        zip.write(("{\n  \"format\": \"NEXUS_ICON_SET_1\",\n  \"icons\": {\n    " + records.joinToString(",\n    ") + "\n  }\n}").toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                } ?: error("Не удалось открыть файл")
                exportMessage = true
            }.onFailure { android.widget.Toast.makeText(context, "Ошибка экспорта: ${it.message}", android.widget.Toast.LENGTH_LONG).show() }
        }
    }
    val filtered = remember(apps, query) { apps.filter { it.first.contains(query, true) || it.second.contains(query, true) } }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Heading("Приложения на устройстве", ink)
        Text("Назначай отдельные изображения приложениям. Назначения сохраняются локально и входят в экспортируемый набор.", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Назначено: ${customIcons.size}", color = accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { exportLauncher.launch("NEXUS_Icon_Set.zip") }, enabled = customIcons.isNotEmpty()) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(5.dp)); Text("Экспорт ZIP") }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Поиск приложения") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        Spacer(Modifier.height(8.dp))
        if (apps.isEmpty()) {
            Text("Список приложений недоступен на этой сборке Android.", color = muted)
        } else {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filtered.size) { index ->
                    val app = filtered[index]
                    val assigned = customIcons[app.second]
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(card).clickable { chooseIcon(app.second) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val bitmap = remember(assigned) { assigned?.let { uri -> runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull() } }
                        if (bitmap != null) Image(bitmap, contentDescription = app.first, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        else Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(.16f)), contentAlignment = Alignment.Center) { Text(app.first.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold) }
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(app.first, color = ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(app.second, color = muted, fontSize = 10.sp, maxLines = 1)
                        }
                        Text(if (assigned != null) "Изменить" else "Назначить", color = accent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
    if (exportMessage) AlertDialog(onDismissRequest = { exportMessage = false }, title = { Text("Набор экспортирован") }, text = { Text("ZIP содержит изображения, назначенные приложениям, и manifest.json с привязкой к package name. Это переносимый набор для дальнейшей сборки темы; сам Android Launcher не изменён.") }, confirmButton = { TextButton(onClick = { exportMessage = false }) { Text("Понятно") } })
}

@Composable
private fun Heading(text: String, color: Color) {
    Text(text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(9.dp))
}
