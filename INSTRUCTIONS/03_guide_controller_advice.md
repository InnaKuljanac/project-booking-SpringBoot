# Global felhantering med @ControllerAdvice

När något går fel i en Spring Boot-applikation – en bok hittas inte, indata är ogiltig, ett undantag kastas – behöver klienten ett strukturerat och begripligt svar. Utan global felhantering får klienten antingen ett `500 Internal Server Error` med en lång Java-stacktrace, eller ett `400`-svar vars format är Spring Boots inbyggda och svårt att styra.

`@ControllerAdvice` löser det. Det är en klass som fångar undantag från alla controllers och låter dig forma svaret precis som du vill.

---

## Problemet utan felhantering

Med de guider vi hittills har gjort kastar servicen ett `RuntimeException` om en bok inte hittas:

```java
public BookDto getById(Long id) {
    Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Bok med id " + id + " hittades inte"));
    return bookMapper.toDto(book);
}
```

Det ger ett `500 Internal Server Error` – vilket är fel. Det är inte ett serverfel, det är ett klientfel. Klienten frågade efter ett id som inte finns. Rätt statuskod är `404 Not Found`.

Dessutom ser svaret ut ungefär så här utan felhantering:

```json
{
  "timestamp": "2025-05-05T10:23:41.123+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/books/99"
}
```

Det är Springs standardformat och berättar ingenting användbart. Frontend vet inte vad som gick fel och kan inte visa ett vettigt meddelande.

---

## Egna undantagsklasser

Första steget är att byta ut `RuntimeException` mot egna undantagsklasser. Det gör det möjligt att fånga specifika typer av fel och ge dem rätt statuskod.

```java
package se.bibliotek.library.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

Servicen uppdateras till att kasta det nya undantaget:

```java
public BookDto getById(Long id) {
    Book book = bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Bok med id " + id + " hittades inte"));
    return bookMapper.toDto(book);
}
```

Koden ser nästan likadan ut, men nu kan du fånga just `ResourceNotFoundException` i felhanteraren och ge den statuskod `404` – utan att påverka hur andra undantag hanteras.

---

## Felsvarets form

Innan vi skriver `@ControllerAdvice` behöver vi bestämma vilket format felsvar ska ha. En vanlig och enkel struktur är:

```java
package se.bibliotek.library.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;

    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public ErrorResponse(int status, String message, LocalDateTime timestamp, List<String> errors) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getErrors() { return errors; }
}
```

`errors`-listan används när det finns flera valideringsfel samtidigt – en klient kan skicka in ett formulär där tre fält är ogiltiga och alla tre ska returneras på en gång, inte ett i taget.

---

## @ControllerAdvice

Nu kan vi skriva den globala felhanteraren. Klassen annoteras med `@ControllerAdvice` och varje metod annoteras med `@ExceptionHandler` för den undantagstyp den ska fånga.

```java
package se.bibliotek.library.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import se.bibliotek.library.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 404 – resursen finns inte
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                404,
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(404).body(body);
    }

    // 400 – valideringsfel från @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorResponse body = new ErrorResponse(
                400,
                "Valideringsfel",
                LocalDateTime.now(),
                errors
        );
        return ResponseEntity.status(400).body(body);
    }

    // 500 – allt annat som inte fångas specifikt
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                500,
                "Ett oväntat fel inträffade",
                LocalDateTime.now()
        );
        return ResponseEntity.status(500).body(body);
    }
}
```

Spring letar upp den mest specifika `@ExceptionHandler` som matchar undantaget. Om en `ResourceNotFoundException` kastas körs `handleNotFound`. Om ett okänt undantag kastas faller det igenom till `handleGeneric`. Ordningen i klassen spelar ingen roll – det är typmatchningen som avgör.

---

## Hur svaren ser ut nu

### GET /books/99 – boken finns inte

```json
{
  "status": 404,
  "message": "Bok med id 99 hittades inte",
  "timestamp": "2025-05-05T10:23:41.123",
  "errors": null
}
```

Statuskod `404`. Frontend vet nu exakt vad som gick fel och kan visa ett passande meddelande.

### POST /books – ogiltig indata

```json
{
  "status": 400,
  "message": "Valideringsfel",
  "timestamp": "2025-05-05T10:25:12.456",
  "errors": [
    "title: Titel får inte vara tom",
    "year: År måste vara ett positivt tal"
  ]
}
```

Statuskod `400`. Alla valideringsfel returneras på en gång – klienten behöver inte skicka om requesten för att få reda på nästa fel.

---

## Fler egna undantag

Det är vanligt att ha ett undantag per typ av felscenario. Här är ett till som passar biblioteksdatabasen:

```java
// När man försöker låna en bok som inte finns tillgänglig
public class BookNotAvailableException extends RuntimeException {

    public BookNotAvailableException(String message) {
        super(message);
    }
}
```

Lägg till en handler i `GlobalExceptionHandler`:

```java
@ExceptionHandler(BookNotAvailableException.class)
public ResponseEntity<ErrorResponse> handleNotAvailable(BookNotAvailableException ex) {
    ErrorResponse body = new ErrorResponse(
            409,
            ex.getMessage(),
            LocalDateTime.now()
    );
    return ResponseEntity.status(409).body(body);
}
```

`409 Conflict` är rätt statuskod när en operation inte kan genomföras på grund av resursens nuvarande tillstånd – boken finns men kan inte lånas ut.

---

## Från frontends perspektiv

Med strukturerade felsvar kan frontend hantera varje scenario explicit:

```javascript
const response = await fetch("http://localhost:8080/books/99");

if (response.ok) {
    const book = await response.json();
    // visa boken
} else {
    const error = await response.json();

    if (response.status === 404) {
        showMessage(`Hittades inte: ${error.message}`);
    } else if (response.status === 400) {
        error.errors.forEach(e => showFieldError(e));
    } else {
        showMessage("Något gick fel, försök igen senare");
    }
}
```

Utan strukturerade felsvar tvingas frontend antingen visa ett generiskt meddelande eller försöka tolka Springs råa felformat – vilket inte är avsett att läsas av en människa eller en klient-applikation.

---

## Var ska klassen ligga?

En vanlig paketstruktur:

```
exception/
├── GlobalExceptionHandler.java
├── ResourceNotFoundException.java
└── BookNotAvailableException.java
```

Undantagen och felhanteraren hör ihop och bör ligga i samma paket. `ErrorResponse` är en DTO och kan ligga i `dto/`.

---

## Flödet vid ett fel

```
GET /books/99
      ↓
  BookController.getById(99)
      ↓
  BookService.getById(99)
      ↓
  bookRepository.findById(99) → Optional.empty()
      ↓
  throw new ResourceNotFoundException("Bok med id 99 hittades inte")
      ↓
  Spring fångar undantaget
      ↓
  GlobalExceptionHandler.handleNotFound(ex)
      ↓
  ResponseEntity.status(404).body(errorResponse)
      ↓
HTTP 404 Not Found + strukturerat JSON-felsvar
```

Controllern behöver inte göra någonting. Den kastar inte undantag, den fångar inte undantag – det sköts helt av `@ControllerAdvice`. Varje controller-metod kan förutsätta att den bara behöver hantera lyckade fall.
