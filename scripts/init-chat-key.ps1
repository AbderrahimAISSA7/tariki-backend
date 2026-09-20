$ErrorActionPreference = 'Stop'
$envPath = Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
if ((Test-Path -LiteralPath $envPath) -and (Select-String -LiteralPath $envPath -Pattern '^CHAT_ENCRYPTION_KEY=\S+' -Quiet)) {
    Write-Output 'La cle de messagerie existe deja. Elle est conservee.'
    exit 0
}
$bytes = New-Object byte[] 32
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
$line = "`nCHAT_ENCRYPTION_KEY=$([Convert]::ToBase64String($bytes))`n"
[System.IO.File]::AppendAllText($envPath, $line, [System.Text.UTF8Encoding]::new($false))
Write-Output 'Cle de messagerie creee dans .env (non versionne). Sauvegardez-la separement de la base.'
