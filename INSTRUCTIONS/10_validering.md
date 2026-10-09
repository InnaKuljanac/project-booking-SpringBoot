# Validering med Bean Validation

Bean Validation är ett standardbibliotek för att validera indata i Java. Spring Boot inkluderar det automatiskt när du har `spring-boot-starter-validation` i din `pom.xml`. Med ett par annotations kan du säkerställa att det som skickas in till ditt API uppfyller dina krav – utan att skriva if-satser i service-lagret.

---

## Lägg till beroendet

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Annotations på DTO

Lägg valideringsannotations direkt på fälten i din DTO:

```java
public class CreateBookDto {

    @NotBlank(message = "Titel får inte vara tom")
    private String title;

    @NotNull(message = "Pris måste anges")
    @Positive(message = "Pris måste vara ett positivt tal")
    private Double price;

    @Size(max = 500, message = "Beskrivningen får inte vara längre än 500 tecken")
    private String description;

    public CreateBookDto() {}

    // getters och setters...
}
```

---

## Aktivera validering i controllern

Lägg till `@Valid` på `@RequestBody`-parametern:

```java
@PostMapping
public ResponseEntity<BookDto> create(@Valid @RequestBody CreateBookDto dto) {
    return ResponseEntity.status(201).body(bookService.create(dto));
}
```

Om valideringen misslyckas kastas ett `MethodArgumentNotValidException` automatiskt av Spring – du behöver inte hantera det manuellt i service-lagret.

---

## Vanliga annotations

| Annotation | Beskrivning |
|---|---|
| `@NotNull` | Värdet får inte vara null |
| `@NotBlank` | String får inte vara null, tom eller bara whitespace |
| `@NotEmpty` | String eller lista får inte vara null eller tom |
| `@Size(min, max)` | Längd på String eller storlek på lista |
| `@Min(value)` | Lägsta tillåtna numeriska värde |
| `@Max(value)` | Högsta tillåtna numeriska värde |
| `@Positive` | Måste vara ett positivt tal (> 0) |
| `@PositiveOrZero` | Måste vara noll eller positivt |
| `@Email` | Måste vara ett giltigt e-postformat |
| `@Pattern(regexp)` | Måste matcha ett reguljärt uttryck |
| `@Future` | Datum måste vara i framtiden |
| `@Past` | Datum måste vara i det förflutna |

---

## Valideringsmeddelanden

Varje annotation har ett `message`-attribut. Om du inte anger något används ett standardmeddelande på engelska. Egna meddelanden gör felmeddelandena mer användbara för den som konsumerar API:et:

```java
@NotBlank(message = "Titel får inte vara tom")
@Size(min = 2, max = 100, message = "Titeln måste vara mellan 2 och 100 tecken")
private String title;
```

---

## Validering av nästlade objekt

Har du en DTO med ett nästlat objekt behöver du `@Valid` även där för att valideringen ska gälla det nästlade objektet:

```java
public class CreateOrderDto {

    @NotNull
    @Valid
    private AddressDto shippingAddress;
}
```

---

## Nästa steg

När `@Valid` misslyckas kastas ett undantag med information om vilka fält som inte validerade. För att returnera ett strukturerat felmeddelande till klienten istället för ett generiskt 400-svar, se guiden `global_felhantering`.
