# TBC 8606 Java server

Auth (`tbc-auth`) on TCP **3724** and world (`tbc-world`) on TCP **8085**. Spec: `../spec/`. Gates: `../spec/07-rebuild/test-plan.md`.

Requires **Java 21** and **Maven 3.9+**.

## Lab

1. MySQL with `tbcrealmd`, `tbcmangos`, `tbccharacters`, `tbclogs` (mangos-tbc SQL + `tbc-db/InstallFullDB.sh`).
2. Extract `DataDir` (`dbc/`, `maps/`) from a legal 2.4.3 client. Do not commit it.
3. Stop CMaNGOS if it holds the same ports.
4. Copy `conf/realmd.conf.dist` → `conf/realmd.conf` and `conf/mangosd.conf.dist` → `conf/mangosd.conf`. Connection string: `host;port;user;password;database`.
5. Client `realmlist.wtf`: `SET realmlist <auth-ip>`.

## Build and run

```
mvn -f tbc-server/pom.xml -q package
java -jar tbc-server/tbc-auth/target/tbc-auth-0.1.0-SNAPSHOT.jar tbc-server/conf/realmd.conf
java -jar tbc-server/tbc-world/target/tbc-world-0.1.0-SNAPSHOT.jar tbc-server/conf/mangosd.conf
```

Start MySQL, then auth, then world.

## Tests

```
mvn -f tbc-server/pom.xml -q test
```

JUnit covers `TP-INV` crypto/headers and SRP6 round-trip. Slice 4+ `method: client` P0 needs the official 8606 client.

## Current slice

See [progress.md](progress.md). One Cursor session per slice (`AGENTS.md`).
