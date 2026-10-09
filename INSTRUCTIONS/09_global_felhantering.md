# Global felhantering

När något går fel i ditt API – en resurs hittas inte, indata är ogiltig, något oväntat inträffar – vill du returnera ett strukturerat och begripligt felmeddelande istället för ett generiskt 500-svar. Det sköter du med `@ControllerAdvice`.

---

## Vad är @ControllerAdvice?

`@ControllerAdvice` är en klass som fångar undantag från alla controllers i applikationen. Istället för att hantera fel i varje controller för sig samlar du all felhantering på ett ställe.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    // exception-hanterare här
}
```

---

## Skapa en felklass

Börja med en enkel klass som representerar ett felmeddelande:

```java
public class ErrorResponse {

    private int status;
    private String message;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
}
```

---

## Hantera vanliga fel

### Resurs hittades inte

Skapa ett eget undantag:

```java
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

Kasta det i service-lagret:

```java
public BookDto getById(Long id) {
    Book book = bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bok med id " + id + " hittades inte"));
    return toDto(book);
}
```

Fånga det i `GlobalExceptionHandler`:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(404, ex.getMessage());
    return ResponseEntity.status(404).body(error);
}
```

### Valideringsfel

När `@Valid` misslyckas kastas ett `MethodArgumentNotValidException`. Fånga det och samla ihop alla felmeddelanden:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

    ErrorResponse error = new ErrorResponse(400, message);
    return ResponseEntity.status(400).body(error);
}
```

### Generellt fallback

Fånga alla oväntade fel med en generell hanterare:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    ErrorResponse error = new ErrorResponse(500, "Något gick fel");
    return ResponseEntity.status(500).body(error);
}
```

---

## Komplett exempel

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(new ErrorResponse(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(500).body(new ErrorResponse(500, "Något gick fel"));
    }
}
```

---

## Vad klienten får

Ett anrop mot `GET /books/999` där boken inte finns returnerar nu:

```json
{
  "status": 404,
  "message": "Bok med id 999 hittades inte"
}
```

Ett POST-anrop med ogiltig data returnerar:

```json
{
  "status": 400,
  "message": "title: Titel får inte vara tom, price: Pris måste anges"
}
```

Istället för ett generiskt 500-svar med en lång Java-stacktrace.
