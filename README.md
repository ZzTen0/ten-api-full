# Backstage-end 项目目录

API 开放平台已集中到 [ten-api-full](ten-api-full/README.md)，目录布局参考
[ZzTen0/ten-api-full](https://github.com/ZzTen0/ten-api-full)。

| 目录 | 内容 |
| --- | --- |
| `ten-api-full/` | TEN API 平台：6 个模块、部署配置、文档及前端预览资料 |
| `cyber-fortune-telling/` | 独立项目，保持原位 |
| `sky-take-out/` | 独立项目，保持原位 |
| `sql-father-backend-public-master/` | 独立项目，保持原位 |
| `ten-ai-agent/` | 独立项目，保持原位 |

开发或部署 API 平台时，先进入项目目录：

```powershell
cd D:\CODE\Backstage-end\ten-api-full
```

然后参照项目内的 [README](ten-api-full/README.md) 和
[部署说明](ten-api-full/DEPLOYMENT.md) 操作。
本地 `.env` 和密码文件已随项目移动，继续由 `.gitignore` 忽略。

现有 Git 仓库元数据 `.git/` 与旧后端仓库备份 `.git-backup-ten-api-backend/`
保留在本目录，版本历史未改动。Git 管理范围仍是整个 `Backstage-end` 目录；
`ten-api-full/` 是项目文件夹，不是新建的独立 Git 仓库。
