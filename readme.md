# 校园科研协作平台

一个围绕用户、科研项目、项目成员、科研任务、项目文件、论文成果、讨论和通知构建的校园科研协作系统。

## 技术栈

- 后端：Java 21、Spring Boot 3.3、Spring Security、JWT、MyBatis-Plus
- 前端：Vue 3、Vite、Vue Router、Pinia、Axios、ECharts、Lucide
- 数据库：MySQL 8.0+

## 快速启动

环境要求：JDK 21、Maven 3.9+、Node.js 18+、MySQL 8.0+。

1. 初始化数据库：

```powershell
Get-Content .\data.sql -Raw | mysql -h 127.0.0.1 -P 3306 -u root -p
```

2. 设置数据库和 JWT 环境变量，可参考 `.env.example`：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="replace-with-a-long-random-secret"
```

3. 启动后端：

```powershell
cd backend
mvn spring-boot:run
```

4. 启动前端：

```powershell
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`  
后端地址：`http://localhost:8080`

也可以在项目根目录运行：

```powershell
.\start-all.ps1
```

停止服务：

```powershell
.\stop-all.ps1
```

## 主要模块

- 用户注册、登录、JWT 身份认证
- 科研项目创建、编辑、结项和进度管理
- 项目成员与角色管理
- 任务分配、优先级、截止日期、进度和状态流转
- 项目文件上传、分类、下载和删除
- 论文登记、投稿状态和成果链接
- 项目讨论与回复
- 通知中心和批量已读
- 科研总览、项目进度和任务分布图表

## 本地演示账号

登录页面不展示演示账号信息。需要体验本地演示数据时，可使用以下账号登录，统一密码为 `123456`：

- `zhangwei`：项目负责人
- `liming`：项目管理员
- `wangfang`：项目成员
- `chenyu`：研究生成员
- `admin`：系统管理员

## 测试与构建

```powershell
cd backend
mvn test
mvn package

cd ..\frontend
npm run build
```
