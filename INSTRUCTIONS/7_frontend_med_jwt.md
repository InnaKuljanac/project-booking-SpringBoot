# Frontend med JWT

Den här guiden visar hur en frontend kommunicerar med ett JWT-skyddat API. Vi tittar på login-flödet, hur token sparas, och ett mönster för att slippa upprepa auth-logik i varje fetch-anrop.

---

## Login

Skicka ett POST-request med användarnamn och lösenord. Backend returnerar en JWT som en sträng:

```javascript
async function login(username, password) {
    const response = await fetch("http://localhost:8080/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
    });

    if (!response.ok) {
        throw new Error("Fel användarnamn eller lösenord");
    }

    const token = await response.text();
    localStorage.setItem("token", token);
}
```

---

## Skyddade anrop – problemet med upprepning

När token finns behöver varje request till ett skyddat endpoint skicka med den i `Authorization`-headern:

```javascript
// Hämta todos
const response = await fetch("http://localhost:8080/todos", {
    headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${localStorage.getItem("token")}`
    }
});

// Skapa todo
const response = await fetch("http://localhost:8080/todos", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${localStorage.getItem("token")}`
    },
    body: JSON.stringify(newTodo)
});

// Ta bort todo
const response = await fetch(`http://localhost:8080/todos/${id}`, {
    method: "DELETE",
    headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${localStorage.getItem("token")}`
    }
});
```

Det fungerar, men varje anrop upprepar samma header-logik. I ett riktigt projekt kan det bli dussintals anrop. När något behöver ändras – t.ex. hur token hämtas eller hur 401 hanteras – måste du hitta och ändra på varje enskilt ställe.

---

## apiFetch – lös det på ett ställe

Lösningen är en wrapper-funktion som lägger till auth-headern automatiskt och hanterar 401 centralt:

```javascript
async function apiFetch(url, options = {}) {
    const token = localStorage.getItem("token");

    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(token && { "Authorization": `Bearer ${token}` }),
            ...options.headers
        }
    });

    if (response.status === 401) {
        localStorage.removeItem("token");
        window.location.href = "/login";
        return;
    }

    return response;
}
```

Nu ser alla anrop ut så här istället:

```javascript
// Hämta todos
const response = await apiFetch("http://localhost:8080/todos");

// Skapa todo
const response = await apiFetch("http://localhost:8080/todos", {
    method: "POST",
    body: JSON.stringify(newTodo)
});

// Ta bort todo
const response = await apiFetch(`http://localhost:8080/todos/${id}`, {
    method: "DELETE"
});
```

Auth-logiken finns på ett ställe. Vill du ändra hur token hanteras eller lägga till mer felhantering ändrar du i `apiFetch` – ingenting annat behöver röras.

Lägg märke till att `Authorization`-headern bara läggs till om token finns – `...(token && { ... })`. Det betyder att samma funktion fungerar för publika endpoints som `/auth/login` och `/auth/register` också.

---

## Logga ut

Ta bort token från localStorage och skicka användaren till login-sidan:

```javascript
function logout() {
    localStorage.removeItem("token");
    window.location.href = "/login";
}
```

---

## Kontrollera om användaren är inloggad

En enkel kontroll för att t.ex. dölja/visa navigeringslänkar eller skydda sidor:

```javascript
function isLoggedIn() {
    return localStorage.getItem("token") !== null;
}
```

Det kontrollerar bara om token finns, inte om den fortfarande är giltig. För att kontrollera om token har gått ut behöver du antingen avkoda den på klienten eller vänta på att backend returnerar 401.

---

## Hantera utgångna tokens

Token har en begränsad livslängd. När den går ut returnerar backend 401. `apiFetch` hanterar det redan – användaren loggas ut och omdirigeras till login-sidan automatiskt.

Det är det enklaste sättet att hantera utgångna tokens. I mer avancerade system använder man refresh tokens för att hämta en ny access token tyst i bakgrunden utan att användaren märker något – men det är utanför scope för den här guiden.

---

## localStorage – en notering om säkerhet

`localStorage` är enkelt och fungerar bra för utveckling och de flesta projekt. Det finns dock en sårbarhet att känna till: om en angripare lyckas köra JavaScript på din sida (XSS) kan de läsa token från localStorage.

Det säkrare alternativet är att lagra token i en `httpOnly`-cookie, som inte kan läsas av JavaScript alls. Det kräver mer konfiguration på serversidan och är mer komplext att sätta upp. Vi återkommer till det i en senare modul om avancerad säkerhet.

---

## Komplett exempel

```javascript
// auth.js

async function login(username, password) {
    const response = await fetch("http://localhost:8080/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
    });

    if (!response.ok) throw new Error("Fel användarnamn eller lösenord");

    const token = await response.text();
    localStorage.setItem("token", token);
}

function logout() {
    localStorage.removeItem("token");
    window.location.href = "/login";
}

function isLoggedIn() {
    return localStorage.getItem("token") !== null;
}

async function apiFetch(url, options = {}) {
    const token = localStorage.getItem("token");

    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(token && { "Authorization": `Bearer ${token}` }),
            ...options.headers
        }
    });

    if (response.status === 401) {
        localStorage.removeItem("token");
        window.location.href = "/login";
        return;
    }

    return response;
}
```

Importera och använd `apiFetch` överallt i appen där du kommunicerar med backend.
