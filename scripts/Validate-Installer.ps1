param(
    [string]$Installer = ".\release\DerMegaMecha-Launcher-Setup.exe"
)

$resolved = (Resolve-Path $Installer).Path
$bytes = [System.IO.File]::ReadAllBytes($resolved)
if ($bytes.Length -lt 1024) { throw "Installer is too small" }
if ($bytes[0] -ne 0x4D -or $bytes[1] -ne 0x5A) { throw "Installer does not have an MZ executable header" }

$signature = Get-AuthenticodeSignature $resolved
$properties = [System.Diagnostics.FileVersionInfo]::GetVersionInfo($resolved)
$hash = Get-FileHash $resolved -Algorithm SHA256

[pscustomobject]@{
    Path = $resolved
    Length = $bytes.Length
    SHA256 = $hash.Hash
    AuthenticodeStatus = $signature.Status.ToString()
    ProductName = $properties.ProductName
    FileDescription = $properties.FileDescription
} | ConvertTo-Json
