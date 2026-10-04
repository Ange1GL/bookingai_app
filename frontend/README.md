# BookingApp — Frontend

Angular 21 + PrimeNG 21 + Tailwind 4, organized by feature (`core / shared / feature`). See [docs/frontend-structure.md](../docs/frontend-structure.md).

**Prerequisites:** Node 22+, backend running on `http://localhost:8082` (dev profile).

## Development server

To start a local development server, run:

```bash
npm start
```

The API host comes from `src/environments/environment.ts` (`http://localhost:8082` in dev). Services write the full path (`${environment.apiBaseUrl}/api/v1/...`).

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev) runner (`@angular/build:unit-test`), use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

## Security audit

```bash
npm run audit
```

Fails on high/critical advisories (override with `AUDIT_LEVEL`). Never run `npm audit fix --force`: it downgrades majors (e.g. Karma 6 to 4) and increases vulnerabilities. See [docs/security-remediation-plan.md](../docs/security-remediation-plan.md).
