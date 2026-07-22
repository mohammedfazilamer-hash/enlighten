$ErrorActionPreference = "Stop"

$ruleName = "Enlighten local AI and voice"
$existingRule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue

if ($existingRule) {
    $existingRule | Set-NetFirewallRule `
        -Enabled True `
        -Profile Private,Public `
        -Direction Inbound `
        -Action Allow
} else {
    New-NetFirewallRule `
        -DisplayName $ruleName `
        -Description "Allow Enlighten from devices on the local network only." `
        -Direction Inbound `
        -Action Allow `
        -Protocol TCP `
        -LocalPort 11434,11435 `
        -Profile Private,Public `
        -RemoteAddress LocalSubnet | Out-Null
}

Write-Host "Enlighten firewall access is enabled for the local network."
