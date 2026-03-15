$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$packagingDir = Join-Path $rootDir 'packaging'

New-Item -ItemType Directory -Path $packagingDir -Force | Out-Null

Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class NativeIcon {
    [DllImport("user32.dll")]
    public static extern bool DestroyIcon(IntPtr handle);
}
"@

$size = 256
$bitmap = New-Object System.Drawing.Bitmap($size, $size)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

$backgroundRect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
$backgroundBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $backgroundRect,
    [System.Drawing.Color]::FromArgb(35, 67, 126),
    [System.Drawing.Color]::FromArgb(18, 157, 133),
    45
)
$graphics.FillRectangle($backgroundBrush, $backgroundRect)

$gridPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(55, 255, 255, 255), 3)
foreach ($line in 48, 96, 144, 192) {
    $graphics.DrawLine($gridPen, $line, 36, $line, 220)
    $graphics.DrawLine($gridPen, 36, $line, 220, $line)
}

$tracePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 245, 191, 66), 10)
$tracePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$tracePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$graphics.DrawLine($tracePen, 64, 86, 128, 86)
$graphics.DrawLine($tracePen, 128, 86, 128, 158)
$graphics.DrawLine($tracePen, 128, 158, 194, 158)

$nodeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 244, 214))
foreach ($point in @(@(64, 86), @(128, 86), @(128, 158), @(194, 158))) {
    $graphics.FillEllipse($nodeBrush, $point[0] - 8, $point[1] - 8, 16, 16)
}

$font = New-Object System.Drawing.Font('Segoe UI', 64, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(70, 0, 0, 0))
$textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$stringFormat = New-Object System.Drawing.StringFormat
$stringFormat.Alignment = [System.Drawing.StringAlignment]::Center
$stringFormat.LineAlignment = [System.Drawing.StringAlignment]::Center
$textRect = New-Object System.Drawing.RectangleF(0, 120, $size, 104)
$shadowRect = New-Object System.Drawing.RectangleF(4, 124, $size, 104)
$graphics.DrawString('CS', $font, $shadowBrush, $shadowRect, $stringFormat)
$graphics.DrawString('CS', $font, $textBrush, $textRect, $stringFormat)

$pngPath = Join-Path $packagingDir 'circuitsim.png'
$icoPath = Join-Path $packagingDir 'circuitsim.ico'

$bitmap.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)

$iconHandle = $bitmap.GetHicon()
try {
    $icon = [System.Drawing.Icon]::FromHandle($iconHandle)
    try {
        $stream = [System.IO.File]::Create($icoPath)
        try {
            $icon.Save($stream)
        } finally {
            $stream.Dispose()
        }
    } finally {
        $icon.Dispose()
    }
} finally {
    [NativeIcon]::DestroyIcon($iconHandle) | Out-Null
}

$stringFormat.Dispose()
$textBrush.Dispose()
$shadowBrush.Dispose()
$font.Dispose()
$nodeBrush.Dispose()
$tracePen.Dispose()
$gridPen.Dispose()
$backgroundBrush.Dispose()
$graphics.Dispose()
$bitmap.Dispose()

Write-Host "Generated packaging assets in $packagingDir"
