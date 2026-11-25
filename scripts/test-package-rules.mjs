// Tests the Firestore rules of the collection "packages" on the live Firebase project.
// It signs in with real test accounts and tries to write, so it needs internet and the published rules.
//
// Run from the repo root (PowerShell), with the passwords in environment variables, never in a file:
//   $env:CUSTOMER_EMAIL=...; $env:CUSTOMER_PASSWORD=...
//   $env:MERCHANT_EMAIL=...; $env:MERCHANT_PASSWORD=...
//   $env:OTHER_MERCHANT_EMAIL=...; $env:OTHER_MERCHANT_PASSWORD=...   (optional: a second merchant that already exists)
//   node scripts/test-package-rules.mjs
//
// The script never registers an account. Without a second merchant the cases that need one are skipped.
//
// The API key is read from app/google-services.json (that file is not in git). The documents
// the script makes are deleted at the end.
import { readFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';

const googleServices = JSON.parse(readFileSync(new URL('../app/google-services.json', import.meta.url)));
const API_KEY = googleServices.client[0].api_key[0].current_key;
const PROJECT = googleServices.project_info.project_id;
const DOCS = `projects/${PROJECT}/databases/(default)/documents`;
const FIRESTORE = `https://firestore.googleapis.com/v1/${DOCS}`;

function env(name) {
  const value = process.env[name];
  if (!value) { console.error(`Set the environment variable ${name}`); process.exit(2); }
  return value;
}

async function identity(action, body) {
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:${action}?key=${API_KEY}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...body, returnSecureToken: true })
  });
  return { status: response.status, json: await response.json() };
}

async function commit(token, writes) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${FIRESTORE}:commit`, { method: 'POST', headers, body: JSON.stringify({ writes }) });
  return response.status;
}

async function read(token, id) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  return (await fetch(`${FIRESTORE}/packages/${id}`, { headers })).status;
}

const str = (value) => ({ stringValue: value });
const int = (value) => ({ integerValue: String(value) });

/** A package that passes all the rules; each case changes one thing. */
function validFields(uid, overrides = {}) {
  return {
    ownerId: str(uid), shopId: str(uid), title: str('Brood en koffiekoeken'), description: str('Een mix van brood.'),
    priceCents: int(400), originalPriceCents: int(1200), quantity: int(10), pickupDate: str('2026-11-25'),
    pickupFrom: str('18:00'), pickupTo: str('19:30'), visible: { booleanValue: true }, ...overrides
  };
}

const createWrite = (id, fields, { serverTime = true } = {}) => ({
  update: { name: `${DOCS}/packages/${id}`, fields },
  updateTransforms: serverTime ? [{ fieldPath: 'createdAt', setToServerValue: 'REQUEST_TIME' }] : [],
  currentDocument: { exists: false }
});

const patchWrite = (id, fields) => ({
  update: { name: `${DOCS}/packages/${id}`, fields },
  updateMask: { fieldPaths: Object.keys(fields) }
});

const deleteWrite = (id) => ({ delete: `${DOCS}/packages/${id}` });

let failures = 0;
let total = 0;
function check(name, status, shouldAllow) {
  total += 1;
  const allowed = status === 200;
  const denied = status === 403;
  const ok = shouldAllow ? allowed : denied;
  if (!ok) failures += 1;
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${name} (HTTP ${status}, expected ${shouldAllow ? 'allowed' : 'denied'})`);
}

async function signIn(email, password) {
  const result = await identity('signInWithPassword', { email, password });
  if (result.status !== 200) throw new Error(`Cannot sign in ${email}: ${JSON.stringify(result.json.error)}`);
  return { token: result.json.idToken, uid: result.json.localId };
}

const customer = await signIn(env('CUSTOMER_EMAIL'), env('CUSTOMER_PASSWORD'));
const merchant = await signIn(env('MERCHANT_EMAIL'), env('MERCHANT_PASSWORD'));
// A second merchant is optional: only signed in when the variables are set.
const other = process.env.OTHER_MERCHANT_EMAIL && process.env.OTHER_MERCHANT_PASSWORD
  ? await signIn(process.env.OTHER_MERCHANT_EMAIL, process.env.OTHER_MERCHANT_PASSWORD)
  : null;
// A made-up owner, to test that the owner of a package must be the signed-in merchant.
const STRANGER_UID = 'uid-of-somebody-else';

const created = [];
const newId = () => { const id = `ruletest-${randomUUID()}`; created.push(id); return id; };
const longText = (n) => 'x'.repeat(n);

try {
  console.log('--- create');
  const mainId = newId();
  check('merchant creates a valid package', await commit(merchant.token, [createWrite(mainId, validFields(merchant.uid))]), true);
  check('customer cannot create a package', await commit(customer.token, [createWrite(newId(), validFields(customer.uid))]), false);
  check('nobody signed in cannot create a package', await commit(null, [createWrite(newId(), validFields(merchant.uid))]), false);
  check('merchant cannot create a package for another owner', await commit(merchant.token, [createWrite(newId(), validFields(STRANGER_UID))]), false);
  check('merchant cannot give another shopId', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { shopId: str(STRANGER_UID) }))]), false);
  if (other) check('another merchant cannot create a package for this merchant', await commit(other.token, [createWrite(newId(), validFields(merchant.uid))]), false);
  check('price higher than the original value is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { priceCents: int(1400), originalPriceCents: int(1200) }))]), false);
  check('price equal to the original value is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { priceCents: int(1200) }))]), false);
  check('price 0 is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { priceCents: int(0) }))]), false);
  check('price as text is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { priceCents: str('400') }))]), false);
  check('empty title is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { title: str('') }))]), false);
  check('title of 61 characters is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { title: str(longText(61)) }))]), false);
  check('title of 60 characters is allowed', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { title: str(longText(60)) }))]), true);
  check('description of 501 characters is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { description: str(longText(501)) }))]), false);
  check('quantity 0 is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { quantity: int(0) }))]), false);
  check('quantity 1000 is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { quantity: int(1000) }))]), false);
  check('wrong date format is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { pickupDate: str('25/11/2026') }))]), false);
  check('time 25:00 is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { pickupTo: str('25:00') }))]), false);
  check('pick-up from after until is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { pickupFrom: str('19:30'), pickupTo: str('18:00') }))]), false);
  check('an extra field is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { secret: str('x') }))]), false);
  check('a createdAt of the client is refused', await commit(merchant.token, [createWrite(newId(), validFields(merchant.uid, { createdAt: { timestampValue: '2020-01-01T00:00:00Z' } }), { serverTime: false })]), false);

  console.log('--- read');
  check('customer can read a package', await read(customer.token, mainId), true);
  check('nobody signed in cannot read a package', await read(null, mainId), false);

  console.log('--- update');
  check('owner can switch visible off', await commit(merchant.token, [patchWrite(mainId, { visible: { booleanValue: false } })]), true);
  if (other) check('other merchant cannot change a package', await commit(other.token, [patchWrite(mainId, { visible: { booleanValue: true } })]), false);
  check('customer cannot change a package', await commit(customer.token, [patchWrite(mainId, { visible: { booleanValue: true } })]), false);
  check('owner cannot give the package to somebody else', await commit(merchant.token, [patchWrite(mainId, { ownerId: str(STRANGER_UID) })]), false);
  check('owner cannot make the price higher than the original', await commit(merchant.token, [patchWrite(mainId, { priceCents: int(5000) })]), false);

  console.log('--- delete');
  check('customer cannot delete a package', await commit(customer.token, [deleteWrite(mainId)]), false);
  if (other) check('other merchant cannot delete a package', await commit(other.token, [deleteWrite(mainId)]), false);
  check('owner can delete a package', await commit(merchant.token, [deleteWrite(mainId)]), true);
} finally {
  // Clean up everything that was made, also when a case failed. Deleting a missing document is fine.
  for (const id of created) await commit(merchant.token, [deleteWrite(id)]);
}

if (!other) console.log('\nSkipped: the cases with a second merchant (set OTHER_MERCHANT_EMAIL and OTHER_MERCHANT_PASSWORD).');
console.log(`\n${total - failures} of ${total} cases as expected`);
process.exit(failures === 0 ? 0 : 1);
