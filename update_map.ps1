$path = "C:\Users\ADMIN\AndroidStudioProjects\SnowTrail\app\src\main\java\mx\utng\snowtrail\MainActivity.kt"
$content = [System.IO.File]::ReadAllText($path)
$target = "(?s)onGPSMoved = \{ sliderValue ->\s+val basePositions = mapOf\([^)]+\)"
$replacement = @"
onGPSMoved = { sliderValue ->
                                                 val basePositions = mapOf(
                                                     "nev_los_abuelos" to 80.0,
                                                     "nev_la_mich" to 350.0,
                                                     "nev_zero" to 1200.0,
                                                     "nev_artis" to 2900.0,
                                                     "nev_far" to 4500.0,
                                                     "nev_centenario" to 2800.0,
                                                     "nev_gelato" to 3800.0,
                                                     "nev_antonio" to 1800.0,
                                                     "nev_copo" to 2600.0,
                                                     "nev_flor" to 8000.0
                                                 )
"@
$newContent = $content -replace $target, $replacement
[System.IO.File]::WriteAllText($path, $newContent)
Write-Output "Successfully updated map!"
