# 本地音乐播放器 (Local Music Player)

一个纯本地、无网络的 Android 音乐播放器，使用 Kotlin + Jetpack Compose + Media3 构建，界面采用 Material You 动态取色。

## 功能特性

- **本地音乐库**：扫描 MediaStore 全盘音频，支持搜索、按歌手筛选
- **手动选择目录**：内置文件浏览器，可选择任意目录递归扫描音频文件，与 MediaStore 结果合并去重，选择会持久化保存
- **后台播放**：基于 Media3 `MediaSessionService` + ExoPlayer，支持通知栏控制、锁屏控制、耳机线控
- **播放详情页**：专辑封面、进度拖拽、上一首/下一首/暂停
- **迷你播放条**：全局悬浮于底部导航之上
- **播放列表**：新建、播放全部、添加/移除歌曲
- **收藏**：歌曲收藏与收藏列表
- **专辑封面**：优先读取音频内嵌封面（提取到应用缓存并等比缩放），回退到 MediaStore 封面，无封面时显示占位图标
- **歌词**：支持同名 `.lrc` 文件与音频内嵌歌词，逐句同步高亮滚动
- **音效**：均衡器（含系统预设与频段调节）、低音增强、环绕音效、音量增强
- **睡眠定时**：15 / 30 / 45 / 60 分钟，到点淡出并自动暂停
- **桌面小组件**：基于 Jetpack Glance，显示当前歌曲并支持播放/暂停/切歌

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 | Kotlin 2.1.20 |
| UI | Jetpack Compose + Material 3（动态取色） |
| 播放 | AndroidX Media3 1.6.1（ExoPlayer + MediaSessionService） |
| 数据库 | Room 2.7.1 |
| 依赖注入 | Hilt |
| 偏好存储 | DataStore Preferences |
| 图片加载 | Coil |
| 桌面小组件 | Jetpack Glance |
| 构建 | Gradle 8.13 (Kotlin DSL) |
| 最低版本 | Android 10 (API 29) |
| 目标版本 | Android 15 (API 35) |

## 项目结构

```
app/src/main/java/com/localmusic/player/
├── data/               # 数据层
│   ├── db/             # Room 实体、DAO、数据库
│   ├── MediaStoreScanner.kt   # MediaStore 扫描
│   ├── FolderScanner.kt       # 目录递归扫描
│   ├── ArtworkCache.kt        # 内嵌封面提取与缓存
│   ├── AlbumArt.kt            # 统一封面来源
│   ├── SettingsStore.kt       # DataStore 配置
│   └── MusicRepository.kt     # 仓库层
├── playback/           # 播放层
│   ├── PlaybackService.kt          # MediaSessionService
│   ├── PlayerConnection.kt         # MediaController 连接
│   ├── AudioEffectsController.kt   # 音效控制
│   └── SleepTimerController.kt     # 睡眠定时
├── lyrics/             # 歌词解析与展示
├── ui/                 # Compose 界面
│   ├── library/        # 音乐库
│   ├── player/         # 播放详情、迷你条
│   ├── playlist/       # 播放列表、收藏
│   ├── folder/         # 文件浏览器
│   ├── settings/       # 音效与定时
│   └── theme/          # 主题
└── widget/             # Glance 桌面小组件
```

## 权限

| 权限 | 用途 |
|---|---|
| `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API 32-) | 扫描本地音频 |
| `POST_NOTIFICATIONS` | 播放通知 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放前台服务 |
| `WAKE_LOCK` | 播放期间保持唤醒 |

## 构建

前置：JDK 17+、Android SDK（platform 35、build-tools 35.0.0）。

在 `local.properties` 中配置 SDK 路径：

```properties
sdk.dir=/path/to/Android/Sdk
```

构建 Debug APK：

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

安装到设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 说明

- 本项目为**纯本地播放器**，不包含任何在线音源或网络请求。
- 歌词匹配规则：优先读取音频同目录下的同名 `.lrc` 文件（支持 UTF-8 / UTF-16 编码），其次读取音频文件内嵌歌词。
- 通过文件浏览器扫描的歌曲使用合成 ID，与 MediaStore 歌曲按文件路径去重合并。

## 许可证

个人自用项目，未指定开源许可证。
