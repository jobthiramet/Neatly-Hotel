$ErrorActionPreference = "Stop"
$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
	throw "Missing server/.env - copy .env.example to .env and fill in Supabase values."
}

Get-Content $envFile | ForEach-Object {
	$line = $_.Trim()
	if ($line -eq "" -or $line.StartsWith("#")) {
		return
	}
	$idx = $line.IndexOf("=")
	if ($idx -lt 1) {
		return
	}
	$key = $line.Substring(0, $idx).Trim()
	$value = $line.Substring($idx + 1).Trim().Trim('"').Trim("'")
	Set-Item -Path "Env:$key" -Value $value
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
	$env:SPRING_PROFILES_ACTIVE = "supabase"
}

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
