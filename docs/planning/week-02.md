# Week 02 (11/11 – 18/11, wo)

## Wat is af
- [x] Thema "Groentezak" uit de Figma (licht + donker, geen paars, dynamicColor uit), lettertypes Fredoka en Inter, logo en app-icoon
- [x] Loginscherm en registratiescherm volledig volgens de Figma (logo, rolkaarten, foutmeldingen bij het juiste veld, "Wachtwoord vergeten?")
- [x] Klantscherm met zwevende navigatiebalk (Ontdekken, Kaart, Bestellingen, Favorieten) en profielknop met Uitloggen; Kaart toont de bestaande kaart, de andere tabs zijn nog leeg
- [x] Light mode is de standaard (ook bij het tonen aan de lector); bij open toetsenbord verdwijnt het logo zodat velden en knop in beeld blijven
- [x] Testaccounts voor klant en handelaar aangemaakt (verzonnen gegevens; wachtwoorden staan buiten de repo)
- [x] Registreren met naam, e-mail, wachtwoord en rol klant/handelaar (A1) — gemerged via PR #2
- [x] Strengere Firestore security rules: name en email verplicht, role enkel "customer" of "merchant", rol niet wijzigbaar

## Problemen / niet gelukt
- De schermen Ontdekken, Bestellingen en Favorieten volgen de Figma nog niet: die komen mee met de functionele schermen (K3, K8-K12).
- Op de emulator getest en goed: geen internet (inloggen, opnieuw opstarten met bewaarde sessie, wachtwoord vergeten), account zonder profiel, groot lettertype, liggend scherm en dark mode. Daarbij zijn drie fouten gevonden en opgelost (afgebroken link bij groot lettertype, formulier onder het logo in liggende stand, donker logo en donkere balk in dark mode).
- Nog niet getest: of de resetmail echt aankomt (de testadressen bestaan niet).
- Het app-icoon is de Figma-versie "voorlopig"; de Figma vraagt nog een vereenvoudigde versie voor kleine formaten.

## Tegen volgende week
- [x] Inloggen/uitloggen + juist startscherm per rol (A2) — op de emulator getest met een klant- en een handelaaraccount (gemerged via PR #3)
- [x] Navigatie-skelet: bottom navigation bar voor de klant met Ontdekken, Kaart, Bestellingen, Favorieten (gemerged via PR #3)
- [x] Duidelijke foutmeldingen afwerken: verkeerd wachtwoord, e-mail al in gebruik, geen internet (A3) — in de code en met unit tests; op de emulator gezien: lege/ongeldige invoer en verkeerd e-mailadres of wachtwoord

## Tegen de week erna
- [ ] Locatiepermissie vragen (FINE + COARSE), pin op de kaart als geweigerd (K1)
- [ ] "Pakketten in de buurt van {gemeente}" via Nominatim reverse geocoding + Room-cache CachedAddress (K2)
- [ ] Winkel aanmaken met pin op de kaart en automatisch adres (H1/H2)

## Vragen voor de docent
- Is het goed dat we de lettertypes Fredoka en Inter (open licentie) als bestand in de app zetten?
- "Wachtwoord vergeten?" staat in onze Figma maar niet in de opgave: tellen extra functies zoals deze mee als extra punten?
