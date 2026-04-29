# 香港智慧灯柱实时天气小程序

本项目是一个用于实时查看香港智慧灯柱试验性气象数据的微信小程序。系统由 Java Spring Boot 后端、可配置的 MySQL/H2 数据库和微信原生小程序端组成，后端定时从香港天文台开放数据接口获取气象数据并保存到数据库，小程序端负责展示当前区间数据、历史数据和温度统计结果。

## 数据来源

数据来源为香港开放数据集：

- 数据集页：https://data.gov.hk/sc-data/dataset/hk-hko-rss-smart-lamppost-weather-data
- 灯柱位置：https://www.hko.gov.hk/common/hko_data/smart-lamppost/files/smart_lamppost_met_device_location.json
- 灯柱类型与设备：https://www.hko.gov.hk/common/hko_data/smart-lamppost/files/smart_lamppost_met_device_type.json
- 实时天气接口示例：https://data.weather.gov.hk/weatherAPI/smart-lamppost/smart-lamppost.php?pi=GF3637&di=01

数据内容包括：

- 气温
- 相对湿度
- 十分钟平均风向
- 十分钟平均风速

## 系统架构

```text
香港天文台开放数据接口
        |
        v
Java Spring Boot 后端
        |
        v
MySQL/H2 数据库
        |
        v
微信小程序端
```

## 目录结构

```text
backend/       Spring Boot 后端
miniprogram/   微信原生小程序
```

## 后端说明

后端使用 Java Spring Boot 开发，主要负责：

- 获取智慧灯柱位置与设备类型
- 按灯柱编号和设备编号获取实时气象数据
- 将每次获取的数据写入当前启用的数据库
- 按自然 10 分钟区间提供当前数据
- 提供历史获取数据查询
- 计算今日最高平均温度和最低平均温度

技术栈：

- Java 17
- Spring Boot 3
- MySQL 8 或 H2
- JDBC

默认 MySQL 配置：

```text
host: localhost:3306
database: hk_weather
username: root
password: root
```

数据库可通过配置选择：

```powershell
# 使用 MySQL，默认值
$env:APP_DATABASE="mysql"

# 使用 H2 文件型备份数据库
$env:APP_DATABASE="h2"
```

MySQL 配置：

```powershell
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="root"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/hk_weather?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
```

H2 配置：

```powershell
$env:H2_URL="jdbc:h2:file:./data/hk_weather;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
$env:H2_USERNAME="sa"
$env:H2_PASSWORD=""
$env:H2_CONSOLE_ENABLED="true"
```

H2 数据默认保存到后端目录下的 `data/` 文件夹，适合作为本地备份或轻量运行方案。H2 控制台默认地址为：

```text
http://localhost:8080/h2-console
```

采集间隔配置在后端环境变量中，默认 10 分钟：

```powershell
$env:WEATHER_FETCH_INTERVAL_MINUTES="10"
```

常用配置：

```powershell
$env:APP_DATABASE="mysql"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="root"
$env:SERVER_PORT="8080"
$env:WEATHER_SYNC_ON_START="true"
$env:WEATHER_MAX_LAMPPOSTS="0"
```

`WEATHER_MAX_LAMPPOSTS=0` 表示采集全部灯柱；开发调试时可以设成 `5` 先少量采集。

## 数据获取规则

系统按照自然时间的 10 分钟边界获取数据，例如：

```text
01:00
01:10
01:20
01:30
01:40
01:50
```

如果系统在 `01:05` 才启动，那么 `01:10` 这一次不作为完整 10 分钟区间参与平均温度最高/最低统计，下一次 `01:20` 起才参与统计。

## 数据展示规则

### 当前数据

首页只展示当前自然 10 分钟区间内获取到的数据。

例如当前时间是 `13:16`，则首页展示 `13:10` 到 `13:20` 之间后端获取到的数据。

当前数据按后端获取时间倒序排列：

```text
13:18
13:15
13:12
```

时间越后的数据越靠前。

### 历史数据

小程序首页提供“历史”按钮，点击后进入历史获取数据页面。

历史页面展示当前 10 分钟区间之前获取到的数据，同样按后端获取时间倒序排列。

例如：

```text
01:12
01:10
01:05
```

其中 `01:12` 会排在 `01:10` 前面。

## 温度统计规则

首页展示：

- 本次平均温度
- 今日最高平均温度
- 今日最低平均温度
- 本次平均湿度
- 本次平均风速

今日最高平均温度和今日最低平均温度按完整自然 10 分钟区间计算。程序只统计实际运行期间成功获取到的数据，不会补算程序未开启期间的数据。

## 微信小程序说明

小程序主要页面包括：

- 首页：展示当前 10 分钟区间天气数据、平均温度、湿度、风速和今日最高/最低平均温度
- 历史页：展示之前获取到的历史天气数据
- 详情页：展示某个灯柱的坐标、设备信息和历史观测记录

小程序页面会每 1 分钟自动向后端同步一次显示数据。

用微信开发者工具打开小程序目录：

```text
C:\Users\Desktop\weather\miniprogram
```

本地调试时，小程序请求地址配置在：

```text
miniprogram/app.js
```

默认后端地址：

```javascript
apiBaseUrl: 'http://localhost:8080/api/weather'
```

如果使用真机预览，需要将 `localhost` 改为电脑局域网 IP，并在微信开发者工具中开启“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。正式上线时需要把后端部署到 HTTPS 域名，并在微信公众平台配置 request 合法域名。

## 数据库表说明

### lampposts

保存智慧灯柱基础信息：

- 灯柱编号
- 经纬度
- 灯柱类型
- 设备编号

### weather_observations

保存每次获取到的天气数据：

- 灯柱编号
- 设备编号
- 气温
- 相对湿度
- 十分钟平均风向
- 十分钟平均风速
- 数据观测时间
- 后端获取时间
- 原始接口 JSON

`weather_observations` 对 `(lp_number, device_id, source_observed_at)` 做了唯一约束，所以官方 10 分钟更新一次时，重复采集同一条观测不会产生重复数据。

### sync_runs

保存每次同步任务信息：

- 同步状态
- 开始时间
- 结束时间
- 获取数量
- 失败数量
- 本批平均温度
- 本批平均湿度
- 本批平均风速

## 主要接口

```text
GET  /api/weather/health
GET  /api/weather/summary
GET  /api/weather/latest?q=GF&limit=80
GET  /api/weather/history?q=GF&limit=200
GET  /api/weather/lampposts
GET  /api/weather/lampposts/{lpNumber}
GET  /api/weather/lampposts/{lpNumber}/history?deviceId=01&limit=48
POST /api/weather/sync
GET  /api/weather/sync/latest
```

## 启动方式

进入后端目录：

```powershell
cd C:\Users\Desktop\weather\backend
```

启动后端：

```powershell
mvn spring-boot:run
```

后端会根据 `APP_DATABASE` 自动执行对应建表脚本：

```text
MySQL: backend/src/main/resources/schema-mysql.sql
H2:    backend/src/main/resources/schema-h2.sql
```

也可以手动执行 MySQL 建表脚本：

```powershell
mysql -u root -proot < C:\Users\Desktop\weather\backend\src\main\resources\schema-mysql.sql
```

## 项目特点

- 使用官方开放数据集
- 支持 MySQL 持久化保存，也支持 H2 文件型备份数据库
- 支持当前区间数据和历史数据分开查看
- 支持按照后端获取时间倒序展示
- 支持今日最高/最低平均温度统计
- 小程序端每分钟自动刷新
- 采集间隔可通过后端环境变量调整
