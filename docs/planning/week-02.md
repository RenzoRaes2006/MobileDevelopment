# Week 02 (dd/mm – dd/mm)

## Wat is af
- [x] Thema "Groentezak" uit de Figma (licht + donker, geen paars, dynamicColor uit)
- [x] Registreren met naam, e-mail, wachtwoord en rol klant/handelaar (A1) — gemerged via PR #2
- [x] Strengere Firestore security rules: name en email verplicht, role enkel "customer" of "merchant", rol niet wijzigbaar

## Problemen / niet gelukt
- ...

## Tegen volgende week
- [x] Inloggen/uitloggen + juist startscherm per rol (A2) — op emulator getest: validatie en mislukte login; nog niet getest met een echt account (branch `feature-login-per-role`, nog geen PR)
- [x] Navigatie-skelet: bottom navigation bar voor de klant met Ontdekken, Kaart, Bestellingen, Favorieten (branch `feature-bottom-navigation`)
- [x] Duidelijke foutmeldingen afwerken: verkeerd wachtwoord, e-mail al in gebruik, geen internet (A3) — in de code; alleen "verkeerd e-mailadres of wachtwoord" is op de emulator gezien

## Tegen de week erna
- [ ] Locatiepermissie vragen (FINE + COARSE), pin op de kaart als geweigerd (K1)
- [ ] "Pakketten in de buurt van {gemeente}" via Nominatim reverse geocoding + Room-cache CachedAddress (K2)
- [ ] Winkel aanmaken met pin op de kaart en automatisch adres (H1/H2)

## Vragen voor de docent
- ...
