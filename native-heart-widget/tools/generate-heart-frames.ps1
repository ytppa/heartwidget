$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $PSScriptRoot "..\app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$frames = @(
    @{ Name = "heart_00.png"; Scale = 1.00; Ghosts = @() },
    @{ Name = "heart_01.png"; Scale = 1.07; Ghosts = @(@{ Scale = 1.00; Alpha = 0.18 }, @{ Scale = 1.03; Alpha = 0.12 }) },
    @{ Name = "heart_02.png"; Scale = 1.16; Ghosts = @(@{ Scale = 1.06; Alpha = 0.16 }, @{ Scale = 1.11; Alpha = 0.10 }, @{ Scale = 1.20; Alpha = 0.08 }) },
    @{ Name = "heart_03.png"; Scale = 1.09; Ghosts = @(@{ Scale = 1.16; Alpha = 0.16 }, @{ Scale = 1.13; Alpha = 0.10 }) },
    @{ Name = "heart_04.png"; Scale = 0.94; Ghosts = @(@{ Scale = 1.08; Alpha = 0.14 }, @{ Scale = 1.00; Alpha = 0.10 }) },
    @{ Name = "heart_05.png"; Scale = 1.00; Ghosts = @(@{ Scale = 0.94; Alpha = 0.12 }, @{ Scale = 0.98; Alpha = 0.08 }) },
    @{ Name = "heart_06.png"; Scale = 1.10; Ghosts = @(@{ Scale = 1.00; Alpha = 0.14 }, @{ Scale = 1.05; Alpha = 0.09 }) },
    @{ Name = "heart_07.png"; Scale = 1.04; Ghosts = @(@{ Scale = 1.10; Alpha = 0.13 }, @{ Scale = 1.07; Alpha = 0.08 }) },
    @{ Name = "heart_08.png"; Scale = 0.98; Ghosts = @(@{ Scale = 1.04; Alpha = 0.10 }) },
    @{ Name = "heart_09.png"; Scale = 1.00; Ghosts = @() }
)

$baseScale = 0.82

function New-HeartPath {
    param([double] $Scale)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $points = @(
        [System.Drawing.PointF]::new(484, 902),
        [System.Drawing.PointF]::new(260, 698),
        [System.Drawing.PointF]::new(116, 566),
        [System.Drawing.PointF]::new(116, 372),
        [System.Drawing.PointF]::new(116, 250),
        [System.Drawing.PointF]::new(212, 152),
        [System.Drawing.PointF]::new(334, 152),
        [System.Drawing.PointF]::new(404, 152),
        [System.Drawing.PointF]::new(470, 186),
        [System.Drawing.PointF]::new(512, 240),
        [System.Drawing.PointF]::new(554, 186),
        [System.Drawing.PointF]::new(620, 152),
        [System.Drawing.PointF]::new(690, 152),
        [System.Drawing.PointF]::new(812, 152),
        [System.Drawing.PointF]::new(908, 250),
        [System.Drawing.PointF]::new(908, 372),
        [System.Drawing.PointF]::new(908, 566),
        [System.Drawing.PointF]::new(764, 698),
        [System.Drawing.PointF]::new(540, 902),
        [System.Drawing.PointF]::new(512, 928)
    )
    $types = [byte[]] @(0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 129)
    $path.AddPath((New-Object System.Drawing.Drawing2D.GraphicsPath($points, $types)), $false)

    $matrix = New-Object System.Drawing.Drawing2D.Matrix
    $matrix.Translate(-512, -540)
    $matrix.Scale([single] $Scale, [single] $Scale)
    $matrix.Translate(512, 540)
    $path.Transform($matrix)
    $matrix.Dispose()

    return $path
}

function Fill-GlassyHeart {
    param(
        [System.Drawing.Graphics] $Graphics,
        [System.Drawing.Drawing2D.GraphicsPath] $HeartPath
    )

    $bodyBounds = [System.Drawing.RectangleF]::new(96, 130, 832, 820)
    $bodyBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $bodyBounds,
        [System.Drawing.Color]::FromArgb(255, 255, 78, 108),
        [System.Drawing.Color]::FromArgb(255, 221, 25, 63),
        45
    )
    $Graphics.FillPath($bodyBrush, $HeartPath)
    $bodyBrush.Dispose()

    $previousClip = $Graphics.Clip
    $Graphics.SetClip($HeartPath)

    $glowBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(30, 255, 172, 186))
    $Graphics.FillEllipse($glowBrush, 230, 245, 385, 290)
    $glowBrush.Dispose()

    $glassPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $glassPath.StartFigure()
    $glassPath.AddBezier(
        [System.Drawing.PointF]::new(280, 218),
        [System.Drawing.PointF]::new(188, 232),
        [System.Drawing.PointF]::new(126, 324),
        [System.Drawing.PointF]::new(136, 432)
    )
    $glassPath.AddBezier(
        [System.Drawing.PointF]::new(136, 432),
        [System.Drawing.PointF]::new(206, 520),
        [System.Drawing.PointF]::new(345, 514),
        [System.Drawing.PointF]::new(425, 420)
    )
    $glassPath.AddBezier(
        [System.Drawing.PointF]::new(425, 420),
        [System.Drawing.PointF]::new(502, 326),
        [System.Drawing.PointF]::new(420, 202),
        [System.Drawing.PointF]::new(280, 218)
    )
    $glassPath.CloseFigure()

    $glassBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.RectangleF]::new(126, 206, 350, 320),
        [System.Drawing.Color]::FromArgb(92, 255, 250, 251),
        [System.Drawing.Color]::FromArgb(34, 176, 12, 48),
        42
    )
    $Graphics.FillPath($glassBrush, $glassPath)
    $glassBrush.Dispose()

    $topSpark = New-Object System.Drawing.Drawing2D.GraphicsPath
    $topSpark.AddEllipse(166, 158, 118, 70)
    $topSparkBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.RectangleF]::new(160, 154, 134, 82),
        [System.Drawing.Color]::FromArgb(150, 255, 255, 255),
        [System.Drawing.Color]::FromArgb(0, 255, 255, 255),
        32
    )
    $Graphics.FillPath($topSparkBrush, $topSpark)
    $topSparkBrush.Dispose()
    $topSpark.Dispose()

    $upperReflection = New-Object System.Drawing.Drawing2D.GraphicsPath
    $upperReflection.StartFigure()
    $upperReflection.AddBezier(
        [System.Drawing.PointF]::new(148, 236),
        [System.Drawing.PointF]::new(218, 178),
        [System.Drawing.PointF]::new(313, 180),
        [System.Drawing.PointF]::new(370, 240)
    )
    $upperReflection.AddBezier(
        [System.Drawing.PointF]::new(370, 240),
        [System.Drawing.PointF]::new(300, 220),
        [System.Drawing.PointF]::new(220, 250),
        [System.Drawing.PointF]::new(146, 330)
    )
    $upperReflection.CloseFigure()
    $upperReflectionBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(58, 255, 247, 249))
    $Graphics.FillPath($upperReflectionBrush, $upperReflection)
    $upperReflectionBrush.Dispose()
    $upperReflection.Dispose()

    $lowerReflection = New-Object System.Drawing.Drawing2D.GraphicsPath
    $lowerReflection.AddEllipse(222, 320, 205, 88)
    $lowerBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.RectangleF]::new(222, 320, 205, 88),
        [System.Drawing.Color]::FromArgb(0, 255, 255, 255),
        [System.Drawing.Color]::FromArgb(48, 255, 230, 236),
        90
    )
    $Graphics.FillPath($lowerBrush, $lowerReflection)
    $lowerBrush.Dispose()
    $lowerReflection.Dispose()

    $rightDepthBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(22, 124, 0, 30))
    $Graphics.FillEllipse($rightDepthBrush, 640, 342, 235, 300)
    $rightDepthBrush.Dispose()

    $rimPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(26, 255, 196, 205), 2.4)
    $Graphics.DrawPath($rimPen, $glassPath)
    $rimPen.Dispose()
    $glassPath.Dispose()

    $Graphics.Clip = $previousClip
}

function Draw-CenteredImage {
    param(
        [System.Drawing.Graphics] $Graphics,
        [System.Drawing.Bitmap] $Image,
        [double] $Scale,
        [double] $Alpha
    )

    $size = [int] [Math]::Round(1024 * $Scale)
    $offset = [int] [Math]::Round((1024 - $size) / 2)
    $destRect = [System.Drawing.Rectangle]::new($offset, $offset, $size, $size)

    if ($Alpha -ge 0.999) {
        $Graphics.DrawImage($Image, $destRect)
        return
    }

    $matrix = [System.Drawing.Imaging.ColorMatrix]::new()
    $matrix.Matrix00 = 1
    $matrix.Matrix11 = 1
    $matrix.Matrix22 = 1
    $matrix.Matrix33 = [single] $Alpha
    $matrix.Matrix44 = 1

    $attributes = [System.Drawing.Imaging.ImageAttributes]::new()
    $attributes.SetColorMatrix(
        $matrix,
        [System.Drawing.Imaging.ColorMatrixFlag]::Default,
        [System.Drawing.Imaging.ColorAdjustType]::Bitmap
    )

    $Graphics.DrawImage(
        $Image,
        $destRect,
        0,
        0,
        $Image.Width,
        $Image.Height,
        [System.Drawing.GraphicsUnit]::Pixel,
        $attributes
    )

    $attributes.Dispose()
}

$baseBitmap = New-Object System.Drawing.Bitmap 1024, 1024
$baseGraphics = [System.Drawing.Graphics]::FromImage($baseBitmap)
$baseGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$baseGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$baseGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$baseGraphics.Clear([System.Drawing.Color]::Transparent)
$baseHeartPath = New-HeartPath -Scale 1.0
Fill-GlassyHeart -Graphics $baseGraphics -HeartPath $baseHeartPath

foreach ($frame in $frames) {
    $bitmap = New-Object System.Drawing.Bitmap 1024, 1024
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.Clear([System.Drawing.Color]::Transparent)

    foreach ($ghost in $frame.Ghosts) {
        Draw-CenteredImage -Graphics $graphics -Image $baseBitmap -Scale ($baseScale * $ghost.Scale) -Alpha $ghost.Alpha
    }

    Draw-CenteredImage -Graphics $graphics -Image $baseBitmap -Scale ($baseScale * $frame.Scale) -Alpha 1.0

    $outputPath = Join-Path $outDir $frame.Name
    $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $graphics.Dispose()
    $bitmap.Dispose()
}

$baseHeartPath.Dispose()
$baseGraphics.Dispose()
$baseBitmap.Dispose()

Write-Output "Generated $($frames.Count) heart frames in $outDir"
