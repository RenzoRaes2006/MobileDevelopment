# Week 04 (25/11 – 2/12)

Doel: de klant ziet de pakketten van de winkels (alleen bekijken), de handelaar zet er foto's bij. Reserveren komt in week 5.

## Wat is af
- [x] Een klant ziet bij het opstarten op "Ontdekken" de lijst "Dichtbij jou" met alle zichtbare pakketten van vandaag: winkel, gemeente en afstand, titel, ophaaltijd, "Nog X over", korting, oude en nieuwe prijs; gesorteerd op afstand van de klant (de echte plek of de gekozen plek) (K3, alleen bekijken)
- [x] Op de kaart staat elke winkel met pakketten als prijslabel ("€3", het laagste bedrag van die winkel); een tik toont het kaartje met winkel, afstand, ophaalvenster, "nog X" en "Vanaf €3"; een tik op de kaart sluit het kaartje; de balk zegt "1 winkel met pakketten" (K4, K5, alleen bekijken, zonder de knop "Bekijk")
- [x] Alleen pakketten met de schakelaar "Zichtbaar voor klanten" aan en met de datum van vandaag tellen mee; een pakket dat de handelaar offline zet, verdwijnt bij de klant (getest op de emulator met twee accounts: handelaar zet "overschot" uit, de klant ziet nog twee pakketten)
- [x] Unit tests voor `CustomerPackagesViewModel` (laden, sorteren bij een andere plek zonder opnieuw te lezen, fouten, een nieuwe lijst na opnieuw laden) en voor de afstand, de korting en het koppelen van pakketten aan winkels

- [x] "Mijn winkel": een adres intypen zet de pin erheen (H2, omgekeerd van de bestaande pin-naar-adres). Na 1 seconde zonder typen zoekt Nominatim het adres (straat en nr., postcode, gemeente; alleen België); de pin springt erheen en de kaart volgt. Het getypte adres blijft zoals het getypt is. Zonder huisnummer in de kaart wordt de straat gebruikt. Er is een straat en een postcode of gemeente nodig; bij elke nieuwe letter begint het wachten opnieuw, zodat alleen het laatste adres gezocht wordt. Tijdens het zoeken staat er "Pin zoeken…" en kan je niet opslaan, zodat de oude pin niet bij het nieuwe adres wordt opgeslagen. Een adres dat niet gevonden wordt geeft een melding en laat de pin staan; een pin die je zelf zet, wint van een lopende zoekopdracht. Getest op de emulator: Zeedijk 12, 8300 Knokke-Heist zette de pin in Knokke; een straat die niet bestaat gaf de melding. 30 nieuwe unit tests

- [x] Een winkel met meerdere pakketten: tik op de winkel op de kaart en het kaartje toont de winkel met afstand en het aantal aanbiedingen, met daaronder elke aanbieding als eigen rij (titel, ophaaltijd, "nog X", korting en prijs); veel aanbiedingen scrollen in het kaartje. Het kiezen van één aanbieding (de knop "Bekijk") komt met reserveren. Getest op de emulator met "Di papa" en twee aanbiedingen
- [x] Regels: een handelaar kan alleen zijn eigen pakketten lezen (ook geen lijst van alle pakketten), een klant kan alle pakketten lezen; gepubliceerd en getest met `scripts/test-package-rules.mjs` op het live project (34 gevallen, alle goed)

## Problemen / niet gelukt
- Er bleek maar één winkel in Firestore te staan ("Di papa", van het account handelaar@gmail.com, hernoemd van "Bakkerij NieuwX"), terwijl het account dipapa@gmail.com (aangemaakt om 14:34) geen winkel had. Oorzaak: geen fout in de app maar een sessie die door elkaar liep. Tijdens het testen wisselden Claude en de ontwikkelaar op dezelfde emulator van account (klant, handelaar), waardoor de sessie van dipapa werd uitgelogd en de emulator als handelaar@gmail.com bleef staan. Het scherm "Mijn winkel" toont nergens welk account ingelogd is, dus "Di papa" werd bij het verkeerde account opgeslagen; de pakketten van dat account bleven bij die winkel. Besluit: één winkel per handelaar-account; een tweede winkel is een tweede account.
- Hersteld op 25/11 met de app zelf: dipapa kreeg zijn winkel "Di papa" (Kronenburgstraat 41a) via "Mijn winkel", en handelaar@gmail.com kreeg "Bakkerij NieuwX" (Ellermanstraat 58) terug, met de pin automatisch gezet door het adres. Gecontroleerd: dipapa ziet op "Vandaag" alleen zijn eigen winkel en geen pakketten van de andere winkel; er is geen gegevenslek tussen accounts. De fout is niet in de app gereproduceerd: het aanmaken van een nieuw account op het live project werd door de automatische controle geweigerd.
- Idee (niet gebouwd): in het profielmenu "Ingelogd als …" tonen, zodat duidelijk is welk account een scherm aanpast.
- De klant ziet nog geen foto's: het grijze fotovakje staat er als plaatsvervanger tot de foto bij een pakket (H5) er is.
- Uit de Figma ontbreken nog: de filters (Alle, ≤ 5 km, Bakker, Supermarkt), de rij "Laatste ronde" met "sluit over 25 min", het hartje en de knop "Bekijk". Het hartje en "Bekijk" horen bij favorieten en reserveren.
- Op de emulator duurde het soms even voor de gemeente bovenaan verscheen (Nominatim, en we wachten op de plek van de klant). De lijst en de kaart hebben daar geen last van.
- Tijdens het testen zette ik "overschot" van het handelaaraccount uit; het staat nu op "Offline gehaald".

## Tegen volgende week (2/12)
- [ ] Foto bij een pakket: verkleinen, JPEG, Base64 (H5), en tonen in de lijst, op de kaart en bij de handelaar
- [ ] Filters en de rij "Laatste ronde" op Ontdekken (K3, de rest)
- [ ] Pakketdetail met beschrijving (K7)

## Tegen de week erna (9/12)
- [ ] Reserveren, ophaalticket en bestellingen (K8/K9/K10), reservaties bekijken door de handelaar

## Vragen voor de docent
- ...
