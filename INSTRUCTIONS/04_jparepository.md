# JpaRepository och queries

`JpaRepository` ger dig ett färdigt repository med de vanligaste databasoperationerna inbyggda. Men det slutar inte där – Spring Data JPA låter dig skriva egna queries utan SQL, och när det inte räcker kan du skriva JPQL eller vanlig SQL direkt.

---

## Inbyggda metoder

Alla repositories som extends `JpaRepository` får dessa metoder utan att du skriver något:

```java
bookRepository.findAll();               // Hämta alla
bookRepository.findById(id);            // Hämta en – returnerar Optional
bookRepository.save(book);              // Spara eller uppdatera
bookRepository.deleteById(id);          // Ta bort med id
bookRepository.delete(book);            // Ta bort ett objekt
bookRepository.existsById(id);          // Finns den?
bookRepository.count();                 // Räkna antal rader
```

---

## Metodnamnskonventionen

Spring Data JPA kan generera queries automatiskt utifrån metodnamnet. Du deklarerar metoden i repositoryt – Spring skapar SQL-frågan åt dig.

```java
public interface BookRepository extends JpaRepository<Book, Long> {

    // SELECT * FROM books WHERE title = ?
    List<Book> findByTitle(String title);

    // SELECT * FROM books WHERE price < ?
    List<Book> findByPriceLessThan(Double price);

    // SELECT * FROM books WHERE title LIKE %?%
    List<Book> findByTitleContaining(String keyword);

    // SELECT * FROM books WHERE published = true
    List<Book> findByPublishedTrue();

    // SELECT * FROM books WHERE author_id = ? ORDER BY title ASC
    List<Book> findByAuthorIdOrderByTitleAsc(Long authorId);

    // SELECT * FROM books WHERE author_id = ? AND price < ?
    List<Book> findByAuthorIdAndPriceLessThan(Long authorId, Double price);
}
```

### Nyckelord i metodnamn

| Nyckelord | SQL-motsvarighet | Exempel |
|---|---|---|
| `findBy` | `WHERE` | `findByTitle` |
| `And` | `AND` | `findByTitleAndPrice` |
| `Or` | `OR` | `findByTitleOrAuthor` |
| `LessThan` | `<` | `findByPriceLessThan` |
| `GreaterThan` | `>` | `findByPriceGreaterThan` |
| `Between` | `BETWEEN` | `findByPriceBetween` |
| `Containing` | `LIKE %?%` | `findByTitleContaining` |
| `StartingWith` | `LIKE ?%` | `findByTitleStartingWith` |
| `EndingWith` | `LIKE %?` | `findByTitleEndingWith` |
| `True` / `False` | `= true/false` | `findByPublishedTrue` |
| `IsNull` | `IS NULL` | `findByDescriptionIsNull` |
| `OrderBy` | `ORDER BY` | `findByAuthorIdOrderByTitleAsc` |
| `Asc` / `Desc` | `ASC` / `DESC` | `...OrderByPriceDesc` |

---

## @Query med JPQL

När metodnamnen blir för långa eller komplicerade kan du skriva en query med `@Query`. JPQL (Java Persistence Query Language) ser ut som SQL men skriver mot entiteter och fält – inte tabeller och kolumner.

```java
// JPQL – använder entitetsnamnet Book och fältnamnet title (inte tabellen books)
@Query("SELECT b FROM Book b WHERE b.title LIKE %:keyword%")
List<Book> searchByTitle(@Param("keyword") String keyword);

// Hämta böcker med mer än ett visst antal sidor, sorterat
@Query("SELECT b FROM Book b WHERE b.pages > :min ORDER BY b.pages DESC")
List<Book> findLongBooks(@Param("min") int minPages);

// JOIN mot relaterad entitet
@Query("SELECT b FROM Book b JOIN b.author a WHERE a.name = :authorName")
List<Book> findByAuthorName(@Param("authorName") String name);
```

JPQL fungerar med alla databaser – du skriver mot din Java-modell, Hibernate översätter till rätt SQL-dialekt.

---

## @Query med native SQL

Ibland behöver du skriva ren SQL – t.ex. när du använder databasspecifika funktioner eller optimerar komplexa queries. Lägg till `nativeQuery = true`:

```java
@Query(value = "SELECT * FROM books WHERE YEAR(published_at) = :year",
       nativeQuery = true)
List<Book> findByPublishedYear(@Param("year") int year);
```

Tänk på att native queries är kopplade till en specifik databas och kan sluta fungera om du byter.

---

## @Modifying – uppdatera och ta bort med query

För UPDATE och DELETE med `@Query` behövs `@Modifying`. Det talar om för Spring att queryn ändrar data och inte bara läser.

```java
@Modifying
@Query("UPDATE Book b SET b.published = true WHERE b.id = :id")
void publishBook(@Param("id") Long id);

@Modifying
@Query("DELETE FROM Book b WHERE b.published = false")
void deleteUnpublished();
```

`@Modifying` används alltid tillsammans med `@Transactional` på service-metoden som anropar den.

---

## Exists och Count

```java
// Returnerar true om minst en bok med den titeln finns
boolean existsByTitle(String title);

// Räknar böcker av en viss författare
long countByAuthorId(Long authorId);
```

Praktiskt för att kontrollera duplicat innan du sparar, eller för statistik.

---

## Nästa steg

Med metodnamnskonventionen och `@Query` täcker du de flesta scenarion. För mer komplexa dynamiska queries – där villkoren varierar beroende på input – finns `Specification` och `Criteria API`, men det är sällan nödvändigt för vanliga projekt.
