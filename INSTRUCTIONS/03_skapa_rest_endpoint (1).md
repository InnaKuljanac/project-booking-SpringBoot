# Skapa REST-endpoints

En REST-endpoint i Spring Boot är en metod i en controller-klass som svarar på ett specifikt HTTP-anrop. Spring kopplar ihop URL, HTTP-metod och din Java-metod automatiskt via annotations.

---

## Controller-klassen

```java
package se.dittnamn.mittprojekt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
}
```

`@RestController` talar om för Spring att det här är en HTTP-controller som returnerar JSON.
`@RequestMapping("/products")` sätter bas-URL för alla endpoints i klassen.

---

## HTTP-metoder

### GET – hämta data

```java
// Hämta alla
@GetMapping
public ResponseEntity<List<ProductDto>> getAll() {
    return ResponseEntity.ok(productService.getAll());
}

// Hämta en specifik
@GetMapping("/{id}")
public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(productService.getById(id));
}
```

### POST – skapa ny resurs

```java
@PostMapping
public ResponseEntity<ProductDto> create(@RequestBody ProductDto dto) {
    return ResponseEntity.status(201).body(productService.create(dto));
}
```

### PUT – uppdatera befintlig resurs

```java
@PutMapping("/{id}")
public ResponseEntity<ProductDto> update(
        @PathVariable Long id,
        @RequestBody ProductDto dto) {
    return ResponseEntity.ok(productService.update(id, dto));
}
```

### DELETE – ta bort resurs

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
}
```

---

## Annotations

### @PathVariable

Extraherar ett värde ur URL:en. `{id}` i path-mönstret mappas till parametern:

```java
@GetMapping("/{id}")
public ResponseEntity<ProductDto> getById(@PathVariable Long id) { ... }
// GET /products/42  →  id = 42
```

### @RequestBody

Läser JSON från request body och konverterar till ett Java-objekt. Jackson sköter konverteringen:

```java
@PostMapping
public ResponseEntity<ProductDto> create(@RequestBody ProductDto dto) { ... }
// Body: {"name": "Stol", "price": 599.0}  →  dto.getName() = "Stol"
```

### @RequestParam

Extraherar query-parametrar från URL:en:

```java
@GetMapping
public ResponseEntity<List<ProductDto>> getAll(
        @RequestParam(required = false) String category) { ... }
// GET /products?category=möbler  →  category = "möbler"
// GET /products  →  category = null
```

---

## ResponseEntity

`ResponseEntity` ger dig full kontroll över HTTP-responsen – statuskod, headers och body.

```java
// 200 OK med body
ResponseEntity.ok(dto)

// 201 Created med body
ResponseEntity.status(201).body(dto)

// 204 No Content utan body
ResponseEntity.noContent().build()

// 404 Not Found utan body
ResponseEntity.notFound().build()
```

### Vanliga statuskoder

| Kod | Betydelse | Används vid |
|---|---|---|
| 200 OK | Lyckad förfrågan | GET, PUT |
| 201 Created | Resurs skapad | POST |
| 204 No Content | Lyckad, inget att returnera | DELETE |
| 400 Bad Request | Felaktig indata | Valideringsfel |
| 404 Not Found | Resursen finns inte | GET/PUT/DELETE med okänt id |

---

## Komplett exempel

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@RequestBody ProductDto dto) {
        return ResponseEntity.status(201).body(productService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> update(
            @PathVariable Long id,
            @RequestBody ProductDto dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Testa med Postman

Skapa en ny request i Postman och ange URL och metod. För POST och PUT behöver du sätta body:

1. Välj **Body** → **raw** → **JSON**
2. Skriv in ditt JSON-objekt
3. Skicka

Spring svarar med statuskod och body som du kan inspektera direkt i Postman.
