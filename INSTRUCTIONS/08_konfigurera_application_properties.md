# Konfigurera application.properties

`application.properties` är Spring Boots konfigurationsfil och ligger i `src/main/resources/`. Här styr du allt från databasanslutning till hur Hibernate beter sig – utan att behöva röra någon Java-kod.

---

## Grundläggande struktur

Filen innehåller nyckel-värde-par på formen:

```properties
nyckel=värde
```

Spring Boot läser den automatiskt vid uppstart och konfigurerar applikationen därefter.

---

## H2 – databas för utveckling

H2 är en lättviktig databas som körs direkt i minnet. Den kräver ingen installation och är perfekt att använda under utveckling.

```properties
# Databasanslutning
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Aktivera H2-konsolen i webbläsaren
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

När applikationen körs kan du nå H2-konsolen på `http://localhost:8080/h2-console`. Logga in med JDBC URL `jdbc:h2:mem:testdb` och användarnamn `sa`, lösenord tomt.

---

## JPA och Hibernate

```properties
# Dialekt – talar om för Hibernate vilket SQL-format som ska användas
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Hur Hibernate hanterar databasschemat vid uppstart
spring.jpa.hibernate.ddl-auto=create-drop

# Visa genererad SQL i terminalen (bra för felsökning)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Värden för ddl-auto

| Värde | Vad som händer |
|---|---|
| `create` | Skapar om schemat vid varje uppstart, raderar befintlig data |
| `create-drop` | Skapar vid uppstart, droppar när applikationen stängs |
| `update` | Försöker uppdatera schemat utan att radera data |
| `validate` | Kontrollerar att entiteterna matchar databasen, ändrar ingenting |
| `none` | Gör ingenting |

Under utveckling med H2 är `create-drop` bekvämt – databasen återställs automatiskt varje gång du startar om. När du senare arbetar mot en riktig databas och har designat schemat manuellt använder du `validate` för att bekräfta att dina entiteter stämmer överens med tabellerna.

---

## Applikationsinställningar

```properties
# Porten applikationen körs på (standard är 8080)
server.port=8080

# Namn på applikationen
spring.application.name=mitt-projekt
```

---

## Komplett exempel

Här är en fullständig `application.properties` för ett nytt Spring Boot-projekt med H2:

```properties
spring.application.name=mitt-projekt

# H2-databas
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2-konsol
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## Nästa steg

När projektet växer och du byter till en riktig databas som MySQL eller PostgreSQL behöver du byta ut datasource-inställningarna och lägga till rätt JDBC-driver i `pom.xml`. Det är en av de få sakerna som faktiskt skiljer sig åt mellan databaser – resten av koden förblir oförändrad.
