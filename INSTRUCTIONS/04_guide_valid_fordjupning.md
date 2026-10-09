# Validering med @Valid

När en klient skickar data till ditt API kan du aldrig lita på att den är korrekt. Fält kan saknas, strängar kan vara tomma, tal kan vara negativa. Utan validering når felaktig data service-lagret, repository-lagret och slutligen databasen – där den antingen orsakar ett kryptiskt databasfel eller sparas och skapar problem senare.

`@Valid` och Bean Validation löser det här på ett strukturerat sätt: du deklarerar reglerna direkt på DTOn, och Spring kontrollerar dem automatiskt innan din metod ens körs.

---

## Beroendet

Bean Validation ingår inte i `spring-boot-starter-web`. Du behöver lägga till det explicit i `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Det här beroendet drar in Hibernate Validator, som är standardimplementationen av Bean Validation-specifikationen. Det är Hibernate Validator som faktiskt utför kontrollen när Spring triggar valideringen.

---

## Hur det fungerar – steg för steg

Det är viktigt att förstå att validerings-annotations på en DTO inte gör något på egen hand. De är bara metadata – instruktioner som väntar på att bli körda.

Flödet ser ut så här:

```
Klient skickar JSON
        ↓
Jackson deserialiserar JSON → CreateBookDto
        ↓
Spring ser @Valid på parametern → triggar Hibernate Validator
        ↓
Hibernate Validator kontrollerar varje annotation på DTOn
        ↓
    Fel hittas?
    ├── Ja → kastar MethodArgumentNotValidException → 400 Bad Request
    └── Nej → metoden anropas med den validerade DTOn
```

Det kritiska steget är att `@Valid` måste finnas i controller-signaturen. Utan den sker ingen validering – annotations på DTOn ignoreras helt.

---

## Annotations i DTOn

Annotations placeras på fälten i din request-DTO. Varje annotation representerar en regel som måste uppfyllas.

```java
package se.bibliotek.library.dto;

import jakarta.validation.constraints.*;

public class CreateBookDto {

    @NotBlank(message = "Titel får inte vara tom")
    @Size(max = 255, message = "Titel får inte vara längre än 255 tecken")
    private String title;

    @NotBlank(message = "ISBN får inte vara tomt")
    @Pattern(regexp = "^[\\d-]{10,17}$", message = "ISBN har ogiltigt format")
    private String isbn;

    @Min(value = 1000, message = "Utgivningsår verkar orimligt lågt")
    @Max(value = 2100, message = "Utgivningsår verkar orimligt högt")
    private int year;

    @Positive(message = "Antal exemplar måste vara minst 1")
    private int copiesTotal;

    @NotNull(message = "Författar-id måste anges")
    private Long authorId;

    public CreateBookDto() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getCopiesTotal() { return copiesTotal; }
    public void setCopiesTotal(int copiesTotal) { this.copiesTotal = copiesTotal; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
}
```

---

## Annotations i detalj

### @NotNull

Fältet får inte vara `null`. Det är den mest grundläggande kontrollen.

```java
@NotNull(message = "Författar-id måste anges")
private Long authorId;
```

Godkänt: `2`, `0`, `-1`
Underkänt: `null` (fältet saknas helt i JSON)

Viktigt: `@NotNull` godkänner en tom sträng `""`. För strängar vill du nästan alltid använda `@NotBlank` istället.

---

### @NotEmpty

Fältet får inte vara `null` och inte heller tomt – men blanksteg räknas som innehåll.

```java
@NotEmpty(message = "Titel får inte vara tom")
private String title;
```

Godkänt: `"Sagan om ringen"`, `" "` (blanksteg räknas!)
Underkänt: `null`, `""`

Fungerar även på listor och samlingar – en tom lista `[]` underkänns.

---

### @NotBlank

Fältet får inte vara `null`, tomt eller enbart bestå av blanksteg. Det här är den striktaste av de tre och den du nästan alltid vill använda för strängar.

```java
@NotBlank(message = "Titel får inte vara tom")
private String title;
```

Godkänt: `"Sagan om ringen"`
Underkänt: `null`, `""`, `" "`, `"\t"`, `"\n"`

Regeln att komma ihåg: om du vill ha en faktisk sträng med innehåll, använd `@NotBlank`.

---

### @Size

Kontrollerar längden på en sträng eller storlek på en samling.

```java
@Size(min = 2, max = 255, message = "Titel måste vara mellan 2 och 255 tecken")
private String title;

@Size(max = 10, message = "Får inte innehålla fler än 10 taggar")
private List<String> tags;
```

`min` och `max` är båda valfria – du kan använda bara en av dem.

---

### @Min och @Max

Kontrollerar att ett numeriskt värde ligger inom ett intervall.

```java
@Min(value = 1000, message = "Utgivningsår verkar orimligt lågt")
@Max(value = 2100, message = "Utgivningsår verkar orimligt högt")
private int year;
```

Fungerar på `int`, `long`, `Integer`, `Long`, `BigInteger` och `BigDecimal`.

---

### @Positive och @PositiveOrZero

`@Positive` kräver att värdet är strikt större än noll. `@PositiveOrZero` tillåter även noll.

```java
@Positive(message = "Antal exemplar måste vara minst 1")
private int copiesTotal;

@PositiveOrZero(message = "Antal tillgängliga exemplar kan inte vara negativt")
private int copiesAvailable;
```

Det finns även `@Negative` och `@NegativeOrZero` för de sällsynta fall du behöver negativa tal.

---

### @Pattern

Kontrollerar att en sträng matchar ett reguljärt uttryck.

```java
@Pattern(regexp = "^[\\d-]{10,17}$", message = "ISBN har ogiltigt format")
private String isbn;
```

Reguljära uttryck är kraftfulla men kan bli svårlästa. Skriv dem med ett tydligt `message` så att felmeddelandena berättar vad som är fel – inte bara att det är fel.

---

### @Email

Kontrollerar att strängen ser ut som en e-postadress. Kontrollen är inte vattentät – den verifierar format, inte att adressen faktiskt existerar.

```java
@Email(message = "Ogiltig e-postadress")
private String email;
```

---

### @Future och @Past

Kontrollerar att ett datum ligger i framtiden respektive i det förflutna. Fungerar med `LocalDate`, `LocalDateTime` och `ZonedDateTime`.

```java
@Future(message = "Återlämningsdatum måste vara i framtiden")
private LocalDate dueDate;

@Past(message = "Utgivningsdatum kan inte vara i framtiden")
private LocalDate publishedDate;
```

---

## @Valid i controllern

Utan `@Valid` händer ingenting. Med `@Valid` triggas valideringen automatiskt.

```java
@PostMapping
public ResponseEntity<BookDto> create(@Valid @RequestBody CreateBookDto dto) {
    BookDto created = bookService.create(dto);
    return ResponseEntity.status(201).body(created);
}
```

`@Valid` placeras direkt framför `@RequestBody`. Det är ordningen som signalerar till Spring att parametern ska valideras innan metoden körs.

Om valideringen misslyckas kastas `MethodArgumentNotValidException` och Spring returnerar `400 Bad Request`. Din `create`-metod anropas aldrig – det sker redan innan Spring ens försöker anropa den.

---

## Vad som händer vid valideringsfel

Spring Boot har ett standardsvar för valideringsfel, men det är verbose och svårläst för en klient. Utan en `@ControllerAdvice` kan ett valideringsfel se ut så här:

```json
{
  "timestamp": "2025-05-05T10:23:41.123+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/books"
}
```

Felmeddelandena du definierade i `message`-attributet syns inte alls i standardsvaret. För att exponera dem behöver du en global felhanterare.

---

## Fånga felen med @ControllerAdvice

`MethodArgumentNotValidException` innehåller alla valideringsfel. Du plockar ut dem via `getBindingResult()` och formar svaret som du vill.

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();

    ErrorResponse body = new ErrorResponse(400, "Valideringsfel", LocalDateTime.now(), errors);
    return ResponseEntity.status(400).body(body);
}
```

`getFieldErrors()` returnerar en lista med ett `FieldError` per fält som misslyckades. `getField()` ger fältnamnet, `getDefaultMessage()` ger det meddelande du angav i `message`-attributet.

Med felhanteraren ser svaret ut så här om klienten skickar en tom titel och ett negativt exemplarantal:

```json
{
  "status": 400,
  "message": "Valideringsfel",
  "timestamp": "2025-05-05T10:23:41.123",
  "errors": [
    "title: Titel får inte vara tom",
    "copiesTotal: Antal exemplar måste vara minst 1"
  ]
}
```

Alla fel returneras på en gång. Klienten behöver inte skicka om requesten för att hitta nästa fel.

---

## Kombinera flera annotations

Du kan stapla hur många annotations du vill på ett fält. Alla kontrolleras, och alla som misslyckas returneras i felsvarslistan.

```java
@NotBlank(message = "Titel får inte vara tom")
@Size(min = 2, max = 255, message = "Titel måste vara mellan 2 och 255 tecken")
@Pattern(regexp = "^[^<>]*$", message = "Titel får inte innehålla < eller >")
private String title;
```

Om klienten skickar `""` triggas `@NotBlank` och `@Size`. Klienten får båda felen i svaret.

---

## Viktiga skillnader att hålla koll på

### @NotNull vs @NotBlank vs @NotEmpty

| Annotation | null | `""` | `" "` | `"text"` |
|---|---|---|---|---|
| `@NotNull` | ✗ | ✓ | ✓ | ✓ |
| `@NotEmpty` | ✗ | ✗ | ✓ | ✓ |
| `@NotBlank` | ✗ | ✗ | ✗ | ✓ |

För strängar som ska ha faktiskt innehåll: använd alltid `@NotBlank`.
För `Long`-, `Integer`- eller andra icke-sträng-fält som inte får saknas: använd `@NotNull`.

### Primitiver kan inte vara null

`int`, `long`, `boolean` och andra primitiver kan aldrig vara `null` i Java. Om du behöver kontrollera att ett numeriskt fält faktiskt skickats med – istället för att få standardvärdet `0` – använd wrapper-klassen:

```java
// Primitiv – kan aldrig vara null, standardvärde är 0 om fältet saknas i JSON
private int copiesTotal;

// Wrapper – kan vara null om fältet saknas, @NotNull fungerar här
@NotNull(message = "Antal exemplar måste anges")
@Positive(message = "Antal exemplar måste vara minst 1")
private Integer copiesTotal;
```

Det är en subtle men viktig skillnad. Med `int` och `@NotNull` ger kompilatorn en varning eftersom primitiver aldrig kan bli null – valideringen är meningslös.

---

## Validering på update-metoder

PUT-metoder tar ofta emot samma typ av data som POST. Samma annotations gäller.

```java
@PutMapping("/{id}")
public ResponseEntity<BookDto> update(
        @PathVariable Long id,
        @Valid @RequestBody CreateBookDto dto) {
    return ResponseEntity.ok(bookService.update(id, dto));
}
```

Vissa väljer att ha separata DTOs för create och update – `CreateBookDto` och `UpdateBookDto` – om reglerna skiljer sig åt. Till exempel kanske du vid uppdatering vill tillåta att bara några fält skickas med, eller ha andra längdbegränsningar.

---

## Vad @Valid inte gör

Validering med `@Valid` kontrollerar format och struktur – inte affärsregler.

Den kan kontrollera att `authorId` inte är `null`. Den kan inte kontrollera att en författare med det id:t faktiskt finns i databasen. Det är service-lagrets ansvar.

```java
public BookDto create(CreateBookDto dto) {
    // @Valid har redan kontrollerat att authorId inte är null
    // Men existensen kontrolleras här – det är affärslogik, inte formatvalidering
    Author author = authorRepository.findById(dto.getAuthorId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Författare med id " + dto.getAuthorId() + " hittades inte"));

    Book book = bookMapper.toEntity(dto, author);
    return bookMapper.toDto(bookRepository.save(book));
}
```

En tumregel: om valideringsregeln kräver ett databasanrop hör den hemma i servicen, inte i en Bean Validation-annotation.
