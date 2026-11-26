# Week 03 (18/11 – 25/11, wo)

Doel: alle Must-vereisten klaar rond woensdag 16/12 (3 weken voor de presentatie op 6/1), daarna Should's, testen en afwerken. Elk scherm meteen volledig afwerken volgens de Figma.

## Wat is af
- [x] Locatiepermissie (FINE + COARSE) met het uitlegscherm uit de Figma ("3 · Locatie-uitleg", volledig scherm met "Locatie toestaan" en "Zelf een locatie kiezen") (K1)
- [x] Kaart-tab volgens de Figma ("5 · Kaart"): balk met de gemeente, ronde knop "Naar mijn locatie", blauw bolletje voor de positie; zonder positie kies je zelf een plek door op de kaart te tikken
- [x] De positie wordt opnieuw opgevraagd bij het openen van de Kaart-tab en bij het terugkeren in de app, met een tijdslimiet van 10 seconden
- [x] Kop van het tabblad Ontdekken volgens de Figma ("4 · Ontdekken"): "Pakketten in de buurt van" + gemeente groot, via Nominatim reverse geocoding met Room-cache CachedAddress van 30 dagen (K2)
- [x] Locatie opnieuw op de emulator getest met een testprovider (Oostende, Gent): "Tijdens gebruik van de app" volgt de positie en vraagt niet opnieuw, "Alleen deze keer" vraagt na het afsluiten opnieuw (Android trekt de permissie in), "Niet toestaan" toont Antwerpen (AP Ellermanstraat) (K1)
- [x] Scherm "Mijn winkel" volgens de Figma ("10 · Mijn winkel") met naam, categorie, kaart met pin (tikken of slepen), adresblok met straat, nr., postcode en gemeente (H1/H2); handelaar zonder winkel start hier, de balk heeft Vandaag en Mijn winkel — op de emulator getest met een handelaaraccount: starten op "Mijn winkel", naam typen, categorie kiezen, pin zetten (adres vult automatisch in), opslaan, app herstarten (start nu op "Vandaag", "Mijn winkel" is gevuld)
- [x] Scherm "Nieuw pakket" volgens de Figma ("Pakket aanmaken"): titel, beschrijving, prijs en originele waarde (de prijs moet lager zijn), aantal, ophaalvenster van-tot; zonder foto (H3)
- [x] Scherm "Vandaag" volgens de Figma: winkel met "sluit om", "Mijn pakketten" met "+ Nieuw", kaarten met de schakelaar "Zichtbaar voor klanten" / "Offline gehaald"; zonder de groene kaart met "opgehaald" (die hoort bij de reserveringen) (H5)
- [x] Op de emulator getest met het handelaaraccount: leeg formulier (alle foutmeldingen), prijs hoger dan de originele waarde, pakket online zetten, het verschijnt in de lijst met "sluit om 19:30", schakelaar om en na een herstart nog zo, het kruisje, rotatie van het formulier (ingevulde titel blijft staan)
- [x] Zonder internet getest op de emulator (wifi uit en weer aan): schakelaar, nieuw pakket en "Winkel opslaan" geven "Geen internetverbinding" en slaan niets achteraf op

## Problemen / niet gelukt
- Zonder internet bleef een schrijfactie in de wachtrij van Firestore staan en werd later alsnog uitgevoerd: de schakelaar "Zichtbaar voor klanten" sloeg achteraf om, en "Pakket online zetten" gaf bij opnieuw proberen een dubbel pakket. Gevonden bij het testen op de emulator en opgelost: een pakket, de schakelaar en "Winkel opslaan" gebruiken nu een transactie. Zonder internet mislukt die meteen (melding binnen 10 seconden) en er wordt niets achteraf opgeslagen. Getest op de emulator met wifi uit en weer aan: de schakelaar blijft zoals hij was en opnieuw proberen geeft precies één pakket.
- Het testscript voor de regels van pakketten (`scripts/test-package-rules.mjs`) kon eerst zelf een testaccount in Firebase Auth aanmaken; dat willen we niet op het live project. Het script maakt nu nooit meer een account aan en draait met de twee bestaande testaccounts: 28 van 28 gevallen goed. De 3 gevallen met een tweede handelaar (aanmaken, wijzigen en verwijderen door een andere handelaar) zijn overgeslagen; de eigenaar-controle is wel getest met een verzonnen uid.
- Na een geslaagde "Winkel opslaan" bleef "✓ Winkel opgeslagen" staan naast een latere foutmelding zonder internet. Gevonden bij het testen op de emulator en opgelost, met een unit test.
- De locatie bleef op Willebroek staan op de emulator. Oorzaak: de GPS van de emulator zat vast op die plek en negeerde `adb emu geo fix`; de app deed niets fout. Opgelost door een testprovider te gebruiken: `adb shell appops set com.android.shell android:mock_location allow`, daarna `adb shell cmd location providers add-test-provider gps` en `set-test-provider-location gps --location <lat>,<lon> --accuracy 5`.
- Eerste versie vroeg de positie maar één keer en zonder tijdslimiet: de pin bleef op de oude plek of op de AP Hogeschool staan. Gevonden bij het testen op de emulator en opgelost.
- Registreren op de emulator Pixel_10_Pro mislukte met "Trust anchor for certification path not found" (Firebase-aanroep; het certificaat van Firebase wordt niet vertrouwd, waarschijnlijk door het netwerk of de emulator). Nominatim werkte op dezelfde emulator wel.
- Gevonden bij het testen op de emulator en opgelost: opslaan zonder internet bleef eindeloos draaien (nu na 10 seconden "Geen internetverbinding"); na het draaien van het scherm stond de pin half buiten de kaart; de gemeente op Ontdekken aantikken deed niets als de permissie al gegeven was (nu naar de kaart om een andere plek te kiezen).
- Bij het testen van "Mijn winkel" gaf OpenStreetMap voor de AP-campus meerdere huisnummers ("51;53;55;…"). Nu wordt het eerste nummer gebruikt.
- De Figma was eerst niet leesbaar vanuit de ontwikkelomgeving: de koppeling gebruikte een ander Figma-account dan het account waarop het bestand staat.
- Het tabblad Ontdekken toont nog alleen de kop; de filters en de lijsten ("Laatste ronde", "Dichtbij jou") komen met K3. Op de kaart komen de prijsmarkers, het popupkaartje en "x winkels met pakketten" met K4/K5.
- De Figma zegt in de locatie-uitleg "Je locatie wordt niet bewaard of gedeeld". Dat klopt niet helemaal: de coördinaten gaan naar Nominatim en een afgeronde plek staat 30 dagen in de Room-cache. Tekst overgenomen zoals in de Figma; vraag of die aangepast moet worden.
- Wat niet in de Figma staat maar wel nodig is: de profielknop met Uitloggen op "Mijn winkel", een regel onder de gemeente op de kaart (status van de locatie), een hulplabel "Tik op de kaart om je locatie te kiezen" en "✓ Winkel opgeslagen" na het opslaan.

## Tegen volgende week (25/11)
- [x] Locatiepermissie vragen (FINE + COARSE) met uitleg, pin op de kaart als geweigerd (K1) — op de emulator getest: toestaan, positie verplaatsen, tab wisselen
- [x] "Pakketten in de buurt van {gemeente}" via Nominatim reverse geocoding + Room-cache CachedAddress (K2) — op de emulator getest: nieuwe plek (Willebroek), uit de cache zonder internet, onbekende plek zonder internet ("Pakketten in je buurt")
- [x] Handelaar: winkel aanmaken met pin op de kaart en automatisch adres (H1/H2), scherm "Mijn winkel" volgens de Figma — klaar en op de emulator getest, ook het opslaan in Firestore met de gepubliceerde regels
- [x] Handelaar: pakket aanmaken en lijst "Vandaag (mijn pakketten)" (H3/H5), zonder foto — op de emulator getest, ook de schakelaar en het opslaan in Firestore met de gepubliceerde regels
- [x] Firestore security rules uitbreiden voor winkels en pakketten (alleen de eigenaar mag schrijven) — winkels staan in `firestore.rules` en zijn gepubliceerd; getest met een script op het live project (24 gevallen, alle goed: een klant, een andere handelaar of niet-ingelogd mag niet schrijven, foute categorie, lege of te lange velden en een foute positie worden geweigerd, de eigenaar mag wel schrijven); de regels voor pakketten (eigenaar schrijft, prijs lager dan de originele waarde, grenzen voor titel, beschrijving, aantal, datum en tijden) zijn gepubliceerd en getest met `scripts/test-package-rules.mjs` op het live project (28 gevallen, alle goed)
- [x] Unit tests voor de nieuwe ViewModels — klaar voor LocationViewModel, DiscoverViewModel, ShopViewModel, PackageFormViewModel, TodayViewModel, de hulpfuncties voor bedragen en tijden, de adrescache en het lezen van het Nominatim-antwoord (120 tests in totaal, allemaal geslaagd; build en lint ook)

## Tegen de week erna (2/12)
- [ ] Foto bij een pakket: verkleinen, JPEG, Base64 (H4)
- [ ] Pakkettenlijst "Dichtbij jou" met afstand en "Laatste ronde" bovenaan (K3, scherm Ontdekken) — de lijst met afstand is al klaar, zie `week-04.md`; de rij "Laatste ronde" en de filters volgen
- [ ] Kaart met pakketten en pakketdetail (K4/K5/K7) — de prijslabels en het winkelkaartje zijn al klaar, zie `week-04.md`; het pakketdetail volgt

## Daarna (voorlopig plan)
- 9/12: reserveren, ophaalticket en bestellingen (K8/K9/K10), reservaties bekijken door de handelaar
- 16/12: favorieten met Room (K12), geen crashes, rotatie en laadstaten (G1-G3); alle Must's klaar
- 23/12: Should's (A4, K6, K11, H6, H7, G4) en eigen extra's, grondig testen
- 30/12: presentatie voorbereiden, laatste fouten

## Vragen voor de docent
- De Figma zegt in de locatie-uitleg "Je locatie wordt niet bewaard of gedeeld", maar de coördinaten gaan naar Nominatim en een afgeronde plek staat 30 dagen in de Room-cache. Mag de tekst zo blijven of moet hij aangepast worden?
