$path = "app/build/outputs/bundle/release/app-release.aab"
if (Test-Path $path) {
    $file = Get-Item $path
    Write-Output "File: $($file.FullName)"
    Write-Output "Size: $($file.Length) bytes"
} else {
    Write-Output "File not found at $path"
}
