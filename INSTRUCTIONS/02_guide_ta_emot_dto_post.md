# Ta emot en DTO, validera och spara

Den här guiden bygger vidare på flödet från förra guiden men fokuserar på det inkommande hållet – när en klient skickar data till API:et via POST. Vi tittar på hur man tar emot en `CreateBookDto`, validerar den med `@Valid`, sparar via ett transaktionellt service-lager och returnerar ett vettigt svar.

---

## Två separata DTOs för in och ut

I förra guiden hade vi `BookDto` som används för att skicka *ut* data. Nu behöver vi en separat DTO för inkommande data. Skälet är att de två har olika ansvar och olika fält.

`BookDto` – svars-DTOn, innehåller alltid `id`, `authorName` och det beräknade `available`-fältet. Det är vad klienten *får tillbaka*.

`CreateBookDto` – request-DTOn, innehåller bara det en klient kan och ska skicka in vid skapande. Inget `id` – det finns inte än. Inget `available` – det är alltid sant när en bok är ny.

```java
package se.bibliotek.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateBookDto {

    @NotBlank(message = "Titel får inte vara tom")
    @Size(max = 255, message = "Titel får inte vara längre än 255 tecken")
    private String title;

    @NotBlank(message = "ISBN får inte vara tomt")
    private String isbn;

    @Positive(message = "År måste vara ett positivt tal")
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

Request-DTOn har inga `id`- eller `available`-fält alls. Det finns inget att sätta setter eller getter på för det som inte ska kunna skickas in.

---

## @Valid – validera indata innan den når servicen

Annotations som `@NotBlank` och `@Positive` är bara metadata om de inte aktiveras. Det är `@Valid` i controllern som triggar valideringen.

Utan `@Valid` sitter annotations i DTOn och gör ingenting – de är dekorativa tills Spring ombeds att faktiskt kontrollera dem.

Vanliga annotations:

| Annotation | Vad den kontrollerar |
|---|---|
| `@NotNull` | Fältet får inte vara `null` |
| `@NotBlank` | Strängen får inte vara `null`, tom eller bara blanksteg |
| `@NotEmpty` | Sträng eller samling får inte vara `null` eller tom |
| `@Size(min, max)` | Längd på sträng eller samling |
| `@Min(n)` / `@Max(n)` | Numeriskt värde minst/max n |
| `@Positive` | Tal måste vara större än noll |
| `@Email` | Strängen måste se ut som en e-postadress |
| `@Pattern(regexp)` | Strängen måste matcha ett reguljärt uttryck |

För att Bean Validation ska fungera behöver du beroendet i `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Mappern – nu i båda riktningarna

Mappern behöver nu också kunna konvertera en inkommande `CreateBookDto` till en entitet. Metoden `toEntity` tar DTOn och en redan hämtad `Author`-entitet som argument – det är servicens ansvar att hämta författaren, inte mapperns.

```java
@Component
public class BookMapper {

    // Entitet → svars-DTO (från förra guiden)
    public BookDto toDto(Book book) {
        String authorName = book.getAuthor() != null
                ? book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName()
                : "Okänd";

        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getYear(),
                book.getCopiesAvailable() > 0,
                authorName
        );
    }

    // Request-DTO + Author → entitet (ny)
    public Book toEntity(CreateBookDto dto, Author author) {
        return new Book(
                dto.getTitle(),
                dto.getIsbn(),
                dto.getYear(),
                dto.getCopiesTotal(),
                author
        );
    }
}
```

Mappern vet ingenting om databaser. Den konverterar bara – det gör den enkel att läsa och testa.

---

## @Transactional i service-lagret

`@Transactional` talar om för Spring att en metod ska köras inom en databastransaktion. Det innebär att alla databasoperationer som sker inuti metoden behandlas som ett block – antingen lyckas allt, eller rullas allt tillbaka.

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.bookMapper = bookMapper;
    }

    @Transactional
    public BookDto create(CreateBookDto dto) {
        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> new RuntimeException(
                        "Författare med id " + dto.getAuthorId() + " hittades inte"));

        Book book = bookMapper.toEntity(dto, author);
        Book saved = bookRepository.save(book);
        return bookMapper.toDto(saved);
    }
}
```

### Varför behövs @Transactional här?

I `create`-metoden ovan sker två databasanrop: ett `findById` för att hämta författaren, och ett `save` för att spara boken. `@Transactional` garanterar att de körs i samma databassession.

Det spelar roll om `save` kastar ett undantag – med `@Transactional` rullas hela operationen tillbaka och databasen lämnas i samma tillstånd som innan anropet. Utan det kan du hamna i ett läge där delar av en operation är genomförd och andra inte.

`@Transactional` har även en annan praktisk effekt: Hibernate håller sessionen öppen hela metoden ut. Det betyder att lazy-laddade relationer (som `author` på `Book`) kan nås utan att orsaka `LazyInitializationException`, eftersom mappern anropas *inuti* den transaktionella metoden.

### Read-only för GET-metoder

För hämtningsmetoder kan man använda `@Transactional(readOnly = true)`. Det signalerar till databasen att inga skrivoperationer kommer ske, vilket kan ge bättre prestanda.

```java
@Transactional(readOnly = true)
public BookDto getById(Long id) {
    Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Bok med id " + id + " hittades inte"));
    return bookMapper.toDto(book);
}

@Transactional(readOnly = true)
public List<BookDto> getAll() {
    return bookRepository.findAll().stream()
            .map(bookMapper::toDto)
            .toList();
}
```

---

## Controllern – ta emot och validera

`@Valid` placeras direkt framför `@RequestBody`-parametern. Det är allt som krävs för att Spring ska köra valideringen innan metoden exekveras.

```java
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody CreateBookDto dto) {
        BookDto created = bookService.create(dto);
        return ResponseEntity.status(201).body(created);
    }
}
```

Om valideringen misslyckas – till exempel om `title` är tom – kastar Spring automatiskt ett `MethodArgumentNotValidException` och svaret blir `400 Bad Request`. Din `create`-metod anropas aldrig.

---

## Vad händer vid ett valideringsfel?

Om klienten skickar in ett tomt `title`:

```json
{
  "title": "",
  "isbn": "978-91-7549-101-2",
  "year": 1954,
  "copiesTotal": 3,
  "authorId": 2
}
```

Svarar Spring med `400 Bad Request` och ett standardiserat felmeddelande som ser ut ungefär så här:

```json
{
  "status": 400,
  "errors": [
    "Titel får inte vara tom"
  ]
}
```

Det exakta formatet beror på om du har en `@ControllerAdvice` för felhantering – utan den ser Spring Boots standardsvar lite verbose ut. Med en global felhanterare kan du forma svaret precis som du vill.

---

## Varför returnera ResponseEntity med 201 efter en POST?

Man *kan* returnera den skapade DTOn direkt utan `ResponseEntity` och Spring sätter då `200 OK`. Det fungerar, men `201 Created` är det korrekta HTTP-svaret när en ny resurs har skapats. Det är inte en godtycklig detalj – det är en del av HTTP-standarden och är vad klienter förväntar sig.

Det har konkreta konsekvenser i frontend. En React-app som anropar ett API kan skilja på om ett anrop *hittade* något (`200`) eller *skapade* något nytt (`201`). Det gör det möjligt att hantera de två fallen olika:

```javascript
const response = await fetch("http://localhost:8080/books", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
        title: "Sagan om ringen",
        isbn: "978-91-7549-101-2",
        year: 1954,
        copiesTotal: 3,
        authorId: 2
    })
});

if (response.status === 201) {
    const created = await response.json();
    console.log("Boken skapades med id:", created.id);
    // Navigera till den nya resursen, uppdatera en lista, visa ett meddelande etc.
} else if (response.status === 400) {
    const error = await response.json();
    console.log("Valideringsfel:", error.errors);
}
```

Svaret vid `201` innehåller den skapade boken med `id` tilldelat av databasen – det är något klienten behöver och inte kan veta innan servern har sparat den:

```json
{
  "id": 7,
  "title": "Sagan om ringen",
  "isbn": "978-91-7549-101-2",
  "year": 1954,
  "available": true,
  "authorName": "J.R.R. Tolkien"
}
```

Det är också möjligt att sätta en `Location`-header i svaret som pekar på den nya resursen, vilket är praxis i REST-APIer som följer specifikationen noga:

```java
URI location = URI.create("/books/" + created.getId());
return ResponseEntity.created(location).body(created);
```

`ResponseEntity.created(location)` sätter statuskod `201` och lägger automatiskt till `Location: /books/7` i svarshuvudet. Frontend kan då följa den headern för att hämta resursen vid behov.

---

## Flödet i korthet

```
POST /books  (JSON-body med CreateBookDto)
      ↓
  @Valid validerar DTOn
  – misslyckas → 400 Bad Request, metoden körs aldrig
  – lyckas     ↓
  BookController.create(dto)
      ↓
  BookService.create(dto)           ← @Transactional öppnar session
      ↓
  authorRepository.findById(...)    ← hämtar Author inom transaktionen
      ↓
  bookMapper.toEntity(dto, author)  ← bygger Book-entiteten
      ↓
  bookRepository.save(book)         ← sparar, Hibernate tilldelar id
      ↓
  bookMapper.toDto(saved)           ← konverterar till BookDto
      ↓                               (author är fortfarande laddad – vi är kvar i transaktionen)
  @Transactional stänger sessionen och committar
      ↓
  ResponseEntity.status(201).body(bookDto)
      ↓
HTTP 201 Created + JSON-body med den skapade boken inklusive id
```

Varje lager har ett tydligt ansvar: controllern validerar och hanterar HTTP, servicen koordinerar och håller transaktionen, mappern konverterar, repositoryt skriver till databasen.
