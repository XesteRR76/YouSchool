# Документация API платформы eSchool (eschool.center)

Спецификация получена в результате реверс-инжиниринга официального Android-клиента (`ru.spb.itstrategy.mobilejournal`), веб-интерфейса `app.eschool.center` и анализа открытых библиотек.

## Базовый URL
`https://app.eschool.center/ec-server`

---

## 1. Аутентификация

### 1.1. Вход в систему (Login)
* **Эндпоинт:** `POST /login`
* **Content-Type:** `application/x-www-form-urlencoded`
* **Параметры формы (Form Data):**
  * `username` (строка): логин пользователя (ученика или родителя).
  * `password` (строка): SHA-256 хэш исходного пароля в hex-формате (`MessageDigest.getInstance("SHA-256")`).
  * `device` (опционально, JSON-строка): метаданные устройства:
    ```json
    {
      "cliType": "android",
      "cliVer": "1.0.0",
      "pushToken": "",
      "deviceName": "Android Device",
      "deviceModel": "Pixel",
      "cliOs": "Android",
      "cliOsVer": "14"
    }
    ```
* **Заголовки ответа (Response Headers):**
  * `Set-Cookie`: 
    * `JSESSIONID=<session_id>; Path=/ec-server; Secure; HttpOnly`
    * `route=<route_id>; Path=/ec-server`
* **Коды ответа:**
  * `200 OK`: Успешная авторизация.
  * `401 / Response Data: 1`: Неверный логин или пароль.
  * `Response Data: 3`: Требуется капча.
  * `Response Data: 4`: Учетная запись заблокирована.

### 1.2. Сессионные куки для последующих запросов
Во всех последующих HTTP-запросах необходимо передавать заголовок:
`Cookie: JSESSIONID=<session_id>; route=<route_id>; site_ver=app`

Если сервер возвращает `401 Unauthorized`, клиент автоматически выполняет повторный вход с сохраненными хэшированными учетными данными и повторяет запрос.

---

## 2. Профиль и состояние пользователя

### 2.1. Получение текущего состояния
* **Эндпоинт:** `GET /state?menu=false`
* **Описание:** Возвращает информацию об авторизованном пользователе, его роли, персоне и учебном заведении.
* **Пример ответа:**
```json
{
  "userId": 123456,
  "authenticated": true,
  "profile": {
    "firstName": "Иван",
    "lastName": "Иванов",
    "middleName": "Иванович",
    "email": "ivan@example.com",
    "phoneMob": "+79991234567"
  },
  "user": {
    "userId": 123456,
    "prsId": 654321,
    "roles": ["ROLE_STUDENT"],
    "currentPosition": {
      "orgId": 100,
      "orgName": "ГБОУ СОШ №1",
      "orgFullName": "Государственное бюджетное общеобразовательное учреждение...",
      "orgYearId": 2026
    }
  }
}
```

### 2.2. Профиль подопечных (для родителей)
* **Эндпоинт:** `GET /profile/{userId}/children`
* **Описание:** Если `roles` пользователя `ROLE_PARENT`, этот запрос возвращает список детей с их персональными `userId` и `prsId`.

---

## 3. Учебные периоды

### 3.1. Периоды обучения (четверти / триместры / год)
* **Эндпоинт:** `GET /dict/periods2?year={academicYear}` (например, `2026`)
* **Описание:** Список учебных периодов.
* **Структура элементов `items`:**
  * `typeCode: "Y"` — Академический год (`date1`, `date2`, `id`, `name`).
  * `typeCode: "HY"` — Полугодие.
  * `typeCode: "Q"` — Четверть / триместр (`id`, `name`, `date1`, `date2`, `num`).

---

## 4. Расписание уроков и домашние задания

### 4.1. Дневник уроков за диапазон дат
* **Эндпоинт:** `GET /student/diary?userId={userId}&d1={timestampMsStart}&d2={timestampMsEnd}`
* **Описание:** Возвращает массив уроков за указанный период времени (в миллисекундах Unix epoch).
* **Пример ответа:**
```json
{
  "lesson": [
    {
      "id": 1092831,
      "date": "1731542400000",
      "numInDay": 1,
      "isODOD": 0,
      "unit": {
        "id": 42,
        "name": "Алгебра",
        "short": "Алгебра"
      },
      "tp": {
        "topicName": "Решение квадратных уравнений"
      },
      "teacher": {
        "factTeacherIN": "Петрова А.В."
      },
      "part": [
        {
          "cat": "DZ",
          "variant": [
            {
              "text": "Учебник стр. 84, №241, 243 (а, б)",
              "file": [
                {
                  "id": 84729,
                  "fileName": "tablica_formul.pdf"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

---

## 5. Оценки и успеваемость

### 5.1. Журнал оценок за учебный период
* **Эндпоинт:** `GET /student/getDiaryPeriod?userId={userId}&eiId={periodId}`
* **Описание:** Список всех выставленных оценок за выбранный период обучения (`eiId`).
* **Пример ответа:**
```json
{
  "result": [
    {
      "lessonId": 1092831,
      "unitId": 42,
      "subject": "Алгебра",
      "mktWt": 1,
      "markVal": "5",
      "markNum": 5,
      "markDate": "2026-11-14T09:45:00",
      "lptName": "Самостоятельная работа",
      "teachFio": "Петрова А.В."
    }
  ]
}
```

### 5.2. Сводка по предметам и текущий средний балл
* **Эндпоинт:** `GET /student/getDiaryUnits?userId={userId}&eiId={periodId}`
* **Описание:** Текущие сводные показатели по каждому предмету.
* **Пример ответа:**
```json
{
  "result": [
    {
      "unitId": 42,
      "unitName": "Алгебра",
      "overMark": 4.75,
      "rating": "1/28",
      "totalMark": "5"
    }
  ]
}
```

### 5.3. Итоговые годовые оценки
* **Эндпоинт:** `GET /student/getTotalMarks?yearid={yearId}&userid={userId}&json=true`
* **Описание:** Итоговая ведомость оценок за все четверти/полугодия и год.

---

## 6. Файлы и вложения

### 6.1. Скачивание прикрепленных файлов
* **Эндпоинт:** `GET /files/{fileId}`
* **Заголовки:** `Cookie: JSESSIONID=...; route=...`
* **Возвращает:** Бинарное содержимое файла (PDF, DOCX, JPG, PNG).
