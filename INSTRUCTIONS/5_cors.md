# Konfigurera CORS

När din frontend och backend körs på olika portar – vilket är det normala under utveckling – blockerar webbläsaren requestet. Det är inte ett fel i din kod, det är ett inbyggt säkerhetsbeteende i webbläsaren som kallas Same-Origin Policy.

CORS (Cross-Origin Resource Sharing) är mekanismen som låter dig explicit tala om för webbläsaren vilka origins som får prata med din backend.

---

## Vad är en origin?

En origin består av protokoll, domän och port. Två URLs har samma origin bara om alla tre stämmer överens:

| URL | Origin |
|---|---|
| `http://localhost:5173` | `http://localhost:5173` |
| `http://localhost:8080` | `http://localhost:8080` |

En React-app på port 5173 och en Spring Boot-app på port 8080 har alltså olika origins – därav CORS-felet.

---

## Felmeddelandet

Utan CORS-konfiguration ser du det här i webbläsarens konsol:

```
Access to fetch at 'http://localhost:8080/todos' from origin 
'http://localhost:5173' has been blocked by CORS policy
```

Notera att det är webbläsaren som blockerar – requesten når faktiskt fram till servern, men svaret blockeras på väg tillbaka. Det är därför anropet kan fungera i Postman men inte från din frontend.

---

## Konfigurera CORS i Spring Security

Lägg till en `CorsConfigurationSource`-bean i `SecurityConfig`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Aktivera den i `SecurityFilterChain`:

```java
http
    .cors(Customizer.withDefaults())
    .csrf(csrf -> csrf.disable())
    // ...
```

`Customizer.withDefaults()` plockar automatiskt upp din `CorsConfigurationSource`-bean.

---

## Tillåta flera origins

Under utveckling kan du behöva tillåta flera origins, t.ex. om teammedlemmar kör på olika portar:

```java
config.setAllowedOrigins(List.of(
    "http://localhost:5173",
    "http://localhost:3000"
));
```

I produktion byter du ut localhost mot din faktiska domän:

```java
config.setAllowedOrigins(List.of("https://dinapp.se"));
```

Undvik `*` som wildcard för origins i produktion – det tillåter alla domäner att prata med din backend.

---

## Origins i application.properties

Origins är något som förändras mellan miljöer – under utveckling är det localhost, i produktion är det din riktiga domän. Därför passar det bra att lägga i `application.properties`:

```properties
cors.allowed-origins=http://localhost:5173
```

```java
@Value("${cors.allowed-origins}")
private String allowedOrigins;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigins));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

---

## Preflight-requests

Webbläsaren skickar ibland ett förberedande `OPTIONS`-request innan det faktiska anropet – ett så kallat preflight. Det kontrollerar att CORS tillåts innan webbläsaren skickar det riktiga anropet.

Spring Security hanterar preflight-requests automatiskt när CORS är konfigurerat. Du behöver se till att `OPTIONS` finns med i `setAllowedMethods`, vilket det gör i exemplen ovan.

---

## Nästa steg

Backend är nu redo att ta emot anrop från en frontend. I nästa guide tittar vi på hur frontend hanterar JWT – hur man loggar in, sparar token och skickar med den på varje request.
