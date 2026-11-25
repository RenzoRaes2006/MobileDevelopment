# Week 03 (18/11 – 25/11, wo)

Doel: alle Must-vereisten klaar rond woensdag 16/12 (3 weken voor de presentatie op 6/1), daarna Should's, testen en afwerken. Elk scherm meteen volledig afwerken volgens de Figma.

## Wat is af
- [x] Locatiepermissie (FINE + COARSE) met het uitlegscherm uit de Figma ("3 · Locatie-uitleg", volledig scherm met "Locatie toestaan" en "Zelf een locatie kiezen") (K1)
- [x] Kaart-tab volgens de Figma ("5 · Kaart"): balk met de gemeente, ronde knop "Naar mijn locatie", blauw bolletje voor de positie; zonder positie kies je zelf een plek door op de kaart te tikken
- [x] De positie wordt opnieuw opgevraagd bij het openen van de Kaart-tab en bij het terugkeren in de app, met een tijdslimiet van 10 seconden
- [x] Kop van het tabblad Ontdekken volgens de Figma ("4 · Ontdekken"): "Pakketten in de buurt van" + gemeente groot, via Nominatim reverse geocoding met Room-cache CachedAddress van 30 dagen (K2)
- [x] Scherm "Mijn winkel" volgens de Figma ("10 · Mijn winkel") met naam, categorie, kaart met pin (tikken of slepen), adresblok met straat, nr., postcode en gemeente (H1/H2); handelaar zonder winkel start hier, de balk heeft Vandaag en Mijn winkel — op de emulator getest met een handelaaraccount: starten op "Mijn winkel", naam typen, categorie kiezen, pin zetten (adres vult automatisch in), opslaan, app herstarten (start nu op "Vandaag", "Mijn winkel" is gevuld)

## Problemen / niet gelukt
- Eerste versie vroeg de positie maar één keer en zonder tijdslimiet: de pin bleef op de oude plek of op de AP Hogeschool staan. Gevonden bij het testen op de emulator en opgelost.
- Registreren op de emulator Pixel_10_Pro mislukte met "Trust anchor for certification path not found" (Firebase-aanroep; het certificaat van Firebase wordt niet vertrouwd, waarschijnlijk door het netwerk of de emulator). Nominatim werkte op dezelfde emulator wel.
- Bij het testen van "Mijn winkel" gaf OpenStreetMap voor de AP-campus meerdere huisnummers ("51;53;55;…"). Nu wordt het eerste nummer gebruikt.
- De Figma was eerst niet leesbaar voor Claude: de koppeling gebruikte een ander Figma-account (rraes100@gmail.com) dan dat van het bestand (renzo.raes@student.ap.be).
- Het tabblad Ontdekken toont nog alleen de kop; de filters en de lijsten ("Laatste ronde", "Dichtbij jou") komen met K3. Op de kaart komen de prijsmarkers, het popupkaartje en "x winkels met pakketten" met K4/K5.
- De Figma zegt in de locatie-uitleg "Je locatie wordt niet bewaard of gedeeld". Dat klopt niet helemaal: de coördinaten gaan naar Nominatim en een afgeronde plek staat 30 dagen in de Room-cache. Tekst overgenomen zoals in de Figma; vraag of die aangepast moet worden.
- Wat niet in de Figma staat maar wel nodig is: de profielknop met Uitloggen op "Mijn winkel", een regel onder de gemeente op de kaart (status van de locatie), een hulplabel "Tik op de kaart om je locatie te kiezen" en "✓ Winkel opgeslagen" na het opslaan.

## Tegen volgende week (25/11)
- [x] Locatiepermissie vragen (FINE + COARSE) met uitleg, pin op de kaart als geweigerd (K1) — op de emulator getest: toestaan, positie verplaatsen, tab wisselen
- [x] "Pakketten in de buurt van {gemeente}" via Nominatim reverse geocoding + Room-cache CachedAddress (K2) — op de emulator getest: nieuwe plek (Willebroek), uit de cache zonder internet, onbekende plek zonder internet ("Pakketten in je buurt")
- [ ] Handelaar: winkel aanmaken met pin op de kaart en automatisch adres (H1/H2), scherm "Mijn winkel" volgens de Figma — klaar en op de emulator getest, ook het opslaan in Firestore met de gepubliceerde regels
- [ ] Handelaar: pakket aanmaken en lijst "Vandaag (mijn pakketten)" (H3/H4), zonder foto
- [ ] Firestore security rules uitbreiden voor winkels en pakketten (alleen de eigenaar mag schrijven) — winkels staan in `firestore.rules` en zijn gepubliceerd (schrijven door een handelaar werkt); het weigeren van andere gebruikers is nog niet getest, pakketten volgen
- [ ] Unit tests voor de nieuwe ViewModels — klaar voor LocationViewModel, DiscoverViewModel, ShopViewModel, de adrescache en het lezen van het Nominatim-antwoord (71 tests in totaal, allemaal geslaagd); de rest volgt met pakket

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
