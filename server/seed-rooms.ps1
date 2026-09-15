param(
	# Base URL of a running Neatly API (start it with run-supabase.ps1 or mvnw spring-boot:run).
	[string]$ApiBase = "http://localhost:8080/api"
)

# Seeds the rooms in seed/rooms.json through the API's own POST /api/rooms, so images land in the
# room-images bucket exactly like admin uploads. Safe to re-run: rooms whose name already exists are skipped.
# Needs curl (built into Windows 10+, macOS and Linux). On macOS/Linux run: pwsh ./seed-rooms.ps1
$ErrorActionPreference = "Stop"
$seedDir = Join-Path $PSScriptRoot "seed"
$imageDir = Join-Path $seedDir "rooms"
$rooms = (Get-Content (Join-Path $seedDir "rooms.json") -Raw | ConvertFrom-Json).rooms
$curl = if ($env:OS -eq "Windows_NT") { "curl.exe" } else { "curl" }

# Create in reverse so the list (newest first) shows the rooms in the file's order.
[array]::Reverse($rooms)

foreach ($room in $rooms) {
	$search = [uri]::EscapeDataString($room.name)
	$existing = (Invoke-RestMethod "$ApiBase/rooms?search=$search&size=50").data.content
	if ($existing | Where-Object { $_.name -eq $room.name }) {
		Write-Host "skip    $($room.name) (already exists)"
		continue
	}

	$fields = $room | Select-Object name, bedType, sizeSqm, capacity, pricePerNight, promotionPrice, description, amenities
	$jsonFile = New-TemporaryFile
	$responseFile = New-TemporaryFile
	try {
		# UTF-8 without BOM: the server's JSON parser rejects a BOM.
		[IO.File]::WriteAllText($jsonFile.FullName, ($fields | ConvertTo-Json -Compress -Depth 3), (New-Object Text.UTF8Encoding $false))

		$form = @("-F", "room=@$($jsonFile.FullName);type=application/json",
			"-F", "mainImage=@$(Join-Path $imageDir $room.mainImage);type=image/webp")
		foreach ($image in $room.gallery) {
			$form += @("-F", "gallery=@$(Join-Path $imageDir $image);type=image/webp")
		}

		$status = & $curl -sS -o $responseFile.FullName -w "%{http_code}" @form "$ApiBase/rooms"
		$body = Get-Content $responseFile.FullName -Raw
	}
	finally {
		Remove-Item $jsonFile, $responseFile -ErrorAction SilentlyContinue
	}

	if ($status -eq "201") {
		Write-Host "created $($room.name)"
	}
	elseif ($status -eq "503") {
		Write-Warning "Storage is not configured on this server (e.g. local profile), so rooms with images can't be created. Local rooms come from db/local_rooms_seed.sql."
		exit 0
	}
	else {
		throw "Creating '$($room.name)' failed with HTTP ${status}: $body"
	}
}
