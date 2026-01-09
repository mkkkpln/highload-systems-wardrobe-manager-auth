# Wardrobe Manager - Микросервисная архитектура

## Текущая архитектура проекта

Проект представляет собой полнофункциональную микросервисную систему Wardrobe Manager с следующими характеристиками:

### **Технологический стек:**
- **Инфраструктура**: Spring Cloud Netflix (Eureka, Config Server, Gateway)
- **Базы данных**: PostgreSQL + **Flyway миграции** 
- **Мониторинг**: Spring Boot Actuator, Circuit Breaker (Resilience4j)
- **Тестирование**: JUnit 5, Testcontainers, Mockito, Jacoco (покрытие >70%)

### **Архитектура микросервисов:**

**Краткая характеристика сервисов:**
- **discovery-service (Eureka)** - Сервис регистрации и обнаружения
- **config-server** - Централизованный сервер конфигурации
- **api-gateway** - API Gateway для маршрутизации запросов
- **user-service** - Сервис управления пользователями (Spring Data JPA)
- **wardrobe-service** - Сервис управления предметами гардероба (Reactor + Spring Data R2DBC)
- **outfit-service** - Сервис управления образами (Spring Data JPA)

| Сервис | Технологии | Доступ к БД | Межсервисное взаимодействие |
|--------|------------|--------------|------------------------------|
| **user-service** | Spring MVC + JPA | Императивный (JDBC) | OpenFeign → другие сервисы |
| **wardrobe-service** | Spring WebFlux + R2DBC | Реактивный | WebClient → user-service |
| **outfit-service** | Spring MVC + JPA | Императивный (JDBC) | OpenFeign → user-service, wardrobe-service |
| **api-gateway** | Spring WebFlux Gateway | - | Маршрутизация всех запросов |
| **config-server** | Spring Cloud Config | - | Централизованная конфигурация |
| **discovery-service** | Netflix Eureka | - | Регистрация сервисов |

### **Ключевые особенности:**
- **Database-per-Service**: Каждая БД изолирована
- **Circuit Breaker**: Защита от каскадных сбоев между сервисами
- **Reactive Programming**: Wardrobe Service использует WebFlux + R2DBC
- **Миграции БД**: **Flyway** для версионирования схем (заменили Liquibase)
- **Тестирование**: Unit + Integration тесты с Testcontainers
- **Документация**: OpenAPI/Swagger для всех сервисов


### **Кратко о проекте:**

**Архитектурные особенности:**
- **Гибридная архитектура**: MVC сервисы (user, outfit) + Reactive сервис (wardrobe)
- **Database-per-Service**: Каждая БД изолирована (users_db, wardrobe_db, outfits_db)
- **Service Discovery**: Авторегистрация в Eureka с балансировкой нагрузки
- **Config Management**: Централизованная конфигурация через Git (config-repo/)
- **Circuit Breaker**: Защита от каскадных сбоев между сервисами

**Технологии по сервисам:**
- **user-service & outfit-service**: Spring MVC + JPA + PostgreSQL + Feign + Flyway
- **wardrobe-service**: Spring WebFlux + R2DBC + PostgreSQL + WebClient + Flyway (JDBC)
- **api-gateway**: Spring Cloud Gateway (WebFlux)
- **config-server & discovery-service**: Spring Boot стандартные реализации

**Миграции БД:**
- **Flyway вместо Liquibase**: Более простая и надежная система миграций
- **Wardrobe Service**: Flyway с JDBC (для миграций) + R2DBC (для runtime)
- **Автоматический запуск**: Миграции применяются при старте каждого сервиса

**Тестирование:**
- **Unit тесты**: Mockito для моков зависимостей
- **Integration тесты**: Testcontainers для реальных БД
- **Coverage**: Jacoco с порогом 70%
- **WebMVC/WebFlux тесты**: @WebMvcTest / @WebFluxTest соответственно

## Предметная область

Система Wardrobe Manager предназначена для ведения онлайн-гардероба и подбора образов.
Пользователь может:
- добавлять вещи в гардероб (например: куртка, платье, сумка),
- хранить информацию о характеристиках предметов (тип, бренд, цвет, сезон, фото),
- создавать готовые образы (outfits) из нескольких вещей,
- управлять своим гардеробом через REST API (CRUD-операции).

## Задача

Реализация микросервисной архитектуры Wardrobe Manager на Spring Boot с использованием Spring Cloud Netflix.
Монолитное приложение было декомпозировано на независимые микросервисы с централизованным сервисом обнаружения (Eureka).

**Архитектура**:

- **Service Discovery**: Eureka Server для регистрации и обнаружения сервисов
- **Config Server**: Централизованное управление конфигурацией через Git
- **User Service**: управление пользователями
- **Wardrobe Service**: управление предметами гардероба
- **Outfit Service**: управление образами
- **API Gateway**: единая точка входа для всех микросервисов
- **Межсервисное общение**: OpenFeign HTTP-клиенты
- **Базы данных**: Отдельные PostgreSQL БД для каждого микросервиса с Flyway миграциями
- **Конфигурации**: Отдельный Git репозиторий для управления конфигурациями

## Конфигурационный репозиторий

Все конфигурации микросервисов хранятся в отдельном репозитории `config-repo/` и загружаются через **Config Server**:

```
config-repo/
├── README.md              # Документация по конфигурациям
├── application.yml        # Глобальные настройки
├── user-service.yml       # Настройки User Service (порт, БД, Circuit Breaker)
├── wardrobe-service.yml   # Настройки Wardrobe Service (порт, БД, Circuit Breaker)
├── outfit-service.yml     # Настройки Outfit Service (порт, БД, Circuit Breaker)
└── api-gateway.yml        # Настройки API Gateway
```

**Как микросервисы подтягивают конфигурацию:**

Каждый микросервис использует **Config Client** для получения конфигурации из Config Server:

```yaml
# В application.yml каждого сервиса:
spring:
  application:
    name: user-service  # Имя сервиса определяет, какой файл загружать
  config:
    import: optional:configserver:${CONFIG_SERVER_URI:http://config-server:8888}
```

Config Server загружает конфигурацию из `config-repo/{application-name}.yml` (например, `user-service.yml` для User Service).

**Что вынесено в remote config:**
- Порты сервисов (`SERVER_PORT`)
- Параметры подключения к БД (URL, username, password)
- Конфигурация Circuit Breaker (Resilience4j)
- Eureka настройки
- Management endpoints

**Преимущества:**
- Централизованное управление конфигурациями
- Возможность изменять конфигурацию без пересборки приложения
- Разные конфигурации для разных сред (dev/staging/prod)
- Автоматическое обновление через `/actuator/refresh` endpoint


## Микросервисы

### 1. Config Server 
- **Порт**: 8888
- **Назначение**: Централизованное управление конфигурацией
- **Технологии**: Spring Cloud Config Server + Git
- **URL**: http://localhost:8888
- **Конфигурации**: Хранятся в локальном git репозитории `config-repo/`
- **Регистрация**: Зарегистрирован в Eureka
- **Как работает**: 
  - Микросервисы подключаются через `spring.config.import: optional:configserver:http://config-server:8888`
  - Config Server загружает конфигурацию из `config-repo/{service-name}.yml`
  - Конфигурация загружается при старте сервиса

### 2. API Gateway (Spring Cloud Gateway)
- **Порт**: 8080
- **Назначение**: Единая точка входа для всех микросервисов
- **Функции**: Маршрутизация, балансировка нагрузки, фильтры, Swagger агрегация
- **URL**: http://localhost:8080
- **Главная страница**: http://localhost:8080 (выбор сервиса)
- **Маршруты**:
  - `/users/**` → User Service
  - `/items/**` → Wardrobe Service
  - `/outfits/**` → Outfit Service
  - `/swagger-ui/**` → Единая Swagger страница
  - `/eureka/**` → Eureka Dashboard

### 3. Discovery Service (Eureka)
- **Порт**: 8761
- **Назначение**: Централизованный сервис обнаружения
- **Технологии**: Spring Cloud Netflix Eureka Server
- **URL**: http://localhost:8761

### 5. User Service
- **Внутренний порт**: 8081 (доступен через Gateway)
- **Назначение**: Управление пользователями
- **Технологии**: Spring Data JPA (императивный подход)
- **API** (через Gateway http://localhost:8080):
  - `GET /users/{id}` - получить пользователя
  - `POST /users` - создать пользователя
  - `PUT /users/{id}` - обновить пользователя
  - `DELETE /users/{id}` - удалить пользователя

### 6. Wardrobe Service ⚡
- **Внутренний порт**: 8082 (доступен через Gateway)
- **Назначение**: Управление предметами гардероба
- **Технологии**: **Reactor + Spring Data R2DBC** (реактивный подход)
  - Spring WebFlux для реактивных HTTP запросов
  - R2DBC для реактивного доступа к базе данных
  - Reactor (Mono/Flux) для обработки асинхронных потоков
- **API** (через Gateway http://localhost:8080):
  - `GET /items/paged` - получить предметы с пагинацией
  - `GET /items/scroll` - получить предметы (бесконечная прокрутка)
  - `GET /items/{id}` - получить предмет по ID
  - `POST /items` - создать предмет
  - `PUT /items/{id}` - обновить предмет
  - `DELETE /items/{id}` - удалить предмет

### 7. Outfit Service
- **Внутренний порт**: 8083 (доступен через Gateway)
- **Назначение**: Управление образами
- **Технологии**: Spring Data JPA (императивный подход)
- **API** (через Gateway http://localhost:8080):
  - `GET /outfits/{id}` - получить образ по ID
  - `GET /outfits/paged` - получить образы с пагинацией
  - `GET /outfits/scroll` - получить образы (бесконечная прокрутка)
  - `POST /outfits` - создать образ
  - `PUT /outfits/{id}` - обновить образ
  - `DELETE /outfits/{id}` - удалить образ

## Структура проекта

```
microservices/
├─ config-server/               # Spring Cloud Config Server
│  ├─ src/main/java/com/example/configserver/
│  │  └─ ConfigServerApplication.java
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  └─ config/               # Конфигурационные файлы для всех сервисов
│  ├─ Dockerfile
│  └─ pom.xml
├─ api-gateway/                 # Spring Cloud Gateway
│  ├─ src/main/java/com/example/gateway/
│  │  └─ ApiGatewayApplication.java
│  ├─ src/main/resources/
│  │  ├─ static/index.html     # Главная страница с кнопками сервисов
│  │  ├─ application.yml
│  │  └─ bootstrap.yml
│  ├─ Dockerfile
│  └─ pom.xml
├─ discovery-service/          # Eureka Server
│  ├─ src/main/java/com/example/discovery/
│  │  └─ DiscoveryServiceApplication.java
│  ├─ src/main/resources/application.yml
│  ├─ Dockerfile
│  └─ pom.xml
├─ user-service/               # Сервис пользователей
│  ├─ src/main/java/com/example/userservice/
│  │  ├─ controller/
│  │  ├─ dto/
│  │  ├─ entity/
│  │  ├─ exception/
│  │  ├─ mapper/
│  │  ├─ repository/
│  │  └─ service/
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  └─ db/changelog/
│  ├─ Dockerfile
│  └─ pom.xml
├─ wardrobe-service/           # Сервис предметов гардероба
│  ├─ src/main/java/com/example/wardrobeservice/
│  │  ├─ client/              # Feign клиенты
 │  │  ├─ controller/
 │  │  ├─ dto/
│  │  ├─ entity/
│  │  ├─ exception/
│  │  ├─ mapper/
│  │  ├─ repository/
 │  │  └─ service/
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  └─ db/changelog/
│  ├─ Dockerfile
│  └─ pom.xml
└─ outfit-service/             # Сервис образов
   ├─ src/main/java/com/example/outfitservice/
   │  ├─ client/              # Feign клиенты
   │  ├─ controller/
   │  ├─ dto/
   │  ├─ entity/
   │  ├─ exception/
   │  ├─ mapper/
   │  ├─ repository/
   │  └─ service/
   ├─ src/main/resources/
   │  ├─ application.yml
   │  └─ db/changelog/
   ├─ Dockerfile
   └─ pom.xml
```

## Быстрый старт

### Запуск через Docker Compose
```bash
# Сборка и запуск всех сервисов
docker-compose up --build -d

# Проверка статуса
docker-compose ps

# Просмотр логов конкретного сервиса
docker-compose logs -f user-service
docker-compose logs -f wardrobe-service
docker-compose logs -f outfit-service
docker-compose logs -f discovery-service

# Остановка
docker-compose down
```

### Сервисы будут доступны по адресам:
- **API Gateway** (главная страница с выбором сервиса): http://localhost:8080
- **Единая Swagger страница**: http://localhost:8080/swagger-ui/index.html
- **Config Server**: http://localhost:8888
- **Eureka Dashboard**: http://localhost:8761
- **User Service** (внутренний): http://localhost:8081
- **Wardrobe Service** (внутренний): http://localhost:8082
- **Outfit Service** (внутренний): http://localhost:8083

### Проверка работоспособности

```bash
# 1) Логин под seed-пользователями (пароль везде: 'password')
# supervisor@example.com -> ROLE_SUPERVISOR
# moderator@example.com  -> ROLE_MODERATOR
SUP_TOKEN=$(curl -s -X POST http://localhost:8080/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"supervisor@example.com","password":"password"}' | jq -r '.access_token')

MOD_TOKEN=$(curl -s -X POST http://localhost:8080/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"moderator@example.com","password":"password"}' | jq -r '.access_token')

# 2) Создать обычного пользователя может только SUPERVISOR (MODERATOR должен получить 403)
curl -i -X POST http://localhost:8080/users/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SUP_TOKEN" \
  -d '{"email":"u1@example.com","name":"U1","password":"password","role":"ROLE_USER"}'

curl -i -X POST http://localhost:8080/users/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MOD_TOKEN" \
  -d '{"email":"u2@example.com","name":"U2","password":"password","role":"ROLE_USER"}'

# 3) Проверка прав на вещи (wardrobe-service): MODERATOR не может создавать вещи за другого пользователя
curl -i -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MOD_TOKEN" \
  -d '{"ownerId":999,"type":"SHIRT","brand":"Nike","color":"Blue","season":"SUMMER","imageUrl":"test.jpg"}'

# 4) Проверка прав на аутфиты (outfit-service): MODERATOR может создавать аутфиты для другого пользователя
curl -i -X POST http://localhost:8080/outfits \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MOD_TOKEN" \
  -d '{"title":"Moderator Outfit","user_id":1,"items":[]}'

# Проверить статус Circuit Breaker
curl http://localhost:8082/actuator/health | jq '.components.circuitBreakers'
```

## Роли и матрица прав

В системе используется claim `roles` в JWT (например: `ROLE_USER`, `ROLE_SUPERVISOR`, `ROLE_MODERATOR`).

- **ROLE_USER**: управляет только своими вещами и своими аутфитами.
- **ROLE_MODERATOR**: не может регистрировать пользователей и не может менять вещи других пользователей, но может создавать/изменять/удалять аутфиты пользователей.
- **ROLE_SUPERVISOR**: расширенные права (в т.ч. регистрация пользователей), доступ к данным пользователей шире обычного пользователя.

## Проверка ролей через Swagger (3 роли)

Ниже — инструкция проверки различий между **ROLE_USER**, **ROLE_MODERATOR**, **ROLE_SUPERVISOR** через Swagger UI (агрегированная страница на Gateway).

### Кратко: кому что доступно

- **Регистрация пользователей** (`POST /users/auth/register`)
  - **ROLE_SUPERVISOR**: ✅ можно
  - **ROLE_MODERATOR**: ❌ нельзя (403)
  - **ROLE_USER**: ❌ нельзя (403)

- **Вещи** (`/items`, Wardrobe Service)
  - **ROLE_USER**: ✅ CRUD только **свои** вещи (owner_id == user_id), ❌ чужие (403)
  - **ROLE_MODERATOR**: ✅ CRUD только **свои** вещи, ❌ чужие (403)
  - **ROLE_SUPERVISOR**: ✅ может работать с **любыми** вещами (любой owner_id)

- **Аутфиты** (`/outfits`, Outfit Service)
  - **ROLE_USER**: ✅ CRUD только **свои** аутфиты (user_id == user_id), ❌ чужие (403)
  - **ROLE_MODERATOR**: ✅ может CRUD **любые** аутфиты (в т.ч. чужие)
  - **ROLE_SUPERVISOR**: ✅ может CRUD **любые** аутфиты (в т.ч. чужие)

### 0) Подготовка: получить 3 токена и 3 userId

Перейди в Swagger UI на Gateway: `http://localhost:8080/swagger-ui/index.html`

#### 0.1 Логин супервайзера
User Service → **POST** `/users/auth/login` (Try it out)
Body:
`{ "email": "supervisor@example.com", "password": "password" }`
Execute → скопируй `access_token` → **SUP_TOKEN**.

#### 0.2 Авторизоваться супервайзером
Нажми **Authorize** → вставь:
`Bearer <SUP_TOKEN>`
Authorize → Close.

#### 0.3 Узнать SUP_ID
User Service → **GET** `/users/auth/me` → Execute
Запомни `user_id` → **SUP_ID**.

#### 0.4 Логин модератора
User Service → **POST** `/users/auth/login`
Body:
`{ "email": "moderator@example.com", "password": "password" }`
Execute → скопируй `access_token` → **MOD_TOKEN**.

#### 0.5 Авторизоваться модератором
Authorize → вставь:
`Bearer <MOD_TOKEN>`
Authorize → Close.

#### 0.6 Узнать MOD_ID
User Service → **GET** `/users/auth/me` → Execute
Запомни `user_id` → **MOD_ID**.

#### 0.7 Создать обычного пользователя (если ещё нет)
Authorize должен стоять на **SUP_TOKEN**.
User Service → **POST** `/users/auth/register`
Body:
`{ "email": "u-check@example.com", "name": "User Check", "password": "password", "role": "ROLE_USER" }`
Execute → ожидай **201**.

*(доп. проверка)* Попробуй то же самое с **MOD_TOKEN** → ожидай **403**.

#### 0.8 Логин обычного пользователя
User Service → **POST** `/users/auth/login`
Body:
`{ "email": "u-check@example.com", "password": "password" }`
Execute → скопируй `access_token` → **USER_TOKEN**.

#### 0.9 Авторизоваться обычным пользователем
Authorize → вставь:
`Bearer <USER_TOKEN>`
Authorize → Close.

#### 0.10 Узнать USER_ID
User Service → **GET** `/users/auth/me` → Execute
Запомни `user_id` → **USER_ID**.

---

### 1) Проверка Wardrobe `/items`

Переключись вверху Swagger на **Wardrobe Service**.

#### 1.1 ROLE_USER: создать вещь себе → 201
Authorize = **USER_TOKEN**.
**POST** `/items`
Body:
`{ "type":"SHIRT","brand":"Nike","color":"Blue","season":"SUMMER","image_url":"img.jpg","owner_id": <USER_ID> }`
Execute → ожидай **201**.
Запомни `id` → **ITEM_ID_USER**.

#### 1.2 ROLE_USER: попытка создать вещь “чужому” → 403
Authorize = **USER_TOKEN**.
**POST** `/items`
Body:
`{ "type":"SHIRT","brand":"Nike","color":"Blue","season":"SUMMER","image_url":"img.jpg","owner_id": <SUP_ID> }`
Execute → ожидай **403**.

#### 1.3 ROLE_MODERATOR: создать вещь себе → 201
Authorize = **MOD_TOKEN**.
**POST** `/items`
Body:
`{ "type":"HAT","brand":"Puma","color":"Gray","season":"SUMMER","image_url":"img3.jpg","owner_id": <MOD_ID> }`
Execute → ожидай **201**.
Запомни `id` → **ITEM_ID_MOD**.

#### 1.4 ROLE_MODERATOR: попытка создать вещь “чужому” → 403
Authorize = **MOD_TOKEN**.
**POST** `/items`
Body:
`{ "type":"HAT","brand":"Puma","color":"Gray","season":"SUMMER","image_url":"img3.jpg","owner_id": <USER_ID> }`
Execute → ожидай **403**.

#### 1.5 ROLE_SUPERVISOR: создать вещь любому owner_id → 201
Authorize = **SUP_TOKEN**.
**POST** `/items`
Body:
`{ "type":"JACKET","brand":"Adidas","color":"Black","season":"WINTER","image_url":"img2.jpg","owner_id": <SUP_ID> }`
Execute → ожидай **201**.
Запомни `id` → **ITEM_ID_SUP**.

#### 1.6 ROLE_USER: попытка получить чужую вещь → 403
Authorize = **USER_TOKEN**.
**GET** `/items/{id}` → подставь **ITEM_ID_SUP** → Execute
Ожидай **403**.

#### 1.7 ROLE_MODERATOR: попытка получить чужую вещь → 403
Authorize = **MOD_TOKEN**.
**GET** `/items/{id}` → подставь **ITEM_ID_USER** → Execute
Ожидай **403**.

#### 1.8 ROLE_SUPERVISOR: получить любую вещь → 200
Authorize = **SUP_TOKEN**.
**GET** `/items/{id}` → подставь **ITEM_ID_USER** (или **ITEM_ID_MOD**) → Execute
Ожидай **200**.

---

### 2) Проверка Outfit `/outfits`

Переключись вверху Swagger на **Outfit Service**.

#### 2.1 ROLE_USER: создать outfit себе → 201
Authorize = **USER_TOKEN**.
**POST** `/outfits`
Body:
`{ "title":"My outfit","user_id": <USER_ID>, "items":[] }`
Execute → **201**.
Запомни `id` → **OUTFIT_ID_USER**.

#### 2.2 ROLE_USER: попытка создать outfit “чужому” → 403
Authorize = **USER_TOKEN**.
**POST** `/outfits`
Body:
`{ "title":"Foreign outfit","user_id": <SUP_ID>, "items":[] }`
Execute → **403**.

#### 2.3 ROLE_MODERATOR: создать outfit любому → 201
Authorize = **MOD_TOKEN**.
**POST** `/outfits`
Body:
`{ "title":"Moderator outfit","user_id": <USER_ID>, "items":[] }`
Execute → **201**.
Запомни `id` → **OUTFIT_ID_MOD**.

#### 2.4 ROLE_SUPERVISOR: создать outfit любому → 201
Authorize = **SUP_TOKEN**.
**POST** `/outfits`
Body:
`{ "title":"Supervisor outfit","user_id": <SUP_ID>, "items":[] }`
Execute → **201**.
Запомни `id` → **OUTFIT_ID_SUP**.

#### 2.5 ROLE_USER: попытка получить чужой outfit → 403
Authorize = **USER_TOKEN**.
**GET** `/outfits/{id}` → подставь **OUTFIT_ID_SUP** → Execute
Ожидай **403**.

#### 2.6 ROLE_MODERATOR: получить чужой outfit → 200
Authorize = **MOD_TOKEN**.
**GET** `/outfits/{id}` → подставь **OUTFIT_ID_USER** (или **OUTFIT_ID_SUP**) → Execute
Ожидай **200**.

#### 2.7 ROLE_SUPERVISOR: получить любой outfit → 200
Authorize = **SUP_TOKEN**.
**GET** `/outfits/{id}` → подставь **OUTFIT_ID_USER** (или **OUTFIT_ID_MOD**) → Execute
Ожидай **200**.

## Архитектура базы данных

**Database-per-Service Pattern**: Каждый микросервис использует свою собственную базу данных для обеспечения полной изоляции данных.

**Базы данных и таблицы**:

### User Service (`users_db`)
- `users` - пользователи системы

### Wardrobe Service (`wardrobe_db`)
- `wardrobe_items` - предметы гардероба пользователей

### Outfit Service (`outfits_db`)
- `outfits` - готовые образы пользователей
- `outfit_items` - связи между образами и предметами гардероба

<img width="555" height="460" alt="image" src="https://github.com/user-attachments/assets/95cd5c46-411c-4169-a122-23a3694dc94b" />

**Таблицы**:
- `users` - пользователи
- `wardrobe_items` - предметы гардероба
- `outfits` - образы
- `outfit_items` - связи между образами и предметами

## Особенности реализации

### Технологические стеки микросервисов

Проект демонстрирует использование разных подходов к реализации микросервисов:

#### User Service - Spring Data JPA (Императивный подход)
- **Технологии**: Spring MVC + Spring Data JPA + PostgreSQL JDBC
- **Подход**: Императивный (блокирующий)
- **Особенности**:
  - Синхронные операции
  - Блокирующие вызовы к БД
  - Простая и понятная модель программирования
  - Хорошо подходит для простых CRUD операций

#### Wardrobe Service - Reactor + R2DBC (Реактивный подход) ⚡
- **Технологии**: Spring WebFlux + Spring Data R2DBC + Reactor (Mono/Flux)
- **Подход**: Реактивный (неблокирующий)
- **Особенности**:
  - Асинхронные, неблокирующие операции
  - Высокая пропускная способность при большом количестве одновременных запросов
  - Эффективное использование ресурсов (меньше потоков)
  - Использует ReactiveCrudRepository для работы с БД
  - Все методы возвращают Mono (для одного элемента) или Flux (для потока элементов)

**Пример реактивного кода:**
```java
// Реактивный сервис
public Mono<WardrobeItemDto> getById(Long id) {
    return itemRepository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Item not found")))
        .map(itemMapper::toDto);
}

// Реактивный контроллер
@GetMapping("/{id}")
public Mono<ResponseEntity<WardrobeItemDto>> getById(@PathVariable Long id) {
    return itemService.getById(id)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

**Преимущества реактивного подхода:**
- Масштабируемость: обрабатывает больше запросов на том же оборудовании
- Отзывчивость: неблокирующие операции не блокируют потоки
- Backpressure: автоматическое управление нагрузкой через реактивные потоки

#### Outfit Service - Spring Data JPA (Императивный подход)
- **Технологии**: Spring MVC + Spring Data JPA + PostgreSQL JDBC
- **Подход**: Императивный (блокирующий)
- Аналогично User Service

### Межсервисное взаимодействие

**OpenFeign Clients + Circuit Breaker** используются для отказоустойчивого взаимодействия:

- **Wardrobe Service** → **User Service**: Проверка существования пользователя с Circuit Breaker защитой
- **Outfit Service** → **User Service**: Проверка существования пользователя с Circuit Breaker защитой
- **Outfit Service** → **Wardrobe Service**: Проверка существования предметов с Circuit Breaker защитой

**Примеры использования Feign клиентов с Circuit Breaker:**

```java
// WardrobeService вызывает User Service для валидации
@CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
UserDto getUserById(@PathVariable Long id);

default UserDto getUserByIdFallback(Long id, Throwable throwable) {
    return null; // Fallback: service unavailable
}
```

**Конфигурация Circuit Breaker (Resilience4j):**
- **Failure Rate Threshold**: 50% - открытие при превышении
- **Sliding Window**: 10 вызовов для расчета
- **Wait Duration**: 5 секунд в OPEN состоянии
- **Minimum Calls**: 5 вызовов перед активацией
- **Timeout**: 2 секунды на вызов

**Состояния Circuit Breaker:**
- **CLOSED**: Нормальная работа
- **OPEN**: Защита от дальнейших вызовов
- **HALF_OPEN**: Тестирование восстановления

### Архитектурные компоненты

- **API Gateway**: Единая точка входа через Spring Cloud Gateway
- **Service Discovery**: Автоматическая регистрация сервисов в Eureka
- **Circuit Breaker**: Resilience4j для защиты от каскадных сбоев
- **Валидация**: Bean Validation на всех уровнях
- **Пагинация**: Поддержка постраничной навигации и бесконечной прокрутки
- **Транзакции**: Гарантия консистентности данных
- **Обработка ошибок**: Централизованный Exception Handler
- **Миграции БД**:
  - Все сервисы: **Flyway SQL migrations** 
  - Wardrobe Service: Flyway с JDBC для миграций + R2DBC для runtime
- **Мониторинг**: Actuator endpoints для Circuit Breaker состояния

### Сравнение технологических стеков

| Аспект | User/Outfit Service (JPA) | Wardrobe Service (R2DBC) |
|--------|---------------------------|--------------------------|
| **Подход** | Императивный (блокирующий) | Реактивный (неблокирующий) |
| **Web Framework** | Spring MVC | Spring WebFlux |
| **Database Access** | Spring Data JPA (JDBC) | Spring Data R2DBC |
| **Типы данных** | Обычные объекты, List | Mono, Flux |
| **Потоки** | Один поток на запрос | Общий пул потоков (event loop) |
| **Пропускная способность** | Ограничена количеством потоков | Выше за счет неблокирующих операций |
| **Сложность** | Проще для понимания | Требует знания реактивного программирования |
| **Использование** | Простые CRUD операции | Высоконагруженные сервисы с большим количеством одновременных запросов |

### Мониторинг Circuit Breaker

**Health Check endpoints:**
```
GET /actuator/health
GET /actuator/circuitbreakers
```

**Circuit Breaker метрики:**
- Текущее состояние (CLOSED/OPEN/HALF_OPEN)
- Количество успешных/неудачных вызовов
- Rate лимиты и таймауты

### База данных и миграции

**Автоматическое создание схемы:**
- Миграции запускаются автоматически при старте каждого сервиса
- Используется **Flyway** с SQL файлами в `db/migration/`
- Поддерживает версионирование и rollback

**Миграционные файлы:**
```
# User Service (users_db):
V1__create_users_table.sql

# Wardrobe Service (wardrobe_db):
V1__create_wardrobe_items_table.sql

# Outfit Service (outfits_db):
V1__create_outfits_table.sql
V2__create_outfit_items_table.sql
V3__create_indexes.sql
```

**Проверка миграций:**
```bash
# Проверить статус миграций для каждого сервиса
docker-compose exec user-service ./mvnw flyway:info
docker-compose exec wardrobe-service ./mvnw flyway:info
docker-compose exec outfit-service ./mvnw flyway:info

# Запустить миграции вручную
docker-compose exec user-service ./mvnw flyway:migrate
```



