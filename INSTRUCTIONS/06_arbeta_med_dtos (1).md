# Arbeta med DTOs

DTO står för Data Transfer Object. Det är enkla klasser vars enda syfte är att flytta data mellan lager – i det här fallet mellan service-lagret och controllern, och vidare ut i API-responsen.

---

## Varför inte exponera entiteter direkt?

Det kan verka onödigt att skapa en separat klass som ser nästan likadan ut som entiteten. Men det finns tydliga skäl:

**Säkerhet** – en entitet kan innehålla fält du inte vill skicka ut, till exempel lösenordshashar eller interna flaggor. Med en DTO bestämmer du exakt vad som är synligt.

**Serialisering** – entiteter med relationer kan orsaka cirkulära references när Jackson försöker konvertera dem till JSON. DTOs har inga sådana beroenden.

**Flexibilitet** – API:ets form behöver inte matcha databasens form. Du kan slå ihop fält, döpa om dem, eller lägga till beräknade värden i DTOn utan att röra entiteten.

**Stabilitet** – om du ändrar databasen och entiteten påverkas inte nödvändigtvis det externa API:et, och vice versa.

---

## En enkel DTO

```java
package se.dittnamn.mittprojekt.dto;

public class ProductDto {

    private Long id;
    private String name;
    private double price;

    public ProductDto() {}

    public ProductDto(Long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
```

Lägg märke till att `id` saknar setter – det är ett medvetet val. En klient ska inte kunna skicka in ett id vid skapande, det genereras av databasen.

---

## Mappning i service-lagret

Mappningen mellan entitet och DTO sker i service-lagret. Det är viktigt att den sker där och inte i controllern, eftersom service-lagret håller en aktiv databassession via `@Transactional`.

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> getAll() {
        return productRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produkt med id " + id + " hittades inte"));
        return toDto(product);
    }

    public ProductDto create(ProductDto dto) {
        Product product = new Product(dto.getName(), dto.getPrice());
        Product saved = productRepository.save(product);
        return toDto(saved);
    }

    // Privat hjälpmetod för mappning
    private ProductDto toDto(Product product) {
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getPrice()
        );
    }
}
```

---

## Request-DTO vs response-DTO

Ibland är det meningsfullt att ha separata DTOs för inkommande och utgående data. Till exempel kan en `CreateProductDto` sakna `id`-fältet helt eftersom det inte finns ännu, medan `ProductDto` alltid har ett `id`.

```java
// Används som @RequestBody vid POST
public class CreateProductDto {
    private String name;
    private double price;
    // ingen id
}

// Används som response
public class ProductDto {
    private Long id;
    private String name;
    private double price;
}
```

Det är ett mönster du kan välja att använda när det känns motiverat – för enklare projekt räcker det ofta med en enda DTO-klass.

---

## Flödet i korthet

```
HTTP Request (JSON)
      ↓
  @RequestBody → DTO        ← Jackson konverterar JSON till DTO
      ↓
  Controller → Service
      ↓
  Service mappar DTO → Entitet
      ↓
  Repository sparar Entitet
      ↓
  Service mappar Entitet → DTO
      ↓
  Controller returnerar DTO
      ↓
HTTP Response (JSON)        ← Jackson konverterar DTO till JSON
```

Entiteten lämnar aldrig service-lagret. Det som kommer in och ut ur API:et är alltid DTOs.
