# Använda MapStruct för mappning

MapStruct är ett kodgenereringsbibliotek som automatiskt skapar mappningslogik mellan klasser – till exempel mellan entiteter och DTOs. Istället för att skriva `toDto()`-metoder för hand genererar MapStruct implementationerna åt dig vid kompilering.

---

## Varför MapStruct?

I `arbeta_med_dtos.md` skriver vi mappningen manuellt:

```java
private ProductDto toDto(Product product) {
    return new ProductDto(
        product.getId(),
        product.getName(),
        product.getPrice()
    );
}
```

Det fungerar utmärkt för enkla klasser. Men när entiteterna växer – fler fält, nästlade objekt, listor – blir de här metoderna långa och repetitiva. MapStruct löser det genom att generera dem automatiskt baserat på ett interface du definierar.

**Fördelen** är att koden kompileras och typkontrolleras. Om ett fält byter namn märker du det direkt som ett kompileringsfel, inte som en bugg i produktion.

---

## Lägga till MapStruct i pom.xml

Lägg till beroende och annotation processor i `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.5.5.Final</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Ladda om Maven-projektet i IntelliJ efter att du sparat filen.

---

## Ett enkelt mapper-interface

Skapa ett interface i paketet `mapper` och annotera det med `@Mapper`:

```java
package se.dittnamn.mittprojekt.mapper;

import org.mapstruct.Mapper;
import se.dittnamn.mittprojekt.dto.ProductDto;
import se.dittnamn.mittprojekt.model.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto toDto(Product product);

    Product toEntity(ProductDto dto);
}
```

`componentModel = "spring"` gör att MapStruct registrerar den genererade klassen som en Spring-bean. Du kan sedan injicera mappern precis som en service eller ett repository.

MapStruct matchar fält automatiskt på namn. `product.getName()` mappas till `dto.getName()` utan att du behöver säga det explicit.

---

## Använda mappern i service-lagret

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductDto> getAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .toList();
    }

    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produkt med id " + id + " hittades inte"));
        return productMapper.toDto(product);
    }

    public ProductDto create(ProductDto dto) {
        Product product = productMapper.toEntity(dto);
        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }
}
```

Service-lagret innehåller nu ingen mappningslogik – det delegeras till mappern.

---

## När fältnamnen skiljer sig

Om entitetens fält heter något annat än DTOns fält används `@Mapping`:

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "customer.fullName", target = "customerName")
    @Mapping(source = "totalAmount", target = "price")
    OrderDto toDto(Order order);
}
```

`source` är fältet i källklassen, `target` är fältet i målklassen. Du kan även navigera i nästlade objekt med punktnotation som ovan.

---

## Ignorera fält

Vill du att ett fält i målklassen lämnas orört – till exempel ett `id` som inte ska sättas vid skapande:

```java
@Mapping(target = "id", ignore = true)
ProductDto toDto(Product product);
```

---

## Mappning av listor

MapStruct genererar automatiskt en listmappning om du deklarerar den:

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto toDto(Product product);

    List<ProductDto> toDtoList(List<Product> products);
}
```

`toDtoList` anropar `toDto` för varje element i listan. Du behöver inte implementera den.

---

## Var genereras koden?

MapStruct genererar en konkret klass vid kompilering. Den hamnar i `target/generated-sources/annotations/`. Du ser aldrig den i din källkod men IntelliJ kan visa den om du öppnar den mappen. Det är den klassen Spring injicerar när du begär en `ProductMapper`.

---

## Paketstruktur

```
se/dittnamn/mittprojekt/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
└── mapper/          ← mapper-interface läggs här
```

---

## Snabbref – vanliga annotations

| Annotation | Används för |
|---|---|
| `@Mapper(componentModel = "spring")` | Deklarera ett mapper-interface som Spring-bean |
| `@Mapping(source, target)` | Mappa fält med olika namn |
| `@Mapping(target, ignore = true)` | Hoppa över ett fält |
| `@BeanMapping(nullValuePropertyMappingStrategy = ...)` | Styr beteende vid null-värden |
