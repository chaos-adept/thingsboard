#
# Copyright © 2016-2022 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Image with pre compiled resources

FROM openjdk:11-jre-slim

WORKDIR /usr/local/lib/app

ARG VERSION=3.3.4
ENV ENTRY_POINT_VERSION=${VERSION}

COPY ./application/target/data ./data
COPY ./application/target/conf ./conf
COPY ./application/target/thingsboard-${VERSION}-SNAPSHOT-boot.jar ./thingsboard-${VERSION}-SNAPSHOT-boot.jar

EXPOSE 8080
ENTRYPOINT java -jar thingsboard-${ENTRY_POINT_VERSION}-SNAPSHOT-boot.jar
