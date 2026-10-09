# Webbsäkerhet – grunderna

När du bygger ett webb-API exponerar du din applikation mot omvärlden. Det finns ett antal välkända angreppssätt som angripare använder, och det är viktigt att känna till dem – även om du inte behöver lösa alla på en gång.

Den här guiden fokuserar på de hot du i stor utsträckning är skyddad mot redan när du använder Spring Boot med Spring Security och Spring Data JPA. Andra hot, som XSS och brute force, återkommer vi till i en senare modul.

---

## SQL Injection

En angripare skickar in SQL-kod som indata i syfte att manipulera dina databasfrågor.

**Exempel på ett sårbart anrop:**
```
GET /users?name=admin' OR '1'='1
```

Om man bygger SQL-frågor med strängkonkatenering kan det resultera i att hela `users`-tabellen returneras, eller att data raderas.

**Hur Spring Data JPA skyddar dig:**

Spring Data JPA använder alltid *prepared statements* under huven. Det innebär att indata aldrig tolkas som SQL – det behandlas alltid som ett värde, aldrig som kod.

```java
// Det här är säkert – JPA hanterar parametern korrekt
userRepository.findByUsername(username);
```

Du är skyddad så länge du använder JPA:s metoder och inte skriver rå SQL med strängkonkatenering. Om du någonsin använder `@Query` med native SQL, se till att alltid använda parametrar:

```java
// Säkert – använder parameter
@Query(value = "SELECT * FROM users WHERE username = :username", nativeQuery = true)
Optional<User> findByUsername(@Param("username") String username);

// Osäkert – konkatenering, gör aldrig så här
@Query(value = "SELECT * FROM users WHERE username = '" + username + "'", nativeQuery = true)
```

---

## CSRF – Cross-Site Request Forgery

En inloggad användare besöker en skadlig webbsida. Den skadliga sidan skickar i bakgrunden en request till din backend – och webbläsaren skickar automatiskt med användarens session-cookie.

Utan skydd kan angriparen alltså utföra handlingar i användarens namn, som att ändra lösenord eller göra betalningar.

**Hur Spring Security skyddar dig:**

Spring Security aktiverar CSRF-skydd automatiskt. Det fungerar genom att varje formulär eller request måste skicka med ett unikt token som bara den legitima sidan känner till. Den skadliga sidan har inte tillgång till det tokenet och kan därför inte förfalska requesten.

**Viktigt undantag med JWT:**

När du bygger ett REST API med JWT och stateless sessioner stänger du av CSRF-skyddet. Det är säkert att göra eftersom CSRF-attacken förutsätter att webbläsaren automatiskt skickar med autentisering – vilket den gör med cookies, men *inte* med en Authorization-header. JWT skickas manuellt i varje request, så CSRF är inte relevant.

```java
http.csrf(csrf -> csrf.disable()); // OK när man använder JWT
```

---

## Säkerhetsheaders

Webbläsare har inbyggda säkerhetsfunktioner som aktiveras via HTTP-headers. Spring Security lägger automatiskt till flera av dessa när du inkluderar det som beroende.

Några exempel på vad som sätts automatiskt:

| Header | Vad den gör |
|---|---|
| `X-Content-Type-Options: nosniff` | Hindrar webbläsaren från att gissa innehållstyp |
| `X-Frame-Options: DENY` | Förhindrar att din sida laddas i en iframe (skydd mot clickjacking) |
| `Cache-Control: no-cache` | Förhindrar att känsliga svar cachas |

Du behöver inte göra något för att få dessa – de kommer med Spring Security automatiskt.

---

## Lösenordshantering med BCrypt

Lösenord ska aldrig lagras i klartext i databasen. Om databasen läcker har angriparen direkt tillgång till alla användares lösenord – och eftersom många återanvänder lösenord kan det få konsekvenser långt bortom din app.

BCrypt är en hashfunktion designad specifikt för lösenord. Den har tre viktiga egenskaper:

**Enkelriktad** – ett hashat lösenord kan inte reverseras tillbaka till klartexten.

**Saltat** – ett slumpmässigt värde (salt) blandas in i varje hash. Det innebär att två användare med samma lösenord får olika hash i databasen, och att förberäknade attacktabeller (rainbow tables) inte fungerar.

**Långsamt med avsikt** – BCrypt är designat för att ta tid att beräkna, vilket gör brute force-attacker opraktiska.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

```java
// Vid registrering – spara aldrig råa lösenord
String hashedPassword = passwordEncoder.encode(rawPassword);
user.setPassword(hashedPassword);

// Vid inloggning – jämför råa lösenordet med hashen
passwordEncoder.matches(rawPassword, storedHash); // returnerar true/false
```

Samma lösenord hashat två gånger ger olika resultat – men `matches()` fungerar ändå korrekt eftersom saltet är inbakat i hashen.

---

## Sammanfattning

| Hot | Skydd | Var det sker |
|---|---|---|
| SQL Injection | Prepared statements | Spring Data JPA automatiskt |
| CSRF | CSRF-token | Spring Security automatiskt (ej relevant med JWT) |
| Clickjacking m.fl. | Säkerhetsheaders | Spring Security automatiskt |
| Lösenordsläckage | BCrypt-hashing | Du konfigurerar, Spring tillhandahåller |

De hot som inte täcks här – som XSS och brute force – kräver mer aktiva åtgärder och återkommer vi till senare.
