# AGENTS.md

Guía para agentes de IA (y personas) que trabajen en este repositorio. Sigue estas reglas **siempre** al generar, modificar o revisar código.

---

## 1. Stack tecnológico

| Tecnología | Uso |
|---|---|
| **Angular 21** | Framework principal (standalone components, control flow nativo, signals) |
| **PrimeNG** | Librería de componentes UI |
| **Tailwind CSS** | Estilos utilitarios y layout |
| **TypeScript (modo `strict`)** | Lenguaje |
| **RxJS** | Flujos asíncronos (HTTP, eventos de formularios) |

---

## 2. Reglas críticas (no negociables)

1. **PROHIBIDO usar `any`** bajo cualquier circunstancia: ni explícito, ni implícito, ni en genéricos (`Array<any>`, `Record<string, any>`), ni en casteos (`as any`).
   - Si el tipo es desconocido usa `unknown` y estréchalo con *type guards*.
   - Si un tipo es complejo, crea una `interface` o `type` en la carpeta `models/` del feature.
2. **Inyección de dependencias solo con `inject()`**. No usar inyección por constructor.
3. **Formularios solo con Reactive Forms tipados + `NonNullableFormBuilder`**. **No usar Signal Forms** (`@angular/forms/signals`) ni Template-driven forms (`ngModel`).
4. **Signals sí** para estado de componentes y servicios (`signal`, `computed`, `effect`, `input`, `output`, `model`), **excepto** para formularios.
5. **Estructura por features** (ver sección 3). No crear carpetas globales por tipo (`/components`, `/services`) fuera de `core` y `shared`.

---

## 3. Estructura de carpetas (feature-based)

```
src/
├── app/
│   ├── core/                     # Singleton: se usa una sola vez en la app
│   │   ├── guards/
│   │   ├── interceptors/
│   │   ├── services/             # auth, config, logger, etc.
│   │   └── models/
│   ├── shared/                   # Reutilizable entre features
│   │   ├── components/
│   │   ├── directives/
│   │   ├── pipes/
│   │   ├── validators/
│   │   └── models/
│   ├── features/
│   │   └── users/                # Un feature = una carpeta
│   │       ├── pages/            # Componentes enrutables (smart)
│   │       │   └── user-list/
│   │       │       ├── user-list.page.ts
│   │       │       ├── user-list.page.html
│   │       │       └── user-list.page.spec.ts
│   │       ├── components/       # Componentes de presentación (dumb)
│   │       │   └── user-form/
│   │       ├── services/
│   │       │   └── users.service.ts
│   │       ├── models/
│   │       │   └── user.model.ts
│   │       └── users.routes.ts   # Rutas del feature (lazy loading)
│   ├── layout/                   # Shell: header, sidebar, footer
│   ├── app.config.ts
│   ├── app.routes.ts
│   └── app.ts
├── styles.css                    # Tailwind + PrimeNG
└── main.ts
```

### Reglas de la estructura

- Un feature **no importa** nada de otro feature. Si algo se comparte, se mueve a `shared/`.
- `core/` no importa de `features/`.
- Cada feature expone sus rutas en `<feature>.routes.ts` y se carga con *lazy loading*.
- Nombres de archivo en `kebab-case`: `user-form.component.ts`, `users.service.ts`, `user.model.ts`.

---

## 4. Componentes

- Todos los componentes son **standalone** (en Angular 21 es el valor por defecto; no escribas `standalone: true`).
- Usa `ChangeDetectionStrategy.OnPush` siempre.
- Usa `input()`, `output()` y `model()` en lugar de `@Input()` / `@Output()`.
- Usa el control flow nativo: `@if`, `@for` (con `track`), `@switch`, `@defer`. **No** uses `*ngIf`, `*ngFor`.
- Usa bindings de `class` y `style` en lugar de `ngClass` / `ngStyle`.
- Usa la propiedad `host` del decorador en lugar de `@HostBinding` / `@HostListener`.
- Templates y estilos en archivos separados cuando superen unas pocas líneas.

```ts
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-user-card',
  imports: [ButtonModule],
  templateUrl: './user-card.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserCardComponent {
  readonly user = input.required<User>();
  readonly selected = output<User>();

  protected readonly fullName = computed(
    () => `${this.user().firstName} ${this.user().lastName}`,
  );

  protected onSelect(): void {
    this.selected.emit(this.user());
  }
}
```

```html
<div class="flex items-center justify-between rounded-lg p-4 shadow-sm">
  <span class="font-semibold">{{ fullName() }}</span>
  <p-button label="Ver" (onClick)="onSelect()" />
</div>
```

---

## 5. Inyección de dependencias con `inject()`

```ts
// ✅ Correcto
export class UserListPage {
  private readonly usersService = inject(UsersService);
  private readonly router = inject(Router);
}

// ❌ Incorrecto
export class UserListPage {
  constructor(private usersService: UsersService) {}
}
```

- Las propiedades inyectadas son `private readonly` (o `protected readonly` si se usan en el template).
- Servicios globales: `@Injectable({ providedIn: 'root' })`.
- Servicios con estado propio de un feature: proveerlos en las rutas del feature (`providers` en la ruta).

---

## 6. Estado con Signals

Usa signals para el estado de componentes y servicios:

```ts
@Injectable({ providedIn: 'root' })
export class UsersStore {
  private readonly usersService = inject(UsersService);

  private readonly _users = signal<User[]>([]);
  private readonly _loading = signal<boolean>(false);

  readonly users = this._users.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly total = computed(() => this._users().length);

  load(): void {
    this._loading.set(true);
    this.usersService.getAll().subscribe({
      next: (users) => this._users.set(users),
      complete: () => this._loading.set(false),
      error: () => this._loading.set(false),
    });
  }
}
```

- Expón signals de solo lectura (`asReadonly()`) y modifica el estado mediante métodos.
- Usa `update()` para derivar del valor anterior, nunca mutes arrays/objetos internos.
- Usa `toSignal()` para convertir Observables a signals en componentes.
- Usa `effect()` solo para efectos secundarios (logging, sincronización con APIs externas), **no** para derivar estado (para eso está `computed`).

---

## 7. Formularios: Reactive Forms con `NonNullableFormBuilder`

> ⚠️ **No usar Signal Forms.** Los formularios se hacen como en versiones previas de Angular: **Reactive Forms tipados** con `NonNullableFormBuilder`.

### Reglas

- Inyecta siempre `NonNullableFormBuilder` con `inject()`.
- Define una `interface` para la forma del formulario con los tipos de control (`FormControl<T>`).
- Tipa el `FormGroup` explícitamente; nunca `FormGroup<any>` ni `UntypedFormGroup`.
- Obtén los valores con `getRawValue()` (devuelve el tipo completo, incluidos controles deshabilitados).
- Validadores personalizados en `shared/validators/` y tipados como `ValidatorFn`.
- Para reaccionar a cambios usa `valueChanges` / `statusChanges` (RxJS) con `takeUntilDestroyed()`.

### Ejemplo

```ts
// features/users/models/user-form.model.ts
import { FormControl } from '@angular/forms';

export interface UserForm {
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  email: FormControl<string>;
  age: FormControl<number | null>;
  active: FormControl<boolean>;
}

export interface UserFormValue {
  firstName: string;
  lastName: string;
  email: string;
  age: number | null;
  active: boolean;
}
```

```ts
// features/users/components/user-form/user-form.component.ts
import { ChangeDetectionStrategy, Component, DestroyRef, inject, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { CheckboxModule } from 'primeng/checkbox';
import { UserForm, UserFormValue } from '../../models/user-form.model';

@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, InputNumberModule, CheckboxModule],
  templateUrl: './user-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitted = output<UserFormValue>();

  protected readonly form: FormGroup<UserForm> = this.fb.group({
    firstName: this.fb.control('', [Validators.required, Validators.maxLength(50)]),
    lastName: this.fb.control('', [Validators.required]),
    email: this.fb.control('', [Validators.required, Validators.email]),
    age: this.fb.control<number | null>(null, [Validators.min(18)]),
    active: this.fb.control(true),
  });

  constructor() {
    this.form.controls.active.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((active: boolean) => {
        if (active) {
          this.form.controls.age.enable();
        } else {
          this.form.controls.age.disable();
        }
      });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value: UserFormValue = this.form.getRawValue();
    this.submitted.emit(value);
  }
}
```

```html
<form [formGroup]="form" (ngSubmit)="onSubmit()" class="flex flex-col gap-4">
  <div class="flex flex-col gap-1">
    <label for="firstName" class="text-sm font-medium">Nombre</label>
    <input pInputText id="firstName" formControlName="firstName" />
    @if (form.controls.firstName.touched && form.controls.firstName.hasError('required')) {
      <small class="text-red-500">El nombre es obligatorio</small>
    }
  </div>

  <div class="flex flex-col gap-1">
    <label for="email" class="text-sm font-medium">Email</label>
    <input pInputText id="email" formControlName="email" />
  </div>

  <div class="flex flex-col gap-1">
    <label for="age" class="text-sm font-medium">Edad</label>
    <p-inputnumber inputId="age" formControlName="age" />
  </div>

  <div class="flex items-center gap-2">
    <p-checkbox inputId="active" formControlName="active" [binary]="true" />
    <label for="active">Activo</label>
  </div>

  <p-button type="submit" label="Guardar" [disabled]="form.invalid" />
</form>
```

### Validador personalizado tipado

```ts
// shared/validators/no-whitespace.validator.ts
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const noWhitespaceValidator: ValidatorFn = (
  control: AbstractControl<string>,
): ValidationErrors | null => {
  const value = control.value;
  return value.trim().length === 0 && value.length > 0 ? { whitespace: true } : null;
};
```

---

## 8. Servicios HTTP

- Usa `inject(HttpClient)` y tipa siempre la respuesta con genéricos.
- Los modelos de la API van en `models/` del feature.
- Los servicios devuelven `Observable<T>`; la conversión a signal se hace en el store o el componente.

```ts
@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/users';

  getAll(): Observable<User[]> {
    return this.http.get<User[]>(this.baseUrl);
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  create(payload: CreateUserDto): Observable<User> {
    return this.http.post<User>(this.baseUrl, payload);
  }
}
```

---

## 9. Cómo evitar `any`

| Situación | En lugar de `any` usa |
|---|---|
| Dato externo desconocido (JSON, `localStorage`) | `unknown` + type guard |
| Errores en `catch` / `catchError` | `unknown` o `HttpErrorResponse` y estrechar con `instanceof` |
| Objeto con claves dinámicas | `Record<string, T>` con `T` concreto |
| Función genérica | Genéricos `<T>` |
| Eventos del DOM | `Event`, `KeyboardEvent`, `MouseEvent` + `instanceof HTMLInputElement` |
| Eventos de PrimeNG | Tipos exportados por PrimeNG (p. ej. `TableLazyLoadEvent`) |

```ts
// Type guard
export function isUser(value: unknown): value is User {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    typeof value.id === 'number' &&
    'email' in value &&
    typeof value.email === 'string'
  );
}

// Manejo de errores
catchError((error: unknown) => {
  const message = error instanceof HttpErrorResponse ? error.message : 'Error desconocido';
  return throwError(() => new Error(message));
});

// Eventos del DOM
protected onInput(event: Event): void {
  if (event.target instanceof HTMLInputElement) {
    this.search.set(event.target.value);
  }
}
```

### Configuración que lo hace cumplir

`tsconfig.json`:

```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "noImplicitReturns": true,
    "noUncheckedIndexedAccess": true
  },
  "angularCompilerOptions": {
    "strictTemplates": true,
    "strictInjectionParameters": true,
    "strictInputAccessModifiers": true
  }
}
```

ESLint (`eslint.config.js`):

```js
rules: {
  '@typescript-eslint/no-explicit-any': 'error',
  '@typescript-eslint/no-unsafe-assignment': 'error',
  '@typescript-eslint/no-unsafe-member-access': 'error',
  '@typescript-eslint/no-unsafe-call': 'error',
  '@typescript-eslint/no-unsafe-return': 'error',
  '@typescript-eslint/no-unsafe-argument': 'error',
}
```

---

## 10. Estilos: Tailwind CSS + PrimeNG

- **Layout, espaciado, tipografía y colores:** Tailwind.
- **Componentes interactivos** (tablas, diálogos, selects, date pickers, toasts…): PrimeNG.
- No reescribas componentes que PrimeNG ya ofrece.
- Para personalizar PrimeNG, usa el sistema de temas (preset / design tokens) o las props `styleClass` / `pt` (pass-through) con clases de Tailwind. Evita `::ng-deep`.
- No uses CSS en línea (`style="..."`) salvo valores dinámicos imposibles de expresar con clases.
- Evita archivos `.css` de componente salvo que sea imprescindible.

Configuración de PrimeNG en `app.config.ts`:

```ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.app-dark',
          cssLayer: {
            name: 'primeng',
            order: 'theme, base, primeng',
          },
        },
      },
    }),
  ],
};
```

`styles.css`:

```css
@import 'tailwindcss';
@import 'tailwindcss-primeui';
```

---

## 11. Routing

- Lazy loading por feature con `loadChildren` y por página con `loadComponent`.
- Guards e interceptores **funcionales** (`CanActivateFn`, `HttpInterceptorFn`), no basados en clases.

```ts
// app.routes.ts
export const routes: Routes = [
  {
    path: 'users',
    canActivate: [authGuard],
    loadChildren: () => import('./features/users/users.routes').then((m) => m.USERS_ROUTES),
  },
  { path: '', pathMatch: 'full', redirectTo: 'users' },
];

// features/users/users.routes.ts
export const USERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/user-list/user-list.page').then((m) => m.UserListPage),
  },
];
```

---

## 12. Convenciones de nombres

| Elemento | Convención | Ejemplo |
|---|---|---|
| Archivos | kebab-case | `user-form.component.ts` |
| Clases | PascalCase | `UserFormComponent` |
| Páginas enrutables | sufijo `.page.ts` / `Page` | `UserListPage` |
| Interfaces / tipos | PascalCase, sin prefijo `I` | `User`, `UserForm` |
| Signals privados con setter público | prefijo `_` | `_users` / `users` |
| Constantes de rutas | UPPER_SNAKE_CASE | `USERS_ROUTES` |
| Selectores | prefijo `app-` | `app-user-card` |

---

## 13. Checklist antes de entregar código

- [ ] No hay ningún `any` (explícito, implícito ni en casteos).
- [ ] Todas las dependencias se obtienen con `inject()`.
- [ ] Los formularios usan `NonNullableFormBuilder` con `FormGroup<Interface>` tipado (sin Signal Forms).
- [ ] El código nuevo vive en la carpeta del feature correspondiente.
- [ ] Componentes con `OnPush`, `input()`/`output()` y control flow `@if`/`@for`.
- [ ] Estilos con Tailwind; componentes UI con PrimeNG.
- [ ] Suscripciones manuales cerradas con `takeUntilDestroyed()`.
- [ ] `ng build` y `ng lint` pasan sin errores ni advertencias.