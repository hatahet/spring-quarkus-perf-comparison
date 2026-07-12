#!/usr/bin/env bash
set -euo pipefail

version=${1:?Go version is required}
destination=${2:?destination is required}
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "invalid Go version: $version" >&2; exit 1; }

if [[ -x "$destination/bin/go" ]] && [[ "$(GOTOOLCHAIN=local "$destination/bin/go" version)" == "go version go${version} "* ]]; then
  exit 0
fi

case "$(uname -s)" in Linux) os=linux ;; Darwin) os=darwin ;; *) echo "unsupported OS" >&2; exit 1 ;; esac
case "$(uname -m)" in x86_64|amd64) arch=amd64 ;; aarch64|arm64) arch=arm64 ;; *) echo "unsupported architecture" >&2; exit 1 ;; esac

archive="go${version}.${os}-${arch}.tar.gz"
metadata=$(curl -fsSL "https://go.dev/dl/?mode=json&include=all")
checksum=$(printf '%s' "$metadata" | jq -r --arg v "go${version}" --arg f "$archive" '.[] | select(.version == $v) | .files[] | select(.filename == $f) | .sha256')
[[ "$checksum" =~ ^[0-9a-f]{64}$ ]] || { echo "official checksum not found for $archive" >&2; exit 1; }
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
curl -fsSL "https://go.dev/dl/${archive}" -o "$tmp/$archive"
if command -v sha256sum >/dev/null 2>&1; then
  printf '%s  %s\n' "$checksum" "$tmp/$archive" | sha256sum -c -
else
  [[ "$(shasum -a 256 "$tmp/$archive" | awk '{print $1}')" == "$checksum" ]] || { echo "checksum verification failed" >&2; exit 1; }
fi
rm -rf "$destination"
mkdir -p "$destination"
tar -C "$destination" --strip-components=1 -xzf "$tmp/$archive"
