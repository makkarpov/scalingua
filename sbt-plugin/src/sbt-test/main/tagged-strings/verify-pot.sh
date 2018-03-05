#!/bin/bash

if [ ! -f "target/scala-2.10/messages/test.pot" ]; then
    echo "test.pot does not exists!" >&2
    exit 1
fi

tail -n +2 "target/scala-2.10/messages/test.pot" > generated.pot
diff -u generated.pot messages.pot || exit 1