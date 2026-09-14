#!/usr/bin/env node
// Prueba manual del flujo de login con cookies httpOnly + CSRF.
// Uso: node backend/scripts/test-auth-flow.mjs
// Requiere el backend corriendo en BASE_URL (default http://localhost:8082).

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8082';
const EMAIL = process.env.TEST_EMAIL ?? 'miguel@correo.com';
const PASSWORD = process.env.TEST_PASSWORD ?? 'Glpi12345$';

const cookieJar = new Map();

function updateCookieJar(response) {
    const setCookieHeaders = response.headers.getSetCookie();
    for (const raw of setCookieHeaders) {
        const pair = raw.split(';')[0];
        const separatorIndex = pair.indexOf('=');
        const name = pair.slice(0, separatorIndex).trim();
        const value = pair.slice(separatorIndex + 1).trim();
        cookieJar.set(name, value);
    }
    return setCookieHeaders;
}

function cookieHeader() {
    return [...cookieJar.entries()].map(([name, value]) => `${name}=${value}`).join('; ');
}

function describeCookie(raw) {
    const [nameValue, ...attributes] = raw.split(';').map((part) => part.trim());
    return `  - ${nameValue} (${attributes.join(', ') || 'sin atributos'})`;
}

async function printResponse(label, response) {
    const setCookieHeaders = updateCookieJar(response);
    const contentType = response.headers.get('content-type') ?? '';
    const rawBody = await response.text();

    let body = rawBody || '(vacío)';
    if (contentType.includes('application/json') && rawBody) {
        try {
            body = JSON.stringify(JSON.parse(rawBody), null, 2);
        } catch {
            body = rawBody;
        }
    }

    console.log(`\n=== ${label} ===`);
    console.log(`Status: ${response.status} ${response.statusText}`);
    if (setCookieHeaders.length > 0) {
        console.log('Set-Cookie:');
        setCookieHeaders.forEach((raw) => console.log(describeCookie(raw)));
    }
    console.log(`Body:\n${body}`);

    return { status: response.status, rawBody };
}

async function warmUpCsrfCookie() {
    const response = await fetch(`${BASE_URL}/api/v1/auth/login`, {
        method: 'GET',
        headers: { Cookie: cookieHeader() },
    });
    await printResponse('GET /api/v1/auth/login (warm-up CSRF)', response);
}

async function login() {
    const xsrfToken = cookieJar.get('XSRF-TOKEN') ?? '';
    const response = await fetch(`${BASE_URL}/api/v1/auth/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': xsrfToken,
            Cookie: cookieHeader(),
        },
        body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
    });
    return printResponse('POST /api/v1/auth/login', response);
}

async function main() {
    console.log(`Probando login contra ${BASE_URL} con email=${EMAIL}`);

    await warmUpCsrfCookie();
    const { status } = await login();

    const hasAccessToken = cookieJar.has('access_token');
    const hasRefreshToken = cookieJar.has('refresh_token');

    console.log('\n=== Resumen ===');
    if (status === 200 && hasAccessToken && hasRefreshToken) {
        console.log('Login OK: se recibieron access_token y refresh_token.');
    } else {
        console.log(`Login FALLÓ: status=${status}, access_token=${hasAccessToken}, refresh_token=${hasRefreshToken}`);
    }
}

main().catch((error) => {
    console.error('Error ejecutando el script:', error);
    process.exitCode = 1;
});
