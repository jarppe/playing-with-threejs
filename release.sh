#!/usr/bin/env zsh

set -e

message=${1:-Another magnificent release}

function die {
  echo "error" $1
  exit 1
}

#git diff-index --quiet HEAD -- || die "git not clean"

echo "releasing" $message

rm -fr ./docs/*
cp -r ./resources/public/index.html ./resources/public/css ./docs/
lein dist
git add ./docs
git commit -m "$message"
git push

echo "Done"
