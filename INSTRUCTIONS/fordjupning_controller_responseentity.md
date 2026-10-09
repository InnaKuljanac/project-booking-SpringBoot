# Fördjupning – Controller-lagret och ResponseEntity

I introduktionsguiden såg du hur man skapar grundläggande endpoints. Den här guiden går djupare – hur controllern egentligen är tänkt att fungera, vad `ResponseEntity` faktiskt ger dig, och hur du hanterar fel på ett vettigt sätt.

---

## Vad controllern ska göra – och inte göra

En controller har ett enda ansvar: ta emot HTTP-anrop och skicka tillbaka ett HTTP-svar. Den ska inte innehålla affärslogik, databasanrop eller beslut om vad som ska hända med data. Det är service-lagrets jobb.

En välskriven controller-metod är nästan alltid ett eller två rader:

```java
@GetMapping("/{id}")
public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(productService.getById(id));
}
```

Om du skriver mer kod än så i en controller-metod är det ofta ett tecken på att något borde ligga i service-lagret istället.

---

## ResponseEntity i detalj

`ResponseEntity<T>` representerar hela HTTP-svaret – statuskod, headers och body. Utan det returnerar Spring alltid 200 OK, vilket inte alltid är korrekt.

### Bygga ett svar steg för steg

```java
// Kortform – 200 OK med body
return ResponseEntity.ok(dto);

// Längre form – samma sak
return ResponseEntity.status(HttpStatus.OK).body(dto);

// 201 Created – används alltid efter POST
return ResponseEntity.status(HttpStatus.CREATED).body(dto);

// 204 No Content – ingen body, används efter DELETE
return ResponseEntity.noContent().build();

// 404 Not Found – ingen body
return ResponseEntity.notFound().build();
```

### Lägga till headers

Du kan lägga till valfria HTTP-headers i svaret. Ett vanligt mönster efter POST är att returnera en `Location`-header som pekar på den nyskapade resursen:

```java
@PostMapping
public ResponseEntity<ProductDto> create(@RequestBody ProductDto dto) {
    ProductDto created = productService.create(dto);
    URI location = URI.create("/products/" + created.getId());
    return ResponseEntity.created(location).body(created);
}
```

`ResponseEntity.created(uri)` sätter automatiskt statuskod 201 och lägger till `Location`-headern. Det är vad HTTP-standarden föreskriver för POST-svar.

---

## Felhantering i controllern

Det vanligaste misstaget är att låta undantag bubbla upp okontrollerat. Då får klienten ett 500 Internal Server Error med en lång Java-stacktrace – det berättar ingenting vettigt för den som anropar API:et.

### Hantera fel lokalt

Du kan fånga undantag direkt i metoden:

```java
@GetMapping("/{id}")
public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(productService.getById(id));
    } catch (RuntimeException e) {
        return ResponseEntity.notFound().build();
    }
}
```

Det fungerar, men det blir repetitivt om du har många endpoints. Det finns ett bättre sätt.

### @ExceptionHandler – hantera fel på ett ställe

Med `@ExceptionHandler` kan du definiera hur specifika undantag ska hanteras, en gång per controller:

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
}
```

Nu kan `getById` kasta ett undantag utan att du behöver skriva try-catch överallt. `@ExceptionHandler` fångar det och returnerar ett läsbart 404-svar med felmeddelandet som body.

### @ControllerAdvice – global felhantering

Om du vill att samma felhantering ska gälla för alla controllers kan du skapa en separat klass med `@ControllerAdvice`:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
}
```

Den här klassen fångar undantag från alla controllers i applikationen. Det håller varje controller ren och felhanteringen samlad på ett ställe.

---

## @RequestParam – frivilliga och obligatoriska parametrar

Query-parametrar används ofta för filtrering och sökning. Standardbeteendet är att parametern är obligatorisk – saknas den returnerar Spring 400 Bad Request automatiskt.

```java
// Obligatorisk – anrop utan ?status= ger 400
@GetMapping
public ResponseEntity<List<ProductDto>> getByStatus(@RequestParam String status) {
    return ResponseEntity.ok(productService.getByStatus(status));
}
```

Gör den frivillig med `required = false`, och sätt ett standardvärde med `defaultValue`:

```java
// Frivillig med standardvärde
@GetMapping
public ResponseEntity<List<ProductDto>> getAll(
        @RequestParam(required = false, defaultValue = "all") String status) {
    return ResponseEntity.ok(productService.getByStatus(status));
}
```

---

## Kombinera path-variabler och query-parametrar

Det är vanligt att kombinera `@PathVariable` och `@RequestParam` i samma metod. Till exempel hämta alla beställningar för en specifik användare, med möjlighet att filtrera på status:

```java
// GET /users/42/orders?status=pending
@GetMapping("/users/{userId}/orders")
public ResponseEntity<List<OrderDto>> getOrders(
        @PathVariable Long userId,
        @RequestParam(required = false) String status) {
    return ResponseEntity.ok(orderService.getOrders(userId, status));
}
```

---

## Checklista för en välskriven controller

- Varje metod delegerar till service-lagret – ingen affärslogik i controllern
- POST returnerar 201, DELETE returnerar 204, GET och PUT returnerar 200
- Fel hanteras med `@ExceptionHandler` eller `@ControllerAdvice` – inga nakna undantag bubblar upp
- `@RequestParam` är markerade som `required = false` eller `defaultValue` när de är frivilliga
- Metodnamnen beskriver vad de gör – `getById`, `create`, `update`, `delete`
