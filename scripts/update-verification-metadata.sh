#!/usr/bin/env bash
#
# Пересобирает снимок контрольных сумм зависимостей (gradle/verification-metadata.xml).
#
# Запускать осознанно, а не «чтобы сборка перестала падать». Снимок фиксирует
# конкретный набор версий: после обновления дифф обязан быть прочитан глазами —
# новые координаты в нём означают, что в сборку приехало что-то, чего там
# раньше не было.
#
# Почему с нуля, а не «дописать недостающее»: `--write-verification-metadata`
# только добавляет записи и никогда не удаляет. Перегенерация этого проекта
# убрала 31 мёртвую запись — версии 2021 года (androidx.core:core:1.1.0,
# androidx.compose.ui:ui:1.0.1, lifecycle-*:2.3.0), которые сборка уже не
# резолвит. Накопленный мусор делает дифф нечитаемым, а нечитаемый дифф
# означает, что проверка превратилась в ритуал.
#
# Почему только ./gradlew: снимок, снятый другим Gradle, описывает другую
# сборку — другой дистрибутив, другие версии поставляемых им артефактов.

set -euo pipefail

cd "$(dirname "$0")/.."

METADATA="gradle/verification-metadata.xml"

# Задачи выбраны так, чтобы резолвились все конфигурации, которые трогают
# сборка и IDE: main/test/androidTest всех вариантов, lint, скриншот-тесты и
# конвенционный плагин графа модулей.
TASKS=(
  help
  assembleDebug
  assembleRelease
  assembleAndroidTest
  testDebugUnitTest
  lintDebug
  verifyRoborazziDebug
  checkModuleDependencies
)

if [ ! -x "./gradlew" ]; then
  echo "Нет ./gradlew — запускай из корня проекта." >&2
  exit 1
fi

CONFIG_BLOCK="$(mktemp)"
BACKUP="$(mktemp)"
SNAPSHOT_WRITTEN=""

# Генерация удаляет файл. Если что-то пойдёт не так, вернуть прежний снимок —
# иначе останется либо пустота, либо обрывок.
cleanup() {
  if [ -z "$SNAPSHOT_WRITTEN" ] && [ -s "$BACKUP" ]; then
    cp "$BACKUP" "$METADATA"
    echo "Генерация не удалась — вернул прежний снимок." >&2
  fi
  rm -f "$CONFIG_BLOCK" "$BACKUP"
}
trap cleanup EXIT

# Блок <configuration> (доверенные артефакты) — единственная часть файла,
# которую пишет человек. Генерация с нуля его теряет, поэтому сохраняем и
# возвращаем на место.
extract_config() {
  awk '/<configuration>/,/<\/configuration>/' "$1"
}

if [ -f "$METADATA" ]; then
  extract_config "$METADATA" > "$CONFIG_BLOCK"
fi

if [ ! -s "$CONFIG_BLOCK" ]; then
  echo "Не найден блок <configuration> в $METADATA." >&2
  echo "Восстанови файл из git (git checkout -- $METADATA) и повтори." >&2
  exit 1
fi

BEFORE=$(grep -c "<component " "$METADATA" 2>/dev/null || echo 0)

echo "Пересобираю снимок с нуля: было $BEFORE компонентов"
cp "$METADATA" "$BACKUP"
rm -f "$METADATA"

# Падение отдельной задачи (упавший тест, ошибка lint) не должно отменять
# обновление снимка: цель — резолв конфигураций, а не зелёная сборка. Поэтому
# код возврата запоминаем, а снимок проверяем отдельно.
set +e
./gradlew \
  --refresh-dependencies \
  --no-configuration-cache \
  --write-verification-metadata sha256 \
  "${TASKS[@]}"
GRADLE_STATUS=$?
set -e

if [ ! -s "$METADATA" ]; then
  echo "Gradle не записал снимок (код возврата $GRADLE_STATUS)." >&2
  exit 1
fi

if [ "$GRADLE_STATUS" -ne 0 ]; then
  echo
  echo "ВНИМАНИЕ: сборка завершилась с кодом $GRADLE_STATUS." >&2
  echo "Снимок записан, но часть конфигураций могла не резолвиться — прочитай ошибки выше." >&2
fi

# Возвращаем блок доверенных артефактов поверх сгенерированного.
python3 - "$METADATA" "$CONFIG_BLOCK" <<'PY'
import io
import re
import sys

path, block_path = sys.argv[1], sys.argv[2]
block = io.open(block_path, encoding="utf-8").read().strip()
text = io.open(path, encoding="utf-8").read()

text, replacements = re.subn(
    r"<configuration>.*?</configuration>",
    lambda _match: block,
    text,
    count=1,
    flags=re.S,
)

if replacements != 1:
    sys.exit(f"Не удалось вернуть блок <configuration> в {path}: совпадений {replacements}")

io.open(path, "w", encoding="utf-8").write(text)
PY

SNAPSHOT_WRITTEN="yes"

AFTER=$(grep -c "<component " "$METADATA" || echo 0)
echo
echo "Компонентов: было $BEFORE, стало $AFTER"
git diff --stat -- "$METADATA"
echo
echo "Дальше: прочитай дифф. Ожидаемое — версии обновились или пропали."
echo "Незнакомые координаты — повод остановиться и разобраться, откуда они."
