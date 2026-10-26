#Requires -Version 5.1
<#
.SYNOPSIS
Commit TEN API changes and push the current branch to Gitee and GitHub.
.EXAMPLE
.\commit.ps1 -Message "feat: add a new API"
.EXAMPLE
.\commit.ps1 -PushOnly -Remote GitHub
.EXAMPLE
.\commit.ps1 -DryRun
#>
[CmdletBinding()]
param(
    [string]$Message,
    [switch]$PushOnly,
    [ValidateSet('Both', 'GitHub', 'Gitee')]
    [string]$Remote = 'Both',
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (Get-Variable PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Invoke-Git {
    param([string[]]$GitArguments)
    # Git writes progress and warnings to stderr, including on success.
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = @(& git @GitArguments 2>&1 | ForEach-Object { $_.ToString() })
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $savedPreference
    }
    if ($code -ne 0) {
        throw "git $($GitArguments[0]) failed (exit $code).`n$($output -join [Environment]::NewLine)"
    }
    return $output
}

function Get-GitPaths {
    param([string[]]$GitArguments)
    $output = @(Invoke-Git -GitArguments $GitArguments)
    return @((($output -join "`n") -split "`0") | Where-Object { $_.Length -gt 0 })
}

function Test-ProjectPath {
    param([string]$Path)
    return $Path.StartsWith('ten-api-full/', [StringComparison]::OrdinalIgnoreCase) -or
        $Path -in @('README.md', '.gitignore', 'AGENTS.md', 'commit.ps1')
}

function Assert-PublishablePaths {
    param([string[]]$Paths)
    foreach ($path in $Paths) {
        if (-not (Test-ProjectPath $path)) {
            throw "Unrelated staged path: $path. Commit or unstage it separately first."
        }
        if (($path -match '(?i)(^|/)\.env(?:\.[^/]*)?$' -and $path -notmatch '(?i)(^|/)\.env\.example$') -or
            $path -match '(?i)(^|/)(node_modules|target|dist|\.build|\.git(?:-backup[^/]*)?)(/|$)' -or
            $path -match '\u5bc6\u7801' -or
            $path -match '(?i)\.(pem|key|p12|pfx|jks)$' -or
            $path -match '(?i)(^|/)[^/]*(passwords?|credentials)\.(txt|json|ya?ml|ini|properties|csv)$') {
            throw "Local-only or sensitive path cannot be committed: $path"
        }
    }
}

$locationPushed = $false
$exitCode = 2
$previousConsoleEncoding = [Console]::OutputEncoding
try {
    [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    Get-Command git -ErrorAction Stop | Out-Null
    Push-Location -LiteralPath $PSScriptRoot
    $locationPushed = $true
    $gitRoot = (@(Invoke-Git -GitArguments @('rev-parse', '--show-toplevel')) -join '').Trim()
    if ([IO.Path]::GetFullPath($gitRoot).TrimEnd('\', '/') -ne
        [IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\', '/')) {
        throw 'Place commit.ps1 at the Git repository root.'
    }
    $branch = (@(Invoke-Git -GitArguments @('symbolic-ref', '--quiet', '--short', 'HEAD')) -join '').Trim()
    foreach ($operation in @('MERGE_HEAD', 'CHERRY_PICK_HEAD', 'REVERT_HEAD', 'rebase-merge', 'rebase-apply')) {
        $operationPath = (@(Invoke-Git -GitArguments @('rev-parse', '--git-path', $operation)) -join '').Trim()
        if (Test-Path -LiteralPath $operationPath) { throw 'Finish the in-progress Git operation before publishing.' }
    }
    if (@(Invoke-Git -GitArguments @('ls-files', '--unmerged')).Count -gt 0) {
        throw 'Resolve merge conflicts before publishing.'
    }

    $targets = if ($Remote -eq 'Both') { @('gitee', 'github') } else { @($Remote.ToLowerInvariant()) }
    foreach ($target in $targets) {
        $urls = @(Invoke-Git -GitArguments @('remote', 'get-url', '--push', '--all', $target))
        if ($urls.Count -ne 1) { throw "Remote '$target' must have exactly one push URL." }
    }
    Write-Host "Branch: $branch"
    Write-Host "Push destinations: $($targets -join ', ')"

    if (-not $PushOnly) {
        $scope = @('ten-api-full', 'README.md', '.gitignore', 'AGENTS.md', 'commit.ps1')
        $tracked = @(Get-GitPaths -GitArguments @('ls-files', '-z', '--'))
        $scope = @($scope | Where-Object {
            $entry = $_
            (Test-Path -LiteralPath $entry) -or @($tracked | Where-Object {
                $_ -eq $entry -or $_.StartsWith($entry + '/', [StringComparison]::OrdinalIgnoreCase)
            }).Count -gt 0
        })
        Assert-PublishablePaths -Paths @(Get-GitPaths -GitArguments @('diff', '--cached', '--name-only', '-z'))
        Assert-PublishablePaths -Paths @(Get-GitPaths -GitArguments (
            @('ls-files', '--cached', '--others', '--exclude-standard', '-z', '--') + $scope))
        $changes = @(Invoke-Git -GitArguments (@('-c', 'core.quotepath=false', 'status', '--short', '--') + $scope))
        if ($changes.Count -gt 0) {
            Write-Host ($changes -join [Environment]::NewLine)
            if (-not $DryRun -and [string]::IsNullOrWhiteSpace($Message)) {
                throw 'Changes found. Supply a commit message with -Message "your description".'
            }
            if (-not $DryRun) {
                Invoke-Git -GitArguments (@('add', '--all', '--') + $scope) | ForEach-Object { Write-Host $_ }
                Assert-PublishablePaths -Paths @(Get-GitPaths -GitArguments @('diff', '--cached', '--name-only', '-z'))
                Invoke-Git -GitArguments @('diff', '--cached', '--check') | ForEach-Object { Write-Host $_ }
                $messageFile = [IO.Path]::GetTempFileName()
                try {
                    [IO.File]::WriteAllText($messageFile, $Message + [Environment]::NewLine,
                        (New-Object System.Text.UTF8Encoding($false)))
                    Invoke-Git -GitArguments @('commit', '--file', $messageFile) | ForEach-Object { Write-Host $_ }
                } finally {
                    Remove-Item -LiteralPath $messageFile -ErrorAction SilentlyContinue
                }
            }
        } else {
            Write-Host 'No new project changes to commit. Existing commits will still be pushed.'
        }
    }

    if ($DryRun) {
        Write-Host 'Preview only: no staging, commit, or network push was performed.'
        $exitCode = 0
    } else {
        $head = (@(Invoke-Git -GitArguments @('rev-parse', 'HEAD')) -join '').Trim()
        $failed = @()
        foreach ($target in $targets) {
            Write-Host "Pushing $head to $target..."
            try {
                Invoke-Git -GitArguments @('push', '--porcelain', $target, "HEAD:refs/heads/$branch") |
                    ForEach-Object { Write-Host $_ }
                Write-Host "[OK] $target"
            } catch {
                $failed += $target
                Write-Host "[FAILED] $target`: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
        if ($failed.Count -eq 0) {
            Write-Host "Published $head to all selected destinations." -ForegroundColor Green
            $exitCode = 0
        } else {
            foreach ($target in $failed) {
                Write-Host "Retry only this destination: .\commit.ps1 -PushOnly -Remote $target"
            }
            Write-Host 'The local commit was preserved. No force-push or automatic merge was attempted.'
            $exitCode = 1
        }
    }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    $exitCode = 2
} finally {
    if ($locationPushed) { Pop-Location }
    [Console]::OutputEncoding = $previousConsoleEncoding
}
exit $exitCode
