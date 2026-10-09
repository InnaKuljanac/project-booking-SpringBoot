# Fördjupning – DTOs och problemen de löser

Introduktionsguiden visade hur man skriver en DTO och mappar den i service-lagret. Den här guiden förklarar *varför* det faktiskt är nödvändigt – vad som händer om du inte använder DTOs, och hur du tänker kring DTOs när entiteterna har relationer.

---

## Vad händer om du returnerar entiteter direkt?

Låt säga att du returnerar en entitet direkt från en controller-metod:

```java
@GetMapping("/{id}")
public ResponseEntity<Product> getById(@PathVariable Long id) {
    return ResponseEntity.ok(productRepository.findById(id).orElseThrow());
}
```

Det kompilerar. Det kanske till och med fungerar i ett enkelt scenario. Men det finns flera problem.

### Problem 1 – Du exponerar databasens struktur

Klienten ser exakt hur din databas ser ut – alla kolumnnamn, alla relationer, alla interna flaggor. Om du lägger till ett fält i entiteten dyker det upp i API-svaret automatiskt. Det är inte ett beteende du vill ha. API:ets form bör vara ett medvetet beslut, inte en biprodukt av databasens struktur.

### Problem 2 – Känslig data läcker

En `User`-entitet har troligtvis ett `passwordHash`-fält. Returnerar du entiteten direkt så följer det med i JSON-svaret. Det är ett säkerhetshål.

### Problem 3 – Cirkulära references kraschar Jackson

Det här är det vanligaste akuta problemet. Anta att du har en `Order` med en lista av `OrderItem`, och varje `OrderItem` har en referens tillbaka till `Order`:

```java
@Entity
public class Order {
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
}

@Entity
public class OrderItem {
    @ManyToOne
    private Order order;
}
```

När Jackson försöker serialisera en `Order` till JSON händer följande:
- Serialisera `Order` → serialisera `items` → serialisera varje `OrderItem` → serialisera `order`-fältet på `OrderItem` → serialisera `Order` igen → och så vidare i all evighet

Resultatet är antingen ett `StackOverflowError` eller ett oändligt JSON-dokument. Applikationen kraschar eller hänger sig.

### Problem 4 – Lazy loading-undantag

JPA laddar som standard relaterade entiteter *lazy* – det vill säga inte förrän du faktiskt accessar dem. Men när Hibernate-sessionen är stängd (vilket den är när Jackson försöker serialisera svaret utanför service-lagret) kastar Spring ett `LazyInitializationException`.

---

## DTOs löser alla dessa problem

En DTO är en vanlig Java-klass utan JPA-annotations och utan kopplingar till andra entiteter. Jackson kan serialisera den utan problem. Du bestämmer exakt vilka fält som är med. Sessionen spelar ingen roll – DTOn har redan all data.

---

## DTOs med relationer

När entiteterna har relationer behöver du tänka på vad klienten faktiskt behöver se. Du behöver inte ta med hela objektgrafen – bara det som är relevant.

### Scenario: Order med tillhörande rader

```java
@Entity
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerName;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
}

@Entity
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
```

Skapa en DTO för `OrderItem` som *inte* innehåller en referens tillbaka till `Order`:

```java
public class OrderItemDto {
    private Long id;
    private String productName;
    private int quantity;

    public OrderItemDto(Long id, String productName, int quantity) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
    }

    // getters
    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
}
```

Och en DTO för `Order` som använder `OrderItemDto` – inte entiteten:

```java
public class OrderDto {
    private Long id;
    private String customerName;
    private List<OrderItemDto> items;

    public OrderDto(Long id, String customerName, List<OrderItemDto> items) {
        this.id = id;
        this.customerName = customerName;
        this.items = items;
    }

    // getters
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public List<OrderItemDto> getItems() { return items; }
}
```

Inga cirklar. Inga sessioner. Jackson är nöjd.

### Mappning i service-lagret

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderDto getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order " + id + " hittades inte"));
        return toDto(order);
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(item.getId(), item.getProductName(), item.getQuantity()))
                .toList();

        return new OrderDto(order.getId(), order.getCustomerName(), itemDtos);
    }
}
```

Mappningen sker i service-lagret medan Hibernate-sessionen fortfarande är aktiv – det är därför lazy-laddade listor som `order.getItems()` kan läsas utan problem.

---

## Hur mycket ska en DTO innehålla?

Det finns inga universalregler, men en bra tumregel:

**Returnera det klienten behöver – inte mer, inte mindre.**

Om en lista-vy av orders bara visar id, kundnamn och antal rader behöver du inte ta med hela `items`-listan. Skapa en `OrderSummaryDto` som bara har de fälten. Sparar bandbredd och minskar risken för att exponera data i onödan.

```java
// Tunn DTO för listor
public class OrderSummaryDto {
    private Long id;
    private String customerName;
    private int itemCount;
    // ...
}

// Fyllig DTO för detaljvy
public class OrderDto {
    private Long id;
    private String customerName;
    private List<OrderItemDto> items;
    // ...
}
```

---

## Request-DTOs vs response-DTOs

När du tar emot data från en klient (POST, PUT) behöver du ofta en annan form än när du skickar data tillbaka. En `CreateOrderDto` behöver till exempel inte ha ett `id`-fält eftersom det inte finns ännu.

```java
// Inkommande vid POST – klienten skickar detta
public class CreateOrderDto {
    private String customerName;
    private List<CreateOrderItemDto> items;
}

// Utgående – API:et returnerar detta
public class OrderDto {
    private Long id;
    private String customerName;
    private List<OrderItemDto> items;
}
```

Separata klasser är tydligare och minskar risken att klienten av misstag kan påverka fält den inte ska ha åtkomst till. För enkla CRUD-resurser utan känslig data kan det räcka med en enda DTO-klass – det är ett avvägningsbeslut.

---

## Sammanfattning

| Problem med entiteter direkt | Hur DTOs löser det |
|---|---|
| Exponerar databasens struktur | Du bestämmer exakt vilka fält som syns |
| Känslig data läcker | Fält du inte lägger i DTOn skickas aldrig ut |
| Cirkulära references kraschar Jackson | DTOs har inga JPA-relationer och skapar ingen cirkel |
| Lazy loading-undantag | Mappningen sker i service-lagret medan sessionen är öppen |
