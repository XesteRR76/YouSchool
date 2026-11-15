#!/usr/bin/env python3
import os
import sys
import hashlib
import requests

def load_env(env_path=".env"):
    creds = {}
    if not os.path.exists(env_path):
        print(f"Файл {env_path} не найден! Создайте его с переменными ESCHOOL_LOGIN и ESCHOOL_PASSWORD.")
        return creds
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                creds[k.strip()] = v.strip().strip('"').strip("'")
    return creds

def test_login():
    creds = load_env()
    login = creds.get("ESCHOOL_LOGIN")
    password = creds.get("ESCHOOL_PASSWORD")

    if not login or not password:
        print("Ошибка: Укажите ESCHOOL_LOGIN и ESCHOOL_PASSWORD в файле .env")
        sys.exit(1)

    url = "https://app.eschool.center/ec-server/login"
    
    sha256_hash = hashlib.sha256(password.encode("utf-8")).hexdigest()
    md5_hash = hashlib.md5(password.encode("utf-8")).hexdigest()

    variations = [
        ("SHA-256 (lowercase)", sha256_hash, None),
        ("SHA-256 with device param", sha256_hash, '{"cliType":"android","cliVer":"1.0.0"}'),
        ("Plain text password", password, None),
        ("Plain text with device param", password, '{"cliType":"android","cliVer":"1.0.0"}'),
        ("MD5 hash", md5_hash, None)
    ]

    session = requests.Session()
    session.headers.update({
        "User-Agent": "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept": "*/*"
    })

    print(f"=== Тестирование авторизации для пользователя '{login}' ===")

    for label, pwd_val, device in variations:
        print(f"\n--- Пробуем вариант: {label} ---")
        data = {
            "username": login,
            "password": pwd_val
        }
        if device:
            data["device"] = device

        resp = session.post(url, data=data, timeout=10)
        print(f"HTTP Status: {resp.status_code}")
        print(f"Response Body: {resp.text.strip()}")
        print(f"Cookies: {dict(resp.cookies)}")
        for h, v in resp.headers.items():
            if "cookie" in h.lower() or "node" in h.lower():
                print(f"Header {h}: {v}")

        if resp.status_code == 200:
            print(f"\n>>> УСПЕХ! Вариант '{label}' сработал! <<<")
            
            # Проверим запрос /state
            state_resp = session.get("https://app.eschool.center/ec-server/state?menu=false")
            print(f"\nЗапрос /state: HTTP {state_resp.status_code}")
            print(f"State: {state_resp.text[:300]}...")
            return

if __name__ == "__main__":
    test_login()
