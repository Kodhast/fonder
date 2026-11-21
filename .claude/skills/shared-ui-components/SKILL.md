---
name: shared-ui-components
description: 'Regel 4 i Fonder — samma elementtyp ser ut och beter sig likadant överallt och varje sak finns på ett ställe. Enda källan för komponent- och byggstenstabellerna, de kända dubbletterna och förbudslistan. Ladda alltid vid UI, skärmar, komponenter, interaktionsmönster, formulär, listor, beräkningar eller liknande kod på två ställen. Trigger-ord: komponent, skärm, knapp, kort, lista, listrad, fält, dialog, laddning, tomt tillstånd, diagram, dra för att uppdatera, dubbelkod, återbruk, TotalCard, ComparisonChart, UiConsistencyTest, cpdCheck, allowlist.'
---

# Enhetligt och utan dubbelkod

**Invariant:** en elementtyp = en komponent, ett interaktionsmönster = ett ställe, en sorts
logik = ett ställe. Två skärmar som visar eller gör samma sak ska se ut och bete sig
identiskt. Skillnader uttrycks **bara** med parametrar på den delade delen.

Denna skill är **enda källan** för tabellerna nedan. `CLAUDE.md`, andra skills och agenter
länkar hit – de kopierar dem inte. Komponenter som inte är byggda står **bara** under
*Planerade delade delar*.

```
Grep i app/src/main/kotlin/se/kodhast/fonder/ui/components/ och ui/diagram/ först
```

Utseendet följer Fonders designspråk – nordisk blå, kopparaccent, M3 Expressive, Space Grotesk, tabulära
siffror, fast palett (UI-1). Temat är android-ui:s `AppTheme` via `FonderTheme`; färger och
typografi kommer från `ui/theme` (formerna från biblioteket) (`FonderLightColorScheme`/`FonderDarkColorScheme`, `ReturnColors`,
`StatusColors`, `ChartSeriesColors`, `FonderTypography`, `MonoAmountStyle`) och avstånden
från android-ui:s `Spacing` (`xs` 4 · `s` 8 · `m` 12 · `l` 16 · `xl` 24 · `xxl` 32 dp; skill `ui-style`).

## Utseende: elementtyp → enda tillåtna komponent (`ui/components/`)

| Elementtyp | Komponent | Används i · det som gör den speciell |
|---|---|---|
| Kort | android-ui:s `AppCard` (+ `CardSegment`) | Varje kort på varje skärm. `FonderTheme` sätter ingen kortstil: bibliotekets standard (22 dp hörn, luft 12 dp) med kant och skugga ur djupet (`FonderLight/DarkDepth`, UI-18). `tone = Tone.Filled` (tonad, för rader inne i ett kort), `Tone.Error` + `contentColor` (farozonen); en ton tar bort konturen. Med `onClick` en klickbar nod (UI-13). `nestedCardStyle`, `flushCardStyle`, `ListFrameCards` och `LocalAppCardStyle` finns inte längre (F1 och F6, #266) – ramarna ger sina kort rätt stil själva. |
| Knapp | android-ui:s `AppButton`, `AppIconButton` | Varje knapp (sedan 0.77.0; Fonders egna är borta). `ButtonVariant.Primary`/`Secondary` (Material 3:s tonade)/`Outlined`/`Text`/`Destructive`; bibliotekets standardstil (56 dp, `compact = true` 40 dp, ingen formmorf) – `FonderTheme` sätter **ingen** `LocalAppButtonStyle` sedan 1.2.0 (UI-13). Ikonknappen tar en drawable (`R.drawable.ic_*` eller bibliotekets `UiR.drawable.ic_close`/`ic_arrow_back`), kräver `contentDescription` och är 48 dp (UI-3, UI-13); `IconButtonVariant.Tonal` + `tone = Tone.NeutralPrimary` = rund platta i ytvariant med ikonen i primär, Översiktens Inställningar. |
| Textfält | android-ui:s `AppTextField` | Transaktionsformuläret, importerna, fondsök, ISIN-fältet, riskprofilens andelar (sedan 0.76.0; Fonders egen är borta). Fylld med kortytan, `outlineVariant`-kontur i vila, `shapes.field`; `label` obligatorisk, `error` (felfärg, läses som fel) eller `helper` under fältet, `suffix`, `maxLength` (200 enradigt). Tangentbordet börjar med versal som standard – sätt `keyboardOptions` uttryckligen: tal med `KeyboardType.Decimal`/`Number`, ISIN med versaler utan autokorrigering, fondsök `KeyboardOptions.Default` (UI-13). |
| Dialog | android-ui:s `ConfirmDialog` och `LoadingDialog` | `ConfirmDialog(title, message, confirmLabel, onConfirm, onDismiss)` före varje åtgärd som raderar eller ersätter data (Transaktioner, Inställningar) – `destructive` ger bekräftelseknappen i felfärgen; `dismissLabel = null` ger en dialog med en knapp (`ImportCompleteDialog`, galleriet). Bibliotekets hörn, kortytan, texten 16 sp dämpad. `LoadingDialog(title, message)` (android-ui, den formskiftande indikatorn – Fonder slår inte på `dotLoading`) för importernas blockerande väntan (UI-13, UI-19). |
| Kryssrad | android-ui:s `CheckRow` | Importernas rader (`ImportRowHeader`), och via `FollowedToggleRow` "Genomförd". `CheckRow(title, checked, onCheckedChange, subtitle = …, done = false, tinted = false)` – kryss betyder "ta med"/"gjort", så ingen överstruken eller tonad rad; hela raden en nod med rollen kryssruta, ≥ 48 dp (UI-3, UI-13). Fonders egen `CheckRow` är borta sedan 1.2.0. |
| Listrad | android-ui:s `ItemRow` (+ `ItemRowText`) | Beslut (bytesraderna, de avslutade och Facit-raden), Fondsök och Händelser (sedan 0.77.0; Fonders egen är borta). `title` (16 sp Medium), `subtitle`, `leading` (Händelsers datum, Besluts chip), `trailing`, `below`, `overline` (ovanför rubriken – Besluts chip), `navigates` (pil, Facit-raden); ≥ 56 dp, luft 12/8 dp. Med `onClick` en nod; utan slås den inte ihop – lägg `Modifier.semantics(mergeDescendants = true) {}` när raden ska läsas i ett svep. `ItemRowText(title, subtitle)` för ett korts rubrik + förklaring utan rad (Inställningar, Beslut). |
| ~~Förloppsstapel~~ | ~~`ProgressBar`~~ | *(borttagen i 1.9.0, #266 F6 – den användes bara i galleriet; laddning är ramarnas `AppLoading`.)* |
| Tomt tillstånd och läsfel (flikar och detaljskärmar) | android-ui:s `EmptyState` (`actions = listOf(EmptyAction(…))`) och `LoadErrorState` ("Försök igen" när felet går att försöka om), – sedan 1.9.0 (#266 F6) alltid genom en ram, `CenteredState` är borta | Facit, importerna (Försök igen öppnar filväljaren, `ImportFrame`), transaktionsformulärets "Inga bevakade fonder" (listramens tomma läge med `actions`) och Pågående byte (saknad bevakning = läsfel utan Försök igen med egen text, läsfel med `SwitchWatchEvent.Retry`) står på list- eller detaljramen och får dem av ramen (`EntityListScreen(empty = EmptyContent(…, actions), onRetry, errorTitle)`). Händelsers och Innehavs knappar i det tomma läget kommer ur `firstTransactionActions` (TRX-7, POR-10). Retry i ViewModel: `RetrySignal` + `restartOn` efter felfångsten (`ui/common/Retry.kt`). Ikonerna är vektorerna `ic_*` i `res/drawable`. |
| ~~Tomt tillstånd och läsfel (övriga)~~ | ~~Fonders `EmptyState`~~ | *(borttaget i 1.2.0 – alla går till android-ui:s `EmptyState`/`LoadErrorState` ovan.)* Rita aldrig en egen tom- eller felvy. |
| ~~Kortets rubrikrad med väntesnurra~~ | ~~`CardTitleRow`~~ | *(borttagen i 1.9.0, #266 F6 – ingen snurra i kort, UI-19.)* Ett kort med rubrik är android-ui:s `SectionCard`; ett värde på väg säger "Hämtar …" i text. |
| Avsnittsrubrik | android-ui:s `SectionHeader` | Besluts avsnitt (Så kommer du närmare målet, Avslutade och överhoppade, Så har råden gått) och Inställningars Tema (sedan 0.77.0). 20 sp, `count` i en pill (Besluts antal står sedan 1.6.0 i sidhuvudet i stället), ingen snurra på ramarnas skärmar (NAV-6); läses som rubrik. Flikarnas rubrik står i android-ui:s `AppHeader` (NAV-7). |
| Avdelare | android-ui:s `AppDivider` | Mellan delar av ett kort (Inställningar, Besluts avslutade) och mellan bytesavsnittet och diagrammet i Fonddetalj. 12 dp indrag; `Modifier.padding` för luft. |
| Laddning / bakgrundsjobb | android-ui:s sidhuvudprickar, ramarnas `AppLoading`, `LoadingDialog` | En indikator (UI-19): bakgrundsjobbet syns som hoppande prickar i `AppHeader` och underskärmarnas `AppTopBar` (`AppNavHost`s `sync`, NAV-6); första laddningen är ramens `AppLoading`; ett värde på väg säger "Hämtar …" i text; dra ned behåller sin indikator; importerna har den spärrande `LoadingDialog`. `WorkingIndicator` är borta sedan 1.9.0 (#266 F6) – ingen snurra i kort, rubriker eller diagram. |
| Dra ned för att uppdatera | android-ui:s `AppPullToRefresh(refreshing, onRefresh, modifier.fillMaxSize())` | Varje skärm med hämtad data (Facit, Bytesfönster); ramskärmarna (Översikt, Innehav, Beslut – där bara när en plan kan ges – Fondsök, Fonddetalj) får dragningen av ramen (`refreshing`/`onRefresh`). `refreshing` drivs av skärmens körstatus (kurserna, bytesplanen), aldrig av en lokal flagga (UI-11). Taggen `ENTITY_REFRESH_TAG` i tester. Fonders `PullToRefreshContainer` är borta sedan 1.2.0. |
| Ett val bland några | android-ui:s `AppSegmentedChoice`, `ChoiceChips` | `AppSegmentedChoice(options, selectedIndex, onSelect)` för 2–3 korta val: tema, kontotyp, bevakningstid (Inställningar), Händelsers filter Alla/Köp/Sälj och Köp/Sälj i formuläret och importen (fler segment klipper det valdas text bakom bocken). `ChoiceChips` för fler eller längre val som bryts på flera rader: `FundLineChart`s sex perioder, köpnivån 1–7 i Pågående byte (ANA-13; säljfondens nivå står i texten under) och riskenkätens tre frågor (`AnswerChips` i `RiskProfilScreen` gör svaret nullbart innan något är valt). Läses som radioval (UI-12). Segmentvalet har ingen `enabled`: ett låst val (typen i ett förifyllt formulär från Följ) visas som ett låst `SelectField`. |
| Dropdown | android-ui:s `SelectField` | Fondsök, transaktionsformuläret (fonden och en låst typ), importerna och bytesväljaren (`SwitchDecisionField` i `ui/transaktioner`, BYT-2). `optionLabel` är ingen composable – slå upp texterna före anropet. `clearOptionLabel` ger ett "alla/inget"-val överst; `supportingText` och `error` under fältet; `enabled = false` låser fältet med valet synligt. |
| Datumfält | android-ui:s `DateField` | Transaktionsformuläret, importerna. Visar "tis 20 okt 2026" och öppnar Material 3-datumväljaren på kortytan (OK/Avbryt), öppet läge i `rememberSaveable` (UI-9); läses som **en** knapp med etikett och värde. Datumet är `kotlinx.datetime.LocalDate` – ViewModels har `java.time`, så anroparen konverterar med `toKotlinLocalDate()`/`toJavaLocalDate()`. |
| Värde per period / nyckeltal | `PeriodRow` (sedan 1.6.0 ett tunt lager över android-ui:s `ValueRow`, som läses som en nod "etikett, värde") | Hem, Portfölj, Fonddetalj, Facit, Bytesfönster, Händelser (realiserat resultat). kr + %, eller bara % (`amount = null`); `stackValue` för mått som inte ska färgkodas som vinst/förlust; `null` = otillräcklig data, aldrig 0 (ANA-4); `insufficientText` när orsaken är känd ("Visas efter ett år", "Kurs saknas ännu"); `muted` för en underordnad rad utan vinst/förlust-färg (totalkortets "Varav orealiserat"); `supportingLabel`/`supportingValue` för en dämpad rad under etiketten respektive värdet och `valueTone` för ett färdigformaterat värde som ändå färgas som vinst/förlust (Facit: "3 av 5 bättre", "mätt till …"); `valueMuted` dämpar bara värdet, etiketten står kvar (ungefärliga skatter, se `ApproxTaxRow`). |
| Ungefärlig skatt | `ApproxTaxRow` | Hem (schablonskatt i ISK/KF, HEM-16), Händelsers årskort (depå/AF, TRX-8) och Fonddetaljs bytesavsnitt (depå/AF, ANA-15). En `PeriodRow` med "ca −3 000 kr" i hela kronor, `valueMuted` och vad skatten bygger på som `supportingLabel` (issue #199). Förklaringen under raderna är `ItemRowText(title = null, …)`. |
| Portföljens totalkort | `TotalCard` | Hem och Portfölj (HEM-1/POR-3). `SectionCard` (sedan 1.6.0) med huvudtal, `PeriodRow`-rader (totalresultat, årsavkastning, varav orealiserat), `ValueAsOfRow` och `ExpandableInfoRow`; tar primitiver och `PortfolioTotalCalc.Result`, aldrig en skärms `UiState`. Platserna `afterTotalResult` (direkt under totalresultatet) och `footer` (sist) tar Hems schablonskatt och dess förklaring (HEM-16); null = kortet som förut. |
| "Värde per <datum>" | `ValueAsOfRow` | Hem och Portfölj (i `TotalCard`). Renderar ingenting utan datum (POR-7). |
| Riskkortet | `RiskCard` (`ui/components/RiskCard.kt`) | Översikt och Beslut (HEM-7, NAV-9). Ett `SectionCard` med en `ShareBar` per risknivå med målstreck, läget i en mening och vad planen flyttar; `portfolioRisk` ger Översiktens snittrader, `goal` "Ditt mål är …", `footer` Besluts "Ändra mål". Tar `RiskSummary` ur `PortfolioRiskSource`. |
| Andel per kategori | android-ui:s `ShareBar(label, fraction, value, target, targetValue, color, targetColor)` | Innehavs exponeringskarta (POR-9, okänt i `outline`) och riskkortets nivåer mot målet (`target` ritar målstrecket, färgerna byts när nivån ligger över). Läses som en enhet: "Aktiefond, 80,0 %" eller "Nivå 3: 12,0 %, mål 31,0 %" – texterna går inte att hitta med `onNodeWithText` i tester, bara med beskrivningen. Fonders `ExposureBar` är borta sedan 1.6.0. |
| Banderoll med åtgärd | android-ui:s `NoticeBanner(text, icon, onClick, tone, onClickLabel, detail)` | Översikten (HEM-13 "N beslut väntar", HEM-12 säkerhetskopian), `tone = Tone.Copper`. Hela banderollen är en knapp som läses i ett svep (UI-3); ingen stängknapp – den försvinner med orsaken. Fonders `ReminderRow` och `CountBadge` är borta sedan 1.6.0; Beslut-flikens märke är flikradens `ToolbarItem.badgeCount`. |
| Sektionskort | android-ui:s `SectionCard(title, count, tone)` | Översiktens och Innehavs kort (totalkortet, avkastning, utveckling, att se över, avgifter, riskkortet, exponeringen) sedan 1.6.0: prickarna före rubriken, ingen snurra – ett värde på väg säger "Hämtar …" (NAV-6). Sedan 1.9.0 (#266 F6) även Facits summering och Pågående bytes målnivå; `CardTitleRow` är borta. |
| Fondens fakta | `FundFactsCard`, `FundFactsRow` (`ui/components/FundFactsCard.kt`) | Besluts infokort (BYT-5). Tar `FundFacts` (primitiver, `of(FundMetadata)` eller `of(SuggestionCandidate)`); kortet är ett tonat `AppCard` med `PeriodRow`-rader (risk, avgift, senaste året, typ, fondbolag), `badge` och `reason`; raden en `ItemRow` med avgift och senaste året till höger. Saknat värde = "okänt", aldrig 0. |
| Kopierbart fondnamn | `Modifier.copyableFundName(name)` (`ui/components/CopyableFundName.kt`) | UI-16: android-ui:s `Modifier.copyOnLongPress(text, label, clipLabel)` med Fonders texter ("Kopiera fondnamn", urklippets etikett) – ett `@Composable`-skal, eftersom texterna slås upp ur resurserna. Långtryck kopierar bara namnet (haptik, Androids egen bekräftelse) och TalkBack-åtgärden "Kopiera fondnamn". Sätts på namnets text eller på en rad/ett kort med en fond; på en `ItemRow` utan `onClick` tillsammans med `semantics(mergeDescendants = true)`. Långtrycket förbrukas i Initial-passet, så raden öppnas inte. Inte där långtryck redan betyder något (Transaktioner). Urklipp rörs bara i biblioteket (`ui-forbidden.txt`). |
| Bottenark | android-ui:s `AppBottomSheet` | Besluts infokort (BYT-5). `title`, `onDismiss`, `footer` (knapprad), `content`. Material3:s `ModalBottomSheet` används aldrig direkt. |
| Ihopfällbar sektion | android-ui:s `Foldout` (och `SectionFoldout` för sektionskort) | Fonddetaljs Analys och ordlista, Besluts "Avslutade och överhoppade" och Facits rader (`FacitRadRow` i listramens grupp "Förslagen": rubrik, undertext och meravkastning i `header` – alltid synliga – och detaljerna i det som fälls ut) och fördelningen per horisont i Facits summering. `Foldout(title, expanded, onToggle)` har inget eget läge: anropsstället håller det med `rememberSaveable` (UI-9). Rubrikraden är `CollapsibleHeader`: knapp ≥ 48 dp med "Hopfälld"/"Utfälld" som `stateDescription` och fjädrande chevron (UI-3). |
| Utfällbar rad med förklaring | `ExpandableInfoRow` | Fonddetalj, Facit, totalkortet ("Vad räknas in?"). Skal på android-ui:s `Foldout` som lägger till sparat läge (`rememberSaveable`) och `onExpand` (innehåll som är dyrt att hämta); `extraContent` för t.ex. jämförelsediagrammet (ANA-11). Bara för rader där radens innehåll är en egen komponent (`PeriodRow`, signalrad) – en sektion med rubrik är `Foldout`. |
| Strömbrytarrad | android-ui:s `SwitchRow` | Pågående byte (*Bara indexfonder*, ANA-13). `SwitchRow(title, checked, onCheckedChange)`; `toggleable` på hela raden, en nod med rollen strömbrytare, ≥ 56 dp; `subtitle`, `enabled` (låst reglage med läget synligt). `CheckRow` för en markering i en lista, `SwitchRow` för ett läge som gäller direkt. |
| "Genomförd"-kvittering | `FollowedToggleRow` | Bara Facit (SET-5) sedan 0.68.0 — Följ på Beslut och Fonddetalj öppnar säljformuläret (BYT-2). En `CheckRow` med kvitteringens etikett; skriver mot det inspelade förslaget (SET-5). |
| Tillståndsmärke | android-ui:s `InfoPill(shape = AppTheme.shapes.smallTile)` | Beslut (BYT-4), `RiskBadge`, `ProfitTakeBadge` (sedan 0.77.0; `StateChip` är borta). Text i `labelMedium` på tonad behållare: `Tone.Primary` (planens steg), `Tone.Copper` (Sålt), `Tone.Gain` (Köpt), `Tone.Tertiary` (Sparar avgift), `Tone.Neutral` (ytvariant – Överhoppat/Avbrutet/Utgånget, risknivån); tonerna i `ui/theme/Color.kt`. Tillståndet alltid i text (UI-3); `contentDescription` för förkortad text. Inte klickbart. |
| Risknivå | `RiskBadge` | Fonddetalj, Portfölj, Fondsök, Bytesfönster. "Risk 5/7" i text, `toLevel` ger "Risk 5 → 4" (UI-10). En `InfoPill` i `Tone.Neutral` med utskriven beskrivning ("Risknivå 5 av 7") som `contentDescription`. |
| Vinsthemtagningsläge | `ProfitTakeBadge` | Portfölj. En `InfoPill` med ikon (`ic_trending_up`) + text i vinstens kulör, medvetet skild från trafikljuset; aldrig ett köp-/säljråd (ANA-8). |
| Säljsignalens status | `StatusDot`, `AnalysisStatusBanner`, `AnalysisGuidanceCard` (`AnalysisStatus.kt`) | Fonddetalj och Hem (`StatusDot` även i Portfölj). Samma titel och triggertexter för samma status (ANA-3, HEM-4). |
| Import klar | `ImportCompleteDialog` | Excel- och PDF-importen. android-ui:s `ConfirmDialog` med bara Stäng (`dismissLabel = null`), inte en fullskärmsvy. |
| Listskärm | android-ui:s `EntityListScreen` | Innehav (sedan 1.6.0: flikläge, totalkortet och exponeringen som `header`, listgruppen "Fonder", antalet fonder som `subtitle`), Händelser (`add = null`, filtret och resultatkortet som `filter`/`header`, stor rubrik som fälls ihop; i flikläget med Kodhäst-sidhuvudet) och Fondsök (liten topprad med bakåt, fälten som `filter`, egen `errorTitle`, dra ned; även som jämförelsefondens och Pågående bytes väljare). Kontraktstest med `runListScreenContract` (android-ui:s `ui-testing`). |
| Detaljskärm | android-ui:s `EntityDetailScreen` | Fonddetalj: `header = null` och fondens namn som `title` i den lilla toppraden ("Fond" tills namnet lästs), märkningarna vänsterställda överst i innehållet, sektionerna `sectionGap = Spacing.m` isär, dra ned via `refreshing`/`onRefresh` (UI-11); laddning (`AppLoading`), läsfel med "Försök igen" och "finns inte längre" utan knapp är ramens (NFR-6). Ramen har ingen egen `imePadding` – skärmen lägger den på ramens `modifier` (ISIN-fältet). Kontraktstest med `runDetailScreenContract`. Sedan 1.6.0 även Översikt och Beslut i flikläge (`onBack = null`, `header = null`, flikens namn som `title`, Besluts "N väntar på dig" som `subtitle`). Pågående byte har kvar sina egna lägen. |
| Redigeringsskärm | android-ui:s `EntityEditScreen` + `EditorState`/`EditorLoader` | Riskprofilen och transaktionsformuläret (UI-15; formuläret med `saveUnchanged` förifyllt, `formError` för kopplingen och fältfel via `errorFor`). Liten topprad med rubrik och kompakt Spara (aktiv när giltigt **och** ändrat), "Släng ändringar?" vid bakåt, sparfel som snackbar, läsfel med "Försök igen" (`loader.retry()`); `horizontalMargin = Spacing.l`. Knapparna har bibliotekets standardstil (UI-13). Inställningar har kvar sitt eget tillstånd tills vidare. |
| Flikrad | android-ui:s `AppFloatingToolbar` (+ `ToolbarItem`) | `AppNavigation` (taggen `APP_FLOATING_TOOLBAR_TAG`). Bibliotekets standard sedan 1.2.0: vald flik fylld i primärfärgen med ikon + namn, ovalda bara ikon (`R.drawable.ic_overview`/`ic_holdings`/`ic_decisions`/`ic_receipt`); kopparmärke via `ToolbarItem.badgeCount`. `Role.Tab` + valt-läge, ≥ 48 dp (NAV-7, UI-14). Ingen egen stil. |
| Plusknapp med meny | android-ui:s `ToolbarAction(label, icon, menu: List<AppMenuItem>)` i `AppFloatingToolbar(action = …)` | `AppNavigation`. Sist i flikraden, i kopparen (`secondary`); fäller ut `AppMenuPopup` med NAV-8:s fyra val; läses "Lägg till" med "Hopfälld"/"Utfälld"; ett val, ett tryck utanför eller bakåt stänger (NAV-8, UI-14). Fonders `AppFabMenu` är borta sedan 1.2.0. |
| Avatarmeny | android-ui:s `AccountMenuAvatar` i `AppNavHost(account = …)` | `FonderAccountMenu` i `AppNavigation`, i flikarnas sidhuvud (NAV-7): Inställningar · [debug: Komponentgalleri] · Om Fonder (`AboutScreen`, bibliotekets `AboutCard`) · Logga in/Logga ut, kontoraden överst när inloggad. Kontot kommer från `AccountViewModel`; inloggningsfel visas som `ErrorSnackbar` över flikraden i `FonderNavHost` – inte via ramens `SyncUiState.writeError`, som skriver "En ändring kunde inte sparas …" före orsaken. Ingen egen meny eller kugghjul. |
| Komponentgalleri (debug) | android-ui:s `ComponentGallery(onBack, appSections)` (`ui-navigation`), anropad från Fonders `ComponentGallery` i `ui/components/` | Avatarmenyn → Komponentgalleri, bara i debug-bygget (`BuildConfig.DEBUG` i `AppNavigation`). Bibliotekets sektioner med varje delad komponent kommer först och Fonders egna sist (`GallerySection`): belopp och andelar, kort/märken/val, analys och dialog, diagram (hopfällt – Vico ritar på en annan tråd). **Fonder ritar ingen kopia av bibliotekets delar i galleriet.** Ny Fonder-komponent i `ui/components` eller `ui/diagram` → lägg den i `appSections` i samma PR (`ComponentGalleryTest` + skärmdump ljust/mörkt); ny bibliotekskomponent läggs i bibliotekets `LibraryGallery.kt`. |
| Topprad / flikrubrik | android-ui:s `AppTopBar(size = TopBarSize.Small, onBack)` och `AppHeader` | Varje skärm djupare i stacken står sedan 1.9.0 (#266 F6) på en ram med liten topprad, bakåtpil och rubrik (UI-8); `FonderPage` och `SubPage` är borta. Flikarna står sedan 1.6.0 på ramarna i flikläge, som ritar Kodhäst-sidhuvudet `AppHeader` med Fonders ikon (`LocalAppIdentity` i `FonderTheme`), flikens rubrik och åtgärder (NAV-7); `TabPage` är borta. Skärmarna på android-ui:s ramar ritar sin egen topprad. Bakåtpilen är bibliotekets tonade platta – `FonderTheme` sätter ingen `LocalTopBarBackStyle` sedan 1.2.0. Fonders egen `AppTopBar` är borta. |

## Diagram (`ui/diagram/`)

| Elementtyp | Komponent | Används i · det som gör den speciell |
|---|---|---|
| Linjediagram | `FundLineChart` | Fonddetalj (kurshistorik, jämförelse), Hem (avkastning), Bytesfönster. Enda stället som rör Vico (TP-12). Periodväljare, köpmarkörer, `comparisonSeries` indexeras till 100 (`ChartSeriesNormalizer`), `valueAxis` för kr eller %. |

| Jämförelsediagram | `ComparisonChart` | Fonddetalj (ANA-11) och Pågående byte (ANA-12). Tre lägen och aldrig ett tomt diagram: hämtar ("Hämtar kurshistorik för jämförelsen …" som text, ingen snurra, UI-19), ingen jämförelse (`unavailableText` säger varför) eller `FundLineChart` med `comparisonSeries`. |

Nytt diagrambehov → utöka `FundLineChart` med en parameter (så byggdes jämförelsen, ANA-11).

## Kända dubbletter – slås ihop

Dessa bryter mot regel 4 i dag. Rör du koden: slå ihop **först**, i en egen commit, och stryk
raden här. Bygg aldrig en tredje kopia.

| Dubblett | Var | Mål |
|---|---|---|
| ~~`ComparisonChart`~~ | ~~`fond/FondDetaljScreen.kt` och `bytesfonster/SwitchWatchScreen.kt`~~ | *(ihopslagen i 1.9.0, #266 F6: den delade `ui/diagram/ComparisonChart.kt`.)* |
`cpdCheck` har ingen baslinje: kopiorna som fanns när kontrollen infördes (66 filuppsättningar)
slogs ihop i 0.78.3 (issue #175) – analysslingan till `FundAnalysisCalc.analyzeAll`, importradernas
huvud till `ImportRowHeader`, testuppställningen till de delade fakes och fixturerna (skill
`testing-strategy` → DRY i tester). Raderna ovan är dubbletter i beteende som CPD inte ser.

## Planerade delade delar

Finns **inte** än – låtsas inte att de gör det. Flyttas upp till tabellerna ovan i PR:en som
bygger dem (med mockup, test och skärmdump ljust + mörkt).

| Del | Vad |
|---|---|
| *(inga just nu – `EntityListScreen` flyttades upp i 0.75.0)* | |

## Kod: delade byggstenar i andra lager

| Behov | Enda stället |
|---|---|
| Portföljberäkningar, avgifter, risk, avkastningsserier | `PortfolioCalc`, `PortfolioFeeCalc`, `PortfolioRiskCalc`, `PortfolioPerformanceCalc`, `PortfolioReturnSeriesCalc`, `PortfolioExposureCalc` i `domain/usecase/` |
| Analys och säljsignaler | `FundAnalysisCalc`, `AnalysisGuidance` |
| Byten | `SwitchPlanCalc`, `SwitchPlanResolver`, `SwitchOutcomeCalc`, `SwitchWatchCalc`; facit ur cachen: `FacitSummary` → `FacitSnapshot` (Facit och Beslut-flikens "Hur gick det?", SET-5/NAV-9); `SwitchWatchRepository.startFromSuggestion` (bevakning ur ett kvitterat förslag – Beslut och Fonddetalj) |
| Det som väntar på beslut och dess antal | `PendingDecisionsSource` → `PendingDecisions` → `SwitchRows.build` (listan Byten, BYT-4: en rad per byte med tillstånd, avslutade i bevakningstiden, SET-8; `proposedCount`/`soldCount`/`feeCount`/`waitingCount`) – Beslut, Översiktens "Att besluta" och flikmärket läser samma källa; de inspelade avgiftsbytena som förslag: `CheaperAlternatives` (ANA-9, även Fonddetaljs kvittering); vilka förslag ett avstående döljer och hur länge: `SuggestionDismissals` (HEM-14, även Fonddetalj) |
| Diagrammets data | `ChartPeriodFilter`, `ChartSeriesNormalizer`, `PurchaseMarkerFilter`, `IndexBenchmarkSelector` |
| Belopp och tal i text | `MoneyFormat`, `SwedishNumberFormat` |
| Fondnamn och matchning | `FundNameKey`, `FundNameMatcher`, `FundCompanyMatcher`, `ImportFundMatcher` |
| Valuta | `CurrencyConverter` |
| Handelsdagar | `NavCalendar` |
| Externa källor | `data/network/` – en klient + en parser per källa (Avanza, Handelsbanken, Riksbanken) |
| Backup-format | `BackupPayload` + `BackupSerializer` (skill `data-safety-backup`) |
| Listans tillstånd (laddning, tomt, fel, innehåll) | android-ui:s `ListUiState` (`se.kodhast.ui:ui-common`, transitivt via `ui-components`) som fält i skärmens tillstånd – Händelser, Beslut, Facit, Fondsök, Portfölj (NFR-6). Raderna med `toListUiState()`, `itemsOrEmpty()`/`errorOrNull()` och ett fel i källan med `catchAsListError(AppErrors)` (alla från `se.kodhast.ui.common`). Detaljskärmarna (Hem, Fonddetalj, Pågående byte) har det lästa som `DetailUiState`-fält på samma sätt, med `valueOrNull()`/`errorOrNull()` och `catchAsDetailError(AppErrors)`; "Försök igen" är `RetrySignal` + `restartOn` efter felfångsten (samma modul); Riskprofilen `EditorState`/`EditorLoader`. `shownAfter` finns i samma modul men används inte än (den skulle fördröja dagens indikatorer) – bygg ingen egen |
| Fel från datalagret | `DataError` + `DataError.of` (Room/SQLite → `Database`, I/O → `Network`, importernas fel) och `AppErrors : ErrorMapper` i `data/common/`; texten `DataError.toMessage()` i `ui/common/DataErrorMessage.kt`. Inloggningens, backupens och Drive-svarens fel (`SignInException.Reason`, `BackupFormatException.Reason`, `DriveResult`) går också via `DataError.of`. `suspendRunCatching` gör ett `Result` av en suspend-åtgärd utan att svälja avbrott. Ett `UiError` visas med `LoadErrorState` eller dess `message` |
| Drive-filnamn och gallring | `DriveBackupFiles` |
| Test-fakes | skill `testing-strategy` |

Beräkningar ligger i `domain/usecase/`, aldrig i en ViewModel eller composable.

## Förbjudet

Mönstren står i **en** fil, `app/src/test/resources/ui-forbidden.txt` (regex + vad som ska
användas i stället, per scope `feature`, `component-only`, `vico`, `dispatchers` och `events`); den här listan
sammanfattar den. Undantag bara i `app/src/test/resources/ui-allowlist.txt`
(`fil:symbol – motivering`, användarens ok i PR:en).

- **Feature-kod** (ui-paket utom `ui/components`, `ui/diagram`, `ui/theme`): nya råa
  Material 3-komponenter som har en delad motsvarighet ovan (`Card`/`Surface`, `ListItem`,
  `AlertDialog`/`Dialog`, knappar och ikonknappar, textfält, datumväljare, `DropdownMenu`, chips,
  progressindikatorer, `Badge`, `Checkbox`, `Switch`, `NavigationBar`, flytande verktygsrad, `FloatingActionButton`,
  `TopAppBar`, avdelare – `HorizontalDivider`/`VerticalDivider`/`Divider` → `AppDivider`). Kvar: `Scaffold` i `AppNavigation`;
  hårdkodade `Color(…)`, `RoundedCornerShape(…)`, `fontSize`/`lineHeight`/`letterSpacing`,
  `TextStyle(…)`, `.shadow(…)`, literal dp för höjd och literala avstånds-dp i `padding(…)`,
  `PaddingValues(…)` och `spacedBy(…)` (använd `Spacing`). Befintliga träffar är skuld, inte förebild –
  avstånden utan token (2, 6, 10, 0 dp) står i baslinjen tills deras skärm ses över.
- **All kod utom `ui/diagram/`:** Vico.
- **All kod utom `ui/components/`:** Material3:s `PullToRefreshBox`, `ExposedDropdownMenuBox`, `ModalBottomSheet` (→ `AppBottomSheet`).
- **All kod utom `di/`:** `Dispatchers.IO`/`Default`/`Main`/`Unconfined`. Klassen tar en
  `CoroutineDispatcher` med `@IoDispatcher`, `@DefaultDispatcher` eller `@MainDispatcher` från
  `di/DispatchersModule` (skill `android:kotlin-coroutines`).
- **All kod i `ui/`** – även `ui/components/`, `ui/diagram/` och `ui/theme/`: `Channel(` och
  `.receiveAsFlow(` för engångshändelser. Händelsen blir ett fält i tillståndet som skärmen agerar
  på i en `LaunchedEffect` nyckelad på värdet och kvitterar till ViewModel:n (`saved` +
  `onSavedHandled()`, `startedSwitchWatchId` + `XxxEvent.SwitchWatchOpened`; skill `android-projekt`, Flöden).
- **UiState-typer** (icke-privata data classes som heter `*State` och sealed interfaces `*UiState` i `ui/`)
  utan `@Immutable` eller `@Stable`. Listor, kartor och mängder i dem är `ImmutableList`/`ImmutableMap`/`ImmutableSet`;
  domänmodellerna, de beräkningsresultat som ligger i tillstånden och datum-/tidstyperna räknas som stabila via `app/compose-stability.conf` (skill `android:compose-expert`).
- **En kontroll utan något att skriva mot** – visa den inte alls (ANA-4-principen).

## Kontroller

| Kontroll | När | Vad |
|---|---|---|
| Pluginens hook `regel4-check` (claude-android, scope i `.claude/regel4.json`) | direkt efter varje `Edit`/`Write` av en `.kt`-fil i `app/src/main`, och vid Stop | samma mönster och undantag som testet; exit 2 med rad, träff och ersättning. Varje repo kontrolleras med sin egen konfiguration |
| `UiConsistencyTest` (Konsist) | `testDebugUnitTest` | hela `app/src/main` mot `ui-forbidden.txt` minus `ui-allowlist.txt`; exemplen i `ui-forbidden-examples.txt` körs både här och i hookens `--verify` i CI, så att hook och bygge tolkar lika; dessutom (bara här) att UiState-typerna är `@Immutable`/`@Stable` |
| `./gradlew cpdCheck` (PMD CPD, 80 tokens) | `build-and-test` | kopierad Kotlin i `core/src` och `app/src/{main,test,androidTest}`; varje kopia fäller bygget (ingen baslinje) |

**Baslinjen** (skuld från före kontrollerna, får bara krympa):

- `ui-allowlist.txt`: rader med motiveringen "baslinje 2026-10 – rättas i skärm-PR:erna" (19 när kontrollen infördes, 0 sedan M3 Expressive steg 2b)
  genereras av pluginens `regel4-check.mjs --baseline .` – kör den när en skärm rättats. Ett undantag gäller
  fil + symbol, så en baslinjerad för `Card(` i `HemScreen.kt` släpper igenom *alla* `Card(` i den
  filen tills den rättats. `--verify` i CI fäller en inaktuell baslinje. Handskrivna undantag står kvar.

## Arbetsflöde

1. Slå upp elementtypen eller behovet i tabellerna. Finns det → använd det.
2. Räcker det inte → **utöka** med en parameter vars default bevarar nuvarande utseende och
   beteende (jfr `PeriodRow.stackValue`, `RiskBadge.toLevel`). Befintliga anropare ändras inte.
3. Något nytt behövs → ny delad del i `ui/components/` eller `ui/diagram/`, rad i tabellen,
   instrumenterat Compose-test i `androidTest/…/ui/components/` – och mockup först (skill `android:mockup` och `android-projekt` → "Mockup").
4. Delade delar utökas eller slås ihop i en egen commit **före** featuren som behöver dem.
5. Två ställen gör nästan samma sak → bryt ut innan det tredje kommer.
6. Komponenten är **stateless**: värde + callback in, state hoistat till anroparen.

## Tillgänglighet

- Avkastning med semantisk färg **och** tecken/pil – aldrig färg ensam.
- Belopp med tabulära siffror (`MonoAmountStyle`), rubriker i Space Grotesk.
- Interaktiva element har `contentDescription` och minst 48 dp (UI-3). android-ui:s `DateField` och
  `FollowedToggleRow` visar mönstret: en nod med roll, etikett och värde.

## Anti-mönster

- Lokal `Card { }` med egen padding "bara här" – eller en egen kopia av `TotalCard`.
- Egen tom-vy i stället för android-ui:s `EmptyState`/`LoadErrorState`; egen `Canvas`-graf vid sidan av `FundLineChart`.
- Kopierad "Genomförd"-kryssruta i stället för `FollowedToggleRow` – en kopia som inte skriver
  mot det inspelade förslaget ser ut att fungera utan att göra något.
- En "nästan likadan" komponent i en feature-mapp som borde bo i `ui/components/`.
- Beräkning i en ViewModel eller composable i stället för i `domain/usecase/`.
- Samma text formulerad på två sätt i `strings.xml`.
- En regeltabell kopierad till en annan fil i stället för en länk hit.
