# Smart Wallet Application

A server-rendered wallet service built with Spring Boot. Users hold multiple
currency wallets, transfer funds to each other, and move between subscription
tiers that are charged against a wallet balance.

**[Try the demo](https://phoenixmaster123.github.io/smart-wallet-application/app/)**
&middot;
[Documentation](https://phoenixmaster123.github.io/smart-wallet-application/)

The demo is the application's own templates and stylesheets running against a
seeded registry in the browser, so transfers, top-ups and subscription upgrades
all work with no backend behind them. State lives in `sessionStorage`.

## Requirements

- JDK 17
- MySQL on `localhost:3306` (the `dev` profile creates the schema on first run)

The Maven wrapper is checked in, so no local Maven install is needed.

## Running

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080` and seeds the default user configured
in `src/main/resources/application.properties`.

### Without a database

To boot against an in-memory H2 instead — the same thing CI does:

```bash
./mvnw clean package
java -jar target/smart-wallet-application-*.jar \
  --spring.datasource.url='jdbc:h2:mem:local;DB_CLOSE_DELAY=-1' \
  --spring.datasource.driverClassName=org.h2.Driver \
  --spring.datasource.username=sa \
  --spring.datasource.password= \
  --spring.jpa.hibernate.ddl-auto=create-drop
```

## Profiles

`dev` is active by default. Deploy with `--spring.profiles.active=prod` and
supply `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` as environment variables.

## Quality gates

```bash
./mvnw checkstyle:check   # style
./mvnw clean verify       # build and test
```

Both run in CI on every push and pull request against `master`, followed by a
smoke test that boots the packaged jar. Checkstyle rules live in
`config/checkstyle/checkstyle.xml`.

## Layout

Packages are organised by feature rather than by layer.

| Package | Holds |
| --- | --- |
| `app.user`, `app.wallet`, `app.transaction`, `app.subscription` | Model, repository and service per feature |
| `app.web` | Controllers, DTOs and mappers |
| `app.security` | `UserDetails` implementation |
| `app.notification`, `app.init` | OpenFeign clients for other services |
| `app.event`, `app.email`, `app.gift` | Application events and their listeners |
