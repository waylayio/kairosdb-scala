# Copyright 2016 Waylay
#
# build the KairosDB image

#VERSION=1.1.3-1
VERSION=1.2.0-1

all: build

build:
	docker build -t brunoballekens/kairosdb-scala-driver-it:${VERSION} .

publish: build
	docker push brunoballekens/kairosdb-scala-driver-it:${VERSION}

.PHONY: all build push
