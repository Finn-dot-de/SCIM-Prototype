# Demo-Material für den SCIM-Prototypen

Alle Beispiele in diesem Ordner sind gegen die laufende Implementierung geprüft
(`DemoBeispieleTest` fährt genau diese Dateien durch). Was hier liegt,
funktioniert auch im Vortrag.

## Womit vorführen

**Empfohlen: `scim-demo.http` in IntelliJ.** Die Datei enthält den kompletten
Ablauf, gegliedert in sechs Akte. Requests einfach von oben nach unten mit dem
grünen Pfeil ausführen — die erzeugten IDs übernimmt der HTTP-Client automatisch
in die Folge-Requests, es muss nichts kopiert werden. Einige Schritte prüfen ihr
Ergebnis selbst und zeigen ein grünes Häkchen; das wirkt in einer Vorführung
besser als Statuscodes vorzulesen.

Alternativ lassen sich die `.json`-Dateien in Postman oder per `curl` verwenden:

```bash
curl -X POST http://localhost:8080/scim/v2/Users -H "Content-Type: application/scim+json" -d @TestJSON/01-user-anlegen.json
```

Voraussetzung: Anwendung läuft (`mvn spring-boot:run`) und PostgreSQL ist
erreichbar. Swagger UI liegt unter `http://localhost:8080/swagger-ui.html`.

## Der Ablauf und was jeder Akt zeigt

| Akt | Inhalt | Der Punkt für das Publikum |
|-----|--------|----------------------------|
| 1 | Duplikat-Prüfung, Anlegen, erneutes Anlegen | Genau diese Reihenfolge fahren Okta und Entra beim Provisioning. Der zweite Versuch liefert **409 mit `scimType: uniqueness`** — das erwartet ein IdP. |
| 2 | Suchen und Filtern | Es wird der **vollständige SCIM-Filter** ausgewertet, nicht nur `userName eq`. Akt 2.3 kombiniert `co` und `eq` mit `and`. |
| 3 | PATCH mit vier Operationen | Ein IdP schickt Änderungen gebündelt. Danach prüfen: **`meta.created` bleibt, `meta.lastModified` ist neu** — ein PATCH darf die Historie nicht verlieren. |
| 4 | Gruppen anlegen, Mitglieder zuweisen | Users und Groups teilen dieselbe Logik: derselbe Duplikat-Check, dasselbe Fehlerformat. |
| 5 | Offboarding | Ein Austritt führt beim IdP fast immer zu **`active=false`**, nicht zu DELETE. Beides wird gezeigt. |
| 6 | Fehlerformat | Für ein Fachpublikum der interessanteste Teil — siehe unten. |

## Warum Akt 6 der wichtigste ist

Ein SCIM-Server ist nicht dadurch SCIM-konform, dass er JSON über REST spricht,
sondern dadurch, dass er sich **auch im Fehlerfall** an RFC 7644 hält. Kommt
statt einer SCIM-`ErrorResponse` das Standard-JSON von Spring zurück, kann der
IdP damit nichts anfangen.

Fünf Fälle werden gezeigt, alle mit `Content-Type: application/scim+json` und
dem Schema `urn:ietf:params:scim:api:messages:2.0:Error`:

- **400** `invalidValue` — Pflichtfeld `userName` fehlt
- **400** `invalidFilter` — kaputte Filter-Syntax. Wichtig: Der Server meldet
  einen Fehler statt einer leeren Trefferliste. Eine leere Liste würde der IdP
  als „Benutzer existiert nicht" deuten und ihn ein zweites Mal anlegen.
- **404** — unbekannte, aber gültige ID
- **400** — ID ist keine UUID, wird abgefangen bevor die Datenbank sie sieht
- **405** — nicht erlaubte Methode. Dieser Fall hat *keinen* eigenen Handler; er
  fällt aus der Vererbung von Springs `ResponseEntityExceptionHandler`. Wer
  danach fragt, bekommt die interessantere Antwort.

## Die Dateien

| Datei | Verwendung |
|-------|------------|
| `scim-demo.http` | Kompletter Ablauf für den IntelliJ HTTP-Client |
| `01-user-anlegen.json` | Vollständiger Benutzer: Name, Kontakt, Adresse, Rollen, Enterprise-Extension |
| `02-user-anlegen-zweiter.json` | Zweiter, schlanker Benutzer für die Filter-Demo |
| `03-user-patch-befoerderung.json` | PATCH mit vier Operationen (`replace`, `add`, Extension) |
| `04-user-patch-deaktivieren.json` | Die Standard-Offboarding-Operation |
| `05-gruppe-anlegen.json` | Gruppe, mit Umlaut im Namen |
| `06-gruppe-patch-mitglied-hinzufuegen.json` | Mitglieder zuweisen — hier `HIER-USER-ID-EINSETZEN` ersetzen (in `scim-demo.http` passiert das automatisch) |
| `90-fehler-user-ohne-username.json` | Absichtlich ungültig, für Akt 6 |

## Zwei Details, auf die man vorbereitet sein sollte

**Umlaute.** `01-user-anlegen.json` enthält „Musterstraße" und „Beispielbehörde",
die Gruppe heißt „Sachbearbeitung Asyl – Prüfgruppe". Dass das den Weg durch
JSONB und zurück unbeschadet übersteht, ist geprüft — die Frage kommt erfahrungs-
gemäß.

**`groups` beim Benutzer.** Das Attribut ist laut RFC 7643 `readOnly` und wird
aus der Gruppenmitgliedschaft abgeleitet, nicht beim Anlegen mitgeschickt.
Deshalb steht es bewusst nicht in `01-user-anlegen.json`. Dieser Prototyp leitet
es noch nicht ab — falls jemand fragt, ist das ein offener Punkt und kein Fehler
in den Beispielen.
