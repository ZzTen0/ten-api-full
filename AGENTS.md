# Workspace instructions

## TEN API repository synchronization

The user requires future changes to the TEN API project to be committed and pushed to both repositories after the task is complete and appropriate validation passes, unless the user explicitly requests local-only work or postpones pushing:

- GitHub: https://github.com/ZzTen0/ten-api-full.git
- Gitee: https://gitee.com/zzten00/ten-api-full.git

This requirement applies to `ten-api-full/` and its workspace-level supporting files. Other projects in this workspace are outside this repository's project scope.

- The Git root is `D:\CODE\Backstage-end`; the application root is `D:\CODE\Backstage-end\ten-api-full`.
- The `all` remote has both repositories as push URLs. Prefer `git push all HEAD` to push the current branch to both destinations. `remote.pushDefault=all` also makes ordinary pushes use both destinations, unless a branch-specific setting overrides it.
- The root `commit.ps1 -Message "description"` stages the TEN API scope, commits it, and pushes the `gitee` and `github` remotes separately with per-destination results. Run appropriate validation first. Use `commit.ps1 -PushOnly -Remote GitHub` or `-Remote Gitee` to retry only a failed destination without staging or committing again.
- Stage only changes relevant to the current task, accounting for any explicitly authorized pending directory migration. Preserve unrelated user changes.
- Never commit `.env`, password files, private keys, build output, or local repository backups. Preserve the existing ignore rules.
- A push to multiple URLs is not atomic. Check the result for both repositories; if one fails, report the partial result and retry only the failed destination after resolving the cause.
- Do not force-push or overwrite remote history without explicit user authorization. If remote commits prevent a normal push, inspect the divergence and preserve both sides' work.
- Report actual push outcomes. Configuring a remote or completing a dry run does not mean changes were published.

## Code comments and formatting

- Follow the existing Java code style when adding or editing TEN API code. Use Chinese Javadoc for new classes and methods, explaining responsibilities, parameters, return values, and relevant failure conditions.
- Document request/response fields with their meaning, defaults, limits, units, and conditional requirements. Do not leave empty `@param` or `@return` descriptions.
- Explain important authentication, quota, encoding, and algorithm decisions with concise Chinese comments. Keep annotations and declarations on separate lines and separate methods with blank lines.
- Give new test classes and frontend helpers clear descriptions of their purpose. Update comments when behavior changes; do not invent authors or creation dates.
