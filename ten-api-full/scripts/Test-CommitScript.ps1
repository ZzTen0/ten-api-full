#Requires -Version 5.1
[CmdletBinding()]
param([ValidateSet('powershell.exe', 'pwsh.exe')][string]$Shell = 'powershell.exe')

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$fixtureRoot = Join-Path $repositoryRoot ('.build/commit-script-tests-' + [Guid]::NewGuid().ToString('N'))
$checkout = Join-Path $fixtureRoot 'checkout'
$gitee = Join-Path $fixtureRoot 'gitee.git'
$github = Join-Path $fixtureRoot 'github.git'
$passed = 0

function Invoke-TestGit {
    param([string]$Directory, [string[]]$Arguments)
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = @(& git -C $Directory @Arguments 2>&1 | ForEach-Object { $_.ToString() })
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $savedPreference }
    if ($code -ne 0) { throw "Fixture git failed: $($output -join [Environment]::NewLine)" }
    return $output
}

function Write-Fixture {
    param([string]$Path, [string]$Text)
    New-Item -ItemType Directory -Path (Split-Path $Path) -Force | Out-Null
    [IO.File]::WriteAllText($Path, $Text, (New-Object System.Text.UTF8Encoding($false)))
}

function Invoke-Commit {
    param([hashtable]$Options)
    $entries = foreach ($key in $Options.Keys) {
        $value = $Options[$key]
        $literal = if ($value -is [bool]) { if ($value) { '$true' } else { '$false' } }
            else { "'" + ([string]$value).Replace("'", "''") + "'" }
        "'$key'=$literal"
    }
    $scriptPath = (Join-Path $checkout 'commit.ps1').Replace("'", "''")
    $command = '$options=@{' + ($entries -join ';') + "}; & '$scriptPath' @options; exit " + '$LASTEXITCODE'
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = @(& $Shell -NoProfile -ExecutionPolicy Bypass -OutputFormat Text -EncodedCommand $encoded 2>&1 |
            ForEach-Object { $_.ToString() })
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $savedPreference }
    return [pscustomobject]@{ Code = $code; Output = $output -join [Environment]::NewLine }
}

function Assert-True {
    param([bool]$Condition, [string]$Description)
    if (-not $Condition) { throw $Description }
}

function Assert-Exit {
    param($Result, [int]$Expected)
    Assert-True ($Result.Code -eq $Expected) "Expected exit $Expected, got $($Result.Code): $($Result.Output)"
}

function Passed {
    param([string]$Name)
    $script:passed++
    Write-Host "[PASS] $Name"
}

try {
    Get-Command $Shell -ErrorAction Stop | Out-Null
    foreach ($directory in @($checkout, $gitee, $github)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
        $arguments = @('init', '--initial-branch=main')
        if ($directory -ne $checkout) { $arguments += '--bare' }
        Invoke-TestGit $directory $arguments | Out-Null
        Invoke-TestGit $directory @('config', 'core.hooksPath', (Join-Path $directory $(if ($directory -eq $checkout) { '.git/hooks' } else { 'hooks' }))) | Out-Null
    }
    Invoke-TestGit $checkout @('config', 'user.name', 'Commit Script Test') | Out-Null
    Invoke-TestGit $checkout @('config', 'user.email', 'commit-script-test@example.invalid') | Out-Null
    Invoke-TestGit $checkout @('config', 'commit.gpgsign', 'false') | Out-Null
    Invoke-TestGit $checkout @('remote', 'add', 'gitee', $gitee) | Out-Null
    Invoke-TestGit $checkout @('remote', 'add', 'github', $github) | Out-Null
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'commit.ps1') -Destination (Join-Path $checkout 'commit.ps1')
    Write-Fixture (Join-Path $checkout 'README.md') "fixture`n"
    Write-Fixture (Join-Path $checkout 'AGENTS.md') "fixture`n"
    Write-Fixture (Join-Path $checkout '.gitignore') ".env`n.env.*`n!.env.example`n**/node_modules/`n**/target/`n**/dist/`n"
    Write-Fixture (Join-Path $checkout 'ten-api-full/api.txt') "initial`n"
    Invoke-TestGit $checkout @('add', '--all') | Out-Null
    Invoke-TestGit $checkout @('commit', '-m', 'initial fixture') | Out-Null
    $initial = (Invoke-TestGit $checkout @('rev-parse', 'HEAD')) -join ''
    Write-Fixture (Join-Path $checkout 'ten-api-full/api.txt') "updated`n"

    Assert-Exit (Invoke-Commit @{ DryRun = $true }) 0
    Assert-True (((Invoke-TestGit $checkout @('rev-parse', 'HEAD')) -join '') -eq $initial) 'Preview changed HEAD.'
    Assert-True (@(Invoke-TestGit $checkout @('diff', '--cached', '--name-only')).Count -eq 0) 'Preview changed the index.'
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $gitee 'refs/heads/main'))) 'Preview pushed a branch.'
    Passed 'Preview does not stage, commit, or push'

    Assert-Exit (Invoke-Commit @{}) 2
    Assert-True (@(Invoke-TestGit $checkout @('diff', '--cached', '--name-only')).Count -eq 0) 'Missing-message failure staged changes.'
    Passed 'Missing commit message fails before staging'

    Write-Fixture (Join-Path $checkout 'unrelated.txt') "other project`n"
    Invoke-TestGit $checkout @('add', '--', 'unrelated.txt') | Out-Null
    Assert-Exit (Invoke-Commit @{ Message = 'should not commit' }) 2
    Assert-True (((Invoke-TestGit $checkout @('diff', '--cached', '--name-only')) -join '') -eq 'unrelated.txt') 'Unrelated staging was changed.'
    Invoke-TestGit $checkout @('restore', '--staged', '--', 'unrelated.txt') | Out-Null
    Passed 'Unrelated staged work is preserved and blocks mixed commits'

    $passwordName = ([char]0x5bc6).ToString() + ([char]0x7801).ToString() + '.txt'
    foreach ($relative in @('ten-api-full/.env.production', ('ten-api-full/' + $passwordName), 'ten-api-full/target/private.txt')) {
        Write-Fixture (Join-Path $checkout $relative) "test-only secret`n"
        Invoke-TestGit $checkout @('add', '--force', '--', $relative) | Out-Null
        Assert-Exit (Invoke-Commit @{ Message = 'should not commit' }) 2
        Invoke-TestGit $checkout @('restore', '--staged', '--', $relative) | Out-Null
    }
    Passed 'Force-staged secrets, Unicode password filenames, and build outputs are rejected'

    # The password filename is intentionally untracked and not ignored yet: it must still block staging.
    Assert-Exit (Invoke-Commit @{ Message = 'should not commit' }) 2
    [IO.File]::AppendAllText((Join-Path $checkout '.gitignore'), $passwordName + "`n", (New-Object System.Text.UTF8Encoding($false)))
    $message = 'feat: preserve "quotes" and ' + ([char]0x63d0).ToString() + ([char]0x4ea4).ToString()
    Assert-Exit (Invoke-Commit @{ Message = $message }) 0
    $head = (Invoke-TestGit $checkout @('rev-parse', 'HEAD')) -join ''
    Assert-True (((Invoke-TestGit $gitee @('rev-parse', 'main')) -join '') -eq $head) 'Gitee did not receive the commit.'
    Assert-True (((Invoke-TestGit $github @('rev-parse', 'main')) -join '') -eq $head) 'GitHub did not receive the commit.'
    Assert-True (((Invoke-TestGit $checkout @('log', '-1', '--format=%s')) -join '') -eq $message) 'Commit message was corrupted.'
    Assert-True (-not (@(Invoke-TestGit $checkout @('ls-tree', '-r', '--name-only', 'HEAD')) -contains 'unrelated.txt')) 'Unrelated file was committed.'
    Passed 'Scoped commit preserves quoted Unicode messages and pushes both remotes'

    Assert-Exit (Invoke-Commit @{}) 0
    Assert-True (((Invoke-TestGit $checkout @('rev-parse', 'HEAD')) -join '') -eq $head) 'No-change invocation created an empty commit.'
    Passed 'No-change invocation pushes existing history without an empty commit'

    Write-Fixture (Join-Path $checkout 'ten-api-full/api.txt') "still local`n"
    Assert-Exit (Invoke-Commit @{ PushOnly = $true; Remote = 'GitHub' }) 0
    Assert-True (((Invoke-TestGit $checkout @('rev-parse', 'HEAD')) -join '') -eq $head) 'PushOnly committed changes.'
    Assert-True (@(Invoke-TestGit $checkout @('diff', '--cached', '--name-only')).Count -eq 0) 'PushOnly staged changes.'
    Assert-True (@(Invoke-TestGit $checkout @('diff', '--name-only')).Count -gt 0) 'PushOnly discarded working changes.'
    Passed 'PushOnly leaves local changes and staging untouched'

    Write-Fixture (Join-Path $github 'hooks/pre-receive') "#!/bin/sh`nexit 1`n"
    Assert-Exit (Invoke-Commit @{ Message = 'test partial push' }) 1
    $head = (Invoke-TestGit $checkout @('rev-parse', 'HEAD')) -join ''
    Assert-True (((Invoke-TestGit $gitee @('rev-parse', 'main')) -join '') -eq $head) 'Successful destination was not updated.'
    Assert-True (((Invoke-TestGit $github @('rev-parse', 'main')) -join '') -ne $head) 'Rejecting destination unexpectedly updated.'
    Write-Fixture (Join-Path $github 'hooks/pre-receive') "#!/bin/sh`nexit 0`n"
    Assert-Exit (Invoke-Commit @{ PushOnly = $true; Remote = 'GitHub' }) 0
    Assert-True (((Invoke-TestGit $github @('rev-parse', 'main')) -join '') -eq $head) 'Targeted retry did not publish the existing commit.'
    Passed 'Partial failure preserves the commit and supports a targeted retry'

    Write-Host "$passed scenarios passed using $Shell. Fixtures: $fixtureRoot"
    exit 0
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Fixtures retained for inspection: $fixtureRoot"
    exit 1
}
