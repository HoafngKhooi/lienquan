# Build APK

## Cách 1 — PC (nhanh nhất)
1. Cài Android Studio
2. Copy project sang PC
3. Gradle sync → Build > Build APK
4. APK: `app/build/outputs/apk/debug/app-debug.apk`

## Cách 2 — GitHub Actions (không cần PC)
1. Push code lên GitHub repo
2. Workflow `.github/workflows/build-apk.yml` tự chạy
3. Tab Actions → run mới nhất → tải artifact `app-debug`

## Cách 3 — Termux (tự build trên máy)
```bash
bash scripts/build-apk.sh
```

Lần đầu ~15-20p setup SDK (~2.5GB), sau đó 3-5p/lần.

Test engine (không cần build APK)

```bash
bash scripts/build-engine-test.sh
```

