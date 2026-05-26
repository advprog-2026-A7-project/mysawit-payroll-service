# mysawit-payroll-service

Spring Boot (Java + Gradle) microservice for MySawit.

## Run (local)
```bash
./gradlew bootRun
```

Runs at: http://localhost:8085

Required environment:

```bash
DB_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
SAWIT_DOLLAR_RATE_IDR=10000
```

## Health
- GET /actuator/health

## Payroll and Wallet Flow

- `GET /api/payrolls?userId=&status=&from=&to=` lists payrolls with FE filters.
- `GET /api/payrolls/user/{userId}` returns payrolls for Identity UUID users.
- `PATCH /api/payrolls/{id}/approve` with `{ "adminId": "..." }` settles wallet balances and marks payroll `ACCEPTED`.
- `PATCH /api/payrolls/{id}/reject` requires `{ "reason": "..." }`.
- `GET /api/wallets/{userId}` returns or creates a wallet.
- `POST /api/wallets/{userId}/top-up/sandbox` creates a fake external sandbox payment transaction.
- `POST /api/wallets/transactions/{transactionId}/settle-sandbox` settles the fake external transaction and credits the wallet.

The sandbox top-up is the only fake boundary; payroll and wallet services still
run real internal business logic and persist real database state.
