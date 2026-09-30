# Изолированный Android Emulator для проверки

Подготовка 30.09.2026. Используется существующий Android Emulator пользователя, а system image, AVD, userdata и служебные файлы проверки находятся в игнорируемой Git папке `.tools`. Пользовательский SDK и прежние AVD не заменялись.

## Проверенное окружение

- Emulator 37.1.11.0, build 15917651.
- `emulator -accel-check`: код 0, `WHPX(10.0.26200) is installed and usable`.
- CPU AMD Ryzen 5 7500F; используется аппаратная виртуализация WHPX.
- AOSP Android 14 API 34, default x86_64, revision 4; образ без Google Play.
- Разрешение 750 × 1624, плотность 320 dpi: область 375 × 812 dp до системных insets.
- 2 CPU, RAM 2048 МБ, GPU SwiftShader, Vulkan выключен.
- Эмулятор запускается без окна и звука; ADB адрес `emulator-5556`.
- Первая загрузка подтверждена 30.09.2026: ADB сообщил `device`, `sys.boot_completed` вернул `1`.

## Полученные файлы

Command-line tools взяты с [официальной страницы Android Developers](https://developer.android.com/studio).
Файл `commandlinetools-win-15859902_latest.zip`, размер 155655386 байт.
SHA-256: `90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a`.
Хеш скачанного файла совпал с опубликованным.

Образ получен по записи `system-images;android-34;default;x86_64` в официальном
[каталоге образов Android](https://dl.google.com/android/repository/sys-img/android/sys-img2-3.xml).
Файл [x86_64-34_r04.zip](https://dl.google.com/android/repository/sys-img/android/x86_64-34_r04.zip),
размер 720747116 байт, SHA-1: `5f6a249f9bc3b1b4c459b13ce2eb646c9680bed1`.
Хеш архива проверен перед распаковкой.

Архивы сохранены в `.tools/downloads`; image распакован в
`.tools/android-sdk/system-images/android-34/default/x86_64`.
Command-line tools размещены в `.tools/android-sdk/cmdline-tools/latest`.
Исполняемый emulator берётся из существующего SDK: `$env:LOCALAPPDATA/Android/Sdk/emulator/emulator.exe`.
Установка новой копии emulator, гипервизора или изменение Windows Features не выполнялись.

## Запуск

Из корня проекта в PowerShell:

```powershell
.\scripts\start-emulator.ps1
$adbPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adbPath devices -l
& $adbPath -s emulator-5556 shell getprop sys.boot_completed
```

Дождаться значения `1` для `sys.boot_completed`. Статус `offline` во время первой загрузки не подтверждает готовность устройства. Скрипт не запускает Gradle.

Скрипт использует существующий Windows 8.3 short-path алиас папки `.tools`, поскольку некоторые файловые функции emulator не смогли прочитать файлы по кириллическому пути. Это та же папка, файлы не переносились. Если 8.3 aliases отключены на другом компьютере и путь содержит не-ASCII символы, потребуется расположение тестовых AVD/image по ASCII-пути.

AVD config создаётся скриптом в `.tools/avd/Contest_API34.avd`. Первоначальный `avdmanager create avd` обнаружил образ, но отказал из-за отсутствия пакета emulator в изолированном SDK. Вместо копирования или изменения пользовательского SDK используется отдельный AVD config и установленный emulator. Все переменные окружения задаются только для процесса скрипта и дочернего emulator.

## Установка и снимок после сборки

```powershell
& $adbPath -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk
& $adbPath -s emulator-5556 install -r storybook\build\outputs\apk\debug\storybook-debug.apk
& $adbPath -s emulator-5556 shell am start -n ru.professionals.storybook/.StorybookActivity
& $adbPath -s emulator-5556 shell screencap -p /sdcard/storybook.png
& $adbPath -s emulator-5556 pull /sdcard/storybook.png captures/storybook.png
```

Папку `captures` необходимо создать заранее. Скриншот следует открыть и проверить; один успешный запуск не подтверждает весь ручной план.

Журналы запуска: `.tools/emulator-stdout.log`, `.tools/emulator-stderr.log`. Не публиковать полные диагностические файлы без проверки: emulator может записывать служебные идентификаторы и публичные ADB-ключи.

Остановить только тестовое устройство:

```powershell
& $adbPath -s emulator-5556 emu kill
```

Флаги запуска основаны на [документации командной строки Android Emulator](https://developer.android.com/studio/run/emulator-commandline) и [документации ускорения](https://developer.android.com/studio/run/emulator-acceleration).
