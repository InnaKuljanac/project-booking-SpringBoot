# Byt till JWT

Hittills har applikationen använt sessionsbaserad autentisering. I den här guiden byter vi ut det mot JWT – JSON Web Tokens. Det gör API:et stateless, vilket innebär att servern inte längre behöver hålla koll på inloggade användare. All information som behövs finns i token.

---

## Vad är en JWT?

En JWT är en sträng med tre base64-kodade delar separerade med punkter:

```
header.payload.signature
```

- **Header** – vilken algoritm som används, t.ex. HS256
- **Payload** – data om användaren, t.ex. användarnamn, roll och när token går ut
- **Signature** – en kontrollsumma skapad med en hemlig nyckel

Servern kan verifiera token utan att slå upp något i databasen – den beräknar om signaturen och jämför. Om någon har manipulerat token stämmer signaturen inte längre.

**Flödet:**
1. Användaren loggar in med username + password
2. Servern verifierar och returnerar en JWT
3. Klienten sparar token och skickar `Authorization: Bearer <token>` på varje request
4. Servern validerar token i ett filter och vet vem användaren är

---

## Steg 1 – Lägg till beroenden

Lägg till i `pom.xml`:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

---

## Steg 2 – Konfigurera i application.properties

```properties
# Ersätt med ett slumpmässigt värde på minst 32 tecken, lägg aldrig detta i git i produktion
jwt.secret=byt-ut-mig-mot-nagot-slumpmassigt-och-langt-nog
jwt.expiration=3600000
```

`jwt.expiration` anges i millisekunder. 3600000 = 1 timme.

---

## Steg 3 – Skapa JwtService

`JwtService` ansvarar för att generera och validera tokens. Skapa den i paketet `service`:

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiry = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiry.before(new Date());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

---

## Steg 4 – Skapa JwtAuthFilter

Filtret körs på varje inkommande request. Det letar efter en JWT i `Authorization`-headern, validerar den och sätter användaren i Spring Securitys kontext om token är giltig.

Skapa `JwtAuthFilter` i paketet `security` (skapa paketet om det inte finns):

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

Filtret gör ingenting om headern saknas – det låter requesten passera vidare och Spring Security hanterar åtkomstreglerna som vanligt.

---

## Steg 5 – Uppdatera SecurityConfig

Tre saker ändras jämfört med den sessionsbaserade konfigurationen: CSRF stängs av, sessioner stängs av, och JWT-filtret läggs till:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

`AuthenticationManager` behövs för att verifiera username och password i login-endpointen.

---

## Steg 6 – Skapa login-endpoint

Lägg till login i `AuthController`:

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
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

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDto dto) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getUsername());
        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(token);
    }
}
```

Skapa `LoginDto` i paketet `dto`:

```java
public class LoginDto {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
```

`authenticationManager.authenticate()` kastar ett undantag om credentials är fel – Spring Security returnerar då automatiskt 401.

---

## Steg 7 – Hantera utgångna tokens

När en token har gått ut returnerar Spring Security 401. Du kan anpassa felmeddelandet med en egen `AuthenticationEntryPoint`:

```java
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Token saknas eller har gått ut\"}");
    }
}
```

Koppla in den i `SecurityConfig`:

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(jwtAuthEntryPoint)
)
```

---

## Testa i Postman

**Registrera en användare:**

```
POST http://localhost:8080/auth/register
Body: {"username": "anna", "password": "hemligt"}
→ 201 Created
```

**Logga in och hämta token:**

```
POST http://localhost:8080/auth/login
Body: {"username": "anna", "password": "hemligt"}
→ 200 OK
→ eyJhbGciOiJIUzI1NiJ9...
```

**Använd token i ett skyddat anrop:**

Lägg till en header i Postman:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

```
GET http://localhost:8080/todos
→ 200 OK
```

**Testa utan token:**

```
GET http://localhost:8080/todos
→ 401 Unauthorized
```

---

## Nästa steg

API:et kommunicerar nu via JWT. Nästa steg är att konfigurera CORS så att en frontend på en annan port kan prata med backend.
