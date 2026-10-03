# MySQL 数据库说明

本项目使用 MySQL 8.0+，默认数据库名为 `research_collaboration`。

## 初始化数据库

在项目根目录执行：

```powershell
Get-Content .\data.sql -Raw | mysql -h 127.0.0.1 -P 3306 -u root -p
```

也可以使用 MySQL 客户端执行 `data.sql`。脚本会创建数据库、数据表、索引以及本地演示数据。

## 应用配置

不要把真实密码写入配置文件。启动前设置环境变量：

```powershell
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="research_collaboration"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="replace-with-a-long-random-secret"
```

项目根目录提供 `.env.example` 作为变量模板。Spring Boot 配置文件会读取这些环境变量。

## 数据表

| 表名 | 说明 |
| --- | --- |
| `sys_user` | 用户和账号 |
| `research_project` | 科研项目 |
| `project_member` | 项目成员关系 |
| `research_task` | 科研任务 |
| `project_file` | 项目文件元数据 |
| `research_paper` | 项目论文成果 |
| `project_discussion` | 项目讨论和回复 |
| `notification` | 消息通知 |

## 演示账号

数据库初始化后，应用会按初始化逻辑写入本地演示账号和业务数据。演示账号仅用于本地开发，生产环境请立即修改密码并替换 JWT 密钥。
