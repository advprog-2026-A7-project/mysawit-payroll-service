# Demo Asdos - MySawit Payroll Service

Dokumen ini dipakai sebagai script demonstrasi terminal untuk membuktikan fitur payroll service berjalan dari sisi build, API, data payroll, status approval/rejection, dan event-driven payroll generation.

## 1. Identitas

Nama Kelompok: A7

Anggota:

- Muhammad Hadziqul Falah Teguh / 2406437432
- Muhammad Hamiz Ghani Ayusha / 2406360413
- Nadia Aisyah Fazila / 2406495584
- Made Shandy Krisnanda / 2406495615

Repository:

- 25% milestone: https://github.com/advprog-2026-A7-project/mysawit-payroll-service/tree/feat-ghani/wage-config-payroll-status
- 50% milestone: https://github.com/advprog-2026-A7-project/mysawit-payroll-service/tree/feature/payroll-otomatis-event
- 75% milestone/latest service: https://github.com/advprog-2026-A7-project/mysawit-payroll-service

Deployment:

- Backend AWS: http://18.205.109.188:8085
- Frontend Vercel: https://mysawit-web.vercel.app/

## 2. Fitur yang Didemokan

Milestone 25%:

- Admin dapat CRUD konfigurasi upah per kg untuk `BURUH`, `SUPIR`, dan `MANDOR`.
- Entity payroll memiliki status dan dapat dilihat per user.

Milestone 50%:

- Consumer/worker menangani event panen dan shipment.
- Event menghasilkan payroll otomatis.
- Idempotency dasar: event dengan `eventId` sama tidak membuat payroll ganda.
- Slip/payroll membawa detail user dari `UserReplica`, yang diisi dari event `user.registered`.

Milestone 75%:

- Admin dapat approve/reject payroll.
- Reject menyimpan alasan pada field `notes`.
- Service dapat menandai payroll sebagai `PAID` dan menyimpan `paymentMethod` serta `paymentDate`.
- Wallet SawitDollar tersedia untuk setiap user.
- Admin dapat top up wallet lewat endpoint sandbox.
- Saat payroll disetujui, saldo wallet admin berkurang dan saldo wallet penerima bertambah sebesar nilai payroll.

## 3. Pre-demo Checklist

Jalankan dari folder service:

```bat
cd C:\UNI\adpro-group\mysawit-payroll-service
git status --short --branch
gradlew.bat test
```

Expected:

- Branch sesuai demo, misalnya `feature/payroll-otomatis-event` untuk milestone 50%.
- `gradlew.bat test` selesai dengan `BUILD SUCCESSFUL`.

Cek frontend:

```powershell
Invoke-WebRequest -Uri "https://mysawit-web.vercel.app/" -UseBasicParsing
```

Expected:

- HTTP status `200`.

Cek backend AWS:

```powershell
Test-NetConnection -ComputerName 18.205.109.188 -Port 8085
Invoke-WebRequest -Uri "http://18.205.109.188:8085/actuator/health" -UseBasicParsing -TimeoutSec 20
```

Expected:

- `TcpTestSucceeded : True`.
- `/actuator/health` mengembalikan JSON health. Jika timeout, kemungkinan service AWS sedang belum responsif walaupun port terbuka. Untuk demo paling aman, jalankan local service.

## 4. Menjalankan Service Local

Di `cmd.exe`, set environment variable database:

```bat
set "DB_URL=jdbc:postgresql://db.luymlqdyfvvrmnepsgsj.supabase.co:5432/postgres?sslmode=require"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=<ISI_PASSWORD_DB>"

gradlew.bat bootRun
```

Jika menggunakan PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://db.luymlqdyfvvrmnepsgsj.supabase.co:5432/postgres?sslmode=require"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="<ISI_PASSWORD_DB>"

.\gradlew.bat bootRun
```

Service berjalan di:

```text
http://localhost:8085
```

Buka terminal kedua untuk menjalankan request demo.

Set base URL di PowerShell:

```powershell
$BASE_URL = "http://localhost:8085"
```

Untuk demo deployment AWS, ganti menjadi:

```powershell
$BASE_URL = "http://18.205.109.188:8085"
```

## 5. Demo Health Check

Command:

```powershell
Invoke-RestMethod "$BASE_URL/actuator/health"
```

Expected:

```json
{
  "status": "UP"
}
```

Narasi:

Service Spring Boot berhasil jalan dan actuator health endpoint aktif.

## 6. Demo Wage Config CRUD

### 6.1 List Semua Wage Config

Command:

```powershell
Invoke-RestMethod "$BASE_URL/api/admin/wage-configs"
```

Expected:

- Muncul konfigurasi awal untuk `BURUH`, `SUPIR`, dan `MANDOR`.
- Seed default:
  - `BURUH`: 350/kg
  - `SUPIR`: 250/kg
  - `MANDOR`: 150/kg

### 6.2 Create Wage Config Baru

Command:

```powershell
$wage = @{
  roleType = "BURUH"
  ratePerKg = 425
  effectiveDate = "2026-05-19"
  description = "Demo upah buruh untuk asdos"
  createdBy = "admin-demo"
} | ConvertTo-Json

$createdWage = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE_URL/api/admin/wage-configs" `
  -ContentType "application/json" `
  -Body $wage

$createdWage
$WAGE_ID = $createdWage.id
```

Expected:

- Response HTTP `201 Created`.
- Response memiliki `id`.
- `roleType` tersimpan uppercase sebagai `BURUH`.

### 6.3 Read Wage Config by ID

Command:

```powershell
Invoke-RestMethod "$BASE_URL/api/admin/wage-configs/$WAGE_ID"
```

Expected:

- Data config yang baru dibuat muncul.

### 6.4 Update Wage Config

Command:

```powershell
$wageUpdate = @{
  ratePerKg = 450
  description = "Demo update upah buruh untuk asdos"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Patch `
  -Uri "$BASE_URL/api/admin/wage-configs/$WAGE_ID" `
  -ContentType "application/json" `
  -Body $wageUpdate
```

Expected:

- `ratePerKg` berubah menjadi `450`.
- `description` berubah sesuai input.

### 6.5 Filter Active Config by Role

Command:

```powershell
Invoke-RestMethod "$BASE_URL/api/admin/wage-configs/role/BURUH/active"
```

Expected:

- Mengembalikan konfigurasi aktif untuk role `BURUH`.
- Config aktif dipilih berdasarkan `effectiveDate` yang sudah berlaku.

### 6.6 Delete Wage Config Demo

Command:

```powershell
Invoke-RestMethod `
  -Method Delete `
  -Uri "$BASE_URL/api/admin/wage-configs/$WAGE_ID"
```

Expected:

- HTTP `204 No Content`.

Validasi sudah terhapus:

```powershell
try {
  Invoke-RestMethod "$BASE_URL/api/admin/wage-configs/$WAGE_ID"
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

Expected:

```text
404
```

## 7. Demo Payroll Manual + Status Flow

### 7.1 Create Payroll PENDING

Command:

```powershell
$demoUserId = "demo-user-asdos-001"

$payroll = @{
  userId = $demoUserId
  periodStart = "2026-05-01T00:00:00"
  periodEnd = "2026-05-19T23:59:00"
  baseAmount = 1500000
  bonusAmount = 200000
  deductionAmount = 50000
  status = "PENDING"
  notes = "Payroll demo manual untuk asdos"
} | ConvertTo-Json

$createdPayroll = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE_URL/api/payrolls" `
  -ContentType "application/json" `
  -Body $payroll

$createdPayroll
$PAYROLL_ID = $createdPayroll.id
```

Expected:

- Response HTTP `201 Created`.
- `status` bernilai `PENDING`.
- `totalAmount` dihitung otomatis:

```text
1500000 + 200000 - 50000 = 1650000
```

### 7.2 List Payroll per User

Command:

```powershell
Invoke-RestMethod "$BASE_URL/api/payrolls/user/$demoUserId"
```

Expected:

- Payroll demo user muncul.

Narasi:

Ini membuktikan fitur list payroll per user.

### 7.3 Approve Payroll

Pastikan wallet admin punya saldo. Untuk demo, top up sandbox dulu:

```powershell
$topUpBody = @{
  amountSawitDollar = "5000000"
  gateway = "MIDTRANS_SANDBOX"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE_URL/api/wallets/admin/top-up/sandbox" `
  -ContentType "application/json" `
  -Body $topUpBody
```

Command:

```powershell
$approveBody = @{
  adminId = "admin"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Patch `
  -Uri "$BASE_URL/api/payrolls/$PAYROLL_ID/approve" `
  -ContentType "application/json" `
  -Body $approveBody
```

Expected:

- `status` berubah menjadi `APPROVED`.
- `walletSettled` menjadi `true`.
- Wallet penerima bertambah dan wallet admin berkurang sesuai `totalAmount`.

Catatan:

- Endpoint `approve` langsung mengubah status ke `APPROVED`.
- Endpoint `accept` hanya valid untuk payroll yang masih `PENDING`.

### 7.4 Create Payroll Kedua untuk Demo Reject

Command:

```powershell
$rejectPayrollBody = @{
  userId = $demoUserId
  periodStart = "2026-05-20T00:00:00"
  periodEnd = "2026-05-20T23:59:00"
  baseAmount = 800000
  bonusAmount = 0
  deductionAmount = 0
  status = "PENDING"
  notes = "Payroll demo reject"
} | ConvertTo-Json

$rejectPayroll = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE_URL/api/payrolls" `
  -ContentType "application/json" `
  -Body $rejectPayrollBody

$REJECT_PAYROLL_ID = $rejectPayroll.id
$rejectPayroll
```

Expected:

- Payroll baru berstatus `PENDING`.

### 7.5 Reject Payroll dengan Alasan

Command:

```powershell
$rejectBody = @{
  reason = "Data panen belum lengkap"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Patch `
  -Uri "$BASE_URL/api/payrolls/$REJECT_PAYROLL_ID/reject" `
  -ContentType "application/json" `
  -Body $rejectBody
```

Expected:

- `status` berubah menjadi `REJECTED`.
- `notes` berisi alasan reject: `Data panen belum lengkap`.

### 7.6 Filter Payroll by Status

Command:

```powershell
Invoke-RestMethod "$BASE_URL/api/payrolls/status/APPROVED"
Invoke-RestMethod "$BASE_URL/api/payrolls/status/REJECTED"
```

Expected:

- Payroll yang sudah di-approve muncul pada list `APPROVED`.
- Payroll yang sudah di-reject muncul pada list `REJECTED`.

## 8. Demo Payment Status

Untuk membuktikan service menyimpan status pembayaran dan wallet sandbox, buat payroll baru, approve, lalu tandai `PAID`.

### 8.1 Create Payroll untuk Payment

Command:

```powershell
$paymentPayrollBody = @{
  userId = $demoUserId
  periodStart = "2026-05-21T00:00:00"
  periodEnd = "2026-05-21T23:59:00"
  baseAmount = 900000
  bonusAmount = 100000
  deductionAmount = 0
  status = "PENDING"
  notes = "Payroll demo payment"
} | ConvertTo-Json

$paymentPayroll = Invoke-RestMethod `
  -Method Post `
  -Uri "$BASE_URL/api/payrolls" `
  -ContentType "application/json" `
  -Body $paymentPayrollBody

$PAYMENT_PAYROLL_ID = $paymentPayroll.id
$paymentPayroll
```

### 8.2 Mark as Paid

Approve dulu payroll payment demo:

```powershell
Invoke-RestMethod `
  -Method Patch `
  -Uri "$BASE_URL/api/payrolls/$PAYMENT_PAYROLL_ID/approve" `
  -ContentType "application/json" `
  -Body $approveBody
```

Command:

```powershell
$paymentBody = @{
  paymentMethod = "MIDTRANS_SANDBOX"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Patch `
  -Uri "$BASE_URL/api/payrolls/$PAYMENT_PAYROLL_ID/pay" `
  -ContentType "application/json" `
  -Body $paymentBody
```

Expected:

- `status` berubah menjadi `PAID`.
- `paymentMethod` tersimpan sebagai `MIDTRANS_SANDBOX`.
- `paymentDate` terisi timestamp saat request.

Narasi:

Service menyimpan status pembayaran payroll dan menyediakan top-up wallet sandbox sebagai simulasi Payment Gateway.

## 9. Demo Event-driven Payroll Generation

Event consumer memakai RabbitMQ queue:

- Harvest: `payroll_queue`
- Shipment: `shipment.completed`
- User registered replica: `user.registered.queue`

Kode consumer:

- `handleHarvestEvent` memproses `HarvestEvent`.
- `handleShipmentEvent` memproses `ShipmentEvent`.
- `handleUserRegisteredEvent` menyimpan `UserReplica`.

Payload event payroll:

```json
{
  "eventId": "harvest-demo-001",
  "employeeId": "demo-user-asdos-001",
  "amount": 1250000,
  "timestamp": 1779148800000
}
```

Payload user replica:

```json
{
  "userId": "demo-user-asdos-001",
  "email": "demo.asdos@mysawit.local",
  "role": "BURUH",
  "username": "Demo Asdos"
}
```

### 9.1 Bukti via Unit Test

Command:

```bat
gradlew.bat test --tests "com.mysawit.payroll.event.PayrollEventConsumerImplTest" --tests "com.mysawit.payroll.service.PayrollServiceTest"
```

Expected:

- Test consumer membuktikan listener mendelegasikan event ke service.
- Test service membuktikan event baru menyimpan payroll.
- Test service membuktikan event duplicate dengan `eventId` sama tidak menyimpan payroll lagi.

Narasi:

Idempotency dilakukan dengan mengecek `payrollRepository.findByEventId(eventId)`. Jika event sudah pernah diproses, service langsung return dan tidak membuat payroll baru.

### 9.2 Bukti via RabbitMQ Manual

Jika RabbitMQ lokal tersedia, jalankan RabbitMQ lalu start service dengan env RabbitMQ default:

```bat
set "RABBITMQ_HOST=localhost"
set "RABBITMQ_PORT=5672"
set "RABBITMQ_USERNAME=guest"
set "RABBITMQ_PASSWORD=guest"
gradlew.bat bootRun
```

Publish event via RabbitMQ Management API:

```powershell
$rabbitAuth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("guest:guest"))
$headers = @{ Authorization = "Basic $rabbitAuth" }

$userEvent = @{
  properties = @{}
  routing_key = "user.registered.queue"
  payload_encoding = "string"
  payload = (@{
    userId = "demo-user-asdos-001"
    email = "demo.asdos@mysawit.local"
    role = "BURUH"
    username = "Demo Asdos"
  } | ConvertTo-Json -Compress)
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:15672/api/exchanges/%2F/amq.default/publish" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $userEvent
```

Publish harvest event:

```powershell
$eventId = "harvest-demo-" + (Get-Date -Format "yyyyMMddHHmmss")

$harvestEvent = @{
  properties = @{}
  routing_key = "payroll_queue"
  payload_encoding = "string"
  payload = (@{
    eventId = $eventId
    employeeId = "demo-user-asdos-001"
    amount = 1250000
    timestamp = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
  } | ConvertTo-Json -Compress)
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:15672/api/exchanges/%2F/amq.default/publish" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $harvestEvent
```

Validasi payroll tercipta:

```powershell
Invoke-RestMethod "$BASE_URL/api/payrolls/user/demo-user-asdos-001"
```

Expected:

- Ada payroll baru dengan:
  - `eventId` sesuai event.
  - `status` `PENDING`.
  - `baseAmount` `1250000`.
  - `notes` mengandung `Generated from HarvestEvent` dan nama user jika `UserReplica` sudah diproses.

Publish event yang sama sekali lagi:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:15672/api/exchanges/%2F/amq.default/publish" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $harvestEvent
```

Validasi idempotency:

```powershell
$all = Invoke-RestMethod "$BASE_URL/api/payrolls/user/demo-user-asdos-001"
$all | Where-Object { $_.eventId -eq $eventId }
```

Expected:

- Hanya ada satu payroll untuk `eventId` tersebut.

## 10. Cleanup Data Demo

Jika ingin menghapus payroll demo yang dibuat manual:

```powershell
Invoke-RestMethod -Method Delete -Uri "$BASE_URL/api/payrolls/$PAYROLL_ID"
Invoke-RestMethod -Method Delete -Uri "$BASE_URL/api/payrolls/$REJECT_PAYROLL_ID"
Invoke-RestMethod -Method Delete -Uri "$BASE_URL/api/payrolls/$PAYMENT_PAYROLL_ID"
```

Jika ada ID yang sudah tidak ada, response `404` tidak masalah untuk cleanup.

## 11. Ringkasan Narasi Demo

Urutan bicara saat demo:

1. Tunjukkan repository dan branch.
2. Jalankan `gradlew.bat test` untuk membuktikan automated test pass.
3. Jalankan service local dengan env database.
4. Cek `/actuator/health`.
5. Demo CRUD wage config.
6. Demo create payroll manual dan list payroll per user.
7. Demo approve dan reject payroll, termasuk alasan reject.
8. Demo mark payroll as paid.
9. Demo event-driven melalui unit test, atau RabbitMQ manual jika RabbitMQ tersedia.
10. Jelaskan idempotency: event duplicate dicegah oleh field unik `eventId` dan pengecekan `findByEventId`.

## 12. Hasil Verifikasi Terakhir dari Workspace Ini

Tanggal verifikasi: 2026-05-19.

- `git fetch --all --prune`: sukses.
- Branch lokal aktif: `feature/payroll-otomatis-event`.
- `gradlew.bat test`: exit code 0.
- Frontend `https://mysawit-web.vercel.app/`: HTTP 200.
- Backend AWS `18.205.109.188:8085`: TCP port 8085 terbuka, tetapi request HTTP ke `/actuator/health` dan `/api/admin/wage-configs` timeout dari mesin ini.
- Search branch remote: endpoint pembayaran payroll tersedia lewat `PATCH /api/payrolls/{id}/pay`, sementara top-up wallet memakai Midtrans Snap sandbox.
