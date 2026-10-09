# Byta databas – från H2 till MySQL eller PostgreSQL

H2 är praktiskt för att komma igång snabbt, men när du ska bygga ett riktigt projekt vill du använda en riktig databas. Bytet är enklare än det låter – det är i princip bara `pom.xml` och `application.properties` som behöver ändras.

---

## MySQL

### 1. Byt beroende i pom.xml

Ta bort H2 och lägg till MySQL-drivern:

```xml
<!-- Ta bort detta -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lägg till detta -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. Uppdatera application.properties

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mitt_projekt
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=ditt_lösenord

spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### 3. Skapa databasen i MySQL Workbench

Spring skapar tabellerna åt dig, men databasen måste finnas. Kör detta i Workbench:

```sql
CREATE DATABASE mitt_projekt;
```

---

## PostgreSQL

### 1. Byt beroende i pom.xml

```xml
<!-- Ta bort H2, lägg till detta -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. Uppdatera application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mitt_projekt
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=ditt_lösenord

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### 3. Skapa databasen

```sql
CREATE DATABASE mitt_projekt;
```

---

## ddl-auto vid byte av databas

Med H2 använde vi `create-drop` eftersom databasen ändå försvinner vid omstart. Med en riktig databas vill du inte att Hibernate ska radera och återskapa tabellerna varje gång.

Använd `update` under utveckling – Hibernate lägger till nya kolumner och tabeller om de saknas, men raderar inte befintlig data.

| Värde | Används när |
|---|---|
| `create-drop` | H2 under tidig utveckling |
| `update` | Riktig databas under utveckling |
| `validate` | Produktion eller när schemat är designat manuellt |
| `none` | Du hanterar schemat helt själv |

---

## data.sql fungerar inte längre

Med H2 körde Spring automatiskt `data.sql` för testdata. Med MySQL eller PostgreSQL fungerar inte det på samma sätt direkt – antingen lägger du in testdata manuellt i databasen, eller så skapar du en `@Component` som seedar data vid uppstart:

```java
@Component
public class DataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;

    public DataSeeder(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            bookRepository.save(new Book("Pippi Långstrump"));
            bookRepository.save(new Book("Emil i Lönneberga"));
        }
    }
}
```

`CommandLineRunner` körs automatiskt när applikationen startar. Villkoret `count() == 0` säkerställer att datan bara läggs in en gång.

---

## Nästa steg

När databasen är på plats och applikationen startar utan fel kan du verifiera att tabellerna skapades korrekt i Workbench eller psql, och börja bygga ditt projekt.
