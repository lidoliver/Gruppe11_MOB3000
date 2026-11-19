# Frisbeegolf-app – Gruppe 11 (MOB3000)

Android-app for frisbeegolf, laget som gruppeprosjekt i emnet MOB3000 ved Universitetet i Sørøst-Norge, høsten 2025. Appen lar deg spille runder alene eller sammen med venner, føre scorekort, se spillhistorikk og chatte med andre spillere.

> Dette er en kopi av gruppens originale repo, med full commit-historikk. Opprinnelig repo: `Baitedr/Gruppe11_MOB3000`.

## Teknologi

| Del | Teknologi |
|---|---|
| App | Kotlin, Jetpack Compose (Material 3), Navigation Compose, ViewModel, DataStore |
| Nettverk | Retrofit, Moshi, OkHttp |
| Backend | Node.js, Express, PostgreSQL, Passport + JWT, bcrypt |
| Sanntid og varsler | Firebase Firestore, Firebase Auth, Firebase Cloud Messaging |
| Kart | Google Maps Compose |

## Mitt bidrag (Oliver Fiveland)

Jeg sto for rundt 60 av prosjektets ~195 commits, og jobbet mest med spillflyt, det sosiale og meldingssystemet:

- **Spillfunksjonalitet** – enkeltspiller-runder, valg av bane, lagring og gjenopptakelse av spill (game sessions), og en baneoversikt som tilpasser seg antall hull.
- **Multiplayer** – invitasjoner til spill via klikkbare lenker i chat, felles spill i sanntid og resultatvisning for alle spillere etter fullført runde.
- **Vennesystem** – venneforespørsler, venneliste hentet fra databasen, fjerning av venner og visning av andre brukeres profiler og spillhistorikk.
- **Meldinger og push-varsler** – bygget meldingssystemet først mot egen backend, og migrerte det deretter til Firebase Firestore med push-varsler via FCM som tar deg rett til riktig skjerm.
- **Navigasjon og profil** – bunnmeny som skjules under spill, strømlinjeformet hovedmeny, endring av brukernavn og navn, og innloggingskrav på riktige steder.
- **Prosjektoppsett** – ProGuard-regler for Retrofit/Moshi, `.gitignore`-opprydding og dokumentasjon for Firebase-oppsett ([FIREBASE_SETUP.md](FIREBASE_SETUP.md)).

## Gruppen

Oliver Fiveland, Sondre Moe, Fredrik Stalsberg, Marcus Einarsen og Martin Mentzen Hansen.
