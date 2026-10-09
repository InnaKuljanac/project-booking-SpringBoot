# Testning med Spring Boot

Det här materialet är inte en del av kursmålen, men det är värt att känna till grunderna. Tester är en naturlig del av professionell Java-utveckling och du kommer stöta på dem direkt när du börjar arbeta. Det räcker att förstå hur ett test är uppbyggt, vad de olika typerna gör och hur man skriver ett enkelt test som faktiskt fångar buggar.

---

## Varför testa?

Kod förändras hela tiden. Varje gång du ändrar i service-lagret, lägger till ett fält eller refaktorerar mappningslogiken finns det en risk att något går sönder som fungerade innan. Manuell testning i Postman tar tid och är lätt att glömma. Automatiserade tester körs på en sekund och ger dig ett omedelbart svar.

---

## Två typer av tester du behöver känna till

### Enhetstester

Testar en enskild klass eller metod helt isolerat – utan databas, utan Spring, utan nätverkstrafik. Det är snabba, enkla tester som kontrollerar att logiken i en specifik metod är korrekt.

Passar bra för: service-lager, mappningslogik, beräkningar.

### Integrationstester

Testar hur flera delar fungerar ihop. I Spring Boot-sammanhang innebär det ofta att man startar upp hela eller delar av applikationen och testar att controller, service och databas fungerar ihop som de ska.

Passar bra för: endpoints, databasinteraktion, hela flöden.

---

## Ett tests anatomi

Alla tester – oavsett typ – följer samma grundstruktur som brukar kallas **AAA**: Arrange, Act, Assert.

```
Arrange  →  Sätt upp det som behövs (data, mockar, tillstånd)
Act      →  Anropa det du vill testa
Assert   →  Kontrollera att resultatet är det förväntade
```

Här är ett konkret exempel:

```java
@Test
void getById_returnsTodoDto_whenTodoExists() {
    // Arrange
    Todo todo = new Todo("Läs på om Spring", false);
    todo.setId(1L);
    when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));

    // Act
    TodoDto result = todoService.getById(1L);

    // Assert
    assertEquals("Läs på om Spring", result.getTitle());
    assertFalse(result.isCompleted());
}
```

Namnge alltid din testmetod beskrivande – `getById_returnsTodoDto_whenTodoExists` berättar vad testet kontrollerar utan att man behöver läsa koden.

---

## Annotationer att känna till

| Annotation | Var | Vad den gör |
|---|---|---|
| `@Test` | Metod | Markerar att metoden är ett test |
| `@SpringBootTest` | Klass | Startar hela Spring-kontexten för integrationstester |
| `@WebMvcTest(X.class)` | Klass | Startar bara webb-lagret (controller), inte service/databas |
| `@DataJpaTest` | Klass | Startar bara JPA-lagret med en inbyggd testdatabas |
| `@MockBean` | Fält | Skapar en mockad bean i Spring-kontexten |
| `@ExtendWith(MockitoExtension.class)` | Klass | Aktiverar Mockito för enhetstester utan Spring |
| `@Mock` | Fält | Skapar ett mockat objekt (utan Spring) |
| `@InjectMocks` | Fält | Skapar objektet som testas och injicerar mockar i det |

---

## Beroenden i pom.xml

Spring Initializr lägger in testberoendet automatiskt:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Det här beroendet inkluderar JUnit 5, Mockito och AssertJ – det räcker gott för att komma igång.

---

## Bibliotekssystemet – konkreta exempel

Vi använder ett enkelt bibliotekssystem som exempel. Systemet har en `Book`-entitet, ett `BookRepository` och en `BookService` med metoder för att hämta och skapa böcker.

```java
// Book.java (förenklad)
@Entity
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String author;
    private boolean available;
    // konstruktorer, getters, setters...
}

// BookDto.java
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private boolean available;
    // konstruktorer, getters, setters...
}
```

---

### Exempel 1 – Enhetstest för service-lagret

Det här testet kontrollerar att `getById` i service-lagret returnerar korrekt DTO när en bok finns i databasen.

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void getById_returnsCorrectDto_whenBookExists() {
        // Arrange
        Book book = new Book("Brott och straff", "Dostojevskij", true);
        book.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        // Act
        BookDto result = bookService.getById(1L);

        // Assert
        assertEquals(1L, result.getId());
        assertEquals("Brott och straff", result.getTitle());
        assertEquals("Dostojevskij", result.getAuthor());
        assertTrue(result.isAvailable());
    }

    @Test
    void getById_throwsException_whenBookNotFound() {
        // Arrange
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> bookService.getById(99L));
    }
}
```

`@Mock` skapar ett falskt repository – inga databasanrop sker. `@InjectMocks` skapar en riktig `BookService` och stoppar in det mockade repositoryt i den.

---

### Exempel 2 – Integrationstest för controller-lagret

Det här testet startar webb-lagret och kontrollerar att `GET /books/1` svarar med rätt JSON och statuskod 200.

```java
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void getById_returns200_withBookDto() throws Exception {
        // Arrange
        BookDto dto = new BookDto(1L, "Brott och straff", "Dostojevskij", true);
        when(bookService.getById(1L)).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Brott och straff"))
                .andExpect(jsonPath("$.author").value("Dostojevskij"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void create_returns201_withCreatedBook() throws Exception {
        // Arrange
        BookDto dto = new BookDto(1L, "Ny bok", "Författare", true);
        when(bookService.create(any())).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Ny bok\", \"author\": \"Författare\", \"available\": true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
```

`@WebMvcTest` startar bara controller-lagret – inget service, ingen databas. `@MockBean` ersätter den riktiga `BookService` med en mockad version.

---

## Vad som faktiskt kan gå fel – och varför tester fångar det

Det här är poängen med tester. Säg att du under kursens gång ändrar i mappningslogiken i `BookService` och av misstag byter ut `book.isAvailable()` mot ett hårdkodat `false`:

```java
// Fel mappning – du ändrade detta av misstag
private BookDto toDto(Book book) {
    return new BookDto(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        false  // ← skulle vara book.isAvailable()
    );
}
```

Din applikation startar utan fel. Postman verkar fungera. Men testet vi skrev ovan misslyckas omedelbart:

```
expected: <true>
 but was: <false>
```

Utan testet skulle den här buggen lätt slinka igenom till produktion. Med testet på plats får du veta om det direkt – vid nästa gång du kör dina tester.

**Tester är ett skyddsnät mot oavsiktliga förändringar.** Ju mer ett system växer, desto mer värdefulla blir de.

---

## Tips: Låt AI hjälpa dig skriva tester

Att skriva tester med AI-stöd fungerar riktigt bra, men kvaliteten beror helt på hur mycket kontext du ger. Här är vad som faktiskt behövs:

### Vad AI behöver för att skriva rimliga tester

**1. Klassen som ska testas – hela källkoden**
Klistra in hela service- eller controller-klassen. AI:n behöver se metodnamn, parametertyper och returtyper för att skriva korrekta assertions.

**2. Beroendeklasserna**
Vilka repositories, andra services eller komponenter injiceras? Klistra in eller beskriv dem. Utan detta vet AI:n inte vad som ska mockas.

**3. DTO- och entitetsklasserna**
Fälten och getters/setters behöver stämma. Skriver AI:n `result.getName()` när metoden egentligen heter `result.getTitle()` kompilerar inte testet.

**4. Vad du vill testa**
Specificera om du vill ha enhetstester (bara service-lagret, med Mockito), kontroller-tester (med MockMvc och `@WebMvcTest`) eller fullständiga integrationstester. Det påverkar vilka annotationer och vilket upplägg som används.

### En bra prompt ser ut ungefär så här

```
Skriv enhetstester för BookService nedan. 
Använd JUnit 5, Mockito och @ExtendWith(MockitoExtension.class).
Testa getAll, getById (bok finns respektive inte finns) och create.

[klistra in BookService.java]
[klistra in BookDto.java]
[klistra in Book.java]
```

### Vad du ska kontrollera efteråt

AI:n kan göra misstag. Kontrollera alltid att:

- Metodnamnen i assertions matchar dina faktiska getters
- Mockar är korrekt definierade med rätt returtyp
- Testet faktiskt misslyckas om du förstör koden det testar – annars testar det ingenting

Det sista är det viktigaste. Ett test som alltid är grönt oavsett vad du gör i koden är värdelöst. Prova att tillfälligt byta ut ett korrekt värde i din service-metod och kontrollera att testet då blir rött.
