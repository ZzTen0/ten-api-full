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

## 提交与双仓库推送

在本目录运行 [commit.ps1](commit.ps1)，填写提交说明即可提交 TEN API 的变更，并分别推送当前分支到 Gitee 和 GitHub：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\commit.ps1 -Message "feat: 新增接口"
```

也可使用 PowerShell 7 的 `pwsh`。脚本从自身位置定位仓库，在其他目录调用时使用脚本的完整路径即可。

```powershell
# 预览变更和目标，不暂存、不提交、不联网推送
.\commit.ps1 -DryRun

# 只重试 GitHub，保留现有提交及工作区中的未提交变更
.\commit.ps1 -PushOnly -Remote GitHub

# 仅向 Gitee 提交并推送本次变更
.\commit.ps1 -Message "fix: 修复接口问题" -Remote Gitee
```

脚本暂存 `ten-api-full/` 和根目录的 README、忽略规则、AGENTS 及脚本本身。
如果已有其他项目文件被暂存，会停止并保留暂存区；环境密钥、密码文件和构建目录也会被拒绝。
没有新变更时不创建空提交，仍会推送已有提交。脚本不代替构建和测试，运行前请先完成对应验证。

退出码：`0` 表示所选仓库全部成功，`1` 表示至少一个推送失败，`2` 表示检查或提交失败。
推送失败时本地提交保留，脚本会输出单独重试失败仓库的命令，不执行强制推送或自动合并。

本地验证脚本使用临时 Git 仓库，不访问真实 GitHub/Gitee：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ten-api-full\scripts\Test-CommitScript.ps1
```

## 提交与双仓库推送

在本目录运行 [commit.ps1](commit.ps1)，填写提交说明即可提交 TEN API 的变更，并分别推送当前分支到 Gitee 和 GitHub：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\commit.ps1 -Message "feat: 新增接口"
```

也可使用 PowerShell 7 的 `pwsh`。脚本从自身位置定位仓库，在其他目录调用时使用脚本的完整路径即可。

```powershell
# 预览变更和目标，不暂存、不提交、不联网推送
.\commit.ps1 -DryRun

# 只重试 GitHub，保留现有提交及工作区中的未提交变更
.\commit.ps1 -PushOnly -Remote GitHub

# 仅向 Gitee 提交并推送本次变更
.\commit.ps1 -Message "fix: 修复接口问题" -Remote Gitee
```

脚本暂存 `ten-api-full/` 和根目录的 README、忽略规则、AGENTS 及脚本本身。
如果已有其他项目文件被暂存，会停止并保留暂存区；环境密钥、密码文件和构建目录也会被拒绝。
没有新变更时不创建空提交，仍会推送已有提交。脚本不代替构建和测试，运行前请先完成对应验证。

退出码：`0` 表示所选仓库全部成功，`1` 表示至少一个推送失败，`2` 表示检查或提交失败。
推送失败时本地提交保留，脚本会输出单独重试失败仓库的命令，不执行强制推送或自动合并。

本地验证脚本使用临时 Git 仓库，不访问真实 GitHub/Gitee：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ten-api-full\scripts\Test-CommitScript.ps1
```
