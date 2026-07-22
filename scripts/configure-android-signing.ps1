param(
    [string]$KeytoolPath = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
    [string]$KeystorePath = "$env:USERPROFILE\.android\enlighten-upload-key.jks",
    [string]$PropertiesPath = "$env:USERPROFILE\.android\enlighten-signing.properties"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $KeytoolPath)) {
    throw "keytool was not found at $KeytoolPath"
}

if ((Test-Path -LiteralPath $KeystorePath) -or (Test-Path -LiteralPath $PropertiesPath)) {
    throw "Enlighten signing files already exist. Refusing to replace the Play upload key."
}

$signingDirectory = Split-Path -Parent $KeystorePath
New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null

function New-RandomPassword {
    $bytes = New-Object byte[] 32
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes).Replace("+", "-").Replace("/", "_").TrimEnd("=")
}

$storePassword = New-RandomPassword
$keyPassword = New-RandomPassword
$keyAlias = "enlighten-upload"

& $KeytoolPath `
    -genkeypair `
    -v `
    -storetype JKS `
    -keystore $KeystorePath `
    -alias $keyAlias `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000 `
    -storepass $storePassword `
    -keypass $keyPassword `
    -dname "CN=Mohammed Faziluddin, OU=Enlighten, O=Enlighten, C=CA"

if ($LASTEXITCODE -ne 0) {
    Remove-Item -LiteralPath $KeystorePath -Force -ErrorAction SilentlyContinue
    throw "keytool failed to create the Enlighten upload key."
}

$properties = @(
    "storeFile=$($KeystorePath.Replace('\', '/'))"
    "storePassword=$storePassword"
    "keyAlias=$keyAlias"
    "keyPassword=$keyPassword"
) -join "`n"

[IO.File]::WriteAllText(
    $PropertiesPath,
    "$properties`n",
    [Text.UTF8Encoding]::new($false)
)

$identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
foreach ($path in @($KeystorePath, $PropertiesPath)) {
    & icacls.exe $path /inheritance:r /grant:r "${identity}:(F)" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Could not restrict access to $path"
    }
}

Write-Output "Created the Enlighten upload key at $KeystorePath"
Write-Output "Created private Gradle signing properties at $PropertiesPath"
Write-Output "Back up both files securely. Losing them can block future Play Store updates."
