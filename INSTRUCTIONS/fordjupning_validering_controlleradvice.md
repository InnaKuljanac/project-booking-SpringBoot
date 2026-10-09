# Validering och felhantering – @Valid och @ControllerAdvice

När en klient skickar felaktig data till ditt API ska du inte låta det krascha inne i service-lagret eller databasen. Det är controllerlagrets ansvar att stoppa dålig indata tidigt och returnera ett begripligt felsvar.

---

## Lägg till beroende

Validering i Spring Boot kräver `spring-boot-starter-validation` i `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Annotera DTOn

Valideringsregler sätts direkt på fälten i din DTO med annotations från `jakarta.validation.constraints`:

```java
public class CreateProductDto {

    @NotBlank(message = "Namnet får inte vara tomt")
    private String name;

    @Min(value = 0, message = "Priset måste vara noll eller högre")
    private double price;

    @Size(max = 500, message = "Beskrivningen får inte vara längre än 500 tecken")
    private String description;

    // getters och setters
}
```

### Vanliga valideringsannotations

| Annotation | Gäller | Vad den kontrollerar |
|---|---|---|
| `@NotNull` | Alla typer | Fältet är inte null |
| `@NotBlank` | String | Inte null, inte tom, inte bara whitespace |
| `@NotEmpty` | String, Collection | Inte null, inte tom |
| `@Size(min, max)` | String, Collection | Längd inom angivet intervall |
| `@Min(value)` | Tal | Värdet är minst `value` |
| `@Max(value)` | Tal | Värdet är högst `value` |
| `@Email` | String | Giltig e-postadress |
| `@Pattern(regexp)` | String | Matchar reguljärt uttryck |
| `@Positive` | Tal | Strikt positivt (> 0) |
| `@PositiveOrZero` | Tal | Noll eller positivt |

---

## Aktivera validering i controllern

Lägg till `@Valid` framför `@RequestBody` i controller-metoden. Det är allt som krävs för att Spring ska köra valideringen innan metoden exekveras:

```java
@PostMapping
public ResponseEntity<ProductDto> create(@Valid @RequestBody CreateProductDto dto) {
    return ResponseEntity.status(201).body(productService.create(dto));
}

@PutMapping("/{id}")
public ResponseEntity<ProductDto> update(
        @PathVariable Long id,
        @Valid @RequestBody CreateProductDto dto) {
    return ResponseEntity.ok(productService.update(id, dto));
}
```

Om valideringen misslyckas kastar Spring automatiskt ett `MethodArgumentNotValidException` – innan din kod ens körs. Utan en handler för det undantaget får klienten ett 400-svar med en lång och rörig JSON-struktur. Med en handler bestämmer du precis vad klienten ser.

---

## @ControllerAdvice – hantera fel globalt

`@ControllerAdvice` är en klass som fångar undantag från alla controllers i applikationen. Kombinera den med `@ExceptionHandler` för att bestämma vilket HTTP-svar varje undantagstyp ska ge.

### Enkelt felsvar

Det enklaste är att returnera felmeddelandet som en sträng:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(message);
    }
}
```

Svar vid ogiltig indata:
```
name: Namnet får inte vara tomt, price: Priset måste vara noll eller högre
```

Det är bättre än inget, men en klient som försöker parsa svaret har det svårt.

### Strukturerat felsvar

I praktiken vill du returnera JSON med ett konsekvent format så att klienten vet vad den kan förvänta sig. Skapa en klass för felsvaret:

```java
public class ErrorResponse {

    private int status;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

Och använd den i din handler:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse(404, e.getMessage()));
    }
}
```

Svar vid valideringsfel:
```json
{
  "status": 400,
  "message": "name: Namnet får inte vara tomt",
  "timestamp": "2025-03-15T14:22:10"
}
```

Svar vid resursen inte hittas:
```json
{
  "status": 404,
  "message": "Produkt med id 99 hittades inte",
  "timestamp": "2025-03-15T14:22:10"
}
```

---

## Fler feltyper att hantera

En produktionsklar handler brukar täcka mer än bara validering:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    // Valideringsfel – @Valid misslyckades
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(400, message));
    }

    // Resursen hittades inte – kastat från service-lagret
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(404, e.getMessage()));
    }

    // Felaktigt format på path-variabel, t.ex. /products/abc istället för /products/42
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "Ogiltigt värde för parametern: " + e.getName();
        return ResponseEntity.badRequest().body(new ErrorResponse(400, message));
    }

    // Catch-all för oväntade fel
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse(500, "Något gick fel"));
    }
}
```

Notera `catch-all`-handleern längst ner. Den fångar allt som inte matchas av en mer specifik handler. Klienten ser aldrig en stacktrace – bara ett generiskt 500-svar.

---

## Best practices

**Sätt alltid ett felmeddelande på dina valideringsannotations.** Defaultmeddelandena är på engelska och tekniska. `message = "Namnet får inte vara tomt"` är betydligt mer användbart för den som konsumerar ditt API.

**Returnera alla valideringsfel på en gång, inte bara det första.** `getFieldErrors().stream()...joining(", ")` samlar ihop alla fel i ett svar. Det är bättre UX än att klienten behöver skicka om requesten flera gånger för att hitta alla problem.

**Håll catch-all-svaret vagt.** `"Något gick fel"` avslöjar ingenting om systemets internals. Logga det faktiska undantaget i handleern, men skicka inte stacktrace eller interna felmeddelanden till klienten.

**Lägg `@ControllerAdvice` i ett eget paket.** En klass som `exception/GlobalExceptionHandler.java` är lätt att hitta och hör inte hemma i `controller/`.

**Använd `@Valid` konsekvent på alla endpoints som tar emot `@RequestBody`.** Det är lätt att glömma på PUT-endpoints – och då kan en uppdatering ta emot tomt namn utan att klaga.
