Android 双列瀑布流与图文展示客户端

本项目是一个 Android 端的高性能双列异步图片瀑布流列表及沉浸式图文详情页应用。系统从底层网络流处理到上层 UI 渲染实现了完整的业务数据流转，并包含了基于高德 API 的实况天气查询以及本地轻量级账号系统。

## 核心功能
| **账户系统** | 基于 SQLite 数据库实现的本地登录验证，内置预埋测试账号及签名信息的持久化。|
| **瀑布流首页** | 接入 Unsplash API 的双列瀑布流，支持滑动触底时的防抖动预加载下一页。|
| **图文详情** | 支持大图预览与动态文本元数据展示，利用三级缓存体系实现无网络 IO 的极速二次加载。|
| **实况天气** | 接入高德地图天气 API，提供多城市的实时天气、温度及风力指标拟物化卡片展示。|
| **个人中心** |利用 SharedPreferences 跨页面同步用户的登录凭证与基本信息。|

## 技术栈与架构
| **开发语言** | Java, Kotlin|
| **页面布局** | ConstraintLayout, Fragment, RecyclerView, StaggeredGridLayoutManager|
| **网络请求** | OkHttp3, Retrofit2, GsonConverterFactory|
| **异步处理** | Retrofit Callback, RxJava3, Kotlin Coroutines (lifecycleScope)|
| **数据存储** | SQLiteOpenHelper, SharedPreferences, File IO|


## 关键技术实现
* **硬核的三级缓存 ImageLoader**：摒弃第三方库，纯手工搭建基于内存（LruCache） + 磁盘（File） + 网络（OkHttpManager）的三级缓存加载架构。
* **多线程并发控制**：利用设备 CPU 核心数动态分配固定线程池处理 IO，并在发起下载前利用 `ConcurrentHashMap` 精准拦截重复的 URL 数据流投递。
* **根治 RecyclerView 视图错位**：在子线程切回主线程 Handler 准备渲染前，强制比对 `imageView.getTag()` 与当前执行的 URL，彻底避免高频滑动复用导致的脏图显示。
* **极致顺滑的瀑布流**：显式配置排版策略为 `GAP_HANDLING_NONE` 防止高度计算跳变，并通过 `findLastVisibleItemPositions` 精准捕捉滑动位置以实现触底分页加载。
* **多范式网络层演进**：针对同一网络接口，并行实现了原生 Callback 机制、RxJava 响应式流以及现代化的 Kotlin 协程挂起函数，并结合生命周期管理切断引用，防御 OOM 及 NPE 异常。
* **本地落盘动态探测**：在 UI 数据绑定期间，通过 `MD5Utils` 哈希映射出本地磁盘文件，利用 `file.length()` 实时读取字节数并动态换算为文件大小，替代服务端冗余字段。

## 快速开始
**克隆项目到本地**
git clone [https://github.com/KeyNG-Y/2st.git](https://github.com/KeyNG-Y/2st.git)
