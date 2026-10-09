# Roller och behörigheter

När autentisering är på plats – att användaren kan logga in – är nästa steg auktorisering: vad får användaren faktiskt göra? Med roller kan du styra åtkomst på både endpoint- och metodnivå.

---

## Roller i databasen

En roll sparas som en sträng på användaren. Spring Security kräver att rollnamn börjar med prefixet `ROLE_`:

```
ROLE_USER    – vanlig användare
ROLE_ADMIN   – administratör
```

Det är konvention, inte valfritt. Metoder som `hasRole("ADMIN")` lägger till prefixet automatiskt – de letar alltså efter `ROLE_ADMIN` i databasen.

---

## Styra åtkomst i SecurityFilterChain

Det enklaste sättet att skydda endpoints är direkt i `SecurityFilterChain`. Reglerna läses uppifrån och ner – den första som matchar gäller:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/todos/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(Customizer.withDefaults());

    return http.build();
}
```

Ordningen spelar roll. `anyRequest().authenticated()` ska alltid vara sist – annars blockerar den reglerna under.

---

## Styra åtkomst med @PreAuthorize

För mer detaljerad kontroll kan du sätta behörighetskrav direkt på enskilda metoder med `@PreAuthorize`. Det kräver att du aktiverar det i konfigurationsklassen:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // ...
}
```

Sen kan du använda det på valfri controller- eller service-metod:

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    todoService.delete(id);
    return ResponseEntity.noContent().build();
}

@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@GetMapping
public ResponseEntity<List<TodoDto>> getAll() {
    return ResponseEntity.ok(todoService.getAll());
}
```

**SecurityFilterChain vs @PreAuthorize – när använder man vad?**

Använd `SecurityFilterChain` för breda regler som gäller hela URL-mönster. Använd `@PreAuthorize` när du vill ha finkorning kontroll per metod, eller när logiken är mer komplex. Det är vanligt att kombinera båda.

---

## Returnera rätt statuskod

När en användare inte är inloggad returnerar Spring Security `401 Unauthorized`. När användaren är inloggad men saknar rätt roll returneras `403 Forbidden`. Det är en viktig distinktion:

| Kod | Betydelse |
|---|---|
| 401 Unauthorized | Inte inloggad |
| 403 Forbidden | Inloggad men saknar behörighet |

Spring Security hanterar detta automatiskt – du behöver inte göra något aktivt.

---

## Hämta inloggad användare

Ibland behöver du veta vem som är inloggad, t.ex. för att bara returnera den inloggade användarens todos. Du kan hämta det från `SecurityContextHolder`:

```java
@GetMapping("/me")
public ResponseEntity<String> whoAmI() {
    String username = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
    return ResponseEntity.ok(username);
}
```

Eller ta emot det som parameter direkt i metoden med `@AuthenticationPrincipal`:

```java
@GetMapping("/me")
public ResponseEntity<String> whoAmI(@AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userDetails.getUsername());
}
```

Båda sätten fungerar. `@AuthenticationPrincipal` är renare och enklare att testa.

---

## Testa i Postman

Logga in som en användare med rollen `ROLE_USER` och försök sedan nå en endpoint som kräver `ROLE_ADMIN`:

```
DELETE http://localhost:8080/todos/1
→ 403 Forbidden
```

Logga in som admin och försök igen:

```
DELETE http://localhost:8080/todos/1
→ 204 No Content
```

---

## Nästa steg

Applikationen har nu autentisering och auktorisering med rollbaserad åtkomststyrning. Nästa steg är att byta ut den sessionsbaserade autentiseringen mot JWT, vilket gör API:et stateless och bättre lämpat för att kommunicera med en separat frontend.
