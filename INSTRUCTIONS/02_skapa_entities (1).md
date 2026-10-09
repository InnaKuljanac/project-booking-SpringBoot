# Skapa entiteter

En entitet är en Java-klass som mappas mot en databastabell. Hibernate läser dina entiteter och skapar tabellerna åt dig – eller validerar att de matchar ett befintligt schema.

---

## En enkel entitet

```java
package se.dittnamn.mittprojekt.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private double price;

    // Konstruktorer
    public Product() {}

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    // Getters och setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
```

---

## Annotations

### @Entity
Markerar klassen som en JPA-entitet. Hibernate skapar en tabell för den.

### @Table
Valfri. Används för att ange ett specifikt tabellnamn. Utan den används klassnamnet (i lowercase beroende på dialect).

```java
@Table(name = "products")
```

### @Id
Markerar vilket fält som är primärnyckel. Varje entitet måste ha exakt ett `@Id`-fält.

### @GeneratedValue
Talar om hur primärnyckeln genereras. `GenerationType.IDENTITY` innebär att databasen sköter det med auto-increment.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

### @Column
Valfri på varje fält – Hibernate mappar fält till kolumner automatiskt. Använd den när du vill styra kolumnens egenskaper:

```java
@Column(nullable = false)           // NOT NULL i databasen
@Column(unique = true)              // unik kolumn
@Column(name = "product_name")      // specifikt kolumnnamn
@Column(length = 500)               // maxlängd för String
```

---

## Namnkonventioner

Hibernate konverterar automatiskt `camelCase` i Java till `snake_case` i databasen. Fältet `firstName` i Java blir kolumnen `first_name` i databasen. Du behöver inte använda `@Column(name = ...)` om du följer den konventionen.

---

## Tom konstruktor

JPA kräver alltid en tom konstruktor (utan argument). Hibernate använder den när den läser data från databasen och skapar Java-objekt. Den kan vara `protected` om du inte vill att den ska användas direkt i koden:

```java
protected Product() {}
```

---

## Vanliga fälttyper

| Java-typ | Kolumntyp i databasen |
|---|---|
| `String` | VARCHAR |
| `int` / `Integer` | INTEGER |
| `long` / `Long` | BIGINT |
| `double` / `Double` | DOUBLE |
| `boolean` / `Boolean` | BOOLEAN |
| `LocalDate` | DATE |
| `LocalDateTime` | TIMESTAMP |

---

## Skapa ett repository

När entiteten är skapad behöver du ett repository för att kunna läsa och skriva till databasen:

```java
package se.dittnamn.mittprojekt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.dittnamn.mittprojekt.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

`JpaRepository<Product, Long>` tar två typparametrar – entitetsklassen och typen för primärnyckeln. Du får direkt tillgång till metoder som `findAll()`, `findById()`, `save()` och `deleteById()` utan att skriva någon kod.

---

## Nästa steg

Med entitet och repository på plats kan du injicera repositoryt i ett service-lager och börja exponera data via REST-endpoints.
