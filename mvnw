#!/usr/bin/env sh
set -e

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MAVEN_VERSION="3.9.11"
MAVEN_DIR="$BASE_DIR/.mvn/wrapper/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_DIR/bin/mvn"
MAVEN_ZIP="$BASE_DIR/.mvn/wrapper/apache-maven-$MAVEN_VERSION-bin.zip"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip"

if [ ! -x "$MAVEN_BIN" ]; then
  mkdir -p "$BASE_DIR/.mvn/wrapper"
  echo "Baixando Apache Maven $MAVEN_VERSION para o wrapper local..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$MAVEN_URL" -o "$MAVEN_ZIP"
  else
    wget -q "$MAVEN_URL" -O "$MAVEN_ZIP"
  fi
  unzip -q -o "$MAVEN_ZIP" -d "$BASE_DIR/.mvn/wrapper"
fi

exec "$MAVEN_BIN" "$@"
