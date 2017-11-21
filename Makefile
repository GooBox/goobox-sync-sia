#
# Copyright (C) 2017 Junpei Kawamoto
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
NAME := goobox-sync-sia
VERSION := 0.0.3
.PHONY: build test dist swagger

build:
	mvn package

test:
	mvn test

dist: 
	mkdir -p dist/$(VERSION) && \
	cd dist/$(VERSION) && \
  cp ../../LICENSE . && \
  cp ../../README.md . && \
	cp ../../target/$(NAME)-$(VERSION).jar . && \
	cp ../../asset/* . && \
	zip -r $(NAME)-$(VERSION).zip . && \
	cd ../..

swagger:
	swagger-codegen generate -i apispec/swagger.json -l java \
			--git-user-id GooBox --git-repo-id goobox-sync-sia \
			--http-user-agent Sia-Agent \
		 	--api-package io.goobox.sync.sia.client.api \
			--model-package io.goobox.sync.sia.client.api.model
