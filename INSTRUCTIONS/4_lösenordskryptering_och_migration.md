# Lösenordskryptering och databasmigration

När du lägger till Spring Security i ett befintligt projekt räcker det inte att bara konfigurera BCrypt – du behöver också se till att databasen är i rätt skick. Den här guiden tar dig igenom vad som behöver göras och i vilken ordning.

---

## Vad BCrypt kräver av databasen

Två saker måste stämma innan autentisering fungerar:

1. **`users`-tabellen måste ha en `role`-kolumn** – Spring Security behöver veta vilken roll användaren har
2. **Lösenorden i `password`-kolumnen måste vara BCrypt-hashade** – klartext fungerar inte när `BCryptPasswordEncoder` är konfigurerad

Om någon av dessa saknas får du antingen ett felmeddelande vid uppstart eller ett kryptiskt `401` vid inloggningsförsök.

---

## Steg 1 – Lägg till role-kolumnen

Kör följande SQL mot din databas. Anpassa tabellnamnet om det skiljer sig:

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER';
```

Alla befintliga användare får rollen `ROLE_USER` automatiskt. Om du vill att vissa användare ska vara admins uppdaterar du dem manuellt efteråt:

```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE username = 'admin';
```

Verifiera att kolumnen finns och har rätt värden:

```sql
SELECT id, username, role FROM users;
```

---

## Steg 2 – Hantera befintliga lösenord

Det här är den knepiga delen. BCrypt är enkelriktad – det går inte att ta ett lagrat klartext-lösenord och hasha det utan att känna till det ursprungliga värdet. Du kan inte bara köra ett SQL-skript som "konverterar" kolumnen.

Du har tre alternativ beroende på situation.

---

### Alternativ A – Sätt ett känt temporärt lösenord (enklast)

Om du kontrollerar alla befintliga användarkonton kan du sätta ett känt temporärt lösenord och hasha det:

Generera ett BCrypt-hash. Lägg temporärt till den här endpointen i din applikation och anropa den en gång:

```java
@GetMapping("/generate-hash")
public String generateHash() {
    return new BCryptPasswordEncoder().encode("temporärt123");
}
```

Kopiera hashen från svaret och kör:

```sql
UPDATE users SET password = '$2a$10$...' WHERE 1=1;
```

Ta sedan bort endpointen direkt – den ska inte finnas kvar.

Informera dina användare om att de måste logga in med det tillfälliga lösenordet och byta det.

---

### Alternativ B – Migrera vid inloggning (mjukare övergång)

Det här är ett vanligt mönster när man inte kan tvinga alla användare att byta lösenord på en gång. Man detekterar vid inloggning om lösenordet är klartext, hashar det då, och sparar det uppdaterade värdet.

Lägg till logik i `UserDetailsServiceImpl` eller din login-hantering:

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Användare hittades inte"));

        // Om lösenordet inte är BCrypt-hashat – hasha och spara det
        if (!user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }
}
```

Varje användare migreras automatiskt nästa gång de loggar in. Efter ett tag när alla användare loggat in kan du ta bort migrationslogiken.

**Viktigt:** det här är en övergångslösning. Ta bort koden när migrationen är klar.

---

### Alternativ C – Tvinga lösenordsbyte

Om du inte känner till några befintliga lösenord och inte vill använda övergångslösningen kan du markera alla användare som att de måste byta lösenord. Det kräver ett extra fält på User-entiteten och logik för att hantera det flödet – mer komplext men det renaste alternativet på sikt.

För de flesta projekt på den här nivån räcker Alternativ A eller B.

---

## Steg 3 – Konfigurera BCryptPasswordEncoder

Lägg till beanen i `SecurityConfig` om du inte redan gjort det:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Spring injicerar denna automatiskt i `UserDetailsServiceImpl` och i din registreringslogik.

---

## Steg 4 – Uppdatera registrering

Se till att alla nya användare som skapas via API:et får hashade lösenord:

```java
@PostMapping("/auth/register")
public ResponseEntity<String> register(@RequestBody RegisterDto dto) {
    User user = new User(
        dto.getUsername(),
        passwordEncoder.encode(dto.getPassword()),  // aldrig klartext
        "ROLE_USER"
    );
    userRepository.save(user);
    return ResponseEntity.status(201).body("Användare skapad");
}
```

---

## Steg 5 – Verifiera att allt fungerar

Starta applikationen och testa login-endpointen:

```
POST /auth/login
Body: {"username": "din-användare", "password": "ditt-lösenord"}
```

Om du får `401` tillbaka trots korrekta uppgifter är det nästan alltid ett av tre problem:

| Problem | Symptom | Lösning |
|---|---|---|
| Lösenordet är inte hashat i databasen | 401 vid korrekt lösenord | Kör migration enligt ovan |
| `role`-kolumnen saknas | Fel vid uppstart eller 401 | Kör ALTER TABLE |
| Rollen saknar `ROLE_`-prefix | 403 vid alla requests | Uppdatera värdet i databasen |

---

## Testdata i data.sql

Om du använder H2 eller kör `create-drop` och har en `data.sql`-fil behöver du uppdatera dina INSERT-satser med hashade lösenord. Nedan är hashen för lösenordet `hemligt`:

```sql
INSERT INTO users (username, password, role)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ROLE_ADMIN');

INSERT INTO users (username, password, role)
VALUES ('user', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ROLE_USER');
```

Vill du använda ett annat lösenord genererar du hashen med hjälp av `BCryptPasswordEncoder` – se Alternativ A ovan.
