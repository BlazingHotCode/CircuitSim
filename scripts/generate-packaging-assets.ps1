$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$packagingDir = Join-Path $rootDir 'packaging'

New-Item -ItemType Directory -Path $packagingDir -Force | Out-Null

Add-Type -AssemblyName System.Drawing

function New-ScaledBitmap {
    param(
        [System.Drawing.Bitmap]$SourceBitmap,
        [int]$Size
    )

    $scaledBitmap = New-Object System.Drawing.Bitmap($Size, $Size)
    $scaledGraphics = [System.Drawing.Graphics]::FromImage($scaledBitmap)
    $scaledGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $scaledGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $scaledGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $scaledGraphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $scaledGraphics.DrawImage($SourceBitmap, 0, 0, $Size, $Size)
    $scaledGraphics.Dispose()
    return $scaledBitmap
}

function Get-PngBytes {
    param([System.Drawing.Bitmap]$Bitmap)

    $memoryStream = New-Object System.IO.MemoryStream
    try {
        $Bitmap.Save($memoryStream, [System.Drawing.Imaging.ImageFormat]::Png)
        return $memoryStream.ToArray()
    } finally {
        $memoryStream.Dispose()
    }
}

function Write-MultiSizeIcon {
    param(
        [System.Drawing.Bitmap]$SourceBitmap,
        [string]$IconPath
    )

    $iconSizes = @(16, 24, 32, 48, 64, 128, 256)
    $images = @()
    foreach ($iconSize in $iconSizes) {
        $scaledBitmap = New-ScaledBitmap -SourceBitmap $SourceBitmap -Size $iconSize
        try {
            $imageBytes = [byte[]](Get-PngBytes -Bitmap $scaledBitmap)
            $images += [pscustomobject]@{
                Size = $iconSize
                Bytes = $imageBytes
            }
        } finally {
            $scaledBitmap.Dispose()
        }
    }

    $fileStream = [System.IO.File]::Create($IconPath)
    try {
        $writer = New-Object System.IO.BinaryWriter($fileStream)
        try {
            $writer.Write([UInt16]0)
            $writer.Write([UInt16]1)
            $writer.Write([UInt16]$images.Count)

            $offset = 6 + (16 * $images.Count)
            foreach ($image in $images) {
                $entrySize = if ($image.Size -ge 256) { 0 } else { $image.Size }
                $writer.Write([byte]$entrySize)
                $writer.Write([byte]$entrySize)
                $writer.Write([byte]0)
                $writer.Write([byte]0)
                $writer.Write([UInt16]1)
                $writer.Write([UInt16]32)
                $writer.Write([UInt32]$image.Bytes.Length)
                $writer.Write([UInt32]$offset)
            $offset += $image.Bytes.Length
            }

            foreach ($image in $images) {
                $writer.Write([byte[]]$image.Bytes)
            }
        } finally {
            $writer.Dispose()
        }
    } finally {
        $fileStream.Dispose()
    }
}

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
Write-MultiSizeIcon -SourceBitmap $bitmap -IconPath $icoPath

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
