# DTO, Mapper och ResponseEntity

Den här guiden visar det kompletta flödet från databas till HTTP-svar – med biblioteksdatabasen som exempel. Vi tittar på `Book` och hur den hanteras genom alla lager.

---

## Varför tre separata delar?

**DTO** – bestämmer *vad* som skickas ut. Entiteten stannar i service-lagret, DTOn är det som faktiskt lämnar applikationen som JSON.

**Mapper** – ansvarar för *hur* man konverterar mellan entitet och DTO. En separat klass håller konverteringslogiken på ett ställe istället för utspridd i servicen.

**ResponseEntity** – bestämmer *hur* HTTP-svaret ser ut. Statuskod, headers och body i ett och samma objekt.

---

## Entiteten

Entiteten representerar en rad i databasen och ska aldrig lämna service-lagret.

```java
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String isbn;
    private int year;
    private int copiesTotal;
    private int copiesAvailable;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;

    public Book() {}

    public Book(String title, String isbn, int year, int copiesTotal, Author author) {
        this.title = title;
        this.isbn = isbn;
        this.year = year;
        this.copiesTotal = copiesTotal;
        this.copiesAvailable = copiesTotal;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getCopiesTotal() { return copiesTotal; }
    public void setCopiesTotal(int copiesTotal) { this.copiesTotal = copiesTotal; }
    public int getCopiesAvailable() { return copiesAvailable; }
    public void setCopiesAvailable(int copiesAvailable) { this.copiesAvailable = copiesAvailable; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
}
```

---

## DTOn

DTOn bestämmer exakt vad som exponeras i API:et. Lägg märke till att vi slår ihop `author`-objektet till en enda sträng, och att `copiesAvailable` görs om till ett `boolean`-fält `available` – det är lättare för en frontend att använda.

```java
package se.bibliotek.library.dto;

public class BookDto {

    private Long id;
    private String title;
    private String isbn;
    private int year;
    private boolean available;
    private String authorName; // inte hela Author-entiteten – bara det frontend behöver

    public BookDto() {}

    public BookDto(Long id, String title, String isbn, int year,
                   boolean available, String authorName) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
        this.year = year;
        this.available = available;
        this.authorName = authorName;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
}
```

`id` har ingen setter – det är ett medvetet val. En klient ska aldrig skicka in ett id.

---

## Mappern

Mappern är en Spring-komponent vars enda ansvar är att konvertera mellan `Book` och `BookDto`. Genom att lägga konverteringslogiken här behöver servicen inte ha privata hjälpmetoder för det.

```java
package se.bibliotek.library.mapper;

import org.springframework.stereotype.Component;
import se.bibliotek.library.dto.BookDto;
import se.bibliotek.library.model.Book;

@Component
public class BookMapper {

    public BookDto toDto(Book book) {
        String authorName = book.getAuthor() != null
                ? book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName()
                : "Okänd";

        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getYear(),
                book.getCopiesAvailable() > 0,  // beräknat värde direkt i mappern
                authorName
        );
    }
}
```

Mappern injiceras sedan in i servicen via konstruktorinjektion.

---

## Servicen

Servicen hämtar data från repositoryt och delegerar konverteringen till mappern. Entiteter lämnar aldrig servicen – det som returneras är alltid DTOs.

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public List<BookDto> getAll() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public BookDto getById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bok med id " + id + " hittades inte"));
        return bookMapper.toDto(book);
    }
}
```

---

## ResponseEntity

`ResponseEntity<T>` är ett Spring-objekt som representerar hela HTTP-svaret. Det ger dig kontroll över tre saker:

- **Statuskod** – `200 OK`, `201 Created`, `404 Not Found` osv.
- **Headers** – kan sättas vid behov, t.ex. `Location` vid skapande
- **Body** – det faktiska innehållet, vanligtvis en DTO serialiserad till JSON

Det vanligaste användningsmönstret ser ut så här:

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

Du *kan* returnera en DTO direkt utan att wrappa den i `ResponseEntity`, men det ger dig ingen kontroll över statuskoden. Att alltid använda `ResponseEntity` är tydligare och mer flexibelt.

---

## Controllern

Controllern tar emot HTTP-anrop, delegerar allt till servicen och returnerar ett `ResponseEntity`.

```java
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<BookDto>> getAll() {
        return ResponseEntity.ok(bookService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id));
    }
}
```

Controllern känner inte till `Book`-entiteten överhuvudtaget. Den pratar bara med servicen och hanterar HTTP-lagret.

---

## Hur JSON-svaret ser ut

Jackson – det bibliotek Spring Boot använder för serialisering – konverterar `BookDto` till JSON automatiskt. Det gör det genom att läsa alla `get`-metoder i klassen.

### getAll – `GET /books`

Svaret blir en JSON-array. Statuskod `200 OK`.

```json
[
  {
    "id": 1,
    "title": "Sagan om ringen",
    "isbn": "978-91-7549-101-2",
    "year": 1954,
    "available": true,
    "authorName": "J.R.R. Tolkien"
  },
  {
    "id": 2,
    "title": "1984",
    "isbn": "978-0-452-28423-4",
    "year": 1949,
    "available": false,
    "authorName": "George Orwell"
  }
]
```

### getById – `GET /books/1`

Svaret blir ett enda JSON-objekt. Statuskod `200 OK`.

```json
{
  "id": 1,
  "title": "Sagan om ringen",
  "isbn": "978-91-7549-101-2",
  "year": 1954,
  "available": true,
  "authorName": "J.R.R. Tolkien"
}
```

Notera att `copiesTotal` och `copiesAvailable` inte syns i svaret – de finns i entiteten men inte i DTOn. Det är poängen: du bestämmer exakt vad som exponeras.

---

## Från frontends perspektiv

När frontend gör ett anrop mot `GET /books/1` tar den emot hela HTTP-svaret, inte bara JSON-kroppen. Det är där `ResponseEntity` kommer in – statuskoden och eventuella headers ingår i svaret och frontend kan använda dem.

### Statuskoden berättar om det gick bra

Frontend bör alltid kontrollera statuskoden innan den försöker läsa svaret. I JavaScript/fetch:

```javascript
const response = await fetch("http://localhost:8080/books/1");

if (response.ok) {                          // true om statuskod är 200-299
    const book = await response.json();
    console.log(book.title);               // "Sagan om ringen"
    console.log(book.authorName);          // "J.R.R. Tolkien"
    console.log(book.available);           // true
} else if (response.status === 404) {
    console.log("Boken hittades inte");
}
```

### getAll – visa en lista

```javascript
const response = await fetch("http://localhost:8080/books");
const books = await response.json();       // books är nu en array

books.forEach(book => {
    console.log(`${book.title} av ${book.authorName}`);
    console.log(book.available ? "Tillgänglig" : "Utlånad");
});
```

### available är ett beräknat värde

I databasen finns `copies_available` som en siffra. I mappern omvandlade vi det till `boolean`. Frontend behöver inte tänka på logiken – den får redan ett sant eller falskt värde och kan visa det direkt:

```javascript
// Rendera en lista av böcker i React
function BookList({ books }) {
    return (
        <ul>
            {books.map(book => (
                <li key={book.id}>
                    <strong>{book.title}</strong> – {book.authorName}
                    <span>{book.available ? " ✓ Ledig" : " ✗ Utlånad"}</span>
                </li>
            ))}
        </ul>
    );
}
```

Det är ett bra exempel på varför DTOs är viktiga: mappern gör ett designbeslut (`copies_available > 0` → `available`) och frontend slipper känna till intern logik.

---

## Flödet i korthet

```
GET /books/1
      ↓
  BookController.getById(1)
      ↓
  BookService.getById(1)
      ↓
  bookRepository.findById(1)  →  Book (entitet från databasen)
      ↓
  bookMapper.toDto(book)      →  BookDto
      ↓
  ResponseEntity.ok(bookDto)  →  HTTP 200 + JSON-body
      ↓
Frontend tar emot: statuskod 200, body som JSON-objekt
```

Entiteten rör sig aldrig utanför service-lagret. Controllern ser bara DTOs. Frontend ser bara JSON.
