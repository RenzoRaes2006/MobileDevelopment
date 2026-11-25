# Week 03 (18/11 – 25/11, wo)

Doel: alle Must-vereisten klaar rond woensdag 16/12 (3 weken voor de presentatie op 6/1), daarna Should's, testen en afwerken. Elk scherm meteen volledig afwerken volgens de Figma.

## Wat is af
- [x] Locatiepermissie (FINE + COARSE) met uitleg vooraf; pin op de positie van de klant, op de AP Hogeschool met een melding als geweigerd of niet gevonden (K1)
- [x] De positie wordt opnieuw opgevraagd bij het openen van de Kaart-tab en bij het terugkeren in de app, met een tijdslimiet van 10 seconden
- [x] "Pakketten in de buurt van {gemeente}" op het tabblad Ontdekken via Nominatim reverse geocoding, met Room-cache CachedAddress van 30 dagen (K2)

## Problemen / niet gelukt
- Eerste versie vroeg de positie maar één keer en zonder tijdslimiet: de pin bleef op de oude plek of op de AP Hogeschool staan. Gevonden bij het testen op de emulator en opgelost.
- Registreren op de emulator Pixel_10_Pro mislukte met "Trust anchor for certification path not found" (Firebase-aanroep; het certificaat van Firebase wordt niet vertrouwd, waarschijnlijk door het netwerk of de emulator). Nominatim werkte op dezelfde emulator wel.
- Het tabblad Ontdekken toont nog alleen de titel; de lijst met pakketten en het ontwerp volgens de Figma komen mee met K3.

## Tegen volgende week (25/11)
- [x] Locatiepermissie vragen (FINE + COARSE) met uitleg, pin op de kaart als geweigerd (K1) — op de emulator getest: toestaan, positie verplaatsen, tab wisselen
- [x] "Pakketten in de buurt van {gemeente}" via Nominatim reverse geocoding + Room-cache CachedAddress (K2) — op de emulator getest: nieuwe plek (Willebroek), uit de cache zonder internet, onbekende plek zonder internet ("Pakketten in je buurt")
- [ ] Handelaar: winkel aanmaken met pin op de kaart en automatisch adres (H1/H2), scherm "Mijn winkel" volgens de Figma
- [ ] Handelaar: pakket aanmaken en lijst "Vandaag (mijn pakketten)" (H3/H4), zonder foto
- [ ] Firestore security rules uitbreiden voor winkels en pakketten (alleen de eigenaar mag schrijven)
- [ ] Unit tests voor de nieuwe ViewModels — klaar voor LocationViewModel, DiscoverViewModel, de adrescache en het lezen van het Nominatim-antwoord (29 nieuwe tests, 41 in totaal, allemaal geslaagd); de rest volgt met winkel en pakket

## Tegen de week erna (2/12)
- [ ] Foto bij een pakket: verkleinen, JPEG, Base64 (H5)
- [ ] Pakkettenlijst "Dichtbij jou" met afstand en "Laatste ronde" bovenaan (K3, scherm Ontdekken)
- [ ] Kaart met pakketten en pakketdetail (K4/K5/K7)

## Daarna (voorlopig plan)
- 9/12: reserveren, ophaalticket en bestellingen (K8/K9/K10), reservaties bekijken door de handelaar
- 16/12: favorieten met Room (K12), geen crashes, rotatie en laadstaten (G1-G3); alle Must's klaar
- 23/12: Should's (A4, K6, K11, H6, H7, G4) en eigen extra's, grondig testen
- 30/12: presentatie voorbereiden, laatste fouten

## Vragen voor de docent
- ...
