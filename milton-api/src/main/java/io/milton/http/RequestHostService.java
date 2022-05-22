/*
 * Copyright 2022 McEvoy Software Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.milton.http;

import io.milton.resource.Resource;

/**
 * Pluggable mechanism to determine the host name of a request. This will normally
 * be based on the host header in the request, but can be extended to include logic
 * determining the host from cookies etc
 *
 * @author brad
 */
public interface RequestHostService {
    String getHostName(Request request, Resource resource);
}
