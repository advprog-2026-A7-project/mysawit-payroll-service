# mysawit-payroll-service

Spring Boot payroll microservice for MySawit.

## Scope

- Admin CRUD wage config for `BURUH`, `SUPIR`, and `MANDOR`.
- Formula payroll generation:
  - Buruh: `upahBuruhPerKg * kilogramPanen * 90%`
  - Supir: `upahSupirPerKg * kilogramDikirim * 90%`
  - Mandor: `upahMandorPerKg * kilogramDiakuiPabrik * 90%`
- RabbitMQ consumers for harvest, shipment, and user replica events.
- Event idempotency through unique role-scoped `eventId`.
- Payroll status flow: `PENDING`, `ACCEPTED`, `APPROVED`, `REJECTED`, `PAID`.
- Admin approve/reject, including mandatory rejection reason.
- Wallet with SawitDollar balance and Midtrans Snap sandbox top-up.
- Payroll approval transfers SawitDollar from admin wallet to worker wallet.

## Run Local

Set database environment variables first:

```powershell
$env:DB_URL="jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"
$env:DB_USERNAME="postgres.luymlqdyfvvrmnepsgsj"
$env:DB_PASSWORD="<your-db-password>"

.\gradlew.bat bootRun
```

For macOS/Linux, copy `.env.example` to `.env`, fill `DB_PASSWORD`, then run `bootRun`. You can also export the values manually:

```bash
export DB_URL='jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0'
export DB_USERNAME='postgres.luymlqdyfvvrmnepsgsj'
export DB_PASSWORD='<your-db-password>'
export MIDTRANS_SERVER_KEY='SB-Mid-server-<your-sandbox-server-key>'

./gradlew bootRun
```

Optional RabbitMQ environment variables:

```powershell
$env:RABBITMQ_DYNAMIC="false"
$env:RABBITMQ_MISSING_QUEUES_FATAL="false"
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="guest"
$env:RABBITMQ_PASSWORD="guest"
```

Runs at: `http://localhost:8085`

## Health

```text
GET /actuator/health
```

## Main Endpoints

Payroll:

```text
GET    /api/payrolls
GET    /api/payrolls?userId={id}&status=PENDING&from=2026-05-01T00:00:00&to=2026-05-31T23:59:59
GET    /api/payrolls/{id}
GET    /api/payrolls/user/{userId}
GET    /api/payrolls/status/{status}
POST   /api/payrolls
PUT    /api/payrolls/{id}
PATCH  /api/payrolls/{id}/accept
PATCH  /api/payrolls/{id}/approve
PATCH  /api/payrolls/{id}/reject
PATCH  /api/payrolls/{id}/pay
DELETE /api/payrolls/{id}
```

Wage config:

```text
GET    /api/admin/wage-configs
GET    /api/admin/wage-configs/{id}
GET    /api/admin/wage-configs/role/{role}
GET    /api/admin/wage-configs/role/{role}/active
POST   /api/admin/wage-configs
PATCH  /api/admin/wage-configs/{id}
DELETE /api/admin/wage-configs/{id}
```

Wallet:

```text
GET  /api/wallets/{userId}
GET  /api/wallets/{userId}/transactions
POST /api/wallets/{userId}/top-up/sandbox
POST /api/wallets/midtrans/webhook
POST /api/wallets/transactions/{transactionId}/settle-sandbox
```

Midtrans sandbox top-up body:

```json
{
  "amountSawitDollar": "100",
  "gateway": "MIDTRANS_SANDBOX"
}
```

`top-up/sandbox` creates a Midtrans Snap transaction and stores the local transaction as `PENDING`. The admin wallet balance is credited when Midtrans sends a webhook with `transaction_status: "settlement"` or `"capture"` to `/api/wallets/midtrans/webhook`. For local demos without a public webhook URL, call `/api/wallets/transactions/{transactionId}/settle-sandbox` to simulate the paid sandbox callback.

Approve payroll body:

```json
{
  "adminId": "admin"
}
```

Reject payroll body:

```json
{
  "reason": "Data panen belum valid"
}
```

## Tests

```powershell
.\gradlew.bat test
```

## Demo

See `DEMO_ASDOS.md` for a terminal demo script.
