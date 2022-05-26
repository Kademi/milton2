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

/**
 *
 * @author brad
 */
public class DefaultRequestHostService implements RequestHostService {

	@Override
	public String getHostName(Request request) {
		if( request == null ) {
			return null;
		}
		String host = request.getHostHeader();
		if (host.contains(":")) {
			host = host.substring(0, host.indexOf(":"));
		}
		if (host == null) {
			host = "nohost";
		}
		return host;
	}

}
