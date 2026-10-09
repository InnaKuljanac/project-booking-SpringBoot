# JPA-relationer

En relation i JPA beskriver hur två entiteter hänger ihop i databasen. Hibernate läser dina annotations och skapar rätt tabeller och foreign keys åt dig.

---

## En-till-många (@OneToMany / @ManyToOne)

Det vanligaste mönstret. En författare har många böcker – en bok tillhör en författare.

```java
@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    private List<Book> books = new ArrayList<>();

    public Author() {}

    public Author(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Book> getBooks() { return books; }
}
```

```java
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;

    public Book() {}

    public Book(String title, Author author) {
        this.title = title;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
}
```

`mappedBy = "author"` talar om för Hibernate att det är `Book`-sidan som äger relationen och håller i foreign key-kolumnen. `@JoinColumn(name = "author_id")` styr vad kolumnen heter i databasen.

---

## Många-till-många (@ManyToMany)

En bok kan ha flera genrer – en genre kan tillhöra många böcker.

```java
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToMany
    @JoinTable(
        name = "book_genre",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres = new ArrayList<>();

    public Book() {}
    // getters och setters...
}
```

```java
@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "genres")
    private List<Book> books = new ArrayList<>();

    public Genre() {}
    // getters och setters...
}
```

`@JoinTable` skapar en kopplingstabell `book_genre` med kolumnerna `book_id` och `genre_id`. En sida äger relationen (den med `@JoinTable`), den andra pekar tillbaka med `mappedBy`.

---

## Ägarsida och invers sida

I varje relation finns det en ägarsida och en invers sida. Det är viktigt att förstå skillnaden:

**Ägarsidan** – den sida som har `@JoinColumn` eller `@JoinTable`. Det är den sidan Hibernate tittar på när den sparar relationen i databasen.

**Invers sida** – den sida som har `mappedBy`. Den är bara för läsning och navigering i Java-koden – Hibernate ignorerar den när den skriver till databasen.

Det betyder att om du lägger till en bok i `author.getBooks()` utan att sätta `book.setAuthor(author)`, kommer relationen inte sparas. Du måste alltid uppdatera ägarsidan.

```java
// Rätt – ägarsidan (Book) uppdateras
Book book = new Book("Pippi Långstrump", author);
bookRepository.save(book);

// Fel – bara den inversa sidan uppdateras, sparas inte i databasen
author.getBooks().add(book);
```

---

## Fetch-typer

Fetch-typen styr när Hibernate hämtar relaterad data från databasen.

**LAZY** (standard för `@OneToMany` och `@ManyToMany`) – den relaterade datan hämtas inte förrän du faktiskt använder den. Mer effektivt, men kan orsaka problem om sessionen är stängd när du försöker nå datan.

**EAGER** (standard för `@ManyToOne`) – den relaterade datan hämtas direkt när huvudentiteten hämtas. Enkelt men kan ge onödigt många databasanrop.

```java
@OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
private List<Book> books = new ArrayList<>();

@ManyToOne(fetch = FetchType.EAGER)
private Author author;
```

I praktiken är LAZY oftast rätt val för listor. Du styr vad som hämtas via dina service-metoder och DTOs.

---

## DTOs med relationer

När du har relationer behöver dina DTOs reflektera vad du faktiskt vill skicka ut i API:et. Undvik att nästla hela entiteter – skapa en enkel DTO för den relaterade resursen.

```java
// En enkel DTO för en bok med författarens namn
public class BookDto {
    private Long id;
    private String title;
    private String authorName;  // inte hela Author-objektet

    public BookDto(Long id, String title, String authorName) {
        this.id = id;
        this.title = title;
        this.authorName = authorName;
    }

    // getters...
}
```

Mappningen i service-lagret:

```java
private BookDto toDto(Book book) {
    return new BookDto(
        book.getId(),
        book.getTitle(),
        book.getAuthor().getName()
    );
}
```

---

## Vad Hibernate genererar

Med entiteterna ovan och `ddl-auto=create-drop` skapar Hibernate följande tabeller:

```sql
CREATE TABLE authors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author_id BIGINT,
    FOREIGN KEY (author_id) REFERENCES authors(id)
);

CREATE TABLE book_genre (
    book_id BIGINT,
    genre_id BIGINT,
    PRIMARY KEY (book_id, genre_id)
);
```

Du kan verifiera detta i H2-konsolen eller i terminalen om `show-sql=true` är aktiverat.
