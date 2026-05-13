param(
    [int]$DurationSec = 600,
    [int]$PollMs = 1000,
    [string]$LogDir = "$PSScriptRoot\logs",
    [string]$AdbPath = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-TraceLine {
    param(
        [string]$Type,
        [string]$Message
    )
    $line = "{0} [{1}] {2}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"), $Type, $Message
    $line | Out-File -FilePath $script:TextLog -Encoding UTF8 -Append
    Write-Host $line
}

function Get-ProcInfo {
    param([int]$ProcessId)
    $p = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId" -ErrorAction SilentlyContinue
    if (-not $p) { return $null }
    [pscustomobject]@{
        ProcessId      = $p.ProcessId
        Name           = $p.Name
        ParentProcessId= $p.ParentProcessId
        ExecutablePath = $p.ExecutablePath
        CommandLine    = $p.CommandLine
    }
}

function Add-EventRow {
    param(
        [string]$Kind,
        [string]$Trigger,
        [int]$ProcId,
        [string]$Name,
        [string]$Path,
        [string]$Cmd,
        [int]$ParentPid,
        [string]$ParentName,
        [string]$ParentPath,
        [string]$ParentCmd
    )
    $row = [pscustomobject]@{
        Time       = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
        Kind       = $Kind
        Trigger    = $Trigger
        Pid        = $ProcId
        Name       = $Name
        Path       = $Path
        Cmd        = $Cmd
        ParentPid  = $ParentPid
        ParentName = $ParentName
        ParentPath = $ParentPath
        ParentCmd  = $ParentCmd
    }
    $script:Rows.Add($row) | Out-Null
}

if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

$runId = Get-Date -Format "yyyyMMdd_HHmmss"
$TextLog = Join-Path $LogDir "adb-conflict-trace-$runId.log"
$CsvLog = Join-Path $LogDir "adb-conflict-trace-$runId.csv"
$Rows = New-Object System.Collections.Generic.List[object]

if ([string]::IsNullOrWhiteSpace($AdbPath)) {
    $adbProc = Get-Process adb -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($adbProc -and $adbProc.Path) {
        $AdbPath = $adbProc.Path
    } else {
        $sdkDefault = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
        if (Test-Path $sdkDefault) { $AdbPath = $sdkDefault }
    }
}

Write-TraceLine "INFO" "DurationSec=$DurationSec PollMs=$PollMs"
Write-TraceLine "INFO" "AdbPath=$AdbPath"
Write-TraceLine "INFO" "TextLog=$TextLog"
Write-TraceLine "INFO" "CsvLog=$CsvLog"
Write-TraceLine "INFO" "Hint: keep Android Studio and device operations running during capture."

$eventMode = $true
try {
    $startSub = Register-WmiEvent -Query "SELECT * FROM Win32_ProcessStartTrace" -SourceIdentifier "ADBTrace.Start"
    $stopSub = Register-WmiEvent -Query "SELECT * FROM Win32_ProcessStopTrace" -SourceIdentifier "ADBTrace.Stop"
    Write-TraceLine "INFO" "Mode=WMI event subscription"
}
catch {
    $eventMode = $false
    Write-TraceLine "WARN" ("WMI event subscription failed: {0}" -f $_.Exception.Message)
    Write-TraceLine "INFO" "Mode=Polling fallback (new process + port owner tracking)"
}

try {
    $begin = Get-Date
    $deadline = $begin.AddSeconds($DurationSec)
    $lastListenerPid = $null

    $seenProcIds = @{}
    foreach ($p in Get-Process -ErrorAction SilentlyContinue) { $seenProcIds[$p.Id] = $true }
    $seenAdbPids = @{}
    foreach ($p in Get-Process adb -ErrorAction SilentlyContinue) { $seenAdbPids[$p.Id] = $true }

    while ((Get-Date) -lt $deadline) {
        $listen = Get-NetTCPConnection -LocalPort 5037 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
        $currPid = if ($listen) { [int]$listen.OwningProcess } else { 0 }
        if ($currPid -ne $lastListenerPid) {
            $proc = if ($currPid -gt 0) { Get-ProcInfo -ProcessId $currPid } else { $null }
            $parent = if ($proc -and $proc.ParentProcessId -gt 0) { Get-ProcInfo -ProcessId $proc.ParentProcessId } else { $null }
            $procName = if ($proc) { $proc.Name } else { "" }
            $procPath = if ($proc) { $proc.ExecutablePath } else { "" }
            $procCmd = if ($proc) { $proc.CommandLine } else { "" }
            $parentPid = if ($proc) { [int]$proc.ParentProcessId } else { 0 }
            $parentName = if ($parent) { $parent.Name } else { "" }
            $parentPath = if ($parent) { $parent.ExecutablePath } else { "" }
            $parentCmd = if ($parent) { $parent.CommandLine } else { "" }
            Add-EventRow -Kind "PORT5037" -Trigger "ListenerPidChanged" `
                -ProcId $currPid -Name $procName -Path $procPath -Cmd $procCmd `
                -ParentPid $parentPid -ParentName $parentName -ParentPath $parentPath -ParentCmd $parentCmd
            Write-TraceLine "PORT5037" ("Listener PID {0} -> {1}; proc={2}; parent={3}" -f $lastListenerPid, $currPid, $procName, $parentName)
            $lastListenerPid = $currPid
        }

        if ($eventMode) {
            foreach ($evt in @(Get-Event -SourceIdentifier "ADBTrace.Start")) {
                $procId = [int]$evt.SourceEventArgs.NewEvent.ProcessId
                $name = [string]$evt.SourceEventArgs.NewEvent.ProcessName
                $proc = Get-ProcInfo -ProcessId $procId
                $cmd = if ($proc) { [string]$proc.CommandLine } else { "" }
                $path = if ($proc) { [string]$proc.ExecutablePath } else { "" }
                $ppid = if ($proc) { [int]$proc.ParentProcessId } else { 0 }
                $parent = if ($ppid -gt 0) { Get-ProcInfo -ProcessId $ppid } else { $null }

                $isAdb = $name -ieq "adb.exe"
                $isAdbCaller = $cmd -match "(^|[\s'`"])(adb|adb\.exe)([\s'`"]|$)"
                $isResetCmd = $cmd -match "kill-server|start-server|reconnect|usb"
                if ($isAdb -or $isAdbCaller -or $isResetCmd) {
                    $trigger = if ($isAdb) { "adb.exe_started" } elseif ($isResetCmd) { "adb_reset_like_cmd" } else { "adb_invoker_cmd" }
                    Add-EventRow -Kind "PROC_START" -Trigger $trigger `
                        -ProcId $procId -Name $name -Path $path -Cmd $cmd `
                        -ParentPid $ppid -ParentName $parent.Name -ParentPath $parent.ExecutablePath -ParentCmd $parent.CommandLine
                    Write-TraceLine "PROC_START" ("{0} pid={1} ppid={2} cmd={3}" -f $name, $procId, $ppid, $cmd)
                }
                Remove-Event -EventIdentifier $evt.EventIdentifier | Out-Null
            }

            foreach ($evt in @(Get-Event -SourceIdentifier "ADBTrace.Stop")) {
                $procId = [int]$evt.SourceEventArgs.NewEvent.ProcessId
                $name = [string]$evt.SourceEventArgs.NewEvent.ProcessName
                if ($name -ieq "adb.exe") {
                    Add-EventRow -Kind "PROC_STOP" -Trigger "adb.exe_stopped" `
                        -ProcId $procId -Name $name -Path "" -Cmd "" `
                        -ParentPid 0 -ParentName "" -ParentPath "" -ParentCmd ""
                    Write-TraceLine "PROC_STOP" ("adb.exe pid={0} exited" -f $procId)
                }
                Remove-Event -EventIdentifier $evt.EventIdentifier | Out-Null
            }
        } else {
            $current = @(Get-Process -ErrorAction SilentlyContinue)
            $currentIds = @{}
            foreach ($p in $current) { $currentIds[$p.Id] = $true }

            foreach ($p in $current) {
                if (-not $seenProcIds.ContainsKey($p.Id)) {
                    $proc = Get-ProcInfo -ProcessId $p.Id
                    if ($proc) {
                        $cmd = [string]$proc.CommandLine
                        $name = [string]$proc.Name
                        $isAdb = $name -ieq "adb.exe"
                        $isAdbCaller = $cmd -match "(^|[\s'`"])(adb|adb\.exe)([\s'`"]|$)"
                        $isResetCmd = $cmd -match "kill-server|start-server|reconnect|usb"
                        if ($isAdb -or $isAdbCaller -or $isResetCmd) {
                            $parent = if ($proc.ParentProcessId -gt 0) { Get-ProcInfo -ProcessId $proc.ParentProcessId } else { $null }
                            $trigger = if ($isAdb) { "adb.exe_started_poll" } elseif ($isResetCmd) { "adb_reset_like_cmd_poll" } else { "adb_invoker_cmd_poll" }
                            $parentName = if ($parent) { $parent.Name } else { "" }
                            $parentPath = if ($parent) { $parent.ExecutablePath } else { "" }
                            $parentCmd = if ($parent) { $parent.CommandLine } else { "" }
                            Add-EventRow -Kind "PROC_START" -Trigger $trigger `
                                -ProcId $proc.ProcessId -Name $proc.Name -Path $proc.ExecutablePath -Cmd $proc.CommandLine `
                                -ParentPid $proc.ParentProcessId -ParentName $parentName -ParentPath $parentPath -ParentCmd $parentCmd
                            Write-TraceLine "PROC_START" ("{0} pid={1} ppid={2} cmd={3}" -f $proc.Name, $proc.ProcessId, $proc.ParentProcessId, $proc.CommandLine)
                        }
                    }
                }
            }

            $adbNow = @(Get-Process adb -ErrorAction SilentlyContinue)
            $adbNowIds = @{}
            foreach ($ap in $adbNow) {
                $adbNowIds[$ap.Id] = $true
                if (-not $seenAdbPids.ContainsKey($ap.Id)) {
                    $proc = Get-ProcInfo -ProcessId $ap.Id
                    $parent = if ($proc -and $proc.ParentProcessId -gt 0) { Get-ProcInfo -ProcessId $proc.ParentProcessId } else { $null }
                    $adbPath = if ($proc) { $proc.ExecutablePath } else { "" }
                    $adbCmd = if ($proc) { $proc.CommandLine } else { "" }
                    $adbParentPid = if ($proc) { [int]$proc.ParentProcessId } else { 0 }
                    $adbParentName = if ($parent) { $parent.Name } else { "" }
                    $adbParentPath = if ($parent) { $parent.ExecutablePath } else { "" }
                    $adbParentCmd = if ($parent) { $parent.CommandLine } else { "" }
                    Add-EventRow -Kind "PROC_START" -Trigger "adb.exe_started_detected_by_scan" `
                        -ProcId $ap.Id -Name "adb.exe" -Path $adbPath -Cmd $adbCmd `
                        -ParentPid $adbParentPid -ParentName $adbParentName -ParentPath $adbParentPath -ParentCmd $adbParentCmd
                    Write-TraceLine "PROC_START" ("adb.exe pid={0} ppid={1} cmd={2}" -f $ap.Id, $adbParentPid, $adbCmd)
                }
            }
            foreach ($oldAdbPid in @($seenAdbPids.Keys)) {
                if (-not $adbNowIds.ContainsKey($oldAdbPid)) {
                    Add-EventRow -Kind "PROC_STOP" -Trigger "adb.exe_stopped_detected_by_scan" `
                        -ProcId ([int]$oldAdbPid) -Name "adb.exe" -Path "" -Cmd "" `
                        -ParentPid 0 -ParentName "" -ParentPath "" -ParentCmd ""
                    Write-TraceLine "PROC_STOP" ("adb.exe pid={0} exited" -f $oldAdbPid)
                }
            }

            $seenProcIds = $currentIds
            $seenAdbPids = $adbNowIds
        }

        Start-Sleep -Milliseconds $PollMs
    }
}
finally {
    Unregister-Event -SourceIdentifier "ADBTrace.Start" -ErrorAction SilentlyContinue
    Unregister-Event -SourceIdentifier "ADBTrace.Stop" -ErrorAction SilentlyContinue
    Get-Event -SourceIdentifier "ADBTrace.Start" -ErrorAction SilentlyContinue | Remove-Event -ErrorAction SilentlyContinue
    Get-Event -SourceIdentifier "ADBTrace.Stop" -ErrorAction SilentlyContinue | Remove-Event -ErrorAction SilentlyContinue
}

$Rows | Export-Csv -Path $CsvLog -NoTypeInformation -Encoding UTF8

Write-TraceLine "INFO" ("Capture done. EventRows={0}" -f $Rows.Count)
$rowsByTrigger = $Rows | Group-Object Trigger | Sort-Object Count -Descending
foreach ($g in $rowsByTrigger) {
    Write-TraceLine "SUMMARY" ("{0} => {1}" -f $g.Name, $g.Count)
}

Write-Host ""
Write-Host "Most useful rows for conflict source:"
$Rows |
    Where-Object { $_.Kind -in @("PROC_START","PORT5037") } |
    Select-Object -Last 20 |
    Format-Table Time,Kind,Trigger,Pid,Name,ParentPid,ParentName -AutoSize
