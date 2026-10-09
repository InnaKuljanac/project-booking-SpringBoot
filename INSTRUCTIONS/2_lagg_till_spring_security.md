# Lägg till Spring Security

Den här guiden tar dig från en oskyddad Spring Boot-applikation till en med fungerande inloggning kopplad till databasen. Vi använder sessionsbaserad autentisering – i en senare guide byter vi ut det mot JWT.

---

## Steg 1 – Lägg till beroendet

Lägg till Spring Security i `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Starta om applikationen. Direkt händer tre saker utan att du skrivit en rad kod:

- Alla endpoints kräver inloggning
- En default login-sida finns på `/login`
- Ett tillfälligt lösenord skrivs ut i terminalen:

```
Using generated security password: a1b2c3d4-...
```

Användarnamnet är `user`. Prova att öppna `http://localhost:8080/todos` – du omdirigeras till login-sidan.

Det är Spring Boots auto-konfiguration som gör allt detta. Så fort du definierar din egen konfiguration tar du över.

---

## Steg 2 – Skapa User-entiteten

Skapa klassen `User` i paketet `model`:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
}
```

Rollen sparas som en sträng, t.ex. `ROLE_USER` eller `ROLE_ADMIN`. Prefixet `ROLE_` krävs av Spring Security.

---

## Steg 3 – Skapa UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

---

## Steg 4 – Konfigurera BCrypt

Skapa konfigurationsklassen `SecurityConfig`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

`PasswordEncoder` måste vara en bean så att Spring kan injicera den där den behövs – både i `UserDetailsService` och i registreringslogiken.

---

## Steg 5 – Implementera UserDetailsService

Spring Security anropar `UserDetailsService` varje gång någon försöker logga in. Din implementation hämtar användaren från databasen och returnerar ett `UserDetails`-objekt som Spring Security kan arbeta med.

Skapa `UserDetailsServiceImpl` i paketet `service`:

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Användare hittades inte: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }
}
```

Du behöver inte koppla ihop den här klassen manuellt – Spring Security hittar den automatiskt eftersom den implementerar `UserDetailsService`.

---

## Steg 6 – Konfigurera SecurityFilterChain

Lägg till `SecurityFilterChain` i `SecurityConfig`. Det är här du bestämmer vilka endpoints som kräver inloggning och vilka som är publika:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(Customizer.withDefaults())
        .logout(Customizer.withDefaults());

    return http.build();
}
```

`/auth/**` är publikt – dit lägger vi senare registrering och login. Allt annat kräver inloggning.

---

## Steg 7 – Registreringsendpoint

För att kunna logga in behöver du kunna skapa användare. Skapa `AuthController` i paketet `controller`:

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDto dto) {
        User user = new User(
            dto.getUsername(),
            passwordEncoder.encode(dto.getPassword()),
            "ROLE_USER"
        );
        userRepository.save(user);
        return ResponseEntity.status(201).body("Användare skapad");
    }
}
```

Skapa `RegisterDto` i paketet `dto`:

```java
public class RegisterDto {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
```

Lösenordet hashas med BCrypt innan det sparas – aldrig i klartext.

---

## Steg 8 – Testdata

Lägg till en testanvändare i `data.sql` så du alltid har någon att logga in med när appen startar. Lösenordet nedan är `hemligt` hashat med BCrypt:

```sql
INSERT INTO users (username, password, role)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ROLE_ADMIN');
```

För att generera en egen hash kan du lägga till en temporär endpoint eller köra detta i ett test:

```java
System.out.println(new BCryptPasswordEncoder().encode("dittlösenord"));
```

---

## Steg 9 – Testa i Postman

**Registrera en ny användare:**

| Fält | Värde |
|---|---|
| Metod | POST |
| URL | `http://localhost:8080/auth/register` |
| Body (JSON) | `{"username": "anna", "password": "hemligt"}` |

**Testa ett skyddat anrop utan inloggning:**

```
GET http://localhost:8080/todos
→ 401 Unauthorized
```

**Logga in via Spring Securitys inbyggda endpoint:**

| Fält | Värde |
|---|---|
| Metod | POST |
| URL | `http://localhost:8080/login` |
| Body (form-data) | `username=anna`, `password=hemligt` |

Spring returnerar en session-cookie (`JSESSIONID`). Postman skickar automatiskt med den på efterföljande anrop.

---

## Hur sessionsbaserad autentisering fungerar

Det här är standardbeteendet i Spring Security:

1. Användaren loggar in → Spring verifierar credentials mot databasen
2. En session skapas på servern och ett sessions-id genereras
3. En cookie (`JSESSIONID`) skickas tillbaka till klienten
4. Klienten skickar cookien på varje efterföljande request
5. Servern slår upp sessionen och vet vem användaren är

Det fungerar bra för traditionella webbappar med en webbläsare som hanterar cookies automatiskt. För REST API:er och mobila klienter, eller när man vill kunna skala applikationen horisontellt, är JWT ett bättre alternativ – vilket vi tittar på i nästa guide.

---

## Nästa steg

Applikationen har nu fungerande autentisering kopplad till databasen. Nästa steg är att lägga till roller och behörigheter för att styra vad olika användare får göra.
