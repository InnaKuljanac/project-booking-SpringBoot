# Skapa ett Spring Boot-projekt

Spring Boot-projekt skapas enklast via Spring Initializr – ett webbverktyg som genererar ett färdigt projekt med de beroenden du väljer.

---

## Spring Initializr

Gå till [start.spring.io](https://start.spring.io) och fyll i följande:

| Fält | Värde |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | Välj senaste stabila versionen |
| Group | t.ex. `se.dittnamn` |
| Artifact | t.ex. `mitt-projekt` |
| Packaging | Jar |
| Java | 17 eller högre |

### Beroenden att lägga till

Klicka på **Add Dependencies** och lägg till:

- `Spring Web` – för REST-endpoints
- `Spring Data JPA` – för databashantering
- `H2 Database` – för lokal utvecklingsdatabas

Klicka **Generate** för att ladda ner projektet som en zip-fil.

---

## Importera i IntelliJ

1. Packa upp zip-filen
2. Öppna IntelliJ och välj **File → Open**
3. Navigera till den uppackade mappen och öppna den
4. IntelliJ identifierar det som ett Maven-projekt och laddar ner beroenden automatiskt – det kan ta en liten stund första gången

---

## Projektstruktur

När projektet är inläst ser strukturen ut så här:

```
src/
├── main/
│   ├── java/
│   │   └── se/dittnamn/mittprojekt/
│   │       └── MittProjektApplication.java   ← startpunkt
│   └── resources/
│       └── application.properties            ← konfiguration
└── test/
    └── java/
        └── se/dittnamn/mittprojekt/
            └── MittProjektApplicationTests.java
pom.xml                                        ← beroenden
```

Du kommer skapa dina egna klasser under `src/main/java/se/dittnamn/mittprojekt/`. Ett vanligt sätt att organisera dem är i underpaket efter lager:

```
se/dittnamn/mittprojekt/
├── controller/
├── service/
├── repository/
├── model/
└── dto/
```

---

## Starta applikationen

Öppna `MittProjektApplication.java` – det är här Spring Boot startas:

```java
@SpringBootApplication
public class MittProjektApplication {
    public static void main(String[] args) {
        SpringApplication.run(MittProjektApplication.class, args);
    }
}
```

Klicka på den gröna pilen bredvid `main`-metoden för att starta. I terminalen ser du loggar och till sist:

```
Tomcat started on port 8080
Started MittProjektApplication in 2.3 seconds
```

Applikationen körs nu på `http://localhost:8080`. Eftersom vi inte har skapat några endpoints ännu ger alla anrop ett 404-svar – det är förväntat.

---

## Nästa steg

Konfigurera `application.properties` för att koppla upp mot H2-databasen, skapa dina första entiteter och börja bygga endpoints.
