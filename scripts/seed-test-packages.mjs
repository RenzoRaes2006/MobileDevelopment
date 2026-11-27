// Puts a fixed set of test packages for TODAY online for the test merchant, so testing does not start
// with filling in forms. Every test package has a title that starts with "Test · ".
//
// Run from the repo root (PowerShell), with the passwords in environment variables, never in a file:
//   $env:MERCHANT_EMAIL=...; $env:MERCHANT_PASSWORD=...
//   $env:OTHER_MERCHANT_EMAIL=...; $env:OTHER_MERCHANT_PASSWORD=...   (optional: a second merchant with a shop)
//   node scripts/seed-test-packages.mjs            -> test packages without photos
//   node scripts/seed-test-packages.mjs --photos   -> also packages with a photo (needs the rules with photoBase64)
//   node scripts/seed-test-packages.mjs --clean    -> only removes the test packages
//
// The script first removes the old test packages of the merchant (all days), so running it twice gives
// no doubles. Other packages are never touched. It never registers an account.
// The API key is read from app/google-services.json (that file is not in git).
import { readFileSync } from 'node:fs';

const googleServices = JSON.parse(readFileSync(new URL('../app/google-services.json', import.meta.url)));
const API_KEY = googleServices.client[0].api_key[0].current_key;
const PROJECT = googleServices.project_info.project_id;
const DOCS = `projects/${PROJECT}/databases/(default)/documents`;
const FIRESTORE = `https://firestore.googleapis.com/v1/${DOCS}`;

const TEST_PREFIX = 'Test · ';
const withPhotos = process.argv.includes('--photos');
const onlyClean = process.argv.includes('--clean');

function env(name) {
  const value = process.env[name];
  if (!value) { console.error(`Set the environment variable ${name}`); process.exit(2); }
  return value;
}

async function signIn(email, password) {
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${API_KEY}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true })
  });
  const json = await response.json();
  if (response.status !== 200) throw new Error(`Cannot sign in ${email}: ${json.error?.message}`);
  return { token: json.idToken, uid: json.localId, email };
}

async function firestore(token, path, body) {
  const response = await fetch(`${FIRESTORE}${path}`, {
    method: body ? 'POST' : 'GET',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body ? JSON.stringify(body) : undefined
  });
  return { status: response.status, json: await response.json() };
}

/** The shop of the merchant (shops/{uid}), or null when the merchant has none yet. */
async function shopOf(merchant) {
  const { status, json } = await firestore(merchant.token, `/shops/${merchant.uid}`);
  return status === 200 ? json.fields : null;
}

/** The ids of the test packages of this merchant, of every day. */
async function testPackageIds(merchant) {
  const where = { fieldFilter: { field: { fieldPath: 'ownerId' }, op: 'EQUAL', value: { stringValue: merchant.uid } } };
  const { json } = await firestore(merchant.token, ':runQuery', { structuredQuery: { from: [{ collectionId: 'packages' }], where } });
  return json
    .filter((row) => row.document?.fields?.title?.stringValue?.startsWith(TEST_PREFIX))
    .map((row) => row.document.name.split('/').pop());
}

async function removeTestPackages(merchant) {
  const ids = await testPackageIds(merchant);
  if (ids.length === 0) return 0;
  const writes = ids.map((id) => ({ delete: `${DOCS}/packages/${id}` }));
  const { status, json } = await firestore(merchant.token, ':commit', { writes });
  if (status !== 200) throw new Error(`Removing failed: ${JSON.stringify(json.error)}`);
  return ids.length;
}

// --- Times: the app compares "HH:mm" text and uses the date of today in Belgium.
const BELGIUM = 'Europe/Brussels';
const today = new Date().toLocaleDateString('sv-SE', { timeZone: BELGIUM }); // "yyyy-MM-dd"
const hourNow = Number(new Intl.DateTimeFormat('en-GB', { timeZone: BELGIUM, hour: '2-digit', hourCycle: 'h23' }).format(new Date()));
const hh = (hour) => `${String(Math.min(hour, 23)).padStart(2, '0')}:00`;

/** A window that starts at the next full hour and lasts [hours]; late in the evening it ends at 23:59. */
function upcomingWindow(hours) {
  const from = Math.min(hourNow + 1, 22);
  const to = from + hours > 23 ? '23:59' : hh(from + hours);
  return { from: hh(from), to };
}

const photo = (file) => readFileSync(new URL(`./seed-photos/${file}`, import.meta.url)).toString('base64');

/** The test packages. Each one tests something on the screens; [why] is printed so you know what to look for. */
function testPackages() {
  const soon = upcomingWindow(2);
  const later = upcomingWindow(4);
  const list = [
    { why: 'normal package', title: 'Brood en koffiekoeken', price: 400, original: 1200, quantity: 6, window: soon },
    { why: 'only 1 left', title: 'Laatste stuk taart', price: 250, original: 700, quantity: 1, window: later },
    { why: 'title of 60 characters: must be cut off with "…" in the list and the map card', title: 'Een heel lange titel om het afkorten in de lijst te testen', price: 500, original: 1500, quantity: 3, window: later },
    { why: 'offline: the customer must NOT see it', title: 'Offline pakket', price: 300, original: 900, quantity: 4, window: later, visible: false }
  ];
  // A window that already ended today: K3 says it must not be shown (not built yet, so it shows now).
  if (hourNow >= 8) {
    list.push({ why: 'window already over: must disappear once K3 filters it', title: 'Venster voorbij', price: 200, original: 600, quantity: 2, window: { from: '06:00', to: '07:00' } });
  }
  if (withPhotos) {
    list.push(
      { why: 'with a photo (bread)', title: 'Broodmand met foto', price: 450, original: 1300, quantity: 5, window: soon, photoBase64: photo('brood.jpg') },
      { why: 'with a photo (vegetables)', title: 'Groentenbox met foto', price: 600, original: 1800, quantity: 2, window: later, photoBase64: photo('groenten.jpg') }
    );
  }
  return list;
}

/** One write that creates a package exactly as the app does, with the server time as createdAt. */
function createWrite(merchant, pack) {
  const fields = {
    ownerId: { stringValue: merchant.uid },
    shopId: { stringValue: merchant.uid },
    title: { stringValue: (TEST_PREFIX + pack.title).slice(0, 60) },
    description: { stringValue: `Testpakket: ${pack.why}.`.slice(0, 500) },
    priceCents: { integerValue: String(pack.price) },
    originalPriceCents: { integerValue: String(pack.original) },
    quantity: { integerValue: String(pack.quantity) },
    pickupDate: { stringValue: today },
    pickupFrom: { stringValue: pack.window.from },
    pickupTo: { stringValue: pack.window.to },
    visible: { booleanValue: pack.visible ?? true }
  };
  if (pack.photoBase64) fields.photoBase64 = { stringValue: pack.photoBase64 };
  return {
    update: { name: `${DOCS}/packages/test-${Math.random().toString(36).slice(2, 12)}`, fields },
    updateTransforms: [{ fieldPath: 'createdAt', setToServerValue: 'REQUEST_TIME' }],
    currentDocument: { exists: false }
  };
}

async function seed(merchant, packages) {
  const shop = await shopOf(merchant);
  if (!shop) {
    console.log(`  ${merchant.email} has no shop yet: fill in "Mijn winkel" in the app first. Skipped.`);
    return;
  }
  console.log(`  shop: ${shop.name?.stringValue} (${shop.street?.stringValue} ${shop.houseNumber?.stringValue}, ${shop.municipality?.stringValue})`);
  // One write per package, so one refused package (for example a photo with the old rules) does not stop the rest.
  for (const pack of packages) {
    const { status, json } = await firestore(merchant.token, ':commit', { writes: [createWrite(merchant, pack)] });
    const result = status === 200 ? 'ok    ' : `REFUSED (HTTP ${status}${status === 403 ? ', are the rules with photoBase64 published?' : ''})`;
    console.log(`  ${result} ${pack.title} ${pack.window.from}-${pack.window.to} -> ${pack.why}`);
    if (status !== 200 && status !== 403) console.log(`         ${JSON.stringify(json.error?.message)}`);
  }
}

const merchants = [await signIn(env('MERCHANT_EMAIL'), env('MERCHANT_PASSWORD'))];
if (process.env.OTHER_MERCHANT_EMAIL && process.env.OTHER_MERCHANT_PASSWORD) {
  merchants.push(await signIn(process.env.OTHER_MERCHANT_EMAIL, process.env.OTHER_MERCHANT_PASSWORD));
}

for (const [index, merchant] of merchants.entries()) {
  console.log(`\n${merchant.email}`);
  console.log(`  removed ${await removeTestPackages(merchant)} old test package(s)`);
  if (onlyClean) continue;
  // The second merchant gets two packages, enough to see a second shop on the map.
  await seed(merchant, index === 0 ? testPackages() : testPackages().slice(0, 2));
}
console.log(onlyClean ? '\nTest packages removed.' : `\nDone: test packages for ${today}. Open the app (or go back to it) to load them.`);
