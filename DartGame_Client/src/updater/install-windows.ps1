param([string]$App, [string]$Archive, [int]$ProcessId)
$ErrorActionPreference = 'Stop'
$temporary = Split-Path -Parent $Archive
$unpacked = Join-Path $temporary 'unpacked'
$backup = "$App.update-backup-$PID"
$staging = "$App.update-new-$PID"
$backedUp = $false
$installed = $false
try {
    Wait-Process -Id $ProcessId -Timeout 60 -ErrorAction SilentlyContinue
    if (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) { throw 'The game did not exit in time.' }
    Expand-Archive -LiteralPath $Archive -DestinationPath $unpacked
    $new = Join-Path $unpacked 'DartsBattle'
    if (!(Test-Path -LiteralPath (Join-Path $new 'DartsBattle.exe'))) { throw 'Missing launcher in update.' }
    Copy-Item -LiteralPath $new -Destination $staging -Recurse
    Move-Item -LiteralPath $App -Destination $backup
    $backedUp = $true
    Move-Item -LiteralPath $staging -Destination $App
    $installed = $true
    Start-Process -FilePath (Join-Path $App 'DartsBattle.exe')
    try { Remove-Item -LiteralPath $backup -Recurse -Force } catch { Write-Warning "Could not remove backup: $_" }
} catch {
    $failure = $_
    Write-Error -ErrorAction Continue "Update failed: $failure"
    if ($backedUp) {
        try {
            if ($installed -and (Test-Path -LiteralPath $App)) { Remove-Item -LiteralPath $App -Recurse -Force }
            Move-Item -LiteralPath $backup -Destination $App
        } catch { Write-Error -ErrorAction Continue "Could not restore backup at ${backup}: $_" }
    }
    if (Test-Path -LiteralPath $staging) { Remove-Item -LiteralPath $staging -Recurse -Force -ErrorAction SilentlyContinue }
    try {
        Add-Type -AssemblyName System.Windows.Forms
        [System.Windows.Forms.MessageBox]::Show("Không thể cài bản cập nhật. Bản trước sẽ được mở lại nếu có thể. Chi tiết: $temporary\install.log", 'Darts Battle') | Out-Null
    } catch { Write-Warning $_ }
    if (!(Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) -and (Test-Path -LiteralPath (Join-Path $App 'DartsBattle.exe'))) {
        try { Start-Process -FilePath (Join-Path $App 'DartsBattle.exe') } catch { Write-Warning $_ }
    }
    exit 1
}
