#!/bin/bash
#
# (c) Copyright 2025 Swiss Post Ltd.
#
set -e

hash_filename="$1_$2_checksum.md"
hash_path="$3/reproducible-build"
version=$2

write_checksum() {
    local file="$1"
    local filename=$(basename "$file")
    local checksum=$(sha256sum "$file" | awk '{print $1}')
    echo "| **$filename** | $version | $checksum |" >> "$hash_path/$hash_filename"
}

process_voter_portal() {
    local file="$1"
    local unzippedFile=$(basename "$file" .zip)
    local targetPath="../voter-portal/target"
    local webappsPath="$targetPath/$unzippedFile/voter-portal"

    rm -rf "$targetPath/$unzippedFile"
    unzip -q "$file" -d "$targetPath/$unzippedFile"

    local files_to_hash=("crypto.ov-api.js" "crypto.ov-worker.js" "index.html" "main.js" "polyfills.js" "runtime.js")

    for target_file in "${files_to_hash[@]}"; do
        local file_path="$webappsPath/$target_file"

        if [[ -f "$file_path" ]]; then
            local sha256_checksum=$(sha256sum "$file_path" | awk '{print $1}')
            echo "| --> $target_file | $version | $sha256_checksum |" >> "$hash_path/$hash_filename"

            if [[ "$target_file" != "index.html" ]]; then
                local sha384_checksum=$(openssl dgst -sha384 -binary "$file_path" | base64)
                echo "| --> $target_file | $version | sha384-$sha384_checksum |" >> "$hash_path/$hash_filename"
            fi
        else
            echo "Warning: $target_file not found in $file_path" >&2
        fi
    done

    rm -rf "$targetPath/$unzippedFile"
}

mkdir -p "$hash_path"

cat > "$hash_path/$hash_filename" << 'EOF'
| Artefact | Version | Checksum |
|----------|---------|----------|
EOF

# Modules to process
modules=(
  control-component
  direct-trust-tool
  dispute-resolver
  file-cryptor
  secure-data-manager-packaging
  voter-portal
  voting-server
  xml-signature
)

# Loop through modules
for module in "${modules[@]}"; do
    target="../$module/target"
    [[ -d "$target" ]] || continue

    # Prefer JAR, but voter-portal and secure-data-manager produce ZIP
    if [[ "$module" == "voter-portal" || "$module" == "secure-data-manager-packaging" ]]; then
        ext="zip"
    else
        ext="jar"
    fi

    while IFS= read -r file; do
        write_checksum "$file"

        if [[ "$module" == "voter-portal" ]]; then
            process_voter_portal "$file"
        fi
    done < <(find "$target" -type f -name "*.${ext}" \
        ! -name "*-sources.${ext}" \
        ! -name "*-javadoc.${ext}")
done
