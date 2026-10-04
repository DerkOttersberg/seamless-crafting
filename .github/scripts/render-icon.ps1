param(
    [string] $SvgPath,
    [string] $PngPath
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
# These icons intentionally use a small subset of SVG with a 64x64 viewBox.
# Keep the editable vector source beside the generated PNG.
$taskRepo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
if (-not $SvgPath) { $SvgPath = Join-Path $taskRepo 'common/src/main/resources/assets/seamless_crafting/icon.svg' }
if (-not $PngPath) { $PngPath = Join-Path $taskRepo 'common/src/main/resources/assets/seamless_crafting/icon.png' }
[xml] $taskSvg = Get-Content -Raw -LiteralPath $SvgPath
$taskBitmap = [System.Drawing.Bitmap]::new(256, 256)
$taskGraphics = [System.Drawing.Graphics]::FromImage($taskBitmap)
try {
    $taskGraphics.ScaleTransform(4, 4)
    $taskGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    foreach ($taskShape in $taskSvg.DocumentElement.ChildNodes) {
        if ($taskShape.NodeType -ne 'Element') { continue }
        $taskBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($taskShape.GetAttribute('fill')))
        try {
            if ($taskShape.LocalName -eq 'rect') {
                $taskGraphics.FillRectangle($taskBrush, [float]$taskShape.x, [float]$taskShape.y, [float]$taskShape.width, [float]$taskShape.height)
            } elseif ($taskShape.LocalName -eq 'polygon') {
                [System.Drawing.PointF[]] $taskPoints = $taskShape.points.Trim() -split '\s+' | ForEach-Object {
                    $taskPair = $_ -split ','
                    [System.Drawing.PointF]::new([float]$taskPair[0], [float]$taskPair[1])
                }
                $taskGraphics.FillPolygon($taskBrush, $taskPoints)
            }
        } finally { $taskBrush.Dispose() }
    }
    $taskBitmap.Save($PngPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "Generated $PngPath (256x256)"
} finally {
    $taskGraphics.Dispose()
    $taskBitmap.Dispose()
}
