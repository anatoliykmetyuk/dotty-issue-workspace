#!/usr/bin/env bash
set -e

# Release command:
RELEASE_CMD="${1:?Missing publish command}"

# Make sure required environment variable are set
: "${SONATYPE_USER:?not set}"
: "${SONATYPE_PW:?not set}"
: "${PGP_PASSPHRASE:?not set}"
: "${PGP_SECRET:?is not set}"

# Setup bouncycastle instead of gpg to do signing, because gpg explodes when
# doing too many signing requests in parallel (https://github.com/sbt/sbt-pgp/issues/168)
mkdir -p "$HOME/.sbt/gpg"
echo "$PGP_SECRET" > "$HOME/.sbt/gpg/secring.asc"

# run sbt with the supplied arg
sbt "$RELEASE_CMD"
