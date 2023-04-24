# Copyright 2016 Waylay
#
# build the KairosDB image

VERSION=1.3.0-1

all: build

build:
	docker build -t brunoballekens/kairosdb-scala-driver-it:${VERSION} .

publish: build
	docker push brunoballekens/kairosdb-scala-driver-it:${VERSION}

.PHONY: all build push
