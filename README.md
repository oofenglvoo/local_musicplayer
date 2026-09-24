# 本地音乐播放器 (Local Music Player)

一个纯本地、无网络的 Android 音乐播放器，使用 Kotlin + Jetpack Compose + Media3 构建，采用沉浸式深色音乐播放器 UI，并支持动态封面、本地图片、渐变与纯色背景。

## 功能特性

- **本地音乐库**：扫描 MediaStore 全盘音频，支持搜索、排序（标题/艺术家/专辑/时长/年份/添加时间等，可升降序）
- **多维度浏览**：歌曲 / 专辑 / 艺术家 / 文件夹 / 播放列表 五个分类标签，横向滑动切换，专辑支持网格与列表布局
- **自动列表**：最近添加、最近播放、最常播放
- **手动选择目录**：内置文件浏览器，可选择任意目录递归扫描音频文件，与 MediaStore 结果合并去重，选择会持久化保存
- **后台播放**：基于 Media3 `MediaSessionService` + ExoPlayer，支持通知栏控制（含随机/循环按钮）、锁屏控制、耳机线控
- **播放详情页**：封面随播放旋转、高斯模糊背景、左右滑动切歌、进度拖拽、音量滑块、随机/循环/收藏、书签、均衡器快捷入口
- **播放队列**：查看、跳转、拖拽调整顺序、移除
- **迷你播放条**：全局悬浮于底部导航之上，含进度条与随机/播放/下一首控制
- **歌曲操作**：长按弹出菜单 —— 下一首播放、加入队列、添加到播放列表、收藏、分享、编辑歌词、更换封面、设为铃声、歌曲详情、删除文件
- **多选批量操作**：长按进入多选，支持全选、批量加入播放列表 / 队列 / 下一首播放、批量删除
- **播放列表**：新建、重命名、删除、播放全部、随机播放、移除曲目、拖拽排序
- **收藏**：歌曲收藏与收藏列表，支持随机播放
- **专辑封面**：优先读取音频内嵌封面（提取到应用缓存并等比缩放），回退到 MediaStore 封面，支持手动指定封面（不受重新扫描影响），无封面时显示占位图标
- **歌词**：支持同名 `.lrc` 文件与音频内嵌歌词，兼容 ID3 `USLT` / `TXXX:USLT` / `SYLT` 字段及 UTF-8、UTF-16 编码，逐句同步高亮滚动并带渐变缩放动效；支持内置编辑器编辑歌词与调整时间偏移
- **书签**：为歌曲记录播放位置并快速跳转
- **音效**：均衡器（含系统预设与频段调节）、低音增强、环绕音效、音量增强
- **播放参数**：播放速度、跳过静音、交叉淡入淡出（0–10 秒）
- **ReplayGain 音量归一化**：关闭 / 音轨增益 / 专辑增益
- **睡眠定时**：按分钟（15/30/45/60）或按曲数（1/3/5/10 首），到点淡出并自动暂停
- **外观设置**：跟随系统 / 浅色 / 深色 / 纯黑（AMOLED），支持动态取色与自定义强调色
- **沉浸式背景**：支持当前封面动态背景、本地图片、渐变与纯色背景，可调背景模糊和暗度
- **音乐库设置**：忽略短音频阈值、管理扫描目录与排除目录
- **主界面导航**：底部音乐库 / 播放列表 / 收藏三个标签
- **桌面小组件**：基于 Jetpack Glance，显示封面、歌曲信息并支持随机/播放/暂停/切歌/循环

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
│   ├── PlaybackCoordinator.kt      # 播放统计 / 队列持久化 / 设置应用
│   ├── AudioEffectsController.kt   # 音效控制
│   ├── ReplayGain.kt               # ReplayGain 读取与音频处理器
│   ├── ReplayGainManager.kt        # ReplayGain 模式管理
│   ├── CrossfadeController.kt      # 交叉淡入淡出
│   ├── SkipSilenceBridge.kt        # 跳过静音桥接
│   └── SleepTimerController.kt     # 睡眠定时（按分钟/按曲数）
├── lyrics/             # 歌词解析、展示与编辑
├── ui/                 # Compose 界面
│   ├── library/        # 音乐库（分类标签、排序、详情）
│   ├── song/           # 歌曲菜单、详情、添加到播放列表、封面选择
│   ├── search/         # 全局搜索
│   ├── player/         # 播放详情、迷你条、队列、书签
│   ├── playlist/       # 播放列表、收藏
│   ├── folder/         # 文件浏览器
│   ├── settings/       # 外观 / 音效 / 音乐库 / 关于
│   └── theme/          # 主题
├── util/               # 工具（时长格式化、歌曲操作、多选状态）
└── widget/             # Glance 桌面小组件
```

## 权限

| 权限 | 用途 |
|---|---|
| `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API 32-) | 扫描本地音频 |
| `POST_NOTIFICATIONS` | 播放通知 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放前台服务 |
| `WAKE_LOCK` | 播放期间保持唤醒 |
| `WRITE_SETTINGS` | 将歌曲设为铃声 |

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
- 歌词匹配规则：优先读取音频同目录下的同名 `.lrc` 文件（支持 UTF-8 / UTF-16 编码），其次读取音频文件内嵌歌词；歌词编辑器可将修改与时间偏移写回同名 `.lrc`。
- 通过文件浏览器扫描的歌曲使用合成 ID，与 MediaStore 歌曲按文件路径去重合并。
- 数据库当前版本为 3，升级时采用破坏性迁移（会清空歌库数据，需重新扫描）。
- 若需在中文路径下构建，需在 `gradle.properties` 中启用 `android.overridePathCheck=true`。

## 许可证

个人自用项目，未指定开源许可证。
