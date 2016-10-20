VERSION=0.5.0
PROJECT=lcmap-see
STANDALONE=target/$(PROJECT)-$(VERSION)-SNAPSHOT-standalone.jar
ROOT_DIR = $(shell pwd)

include dev-resources/make/code.mk
include dev-resources/make/docs.mk
include dev-resources/make/docker.mk

