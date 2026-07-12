$services = & "F:\Android\Sdk\platform-tools\adb.exe" mdns services
$matched = $services | Select-String -Pattern "10.54.217.229:(\d+)"
$port = "41271" # Last known connect port as fallback

if ($matched) {
    $port = $matched.Matches[0].Groups[1].Value
    Write-Host "Discovered connect port via mDNS: $port"
    & "F:\Android\Sdk\platform-tools\adb.exe" connect "10.54.217.229:$port"
} else {
    Write-Host "mDNS discovery failed. Trying last known port: $port"
    & "F:\Android\Sdk\platform-tools\adb.exe" connect "10.54.217.229:$port"
}

Write-Host "Running Gradle clean & assembleDebug (warning-free parameters)..."
& ".\gradlew.bat" clean assembleDebug --no-configuration-cache --no-build-cache

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build succeeded, installing..."
    # Get active devices to confirm connection
    $devices = & "F:\Android\Sdk\platform-tools\adb.exe" devices
    if ($devices -match "10.54.217.229:$port") {
        & "F:\Android\Sdk\platform-tools\adb.exe" -s "10.54.217.229:$port" install -r "f:\AndroidStudioProjects\PresentMate\app\build\outputs\apk\debug\app-debug.apk"
    } else {
        # Fallback to single active connection
        & "F:\Android\Sdk\platform-tools\adb.exe" install -r "f:\AndroidStudioProjects\PresentMate\app\build\outputs\apk\debug\app-debug.apk"
    }
} else {
    Write-Host "Build failed with exit code $LASTEXITCODE"
}
